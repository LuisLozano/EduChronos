#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Deriva los nombres completos de profesores y asignaturas del PDF de horarios de grupos.

USO
    python3 -B tools/carga-centro/extraer-nombres.py --pdf <pdf> [<pdf> ...] \
        --catalogo <catalogo-derivado.json> --salida <nombres-derivados.json>

    2025/2026: --pdf "docs/horario-referencia/pdf/Horarios de grupos.pdf"
               --catalogo docs/horario-referencia/catalogo-derivado.json
               --salida docs/horario-referencia/nombres-derivados.json
    Sin argumentos no escribe nada (S198: las tres rutas eran fijas).

VARIOS PDF (S198). Las variantes de todos los PDF se juntan por codigo y se aplica la
misma regla de eleccion: el resultado no depende del orden de los PDF. Un truncamiento
en un PDF y el nombre completo en otro se resuelven por prefijo; dos nombres que no son
prefijo el uno del otro quedan como conflicto, igual que dentro de un PDF.

CIERRE. Las claves tienen que ser las de --catalogo en las dos familias. La salida se
escribe solo si el cierre pasa; si no, rc=1 y no se escribe nada.

--decisiones (S204, opcional): decisiones-catalogo.json del curso. Antes del cierre se
anaden los nombres que el PDF de grupos no trae: los de las decisiones asignaturas.nombre
y el nombreCompleto de profesores.alta, con procedencia «decision:<id>». Un codigo que ya
viene del PDF, o que esta en dos decisiones, es un error. Sin --decisiones la salida no
cambia. Su «truncado» sale de la misma regla que el de la leyenda, con ANCHO_LEYENDA.

Cada pagina trae al pie dos leyendas, "Profesores:" y "Asignaturas:", que asocian
codigo -> nombre en dos columnas, y arriba una linea "Tutor:" con el nombre del
tutor del grupo. El mismo codigo aparece en varias paginas y no siempre con el
mismo texto: el PDF trunca por ancho de columna (24 caracteres en la leyenda, 35
en la linea de tutor) y a veces pierde tildes. De ahi la regla de eleccion.

LECTURA DEL PDF
    pdftotext -layout (poppler). El PDF tiene capa de texto vectorial, asi que la
    extraccion es determinista y no hay OCR de por medio. Dentro de una leyenda,
    las celdas se separan por dos o mas espacios; se ha verificado sobre las 448
    lineas de leyenda de las 28 paginas que el corte da siempre 2 o 4 campos, es
    decir que ningun nombre contiene un espacio doble interno.

REGLA DE ELECCION entre las variantes de un mismo codigo, en este orden:
    1. Prefijo: si la normalizada de A es prefijo ESTRICTO de la de B, A cae. Es
       el caso del truncamiento. Se aplica por pares hasta quedarse con las
       variantes que no son prefijo de ninguna otra.
    2. Diacriticos: entre las supervivientes con la MISMA normalizada gana la que
       mas caracteres con diacritico tiene. Esto es lo que hace que CyR salga
       "Computacion y Robotica" -> "Computacion y Robotica" con tildes.
    3. Lexicografico sobre el texto original. El desempate es siempre por orden
       explicito: no se recorre ningun set ni nada dependiente del hash, de modo
       que dos corridas dan byte a byte el mismo fichero.
    4. Conflicto: si tras el paso 1 quedan dos o mas variantes cuyas normalizadas
       no son prefijo la una de la otra, no es truncamiento sino discrepancia
       real de la fuente. Se elige lexicograficamente y la entrada se marca con
       "conflicto": true y la lista completa de variantes.

NORMALIZACION
    Solo para COMPARAR. El valor que se guarda es siempre el texto original del
    PDF. Minusculas, sin diacriticos, espacios colapsados, sin espacio adyacente
    a puntuacion, sin espacios en los extremos.

BANDERA DE TRUNCAMIENTO (heuristica, CON FALSOS NEGATIVOS)
    Se marca "truncado": true si (a) la longitud del nombre iguala el ancho de
    corte de su origen -24 en leyenda, 35 en linea de tutor-, o (b) el nombre
    termina en preposicion, articulo o conjuncion suelta, que es el corte por
    palabra del tipo "Proyecto Transversal en".
    Los falsos negativos son inevitables: un nombre cortado justo donde acaba una
    palabra con sentido -"Geografia e Hist" no, pero "Geografia" si- no se
    distingue de un nombre corto legitimo. La bandera avisa, no certifica.

