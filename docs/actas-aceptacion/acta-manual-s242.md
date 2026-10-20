# Acta de O-manual (S242) — el manual de uso de Educhronos 0.5.0

- **Versión:** `v0.5.0`, tag anotado sobre `c2c157f` (el mismo código que `v0.5.0-rc.4`; `tests` n.º 115 y `bundle` n.º 17 en verde). Release no prerelease, con `Educhronos-win.zip` (176 208 048 B, sha256 `34df89a36713a9a01de1c020cf4115367d2a134dd0544cf5011b7666e5d4ec7e`, comprobado con `sha256sum -c` en el host y en el USB).
- **JAR de las capturas:** `app-0.5.0.jar` (156 605 222 B), extraído de ese zip con `zipfile`, porque el zip guarda las rutas con «\» (`s242/f2/v050/extraido/Educhronos/app/`).
- **Base de partida:** `s241/f1/base-partida/base-partida-2025-2026.db`, md5 `d867c2dec4a9ab01357f539d8b09f7e2` (acta de S241).
- **Código:** `eac23f0` (script de capturas, generador del PDF y hoja de estilo), `884e4db` (manual y 24 capturas), `1343b64` (guía de incidencias como última sección) `cf6d2ad` (capturas de Windows, aviso sobre el consejo del 409, anexo del navegador y correcciones de la prueba en la VM) y el commit de F3.9 (lista de lo que lleva el USB).
- **Material:** `/home/luis/educhronos-aceptacion/s242/` (`f1`, `f2` y `f3`, cada una con su `volcado.txt`).

## Criterio, condición por condición

Texto aprobado por el usuario en S242 (decisión CX), citado tal cual:

> O-manual termina cuando:
> 1. Por tareas. docs/manual/ (Markdown) tiene una sección por tarea de esta lista cerrada: instalar y actualizar; abrir, cerrar y ver la versión; preparar el curso nuevo; cambiar profesores (alta, baja en dos pasos, tutoría, disponibilidad y guardias); cambiar las horas de una clase y encontrarla; comprobar antes de generar; generar; ver y ajustar el horario; sacar los cuatro horarios (tres PDF y el CSV). Cada sección indica dónde se hace, con los rótulos literales, los pasos y qué se ve al terminar.
> 2. Fiel a la versión. Las capturas salen de un script versionado que arranca el jar de la versión entregada sobre una copia de la base de partida, y que falla si falta en pantalla algún texto literal citado. Su autoprueba demuestra que lo detecta.
> 3. Avisa de lo que no tiene vuelta. Antes de generar: que congela actividades y jornada, y la salida si F1 la mide. Además: la baja de un profesor en dos pasos, que los cursos no se borran y dónde está cada una de las cuatro descargas.
> 4. Instalación probada. El usuario sigue el capítulo de instalación tal cual en la VM, desde una instantánea, con una cuenta estándar nueva y el zip entregado. Las capturas de Windows se toman con VBoxManage durante esa misma corrida. Acta con versión y md5 de la base instalada.
> 5. Entregable. Un PDF generado con una orden versionada a partir del Markdown, con la versión en la portada y su huella en el acta. Viaja en el USB y va impreso.

**1. Por tareas — cumplida.** `docs/manual/manual.md` tiene una sección por tarea, en el orden de la lista: 2 (instalar y actualizar), 3 (abrir, cerrar y ver la versión), 5 (preparar el curso nuevo), 6 (profesores: alta, datos y guardias, disponibilidad, tutor y baja en dos pasos), 7 (clases: encontrarlas por grupo, asignatura o profesor, y cambiar repeticiones, profesor o aula), 8 (el panel de pre-validación), 9 (generar), 10 (ver y ajustar) y 12 (sacar los horarios). Cada sección lleva «Dónde se hace», los pasos numerados y «Qué ves al terminar». Nota del usuario: el manual tiene además la sección 1 («Antes de empezar»), la 4 (cómo está organizada la aplicación), la 11 (cambiar clases después de generar), un anexo sobre la primera vez que se abre el navegador (D3, abajo) y, como última, la guía de incidencias (decisión CW, condición 7 de O-uso-centro). La aplicación ofrece cinco descargas, no cuatro (también «PDF guardias»); el manual las cubre todas.

