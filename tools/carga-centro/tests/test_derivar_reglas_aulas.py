#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de derivar-reglas-aulas.py (S209 T2) sobre un Excel, un catálogo y unos nombres sintéticos."""
import contextlib
import copy
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import cargar  # noqa: E402

dr = cargar("derivar_reglas_aulas", "derivar-reglas-aulas.py")

AMARILLO, NARANJA, AZUL, ROSA = "rgb=FFFFFF00", "rgb=FFFF9900", "rgb=FF6D9EEB", "rgb=FFC27BA0"
PERSONA = "Zacarías Pérez Quintanilla"
DISTINTIVO = "ZZ-TEXTO-DISTINTIVO-DEL-EXCEL"

# fila: (E, F, G, H, C mesas, D sillas, relleno de E)
HOJA1 = {
    2: ("A1", "1ºESO A", "", "", "30.0", "31.0", AMARILLO),
    3: ("Taller3 A", "1ºBCH A", "", "intentar " + DISTINTIVO, "29.0", "28.0", NARANJA),
    4: ("B1", "3ºESO A", "", "", "20.0", "veinte sillas", AMARILLO),
    5: ("B4(Ant. Empr)", "3ºESO DIVER", "Es mediana, caben 18 alumnos", "", "5+15", "5+15", AZUL),
    6: ("A2", "1ºESO B", "es pequeña, caben 12 alumnos máximo", "", "15.0", "40.0", ROSA),
    7: ("B12 (INFORM)", "AULA TIC", "si es necesario también robótica", "", "", "", AMARILLO),
    8: ("A25 (ATECA)", "", "", "", "", "", ""),
    9: ("COM2", "NO DISPONIBLE MAÑANA", "", "", "34.0", "34.0", ""),
    10: ("A3", "radio", "", "no usar", "", "", ""),
    11: ("A13 (MÚSICA)", "Aula MÚSICA", "si es posible sólo música", "", "24.0", "31.0", ""),
    12: ("B07", "TALLER DE TECNOLOGÍA", "", "sólo para tecnología", "15 mesas dobles", "30.0", ""),
    13: ("C00", "RELIGIÓN", "", "sólo religión", "", "", ""),
    14: ("A18", "AMPA", "", "aquí se da religión evangélica", "Ninguna", "15 sillas de pala", ""),
    15: ("A6", "LABORATORIO", "Para horas de %s, prácticas de laboratotioy anatomía. Si es necesario para horas de "
                              "física y química" % PERSONA, "", "34.0", "33.0", ""),
    16: ("A7", "AULA DIBUJO", "", "sólo dibujo", "", "", ""),
    17: ("A9", "AULA APOYO", "", "si es posible anatomía", "", "", ""),
    18: ("Co1", "AULA PLÁSTICA", "", "solo para plástica", "", "", ""),
    20: ("taller 2 no se puede usar, ni C02", "", "", "", "", "", ""),
}
NOTAS = {40: "Las B sólo llegan de las b01 a la b07", 41: "Las A van de la A1 a la A18",
         42: "Fop mejor en las A o B que tienen portátiles"}
HOJA2 = [("OPTATIVAS Y GRUPOS DE 1ºESO", "Nº ALUMNOS"),
         ("ALCT   1º ESO A", "8.0"),            # CASA-1
         ("CyR    1º ESO A+B", "25.0"),          # AMBIGUA: dos plazas
         ("FRA   1º ESO A+B", "20.0"),           # CASA-PLAZA
         ("OyD   1º ESO A", "9.0"),              # PARCIAL
         ("ALCT   1º ESO D", "7.0"),             # SIN CASACIÓN: no hay 1ºD
         ("REL   1º ESO B", "10+3 " + DISTINTIVO),  # CASA-1 con número ilegible
         ("", "")]

