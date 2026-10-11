#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Carga guardias.json en una base ya cargada, por la API REST (S216 T2).

Nace en S216 (C-prueba-guardias-2026, condición 7 de O-guardias), con el contrato de
s216/t0/CONTRATO.md (C2) y su enmienda 1 (E2).

USO
    python3 -B tools/carga-centro/cargar-guardias.py [--prevalidar | --cargar] \\
        --base-url <url> --guardias <guardias.json, ruta absoluta>
    --base-url es obligatorio y no tiene valor por defecto (desviación deliberada de
    cargar-centro.py y cargar-reglas-aulas.py, E2).

Reutiliza el cliente HTTP de cargar-centro.py, importado por ruta como en cargar-reglas-aulas.py:
una respuesta no-2xx es fatal (sale con 2) y el motivo se lee en el stdout de la aplicación.

PREVALIDACION en seco, antes de cualquier PUT: un listado de profesores y casación por código.
Aborta sin escribir si un código del JSON no existe en la base, si la base tiene profesores que el
JSON no trae, o si un «No puede» tiene un día fuera de 1..5, un tramo fuera de 1..6, un profesor
que no está en el JSON o una clave repetida.

PLAN, también en seco y completo antes del primer PUT:
  - profesor: GET del profesor; si guardiasOrdinarias difiere, PUT con el cuerpo entero del GET sin
    «id» y solo guardiasOrdinarias cambiada (el PUT es de reemplazo total).
  - restricciones: GET de la lista; clave (dia, ordenEnDia). Lo existente se conserva tal cual, en
    la forma del request; cada «No puede» del JSON es una DURA con su motivo. Misma clave con el
    mismo tipo y motivo: ya está. Misma clave con otro tipo u otro motivo: conflicto, y si hay
    alguno se aborta sin escribir NADA. PUT de la lista completa solo si se añade algo.
Imprime las escrituras previstas; con --prevalidar (por defecto) termina ahí con rc 0.

