#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Aplica reglas-aulas.json a una base ya cargada, por la API REST (S209 T2).

ENTRADAS  --reglas    reglas-aulas.json (de derivar-reglas-aulas.py); se normaliza a absoluta
DESTINO   http://localhost:8080 por defecto, --base-url para cambiarlo (como cargar-centro.py)

Reutiliza el cliente HTTP de cargar-centro.py, importado por ruta como lo importan sus tests: una
respuesta no-2xx es fatal (sale con 2) y el motivo se lee en el stdout de la aplicación.

PREVALIDACION en seco, antes de cualquier PUT: un listado por familia (aulas, grupos, asignaturas,
subgrupos) y casación por código. Aborta sin escribir si un código no existe, si una asignatura
mezcla EXCLUSIVA y PREFERIDA, si una regla de aula de referencia apunta a un grupo que no es
ORDINARIO, o si una entidad aparece en dos reglas de la misma familia.

APLICACION, en este orden: aulas (enUso y capacidad), grupos (aulaReferencia), asignaturas (la
lista entera de aulas con rol), subgrupos (alumnos). Cada PUT es un reemplazo total: el cuerpo es
lo leído por GET (los campos del DTO de entrada) con SOLO los campos de la regla cambiados, y se
envía solo si algo cambia. Las escrituras previstas se calculan antes de escribir.

