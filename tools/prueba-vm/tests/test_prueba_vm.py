"""Spec de prueba_vm (C-script-prueba-vm, S255). VM y anfitrión falsos, nunca VBoxManage."""
import contextlib
import hashlib
import io
import json
import os
import pathlib
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import prueba_vm as pv  # noqa: E402

SINCE = "2026-10-26T15:05:34.000000000"
TRAS_APAGAR = "2026-10-26T21:14:10.000000000"
SALIDA_INSTALAR = (
    "[1/3] Programa en C:\\Users\\prueba-vm\\AppData\\Local\\Programs\\Educhronos\r\n"
    "[2/3] Acceso directo en el Escritorio\r\n[3/3] Base de partida\r\n"
    "    Base de partida copiada a C:\\Users\\prueba-vm\\AppData\\Local\\Educhronos\\educhronos.db\r\n"
    "Educhronos instalado en C:\\Users\\prueba-vm\\AppData\\Local\\Programs\\Educhronos.\r\n"
    "Abre Educhronos con el acceso directo del Escritorio.\r\n\r\nPresione una tecla para continuar . . . \r\n"
)
GRUPOS = ("BUILTIN\\Usuarios Alias S-1-5-32-545 Grupo obligatorio\r\n"
          "Etiqueta obligatoria\\Nivel obligatorio medio Etiqueta S-1-16-8192\r\n")
QUSER = (" NOMBRE USUARIO        NOMBRE SESIÓN      ID. ESTADO  TIEMPO IN. TIEMPO SESIÓN\r\n"
         ">prueba-vm             console             2  Activo      ninguno   26/10/2026 22:00\r\n")
MIGRACION = ("2026-10-26T22:08:51.601+01:00  INFO 6120 --- [main] e.y.e.app.config.PreparadorEsquema"
             "       : Esquema preparado ruta=C:\\Users\\prueba-vm\\AppData\\Local\\Educhronos\\educhronos.db"
             " versionInicial=0 versionFinal=9 baseVacia=false")
LECTURAS = ("showvminfo Win11", "snapshot Win11 showvminfo", "snapshot Win11 list")


