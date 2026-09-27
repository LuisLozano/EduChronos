# Empaquetado y distribución

Cómo se construye el bundle de escritorio de Educhronos. Escrito en S152 dentro
de `C-construccion-reproducible`, sobre medición y no sobre estimación.

**Qué cubre:** la CONSTRUCCIÓN. Qué produce cada máquina, qué ficheros necesita
y cómo se invoca.

**Qué NO cubre:** un instalador. Lo que existe es una carpeta y su zip. Desde S174
el zip sale de la Release de GitHub y llega al centro por USB, sin firma de código
(decisión de S156 y decisión C de `O-ci`).

---

## Vía principal: la CI (S174)

Desde S174 el bundle de Windows lo construye GitHub Actions con
`.github/workflows/bundle.yml`. Ejecuta los mismos dos guiones de §2 y §3, sin copias
propias. La vía manual (§2, §3 y la máquina virtual de S163) queda de respaldo: para
cuando la CI no esté disponible o haya que diagnosticar a mano.

**Cómo se dispara.**

- **Un tag `v*` construye y publica:** `git tag -a v0.2.0 -m "…"` y
  `git push origin v0.2.0`. Si el nombre lleva guion (`v0.1.0-rc.1`), la Release sale
  como pre-release.
- **«Run workflow» en la pestaña Actions construye sin publicar:** el zip queda como
  artefacto de la ejecución durante 7 días. Sirve para ensayar. Lanzado a mano sobre un
  tag, sí publica.

**Qué hace.**

1. Job `linux` (`ubuntu-latest`, Temurin 17): `empaquetar-linux.sh --salida`, extrae la
   huella del jar de `SHA256SUMS` y la pasa como salida del job. Sube `build/` y `jdk/`
   como artefacto de un día.
2. Job `windows` (`windows-latest`, Windows PowerShell 5.1, la misma de la construcción
   manual (§1)): la orden de §3 con `-HuellaJar` tomada de esa salida y `-SinHumo`. Después
   comprueba byte a byte que `Educhronos-win.zip.sha256` corresponde al zip.
3. Job `publicar`, sólo con tag: `sha256sum -c` y crea la Release con los dos ficheros.
   Es el único job con permiso de escritura en el repositorio.

**Qué se entrega.** Los dos assets de la Release, `Educhronos-win.zip` y
`Educhronos-win.zip.sha256`. Se descargan desde Linux, se comprueban con
`sha256sum -c Educhronos-win.zip.sha256` y el zip viaja por USB. La identidad de versión
la dan el tag y el sha256 de la Release; el commit va en las notas de la Release.

**Por qué sin humo.** La prueba de humo abre el navegador y la bandeja y, sobre una base
vacía, no llega al solver (§6). El arranque del bundle lo prueba la aceptación
(`docs/guion-aceptacion.md`) sobre el zip de la Release. Decisión de S174.

**Qué la pone en rojo.** Cualquier fallo de los dos guiones; un jar que no casa con la
huella del job `linux`; un app-image de más de 250.000.000 B, porque desde S174 el `.ps1`
aborta (§3); y un `.sha256` que no corresponde al zip. Los tres últimos se comprobaron en
S174 con un defecto provocado en una rama desechable: los tres pusieron la ejecución en
rojo en el paso y con el mensaje esperados.

