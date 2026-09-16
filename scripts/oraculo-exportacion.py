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

Uso:
  oraculo-exportacion.py csv <copia.db> <horario_id> <fichero.csv>
  oraculo-exportacion.py pdf <copia.db> <horario_id> <fichero.pdf> [--vista grupo|profesor]
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


def entradas_esperadas(ruta_db, horario_id, vista="grupo"):
    """Entradas de rejilla que la BD exige, por código de RECURSO de la vista.

    Reutiliza SQL_VISTAS: `v_grupo` y `v_profesor` ya dicen qué recurso ve qué sesión, y de
    ahí se cuelgan asignatura, profesores, aula y grupos. El TEXTO se compone con el orden
    que esa vista imprime —«asignatura, profesor, aula» en la de grupo; «asignatura, aula,
    grupo» en la de profesor—, que es el de los horarios del centro. Devuelve un Counter
    por recurso: la MULTIPLICIDAD importa, porque la misma clase puede repetirse en la
    semana.
    """
    conf = VISTAS[vista]
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
        for recurso, dia, tramo, asignatura, profesores, aula, grupos in con.execute("""
                select v.%s, v.dia, v.tramo, a.codigo,
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
                join v_sesion s on s.sesion_id = v.sesion_id
                join plaza pl on pl.id = s.plaza_id
                join asignatura a on a.id = pl.asignatura_id
                join aula au on au.id = s.aula_id""" % (conf["recurso"], conf["sql"])):
            campos = {"asignatura": asignatura, "profesores": profesores or "",
                      "aula": aula, "grupos": grupos or ""}
            texto = " ".join(campos[c] for c in conf["orden"])
            por_recurso[recurso][(dia, tramo)][texto] += 1
        return por_recurso
    finally:
        con.close()


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


