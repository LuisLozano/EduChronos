#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Puebla el centro real por la API REST a partir del catalogo derivado.

ENTRADAS  docs/horario-referencia/catalogo-derivado.json  (estructura del centro)
          docs/horario-referencia/nombres-derivados.json  (nombres largos del PDF)
DESTINO   http://localhost:8080 por defecto, --base-url para cambiarlo

MODOS
    --prevalidar  (POR DEFECTO) no envia NADA. Valida el catalogo en seco contra
                  las reglas del modelo e informa. Es lo que se ejecuta si no se
                  pide otra cosa: cargar es la accion explicita, no el descuido.
    --cargar      ejecuta la carga.

ORDEN DE CARGA
    jornada, niveles, asignaturas, profesores, aulas, grupos ORDINARIO, PDC,
    tutorias, subgrupos, actividades. Es el orden de dependencias: nada se envia
    antes que aquello a lo que apunta.

IDEMPOTENCIA
    Antes de cada familia, UN solo GET de listado del que sale el mapa
    codigo -> id; se envia unicamente lo que falta. Ocho listados en total, sin
    GET individuales. Tres casos no siguen el molde:
      - las 28 tutorias se envian SIEMPRE, porque el PUT es un reemplazo total y
        el estado final no depende de lo que hubiera antes;
      - los PDC se detectan por su propio codigo en el listado de grupos, y el id
        del padre sale de ese mismo mapa;
      - la jornada se detecta por el campo "persistida" de GET /api/jornada, que
        responde 200 con la malla de referencia aunque no haya nada guardado.

PAYLOADS
    Se construyen campo a campo. NUNCA se reenvia el objeto del catalogo: ese
    lleva metadatos de derivacion (celdas, celdasEnVolcado, _referencia,
    tramoVolcado) que la API no conoce y que no tienen por que viajar.

OMISIONES DELIBERADAS
    - Los 5 subgrupos con creadoAutomaticamentePorPDC=true. Los crea la propia
      alta del PDC, con el codigo derivado {codigoPDC}-Completo; mandarlos seria
      un choque de codigo duplicado.

    Hasta S135 habia una segunda omision: las 11 actividades de FPB cuya unica
    plaza iba marcada con _aulaDesconocida=true (Hallazgo H). En el volcado no
    constaba su aula, incumplian el XOR y eran inexpresables por formulario, asi
    que el cargador las saltaba. CERRADA en S135: el catalogo derivado ya les fija
    aula, no queda ninguna plaza marcada y la carga envia las 219 actividades.

    PROCEDENCIA DE ESE DATO, que NO es una medicion. Lo dio el jefe de estudios
    del centro en entrevista (S134): las horas de FPB que el horario impreso deja
    sin aula se imparten en el Taller 4 (1FPB) y en el Taller 5 (2FPB), de uso
    exclusivo de FPB y tutoria incluida. NO hay segunda fuente. Los once PDF del
    centro no nombran el Taller 5 en ningun sitio y dejan la rejilla del Taller 4
    entera en blanco (medido en S135); eso no contradice la regla, pero tampoco
    la prueba.

TRATAMIENTO DE ERRORES
    Cualquier respuesta que no sea 2xx es FATAL: se para en seco, se vuelca la
    peticion completa (metodo, ruta, cuerpo) y la respuesta cruda, y se sale con
    codigo distinto de cero.
    La razon de no clasificar el error y seguir es que el motivo del rechazo NO
    llega al cliente: el reason phrase viaja vacio y el cuerpo del error no trae
    'message'. Con la prevalidacion en verde no deberia producirse ningun 400; si
    se produce, es un fallo del cargador y no un caso a tratar. El diagnostico se
    lee del stdout de la aplicacion, que es donde si aparece.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
CATALOGO = RAIZ / "docs" / "horario-referencia" / "catalogo-derivado.json"
NOMBRES = RAIZ / "docs" / "horario-referencia" / "nombres-derivados.json"

PATRONES_TEMPORALES = {"DISTRIBUIDA", "AGRUPADA", "NEUTRA"}
TIPOS_AULA = {"ORDINARIA", "LAB_CIENCIAS", "INFORMATICA", "TALLER_TEC",
              "TALLER_PLASTICA", "GIMNASIO", "PISTA", "TALLER_FPB", "COMUN"}