CATALOGO = {
    "aulas": [{"codigo": c} for c in ("A1", "A2", "A3", "A6", "A7", "A9", "A13", "A18", "B01", "B04", "B07",
                                      "C00", "TAL3a")],
    "grupos": [{"codigo": "1ºA", "nivel": "1ESO", "tipo": "ORDINARIO"},
               {"codigo": "1ºB", "nivel": "1ESO", "tipo": "ORDINARIO"},
               {"codigo": "3ºA", "nivel": "3ESO", "tipo": "ORDINARIO"},
               {"codigo": "3ºADi", "nivel": "3ESO", "tipo": "DIVERSIFICACION_PDC", "grupoPadre": "3ºA"},
               {"codigo": "1B-Ac", "nivel": "1BACH", "tipo": "ORDINARIO"},
               {"codigo": "1B-Am", "nivel": "1BACH", "tipo": "ORDINARIO"}],
    "subgrupos": [{"codigo": s, "grupos": [g]} for s, g in (
        ("1ºA-ALCT", "1ºA"), ("1ºA-CyR-1", "1ºA"), ("1ºB-CyR-1", "1ºB"), ("1ºA-CyR-2", "1ºA"), ("1ºB-CyR-2", "1ºB"),
        ("1ºA-Fr2", "1ºA"), ("1ºB-Fr2", "1ºB"), ("1ºA-OyD", "1ºA"), ("1ºB-OyD", "1ºB"), ("1ºB-Rel", "1ºB"))],
    "actividades": [
        {"codigo": "ALCT-1A", "plazas": [{"asignatura": "ALCT", "subgrupos": ["1ºA-ALCT"]}]},
        {"codigo": "CyR-1", "plazas": [{"asignatura": "CyR", "subgrupos": ["1ºA-CyR-1", "1ºB-CyR-1"]},
                                       {"asignatura": "CyR", "subgrupos": ["1ºA-CyR-2", "1ºB-CyR-2"]}]},
        {"codigo": "Fr2-1", "plazas": [{"asignatura": "Fr2", "subgrupos": ["1ºA-Fr2", "1ºB-Fr2"]}]},
        {"codigo": "OyD-1", "plazas": [{"asignatura": "OyD", "subgrupos": ["1ºA-OyD", "1ºB-OyD"]}]},
        {"codigo": "Rel-1B", "plazas": [{"asignatura": "Rel", "subgrupos": ["1ºB-Rel"]}]},
        {"codigo": "RED", "tipo": "REUNION", "plazas": [{"asignatura": "ALCT", "subgrupos": []}]},
    ],
}
NOMBRES = {"asignaturas": {c: {"nombreCompleto": n} for c, n in (
    ("Mús", "Música"), ("PLAB", "Prácticas de Laboratorio"), ("ANAT", "Anatomía"), ("TEC", "Tecnología"),
    ("Tec", "Tecnología y Digitalizac"), ("R1", "Religión"), ("R2", "Religión"), ("REVAN", "Religión Evangélica"),
    ("DT", "Dibujo Técnico"), ("FyQ", "Física y Química"), ("ROB", "Robótica"), ("PLAS", "Plástica"),
    ("ALCT", "ALCT"), ("Fr2", "Francés 2"))}}


def escapar(t):
    return t.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")


def escribir_entradas(d, hoja1=None, notas=None, hoja2=None, catalogo=None, nombres=None, comentario=DISTINTIVO):
    hoja1, notas = hoja1 if hoja1 is not None else HOJA1, notas if notas is not None else NOTAS
    d = Path(d)
    rej = ["\t" + "\t".join("ABCDEFGH"), "1\t\t\tMESAS\tSILLAS\tAULA\tGRUPOS\t\t"]
    cel = ["ref\tvalor\tformula\trelleno\tnegrita\tcomentario"]
    for fila in range(2, max(list(hoja1) + list(notas)) + 1):
        if fila in hoja1:
            e, f, g, h, c, dd, rel = hoja1[fila]
            rej.append("\t".join([str(fila), "", "", c, dd, e, f, g, h]))
            cel.append("\t".join(["E%d" % fila, e, "", rel, "sí", ""]))
            if dd:
                cel.append("\t".join(["D%d" % fila, dd, "", "", "no", escapar(comentario + "\nnota de celda")]))
        elif fila in notas:
            rej.append("\t".join([str(fila), "", notas[fila]] + [""] * 6))
        else:
            rej.append("\t".join([str(fila)] + [""] * 8))
    rutas = {"hoja1_rejilla": d / "h1-rejilla.tsv", "hoja1_celdas": d / "h1-celdas.tsv",
             "hoja2_rejilla": d / "h2-rejilla.tsv", "catalogo": d / "catalogo.json", "nombres": d / "nombres.json"}
    rutas["hoja1_rejilla"].write_text("\n".join(rej) + "\n", encoding="utf-8")
    rutas["hoja1_celdas"].write_text("\n".join(cel) + "\n", encoding="utf-8")
    h2 = ["\tA\tB"] + ["%d\t%s\t%s" % (i + 1, escapar(a), escapar(b)) for i, (a, b) in enumerate(hoja2 or HOJA2)]
    rutas["hoja2_rejilla"].write_text("\n".join(h2) + "\n", encoding="utf-8")
    rutas["catalogo"].write_text(json.dumps(catalogo or CATALOGO, ensure_ascii=False), encoding="utf-8")
    rutas["nombres"].write_text(json.dumps(nombres or NOMBRES, ensure_ascii=False), encoding="utf-8")
    return rutas


