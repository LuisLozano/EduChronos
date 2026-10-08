#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de N9 de derivar-reglas-aulas.py (S209 T2b): alumnos que contradicen la capacidad del aula oficial."""
import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402
from test_derivar_reglas_aulas import CATALOGO, HOJA1, correr, escribir_entradas  # noqa: E402

dr = cargar("derivar_reglas_aulas", "derivar-reglas-aulas.py")


def catalogo(*plazas, tipo=None):
    act = {"codigo": "ACT", "plazas": [dict(p) for p in plazas]}
    if tipo:
        act["tipo"] = tipo
    return {"actividades": [act]}


def alumnos(**n):
    return [{"subgrupo": s, "alumnos": v, "procedencia": [{"hoja": 2, "celdas": ["A%d" % i, "B%d" % i]}]}
            for i, (s, v) in enumerate(sorted(n.items()), 1)]


def capacidad(**c):
    return [{"aula": a, "capacidad": v, "procedencia": []} for a, v in sorted(c.items())]


class CoherenciaAlumnos(unittest.TestCase):

    def n9(self, cat, cap, al):
        omitidas, cap0 = [], copy.deepcopy(cap)
        quedan = dr.coherencia_alumnos(cat, cap, al, omitidas)
        self.assertEqual(cap, cap0)                     # las capacidades no se tocan
        return sorted(r["subgrupo"] for r in quedan), omitidas

    def test_supera_con_aula_fija_omite_todos_los_subgrupos_de_la_plaza(self):
        cat = catalogo({"aulaFija": "X", "subgrupos": ["s1", "s2", "s3"]})
        quedan, om = self.n9(cat, capacidad(X=20), alumnos(s1=12, s2=10, otro=5))
        self.assertEqual(quedan, ["otro"])
        self.assertEqual(sorted(o["subgrupo"] for o in om), ["s1", "s2"])

    def test_igual_a_la_capacidad_se_conserva(self):
        cat = catalogo({"aulaFija": "X", "subgrupos": ["s1", "s2"]})
        self.assertEqual(self.n9(cat, capacidad(X=22), alumnos(s1=12, s2=10)), (["s1", "s2"], []))

    def test_candidatas_todas_superadas_se_omite(self):
        cat = catalogo({"aulaFija": None, "aulasCandidatas": ["X", "Y"], "subgrupos": ["s1"]})
        quedan, om = self.n9(cat, capacidad(X=20, Y=25), alumnos(s1=26))
        self.assertEqual((quedan, [o["capacidad"] for o in om]), ([], [25]))

    def test_una_candidata_con_sitio_se_conserva(self):
        cat = catalogo({"aulasCandidatas": ["X", "Y"], "subgrupos": ["s1"]})
        self.assertEqual(self.n9(cat, capacidad(X=20, Y=30), alumnos(s1=26)), (["s1"], []))
        cat = catalogo({"aulasCandidatas": ["Y", "X"], "subgrupos": ["s1"]})
        self.assertEqual(self.n9(cat, capacidad(X=20, Y=30), alumnos(s1=26)), (["s1"], []))

    def test_aula_sin_capacidad_no_actua(self):
        cat = catalogo({"aulaFija": "Z", "subgrupos": ["s1"]})
        self.assertEqual(self.n9(cat, capacidad(X=5), alumnos(s1=26)), (["s1"], []))
        cat = catalogo({"aulasCandidatas": ["X", "Z"], "subgrupos": ["s1"]})
        self.assertEqual(self.n9(cat, capacidad(X=5), alumnos(s1=26)), (["s1"], []))

    def test_plaza_sin_aula_o_sin_alumnos_no_actua(self):
        cat = catalogo({"aulaFija": None, "aulasCandidatas": [], "subgrupos": ["s1"]})
        self.assertEqual(self.n9(cat, capacidad(X=5), alumnos(s1=26)), (["s1"], []))
        cat = catalogo({"aulaFija": "X", "subgrupos": ["sin-dato"]})
        self.assertEqual(self.n9(cat, capacidad(X=5), alumnos(s1=26)), (["s1"], []))

    def test_reunion_o_funcion_no_cuentan(self):
        cat = catalogo({"aulaFija": "X", "subgrupos": ["s1"]}, tipo="REUNION")
        self.assertEqual(self.n9(cat, capacidad(X=5), alumnos(s1=26)), (["s1"], []))

    def test_la_omision_lleva_motivo_plaza_aula_capacidad_y_suma(self):
        cat = catalogo({"aulaFija": "W", "subgrupos": []}, {"aulaFija": "X", "subgrupos": ["s1", "s2"]})
        _, om = self.n9(cat, capacidad(X=20, W=1), alumnos(s1=12, s2=10))
        self.assertEqual(om[0], {"regla": "alumnos", "origen": {"hoja": 2, "celdas": ["A1", "B1"]},
                                 "motivo": "contradice la capacidad del aula oficial (hoja 1 frente a hoja 2)",
                                 "subgrupo": "s1", "plaza": "ACT-P2", "aulas": ["X"], "capacidad": 20, "suma": 22})


class N9EnLaSalida(unittest.TestCase):

    def derivar(self, sillas_a1):
        cat = copy.deepcopy(CATALOGO)
        cat["actividades"][0]["plazas"][0]["aulaFija"] = "A1"        # ALCT-1A: 1ºA-ALCT, 8 alumnos
        hoja1 = dict(HOJA1)
        hoja1[2] = HOJA1[2][:5] + (sillas_a1,) + HOJA1[2][6:]
        with tempfile.TemporaryDirectory() as d:
            rutas = escribir_entradas(d, hoja1=hoja1, catalogo=cat)
            rc, out = correr(rutas, Path(d, "reglas.json"))
            self.assertEqual(rc, 0, out)
            return json.loads(Path(d, "reglas.json").read_text(encoding="utf-8"))

    def test_supera_omitida_con_motivo_y_capacidad_intacta(self):
        j = self.derivar("5.0")
        self.assertEqual(j["alumnos"], [])
        o = [x for x in j["omitidas"] if x["motivo"] == dr.M_N9]
        self.assertEqual([(x["subgrupo"], x["plaza"], x["aulas"], x["capacidad"], x["suma"]) for x in o],
                         [("1ºA-ALCT", "ALCT-1A-P1", ["A1"], 5, 8)])
        self.assertIn({"aula": "A1", "capacidad": 5, "procedencia": [{"hoja": 1, "celdas": ["E2", "D2", "C2"],
                                                                        "casacion": "EXACTA"}]}, j["capacidad"])

    def test_cabe_y_se_conserva(self):
        j = self.derivar("8.0")
        self.assertEqual([(r["subgrupo"], r["alumnos"]) for r in j["alumnos"]], [("1ºA-ALCT", 8)])
        self.assertFalse([x for x in j["omitidas"] if x["motivo"] == dr.M_N9])


if __name__ == "__main__":
    unittest.main()