**Medido en S174** (`v0.1.0-rc.1`, commit `0cceca4`): carpeta del app-image
231.770.158 B en 193 ficheros, margen 18,2 MB; zip 176.078.739 B; ejecución completa en
unos dos minutos y medio. El zip usa `\` como separador y no lleva entradas de directorio,
igual que el zip manual aceptado en S172, que el Explorador extrajo sin problemas.

**Límites.** La API de GitHub sin credenciales no entrega los logs de las ejecuciones (403,
medido en S174); en S174 se leyeron en la web con la sesión del propietario del
repositorio. La huella del jar está en el log del job `linux`. `ubuntu-latest` y
`windows-latest` no están fijados a una versión: el paso de `ubuntu-latest` a Ubuntu 26,
anunciado para el 19 de octubre de 2026, puede cambiar las herramientas del job `linux`.

---

## 1. Por qué son dos máquinas

`jpackage` no construye para otra plataforma: el app-image de Windows se hace en
Windows. El jar sí es el mismo en las dos, así que se construye una sola vez en
Linux, que es la plataforma de desarrollo.

| Máquina | Guion | Produce |
|---|---|---|
| Linux | `scripts/empaquetar-linux.sh` | el jar y la carpeta de entrega |
| Windows | `scripts/empaquetar-windows.ps1` | el app-image y su zip |

La máquina Windows **no necesita** código fuente, ni Git, ni Maven, ni Java
instalado. El JDK viaja dentro de la entrega. Solo hace falta PowerShell; medido
con Windows PowerShell 5.1.

En la vía manual, el traspaso entre las dos lo hace una persona: el usuario tira la carpeta desde Windows.
El guion de Linux no conoce la dirección de la máquina Windows.
En la CI el traspaso lo hace un artefacto de Actions entre los dos jobs.

---

## 2. Lado Linux

Esta sección y la §3 describen la vía manual, de respaldo desde S174. La vía principal es
la CI, que ejecuta estos mismos guiones (ver «Vía principal: la CI»).

    scripts/empaquetar-linux.sh [--salida DIR]

Por defecto la entrega queda en `$HOME/entrega-educhronos`. `--salida` acepta ruta
relativa: el guion la normaliza a absoluta nada más leerla, contra el directorio desde el
que se lanza.

Qué hace, en orden:

1. Avisa si el árbol de trabajo no está limpio (el jar no correspondería a un
   commit).
2. `mvn clean package -DskipTests` desde la raíz del repo.
3. Comprueba la **condición 1**: que el jar lleve exactamente un
   `static/main-*.js` y que sea el que cita `index.html`. Aborta si no.
4. Imprime el tamaño PREVISTO de la carpeta en Windows y si cumple el límite de
   la condición 2.
5. Descarga el JDK portable de Windows a la caché, verificando su sha256.
6. Arma la carpeta de entrega.
7. Comprueba su propia entrega y **aborta si sale incompleta**: `SHA256SUMS` tiene que
   quedar con dos entradas de dos campos, un `.jar` y un `.zip`. Lo mismo que el `.ps1`
   exige al llegar a Windows, pero en la máquina donde se construye: en S153 una entrega
   con una sola huella se dio por buena aquí y el fallo no apareció hasta después de
   transportar 190 MB.

### La caché del JDK

Vive en `~/.cache/educhronos-empaquetado/`, o donde diga `EDUCHRONOS_CACHE`. El
zip se descarga una sola vez y se verifica contra el sha256 fijado en el guion.
Si no coincide, aborta: no es el JDK que fijamos.

### La carpeta de entrega

    <salida>/
      jdk/     el zip del JDK de Windows  (~182 MB) - se copia UNA vez
      build/   jar, empaquetar-windows.ps1, SHA256SUMS, LEEME.txt  (~150 MB)

`jdk/` solo se rellena si falta, porque el JDK no cambia mientras no cambie su
versión fijada. `build/` se rehace en cada construcción.

`empaquetar-windows.ps1` **se versiona en el repo** y el guion de Linux lo copia
a la entrega. Hay una sola fuente y no dos copias que puedan divergir.

---

## 3. Lado Windows

Desde la carpeta `build\`:

    powershell -ExecutionPolicy Bypass -File .\empaquetar-windows.ps1 -Base C:\DES\educhronos-build -HuellaJar <sha256 del jar>

La huella se copia de la CONSOLA de la máquina de Linux: el guion de Linux termina
imprimiendo esta orden entera, con el número ya puesto, bajo el rótulo «Orden para
Windows». No está en la carpeta de entrega y no debe buscarse ahí (§7).

Opciones:

- `-Base <ruta>` — dónde trabajar. **Tiene que ser absoluta**, y el guion aborta
  con código 2 si no lo es. Es el primer parámetro posicional, así que cualquier
  token suelto detrás del guion cae ahí; una ruta relativa se resolvería contra
  el directorio actual y el guion escribiría en un sitio inventado sin avisar.
  Ocurrió en S152, con un `Copy-Item` encadenado en la misma línea. No encadenes
  otra orden detrás del guion.
- `-HuellaJar <sha256>` — **obligatoria**, y el guion aborta con código 2 si falta
  o no son 64 hexadecimales. Es la huella del jar tal como la midió Linux, y llega
  por un canal distinto de la entrega a propósito: ver §7.
- `-SinHumo` — no arranca la aplicación al terminar. La CI lo usa siempre.

Qué hace, en orden:

1. Verifica las dos huellas de `SHA256SUMS`. Aborta si alguna no coincide.
2. Descomprime el JDK y comprueba que trae `jmods`; sin ellos `jpackage` no
   puede montar el runtime.
3. `jpackage --type app-image` con los 14 módulos.
4. Mide la carpeta y dice si cumple la condición 2. **Desde S174, si no cumple, aborta
   con código 1 y no genera el zip**; antes sólo avisaba y terminaba en 0.
5. Comprime el app-image e imprime el **sha256 del zip**, que sale también en el
   resumen final junto al del jar. Esa es la huella que se comprueba en el equipo de
   destino antes de extraer: viaja por la consola o por donde se anuncie la descarga,
   nunca dentro del propio zip.
   Desde S174 lo escribe también en `Educhronos-win.zip.sha256`, junto al zip, en
   formato `sha256sum` (una línea, LF y sin BOM): es el fichero que publica la CI.
6. Prueba de humo, salvo `-SinHumo`.

Cada corrida deja su propia transcripción en `-Base`, con el modo y la fecha en
el nombre, para poder comparar dos corridas. Esa carpeta irá acumulando
ficheros: es deliberado.

Un `.ps1` que llega por descarga suele venir marcado como bloqueado.
`-ExecutionPolicy Bypass` basta en la práctica; si no, `Unblock-File` sobre él.

---

## 3 bis. Cómo se comporta el programa instalado (S154)

El `.exe` no es el `java -jar` de desarrollo: `jpackage` lo arranca con
`--java-options "-Deduchronos.escritorio=true"`, y esa propiedad enciende el **modo
escritorio**. Ese literal es la constante `ModoEscritorio.PROPIEDAD` del código Java y está
en DOS sitios, el `.ps1` y la clase; si dejan de coincidir, el programa arranca como un
servidor mudo sin dar ningún error.

**Al abrirlo se abre el navegador solo**, en `http://127.0.0.1:8080`. Eso es lo que hace
visible que ha arrancado: no hay ventana propia que mirar.

