#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de comparar-con-oficial.py (S228): oráculos a mano de las métricas del contrato
docs/horario-referencia/2026-2027/METRICAS-COMPARACION.md y de la normalización de P1.

    python3 -B -m unittest discover -s tools/carga-centro/tests -v
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402

cco = cargar("comparar_con_oficial", "comparar-con-oficial.py")


def t(grupo, dia, tramo, asig="Mat", prof="MAT1"):
    return (grupo, dia, tramo, asig, prof)


class M1Huecos(unittest.TestCase):

    def test_clase_en_los_tramos_1_y_4_del_mismo_dia_da_2(self):
        r = cco.m1({t("1A", 1, 1), t("1B", 1, 4)})
        self.assertEqual((r["total"], r["max_profesor_semana"], r["pares_3_o_mas"]), (2, 2, 0))

    def test_tres_huecos_cuentan_como_par_de_3_o_mas(self):
        self.assertEqual(cco.m1({t("1A", 2, 1), t("1A", 2, 5)})["pares_3_o_mas"], 1)


class M2Repetida(unittest.TestCase):

    def test_4_franjas_en_4_dias_con_2_profesores_dan_0(self):
        tuplas = {t("1A", d, 2, prof=p) for d in (1, 2, 3, 4) for p in ("LEN4", "LEN8")}
        r = cco.m2(tuplas, {})
        self.assertEqual((r["total"], r["total_minimo"]), (0, 0))

    def test_3_franjas_en_2_dias_dan_1(self):
        r = cco.m2({t("1A", 1, 1), t("1A", 1, 2), t("1A", 2, 1)}, {("1A", "Mat"): "NEUTRA"})
        self.assertEqual((r["total"], r["NEUTRA"], r["AGRUPADA"]), (1, 1, 0))

    def test_6_franjas_dan_minimo_forzado_1(self):
        tuplas = {t("1A", d, 1) for d in (1, 2, 3, 4, 5)} | {t("1A", 1, 2)}
        r = cco.m2(tuplas, {("1A", "Mat"): "DISTRIBUIDA"})
        self.assertEqual((r["total"], r["total_minimo"], r["DISTRIBUIDA_minimo"]), (1, 1, 1))


class M3Dias(unittest.TestCase):

    def test_suma_de_dias_y_profesores_con_los_5(self):
        tuplas = {t("1A", d, 1) for d in (1, 2, 3, 4, 5)} | {t("1A", d, 2, prof="ING1") for d in (1, 3)}
        self.assertEqual(cco.m3(tuplas), {"suma_dias": 7, "profesores_5_dias": 1})


class M4Guardias(unittest.TestCase):

    def test_guardias_2_0_dan_4(self):
        r = cco.m4([("GH1", 1, 1), ("GH2", 1, 1)], [])
        self.assertEqual((r["a"], r["b"]), (4, 0))

    def test_recreo_uno_por_profesor_y_dia(self):
        r = cco.m4([], [("GH1", 1), ("GH1", 1), ("GH2", 1), ("GH3", 2)])
        self.assertEqual(r["b"], 2 * 2 + 1 * 1)


class M5Aulas(unittest.TestCase):

    def test_a_a_b_da_1(self):
        self.assertEqual(cco.m5([("1A", 1, "A"), ("1A", 1, "A"), ("1A", 1, "B")])["suma"], 1)

    def test_a_null_b_da_1_y_cuenta_la_nula(self):
        r = cco.m5([("1A", 1, "A"), ("1A", 1, None), ("1A", 1, "B")])
        self.assertEqual((r["suma"], r["sin_aula"]), (1, 1))

    def test_solo_null_da_0(self):
        self.assertEqual(cco.m5([("1A", 1, None)])["suma"], 0)


class M6Coincidencia(unittest.TestCase):

    def test_2_de_4_coinciden_dan_50(self):
        oficial = {t("1A", 1, 1), t("1A", 1, 2), t("1A", 2, 1), t("1A", 2, 2)}
        horario = {t("1A", 1, 1), t("1A", 1, 2), t("1A", 3, 1), t("1A", 3, 2)}
        self.assertEqual(cco.m6(oficial, horario), 50.0)


class P1Normalizacion(unittest.TestCase):

    FILAS = [("LUNES", o, o != 4) for o in range(1, 8)] + [("MARTES", o, o != 4) for o in range(1, 8)]

    def test_orden_5_con_recreo_en_el_4_da_tramo_4(self):
        pos = cco.posiciones_lectivas(self.FILAS)
        self.assertEqual(cco.normaliza_dia_tramo("LUNES", 5, pos), (1, 4))
        self.assertEqual(cco.normaliza_dia_tramo("MARTES", 7, pos), (2, 6))

    def test_ya_normalizado_no_se_normaliza_dos_veces(self):
        self.assertEqual(cco.normaliza_dia_tramo(1, 5, {}), (1, 5))

    def test_tramo_no_lectivo_aborta(self):
        with self.assertRaises(SystemExit):
            cco.normaliza_dia_tramo("LUNES", 4, cco.posiciones_lectivas(self.FILAS))

    def test_fuera_de_rango_aborta(self):
        with self.assertRaises(SystemExit):
            cco.comprueba_rango({t("1A", 1, 7)}, "prueba")


class P2Multiconjunto(unittest.TestCase):

    def test_igual_pasa_y_distinto_aborta(self):
        oficial = {t("1A", 1, 1), t("1A", 2, 1)}
        cco.comprueba_p2(oficial, {t("1A", 3, 3), t("1A", 4, 1)}, "igual")
        with self.assertRaises(SystemExit):
            cco.comprueba_p2(oficial, {t("1A", 3, 3)}, "distinto")


if __name__ == "__main__":
    unittest.main()
