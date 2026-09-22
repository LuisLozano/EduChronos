#!/usr/bin/env python3
"""Verificación mecánica del paso M1.6 (metodo.md, §Automatización del cierre).

REPORTA, no corrige. El arquitecto lee la salida y decide.

Comprueba:
  1. Invariante H3: `plan_trabajo_horarios.md` tiene exactamente UNA cabecera
     `### Sesión`.
  2. Censo de tokens R4: cuenta `D-*`, `Dnn`, `C-*` y `O-*` sobre el corpus vivo,
     que es el plan SIN su entrada de sesión, más gestión y método. `§x.y` y `Cx`
     NO se cuentan: no son identificadores únicos —el mismo `§4` existe en varios
     documentos— y quedan a comprobación humana al archivar (ver metodo.md, R4).
     Aproximación declarada: un token que aparece UNA SOLA VEZ en todo el corpus
     vivo es sospechoso —o está definido y nadie lo cita, o lo cita alguien y no
     está definido—. El script no distingue cuál de las dos; señala el token y el
     humano mira. Se listan aparte los tokens que NO aparecen fuera de la entrada
     de sesión y sólo viven dentro de ella: la línea de R4 de una sesión no puede
     rescatar a un token que se archivará con ella (S157).
  3. Extinción respecto de HEAD: tokens que tenían ≥1 aparición en el corpus vivo
     de HEAD y 0 en el actual. Es INFORME y NO suma a problemas.
  4. Coherencia de los DOS censos de la bitácora entre sí y con la crónica de
     archivado del plan.
  5. Índices de M-doc-3: una entrada descuadrada o un índice ausente es FALLO
     DURO y suma a problemas (S157).

El corpus VIVO son los tres documentos de estado: plan, gestión y método.
`bitacora-sesiones.md` es histórico de solo lectura (R4/R5) y NO cuenta como
citante vivo: ésa es justo la trampa que R4 existe para evitar. Desde S157
tampoco cuenta la entrada de sesión viva del plan, por la misma razón.

RAIZ se puede fijar por la variable de entorno EDUCHRONOS_RAIZ, para correr el
guion desde fuera del repo sin escribir en él.

Se somete a AUTOPRUEBA antes de reportar nada (precisión de M2 sobre verificación
de instrumentos): se le inyectan defectos que DEBE detectar, y aborta si no los ve.
"""
import os
import re
import subprocess
import sys

RAIZ = os.environ.get("EDUCHRONOS_RAIZ") or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAN = os.path.join(RAIZ, "docs", "plan_trabajo_horarios.md")
GESTION = os.path.join(RAIZ, "docs", "gestion_proyecto.md")
METODO = os.path.join(RAIZ, "docs", "metodo.md")
BITACORA = os.path.join(RAIZ, "docs", "bitacora-sesiones.md")
VIVOS = [PLAN, GESTION, METODO]
NOMBRES_VIVOS = ["plan_trabajo_horarios.md", "gestion_proyecto.md", "metodo.md"]

# --- a) patrones -----------------------------------------------------------
BASE_TOKEN = (r"\bD-[A-Za-zÀ-ÿ0-9][A-Za-zÀ-ÿ0-9._-]*[A-Za-zÀ-ÿ0-9]"
              r"|\bD\d{1,2}\b"
              r"|\bC-[a-zà-ÿ][A-Za-zÀ-ÿ0-9._-]*[A-Za-zÀ-ÿ0-9]")
O_TOKEN = r"|\bO-[a-zà-ÿ][A-Za-zÀ-ÿ0-9._-]*[A-Za-zÀ-ÿ0-9]"


def construir_token():
    return re.compile(BASE_TOKEN + O_TOKEN)


TOKEN = construir_token()