**Si ya está abierto y se vuelve a pinchar**, el segundo no arranca nada: detecta al primero
por un candado de fichero, devuelve al usuario a la pestaña y termina con código 0. Medido en
S154: 0,3 s, sin abrir la base y sin escribir una línea en el log del que está corriendo.

**Se cierra desde el icono de la bandeja, con «Salir»** —el mismo icono tiene «Abrir
Educhronos» para volver a la pestaña—. No hace falta el Administrador de tareas. Si el
escritorio no tiene bandeja (pasa en varios Linux modernos, y en cualquier sesión sin
pantalla), queda dicho en el log y entonces sí hay que parar el proceso.

**Si el puerto 8080 está cogido**, no arranca y lo dice con todas las letras: «No se puede
abrir Educhronos: otro programa está usando el puerto 8080. Cierra ese programa y vuelve a
intentarlo.» Termina con código 1. Es el único diagnóstico que el programa se atreve a dar,
porque es el único sobre el que el usuario puede actuar; cualquier otro fallo remite al log
por su ruta.

**Dónde está el log.** Junto a la base de datos y al candado, en la carpeta de datos del
usuario:

| Sistema | Carpeta | Ficheros |
|---|---|---|
| Windows | `%LOCALAPPDATA%\Educhronos` | `educhronos.db`, `educhronos.log`, `educhronos.lock` |
| Linux | `$XDG_DATA_HOME/educhronos`, o `~/.local/share/educhronos` | los mismos |

El log sólo existe en modo escritorio. En desarrollo (`java -jar`, `mvn spring-boot:run`, el
e2e) la traza sigue yendo a la consola y no se escribe ningún fichero, igual que antes de
S154.

**Sólo escucha en 127.0.0.1.** Desde otro equipo de la red del centro no se entra: la
conexión se rechaza. Hasta S154 escuchaba en todas las interfaces y respondía desde la IP de
la LAN. Para desarrollo hay escotilla: `--server.address=0.0.0.0`.

