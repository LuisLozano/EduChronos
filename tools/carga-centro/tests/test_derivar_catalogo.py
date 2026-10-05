#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de derivar-catalogo.py (S198, C-catalogo-2026).

El oráculo lee los volcados y las decisiones versionados de 2025/2026 y compara con
docs/horario-referencia/catalogo-derivado.json. El resto son volcados sintéticos que el propio
test escribe en un directorio temporal: una regla por test.
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

dc = cargar("derivar_catalogo", "derivar-catalogo.py")

HR = RAIZ / "docs" / "horario-referencia"
DEC_BASE = [
    {"id": "horas", "fija": "jornada.horas", "valor": {"1": ["08:00", "09:00"], "2": ["09:00", "10:00"],
                                                       "3": ["10:00", "11:00"], "4": ["11:30", "12:30"]},
     "motivo": "prueba", "fuente": "sin fuente"},
    {"id": "orden", "fija": "niveles.orden",
     "valor": ["1ESO", "2ESO", "3ESO", "4ESO", "1BACH", "2BACH", "1FPB", "2FPB"], "motivo": "prueba", "fuente": "sin fuente"},
    {"id": "tipos", "fija": "aulas.tipo", "valor": {"porDefecto": "ORDINARIA", "porCodigo": {}},
     "motivo": "prueba", "fuente": "sin fuente"},
]
# Dos grupos de 1º ESO con su tutoría: el mínimo que deriva sin decisiones de tutor.
BASE = {
    "1º ESO A": [(1, 1, "Mat", "MAT1", "A1"), (2, 1, "Mat", "MAT1", "A1"), (1, 2, "TUT1", "TUA", "A1")],
    "1º ESO B": [(1, 1, "Ing", "ING1", "B1"), (1, 2, "TUT1", "TUB", "B1")],
}


def escribir_volcado(directorio, grupos):
    for i, (titulo, celdas) in enumerate(sorted(grupos.items())):
        doc = {"_meta": {"fuente": "sintetico", "pagina": i + 1, "codigo_crudo": titulo, "modo": "grupos"},
               "celdas": [{"dia": d, "tramo": t, "asignatura": a, "profesor": p, "aula": au,
                           "confianza": "alta", "nota": ""} for d, t, a, p, au in celdas]}
        Path(directorio, "grupo-%02d.json" % i).write_text(json.dumps(doc, ensure_ascii=False), encoding="utf-8")


def con(extra=None, base=BASE):
    g = {k: list(v) for k, v in base.items()}
    for k, v in (extra or {}).items():
        g.setdefault(k, [])
        g[k] += v
    return g


class Base(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)
        self.volcados = self.tmp / "volcados"
        self.volcados.mkdir()

    def tearDown(self):
        self._tmp.cleanup()

    def derivar(self, grupos, decisiones=(), parche=None):
        escribir_volcado(self.volcados, grupos)
        return dc.derivar(str(self.volcados), {"decisiones": DEC_BASE + list(decisiones)}, parche)

    def act(self, catalogo, codigo):
        return next(a for a in catalogo["actividades"] if a["codigo"] == codigo)

    def main(self, grupos, decisiones=(), parche=None, salida=None, crudas=None):
        """Corre la CLI; devuelve (rc, stderr, carpeta de salida)."""
        escribir_volcado(self.volcados, grupos)
        fdec = self.tmp / "decisiones.json"
        fdec.write_text(json.dumps(crudas if crudas is not None else {"decisiones": DEC_BASE + list(decisiones)},
                                   ensure_ascii=False), encoding="utf-8")
        argv = ["--volcados", str(self.volcados), "--decisiones", str(fdec)]
        if parche is not None:
            fpa = self.tmp / "parche.json"
            fpa.write_text(json.dumps(parche, ensure_ascii=False), encoding="utf-8")
            argv += ["--parche", str(fpa)]
        salida = salida or str(self.tmp / "salida")
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            rc = dc.main(argv + ["--salida", salida])
        return rc, err.getvalue(), Path(salida)


# ------------------------------------------------------------------- oráculo

