#!/usr/bin/env python3
"""Verifica un CSV exportado contra un ORÁCULO calculado en SQL sobre la BD.

Nace en S148 (C-exportacion-csv, O-exportación). El CSV lo produce
`HorarioCsv.escribir` a partir del DTO de `proyectar`; este guion NO mira ese
código: recalcula las tres vistas del horario —grupo, profesor, aula— con SQL
puro y las compara, como CONJUNTOS, con las que se reconstruyen leyendo sólo el
fichero. Si las dos vías coinciden, el CSV dice lo que la base dice.

El SQL es el de S148/M2 (/tmp/s148/oraculo.sql, medido allí: 1285 entradas y 840
celdas de grupo, 835 de profesor, 819 de aula sobre el banco s137) más el filtro
por `horario_id`. El `ordenEnDia` 1..6 se recalcula aquí desde cero: el `orden`
de `tramo_semanal` es GLOBAL 1..35 y los recreos consumen número, así que no
sirve tal cual.

La reconstrucción del CSV es deliberadamente CIEGA a la base —la función recibe
una ruta de fichero y nada más—: si leyera la BD para interpretar el CSV, un
error compartido por las dos vías pasaría desapercibido.

La BD se abre en SOLO LECTURA (`mode=ro`): este guion no puede modificarla.

Uso:
  oraculo-exportacion.py csv <copia.db> <horario_id> <fichero.csv>
"""
import argparse
import csv
import sqlite3
import sys

BOM = b"\xef\xbb\xbf"

CABECERA = [
    "Día", "Tramo", "Asignatura", "Nombre asignatura", "Profesores", "Aula",
    "Grupos", "Subgrupos", "Actividad", "Plaza", "Índice", "Sesión",
]

SEPARADOR_CAMPO = ";"
SEPARADOR_LISTA = "/"

# Vistas del oráculo. Se crean como TEMP VIEW: una base abierta en modo ro admite
# objetos temporales porque viven en la BD temporal, no en el fichero. Si alguna
# vez no se pudieran crear, el fallo sale por pantalla y no se enmascara.
SQL_VISTAS = """
create temp view v_tramo as
select
    t.id as tramo_id,
    case t.dia when 'LUNES' then 1 when 'MARTES' then 2 when 'MIERCOLES' then 3
               when 'JUEVES' then 4 when 'VIERNES' then 5 end as dia,
    row_number() over (partition by t.dia order by t.orden) as tramo
from tramo_semanal t
where t.es_lectivo = 1;

create temp view v_sesion as
select s.id as sesion_id, s.plaza_id, s.aula_id, v.dia, v.tramo
from sesion s
join v_tramo v on v.tramo_id = s.tramo_inicio_id
where s.horario_id = :horario_id;

create temp view v_grupo as
select distinct g.codigo as grupo, s.dia, s.tramo, s.sesion_id
from v_sesion s
join plaza_subgrupo ps on ps.plaza_id = s.plaza_id
join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
join grupo_administrativo g on g.id = sg.grupo_id;

create temp view v_profesor as
select distinct p.codigo as profesor, s.dia, s.tramo, s.sesion_id
from v_sesion s
join plaza_profesor pp on pp.plaza_id = s.plaza_id
join profesor p on p.id = pp.profesor_id;

create temp view v_aula as
select distinct a.codigo as aula, s.dia, s.tramo, s.sesion_id
from v_sesion s
join aula a on a.id = s.aula_id;
"""


class Fallo(Exception):
    """Motivo por el que el CSV no se puede dar por bueno."""


def oraculo(ruta_db, horario_id):
    """Las tres vistas calculadas en SQL, como conjuntos de tuplas."""
    con = sqlite3.connect("file:%s?mode=ro" % ruta_db, uri=True)
    try:
        # `executescript` no admite parámetros con nombre; el id se interpola tras
        # forzarlo a int, que es la única forma en que puede llegar aquí.
        con.executescript(SQL_VISTAS.replace(":horario_id", str(int(horario_id))))

        def conjunto(vista):
            return {tuple(f) for f in con.execute(
                "select * from %s" % vista)}

        vistas = {
            "grupo": conjunto("v_grupo"),
            "profesor": conjunto("v_profesor"),
            "aula": conjunto("v_aula"),
        }
        sesiones = con.execute(
            "select count(*) from v_sesion").fetchone()[0]

        print("--- ORÁCULO (horario %s) ---" % horario_id)
        print("  sesiones del horario ......... %d" % sesiones)
        print("  grupo: entradas .............. %d" % len(vistas["grupo"]))
        print("  grupo: celdas (g,día,tramo) .. %d"
              % len({(g, d, t) for g, d, t, _ in vistas["grupo"]}))
        print("  profesor: celdas ............. %d"
              % len({(p, d, t) for p, d, t, _ in vistas["profesor"]}))
        print("  aula: celdas ................. %d"
              % len({(a, d, t) for a, d, t, _ in vistas["aula"]}))
        return vistas
    finally:
        con.close()


