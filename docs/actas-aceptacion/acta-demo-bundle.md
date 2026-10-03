# Acta de la demo — Educhronos (`O-demo-bundle`, H5)

Borrador escrito en S181 (`C-preparacion-demo`). Las secciones marcadas PENDIENTE se rellenan en
la preparación (F3), el ensayo (F4) y la demo. Sin datos personales: códigos, tipos y recuentos;
ni nombres ni disponibilidades.

## ACTA DE LA DEMO — Educhronos

- **Fecha de la demo:** 03/10/2026 (S188).
- **Asistentes por rol:** la persona que elabora hoy los horarios del centro (opera); el
  desarrollador (observa y anota). En la demo (S188): la secretaria del centro (opera) y el usuario
  (observa).
- **Versión:** Release `v0.1.0`, commit `a7846b69146fc64f57a14060da99eb00f569ea44`,
  zip `Educhronos-win.zip` de 176.078.736 B,
  sha256 `7afc5b8c97e2eb8f2484c612cfdab72cf694b01deb5cecb82fdf04131c9fb170`.
  Desde S187, `v0.2.0` (zip `be072519…a2a228a7`, decisión L), que es la de la demo (S188).
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
  En S187 se toma `s187-demo-lista` (`a464f499-85de-4ef7-a7b9-8c14c8ac6295`), con la VM apagada,
  colgando de `s181-demo-lista`: la misma cuenta y la misma base sin abrir, con `v0.2.0` en lugar de
  `v0.1.0`. Desde S187 la demo arranca de `s187-demo-lista`.
  En S188, tras la demo y con la VM apagada, se toma `s188-demo-hecha`
  (`834ef7fe-35ef-42bf-aad0-eaf40dc93bac`), colgando de `s187-demo-lista`, antes de extraer la evidencia.
- **Programa:** extraído con el Explorador en
  `C:\Users\demo\Downloads\Educhronos-win\Educhronos\Educhronos.exe`; ni el zip ni el `.exe` llevan
  `Zone.Identifier`.
  Desde S187, `v0.2.0` (zip `be072519…a2a228a7`, jar `7e243fb5…e4b155b5`) en la misma ruta, tras
  retirar `v0.1.0`; ni el zip ni el `.exe` llevan `Zone.Identifier`. El jar conserva el nombre
  `app-0.1.0-SNAPSHOT.jar` (`D-version-invisible`): la versión se identifica por su sha256.
- **Ids de horario:** 2025/2026, id 1 (el del banco); 2026/2027, id 1 en la demo (S188), como en los ensayos de S182 y S187.

## Resumen por condición

| Condición | Resultado | Sección |
|---|---|---|
| 1. Preparación en la VM | CUMPLIDA (S182) sobre `v0.1.0`; se rehará sobre la versión de `O-pre-demo` (decisión L). **CUMPLIDA (S187) sobre `v0.2.0`**: ensayo entero desde `s187-demo-lista`, sin aclaraciones ni ayudas (sección «Ensayo sobre `v0.2.0` (S187)»). | Cabecera, Procedimiento de transporte, Ensayo |
| 2. Arranque | CUMPLIDA (S181) | Arranque |
| 3. Consulta de 2025/2026 | PENDIENTE (ensayada en verde en S182 y en S187). **MEDIDA (S188), NO CUMPLIDA**: una descarga de cuatro (grupos, oráculo 1285/0/0). **CUMPLIDA CON SALVEDAD (S189, decisión N)** | Consulta de 2025/2026, Demo con la secretaria (S188) |
| 4. Ejercicio de muestra | PENDIENTE. **MEDIDA (S188), NO CUMPLIDA**: T5 sin reasignación (4 actividades borradas, BYG4 sin plazas); duplicado, horas, disponibilidad y generación hechos; registro con las desviaciones de la sección de la demo. **CUMPLIDA CON SALVEDAD (S189, decisión N)** | Ejercicio de muestra, Demo con la secretaria (S188) |
| 5. Inventario de huecos | PENDIENTE. **No se hizo en S188**: el usuario intentará recuperar los datos más adelante (decisión F). **CUMPLIDA (S189)** | Inventario de huecos, Incidencias |
| 6. Base de partida | CUMPLIDA (S181) | Base de partida |
| 7. Acta | PENDIENTE. **EN CURSO (S188)**: acta escrita, falta el inventario. **CUMPLIDA (S189)** | Este documento |

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

