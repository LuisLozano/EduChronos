"""Genera docs/referencia-codigo-solver.md, el índice de API del módulo solver/, solo desde el código
fuente de solver/src (main y test).

Lo especifica docs/Prompt_generar_referencia_codigo.txt. Se aparta de él en un punto: las secciones de
tests van en orden alfabético de paquete, no en el orden domain, cpsat, io, cli.
Versionado en S246 desde s201/cierre/b2-generar-referencia.py (sha256 2dfc6485…), sin más cambios que
este texto y la guarda de argumentos.

Uso: python3 -I -B tools/referencia-solver/generar_referencia.py RAIZ FECHA COMMIT SALIDA
RAIZ y SALIDA, rutas absolutas. FECHA, AAAA-MM-DD. COMMIT, git rev-parse --short HEAD después del
commit de código. Sale con 2, sin escribir, si los argumentos no valen."""
import pathlib
import re
import sys

if len(sys.argv) != 5 or not all(pathlib.PurePath(a).is_absolute() for a in (sys.argv[1], sys.argv[4])):
    print("uso: python3 -I -B tools/referencia-solver/generar_referencia.py RAIZ FECHA COMMIT SALIDA"
          " (RAIZ y SALIDA, rutas absolutas)", file=sys.stderr)
    sys.exit(2)
RAIZ, FECHA, COMMIT, SALIDA = sys.argv[1:5]
MOD = "es.yaroki.educhronos.solver"
ORDEN = ["domain", "cpsat", "io", "cli"]


def sin_comentarios_ni_literales(src):
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
            out.append(" ")
        elif src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            out.append('""')
        elif c in "\"'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            i = j + 1
            out.append(c + c)
        else:
            out.append(c)
            i += 1
    return "".join(out)


def sin_anotaciones(s):
    """Quita @Anotacion y @Anotacion(...) con paréntesis anidados."""
    out, i = [], 0
    while i < len(s):
        m = re.compile(r"@(?!interface)[\w.]+").match(s, i)
        if m:
            i = m.end()
            k = i
            while k < len(s) and s[k] in " \t\n":
                k += 1
            if k < len(s) and s[k] == "(":
                prof = 0
                while k < len(s):
                    prof += s[k] == "("
                    prof -= s[k] == ")"
                    k += 1
                    if prof == 0:
                        break
                i = k
            out.append(" ")
        else:
            out.append(s[i])
            i += 1
    return "".join(out)


def esp(s):
    s = re.sub(r"\s+", " ", s).strip()
    s = re.sub(r"\s*,\s*", ", ", s)
    s = re.sub(r"\(\s+", "(", s)
    s = re.sub(r"\s+\)", ")", s)
    s = re.sub(r"<\s+", "<", s)
    s = re.sub(r"\s+>", ">", s)
    s = re.sub(r"\s+\[", "[", s)
    return s


def cierre(s, i, a, b):
    """Índice del carácter que cierra el `a` abierto en s[i]."""
    prof = 0
    for k in range(i, len(s)):
        if s[k] == a:
            prof += 1
        elif s[k] == b:
            prof -= 1
            if prof == 0:
                return k
    raise ValueError("sin cierre")


def partir_nivel0(s, sep=","):
    partes, prof, ini = [], 0, 0
    for k, c in enumerate(s):
        if c in "<([{":
            prof += 1
        elif c in ">)]}":
            prof -= 1
        elif c == sep and prof == 0:
            partes.append(s[ini:k])
            ini = k + 1
    partes.append(s[ini:])
    return [p for p in (x.strip() for x in partes) if p]


def miembros(cuerpo):
    """Trozos de nivel superior del cuerpo de un tipo: (cabecera, tiene_bloque)."""
    trozos, ini, k = [], 0, 0
    while k < len(cuerpo):
        c = cuerpo[k]
        if c == "{":
            fin = cierre(cuerpo, k, "{", "}")
            trozos.append((cuerpo[ini:k], True))
            k = ini = fin + 1
            continue
        if c == ";":
            trozos.append((cuerpo[ini:k], False))
            ini = k + 1
        elif c == "(":
            k = cierre(cuerpo, k, "(", ")")
        k += 1
    return [(esp(t), b) for t, b in trozos if esp(t)]


MODS = {"public", "protected", "private", "static", "final", "abstract", "synchronized", "native", "default",
        "strictfp", "transient", "volatile", "sealed", "non-sealed"}
TIPO_RE = re.compile(r"\b(class|record|enum|interface)\s+(\w+)")