TIPOS_GRUPO = {"ORDINARIO", "DIVERSIFICACION_PDC", "VIRTUAL_OPTATIVA"}
ROLES_TUTORIA = {"TUTOR_PRINCIPAL", "CO_TUTOR"}
SUFIJO_SUBGRUPO_PDC = "-Completo"

# Escrituras HTTP de una carga completa desde una base vacia:
# 1 jornada + 8 niveles + 100 asignaturas + 59 profesores + 44 aulas + 23 grupos
# + 5 PDC + 28 tutorias + 329 subgrupos + 219 actividades.
# SE MANTIENE A MANO A PROPOSITO. No debe derivarse del catalogo: su valor esta
# justamente en ser un oraculo INDEPENDIENTE de el. Derivada seria una tautologia
# incapaz de detectar nunca un error del propio catalogo; a mano, cualquier
# divergencia entre lo previsto y lo escrito sale en el informe final.
ESCRITURAS_CARGA_COMPLETA = 816

FAMILIAS = ["jornada", "niveles", "asignaturas", "profesores", "aulas", "grupos",
            "pdc", "tutorias", "subgrupos", "actividades", "plazas"]


# --------------------------------------------------------------------- HTTP

class ErrorFatal(Exception):
    """Respuesta no-2xx. Trae el volcado entero para que no haya que adivinar."""


class Cliente:
    def __init__(self, base_url):
        self.base = base_url.rstrip("/")
        self.escrituras = 0
        self.lecturas = 0

    def _peticion(self, metodo, ruta, cuerpo=None):
        datos = None if cuerpo is None else json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
        peticion = urllib.request.Request(self.base + ruta, data=datos, method=metodo)
        peticion.add_header("Accept", "application/json")
        if datos is not None:
            peticion.add_header("Content-Type", "application/json; charset=utf-8")
        try:
            with urllib.request.urlopen(peticion) as respuesta:
                crudo = respuesta.read().decode("utf-8")
                return json.loads(crudo) if crudo.strip() else None
        except urllib.error.HTTPError as e:
            crudo = e.read().decode("utf-8", errors="replace")
            raise ErrorFatal(
                "\n".join([
                    "RESPUESTA NO 2xx. Se para en seco y no se envia nada mas.",
                    "  peticion: %s %s" % (metodo, ruta),
                    "  cuerpo enviado:",
                    "    " + json.dumps(cuerpo, ensure_ascii=False, indent=2).replace("\n", "\n    "),
                    "  estado: %s %r" % (e.code, e.reason),
                    "  cabeceras de respuesta:",
                    "    " + str(e.headers).strip().replace("\n", "\n    "),
                    "  cuerpo de respuesta (crudo):",
                    "    " + (crudo if crudo.strip() else "(vacio)"),
                    "",
                    "El motivo del rechazo no viaja en la respuesta. Miralo en el "
                    "stdout de la aplicacion.",
                ])
            ) from e
        except urllib.error.URLError as e:
            raise ErrorFatal(
                "No hay nadie escuchando en %s (%s %s): %s" % (self.base, metodo, ruta, e.reason)
            ) from e

    def get(self, ruta):
        self.lecturas += 1
        return self._peticion("GET", ruta)

    def post(self, ruta, cuerpo):
        self.escrituras += 1
        return self._peticion("POST", ruta, cuerpo)

    def put(self, ruta, cuerpo):
        self.escrituras += 1
        return self._peticion("PUT", ruta, cuerpo)


# ------------------------------------------------------------- prevalidacion