class VMFalsa:
    """VBoxManage guionizado: un estado de VM y unas respuestas del invitado que cada test cambia."""

    def __init__(self, esc):
        self.eventos = esc.eventos
        self.llamadas = []
        self.estado, self.actual, self.cambio = "poweroff", "s254-cuenta-prueba", SINCE
        self.compartida = esc.compartida
        self.lista = "   Name: s254-cuenta-prueba (UUID: 0a6f) *\n   Description: cuenta estándar sin estrenar\n"
        self.whoami_fallos = 1
        self.grupos = GRUPOS
        self.sha_zip, self.md5_base = esc.sha_zip.upper(), esc.md5_base.upper()
        self.datos_previos = ["datos-existe=False"]
        self.instalar_rc, self.instalar_salida = 0, SALIDA_INSTALAR
        self.md5_instalada = esc.md5_base.upper()
        self.datos = ["dato=educhronos.db|%d" % esc.tamano_base]
        self.lnk = self.exe = "True"
        self.quser = QUSER
        self.log = [MIGRACION]
        self.procesos = ["6120|Educhronos.exe|2", "9508|Educhronos.exe|2"]
        self.tras_apagar = TRAS_APAGAR
        self.tras_restaurar = SINCE
        self.restore_rc = 0
        self.com_caido = 0

    def __call__(self, args, timeout):
        self.llamadas.append(list(args))
        self.eventos.append(("vbox", " ".join(args[:3])))
        return self.responder(list(args))

    def ok(self, texto, codificacion="utf-8", rc=0, err=""):
        return pv.Resultado(rc, texto.encode(codificacion), err.encode(codificacion), 0.01)

    def responder(self, a):
        if a[0] == "showvminfo":
            return self.ok('name="Win11"\nVMState="%s"\nVMStateChangeTime="%s"\nCurrentSnapshotName="%s"\n'
                           'SharedFolderNameMachineMapping1="educhronos-vm"\n'
                           'SharedFolderPathMachineMapping1="%s"\n' % (self.estado, self.cambio, self.actual, self.compartida))
        if a[0] == "snapshot" and a[2] == "showvminfo":
            return self.ok("Name:                        Win11\nState:                       powered off (since %s)\n" % SINCE)
        if a[0] == "snapshot" and a[2] == "list":
            return self.ok(self.lista)
        if a[0] == "snapshot" and a[2] == "restore":
            if self.restore_rc == 0:
                self.estado, self.actual, self.cambio = "poweroff", a[3], self.tras_restaurar
            return self.ok("Restoring snapshot\n", rc=self.restore_rc)
        if a[0] == "startvm":
            self.estado = "running"
            return self.ok('VM "Win11" has been successfully started.\n')
        if a[0] == "controlvm":
            self.estado, self.cambio = "poweroff", self.tras_apagar
            return self.ok("100%\n")
        if a[0] == "guestcontrol" and a[2] == "copyto":
            return self.ok("")
        if a[0] == "guestcontrol" and a[2] == "run":
            return self.run(a[a.index("--exe") + 1], a[a.index("--") + 1:] if "--" in a else [])
        raise AssertionError("orden no prevista: %r" % a)

    def run(self, exe, argumentos):
        if exe == pv.WHOAMI and not argumentos:
            if self.whoami_fallos:
                self.whoami_fallos -= 1
                return self.ok("", "cp850", rc=1, err="VBoxManage: error: guest not ready")
            return self.ok("educhronos\\prueba-vm\r\n", "cp850")
        if exe == pv.WHOAMI:
            return self.ok(self.grupos, "cp850")
        if exe == pv.QUSER:
            return self.ok(self.quser, "cp850")
        if exe == pv.CMD:
            return self.ok(self.instalar_salida, "cp850", rc=self.instalar_rc)
        if exe == pv.POWERSHELL:
            orden = argumentos[-1]
            paso = orden.splitlines()[0].split(": ", 1)[1]
            return self.ok("\r\n".join(self.powershell("procesos" if paso.startswith("procesos-") else paso)
                                       + ["fin=1"]) + "\r\n", "cp850")
        raise AssertionError("exe no previsto: " + exe)

    def powershell(self, paso):
        if paso == "carpeta":
            return ["carpeta=True"]
        if paso == "sumas":
            return ["fichero=Educhronos-win.zip|True", "fichero=Educhronos-win.zip.sha256|True",
                    "fichero=base-partida-2025-2026.db|True", "sha256-zip=" + self.sha_zip, "md5-base=" + self.md5_base]
        if paso == "extraer":
            return ["instalar=C:\\Users\\prueba-vm\\Documents\\educhronos-prueba\\extraido\\Educhronos\\Instalar.cmd"]
        if paso == "datos-previos":
            return self.datos_previos
        if paso == "colocar":
            return ["colocada=base-partida-2025-2026.db|%d" % int(self.datos[0].rsplit("|", 1)[1])]
        if paso == "medida":
            return ["md5=" + self.md5_instalada, *self.datos, "lnk=" + self.lnk, "exe=" + self.exe]
        if paso == "log":
            return ["migracion=" + x for x in self.log]
        if paso == "procesos":
            return ["proceso=" + x for x in self.procesos]
        raise AssertionError("paso no previsto: " + paso)


class AnfitrionFalso(pv.AnfitrionReal):
    """Inhibidor falso; el manifiesto sí se comprueba con sha256sum, que no toca la VM."""

    def __init__(self, eventos):
        self.eventos, self.lanzados, self.terminados = eventos, [], []
        self.vivos, self.no_muere, self.no_lanza, self.dormido = set(), False, False, 0

    def lanzar_inhibidor(self, argv):
        if self.no_lanza:
            raise FileNotFoundError("systemd-inhibit")
        self.eventos.append(("inhibidor", " ".join(argv)))
        self.lanzados.append(list(argv))
        self.vivos.add(4242)
        return 4242

    def terminar(self, pid):
        self.terminados.append(pid)
        if not self.no_muere:
            self.vivos.discard(pid)

    def vivo(self, pid):
        return pid in self.vivos

    def listar_inhibidores(self):
        return "".join("%s 1000 luis %d systemd-inhibit sleep:idle\n" % (pv.INHIBIDOR_QUIEN, p) for p in self.vivos)

    def dormir(self, segundos):
        self.dormido += segundos


