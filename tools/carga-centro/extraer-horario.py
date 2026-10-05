#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Vuelca la vista de GRUPOS de los PDF del programa de horarios del centro al esquema de S115.

Nace en S196 (C-volcado-grupos, condición 1 de O-carga-2026). El extractor de S115 no se
versionó; su método está en docs/horario-referencia/RESUMEN-EXTRACCION.md y aquí se rehace
con dos cambios que pide el material de 2026/2027: las columnas se leen de la cabecera de
cada página (el paso pasó de 72 a 69) y los tramos salen del VALOR de la etiqueta de hora
(la reimpresión del 25/09 no imprime la franja del recreo).

USO
    python3 tools/carga-centro/extraer-horario.py --modo grupos --salida <dir ABSOLUTO> <pdf1> [<pdf2> ...]

    --salida debe ser absoluta y no existir o estar vacía: si tiene algo, aborta sin escribir.
    Cuando dos PDF traen el mismo grupo (por el título «Grupo: ...»), gana el que va DESPUÉS
    en la línea de órdenes. La composición es por título, nunca por número de página.

LECTURA. `pdftotext -bbox` (poppler), una llamada por PDF; solo biblioteca estándar. Los
textos que se buscan se derivaron del volcado de la página 1 de P05 (S196, M2):
    título           «Grupo:» y el resto de palabras de su línea
    cabeceras de día Lunes · Martes · Miérc. · Jueves · Viernes
    horas            H:MM en el margen izquierdo, por pares inicio/fin de tramo
    leyendas         «Profesores:» y «Asignaturas:», en dos columnas código + nombre

REJILLA
  - Columnas: el centro de cada cabecera de día de la página; una palabra va a la columna
    cuyo tramo [punto medio, punto medio] contiene su centro. Las columnas de los extremos
    se cierran con medio paso. Nada de x fijas.
  - Filas: por el valor de la hora de inicio de cada par de etiquetas (TRAMOS). El 11:00 es
    el recreo y se salta por su valor. Una etiqueta desconocida aborta.
  - Dentro de la celda, cada línea con palabra a la derecha del centro de la columna es una
    entrada: asignatura a la izquierda (código de la leyenda «Asignaturas:») y profesor a la
    derecha (código de «Profesores:»; si fueran varios, se unen con «/», convención de S115).
    Una línea con una sola palabra a la izquierda justo debajo de una entrada es su aula.
    Cualquier otra cosa no clasifica: no se adivina, queda como token PERDIDO.
  - Página sin cabeceras de día ni horas y con el mismo «Grupo:» que la anterior: es la
    continuación de su leyenda. Sus códigos se AÑADEN a la leyenda de la anterior, a la
    sección en que esta acabó, antes de clasificar. No genera fichero.

