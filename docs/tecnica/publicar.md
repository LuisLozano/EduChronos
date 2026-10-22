# Cómo se publica e instala

Responde a esta pregunta: cómo sale una versión de Educhronos hacia el centro y cómo se instala allí.
Léelo antes de publicar una versión, o cuando falle el workflow que construye el zip de Windows.

Describe el commit 30b8d67a9b49.

Nada de lo que cuenta este documento se ejecuta en la verificación de la documentación: publicar es externo al repo.
El procedimiento paso a paso, con sus comprobaciones, está en `docs/empaquetado.md`.

## Qué se publica

Se publica un zip para Windows, Educhronos-win.zip, y su huella en Educhronos-win.zip.sha256.
El zip lleva el programa con un Java propio, de modo que el equipo del centro no necesita instalar Java.
Los dos ficheros van en la Release de GitHub del tag.

## Cómo se dispara

Lo construye el workflow `.github/workflows/bundle.yml`.
Corre con un tag que empieza por v, o lanzado a mano (`.github/workflows/bundle.yml:11-15`).
Lanzado a mano sin tag construye el zip pero no lo publica (`.github/workflows/bundle.yml:15`, `.github/workflows/bundle.yml:147`).

Para publicar una versión se crea el tag sobre un commit de main con los tests en verde y se sube (`docs/empaquetado.md:78-80`).
No se ejecuta en la verificación.

```bash
git tag -a vX.Y.Z -m "…"
git push origin vX.Y.Z
```

Si el nombre del tag lleva guion, como v0.1.0-rc.1, la Release sale como pre-release (`docs/empaquetado.md:24-26`).

## Qué hace el workflow

Tiene tres jobs, uno detrás de otro.

El job linux construye el jar con `scripts/empaquetar-linux.sh` y le pasa la versión del tag sin la v (`.github/workflows/bundle.yml:34-43`).
El guion construye el jar como cuenta `docs/tecnica/construir.md`, baja el Java portable de Windows y arma la entrega.
Después saca la huella del jar y la publica como salida del job, por un canal distinto de la entrega (`.github/workflows/bundle.yml:45-62`, `.github/workflows/bundle.yml:5-6`).

El job windows recibe la entrega (`.github/workflows/bundle.yml:76-79`) y ejecuta `scripts/empaquetar-windows.ps1` con esa huella y la opción -SinHumo (`.github/workflows/bundle.yml:81-88`).
El guion comprueba la huella, monta el programa con jpackage y lo comprime.
Luego el job comprueba que el .sha256 casa con el zip, byte a byte, y que el zip lleva el instalador, copia de `scripts/Instalar.cmd`, con finales de línea CRLF (`.github/workflows/bundle.yml:90-125`).

El job publicar solo corre con un tag.
Verifica el zip con su .sha256 y crea la Release con los dos ficheros; si el tag lleva guion, la marca como pre-release (`.github/workflows/bundle.yml:144-171`, `.github/workflows/bundle.yml:166-168`).

## El instalador

El zip lleva el instalador, una copia de `scripts/Instalar.cmd`.
Instala el programa en Windows con una cuenta normal, sin permisos de administrador (`docs/empaquetado.md:122-123`).
Deja el programa en la carpeta de programas del usuario y un acceso directo en el Escritorio (`scripts/Instalar.cmd:8-10`).
Si se entrega una base de partida y no hay datos, la pone como base del programa (`scripts/Instalar.cmd:11-13`).

cmd.exe falla con finales de línea solo LF, así que `.gitattributes:1-2` obliga a que `scripts/Instalar.cmd` tenga CRLF en todo checkout.

El workflow `.github/workflows/instalador.yml` prueba el instalador en Windows (`.github/workflows/instalador.yml:3-27`).
Corre en los push a main que tocan el instalador, su prueba, el propio workflow o `.gitattributes`, y también a mano.
Ejecuta `scripts/probar-instalar.ps1` con su autoprueba: ocho casos sobre carpetas temporales, y después unos mutantes del instalador que tienen que hacer caer algún caso (`scripts/probar-instalar.ps1:4-6`, `scripts/probar-instalar.ps1:14-16`).
Esta prueba solo corre en Windows; por eso no está en `docs/tecnica/probar.md`.

## Instalar en el centro

La instalación con el instalador es el punto 8 de `docs/empaquetado.md:122-155`.
En resumen: se copia el zip del USB al equipo, se extrae y se hace doble clic en Instalar.cmd.
Para actualizar, antes se copia aparte la carpeta de datos como respaldo, porque una base abierta con una versión nueva ya no la abre la anterior (`docs/empaquetado.md:142-144`).

Ese punto 8 y los anteriores cuentan también cómo bajar los dos ficheros de la Release, comprobar la huella y llevarlos por USB.
Ahí está el procedimiento manual, para cuando la CI no esté disponible.