def leer(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def h3(texto):
    return len(re.findall(r"^### Sesión", texto, re.M))


def tokens_de(texto, rx=None):
    """Cuenta apariciones de cada token en un texto."""
    rx = rx if rx is not None else TOKEN
    cuenta = {}
    for m in rx.finditer(texto):
        t = m.group(0).rstrip(".,;:)")
        cuenta[t] = cuenta.get(t, 0) + 1
    return cuenta


# --- b) el bloque de la entrada de sesión sale del corpus -------------------
def _limites_entradas(plan):
    """(ini, fin) de líneas: ini = '### Sesión', fin = 'Última fase completada'.

    Cada marca debe aparecer EXACTAMENTE una vez; si no, aborta con el motivo.
    """
    lineas = plan.split("\n")
    ini = [i for i, l in enumerate(lineas) if re.match(r"^### Sesión", l)]
    fin = [i for i, l in enumerate(lineas) if re.match(r"^Última fase completada", l)]
    if len(ini) != 1:
        raise ValueError("la marca '### Sesión' aparece %d veces, se exige exactamente 1" % len(ini))
    if len(fin) != 1:
        raise ValueError("la marca 'Última fase completada' aparece %d veces, se exige exactamente 1" % len(fin))
    if fin[0] < ini[0]:
        raise ValueError("'Última fase completada' aparece ANTES de '### Sesión'")
    return ini[0], fin[0]


def sin_entradas(plan):
    ini, fin = _limites_entradas(plan)
    lineas = plan.split("\n")
    return "\n".join(lineas[:ini] + lineas[fin:])


def solo_entradas(plan):
    ini, fin = _limites_entradas(plan)
    lineas = plan.split("\n")
    return "\n".join(lineas[ini:fin])


# --- c) censo con dos categorías -------------------------------------------
def acumular(textos, rx=None):
    total = {}
    for txt in textos:
        for t, n in tokens_de(txt, rx).items():
            total[t] = total.get(t, 0) + n
    return total


def censo_r4(textos_vivos, texto_entradas="", rx=None):
    """-> (sospechosos_1_aparicion, solo_en_entrada, nº de tokens distintos)."""
    vivos = acumular(textos_vivos, rx)
    dentro = acumular([texto_entradas], rx)
    sospechosos = sorted(t for t, n in vivos.items() if n == 1)
    solo_entrada = sorted(t for t in dentro if vivos.get(t, 0) == 0)
    return sospechosos, solo_entrada, len(vivos)


# --- d) extinción ----------------------------------------------------------
def extintos(cuenta_head, cuenta_actual):
    """Función PURA: tokens con ≥1 en HEAD y 0 ahora. Se prueba sin git."""
    return sorted(t for t, n in cuenta_head.items() if n >= 1 and cuenta_actual.get(t, 0) == 0)


def docs_de_head():
    """Los tres documentos vivos tal y como están en HEAD, o None si git falla."""
    textos = []
    for nombre in NOMBRES_VIVOS:
        r = subprocess.run(["git", "-C", RAIZ, "show", "HEAD:docs/" + nombre],
                           capture_output=True, text=True)
        if r.returncode != 0:
            return None
        textos.append(r.stdout)
    return textos


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


# --- e) índices: (total, malas) o None, y cuentan como problema -------------
def analizar_indice(texto):
    if "<!-- INDICE:INICIO -->" not in texto:
        return None
    lineas = texto.split("\n")
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
    return (total, malas)


def indices_ok(ruta):
    return analizar_indice(leer(ruta))


def problema_indice(res):
    return res is None or res[1] > 0


# ---------------------------------------------------------------------------
# AUTOPRUEBA. Un verificador que sólo sabe decir "todo bien" no es un
# verificador (precisión de M2). Se le inyectan defectos que DEBE ver.
# Cada comprobación devuelve None si pasa, o el motivo del fallo.
# ---------------------------------------------------------------------------
def _a1():
    if h3("### Sesión 1 — x\n### Sesión 2 — y\n") != 2:
        return "A1: no cuenta bien las cabeceras H3"


def _a1b():
    if h3("texto sin cabeceras") != 0:
        return "A1b: inventa cabeceras donde no las hay"


def _a2():
    sos, _, _ = censo_r4(["habla de D-token-fantasma una vez", "y aquí D-token-vivo", "otra vez D-token-vivo"])
    if "D-token-fantasma" not in sos:
        return "A2: no detecta un token con una sola aparición"


def _a2b():
    sos, _, _ = censo_r4(["habla de D-token-fantasma una vez", "y aquí D-token-vivo", "otra vez D-token-vivo"])
    if "D-token-vivo" in sos:
        return "A2b: marca como huérfano un token citado dos veces"


def _a3():
    falso = "sesiones de trabajo S10–S120\ncronológico ascendente (S10 → S118)"
    if censos_bitacora(falso) != (120, 118):
        return "A3: no lee los dos censos"


def _a4():
    if ultima_archivada("la de S118 en la Sesión 120 y la de S119 en la Sesión 121.") != 119:
        return "A4: no lee la crónica de archivado"


def _a4b():
    # Entrada PARTIDA por el justificado del párrafo: debe leerse igual.
    if ultima_archivada("la de S119 en la Sesión 121 y la de S120 en la\nSesión 122.") != 120:
        return "A4b: no lee una entrada de la crónica partida en dos líneas"


PLAN_SINTETICO = ("cabecera del plan\n"
                  "aquí se define D-fuera-una-vez\n"
                  "### Sesión 99 — entrada viva\n"
                  "la entrada cita D-fuera-una-vez y además D-solo-dentro\n"
                  "Última fase completada (previa): 5 — x\n"
                  "cola del plan\n")


def _a5():
    # Una mención DENTRO del bloque de entrada NO rescata a un token de 1
    # aparición fuera, y un token que sólo vive dentro va a su propia lista.
    sos, solo, _ = censo_r4([sin_entradas(PLAN_SINTETICO)], solo_entradas(PLAN_SINTETICO))
    if "D-fuera-una-vez" not in sos:
        return "A5: una mención dentro de la entrada rescata un token de 1 aparición"
    if "D-solo-dentro" not in solo:
        return "A5c: no rotula el token que sólo vive en la entrada de sesión"
    if "D-solo-dentro" in sos:
        return "A5d: mete el token de sólo-entrada entre los de 1 aparición"


def _a5b():
    try:
        sin_entradas("un plan sin ninguna de las dos marcas\n")
    except ValueError:
        pass
    else:
        return "A5b: sin_entradas no aborta cuando faltan las marcas"
    try:
        sin_entradas("### Sesión 1\n### Sesión 2\nÚltima fase completada (previa): 5\n")
    except ValueError:
        pass
    else:
        return "A5b2: sin_entradas no aborta con DOS cabeceras de sesión"


def _a6():
    head = {"D-extinto": 3, "D-vivo": 2}
    actual = {"D-vivo": 1}
    ext = extintos(head, actual)
    if "D-extinto" not in ext:
        return "A6: no detecta un token que desaparece del corpus vivo"
    if "D-vivo" in ext:
        return "A6b: da por extinto un token que sigue vivo"


def _a7():
    sos, _, _ = censo_r4(["sólo una vez O-instalación, y O-vivo", "otra vez O-vivo"])
    if "O-instalación" not in sos:
        return "A7: no ve un O-* con una sola aparición"
    if "O-vivo" in sos:
        return "A7b: marca un O-* citado dos veces"


def _a8():
    bueno = "<!-- INDICE:INICIO -->\n- L4 — cuarta\n\ncuarta\n"
    malo = "<!-- INDICE:INICIO -->\n- L4 — otra cosa\n\ncuarta\n"
    rb, rm = analizar_indice(bueno), analizar_indice(malo)
    if rb != (1, 0):
        return "A8: no lee bien un índice que cuadra (%r)" % (rb,)
    if rm != (1, 1):
        return "A8b: no ve la entrada descuadrada (%r)" % (rm,)
    if problema_indice(rb):
        return "A8c: cuenta como problema un índice que cuadra"
    if not problema_indice(rm):
        return "A8d: un índice descuadrado no cuenta como problema"
    if not problema_indice(analizar_indice("texto sin marca de índice")):
        return "A8e: la ausencia de índice no cuenta como problema"


COMPROBACIONES = [
    ("A1", _a1), ("A1b", _a1b), ("A2", _a2), ("A2b", _a2b), ("A3", _a3),
    ("A4", _a4), ("A4b", _a4b), ("A5", _a5), ("A5b", _a5b), ("A6", _a6),
    ("A7", _a7), ("A8", _a8),
]


def autoprueba():
    fallos = []
    for nombre, fn in COMPROBACIONES:
        try:
            r = fn()
        except Exception as e:
            r = "%s: excepción inesperada: %r" % (nombre, e)
        if r:
            fallos.append(r)
    inyectados = len(COMPROBACIONES)      # calculado, no literal
    if fallos:
        for f in fallos:
            print("  FALLO DE AUTOPRUEBA: %s" % f)
        raise SystemExit("ABORTA: el verificador no pasa su propia prueba; su informe no vale")
    print("AUTOPRUEBA: %d defectos inyectados, los %d detectados.\n" % (inyectados, inyectados))


def main(argv):
    solo_autoprueba = "--solo-autoprueba" in argv

    autoprueba()
    if solo_autoprueba:
        return 0

    plan, gestion, metodo, bita = (leer(p) for p in (PLAN, GESTION, METODO, BITACORA))
    problemas = 0

    print("=== 1. INVARIANTE H3 (cabecera viva única) ===")
    n = h3(plan)
    print("   plan_trabajo_horarios.md: %d cabecera(s) '### Sesión'  -> %s"
          % (n, "OK" if n == 1 else "FALLO, se esperaba 1"))
    problemas += (n != 1)

    print("\n=== 2. CENSO DE TOKENS R4 (corpus vivo: plan SIN entrada + gestión + método) ===")
    print("   patrón: D-*, Dnn, C-*, O-*   (§x.y y Cx NO se cuentan: ver metodo.md R4)")
    try:
        plan_sin = sin_entradas(plan)
        plan_entrada = solo_entradas(plan)
    except ValueError as e:
        raise SystemExit("ABORTA: no se puede aislar la entrada de sesión del plan: %s" % e)
    quitadas = plan.count("\n") - plan_sin.count("\n")
    print("   entrada de sesión retirada del corpus: %d líneas" % quitadas)
    sospechosos, solo_entrada, total = censo_r4([plan_sin, gestion, metodo], plan_entrada)
    print("   %d tokens distintos en el corpus vivo." % total)
    print("   -- 2a. UNA sola aparición en el corpus vivo (%d):" % len(sospechosos))
    for t in sospechosos:
        sedes = [os.path.basename(p) for p, txt in zip(VIVOS, (plan_sin, gestion, metodo)) if t in txt]
        print("     %-44s en %s" % (t, ", ".join(sedes)))
    print("   -- 2b. CERO apariciones fuera, sólo en la entrada de sesión (%d):" % len(solo_entrada))
    for t in solo_entrada:
        print("     %-44s solo en entrada de sesión" % t)
    print("   (El script NO decide cuál de los casos es: lo mira el arquitecto.)")

    print("\n=== 2c. EXTINCIÓN respecto de HEAD (informe, NO suma a problemas) ===")
    head = docs_de_head()
    if head is None:
        print("   sin referencia HEAD")
    else:
        try:
            head_plan_sin = sin_entradas(head[0])
        except ValueError as e:
            head_plan_sin = None
            print("   sin referencia HEAD (el plan de HEAD no aísla su entrada: %s)" % e)
        if head_plan_sin is not None:
            cuenta_head = acumular([head_plan_sin, head[1], head[2]])
            cuenta_actual = acumular([plan_sin, gestion, metodo])
            ext = extintos(cuenta_head, cuenta_actual)
            print("   %d token(s) con ≥1 aparición en HEAD y 0 ahora:" % len(ext))
            for t in ext:
                print("     %-44s (HEAD: %d)" % (t, cuenta_head[t]))

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
        res = indices_ok(r)
        if res is None:
            print("   %-28s SIN ÍNDICE" % os.path.basename(r))
        else:
            print("   %-28s %d entradas, %d descuadradas" % (os.path.basename(r), res[0], res[1]))
        if problema_indice(res):
            problemas += 1

    print("\n=== RESUMEN ===")
    print("   comprobaciones duras con fallo: %d" % problemas)
    print("   tokens de 1 aparición:          %d" % len(sospechosos))
    print("   tokens sólo en entrada:         %d" % len(solo_entrada))
    return 1 if problemas else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
