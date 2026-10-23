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

Desde S149 verifica TAMBIÉN el PDF de vista de grupo (C-exportacion-pdf-grupo). Mismo
principio y mismas vistas SQL: lo que cambia es de dónde se leen las entradas. Del PDF se
leen con `pdftotext`, NUNCA del flujo de contenido: con IDENTITY_H el texto va como
identificadores de glifo y ahí no hay nada legible; `pdftotext` los traduce con el
ToUnicode de la fuente empotrada, que es justo lo que ve quien abre el fichero.

Desde S150 el modo pdf verifica ADEMÁS la vista de profesor, con `--vista profesor`. Las
vistas SQL no cambian —`v_grupo` y `v_profesor` ya existían desde S148—: lo que cambia es
de cuál se cuelgan las entradas y con qué orden se compone su texto, que es el del centro
(«asignatura, aula, grupo» en la página de un profesor). El guion sigue SIN IMPORTAR NADA
del exportador: si leyera `VistaPdf` para saber qué texto esperar, un error compartido por
las dos vías pasaría desapercibido, que es justo lo que este oráculo existe para impedir.

Desde S170 cada sesión se expande a los tramos que cubre según `actividad.duracion_tramos`,
una guarda aborta antes de comparar si algún bloque del horario desborda el día o cruza el
recreo, y el CSV admite varias filas por sesión (una por tramo) siempre que no repitan un
mismo (Sesión, Día, Tramo) ni difieran en otra columna que «Tramo».

Desde S215 (C-exportacion-guardias):
  - Una sesión SIN AULA (reuniones y funciones, S201) cuenta en las vistas de grupo y de
    profesor, con el aula vacía, y NO cuenta en la de aula: ni en lo esperado ni en lo leído
    del CSV. Su profesor tiene página en el PDF.
  - Las guardias ordinarias se esperan desde la tabla `guardia` del horario, con SQL propio
    (`v_guardia`). En el CSV, una fila de guardia es la que trae «Sesión» vacía y «Nombre
    asignatura» «Guardia»; va tras todas las de sesión y sus faltan y sobran se cuentan
    aparte. En el PDF de profesor, cada guardia es una entrada «Guardia» en su celda,
    después de las sesiones, y un profesor con solo guardias tiene página.
  - `--vista guardias` verifica la página de guardias ordinarias (escrito sobre la página real,
    S215 T2 E7): UNA página titulada «Guardias ordinarias» con la clave «Profesores de
    guardia», y en cada celda el conjunto EXACTO de códigos que la tabla `guardia` pone en
    ese día y tramo, leídos partiendo el texto de la celda por la coma.

Desde S221 (C-exportacion-guardias-recreo, D7 del contrato v2):
  - La consulta de guardias lee `guardia.tipo` (una base anterior al esquema 8 no lo tiene:
    todas son ORDINARIA) y el recreo de cada guardia de RECREO o BIBLIOTECA, con `v_recreo`:
    el número de orden, desde 1, del tramo entre los NO lectivos de su día, por `orden`. Una
    ORDINARIA en un no lectivo o una de recreo en un lectivo es corrupción y aborta.
  - Las guardias se comparan como MULTICONJUNTOS de (profesor, día, lugar, tipo), donde el
    lugar es («tramo», n) o («recreo», n).
  - CSV: una fila de guardia trae «Sesión» vacía y «Nombre asignatura» «Guardia» o
    «Biblioteca»; su «Tramo» es el número lectivo, «Recreo» (recreo 1) o «Recreo n» (n > 1).
    «Guardia» en un recreo es RECREO; «Biblioteca», BIBLIOTECA.
  - PDF: una fila de la rejilla es de recreo si lleva «Recreo» en la columna de horas (banda
    de cinco celdas: profesor y guardias) o en la zona de días (banda única: grupo y aula).
    Las bandas se numeran 1, 2… de arriba abajo y los tramos lectivos saltan todas. En la
    página de profesor, cada guardia de recreo es una entrada «Guardia» (RECREO) o
    «Biblioteca» en la celda de su día en la banda; un profesor con solo guardias de recreo
    tiene página. En la de guardias, la celda de la banda lleva los códigos de RECREO unidos
    por coma y, aparte, «Biblioteca: COD». La página de guardias se reconoce por un título
    que empieza por «Guardias»; si no es exactamente «Guardias», es un fallo de forma.
  - La lectura de las bandas de cinco celdas se escribió sin un PDF que las tenga: la valida
    el T2 sobre la salida real del producto y con mutantes.

Desde S250 (C-asignatura-con-nivel), la asignatura de cada entrada esperada en el PDF es su
código CORTO, `asignatura.codigo_corto`, si la base tiene esa columna (esquema 9), y
`asignatura.codigo` si no: una asignatura con nivel tiene código `Biol-3ESO`, y la rejilla y la
exportación enseñan `Biol`. La columna se mira en la base con `pragma table_info`, no en el
exportador.

Uso:
  oraculo-exportacion.py csv <copia.db> <horario_id> <fichero.csv>
  oraculo-exportacion.py pdf <copia.db> <horario_id> <fichero.pdf> [--vista grupo|profesor|aula|guardias]
"""
import argparse
import collections
import csv
import re
import sqlite3
import subprocess
import sys
import xml.etree.ElementTree as ET

BOM = b"\xef\xbb\xbf"

CABECERA = [
    "Día", "Tramo", "Asignatura", "Nombre asignatura", "Profesores", "Aula",
    "Grupos", "Subgrupos", "Actividad", "Plaza", "Índice", "Sesión",
]

SEPARADOR_CAMPO = ";"
SEPARADOR_LISTA = "/"

# El texto con el que una guardia aparece en el CSV («Nombre asignatura») y como entrada de
# celda en el PDF de profesor (S215, puntos B2 y C1 del contrato).
ENTRADA_GUARDIA = "Guardia"

# Tipos de guardia (`guardia.tipo`, esquema 8) y el rótulo con que el CSV y la página de
# profesor nombran cada uno (S221, X1 y X2). RECREO lleva el mismo rótulo que ORDINARIA: lo
# que las distingue es el lugar, un tramo o un recreo.
ORDINARIA, RECREO, BIBLIOTECA = "ORDINARIA", "RECREO", "BIBLIOTECA"
ENTRADA_BIBLIOTECA = "Biblioteca"
ROTULO_DE_TIPO = {ORDINARIA: ENTRADA_GUARDIA, RECREO: ENTRADA_GUARDIA,
                  BIBLIOTECA: ENTRADA_BIBLIOTECA}

# El «Tramo» de una fila de recreo en el CSV (X5) y el rótulo de la banda en el PDF.
ROTULO_RECREO = "Recreo"
TRAMO_RECREO = re.compile(r"Recreo(?: ([2-9]|[1-9]\d+))?")

# Columnas que una fila de guardia del CSV lleva VACÍAS (B2): todas salvo Día, Tramo,
# Nombre asignatura y Profesores.
VACIAS_EN_GUARDIA = ("Asignatura", "Aula", "Grupos", "Subgrupos", "Actividad", "Plaza",
                     "Índice", "Sesión")

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
select s.id as sesion_id, s.plaza_id, s.aula_id, c.dia, c.tramo
from sesion s
join plaza p     on p.id = s.plaza_id
join actividad a on a.id = p.actividad_id
join v_tramo i   on i.tramo_id = s.tramo_inicio_id
join v_tramo c   on c.dia = i.dia
                and c.tramo between i.tramo and i.tramo + a.duracion_tramos - 1
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


# Sesiones cuyo bloque no cabe: los tramos lectivos que cubre no son `duracion_tramos`
# (desborda el día), o su extensión en el `orden` de tramo_semanal no lo es (cruza el
# recreo: ese orden cuenta el recreo y la numeración lectiva no). Con `v_sesion` expandida,
# una sesión así se compararía con MENOS tramos de los que ocupa y podría salir OK.
SQL_BLOQUES_IMPOSIBLES = """
select s.id, a.codigo, a.duracion_tramos,
       count(c.tramo_id) as cubiertos,
       max(t.orden) - min(t.orden) + 1 as extension
