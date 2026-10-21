"""Spec de comprobar_rutas (C-arquitectura-y-rutas, S245). Repos git fabricados, nunca el real."""
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import comprobar_rutas as cr  # noqa: E402

SCRIPT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "comprobar_rutas.py")
ENTORNO = dict(os.environ, GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM="1")


def git(raiz, *args):
    r = subprocess.run(["git", "-C", raiz, "-c", "user.name=test", "-c", "user.email=test@invalid",
                        "-c", "commit.gpgsign=false", *args], capture_output=True, env=ENTORNO)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode())
    return r.stdout.decode().strip()


def poner(raiz, ruta, contenido):
    destino = os.path.join(raiz, ruta)
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="") as f:
        f.write(contenido)


def doc(commit, cuerpo, veces=1):
    return "# Doc\n\nQué responde.\nCuándo leerlo.\n\n" + ("Describe el commit %s.\n\n" % commit) * veces + cuerpo


class ComprobarRutasTest(unittest.TestCase):
    def setUp(self):
        self.carpeta = tempfile.TemporaryDirectory()
        self.raiz = self.carpeta.name
        subprocess.run(["git", "-c", "init.defaultBranch=main", "init", "-q", self.raiz],
                       check=True, env=ENTORNO)
        poner(self.raiz, "cuatro.txt", "1\n2\n3\n4\n")
        poner(self.raiz, "sin-salto.txt", "1\n2")
        poner(self.raiz, "sub/dir/f.py", "x = 1\n")
        poner(self.raiz, "árbol/canción.md", "la\n")
        poner(self.raiz, "borrado.txt", "b\n")
        git(self.raiz, "add", "-A")
        git(self.raiz, "commit", "-q", "-m", "primero")
        self.primero = git(self.raiz, "rev-parse", "--short=12", "HEAD")
        git(self.raiz, "rm", "-q", "borrado.txt")
        git(self.raiz, "commit", "-q", "-m", "segundo")
        self.segundo = git(self.raiz, "rev-parse", "--short=12", "HEAD")

    def tearDown(self):
        self.carpeta.cleanup()

    def revisar(self, contenido, nombre="uno.md"):
        poner(self.raiz, "docs/tecnica/" + nombre, contenido)
        return cr.revisar(self.raiz)

    def assertSoloFallo(self, r, categoria):
        self.assertEqual(r.categorias(), {categoria}, r.fallos)
        self.assertEqual(r.avisos, [])

    def assertLimpio(self, r):
        self.assertEqual(r.fallos, [])
        self.assertEqual(r.avisos, [])

    # -- defectos d1-d9

    def test_d1_ruta_que_no_existe_es_rota_con_su_linea(self):
        r = self.revisar(doc(self.segundo, "Uno.\n\nVer `no/existe.txt`.\n"))
        self.assertSoloFallo(r, "rota")
        self.assertEqual(r.fallos[0][:2], ("docs/tecnica/uno.md", 10))

    def test_d2_commit_que_no_existe(self):
        r = self.revisar(doc("abcdef0123456", "Ver `cuatro.txt`.\n"))
        self.assertSoloFallo(r, "commit")
        self.assertEqual(r.rutas, 0)

    def test_d3_sin_linea_de_commit(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `cuatro.txt`.\n", veces=0)), "commit")

    def test_d3_dos_lineas_de_commit(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `cuatro.txt`.\n", veces=2)), "commit")

    def test_d3_linea_de_commit_fuera_de_las_diez_primeras(self):
        contenido = "# Doc\n" + "\n" * 10 + "Describe el commit %s.\n" % self.segundo
        self.assertSoloFallo(self.revisar(contenido), "commit")

    def test_d3_linea_de_commit_entre_comillas_invertidas(self):
        contenido = "# Doc\n\nDescribe el commit `%s`.\n" % self.segundo
        self.assertIn("commit", self.revisar(contenido).categorias())

    def test_d4_documento_de_20481_bytes(self):
        base = doc(self.segundo, "Texto.\n")
        contenido = base + "x" * (20481 - len(base.encode()) - 1) + "\n"
        self.assertEqual(len(contenido.encode()), 20481)
        self.assertSoloFallo(self.revisar(contenido), "tamaño")

    def test_d5_lineas_fuera_de_rango(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `cuatro.txt:3-5`.\n")), "líneas")

    def test_d5_cuenta_la_ultima_linea_sin_salto(self):
        self.assertLimpio(self.revisar(doc(self.segundo, "Ver `sin-salto.txt:2`.\n")))
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `sin-salto.txt:3`.\n")), "líneas")

    def test_d6_formas_prohibidas(self):
        for cita in ("/cuatro.txt", "./cuatro.txt", "../cuatro.txt", "sub\\dir\\f.py", "sub//dir/f.py",
                     "sub/./dir/f.py", "sub/dir/:2"):
            with self.subTest(cita=cita):
                self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `%s`.\n" % cita)), "forma")

    def test_d6_ruta_absoluta_dice_ruta_absoluta(self):
        r = self.revisar(doc(self.segundo, "Ver `/a.txt`.\n"))
        self.assertSoloFallo(r, "forma")
        self.assertIn("ruta absoluta", r.fallos[0][3])

    def test_d7_codigo_con_espacio(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Se corre `mvn -B test`.\n")), "forma")

    def test_d7_doble_comilla_y_comilla_sin_cerrar(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver ``cuatro.txt``.\n")), "forma")
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `cuatro.txt.\n")), "forma")

    def test_d8_existe_en_el_commit_citado_y_no_en_head_es_aviso(self):
        r = self.revisar(doc(self.primero, "Ver `borrado.txt`.\n"))
        self.assertEqual(r.fallos, [])
        self.assertEqual(len(r.avisos), 1)
        self.assertIn("borrado.txt", r.avisos[0][2])

    def test_d8_en_el_commit_nuevo_es_rota(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `borrado.txt`.\n")), "rota")

    def test_d9_sin_documentos_es_vacio(self):
        self.assertEqual(cr.revisar(self.raiz).categorias(), {"vacío"})
        os.makedirs(os.path.join(self.raiz, "docs/tecnica"))
        poner(self.raiz, "docs/tecnica/nota.txt", "no es markdown\n")
        self.assertEqual(cr.revisar(self.raiz).categorias(), {"vacío"})

    def test_d9_por_la_orden_sale_con_1(self):
        r = subprocess.run([sys.executable, "-B", SCRIPT, "--raiz", self.raiz], capture_output=True, text=True)
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn("FALLO vacío", r.stdout)

    # -- d10, c4 y líneas: docs/tecnica se mira en el árbol (DT de S245)

    def test_d10_ruta_de_docs_tecnica_que_no_existe_en_el_arbol_es_rota(self):
        r = self.revisar(doc(self.segundo, "Ver `docs/tecnica/no-existe.md`.\n"))
        self.assertSoloFallo(r, "rota")
        self.assertIn("en el árbol", r.fallos[0][3])

    def test_c4_ruta_de_docs_tecnica_en_el_arbol_y_no_en_el_commit_no_da_nada(self):
        poner(self.raiz, "docs/tecnica/otro.md", doc(self.segundo, "Sin rutas.\n"))
        r = self.revisar(doc(self.segundo, "Ver `docs/tecnica/otro.md`, `docs/tecnica/otro.md:8` y `docs/tecnica/`.\n"))
        self.assertLimpio(r)
        self.assertEqual(r.rutas, 3)

    def test_lineas_fuera_de_rango_en_un_documento_de_docs_tecnica(self):
        poner(self.raiz, "docs/tecnica/otro.md", doc(self.segundo, "Sin rutas.\n"))
        r = self.revisar(doc(self.segundo, "Ver `docs/tecnica/otro.md:8-9`.\n"))
        self.assertSoloFallo(r, "líneas")
        self.assertIn("8 líneas en el árbol", r.fallos[0][3])

    def test_docs_tecnica_con_tipo_equivocado_es_rota(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `docs/tecnica/uno.md/`.\n")), "rota")

    # -- controles c1-c2

    def test_c1_documento_limpio(self):
        r = self.revisar(doc(self.segundo, "Ver `árbol/canción.md`, `sub/dir/` y `cuatro.txt:2-4`.\n"))
        self.assertLimpio(r)
        self.assertEqual(r.rutas, 3)

    def test_c2_bloques_delimitados_se_ignoran(self):
        cuerpo = "```bash\n`no-existe.txt`\n```\n\n~~~\n`tampoco.txt`\n~~~\n\n````\n```\n`x`\n````\n"
        r = self.revisar(doc(self.segundo, cuerpo))
        self.assertLimpio(r)
        self.assertEqual(r.rutas, 0)

    def test_c2_bloque_sin_cerrar_es_forma(self):
        self.assertSoloFallo(self.revisar(doc(self.segundo, "```\n`no-existe.txt`\n")), "forma")

    # -- bordes

    def test_borde_20480_bytes_pasa(self):
        base = doc(self.segundo, "Texto.\n")
        contenido = base + "x" * (20480 - len(base.encode()) - 1) + "\n"
        self.assertEqual(len(contenido.encode()), 20480)
        self.assertLimpio(self.revisar(contenido))

    def test_borde_ruta_N_N_valida(self):
        self.assertLimpio(self.revisar(doc(self.segundo, "Ver `cuatro.txt:4-4` y `cuatro.txt:4`.\n")))

    def test_borde_ruta_0_y_N_mayor_que_M_son_forma(self):
        for cita in ("cuatro.txt:0", "cuatro.txt:0-2", "cuatro.txt:3-2"):
            with self.subTest(cita=cita):
                self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `%s`.\n" % cita)), "forma")

    def test_borde_directorio_sin_barra_y_tipos(self):
        self.assertLimpio(self.revisar(doc(self.segundo, "Ver `sub/dir`.\n")))
        self.assertSoloFallo(self.revisar(doc(self.segundo, "Ver `cuatro.txt/`.\n")), "rota")

    def test_borde_raiz_relativa_o_ausente_da_2(self):
        for args in (["--raiz", "relativa/repo"], []):
            with self.subTest(args=args):
                r = subprocess.run([sys.executable, "-B", SCRIPT, *args], capture_output=True, text=True)
                self.assertEqual(r.returncode, 2, r.stdout + r.stderr)
                self.assertNotIn("AUTOPRUEBA", r.stdout)

    # -- salida

    def test_salida_limpia_rc_0_y_cifras(self):
        self.revisar(doc(self.segundo, "Ver `cuatro.txt`.\n"))
        r = subprocess.run([sys.executable, "-B", SCRIPT, "--raiz", self.raiz], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn("AUTOPRUEBA: 14 defectos inyectados, los 14 detectados.", r.stdout)
        for linea in ("documentos: 1", "rutas comprobadas: 1", "rotas: 0", "avisos: 0",
                      "comprobaciones duras con fallo: 0"):
            self.assertIn(linea + "\n", r.stdout)

    def test_salida_anotaciones_solo_con_GITHUB_ACTIONS(self):
        self.revisar(doc(self.segundo, "Ver `no,existe.txt`.\n"), nombre="a,b.md")
        self.revisar(doc(self.primero, "Ver `borrado.txt`.\n"), nombre="c.md")
        con = dict(os.environ, GITHUB_ACTIONS="true")
        sin = {k: v for k, v in os.environ.items() if k != "GITHUB_ACTIONS"}
        r = subprocess.run([sys.executable, "-B", SCRIPT, "--raiz", self.raiz], capture_output=True,
                           text=True, env=con)
        self.assertEqual(r.returncode, 1)
        self.assertIn("\n::error file=docs/tecnica/a%2Cb.md,line=8::rota: no,existe.txt: no existe en ", r.stdout)
        self.assertIn("\n::warning file=docs/tecnica/c.md,line=8::borrado.txt: existe en ", r.stdout)
        self.assertIn("docs/tecnica/a,b.md:8: FALLO rota: no,existe.txt", r.stdout)
        r = subprocess.run([sys.executable, "-B", SCRIPT, "--raiz", self.raiz], capture_output=True,
                           text=True, env=sin)
        self.assertEqual(r.returncode, 1)
        self.assertNotIn("::", r.stdout)

    def test_autoprueba_pasa_y_cuenta_sus_defectos(self):
        problemas, inyectados = cr.autoprueba()
        self.assertEqual(problemas, [])
        self.assertEqual(inyectados, 14)


if __name__ == "__main__":
    unittest.main()