def correr(rutas, salida):
    argv = []
    for k in ("hoja1_rejilla", "hoja1_celdas", "hoja2_rejilla", "catalogo", "nombres"):
        argv += ["--" + k.replace("_", "-"), str(rutas[k])]
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        rc = dr.main(argv + ["--salida", str(salida)])
    return rc, out.getvalue()


class Base(unittest.TestCase):

    def derivar(self, **cambios):
        with tempfile.TemporaryDirectory() as d:
            rutas = escribir_entradas(d, **cambios)
            rc, out = correr(rutas, Path(d, "reglas.json"))
            self.assertEqual(rc, 0, out)
            crudo = Path(d, "reglas.json").read_bytes()
        return json.loads(crudo.decode("utf-8")), crudo

    @classmethod
    def setUpClass(cls):
        with tempfile.TemporaryDirectory() as d:
            rutas = escribir_entradas(d)
            rc, cls.out = correr(rutas, Path(d, "reglas.json"))
            assert rc == 0, cls.out
            cls.crudo = Path(d, "reglas.json").read_bytes()
        cls.j = json.loads(cls.crudo.decode("utf-8"))

    def omitidas(self, regla, celda=None):
        return [o for o in self.j["omitidas"] if o["regla"] == regla and (celda is None or celda in o["origen"]["celdas"])]

    def asig(self, j=None):
        return {r["asignatura"]: [(a["aula"], a["rol"]) for a in r["aulas"]] for r in (j or self.j)["aulasAsignatura"]}


class N1Aulas(Base):

    def test_v1_v2_v3(self):
        self.assertEqual(dr.casar_aula("A13 (MÚSICA)", {"A13"}, {}), ("A13", "V1"))
        self.assertEqual(dr.casar_aula("B1", {"B01"}, {}), ("B01", "V2"))
        self.assertEqual(dr.casar_aula("B4(Ant. Empr)", {"B04"}, {}), ("B04", "V1+V2"))
        self.assertEqual(dr.casar_aula("Taller3 A", {"TAL3a"}, {}), ("TAL3a", "V3"))
        ref = {r["grupo"]: r for r in self.j["aulaReferencia"]}
        self.assertEqual((ref["1B-Ac"]["aula"], ref["1B-Ac"]["procedencia"][0]["casacion"]), ("TAL3a", "V3"))
        self.assertEqual((ref["3ºA"]["aula"], ref["3ºA"]["procedencia"][0]["casacion"]), ("B01", "V2"))
        self.assertEqual(self.asig()["Mús"], [("A13", "PREFERIDA")])
        self.assertEqual(self.j["aulasAsignatura"][[r["asignatura"] for r in self.j["aulasAsignatura"]].index("Mús")]
                         ["aulas"][0]["procedencia"][0]["casacion"], "V1")

    def test_sin_lectura_y_fuera_de_rango_omitidas(self):
        aula = {o["origen"]["celdas"][0]: o["motivo"] for o in self.omitidas("aula")}
        self.assertEqual(aula, {"E7": dr.M_FUERA_DE_RANGO, "E8": dr.M_FUERA_DE_RANGO, "E9": dr.M_AULA_SIN_LECTURA,
                                "E18": dr.M_AULA_SIN_LECTURA, "E20": dr.M_AULA_SIN_LECTURA})
        self.assertEqual([o["motivo"] for o in self.omitidas("aulasAsignatura", "E7")], [dr.M_FUERA_DE_RANGO])
        self.assertEqual([o["motivo"] for o in self.omitidas("aulasAsignatura", "E18")], [dr.M_AULA_SIN_LECTURA])
        self.assertEqual([o["motivo"] for o in self.omitidas("usoAula")], [dr.M_AULA_SIN_LECTURA] * 2)

    def test_rango_sale_de_las_notas(self):
        self.assertEqual(dr.casar_aula("B12", {"B12"}, {"B": 7}), (None, dr.M_FUERA_DE_RANGO))
        self.assertEqual(dr.casar_aula("B12", {"B12"}, {}), ("B12", "EXACTA"))
        self.assertEqual(dr.casar_aula("A18", {"A18"}, {"A": 18}), ("A18", "EXACTA"))
        self.assertEqual([o["origen"]["celdas"] for o in self.omitidas("nota")], [["B42"]])


