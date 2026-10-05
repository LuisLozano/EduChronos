#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests del modo profesores de extraer-horario.py (S197, C-volcado-profesores).

    python3 -B -m unittest discover -s tools/carga-centro/tests -v

Fixture versionado: la copia de P07 en docs/horario-referencia/2026-2027/pdf/. Ningún test
lee docs_extra/. Los esperados salen de la medición de S197, independiente del extractor:
las celdas, del volcado de palabras de pdftotext -bbox clasificadas por vocabulario
(m2/p4-celdas.tsv); las cifras globales, de m2/p4-resumen.txt y m2bis/q1-resumen.txt.
"""
import contextlib
import copy
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar, pagina, LEYENDA, IZQ, DER  # noqa: E402

eh = cargar("extraer_horario", "extraer-horario.py")
P07 = RAIZ / "docs" / "horario-referencia" / "2026-2027" / "pdf" / "Horarios de profesores (6).pdf"

_cache = {}


def leer():
    if "p07" not in _cache:
        _cache["p07"] = eh.leer_pdf(str(P07))
    return copy.deepcopy(_cache["p07"])


def resultado():
    if "res" not in _cache:
        _cache["res"] = eh.procesar([(P07.name, leer())], "profesores")
    return _cache["res"]


def volcado_de_pagina(n):
    g = [g for g in resultado()["grupos"] if g["pagina"] == n]
    if len(g) != 1:
        raise AssertionError("página %d: %d grupos" % (n, len(g)))
    return eh.volcado(g[0], "profesores")


def celda(v, dia, tramo):
    c = [c for c in v["celdas"] if (c["dia"], c["tramo"]) == (dia, tramo)]
    if len(c) != 1:
        raise AssertionError("(%d, %d): %d celdas" % (dia, tramo, len(c)))
    return (c[0]["asignatura"], c[0]["grupos"], c[0]["aula"])


class P07Paginas(unittest.TestCase):
    """Celdas conocidas (m2/p4-celdas.tsv)."""

    def test_dos_grupos(self):
        # p1, lunes, tramo 3: «BIOL COM4 | 2B-A 2B-B»
        self.assertEqual(celda(volcado_de_pagina(1), 1, 3), ("BIOL", "2B-A 2B-B", "COM4"))

    def test_bachillerato_1b(self):
        # p1, jueves, tramo 4: «ANAT A6 | 1B-Ac 1B-Am | 1B-Bc 1B-Bm | 1B-Ch 1B-Cs»
        self.assertEqual(celda(volcado_de_pagina(1), 4, 4),
                         ("ANAT", "1B-Ac 1B-Am 1B-Bc 1B-Bm 1B-Ch 1B-Cs", "A6"))

    def test_recreo(self):
        v = volcado_de_pagina(1)  # GR lunes, martes y jueves, solo
        self.assertEqual([(c["dia"], c["asignatura"], c["grupos"], c["aula"]) for c in v["recreo"]],
                         [(1, "GR", "", None), (2, "GR", "", None), (4, "GR", "", None)])
        self.assertNotIn("tramo", v["recreo"][0])
        v = volcado_de_pagina(9)  # «GRBib GrecB» martes y viernes
        self.assertEqual([(c["dia"], c["asignatura"], c["grupos"], c["aula"]) for c in v["recreo"]],
                         [(2, "GRBib", "", "GrecB"), (5, "GRBib", "", "GrecB")])

    def test_grupo_sin_aula(self):
        # p6, lunes, tramo 2: «LCL 1ºA» en una sola línea
        self.assertEqual(celda(volcado_de_pagina(6), 1, 2), ("LCL", "1ºA", None))

    def test_pagina_vacia(self):
        v = volcado_de_pagina(32)
        self.assertEqual((v["celdas"], v["recreo"]), ([], []))
        self.assertEqual(v["_meta"]["modo"], "profesores")


class P07Cuadre(unittest.TestCase):
    """Cifras globales de S197 (m2/p4-resumen.txt, m2bis/q1-resumen.txt)."""

    def test_cuadre_global(self):
        res = resultado()
        self.assertEqual(eh.codigo_salida(res), 0)
        self.assertEqual(len(res["grupos"]), 62)
        self.assertEqual(sum(len(g["rejilla"]) for g in res["grupos"]), 3269)
        self.assertEqual(sum(len(g["perdidos"]) + len(g["duplicados"]) for g in res["grupos"]), 0)
        with tempfile.TemporaryDirectory() as tmp:
            salida = os.path.join(tmp, "volcado")
            out = io.StringIO()
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
                rc = eh.main(["--modo", "profesores", "--salida", salida, str(P07)])
            self.assertEqual(rc, 0, out.getvalue())
            nombres = sorted(os.listdir(salida))
            self.assertEqual(len(nombres), 62)
            self.assertTrue(all(n.startswith("profesor-") for n in nombres))
            vs = []
            for n in nombres:
                with open(os.path.join(salida, n), encoding="utf-8") as f:
                    vs.append(json.load(f))
        cel = [c for v in vs for c in v["celdas"]]
        rec = [c for v in vs for c in v["recreo"]]
        self.assertEqual((len(cel), len(rec)), (1009, 38))
        self.assertEqual((sum(len(c["grupos"].split()) for c in cel), sum(len(c["grupos"].split()) for c in rec)),
                         (1421, 0))
        self.assertEqual((sum(c["aula"] is not None for c in cel), sum(c["aula"] is not None for c in rec)),
                         (796, 5))


def doc(celdas, leyenda=LEYENDA):
    return [("sintetico.pdf", [pagina(1, "Prueba Uno", celdas, leyenda)])]


class Sinteticos(unittest.TestCase):

    def test_un_caso_valido(self):
        res = eh.procesar(doc({(1, 1): [[("Mat", IZQ), ("A1", DER)], [("1ºA", IZQ), ("1ºB", DER)]]}),
                          "profesores")
        v = eh.volcado(res["grupos"][0], "profesores")
        self.assertEqual(v["celdas"][0]["grupos"], "1ºA 1ºB")
        self.assertEqual(v["celdas"][0]["aula"], "A1")

    def test_umbral_derivado_por_documento(self):
        # Bandas en 5,8 y 15: el umbral sale de este documento (10,4), no de P07 (~23).
        res = eh.procesar(doc({(1, 1): [[("Mat", IZQ), ("A1", 15.0)], [("1ºA", IZQ)]],
                               (2, 2): [[("Ing", IZQ), ("B2", 15.0)]]}), "profesores")
        self.assertAlmostEqual(res["grupos"][0]["umbral"], (IZQ + 15.0) / 2, places=6)
        v = eh.volcado(res["grupos"][0], "profesores")
        self.assertEqual([(c["asignatura"], c["aula"]) for c in v["celdas"]], [("Mat", "A1"), ("Ing", "B2")])

    def test_tercera_banda_aborta(self):
        with self.assertRaises(eh.ErrorExtraccion):
            eh.procesar(doc({(1, 1): [[("Mat", IZQ), ("A1", DER)]], (2, 1): [[("Ing", IZQ), ("A4", 55.0)]]}),
                        "profesores")
        with self.assertRaises(eh.ErrorExtraccion):
            eh.umbral_de_bandas([5.7, 5.9, 40.2, 40.3, 48.8])

    def test_asignatura_fuera_de_la_leyenda_aborta(self):
        with self.assertRaises(eh.ErrorExtraccion):
            eh.procesar(doc({(1, 1): [[("Mat", IZQ), ("A1", DER)]], (2, 1): [[("Zzz", IZQ), ("A1", DER)]]}),
                        "profesores")

    def test_grupo_sin_digito_en_linea_2_aborta(self):
        with self.assertRaises(eh.ErrorExtraccion):
            eh.procesar(doc({(1, 1): [[("Mat", IZQ), ("A1", DER)], [("A99", IZQ)]]}), "profesores")


if __name__ == "__main__":
    unittest.main()
