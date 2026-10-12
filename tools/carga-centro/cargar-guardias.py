#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Carga guardias.json en una base ya cargada, por la API REST (S216 T2; S222 T1).

Nace en S216 (C-prueba-guardias-2026, condición 7 de O-guardias), con el contrato de
s216/t0/CONTRATO.md (C2) y su enmienda 1 (E2). S222 (C-prueba-guardias-especiales-2026, condición 7
de O-guardias-especiales; contrato s222/t0 con su enmienda): guardias de recreo, mínimos, y
biblioteca y convivencia como funciones de su profesor en lugar de «No puede» (decisión A de S218).

USO
    python3 -B tools/carga-centro/cargar-guardias.py [--prevalidar | --cargar] \\
        --base-url <url> --guardias <guardias.json, ruta absoluta> \\
        [--minimo-tramo <n> --minimo-recreo <n> --minimo-biblioteca <n>]
    --base-url es obligatorio y no tiene valor por defecto (desviación deliberada de
    cargar-centro.py y cargar-reglas-aulas.py, E2). Los tres mínimos van juntos o ninguno.

Reutiliza el cliente HTTP de cargar-centro.py, importado por ruta como en cargar-reglas-aulas.py:
una respuesta no-2xx es fatal (sale con 2) y el motivo se lee en el stdout de la aplicación. La
regla A11 del patrón temporal es la de derivar-catalogo.py, importada y sin copia.

PREVALIDACION en seco, antes de cualquier escritura: un listado de profesores y casación por código.
Aborta sin escribir si un código del JSON no existe en la base, si la base tiene profesores que el
JSON no trae, o si una función tiene un profesor que no está en el JSON, una asignatura sin nombre
conocido (Gbibl, conv), una clave (profesor, asignatura) repetida, ninguna celda o una celda con el
día fuera de 1..5 o el tramo fuera de 1..6.

PLAN, también en seco y completo antes de la primera escritura, en este orden:
  - configuración (solo con los tres mínimos): GET /api/configuracion-guardias; PUT del cuerpo
    entero si alguno de los tres difiere.
  - profesor: GET del profesor; si guardiasOrdinarias o guardiasRecreo difieren, PUT con el cuerpo
    entero del GET sin «id» y solo esas dos cambiadas (el PUT es de reemplazo total).
  - restricciones, de todos los profesores del JSON: GET de la lista; se retiran las DURA con
    motivo Gbibl o conv (las que escribió S216) y todo lo demás se conserva tal cual, en la forma
    del request. PUT de la lista restante solo si se retira algo.
  - asignaturas: Gbibl y conv, con el rótulo del PDF (INFORME-NO-REPRESENTABLE.md de 2026/2027),
    POST solo de las que no están en GET /api/asignaturas.
  - actividades: una FUNCION por función del JSON, con la forma de derivar-catalogo.py
    (_actividad_no_clase): código {asignatura}-{profesor}, duración 1, repeticiones = celdas,
    patrón por A11, sin tutor, una plaza sin aula ni subgrupos. POST solo de los códigos que no
    están en GET /api/actividades; una presente con otra forma no se corrige.
Imprime las escrituras previstas; con --prevalidar (por defecto) termina ahí con rc 0.

CARGA (--cargar): las escrituras del plan, relectura de todo lo que el plan fija y comparación con
el objetivo; rc 1 si hay diferencias o las escrituras reales no son las previstas. Una segunda
corrida no envía nada.
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
_spec = importlib.util.spec_from_file_location("derivar_catalogo", Path(__file__).resolve().parent / "derivar-catalogo.py")
dc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(dc)

# Campos de ProfesorRequest y de RestriccionHorariaRequest (s216/t0/p2f-json.txt; guardiasRecreo, S219).
CAMPOS_PROFESOR = ("codigo", "nombreCompleto", "totalDeclarado", "cargo", "guardiasOrdinarias", "guardiasRecreo")
CAMPOS_RESTRICCION = ("tipo", "dia", "ordenEnDia", "motivo")
# ConfiguracionGuardiasDTO.java:13; el PUT exige los tres (ConfiguracionGuardiasService.java:50-52).
CAMPOS_CONFIGURACION = ("minimoPorTramo", "minimoPorRecreo", "minimoBibliotecaPorRecreo")
CAMPOS_ACTIVIDAD = ("tipo", "asignatura", "duracionTramos", "repeticionesPorSemana", "patronTemporal",
                    "requiereTutor")
