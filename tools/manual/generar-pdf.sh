#!/usr/bin/env bash
# PDF del manual de uso (S242, O-manual). Uso: bash tools/manual/generar-pdf.sh <salida.pdf>
#
# pandoc une docs/manual/manual.md y docs/guia-incidencias.md (la última sección, «Si algo no va
# bien», decisión CW) en un HTML autónomo con tools/manual/manual.css, y google-chrome lo imprime
# a PDF sin cabecera ni pie del navegador. El HTML y el perfil de Chrome van a una carpeta
# temporal que se borra al terminar; Chrome usa un perfil propio para no tocar el del usuario.
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: bash tools/manual/generar-pdf.sh <salida.pdf>" >&2
  exit 2
fi

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SALIDA="$(realpath -m "$1")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pandoc "$RAIZ/docs/manual/manual.md" "$RAIZ/docs/guia-incidencias.md" \
  --from markdown --to html5 --standalone --embed-resources \
  --resource-path "$RAIZ/docs/manual:$RAIZ/docs" \
  --css "$RAIZ/tools/manual/manual.css" \
  --metadata pagetitle="Educhronos — Manual de uso" --metadata lang=es \
  --output "$TMP/manual.html"

google-chrome --headless --disable-gpu --no-first-run --no-default-browser-check \
  --user-data-dir="$TMP/perfil" --no-pdf-header-footer \
  --print-to-pdf="$SALIDA" "file://$TMP/manual.html"

test -s "$SALIDA"
echo "PDF escrito: $SALIDA"
