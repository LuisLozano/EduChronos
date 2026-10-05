#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Cruce determinista de los volcados de 2026/2027: vista de grupos <-> vista de profesores.

Nace en S197 (C-volcado-profesores, condición 2 de O-carga-2026). Sigue la estructura de
docs/horario-referencia/INFORME-RECONCILIACION.md (grupos <-> aulas de S115).

USO
    python3 tools/carga-centro/cruzar-grupos-profesores.py --volcados <dir> --informe <fichero.md> \\
        <pdf de grupos 1> [<pdf de grupos 2> ...]

    --volcados: carpeta con los grupo-*.json y profesor-*.json.
    PDF de grupos: los mismos y en el mismo orden que se pasaron a extraer-horario.py --modo
    grupos (el último gana por título). Solo se leen para la leyenda «Profesores:», que los
    volcados no guardan.

CLAVE: (grupo, día, tramo, profesor).
NORMALIZACIÓN, solo aquí (los volcados conservan lo crudo):
  - grupo: el título de grupo-*.json pasa a su forma corta con codigos_grupo.forma_corta; la
    vista de profesores ya imprime la forma corta.
  - profesor: el título de cada profesor-*.json («Profesor: ...») se casa con el nombre de la
    leyenda «Profesores:» de los PDF de grupos, que viene truncado: el nombre de la leyenda debe
    ser prefijo del título. Uno -> su código; ninguno -> «sin código»; varios -> aborta.
ENTRADAS: desde grupos, una por celda (clave repetida -> aborta). Desde profesores, una por cada
grupo de cada celda con grupos (clave repetida -> aborta); las páginas sin código no entran en
el cruce y se listan aparte.

