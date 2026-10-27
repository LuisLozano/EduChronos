#!/usr/bin/env python3
"""Prueba una versión de Educhronos en la VM Win11 desde el anfitrión (C-script-prueba-vm, S255).

Condiciones 1 a 3 de O-prueba-vm. Parte de una instantánea en la que la cuenta estándar nunca ha
iniciado sesión (s254-cuenta-prueba, decisión GB), y vuelve a ella al cerrar. Hace él la parte
no visual de la prueba (los pasos A y B de s252/v1) y deja al usuario solo tres pasos: H1 entrar
con la cuenta, H2 abrir la aplicación y H3 salir desde la bandeja.

Subcomandos, en este orden; preparar escribe <material>/estado.json y los demás lo leen:
  preparar  autoprueba; comprueba el anfitrión y que la VM no ha cambiado desde la instantánea;
            restaura, arranca, espera al invitado, mide la cuenta, copia el zip y la base,
            comprueba sus sumas, extrae, coloca la base, instala y mide; imprime H1.
  sesion    la sesión de la consola es de la cuenta (quser); imprime H2.
  abierta   línea de migración del log y procesos de la aplicación; imprime H3.
  cerrar    proceso terminado tras Salir, apaga, mide la regla de «VM sin cambios», restaura,
            compara y suelta el inhibidor de suspensión; informe final y MANIFIESTO.sha256.
Si algo falla con la VM encendida, la VM se queda encendida y el informe dice que se ejecute
cerrar (GJ). cerrar solo apaga y restaura si preparar llegó a restaurar (E35).

La contraseña solo viaja como ruta, en --passwordfile: el script hace os.stat del fichero y nunca
lo abre. Toda llamada a VBoxManage pasa por un ejecutor inyectable; el real se niega a correr con
EDUCHRONOS_PRUEBA_VM_SIN_VM=1, que ponen los tests. Antes de ninguna cifra corre la AUTOPRUEBA:
inyecta un defecto por capacidad en las mismas funciones de veredicto que la corrida real y exige
FALLO en cada uno y OK en su caso limpio.

Uso:
  python3 -B tools/prueba-vm/prueba_vm.py preparar --clave /ruta/clave.txt \
      --zip /ruta/Educhronos-win.zip --base /ruta/base.db --md5-base d867c2de \
      --material /ruta/material [--migracion-esperada 'versionInicial=0 versionFinal=9']
  python3 -B tools/prueba-vm/prueba_vm.py {sesion|abierta|cerrar} --material /ruta/material
  python3 -B tools/prueba-vm/prueba_vm.py --solo-autoprueba
Salida: 0 todo OK; 1 fallo inesperado; 2 rechazo previsto, sin tocar la VM; 3 alguna
comprobación da FALLO; 4 la autoprueba no pasa.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import ntpath
import os
import re
import shlex
import signal
import stat
import subprocess
import time
from pathlib import Path

VM_POR_DEFECTO = "Win11"
INSTANTANEA_POR_DEFECTO = "s254-cuenta-prueba"
CUENTA_POR_DEFECTO = "prueba-vm"
VARIABLE_SIN_VM = "EDUCHRONOS_PRUEBA_VM_SIN_VM"
RAIZ_REPO = Path(__file__).resolve().parents[2]

# Medido en el T1 de S255 (p3-regla.txt): rc de VBoxManage = código en la VM + 32; el 0 da 0;
# un run que agota su --timeout da 20.
K_RC = 32
RC_TIMEOUT_GUESTCONTROL = 20
COM_CAIDO = b"COM server is not running"
ESPERA_COM = 15

WHOAMI = r"C:\Windows\System32\whoami.exe"
CMD = r"C:\Windows\System32\cmd.exe"
POWERSHELL = r"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"
QUSER = r"C:\Windows\System32\quser.exe"

GRUPO_ADMINISTRADORES = "S-1-5-32-544"
NIVEL_MEDIO = "S-1-16-8192"
PREFIJOS_INSTALAR = (
    "[1/3] Programa en",
    "[2/3] Acceso directo en el Escritorio",
    "[3/3] Base de partida",
    "Base de partida copiada a",
    "Educhronos instalado en",
    "Abre Educhronos con el acceso directo del Escritorio.",
)
CODIGOS_ABORTO_INSTALAR = range(2, 7)
LONGITUD_HUELLA_MINIMA = 8
SHA256_ANFITRION = re.compile(r"^([0-9a-fA-F]{64})  (\S[^\r\n]*)\r?\n?$")
NOMBRE_CUENTA = re.compile(r"^[A-Za-z0-9._-]+$")
SINCE = re.compile(r"^State:\s.*\(since ([^)]+)\)\s*$", re.M)

INTENTOS_INVITADO, ESPERA_INVITADO = 20, 15
INTENTOS_PROCESOS, ESPERA_PROCESOS = 12, 5
INTENTOS_APAGADO, ESPERA_APAGADO = 24, 5
# E51 (S256, s256/diag/notas.txt): a los 30 ms del restore VMStateChangeTime aún marca la hora del
# restore; vuelve al «since» después. Se relee una vez por segundo, 30 s como máximo.
INTENTOS_RESTAURADA, ESPERA_RESTAURADA = 30, 1
MS_WHOAMI, MS_POWERSHELL, MS_LARGO = 60000, 120000, 600000

INHIBIDOR_QUIEN = "educhronos-prueba-vm"
ORDEN_INHIBIDOR = ["systemd-inhibit", "--what=sleep:idle", "--who=" + INHIBIDOR_QUIEN,
                   "--why=prueba de versión en la VM", "sleep", "4h"]

CLAVES_INFORME = (
    "vm-sin-cambios", "clave-fuera", "arranque", "invitado", "cuenta", "transporte", "suma-zip",
    "suma-base", "extraer", "datos-previos", "instalar-rc", "instalar-lineas", "md5-instalada",
    "carpeta-datos", "acceso-directo", "exe", "sesion-cuenta", "migracion", "procesos-abierta",
    "proceso-terminado", "regla-detecta-cambio", "vm-restaurada", "instantaneas", "inhibidor",
)

H1 = ("H1. En la ventana de la VM, entra con la cuenta {cuenta} —elige ESA cuenta en la pantalla"
      " de inicio— y teclea tú la contraseña. Si salen pantallas del primer inicio, elige en cada"
      " una la opción que menos datos comparta y avanza hasta el escritorio. Dime si ves en el"
      " Escritorio un acceso directo llamado Educhronos.")
H2 = ("H2. Haz doble clic en el acceso directo Educhronos. Si el navegador enseña su bienvenida,"
      " sigue el anexo del manual (\"la primera vez que se abre el navegador\"). Dime: (a) si viste"
      " la ventana \"Educhronos se está iniciando\"; (b) si acaba saliendo la aplicación, con la"
      " barra que dice el curso.")
H3 = ("H3. Clic derecho en el icono de Educhronos de la bandeja y elige \"Salir\". Dime si el icono"
      " desaparece.")


class Rechazo(Exception):
    """Rechazo previsto: la VM no se ha tocado (rc 2)."""


class FalloComprobacion(Exception):
    """Una comprobación ha dado FALLO (rc 3)."""


class AutopruebaNoPasa(Exception):
    """La autoprueba no pasa (rc 4)."""


class SinVM(RuntimeError):
    """El ejecutor real se niega a correr con EDUCHRONOS_PRUEBA_VM_SIN_VM=1."""


class Resultado:
    def __init__(self, rc, stdout, stderr, duracion, previo=None):
        self.rc, self.stdout, self.stderr, self.duracion, self.previo = rc, stdout, stderr, duracion, previo


# ------------------------------------------------------------------------- ejecutores reales

def negarse_sin_vm():
    if os.environ.get(VARIABLE_SIN_VM) == "1":
        raise SinVM(VARIABLE_SIN_VM + "=1: el ejecutor real no toca la VM")


class EjecutorReal:
    """La única puerta a VBoxManage: lista de argumentos y timeout; rc, stdout, stderr y duración.

    El entorno se pasa tal cual (GL): con LC_ALL=C, VBoxManage corta la salida en el primer
    carácter no ASCII. Ante «COM server is not running» repite una vez a los 15 s y decide la
    segunda, guardando la primera en `previo` (norma 2 de M-guion).
    """

    def __init__(self, lanzar=subprocess.run, dormir=time.sleep, reloj=time.monotonic):
        self.lanzar, self.dormir, self.reloj = lanzar, dormir, reloj

    def __call__(self, args, timeout):
        negarse_sin_vm()
        r = self.una(args, timeout)
        if COM_CAIDO in r.stderr:
            self.dormir(ESPERA_COM)
            segunda = self.una(args, timeout)
            segunda.previo = r
            return segunda
        return r

    def una(self, args, timeout):
        t0 = self.reloj()
        p = self.lanzar(["VBoxManage", *args], capture_output=True, timeout=timeout, env=dict(os.environ))
        return Resultado(p.returncode, p.stdout, p.stderr, self.reloj() - t0)


class AnfitrionReal:
    """Procesos del anfitrión que no son VBoxManage: el inhibidor de suspensión y el manifiesto."""

    def lanzar_inhibidor(self, argv):
        negarse_sin_vm()
        return subprocess.Popen(argv, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                                stderr=subprocess.DEVNULL, start_new_session=True).pid

    def terminar(self, pid):
        negarse_sin_vm()
        os.kill(pid, signal.SIGTERM)

    def vivo(self, pid):
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return False
        except PermissionError:
            return True
        return True

    def listar_inhibidores(self):
        negarse_sin_vm()
        return subprocess.run(["systemd-inhibit", "--list"], capture_output=True, text=True).stdout

    def dormir(self, segundos):
        time.sleep(segundos)

    def comprobar_manifiesto(self, material):
        return subprocess.run(["sha256sum", "-c", "--quiet", "MANIFIESTO.sha256"], cwd=material,
                              capture_output=True).returncode


# --------------------------------------------------------------------- lectura de salidas

def leer_machinereadable(texto):
    claves = {}
    for linea in texto.splitlines():
        if "=" not in linea:
            continue
        k, v = linea.split("=", 1)
        claves[k.strip('"')] = v.strip().strip('"')
    return claves


def leer_since(texto):
    m = SINCE.search(texto)
    return m.group(1) if m else None


def leer_ps(texto):
    """Líneas «clave=valor» de una orden de PowerShell; None si falta «fin=1» (salida cortada)."""
    claves = {}
    for linea in texto.splitlines():
        linea = linea.rstrip("\r")
        if "=" in linea:
            k, v = linea.split("=", 1)
            claves.setdefault(k, []).append(v)
    return claves if claves.get("fin") == ["1"] else None


def traducir_rc(rc):
    """rc de guestcontrol run → (código en la VM o None, texto). Tabla de E34."""
    if rc == 0:
        return 0, "código 0 en la VM"
    if rc == K_RC:
        # E41: el 0 llega como 0, así que un 32 no sale de la regla medida.
        return None, "rc %d no observado" % rc
    if K_RC < rc <= 255:
        return rc - K_RC, "código %d en la VM" % (rc - K_RC)
    if rc == RC_TIMEOUT_GUESTCONTROL:
        return None, "timeout de guestcontrol"
    return None, "error de VBoxManage %d" % rc


# ------------------------------------------------------------- veredictos (y autoprueba)

def mismo_instante(a, b):
    return a is not None and a == b


def veredicto_vm_sin_cambios(vm, instantanea, since):
    cifra = "%s, %s, %s frente a %s" % (vm.get("VMState"), vm.get("CurrentSnapshotName"),
                                       vm.get("VMStateChangeTime"), since)
    ok = (vm.get("VMState") == "poweroff" and vm.get("CurrentSnapshotName") == instantanea
          and mismo_instante(vm.get("VMStateChangeTime"), since))
    return ("OK" if ok else "FALLO", cifra)


def veredicto_cuenta(grupos):
    admin, medio = GRUPO_ADMINISTRADORES in grupos, NIVEL_MEDIO in grupos
    cifra = "%s %s, %s %s" % (GRUPO_ADMINISTRADORES, "presente" if admin else "ausente",
                              NIVEL_MEDIO, "presente" if medio else "ausente")
    return ("OK" if not admin and medio else "FALLO", cifra)


def veredicto_suma(esperada, obtenida):
    ok = bool(esperada) and bool(obtenida) and esperada.lower() == obtenida.lower()
    return ("OK" if ok else "FALLO", "esperada %s, en la VM %s" % (esperada, obtenida))


def veredicto_instalar_rc(rc):
    codigo, texto = traducir_rc(rc)
    if codigo in CODIGOS_ABORTO_INSTALAR:
        texto += ", aborto de Instalar.cmd"
    return ("OK" if codigo == 0 else "FALLO", "rc %d, %s" % (rc, texto))


def veredicto_lineas(stdout):
    hallados, posiciones = 0, []
    for n, linea in enumerate(stdout.splitlines(), 1):
        if hallados < len(PREFIJOS_INSTALAR) and linea.lstrip().startswith(PREFIJOS_INSTALAR[hallados]):
            posiciones.append(n)
            hallados += 1
    cifra = "%d de %d en orden, líneas %s" % (hallados, len(PREFIJOS_INSTALAR), posiciones)
    return ("OK" if hallados == len(PREFIJOS_INSTALAR) else "FALLO", cifra)


def veredicto_md5_instalada(esperada, obtenida):
    ok = obtenida is not None and obtenida.lower() == esperada.lower()
    return ("OK" if ok else "FALLO", "esperado %s, instalado %s" % (esperada, obtenida))


def veredicto_carpeta_datos(entradas, tamano_base):
    ok = entradas == [["educhronos.db", str(tamano_base)]]
    return ("OK" if ok else "FALLO", "%s; se espera solo educhronos.db de %d B" % (entradas, tamano_base))


def veredicto_sesion(quser, cuenta):
    consola = []
    for linea in quser.splitlines():
        partes = linea.strip().lstrip(">").split()
        if len(partes) >= 2 and partes[1].lower() == "console":
            consola.append(partes[0])
    ok = any(u.lower() == cuenta.lower() for u in consola)
    return ("OK" if ok else "FALLO", "sesión de consola de %s; se espera %s" % (", ".join(consola) or "nadie", cuenta))


def veredicto_migracion(lineas, esperada):
    if not lineas:
        return ("FALLO", "sin línea «Esquema preparado» en el log")
    if esperada is None:
        return ("DATO", lineas[0])
    return ("OK" if any(esperada in l for l in lineas) else "FALLO", lineas[0])


def veredicto_procesos_abierta(procesos):
    return ("OK" if len(procesos) >= 1 else "FALLO", "%d procesos: %s" % (len(procesos), procesos))


def veredicto_proceso_terminado(procesos):
    return ("OK" if len(procesos) == 0 else "FALLO", "%d procesos: %s" % (len(procesos), procesos))


def veredicto_regla_detecta_cambio(tras_apagar, since):
    ok = tras_apagar is not None and not mismo_instante(tras_apagar, since)
    return ("OK" if ok else "FALLO", "tras apagar %s, since %s" % (tras_apagar, since))


def veredicto_vm_restaurada(vm, instantanea, since):
    return veredicto_vm_sin_cambios(vm, instantanea, since)


def clave_fuera(clave, raiz_repo, compartidas, material):
    """Dónde cuelga la clave, si cuelga de algo prohibido (E40); None si está fuera."""
    c = Path(clave).resolve()
    for nombre, carpeta in [("repo", raiz_repo), *[("carpeta compartida", x) for x in compartidas],
                            ("material", material)]:
        if c.is_relative_to(Path(carpeta).resolve()):
            return "%s %s" % (nombre, carpeta)
    return None


def casos_autoprueba():
    """(capacidad, defecto, limpio): el defecto tiene que dar FALLO y el limpio OK."""
    since = "2026-10-26T15:05:34.000000000"
    vm = {"VMState": "poweroff", "CurrentSnapshotName": "s", "VMStateChangeTime": since}
    grupos = "BUILTIN\\Usuarios S-1-5-32-545\nNivel obligatorio medio S-1-16-8192\n"
    salida = ("[1/3] Programa en C:\\P\n[2/3] Acceso directo en el Escritorio\n[3/3] Base de partida\n"
              "    Base de partida copiada a C:\\D\\educhronos.db\nEduchronos instalado en C:\\P.\n"
              "Abre Educhronos con el acceso directo del Escritorio.\n")
    sin_cuarta = "\n".join(l for l in salida.splitlines() if "copiada" not in l)
    md5 = "d867c2dec4a9ab01357f539d8b09f7e2"
    migracion = ["Esquema preparado ruta=C:\\D\\educhronos.db versionInicial=0 versionFinal=9"]
    quser = " USUARIO SESION ID\n>prueba-vm console 2 Activo\n"
    return [
        ("a", lambda: veredicto_suma("a" * 64, "B" * 64), lambda: veredicto_suma("a" * 64, "A" * 64)),
        ("b", lambda: veredicto_suma(md5, "0" * 32), lambda: veredicto_suma(md5, md5.upper())),
        ("c", lambda: veredicto_cuenta(grupos + "BUILTIN\\Administradores S-1-5-32-544\n"),
         lambda: veredicto_cuenta(grupos)),
        ("d", lambda: veredicto_cuenta(grupos.replace(NIVEL_MEDIO, "S-1-16-12288")),
         lambda: veredicto_cuenta(grupos)),
        ("e", lambda: veredicto_instalar_rc(35), lambda: veredicto_instalar_rc(0)),
        ("f", lambda: veredicto_lineas(sin_cuarta), lambda: veredicto_lineas(salida)),
        ("g", lambda: veredicto_vm_sin_cambios(dict(vm, VMStateChangeTime="2026-10-26T21:14:10.000000000"), "s", since),
         lambda: veredicto_vm_sin_cambios(vm, "s", since)),
        ("h", lambda: veredicto_migracion(["Esquema preparado versionInicial=0 versionFinal=8"], "versionInicial=0 versionFinal=9"),
         lambda: veredicto_migracion(migracion, "versionInicial=0 versionFinal=9")),
        ("i", lambda: veredicto_proceso_terminado(["6120|Educhronos.exe|2"]), lambda: veredicto_proceso_terminado([])),
        ("j", lambda: veredicto_sesion(quser.replace("prueba-vm", "educhronos-admin"), "prueba-vm"),
         lambda: veredicto_sesion(quser, "prueba-vm")),
        ("k", lambda: veredicto_md5_instalada(md5, "0" * 32), lambda: veredicto_md5_instalada(md5, md5.upper())),
        ("l", lambda: veredicto_regla_detecta_cambio(since, since),
         lambda: veredicto_regla_detecta_cambio("2026-10-26T21:14:10.000000000", since)),
    ]


def autoprueba():
    """Lista de problemas (vacía si pasa) y número de defectos inyectados. No llama al ejecutor."""
    problemas = []
    casos = casos_autoprueba()
    for capacidad, defecto, limpio in casos:
        try:
            if defecto()[0] != "FALLO":
                problemas.append("(%s) el defecto no da FALLO" % capacidad)
            if limpio()[0] != "OK":
                problemas.append("(%s) el caso limpio no da OK" % capacidad)
        except Exception as e:  # noqa: BLE001 — un caso que revienta tampoco pasa
            problemas.append("(%s) excepción inesperada: %r" % (capacidad, e))
    return problemas, len(casos)


# ------------------------------------------------------------------------------ anfitrión

def huella(ruta, algoritmo):
    h = hashlib.new(algoritmo)
    with open(ruta, "rb") as f:
        for trozo in iter(lambda: f.read(1 << 20), b""):
            h.update(trozo)
    return h.hexdigest()


def comprobar_anfitrion(args):
    """2.1(b): nada toca la VM ni escribe. Devuelve las huellas del zip y de la base."""
    if not NOMBRE_CUENTA.match(args.cuenta):
        raise Rechazo("nombre de cuenta no admitido: %r" % args.cuenta)
    for ruta in (args.zip, args.base):
        if "'" in os.path.basename(ruta):
            raise Rechazo("el nombre lleva una comilla simple: " + ruta)
    if os.path.exists(args.material):
        raise Rechazo("la carpeta de material ya existe y no se toca: " + args.material)
    try:
        st = os.stat(args.clave)
    except FileNotFoundError:
        raise Rechazo("no existe el fichero de contraseñas: " + args.clave) from None
    if not stat.S_ISREG(st.st_mode) or st.st_uid != os.getuid() or stat.S_IMODE(st.st_mode) != 0o600:
        raise Rechazo("el fichero de contraseñas tiene que ser un fichero regular del usuario con modo"
                      " 600; es %o" % stat.S_IMODE(st.st_mode))
    sha_fichero = args.zip + ".sha256"
    if not os.path.isfile(args.zip) or not os.path.isfile(sha_fichero):
        raise Rechazo("faltan el zip o su .sha256: " + args.zip)
    with open(sha_fichero, encoding="utf-8") as f:
        m = SHA256_ANFITRION.match(f.read())
    if not m:
        raise Rechazo("el .sha256 no tiene la forma «64 hex␠␠nombre»: " + sha_fichero)
    sha_zip = huella(args.zip, "sha256")
    if m.group(1).lower() != sha_zip:
        raise Rechazo("el SHA-256 del zip (%s) no es el de su .sha256 (%s)" % (sha_zip, m.group(1)))
    if not os.path.isfile(args.base):
        raise Rechazo("no existe la base: " + args.base)
    md5_base = huella(args.base, "md5")
    esperada = args.md5_base.strip().lower()
    if len(esperada) < LONGITUD_HUELLA_MINIMA or not md5_base.startswith(esperada):
        raise Rechazo("el md5 de la base (%s) no empieza por el esperado (%s)" % (md5_base, esperada))
    return {"sha256_zip": sha_zip, "md5_base": md5_base, "tamano_base": os.path.getsize(args.base)}


# ---------------------------------------------------------------------------- la prueba

class Prueba:
    def __init__(self, material, estado, ejecutor, anfitrion):
        self.material, self.estado, self.ejecutor, self.anfitrion = Path(material), estado, ejecutor, anfitrion

    @classmethod
    def cargar(cls, material, ejecutor, anfitrion):
        ruta = Path(material) / "estado.json"
        if not ruta.is_file():
            raise Rechazo("no hay estado.json en el material: ejecuta antes preparar")
        return cls(material, json.loads(ruta.read_text(encoding="utf-8")), ejecutor, anfitrion)

    def guardar(self):
        (self.material / "estado.json").write_text(
            json.dumps(self.estado, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (self.material / "informe.txt").write_text(texto_informe(self.estado["informe"]), encoding="utf-8")

    def anotar(self, clave, veredicto):
        if clave not in CLAVES_INFORME:
            raise ValueError("clave de informe desconocida: " + clave)
        self.estado["informe"][clave] = list(veredicto)
        self.guardar()
        print("%s %s: %s" % (veredicto[0], clave, veredicto[1]))

    def fallar(self, clave, veredicto):
        self.anotar(clave, veredicto)
        raise FalloComprobacion(clave)

    # Toda llamada a VBoxManage pasa por aquí: la hace el ejecutor y la guarda en ordenes/.
    def vbox(self, nombre, args, timeout, invitado=False):
        r = self.ejecutor(args, timeout)
        self.estado["ordenes"] += 1
        base = self.material / "ordenes" / ("%03d-%s" % (self.estado["ordenes"], nombre))
        codificacion = "cp850" if invitado else "utf-8"
        for sufijo, res in ((".intento1", r.previo), ("", r)):
            if res is None:
                continue
            p = str(base) + sufijo
            Path(p + ".orden").write_text(shlex.join(["VBoxManage", *args]) + "\n", encoding="utf-8")
            Path(p + ".rc").write_text("%d\n" % res.rc, encoding="utf-8")
            Path(p + ".dur").write_text("%.2f\n" % res.duracion, encoding="utf-8")
            Path(p + ".stdout.raw").write_bytes(res.stdout)
            Path(p + ".stdout.txt").write_text(res.stdout.decode(codificacion, "replace"), encoding="utf-8")
            Path(p + ".stderr.txt").write_text(res.stderr.decode(codificacion, "replace"), encoding="utf-8")
        return r

    def texto(self, r, invitado=True):
        return r.stdout.decode("cp850" if invitado else "utf-8", "replace")

    def run(self, nombre, exe, argumentos, ms):
        e = self.estado
        args = ["guestcontrol", e["vm"], "run", "--exe", exe, "--username", e["cuenta"],
                "--passwordfile", e["clave"], "--wait-stdout", "--wait-stderr", "--timeout=%d" % ms]
        if argumentos:
            args += ["--", *argumentos]
        return self.vbox(nombre, args, ms / 1000 + 60, invitado=True)

    def ps(self, nombre, cuerpo, ms=MS_POWERSHELL, **valores):
        for k, v in valores.items():
            cuerpo = cuerpo.replace("<<%s>>" % k, v)
        orden = "# prueba-vm: %s\n$ErrorActionPreference = 'Stop'\n%s\n'fin=1'" % (nombre, cuerpo)
        r = self.run(nombre, POWERSHELL, ["-NoProfile", "-NonInteractive", "-Command", orden], ms)
        codigo, _ = traducir_rc(r.rc)
        return r, (leer_ps(self.texto(r)) if codigo == 0 else None)

    def copyto(self, nombre, fichero):
        e = self.estado
        args = ["guestcontrol", e["vm"], "copyto", "--username", e["cuenta"], "--passwordfile", e["clave"],
                "--target-directory=" + e["g"] + "\\", fichero]
        return self.vbox(nombre, args, 1800, invitado=True)

    def leer_vm(self, nombre):
        r = self.vbox(nombre, ["showvminfo", self.estado["vm"], "--machinereadable"], 120)
        return leer_machinereadable(self.texto(r, invitado=False)) if r.rc == 0 else {}

    def procesos(self, nombre):
        r, d = self.ps(nombre, PS_PROCESOS)
        return None if d is None else d.get("proceso", [])


def texto_informe(informe):
    lineas = []
    for clave in CLAVES_INFORME:
        estado, cifra = informe.get(clave, ("DATO", "no medido"))
        lineas.append("%s %s: %s" % (estado, clave, cifra))
    return "\n".join(lineas) + "\n"


PS_CARPETA = r"""New-Item -ItemType Directory -Force -Path '<<g>>' | Out-Null
'carpeta=' + (Test-Path -LiteralPath '<<g>>' -PathType Container)"""
PS_SUMAS = r"""foreach ($n in '<<zip>>','<<sha>>','<<base>>') { 'fichero=' + $n + '|' + (Test-Path -LiteralPath ('<<g>>\' + $n) -PathType Leaf) }
'sha256-zip=' + (Get-FileHash -Algorithm SHA256 -LiteralPath '<<g>>\<<zip>>').Hash
'md5-base=' + (Get-FileHash -Algorithm MD5 -LiteralPath '<<g>>\<<base>>').Hash"""
PS_EXTRAER = r"""Expand-Archive -LiteralPath '<<g>>\<<zip>>' -DestinationPath '<<g>>\extraido'
Get-ChildItem -LiteralPath '<<g>>\extraido' -Recurse -Filter 'Instalar.cmd' | ForEach-Object { 'instalar=' + $_.FullName }"""
PS_DATOS_PREVIOS = r"""$d = Join-Path $env:LOCALAPPDATA 'Educhronos'
'datos-existe=' + (Test-Path -LiteralPath $d)
if (Test-Path -LiteralPath $d) { 'datos-db=' + @(Get-ChildItem -LiteralPath $d -Filter '*.db').Count; 'datos-curso-abierto=' + (Test-Path -LiteralPath (Join-Path $d 'curso-abierto')) }"""
PS_COLOCAR = r"""New-Item -ItemType Directory -Path '<<ed>>\base-partida' | Out-Null
Copy-Item -LiteralPath '<<g>>\<<base>>' -Destination '<<ed>>\base-partida\'
Get-ChildItem -LiteralPath '<<ed>>\base-partida' | ForEach-Object { 'colocada=' + $_.Name + '|' + $_.Length }"""
PS_MEDIDA = r"""$d = Join-Path $env:LOCALAPPDATA 'Educhronos'
$b = Join-Path $d 'educhronos.db'
if (Test-Path -LiteralPath $b) { 'md5=' + (Get-FileHash -Algorithm MD5 -LiteralPath $b).Hash } else { 'md5=AUSENTE' }
if (Test-Path -LiteralPath $d) { Get-ChildItem -LiteralPath $d | ForEach-Object { 'dato=' + $_.Name + '|' + $_.Length } }
'lnk=' + (Test-Path -LiteralPath (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Educhronos.lnk'))
'exe=' + (Test-Path -LiteralPath (Join-Path $env:LOCALAPPDATA 'Programs\Educhronos\Educhronos.exe'))"""
PS_LOG = r"""$l = Join-Path $env:LOCALAPPDATA 'Educhronos\educhronos.log'
if (Test-Path -LiteralPath $l) { Select-String -LiteralPath $l -Pattern 'Esquema preparado' | ForEach-Object { 'migracion=' + $_.Line } } else { 'log=AUSENTE' }"""
PS_PROCESOS = r"""Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -like '*\Programs\Educhronos\*' } | ForEach-Object { 'proceso=' + $_.ProcessId + '|' + $_.Name + '|' + $_.SessionId }"""


def preparar(args, ejecutor, anfitrion):
    problemas, _ = autoprueba()
    if problemas:
        raise AutopruebaNoPasa("; ".join(problemas))
    huellas = comprobar_anfitrion(args)

    material = Path(args.material)
    (material / "ordenes").mkdir(parents=True)
    zip_nombre, base_nombre = os.path.basename(args.zip), os.path.basename(args.base)
    estado = {"vm": args.vm, "instantanea": args.instantanea, "cuenta": args.cuenta, "clave": args.clave,
              "zip": args.zip, "base": args.base, "migracion_esperada": args.migracion_esperada,
              "g": "C:\\Users\\%s\\Documents\\educhronos-prueba" % args.cuenta, **huellas,
              "restaurada": False, "arrancada": False, "abierta_ok": False, "inhibidor_pid": None,
              "ordenes": 0, "informe": {}}
    p = Prueba(material, estado, ejecutor, anfitrion)
    p.guardar()

    # (c) La VM no ha cambiado desde la instantánea: solo lecturas.
    vm = p.leer_vm("showvminfo-antes")
    snap = p.vbox("snapshot-showvminfo", ["snapshot", args.vm, "showvminfo", args.instantanea], 120)
    lista = p.vbox("snapshot-list-antes", ["snapshot", args.vm, "list", "--details"], 120)
    estado["since"] = leer_since(p.texto(snap, invitado=False)) if snap.rc == 0 else None
    estado["instantaneas_antes"] = p.texto(lista, invitado=False) if lista.rc == 0 else None
    v = veredicto_vm_sin_cambios(vm, args.instantanea, estado["since"])
    if snap.rc != 0 or lista.rc != 0:
        v = ("FALLO", v[1] + "; lecturas con rc %d y %d" % (snap.rc, lista.rc))
    p.anotar("vm-sin-cambios", v)
    if v[0] != "OK":
        raise Rechazo("la VM ha cambiado desde la instantánea o no se pudo leer: no se restaura nada")
    compartidas = [x for k, x in vm.items() if k.startswith("SharedFolderPath")]
    donde = clave_fuera(args.clave, RAIZ_REPO, compartidas, material)
    if donde:
        p.anotar("clave-fuera", ("FALLO", donde))
        raise Rechazo("el fichero de contraseñas cuelga de " + donde)
    p.anotar("clave-fuera", ("OK", "fuera del repo, de %d carpetas compartidas y del material" % len(compartidas)))

    # (d) Inhibidor, restaurar, arrancar y esperar al invitado.
    try:
        estado["inhibidor_pid"] = anfitrion.lanzar_inhibidor(ORDEN_INHIBIDOR)
        p.anotar("inhibidor", ("DATO", "PID %d tomado" % estado["inhibidor_pid"]))
    except OSError as e:
        p.anotar("inhibidor", ("DATO", "no se pudo lanzar: %s" % e))
    r = p.vbox("restore", ["snapshot", args.vm, "restore", args.instantanea], 1800)
    if r.rc != 0:
        p.fallar("arranque", ("FALLO", "restore rc %d" % r.rc))
    estado["restaurada"] = True
    p.guardar()
    r = p.vbox("startvm", ["startvm", args.vm, "--type=gui"], 300)
    if r.rc != 0:
        p.fallar("arranque", ("FALLO", "startvm rc %d" % r.rc))
    estado["arrancada"] = True
    p.anotar("arranque", ("OK", "restore y startvm con rc 0"))
    for intento in range(1, INTENTOS_INVITADO + 1):
        anfitrion.dormir(ESPERA_INVITADO)
        r = p.run("invitado-%02d" % intento, WHOAMI, [], MS_WHOAMI)
        if traducir_rc(r.rc)[0] == 0:
            p.anotar("invitado", ("OK", "responde en el intento %d" % intento))
            break
    else:
        p.fallar("invitado", ("FALLO", "no responde en %d intentos" % INTENTOS_INVITADO))

    # (e) Cuenta.
    r = p.run("grupos", WHOAMI, ["/groups"], MS_WHOAMI)
    codigo, texto = traducir_rc(r.rc)
    v = veredicto_cuenta(p.texto(r)) if codigo == 0 else ("FALLO", "whoami /groups: " + texto)
    if v[0] != "OK":
        p.fallar("cuenta", v)
    p.anotar("cuenta", v)

    # (f) Transporte y (g) sumas en el invitado.
    _, d = p.ps("carpeta", PS_CARPETA, g=estado["g"])
    if d is None or d.get("carpeta") != ["True"]:
        p.fallar("transporte", ("FALLO", "no se pudo crear " + estado["g"]))
    rcs = [p.copyto("copia-" + n, f).rc for n, f in (("zip", args.zip), ("sha256", args.zip + ".sha256"),
                                                      ("base", args.base))]
    _, d = p.ps("sumas", PS_SUMAS, g=estado["g"], zip=zip_nombre, sha=zip_nombre + ".sha256", base=base_nombre)
    ficheros = d.get("fichero", []) if d else []
    ok = rcs == [0, 0, 0] and len(ficheros) == 3 and all(f.endswith("|True") for f in ficheros)
    v = ("OK" if ok else "FALLO", "copyto rc %s; en %s: %s" % (rcs, estado["g"], ficheros))
    if v[0] != "OK":
        p.fallar("transporte", v)
    p.anotar("transporte", v)
    v_zip = veredicto_suma(estado["sha256_zip"], (d.get("sha256-zip") or [None])[0])
    v_base = veredicto_suma(estado["md5_base"], (d.get("md5-base") or [None])[0])
    p.anotar("suma-zip", v_zip)
    p.anotar("suma-base", v_base)
    if "FALLO" in (v_zip[0], v_base[0]):
        raise FalloComprobacion("sumas")

    # (h) Extraer, localizar Instalar.cmd, comprobar los datos previos y colocar la base.
    r, d = p.ps("extraer", PS_EXTRAER, ms=MS_LARGO, g=estado["g"], zip=zip_nombre)
    instalar = d.get("instalar", []) if d else []
    if len(instalar) != 1:
        p.fallar("extraer", ("FALLO", "Instalar.cmd encontrados: %s (rc %d)" % (instalar, r.rc)))
    estado["ed"] = ntpath.dirname(instalar[0])
    _, d = p.ps("datos-previos", PS_DATOS_PREVIOS)
    if d is None:
        p.fallar("datos-previos", ("FALLO", "medida no válida"))
    limpia = d.get("datos-existe") == ["False"] or (d.get("datos-db") == ["0"] and d.get("datos-curso-abierto") == ["False"])
    v = ("OK" if limpia else "FALLO", "existe %s, .db %s, curso-abierto %s" % (
        d.get("datos-existe"), d.get("datos-db"), d.get("datos-curso-abierto")))
    if v[0] != "OK":
        p.fallar("datos-previos", v)
    p.anotar("datos-previos", v)
    _, d = p.ps("colocar", PS_COLOCAR, ed=estado["ed"], g=estado["g"], base=base_nombre)
    colocada = d.get("colocada", []) if d else []
    if colocada != ["%s|%d" % (base_nombre, estado["tamano_base"])]:
        p.fallar("extraer", ("FALLO", "base en base-partida: %s" % colocada))
    p.anotar("extraer", ("OK", "%.1f s; ED=%s; base colocada" % (r.duracion, estado["ed"])))

    # (i) Instalar, con «< nul» para que el pause final no espere una tecla.
    r = p.run("instalar", CMD, ["/c", estado["ed"] + "\\Instalar.cmd < nul"], MS_LARGO)
    v_rc, v_lineas = veredicto_instalar_rc(r.rc), veredicto_lineas(p.texto(r))
    p.anotar("instalar-rc", v_rc)
    p.anotar("instalar-lineas", v_lineas)
    if "FALLO" in (v_rc[0], v_lineas[0]):
        raise FalloComprobacion("instalar")

    # (j) Medida tras instalar.
    _, d = p.ps("medida", PS_MEDIDA)
    if d is None:
        p.fallar("md5-instalada", ("FALLO", "medida no válida"))
    datos = [x.rsplit("|", 1) for x in d.get("dato", [])]
    veredictos = {
        "md5-instalada": veredicto_md5_instalada(estado["md5_base"], (d.get("md5") or [None])[0]),
        "carpeta-datos": veredicto_carpeta_datos(datos, estado["tamano_base"]),
        "acceso-directo": ("OK" if d.get("lnk") == ["True"] else "FALLO", "Escritorio\\Educhronos.lnk " + str(d.get("lnk"))),
        "exe": ("OK" if d.get("exe") == ["True"] else "FALLO", "Programs\\Educhronos\\Educhronos.exe " + str(d.get("exe"))),
    }
    for clave, v in veredictos.items():
        p.anotar(clave, v)
    if any(v[0] == "FALLO" for v in veredictos.values()):
        raise FalloComprobacion("medida")

    print()
    print(H1.format(cuenta=args.cuenta))
    print("Después: prueba_vm.py sesion --material %s" % material)
    return 0


def exigir_restaurada(p):
    if not p.estado.get("restaurada"):
        raise Rechazo("preparar no llegó a restaurar la instantánea: no hay prueba en curso")


def sesion(args, ejecutor, anfitrion):
    p = Prueba.cargar(args.material, ejecutor, anfitrion)
    exigir_restaurada(p)
    r = p.run("quser", QUSER, [], MS_WHOAMI)
    codigo, texto = traducir_rc(r.rc)
    v = veredicto_sesion(p.texto(r), p.estado["cuenta"]) if codigo == 0 else ("FALLO", "quser: " + texto)
    p.anotar("sesion-cuenta", v)
    if v[0] != "OK":
        print("La sesión no es de %s: cierra esa sesión, entra con %s y repite sesion; o ejecuta cerrar."
              % (p.estado["cuenta"], p.estado["cuenta"]))
        return 3
    print()
    print(H2)
    print("Después: prueba_vm.py abierta --material %s" % p.material)
    return 0


def abierta(args, ejecutor, anfitrion):
    p = Prueba.cargar(args.material, ejecutor, anfitrion)
    exigir_restaurada(p)
    _, d = p.ps("log", PS_LOG)
    if d is None:
        v = ("FALLO", "medida no válida")
    else:
        v = veredicto_migracion(d.get("migracion", []), p.estado.get("migracion_esperada"))
    p.anotar("migracion", v)
    procesos = p.procesos("procesos-abierta")
    v_p = ("FALLO", "medida no válida") if procesos is None else veredicto_procesos_abierta(procesos)
    p.anotar("procesos-abierta", v_p)
    p.estado["abierta_ok"] = v_p[0] == "OK"
    p.guardar()
    if "FALLO" in (v[0], v_p[0]):
        print("Ejecuta cerrar.")
        return 3
    print()
    print(H3)
    print("Después: prueba_vm.py cerrar --material %s" % p.material)
    return 0


def soltar_inhibidor(p):
    pid = p.estado.get("inhibidor_pid")
    if pid is None or p.estado.get("inhibidor_soltado"):
        return
    try:
        p.anfitrion.terminar(pid)
    except ProcessLookupError:
        pass
    p.anfitrion.dormir(1)
    sigue = INHIBIDOR_QUIEN in p.anfitrion.listar_inhibidores() or p.anfitrion.vivo(pid)
    p.estado["inhibidor_soltado"] = not sigue
    p.anotar("inhibidor", ("FALLO" if sigue else "OK",
                           "PID %d %s" % (pid, "sigue vivo" if sigue else "terminado; no está en systemd-inhibit --list")))


def escribir_manifiesto(p):
    lineas = []
    for f in sorted(x for x in p.material.rglob("*") if x.is_file() and x.name != "MANIFIESTO.sha256"):
        lineas.append("%s  %s\n" % (huella(f, "sha256"), f.relative_to(p.material).as_posix()))
    (p.material / "MANIFIESTO.sha256").write_text("".join(lineas), encoding="utf-8")
    return p.anfitrion.comprobar_manifiesto(p.material)


def cerrar(args, ejecutor, anfitrion):
    p = Prueba.cargar(args.material, ejecutor, anfitrion)
    e = p.estado
    if not e.get("restaurada"):
        soltar_inhibidor(p)
        print("preparar no llegó a restaurar la instantánea: cerrar no toca la VM.")
        return 2
    try:
        vm = p.leer_vm("showvminfo-cerrar")
        if vm.get("VMState") != "poweroff":
            if e.get("abierta_ok"):
                for intento in range(1, INTENTOS_PROCESOS + 1):
                    procesos = p.procesos("procesos-cerrar-%02d" % intento)
                    v = ("FALLO", "medida no válida") if procesos is None else veredicto_proceso_terminado(procesos)
                    if v[0] == "OK":
                        break
                    anfitrion.dormir(ESPERA_PROCESOS)
                p.anotar("proceso-terminado", (v[0], "%s; intento %d de %d" % (v[1], intento, INTENTOS_PROCESOS)))
            else:
                p.anotar("proceso-terminado", ("DATO", "no medido: abierta no terminó con OK"))
            p.vbox("poweroff", ["controlvm", e["vm"], "poweroff"], 300)
            for intento in range(1, INTENTOS_APAGADO + 1):
                vm = p.leer_vm("apagado-%02d" % intento)
                if vm.get("VMState") == "poweroff":
                    break
                anfitrion.dormir(ESPERA_APAGADO)
            else:
                p.fallar("vm-restaurada", ("FALLO", "la VM no llega a poweroff: %s" % vm.get("VMState")))
        elif "proceso-terminado" not in e["informe"]:
            p.anotar("proceso-terminado", ("DATO", "no medido: la VM ya estaba apagada"))
        if e.get("arrancada"):
            p.anotar("regla-detecta-cambio", veredicto_regla_detecta_cambio(vm.get("VMStateChangeTime"), e["since"]))
        else:
            p.anotar("regla-detecta-cambio", ("DATO", "no medido: la VM no llegó a arrancar"))
        r = p.vbox("restore", ["snapshot", e["vm"], "restore", e["instantanea"]], 1800)
        if r.rc != 0:
            p.fallar("vm-restaurada", ("FALLO", "restore rc %d" % r.rc))
        for intento in range(1, INTENTOS_RESTAURADA + 1):
            v = veredicto_vm_restaurada(p.leer_vm("showvminfo-despues-%02d" % intento), e["instantanea"], e["since"])
            if v[0] == "OK":
                break
            anfitrion.dormir(ESPERA_RESTAURADA)
        p.anotar("vm-restaurada", (v[0], "%s; intento %d de %d" % (v[1], intento, INTENTOS_RESTAURADA)))
        lista = p.vbox("snapshot-list-despues", ["snapshot", e["vm"], "list", "--details"], 120)
        igual = lista.rc == 0 and p.texto(lista, invitado=False) == e["instantaneas_antes"]
        p.anotar("instantaneas", ("OK" if igual else "FALLO", "snapshot list --details %s al de preparar (rc %d)"
                                  % ("idéntico" if igual else "distinto", lista.rc)))
    except FalloComprobacion:
        pass
    finally:
        soltar_inhibidor(p)
    e["cerrada"] = True
    p.guardar()
    rc_manifiesto = escribir_manifiesto(p)
    print("MANIFIESTO.sha256: sha256sum -c rc %d" % rc_manifiesto)
    hay_fallo = any(v[0] == "FALLO" for v in e["informe"].values())
    print("Informe final: %s" % (p.material / "informe.txt"))
    return 3 if hay_fallo or rc_manifiesto != 0 else 0


SUBCOMANDOS = {"preparar": preparar, "sesion": sesion, "abierta": abierta, "cerrar": cerrar}


def main(argv=None, ejecutor=None, anfitrion=None):
    analizador = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    analizador.add_argument("--solo-autoprueba", action="store_true")
    sub = analizador.add_subparsers(dest="subcomando")
    pr = sub.add_parser("preparar")
    pr.add_argument("--vm", default=VM_POR_DEFECTO)
    pr.add_argument("--instantanea", default=INSTANTANEA_POR_DEFECTO)
    pr.add_argument("--cuenta", default=CUENTA_POR_DEFECTO)
    pr.add_argument("--clave", required=True)
    pr.add_argument("--zip", required=True)
    pr.add_argument("--base", required=True)
    pr.add_argument("--md5-base", required=True)
    pr.add_argument("--migracion-esperada", default=None)
    pr.add_argument("--material", required=True)
    for nombre in ("sesion", "abierta", "cerrar"):
        sub.add_parser(nombre).add_argument("--material", required=True)
    args = analizador.parse_args(argv)

    if args.solo_autoprueba:
        problemas, inyectados = autoprueba()
        for problema in problemas:
            print("FALLO DE AUTOPRUEBA:", problema)
        if problemas:
            return 4
        print("AUTOPRUEBA: %d defectos inyectados, los %d dan FALLO." % (inyectados, inyectados))
        return 0
    if args.subcomando is None:
        analizador.error("falta el subcomando")
    if not os.path.isabs(args.material):
        print("RECHAZADO: --material tiene que ser una ruta absoluta:", args.material)
        return 2
    args.material = os.path.normpath(args.material)
    for nombre in ("clave", "zip", "base"):
        if getattr(args, nombre, None) is not None:
            setattr(args, nombre, os.path.abspath(getattr(args, nombre)))

    ejecutor = ejecutor or EjecutorReal()
    anfitrion = anfitrion or AnfitrionReal()
    try:
        return SUBCOMANDOS[args.subcomando](args, ejecutor, anfitrion)
    except Rechazo as ex:
        print("RECHAZADO:", ex)
        return 2
    except AutopruebaNoPasa as ex:
        print("FALLO DE AUTOPRUEBA:", ex)
        return 4
    except FalloComprobacion as ex:
        print("FALLO en %s: la VM sigue como está. Ejecuta cerrar." % ex)
        return 3
    except Exception as ex:  # noqa: BLE001
        print("FALLO inesperado:", repr(ex))
        print("Si la VM está encendida, ejecuta cerrar.")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
