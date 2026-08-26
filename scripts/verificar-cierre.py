#!/usr/bin/env python3
"""Verificación mecánica del paso M1.6 (metodo.md, §Automatización del cierre).

REPORTA, no corrige. El arquitecto lee la salida y decide.

Comprueba:
  1. Invariante H3: `plan_trabajo_horarios.md` tiene exactamente UNA cabecera
     `### Sesión`.
  2. Censo de tokens R4: cada `D-*`, `Cxx`, `§x.y` del corpus vivo debe tener
     definición viva Y citante vivo. Aproximación declarada: un token que aparece
     UNA SOLA VEZ en todo el corpus vivo es sospechoso —o está definido y nadie lo
     cita, o lo cita alguien y no está definido—. El script no distingue cuál de
     las dos; señala el token y el humano mira.
  3. Coherencia de los DOS censos de la bitácora entre sí y con la crónica de
     archivado del plan.
  4. Índices de M-doc-3 presentes y con sus líneas cuadrando.

El corpus VIVO son los tres documentos de estado: plan, gestión y método.
`bitacora-sesiones.md` es histórico de solo lectura (R4/R5) y NO cuenta como
citante vivo: ésa es justo la trampa que R4 existe para evitar.

Se somete a AUTOPRUEBA antes de reportar nada (precisión de M2 sobre verificación
de instrumentos): se le inyectan defectos que DEBE detectar, y aborta si no los ve.
"""
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAN = os.path.join(RAIZ, "docs", "plan_trabajo_horarios.md")
GESTION = os.path.join(RAIZ, "docs", "gestion_proyecto.md")
METODO = os.path.join(RAIZ, "docs", "metodo.md")
BITACORA = os.path.join(RAIZ, "docs", "bitacora-sesiones.md")
VIVOS = [PLAN, GESTION, METODO]

TOKEN = re.compile(r"\bD-[A-Za-zÀ-ÿ0-9][A-Za-zÀ-ÿ0-9._-]*[A-Za-zÀ-ÿ0-9]|\bD\d{1,2}\b|\bC-[a-zà-ÿ][A-Za-zÀ-ÿ0-9._-]*[A-Za-zÀ-ÿ0-9]")


