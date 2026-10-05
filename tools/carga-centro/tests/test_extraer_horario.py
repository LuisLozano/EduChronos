#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de extraer-horario.py (S196, C-volcado-grupos).

    python3 -m unittest discover -s tools/carga-centro/tests -v

Fixtures versionados: el PDF de grupos de 2025/2026 (docs/horario-referencia/pdf/) y las
copias de P05 y P04 de la referencia 2026/2027 (docs/horario-referencia/2026-2027/pdf/).
Ningún test lee docs_extra/. Necesitan `pdftotext` (poppler) en el PATH.

Las celdas fijadas en T2 a T5 se derivaron de la extracción y se cotejaron con el volcado
de palabras de pdfplumber de S196 (M2); no son supuestas.
"""
import contextlib
import copy
import importlib.util
import io
import json
import os
import tempfile
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]
_spec = importlib.util.spec_from_file_location(
    "extraer_horario", RAIZ / "tools" / "carga-centro" / "extraer-horario.py")
eh = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(eh)

ORACULO_DIR = RAIZ / "docs" / "horario-referencia"
ORACULO_PDF = ORACULO_DIR / "pdf" / "Horarios de grupos.pdf"
DIR_2026 = RAIZ / "docs" / "horario-referencia" / "2026-2027" / "pdf"
P05 = DIR_2026 / "Horarios de grupos.pdf"
P04 = DIR_2026 / "Horarios de grupos (11).pdf"

_cache = {}


def leer(ruta):
    if ruta not in _cache:
        _cache[ruta] = eh.leer_pdf(str(ruta))
    return copy.deepcopy(_cache[ruta])


def compuesto():
    return eh.procesar([(P05.name, leer(P05)), (P04.name, leer(P04))])


def grupo(res, titulo):
    g = [g for g in res["grupos"] if g["titulo"] == titulo]
    if len(g) != 1:
        raise AssertionError("grupo %r: %d apariciones" % (titulo, len(g)))
    return g[0]


def tuplas(g):
    return [(c["dia"], c["tramo"], c["asignatura"], c["profesor"], c["aula"])
            for c in eh.volcado(g)["celdas"]]


def ejecutar(argv):
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
        rc = eh.main(argv)
    return rc, out.getvalue()


class T1Oraculo2025(unittest.TestCase):
    """El PDF de 2025/2026 da los 28 volcados versionados de S115, igual parseados."""

    def test_mismos_ficheros_y_mismo_contenido(self):
        with tempfile.TemporaryDirectory() as tmp:
            salida = os.path.join(tmp, "volcado")
            rc, informe = ejecutar(["--modo", "grupos", "--salida", salida, str(ORACULO_PDF)])
            self.assertEqual(rc, 0, informe)
            esperados = sorted(p.name for p in ORACULO_DIR.glob("grupo-*.json"))
            self.assertEqual(len(esperados), 28)
            self.assertEqual(sorted(os.listdir(salida)), esperados)
            for nombre in esperados:
                with open(ORACULO_DIR / nombre, encoding="utf-8") as f:
                    esperado = json.load(f)
                with open(os.path.join(salida, nombre), encoding="utf-8") as f:
                    obtenido = json.load(f)
                self.assertEqual(obtenido, esperado, nombre)


class T2RecreoAusente(unittest.TestCase):
    """P04 no imprime la franja del recreo: las filas 11:30, 12:30 y 13:30 siguen siendo los
    tramos 4, 5 y 6 porque el tramo sale del valor de la etiqueta, no de su posición."""

    def test_filas_tras_el_recreo_en_p04(self):
        g = grupo(eh.procesar([(P04.name, leer(P04))]), "4º ESO A")
        self.assertEqual((g["fuente"], g["pagina"]), (P04.name, 1))
        celdas = tuplas(g)
        self.assertIn((1, 4, "MatAc", "MAT6", "A2"), celdas)  # fila 11:30
        self.assertIn((1, 5, "LCL", "LEN8", "A2"), celdas)    # fila 12:30
        self.assertIn((1, 6, "Biol", "BYG2", "A2"), celdas)   # fila 13:30
        self.assertEqual(sorted({c[1] for c in celdas}), [1, 2, 3, 4, 5, 6])


class T3Columnas2026(unittest.TestCase):
    """Paso de 69 en 2026: una celda por día de 1º ESO A (P05 p1), fila 10:00."""

    def test_un_dia_por_columna(self):
        g = grupo(eh.procesar([(P05.name, leer(P05))]), "1º ESO A")
        celdas = tuplas(g)
        for dia, asig, prof, aula in [(1, "ByG", "BYG1", "A3"), (2, "Mús", "MUS1", "A13"),
                                      (3, "Mat", "MAT8", "A3"), (4, "Ing", "ING6", "A3"),
                                      (5, "EF", "EFI3", "A3")]:
            dias = [c[0] for c in celdas if c[1:] == (3, asig, prof, aula)]
            self.assertEqual(dias, [dia], (asig, prof))


class T4Continuacion(unittest.TestCase):
    """La página de continuación (P04 p4, de 4º ESO B) no es un grupo: su leyenda se suma a la
    de la página anterior. TUT4 solo figura en esa continuación y clasifica su celda."""

    def test_continuacion_fusionada(self):
        res = compuesto()
        self.assertEqual(len(res["grupos"]), 30)
        self.assertEqual(eh.codigo_salida(res), 0)
        g = grupo(res, "4º ESO B")
        self.assertEqual((g["fuente"], g["pagina"], g["continuaciones"]), (P04.name, 3, [4]))
        self.assertNotIn((P04.name, 4), {(x["fuente"], x["pagina"]) for x in res["grupos"]})
        principal = eh.analizar_pagina(leer(P04)[2])
        self.assertNotIn("TUT4", [w["text"] for l in principal["lineas_leyenda"] for w in l])
        self.assertIn("TUT4", [w["text"] for w in g["codigos"]["asig"] if w["id"][0] == 4])
        self.assertIn((3, 5, "TUT4", "ING6", "B03"), tuplas(g))

    def test_volcado_escrito_tiene_30_ficheros(self):
        with tempfile.TemporaryDirectory() as tmp:
            salida = os.path.join(tmp, "volcado")
            rc, informe = ejecutar(["--modo", "grupos", "--salida", salida, str(P05), str(P04)])
            self.assertEqual(rc, 0, informe)
            self.assertEqual(len(os.listdir(salida)), 30)


class T5Composicion(unittest.TestCase):
    """Los cinco grupos de P04 sustituyen a los de P05 por título. La EXPRE de DIB1 en C01 del
    lunes a 1.ª (que P05 trae en 4º ESO B PDC y C PDC) desaparece en el compuesto."""

    def test_p04_gana_y_la_expre_en_c01_desaparece(self):
        solo_p05 = eh.procesar([(P05.name, leer(P05))])
        res = compuesto()
        hueco = (1, 1, "EXPRE", "DIB1", "C01")
        for titulo in ("4º ESO B PDC", "4º ESO C PDC"):
            g = grupo(res, titulo)
            self.assertEqual(g["fuente"], P04.name, titulo)
            self.assertIn(hueco, tuplas(grupo(solo_p05, titulo)), titulo)
            self.assertNotIn(hueco, tuplas(g), titulo)
            self.assertIn((1, 1, "EXPRE", "DIB2", "TALL1"), tuplas(g), titulo)
        self.assertEqual(sorted(t for t, _, _ in res["sustituidos"]),
                         ["4º ESO A", "4º ESO B", "4º ESO B PDC", "4º ESO C", "4º ESO C PDC"])


class T6Cuadre(unittest.TestCase):
    """El cuadre es una función pura sobre (palabras de rejilla, celdas)."""

    @staticmethod
    def palabra(i, texto):
        return {"id": (1, i), "text": texto, "x0": 0.0, "x1": 1.0, "top": 0.0, "bottom": 1.0}

    def test_sin_pdf(self):
        a, p, x = self.palabra(0, "Mat"), self.palabra(1, "MAT8"), self.palabra(2, "A3")
        celda = {"dia": 1, "tramo": 1, "asignatura": a, "profesor": [p], "aula": x}
        self.assertEqual(eh.cuadrar([a, p, x], [celda]), ([], []))
        intrusa = self.palabra(3, "XQZ")
        self.assertEqual(eh.cuadrar([a, p, x, intrusa], [celda]), ([intrusa], []))
        otra = {"dia": 2, "tramo": 1, "asignatura": a, "profesor": [], "aula": None}
        self.assertEqual(eh.cuadrar([a, p, x], [celda, otra]), ([], [a]))

    def test_palabra_inyectada_es_un_perdido(self):
        # 1º ESO A (P05 p1), lunes a 1.ª: «Ing ING6» y debajo «A3»; dos líneas más abajo, y aún
        # dentro del tramo, se inyecta un código que no está en la leyenda.
        pags = leer(P05)
        aula = [w for w in pags[0] if w["text"] == "A3"]
        aula = min(aula, key=lambda w: (w["top"], w["x0"]))
        intrusa = dict(aula, id=(1, 9999), text="XQZ", top=aula["top"] + 17.3,
                       bottom=aula["bottom"] + 17.3)
        pags[0].append(intrusa)
        res = eh.procesar([(P05.name, pags)])
        g = grupo(res, "1º ESO A")
        self.assertEqual([w["id"] for w in g["perdidos"]], [(1, 9999)])
        self.assertEqual(sum(len(x["perdidos"]) for x in res["grupos"]), 1)
        self.assertNotEqual(eh.codigo_salida(res), 0)

    def test_palabra_en_dos_celdas_es_un_duplicado(self):
        res = compuesto()
        g = next(g for g in res["grupos"] if any(c["aula"] is None for c in g["celdas"]))
        celdas = copy.deepcopy(g["celdas"])
        sin_aula = next(c for c in celdas if c["aula"] is None)
        prestada = next(c for c in celdas if c["aula"] is not None)["aula"]
        sin_aula["aula"] = prestada
        perdidos, duplicados = eh.cuadrar(g["rejilla"], celdas)
        self.assertEqual(perdidos, [])
        self.assertEqual([w["id"] for w in duplicados], [prestada["id"]])


class Invocacion(unittest.TestCase):

    def test_salida_relativa_aborta(self):
        rc, _ = ejecutar(["--modo", "grupos", "--salida", "relativa", str(P04)])
        self.assertEqual(rc, 2)

    def test_salida_con_ficheros_aborta_sin_escribir(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "ya-estaba").write_text("x")
            rc, _ = ejecutar(["--modo", "grupos", "--salida", tmp, str(P04)])
            self.assertEqual(rc, 2)
            self.assertEqual(os.listdir(tmp), ["ya-estaba"])

    def test_etiqueta_de_hora_desconocida_aborta(self):
        pags = leer(P04)
        w = next(w for w in pags[0] if w["text"] == "8:00")
        w["text"] = "7:45"
        with self.assertRaises(eh.ErrorExtraccion):
            eh.procesar([(P04.name, pags)])


if __name__ == "__main__":
    unittest.main()
