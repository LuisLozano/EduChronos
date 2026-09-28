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
     rescatar a un token que se archivará con ella (S157). Sale también del corpus
     la línea del índice generado que copia la cabecera de sesión: la reproduce
     literal y le rescataba sus tokens (D-censo-r4-rescate-por-indice, S179).
  3. Extinción respecto de HEAD: tokens que tenían ≥1 aparición en el corpus vivo
     de HEAD y 0 en el actual. Es INFORME y NO suma a problemas.
  4. Coherencia de los DOS censos de la bitácora entre sí y con la crónica de
     archivado del plan.
  5. Índices de M-doc-3: una entrada descuadrada o un índice ausente es FALLO
     DURO y suma a problemas (S157).
  6. Frase de ventana del plan («El plan conserva ahora Sxxx … y Syyy …»): se
     DERIVA de las cabeceras —la previa degradada y la H3 viva— y es FALLO DURO si
     no cuadra, falta o se repite a principio de línea (sección 5 de la salida;
     D-verificar-cierre-ciego-a-la-ventana, S179).
  7. Tablas de los cuatro ficheros (plan, gestión, método y bitácora): cada fila
     tiene tantas celdas como su cabecera, contando la barra sin escapar dentro de
     código, y ninguna fila `|` queda fuera de toda tabla. Cada descuadre y cada
     fila fuera de tabla es FALLO DURO (sección 6 de la salida;
     D-verificar-cierre-ciego-a-las-tablas, S179).

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


def _retirar_entradas(plan):
    """-> (plan sin el tramo [ini, fin) ni sus líneas de índice, nº de líneas de índice quitadas).

    El índice generado (M-doc-3) copia cada encabezado literal, así que la cabecera
    de sesión reaparece en él y rescataba sus tokens (D-censo-r4-rescate-por-indice,
    S158). Se casa por TEXTO y no por número de línea: el verificador corre antes de
    regenerar el índice, cuando sus números pueden estar caducados.
    """
    ini, fin = _limites_entradas(plan)
    lineas = plan.split("\n")
    cabeceras = {l for l in lineas[ini:fin] if re.match(r"^#{2,4} ", l)}
    resto, en_indice, quitadas = [], False, 0
    for l in lineas[:ini] + lineas[fin:]:
        if l == "<!-- INDICE:INICIO -->":
            en_indice = True
        elif l == "<!-- INDICE:FIN -->":
            en_indice = False
        elif en_indice:
            m = re.match(r"^- L\d+ — (.*)$", l)
            if m and m.group(1) in cabeceras:
                quitadas += 1
                continue
        resto.append(l)
    return "\n".join(resto), quitadas


def sin_entradas(plan):
    return _retirar_entradas(plan)[0]


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


# --- f) frase de ventana: se DERIVA del plan, no se lee a mano --------------
RX_VIVA = re.compile(r"^### Sesión (\d+)\b", re.M)
RX_PREVIA = re.compile(r"^Última sesión registrada \(previa\): Sesión (\d+)\b", re.M)
RX_FRASE = re.compile(r"^El plan conserva ahora S(\d+) \(degradada a formato compacto\) y S(\d+) como única"
                      r" cabecera H3 viva\.", re.M)


def ventana(plan):
    """Lista de problemas de la frase de ventana (D-verificar-cierre-ciego-a-la-ventana); vacía si cuadra.

    La frase cuenta sólo A PRINCIPIO DE LÍNEA: las citas a media línea (líneas de R4,
    fichas) no son la frase.
    """
    viva, previa, frase = RX_VIVA.findall(plan), RX_PREVIA.findall(plan), RX_FRASE.findall(plan)
    problemas = []
    if len(viva) != 1:
        problemas.append("cabecera '### Sesión N': %d, se exige 1" % len(viva))
    if len(previa) != 1:
        problemas.append("'Última sesión registrada (previa): Sesión M': %d, se exige 1" % len(previa))
    if len(frase) != 1:
        problemas.append("frase 'El plan conserva ahora…' a principio de línea: %d, se exige 1" % len(frase))
    if not problemas and (int(frase[0][0]), int(frase[0][1])) != (int(previa[0]), int(viva[0])):
        problemas.append("la frase dice S%s y S%s; el plan tiene previa S%s y viva S%s"
                         % (frase[0][0], frase[0][1], previa[0], viva[0]))
    return problemas


# --- g) tablas: celdas por fila y filas fuera de tabla ----------------------
# Lógica escrita y probada contra inyecciones en S179 (casos A11 y A12);
# recibe el TEXTO del fichero, no su ruta.
# GFM: una tabla exige fila separadora; la
# barra dentro de `código` SÍ parte la celda y la escapada `\|` no.
SEP = re.compile(r"^\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)*\|?\s*$")
BARRA = re.compile(r"(?<!\\)\|")