CARGA (--cargar): los PUT del plan, relectura de todo lo que el JSON fija y comparación con el
objetivo; rc 1 si hay diferencias o las escrituras reales no son las previstas. Una segunda
corrida no envía ningún PUT. No toca /api/configuracion-guardias: el mínimo por tramo se queda en
el que tenga la base.
"""

import argparse
import importlib.util
import json
import os
import sys
from pathlib import Path

_spec = importlib.util.spec_from_file_location("cargar_centro", Path(__file__).resolve().parent / "cargar-centro.py")
cc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cc)

# Campos de ProfesorRequest y de RestriccionHorariaRequest (s216/t0/p2f-json.txt).
CAMPOS_PROFESOR = ("codigo", "nombreCompleto", "totalDeclarado", "cargo", "guardiasOrdinarias")
CAMPOS_RESTRICCION = ("tipo", "dia", "ordenEnDia", "motivo")
DURA = "DURA"
DIAS = range(1, 6)
TRAMOS = range(1, 7)


class Rechazo(Exception):
    """La prevalidación o el plan no están limpios: no se escribe nada."""


def crear_cliente(base_url):
    return cc.Cliente(base_url)


def clave(r):
    return (r["dia"], r["ordenEnDia"])


def ruta_profesor(id_):
    return "/api/profesores/%d" % id_


def ruta_restricciones(id_):
    return "/api/profesores/%d/restricciones-horarias" % id_


def prevalidar(cliente, datos):
    """Listado de profesores y casación por código. Devuelve {código: id} o lanza Rechazo."""
    ids = {x["codigo"]: x["id"] for x in cliente.get("/api/profesores")}
    del_json = [p["codigo"] for p in datos["profesores"]]
    fallos = []
    faltan = sorted(set(del_json) - set(ids))
    if faltan:
        fallos.append("profesores del JSON que no existen en la base: %s" % faltan)
    sobran = sorted(set(ids) - set(del_json))
    if sobran:
        fallos.append("profesores de la base que el JSON no trae: %s" % sobran)
    vistas = set()
    for r in datos["noPuede"]:
        donde = "noPuede %s (%r, %r)" % (r["codigo"], r["dia"], r["ordenEnDia"])
        if r["dia"] not in DIAS or r["ordenEnDia"] not in TRAMOS:
            fallos.append("%s: día o tramo fuera de rango" % donde)
        if r["codigo"] not in del_json:
            fallos.append("%s: el profesor no está en «profesores»" % donde)
        if (r["codigo"],) + clave(r) in vistas:
            fallos.append("%s: clave repetida" % donde)
        vistas.add((r["codigo"],) + clave(r))
    if fallos:
        raise Rechazo("\n".join(fallos))
    return ids


def objetivo_restricciones(existentes, no_puede):
    """(lista objetivo en forma de request, ordenada; conflictos; ¿cambia?)."""
    actual = {clave(r): {c: r.get(c) for c in CAMPOS_RESTRICCION} for r in existentes}
    objetivo, conflictos, cambia = dict(actual), [], False
    for r in no_puede:
        nueva = {"tipo": DURA, "dia": r["dia"], "ordenEnDia": r["ordenEnDia"], "motivo": r["motivo"]}
        ya = actual.get(clave(r))
        if ya is None:
            objetivo[clave(r)] = nueva
            cambia = True
        elif ya != nueva:
            conflictos.append("(dia=%d, ordenEnDia=%d) ya tiene %s «%s» y el JSON pide DURA «%s»"
                              % (r["dia"], r["ordenEnDia"], ya["tipo"], ya["motivo"], r["motivo"]))
    return [objetivo[k] for k in sorted(objetivo)], conflictos, cambia


def plan(cliente, datos, ids):
    """([(ruta, cuerpo)] a escribir, {ruta: objetivo} a releer). Rechazo ante un conflicto."""
    puts, objetivos, conflictos = [], {}, []
    for p in sorted(datos["profesores"], key=lambda x: x["codigo"]):
        ruta = ruta_profesor(ids[p["codigo"]])
        leido = cliente.get(ruta)
        faltan = [c for c in CAMPOS_PROFESOR if c not in leido]
        if faltan:
            raise Rechazo("profesor %s: el GET no trae %s y el PUT los borraría" % (p["codigo"], faltan))
        cuerpo = {c: v for c, v in leido.items() if c != "id"}
        cambiado = dict(cuerpo, guardiasOrdinarias=p["guardiasOrdinarias"])
        objetivos[ruta] = cambiado
        if cambiado != cuerpo:
            puts.append((ruta, cambiado))
    por_profesor = {}
    for r in datos["noPuede"]:
        por_profesor.setdefault(r["codigo"], []).append(r)
    for codigo in sorted(por_profesor):
        ruta = ruta_restricciones(ids[codigo])
        objetivo, choques, cambia = objetivo_restricciones(cliente.get(ruta), por_profesor[codigo])
        conflictos += ["profesor %s: %s" % (codigo, c) for c in choques]
        objetivos[ruta] = objetivo
        if cambia:
            puts.append((ruta, objetivo))
    if conflictos:
        raise Rechazo("conflictos con restricciones existentes (no se pisa nada):\n" + "\n".join(conflictos))
    return puts, objetivos


def releer(cliente, objetivos):
    """Diferencias entre el objetivo y lo que hay en la base tras escribir."""
    difs = []
    for ruta, objetivo in objetivos.items():
        leido = cliente.get(ruta)
        if isinstance(objetivo, list):
            actual = sorted(({c: r.get(c) for c in CAMPOS_RESTRICCION} for r in leido), key=clave)
        else:
            actual = {c: v for c, v in leido.items() if c != "id"}
        if actual != objetivo:
            difs.append("%s: %s, se esperaba %s" % (ruta, actual, objetivo))
    return difs


def aplicar(cliente, datos, cargar=False):
    """Prevalida y planifica; con cargar, escribe y relee. Devuelve el código de salida (0, 1)."""
    try:
        ids = prevalidar(cliente, datos)
        puts, objetivos = plan(cliente, datos, ids)
    except Rechazo as e:
        print("ABORTA: la prevalidación no está limpia. No se envía nada.")
        print("  " + str(e).replace("\n", "\n  "))
        return 1
    de_profesor = sum(1 for ruta, _ in puts if not ruta.endswith("/restricciones-horarias"))
    previstas = len(puts)
    print("  profesores: %d; «No puede»: %d" % (len(datos["profesores"]), len(datos["noPuede"])))
    print("  escrituras previstas: %d (PUT de profesor %d, PUT de restricciones %d)"
          % (previstas, de_profesor, previstas - de_profesor))
    if not cargar:
        print("Modo prevalidacion: no se ha enviado nada. Usa --cargar para escribir.")
        return 0
    antes = cliente.escrituras
    for ruta, cuerpo in puts:
        cliente.put(ruta, cuerpo)
    reales = cliente.escrituras - antes
    difs = releer(cliente, objetivos)
    print("  escrituras reales: %d %s" % (reales, "OK" if reales == previstas else "DISTINTAS DE LAS PREVISTAS"))
    print("  relectura: %s" % ("sin diferencias" if not difs else "%d diferencias" % len(difs)))
    for x in difs:
        print("    " + x)
    return 0 if not difs and reales == previstas else 1


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--guardias", required=True, help="guardias.json (ruta absoluta)")
    modo = parser.add_mutually_exclusive_group()
    modo.add_argument("--prevalidar", action="store_true",
                      help="solo calcula y muestra las escrituras previstas (por defecto)")
    modo.add_argument("--cargar", action="store_true", help="escribe, relee y compara")
    args = parser.parse_args(argv)
    if not os.path.isabs(args.guardias):
        parser.error("--guardias tiene que ser una ruta absoluta: %s" % args.guardias)
    print("guardias: %s" % args.guardias)
    datos = json.loads(Path(args.guardias).read_text(encoding="utf-8"))
    print("CARGA DE GUARDIAS contra %s (%s)" % (args.base_url, "cargar" if args.cargar else "prevalidar"))
    try:
        return aplicar(crear_cliente(args.base_url), datos, cargar=args.cargar)
    except cc.ErrorFatal as e:
        print()
        print(str(e))
        return 2


if __name__ == "__main__":
    sys.exit(main())
