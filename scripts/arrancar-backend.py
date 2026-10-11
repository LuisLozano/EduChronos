#!/usr/bin/env python3
"""Arranca y para el backend de desarrollo contra una base SQLite, guardando el grupo de proceso.

Nace de D-arranque-no-literal (plan_trabajo_horarios.md): la orden de arranque contra otra
base se reconstruyó a mano sesión tras sesión, y en S216 el PID guardado fue el del shell
envoltorio y no el del backend. Aquí el literal de tres pasos vive en UNA función
(`ordenes`), y lo que se guarda es el grupo del proceso lanzado, comprobado antes de
escribirlo.

Uso:
  scripts/arrancar-backend.py arrancar --base RUTA --registro DIR [--espera SEG]
  scripts/arrancar-backend.py arrancar --en-seco --base RUTA
  scripts/arrancar-backend.py parar --registro DIR
  scripts/arrancar-backend.py --solo-autoprueba

`arrancar` ejecuta en la raíz del repo, en primer plano, `mvn -pl solver install` y
`mvn -pl app package` (salidas en DIR/instalar-solver.log y DIR/package.log), y lanza
`mvn -pl app spring-boot:run` en una sesión nueva, con la salida en DIR/backend.log. El
grupo queda en DIR/backend.pgid, que `parar` lee y no borra: es evidencia. `--en-seco`
aplica las guardas de argumentos, imprime las tres órdenes y no lanza nada (ni la
autoprueba). La autoprueba corre antes de un `arrancar` real, antes de `parar` y con
`--solo-autoprueba`, como en verificar-cierre.py.

Códigos de salida:
  0  arrancado (GET /api/version responde 200), parado, o en seco impreso.
  1  la autoprueba falla; se nombra la capacidad, de (a) a (e).
  2  argumentos: una ruta no absoluta (norma 6 de M-guion), la base no existe, falta
     DIR/backend.pgid o hay una opción desconocida.
  3  127.0.0.1:8080 ya acepta conexiones antes de arrancar.
  4  el grupo del proceso lanzado no es su PID: se mata y no se escribe nada.
  5  el backend muere antes de responder; se imprimen las 20 últimas líneas del log.
  6  vence la espera: el proceso sigue vivo y su grupo queda escrito. En `parar`, el
     grupo o el puerto siguen ocupados tras SIGTERM y SIGKILL.
  Si falla `install` o `package`, sale con el código de mvn.

Puerto 8080 fijo: es el de S216 (s216/t3/ORDENES.txt:6), y app/src/main/resources no
declara server.port. El literal pasa un único argumento en spring-boot.run.arguments; si
otra forma admite dos no está medido (plan, D-arranque-no-literal), y el script no lo
intenta.
"""
import argparse
import os
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PUERTO = 8080
URL_VERSION = "http://127.0.0.1:%d/api/version" % PUERTO
FICHERO_PGID = "backend.pgid"
LOGS = ("instalar-solver.log", "package.log", "backend.log")

# El literal vigente de D-arranque-no-literal, con la base como hueco. La autoprueba (c)
# compara contra él lo que compone `ordenes`.
LITERAL = (
    'mvn -pl solver install -DskipTests',
    'mvn -pl app package -DskipTests',
    'mvn -pl app spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:{base}"',
)


def ordenes(base):
    """Las tres órdenes del literal, como listas de argumentos, con la base sustituida."""
    return [
        ["mvn", "-pl", "solver", "install", "-DskipTests"],
        ["mvn", "-pl", "app", "package", "-DskipTests"],
        ["mvn", "-pl", "app", "spring-boot:run",
         "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:" + base],
    ]


def texto(orden):
    """Una orden como se teclea: entre comillas el argumento que lleva `=`."""
    return " ".join('"%s"' % a if "=" in a else a for a in orden)


def error_ruta(ruta, nombre):
    """Capacidad (a): None si la ruta es absoluta; si no, el motivo."""
    if not os.path.isabs(ruta):
        return "%s no es absoluta: %s" % (nombre, ruta)
    return None


def error_base(base):
    """Capacidad (b): None si la base existe como fichero; si no, el motivo."""
    if not os.path.isfile(base):
        return "la base no existe: %s" % base
    return None


def puerto_ocupado():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(1)
        return s.connect_ex(("127.0.0.1", PUERTO)) == 0


def lanzar(orden, log):
    """Lanza la orden en una sesión nueva, en la raíz del repo, con la salida al log."""
    with open(log, "ab") as salida:
        return subprocess.Popen(orden, cwd=RAIZ, stdin=subprocess.DEVNULL, stdout=salida,
                                stderr=subprocess.STDOUT, start_new_session=True)