def prevalidar(catalogo, nombres):
    """Valida el catalogo en seco. Devuelve la lista de violaciones.

    Cada violacion es (familia, sujeto, motivo). No se envia nada: esta funcion
    no toca la red.
    """
    v = []

    def fallo(familia, sujeto, motivo):
        v.append((familia, sujeto, motivo))

    niveles = {n["codigo"] for n in catalogo["niveles"]}
    asignaturas = {a["codigo"] for a in catalogo["asignaturas"]}
    profesores = {p["codigo"] for p in catalogo["profesores"]}
    aulas = {a["codigo"] for a in catalogo["aulas"]}
    grupos = {g["codigo"]: g for g in catalogo["grupos"]}
    subgrupos = {s["codigo"]: s for s in catalogo["subgrupos"]}

    # ---- jornada
    tramos = catalogo["jornada"]["diaTipo"]
    if not tramos:
        fallo("jornada", "diaTipo", "la jornada no tiene ningun tramo")
    for t in tramos:
        for campo in ("horaInicio", "horaFin", "esLectivo"):
            if t.get(campo) is None:
                fallo("jornada", "tramo %s" % t.get("orden"), "falta el campo %s" % campo)

    # ---- niveles
    for n in catalogo["niveles"]:
        if not n.get("codigo"):
            fallo("niveles", repr(n), "codigo vacio")
        if not isinstance(n.get("orden"), int):
            fallo("niveles", n.get("codigo"), "orden no es entero")

    # ---- asignaturas y profesores: nombre disponible en nombres-derivados.json
    for familia, codigos in (("asignaturas", asignaturas), ("profesores", profesores)):
        for codigo in sorted(codigos):
            if codigo not in nombres[familia]:
                fallo(familia, codigo, "sin entrada en nombres-derivados.json")

    # ---- aulas: enum de tipo
    for a in catalogo["aulas"]:
        if a.get("tipo") not in TIPOS_AULA:
            fallo("aulas", a.get("codigo"), "tipo de aula fuera del enum: %r" % a.get("tipo"))

    # ---- grupos: enum de tipo, nivel existente, padre existente y ORDINARIO
    for g in catalogo["grupos"]:
        if g.get("tipo") not in TIPOS_GRUPO:
            fallo("grupos", g.get("codigo"), "tipo de grupo fuera del enum: %r" % g.get("tipo"))
        if g.get("nivel") not in niveles:
            fallo("grupos", g.get("codigo"), "nivel inexistente: %r" % g.get("nivel"))
        padre = g.get("grupoPadre")
        if g.get("tipo") == "DIVERSIFICACION_PDC":
            if padre not in grupos:
                fallo("pdc", g.get("codigo"), "grupoPadre inexistente: %r" % padre)
            elif grupos[padre]["tipo"] != "ORDINARIO":
                fallo("pdc", g.get("codigo"), "el grupoPadre %s no es ORDINARIO" % padre)
        elif padre is not None:
            fallo("grupos", g.get("codigo"), "un grupo no PDC no puede tener grupoPadre")

    # ---- tutorias: grupo y profesor existentes, rol del enum, un TUTOR_PRINCIPAL por grupo
    principales = {}
    for t in catalogo["tutorias"]:
        grupo = t.get("grupo")
        if grupo not in grupos:
            fallo("tutorias", grupo, "grupo inexistente")
        if t.get("tutorPrincipal") not in profesores:
            fallo("tutorias", grupo, "profesor inexistente: %r" % t.get("tutorPrincipal"))
        if t.get("rol") not in ROLES_TUTORIA:
            fallo("tutorias", grupo, "rol fuera del enum: %r" % t.get("rol"))
        if t.get("rol") == "TUTOR_PRINCIPAL":
            principales[grupo] = principales.get(grupo, 0) + 1
    for grupo, cuenta in sorted(principales.items()):
        if cuenta > 1:
            fallo("tutorias", grupo, "I4: %d TUTOR_PRINCIPAL en el mismo grupo" % cuenta)

    # ---- subgrupos: al menos un grupo, grupos existentes, y regla del mono-Di
    for s in catalogo["subgrupos"]:
        pobla = s.get("grupos") or []
        if not pobla:
            fallo("subgrupos", s.get("codigo"), "un subgrupo necesita al menos un grupo")
        for g in pobla:
            if g not in grupos:
                fallo("subgrupos", s.get("codigo"), "grupo inexistente: %r" % g)

    pdcs = sorted(g["codigo"] for g in catalogo["grupos"] if g["tipo"] == "DIVERSIFICACION_PDC")
    automaticos = sorted(s["codigo"] for s in catalogo["subgrupos"]
                         if s.get("creadoAutomaticamentePorPDC"))
    esperados = sorted(c + SUFIJO_SUBGRUPO_PDC for c in pdcs)
    if automaticos != esperados:
        fallo("subgrupos", "creadoAutomaticamentePorPDC",
              "los marcados como automaticos %s no son los derivados de los PDC %s"
              % (automaticos, esperados))
    for pdc in pdcs:
        codigo = pdc + SUFIJO_SUBGRUPO_PDC
        s = subgrupos.get(codigo)
        if s is None:
            fallo("subgrupos", codigo, "falta el subgrupo mono-Di del PDC %s" % pdc)
        elif (s.get("grupos") or []) != [pdc]:
            fallo("subgrupos", codigo,
                  "la poblacion del mono-Di debe ser solo su PDC, y es %s" % s.get("grupos"))

    # ---- actividades y plazas
    for a in catalogo["actividades"]:
        codigo = a.get("codigo")
        if not codigo:
            fallo("actividades", repr(a), "codigo vacio")
        if a.get("asignatura") is not None and a["asignatura"] not in asignaturas:
            fallo("actividades", codigo, "asignatura inexistente: %r" % a.get("asignatura"))
        if not isinstance(a.get("duracionTramos"), int) or a["duracionTramos"] < 1:
            fallo("actividades", codigo, "duracionTramos debe ser >= 1, es %r" % a.get("duracionTramos"))
        if not isinstance(a.get("repeticionesPorSemana"), int) or a["repeticionesPorSemana"] < 1:
            fallo("actividades", codigo,
                  "repeticionesPorSemana debe ser >= 1, es %r" % a.get("repeticionesPorSemana"))
        if a.get("patronTemporal") not in PATRONES_TEMPORALES:
            fallo("actividades", codigo, "patronTemporal fuera del enum: %r" % a.get("patronTemporal"))
        plazas = a.get("plazas") or []
        if not plazas:
            fallo("actividades", codigo, "una actividad necesita al menos una plaza")

        vistos = set()
        for i, p in enumerate(plazas):
            sujeto = "%s plaza %d" % (codigo, i + 1)
            if not p.get("asignatura"):
                fallo("plazas", sujeto, "la asignatura de la plaza es obligatoria")
            elif p["asignatura"] not in asignaturas:
                fallo("plazas", sujeto, "asignatura inexistente: %r" % p["asignatura"])

            tiene_fija = bool(p.get("aulaFija"))
            candidatas = p.get("aulasCandidatas") or []
            if tiene_fija and candidatas:
                fallo("plazas", sujeto, "XOR de aula: tiene aula fija y aulas candidatas a la vez")
            if not tiene_fija and not candidatas:
                fallo("plazas", sujeto, "XOR de aula: necesita aula fija o al menos un aula candidata")
            if tiene_fija and p["aulaFija"] not in aulas:
                fallo("plazas", sujeto, "aulaFija inexistente: %r" % p["aulaFija"])
            for aula in candidatas:
                if aula not in aulas:
                    fallo("plazas", sujeto, "aula candidata inexistente: %r" % aula)

            docentes = p.get("profesores") or []
            if not docentes:
                fallo("plazas", sujeto, "I7: una plaza necesita al menos un profesor")
            for prof in docentes:
                if prof not in profesores:
                    fallo("plazas", sujeto, "profesor inexistente: %r" % prof)

            for sg in (p.get("subgrupos") or []):
                if sg not in subgrupos:
                    fallo("plazas", sujeto, "subgrupo inexistente: %r" % sg)
                if sg in vistos:
                    fallo("plazas", sujeto,
                          "I2: el subgrupo %s aparece en mas de una plaza de la actividad" % sg)
                vistos.add(sg)
    return v


