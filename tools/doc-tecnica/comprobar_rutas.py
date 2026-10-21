#!/usr/bin/env python3
"""Comprueba las rutas citadas en docs/tecnica (C-arquitectura-y-rutas, S245).

Condición 2 de O-doc-tecnica: toda ruta del repo se escribe entera, relativa a la raíz y entre
comillas invertidas, y existe en el commit que el documento dice describir. Aquí, ruta es TODO
código en línea entre comillas invertidas simples fuera de los bloques delimitados (``` o ~~~);
lo que no tiene forma de ruta es un fallo de forma. Cada documento lleva, entre sus diez
primeras líneas y sin comillas invertidas, una sola línea «Describe el commit <hash>.».

Fallos duros: rota, commit, tamaño (más de 20 480 bytes), líneas (cita ruta:N-M fuera del
fichero), forma y vacío (ningún documento). Una ruta que existe en el commit citado y no en HEAD
es AVISO, no fallo (DS de S245). Las rutas de docs/tecnica se comprueban en el árbol de trabajo,
bajo --raiz, y no en el commit citado ni en HEAD: la documentación técnica se cita a sí misma tal
como está (DT de S245).

Se somete a AUTOPRUEBA antes de dar ninguna cifra: en un repo git temporal inyecta un defecto
por documento y exige que cada uno salte en su categoría, y que los controles limpios no den
nada. Si alguno no se comporta, aborta con rc=3 y no revisa.

Uso:
  python3 -B tools/doc-tecnica/comprobar_rutas.py --raiz /ruta/absoluta/del/repo
Salida: 0 sin fallos duros; 1 con fallos duros; 2 argumentos inválidos; 3 la autoprueba no pasa.
Con GITHUB_ACTIONS=true, cada fallo y cada aviso sale además como anotación ::error / ::warning.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import tempfile

TOPE_BYTES = 20480
LINEAS_CABECERA = 10
CARPETA_DOCS = "docs/tecnica"
LINEA_COMMIT = re.compile(r"^Describe el commit ([0-9a-f]{7,40})\.$")
CANDIDATA_COMMIT = re.compile(r"^\s*Describe el commit\b")
VALLA = re.compile(r"^ {0,3}(`{3,}|~{3,})")
CITA = re.compile(r"^(?P<ruta>[^:]*)(?::(?P<n>\d+)(?:-(?P<m>\d+))?)?$")


class Resultado:
    def __init__(self):
        self.documentos = 0
        self.rutas = 0
        self.fallos = []  # (documento, línea, categoría, detalle)
        self.avisos = []  # (documento, línea, detalle)

    def fallo(self, documento, linea, categoria, detalle):
        self.fallos.append((documento, linea, categoria, detalle))

    def categorias(self):
        return {c for _, _, c, _ in self.fallos}


# ---------------------------------------------------------------------------------- git

def git(raiz, *args, entorno=None):
    return subprocess.run(["git", "-C", raiz, *args], capture_output=True, env=entorno)


def resolver_commit(raiz, corto):
    r = git(raiz, "rev-parse", "--verify", "--quiet", corto + "^{commit}")
    return r.stdout.decode().strip() if r.returncode == 0 else None


def tipo_objeto(raiz, commit, ruta, cache):
    clave = (commit, ruta)
    if clave not in cache:
        r = git(raiz, "cat-file", "-t", "%s:%s" % (commit, ruta))
        cache[clave] = r.stdout.decode().strip() if r.returncode == 0 else None
    return cache[clave]


def contar_lineas(datos):
    return datos.count(b"\n") + (1 if datos and not datos.endswith(b"\n") else 0)


def lineas_del_blob(raiz, commit, ruta):
    return contar_lineas(git(raiz, "cat-file", "blob", "%s:%s" % (commit, ruta)).stdout)


# -------------------------------------------------------------------------- árbol (DT)

def en_docs_tecnica(ruta):
    return ruta == CARPETA_DOCS or ruta.startswith(CARPETA_DOCS + "/")


def tipo_en_disco(raiz, ruta):
    camino = os.path.join(raiz, *ruta.split("/"))
    if os.path.isfile(camino):
        return "blob"
    if os.path.isdir(camino):
        return "tree"
    return None


def revisar_en_arbol(raiz, documento, numero, codigo, cita, r):
    """Una ruta de docs/tecnica, en disco bajo la raíz. Sin comprobación de HEAD: el árbol ya es lo actual."""
    ruta, directorio, n, fin = cita
    tipo = tipo_en_disco(raiz, ruta)
    if tipo is None:
        r.fallo(documento, numero, "rota", "%s: no existe en el árbol" % codigo)
        return
    if not tipo_esperado(tipo, directorio, n):
        r.fallo(documento, numero, "rota", "%s: es %s en el árbol" % (codigo, tipo))
        return
    if n is not None:
        with open(os.path.join(raiz, *ruta.split("/")), "rb") as f:
            total = contar_lineas(f.read())
        if fin > total:
            r.fallo(documento, numero, "líneas", "%s: el fichero tiene %d líneas en el árbol" % (codigo, total))


# ----------------------------------------------------------------------------- markdown

def fuera_de_bloques(lineas, documento, r):
    """(número, línea) de las líneas fuera de bloques delimitados; las vallas no salen."""
    valla = None
    apertura = 0
    for numero, linea in enumerate(lineas, 1):
        m = VALLA.match(linea)
        if valla is None:
            if m:
                valla, apertura = m.group(1), numero
                continue
            yield numero, linea
        elif m and m.group(1)[0] == valla[0] and len(m.group(1)) >= len(valla) \
                and linea.strip() == m.group(1):
            valla = None
    if valla is not None:
        r.fallo(documento, apertura, "forma", "bloque delimitado sin cerrar")


def buscar_cierre(linea, desde, largo):
    k = desde
    while True:
        k = linea.find("`", k)
        if k < 0:
            return -1
        f = k
        while f < len(linea) and linea[f] == "`":
            f += 1
        if f - k == largo:
            return k
        k = f


def codigos_en_linea(linea):
    """Códigos en línea de una línea, como (texto, problema); problema es None si es simple."""
    codigos = []
    i = 0
    while True:
        i = linea.find("`", i)
        if i < 0:
            return codigos
        j = i
        while j < len(linea) and linea[j] == "`":
            j += 1
        largo = j - i
        cierre = buscar_cierre(linea, j, largo)
        if cierre < 0:
            codigos.append((linea[i:], "comilla invertida sin cerrar"))
            return codigos
        texto = linea[j:cierre]
        codigos.append((texto, None if largo == 1 else "código con %d comillas invertidas" % largo))
        i = cierre + largo


def analizar_cita(texto):
    """(ruta sin barra final, es_directorio, n, m) o un str con el problema de forma."""
    if not texto or any(c.isspace() for c in texto):
        return "vacía o con espacios: no puede ser una ruta"
    m = CITA.match(texto)
    if not m or not m.group("ruta"):
        return "no tiene la forma ruta, ruta/, ruta:N ni ruta:N-M"
    ruta = m.group("ruta")
    if ruta.startswith("/"):
        return "ruta absoluta"
    if "\\" in ruta:
        return "barra invertida"
    directorio = ruta.endswith("/")
    if directorio:
        ruta = ruta[:-1]
    if any(trozo in ("", ".", "..") for trozo in ruta.split("/")):
        return "tramo vacío, . o .."
    n = m.group("n")
    if n is None:
        return ruta, directorio, None, None
    if directorio:
        return "un directorio no lleva líneas"
    n = int(n)
    fin = int(m.group("m")) if m.group("m") is not None else n
    if not 1 <= n <= fin:
        return "líneas fuera de 1 ≤ N ≤ M"
    return ruta, directorio, n, fin


# ------------------------------------------------------------------------------ revisión

def tipo_esperado(tipo, directorio, n):
    if directorio:
        return tipo == "tree"
    if n is not None:
        return tipo == "blob"
    return tipo in ("blob", "tree")


def linea_de_commit(raiz, documento, lineas, r):
    candidatas = [(numero, linea) for numero, linea in fuera_de_bloques(lineas, documento, Resultado())
                  if CANDIDATA_COMMIT.match(linea)]
    if not candidatas:
        r.fallo(documento, 1, "commit", "falta la línea «Describe el commit <hash>.»")
        return None
    if len(candidatas) > 1:
        r.fallo(documento, candidatas[1][0], "commit",
                "la línea de commit aparece %d veces (líneas %s)"
                % (len(candidatas), ", ".join(str(n) for n, _ in candidatas)))
        return None
    numero, linea = candidatas[0]
    m = LINEA_COMMIT.match(linea)
    if not m:
        r.fallo(documento, numero, "commit", "la línea no es «Describe el commit <hash>.»")
        return None
    if numero > LINEAS_CABECERA:
        r.fallo(documento, numero, "commit",
                "la línea de commit está en la %d, fuera de las %d primeras" % (numero, LINEAS_CABECERA))
        return None
    commit = resolver_commit(raiz, m.group(1))
    if commit is None:
        r.fallo(documento, numero, "commit", "el commit %s no existe en el repo" % m.group(1))
    return commit


def revisar_documento(raiz, documento, r, cache):
    ruta_disco = os.path.join(raiz, documento)
    r.documentos += 1
    tamano = os.path.getsize(ruta_disco)
    if tamano > TOPE_BYTES:
        r.fallo(documento, 1, "tamaño", "%d bytes, más de %d" % (tamano, TOPE_BYTES))
    with open(ruta_disco, "rb") as f:
        crudo = f.read()
    try:
        texto = crudo.decode("utf-8")
    except UnicodeDecodeError:
        r.fallo(documento, 1, "forma", "no es UTF-8")
        return
    lineas = [linea.rstrip("\r") for linea in texto.split("\n")]
    commit = linea_de_commit(raiz, documento, lineas, r)
    for numero, linea in fuera_de_bloques(lineas, documento, r):
        for codigo, problema in codigos_en_linea(linea):
            if problema:
                r.fallo(documento, numero, "forma", "%s: %s" % (codigo, problema))
                continue
            cita = analizar_cita(codigo)
            if isinstance(cita, str):
                r.fallo(documento, numero, "forma", "%s: %s" % (codigo, cita))
                continue
            ruta, directorio, n, fin = cita
            if en_docs_tecnica(ruta):
                r.rutas += 1
                revisar_en_arbol(raiz, documento, numero, codigo, cita, r)
                continue
            if commit is None:
                continue
            r.rutas += 1
            tipo = tipo_objeto(raiz, commit, ruta, cache)
            if tipo is None:
                r.fallo(documento, numero, "rota", "%s: no existe en %s" % (codigo, commit[:12]))
                continue
            if not tipo_esperado(tipo, directorio, n):
                r.fallo(documento, numero, "rota", "%s: es %s en %s" % (codigo, tipo, commit[:12]))
                continue
            if n is not None:
                total = lineas_del_blob(raiz, commit, ruta)
                if fin > total:
                    r.fallo(documento, numero, "líneas",
                            "%s: el fichero tiene %d líneas en %s" % (codigo, total, commit[:12]))
                    continue
            tipo_head = tipo_objeto(raiz, "HEAD", ruta, cache)
            if tipo_head is None or not tipo_esperado(tipo_head, directorio, n):
                r.avisos.append((documento, numero, "%s: existe en %s y no en HEAD" % (codigo, commit[:12])))


def revisar(raiz):
    r = Resultado()
    carpeta = os.path.join(raiz, CARPETA_DOCS)
    nombres = []
    if os.path.isdir(carpeta):
        nombres = sorted(n for n in os.listdir(carpeta)
                         if n.endswith(".md") and os.path.isfile(os.path.join(carpeta, n)))
    if not nombres:
        r.fallo(CARPETA_DOCS, 0, "vacío", "no hay ningún documento .md")
        return r
    cache = {}
    for nombre in nombres:
        revisar_documento(raiz, CARPETA_DOCS + "/" + nombre, r, cache)
    return r


# -------------------------------------------------------------------------------- salida

def escapar_dato(s):
    return s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escapar_propiedad(s):
    return escapar_dato(s).replace(":", "%3A").replace(",", "%2C")


def anotacion(nivel, documento, linea, mensaje):
    propiedades = "file=" + escapar_propiedad(documento)
    if linea:
        propiedades += ",line=%d" % linea
    return "::%s %s::%s" % (nivel, propiedades, escapar_dato(mensaje))


def informar(r, en_actions):
    for documento, linea, categoria, detalle in r.fallos:
        lugar = "%s:%d" % (documento, linea) if linea else documento
        print("%s: FALLO %s: %s" % (lugar, categoria, detalle))
        if en_actions:
            print(anotacion("error", documento, linea, "%s: %s" % (categoria, detalle)))
    for documento, linea, detalle in r.avisos:
        print("%s:%d: AVISO: %s" % (documento, linea, detalle))
        if en_actions:
            print(anotacion("warning", documento, linea, detalle))
    print("documentos: %d" % r.documentos)
    print("rutas comprobadas: %d" % r.rutas)
    print("rotas: %d" % sum(1 for f in r.fallos if f[2] == "rota"))
    print("avisos: %d" % len(r.avisos))
    print("comprobaciones duras con fallo: %d" % len(r.fallos))


# ---------------------------------------------------------------------------- AUTOPRUEBA
# Un comprobador que con nada da «0 rotas» no mide (A5). Cada defecto va en su documento y
# tiene que saltar en SU categoría y en ninguna otra; los controles no pueden dar nada.

ENTORNO_FABRICA = dict(os.environ, GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM="1")
IDENTIDAD = ["-c", "user.name=autoprueba", "-c", "user.email=autoprueba@invalid",
             "-c", "commit.gpgsign=false"]


def escribir(raiz, ruta, contenido):
    destino = os.path.join(raiz, ruta)
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="") as f:
        f.write(contenido)


def fabricar_repo(raiz):
    """Repo con dos commits: el viejo tiene viejo.txt; el nuevo (HEAD) lo borra."""
    def g(*args):
        r = git(raiz, *IDENTIDAD, *args, entorno=ENTORNO_FABRICA)
        if r.returncode != 0:
            raise RuntimeError("git %s: %s" % (" ".join(args), r.stderr.decode().strip()))
        return r.stdout.decode().strip()

    r = subprocess.run(["git", "-c", "init.defaultBranch=main", "init", "-q", raiz],
                       capture_output=True, env=ENTORNO_FABRICA)
    if r.returncode != 0:
        raise RuntimeError("git init: %s" % r.stderr.decode().strip())
    escribir(raiz, "a.txt", "uno\ndos\ntres")
    escribir(raiz, "dir/b.txt", "b\n")
    escribir(raiz, "ñandú/tilde.txt", "tilde\n")
    escribir(raiz, "viejo.txt", "viejo\n")
    g("add", "-A")
    g("commit", "-q", "-m", "viejo")
    viejo = g("rev-parse", "HEAD")
    g("rm", "-q", "viejo.txt")
    g("commit", "-q", "-m", "nuevo")
    return viejo, g("rev-parse", "HEAD")


def documento(commit, cuerpo, lineas_commit=1):
    cabecera = "# Prueba\n\nResponde a una pregunta de prueba.\nSe lee en la autoprueba.\n\n"
    return cabecera + ("Describe el commit %s.\n\n" % commit) * lineas_commit + cuerpo


def a_tamano(texto, tamano):
    relleno = tamano - len(texto.encode("utf-8")) - 1
    return texto + "x" * relleno + "\n"


def casos(viejo, nuevo):
    """(nombre, documento o None, categorías de fallo esperadas, avisos esperados, rutas o None[,
    otros documentos de docs/tecnica que el caso necesita en el árbol])."""
    v, n = viejo[:12], nuevo[:12]
    limpio = documento(n, "Ver `a.txt` y `dir/b.txt`.\n")
    defectos = [
        ("d1 ruta que no existe", documento(n, "Ver `no-existe.txt`.\n"), {"rota"}, 0, None),
        ("d2 commit que no existe", documento("dead0000beef", "Ver `a.txt`.\n"), {"commit"}, 0, None),
        ("d3 sin línea de commit", documento(n, "Ver `a.txt`.\n", 0), {"commit"}, 0, None),
        ("d3 dos líneas de commit", documento(n, "Ver `a.txt`.\n", 2), {"commit"}, 0, None),
        ("d4 documento de 20 481 bytes", a_tamano(limpio, TOPE_BYTES + 1), {"tamaño"}, 0, None),
        ("d5 líneas fuera de rango", documento(n, "Ver `a.txt:2-4`.\n"), {"líneas"}, 0, None),
        ("d6 ruta absoluta", documento(n, "Ver `/a.txt`.\n"), {"forma"}, 0, None),
        ("d6 ruta con ./", documento(n, "Ver `./a.txt`.\n"), {"forma"}, 0, None),
        ("d6 ruta con ../", documento(n, "Ver `../a.txt`.\n"), {"forma"}, 0, None),
        ("d6 ruta con barra invertida", documento(n, "Ver `dir\\b.txt`.\n"), {"forma"}, 0, None),
        ("d7 código con espacio", documento(n, "Se corre `mvn test`.\n"), {"forma"}, 0, None),
        ("d8 existe en el commit citado y no en HEAD", documento(v, "Ver `viejo.txt`.\n"), set(), 1, None),
        ("d9 docs/tecnica sin documentos", None, {"vacío"}, 0, None),
        ("d10 ruta de docs/tecnica que no existe en el árbol",
         documento(n, "Ver `docs/tecnica/no-existe.md`.\n"), {"rota"}, 0, None),
    ]
    controles = [
        ("c1 documento limpio", documento(n, "Ver `ñandú/tilde.txt`, `dir/` y `a.txt:1-3`.\n"),
         set(), 0, 3),
        ("c2 ruta inexistente en bloques delimitados",
         documento(n, "Orden:\n\n```\n`no-existe.txt`\n```\n\n~~~~\n`otra-que-no.txt`\n~~~~\n"),
         set(), 0, 0),
        ("c3 documento de 20 480 bytes", a_tamano(limpio, TOPE_BYTES), set(), 0, 2),
        ("c4 ruta de docs/tecnica que está en el árbol y no en el commit",
         documento(n, "Ver `docs/tecnica/otro.md`, `docs/tecnica/otro.md:1-3` y `docs/tecnica/`.\n"),
         set(), 0, 3, {"otro.md": documento(n, "Sin rutas.\n")}),
    ]
    return defectos, controles


def probar(raiz, caso):
    nombre, contenido, categorias, avisos, rutas, *resto = caso
    otros = resto[0] if resto else {}
    carpeta = os.path.join(raiz, CARPETA_DOCS)
    os.makedirs(carpeta, exist_ok=True)
    for viejo in os.listdir(carpeta):
        os.remove(os.path.join(carpeta, viejo))
    if contenido is not None:
        escribir(raiz, CARPETA_DOCS + "/prueba.md", contenido)
    for otro, texto in otros.items():
        escribir(raiz, CARPETA_DOCS + "/" + otro, texto)
    r = revisar(raiz)
    if r.categorias() != categorias:
        return "%s: fallos %s, se esperaba %s" % (nombre, sorted(r.categorias()), sorted(categorias))
    if len(r.avisos) != avisos:
        return "%s: %d avisos, se esperaba %d" % (nombre, len(r.avisos), avisos)
    if rutas is not None and r.rutas != rutas:
        return "%s: %d rutas comprobadas, se esperaba %d" % (nombre, r.rutas, rutas)
    return None


def autoprueba():
    """Lista de problemas (vacía si pasa) y número de defectos inyectados."""
    problemas = []
    with tempfile.TemporaryDirectory(prefix="comprobar-rutas-") as raiz:
        viejo, nuevo = fabricar_repo(raiz)
        defectos, controles = casos(viejo, nuevo)
        for caso in defectos + controles:
            try:
                problema = probar(raiz, caso)
            except Exception as e:  # noqa: BLE001 — un caso que revienta tampoco pasa
                problema = "%s: excepción inesperada: %r" % (caso[0], e)
            if problema:
                problemas.append(problema)
    return problemas, len(defectos)


# ---------------------------------------------------------------------------------- main

def main(argv=None):
    analizador = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    analizador.add_argument("--raiz", required=True, help="ruta absoluta de la raíz del repo")
    args = analizador.parse_args(argv)
    if not os.path.isabs(args.raiz):
        print("--raiz tiene que ser una ruta absoluta: %s" % args.raiz, file=sys.stderr)
        return 2
    raiz = os.path.normpath(args.raiz)
    if not os.path.isdir(raiz):
        print("--raiz no es un directorio: %s" % raiz, file=sys.stderr)
        return 2

    problemas, inyectados = autoprueba()
    if problemas:
        for p in problemas:
            print("FALLO DE AUTOPRUEBA: %s" % p)
        return 3
    print("AUTOPRUEBA: %d defectos inyectados, los %d detectados." % (inyectados, inyectados))

    r = revisar(raiz)
    informar(r, os.environ.get("GITHUB_ACTIONS") == "true")
    return 1 if r.fallos else 0


if __name__ == "__main__":
    sys.exit(main())