**2. Fiel a la versión — cumplida.** `tools/manual/capturas.mjs`:
- Copia la base de partida a una carpeta de datos nueva y comprueba su md5.
- Arranca `app-0.5.0.jar` con `java -jar` y `XDG_DATA_HOME`, sin modo escritorio, y aborta si `/api/version` no es `0.5.0`.
- Recorre 24 pantallas a 1366x768, esperando a un texto concreto en cada paso.
- Comprueba cada cita «…» del manual contra el texto acumulado. Sale con rc=1 si falta alguna.
- Ejecución de referencia de F3 (`s242/f3/volcado.txt`, F3.6): **172 de 172 citas encontradas, rc=0**.
- Autoprueba: «Botón inexistente 9.9.9» y «versión 9.9.9» dan ausentes y «versión 0.5.0» presente; si no, rc=2.
- En F2.4 la primera vuelta dio rc=1, con 6 citas del diálogo de generar. Era una carrera del script (esperaba a textos fijos y no a los enlazados), corregida en el script sin cambiar ninguna cita.

**Sin comprobar contra la aplicación, y por qué:**
- Los textos de Windows, la ventana de arranque, la bandeja, `Instalar.cmd` y la bienvenida del navegador no están en la página. Van entre las marcas `sin-comprobar` (22 citas) y se cotejaron con las capturas de Windows de la condición 4: 13 vistas en ellas; 9 no visibles en ninguna («Extraer todo» y «Extraer», confirmados de palabra por el usuario; los avisos de error de `Instalar.cmd`, que la corrida no provoca; y el aviso del puerto 8080).
- La ventana de generar y los avisos al mover una clase se cuentan sin cita: el recorrido no genera (tarda minutos) ni arrastra.

**3. Avisa de lo que no tiene vuelta — cumplida.**
- La sección 1 enumera los cuatro avisos, cada uno con su sección: generar fija las clases y la jornada; duplicar deja el curso de partida en solo lectura; los cursos no se borran; la baja de un profesor con clases son dos pasos.
- La sección 9 lleva el aviso antes de generar, con el rechazo real «No se puede editar: referenciada por 3 sesion(es)» (captura 06).
- Desde F3.0, la sección 9 dice además que el consejo con que termina ese aviso («Borra el horario generado y los bloqueos que dependen de ella antes de editarla.») no se puede seguir, porque Educhronos no tiene forma de borrar un horario, y remite a la sección 11.
- La salida está medida en F1.5: PUT de LAT-4ºC con el mismo cuerpo, 409 antes de duplicar y 200 en la copia. La sección 11 la explica, con su coste: el curso de partida queda archivado y no se borra.
- La sección 12 dice dónde quedan las cinco descargas: la carpeta de descargas del navegador.

**4. Instalación probada — cumplida, con las desviaciones de abajo.** Detalle en la sección «Verificación en la VM».

**5. Entregable — cumplida en lo que toca a esta sesión.**
- `tools/manual/generar-pdf.sh` (pandoc + `google-chrome --headless`) genera `Educhronos-manual-0.5.0.pdf`: 31 páginas A4, 1 939 835 B, sha256 `6635b4b2972815f74437878f1b48641e9217c75fe78f06a7f3cadbc37c9426ce`. En la portada pone «versión 0.5.0». Es el de F3.9: sustituye al de F3.7 (30 páginas, `6bafd5d6…6861c5811490`), que no llevaba la lista de lo que trae el USB.
- Está fuera del repo, en `/home/luis/educhronos-aceptacion/s242/f3/`.
- Viaja en el USB del centro (ver «El USB que va al centro», abajo). La impresión la hace el usuario.

## Verificación en la VM (F3, `s242/f3`)