def informar_prevalidacion(violaciones):
    print("PREVALIDACION EN SECO (no se ha enviado nada)")
    print("  familias comprobadas: %s" % ", ".join(FAMILIAS))
    print("  violaciones: %d" % len(violaciones))
    por_familia = {}
    for familia, _, _ in violaciones:
        por_familia[familia] = por_familia.get(familia, 0) + 1
    for familia in FAMILIAS:
        if familia in por_familia:
            print("    %-12s %d" % (familia, por_familia[familia]))
    for familia, sujeto, motivo in violaciones:
        print("    [%s] %s: %s" % (familia, sujeto, motivo))
    return por_familia


def prevalidacion_limpia(violaciones):
    """True si la prevalidacion no ha encontrado NINGUNA violacion.

    Hasta S135 el criterio era otro: se toleraban 11 violaciones concretas -las
    plazas de FPB sin aula en el volcado- y la carga seguia adelante omitiendo
    esas 11 actividades. Cerrada esa omision (ver OMISIONES DELIBERADAS en la
    cabecera), el unico estado aceptable es CERO. Cualquier violacion, sea del
    XOR de aula o de cualquier otra regla, aborta la carga sin enviar nada: no
    hay ya un conjunto de fallos conocidos que merezca pasar.

    No recibe el catalogo. El criterio es el recuento, no una expectativa
    derivada de lo que el catalogo contenga.
    """
    if violaciones:
        return False, "hay %d violacion(es) y el unico estado aceptable es cero" % len(violaciones)
    return True, "sin violaciones"