RELECTURA tras escribir: GET de cada entidad con regla y comparación de los campos de la regla;
cualquier diferencia, o escrituras reales distintas de las previstas, sale con 1. Una segunda
corrida sobre la misma base no envía ningún PUT. `conservarAula` y `omitidas` no se usan aquí.
"""

import argparse
import importlib.util
import json
import sys
from pathlib import Path

_spec = importlib.util.spec_from_file_location("cargar_centro", Path(__file__).resolve().parent / "cargar-centro.py")
cc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cc)

# Campos del DTO de entrada de cada PUT (GrupoRequest, AulaRequest, SubgrupoRequest; S209 T2 F0).
CAMPOS = {
    "aulas": ("codigo", "tipo", "capacidad", "edificio", "planta", "sector", "enUso"),
    "grupos": ("codigo", "nivel", "tipo", "totalDeclarado", "aulaReferencia"),
    "subgrupos": ("codigo", "grupos", "alumnos"),
}


class Rechazo(Exception):
    """La prevalidación no está limpia: no se escribe nada."""


def crear_cliente(base_url):
    return cc.Cliente(base_url)


def deseado(reglas):
    """{familia: {código: {campo: valor}}} con lo que fija cada regla."""
    d = {"aulas": {}, "grupos": {}, "asignaturas": {}, "subgrupos": {}}
    dobles = []

    def poner(familia, codigo, campo, valor):
        if campo in d[familia].get(codigo, {}):
            dobles.append("%s %s: dos reglas fijan %s" % (familia, codigo, campo))
        d[familia].setdefault(codigo, {})[campo] = valor

    for r in reglas.get("usoAula", []):
        poner("aulas", r["aula"], "enUso", r["enUso"])
    for r in reglas.get("capacidad", []):
        poner("aulas", r["aula"], "capacidad", r["capacidad"])
    for r in reglas.get("aulaReferencia", []):
        poner("grupos", r["grupo"], "aulaReferencia", r["aula"])
    for r in reglas.get("aulasAsignatura", []):
        poner("asignaturas", r["asignatura"], "aulas",
              sorted(({"aula": a["aula"], "rol": a["rol"]} for a in r["aulas"]), key=lambda a: a["aula"]))
    for r in reglas.get("alumnos", []):
        poner("subgrupos", r["subgrupo"], "alumnos", r["alumnos"])
    return d, dobles


def prevalidar(cliente, d, dobles):
    """Listados y casación por código. Devuelve {familia: {código: id}} o lanza Rechazo."""
    fallos = list(dobles)
    ids, tipos = {}, {}
    for familia in ("aulas", "grupos", "asignaturas", "subgrupos"):
        listado = cliente.get("/api/%s" % familia)
        ids[familia] = {x["codigo"]: x["id"] for x in listado}
        if familia == "grupos":
            tipos = {x["codigo"]: x.get("tipo") for x in listado}
    for familia, entidades in d.items():
        for codigo in sorted(entidades):
            if codigo not in ids[familia]:
                fallos.append("%s %s: no existe en la base" % (familia, codigo))
    for g in sorted(d["grupos"]):
        if g in tipos and tipos[g] != "ORDINARIO":
            fallos.append("grupos %s: es %s y solo un grupo ORDINARIO admite aula de referencia" % (g, tipos[g]))
    aulas_usadas = {v["aulaReferencia"] for v in d["grupos"].values()}
    for a, v in sorted(d["asignaturas"].items()):
        roles = {x["rol"] for x in v["aulas"]}
        if len(roles) > 1:
            fallos.append("asignaturas %s: mezcla de roles %s" % (a, sorted(roles)))
        aulas_usadas.update(x["aula"] for x in v["aulas"])
    for a in sorted(aulas_usadas):
        if a not in ids["aulas"]:
            fallos.append("aula %s: la nombra una regla y no existe en la base" % a)
    if fallos:
        raise Rechazo("\n".join(fallos))
    return ids


def ruta_de(familia, id_):
    return "/api/asignaturas/%d/aulas" % id_ if familia == "asignaturas" else "/api/%s/%d" % (familia, id_)


def plan(cliente, d, ids):
    """[(familia, código, ruta, cuerpo)] de las entidades que difieren, en el orden de aplicación."""
    puts = []
    for familia in ("aulas", "grupos", "asignaturas", "subgrupos"):
        for codigo in sorted(d[familia]):
            ruta = ruta_de(familia, ids[familia][codigo])
            leido = cliente.get(ruta)
            if familia == "asignaturas":
                actual = sorted(({"aula": x["aula"], "rol": x["rol"]} for x in leido), key=lambda a: a["aula"])
                if actual != d[familia][codigo]["aulas"]:
                    puts.append((familia, codigo, ruta, d[familia][codigo]["aulas"]))
                continue
            faltan = [c for c in CAMPOS[familia] if c not in leido]
            if faltan:
                raise Rechazo("%s %s: el GET no trae %s y el PUT los borraría" % (familia, codigo, faltan))
            cuerpo = {c: leido[c] for c in CAMPOS[familia]}
            cambiado = dict(cuerpo, **d[familia][codigo])
            if cambiado != cuerpo:
                puts.append((familia, codigo, ruta, cambiado))
    return puts


def releer(cliente, d, ids):
    """Diferencias entre lo que fijan las reglas y lo que hay en la base tras escribir."""
    difs = []
    for familia in ("aulas", "grupos", "asignaturas", "subgrupos"):
        for codigo in sorted(d[familia]):
            leido = cliente.get(ruta_de(familia, ids[familia][codigo]))
            if familia == "asignaturas":
                actual = sorted(({"aula": x["aula"], "rol": x["rol"]} for x in leido), key=lambda a: a["aula"])
                if actual != d[familia][codigo]["aulas"]:
                    difs.append("%s %s: aulas %s, se esperaba %s" % (familia, codigo, actual, d[familia][codigo]["aulas"]))
                continue
            for campo, valor in d[familia][codigo].items():
                if leido.get(campo) != valor:
                    difs.append("%s %s: %s=%r, se esperaba %r" % (familia, codigo, campo, leido.get(campo), valor))
    return difs


def aplicar(cliente, reglas):
    """Prevalida, escribe y relee. Devuelve el código de salida (0, 1)."""
    d, dobles = deseado(reglas)
    try:
        ids = prevalidar(cliente, d, dobles)
        puts = plan(cliente, d, ids)
    except Rechazo as e:
        print("ABORTA: la prevalidación no está limpia. No se envía nada.")
        print("  " + str(e).replace("\n", "\n  "))
        return 1
    previstas = len(puts)
    print("  reglas: %s" % ", ".join("%s %d" % (f, len(d[f])) for f in d))
    print("  escrituras previstas (entidades que difieren): %d" % previstas)
    antes = cliente.escrituras
    for familia, codigo, ruta, cuerpo in puts:
        cliente.put(ruta, cuerpo)
    reales = cliente.escrituras - antes
    difs = releer(cliente, d, ids)
    print("  escrituras reales: %d %s" % (reales, "OK" if reales == previstas else "DISTINTAS DE LAS PREVISTAS"))
    print("  relectura: %s" % ("sin diferencias" if not difs else "%d diferencias" % len(difs)))
    for x in difs:
        print("    " + x)
    return 0 if not difs and reales == previstas else 1


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--reglas", required=True, help="reglas-aulas.json")
    parser.add_argument("--base-url", default="http://localhost:8080")
    args = parser.parse_args(argv)
    ruta = Path(args.reglas).resolve()
    print("reglas: %s" % ruta)
    reglas = json.loads(ruta.read_text(encoding="utf-8"))
    print("CARGA DE REGLAS contra %s" % args.base_url)
    try:
        return aplicar(crear_cliente(args.base_url), reglas)
    except cc.ErrorFatal as e:
        print()
        print(str(e))
        return 2


if __name__ == "__main__":
    sys.exit(main())