CAMPOS_PLAZA = ("asignatura", "aulaFija", "aulasCandidatas", "profesores", "subgrupos")
DURA = "DURA"
# Rótulos de la leyenda del PDF (docs/horario-referencia/2026-2027/INFORME-NO-REPRESENTABLE.md:14).
NOMBRES_ASIGNATURA = {"Gbibl": "Gbiblioteca", "conv": "R.Convivencia"}
RUTA_CONFIGURACION = "/api/configuracion-guardias"
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


def codigo_funcion(f):
    return "%s-%s" % (f["asignatura"], f["profesor"])


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
    for f in datos["funciones"]:
        donde = "funcion %s %s" % (f["profesor"], f["asignatura"])
        if f["profesor"] not in del_json:
            fallos.append("%s: el profesor no está en «profesores»" % donde)
        if f["asignatura"] not in NOMBRES_ASIGNATURA:
            fallos.append("%s: asignatura sin nombre conocido (%s)" % (donde, ", ".join(NOMBRES_ASIGNATURA)))
        if (f["profesor"], f["asignatura"]) in vistas:
            fallos.append("%s: repetida" % donde)
        vistas.add((f["profesor"], f["asignatura"]))
        if not f["celdas"]:
            fallos.append("%s: sin celdas" % donde)
        for c in f["celdas"]:
            if c["dia"] not in DIAS or c["ordenEnDia"] not in TRAMOS:
                fallos.append("%s (%r, %r): día o tramo fuera de rango" % (donde, c["dia"], c["ordenEnDia"]))
    if fallos:
        raise Rechazo("\n".join(fallos))
    return ids


def restricciones_sin_funciones(existentes):
    """(lista objetivo en forma de request, ordenada; ¿cambia?): fuera las DURA de Gbibl y conv."""
    actual = sorted(({c: r.get(c) for c in CAMPOS_RESTRICCION} for r in existentes), key=clave)
    objetivo = [r for r in actual if not (r["tipo"] == DURA and r["motivo"] in NOMBRES_ASIGNATURA)]
    return objetivo, len(objetivo) != len(actual)


def cuerpo_funcion(f):
    """El POST de una FUNCION, con la forma de derivar-catalogo.py:741-751."""
    instancias = [(c["dia"], c["ordenEnDia"]) for c in f["celdas"]]
    return {
        "codigo": codigo_funcion(f),
        "tipo": "FUNCION",
        "asignatura": f["asignatura"],
        "duracionTramos": 1,
        "repeticionesPorSemana": len(instancias),
        "patronTemporal": dc.patron_temporal(instancias),
        "requiereTutor": False,
        "plazas": [{"asignatura": f["asignatura"], "aulaFija": None, "aulasCandidatas": [],
                    "profesores": [f["profesor"]], "subgrupos": []}],
    }


def forma_actividad(a):
    """Lo que la relectura compara de una actividad: sin id ni códigos de plaza."""
    return dict({c: a.get(c) for c in CAMPOS_ACTIVIDAD},
                plazas=[{c: p.get(c) for c in CAMPOS_PLAZA} for p in a.get("plazas") or []])


def plan(cliente, datos, ids, minimos=None):
    """([(familia, método, ruta, cuerpo)] a escribir, en orden; {ruta: objetivo} a releer)."""
    escrituras, objetivos = [], {}
    if minimos is not None:
        leido = cliente.get(RUTA_CONFIGURACION)
        objetivo = {c: minimos[c] for c in CAMPOS_CONFIGURACION}
        objetivos[RUTA_CONFIGURACION] = objetivo
        if {c: leido.get(c) for c in CAMPOS_CONFIGURACION} != objetivo:
            escrituras.append(("configuración", "PUT", RUTA_CONFIGURACION, objetivo))
    for p in sorted(datos["profesores"], key=lambda x: x["codigo"]):
        ruta = ruta_profesor(ids[p["codigo"]])
        leido = cliente.get(ruta)
        faltan = [c for c in CAMPOS_PROFESOR if c not in leido]
        if faltan:
            raise Rechazo("profesor %s: el GET no trae %s y el PUT los borraría" % (p["codigo"], faltan))
        cuerpo = {c: v for c, v in leido.items() if c != "id"}
        cambiado = dict(cuerpo, guardiasOrdinarias=p["guardiasOrdinarias"], guardiasRecreo=p["guardiasRecreo"])
        objetivos[ruta] = cambiado
        if cambiado != cuerpo:
            escrituras.append(("profesor", "PUT", ruta, cambiado))
    for codigo in sorted(p["codigo"] for p in datos["profesores"]):
        ruta = ruta_restricciones(ids[codigo])
        objetivo, cambia = restricciones_sin_funciones(cliente.get(ruta))
        objetivos[ruta] = objetivo
        if cambia:
            escrituras.append(("restricciones", "PUT", ruta, objetivo))
    asignaturas = {a["codigo"] for a in cliente.get("/api/asignaturas")}
    for codigo in sorted({f["asignatura"] for f in datos["funciones"]}):
        if codigo not in asignaturas:
            escrituras.append(("asignaturas", "POST", "/api/asignaturas",
                               {"codigo": codigo, "nombreCompleto": NOMBRES_ASIGNATURA[codigo]}))
    actividades = {a["codigo"] for a in cliente.get("/api/actividades")}
    for f in sorted(datos["funciones"], key=lambda x: (x["profesor"], x["asignatura"])):
        cuerpo = cuerpo_funcion(f)
        objetivos[("actividad", cuerpo["codigo"])] = forma_actividad(cuerpo)
        if cuerpo["codigo"] not in actividades:
            escrituras.append(("actividades", "POST", "/api/actividades", cuerpo))
    return escrituras, objetivos


