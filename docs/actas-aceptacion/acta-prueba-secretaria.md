# Acta de la prueba corta de la secretaria: `v0.4.0` en la VM `Win11` (`O-prueba-secretaria`, H5)

## Estado

Preparada en S235: condiciones 3 (instantánea) y 4 (consigna). La prueba (condición 5) se hizo el 20/10/2026, en S239: resultado abajo. Desde S235 es
una prueba corta, como la demo de S188: comprueba que la versión que se dejará en el centro no tiene ningún bloqueo. El uso del curso
completo, que es el estado final 2, pasa a `O-uso-centro`.

Revisada en S239, antes de la prueba, por decisión del usuario: el reintento tras un 503 pasa a
10 minutos (CE), la espera de la generación se usa para la conversación final (CF) y el aviso de
traducir de Edge se desactiva el día de la prueba (CG, revoca BJ). La VM se monta unos 15 minutos
antes de que llegue la secretaria.

## Versión e instantánea (condición 3)

- **Versión:** `v0.4.0`, Release normal, tag anotado sobre `971646f`, el mismo commit que `v0.4.0-rc.1` (decisión AU). Construida por
  `bundle` #12. `Educhronos-win.zip`, 176 197 679 B, sha256 `2c08596e8e8ee4f77ca9ad40ec8ea6410bd1771cadf80ff429bf2a740d73e053`.
  Frente a la rc cambian el jar, que pasa a llamarse `app-0.4.0.jar` y lleva dentro la versión nueva (156 595 030 B frente a 156 595 073 B), y la línea de `Educhronos.cfg` que lo apunta.
- **VM:** `s187-demo-lista` con Windows Update hasta estar al día (Windows 26200.9711) da `s235-actualizada`. Sobre ella, la cuenta de
  la secretaria da `s235-prueba-lista` (UUID `14464016-0340-4a1c-aefa-9bf6d6ff892b`), tomada con el cable de red virtual desconectado
  para que Windows y Edge no descarguen nada durante la prueba.
- **Cuenta `secretaria`:** estándar (sin S-1-5-32-544 en su token) y sin contraseña. Instalada por el punto 8 de la guía de
  distribución de `docs/empaquetado.md`: `v0.4.0` en `%LOCALAPPDATA%\Programs\Educhronos`, la base de partida como `educhronos.db`
  (md5 `ba0db78259de85c819fe507cbf0c2496`, esquema 0) y el acceso directo en el Escritorio. Educhronos no se ha lanzado: la carpeta
  de datos tiene un único fichero y ningún log. Edge se abrió una vez para pasar sus pantallas de bienvenida. No hay nada de la base
  de 2026/2027.
- **Verificación previa de `v0.4.0`**, en una cuenta desechable (`verif235`) sobre `s235-actualizada`, borrada al restaurar: el
  punto 8 entero; «versión 0.4.0» bajo la marca y en el log; «Esquema preparado» con `versionInicial=0` y `versionFinal=8`. El
  paso 3, repetido: la guarda corregida en `2a0e6fa` lanza su error, pero, con las órdenes pegadas línea a línea, el `Copy-Item`
  siguiente se ejecuta igual. Envuelto en `& { … }`, el bloque se detiene. Evidencia en el material.

## Lo que verá y no se toca

- **Edge ofrecería traducir la página «de inglés»**, porque `index.html` declara `lang="en"`, y el
  aviso tapa el botón «Cursos…», que es el de la tarea 2. Desde S239 (CG, revoca BJ) la oferta se
  desactiva en Edge antes de que ella se siente (paso 3 de «Antes»): el defecto está corregido en
  la línea `v0.5.0` (`c48134c`), que es la que recibirá el centro, y observarlo no aporta nada.
- **La barra dice «Curso sin nombre»:** la base de partida es anterior al nombre de curso. No se toca, para no abrir la aplicación ni
  cambiar la base (condición 3).
- **La pantalla de inicio de Windows lista otras cuentas**, entre ellas `demo`, con `v0.2.0`. Por eso el observador entra en
  `secretaria` antes de que ella se siente.

## Procedimiento del día

Antes, unos 15 minutos antes de que llegue ella. Los pasos 1 y 2, en el anfitrión (Claude Code):

