#!/usr/bin/env bash
#
# Educhronos — empaquetado, lado Linux.
#
# Construye el jar desde cero y arma la carpeta de entrega que se lleva a la
# máquina Windows, donde `empaquetar-windows.ps1` produce el app-image.
#
# jpackage no construye para otra plataforma, así que el app-image de Windows
# se hace en Windows. El jar sí es el mismo en ambas.
#
#   scripts/empaquetar-linux.sh [--salida DIR]
#
# La entrega queda en dos carpetas:
#   <salida>/jdk/    el JDK portable de Windows. Se copia UNA vez.
#   <salida>/build/  el jar, el guion, las huellas y el LEEME. Cambia en cada build.
#
# Decisión (S152): NO ejecuta la suite. Empaquetar y verificar son puertas
# distintas del método; mezclarlas daría dos motivos para un mismo fallo.
#
set -uo pipefail

VERSION_JDK="17.0.20.1+1"
RUTA_JDK_URL="jdk-17.0.20.1%2B1"
ZIP_JDK="OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip"
SHA_JDK="e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0"

CACHE="${EDUCHRONOS_CACHE:-$HOME/.cache/educhronos-empaquetado}"
SALIDA="$HOME/entrega-educhronos"
LIMITE=250000000

# Todo lo que NO es el jar dentro del app-image de Windows, medido el 2026-09-18
# sobre la máquina de construcción con estos mismos 14 módulos (transcripción
# citada en la entrada de S152 del plan): carpeta 231.691.863, de los que
# runtime 74.879.328 + Educhronos.exe 449.024 + el .cfg de app\ 381.
# Sirve para PREVER el tamaño y vale mientras no cambien la lista de módulos ni
# la versión del JDK. El número que decide la condición 2 lo mide el guion de
# Windows sobre el app-image construido, no esta resta.
CONSTANTE_WINDOWS=75328733

uso () {
  echo "Uso: $(basename "$0") [--salida DIR]"
  echo
  echo "Construye el jar desde cero y arma la carpeta de entrega para Windows."
  echo "  --salida DIR   carpeta de entrega (por defecto \$HOME/entrega-educhronos)"
  echo "  --ayuda, -h    esta ayuda"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --salida)
      if [ $# -lt 2 ] || [ -z "$2" ]; then
        echo "ABORTA: --salida necesita un valor."; uso; exit 2
      fi
      SALIDA="$2"; shift 2 ;;
    --ayuda|-h) uso; exit 0 ;;
    *) echo "Opción desconocida: $1"; uso; exit 2 ;;
  esac
done

# --salida admite ruta RELATIVA y se normaliza AQUÍ, antes de que nadie la use. Se resuelve
# contra el directorio desde el que se lanza el guion, que es lo que espera quien la teclea.
# Hace falta porque más abajo la huella del JDK se calcula dentro de un `cd` a otra carpeta:
# con $SALIDA relativa, el destino del volcado se resolvía desde ESE cd y apuntaba a un
# directorio inexistente, así que la línea del JDK no se escribía y la entrega salía con una
# sola huella. Medido en S153 con `--salida ./empaquetado`.
mkdir -p "$SALIDA" || { echo "ABORTA: no se puede crear la carpeta de salida: $SALIDA"; exit 1; }
SALIDA="$(cd "$SALIDA" && pwd)" || { echo "ABORTA: no se puede resolver la carpeta de salida."; exit 1; }

DIR_GUION="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(git -C "$DIR_GUION" rev-parse --show-toplevel 2>/dev/null)"
if [ -z "$RAIZ" ]; then echo "ABORTA: el guion no está dentro de un repo git."; exit 1; fi

echo "=============================================================="
echo " 1. PUNTO DE PARTIDA"
echo "=============================================================="
echo "repo   : $RAIZ"
echo "commit : $(git -C "$RAIZ" rev-parse --short HEAD)"
SUCIOS=$(git -C "$RAIZ" status --porcelain | wc -l)
echo "ficheros sin commitear: $SUCIOS"
if [ "$SUCIOS" -ne 0 ]; then
  echo "AVISO: el árbol no está limpio. El jar no corresponderá a un commit."
fi
echo "salida : $SALIDA"
echo "caché  : $CACHE"

echo
echo "=============================================================="
echo " 2. CONSTRUCCIÓN (sin tests, por decisión escrita)"
echo "=============================================================="
cd "$RAIZ" || exit 1
INICIO=$(date +%s)
mvn clean package -DskipTests
EXIT_MVN=$?
FIN=$(date +%s)
echo "exit mvn = $EXIT_MVN   segundos = $((FIN-INICIO))"
if [ "$EXIT_MVN" -ne 0 ]; then echo "ABORTA: la construcción falló."; exit 1; fi