class N2Referencia(Base):

    def test_amarillas_y_naranjas_dan_aula_de_referencia(self):
        self.assertEqual({(r["grupo"], r["aula"]) for r in self.j["aulaReferencia"]},
                         {("1ºA", "A1"), ("1B-Ac", "TAL3a"), ("1B-Am", "TAL3a"), ("3ºA", "B01")})

    def test_fila_pdc_omitida(self):
        o = self.omitidas("aulaReferencia", "E5")
        self.assertEqual(len(o), 1)
        self.assertIn("PDC", o[0]["motivo"])
        self.assertEqual(o[0]["grupos"], ["3ºADi"])

    def test_color_que_no_es_amarillo_ni_naranja_omitido(self):
        o = self.omitidas("aulaReferencia", "E6")
        self.assertEqual([(x["motivo"], x["grupos"]) for x in o],
                         [("relleno de la fila que no es amarillo ni naranja", ["1ºB"])])


class N3Asignaturas(Base):

    def test_reglas(self):
        self.assertEqual(self.asig(), {"ANAT": [("A6", "PREFERIDA"), ("A9", "PREFERIDA")],
                                       "Mús": [("A13", "PREFERIDA")], "PLAB": [("A6", "PREFERIDA")],
                                       "R1": [("C00", "EXCLUSIVA")], "R2": [("C00", "EXCLUSIVA")],
                                       "REVAN": [("A18", "PREFERIDA")], "TEC": [("B07", "EXCLUSIVA")]})

    def test_solo_exclusiva_y_si_es_posible_preferida(self):
        self.assertEqual(self.asig()["TEC"], [("B07", "EXCLUSIVA")])
        self.assertEqual(self.asig()["Mús"], [("A13", "PREFERIDA")])      # «si es posible sólo»
        self.assertIn(("A9", "PREFERIDA"), self.asig()["ANAT"])          # «si es posible»
        self.assertEqual(self.asig()["REVAN"], [("A18", "PREFERIDA")])   # sin calificativo

    def test_si_es_necesario_omitida(self):
        o = self.omitidas("aulasAsignatura", "E15")
        self.assertEqual([x["motivo"] for x in o], ["calificativo «si es necesario»: no da rol"])
        self.assertNotIn("FyQ", self.asig())

    def test_nombre_exacto(self):
        self.assertNotIn("Tec", self.asig())                              # solo contiene «tecnología»
        self.assertEqual((self.asig()["R1"], self.asig()["R2"]), ([("C00", "EXCLUSIVA")],) * 2)
        self.assertNotIn("DT", self.asig())
        self.assertEqual([x["motivo"] for x in self.omitidas("aulasAsignatura", "E16")],
                         ["ninguna asignatura con ese nombre exacto"])

    def test_mezcla_de_roles_omitida(self):
        hoja1 = dict(HOJA1)
        hoja1[19] = ("A3", "AULA ANATOMÍA", "", "sólo anatomía", "", "", "")
        j, _ = self.derivar(hoja1=hoja1)
        self.assertNotIn("ANAT", self.asig(j))
        mezcla = [o for o in j["omitidas"] if o["motivo"].startswith("la asignatura tendría aulas EXCLUSIVA y PREFERIDA")]
        self.assertEqual(sorted(o["origen"]["celdas"][0] for o in mezcla), ["E15", "E17", "E19"])
        self.assertTrue(all(o["asignatura"] == "ANAT" for o in mezcla))
        self.assertIn("PLAB", self.asig(j))