class Escenario(unittest.TestCase):
    def setUp(self):
        entorno = mock.patch.dict(os.environ, {pv.VARIABLE_SIN_VM: "1"})
        entorno.start()
        self.addCleanup(entorno.stop)
        self.carpeta = tempfile.TemporaryDirectory()
        self.addCleanup(self.carpeta.cleanup)
        raiz = pathlib.Path(self.carpeta.name)
        self.marcador = "CLAVE-FALSA-7f3a9c1e"
        self.clave = raiz / "secreto" / "claveWin.txt"
        self.clave.parent.mkdir()
        self.clave.write_text(self.marcador + "\n", encoding="utf-8")
        self.clave.chmod(0o600)
        self.zip = raiz / "v061" / "Educhronos-win.zip"
        self.zip.parent.mkdir()
        self.zip.write_bytes(b"PK zip falso " * 1000)
        self.sha_zip = hashlib.sha256(self.zip.read_bytes()).hexdigest()
        pathlib.Path(str(self.zip) + ".sha256").write_text("%s  Educhronos-win.zip\n" % self.sha_zip, encoding="utf-8")
        self.base = raiz / "base" / "base-partida-2025-2026.db"
        self.base.parent.mkdir()
        self.base.write_bytes(b"SQLite format 3\x00" + b"\x01" * 4000)
        self.md5_base = hashlib.md5(self.base.read_bytes()).hexdigest()
        self.tamano_base = self.base.stat().st_size
        self.material = raiz / "material"
        self.compartida = str(raiz / "compartida")
        self.eventos = []
        self.vm = VMFalsa(self)
        self.anfitrion = AnfitrionFalso(self.eventos)
        self.salida = ""

    def main(self, *argv):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
            rc = pv.main(list(argv), ejecutor=self.vm, anfitrion=self.anfitrion)
        self.salida += out.getvalue()
        return rc

    def preparar(self, *extra):
        return self.main("preparar", "--clave", str(self.clave), "--zip", str(self.zip), "--base", str(self.base),
                         "--md5-base", self.md5_base[:8], "--migracion-esperada", "versionInicial=0 versionFinal=9",
                         "--material", str(self.material), *extra)

    def sub(self, nombre):
        return self.main(nombre, "--material", str(self.material))

    def informe(self):
        lineas = (self.material / "informe.txt").read_text(encoding="utf-8").splitlines()
        return {l.split(" ", 2)[1].rstrip(":"): l.split(" ", 1)[0] for l in lineas}

    def ordenes(self):
        return [" ".join(a[:3]) for a in self.vm.llamadas]

    def corrida_completa(self):
        rcs = [self.preparar(), self.sub("sesion"), self.sub("abierta")]
        self.vm.procesos = []
        rcs.append(self.sub("cerrar"))
        return rcs


class PrecondicionesTest(Escenario):
    def comprobar_rechazo_sin_llamadas(self, rc):
        self.assertEqual(rc, 2)
        self.assertEqual(self.vm.llamadas, [])
        self.assertEqual(self.anfitrion.lanzados, [])

    def test_material_existente(self):
        self.material.mkdir()
        self.comprobar_rechazo_sin_llamadas(self.preparar())
        self.assertEqual(list(self.material.iterdir()), [])

    def test_clave_sin_600(self):
        self.clave.chmod(0o644)
        self.comprobar_rechazo_sin_llamadas(self.preparar())
        self.assertFalse(self.material.exists())

    def test_sha256_distinto(self):
        pathlib.Path(str(self.zip) + ".sha256").write_text("%s  Educhronos-win.zip\n" % ("0" * 64), encoding="utf-8")
        self.comprobar_rechazo_sin_llamadas(self.preparar())
        self.assertFalse(self.material.exists())

    def test_sha256_sin_la_forma(self):
        pathlib.Path(str(self.zip) + ".sha256").write_text(self.sha_zip + " Educhronos-win.zip\n", encoding="utf-8")
        self.comprobar_rechazo_sin_llamadas(self.preparar())

    def test_md5_de_la_base_corto_o_distinto(self):
        for prefijo in (self.md5_base[:7], "0" * 8):
            self.vm.llamadas.clear()
            rc = self.main("preparar", "--clave", str(self.clave), "--zip", str(self.zip), "--base", str(self.base),
                           "--md5-base", prefijo, "--material", str(self.material))
            self.comprobar_rechazo_sin_llamadas(rc)

    def test_material_relativo(self):
        rc = self.main("preparar", "--clave", str(self.clave), "--zip", str(self.zip), "--base", str(self.base),
                       "--md5-base", self.md5_base[:8], "--material", "material-relativo")
        self.comprobar_rechazo_sin_llamadas(rc)