- **Instantánea de partida:** `s239-prueba-hecha`, restaurada con permiso del usuario (se perdió la cuenta `inst241` de S241, que no tenía instantánea, CQ).
- **Cuenta:** `inst242`, nueva, creada con `net user inst242 /add`, sin contraseña. Es estándar: sin S-1-5-32-544 en `whoami /groups`, con «BUILTIN\Usuarios» y nivel obligatorio medio, S-1-16-8192 (`grupos.txt`).
- **USB:** ESD-USB, conectado a la VM como **F:** (Removable, NTFS), con `EDUCHRONOS-S242\` (zip y `.sha256`, `base-partida\` con la base y su `.md5`, PDF provisional). En el anfitrión, antes de llevarlo a la VM, «La suma coincide» para el zip y para la base.
- **Instalación:** el usuario siguió la sección 2 del PDF provisional, paso a paso:
  - Zip copiado a Documentos y extraído con «Extraer todo» / «Extraer».
  - `base-partida` copiada junto a `Instalar.cmd` (w01).
  - Doble clic en `Instalar.cmd`, sin avisos de Windows.
  - La ventana dijo, en orden: «[1/3] Programa en C:\Users\inst242\AppData\Local\Programs\Educhronos», «[2/3] Acceso directo en el Escritorio», «[3/3] Base de partida», «Base de partida copiada a C:\Users\inst242\AppData\Local\Educhronos\educhronos.db», «Educhronos instalado en …» y «Abre Educhronos con el acceso directo del Escritorio.» (w02).
  - Al pulsar una tecla se cerró, y en el Escritorio quedó el acceso directo `Educhronos`.
- **Oráculo antes de abrir:** md5 de la base instalada `D867C2DEC4A9AB01357F539D8B09F7E2` (`md5-instalada.txt`); en la carpeta de datos, un único fichero, `educhronos.db`, de 249 856 B (`datos-antes.txt`).
- **Primer arranque:**
  - Doble clic en el acceso directo. Ventana «Educhronos se está iniciando…» / «El navegador se abrirá en unos segundos.» (w04, de una captura por segundo).
  - Edge se abrió en `127.0.0.1:8080` con su bienvenida por encima (D3).
  - Pasada la bienvenida, la portada enseña «versión 0.5.0» bajo la marca y «2025/2026» en la barra (w05).
  - Log: `Esquema preparado ruta=C:\Users\inst242\AppData\Local\Educhronos\educhronos.db versionInicial=0 versionFinal=8 baseVacia=false` (`esquema.txt`).
- **Cierre:** icono de la bandeja → menú con «Abrir Educhronos» y «Salir» (w06) → «Salir». El icono desapareció (w07).
- **Capturas:** con `VBoxManage controlvm Win11 screenshotpng`, durante la misma corrida (`s242/f3/capturas-windows/`). Al manual van w01, w02 y w06, recortadas a su ventana.
- **Actualización:** no se repite aquí; está verificada en S237 (condición 2 de O-uso-centro).

## El USB que va al centro

- **Raíz del USB** (ESD-USB, tras la decisión D1):
  - `Educhronos-win.zip` y `Educhronos-win.zip.sha256`: «La suma coincide».
  - `base-partida\`, con `base-partida-2025-2026.db` y su `.md5`: «La suma coincide».
  - `Educhronos-manual-0.5.0.pdf`, sha256 `6635b4b2…7c9426ce`, el mismo de arriba.
- **Lo que no se ha borrado:**
  - El `Educhronos-win.zip` antiguo de la raíz (28/09, sha256 `7afc5b8c…`) y el PDF provisional están en `retirado-S242\`.
  - El usuario agrupó las carpetas de sesiones anteriores en `SesionesAntiguas\`.
  - Qué se deja en el USB antes de entregarlo lo decide el usuario. El manual pide comprobar que, nada más abrir el USB, están los cuatro elementos (zip, `.sha256`, `base-partida` y el PDF) y no usar otras carpetas (F3.9, petición del usuario).

## Desviaciones (F3.4 y F3.5), literales

- **D1** (paso 1). Usuario: «Lo dejé en documentos y estaba en F:\EDUCHRONOS-S242, pero en el manual quizás debería estar en el raiz de F:.» Además, en la raíz del USB había otro `Educhronos-win.zip`, anterior (28/09). Decisión del usuario: el zip, su `.sha256`, `base-partida` y el PDF van en la raíz del USB del centro, y el zip antiguo se retira. El manual dice que están «nada más abrirlo».
- **D2** (paso 2). Usuario: «El manual también debería indicar que el que hay que extraer es el de Documentos.» El paso 2 dice ahora que se extrae «esa copia» del zip, no la del USB.
- **D3** (primer arranque). La bienvenida de Microsoft Edge salió por encima de la portada («Le damos la bienvenida a Microsoft Edge, el explorador recomendado en Windows», con «Comience sin sus datos»). Usuario: «He avanzado hasta configurar Edge completamente. Creo que no debemos incluir esta parte en el manual, quizás como anexo.» Decisión del usuario: un anexo antes de la última sección y una frase en la sección 3.
- **Observaciones** (corregidas en el manual por decisión del usuario):
  - El Explorador enseña el fichero como «Instalar», sin `.cmd`.
  - La primera vez, `base-partida` no se copió antes de lanzar `Instalar.cmd` (w00): el paso 3 dice ahora «Antes del paso 4».
  - El icono de la bandeja estaba entre los iconos ocultos.
  - Tras «Salir», la pestaña del navegador se queda abierta.
- Lista literal completa en `s242/f3/desviaciones.txt`.

## Notas

- Las capturas de la aplicación pintan de gris los nombres completos del profesorado, porque la base de partida es la del centro y las capturas se versionan; el alta y la disponibilidad se capturan sobre un profesor inventado (BYG4 «Profesora Nueva»). El texto que se comprueba no cambia.
- La base de partida da desde el primer momento un ERROR de pre-validación: «Las guardias ordinarias de los profesores suman 0 y hacen falta 120…». La sección 8 lo usa como ejemplo de qué hacer con un error antes de generar.
- PDF provisional seguido en la VM: 28 páginas, sha256 `2404c66576f821eab4542db9cf1de33d5c6d1d8caa0f282f7502b0929ec9a5c8`, sustituido por el definitivo.