from sesion s
join plaza p              on p.id = s.plaza_id
join actividad a          on a.id = p.actividad_id
left join v_tramo i       on i.tramo_id = s.tramo_inicio_id
left join v_tramo c       on c.dia = i.dia
                         and c.tramo between i.tramo and i.tramo + a.duracion_tramos - 1
left join tramo_semanal t on t.id = c.tramo_id
where s.horario_id = ?
group by s.id, a.codigo, a.duracion_tramos
having count(c.tramo_id) <> a.duracion_tramos
    or coalesce(max(t.orden) - min(t.orden) + 1, 0) <> a.duracion_tramos
order by s.id
"""


def guarda_bloques(ruta_db, horario_id):
    """Aborta con Fallo si alguna sesión del horario tiene un bloque imposible. Corre antes
    de cualquier comparación, en los dos modos."""
    con = sqlite3.connect("file:%s?mode=ro" % ruta_db, uri=True)
    try:
        con.executescript(SQL_VISTAS.replace(":horario_id", str(int(horario_id))))
        filas = con.execute(SQL_BLOQUES_IMPOSIBLES, (int(horario_id),)).fetchall()
    finally:
        con.close()
    if filas:
        raise Fallo("%d sesión(es) del horario %s con un bloque imposible:\n%s" % (
            len(filas), horario_id, "\n".join(
                "  sesión %d, actividad %s, duración %d: cubre %d tramo(s) lectivo(s), "
                "extensión %s en el orden de tramo_semanal"
                % (sid, act, dur, cub, "-" if ext is None else ext)
                for sid, act, dur, cub, ext in filas)))
    print("--- GUARDA DE BLOQUES (horario %s): ninguna sesión con bloque imposible ---"
          % horario_id)


def nombre_dia(dia):
    """«lunes»..«viernes» para un Día 1..5 leído del CSV."""
    if dia in ("1", "2", "3", "4", "5"):
        return DIAS_CABECERA[int(dia) - 1].lower()
    return "fuera de 1..5"


# Los recreos de cada día (S221, D2 del contrato v2): el número de orden, desde 1, de cada
# tramo NO lectivo entre los no lectivos de su día, por `orden`. Es `v_tramo` con el filtro
# al revés, escrito aquí y no importado del producto.
SQL_VISTA_RECREO = """
create temp view v_recreo as
select
    t.id as tramo_id,
    case t.dia when 'LUNES' then 1 when 'MARTES' then 2 when 'MIERCOLES' then 3
               when 'JUEVES' then 4 when 'VIERNES' then 5 end as dia,
    row_number() over (partition by t.dia order by t.orden) as recreo
