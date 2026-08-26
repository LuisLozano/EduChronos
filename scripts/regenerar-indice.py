#!/usr/bin/env python3
"""Regenera el índice de M-doc-3 en los documentos grandes. IDEMPOTENTE.

`gestion_proyecto.md` y `plan_trabajo_horarios.md` llevan al principio un índice
GENERADO entre `<!-- INDICE:INICIO -->` y `<!-- INDICE:FIN -->` con cada encabezado
y su línea. Los números caducan en cuanto alguien edita, así que esto se corre en
cada cierre (metodo.md, §Automatización del cierre).

Nace en S122 al separarlo de `s122-doc-normas-e-indice.sh`, que era un guion de UN
SOLO USO: contenía además la inserción de las normas M-doc y reejecutarlo las
DUPLICABA. Ocurrió en el propio cierre de S122. La parte que hay que repetir en cada
cierre vive aquí, sola y sin efectos colaterales.
"""
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = [os.path.join(RAIZ, "docs", n) for n in ("gestion_proyecto.md", "plan_trabajo_horarios.md")]
INI, FIN = "<!-- INDICE:INICIO -->", "<!-- INDICE:FIN -->"
CABECERA = re.compile(r"^#{2,4} ")


def indexar(ruta):
    with open(ruta, encoding="utf-8") as f:
        t = f.read()
    # Quitar el índice previo. El `\n\n` final es EXACTO y no `\n*`: con `\n*` se
    # comía además la línea en blanco del cuerpo y cada regeneración movía el texto.
    t = re.sub(re.escape(INI) + r".*?" + re.escape(FIN) + r"\n\n", "", t, flags=re.S)
    lineas = t.split("\n")
    cabeceras = [(i + 1, l) for i, l in enumerate(lineas) if CABECERA.match(l)]
    if not cabeceras:
        raise SystemExit("ABORTA: sin encabezados en %s" % ruta)

    def bloque(nums):
        c = [INI, "<!-- Generado en M1 (M-doc-3). Líneas INDICATIVAS; manda el texto. -->", ""]
        c += ["- L%d — %s" % (n, txt) for n, txt in nums]
        c += ["", FIN, ""]
        return c

    # El desplazamiento no depende de los dígitos: la longitud del bloque es fija.
    desplaz = len(bloque([(0, txt) for _, txt in cabeceras]))
    cuerpo = bloque([(n + desplaz, txt) for n, txt in cabeceras])
    nuevas = [lineas[0]] + cuerpo + lineas[1:]
    with open(ruta, "w", encoding="utf-8") as f:
        f.write("\n".join(nuevas))
    return len(cabeceras)


def comprobar(ruta):
    """Cada entrada debe apuntar de verdad a su encabezado."""
    lineas = open(ruta, encoding="utf-8").read().split("\n")
    malas = 0
    total = 0
    for l in lineas:
        m = re.match(r"^- L(\d+) — (.*)$", l)
        if not m:
            continue
        total += 1
        n, txt = int(m.group(1)), m.group(2)
        if not (1 <= n <= len(lineas)) or lineas[n - 1] != txt:
            malas += 1
    return total, malas


def main():
    fallo = 0
    for r in DOCS:
        n = indexar(r)
        total, malas = comprobar(r)
        print("%-28s %d encabezados, %d entradas, %d descuadradas %s"
              % (os.path.basename(r), n, total, malas, "" if malas == 0 else "<-- FALLO"))
        fallo += malas
        # Idempotencia: regenerar sobre lo ya generado no debe cambiar nada.
        antes = open(r, encoding="utf-8").read()
        indexar(r)
        if open(r, encoding="utf-8").read() != antes:
            print("   FALLO: no es idempotente")
            fallo += 1
    return 1 if fallo else 0


if __name__ == "__main__":
    sys.exit(main())
