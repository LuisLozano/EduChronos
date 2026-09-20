# Empaquetado y distribución

Cómo se construye el bundle de escritorio de Educhronos. Escrito en S152 dentro
de `C-construccion-reproducible`, sobre medición y no sobre estimación.

**Qué cubre:** la CONSTRUCCIÓN. Qué produce cada máquina, qué ficheros necesita
y cómo se invoca.

**Qué NO cubre:** cómo llega el programa al usuario final. El medio de entrega y
la firma del ejecutable siguen sin decidir; son trabajo de la condición 3 de
`O-instalación`. Lo que hoy existe es una carpeta y su zip, no un instalador.

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

El traspaso entre las dos es manual: el usuario tira la carpeta desde Windows.
El guion de Linux no conoce la dirección de la máquina Windows.

---

## 2. Lado Linux

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

    powershell -ExecutionPolicy Bypass -File .\empaquetar-windows.ps1 -Base C:\DES\educhronos-build

Opciones:

- `-Base <ruta>` — dónde trabajar. **Tiene que ser absoluta**, y el guion aborta
  con código 2 si no lo es. Es el primer parámetro posicional, así que cualquier
  token suelto detrás del guion cae ahí; una ruta relativa se resolvería contra
  el directorio actual y el guion escribiría en un sitio inventado sin avisar.
  Ocurrió en S152, con un `Copy-Item` encadenado en la misma línea. No encadenes
  otra orden detrás del guion.
- `-SinHumo` — no arranca la aplicación al terminar.

Qué hace, en orden:

1. Verifica las dos huellas de `SHA256SUMS`. Aborta si alguna no coincide.
2. Descomprime el JDK y comprueba que trae `jmods`; sin ellos `jpackage` no
   puede montar el runtime.
3. `jpackage --type app-image` con los 14 módulos.
4. Mide la carpeta y dice si cumple la condición 2.
5. Comprime el app-image.
6. Prueba de humo, salvo `-SinHumo`.

Cada corrida deja su propia transcripción en `-Base`, con el modo y la fecha en
el nombre, para poder comparar dos corridas. Esa carpeta irá acumulando
ficheros: es deliberado.

Un `.ps1` que llega por descarga suele venir marcado como bloqueado.
`-ExecutionPolicy Bypass` basta en la práctica; si no, `Unblock-File` sobre él.

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

**Nada de lo medido aquí vale para la condición 3.** La máquina de construcción
tiene cuenta de dominio y no es un Windows limpio. Windows 10, sin probar.

**Lo que este procedimiento deja igual que S151**, porque no es su trabajo: la
base se crea en el directorio de trabajo (condición 7), la aplicación escucha en
todas las interfaces (condición 8), no avisa de que ha arrancado (condición 4),
no se cierra desde ella misma (condición 5) y F5 da 404 (condición 9).