# -------------------------------------------------------------------- carga

def mapa_por_codigo(listado):
    return {x["codigo"]: x["id"] for x in listado}


def cargar(cliente, catalogo, nombres):
    enviados = {}
    omitidos = {}

    # ---- jornada: recurso singleton, se detecta por "persistida"
    jornada = cliente.get("/api/jornada")
    if jornada.get("persistida"):
        print("  jornada: ya persistida, no se toca")
        enviados["jornada"] = 0
    else:
        cliente.put("/api/jornada", {"tramos": [
            {"horaInicio": t["horaInicio"], "horaFin": t["horaFin"], "esLectivo": t["esLectivo"]}
            for t in catalogo["jornada"]["diaTipo"]
        ]})
        enviados["jornada"] = 1
        print("  jornada: 1 PUT con %d tramos" % len(catalogo["jornada"]["diaTipo"]))

    # ---- niveles
    existentes = mapa_por_codigo(cliente.get("/api/niveles"))
    n = 0
    for nivel in catalogo["niveles"]:
        if nivel["codigo"] in existentes:
            continue
        cliente.post("/api/niveles", {"codigo": nivel["codigo"], "orden": nivel["orden"]})
        n += 1
    enviados["niveles"] = n
    print("  niveles: %d altas (%d ya estaban)" % (n, len(existentes)))

    # ---- asignaturas
    existentes = mapa_por_codigo(cliente.get("/api/asignaturas"))
    n = 0
    for a in catalogo["asignaturas"]:
        if a["codigo"] in existentes:
            continue
        cliente.post("/api/asignaturas", {
            "codigo": a["codigo"],
            "nombreCompleto": nombres["asignaturas"][a["codigo"]]["nombreCompleto"],
        })
        n += 1
    enviados["asignaturas"] = n
    print("  asignaturas: %d altas (%d ya estaban)" % (n, len(existentes)))

    # ---- profesores
    existentes = mapa_por_codigo(cliente.get("/api/profesores"))
    n = 0
    for p in catalogo["profesores"]:
        if p["codigo"] in existentes:
            continue
        cliente.post("/api/profesores", {
            "codigo": p["codigo"],
            "nombreCompleto": nombres["profesores"][p["codigo"]]["nombreCompleto"],
        })
        n += 1
    enviados["profesores"] = n
    print("  profesores: %d altas (%d ya estaban)" % (n, len(existentes)))

    # ---- aulas
    existentes = mapa_por_codigo(cliente.get("/api/aulas"))
    n = 0
    for a in catalogo["aulas"]:
        if a["codigo"] in existentes:
            continue
        cliente.post("/api/aulas", {
            "codigo": a["codigo"],
            "tipo": a["tipo"],
            "capacidad": a.get("capacidad"),
            "edificio": a.get("edificio"),
            "planta": a.get("planta"),
            "sector": a.get("sector"),
        })
        n += 1
    enviados["aulas"] = n
    print("  aulas: %d altas (%d ya estaban)" % (n, len(existentes)))

    # ---- grupos ORDINARIO y PDC: un unico listado sirve para las dos familias
    existentes = mapa_por_codigo(cliente.get("/api/grupos"))
    n = 0
    for g in catalogo["grupos"]:
        if g["tipo"] != "ORDINARIO" or g["codigo"] in existentes:
            continue
        creado = cliente.post("/api/grupos", {
            "codigo": g["codigo"], "nivel": g["nivel"], "tipo": g["tipo"],
        })
        existentes[creado["codigo"]] = creado["id"]
        n += 1
    enviados["grupos"] = n
    print("  grupos ORDINARIO: %d altas" % n)

    n = 0
    for g in catalogo["grupos"]:
        if g["tipo"] != "DIVERSIFICACION_PDC" or g["codigo"] in existentes:
            continue
        creado = cliente.post("/api/grupos/%d/pdc" % existentes[g["grupoPadre"]],
                              {"codigo": g["codigo"]})
        existentes[creado["codigo"]] = creado["id"]
        n += 1
    enviados["pdc"] = n
    print("  PDC: %d altas (cada una crea ademas su subgrupo %s)" % (n, "{codigo}" + SUFIJO_SUBGRUPO_PDC))

    # ---- tutorias: PUT de reemplazo total, se envian SIEMPRE
    for t in catalogo["tutorias"]:
        cliente.put("/api/grupos/%d/tutoria" % existentes[t["grupo"]],
                    [{"profesor": t["tutorPrincipal"], "rol": t["rol"]}])
    enviados["tutorias"] = len(catalogo["tutorias"])
    print("  tutorias: %d PUT (reemplazo total, idempotente)" % enviados["tutorias"])

    # ---- subgrupos: se omiten los mono-Di, que ya los creo el alta del PDC
    presentes = mapa_por_codigo(cliente.get("/api/subgrupos"))
    n = 0
    omitidos_sg = []
    for s in catalogo["subgrupos"]:
        if s.get("creadoAutomaticamentePorPDC"):
            omitidos_sg.append(s["codigo"])
            continue
        if s["codigo"] in presentes:
            continue
        cliente.post("/api/subgrupos", {"codigo": s["codigo"], "grupos": list(s["grupos"])})
        n += 1
    enviados["subgrupos"] = n
    omitidos["subgrupos"] = omitidos_sg
    print("  subgrupos: %d altas, %d omitidos por creadoAutomaticamentePorPDC" % (n, len(omitidos_sg)))

    # ---- actividades: se envian todas. Hasta S135 se saltaban las que llevaban
    # alguna plaza con _aulaDesconocida; hoy no queda ninguna y un marcador
    # superviviente debe ABORTAR en la prevalidacion, no colarse como omision.
    presentes = mapa_por_codigo(cliente.get("/api/actividades"))
    n = 0
    plazas_enviadas = 0
    for a in catalogo["actividades"]:
        if a["codigo"] in presentes:
            continue
        cliente.post("/api/actividades", {
            "codigo": a["codigo"],
            "asignatura": a.get("asignatura"),
            "duracionTramos": a["duracionTramos"],
            "repeticionesPorSemana": a["repeticionesPorSemana"],
            "patronTemporal": a["patronTemporal"],
            "requiereTutor": a["requiereTutor"],
            "plazas": [{
                "asignatura": p["asignatura"],
                "aulaFija": p.get("aulaFija"),
                "aulasCandidatas": list(p.get("aulasCandidatas") or []),
                "profesores": list(p.get("profesores") or []),
                "subgrupos": list(p.get("subgrupos") or []),
            } for p in a["plazas"]],
        })
        plazas_enviadas += len(a["plazas"])
        n += 1
    enviados["actividades"] = n
    enviados["plazas"] = plazas_enviadas
    print("  actividades: %d altas (%d plazas)" % (n, plazas_enviadas))

    return enviados, omitidos


