# Acta de la prueba corta de la secretaria: `v0.4.0` en la VM `Win11` (`O-prueba-secretaria`, H5)

## Estado

Preparada en S235: condiciones 3 (instantánea) y 4 (consigna). La prueba (condición 5) espera fecha con la secretaria. Desde S235 es
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

Pendiente de la prueba.

## Material

`/home/luis/educhronos-aceptacion/s235/`, fuera del repo, con un manifiesto por carpeta: `t0` y `t0b` (lecturas), `f1` (tag,
Release y zip), `f2` (VM: instantáneas, `verif235` y `secretaria`) y `f3` (esta acta).