class Oraculo(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        decisiones = json.loads((HR / "decisiones-catalogo.json").read_text(encoding="utf-8"))
        cls.catalogo, cls.ctx = dc.derivar(str(HR), decisiones)
        cls.versionado = (HR / "catalogo-derivado.json").read_text(encoding="utf-8")

    def test_igual_en_json_al_catalogo_de_2025(self):
        self.assertEqual(self.catalogo, json.loads(self.versionado))

    def test_identico_byte_a_byte(self):
        self.assertEqual(json.dumps(self.catalogo, ensure_ascii=False, indent=1), self.versionado)

    def test_conservacion_1301_sin_divergencias(self):
        cons = self.ctx["cons"]
        self.assertEqual((cons["volcado"], cons["catalogo"], cons["comunes"], cons["explicadas"]), (1301, 1301, 1301, []))

    def test_requiere_tutor_en_todas_las_actividades(self):
        self.assertTrue(all(isinstance(a.get("requiereTutor"), bool) for a in self.catalogo["actividades"]))
        self.assertEqual(sum(a["requiereTutor"] for a in self.catalogo["actividades"]), 16)


class Curso2026(unittest.TestCase):
    """Regresión: el catálogo versionado de 2026/2027 es lo que dan sus volcados, decisiones y parche."""

    @classmethod
    def setUpClass(cls):
        d = HR / "2026-2027"
        cls.catalogo, cls.ctx = dc.derivar(str(d), json.loads((d / "decisiones-catalogo.json").read_text(encoding="utf-8")),
                                           json.loads((d / "parche-aulas.json").read_text(encoding="utf-8")))
        cls.versionado = json.loads((d / "catalogo-derivado.json").read_text(encoding="utf-8"))

    def test_igual_al_versionado(self):
        self.assertEqual(self.catalogo, self.versionado)

    def test_parche_18_sesiones_en_12_plazas_todas_con_aula_fija(self):
        cambios = self.ctx["cambios"]
        self.assertEqual(len(cambios), 18)
        self.assertEqual(len({(c["actividad"], c["plaza"]) for c in cambios}), 12)
        self.assertTrue(all(c["despues"][0] and not c["despues"][1] for c in cambios))


# ---------------------------------------------------------- reglas, sintéticos

class Reglas(Base):

    def test_particion_una_sesion_con_dos_grupos_y_sufijo_de_nivel(self):
        cat, _ = self.derivar(con({"1º ESO A": [(3, 1, "Rel", "REL1", "A2")], "1º ESO B": [(3, 1, "Rel", "REL1", "A2")]}))
        a = self.act(cat, "Rel-1ESO")
        self.assertEqual(len(a["plazas"]), 1)
        self.assertEqual(a["plazas"][0]["subgrupos"], ["1ºA-Completo", "1ºB-Completo"])
        self.assertEqual(a["_referencia"]["gruposTocados"], ["1ºA", "1ºB"])

    def test_codigo_con_grupos_si_no_cubre_el_nivel(self):
        base = con({"1º ESO C": [(1, 2, "TUT1", "TUC", "C1")]})
        cat, _ = self.derivar(con({"1º ESO A": [(3, 1, "Rel", "REL1", "A2")], "1º ESO B": [(3, 1, "Rel", "REL1", "A2")]}, base))
        self.act(cat, "Rel-1ºA+1ºB")

    def test_bloque_de_optativas_con_asignatura_nula(self):
        cat, _ = self.derivar(con({"1º ESO A": [(3, 2, "Fr2", "FRA1", "A3"), (3, 2, "ALCT", "LEN2", "A4")]}))
        a = self.act(cat, "Bloque-ALCT_Fr2-1ºA")
        self.assertIsNone(a["asignatura"])
        self.assertEqual([p["subgrupos"] for p in a["plazas"]], [["1ºA-ALCT"], ["1ºA-Fr2"]])

    def test_r5_profesor_en_el_nombre_si_hay_varias_combinaciones(self):
        cat, _ = self.derivar(con({"1º ESO A": [(3, 3, "RefMt", "MAT6", "A5"), (3, 3, "RefMt", "MAT7", "A6")]}))
        a = self.act(cat, "Bloque-RefMt-1ºA")
        self.assertEqual(a["asignatura"], "RefMt")
        self.assertEqual([p["subgrupos"] for p in a["plazas"]], [["1ºA-RefMt-MAT6"], ["1ºA-RefMt-MAT7"]])

    def test_codocencia_una_plaza_con_dos_profesores(self):
        cat, _ = self.derivar(con({"1º ESO A": [(4, 1, "LCL", "LEN2", "A1"), (4, 1, "LCL", "LEN8", None)]}))
        a = self.act(cat, "LCL-1ºA")
        self.assertEqual([(p["profesores"], p["aulaFija"], p["subgrupos"]) for p in a["plazas"]],
                         [(["LEN2", "LEN8"], "A1", ["1ºA-Completo"])])

    def test_desdoble_dos_plazas_si_las_dos_aulas_son_distintas(self):
        cat, _ = self.derivar(con({"1º ESO A": [(4, 1, "LCL", "LEN2", "A1"), (4, 1, "LCL", "LEN8", "A7")]}))
        a = self.act(cat, "Bloque-LCL-1ºA")
        self.assertEqual([(p["profesores"], p["aulaFija"]) for p in a["plazas"]], [(["LEN2"], "A1"), (["LEN8"], "A7")])

    def test_aula_una_sola_da_aula_fija(self):
        cat, _ = self.derivar(BASE)
        p = self.act(cat, "Mat-1ºA")["plazas"][0]
        self.assertEqual((p["aulaFija"], p["aulasCandidatas"]), ("A1", []))

    def test_aula_dos_dan_candidatas_exactamente_esas_dos(self):
        base = dict(BASE, **{"1º ESO A": [(1, 1, "Mat", "MAT1", "A1"), (2, 1, "Mat", "MAT1", "B2"), (1, 2, "TUT1", "TUA", "A1")]})
        cat, _ = self.derivar(base)
        p = self.act(cat, "Mat-1ºA")["plazas"][0]
        self.assertEqual((p["aulaFija"], p["aulasCandidatas"]), (None, ["A1", "B2"]))

    def test_alias_de_aula(self):
        cat, _ = self.derivar(BASE, [{"id": "al", "fija": "aulas.alias", "valor": {"A1": "A1 Larga"},
                                      "motivo": "m", "fuente": "f"}])
        self.assertEqual(self.act(cat, "Mat-1ºA")["plazas"][0]["aulaFija"], "A1 Larga")
        self.assertIn("A1 Larga", [a["codigo"] for a in cat["aulas"]])

    def test_plaza_sin_aula_y_sin_decision_aborta(self):
        with self.assertRaises(dc.Aborto) as e:
            self.derivar(con({"1º ESO A": [(3, 1, "Taller", "PAU1", None)]}))
        self.assertIn("sin aula", str(e.exception))

    def test_plaza_sin_aula_la_fija_la_decision_por_grupo(self):
        cat, ctx = self.derivar(con({"1º ESO A": [(3, 1, "Taller", "PAU1", None)]}),
                                [{"id": "sa", "fija": "plazas.aulaSinVolcado", "valor": {"1ºA": "Taller 4"},
                                  "motivo": "m", "fuente": "f"}])
        self.assertEqual(self.act(cat, "Taller-1ºA")["plazas"][0]["aulaFija"], "Taller 4")
        self.assertEqual(ctx["dec"].usos["sa"], 1)

    def test_patron_temporal(self):
        cat, _ = self.derivar(con({"1º ESO B": [(3, 1, "Fis", "FIS1", "B1"), (3, 3, "Fis", "FIS1", "B1"),
                                                (4, 1, "Geo", "GH1", "B1")]}))
        self.assertEqual(self.act(cat, "Mat-1ºA")["patronTemporal"], "DISTRIBUIDA")
        self.assertEqual(self.act(cat, "Fis-1ºB")["patronTemporal"], "NEUTRA")
        self.assertEqual(self.act(cat, "Geo-1ºB")["patronTemporal"], "NEUTRA")

    def test_repeticiones_e_instancias(self):
        cat, _ = self.derivar(BASE)
        a = self.act(cat, "Mat-1ºA")
        self.assertEqual((a["repeticionesPorSemana"], a["_referencia"]["instanciasEnElHorarioReal"]), (2, ["L1", "M1"]))

    def test_grupos_nivel_tipo_y_padre_y_subgrupo_automatico(self):
        cat, _ = self.derivar({
            "3º ESO A": [(1, 1, "TUT3", "T3", "A1"), (1, 2, "EF", "EFI1", "Gim")],
            "3º ESO A PDC": [(1, 1, "TUT3", "T3", "A1"), (1, 2, "ÁmbCM", "MAT4", "A8")],
            "1ºBACH A Ciencias": [(1, 1, "PTVE", "FIL2", "B1")],
        })
        g = {x["codigo"]: (x["nivel"], x["tipo"], x["grupoPadre"]) for x in cat["grupos"]}
        self.assertEqual(g, {"3ºA": ("3ESO", "ORDINARIO", None), "3ºADi": ("3ESO", "DIVERSIFICACION_PDC", "3ºA"),
                             "1B-Ac": ("1BACH", "ORDINARIO", None)})
        auto = [s["codigo"] for s in cat["subgrupos"] if s["creadoAutomaticamentePorPDC"]]
        self.assertEqual(auto, ["3ºADi-Completo"])
        self.assertEqual([n["codigo"] for n in cat["niveles"]], ["3ESO", "1BACH"])
        self.assertEqual([n["orden"] for n in cat["niveles"]], [3, 5])

    def test_tutoria_derivada_y_fijada_por_decision(self):
        cat, _ = self.derivar(BASE, [{"id": "tu", "fija": "tutorias.tutorPrincipal", "valor": {"1ºB": "TUB"},
                                      "motivo": "m", "fuente": "f"}])
        t = {x["grupo"]: (x["tutorPrincipal"], x["candidatos"]) for x in cat["tutorias"]}
        self.assertEqual(t, {"1ºA": ("TUA", ["TUA"]), "1ºB": ("TUB", [])})

    def test_tutor_fijado_que_no_imparte_la_tutoria_rompe_s8(self):
        with self.assertRaises(dc.Aborto) as e:
            self.derivar(BASE, [{"id": "tu", "fija": "tutorias.tutorPrincipal", "valor": {"1ºB": "ING1"},
                                 "motivo": "m", "fuente": "f"}])
        self.assertIn("S8: ['TUT1-1ºB']", str(e.exception))

    def test_grupo_sin_tutor_y_sin_decision_aborta(self):
        with self.assertRaises(dc.Aborto):
            self.derivar(con({"1º ESO C": [(1, 1, "Mat", "MAT2", "C1")]}))

    def test_requiere_tutor_por_regla_y_por_decision(self):
        cat, _ = self.derivar(BASE)
        self.assertEqual({a["codigo"]: a["requiereTutor"] for a in cat["actividades"]},
                         {"Mat-1ºA": False, "Ing-1ºB": False, "TUT1-1ºA": True, "TUT1-1ºB": True})
        cat, _ = self.derivar(BASE, [{"id": "rt", "fija": "actividades.requiereTutor",
                                      "valor": {"asignaturas": ["TUT1"], "requiereTutor": False},
                                      "motivo": "m", "fuente": "f"}])
        self.assertTrue(all("requiereTutor" in a and a["requiereTutor"] is False for a in cat["actividades"]))

    def test_jornada_con_recreo_insertado(self):
        cat, _ = self.derivar(BASE, [{"id": "re", "fija": "jornada.recreo",
                                      "valor": {"despuesDelTramo": 1, "horaInicio": "09:00", "horaFin": "09:30"},
                                      "motivo": "m", "fuente": "f"}])
        dt = cat["jornada"]["diaTipo"]
        self.assertEqual([(t["orden"], t["esLectivo"], t["tramoVolcado"]) for t in dt],
                         [(1, True, 1), (2, False, None), (3, True, 2)])
        self.assertEqual(cat["jornada"]["dias"], ["L", "M"])

    def test_nivel_sin_orden_aborta(self):
        dec = [dict(DEC_BASE[1], valor=["2ESO"])]
        escribir_volcado(self.volcados, BASE)
        with self.assertRaises(dc.Aborto):
            dc.derivar(str(self.volcados), {"decisiones": [DEC_BASE[0], DEC_BASE[2]] + dec})

    def test_aula_sin_tipo_aborta(self):
        escribir_volcado(self.volcados, BASE)
        dec = [DEC_BASE[0], DEC_BASE[1], dict(DEC_BASE[2], valor={"porDefecto": None, "porCodigo": {"A1": "ORDINARIA"}})]
        with self.assertRaises(dc.Aborto) as e:
            dc.derivar(str(self.volcados), {"decisiones": dec})
        self.assertIn("sin tipo", str(e.exception))


# --------------------------------------------------------------- conservación

class Conservacion(Base):

    def test_hora_perdida_aborta_y_nombra_la_clave(self):
        original = dc.expandir
        with mock.patch.object(dc, "expandir", lambda c: original(c) - {("1ºA", 2, 1, "Mat", "MAT1")}):
            rc, err, salida = self.main(BASE)
        self.assertEqual(rc, 1)
        self.assertIn("solo en el volcado: ('1ºA', 2, 1, 'Mat', 'MAT1')", err)
        self.assertFalse(salida.exists() and os.listdir(salida))

    def test_hora_inventada_aborta_y_nombra_la_clave(self):
        original = dc.expandir
        with mock.patch.object(dc, "expandir", lambda c: original(c) | {("1ºB", 3, 3, "Ing", "ING1")}):
            rc, err, _ = self.main(BASE)
        self.assertEqual(rc, 1)
        self.assertIn("solo en el catálogo: ('1ºB', 3, 3, 'Ing', 'ING1')", err)

    def test_divergencia_explicada_por_decision_pasa_y_se_cita(self):
        dec = [{"id": "fuera-guardia", "fija": "conservacion.excluirCeldas", "valor": [["1ºB", 1, 1, "Ing", "ING1"]],
                "motivo": "no es una clase", "fuente": "sin fuente"}]
        rc, err, salida = self.main(BASE, dec)
        self.assertEqual(rc, 0, err)
        informe = (salida / dc.SALIDA_INFORME).read_text(encoding="utf-8")
        self.assertIn("solo en el volcado `('1ºB', 1, 1, 'Ing', 'ING1')`: explicada por la decisión `fuera-guardia`", informe)
        cat = json.loads((salida / dc.SALIDA_CATALOGO).read_text(encoding="utf-8"))
        self.assertNotIn("Ing-1ºB", [a["codigo"] for a in cat["actividades"]])

    def test_exclusion_de_celda_inexistente_aborta(self):
        dec = [{"id": "x", "fija": "conservacion.excluirCeldas", "valor": [["1ºB", 5, 5, "Ing", "ING1"]],
                "motivo": "m", "fuente": "f"}]
        rc, _, _ = self.main(BASE, dec)
        self.assertEqual(rc, 2)


# ---------------------------------------------------------------- decisiones

class Decisiones(Base):

    def test_decision_sin_motivo_aborta(self):
        sin = {k: v for k, v in DEC_BASE[0].items() if k != "motivo"}
        rc, err, salida = self.main(BASE, crudas={"decisiones": [sin] + DEC_BASE[1:]})
        self.assertEqual(rc, 2)
        self.assertIn("motivo", err)
        self.assertFalse(salida.exists() and os.listdir(salida))

    def test_decision_con_motivo_vacio_aborta(self):
        rc, err, _ = self.main(BASE, crudas={"decisiones": [dict(DEC_BASE[0], motivo="  ")] + DEC_BASE[1:]})
        self.assertEqual(rc, 2)
        self.assertIn("«motivo» vacío", err)

    def test_decision_sin_fuente_aborta(self):
        rc, err, _ = self.main(BASE, crudas={"decisiones": [dict(DEC_BASE[0], fuente="")] + DEC_BASE[1:]})
        self.assertEqual(rc, 2)
        self.assertIn("«fuente» vacío", err)

    def test_fija_desconocido_aborta(self):
        rc, _, _ = self.main(BASE, crudas={"decisiones": DEC_BASE + [dict(DEC_BASE[0], id="z", fija="otra.cosa")]})
        self.assertEqual(rc, 2)


# --------------------------------------------------------------------- parche

class Parche(Base):

    ENTRADA = {"profesor": "Profesor Uno", "pagina": 1, "dia": 1, "tramo": 1, "asignatura": "Mat",
               "grupos": "1ºA", "aula_antes": "A1", "aula_despues": "A9", "confirmacion": "pendiente"}

    def test_cambia_la_plaza_indicada_y_solo_esa_y_lo_lista(self):
        sin, _ = self.derivar(BASE)
        con_parche, ctx = self.derivar(BASE, parche={"entradas": [self.ENTRADA]})
        p = self.act(con_parche, "Mat-1ºA")["plazas"][0]
        self.assertEqual((p["aulaFija"], p["aulasCandidatas"]), (None, ["A1", "A9"]))
        for a in sin["actividades"]:
            if a["codigo"] != "Mat-1ºA":
                self.assertEqual(a, self.act(con_parche, a["codigo"]))
        self.assertEqual([(c["actividad"], c["plaza"], c["antes"], c["despues"]) for c in ctx["cambios"]],
                         [("Mat-1ºA", 1, ("A1", ()), (None, ("A1", "A9")))])
        rc, err, salida = self.main(BASE, parche={"entradas": [self.ENTRADA]})
        self.assertEqual(rc, 0, err)
        informe = (salida / dc.SALIDA_INFORME).read_text(encoding="utf-8")
        self.assertIn("| 1 | Profesor Uno | MAT1 | L1 | Mat | 1ºA | A1 → A9 | `Mat-1ºA` · 1 (Mat/MAT1) | fija A1 → candidatas A1, A9 | pendiente |", informe)

    def test_todas_las_sesiones_de_la_plaza_dan_aula_fija_nueva(self):
        dos = dict(self.ENTRADA, dia=2)
        cat, _ = self.derivar(BASE, parche={"entradas": [self.ENTRADA, dos]})
        p = self.act(cat, "Mat-1ºA")["plazas"][0]
        self.assertEqual((p["aulaFija"], p["aulasCandidatas"]), ("A9", []))
        aulas = {a["codigo"]: (a["usadaPorCatalogo"], a["celdasEnVolcado"]) for a in cat["aulas"]}
        self.assertEqual(aulas["A9"], (True, 0))
        self.assertEqual(aulas["A1"], (True, 3))

    def test_sesion_que_no_cae_en_ninguna_plaza_aborta(self):
        rc, err, _ = self.main(BASE, parche={"entradas": [dict(self.ENTRADA, tramo=3)]})
        self.assertEqual(rc, 2)
        self.assertIn("no cae en una sesión única", err)

    def test_aula_antes_que_no_es_la_del_volcado_aborta(self):
        with self.assertRaises(dc.Aborto):
            self.derivar(BASE, parche={"entradas": [dict(self.ENTRADA, aula_antes="Z9")]})


# ----------------------------------------------------------------------- CLI

class Salida(Base):

    def test_salida_no_vacia_aborta(self):
        salida = self.tmp / "llena"
        salida.mkdir()
        (salida / "algo").write_text("x", encoding="utf-8")
        rc, err, _ = self.main(BASE, salida=str(salida))
        self.assertEqual(rc, 2)
        self.assertIn("no está vacía", err)
        self.assertEqual(os.listdir(salida), ["algo"])

    def test_salida_relativa_aborta(self):
        rc, _, _ = self.main(BASE, salida="relativa")
        self.assertEqual(rc, 2)

    def test_salida_escribe_los_dos_ficheros(self):
        rc, err, salida = self.main(BASE)
        self.assertEqual(rc, 0, err)
        self.assertEqual(set(os.listdir(salida)), {dc.SALIDA_CATALOGO, dc.SALIDA_INFORME})


if __name__ == "__main__":
    unittest.main()
