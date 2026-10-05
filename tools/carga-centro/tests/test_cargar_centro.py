#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de cargar-centro.py (S199): rutas por argumento y escrituras previstas.

Los esperados de escrituras son independientes de la función: 816 es la cifra que S135 y S137
escribieron al cargar desde vacío (bitácora, «816/816 escrituras») y 852 la de
`_meta.enviosDeFormulario.TOTAL` del catálogo de 2026/2027, que escribe derivar-catalogo.py.
"""
import contextlib
import io
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

cc = cargar("cargar_centro", "cargar-centro.py")

HR = RAIZ / "docs" / "horario-referencia"


def leer(ruta):
    return json.loads(ruta.read_text(encoding="utf-8"))


class EscriturasPrevistas(unittest.TestCase):

    def test_816_con_el_catalogo_de_2025(self):
        self.assertEqual(cc.escrituras_previstas(leer(HR / "catalogo-derivado.json")), 816)

    def test_852_con_el_catalogo_de_2026(self):
        self.assertEqual(cc.escrituras_previstas(leer(HR / "2026-2027" / "catalogo-derivado.json")), 852)


class Argumentos(unittest.TestCase):

    def _sale_con_error(self, argv):
        with contextlib.redirect_stderr(io.StringIO()), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as cm:
                cc.main(argv)
        self.assertNotEqual(cm.exception.code, 0)

    def test_sin_catalogo_es_error_de_argparse(self):
        self._sale_con_error(["--nombres", str(HR / "nombres-derivados.json")])

    def test_sin_nombres_es_error_de_argparse(self):
        self._sale_con_error(["--catalogo", str(HR / "catalogo-derivado.json")])


if __name__ == "__main__":
    unittest.main()
