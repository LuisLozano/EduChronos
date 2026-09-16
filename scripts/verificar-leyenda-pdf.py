#!/usr/bin/env python3
"""Verifica la LEYENDA de un PDF de horario contra la base (condición 1 de O-exportación).

Nace en S150 (C-exportacion-pdf-profesor-aula, M4-B). Comprueba lo que el oráculo de
`oraculo-exportacion.py` NO mira: allí la leyenda sólo sirve de coordenada para acotar la
rejilla y para exigir que una página vacía no la lleve; lo que DICE no lo cotejaba nadie.
Aquí se exige, por página:

  - toda asignatura con entrada en la página tiene su línea «CÓDIGO — nombre», con el
    nombre_completo EXACTO del catálogo;
  - en las vistas de grupo y de aula, lo mismo para cada profesor con entrada;
  - en la de profesor, el TÍTULO es «CÓDIGO — nombre» del titular y no hay bloque de
    profesores;
  - EXCLUSIVIDAD: ninguna línea de leyenda nombra un código que no tenga entrada en esa
    página.

NO IMPORTA NADA DEL EXPORTADOR, igual que el oráculo: los nombres se leen de la BD y el
texto del PDF. Si leyera `VistaPdf` para saber qué esperar, un error compartido por las
dos vías pasaría desapercibido. La BD se abre en SOLO LECTURA (`mode=ro`).

LA LEYENDA SE LEE POR REGIÓN, con `pdftotext -bbox-layout`, y no del texto plano. Con
`-layout` las dos columnas caen en la MISMA línea de texto
(«FIL1 — Montes García, Antonio      Fr2 — Francés 2»), así que partir por líneas fundiría
entradas de bloques distintos. Aquí se localizan las x de los encabezados «Profesores» y
«Asignaturas», cada una abre una columna hasta la x de la siguiente, y dentro de cada
columna las palabras se agrupan por su y. Una línea de continuación —un nombre que no
cupo— no lleva « — » y se pega a la anterior.

VALIDADO CON TRES MUTACIONES en S150, sobre copias de la base y antes de creerle:
  (a) cambiado el `nombre_completo` de un profesor → falla en sus páginas de grupo y de
      aula, y en el TÍTULO de su página de profesor;
  (b) cambiado el `nombre_completo` de una asignatura → falla en las tres vistas;
  (c) borradas de la base las sesiones de una asignatura en un grupo, dejando su línea de
      leyenda en el PDF → salta la EXCLUSIVIDAD. La (c) se añadió porque (a) y (b) cambian
      NOMBRES y no códigos, y sin ella esa cuarta comprobación quedaba escrita pero sin
      probar.

COSTE: dos llamadas a `pdftotext` por página, unos 2 min para las 44 de la vista de aula.
Mejora posible si molesta: una sola llamada a `-bbox-layout` para el documento entero,
cacheando las palabras por página en vez de invocarlo página a página.

Uso:
  verificar-leyenda-pdf.py <copia.db> <horario_id> <fichero.pdf> <grupo|profesor|aula>
"""
import collections, re, sqlite3, subprocess, sys
import xml.etree.ElementTree as ET

NS = "{http://www.w3.org/1999/xhtml}"
RAYA = " — "
DIAS = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"]


def palabras_de(ruta, pagina):
    raiz = ET.fromstring(subprocess.run(
        ["pdftotext", "-bbox-layout", "-f", str(pagina), "-l", str(pagina), ruta, "-"],
        capture_output=True, text=True, check=True).stdout)
    return [(float(w.get("xMin")), float(w.get("yMin")), (w.text or "").strip())
            for w in raiz.iter(NS + "word") if (w.text or "").strip()]


def leyenda_de(ruta, pagina):
    """{encabezado: [lineas]} de una página. Vacío si no hay leyenda."""
    ws = palabras_de(ruta, pagina)
    cabeceras = [(x, y, t) for x, y, t in ws if t in ("Profesores", "Asignaturas")]
    if not cabeceras:
        return {}
    suelo = min(y for _, y, _ in cabeceras)
    columnas = sorted((x, t) for x, y, t in cabeceras)
    bloques = {}
    for i, (x0, rotulo) in enumerate(columnas):
        x1 = columnas[i + 1][0] if i + 1 < len(columnas) else 10_000.0
        porLinea = collections.defaultdict(list)
        for x, y, t in ws:
            if y > suelo + 1.0 and x0 - 1.0 <= x < x1 - 1.0:
                porLinea[round(y, 1)].append((x, t))
        lineas = []
        for y in sorted(porLinea):
            texto = " ".join(t for _, t in sorted(porLinea[y]))
            if RAYA in texto or not lineas:
                lineas.append(texto)
            else:
                lineas[-1] += " " + texto   # continuación de un nombre largo
        bloques[rotulo] = lineas
    return bloques


def titulo_de(ruta, pagina):
    ws = palabras_de(ruta, pagina)
    techo = min(y for _, y, _ in ws)
    return " ".join(t for _, t in sorted(
        (x, t) for x, y, t in ws if y <= techo + 1.0))