def parsear(ruta):
    src = sin_anotaciones(sin_comentarios_ni_literales(ruta.read_text(encoding="utf-8")))
    paquete = re.search(r"\bpackage\s+([\w.]+)\s*;", src).group(1)
    imports = re.findall(r"\bimport\s+([\w.]+)\s*;", src)
    m = TIPO_RE.search(src, re.search(r"\bpackage\s+[\w.]+\s*;", src).end())
    # modificadores del tipo: lo que va entre el último ';' (import/package) y la palabra clave
    previo = src[:m.start()]
    mods = previo[previo.rfind(";") + 1:].split()
    kind, nombre = m.group(1), m.group(2)
    llave = src.index("{", m.end())
    cab = src[m.end():llave]
    info = {"paquete": paquete, "imports": imports, "kind": kind, "nombre": nombre, "mods": mods,
            "tparams": None, "componentes": [], "extiende": None, "implementa": None}
    resto = cab
    if resto.lstrip().startswith("<"):
        a = resto.index("<")
        b = cierre(resto, a, "<", ">")
        info["tparams"] = esp(resto[a:b + 1])
        resto = resto[b + 1:]
    if kind == "record":
        a = resto.index("(")
        b = cierre(resto, a, "(", ")")
        info["componentes"] = [esp(p) for p in partir_nivel0(resto[a + 1:b])]
        resto = resto[b + 1:]
    me = re.search(r"\bextends\s+(.+?)(?=\bimplements\b|$)", resto, re.S)
    mi = re.search(r"\bimplements\s+(.+)$", resto, re.S)
    if me:
        info["extiende"] = esp(me.group(1))
    if mi:
        info["implementa"] = esp(mi.group(1))
    cuerpo = src[llave + 1:cierre(src, llave, "{", "}")]
    constantes = []
    if kind == "enum":
        # constantes: hasta el primer ';' de nivel 0 (o todo el cuerpo)
        prof, fin = 0, len(cuerpo)
        for k, c in enumerate(cuerpo):
            if c in "({[":
                prof += 1
            elif c in ")}]":
                prof -= 1
            elif c == ";" and prof == 0:
                fin = k
                break
        for p in partir_nivel0(cuerpo[:fin]):
            constantes.append(re.match(r"\w+", p).group(0))
        cuerpo = cuerpo[fin + 1:] if fin < len(cuerpo) else ""
    ctors, metodos = [], []
    for t, _bloque in miembros(cuerpo):
        if "(" not in t:
            continue  # campos sin inicializador con llamada, inicializadores y constructores compactos
        antes = t[:t.index("(")]
        if "=" in antes or TIPO_RE.search(antes) or "->" in antes:
            continue  # campo con inicializador, o tipo anidado
        palabras = antes.split()
        if "private" in palabras:
            continue
        firma = esp(t)
        nombre_m = palabras[-1]
        if nombre_m == nombre and len([w for w in palabras if w not in MODS]) == 1:
            if kind != "enum":  # el constructor de un enum es implícitamente private (JLS 8.9.2)
                ctors.append(firma)
        else:
            metodos.append(firma)
    # Orden: primero los public, después el resto; dentro de cada grupo, el del código.
    metodos = [x for x in metodos if x.startswith("public ")] + [x for x in metodos if not x.startswith("public ")]
    info.update(constantes=constantes, ctors=ctors, metodos=metodos)
    return info


fuentes = sorted(p for p in pathlib.Path(RAIZ, "solver/src").rglob("*.java"))
tipos = []
for p in fuentes:
    t = parsear(p)
    t["test"] = "/src/test/" in str(p)
    t["sub"] = t["paquete"][len(MOD) + 1:]
    tipos.append(t)
nombres_modulo = {t["nombre"] for t in tipos}


def consume(t):
    usados = set()
    for imp in t["imports"]:
        if imp.startswith(MOD + "."):
            usados.add(imp.split(".")[-1])
    firmas = t["componentes"] + t["ctors"] + t["metodos"] + [t["extiende"] or "", t["implementa"] or ""]
    for ident in re.findall(r"\b[A-Z]\w*\b", " ".join(firmas)):
        if ident in nombres_modulo and any(o["nombre"] == ident and o["paquete"] == t["paquete"] for o in tipos):
            usados.add(ident)
    usados.discard(t["nombre"])
    return ", ".join(f"`{u}`" for u in sorted(usados)) if usados else "(ninguno)"


def bloque(t):
    vis = "public" if "public" in t["mods"] else "package-private"
    extra = " ".join(m for m in t["mods"] if m in ("final", "abstract", "sealed"))
    clase = f"{vis} {extra + ' ' if extra else ''}{t['kind']}"
    L = [f"### `{t['nombre']}` — {clase}", f"Paquete: `{t['paquete']}`"]
    if t["tparams"]:
        L.append(f"Parámetro de tipo: `{t['tparams']}`")
    if t["extiende"]:
        L.append(f"Extiende: `{t['extiende']}`")
    if t["implementa"]:
        L.append(f"Implementa: `{t['implementa']}`")
    if t["kind"] == "enum":
        L.append("Constantes: " + ", ".join(f"`{c}`" for c in t["constantes"]))
    for titulo, lista in (("Componentes:", t["componentes"]), ("Constructores:", t["ctors"]),
                          ("Métodos:", t["metodos"])):
        if lista:
            L.append(titulo)
            L += [f"- `{x}`" for x in lista]
            L.append("")
    if L[-1] != "":
        L.append("")
    L.append(f"Consume: {consume(t)}")
    return L


out = ["# Referencia de código — módulo `solver/`", "", "Índice de API generado exclusivamente a partir del código fuente.",
       "", f"- Fecha: {FECHA}", f"- Commit: `{COMMIT}`", "",
       "Visibilidad: `public`, package-private (sin modificador). Se omiten todos los",
       "miembros `private`. La línea **Consume** lista los tipos del módulo",
       "(`es.yaroki.educhronos.solver.*`) que aparecen en firmas o imports del tipo.", ""]
secciones = [(f"Paquete `{s}`", [t for t in tipos if not t["test"] and t["sub"] == s]) for s in ORDEN]
secciones += [(f"Tests — paquete `{s}`", [t for t in tipos if t["test"] and t["sub"] == s])
              for s in sorted({t["sub"] for t in tipos if t["test"]})]
for titulo, lista in secciones:
    if not lista:
        continue
    out += ["---", "", f"## {titulo}", ""]
    for t in sorted(lista, key=lambda x: x["nombre"]):
        out += bloque(t) + [""]
pathlib.Path(SALIDA).write_text("\n".join(out).rstrip("\n") + "\n", encoding="utf-8")
print(f"{len(tipos)} tipos de {len(fuentes)} ficheros")
