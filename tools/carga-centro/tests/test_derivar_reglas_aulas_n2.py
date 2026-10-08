#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de N2'' de derivar-reglas-aulas.py (S209 T2c): una fila que casa con varios grupos se omite entera."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402
from test_derivar_reglas_aulas import AMARILLO, NARANJA, correr, escribir_entradas  # noqa: E402

dr = cargar("derivar_reglas_aulas", "derivar-reglas-aulas.py")


def derivar(hoja1):
    with tempfile.TemporaryDirectory() as d:
        rutas = escribir_entradas(d, hoja1=hoja1, notas={})
        rc, out = correr(rutas, Path(d, "reglas.json"))
        assert rc == 0, out
        return json.loads(Path(d, "reglas.json").read_text(encoding="utf-8"))


class FilaConVariosGrupos(unittest.TestCase):

    def test_fila_con_un_grupo_da_la_regla(self):
        j = derivar({2: ("A1", "1ºESO A", "", "", "", "30.0", AMARILLO)})
        self.assertEqual([(r["grupo"], r["aula"]) for r in j["aulaReferencia"]], [("1ºA", "A1")])
        self.assertFalse([o for o in j["omitidas"] if o["regla"] == "aulaReferencia"])

    def test_fila_con_dos_grupos_se_omite_entera(self):
        j = derivar({2: ("Taller3 A", "1ºBCH A", "", "", "", "30.0", NARANJA),
                     3: ("A1", "1ºESO A", "", "", "", "30.0", AMARILLO)})
        self.assertEqual([(r["grupo"], r["aula"]) for r in j["aulaReferencia"]], [("1ºA", "A1")])
        o = [x for x in j["omitidas"] if x["regla"] == "aulaReferencia"]
        self.assertEqual([(x["origen"]["celdas"], x["motivo"], x["grupos"]) for x in o],
                         [(["E2", "F2"], "la fila casa con varios grupos de la base", ["1B-Ac", "1B-Am"])])
        self.assertEqual(dr.M_VARIOS_GRUPOS, "la fila casa con varios grupos de la base")

    def test_la_fila_pdc_conserva_su_motivo(self):
        j = derivar({2: ("B4(Ant. Empr)", "3ºESO DIVER", "", "", "", "", AMARILLO)})
        o = [x for x in j["omitidas"] if x["regla"] == "aulaReferencia"]
        self.assertEqual([x["motivo"] for x in o], ["el PDC no admite aula de referencia; cuenta como su grupo padre"])


if __name__ == "__main__":
    unittest.main()