# ------------------------------------------------------------------ informe

def informe_final(cliente, catalogo, enviados, omitidos):
    """Relee el estado por GET y lo confronta con lo esperado.

    Estos GET son de VERIFICACION y van aparte de los ocho listados que usa la
    idempotencia: no deciden que se envia, solo comprueban que lo enviado esta.
    """
    print()
    print("INFORME FINAL (leido por GET tras la carga)")

    esperado = {
        "niveles": len(catalogo["niveles"]),
        "asignaturas": len(catalogo["asignaturas"]),
        "profesores": len(catalogo["profesores"]),
        "aulas": len(catalogo["aulas"]),
        "grupos": len(catalogo["grupos"]),
        "subgrupos": len(catalogo["subgrupos"]),
        "actividades": len(catalogo["actividades"]),
        "plazas": sum(len(a["plazas"]) for a in catalogo["actividades"]),
        "tutorias": len(catalogo["tutorias"]),
    }

    grupos = cliente.get("/api/grupos")
    actividades = cliente.get("/api/actividades")
    leido = {
        "niveles": len(cliente.get("/api/niveles")),
        "asignaturas": len(cliente.get("/api/asignaturas")),
        "profesores": len(cliente.get("/api/profesores")),
        "aulas": len(cliente.get("/api/aulas")),
        "grupos": len(grupos),
        "subgrupos": len(cliente.get("/api/subgrupos")),
        "actividades": len(actividades),
        "plazas": sum(len(a["plazas"]) for a in actividades),
        "tutorias": sum(len(cliente.get("/api/grupos/%d/tutoria" % g["id"])) for g in grupos),
    }

    print("  %-13s %9s %9s   %s" % ("familia", "esperado", "leido", ""))
    desajustes = 0
    for familia in ("niveles", "asignaturas", "profesores", "aulas", "grupos",
                    "subgrupos", "actividades", "plazas", "tutorias"):
        marca = "OK" if esperado[familia] == leido[familia] else "DESAJUSTE"
        if marca != "OK":
            desajustes += 1
        print("  %-13s %9d %9d   %s" % (familia, esperado[familia], leido[familia], marca))
    # Las 816 escrituras son las de una carga completa desde vacio. En una corrida
    # idempotente sobre un centro ya poblado solo quedan los PUT de tutoria, que se
    # envian siempre: eso no es un desajuste.
    if cliente.escrituras == ESCRITURAS_CARGA_COMPLETA:
        marca_escrituras = "OK"
    elif cliente.escrituras == enviados["tutorias"]:
        marca_escrituras = "corrida idempotente: solo los %d PUT de tutoria" % enviados["tutorias"]
    else:
        marca_escrituras = "DISTINTO DE LO PREVISTO"
        desajustes += 1
    print("  %-13s %9d %9d   %s"
          % ("escrituras", ESCRITURAS_CARGA_COMPLETA, cliente.escrituras, marca_escrituras))
    print("  (lecturas GET en toda la corrida: %d)" % cliente.lecturas)

    print()
    print("  OMITIDOS")
    print("    subgrupos creados por el alta de los PDC (%d): %s"
          % (len(omitidos.get("subgrupos", [])), ", ".join(omitidos.get("subgrupos", []))))

    # ---- catalogo que se carga pero no lo usa ninguna actividad. AVISO, no error.
    # Solo se imprime si hay algo que decir: en una carga sana no sale nada.
    usadas_asig, usados_prof = set(), set()
    for a in catalogo["actividades"]:
        if a.get("asignatura"):
            usadas_asig.add(a["asignatura"])
        for p in a["plazas"]:
            if p.get("asignatura"):
                usadas_asig.add(p["asignatura"])
            usados_prof.update(p.get("profesores") or [])
    huerfanas = sorted({a["codigo"] for a in catalogo["asignaturas"]} - usadas_asig)
    huerfanos = sorted({p["codigo"] for p in catalogo["profesores"]} - usados_prof)
    if huerfanas or huerfanos:
        print()
        print("  AVISO (no es un error): quedan cargados sin aparecer en ninguna actividad:")
        if huerfanas:
            print("    %d asignaturas: %s" % (len(huerfanas), ", ".join(huerfanas)))
        if huerfanos:
            print("    %d profesores:  %s" % (len(huerfanos), ", ".join(huerfanos)))
    return desajustes


