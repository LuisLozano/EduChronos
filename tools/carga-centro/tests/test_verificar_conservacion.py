#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de verificar-conservacion.py (S199): volcados por argumento y reglas de codigos_grupo.py.

El esperado de las formas cortas es la lista de grupos del catálogo de 2026/2027, que deriva
derivar-catalogo.py; aquí solo se exige que cada título de los volcados tenga una y que la
imagen sea exactamente esa lista.
"""
import contextlib
import io
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

vc = cargar("verificar_conservacion", "verificar-conservacion.py")

V2026 = RAIZ / "docs" / "horario-referencia" / "2026-2027"


class FormaCortaDeLosVolcados(unittest.TestCase):

    def test_los_30_titulos_de_2026_tienen_forma_corta(self):
        titulos = [json.loads(f.read_text(encoding="utf-8"))["_meta"]["codigo_crudo"]
                   for f in sorted(V2026.glob("grupo-*.json"))]
        self.assertEqual(len(titulos), 30)
        cortos = {t: vc.normaliza(t) for t in titulos}
        self.assertEqual([t for t, c in cortos.items() if c is None], [])
        bach = {t: c for t, c in cortos.items() if t.startswith("1ºBACH")}
        self.assertEqual(sorted(bach.values()), ["1B-Ac", "1B-Am", "1B-Bc", "1B-Bm", "1B-Ch", "1B-Cs"])
        grupos = {g["codigo"] for g in json.loads((V2026 / "catalogo-derivado.json").read_text(encoding="utf-8"))["grupos"]}
        self.assertEqual(sorted(cortos.values()), sorted(grupos))

    def test_entradas_del_pdf_sin_titulos_sin_regla(self):
        _, mapa, sin_regla, n = vc.entradas_del_pdf(V2026)
        self.assertEqual((n, len(mapa), sin_regla), (30, 30, []))


class Argumentos(unittest.TestCase):

    def test_sin_volcados_es_error_de_argparse(self):
        with contextlib.redirect_stderr(io.StringIO()), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as cm:
                vc.main(["--db", "/no/existe.db"])
        self.assertNotEqual(cm.exception.code, 0)


if __name__ == "__main__":
    unittest.main()