class VMCambiadaTest(Escenario):
    def comprobar_solo_lecturas(self, rc):
        self.assertEqual(rc, 2)
        self.assertEqual([o for o in self.ordenes() if not o.startswith(LECTURAS)], [])
        self.assertEqual(sorted(set(self.ordenes())), ["showvminfo Win11 --machinereadable",
                                                      "snapshot Win11 list", "snapshot Win11 showvminfo"])
        informe = self.informe()
        self.assertEqual(informe["vm-sin-cambios"], "FALLO")
        self.assertEqual({k: v for k, v in informe.items() if k != "vm-sin-cambios"},
                         {k: "DATO" for k in pv.CLAVES_INFORME if k != "vm-sin-cambios"})
        self.assertIn("frente a " + SINCE, (self.material / "informe.txt").read_text(encoding="utf-8"))

    def test_hora_de_cambio_distinta(self):
        self.vm.cambio = TRAS_APAGAR
        self.comprobar_solo_lecturas(self.preparar())

    def test_instantanea_actual_distinta(self):
        self.vm.actual = "s254-admin-clave"
        self.comprobar_solo_lecturas(self.preparar())

    def test_cerrar_tras_el_rechazo_no_toca_la_vm(self):
        self.vm.cambio = TRAS_APAGAR
        self.assertEqual(self.preparar(), 2)
        self.vm.llamadas.clear()
        self.assertEqual(self.sub("cerrar"), 2)
        self.assertEqual([o for o in self.ordenes() if not o.startswith(LECTURAS)], [])
        self.assertEqual(self.vm.llamadas, [])


class ClaveFueraTest(Escenario):
    def test_clave_dentro_del_repo(self):
        with mock.patch.object(pv, "RAIZ_REPO", self.clave.parent.parent):
            self.assertEqual(self.preparar(), 2)
        self.assertNotIn("snapshot Win11 restore", self.ordenes())
        self.assertEqual(self.informe()["clave-fuera"], "FALLO")
        self.assertEqual(self.anfitrion.lanzados, [])

    def test_clave_dentro_de_la_carpeta_compartida(self):
        self.vm.compartida = str(self.clave.parent)
        self.assertEqual(self.preparar(), 2)
        self.assertNotIn("snapshot Win11 restore", self.ordenes())
        self.assertIn("FALLO clave-fuera: carpeta compartida", (self.material / "informe.txt").read_text(encoding="utf-8"))

    def test_clave_dentro_del_material(self):
        self.assertIn("material", pv.clave_fuera(self.material / "claveWin.txt", "/no/repo", [], self.material))
        self.assertIsNone(pv.clave_fuera(self.clave, "/no/repo", [self.compartida], self.material))
        rc = self.main("preparar", "--clave", str(self.material / "claveWin.txt"), "--zip", str(self.zip),
                       "--base", str(self.base), "--md5-base", self.md5_base[:8], "--material", str(self.material))
        self.assertEqual(rc, 2)
        self.assertEqual(self.vm.llamadas, [])

    def test_la_raiz_del_repo_es_la_de_este_fichero(self):
        self.assertTrue((pv.RAIZ_REPO / "tools" / "prueba-vm" / "prueba_vm.py").is_file())


