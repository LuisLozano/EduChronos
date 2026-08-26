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
TRAMOS = 6


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


def main():
    ruta = sys.argv[1] if len(sys.argv) > 1 else "/tmp/datos-maqueta.json"
    try:
        datos = json.load(open(ruta, encoding="utf-8"))
    except FileNotFoundError:
        raise SystemExit(
            "No existe %s.\n"
            "Este guion necesita el volcado de sesiones por grupo que produjo S122 desde\n"
            "app/educhronos-demo-m4.db en SOLO LECTURA. Formato: {\"grupos\": {codigo: [sesion, ...]}},\n"
            "donde cada sesion lleva dia, tramo, actividadCodigo e indice.\n"
            "Uso: %s [ruta-del-json]" % (ruta, sys.argv[0]))
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
    alto_tabla = ALTO_THEAD + ALTO_RECREO + TRAMOS * necesario
    print("  -> tabla completa: %.1f + %.1f + 6 x %.1f = %.1f px"
          % (ALTO_THEAD, ALTO_RECREO, necesario, alto_tabla))
    print()

    print("--- ¿CABE EN 1920x1080? El cromo de la vista es el parámetro ---")
    print("  util = 1080 - cromo_navegador; presupuesto de tabla = util - cromo_vista")
    print()
    print("  %-14s %-10s %-12s %s" % ("cromo total", "util tabla", "alto/fila", "celdas recortadas"))
    for cromo in (100, 120, 140, 160, 180, 200, 220):
        util = 1080 - cromo
        disponible = util - ALTO_THEAD - ALTO_RECREO - 4
        af = disponible / TRAMOS
        m, t, _ = recortes(grupos, af)
        marca = "  <-- CERO" if m == 0 else ""
        print("  %-14s %-10.0f %-12.1f %d de %d%s" % (cromo, util, af, m, t, marca))
    print()
    cromo_max = 1080 - (ALTO_THEAD + ALTO_RECREO + 4 + TRAMOS * necesario)
    print("CROMO MÁXIMO ADMISIBLE para cero recortes: %.1f px" % cromo_max)
    print("  (barra superior + padding + cabecera de vista + cromo del navegador)")


if __name__ == "__main__":
    sys.exit(main())
