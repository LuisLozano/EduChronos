#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deriva las reglas de aulas del Excel del centro para la base de prueba de O-aulas (S209 T2).

ENTRADAS (todas obligatorias; se normalizan a absolutas y se comprueban antes de escribir nada)
    --hoja1-rejilla  TSV de la hoja 1 por filas (columnas A-H), de s205/m2/inventario_xlsx.py
    --hoja1-celdas   TSV de la hoja 1 por celdas (ref, valor, formula, relleno, negrita, comentario)
    --hoja2-rejilla  TSV de la hoja 2 por filas (optativa en A, alumnos en B)
    --catalogo       catalogo-derivado.json del curso (codigos de aulas, grupos, subgrupos y plazas)
    --nombres        nombres-derivados.json del curso: el catalogo no trae el nombre de las
                     asignaturas (nombreCompleto null) y la casacion de materia es por nombre
    --salida         reglas-aulas.json
Sin red y sin base. Mismas entradas, mismo fichero byte a byte.

PRINCIPIO (S209): lo dudoso del Excel se OMITE con su motivo; no se supone ninguna regla. El JSON
lleva solo codigos, numeros, roles y referencias de celda: ningun texto libre del Excel.

NORMAS
N1  Aula (columna E de la hoja 1): EXACTA si es un codigo del catalogo; si no, VARIANTE por UNA de
    V1 (quitar parentesis y espacios), V2 (B<cifra> -> B0<cifra>, encadenable tras V1) o
    V3 (Taller<n> <L> -> TAL<n><l>). Fuera del rango que dan las notas de la columna B sin aula
    («de las b01 a la b07», «de la A1 a la A18»): sin lectura. Lo demas, sin lectura segura;
    toda regla que use un aula sin lectura se omite.