def desde_csv(ruta_csv):
    """Las tres vistas reconstruidas leyendo SÓLO el fichero.

    Comprueba de paso el BOM, la cabecera exacta y que ninguna Sesión se repita:
    son condiciones del formato, y sin ellas la comparación de conjuntos podría
    salir verde sobre un fichero que Excel no abriría.
    """
    with open(ruta_csv, "rb") as f:
        crudo = f.read()

    if not crudo.startswith(BOM):
        raise Fallo("el fichero no empieza por el BOM UTF-8 (EF BB BF): empieza por %s"
                    % crudo[:3].hex(" ").upper())

    texto = crudo[len(BOM):].decode("utf-8")
    filas = list(csv.reader(texto.splitlines(), delimiter=SEPARADOR_CAMPO,
                            quotechar='"'))
    if not filas:
        raise Fallo("el fichero no tiene ni cabecera")
    if filas[0] != CABECERA:
        raise Fallo("cabecera inesperada.\n  esperada: %s\n  leída:    %s"
                    % (SEPARADOR_CAMPO.join(CABECERA),
                       SEPARADOR_CAMPO.join(filas[0])))

    col = {nombre: i for i, nombre in enumerate(filas[0])}
    vistas = {"grupo": set(), "profesor": set(), "aula": set()}
    vistas_por_columna = [("grupo", "Grupos"), ("profesor", "Profesores")]
    sesiones_vistas = set()

    for n, fila in enumerate(filas[1:], start=2):
        if len(fila) != len(CABECERA):
            raise Fallo("la línea %d tiene %d campos y la cabecera %d"
                        % (n, len(fila), len(CABECERA)))
        dia = fila[col["Día"]]
        tramo = fila[col["Tramo"]]
        sesion = fila[col["Sesión"]]
        if sesion in sesiones_vistas:
            raise Fallo("la Sesión %s aparece en más de una fila (línea %d)"
                        % (sesion, n))
        sesiones_vistas.add(sesion)

        for vista, columna in vistas_por_columna:
            crudo_col = fila[col[columna]]
            elementos = crudo_col.split(SEPARADOR_LISTA) if crudo_col else []
            for e in elementos:
                vistas[vista].add((e, int(dia), int(tramo), int(sesion)))
        vistas["aula"].add((fila[col["Aula"]], int(dia), int(tramo), int(sesion)))

    print("--- CSV (%s) ---" % ruta_csv)
    print("  filas de datos ............... %d" % (len(filas) - 1))
    print("  grupo: entradas .............. %d" % len(vistas["grupo"]))
    print("  profesor: entradas ........... %d" % len(vistas["profesor"]))
    print("  aula: entradas ............... %d" % len(vistas["aula"]))
    return vistas


def comparar(nombre, del_oraculo, del_csv):
    """Imprime el cotejo de una vista y devuelve True si son el mismo conjunto."""
    faltan = del_oraculo - del_csv
    sobran = del_csv - del_oraculo
    print("--- vista %s ---" % nombre)
    print("  oráculo %d   csv %d   faltan %d   sobran %d"
          % (len(del_oraculo), len(del_csv), len(faltan), len(sobran)))
    for etiqueta, conjunto in (("falta", faltan), ("sobra", sobran)):
        for ejemplo in sorted(conjunto, key=str)[:5]:
            print("    %s: %s" % (etiqueta, ejemplo))
    if not del_oraculo:
        print("  FALLO: el oráculo de esta vista está vacío")
        return False
    if not del_csv:
        print("  FALLO: el CSV no aporta ninguna entrada a esta vista")
        return False
    return not faltan and not sobran


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("modo", choices=["csv"])
    parser.add_argument("db", help="copia de la BD (se abre en modo ro)")
    parser.add_argument("horario_id", type=int)
    parser.add_argument("csv", help="fichero exportado a verificar")
    args = parser.parse_args()

    vistas_oraculo = oraculo(args.db, args.horario_id)
    try:
        vistas_csv = desde_csv(args.csv)
    except Fallo as e:
        print("FALLO: %s" % e, file=sys.stderr)
        return 1

    iguales = [comparar(n, vistas_oraculo[n], vistas_csv[n])
               for n in ("grupo", "profesor", "aula")]
    if all(iguales):
        print("OK: las tres vistas coinciden")
        return 0
    print("FALLO: alguna vista no coincide (detalle arriba)", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