1. Inhibidor de suspensión. El PID que vale es el que da `systemd-inhibit --list`, no `$!`, que es el de `setsid`:
```
setsid nohup systemd-inhibit --what=sleep:idle:handle-lid-switch --who=educhronos --why='Prueba secretaria' --mode=block sleep infinity
```
2. Con la VM apagada:
```
VBoxManage snapshot Win11 restore s235-prueba-lista
VBoxManage startvm Win11 --type gui
```
3. El observador entra en `secretaria`, abre Edge, va a `edge://settings/languages`, desactiva la
   opción de ofrecer traducir las páginas que no están en un idioma que lee y cierra Edge (CG,
   S239). No se abre Educhronos. Si la opción no aparece, se sigue sin desactivarla y se anota.
4. El observador deja el Escritorio a la vista. Ella empieza ahí.

Durante: la consigna y las reglas de abajo.

Después, ya sin la secretaria:

1. Cerrar Educhronos desde el icono de la bandeja.
2. En una PowerShell de `secretaria`, copiar la carpeta de datos a la carpeta compartida, que funciona sin red.
   `E:\prueba-secretaria` no debe existir antes:
```
Copy-Item "$env:LOCALAPPDATA\Educhronos" E:\prueba-secretaria -Recurse
```
3. Apagar Windows. Claude Code toma una instantánea de la VM tal como quedó y para el inhibidor por su PID.
4. Claude Code comprueba sobre la copia lo de «Qué se comprueba después».

## Consigna (condición 4)

Se lee entera antes de empezar:

> El curso actual del centro es el 2025/2026 y ya está cargado en Educhronos. Haz las tareas en orden. Piensa en voz alta. Si algo no
> se entiende, pregunta.
>
> 1. Abre Educhronos.
> 2. Prepara el curso 2026/2027 a partir del actual, sin perder el 2025/2026.
> 3. BYG1 deja el centro. Sus clases las dará una profesora nueva, con código BYG4 y nombre «Profesora Nueva». BYG1 no debe quedar en
>    el curso 2026/2027.
> 4. En 4ºC, Latín pasa a tener 2 horas a la semana y Geografía e Historia, 4.
> 5. Consigue el horario de 2026/2027 y comprueba en él que BYG4 da las clases que eran de BYG1 y que 4ºC tiene 2 horas de Latín.

Son las tareas 4, 5, 6 y 8 de la demo (`acta-demo-bundle.md`), más abrir la aplicación, para poder compararlas una a una con S188.
Sin ensayo previo, por decisión del usuario.

## Reglas de observación

1. La consigna se lee entera antes de empezar.
2. Ella piensa en voz alta. Quien observa no señala la pantalla ni sugiere.
3. Cada pregunta se anota, con el minuto y la tarea, antes de responderla.
4. Si la pregunta es sobre QUÉ pide la tarea, se contesta y se anota como aclaración, que es un defecto de la consigna. Si es sobre
   CÓMO hacerlo en Educhronos, se anota y solo se contesta pasado el umbral de atasco o si ella lo pide expresamente: es una ayuda,
   incidencia candidata.
5. Umbral de atasco: 5 minutos sin avanzar en una tarea. Entonces se da la menor pista que la desbloquee y se anota literal. Las
   pistas mínimas de la demo («Pistas mínimas (S182)» en `acta-demo-bundle.md`, tareas 4, 5, 6 y 8) están escritas para `v0.2.0` y
   no se han comprobado en `v0.4.0`: sirven de orientación, no de texto fijo.
6. Se anotan el inicio y el final de cada tarea, y de cada generación el tiempo elegido, la duración y el resultado.
7. Si la generación da 503, se le pide un único reintento con 10 minutos, y se anota como
   intervención. Si el reintento también da 503, la prueba con ella termina ahí: se anota, y el
   usuario decide después si se acepta con salvedad o se repite (CE, S239; antes, 30 minutos, un
   tiempo que no se había medido nunca en la VM).
8. Se anota además, sin intervenir: si el aviso de traducir de Edge la detiene o lo acepta; si «Curso sin nombre» le genera alguna
   duda; y cada petición o queja, literal (R-incidencia).
9. Si lo deja antes de terminar, se anota en qué tarea y por qué.
10. Mientras se genera, el observador hace la conversación final sobre las tareas 1 a 4: qué le
    costó y qué esperaba. No toca la pantalla ni anticipa lo que comprueba la tarea 5. Se anota
    (CF, S239).

## Qué se comprueba después

Sobre la copia de su carpeta de datos, sin decírselo antes:

- El horario de 2026/2027 tiene 0 violaciones duras.
- El curso 2025/2026 sigue y no ha cambiado.
- En 2026/2027, BYG4 «Profesora Nueva» tiene las actividades que eran de BYG1, y BYG1 no está.
- En 4ºC, Latín tiene 2 horas y Geografía e Historia, 4.
- Comparación tarea a tarea con la demo de S188, en especial su tarea 5, donde cuatro actividades se borraron sin aviso.

