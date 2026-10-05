#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Forma corta de un título de grupo («1º ESO A» -> «1ºA»), la que usan las vistas de aulas y de
profesores del programa de horarios del centro.

Nace en S197 (C-volcado-profesores) para el cruce grupos <-> profesores. Las reglas se aplican
SOLO al cruzar; los volcados conservan el código crudo.

ORIGEN DE LAS REGLAS
  - Las cinco primeras vienen de REGLAS_CODIGO de tools/carga-centro/verificar-conservacion.py
    (líneas 70-76 en ab5118f), que las sacó de la tabla de
    docs/horario-referencia/INFORME-RECONCILIACION.md más el caso «3º ESO PDC» cerrado en S115.
    Desde S199 verificar-conservacion.py ya no las define: las importa de aquí (forma_corta), y
    este módulo es su única sede. Se aplican con `patron.match` y luego
    `patron.sub(plantilla, crudo)`.
  - La sexta, «1ºBACH <letra> <Modalidad>» -> «1B-<letra><inicial de la modalidad en
    minúscula>», sale de S197: los seis títulos de 1ºBACH de 2026/2027 no casan ninguna de las
    cinco y la vista de profesores (P07) los imprime como 1B-Ac, 1B-Am, ... La correspondencia
    se midió por datos (Jaccard 1,000 de cada token con su título sobre (profesor, día, tramo)
    y el segundo en 0,683 como mucho) en m2bis/q4-bach.tsv de S197.

Un título sin regla, o que case más de una, es un error: no se adivina.
"""
import re

# verificar-conservacion.py importa estas reglas desde S199; este módulo es su única sede.
REGLAS_CODIGO = [
    ("Nº ESO L PDC -> NºLDi", re.compile(r"^(\d)º ESO ([A-D]) PDC$"), r"\1º\2Di"),
    ("Nº ESO PDC   -> 3ºCDi", re.compile(r"^3º ESO PDC$"),            "3ºCDi"),
    ("Nº ESO L     -> NºL",   re.compile(r"^(\d)º ESO ([A-D])$"),     r"\1º\2"),
    ("NºBACH L     -> NB-L",  re.compile(r"^(\d)ºBACH ([A-D])$"),     r"\1B-\2"),
    ("Nº FPB       -> NFPB",  re.compile(r"^(\d)º FPB$"),             r"\1FPB"),
]

# S197: 1ºBACH con modalidad (m2bis/q4-bach.tsv).
REGLA_BACH_MODALIDAD = (
    "1ºBACH L Modalidad -> 1B-Lm",
    re.compile(r"^1ºBACH ([A-D]) ([A-ZÁÉÍÓÚ]\w*)$"),
    lambda m: "1B-%s%s" % (m.group(1), m.group(2)[0].lower()),
)

REGLAS = REGLAS_CODIGO + [REGLA_BACH_MODALIDAD]


class SinFormaCorta(ValueError):
    """El título no casa ninguna regla, o casa más de una."""


def forma_corta(titulo):
    casan = [(nombre, patron, plantilla) for nombre, patron, plantilla in REGLAS if patron.match(titulo)]
    if len(casan) != 1:
        raise SinFormaCorta("%r casa %d reglas: %s" % (titulo, len(casan), [n for n, _, _ in casan]))
    _, patron, plantilla = casan[0]
    return patron.sub(plantilla, titulo)