class N4N5AulaYCapacidad(Base):

    def test_uso(self):
        self.assertEqual([(r["aula"], r["enUso"]) for r in self.j["usoAula"]], [("A3", False)])

    def test_capacidad_g_sillas_nada_y_las_mesas_no_cuentan(self):
        cap = {r["aula"]: r["capacidad"] for r in self.j["capacidad"]}
        self.assertEqual(cap["A1"], 31)        # sillas, no las 30 mesas
        self.assertEqual(cap["A2"], 12)        # G «caben 12» antes que 40 sillas
        self.assertEqual(cap["B04"], 18)       # G con mesas y sillas en texto
        self.assertEqual(cap["B07"], 30)       # sillas con mesas en texto
        self.assertNotIn("B01", cap)           # mesas 20, sillas en texto: nada
        self.assertNotIn("A18", cap)
        o = {x["aula"] for x in self.omitidas("capacidad")}
        self.assertTrue({"B01", "A18"} <= o)
        proc = next(r for r in self.j["capacidad"] if r["aula"] == "A1")["procedencia"][0]
        self.assertEqual(proc["celdas"], ["E2", "D2", "C2"])


class N6Alumnos(Base):

    def test_solo_filas_sin_ambiguedad(self):
        self.assertEqual([(r["subgrupo"], r["alumnos"]) for r in self.j["alumnos"]], [("1ºA-ALCT", 8)])
        motivos = {o["origen"]["celdas"][0]: o["motivo"].split(":")[0] for o in self.omitidas("alumnos")}
        self.assertEqual(motivos, {"A3": "AMBIGUA", "A4": "CASA-PLAZA", "A5": "PARCIAL", "A6": "SIN CASACIÓN",
                                   "A7": "CASA-1"})


class SalidaLimpia(Base):

    def test_ningun_texto_libre_ni_persona(self):
        texto = self.crudo.decode("utf-8")
        for prohibido in (DISTINTIVO, PERSONA, "Zacar", "intentar", "laboratotio", "mediana", "portátiles",
                          "INFORM", "ATECA", "Ant. Empr"):
            self.assertNotIn(prohibido, texto)

    def test_determinista(self):
        _, otra = self.derivar()
        self.assertEqual(otra, self.crudo)

    def test_listas_ordenadas(self):
        self.assertEqual([r["grupo"] for r in self.j["aulaReferencia"]], sorted(r["grupo"] for r in self.j["aulaReferencia"]))
        self.assertEqual([r["aula"] for r in self.j["capacidad"]], sorted(r["aula"] for r in self.j["capacidad"]))
        self.assertEqual([r["asignatura"] for r in self.j["aulasAsignatura"]],
                         sorted(r["asignatura"] for r in self.j["aulasAsignatura"]))

    def test_independiente_del_orden_del_catalogo(self):
        cat = copy.deepcopy(CATALOGO)
        for k in ("aulas", "grupos", "subgrupos", "actividades"):
            cat[k].reverse()
        j, _ = self.derivar(catalogo=cat)
        j["_meta"], base = None, dict(self.j, _meta=None)
        self.assertEqual(j, base)

    def test_conservar_aula_presente_y_vacia(self):
        self.assertIn("conservarAula", self.j)
        self.assertEqual(self.j["conservarAula"], [])

    def test_formato_de_volcado(self):
        self.assertEqual(self.crudo.decode("utf-8"), json.dumps(self.j, ensure_ascii=False, indent=1))


class Argumentos(unittest.TestCase):

    def test_falta_una_entrada_sale_sin_escribir(self):
        with tempfile.TemporaryDirectory() as d:
            rutas = escribir_entradas(d)
            rutas["nombres"].unlink()
            rc, out = correr(rutas, Path(d, "reglas.json"))
            self.assertNotEqual(rc, 0)
            self.assertFalse(Path(d, "reglas.json").exists())
            self.assertIn(str(rutas["nombres"]), out)

    def test_rutas_a_absolutas(self):
        import os
        antes = os.getcwd()
        with tempfile.TemporaryDirectory() as d:
            rutas = escribir_entradas(d)
            os.chdir(d)
            try:
                rc, out = correr({k: Path(v.name) for k, v in rutas.items()}, Path("reglas.json"))
            finally:
                os.chdir(antes)
            self.assertEqual(rc, 0, out)
            self.assertIn("salida:        %s" % (Path(d).resolve() / "reglas.json"), out)
            self.assertIn("catalogo       %s" % (Path(d).resolve() / "catalogo.json"), out)


if __name__ == "__main__":
    unittest.main()
