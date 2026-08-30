#!/usr/bin/env python3
"""Cuenta, POR CÁLCULO, las celdas que el diseño de S122 recortaría en todo el centro.

Nace en S122 y se conserva por dos razones: quien implemente D11 (docs/diseno-navegacion.md)
tendrá que volver a correrlo en cuanto cambie la geometría de la celda, y porque es el
EJEMPLAR DE REFERENCIA de la precisión de M2 sobre verificación de instrumentos
(docs/metodo.md): sus cinco mutaciones viven en `autoprueba()`, se ejecutan SIEMPRE antes
de dar ningún número, y abortan si alguna no salta.

No mira el DOM. El contador de DOM de la maqueta devolvió 0 sobre un caso que sí
desbordaba (§6 del documento), porque medía el <td>, que crece con su contenido.
Aquí la altura se DERIVA de las reglas CSS decididas y de los datos reales.

Determinismo: todas las alturas salen de `line-height` NUMÉRICO por `font-size`
(un múltiplo exacto, independiente de la fuente) y de paddings en `rem` con raíz
de 16px. No hay wrap: `.ln` y `.entrada--fila` llevan `white-space: nowrap`.
La ÚNICA magnitud no calculable es el cromo de la vista (barra + cabecera), que
depende de la altura que el navegador dé a `<select>` y `<button>` nativos: entra
como PARÁMETRO y se barre en un rango.
"""
import argparse
import json
import sys

REM = 16.0
TAM_S, TAM_XS = 0.875 * REM, 0.75 * REM      # 14.0, 12.0
LH_APRETADO = 1.25

# --- Alturas de las piezas, derivadas del CSS de la propuesta (v8) ---
PAD_ENTRADA = 0.2 * REM * 2                  # .v2 .entrada  padding 0.2rem
LN1 = TAM_S * LH_APRETADO                    # .ln--1: la manda .asig (--tam-s)
LN2 = TAM_XS * LH_APRETADO                   # .ln--2: --tam-xs
ALTO_SUELTA = PAD_ENTRADA + LN1 + LN2        # instancia de UNA plaza: dos líneas

PAD_FILA = 0.1 * REM * 2                     # .entrada--fila padding 0.1rem
MARGEN_FILA = 1.0                            # .entrada--fila margin-bottom 1px
ALTO_FILA_PLAZA = PAD_FILA + LN1 + MARGEN_FILA   # una plaza de bloque: una línea

BANDA_BADGE = 16.0                           # .instancia.con-badge padding-top
MARGEN_INSTANCIA = 0.2 * REM                 # .instancia margin-bottom
PAD_CELDA = 0.25 * REM * 2                   # .celda padding 0.25rem

ALTO_THEAD = 0.25 * REM * 2 + TAM_S * 1.5 + 2    # th + line-height normal + bordes
ALTO_RECREO = 1.35 * REM + 2                     # tr.recreo height + bordes
TRAMOS = 6                                       # defecto de --tramos
ALTO_DEFECTO = 1080                              # defecto de --alto (pantalla fisica)
CROMOS_DEFECTO = (100, 120, 140, 160, 180, 200, 220)   # defecto de --cromos


def alto_instancia(n_entradas):
    """Alto de UNA instancia con n plazas, incluida la banda del badge."""
    if n_entradas == 1:
        return BANDA_BADGE + ALTO_SUELTA
    return BANDA_BADGE + n_entradas * ALTO_FILA_PLAZA


def alto_celda(instancias):
    """instancias: lista con el nº de plazas de cada instancia de la celda."""
    if not instancias:
        return 0.0
    alto = PAD_CELDA + sum(alto_instancia(n) for n in instancias)
    return alto + MARGEN_INSTANCIA * (len(instancias) - 1)


def celdas_de(sesiones):
    """(dia,tramo) -> lista de tamaños de instancia, como agruparPorActividad."""
    slots = {}
    for s in sesiones:
        slots.setdefault((s["dia"], s["tramo"]), {}) \
             .setdefault((s["actividadCodigo"], s["indice"]), 0)
        slots[(s["dia"], s["tramo"])][(s["actividadCodigo"], s["indice"])] += 1
    return {k: sorted(v.values(), reverse=True) for k, v in slots.items()}


def recortes(grupos, alto_fila):
    """Celdas cuyo contenido no cabe en `alto_fila`, en todo el centro."""
    total = malas = 0
    peor = None
    for g, sesiones in grupos.items():
        for slot, inst in celdas_de(sesiones).items():
            total += 1
            req = alto_celda(inst)
            if req > alto_fila + 0.01:
                malas += 1
                if peor is None or req > peor[0]:
                    peor = (req, g, slot, inst)
    return malas, total, peor


