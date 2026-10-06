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

    def test_875_con_el_catalogo_de_2026(self):
        # S204: 852 + 9 asignaturas, 1 profesor (PROV1) y 13 actividades de reuniones y funciones.
        self.assertEqual(cc.escrituras_previstas(leer(HR / "2026-2027" / "catalogo-derivado.json")), 875)


class PrevalidarTipo(unittest.TestCase):
    """S204 (D9 en prevalidar): el XOR de aula solo para CLASE y el tipo dentro del enum."""

    @classmethod
    def setUpClass(cls):
        cls.cat = leer(HR / "2026-2027" / "catalogo-derivado.json")
        cls.nombres = {"asignaturas": {a["codigo"]: {} for a in cls.cat["asignaturas"]},
                       "profesores": {p["codigo"]: {} for p in cls.cat["profesores"]}}

    def con_actividad(self, **cambios):
        cat = json.loads(json.dumps(self.cat))
        a = next(x for x in cat["actividades"] if x["codigo"] == "AMO-1FPB")
        a["plazas"][0].update(aulaFija=None, aulasCandidatas=[])
        a.update(cambios)
        return cc.prevalidar(cat, self.nombres)

    def test_sin_violaciones_el_catalogo_versionado(self):
        self.assertEqual(cc.prevalidar(self.cat, self.nombres), [])

    def test_clase_sin_aula_es_violacion(self):
        v = self.con_actividad()
        self.assertEqual([(f, s) for f, s, _ in v], [("plazas", "AMO-1FPB plaza 1")])
        self.assertIn("XOR de aula", v[0][2])
        self.assertEqual(len(self.con_actividad(tipo="CLASE")), 1)

    def test_reunion_o_funcion_sin_aula_no_es_violacion(self):
        self.assertEqual(self.con_actividad(tipo="REUNION"), [])
        self.assertEqual(self.con_actividad(tipo="FUNCION"), [])

    def test_tipo_fuera_del_enum_es_violacion(self):
        v = self.con_actividad(tipo="GUARDIA")
        self.assertIn(("actividades", "AMO-1FPB", "tipo fuera del enum: 'GUARDIA'"), v)


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