def capturar(proc):
    """Capacidad (d): el grupo del proceso, solo si es su propio PID; si no, None."""
    try:
        pgid = os.getpgid(proc.pid)
    except ProcessLookupError:
        return None
    return pgid if pgid == proc.pid else None


def vivos_del_grupo(pgid):
    """PIDs no zombis del grupo, leídos de /proc."""
    vivos = []
    for nombre in os.listdir("/proc"):
        if not nombre.isdigit():
            continue
        try:
            with open("/proc/%s/stat" % nombre) as f:
                campos = f.read().rsplit(")", 1)[1].split()
        except (OSError, IndexError):
            continue
        if int(campos[2]) == pgid and campos[0] not in "ZX":
            vivos.append(int(nombre))
    return vivos


def senal_grupo(pgid, senal):
    try:
        os.killpg(pgid, senal)
    except ProcessLookupError:
        pass


def parar_grupo(pgid, espera, con_puerto=True, hijo=None):
    """Capacidad (e): SIGTERM al grupo y espera a que quede vacío (y el puerto libre).
    Si no ocurre, SIGKILL al grupo. Devuelve (parado, hubo_sigkill)."""
    if pgid <= 1 or pgid == os.getpgrp():
        raise SystemExit("ABORTA: grupo %d no parable desde aquí" % pgid)

    def parado():
        if hijo is not None:
            hijo.poll()
        return not vivos_del_grupo(pgid) and not (con_puerto and puerto_ocupado())

    for senal, plazo in ((signal.SIGTERM, espera), (signal.SIGKILL, 10)):
        senal_grupo(pgid, senal)
        fin = time.monotonic() + plazo
        while time.monotonic() < fin:
            if parado():
                return True, senal == signal.SIGKILL
            time.sleep(0.2)
        if senal == signal.SIGTERM:
            print("El grupo %d sigue vivo tras %d s: SIGKILL al grupo." % (pgid, espera))
    return parado(), True


def _limpiar(proc, pgid):
    """Mata lo que haya lanzado la autoprueba sin pasar por las funciones que prueba."""
    if pgid != os.getpgrp():
        try:
            os.killpg(pgid, signal.SIGKILL)
        except ProcessLookupError:
            pass
    proc.kill()
    proc.wait()


def autoprueba():
    """Cada capacidad, por la misma función que usa el script. Devuelve las que fallan."""
    fallos = []
    if error_ruta("rel/base.db", "--base") is None or error_ruta("/abs/base.db", "--base"):
        fallos.append("(a) una ruta relativa no se rechaza")
    with tempfile.NamedTemporaryFile(suffix=".db") as existente:
        if error_base("/no/existe/s217.db") is None or error_base(existente.name):
            fallos.append("(b) una base inexistente no se rechaza")
    base = "/tmp/autoprueba/base.db"
    if tuple(texto(o) for o in ordenes(base)) != tuple(l.format(base=base) for l in LITERAL):
        fallos.append("(c) las órdenes no son las del literal")

    grupos = []
    with tempfile.TemporaryDirectory() as d:
        proc = lanzar(["sleep", "30"], os.path.join(d, "d.log"))
        real = os.getpgid(proc.pid)
        grupos.append(real)
        pgid = capturar(proc)
        if pgid != proc.pid or pgid == os.getpgrp():
            fallos.append("(d) el grupo guardado no es el del proceso lanzado")
        _limpiar(proc, real)

        # (e) prueba solo la parada: su grupo se crea aquí y no con `lanzar`, que es de (d).
        proc = subprocess.Popen(["sh", "-c", "sleep 60 & sleep 60"], stdin=subprocess.DEVNULL,
                                start_new_session=True)
        time.sleep(0.3)
        real = os.getpgid(proc.pid)
        grupos.append(real)
        if real != os.getpgrp():
            parado, _ = parar_grupo(real, espera=3, con_puerto=False, hijo=proc)
            if not parado or vivos_del_grupo(real):
                fallos.append("(e) tras parar quedan procesos en el grupo")
        else:
            fallos.append("(e) el proceso no tiene grupo propio")
        _limpiar(proc, real)

    time.sleep(0.2)
    for g in grupos:
        restos = vivos_del_grupo(g) if g != os.getpgrp() else []
        if restos:
            fallos.append("la autoprueba deja procesos vivos en el grupo %d: %s" % (g, restos))
    return fallos


