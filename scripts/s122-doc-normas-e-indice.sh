#!/usr/bin/env bash
set -uo pipefail
RAIZ="/home/luis/desarrollo/educhronos"
cd "$RAIZ" || { echo "ABORTA: no existe $RAIZ"; exit 1; }

# --- 1. Localizar los documentos (no se asume la ruta; se descubre y se reporta) ---
MET=$(find "$RAIZ" -name metodo.md -not -path '*/node_modules/*' -not -path '*/.git/*' -not -path '*/docs_extra/*')
GES=$(find "$RAIZ" -name gestion_proyecto.md -not -path '*/node_modules/*' -not -path '*/.git/*' -not -path '*/docs_extra/*')
PLA=$(find "$RAIZ" -name plan_trabajo_horarios.md -not -path '*/node_modules/*' -not -path '*/.git/*' -not -path '*/docs_extra/*')
for f in "$MET" "$GES" "$PLA"; do
  if [ "$(printf '%s\n' "$f" | wc -l)" -ne 1 ] || [ ! -f "$f" ]; then
    echo "ABORTA: no se localiza exactamente un fichero. MET=[$MET] GES=[$GES] PLA=[$PLA]"; exit 1
  fi
done
echo "RUTAS ABSOLUTAS LOCALIZADAS (anótalas, D-arranque-no-literal):"
echo "  metodo.md               = $MET"
echo "  gestion_proyecto.md     = $GES"
echo "  plan_trabajo_horarios.md= $PLA"

# --- 2. Copia de seguridad ---
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p "/tmp/s122-backup-$STAMP" || exit 1
cp "$MET" "$GES" "$PLA" "/tmp/s122-backup-$STAMP/"; CP=$?
[ $CP -eq 0 ] || { echo "ABORTA: fallo al copiar backup"; exit 1; }
echo "Backup en /tmp/s122-backup-$STAMP"

# --- GUARDA DE UN SOLO USO (añadida en el cierre de S122) ---------------------
# Este guion NO es reejecutable: inserta las normas M-doc en metodo.md y una
# segunda pasada las DUPLICA. Ocurrió en el propio cierre de S122. Se conserva
# como registro de lo que hizo, no como herramienta.
# Para regenerar los índices en cada cierre: scripts/regenerar-indice.py
if grep -qF '## M-doc — Cómo se entrega' "$MET"; then
  echo "ABORTA: las normas M-doc YA están en $MET. Este guion es de un solo uso."
  echo "Para regenerar los índices usa: python3 scripts/regenerar-indice.py"
  exit 1
fi

# --- 3. Insertar normas y generar índices ---
python3 - "$MET" "$GES" "$PLA" <<'PYEOF'
import sys, re
MET, GES, PLA = sys.argv[1], sys.argv[2], sys.argv[3]

def leer(p):
    with open(p, encoding='utf-8') as f: return f.read()
def escribir(p, s):
    with open(p, 'w', encoding='utf-8') as f: f.write(s)

def insertar_antes(texto, ancla, nuevo, etiqueta):
    if texto.count(ancla) != 1:
        raise SystemExit("ABORTA [%s]: el ancla aparece %d veces, se esperaba 1."
                         % (etiqueta, texto.count(ancla)))
    return texto.replace(ancla, nuevo + ancla)

def insertar_despues(texto, ancla, nuevo, etiqueta):
    if texto.count(ancla) != 1:
        raise SystemExit("ABORTA [%s]: el ancla aparece %d veces, se esperaba 1."
                         % (etiqueta, texto.count(ancla)))
    return texto.replace(ancla, ancla + nuevo)

# ---------- metodo.md ----------
m = leer(MET)

