#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de parche-aulas.py (S197).

Sintéticos con páginas fabricadas (paginas_sinteticas.py), y el parche-aulas.json versionado
contra la tabla independiente de S197 (m2/p6-diff.tsv: diff de palabras de pdftotext entre P07
y P20). Ningún test lee docs_extra/.
"""
import copy
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar, pagina, LEYENDA, IZQ, DER  # noqa: E402

pa = cargar("parche_aulas", "parche-aulas.py")

BASE = {
    (1, 1): [[("Mat", IZQ), ("A1", DER)], [("1ºA", IZQ)]],
    (2, 2): [[("Ing", IZQ), ("B12", DER)], [("2ºA", IZQ), ("2ºB", DER)]],
    (3, None): [[("GR", IZQ)]],
}


def doc(celdas=BASE, titulo="Prueba Uno", leyenda=LEYENDA):
    return [pagina(1, titulo, celdas, leyenda)]


def con(cambios):
    c = copy.deepcopy(BASE)
    c.update(cambios)
    return c


class Sinteticos(unittest.TestCase):

    def test_sin_cambios(self):
        entradas, fuera, _ = pa.comparar(doc(), doc())
        self.assertEqual((entradas, fuera), ([], 0))

    def test_un_cambio_de_aula_es_una_entrada(self):
        entradas, _, _ = pa.comparar(doc(), doc(con({(2, 2): [[("Ing", IZQ), ("A12", DER)],
                                                               [("2ºA", IZQ), ("2ºB", DER)]]})))
        self.assertEqual(entradas, [{"profesor": "Prueba Uno", "pagina": 1, "dia": 2, "tramo": 2,
                                     "asignatura": "Ing", "grupos": "2ºA 2ºB", "aula_antes": "B12",
                                     "aula_despues": "A12", "confirmacion": "pendiente"}])

    def test_aula_desplazada_fuera_de_la_banda_de_la_referencia(self):
        # Como «A4» en p57 de P20: x relativa 48,8, a la derecha del umbral pero fuera de la banda
        # derecha de la referencia (~40,3). Con las bandas de la referencia es un cambio de aula.
        entradas, _, umbral = pa.comparar(doc(), doc(con({(2, 2): [[("Ing", IZQ), ("A4", 48.8)],
                                                                    [("2ºA", IZQ), ("2ºB", DER)]]})))
        self.assertAlmostEqual(umbral, (IZQ + DER) / 2, places=6)
        self.assertEqual([(e["aula_antes"], e["aula_despues"]) for e in entradas], [("B12", "A4")])

    def test_cambio_de_asignatura_aborta(self):
        with self.assertRaises(pa.ErrorParche):
            pa.comparar(doc(), doc(con({(1, 1): [[("Ing", IZQ), ("A1", DER)], [("1ºA", IZQ)]]})))

    def test_cambio_de_grupo_aborta(self):
        with self.assertRaises(pa.ErrorParche):
            pa.comparar(doc(), doc(con({(1, 1): [[("Mat", IZQ), ("A1", DER)], [("1ºB", IZQ)]]})))

    def test_celda_movida_con_la_misma_secuencia_aborta(self):
        # La celda del lunes a 1.ª pasa al martes a 1.ª: el orden de lectura de las palabras no
        # cambia, las ranuras sí.
        c = copy.deepcopy(BASE)
        c[(2, 1)] = c.pop((1, 1))
        with self.assertRaises(pa.ErrorParche):
            pa.comparar(doc(), doc(c))

    def test_etiqueta_de_hora_reordenada_fuera_de_rejilla_se_ignora(self):
        b = doc()
        w = next(w for w in b[0] if w["text"] == "14:30")
        b[0].remove(w)
        w["top"] += 0.6
        b[0].insert(0, w)
        self.assertNotEqual([x["text"] for x in doc()[0]], [x["text"] for x in b[0]])  # precondición
        entradas, fuera, _ = pa.comparar(doc(), b)
        self.assertEqual((entradas, fuera), ([], 0))

    def test_texto_distinto_fuera_de_rejilla_se_cuenta(self):
        ley = [("Mat", "Matemática")] + LEYENDA[1:]
        entradas, fuera, _ = pa.comparar(doc(), doc(leyenda=ley))
        self.assertEqual((entradas, fuera), ([], 1))

    def test_titulos_distintos_abortan(self):
        with self.assertRaises(pa.ErrorParche):
            pa.comparar(doc(), doc(titulo="Prueba Dos"))


class ParcheVersionado(unittest.TestCase):
    """parche-aulas.json frente a m2/p6-diff.tsv (S197): 18 cambios."""

    ESPERADO = sorted(
        [(2, d, t, "B12", "A12") for d, t in [(1, 1), (5, 1), (3, 2), (5, 2), (3, 3), (4, 3), (5, 3),
                                               (2, 4), (3, 4), (5, 4), (1, 5), (3, 5)]]
        + [(4, 2, 3, "B12", "A12"), (4, 4, 5, "B12", "A12")]
        + [(57, 1, 2, "TALL1", "A4"), (57, 3, 2, "TALL1", "A4"), (57, 1, 3, "TALL1", "A4"),
           (57, 2, 6, "TALL1", "A5")])

    def test_dieciocho_cambios(self):
        with open(RAIZ / "docs" / "horario-referencia" / "2026-2027" / "parche-aulas.json", encoding="utf-8") as f:
            p = json.load(f)
        self.assertEqual(sorted((e["pagina"], e["dia"], e["tramo"], e["aula_antes"], e["aula_despues"])
                                for e in p["entradas"]), self.ESPERADO)
        self.assertEqual({e["confirmacion"] for e in p["entradas"]}, {"pendiente"})
        self.assertEqual(p["_meta"]["antes"]["sha256"][:12], "5569ba866089")
        self.assertEqual(p["_meta"]["despues"]["sha256"][:12], "4f5a62eed0e5")


if __name__ == "__main__":
    unittest.main()
