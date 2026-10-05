#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de codigos_grupo.py (S197).

Esperados independientes de la regla: las 24 formas cortas de los títulos con regla de
REGLAS_CODIGO son el token de P07 con el que cada título casa por datos (Jaccard 1,0 sobre
(profesor, día, tramo), m2bis/q4-bach.tsv, sección «grupos con REGLAS_CODIGO»); las seis de
1ºBACH, las de la matriz 6×6 de m2bis/q4-bach.tsv.
"""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

cg = cargar("codigos_grupo", "codigos_grupo.py")

ESPERADO = {
    "1º ESO A": "1ºA", "1º ESO B": "1ºB", "1º ESO C": "1ºC", "1º ESO D": "1ºD",
    "1º FPB": "1FPB", "2º FPB": "2FPB",
    "2º ESO A": "2ºA", "2º ESO B": "2ºB", "2º ESO C": "2ºC", "2º ESO D": "2ºD",
    "2ºBACH A": "2B-A", "2ºBACH B": "2B-B", "2ºBACH C": "2B-C",
    "3º ESO A": "3ºA", "3º ESO B": "3ºB", "3º ESO C": "3ºC",
    "3º ESO A PDC": "3ºADi", "3º ESO B PDC": "3ºBDi", "3º ESO PDC": "3ºCDi",
    "4º ESO A": "4ºA", "4º ESO B": "4ºB", "4º ESO C": "4ºC",
    "4º ESO B PDC": "4ºBDi", "4º ESO C PDC": "4ºCDi",
    # m2bis/q4-bach.tsv
    "1ºBACH A Ciencias": "1B-Ac", "1ºBACH A Mixto": "1B-Am",
    "1ºBACH B Ciencias": "1B-Bc", "1ºBACH B Mixto": "1B-Bm",
    "1ºBACH C Humanidades": "1B-Ch", "1ºBACH C Sociales": "1B-Cs",
}


class FormaCorta(unittest.TestCase):

    def test_los_30_titulos_del_volcado(self):
        titulos = []
        for f in sorted((RAIZ / "docs" / "horario-referencia" / "2026-2027").glob("grupo-*.json")):
            with open(f, encoding="utf-8") as fh:
                titulos.append(json.load(fh)["_meta"]["codigo_crudo"])
        self.assertEqual(sorted(titulos), sorted(ESPERADO))
        self.assertEqual({t: cg.forma_corta(t) for t in titulos}, ESPERADO)

    def test_las_cinco_reglas_son_las_de_verificar_conservacion(self):
        fuente = (RAIZ / "tools" / "carga-centro" / "verificar-conservacion.py").read_text(encoding="utf-8").splitlines()
        propio = (RAIZ / "tools" / "carga-centro" / "codigos_grupo.py").read_text(encoding="utf-8").splitlines()

        def bloque(lineas):
            i = lineas.index("REGLAS_CODIGO = [")
            return lineas[i:lineas.index("]", i) + 1]
        self.assertEqual(bloque(propio), bloque(fuente))

    def test_sin_regla_aborta(self):
        for t in ("1ºBACH E", "1º ESO E", "2ºBACH A Ciencias", "1ºBACH A ciencias", ""):
            with self.assertRaises(cg.SinFormaCorta, msg=t):
                cg.forma_corta(t)


if __name__ == "__main__":
    unittest.main()