def plazas_de_celda(inst):
    """Tamaño con que se nombra una celda: el de su instancia mayor."""
    return max(inst) if inst else 0


def desglose(grupos, alto_fila):
    """{n_plazas: [recortadas, totales]} en todo el centro, a ese alto de fila."""
    d = {}
    for sesiones in grupos.values():
        for inst in celdas_de(sesiones).values():
            n = plazas_de_celda(inst)
            fila = d.setdefault(n, [0, 0])
            fila[1] += 1
            if alto_celda(inst) > alto_fila + 0.01:
                fila[0] += 1
    return d


def texto_desglose(d):
    """'6:22/22 5:0/57 ...' sólo con los tamaños que tienen alguna recortada."""
    trozos = ["%dp:%d/%d" % (n, d[n][0], d[n][1]) for n in sorted(d, reverse=True) if d[n][0]]
    return " ".join(trozos) if trozos else "-"


# ---------------------------------------------------------------------------
# PRUEBA DE MUTACIÓN DEL INSTRUMENTO. Antes de creerse un número, se le fuerzan
# casos que DEBEN hacerlo saltar. Un contador que sólo sabe decir "todo bien" no
# es un contador: es lo que falló en la maqueta.
# ---------------------------------------------------------------------------
def autoprueba(grupos):
    fallos = []
    n_celdas = sum(len(celdas_de(s)) for s in grupos.values())

    # M1: alto de fila ridículo -> TODAS deben salir recortadas.
    m, t, _ = recortes(grupos, 1)
    if m != t or t == 0:
        fallos.append("M1 con alto=1px: %d de %d recortadas, se esperaban todas" % (m, t))

    # M2: alto de fila enorme -> NINGUNA.
    m, t, _ = recortes(grupos, 10000)
    if m != 0:
        fallos.append("M2 con alto=10000px: %d recortadas, se esperaban 0" % m)

    # M3: una celda sintética de DOCE plazas debe salir recortada a 154px, que es
    #     el alto que basta para la de seis. Si no salta, el instrumento es ciego
    #     al caso que existe para cazar.
    falso = {"XX": [{"dia": 1, "tramo": 1, "actividadCodigo": "BLOQUE-FALSO",
                     "indice": 1, "asignaturaCodigo": "X", "profesores": [],
                     "aulaCodigo": "A", "grupos": ["XX"], "subgrupos": []}
                    for _ in range(12)]}
    m, t, _ = recortes(falso, 154)
    if m != 1:
        fallos.append("M3 celda sintética de 12: %d recortadas de %d, se esperaba 1" % (m, t))

    # M4: monotonía. Bajar el alto de fila NUNCA puede reducir los recortes.
    previo = -1
    for a in range(60, 200, 10):
        m, _, _ = recortes(grupos, a)
        if previo != -1 and m > previo:
            fallos.append("M4: recortes suben al subir el alto (%dpx)" % a)
        previo = m

    # M5: el alto calculado de la celda de 6 debe superar al de la de 5.
    if not alto_celda([6]) > alto_celda([5]) > alto_celda([1]):
        fallos.append("M5: el alto no crece con el número de plazas")

    print("--- AUTOPRUEBA DEL INSTRUMENTO (%d celdas reales) ---" % n_celdas)
    if fallos:
        for f in fallos:
            print("  FALLO: %s" % f)
        raise SystemExit("ABORTA: el instrumento no pasa su propia prueba; sus números no valen")
    print("  M1 alto=1px -> todas recortadas ......... salta")
    print("  M2 alto=10000px -> ninguna .............. salta")
    print("  M3 celda sintética de 12 plazas ......... salta")
    print("  M4 monotonía al bajar el alto ........... salta")
    print("  M5 alto(6) > alto(5) > alto(1) .......... salta")
    print()