from tramo_semanal t
where t.es_lectivo = 0;
"""

# Guardias del horario (S215), con el día y el tramo LECTIVO de `v_tramo` o el recreo de
# `v_recreo` (S221). Los dos `left join` son a propósito: una guardia cuyo tipo no casa con
# su tramo sale con el lugar nulo y aborta en `guardias_esperadas`, en vez de desaparecer de
# lo esperado sin decir nada. `{tipo}` es `g.tipo`, o la constante ORDINARIA en una base
# anterior al esquema 8, que no tiene la columna.
SQL_GUARDIAS = """
select p.codigo, coalesce(t.dia, r.dia), t.tramo, r.recreo, {tipo}, g.tramo_id
from guardia g
join profesor p    on p.id = g.profesor_id
left join v_tramo t  on t.tramo_id = g.tramo_id
left join v_recreo r on r.tramo_id = g.tramo_id
where g.horario_id = ?
"""


def guardias_esperadas(ruta_db, horario_id):
    """Las guardias que la BD exige, como multiconjunto de (profesor, día, lugar, tipo), con
    el lugar («tramo», n) para una ORDINARIA y («recreo», n) para una de RECREO o BIBLIOTECA.

    Una base anterior al esquema 6 no tiene la tabla `guardia`, y su horario no tiene
    guardias: se espera la lista vacía. Una ORDINARIA en un tramo no lectivo, o una de recreo
    en un tramo lectivo, es un invariante roto que la exportación no puede pintar (enmienda
    EA2): aborta con Fallo. Un tipo desconocido, también."""
    con = sqlite3.connect("file:%s?mode=ro" % ruta_db, uri=True)
    try:
        if con.execute("select count(*) from sqlite_master where type = 'table' "
                       "and name = 'guardia'").fetchone()[0] == 0:
            return collections.Counter()
        con.executescript(SQL_VISTAS.replace(":horario_id", str(int(horario_id))))
        con.executescript(SQL_VISTA_RECREO)
        con_tipo = any(c[1] == "tipo" for c in con.execute("pragma table_info(guardia)"))
        filas = con.execute(SQL_GUARDIAS.format(tipo="g.tipo" if con_tipo else "'%s'" % ORDINARIA),
                            (int(horario_id),)).fetchall()
    finally:
        con.close()
    desconocidas = sorted({tipo for _, _, _, _, tipo, _ in filas if tipo not in ROTULO_DE_TIPO})
    if desconocidas:
        raise Fallo("guardias del horario %s con un tipo desconocido: %s"
                    % (horario_id, ", ".join(map(repr, desconocidas))))
    no_lectivas = [(p, tid) for p, d, t, r, tipo, tid in filas if tipo == ORDINARIA and t is None]
    if no_lectivas:
        raise Fallo("%d guardia(s) del horario %s en un tramo no lectivo: %s" % (
            len(no_lectivas), horario_id,
            ", ".join("%s en el tramo %d" % (p, tid) for p, tid in no_lectivas)))
    lectivas = [(p, tipo, tid) for p, d, t, r, tipo, tid in filas if tipo != ORDINARIA and r is None]
    if lectivas:
        raise Fallo("%d guardia(s) de recreo del horario %s en un tramo lectivo: %s" % (
            len(lectivas), horario_id,
            ", ".join("%s (%s) en el tramo %d" % (p, tipo, tid) for p, tipo, tid in lectivas)))
    return collections.Counter(
        (p, d, ("tramo", t) if tipo == ORDINARIA else ("recreo", r), tipo)
        for p, d, t, r, tipo, _ in filas)


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
            "select count(distinct sesion_id) from v_sesion").fetchone()[0]

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
    """Las tres vistas y las guardias reconstruidas leyendo SÓLO el fichero.

    Comprueba de paso el BOM, la cabecera exacta y que ninguna Sesión se repita:
    son condiciones del formato, y sin ellas la comparación de conjuntos podría
    salir verde sobre un fichero que Excel no abriría.

    Desde S215 devuelve `(vistas, guardias)`. Una fila de guardia —«Sesión» vacía y «Nombre
    asignatura» «Guardia»— se aparta ANTES de tocar la Sesión, que no es un número, y va al
    multiconjunto de guardias (profesor, día, tramo): una guardia duplicada es una que sobra.
    Su forma es la de B2: el resto de columnas, vacías, y un solo código en «Profesores».
    Una fila de sesión tras una de guardia aborta (B1: las guardias van al final), igual que
    una «Sesión» vacía en una fila que no es de guardia. Una fila de sesión con el «Aula»
    vacía no aporta entrada a la vista de aula (EE6).

    Desde S221 la clave de una guardia es (profesor, día, lugar, tipo). «Nombre asignatura»
    «Guardia» o «Biblioteca» marca la fila como de guardia; el lugar sale del «Tramo»: un
    número es un tramo lectivo y «Recreo» / «Recreo n», un recreo. El tipo sale de los dos:
    «Guardia» en un tramo es ORDINARIA, en un recreo RECREO; «Biblioteca» es BIBLIOTECA. Una
    «Biblioteca» en un tramo lectivo no aborta: entra con su clave y saldrá como sobrante.
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
    # Una sesión de un bloque ocupa VARIAS filas, una por tramo que cubre. Lo que no puede
    # pasar es que un mismo (Sesión, Día, Tramo) salga dos veces, ni que dos filas de la
    # misma Sesión digan cosas distintas fuera de «Tramo»: serían dos sesiones con un id.
    celdas_vistas = set()
    primera_fila = {}
    guardias = collections.Counter()
    primera_guardia = None

    for n, fila in enumerate(filas[1:], start=2):
        if len(fila) != len(CABECERA):
            raise Fallo("la línea %d tiene %d campos y la cabecera %d"
                        % (n, len(fila), len(CABECERA)))
        dia = fila[col["Día"]]
        tramo = fila[col["Tramo"]]
        sesion = fila[col["Sesión"]]

        rotulo = fila[col["Nombre asignatura"]]
        if sesion == "" and rotulo in (ENTRADA_GUARDIA, ENTRADA_BIBLIOTECA):
            llenas = [c for c in VACIAS_EN_GUARDIA if fila[col[c]] != ""]
            profesor = fila[col["Profesores"]]
            if llenas:
                raise Fallo("la fila de guardia de la línea %d trae valor en %s"
                            % (n, ", ".join("«%s»" % c for c in llenas)))
            if not profesor or SEPARADOR_LISTA in profesor:
                raise Fallo("la fila de guardia de la línea %d no trae UN código de profesor: %r"
                            % (n, profesor))
            recreo = TRAMO_RECREO.fullmatch(tramo)
            if not dia.isdigit() or not (tramo.isdigit() or recreo):
                raise Fallo("la fila de guardia de la línea %d no trae un día numérico y un tramo "
                            "numérico o «Recreo»/«Recreo n»: %r, %r" % (n, dia, tramo))
            if recreo:
                lugar = ("recreo", int(recreo.group(1) or 1))
                tipo = RECREO if rotulo == ENTRADA_GUARDIA else BIBLIOTECA
            else:
                lugar = ("tramo", int(tramo))
                tipo = ORDINARIA if rotulo == ENTRADA_GUARDIA else BIBLIOTECA
            guardias[(profesor, int(dia), lugar, tipo)] += 1
            if primera_guardia is None:
                primera_guardia = n
            continue
        if sesion == "":
            raise Fallo("la línea %d trae la «Sesión» vacía y no es una fila de guardia "
                        "(«Nombre asignatura» %r)" % (n, fila[col["Nombre asignatura"]]))
        if primera_guardia is not None:
            raise Fallo("la línea %d es de sesión y va tras la fila de guardia de la línea %d: "
                        "las guardias van tras TODAS las sesiones" % (n, primera_guardia))

        if (sesion, dia, tramo) in celdas_vistas:
            raise Fallo("la Sesión %s aparece repetida en el día %s (%s), tramo %s (línea %d)"
                        % (sesion, dia, nombre_dia(dia), tramo, n))
        celdas_vistas.add((sesion, dia, tramo))
        if sesion in primera_fila:
            linea_primera, anterior = primera_fila[sesion]
            for columna, i in col.items():
                if columna != "Tramo" and fila[i] != anterior[i]:
                    raise Fallo("la Sesión %s tiene filas que difieren en la columna «%s»: "
                                "%r en la línea %d y %r en la línea %d"
                                % (sesion, columna, anterior[i], linea_primera, fila[i], n))
        else:
            primera_fila[sesion] = (n, fila)

        for vista, columna in vistas_por_columna:
            crudo_col = fila[col[columna]]
            elementos = crudo_col.split(SEPARADOR_LISTA) if crudo_col else []
            for e in elementos:
                vistas[vista].add((e, int(dia), int(tramo), int(sesion)))
        # Sin aula (reunión o función, S201), la sesión no ocupa ninguna: no entra en la
        # vista de aula, como tampoco entra en `v_aula`.
        if fila[col["Aula"]]:
            vistas["aula"].add((fila[col["Aula"]], int(dia), int(tramo), int(sesion)))

    print("--- CSV (%s) ---" % ruta_csv)
    print("  filas de datos ............... %d" % (len(filas) - 1))
    print("  grupo: entradas .............. %d" % len(vistas["grupo"]))
    print("  profesor: entradas ........... %d" % len(vistas["profesor"]))
    print("  aula: entradas ............... %d" % len(vistas["aula"]))
    print("  guardias: filas .............. %d" % sum(guardias.values()))
    return vistas, guardias


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


def por_tipo(multiconjunto):
    """«ORDINARIA a, RECREO b, BIBLIOTECA c» de un multiconjunto de guardias (S221)."""
    cuenta = collections.Counter()
    for (_, _, _, tipo), n in multiconjunto.items():
        cuenta[tipo] += n
    return ", ".join("%s %d" % (t, cuenta[t]) for t in (ORDINARIA, RECREO, BIBLIOTECA))


def comparar_guardias(del_oraculo, del_csv):
    """Coteja las guardias como MULTICONJUNTOS de (profesor, día, lugar, tipo), aparte de las
    vistas de sesiones (S215; el lugar y el tipo, S221). A diferencia de `comparar`, una
    lista vacía en los dos lados es lo correcto: un horario sin guardias no exporta ninguna
    fila de guardia."""
    faltan = del_oraculo - del_csv
    sobran = del_csv - del_oraculo
    print("--- guardias ---")
    print("  oráculo %d   csv %d   faltan %d   sobran %d"
          % (sum(del_oraculo.values()), sum(del_csv.values()),
             sum(faltan.values()), sum(sobran.values())))
    print("  oráculo por tipo: %s" % por_tipo(del_oraculo))
    print("  faltan por tipo:  %s" % por_tipo(faltan))
    print("  sobran por tipo:  %s" % por_tipo(sobran))
    for etiqueta, multiconjunto in (("falta", faltan), ("sobra", sobran)):
        for ejemplo in sorted(multiconjunto)[:5]:
            print("    %s x%d: %s" % (etiqueta, multiconjunto[ejemplo], ejemplo))
    return not faltan and not sobran


# --------------------------------------------------------------------------- PDF (S149)

