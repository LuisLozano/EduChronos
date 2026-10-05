#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Vuelca la vista de GRUPOS de los PDF del programa de horarios del centro al esquema de S115.

Nace en S196 (C-volcado-grupos, condición 1 de O-carga-2026). El extractor de S115 no se
versionó; su método está en docs/horario-referencia/RESUMEN-EXTRACCION.md y aquí se rehace
con dos cambios que pide el material de 2026/2027: las columnas se leen de la cabecera de
cada página (el paso pasó de 72 a 69) y los tramos salen del VALOR de la etiqueta de hora
(la reimpresión del 25/09 no imprime la franja del recreo).

USO
    python3 tools/carga-centro/extraer-horario.py --modo grupos --salida <dir ABSOLUTO> <pdf1> [<pdf2> ...]
    python3 tools/carga-centro/extraer-horario.py --modo profesores --salida <dir ABSOLUTO> <pdf1> [...]

    --salida debe ser absoluta y no existir o estar vacía: si tiene algo, aborta sin escribir.
    Cuando dos PDF traen el mismo grupo (por el título «Grupo: ...»), gana el que va DESPUÉS
    en la línea de órdenes. La composición es por título, nunca por número de página.

LECTURA. `pdftotext -bbox` (poppler), una llamada por PDF; solo biblioteca estándar. Los
textos que se buscan se derivaron del volcado de la página 1 de P05 (S196, M2):
    título           «Grupo:» y el resto de palabras de su línea
    cabeceras de día Lunes · Martes · Miérc. · Jueves · Viernes
    horas            H:MM en el margen izquierdo, por pares inicio/fin de tramo
    leyendas         «Profesores:» y «Asignaturas:», en dos columnas código + nombre

REJILLA
  - Columnas: el centro de cada cabecera de día de la página; una palabra va a la columna
    cuyo tramo [punto medio, punto medio] contiene su centro. Las columnas de los extremos
    se cierran con medio paso. Nada de x fijas.
  - Filas: por el valor de la hora de inicio de cada par de etiquetas (TRAMOS). El 11:00 es
    el recreo y se salta por su valor. Una etiqueta desconocida aborta.
  - Dentro de la celda, cada línea con palabra a la derecha del centro de la columna es una
    entrada: asignatura a la izquierda (código de la leyenda «Asignaturas:») y profesor a la
    derecha (código de «Profesores:»; si fueran varios, se unen con «/», convención de S115).
    Una línea con una sola palabra a la izquierda justo debajo de una entrada es su aula.
    Cualquier otra cosa no clasifica: no se adivina, queda como token PERDIDO.
  - Página sin cabeceras de día ni horas y con el mismo «Grupo:» que la anterior: es la
    continuación de su leyenda. Sus códigos se AÑADEN a la leyenda de la anterior, a la
    sección en que esta acabó, antes de clasificar. No genera fichero.

SALIDA. Un grupo-<código>.json por grupo con el esquema de S115 (_meta {fuente, pagina,
codigo_crudo, modo}; celdas [{dia, tramo, asignatura, profesor, aula, confianza, nota}]), y
un informe por stdout. rc=0 solo con 0 perdidos y 0 duplicados; si no, no escribe nada.
rc=1 por cuadre, rc=2 por uso o estructura de página no reconocida.

MODO PROFESORES (S197, C-volcado-profesores). La vista de profesores titula «Profesor: ...»,
no trae leyenda «Profesores:» y su celda es «asignatura aula» en la primera línea y los grupos
en las siguientes. Mismas columnas, tramos, leyenda de asignaturas, composición por título y
salida que el modo grupos; cambian la página y la clasificación:
  - Bandas: la x de cada palabra relativa al borde izquierdo de su columna. Sobre TODAS las
    palabras de rejilla del PDF, los valores se agrupan por huecos de más de HUECO_BANDAS;
    deben salir dos bandas y el umbral es el punto medio del hueco. Nada de x fijas.
  - Celda (líneas por y): línea 1, banda izquierda, exactamente una palabra = asignatura, que
    debe estar en la leyenda «Asignaturas:» de su página; línea 1, banda derecha, como mucho
    una: si empieza por dígito es grupo, si no, aula; líneas 2 y siguientes: grupos, y todos
    deben empezar por dígito. Lo que no cumple aborta (rc=2): no se adivina.
  - La fila del recreo se clasifica igual y va a «recreo», sin tramo.
  - profesor-<título>.json: _meta como en grupos con modo «profesores»; celdas [{dia, tramo,
    asignatura, grupos, aula, confianza, nota}] y recreo [{dia, asignatura, grupos, aula,
    confianza, nota}]. «grupos» es una cadena separada por espacios, como en los aula-*.json
    de S115; sin grupo, «»; sin aula, null.
