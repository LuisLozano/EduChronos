#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de verificar-conservacion.py (S199): volcados por argumento y reglas de codigos_grupo.py.

El esperado de las formas cortas es la lista de grupos del catálogo de 2026/2027, que deriva
derivar-catalogo.py; aquí solo se exige que cada título de los volcados tenga una y que la
imagen sea exactamente esa lista.
"""
import contextlib
import io
import json
import sqlite3
import sys
import tempfile
import unittest
from collections import Counter
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
from paginas_sinteticas import RAIZ, cargar  # noqa: E402

vc = cargar("verificar_conservacion", "verificar-conservacion.py")

V2026 = RAIZ / "docs" / "horario-referencia" / "2026-2027"


class FormaCortaDeLosVolcados(unittest.TestCase):

    def test_los_30_titulos_de_2026_tienen_forma_corta(self):
        titulos = [json.loads(f.read_text(encoding="utf-8"))["_meta"]["codigo_crudo"]
                   for f in sorted(V2026.glob("grupo-*.json"))]
        self.assertEqual(len(titulos), 30)
        cortos = {t: vc.normaliza(t) for t in titulos}
        self.assertEqual([t for t, c in cortos.items() if c is None], [])
        bach = {t: c for t, c in cortos.items() if t.startswith("1ºBACH")}
        self.assertEqual(sorted(bach.values()), ["1B-Ac", "1B-Am", "1B-Bc", "1B-Bm", "1B-Ch", "1B-Cs"])
        grupos = {g["codigo"] for g in json.loads((V2026 / "catalogo-derivado.json").read_text(encoding="utf-8"))["grupos"]}
        self.assertEqual(sorted(cortos.values()), sorted(grupos))

    def test_entradas_del_pdf_sin_titulos_sin_regla(self):
        _, mapa, sin_regla, n = vc.entradas_del_pdf(V2026)
        self.assertEqual((n, len(mapa), sin_regla), (30, 30, []))


# ------------------------------------------------- reuniones y funciones (S204)

ESQUEMA = RAIZ / "app" / "src" / "main" / "resources" / "schema.sql"


COLUMNA_TIPO = ", tipo varchar(255) not null default 'CLASE' check ((tipo in ('CLASE','REUNION','FUNCION')))"


def base_de_prueba(ruta, reunion_profesores=("MAT1", "TUA"), reunion_sesiones=1, clase_sin_subgrupo=False,
                   clase_sin_aula=False, reunion_con_aula=False, sin_columna_tipo=False):
    """Base con el esquema real: 1ºA con Mat (CLASE, MAT1, A1, 2 sesiones) y la REUNION RED
    sin aula ni subgrupos. Opciones para romper un supuesto o la reunión; sin_columna_tipo
    imita una base anterior al esquema 2, donde todo es CLASE."""
    con = sqlite3.connect(ruta)
    esquema = ESQUEMA.read_text(encoding="utf-8")
    if sin_columna_tipo:
        assert esquema.count(COLUMNA_TIPO) == 1
        esquema = esquema.replace(COLUMNA_TIPO, "")
    con.executescript(esquema)
    x = con.execute
    x("insert into nivel (id, codigo, orden) values (1, '1ESO', 1)")
    x("insert into grupo_administrativo (id, codigo, nivel_id, tipo) values (1, '1ºA', 1, 'ORDINARIO')")
    x("insert into subgrupo (id, codigo) values (1, '1ºA-Completo')")
    x("insert into subgrupo_grupo (grupo_id, subgrupo_id) values (1, 1)")
    x("insert into asignatura (id, codigo, nombre_completo) values (1, 'Mat', 'Matemáticas'), (2, 'RED', 'Reunión')")
    x("insert into profesor (id, codigo, nombre_completo) values (1, 'MAT1', 'Uno'), (2, 'TUA', 'Dos')")
    x("insert into aula (id, codigo, tipo) values (1, 'A1', 'ORDINARIA')")
    for i, dia in ((1, "LUNES"), (2, "MARTES")):
        x("insert into tramo_semanal (id, dia, orden, es_lectivo, hora_inicio, hora_fin) values (?, ?, 1, 1, '08:00', '09:00')", (i, dia))
    if sin_columna_tipo:
        x("insert into actividad (id, codigo, asignatura_id, duracion_tramos, repeticiones_por_semana, patron_temporal, requiere_tutor)"
          " values (1, 'Mat-1ºA', 1, 1, 2, 'NEUTRA', 0), (2, 'RED', 2, 1, ?, 'NEUTRA', 0)", (reunion_sesiones,))
    else:
        x("insert into actividad (id, codigo, asignatura_id, duracion_tramos, repeticiones_por_semana, patron_temporal, requiere_tutor, tipo)"
          " values (1, 'Mat-1ºA', 1, 1, 2, 'NEUTRA', 0, 'CLASE'), (2, 'RED', 2, 1, ?, 'NEUTRA', 0, 'REUNION')", (reunion_sesiones,))
    x("insert into plaza (id, codigo, actividad_id, asignatura_id, aula_fija_id) values (1, 'Mat-1ºA-P1', 1, 1, 1), (2, 'RED-P1', 2, 2, NULL)")
    x("insert into plaza_profesor (plaza_id, profesor_id) values (1, 1)")
    for prof in reunion_profesores:
        x("insert into plaza_profesor (plaza_id, profesor_id) values (2, (select id from profesor where codigo = ?))", (prof,))
    if not clase_sin_subgrupo:
        x("insert into plaza_subgrupo (plaza_id, subgrupo_id) values (1, 1)")
    x("insert into horario_generado (id, nombre, estado, estado_solver, fecha_generacion) values (1, 'h', 'BORRADOR', 'FEASIBLE', 0)")
    x("insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id) values (1, 1, 1, 0, 1, ?), (2, 1, 1, 1, 2, 1)",
      (None if clase_sin_aula else 1,))
    for n in range(reunion_sesiones):
        x("insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id) values (?, 1, 2, ?, ?, ?)",
          (10 + n, n, 1 + n % 2, 1 if reunion_con_aula else None))
    con.commit()
    con.close()


SIN = "sin código"
DEC = {"decisiones": [{"id": "t", "fija": "noClase.tipos", "valor": {"REUNION": ["RED"], "FUNCION": []}},
                      {"id": "a", "fija": "profesores.alta",
                       "valor": {"PROV1": {"pagina": 3, "titulo": "Sin", "nombreCompleto": "Sin"}}}]}


def pagina(n, titulo, celdas):
    return {"_meta": {"pagina": n, "codigo_crudo": titulo, "modo": "profesores"},
            "celdas": [{"dia": d, "tramo": t, "asignatura": a, "grupos": g, "aula": au} for d, t, a, g, au in celdas],
            "recreo": []}


PAGINAS = [pagina(1, "Prof Mat", [(1, 1, "Mat", "1ºA", "A1"), (2, 1, "Mat", "1ºA", "A1"), (3, 3, "RED", "", None),
                                  (4, 4, "G", "", None)]),
           pagina(2, "Prof Tua", [(3, 3, "RED", "", None)])]
MAPA = {1: "MAT1", 2: "TUA"}


class ConBase(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)

    def tearDown(self):
        self._tmp.cleanup()

    def abrir(self, **kw):
        ruta = self.tmp / ("b%d.db" % len(list(self.tmp.glob("*.db"))))
        base_de_prueba(ruta, **kw)
        return sqlite3.connect("file:%s?mode=ro" % ruta, uri=True), ruta

    def rotos(self, **kw):
        con, _ = self.abrir(**kw)
        with contextlib.redirect_stdout(io.StringIO()):
            return [r[0] for r in vc.comprobar_supuestos(con, 1)]


class Supuestos(ConBase):

    def test_reunion_sin_aula_ni_subgrupo_no_rompe_nada(self):
        self.assertEqual(self.rotos(), [])

    def test_clase_sin_subgrupo_sigue_rompiendo(self):
        self.assertEqual(self.rotos(clase_sin_subgrupo=True), ["plazas sin subgrupo"])

    def test_base_sin_columna_tipo_todo_es_clase(self):
        self.assertEqual(self.rotos(sin_columna_tipo=True), ["plazas sin subgrupo", "FK nulas en sesion"])
        con, _ = self.abrir(sin_columna_tipo=True)
        self.assertEqual(vc.no_clase_de_la_base(con, 1), Counter())

    def test_clase_sin_aula_sigue_rompiendo(self):
        self.assertEqual(self.rotos(clase_sin_aula=True), ["FK nulas en sesion"])


class RecuentoNoClase(ConBase):

    def test_la_base_las_encuentra_por_los_profesores_de_su_plaza(self):
        con, _ = self.abrir(reunion_sesiones=2)
        self.assertEqual(vc.no_clase_de_la_base(con, 1), Counter({("MAT1", "RED"): 2, ("TUA", "RED"): 2}))

    def test_la_base_las_encuentra_tambien_con_aula(self):
        con, _ = self.abrir(reunion_con_aula=True)
        self.assertEqual(vc.no_clase_de_la_base(con, 1), Counter({("MAT1", "RED"): 1, ("TUA", "RED"): 1}))

    def test_el_volcado_cuenta_por_profesor_y_codigo_con_la_pagina_de_alta(self):
        pags = PAGINAS + [pagina(3, "Sin", [(3, 3, "RED", "", None), (5, 2, "RED", "", None)]),
                          pagina(4, "Otra", [(3, 3, "RED", "", None)])]
        c = vc.no_clase_del_volcado(pags, {**MAPA, 3: SIN, 4: SIN}, SIN, DEC)
        self.assertEqual(c, Counter({("MAT1", "RED"): 1, ("TUA", "RED"): 1, ("PROV1", "RED"): 2, (vc.SIN_PAGINA, "RED"): 1}))

    def test_las_de_clase_como_antes(self):
        con, _ = self.abrir(reunion_sesiones=2)
        self.assertEqual(vc.entradas_de_la_base(con, 1), {("1ºA", "LUNES", 1, "Mat", "MAT1"), ("1ºA", "MARTES", 1, "Mat", "MAT1")})


class MainConReunion(ConBase):

    def correr(self, **kw):
        _, ruta = self.abrir(**kw)
        vg = self.tmp / "volcados"
        vg.mkdir(exist_ok=True)
        (vg / "grupo-01.json").write_text(json.dumps({"_meta": {"codigo_crudo": "1º ESO A"}, "celdas": [
            {"dia": 1, "tramo": 1, "asignatura": "Mat", "profesor": "MAT1", "aula": "A1"},
            {"dia": 2, "tramo": 1, "asignatura": "Mat", "profesor": "MAT1", "aula": "A1"}]}, ensure_ascii=False), encoding="utf-8")
        fdec = self.tmp / "dec.json"
        fdec.write_text(json.dumps(DEC), encoding="utf-8")
        out = io.StringIO()
        with mock.patch.object(vc, "paginas_y_mapa", return_value=(PAGINAS, MAPA, SIN)), contextlib.redirect_stdout(out):
            rc = vc.main(["--db", str(ruta), "--volcados", str(vg), "--volcados-profesores", str(vg),
                          "--pdf-grupos", "x.pdf", "--decisiones", str(fdec)])
        return rc, out.getvalue()

    def test_reunion_conservada_rc_0(self):
        rc, out = self.correr()
        self.assertEqual(rc, 0, out)
        self.assertIn("divergentes .................... 0", out)

    def test_reunion_sin_un_profesor_rc_1(self):
        rc, out = self.correr(reunion_profesores=("MAT1",))
        self.assertEqual(rc, 1)
        self.assertIn("TUA        RED      volcado   1 base   0", out)

    def test_reunion_con_una_sesion_de_mas_rc_1(self):
        rc, out = self.correr(reunion_sesiones=2)
        self.assertEqual(rc, 1)

    def test_opciones_de_reuniones_van_juntas(self):
        _, ruta = self.abrir()
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as cm:
                vc.main(["--db", str(ruta), "--volcados", str(self.tmp), "--decisiones", "d.json"])
        self.assertEqual(cm.exception.code, 2)
        self.assertIn("van juntos", err.getvalue())


class Argumentos(unittest.TestCase):

    def test_sin_volcados_es_error_de_argparse(self):
        with contextlib.redirect_stderr(io.StringIO()), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as cm:
                vc.main(["--db", "/no/existe.db"])
        self.assertNotEqual(cm.exception.code, 0)


if __name__ == "__main__":
    unittest.main()
