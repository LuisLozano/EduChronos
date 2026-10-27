# Acta de O-prueba-vm (S256) — Educhronos 0.6.1 probado en la VM con el script

- **Versión probada:** `v0.6.1`, la de `docs/actas-aceptacion/acta-version-s252.md`: `Educhronos-win.zip` de 176 222 380 B, sha256 `acf5f8bb60966d21336cc4a9537d02256da30da443408074f1ba1399f20bdd03`, de `s252/f3/v061/`.
- **Base de partida:** `s241/f1/base-partida/base-partida-2025-2026.db`, md5 `d867c2dec4a9ab01357f539d8b09f7e2`, 249 856 B (acta de S241).
- **Script:** `tools/prueba-vm/prueba_vm.py` en `21cd952`. El de la corrida que cuenta es idéntico al del commit: mismo sha256 (`e0e211b3…`) que `git show 21cd952:tools/prueba-vm/prueba_vm.py`, y su última escritura es anterior al commit y a la primera orden de la corrida.
- **VM:** `Win11`, instantánea `s254-cuenta-prueba`, cuenta estándar `prueba-vm`. En el anfitrión, VirtualBox 7.2.22, instalado el 27 de octubre a las 07:27, antes de las tres corridas; las medidas de S254 y S255 se hicieron con 7.2.18 (`s256/cierre/c0/p6-version-vbox.txt`).
- **Código de la sesión:** `bffc815` (E41: el rc 32 da FALLO), `5beebed` (E51, retirado), `21cd952` (E52: el estado de la VM se juzga con el `.vbox`) y `d462e3f` (`docs/tecnica/probar-vm.md`).
- **Material:** `/home/luis/educhronos-aceptacion/s256/`: `t0`, `e41`, `e51`, `e52`, `v1`, `v2` y `v3` con sus consolas, `diag`, `diag2`, `v1-capturas` y `pasos-humanos.txt`.

## El criterio

Texto aprobado por el usuario en S254 (decisión FY), con las condiciones 1, 2 y 5 según GB, citado tal cual:

> 1. **Instrumento.** `tools/prueba-vm/` tiene un script versionado, con tests y autoprueba, que prueba una versión en la VM `Win11` desde el anfitrión a partir de una instantánea dada: restaura, arranca, espera al invitado, copia el zip y la base, comprueba sus sumas, extrae e instala, mide la base instalada y la carpeta de datos y, tras la parte humana, lee el log y apaga. Hace él los pasos A y B de `s252/v1`, y usa la cuenta estándar de la instantánea, que nunca ha iniciado sesión: la restauración la deja sin estrenar en cada prueba.
> 2. **Credenciales.** Las contraseñas viven en un fichero 600 del anfitrión, fuera del repo, de la carpeta compartida y del material; el script solo pasa su ruta, y ninguna salida del script ni del material las contiene, lo que comprueba un test.
> 3. **Salida.** Cada prueba deja su material con manifiesto y un informe con una línea OK o FALLO por comprobación: sumas del zip y de la base, md5 de la base instalada, carpeta de datos, cuenta sin S-1-5-32-544 y con S-1-16-8192, línea de migración del log y proceso terminado tras Salir en la bandeja.
> 4. **Pasos humanos.** La prueba que recibe el usuario lleva solo los pasos de mirar o interactuar, de uno en uno (norma 17 de M-guion): como mucho 10, frente a los 20 de `s252/v1`.
> 5. **Equivalencia.** La prueba de `v0.6.1`, repetida con el script desde `s254-cuenta-prueba`, da en sus comprobaciones lo que dice `docs/actas-aceptacion/acta-version-s252.md`: md5 `D867C2DE…`, 249 856 B, `versionInicial=0 versionFinal=9` y la cuenta estándar. La autoprueba falla con una suma o una base equivocadas.
> 6. **La VM no pierde nada.** Ninguna instantánea se toma ni se borra sin decisión del usuario, y al terminar la VM queda apagada.
> 7. **Documentación.** `docs/tecnica/` explica cómo se prueba una versión en la VM con el script; hoy ninguna línea dice cómo se prueba una versión en la VM, y solo `docs/tecnica/decisiones.md:86` la nombra (`s254/t0`, 4.3). Salda `D-prueba-vm-a-mano`.

El usuario declaró el objetivo en 6 de 7 tras la tercera corrida (decisión GX) y TERMINADO, 7 de 7, tras la condición 7 (decisión GY).

## Las tres corridas

| Corrida | Código | Pasos de la persona | Informe | Qué enseñó |
|---|---|---|---|---|
| `v1` | `bffc815` | 9: los tres del script y los de mirar de `s252/v1` (GR) | 23 OK, 1 FALLO | `vm-restaurada` da FALLO aunque la VM está bien: `showvminfo` sigue con la hora del restore |
| `v2` | `5beebed` | 3 (GT) | 23 OK, 1 FALLO | releer 30 s no basta: la hora sigue igual mientras vive el mismo VBoxSVC, y un VBoxSVC nuevo da la de la instantánea (`diag2/notas.txt`) |
| `v3` | `21cd952` | 3 (GU) | 24 OK, 0 FALLO | con el `.vbox`, las tres comprobaciones de la VM dan OK |

## Por partes

**1. Instrumento — cumplida** (`v3`). `preparar` comprobó la VM y el anfitrión, restauró, arrancó, esperó al invitado (intento 1), midió la cuenta, copió el zip y la base, comprobó sus sumas en la VM, extrajo, instaló (6 de 6 líneas) y midió la base instalada y la carpeta de datos. `sesion` vio la sesión de `prueba-vm`; `abierta` leyó la línea de migración y dos procesos `Educhronos.exe`; `cerrar`, tras Salir, vio cero procesos, apagó y restauró. 55 tests y autoprueba de 12 capacidades (`s256/e52`).

