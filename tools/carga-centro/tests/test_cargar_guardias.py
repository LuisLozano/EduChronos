#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de cargar-guardias.py (S216 T2) sobre una API en memoria con PUT de reemplazo total."""
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


class ApiFalsa:
    """Profesores y sus restricciones horarias. Los PUT REEMPLAZAN el estado entero, como en la
    aplicación: un campo de profesor ausente vuelve a null, PROFESOR o 0. `ignora` simula una base
    que no guarda esos campos del profesor (para la relectura). Toda petición queda en `peticiones`.
    """

    def __init__(self, ignora=()):
        self.escrituras, self.lecturas, self.puts, self.peticiones = 0, 0, [], []
        self.ignora = set(ignora)
        self.profesores = {
            1: {"id": 1, "codigo": "ANA", "nombreCompleto": "Ana Uno", "totalDeclarado": 17, "cargo": "DIRECTOR",
                "guardiasOrdinarias": 0},
            2: {"id": 2, "codigo": "BEA", "nombreCompleto": "Bea Dos", "totalDeclarado": None, "cargo": "PROFESOR",
                "guardiasOrdinarias": 2},
            3: {"id": 3, "codigo": "CID", "nombreCompleto": "Cid Tres", "totalDeclarado": 12, "cargo": "PROFESOR",
                "guardiasOrdinarias": 0},
        }
        self.restricciones = {
            1: [{"tipo": "BLANDA", "dia": 1, "ordenEnDia": 1, "motivo": "Reunión de departamento"}],
            2: [],
            3: [{"tipo": "DURA", "dia": 1, "ordenEnDia": 1, "motivo": "conv"}],
        }
        self.otros = {}

    def get(self, ruta):
        self.lecturas += 1
        self.peticiones.append(("GET", ruta))
        partes = ruta.strip("/").split("/")
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
                 "guardiasOrdinarias": cuerpo.get("guardiasOrdinarias") or 0}
        for c in self.ignora:
            nuevo[c] = viejo[c]
        self.profesores[id_] = nuevo
        return copy.deepcopy(nuevo)

    def post(self, ruta, cuerpo):
        raise AssertionError("el cargador de guardias no debe crear nada")


GUARDIAS = {
    "_meta": {"generadoPor": "tools/carga-centro/derivar-guardias.py", "entradas": {}, "omitidos": []},
    "profesores": [{"codigo": "ANA", "guardiasOrdinarias": 3}, {"codigo": "BEA", "guardiasOrdinarias": 2},
                   {"codigo": "CID", "guardiasOrdinarias": 0}],
    "noPuede": [{"codigo": "ANA", "dia": 3, "ordenEnDia": 4, "motivo": "Gbibl"},
                {"codigo": "CID", "dia": 1, "ordenEnDia": 1, "motivo": "conv"}],
}


def aplicar(api, datos=None, cargar=True):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        rc = cg.aplicar(api, copy.deepcopy(datos or GUARDIAS), cargar=cargar)
    return rc, out.getvalue()


def con(**cambios):
    datos = copy.deepcopy(GUARDIAS)
    datos.update(cambios)
    return datos


class Carga(unittest.TestCase):

    def test_prevalidar_no_escribe_y_cuenta_lo_previsto(self):
        api = ApiFalsa()
        rc, out = aplicar(api, cargar=False)
        self.assertEqual((rc, api.escrituras, api.puts), (0, 0, []), out)
        self.assertIn("escrituras previstas: 2 (PUT de profesor 1, PUT de restricciones 1)", out)

    def test_cuerpo_completo_del_put_de_profesor(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertIn(("/api/profesores/1", {"codigo": "ANA", "nombreCompleto": "Ana Uno", "totalDeclarado": 17,
                                             "cargo": "DIRECTOR", "guardiasOrdinarias": 3}), api.puts)
        self.assertEqual(api.profesores[1], {"id": 1, "codigo": "ANA", "nombreCompleto": "Ana Uno",
                                             "totalDeclarado": 17, "cargo": "DIRECTOR", "guardiasOrdinarias": 3})

    def test_sin_put_si_no_cambia(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(sorted(r for r, _ in api.puts),
                         ["/api/profesores/1", "/api/profesores/1/restricciones-horarias"])
        self.assertIn("escrituras previstas: 2 (PUT de profesor 1, PUT de restricciones 1)", out)
        self.assertIn("escrituras reales: 2 OK", out)

    def test_union_conserva_las_existentes_con_su_motivo(self):
        api = ApiFalsa()
        rc, out = aplicar(api)
        self.assertEqual(rc, 0, out)
        self.assertEqual(dict(api.puts)["/api/profesores/1/restricciones-horarias"], [
            {"tipo": "BLANDA", "dia": 1, "ordenEnDia": 1, "motivo": "Reunión de departamento"},
            {"tipo": "DURA", "dia": 3, "ordenEnDia": 4, "motivo": "Gbibl"},
        ])

    def conflicto(self, existente, texto):
        api = ApiFalsa()
        api.restricciones[1].append(existente)
        rc, out = aplicar(api)
        self.assertEqual((rc, api.escrituras, api.puts), (1, 0, []), out)
        self.assertIn("conflictos con restricciones existentes", out)
        self.assertIn(texto, out)
        self.assertEqual(api.profesores[1]["guardiasOrdinarias"], 0)

    def test_conflicto_de_tipo_aborta_sin_ningun_put(self):
        self.conflicto({"tipo": "BLANDA", "dia": 3, "ordenEnDia": 4, "motivo": "Gbibl"},
                       "profesor ANA: (dia=3, ordenEnDia=4) ya tiene BLANDA «Gbibl» y el JSON pide DURA «Gbibl»")

    def test_conflicto_de_motivo_aborta_sin_ningun_put(self):
        self.conflicto({"tipo": "DURA", "dia": 3, "ordenEnDia": 4, "motivo": "Reducción"},
                       "ya tiene DURA «Reducción» y el JSON pide DURA «Gbibl»")

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

    def test_no_puede_fuera_de_rango_aborta(self):
        datos = con(noPuede=GUARDIAS["noPuede"] + [{"codigo": "BEA", "dia": 6, "ordenEnDia": 1, "motivo": "conv"}])
        self.aborta_sin_put(datos, "noPuede BEA (6, 1): día o tramo fuera de rango")

    def test_no_puede_de_tramo_siete_aborta(self):
        datos = con(noPuede=GUARDIAS["noPuede"] + [{"codigo": "BEA", "dia": 2, "ordenEnDia": 7, "motivo": "conv"}])
        self.aborta_sin_put(datos, "noPuede BEA (2, 7): día o tramo fuera de rango")

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
        self.assertIn("escrituras reales: 4 DISTINTAS DE LAS PREVISTAS", out)

    def test_segunda_corrida_sin_put(self):
        api = ApiFalsa()
        self.assertEqual(aplicar(api)[0], 0)
        antes = api.escrituras
        rc, out = aplicar(api)
        self.assertEqual((rc, api.escrituras - antes), (0, 0), out)
        self.assertIn("escrituras previstas: 0 (PUT de profesor 0, PUT de restricciones 0)", out)

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
        self.assertEqual((rc, len(api.puts)), (0, 2), out)

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