def parsear(argv):
    ap = argparse.ArgumentParser(
        description="Cuenta por cálculo las celdas que el diseño de D11 recortaría.",
        epilog="Sin banderas reproduce la pasada original de S122 (alto 1080, "
               "cromos 100..220, 6 tramos).")
    ap.add_argument("ruta", nargs="?", default="/tmp/datos-maqueta.json",
                    help="volcado JSON de sesiones por grupo (defecto: %(default)s)")
    ap.add_argument("--alto", type=float, default=ALTO_DEFECTO,
                    help="altura de la que se resta el cromo, en px. OJO: es alto de "
                         "PANTALLA si los cromos incluyen el del navegador, y alto de "
                         "VIEWPORT CSS si no (defecto: %(default)s)")
    ap.add_argument("--cromos", default=",".join(str(c) for c in CROMOS_DEFECTO),
                    help="lista separada por comas del cromo a barrer, en px "
                         "(defecto: %(default)s)")
    ap.add_argument("--tramos", type=int, default=TRAMOS,
                    help="filas lectivas de la tabla (defecto: %(default)s)")
    a = ap.parse_args(argv)
    try:
        a.cromos = [float(c) for c in a.cromos.split(",") if c.strip() != ""]
    except ValueError:
        ap.error("--cromos: se esperaba una lista de números separada por comas")
    if not a.cromos:
        ap.error("--cromos: lista vacía")
    if a.tramos < 1:
        ap.error("--tramos: debe ser >= 1")
    return a


def alto_fila_de(alto, cromo, tramos):
    """El presupuesto por fila: se descuentan cabecera, recreo y bordes."""
    return (alto - cromo - ALTO_THEAD - ALTO_RECREO - 4) / tramos


def main(argv=None):
    a = parsear(sys.argv[1:] if argv is None else argv)
    try:
        datos = json.load(open(a.ruta, encoding="utf-8"))
    except FileNotFoundError:
        raise SystemExit(
            "No existe %s.\n"
            "Este guion necesita el volcado de sesiones por grupo que produjo S122 desde\n"
            "app/educhronos-demo-m4.db en SOLO LECTURA. Formato: {\"grupos\": {codigo: [sesion, ...]}},\n"
            "donde cada sesion lleva dia, tramo, actividadCodigo e indice.\n"
            "Uso: %s [ruta-del-json]" % (a.ruta, sys.argv[0]))
    grupos = datos["grupos"]
    autoprueba(grupos)

    print("--- ALTO REQUERIDO POR CELDA, SEGÚN SU CONTENIDO ---")
    for n in (1, 2, 3, 4, 5, 6):
        print("  instancia de %d plaza(s): celda necesita %6.1f px" % (n, alto_celda([n])))
    print()

    print("--- PEOR CELDA DE CADA GRUPO (%d grupos) ---" % len(grupos))
    peores = []
    for g, s in grupos.items():
        c = celdas_de(s)
        if not c:
            continue
        req, slot, inst = max(((alto_celda(i), k, i) for k, i in c.items()))
        peores.append((req, g, slot, inst))
    peores.sort(reverse=True)
    for req, g, slot, inst in peores[:8]:
        print("  %-8s dia %d T%d  instancias %s  -> %6.1f px" % (g, slot[0], slot[1], inst, req))
    print("  … (%d grupos más, todos por debajo)" % max(0, len(peores) - 8))
    print()

    necesario = peores[0][0]
    print("ALTO DE FILA NECESARIO PARA CERO RECORTES EN TODO EL CENTRO: %.1f px" % necesario)
    alto_tabla = ALTO_THEAD + ALTO_RECREO + a.tramos * necesario
    print("  -> tabla completa: %.1f + %.1f + %d x %.1f = %.1f px"
          % (ALTO_THEAD, ALTO_RECREO, a.tramos, necesario, alto_tabla))
    print()

    print("--- ¿CABE EN %.0f px de alto? El cromo es el parámetro ---" % a.alto)
    print("  util = %.0f - cromo; presupuesto de tabla = util - cabecera - recreo - bordes" % a.alto)
    print()
    print("  %-12s %-10s %-12s %-18s %s"
          % ("cromo", "util", "alto/fila", "celdas recortadas", "desglose por plazas"))
    for cromo in a.cromos:
        af = alto_fila_de(a.alto, cromo, a.tramos)
        m, t, _ = recortes(grupos, af)
        marca = "  <-- CERO" if m == 0 else ""
        print("  %-12g %-10.0f %-12.2f %-18s %s%s"
              % (cromo, a.alto - cromo, af, "%d de %d" % (m, t),
                 texto_desglose(desglose(grupos, af)), marca))
    print()
    cromo_max = a.alto - (ALTO_THEAD + ALTO_RECREO + 4 + a.tramos * necesario)
    print("CROMO MÁXIMO ADMISIBLE para cero recortes: %.1f px" % cromo_max)
    print("  (barra superior + padding + cabecera de vista + cromo del navegador si --alto es la pantalla)")


if __name__ == "__main__":
    sys.exit(main())
