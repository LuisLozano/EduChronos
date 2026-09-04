#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Capa 2: conservacion de la carga del horario generado contra el PDF del centro.

QUE ASEVERA. Para cada grupo, los pares (asignatura, profesor) y su numero de
tramos semanales coinciden con lo que imprime el horario que el centro reparte.
Prueba la cadena entera: PDF -> volcados -> catalogo derivado -> carga por API ->
problema -> solucion -> filas de `sesion`.

QUE NO ASEVERA, y conviene tenerlo delante:
  - NO valida el solver. Dado un catalogo correcto, la conservacion se sigue de
    que `aSesiones` lanza si alguna instancia queda sin colocar. Lo que esta capa
    ejercita de verdad es la DERIVACION del catalogo.
  - NO dice nada de la COLOCACION. La comparacion es de multiconjuntos, sin dia
    ni tramo. Los dos horarios son distintos por diseno: los volcados no traen
    ninguna disponibilidad de profesor y el centro si las tuvo al construir el
    suyo, asi que comparar colocaciones solo podria producir diferencias, todas
    esperadas.
  - NO compara aulas. El PDF omite el aula en 65 celdas y el solver elige libre.

ORIGEN. Instrumento de la capa 2 de S119, que se construyo como arnes desechable
y hubo que reconstruir en S135 leyendo la bitacora. Se versiona para que la
tercera vez no haya que reconstruirlo: la especificacion esta en
docs/bitacora-sesiones.md (S119, "M2 (2) — CAPA 2, SIN JVM").

UNIDAD DE COMPARACION, fijada para disolver dos desajustes de unidades:
  entrada  = (grupo, dia, tramo, asignatura, profesor)
             Es lo que imprime una linea del PDF, y lo que se obtiene expandiendo
             una fila de `sesion` por `plaza_subgrupo -> subgrupo_grupo` y por
             `plaza_profesor` -que es como el PDF imprime la co-docencia-.
             Se toman entradas DISTINTAS: si una plaza agrupa dos subgrupos del
             mismo grupo, el PDF imprime una celda, no dos.
  agregado = (grupo, asignatura, profesor) -> numero de entradas semanales.
             Sin dia ni tramo.
El lado izquierdo sale de la BASE, no del catalogo derivado: comparar el catalogo
contra el PDF solo re-examinaria la derivacion contra si misma.