**2. Credenciales — cumplida.** El fichero de contraseñas es un fichero 600 del usuario fuera del repo, de la carpeta compartida y del material, y el script lo comprueba antes de tocar la VM (`clave-fuera` OK en las tres corridas). Solo viaja su ruta, y los tests de `CredencialesTest` comprueban que ni la salida ni el material contienen la contraseña falsa.

**3. Salida — cumplida** (`v3/informe.txt`). Una línea OK, FALLO o DATO por comprobación, 24 OK en `v3`; `MANIFIESTO.sha256` del material, con `sha256sum -c` rc 0. Las comprobaciones que nombra la condición están todas: `suma-zip`, `suma-base`, `md5-instalada`, `carpeta-datos`, `cuenta`, `migracion` y `proceso-terminado`.

**4. Pasos humanos — cumplida**, con 9 (`v1`, `pasos-humanos.txt`). Contados como en `s252/v1`, donde las pantallas del primer inicio de Windows y la bienvenida del navegador cuentan dentro de su paso (decisión GW). Contando cada pantalla por separado salen 16: los 9, las 3 de Windows y las 4 de Edge.
1. Entrar con `prueba-vm` (con las tres pantallas de Windows: ubicación, entrada manuscrita y ofertas, las tres con No).
2. Doble clic en el acceso directo: «Educhronos se está iniciando» y la bienvenida de Edge, pasada con el anexo del manual (cuatro pantallas).
3. La portada: «versión 0.6.1» y «2025/2026».
4. «Configuración» › «Asignaturas», buscar Latín: la columna Nivel y 3 de 141, `LAT-4ESO` (4ESO), `Lat2-2BACH` (2BACH) y `Latín-1BACH` (1BACH).
5. «Nueva actividad», sin rellenar: el campo Nivel y 219 actividades detrás.
6. Volver a abrir «Nueva actividad», añadido por la discrepancia del paso 5 (ver Desviaciones): la captura `v1-capturas/p5b-nueva-actividad.png` enseña «Elige primero el nivel.».
7. «Cancelar» sin guardar: siguen 219 actividades.
8. Clic derecho en el icono de la bandeja: «Abrir Educhronos» y «Salir» (captura `v1-capturas/p7-menu-bandeja.png`).
9. «Salir»: el icono desaparece.

**5. Equivalencia — cumplida** (`v3`, como en `v1`). md5 de la base instalada `D867C2DEC4A9AB01357F539D8B09F7E2`; en la carpeta de datos, solo `educhronos.db`, de 249 856 B; el log dice `versionInicial=0 versionFinal=9`; la cuenta, sin S-1-5-32-544 y con S-1-16-8192. Es lo que dice el acta de S252. La autoprueba da FALLO con una suma del zip, una base o un md5 instalado equivocados (capacidades a, b y k).

**6. La VM no pierde nada — cumplida.** En las tres corridas, `snapshot list --details` al final es idéntico al del principio, y la VM acaba apagada en `s254-cuenta-prueba`. No se tomó ni se borró ninguna instantánea.

**7. Documentación — cumplida.** `docs/tecnica/probar-vm.md` (`d462e3f`) explica cómo se prueba una versión en la VM con el script, y el README de `docs/tecnica/` lo pone en su orden de lectura. La CI de ese commit, `tests` n.º 142, terminó en verde con sus cuatro jobs, también «rutas de docs/tecnica» (`s256/cierre/c0/p2-jobs.txt`).

**Con acta — esta.**

## Lo que apareció por el camino

- **La hora de `showvminfo` tras restaurar.** Con VirtualBox 7.2.22, `showvminfo` da la hora del restore mientras vive el VBoxSVC que lo hizo; el `.vbox`, escrito en el mismo segundo, ya tiene la hora de la instantánea y `currentStateModified="false"` (`diag2/notas.txt`). El primer diagnóstico, que la hora volvía «después», era falso: la lectura buena la había contestado un VBoxSVC nuevo. Costó una corrida y E51.
- **La lectura de S255.** En S255, con 7.2.18, la lectura que dio «IGUAL» tras el restore (`s255/t1/p9-comparacion.txt`) fue 6 s después y la contestó un VBoxSVC nuevo, porque el que restauró se había cerrado un segundo antes: es lo mismo que con 7.2.22, y no hay indicio de que la diferencia sea de versión (`s256/cierre/c0/p7-vboxsvc-s255.txt`).
- **El `.vbox` tras apagar** no lleva el atributo `currentStateModified` (`v3/ordenes/026-vbox-tras-apagar.txt`); `regla-detecta-cambio` lo anota como DATO.

## Sin comprobar, y por qué

- El material de las corridas no se ha buscado contra la contraseña real: el fichero de contraseñas no se lee nunca. Lo cubren el test con una contraseña falsa y que el script solo pase la ruta.
- El `git status` del momento de la tercera corrida no está en el material; el más cercano es el vacío que salió con el commit `21cd952`, justo antes de lanzar `preparar`.

## Desviaciones

- En el paso 5 el usuario respondió que «Elige primero el nivel.» no se mostraba. Una captura tomada después ya no tenía el diálogo abierto; al volver a abrirlo, la captura enseña el aviso en el panel de la plaza 1, como en S252. Se anotan las dos cosas; el aviso es texto gris pequeño y no se vio a simple vista. Queda como mejora: `D-nota-nivel-poco-visible`.
- La bienvenida del navegador, prevista como paso propio, el usuario la pasó dentro del paso 2; el paso de la portada ocupó su lugar.