**Nada de esto está medido en Windows todavía.** Lo de arriba se verificó en Linux, y además
sin pantalla; qué hace el `.exe` con la bandeja, el navegador y el candado en un Windows de
verdad es trabajo del M4 sobre esa máquina.

### Rutas de la SPA (S155)

El jar sirve la interfaz desde `classpath:/static/` con un manejador propio (`RutasSpaConfig` y
`ResolvedorRutasSpa`, en `es.yaroki.educhronos.app.config`). Toda ruta que no sea un fichero, no empiece por
`/api/` y no tenga extensión en su último segmento recibe `index.html`, así que F5 y una URL directa a una vista
funcionan. Una URL que no es ninguna vista la redirige Angular a la portada. `spring.web.resources.add-mappings=false`
hace que ese manejador sea el único de `/**`. El e2e corre contra `ng serve` y NO ejercita este servido;
compruébalo a mano sobre el jar o el bundle: `curl -s -o /dev/null -w '%{http_code} %{content_type}\n'
http://127.0.0.1:8080/horario/1` debe dar `200 text/html`, y `/api/no-existe` debe dar `404`.

---

## 4. Los 14 módulos del runtime

La lista está MEDIDA, no supuesta, y el suelo está cerrado por las dos
direcciones (S152):

    java.base, java.compiler, java.desktop, java.instrument, java.management,
    java.net.http, java.prefs, java.rmi, java.scripting, java.security.jgss,
    java.sql.rowset, jdk.jfr, jdk.unsupported, jdk.zipfs

Trece salieron de `jdeps` sobre las librerías del jar. El decimocuarto no:

- **`jdk.zipfs`** lo exige el cargador nativo de OR-Tools, que resuelve el
  proveedor `jar` por `ServiceLoader` en tiempo de ejecución, invisible al
  análisis estático. Sin él la aplicación arranca, sirve `/api/jornada` y no da
  un solo ERROR; muere al primer `POST /api/horarios` con
  `ProviderNotFoundException`. **Un humo de arranque no lo detecta.**
- **`java.desktop`** lo exige el enlazador de propiedades de Spring Boot, que usa
  `java.beans`. Sin él la aplicación ni arranca: muere en `prepareEnvironment`.
  No es openpdf quien lo hace obligatorio, aunque también lo use.

`jlink` resuelve dependencias y monta 21 módulos a partir de estos 14.

**Si algún día se cambia esta lista, hay que volver a ejercitar el arranque, las
cuatro descargas contra el oráculo y una generación que llegue al solver.**
Construir y arrancar no basta.

---

## 5. Tamaños medidos (S152, commit `a4aa695`)