class VeredictosTest(unittest.TestCase):
    def test_traduccion_del_rc(self):
        self.assertEqual(pv.traducir_rc(0), (0, "código 0 en la VM"))
        self.assertEqual(pv.traducir_rc(35), (3, "código 3 en la VM"))
        self.assertEqual(pv.traducir_rc(39), (7, "código 7 en la VM"))
        self.assertEqual(pv.traducir_rc(20), (None, "timeout de guestcontrol"))
        self.assertEqual(pv.traducir_rc(1), (None, "error de VBoxManage 1"))

    def test_seis_lineas(self):
        self.assertEqual(pv.veredicto_lineas(SALIDA_INSTALAR)[0], "OK")
        falta = "\r\n".join(l for l in SALIDA_INSTALAR.split("\r\n") if not l.startswith("Educhronos instalado"))
        self.assertEqual(pv.veredicto_lineas(falta)[0], "FALLO")
        lineas = SALIDA_INSTALAR.split("\r\n")
        lineas[0], lineas[1] = lineas[1], lineas[0]
        self.assertEqual(pv.veredicto_lineas("\r\n".join(lineas))[0], "FALLO")

    def test_sesion(self):
        self.assertEqual(pv.veredicto_sesion(QUSER, "prueba-vm")[0], "OK")
        otra = QUSER.replace(">prueba-vm            ", " educhronos-admin     ")
        v = pv.veredicto_sesion(otra, "prueba-vm")
        self.assertEqual(v[0], "FALLO")
        self.assertIn("educhronos-admin", v[1])

    def test_autoprueba_pasa_y_cuenta_sus_defectos(self):
        problemas, inyectados = pv.autoprueba()
        self.assertEqual(problemas, [])
        self.assertEqual(inyectados, 12)


class PrepararTest(Escenario):
    def test_preparar_limpio_deja_la_vm_encendida_e_imprime_h1(self):
        self.assertEqual(self.preparar(), 0)
        informe = self.informe()
        for clave in ("vm-sin-cambios", "clave-fuera", "arranque", "invitado", "cuenta", "transporte", "suma-zip",
                      "suma-base", "extraer", "datos-previos", "instalar-rc", "instalar-lineas", "md5-instalada",
                      "carpeta-datos", "acceso-directo", "exe"):
            self.assertEqual(informe[clave], "OK", clave)
        self.assertEqual(informe["inhibidor"], "DATO")
        self.assertEqual(self.vm.estado, "running")
        self.assertIn("H1. En la ventana de la VM, entra con la cuenta prueba-vm", self.salida)
        estado = json.loads((self.material / "estado.json").read_text(encoding="utf-8"))
        self.assertTrue(estado["restaurada"])
        self.assertEqual(estado["inhibidor_pid"], 4242)
        instalar = [a for a in self.vm.llamadas if pv.CMD in a][0]
        self.assertEqual(instalar[instalar.index("--") + 1:],
                         ["/c", "C:\\Users\\prueba-vm\\Documents\\educhronos-prueba\\extraido\\Educhronos\\Instalar.cmd < nul"])

    def comprobar_fallo(self, clave, sin_instalar=False):
        self.assertEqual(self.preparar(), 3)
        self.assertEqual(self.informe()[clave], "FALLO")
        self.assertIn("Ejecuta cerrar", self.salida)
        self.assertEqual(self.vm.estado, "running")
        if sin_instalar:
            self.assertFalse(any(pv.CMD in a for a in self.vm.llamadas))

    def test_cuenta_administradora(self):
        self.vm.grupos = GRUPOS + "BUILTIN\\Administradores Alias S-1-5-32-544\r\n"
        self.comprobar_fallo("cuenta", sin_instalar=True)

    def test_cuenta_sin_nivel_medio(self):
        self.vm.grupos = GRUPOS.replace("S-1-16-8192", "S-1-16-12288")
        self.comprobar_fallo("cuenta", sin_instalar=True)

    def test_suma_del_zip(self):
        self.vm.sha_zip = "0" * 64
        self.comprobar_fallo("suma-zip", sin_instalar=True)

    def test_suma_de_la_base(self):
        self.vm.md5_base = "0" * 32
        self.comprobar_fallo("suma-base", sin_instalar=True)

    def test_datos_previos_con_base(self):
        self.vm.datos_previos = ["datos-existe=True", "datos-db=1", "datos-curso-abierto=False"]
        self.comprobar_fallo("datos-previos", sin_instalar=True)

    def test_instalar_aborta_con_codigo_3(self):
        self.vm.instalar_rc = 35
        self.comprobar_fallo("instalar-rc")
        self.assertIn("código 3 en la VM, aborto de Instalar.cmd", (self.material / "informe.txt").read_text(encoding="utf-8"))

    def test_md5_instalado(self):
        self.vm.md5_instalada = "0" * 32
        self.comprobar_fallo("md5-instalada")

    def test_carpeta_de_datos_con_un_db_de_mas(self):
        self.vm.datos = self.vm.datos + ["dato=otra.db|4096"]
        self.comprobar_fallo("carpeta-datos")

    def test_invitado_que_no_responde(self):
        self.vm.whoami_fallos = 99
        self.comprobar_fallo("invitado")
        self.assertEqual(self.anfitrion.dormido, pv.INTENTOS_INVITADO * pv.ESPERA_INVITADO)


