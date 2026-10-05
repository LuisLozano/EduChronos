#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Parche de aulas: lo que el retoque en Word de la vista de profesores (P20) cambia respecto a
la impresión del programa de horarios (P07).

Nace en S197 (C-volcado-profesores). REFERENCIA.md de 2026/2027 fija que los retoques hechos en
Word a partir del 28/09 solo cambian aulas y se tratan como un parche, no como fuente. Este
script lo comprueba y lo escribe.

USO
    python3 tools/carga-centro/parche-aulas.py --antes <P07.pdf> --despues <P20.pdf> \\
        --sesion S197 --fecha AAAA-MM-DD --salida <parche-aulas.json>

MÉTODO
  - Mismo número de páginas y mismos títulos «Profesor: ...» en el mismo orden, o aborta.
  - Cada PDF con su propia geometría (columnas y tramos de extraer-horario.py): Word desplaza
    las x unas décimas.
  - Bandas: SOLO las de la referencia (P07), tomadas del umbral que devuelve
    extraer-horario.procesar() en modo profesores, y aplicadas a los dos PDF. El parche NO
    deriva bandas del retocado: P20 es P07 editado en Word y la edición desplaza justo las
    palabras que cambia. Caso medido en S197 (f2/paso5-medida-bandas-P20.txt): «A4» en p57,
    miércoles, x relativa 48,83 frente a la banda derecha de 38,0-42,0 de P20 (40,19-40,33 en
    P07); derivadas de P20, las bandas salen tres y no dos.
  - Cada palabra de rejilla va a su RANURA (página, día, tramo o recreo, línea dentro de la
    celda, banda izquierda/derecha) y se compara ranura a ranura. No se lee la leyenda: en P20
    las columnas de la leyenda no alinean (columnas_de_codigo falla).
  - Única diferencia admitida: la palabra de la banda derecha de la línea 1 cambia y empieza por
    letra en los dos lados (cambio de aula). Cualquier otra diferencia de rejilla aborta con la
    lista. Lo de fuera de la rejilla (título, cabeceras, horas, leyenda) se ignora y se cuenta.