## Resultado

Hecha el 20/10/2026 (S239) desde `s235-prueba-lista`, con `v0.4.0`. La secretaria hizo sola las
cinco tareas, sin preguntas, ayudas ni pistas, y obtuvo a la primera un horario de 2026/2027 con 0
violaciones duras y 0 de guardia. Condición 5 cumplida con salvedad (CI): BYG1 sigue en el curso
2026/2027, sin ninguna referencia. Condición 6 cumplida.

Tiempos, de la hoja del observador y del log de la aplicación:
- Llegada a las 12:30; consigna leída a las 12:45; fin a las 13:30.
- Tarea 1: su arranque empieza en el log a las 12:45:46 y abre el navegador a las 12:45:54
  (Spring, 9,4 s; el proceso, 12,7 s).
- Tarea 2: el curso 2026/2027 queda abierto a las 12:46:38.
- Tareas 3 y 4: de 12:46:38 a 12:54:31.
- Tarea 5: ella elige 20 minutos (1 200 s); generación de 12:54:31 a 13:14:39, FEASIBLE a la
  primera, objetivo 198, 819 sesiones, 1 226 s. Comprobación en pantalla hasta las 13:30. No hubo
  reintento.

Comprobaciones sobre la copia de su carpeta de datos (`s239/d4` y `s239/d5`):
- 2025/2026: 21 de las 22 tablas, idénticas a la base de partida; `curso` gana la fila del curso
  archivado «2025/2026». Las columnas añadidas por la migración, con su valor por defecto.
- BYG1 → BYG4: las 4 actividades de BYG1 (Biología y Geología de 1ºA a 1ºD) pasan a BYG4
  «Profesora Nueva», con 3 sesiones cada una. 0 cambios inesperados en las 219 actividades.
- 4ºC: Latín pasa de 3 a 2 horas y Geografía e Historia, de 3 a 4, con 2 y 4 sesiones.
- Horario: 0 choques de profesor, aula y subgrupo. El diagnóstico de la aplicación, con el código de
  `v0.4.0` más la suspensión, da 0 violaciones duras, 0 de guardia y 517 penalizaciones blandas
  (ventanas 174 y consecutivas 24, que suman el objetivo).
- Salvedad: la fila de BYG1 sigue en `profesor` de 2026/2027, sin plazas, tutoría, restricciones ni
  guardias. Ella intentó borrarla al empezar la tarea 3; la aplicación no la dejó por tener clases,
  y ese aviso la guió a reasignarlas, pero no volvió a borrarla y nadie lo advirtió. Frente a la
  tarea 5 de S188, con cuatro actividades borradas sin aviso, ahora no se pierde nada.

Observaciones:
- Edge no ofreció traducir: la oferta se desactivó en el montaje (CG).
- «Curso sin nombre» le hizo dudar a qué curso pertenecían los datos; lo entendió al leer el texto de
  la ventana. Es un dato para la base de partida de `O-uso-centro`, no un fallo del producto.
- En la conversación durante la espera (CF), no señaló ninguna tarea difícil; esperaba que el curso
  anterior tuviera nombre, y el error al borrar a BYG1 le indicó qué buscar.

Desviaciones, sin coste:
- El observador abrió Educhronos antes de que ella se sentara (de 12:44:56 a 12:45:39), para medir
  la ventana de arranque, que tardó como mucho 2 s: el primer arranque y la migración no fueron suyos.
  El primer arranque ya está cubierto por el humo (condición 1).
- No se anotaron los tiempos por tarea (regla 6); se reconstruyeron con el log, como en S188.

Incidencias (R-incidencia): `D-baja-profesor-incompleta` y `D-aviso-arranque-tardio`, en §4 de
`gestion_proyecto.md`. `D-infactible-sin-aviso-residual` y `D-capacidad-sin-aviso` no se observaron.

## Material

`/home/luis/educhronos-aceptacion/s235/`, fuera del repo, con un manifiesto por carpeta: `t0` y `t0b` (lecturas), `f1` (tag,
Release y zip), `f2` (VM: instantáneas, `verif235` y `secretaria`) y `f3` (esta acta).

S239: `/home/luis/educhronos-aceptacion/s239/` (t0, t0b, f1, dia, d3, d4, d5, limpieza-vm, limpieza-vm-b y cierre), con un manifiesto por carpeta; la hoja del observador es un documento de Claude (https://claude.ai/code/artifact/9167bd0f-9846-4a8d-a254-e5690e1fd71a).