TRANSCRIPCION FIEL
    Las erratas del PDF se conservan tal cual. TEC1 queda como "Jimeez Lopez,
    Juan" (sic) porque asi esta impreso. Este script no corrige la fuente.
"""

import argparse
import json
import re
import subprocess
import sys
import unicodedata
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]

ANCHO_LEYENDA = 24
ANCHO_TUTOR = 35

# Palabras que, sueltas al final, delatan un corte por palabra.
PALABRAS_DE_CORTE = {
    # preposiciones
    "a", "ante", "bajo", "cabe", "con", "contra", "de", "desde", "durante",
    "en", "entre", "hacia", "hasta", "mediante", "para", "por", "segun",
    "sin", "so", "sobre", "tras", "versus", "via",
    # articulos y contracciones
    "el", "la", "los", "las", "un", "una", "unos", "unas", "lo", "al", "del",
    # conjunciones
    "y", "e", "o", "u", "ni", "que", "pero", "sino", "mas", "aunque", "si",
}


def normalizar(texto):
    """Forma de comparacion. Nunca se guarda: solo se compara con ella."""
    t = texto.lower()
    t = "".join(c for c in unicodedata.normalize("NFD", t)
                if unicodedata.category(c) != "Mn")
    t = re.sub(r"\s+", " ", t)
    t = re.sub(r"\s*([^\w\s])\s*", r"\1", t, flags=re.UNICODE)
    return t.strip()


def cuenta_diacriticos(texto):
    return sum(1 for c in unicodedata.normalize("NFD", texto)
               if unicodedata.category(c) == "Mn")


def leer_paginas(pdf):
    """Devuelve la lista de paginas del PDF como texto con la maqueta preservada."""
    salida = subprocess.run(
        ["pdftotext", "-layout", str(pdf), "-"],
        check=True, capture_output=True,
    ).stdout.decode("utf-8")
    return [p for p in salida.split("\f") if p.strip()]


def leer_varios(pdfs):
    """Paginas de todos los PDF juntas. recolectar() reparte por codigo y construir()
    ordena las variantes, asi que el orden de los PDF no cambia el resultado."""
    paginas = []
    for pdf in pdfs:
        paginas.extend(leer_paginas(pdf))
    return paginas


def recolectar(paginas):
    """Recorre las paginas y acumula, por codigo, la lista ordenada de variantes.

    Las variantes se guardan en listas (no en sets) y se ordenan al final, para
    que el resultado no dependa del orden de iteracion de ninguna estructura con
    hash.
    """
    profesores, asignaturas, tutores = {}, {}, []
    for pagina in paginas:
        destino = None
        for linea in pagina.split("\n"):
            desnuda = linea.strip()
            if desnuda.startswith("Tutor:"):
                nombre = desnuda[len("Tutor:"):].strip()
                if nombre:
                    tutores.append(nombre)
                continue
            if desnuda == "Profesores:":
                destino = profesores
                continue
            if desnuda == "Asignaturas:":
                destino = asignaturas
                continue
            if destino is None:
                continue
            if not desnuda:
                destino = None
                continue
            campos = re.split(r"\s{2,}", desnuda)
            if len(campos) % 2 != 0:
                raise SystemExit(
                    "Linea de leyenda con numero impar de campos, la maqueta no "
                    "es la esperada: %r" % linea
                )
            for i in range(0, len(campos), 2):
                destino.setdefault(campos[i], []).append(campos[i + 1])
    return profesores, asignaturas, sorted(set(tutores))


def elegir(variantes):
    """Aplica los cuatro pasos. Devuelve (elegida, hay_conflicto, todas)."""
    todas = sorted(set(variantes))
    normales = {v: normalizar(v) for v in todas}

    # Paso 1: cae toda variante cuya normalizada es prefijo ESTRICTO de otra.
    # Estricto y no simple `startswith` para no aniquilar entre si a dos
    # variantes que normalizan igual: de esas se ocupa el paso 2.
    supervivientes = [
        a for a in todas
        if not any(normales[b] != normales[a] and normales[b].startswith(normales[a])
                   for b in todas)
    ]
    distintas = sorted({normales[v] for v in supervivientes})

    if len(distintas) > 1:
        # Paso 4: discrepancia real de la fuente, no truncamiento.
        return sorted(supervivientes)[0], True, todas

    # Pasos 2 y 3: mas diacriticos, y a igualdad, lexicografico ascendente.
    elegida = sorted(supervivientes, key=lambda v: (-cuenta_diacriticos(v), v))[0]
    return elegida, False, todas


def emparejar_con_tutores(nombre_leyenda, lineas_tutor):
    """Devuelve (linea_ganadora, ambiguo).

    Una linea "Tutor:" casa con el codigo si la normalizada del nombre de leyenda
    es prefijo de la de la linea. Si un mismo nombre de leyenda casa con dos
    lineas distintas, el emparejamiento no es fiable y se descarta.
    """
    n = normalizar(nombre_leyenda)
    if not n:
        return None, False
    casan = sorted({t for t in lineas_tutor if normalizar(t).startswith(n)})
    if not casan:
        return None, False
    if len({normalizar(t) for t in casan}) > 1:
        return None, True
    # Misma persona escrita igual: la mas larga y, a igualdad, la lexicografica.
    return sorted(casan, key=lambda t: (-len(t), t))[0], False


def es_truncado(nombre, ancho_de_corte):
    if len(nombre) == ancho_de_corte:
        return True
    ultima = normalizar(nombre).split(" ")[-1] if nombre.strip() else ""
    return ultima in PALABRAS_DE_CORTE


def construir(codigos_a_variantes, lineas_tutor=None):
    entradas = {}
    for codigo in sorted(codigos_a_variantes):
        elegida, conflicto, todas = elegir(codigos_a_variantes[codigo])
        procedencia = "leyenda"
        ancho = ANCHO_LEYENDA
        ambiguo = False

        if lineas_tutor is not None:
            linea, ambiguo = emparejar_con_tutores(elegida, lineas_tutor)
            if linea is not None and len(linea) > len(elegida):
                elegida = linea
                procedencia = "tutor"
                ancho = ANCHO_TUTOR

        truncado = es_truncado(elegida, ancho)

        # Clausula final: un nombre que no aporta nada sobre el codigo no es nombre.
        if not normalizar(elegida) or normalizar(elegida) == normalizar(codigo):
            elegida = codigo
            procedencia = "codigo"
            truncado = False

        entrada = {
            "nombreCompleto": elegida,
            "procedencia": procedencia,
            "truncado": truncado,
        }
        if conflicto:
            entrada["conflicto"] = True
            entrada["variantes"] = todas
        if ambiguo:
            entrada["tutorAmbiguo"] = True
        entradas[codigo] = entrada
    return entradas


def nombres_de_decisiones(doc):
    """decisiones-catalogo.json -> (asignaturas, profesores), cada uno {codigo: (nombre, id)}."""
    asignaturas, profesores = {}, {}
    for d in doc.get("decisiones", []):
        if d.get("fija") == "asignaturas.nombre":
            pares = d["valor"].items()
            destino = asignaturas
        elif d.get("fija") == "profesores.alta":
            pares = ((c, e.get("nombreCompleto")) for c, e in d["valor"].items())
            destino = profesores
        else:
            continue
        for codigo, nombre in pares:
            if codigo in destino:
                raise SystemExit("El codigo %s tiene nombre en dos decisiones (%s y %s)"
                                 % (codigo, destino[codigo][1], d["id"]))
            if not isinstance(nombre, str) or not nombre.strip():
                raise SystemExit("Decision %s: nombre vacio para %s" % (d["id"], codigo))
            destino[codigo] = (nombre, d["id"])
    return asignaturas, profesores


def anadir_decisiones(entradas, de_decisiones, familia):
    """Entradas del PDF + las de las decisiones, ordenadas por codigo. Un choque es un error."""
    choques = sorted(set(entradas) & set(de_decisiones))
    if choques:
        raise SystemExit("%s: los codigos %s vienen del PDF y de una decision" % (familia, choques))
    todas = dict(entradas)
    for codigo, (nombre, ident) in de_decisiones.items():
        # S204: misma bandera que un nombre de leyenda (los de asignaturas.nombre son literales de la
        # leyenda «Asignaturas:» del PDF de profesores, que corta igual, a ANCHO_LEYENDA).
        todas[codigo] = {"nombreCompleto": nombre, "procedencia": "decision:%s" % ident,
                         "truncado": es_truncado(nombre, ANCHO_LEYENDA)}
    return {c: todas[c] for c in sorted(todas)}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--pdf", nargs="+", required=True, help="uno o varios PDF de horarios de grupos")
    ap.add_argument("--catalogo", required=True, help="catalogo-derivado.json del curso (cierre)")
    ap.add_argument("--salida", required=True, help="nombres-derivados.json que se escribe")
    ap.add_argument("--decisiones", help="decisiones-catalogo.json del curso (S204, opcional)")
    args = ap.parse_args(argv)
    pdfs = [Path(p) for p in args.pdf]
    salida, catalogo_ruta = Path(args.salida), Path(args.catalogo)
    for pdf in pdfs:
        if not pdf.is_file():
            raise SystemExit("No encuentro el PDF de entrada: %s" % pdf)
    if not catalogo_ruta.is_file():
        raise SystemExit("No encuentro el catalogo: %s" % catalogo_ruta)

    paginas = leer_varios(pdfs)
    crudos_prof, crudos_asig, lineas_tutor = recolectar(paginas)

    profesores = construir(crudos_prof, lineas_tutor)
    asignaturas = construir(crudos_asig)
    if args.decisiones:
        dec_asig, dec_prof = nombres_de_decisiones(json.loads(Path(args.decisiones).read_text(encoding="utf-8")))
        asignaturas = anadir_decisiones(asignaturas, dec_asig, "asignaturas")
        profesores = anadir_decisiones(profesores, dec_prof, "profesores")

    truncados = sum(1 for e in list(profesores.values()) + list(asignaturas.values())
                    if e["truncado"])
    conflictos = sum(1 for e in list(profesores.values()) + list(asignaturas.values())
                     if e.get("conflicto"))

    documento = {
        "_meta": {
            "fuente": ", ".join(sorted(p.name for p in pdfs)),
            "generadoPor": "tools/carga-centro/extraer-nombres.py",
            "reglaDeEleccion": "prefijo > diacriticos > lexicografico; conflicto marcado",
            "avisoTruncamiento": "heuristica por ancho de corte y por corte de palabra; tiene falsos negativos",
            "resumen": {
                "profesores": len(profesores),
                "asignaturas": len(asignaturas),
                "truncados": truncados,
                "conflictos": conflictos,
            },
        },
        "profesores": profesores,
        "asignaturas": asignaturas,
    }
    if args.decisiones:
        documento["_meta"]["decisiones"] = Path(args.decisiones).name
        documento["_meta"]["resumen"]["deDecisiones"] = sum(
            1 for e in list(profesores.values()) + list(asignaturas.values()) if e["procedencia"].startswith("decision:"))

    print("PDF leidos:          %d (%s)" % (len(pdfs), ", ".join(p.name for p in pdfs)))
    print("Paginas leidas:      %d" % len(paginas))
    print("Lineas 'Tutor:':     %d" % len(lineas_tutor))
    print("Profesores:          %d" % len(profesores))
    print("Asignaturas:         %d" % len(asignaturas))
    print("Marcados truncados:  %d" % truncados)
    print("Conflictos:          %d" % conflictos)

    # Cierre ANTES de escribir (S198): un desajuste no deja salida a medias.
    catalogo = json.loads(catalogo_ruta.read_text(encoding="utf-8"))
    for familia, entradas in (("profesores", profesores), ("asignaturas", asignaturas)):
        esperados = {x["codigo"] for x in catalogo[familia]}
        if esperados != set(entradas):
            print("DESAJUSTE en %s contra el catalogo derivado:" % familia)
            print("  solo en el PDF: %s" % sorted(set(entradas) - esperados))
            print("  solo en el catalogo: %s" % sorted(esperados - set(entradas)))
            print("No se escribe %s." % salida)
            return 1
    print("Claves identicas a las de %s en ambas familias." % catalogo_ruta.name)

    salida.write_text(
        json.dumps(documento, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print("Escrito: %s" % salida)
    return 0


if __name__ == "__main__":
    sys.exit(main())