class SesionYAbiertaTest(Escenario):
    def test_sesion_de_otro_usuario(self):
        self.assertEqual(self.preparar(), 0)
        self.vm.quser = QUSER.replace(">prueba-vm            ", " educhronos-admin     ")
        self.assertEqual(self.sub("sesion"), 3)
        self.assertIn("educhronos-admin", (self.material / "informe.txt").read_text(encoding="utf-8"))
        self.assertNotIn("H2.", self.salida)

    def test_sesion_de_la_cuenta_imprime_h2(self):
        self.assertEqual(self.preparar(), 0)
        self.assertEqual(self.sub("sesion"), 0)
        self.assertIn('H2. Haz doble clic en el acceso directo Educhronos.', self.salida)

    def test_abierta_sin_linea_de_migracion(self):
        self.assertEqual(self.preparar(), 0)
        self.vm.log = []
        self.assertEqual(self.sub("abierta"), 3)
        self.assertEqual(self.informe()["migracion"], "FALLO")

    def test_abierta_con_otra_migracion(self):
        self.assertEqual(self.preparar(), 0)
        self.vm.log = [MIGRACION.replace("versionFinal=9", "versionFinal=8")]
        self.assertEqual(self.sub("abierta"), 3)
        self.assertEqual(self.informe()["migracion"], "FALLO")

    def test_abierta_sin_procesos(self):
        self.assertEqual(self.preparar(), 0)
        self.vm.procesos = []
        self.assertEqual(self.sub("abierta"), 3)
        self.assertEqual(self.informe()["procesos-abierta"], "FALLO")