rc=0 aunque haya discrepancias; rc=2 ante error estructural (y no se escribe el informe).
"""
import argparse
import glob
import importlib.util
import json
import os
import sys
from collections import defaultdict

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from codigos_grupo import forma_corta, SinFormaCorta  # noqa: E402

_spec = importlib.util.spec_from_file_location("extraer_horario", os.path.join(AQUI, "extraer-horario.py"))
eh = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(eh)

SIN_CODIGO = "sin código"
DIAS = {1: "Lunes", 2: "Martes", 3: "Miércoles", 4: "Jueves", 5: "Viernes"}


class ErrorCruce(Exception):
    """Error estructural: aborta con rc=2."""


def norm(s):
    return " ".join(s.split())


# ------------------------------------------------------------------ entradas

def leyenda_profesores(documentos):
    """documentos: [(nombre, páginas)] de los PDF de grupos -> {código: nombre}. Misma composición
    que el extractor; un código con dos nombres distintos aborta."""
    res = eh.procesar(documentos, "grupos")
    mapa = {}
    for g in res["grupos"]:
        for cod, nombre in g["nombres"]["prof"].items():
            if mapa.setdefault(cod, nombre) != nombre:
                raise ErrorCruce("leyenda: el código %r tiene dos nombres: %r y %r" % (cod, mapa[cod], nombre))
    return mapa


def mapa_profesores(titulos, leyenda):
    """titulos: {página: título}; leyenda: {código: nombre} -> {página: código o SIN_CODIGO}."""
    res = {}
    for pag, titulo in sorted(titulos.items()):
        casan = sorted(c for c, n in leyenda.items() if norm(titulo).startswith(norm(n)))
        if len(casan) > 1:
            raise ErrorCruce("p%d %r: el prefijo casa con varios códigos: %s" % (pag, titulo, casan))
        res[pag] = casan[0] if casan else SIN_CODIGO
    return res


def entradas_grupos(volcados):
    """volcados: [grupo-*.json cargados] -> {clave: {asignatura, aula, titulo}}."""
    res = {}
    for v in volcados:
        titulo = v["_meta"]["codigo_crudo"]
        try:
            corto = forma_corta(titulo)
        except SinFormaCorta as e:
            raise ErrorCruce(str(e))
        for c in v["celdas"]:
            clave = (corto, c["dia"], c["tramo"], c["profesor"])
            if clave in res:
                raise ErrorCruce("grupos: clave repetida %s" % (clave,))
            res[clave] = {"asignatura": c["asignatura"], "aula": c["aula"], "titulo": titulo}
    return res


def entradas_profesores(volcados, mapa):
    """volcados: [profesor-*.json cargados]; mapa: {página: código} -> ({clave: {asignatura, aula,
    pagina}}, aparte) con aparte = {sin_grupo, recreo, sin_codigo}."""
    res = {}
    sin_grupo = defaultdict(lambda: {"celdas": 0, "profesores": set()})
    recreo = defaultdict(int)
    sin_codigo = []
    for v in volcados:
        pag = v["_meta"]["pagina"]
        cod = mapa[pag]
        if cod == SIN_CODIGO:
            todas = v["celdas"] + v["recreo"]
            sin_codigo.append({"pagina": pag, "titulo": v["_meta"]["codigo_crudo"], "celdas": len(todas),
                               "codigos": sorted({c["asignatura"] for c in todas}),
                               "con_grupo": sum(1 for c in todas if c["grupos"])})
            continue
        for c in v["recreo"]:
            recreo[c["asignatura"]] += 1
        for c in v["celdas"]:
            if not c["grupos"]:
                sin_grupo[c["asignatura"]]["celdas"] += 1
                sin_grupo[c["asignatura"]]["profesores"].add(cod)
                continue
            for g in c["grupos"].split():
                clave = (g, c["dia"], c["tramo"], cod)
                if clave in res:
                    raise ErrorCruce("profesores: clave repetida %s" % (clave,))
                res[clave] = {"asignatura": c["asignatura"], "aula": c["aula"], "pagina": pag}
    aparte = {"sin_grupo": {k: {"celdas": v["celdas"], "profesores": len(v["profesores"])}
                            for k, v in sin_grupo.items()},
              "recreo": dict(recreo), "sin_codigo": sorted(sin_codigo, key=lambda x: x["pagina"])}
    return res, aparte


# ------------------------------------------------------------------ cruce

def cruzar(G, P):
    """G, P: {clave: entrada} -> secciones de discrepancias, cada una lista ordenada."""
    comunes = sorted(set(G) & set(P))
    return {
        "solo_grupos": [(k, G[k]) for k in sorted(set(G) - set(P))],
        "solo_profesores": [(k, P[k]) for k in sorted(set(P) - set(G))],
        "asignatura": [(k, G[k], P[k]) for k in comunes if G[k]["asignatura"] != P[k]["asignatura"]],
        "aula_distinta": [(k, G[k], P[k]) for k in comunes
                          if G[k]["aula"] is not None and P[k]["aula"] is not None and G[k]["aula"] != P[k]["aula"]],
        "aula_una_vista": [(k, G[k], P[k]) for k in comunes if (G[k]["aula"] is None) != (P[k]["aula"] is None)],
        "comunes": len(comunes),
    }


# ------------------------------------------------------------------ informe

def _dt(k):
    return "%s · T%d" % (DIAS[k[1]], k[2])


def _a(x):
    return "—" if x is None else "`%s`" % x


def informe_md(titulos_grupo, leyenda, mapa, titulos_prof, G, P, sec, aparte, fuentes):
    o = []
    o.append("# Informe de cruce — horario por grupos ↔ horario por profesores (2026/2027)")
    o.append("")
    o.append("Cruce determinista de los volcados de `docs/horario-referencia/2026-2027/` (`grupo-*.json` vs "
             "`profesor-*.json`), generado por `tools/carga-centro/cruzar-grupos-profesores.py` (S197). "
             "**Clave de cruce:** `(grupo, día, tramo, profesor)`.")
    o.append("")
    o.append("> Leyenda «Profesores:» leída de: %s (el último gana por título, como en el extractor). "
             "Los volcados no la guardan." % ", ".join("`%s`" % f for f in fuentes))
    o.append("")
    o.append("## Normalización de códigos de grupo")
    o.append("")
    o.append("La vista de grupos titula con forma larga y la de profesores imprime la forma corta. Mapeo de "
             "`tools/carga-centro/codigos_grupo.py`, aplicado **solo para el cruce**:")
    o.append("")
    o.append("| Forma larga (grupos) | Forma corta (profesores) |")
    o.append("|---|---|")
    for t in sorted(titulos_grupo):
        o.append("| `%s` | `%s` |" % (t, forma_corta(t)))
    o.append("")
    o.append("## Normalización de profesores")
    o.append("")
    o.append("Cada página de la vista de profesores se casa con el código de la leyenda «Profesores:» cuyo "
             "nombre (truncado en la leyenda) es prefijo del título. Uno → su código; ninguno → «%s»; "
             "varios → error." % SIN_CODIGO)
    o.append("")
    o.append("| Página | Título (profesores) | Código | Nombre en la leyenda |")
    o.append("|---|---|---|---|")
    for pag in sorted(mapa):
        cod = mapa[pag]
        o.append("| %d | %s | %s | %s |" % (pag, titulos_prof[pag], "`%s`" % cod if cod != SIN_CODIGO else SIN_CODIGO,
                                          leyenda.get(cod, "—")))
    o.append("")
    o.append("Códigos de la leyenda sin página: **%d**%s." % (
        len(set(leyenda) - set(mapa.values())),
        (" (%s)" % ", ".join("`%s`" % c for c in sorted(set(leyenda) - set(mapa.values()))))
        if set(leyenda) - set(mapa.values()) else ""))
    o.append("")

    def seccion(num, titulo, texto, cab, filas):
        o.append("## %d. %s" % (num, titulo))
        o.append("")
        o.append(texto)
        o.append("")
        o.append("**Total: %d.**" % len(filas))
        o.append("")
        o.append(cab)
        o.append("|" + "---|" * (cab.count("|") - 1))
        o.extend(filas)
        o.append("")

    seccion(1, "Solo en el horario por grupos", "Clave presente en *grupos* y ausente en *profesores*.",
            "| Grupo | Día · Tramo | Profesor | Asignatura | Aula | Título (grupos) |",
            ["| %s | %s | `%s` | `%s` | %s | %s |" % (k[0], _dt(k), k[3], e["asignatura"], _a(e["aula"]), e["titulo"])
             for k, e in sec["solo_grupos"]])
    seccion(2, "Solo en el horario por profesores", "Clave presente en *profesores* y ausente en *grupos*.",
            "| Grupo | Día · Tramo | Profesor | Asignatura | Aula | Página (profesores) |",
            ["| %s | %s | `%s` | `%s` | %s | %d |" % (k[0], _dt(k), k[3], e["asignatura"], _a(e["aula"]), e["pagina"])
             for k, e in sec["solo_profesores"]])
    seccion(3, "Asignatura distinta", "Misma clave en las dos vistas con distinto código de asignatura.",
            "| Grupo | Día · Tramo | Profesor | Asignatura (grupos) | Asignatura (profesores) |",
            ["| %s | %s | `%s` | `%s` | `%s` |" % (k[0], _dt(k), k[3], g["asignatura"], p["asignatura"])
             for k, g, p in sec["asignatura"]])
    seccion(4, "Aula distinta", "Misma clave, aula no nula en las dos vistas y distinta.",
            "| Grupo | Día · Tramo | Profesor | Aula (grupos) | Aula (profesores) |",
            ["| %s | %s | `%s` | %s | %s |" % (k[0], _dt(k), k[3], _a(g["aula"]), _a(p["aula"]))
             for k, g, p in sec["aula_distinta"]])
    seccion(5, "Aula en una sola vista", "Misma clave, aula impresa en una vista y no en la otra.",
            "| Grupo | Día · Tramo | Profesor | Asignatura | Aula (grupos) | Aula (profesores) |",
            ["| %s | %s | `%s` | `%s` | %s | %s |" % (k[0], _dt(k), k[3], g["asignatura"], _a(g["aula"]), _a(p["aula"]))
             for k, g, p in sec["aula_una_vista"]])

    o.append("## Aparte (no son discrepancias)")
    o.append("")
    o.append("### Actividades sin grupo")
    o.append("Celdas de la vista de profesores sin ningún grupo (guardias, reuniones…): no tienen equivalente "
             "en la vista de grupos.")
    o.append("")
    o.append("| Código | Celdas | Profesores |")
    o.append("|---|---|---|")
    sg = aparte["sin_grupo"]
    for k in sorted(sg, key=lambda k: (-sg[k]["celdas"], k)):
        o.append("| `%s` | %d | %d |" % (k, sg[k]["celdas"], sg[k]["profesores"]))
    o.append("")
    o.append("### Recreo")
    o.append("Celdas de la fila del recreo de la vista de profesores (la de grupos no la tiene).")
    o.append("")
    o.append("| Código | Celdas |")
    o.append("|---|---|")
    rc = aparte["recreo"]
    for k in sorted(rc, key=lambda k: (-rc[k], k)):
        o.append("| `%s` | %d |" % (k, rc[k]))
    o.append("")
    o.append("### Profesores sin código")
    o.append("Páginas cuyo título no casa con ningún nombre de la leyenda; no entran en el cruce.")
    o.append("")
    o.append("| Página | Celdas | Celdas con grupo | Códigos de asignatura |")
    o.append("|---|---|---|---|")
    for x in aparte["sin_codigo"]:
        o.append("| %d | %d | %d | %s |" % (x["pagina"], x["celdas"], x["con_grupo"],
                                            ", ".join("`%s`" % c for c in x["codigos"]) or "—"))
    o.append("")
    o.append("## Resumen")
    o.append("")
    o.append("- Entradas de **grupos** (una por celda): **%d**" % len(G))
    o.append("- Entradas de **profesores** (una por grupo de cada celda con grupos): **%d**" % len(P))
    o.append("- Claves comunes: **%d**" % sec["comunes"])
    o.append("- Grupos normalizados: **%d** · profesores con código: **%d** · sin código: **%d**"
             % (len(titulos_grupo), sum(1 for c in mapa.values() if c != SIN_CODIGO),
                sum(1 for c in mapa.values() if c == SIN_CODIGO)))
    o.append("- §1 Solo en grupos: **%d**" % len(sec["solo_grupos"]))
    o.append("- §2 Solo en profesores: **%d**" % len(sec["solo_profesores"]))
    o.append("- §3 Asignatura distinta: **%d**" % len(sec["asignatura"]))
    o.append("- §4 Aula distinta: **%d**" % len(sec["aula_distinta"]))
    o.append("- §5 Aula en una sola vista: **%d**" % len(sec["aula_una_vista"]))
    o.append("- Aparte: actividades sin grupo **%d** celdas (%d códigos) · recreo **%d** celdas · "
             "profesores sin código **%d** páginas"
             % (sum(v["celdas"] for v in sg.values()), len(sg), sum(rc.values()), len(aparte["sin_codigo"])))
    o.append("- **Discrepancias totales (§1–§5): %d**" % sum(len(sec[k]) for k in
             ("solo_grupos", "solo_profesores", "asignatura", "aula_distinta", "aula_una_vista")))
    o.append("")
    return "\n".join(o)


# ------------------------------------------------------------------ main

def cargar(directorio, patron):
    res = []
    for f in sorted(glob.glob(os.path.join(directorio, patron))):
        with open(f, encoding="utf-8") as fh:
            res.append(json.load(fh))
    return res


def ejecutar(volcados_dir, pdfs):
    vg = cargar(volcados_dir, "grupo-*.json")
    vp = cargar(volcados_dir, "profesor-*.json")
    if not vg or not vp:
        raise ErrorCruce("faltan volcados en %s: %d de grupos, %d de profesores" % (volcados_dir, len(vg), len(vp)))
    if any(v["_meta"]["modo"] != "profesores" for v in vp):
        raise ErrorCruce("profesor-*.json con modo distinto de «profesores»")
    try:
        leyenda = leyenda_profesores([(os.path.basename(p), eh.leer_pdf(p)) for p in pdfs])
    except eh.ErrorExtraccion as e:
        raise ErrorCruce("leyenda de grupos: %s" % e)
    titulos_prof = {v["_meta"]["pagina"]: v["_meta"]["codigo_crudo"] for v in vp}
    if len(titulos_prof) != len(vp):
        raise ErrorCruce("dos profesor-*.json con la misma página")
    mapa = mapa_profesores(titulos_prof, leyenda)
    G = entradas_grupos(vg)
    P, aparte = entradas_profesores(vp, mapa)
    sec = cruzar(G, P)
    titulos_grupo = [v["_meta"]["codigo_crudo"] for v in vg]
    return informe_md(titulos_grupo, leyenda, mapa, titulos_prof, G, P, sec, aparte,
                      [os.path.basename(p) for p in pdfs]), sec


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--volcados", required=True)
    ap.add_argument("--informe", required=True)
    ap.add_argument("pdfs", nargs="+")
    a = ap.parse_args(argv)
    try:
        texto, sec = ejecutar(a.volcados, a.pdfs)
    except ErrorCruce as e:
        print("ERROR: %s" % e, file=sys.stderr)
        return 2
    with open(a.informe, "w", encoding="utf-8") as f:
        f.write(texto)
    for k in ("solo_grupos", "solo_profesores", "asignatura", "aula_distinta", "aula_una_vista"):
        print("%-16s %d" % (k, len(sec[k])))
    print("comunes          %d" % sec["comunes"])
    print("escrito %s" % a.informe)
    return 0


if __name__ == "__main__":
    sys.exit(main())