echo
echo "=============================================================="
echo " 3. EL JAR"
echo "=============================================================="
JAR=$(find "$RAIZ/app/target" -maxdepth 1 -type f -name 'app-*.jar' -not -name '*sources*' | head -1)
if [ -z "$JAR" ]; then echo "ABORTA: no se encontró el jar en app/target."; exit 1; fi
JAR_NOMBRE=$(basename "$JAR")
JAR_BYTES=$(stat -c %s "$JAR")
echo "jar    : $JAR_NOMBRE"
echo "bytes  : $JAR_BYTES"

# Condición 1: un único main-*.js, y es el que cita index.html.
N_MAIN=$(unzip -l "$JAR" | grep -cE 'static/main-[^/]*\.js$')
echo "main-*.js en el jar: $N_MAIN"
if [ "$N_MAIN" -ne 1 ]; then
  echo "ABORTA: se esperaba exactamente 1 main-*.js y hay $N_MAIN."
  unzip -l "$JAR" | grep -E 'static/main-[^/]*\.js$'
  exit 1
fi
TMPIDX=$(mktemp -d)
unzip -o -j "$JAR" 'BOOT-INF/classes/static/index.html' -d "$TMPIDX" > /dev/null
EXIT_IDX=$?
if [ "$EXIT_IDX" -ne 0 ]; then echo "ABORTA: no se pudo extraer index.html."; rm -rf "$TMPIDX"; exit 1; fi
CITADO=$(grep -oE 'main-[A-Za-z0-9]*\.js' "$TMPIDX/index.html" | head -1)
PRESENTE=$(unzip -l "$JAR" | grep -oE 'main-[A-Za-z0-9]*\.js' | head -1)
rm -rf "$TMPIDX"
echo "citado en index.html : $CITADO"
echo "presente en el jar   : $PRESENTE"
if [ "$CITADO" != "$PRESENTE" ]; then
  echo "ABORTA: el bundle del jar no es el que cita index.html."
  exit 1
fi

echo
echo "=============================================================="
echo " 4. TAMAÑO PREVISTO EN WINDOWS"
echo "=============================================================="
PREVISTO=$((JAR_BYTES + CONSTANTE_WINDOWS))
echo "jar                    : $JAR_BYTES B"
echo "runtime + exe (S152)   : $CONSTANTE_WINDOWS B"
echo "carpeta prevista       : $PREVISTO B  ($((PREVISTO / 1000000)) MB de 10^6)"
echo "límite (condición 2)   : $LIMITE B"
if [ "$PREVISTO" -lt "$LIMITE" ]; then
  echo "PREVISIÓN: cumple. Margen $(( (LIMITE - PREVISTO) / 1000000 )) MB."
else
  echo "PREVISIÓN: NO cumple. Exceso $(( (PREVISTO - LIMITE) / 1000000 )) MB."
  echo "Palanca medida y no aplicada (S152): los nativos de OR-Tools de otras"
  echo "plataformas son 60.869.367 B y cuelgan como runtime de ortools-java."
fi
echo "Es una previsión. El número que vale lo mide empaquetar-windows.ps1."

echo
echo "=============================================================="
echo " 5. JDK PORTABLE DE WINDOWS (caché)"
echo "=============================================================="
mkdir -p "$CACHE"
if [ -f "$CACHE/$ZIP_JDK" ]; then
  echo "En caché, no se descarga."
else
  echo "Descargando Temurin $VERSION_JDK para Windows…"
  curl -fL -o "$CACHE/$ZIP_JDK" \
    "https://api.adoptium.net/v3/binary/version/$RUTA_JDK_URL/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
  EXIT_CURL=$?
  echo "exit curl = $EXIT_CURL"
  if [ "$EXIT_CURL" -ne 0 ]; then
    echo "ABORTA: la descarga falló."
    rm -f "$CACHE/$ZIP_JDK"
    exit 1
  fi
fi
TIPO=$(file -b --mime-type "$CACHE/$ZIP_JDK")
echo "tipo: $TIPO"
if [ "$TIPO" != "application/zip" ]; then
  echo "ABORTA: lo descargado no es un zip."
  head -c 300 "$CACHE/$ZIP_JDK"; echo
  exit 1
fi
SHA_MEDIDO=$(sha256sum "$CACHE/$ZIP_JDK" | cut -d' ' -f1)
echo "sha256 esperado : $SHA_JDK"
echo "sha256 medido   : $SHA_MEDIDO"
if [ "$SHA_MEDIDO" != "$SHA_JDK" ]; then
  echo "ABORTA: el JDK de la caché no es el fijado. Bórralo y vuelve a lanzar."
  exit 1
fi

echo
echo "=============================================================="
echo " 6. CARPETA DE ENTREGA"
echo "=============================================================="
mkdir -p "$SALIDA/jdk"
rm -rf "$SALIDA/build"
mkdir -p "$SALIDA/build"

