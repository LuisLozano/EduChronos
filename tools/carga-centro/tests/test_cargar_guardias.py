#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de cargar-guardias.py (S216 T2; S222 T1: recreo, mínimos, y biblioteca y convivencia como
funciones) sobre una API en memoria con PUT de reemplazo total."""
import contextlib
import copy
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402

cg = cargar("cargar_guardias", "cargar-guardias.py")

CAMPOS_CONFIGURACION = ("minimoPorTramo", "minimoPorRecreo", "minimoBibliotecaPorRecreo")


class ApiFalsa:
    """Profesores y sus restricciones horarias, configuración de guardias, asignaturas y actividades.
    Los PUT REEMPLAZAN el estado entero, como en la aplicación: un campo de profesor ausente vuelve a
    null, PROFESOR o 0. `ignora` simula una base que no guarda esos campos del profesor (para la
    relectura). Como el servicio, el PUT de configuración sin uno de los tres mínimos, el alta de un
    código que ya existe y una plaza con una asignatura que no existe responden no-2xx. Toda petición
    queda en `peticiones`; las escrituras, en `puts` y `posts`.
    """

    def __init__(self, ignora=()):
        self.escrituras, self.lecturas, self.puts, self.posts, self.peticiones = 0, 0, [], [], []
        self.ignora = set(ignora)
        self.profesores = {
            1: {"id": 1, "codigo": "ANA", "nombreCompleto": "Ana Uno", "totalDeclarado": 17, "cargo": "DIRECTOR",
                "guardiasOrdinarias": 0, "guardiasRecreo": 0},
            2: {"id": 2, "codigo": "BEA", "nombreCompleto": "Bea Dos", "totalDeclarado": None, "cargo": "PROFESOR",
                "guardiasOrdinarias": 2, "guardiasRecreo": 0},
            3: {"id": 3, "codigo": "CID", "nombreCompleto": "Cid Tres", "totalDeclarado": 12, "cargo": "PROFESOR",
                "guardiasOrdinarias": 0, "guardiasRecreo": 0},
        }
        self.restricciones = {
            1: [{"tipo": "BLANDA", "dia": 1, "ordenEnDia": 1, "motivo": "Reunión de departamento"},
                {"tipo": "DURA", "dia": 3, "ordenEnDia": 4, "motivo": "Gbibl"}],
            2: [{"tipo": "BLANDA", "dia": 4, "ordenEnDia": 4, "motivo": "conv"}],
            3: [{"tipo": "DURA", "dia": 1, "ordenEnDia": 1, "motivo": "conv"},
                {"tipo": "DURA", "dia": 2, "ordenEnDia": 2, "motivo": "Reducción"}],
        }
        self.configuracion = {"minimoPorTramo": 4, "minimoPorRecreo": 0, "minimoBibliotecaPorRecreo": 0}
        self.asignaturas = [{"id": 1, "codigo": "ORYCA", "nombreCompleto": "Ordenación y Catalogació"}]
        self.actividades = []
        self.otros = {}

    def get(self, ruta):
        self.lecturas += 1
        self.peticiones.append(("GET", ruta))
        partes = ruta.strip("/").split("/")
        if partes == ["api", "configuracion-guardias"]:
            return copy.deepcopy(self.configuracion)
        if partes == ["api", "asignaturas"]:
            return copy.deepcopy(self.asignaturas)
        if partes == ["api", "actividades"]:
            return copy.deepcopy(self.actividades)
        if partes == ["api", "profesores"]:
            return [copy.deepcopy(p) for p in self.profesores.values()]
        if len(partes) == 3:
            return copy.deepcopy(self.profesores[int(partes[2])])
        return copy.deepcopy(sorted(self.restricciones[int(partes[2])], key=lambda r: (r["dia"], r["ordenEnDia"])))

    def put(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("PUT", ruta))
        self.puts.append((ruta, copy.deepcopy(cuerpo)))
        partes = ruta.strip("/").split("/")
        if partes == ["api", "configuracion-guardias"]:
            if any(cuerpo.get(c) is None for c in CAMPOS_CONFIGURACION):
                raise cg.cc.ErrorFatal("400 simulado: falta un mínimo en %s" % cuerpo)
            self.configuracion = {c: cuerpo[c] for c in CAMPOS_CONFIGURACION}
            return copy.deepcopy(self.configuracion)
        if partes[:2] != ["api", "profesores"]:
            self.otros[ruta] = copy.deepcopy(cuerpo)
            return cuerpo
        id_ = int(partes[2])
        if len(partes) == 4:
            self.restricciones[id_] = [{c: r.get(c) for c in ("tipo", "dia", "ordenEnDia", "motivo")} for r in cuerpo]
            return self.get(ruta)
        viejo = self.profesores[id_]
        nuevo = {"id": id_, "codigo": cuerpo.get("codigo"), "nombreCompleto": cuerpo.get("nombreCompleto"),
                 "totalDeclarado": cuerpo.get("totalDeclarado"), "cargo": cuerpo.get("cargo") or "PROFESOR",
                 "guardiasOrdinarias": cuerpo.get("guardiasOrdinarias") or 0,
                 "guardiasRecreo": cuerpo.get("guardiasRecreo") or 0}
        for c in self.ignora:
            nuevo[c] = viejo[c]
        self.profesores[id_] = nuevo
        return copy.deepcopy(nuevo)

    def post(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("POST", ruta))
        self.posts.append((ruta, copy.deepcopy(cuerpo)))
        if ruta == "/api/asignaturas":
            if cuerpo["codigo"] in {a["codigo"] for a in self.asignaturas}:
                raise cg.cc.ErrorFatal("409 simulado: asignatura %s duplicada" % cuerpo["codigo"])
            nueva = {"id": len(self.asignaturas) + 1, "codigo": cuerpo["codigo"],
                     "nombreCompleto": cuerpo["nombreCompleto"]}
            self.asignaturas.append(nueva)
            return copy.deepcopy(nueva)
        if ruta == "/api/actividades":
            if cuerpo["codigo"] in {a["codigo"] for a in self.actividades}:
                raise cg.cc.ErrorFatal("409 simulado: actividad %s duplicada" % cuerpo["codigo"])
            existentes = {a["codigo"] for a in self.asignaturas}
            for pl in cuerpo["plazas"]:
                if pl.get("asignatura") not in existentes:
                    raise cg.cc.ErrorFatal("400 simulado: No existe asignatura con codigo %s" % pl.get("asignatura"))
            nueva = dict(copy.deepcopy(cuerpo), id=len(self.actividades) + 1)
            nueva["plazas"] = [dict(pl, id=i + 1, codigo="%s-P%d" % (cuerpo["codigo"], i + 1))
                               for i, pl in enumerate(nueva["plazas"])]
            self.actividades.append(nueva)
            return copy.deepcopy(nueva)
        raise AssertionError("POST inesperado: %s" % ruta)


GUARDIAS = {
    "_meta": {"generadoPor": "tools/carga-centro/derivar-guardias.py", "entradas": {}, "omitidos": []},
    "profesores": [{"codigo": "ANA", "guardiasOrdinarias": 3, "guardiasRecreo": 2},
                   {"codigo": "BEA", "guardiasOrdinarias": 2, "guardiasRecreo": 0},
                   {"codigo": "CID", "guardiasOrdinarias": 0, "guardiasRecreo": 1}],
    "funciones": [{"profesor": "ANA", "asignatura": "Gbibl",
                   "celdas": [{"dia": 3, "ordenEnDia": 4}, {"dia": 5, "ordenEnDia": 1}]},
                  {"profesor": "CID", "asignatura": "conv", "celdas": [{"dia": 1, "ordenEnDia": 1}]}],
}

MINIMOS = {"minimoPorTramo": 4, "minimoPorRecreo": 7, "minimoBibliotecaPorRecreo": 1}

# Lo que el alta tiene que mandar para el GUARDIAS de arriba (forma de derivar-catalogo.py:741-751).
FUNCION_ANA = {"codigo": "Gbibl-ANA", "tipo": "FUNCION", "asignatura": "Gbibl", "duracionTramos": 1,
               "repeticionesPorSemana": 2, "patronTemporal": "DISTRIBUIDA", "requiereTutor": False,
               "plazas": [{"asignatura": "Gbibl", "aulaFija": None, "aulasCandidatas": [], "profesores": ["ANA"],
                           "subgrupos": []}]}
FUNCION_CID = {"codigo": "conv-CID", "tipo": "FUNCION", "asignatura": "conv", "duracionTramos": 1,
               "repeticionesPorSemana": 1, "patronTemporal": "NEUTRA", "requiereTutor": False,
               "plazas": [{"asignatura": "conv", "aulaFija": None, "aulasCandidatas": [], "profesores": ["CID"],
                           "subgrupos": []}]}


def aplicar(api, datos=None, cargar=True, minimos=None):
    out = io.StringIO()
    kw = {} if minimos is None else {"minimos": dict(minimos)}
    with contextlib.redirect_stdout(out):
        rc = cg.aplicar(api, copy.deepcopy(datos or GUARDIAS), cargar=cargar, **kw)
    return rc, out.getvalue()


def con(**cambios):
    datos = copy.deepcopy(GUARDIAS)
    datos.update(cambios)
    return datos


class Carga(unittest.TestCase):

    def test_prevalidar_no_escribe_y_cuenta_lo_previsto(self):
        api = ApiFalsa()
        rc, out = aplicar(api, cargar=False, minimos=MINIMOS)
        self.assertEqual((rc, api.escrituras, api.puts, api.posts), (0, 0, [], []), out)
        self.assertIn("escrituras previstas: 9 (configuración 1, profesor 2, restricciones 2, asignaturas 2,"
                      " actividades 2)", out)

    def test_cuerpo_completo_del_put_de_profesor(self):
        # K3 (S222): guardiasOrdinarias y guardiasRecreo cambian, el resto del GET se conserva (M2).
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertIn(("/api/profesores/1", {"codigo": "ANA", "nombreCompleto": "Ana Uno", "totalDeclarado": 17,
                                             "cargo": "DIRECTOR", "guardiasOrdinarias": 3, "guardiasRecreo": 2}),
                      api.puts)
        self.assertIn(("/api/profesores/3", {"codigo": "CID", "nombreCompleto": "Cid Tres", "totalDeclarado": 12,
                                             "cargo": "PROFESOR", "guardiasOrdinarias": 0, "guardiasRecreo": 1}),
                      api.puts)
        self.assertEqual(api.profesores[1], {"id": 1, "codigo": "ANA", "nombreCompleto": "Ana Uno",
                                             "totalDeclarado": 17, "cargo": "DIRECTOR", "guardiasOrdinarias": 3,
                                             "guardiasRecreo": 2})

    def test_get_de_profesor_sin_guardias_recreo_aborta(self):
        # K3: un GET sin el campo (base sin migrar) no se pisa con un PUT que lo pondría a 0.
        api = ApiFalsa()
        for p in api.profesores.values():
            del p["guardiasRecreo"]
        self.aborta_sin_put(con(), "profesor ANA: el GET no trae ['guardiasRecreo'] y el PUT los borraría", api)

    def test_sin_put_si_no_cambia(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(sorted(r for r, _ in api.puts),
                         ["/api/profesores/1", "/api/profesores/1/restricciones-horarias",
                          "/api/profesores/3", "/api/profesores/3/restricciones-horarias"])
        self.assertIn("escrituras previstas: 8 (configuración 0, profesor 2, restricciones 2, asignaturas 2,"
                      " actividades 2)", out)
        self.assertIn("escrituras reales: 8 OK", out)

    def test_retira_solo_las_dura_de_gbibl_y_conv(self):
        # K5 (S222): fuera las DURA «Gbibl» y «conv»; la BLANDA «conv» de BEA y la DURA «Reducción» de
        # CID se quedan (M3).
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        puts = dict(api.puts)
        self.assertEqual(puts["/api/profesores/1/restricciones-horarias"], [
            {"tipo": "BLANDA", "dia": 1, "ordenEnDia": 1, "motivo": "Reunión de departamento"}])
        self.assertEqual(puts["/api/profesores/3/restricciones-horarias"], [
            {"tipo": "DURA", "dia": 2, "ordenEnDia": 2, "motivo": "Reducción"}])
        self.assertNotIn("/api/profesores/2/restricciones-horarias", puts)
        self.assertEqual(api.restricciones[2], [{"tipo": "BLANDA", "dia": 4, "ordenEnDia": 4, "motivo": "conv"}])

    def test_minimos_put_con_los_tres_campos(self):
        # K4 (S222): GET y PUT del cuerpo entero solo si cambia (M5).
        api = ApiFalsa()
        rc, out = aplicar(api, minimos=MINIMOS)
        self.assertEqual(rc, 0, out)
        self.assertIn(("/api/configuracion-guardias", MINIMOS), api.puts)
        self.assertEqual(api.configuracion, MINIMOS)

    def test_minimos_iguales_sin_put(self):
        api = ApiFalsa()
        api.configuracion = dict(MINIMOS)
        rc, out = aplicar(api, minimos=MINIMOS)
        self.assertEqual(rc, 0, out)
        self.assertNotIn("/api/configuracion-guardias", [r for r, _ in api.puts])
        self.assertIn("configuración 0,", out)

    def test_alta_de_asignaturas_y_funciones(self):
        # K6 (S222): las dos asignaturas con el rótulo del PDF y una FUNCION por (profesor, asignatura).
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(api.posts, [
            ("/api/asignaturas", {"codigo": "Gbibl", "nombreCompleto": "Gbiblioteca"}),
            ("/api/asignaturas", {"codigo": "conv", "nombreCompleto": "R.Convivencia"}),
            ("/api/actividades", FUNCION_ANA),
            ("/api/actividades", FUNCION_CID),
        ])

    def test_asignatura_y_funcion_ya_presentes_no_se_mandan(self):
        # K6, idempotencia por código (M4).
        api = ApiFalsa()
        api.asignaturas.append({"id": 2, "codigo": "conv", "nombreCompleto": "R.Convivencia"})
        api.actividades.append(dict(copy.deepcopy(FUNCION_CID), id=1,
                                    plazas=[dict(FUNCION_CID["plazas"][0], id=1, codigo="conv-CID-P1")]))
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(api.posts, [("/api/asignaturas", {"codigo": "Gbibl", "nombreCompleto": "Gbiblioteca"}),
                                     ("/api/actividades", FUNCION_ANA)])

    def test_funcion_presente_con_otra_forma_no_se_corrige_y_sale_con_1(self):
        api = ApiFalsa()
        api.asignaturas.append({"id": 2, "codigo": "conv", "nombreCompleto": "R.Convivencia"})
        api.actividades.append(dict(copy.deepcopy(FUNCION_CID), id=1, repeticionesPorSemana=2,
                                    plazas=[dict(FUNCION_CID["plazas"][0], id=1, codigo="conv-CID-P1")]))
        rc, out = aplicar(api)
        self.assertEqual(rc, 1, out)
        self.assertNotIn("conv-CID", [c["codigo"] for r, c in api.posts if r == "/api/actividades"])
        self.assertIn("relectura: 1 diferencias", out)
        self.assertIn("conv-CID", out)

    def test_orden_de_escritura(self):
        # K7 (S222): configuración, profesores, restricciones, asignaturas, actividades.
        api = ApiFalsa()
        rc, out = aplicar(api, minimos=MINIMOS)
        self.assertEqual(rc, 0, out)
        self.assertEqual([(m, r) for m, r in api.peticiones if m != "GET"], [
            ("PUT", "/api/configuracion-guardias"),
            ("PUT", "/api/profesores/1"), ("PUT", "/api/profesores/3"),
            ("PUT", "/api/profesores/1/restricciones-horarias"), ("PUT", "/api/profesores/3/restricciones-horarias"),
            ("POST", "/api/asignaturas"), ("POST", "/api/asignaturas"),
            ("POST", "/api/actividades"), ("POST", "/api/actividades"),
        ])

    def aborta_sin_put(self, datos, texto, api=None):
        api = api or ApiFalsa()
        rc, out = aplicar(api, datos)
        self.assertEqual((rc, api.escrituras, api.puts), (1, 0, []), out)
        self.assertIn(texto, out)


    def test_codigo_del_json_ausente_en_la_base_aborta(self):
        # Sin el aborto, plan() pide el id de ZOE y revienta con KeyError: el test lo convierte en
        # un fallo que dice qué capacidad falta, en vez de un error (N8 de S216).
        api = ApiFalsa()
        datos = con(profesores=GUARDIAS["profesores"] + [{"codigo": "ZOE", "guardiasOrdinarias": 1}])
        try:
            rc, out = aplicar(api, datos)
        except Exception as e:
            self.fail("el código ausente no abortó en la prevalidación nombrándolo: %r" % e)
        self.assertEqual((rc, api.escrituras, api.puts), (1, 0, []), out)
        self.assertIn("ZOE", out)
        self.assertIn("profesores del JSON que no existen en la base: ['ZOE']", out)


    def test_profesor_de_la_base_ausente_del_json_aborta(self):
        datos = con(profesores=[p for p in GUARDIAS["profesores"] if p["codigo"] != "BEA"])
        self.aborta_sin_put(datos, "profesores de la base que el JSON no trae: ['BEA']")


    def test_funcion_fuera_de_rango_aborta(self):
        datos = con(funciones=GUARDIAS["funciones"] + [
            {"profesor": "BEA", "asignatura": "conv", "celdas": [{"dia": 6, "ordenEnDia": 1}]}])
        self.aborta_sin_put(datos, "funcion BEA conv (6, 1): día o tramo fuera de rango")

    def test_funcion_de_tramo_siete_aborta(self):
        datos = con(funciones=GUARDIAS["funciones"] + [
            {"profesor": "BEA", "asignatura": "conv", "celdas": [{"dia": 2, "ordenEnDia": 7}]}])
        self.aborta_sin_put(datos, "funcion BEA conv (2, 7): día o tramo fuera de rango")

    def test_funcion_de_profesor_ausente_aborta(self):
        datos = con(funciones=GUARDIAS["funciones"] + [
            {"profesor": "ZOE", "asignatura": "conv", "celdas": [{"dia": 2, "ordenEnDia": 1}]}])
        self.aborta_sin_put(datos, "funcion ZOE conv: el profesor no está en «profesores»")

    def test_funcion_de_asignatura_desconocida_aborta(self):
        datos = con(funciones=GUARDIAS["funciones"] + [
            {"profesor": "BEA", "asignatura": "GR", "celdas": [{"dia": 2, "ordenEnDia": 1}]}])
        self.aborta_sin_put(datos, "funcion BEA GR: asignatura sin nombre conocido (Gbibl, conv)")

    def test_funcion_repetida_aborta(self):
        datos = con(funciones=GUARDIAS["funciones"] + [
            {"profesor": "ANA", "asignatura": "Gbibl", "celdas": [{"dia": 2, "ordenEnDia": 1}]}])
        self.aborta_sin_put(datos, "funcion ANA Gbibl: repetida")

    def test_relectura_distinta_sale_con_1(self):
        api = ApiFalsa(ignora={"guardiasOrdinarias"})
        rc, out = aplicar(api)
        self.assertEqual(rc, 1, out)
        self.assertIn("relectura: 1 diferencias", out)


    def test_escrituras_reales_distintas_de_las_previstas_sale_con_1(self):
        class Doble(ApiFalsa):
            def put(self, ruta, cuerpo):
                self.escrituras += 1          # un cliente que reenvía: cuenta dos escrituras por PUT
                return super().put(ruta, cuerpo)
        rc, out = aplicar(Doble())
        self.assertEqual(rc, 1)
        self.assertIn("escrituras reales: 12 DISTINTAS DE LAS PREVISTAS", out)

    def test_segunda_corrida_sin_escrituras(self):
        # K7: ni PUT ni POST en la segunda (M4).
        api = ApiFalsa()
        self.assertEqual(aplicar(api, minimos=MINIMOS)[0], 0)
        antes, posts = api.escrituras, len(api.posts)
        rc, out = aplicar(api, minimos=MINIMOS)
        self.assertEqual((rc, api.escrituras - antes, len(api.posts) - posts), (0, 0, 0), out)
        self.assertIn("escrituras previstas: 0 (configuración 0, profesor 0, restricciones 0, asignaturas 0,"
                      " actividades 0)", out)

    def test_ninguna_peticion_a_configuracion_guardias(self):
        api = ApiFalsa()
        aplicar(api, cargar=False)
        aplicar(api)
        aplicar(api)
        self.assertEqual([p for p in api.peticiones if "configuracion" in p[1]], [])
        self.assertEqual(api.otros, {})


class Main(unittest.TestCase):

    def correr(self, args, api, err=None):
        with tempfile.TemporaryDirectory() as t:
            ruta = Path(t) / "guardias.json"
            ruta.write_text(json.dumps(GUARDIAS, ensure_ascii=False), encoding="utf-8")
            args = [a.replace("{guardias}", str(ruta)) for a in args]
            out, err = io.StringIO(), err if err is not None else io.StringIO()
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err), \
                    mock.patch.object(cg, "crear_cliente", return_value=api) as cc:
                rc = cg.main(args)
        return rc, out.getvalue(), cc

    def falla_el_argparse(self, args, texto):
        api = ApiFalsa()
        err = io.StringIO()
        with self.assertRaises(SystemExit) as ctx:
            self.correr(args, api, err)
        self.assertEqual(ctx.exception.code, 2)
        self.assertIn(texto, err.getvalue())
        self.assertEqual(api.peticiones, [])

    def test_sin_base_url_falla(self):
        self.falla_el_argparse(["--guardias", "{guardias}"], "--base-url")

    def test_modos_excluyentes(self):
        self.falla_el_argparse(["--prevalidar", "--cargar", "--base-url", "http://x", "--guardias", "{guardias}"],
                               "not allowed with argument")

    def test_guardias_relativa_falla(self):
        self.falla_el_argparse(["--base-url", "http://x", "--guardias", "guardias.json"], "ruta absoluta")

    def test_por_defecto_prevalida_y_no_escribe(self):
        api = ApiFalsa()
        rc, out, cc = self.correr(["--base-url", "http://x", "--guardias", "{guardias}"], api)
        self.assertEqual((rc, api.puts), (0, []), out)
        self.assertIn("Modo prevalidacion", out)
        cc.assert_called_once_with("http://x")

    def test_cargar_escribe(self):
        api = ApiFalsa()
        rc, out, _ = self.correr(["--cargar", "--base-url", "http://x", "--guardias", "{guardias}"], api)
        self.assertEqual((rc, len(api.puts), len(api.posts)), (0, 4, 4), out)

    def test_minimos_los_tres_o_ninguno(self):
        self.falla_el_argparse(["--base-url", "http://x", "--guardias", "{guardias}", "--minimo-tramo", "4",
                                "--minimo-recreo", "7"], "los tres o ninguno")

    def test_minimos_por_argumento_llegan_al_put(self):
        api = ApiFalsa()
        rc, out, _ = self.correr(["--cargar", "--base-url", "http://x", "--guardias", "{guardias}",
                                  "--minimo-tramo", "4", "--minimo-recreo", "7", "--minimo-biblioteca", "1"], api)
        self.assertEqual(rc, 0, out)
        self.assertIn(("/api/configuracion-guardias", MINIMOS), api.puts)

    def test_error_http_sale_con_2_y_no_sigue(self):
        class Rechaza(ApiFalsa):
            def put(self, ruta, cuerpo):
                self.escrituras += 1
                self.puts.append((ruta, cuerpo))
                raise cg.cc.ErrorFatal("RESPUESTA NO 2xx (simulada)")
        api = Rechaza()
        rc, out, _ = self.correr(["--cargar", "--base-url", "http://x", "--guardias", "{guardias}"], api)
        self.assertEqual((rc, len(api.puts)), (2, 1), out)
        self.assertIn("RESPUESTA NO 2xx (simulada)", out)


if __name__ == "__main__":
    unittest.main()