def celdas(linea):
    s = linea.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|") and not s.endswith("\\|"):
        s = s[:-1]
    return len(BARRA.split(s))


def es_valla(s):
    return s.startswith("```") or s.startswith("~~~")


def analizar_con_huerfanas(texto):
    """-> (tablas, filas, descuadres, huérfanas). Descuadre = (línea, celdas, esperadas, tipo).
    Huérfana = línea que empieza por '|' y tiene otra barra, fuera de toda tabla y de toda
    valla (GFM las pinta como párrafo, barras incluidas)."""
    lineas = texto.split("\n")
    tablas, filas, malas, huerfanas = 0, 0, [], []
    i, n, en_valla = 0, len(lineas), False
    while i < n:
        s = lineas[i].strip()
        if es_valla(s):
            en_valla = not en_valla
            i += 1
            continue
        if (not en_valla and s.startswith("|") and len(BARRA.findall(s)) >= 2
                and not (i + 1 < n and SEP.match(lineas[i + 1].strip()) and "-" in lineas[i + 1])):
            huerfanas.append(i + 1)
            i += 1
            continue
        if (not en_valla and BARRA.search(s) and i + 1 < n
                and SEP.match(lineas[i + 1].strip()) and "-" in lineas[i + 1]):
            tablas += 1
            esperadas = celdas(lineas[i])
            sep_c = celdas(lineas[i + 1])
            if sep_c != esperadas:
                malas.append((i + 2, sep_c, esperadas, "separadora"))
            j = i + 2
            while j < n:
                t = lineas[j].strip()
                if not t or t.startswith("#") or t.startswith(">") or es_valla(t):
                    break
                filas += 1
                c = celdas(lineas[j])
                if c != esperadas:
                    malas.append((j + 1, c, esperadas, "fila"))
                j += 1
            i = j
            continue
        i += 1
    return tablas, filas, malas, huerfanas


def tramos(nums):
    out = []
    for x in nums:
        if out and x == out[-1][1] + 1:
            out[-1][1] = x
        else:
            out.append([x, x])
    return ["%d–%d" % (a, b) if a != b else "%d" % a for a, b in out]


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


# Número de línea CADUCADO a propósito (L77): se casa por texto.
PLAN_CON_INDICE = ("cabecera del plan\n"
                   "<!-- INDICE:INICIO -->\n"
                   "- L77 — ### Sesión 99 — entrada viva con D-solo-cabecera\n"
                   "- L8 — ## Otra sección con D-control-fuera\n"
                   "<!-- INDICE:FIN -->\n"
                   "### Sesión 99 — entrada viva con D-solo-cabecera\n"
                   "Última fase completada (previa): 5 — x\n"
                   "## Otra sección con D-control-fuera\n")


def _a9():
    # La línea del índice que copia la cabecera de sesión NO rescata a sus tokens,
    # y la de un encabezado de fuera del tramo se queda.
    sos, solo, _ = censo_r4([sin_entradas(PLAN_CON_INDICE)], solo_entradas(PLAN_CON_INDICE))
    if "D-solo-cabecera" in sos:
        return "A9: la línea de índice de la cabecera de sesión rescata su token"
    if "D-solo-cabecera" not in solo:
        return "A9b: el token de la cabecera de sesión no sale como sólo-entrada"
    if acumular([sin_entradas(PLAN_CON_INDICE)]).get("D-control-fuera", 0) != 2:
        return "A9c: se retira la línea de índice de un encabezado de fuera del tramo"
    if _retirar_entradas(PLAN_CON_INDICE)[1] != 1:
        return "A9d: no cuenta bien las líneas de índice retiradas"


def _frase(m, n, cola=" (paréntesis)"):
    return ("El plan conserva ahora S%d (degradada a formato compacto) y S%d como única cabecera H3 viva."
            % (m, n)) + cola


def _a10():
    base = "### Sesión 8 — viva\nÚltima sesión registrada (previa): Sesión 7 — x\n"
    casos = [
        ("A10a", base + _frase(7, 8) + "\n", 0, "da problema con todo cuadrando"),
        ("A10b", base + _frase(6, 8) + "\n", 1, "no ve una frase que nombra otra previa"),
        ("A10c", base + _frase(7, 9) + "\n", 1, "no ve una frase que nombra otra viva"),
        ("A10d", base, 1, "no ve que falta la frase"),
        ("A10e", base + "cita: " + _frase(7, 8) + "\n", 1, "acepta la frase a media línea"),
        ("A10f", base + _frase(7, 8) + "\n" + _frase(7, 8) + "\n", 1, "acepta dos frases"),
        ("A10g", base + "cita: «" + _frase(7, 8, cola="") + "»\n", 1,
         "acepta una cita a media línea acabada en viva.»"),
        ("A10h", base + _frase(7, 8, cola="") + "\n", 0, "no acepta la frase sin nada detrás"),
    ]
    for nombre, texto, esperados, motivo in casos:
        if len(ventana(texto)) != esperados:
            return "%s: %s (%r)" % (nombre, motivo, ventana(texto))