rc=0 con el parche escrito; rc=2 ante cualquier diferencia no admitida o error de estructura.
"""
import argparse
import difflib
import hashlib
import importlib.util
import json
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("extraer_horario", os.path.join(AQUI, "extraer-horario.py"))
eh = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(eh)


class ErrorParche(Exception):
    """Diferencia no admitida o estructura no reconocida: aborta con rc=2."""


def paginas(pags):
    """Analiza las páginas de un PDF de profesores (geometría propia de cada PDF)."""
    try:
        return [eh.analizar_pagina_profesor(p) for p in pags]
    except eh.ErrorExtraccion as e:
        raise ErrorParche(str(e))


def umbral_de_referencia(pags):
    """Umbral de bandas de la referencia, el que deriva el extractor al procesarla."""
    try:
        res = eh.procesar([("referencia", pags)], "profesores")
    except eh.ErrorExtraccion as e:
        raise ErrorParche("referencia: %s" % e)
    umbrales = {g["umbral"] for g in res["grupos"]}
    if len(umbrales) != 1:
        raise ErrorParche("referencia: umbrales %s" % sorted(umbrales))
    return umbrales.pop()


def ranuras(n, a, umbral):
    """Palabras de rejilla de la página n -> {(n, día, tramo|None, línea, banda): [textos]}."""
    res = {}
    if a["tipo"] != "rejilla":
        return res
    por_celda = {}
    for w in a["rejilla"]:
        dia = eh.columna_de(w, a["limites"])
        dentro, tramo = eh.tramo_de(w, a["filas"])
        if dia is None or not dentro:
            raise ErrorParche("p%d: %r fuera de la rejilla (x0=%.2f top=%.2f)" % (n, w["text"], w["x0"], w["top"]))
        por_celda.setdefault((dia, tramo), []).append(w)
    for (dia, tramo), ws in por_celda.items():
        borde = a["limites"][dia - 1][0]
        for i, l in enumerate(eh.lineas(ws), start=1):
            for w in l:
                banda = "izq" if w["x0"] - borde < umbral else "der"
                res.setdefault((n, dia, tramo, i, banda), []).append(w["text"])
    return res


def fuera_de_rejilla(pa, pb, aa, ab):
    """Palabras distintas fuera de la rejilla (orden de lectura), por página."""
    n = 0
    for p, q, x, y in zip(pa, pb, aa, ab):
        ida = {w["id"] for w in x.get("rejilla", [])}
        idb = {w["id"] for w in y.get("rejilla", [])}
        sa = [w["text"] for l in eh.lineas([w for w in p if w["id"] not in ida]) for w in l]
        sb = [w["text"] for l in eh.lineas([w for w in q if w["id"] not in idb]) for w in l]
        for op, i1, i2, j1, j2 in difflib.SequenceMatcher(a=sa, b=sb, autojunk=False).get_opcodes():
            if op != "equal":
                n += max(i2 - i1, j2 - j1)
    return n


def es_cambio_de_aula(clave, antes, despues):
    _, _, _, linea, banda = clave
    return (linea == 1 and banda == "der" and len(antes) == 1 and len(despues) == 1
            and antes[0][:1].isalpha() and despues[0][:1].isalpha())


def comparar(pags_antes, pags_despues):
    """-> (entradas del parche, nº de palabras distintas fuera de rejilla, umbral). Aborta ante cualquier
    diferencia de rejilla que no sea un cambio de aula."""
    if len(pags_antes) != len(pags_despues):
        raise ErrorParche("nº de páginas distinto: %d frente a %d" % (len(pags_antes), len(pags_despues)))
    umbral = umbral_de_referencia(pags_antes)
    aa = paginas(pags_antes)
    ab = paginas(pags_despues)
    distintos = [(n, x["titulo"], y["titulo"]) for n, (x, y) in enumerate(zip(aa, ab), start=1)
                 if x["titulo"] != y["titulo"]]
    if distintos:
        raise ErrorParche("títulos distintos: %s" % distintos)
    ra, rb = {}, {}
    for n, (x, y) in enumerate(zip(aa, ab), start=1):
        ra.update(ranuras(n, x, umbral))
        rb.update(ranuras(n, y, umbral))
    cambios, errores = [], []
    for clave in sorted(set(ra) | set(rb), key=lambda k: (k[0], k[1], k[2] or 0, k[3], k[4])):
        antes, despues = ra.get(clave, []), rb.get(clave, [])
        if antes == despues:
            continue
        if es_cambio_de_aula(clave, antes, despues):
            cambios.append((clave, antes[0], despues[0]))
        else:
            errores.append((clave, antes, despues))
    if errores:
        raise ErrorParche("diferencias de rejilla no admitidas (%d): %s" % (len(errores), errores))
    entradas = []
    for (n, dia, tramo, _, _), antes, despues in cambios:
        a = aa[n - 1]
        try:
            celdas = eh.clasificar_profesor(a["rejilla"], a["limites"], a["filas"], umbral, None)
        except eh.ErrorExtraccion as e:
            raise ErrorParche(str(e))
        c = next(c for c in celdas if (c["dia"], c["tramo"]) == (dia, tramo))
        entradas.append({"profesor": a["titulo"], "pagina": n, "dia": dia, "tramo": tramo,
                         "asignatura": c["asignatura"]["text"], "grupos": " ".join(w["text"] for w in c["grupos"]),
                         "aula_antes": antes, "aula_despues": despues, "confirmacion": "pendiente"})
    return entradas, fuera_de_rejilla(pags_antes, pags_despues, aa, ab), umbral


def sha256(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as f:
        for b in iter(lambda: f.read(1 << 16), b""):
            h.update(b)
    return h.hexdigest()


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--antes", required=True)
    ap.add_argument("--despues", required=True)
    ap.add_argument("--sesion", required=True)
    ap.add_argument("--fecha", required=True)
    ap.add_argument("--salida", required=True)
    a = ap.parse_args(argv)
    if os.path.exists(a.salida):
        print("ERROR: --salida ya existe: %s" % a.salida, file=sys.stderr)
        return 2
    try:
        entradas, fuera, umbral = comparar(eh.leer_pdf(a.antes), eh.leer_pdf(a.despues))
    except (ErrorParche, eh.ErrorExtraccion) as e:
        print("ERROR: %s" % e, file=sys.stderr)
        return 2
    salida = {
        "_meta": {"antes": {"fichero": os.path.basename(a.antes), "sha256": sha256(a.antes)},
                  "despues": {"fichero": os.path.basename(a.despues), "sha256": sha256(a.despues)},
                  "fuera_de_rejilla_ignoradas": fuera, "sesion": a.sesion, "fecha": a.fecha},
        "entradas": entradas,
    }
    with open(a.salida, "w", encoding="utf-8") as f:
        json.dump(salida, f, ensure_ascii=False, indent=2)
    print("umbral de bandas (referencia)=%.2f entradas=%d fuera de rejilla ignoradas=%d -> %s"
          % (umbral, len(entradas), fuera, a.salida))
    return 0


if __name__ == "__main__":
    sys.exit(main())