**Actualización a `v0.2.0` (S187).** Desde `s181-demo-lista`. Linux: el zip de `v0.2.0` ya estaba en
el USB, en `s186-f3/`, y `sha256sum -c` da OK; la raíz del USB y `EDUCHRONOS-S181/` conservan zips
de `v0.1.0`, así que en Windows el zip se toma siempre por su ruta completa
(`<letra>:\s186-f3\Educhronos-win.zip`). La base no viaja: ya está en la instantánea, y en Windows
se comprueban su md5 (`DFA4C077…`) y que no hay `curso-abierto`. Windows: sha256 del zip en el USB y
de su copia en Descargas (`BE072519…`), y `Get-Item -Stream *` sólo da `:$DATA`; se retiran
`Downloads\Educhronos-win\`, `Educhronos-win.zip` y su `.sha256` de `v0.1.0`, y se extrae `v0.2.0`
con el Explorador en la misma ruta. Es la actualización de `docs/empaquetado.md:101-107` con dos
pasos omitidos: el respaldo de datos (paso 2), porque la instantánea ya lo es, y abrir la aplicación
para comprobar los cursos (paso 4), porque abriría la base, que la condición 1 exige sin abrir. Así
queda un único `Educhronos.exe` en el perfil, el que encuentra la búsqueda de Inicio (en S186 se
extrajo al lado, en `Descargas\s186\`, que para la demo dejaría dos). Comprobación final con la
aplicación sin abrir: un único `Educhronos.exe`, jar `7E243FB5…E4B155B5`, base `DFA4C077…` sola en
su carpeta, sin `curso-abierto` y sin procesos.

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
5. La generación falla y vuelve a intentarlo con el mismo tiempo: «Puedes darle más tiempo».
   (S187: con 10 minutos salió horario en 1 de 1 en S187 y en 1 de 3 en S182; el mensaje del 503
   dice «Vuelve a intentarlo» sin nombrar el tiempo.)

Se revisan cuando exista la versión de `O-pre-demo`. **Revisadas en S187 sobre `v0.2.0`:** siguen
valiendo de la 1 a la 4 (T1 y T4 se hicieron por ese camino; en T5 el horario de 2025/2026 fue el
rodeo, sin ayuda) y se añade la 5.

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

## Ensayo sobre `v0.2.0` (S187)

01/10/2026 (S187), en la VM desde `s187-demo-lista`, cuenta `demo`, `v0.2.0`. Ejecuta el
desarrollador y guía el arquitecto, con las mismas reglas y la misma consigna que en S182. Se ensaya
entera (condición 1), no sólo las tareas que tocan los cambios de `v0.2.0` (T2, T5, T6, T8 y T9):
las tareas van encadenadas y el texto de la condición pide el ensayo entero. Horas de la VM. La
aplicación arrancó a las 07:24:47 (fecha de `educhronos.lock` en el listado de PowerShell de T4; el
fichero no se trajo, y el log empieza a las 07:24:49), antes de la hora anotada para T1.

| Tarea | Inicio | Fin | QUÉ claro | Notas |
|---|---|---|---|---|
| T1 | 07:24:47 | 07:27:45 | sí | Encontrado en Inicio; sin avisos de Windows. Hora anotada al empezar, 07:25:36, ya con la aplicación abierta |
| T2 | 07:29:45 | 07:31:40 | sí | Filtro de los desplegables usado en Profesor y Grupo; con «4º» encontró 4ºC a la primera |
| T3 | 07:34:10 | 07:35:11 | sí | Cuatro descargas en Descargas, con los nombres de S182; el navegador no preguntó nada |
| T4 | 07:41:05 | 07:42:32 | sí | 2025/2026 archivado como «2025/2026»; nuevo «2026/2027» en `curso-2026-2027.db`; sin mensajes |
| T5 | 07:48:29 | 07:55:57 | sí | 409 «No se puede borrar: referenciada por 4 plaza(s)» al borrar BYG1 antes de reasignar. Sin ayuda: volvió al 2025/2026, vio en su horario que BYG1 daba en 1º y filtró Actividades por «ByG-1º» (`D-entidad-sin-actividades`). BYG4 no existía al editar la primera actividad; la creó y siguió. Selector de profesor con filtro, sin problemas |
| T6 | 08:06:15 | 08:09:42 | sí | El filtro «4ºC» da 11 de 219 y omite `Bloque-ATEDU_Rel-4ESO`, como en S182 (`D-filtro-por-codigo`) |
| T7 | 08:16:24 | 08:18:41 | sí | Sin mensajes. Observación sin alta: en el diálogo de disponibilidad, «Guardar» está siempre habilitado, haya cambios o no |
| T8 | 08:24:06 | 08:37:06 | sí | Diálogo con 10 minutos por defecto; la barra mostró «de 10:00». Comprobados en las vistas BYG4 y el Latín de 4ºC |
| T9 | 08:42:17 | 08:43:24 | sí | — |

Generación en el curso 2026/2027, un solo intento:

| Intento | Pulsa | Resultado | Salida |
|---|---|---|---|
| I1 | 08:25:18 | 08:35:20 | Horario 1: FEASIBLE, objetivo 233, 602,8 s según la base (`fecha_generacion` menos el instante del nombre), con los 10 minutos por defecto |

La hora de pulsar es la de la base; la anotada en PowerShell justo después fue 08:25:21. Una corrida
no es una medida (`D-generacion-no-reproducible`): en S182, 1 de 3 a 600 s. No hay captura del
resultado (`T8-resultado.png` no se guardó); el horario generado aparece en `T8-BYG4.png` y
`T8-4C.png` («Horario 01/10/2026 08:25»).

Resultado en datos, medido en Linux sobre las copias traídas (S187):
- Transporte de vuelta por la carpeta compartida a `E:\medidas\s187\`: ocho ficheros copiados uno a
  uno con la aplicación cerrada («Salir»), con el md5 calculado en Windows sobre el original e igual
  en Linux.
- `integrity_check` ok y `foreign_key_check` vacío en las dos bases. `educhronos.db` tiene el mismo
  md5 que en S182.
- 2025/2026: 0/0 frente al banco en 21 tablas; `curso`, una fila, «2025/2026», archivada. Las cuatro
  descargas pasan el oráculo (`docs/empaquetado.md:455-460`) y la leyenda
  (`docs/guion-aceptacion.md:563-566`): grupo 1285/0/0 en 28 páginas, profesor 835/0/0 en 59, aula
  819/0/0 en 44 con 0 páginas vacías con leyenda, y CSV «OK: las tres vistas coinciden». El CSV es
  idéntico al de S182; los PDF difieren en md5 (no investigado; probablemente por la fecha que
  llevan dentro).
- 2026/2027: exactamente los cambios de la consigna y ninguno más, como en S182.
- Horario 1 del 2026/2027: BYG4 con 12 sesiones, 3 por grupo de 1ºA a 1ºD y sin repetir día; 4ºC con
  sus 30 tramos ocupados (53 sesiones contando las optativas en paralelo, igual que en el banco y en
  S182), LAT 2 y GeH 4; ING1 con 0 el viernes y 4 cada día de lunes a jueves; ninguna sesión en un
  tramo DURA; FIS2 con 1 sesión en el primer tramo (preferencia blanda; 2 en S182).
- Log sin ERROR ni Exception; cierre ordenado con «Salir».

Aclaraciones: ninguna. Ayudas: ninguna. Cambios en la consigna: ninguno. Material en
`/home/luis/educhronos-aceptacion/s187/`.

## Demo con la secretaria (S188)

03/10/2026 (S188), en la VM desde `s187-demo-lista`, cuenta `demo`, `v0.2.0` (zip `be072519…a2a228a7`).
Opera la secretaria del centro y observa el usuario. Horas de la VM, que casan al segundo con las del
log (`+02:00`) y con las de la base (UTC).

Procedimiento seguido:
- La consigna se leyó entera en voz alta y se le entregó en papel con el mismo texto; ella la leyó.
- El observador arrancó la VM, con la sesión de `demo` iniciada y el escritorio vacío. Educhronos lo
  abrió ella (T1).
- Al acabar, la VM se apagó desde Windows sin «Salir»: el log se corta a las 13:14:39, sin la
  secuencia de cierre.
- Con la VM apagada, y ANTES de volver a arrancarla, instantánea `s188-demo-hecha`
  (`834ef7fe-35ef-42bf-aad0-eaf40dc93bac`), colgando de `s187-demo-lista`.
- Evidencia traída con `copiar-evidencia-ejecutado.ps1` a `E:\medidas\s188\`: 6 ficheros, con el md5
  calculado en Windows sobre el original e igual en Linux. `integrity_check` ok en las dos bases.

Tiempos, de los artefactos: un solo arranque, a las 13:01:35 (un único `Starting` en el log); una
sola descarga, `horario-1-grupo.pdf`, a las 13:02:12; cambios de base en el log (pool nuevo) a las
13:06:30, 13:14:13 y 13:14:39, que el log no atribuye a ninguna acción. Hora de fin del ejercicio: no
medida. No se anotaron tiempos por tarea ni el minuto de cada pregunta.

Generación en el curso 2026/2027:

| Intento | Pulsa | Resultado | Salida |
|---|---|---|---|
| I1 | 13:22:44 | 13:32:47 | Horario 1: FEASIBLE, objetivo 218, 807 sesiones, 603,165 s según la base (`fecha_generacion` menos el instante del nombre), con 10 minutos elegidos |

Al primer intento, según el observador: un intento fallido no dejaría rastro en la base ni en el log
(`D-generacion-sin-rastro`), y la base sólo tiene este horario.

Desviaciones de las reglas de observación:
- Sin inicio ni final por tarea (regla 6).
- Sin el minuto ni el texto literal de las preguntas (reglas 3 y 5).
- El umbral de atasco se aplicó por criterio del observador («respondía cuando veía que iba a
  ceder»; persona poco accesible), no a los 5 minutos (regla 5).
- Sin capturas.
- El observador la paró cuando iba a generar en el 2026/2027 antes de configurar: por
  `D-horario-irreversible`, las actividades con sesiones no se pueden editar ni borrar y no hay forma
  de descartar un horario, así que eso habría bloqueado las tareas 5 y 6 sin salida.

| Tarea | Resultado | Notas |
|---|---|---|
| T1 | Hecha sola | Relanzó el programa creyendo que no arrancaba: unos 13 s sin ninguna señal hasta el navegador (`D-arranque-sin-aviso-de-espera`). Un segundo lanzamiento abre otra pestaña y no escribe en el log: el relanzamiento no consta en los datos |
| T2 | Hecha con AYUDA | AYUDA: buscaba los grupos en el PDF descargado; se le explicó que los horarios se ven en pantalla (`D-vista-horario-no-se-descubre`). AYUDA: con el filtro del selector no veía que la lista cambiaba; se le dijo que abriera el desplegable (`D-selectores-combobox`) |
| T3 | NO COMPLETADA | Sólo `horario-1-grupo.pdf`, que pasa el oráculo (1285/0/0 en 28 páginas, idénticas a las de S187); no hay PDF de profesores ni de aulas, ni CSV. Causa no medida |
| T4 | Hecha sola | Buscó primero el botón en la configuración; está en la barra superior |
| T5 | NO CUMPLIDA, sin que nadie lo advirtiera | BYG1 borrado y BYG4 creado como «PROFESORA NUEVA», en mayúsculas tal como se escribió (la aplicación no transforma el texto), pero las cuatro actividades de ByG de 1º, con sus plazas, se borraron en vez de reasignarse, y BYG4 queda con 0 plazas (`D-borrado-sin-control-de-horas`). AYUDA: el 409 de borrar BYG1 no le llamó la atención ni lo entendió; se le explicó (`D-error-poco-visible`). Para saber qué daba BYG1 usó el PDF de grupos (`D-entidad-sin-actividades`). ACLARACIÓN: preguntó si no era más sencillo editar BYG1 y cambiarle el nombre; el código de `v0.2.0` lo permite (código y nombre editables, plazas conservadas). Es una observación sobre la consigna y sobre la decisión I |
| T6 | Hecha | Latín 2 y Geografía e Historia 4 en 4ºC, 30 de 30 tramos, tras buscar en Asignaturas y por «Latín»: la lista de Actividades busca por el código, «LAT» (`D-filtro-por-codigo`) |
| T7 | Hecha sin ayuda | Celda a celda: las cabeceras de día y de tramo pintan una columna o una fila entera, y no lo descubrió. «Guardar» siempre habilitado, como en S187: sin daño, sigue sin alta |
| T8 | Generada y comprobada | Comprobada con el observador sin advertir que faltaba ByG en 1ºA–1ºD (40 sesiones cada uno, frente a 43 en S187). AYUDA, sin atasco: preguntó para qué sirven los otros tiempos y se le explicó (`D-tiempo-generacion-poco-claro`). `D-tiempo-elegido-no-se-recuerda` no se dio. FIS2 en el primer tramo 4 veces (S187: 1): preferencia blanda, no es criterio |
| T9 | Hecha | El 2025/2026 sigue ahí, idéntico byte a byte al de los ensayos (md5 `f88bfa7d…`). Preguntó si se pueden borrar cursos anteriores; se le dijo que de momento no (`D-curso-sin-borrado`) |

Resultado en datos, medido en Linux sobre las copias traídas (S188):
- Transporte de vuelta por la carpeta compartida a `E:\medidas\s188\`: seis ficheros (las dos bases,
  `curso-abierto`, `educhronos.lock`, el log y `horario-1-grupo.pdf`) copiados uno a uno con la
  aplicación cerrada, con el md5 calculado en Windows sobre el original e igual en Linux.
- `integrity_check` ok en las dos bases.
- 2025/2026: `educhronos.db` idéntico byte a byte al de los ensayos (md5 `f88bfa7d…`, el de S182 y
  S187), archivado como «2025/2026». La descarga de grupos pasa el oráculo
  (`docs/empaquetado.md:455-460`): 1285/0/0 en 28 páginas, las mismas que en S187.
- 2026/2027, frente a la base del ensayo de S187: faltan las cuatro actividades `ByG-1ºA` a
  `ByG-1ºD`, con sus cuatro plazas y sus filas de `plaza_profesor` y `plaza_subgrupo`; BYG4 tiene el
  mismo id y código con el nombre en mayúsculas. Las otras 14 tablas comparadas, iguales: `LAT-4ºC` 2
  y `GeH-4ºC` 4, 6 filas DURA de ING1 el viernes y 5 BLANDA de FIS2 en el primer tramo, peso 1.
- Horario 1 del 2026/2027: 807 sesiones, 12 menos que en S187 (cuatro actividades de tres horas);
  BYG4 con 0 sesiones; 1ºA–1ºD con 40 cada uno; 4ºC con sus 30 tramos ocupados (53 sesiones), LAT 2 y
  GeH 4; ING1 con 0 el viernes; ninguna sesión en un tramo DURA; FIS2 con 4 sesiones en el primer
  tramo.
- Log: un solo arranque y ninguna línea de cierre; la última, a las 13:14:39, es anterior a la
  generación.

Aclaraciones: una (T5). Ayudas: cuatro (dos en T2, una en T5 y una en T8). Lo enseñado fuera del
ejercicio: nada.

Sugerencias recogidas, que no se trabajan aquí (las decide el objetivo (3)): una ventana de
«iniciando»; el filtro dentro del desplegable; navegar desde profesor, asignatura, grupo y subgrupo
a sus actividades; más información en la tabla de actividades; un curso inicial de uso (fuera del
criterio: guía de uso).

Material en `/home/luis/educhronos-aceptacion/s188/` (`f1/`, `f2/`, `f3/`).

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
Ensayada otra vez en S187 sobre `v0.2.0`, con las mismas cifras.
**S188, en la demo:** MEDIDA, NO CUMPLIDA. Una descarga de cuatro, `horario-1-grupo.pdf`, que pasa el
oráculo (1285/0/0 en 28 páginas, idénticas a las de S187); no hay PDF de profesores ni de aulas, ni
CSV, y la causa no se midió.

## Ejercicio de muestra (condición 4)

PENDIENTE, por tarea: inicio, final, aclaraciones y ayudas (literal). Nombre que dio al curso
2025/2026 al archivarlo. Tiempo de generación y estado del solver.

Protocolo de la generación en la demo: pendiente de la versión de `O-pre-demo`, que cambia el
tiempo de cálculo (decisión L).
**S188, en la demo:** MEDIDA, NO CUMPLIDA; detalle por tarea en «Demo con la secretaria (S188)». La
tarea 5 no se cumplió (cuatro actividades borradas en vez de reasignarse, BYG4 sin plazas); el
duplicado, las horas, la disponibilidad y la generación se hicieron. Nombre que dio al curso
2025/2026 al archivarlo: «2025/2026». Generación: FEASIBLE, 603,165 s, con 10 minutos elegidos en el
diálogo de `v0.2.0`. Sin inicio ni final por tarea: ver las desviaciones.

## Inventario de huecos (condición 5)

Inventario de huecos (S189). Fuente: la lista que la secretaria dio directamente al usuario,
transcrita en S189 (decisión F). La prematrícula 2025/26 se aportó solo como ayuda y no se usó para
clasificar. El horario de guardias 2025/2026 (`docs_extra/Ejemplos_SJ/HorariosProfesores/Horarios de guardias.pdf`, no
versionado, sha256 `8791a132529bb837dcfe65ed97f22d3eed98186f7d2e4df3db45676014c53694`) es la prueba de la fila 9.
Criterio (decisión O): «meter» es por pantalla; «ya está» quiere decir representado y presente en la
base de partida (s137); «se puede meter», representable por pantalla y ausente; «no se puede», ese
dato no se guarda por pantalla, y se anota si su efecto se expresa de otro modo. Pruebas: lectura de
`v0.2.0` en `s189/f1/informe.md`.

| # | Tipo de dato que entrega | Clasificación | Nota | Deuda |
|---|---|---|---|---|
| 1 | Niveles, grupos y materias con sus horas | Ya está | 8 niveles, 28 grupos, 219 actividades; las horas viven en cada actividad | — |
| 2 | Bloques de optativas «elige 1 de N» | Ya está | 39 actividades de varias plazas | — |
| 2b | «Hasta completar 4 horas» (Bachillerato) | No se puede | No hay alumnos; se parte de subgrupos ya formados | `D-matricula-fuera` |
| 3 | Itinerarios | No se puede | Sin concepto; su efecto está repartido en subgrupos | `D-matricula-fuera` |
| 4 | Religión o Atención Educativa | Ya está | 12 bloques | — |
| 5 | Alumnos matriculados por optativa | No se puede | Ningún campo cuenta alumnos | `D-matricula-fuera` |
| 6 | Asignaturas de cada profesor | Ya está | A través de sus plazas, 59 de 59 | — |
| 7 | Disponibilidad de los profesores | Se puede meter | Por pantalla; la base no tiene ninguna | — |
| 8 | Horas de clase por profesor | No se puede | Solo derivada de las plazas; solo se avisa por exceso | `D-totales-sin-contraste` |
| 9 | Guardias: número por profesor y calendario | No se puede | Sin concepto ni cobertura por tramo; el recreo no admite sesiones | `D-guardias-sin-modelo` |
| 10 | Uso de las aulas | Ya está | Tipo en las 44 aulas, con `D-aula-tipo-sin-uso-real` | — |
| 10b | Capacidad de las aulas | Se puede meter | Se guarda, pero nada la usa | — |
| 11 | Tipo de aula exigido por asignatura | No se puede | Solo por API; el aula de cada plaza sí está | `D-S103-compat` |
| 12 | Cargos del equipo directivo y jefaturas de estudio | No se puede | Solo existe la tutoría | `D-cargos-sin-modelo` |
| 13 | Reuniones semanales de tutores y de equipo directivo | Se puede meter, con rodeo | Actividad de una plaza con sus profesores y sin subgrupos; caen en horas lectivas | `D-cargos-sin-modelo` |
| 14 | Tutores | Ya está | 28 tutores principales | — |

Recuento: 16 filas; 6 «ya está», 3 «se puede meter», 7 «no se puede». Cada «no se puede» tiene ficha
en §4 (R-incidencia). Las ayudas de la condición 4 se dieron de alta en S188.

**S188:** no se hizo. El usuario intentará recuperar los datos más adelante, con el paquete de 2026/2027
o con lo que ella cuente (decisión F).

## Incidencias

PENDIENTE: cada una con su `D-*`, o «ninguna». Salen de las ayudas de la condición 4 y de los
«no se puede» de la condición 5 (R-incidencia), anotando que salen de `O-demo-bundle`.

**S188, de la demo** (todas salen de `O-demo-bundle`). Nuevas, con sede en el objetivo (3):
1. `D-borrado-sin-control-de-horas`: borrar una actividad con profesor se permite y nada avisa de
   que un grupo queda por debajo de sus horas (T5).
2. `D-vista-horario-no-se-descubre`: no descubrió que los horarios se ven en pantalla y los buscó
   en el PDF (T2).
3. `D-error-poco-visible`: el 409 se muestra como un párrafo rojo sin fondo que no llamó la
   atención ni se entendió (T5).
4. `D-tiempo-generacion-poco-claro`: la línea del diálogo que explica los tiempos no bastó y
   preguntó para qué sirven (T8).

Ya registradas, con nota de S188:
5. `D-selectores-combobox`: el filtro es un campo aparte y no abre la lista (T2).
6. `D-arranque-sin-aviso-de-espera`: relanzó el programa creyendo que no arrancaba (T1).
7. `D-horario-irreversible`: el observador paró una generación previa a configurar.
8. `D-entidad-sin-actividades`: usó el PDF para saber qué daba BYG1 y pide navegar a las
   actividades (T5).
9. `D-filtro-por-codigo`: «Latín» no encuentra «LAT» (T6).
10. `D-actividad-ux`: el manejo de actividades le resultó engorroso, y sus nombres, confusos.
11. `D-curso-sin-borrado`: preguntó si se pueden borrar cursos anteriores (T9).
12. `D-tiempo-elegido-no-se-recuerda`: revisada con la demo, no se dio.

**S189, del inventario** (todas salen de `O-demo-bundle`). Nuevas, con sede en el objetivo (3):
13. `D-matricula-fuera`: la matrícula no entra en Educhronos: ni alumnos por optativa, ni
    itinerarios, ni preferencias (inventario (S189), filas 2b, 3 y 5).
14. `D-guardias-sin-modelo`: las guardias no son un dato: ni su número por profesor, ni su cobertura
    por tramo, ni las de recreo (inventario (S189), fila 9).
15. `D-totales-sin-contraste`: los totales que entrega el centro no se pueden declarar ni contrastar
    con lo configurado (inventario (S189), fila 8).
16. `D-cargos-sin-modelo`: no hay cargos, y sus reuniones solo entran montadas a mano (inventario
    (S189), filas 12 y 13).

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
`/home/luis/educhronos-aceptacion/s188/`: `f1/` (instantánea y diagnóstico de la VM), `f2/` (los seis
ficheros traídos, `md5-windows.txt`, la salida de Windows y las medidas) y `f3/` (M2 de solo
lectura sobre `v0.2.0`), cada una con su `MANIFIESTO.sha256`. La instantánea `s188-demo-hecha` guarda
el estado de la VM tras la demo (S188).
`/home/luis/educhronos-aceptacion/s189/`: `f1/` (lectura de solo lectura de `v0.2.0` para el
inventario) y `f2/` (sha256 del horario de guardias, diff y medidas de esta edición), cada una con
su `MANIFIESTO.sha256`.

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