def releer(cliente, objetivos):
    """Diferencias entre el objetivo y lo que hay en la base tras escribir."""
    difs = []
    actividades = None
    for ruta, objetivo in objetivos.items():
        if isinstance(ruta, tuple):
            if actividades is None:
                actividades = {a["codigo"]: a for a in cliente.get("/api/actividades")}
            leida = actividades.get(ruta[1])
            actual = None if leida is None else forma_actividad(leida)
            ruta = "actividad %s" % ruta[1]
        elif ruta == RUTA_CONFIGURACION:
            leido = cliente.get(ruta)
            actual = {c: leido.get(c) for c in CAMPOS_CONFIGURACION}
        else:
            leido = cliente.get(ruta)
            if isinstance(objetivo, list):
                actual = sorted(({c: r.get(c) for c in CAMPOS_RESTRICCION} for r in leido), key=clave)
            else:
                actual = {c: v for c, v in leido.items() if c != "id"}
        if actual != objetivo:
            difs.append("%s: %s, se esperaba %s" % (ruta, actual, objetivo))
    return difs


FAMILIAS = ("configuración", "profesor", "restricciones", "asignaturas", "actividades")


def aplicar(cliente, datos, cargar=False, minimos=None):
    """Prevalida y planifica; con cargar, escribe y relee. Devuelve el código de salida (0, 1)."""
    try:
        ids = prevalidar(cliente, datos)
        escrituras, objetivos = plan(cliente, datos, ids, minimos)
    except Rechazo as e:
        print("ABORTA: la prevalidación no está limpia. No se envía nada.")
        print("  " + str(e).replace("\n", "\n  "))
        return 1
    previstas = len(escrituras)
    por_familia = {f: sum(1 for x in escrituras if x[0] == f) for f in FAMILIAS}
    print("  profesores: %d; funciones: %d; mínimos: %s"
          % (len(datos["profesores"]), len(datos["funciones"]), "sí" if minimos is not None else "no"))
    print("  escrituras previstas: %d (%s)"
          % (previstas, ", ".join("%s %d" % (f, por_familia[f]) for f in FAMILIAS)))
    if not cargar:
        print("Modo prevalidacion: no se ha enviado nada. Usa --cargar para escribir.")
        return 0
    antes = cliente.escrituras
    for _, metodo, ruta, cuerpo in escrituras:
        if metodo == "PUT":
            cliente.put(ruta, cuerpo)
        else:
            cliente.post(ruta, cuerpo)
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
    parser.add_argument("--minimo-tramo", type=int, help="minimoPorTramo")
    parser.add_argument("--minimo-recreo", type=int, help="minimoPorRecreo")
    parser.add_argument("--minimo-biblioteca", type=int, help="minimoBibliotecaPorRecreo")
    args = parser.parse_args(argv)
    if not os.path.isabs(args.guardias):
        parser.error("--guardias tiene que ser una ruta absoluta: %s" % args.guardias)
    valores = (args.minimo_tramo, args.minimo_recreo, args.minimo_biblioteca)
    if any(v is None for v in valores) and any(v is not None for v in valores):
        parser.error("--minimo-tramo, --minimo-recreo y --minimo-biblioteca: los tres o ninguno")
    minimos = None if valores[0] is None else dict(zip(CAMPOS_CONFIGURACION, valores))
    print("guardias: %s" % args.guardias)
    datos = json.loads(Path(args.guardias).read_text(encoding="utf-8"))
    print("CARGA DE GUARDIAS contra %s (%s)" % (args.base_url, "cargar" if args.cargar else "prevalidar"))
    try:
        return aplicar(crear_cliente(args.base_url), datos, cargar=args.cargar, minimos=minimos)
    except cc.ErrorFatal as e:
        print()
        print(str(e))
        return 2


if __name__ == "__main__":
    sys.exit(main())
