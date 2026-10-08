#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de cargar-reglas-aulas.py (S209 T2) sobre una API en memoria con PUT de reemplazo total."""
import contextlib
import copy
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402

cr = cargar("cargar_reglas_aulas", "cargar-reglas-aulas.py")


class ApiFalsa:
    """GET de listado, de entidad y de aulas de asignatura; PUT que REEMPLAZA el estado entero.

    Un campo que el PUT no trae queda a None, como en la aplicación: así se ve si el cargador lo
    conserva. `ignora` simula una base que no guarda esos campos (para la relectura).
    """

    CAMPOS = {"aulas": ("codigo", "tipo", "capacidad", "edificio", "planta", "sector", "enUso"),
              "grupos": ("codigo", "nivel", "tipo", "totalDeclarado", "aulaReferencia"),
              "subgrupos": ("codigo", "grupos", "alumnos")}

    def __init__(self, ignora=()):
        self.escrituras, self.lecturas, self.puts, self.ignora = 0, 0, [], set(ignora)
        self.datos = {
            "aulas": {1: {"id": 1, "codigo": "A1", "tipo": "ORDINARIA", "capacidad": None, "edificio": "A",
                          "planta": 1, "sector": "norte", "enUso": True},
                      2: {"id": 2, "codigo": "A6", "tipo": "LAB_CIENCIAS", "capacidad": 33, "edificio": "A",
                          "planta": 0, "sector": None, "enUso": True},
                      3: {"id": 3, "codigo": "A16", "tipo": "ORDINARIA", "capacidad": None, "edificio": None,
                          "planta": None, "sector": None, "enUso": True}},
            "grupos": {10: {"id": 10, "codigo": "1ºA", "nivel": "1ESO", "tipo": "ORDINARIO", "totalDeclarado": 30,
                            "aulaReferencia": None},
                       11: {"id": 11, "codigo": "3ºADi", "nivel": "3ESO", "tipo": "DIVERSIFICACION_PDC",
                            "totalDeclarado": 30, "aulaReferencia": None}},
            "asignaturas": {20: {"id": 20, "codigo": "PLAB", "nombreCompleto": "Prácticas de Laboratorio"},
                            21: {"id": 21, "codigo": "TEC", "nombreCompleto": "Tecnología"}},
            "subgrupos": {30: {"id": 30, "codigo": "1ºA-ALCT", "grupos": ["1ºA"], "alumnos": None, "delPdc": False}},
        }
        self.aulas_asig = {20: [], 21: [{"aula": "A6", "rol": "PREFERIDA"}]}

    def get(self, ruta):
        self.lecturas += 1
        partes = ruta.strip("/").split("/")
        if len(partes) == 2:
            return [copy.deepcopy(x) for x in self.datos[partes[1]].values()]
        if len(partes) == 4:
            return copy.deepcopy(self.aulas_asig[int(partes[2])])
        return copy.deepcopy(self.datos[partes[1]][int(partes[2])])

    def put(self, ruta, cuerpo):
        self.escrituras += 1
        self.puts.append((ruta, copy.deepcopy(cuerpo)))
        partes = ruta.strip("/").split("/")
        if len(partes) == 4:
            self.aulas_asig[int(partes[2])] = copy.deepcopy(cuerpo)
            return cuerpo
        familia, id_ = partes[1], int(partes[2])
        nuevo = {"id": id_}
        for c in self.CAMPOS[familia]:
            nuevo[c] = self.datos[familia][id_][c] if c in self.ignora else cuerpo.get(c)
        if familia == "subgrupos":
            nuevo["delPdc"] = self.datos[familia][id_]["delPdc"]
        self.datos[familia][id_] = nuevo
        return nuevo

    def post(self, ruta, cuerpo):
        raise AssertionError("el cargador de reglas no debe crear nada")


