#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de cruzar-grupos-profesores.py (S197).

Sintéticos mínimos sobre las funciones del cruce, y sobre los volcados reales las cifras de la
medición de S197 (m2/p5-resumen.txt): 30 grupos, 59 profesores con código por prefijo y 3
páginas sin código (7, 25 y 32).
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

cr = cargar("cruzar_grupos_profesores", "cruzar-grupos-profesores.py")
D = RAIZ / "docs" / "horario-referencia" / "2026-2027"


def vg(titulo, *celdas):
    return {"_meta": {"codigo_crudo": titulo, "pagina": 1, "fuente": "g.pdf", "modo": "grupos"},
            "celdas": [{"dia": d, "tramo": t, "asignatura": a, "profesor": p, "aula": au,
                        "confianza": "alta", "nota": ""} for d, t, a, p, au in celdas]}


def vp(pagina, titulo, celdas, recreo=()):
    return {"_meta": {"codigo_crudo": titulo, "pagina": pagina, "fuente": "p.pdf", "modo": "profesores"},
            "celdas": [{"dia": d, "tramo": t, "asignatura": a, "grupos": g, "aula": au,
                        "confianza": "alta", "nota": ""} for d, t, a, g, au in celdas],
            "recreo": [{"dia": d, "asignatura": a, "grupos": "", "aula": au, "confianza": "alta", "nota": ""}
                       for d, a, au in recreo]}


class Sinteticos(unittest.TestCase):

    def test_el_profesor_es_parte_de_la_clave(self):
        G = cr.entradas_grupos([vg("1º ESO A", (1, 1, "Mat", "MAT1", "A1"))])
        P, _ = cr.entradas_profesores([vp(1, "Pérez López, Ana", [(1, 1, "Mat", "1ºA", "A1")])], {1: "MAT2"})
        s = cr.cruzar(G, P)
        self.assertEqual([k for k, _ in s["solo_grupos"]], [("1ºA", 1, 1, "MAT1")])
        self.assertEqual([k for k, _ in s["solo_profesores"]], [("1ºA", 1, 1, "MAT2")])
        self.assertEqual(s["comunes"], 0)

    def test_misma_clave_casa(self):
        G = cr.entradas_grupos([vg("1º ESO A", (1, 1, "Mat", "MAT1", "A1"))])
        P, _ = cr.entradas_profesores([vp(1, "X", [(1, 1, "Mat", "1ºA 1ºB", "A1")])], {1: "MAT1"})
        s = cr.cruzar(G, P)
        self.assertEqual(s["comunes"], 1)
        self.assertEqual([k for k, _ in s["solo_profesores"]], [("1ºB", 1, 1, "MAT1")])

    def test_aula_null_frente_a_aula_distinta(self):
        G = cr.entradas_grupos([vg("1º ESO A", (1, 1, "Mat", "MAT1", None), (1, 2, "Mat", "MAT1", "A1"))])
        P, _ = cr.entradas_profesores([vp(1, "X", [(1, 1, "Mat", "1ºA", "A1"), (1, 2, "Mat", "1ºA", "A2")])],
                                      {1: "MAT1"})
        s = cr.cruzar(G, P)
        self.assertEqual([k for k, _, _ in s["aula_una_vista"]], [("1ºA", 1, 1, "MAT1")])
        self.assertEqual([k for k, _, _ in s["aula_distinta"]], [("1ºA", 1, 2, "MAT1")])
        self.assertEqual(s["asignatura"], [])

    def test_asignatura_distinta(self):
        G = cr.entradas_grupos([vg("1º ESO A", (1, 1, "Mat", "MAT1", "A1"))])
        P, _ = cr.entradas_profesores([vp(1, "X", [(1, 1, "RefMt", "1ºA", "A1")])], {1: "MAT1"})
        self.assertEqual([k for k, _, _ in cr.cruzar(G, P)["asignatura"]], [("1ºA", 1, 1, "MAT1")])

    def test_profesor_sin_codigo(self):
        mapa = cr.mapa_profesores({1: "Pérez López, Ana María", 2: "Nadie Nadie, Juan"},
                                  {"MAT1": "Pérez López, Ana Ma"})
        self.assertEqual(mapa, {1: "MAT1", 2: cr.SIN_CODIGO})
        P, aparte = cr.entradas_profesores(
            [vp(1, "Pérez López, Ana María", [(1, 1, "Mat", "1ºA", "A1")]),
             vp(2, "Nadie Nadie, Juan", [(2, 2, "G", "", None)], recreo=[(3, "GR", None)])], mapa)
        self.assertEqual(list(P), [("1ºA", 1, 1, "MAT1")])
        self.assertEqual([(x["pagina"], x["celdas"], x["codigos"]) for x in aparte["sin_codigo"]],
                         [(2, 2, ["G", "GR"])])

    def test_prefijo_ambiguo_aborta(self):
        with self.assertRaises(cr.ErrorCruce):
            cr.mapa_profesores({1: "Pérez López, Ana"}, {"A": "Pérez López", "B": "Pérez"})

    def test_clave_repetida_aborta(self):
        with self.assertRaises(cr.ErrorCruce):
            cr.entradas_grupos([vg("1º ESO A", (1, 1, "Mat", "MAT1", "A1"), (1, 1, "Ing", "MAT1", "A2"))])


class VolcadosReales(unittest.TestCase):

    def test_grupos_profesores_y_paginas_sin_codigo(self):
        vgs = cr.cargar(str(D), "grupo-*.json")
        vps = cr.cargar(str(D), "profesor-*.json")
        cortos = {cr.forma_corta(v["_meta"]["codigo_crudo"]) for v in vgs}
        self.assertEqual((len(vgs), len(cortos)), (30, 30))
        leyenda = cr.leyenda_profesores([(p.name, cr.eh.leer_pdf(str(p))) for p in
                                         (D / "pdf" / "Horarios de grupos.pdf", D / "pdf" / "Horarios de grupos (11).pdf")])
        mapa = cr.mapa_profesores({v["_meta"]["pagina"]: v["_meta"]["codigo_crudo"] for v in vps}, leyenda)
        self.assertEqual(len(mapa), 62)
        self.assertEqual(sum(1 for c in mapa.values() if c != cr.SIN_CODIGO), 59)
        self.assertEqual(sorted(p for p, c in mapa.items() if c == cr.SIN_CODIGO), [7, 25, 32])
        self.assertEqual(len(set(c for c in mapa.values() if c != cr.SIN_CODIGO)), 59)


if __name__ == "__main__":
    unittest.main()
