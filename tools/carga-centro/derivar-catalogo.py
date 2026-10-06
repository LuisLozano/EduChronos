#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deriva catalogo-derivado.json de un curso a partir de sus volcados de grupos.

Nace en S198 (C-catalogo-2026, condición 3 de O-carga-2026). El catálogo de 2025/2026 se derivó
en S115 sin guion versionado (docs/bitacora-sesiones.md:6394-6418); las reglas de aquí son las
que el M2 de S198 reconstruyó y midió contra él (219/219 actividades, 316/316 plazas).

USO
    python3 -B tools/carga-centro/derivar-catalogo.py --volcados <dir> --decisiones <json> \\
        [--parche <parche-aulas.json>] --salida <carpeta ABSOLUTA vacía>

ENTRADAS
    --volcados    carpeta con los grupo-*.json del curso (esquema de S115). Entrada principal:
                  de ella sale todo lo que el horario imprime. Si la carpeta trae además
                  aula-*.json (2025/2026), de ellos solo se toma la LISTA de aulas, para dar de
                  alta los espacios sin ninguna clase.
    --decisiones  lo que el volcado no contiene. Cada entrada: id, fija, valor, motivo, fuente
                  («sin fuente» si no la hay). Sin motivo o sin fuente, aborta.
    --parche      opcional. Cambios de aula por sesión (parche-aulas.py, S197). Se aplica
                  DESPUÉS de derivar y la regla de aulas por plaza se recalcula.
    --volcados-profesores <dir> --pdf-grupos <pdf>...   opcionales y juntos (S204). Añaden
                  las reuniones y funciones de los profesor-*.json (sin grupo ni aula) y los
                  totales declarados. Los PDF de grupos solo dan la leyenda «Profesores:» con la
                  que cruzar-grupos-profesores.py pasa del título de página al código. Sin ellos
                  la salida es la de S198 byte a byte.

REUNIONES, FUNCIONES Y TOTALES (S204, con --volcados-profesores). Cada celda de profesor sin
grupo tiene que tener su código en la decisión noClase.tipos (REUNION o FUNCION) o en
noClase.ignorados (guardias, con su motivo); lo mismo las de recreo. Si no, aborta (D8).
  - REUNION: una actividad por código, con una plaza que lleva a todos sus profesores;
    repeticiones = tramos distintos; aborta si los profesores cambian de un tramo a otro.
  - FUNCION: una actividad {código}-{profesor} por profesor; repeticiones = sus celdas.
  - Las dos: duración 1, sin aula ni subgrupos, sin tutor, patrón por la regla A11 y «tipo».
    Una actividad de clase no lleva «tipo» (D6).
  - Totales (totalDeclarado): profesor = sus celdas con grupo en su página; grupo y PDC = sus
    (día, tramo) distintos en su grupo-*.json. No pasan por la derivación del catálogo.
  - Una página sin código en la leyenda entra por profesores.alta o se ignora por
    profesores.paginasIgnoradas, con su motivo; si no, aborta.

SALIDA (solo si todo cuadra; si algo aborta no se escribe nada)
    catalogo-derivado.json              mismo formato que el de 2025/2026 (indent=1)
    INFORME-CONSERVACION-CATALOGO.md    decisiones aplicadas, cifras, conservación y parche

CONSERVACIÓN. La clave es la del M2 de S198 y la de verificar-conservacion.py:
(grupo, día, tramo, asignatura, profesor), como conjunto. Se expande el catálogo ya escrito
(instancias x plazas x grupos de sus subgrupos x profesores) y se compara con las celdas del
volcado. Toda divergencia tiene que estar explicada por una decisión, citada por su id; si no,
aborta con rc=1 y nombra las claves.