SALIDA. Un grupo-<código>.json por grupo con el esquema de S115 (_meta {fuente, pagina,
codigo_crudo, modo}; celdas [{dia, tramo, asignatura, profesor, aula, confianza, nota}]), y
un informe por stdout. rc=0 solo con 0 perdidos y 0 duplicados; si no, no escribe nada.
rc=1 por cuadre, rc=2 por uso o estructura de página no reconocida.
"""
import argparse
import html
import json
import os
import re
import shutil
import subprocess
import sys
from collections import Counter

TITULO = "Grupo:"
DIAS = ("Lunes", "Martes", "Miérc.", "Jueves", "Viernes")
LEY_PROF = "Profesores:"
LEY_ASIG = "Asignaturas:"
HORA = re.compile(r"^\d{1,2}:\d{2}$")
# Hora de inicio -> (tramo, hora de fin). None es el recreo.
TRAMOS = {
    "8:00": (1, "9:00"),
    "9:00": (2, "10:00"),
    "10:00": (3, "11:00"),
    "11:00": (None, "11:30"),
    "11:30": (4, "12:30"),
    "12:30": (5, "13:30"),
    "13:30": (6, "14:30"),
}
TOL_LINEA = 1.0       # misma línea: tops a menos de esto
MARGEN_FILA = 2.0     # la primera línea de un tramo está a la altura de su etiqueta

PAGINA_RE = re.compile(r"<page\b[^>]*>(.*?)</page>", re.S)
PALABRA_RE = re.compile(
    r'<word xMin="([-\d.]+)" yMin="([-\d.]+)" xMax="([-\d.]+)" yMax="([-\d.]+)">(.*?)</word>', re.S)


class ErrorExtraccion(Exception):
    """Estructura de página no reconocida: aborta con rc=2."""


# ------------------------------------------------------------------ lectura

def version_poppler():
    r = subprocess.run(["pdftotext", "-v"], capture_output=True, text=True)
    return (r.stderr or r.stdout).splitlines()[0].strip()


def palabras_de_bbox(texto):
    """Parsea la salida de `pdftotext -bbox`: lista de páginas, cada una lista de palabras."""
    paginas = []
    for n, cuerpo in enumerate(PAGINA_RE.findall(texto), start=1):
        paginas.append([
            {"id": (n, i), "text": html.unescape(t), "x0": float(a), "top": float(b),
             "x1": float(c), "bottom": float(d)}
            for i, (a, b, c, d, t) in enumerate(PALABRA_RE.findall(cuerpo))])
    return paginas


def leer_pdf(ruta):
    r = subprocess.run(["pdftotext", "-bbox", ruta, "-"], capture_output=True)
    if r.returncode != 0:
        raise ErrorExtraccion("pdftotext -bbox rc=%d sobre %s: %s"
                              % (r.returncode, ruta, r.stderr.decode("utf-8", "replace").strip()))
    return palabras_de_bbox(r.stdout.decode("utf-8"))


# ------------------------------------------------------------------ geometría

def lineas(palabras):
    """Agrupa por top (tolerancia TOL_LINEA); cada línea, ordenada por x0."""
    res = []
    for w in sorted(palabras, key=lambda w: (w["top"], w["x0"])):
        if res and abs(w["top"] - res[-1][0]["top"]) <= TOL_LINEA:
            res[-1].append(w)
        else:
            res.append([w])
    return [sorted(l, key=lambda w: w["x0"]) for l in res]


def centro_x(w):
    return (w["x0"] + w["x1"]) / 2


def titulo_de(lins):
    for l in lins:
        for i, w in enumerate(l):
            if w["text"] == TITULO:
                return " ".join(x["text"] for x in l[i + 1:]), l
    return None, None


def cabeceras(palabras):
    """Centros de las cinco cabeceras de día, o None si la página no tiene ninguna."""
    vistas = {}
    for w in palabras:
        if w["text"] in DIAS:
            if w["text"] in vistas:
                raise ErrorExtraccion("cabecera %r repetida en la página" % w["text"])
            vistas[w["text"]] = w
    if not vistas:
        return None
    if len(vistas) != len(DIAS):
        raise ErrorExtraccion("faltan cabeceras de día: %s" % sorted(set(DIAS) - set(vistas)))
    return [vistas[d] for d in DIAS]


def limites_columnas(centros):
    """[(izq, der)] por día: puntos medios entre centros; los extremos, medio paso."""
    medios = [(centros[i] + centros[i + 1]) / 2 for i in range(len(centros) - 1)]
    izq = [centros[0] - (centros[1] - centros[0]) / 2] + medios
    der = medios + [centros[-1] + (centros[-1] - centros[-2]) / 2]
    return list(zip(izq, der))


def columna_de(w, limites):
    c = centro_x(w)
    for i, (a, b) in enumerate(limites):
        if a <= c < b:
            return i + 1
    return None


def filas(etiquetas):
    """Pares (inicio, fin) de etiquetas de hora -> [(top de inicio, tramo o None)]."""
    if len(etiquetas) % 2:
        raise ErrorExtraccion("número impar de etiquetas de hora: %s" % [w["text"] for w in etiquetas])
    res = []
    for i in range(0, len(etiquetas), 2):
        ini, fin = etiquetas[i], etiquetas[i + 1]
        if ini["text"] not in TRAMOS:
            raise ErrorExtraccion("etiqueta de hora desconocida: %r" % ini["text"])
        tramo, fin_esperado = TRAMOS[ini["text"]]
        if fin["text"] != fin_esperado:
            raise ErrorExtraccion("tramo %s-%s: se esperaba fin %s" % (ini["text"], fin["text"], fin_esperado))
        res.append((ini["top"], tramo))
    return res


def tramo_de(w, filas_):
    """(True, tramo) de la última fila cuyo inicio queda por encima de w; el tramo es None en
    el recreo. (False, None) si w está por encima de la primera fila."""
    res = (False, None)
    for top, t in filas_:
        if w["top"] >= top - MARGEN_FILA:
            res = (True, t)
    return res


# ------------------------------------------------------------------ leyenda

def secciones_de_leyenda(lins):
    """Líneas de leyenda de un grupo (las de su página y las de sus continuaciones, en orden)
    -> ([(sección, línea de contenido)], nº de palabras de leyenda). Las cabeceras
    «Profesores:» y «Asignaturas:» abren sección; la continuación sigue en la que estaba."""
    seccion = None
    contenido = []
    n = 0
    for l in lins:
        textos = [w["text"] for w in l]
        n += len(l)
        if LEY_PROF in textos or LEY_ASIG in textos:
            if len(l) != 1:
                raise ErrorExtraccion("cabecera de leyenda con más texto en su línea: %s" % textos)
            seccion = "prof" if textos[0] == LEY_PROF else "asig"
            continue
        if seccion is None:
            raise ErrorExtraccion("línea de leyenda antes de «%s» o «%s»: %s" % (LEY_PROF, LEY_ASIG, textos))
        contenido.append((seccion, l))
    return contenido, n


def columnas_de_codigo(contenido):
    """Cada línea lleva una o dos parejas «código nombre» en columnas fijas. Sobre TODAS las
    líneas de leyenda de un PDF, las columnas son las x0 presentes en más de la mitad de las
    líneas: deben salir cuatro (código, nombre, código, nombre) o aborta. Con las de un solo
    grupo no basta: en uno de pocas líneas, una palabra interna de nombre también alinea."""
    cuenta = Counter()
    for _, l in contenido:
        for x in {round(w["x0"], 1) for w in l}:
            cuenta[x] += 1
    columnas = sorted(x for x, k in cuenta.items() if 2 * k > len(contenido))
    if len(columnas) != 4:
        raise ErrorExtraccion("leyenda: se esperaban 4 columnas alineadas y salen %s" % columnas)
    for _, l in contenido:
        if round(l[0]["x0"], 1) != columnas[0]:
            raise ErrorExtraccion("línea de leyenda que no empieza en la columna de código: %s"
                                  % [w["text"] for w in l])
    return columnas[0], columnas[2]


def codigos_de_leyenda(contenido, x_codigo):
    codigos = {"prof": [], "asig": []}
    for sec, l in contenido:
        codigos[sec].extend(w for w in l if round(w["x0"], 1) in x_codigo)
    return codigos


# ------------------------------------------------------------------ página

def analizar_pagina(palabras):
    """Estructura de una página. tipo: 'rejilla' o 'continuacion'."""
    lins = lineas(palabras)
    titulo, linea_titulo = titulo_de(lins)
    if titulo is None:
        raise ErrorExtraccion("página sin «%s»" % TITULO)
    cab = cabeceras(palabras)
    ids_titulo = {w["id"] for w in linea_titulo}
    if cab is None:
        if any(HORA.match(w["text"]) for w in palabras):
            raise ErrorExtraccion("página con horas y sin cabeceras de día")
        resto = [l for l in lins if l is not linea_titulo]
        return {"tipo": "continuacion", "titulo": titulo, "lineas_leyenda": resto}
    centros = [centro_x(w) for w in cab]
    limites = limites_columnas(centros)
    suelo_cab = max(w["bottom"] for w in cab)
    ley = [w for w in palabras if w["text"] == LEY_PROF]
    if len(ley) != 1:
        raise ErrorExtraccion("página de rejilla con %d «%s»" % (len(ley), LEY_PROF))
    techo_ley = ley[0]["top"]
    banda = [w for w in palabras if w["top"] > suelo_cab and w["top"] < techo_ley - TOL_LINEA]
    etiquetas = sorted((w for w in banda if HORA.match(w["text"]) and w["x1"] < limites[0][0]),
                       key=lambda w: w["top"])
    if not etiquetas:
        raise ErrorExtraccion("página con cabeceras de día y sin etiquetas de hora")
    ids_et = {w["id"] for w in etiquetas}
    rejilla = [w for w in banda if w["id"] not in ids_et]
    lineas_leyenda = [l for l in lins if l[0]["top"] >= techo_ley - TOL_LINEA]
    return {"tipo": "rejilla", "titulo": titulo, "centros": centros, "limites": limites,
            "filas": filas(etiquetas), "rejilla": rejilla, "lineas_leyenda": lineas_leyenda,
            "ids_titulo": ids_titulo}


# ------------------------------------------------------------------ clasificación

def clasificar(rejilla, centros, limites, filas_, prof, asig):
    """Asigna palabras de rejilla a celdas. Cada celda: dia, tramo, asignatura, profesor, aula
    (palabras) y la lista de ids que consume. Lo que no clasifica no se asigna."""
    por_celda = {}
    for w in rejilla:
        dia = columna_de(w, limites)
        dentro, tramo = tramo_de(w, filas_)
        if dia is None or not dentro or tramo is None:
            continue  # fuera de columnas, sobre la primera fila o en el recreo: perdido
        por_celda.setdefault((tramo, dia), []).append(w)
    celdas = []
    for (tramo, dia) in sorted(por_celda):
        centro = centros[dia - 1]
        previa = None
        for l in lineas(por_celda[(tramo, dia)]):
            izq = [w for w in l if w["x0"] < centro]
            der = [w for w in l if w["x0"] >= centro]
            actual = None
            if der:
                if len(izq) == 1 and izq[0]["text"] in asig and all(w["text"] in prof for w in der):
                    actual = {"dia": dia, "tramo": tramo, "asignatura": izq[0], "profesor": der,
                              "aula": None}
                    celdas.append(actual)
            elif len(izq) == 1 and previa is not None and previa["aula"] is None:
                previa["aula"] = izq[0]
            previa = actual
    return celdas


def ids_de(celda):
    ids = [celda["asignatura"]["id"]] + [w["id"] for w in celda["profesor"]]
    if celda["aula"] is not None:
        ids.append(celda["aula"]["id"])
    return ids


def cuadrar(palabras_rejilla, celdas):
    """Función pura. Perdidos: palabras de rejilla que ninguna celda consume. Duplicados:
    palabras que consume más de una celda."""
    usos = Counter(i for c in celdas for i in ids_de(c))
    perdidos = [w for w in palabras_rejilla if usos[w["id"]] == 0]
    duplicados = [w for w in palabras_rejilla if usos[w["id"]] > 1]
    return perdidos, duplicados


def a_json(celda):
    return {
        "dia": celda["dia"],
        "tramo": celda["tramo"],
        "asignatura": celda["asignatura"]["text"],
        "profesor": "/".join(w["text"] for w in celda["profesor"]),
        "aula": celda["aula"]["text"] if celda["aula"] is not None else None,
        "confianza": "alta",
        "nota": "",
    }


def nombre_fichero(titulo):
    return "grupo-%s.json" % "-".join(titulo.replace("º", "").split())


# ------------------------------------------------------------------ composición

def procesar(documentos):
    """documentos: [(nombre del PDF, [páginas de palabras])], en orden de precedencia creciente.
    Devuelve el resultado completo sin escribir nada."""
    grupos = {}
    sustituidos = []
    paginas = 0
    for nombre, pags in documentos:
        vistos = {}
        ultimo = None
        del_documento = []
        for n, palabras in enumerate(pags, start=1):
            paginas += 1
            p = analizar_pagina(palabras)
            if p["tipo"] == "continuacion":
                if ultimo is None or ultimo["titulo"] != p["titulo"]:
                    raise ErrorExtraccion("%s p%d: página sin rejilla que no continúa a la anterior (%r)"
                                          % (nombre, n, p["titulo"]))
                ultimo["continuaciones"].append(n)
                ultimo["lineas_leyenda"].extend(p["lineas_leyenda"])
                continue
            if p["titulo"] in vistos:
                raise ErrorExtraccion("%s p%d: grupo %r ya visto en p%d"
                                      % (nombre, n, p["titulo"], vistos[p["titulo"]]))
            vistos[p["titulo"]] = n
            p.update({"fuente": nombre, "pagina": n, "continuaciones": []})
            ultimo = p
            del_documento.append(p)
            if p["titulo"] in grupos:
                sustituidos.append((p["titulo"], grupos[p["titulo"]]["fuente"], nombre))
            grupos[p["titulo"]] = p
        for g in del_documento:
            g["leyenda"], g["palabras_leyenda"] = secciones_de_leyenda(g["lineas_leyenda"])
        x_codigo = columnas_de_codigo([c for g in del_documento for c in g["leyenda"]])
        for g in del_documento:
            g["x_codigo"] = x_codigo
    res = {"paginas": paginas, "grupos": [], "sustituidos": sustituidos}
    for titulo, g in grupos.items():
        codigos = codigos_de_leyenda(g["leyenda"], g["x_codigo"])
        n_ley = g["palabras_leyenda"]
        prof = {w["text"] for w in codigos["prof"]}
        asig = {w["text"] for w in codigos["asig"]}
        celdas = clasificar(g["rejilla"], g["centros"], g["limites"], g["filas"], prof, asig)
        perdidos, duplicados = cuadrar(g["rejilla"], celdas)
        res["grupos"].append({
            "titulo": titulo, "fuente": g["fuente"], "pagina": g["pagina"],
            "continuaciones": g["continuaciones"], "fichero": nombre_fichero(titulo),
            "celdas": celdas, "rejilla": g["rejilla"], "codigos": codigos, "palabras_leyenda": n_ley,
            "perdidos": perdidos, "duplicados": duplicados})
    ficheros = Counter(g["fichero"] for g in res["grupos"])
    choque = [f for f, n in ficheros.items() if n > 1]
    if choque:
        raise ErrorExtraccion("dos grupos dan el mismo nombre de fichero: %s" % choque)
    return res


def codigo_salida(res):
    return 0 if all(not g["perdidos"] and not g["duplicados"] for g in res["grupos"]) else 1


def volcado(g):
    return {
        "_meta": {"fuente": g["fuente"], "pagina": g["pagina"], "codigo_crudo": g["titulo"],
                  "modo": "grupos"},
        "celdas": [a_json(c) for c in g["celdas"]],
    }


def informe(res, version):
    out = ["extraer-horario.py --modo grupos", "poppler: %s" % version]
    tot = Counter()
    for g in res["grupos"]:
        cont = (" + continuación p%s" % ",".join(map(str, g["continuaciones"]))) if g["continuaciones"] else ""
        n_cod = len(g["codigos"]["prof"]) + len(g["codigos"]["asig"])
        out.append("  %-24s %-30s p%-3d%s  celdas=%d rejilla=%d leyenda=%d (códigos prof=%d asig=%d) "
                   "perdidos=%d duplicados=%d"
                   % (g["fichero"], g["fuente"], g["pagina"], cont, len(g["celdas"]), len(g["rejilla"]),
                      g["palabras_leyenda"], len(g["codigos"]["prof"]), len(g["codigos"]["asig"]),
                      len(g["perdidos"]), len(g["duplicados"])))
        for w in g["perdidos"]:
            out.append("      PERDIDO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        for w in g["duplicados"]:
            out.append("      DUPLICADO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        tot["celdas"] += len(g["celdas"])
        tot["rejilla"] += len(g["rejilla"])
        tot["leyenda"] += g["palabras_leyenda"]
        tot["codigos"] += n_cod
        tot["perdidos"] += len(g["perdidos"])
        tot["duplicados"] += len(g["duplicados"])
    for t, antes, despues in res["sustituidos"]:
        out.append("  sustituido %r: %s -> %s" % (t, antes, despues))
    out.append("páginas=%d grupos=%d celdas=%d tokens de rejilla=%d tokens de leyenda=%d (códigos=%d) "
               "perdidos=%d duplicados=%d"
               % (res["paginas"], len(res["grupos"]), tot["celdas"], tot["rejilla"], tot["leyenda"],
                  tot["codigos"], tot["perdidos"], tot["duplicados"]))
    return "\n".join(out)


def escribir(res, salida):
    os.makedirs(salida, exist_ok=True)
    for g in sorted(res["grupos"], key=lambda g: g["fichero"]):
        with open(os.path.join(salida, g["fichero"]), "w", encoding="utf-8") as f:
            json.dump(volcado(g), f, ensure_ascii=False, indent=2)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--modo", required=True, choices=["grupos"])
    ap.add_argument("--salida", required=True)
    ap.add_argument("pdfs", nargs="+")
    a = ap.parse_args(argv)
    if not os.path.isabs(a.salida):
        print("ERROR: --salida debe ser una ruta absoluta: %r" % a.salida, file=sys.stderr)
        return 2
    if os.path.exists(a.salida):
        if not os.path.isdir(a.salida) or os.listdir(a.salida):
            print("ERROR: --salida existe y no es un directorio vacío: %s" % a.salida, file=sys.stderr)
            return 2
    if shutil.which("pdftotext") is None:
        print("ERROR: no se encuentra pdftotext (poppler-utils) en el PATH", file=sys.stderr)
        return 2
    try:
        docs = [(os.path.basename(p), leer_pdf(p)) for p in a.pdfs]
        res = procesar(docs)
    except ErrorExtraccion as e:
        print("ERROR: %s" % e, file=sys.stderr)
        return 2
    print(informe(res, version_poppler()))
    rc = codigo_salida(res)
    if rc == 0:
        escribir(res, a.salida)
        print("escritos %d ficheros en %s" % (len(res["grupos"]), a.salida))
    else:
        print("CUADRE FALLIDO: no se escribe nada")
    return rc


if __name__ == "__main__":
    sys.exit(main())