if [ -f "$SALIDA/jdk/$ZIP_JDK" ]; then
  echo "jdk/ ya tiene el zip; no se vuelve a copiar (son ~190 MB)."
else
  cp "$CACHE/$ZIP_JDK" "$SALIDA/jdk/"
  echo "exit cp jdk = $?"
fi

cp "$JAR" "$SALIDA/build/"
echo "exit cp jar = $?"
cp "$RAIZ/scripts/empaquetar-windows.ps1" "$SALIDA/build/"
EXIT_PS=$?
echo "exit cp ps1 = $EXIT_PS"
if [ "$EXIT_PS" -ne 0 ]; then echo "ABORTA: falta scripts/empaquetar-windows.ps1."; exit 1; fi

# Los dos `cd` son para que en SHA256SUMS quede el nombre PELADO del fichero, que es lo que
# el guion de Windows espera (compone las rutas por su cuenta: el jar junto a él, el zip en
# la carpeta jdk\ hermana). El volcado del segundo va FUERA del subshell, para que el
# destino se resuelva en el directorio de partida y no dentro del `cd`.
( cd "$SALIDA/build" && sha256sum "$JAR_NOMBRE" ) > "$SALIDA/build/SHA256SUMS"
EXIT_SHA_JAR=$?
( cd "$SALIDA/jdk"   && sha256sum "$ZIP_JDK"   ) >> "$SALIDA/build/SHA256SUMS"
EXIT_SHA_JDK=$?
echo "exit sha jar = $EXIT_SHA_JAR ; exit sha jdk = $EXIT_SHA_JDK"
if [ "$EXIT_SHA_JAR" -ne 0 ] || [ "$EXIT_SHA_JDK" -ne 0 ]; then
  echo "ABORTA: no se pudo calcular alguna huella."
  exit 1
fi
echo "--- SHA256SUMS:"
cat "$SALIDA/build/SHA256SUMS"

# GUARDA DE LA ENTREGA. El .ps1 ya comprueba esto mismo al llegar a Windows —exige 2
# entradas, un .jar y un .zip— pero el lado Linux no comprobaba NADA de lo que acababa de
# escribir. Por eso en S153 una entrega con una sola huella se dio por buena aquí, se
# transportaron 190 MB a la otra máquina, y el defecto no apareció hasta que el .ps1 abortó
# con «se esperaban 2 entradas (jar y jdk)». La entrega se valida donde se construye.
LINEAS_SHA=$(wc -l < "$SALIDA/build/SHA256SUMS")
CAMPOS_MAL=$(awk 'NF != 2 { n++ } END { print n+0 }' "$SALIDA/build/SHA256SUMS")
N_JAR=$(awk '{ print $2 }' "$SALIDA/build/SHA256SUMS" | grep -c '\.jar$')
N_ZIP=$(awk '{ print $2 }' "$SALIDA/build/SHA256SUMS" | grep -c '\.zip$')
echo "guarda: $LINEAS_SHA líneas, $CAMPOS_MAL con campos mal, $N_JAR .jar, $N_ZIP .zip"
if [ "$LINEAS_SHA" -ne 2 ] || [ "$CAMPOS_MAL" -ne 0 ] || [ "$N_JAR" -ne 1 ] || [ "$N_ZIP" -ne 1 ]; then
  echo "ABORTA: SHA256SUMS no ha quedado con dos entradas de dos campos, un .jar y un .zip."
  echo "        Contenido escrito:"
  sed 's/^/        /' "$SALIDA/build/SHA256SUMS"
  echo "        La entrega NO sirve; no se transporta."
  exit 1
fi

cat > "$SALIDA/build/LEEME.txt" <<LEEME_EOF
Educhronos - entrega para construir el app-image de Windows.

Generado por scripts/empaquetar-linux.sh
Commit: $(git -C "$RAIZ" rev-parse --short HEAD)
Fecha:  $(date '+%Y-%m-%d %H:%M')

Que traer a Windows:
  jdk/     una sola vez (el JDK no cambia mientras no cambie su version fijada)
  build/   en cada construccion

Como construir, desde la carpeta build/:
  powershell -ExecutionPolicy Bypass -File .\empaquetar-windows.ps1

Opciones:
  -Base C:\ruta    donde trabajar (por defecto C:\DES\educhronos-build)
  -SinHumo         no arranca la aplicacion despues de empaquetar

El guion verifica las dos huellas de SHA256SUMS antes de nada y aborta si
alguna no coincide.
LEEME_EOF

echo
echo "=============================================================="
echo " ENTREGA LISTA"
echo "=============================================================="
ls -l "$SALIDA/jdk" "$SALIDA/build"
du -sh "$SALIDA/jdk" "$SALIDA/build"
echo
echo "Desde Windows, trae estas carpetas y ejecuta en build\\:"
echo "  powershell -ExecutionPolicy Bypass -File .\\empaquetar-windows.ps1"
