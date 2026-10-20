"""Spec de preparar_base_partida (C-base-partida, S241). Bases fabricadas, nunca las del centro."""
import contextlib
import hashlib
import io
import os
import sqlite3
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import preparar_base_partida as pbp  # noqa: E402


def md5(ruta):
    with open(ruta, "rb") as f:
        return hashlib.md5(f.read()).hexdigest()


def fabricar(ruta, fila=None, user_version=0, con_tabla_curso=True):
    con = sqlite3.connect(ruta)
    con.execute(
        "create table profesor (id integer, codigo varchar(255) not null unique,"
        " nombre_completo varchar(255) not null, primary key (id))"
    )
    con.execute("insert into profesor values (1, 'P1', 'Uno')")
    con.execute("insert into profesor values (2, 'P2', 'Dos')")
    if con_tabla_curso:
        con.execute(pbp.DDL_CURSO)
    if fila is not None:
        con.execute("insert into curso (id, nombre, archivado) values (1, ?, ?)", fila)
    con.execute(f"PRAGMA user_version = {user_version}")
    con.commit()
    con.close()
    return ruta


def curso(ruta):
    con = sqlite3.connect(ruta)
    try:
        return con.execute("select id, nombre, archivado from curso").fetchall()
    finally:
        con.close()


def version(ruta):
    con = sqlite3.connect(ruta)
    try:
        return con.execute("PRAGMA user_version").fetchone()[0]
    finally:
        con.close()


class PrepararBasePartidaTest(unittest.TestCase):
    def setUp(self):
        self.carpeta = tempfile.TemporaryDirectory()
        self.origen = os.path.join(self.carpeta.name, "partida-original.db")
        self.destino = os.path.join(self.carpeta.name, "salida", "base-partida-2025-2026.db")
        self.temporal = os.path.join(self.carpeta.name, "salida", ".base-partida-2025-2026.db.tmp")

    def tearDown(self):
        self.carpeta.cleanup()

    def test_1_nombre_valido_es_espejo_de_NombreCurso(self):
        for bueno in ("2025/2026", "2026/2027", "1999/2000"):
            self.assertTrue(pbp.nombre_valido(bueno), bueno)
        for malo in (None, "", "2025-2026", "2025/2027", "2026/2025", "25/26", " 2025/2026"):
            self.assertFalse(pbp.nombre_valido(malo), repr(malo))

    def test_2_escribe_la_fila_y_no_toca_el_origen(self):
        fabricar(self.origen)
        antes = md5(self.origen)

        r = pbp.preparar(self.origen, self.destino, "2025/2026")

        self.assertEqual(curso(self.destino), [(1, "2025/2026", 0)])
        self.assertEqual(version(self.destino), 0)
        self.assertEqual(md5(self.origen), antes, "el origen, intacto")
        self.assertEqual(r["md5_origen"], antes)
        self.assertEqual(r["md5_destino"], md5(self.destino))
        self.assertEqual(r["tablas"], 2)
        self.assertFalse(os.path.exists(self.temporal), "sin temporal al terminar")
        con = sqlite3.connect(self.destino)
        try:
            self.assertEqual(
                con.execute("select id, codigo, nombre_completo from profesor order by id").fetchall(),
                [(1, "P1", "Uno"), (2, "P2", "Dos")],
            )
        finally:
            con.close()

    def test_3_conserva_el_user_version_del_origen(self):
        fabricar(self.origen, user_version=3)

        pbp.preparar(self.origen, self.destino, "2025/2026")

        self.assertEqual(version(self.destino), 3)

    def test_4_rechaza_una_base_que_ya_tiene_fila_sin_escribir_nada(self):
        fabricar(self.origen, fila=("2024/2025", 0))
        antes = md5(self.origen)

        with self.assertRaises(pbp.Rechazo) as cm:
            pbp.preparar(self.origen, self.destino, "2025/2026")

        self.assertIn("no renombra", str(cm.exception))
        self.assertFalse(os.path.exists(self.destino))
        self.assertFalse(os.path.exists(self.temporal))
        self.assertEqual(md5(self.origen), antes)

    def test_5_rechaza_una_base_sin_tabla_curso(self):
        fabricar(self.origen, con_tabla_curso=False)

        with self.assertRaises(pbp.Rechazo) as cm:
            pbp.preparar(self.origen, self.destino, "2025/2026")

        self.assertIn("tabla curso", str(cm.exception))
        self.assertFalse(os.path.exists(self.destino))

    def test_6_rechaza_un_destino_existente_sin_tocarlo(self):
        fabricar(self.origen)
        os.makedirs(os.path.dirname(self.destino))
        with open(self.destino, "wb") as f:
            f.write(b"no soy una base")
        antes = md5(self.destino)

        with self.assertRaises(pbp.Rechazo):
            pbp.preparar(self.origen, self.destino, "2025/2026")

        self.assertEqual(md5(self.destino), antes)

    def test_7_la_huella_del_origen_se_comprueba_por_prefijo(self):
        fabricar(self.origen)
        huella = md5(self.origen)

        with self.assertRaises(pbp.Rechazo):
            pbp.preparar(self.origen, self.destino, "2025/2026", huella_origen="0000000000")
        self.assertFalse(os.path.exists(self.destino))
        with self.assertRaises(pbp.Rechazo):
            pbp.preparar(self.origen, self.destino, "2025/2026", huella_origen=huella[:4])

        pbp.preparar(self.origen, self.destino, "2025/2026", huella_origen=huella[:8].upper())

        self.assertEqual(curso(self.destino), [(1, "2025/2026", 0)])

    def test_8_un_nombre_invalido_se_rechaza_antes_de_leer_nada(self):
        with self.assertRaises(pbp.Rechazo) as cm:
            pbp.preparar(os.path.join(self.carpeta.name, "no-existe.db"), self.destino, "2025-2026")

        self.assertIn("nombre de curso", str(cm.exception))

    def test_9_main_devuelve_0_con_HECHO_y_2_con_RECHAZADO(self):
        fabricar(self.origen)
        salida = io.StringIO()
        with contextlib.redirect_stdout(salida):
            rc = pbp.main(["--origen", self.origen, "--destino", self.destino, "--nombre", "2025/2026"])
        self.assertEqual(rc, 0)
        self.assertIn("HECHO", salida.getvalue())
        self.assertIn(md5(self.destino), salida.getvalue())

        salida = io.StringIO()
        with contextlib.redirect_stdout(salida):
            rc = pbp.main(["--origen", self.origen, "--destino", self.destino, "--nombre", "2025/2026"])
        self.assertEqual(rc, 2)
        self.assertIn("RECHAZADO", salida.getvalue())


if __name__ == "__main__":
    unittest.main()