def celdas_de_pagina(ruta_pdf, pagina):
    """Las celdas de la rejilla de una página: {(dia, tramo): texto}.

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
    # leyenda solo sirve para localizarla.
    suelo = min((y for _, y, _, t in palabras if t in ("Profesores", "Asignaturas")),
                default=float("inf"))

    def fila_de(centro):
        for i in range(len(topes) - 1, -1, -1):
            if centro >= topes[i] - 1.0:
                return i
        return None

    filas = {}
    for x, y0, y1, texto in palabras:
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

    # La fila del recreo no es lectiva: se identifica por su rótulo y se descuenta para
    # que las demás lleven la numeración 1..6 que usa la proyección.
    fila_recreo = next((f for (d, f), ws in filas.items()
                        if any(t == "Recreo" for _, _, t in ws)), None)

    celdas = {}
    for (dia, fila), ws in filas.items():
        if fila == fila_recreo:
            continue
        tramo = fila + 1 if (fila_recreo is None or fila < fila_recreo) else fila
        texto = unir([t for _, _, t in sorted(ws)])
        celdas[(dia, tramo)] = re.sub(r"[^\S\n]+", " ", texto).strip()
    return celdas


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
    """Coteja celda a celda. Devuelve (halladas, faltan, sobra).

    Dentro de una celda se tachan las entradas que la BD exige, de la más larga a la más
    corta —así una que sea subcadena de otra no se cobra las apariciones de aquélla—. Lo
    que la BD exige y no se pudo tachar FALTA; lo que queda en la celda tras tachar todo
    SOBRA, y es texto de rejilla que la base no respalda.
    """
    halladas = 0
    faltan = collections.Counter()
    sobra = []
    for clave in set(celdas) | set(esperadas_por_celda):
        texto = celdas.get(clave, "")
        exige = esperadas_por_celda.get(clave, collections.Counter())
        encontradas = collections.Counter()
        for entrada in sorted(exige, key=len, reverse=True):
            while encontradas[entrada] < exige[entrada] and entrada in texto:
                texto = texto.replace(entrada, "\x00", 1)
                encontradas[entrada] += 1
        halladas += sum(encontradas.values())
        for entrada, cuantas in exige.items():
            if encontradas[entrada] < cuantas:
                faltan["%s @dia %d tramo %d" % (entrada, clave[0], clave[1])] += (
                    cuantas - encontradas[entrada])
        resto = " ".join(texto.replace("\x00", " ").split())
        if resto:
            sobra.append("dia %d tramo %d: %r" % (clave[0], clave[1], resto))
    return halladas, faltan, sobra


def verificar_pdf(ruta_db, horario_id, ruta_pdf, vista="grupo"):
    """Coteja el PDF entero contra la vista pedida. Devuelve True si cuadra."""
    conf = VISTAS[vista]
    uno, varios = conf["recurso"], conf["recursos"]
    esperadas = entradas_esperadas(ruta_db, horario_id, vista)
    paginas = paginas_de(ruta_pdf)

    print("--- ORÁCULO PDF (horario %s, vista %s) ---" % (horario_id, vista))
    # Con catálogo, `esperadas` son TODOS los recursos y no solo los que dan clase: el
    # rótulo lo dice, porque «con clases» sería falso y el número no cuadraría con las 35
    # aulas que sí tienen horario.
    rotulo = "del catálogo" if conf["catalogo"] else "con clases en la BD"
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
            print("  pág %2d: FALLO, el %s %r no tiene clases en la BD"
                  % (pagina, uno, recurso))
            total_sobran += 1
            continue
        halladas, faltan, sobra = cotejar_celdas(
            celdas_de_pagina(ruta_pdf, pagina), esperadas[recurso])
        exige = sum(sum(c.values()) for c in esperadas[recurso].values())

        # Una página que la BD deja SIN NINGUNA entrada no puede llevar leyenda: sus
        # encabezados anunciarían una lista que no existe. Solo se puede comprobar donde
        # hay páginas vacías, o sea en las vistas con catálogo.
        if exige == 0:
            texto = texto_de_pagina(ruta_pdf, pagina)
            colados = [r for r in ("Profesores", "Asignaturas") if r in texto]
            if colados:
                print("  pág %2d  %-7s  VACÍA pero lleva leyenda: %s   <<< DESCUADRE"
                      % (pagina, recurso, ", ".join(colados)))
                total_sobran += len(colados)
        n_faltan = sum(faltan.values())
        n_sobran = len(sobra)
        total_halladas += halladas
        total_faltan += n_faltan
        total_sobran += n_sobran
        marca = "" if not (n_faltan or n_sobran) else "   <<< DESCUADRE"
        print("  pág %2d  %-7s  exige %3d  halla %3d  faltan %d  sobran %d%s"
              % (pagina, recurso, exige, halladas, n_faltan, n_sobran, marca))
        for entrada, cuantas in sorted(faltan.items()):
            print("        FALTA x%d: %s" % (cuantas, entrada))
        for celda in sobra:
            print("        SOBRA en %s" % celda)

    sin_pagina = sorted(set(esperadas) - vistos)
    if sin_pagina:
        print("  FALLO: %s %s y SIN página: %s" % (varios, rotulo, ", ".join(sin_pagina)))

    print("  TOTAL: halladas %d, FALTAN %d, SOBRAN %d"
          % (total_halladas, total_faltan, total_sobran))
    return (not total_faltan and not total_sobran and not sin_pagina
            and paginas == len(esperadas))


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
    parser.add_argument("--vista", choices=sorted(VISTAS), default="grupo",
                        help="modo pdf: qué vista se espera en el fichero (por defecto grupo)")
    args = parser.parse_args()

    if args.modo == "pdf":
        try:
            return 0 if verificar_pdf(
                    args.db, args.horario_id, args.fichero, args.vista) else 1
        except Fallo as e:
            print("FALLO: %s" % e, file=sys.stderr)
            return 1

    vistas_oraculo = oraculo(args.db, args.horario_id)
    try:
        vistas_csv = desde_csv(args.fichero)
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