def leer(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def h3(texto):
    return len(re.findall(r"^### Sesión", texto, re.M))


def tokens_de(texto):
    """Cuenta apariciones de cada token en un texto."""
    cuenta = {}
    for m in TOKEN.finditer(texto):
        t = m.group(0).rstrip(".,;:)")
        cuenta[t] = cuenta.get(t, 0) + 1
    return cuenta


def censo_r4(textos_vivos):
    """Tokens con UNA sola aparición en todo el corpus vivo."""
    total = {}
    for txt in textos_vivos:
        for t, n in tokens_de(txt).items():
            total[t] = total.get(t, 0) + n
    return sorted(t for t, n in total.items() if n == 1), len(total)


def censos_bitacora(texto):
    """Los dos censos de la cabecera: 'S10–SNNN' y 'S10 → SNNN'."""
    a = re.search(r"sesiones de trabajo S10[–-]S(\d+)", texto)
    b = re.search(r"cronológico ascendente \(S10 → S(\d+)\)", texto)
    return (int(a.group(1)) if a else None, int(b.group(1)) if b else None)


def ultima_archivada(texto_plan):
    """La última sesión que la crónica del plan dice haber archivado."""
    # \s+ y no " ": la crónica es un párrafo justificado y parte las entradas
    # por la mitad («la de S120 en la\nSesión 122»). Con un espacio literal se
    # pierden justo las más recientes, que son las que importan.
    ms = re.findall(r"la de S(\d+) en la\s+Sesión\s+(\d+)", texto_plan)
    return max((int(s) for s, _ in ms), default=None)


def indices_ok(ruta):
    t = leer(ruta)
    if "<!-- INDICE:INICIO -->" not in t:
        return "SIN ÍNDICE"
    lineas = t.split("\n")
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
    return "%d entradas, %d descuadradas" % (total, malas)


# ---------------------------------------------------------------------------
# AUTOPRUEBA. Un verificador que sólo sabe decir "todo bien" no es un
# verificador (precisión de M2). Se le inyectan defectos que DEBE ver.
# ---------------------------------------------------------------------------
def autoprueba():
    fallos = []
    inyectados = 7

    if h3("### Sesión 1 — x\n### Sesión 2 — y\n") != 2:
        fallos.append("A1: no cuenta bien las cabeceras H3")
    if h3("texto sin cabeceras") != 0:
        fallos.append("A1b: inventa cabeceras donde no las hay")

    # Un token citado una sola vez DEBE salir como huérfano; uno citado dos veces, no.
    huerf, _ = censo_r4(["habla de D-token-fantasma una vez", "y aquí D-token-vivo", "otra vez D-token-vivo"])
    if "D-token-fantasma" not in huerf:
        fallos.append("A2: no detecta un token con una sola aparición")
    if "D-token-vivo" in huerf:
        fallos.append("A2b: marca como huérfano un token citado dos veces")

    # Censos descuadrados entre sí.
    falso = "sesiones de trabajo S10–S120\ncronológico ascendente (S10 → S118)"
    if censos_bitacora(falso) != (120, 118):
        fallos.append("A3: no lee los dos censos")

    if ultima_archivada("la de S118 en la Sesión 120 y la de S119 en la Sesión 121.") != 119:
        fallos.append("A4: no lee la crónica de archivado")
    # Entrada PARTIDA por el justificado del párrafo: debe leerse igual.
    if ultima_archivada("la de S119 en la Sesión 121 y la de S120 en la\nSesión 122.") != 120:
        fallos.append("A4b: no lee una entrada de la crónica partida en dos líneas")

    if fallos:
        for f in fallos:
            print("  FALLO DE AUTOPRUEBA: %s" % f)
        raise SystemExit("ABORTA: el verificador no pasa su propia prueba; su informe no vale")
    print("AUTOPRUEBA: %d defectos inyectados, los %d detectados.\n" % (inyectados, inyectados))


def main():
    autoprueba()
    plan, gestion, metodo, bita = (leer(p) for p in (PLAN, GESTION, METODO, BITACORA))
    problemas = 0

    print("=== 1. INVARIANTE H3 (cabecera viva única) ===")
    n = h3(plan)
    print("   plan_trabajo_horarios.md: %d cabecera(s) '### Sesión'  -> %s"
          % (n, "OK" if n == 1 else "FALLO, se esperaba 1"))
    problemas += (n != 1)

    print("\n=== 2. CENSO DE TOKENS R4 (corpus vivo: plan + gestión + método) ===")
    huerfanos, total = censo_r4([plan, gestion, metodo])
    print("   %d tokens distintos; %d con UNA sola aparición en el corpus vivo." % (total, len(huerfanos)))
    if huerfanos:
        print("   Sospechosos (definido sin citante, o citado sin definición):")
        for t in huerfanos:
            sedes = [os.path.basename(p) for p, txt in zip(VIVOS, (plan, gestion, metodo)) if t in txt]
            print("     %-42s en %s" % (t, ", ".join(sedes)))
        print("   (El script NO decide cuál de los dos casos es: lo mira el arquitecto.)")

    print("\n=== 3. COHERENCIA DE LOS DOS CENSOS Y LA CRÓNICA ===")
    c1, c2 = censos_bitacora(bita)
    ultima = ultima_archivada(plan)
    print("   censo 1 (cabecera 'sesiones de trabajo'): S%s" % c1)
    print("   censo 2 (orden 'cronológico ascendente'): S%s" % c2)
    print("   crónica del plan, última archivada:       S%s" % ultima)
    if c1 == c2 == ultima:
        print("   -> OK, los tres coinciden")
    else:
        print("   -> FALLO: no coinciden")
        problemas += 1

    print("\n=== 4. ÍNDICES GENERADOS (M-doc-3) ===")
    for r in (GESTION, PLAN):
        print("   %-28s %s" % (os.path.basename(r), indices_ok(r)))

    print("\n=== RESUMEN ===")
    print("   comprobaciones duras con fallo: %d" % problemas)
    print("   tokens sospechosos a revisar:   %d" % len(huerfanos))
    return 1 if problemas else 0


if __name__ == "__main__":
    sys.exit(main())
