#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deriva guardias.json: guardias ordinarias y «No puede» por profesor desde los volcados de profesor.

Nace en S216 (C-prueba-guardias-2026, condición 7 de O-guardias), con el contrato de
s216/t0/CONTRATO.md y su enmienda 1 (s216/t1/ENMIENDA-1.md).

USO
    python3 -B tools/carga-centro/derivar-guardias.py --volcados-profesores <dir> \\
        --pdf-grupos <pdf> <pdf> --decisiones <json> --catalogo <json> --salida <json>
    Todas las rutas, absolutas y obligatorias.

ENTRADAS
    --volcados-profesores  carpeta con los profesor-*.json del curso.
    --pdf-grupos           PDF de grupos, en el orden de derivar-catalogo.py: solo dan la leyenda
                           «Profesores:» con la que se pasa del título de página al código.
    --decisiones           decisiones-catalogo.json: profesores.alta y profesores.paginasIgnoradas.
    --catalogo             catalogo-derivado.json: los profesores que tienen que salir.

EMPAREJAMIENTO, el mismo que usó el catálogo (E1): mapa_de_paginas y codigos_de_paginas de
derivar-catalogo.py, importados y sin copia. De ahí vienen dos abortos: una página sin código que
no tenga alta ni esté ignorada (cualquiera, tenga o no guardias) y un código en dos páginas.

CUENTA. Solo las celdas de clase (lista «celdas») cuyo código es EXACTAMENTE G, Gbibl o conv; el
recreo no cuenta. G suma a guardiasOrdinarias; Gbibl y conv son un «No puede» en (dia, ordenEnDia)
con el código literal como motivo (decisión H de S211).

ABORTA sin escribir (rc=2) si un día no está en 1..5 o un tramo en 1..6, si una celda de una página
tiene dos códigos de guardia, si los códigos emparejados no son exactamente los profesores del
catálogo, o por cualquiera de los abortos de la vía del catálogo.

