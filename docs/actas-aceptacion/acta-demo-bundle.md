# Acta de la demo — Educhronos (`O-demo-bundle`, H5)

Borrador escrito en S181 (`C-preparacion-demo`). Las secciones marcadas PENDIENTE se rellenan en
la preparación (F3), el ensayo (F4) y la demo. Sin datos personales: códigos, tipos y recuentos;
ni nombres ni disponibilidades.

## ACTA DE LA DEMO — Educhronos

- **Fecha de la demo:** PENDIENTE.
- **Asistentes por rol:** la persona que elabora hoy los horarios del centro (opera); el
  desarrollador (observa y anota). PENDIENTE confirmar.
- **Versión:** Release `v0.1.0`, commit `a7846b69146fc64f57a14060da99eb00f569ea44`,
  zip `Educhronos-win.zip` de 176.078.736 B,
  sha256 `7afc5b8c97e2eb8f2484c612cfdab72cf694b01deb5cecb82fdf04131c9fb170`.
- **Máquina:** máquina virtual `Win11` de S163 (VirtualBox, 3 CPU, 8192 MB). Windows 11 Pro,
  versión 10.0.26200.9457 (`Get-CimInstance Win32_OperatingSystem` y `ver`, S181).
- **Cuenta:** `demo` (`educhronos\demo`), creada en S181 con `net user demo /add`; `whoami /groups`
  da BUILTIN\Usuarios (S-1-5-32-545) y nivel obligatorio medio (S-1-16-8192), sin S-1-5-32-544.
- **Java y Node:** ausentes: `where.exe` no encuentra ni `java` ni `node`, y `JAVA_HOME` está vacío (S181).
- **Base:** copia de `educhronos-s137.db` (md5 `dfa4c0774a842d8eb6b7a941e23df2a9`) colocada como
  `%LOCALAPPDATA%\Educhronos\educhronos.db` con la aplicación cerrada y sin `curso-abierto`. En Windows,
  `Get-FileHash -Algorithm MD5` da `DFA4C077…` y la carpeta contiene un único fichero (S181).
- **Instantáneas:** `s177-estado` (estado previo, red de seguridad) y `s181-demo-lista` (cuenta
  nueva con la base colocada, sin abrir). La demo arranca de `s181-demo-lista`. Tomadas en S181 con la VM
  apagada: `s177-estado` (`092bcb0d-5ea9-4af3-b757-9e159e53e85c`) y, colgando de ella,
  `s181-demo-lista` (`89411b3e-9921-4a7d-8f8a-fc55aed8dc82`), la actual.
- **Programa:** extraído con el Explorador en
  `C:\Users\demo\Downloads\Educhronos-win\Educhronos\Educhronos.exe`; ni el zip ni el `.exe` llevan
  `Zone.Identifier`.
- **Ids de horario:** 2025/2026, id 1 (el del banco); 2026/2027, PENDIENTE.

## Resumen por condición

| Condición | Resultado | Sección |
|---|---|---|
| 1. Preparación en la VM | EN CURSO (S181): falta el ensayo | Cabecera, Procedimiento de transporte, Ensayo |
| 2. Arranque | CUMPLIDA (S181) | Arranque |
| 3. Consulta de 2025/2026 | PENDIENTE | Consulta de 2025/2026 |
| 4. Ejercicio de muestra | PENDIENTE | Ejercicio de muestra |
| 5. Inventario de huecos | PENDIENTE | Inventario de huecos, Incidencias |
| 6. Base de partida | CUMPLIDA (S181) | Base de partida |
| 7. Acta | PENDIENTE | Este documento |

## Procedimiento de transporte

Medido en S181.

1. Linux: `sha256sum -c Educhronos-win.zip.sha256` da OK en
   `/home/luis/educhronos-aceptacion/s177/`. En el USB (NTFS, etiqueta `ESD-USB`) se crea
   `EDUCHRONOS-S181/` con el zip y su `.sha256`, y `base/educhronos.db` (copia de
   `educhronos-s137.db`) con su `.md5`; se verifican los dos en el USB y se desmonta sin apagar el
   dispositivo.
2. VM: el USB se conecta con Dispositivos → USB. Su letra se identifica con `Get-Volume` (la unidad
   `Removable`) y nunca se supone: la carpeta compartida ocupa otra letra (`E:`,
   `\\VBoxSvr\educhronos-vm`), y copiar el zip desde ella lo marca como descargado de Internet y
   SmartScreen lo detiene (S177).