class CerrarTest(Escenario):
    def test_corrida_completa(self):
        self.assertEqual(self.corrida_completa(), [0, 0, 0, 0])
        informe = self.informe()
        self.assertEqual({k for k, v in informe.items() if v != "OK"}, set())
        self.assertIn("H3. Clic derecho en el icono de Educhronos", self.salida)
        self.assertEqual(self.vm.estado, "poweroff")
        self.assertEqual(self.vm.cambio, SINCE)
        self.assertEqual(self.anfitrion.terminados, [4242])
        self.assertIn("sha256sum -c rc 0", self.salida)

    def test_proceso_que_no_muere_apaga_y_restaura_igual(self):
        rcs = [self.preparar(), self.sub("sesion"), self.sub("abierta"), self.sub("cerrar")]
        self.assertEqual(rcs, [0, 0, 0, 3])
        self.assertEqual(self.informe()["proceso-terminado"], "FALLO")
        self.assertIn("controlvm Win11 poweroff", self.ordenes())
        self.assertIn("snapshot Win11 restore", self.ordenes()[-3:])
        sondeos = [a for a in self.vm.llamadas if a[-1].startswith("# prueba-vm: procesos-cerrar-")]
        self.assertEqual(len(sondeos), pv.INTENTOS_PROCESOS)

    def test_hora_distinta_tras_restaurar(self):
        self.vm.tras_restaurar = "2026-10-26T22:30:00.000000000"
        self.assertEqual(self.corrida_completa()[-1], 3)
        self.assertEqual(self.informe()["vm-restaurada"], "FALLO")

    def test_regla_que_no_detecta_el_cambio(self):
        self.vm.tras_apagar = SINCE
        self.assertEqual(self.corrida_completa()[-1], 3)
        informe = self.informe()
        self.assertEqual(informe["regla-detecta-cambio"], "FALLO")
        self.assertEqual(informe["vm-restaurada"], "OK")
        orden = self.ordenes()
        self.assertLess(orden.index("controlvm Win11 poweroff"), len(orden) - orden[::-1].index("snapshot Win11 restore") - 1)

    def test_cerrar_tras_un_fallo_posterior_a_restaurar(self):
        self.vm.sha_zip = "0" * 64
        self.assertEqual(self.preparar(), 3)
        self.assertEqual(self.sub("cerrar"), 3)
        self.assertEqual(self.ordenes()[-5:-2], ["controlvm Win11 poweroff", "showvminfo Win11 --machinereadable",
                                                 "snapshot Win11 restore"])
        self.assertEqual(self.vm.estado, "poweroff")
        informe = self.informe()
        self.assertEqual((informe["proceso-terminado"], informe["vm-restaurada"], informe["instantaneas"]),
                         ("DATO", "OK", "OK"))

    def test_inhibidor_antes_del_restore_y_soltado_por_su_pid(self):
        self.corrida_completa()
        self.assertEqual(self.anfitrion.lanzados, [pv.ORDEN_INHIBIDOR])
        self.assertEqual(pv.ORDEN_INHIBIDOR, ["systemd-inhibit", "--what=sleep:idle", "--who=educhronos-prueba-vm",
                                              "--why=prueba de versión en la VM", "sleep", "4h"])
        i = self.eventos.index(("inhibidor", " ".join(pv.ORDEN_INHIBIDOR)))
        self.assertEqual(self.eventos[i + 1], ("vbox", "snapshot Win11 restore"))
        self.assertEqual(self.anfitrion.terminados, [4242])
        self.assertEqual(self.informe()["inhibidor"], "OK")

    def test_inhibidor_que_sigue_vivo(self):
        self.anfitrion.no_muere = True
        self.assertEqual(self.corrida_completa()[-1], 3)
        self.assertEqual(self.informe()["inhibidor"], "FALLO")

    def test_sin_systemd_inhibit_es_dato_y_sigue(self):
        self.anfitrion.no_lanza = True
        self.assertEqual(self.corrida_completa(), [0, 0, 0, 0])
        self.assertEqual(self.informe()["inhibidor"], "DATO")


class CredencialesTest(Escenario):
    def test_la_clave_solo_viaja_como_ruta(self):
        clave = os.path.realpath(self.clave)
        abrir_original = open

        def vigilado(f, *a, **k):
            if isinstance(f, (str, bytes, os.PathLike)) and os.path.realpath(os.fsdecode(f)) == clave:
                raise AssertionError("se ha abierto el fichero de contraseñas")
            return abrir_original(f, *a, **k)

        def prohibido(self_, *a, **k):
            raise AssertionError("se ha leído el fichero de contraseñas")

        with mock.patch("builtins.open", vigilado), mock.patch("io.open", vigilado), \
                mock.patch.object(pathlib.Path, "read_text", lambda s, *a, **k: prohibido(s) if os.path.realpath(s) == clave
                                  else pathlib.Path.read_bytes(s).decode(k.get("encoding") or "utf-8")), \
                mock.patch.object(pathlib.Path, "read_bytes", lambda s: prohibido(s) if os.path.realpath(s) == clave
                                  else abrir_original(s, "rb").read()):
            self.assertEqual(self.corrida_completa(), [0, 0, 0, 0])
        self.assertNotIn(self.marcador, self.salida)
        for f in self.material.rglob("*"):
            if f.is_file():
                self.assertNotIn(self.marcador.encode(), f.read_bytes(), str(f))
        guestcontrol = [a for a in self.vm.llamadas if a[0] == "guestcontrol"]
        self.assertGreater(len(guestcontrol), 10)
        for a in guestcontrol:
            self.assertEqual(a[a.index("--passwordfile") + 1], str(self.clave))
            self.assertEqual(a[a.index("--username") + 1], "prueba-vm")

    def test_las_ordenes_guardadas_llevan_la_ruta_y_no_el_contenido(self):
        self.corrida_completa()
        ordenes = sorted((self.material / "ordenes").glob("*.orden"))
        self.assertTrue(any(str(self.clave) in o.read_text(encoding="utf-8") for o in ordenes))


