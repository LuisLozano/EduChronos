#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de derivar-guardias.py (S216 T1; S222 T1: recreo y funciones) con páginas de profesor fabricadas aquí.

La leyenda «Profesores:» es un diccionario y pasa por el mapa_profesores de verdad; en main se
sustituye mapa_de_paginas para no leer PDF.
"""
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

dg = cargar("derivar_guardias", "derivar-guardias.py")
cr = dg.dc._cruzar()

LEYENDA = {"ANA": "Ana Uno, María", "BEA": "Bea Dos", "CID": "Cid Tres, Ro"}


def celda(dia, tramo, asignatura):
    return {"dia": dia, "tramo": tramo, "asignatura": asignatura, "grupos": "", "aula": None,
            "confianza": "alta", "nota": ""}


def pagina(n, titulo, celdas=(), recreo=()):
    return {"_meta": {"fuente": "Horarios de profesores (6).pdf", "pagina": n, "codigo_crudo": titulo,
                      "modo": "profesores"},
            "celdas": [celda(*c) for c in celdas],
            "recreo": [{"dia": d, "asignatura": a, "grupos": "", "aula": None, "confianza": "alta", "nota": ""}
                       for d, a in recreo]}


def decision(id_, fija, valor):
    return {"id": id_, "fija": fija, "valor": valor, "motivo": "test", "fuente": "sin fuente"}


def paginas_base():
    """Cuatro páginas con código por la leyenda (1, 2, 3), una por alta (4) y una ignorada (5)."""
    return [
        pagina(1, "Ana Uno, María José", [(1, 1, "G"), (1, 2, "Geo"), (2, 3, "Gbibl"), (3, 4, "GeH"), (4, 5, "G")],
               recreo=[(2, "GR"), (3, "GRBib"), (4, "G"), (5, "Gbibl")]),
        pagina(2, "Bea Dos Tres", [(5, 6, "Mat"), (2, 1, "conv"), (1, 6, "GRI")]),
        pagina(3, "Cid Tres, Rosa", [(3, 3, "G"), (3, 2, "Gbibl"), (1, 1, "Lengua")], recreo=[(1, "GRBib")]),
        pagina(4, "Lobato Sin Leyenda", [(1, 1, "RED")]),
        pagina(5, "Ignorada Con Guardias", [(2, 2, "G"), (3, 3, "G"), (4, 4, "conv")], recreo=[(1, "GR")]),
    ]


DECISIONES = {"_meta": {}, "decisiones": [
    decision("alta", "profesores.alta", {"PRV": {"pagina": 4, "titulo": "Lobato Sin Leyenda",
                                                  "nombreCompleto": "Lobato Sin Leyenda"}}),
    decision("ignoradas", "profesores.paginasIgnoradas", {"5": "solo guardias"}),
]}
CATALOGO = {"profesores": [{"codigo": c} for c in ("ANA", "BEA", "CID", "PRV")]}


def derivar(paginas=None, decisiones=None, catalogo=None, leyenda=None):
    paginas = paginas if paginas is not None else paginas_base()
    mapa = cr.mapa_profesores({p["_meta"]["pagina"]: p["_meta"]["codigo_crudo"] for p in paginas},
                              leyenda or LEYENDA)
    return dg.derivar(paginas, mapa, cr.SIN_CODIGO, copy.deepcopy(decisiones or DECISIONES),
                      copy.deepcopy(catalogo or CATALOGO))


class Derivar(unittest.TestCase):

    def test_solo_codigos_exactos_y_sin_recreo(self):
        r = derivar()
        self.assertEqual({p["codigo"]: p["guardiasOrdinarias"] for p in r["profesores"]},
                         {"ANA": 2, "BEA": 0, "CID": 1, "PRV": 0})

    def test_los_de_cero_guardias_salen(self):
        r = derivar()
        self.assertIn({"codigo": "BEA", "guardiasOrdinarias": 0, "guardiasRecreo": 0}, r["profesores"])
        self.assertIn({"codigo": "PRV", "guardiasOrdinarias": 0, "guardiasRecreo": 0}, r["profesores"])

    def test_recreo_cuenta_gr_y_grbib_y_nada_mas(self):
        # K1 (S222). ANA: GR y GRBib cuentan, G y Gbibl en «recreo» no; CID solo tiene GRBib (M1).
        self.assertEqual({p["codigo"]: p["guardiasRecreo"] for p in derivar()["profesores"]},
                         {"ANA": 2, "BEA": 0, "CID": 1, "PRV": 0})

    def test_recreo_dia_fuera_de_rango_aborta(self):
        paginas = paginas_base()
        paginas[1]["recreo"].append({"dia": 6, "asignatura": "GR", "grupos": "", "aula": None})
        self.aborta("recreo fuera de rango", paginas=paginas)

    def test_dos_guardias_de_recreo_el_mismo_dia_aborta(self):
        # El volcado no trae franja: un recreo es un día (s218/m2/INFORME-M2.md:41).
        paginas = paginas_base()
        paginas[0]["recreo"].append({"dia": 2, "asignatura": "GRBib", "grupos": "", "aula": None})
        self.aborta("dos guardias de recreo el mismo día: [2]", paginas=paginas)

    def test_funciones_una_por_profesor_y_asignatura(self):
        # K2 (S222): Gbibl y conv ya no son «No puede».
        r = derivar()
        self.assertNotIn("noPuede", r)
        self.assertEqual(r["funciones"], [
            {"profesor": "ANA", "asignatura": "Gbibl", "celdas": [{"dia": 2, "ordenEnDia": 3}]},
            {"profesor": "BEA", "asignatura": "conv", "celdas": [{"dia": 2, "ordenEnDia": 1}]},
            {"profesor": "CID", "asignatura": "Gbibl", "celdas": [{"dia": 3, "ordenEnDia": 2}]},
        ])

    def test_funciones_ordenadas_y_celdas_por_dia_y_tramo(self):
        paginas = paginas_base()
        paginas[0]["celdas"] = [celda(4, 2, "Gbibl"), celda(1, 5, "conv"), celda(1, 3, "Gbibl")]
        self.assertEqual([f for f in derivar(paginas)["funciones"] if f["profesor"] == "ANA"], [
            {"profesor": "ANA", "asignatura": "Gbibl", "celdas": [{"dia": 1, "ordenEnDia": 3}, {"dia": 4, "ordenEnDia": 2}]},
            {"profesor": "ANA", "asignatura": "conv", "celdas": [{"dia": 1, "ordenEnDia": 5}]},
        ])

    def test_profesores_por_codigo_aunque_las_paginas_vengan_en_otro_orden(self):
        r = derivar(list(reversed(paginas_base())))
        self.assertEqual([p["codigo"] for p in r["profesores"]], ["ANA", "BEA", "CID", "PRV"])

    def test_profesores_por_codigo_y_no_por_numero_de_pagina(self):
        # codigos_de_paginas recorre las páginas por número: CID en la 1 y ANA en la 3 (M8 de S216).
        paginas = paginas_base()
        paginas[0]["_meta"]["pagina"], paginas[2]["_meta"]["pagina"] = 3, 1
        r = derivar(paginas)
        self.assertEqual([p["codigo"] for p in r["profesores"]], ["ANA", "BEA", "CID", "PRV"])

    def test_omitidos_con_sus_cuentas(self):
        self.assertEqual(derivar()["omitidos"],
                         [{"pagina": 5, "titulo": "Ignorada Con Guardias", "G": 2, "Gbibl": 0, "conv": 1,
                           "GR": 1, "GRBib": 0}])

    def aborta(self, texto, **kw):
        with self.assertRaises(dg.dc.Aborto) as ctx:
            derivar(**kw)
        self.assertEqual(ctx.exception.rc, 2)
        self.assertIn(texto, str(ctx.exception))

    def test_dia_fuera_de_rango_aborta(self):
        paginas = paginas_base()
        paginas[1]["celdas"].append(celda(6, 1, "Mat"))
        self.aborta("fuera de rango", paginas=paginas)

    def test_tramo_fuera_de_rango_aborta(self):
        paginas = paginas_base()
        paginas[2]["celdas"].append(celda(1, 7, "G"))
        self.aborta("fuera de rango", paginas=paginas)

    def test_tramo_cero_aborta(self):
        paginas = paginas_base()
        paginas[2]["celdas"].append(celda(1, 0, "Lengua"))
        self.aborta("fuera de rango", paginas=paginas)

    def test_pagina_con_guardias_sin_emparejar_ni_ignorar_aborta(self):
        paginas = paginas_base() + [pagina(6, "Nadie Nadie", [(1, 1, "G")])]
        self.aborta("página 6 (Nadie Nadie) sin código", paginas=paginas)

    def test_dos_volcados_en_el_mismo_codigo_aborta(self):
        paginas = paginas_base() + [pagina(6, "Ana Uno, María Otra", [(1, 1, "G")])]
        self.aborta("códigos de profesor en más de una página: ['ANA']", paginas=paginas)

    def test_celda_con_dos_codigos_de_guardia_aborta(self):
        paginas = paginas_base()
        paginas[2]["celdas"].append(celda(3, 3, "Gbibl"))
        self.aborta("dos códigos de guardia: [(3, 3)]", paginas=paginas)

    def test_codigo_del_catalogo_sin_volcado_aborta(self):
        catalogo = {"profesores": CATALOGO["profesores"] + [{"codigo": "ZOE"}]}
        self.aborta("sobran [], faltan ['ZOE']", catalogo=catalogo)

    def test_volcado_emparejado_fuera_del_catalogo_aborta(self):
        catalogo = {"profesores": [p for p in CATALOGO["profesores"] if p["codigo"] != "CID"]}
        self.aborta("sobran ['CID'], faltan []", catalogo=catalogo)


class Main(unittest.TestCase):

    def preparar(self, d, paginas=None):
        vol = d / "volcados"
        vol.mkdir()
        for p in paginas if paginas is not None else paginas_base():
            (vol / ("profesor-%s.json" % p["_meta"]["codigo_crudo"].replace(" ", "-"))).write_text(
                json.dumps(p, ensure_ascii=False), encoding="utf-8")
        for nombre, contenido in (("grupos.pdf", b"%PDF uno"), ("grupos (11).pdf", b"%PDF dos")):
            (d / nombre).write_bytes(contenido)
        (d / "decisiones.json").write_text(json.dumps(DECISIONES, ensure_ascii=False), encoding="utf-8")
        (d / "catalogo.json").write_text(json.dumps(CATALOGO, ensure_ascii=False), encoding="utf-8")
        return vol

    def correr(self, d, salida, vol=None, extra=None):
        args = ["--volcados-profesores", str(vol or d / "volcados"),
                "--pdf-grupos", str(d / "grupos.pdf"), str(d / "grupos (11).pdf"),
                "--decisiones", str(d / "decisiones.json"), "--catalogo", str(d / "catalogo.json"),
                "--salida", str(salida)] + (extra or [])

        def mapa_de_paginas(paginas, pdfs):
            return cr.mapa_profesores({p["_meta"]["pagina"]: p["_meta"]["codigo_crudo"] for p in paginas},
                                      LEYENDA), cr.SIN_CODIGO
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err), \
                mock.patch.object(dg.dc, "mapa_de_paginas", side_effect=mapa_de_paginas):
            rc = dg.main(args)
        return rc, out.getvalue(), err.getvalue()

    def test_dos_corridas_identicas_byte_a_byte_sin_rutas(self):
        with tempfile.TemporaryDirectory() as t:
            d = Path(t)
            self.preparar(d)
            rc1, _, e1 = self.correr(d, d / "uno.json")
            rc2, _, e2 = self.correr(d, d / "dos.json")
            self.assertEqual((rc1, rc2), (0, 0), e1 + e2)
            texto = (d / "uno.json").read_text(encoding="utf-8")
            self.assertEqual(texto, (d / "dos.json").read_text(encoding="utf-8"))
            self.assertNotIn(t, texto)
            salida = json.loads(texto)
            self.assertEqual(texto, json.dumps(salida, ensure_ascii=False, indent=1))
            self.assertEqual(list(salida), ["_meta", "profesores", "funciones"])
            self.assertEqual(list(salida["_meta"]), ["generadoPor", "entradas", "omitidos"])
            self.assertEqual(salida["_meta"]["generadoPor"], "tools/carga-centro/derivar-guardias.py")
            entradas = salida["_meta"]["entradas"]
            self.assertEqual([e["fichero"] for e in entradas["pdfGrupos"]], ["grupos.pdf", "grupos (11).pdf"])
            self.assertEqual(len(entradas["volcadosProfesores"]), 5)
            self.assertEqual(entradas["catalogo"]["fichero"], "catalogo.json")
            self.assertEqual(len(entradas["decisiones"]["sha256"]), 64)
            self.assertEqual(salida["_meta"]["omitidos"][0]["pagina"], 5)

    def test_aborto_no_escribe(self):
        with tempfile.TemporaryDirectory() as t:
            d = Path(t)
            paginas = paginas_base()
            paginas[0]["celdas"].append(celda(9, 1, "G"))
            self.preparar(d, paginas)
            rc, _, err = self.correr(d, d / "salida.json")
            self.assertEqual(rc, 2)
            self.assertIn("fuera de rango", err)
            self.assertFalse((d / "salida.json").exists())

    def test_ruta_relativa_aborta_sin_escribir(self):
        with tempfile.TemporaryDirectory() as t:
            d = Path(t)
            self.preparar(d)
            rc, _, err = self.correr(d, "relativa.json")
            self.assertEqual(rc, 2)
            self.assertIn("absolutas: ['relativa.json']", err)
            self.assertFalse(Path("relativa.json").exists())


if __name__ == "__main__":
    unittest.main()