CUIDADO CON LAS UNIDADES: una fila de `sesion` es una PLAZA de una INSTANCIA, no
una instancia. filas = suma(repeticiones x plazas). Es la trampa sobre la que se
construye esta capa.
"""
import argparse
import glob
import json
import re
import sqlite3
import sys
from collections import Counter
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
VOLCADOS = RAIZ / "docs" / "horario-referencia"


# ------------------------------------------------------- mapa de codigos
#
# REGLA DETERMINISTA Y CIEGA A LA MEDICION. Sale de la tabla de normalizacion de
# docs/horario-referencia/INFORME-RECONCILIACION.md mas el caso `3o ESO PDC`, que
# S115 cerro por tres vias independientes y la nota posterior de ese mismo fichero
# recoge.
#
# POR QUE ESTA ESCRITO A MANO Y NO DERIVADO. En S119 el mapa se dedujo primero
# maximizando el solapamiento de (asignatura, profesor) entre los dos universos
# -que es EXACTAMENTE el dato que esta capa compara despues-. Circularidad de
# grado bajo pero real, y peor justo donde menos evidencia habia: 1FPB y 2FPB
# emparejaban con 0.14 y 0.17 y eran los dos grupos donde vivia toda la
# divergencia. NO SUSTITUIR ESTAS REGLAS POR UNA DERIVACION AUTOMATICA: el mapa
# debe ser ajeno a lo que se mide. `verificar_mapa` mira la base, pero solo para
# comprobar la regla ya escrita, nunca para construirla.
REGLAS_CODIGO = [
    ("Nº ESO L PDC -> NºLDi", re.compile(r"^(\d)º ESO ([A-D]) PDC$"), r"\1º\2Di"),
    ("Nº ESO PDC   -> 3ºCDi", re.compile(r"^3º ESO PDC$"),            "3ºCDi"),
    ("Nº ESO L     -> NºL",   re.compile(r"^(\d)º ESO ([A-D])$"),     r"\1º\2"),
    ("NºBACH L     -> NB-L",  re.compile(r"^(\d)ºBACH ([A-D])$"),     r"\1B-\2"),
    ("Nº FPB       -> NFPB",  re.compile(r"^(\d)º FPB$"),             r"\1FPB"),
]


def normaliza(crudo):
    """Forma larga del PDF -> forma corta de la base. (None, None) si no aplica."""
    for nombre, patron, plantilla in REGLAS_CODIGO:
        if patron.match(crudo):
            return patron.sub(plantilla, crudo), nombre
    return None, None


# ------------------------------------------------------------- supuestos

def comprobar_supuestos(con, horario_id):
    """Mide en vez de asumir. Devuelve la lista de supuestos rotos."""
    def uno(sql, *args):
        return con.execute(sql, args).fetchone()[0]

    comprobaciones = [
        ("duracion_tramos distinta de 1", uno("SELECT COUNT(*) FROM actividad WHERE duracion_tramos <> 1"), 0),
        ("plazas sin asignatura", uno("SELECT COUNT(*) FROM plaza WHERE asignatura_id IS NULL"), 0),
        ("plazas sin profesor", uno("SELECT COUNT(*) FROM plaza p WHERE NOT EXISTS"
                                    " (SELECT 1 FROM plaza_profesor x WHERE x.plaza_id = p.id)"), 0),
        ("plazas sin subgrupo", uno("SELECT COUNT(*) FROM plaza p WHERE NOT EXISTS"
                                    " (SELECT 1 FROM plaza_subgrupo x WHERE x.plaza_id = p.id)"), 0),
        ("subgrupos sin grupo", uno("SELECT COUNT(*) FROM subgrupo s WHERE NOT EXISTS"
                                    " (SELECT 1 FROM subgrupo_grupo x WHERE x.subgrupo_id = s.id)"), 0),
        ("sesiones en tramo no lectivo",
         uno("SELECT COUNT(*) FROM sesion s JOIN tramo_semanal t ON t.id = s.tramo_inicio_id"
             " WHERE NOT t.es_lectivo AND s.horario_id = ?", horario_id), 0),
        ("FK nulas en sesion",
         uno("SELECT COUNT(*) FROM sesion WHERE plaza_id IS NULL OR aula_id IS NULL"
             " OR tramo_inicio_id IS NULL"), 0),
    ]
    rotos = []
    print("SUPUESTOS (medidos, no asumidos)")
    for nombre, valor, esperado in comprobaciones:
        marca = "OK" if valor == esperado else "ROTO"
        if valor != esperado:
            rotos.append((nombre, valor, esperado))
        print("  %-32s %6d   %s" % (nombre, valor, marca))
    lect = uno("SELECT COUNT(*) FROM tramo_semanal WHERE es_lectivo")
    nolect = uno("SELECT COUNT(*) FROM tramo_semanal WHERE NOT es_lectivo")
    print("  %-32s %6s" % ("tramos lectivos / no lectivos", "%d / %d" % (lect, nolect)))
    return rotos


# ----------------------------------------------------------- los dos lados

def entradas_del_pdf():
    """Conjunto de (grupo_corto, dia, tramo, asignatura, profesor) desde los volcados."""
    ficheros = sorted(glob.glob(str(VOLCADOS / "grupo-*.json")))
    if not ficheros:
        raise SystemExit("No hay grupo-*.json en %s" % VOLCADOS)
    entradas, mapa, sin_regla = set(), {}, []
    for f in ficheros:
        d = json.loads(Path(f).read_text(encoding="utf-8"))
        crudo = d["_meta"]["codigo_crudo"]
        corto, _ = normaliza(crudo)
        if corto is None:
            sin_regla.append(crudo)
            continue
        mapa[crudo] = corto
        for c in d["celdas"]:
            entradas.add((corto, c["dia"], c["tramo"], c["asignatura"], c["profesor"]))
    return entradas, mapa, sin_regla, len(ficheros)


def entradas_de_la_base(con, horario_id):
    """Conjunto de (grupo, dia, tramo, asignatura, profesor) desde `sesion`."""
    filas = con.execute("""
        SELECT DISTINCT g.codigo, t.dia, t.orden, a.codigo, pr.codigo
        FROM sesion s
        JOIN plaza            p   ON p.id  = s.plaza_id
        JOIN asignatura       a   ON a.id  = p.asignatura_id
        JOIN plaza_profesor   pp  ON pp.plaza_id = p.id
        JOIN profesor         pr  ON pr.id = pp.profesor_id
        JOIN plaza_subgrupo   ps  ON ps.plaza_id = p.id
        JOIN subgrupo_grupo   sg  ON sg.subgrupo_id = ps.subgrupo_id
        JOIN grupo_administrativo g ON g.id = sg.grupo_id
        JOIN tramo_semanal    t   ON t.id  = s.tramo_inicio_id
        WHERE s.horario_id = ?
    """, (horario_id,)).fetchall()
    return set(filas)


def agrega(entradas):
    """(grupo, dia, tramo, asig, prof) -> Counter[(grupo, asig, prof)]."""
    c = Counter()
    for grupo, _dia, _tramo, asig, prof in entradas:
        c[(grupo, asig, prof)] += 1
    return c


def verificar_mapa(con, mapa, sin_regla):
    """La base SOLO aparece aqui, y solo para comprobar la regla ya escrita."""
    print("\nMAPA DE CODIGOS (regla del INFORME-RECONCILIACION, ajena a la medicion)")
    print("  codigos con regla aplicable: %d" % len(mapa))
    if sin_regla:
        print("  SIN REGLA: %s" % sin_regla)
    img = list(mapa.values())
    inyectiva = len(set(img)) == len(img)
    base = {r[0] for r in con.execute("SELECT codigo FROM grupo_administrativo")}
    coincide = set(img) == base
    print("  inyectiva: %s" % inyectiva)
    print("  imagen == codigos de la base (%d): %s" % (len(base), coincide))
    if not coincide:
        print("    en el mapa y no en la base: %s" % sorted(set(img) - base))
        print("    en la base y no en el mapa: %s" % sorted(base - set(img)))
    return inyectiva and coincide and not sin_regla


# ------------------------------------------------------------------- main

def main():
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("--db", required=True, help="base sqlite con el horario generado")
    p.add_argument("--horario", type=int, default=None, help="id del horario (por defecto, el unico)")
    args = p.parse_args()

    ruta = Path(args.db)
    if not ruta.is_file():
        raise SystemExit("No existe la base: %s" % ruta)
    con = sqlite3.connect("file:%s?mode=ro" % ruta, uri=True)

    horarios = [r[0] for r in con.execute("SELECT id FROM horario_generado ORDER BY id")]
    if not horarios:
        raise SystemExit("La base no tiene ningun horario generado.")
    if args.horario is None:
        if len(horarios) > 1:
            raise SystemExit("Hay %d horarios (%s). Elige uno con --horario."
                             % (len(horarios), horarios))
        horario_id = horarios[0]
    else:
        if args.horario not in horarios:
            raise SystemExit("No existe el horario %d. Hay: %s" % (args.horario, horarios))
        horario_id = args.horario

    print("CAPA 2 — CONSERVACION DE LA CARGA")
    print("  base:    %s" % ruta)
    print("  horario: %d de %s" % (horario_id, horarios))
    print("  PDF:     %s/grupo-*.json" % VOLCADOS.relative_to(RAIZ))
    print()

    rotos = comprobar_supuestos(con, horario_id)

    pdf, mapa, sin_regla, n_ficheros = entradas_del_pdf()
    mapa_ok = verificar_mapa(con, mapa, sin_regla)
    base = entradas_de_la_base(con, horario_id)

    a_pdf, a_base = agrega(pdf), agrega(base)
    claves = sorted(set(a_pdf) | set(a_base))
    divergentes = [k for k in claves if a_pdf.get(k, 0) != a_base.get(k, 0)]

    print("\nRECUENTOS")
    print("  ficheros de grupo leidos ....... %d" % n_ficheros)
    print("  entradas del PDF ............... %d" % len(pdf))
    print("  entradas de la base ............ %d" % len(base))
    print("  claves (grupo, asignatura, prof) %d" % len(claves))
    print("  divergentes .................... %d" % len(divergentes))
    print("  delta total (PDF - base) ....... %d - %d = %d"
          % (sum(a_pdf.values()), sum(a_base.values()),
             sum(a_pdf.values()) - sum(a_base.values())))

    if divergentes:
        print("\nDIVERGENCIAS")
        print("  %-10s %-10s %-8s %6s %6s %6s" % ("grupo", "asignatura", "profesor",
                                                  "PDF", "base", "delta"))
        for k in divergentes:
            g, a, pr = k
            vp, vb = a_pdf.get(k, 0), a_base.get(k, 0)
            print("  %-10s %-10s %-8s %6d %6d %6d" % (g, a, pr, vp, vb, vp - vb))
        por_grupo = Counter(k[0] for k in divergentes)
        print("  por grupo: %s" % dict(sorted(por_grupo.items())))

    print("\nVEREDICTO")
    if rotos:
        print("  SUPUESTOS ROTOS: %s" % [r[0] for r in rotos])
    if not mapa_ok:
        print("  MAPA NO VERIFICADO: la comparacion no es de fiar.")
    if divergentes:
        print("  %d claves divergentes. La carga NO se conserva por completo." % len(divergentes))
    else:
        print("  CERO divergentes sobre %d claves: la carga se conserva." % len(claves))
    return 0 if (not divergentes and not rotos and mapa_ok) else 1


if __name__ == "__main__":
    sys.exit(main())
