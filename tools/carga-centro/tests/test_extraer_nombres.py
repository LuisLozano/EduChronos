#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de extraer-nombres.py (S198: rutas por argumento, variantes de varios PDF, cierre antes
de escribir).

El oráculo lee el PDF de grupos de 2025/2026 versionado en docs/horario-referencia/pdf/. El
resto sustituye la lectura del PDF por páginas de texto fabricadas en el test (la maqueta de
`pdftotext -layout`: campos separados por dos o más espacios).
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

en = cargar("extraer_nombres", "extraer-nombres.py")

HR = RAIZ / "docs" / "horario-referencia"


def pagina(profesores, asignaturas, tutor=None):
    """Texto de una página: línea Tutor opcional y las dos leyendas, dos pares por línea."""
    lineas = ["Grupo: prueba"]
    if tutor:
        lineas.append("Tutor: %s" % tutor)
    for cabecera, pares in (("Profesores:", profesores), ("Asignaturas:", asignaturas)):
        lineas.append(cabecera)
        for i in range(0, len(pares), 2):
            lineas.append("    ".join("%s  %s" % p for p in pares[i:i + 2]))
        lineas.append("")
    return "\n".join(lineas)


class Base(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)

    def tearDown(self):
        self._tmp.cleanup()

    def correr(self, pdfs, catalogo, salida=None, decisiones=None):
        """pdfs: {nombre: [páginas]}. Devuelve (rc, stdout, ruta de salida)."""
        rutas = []
        for nombre in pdfs:
            ruta = self.tmp / nombre
            ruta.write_bytes(b"%PDF sintetico")
            rutas.append(str(ruta))
        cat = self.tmp / "catalogo.json"
        cat.write_text(json.dumps(catalogo), encoding="utf-8")
        salida = salida or self.tmp / "nombres.json"
        por_ruta = {str(self.tmp / n): p for n, p in pdfs.items()}
        out = io.StringIO()
        with mock.patch.object(en, "leer_paginas", lambda pdf: por_ruta[str(pdf)]), contextlib.redirect_stdout(out):
            extra = []
            if decisiones is not None:
                fdec = self.tmp / "decisiones.json"
                fdec.write_text(json.dumps({"decisiones": decisiones}, ensure_ascii=False), encoding="utf-8")
                extra = ["--decisiones", str(fdec)]
            rc = en.main(["--pdf"] + rutas + ["--catalogo", str(cat), "--salida", str(salida)] + extra)
        return rc, out.getvalue(), Path(salida)


def catalogo(profesores, asignaturas):
    return {"profesores": [{"codigo": c} for c in profesores], "asignaturas": [{"codigo": c} for c in asignaturas]}


class Oraculo(unittest.TestCase):

    def test_2025_identico_byte_a_byte_al_versionado(self):
        with tempfile.TemporaryDirectory() as d:
            salida = Path(d, "nombres.json")
            with contextlib.redirect_stdout(io.StringIO()):
                rc = en.main(["--pdf", str(HR / "pdf" / "Horarios de grupos.pdf"),
                              "--catalogo", str(HR / "catalogo-derivado.json"), "--salida", str(salida)])
            self.assertEqual(rc, 0)
            self.assertEqual(salida.read_bytes(), (HR / "nombres-derivados.json").read_bytes())


class VariosPdf(Base):

    # TPMAR en conflicto con la variante «mayor» en el primer PDF: si el orden de entrada
    # contara, cambiaría la lista de variantes o el nombre elegido.
    A = [pagina([("P1", "Pérez Gómez, Ana")], [("Mat", "Matemáticas"), ("TPMAR", "Tutoría diversificación")])]
    B = [pagina([("P1", "Pérez Gómez, Ana"), ("P2", "López Ruiz, Lu")],
                [("Mat", "Matemáticas"), ("Ing", "Inglés"), ("TPMAR", "Tutoría Orientación")])]
    CAT = catalogo(["P1", "P2"], ["Mat", "Ing", "TPMAR"])

    def test_dos_pdf_en_orden_distinto_dan_la_misma_salida(self):
        rc1, _, s1 = self.correr({"a.pdf": self.A, "b.pdf": self.B}, self.CAT, self.tmp / "uno.json")
        rc2, _, s2 = self.correr({"b.pdf": self.B, "a.pdf": self.A}, self.CAT, self.tmp / "dos.json")
        self.assertEqual((rc1, rc2), (0, 0))
        self.assertEqual(s1.read_bytes(), s2.read_bytes())

    def test_las_variantes_de_todos_los_pdf_cuentan(self):
        # Cada PDF trae un código que el otro no tiene: con solo uno de los dos, falta alguno.
        a = [pagina([("P1", "Pérez Gómez, Ana")], [("Mat", "Matemáticas")])]
        b = [pagina([("P2", "López Ruiz, Lu")], [("Ing", "Inglés")])]
        rc, _, s = self.correr({"a.pdf": a, "b.pdf": b}, catalogo(["P1", "P2"], ["Mat", "Ing"]))
        self.assertEqual(rc, 0)
        d = json.loads(s.read_text(encoding="utf-8"))
        self.assertEqual((sorted(d["profesores"]), sorted(d["asignaturas"])), (["P1", "P2"], ["Ing", "Mat"]))
        self.assertEqual(d["_meta"]["fuente"], "a.pdf, b.pdf")

    def test_truncado_en_un_pdf_y_completo_en_otro_se_resuelve_por_prefijo(self):
        a = [pagina([("P1", "Gutiérrez Sánchez, Carlo")], [("Mat", "Matemáticas")])]
        b = [pagina([("P1", "Gutiérrez Sánchez, Carlos Alberto")], [("Mat", "Matemáticas")])]
        rc, _, s = self.correr({"a.pdf": a, "b.pdf": b}, catalogo(["P1"], ["Mat"]))
        self.assertEqual(rc, 0)
        p1 = json.loads(s.read_text(encoding="utf-8"))["profesores"]["P1"]
        self.assertEqual(p1["nombreCompleto"], "Gutiérrez Sánchez, Carlos Alberto")
        self.assertNotIn("conflicto", p1)

    def test_dos_nombres_que_no_son_prefijo_dan_conflicto_con_las_dos_variantes(self):
        a = [pagina([("P1", "Uno")], [("TPMAR", "Tutoría Orientación")])]
        b = [pagina([("P1", "Uno")], [("TPMAR", "Tutoría diversificación")])]
        rc, _, s = self.correr({"a.pdf": a, "b.pdf": b}, catalogo(["P1"], ["TPMAR"]))
        self.assertEqual(rc, 0)
        t = json.loads(s.read_text(encoding="utf-8"))["asignaturas"]["TPMAR"]
        self.assertEqual((t["nombreCompleto"], t["conflicto"], t["variantes"]),
                         ("Tutoría Orientación", True, ["Tutoría Orientación", "Tutoría diversificación"]))


