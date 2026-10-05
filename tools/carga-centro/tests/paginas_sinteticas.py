# -*- coding: utf-8 -*-
"""Páginas fabricadas de la vista de profesores para los tests de S197.

La geometría imita la de P07 (m2/p3-paginas.tsv de S197): cabeceras de día en las mismas x,
etiquetas de hora por pares en el margen izquierdo, celda con asignatura a x relativa ~5,8 y
aula o grupo a ~40,3, leyenda «Asignaturas:» en dos parejas código + nombre. Las x de las
celdas se dan RELATIVAS al borde izquierdo de la columna, calculado aquí a mano (puntos medios
entre centros de cabecera), no con el código probado.
"""
import importlib.util
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[3]


def cargar(nombre, fichero):
    spec = importlib.util.spec_from_file_location(nombre, RAIZ / "tools" / "carga-centro" / fichero)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


CABECERAS = [("Lunes", 122.2, 146.2), ("Martes", 188.8, 217.6), ("Miérc.", 258.0, 286.8),
             ("Jueves", 327.0, 355.8), ("Viernes", 393.6, 427.2)]
_CENTROS = [(a + b) / 2 for _, a, b in CABECERAS]
BORDES = [_CENTROS[0] - (_CENTROS[1] - _CENTROS[0]) / 2] + [(_CENTROS[i] + _CENTROS[i + 1]) / 2 for i in range(4)]
# (inicio, fin, tramo); None es el recreo
FILAS = [("8:00", "9:00", 1), ("9:00", "10:00", 2), ("10:00", "11:00", 3), ("11:00", "11:30", None),
         ("11:30", "12:30", 4), ("12:30", "13:30", 5), ("13:30", "14:30", 6)]
TOP0, PASO_FILA, LINEA = 90.0, 40.0, 8.5
IZQ, DER = 5.8, 40.3


def top_fila(tramo):
    return TOP0 + PASO_FILA * [f[2] for f in FILAS].index(tramo)


class Pagina:
    def __init__(self, n):
        self.n = n
        self.palabras = []

    def palabra(self, texto, x0, top, x1=None):
        w = {"id": (self.n, len(self.palabras)), "text": texto, "x0": x0, "top": top,
             "x1": x0 + 5.0 * len(texto) if x1 is None else x1, "bottom": top + 8.0}
        self.palabras.append(w)
        return w


def pagina(n, titulo, celdas, leyenda):
    """celdas: {(dia, tramo|None): [[(texto, x relativa), ...] por línea]};
    leyenda: [(código, nombre)] o None para una página sin «Asignaturas:»."""
    p = Pagina(n)
    p.palabra("Profesor:", 59.5, 29.66)
    x = 106.0
    for t in titulo.split():
        p.palabra(t, x, 29.66)
        x += 5.0 * len(t) + 5.0
    for texto, a, b in CABECERAS:
        p.palabra(texto, a, 73.03, b)
    for ini, fin, tramo in FILAS:
        t = top_fila(tramo)
        p.palabra(ini, 64.3, t)
        p.palabra(fin, 64.3, t + LINEA)
    for (dia, tramo), lins in celdas.items():
        for k, lin in enumerate(lins):
            for texto, xr in lin:
                p.palabra(texto, BORDES[dia - 1] + xr, top_fila(tramo) + LINEA * k)
    if leyenda is not None:
        p.palabra("Asignaturas:", 59.5, 400.0)
        for i in range(0, len(leyenda), 2):
            top = 410.0 + LINEA * (i // 2)
            for j, (cod, nom) in enumerate(leyenda[i:i + 2]):
                p.palabra(cod, (70.9, 260.8)[j], top)
                p.palabra(nom, (111.1, 301.0)[j], top)
    return p.palabras


LEYENDA = [("Mat", "Matemáticas"), ("Ing", "Inglés"), ("G", "Guardia"), ("GR", "Guardia Recreo")]
