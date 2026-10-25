# Acta de la condición 7 de O-actividad-por-nivel (S252) — Educhronos 0.6.1

- **Versión:** `v0.6.1`, tag anotado sobre `b017ca7` (`tests` n.º 136 y `bundle` n.º 20 en verde). Release no prerelease, con `Educhronos-win.zip` (176 222 380 B, sha256 `acf5f8bb60966d21336cc4a9537d02256da30da443408074f1ba1399f20bdd03`, comprobado con `sha256sum -c` en el anfitrión y en el USB).
- **Versión sustituida:** `v0.6.0`, tag anotado sobre `a607e60` (`tests` n.º 135 y `bundle` n.º 19 en verde). Su Release sigue publicada, con una nota que dice que la sustituye `v0.6.1` (decisión FK).
- **JAR de las capturas:** `app-0.6.1.jar` (156 619 355 B, sha256 `c199e9a179d060e89688588152ad8af413e8e373e196b7939e1c7016b0f0295d`), extraído de ese zip con `zipfile`.
- **Base de partida:** `s241/f1/base-partida/base-partida-2025-2026.db`, md5 `d867c2dec4a9ab01357f539d8b09f7e2` (acta de S241).
- **Código:** `7d89888` y `b017ca7` (la ventana de confirmar la generación), `c86eee8` (recorrido de capturas) y `4e188a5` (manual y 24 capturas).
- **Material:** `/home/luis/educhronos-aceptacion/s252/`: `t0` a `t0f`, `f1`, `f2`, `f3`, `f2b` y `v1`, cada una con su volcado.

## El criterio

Texto aprobado por el usuario en S249 (decisión EU), citado tal cual:

> 7. **Versión (ET).** `v0.6.0`, construida por CI desde su tag; el manual al día, con su sección 7 y las capturas comprobadas contra la versión (`tools/manual`); en la VM, con una cuenta estándar nueva, `Instalar.cmd` coloca la base de partida intacta (md5 `d867c2dec4a9ab01357f539d8b09f7e2`) y, al abrirla, migra y muestra los niveles; con acta. No bloquea la condición 8 de `O-uso-centro`: si la fecha llega antes, el centro recibe `v0.5.0`.

Lectura del usuario (decisión FI): se cumple con `v0.6.1`, la versión del objetivo construida por CI desde su tag; las capturas, el manual y la VM se hacen con ella.

## Por partes

**Construida por CI desde su tag — cumplida.** El tag `v0.6.1` sobre `b017ca7` lanzó `bundle` n.º 20, con sus tres jobs en verde (linux, windows y publicar); el commit tenía `tests` n.º 136 en verde, con sus cuatro jobs. El zip y su `.sha256` se descargaron de la Release y `sha256sum -c` dio OK (`s252/f3`).

**El manual al día — cumplido** (`s252/f2b`).
- Sección 7, «Clases»: el nivel en el código de cada asignatura, la búsqueda de Latín con una asignatura por nivel, el filtro por tipo, el nivel al editar, «Mostrar todos los niveles» y una subsección nueva para crear una clase.
- Sección 8: los tres avisos de nivel, los dos que trae la base de partida y que también salen en la ventana de confirmar la generación.
- La versión 0.6.1 en la portada, en la barra y en el nombre del PDF.
- `tools/manual/capturas.mjs` con `app-0.6.1.jar`: 186 de 186 citas en la corrida de comprobación y en la definitiva; las 24 capturas, rehechas.
- `Educhronos-manual-0.6.1.pdf`: 32 páginas, 2 025 180 B, sha256 `6c3a27a4c539a6c5f75259aa277eee33a34ee1829b23e95e4c410ecd19f06ba8`; la página 1 dice «versión 0.6.1 · 25 de octubre de 2026». Está fuera del repo, en `s252/f2b/`.

**En la VM — cumplido** (`s252/v1`).
- Instantánea de partida: `s243-v050-manual`, restaurada (decisión FH).
- Cuenta: `inst252`, nueva y estándar: sin S-1-5-32-544 en `whoami /groups` y con nivel obligatorio medio, S-1-16-8192 (`v1/vm/grupos.txt`).
- USB: ESD-USB como F:, con `EDUCHRONOS-S252\` (zip y `.sha256`, `base-partida\` y PDF); en el anfitrión, la suma coincide para el zip y la base.
- Instalación: zip copiado a Documentos, «Extraer todo», `base-partida` junto a `Instalar.cmd` y doble clic. La ventana dijo sus seis líneas, hasta «Educhronos instalado en C:\Users\inst252\AppData\Local\Programs\Educhronos.».
- Antes de abrir: md5 de la base instalada `D867C2DEC4A9AB01357F539D8B09F7E2` (`v1/vm/md5-instalada.txt`); en la carpeta de datos, solo `educhronos.db`, de 249 856 B (`v1/vm/datos-antes.txt`).
- Primer arranque: la bienvenida de Edge, pasada como dice el anexo del manual; la portada con «versión 0.6.1» y «2025/2026».
- Migración: el log dice `versionInicial=0 versionFinal=9` (`v1/vm/esquema.txt`).
- Niveles: en «Configuración» › «Asignaturas», la columna Nivel; buscar Latín da 3 de 141: `LAT-4ESO` (4ESO), `Lat2-2BACH` (2BACH) y `Latín-1BACH` (1BACH).
- «Nueva actividad» enseña «Nivel» y «Elige primero el nivel.»; se canceló sin guardar y siguen 219 actividades.
- Cierre desde la bandeja con «Salir»; el icono desapareció.
- Instantánea final: `s252-v061`; se conserva `s243-v050-manual`.

**Con acta — esta.**

## El defecto que apareció por el camino

- La corrida de capturas con `v0.6.0` no podía pulsar «Cancelar» en la ventana de confirmar la generación (`s252/f2`).
- Medido en `s252/t0e`: con los dos avisos de asignaturas repetidas de la base de partida, la ventana medía 832 px. A 1366x768 y a 1366x657 sus dos botones quedaban fuera de la pantalla, sin nada que desplazar, ni con la rueda ni con Tab; solo Escape la cerraba. El bloque de avisos existe desde `875ea3f` (S203); con `v0.5.0` la base no daba ningún aviso y no se veía.
- Arreglo (decisiones FI y FJ): la ventana no pasa del 80 % del alto de la pantalla, el contenido se desplaza y los botones quedan pegados abajo (`7d89888`); el relleno inferior pasa al pie, para que no asome texto bajo los botones (`b017ca7`).
- Verificación (`s252/f3`), con un jar construido en local, a 1920x1080, 1366x768 y 1366x657: los botones se ven al abrir y tras la rueda, la ventana se desplaza (scrollTop 207 y 296) y Escape la cierra; el hueco bajo los botones pasa de 16 px a 0. El jar de `v0.6.0` no pasa, y es el control negativo.
- Queda como mejora: el texto de cada error se lee apretado (decisión FL, `D-dialogo-generar-errores-apretados`).

## Sin comprobar, y por qué

- La ventana pequeña de arranque no se capturó en esta corrida. El usuario la vio bien (decisión FM), y ya se verificó con capturas en S242.

## Desviaciones

- Al entrar por primera vez con `inst252`, Windows hizo preguntas de privacidad que en S242 no constan.
- La raíz del USB sigue con el zip y el PDF de `0.5.0` de S242: lo que lleve el USB del centro se decide en la condición 8 de `O-uso-centro`.
- La primera captura de la VM se llama `c00-net-user.png`, porque se tomó tras crear la cuenta.