TABLA_SINTETICA = ("| a | b |\n"
                   "|---|---|\n"
                   "| 1 | 2 |\n"
                   "| 1 | 2 | 3 |\n"        # L4: celda de más
                   "| `x|y` | 2 |\n"        # L5: barra dentro de código
                   "| 1 |\n"                # L6: celda de menos
                   "| `x\\|y` | 2 |\n"      # L7: control, barra escapada
                   "\n"
                   "```\n"
                   "| a | b |\n"
                   "|---|---|\n"
                   "| 1 | 2 | 3 |\n"        # control: dentro de valla
                   "```\n")


def _a11():
    _, _, malas, _ = analizar_con_huerfanas(TABLA_SINTETICA)
    lineas = [m[0] for m in malas]
    if 4 not in lineas:
        return "A11: no ve una fila con una celda de más (%r)" % (malas,)
    if 5 not in lineas:
        return "A11b: no ve la barra sin escapar dentro de código (%r)" % (malas,)
    if 6 not in lineas:
        return "A11c: no ve una fila con una celda de menos (%r)" % (malas,)
    if 7 in lineas:
        return "A11d (control): parte por una barra escapada (%r)" % (malas,)
    if [x for x in lineas if x > 7]:
        return "A11e (control): no salta la tabla dentro de una valla (%r)" % (malas,)
    if malas != [(4, 3, 2, "fila"), (5, 3, 2, "fila"), (6, 1, 2, "fila")]:
        return "A11f: descuadres inesperados (%r)" % (malas,)


def _a12():
    huerfana = "| a | b |\n|---|---|\n| 1 | 2 |\n\n| huérfana | x |\n"
    if analizar_con_huerfanas(huerfana)[3] != [5]:
        return "A12: no ve la fila fuera de tabla (%r)" % (analizar_con_huerfanas(huerfana)[3],)
    if analizar_con_huerfanas("| a | b |\n|---|---|\n| 1 | 2 |\n")[3] != []:
        return "A12b: una tabla con separadora produce huérfanas"


COMPROBACIONES = [
    ("A1", _a1), ("A1b", _a1b), ("A2", _a2), ("A2b", _a2b), ("A3", _a3),
    ("A4", _a4), ("A4b", _a4b), ("A5", _a5), ("A5b", _a5b), ("A6", _a6),
    ("A7", _a7), ("A8", _a8), ("A9", _a9), ("A10", _a10), ("A11", _a11),
    ("A12", _a12),
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
        plan_sin, indice_quitadas = _retirar_entradas(plan)
        plan_entrada = solo_entradas(plan)
    except ValueError as e:
        raise SystemExit("ABORTA: no se puede aislar la entrada de sesión del plan: %s" % e)
    quitadas = plan.count("\n") - plan_sin.count("\n") - indice_quitadas
    print("   entrada de sesión retirada del corpus: %d líneas, y %d línea(s) de índice que copian su cabecera"
          % (quitadas, indice_quitadas))
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

    print("\n=== 5. FRASE DE VENTANA DEL PLAN (derivada de las cabeceras) ===")
    pv = ventana(plan)
    if pv:
        for p in pv:
            print("   FALLO: %s" % p)
        problemas += len(pv)
    else:
        (m, n), = RX_FRASE.findall(plan)
        print("   la frase dice S%s (previa) y S%s (viva), como las cabeceras del plan -> OK" % (m, n))

    print("\n=== 6. TABLAS (celdas por fila y filas fuera de tabla) ===")
    for ruta, txt in ((PLAN, plan), (GESTION, gestion), (METODO, metodo), (BITACORA, bita)):
        t, f, malas, huerfanas = analizar_con_huerfanas(txt)
        nombre = os.path.basename(ruta)
        print("   %-28s %d tablas, %d filas, %d descuadres, %d fuera de tabla"
              % (nombre, t, f, len(malas), len(huerfanas)))
        for ln, c, e, tipo in malas:
            print("     FALLO %s:%d  %s con %d celdas, esperadas %d" % (nombre, ln, tipo, c, e))
        if huerfanas:
            print("     FALLO %s: filas fuera de tabla en líneas %s" % (nombre, ", ".join(tramos(huerfanas))))
        problemas += len(malas) + len(huerfanas)

    print("\n=== RESUMEN ===")
    print("   comprobaciones duras con fallo: %d" % problemas)
    print("   tokens de 1 aparición:          %d" % len(sospechosos))
    print("   tokens sólo en entrada:         %d" % len(solo_entrada))
    return 1 if problemas else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
