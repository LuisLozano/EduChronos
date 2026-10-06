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
import tempfile
import unittest
from pathlib import Path
from unittest import mock

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


class ClienteFalso:
    """Registra las escrituras sin red: base vacía, ids correlativos (S204)."""

    def __init__(self):
        self.escrituras, self.lecturas, self.peticiones, self.n = 0, 0, [], 0

    def get(self, ruta):
        self.lecturas += 1
        return {"persistida": False} if ruta == "/api/jornada" else []

    def post(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("POST", ruta, cuerpo))
        self.n += 1
        return {"codigo": cuerpo.get("codigo"), "id": self.n}

    def put(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("PUT", ruta, cuerpo))


class Envio(unittest.TestCase):
    """S204: tipo, totales y plazas sin aula de reuniones y funciones viajan en los POST de alta."""

    @classmethod
    def setUpClass(cls):
        cls.res = {}
        for curso, ruta in (("2026", HR / "2026-2027"), ("2025", HR)):
            cat, nom = leer(ruta / "catalogo-derivado.json"), leer(ruta / "nombres-derivados.json")
            cliente = ClienteFalso()
            with contextlib.redirect_stdout(io.StringIO()):
                cc.cargar(cliente, cat, nom)
            cls.res[curso] = (cat, cliente)

    def cuerpos(self, curso, ruta):
        return {c.get("codigo"): c for m, r, c in self.res[curso][1].peticiones if m == "POST" and r == ruta}

    def test_reunion_y_funcion_con_tipo_y_sin_aula_ni_subgrupos(self):
        act = self.cuerpos("2026", "/api/actividades")
        self.assertEqual((act["RED"]["tipo"], act["ORYCA-FIL1"]["tipo"]), ("REUNION", "FUNCION"))
        self.assertEqual(act["RED"]["plazas"], [{"asignatura": "RED", "aulaFija": None, "aulasCandidatas": [],
                                                 "profesores": ["FIS1", "FOL2", "GH3", "MAT2", "MAT3", "PROV1", "TEC1"],
                                                 "subgrupos": []}])

    def test_clase_sin_tipo_en_el_cuerpo(self):
        act = self.cuerpos("2026", "/api/actividades")
        self.assertEqual(sum("tipo" in c for c in act.values()), 13)
        self.assertNotIn("tipo", act["AMO-1FPB"])
        self.assertTrue(all("tipo" not in c for c in self.cuerpos("2025", "/api/actividades").values()))

    def test_total_en_el_alta_de_profesor(self):
        prof = self.cuerpos("2026", "/api/profesores")
        cat = self.res["2026"][0]
        self.assertEqual({c: b["totalDeclarado"] for c, b in prof.items()},
                         {p["codigo"]: p["totalDeclarado"] for p in cat["profesores"]})
        self.assertEqual(prof["PROV1"], {"codigo": "PROV1", "nombreCompleto": "Lobato Montes, María Carmen", "totalDeclarado": 0})
        self.assertTrue(all("totalDeclarado" not in b for b in self.cuerpos("2025", "/api/profesores").values()))

    def test_total_en_el_alta_de_grupo(self):
        gr = self.cuerpos("2026", "/api/grupos")
        self.assertEqual(len(gr), 25)
        self.assertEqual({b["totalDeclarado"] for b in gr.values()}, {30})
        self.assertTrue(all("totalDeclarado" not in b for b in self.cuerpos("2025", "/api/grupos").values()))

    def test_total_del_pdc_en_su_post_de_alta_y_ningun_put(self):
        pdc = [(r, c) for m, r, c in self.res["2026"][1].peticiones if m == "POST" and r.endswith("/pdc")]
        self.assertEqual(sorted(c["codigo"] for _, c in pdc), ["3ºADi", "3ºBDi", "3ºCDi", "4ºBDi", "4ºCDi"])
        self.assertTrue(all(c == {"codigo": c["codigo"], "totalDeclarado": 30} for _, c in pdc))
        self.assertFalse([r for m, r, _ in self.res["2026"][1].peticiones if m == "PUT" and "/pdc" in r])

    def test_escrituras_son_las_previstas(self):
        for curso, n in (("2026", 875), ("2025", 816)):
            cat, cliente = self.res[curso]
            self.assertEqual((cliente.escrituras, cc.escrituras_previstas(cat)), (n, n))


class CargaAbortaSiNoEsLimpia(unittest.TestCase):
    """S204: una CLASE sin aula sigue abortando la carga sin enviar nada."""

    def test_clase_sin_aula_aborta_sin_enviar(self):
        cat = leer(HR / "2026-2027" / "catalogo-derivado.json")
        a = next(x for x in cat["actividades"] if x["codigo"] == "AMO-1FPB")
        a["plazas"][0].update(aulaFija=None, aulasCandidatas=[])
        with tempfile.TemporaryDirectory() as d:
            fc = Path(d, "catalogo.json")
            fc.write_text(json.dumps(cat, ensure_ascii=False), encoding="utf-8")
            out = io.StringIO()
            with contextlib.redirect_stdout(out), mock.patch.object(cc, "Cliente", side_effect=AssertionError("no debe enviar")):
                rc = cc.main(["--cargar", "--catalogo", str(fc),
                              "--nombres", str(HR / "2026-2027" / "nombres-derivados.json")])
        self.assertEqual(rc, 1)
        self.assertIn("ABORTA: la prevalidacion no esta limpia", out.getvalue())


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