class Cierre(Base):

    def test_si_el_cierre_falla_no_se_escribe_y_rc_1(self):
        p = [pagina([("P1", "Uno")], [("Mat", "Matemáticas")])]
        rc, out, s = self.correr({"a.pdf": p}, catalogo(["P1", "P9"], ["Mat"]))
        self.assertEqual(rc, 1)
        self.assertIn("DESAJUSTE en profesores", out)
        self.assertFalse(s.exists())

    def test_escribe_en_la_salida_que_se_le_da(self):
        p = [pagina([("P1", "Uno")], [("Mat", "Matemáticas")])]
        destino = self.tmp / "sub" / "otra.json"
        destino.parent.mkdir()
        rc, _, s = self.correr({"a.pdf": p}, catalogo(["P1"], ["Mat"]), destino)
        self.assertEqual(rc, 0)
        self.assertTrue(destino.is_file())

    def test_sin_argumentos_no_escribe_nada(self):
        antes = (HR / "nombres-derivados.json").read_bytes()
        with self.assertRaises(SystemExit) as e, contextlib.redirect_stderr(io.StringIO()):
            en.main([])
        self.assertEqual(e.exception.code, 2)
        self.assertEqual((HR / "nombres-derivados.json").read_bytes(), antes)



class Decisiones(Base):
    """S204: --decisiones añade los nombres que el PDF de grupos no trae."""

    P = [pagina([("P1", "Uno")], [("Mat", "Matemáticas")])]
    DEC = [{"id": "n", "fija": "asignaturas.nombre", "valor": {"RED": "Reunión de Equipo Direct"}},
           {"id": "a", "fija": "profesores.alta", "valor": {"PROV1": {"pagina": 5, "titulo": "X", "nombreCompleto": "Lobato"}}},
           {"id": "otra", "fija": "jornada.horas", "valor": {}}]

    def test_anade_los_nombres_de_las_decisiones_y_cierra(self):
        rc, _, s = self.correr({"a.pdf": self.P}, catalogo(["P1", "PROV1"], ["Mat", "RED"]), decisiones=self.DEC)
        self.assertEqual(rc, 0)
        d = json.loads(s.read_text(encoding="utf-8"))
        self.assertEqual(d["asignaturas"]["RED"], {"nombreCompleto": "Reunión de Equipo Direct",
                                                   "procedencia": "decision:n", "truncado": False})
        self.assertEqual(d["profesores"]["PROV1"]["nombreCompleto"], "Lobato")
        self.assertEqual(list(d["asignaturas"]), ["Mat", "RED"])
        self.assertEqual(d["_meta"]["resumen"]["deDecisiones"], 2)

    def test_sin_decisiones_el_cierre_falla_con_los_codigos_nuevos(self):
        rc, out, s = self.correr({"a.pdf": self.P}, catalogo(["P1", "PROV1"], ["Mat", "RED"]))
        self.assertEqual(rc, 1)
        self.assertIn("DESAJUSTE", out)

    def test_codigo_del_pdf_y_de_una_decision_es_error(self):
        dec = [{"id": "n", "fija": "asignaturas.nombre", "valor": {"Mat": "Otra"}}]
        with self.assertRaises(SystemExit) as e:
            self.correr({"a.pdf": self.P}, catalogo(["P1"], ["Mat"]), decisiones=dec)
        self.assertIn("vienen del PDF y de una decision", str(e.exception.code))

    def test_codigo_en_dos_decisiones_es_error(self):
        dec = [{"id": "n1", "fija": "asignaturas.nombre", "valor": {"RED": "A"}},
               {"id": "n2", "fija": "asignaturas.nombre", "valor": {"RED": "B"}}]
        with self.assertRaises(SystemExit) as e:
            self.correr({"a.pdf": self.P}, catalogo(["P1"], ["Mat", "RED"]), decisiones=dec)
        self.assertIn("dos decisiones", str(e.exception.code))


if __name__ == "__main__":
    unittest.main()
