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
- **Ids de horario:** 2025/2026, id 1 (el del banco); 2026/2027, PENDIENTE (en el ensayo de S182 fue el id 1).

## Resumen por condición

| Condición | Resultado | Sección |
|---|---|---|
| 1. Preparación en la VM | CUMPLIDA (S182) sobre `v0.1.0`; se rehará sobre la versión de `O-pre-demo` (decisión L) | Cabecera, Procedimiento de transporte, Ensayo |
| 2. Arranque | CUMPLIDA (S181) | Arranque |
| 3. Consulta de 2025/2026 | PENDIENTE (ensayada en verde en S182) | Consulta de 2025/2026 |
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
8. Corrección de la consigna tras un ensayo (S182): una aclaración de redacción se aplica sin
   volver a ensayar; un cambio de tarea (otra acción u otro resultado) se vuelve a ensayar en la
   misma sesión. En la demo se lee la versión que quedó en el repo al cerrar el último ensayo.

## Pistas mínimas (S182)

Preparadas antes de la demo para los atascos previsibles. Se dan sólo según la regla 5 (pasado el
umbral o si ella la pide) y se anotan literales, con el minuto.

1. No encuentra el programa: «Búscalo en Inicio».
2. No encuentra dónde preparar el curso nuevo: «Mira arriba a la derecha».
3. Intenta cambiar actividades en el 2025/2026 y la aplicación no la deja: «Eso se cambia en el
   curso nuevo».
4. No sabe qué clases tenía BYG1: «El horario de 2025/2026 dice qué clases da cada profesor». Si
   sigue atascada otros 5 minutos: «Son las de ByG de 1º».

Se revisan cuando exista la versión de `O-pre-demo`.

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

29/09/2026 (S182), en la VM desde `s181-demo-lista`, cuenta `demo`, `v0.1.0`. Ejecuta el
desarrollador y guía el arquitecto. Quien ejecuta conoce la aplicación: el ensayo mide si cada
tarea está bien pedida y se puede ejecutar, no si se sabe hacer; eso sólo lo mide la demo. Horas
de la VM (`Get-Date` 06:59:19 al empezar, en línea con el host). La consigna se leyó entera antes
de T1.

| Tarea | Inicio | Fin | QUÉ claro | Notas |
|---|---|---|---|---|
| T1 | 07:15 | 07:15 | sí | Duda de CÓMO, «¿dónde está instalado Educhronos?»; lo encontró buscando en Inicio (`D-sin-acceso-directo`) |
| T2 | 07:18 | 07:18 | sí | Falta filtro en los desplegables de Grupo, Profesor y Aula (`D-selectores-sin-busqueda`) |
| T3 | 07:21 | 07:21 | sí | Cuatro descargas en Descargas; sin preguntas del navegador anotadas |
| T4 | 07:25 | 07:27 | sí | 2025/2026 archivado con el nombre «2025/2026»; nuevo «2026/2027» en `curso-2026-2027.db` |
| T5 | 07:28 | 07:41 | sí | 409 «No se puede borrar: referenciada por 4 plaza(s)» a las 07:29 al borrar BYG1 antes de reasignar. AYUDA DEL ENSAYO: las cuatro actividades `ByG-1ºA` a `ByG-1ºD`; retomada a las 07:39. La parada incluye la conversación y no mide un atasco (`D-entidad-sin-actividades`) |
| T6 | 07:46 | 07:48 | sí | El filtro «4ºC» da 11 de 219 y omite `Bloque-ATEDU_Rel-4ESO` (`D-filtro-por-codigo`) |
| T7 | 07:51 | 07:52 | sí | — |
| T8 | 07:58 | 08:11 | sí | «No se sabe si se ha quedado colgada o está trabajando» (`D-generacion-sin-movimiento`); pregunta qué pasa si se toca la configuración mientras genera (`D-generacion-sin-exclusion`) |
| T9 | 08:16 | 08:16 | sí | — |

Generaciones en el curso 2026/2027. I2 e I3 son medición fuera de la consigna, porque una corrida
única no es una medida (`D-generacion-no-reproducible`).

| Intento | Pulsa | Resultado | Salida |
|---|---|---|---|
| I1 | 07:59:00 | 08:09:10 | Horario 1: FEASIBLE, objetivo 219, cota 0,0, 602,8 s según la base (`fecha_generacion` menos el instante del nombre) |
| I2 | 08:18:56 | 08:29:06 | 503 «Se agotó el tiempo de cálculo. Vuelve a intentarlo.» |
| I3 | 08:30:08 | 08:40:22 | 503, mismo texto |