def correr_autoprueba():
    fallos = autoprueba()
    if fallos:
        for f in fallos:
            print("  FALLO DE AUTOPRUEBA: %s" % f)
        print("ABORTA: el lanzador no pasa su propia prueba")
        return 1
    print("AUTOPRUEBA: capacidades (a) a (e) comprobadas, ninguna falla.")
    return 0


def responde():
    abridor = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with abridor.open(URL_VERSION, timeout=3) as r:
            return r.status == 200
    except OSError:
        return False


def cola(log, n=20):
    with open(log, encoding="utf-8", errors="replace") as f:
        return f.read().splitlines()[-n:]


def arrancar(a):
    for ruta, nombre in ((a.base, "--base"), (a.registro, "--registro")):
        if ruta is None and (nombre == "--base" or not a.en_seco):
            print("Falta %s" % nombre)
            return 2
        if ruta is not None and error_ruta(ruta, nombre):
            print(error_ruta(ruta, nombre))
            return 2
    if error_base(a.base):
        print(error_base(a.base))
        return 2
    if a.en_seco:
        for o in ordenes(a.base):
            print(texto(o))
        return 0

    if correr_autoprueba():
        return 1
    if puerto_ocupado():
        print("El puerto %d ya acepta conexiones: no se arranca nada." % PUERTO)
        return 3
    os.makedirs(a.registro, exist_ok=True)
    logs = [os.path.join(a.registro, n) for n in LOGS]

    for orden, log in zip(ordenes(a.base)[:2], logs):
        print("$ %s  > %s" % (texto(orden), log))
        with open(log, "ab") as salida:
            rc = subprocess.run(orden, cwd=RAIZ, stdin=subprocess.DEVNULL, stdout=salida,
                                stderr=subprocess.STDOUT).returncode
        if rc != 0:
            print("Falló con código %d; ver %s" % (rc, log))
            return rc

    orden = ordenes(a.base)[2]
    print("$ %s  > %s" % (texto(orden), logs[2]))
    proc = lanzar(orden, logs[2])
    pgid = capturar(proc)
    if pgid is None:
        _limpiar(proc)
        print("El grupo del proceso %d no es su PID: se mata y no se escribe." % proc.pid)
        return 4
    with open(os.path.join(a.registro, FICHERO_PGID), "w") as f:
        f.write("%d\n" % pgid)

    fin = time.monotonic() + a.espera
    while time.monotonic() < fin:
        if proc.poll() is not None:
            print("El backend murió con código %d antes de responder. Últimas líneas:" % proc.returncode)
            for linea in cola(logs[2]):
                print("  " + linea)
            return 5
        if responde():
            print("Backend arriba: GET %s → 200. pgid %d. Registro: %s" % (URL_VERSION, pgid, a.registro))
            return 0
        time.sleep(2)
    print("Vence la espera de %d s sin respuesta. El proceso sigue vivo, pgid %d en %s."
          % (a.espera, pgid, os.path.join(a.registro, FICHERO_PGID)))
    return 6


def parar(a):
    if error_ruta(a.registro, "--registro"):
        print(error_ruta(a.registro, "--registro"))
        return 2
    fichero = os.path.join(a.registro, FICHERO_PGID)
    if not os.path.isfile(fichero):
        print("No existe %s" % fichero)
        return 2
    if correr_autoprueba():
        return 1
    with open(fichero) as f:
        pgid = int(f.read().strip())
    parado, sigkill = parar_grupo(pgid, espera=60)
    if not parado:
        print("El grupo %d o el puerto %d siguen ocupados tras SIGKILL: %s"
              % (pgid, PUERTO, vivos_del_grupo(pgid)))
        return 6
    print("Parado el grupo %d%s; el puerto %d no acepta conexiones."
          % (pgid, " (con SIGKILL)" if sigkill else "", PUERTO))
    return 0


def parsear(argv):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = ap.add_subparsers(dest="accion", required=True)
    pa = sub.add_parser("arrancar")
    pa.add_argument("--base")
    pa.add_argument("--registro")
    pa.add_argument("--espera", type=int, default=180)
    pa.add_argument("--en-seco", action="store_true")
    pp = sub.add_parser("parar")
    pp.add_argument("--registro", required=True)
    return ap.parse_args(argv)


def main(argv):
    if "--solo-autoprueba" in argv:
        return correr_autoprueba()
    a = parsear(argv)
    return arrancar(a) if a.accion == "arrancar" else parar(a)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