def datos(ruta_db, horario_id, vista):
    """Por recurso: asignaturas y profesores CON entrada, y los nombres de catálogo."""
    con = sqlite3.connect("file:%s?mode=ro" % ruta_db, uri=True)
    tramos = list(con.execute("select id, dia, orden, es_lectivo from tramo_semanal"))
    lect = sorted([t for t in tramos if t[3]], key=lambda t: (DIAS.index(t[1]), t[2]))
    nAsig = dict(con.execute("select codigo, nombre_completo from asignatura"))
    nProf = dict(con.execute("select codigo, nombre_completo from profesor"))
    profs = collections.defaultdict(set)
    for pid, c in con.execute("select pp.plaza_id, p.codigo from plaza_profesor pp "
                              "join profesor p on p.id = pp.profesor_id"):
        profs[pid].add(c)
    grupos = collections.defaultdict(set)
    for pid, c in con.execute("select ps.plaza_id, g.codigo from plaza_subgrupo ps "
                              "join subgrupo_grupo sg on sg.subgrupo_id = ps.subgrupo_id "
                              "join grupo_administrativo g on g.id = sg.grupo_id"):
        grupos[pid].add(c)
    porRecurso = collections.defaultdict(lambda: {"asig": set(), "prof": set()})
    for plaza, aula, asig in con.execute(
            "select s.plaza_id, a.codigo, g.codigo from sesion s "
            "join aula a on a.id = s.aula_id join plaza pl on pl.id = s.plaza_id "
            "join asignatura g on g.id = pl.asignatura_id where s.horario_id = ?",
            (horario_id,)):
        if vista == "grupo":
            recursos = grupos.get(plaza, set())
        elif vista == "profesor":
            recursos = profs.get(plaza, set())
        else:
            recursos = {aula}
        for r in recursos:
            porRecurso[r]["asig"].add(asig)
            porRecurso[r]["prof"].update(profs.get(plaza, set()))
    con.close()
    return porRecurso, nAsig, nProf


def verificar(ruta_db, horario_id, ruta_pdf, vista):
    porRecurso, nAsig, nProf = datos(ruta_db, horario_id, vista)
    paginas = int(re.search(r"Pages:\s+(\d+)", subprocess.run(
        ["pdfinfo", ruta_pdf], capture_output=True, text=True, check=True).stdout).group(1))
    print("--- LEYENDA (vista %s, %d páginas) ---" % (vista, paginas))
    ok = 0
    fallos = []
    for pag in range(1, paginas + 1):
        titulo = titulo_de(ruta_pdf, pag)
        recurso = titulo.split(RAYA, 1)[0].strip()
        bloques = leyenda_de(ruta_pdf, pag)
        malas = []

        esperadas = porRecurso.get(recurso, {"asig": set(), "prof": set()})

        # (1) toda asignatura con entrada tiene su línea, con el nombre de catálogo exacto
        lineasA = bloques.get("Asignaturas", [])
        for cod in sorted(esperadas["asig"]):
            if (cod + RAYA + nAsig[cod]) not in lineasA:
                malas.append("falta o no cuadra la asignatura %r (esperado %r, hay %r)"
                             % (cod, cod + RAYA + nAsig[cod],
                                [l for l in lineasA if l.startswith(cod + RAYA)]))
        # (2) grupo y aula: todo profesor con entrada tiene su línea
        if vista in ("grupo", "aula"):
            lineasP = bloques.get("Profesores", [])
            for cod in sorted(esperadas["prof"]):
                if (cod + RAYA + nProf[cod]) not in lineasP:
                    malas.append("falta o no cuadra el profesor %r (esperado %r, hay %r)"
                                 % (cod, cod + RAYA + nProf[cod],
                                    [l for l in lineasP if l.startswith(cod + RAYA)]))
        # (3) profesor: el TÍTULO es "COD — nombre" del titular
        if vista == "profesor":
            if titulo != recurso + RAYA + nProf.get(recurso, ""):
                malas.append("título %r, esperado %r"
                             % (titulo, recurso + RAYA + nProf.get(recurso, "")))
            if "Profesores" in bloques:
                malas.append("lleva bloque de profesores, que esta vista no tiene")
        # (4) EXCLUSIVIDAD: ninguna línea nombra un código sin entrada en la página
        for rotulo, lineas in bloques.items():
            permitidos = esperadas["asig"] if rotulo == "Asignaturas" else esperadas["prof"]
            for linea in lineas:
                if RAYA not in linea:
                    malas.append("línea de leyenda sin raya: %r" % linea)
                    continue
                cod = linea.split(RAYA, 1)[0].strip()
                if cod not in permitidos:
                    malas.append("%s: %r no tiene entrada en esta página" % (rotulo, cod))

        if malas:
            fallos.append((pag, recurso, malas))
        else:
            ok += 1
    for pag, recurso, malas in fallos:
        print("  pág %2d  %-24s FALLA" % (pag, recurso))
        for m in malas[:4]:
            print("        %s" % m)
    print("  páginas OK: %d   con fallo: %d" % (ok, len(fallos)))
    return not fallos


if __name__ == "__main__":
    bien = verificar(sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4])
    sys.exit(0 if bien else 1)
