#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests del modo sin aulas de cargar-centro.py (S209 T1, base de prueba de O-aulas).

Catálogo de 2026/2027: 310 plazas de CLASE (287 con aula fija, 23 con candidatas) y 13 de REUNION o
FUNCION sin aula; las 217 actividades CLASE no llevan «tipo». 875 escrituras desde vacío (S204).
"""
import contextlib
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

cc = cargar("cargar_centro", "cargar-centro.py")

CURSO = RAIZ / "docs" / "horario-referencia" / "2026-2027"
CLAVES_AULA = {"aulaFija", "aulasCandidatas"}
CON_FIJA, CON_CANDIDATAS = "AMO-1FPB", "Bloque-ALCT_Fr2-1ºA"


def leer(ruta):
    return json.loads(ruta.read_text(encoding="utf-8"))


def copia(x):
    return json.loads(json.dumps(x))


class ClienteMemoria:
    """Base en memoria sin red: guarda lo que recibe y lo devuelve en los listados.

    El alta de un PDC lo añade a /api/grupos y crea su subgrupo {codigo}-Completo, como la aplicación.
    """

    def __init__(self):
        self.escrituras, self.lecturas, self.peticiones, self.n = 0, 0, [], 0
        self.listados = {r: [] for r in ("/api/niveles", "/api/asignaturas", "/api/profesores", "/api/aulas",
                                         "/api/grupos", "/api/subgrupos", "/api/actividades")}
        self.tutorias = {}

    def _alta(self, ruta, cuerpo):
        self.n += 1
        x = dict(cuerpo, id=self.n)
        self.listados[ruta].append(x)
        return x

    def get(self, ruta):
        self.lecturas += 1
        if ruta == "/api/jornada":
            return {"persistida": False}
        if ruta.endswith("/tutoria"):
            return self.tutorias.get(ruta, [])
        return list(self.listados[ruta])

    def post(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("POST", ruta, cuerpo))
        if ruta.endswith("/pdc"):
            self._alta("/api/subgrupos", {"codigo": cuerpo["codigo"] + cc.SUFIJO_SUBGRUPO_PDC})
            return self._alta("/api/grupos", cuerpo)
        return self._alta(ruta, cuerpo)

    def put(self, ruta, cuerpo):
        self.escrituras += 1
        self.peticiones.append(("PUT", ruta, cuerpo))
        if ruta.endswith("/tutoria"):
            self.tutorias[ruta] = cuerpo


def actividades_enviadas(cliente):
    return {c["codigo"]: c for m, r, c in cliente.peticiones if m == "POST" and r == "/api/actividades"}


def cargar_en_memoria(cat, **modo):
    cliente = ClienteMemoria()
    with contextlib.redirect_stdout(io.StringIO()):
        enviados, _ = cc.cargar(cliente, cat, leer(CURSO / "nombres-derivados.json"), **modo)
    return cliente, enviados


class Base(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.cat = leer(CURSO / "catalogo-derivado.json")
        cls.nombres = {"asignaturas": {a["codigo"]: {} for a in cls.cat["asignaturas"]},
                       "profesores": {p["codigo"]: {} for p in cls.cat["profesores"]}}

    def actividad(self, cat, codigo):
        return next(a for a in cat["actividades"] if a["codigo"] == codigo)

    def main(self, argv, catalogo=None, cliente=None):
        """main() con el catálogo dado (o el versionado); sin cliente, crear uno es un fallo."""
        with tempfile.TemporaryDirectory() as d:
            fc = CURSO / "catalogo-derivado.json"
            if catalogo is not None:
                fc = Path(d, "catalogo.json")
                fc.write_text(json.dumps(catalogo, ensure_ascii=False), encoding="utf-8")
            doble = (mock.patch.object(cc, "Cliente", return_value=cliente) if cliente is not None
                     else mock.patch.object(cc, "Cliente", side_effect=AssertionError("no debe enviar")))
            out = io.StringIO()
            with contextlib.redirect_stdout(out), doble:
                rc = cc.main(["--catalogo", str(fc), "--nombres", str(CURSO / "nombres-derivados.json")] + argv)
        return rc, out.getvalue()

    def fichero_conservar(self, d, lista, nombre="conservar.json"):
        ruta = Path(d, nombre)
        ruta.write_text(json.dumps({"otraClave": {"x": 1}, "conservarAula": lista}, ensure_ascii=False),
                        encoding="utf-8")
        return ruta


class T1Prevalidacion(Base):
    """F1.4: una CLASE sin aula solo pasa la prevalidación con --sin-aulas."""

    def sin_aula(self, codigo, **cambios):
        cat = copia(self.cat)
        a = self.actividad(cat, codigo)
        a["plazas"][0].update(aulaFija=None, aulasCandidatas=[])
        a.update(cambios)
        return cat

    def test_sin_el_modo_sigue_siendo_violacion(self):
        for codigo, cambios in ((CON_FIJA, {}), (CON_CANDIDATAS, {}), (CON_FIJA, {"tipo": "CLASE"})):
            v = cc.prevalidar(self.sin_aula(codigo, **cambios), self.nombres)
            self.assertEqual([(f, s) for f, s, _ in v], [("plazas", "%s plaza 1" % codigo)])
            self.assertIn("XOR de aula: una clase necesita", v[0][2])

    def test_con_el_modo_se_admite(self):
        for codigo, cambios in ((CON_FIJA, {}), (CON_CANDIDATAS, {}), (CON_FIJA, {"tipo": "CLASE"})):
            self.assertEqual(cc.prevalidar(self.sin_aula(codigo, **cambios), self.nombres, sin_aulas=True), [])

    def test_con_el_modo_fija_y_candidatas_a_la_vez_sigue_siendo_violacion(self):
        cat = copia(self.cat)
        self.actividad(cat, CON_FIJA)["plazas"][0]["aulasCandidatas"] = ["A1"]
        v = cc.prevalidar(cat, self.nombres, sin_aulas=True)
        self.assertEqual([(s, m) for _, s, m in v],
                         [("%s plaza 1" % CON_FIJA, "XOR de aula: tiene aula fija y aulas candidatas a la vez")])

    def test_main_sin_el_modo_aborta_y_con_el_modo_carga(self):
        cat = self.sin_aula(CON_FIJA)
        rc, out = self.main(["--cargar"], catalogo=cat)
        self.assertEqual(rc, 1)
        self.assertIn("ABORTA: la prevalidacion no esta limpia", out)
        cliente = ClienteMemoria()
        rc, out = self.main(["--cargar", "--sin-aulas"], catalogo=cat, cliente=cliente)
        self.assertEqual(rc, 0, out)
        self.assertNotIn("aulaFija", actividades_enviadas(cliente)[CON_FIJA]["plazas"][0])


class T2ClaseSinAula(Base):
    """F1.2: con --sin-aulas ninguna plaza de CLASE lleva claves de aula, tenga «tipo» o no."""

    def test_ninguna_plaza_de_clase_lleva_claves_de_aula(self):
        cliente, _ = cargar_en_memoria(self.cat, sin_aulas=True)
        act = actividades_enviadas(cliente)
        clases = [c for c in act.values() if "tipo" not in c]
        self.assertEqual((len(clases), sum(len(c["plazas"]) for c in clases)), (217, 310))
        for c in clases:
            for p in c["plazas"]:
                self.assertEqual(set(p) & CLAVES_AULA, set(), c["codigo"])
        self.assertEqual(act[CON_FIJA]["plazas"], [{"asignatura": "AMO", "profesores": ["PAU1"],
                                                    "subgrupos": ["1FPB-Completo"]}])

    def test_tipo_clase_explicito_tambien_pierde_el_aula(self):
        cat = copia(self.cat)
        self.actividad(cat, CON_CANDIDATAS)["tipo"] = "CLASE"
        cliente, _ = cargar_en_memoria(cat, sin_aulas=True)
        cuerpo = actividades_enviadas(cliente)[CON_CANDIDATAS]
        self.assertEqual(cuerpo["tipo"], "CLASE")
        self.assertTrue(all(not set(p) & CLAVES_AULA for p in cuerpo["plazas"]))

    def test_sin_el_modo_los_cuerpos_no_cambian(self):
        hoy, _ = cargar_en_memoria(self.cat)
        explicito, _ = cargar_en_memoria(self.cat, sin_aulas=False)
        self.assertEqual(hoy.peticiones, explicito.peticiones)
        self.assertEqual(actividades_enviadas(hoy)[CON_FIJA]["plazas"][0]["aulaFija"], "TAL41")


class T3NoClaseNoCambia(Base):
    """F1.2: las plazas de REUNION y FUNCION viajan igual con el modo que sin él."""

    def test_no_clase_igual_en_los_dos_modos(self):
        cat = copia(self.cat)
        # Ninguna REUNION o FUNCION del catálogo lleva aula; una con aula fija comprueba que no se le quita.
        self.actividad(cat, "ORYCA-FIL1")["plazas"][0]["aulaFija"] = "A1"
        sin, _ = cargar_en_memoria(cat, sin_aulas=True)
        con, _ = cargar_en_memoria(cat)
        a_sin, a_con = actividades_enviadas(sin), actividades_enviadas(con)
        no_clase = sorted(c for c, b in a_con.items() if b.get("tipo") in ("REUNION", "FUNCION"))
        self.assertEqual(len(no_clase), 13)
        for c in no_clase:
            self.assertEqual(a_sin[c], a_con[c], c)
        self.assertEqual(a_sin["ORYCA-FIL1"]["plazas"][0]["aulaFija"], "A1")
        self.assertEqual(a_sin["RED"]["plazas"][0]["aulasCandidatas"], [])


class T4Conservar(Base):
    """F1.3: las actividades de la lista conservan aula fija y candidatas del catálogo."""

    def test_conservan_el_aula_del_catalogo(self):
        cliente, enviados = cargar_en_memoria(self.cat, sin_aulas=True,
                                              conservar=frozenset({CON_FIJA, CON_CANDIDATAS}))
        act = actividades_enviadas(cliente)
        for codigo in (CON_FIJA, CON_CANDIDATAS):
            esperado = [{"aulaFija": p.get("aulaFija"), "aulasCandidatas": list(p.get("aulasCandidatas") or [])}
                        for p in self.actividad(self.cat, codigo)["plazas"]]
            self.assertEqual([{k: p[k] for k in CLAVES_AULA} for p in act[codigo]["plazas"]], esperado)
        self.assertEqual(act[CON_FIJA]["plazas"][0]["aulaFija"], "TAL41")
        self.assertTrue(act[CON_CANDIDATAS]["plazas"][0]["aulasCandidatas"])
        conservadas = sum(len(self.actividad(self.cat, c)["plazas"]) for c in (CON_FIJA, CON_CANDIDATAS))
        self.assertEqual((enviados["plazasClaseSinAula"], enviados["plazasConservanAula"]),
                         (310 - conservadas, conservadas))
        otra = act["Bloque-ALCT_Fr2-1ºB"]
        self.assertTrue(all(not set(p) & CLAVES_AULA for p in otra["plazas"]))

    def test_actividad_sin_tipo_se_admite_como_clase(self):
        self.assertNotIn("tipo", self.actividad(self.cat, CON_FIJA))
        self.assertEqual(cc.validar_conservar(self.cat, [{"actividad": CON_FIJA, "motivo": "taller de FPB"}]), [])

    def test_main_con_la_lista(self):
        cliente = ClienteMemoria()
        with tempfile.TemporaryDirectory() as d:
            ruta = self.fichero_conservar(d, [{"actividad": CON_FIJA, "motivo": "taller de FPB"}])
            rc, out = self.main(["--cargar", "--sin-aulas", "--conservar-aulas", str(ruta)], cliente=cliente)
        self.assertEqual(rc, 0, out)
        self.assertEqual(actividades_enviadas(cliente)[CON_FIJA]["plazas"][0]["aulaFija"], "TAL41")
        self.assertIn("MODO SIN AULAS: 309 plazas de CLASE sin aula, 1 plazas conservan aula, "
                      "1 actividades en la lista de conservar", out)


class T5ListaInvalidaAborta(Base):
    """F1.3: desconocido, repetido, motivo vacío o no CLASE abortan sin envíos y nombran el identificador."""

    CASOS = {
        "desconocido": ([{"actividad": "NO-EXISTE", "motivo": "x"}], "'NO-EXISTE'", "actividad inexistente"),
        "repetido": ([{"actividad": CON_FIJA, "motivo": "x"}, {"actividad": CON_FIJA, "motivo": "y"}],
                     CON_FIJA, "mas de una vez"),
        "motivo vacio": ([{"actividad": CON_FIJA, "motivo": ""}], repr(CON_FIJA), "motivo vacio"),
        "motivo en blanco": ([{"actividad": CON_FIJA, "motivo": "  "}], repr(CON_FIJA), "motivo vacio"),
        "sin motivo": ([{"actividad": CON_FIJA}], repr(CON_FIJA), "motivo vacio"),
        "reunion": ([{"actividad": "RED", "motivo": "x"}], "RED", "es REUNION"),
        "funcion": ([{"actividad": "ORYCA-FIL1", "motivo": "x"}], "ORYCA-FIL1", "es FUNCION"),
    }

    def test_cada_caso_aborta_sin_enviar_y_nombra_el_identificador(self):
        for nombre, (lista, sujeto, motivo) in self.CASOS.items():
            with self.subTest(nombre), tempfile.TemporaryDirectory() as d:
                ruta = self.fichero_conservar(d, lista)
                rc, out = self.main(["--cargar", "--sin-aulas", "--conservar-aulas", str(ruta)])
                self.assertEqual(rc, 1, out)
                self.assertIn("ABORTA: la prevalidacion no esta limpia", out)
                linea = [x for x in out.splitlines() if x.startswith("  [conservarAula] ")]
                self.assertEqual(len(linea), 1, out)
                self.assertTrue(linea[0].startswith("  [conservarAula] %s: " % sujeto), linea)
                self.assertIn(motivo, linea[0])

    def test_en_prevalidacion_tambien_falla(self):
        with tempfile.TemporaryDirectory() as d:
            ruta = self.fichero_conservar(d, [{"actividad": "NO-EXISTE", "motivo": "x"}])
            rc, out = self.main(["--sin-aulas", "--conservar-aulas", str(ruta)])
        self.assertEqual(rc, 1)
        self.assertIn("conservarAula 1", " ".join(out.split()))

    def test_sin_la_clave_sale_sin_enviar(self):
        with tempfile.TemporaryDirectory() as d:
            ruta = Path(d, "reglas.json")
            ruta.write_text(json.dumps({"otraClave": []}), encoding="utf-8")
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(SystemExit) as cm, \
                    mock.patch.object(cc, "Cliente", side_effect=AssertionError("no debe enviar")):
                cc.main(["--cargar", "--sin-aulas", "--conservar-aulas", str(ruta),
                         "--catalogo", str(CURSO / "catalogo-derivado.json"),
                         "--nombres", str(CURSO / "nombres-derivados.json")])
        self.assertIn("conservarAula", str(cm.exception.code))


class T6Argumentos(Base):
    """F1.3: --conservar-aulas sin --sin-aulas es error de argumentos antes de leer nada."""

    def test_conservar_sin_el_modo_es_error_de_argparse(self):
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as cm:
                cc.main(["--catalogo", "/no/existe/catalogo.json", "--nombres", "/no/existe/nombres.json",
                         "--conservar-aulas", "/no/existe/conservar.json", "--cargar"])
        self.assertEqual(cm.exception.code, 2)
        self.assertIn("--conservar-aulas solo vale junto a --sin-aulas", err.getvalue())


class T8Escrituras(Base):
    """F1.6: las candidatas viajan dentro del POST de la actividad; el recuento no cambia con el modo."""

    def test_escrituras_iguales_a_las_previstas_en_el_modo_sin_aulas(self):
        for conservar in (frozenset(), frozenset({CON_FIJA, CON_CANDIDATAS})):
            cliente, _ = cargar_en_memoria(self.cat, sin_aulas=True, conservar=conservar)
            self.assertEqual((cliente.escrituras, cc.escrituras_previstas(self.cat)), (875, 875))

    def test_informe_final_cuadra_y_la_linea_del_modo(self):
        cliente = ClienteMemoria()
        rc, out = self.main(["--cargar", "--sin-aulas"], cliente=cliente)
        self.assertEqual(rc, 0, out)
        self.assertIn("escrituras", out)
        self.assertNotIn("DESAJUSTE", out)
        self.assertNotIn("DISTINTO DE LO PREVISTO", out)
        self.assertIn("MODO SIN AULAS: 310 plazas de CLASE sin aula, 0 plazas conservan aula, "
                      "0 actividades en la lista de conservar", out)

    def test_sin_el_modo_no_hay_linea_del_modo(self):
        rc, out = self.main(["--cargar"], cliente=ClienteMemoria())
        self.assertEqual(rc, 0, out)
        self.assertNotIn("MODO SIN AULAS", out)
        self.assertNotIn("modo sin aulas", out)


class T9RutaRelativa(Base):
    """F1.3: la ruta de --conservar-aulas se normaliza a absoluta al leerla."""

    def test_ruta_relativa_a_absoluta(self):
        antes = os.getcwd()
        with tempfile.TemporaryDirectory() as d:
            self.fichero_conservar(d, [{"actividad": CON_FIJA, "motivo": "taller de FPB"}])
            os.chdir(d)
            try:
                ruta, lista = cc.leer_conservar("conservar.json")
                rc, out = self.main(["--sin-aulas", "--conservar-aulas", "conservar.json"])
            finally:
                os.chdir(antes)
            absoluta = Path(d).resolve() / "conservar.json"
        self.assertTrue(ruta.is_absolute())
        self.assertEqual(ruta, absoluta)
        self.assertEqual(lista, [{"actividad": CON_FIJA, "motivo": "taller de FPB"}])
        self.assertEqual(rc, 0, out)
        self.assertIn("conservar aulas: %s (1 actividades)" % absoluta, out)


if __name__ == "__main__":
    unittest.main()