SALIDA guardias.json, determinista: _meta (generadoPor, entradas con nombre y sha256, omitidos),
profesores por código (también los de 0) y noPuede por (codigo, dia, ordenEnDia). Mismo formato
de escritura que derivar-reglas-aulas.py (indent=1, sin salto de línea final).
"""

import argparse
import hashlib
import importlib.util
import json
import os
import sys
from collections import Counter
from pathlib import Path

_spec = importlib.util.spec_from_file_location("derivar_catalogo", Path(__file__).resolve().parent / "derivar-catalogo.py")
dc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(dc)

GUARDIA = "G"
NO_PUEDE = ("Gbibl", "conv")
CODIGOS = (GUARDIA,) + NO_PUEDE
DIAS = range(1, 6)
TRAMOS = range(1, 7)


def guardias_de(pagina):
    """Las celdas de clase con código de guardia exacto, como [(dia, tramo, código)]."""
    return [(c["dia"], c["tramo"], c["asignatura"]) for c in pagina["celdas"] if c["asignatura"] in CODIGOS]


def comprobar_celdas(paginas):
    """Rango de día y tramo de toda celda de clase, y como mucho un código de guardia por celda."""
    for p in paginas:
        donde = "página %d (%s)" % (p["_meta"]["pagina"], p["_meta"]["codigo_crudo"])
        for c in p["celdas"]:
            if c["dia"] not in DIAS or c["tramo"] not in TRAMOS:
                raise dc.Aborto(2, "%s: celda fuera de rango, día %r y tramo %r" % (donde, c["dia"], c["tramo"]))
        dobles = sorted(k for k, n in Counter((d, t) for d, t, _ in guardias_de(p)).items() if n > 1)
        if dobles:
            raise dc.Aborto(2, "%s: celdas con dos códigos de guardia: %s" % (donde, dobles))


def derivar(paginas, mapa, sin_codigo, decisiones, catalogo):
    """{omitidos, profesores, noPuede} o dc.Aborto. `mapa` es el de mapa_de_paginas."""
    comprobar_celdas(paginas)
    codigos, ignoradas = dc.codigos_de_paginas(paginas, mapa, sin_codigo, dc.Decisiones(decisiones))
    emparejados = set(codigos.values())
    del_catalogo = {p["codigo"] for p in catalogo["profesores"]}
    if emparejados != del_catalogo:
        raise dc.Aborto(2, "los códigos emparejados no son los profesores del catálogo: sobran %s, faltan %s"
                        % (sorted(emparejados - del_catalogo), sorted(del_catalogo - emparejados)))
    por_pagina = {p["_meta"]["pagina"]: p for p in paginas}
    profesores, no_puede = [], []
    for pag, codigo in codigos.items():
        celdas = guardias_de(por_pagina[pag])
        profesores.append({"codigo": codigo, "guardiasOrdinarias": sum(1 for _, _, k in celdas if k == GUARDIA)})
        no_puede += [{"codigo": codigo, "dia": d, "ordenEnDia": t, "motivo": k} for d, t, k in celdas if k in NO_PUEDE]
    omitidos = []
    for pag in ignoradas:
        cuenta = Counter(k for _, _, k in guardias_de(por_pagina[pag]))
        omitidos.append(dict({"pagina": pag, "titulo": por_pagina[pag]["_meta"]["codigo_crudo"]},
                             **{k: cuenta[k] for k in CODIGOS}))
    return {
        "omitidos": omitidos,
        "profesores": sorted(profesores, key=lambda p: p["codigo"]),
        "noPuede": sorted(no_puede, key=lambda x: (x["codigo"], x["dia"], x["ordenEnDia"])),
    }


def entrada(ruta):
    return {"fichero": os.path.basename(ruta), "sha256": hashlib.sha256(Path(ruta).read_bytes()).hexdigest()}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--volcados-profesores", required=True)
    ap.add_argument("--pdf-grupos", required=True, nargs="+")
    ap.add_argument("--decisiones", required=True)
    ap.add_argument("--catalogo", required=True)
    ap.add_argument("--salida", required=True)
    args = ap.parse_args(argv)
    try:
        rutas = [args.volcados_profesores, args.decisiones, args.catalogo, args.salida] + args.pdf_grupos
        relativas = [r for r in rutas if not os.path.isabs(r)]
        if relativas:
            raise dc.Aborto(2, "las rutas tienen que ser absolutas: %s" % relativas)
        paginas = dc.cargar_volcados_profesores(args.volcados_profesores)
        mapa, sin_codigo = dc.mapa_de_paginas(paginas, args.pdf_grupos)
        decisiones = json.loads(Path(args.decisiones).read_text(encoding="utf-8"))
        catalogo = json.loads(Path(args.catalogo).read_text(encoding="utf-8"))
        r = derivar(paginas, mapa, sin_codigo, decisiones, catalogo)
    except dc.Aborto as e:
        print("ABORTA (rc=%d): %s" % (e.rc, e), file=sys.stderr)
        return e.rc
    volcados = sorted(Path(args.volcados_profesores).glob("profesor-*.json"))
    salida = {
        "_meta": {
            "generadoPor": "tools/carga-centro/derivar-guardias.py",
            "entradas": {
                "volcadosProfesores": [entrada(str(f)) for f in volcados],
                "pdfGrupos": [entrada(p) for p in args.pdf_grupos],
                "decisiones": entrada(args.decisiones),
                "catalogo": entrada(args.catalogo),
            },
            "omitidos": r["omitidos"],
        },
        "profesores": r["profesores"],
        "noPuede": r["noPuede"],
    }
    Path(args.salida).write_text(json.dumps(salida, ensure_ascii=False, indent=1), encoding="utf-8")
    motivos = Counter(x["motivo"] for x in r["noPuede"])
    print("Escrito %s: %d profesores, %d guardias ordinarias, %d «No puede» (%s); omitidos: %s"
          % (args.salida, len(r["profesores"]), sum(p["guardiasOrdinarias"] for p in r["profesores"]),
             len(r["noPuede"]), ", ".join("%s %d" % (k, motivos[k]) for k in NO_PUEDE),
             "; ".join("p%d %s (G %d, Gbibl %d, conv %d)" % (o["pagina"], o["titulo"], o["G"], o["Gbibl"], o["conv"])
                       for o in r["omitidos"]) or "ninguno"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
