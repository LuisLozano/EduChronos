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

## Publicar una versión, paso a paso

No se ejecuta en la verificación.

1. **Comprobar la CI de main.** El commit que se publica tiene que estar en main, con la ejecución del workflow tests en verde (`docs/empaquetado.md:78`).
   Ese workflow, `.github/workflows/tests.yml`, corre en cada push a main (`.github/workflows/tests.yml:3-6`).
   Su resultado se ve en la pestaña Actions del repositorio.
2. **Crear el tag** sobre ese commit, con la primera orden del bloque de abajo.
   Sin guion en el nombre, la Release sale como definitiva; con guion, como v0.1.0-rc.1, sale como pre-release (`docs/empaquetado.md:24-26`, `docs/empaquetado.md:79-80`).
3. **Empujar el tag**, con la segunda orden:

   ```bash
   git tag -a vX.Y.Z -m "…"
   git push origin vX.Y.Z
   ```

4. **Seguir el workflow.** El tag lanza el workflow bundle, `.github/workflows/bundle.yml`, que corre con todo tag que empieza por v (`.github/workflows/bundle.yml:9-14`).
   Se ve en la pestaña Actions, con el nombre bundle.
   También se puede lanzar a mano; sin tag, construye el zip pero no lo publica (`.github/workflows/bundle.yml:15`, `.github/workflows/bundle.yml:147`).
   Tiene tres jobs, uno detrás de otro:
   - El job linux construye el jar con `scripts/empaquetar-linux.sh` y le pasa la versión del tag sin la v (`.github/workflows/bundle.yml:34-43`).
     El guion construye el jar como cuenta `docs/tecnica/construir.md`, baja el Java portable de Windows y arma la entrega.
     Después saca la huella del jar y la publica como salida del job, por un canal distinto de la entrega (`.github/workflows/bundle.yml:45-62`, `.github/workflows/bundle.yml:5-6`).
   - El job windows recibe la entrega (`.github/workflows/bundle.yml:76-79`) y ejecuta `scripts/empaquetar-windows.ps1` con esa huella y la opción -SinHumo (`.github/workflows/bundle.yml:81-88`).
     El guion comprueba la huella, monta el programa con jpackage y lo comprime.
     Luego el job comprueba que el .sha256 casa con el zip, byte a byte, y que el zip lleva el instalador con finales de línea CRLF (`.github/workflows/bundle.yml:90-125`).
   - El job publicar solo corre con un tag. Verifica el zip con su .sha256 y crea la Release (`.github/workflows/bundle.yml:144-171`).
5. **Ver la Release.** Cuando bundle termina en verde, la Release del tag aparece en la página de Releases del repositorio.
   Se titula «Educhronos X.Y.Z», lleva el commit en sus notas y trae dos ficheros: Educhronos-win.zip y Educhronos-win.zip.sha256 (`.github/workflows/bundle.yml:160-171`).
   Si el tag lleva guion, está marcada como pre-release (`.github/workflows/bundle.yml:166-168`).
6. **Saber qué lleva el zip.** Dentro hay una carpeta Educhronos con Educhronos.exe, app y runtime, que es el programa con su Java, y el instalador Instalar.cmd (`scripts/Instalar.cmd:5-6`).
   El instalador es `scripts/Instalar.cmd`.
   El guion de Linux lo copia a la entrega (`scripts/empaquetar-linux.sh:220-223`).
   El de Windows comprueba que tiene CRLF y lo copia a la carpeta del programa después de jpackage (`scripts/empaquetar-windows.ps1:185-195`); después comprime esa carpeta (`scripts/empaquetar-windows.ps1:225-234`).
   El zip no lleva base de partida (`docs/empaquetado.md:129-131`).
7. **Preparar la base de partida.** Es la base de 2025/2026 del centro, con su curso nombrado: nombre 2025/2026, sin archivar.
   La saca `tools/base-partida/preparar_base_partida.py` de la base original, que tiene la tabla de cursos vacía. Escribe esa fila en una copia y no migra el esquema, porque lo migra la aplicación al abrirla (`tools/base-partida/preparar_base_partida.py:2-14`).
   La base original no está en el repo: no hay ningún .db versionado.
   Según su acta, la original está en la carpeta de aceptación de quien entrega, /home/luis/educhronos-aceptacion/s239/d3/trabajo/partida-original.db, y la preparada, en /home/luis/educhronos-aceptacion/s241/f1/base-partida/base-partida-2025-2026.db, con sus huellas (`docs/actas-aceptacion/acta-base-partida-s241.md:4-16`).
   La base no entra en el zip: viaja en el USB y la copia a mano quien entrega (`docs/actas-aceptacion/acta-base-partida-s241.md:32`).
   En el equipo del centro, la carpeta base-partida, con un único .db, va dentro de la carpeta Educhronos extraída, junto a Instalar.cmd (`docs/empaquetado.md:129-134`).
8. **Saber qué hace Instalar.cmd con ella.** Si base-partida tiene un solo .db y la carpeta de datos no tiene ningún .db ni el fichero curso-abierto, la copia allí como educhronos.db (`scripts/Instalar.cmd:11-14`, `scripts/Instalar.cmd:47-61`).
   Si hay varios .db, no copia ninguno, y si la carpeta de datos ya tiene datos, tampoco (`scripts/Instalar.cmd:71-79`).
   La aplicación migra la base al abrirla por primera vez (`docs/empaquetado.md:150-152`).
9. **Saber qué pasa sin base de partida.** Instalar.cmd no toca la carpeta de datos (`scripts/Instalar.cmd:68-70`).
   Al arrancar, el programa abre educhronos.db en esa carpeta (`app/src/main/java/es/yaroki/educhronos/app/config/CarpetaDatos.java:174-178`).
   Una base vacía recibe el esquema completo (`app/src/main/java/es/yaroki/educhronos/app/config/PreparadorEsquema.java:35-37`): el programa arranca sin datos del centro.

## El instalador

`scripts/Instalar.cmd` instala el programa en Windows con una cuenta normal, sin permisos de administrador (`docs/empaquetado.md:122-123`).
Deja el programa en la carpeta de programas del usuario y un acceso directo en el Escritorio (`scripts/Instalar.cmd:8-10`).

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
Los dos se bajan desde Linux, sin credenciales, y el zip se comprueba con sha256sum -c antes de copiarlo al USB (`docs/empaquetado.md:83-88`).

El manual de uso, `docs/manual/manual.md`, cuenta también la instalación y la actualización en el centro, en su sección 2.
Su última sección es la guía de incidencias, `docs/guia-incidencias.md`, que `tools/manual/generar-pdf.sh` añade al PDF.
Ahí está el procedimiento manual, para cuando la CI no esté disponible.