| Concepto | Bytes | 10^6 |
|---|---|---|
| jar | 156.363.130 | 156,4 MB |
| carpeta del app-image en Windows | **231.691.863** | **231,7 MB** |
| de ella, `runtime\` | 74.879.328 | 74,9 MB |
| de ella, `app\` | 156.363.511 | 156,4 MB |
| `Educhronos.exe` | 449.024 | 0,4 MB |
| zip | 176.010.699 | 176,0 MB |

Límite de la condición 2: 250.000.000 B. **Cumple, con 18,3 MB de margen.**
Tres corridas independientes dieron el mismo número al byte.

Referencia de S151, sin `--add-modules`: carpeta 291.502.931 B, runtime
134.690.396 B. El recorte del runtime es de 59,8 MB, un 44,4 %.

Tiempos: `mvn clean package -DskipTests` entre 21 y 35 s; `jpackage` unos 4 s;
el zip unos 7 s.

### Palanca medida y NO aplicada

El jar lleva los nativos de OR-Tools de **cinco** plataformas, 80.936.474 B en
total. Para un bundle de Windows solo sirve uno, `ortools-win32-x86-64`, que pesa
11.482.754 B, así que sobran **69.453.720 B**: los de `linux-x86-64`,
`linux-aarch64`, `darwin-x86-64` y `darwin-aarch64`. Cuelgan como dependencias
runtime transitivas de `ortools-java` y se podan con `<exclusions>`, sin tocar
código.

La resta va escrita porque depende de la plataforma de destino y es fácil
equivocarse: desde Linux la cifra sería 60.869.367 B, y esa es la que se coló en
la primera redacción de este documento.

No se aplica: la condición 2 se cumple sin ella, y una poda que no cambia el
criterio de terminado no se ejecuta dentro del objetivo (R-terminado). Queda
escrita con su medición por si el bundle vuelve a acercarse al límite. Los
18,3 MB de margen son un 7 %, así que el guion de Linux imprime el tamaño
previsto en cada construcción para que un crecimiento se vea el día que ocurre.

---

## 6. Decisiones tomadas, con su razón

**Los guiones no ejecutan la suite.** Empaquetar y verificar son puertas
distintas del método; mezclarlas daría dos motivos para un mismo fallo y
convertiría un empaquetado de segundos en varios minutos.

**La prueba de humo mide el arranque, y solo eso.** Corre sobre una base vacía,
así que un `POST /api/horarios` se rechaza en la prevalidación con un 422
`CONFIGURACION_INCOMPLETA` sin llegar a tocar el solver. Medido en S152: en
Linux con el rastro del log vacío, que es lo que prueba que el solver no se toca,
y en Windows por el cuerpo de la respuesta. El guion lo clasifica como `NO DISCRIMINANTE` en vez de darlo
por bueno. Lo que ejercita de verdad el cargador nativo es la condición 3, con
el banco.

**El tamaño se mide en bytes de 10^6**, como dice la condición 2, y no en MiB.

**La entrega va en dos carpetas** para no arrastrar los 182 MB del JDK en cada
construcción.

**El `.ps1` lee las huellas de `SHA256SUMS`** en vez de llevarlas inyectadas, de
modo que el fichero del repo y el de la entrega son el mismo.

---

## 7. Límites conocidos

**La construcción no es reproducible bit a bit.** Tres pasadas del mismo commit
dan tres huellas de jar distintas y el mismo tamaño exacto. De las 399 entradas
del jar, 201 llevan la hora de pared de la construcción; solo las 153 del
cargador de Spring Boot van normalizadas. El contenido es el mismo: lo que baila
son las fechas. `project.build.outputTimestamp` es la palanca estándar y no está
declarada en ninguno de los tres `pom`. No se aplica en S152 (R-terminado: la
condición 1 no pide reproducibilidad bit a bit) y además habría que comprobar si
basta, porque antes del empaquetado hay un `npm ci` y un build de Angular.

Consecuencia práctica: `SHA256SUMS` verifica el TRANSPORTE, no identifica una
versión. Si se relanza el guion de Linux, una `build/` ya copiada a Windows deja
de casar aunque no haya cambiado una línea, y el `.ps1` abortará con «alguna
huella no coincide». Es correcto, pero puede despistar. La identidad de versión
la da el commit escrito en `LEEME.txt`.

**Desde S156 la identidad del jar sí se comprueba, y con eso queda saldada
`D-entrega-caducada-indetectable`.** Lo que `SHA256SUMS` no podía hacer es detectar una
entrega CADUCADA: el fichero de huellas viaja DENTRO de la carpeta, así que una entrega de
hace tres construcciones cuadra consigo misma y el `.ps1` la daba por buena. Ahora el `.ps1`
exige `-HuellaJar` y compara el jar contra ella; ese número llega por OTRO canal —la consola
de Linux, copiada a mano— y no está en la carpeta, que es lo único que lo hace independiente.
Si no casa, aborta con «el jar de esta carpeta no es el que construyó Linux». Sigue sin ser
una identidad de versión —dos construcciones del mismo commit dan huellas distintas, y para
eso sigue estando el commit del `LEEME.txt`—: lo que garantiza es que se empaqueta EL jar que
acaba de construirse y no otro.

**En la CI (S174) la huella viaja como salida del job `linux`, no dentro del artefacto**
(decisión D de `O-ci`). Es el mismo canal distinto, sin la copia a mano. Un defecto que la
altera en el traspaso pone el job `windows` en rojo con ese mismo mensaje (medido en S174).

**Nada de lo medido aquí vale para la condición 3.** La máquina de construcción
tiene cuenta de dominio y no es un Windows limpio. Windows 10, sin probar.

**Lo que este procedimiento deja igual que S151**, porque no es su trabajo: F5 da 404
(condición 9). Las otras cuatro que aquí se listaban ya no valen: la base dejó de crearse en
el directorio de trabajo en S153 (condición 7), y en S154 la aplicación pasó a escuchar sólo
en 127.0.0.1 (condición 8), a avisar de que ha arrancado abriendo el navegador y a impedir la
segunda instancia (condición 4), y a cerrarse desde su propio icono de bandeja (condición 5).
Ver §3 bis. Medido en Linux; en Windows, pendiente del M4.

---

## Prueba final en Windows limpio (S156)

La condición 3 de `O-instalación` se verificó así. Repetirla con cada versión que se entregue.

**Máquina y cuenta.** Un Windows 11 sin Java ni Node. Desde una consola de administrador:
`net user educhronos-prueba * /add` crea una cuenta local ESTÁNDAR (sólo «Usuarios»).
Comprobación: `net localgroup Administradores` no la lista, y DENTRO de esa cuenta
`whoami /groups | findstr S-1-5-32-544` sale vacío (en una consola elevada de un
administrador sale el SID: no vale como prueba). `where.exe java` y `where.exe node` no
encuentran nada (en PowerShell, `where` sin `.exe` es otra orden).

**Entrega.** El zip que produce `empaquetar-windows.ps1` viaja por USB; su sha256 lo imprime
el guion. Por USB el fichero no lleva la marca de descarga y Windows no muestra SmartScreen.
Desde S174 el zip es el de la Release del tag, y su sha256 el del asset `.sha256`,
comprobado en Linux con `sha256sum -c` antes de copiarlo al USB.
Se extrae con el Explorador en una carpeta del usuario y se arranca con doble clic: no debe
aparecer ningún diálogo de seguridad ni de credenciales.

**Datos.** Para probar con el centro real, la copia del banco `educhronos-s137.db`
(md5 `dfa4c0774a842d8eb6b7a941e23df2a9`) se coloca como
`%LOCALAPPDATA%\Educhronos\educhronos.db`.

**Oráculo.** Las cuatro descargas (CSV y PDF por grupo, profesor y aula) se llevan a Linux y
se comprueban contra OTRA copia del mismo banco. El horario del banco es el id 1, el único.

    O=scripts/oraculo-exportacion.py
    DB=<copia de educhronos-s137.db>
    python3 $O csv $DB 1 horario-1.csv
    python3 $O pdf $DB 1 horario-1-grupo.pdf    --vista grupo
    python3 $O pdf $DB 1 horario-1-profesor.pdf --vista profesor
    python3 $O pdf $DB 1 horario-1-aula.pdf     --vista aula

Esperado, todo con código 0: CSV «OK: las tres vistas coinciden»; grupo 28 páginas y
«halladas 1285, FALTAN 0, SOBRAN 0»; profesor 59 páginas y 835/0/0; aula 44 páginas, 819/0/0
y «páginas vacías con leyenda: 0». El CSV, además, se abre en Excel con las columnas
separadas y las tildes correctas.

## Construir en la máquina virtual (S163)

Desde S163 el bundle de Windows se construye en una máquina virtual VirtualBox con Windows 11 Pro. El procedimiento de este documento no cambia; sólo cambian el transporte y las cuentas.

1. En Linux: `scripts/empaquetar-linux.sh --salida /home/luis/educhronos-vm/entrega`. `/home/luis/educhronos-vm` es la carpeta compartida con el invitado, que la monta como `E:`.
2. En Windows, con la cuenta de administrador y PowerShell elevada: `Copy-Item -Recurse E:\entrega C:\DES\entrega`, `cd C:\DES\entrega\build` y la orden que imprime el paso 1, con su `-HuellaJar`. Si `C:\DES\entrega` ya existe, bórrala antes, o `Copy-Item` anidará la copia.
3. Copia `Educhronos-win.zip` y la transcripción a `E:\bundle\`.
4. La prueba se hace con una cuenta estándar, copiando el zip a disco local. Ni el bundle ni la base se ejecutan desde `E:`.

La prueba de humo escribe `%LOCALAPPDATA%\Educhronos` en la cuenta que construye; construir con el administrador deja limpia la carpeta de datos de la cuenta de prueba.