1 de 3, con la semilla 42 en los tres. Los dos fallos llegan tras agotar el presupuesto; no dejan
rastro ni en la base ni en el log, y sus horas son las del usuario. El fallo conserva el horario
anterior y su mensaje se entiende. Los núcleos de la máquina real de la secretaria no están
medidos: no se sabe si la VM (3 CPU) es más pesimista que ella.

Resultado en datos, medido en Linux sobre las copias traídas (S182):
- Transporte de vuelta por la carpeta compartida a `E:\medidas\s182\`, con el md5 igual en los dos
  lados en los nueve ficheros. `Get-ChildItem` sobre la carpeta compartida falló con
  `NotSupportedException`: las descargas llevan `Zone.Identifier` y VirtualBox lo vuelca como un
  fichero con «:» en el nombre (causa probable, no reproducida). Basta el md5 del lado Linux.
- `integrity_check` ok y `foreign_key_check` vacío en las dos bases.
- 2025/2026: 0/0 frente a la base de partida en 21 tablas; sólo `curso`, 1/0 (la fila archivada).
- 2026/2027: exactamente los cambios de la consigna y ninguno más. BYG1 fuera y BYG4 («Profesora
  Nueva») con las cuatro plazas de `ByG-1ºA` a `ByG-1ºD`; `LAT-4ºC` 2 y `GeH-4ºC` 4; 6 filas DURA
  de ING1 en los tramos lectivos del viernes y 5 BLANDA de FIS2 en el primer tramo de cada día,
  peso 1.
- Horario 1 del 2026/2027: BYG4 con 12 sesiones, 3 por grupo y sin repetir día; 4ºC con 30 (LAT 2,
  GeH 4); ING1 con 0 el viernes y 4 cada día de lunes a jueves; FIS2 con 2 sesiones en el primer
  tramo (preferencia blanda); ninguna sesión en un tramo DURA.

Aclaraciones: ninguna. Cambios en la consigna: ninguno; queda congelada como está en este acta.

## Arranque (condición 2)

CUMPLIDA en S181, en la preparación (22:16, hora de la VM). Doble clic en `Educhronos.exe`: se abre
el navegador en `127.0.0.1:8080`, sin avisos de seguridad ni del cortafuegos. «Cursos…» lista un
único curso, «Sin nombre», sobre `educhronos.db`, Activo y Abierto ahora; la barra dice «Curso sin
nombre». «Horario» lleva a `/horario/1`, «Horario 05/09/2026 14:48», con sus sesiones en la rejilla.
Capturas 01 a 03. Se cierra con «Salir» en la bandeja; el log registra el apagado ordenado a las
22:22:02 (captura 05).

## Consulta de 2025/2026 (condición 3)

PENDIENTE: las cuatro descargas y el oráculo (se espera 1285/0/0, 835/0/0, 819/0/0, CSV OK).

Ensayada en S182: las cuatro descargas pasan el oráculo contra la `educhronos.db` de la VM. Grupo
1285/0/0 en 28 páginas, profesor 835/0/0 en 59, aula 819/0/0 en 44 con 0 páginas vacías con
leyenda, y CSV «OK: las tres vistas coinciden». La condición se cumple en la demo.

## Ejercicio de muestra (condición 4)

PENDIENTE, por tarea: inicio, final, aclaraciones y ayudas (literal). Nombre que dio al curso
2025/2026 al archivarlo. Tiempo de generación y estado del solver.

Protocolo de la generación en la demo: pendiente de la versión de `O-pre-demo`, que cambia el
tiempo de cálculo (decisión L).

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

Del ensayo (S182). Ninguna es incidencia: no salen del uso del centro (R-incidencia). Cada una
tiene ficha en §4 de `gestion_proyecto.md`; si la demo la reproduce, esa ficha toma la sede y la
prioridad de la incidencia.

1. La generación falla a menudo: 1 de 3 en la VM (`D-generacion-no-reproducible`).
2. Nada impide tocar la configuración ni lanzar otra generación mientras se genera, y el guardado
   no es atómico; por lectura de código, sin medir (`D-generacion-sin-exclusion`).
3. No hay camino de un profesor o una asignatura a sus actividades, y el 409 de borrado no dice
   qué plazas lo impiden (`D-entidad-sin-actividades`).
4. El filtro de Actividades busca en el texto y omite sin aviso actividades del grupo buscado
   (`D-filtro-por-codigo`).
5. Una generación no deja rastro: ni log, ni fila si falla; estado y objetivo no se ven
   (`D-generacion-sin-rastro`).
6. Durante la generación no se sabe si la aplicación sigue trabajando
   (`D-generacion-sin-movimiento`).
7. Tras extraer el zip no hay acceso directo (`D-sin-acceso-directo`).
8. Los selectores de entidades no tienen filtro, tampoco los de la vista de horario; la ventana
   de actividades resulta tosca con muchas (`D-selectores-sin-busqueda`).
9. En un curso archivado, «Generar horario» sigue visible y habilitado
   (`D-generar-en-solo-lectura`).
10. Datos del centro, no de la aplicación: tres asignaturas «Latín» (LAT, Lat2, Latín) y tres
    «Geografía e Historia» (GeH, Geo, Geogr). Sin ficha.

Previsión para la demo: T1 (dónde está el programa), T4 («Cursos…»), T5 (el 409, antes y después
de duplicar) y la generación son los puntos donde más probable es una ayuda.

## Material (fuera del repo)

`/home/luis/educhronos-aceptacion/s181/`: `base-partida/`, `capturas/` y `trabajo/` (informe del
M2, borradores y comparación de la base de partida). El USB conserva `EDUCHRONOS-S181/`. La VM
`Win11` queda apagada en la instantánea `s181-demo-lista`.
`/home/luis/educhronos-aceptacion/s182/`: `datos/` (las dos bases, `curso-abierto`, `.lock` y
log de la VM tras «Salir»), `descargas/` (las cuatro del horario 1 de 2025/2026), `capturas/` y
`MANIFIESTO.sha256`. Tras verificar, la VM se restauró a `s181-demo-lista` (S182).

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

### Capturas del ensayo (S182)

Descargadas del chat, que las recodifica; no se conservaron los originales de la VM. El sha256 es
el del fichero conservado; su contenido se comprobó igual al de las capturas pegadas en el chat por
el sha256 de los píxeles en RGBA (S182). `T5-profesores.png` muestra nombres reales: queda fuera
del repo.

| Fichero | sha256 | Muestra |
|---|---|---|
| T1-horario.png | `9fa8f4891eeb5b5280aedff512c108ab9dc37d1f886ac4f0ffd01b58a3349159` | `/horario/1` del 2025/2026 |
| T3-descargas.png | `3e2904f0c92c383fe84778c7dec2ee806e52bc453de112ee1702dc2c295ba0b7` | Descargas con los cuatro ficheros |
| T4-cursos.png | `8d2b0676185b3e6a90e73b828dd53d846088313fe626c7b9f9a617c4a8d1a721` | «Cursos…» con los dos cursos |
| T5-profesores.png | `c1058f84d0af1d5e62b418b5c04546b49ed1b18f0ffbbada5a33b844174abeda` | Profesores filtrados por ByG: BYG4 sí, BYG1 no |
| T6-actividades-4C.png | `b9edfaed71ba6537258012afc86bfff5af691c9940d1dcfab6f2b3cc813e944a` | Actividades filtradas por 4ºC: 11 de 219 |
| T7-FIS2-disponibilidad.png | `552fbc053af63ce399054993ff6132734ff4a5141c06c6ba2464535da414fe74` | FIS2: «Prefiere no» a primera hora |
| T7-ING1-disponibilidad.png | `57e57eab8042630d78ed67994d6eafae99f6c23091b18f4e5e95ddafb1654f0a` | ING1: «No puede» el viernes |
| T8-BYG4.png | `ba8a960ad23beee09b0687391553f4e85dd57c56ffe2fecdd4084c88ab8b7f84` | Horario 2026/2027, vista de BYG4 |
| T8-4C.png | `b1421b36ab6a23d7bcb2c89c90c7395f23d006963f56556f9881d9f03a66dc08` | Horario 2026/2027, vista de 4ºC |
| T9-2025-2026.png | `34b0bdb867f8134adf07a94f775ea9a18a1aa718eebc340699c6679c7f0091b2` | 2025/2026 en solo lectura |
| I2-I3-503.png | `76d28c0f9bbde8034fbadbfdbb419b277678039962e1633ae92ef42ba3498ecd` | Mensaje del 503 tras un intento fallido |
