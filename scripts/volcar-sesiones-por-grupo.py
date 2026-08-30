#!/usr/bin/env python3
"""Reconstruye el volcado que `calcular-recortes.py` espera, desde una BD SQLite.

Nace en S125 porque el volcado original de S122 vivía en /tmp y no sobrevivió: el
instrumento se conservó y su ENTRADA no, así que el número no era reproducible.
Esto lo cierra: la BD está versionada, y ahora también el paso que la convierte.

Formato de salida: {"grupos": {codigoGrupo: [sesion, ...]}}, con los campos que
`celdas_de()` lee (dia, tramo, actividadCodigo, indice) más los que documentan la
fila. Reproduce la vista por grupo del frontend:
  - `dia` 1..5 y `tramo` = ordenEnDia 1..6 CON LOS RECREOS EXCLUIDOS
    (`proyeccion.ts`: DIAS/TRAMOS), no el `orden` global 1..35 de `tramo_semanal`.
  - una sesión aparece en TODOS los grupos de los subgrupos de su plaza
    (D-F7-1, `filtrar()`: `s.grupos.includes(entidad)`), que es justo lo que hace
    que un bloque de seis destinos caiga en varios grupos a la vez.

La BD se abre en SOLO LECTURA (`mode=ro`): este guion no puede modificarla.
"""
import argparse
import json
import sqlite3
import sys

DIAS = {"LUNES": 1, "MARTES": 2, "MIERCOLES": 3, "JUEVES": 4, "VIERNES": 5}

SQL = """
select s.id, s.indice, t.dia, t.orden,
       asig.codigo, p.codigo, act.codigo, au.codigo
  from sesion s
  join plaza p       on p.id = s.plaza_id
  join actividad act on act.id = p.actividad_id
  join asignatura asig on asig.id = p.asignatura_id
  join tramo_semanal t on t.id = s.tramo_inicio_id
  join aula au       on au.id = s.aula_id
 where s.horario_id = ?
"""

SQL_GRUPOS = """
select ps.plaza_id, g.codigo
  from plaza_subgrupo ps
  join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
  join grupo_administrativo g on g.id = sg.grupo_id
"""


def orden_en_dia(conn):
    """orden global -> (dia, ordenEnDia 1..6) saltándose los tramos no lectivos."""
    mapa = {}
    contador = {}
    for orden, dia, lectivo in conn.execute(
            "select orden, dia, es_lectivo from tramo_semanal order by orden"):
        if not lectivo:
            continue
        d = DIAS[dia]
        contador[d] = contador.get(d, 0) + 1
        mapa[orden] = (d, contador[d])
    return mapa


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("bd", nargs="?", default="app/educhronos-demo-m4.db")
    ap.add_argument("-o", "--salida", default="/tmp/datos-maqueta.json")
    ap.add_argument("--horario", type=int, default=1)
    a = ap.parse_args(sys.argv[1:] if argv is None else argv)

    conn = sqlite3.connect("file:%s?mode=ro" % a.bd, uri=True)
    tramos = orden_en_dia(conn)

    por_plaza = {}
    for plaza_id, codigo in conn.execute(SQL_GRUPOS):
        por_plaza.setdefault(plaza_id, set()).add(codigo)

    plaza_id_de = dict(conn.execute("select codigo, id from plaza"))

    grupos = {}
    n = 0
    for sid, indice, dia, orden, asig, plaza_cod, act_cod, aula_cod in conn.execute(SQL, (a.horario,)):
        if orden not in tramos:            # sesión colocada en un recreo: no debería haberla
            raise SystemExit("sesion %d en tramo NO lectivo (orden %d)" % (sid, orden))
        d, t = tramos[orden]
        gs = sorted(por_plaza.get(plaza_id_de[plaza_cod], ()))
        fila = {"sesionId": sid, "indice": indice, "dia": d, "tramo": t,
                "asignaturaCodigo": asig, "aulaCodigo": aula_cod,
                "actividadCodigo": act_cod, "plazaCodigo": plaza_cod,
                "grupos": gs, "profesores": [], "subgrupos": []}
        n += 1
        for g in gs:
            grupos.setdefault(g, []).append(fila)

    json.dump({"grupos": grupos}, open(a.salida, "w", encoding="utf-8"))
    print("volcado: %s  (%d grupos, %d sesiones, %d filas proyectadas)"
          % (a.salida, len(grupos), n, sum(len(v) for v in grupos.values())))


if __name__ == "__main__":
    sys.exit(main())