REGLAS = {
    "aulaReferencia": [{"grupo": "1ºA", "aula": "A1", "procedencia": []}],
    "aulasAsignatura": [{"asignatura": "PLAB", "aulas": [{"aula": "A6", "rol": "PREFERIDA", "procedencia": []}]},
                        {"asignatura": "TEC", "aulas": [{"aula": "A6", "rol": "PREFERIDA", "procedencia": []}]}],
    "usoAula": [{"aula": "A16", "enUso": False, "procedencia": []}],
    "capacidad": [{"aula": "A1", "capacidad": 32, "procedencia": []},
                  {"aula": "A6", "capacidad": 33, "procedencia": []}],
    "alumnos": [{"subgrupo": "1ºA-ALCT", "alumnos": 8, "procedencia": []}],
    "conservarAula": [{"actividad": "NO-SE-USA", "motivo": "la ignora el cargador de reglas"}],
    "omitidas": [],
}


def aplicar(api, reglas=None):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        rc = cr.aplicar(api, copy.deepcopy(reglas or REGLAS))
    return rc, out.getvalue()


class Carga(unittest.TestCase):

    def test_conserva_los_demas_campos(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(api.datos["aulas"][1], {"id": 1, "codigo": "A1", "tipo": "ORDINARIA", "capacidad": 32,
                                                 "edificio": "A", "planta": 1, "sector": "norte", "enUso": True})
        self.assertEqual(api.datos["aulas"][3]["enUso"], False)
        self.assertEqual(api.datos["grupos"][10], {"id": 10, "codigo": "1ºA", "nivel": "1ESO", "tipo": "ORDINARIO",
                                                   "totalDeclarado": 30, "aulaReferencia": "A1"})
        self.assertEqual(api.datos["subgrupos"][30], {"id": 30, "codigo": "1ºA-ALCT", "grupos": ["1ºA"],
                                                      "alumnos": 8, "delPdc": False})
        self.assertEqual(api.aulas_asig[20], [{"aula": "A6", "rol": "PREFERIDA"}])
        cuerpo = dict(api.puts)["/api/aulas/1"]
        self.assertEqual(set(cuerpo), set(ApiFalsa.CAMPOS["aulas"]))

    def test_sin_put_si_no_cambia_nada(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        rutas = [r for r, _ in api.puts]
        self.assertNotIn("/api/aulas/2", rutas)                 # A6 ya tiene capacidad 33
        self.assertNotIn("/api/asignaturas/21/aulas", rutas)    # TEC ya tiene [A6 PREFERIDA]
        self.assertEqual(len(rutas), 5)
        self.assertIn("escrituras previstas (entidades que difieren): 5", out)
        self.assertIn("escrituras reales: 5 OK", out)

    def test_orden_aulas_grupos_asignaturas_subgrupos(self):
        api = ApiFalsa()
        aplicar(api)
        self.assertEqual([r for r, _ in api.puts], ["/api/aulas/1", "/api/aulas/3", "/api/grupos/10",
                                                    "/api/asignaturas/20/aulas", "/api/subgrupos/30"])

    def test_segunda_corrida_sin_put(self):
        api = ApiFalsa()
        aplicar(api)
        antes = api.escrituras
        rc, out = aplicar(api)
        self.assertEqual((rc, api.escrituras - antes), (0, 0), out)

    def test_relectura_distinta_sale_con_1(self):
        api = ApiFalsa(ignora={"capacidad"})
        rc, out = aplicar(api)
        self.assertEqual(rc, 1)
        self.assertIn("aulas A1: capacidad=None, se esperaba 32", out)

    def test_escrituras_reales_distintas_de_las_previstas_sale_con_1(self):
        class Reintenta(ApiFalsa):
            def put(self, ruta, cuerpo):
                self.escrituras += 1          # un cliente que reenvía: cuenta dos escrituras por PUT
                return super().put(ruta, cuerpo)
        api = Reintenta()
        rc, out = aplicar(api)
        self.assertEqual(rc, 1, out)
        self.assertIn("relectura: sin diferencias", out)
        self.assertIn("escrituras reales: 10 DISTINTAS DE LAS PREVISTAS", out)

    def aborta_sin_put(self, reglas, texto):
        api = ApiFalsa()
        rc, out = aplicar(api, reglas)
        self.assertEqual((rc, api.escrituras, api.puts), (1, 0, []), out)
        self.assertIn("ABORTA", out)
        self.assertIn(texto, out)

    def test_codigo_desconocido_aborta_sin_put(self):
        for familia, clave, valor, texto in (
                ("capacidad", "aula", "ZZ9", "aulas ZZ9: no existe"),
                ("aulaReferencia", "grupo", "9ºZ", "grupos 9ºZ: no existe"),
                ("aulaReferencia", "aula", "ZZ8", "aula ZZ8: la nombra una regla y no existe"),
                ("aulasAsignatura", "asignatura", "XYZ", "asignaturas XYZ: no existe"),
                ("alumnos", "subgrupo", "no-hay", "subgrupos no-hay: no existe")):
            with self.subTest(familia=familia, clave=clave):
                reglas = copy.deepcopy(REGLAS)
                reglas[familia][-1][clave] = valor
                self.aborta_sin_put(reglas, texto)

    def test_aula_de_asignatura_desconocida_aborta_sin_put(self):
        reglas = copy.deepcopy(REGLAS)
        reglas["aulasAsignatura"][0]["aulas"].append({"aula": "ZZ7", "rol": "PREFERIDA"})
        self.aborta_sin_put(reglas, "aula ZZ7: la nombra una regla y no existe")

    def test_roles_mezclados_abortan_sin_put(self):
        reglas = copy.deepcopy(REGLAS)
        reglas["aulasAsignatura"][0]["aulas"].append({"aula": "A1", "rol": "EXCLUSIVA"})
        self.aborta_sin_put(reglas, "asignaturas PLAB: mezcla de roles")

    def test_grupo_pdc_aborta_sin_put(self):
        reglas = copy.deepcopy(REGLAS)
        reglas["aulaReferencia"].append({"grupo": "3ºADi", "aula": "A1"})
        self.aborta_sin_put(reglas, "grupos 3ºADi: es DIVERSIFICACION_PDC")

    def test_entidad_en_dos_reglas_aborta_sin_put(self):
        reglas = copy.deepcopy(REGLAS)
        reglas["capacidad"].append({"aula": "A1", "capacidad": 40})
        self.aborta_sin_put(reglas, "aulas A1: dos reglas fijan capacidad")

    def test_get_sin_un_campo_aborta_sin_put(self):
        api = ApiFalsa()
        del api.datos["aulas"][1]["sector"]
        rc, out = aplicar(api)
        self.assertEqual((rc, api.puts), (1, []), out)
        self.assertIn("el GET no trae ['sector']", out)


class Main(unittest.TestCase):

    def test_ruta_relativa_y_cliente_de_cargar_centro(self):
        self.assertEqual(cr.cc.Cliente.__name__, "Cliente")
        self.assertTrue(hasattr(cr.cc, "ErrorFatal"))
        api = ApiFalsa()
        antes = os.getcwd()
        with tempfile.TemporaryDirectory() as d:
            Path(d, "reglas.json").write_text(json.dumps(REGLAS, ensure_ascii=False), encoding="utf-8")
            os.chdir(d)
            out = io.StringIO()
            try:
                with contextlib.redirect_stdout(out), mock.patch.object(cr, "crear_cliente", return_value=api) as cc:
                    rc = cr.main(["--reglas", "reglas.json"])
            finally:
                os.chdir(antes)
            absoluta = Path(d).resolve() / "reglas.json"
        self.assertEqual(rc, 0, out.getvalue())
        self.assertIn("reglas: %s" % absoluta, out.getvalue())
        cc.assert_called_once_with("http://localhost:8080")

    def test_error_http_sale_con_2(self):
        api = mock.Mock()
        api.get.side_effect = cr.cc.ErrorFatal("RESPUESTA NO 2xx")
        with tempfile.TemporaryDirectory() as d:
            Path(d, "reglas.json").write_text(json.dumps(REGLAS), encoding="utf-8")
            out = io.StringIO()
            with contextlib.redirect_stdout(out), mock.patch.object(cr, "crear_cliente", return_value=api):
                rc = cr.main(["--reglas", str(Path(d, "reglas.json")), "--base-url", "http://x:1"])
        self.assertEqual(rc, 2)
        self.assertIn("RESPUESTA NO 2xx", out.getvalue())


if __name__ == "__main__":
    unittest.main()
