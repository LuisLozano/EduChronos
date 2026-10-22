"""Spec de generar_referencia (C-construir-probar-publicar, S246). Un solver/src fabricado, nunca el real."""
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "generar_referencia.py")
PAQ = "es.yaroki.educhronos.solver"

# Un .java por tipo: (carpeta bajo solver/src, subpaquete, nombre, cuerpo tras la línea package).
FUENTES = [
    ("main", "domain", "Plaza", "public record Plaza(String codigo, int horas) {}\n"),
    ("main", "domain", "Turno",
     "public enum Turno {\n    MANANA, TARDE;\n    Turno() {}\n"
     "    public String etiqueta() { return name(); }\n}\n"),
    ("main", "domain", "Calculo",
     "public class Calculo {\n"
     "    private int contador;\n"
     "    public int sumar(int a, int b) { return a + b; }\n"
     "    private int restar(int a, int b) { return a - b; }\n"
     "    int doble(int a) { return 2 * a; }\n"
     "    public Plaza crear(String c) { return new Plaza(c, 1); }\n"
     "}\n"),
    # Turno solo aparece en un comentario dentro de la firma.
    ("main", "domain", "ConComentario",
     "public class ConComentario {\n    public void usar(/* Turno */ int x) {}\n}\n"),
    ("main", "cpsat", "Motor",
     "import %s.domain.Turno;\n\npublic class Motor {\n    public Turno elegir() { return Turno.MANANA; }\n}\n" % PAQ),
    # Turno solo aparece dentro de un literal con forma de import.
    ("main", "io", "ConLiteral",
     "public class ConLiteral {\n"
     "    static final String TEXTO = \"import %s.domain.Turno;\";\n"
     "    public String texto() { return TEXTO; }\n"
     "}\n" % PAQ),
    ("main", "cli", "Main", "public class Main {\n    public static void main(String[] args) {}\n}\n"),
    ("test", "domain", "PlazaTest", "class PlazaTest {\n    void crea() {}\n}\n"),
    ("test", "cpsat", "MotorTest", "class MotorTest {\n    void elige() {}\n}\n"),
    ("test", "cli", "MainTest", "class MainTest {\n    void arranca() {}\n}\n"),
]


def lanzar(*args, cwd=None):
    return subprocess.run([sys.executable, "-I", "-B", SCRIPT, *args], capture_output=True, text=True, cwd=cwd)


def fabricar(raiz):
    for carpeta, sub, nombre, cuerpo in FUENTES:
        destino = os.path.join(raiz, "solver", "src", carpeta, "java", *PAQ.split("."), sub, nombre + ".java")
        os.makedirs(os.path.dirname(destino), exist_ok=True)
        with open(destino, "w", encoding="utf-8", newline="") as f:
            f.write("package %s.%s;\n\n%s" % (PAQ, sub, cuerpo))


class GenerarReferenciaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raiz = tempfile.mkdtemp()
        if "/src/test/" in cls.raiz:
            raise RuntimeError("el directorio temporal contiene /src/test/: todo saldría como test")
        fabricar(cls.raiz)
        cls.salida = os.path.join(cls.raiz, "referencia.md")
        cls.r = lanzar(cls.raiz, "2026-01-02", "abc1234", cls.salida)
        # Sin salida, cada test cae por su cuenta en vez de abortar la clase entera.
        cls.texto = ""
        if os.path.exists(cls.salida):
            with open(cls.salida, encoding="utf-8") as f:
                cls.texto = f.read()
        cls.lineas = cls.texto.split("\n")

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.raiz)

    def bloque(self, nombre):
        """Las líneas del tipo, desde su «### » hasta el siguiente encabezado o separador."""
        ini = next(i for i, l in enumerate(self.lineas) if l.startswith("### `%s` — " % nombre))
        fin = next((i for i in range(ini + 1, len(self.lineas))
                    if self.lineas[i].startswith(("### ", "## ", "---"))), len(self.lineas))
        return self.lineas[ini:fin]

    def posicion(self, linea):
        return self.lineas.index(linea)

    def test_corrida_valida(self):
        self.assertEqual(self.r.returncode, 0, self.r.stderr)
        self.assertEqual(self.r.stdout.strip(), "%d tipos de %d ficheros" % (len(FUENTES), len(FUENTES)))

    def test_t1_private_fuera_public_dentro(self):
        b = self.bloque("Calculo")
        self.assertIn("- `public int sumar(int a, int b)`", b)
        self.assertNotIn("restar", self.texto)
        self.assertNotIn("contador", self.texto)

    def test_t2_package_private_dentro(self):
        self.assertIn("- `int doble(int a)`", self.bloque("Calculo"))
        self.assertEqual(self.bloque("PlazaTest")[0], "### `PlazaTest` — package-private class")

    def test_t3_record_con_componentes(self):
        b = self.bloque("Plaza")
        self.assertEqual(b[:5], ["### `Plaza` — public record", "Paquete: `%s.domain`" % PAQ, "Componentes:",
                                 "- `String codigo`", "- `int horas`"])

    def test_t4_enum_con_constantes_y_sin_constructor(self):
        b = self.bloque("Turno")
        self.assertIn("Constantes: `MANANA`, `TARDE`", b)
        self.assertNotIn("Constructores:", b)
        self.assertIn("- `public String etiqueta()`", b)

    def test_t5_consume(self):
        self.assertIn("Consume: `Plaza`", self.bloque("Calculo"))   # por firma, mismo paquete
        self.assertIn("Consume: `Turno`", self.bloque("Motor"))     # por import, otro paquete
        self.assertIn("Consume: (ninguno)", self.bloque("Plaza"))

    def test_t6a_secciones_de_main_en_orden_domain_cpsat_io_cli(self):
        # Por nombre de fichero el orden sería cli, cpsat, domain, io.
        orden = [self.posicion("## Paquete `%s`" % s) for s in ("domain", "cpsat", "io", "cli")]
        self.assertEqual(orden, sorted(orden))

    def test_t6b_secciones_de_test_en_orden_alfabetico(self):
        orden = [self.posicion("## Tests — paquete `%s`" % s) for s in ("cli", "cpsat", "domain")]
        self.assertEqual(orden, sorted(orden))
        self.assertLess(self.posicion("## Paquete `cli`"), orden[0])

    def test_t7_cabecera(self):
        self.assertEqual(self.lineas[4], "- Fecha: 2026-01-02")
        self.assertEqual(self.lineas[5], "- Commit: `abc1234`")

    def test_t8_comentario_y_literal_no_entran_en_consume(self):
        self.assertIn("Consume: (ninguno)", self.bloque("ConComentario"))
        self.assertIn("- `public void usar(int x)`", self.bloque("ConComentario"))
        self.assertIn("Consume: (ninguno)", self.bloque("ConLiteral"))

    def test_t9_tipo_de_src_test(self):
        self.assertIn("## Tests — paquete `cli`", self.lineas)
        self.assertEqual(self.bloque("MainTest")[:2],
                         ["### `MainTest` — package-private class", "Paquete: `%s.cli`" % PAQ])

    def test_t10_argumentos_invalidos(self):
        with tempfile.TemporaryDirectory() as aparte:
            salida = os.path.join(aparte, "salida.md")
            r = lanzar(self.raiz, "2026-01-02", salida)
            self.assertEqual(r.returncode, 2, r.stderr)
            self.assertIn("uso:", r.stderr)
            self.assertFalse(os.path.exists(salida))
            r = lanzar(self.raiz, "2026-01-02", "abc1234", "salida.md", cwd=aparte)
            self.assertEqual(r.returncode, 2, r.stderr)
            self.assertFalse(os.path.exists(os.path.join(aparte, "salida.md")))
            r = lanzar("relativa", "2026-01-02", "abc1234", salida, cwd=aparte)
            self.assertEqual(r.returncode, 2, r.stderr)
            self.assertFalse(os.path.exists(salida))
            self.assertEqual(os.listdir(aparte), [])


if __name__ == "__main__":
    unittest.main()
