#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Métricas de la comparación con el oficial de 2026/2027 (O-comparacion, S228).

Uso:
    python3 -I -B tools/carga-centro/comparar-con-oficial.py --db BASE --volcados CARPETA \
        [--horario ID ...] --salida CARPETA

Calcula M1–M6 tal como las define el contrato sellado
docs/horario-referencia/2026-2027/METRICAS-COMPARACION.md (commit 1344596), sobre el oficial
(los grupo-*.json y profesor-*.json de --volcados) y sobre cada --horario de --db. Sin --horario
calcula solo el oficial. La base se abre en mode=ro.

Escribe en --salida:
  metricas.tsv     métrica, lado, valor
  por-entidad.tsv  métrica, entidad, oficial y el valor en cada horario (solo métricas con dirección)

Las tuplas (grupo, día, tramo, asignatura, profesor) NO se construyen aquí: salen de
entradas_del_pdf y entradas_de_la_base de verificar-conservacion.py, que se importa (C1).
"""
import argparse
import glob
import importlib.util
import json
import sqlite3
import sys
from collections import Counter, defaultdict
from pathlib import Path

AQUI = Path(__file__).resolve().parent


def _cargar_verificador():
    spec = importlib.util.spec_from_file_location("verificar_conservacion", AQUI / "verificar-conservacion.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


vc = _cargar_verificador()

DIAS = {"LUNES": 1, "MARTES": 2, "MIERCOLES": 3, "JUEVES": 4, "VIERNES": 5}
TRAMOS_LECTIVOS = 6
CLASES_PATRON = ("DISTRIBUIDA", "AGRUPADA", "NEUTRA", "mixto")


# ------------------------------------------------------------ normalización (P1)

def posiciones_lectivas(filas):
    """[(dia, orden, es_lectivo)] de tramo_semanal -> {(dia, orden): (día 1..5, tramo o None)}.

    El tramo es la posición del tramo entre los LECTIVOS de su día, por orden (P1); un tramo no
    lectivo da None."""
    por_dia = defaultdict(list)
    for dia, orden, lectivo in filas:
        por_dia[dia].append((orden, bool(lectivo)))
    pos = {}
    for dia, tramos in por_dia.items():
        n = 0
        for orden, lectivo in sorted(tramos):
            if lectivo:
                n += 1
            pos[(dia, orden)] = (DIAS[dia], n if lectivo else None)
    return pos


def normaliza_dia_tramo(dia, orden, pos):
    """(día, tramo) en 1..5 / 1..6. Si ya viene numérico, no se normaliza dos veces (P1)."""
    if isinstance(dia, int):
        return dia, orden
    d, t = pos[(dia, orden)]
    if t is None:
        raise SystemExit("Tramo no lectivo en una tupla: %s %s" % (dia, orden))
    return d, t


def comprueba_rango(tuplas, lado):
    malas = [x for x in tuplas if not (1 <= x[1] <= 5 and 1 <= x[2] <= TRAMOS_LECTIVOS)]
    if malas:
        raise SystemExit("%s: %d tuplas fuera de 1..5 / 1..%d, p. ej. %s"
                         % (lado, len(malas), TRAMOS_LECTIVOS, malas[0]))


def comprueba_p2(oficial, horario, lado):
    """El multiconjunto (grupo, asignatura, profesor) es igual en los dos lados, o aborta (P2)."""
    a = Counter((g, asig, p) for g, _d, _t, asig, p in oficial)
    b = Counter((g, asig, p) for g, _d, _t, asig, p in horario)
    if a != b:
        dif = sorted(k for k in set(a) | set(b) if a.get(k, 0) != b.get(k, 0))
        raise SystemExit("%s: el multiconjunto (grupo, asignatura, profesor) no casa con el oficial "
                         "(%d claves distintas, p. ej. %s)" % (lado, len(dif), dif[:3]))


# --------------------------------------------------------------------- métricas

def m1(tuplas):
    """Huecos del profesorado: por (profesor, día), tramos sin clase entre la primera y la última."""
    tramos = defaultdict(set)
    for _g, d, t, _a, p in tuplas:
        tramos[(p, d)].add(t)
    huecos = {k: max(ts) - min(ts) + 1 - len(ts) for k, ts in tramos.items()}
    semana = Counter()
    for (p, _d), h in huecos.items():
        semana[p] += h
    return {
        "total": sum(huecos.values()),
        "max_profesor_semana": max(semana.values(), default=0),
        "pares_3_o_mas": sum(1 for h in huecos.values() if h >= 3),
        "por_entidad": dict(semana),
    }


def m2(tuplas, patron):
    """Asignatura repetida en el día: por (grupo, asignatura), franjas (día, tramo) menos días."""
    franjas = defaultdict(set)
    for g, d, t, a, _p in tuplas:
        franjas[(g, a)].add((d, t))
    rep = {k: len(fs) - len({d for d, _t in fs}) for k, fs in franjas.items()}
    minimo = {k: max(0, len(fs) - 5) for k, fs in franjas.items()}
    res = {"total": sum(rep.values()), "total_minimo": sum(minimo.values()),
           "por_entidad": {"%s|%s" % k: v for k, v in rep.items()}}
    for clase in CLASES_PATRON + ("sin_patron",):
        claves = [k for k in rep if patron.get(k, "sin_patron") == clase]
        if claves or clase != "sin_patron":
            res[clase] = sum(rep[k] for k in claves)
            res[clase + "_minimo"] = sum(minimo[k] for k in claves)
    return res


def m3(tuplas):
    """Días con clase por profesor: su suma y cuántos profesores tienen clase los 5 días."""
    dias = defaultdict(set)
    for _g, d, _t, _a, p in tuplas:
        dias[p].add(d)
    return {"suma_dias": sum(len(v) for v in dias.values()),
            "profesores_5_dias": sum(1 for v in dias.values() if len(v) == 5)}


def m4(ordinarias, recreo):
    """a) Σ cuadrados de ordinarias por tramo lectivo; b) Σ cuadrados de las de recreo por día.

    ordinarias: [(profesor, día, tramo)]; recreo: [(profesor, día)], uno por (profesor, día)."""
    por_tramo = Counter((d, t) for _p, d, t in ordinarias)
    por_dia = Counter(d for _p, d in set(recreo))
    return {"a": sum(n * n for n in por_tramo.values()),
            "b": sum(n * n for n in por_dia.values()),
            "por_entidad_a": {"%d-%d" % k: n for k, n in por_tramo.items()},
            "por_entidad_b": {str(d): n for d, n in por_dia.items()}}


def m5(aulas):
    """Aulas por grupo y día: Σ max(0, aulas distintas no nulas − 1). aulas: [(grupo, día, aula|None)]."""
    usadas = defaultdict(set)
    nulas = 0
    for g, d, aula in aulas:
        if aula is None:
            nulas += 1
            usadas[(g, d)]
        else:
            usadas[(g, d)].add(aula)
    por = {"%s|%d" % k: max(0, len(v) - 1) for k, v in usadas.items()}
    return {"suma": sum(por.values()), "sin_aula": nulas, "por_entidad": por}


def m6(oficial, horario):
    """Porcentaje de tuplas del horario que también están en el oficial."""
    horario = set(horario)
    return 100.0 * len(horario & set(oficial)) / len(horario) if horario else 0.0


# ------------------------------------------------------------- lectura: oficial

def oficial(volcados):
    """Tuplas, aulas por (grupo, día), guardias ordinarias y de recreo del oficial."""
    tuplas, mapa, sin_regla, _n = vc.entradas_del_pdf(volcados)
    if sin_regla:
        raise SystemExit("Grupos del volcado sin forma corta: %s" % sin_regla)
    aulas = []
    for f in sorted(glob.glob(str(volcados / "grupo-*.json"))):
        d = json.loads(Path(f).read_text(encoding="utf-8"))
        corto = mapa[d["_meta"]["codigo_crudo"]]
        aulas.extend((corto, c["dia"], c["aula"]) for c in d["celdas"])
    ordinarias, recreo = [], []
    for f in sorted(glob.glob(str(volcados / "profesor-*.json"))):
        d = json.loads(Path(f).read_text(encoding="utf-8"))
        prof = d["_meta"]["codigo_crudo"]
        ordinarias.extend((prof, c["dia"], c["tramo"]) for c in d["celdas"] if c["asignatura"] == "G")
        recreo.extend((prof, c["dia"]) for c in d.get("recreo") or [] if c["asignatura"] in ("GR", "GRBib"))
    return set(tuplas), aulas, ordinarias, recreo


# --------------------------------------------------------------- lectura: base

def patrones(con):
    """(grupo, asignatura) -> patrón de sus actividades, o «mixto» si tiene varios (C4)."""
    vistos = defaultdict(set)
    for g, a, pt in con.execute("""
            SELECT DISTINCT g.codigo, a.codigo, act.patron_temporal
            FROM plaza p
            JOIN actividad            act ON act.id = p.actividad_id
            JOIN asignatura           a   ON a.id = p.asignatura_id
            JOIN plaza_subgrupo       ps  ON ps.plaza_id = p.id
            JOIN subgrupo_grupo       sg  ON sg.subgrupo_id = ps.subgrupo_id
            JOIN grupo_administrativo g   ON g.id = sg.grupo_id"""):
        vistos[(g, a)].add(pt)
    return {k: (next(iter(v)) if len(v) == 1 else "mixto") for k, v in vistos.items()}


def horario_de_la_base(con, horario_id, pos):
    tuplas = {(g,) + normaliza_dia_tramo(d, t, pos) + (a, p)
              for g, d, t, a, p in vc.entradas_de_la_base(con, horario_id)}
    if not tuplas:
        raise SystemExit("El horario %d no tiene sesiones." % horario_id)
    sesiones_sin_aula = set()
    aulas = []
    for sid, g, dia, orden, aula in con.execute("""
            SELECT DISTINCT s.id, g.codigo, t.dia, t.orden, au.codigo
            FROM sesion s
            JOIN plaza            p   ON p.id  = s.plaza_id
            JOIN asignatura       a   ON a.id  = p.asignatura_id
            JOIN plaza_profesor   pp  ON pp.plaza_id = p.id
            JOIN profesor         pr  ON pr.id = pp.profesor_id
            JOIN plaza_subgrupo   ps  ON ps.plaza_id = p.id
            JOIN subgrupo_grupo   sg  ON sg.subgrupo_id = ps.subgrupo_id
            JOIN grupo_administrativo g ON g.id = sg.grupo_id
            JOIN tramo_semanal    t   ON t.id  = s.tramo_inicio_id
            LEFT JOIN aula        au  ON au.id = s.aula_id
            WHERE s.horario_id = ?""", (horario_id,)):
        d, _t = normaliza_dia_tramo(dia, orden, pos)
        aulas.append((g, d, aula))
        if aula is None:
            sesiones_sin_aula.add(sid)
    ordinarias, recreo = [], []
    for prof, tipo, dia, orden in con.execute("""
            SELECT gu.profesor_id, gu.tipo, t.dia, t.orden
            FROM guardia gu JOIN tramo_semanal t ON t.id = gu.tramo_id
            WHERE gu.horario_id = ?""", (horario_id,)):
        if tipo == "ORDINARIA":
            ordinarias.append((prof,) + normaliza_dia_tramo(dia, orden, pos))
        else:
            recreo.append((prof, DIAS[dia]))
    return tuplas, aulas, len(sesiones_sin_aula), ordinarias, recreo


# --------------------------------------------------------------------- salida

def metricas_de(tuplas, aulas, ordinarias, recreo, patron):
    r1, r2, r3, r4, r5 = m1(tuplas), m2(tuplas, patron), m3(tuplas), m4(ordinarias, recreo), m5(aulas)
    filas = [("M1_total", r1["total"]), ("M1_max_profesor_semana", r1["max_profesor_semana"]),
             ("M1_pares_3_o_mas", r1["pares_3_o_mas"]),
             ("M2_total", r2["total"]), ("M2_total_minimo", r2["total_minimo"])]
    filas += [("M2_" + k, v) for k, v in r2.items() if k not in ("total", "total_minimo", "por_entidad")]
    filas += [("M3_suma_dias", r3["suma_dias"]), ("M3_profesores_5_dias", r3["profesores_5_dias"]),
              ("M4a", r4["a"]), ("M4b", r4["b"]), ("M5", r5["suma"])]
    entidades = {"M1": r1["por_entidad"], "M2": r2["por_entidad"], "M4a": r4["por_entidad_a"],
                 "M4b": r4["por_entidad_b"], "M5": r5["por_entidad"]}
    return filas, entidades, r5["sin_aula"]


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("--db", required=True, help="base sqlite (se abre en mode=ro)")
    p.add_argument("--volcados", required=True, help="carpeta con los grupo-*.json y profesor-*.json")
    p.add_argument("--horario", type=int, nargs="+", action="extend", default=[],
                   help="id de horario (repetible); sin él, solo el oficial")
    p.add_argument("--salida", required=True, help="carpeta de salida")
    args = p.parse_args(argv)

    ruta = Path(args.db)
    if not ruta.is_file():
        raise SystemExit("No existe la base: %s" % ruta)
    con = sqlite3.connect("file:%s?mode=ro" % ruta, uri=True)
    volcados = Path(args.volcados).resolve()
    pos = posiciones_lectivas(con.execute("SELECT dia, orden, es_lectivo FROM tramo_semanal"))
    patron = patrones(con)

    of_tuplas, of_aulas, of_ord, of_rec = oficial(volcados)
    comprueba_rango(of_tuplas, "oficial")
    filas, entidades, nulas = metricas_de(of_tuplas, of_aulas, of_ord, of_rec, patron)
    tsv = [("métrica", "lado", "valor")]
    tsv += [(m, "oficial", v) for m, v in filas]
    tsv.append(("M5_sin_aula_celdas", "oficial", nulas))
    en_base = {r[0] for r in con.execute("SELECT codigo FROM aula")}
    fuera = sorted({a for _g, _d, a in of_aulas if a is not None} - en_base)
    tsv.append(("P3_aulas_oficial_sin_base", "oficial", len(fuera)))
    tsv.append(("P3_codigos", "oficial", ",".join(fuera)))

    por_lado = [entidades]
    for h in args.horario:
        lado = "horario-%d" % h
        tuplas, aulas, sin_aula, ordi, rec = horario_de_la_base(con, h, pos)
        comprueba_rango(tuplas, lado)
        comprueba_p2(of_tuplas, tuplas, lado)
        f, e, _ = metricas_de(tuplas, aulas, ordi, rec, patron)
        tsv += [(m, lado, v) for m, v in f]
        tsv.append(("M5_sin_aula_sesiones", lado, sin_aula))
        tsv.append(("M6", lado, round(m6(of_tuplas, tuplas), 2)))
        por_lado.append(e)

    salida = Path(args.salida)
    salida.mkdir(parents=True, exist_ok=True)
    (salida / "metricas.tsv").write_text("".join("\t".join(map(str, f)) + "\n" for f in tsv), encoding="utf-8")
    cab = ["métrica", "entidad", "oficial"] + ["horario-%d" % h for h in args.horario]
    lineas = ["\t".join(cab)]
    for metrica in ("M1", "M2", "M4a", "M4b", "M5"):
        claves = sorted(set().union(*(lado[metrica] for lado in por_lado)))
        for k in claves:
            lineas.append("\t".join([metrica, k] + [str(lado[metrica].get(k, 0)) for lado in por_lado]))
    (salida / "por-entidad.tsv").write_text("\n".join(lineas) + "\n", encoding="utf-8")
    print("metricas.tsv: %d filas; por-entidad.tsv: %d filas; en %s" % (len(tsv) - 1, len(lineas) - 1, salida))


if __name__ == "__main__":
    main()