N2  Aula de referencia: E con relleno amarillo (FFFFFF00) o naranja (FFFF9900) y F que nombra
    UN grupo ORDINARIO del catalogo. Una fila DIVER (PDC) se omite: el PDC no admite aula de
    referencia y cuenta como su grupo padre. Otro color, omitida (N2'). Una fila que casa con
    varios grupos (un 1º BCH con sus dos grupos) se omite entera (N2'', S209 T2c).
N3  Aulas de una asignatura, en las filas de uso (F no nombra grupos), con el texto de G y H
    partido en frases por «.», «,» y «;»:
      rol (N3''): «si es necesario» -> la frase se omite; «si es posible» (aunque diga «solo»)
        -> PREFERIDA; «solo»/«sólo» -> EXCLUSIVA; sin calificativo -> PREFERIDA.
      materia (N3'): las asignaturas cuyo nombre normalizado (sin tildes ni mayusculas, espacios
        colapsados) aparece ENTERO como palabras de la frase, la mas larga primero; reciben la
        regla TODAS las del mismo nombre. Un nombre que solo contiene la palabra no casa. Una
        frase con calificativo y sin ninguna materia se omite.
      ERRATAS: correcciones de lectura decididas por el arquitecto (S209), antes de casar.
      Una asignatura con aulas EXCLUSIVA y PREFERIDA a la vez (el backend no lo admite): se
      omiten todas sus reglas.
N4  Uso: «no disponible», «no usar» o «no se puede usar» en E, F, G o H -> enUso=false.
N5  Capacidad: «caben N» en G -> N; si no, las sillas (D) si son un numero; si no, nada.
    Las mesas (C) solo van a la procedencia.
N6  Alumnos (hoja 2): solo las filas que casan con UN subgrupo que es la plaza entera
    (CASA-1, M2 de S209 P5.4); el resto se omite con su clase. Tabla TERMINOS del M2.
N7  Cada regla lleva su procedencia: hoja, celdas y la casacion del aula (EXACTA, V1-V3).
N8  `conservarAula` vacia (la llena el M4) y `omitidas` con regla, origen y motivo.
N9  Coherencia de alumnos con el aula oficial (S209 T2b). Para cada plaza de CLASE del catalogo
    con subgrupos que tienen alumnos derivados (N6) y cuyas aulas tienen TODAS capacidad
    derivada (N5): si la suma de esos alumnos supera la capacidad del aula fija, o la MAYOR de
    las candidatas, se omiten los alumnos de todos esos subgrupos. Las capacidades no se tocan.
"""

import argparse
import csv
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

AMARILLO, NARANJA = "rgb=FFFFFF00", "rgb=FFFF9900"
NO_DISPONIBLE = ("no disponible", "no usar", "no se puede usar")
ERRATAS = {"laboratotioy": "laboratorio y"}   # G7: «prácticas de laboratotioy anatomía» (S209, N3'')

# Hoja 2: término del Excel -> códigos de asignatura (tabla del M2 de S209, P5.4).
TERMINOS = {
    "OyD": ["OyD"], "CyR": ["CyR"], "ATEDU": ["ATEDU", "ATED"],
    "REL": ["Rel", "Relig"], "RELIG": ["Rel", "Relig"],
    "REF MAT": ["RefMt"], "REF MAT/LEN": ["RefMt", "RefLe", "ReLen"], "REF": ["RefMt", "RefLe", "ReLen"],
    "ALCT": ["ALCT"], "FRA": ["Fr2"], "P. ED. PLÁST. AUD.": ["PEPA"], "BIO Y NUTRIC.": ["BioNu"],
    "BIO": ["Bio", "Biol", "ByG", "BIOL"], "FyQ": ["FyQ", "FQ"],
    "TECN (Optativa)": ["TEC", "Tec", "TecIn"], "TECN (Modalidad)": ["TEC", "Tec", "TecIn"],
    "LATÍN": ["LAT", "Latín", "Lat2"], "DIGIT (Optativa)": ["DIG"], "DIGIT (Modalidad)": ["DIG"],
    "FOPP (Optativa -Diver)": ["FOPP"], "FOPP (Modalidad)": ["FOPP"], "FOPP": ["FOPP"],
    "ECOyEMP": ["ECO", "Econ"], "MAT A": ["MatAp", "MatAc"], "DIB TÉC": ["DT", "DTec"],
    "DIB TEC": ["DT", "DTec"], "ACTIV FÍS": ["AFAVS"], "CINE Hª": ["CeH"], "EXPR ART": ["EXPRE"],
    "TEC IND": ["TecIn"], "ANAT": ["ANAT"], "PRAC LAB": ["PLAB"], "GRIEGO": ["GRI", "Gri2"],
    "ECON": ["ECO", "Econ"], "CEE": ["CEE"], "MCS": ["MCCSS", "MaCSa"], "HªMC": ["HMC"],
    "LITUN": ["LU"], "TEC EST": ["TES1", "TES2"], "DIB ART": ["DART"], "PCUL": ["PATRI"],
    "TICO": ["TICO"], "PTEV": ["PTEV", "PTVE"], "QUIM": ["QUI"], "FIS": ["Físic"], "Hª ARTE": ["HART"],
    "EMPRESA": ["Econ", "ECO"], "GEO": ["Geogr", "Geo", "GeH"], "MITO": ["MIT"], "COMENT": ["CT"],
    "ESTAD": ["EST"],
}

M_AULA_SIN_LECTURA = "aula sin lectura segura"
M_FUERA_DE_RANGO = "aula fuera del rango que da el propio Excel"
M_VARIOS_GRUPOS = "la fila casa con varios grupos de la base"


# ---------------------------------------------------------------- lectura

def desescapar(t):
    return re.sub(r"\\(.)", lambda m: {"n": "\n", "t": "\t", "r": "\r"}.get(m.group(1), m.group(1)), t)


def leer_tsv(ruta):
    with open(ruta, encoding="utf-8", newline="") as f:
        return [[desescapar(c) for c in x] for x in csv.reader(f, delimiter="\t", quoting=csv.QUOTE_NONE)]


def leer_rejilla(ruta):
    """{fila: {columna: valor}} de un -rejilla.tsv (cabecera: vacía y las letras de columna)."""
    filas = leer_tsv(ruta)
    cab = filas[0][1:]
    return {int(x[0]): dict(zip(cab, x[1:])) for x in filas[1:] if x and x[0].strip()}


def leer_rellenos(ruta):
    filas = leer_tsv(ruta)
    i_ref, i_rel = filas[0].index("ref"), filas[0].index("relleno")
    return {x[i_ref]: x[i_rel] for x in filas[1:] if len(x) > i_rel}


def norm(s):
    s = "".join(ch for ch in unicodedata.normalize("NFD", s.lower()) if unicodedata.category(ch) != "Mn")
    return " ".join(s.split())


def numero(s):
    s = s.strip()
    if not re.fullmatch(r"\d+(\.0+)?", s):
        return None
    return int(float(s))


def celda(hoja, *celdas, casacion=None):
    p = {"hoja": hoja, "celdas": list(celdas)}
    if casacion:
        p["casacion"] = casacion
    return p


# ---------------------------------------------------------------- hoja 1

def rangos_de_notas(h1):
    """{letra: máximo} de las notas de B en filas sin aula: dos códigos de la misma letra."""
    rangos = {}
    for fila in sorted(h1):
        if h1[fila].get("E", "").strip():
            continue
        cods = re.findall(r"(?<![A-Za-z0-9])([AaBb])0*(\d+)(?!\d)", h1[fila].get("B", ""))
        letras = {l.upper() for l, _ in cods}
        if len(cods) >= 2 and len(letras) == 1:
            rangos[letras.pop()] = max(int(n) for _, n in cods)
    return rangos


def casar_aula(e, aulas, rangos):
    """(código, casación) o (None, motivo de omisión)."""
    e = e.strip()
    v1 = re.sub(r"\(.*?\)", "", e).replace(" ", "")
    m = re.fullmatch(r"([AB])(\d+)", v1)
    if m and m.group(1) in rangos and int(m.group(2)) > rangos[m.group(1)]:
        return None, M_FUERA_DE_RANGO
    if e in aulas:
        return e, "EXACTA"
    m3 = re.fullmatch(r"Taller(\d) ([A-Z])", e)
    if m3 and "TAL%s%s" % (m3.group(1), m3.group(2).lower()) in aulas:
        return "TAL%s%s" % (m3.group(1), m3.group(2).lower()), "V3"
    m2 = re.fullmatch(r"B(\d)", v1)
    if m2 and "B0" + m2.group(1) in aulas:
        return "B0" + m2.group(1), "V1+V2" if v1 != e else "V2"
    if v1 != e and v1 in aulas:
        return v1, "V1"
    return None, M_AULA_SIN_LECTURA


def grupos_de(f, grupos):
    """Grupos del catálogo que nombra F, o None si F no nombra grupos (fila de uso)."""
    f = f.strip()
    m = re.fullmatch(r"(\d)º\s*ESO\s+([A-D])", f)
    if m:
        return [g for g in ["%sº%s" % (m.group(1), m.group(2))] if g in grupos]
    m = re.fullmatch(r"(\d)º\s*ESO\s+DIVER", f)
    if m:
        return sorted(g for g, x in grupos.items() if x["tipo"] == "DIVERSIFICACION_PDC"
                      and x["nivel"] == m.group(1) + "ESO")
    m = re.fullmatch(r"1º\s*(?:BCH|BTO)\s+([A-C])", f)
    if m:
        return sorted(g for g, x in grupos.items() if x["nivel"] == "1BACH" and g.startswith("1B-" + m.group(1)))
    m = re.fullmatch(r"2º\s*(?:BCH|BTO)\s+([A-C])", f)
    if m:
        return [g for g in ["2B-" + m.group(1)] if g in grupos]
    m = re.fullmatch(r"(\d)º\s*FPB", f)
    if m:
        return [g for g in [m.group(1) + "FPB"] if g in grupos]
    return None


def materias(frase, por_nombre):
    """Asignaturas cuyo nombre aparece entero en la frase, la más larga primero, sin solaparse."""
    hallados, resto = [], frase
    for nombre in sorted(por_nombre, key=lambda n: (-len(n), n)):
        patron = r"(?<!\w)%s(?!\w)" % re.escape(nombre)
        if re.search(patron, resto):
            hallados.extend(por_nombre[nombre])
            resto = re.sub(patron, " | ", resto)
    return sorted(set(hallados))


def calificativo(frase):
    if "si es necesario" in frase:
        return "NECESARIO"
    if "si es posible" in frase:
        return "PREFERIDA"
    if re.search(r"(?<!\w)solo(?!\w)", frase):
        return "EXCLUSIVA"
    return None


def derivar_hoja1(h1, rellenos, cat, nombres, omitidas):
    aulas = {a["codigo"] for a in cat["aulas"]}
    grupos = {g["codigo"]: g for g in cat["grupos"]}
    por_nombre = {}
    for cod, x in nombres["asignaturas"].items():
        if x.get("nombreCompleto"):
            por_nombre.setdefault(norm(x["nombreCompleto"]), []).append(cod)
    rangos = rangos_de_notas(h1)
    ref, asig, uso, capac = {}, {}, {}, {}

    def omitir(regla, origen, motivo, **codigos):
        omitidas.append(dict({"regla": regla, "origen": origen, "motivo": motivo}, **codigos))

    for fila in sorted(h1):
        x = h1[fila]
        e = x.get("E", "").strip()
        if fila == 1:
            continue
        if not e:
            # Una nota suelta en B que no es un rango (B40, B41) nombra aulas sin código: se omite.
            if x.get("B", "").strip() and not re.search(r"(?<![A-Za-z0-9])[AaBb]0*\d", x["B"]):
                omitir("nota", celda(1, "B%d" % fila), "nota sin aula con lectura segura")
            continue
        cod, cas = casar_aula(e, aulas, rangos)
        if cod is None:
            omitir("aula", celda(1, "E%d" % fila), cas)
        f, g, h = x.get("F", ""), x.get("G", ""), x.get("H", "")
        gs = grupos_de(f, grupos)

        # N2: aula de referencia
        if gs == []:
            omitir("aulaReferencia", celda(1, "E%d" % fila, "F%d" % fila), "el grupo no está en el catálogo")
        if gs:
            origen = celda(1, "E%d" % fila, "F%d" % fila, casacion=cas if cod else None)
            if all(grupos[k]["tipo"] == "DIVERSIFICACION_PDC" for k in gs):
                omitir("aulaReferencia", origen, "el PDC no admite aula de referencia; cuenta como su grupo padre",
                       grupos=gs)
            elif cod is None:
                omitir("aulaReferencia", origen, cas, grupos=gs)
            elif rellenos.get("E%d" % fila, "") not in (AMARILLO, NARANJA):
                omitir("aulaReferencia", origen, "relleno de la fila que no es amarillo ni naranja", grupos=gs)
            elif len(gs) > 1:
                omitir("aulaReferencia", origen, M_VARIOS_GRUPOS, grupos=gs)
            else:
                for k in gs:
                    ref.setdefault(k, []).append((cod, origen))

        # N3: aulas de una asignatura (filas de uso)
        if gs is None and (g.strip() or h.strip()):
            col = [c for c in "GH" if x.get(c, "").strip()]
            texto = norm(" . ".join(x.get(c, "") for c in col))
            for mal, bien in ERRATAS.items():
                texto = texto.replace(mal, bien)
            origen = celda(1, "E%d" % fila, "F%d" % fila, *["%s%d" % (c, fila) for c in col],
                           casacion=cas if cod else None)
            frases = [(calificativo(fr), materias(fr, por_nombre)) for fr in re.split(r"[.,;]", texto)]
            relevantes = [(q, ms) for q, ms in frases if q or ms]
            if relevantes and cod is None:
                omitir("aulasAsignatura", origen, cas)
            elif relevantes:
                for q, ms in relevantes:
                    if q == "NECESARIO":
                        omitir("aulasAsignatura", origen, "calificativo «si es necesario»: no da rol", aula=cod)
                    elif not ms:
                        omitir("aulasAsignatura", origen, "ninguna asignatura con ese nombre exacto", aula=cod)
                    else:
                        for a in ms:
                            asig.setdefault(a, []).append((cod, q or "PREFERIDA", origen))

        # N4: uso
        if any(n in norm(x.get(c, "")) for c in "EFGH" for n in NO_DISPONIBLE):
            origen = celda(1, "E%d" % fila, *["%s%d" % (c, fila) for c in "FGH" if x.get(c, "").strip()],
                           casacion=cas if cod else None)
            if cod is None:
                omitir("usoAula", origen, cas)
            else:
                uso.setdefault(cod, []).append(origen)

        # N5: capacidad
        if cod is not None:
            m = re.search(r"caben (\d+)", norm(g))
            mesas = ["C%d" % fila] if x.get("C", "").strip() else []
            if m:
                capac.setdefault(cod, []).append((int(m.group(1)), celda(1, "E%d" % fila, "G%d" % fila, *mesas,
                                                                           "D%d" % fila, casacion=cas)))
            elif numero(x.get("D", "")) is not None:
                capac.setdefault(cod, []).append((numero(x["D"]), celda(1, "E%d" % fila, "D%d" % fila, *mesas,
                                                                       casacion=cas)))
            else:
                omitir("capacidad", celda(1, "E%d" % fila, "D%d" % fila, casacion=cas),
                       "ni «caben N» en G ni un número de sillas en D", aula=cod)

    salida = {"aulaReferencia": [], "aulasAsignatura": [], "usoAula": [], "capacidad": []}
    for k in sorted(ref):
        if len({a for a, _ in ref[k]}) > 1:
            for _, o in ref[k]:
                omitir("aulaReferencia", o, "el grupo recibe aulas distintas de varias filas", grupos=[k])
        else:
            salida["aulaReferencia"].append({"grupo": k, "aula": ref[k][0][0],
                                             "procedencia": [o for _, o in ref[k]]})
    for a in sorted(asig):
        if len({r for _, r, _ in asig[a]}) > 1:
            for _, _, o in asig[a]:
                omitir("aulasAsignatura", o, "la asignatura tendría aulas EXCLUSIVA y PREFERIDA a la vez",
                       asignatura=a)
            continue
        por_aula = {}
        for aula, rol, o in asig[a]:
            por_aula.setdefault(aula, (rol, []))[1].append(o)
        salida["aulasAsignatura"].append({"asignatura": a, "aulas": [
            {"aula": aula, "rol": rol, "procedencia": os_} for aula, (rol, os_) in sorted(por_aula.items())]})
    for a in sorted(uso):
        salida["usoAula"].append({"aula": a, "enUso": False, "procedencia": uso[a]})
    for a in sorted(capac):
        if len({n for n, _ in capac[a]}) > 1:
            for _, o in capac[a]:
                omitir("capacidad", o, "el aula recibe capacidades distintas de varias filas", aula=a)
        else:
            salida["capacidad"].append({"aula": a, "capacidad": capac[a][0][0],
                                        "procedencia": [o for _, o in capac[a]]})
    return salida


# ---------------------------------------------------------------- hoja 2

def clasificar_hoja2(h2, cat):
    """[(fila, clase, subgrupos, número o None)] con la regla del M2 de S209 (P5.4), sobre el catálogo."""
    grupos = {g["codigo"]: g for g in cat["grupos"]}
    pob = {s["codigo"]: set(s.get("grupos") or []) for s in cat["subgrupos"]}

    def letra(cod):
        nivel = grupos[cod]["nivel"]
        if "ESO" in nivel:
            return cod.split("º")[1][0]
        if "BACH" in nivel:
            return cod.split("-")[1][0]
        return ""

    plazas = []
    for a in cat["actividades"]:
        if a.get("tipo", "CLASE") != "CLASE":
            continue
        for p in a["plazas"]:
            plazas.append((p["asignatura"], {s: pob.get(s, set()) for s in p.get("subgrupos") or []}))
    res = []
    for fila in sorted(h2):
        A, B = h2[fila].get("A", "").strip(), h2[fila].get("B", "").strip()
        if not A or not B or B == "Nº ALUMNOS":
            continue
        m = re.match(r"^(.*?)\s+([1-4])º\s*(ESO|BTO)\s+(.*)$", A)
        if not m:
            res.append((fila, "SIN CASACIÓN", [], None, "no se lee término y nivel"))
            continue
        termino, cifra, etapa, resto = m.group(1).strip(), m.group(2), m.group(3), m.group(4)
        if termino not in TERMINOS:
            res.append((fila, "SIN CASACIÓN", [], None, "término sin tabla"))
            continue
        codigos = TERMINOS[termino]
        nivel = cifra + ("ESO" if etapa == "ESO" else "BACH")
        sin_diver = "(- Diver)" in resto
        resto2 = resto.replace("(- Diver)", "")
        diver = "DIVER" in resto2.upper()
        resto2 = re.sub(r"\(.*?\)", "", resto2)
        letras = re.findall(r"(?<![A-Za-z])([A-D])(?![a-z])", resto2.replace("DIVER", ""))
        if nivel == "1BACH":
            res.append((fila, "SIN CASACIÓN", [], None, "1º BTO: el Excel usa A-D y el catálogo otras letras"))
            continue
        ordinarios = {g for g, x in grupos.items() if x["nivel"] == nivel and x["tipo"] == "ORDINARIO"}
        gs, falta = set(), []
        for L in letras:
            hit = [g for g in ordinarios if letra(g) == L]
            if not hit:
                falta.append(L)
            gs.update(hit)
        if falta:
            res.append((fila, "SIN CASACIÓN", [], None, "el catálogo no tiene ese grupo en el nivel"))
            continue
        pdcs = {g for g, x in grupos.items() if x["nivel"] == nivel and x["tipo"] != "ORDINARIO"}
        if diver:
            gs.update(pdcs)
        if not sin_diver:
            gs.update(g for g in pdcs if grupos[g].get("grupoPadre") in gs)
        S = {}
        for i, (asignatura, subs) in enumerate(plazas):
            if asignatura not in codigos:
                continue
            for sg, gg in subs.items():
                if gg <= gs:
                    S[sg] = i
        idx = sorted(set(S.values()))
        if not S:
            res.append((fila, "SIN CASACIÓN", [], None, "ningún subgrupo de esa asignatura en esos grupos"))
            continue
        if len(idx) > 1:
            res.append((fila, "AMBIGUA", sorted(S), None, "la fila cae en varias plazas"))
            continue
        subs = plazas[idx[0]][1]
        cubiertos = set()
        for sg in S:
            for g in subs[sg]:
                cubiertos.add(grupos[g].get("grupoPadre") if grupos[g]["tipo"] != "ORDINARIO" else g)
        if any(grupos[g]["tipo"] == "ORDINARIO" and g not in cubiertos for g in gs):
            res.append((fila, "INCOMPLETA", sorted(S), None, "la fila nombra un grupo sin subgrupo de esa asignatura"))
            continue
        if set(S) == set(subs):
            clase = "CASA-1" if len(S) == 1 else "CASA-PLAZA"
        else:
            clase = "PARCIAL"
        motivo = {"CASA-1": None,
                  "CASA-PLAZA": "plaza entera con varios subgrupos: el número es el total y su reparto no tiene fuente",
                  "PARCIAL": "la fila cubre parte de una plaza"}[clase]
        res.append((fila, clase, sorted(S), numero(B), motivo))
    return res


def derivar_hoja2(h2, cat, omitidas):
    por_sub = {}
    for fila, clase, subs, n, motivo in clasificar_hoja2(h2, cat):
        origen = celda(2, "A%d" % fila, "B%d" % fila)
        if clase != "CASA-1":
            omitidas.append({"regla": "alumnos", "origen": origen, "motivo": "%s: %s" % (clase, motivo)})
        elif n is None:
            omitidas.append({"regla": "alumnos", "origen": origen, "motivo": "CASA-1: el número de alumnos no es un número"})
        else:
            por_sub.setdefault(subs[0], []).append((n, origen))
    alumnos = []
    for s in sorted(por_sub):
        if len(por_sub[s]) > 1:
            for _, o in por_sub[s]:
                omitidas.append({"regla": "alumnos", "origen": o, "motivo": "el subgrupo recibe número de varias filas",
                                 "subgrupo": s})
        else:
            alumnos.append({"subgrupo": s, "alumnos": por_sub[s][0][0], "procedencia": [por_sub[s][0][1]]})
    return alumnos


M_N9 = "contradice la capacidad del aula oficial (hoja 1 frente a hoja 2)"


def coherencia_alumnos(cat, capacidad, alumnos, omitidas):
    """N9: quita de `alumnos` los subgrupos de las plazas cuya suma supera la capacidad de su aula."""
    cap = {r["aula"]: r["capacidad"] for r in capacidad}
    por_sub = {r["subgrupo"]: r for r in alumnos}
    quitados = {}
    for a in cat["actividades"]:
        if a.get("tipo", "CLASE") != "CLASE":
            continue
        for i, p in enumerate(a["plazas"], 1):
            subs = [s for s in p.get("subgrupos") or [] if s in por_sub]
            aulas = [p["aulaFija"]] if p.get("aulaFija") else list(p.get("aulasCandidatas") or [])
            if not subs or not aulas or any(x not in cap for x in aulas):
                continue
            limite = max(cap[x] for x in aulas)
            suma = sum(por_sub[s]["alumnos"] for s in subs)
            if suma > limite:
                for s in subs:
                    quitados.setdefault(s, {"plaza": "%s-P%d" % (a["codigo"], i), "aulas": sorted(aulas),
                                            "capacidad": limite, "suma": suma})
    for s in sorted(quitados):
        for o in por_sub[s]["procedencia"]:
            omitidas.append(dict({"regla": "alumnos", "origen": o, "motivo": M_N9, "subgrupo": s}, **quitados[s]))
    return [r for r in alumnos if r["subgrupo"] not in quitados]


# ---------------------------------------------------------------- salida

def derivar(rutas):
    h1 = leer_rejilla(rutas["hoja1_rejilla"])
    rellenos = leer_rellenos(rutas["hoja1_celdas"])
    h2 = leer_rejilla(rutas["hoja2_rejilla"])
    cat = json.loads(rutas["catalogo"].read_text(encoding="utf-8"))
    nombres = json.loads(rutas["nombres"].read_text(encoding="utf-8"))
    omitidas = []
    reglas = derivar_hoja1(h1, rellenos, cat, nombres, omitidas)
    reglas["alumnos"] = coherencia_alumnos(cat, reglas["capacidad"], derivar_hoja2(h2, cat, omitidas), omitidas)
    orden = {"aula": 0, "nota": 1, "aulaReferencia": 2, "aulasAsignatura": 3, "usoAula": 4, "capacidad": 5, "alumnos": 6}
    omitidas.sort(key=lambda o: (orden[o["regla"]], o["origen"]["hoja"],
                                 [(re.sub(r"\d", "", c), int(re.sub(r"\D", "", c))) for c in o["origen"]["celdas"]],
                                 o["motivo"]))
    return {
        "_meta": {
            "generadoPor": "tools/carga-centro/derivar-reglas-aulas.py",
            "entradas": {k: {"fichero": rutas[k].name, "sha256": hashlib.sha256(rutas[k].read_bytes()).hexdigest()}
                         for k in ("hoja1_rejilla", "hoja1_celdas", "hoja2_rejilla", "catalogo", "nombres")},
            "normas": "N1-N8 con N2', N3' y N3'' de S209 T2 (cabecera del script)",
        },
        "aulaReferencia": reglas["aulaReferencia"],
        "aulasAsignatura": reglas["aulasAsignatura"],
        "usoAula": reglas["usoAula"],
        "capacidad": reglas["capacidad"],
        "alumnos": reglas["alumnos"],
        "conservarAula": [],
        "omitidas": omitidas,
    }


def recuentos(salida):
    lineas = []
    for regla in ("aulaReferencia", "aulasAsignatura", "usoAula", "capacidad", "alumnos"):
        lineas.append("  %-16s %3d reglas" % (regla, len(salida[regla])))
    lineas.append("  omitidas: %d" % len(salida["omitidas"]))
    cuenta = {}
    for o in salida["omitidas"]:
        cuenta[(o["regla"], o["motivo"])] = cuenta.get((o["regla"], o["motivo"]), 0) + 1
    for (regla, motivo), n in sorted(cuenta.items()):
        lineas.append("    %-16s %3d  %s" % (regla, n, motivo))
    return lineas


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    for nombre in ("--hoja1-rejilla", "--hoja1-celdas", "--hoja2-rejilla", "--catalogo", "--nombres", "--salida"):
        parser.add_argument(nombre, required=True)
    args = parser.parse_args(argv)
    rutas = {k: Path(getattr(args, k)).resolve()
             for k in ("hoja1_rejilla", "hoja1_celdas", "hoja2_rejilla", "catalogo", "nombres", "salida")}
    faltan = [str(rutas[k]) for k in rutas if k != "salida" and not rutas[k].is_file()]
    if faltan:
        print("Faltan entradas; no se escribe nada:\n  " + "\n  ".join(faltan), file=sys.stderr)
        return 1
    for k in ("hoja1_rejilla", "hoja1_celdas", "hoja2_rejilla", "catalogo", "nombres"):
        print("%-14s %s" % (k, rutas[k]))
    salida = derivar(rutas)
    rutas["salida"].write_text(json.dumps(salida, ensure_ascii=False, indent=1), encoding="utf-8")
    print("salida:        %s" % rutas["salida"])
    print("\n".join(recuentos(salida)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