rc=0 escrito; rc=1 conservación o invariante rota; rc=2 uso, decisiones, parche o datos que
ninguna regla cubre.
"""
import argparse
import glob
import importlib.util
import json
import os
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from codigos_grupo import forma_corta  # noqa: E402  (solo se importa; S197)

_spec = importlib.util.spec_from_file_location("cargar_centro", Path(__file__).resolve().parent / "cargar-centro.py")
cargar_centro = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cargar_centro)   # sin efectos al importar: main() va tras __main__

SALIDA_CATALOGO = "catalogo-derivado.json"
SALIDA_INFORME = "INFORME-CONSERVACION-CATALOGO.md"
DIAS = "LMXJV"                                   # dia 1..5 del volcado -> letra de _referencia
TUTORIAL = re.compile(r"^(TUT\d|Tut|PTVE|PTEV)$")  # ESPECIFICACION-CATALOGO.md:218-219
ORDEN_DE_TECLEO = ["jornada", "niveles", "asignaturas", "profesores", "aulas", "grupos",
                   "pdc", "tutorias", "subgrupos", "actividades"]
PROPOSITO = "Guion de tecleo manual del catalogo de entrada + oraculo de regresion"
NORMATIVA = "docs/modelo_datos_fase1.md §5 (I1-I7, S1-S9) y §6.1"
ADVERTENCIA = ("La aplicacion NO lee este fichero. Lo lee tools/carga-centro/cargar-centro.py, "
               "que con el escribe el centro entero por la API REST del producto: es la sede "
               "unica de la estructura del centro y donde aterriza cualquier correccion de "
               "catalogo.")

# Qué puede fijar una decisión, y si admite varias entradas del mismo «fija».
FIJA = {
    "jornada.horas": False,
    "jornada.recreo": False,
    "niveles.orden": False,
    "aulas.alias": False,
    "aulas.tipo": False,
    "aulas.alta": False,
    "plazas.aulaSinVolcado": False,
    "tutorias.tutorPrincipal": False,
    "actividades.requiereTutor": False,
    "conservacion.excluirCeldas": True,
    "meta.notasInvariantes": False,
    # S204 (C-carga-datos-centro): solo los lee --volcados-profesores.
    "noClase.tipos": False,
    "noClase.ignorados": False,
    "asignaturas.nombre": True,
    "profesores.alta": False,
    "profesores.paginasIgnoradas": False,
}
TIPOS_NO_CLASE = ("REUNION", "FUNCION")
CAMPOS_DECISION = ("id", "fija", "valor", "motivo", "fuente")


class Aborto(Exception):
    def __init__(self, rc, mensaje):
        super().__init__(mensaje)
        self.rc = rc


# ----------------------------------------------------------------- decisiones

class Decisiones:
    """Decisiones validadas, por «fija». Cuenta cuántas veces se aplica cada una."""

    def __init__(self, doc):
        if not isinstance(doc, dict) or not isinstance(doc.get("decisiones"), list):
            raise Aborto(2, "decisiones: se espera un objeto con la lista «decisiones»")
        self.lista = doc["decisiones"]
        self.meta = doc.get("_meta", {})
        self.por_fija = defaultdict(list)
        vistos = set()
        for i, d in enumerate(self.lista):
            validar_decision(d, i)
            if d["id"] in vistos:
                raise Aborto(2, "decisiones: id repetido %r" % d["id"])
            vistos.add(d["id"])
            if self.por_fija[d["fija"]] and not FIJA[d["fija"]]:
                raise Aborto(2, "decisiones: «%s» solo admite una entrada (%s y %s)"
                             % (d["fija"], self.por_fija[d["fija"]][0]["id"], d["id"]))
            self.por_fija[d["fija"]].append(d)
        self.usos = Counter()

    def una(self, fija):
        return self.por_fija[fija][0] if self.por_fija[fija] else None

    def todas(self, fija):
        return self.por_fija[fija]

    def usar(self, d, n=1):
        self.usos[d["id"]] += n


def validar_decision(d, i):
    """Exige los cinco campos, motivo y fuente no vacíos y un «fija» conocido."""
    if not isinstance(d, dict):
        raise Aborto(2, "decisión %d: no es un objeto" % i)
    faltan = [c for c in CAMPOS_DECISION if c not in d]
    if faltan:
        raise Aborto(2, "decisión %d (%s): faltan %s" % (i, d.get("id"), faltan))
    for c in ("id", "motivo", "fuente"):
        if not isinstance(d[c], str) or not d[c].strip():
            raise Aborto(2, "decisión %d (%s): «%s» vacío; toda decisión lleva motivo y fuente "
                            "(«sin fuente» si no la hay)" % (i, d.get("id"), c))
    if d["fija"] not in FIJA:
        raise Aborto(2, "decisión %s: «fija» desconocido %r (conocidos: %s)"
                     % (d["id"], d["fija"], sorted(FIJA)))


# ------------------------------------------------------------------- volcados

def cargar_volcados(directorio):
    """Celdas de los grupo-*.json con la forma corta del grupo, y la lista de aula-*.json."""
    ficheros = sorted(glob.glob(os.path.join(directorio, "grupo-*.json")))
    if not ficheros:
        raise Aborto(2, "no hay grupo-*.json en %s" % directorio)
    celdas, crudos = [], {}
    for f in ficheros:
        d = json.loads(Path(f).read_text(encoding="utf-8"))
        crudo = d["_meta"]["codigo_crudo"]
        g = forma_corta(crudo)
        if g in crudos.values():
            raise Aborto(2, "dos títulos dan el mismo grupo %s" % g)
        crudos[crudo] = g
        for c in d["celdas"]:
            celdas.append({"grupo": g, "dia": c["dia"], "tramo": c["tramo"],
                           "asignatura": c["asignatura"], "profesor": c["profesor"], "aula": c["aula"]})
    aulas_fichero = []
    for f in sorted(glob.glob(os.path.join(directorio, "aula-*.json"))):
        aulas_fichero.append(json.loads(Path(f).read_text(encoding="utf-8"))["_meta"]["codigo_crudo"])
    return celdas, crudos, aulas_fichero, len(ficheros)


def clave(c):
    """Clave de conservación del M2: (grupo, día, tramo, asignatura, profesor)."""
    return (c["grupo"], c["dia"], c["tramo"], c["asignatura"], c["profesor"])


def indice_de_aulas(celdas):
    """Clave -> aula cruda del volcado. Una clave repetida aborta: no se elige entre dos."""
    idx = {}
    for c in celdas:
        k = clave(c)
        if k in idx:
            raise Aborto(2, "celda duplicada en el volcado: %s" % (k,))
        idx[k] = c["aula"]
    return idx


def excluir_celdas(celdas, dec):
    """Quita del insumo de derivación las celdas que una decisión excluye; devuelve qué excluyó cada una."""
    excluidas = {}
    for d in dec.todas("conservacion.excluirCeldas"):
        for k in d["valor"]:
            excluidas[tuple(k)] = d["id"]
    presentes = {clave(c) for c in celdas}
    for k, i in excluidas.items():
        if k not in presentes:
            raise Aborto(2, "decisión %s excluye una celda que no está en el volcado: %s" % (i, k))
    for d in dec.todas("conservacion.excluirCeldas"):
        dec.usar(d, len(d["valor"]))
    return [c for c in celdas if clave(c) not in excluidas], excluidas


# --------------------------------------------------------- grupos y niveles

def nivel_de_grupo(codigo):
    """Nivel por la forma del código de grupo (NºX[Di] -> NESO, NB-X[m] -> NBACH, NFPB -> NFPB)."""
    for patron, plantilla in ((r"^(\d)º[A-D](Di)?$", "%sESO"), (r"^(\d)B-[A-D][a-z]?$", "%sBACH"),
                              (r"^(\d)FPB$", "%sFPB")):
        m = re.match(patron, codigo)
        if m:
            return plantilla % m.group(1)
    raise Aborto(2, "grupo %r sin regla de nivel" % codigo)


def tipo_y_padre(codigo):
    """Un código que acaba en «Di» es PDC y su padre es el mismo código sin «Di» (R9)."""
    if codigo.endswith("Di"):
        return "DIVERSIFICACION_PDC", codigo[:-2]
    return "ORDINARIO", None


def derivar_niveles(codigos_grupo, dec):
    """Niveles presentes en los grupos, con el orden que fija la decisión niveles.orden."""
    d = dec.una("niveles.orden")
    if d is None:
        raise Aborto(2, "falta la decisión niveles.orden")
    presentes = {nivel_de_grupo(g) for g in codigos_grupo}
    sin_orden = presentes - set(d["valor"])
    if sin_orden:
        raise Aborto(2, "niveles sin orden en la decisión %s: %s" % (d["id"], sorted(sin_orden)))
    dec.usar(d)
    return [{"codigo": n, "orden": i + 1} for i, n in enumerate(d["valor"]) if n in presentes]


def derivar_grupos(codigos_grupo, niveles):
    """Grupos con nivel, tipo y padre por código; orden (orden del nivel, código)."""
    orden = {n["codigo"]: n["orden"] for n in niveles}
    grupos = []
    for g in sorted(codigos_grupo, key=lambda g: (orden[nivel_de_grupo(g)], g)):
        tipo, padre = tipo_y_padre(g)
        if padre is not None and padre not in codigos_grupo:
            raise Aborto(2, "el PDC %s no tiene a su padre %s en los volcados" % (g, padre))
        grupos.append({"codigo": g, "nivel": nivel_de_grupo(g), "tipo": tipo, "grupoPadre": padre})
    return grupos


# ------------------------------------------------------------------- jornada

def derivar_jornada(celdas, dec):
    """Tramos del volcado con sus horas (decisión) y el recreo insertado tras el tramo que diga la decisión."""
    horas = dec.una("jornada.horas")
    if horas is None:
        raise Aborto(2, "falta la decisión jornada.horas")
    tramos = sorted({c["tramo"] for c in celdas})
    recreo = dec.una("jornada.recreo")
    dia_tipo = []
    for t in tramos:
        if str(t) not in horas["valor"]:
            raise Aborto(2, "decisión %s sin horas para el tramo %d" % (horas["id"], t))
        ini, fin = horas["valor"][str(t)]
        dia_tipo.append({"orden": 0, "horaInicio": ini, "horaFin": fin, "esLectivo": True, "tramoVolcado": t})
        if recreo is not None and t == recreo["valor"]["despuesDelTramo"]:
            dia_tipo.append({"orden": 0, "horaInicio": recreo["valor"]["horaInicio"],
                             "horaFin": recreo["valor"]["horaFin"], "esLectivo": False, "tramoVolcado": None})
            dec.usar(recreo)
    for i, t in enumerate(dia_tipo):
        t["orden"] = i + 1
    dec.usar(horas)
    return {"diaTipo": dia_tipo, "dias": [DIAS[d - 1] for d in sorted({c["dia"] for c in celdas})]}


# --------------------------------------------------------------- actividades

def particionar(celdas):
    """R4/S9: en cada (día, tramo), celdas unidas por grupo o por profesor son UNA sesión; firma -> instancias."""
    por_franja = defaultdict(list)
    for c in celdas:
        por_franja[(c["dia"], c["tramo"])].append(c)
    firmas = defaultdict(list)
    for (d, t), cel in sorted(por_franja.items()):
        padre = list(range(len(cel)))

        def raiz(i):
            while padre[i] != i:
                padre[i] = padre[padre[i]]
                i = padre[i]
            return i
        for i in range(len(cel)):
            for j in range(i + 1, len(cel)):
                if cel[i]["grupo"] == cel[j]["grupo"] or cel[i]["profesor"] == cel[j]["profesor"]:
                    padre[raiz(i)] = raiz(j)
        comp = defaultdict(set)
        for i, c in enumerate(cel):
            comp[raiz(i)].add((c["grupo"], c["asignatura"], c["profesor"]))
        for firma in comp.values():
            firmas[frozenset(firma)].append((d, t))
    return firmas


def pares_de_codocencia(firma, instancias, idx):
    """Co-docencia (S115): misma asignatura en la celda de un grupo, dos entradas, una con aula y otra nula."""
    pares = set()
    por_celda = defaultdict(list)
    for g, a, p in firma:
        for d, t in instancias:
            if (g, d, t, a, p) in idx:
                por_celda[(g, d, t, a)].append((p, idx[(g, d, t, a, p)]))
    for (g, d, t, a), entradas in por_celda.items():
        nulas = [p for p, au in entradas if au is None]
        nombradas = [p for p, au in entradas if au is not None]
        if len(entradas) == 2 and len(nulas) == 1 and len(nombradas) == 1:
            pares.add((a, tuple(sorted((nulas[0], nombradas[0])))))
    return pares


def plazas_de(firma, instancias, idx):
    """Plaza = (asignatura, profesores) con los grupos que cubre; la co-docencia funde a sus dos profesores."""
    grupos_de = defaultdict(set)
    for g, a, p in firma:
        grupos_de[(a, p)].add(g)
    padre = {k: k for k in grupos_de}

    def raiz(k):
        while padre[k] != k:
            k = padre[k]
        return k
    for a, (p1, p2) in pares_de_codocencia(firma, instancias, idx):
        padre[raiz((a, p1))] = raiz((a, p2))
    plazas = defaultdict(lambda: {"profesores": set(), "grupos": set()})
    for (a, p), gs in grupos_de.items():
        r = raiz((a, p))
        plazas[r]["asignatura"] = a
        plazas[r]["profesores"].add(p)
        plazas[r]["grupos"] |= gs
    return sorted(({"asignatura": v["asignatura"], "profesores": sorted(v["profesores"]),
                    "grupos": sorted(v["grupos"])} for v in plazas.values()),
                  key=lambda p: (p["asignatura"], p["profesores"]))


def aulas_de_plaza(plaza, instancias, idx, alias):
    """Conjunto de aulas no nulas (con alias R6) de las celdas de la plaza en todas sus instancias."""
    aulas = set()
    for d, t in instancias:
        for g in plaza["grupos"]:
            for p in plaza["profesores"]:
                au = idx.get((g, d, t, plaza["asignatura"], p))
                if au is not None:
                    aulas.add(alias.get(au, au))
    return aulas


def regla_de_aula(aulas, plaza, dec):
    """1b de S198: un aula -> aulaFija; varias -> aulasCandidatas exactas; ninguna -> decisión por grupo."""
    if len(aulas) == 1:
        return next(iter(aulas)), [], None
    if len(aulas) > 1:
        return None, sorted(aulas), None
    d = dec.una("plazas.aulaSinVolcado")
    elegidas = {d["valor"].get(g) for g in plaza["grupos"]} if d else {None}
    if len(elegidas) != 1 or None in elegidas:
        raise Aborto(2, "plaza %s/%s de %s sin aula en el volcado y sin decisión que la fije"
                     % (plaza["asignatura"], "+".join(plaza["profesores"]), "+".join(plaza["grupos"])))
    return elegidas.pop(), [], d


def patron_temporal(instancias):
    """A11: NEUTRA si hay una repetición o dos caen el mismo día; DISTRIBUIDA si todas en días distintos."""
    dias = [d for d, _ in instancias]
    return "NEUTRA" if len(dias) == 1 or len(set(dias)) < len(dias) else "DISTRIBUIDA"


def codigo_actividad(plazas, grupos_por_nivel, nivel_de):
    """[Bloque-]{asignaturas con _}-{nivel si cubre el nivel entero, si no grupos con +}."""
    gs = sorted({g for p in plazas for g in p["grupos"]})
    niveles = {nivel_de[g] for g in gs}
    if len(niveles) == 1 and len(gs) > 1 and set(gs) == grupos_por_nivel[next(iter(niveles))]:
        sufijo = next(iter(niveles))
    else:
        sufijo = "+".join(gs)
    asigs = "_".join(sorted({p["asignatura"] for p in plazas}))
    return ("Bloque-" if len(plazas) > 1 else "") + asigs + "-" + sufijo


def requiere_tutor(plazas, dec):
    """True si alguna plaza es de tutoría (TUT*, Tut, PTVE, PTEV), salvo que una decisión fije otro valor."""
    d = dec.una("actividades.requiereTutor")
    if d and any(p["asignatura"] in d["valor"]["asignaturas"] for p in plazas):
        dec.usar(d)
        return bool(d["valor"]["requiereTutor"])
    return any(TUTORIAL.match(p["asignatura"]) for p in plazas)


def nombre_subgrupo(g, plaza, completo, combinaciones):
    """R5: {g}-Completo si el grupo está en una sola plaza; {g}-{asig} o {g}-{asig}-{profesores} si varias combinaciones."""
    if completo:
        return g + cargar_centro.SUFIJO_SUBGRUPO_PDC
    a = plaza["asignatura"]
    if len(combinaciones[(g, a)]) == 1:
        return "%s-%s" % (g, a)
    return "%s-%s-%s" % (g, a, "-".join(plaza["profesores"]))


def derivar_actividades(celdas, idx, grupos, dec, alias):
    """Actividades con plazas, aulas y subgrupos; y, aparte, el grupo de cada subgrupo y de cada plaza."""
    nivel_de = {g["codigo"]: g["nivel"] for g in grupos}
    grupos_por_nivel = defaultdict(set)
    for g in grupos:
        grupos_por_nivel[g["nivel"]].add(g["codigo"])
    crudas = []
    for firma, instancias in particionar(celdas).items():
        instancias = sorted(instancias)
        plazas = plazas_de(firma, instancias, idx)
        cuenta = Counter(g for p in plazas for g in p["grupos"])
        for p in plazas:
            p["completos"] = {g for g in p["grupos"] if cuenta[g] == 1}
        crudas.append((instancias, plazas))
    combinaciones = defaultdict(set)
    for _, plazas in crudas:
        for p in plazas:
            for g in p["grupos"]:
                if g not in p["completos"]:
                    combinaciones[(g, p["asignatura"])].add(tuple(p["profesores"]))
    actividades, sg_grupo, plaza_grupos = [], {}, {}
    for instancias, plazas in crudas:
        salida = []
        for p in plazas:
            fija, cand, usada = regla_de_aula(aulas_de_plaza(p, instancias, idx, alias), p, dec)
            if usada:
                dec.usar(usada)
            for g in p["grupos"]:
                s = nombre_subgrupo(g, p, g in p["completos"], combinaciones)
                if sg_grupo.setdefault(s, g) != g:
                    raise Aborto(2, "el subgrupo %s saldría de dos grupos (%s y %s)" % (s, sg_grupo[s], g))
            salida.append({
                "asignatura": p["asignatura"],
                "profesores": p["profesores"],
                "aulaFija": fija,
                "aulasCandidatas": cand,
                "subgrupos": sorted(nombre_subgrupo(g, p, g in p["completos"], combinaciones) for g in p["grupos"]),
            })
        asigs = {p["asignatura"] for p in plazas}
        codigo = codigo_actividad(plazas, grupos_por_nivel, nivel_de)
        for i, p in enumerate(plazas):
            plaza_grupos[(codigo, i)] = p["grupos"]
        actividades.append({
            "codigo": codigo,
            "asignatura": next(iter(asigs)) if len(asigs) == 1 else None,
            "duracionTramos": 1,
            "repeticionesPorSemana": len(instancias),
            "patronTemporal": patron_temporal(instancias),
            "requiereTutor": requiere_tutor(plazas, dec),
            "plazas": salida,
            "_referencia": {
                "gruposTocados": sorted({g for p in plazas for g in p["grupos"]}),
                "instanciasEnElHorarioReal": ["%s%d" % (DIAS[d - 1], t) for d, t in instancias],
            },
        })
    repetidos = [c for c, n in Counter(a["codigo"] for a in actividades).items() if n > 1]
    if repetidos:
        raise Aborto(2, "códigos de actividad repetidos (ninguna regla los distingue): %s" % sorted(repetidos))
    actividades.sort(key=lambda a: a["codigo"])
    return actividades, sg_grupo, plaza_grupos


def derivar_subgrupos(sg_grupo, grupos):
    """Subgrupos usados por las plazas, más el -Completo de cada PDC; todos mono-grupo."""
    automaticos = {g["codigo"] + cargar_centro.SUFIJO_SUBGRUPO_PDC: g["codigo"]
                   for g in grupos if g["tipo"] == "DIVERSIFICACION_PDC"}
    todos = dict(sg_grupo)
    todos.update(automaticos)
    return [{"codigo": s, "grupos": [todos[s]], "creadoAutomaticamentePorPDC": s in automaticos}
            for s in sorted(todos)]


# ------------------------------------------------------- asignaturas, aulas...

def derivar_codigos(celdas_volcado, celdas, campo):
    """Asignaturas o profesores presentes en la derivación, con su número de celdas en el volcado."""
    cuenta = Counter(c[campo] for c in celdas_volcado)
    return [{"codigo": x, "nombreCompleto": None, "celdas": cuenta[x]} for x in sorted({c[campo] for c in celdas})]


def derivar_tutorias(celdas, grupos, dec):
    """Tutor = profesor de TUT*/Tut/PTVE/PTEV del grupo (único); una decisión lo fija y vacía candidatos."""
    d = dec.una("tutorias.tutorPrincipal")
    fijados = d["valor"] if d else {}
    cand = defaultdict(set)
    for c in celdas:
        if TUTORIAL.match(c["asignatura"]):
            cand[c["grupo"]].add(c["profesor"])
    tutorias = []
    for g in grupos:
        g = g["codigo"]
        if g in fijados:
            tutorias.append({"grupo": g, "tutorPrincipal": fijados[g], "candidatos": [], "rol": "TUTOR_PRINCIPAL"})
            dec.usar(d)
        elif len(cand[g]) == 1:
            tutorias.append({"grupo": g, "tutorPrincipal": next(iter(cand[g])),
                             "candidatos": sorted(cand[g]), "rol": "TUTOR_PRINCIPAL"})
        else:
            raise Aborto(2, "grupo %s con %d candidatos a tutor %s y sin decisión" % (g, len(cand[g]), sorted(cand[g])))
    sobran = set(fijados) - {g["codigo"] for g in grupos}
    if sobran:
        raise Aborto(2, "decisión %s fija tutor de grupos que no existen: %s" % (d["id"], sorted(sobran)))
    return tutorias


def derivar_aulas(celdas_volcado, idx, aulas_fichero, actividades, dec, alias):
    """Aulas = las del volcado (con alias), las de aula-*.json y las altas; tipo por decisión."""
    sesiones = defaultdict(set)
    for c in celdas_volcado:
        if c["aula"] is not None:
            sesiones[alias.get(c["aula"], c["aula"])].add((c["dia"], c["tramo"], c["asignatura"], c["profesor"]))
    actuales = {alias.get(a, a) for a in idx.values() if a is not None}
    usadas = {x for a in actividades for p in a["plazas"] for x in ([p["aulaFija"]] if p["aulaFija"] else []) + p["aulasCandidatas"]}
    alta = dec.una("aulas.alta")
    altas = set(alta["valor"]) if alta else set()
    if alta:
        dec.usar(alta, len(altas))
    codigos = actuales | set(aulas_fichero) | altas | usadas
    tipos = dec.una("aulas.tipo")
    if tipos is None:
        raise Aborto(2, "falta la decisión aulas.tipo")
    aulas = []
    for a in sorted(codigos):
        tipo = tipos["valor"].get("porCodigo", {}).get(a) or tipos["valor"].get("porDefecto")
        if tipo not in cargar_centro.TIPOS_AULA:
            raise Aborto(2, "aula %s sin tipo válido en la decisión %s (%r)" % (a, tipos["id"], tipo))
        dec.usar(tipos)
        aulas.append({"codigo": a, "tipo": tipo, "capacidad": None, "edificio": None, "planta": None,
                      "sector": None, "celdasEnVolcado": len(sesiones.get(a, ())), "usadaPorCatalogo": a in usadas})
    return aulas


# ----------------------------------------------------------------------- parche

def aplicar_parche(parche, idx, actividades, plaza_grupos, dec, alias):
    """Cambia el aula de cada sesión del parche en el índice de celdas y recalcula la regla de aulas por plaza."""
    cambios = []
    antes = {(a["codigo"], i): (p["aulaFija"], tuple(p["aulasCandidatas"]))
             for a in actividades for i, p in enumerate(a["plazas"])}
    sesiones = []
    for n, e in enumerate(parche["entradas"], start=1):
        grupos = e["grupos"].split()
        claves = [k for k, au in idx.items() if k[0] in grupos and k[1] == e["dia"] and k[2] == e["tramo"]
                  and k[3] == e["asignatura"] and au == e["aula_antes"]]
        profes = {k[4] for k in claves}
        if len(profes) != 1 or sorted(k[0] for k in claves) != sorted(grupos):
            raise Aborto(2, "parche %d (%s, día %d tramo %d %s %s): %d celdas y profesores %s; no cae en una "
                            "sesión única" % (n, e["profesor"], e["dia"], e["tramo"], e["asignatura"],
                                              e["grupos"], len(claves), sorted(profes)))
        prof = profes.pop()
        sitio = [(a, i) for a in actividades for i, p in enumerate(a["plazas"])
                 if "%s%d" % (DIAS[e["dia"] - 1], e["tramo"]) in a["_referencia"]["instanciasEnElHorarioReal"]
                 and p["asignatura"] == e["asignatura"] and prof in p["profesores"]]
        if len(sitio) != 1:
            raise Aborto(2, "parche %d: la sesión cae en %d plazas" % (n, len(sitio)))
        sesiones.append((n, e, prof, sitio[0], claves))
    for n, e, prof, (a, i), claves in sesiones:
        for k in claves:
            idx[k] = e["aula_despues"]
    for a in actividades:
        inst = [(DIAS.index(s[0]) + 1, int(s[1:])) for s in a["_referencia"]["instanciasEnElHorarioReal"]]
        for i, p in enumerate(a["plazas"]):
            plaza = {"asignatura": p["asignatura"], "profesores": p["profesores"],
                     "grupos": plaza_grupos[(a["codigo"], i)]}
            p["aulaFija"], p["aulasCandidatas"], _ = regla_de_aula(aulas_de_plaza(plaza, inst, idx, alias), plaza, dec)
    tocadas = set()
    for n, e, prof, (a, i), claves in sesiones:
        p = a["plazas"][i]
        cambios.append({"n": n, "entrada": e, "profesor": prof, "actividad": a["codigo"], "plaza": i + 1,
                        "asignatura": p["asignatura"], "profesores": p["profesores"], "subgrupos": p["subgrupos"],
                        "antes": antes[(a["codigo"], i)], "despues": (p["aulaFija"], tuple(p["aulasCandidatas"]))})
        tocadas.add((a["codigo"], i))
    otras = [k for k in antes if k not in tocadas and antes[k] != _aula_de(actividades, k)]
    if otras:
        raise Aborto(2, "el parche cambió plazas que no nombra: %s" % otras)
    return cambios


def _aula_de(actividades, k):
    a = next(x for x in actividades if x["codigo"] == k[0])
    p = a["plazas"][k[1]]
    return (p["aulaFija"], tuple(p["aulasCandidatas"]))


# ------------------------------------------- reuniones, funciones y totales (S204)

def _cruzar():
    """cruzar-grupos-profesores.py, cargado solo si hace falta (arrastra extraer-horario.py)."""
    spec = importlib.util.spec_from_file_location(
        "cruzar_grupos_profesores", Path(__file__).resolve().parent / "cruzar-grupos-profesores.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)   # sin efectos al importar: main() va tras __main__
    return mod


def cargar_volcados_profesores(directorio):
    """Los profesor-*.json, en orden de fichero. Sin ninguno, o con otro modo, aborta."""
    ficheros = sorted(glob.glob(os.path.join(directorio, "profesor-*.json")))
    if not ficheros:
        raise Aborto(2, "no hay profesor-*.json en %s" % directorio)
    paginas = [json.loads(Path(f).read_text(encoding="utf-8")) for f in ficheros]
    if any(p["_meta"].get("modo") != "profesores" for p in paginas):
        raise Aborto(2, "profesor-*.json con modo distinto de «profesores» en %s" % directorio)
    if len({p["_meta"]["pagina"] for p in paginas}) != len(paginas):
        raise Aborto(2, "dos profesor-*.json con la misma página en %s" % directorio)
    return paginas


def mapa_de_paginas(paginas, pdfs):
    """Página -> código de profesor o «sin código», con la leyenda de los PDF de grupos y
    las dos funciones de cruzar-grupos-profesores.py; la correspondencia no se reescribe."""
    cr = _cruzar()
    try:
        leyenda = cr.leyenda_profesores([(os.path.basename(p), cr.eh.leer_pdf(p)) for p in pdfs])
        return cr.mapa_profesores({p["_meta"]["pagina"]: p["_meta"]["codigo_crudo"] for p in paginas}, leyenda), cr.SIN_CODIGO
    except (cr.ErrorCruce, cr.eh.ErrorExtraccion) as e:
        raise Aborto(2, "leyenda de profesores de los PDF de grupos: %s" % e)


def codigos_de_paginas(paginas, mapa, sin_codigo, dec):
    """Página -> código para las páginas que cuentan. Una página sin código entra por
    profesores.alta (casada por número y título) o se salta por profesores.paginasIgnoradas;
    si no, aborta. Devuelve también las ignoradas, para comprobar luego sus celdas."""
    alta = dec.una("profesores.alta")
    ignoradas_d = dec.una("profesores.paginasIgnoradas")
    altas = alta["valor"] if alta else {}
    ignoradas = {int(k): v for k, v in (ignoradas_d["valor"] if ignoradas_d else {}).items()}
    titulo = {p["_meta"]["pagina"]: p["_meta"]["codigo_crudo"] for p in paginas}
    por_pagina_alta = {}
    for cod, e in altas.items():
        if e.get("pagina") not in titulo or titulo[e["pagina"]] != e.get("titulo"):
            raise Aborto(2, "decisión %s: la página %r con título %r no está en los volcados de profesores"
                         % (alta["id"], e.get("pagina"), e.get("titulo")))
        if mapa[e["pagina"]] != sin_codigo:
            raise Aborto(2, "decisión %s: la página %d ya tiene código %s" % (alta["id"], e["pagina"], mapa[e["pagina"]]))
        if not (e.get("nombreCompleto") or "").strip():
            raise Aborto(2, "decisión %s: %s sin nombreCompleto" % (alta["id"], cod))
        por_pagina_alta[e["pagina"]] = cod
    for pag in ignoradas:
        if pag not in titulo:
            raise Aborto(2, "decisión %s: la página %d no está en los volcados de profesores" % (ignoradas_d["id"], pag))
        if mapa[pag] != sin_codigo or pag in por_pagina_alta:
            raise Aborto(2, "decisión %s: la página %d no es una página sin código libre" % (ignoradas_d["id"], pag))
    codigos = {}
    for pag in sorted(titulo):
        if mapa[pag] != sin_codigo:
            codigos[pag] = mapa[pag]
        elif pag in por_pagina_alta:
            codigos[pag] = por_pagina_alta[pag]
        elif pag not in ignoradas:
            raise Aborto(2, "página %d (%s) sin código en la leyenda y sin decisión profesores.alta ni "
                            "profesores.paginasIgnoradas" % (pag, titulo[pag]))
    repetidos = [c for c, n in Counter(codigos.values()).items() if n > 1]
    if repetidos:
        raise Aborto(2, "códigos de profesor en más de una página: %s" % sorted(repetidos))
    if alta:
        dec.usar(alta, len(altas))
    if ignoradas_d:
        dec.usar(ignoradas_d, len(ignoradas))
    return codigos, sorted(ignoradas)


def clasificar_celdas_profesor(paginas, codigos, ignoradas, dec):
    """D8. Celdas con grupo -> clase (cuentan para el total del profesor). Celdas sin grupo
    y de recreo -> su código tiene que estar en noClase.tipos o en noClase.ignorados; si no,
    aborta. Devuelve (clase por código de profesor, celdas no de clase tipadas, tipo por
    código, celdas ignoradas por código)."""
    tipos_d, ign_d = dec.una("noClase.tipos"), dec.una("noClase.ignorados")
    if tipos_d is None:
        raise Aborto(2, "falta la decisión noClase.tipos")
    tipo_de = {}
    for tipo, cods in tipos_d["valor"].items():
        if tipo not in TIPOS_NO_CLASE:
            raise Aborto(2, "decisión %s: tipo %r (válidos: %s)" % (tipos_d["id"], tipo, list(TIPOS_NO_CLASE)))
        for c in cods:
            if tipo_de.setdefault(c, tipo) != tipo:
                raise Aborto(2, "decisión %s: el código %s tiene dos tipos" % (tipos_d["id"], c))
    ignorados = ign_d["valor"] if ign_d else {}
    for c, motivo in ignorados.items():
        if not isinstance(motivo, str) or not motivo.strip():
            raise Aborto(2, "decisión %s: el código ignorado %s no lleva motivo" % (ign_d["id"], c))
    ambos = sorted(set(tipo_de) & set(ignorados))
    if ambos:
        raise Aborto(2, "códigos tipados e ignorados a la vez: %s" % ambos)
    clase, tipadas, ignoradas_n = Counter(), [], Counter()
    for p in paginas:
        pag = p["_meta"]["pagina"]
        if pag not in codigos and pag not in ignoradas:
            continue
        prof = codigos.get(pag)
        donde = "página %d (%s)" % (pag, p["_meta"]["codigo_crudo"])
        for c in p["celdas"]:
            a = c["asignatura"]
            if c["grupos"].strip():
                if a in tipo_de or a in ignorados:
                    raise Aborto(2, "%s: %s con grupo en día %d tramo %d, y es un código no de clase"
                                 % (donde, a, c["dia"], c["tramo"]))
                if prof is None:
                    raise Aborto(2, "%s: página ignorada con una celda de clase (%s)" % (donde, a))
                clase[prof] += 1
            elif a in tipo_de:
                if prof is None:
                    raise Aborto(2, "%s: página ignorada con una celda de %s" % (donde, a))
                tipadas.append((a, prof, c["dia"], c["tramo"]))
            elif a in ignorados:
                ignoradas_n[a] += 1
            else:
                raise Aborto(2, "%s: código %s sin grupo y sin decisión noClase (día %d tramo %d)"
                             % (donde, a, c["dia"], c["tramo"]))
        for c in p["recreo"]:
            if c["asignatura"] not in ignorados:
                raise Aborto(2, "%s: código %s en el recreo y sin decisión noClase.ignorados" % (donde, c["asignatura"]))
            ignoradas_n[c["asignatura"]] += 1
    sin_celdas = sorted((set(tipo_de) - {a for a, _, _, _ in tipadas}) | (set(ignorados) - set(ignoradas_n)))
    if sin_celdas:
        raise Aborto(2, "códigos de las decisiones noClase sin ninguna celda en los volcados: %s" % sin_celdas)
    dec.usar(tipos_d, len(tipo_de))
    if ign_d:
        dec.usar(ign_d, len(ignorados))
    return clase, tipadas, tipo_de, ignoradas_n


def _actividad_no_clase(codigo, tipo, asignatura, profesores, instancias):
    return {
        "codigo": codigo,
        "tipo": tipo,
        "asignatura": asignatura,
        "duracionTramos": 1,
        "repeticionesPorSemana": len(instancias),
        "patronTemporal": patron_temporal(instancias),
        "requiereTutor": False,
        "plazas": [{"asignatura": asignatura, "profesores": profesores, "aulaFija": None,
                    "aulasCandidatas": [], "subgrupos": []}],
        "_referencia": {"gruposTocados": [],
                        "instanciasEnElHorarioReal": ["%s%d" % (DIAS[d - 1], t) for d, t in instancias]},
    }


def derivar_no_clase(tipadas, tipo_de):
    """D1 y D2. REUNION: una por código, una plaza con todos sus profesores, repeticiones =
    tramos distintos; aborta si los profesores no son los mismos en todos sus tramos.
    FUNCION: una por (código, profesor), repeticiones = celdas."""
    por_codigo = defaultdict(list)
    for a, prof, d, t in tipadas:
        por_codigo[a].append((prof, d, t))
    actividades = []
    for a in sorted(por_codigo):
        if tipo_de[a] == "REUNION":
            por_franja = defaultdict(set)
            for prof, d, t in por_codigo[a]:
                por_franja[(d, t)].add(prof)
            grupos_de_profes = {frozenset(v) for v in por_franja.values()}
            if len(grupos_de_profes) != 1:
                raise Aborto(2, "reunión %s con profesores distintos según el tramo: %s"
                             % (a, {"%s%d" % (DIAS[d - 1], t): sorted(v) for (d, t), v in sorted(por_franja.items())}))
            actividades.append(_actividad_no_clase(a, "REUNION", a, sorted(next(iter(grupos_de_profes))),
                                                   sorted(por_franja)))
        else:
            por_prof = defaultdict(list)
            for prof, d, t in por_codigo[a]:
                por_prof[prof].append((d, t))
            for prof in sorted(por_prof):
                actividades.append(_actividad_no_clase("%s-%s" % (a, prof), "FUNCION", a, [prof], sorted(por_prof[prof])))
    return actividades


def nombres_de_no_clase(tipo_de, dec):
    """Cada código tipado lleva su nombre en una decisión asignaturas.nombre (lo lee
    extraer-nombres.py --decisiones); aquí solo se comprueba que estén todos y nada más."""
    nombres = {}
    for d in dec.todas("asignaturas.nombre"):
        for c, n in d["valor"].items():
            if c in nombres:
                raise Aborto(2, "el código %s tiene nombre en dos decisiones asignaturas.nombre" % c)
            if not isinstance(n, str) or not n.strip():
                raise Aborto(2, "decisión %s: nombre vacío para %s" % (d["id"], c))
            nombres[c] = n
        dec.usar(d, len(d["valor"]))
    faltan, sobran = sorted(set(tipo_de) - set(nombres)), sorted(set(nombres) - set(tipo_de))
    if faltan or sobran:
        raise Aborto(2, "asignaturas.nombre no casa con noClase.tipos: faltan %s, sobran %s" % (faltan, sobran))
    return nombres


def totales_de_grupo(directorio):
    """D7: (día, tramo) distintos de cada grupo-*.json, con el código por forma_corta. No pasa
    por la derivación: no lee decisiones ni exclusiones."""
    franjas = defaultdict(set)
    for f in sorted(glob.glob(os.path.join(directorio, "grupo-*.json"))):
        d = json.loads(Path(f).read_text(encoding="utf-8"))
        g = forma_corta(d["_meta"]["codigo_crudo"])
        for c in d["celdas"]:
            franjas[g].add((c["dia"], c["tramo"]))
    return {g: len(v) for g, v in franjas.items()}


def anadir_no_clase(catalogo, celdas_volcado, prof, dec, volcados):
    """Mete en el catálogo ya derivado las reuniones, funciones, sus asignaturas, los
    profesores nuevos y los totales declarados. Devuelve el resumen para el informe."""
    paginas, codigos, ignoradas = prof["paginas"], prof["codigos"], prof["ignoradas"]
    clase, tipadas, tipo_de, ignoradas_n = clasificar_celdas_profesor(paginas, codigos, ignoradas, dec)
    nuevas = derivar_no_clase(tipadas, tipo_de)
    nombres_de_no_clase(tipo_de, dec)
    existentes = {a["codigo"] for a in catalogo["actividades"]}
    choques = sorted(a["codigo"] for a in nuevas if a["codigo"] in existentes)
    if choques:
        raise Aborto(2, "códigos de actividad de reunión o función que ya existen: %s" % choques)
    asig_clase = {a["codigo"] for a in catalogo["asignaturas"]}
    if set(tipo_de) & asig_clase:
        raise Aborto(2, "códigos no de clase que ya son asignaturas de clase: %s" % sorted(set(tipo_de) & asig_clase))
    catalogo["actividades"] = sorted(catalogo["actividades"] + nuevas, key=lambda a: a["codigo"])
    cuenta = Counter(a for a, _, _, _ in tipadas)
    catalogo["asignaturas"] = sorted(catalogo["asignaturas"] + [{"codigo": c, "nombreCompleto": None, "celdas": cuenta[c]}
                                                                for c in sorted(set(cuenta))],
                                     key=lambda a: a["codigo"])
    en_catalogo = {p["codigo"] for p in catalogo["profesores"]}
    con_pagina = set(codigos.values())
    sin_pagina = sorted(en_catalogo - con_pagina)
    if sin_pagina:
        raise Aborto(2, "profesores del catálogo sin página en los volcados de profesores: %s" % sin_pagina)
    celdas_grupo = Counter(c["profesor"] for c in celdas_volcado)
    catalogo["profesores"] = sorted(catalogo["profesores"] + [{"codigo": c, "nombreCompleto": None, "celdas": celdas_grupo[c]}
                                                              for c in sorted(con_pagina - en_catalogo)],
                                    key=lambda p: p["codigo"])
    for p in catalogo["profesores"]:
        p["totalDeclarado"] = clase[p["codigo"]]
    tg = totales_de_grupo(volcados)
    if set(tg) != {g["codigo"] for g in catalogo["grupos"]}:
        raise Aborto(2, "los grupos de los volcados no son los del catálogo: %s"
                     % sorted(set(tg) ^ {g["codigo"] for g in catalogo["grupos"]}))
    for g in catalogo["grupos"]:
        g["totalDeclarado"] = tg[g["codigo"]]
    return {"nuevas": nuevas, "ignoradas_celdas": ignoradas_n, "paginas_ignoradas": ignoradas,
            "profesores_nuevos": sorted(con_pagina - en_catalogo),
            "celdas_tipadas": len(tipadas)}


# ------------------------------------------------------------------ conservación

def expandir(catalogo):
    """Catálogo -> conjunto de claves (grupo, día, tramo, asignatura, profesor)."""
    grupos_de = {s["codigo"]: s["grupos"] for s in catalogo["subgrupos"]}
    claves = set()
    for a in catalogo["actividades"]:
        for s in a["_referencia"]["instanciasEnElHorarioReal"]:
            d, t = DIAS.index(s[0]) + 1, int(s[1:])
            for p in a["plazas"]:
                for sg in p["subgrupos"]:
                    for g in grupos_de[sg]:
                        for prof in p["profesores"]:
                            claves.add((g, d, t, p["asignatura"], prof))
    return claves


def conservacion(catalogo, celdas_volcado, excluidas):
    """Compara catálogo y volcado clave a clave; aborta con rc=1 si una divergencia no la explica una decisión."""
    volcado = {clave(c) for c in celdas_volcado}
    cat = expandir(catalogo)
    solo_volcado, solo_catalogo = sorted(volcado - cat), sorted(cat - volcado)
    sin_explicar = [("solo en el volcado", k) for k in solo_volcado if k not in excluidas]
    sin_explicar += [("solo en el catálogo", k) for k in solo_catalogo]
    if sin_explicar:
        raise Aborto(1, "conservación: %d divergencia(s) sin explicar:\n%s" % (
            len(sin_explicar), "\n".join("  %s: %s" % (lado, k) for lado, k in sin_explicar)))
    agregado = lambda cl: Counter((g, a, p) for g, _, _, a, p in cl)  # noqa: E731
    return {"volcado": len(volcado), "catalogo": len(cat), "comunes": len(volcado & cat),
            "explicadas": [(k, excluidas[k]) for k in solo_volcado],
            "agregadas_volcado": len(agregado(volcado)), "agregadas_catalogo": len(agregado(cat))}


# -------------------------------------------------------------- invariantes y meta

def invariantes(catalogo, celdas, dec):
    """Comprueba I2, I4, I5, I7, S8 y S9 sobre el catálogo; rota alguna, aborta. Devuelve los textos de _meta."""
    notas = dec.una("meta.notasInvariantes")
    nota = (notas["valor"] if notas else {})
    if notas:
        dec.usar(notas)
    grupos = {g["codigo"]: g for g in catalogo["grupos"]}
    grupos_de = {s["codigo"]: s["grupos"] for s in catalogo["subgrupos"]}
    fallos = []
    i2 = sum(len(sgs) - len(set(sgs)) for a in catalogo["actividades"]
             for sgs in [[s for p in a["plazas"] for s in p["subgrupos"]]])
    if i2:
        fallos.append("I2: %d subgrupos repetidos" % i2)
    principales = Counter(t["grupo"] for t in catalogo["tutorias"] if t["rol"] == "TUTOR_PRINCIPAL")
    if set(principales) != set(grupos) or any(n != 1 for n in principales.values()):
        fallos.append("I4: tutorías %s" % dict(principales))
    pdcs = [g for g in grupos.values() if g["tipo"] == "DIVERSIFICACION_PDC"]
    if any(grupos[g["grupoPadre"]]["tipo"] != "ORDINARIO" for g in pdcs):
        fallos.append("I5")
    i7 = sum(1 for a in catalogo["actividades"] for p in a["plazas"] if not p["profesores"])
    if i7:
        fallos.append("I7: %d plazas sin profesor" % i7)
    tutor_de = defaultdict(set)
    for t in catalogo["tutorias"]:
        tutor_de[t["tutorPrincipal"]].add(t["grupo"])
    con_tutor = [a for a in catalogo["actividades"] if a["requiereTutor"]]
    s8 = [a["codigo"] for a in con_tutor if not any(
        tutor_de[prof] & {g for s in p["subgrupos"] for g in grupos_de[s]}
        for p in a["plazas"] for prof in p["profesores"])]
    if s8:
        fallos.append("S8: %s" % s8)
    ocupa = Counter()
    for a in catalogo["actividades"]:
        for s in a["_referencia"]["instanciasEnElHorarioReal"]:
            for g in a["_referencia"]["gruposTocados"]:
                ocupa[(g, s)] += 1
    dobles = [k for k, n in ocupa.items() if n > 1]
    if dobles:
        fallos.append("S9: %s" % dobles[:5])
    if fallos:
        raise Aborto(1, "invariantes rotas: %s" % fallos)
    lectivos = sum(1 for t in catalogo["jornada"]["diaTipo"] if t["esLectivo"]) * len(catalogo["jornada"]["dias"])
    slots = len(grupos) * lectivos
    usos = Counter(s for a in catalogo["actividades"] for s in {s for p in a["plazas"] for s in p["subgrupos"]})
    reutilizados = sum(1 for s, n in usos.items() if n > 1 and not s.endswith(cargar_centro.SUFIJO_SUBGRUPO_PDC))
    fijados = sum(1 for t in catalogo["tutorias"] if not t["candidatos"])
    i4 = "OK - un TUTOR_PRINCIPAL por grupo; %d/%d, %d derivados" % (len(principales), len(grupos), len(grupos) - fijados)
    if fijados:
        i4 += " y %d %s" % (fijados, nota.get("I4", "fijados por decision"))
    s9 = ("OK - los %d slots (%d grupos x %d tramos) quedan cubiertos por exactamente una actividad"
          % (slots, len(grupos), lectivos) if len(ocupa) == slots else
          "OK sin dobles - %d de %d slots cubiertos por exactamente una actividad; %d sin actividad"
          % (len(ocupa), slots, slots - len(ocupa)))
    return {
        "I1": _con_nota("cada grupo queda cubierto por sus subgrupos en cada bloque; poblacion real NO derivable", nota, "I1"),
        "I2": "OK - %d subgrupos repetidos entre plazas de una misma actividad" % i2,
        "I4": i4,
        "I5": "OK - los %d PDC tienen grupoPadre ORDINARIO" % len(pdcs),
        "I6": _con_nota("%d subgrupos parciales reutilizados en >1 actividad" % reutilizados, nota, "I6"),
        "I7": "OK - %d plazas sin profesor" % i7,
        "S8": "OK - %d actividades requiereTutor, todas con un tutor del grupo cubierto en alguna plaza" % len(con_tutor),
        "S9": s9,
    }


def _con_nota(texto, nota, k):
    return texto + (" " + nota[k] if nota.get(k) else "")


def meta(catalogo, fuente, invs):
    grupos = catalogo["grupos"]
    pdc = sum(1 for g in grupos if g["tipo"] == "DIVERSIFICACION_PDC")
    auto = sum(1 for s in catalogo["subgrupos"] if s["creadoAutomaticamentePorPDC"])
    envios = {"jornada": 1, "niveles": len(catalogo["niveles"]), "asignaturas": len(catalogo["asignaturas"]),
              "profesores": len(catalogo["profesores"]), "aulas": len(catalogo["aulas"]),
              "grupos": len(grupos) - pdc, "pdc": pdc, "tutores": len(catalogo["tutorias"]),
              "subgrupos": len(catalogo["subgrupos"]) - auto, "actividades": len(catalogo["actividades"])}
    envios["TOTAL"] = sum(envios.values())
    return {
        "proposito": PROPOSITO, "fuente": fuente, "normativa": NORMATIVA, "advertencia": ADVERTENCIA,
        "ordenDeTecleo": list(ORDEN_DE_TECLEO), "enviosDeFormulario": envios,
        "resumen": {"niveles": len(catalogo["niveles"]), "asignaturas": len(catalogo["asignaturas"]),
                    "profesores": len(catalogo["profesores"]), "aulas": len(catalogo["aulas"]),
                    "grupos": len(grupos), "subgrupos": len(catalogo["subgrupos"]),
                    "actividades": len(catalogo["actividades"]),
                    "plazas": sum(len(a["plazas"]) for a in catalogo["actividades"]),
                    "sesionesSemanales": sum(a["repeticionesPorSemana"] for a in catalogo["actividades"])},
        "verificacionInvariantes": invs,
    }


def prevalidar_con_cargador(catalogo):
    """El catálogo tiene que pasar la prevalidación de cargar-centro.py (nombres aparte: llegan en F3)."""
    nombres = {"asignaturas": {a["codigo"]: {} for a in catalogo["asignaturas"]},
               "profesores": {p["codigo"]: {} for p in catalogo["profesores"]}}
    v = cargar_centro.prevalidar(catalogo, nombres)
    if v:
        raise Aborto(1, "la prevalidación de cargar-centro.py da %d violaciones: %s" % (len(v), v[:5]))
    return 0


# ----------------------------------------------------------------------- informe

def ruta_legible(p):
    p = Path(p).resolve()
    try:
        return p.relative_to(RAIZ).as_posix()
    except ValueError:
        return p.as_posix()


def informe(cat, args, dec, cons, cambios, celdas_volcado, n_ficheros, aulas_fichero, crudos, no_clase=None):
    o = ["# Informe de conservación del catálogo derivado", ""]
    o.append("Generado por `tools/carga-centro/derivar-catalogo.py`. Volcados: `%s` (%d grupo-*.json, "
             "%d aula-*.json). Decisiones: `%s`. Parche: %s." % (
                 ruta_legible(args.volcados), n_ficheros, len(aulas_fichero), ruta_legible(args.decisiones),
                 "`%s`" % ruta_legible(args.parche) if args.parche else "ninguno"))
    o += ["", "## Cifras", ""]
    r = cat["_meta"]["resumen"]
    plazas = [p for a in cat["actividades"] for p in a["plazas"]]
    o.append("| familia | n |")
    o.append("|---|---|")
    for k, v in (("grupos", r["grupos"]), ("niveles", r["niveles"]), ("asignaturas", r["asignaturas"]),
                 ("profesores", r["profesores"]), ("aulas", r["aulas"]), ("actividades", r["actividades"]),
                 ("plazas", r["plazas"]), ("plazas con aulaFija", sum(1 for p in plazas if p["aulaFija"])),
                 ("plazas con aulasCandidatas", sum(1 for p in plazas if p["aulasCandidatas"])),
                 ("subgrupos", r["subgrupos"]), ("sesiones semanales", r["sesionesSemanales"]),
                 ("envíos de la carga", cat["_meta"]["enviosDeFormulario"]["TOTAL"])):
        o.append("| %s | %d |" % (k, v))
    o += ["", "Prevalidación de `cargar-centro.py` sobre el catálogo (nombres aparte): 0 violaciones.", ""]
    o += ["## Horas por grupo", "", "| grupo | catálogo | franjas del volcado | celdas del volcado |", "|---|---|---|---|"]
    horas = Counter()
    for a in cat["actividades"]:
        for g in a["_referencia"]["gruposTocados"]:
            horas[g] += a["repeticionesPorSemana"]
    franjas = Counter(g for g, _, _ in {(c["grupo"], c["dia"], c["tramo"]) for c in celdas_volcado})
    celdas = Counter(c["grupo"] for c in celdas_volcado)
    for g in cat["grupos"]:
        g = g["codigo"]
        o.append("| %s | %d | %d | %d |" % (g, horas[g], franjas[g], celdas[g]))
    o += ["", "## Conservación", "",
          "Clave `(grupo, día, tramo, asignatura, profesor)`, como conjunto (M2 de S198).", "",
          "| | n |", "|---|---|",
          "| claves del volcado | %d |" % cons["volcado"], "| claves del catálogo | %d |" % cons["catalogo"],
          "| comunes | %d |" % cons["comunes"],
          "| agregadas (grupo, asignatura, profesor) volcado / catálogo | %d / %d |" % (cons["agregadas_volcado"], cons["agregadas_catalogo"]),
          "| divergencias explicadas por decisión | %d |" % len(cons["explicadas"]),
          "| divergencias sin explicar | 0 |", ""]
    for k, i in cons["explicadas"]:
        o.append("- solo en el volcado `%s`: explicada por la decisión `%s`." % (k, i))
    o += ["", "## Parche de aulas", ""]
    if not cambios:
        o.append("Sin parche.")
    else:
        o.append("%d sesiones aplicadas. La regla de aulas por plaza se recalcula después del parche; "
                 "ninguna otra plaza cambia." % len(cambios))
        o += ["", "| # | profesor (parche) | código | día·tramo | asignatura | grupos | aula | actividad · plaza | aula de la plaza antes → después | confirmación |",
              "|---|---|---|---|---|---|---|---|---|---|"]
        fmt = lambda x: ("fija %s" % x[0]) if x[0] else ("candidatas %s" % ", ".join(x[1]))  # noqa: E731
        for c in cambios:
            e = c["entrada"]
            o.append("| %d | %s | %s | %s%d | %s | %s | %s → %s | `%s` · %d (%s/%s) | %s → %s | %s |" % (
                c["n"], e["profesor"], c["profesor"], DIAS[e["dia"] - 1], e["tramo"], e["asignatura"], e["grupos"],
                e["aula_antes"], e["aula_despues"], c["actividad"], c["plaza"], c["asignatura"],
                "+".join(c["profesores"]), fmt(c["antes"]), fmt(c["despues"]), e.get("confirmacion", "")))
        plazas_tocadas = sorted({(c["actividad"], c["plaza"]) for c in cambios})
        o += ["", "Plazas modificadas: %d." % len(plazas_tocadas)]
    if no_clase is not None:
        o += informe_no_clase(cat, args, no_clase)
    o += ["", "## Decisiones", "", "| id | fija | aplicaciones | fuente |", "|---|---|---|---|"]
    for d in dec.lista:
        o.append("| `%s` | %s | %d | %s |" % (d["id"], d["fija"], dec.usos[d["id"]], d["fuente"]))
    sin = [d["id"] for d in dec.lista if d["fuente"].strip().lower() == "sin fuente"]
    o += ["", "Entradas «sin fuente»: %d%s." % (len(sin), (" (" + ", ".join("`%s`" % s for s in sin) + ")") if sin else "")]
    muertas = [d["id"] for d in dec.lista if not dec.usos[d["id"]]]
    if muertas:
        o.append("Decisiones sin ninguna aplicación: %s." % ", ".join("`%s`" % m for m in muertas))
    o += ["", "## Grupos", "", "| título del volcado | grupo |", "|---|---|"]
    for crudo, g in sorted(crudos.items(), key=lambda x: x[1]):
        o.append("| %s | %s |" % (crudo, g))
    return "\n".join(o) + "\n"


def informe_no_clase(cat, args, nc):
    """Sección de S204: solo con --volcados-profesores."""
    o = ["", "## Reuniones, funciones y totales declarados", "",
         "Volcados de profesores: `%s`. PDF de grupos (leyenda «Profesores:»): %s." % (
             ruta_legible(args.volcados_profesores), ", ".join("`%s`" % os.path.basename(p) for p in args.pdf_grupos)), "",
         "| actividad | tipo | profesores | repeticiones | instancias |", "|---|---|---|---|---|"]
    for a in nc["nuevas"]:
        o.append("| %s | %s | %s | %d | %s |" % (a["codigo"], a["tipo"], " ".join(a["plazas"][0]["profesores"]),
                                                 a["repeticionesPorSemana"], " ".join(a["_referencia"]["instanciasEnElHorarioReal"])))
    representadas = sum(len(a["plazas"][0]["profesores"]) * a["repeticionesPorSemana"] for a in nc["nuevas"])
    o += ["", "Celdas de profesor representadas (profesores × repeticiones): %d de %d sin grupo tipadas." % (
        representadas, nc["celdas_tipadas"]),
          "Celdas ignoradas por decisión: %s." % ", ".join("%s %d" % kv for kv in sorted(nc["ignoradas_celdas"].items())),
          "Páginas ignoradas por decisión: %s. Profesores nuevos: %s." % (
              ", ".join(str(p) for p in nc["paginas_ignoradas"]) or "ninguna", ", ".join(nc["profesores_nuevos"]) or "ninguno"),
          "Totales declarados: %d profesores, suma %d; %d grupos y PDC, suma %d." % (
              len(cat["profesores"]), sum(p["totalDeclarado"] for p in cat["profesores"]),
              len(cat["grupos"]), sum(g["totalDeclarado"] for g in cat["grupos"]))]
    return o


# --------------------------------------------------------------------------- main

def derivar(volcados, decisiones_doc, parche_doc=None, profesores=None):
    """Toda la derivación en memoria. Devuelve (catálogo, contexto para el informe).
    profesores (S204, opcional): {"paginas", "mapa", "sin_codigo"} de los profesor-*.json;
    sin él, la salida es la de S198."""
    dec = Decisiones(decisiones_doc)
    if profesores is not None:
        codigos, ignoradas = codigos_de_paginas(profesores["paginas"], profesores["mapa"], profesores["sin_codigo"], dec)
        profesores = dict(profesores, codigos=codigos, ignoradas=ignoradas)
    celdas_volcado, crudos, aulas_fichero, n_ficheros = cargar_volcados(volcados)
    indice_de_aulas(celdas_volcado)
    celdas, excluidas = excluir_celdas(celdas_volcado, dec)
    idx = indice_de_aulas(celdas)
    alias_d = dec.una("aulas.alias")
    alias = alias_d["valor"] if alias_d else {}
    if alias_d:
        dec.usar(alias_d, sum(1 for au in idx.values() if au in alias))
    codigos_grupo = {c["grupo"] for c in celdas}
    niveles = derivar_niveles(codigos_grupo, dec)
    grupos = derivar_grupos(codigos_grupo, niveles)
    actividades, sg_grupo, plaza_grupos = derivar_actividades(celdas, idx, grupos, dec, alias)
    cambios = aplicar_parche(parche_doc, idx, actividades, plaza_grupos, dec, alias) if parche_doc else []
    catalogo = {
        "_meta": None,
        "jornada": derivar_jornada(celdas, dec),
        "niveles": niveles,
        "asignaturas": derivar_codigos(celdas_volcado, celdas, "asignatura"),
        "profesores": derivar_codigos(celdas_volcado, celdas, "profesor"),
        "aulas": derivar_aulas(celdas_volcado, idx, aulas_fichero, actividades, dec, alias),
        "grupos": grupos,
        "tutorias": derivar_tutorias(celdas, grupos, dec),
        "subgrupos": derivar_subgrupos(sg_grupo, grupos),
        "actividades": actividades,
    }
    no_clase = anadir_no_clase(catalogo, celdas_volcado, profesores, dec, volcados) if profesores is not None else None
    cons = conservacion(catalogo, celdas_volcado, excluidas)
    invs = invariantes(catalogo, celdas, dec)
    fuente = "%s/ (%d grupo-*.json%s)" % (ruta_legible(volcados), n_ficheros,
                                          ", %d aula-*.json" % len(aulas_fichero) if aulas_fichero else "")
    catalogo["_meta"] = meta(catalogo, fuente, invs)
    prevalidar_con_cargador(catalogo)
    return catalogo, {"dec": dec, "cons": cons, "cambios": cambios, "celdas_volcado": celdas_volcado,
                      "n_ficheros": n_ficheros, "aulas_fichero": aulas_fichero, "crudos": crudos,
                      "no_clase": no_clase}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--volcados", required=True)
    ap.add_argument("--decisiones", required=True)
    ap.add_argument("--parche")
    ap.add_argument("--salida", required=True)
    ap.add_argument("--volcados-profesores")
    ap.add_argument("--pdf-grupos", nargs="+")
    args = ap.parse_args(argv)
    try:
        if not os.path.isabs(args.salida):
            raise Aborto(2, "--salida debe ser una ruta absoluta: %s" % args.salida)
        if os.path.exists(args.salida) and os.listdir(args.salida):
            raise Aborto(2, "--salida no está vacía: %s" % args.salida)
        if bool(args.volcados_profesores) != bool(args.pdf_grupos):
            raise Aborto(2, "--volcados-profesores y --pdf-grupos van juntos")
        decisiones = json.loads(Path(args.decisiones).read_text(encoding="utf-8"))
        parche = json.loads(Path(args.parche).read_text(encoding="utf-8")) if args.parche else None
        profesores = None
        if args.volcados_profesores:
            paginas = cargar_volcados_profesores(args.volcados_profesores)
            mapa, sin_codigo = mapa_de_paginas(paginas, args.pdf_grupos)
            profesores = {"paginas": paginas, "mapa": mapa, "sin_codigo": sin_codigo}
        catalogo, ctx = derivar(args.volcados, decisiones, parche, profesores)
        texto = informe(catalogo, args, ctx["dec"], ctx["cons"], ctx["cambios"], ctx["celdas_volcado"],
                        ctx["n_ficheros"], ctx["aulas_fichero"], ctx["crudos"], ctx["no_clase"])
    except Aborto as e:
        print("ABORTA (rc=%d): %s" % (e.rc, e), file=sys.stderr)
        return e.rc
    os.makedirs(args.salida, exist_ok=True)
    Path(args.salida, SALIDA_CATALOGO).write_text(json.dumps(catalogo, ensure_ascii=False, indent=1), encoding="utf-8")
    Path(args.salida, SALIDA_INFORME).write_text(texto, encoding="utf-8")
    r = catalogo["_meta"]["resumen"]
    print("Escrito en %s: %d grupos, %d actividades, %d plazas, %d subgrupos; conservación %d/%d claves, "
          "%d explicadas; parche %d sesiones." % (args.salida, r["grupos"], r["actividades"], r["plazas"],
                                                  r["subgrupos"], ctx["cons"]["comunes"], ctx["cons"]["volcado"],
                                                  len(ctx["cons"]["explicadas"]), len(ctx["cambios"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