# Lo que cada vista cambia de la página. Se escribe aquí, a mano y a propósito: es la
# expectativa CONTRA la que se coteja, y derivarla del exportador la volvería tautológica.
VISTAS = {
    "grupo": {
        "sql": "v_grupo",
        "clave_de_lectura": "Asignatura - Profesor - Aula",
        # De la fila de `v_*` unida a la sesión: qué campos y en qué orden se concatenan.
        "orden": ("asignatura", "profesores", "aula"),
        "recurso": "grupo",
        "recursos": "grupos",
        "catalogo": None,
    },
    "profesor": {
        "sql": "v_profesor",
        "clave_de_lectura": "Asignatura - Aula - Grupo",
        "orden": ("asignatura", "aula", "grupos"),
        "recurso": "profesor",
        "recursos": "profesores",
        "catalogo": None,
    },
    "aula": {
        "sql": "v_aula",
        "clave_de_lectura": "Asignatura - Profesor - Grupo",
        "orden": ("asignatura", "profesores", "grupos"),
        "recurso": "aula",
        "recursos": "aulas",
        # ÚNICA vista con catálogo: sus páginas NO son las que tienen clases, son TODAS
        # las del catálogo. Un aula sin una sola sesión también tiene página, y vacía es
        # justo lo que hay que mirar para saber qué está libre.
        "catalogo": "select codigo from aula order by codigo",
    },
}