3. Windows: el zip y su `.sha256` van a Descargas; `Get-FileHash` da `7AFC5B8C…9FB170` y
   `Get-Item -Stream Zone.Identifier` da error (sin marca). Se extrae con el Explorador. La base se
   copia a `%LOCALAPPDATA%\Educhronos\` con la aplicación sin abrir, y su md5 da `DFA4C077…`.
4. Windows → Linux: por la carpeta compartida `educhronos-vm` (en el host,
   `/home/luis/educhronos-vm`), con el md5 comprobado en los dos lados. En este sentido la marca de
   Internet no importa.

## Reglas de observación

1. La consigna se lee entera antes de empezar, igual en el ensayo y en la demo.
2. Ella piensa en voz alta. Quien observa no señala la pantalla ni sugiere.
3. Cada pregunta se anota, con el minuto y la tarea, ANTES de responderla.
4. Si la pregunta es sobre QUÉ pide la tarea, se contesta y se anota como ACLARACIÓN (defecto de la
   consigna). Si es sobre CÓMO hacerlo en Educhronos, se anota y sólo se contesta pasado el umbral
   de atasco o si ella lo pide expresamente: es una AYUDA, incidencia candidata.
5. Umbral de atasco: 5 minutos sin avanzar en una tarea. Entonces se da la menor pista que la
   desbloquee y se anota literal.
6. Se anotan el inicio y el final de cada tarea, y el tiempo de generación.
7. Mientras genera, se espera; no se adelanta el inventario, que va después del ejercicio.

## Consigna

El curso actual del centro es el 2025/2026 y ya está cargado en Educhronos. Haz las tareas en
orden. Piensa en voz alta. Si algo no se entiende, pregunta.

Primera parte: el curso 2025/2026

1. Abre Educhronos y encuentra el horario de este curso.
2. Mira el horario del grupo 4ºC, el del profesor ING1 y el de un aula cualquiera.
3. Consigue, listos para imprimir, los horarios de todos los grupos, de todos los profesores y de
   todas las aulas, y el horario completo en un fichero que se pueda abrir con una hoja de
   cálculo. Enséñanos dónde han quedado.

Segunda parte: el curso 2026/2027

4. Prepara el curso 2026/2027 a partir del actual, sin perder el 2025/2026.
5. BYG1 deja el centro. Sus clases las dará una profesora nueva, con código BYG4 y nombre
   «Profesora Nueva». BYG1 no debe quedar en el curso 2026/2027.
6. En 4ºC, Latín pasa a tener 2 horas a la semana y Geografía e Historia, 4.
7. ING1 no puede dar clase los viernes. FIS2 prefiere no dar clase a primera hora.
8. Consigue el horario de 2026/2027 y comprueba en él que BYG4 da las clases que eran de BYG1 y
   que 4ºC tiene 2 horas de Latín.
9. Comprueba que el horario de 2025/2026 sigue ahí.

## Ensayo (F4)

PENDIENTE: fecha, quién ejecuta, inicio y final de cada tarea, tiempo de generación en la VM,
estado del solver, aclaraciones que la consigna necesitó y cambios hechos en ella.

## Arranque (condición 2)

CUMPLIDA en S181, en la preparación (22:16, hora de la VM). Doble clic en `Educhronos.exe`: se abre
el navegador en `127.0.0.1:8080`, sin avisos de seguridad ni del cortafuegos. «Cursos…» lista un
único curso, «Sin nombre», sobre `educhronos.db`, Activo y Abierto ahora; la barra dice «Curso sin
nombre». «Horario» lleva a `/horario/1`, «Horario 05/09/2026 14:48», con sus sesiones en la rejilla.
Capturas 01 a 03. Se cierra con «Salir» en la bandeja; el log registra el apagado ordenado a las
22:22:02 (captura 05).

## Consulta de 2025/2026 (condición 3)

PENDIENTE: las cuatro descargas y el oráculo (se espera 1285/0/0, 835/0/0, 819/0/0, CSV OK).

## Ejercicio de muestra (condición 4)

PENDIENTE, por tarea: inicio, final, aclaraciones y ayudas (literal). Nombre que dio al curso
2025/2026 al archivarlo. Tiempo de generación y estado del solver.

## Inventario de huecos (condición 5)

PENDIENTE: lo que entrega a la empresa, por tipo de dato, cada uno como «ya está en Educhronos»,
«se puede meter» o «no se puede».

## Incidencias

PENDIENTE: cada una con su `D-*`, o «ninguna». Salen de las ayudas de la condición 4 y de los
«no se puede» de la condición 5 (R-incidencia), anotando que salen de `O-demo-bundle`.

## Base de partida (condición 6)

CUMPLIDA en S181. La base de la cuenta `demo`, abierta una vez (el arranque) y sin duplicar, se saca
tras el cierre limpio: 249.856 B, sin `-wal` ni `-shm`, md5 `ba0db78259de85c819fe507cbf0c2496` en
Windows y en Linux. Frente a `educhronos-s137.db`: `integrity_check` ok, `foreign_key_check` vacío y
`user_version` 0; las 21 tablas del banco con el mismo DDL y 0/0 en las dos direcciones (control
positivo: la misma comparación contra `educhronos-s137-centro-completo.db` da `sesion` 819/0), más
la tabla `curso`, vacía. Copia en `/home/luis/educhronos-aceptacion/s181/base-partida/`. Dónde se
instala lo decide el objetivo (3).

## Observaciones

PENDIENTE: lo observado que no es incidencia, del ensayo y de la demo.

## Material (fuera del repo)

`/home/luis/educhronos-aceptacion/s181/`: `base-partida/`, `capturas/` y `trabajo/` (informe del
M2, borradores y comparación de la base de partida). El USB conserva `EDUCHRONOS-S181/`. La VM
`Win11` queda apagada en la instantánea `s181-demo-lista`.

### Capturas

| Fichero | sha256 | Muestra |
|---|---|---|
| 01-arranque-inicio.png | `33a2c2695d4bbadaa4e934fbaf8e1f381a60152a66ca2ceae2b7435e8575322b` | Inicio tras el doble clic |
| 02-cursos-dialogo.png | `b51baa8901452256fd4d2f5431091d764694559c1c1a137adb9f25cad58e958c` | «Cursos…»: un curso sin nombre, activo |
| 03-horario-1.png | `b872cf243147d726c90fb2fcc7218a5b9bb37a5893758644149e22d9d596c917` | `/horario/1` |
| 04-cierre-carpeta-md5.png | `a8db940dee88b08449a012449fed5f2f3dbf67fb3a0e0fefa421e0278aeecf2d` | Carpeta de datos y md5 tras cerrar |
| 05-log-cierre-copia.png | `f2835de226122d3885cedc8ae99cb6b74286744e9d7415cad6aba25c896ea702` | Log del cierre y copia a la carpeta compartida |

Los sha256 son los de los ficheros conservados. Se descargaron del chat, que los recodifica: su
contenido se comprobó igual al de las capturas originales por el sha256 de los píxeles en RGBA (S181).
