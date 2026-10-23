#!/usr/bin/env python3
"""Arranca y para el backend de desarrollo contra una base SQLite, guardando el grupo de proceso.

Nace de D-arranque-no-literal (plan_trabajo_horarios.md): la orden de arranque contra otra
base se reconstruyó a mano sesión tras sesión, y en S216 el PID guardado fue el del shell
envoltorio y no el del backend. Aquí el literal de tres pasos vive en UNA función
(`ordenes`), y lo que se guarda es el grupo del proceso lanzado, comprobado antes de
escribirlo.

Uso:
  scripts/arrancar-backend.py arrancar --base RUTA --registro DIR [--espera SEG] [--con-frontend]
  scripts/arrancar-backend.py arrancar --en-seco --base RUTA [--con-frontend]
  scripts/arrancar-backend.py parar --registro DIR
  scripts/arrancar-backend.py --solo-autoprueba

`arrancar` ejecuta en la raíz del repo, en primer plano, `mvn -pl solver install` y
`mvn -pl app package` (salidas en DIR/instalar-solver.log y DIR/package.log), y lanza
`mvn -pl app spring-boot:run` en una sesión nueva, con la salida en DIR/backend.log. El
grupo queda en DIR/backend.pgid, que `parar` lee y no borra: es evidencia. `--en-seco`
aplica las guardas de argumentos, imprime las tres órdenes y no lanza nada (ni la
autoprueba). La autoprueba corre antes de un `arrancar` real, antes de `parar` y con
`--solo-autoprueba`, como en verificar-cierre.py.

Con `--con-frontend`, cuando el backend ya responde 200, lanza `npx ng serve` en
app/frontend en otra sesión nueva, con la salida en DIR/frontend.log y el grupo en
DIR/frontend.pgid, y espera, con otro plazo de `--espera`, a que GET
http://localhost:4200/ y http://localhost:4200/api/version respondan 200. Va después y no
a la vez: un `ng serve` lanzado durante el `npm ci` de `mvn package` falló (S224).
`ng serve` escucha en «localhost», que puede ser ::1: su puerto se mira en todas las
direcciones de ese nombre. En seco imprime también la cuarta orden. `parar` para primero
el grupo de DIR/frontend.pgid, si existe, y después el del backend. Un `arrancar` real
exige una carpeta de registro sin backend.pgid ni frontend.pgid: un pgid viejo puede
nombrar, cuando `parar` lo lea, un grupo que ya no es nuestro.

Códigos de salida:
  0  arrancado (GET /api/version responde 200; con `--con-frontend`, también las dos de
     localhost:4200), parado, o en seco impreso.
  1  la autoprueba falla; se nombra la capacidad, de (a) a (i).
  2  argumentos: una ruta no absoluta (norma 6 de M-guion), la base no existe, falta
     DIR/backend.pgid en `parar`, DIR ya tiene backend.pgid o frontend.pgid en un
     `arrancar` real («usa una carpeta de registro nueva»), o hay una opción desconocida.
  3  127.0.0.1:8080, o con `--con-frontend` localhost:4200, ya acepta conexiones antes de
     arrancar; se dice cuál.
  4  el grupo del proceso lanzado no es su PID: se mata y no se escribe su pgid.
  5  el backend o `ng serve` muere antes de responder; se imprimen las 20 últimas líneas
     de su log.
  6  vence la espera: el proceso sigue vivo y su grupo queda escrito. En `parar`, alguno
     de los dos grupos o su puerto siguen ocupados tras SIGTERM y SIGKILL.
  En 4, 5 y 6 del frontend el backend sigue arriba: el mensaje da su pgid y la orden de
  `parar`. Si falla `install` o `package`, sale con el código de mvn.

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
HOST_FRONT = "localhost"
PUERTO_FRONT = 4200
URLS_FRONT = ("http://%s:%d/" % (HOST_FRONT, PUERTO_FRONT),
              "http://%s:%d/api/version" % (HOST_FRONT, PUERTO_FRONT))
FICHERO_PGID_FRONT = "frontend.pgid"
LOG_FRONT = "frontend.log"

# El literal vigente de D-arranque-no-literal, con la base como hueco. La autoprueba (c)
# compara contra él lo que compone `ordenes`.
LITERAL = (
    'mvn -pl solver install -DskipTests',
    'mvn -pl app package -DskipTests',
    'mvn -pl app spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:{base}"',
)
# La orden del frontend, que corre en app/frontend (D-ng-serve-sin-literal). La autoprueba
# (f) compara contra él lo que compone `orden_frontend`.
LITERAL_FRONT = 'npx ng serve'


def ordenes(base):
    """Las tres órdenes del literal, como listas de argumentos, con la base sustituida."""
    return [
        ["mvn", "-pl", "solver", "install", "-DskipTests"],
        ["mvn", "-pl", "app", "package", "-DskipTests"],
        ["mvn", "-pl", "app", "spring-boot:run",
         "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:" + base],
    ]


def orden_frontend():
    """La orden de `ng serve`, como lista de argumentos, y su directorio."""
    return ["npx", "ng", "serve"], os.path.join(RAIZ, "app", "frontend")


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


def error_registro(registro):
    """Capacidad (i): None si DIR no tiene el pgid de un arranque anterior; si no, el motivo."""
    viejos = [n for n in (FICHERO_PGID, FICHERO_PGID_FRONT) if os.path.exists(os.path.join(registro, n))]
    if viejos:
        return "%s ya tiene %s: usa una carpeta de registro nueva" % (registro, " y ".join(viejos))
    return None


def puerto_ocupado(host="127.0.0.1", puerto=PUERTO):
    """Capacidad (g): True si alguna dirección de `host` acepta conexiones en el puerto."""
    try:
        with socket.create_connection((host, puerto), timeout=1):
            return True
    except OSError:
        return False


def frontend_ocupado():
    return puerto_ocupado(HOST_FRONT, PUERTO_FRONT)


def lanzar(orden, log, cwd=RAIZ):
    """Lanza la orden en una sesión nueva, en `cwd` (la raíz del repo), con la salida al log."""
    with open(log, "ab") as salida:
        return subprocess.Popen(orden, cwd=cwd, stdin=subprocess.DEVNULL, stdout=salida,
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


def parar_grupo(pgid, espera, ocupado=puerto_ocupado, hijo=None):
    """Capacidad (e): SIGTERM al grupo y espera a que quede vacío (y su puerto libre, si
    `ocupado` no es None). Si no ocurre, SIGKILL al grupo. Devuelve (parado, hubo_sigkill)."""
    if pgid <= 1 or pgid == os.getpgrp():
        raise SystemExit("ABORTA: grupo %d no parable desde aquí" % pgid)

    def parado():
        if hijo is not None:
            hijo.poll()
        return not vivos_del_grupo(pgid) and not (ocupado is not None and ocupado())

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


def parar_registro(registro, espera, con_puertos=True):
    """Capacidad (h): para el grupo de frontend.pgid, si existe, y después el de backend.pgid,
    cada uno con su puerto. Devuelve [(fichero, pgid, parado, hubo_sigkill)]."""
    res = []
    for nombre, ocupado in ((FICHERO_PGID_FRONT, frontend_ocupado), (FICHERO_PGID, puerto_ocupado)):
        fichero = os.path.join(registro, nombre)
        if not os.path.isfile(fichero):
            continue
        with open(fichero) as f:
            pgid = int(f.read().strip())
        parado, sigkill = parar_grupo(pgid, espera, ocupado if con_puertos else None)
        res.append((nombre, pgid, parado, sigkill))
    return res


def descartar(proc):
    """Rama de código 4: mata el grupo real del proceso, si no es el nuestro, y el proceso."""
    try:
        real = os.getpgid(proc.pid)
    except ProcessLookupError:
        real = None
    if real is not None and real != os.getpgrp():
        senal_grupo(real, signal.SIGKILL)
    proc.kill()
    proc.wait()


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
    orden, cwd = orden_frontend()
    if texto(orden) != LITERAL_FRONT or not cwd.endswith(os.path.join(os.sep, "app", "frontend")):
        fallos.append("(f) la orden del frontend no es la de su literal o no corre en app/frontend")

    grupos = []
    with tempfile.TemporaryDirectory() as d:
        proc = lanzar(["sh", "-c", "sleep 30 & sleep 30"], os.path.join(d, "d.log"))
        time.sleep(0.3)
        real = os.getpgid(proc.pid)
        grupos.append(real)
        pgid = capturar(proc)
        if pgid != proc.pid or pgid == os.getpgrp():
            fallos.append("(d) el grupo guardado no es el del proceso lanzado")
        descartar(proc)
        time.sleep(0.2)
        if real != os.getpgrp() and vivos_del_grupo(real):
            fallos.append("(d) descartar() deja vivo el grupo del proceso lanzado")
        _limpiar(proc, real)

        # (e) prueba solo la parada: su grupo se crea aquí y no con `lanzar`, que es de (d).
        proc = subprocess.Popen(["sh", "-c", "sleep 60 & sleep 60"], stdin=subprocess.DEVNULL,
                                start_new_session=True)
        time.sleep(0.3)
        real = os.getpgid(proc.pid)
        grupos.append(real)
        if real != os.getpgrp():
            parado, _ = parar_grupo(real, espera=3, ocupado=None, hijo=proc)
            if not parado or vivos_del_grupo(real):
                fallos.append("(e) tras parar quedan procesos en el grupo")
        else:
            fallos.append("(e) el proceso no tiene grupo propio")
        _limpiar(proc, real)

        # (h) recorre un registro con los dos ficheros, sin puertos: grupos creados aquí, como en (e).
        procs = []
        for nombre in (FICHERO_PGID, FICHERO_PGID_FRONT):
            proc = subprocess.Popen(["sh", "-c", "sleep 60 & sleep 60"], stdin=subprocess.DEVNULL,
                                    start_new_session=True)
            procs.append(proc)
            grupos.append(proc.pid)
            with open(os.path.join(d, nombre), "w") as f:
                f.write("%d\n" % proc.pid)
        time.sleep(0.3)
        res = parar_registro(d, espera=3, con_puertos=False)
        if len(res) != 2 or not all(p for _, _, p, _ in res) or any(vivos_del_grupo(p.pid) for p in procs):
            fallos.append("(h) parar no deja vacíos los dos grupos del registro: %s" % res)
        for proc in procs:
            _limpiar(proc, proc.pid)

    # (g) oyentes en puertos efímeros, no en el 4200.
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s4:
        s4.bind(("127.0.0.1", 0))
        s4.listen(1)
        if not puerto_ocupado(HOST_FRONT, s4.getsockname()[1]):
            fallos.append("(g) no ve un oyente en 127.0.0.1")
    try:
        s6 = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
        s6.bind(("::1", 0))
    except OSError:
        s6 = None
        print("(g) no se puede escuchar en ::1: se prueba solo IPv4.")
    if s6 is not None:
        with s6:
            s6.listen(1)
            if not puerto_ocupado(HOST_FRONT, s6.getsockname()[1]):
                fallos.append("(g) no ve un oyente en ::1")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        libre = s.getsockname()[1]
    if puerto_ocupado(HOST_FRONT, libre):
        fallos.append("(g) da por ocupado un puerto libre")

    with tempfile.TemporaryDirectory() as r:
        if error_registro(r) is not None:
            fallos.append("(i) rechaza una carpeta de registro vacía")
        with open(os.path.join(r, FICHERO_PGID_FRONT), "w") as f:
            f.write("1\n")
        if error_registro(r) is None:
            fallos.append("(i) acepta una carpeta de registro con un pgid")

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
    print("AUTOPRUEBA: capacidades (a) a (i) comprobadas, ninguna falla.")
    return 0


def responde(url=URL_VERSION):
    abridor = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    try:
        with abridor.open(url, timeout=3) as r:
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
        if a.con_frontend:
            print("%s  (en app/frontend)" % texto(orden_frontend()[0]))
        return 0
    if error_registro(a.registro):
        print(error_registro(a.registro))
        return 2

    if correr_autoprueba():
        return 1
    if puerto_ocupado():
        print("El puerto %d ya acepta conexiones: no se arranca nada." % PUERTO)
        return 3
    if a.con_frontend and frontend_ocupado():
        print("%s:%d ya acepta conexiones: no se arranca nada." % (HOST_FRONT, PUERTO_FRONT))
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
        descartar(proc)
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
            return arrancar_frontend(a, pgid) if a.con_frontend else 0
        time.sleep(2)
    print("Vence la espera de %d s sin respuesta. El proceso sigue vivo, pgid %d en %s."
          % (a.espera, pgid, os.path.join(a.registro, FICHERO_PGID)))
    return 6


def arrancar_frontend(a, pgid_backend):
    """Con --con-frontend, cuando el backend ya responde: no a la vez que su `npm ci` (S224)."""
    sigue = ("El backend sigue arriba, pgid %d; para pararlo todo: python3 -B %s parar --registro %s"
             % (pgid_backend, os.path.abspath(__file__), a.registro))
    orden, cwd = orden_frontend()
    log = os.path.join(a.registro, LOG_FRONT)
    print("$ %s  (en app/frontend)  > %s" % (texto(orden), log))
    proc = lanzar(orden, log, cwd)
    pgid = capturar(proc)
    if pgid is None:
        descartar(proc)
        print("El grupo del proceso %d no es su PID: se mata y no se escribe. %s" % (proc.pid, sigue))
        return 4
    with open(os.path.join(a.registro, FICHERO_PGID_FRONT), "w") as f:
        f.write("%d\n" % pgid)

    fin = time.monotonic() + a.espera
    while time.monotonic() < fin:
        if proc.poll() is not None:
            print("ng serve murió con código %d antes de responder. %s Últimas líneas:" % (proc.returncode, sigue))
            for linea in cola(log):
                print("  " + linea)
            return 5
        if all(responde(u) for u in URLS_FRONT):
            print("Frontend arriba: GET %s → 200. pgid %d." % (" y ".join(URLS_FRONT), pgid))
            return 0
        time.sleep(2)
    print("Vence la espera de %d s sin respuesta de ng serve. Sigue vivo, pgid %d en %s. %s"
          % (a.espera, pgid, os.path.join(a.registro, FICHERO_PGID_FRONT), sigue))
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
    puertos = {FICHERO_PGID: "127.0.0.1:%d" % PUERTO, FICHERO_PGID_FRONT: "%s:%d" % (HOST_FRONT, PUERTO_FRONT)}
    res = parar_registro(a.registro, espera=60)
    grupos = "; ".join("%s %d%s" % (n, pgid, " (con SIGKILL)" if k else "") for n, pgid, _, k in res)
    vivos = [(n, pgid) for n, pgid, p, _ in res if not p]
    if vivos:
        print("Siguen ocupados tras SIGKILL: %s. Grupos: %s"
              % ("; ".join("%s %d o %s: %s" % (n, g, puertos[n], vivos_del_grupo(g)) for n, g in vivos), grupos))
        return 6
    print("Parados los grupos: %s. No aceptan conexiones: %s."
          % (grupos, " ni ".join(puertos[n] for n, _, _, _ in res)))
    return 0


def parsear(argv):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = ap.add_subparsers(dest="accion", required=True)
    pa = sub.add_parser("arrancar")
    pa.add_argument("--base")
    pa.add_argument("--registro")
    pa.add_argument("--espera", type=int, default=180)
    pa.add_argument("--en-seco", action="store_true")
    pa.add_argument("--con-frontend", action="store_true")
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