# Rótulos fijos de la página que NO son entradas de rejilla. Ver `entradas_de_pagina`.
ENCABEZADO_LEYENDA = "Profesores"
DIAS_CABECERA = ("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
RANGO_HORARIO = re.compile(r"\d{2}:\d{2}-\d{2}:\d{2}")

# Geometría de la rejilla, las dos únicas medidas de la maqueta que el verificador
# necesita: dónde acaba la columna de horas y cuánto mide cada columna de día.
X_PRIMERA_COLUMNA = 86.0
ANCHO_COLUMNA = 96.2


def columna_codigo_asignatura(con):
    """La columna con el código que enseña la exportación: `codigo_corto` si existe (esquema 9,
    S250) y `codigo` si no. Se pregunta a la base, no al exportador."""
    columnas = {fila[1] for fila in con.execute("pragma table_info(asignatura)")}
    return "codigo_corto" if "codigo_corto" in columnas else "codigo"


def entradas_esperadas(ruta_db, horario_id, vista="grupo"):
    """Entradas de rejilla que la BD exige, por código de RECURSO de la vista.

    Reutiliza SQL_VISTAS: `v_grupo` y `v_profesor` ya dicen qué recurso ve qué sesión, y de
    ahí se cuelgan asignatura, profesores, aula y grupos. El TEXTO se compone con el orden
    que esa vista imprime —«asignatura, profesor, aula» en la de grupo; «asignatura, aula,
    grupo» en la de profesor—, que es el de los horarios del centro. Devuelve un Counter
    por recurso: la MULTIPLICIDAD importa, porque la misma clase puede repetirse en la
    semana.

    Desde S215, el aula va por `left join`: una sesión sin aula cuenta en grupo y profesor
    con el aula vacía (EE6), y el texto se compone con los campos NO vacíos separados por un
    espacio —una entrada impresa no lleva dos espacios seguidos ni uno al final—. En la de
    aula no aparece, porque `v_aula` solo trae sesiones con aula. En la vista de profesor
    cada guardia de `v_guardia` es una entrada «Guardia» en su celda, y un profesor con solo
    guardias tiene así su página.

    Desde S221 aquí solo entran las ORDINARIA; las de recreo las espera `bandas_esperadas`.
    """
    conf = VISTAS[vista]
    guardias = (guardias_esperadas(ruta_db, horario_id) if vista == "profesor"
                else collections.Counter())
    con = sqlite3.connect("file:%s?mode=ro" % ruta_db, uri=True)
    try:
        con.executescript(SQL_VISTAS.replace(":horario_id", str(int(horario_id))))
        por_recurso = collections.defaultdict(lambda: collections.defaultdict(collections.Counter))
        # Las vistas con catálogo SIEMBRAN todos sus recursos antes de contar entradas: los
        # que no aparezcan en ninguna sesión se quedan con cero, que es una página vacía
        # EXIGIDA y no un recurso del que no se sepa nada.
        if conf["catalogo"]:
            for (codigo,) in con.execute(conf["catalogo"]):
                por_recurso[codigo]  # noqa: B018 -- crea la entrada vacía a propósito
        codigo_asignatura = columna_codigo_asignatura(con)
        for recurso, dia, tramo, asignatura, profesores, aula, grupos in con.execute("""
                select v.%s, v.dia, v.tramo, a.%s,
                       (select group_concat(p.codigo, '/') from (
                            select p2.codigo from plaza_profesor pp
                              join profesor p2 on p2.id = pp.profesor_id
                             where pp.plaza_id = s.plaza_id order by p2.codigo) p),
                       au.codigo,
                       (select group_concat(g.codigo, '/') from (
                            select distinct g2.codigo from plaza_subgrupo ps
                              join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id
                              join grupo_administrativo g2 on g2.id = sg.grupo_id
                             where ps.plaza_id = s.plaza_id order by g2.codigo) g)
                from %s v
                join sesion s on s.id = v.sesion_id
                join plaza pl on pl.id = s.plaza_id
                join asignatura a on a.id = pl.asignatura_id
                left join aula au on au.id = s.aula_id""" % (conf["recurso"], codigo_asignatura, conf["sql"])):
            campos = {"asignatura": asignatura, "profesores": profesores or "",
                      "aula": aula or "", "grupos": grupos or ""}
            texto = " ".join(campos[c] for c in conf["orden"] if campos[c])
            por_recurso[recurso][(dia, tramo)][texto] += 1
        # Las ORDINARIA son entradas «Guardia» de su celda. Las de recreo van a las bandas
        # (`bandas_esperadas`, S221), pero su profesor tiene página aunque no tenga más.
        for (profesor, dia, lugar, tipo), cuantas in guardias.items():
            if tipo == ORDINARIA:
                por_recurso[profesor][(dia, lugar[1])][ENTRADA_GUARDIA] += cuantas
            else:
                por_recurso[profesor]  # noqa: B018 -- crea la página a propósito
        return por_recurso
    finally:
        con.close()


def bandas_esperadas(ruta_db, horario_id):
    """Lo que la BD exige en las BANDAS de recreo de la página de profesor (S221, D5 y D7):
    por profesor, {(día, recreo): Counter(rótulo)}, con «Guardia» por cada RECREO y
    «Biblioteca» por cada BIBLIOTECA."""
    por_recurso = collections.defaultdict(lambda: collections.defaultdict(collections.Counter))
    for (profesor, dia, lugar, tipo), cuantas in guardias_esperadas(ruta_db, horario_id).items():
        if tipo != ORDINARIA:
            por_recurso[profesor][(dia, lugar[1])][ROTULO_DE_TIPO[tipo]] += cuantas
    return por_recurso


def texto_de_pagina(ruta_pdf, pagina):
    """El texto de UNA página, por `pdftotext`. Falla ruidosamente si no está la
    herramienta: un verificador que se salte una página en silencio no verifica nada."""
    r = subprocess.run(["pdftotext", "-f", str(pagina), "-l", str(pagina), ruta_pdf, "-"],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise Fallo("pdftotext falló en la página %d: %s" % (pagina, r.stderr.strip()))
    return r.stdout


def paginas_de(ruta_pdf):
    """Número de páginas, según `pdfinfo`."""
    r = subprocess.run(["pdfinfo", ruta_pdf], capture_output=True, text=True)
    if r.returncode != 0:
        raise Fallo("pdfinfo falló: %s" % r.stderr.strip())
    for linea in r.stdout.splitlines():
        if linea.startswith("Pages:"):
            return int(linea.split(":", 1)[1])
    raise Fallo("pdfinfo no dice cuántas páginas hay")


def rejilla_de_pagina(ruta_pdf, pagina):
    """Las celdas de la rejilla de una página: `(celdas, bandas)`, con {(dia, tramo): texto}
    para las lectivas y, desde S221, {(dia, recreo): texto} para las de las bandas de recreo
    de cinco celdas.

    SE ACOTA POR REGIÓN, con las coordenadas que da `pdftotext -bbox-layout`, y no por la
    forma del texto. La razón es medida, no estética: `pdftotext` NO emite el contenido de
    arriba abajo —en el banco real hay páginas donde entradas de la rejilla salen DESPUÉS
    de líneas de la leyenda—, así que cualquier corte por marcador («de la leyenda en
    adelante, fuera») se lleva entradas buenas por delante. Y una entrada que no cabe a lo
    ancho se parte en dos líneas que, en una fila con varias columnas ocupadas, no quedan
    contiguas en el texto plano. Dentro de una celda, en cambio, sus líneas SÍ son
    contiguas, y por eso la celda es la unidad de cotejo.

    La rejilla se reconstruye del propio fichero, sin constantes de la maqueta salvo las
    dos que fijan las columnas: la columna de horas ocupa hasta {0} pt y cada día {1} pt.
    Las FILAS se delimitan con los rótulos de hora, que van arriba de su celda: el rango
    HH:MM-HH:MM de cada fila marca dónde empieza.
    """
    raiz = ET.fromstring(subprocess.run(
        ["pdftotext", "-bbox-layout", "-f", str(pagina), "-l", str(pagina), ruta_pdf, "-"],
        capture_output=True, text=True, check=True).stdout)
    ns = "{http://www.w3.org/1999/xhtml}"

    palabras = []
    for w in raiz.iter(ns + "word"):
        palabras.append((float(w.get("xMin")), float(w.get("yMin")),
                         float(w.get("yMax")), (w.text or "").strip()))

    topes = sorted(y for x, y, _, t in palabras
                   if x < X_PRIMERA_COLUMNA and RANGO_HORARIO.fullmatch(t))
    if not topes:
        raise Fallo("la página %d no tiene rótulos de hora: ¿es una página de horario?"
                    % pagina)

    # SUELO DE LA REJILLA. La última fila no tiene rótulo debajo que la cierre, así que sin
    # este corte se tragaría la leyenda entera —y no basta con filtrar por x: una línea de
    # leyenda empieza en la columna izquierda pero sus palabras siguen hacia la derecha y
    # caen dentro de las columnas de día—. El corte es una COORDENADA; el rótulo de la
    # leyenda solo sirve para localizarla. Solo cuenta un rótulo POR DEBAJO del primer tramo:
    # la página de guardias lleva «Profesores» ya en su clave de lectura, encima de la
    # rejilla (S215), y tomarla por la leyenda dejaría la rejilla vacía.
    suelo = min((y for _, y, _, t in palabras
                 if t in ("Profesores", "Asignaturas") and y > topes[0]),
                default=float("inf"))

    def fila_de(centro):
        for i in range(len(topes) - 1, -1, -1):
            if centro >= topes[i] - 1.0:
                return i
        return None

    filas = {}
    # S221: las filas con «Recreo» en la COLUMNA DE HORAS, que son bandas de cinco celdas.
    filas_banda = set()
    for x, y0, y1, texto in palabras:
        if texto == ROTULO_RECREO and x < X_PRIMERA_COLUMNA and (y0 + y1) / 2 < suelo:
            fila = fila_de((y0 + y1) / 2)
            if fila is not None:
                filas_banda.add(fila)
        if not texto or x < X_PRIMERA_COLUMNA:
            continue
        dia = int((x - X_PRIMERA_COLUMNA) // ANCHO_COLUMNA) + 1
        if dia < 1 or dia > 5:
            continue
        centro = (y0 + y1) / 2
        if centro >= suelo:                   # de la leyenda hacia abajo, fuera
            continue
        fila = fila_de(centro)
        if fila is None:                      # cabecera de días, por encima de todo
            continue
        filas.setdefault((dia, fila), []).append((y0, x, texto))

    # Las filas de recreo no son lectivas y se descuentan para que las demás lleven la
    # numeración 1..6 que usa la proyección. Una es BANDA ÚNICA si lleva «Recreo» en la zona
    # de días (grupo y aula; todas hasta S220) y BANDA DE DÍAS si lo lleva en la columna de
    # horas (profesor y guardias, S221). Desde S221 se reconocen todas, no solo la primera, y
    # se numeran 1, 2… de arriba abajo, que es el recreo n de cada día.
    unicas = {f for (d, f), ws in filas.items() if any(t == ROTULO_RECREO for _, _, t in ws)}
    recreos = sorted(unicas | filas_banda)

    celdas, bandas = {}, {}
    for (dia, fila), ws in filas.items():
        texto = re.sub(r"[^\S\n]+", " ", unir([t for _, _, t in sorted(ws)])).strip()
        if fila in unicas:
            continue
        if fila in filas_banda:
            bandas[(dia, recreos.index(fila) + 1)] = texto
            continue
        tramo = fila + 1 - sum(1 for f in recreos if f < fila)
        celdas[(dia, tramo)] = texto
    return celdas, bandas


def celdas_de_pagina(ruta_pdf, pagina):
    """Las celdas lectivas de una página, {(dia, tramo): texto} (ver `rejilla_de_pagina`)."""
    return rejilla_de_pagina(ruta_pdf, pagina)[0]


def unir(palabras):
    """Rehace el texto de una celda a partir de las palabras que `pdftotext` devuelve.

    Normalmente se unen con un espacio, PERO no cuando la anterior acaba en `/`: ahí el
    espacio no existe en el dato, lo pone el SALTO DE LÍNEA de la maqueta. Desde S150 el
    exportador solo parte una entrada tras un espacio o tras esa barra, así que una lista
    larga se corta como `1B-A/1B-B/1B-C/` + `1B-D`, y unirla a ciegas daría
    `1B-C/ 1B-D`, que no casa con nada y se reportaría como una entrada que falta.

    El guion NO recibe este trato: un `1B-` al final de línea sería un código partido, y
    pegarle lo siguiente escondería justo el defecto que el corte nuevo evita.
    """
    texto = ""
    for palabra in palabras:
        if texto and not texto.endswith("/"):
            texto += " "
        texto += palabra
    return texto


def cotejar_celdas(celdas, esperadas_por_celda):
    """Coteja celda a celda. Devuelve un dict con lo de las SESIONES (`halladas`, `faltan`,
    `sobra`) y, aparte, lo de las GUARDIAS (`halladas_g`, `faltan_g`, `sobra_g`,
    `fuera_de_orden`).

    Dentro de una celda se tachan las entradas que la BD exige, de la más larga a la más
    corta —así una que sea subcadena de otra no se cobra las apariciones de aquélla—. Lo
    que la BD exige y no se pudo tachar FALTA; lo que queda en la celda tras tachar todo
    SOBRA, y es texto de rejilla que la base no respalda.

    Desde S215 la entrada «Guardia» se tacha con su propia marca: así se cuenta aparte, un
    «Guardia» que quede sin tachar es una guardia que SOBRA, y se comprueba que va DESPUÉS
    de todas las sesiones de su celda (C2): una sesión tachada tras ella es fuera de orden.
    El texto de la celda viene de arriba abajo (`celdas_de_pagina` ordena por la y).
    """
    r = {"halladas": 0, "faltan": collections.Counter(), "sobra": [],
         "halladas_g": 0, "faltan_g": collections.Counter(), "sobra_g": [],
         "fuera_de_orden": []}
    for clave in set(celdas) | set(esperadas_por_celda):
        texto = celdas.get(clave, "")
        exige = esperadas_por_celda.get(clave, collections.Counter())
        encontradas = collections.Counter()
        for entrada in sorted(exige, key=len, reverse=True):
            marca = "\x01" if entrada == ENTRADA_GUARDIA else "\x00"
            while encontradas[entrada] < exige[entrada] and entrada in texto:
                texto = texto.replace(entrada, marca, 1)
                encontradas[entrada] += 1
        donde = "dia %d tramo %d" % clave
        for entrada, cuantas in exige.items():
            es_guardia = entrada == ENTRADA_GUARDIA
            r["halladas_g" if es_guardia else "halladas"] += encontradas[entrada]
            if encontradas[entrada] < cuantas:
                r["faltan_g" if es_guardia else "faltan"]["%s @%s" % (entrada, donde)] += (
                    cuantas - encontradas[entrada])
        if "\x01" in texto and "\x00" in texto[texto.index("\x01"):]:
            r["fuera_de_orden"].append("%s: una sesión va después de «%s»"
                                       % (donde, ENTRADA_GUARDIA))
        palabras = texto.replace("\x00", " ").replace("\x01", " ").split()
        sobran_g = [w for w in palabras if w == ENTRADA_GUARDIA]
        resto = " ".join(w for w in palabras if w != ENTRADA_GUARDIA)
        if sobran_g:
            r["sobra_g"].append("%s: %d «%s»" % (donde, len(sobran_g), ENTRADA_GUARDIA))
        if resto:
            r["sobra"].append("%s: %r" % (donde, resto))
    return r


def cotejar_bandas(bandas, esperadas_por_banda):
    """Coteja las celdas de las bandas de recreo de una página de profesor (S221). Cada celda
    son rótulos sueltos, «Guardia» o «Biblioteca», uno por guardia: se cuentan y se comparan
    como multiconjunto con lo esperado. Una palabra que no sea un rótulo SOBRA, y también una
    banda leída sin nada esperado. Devuelve `halladas`, `faltan` (Counter de rótulo) y
    `sobra` (lista de textos)."""
    r = {"halladas": 0, "faltan": collections.Counter(), "sobra": []}
    for clave in sorted(set(bandas) | set(esperadas_por_banda)):
        palabras = bandas.get(clave, "").split()
        lee = collections.Counter(w for w in palabras if w in ROTULO_DE_TIPO.values())
        otras = [w for w in palabras if w not in ROTULO_DE_TIPO.values()]
        exige = esperadas_por_banda.get(clave, collections.Counter())
        donde = "dia %d recreo %d" % clave
        r["halladas"] += sum((exige & lee).values())
        for rotulo, n in (exige - lee).items():
            r["faltan"][rotulo] += n
        for rotulo, n in (lee - exige).items():
            r["sobra"].append("%s: %d «%s»" % (donde, n, rotulo))
        if otras:
            r["sobra"].append("%s: %r" % (donde, " ".join(otras)))
    return r


def verificar_pdf(ruta_db, horario_id, ruta_pdf, vista="grupo"):
    """Coteja el PDF entero contra la vista pedida. Devuelve True si cuadra.

    Desde S221 coteja también las bandas de recreo: en la vista de profesor, contra
    `bandas_esperadas`; en las demás no se espera ninguna banda de cinco celdas, y lo que
    se lea en una sobra. Las guardias de recreo de un profesor SIN página faltan."""
    conf = VISTAS[vista]
    uno, varios = conf["recurso"], conf["recursos"]
    esperadas = entradas_esperadas(ruta_db, horario_id, vista)
    esperadas_r = (bandas_esperadas(ruta_db, horario_id) if vista == "profesor" else {})
    paginas = paginas_de(ruta_pdf)

    print("--- ORÁCULO PDF (horario %s, vista %s) ---" % (horario_id, vista))
    # Con catálogo, `esperadas` son TODOS los recursos y no solo los que dan clase: el
    # rótulo lo dice, porque «con clases» sería falso y el número no cuadraría con las 35
    # aulas que sí tienen horario.
    rotulo = ("del catálogo" if conf["catalogo"] else
              "con clases o guardias en la BD" if vista == "profesor" else "con clases en la BD")
    print("  %s %s %s %d"
          % (varios, rotulo, "." * max(1, 22 - len(varios) - len(rotulo)), len(esperadas)))
    print("  entradas que exige la BD ..... %d"
          % sum(sum(c.values()) for g in esperadas.values() for c in g.values()))
    if conf["catalogo"]:
        print("  %s del catálogo SIN clases %s %d"
              % (varios, "." * (20 - len(varios)),
                 sum(1 for g in esperadas.values() if not g)))
    print("  páginas del PDF .............. %d" % paginas)

    if paginas != len(esperadas):
        print("  FALLO: %d páginas para %d %s %s" % (paginas, len(esperadas), varios, rotulo))

    vistos, total_faltan, total_sobran, total_halladas = set(), 0, 0, 0
    # Las guardias, aparte (S215): solo las espera la vista de profesor, pero un «Guardia»
    # colado en otra vista también se cuenta aquí como sobrante.
    total_exige_g, total_halladas_g, total_faltan_g, total_sobran_g = 0, 0, 0, 0
    total_fuera_de_orden = 0
    # Las guardias de recreo de las bandas, aparte también (S221), con los rótulos que faltan.
    total_exige_r, total_halladas_r, total_sobran_r = 0, 0, 0
    total_faltan_r = collections.Counter()
    # Contador APARTE. Una página vacía con leyenda no es una entrada de más: no hay
    # ninguna entrada en esa página. Sumarla a SOBRAN mezclaba dos defectos distintos
    # en un número, y encima contaba dos —un encabezado colado por cada rótulo— donde
    # el defecto es UNO: esa página no debía llevar leyenda.
    total_vacias_con_leyenda = 0
    for pagina in range(1, paginas + 1):
        cabecera = [l.strip() for l in texto_de_pagina(ruta_pdf, pagina).splitlines()
                    if l.strip()]
        titulo = cabecera[0] if cabecera else "(vacía)"
        # El título de una página puede llevar el nombre detrás del código («DIB2 — Ramírez
        # Soto, Ana»): el recurso es lo que va ANTES de la raya. Se parte por la raya y no
        # por el espacio porque hay códigos con espacios dentro.
        recurso = titulo.split(" — ", 1)[0].strip()
        vistos.add(recurso)
        if recurso not in esperadas:
            print("  pág %2d: FALLO, el %s %r no tiene %s en la BD"
                  % (pagina, uno, recurso,
                     "clases ni guardias" if vista == "profesor" else "clases"))
            total_sobran += 1
            continue
        celdas, bandas = rejilla_de_pagina(ruta_pdf, pagina)
        cotejo = cotejar_celdas(celdas, esperadas[recurso])
        cotejo_r = cotejar_bandas(bandas, esperadas_r.get(recurso, {}))
        exige_r = sum(sum(c.values()) for c in esperadas_r.get(recurso, {}).values())
        total_exige_r += exige_r
        total_halladas_r += cotejo_r["halladas"]
        total_faltan_r.update(cotejo_r["faltan"])
        total_sobran_r += len(cotejo_r["sobra"])
        halladas, faltan, sobra = cotejo["halladas"], cotejo["faltan"], cotejo["sobra"]
        exige = sum(sum(c.values()) for c in esperadas[recurso].values())
        exige_g = sum(c[ENTRADA_GUARDIA] for c in esperadas[recurso].values())

        # Una página que la BD deja SIN NINGUNA entrada no puede llevar leyenda: sus
        # encabezados anunciarían una lista que no existe. Solo se puede comprobar donde
        # hay páginas vacías, o sea en las vistas con catálogo.
        if exige == 0:
            texto = texto_de_pagina(ruta_pdf, pagina)
            colados = [r for r in ("Profesores", "Asignaturas") if r in texto]
            if colados:
                print("  pág %2d  %-7s  VACÍA pero lleva leyenda: %s   <<< DESCUADRE"
                      % (pagina, recurso, ", ".join(colados)))
                total_vacias_con_leyenda += 1
        n_faltan = sum(faltan.values())
        n_sobran = len(sobra)
        n_faltan_g = sum(cotejo["faltan_g"].values())
        n_sobran_g = len(cotejo["sobra_g"])
        n_fuera = len(cotejo["fuera_de_orden"])
        total_halladas += halladas
        total_faltan += n_faltan
        total_sobran += n_sobran
        total_exige_g += exige_g
        total_halladas_g += cotejo["halladas_g"]
        total_faltan_g += n_faltan_g
        total_sobran_g += n_sobran_g
        total_fuera_de_orden += n_fuera
        n_faltan_r = sum(cotejo_r["faltan"].values())
        n_sobran_r = len(cotejo_r["sobra"])
        marca = ("" if not (n_faltan or n_sobran or n_faltan_g or n_sobran_g or n_fuera
                            or n_faltan_r or n_sobran_r)
                 else "   <<< DESCUADRE")
        guardias = ("" if not (exige_g or n_sobran_g or n_fuera) else
                    "  guardias: exige %d halla %d" % (exige_g, cotejo["halladas_g"]))
        recreo = ("" if not (exige_r or n_sobran_r) else
                  "  recreo: exige %d halla %d" % (exige_r, cotejo_r["halladas"]))
        print("  pág %2d  %-7s  exige %3d  halla %3d  faltan %d  sobran %d%s%s%s"
              % (pagina, recurso, exige - exige_g, halladas, n_faltan, n_sobran, guardias,
                 recreo, marca))
        for entrada, cuantas in sorted(faltan.items()) + sorted(cotejo["faltan_g"].items()):
            print("        FALTA x%d: %s" % (cuantas, entrada))
        for rotulo_r, cuantas in sorted(cotejo_r["faltan"].items()):
            print("        FALTA x%d en las bandas de recreo: %s" % (cuantas, rotulo_r))
        for celda in sobra + cotejo["sobra_g"] + cotejo_r["sobra"]:
            print("        SOBRA en %s" % celda)
        for celda in cotejo["fuera_de_orden"]:
            print("        FUERA DE ORDEN en %s" % celda)

    sin_pagina = sorted(set(esperadas) - vistos)
    if sin_pagina:
        print("  FALLO: %s %s y SIN página: %s" % (varios, rotulo, ", ".join(sin_pagina)))
    # Las guardias de recreo de un profesor sin página no se ven en ninguna parte: faltan.
    for recurso in sin_pagina:
        for c in esperadas_r.get(recurso, {}).values():
            total_exige_r += sum(c.values())
            total_faltan_r.update(c)

    print("  TOTAL: halladas %d, FALTAN %d, SOBRAN %d"
          % (total_halladas, total_faltan, total_sobran))
    print("  TOTAL guardias: exigidas %d, halladas %d, FALTAN %d, SOBRAN %d, fuera de orden %d"
          % (total_exige_g, total_halladas_g, total_faltan_g, total_sobran_g,
             total_fuera_de_orden))
    print("  TOTAL guardias de recreo: exigidas %d, halladas %d, FALTAN %d (%s), SOBRAN %d"
          % (total_exige_r, total_halladas_r, sum(total_faltan_r.values()),
             ", ".join("«%s» %d" % (r, total_faltan_r[r])
                       for r in (ENTRADA_GUARDIA, ENTRADA_BIBLIOTECA)),
             total_sobran_r))
    if conf["catalogo"]:
        print("  TOTAL: páginas vacías con leyenda: %d" % total_vacias_con_leyenda)
    return (not total_faltan and not total_sobran and not total_vacias_con_leyenda
            and not total_faltan_g and not total_sobran_g and not total_fuera_de_orden
            and not total_faltan_r and not total_sobran_r
            and not sin_pagina and paginas == len(esperadas))


# --------------------------------------------------------------------------- página de guardias (S215)

VISTA_GUARDIAS = "guardias"
TITULO_GUARDIAS = "Guardias"          # X4 (S221); hasta S220, «Guardias ordinarias»
CLAVE_GUARDIAS = "Profesores de guardia"
PREFIJO_BIBLIOTECA = "Biblioteca:"    # X3 (S221): «Biblioteca: COD», en línea aparte


def verificar_pagina_guardias(ruta_db, horario_id, ruta_pdf):
    """Coteja la página de guardias contra la tabla `guardia`. Devuelve True si cuadra.

    Escrito sobre la página REAL (S215 T2, E6.d), no sobre lo que el exportador dice hacer:
      - el fichero tiene UNA página; su primera línea es el título y la segunda la clave;
      - las celdas se reconstruyen con `celdas_de_pagina`, por región, igual que en las otras
        vistas: en el texto plano, las columnas de lunes y martes se entremezclan;
      - el texto de una celda son códigos separados por coma; el salto de línea cae tras la
        coma y su espacio, así que partir por la coma y quitar espacios da los códigos;
      - por celda se compara el MULTICONJUNTO leído con el esperado: un código de más SOBRA, uno
        que no está FALTA y un código repetido en la celda también sobra.
    El orden de los códigos dentro de la celda no se coteja aquí: lo fija el test de D3.

    Desde S221 la página lleva también las guardias de recreo, en las bandas de cinco celdas:
    en cada una, los códigos de RECREO separados por coma y, aparte, «Biblioteca: COD». Se
    coteja por banda el multiconjunto (código, tipo). La página se reconoce por un título que
    empieza por «Guardias»; si no es exactamente «Guardias», es un fallo de forma."""
    esperadas = collections.defaultdict(collections.Counter)
    esperadas_r = collections.defaultdict(collections.Counter)
    for (profesor, dia, lugar, tipo), n in guardias_esperadas(ruta_db, horario_id).items():
        if tipo == ORDINARIA:
            esperadas[(dia, lugar[1])][profesor] += n
        else:
            esperadas_r[(dia, lugar[1])][(profesor, tipo)] += n
    paginas = paginas_de(ruta_pdf)
    lineas = [l.strip() for l in texto_de_pagina(ruta_pdf, 1).splitlines() if l.strip()]

    print("--- ORÁCULO PDF (horario %s, vista guardias) ---" % horario_id)
    print("  guardias que exige la BD ..... %d"
          % sum(sum(c.values()) for c in esperadas.values()))
    print("  celdas con guardia en la BD .. %d" % len(esperadas))
    print("  guardias de recreo en la BD .. %d" % sum(sum(c.values()) for c in esperadas_r.values()))
    print("  bandas con guardia en la BD .. %d" % len(esperadas_r))
    print("  páginas del PDF .............. %d" % paginas)
    bien = True
    if paginas != 1:
        print("  FALLO: la página de guardias es UNA y el fichero trae %d" % paginas)
        bien = False
    titulo = lineas[0] if lineas else "(vacía)"
    if not titulo.startswith(TITULO_GUARDIAS):
        print("  FALLO: el título %r no empieza por %r: no es la página de guardias"
              % (titulo, TITULO_GUARDIAS))
        bien = False
    elif titulo != TITULO_GUARDIAS:
        print("  FALLO DE FORMA: título %r, se esperaba %r" % (titulo, TITULO_GUARDIAS))
        bien = False
    if lineas[1:2] != [CLAVE_GUARDIAS]:
        print("  FALLO: clave %r, se esperaba %r" % (lineas[1:2], CLAVE_GUARDIAS))
        bien = False

    celdas, bandas = rejilla_de_pagina(ruta_pdf, 1)
    leidas = {}
    for clave, texto in celdas.items():
        leidas[clave] = collections.Counter(c.strip() for c in texto.split(",") if c.strip())
    halladas, faltan, sobran = 0, 0, 0
    for clave in sorted(set(esperadas) | set(leidas)):
        exige = esperadas.get(clave, collections.Counter())
        lee = leidas.get(clave, collections.Counter())
        falta, sobra = exige - lee, lee - exige
        halladas += sum((exige & lee).values())
        faltan += sum(falta.values())
        sobran += sum(sobra.values())
        if falta or sobra:
            print("  dia %d tramo %d  exige %d  lee %d   <<< DESCUADRE"
                  % (clave[0], clave[1], sum(exige.values()), sum(lee.values())))
            for codigo, n in sorted(falta.items()):
                print("        FALTA x%d: %s" % (n, codigo))
            for codigo, n in sorted(sobra.items()):
                print("        SOBRA x%d: %s" % (n, codigo))
    print("  TOTAL guardias: halladas %d, FALTAN %d, SOBRAN %d" % (halladas, faltan, sobran))

    # Las bandas de recreo (S221): delante de «Biblioteca:», los códigos de RECREO; detrás,
    # los de BIBLIOTECA. Los dos lados, partidos por la coma.
    leidas_r = {}
    for clave, texto in bandas.items():
        de_recreo, _, de_biblioteca = texto.partition(PREFIJO_BIBLIOTECA)
        lee = collections.Counter()
        for trozo, tipo in ((de_recreo, RECREO), (de_biblioteca, BIBLIOTECA)):
            for codigo in trozo.split(","):
                if codigo.strip():
                    lee[(codigo.strip(), tipo)] += 1
        leidas_r[clave] = lee
    halladas_r, faltan_r, sobran_r = 0, collections.Counter(), collections.Counter()
    for clave in sorted(set(esperadas_r) | set(leidas_r)):
        exige = esperadas_r.get(clave, collections.Counter())
        lee = leidas_r.get(clave, collections.Counter())
        falta, sobra = exige - lee, lee - exige
        halladas_r += sum((exige & lee).values())
        for (_, tipo), n in falta.items():
            faltan_r[tipo] += n
        for (_, tipo), n in sobra.items():
            sobran_r[tipo] += n
        if falta or sobra:
            print("  dia %d recreo %d  exige %d  lee %d   <<< DESCUADRE"
                  % (clave[0], clave[1], sum(exige.values()), sum(lee.values())))
            for (codigo, tipo), n in sorted(falta.items()):
                print("        FALTA x%d: %s (%s)" % (n, codigo, tipo))
            for (codigo, tipo), n in sorted(sobra.items()):
                print("        SOBRA x%d: %s (%s)" % (n, codigo, tipo))
    print("  TOTAL guardias de recreo: halladas %d, FALTAN %d (RECREO %d, BIBLIOTECA %d), "
          "SOBRAN %d (RECREO %d, BIBLIOTECA %d)"
          % (halladas_r, sum(faltan_r.values()), faltan_r[RECREO], faltan_r[BIBLIOTECA],
             sum(sobran_r.values()), sobran_r[RECREO], sobran_r[BIBLIOTECA]))
    return (bien and not faltan and not sobran
            and not sum(faltan_r.values()) and not sum(sobran_r.values()))


def censo_de_cuerpos(ruta_pdf):
    """Censo de los tamaños de fuente del documento, leyendo los operadores Tf del flujo
    DESCOMPRIMIDO. Esto sí se lee del flujo —es geometría, no texto— y a propósito no se
    usa `pdftohtml`, que redondea 7,99 a 8,00 y redondearía igual un 7,6."""
    import zlib
    datos = open(ruta_pdf, "rb").read()
    censo = collections.Counter()
    for m in re.finditer(rb"stream\r?\n", datos):
        ini = m.end()
        fin = datos.find(b"endstream", ini)
        try:
            crudo = zlib.decompress(datos[ini:fin])
        except Exception:
            continue
        for t in re.finditer(rb"/F\d+\s+([0-9.]+)\s+Tf", crudo):
            censo[float(t.group(1))] += 1
    return censo


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("modo", choices=["csv", "pdf"])
    parser.add_argument("db", help="copia de la BD (se abre en modo ro)")
    parser.add_argument("horario_id", type=int)
    parser.add_argument("fichero", help="fichero exportado a verificar")
    parser.add_argument("--vista", choices=sorted(list(VISTAS) + [VISTA_GUARDIAS]), default="grupo",
                        help="modo pdf: qué vista se espera en el fichero (por defecto grupo)")
    args = parser.parse_args()

    try:
        guarda_bloques(args.db, args.horario_id)
    except Fallo as e:
        print("FALLO: %s" % e, file=sys.stderr)
        return 1

    if args.modo == "pdf" and args.vista == VISTA_GUARDIAS:
        try:
            return 0 if verificar_pagina_guardias(
                    args.db, args.horario_id, args.fichero) else 1
        except Fallo as e:
            print("FALLO: %s" % e, file=sys.stderr)
            return 1

    if args.modo == "pdf":
        try:
            return 0 if verificar_pdf(
                    args.db, args.horario_id, args.fichero, args.vista) else 1
        except Fallo as e:
            print("FALLO: %s" % e, file=sys.stderr)
            return 1

    vistas_oraculo = oraculo(args.db, args.horario_id)
    try:
        guardias_oraculo = guardias_esperadas(args.db, args.horario_id)
        print("  guardias del horario ......... %d" % sum(guardias_oraculo.values()))
        vistas_csv, guardias_csv = desde_csv(args.fichero)
    except Fallo as e:
        print("FALLO: %s" % e, file=sys.stderr)
        return 1

    iguales = [comparar(n, vistas_oraculo[n], vistas_csv[n])
               for n in ("grupo", "profesor", "aula")]
    iguales.append(comparar_guardias(guardias_oraculo, guardias_csv))
    if all(iguales):
        print("OK: las tres vistas coinciden")
        return 0
    print("FALLO: alguna vista o las guardias no coinciden (detalle arriba)", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