"""
import argparse
import html
import json
import os
import re
import shutil
import subprocess
import sys
from collections import Counter

TITULO = "Grupo:"
TITULO_PROF = "Profesor:"
DIAS = ("Lunes", "Martes", "Miérc.", "Jueves", "Viernes")
LEY_PROF = "Profesores:"
LEY_ASIG = "Asignaturas:"
HORA = re.compile(r"^\d{1,2}:\d{2}$")
# Hora de inicio -> (tramo, hora de fin). None es el recreo.
TRAMOS = {
    "8:00": (1, "9:00"),
    "9:00": (2, "10:00"),
    "10:00": (3, "11:00"),
    "11:00": (None, "11:30"),
    "11:30": (4, "12:30"),
    "12:30": (5, "13:30"),
    "13:30": (6, "14:30"),
}
TOL_LINEA = 1.0       # misma línea: tops a menos de esto
MARGEN_FILA = 2.0     # la primera línea de un tramo está a la altura de su etiqueta
# Modo profesores: hueco mínimo entre bandas de x relativa a la columna. En P07 (S197,
# m2bis/q1-xrel.tsv) las 3226 palabras de rejilla caen en los bins de 5 y 40 pt y nada entre
# medias; 3 pt separa las bandas sin partir ninguna.
HUECO_BANDAS = 3.0

PAGINA_RE = re.compile(r"<page\b[^>]*>(.*?)</page>", re.S)
PALABRA_RE = re.compile(
    r'<word xMin="([-\d.]+)" yMin="([-\d.]+)" xMax="([-\d.]+)" yMax="([-\d.]+)">(.*?)</word>', re.S)


class ErrorExtraccion(Exception):
    """Estructura de página no reconocida: aborta con rc=2."""


# ------------------------------------------------------------------ lectura

def version_poppler():
    r = subprocess.run(["pdftotext", "-v"], capture_output=True, text=True)
    return (r.stderr or r.stdout).splitlines()[0].strip()


def palabras_de_bbox(texto):
    """Parsea la salida de `pdftotext -bbox`: lista de páginas, cada una lista de palabras."""
    paginas = []
    for n, cuerpo in enumerate(PAGINA_RE.findall(texto), start=1):
        paginas.append([
            {"id": (n, i), "text": html.unescape(t), "x0": float(a), "top": float(b),
             "x1": float(c), "bottom": float(d)}
            for i, (a, b, c, d, t) in enumerate(PALABRA_RE.findall(cuerpo))])
    return paginas


def leer_pdf(ruta):
    r = subprocess.run(["pdftotext", "-bbox", ruta, "-"], capture_output=True)
    if r.returncode != 0:
        raise ErrorExtraccion("pdftotext -bbox rc=%d sobre %s: %s"
                              % (r.returncode, ruta, r.stderr.decode("utf-8", "replace").strip()))
    return palabras_de_bbox(r.stdout.decode("utf-8"))


# ------------------------------------------------------------------ geometría

def lineas(palabras):
    """Agrupa por top (tolerancia TOL_LINEA); cada línea, ordenada por x0."""
    res = []
    for w in sorted(palabras, key=lambda w: (w["top"], w["x0"])):
        if res and abs(w["top"] - res[-1][0]["top"]) <= TOL_LINEA:
            res[-1].append(w)
        else:
            res.append([w])
    return [sorted(l, key=lambda w: w["x0"]) for l in res]


def centro_x(w):
    return (w["x0"] + w["x1"]) / 2


def titulo_de(lins, marca=TITULO):
    for l in lins:
        for i, w in enumerate(l):
            if w["text"] == marca:
                return " ".join(x["text"] for x in l[i + 1:]), l
    return None, None


def cabeceras(palabras):
    """Centros de las cinco cabeceras de día, o None si la página no tiene ninguna."""
    vistas = {}
    for w in palabras:
        if w["text"] in DIAS:
            if w["text"] in vistas:
                raise ErrorExtraccion("cabecera %r repetida en la página" % w["text"])
            vistas[w["text"]] = w
    if not vistas:
        return None
    if len(vistas) != len(DIAS):
        raise ErrorExtraccion("faltan cabeceras de día: %s" % sorted(set(DIAS) - set(vistas)))
    return [vistas[d] for d in DIAS]


def limites_columnas(centros):
    """[(izq, der)] por día: puntos medios entre centros; los extremos, medio paso."""
    medios = [(centros[i] + centros[i + 1]) / 2 for i in range(len(centros) - 1)]
    izq = [centros[0] - (centros[1] - centros[0]) / 2] + medios
    der = medios + [centros[-1] + (centros[-1] - centros[-2]) / 2]
    return list(zip(izq, der))


def columna_de(w, limites):
    c = centro_x(w)
    for i, (a, b) in enumerate(limites):
        if a <= c < b:
            return i + 1
    return None


def filas(etiquetas):
    """Pares (inicio, fin) de etiquetas de hora -> [(top de inicio, tramo o None)]."""
    if len(etiquetas) % 2:
        raise ErrorExtraccion("número impar de etiquetas de hora: %s" % [w["text"] for w in etiquetas])
    res = []
    for i in range(0, len(etiquetas), 2):
        ini, fin = etiquetas[i], etiquetas[i + 1]
        if ini["text"] not in TRAMOS:
            raise ErrorExtraccion("etiqueta de hora desconocida: %r" % ini["text"])
        tramo, fin_esperado = TRAMOS[ini["text"]]
        if fin["text"] != fin_esperado:
            raise ErrorExtraccion("tramo %s-%s: se esperaba fin %s" % (ini["text"], fin["text"], fin_esperado))
        res.append((ini["top"], tramo))
    return res


def tramo_de(w, filas_):
    """(True, tramo) de la última fila cuyo inicio queda por encima de w; el tramo es None en
    el recreo. (False, None) si w está por encima de la primera fila."""
    res = (False, None)
    for top, t in filas_:
        if w["top"] >= top - MARGEN_FILA:
            res = (True, t)
    return res


# ------------------------------------------------------------------ leyenda

def secciones_de_leyenda(lins):
    """Líneas de leyenda de un grupo (las de su página y las de sus continuaciones, en orden)
    -> ([(sección, línea de contenido)], nº de palabras de leyenda). Las cabeceras
    «Profesores:» y «Asignaturas:» abren sección; la continuación sigue en la que estaba."""
    seccion = None
    contenido = []
    n = 0
    for l in lins:
        textos = [w["text"] for w in l]
        n += len(l)
        if LEY_PROF in textos or LEY_ASIG in textos:
            if len(l) != 1:
                raise ErrorExtraccion("cabecera de leyenda con más texto en su línea: %s" % textos)
            seccion = "prof" if textos[0] == LEY_PROF else "asig"
            continue
        if seccion is None:
            raise ErrorExtraccion("línea de leyenda antes de «%s» o «%s»: %s" % (LEY_PROF, LEY_ASIG, textos))
        contenido.append((seccion, l))
    return contenido, n


def columnas_de_codigo(contenido):
    """Cada línea lleva una o dos parejas «código nombre» en columnas fijas. Sobre TODAS las
    líneas de leyenda de un PDF, las columnas son las x0 presentes en más de la mitad de las
    líneas: deben salir cuatro (código, nombre, código, nombre) o aborta. Con las de un solo
    grupo no basta: en uno de pocas líneas, una palabra interna de nombre también alinea."""
    cuenta = Counter()
    for _, l in contenido:
        for x in {round(w["x0"], 1) for w in l}:
            cuenta[x] += 1
    columnas = sorted(x for x, k in cuenta.items() if 2 * k > len(contenido))
    if len(columnas) != 4:
        raise ErrorExtraccion("leyenda: se esperaban 4 columnas alineadas y salen %s" % columnas)
    for _, l in contenido:
        if round(l[0]["x0"], 1) != columnas[0]:
            raise ErrorExtraccion("línea de leyenda que no empieza en la columna de código: %s"
                                  % [w["text"] for w in l])
    return columnas[0], columnas[2]


def codigos_de_leyenda(contenido, x_codigo):
    codigos = {"prof": [], "asig": []}
    for sec, l in contenido:
        codigos[sec].extend(w for w in l if round(w["x0"], 1) in x_codigo)
    return codigos


def nombres_de_leyenda(contenido, x_codigo):
    """Como codigos_de_leyenda, pero código -> nombre: el nombre son las palabras que siguen al
    código hasta el siguiente código de la línea. Un código con dos nombres distintos aborta."""
    nombres = {"prof": {}, "asig": {}}
    for sec, l in contenido:
        cod, nom = None, []
        for w in l + [None]:
            if w is None or round(w["x0"], 1) in x_codigo:
                if cod is not None:
                    previo = nombres[sec].setdefault(cod, " ".join(nom))
                    if previo != " ".join(nom):
                        raise ErrorExtraccion("leyenda: el código %r tiene dos nombres: %r y %r"
                                              % (cod, previo, " ".join(nom)))
                if w is None:
                    break
                cod, nom = w["text"], []
            else:
                nom.append(w["text"])
    return nombres


# ------------------------------------------------------------------ página

def analizar_pagina(palabras):
    """Estructura de una página. tipo: 'rejilla' o 'continuacion'."""
    lins = lineas(palabras)
    titulo, linea_titulo = titulo_de(lins)
    if titulo is None:
        raise ErrorExtraccion("página sin «%s»" % TITULO)
    cab = cabeceras(palabras)
    ids_titulo = {w["id"] for w in linea_titulo}
    if cab is None:
        if any(HORA.match(w["text"]) for w in palabras):
            raise ErrorExtraccion("página con horas y sin cabeceras de día")
        resto = [l for l in lins if l is not linea_titulo]
        return {"tipo": "continuacion", "titulo": titulo, "lineas_leyenda": resto}
    centros = [centro_x(w) for w in cab]
    limites = limites_columnas(centros)
    suelo_cab = max(w["bottom"] for w in cab)
    ley = [w for w in palabras if w["text"] == LEY_PROF]
    if len(ley) != 1:
        raise ErrorExtraccion("página de rejilla con %d «%s»" % (len(ley), LEY_PROF))
    techo_ley = ley[0]["top"]
    banda = [w for w in palabras if w["top"] > suelo_cab and w["top"] < techo_ley - TOL_LINEA]
    etiquetas = sorted((w for w in banda if HORA.match(w["text"]) and w["x1"] < limites[0][0]),
                       key=lambda w: w["top"])
    if not etiquetas:
        raise ErrorExtraccion("página con cabeceras de día y sin etiquetas de hora")
    ids_et = {w["id"] for w in etiquetas}
    rejilla = [w for w in banda if w["id"] not in ids_et]
    lineas_leyenda = [l for l in lins if l[0]["top"] >= techo_ley - TOL_LINEA]
    return {"tipo": "rejilla", "titulo": titulo, "centros": centros, "limites": limites,
            "filas": filas(etiquetas), "rejilla": rejilla, "lineas_leyenda": lineas_leyenda,
            "ids_titulo": ids_titulo}


def analizar_pagina_profesor(palabras):
    """Como analizar_pagina para la vista de profesores: título «Profesor:», sin leyenda
    «Profesores:»; la rejilla acaba en «Asignaturas:» (una página sin clases no la trae)."""
    lins = lineas(palabras)
    titulo, linea_titulo = titulo_de(lins, TITULO_PROF)
    if titulo is None:
        raise ErrorExtraccion("página sin «%s»" % TITULO_PROF)
    cab = cabeceras(palabras)
    if cab is None:
        if any(HORA.match(w["text"]) for w in palabras):
            raise ErrorExtraccion("página con horas y sin cabeceras de día")
        resto = [l for l in lins if l is not linea_titulo]
        return {"tipo": "continuacion", "titulo": titulo, "lineas_leyenda": resto}
    if any(w["text"] == LEY_PROF for w in palabras):
        raise ErrorExtraccion("página de profesor con «%s»" % LEY_PROF)
    centros = [centro_x(w) for w in cab]
    limites = limites_columnas(centros)
    suelo_cab = max(w["bottom"] for w in cab)
    ley = [w for w in palabras if w["text"] == LEY_ASIG]
    if len(ley) > 1:
        raise ErrorExtraccion("página de profesor con %d «%s»" % (len(ley), LEY_ASIG))
    techo_ley = ley[0]["top"] if ley else float("inf")
    banda = [w for w in palabras if w["top"] > suelo_cab and w["top"] < techo_ley - TOL_LINEA]
    etiquetas = sorted((w for w in banda if HORA.match(w["text"]) and w["x1"] < limites[0][0]),
                       key=lambda w: w["top"])
    if not etiquetas:
        raise ErrorExtraccion("página con cabeceras de día y sin etiquetas de hora")
    ids_et = {w["id"] for w in etiquetas}
    rejilla = [w for w in banda if w["id"] not in ids_et]
    lineas_leyenda = [l for l in lins if l[0]["top"] >= techo_ley - TOL_LINEA]
    return {"tipo": "rejilla", "titulo": titulo, "centros": centros, "limites": limites,
            "filas": filas(etiquetas), "rejilla": rejilla, "lineas_leyenda": lineas_leyenda,
            "ids_titulo": {w["id"] for w in linea_titulo}}


def x_relativa(w, limites):
    """(día, x0 menos el borde izquierdo de su columna), o (None, None) fuera de columnas."""
    dia = columna_de(w, limites)
    return (dia, w["x0"] - limites[dia - 1][0]) if dia is not None else (None, None)


def umbral_de_bandas(xs):
    """x relativas de las palabras de rejilla de un PDF -> umbral entre las dos bandas.
    Grupos separados por huecos de más de HUECO_BANDAS: deben salir dos o aborta."""
    xs = sorted(xs)
    if not xs:
        raise ErrorExtraccion("bandas: no hay palabras de rejilla")
    bandas = [[xs[0]]]
    for x in xs[1:]:
        if x - bandas[-1][-1] > HUECO_BANDAS:
            bandas.append([x])
        else:
            bandas[-1].append(x)
    if len(bandas) != 2:
        raise ErrorExtraccion("bandas: se esperaban 2 y salen %d: %s"
                              % (len(bandas), [(round(b[0], 1), round(b[-1], 1)) for b in bandas]))
    return (bandas[0][-1] + bandas[1][0]) / 2


# ------------------------------------------------------------------ clasificación

def clasificar(rejilla, centros, limites, filas_, prof, asig):
    """Asigna palabras de rejilla a celdas. Cada celda: dia, tramo, asignatura, profesor, aula
    (palabras) y la lista de ids que consume. Lo que no clasifica no se asigna."""
    por_celda = {}
    for w in rejilla:
        dia = columna_de(w, limites)
        dentro, tramo = tramo_de(w, filas_)
        if dia is None or not dentro or tramo is None:
            continue  # fuera de columnas, sobre la primera fila o en el recreo: perdido
        por_celda.setdefault((tramo, dia), []).append(w)
    celdas = []
    for (tramo, dia) in sorted(por_celda):
        centro = centros[dia - 1]
        previa = None
        for l in lineas(por_celda[(tramo, dia)]):
            izq = [w for w in l if w["x0"] < centro]
            der = [w for w in l if w["x0"] >= centro]
            actual = None
            if der:
                if len(izq) == 1 and izq[0]["text"] in asig and all(w["text"] in prof for w in der):
                    actual = {"dia": dia, "tramo": tramo, "asignatura": izq[0], "profesor": der,
                              "aula": None}
                    celdas.append(actual)
            elif len(izq) == 1 and previa is not None and previa["aula"] is None:
                previa["aula"] = izq[0]
            previa = actual
    return celdas


def clasificar_profesor(rejilla, limites, filas_, umbral, asig):
    """Modo profesores. Cada celda: dia, tramo (None en el recreo), asignatura, grupos (lista) y
    aula. Lo que no cumple las reglas aborta. Con asig=None no se valida contra la leyenda (lo
    usa parche-aulas.py, que no lee leyendas). Lo que cae fuera de columnas o sobre la primera
    fila no se asigna."""
    por_celda = {}
    for w in rejilla:
        dia = columna_de(w, limites)
        dentro, tramo = tramo_de(w, filas_)
        if dia is None or not dentro:
            continue  # perdido
        por_celda.setdefault((tramo, dia), []).append(w)
    celdas = []
    for (tramo, dia) in sorted(por_celda, key=lambda k: (k[0] or 0, k[1])):
        lins = lineas(por_celda[(tramo, dia)])
        donde = "p%d %s día %d" % (lins[0][0]["id"][0], "recreo" if tramo is None else "tramo %d" % tramo, dia)
        borde = limites[dia - 1][0]
        izq = [w for w in lins[0] if w["x0"] - borde < umbral]
        der = [w for w in lins[0] if w["x0"] - borde >= umbral]
        if len(izq) != 1:
            raise ErrorExtraccion("%s: la línea 1 lleva %d palabras en la banda izquierda: %s"
                                  % (donde, len(izq), [w["text"] for w in izq]))
        if asig is not None and izq[0]["text"] not in asig:
            raise ErrorExtraccion("%s: asignatura %r fuera de la leyenda" % (donde, izq[0]["text"]))
        if len(der) > 1:
            raise ErrorExtraccion("%s: la línea 1 lleva %d palabras en la banda derecha: %s"
                                  % (donde, len(der), [w["text"] for w in der]))
        grupos, aula = [], None
        if der:
            if der[0]["text"][:1].isdigit():
                grupos.append(der[0])
            else:
                aula = der[0]
        for l in lins[1:]:
            for w in l:
                if not w["text"][:1].isdigit():
                    raise ErrorExtraccion("%s: %r en la línea %d no empieza por dígito"
                                          % (donde, w["text"], lins.index(l) + 1))
                grupos.append(w)
        celdas.append({"dia": dia, "tramo": tramo, "asignatura": izq[0], "grupos": grupos, "aula": aula})
    return celdas


def ids_de(celda):
    ids = [celda["asignatura"]["id"]] + [w["id"] for w in celda.get("profesor", []) + celda.get("grupos", [])]
    if celda["aula"] is not None:
        ids.append(celda["aula"]["id"])
    return ids


def cuadrar(palabras_rejilla, celdas):
    """Función pura. Perdidos: palabras de rejilla que ninguna celda consume. Duplicados:
    palabras que consume más de una celda."""
    usos = Counter(i for c in celdas for i in ids_de(c))
    perdidos = [w for w in palabras_rejilla if usos[w["id"]] == 0]
    duplicados = [w for w in palabras_rejilla if usos[w["id"]] > 1]
    return perdidos, duplicados


def a_json(celda):
    return {
        "dia": celda["dia"],
        "tramo": celda["tramo"],
        "asignatura": celda["asignatura"]["text"],
        "profesor": "/".join(w["text"] for w in celda["profesor"]),
        "aula": celda["aula"]["text"] if celda["aula"] is not None else None,
        "confianza": "alta",
        "nota": "",
    }


def a_json_profesor(celda):
    res = {"dia": celda["dia"], "tramo": celda["tramo"], "asignatura": celda["asignatura"]["text"],
           "grupos": " ".join(w["text"] for w in celda["grupos"]),
           "aula": celda["aula"]["text"] if celda["aula"] is not None else None,
           "confianza": "alta", "nota": ""}
    if celda["tramo"] is None:
        del res["tramo"]
    return res


def nombre_fichero(titulo, prefijo="grupo"):
    return "%s-%s.json" % (prefijo, "-".join(titulo.replace("º", "").split()))


# ------------------------------------------------------------------ composición

def procesar(documentos, modo="grupos"):
    """documentos: [(nombre del PDF, [páginas de palabras])], en orden de precedencia creciente.
    modo: «grupos» o «profesores» (cambian el analizador de página y la clasificación).
    Devuelve el resultado completo sin escribir nada."""
    analizar = analizar_pagina_profesor if modo == "profesores" else analizar_pagina
    grupos = {}
    sustituidos = []
    paginas = 0
    for nombre, pags in documentos:
        vistos = {}
        ultimo = None
        del_documento = []
        for n, palabras in enumerate(pags, start=1):
            paginas += 1
            p = analizar(palabras)
            if p["tipo"] == "continuacion":
                if ultimo is None or ultimo["titulo"] != p["titulo"]:
                    raise ErrorExtraccion("%s p%d: página sin rejilla que no continúa a la anterior (%r)"
                                          % (nombre, n, p["titulo"]))
                ultimo["continuaciones"].append(n)
                ultimo["lineas_leyenda"].extend(p["lineas_leyenda"])
                continue
            if p["titulo"] in vistos:
                raise ErrorExtraccion("%s p%d: grupo %r ya visto en p%d"
                                      % (nombre, n, p["titulo"], vistos[p["titulo"]]))
            vistos[p["titulo"]] = n
            p.update({"fuente": nombre, "pagina": n, "continuaciones": []})
            ultimo = p
            del_documento.append(p)
            if p["titulo"] in grupos:
                sustituidos.append((p["titulo"], grupos[p["titulo"]]["fuente"], nombre))
            grupos[p["titulo"]] = p
        for g in del_documento:
            g["leyenda"], g["palabras_leyenda"] = secciones_de_leyenda(g["lineas_leyenda"])
        x_codigo = columnas_de_codigo([c for g in del_documento for c in g["leyenda"]])
        umbral = None
        if modo == "profesores":
            umbral = umbral_de_bandas([x for g in del_documento for w in g["rejilla"]
                                       for d, x in [x_relativa(w, g["limites"])] if d is not None])
        for g in del_documento:
            g["x_codigo"] = x_codigo
            g["umbral"] = umbral
    res = {"paginas": paginas, "grupos": [], "sustituidos": sustituidos, "modo": modo}
    for titulo, g in grupos.items():
        codigos = codigos_de_leyenda(g["leyenda"], g["x_codigo"])
        n_ley = g["palabras_leyenda"]
        prof = {w["text"] for w in codigos["prof"]}
        asig = {w["text"] for w in codigos["asig"]}
        if modo == "profesores":
            celdas = clasificar_profesor(g["rejilla"], g["limites"], g["filas"], g["umbral"], asig)
        else:
            celdas = clasificar(g["rejilla"], g["centros"], g["limites"], g["filas"], prof, asig)
        perdidos, duplicados = cuadrar(g["rejilla"], celdas)
        res["grupos"].append({
            "titulo": titulo, "fuente": g["fuente"], "pagina": g["pagina"],
            "continuaciones": g["continuaciones"],
            "fichero": nombre_fichero(titulo, "profesor" if modo == "profesores" else "grupo"),
            "celdas": celdas, "rejilla": g["rejilla"], "codigos": codigos, "palabras_leyenda": n_ley,
            "nombres": nombres_de_leyenda(g["leyenda"], g["x_codigo"]), "umbral": g["umbral"],
            "perdidos": perdidos, "duplicados": duplicados})
    ficheros = Counter(g["fichero"] for g in res["grupos"])
    choque = [f for f, n in ficheros.items() if n > 1]
    if choque:
        raise ErrorExtraccion("dos grupos dan el mismo nombre de fichero: %s" % choque)
    return res


def codigo_salida(res):
    return 0 if all(not g["perdidos"] and not g["duplicados"] for g in res["grupos"]) else 1


def volcado(g, modo="grupos"):
    meta = {"fuente": g["fuente"], "pagina": g["pagina"], "codigo_crudo": g["titulo"], "modo": modo}
    if modo == "profesores":
        return {"_meta": meta,
                "celdas": [a_json_profesor(c) for c in g["celdas"] if c["tramo"] is not None],
                "recreo": [a_json_profesor(c) for c in g["celdas"] if c["tramo"] is None]}
    return {"_meta": meta, "celdas": [a_json(c) for c in g["celdas"]]}


def informe_profesores(res, version):
    out = ["extraer-horario.py --modo profesores", "poppler: %s" % version]
    tot = Counter()
    umbrales = sorted({g["umbral"] for g in res["grupos"]})
    for g in res["grupos"]:
        cel = [c for c in g["celdas"] if c["tramo"] is not None]
        rec = [c for c in g["celdas"] if c["tramo"] is None]
        cont = (" + continuación p%s" % ",".join(map(str, g["continuaciones"]))) if g["continuaciones"] else ""
        out.append("  %-44s %-32s p%-3d%s  celdas=%d recreo=%d rejilla=%d asig=%d perdidos=%d duplicados=%d"
                   % (g["fichero"], g["fuente"], g["pagina"], cont, len(cel), len(rec), len(g["rejilla"]),
                      len(g["codigos"]["asig"]), len(g["perdidos"]), len(g["duplicados"])))
        for w in g["perdidos"]:
            out.append("      PERDIDO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        for w in g["duplicados"]:
            out.append("      DUPLICADO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        tot["celdas"] += len(cel)
        tot["recreo"] += len(rec)
        tot["grupos_celdas"] += sum(len(c["grupos"]) for c in cel)
        tot["grupos_recreo"] += sum(len(c["grupos"]) for c in rec)
        tot["aula_celdas"] += sum(1 for c in cel if c["aula"] is not None)
        tot["aula_recreo"] += sum(1 for c in rec if c["aula"] is not None)
        tot["rejilla"] += len(g["rejilla"])
        tot["perdidos"] += len(g["perdidos"])
        tot["duplicados"] += len(g["duplicados"])
    for t, antes, despues in res["sustituidos"]:
        out.append("  sustituido %r: %s -> %s" % (t, antes, despues))
    out.append("umbral de bandas: %s" % ", ".join("%.2f" % u for u in umbrales))
    out.append("páginas=%d profesores=%d celdas=%d recreo=%d grupos en celdas=%d grupos en recreo=%d "
               "aula en celdas=%d aula en recreo=%d tokens de rejilla=%d perdidos=%d duplicados=%d"
               % (res["paginas"], len(res["grupos"]), tot["celdas"], tot["recreo"], tot["grupos_celdas"],
                  tot["grupos_recreo"], tot["aula_celdas"], tot["aula_recreo"], tot["rejilla"],
                  tot["perdidos"], tot["duplicados"]))
    return "\n".join(out)


def informe(res, version):
    if res.get("modo") == "profesores":
        return informe_profesores(res, version)
    out = ["extraer-horario.py --modo grupos", "poppler: %s" % version]
    tot = Counter()
    for g in res["grupos"]:
        cont = (" + continuación p%s" % ",".join(map(str, g["continuaciones"]))) if g["continuaciones"] else ""
        n_cod = len(g["codigos"]["prof"]) + len(g["codigos"]["asig"])
        out.append("  %-24s %-30s p%-3d%s  celdas=%d rejilla=%d leyenda=%d (códigos prof=%d asig=%d) "
                   "perdidos=%d duplicados=%d"
                   % (g["fichero"], g["fuente"], g["pagina"], cont, len(g["celdas"]), len(g["rejilla"]),
                      g["palabras_leyenda"], len(g["codigos"]["prof"]), len(g["codigos"]["asig"]),
                      len(g["perdidos"]), len(g["duplicados"])))
        for w in g["perdidos"]:
            out.append("      PERDIDO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        for w in g["duplicados"]:
            out.append("      DUPLICADO %r p%d x0=%.2f top=%.2f" % (w["text"], w["id"][0], w["x0"], w["top"]))
        tot["celdas"] += len(g["celdas"])
        tot["rejilla"] += len(g["rejilla"])
        tot["leyenda"] += g["palabras_leyenda"]
        tot["codigos"] += n_cod
        tot["perdidos"] += len(g["perdidos"])
        tot["duplicados"] += len(g["duplicados"])
    for t, antes, despues in res["sustituidos"]:
        out.append("  sustituido %r: %s -> %s" % (t, antes, despues))
    out.append("páginas=%d grupos=%d celdas=%d tokens de rejilla=%d tokens de leyenda=%d (códigos=%d) "
               "perdidos=%d duplicados=%d"
               % (res["paginas"], len(res["grupos"]), tot["celdas"], tot["rejilla"], tot["leyenda"],
                  tot["codigos"], tot["perdidos"], tot["duplicados"]))
    return "\n".join(out)


def escribir(res, salida):
    os.makedirs(salida, exist_ok=True)
    for g in sorted(res["grupos"], key=lambda g: g["fichero"]):
        with open(os.path.join(salida, g["fichero"]), "w", encoding="utf-8") as f:
            json.dump(volcado(g, res.get("modo", "grupos")), f, ensure_ascii=False, indent=2)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--modo", required=True, choices=["grupos", "profesores"])
    ap.add_argument("--salida", required=True)
    ap.add_argument("pdfs", nargs="+")
    a = ap.parse_args(argv)
    if not os.path.isabs(a.salida):
        print("ERROR: --salida debe ser una ruta absoluta: %r" % a.salida, file=sys.stderr)
        return 2
    if os.path.exists(a.salida):
        if not os.path.isdir(a.salida) or os.listdir(a.salida):
            print("ERROR: --salida existe y no es un directorio vacío: %s" % a.salida, file=sys.stderr)
            return 2
    if shutil.which("pdftotext") is None:
        print("ERROR: no se encuentra pdftotext (poppler-utils) en el PATH", file=sys.stderr)
        return 2
    try:
        docs = [(os.path.basename(p), leer_pdf(p)) for p in a.pdfs]
        res = procesar(docs, a.modo)
    except ErrorExtraccion as e:
        print("ERROR: %s" % e, file=sys.stderr)
        return 2
    print(informe(res, version_poppler()))
    rc = codigo_salida(res)
    if rc == 0:
        escribir(res, a.salida)
        print("escritos %d ficheros en %s" % (len(res["grupos"]), a.salida))
    else:
        print("CUADRE FALLIDO: no se escribe nada")
    return rc


if __name__ == "__main__":
    sys.exit(main())
