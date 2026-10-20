#!/usr/bin/env python3
"""Prepara la base de partida que viaja con Educhronos (C-base-partida, S241).

Toma una base de SQLite de Educhronos cuya tabla `curso` está vacía (una base anterior al
nombre de curso, como la de 2025/2026 del centro) y produce una copia con la fila única de
`curso` escrita: id 1, el nombre dado y archivado 0, igual que la escribe la aplicación
(DuplicadorCurso.escribirCurso). No migra el esquema: lo hace la aplicación al abrir la base
(PreparadorEsquema), y ninguna migración toca `curso`. No renombra: si la base ya tiene fila,
se rechaza (D-curso-sin-renombrar sigue viva, decisión CJ de S240).

Uso:
  python3 -B preparar_base_partida.py --origen A.db --destino B.db --nombre 2025/2026 \
      [--huella-origen ba0db782]
Salida: 0 hecho; 2 rechazo previsto, sin escribir nada; 1 fallo inesperado.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import sqlite3
from pathlib import Path

DDL_CURSO = (
    "CREATE TABLE curso (id integer not null check (id = 1), nombre varchar(9) not null,"
    " archivado boolean not null, primary key (id))"
)
ID_CURSO = 1
FORMA = re.compile(r"^(\d{4})/(\d{4})$")
LONGITUD_HUELLA_MINIMA = 8


class Rechazo(Exception):
    """Rechazo previsto: no se ha escrito nada."""


def nombre_valido(nombre):
    """Espejo de NombreCurso.valido: forma 2026/2027 y segundo año = primero + 1."""
    if nombre is None:
        return False
    m = FORMA.match(nombre)
    if not m:
        return False
    primero, segundo = int(m.group(1)), int(m.group(2))
    return segundo == primero + 1


def md5(ruta):
    h = hashlib.md5()
    with open(ruta, "rb") as f:
        for trozo in iter(lambda: f.read(1 << 20), b""):
            h.update(trozo)
    return h.hexdigest()


def uri_solo_lectura(ruta):
    return Path(ruta).resolve().as_uri() + "?mode=ro"


def abrir_solo_lectura(ruta):
    return sqlite3.connect(uri_solo_lectura(ruta), uri=True)


def tablas(con):
    return [
        n
        for (n,) in con.execute(
            "select name from sqlite_master where type = 'table'"
            " and name not like 'sqlite_%' order by name"
        )
    ]


def version(con):
    return con.execute("PRAGMA user_version").fetchone()[0]


def leer_curso(con):
    """Las filas de curso. Exige la tabla con la DDL literal de schema.sql."""
    ddl = con.execute(
        "select sql from sqlite_master where type = 'table' and name = 'curso'"
    ).fetchone()
    if ddl is None:
        raise Rechazo("la base no tiene tabla curso: no es una base de Educhronos con curso")
    if ddl[0] != DDL_CURSO:
        raise Rechazo("la tabla curso no tiene la definición esperada: " + ddl[0])
    return con.execute("select id, nombre, archivado from curso order by id").fetchall()


def verificar(copia, origen, nombre, version_origen, tablas_origen):
    con = abrir_solo_lectura(copia)
    try:
        filas = leer_curso(con)
        if filas != [(ID_CURSO, nombre, 0)]:
            raise RuntimeError("la copia no tiene la fila esperada: " + repr(filas))
        if version(con) != version_origen:
            raise RuntimeError("la copia cambia user_version")
        if tablas(con) != tablas_origen:
            raise RuntimeError("la copia no tiene las mismas tablas que el origen")
        con.execute("attach database ? as origen", (uri_solo_lectura(origen),))
        for tabla in tablas_origen:
            if tabla == "curso":
                continue
            sobran = con.execute(
                f"select count(*) from (select * from main.{tabla}"
                f" except select * from origen.{tabla})"
            ).fetchone()[0]
            faltan = con.execute(
                f"select count(*) from (select * from origen.{tabla}"
                f" except select * from main.{tabla})"
            ).fetchone()[0]
            n_copia = con.execute(f"select count(*) from main.{tabla}").fetchone()[0]
            n_origen = con.execute(f"select count(*) from origen.{tabla}").fetchone()[0]
            if sobran or faltan or n_copia != n_origen:
                raise RuntimeError(
                    f"la tabla {tabla} difiere del origen: sobran {sobran}, faltan {faltan},"
                    f" filas {n_copia}/{n_origen}"
                )
    finally:
        con.close()


def preparar(origen, destino, nombre, huella_origen=None):
    """Produce `destino` a partir de `origen`. Devuelve las huellas y el user_version."""
    if not nombre_valido(nombre):
        raise Rechazo(
            f"«{nombre}» no es un nombre de curso: forma 2025/2026, con dos años consecutivos"
        )
    if not os.path.isfile(origen):
        raise Rechazo("no existe el origen: " + str(origen))
    if os.path.exists(destino):
        raise Rechazo("el destino ya existe y no se toca: " + str(destino))
    huella = md5(origen)
    if huella_origen is not None:
        esperada = huella_origen.strip().lower()
        if len(esperada) < LONGITUD_HUELLA_MINIMA or not huella.startswith(esperada):
            raise Rechazo(f"la huella del origen ({huella}) no empieza por la esperada ({esperada})")
    con = abrir_solo_lectura(origen)
    try:
        filas_curso = leer_curso(con)
        version_origen = version(con)
        tablas_origen = tablas(con)
    finally:
        con.close()
    if len(filas_curso) != 0:
        raise Rechazo("la base ya tiene fila de curso " + repr(filas_curso) + ": este script no renombra")

    carpeta = os.path.dirname(os.path.abspath(destino))
    os.makedirs(carpeta, exist_ok=True)
    temporal = os.path.join(carpeta, "." + os.path.basename(destino) + ".tmp")
    try:
        if os.path.exists(temporal):
            os.remove(temporal)
        shutil.copyfile(origen, temporal)
        con = sqlite3.connect(temporal)
        try:
            con.execute(
                "insert into curso (id, nombre, archivado) values (?, ?, 0)", (ID_CURSO, nombre)
            )
            con.commit()
        finally:
            con.close()
        verificar(temporal, origen, nombre, version_origen, tablas_origen)
        if md5(origen) != huella:
            raise RuntimeError("el origen ha cambiado durante la preparación")
        os.replace(temporal, destino)
    except BaseException:
        if os.path.exists(temporal):
            os.remove(temporal)
        raise
    return {
        "md5_origen": huella,
        "md5_destino": md5(destino),
        "user_version": version_origen,
        "tablas": len(tablas_origen),
    }


def main(argv=None):
    analizador = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    analizador.add_argument("--origen", required=True)
    analizador.add_argument("--destino", required=True)
    analizador.add_argument("--nombre", required=True)
    analizador.add_argument("--huella-origen", default=None)
    args = analizador.parse_args(argv)
    try:
        r = preparar(args.origen, args.destino, args.nombre, args.huella_origen)
    except Rechazo as e:
        print("RECHAZADO:", e)
        return 2
    except Exception as e:  # noqa: BLE001
        print("FALLO:", repr(e))
        return 1
    print("origen  :", args.origen)
    print("md5     :", r["md5_origen"])
    print("destino :", args.destino)
    print("md5     :", r["md5_destino"])
    print(f"curso   : {args.nombre}, archivado 0, user_version {r['user_version']}, {r['tablas']} tablas")
    print("HECHO")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