class EjecutorRealTest(unittest.TestCase):
    def test_se_niega_sin_vm(self):
        with mock.patch.dict(os.environ, {pv.VARIABLE_SIN_VM: "1"}):
            with self.assertRaises(pv.SinVM):
                pv.EjecutorReal(lanzar=lambda *a, **k: self.fail("no debía lanzar"))(["list", "vms"], 10)
            with self.assertRaises(pv.SinVM):
                pv.AnfitrionReal().lanzar_inhibidor(["true"])

    def lanzar_falso(self, respuestas):
        llamadas = []

        def lanzar(argv, **k):
            llamadas.append((argv, k))
            rc, err = respuestas.pop(0)
            return mock.Mock(returncode=rc, stdout=b"salida", stderr=err)
        return lanzar, llamadas

    def test_repite_una_vez_si_el_com_no_responde(self):
        lanzar, llamadas = self.lanzar_falso([(1, b"Failed to create the VirtualBox object! COM server is not running"),
                                              (0, b"")])
        dormido = []
        with mock.patch.dict(os.environ, {pv.VARIABLE_SIN_VM: "0"}):
            r = pv.EjecutorReal(lanzar=lanzar, dormir=dormido.append)(["snapshot", "Win11", "list"], 10)
        self.assertEqual((r.rc, len(llamadas), dormido), (0, 2, [15]))
        self.assertIn(b"COM server is not running", r.previo.stderr)

    def test_no_repite_mas_de_una_vez(self):
        caido = (1, b"COM server is not running")
        lanzar, llamadas = self.lanzar_falso([caido, caido, (0, b"")])
        with mock.patch.dict(os.environ, {pv.VARIABLE_SIN_VM: "0"}):
            r = pv.EjecutorReal(lanzar=lanzar, dormir=lambda s: None)(["list", "vms"], 10)
        self.assertEqual((r.rc, len(llamadas)), (1, 2))

    def test_pasa_el_entorno_tal_cual(self):
        for extra, quitar in (({"LC_ALL": "es_ES.UTF-8", "LANG": "es_ES.UTF-8", "LC_MESSAGES": "es_ES.UTF-8"}, ()),
                              ({"LANG": "es_ES.UTF-8"}, ("LC_ALL",))):
            lanzar, llamadas = self.lanzar_falso([(0, b"")])
            with mock.patch.dict(os.environ, {pv.VARIABLE_SIN_VM: "0", **extra}):
                for v in quitar:
                    os.environ.pop(v, None)
                pv.EjecutorReal(lanzar=lanzar)(["list", "vms"], 10)
                self.assertEqual(llamadas[0][1]["env"], dict(os.environ))
                self.assertEqual(llamadas[0][0], ["VBoxManage", "list", "vms"])
                self.assertNotIn("shell", llamadas[0][1])


class MainTest(Escenario):
    def test_codigos(self):
        self.assertEqual(self.main("--solo-autoprueba"), 0)
        with mock.patch.object(pv, "casos_autoprueba", lambda: [("x", lambda: ("OK", ""), lambda: ("OK", ""))]):
            self.assertEqual(self.main("--solo-autoprueba"), 4)
            self.assertEqual(self.preparar(), 4)
        self.assertEqual(self.vm.llamadas, [])
        self.assertIn("(x) el defecto no da FALLO", self.salida)
        self.assertEqual(self.sub("sesion"), 2)
        self.vm.md5_instalada = "0" * 32
        self.assertEqual(self.preparar(), 3)

    def test_codigo_0(self):
        self.assertEqual(self.preparar(), 0)


if __name__ == "__main__":
    unittest.main()