M_DOC = """## M-doc — Cómo se entrega la documentación (S122)

Los documentos de gestión los EDITA Claude Code sobre el repo. El modelo principal
entrega el TEXTO nuevo y su punto de inserción; NO devuelve el fichero completo.

Razón, medida en S122: `gestion_proyecto.md` (144 kB) y `plan_trabajo_horarios.md`
(264 kB) devueltos enteros son del orden de cien mil tokens de salida por cierre,
en sesiones cuyo trabajo real cabe en una fracción de eso. La norma anterior
—«pide los ficheros, modifícalos y devuélvelos enteros»— nació para evitar
instrucciones de edición que se aplican mal al copiar y pegar; que las aplique
quien tiene los ficheros delante cumple esa garantía mejor, no peor.

1. Toda edición de `.md` va en un guion autocontenido para Claude Code, con rutas
   ABSOLUTAS y con GUARDA: cada inserción se ancla a una cadena que debe aparecer
   EXACTAMENTE UNA VEZ en el fichero; si no aparece una sola vez, el guion aborta
   sin tocar nada.
2. El guion hace copia de seguridad antes de escribir y VERIFICA después, por grep
   y contra el fichero, que el texto quedó donde debía. Los códigos de salida se
   capturan inmediatamente (regla de guion de S117).
3. El modelo principal no pide los ficheros para devolverlos. Si necesita leer,
   lee del Project o encarga a Claude Code una lectura acotada.
4. Los mensajes de commit los sigue entregando el modelo principal, de una línea.

---

## M-doc-2 — Maquetas y artefactos desechables (S122)

Una maqueta de M-mockup la ESCRIBE EN DISCO Claude Code y se abre en el navegador.
Nunca se vuelca en la conversación: es un fichero grande de un solo uso. Vive fuera
del repo (`/tmp`) salvo decisión expresa de conservarla.

---

## M-doc-3 — Índice generado (S122)

`gestion_proyecto.md` y `plan_trabajo_horarios.md` llevan al principio un índice
GENERADO, delimitado por `<!-- INDICE:INICIO -->` y `<!-- INDICE:FIN -->`, con cada
encabezado y su línea. Existe para poder leer por secciones en la apertura en vez
de leer el documento entero.

Los números de línea son INDICATIVOS: se regeneran en el cierre (ver §Automatización
del cierre). Si no cuadran, manda el TEXTO del encabezado, que es lo que se busca
por grep. Un índice que se cree exacto y no lo sea es la familia de
D-tokens-inexistentes: estado vivo equivocado.

---

"""
m = insertar_antes(m, "## Tipos de sesión\n", M_DOC, "M-doc en metodo.md")

m = insertar_despues(m, "se declara como inversión consciente de M2.",
                     "\n\nLa maqueta se escribe en disco y se abre en el navegador (M-doc-2);\n"
                     "no se vuelca en la conversación.", "remisión en M-mockup")

m = insertar_despues(m, "- Diff de costura: que las regiones tocadas sean solo las previstas.",
                     "\n- Regenerar el índice de `gestion_proyecto.md` y `plan_trabajo_horarios.md`\n"
                     "  (M-doc-3): los números de línea caducan en cada cierre.",
                     "índice en automatización del cierre")
escribir(MET, m)

# ---------- índices generados ----------
INI, FIN = "<!-- INDICE:INICIO -->", "<!-- INDICE:FIN -->"

def indexar(ruta):
    t = leer(ruta)
    # quitar índice previo si lo hay (idempotencia)
    t = re.sub(re.escape(INI) + r".*?" + re.escape(FIN) + r"\n\n", "", t, flags=re.S)
    lineas = t.split("\n")
    cabeceras = [(i + 1, l) for i, l in enumerate(lineas) if re.match(r"^#{2,4} ", l)]
    if not cabeceras:
        raise SystemExit("ABORTA: sin encabezados en %s" % ruta)
    # el bloque se inserta tras la línea 1 (el H1); su longitud no depende de los dígitos
    cuerpo = ["%s" % INI, "<!-- Generado en M1 (M-doc-3). Líneas INDICATIVAS; manda el texto. -->", ""]
    cuerpo += ["- L%d — %s" % (0, c[1]) for c in cabeceras]
    cuerpo += ["", FIN, ""]
    desplaz = len(cuerpo)
    cuerpo = ["%s" % INI, "<!-- Generado en M1 (M-doc-3). Líneas INDICATIVAS; manda el texto. -->", ""]
    cuerpo += ["- L%d — %s" % (n + desplaz, txt) for n, txt in cabeceras]
    cuerpo += ["", FIN, ""]
    nuevas = [lineas[0]] + cuerpo + lineas[1:]
    escribir(ruta, "\n".join(nuevas))
    return len(cabeceras)

for r in (GES, PLA):
    n = indexar(r)
    print("Índice generado en %s: %d encabezados" % (r, n))
PYEOF
RC=$?
[ $RC -eq 0 ] || { echo "ABORTA: el paso python falló (rc=$RC). Restaura de /tmp/s122-backup-$STAMP"; exit 1; }

# --- 4. Verificación contra el fichero, no de memoria ---
echo "--- VERIFICACIÓN ---"
for par in "$MET:## M-doc — Cómo se entrega" "$MET:## M-doc-2 — Maquetas" "$MET:## M-doc-3 — Índice generado" \
           "$MET:La maqueta se escribe en disco" "$MET:Regenerar el índice de" \
           "$GES:<!-- INDICE:INICIO -->" "$PLA:<!-- INDICE:INICIO -->"; do
  F="${par%%:*}"; S="${par#*:}"
  N=$(grep -cF "$S" "$F")
  printf '%s  [%s] -> %s\n' "$([ "$N" -eq 1 ] && echo OK || echo FALLO)" "$S" "$N"
done
echo "--- INVARIANTE H3 DEL PLAN (debe dar 1) ---"
grep -c "^### Sesión" "$PLA"
echo "--- git ---"
git -C "$RAIZ" status --short