# --------------------------------------------------------------------- main

def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--base-url", default="http://localhost:8080")
    modo = parser.add_mutually_exclusive_group()
    modo.add_argument("--prevalidar", action="store_true",
                      help="solo valida en seco e informa (por defecto)")
    modo.add_argument("--cargar", action="store_true", help="ejecuta la carga")
    args = parser.parse_args()

    catalogo = json.loads(CATALOGO.read_text(encoding="utf-8"))
    if not NOMBRES.is_file():
        raise SystemExit("Falta %s. Ejecuta antes tools/carga-centro/extraer-nombres.py"
                         % NOMBRES.relative_to(RAIZ))
    nombres = json.loads(NOMBRES.read_text(encoding="utf-8"))

    violaciones = prevalidar(catalogo, nombres)
    informar_prevalidacion(violaciones)
    limpia, razon = prevalidacion_limpia(violaciones)
    print("  veredicto: %s" % razon)

    if not args.cargar:
        print()
        print("Modo prevalidacion: no se ha enviado nada. Usa --cargar para poblar.")
        return 0 if limpia else 1

    if not limpia:
        print()
        print("ABORTA: la prevalidacion no esta limpia. No se envia nada.")
        print("Violaciones observadas (%d):" % len(violaciones))
        for familia, sujeto, motivo in violaciones:
            print("  [%s] %s: %s" % (familia, sujeto, motivo))
        return 1

    print()
    print("CARGA contra %s" % args.base_url)
    cliente = Cliente(args.base_url)
    try:
        enviados, omitidos = cargar(cliente, catalogo, nombres)
        desajustes = informe_final(cliente, catalogo, enviados, omitidos)
    except ErrorFatal as e:
        print()
        print(str(e))
        return 2
    return 0 if desajustes == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
