# Sistema de gestión del proyecto — Educhronos
<!-- INDICE:INICIO -->
<!-- Generado en M1 (M-doc-3). Líneas INDICATIVAS; manda el texto. -->

- L70 — ## 1. Estado final del proyecto
- L99 — ## 2. Hitos
- L114 — ### Hitos: valor, dependencias, orden
- L137 — ## 3. Objetivos técnicos
- L148 — ### H2 — Configurar un centro desde cero
- L150 — #### O-shell — "La aplicación es navegable." ✔ TERMINADO (S100)
- L166 — #### O-catálogo — "Creo los elementos simples del centro." ✔ TERMINADO (S106)
- L238 — #### O-estructura — "Expreso la complejidad real del centro." ✔ TERMINADO (S114)
- L488 — #### O-demo — "El centro real funciona de punta a punta." ✔ TERMINADO (S137)
- L775 — #### O-particiones — "Un grupo nuevo entra en el curso sin reconfigurar a mano." ✔ TERMINADO (S141)
- L876 — #### O-disponibilidad — "El centro introduce la disponibilidad de su profesorado." ✔ TERMINADO (S167)
- L975 — ### H1 — Ajustar (cierre)
- L977 — #### O-ajuste-cierre — "El ajuste manual está completo y verificado." ✔ TERMINADO (S146)
- L1154 — #### O-diseño — "La aplicación tiene un aspecto cuidado y coherente." ✔ TERMINADO (S133)
- L1445 — #### O-navegación — "La aplicación se maneja como una aplicación de escritorio." ✔ TERMINADO (S127)
- L1635 — ### H3 — Exportar
- L1637 — #### O-exportación — "El horario sale de la aplicación, en papel y en datos." ✔ TERMINADO (S150)
- L1698 — ### H4 — Instalar y pasar de curso
- L1700 — #### O-instalación — "La aplicación se instala, arranca y se cierra en un Windows limpio sin ayuda técnica." ✔ TERMINADO (S156)
- L1874 — #### O-curso — "El centro empieza el curso siguiente sin perder el anterior." ✔ TERMINADO (S163)
- L1969 — #### O-aceptación — "La cadena entera de §1 pasa en un Windows limpio." ✔ TERMINADO (S172, con salvedad)
- L2038 — #### O-ci — "Cada cambio se prueba solo, y cada versión sale construida y aceptada." TERMINADO (S177)
- L2074 — ### H5 — Un profesor hace el horario 2026/2027 con Educhronos
- L2076 — #### O-demo-bundle — "El centro ve su curso en Educhronos y dice qué le falta." ✔ TERMINADO (S189, con salvedad)
- L2125 — #### O-pre-demo — "La secretaria elige cuánto espera, ve que se está generando y encuentra lo que busca en las listas." ✔ TERMINADO (S186)
- L2172 — #### O-base-tecnica — "Cada versión adapta las bases anteriores, dice qué versión es y deja rastro de lo que hace." ✔ TERMINADO (S193)
- L2207 — #### O-carga-2026 — "El horario oficial de 2026/2027 entra en Educhronos, en una base aparte que la secretaria no ve."
- L2236 — #### O-datos-centro — "La secretaria declara los totales del centro y ve si cuadran, y mete reuniones, funciones y cargos sin rodeos."
- L2275 — ## 4. Clasificación del trabajo pendiente
- L2293 — ### Clasificación de las deudas vivas actuales
- L2300 — #### Objetivos disfrazados de deuda → se PROMUEVEN a objetivo (§3)
- L2307 — #### Deuda técnica real, colgada de su objetivo
- L2441 — #### Mejora futura, cuelga y espera
- L2490 — #### Decisión arquitectónica consciente → sale de la cola
- L2504 — #### Limitación conocida → sale de la cola, se documenta el "no se hará"
- L2516 — #### Deuda de MÉTODO → se integra en `metodo.md`, no en el producto
- L2525 — #### Deuda ya CERRADA (histórico, no pendiente)
- L2592 — ## 5. Revisión del roadmap: por qué H2 va primero
- L2657 — ## 6. Reglas estratégicas
- L2747 — ## 7. Métricas del sistema
- L2768 — ## 8. El sistema respondiendo a las preguntas clave

<!-- INDICE:FIN -->


Este documento gobierna **qué se hace y por qué**. Responde a: ¿por qué se abre
esta sesión? ¿qué objetivo avanza? ¿qué hito acerca? ¿cuándo se deja de refinar
una pieza?

Es uno de tres documentos con responsabilidad separada:
- **`gestion_proyecto.md`** (este) — planificación: el mapa Hito→Objetivo→Cambio,
  la clasificación del trabajo pendiente y las reglas estratégicas.
- **`metodo.md`** — método: cómo se ejecuta una sesión (M0–M5, R4/R5, tipos de
  sesión). Referencia estable; no se relee en cada apertura.
- **`plan_trabajo_horarios.md`** — registro: qué pasó (progreso, decisiones
  permanentes de stack, fases completadas, notas técnicas).

La AUTORIDAD sobre los hechos del código es la documentación del repo, no la
memoria. Este documento se deriva de `plan_trabajo_horarios.md` (roadmap y
criterios de las Fases 8–12) y de `modelo_datos_fase1.md` (§6, casos de
validación del modelo). Donde algo no se puede deducir de esa documentación, se
marca **[LAGUNA]** en vez de inventarlo.

---

## 1. Estado final del proyecto

El proyecto está TERMINADO cuando existe un **guion de aceptación end-to-end que
un humano ejecuta sobre el bundle de Windows y pasa entero**, sin tocar la base
de datos a mano ni editar ningún JSON:

1. Instalar el bundle en un Windows limpio (sin Java/Node previos, sin permisos
   de administrador).
2. Crear un centro desde cero por la interfaz: profesores, aulas, asignaturas,
   grupos, currículo, desdobles, agrupamientos, PDC, tutores y la disponibilidad
   del profesorado (restricciones horarias duras y blandas).
3. Generar un horario que respeta las restricciones duras y minimiza las blandas.
4. Ajustarlo a mano con drag & drop, viendo los conflictos duros y blandos por
   celda, con posibilidad de bloquear sesiones antes de relanzar.
5. Exportar el horario a PDF (por grupo, por profesor, por aula) y a CSV.
6. Duplicar el curso para el año siguiente, conservando la configuración y
   dejando el anterior en solo lectura.

Cada eslabón corresponde a un criterio de verificación ya escrito en las Fases
8–12 del plan. La novedad de esta definición es EXIGIRLOS COMO UNA SOLA CADENA
EJECUTABLE, no como casillas independientes. Mientras el guion falle en cualquier
paso, el proyecto no está terminado, por muchos tests unitarios verdes que haya.

Hoy el guion falla en el paso 4 y en el paso 5. **CORREGIDO en S142:** esta frase decía «falla en el paso 2: no existe interfaz para crear un centro», y dejó de ser cierta al cerrar H2 en S141 —el centro se crea entero por pantalla—. Falla en el **paso 4** porque el ajuste a mano no existe como tal: medido en S142, soltar una clase en la rejilla emite un PIN de tramo (`POST /api/bloqueos`), no la reubica, y no hay ninguna vía de API para mover una sesión —no existe `SesionController` y la única escritura sobre `sesionRepository` en todo `src/main` es el `saveAll` de la generación—, así que el horario sólo cambia regenerándolo entero. Y falla en el **paso 5** porque la exportación a PDF y CSV no está empezada (H3 al 0 %). **ACTUALIZADO en S146:** el paso 4 deja de fallar como eslabón al cerrar H1 —las cinco condiciones de O-ajuste-cierre cumplidas y verificadas sobre el centro real—. Hoy el guion falla en el paso 5 (exportación, H3 al 0 %), y los pasos 1 y 6 pertenecen a H4 (~10 %), cuyos criterios no se han verificado: la frase anterior nombraba sólo el 4 y el 5 y callaba los dos de H4. La cadena entera, que es lo que este apartado exige, no se ha ejecutado nunca. **ACTUALIZADO en S147:** H3 abre con `O-exportación` (§3), con el criterio escrito sobre medición. El paso 5 sigue fallando —0 de 5 condiciones—, pero ya falla contra un criterio verificable. Su prueba en Windows real deja de ser de H3: se ejecuta con el paso 1, dentro de H4. **ACTUALIZADO en S150:** el paso 5 deja de fallar como eslabón al cerrar H3; su prueba en Windows real (Excel y PDF) queda en H4. Quedan los pasos 1 y 6, los dos de H4, y la cadena entera sigue sin ejecutarse nunca. **ACTUALIZADO en S151:** H4 abre con `O-instalación` (§3), con el criterio escrito después de medir en Linux y en dos Windows: 0 de 9 condiciones. El paso 1 sigue fallando, ahora contra un criterio verificable; el paso 6 espera su propio objetivo. **ACTUALIZADO en S156:** el paso 1 deja de fallar como eslabón al cerrar `O-instalación`, verificado en un Windows 11 con cuenta estándar y el bundle llegado por USB; con él queda verificado en Windows real también el paso 5. Queda el paso 6, que espera su propio objetivo, y la cadena entera sigue sin ejecutarse nunca. **ACTUALIZADO en S158:** el paso 6 abre con `O-curso` (§3), con el criterio escrito sobre medición: 0 de 9 condiciones. La ejecución de la cadena entera sale de ese objetivo y será un objetivo propio de aceptación, posterior. **ACTUALIZADO en S159:** el paso 2 se amplía con la disponibilidad del profesorado. Medido en S159: el backend (`GET/PUT /api/profesores/{id}/restricciones-horarias`, S78) y el solver (S26) la soportan, pero ningún código del frontend alcanza ese endpoint ni lo ha alcanzado nunca (`git log -S` vacío), y los tres bancos del centro real tienen 0 restricciones. El paso 2 vuelve a fallar para un centro real; lo recoge `O-disponibilidad` (§3), que va tras `O-curso`. **ACTUALIZADO en S163:** el paso 6 deja de fallar como eslabón al cerrar `O-curso`, verificado en Linux y, en sus condiciones 1 a 5, en el bundle sobre Windows 11 con cuenta estándar. Queda el paso 2 (`O-disponibilidad`), y la cadena entera sigue sin ejecutarse nunca: la ejecutará el objetivo de aceptación, que aún no tiene ficha. **ACTUALIZADO en S164:** el paso 2 abre con `O-disponibilidad` (§3), con el criterio escrito sobre medición: 0 de 7 condiciones. **ACTUALIZADO en S168:** el paso 2 deja de fallar como eslabón al cerrar `O-disponibilidad` en S167, verificado en Linux; el cierre de S167 no escribió esta nota. La cadena entera abre con `O-aceptación` (§3), en H4, con el criterio escrito sobre medición: 0 de 4 condiciones. El M2 de S168 midió dos huecos: los pasos 2, 3 y 4 no se han ejecutado nunca en el bundle de Windows —ni siquiera se ha generado allí un horario—, y el paso 5 pinta en una sola celda una actividad de más de un tramo sin que el oráculo de exportación lo detecte. **ACTUALIZADO en S169:** el paso 3 se ejecuta por primera vez en el bundle de Windows: en Windows 11 con cuenta estándar genera un horario OPTIMAL con 0 violaciones duras sobre un centro sintético (condición 2 de `O-aceptación`). La cadena entera sigue sin ejecutarse. **ACTUALIZADO en S170:** el paso 5 pinta una actividad de más de un tramo en todas las celdas que ocupa —rejilla, CSV y los tres PDF— y el oráculo de exportación lo comprueba (condición 3 de `O-aceptación`). La cadena entera sigue sin ejecutarse. **ACTUALIZADO en S171:** los pasos 2 a 6 se ejecutan por primera vez seguidos, por pantalla y por un humano, en el ensayo en Linux de `docs/guion-aceptacion.md` (condición 1 de `O-aceptación`), y pasan su oráculo. La cadena entera, con el paso 1 en Windows, sigue sin ejecutarse: es la condición 4. **ACTUALIZADO en S172:** la cadena entera se ejecuta por primera vez en el bundle, por un humano, en Windows 11 Pro con una cuenta estándar nueva y sin Java ni Node (condición 4 de `O-aceptación`). Los pasos 1 a 5 pasan su oráculo; el paso 6 falla en §7.5 tal como está escrito por una generación hecha a mano fuera del guion, y cuatro capturas faltan o no muestran lo exigido. El usuario da la corrida por válida con salvedad, contra la recomendación del arquitecto de repetirla (acta en `docs/actas-aceptacion/acta-s172.md`). Con esa salvedad, el estado final de este apartado queda alcanzado; H4 no cierra, porque queda la Fase 12. **ACTUALIZADO en S173:** abre `O-ci` (§3), el objetivo de la Fase 12, con el criterio escrito sobre medición: 0 de 4. Su condición 4 repite la cadena entera sobre el bundle que construye la CI; una corrida válida deja atrás la salvedad de S172. **ACTUALIZADO en S177:** la cadena entera se ejecuta sobre el bundle que construye la CI (`v0.1.0`), por un humano, en Windows 11 con cuenta estándar nueva y el zip llegado por USB, y los seis pasos pasan su oráculo sin salvedad (acta en `docs/actas-aceptacion/acta-s177.md`). La salvedad de S172 queda atrás.

**ESTADO FINAL 2 (S178, decidido por el usuario).** El estado final de arriba se alcanzó en S177, sin salvedad. El proyecto sigue con uno nuevo, que lo presupone y no lo sustituye: **un profesor del centro, sin guion ni ayuda técnica, crea el curso 2026/2027 en Educhronos, sobre el bundle de Windows y partiendo del curso 2025/2026 que ya tiene cargado, y obtiene un horario válido; ese horario se compara con el oficial del centro con métricas escritas antes de mirar.** Educhronos no se usa en producción en el curso 2026/2027 (decisión del usuario): ese curso es el banco de prueba real. Lo recorre el hito H5 (§2), y los fallos y peticiones que salgan del uso se tratan por R-incidencia (§6). En S178, H5 no tiene ningún objetivo abierto. **S190 (decidido por el usuario):** la prueba la hace la secretaria, que es profesora del centro y será quien maneje la aplicación; deja sin efecto la «persona nueva» de la decisión N de `O-demo-bundle`. La prueba con otro profesor y un manual de usuario quedan como idea, sin planificar. En 2026/2027 el centro ya no indica el aula de cada actividad, así que H5 incluye que la elija el sistema. El resto del estado final no cambia.

---

## 2. Hitos

Un hito es un resultado VISIBLE para el usuario: algo que se le puede enseñar
funcionando. Los cuatro hitos se derivan del estado final y de reagrupar los
seis criterios de verificación de la Fase 8 (que mezclaban "ajustar" y
"configurar" bajo un mismo título) por VALOR DE USUARIO.

| Hito | El usuario puede… | Estado real | Criterio de terminado |
|---|---|---|---|
| **H1 — Ajustar un horario existente** ✔ TERMINADO S146 | Ver un horario, ver conflictos duros y blandos, **bloquear** sesiones (arrastrar PINCHA la instancia en su tramo), relanzar | **Sin porcentaje: no es medible hoy.** **CORREGIDO en S142**, detectado por Claude Code al verificar el cierre. Decía «~90 %», cifra que ya no se sostiene: de las cinco condiciones del criterio nuevo (§3) hay UNA cumplida y verificada —el pin de tramo—, y la condición 1 no está incumplida sino que describe **superficie que no existe** (recolocar una sesión no tiene endpoint, validación ni persistencia), así que no hay denominador honesto sobre el que calcular un avance. Un porcentaje sobre un criterio que acaba de AGRANDARSE afirmaría más de lo medido. **ACTUALIZADO en S145.** Sigue sin porcentaje y por la misma razón, pero el estado ya es medible por conteo de condiciones: CUATRO de las cinco están cumplidas y verificadas sobre el centro real —(1) y (2) con sus dos mitades, (4) y (5)—, y queda UNA, la prevalidación S8 de la condición (3), que es también el único Cambio abierto del objetivo. La columna «El usuario puede…» deja de ser falsa en su primera mitad: arrastrar YA reubica, por intercambio sobre celda ocupada y por movimiento sobre celda vacía, y el pin dejó de colgar del arrastre para tener gesto propio. **CERRADO en S146:** las cinco condiciones cumplidas y verificadas sobre el centro real; la última, la (3), con `C-prevalidacion-s8`. Segundo hito terminado del proyecto, tras H2 (S141) | **Ver el criterio REESCRITO en §3 (ficha de O-ajuste-cierre), que sustituye a la remisión a los criterios 1–4 de Fase 8.** **CORREGIDO en S142**, dos veces y las dos por medición. (a) La columna «El usuario puede…» decía «moverlo con drag & drop», y es FALSO: soltar una clase en la rejilla emite un PIN de tramo (`POST /api/bloqueos` con `aulas: []`, `horario-view.ts:284`), NO la reubica; la rejilla ni siquiera se mueve. Y no existe vía de API para mover una sesión —no hay `SesionController` entre los trece controladores, y la única escritura sobre `sesionRepository` en todo `src/main` es el `saveAll` de `GeneradorHorarioService:277`—, así que el horario sólo cambia regenerándolo entero. (b) «Cumplidos salvo verificación de cadena» YA NO APLICA: hay medición. De los cuatro criterios, el 4 (sesión bloqueada que no se mueve al relanzar) está VERIFICADO POR MUTACIÓN sobre el centro real en S142; el 1 no puede cumplirse porque el gesto que describe no existe; el 2 se pinta siempre y sin selección, con el detalle en `title`; y el 3 declara «catálogo sano» sobre un centro que a continuación agota los 600 s. Por eso el criterio se reescribe en §3 en vez de remendar esta celda. Se conserva lo dicho en S135 (R5): esta fila decía «y el gesto de despinar», apoyándose en `D-F8.6-ii-b`; el M0 de S134 midió que esa deuda está CERRADA desde S83 (bloque 8.6-iii-B1 marcado, la ficha remite a S83 y la bitácora lo confirma; S84 le añadió cobertura) y el gesto EXISTE |
| **H2 — Configurar un centro desde cero** ✔ TERMINADO S141 · **REABIERTO S159** · ✔ **CERRADO de nuevo S167** | Crear profesores, aulas, grupos, currículo, desdobles, PDC, tutores por formularios y llegar a un horario válido sin tocar la BD | ~70% (O-shell hecho S100; O-catálogo TERMINADO S104, criterio precisado S106: 4 de 4 entidades CRUD por UI — Profesor (S101), Aula (S102), Asignatura (S103), Grupo (S104). El e2e UI→solver, antes 2ª mitad de O-catálogo, se reasignó a O-estructura en S106 al medirse que depende de currículo/jornada. O-estructura ABIERTO S107, 7 piezas hechas: C-jornada (S107, backend REST `/api/jornada` + formulario singleton, dimensión temporal del solve), C-subgrupos (S108, CRUD de subgrupos por UI sobre `/api/subgrupos`, con multiselect de grupos), C-actividades COMPLETO (trozo A en S109 —editor de Actividad de una plaza + guarda 409 del PUT + fin del vaciado de la BD en cada arranque— y trozo B en S110 —lista de plazas variable con alta/baja e I2 en cliente, con lo que desdobles, agrupamientos y bloques de optativas quedan construibles por UI—), C-niveles (S111, CRUD de Nivel por UI: cerraba el hueco medido en S109 —sin niveles por UI no hay grupos ni subgrupos— y con él las nueve filas del centro mínimo son construibles por pantalla) y C-e2e (S112, el e2e de navegador que crea el centro mínimo por la UI y verifica que el solver produce horario: TERCERA PATA del criterio, CUMPLIDA; incluyó arreglar un hueco funcional real de la primera generación) y C-pdc (S113, alta/consulta/borrado del grupo PDC por UI desde la fila de su padre + dos guardas de backend que impiden que el CRUD plano deshaga el agregado: con él el caso §6.2 del modelo —en su versión válida, la Nota (S23)— se construye íntegramente por pantalla y el solver produce horario sobre él, que es la SEGUNDA PATA demostrada en su caso más difícil) y C-tutores (S114, la asignación del tutor por UI sobre el sub-recurso que existía desde S77: su M0 midió que SÍ hacía falta —tres casos del §6 registran `ProfesorTutoria` en su configuración— y su M4 verificó en navegador que `TUTORIA_SIN_TUTOR` aparece sin tutor y desaparece con él; con ella las TRES PATAS quedan cumplidas y O-estructura CIERRA). «Desdobles y agrupamientos» dejó de ser trabajo propio al medirse que son actividades multiplaza. **O-estructura ✔ TERMINADO S114**, 8 piezas. **O-demo ABIERTO S115** y descompuesto en cinco Cambios; la misma sesión midió que el criterio 6 de Fase 8 no tiene constructor y lo sacó a objetivo propio, **O-particiones**, así que H2 pasa a cerrar con DOS objetivos por delante y no uno. **2 piezas: C-derivación (S115) y C-cargador (S116), con el que el IES completo —804 escrituras por la API REST— está en la base creado por las vías legítimas del producto; y, fuera de alcance, la primera prueba de que ese centro GENERA horario (FEASIBLE, objetivo 188.0), que despeja el mayor riesgo abierto del objetivo). C-generación EN CURSO desde S117, que resolvió dos de sus tres preguntas: la reconciliación de `sesion` (770/819 filas frente a 632 instancias: dos magnitudes distintas, no una discrepancia) y la caracterización del presupuesto (no gobierna la calidad sino la probabilidad de obtener horario; el defecto de 30 s es indefendible). **SEGUNDA PARTE en S118 (primera sesión de Desarrollo desde S114): la PIEZA DE PRODUCTO cerrada —presupuesto configurable `educhronos.solver.max-segundos` con defecto 600, separación 503 PRESUPUESTO_AGOTADO / 422 CATALOGO_INFACTIBLE / 422 CONFIGURACION_INCOMPLETA, y estado de espera en la vista— y el PRIMER HORARIO DEL CENTRO REAL generado por la vía de producción DESDE LA INTERFAZ (770 sesiones, el oráculo de S117 clavado). **C-generación CIERRA en S119 con la TERCERA parte: el contraste con el PDF.** Su M0 fijó qué se asevera como «válido» —el PDF no juzga la validez, es oráculo de CONTENIDO— en tres capas, y las tres se midieron sobre el horario de S118: **cero violaciones de regla dura sobre 770 sesiones** a escala real; **conservación de la carga con 526 claves, 11 divergentes todas de FPB y delta 49 idéntico al déficit predicho antes de mirar**; y los blandos recomponiendo el objetivo sin residuo (174 + 0 + 18 = 192). De paso se detectó y eliminó una circularidad en el mapa de códigos de grupo. **C-carga-manual-1eso, tras TRES sesiones sin decidirse, se decide y se ejecuta en S120** en versión recortada por ancho: el caso §6.1 —bloque de seis destinos alternativos con subgrupos de DOS grupos, y co-docencia de LCL— tecleado A MANO por la interfaz sobre base vacía, 38 envíos, veinte minutos, horario válido en 1 s y **ningún caso inexpresable**; con él la nota de alcance del criterio 5 se ESTRECHA (queda sin demostrar por UI la escala y el descubrimiento del modelado, porque el guion decía qué construir) y O-demo se queda **SIN TRABAJO EJECUTABLE**. **O-demo sigue ABIERTO**: faltan las 11 actividades de FPB (D31-a). **DESBLOQUEADO en S134**: no hubo correo, el arquitecto entrevistó al jefe de estudios y D31-a quedó SALDADA —lo que el horario no imprime va a Taller 4 (1º FPB) y Taller 5 (2º FPB), regla y no once datos—, con lo que la deuda bloqueante del proyecto baja a 0 por primera vez. **O-demo AVANZA en S135 con C-centro-completo HECHO, y es el mayor salto del objetivo desde S116:** alta del aula Taller 5, aula fija en las 11 plazas, carga del centro entero DESDE BASE VACÍA por la API REST (816 escrituras previstas, 816 escritas, diez familias cuadrando por GET: 44 aulas, 219 actividades, 316 plazas, 334 subgrupos, 28 grupos) y horario generado al primer intento pese a que los 28 grupos pasan a holgura cero: `FEASIBLE`, 819 filas de `sesion` clavando el oráculo rederivado del catálogo, delta 49 sobre las 770 de S118. Las tres capas de S119 remedidas a escala real: **cero violaciones de regla dura sobre 819**, **conservación de la carga con 526 claves y CERO divergentes (1301 = 1301)** —en S119 divergían 11, todas de FPB— y la identidad del objetivo sin residuo (188 + 0 + 15 = 203). **Con esto las DOS primeras patas del criterio 5 quedan demostradas a escala real. O-demo NO cierra**: falta juzgar «presentable al centro», y el obstáculo conocido son los 28 tutores, que S134 midió INCORRECTOS y no incompletos**. **S136 DEFINE Y MIDE la tercera pata con `C-presentable`, y corrige el diagnóstico de S134:** «presentable» queda escrito como «ningún dato mostrado contradice lo que el centro dijo de sí mismo, y lo que no tiene fuente aparece como ausente y no como inventado»; y de los 28 tutores 21 están BIEN (los 16 ordinarios de ESO y FPB casan 16/16 contra el PDF, los 5 PDC heredan el del padre), faltan 5 co-tutorías de ORI1 y sólo los 7 de Bachillerato son falsos —y son PORTANTES: sostienen S8 en las seis actividades de bloque—. La mitad declarativa del criterio queda cumplida por `docs/salvedades-demo.md`; la otra mitad **no depende de trabajo técnico sino de que el centro entregue la lista oficial de tutores**, así que O-demo vuelve a quedarse sin trabajo ejecutable **REABIERTO en S159:** el paso 2 de §1 se amplía con la disponibilidad del profesorado, que no tiene interfaz (medido en S159). H2 gana un objetivo, `O-disponibilidad` (§3), que se abre tras cerrar `O-curso`. Lo construido hasta S141 no se toca. **ACTUALIZADO en S164:** `O-disponibilidad` ABIERTO, 0 de 7, con el criterio escrito sobre medición. La medición encontró que la DURA sólo veta el tramo de inicio de cada sesión y que el verificador no la comprueba; su arreglo entra en el criterio. **ACTUALIZADO en S165:** `O-disponibilidad` sigue en 0 de 7 con `C-dura-completa` en curso: verificador y prevalidación hechos; faltan el solver con bloques (la 5) y medir el cuerpo HTTP del 422 (la 6). **ACTUALIZADO en S166:** `O-disponibilidad` en 2 de 7 con `C-dura-completa` HECHO: el solver cumple la disponibilidad en todos los tramos de cada sesión, el cortafuegos se retira y el 422 del pin sobre DURA lleva su motivo. Queda `C-rejilla-disponibilidad` (condiciones 1 a 4 y 7). | Criterios 5–6 de Fase 8: "configurar centro desde cero → horario válido" (O-demo) y "crear grupo nuevo se incorpora a las particiones" (O-particiones, §3). NOTA DE ALCANCE (S115): con la carga del centro real entrando por la API REST, el criterio 5 queda demostrado por UI a escala del centro mínimo (e2e de S112) y no a escala real; ver la nota escrita en la ficha de O-demo |
| **H3 — Exportar** ✔ TERMINADO S150 | Obtener PDF por grupo/profesor/aula y CSV | 0 %. **ACTUALIZADO en S147:** abre con `O-exportación`, su único objetivo previsto, y su criterio se escribe sobre medición del centro real. **ACTUALIZADO en S148:** la condición 4 (CSV) CUMPLIDA y la 5 a medias —el CSV declara su codificación; el PDF no existe—; quedan las tres del PDF. Primera línea de código de exportación en el árbol. **ACTUALIZADO en S149:** el PDF existe y tiene su primera vista. `C-exportacion-pdf-grupo` entrega 28 páginas A4 de grupo verificadas celda a celda contra el oráculo (1285 entradas, 0 faltan y 0 sobran), con las horas del centro, tutor, leyenda y fuentes incrustadas. Las condiciones 1, 2 y 3 avanzan en su tercio de grupo y no se marcan: se enuncian sobre las tres vistas. Quedan profesor y aula. **CERRADO en S150:** `C-exportacion-pdf-profesor-aula` entrega las vistas de profesor y aula y las cinco condiciones de `O-exportación` quedan cumplidas y verificadas sobre lo que descarga el navegador. Tercer hito terminado del proyecto, tras H2 (S141) y H1 (S146). | **Ver el criterio de `O-exportación` en §3, que SUSTITUYE a los 4 criterios de Fase 9** (S147). El 1 y el 3 no eran medibles tal como estaban escritos —«buena legibilidad» y «toda la información necesaria», sin umbral—, el 4 (Windows) pasa a H4 y la fase omitía el PDF por aula que §1 exige |
| **H4 — Instalar y pasar de curso** ✔ TERMINADO S177 | Instalar en Windows limpio; duplicar curso | **0 %, CORREGIDO en S151** por medición. Decía «~10 % (Fase 0 validó empaquetado una vez)», pero la Fase 0 empaquetó sólo el `HelloOrTools` y no dejó ninguna orden en el repo: la aplicación real no se había empaquetado nunca. **ACTUALIZADO en S151:** abre con `O-instalación` (§3), 0 de 9 condiciones. El app-image ya se construye, arranca y exporta con el oráculo a cero en Linux y en Windows, pero ninguna condición está cumplida. **ACTUALIZADO en S152: 2 de 9.** `C-construccion-reproducible` cumple la condición 1 —reescrita, porque `jpackage` no construye cruzado: son DOS guiones versionados, uno por plataforma— y la condición 2, con el runtime recortado a 14 módulos: **231.691.863 B** frente al límite de 250.000.000, margen 18,3 MB, medido en tres corridas que dieron el mismo número al byte. El procedimiento vive en `docs/empaquetado.md`. **ACTUALIZADO en S153: 3 de 9.** `C-datos-usuario` cumple la condición 7: la base deja de nacer en el directorio de trabajo y vive en la carpeta de datos del usuario (`%LOCALAPPDATA%\Educhronos` en Windows, el directorio XDG en el resto), verificado en las dos plataformas; las condiciones 1 y 2 se re-verifican sobre el bundle nuevo, que crece 4.083 B hasta 231.695.946 B. **ACTUALIZADO en S154: 7 de 9** (celda escrita en S155: S154 no la actualizó y el verificador no lo detectó, `D-censo-r4-ciego-a-los-objetivos`). `C-arranque-cierre` cumple la 4, la 5, la 6 y la 8: modo escritorio (instancia única, navegador, bandeja, diálogos, log) y escucha sólo en 127.0.0.1; la carpeta de Windows mide 231.715.485 B. **ACTUALIZADO en S155: 8 de 9.** `C-rutas-spa` cumple la condición 9: F5 y una URL directa a una vista la sirven desde el jar. Queda sólo la condición 3, la prueba final en Windows limpio con cuenta estándar, que sigue sin máquina. **ACTUALIZADO en S156: 9 de 9, `O-instalación` TERMINADO.** `C-windows-limpio` cumple la condición 3 en un Windows 11 Home con cuenta estándar, sin Java ni Node y con el bundle llegado por USB: ningún diálogo de seguridad ni de credenciales, y las cuatro descargas pasan el oráculo (1285/0/0, 835/0/0, 819/0/0, CSV OK). Windows 10 sale del criterio por decisión del usuario. H4 NO cierra: quedan la Fase 10 (duplicar curso) y la Fase 12 (CI), sin objetivo escrito todavía **ACTUALIZADO en S158:** abre `O-curso` (§3), segundo objetivo de H4, con el criterio escrito sobre medición: 0 de 9 condiciones. Queda la Fase 12 (CI), sin objetivo escrito. **ACTUALIZADO en S159:** `O-curso` en 2 de 9 con `C-duplicado-guarda` (backend del duplicado y de la guarda de solo lectura); H2 reabierto con `O-disponibilidad`, que va tras `O-curso`. **ACTUALIZADO en S160:** `O-curso` en 6 de 9 con `C-selector-curso`: el curso se duplica, se cambia y se marca como de solo lectura desde la interfaz. Quedan el horario vigente, el e2e y Windows. **ACTUALIZADO en S161:** `O-curso` en 7 de 9 con `C-horario-vigente`: «Horario» lleva al último horario del curso abierto, también en uno archivado, y sus cuatro descargas pasan el oráculo. Quedan el e2e y Windows. **ACTUALIZADO en S162:** `O-curso` en 8 de 9 con `C-e2e-curso`: el e2e duplica, cambia de curso y ve rechazada una escritura en el archivado, y la suite e2e pasa a correr en serie con carpeta de datos propia. Queda Windows. **ACTUALIZADO en S163: `O-curso` TERMINADO, 9 de 9**, con `C-windows-curso`: las condiciones 1 a 5 se verifican en el bundle, en Windows 11 Pro con cuenta estándar, construido y probado en una máquina virtual nueva. H4 NO cierra: queda la Fase 12 (CI), sin ficha, que va tras `O-disponibilidad` y tras el objetivo de aceptación. **ACTUALIZADO en S168:** H4 gana un tercer objetivo, `O-aceptación` (§3), que ejecuta la cadena entera de §1 y no tenía hito asignado: 0 de 4. Va antes de la Fase 12, por el orden de S163. **ACTUALIZADO en S169:** `O-aceptación` en 1 de 4 con `C-windows-generacion`: el bundle genera en Windows 11 con cuenta estándar y su diagnóstico no tiene violaciones duras; el nativo de OR-Tools carga con las DLL de Visual C++ que trae el propio runtime. **ACTUALIZADO en S170:** `O-aceptación` en 2 de 4 con `C-exportacion-bloques`: una actividad de varios tramos aparece en todos los que ocupa en la rejilla, el CSV y los PDF, y el oráculo de exportación lo comprueba, con una mutación que lo demuestra; salda `D-proyeccion-sin-duracion`. **ACTUALIZADO en S171:** `O-aceptación` en 3 de 4 (S171: condición 1, guion) con `C-guion-aceptacion`: `docs/guion-aceptacion.md` existe con su centro, y un humano lo ha recorrido por pantalla en Linux con los pasos 2 a 6 pasando su oráculo. **ACTUALIZADO en S172: `O-aceptación` TERMINADO, 4 de 4, CON SALVEDAD por decisión del usuario** (`C-corrida-aceptacion`): un humano ejecuta el guion entero en el bundle de `8d9a74a` sobre Windows 11 Pro con cuenta estándar nueva; pasos 1 a 5 con su oráculo, y el paso 6 con la salvedad del acta. H4 NO cierra: queda la Fase 12 (CI), sin ficha. **ACTUALIZADO en S173:** abre `O-ci` (§3), cuarto objetivo de H4 y último del hito, con el criterio escrito sobre medición: 0 de 4. Sus cuatro condiciones sustituyen a los tres criterios de la Fase 12, que no eran verificables tal como estaban escritos. **ACTUALIZADO en S174:** `O-ci` en 1 de 4 con `C-bundle-por-tag`: un tag `v*` construye el bundle de Windows en GitHub Actions y lo publica en la Release; verificado con `v0.1.0-rc.1`, con la carpeta del app-image idéntica al byte a la aceptada en S172. **ACTUALIZADO en S175:** `O-ci` sigue en 1 de 4. `C-e2e-verde` cumple la condición 2 en local: el rojo de `centro-minimo` no era del producto, sino un spec anterior al diálogo de confirmación de S145. La mitad en CI espera a la condición 1. **ACTUALIZADO en S176:** `O-ci` en 3 de 4 con `C-pipeline-push`: cada push a `main` ejecuta en GitHub Actions `mvn test`, vitest y el e2e, y un fallo en cualquiera pone su job en rojo; el e2e corre allí sin reintentos, lo que completa la condición 2. Queda la condición 4, la aceptación del bundle de CI. **ACTUALIZADO en S177: `O-ci` TERMINADO, 4 de 4,** con `C-aceptacion-ci`: la cadena entera pasa sobre el zip de la Release `v0.1.0` en un Windows 11 limpio, sin salvedad, y la actualización sobre la instalación de S172 conserva los datos. **H4 TERMINADO.** | Criterios de Fases 10, 11 y 12, y el 4 de Fase 9 —«la exportación funciona en Windows»—, trasladado aquí en S147 porque sólo se verifica con el bundle (precedente: Fase 6 → Fase 11) **AMPLIADO en S168:** y el criterio de `O-aceptación` (§3), la cadena entera de §1 en un Windows limpio. Está en H4 por su valor —la condición de entrega— y porque se ejecuta sobre el bundle, no porque sea instalar ni pasar de curso: el nombre del hito no la describe. Crear un H5 contradiría la definición de hito de este apartado, porque la aceptación no da al usuario ninguna capacidad nueva. |
| **H5 — Un profesor hace el horario 2026/2027 con Educhronos** (S178) | Un profesor del centro, sin guion ni ayuda técnica, duplica el curso 2025/2026 cargado en Educhronos, lo ajusta al 2026/2027 y genera su horario, sobre el bundle de Windows | `O-demo-bundle` ABIERTO (S180), objetivo activo desde S186; 3 de 7 en S182 (§3), con la condición 1 rehecha sobre `v0.2.0` en S187 (`s187-demo-lista`), con su versión publicada (`v0.2.0`, S186); demo con la secretaria en S188 (`C-demo-secretaria` EN CURSO), con las condiciones 3 y 4 medidas y no cumplidas y la 5 sin hacer; **TERMINADO en S189 con salvedad** (7 de 7; 3 y 4 con salvedad, decisión N; inventario con cuatro huecos nuevos en §4); `O-pre-demo` TERMINADO en S186 (abierto en S183, 6 de 6), intercalado antes de la demo (decisión L). Cinco previstos, en orden: (1) demo con el curso 2025/2026 en el bundle (`O-demo-bundle`), que deja la base de partida del profesor con su md5 citado —dónde se instala lo decide el (3)— y produce las primeras incidencias; (2) carga de los PDF de 2026/2027 en una base aparte que el profesor no ve, en cuanto el centro los entregue; (3) preparación de la prueba: `D-esquema-sin-version`, `D-version-invisible` y las incidencias de la demo que la bloqueen; (4) la prueba del profesor; (5) la comparación con el horario oficial. **S182 (decisión L de `O-demo-bundle`):** antes de la demo se intercala `O-pre-demo` —más tiempo de cálculo, bloqueo con barra durante la generación y filtro en los selectores de entidades—; la demo espera a su versión y su criterio se escribe al abrirlo, sobre medición. **Medido en S180:** los dos bancos del centro (S137), anteriores a S159, abren en `v0.1.0` sin migración —solo se añade la tabla `curso`— y el cargador de S116 usa los mismos endpoints en `v0.1.0`. Sin medir: si los PDF nuevos tienen el formato de los de 2025/2026. **S190: REPLANIFICADO** (§3, bloque «Replanificación de H5 (S190)»): ocho objetivos en orden; el primero, `O-base-tecnica`, ABIERTO en S190. **S191:** `O-base-tecnica` en 1 de 4 (`C-esquema-versionado`). **S192:** `O-base-tecnica` en 3 de 4 (condiciones 1, 3 y 4); `C-version-y-rastro` EN CURSO: queda la condición 2 en su parte de interfaz y su verificación sobre un bundle construido desde un tag. **S193:** `O-base-tecnica` TERMINADO, 4 de 4 (`C-version-y-rastro` HECHO, Release `v0.3.0`). No queda ningún objetivo abierto. **S195:** `O-carga-2026` ABIERTO, objetivo activo, 0 de 5 (§3). **S196:** `O-carga-2026` en 1 de 5 (`C-volcado-grupos` HECHO). **S197:** `O-carga-2026` en 2 de 5 (`C-volcado-profesores` HECHO); no se parte en dos objetivos (decisión del usuario). **S198:** `O-carga-2026` en 3 de 5 (`C-catalogo-2026` HECHO). **S199:** `O-carga-2026` TERMINADO, 5 de 5 (`C-carga-2026` HECHO). **S200:** `O-datos-centro` ABIERTO, objetivo activo, 0 de 5 (§3). **S201:** `O-datos-centro` en 1 de 5 (condición 1; `C-actividad-sin-alumnos` EN CURSO, con la verificación de la condición 4 pendiente). **S202:** `O-datos-centro` en 2 de 5 (condición 4; `C-actividad-sin-alumnos` HECHO; `C-totales-y-cargo` EN CURSO, con su contrato cerrado). **S203:** `O-datos-centro` en 4 de 5 (condiciones 2 y 3; `C-totales-y-cargo` HECHO). | Cada objetivo escribe su criterio al abrirse, sobre medición. La demo y la comparación no son hitos propios por la vara de S168: no dan al usuario una capacidad nueva. Las métricas de la comparación se fijan antes de mirar, con las tres capas de S119 como base. Avisos: el horario oficial puede cumplir reglas que Educhronos no modela, y `D-generacion-no-reproducible` obliga a comparar varias corridas o a saldarla |

### Hitos: valor, dependencias, orden

- **H1 — Ajustar.** Valor solo: BAJO (ajustar un horario que no se puede crear
  por UI no sirve a un usuario real). Depende de: nada (opera sobre datos
  sembrados). Riesgo de rehacer: MEDIO — el shell de H2 reubicará esta vista.
- **H2 — Configurar.** Valor solo: ES EL PRODUCTO. Sin esto no hay aplicación
  usable. Depende de: un shell donde alojar formularios. Desbloquea: H1 se vuelve
  útil, H3 tiene datos reales que exportar, H4 tiene algo que instalar.
- **H3 — Exportar.** Valor: ALTO y cobrable (es lo que el usuario se lleva
  impreso). Depende de: un horario (H1) sobre datos reales (H2). Bajo riesgo de
  rehacer (es hoja).
- **H4 — Instalar/curso.** Valor: es la condición de entrega. Depende de: todo lo
  anterior estable.

**Orden recomendado: H2 → cierre de H1 → H3 → H4.** Justificación en §5. **ESTADO en S146:** H2 cerrado en S141 y H1 en S146; el siguiente es H3, que todavía no tiene ningún objetivo escrito en §3. **ESTADO en S147:** H3 ABIERTO con `O-exportación`, objetivo activo del proyecto, con el criterio escrito sobre medición y 0 de 5 condiciones cumplidas. **ESTADO en S150:** H3 CERRADO. Queda H4, que no tiene todavía ningún objetivo escrito en §3. **ESTADO en S151:** H4 ABIERTO con `O-instalación`, objetivo activo del proyecto, 0 de 9 condiciones. La Fase 10 (duplicar curso) y la Fase 12 (CI) serán objetivos propios; `O-instalación` va primero porque decide dónde viven los datos, y de eso depende la Fase 10. **ESTADO en S156:** `O-instalación` TERMINADO. H4 sigue abierto y SIN objetivo activo: la Fase 10 (duplicar curso) y la Fase 12 (CI) no tienen todavía ficha en §3. **ESTADO en S158:** H4 ABIERTO con `O-curso`, objetivo activo del proyecto, 0 de 9 condiciones. La Fase 12 (CI) sigue sin ficha; la cadena entera de §1 será un objetivo propio de aceptación, y su orden respecto a la Fase 12 se decide al cerrar `O-curso`. **ESTADO en S159:** H2 se reabre con `O-disponibilidad` (§3), registrado y NO abierto: `O-curso` sigue activo. Orden: `O-curso` → `O-disponibilidad` → Fase 12 y aceptación, cuyo orden entre sí se sigue decidiendo al cerrar `O-curso`. Va tras `O-curso` porque no hay dependencia en ningún sentido; antes de la aceptación porque ésta ejecuta el paso 2; y antes de la Fase 12 por el argumento de S157: la CI protege la cadena entera. **ESTADO en S163:** `O-curso` TERMINADO. Orden decidido al cerrarlo (usuario, a propuesta del arquitecto): `O-disponibilidad` → objetivo de aceptación → Fase 12. La aceptación va antes que la CI por el mismo argumento de S157: la CI protege la cadena, y la cadena sólo queda validada cuando la recorre la aceptación. Pesa además que la máquina virtual de construcción (S163) abarata el bundle manual de Windows, que era lo principal que la Fase 12 iba a automatizar. Coste asumido: los arreglos que salgan de la aceptación se harán sin CI, sólo con las suites locales. **ESTADO en S164:** `O-disponibilidad` ABIERTO, objetivo activo del proyecto, 0 de 7 condiciones. El orden no cambia. **ESTADO en S167:** `O-disponibilidad` TERMINADO y H2 CERRADO de nuevo. No queda ningún objetivo abierto. Sigue el orden de S163: el objetivo de aceptación, que no tiene ficha en §3, y después la Fase 12. **ESTADO en S168:** `O-aceptación` ABIERTO en H4, objetivo activo del proyecto, 0 de 4 condiciones. El orden no cambia: después, la Fase 12. (El
acabado visual transversal —O-diseño, §3— no es un hito funcional. **CORREGIDO en
S121:** este paréntesis decía «va tras cerrar H1»; la revisión de S115 en §5 lo
dejó desfasado y el M0 de S121 lo detectó como costura. El orden vigente hacia la
demo es O-demo → O-diseño → demo → O-particiones → cierre de H2, y O-diseño abrió
en S121 sin esperar a H1.) **ESTADO en S172:** `O-aceptación` TERMINADO con salvedad (condición 4, por decisión del usuario). No queda ningún objetivo abierto. Sigue la Fase 12 (CI), que no tiene ficha en §3. **ESTADO en S173:** `O-ci` ABIERTO en H4, objetivo activo del proyecto, 0 de 4 condiciones. Es el último objetivo planificado. **ESTADO en S174:** `O-ci` en 1 de 4 (condición 3). Sigue el e2e en verde (condición 2). **ESTADO en S175:** `O-ci` en 1 de 4; condición 2 cumplida en local. Sigue la pipeline de push (condición 1). **ESTADO en S176:** `O-ci` en 3 de 4 (condiciones 1, 2 y 3). Sigue la aceptación del bundle de CI (condición 4), con la actualización y la guía de distribución. **ESTADO en S177:** `O-ci` TERMINADO y H4 TERMINADO: los cuatro hitos están cerrados y no queda ningún objetivo planificado. Lo siguiente es decidir qué se planifica. **ESTADO en S178:** el usuario fija el estado final 2 (§1) y el hito H5, con cinco objetivos en orden: demo → carga de los PDF de 2026/2027 → preparación → prueba del profesor → comparación. La carga va antes de la prueba para contrastar el curso del profesor en cuanto lo termine: qué ajustó bien, qué dejó sin ajustar y qué no supo expresar. Si los PDF se retrasan, la prueba no los espera. `D-esquema-sin-version` y `D-version-invisible` bloquean H5 (§4). No hay ningún objetivo abierto. **ESTADO en S180:** `O-demo-bundle` ABIERTO en H5, objetivo activo del proyecto, 0 de 7 condiciones. Ninguna de las dos deudas bloquea la demo (§4); siguen en el objetivo (3). **ESTADO en S181:** `O-demo-bundle` en 2 de 7 (condiciones 2 y 6, con `C-preparacion-demo`); la condición 1 espera al ensayo. **ESTADO en S182:** `O-demo-bundle` en 3 de 7 (condición 1 cumplida sobre `v0.1.0` con el ensayo; `C-preparacion-demo` HECHO). Por la decisión L, antes de la demo se intercala `O-pre-demo` (sin abrir; su criterio se escribe al abrirlo); `O-demo-bundle` queda ABIERTO esperando su versión. **ESTADO en S183:** `O-pre-demo` ABIERTO en H5, objetivo activo del proyecto, 0 de 6 condiciones (`C-alcance-pre-demo` HECHO). `O-demo-bundle` sigue ABIERTO en 3 de 7, esperando su versión. **ESTADO en S184:** `O-pre-demo` en 4 de 6 (condiciones 1 a 4, `C-generacion-controlada` HECHO); quedan el filtro (5) y la versión (6). `O-demo-bundle` sigue ABIERTO en 3 de 7, esperando su versión. **ESTADO en S185:** `O-pre-demo` en 5 de 6 (condición 5, `C-filtro-selectores` HECHO); queda la versión (6). `O-demo-bundle` sigue ABIERTO en 3 de 7, esperando su versión. **ESTADO en S186:** `O-pre-demo` TERMINADO, 6 de 6 (condición 6, `C-version-pre-demo` HECHO: Release `v0.2.0` y generación de 60 minutos verificada en la VM). `O-demo-bundle` sigue ABIERTO en 3 de 7 y pasa a objetivo activo: su condición 1 se rehace sobre `v0.2.0` (decisión L). **ESTADO en S187:** `O-demo-bundle` en 3 de 7, con la condición 1 rehecha sobre `v0.2.0` (`C-preparacion-demo-v020` HECHO: sustitución en la VM, instantánea `s187-demo-lista` y ensayo entero). Queda la demo con la secretaria (condiciones 3, 4, 5 y 7), según su fecha. **ESTADO en S188:** `O-demo-bundle` en 3 de 7, con la demo con la secretaria hecha (`C-demo-secretaria` EN CURSO): las condiciones 3 y 4, medidas y no cumplidas, esperan al inventario (5) para decidir entre salvedad y repetición (decisión M); la 7, en curso. **ESTADO en S189:** `O-demo-bundle` TERMINADO con salvedad, 7 de 7 (`C-demo-secretaria` HECHO). No queda ningún objetivo abierto. Siguiente, por el orden de S178: la carga de los PDF de 2026/2027 (2), en cuanto el centro los entregue; si se retrasan, la prueba no los espera. **ESTADO en S190:** H5 replanificado por el usuario (§3). `O-base-tecnica` ABIERTO, objetivo activo. Los PDF de 2026/2027 llegaron en S190 (`docs_extra/Ejemplos_SJ/2026-2027/`, no versionados; manifiesto en `s190/f2/MANIFIESTO-2026-2027.sha256`, 11 ficheros). **ESTADO en S191:** `O-base-tecnica` en 1 de 4 (condición 1, `C-esquema-versionado` HECHO). Sigue `C-version-y-rastro`. **ESTADO en S192:** `O-base-tecnica` en 3 de 4 (condiciones 3 y 4 cumplidas con `C-version-y-rastro`, EN CURSO). Queda la condición 2: la línea de versión en pantalla y su verificación sobre un bundle construido desde un tag. **ESTADO en S193:** `O-base-tecnica` TERMINADO, 4 de 4 (condición 2 con `C-version-y-rastro`, Release `v0.3.0`). No queda ningún objetivo abierto. Higiene/Método, aplazada desde S190 y con el disparador saltado otra vez en S193, es la primera candidata; por el mapa de S190 siguen `O-carga-2026`, intercalable, y `O-datos-centro`. **ESTADO en S194:** Higiene/Método por la excepción a R-apertura (§6), con el disparador saltado en S193: cinco fichas de guiones y de tipos de sesión saldadas en `metodo.md`, de 16 a 11. No queda ningún objetivo abierto; siguen `O-carga-2026`, intercalable, y `O-datos-centro`. **ESTADO en S195:** `O-carga-2026` ABIERTO en H5, objetivo activo del proyecto, 0 de 5 condiciones (`C-alcance-carga-2026` HECHO). Después, `O-datos-centro`. **ESTADO en S196:** `O-carga-2026` en 1 de 5 (condición 1, `C-volcado-grupos` HECHO). Sigue `C-volcado-profesores` (condición 2). **ESTADO en S197:** `O-carga-2026` en 2 de 5 (condición 2, `C-volcado-profesores` HECHO). No se parte en dos objetivos (decisión del usuario a propuesta del arquitecto). Sigue `C-catalogo-2026` (condición 3). **ESTADO en S198:** `O-carga-2026` en 3 de 5 (condición 3, `C-catalogo-2026` HECHO). Sigue `C-carga-2026` (condiciones 4 y 5). **ESTADO en S199:** `O-carga-2026` TERMINADO, 5 de 5 (condiciones 4 y 5, `C-carga-2026` HECHO). No queda ningún objetivo abierto. Siguiente por el mapa de S190: `O-datos-centro`. **ESTADO en S200:** `O-datos-centro` ABIERTO en H5, objetivo activo del proyecto, 0 de 5 condiciones (`C-alcance-datos-centro` HECHO). Después, `O-aulas`. **ESTADO en S201:** `O-datos-centro` en 1 de 5 (condición 1, con `C-actividad-sin-alumnos` EN CURSO). Sigue la verificación de la condición 4 sobre copias de los bancos. **ESTADO en S202:** `O-datos-centro` en 2 de 5 (condición 4, `C-actividad-sin-alumnos` HECHO). Sigue `C-totales-y-cargo` (condiciones 2 y 3), con su contrato cerrado y cotejado. **ESTADO en S203:** `O-datos-centro` en 4 de 5 (condiciones 2 y 3, `C-totales-y-cargo` HECHO). Sigue `C-carga-datos-centro` (condición 5 y reconfirmación de la 4 sobre el esquema 3).

---

## 3. Objetivos técnicos

Un objetivo se define por el AVANCE QUE PRODUCE sobre el producto, no por el
componente de código que toca. Cada objetivo tiene propósito, criterio de
terminado, dependencias, valor y los cambios que agrupa.

Se desarrollan los objetivos de H1 y H2 (los calientes). H3 y H4 se descomponen
al abrirse; su descomposición es de bajo riesgo y está acotada por los criterios
de las Fases 9–12.
**CORREGIDO en S147, al abrir H3:** «de bajo riesgo» no se sostuvo para el criterio. De los cuatro criterios de Fase 9, el 1 y el 3 no eran medibles tal como estaban escritos, el 4 sólo se verifica con el bundle de H4, y la fase omitía el PDF por aula que §1 exige. El criterio de `O-exportación` se escribió midiendo antes, con la lección de S142.

### H2 — Configurar un centro desde cero

#### O-shell — "La aplicación es navegable." ✔ TERMINADO (S100)
- **Propósito:** carcasa de aplicación con navegación entre Configuración y
  Horario y una landing.
- **Terminado cuando:** se navega de una landing a cada sección y se vuelve, sin
  editar la URL a mano. **CUMPLIDO en S100** (verificado a mano en localhost:4200).
- **Depende de:** nada.
- **Valor:** convierte componentes sueltos en aplicación; es donde viven los
  formularios y la vista de horario.
- **Cambios que agrupa:** layout raíz, router de secciones, landing.
- **Absorbe:** D-UI-shell (que era un objetivo disfrazado de deuda). **D-UI-shell
  CERRADA en S100.**
- **Cierre (S100):** landing + sección Configuración (placeholder que O-catálogo
  rellenará) + barra de navegación persistente sobre el router ya existente. 11
  ficheros (6 nuevos, 5 sobrescritos); suite frontend 75 → 76; ningún componente
  de H1 tocado. Detalle en la cabecera S100 del plan.

#### O-catálogo — "Creo los elementos simples del centro." ✔ TERMINADO (S106)
- **Propósito:** CRUD con formularios de profesores, aulas, asignaturas, grupos.
- **Terminado cuando:** las 4 entidades de catálogo (Profesor, Aula, Asignatura,
  Grupo) se crean, listan, editan y borran desde la UI, y su escritura llega
  correctamente al backend REST existente. **CUMPLIDO en S104** (4/4 CRUD); el
  criterio se precisó en S106 (ver nota de recorte abajo). Criterio de entidades
  simples: no incluye currículo, jornada ni estructura, que son O-estructura.
- **Progreso (S104):** 4 de 4 entidades CRUD. Profesor HECHO (S101, commit `ddc6c48`),
  Aula HECHO (S102, commit `5094462`), Asignatura HECHO (S103, commits `7fa8278`
  entidad + `4f7dded` cableado) y Grupo HECHO (S104). Asignatura fue el CASO PLANO del
  molde (`AsignaturaController` byte por byte el de Profesor). Grupo = caso plano + UNA
  extensión: el desplegable `nivel` poblado por red (`nivel.model`/`nivel.service` solo-
  `listar()`); `tipo` fijo a ORDINARIO (decisión consciente §4); 409 ya en backend
  (`contarSubgrupos`+`contarGruposHijos`). El sub-recurso `aulas-compatibles` (S103) y
  `/{id}/tutoria` (S104) quedaron fuera de alcance por el mismo criterio; son Cambio
  propio, no CRUD plano.
  **Recorte de alcance (S106).** El criterio previo era AGREGADO con dos mitades: (1)
  crear un centro mínimo por UI y (2) el solver corre sobre él. El M2 de S106 midió
  contra el código que la 2ª mitad NO es alcanzable dentro de O-catálogo: el centro
  mínimo que pasa prevalidación y produce un solve son 9 filas irreducibles (Nivel,
  Grupo, Subgrupo, Profesor, Asignatura, Aula, ≥1 TramoSemanal lectivo, Actividad,
  Plaza; `GenerarHorarioEndpointTest.poblarCatalogoMinimo`), y solo 4 de ellas
  (Profesor, Aula, Asignatura, Grupo) tienen formulario. Las otras 5 van de "solo
  listar" (Nivel: existe endpoint, la UI solo hace `listar()`) a "sin controller"
  (TramoSemanal: bloqueo duro, sin rejilla `tramosLectivos=0` ⇒ PROFESOR/GRUPO
  SOBRECARGADO ⇒ 422). Subgrupo, Actividad y Plaza (la demanda curricular) solo tienen
  API. Esas piezas —currículo/demanda y estructura de jornada— son O-estructura por
  diseño (§3 O-estructura; §4 D22, frontera S103/S104). El criterio agregado se redactó
  (≤S104) sin haber medido esa dependencia; el recorte lo corrige. Es un cambio
  localizado en este documento, previsto por §5 ("decisión reversible: si al ejecutar
  se revela una razón para otro grano, es un cambio localizado, no un rehacer").
  La verificación e2e "el solver corre sobre un centro creado íntegramente por UI" se
  reasigna a O-estructura, que es quien construye las piezas que faltan (ver su criterio
  de terminado). Es el primer e2e del proyecto y su mayor riesgo abierto en H2, pero no
  puede ejecutarse hasta que exista la UI de estructura.
- **Depende de:** O-shell.
- **Valor:** primer centro creado sin SQL.
- **Cambios que agrupa:** un formulario CRUD por entidad de catálogo. El backend
  REST ya existe desde la Fase 6; esto es la capa de presentación.
- **Molde de CRUD de catálogo (CANON desde S102; era candidato de S101):** la primera
  entidad fijó el patrón y la segunda (Aula, S102) lo VALIDÓ con correcciones. Decidido
  y NO se rediscute: Reactive Forms tipados `nonNullable`; sin async validator de
  unicidad (el 400 del backend se presenta); form en diálogo CDK + `ConfirmarBorrado`
  genérico (ya existe); dos componentes lista+form; CRUD inline en `Configuracion`;
  servicio = wrappers pelados; traducción de error propia del componente (no
  compartida: tocaría H1); secuencia de tests propia por spec desde `(1)` por fichero
  (evita la global colisionada, D-S101-num). FORMA CANÓNICA precisada por el cotejo de
  S102 (5 correcciones al candidato de S101): `ConfirmarBorrado` recibe `string[]` (no
  `{ nombre }`); `DIALOG_DATA` es la entidad directa (`T | null`, no envuelta);
  estado del componente con signals + miembros `protected` (no campos planos);
  traducción `mensaje(err, degradado)` con degradado con forma `${texto} (${status}).`;
  la lista NO ordena en cliente (el `listar()` del backend ya llega ordenado). Además:
  `imports:` sin `standalone: true` explícito, valores iniciales por `setValue` en
  constructor, CSS con BEM `<entidad>__*`, runner vitest (`vi.fn()`), no Karma. Nota de
  molde para entidades con enum de dominio (aprendida en Aula): el selector ofrece solo
  los valores con semántica y OMITE los indefinidos, pero al EDITAR añade el valor
  preexistente si cae fuera de la lista, para no borrarlo en silencio.
  PENDIENTE aún de ≥2 entidades EN PANTALLA a la vez (ahora sí las hay): la decisión
  ruta-hija vs contenedor para la navegación de `Configuracion`, en Cambio propio.
- **Absorbe:** las deudas D-F8.5-* de "sin red bajo la aplicación" (I4, unicidad
  profesor-tramo). MATIZ medido en S101: NO son del CRUD de Profesor sino de tutoría
  (`ProfesorTutoria`) y disponibilidad (`ProfesorRestriccionHoraria`); se pagan con
  el formulario de SU entidad, no con cualquier CRUD de catálogo (ver §4). También
  D31 b/c/d (poblaciones a confirmar con el centro, al abrir el CRUD de cada nivel).
  D26 (nombre de aula) fue CERRADA en S102 como no aplicable (el aula se identifica
  por `codigo`; no hay `nombre` que poblar) — ya no cuelga aquí. D-F8.5-C3-a (COMUN
  sin semántica) queda CONTENIDA en UI por el form de Aula (COMUN fuera del selector)
  pero sigue viva a nivel de esquema.
- **Consulta útil:** `INFORME-RECONCILIACION.md` documenta las discrepancias reales
  entre los horarios de origen (familia D8); las decisiones que tomó son evidencia
  de qué casos reales deben soportar estos formularios.

#### O-estructura — "Expreso la complejidad real del centro." ✔ TERMINADO (S114)
- **Propósito:** configurar currículo/demanda, desdobles, agrupamientos, PDC,
  tutores desde la UI.
- **Terminado cuando:** los 8 tipos de sesión del modelo (§6 de
  `modelo_datos_fase1.md`) se pueden expresar por formulario; los casos de
  validación del §6 se reproducen desde la UI; y **un e2e de navegador crea un
  centro mínimo íntegramente por la UI y el solver corre sobre él** (heredado de
  O-catálogo en S106: es la prueba de que la UI de estructura funciona de punta a
  punta, y solo es ejecutable cuando O-estructura ya construye Nivel, Subgrupo,
  demanda curricular y jornada). Andamiaje Playwright ya instalado en S106
  (`app/frontend/e2e/`, humo verde); este objetivo lo reutiliza. Sujeto a la
  política e2e de §6. **TERCERA PATA CUMPLIDA en S112** (`e2e/centro-minimo.spec.ts`,
  un solo test por R-e2e). Precisión registrada en S112 sobre la segunda pata, que zanja
  una grieta que S110 dejó anotada: los «casos de validación del §6» son los del MODELO
  —reproducir por formulario los horarios reales del centro—, no la superficie de error
  de la UI; que un 400 pinte «Bad Request» (D-F8.6-ii-a) degrada la usabilidad pero no
  impide reproducir ningún caso. **SEGUNDA PATA DEMOSTRADA EN SU CASO MÁS DIFÍCIL en
  S113**: el §6.2 del modelo —el caso 3ºADi, y el que exigía el sub-recurso
  `/api/grupos/{idPadre}/pdc` que el CRUD de Grupo por UI no alcanza (lista blanca
  `ORDINARIO` de `GrupoService`)— se construyó ÍNTEGRAMENTE por pantalla y el solver
  produjo horario sobre él, en su versión válida (la Nota de Sesión 23: UN solo grupo Di
  con padre, subgrupo con `grupos={PDC}`, compartidas que se quedan en el ordinario), sin
  que nada del caso resultara inexpresable por formulario. RECORTE MEDIDO EN S113 sobre lo
  que la primera pata todavía exige: `VIRTUAL_OPTATIVA` —el otro tipo que la lista blanca
  bloquea— NO lo pide ningún caso del §6, no aparece en ninguno de los 44 fixtures del
  solver y ni siquiera existe como constante en el dominio del solver; no hace falta
  construir su formulario. **TUTORES RESUELTO EN S114, y la respuesta fue que SÍ hacía
  falta.** La duda estaba bien planteada —tutores figura en el PROPÓSITO y en los «Cambios
  que agrupa» pero NO en el texto del criterio, así que por R-terminado no podía construirse
  por simetría— y su M0 la resolvió MIDIENDO el §6 del modelo, no razonando desde la lista de
  Cambios: TRES de los seis casos (§6.1 con GH6, §6.5 con FIL2, §6.6 con PAU2) incluyen el
  registro `ProfesorTutoria` en su configuración y lo usan en su tabla de verificación de
  invariantes para declarar S8 ✅. Reproducir un caso es poder introducir SU CONFIGURACIÓN por
  formulario, y esa fila no tenía pantalla: es el mismo razonamiento con que S106 recortó
  O-catálogo (9 filas irreducibles, 4 con formulario). No cae en el recorte de S112, que
  excluyó la superficie de ERROR de la UI: `ProfesorTutoria` es dato del centro, contenido del
  modelo. El M2 lo reforzó por un flanco no previsto: la UI ya sabía CREAR el problema y no
  RESOLVERLO —`actividad-form` ofrece la casilla «Requiere tutor» desde S109 y no existía vía
  alguna de asignar el tutor que exige, de modo que marcarla producía un `TUTORIA_SIN_TUTOR`
  inevitable; `grupo-form.ts:45` tenía el hueco documentado como decisión—. **PRIMERA Y SEGUNDA
  PATAS CUMPLIDAS en S114**, con el alcance de la prueba declarado sin adornos: lo demostrado en
  M4 es que la ÚNICA fila que faltaba a esos tres casos ya es introducible y que S8 se satisface
  por la vía que el modelo describe (contraste medido en navegador: `TUTORIA_SIN_TUTOR` con sus
  tres celdas y el grupo nombrado antes de asignar el tutor, `violaciones: []` después). Los seis
  casos del §6 NO se han tecleado uno a uno; §6.3 y §6.4 se apoyan en el recorte medido en S113
  (usan subgrupos multi-grupo, es decir actividades multiplaza, demostradas en S110). El cierre
  descansa por tanto en un ARGUMENTO ESTRUCTURAL —cada pieza que esos casos necesitan está
  demostrada como expresable— y no en una reproducción exhaustiva; se declara así, como
  inferencia y no como medición, y se aceptó porque una sesión más de tecleo no podía descubrir
  ninguna pieza sin demostrar. Si O-demo destapara un caso inexpresable, es hueco funcional de
  H2 y se afronta allí, no reabriendo este objetivo.
- **Depende de:** O-catálogo.
- **Valor:** valida el MODELO UNIFICADO contra el usuario real. Es el objetivo de
  mayor riesgo del proyecto (si el modelo Actividad→Plaza→Subgrupo no se puede
  configurar de forma usable, hay que rediseñarlo) y por eso debe abordarse
  temprano, con margen.
- **Cambios que agrupa:** editor de demanda curricular, asistente de
  desdoble/agrupamiento (D1, D10), editor de PDC (D7), asignación de tutores,
  configuración de estructura de jornada (D22).
- **Progreso (S114):** ✔ CERRADO, 8 piezas. **C-jornada** (S107, D22):
  backend REST singleton `GET|PUT /api/jornada` (reemplazo total, guarda 409 ante
  dependientes, techo conservador ≤6 lectivos/día sin tocar `domain.Tramo`, malla
  expandida a los 5 días en el backend) + formulario singleton en Configuración
  (FormArray fijo, propuesta precargada, 409 diferenciado del 400). Retira
  `SeedCatalogoRunner`. Es la pieza del camino crítico del solve (sin ≥1 TramoSemanal
  lectivo el centro mínimo da 422). **C-subgrupos** (S108): CRUD de subgrupos por UI
  sobre `/api/subgrupos` (backend ya existía completo) —`subgrupo.model`/`service`,
  `subgrupo-form` con `<select multiple>` de grupos poblado por red (M3 real: control
  `string[]`, validator `arrayNoVacio` que replica I6, handler `alSeleccionar` +
  `[selected]` porque el `<select multiple>` no reconcilia el array como el único),
  `subgrupo-lista`, cableado en Configuración—. Es la pieza que faltaba para que las
  Plazas de una Actividad puedan referenciar subgrupos creados por UI (la Plaza los
  referencia por código de subgrupos ya existentes ⇒ deben ser creables antes que las
  actividades). Suite app sin tocar; vitest 180→206. Decisión de alcance del M2 (S108,
  medido contra el repo): NO falta backend de currículo, falta UI —`Subgrupo` y
  `Actividad`→`Plaza` ya existen con CRUD REST; `Particion`/`SubgrupoParticion`/
  `DemandaCurricular` NO existen (Particion por decisión S48, DemandaCurricular solo en
  el doc de modelo)—. Se adopta Opción A (currículo = subgrupos + actividades sin
  materializar Particion): el solver no consume Particion y expresar el §6.1 no la
  exige. **C-actividades, trozo A** (S109): el editor de Actividad con la plaza
  embebida, entregado TROCEADO tras medir el contrato real. Incluye dos piezas de
  backend que el Cambio destapó: (a) `schema.sql` dejó de dropear las 21 tablas en
  cada arranque —la aplicación vaciaba la base de datos al iniciarse, medido en
  ejecución; sin esto ningún formulario es demostrable de una sesión a otra ni O-demo
  es posible—; y (b) guarda 409 en el `PUT /api/actividades/{id}` ante cualquier
  dependiente, molde C-jornada. La guarda NO es cosmética: la reconciliación de plazas
  es POSICIONAL y muta filas vivas conservando su id, mientras `Sesion` y
  `AulaBloqueada` referencian a `Plaza` POR ID, así que sin ella eliminar una plaza
  intermedia dejaba una sesión del horario describiendo otra plaza distinta, sin error
  ni aviso. Frontend: `actividad.model`/`service`, `actividad-lista` y `actividad-form`
  con la plaza dentro de un `FormArray` de longitud fija 1 (molde `jornada`, para que
  el trozo B sea un delta), XOR de aula resuelto con un control de UI que no viaja al
  backend, y tres multiselects molde `subgrupo-form`. La lista BLOQUEA la edición de
  actividades multiplaza: un formulario de una plaza abriendo una de seis borraría las
  otras cinco. Suite app 259→261, vitest 206→239. **C-actividades, trozo B** (S110):
  cierra el Cambio. El `FormArray` de plazas se abre a alta y baja dirigidas por el
  usuario (mínimo 1 fila, que es regla del contrato; sin máximo, que el contrato tampoco
  tiene), `precargar` reconstruye una fila por plaza del dato con el molde
  `jornada.rellenar`, y entra la validación cruzada I2 en cliente como validador de
  ARRAY, replicando la semántica del backend con sus dos rarezas —no normaliza mayúsculas
  y deduplica dentro de la plaza— porque normalizar haría que el formulario rechazara
  cuerpos que la API acepta. Se retira la guarda de multiplaza de la lista: toda actividad
  vuelve a ser editable. Frontend puro: el backend ya aceptaba N plazas y sus cuatro tests
  de reconciliación (crecer, reducir, estabilidad de códigos, reuso de hueco) ya cubrían
  el PUT. Suite vitest 239→249, backend intacto 261/91. Verificado en navegador real: seis
  plazas construidas desde cero van y vuelven con su rama del XOR y sus multiselects
  marcados, y quitar la del medio deja cinco con el desplazamiento posicional previsto.
  Hallazgo de la campaña de mutación que costó una reescritura: el caso que protegía el
  `track` del `@for` NO lo protegía —el `@if` del XOR cura el desalineamiento en los nodos
  que miraba—; los únicos testigos válidos son los nodos fuera de todo `@if` enlazados por
  `formControlName`.
  **Hallazgo que RECORTA el objetivo** (medido, no supuesto): «desdobles y
  agrupamientos» NO es un Cambio propio. §4.6 del modelo es explícito —no existe campo
  `tipo`, la naturaleza estructural se infiere del contenido— y el test
  `roundTrip_bloqueSeisPlazas` lo confirma: un desdoble ES una actividad multiplaza.
  Con el trozo B (S110) la lista de plazas está abierta y esa capacidad queda ENTREGADA;
  lo que sobrevive del «asistente de desdoble» de la lista de Cambios es un atajo de UX,
  no una capacidad nueva.
  **C-niveles** (S111; hueco descubierto en S109). Con la base de datos vacía
  no se podía crear un Grupo desde la UI —no había seed, ni `data.sql`, ni migración, ni
  runner, y `nivel.service.ts` solo tenía `listar()`—, luego tampoco Subgrupo, luego las
  plazas se quedaban sin población, lo que hacía INEJECUTABLE el e2e del criterio. S111
  entregó el CRUD por UI sobre el molde plano de catálogo (backend con cero trabajo: ya
  existía completo desde S70) y lo VERIFICÓ recorriendo el centro mínimo entero en
  navegador: las nueve filas irreducibles —Nivel, Grupo, Subgrupo, Profesor, Asignatura,
  Aula, ≥1 tramo lectivo, Actividad, Plaza— se crean por pantalla y el solver produce
  horario. El e2e queda DESBLOQUEADO.
  **C-e2e** (S112): la TERCERA PATA del criterio, cumplida. `e2e/centro-minimo.spec.ts`,
  un solo test por R-e2e, monta las nueve filas por formulario y asevera que tras generar
  hay exactamente 3 `div.instancia` en la rejilla (3 = `repeticionesPorSemana`, con una
  plaza). Dos piezas que el Cambio exigió y que valen más que el test: (a) AISLAMIENTO —el
  andamiaje de S106 declaraba «BD limpia por construcción porque `schema.sql` dropea», y
  eso dejó de ser cierto en S109; el e2e habría corrido contra la base de trabajo. Ahora
  la BD es `app/educhronos-e2e.db`, la borra el `command` del `webServer` y
  `reuseExistingServer:false` impide reutilizar un backend de desarrollo—. (b) UN HUECO
  FUNCIONAL REAL, destapado por el M4 y arreglado en sesión: con `onSameUrlNavigation` en
  'ignore' (el defecto), navegar a `/horario/1` estando en `/horario/1` se descarta, así
  que tras la PRIMERA generación de una instalación nueva la rejilla no se recargaba y la
  pantalla se quedaba en el 404 de la carga inicial. Se manifestaba exactamente en el
  criterio 5 de Fase 8 que define H2, y era invisible en el uso manual repetido (el id
  cambia y la URL difiere). Arreglado con una bifurcación en el `next` de
  `lanzarGeneracion`, conservando intacta la decisión de S93 de recargar por GET fresco.
  El control de vacuidad del e2e lo demuestra: revertir el arreglo lo pone ROJO.
  NO cierra el objetivo: faltan PDC y tutores, y ambos son patas 1 y 2 del criterio.
  **C-pdc** (S113, D7): el alta, la consulta y el borrado del grupo PDC por UI, con el que
  la SEGUNDA PATA queda demostrada en su caso más difícil. Entra en dos mitades y la
  primera no estaba prevista. (a) DOS GUARDAS DE BACKEND, que este Cambio paga porque este
  Cambio las abre: el M2 midió CUATRO caminos por los que el usuario destruiría el PDC
  desde botones que la pantalla ya ofrecía —degradarlo a ORDINARIO abriendo su diálogo de
  edición y pulsando Guardar (el formulario inyecta `tipo:'ORDINARIO'` fijo y `validarTipo`
  solo mira el tipo del request); renombrar su subgrupo mono-Di, que deja el DELETE del PDC
  en 404 permanente porque el vínculo es por código derivado; borrar ese subgrupo, que deja
  el PDC huérfano y borrable por el CRUD plano; y añadir el grupo padre a la población del
  mono-Di, que el backend aceptaba y que produce el INFEASIBLE que la regla S23 existe para
  evitar, sin error visible ni pista alguna—. Los cuatro eran INALCANZABLES antes, porque
  sin PDC creable por UI no hay fila de PDC ni subgrupo mono-Di en las listas: no son deuda
  declinada, son un hueco que el propio Cambio abriría. G1 rechaza editar por el CRUD plano
  una entidad que no sea ORDINARIO; G2 declara que un subgrupo cuya población es EXACTAMENTE
  UN grupo `DIVERSIFICACION_PDC` pertenece al agregado PDC. El «exactamente uno» se corrigió
  en sesión: «que incluya un PDC» habría roto el ámbito compartido de 4ºESO, que S29 modeló
  como un subgrupo con los DOS Di dentro, caso legítimo del §6. Ambas devuelven 400 y no 409,
  que sigue reservado a `ReferenciaEntranteException`. (b) EL DIÁLOGO, colgado de una tercera
  acción por fila en la lista de grupos —no de una ruta hija: eso obligaría a resolver aquí la
  decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio propio, y a fijarla con un solo
  ejemplo delante—. Sus tres estados los gobierna la RESPUESTA DEL SERVIDOR y no `DIALOG_DATA`,
  que es lo que lo saca del molde de form de catálogo: 404 es el estado «sin PDC» y no un
  error, 200 pinta la ficha con su borrado, y el estado inicial es `cargando` porque pintar el
  alta mientras el GET viaja enseña «este grupo no tiene PDC» a un grupo que sí lo tiene.
  Vuelve la columna `Tipo` a la lista y las filas de PDC no ofrecen ninguna de las tres
  acciones, porque las tres fallarían y ninguna capacidad se pierde (el backend no tiene
  edición de PDC y su borrado vive en el diálogo del padre). Suites: app 261→268, vitest
  271→290, solver y e2e intactos.
  **C-tutores** (S114): la asignación del tutor por UI, OCTAVA pieza y la que CIERRA el
  objetivo. Frontend puro —el sub-recurso `GET|PUT /api/grupos/{id}/tutoria` existe completo
  desde S77 con 17 tests—: `tutoria.model`/`service`, `tutoria-dialogo` colgado de una cuarta
  acción por fila en la lista de grupos, molde `PdcDialogo`. Cuatro decisiones de diseño, y las
  dos primeras son las que importan. (1) EL DIÁLOGO EDITA EL PRINCIPAL PERO GUARDA LA LISTA
  ENTERA: el PUT es reemplazo total, así que los co-tutores se cargan, se pintan en solo lectura
  y se REENVÍAN INTACTOS; un formulario que solo conociera al principal los borraría en
  silencio, que es el género de destrucción que S113 previno con G1/G2. El alta y baja de
  co-tutores queda FUERA por R-terminado (ningún caso del §6 los pide). (2) TRES ESTADOS, NO
  CUATRO, y aquí el molde de S113 NO se traslada: `PdcDialogo` deriva el vacío de un 404, pero
  el GET de tutoría devuelve 200 con lista vacía, luego el «sin tutor» se deriva de
  `length === 0`. (3) I4 en cliente NO se replica: con un único desplegable de principal el
  escenario de dos principales es inalcanzable y el validador sería código muerto —misma familia
  que D-i2-dedup-cliente—; la red es el 400 del backend. (4) La opción «— sin tutor —» es
  seleccionable y el control va sin `required`, porque elegirla ES el gesto de quitar el tutor
  (PUT con `[]`: el sub-recurso no tiene DELETE). Dos hallazgos de la campaña de mutación, los
  dos de mutaciones NO pedidas: cerrar el diálogo con `true` al CANCELAR sobrevivía a los doce
  casos —contrato de cierre asimétrico, `true` significa «hubo escritura», y un cancelar
  mentiroso provocaría una recarga fantasma—, y el botón «Tutoría» abriendo `PdcDialogo`
  también, porque el aserto miraba el dato y no el componente. Ambos tapados. Corrección de
  método registrada: el M2 de esta sesión DESMINTIÓ un hecho que el arquitecto había afirmado
  como medido —«pintar un `<select>` antes de tener las opciones pierde la preselección»— que
  era analogía indebida con el `<select multiple>` de S108; Angular reconcilia el select único y
  hay test que lo congela desde S104 (`grupo-form.spec.ts:174`). El `forkJoin` se conservó por
  el argumento que sí lo sostiene (el gating de estados) y su TSDoc lleva un párrafo explícito
  sobre lo que NO arregla. El cableado NO recarga la lista al cerrar, y está documentado por
  qué: la tabla no muestra ningún dato de tutoría. Añadir una columna «Tutor» exigiría que
  `GrupoDTO` transportara la tutoría —mover el contrato por comodidad de pintura, el error que
  D-monodi-botones-inertes decidió no cometer—, así que queda fuera. El botón se pinta en TODAS
  las filas, incluidas las de PDC, y eso saca las tres acciones existentes del `@if` de
  ordinarios: un PDC hereda el principal del padre en el alta y puede editarlo después, luego
  excluirlo dejaría sin editar justo el caso que crea la herencia. Suites: vitest 290→310
  (3 ficheros nuevos), app/solver/e2e intactos. Bundle 514,42→520,22 kB (D-bundle-presupuesto).
- **Absorbe:** D1, D7, D10, D22, D30, D-F8.5-D1-b, y las deudas de subgrupos
  compartidos. D22 saldada de facto (C-jornada, S107). Nace y cuelga aquí
  D-subgrupo-ux-multiselect (S108): la UX del `<select multiple>` de subgrupos es
  mejora PLANIFICADA (fase de mejora de UX de subgrupos), no deuda técnica ni
  bloqueante (ver §4 y el plan). Nacen y cuelgan aquí, en S109,
  D-plaza-sin-subgrupos (técnica real: el backend acepta una plaza sin población) y
  D-actividad-ux (mejora planificada), esta última RECORTADA en S110 al cerrarse su tercer
  síntoma con la retirada del aviso de multiplaza. Nace y cuelga aquí, en S110,
  D-i2-dedup-cliente (deuda de test: la deduplicación intra-plaza del validador de I2 no
  la cubre nadie, y el escenario es inalcanzable desde la UI). Nacen y cuelgan aquí, en
  S111, D-horario-irreversible (técnica real, la más grave del objetivo: no hay forma de
  borrar un horario generado y eso congela permanentemente las actividades que usa) y
  D-error-generacion-pin (técnica real menor). Nacen en S111 pero NO cuelgan aquí:
  D-molde-mensaje-cubierto-en-form es de O-catálogo (cerrado, R-terminado) y
  D-log-aplicacion es transversal. Nacen y cuelgan aquí, en S112, D-e2e-retry-bd (un
  reintento de Playwright en CI correría sobre la BD del intento fallido) y
  D-e2e-aislamiento (la suite e2e corre en paralelo sin aislamiento entre specs; hoy
  inocuo porque solo un spec escribe), más D-props-test-obsoleto (el
  `application.properties` de test repite la premisa caducada que S112 corrigió en el
  `playwright.config.ts`). Nacen y cuelgan aquí, en S113, D-pdc-lista-rancia (técnica real de
  UX, la más visible del Cambio: el alta de un PDC toca DOS catálogos y solo recarga el que
  abrió el diálogo, así que la lista de subgrupos no se entera hasta un F5),
  D-pdc-sin-edicion, D-pdc-vinculo-por-cadena (técnica real: el agregado localiza su subgrupo
  por convención de código y no por referencia; G2 la CONTIENE, no la resuelve),
  D-pdc-sufijo-completo y D-monodi-botones-inertes. Nacen en S113 pero NO cuelgan aquí:
  D-bundle-presupuesto es de O-diseño (el bundle pasa de 507,66 a 514,42 kB frente a un techo
  de 500) y D-tokens-inexistentes es transversal y de costura R4 (la familia `D-nueva-*` se
  cita en nueve sitios del código sin tener definición en ningún documento). Nace en S112 pero
  NO cuelga aquí: D-doble-proyeccion-compartido
  es de O-ajuste-cierre, superficie de specs de la vista de horario. Y hereda el daño vivo de
  D-F8.6-ii-a, que S109 midió y amplió y S110 afinó desde el navegador: el mensaje
  accionable existe pero viaja como reason phrase y el cuerpo llega sin `message`, así que
  TODOS los formularios de este objetivo —incluido el 409 construido en S109— muestran
  «Bad Request» en vez del motivo. AFINADA por CUARTA vez en S113, que ejecutó la comprobación
  que la propia ficha reclamaba: la clave está puesta, está compilada y aun así el cuerpo llega
  sin `message`, luego «reactivar la clave» sale del abanico de arreglos POR MEDICIÓN. Los seis
  mensajes del flujo del PDC son genéricos («Bad Request» ×5 y «Conflict» ×1).
  Nace y cuelga aquí, en S114, D-tutor-pdc-desincronizado (técnica real: la herencia del
  principal al PDC corre solo en el alta). Nacen en S114 pero NO cuelgan aquí: D-s8-muda,
  D-diagnostico-no-es-foto y D-post-horario-sin-sesiones son de O-ajuste-cierre (las tres son
  superficie de la VISTA DE HORARIO, mismo criterio con que S113 dejó fuera D1-8 y D1-10), y
  D-dialogo-foco-perdido es de O-diseño. Ninguna bloqueaba el criterio, y por eso el objetivo
  cierra con ellas vivas (R-terminado): el M4 las encontró DESPUÉS de que las dos patas
  quedaran cumplidas, y ninguna impide expresar ningún caso del §6.

#### O-demo — "El centro real funciona de punta a punta." ✔ TERMINADO (S137)
- **Propósito:** cargar el IES de Sevilla completo y generar su horario.
- **Terminado cuando:** el criterio 5 de Fase 8 —«configurar un centro desde cero y llegar
  a un horario válido»— pasa sobre los datos reales del IES: el centro entero está en la
  base creado por las vías legítimas del producto, el solver produce un horario válido
  sobre él y el resultado es presentable al centro. PRECISADO en S115 en dos puntos: (1) el
  criterio 6 de Fase 8 NO forma parte de este objetivo (ver O-particiones); (2) «datos
  reales» significa el IES COMPLETO —28 grupos— y no una rebanada representativa, decisión
  del arquitecto en S115 motivada por la demo al centro. **PRECISADO EN EL M0 DE S136, la tercera
  pata:** «presentable al centro» no era juzgable tal como estaba escrito, así que ese M0 lo definió
  ANTES de medir, con la misma mecánica con que el M0 de S119 fijó qué asevera «válido» — **el
  resultado es presentable cuando ningún dato identificativo mostrado contradice lo que el centro dijo
  de sí mismo, y lo que no tiene fuente aparece como ausente y no como inventado**. LECTURA ESTRECHA
  y decidida con argumento: la ancha —«el centro lo aceptaría como horario usable»— metería la calidad
  dentro del criterio y el objetivo no terminaría nunca, contra lo que S119 decidió con R-terminado.
  Quedan FUERA la calidad (se mide y no se corrige) y los nombres truncados, que vienen así del origen
  y son incompletos, no falsos. La pata tiene DOS mitades: «nada falso» y «lo ausente, declarado».
  **CRITERIO CUMPLIDO EN S137 y OBJETIVO TERMINADO.** Las tres patas medidas a escala real: el centro entero
  creado por las vías legítimas del producto (816/816 escrituras por la API REST desde base vacía), un horario
  válido sobre él (819 sesiones, cero violaciones duras, 28 grupos a 30/30 y conservación con delta 0 contra el
  horario que el centro imparte) y presentable en la lectura estrecha de S136, con los 7 tutores de Bachillerato
  sustituidos por los oficiales del centro y la hoja de salvedades declarando lo que sigue ausente. Queda VIVA la
  NOTA DE ALCANCE de S115, estrechada en S120 y NO cubierta por este criterio: siguen sin demostrarse por interfaz
  la ESCALA y el DESCUBRIMIENTO del modelado.
- **Depende de:** O-estructura. **DESBLOQUEADO desde S114**. Es el juez natural del
  argumento estructural con que se cerró O-estructura: si algún caso del centro real
  resultara inexpresable por formulario, es hueco funcional de H2 y se afronta AQUÍ, no
  reabriendo el objetivo anterior. **EXAMEN PASADO en S115**, y este es el resultado que
  más pesa del Cambio: la derivación contrastó el catálogo completo contra la UI existente
  y todo cabe —asignatura NULL, plaza con dos profesores, actividad de 6 plazas, plaza con
  subgrupos de seis grupos, `duracionTramos > 1`, PDC con padre, recreo no lectivo—. El
  único candidato a inexpresable (11 plazas de FPB sin aula, rechazadas por
  `ActividadService.validarXor`) NO lo es: la plaza sin aula es configuración inválida por
  diseño y el dato falta en la FUENTE, no en el formulario.
- **Valor:** prueba de que H2 funciona sobre el centro para el que se construye.
- **Cambios que agrupa** (nombrados en el M0 de S115; la ficha no los tenía, y crearlos fue
  trabajo de esa apertura, como el M0 de S107 con O-estructura):
  - **C-derivación** ✔ HECHO (S115) — de los volcados al catálogo de entrada. Entregables
    `docs/horario-referencia/ESPECIFICACION-CATALOGO.md` y `catalogo-derivado.json`.
  - **C-cargador** ✔ HECHO (S116) — el script que lee el catálogo derivado y puebla el centro
    por la API REST. Entregables `tools/carga-centro/extraer-nombres.py` (que crea
    `docs/horario-referencia/nombres-derivados.json`) y `tools/carga-centro/cargar-centro.py`;
    `tools/` nace con él. **804 escrituras HTTP, diez familias cuadrando por GET**: niveles 8,
    asignaturas 100, profesores 59, aulas 43, grupos 28, subgrupos 334, actividades 208, plazas
    305, tutorías 28, jornada persistida. Constaba «BLOQUEADO hasta que el centro responda las
    aulas de FPB» y su M0 lo CORRIGIÓ por medición: el bloqueo era parcial —11 actividades de
    219, 804/815 cargables— y afectaba a declarar el centro COMPLETO (requisito de
    C-generación), no a construir ni ejecutar el cargador. Idempotencia verificada corriendo la
    carga tres veces: la segunda envía 0 altas.
  - **C-generación** ✔ HECHO (S117 + S118 + S119). Solve sobre el IES real: factibilidad, tiempo y contraste con el
    horario del PDF. El contraste NO exige igualdad, exige validez (ver la nota de
    restricciones abajo). **ARRANCA CON FACTIBILIDAD YA DEMOSTRADA (S116, fuera de alcance, por
    iniciativa del arquitecto): el IES real GENERA horario** —`estadoSolver: FEASIBLE`, objetivo
    188.0, 770 sesiones— y con ello se despeja el mayor riesgo abierto de O-demo. Su M2 hereda
    tres preguntas ya formuladas y una dificultad medida: (1) qué presupuesto necesita de verdad,
    porque el defecto de 30 s se agota siempre y 600 s bastan, pero nadie ha medido si bastan 90 o
    200 ni cuánto mejora el objetivo con más tiempo; (2) cómo se reconcilian las 770 filas de
    `sesion` contra las 632 sesiones semanales que el catálogo describe —no está medido cómo mapea
    una fila de `sesion`, así que la cifra no está ni bien ni mal, está sin explicar—; (3) el
    contraste con el PDF. La dificultad, medida en S116: **26 de los 28 grupos deben llenar sus 30
    tramos EXACTOS, holgura cero** (1FPB y 2FPB son los dos únicos con holgura, +24 y +25, por el
    recorte). Es un empaquetado perfecto y explica por qué CP-SAT no resuelve en 30 s y sí en 600.
    Aviso metodológico heredado: la demanda por grupo se cuenta DEDUPLICADA POR ACTIVIDAD, como
    hace el no-solape (`ModeloCpSat.java:1046`); sumar por plaza da 40–73 tramos y es la
    sobrestimación que advierte `PrevalidacionService.java:253-256`. Nacen en su terreno
    D-timeout-como-infactible y D-motivo-rechazo-sin-registro (§4).
    **PRIMERA PARTE HECHA EN S117: dos de las tres preguntas quedan RESUELTAS y el Cambio NO cierra.**
    (2) RECONCILIACIÓN CERRADA, y no había discrepancia: una fila de `sesion` es una PLAZA de una INSTANCIA
    en un tramo (`SolucionMapper.aSesiones`, bucles :113 × :114 × :129 con el `add` en el más interno), luego
    filas = Σ (repeticiones × plazas) — `duracionTramos` no multiplica y aquí vale 1 en todo el catálogo—.
    Da **770 con las 208 actividades cargadas y 819 con las 219 del catálogo completo**; **632 es otra
    magnitud**, Σ repeticiones, es decir INSTANCIAS, idéntica a las celdas del PDF de donde se derivó. La
    diferencia 819 − 632 = 187 son las plazas extra de las actividades multiplaza. Salvedad: `aSesiones` lanza
    si alguna instancia no está colocada, así que 770 demuestra guardado COMPLETO y no calidad. **819 queda
    como oráculo aritmético para cuando lleguen las aulas de FPB.**
    (1) PRESUPUESTO CARACTERIZADO, y la respuesta cambia la forma de la pregunta: **el presupuesto no gobierna
    la calidad sino la PROBABILIDAD de obtener horario.** Cuatro pasadas, 43 corridas sobre copia y sin
    persistir (arnés desechable que encadena `cargarProblema()` + solver sin llamar a `guardar()`). Tasa de
    éxito acumulada por la vía de producción, sumando S116: 5 s 0/1 · 30 s 0/4 · 40 s 0/3 · 45 s 1/3 · 50 s
    0/3 · 60 s 1/9 · 90 s 1/1 · 120 s 3/4 · 180 s 3/4 · 300 s 1/1 · 600 s 2/2. Entre 120 s y 180 s la calidad
    no es distinguible (rangos 240–286 y 222–262, solapados). **El defecto de 30 s es indefendible** y lo
    defendible hoy es 600 s, con la salvedad de que son 2 corridas en una máquina de 8 núcleos.
    HALLAZGO ESTRUCTURAL: **el cuello de botella son las RESTRICCIONES DURAS, no la optimización.** La vía de
    factibilidad pura (`SolverHorario.resolver`, modelo sin objetivo) da 0 de 6 a 30 y 60 s, y con 900 s
    resuelve 5 de 5 en **844–872 s** (dispersión 3,3 %): el catálogo TIENE solución y el solver la encuentra de
    forma fiable, con un tiempo de primera solución en torno a los 14,3 minutos. Queda DESCARTADO que el centro
    real esté en el límite de lo que este solver resuelve. Precisión que evita una conclusión falsa: las dos
    vías construyen modelos DISTINTOS (`construir()` vs `construirConObjetivo()`) y no son muestras de la misma
    distribución; quitar el objetivo NO acelera. Corolario sobre D23: `resolverOptimizandoConSemilla` toma una
    `SolucionHorario` que su javadoc espera de `resolver`, y esa semilla cuesta ~860 s, luego el warm start no
    es palanca aplicable sin cachear la semilla, y eso es objetivo propio.
    NACEN AQUÍ, en S117: D-generacion-no-reproducible y D-prevalidacion-ciega-a-holgura-cero (§4). Nace en S117
    pero NO cuelga aquí: D-guion-exit-enmascarado, transversal y de método.
    LO QUE LE QUEDA AL CAMBIO, y por qué no cierra: su enunciado incluye «tiempo», y declararlo terminado
    dejando el defecto en 30 s sería declarar terminado justo lo que la medición demostró que está mal. Falta
    una pieza de PRODUCTO con tres ediciones en el mismo camino de fallo —calibrar el defecto, distinguir
    UNKNOWN de INFEASIBLE, dar señal durante la espera— y falta la pregunta (3), el contraste con el PDF, que
    no ha empezado.
    **SEGUNDA PARTE HECHA EN S118: la PIEZA DE PRODUCTO queda CERRADA y verificada en ejecución; el Cambio
    SIGUE EN CURSO porque la pregunta (3) no ha empezado.** Fue la primera sesión de Desarrollo desde S114
    (M0+M2+M3+M4+M1). Las tres ediciones: (a) el presupuesto pasa a la property `educhronos.solver.max-segundos`
    con defecto **600**, primera clave del espacio de nombres `educhronos.*` y primer `@Value` del proyecto
    —decisión de PRECEDENTE del arquitecto, motivada por H4: el bundle va a un centro cuyo portátil no
    conocemos y una constante compilada obligaría a recompilar—; (b) el mapeo de fallo separa `INFEASIBLE` →
    422 CATALOGO_INFACTIBLE, `UNKNOWN` → **503 con `Retry-After: 0`** PRESUPUESTO_AGOTADO, `null` (no hubo
    solve) → 422 CONFIGURACION_INCOMPLETA y el resto → 500, con el cuerpo construido en el controlador para no
    depender del mecanismo de error de Spring; (c) estado de espera en la vista, marcado mínimo y cero estilo
    por R-invalidación. **EL RESULTADO QUE MÁS PESA: el primer horario del centro real generado por la VÍA DE
    PRODUCCIÓN desde la INTERFAZ** —clic en navegador, POST de 600 s llegando vivo, `horarios 1`, `sesion` **770**
    (el oráculo aritmético de S117 clavado) y las 305 plazas colocadas—. Salvedad de calidad: `FEASIBLE`,
    objetivo 192.0 y **cota inferior 0.0**, gap abierto; es horario válido y completo, no óptimo. Verificado
    también que NADA corta un POST de 600 s (cero claves de timeout en `app/src`, sin interceptores en
    Angular) y que el e2e sigue en 2/2 y 14,6 s. Toda la generación corrió sobre COPIA: la canónica conserva
    su md5 y `sesion 0`, así que O-particiones no pierde su punto de partida. HALLAZGO QUE MATIZA S117: dos
    corridas CONSECUTIVAS a 600 s sobre la misma base dieron UNKNOWN y FEASIBLE, luego «600 s es el único
    punto sin fallos» deja de ser cierto y la tasa acumulada pasa a **3/4**; el 600 se mantiene porque el
    presupuesto se consume ENTERO siempre y subirlo se paga en cada corrida. Ese mismo hallazgo VALIDA el
    diseño: si el mismo botón unas veces da horario y otras no, el 503 reintentable no es un borde sino la
    mitad del comportamiento normal. Se PAGAN DE PASO D-timeout-como-infactible y D-generacion-sin-indicador
    (ambas CERRADAS); nace D-presupuesto-anunciado-espejo; se afinan D-generacion-no-reproducible,
    D-prevalidacion-ciega-a-holgura-cero y D-F8.6-ii-a (su causa REAL, medida: en Boot 4 la clave se llama
    `spring.web.error.include-message` y la del fichero es sintaxis de Boot 3, muerta desde la migración).
    Suites: app 268 → **282**, vitest 310 → **316**, solver 91 y e2e 2 intactas. LO ÚNICO QUE LE QUEDA AL
    CAMBIO: la pregunta (3), el contraste con el PDF, que exige decidir antes qué se asevera como «válido».
    **TERCERA PARTE HECHA EN S119 y CAMBIO CERRADO.** Sesión de MEDICIÓN (M0 + M2 en cuatro pasadas + M1, sin
    M3 ni M4 canónicos, cero líneas de producto, suites intactas). El trabajo del M0 fue el encuadre que S117
    dejó pendiente y que nadie había tomado: **el PDF NO puede juzgar la validez del horario generado** —los
    dos son distintos por diseño, porque los volcados no traen disponibilidades de profesor (S115)— sino que
    es ORÁCULO DE CONTENIDO. De ahí «válido» en TRES CAPAS con oráculos distintos: (1) validez formal contra
    el modelo, cero violaciones de las ocho `ReglaDura`, releyendo las 770 filas PERSISTIDAS para que no sea
    circular; (2) conservación de la carga contra el PDF, por multiconjunto (grupo, asignatura, profesor),
    descontadas las 11 de FPB; (3) calidad, que **se mide y no se corrige** (R-terminado), justificada solo
    porque el criterio contiene la cláusula «presentable al centro» y medir no es mejorar. Las divergencias
    esperadas se declararon ANTES de medir para que un desajuste fuera hallazgo y una coincidencia no fuera
    racionalización. **RESULTADOS.** El horario de S118 SOBREVIVE en `educhronos-demo-m4.db`, así que no hubo
    que regenerar. CAPA 1: **cero violaciones sobre 770 sesiones** por `GET /api/horarios/{id}/diagnostico`,
    con la aplicación arrancada y comprobado antes que la proyección traía 770 —lo que añade sobre
    `DiagnosticoRoundTripTest` es la ESCALA, no la propiedad: 208 actividades, 305 plazas, 28 grupos, 30
    tramos—. CAPA 2: **526 claves, 11 divergentes, todas FPB, cero fuera de FPB, delta 1301 − 1252 = 49**, y
    las 11 son una a una las marcadas `_aulaDesconocida` ANTES de mirar el lado generado; predicción y
    observación coinciden al entero, y el control independiente de ocupación (26 grupos a 30/30, 1FPB a 6 y
    2FPB a 5) apunta al mismo agujero. CAPA 3: ventanas 174, consecutivas 18, indisponibilidad 0, y como los
    tres pesos valen 1 (`ModeloCpSat:69,80,104`) y el objetivo es su suma (`:301`), **174 + 0 + 18 = 192**,
    exactamente el objetivo que CP-SAT escribió: `VerificadorSolucion` recompone el objetivo desde la solución
    reconstruida, con código distinto del que construyó el modelo, sin residuo. **CONTROL METODOLÓGICO QUE
    VALE POR SÍ SOLO: se detectó y eliminó una circularidad.** El mapa de códigos de grupo se había derivado
    maximizando el solapamiento de (asignatura, profesor) —el mismo dato que luego se comparaba—, y peor donde
    menos evidencia había (1FPB y 2FPB emparejaban a 0.14 y 0.17). Se rehízo aplicando la regla determinista
    de `INFORME-RECONCILIACION.md` sin mirar la base: 28 códigos con regla aplicable, CERO diferencias, regla
    inyectiva, imagen coincidente en los dos sentidos. El déficit de 49 no descansa sobre un mapa ajustado a
    posteriori. **ADVERTENCIAS QUE ACOMPAÑAN AL RESULTADO:** el `indispBlanda = 0` es VACUO —
    `profesor_restriccion_horaria` y `sesion_bloqueada` están vacías, es el cero de «no había nada que
    incumplir»— y las 465 `penalizaciones` son contrafactuales por celda (suma de deltas −73), no una
    descomposición del total. LÍMITES: n = 1 (no se generó un segundo horario: diez minutos, un cuarto de
    fallo, y la capa 2 es invariante entre corridas); `FEASIBLE` con cota 0.0, sin óptimo demostrado; la
    comparación es de multiconjuntos y no dice nada de la colocación. **C-generación CIERRA. O-demo NO**,
    porque siguen faltando las 11 de FPB (D31-a). Ninguna deuda nace ni se paga; se afina a la baja
    D-diagnostico-no-es-foto. Suites intactas: app 282, solver 91, vitest 316, e2e 2.
  - **C-hueco-\*** — cada caso que resulte inexpresable. No se planifican: se abren cuando
    aparecen. Tras S115 no hay ninguno conocido.
  - **C-carga-manual-1eso** ✔ HECHO (S120), en su versión RECORTADA POR ANCHO. Constaba PROPUESTO
    desde S115 y sin decidir durante TRES sesiones (S117, S118, S119); S120 lo decidió y lo ejecutó
    el mismo día. Su M0 corrigió el prompt de apertura en el punto que gobierna lo que podía
    prometer: este Cambio NO avanza el criterio de terminado de O-demo —la API REST es vía legítima
    desde S115— sino la NOTA DE ALCANCE, es decir la distancia con el paso 2 del guion de §1
    («crear un centro desde cero POR LA INTERFAZ»). DECISIÓN DE ALCANCE DEL ARQUITECTO, distinta de
    la que S117 recomendaba: **dos grupos, 1ºA y 1ºB**, no uno ni los cuatro, porque cada plaza del
    bloque referencia cuatro subgrupos y con un solo grupo desaparecería el caso de «plaza que agrega
    subgrupos de varios grupos», que es la interacción de UI más difícil. **RESULTADO: ningún caso
    resultó inexpresable. 38 envíos, veinte minutos, horario válido en 1 s.** El bloque se construyó
    como UNA actividad de seis plazas con dos subgrupos por plaza, una con aula fija y cinco con
    candidatas; la co-docencia de LCL como UNA plaza con DOS profesores; las dos a la primera. El
    horario se verificó de forma independiente sobre las capturas (repeticiones, bloque entero en un
    tramo, seis aulas distintas, candidatas respetadas, y la aritmética de S117 dando 2×6+4×1 = 16
    filas de `sesion`). HALLAZGO TÉCNICO QUE S119 NO PODÍA PRODUCIR: `1ºA-Completo` contiene a los
    seis subgrupos del bloque, y que LCL y el bloque no coincidan NO es suerte —hay regla dura propia
    de solape por grupo (`SOLAPE_GRUPO`, `RestriccionNoSolapeGrupo` y sus tests)—; con el centro
    completo esa regla no se ejercita, porque los 26 grupos van a 30/30 y no queda hueco donde el
    conflicto pueda darse. LÍMITE QUE ACOMPAÑA AL RESULTADO, y es el que importa: **el guion decía QUÉ
    construir**, así que lo demostrado es que los formularios EXPRESAN el caso, no que un usuario
    averigüe cómo MODELARLO. Nacen D-vista-horario-sin-horario, D-selectores-sin-busqueda,
    D-actividad-forma-implicita y D-arranque-no-literal (§4); ninguna se paga. **Con este Cambio,
    O-demo se queda SIN TRABAJO EJECUTABLE** hasta que el centro responda D31-a.
  - **S134 — el centro RESPONDE y `D31-a` se salda; O-demo queda DESBLOQUEADO.** No hubo correo: el
    arquitecto sentó al jefe de estudios delante y la consulta salió en tres rondas. Las aulas de las 11
    plazas de FPB son una REGLA y no once datos —lo que el horario no imprime va al Taller 4 (1º FPB) y al
    Taller 5 (2º FPB), uso exclusivo de FPB—, corroborada por contraste con las únicas horas de FPB que sí
    traen aula (CS/CyS en TALL3) y con las cuentas exactas 24 + 6 = 30 y 25 + 5 = 30. **La deuda bloqueante
    del proyecto baja a 0 por primera vez desde que existe el mapa.** Queda trabajo ejecutable por primera
    vez desde S120: dar de alta el aula Taller 5 (`D-taller5-inexistente`), cargar las 11 actividades y
    contrastar contra el oráculo escrito en S117, 819 filas en `sesion` frente a las 770 de hoy, delta 49.
    De la misma entrevista salen: `D31 (b)` REFUTADA (el subgrupo de 4º ESO es el itinerario y SÍ se
    reutiliza entre bloques), 21 de los 28 tutores encontrados impresos en `Horarios de profesores.pdf`
    —pero por nombre y no por código, `D-nombres-sin-codigo`— y los 7 de Bachillerato sin fuente
    (`D-tutores-bachillerato`). El criterio de O-demo NO se cumple todavía: el centro no está completo
    hasta que las 11 estén cargadas.
  - **C-centro-completo** ✔ HECHO (S135) — **creado en el M0 de esa sesión**, porque la lista de Cambios se cerró en S115 con cinco piezas y ninguna cubría esto; precedente escrito en esta misma ficha (el M0 de S115, como el de S107 con O-estructura). Alta del aula Taller 5, `aulaFija` en las 11 plazas marcadas `_aulaDesconocida`, carga del centro entero y contraste contra el oráculo. **RESULTADO: el centro real está COMPLETO en la base por las vías legítimas del producto y GENERA horario válido.** 816 escrituras previstas y 816 escritas sin un desajuste, cargando **desde base VACÍA** y no incremental —decisión del M0: es lo único que demuestra el criterio en un solo acto y ejercita la constante nueva—; `HTTP 200` en 601 s al PRIMER intento, `FEASIBLE`, objetivo 203.0, **`sesion` 819**. El oráculo NO se heredó de S117: se rederivó del catálogo de hoy y de paso reprodujo 770 sin las 11, delta 49 = 24 + 25. **Las tres capas de S119, remedidas: (1) cero violaciones duras sobre 819 y los 28 grupos a 30/30, con 1FPB y 2FPB teniendo horario por primera vez; (2) conservación con 526 claves, CERO divergentes y 1301 = 1301, que es lo que convierte «completo» en «correcto» —el recuento demuestra que hay once actividades colocadas, no que sean las once que el centro imparte—; (3) identidad exacta 188 + 0 + 15 = 203**, con las dos advertencias remedidas y no heredadas (el `indispBlanda = 0` es VACUO, las tres tablas de restricción están vacías; las 479 penalizaciones son contrafactuales por celda). **El 203.0 NO es comparable con el 192.0 de S118**: son instancias distintas, y las consecutivas incluso BAJAN de 18 a 15. El M0 midió además la trampa que habría invalidado el oráculo: `POST /api/horarios` ACUMULA (D-horario-irreversible), así que generar sobre la m4 habría dado 1589 filas. Piezas de método que deja: la guarda del cargador pasa de exigir «11 violaciones» a exigir CERO —verificada por mutación, y ahora detecta por sí sola violaciones ajenas al XOR— y la capa 2 deja de ser arnés desechable (`tools/carga-centro/verificar-conservacion.py`, validado reproduciendo las once divergencias de S119 terna a terna). Se cierra `D-taller5-inexistente`; nacen `D-aula-tipo-sin-uso-real` y `D-guion-busca-token-esperado`. **LO QUE LE FALTA A O-DEMO, y no lo da este Cambio:** la tercera pata, «presentable al centro». Los 28 tutores cargados son INCORRECTOS (S134), no incompletos, y decidir si eso cae dentro del criterio es trabajo del M0 siguiente.
  - **C-presentable** ✔ HECHO (S136 + S137) — **creado en el M0 de esa sesión**, tercer precedente de la práctica que ya usaron el M0 de S107 y el de S135. Cubre la TERCERA pata del criterio 5, que ningún Cambio de la lista de S115 contemplaba. Sesión de MEDICIÓN en cinco pasadas de solo lectura: ni una fila escrita en ninguna base. **RESULTADO PRINCIPAL, y corrige a S134: los 28 tutores NO son incorrectos.** Los 16 ordinarios de ESO y FPB casan 16/16 contra las 17 cabeceras `Tutor:` de `Horarios de profesores.pdf` (1ºA/GH6, 1ºB/MAT8, 1ºC/LEN9, 1ºD/EFI2, 2ºA/MAT5, 2ºB/MAT1, 2ºC/GH2, 3ºA/MAT6, 3ºB/BYG2, 3ºC/BYG3, 4ºA/ING6, 4ºB/GH4, 4ºC/LEN6, 4ºD/EFI3, 1FPB/PAU2, 2FPB/PAU1), los 5 PDC heredan correctamente el principal del padre, y los fallos son SIETE: los de Bachillerato. La heurística de S115 acierta donde hay hora de tutoría de la que tirar y fabrica donde no la hay. **DOS DIVERGENCIAS, y sólo una miente:** (A) faltan las 5 filas `CO_TUTOR` de ORI1 Guerrero Serrano sobre los grupos Di, respaldadas por el volcado y que la UI ya sabría mostrar —`@if (coTutores().length > 0)`, así que hoy la pantalla no afirma que no las haya: es omisión, no falsedad—; (B) los 7 `TUTOR_PRINCIPAL` de Bachillerato no tienen respaldo en ninguna fuente. **LAS 7 SON PORTANTES**, medido: cada actividad 40–45 (`Bloque-PTEV/PTVE_Relig`) tiene dos plazas —REL1, que no aporta tutor, y el profesor que resulta ser el `TUTOR_PRINCIPAL` del grupo cubierto—, y `verificarTutorias` exige cobertura por IDENTIDAD y descarta `CO_TUTOR`, así que borrar cualquiera dispara `TUTORIA_SIN_TUTOR`. **RECETA DE LA CORRECCIÓN, con coste medido:** reescribir las 7 no basta (el tutor real no imparte el bloque), hay que bajar `requiere_tutor` a 0 en las 6 —lo correcto en sí, porque Bachillerato tiene tutor y no tiene TUT—, y eso choca con `exigirSinDependientes` (409, 12 sesiones), luego exige base sin horario y REGENERAR. Asimetría a favor: `TutoriaService.reemplazar` no tiene guarda, y `GET /api/horarios/{id}/diagnostico` re-verifica un horario ya guardado con el MISMO `VerificadorSolucion`. **ENTREGABLE: `docs/salvedades-demo.md`**, que cumple la mitad declarativa del criterio y sirve de petición al centro; su línea mayor no son los tutores sino que el horario NO usa disponibilidades del profesorado (`profesor_restriccion_horaria` vacía). **LA DIVERGENCIA A NO SE EJECUTA (R-terminado)**: no miente y no es un PUT —`cargar-centro.py` emite un PUT por entrada y el PUT es reemplazo total, así que exige cambiar la forma del catálogo, el bucle y la prevalidación (`D-cargador-tutoria-pisa`)—. Se cierra `D-nombres-sin-codigo`; nacen `D-cargador-tutoria-pisa`, `D-catalogo-meta-enganosa` y `D-preguntas-sin-cola`. **LO QUE LE FALTA A O-DEMO, y este Cambio NO puede darlo:** la mitad «nada falso» de la tercera pata depende de que el centro entregue los 7 tutores. **O-demo vuelve a quedarse sin trabajo ejecutable, por dependencia de REQUISITOS y no de deuda.**
    **SEGUNDA PARTE HECHA EN S137 y CAMBIO CERRADO; CON ÉL CIERRA O-DEMO.** El centro entregó la lista oficial de los 28 grupos, que casa **16/16** con el cruce de S136 en todo lo verificable y da los siete que faltaban (1B-A/LEN7, 1B-B/MAT7, 1B-C/LEN2, 1B-D/LEN1, 2B-A/MAT2, 2B-B/GH3, 2B-C/GH5), los siete presentes entre los 59 profesores. **El par indivisible dejó de ser cita y pasó a ser medida:** recalculando S8 en tres estados, el escenario «sólo las tutorías» rompe EXACTAMENTE las seis actividades de bloque, y el detalle destapa el mecanismo —el «S8: OK» se sostenía porque el tutor inventado ERA el profesor de la plaza—. La vía se decidió por construcción y no por prudencia: `requiereTutor` sólo viaja en el POST de alta, así que una recarga incremental propagaría media corrección. **Ejecución:** parche del catálogo por guion con procedencia ejecutable (`aplicar-tutores-oficiales.py`, `indent=1` medido reproduciendo el fichero byte a byte, diff de 16/16 sin ruido), carga desde base VACÍA con 816/816 escrituras, y `HTTP 200` en 10 min 2 s, `FEASIBLE`, `sesion` **819**. **Las tres capas: (1) cero violaciones duras sobre 819, los 28 grupos a 30/30 y ningún `TUTORIA_SIN_TUTOR` —la comprobación nueva, porque S8 pasa a exigirse sobre 16 actividades—; (2) conservación con 526 claves, CERO divergentes y 1301 = 1301, predicha como EXIGIBLE antes de generar porque el parche no toca ninguna plaza; (3) identidad 201 + 0 + 34 = 235.** El 235 frente al 203 de S135 es **varianza del solver, demostrada**: mismas restricciones menos seis, misma función objetivo y ninguna de las 16 actividades con `requiereTutor` toca Bachillerato, luego toda solución de S135 lo es de S137 al mismo coste y el óptimo de S137 es ≤ 203; mide una dispersión de al menos 32 puntos a 600 s. NO se corrige (R-terminado): se DENIEGA regenerar con semilla o mejor de varias corridas, porque la calidad se mide y no se corrige y porque las capas 1 y 2 no dependen del coste blando. Bases nuevas: `app/educhronos-s137-centro-completo.db` (`64d671fe…`, `sesion` 0) y `app/educhronos-s137.db` (`dfa4c077…`); las dos de S135 intactas y degradadas a histórico, porque contienen los siete tutores inventados. Se cierran `D-tutores-bachillerato`, `D-hallazgo-E-refutado` y, de paso, `D-catalogo-meta-enganosa`; nacen `D-meta-invariantes-a-mano` y `D-catalogo-candidatos-huerfano`. **LA DIVERGENCIA A SIGUE FUERA (R-terminado)** aunque la lista oficial la respalde: un co-tutor ausente no es falso, está declarado en `docs/salvedades-demo.md`, y añadirlo exige cambiar la forma del catálogo, el bucle y la prevalidación (`D-cargador-tutoria-pisa`).
  - **C-borrado-horario** — RETIRADO del camino crítico en S115: con la carga por API la
    base se rehace en minutos y D-horario-irreversible deja de ser callejón sin salida.
  - **C-configuracion-navegable** — RETIRADO del camino crítico en S115 por medición: el
    riesgo que lo justificaba exige subgrupos multi-grupo y los 334 derivados son todos
    mono-grupo. Pasa a D-configuracion-monolitica (§4).
- **La vía de carga es la API REST, decidido en S115 y no se rediscute.** La alternativa
  —un script de INSERT contra SQLite— se descartó con argumento: las invariantes I1–I7 viven
  ENTERAS en la capa de aplicación y el esquema no las replica (D-F8.2b-iv-a), así que un
  INSERT no comprueba I2, I7, I5 ni el XOR del aula; con 219 actividades derivadas por
  inferencia, los errores no aparecerían al insertar sino como INFEASIBLE opaco o como un
  horario válido y equivocado. Precedente escrito: `SeedCatalogoRunner`, que D-seed-demo
  declara muerto por poblar por debajo de la aplicación, y que S116 confirmó BORRADO del árbol.
  **CORRECCIÓN (S116): el «efecto lateral útil» que esta ficha declaraba —«un cliente HTTP lee el
  motivo del rechazo aunque el navegador no, porque viaja como reason phrase»— es FALSO, medido en
  ejecución.** La línea de estado llega vacía (`HTTP/1.1 400 `) y el cuerpo no trae `message`: el
  cliente HTTP no distingue «ya existe» de «payload inválido». La DECISIÓN de cargar por API sigue
  en pie y su argumento principal —las invariantes I1–I7 viven enteras en la capa de aplicación—
  no depende de esa frase; lo que desaparece es un beneficio secundario que nunca existió. El
  cargador se apaña por otra vía, medida y escrita en S116: la prevalidación en seco garantiza que
  no deba haber ningún 400, así que cualquier respuesta no-2xx se trata como FATAL.
- **EL CATÁLOGO NO TRAE NOMBRES, Y ESO ESTUVO A PUNTO DE BLOQUEAR MÁS QUE FPB (S116).**
  `AsignaturaService.java:201-205` y `ProfesorService.java:121,:124` exigen `nombreCompleto` no
  nulo, y el catálogo derivado trae los 100 y los 59 a `null`: 159 envíos que fallarían con 400
  seguro, en el PRIMER eslabón de la carga, frente a 11 por FPB. El dato falta en la FUENTE igual
  que las aulas, así que NO es hueco funcional de H2. Resuelto extrayéndolos de las leyendas del
  PDF de grupos —única fuente: los volcados JSON solo guardan códigos, porque la leyenda se usó
  como vocabulario de clasificación y no se persistió—. Cobertura 59/59 y 100/100. SALVEDAD
  PERMANENTE: el PDF los trae TRUNCADOS y el truncamiento está EN EL ORIGEN (24 caracteres en
  leyenda, 35 en la línea `Tutor:`), así que ninguna técnica los recupera; 39 entradas quedan
  marcadas como truncadas y 7 asignaturas se quedan con su propio código porque la leyenda lo
  repite. `Distribución materias ESO.pdf` se descartó con argumento: solo completaría 1 de 15 y su
  nomenclatura es LOMCE 2016, ajena al centro. Los nombres reales dejan de ser pregunta para el
  jefe de estudios y pasan a ser pulido opcional.
- **NOTA DE ALCANCE (S115), escrita para que no viva en la memoria de nadie:** con la carga
  por API, el criterio 5 de Fase 8 no queda demostrado a escala real POR LA INTERFAZ. Lo
  demostrado por UI es el centro mínimo (e2e de S112). C-carga-manual-1eso existe para
  cerrar esa distancia; si se decide no hacerlo, la distancia se declara y no se disimula.
  **ESTRECHADA EN S120, y NO eliminada.** El caso más difícil del centro real —bloque de seis
  destinos alternativos con subgrupos de dos grupos, y co-docencia— quedó construido a mano por
  la interfaz y generando horario válido. Lo que sigue SIN demostrar por UI es la ESCALA (38 envíos
  de los 815; dos grupos de veintiocho; sin PDC, sin tutorías y sin currículo ordinario) y, sobre
  todo, el DESCUBRIMIENTO del modelado: el guion de S120 decía qué construir, así que se demostró
  que los formularios expresan el caso, no que un usuario dé con la forma de expresarlo. Esa
  segunda mitad no la demuestra ningún ejercicio guionizado por el arquitecto.
- **El centro real, medido (S115):** 815 envíos de formulario equivalentes —jornada 1,
  niveles 8, asignaturas 100, profesores 59, aulas 43, grupos 23, PDC 5, tutores 28,
  subgrupos 329, actividades 219—; 334 subgrupos (28 completos + 306 parciales, TODOS
  mono-grupo); 219 actividades con 316 plazas que describen 632 sesiones y cubren los 840
  slots del horario real sin huecos ni solapes, S9 verificada.
- **Orden de carga condicionado (S115):** los tutores de los grupos ORDINARIOS se asignan
  ANTES de crear sus PDC, o cada PDC se queda sin tutor. `PdcService.heredarTutorPrincipal`
  corre SOLO en el alta (medido en S114, `PdcService.java:110`), así que un PDC creado antes
  de que su padre tenga tutor no hereda nada y nadie lo resincroniza después: es
  D-tutor-pdc-desincronizado mordiendo por primera vez, tal como su ficha preveía. El centro
  real tiene cinco PDC y S8 exige tutor donde `requiereTutor`. **NEUTRALIZADO en S116 y por una vía
  distinta de la prevista:** el cargador crea primero los 28 grupos (23 ordinarios y luego los 5
  PDC) y asigna las 28 tutorías DESPUÉS, porque `PUT /api/grupos/{id}/tutoria` es reemplazo total
  idempotente (verificado: dos PUT iguales y un tercero distinto, sin residuo) y acepta grupos PDC
  (no hay lista blanca de tipo en el sub-recurso; `TutoriaService.java:92-93` solo hace
  `findById`). Con ese orden ningún PDC hereda nada y los 28 PUT ponen todo. Medido además que en
  ESTE catálogo la cuestión es vacía: los 5 PDC tienen EL MISMO tutor que su padre (3ºADi/MAT6,
  3ºBDi/BYG2, 3ºCDi/BYG3, 4ºADi/ING6, 4ºDDi/EFI3), luego no había nada que sobreescribir. La
  deuda sigue viva; lo que deja de existir es la condición de orden.
- **Lo que los volcados NO contienen y hay que asumir:** ninguna disponibilidad ni
  restricción horaria de profesor (medido en S115; un horario ya resuelto no puede contener
  las restricciones que lo produjeron). El backend sí modela `ProfesorRestriccionHoraria`,
  pero los datos no la alimentan. Consecuencia asumida por el arquitecto: el horario generado
  será válido en el modelo y distinto del que usa el centro. La demo entrega un horario
  válido, no un parecido.
- **Absorbe:** D-seed-demo, D-demo-cliente (ambos objetivos disfrazados de deuda),
  y cierra la parte VIVA de D31 (validación de poblaciones con el centro). D31 tiene por
  primera vez PREGUNTAS CONCRETAS y no dudas genéricas, y son las tres de la consulta
  pendiente al jefe de estudios: las aulas reales de las 11 plazas de FPB (lo único que
  BLOQUEA, y S116 confirma que sigue siéndolo SOLO para el criterio 5 al completo: no impide
  generar, porque no existe ninguna restricción de cobertura total de tramos y los dos grupos de
  FPB son precisamente los que tienen holgura), los tutores reales (la heurística «tutor = quien imparte TUT» saca a FIL2 como
  principal de cinco grupos: implausible, aunque no viola I4) y los itinerarios de 4º ESO,
  1º Bach y 2º Bach, que deciden si un subgrupo se reutiliza entre bloques por I6 y que son
  exactamente D31 (b), (c) y (d).
- **Fuentes de datos:** los horarios reales del IES se extrajeron de los PDFs a
  JSON en una operación previa. Su documentación —`RESUMEN-EXTRACCION.md` e
  `INFORME-RECONCILIACION.md`— fue el insumo de C-derivación. Dos correcciones registradas en
  S115 sobre ellas: la «correspondencia incierta» de `3º ESO PDC` queda CERRADA (es el PDC de
  3ºC, por tres vías independientes) y FOPP no colisiona profesor↔asignatura (el único código
  que es las dos cosas es ECO). Hallazgo de reutilización: los volcados sirven además como
  ORÁCULO DE REGRESIÓN —cero inconsistencias internas medidas—, contra el que C-generación
  puede cruzar lo que produzca el solver.

#### O-particiones — "Un grupo nuevo entra en el curso sin reconfigurar a mano." ✔ TERMINADO (S141)
- **Propósito:** cumplir el criterio 6 de Fase 8, que hoy no tiene constructor: crear un
  grupo dentro del curso lo incorpora automáticamente a las particiones existentes de su
  nivel.
- **Terminado cuando:** **[REESCRITO en S138 tras medir. El criterio anterior decía
  «sin edición manual subgrupo a subgrupo», y S138 midió que eso es INALCANZABLE en 10 de
  los 39 bloques del centro por falta de INFORMACIÓN y no de código: en esos bloques el
  reparto de un grupo entre las vías depende de la matrícula del curso y no se deduce de
  nada persistido.]** Sobre la base de referencia del centro y **SIN horario generado**,
  dar de alta por la interfaz un grupo ORDINARIO en un nivel con particiones densas
  (1º ESO o 4º ESO; NO un FPB, que no comparte partición con nadie) lo deja participando
  en todos los BLOQUES del nivel —las actividades de MÁS DE UNA PLAZA— en los que
  participan sus hermanos, con tres
  condiciones verificadas POR CONSULTA y no por inspección visual:
  **(1) En los bloques de universo replicado la incorporación es AUTOMÁTICA:** cero
  decisiones del usuario y cero edición subgrupo a subgrupo. Son 29 de los 39 bloques.
  **(2) En los bloques de REPARTO el usuario toma exactamente N decisiones por pantalla**
  —N medido en S138 sobre el centro real: 2 en 1º ESO, 1 en 4º ESO, 1 en 2º ESO, 0 en
  3º ESO, 2 en 1º Bachillerato y 4 en 2º Bachillerato— **y ninguna de ellas exige tocar
  subgrupos ni plazas a mano.**
  **AÑADIDO EN S140, y recuenta las tres condiciones.** La **(1)** queda CUMPLIDA de verdad: S139 la
  declaró cumplida en servidor y estaba ROTA sobre el catálogo real —`Actividad.plazas` es bolsa y el
  `left join fetch` de `findConPoblacionPorGrupo` duplicaba cada plaza una vez por subgrupo, de modo que
  30 de las 219 actividades (las de plaza única compartida entre un ordinario y su PDC) medían 2, entraban
  como bloque y recibían el espejo—. Reparado en S140 y verificado sobre el centro real (4º ESO: 6/1,
  clavando a S138). La **(3)** queda CUMPLIDA: existe `DELETE /api/grupos/{id}/replicacion` y el ciclo
  alta→deshacer sobre copia del centro completo deja dump IDÉNTICO, ids incluidos. **La (2) se PRECISA:**
  N son BLOQUES de reparto —así los contó S138— y dentro de cada uno el usuario hace una asignación POR
  ESPEJO, porque el M4 de S139 cambió la unidad de decisión para que I2 fuera inviolable por construcción.
  Medido en S140 sobre 4º ESO: 1 bloque, 4 asignaciones. No cambia lo que hay que construir; sin la
  precisión, `C-alta-por-pantalla` se declararía incumplida sin razón.
  **CUMPLIDA EN S141, y con ella el objetivo CIERRA.** `C-alta-por-pantalla` entrega el diálogo de
  replicación, y el gesto del criterio se ejecuta ENTERO desde la interfaz sobre copia del centro
  completo: 4ºE creado en 4º ESO, replicado desde 4ºA, 6 bloques automáticos y **1 de reparto con 4
  decisiones**, cero edición subgrupo a subgrupo. Verificado por CONSULTA con un montaje de TRES dumps
  —79 líneas escritas en el pico (24 subgrupo, 24 subgrupo_grupo, 30 plaza_subgrupo, 1 grupo), 79
  retiradas, `antes == después` con los ids dentro—, que es el primero concluyente: el de S140 no
  distinguía «ida y vuelta» de «nada». `4ºE-Completo` con CERO plazas e I2 intacto, los dos verificados
  por primera vez fuera de fixture.
  **(3) El alta es REVERSIBLE:** borrar el grupo recién creado deshace lo que el alta creó
  y devuelve la base a su estado anterior, comprobado por comparación. Hoy no lo es:
  `GrupoService.borrar` rechaza con 409 un grupo que esté en algún subgrupo, así que un
  alta que cree 24 subgrupos produce un grupo que no se puede borrar sin desmontarlos uno
  a uno, y septiembre es justamente cuando el alta se hace a tientas.
  **FUERA del criterio por decisión explícita de S138:** operar sobre base CON horario ya
  generado (`plaza_subgrupo` sólo se escribe con `PUT /api/actividades/{id}` y
  `ActividadService.exigirSinDependientes` lo bloquea con 409; la guarda es deliberada y
  protege la reconciliación posicional, así que no se toca); materializar `Particion`
  (ver el punto siguiente); y la gestión de los subgrupos `-ATED`/`-Rel` de los grupos PDC
  (`D-subgrupos-di-sin-api`), que el gesto de prueba no toca por ser de grupo ordinario.
  **AÑADIDO en S139 tras medir, y con ello la cabecera de este criterio queda ESTRECHADA
  a los bloques:** quedan también FUERA las actividades de UNA SOLA PLAZA —las materias
  ordinarias del grupo, 180 de las 219 del centro—. Clonar en ellas el subgrupo del
  hermano no replica al grupo nuevo: lo FUNDE con su hermano en la misma sesión y con el
  mismo profesor. Lo que necesitan es una ACTIVIDAD NUEVA, con profesor, aula y carga que
  no se deducen de ningún hermano —el mismo dato faltante que la matrícula—, y su vía es
  el alta de actividad por UI, que existe desde S110. La cabecera anterior era más ancha
  que las tres condiciones de abajo, que sólo hablan de los 39 bloques. **La frontera no
  es una convención impuesta:** medido en S139, ninguno de los 28 subgrupos `-Completo`
  toca una sola plaza de bloque (cero pares), y los de optatividad viven exclusivamente
  en bloques.
- **Depende de:** O-demo (necesita un centro real con particiones densas delante). Cuidado
  de ORDEN: la prueba se hace ANTES de generar, o después de que exista un borrado de
  horario; ampliar la población de un subgrupo toca actividades que quizá ya tengan
  sesiones, y `ActividadService.exigirSinDependientes` las bloquea con 409.
- **Valor:** es el segundo de los dos criterios que cierran H2. Sin él, H2 no termina.
- **Por qué es objetivo y no un Cambio de O-demo (medido en S115; ALCANCE CORREGIDO en
  S138).** Sigue siendo objetivo: `GrupoService.crear` no importa `SubgrupoRepository`, la
  relación grupo↔subgrupo vive sólo del lado del subgrupo (`subgrupo_grupo`) y
  `GrupoAdministrativo` no tiene lado inverso —verificado en S138: cero `OneToMany` y cero
  `Set<`—, luego no hay nada que un grupo nuevo pueda heredar y el trabajo no cabe dentro de
  otro objetivo. **Lo que S138 REFUTA es el tamaño.** Este punto afirmaba que el objetivo
  «exige materializar `Particion`» y que toca dominio, persistencia con migración de
  `schema.sql`, servicio, API y frontend. La medición dice que NO hace falta: la mezcla
  entre grupos no se hace con subgrupos multi-grupo —los 334 subgrupos de la base de
  referencia son mono-grupo SIN EXCEPCIÓN, y el `Set<GrupoAdministrativo>` de `Subgrupo` es
  capacidad construida y jamás estrenada— sino en `plaza_subgrupo`, con 127 plazas de dos o
  más subgrupos. Ese mecanismo ya expresa reparto ARBITRARIO, incluido el caso que ninguna
  plantilla declarativa captura: `BIOL_Físic_Geogr_HART-2BACH` tiene 5 vías y el universo
  {2B-A, 2B-B, 2B-C} no aparece completo en NINGUNA. Por eso se DESCARTA también `D1` en su
  forma escrita (campo `patron_generacion` JSON sobre `Particion`, `modelo_datos_fase1.md`
  §8): una plantilla de producto cartesiano funcionaría en 3º y 4º ESO y se rompería justo
  donde el horario es más difícil, que es 2º de Bachillerato —4 de sus 8 bloques reparten—.
  **El diseño que se sigue de los datos es CLONAR DE UN HERMANO**: replicar la estructura de
  subgrupos de un grupo existente del nivel y preguntar sólo en los bloques de reparto. Sin
  esquema nuevo y sin migración. **Y las cuatro preguntas de dominio quedan CERRADAS en
  S138, ninguna para el jefe de estudios:** (1) a qué vía va el grupo entrante NO es una
  regla sino un dato de matrícula, luego es UX del alta y no requisito externo; (2) «la
  partición del nivel» es derivable por consulta —ninguna actividad cruza niveles— y no
  necesita entidad; (3) las plazas con sesiones bloquean con 409 y eso pasa a ser condición
  de operación escrita en el criterio; (4) la irreversibilidad es real y pasa a ser
  condición (3) del criterio.
- **Absorbe:** D1 (generación automática de subgrupos por plantilla), que O-estructura
  declaraba absorber y cerró sin construirla —correctamente, porque no estaba en el texto de
  su criterio (R-terminado)—. También la invariante de población de D31: hoy I1 no la hace
  cumplir ningún componente, y materializar `Particion` es la sede natural para decidir si
  eso cambia.
  **S138 acota qué queda de D1:** su forma escrita (`patron_generacion` sobre `Particion`)
  se descarta por medición; lo que sobrevive de D1 es el PROPÓSITO —que el alta de un grupo
  no obligue a crear subgrupos a mano—, que es exactamente el criterio reescrito.

#### O-disponibilidad — "El centro introduce la disponibilidad de su profesorado." ✔ TERMINADO (S167)
- **Propósito:** que el jefe de estudios introduzca por la interfaz, para cada profesor, los tramos en que no
  puede dar clase (DURA) y en los que prefiere no darla (BLANDA), y que el horario generado lo respete. Es parte
  del paso 2 de §1 desde S159.
- **Por qué existe (medido en S159, HEAD `7a45f3e`, Claude Code, solo lectura):** ninguna ruta, servicio, botón
  ni llamada del frontend alcanza `GET/PUT /api/profesores/{id}/restricciones-horarias`; `ProfesorService` tiene
  cinco métodos y ningún sub-recurso; `git log -S` sobre `app/frontend` sale vacío para `restricciones-horarias`
  y para `RestriccionHoraria`. El backend existe desde S78 y el solver consume DURA y BLANDA desde S26. Los tres
  bancos del centro real: 59 profesores, 0 restricciones. La rejilla estuvo planificada —bloque 8.5-E,
  «puede / no puede / prefiere-que-no», con mockup previo— y S78 cerró 8.5 sólo con el backend; ningún objetivo
  la recogió después. Sin ella, el horario de un centro real es válido para el modelo y no para el centro, que es
  lo que S115 asumió para la demo.
- **Por qué en H2:** los hitos salen de los pasos de §1, y el paso 2 es H2. H2 se reabre en S159.
- **Orden:** tras `O-curso`, y antes de la Fase 12 y del objetivo de aceptación (§2). Sin dependencia con
  `O-curso` en ningún sentido: `profesor_restriccion_horaria` está entre las 17 tablas que copia el duplicado
  (21 menos las 4 del horario, medido en S159), y la guarda de solo lectura de `O-curso` es genérica (ver su
  ficha), así que cubrirá el PUT de restricciones sin tocarla.
- **Terminado cuando** — ESCRITO en S164 sobre medición (M2 de solo lectura, HEAD `fc9076d`, bancos s137
  con md5 intacto), con las pautas de S159. O-disponibilidad termina cuando:
  1. **Entrada.** Desde cada fila de Profesores se abre la disponibilidad del profesor (patrón del diálogo
     de tutoría, S114): una rejilla días × tramos lectivos derivada de `GET /api/jornada`, con las horas de
     cada tramo, y el recreo como separador no editable.
  2. **Tres estados.** Cada celda está en `{sin fila, BLANDA, DURA}` —«Disponible», «Prefiere no», «No
     puede»—, que se distinguen sin depender del color, con una leyenda visible que indica que las guardias y
     reducciones en tramos lectivos se marcan como «No puede» (decisión S79). No se exponen `peso`
     (`D-F8.5-E-a`) ni `motivo`.
  3. **Pincel.** Se elige un estado y se aplica con una sola acción a una celda, a un día completo o a un
     tramo en todos los días. «Disponible» borra.
  4. **Guardar.** Guardar hace el PUT de reemplazo total; al reabrir se ve lo guardado; los rechazos del
     backend, incluido el 403 `CURSO_SOLO_LECTURA` del curso archivado, se muestran con su mensaje.
  5. **La DURA se cumple entera.** La DURA veta todos los tramos que ocupa cada sesión, también en
     actividades de más de un tramo, y la BLANDA penaliza también cuando la sesión ocupa el tramo sin empezar
     en él. El verificador tiene una regla dura de indisponibilidad del profesor, con lo que un movimiento
     manual a un tramo vetado se rechaza. Se demuestra con tests que fallan antes del arreglo. (Reescrita y revocada en el cierre de S165: ver la decisión F.)
  6. **Pin contradictorio.** Un pin sobre un tramo DURA del profesor de esa sesión se rechaza en la
     prevalidación, antes del solver, con un 422 que nombra al profesor y el tramo.
  7. **Verificación.** Con un caso sintético y desde la interfaz: se introduce disponibilidad con DURA y
     BLANDA, se genera el horario y el verificador da cero violaciones de la regla de la condición 5.
     Comprobado en navegador (M4). No depende de los datos del centro.
- **Fuera del criterio, con motivo (S164):** un e2e, porque la aceptación recorrerá la cadena y la suite e2e
  tiene un rojo preexistente con sede en la Fase 12; la verificación en Windows, porque nada es propio de
  plataforma salvo el dibujo de los símbolos, que la aceptación verá en el bundle; y el recreo editable (ver
  Decisiones).
- **Dependencia externa: DESAPARECE en S164.** El centro no entregará el formulario en papel: digitalizarlo
  sin herramienta es justo lo que resuelve este objetivo, y los datos los introducirá el centro con la
  rejilla. Se descarta inferir la disponibilidad de los horarios del centro: un hueco en un horario es
  holgura del reparto, no una imposibilidad. Como DURA forzaría a reproducir el horario anterior, como BLANDA
  lo favorecería, y en los dos casos sería un dato inventado atribuido a personas, contra la definición de
  «presentable» de S136.
- **Medido al abrir (S164, Claude Code, solo lectura, HEAD `fc9076d`):** (1) El PUT valida profesor (404),
  tramo lectivo por `(dia, ordenEnDia)` —el recreo no tiene `ordenEnDia` y da 400—, tipo (400) y duplicados
  (400); la lista vacía borra; es reemplazo total y transaccional. `peso` se escribe siempre a 1 y no viaja;
  `motivo` viaja sin validar. Sin UNIQUE ni índices. (2) `PROFESOR_SOBRECARGADO` ya compara la carga con los
  tramos libres tras las DURA y bloquea con 422: no es trabajo. (3) DURA y BLANDA sólo restringen el tramo
  de INICIO (`ModeloCpSat.java:497-506`, `:652-657`); con `duracionTramos > 1` un bloque puede ocupar un
  tramo vetado. Por lectura; los bancos no tienen actividades de más de un tramo. (4) `ReglaDura` no tiene
  regla de indisponibilidad, y `MovimientoInstanciaService` valida con el verificador. (5) Nada detecta un
  pin sobre un tramo DURA antes del solver; qué devuelve CP-SAT en ese caso, NO DETERMINADO. (6) La guarda
  de solo lectura cubre el PUT sin configuración. (7) El PUT de `/api/jornada` responde 409 en cuanto existe
  una restricción: `D-jornada-congelada-por-disponibilidad`. (8) Frontend: nada alcanza el endpoint; la
  rejilla del horario no sirve (filas fijas, exige sesiones, lleva arrastre); no hay librería de iconos ni
  tema oscuro; `--color-ok-fondo` no tiene uso.
- **Decisiones (S164):** (A, del usuario) El marcado en bloque, por día y por tramo, entra en el criterio.
  (B, del arquitecto, aceptada) Modelo de pincel y no rotación por clic: con rotación, pulsar un día con
  celdas en estados distintos no tiene resultado inequívoco. (C, del arquitecto; el usuario lo proponía
  editable) El recreo no es editable: en él no se programa nada, el backend lo rechaza y una marca ahí no la
  leería nadie; una guardia de recreo es una asignación, no disponibilidad. (D) Los hallazgos (3), (4) y (5)
  entran en el criterio y no son deuda: sin ellos la rejilla promete algo que el horario no cumple. (E)
  Dirección de los símbolos: disponible = celda vacía; no puede = prohibido sobre fondo de error; prefiere no
  = «−» sobre fondo de aviso. La forma, los tonos medidos contra los tokens y SVG en línea frente a Unicode
  se deciden en la maqueta de `C-rejilla-disponibilidad`.
- **Decisiones (S165):** (F, del usuario, REVOCADA en el mismo cierre) Se aceptó reescribir la
  condición 5 para rechazar el bloque con disponibilidad en vez de resolverlo, y
  se revocó al medirse falsa la premisa del cambio de plan: la interfaz permite
  actividades de más de un tramo (`actividad-form.html:30-35`, fijado por el test
  de `actividad-form.spec.ts:542`). Con la reescritura, un centro con sesiones
  dobles y disponibilidad del mismo profesor no podría generar horario. El
  arreglo del solver vuelve a `C-dura-completa`; el cortafuegos se mantiene hasta
  que el solver cubra el caso, y entonces se decide si se retira. (G,
  del usuario) El arreglo futuro del modelo no reutiliza
  `VerificadorSolucion.tramosOcupados`, para que el verificador siga siendo un
  oráculo independiente.
- **Decisiones (S166):** (H) No se reescribe la condición 6, que sería repetir F: el controlador construye el cuerpo del 422 de prevalidación reutilizando `respuestaDeFallo`/`FalloGeneracionDTO` de S118, con causa nueva `PREVALIDACION_FALLIDA`; no se paga la clave global, `D-F8.6-ii-a` sigue viva. (I) La invariante de indexación se valida en el constructor de `ProblemaHorario` (opción A): tramos ordenados por (día, ordenEnDia) y sin dos con el mismo par; se admiten huecos en ordenEnDia. El modelo calcula los tramos que cubre un inicio como t..t+d−1, la misma definición que los intervalos del no-solape. (J) Se retira el cortafuegos `RESTRICCION_HORARIA_CON_BLOQUE`, en un commit posterior al arreglo del solver, como exigía su deuda.
- **Decisiones (S167):** (K) Símbolos en SVG en línea con `currentColor` y tokens existentes: «No puede», círculo con barra en `--color-error` sobre `--color-error-fondo` (6,35:1); «Prefiere no», raya en `--color-aviso` sobre `--color-aviso-fondo` (5,33:1). Los fondos no se distinguen de la celda vacía (1,19:1 y 1,11:1) y no hace falta: la forma lleva la distinción, comprobado en escala de grises en la maqueta. Unicode descartado: U+1F6C7 puede salir como emoji a color y no seguir el token, y U+2298 depende de la fuente del sistema. (L) M-mockup se aplicó dentro de una sesión con fases: la inversión de M2 se admite porque ningún código de producción precedió al M2 (precisión en `metodo.md`). (M) `D-F8.6-ii-a` se salda dentro del Cambio (R-deuda): bloqueaba la condición 4, porque el 404 se provoca desde la interfaz borrando al profesor en otra pestaña; reinterpretar la condición habría repetido F, y se descartó un cuarto rodeo local. (N) El `motivo` se conserva sólo si la celda mantiene su tipo; se pierde al cambiar de tipo o al pasar a «Disponible». Una restricción cuyo tramo no está en la rejilla se reenvía tal cual. (O) Las horas van en las cabeceras de fila, como pide la condición 1: la decisión D8 de la rejilla del horario no aplica aquí, y su riesgo es `D-hora-tramo-dependiente-de-zona`. Con la jornada sin guardar, el diálogo avisa y no ofrece la rejilla, porque todo lo marcado acabaría en un 400.
- **Cambios:** `C-alcance-disponibilidad` ✔ HECHO (S164, este criterio). `C-dura-completa` ✔ HECHO (S165–S166): regla `INDISPONIBILIDAD_PROFESOR` y BLANDA por tramo ocupado en el verificador y `PIN_SOBRE_TRAMO_DURA` en la prevalidación (S165); el 422 de prevalidación con cuerpo propio, el solver vetando y penalizando todos los tramos ocupados, la invariante de tramos en `ProblemaHorario` y la retirada del cortafuegos `RESTRICCION_HORARIA_CON_BLOQUE` (S166).
  `C-rejilla-disponibilidad` ✔ HECHO (S167, `6a29f65`, `fae2168`, `1d8a6d1`): clave de error de Boot 4 con test por HTTP real; modelo, servicio y lógica pura de la rejilla; diálogo con pincel abierto desde la lista de profesores.
- **ESTADO en S164: 0 de 7.**
- **ESTADO en S165: 0 de 7.** La 5 está a medias: el verificador ya cuenta DURA
  y BLANDA por tramo ocupado y el movimiento manual a un tramo vetado se rechaza,
  pero el solver sólo veta el tramo de inicio (`D-indisp-solo-tramo-de-inicio`);
  mientras tanto, el cortafuegos `RESTRICCION_HORARIA_CON_BLOQUE` rechaza la
  generación en ese caso. La 6 está construida y probada en MockMvc, sin contar:
  falta comprobar que el motivo del 422 llega en el cuerpo HTTP real (riesgo:
  `D-F8.6-ii-a`).
- **ESTADO en S166: 2 de 7.** Se cumplen la 5 —el solver veta y penaliza todos los tramos que ocupa cada sesión, probado con tests que fallaban antes y de punta a punta con mutante— y la 6 —el 422 nombra al profesor y al tramo en el cuerpo HTTP real, y la vista lo muestra—. Queda `C-rejilla-disponibilidad` (condiciones 1 a 4 y 7).
- **ESTADO en S167: 7 de 7. TERMINADO.** Se cumplen la 1 a la 4 y la 7, verificadas en navegador sobre base vacía (M4 de S167): rejilla con horas y recreo no editable, tres estados que se distinguen por la forma, pincel por celda, día y tramo, guardado con reapertura, y los rechazos 404 y 403 con su mensaje. La 7, con predicción escrita antes de generar: 28 DURA y 1 BLANDA puestas desde la rejilla, diagnóstico sin violaciones de `INDISPONIBILIDAD_PROFESOR`, 1 BLANDA penalizada y OPTIMAL con objetivo 1.0, que sólo es posible si los 19 inicios de coste 0 están prohibidos.
- **Deudas:** `D-F8.5-E-b` cuelga de aquí y NO bloquea (§4). `D-F8.5-E-a` sigue como limitación conocida.
- **Fuera:** las preferencias positivas (modelo §7), la calibración de `peso` (D21) y la gestión de guardias —repartir guardias de recreo o de aula—, que sería un objetivo propio (S164).

### H1 — Ajustar (cierre)

#### O-ajuste-cierre — "El ajuste manual está completo y verificado." ✔ TERMINADO (S146)
- **Propósito:** cerrar los HUECOS FUNCIONALES reales del ajuste, no la cobertura
  de tests.
- **Terminado cuando** — REESCRITO en S142 sobre medición (precedente de forma y
  coste: S138, que reescribió el criterio de O-particiones). El anterior estaba
  marcado INVÁLIDO desde S135 y sus dos mitades estaban cumplidas, de modo que
  leído al pie de la letra el objetivo estaría terminado. O-ajuste-cierre termina
  cuando:
  1. Un ajuste manual **persiste sin regenerar el horario entero**. Sobre un centro sin
     holgura la operación demostrable es el **INTERCAMBIO de dos instancias en una sola
     transacción**; el movimiento a un tramo libre queda como caso del mismo servicio,
     construido y cubierto desde S143, verificable sobre un centro que tenga holgura.
     **REESCRITA en S144, y el enunciado anterior —«una sesión se recoloca a otro tramo y
     persiste»— se conserva como caso particular, no se deroga.** La razón no es que el
     movimiento esté mal, sino que sobre este centro no puede producir ni una operación
     válida, y un criterio cuyo verde sólo existe contra fixtures sintéticas es el mismo
     defecto que S142 desmontó en el criterio anterior.
     **NOTA DE S143, medida y no supuesta:** la mitad SERVIDORA del movimiento está
     construida y medida (`PUT /api/horarios/{id}/instancias`, veredicto por diferencia,
     14 tests, commits `8729e62` y `c5df06a`). Pero no es producible sobre el centro real:
     los 28 grupos están a 30/30 tramos y no hay plazas sin subgrupo, así que un barrido de
     los 29 destinos rechazó 29 de 29, en una instancia de 6 filas y en otra de 1. Lo que
     este centro necesita es INTERCAMBIAR, y un intercambio no se compone de dos movimientos
     porque el estado intermedio es inválido y lo rechaza el propio veredicto.
     **MITAD SERVIDORA CUMPLIDA en S144, y la reescritura NO se hizo a ciegas:** antes de
     escribir una línea de producto, un arnés de solo lectura midió sobre el centro real que
     el intercambio válido EXISTE —10 678 pares que comparten grupo, **1 774 válidos**, de
     los que 979 son entre actividades distintas—, con el criterio de aborto escrito ANTES de
     medir (cero válidos habría obligado a una TERCERA reescritura). Demostrada por HTTP
     sobre el centro real: 200, filas y pares plaza->aula comprobados, y **`GET
     /{id}/diagnostico` devolviendo CERO violaciones duras después**, que es la prueba
     independiente —el veredicto del propio endpoint no vale como su propia prueba—.
     **Falta la mitad de INTERFAZ**, que vive en el Cambio del gesto.
     **CUMPLIDA ENTERA en S145.** La mitad de INTERFAZ, demostrada en navegador sobre el
     centro real: soltar sobre celda ocupada emite el intercambio, soltar sobre celda vacía
     el movimiento, y la decisión la toma el contenedor contando los ocupantes de la celda
     destino, sin calcular validez en el cliente. El ajuste PERSISTE y lo certifican dos
     endpoints que no lo escribieron: `/diagnostico` devolvió cero violaciones duras y
     `/proyeccion` las dos instancias en su tramo nuevo. `cmp -l` localizó los bytes de la
     escritura: enteros de `sesion.tramo_inicio_id` permutados y los dos de la cabecera
     SQLite, y nada más.
     **Demostrada además sobre par NO degenerado y MULTIPLAZA**, después de que la primera
     pasada cayera sobre un par de la misma actividad —de los 795 que S144 censó aparte—
     donde reagrupar mal no se habría notado. El par: `Bloque-ALCT_Fr2-1ºC` #1, de dos filas
     y dos aulas, contra `Geo-1ºC` #2, de una. Tres evidencias que el par degenerado NO podía
     dar: la respuesta llegó como objeto con `primera` y `segunda` de **longitudes distintas,
     2 y 1**, y con `actividadCodigo` distinto en cada lado, que es donde concatenar y
     reagrupar sí se notaría; los totales BLANDOS se movieron —ventanas 206 -> 205,
     consecutivas 9 -> 10— cuando el degenerado los dejó clavados en 206/9, que es la prueba
     numérica de que este intercambio mueve recursos de verdad; y `cmp -l` localizó **TRES**
     bytes de `sesion.tramo_inicio_id` y no dos —las dos filas del bloque y la de Geo—, que
     es la firma de la multiplaza. La rejilla pintó cada lado sin recargar:
     `XMLHttpRequest` instrumentado antes de soltar registró **una sola petición, el PUT, y
     ningún GET**.
  2. Al intentarlo sobre un tramo que viola una regla dura, la interfaz **nombra el
     recurso concreto** que la causa y, cuando la regla NO tiene recurso que nombrar,
     muestra la regla y las celdas culpables. El veredicto lo emite el SERVIDOR; la rejilla
     lo muestra sin calcularlo — el javadoc de `slotsOcupados` rechaza expresamente
     crecer hacia una verificación, para no fabricar un cuarto espejo de las
     restricciones, y ese argumento se respeta.
     **PRECISIÓN RESUELTA en S145** (medida en el M4 de S144 y no anticipada entonces):
     `DISTRIBUCION_MISMO_DIA` es regla dura y **no tiene recurso que nombrar** —el conflicto
     es de la actividad consigo misma—, así que leída al pie de la letra esta condición tiene
     una rama que nadie puede cumplir. Se resuelve en el Cambio del gesto: o se pinta ese caso
     sin recurso, o se precisa la redacción. La mitad de SERVIDOR queda reconfirmada sobre
     datos reales: un rechazo del intercambio devolvió 8 violaciones nuevas con
     `recursoCodigo`, `tramoCodigo` y las celdas culpables —`SOLAPE_AULA A3`,
     `SOLAPE_PROFESOR ING5`, `SOLAPE_SUBGRUPO`, `SOLAPE_GRUPO`—, cuatro por cada destino.
     Se hicieron las dos cosas y no eran alternativas: se pinta el caso sin recurso con lo
     que el servidor sí manda, y se precisa la redacción. NO es una sexta condición —es la
     misma dicha con precisión—, así que no dispara el replanteo del objetivo. **CUMPLIDA
     ENTERA en S145**, y el caso cayó SOLO en la UI durante el M4, no fabricado: un rechazo
     real pintó `SOLAPE_AULA` con su aula, `SOLAPE_PROFESOR` con su profesor y un
     `DISTRIBUCION_MISMO_DIA` con sus dos celdas y sin recurso. Ni `null`, ni `undefined`,
     ni violación oculta.
  3. La prevalidación **comprueba S8** —única invariante verificable sin solución,
     según `VerificadorSolucion:43,56`— y no declara «catálogo sano» sobre lo que no
     ha mirado.
     ✔ **CUMPLIDA (S146).** La prevalidación comprueba S8 REUTILIZANDO la del solver —`VerificadorSolucion`
     gana un público `verificarTutorias(ProblemaHorario)` que delega en el privado; ningún espejo nuevo—, como
     cuarta regla de `PrevalidacionService` y con severidad **AVISO**, no ERROR: S8 no depende de la
     colocación, el horario generado es válido en todo lo demás y se corrige cambiando el tutor sin regenerar,
     y un bloqueo empujaría a falsear tutores para poder generar. El estado vacío del panel deja de decir
     «Catálogo sano»: dice que no hay hallazgos y que eso no garantiza horario en el tiempo previsto.
     Demostrada sobre el centro real por MUTACIÓN sobre copia y por la vía legítima —`PUT
     /api/grupos/{id}/tutoria`—, con la predicción escrita antes de medir y confirmada por un oráculo SQL
     independiente antes de tocar la base servida. Sin tocar, `[]`. Con 1ºA sin tutor, 2ºB con su tutor
     degradado a co-tutor y 3ºA sin tutor, exactamente {TUT1-1ºA, TUT2-2ºB} como AVISO 1/0, y
     TUT3-3ºA+3ºADi AUSENTE —basta un grupo cubierto, y el PUT al padre no se propaga al PDC—. `/diagnostico`
     dio el mismo conjunto. Restaurado, `[]` y `profesor_tutoria` idéntica. Es el primer AVISO de la
     aplicación visto en vivo.
  4. Ninguna acción irreversible de diez minutos se dispara sin confirmación
     explícita.
     ✔ **CUMPLIDA (S145).** `generar()` abre SIEMPRE el diálogo `ConfirmarGeneracion`,
     también con lista de avisos vacía, y la plantilla —que estaba muda en ese caso— pasa a
     decir el coste: unos diez minutos, sustituye el trabajo en curso, no se deshace.
     Verificado en navegador: cancelado con Escape y con backdrop, cero peticiones y las
     filas de `horario` sin variar.
  5. ✔ **CUMPLIDA (S142):** una sesión bloqueada no se mueve al relanzar.
     Verificado por MUTACIÓN sobre el centro real, contra 96 % de ruido de fondo:
     tres corridas de control con el pin en el tramo 10 y la instancia en el 10;
     mutado a 34, la instancia y sus tres plazas al 34. Queda descartada la pérdida
     silenciosa por identidad de objeto que advierte el javadoc de `BloqueoMapper`.
     **Nota de alcance:** pin de TRAMO verificado; pin de AULA no ejercitado, y sólo
     alcanzaría al 13 % de las sesiones (109 de 819; 279 plazas de aula fija contra
     37 variables).
     **Nota de S145:** vuelve a ser ALCANZABLE. El Cambio del gesto retiró el pin del
     arrastre y con él la única vía de crear un pin, que §1 exige en el paso 4 del guion de
     aceptación; se restauró en la misma sesión con gesto propio —el candado alterna—, y se
     verificó en navegador que el aviso «N pines sin aplicar» vuelve a poder CRECER, que era
     el síntoma exacto de la capacidad perdida. Reparar lo que el propio Cambio rompe es
     terminarlo, no ampliarlo.

  **Fuera del criterio, con argumento:** el pin de aula (no lo pide y su alcance es
  marginal); el detalle de la atribución por `title` sin foco de teclado
  (accesibilidad, sede O-diseño); la cobertura del gesto del CDK (cobertura, familia
  `D-F8.6`, que el propósito de este objetivo excluye por escrito).
  **AMPLIADO en S145**, cada uno con su razón: la navegación por teclado de los candados
  (`D-candados-en-tabulacion`, sede aquí, no se paga); el lado de la instancia inexistente
  viajando en prosa (`D-lado-instancia-en-prosa`); y la ceguera del gesto fuera de la vista
  por grupo (`D-ajuste-ciego-fuera-de-vista-grupo`). Ninguna cambia el criterio.
- **Depende de:** O-shell (dónde vive la vista).
- **Valor:** cierra H1 de verdad.
- **Cambios que agrupa:** las condiciones **1 a 4** del criterio nuevo —recolocar
  una sesión y que persista; nombrar el recurso concreto que causa el conflicto, con
  el veredicto emitido por el servidor; comprobar S8 en la prevalidación; y exigir
  confirmación explícita antes de una acción irreversible de diez minutos—. La
  descomposición de esas cuatro condiciones en Cambios es **el primer trabajo del M0
  de la próxima sesión**, y no se adelanta aquí. **CORREGIDO en S142:** este bullet
  decía «el gesto de despinar», cerrado desde S83 (medido en el M0 de S134).
  **HECHA YA en S143, y no queda pendiente para ningún M0 futuro:** la descomposición
  salió en **TRES** Cambios y no en cuatro. (i) `C-mover-sesion-backend` — el endpoint
  de recolocación con su veredicto — **HECHO** (commits `8729e62` y `c5df06a`). (ii) el
  gesto en la rejilla, que cubre la mitad de interfaz de las condiciones 1 y 2: (1) y
  (2) son el mismo endpoint —su camino verde y su camino de rechazo— y se parten en
  backend y gesto por dependencia técnica real, no por tamaño. (iii) la prevalidación
  S8 de la condición 3. La condición **(4) NO es Cambio propio**: se arregla en
  `generar()` (`horario-view.ts:342`) abriendo siempre el diálogo, y cae dentro del
  Cambio del gesto. **Y (3) y (4) NO son la misma familia a efectos de arreglo**,
  contra lo que el traspaso de S142 daba por hecho: S136 midió que los 7 tutores de
  Bachillerato son falsos pero PORTANTES, así que S8 pasa; meter S8 en la
  prevalidación seguiría devolviendo lista vacía y el diálogo seguiría inalcanzable.
  **CORREGIDO en S146 (R5): este dato estaba CADUCADO cuando se escribió.** S137 reescribió las 7 tutorías
  de Bachillerato con la lista oficial del centro y bajó `requiereTutor` a 0 en las 6 actividades de bloque, y
  la base de referencia `s137-centro-completo` es la que lo lleva: el M2 de S146 midió 7 de 7 tutores
  oficiales y 16 de 16 actividades cumpliendo S8. S8 pasa porque el catálogo es CORRECTO, no porque lo
  sostenga un andamio. La conclusión práctica —la lista vacía— era cierta igual; su lectura no: el vacío era la
  respuesta correcta, y lo que faltaba era demostrar que la comprobación corre. Vale también para la segunda
  aparición de la frase, más abajo en este mismo bullet.
  **EJECUTADO en S144: `C-intercambiar-instancias` HECHO.** Son DOS los Cambios hechos y
  quedan DOS: el gesto en la rejilla —que absorbe la mitad de interfaz de las condiciones 1
  y 2 y la condición 4— y la prevalidación S8 de la condición 3. **EL OBJETIVO NO SE PARTE,
  decidido en el M0 de S144 contra la pregunta que dejó S143:** la única señal escrita que lo
  justificaría es la de §7 —más de ocho sesiones— y van cuatro Cambios; el corte «motor de
  ajuste» / «cierre de H1» no cae limpio, porque dejaría partidas entre dos objetivos las
  condiciones 1 y 2, que tienen su mitad de servidor hecha y la de interfaz en el mismo
  Cambio del gesto; y sigue en pie el argumento de S142 de que un objetivo llamado «el ajuste
  manual está completo» que excluyera recolocar una clase tendría el nombre mintiendo.
  **Disparador escrito para reabrirlo:** si al cerrar el Cambio del gesto el objetivo pasa de
  ocho Cambios, o si aparece una sexta condición, se replantea con §7 en la mano.
  **EJECUTADO en S145: `C-gesto-rejilla` HECHO.** Son TRES los Cambios hechos y queda UNO: la
  prevalidación S8 de la condición (3). El disparador de replanteo NO se dispara: son cinco
  Cambios, por debajo del ocho de §7, y no ha aparecido ninguna sexta condición —la precisión
  de la condición (2) es la misma condición dicha con precisión—. **Y el orden se decidió con
  argumento, no por inercia:** la prevalidación S8, sobre ESTE centro, no puede producir un
  verde observable, porque S136 midió que los 7 tutores de Bachillerato son falsos pero
  PORTANTES y S8 pasa; hacerla primero habría repetido el defecto que S142 desmontó y S143
  confirmó.
  **EJECUTADO en S146: `C-prevalidacion-s8` HECHO, y con él el objetivo CIERRA** con cinco Cambios
  —`C-alcance-ajuste`, `C-mover-sesion-backend`, `C-intercambiar-instancias`, `C-gesto-rejilla` y este— en
  cinco sesiones (S142–S146), por debajo del ocho de §7: el disparador de replanteo no se disparó nunca. La
  premisa sobre los 7 tutores estaba caducada (corrección de S146, arriba), y el orden decidido se sostiene
  igual: sobre este centro la lista vacía seguía sin poder dar un verde observable.
- **Absorbe:** la única deuda FUNCIONAL de F8.6 (D-F8.6-ii-b). El resto de la
  familia F8.6 NO entra aquí (ver §4): es cobertura o superficie de error, no
  hueco funcional.

#### O-diseño — "La aplicación tiene un aspecto cuidado y coherente." ✔ TERMINADO (S133)
- **Propósito:** trabajar la maquetación y la identidad visual de la aplicación
  —sistema de estilos, tipografía, tokens de color, consistencia entre vistas—,
  transversal a todas las pantallas. NO es maquetar una vista suelta: es el acabado
  visual del conjunto.
- **Terminado cuando (DEFINIDO en el M0 de S121, sobre el inventario de superficie):**
  1. Existe una capa de tokens en `src/styles.css` —color, tipografía, espaciado,
     radios, sombras— y NINGÚN CSS de componente contiene un color literal ni un
     `font-size` literal. Verificación binaria por grep: hex, `rgb(`, `hsl(` y
     `font-size` con valor crudo fuera de `styles.css` = 0.
  2. La tipografía está gobernada desde un solo sitio: existe `font-family` de
     proyecto y una escala cerrada de tamaños.
  3. Las tres vistas y los tres diálogos aplican el sistema sin regresión funcional:
     suites intactas y verificación en navegador.
  4. Seis decisiones de identidad escritas y aplicadas: paleta, tipografía,
     densidad/espaciado, tratamiento de cabecera y marca, tratamiento de estados
     (error, vacío, cargando) y tratamiento de tablas y rejilla.
  El criterio se definió como SISTEMA, no como maquetación vista a vista, y la razón
  es de invalidación: O-particiones toca frontend (su propia ficha lo dice), así que
  una UI nueva posterior debe poder APLICAR el sistema sin rehacerlo. Ése fue el
  argumento que permitió NO invertir el orden de §5. El punto 4 arrastra un juicio
  que el asistente no puede emitir: el juez del aspecto es el arquitecto, en una
  pasada final por navegador. Declarado en el M0 en vez de fingir objetividad total.
- **Fuera del criterio, decidido expresamente en el M0 de S121 (R-terminado):** las
  seis deudas de UX o estructura que la tabla §4 le cuelga —D-selectores-sin-busqueda,
  D-actividad-forma-implicita, D-configuracion-monolitica, D-actividad-ux,
  D-subgrupo-ux-multiselect, D-monodi-botones-inertes— porque si entran, esto deja de
  ser acabado y pasa a ser rehacer la UI. Fuera también D-dialogo-foco-perdido pese a
  estar asignada aquí: es comportamiento, mismo corte que se aplica a
  D-vista-horario-sin-horario. Y fuera el responsive: el inventario midió CERO `@media`
  en todo el proyecto, y añadir puntos de ruptura duplica el trabajo sin servir al
  producto (portátil de jefe de estudios, rejilla 6×5, tablas sin diseño móvil
  pensado). El criterio pide que las vistas no se rompan en UNA resolución declarada,
  la de la demo. Nace D-sin-puntos-de-ruptura.
- **Cambios que agrupa (creados en el M0 de S121; la ficha no los tenía, y crearlos
  fue trabajo de esa apertura, igual que el M0 de S115 con O-demo):**
  - **C-tokens** — la capa en `styles.css` y el pago de paso de D-bundle-presupuesto.
    **HECHO en S121.**
  - **C-sustitución** — los literales de color y tamaño de las 26 hojas pasan a
    `var(--x)`. **HECHO en S121.**
  - **C-identidad** — cabecera, marca, landing y aspecto de los estados transversales.
    **HECHO en S128**, en dos bloques sin dependencia técnica entre ellos, ordenados por riesgo.
    *Bloque 1:* barra con fondo de acento pleno (`--color-sobre-acento` y su variante tenue nacen
    aquí, contrastes medidos 8,67:1 y 5,76:1), marca preparada para el logo que aún no existe,
    landing con el PRIMER `<h1>` del proyecto y tarjetas sobre superficie, leyenda de la insignia
    de coste blando, y la decisión 3 escrita. *Bloque 2:* `app-estado-lista`, que funde las cuatro
    ramas de estado que las siete listas repetían **carácter a carácter** —estructura de hash
    idéntico en las siete, medido— y borra ~21 reglas CSS repartidas por siete hojas, cinco de
    ellas ya byte a byte la misma. Cierra la salvedad del criterio 1 de O-navegación,
    `D-insignia-sin-leyenda` y `D-vacio-miente-con-error`.
  - **C-revisión** — pasada por las tres vistas y los tres diálogos, M4 en navegador,
    el arquitecto como juez. **PARTIDO EN TRES TRAMOS en el M0 de S129**, por dependencia real y no por
    tamaño, al medirse que no era una pasada de juicio: de las seis decisiones de identidad sólo UNA
    estaba escrita y rotulada, una fuera de `styles.css` sin rótulo, una a medias y TRES sin constancia
    en ninguna parte, luego el criterio 4 estaba sin cumplir y cumplirlo exigía decidir, no transcribir.
    La dependencia es la misma que validó la partición de S125: no se puede juzgar el acabado de algo que
    aún no se ha aplicado, y el veredicto del juez genera trabajo.
    - *Tramo 1 — el criterio 4.* **HECHO en S129**: las cuatro decisiones que faltaban escritas (1 paleta,
      2 tipografía, 5 estados, 6 tablas y rejilla) más la 4 rotulada en `app.css`, los 23 literales de peso
      a la escala, y las tres deudas que son su aplicación —`D-prevalidacion-contraste-sin-ver`,
      `D-contador-se-apaga-con-error` y `D-desbordamiento-sin-etiqueta`, las tres CERRADAS—. Cero reglas
      CSS cambian en `styles.css`: +189 líneas, todas comentario.
    - *Tramo 2 — la aplicación del criterio 3.* **HECHO en S130**: los 195 valores de espaciado a la
      escala, 171 exactos y los 24 restantes redondeados y DECLARADOS uno a uno en la entrada de S130
      del plan; y el radio unificado en DOS niveles sobre 36 reglas —21 controles a `--radio-s`, que
      sube de 3 a 4 px A PROPÓSITO para que ninguno de los 20 que ya estaban en 4 px literal se mueva,
      y 15 contenedores a `--radio-m`—. **«El radio de los once controles» era un recuento FALSO**: el
      literal `border-radius: 4px` no vivía en diez reglas sino en 36 sitios, doce de ellos
      contenedores, y ese hecho es el que tumbó el esquema de un solo nivel que el arquitecto había
      elegido —unificar sólo los once habría dejado el diálogo contenedor a 4 px junto a un control a
      6 px, moviendo la incoherencia de sitio en vez de cerrarla—. Único píxel de radio que se mueve en
      toda la aplicación: el buscador de las listas, de 6 a 4 px. Bundle +1,18 kB crudos y −0,05
      transferidos: repetir un token comprime mejor que dispersar literales.
    - *Tramo 3 — el juicio.* **APLICADO EN S132, SIN JUZGAR**. El recorrido se hizo en S130 sobre la UI aplicada y produjo
      trabajo que su previsión no contemplaba: aplicar de verdad la jerarquía de acciones a los 34
      botones (`D-jerarquia-declarada-sin-aplicar`), el `padding` lateral de `.app__contenido`, la
      flecha nativa de `D-select-nativo-desparejo` y `D-tokens-sin-uso`. Cierra el objetivo.
      **S132 EJECUTÓ ESOS CUATRO PUNTOS** (ver el bloque de S132 más abajo). Lo que le queda al tramo es el
      JUICIO sobre lo aplicado, y por eso el objetivo no cierra ahí.
    Los tramos 2 y 3 SE FUNDIERON, decidido en el M0 de S130 con el censo delante y ejecutado: el M4 de
    la aplicación FUE el recorrido. La fusión se cumplió; lo que no se cumplió fue su previsión, porque
    el recorrido encontró trabajo nuevo. Lo que S131 hereda es un tramo 3 REPARTIDO DE NUEVO, no el
    tramo 3 original.
- **Depende de:** O-demo. **RAZÓN REVISADA en S115, y el cambio importa.** La dependencia
  escrita era «H2 cerrado» por R-invalidación: O-estructura añadía los formularios pesados de
  currículo/desdobles/PDC/tutores/jornada, que reorganizan Configuración entera, y maquetar
  antes era pulir superficie que se va a reubicar. **Esa razón se consumió al cerrar
  O-estructura en S114:** las vistas están congeladas. Lo que queda de dependencia es más
  estrecho: O-DEMO, porque es quien puede destapar un caso inexpresable y con él un formulario
  nuevo que habría que maquetar después. NO depende de O-particiones ni, por tanto, del cierre
  formal de H2. **DEPENDENCIA CONSUMIDA, verificada en el M0 de S121 por tres vías y no por
  una:** S115 contrastó el catálogo COMPLETO contra la UI por lectura y todo cabía; S116 metió
  804 escrituras por los mismos servicios que respaldan los formularios; S120 tecleó a mano el
  caso más difícil del centro y no destapó ninguno. Lo que le falta a O-demo son 11 actividades
  de FPB con aula (D31-a), de la misma forma que las 208 ya cargadas, así que el riesgo residual
  de formulario nuevo es despreciable. Consecuencia registrada: **O-diseño se abre con O-demo
  todavía ABIERTO**, que es la primera vez que dos objetivos conviven en el mapa. Nada del método
  lo prohíbe —R-apertura solo exige nombrar los tres términos— y O-demo no está pausado por
  conveniencia sino bloqueado por una respuesta externa (el correo al centro).
- **Valor:** presentabilidad. Hasta aquí los hitos se definen por función; este es
  el único objetivo puramente de acabado. Registrado en S106 a petición del
  arquitecto: sin sede propia, la maquetación o no se hace nunca o se cuela a trozos
  dentro de otros objetivos violando R-terminado ("pulir CSS ya que estoy en esta
  vista"). Tenerlo como objetivo lo protege por ambos lados.
- **Salvedad de prioridad: ACTIVADA en S115.** Decía que si hay que ENSEÑAR la app a un
  cliente o al IES antes de cerrar H2, el aspecto deja de ser estético y pasa a ser
  presentabilidad de demo, que sí es valor entregable. El arquitecto confirmó en S115 que HAY
  demo al centro en el horizonte, sin fecha fija y previsiblemente antes de la Fase 9. Orden
  resultante: **O-demo → O-diseño → demo → O-particiones → cierre de H2.** La demo NO espera
  al cierre formal de H2. Razón de que O-diseño vaya detrás de O-demo y no delante: sin fecha
  que apriete, maquetar antes de saber si la UI está completa es apostar a que O-demo no
  destapa ningún formulario nuevo. Si apareciera una fecha corta, lo racional es invertir el
  orden y aceptar retocar lo que salga.
- **Registrado en el cierre de S122, sin cambiar el criterio:**
  - **La decisión 3 de las seis (densidad/espaciado) SIGUE SIN SEDE.** *(SEDE FIJADA en el M0 de
    S128; ver el bloque de S128 más abajo.)* El espaciado NO
    está tokenizado: C-sustitución cubrió color y `font-size`, que es lo que el criterio 1
    verifica por grep, y nada más. La prueba es `padding-top: 16px` en
    `horario-grid.css:43-45`, un literal que sobrevivió entero a la pasada. Los tokens
    `--e1..--e6` existen en `styles.css:63-68` y no gobiernan el CSS de componente.
    Decidir su sede —C-identidad o un Cambio propio de tokenización— queda pendiente.
  - **La pregunta que S121 dejó abierta sobre las insignias `1`/`-1` ESTÁ RESUELTA:** son
    la suma CON SIGNO del coste blando de la instancia, calculada por el contenedor a
    partir de `GET /api/horarios/{id}/diagnostico` (medición y evidencia en
    `docs/diseno-navegacion.md` §A1). `>0` significa que mover la instancia mejora; `<0`,
    que tapa un hueco. **Su leyenda —tooltip y etiqueta accesible, como las del candado—
    es material de C-identidad**; nace `D-insignia-sin-leyenda`. **PAGADA en S128, en esa sede.**
  - **La «resolución declarada» que este criterio invoca se declaró en S122: 1920×1080.**
    Hasta entonces la ficha la exigía sin que nadie la hubiera fijado en ningún documento.
    La resolución del portátil queda como PARÁMETRO SIN FIJAR.

- **Registrado en el cierre de S128, sin cambiar el criterio:**
  - **LA DECISIÓN 3 (densidad/espaciado) YA TIENE SEDE, y no es un Cambio propio.** Se ESCRIBE en
    `styles.css` —la regla de uso de la escala `--e1..--e6`, que existía desde C-tokens sin
    gobernar nada— y se APLICA sólo donde C-identidad y C-revisión tocan. **No se abre un Cambio de
    tokenización de espaciado**, con argumento y no por comodidad: C-sustitución fue viable porque
    el color tiene equivalencia EXACTA (`#666` → `var(--color-borde)`, mismo valor), y el
    espaciado no —tokenizar obliga a elegir escalón y mueve píxeles—. Los **cuatro** literales que
    no caían en un escalón se redondearon; su declaración **no llegó a escribirse en S128** y se
    recupera en S130 desde el diff `cd6b43f..94b97df`, verificada línea a línea sobre los blobs:
    `app.css` `padding: 0.75rem 1.25rem` → `var(--e3) var(--e5)` (+4 px, sólo el horizontal),
    `app.css` `gap: 1.25rem` → `var(--e5)` (+4 px), `landing.css` `gap: 0.4rem` → `var(--e2)`
    (+1,6 px) y `landing.css` `padding: 1.25rem` → `var(--e5)` (+4 px). El «tres» que esta ficha
    afirmó durante dos sesiones era falso, y lo era porque nadie enumeró; la afirmación de S128 de
    que ninguno tocaba la altura de la rejilla SÍ era cierta —el vertical de la barra cae exacto en
    `--e3`— pero no era comprobable. Nace `D-declarado-sin-artefacto`. **Los 24 redondeos del tramo
    2 (S130) están medidos y reproducidos por dos derivaciones independientes, y quedan PENDIENTES
    DE ENUMERAR en la entrada de S130 del plan; mientras esa lista no exista, no se consideran
    declarados.** **EXCLUSIÓN EXPRESA: la geometría de `horario-grid.css` no se tokeniza**, porque
    sus valores son presupuesto MEDIDO contra el criterio 4 de O-navegación (`diseno-navegacion.md`
    §4) y no elecciones de densidad; el reparto vive a 0,8 px de un escalón de recorte, así que
    sustituir un número medido por uno elegido es riesgo puro. Lo que quede de las 27 hojas se pasa
    en C-revisión.
  - **RESTRICCIÓN QUE C-IDENTIDAD DESCUBRIÓ Y QUE C-REVISIÓN HEREDA: la barra de la aplicación
    puede ENCOGER, NO CRECER.** El javadoc del marco flex de `styles.css` dice que «si la barra
    cambia de alto, el reparto se rehace solo», y eso describe el LAYOUT y no el CRITERIO: con
    `flex: 1` en `.app__contenido`, lo que la barra engorda sale del hueco de la rejilla, que es el
    presupuesto del criterio 4. Cada ~7 px de barra bajan `--alto-celda` ~1 px, y las 22 celdas de
    cuatro plazas se pasan hoy 1,23 px sin llegar a marcarse: al cruzarlo, las 50 marcas pasan a
    72. La barra bajó de 52 a **51 px** y no declara `height` ni lo hará. **Si algún trabajo
    posterior mueve su alto, se recuentan las marcas `+N` de 1B-A, 4ºA y 2B-B (4 / 3 / 0) antes de
    darlo por bueno**, que es el mismo contraste con que S127 verificó el criterio.
  - **LOS 32 PX QUE S126 DEJÓ ABIERTOS TIENEN CAUSA CANDIDATA Y NO SE TOCAN.** `.app__contenido`
    declara `padding: 1rem 0`, 32 px exactos, en el contenedor de la vista que ni S126 ni S127
    miraron por estar midiendo dentro de `horario-grid`. Coincide al píxel con la diferencia entre
    los 748 calculados y los 716 medidos, pero se declara CANDIDATA y no probada: la derivación de
    los 748 no está escrita en ningún documento del repo (nace `D-748-sin-derivacion`). NO se
    reclaman, por R-terminado.

- **Registrado en el cierre de S129, sin cambiar el criterio:**
  - **CRITERIO 4 CUMPLIDO: las seis decisiones de identidad están escritas y aplicadas**, cinco en
    `styles.css` (1 paleta, 2 tipografía, 3 densidad, 5 estados, 6 tablas y rejilla) y la 4 rotulada en
    `app.css`, donde ya vivía sin rótulo. Vive allí y no aquí por una razón y no por descuido: la
    restricción de altura que la gobierna sólo tiene sentido junto al elemento que la sufre, y el índice de
    `styles.css` remite a ese punto. Con el rótulo, el criterio 4 se verifica por grep igual que el 1.
  - **«LAS TRES VISTAS» DEL CRITERIO 3 ES UN RECUENTO CADUCADO DESDE S123.** Cuando se escribió,
    Configuración era una pantalla; hoy son ocho destinos con URL propia, más la landing y la vista de
    horario con sus tres modos. Los tres diálogos siguen siendo tres. NO se reinterpreta el criterio: se
    registra que el número está obsoleto y se lee la cláusula por lo que pide —cobertura transversal—, que
    es lo que el criterio 1 ya verifica por grep sobre las hojas enteras. Decisión del arquitecto en el M0.
  - **EL ESPACIADO ES MÁS FÁCIL DE LO QUE S128 SUPUSO, y conviene que conste.** El argumento con que S128
    se negó a abrir un Cambio de tokenización fue que el color tenía equivalencia exacta y el espaciado no.
    Censado en S129: **171 de 195 valores (88 %) caen EXACTOS en un escalón**, y sólo 24 exigen redondeo
    declarado. **NO se reabre la decisión de sede**, que sigue siendo correcta —tokenizar sigue obligando a
    elegir en el 12 % y la exclusión de `horario-grid.css` sigue en pie—; lo que baja es el riesgo del
    tramo 2. Y un riesgo se retira solo: `app.css` ya está limpia de literales de espaciado, así que ese
    trabajo NO toca la barra y la restricción heredada de S128 no gobierna el tramo 2.
  - **LA JERARQUÍA DE ACCIONES ESTABA APLICADA Y NO ESCRITA, y se declara dentro de la decisión 1.** El
    arquitecto preguntó si el verde debía pasar a ser el color de guardar; se argumentó en contra —el verde
    significa VERIFICADO, extenderlo dejaría la decisión 5 sin color para «hecho y correcto» y obligaría a
    repintar ocho formularios y tres diálogos— y lo zanjó la evidencia: la jerarquía ya se resuelve **por
    relleno y no por color**. Principal en acento pleno, secundaria en superficie con borde, destructiva
    con tinta de error, en curso en acento apagado. El verde se queda en verificado, que hoy cubre dos usos
    y un solo trabajo: el resultado sano de la prevalidación y el destino válido de un arrastre, que S121
    declaró veredicto.
    **CORREGIDO EN EL M4 DE S130: el hecho que zanjaba la pregunta NO ERA CIERTO.** Medido sobre el
    código: en toda la aplicación hay un solo `background: var(--color-acento)` y es la barra; ningún
    botón lleva relleno de acento; `__cancelar` y `__guardar` comparten UNA sola regla en los siete
    formularios, así que principal y secundaria son indistinguibles; y los siete `__borrar` de fila
    llevan sólo `color`, sin superficie ni borde. El argumento contra el verde sigue en pie —no se
    reabre—, pero la evidencia que lo zanjaba no existía: la jerarquía estaba DECLARADA y no aplicada.
    Nace `D-jerarquia-declarada-sin-aplicar`, y aplicarla es trabajo del tramo 3, no diseño nuevo.
  - **CUATRO TOKENS DE `styles.css` NO LOS USA NADIE** (`--radio-s`, `--fuente-datos`, `--color-ok-fondo`,
    `--color-info-fondo`). Se marcan como sin uso y NO se retiran: borrar es un cambio sin criterio detrás.
    Nace `D-tokens-sin-uso`, con sede en el tramo 3. El de `--radio-s` cierra de paso una pregunta del
    censo: las diez reglas `__input` llevan `border-radius: 4px` LITERAL, que no es ninguno de los dos
    tokens de radio, y por eso `.cabecera-lista__busqueda` es hoy el único control con las esquinas
    distintas a los otros diez.
    **ACTUALIZADO EN S130:** `--radio-s` pasa a 4 px y estrena 21 usos, así que la deuda baja a TRES
    tokens. Y la pregunta que este censo creía cerrar estaba mal medida: los `border-radius: 4px`
    literales no eran las diez reglas `__input` sino 36 sitios, doce de ellos contenedores.
  - **EL BUNDLE REGISTRADO ERA INCORRECTO, y es la familia de `D-748-sin-derivacion`.** El plan anotaba
    542,03 kB; medido sobre HEAD limpio en `94b97df` son **539,86 kB**, con 10,14 kB de margen hasta el
    aviso de 550. La hipótesis de que la cifra vieja se tomó en un punto intermedio del bloque de S128 es
    razonable y NO está probada; no se reconstruye. Lección: un número de bundle se anota con el commit
    sobre el que se midió.

- **Registrado en el cierre de S132, primera sesión de Acabado visual (M-visual), sin cambiar el criterio:**
  - **EL TRAMO 3 QUEDA APLICADO Y SIN JUZGAR, y por eso O-diseño NO CIERRA.** El acta de S132 se agotó entera
    —sus cuatro puntos son los cuatro que el tramo 3 nombraba— pero el criterio 3 pide DOS cosas y sólo una
    está hecha: el sistema está APLICADO y falta la verificación en navegador SOBRE LO APLICADO. El único
    recorrido que hay en el registro es el de S130, que juzgó el estado ANTERIOR a estos cuatro commits y que
    es precisamente quien los generó. Cerrar aquí sería dar el criterio por cumplido por agotamiento de lista
    y no por juicio, que es el mismo argumento con que S129 partió C-revisión en tres tramos: no se puede
    juzgar el acabado de algo que aún no se ha aplicado, y el veredicto del juez genera trabajo. Lo que queda
    vivo del objetivo es UNA sola cosa: la pasada del arquitecto por las tres vistas y los tres diálogos sobre
    el estado actual. Criterios 1, 2 y 4 siguen CUMPLIDOS; el 3 tiene la aplicación completa y el juicio
    pendiente.
  - **LA JERARQUÍA DE ACCIONES YA ESTÁ APLICADA, y con ella `D-jerarquia-declarada-sin-aplicar` CIERRA.**
    Reglas globales sobre `button` en la sección «Jerarquía de acciones» de `styles.css`, y 32 bloques
    retirados de 20 hojas de componente: 14 `.accion-principal`, 9 `.accion-destructiva`, secundaria por
    defecto y `:disabled` como acento apagado. Se retiran además los tres `font-weight` que decían jerarquía
    por peso, contra la decisión 2. Variante elegida por el arquitecto: la que añade un nivel de TAMAÑO,
    `.accion-compacta`, en las 16 acciones de fila —41 px hoy, 43 con compacta, 53 sin ella—, porque la regla
    global engorda el botón de fila y ahí el alto es presupuesto.
  - **LOS CONTROLES NATIVOS QUEDAN NEUTRALIZADOS, y la premisa del acta era incorrecta.** El acta pedía
    redibujar el adorno de radios y casillas; se resolvió con `accent-color`, que hace el trabajo sin dibujar
    nada. 13 `<select>` con `appearance: none` y galón en SVG —no `::after`, que un elemento reemplazado no
    admite— y 5 `input[type=number]` sin flecha doble. Las diez reglas `__input` no se tocan. Consecuencia
    menor y declarada: un literal de color fuera de paleta (`#5A6673`) dentro del SVG del galón, anotado en la
    decisión 1; vive en `styles.css`, así que el criterio 1 —que prohíbe literales en el CSS DE COMPONENTE—
    sigue cumplido.
  - **`--fuente-datos` SE RESUELVE POR EL LADO DE USARLA, y no de paso.** Gobierna con la clase `.dato` en 9
    sitios —la columna Código de las siete listas y las dos horas de jornada— y excluye la rejilla. Va como
    CAMBIO de la decisión 2 de `styles.css` y no como retoque, porque la ficha de `D-tokens-sin-uso` declaraba
    el binario «o donde toca o se retira» y aquí se rompe con criterio medido. `--color-ok-fondo` sale de la
    cola: no tiene trabajo porque la decisión 5 se lo dio a la tinta, y eso es decisión escrita y no «sin uso
    hoy». `--color-info-fondo` queda como ÚNICO token vivo de la deuda: nombra un trabajo que no existe, y su
    retirada es una línea que se paga cuando algo vuelva a tocar `:root`. Conservarlo lo decidió Claude Code
    dentro del bucle y NO estaba escrito ni era de gusto; se ratifica en el cierre, y el hueco de método que
    destapa queda anotado en `M-visual`.
  - **EL PADDING LATERAL SE DA, Y TIENE UN COSTE MEDIDO QUE NO ESTABA PREVISTO.** `.app__contenido` pasa de
    `var(--e4) 0` a `var(--e4) var(--e5)`, sin variante, porque la decisión 3 asigna `--e5` al padding de
    contenedores; queda asimétrico A PROPÓSITO —el vertical sigue congelado por los 32 px de S128— y el porqué
    se escribe en `app.css` con puntero desde la decisión 3. El coste: el umbral de corte lateral de la
    rejilla se mueve de 1200 a 1280 px, 80 px de margen que se pierden. No rompe nada declarado (1280 está
    excluido del criterio 4 de O-navegación desde S123) y nace `D-corte-lateral-a-1280`.
  - **EL CENSO DE BOTONES DE S130 ERA CORTO, y se corrige aquí porque es afirmación viva.** Medido sobre
    `82ec04c`: **54 `<button>` en 24 plantillas**, no «34 botones en 10 componentes»; caja y radio 10
    (correcto), una sola propiedad **10** y no 9, y **15** clases sin regla y no 8 —faltaban `cancelar` de
    confirmar-generación, `grupos__pdc`, `grupos__tutoria`, `generar`, `confirmar-reemplazo__cancelar`,
    `jornada__recargar` y el botón sin clase de `panel-prevalidacion`—. Lo ya archivado en la bitácora NO se
    corrige (M2): la corrección vive aquí y en la ficha de la deuda.


- **Registrado en el cierre de S133, que es el que CIERRA el objetivo:**
  - **CRITERIO 3 CUMPLIDO: el recorrido de juicio no encontró nada que arreglar.** El arquitecto recorrió la
    aplicación levantada sobre el centro real en seis paradas, a 1920x1080, sobre el bundle de `4ba1fd4` y una
    copia de `educhronos-demo-m4.db`. Las cuatro paradas de producto salieron limpias, incluidas las dos cosas
    que S132 aplicó y nadie había juzgado todavía: la jerarquía de acciones por relleno y la neutralización de
    los controles nativos. La mitad de suites del criterio no se repitió porque entre el último commit de
    producto de S132 y el juzgado no hay un solo fichero de `app/` ni de `solver/`.
  - **EL ACTA DE LA SIGUIENTE SESIÓN DE ACABADO SALE VACÍA, y esa es la salida legítima del recorrido.** Su
    propósito era producir la lista de lo que falla; no hay lista, luego no hay sesión. Se registra porque el
    caso contrario —el de S130, cuyo recorrido partió el tramo 3 por segunda vez— está registrado, y el
    mecanismo sólo es honesto si se anota también cuando devuelve cero.
  - **LAS DOS OBSERVACIONES DEL ARQUITECTO CAEN FUERA DEL CRITERIO, y las dos con medición detrás.** Ver el
    tutor en la tabla de grupos exige ampliar el contrato, porque el dato no viaja al cliente
    (`D-tutor-invisible-en-grupos`, mejora futura de O-demo); ver las plazas ocultas sin arrastrar cambia una
    decisión de interacción escrita en S127 sobre la superficie donde el alto es presupuesto
    (`D-plazas-ocultas-solo-al-arrastrar`, candidata a O-particiones). Ninguna es aspecto: R-terminado.
  - **LA RESERVA DEL SELECTOR DE CURSO ESTABA BIEN CONSTRUIDA Y MAL DOCUMENTADA.** `app__curso` existe, vacío a
    propósito, y reserva ALTO y no ancho; D18 de `diseno-navegacion.md` afirmaba un rótulo que no existe y una
    reserva de ancho que tampoco. Corregido allí. Lo que importa conservar: la barra NO crecerá cuando llegue
    el selector, luego el presupuesto de la rejilla está a salvo; lo que no está reservado es el ancho.

- **Grano abierto:** objetivo propio y separado, NO colgado de O-demo, porque el
  diseño transversal toca todas las vistas a la vez y no es "parte de" ningún hito
  funcional. Si al abrirlo resulta grande, se parte (métrica de §7).


#### O-navegación — "La aplicación se maneja como una aplicación de escritorio." ✔ TERMINADO (S127)
- **Propósito:** que la aplicación se recorra sin pelearse con ella —enrutado, densidad
  de la rejilla y listas del tamaño del centro real—, no que se vea mejor. Es transversal,
  como O-diseño, y hermano suyo: uno hace el ACABADO y el otro el MANEJO. Lo funda
  `docs/diseno-navegacion.md`, la medición de S122 sobre el horario del centro real y sus
  28 grupos.
- **Terminado cuando:**
  1. Barra superior con Configuración y Horario, entrada activa distinguible, y sitio
     reservado para el selector de curso activo de Fase 10 sin rehacerla. (Nota: la barra
     YA EXISTE; lo que falta es estilo, y eso es C-identidad.)
     **CERRADO en S127 sobre esa decisión escrita, con la salvedad registrada:** este criterio
     nunca abrió Cambio y nunca se verificó formalmente. La barra existe y en las capturas del M4
     la entrada activa se distingue, pero eso es lectura de unas capturas tomadas para otra cosa,
     no una verificación. **C-identidad reestila esta barra**, así que es allí donde «entrada
     activa distinguible» y el sitio reservado para el selector de curso de Fase 10 se comprueban
     de verdad. Se deja dicho para que nadie lo lea como verificado.
     **SALVEDAD CONSUMIDA EN S128, dentro de C-identidad, y con las DOS mitades medidas y no leídas.**
     (a) *Entrada activa distinguible*: la barra reestilada la distingue por CUATRO canales —color pleno,
     peso, subrayado y `aria-current="page"`—, y el cuarto es el que faltaba: hasta S128 la distinción era
     sólo de CSS, así que para un lector de pantalla las dos entradas eran idénticas. Medido en navegador:
     `page` en la activa y `null` en la otra. (b) *Sitio reservado para el selector de curso de Fase 10*:
     hasta S128 eran 1.500 px de barra vacía y nada reservado. Ahora existe `.app__curso`, con
     `margin-left: auto` y un `min-height` igual a la caja de la marca. Lo que se reserva es una ALTURA
     CONOCIDA y no una promesa de que todo quepa: un control de 27 px o menos entra sin mover la barra; uno
     mayor la engorda, y entonces Fase 10 recuenta las marcas `+N` antes de darlo por bueno (ver la
     restricción escrita en la ficha de O-diseño). **El criterio 1 deja de llevar salvedad.**
  2. Configuración se navega por rutas hijas con `router-outlet`: los ocho destinos se
     alcanzan en un gesto, cada uno con URL enlazable. `/configuracion` redirige a
     `jornada`. Añadir un noveno destino es una entrada nueva, no una reforma.
     `loadComponent` queda APLAZADO, no descartado: se añade si se mide que hace falta.
     **CUMPLIDO en S123** por C-rutas-hijas, con el índice derivado de `routeConfig.children` y un test de
     mutación que lo vigila. `loadComponent` sigue aplazado: no se midió, y no se promete lo que no se mide.
  3. Con la base del centro real, ninguna lista obliga a recorrer el scroll hasta el
     final. Lo medido en S122 fue FILTRO más lista desplazable dentro del destino, no un
     paginador; el criterio admite cualquiera de los dos, pero el que tiene evidencia es
     el primero. Casos duros: Actividades (208) y Subgrupos (334). Frontera con
     `D-selectores-sin-busqueda`: esa deuda habla de los `<select multiple>` de los
     FORMULARIOS; el filtro de este criterio actúa sobre las LISTAS de un destino. Son
     sitios distintos y no se saldan la una con la otra.
     **CUMPLIDO en S124** por C-listas-filtradas, con búsqueda por subcadena normalizada (tildes,
     caja, ordinales y todo lo que no sea letra o dígito), casado por partes y contador «n de N». **S185:** el ordinal escrito en la consulta ya discrimina («1ºB» no encuentra «1B-A»; `7c9f6b0`, `D-filtro-por-codigo`).
     Verificado en navegador sobre los dos casos duros con la base del centro real: en Subgrupos,
     `3ºA` deja 17 de 334 y `3ºA Di` deja 5. Sin paginador, sin `debounce` —medido que las siete
     listas ya montan todas sus filas— y sin longitud mínima de consulta, que se descartó porque
     no reaccionar a la primera letra se lee como roto.
  4. El horario de un grupo cabe sin scroll vertical en el VIEWPORT DE VERIFICACIÓN,
     con el mecanismo de expansión activo. Las celdas que no caben se muestran colapsadas
     Y con forma visible de expandirlas: recortar sin expansión es perder una clase, no una
     explicación. El criterio se verifica sobre VIEWPORT CSS, no sobre especificación de
     panel: escribir «1920×1080» a secas fue el error que S123 descubrió, y **«~1920×945»
     se usaba mal**: S125 midió que ese número se manejaba con el cromo a CERO, es decir
     suponiendo barra de aplicación y cabecera de vista de altura nula, cuando la barra mide
     52 px medidos. El número no era falso —Chrome da 946 de viewport, clavado— pero se
     comparaba un VIEWPORT contra un presupuesto que exige CONTENIDO NETO.
     **SUPERFICIE DE VERIFICACIÓN (S125): el equipo de desarrollo, medido en viewport CSS
     1920×887 con `devicePixelRatio` 1, Firefox maximizado (no F11) con barra de marcadores
     visible, del que la barra de la aplicación descuenta 52 px y deja 835 de contenido.**
     NO se mide el sobremesa del centro: no hay uno solo, la aplicación correrá en varias
     máquinas y D11 absorbe la variación —un viewport menor colapsa más celdas, no rompe
     nada—. Se nombra Firefox por ser el PEOR CASO de los dos navegadores medidos (149 px de
     cromo frente a los 90 de Chrome, que da viewport 946 y contenido 894) —y los dos NO dan
     el mismo veredicto: en el eje común, el presupuesto para barra más cabecera de vista es
     33,4 px en Firefox y 92,4 en Chrome, así que con la barra en 52 px **Firefox se queda
     18,6 px corto aun con cabecera de altura cero y da 50 recortes, mientras Chrome mantiene
     las 22 si la cabecera de D9 cabe en 40,4 px**. Verificar en el peor caso es lo que
     convierte el número en un SUELO y lo que permite prescindir de medir las máquinas del
     centro; fijarlo en Chrome haría el criterio dependiente del navegador—.
     RECORTE MEDIDO sobre esa superficie: **50 de 791, el 6,3 %** —las 22 celdas de seis
     plazas y las 28 de cinco—. CUMPLE: lo que el criterio exige es que quepa sin scroll con
     expansión activa; el «22 de 791 / 2,8 %» era descripción de lo medido en S122, no un
     tope. Umbral de reapertura, heredado del argumento de S123 (a 585 px eran 109, el
     13,8 %, «al 14 % el colapso deja de ser el caso excepcional»): si el recorte supera
     el ~10 %, el criterio se rediscute.
     RESTRICCIÓN DE DISEÑO DERIVADA Y MEDIDA, que hereda C-rejilla-densidad: **la fila única
     de D9 (título + controles) con su padding debe caber en 111 px.** El escalón de las
     celdas de cuatro plazas cae en cromo de vista 111,6; por encima entran 22 celdas más y
     el recorte salta a 72 de 791, el 9,1 %.
     **VERIFICADO EN S126 (tramo 1), y el resultado es 72, no 50.** Lo que el criterio exige SÍ se cumple:
     el scroll vertical desaparece, medido en 1ºA, 4ºA —que desbordaba 71 px— y 1B-A. La cabecera de D9
     quedó en 27 px, muy por debajo del techo de 111, pero el hueco real que el flex deja a la rejilla es
     **716 px y no los 748 calculados**, y esos 32 px de diferencia SIGUEN SIN EXPLICAR. El reparto cae a
     110 px de fila contra los 110,8 que pide una celda de cuatro plazas: el acantilado se cruza por 0,8 px
     y entran las 22 celdas de cuatro plazas. **72 de 791 = 9,1 %, por debajo del umbral de reapertura del
     ~10 %, así que el criterio NO se rediscute**; el «50 de 791» era la predicción de S125, no un requisito.
     Y no se arregla afinando: el aviso de pines cuesta 62 px de hueco (~10 px por fila), así que con un
     aviso en pantalla 72 es inevitable a esta geometría. El instrumento sale VALIDADO del contraste con el
     navegador —cinco tamaños de celda, ninguna desviación mayor de 1 px— y no se toca.
     **CUMPLIDO EN S127 (tramo 2), y con ello el criterio ENTERO.** La otra mitad —«colapsadas Y
     con forma visible de expandirlas»— la cierra la marca `+N` en la banda del rótulo con el
     detalle en el `title` (`diseno-navegacion.md` §4, «Decisiones de S127»). La marca NO se
     dispara por número de plazas sino por desbordamiento medido con una FRACCIÓN —una plaza
     cuenta como oculta si se ve menos de la mitad—, así que **se marcan 50 de las 72 recortadas**:
     las 22 de cuatro plazas se pasan 1,23 px y no esconden nada legible, y marcarlas habría sido
     poner una señal falsa al lado de la marca verdadera de D6. Verificado en la superficie del
     criterio (Firefox del arquitecto, 1920×887, dpr 1, hueco 716, `--alto-celda` 101): 1B-A 4
     marcas, 4ºA 3 —dos `+2` y un `+1`— con sus tres celdas de cuatro plazas SIN marcar en la misma
     pantalla, y 2B-B 0 sobre seis celdas de cuatro plazas. Las marcas siguen al grupo al cambiar
     de vista y volver. **El 9,1 % de recorte no se toca y sigue sin rediscutirse**: lo que cambia
     no es cuántas celdas se recortan, sino que ya no lo hacen en silencio.
     Confirmación colateral que vale por sí sola: con el aviso «1 pines sin aplicar» en pantalla la
     celda de seis pasa a `+3` y **el scroll no reaparece** —los 62 px que S126 midió, absorbidos en
     caliente—. Es la prueba de que el reparto en runtime de D1 compró algo real.
     El hueco de 716 px queda además CONFIRMADO en el navegador del arquitecto y no sólo en el de
     Playwright; los 32 px contra los 748 calculados siguen sin explicar, pero ya no son sospechosos
     de ser artefacto del entorno de prueba.
     **PRECISIÓN AÑADIDA EN S132, que NO reabre el criterio:** lo que este criterio pide es que quepa sin
     scroll VERTICAL, y todo su aparato de verificación —el reparto de altura y el recuento de marcas `+N`—
     mide exactamente eso. El ANCHO no lo cubre ni este criterio ni ningún instrumento del proyecto, cosa que
     nadie había escrito: S132 movió el umbral de corte lateral de la rejilla de 1200 a 1280 px y lo detectó
     DESPUÉS de cerrar el punto, por medir con el recuento de marcas, que da el mismo número mientras el texto
     se pierde de lado. Nacen `D-instrumento-criterio4-ciego-al-ancho` y `D-corte-lateral-a-1280`. El criterio
     sigue CUMPLIDO, remedido sobre el estado final de S132 a 1920×887: 1ºA 2, 1B-A 4, 4ºA 3 y 2B-B 0 marcas,
     idéntico en los cuatro commits y coincidente con lo que S127 cerró.
     La resolución del portátil queda MEDIDA y CERRADA en S123 —1280×585 con escala 150 %— y
     EXCLUIDA del criterio.
  5. Ninguna escritura nueva. Se admite composición de solo lectura (enseñar en un destino
     lo que cuelga de él, con enlace). Crear o editar desde un sitio que hoy no lo hace
     queda fuera: eso es O-particiones.
  6. Sin regresión: suites verdes salvo las que el cambio de plantillas obligue a tocar,
     declarado ANTES y no descubierto en rojo. Previstos y confirmados: los 8 de `configuracion.spec.ts`,
     que MUEREN y se sustituyen por 6 de enrutado, y `centro-minimo.spec.ts:107-180`, que es reescritura
     de media suite y no «dos puntos» —el otro punto, `:213`, es de C-rejilla-densidad por D6—. Suites
     tras S124: app 282, solver 91, vitest 356, e2e 2. **Tras S126: app 282, solver 91, vitest 381, e2e 2**,
     con la única baja declarada del tramo 1 —`centro-minimo.spec.ts`, el aserto sobre `.grupos`— sustituida
     por su contraria, que es lo que D6 promete y antes no verificaba nadie.
     **CUMPLIDO. Tras S127: app 282, solver 91, vitest 403, e2e 2.** vitest sube +22 y **ni uno solo de
     los 381 heredados se modifica**: el tramo 2 sólo añade. Las bajas previstas del objetivo se
     consumaron todas en S123 y S126; el tramo 2 no rompió ninguna.
- **Deudas que absorbe:** `D-configuracion-monolitica` y `D-pdc-lista-rancia`, que
  llevaban desde S115 y S113 remitiendo las dos, con esas palabras, a «el Cambio que
  decida la navegación» y a «la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio
  propio». **Esa decisión se toma aquí y es el criterio 2.** `D-pdc-lista-rancia` no se
  paga con un parche: al cargar cada destino al entrar deja de existir, que es justo lo que
  su ficha pedía («no se parchea con un `EventEmitter` ad hoc, que fijaría el molde por la
  puerta de atrás»). La deuda aplazada de S101 se cierra al cumplirse el criterio 2. Cierra
  además el hilo de **C-configuracion-navegable**, el Cambio de O-demo que S115 RETIRÓ del
  camino crítico por medición y derivó precisamente a `D-configuracion-monolitica`: lo que allí
  se aplazó por no ser urgente para la demo reaparece aquí como criterio de un objetivo propio,
  que es donde debía estar.
- **Fuera del criterio, por R-terminado:** `D-selectores-sin-busqueda` (habla de otro
  sitio: ver la frontera escrita en el criterio 3), `D-actividad-forma-implicita` y
  `D-actividad-ux` (son el formulario de actividad, no la navegación),
  `D-vista-horario-sin-horario` (es comportamiento y sigue en O-demo),
  `D-insignia-sin-leyenda` (es C-identidad) y el responsive (`D-sin-puntos-de-ruptura`,
  decisión escrita de S121). Fuera también tokenizar el espaciado: es la decisión 3 de
  identidad de O-diseño y sigue sin sede.
- **Cambios que agrupa — PROPUESTOS en S122, RATIFICADOS los tres en S123:** **C-rutas-hijas**
  (criterio 2, y con él las dos deudas absorbidas) — **HECHO en S123**; **C-listas-filtradas**
  (criterio 3, los casos duros son Subgrupos y Actividades) — **HECHO en S124**; y **C-rejilla-densidad**
  (criterio 4, la geometría de celda de `diseno-navegacion.md` §4 más el mecanismo de expansión de D11)
  — PENDIENTE, y ya sin parámetros abiertos tras cerrarse D0-2. **PARTIDO EN DOS TRAMOS en S125**, por dependencia real y no por tamaño: **tramo 1 — geometría y presupuesto** (D1-D6, D7, D8, D9, D10: todo lo que CONSUME altura, con la verificación en navegador de que da 50 de 791) y **tramo 2 — el mecanismo de expansión** (D11, lo único interactivo y lo único que hoy no existe en ninguna forma; va después porque no se puede verificar «colapsada Y con forma visible de expandirla» hasta que algo colapse). **TRAMO 1 HECHO en S126** (seis commits: geometría de celda, cabecera fundida, fila de recreo y reparto de altura), con el criterio 4 cumplido en su mitad medible —ausencia de scroll— y PENDIENTE en la otra: la expansión. La CONDICIÓN DE SALIDA que heredó el tramo 2 —el repositorio ocultando 72 celdas de 791 sin marca ni forma de verlas— queda **DESCARGADA en S127**. **TRAMO 2 HECHO en S127** (tres commits: la regla pura, el adaptador DOM→regla y el cableado con la marca), y con él el criterio 4 cumplido entero. La partición de S125 sale VALIDADA: la dependencia era real —no se puede verificar «colapsada y con forma visible de expandirla» hasta que algo colapse— y el tramo 2 se apoyó en la geometría del tramo 1 en cada medida, incluida la que decidió el umbral. El criterio 1 no abre Cambio: la barra
  existe y lo que le falta es estilo. Salen uno a uno de los criterios 2, 3 y 4, y tenerlos escritos
  convirtió el M0 de S123 en ratificar en vez de deliberar.
  **RENOMBRADO en S123: `C-listas-filtradas` se llamaba `C-listas-paginadas`.** Los dos nombres designan
  el mismo Cambio; el viejo sobrevive en el cuerpo de S122 del plan, que no se reescribe. El motivo es
  que el criterio 3 admite paginador o filtro pero solo el filtro tiene evidencia medida, y D16 no diseña
  paginador alguno: un nombre que apunta a la opción sin evidencia acaba construyéndola.
  FRONTERA entre los dos primeros, cortada en S123: la cabecera fija y el scroll dentro del destino son
  consecuencia estructural de tener destino propio y fueron a C-rutas-hijas, con el hueco del filtro
  montado y vacío; el filtro y el componente de cabecera compartido van íntegros a C-listas-filtradas.
  **CORREGIDO en S124: los «contadores en el índice» se CAEN de esa lista.** No se declaran fuera de
  alcance ni pasan a deuda: la frase no tenía respaldo. `diseno-navegacion.md` no los diseña —ni en §4,
  donde D14 genera el índice de una lista de destinos, ni en §5, que enumera lo que quedó sin decidir— y
  el M2 de S124 midió por qué no deben existir: `Configuracion` es presentacional pura y sin servicios de
  dominio (`configuracion.ts:85-87`, invariante declarada en su javadoc y protegida por su propio spec), y
  ocho contadores le obligarían a consultar los ocho servicios al entrar, deshaciendo las ocho cargas que
  C-rutas-hijas acababa de separar. No es una extensión del índice: es cambiarle el rol. Aviso medido para ese Cambio: «Grupos» casa dos entradas del índice por
  subcadena, al estar contenido en «Subgrupos».
- **No cuelga de ningún hito funcional.** Igual que O-diseño, **acerca la demo**: es manejo,
  no función nueva, y ninguna de las dos cosas que hace —enrutar y acotar altura— añade
  capacidad al producto. No depende de O-particiones ni del cierre formal de H2. Sí conviene
  que vaya ANTES de C-identidad y C-revisión, porque esos dos juzgan el aspecto de la UI
  definitiva y esta la cambia.
- **Orden resultante, que sustituye al de la ficha de O-diseño:** sistema (hecho) →
  **O-navegación** → C-identidad y C-revisión sobre la UI definitiva → demo.
- **TERMINADO en S127.** Los seis criterios cumplidos: el 2 en S123, el 3 en S124, el 4 en S126 (sin
  scroll) y S127 (colapso con marca), el 5 sin una sola escritura nueva —D11 no añade ninguna—, el 6 con
  403 verdes y ninguna baja imprevista, y el 1 sobre la decisión escrita de la ficha, con la salvedad
  anotada en su propio texto. **Siguiente por el orden escrito desde S122: C-identidad**, de O-diseño,
  sobre la UI definitiva que este objetivo acaba de fijar.
- **Grano abierto:** objetivo propio y NO Cambio de O-diseño, por decisión del arquitecto
  en S122, con razón escrita: esto toca enrutado, paginación y densidad de rejilla, arrastra
  la decisión aplazada desde S101 y rompe un e2e de criterio. **Un Cambio que rompe un e2e
  de criterio no es un Cambio.** Si al abrirlo resulta grande, se parte (métrica de §7).

Fue el motor de las sesiones S84–S99 y no produce avance de producto. Su trabajo
legítimo (que el ajuste funcione) está en O-ajuste-cierre; su trabajo ilegítimo
(pulir tests de una vista que el shell reubicará) desaparece por la regla de
terminado (§6).

### H3 — Exportar

#### O-exportación — "El horario sale de la aplicación, en papel y en datos." ✔ TERMINADO (S150)
- **Propósito:** que el usuario se lleve el horario en las dos formas que usa el centro:
  impreso por grupo, por profesor y por aula, y como datos para otras herramientas. Es el
  paso 5 del guion de §1. Abierto en S147 como único objetivo previsto de H3.
- **Terminado cuando** — ESCRITO en S147 sobre medición (M2 de solo lectura sobre el
  horario de referencia y sobre los PDF del centro), con la lección de S142: un criterio
  escrito antes de medir puede ser inmedible. SUSTITUYE a los cuatro criterios de Fase 9,
  que no se marcan (nota en la Fase 9 del plan). Todo se mide sobre COPIA de
  `educhronos-s137.db` (`dfa4c0774a842d8eb6b7a941e23df2a9`, 819 sesiones).
  O-exportación termina cuando:
  1. Desde la vista del horario, un PDF por vista (grupo, profesor, aula) con una página
     A4 por recurso: 28 grupos, 59 profesores y las 44 aulas del CATÁLOGO, vacías
     incluidas —el centro imprime así sus 43 páginas de aula, 10 de ellas vacías—. Cada
     página lleva la columna de horas y el recreo de la jornada que imprime el centro
     (8:00–14:30, recreo 11:00–11:30), comprobada contra ese PDF y NO contra la base
     (`D-hora-tramo-dependiente-de-zona`), y una leyenda con el nombre de catálogo de cada
     profesor y asignatura que aparece en la página. **PRECISADA en S149, escrito ANTES de ejecutar** (precedente de la condición 4 en S148): la página lleva además la línea de TUTOR y la CLAVE DE LECTURA «Asignatura - Profesor - Aula». Las dos salieron de la revisión humana que la condición 3 exige, y sin la clave la celda es ambigua porque nuestro texto de entrada no lleva separadores. **AVISO que la precisión arrastra:** de los 28 tutores, los 7 de Bachillerato son FALSOS y faltan 5 co-tutorías de ORI1 (medido en S136). El papel imprime lo que el sistema contiene; el defecto es de datos, vive en `O-demo` y su salvedad está en `docs/salvedades-demo.md`. **CUMPLIDA en S150** (`C-exportacion-pdf-profesor-aula`): 28, 59 y 44 páginas; las 9 aulas del catálogo sin clase (A19 TUTOR, B08, B09, B10, B11 Taller Tecnología, C02, C03, C04, Taller 2) con página y sin leyenda —el centro imprimía 43 y 10 porque su catálogo no tenía Taller 5—; los seis tramos y el recreo 11:00-11:30 en las 131 páginas; la leyenda verificada contra la base por `scripts/verificar-leyenda-pdf.py` (28/59/44 páginas sin fallo, validado con tres mutaciones: nombre de profesor, nombre de asignatura y exclusividad); tutor en 28 páginas de grupo y «Tutor de:» en 23 de profesor, 5 con dos grupos. **PRECISADA en S150, escrito ANTES de ejecutar:** en la vista de profesor las celdas no llevan códigos de profesor y el único que aparece es el titular, cuyo nombre de catálogo va en el TÍTULO (`COD — Nombre`); la leyenda lleva sólo asignaturas. Claves de lectura: «Asignatura - Aula - Grupo» (profesor) y «Asignatura - Profesor - Grupo» (aula), las del centro. Una página de aula vacía no lleva leyenda porque no aparece nada que nombrar, como en el centro.
  2. Completo y exclusivo: el texto de cada página contiene exactamente las entradas de su
     recurso según un oráculo SQL sobre la copia, independiente del exportador y de la
     proyección. Totales esperados, medidos en S147: 1285 entradas en 840 celdas de grupo,
     835 celdas de profesor y 819 de aula. En papel no hay «+N». **CUMPLIDA en S150**: oráculo por vista (`--vista`) sobre los ficheros descargados por el navegador: 1285/0/0, 835/0/0 y 819/0/0, y 0 páginas vacías con leyenda, contador propio desde S150. El oráculo se mutó antes de creerle: página de aula quitada, entrada quitada, código partido por su guion y página vacía con leyenda.
  3. Legible: ningún texto por debajo de 7,99 pt, el cuerpo único de los PDF del centro en
     sus 71 páginas (medido en S147). Y revisión humana de los dos peores casos medidos:
     1ºA (celdas de 6 entradas, el máximo del horario: martes T2 y jueves T5) y DIB2 (la
     entrada más larga, 47 caracteres). **CORRECCIÓN DE INSTRUMENTO (S149):** `pdftohtml` NO sirve para esta condición. Redondea a 8.00 lo que el flujo de contenido dice que es 7,99, luego también daría por buena una página a 7,6. El umbral se verifica leyendo los operadores `Tf` del flujo descomprimido. Medido en el documento de 28 páginas: `{7.99: 2612, 10.0: 28}` y nada más, con un 10,0 por página que son los títulos de grupo. **MITAD HUMANA, en su parte de grupo: HECHA.** El usuario revisó 1ºA y 4ºA y de esa revisión salieron cinco correcciones. DIB2 es vista de profesor y queda para su Cambio. **CUMPLIDA en S150**: operadores `Tf` `{7.99: 2612, 10.0: 28}`, `{7.99: 1902, 10.0: 59}` y `{7.99: 2055, 10.0: 44}`, un 10,0 por página. Revisión humana completa: 1ºA y 4ºA en S149, DIB2 en S150. Su entrada de 47 caracteres ocupa 3 líneas partidas SIN romper un código: OpenPDF partía por el guion (`1B-` / `C`), y un `SplitCharacter` que sólo parte tras espacio y `/` lo evita sin mover un byte de la vista de grupo (medido sobre sus 311 entradas).
  4. CSV: descargable DESDE LA VISTA DEL HORARIO, un proceso que lee SÓLO el CSV
     reconstruye las tres vistas idénticas al oráculo de la condición 2. El grano —fila por
     sesión o por sesión y profesor— se decide en su Cambio. **La exigencia «desde la vista»
     se precisa en el M0 de S148**: la condición 1 la llevaba y la 4 no, pero el paso 5 de §1
     lo ejecuta una persona sobre la interfaz, y un CSV que sólo se obtenga con `curl` no pasa
     ese guion. **CUMPLIDA en S148** (`C-exportacion-csv`): grano de fila por sesión, 819
     filas, `GET /api/horarios/{id}/csv` y enlace de descarga en la vista;
     `scripts/oraculo-exportacion.py` reconstruye 1285 entradas de grupo, 835 de profesor y
     819 de aula con 0 faltan y 0 sobran en las tres, sobre el fichero descargado por el
     NAVEGADOR, cuyo md5 (`eff9b823c6baca32b1b6b36e1569933d`) coincide con el de `curl`. **Grano ampliado en S170** (`C-exportacion-bloques`): una fila por sesión y tramo cubierto; con actividades de un tramo el fichero no cambia.
  5. Sin dependencia de plataforma comprobable desde Linux: el PDF lleva incrustadas todas
     sus fuentes (`pdffonts`) y el CSV declara su codificación. La prueba en Windows real
     —antes criterio 4 de Fase 9— se ejecuta en H4 con el bundle (precedente: Fase 6 →
     Fase 11). Los PDF del centro NO incrustan su fuente: esta condición es más estricta a
     propósito, porque es nuestro bundle el que ha de funcionar en un Windows limpio. **MEDIA CUMPLIDA en S148:** el CSV declara su codificación por partida doble —BOM UTF-8 y `charset=UTF-8` en la respuesta—, verificado por bytes; falta el PDF y su `pdffonts`. **LA MITAD DEL PDF, CUMPLIDA sobre el documento que existe (S149):** `pdffonts` da `emb yes / sub yes / uni yes` en las dos fuentes (DejaVuSansCondensed y su Bold), cargadas del classpath como `byte[]` y nunca de `/usr/share/fonts`, que es lo que la prueba en Windows de H4 exige. NO se declara cumplida entera hasta que el documento lleve las tres vistas, aunque no se prevé trabajo: el cargador de fuentes es el mismo. Hallazgo que se registra: con `IDENTITY_H` el flag `EMBEDDED` es inoperante —OpenPDF empotra siempre las CID—, así que ningún test puede matar un mutante que lo quite. **CUMPLIDA en S150**: las dos DejaVu `emb yes / sub yes / uni yes` en las tres vistas. La prueba en Windows real sigue siendo de H4.
- **CRITERIO CUMPLIDO en S150: 5 de 5.** Cuatro sesiones (S147–S150) de las ocho de §7 y cuatro Cambios.
- **Depende de:** H1 y H2 cerrados (S146 y S141). No depende de generar: se mide por
  lectura sobre un horario ya guardado, sin gastar 600 s y sin `D-horario-irreversible`.
- **Valor:** es lo que el usuario se lleva impreso; §2 lo califica de ALTO y cobrable.
- **Cambios que agrupa:** `C-alcance-exportacion` ✔ HECHO (S147), el M0 y la medición que
  escribieron este criterio, y `C-exportacion-csv` ✔ HECHO (S148), que cumple la condición 4
  y media 5 y deja construido el ORÁCULO que la condición 2 de los tres PDF reutiliza, y `C-exportacion-pdf-grupo` ✔ HECHO (S149), que construye el motor, la fuente empotrada, la maqueta y el oráculo de PDF, y `C-exportacion-pdf-profesor-aula` ✔ HECHO (S150), que generaliza la maqueta con `VistaPdf` —un enum de métodos abstractos para que una vista no compile incompleta— dejando la de grupo IDÉNTICA byte a byte, añade profesor (59 páginas) y aula (44, con las del catálogo sin clase), el corte de línea que no parte códigos, tres enlaces en la vista y el oráculo por vista. Decisiones tomadas por medición y no por gusto: **OpenPDF 1.3.32** (LGPL-2.1 + MPL-2.0, cierre transitivo vacío por dependencias `<optional>`, Build-Jdk 11 contra `release 17`), **DejaVu Sans Condensed** empotrada con su licencia adjunta, **A4 vertical** —no apaisado, que es lo que imprime el centro—, columna de horas de 58 pt y cinco de día de 96,2. El presupuesto de página se calculó ANTES de dibujar, midiendo anchos reales de las cadenas peores, y la página más apretada del documento (4ºA: leyenda más larga, más entradas y el aula de 22 caracteres) cierra con 97,5 pt de holgura medida. Los dos que quedaban se hicieron en UNO (S150): con una entrada por celda en profesor y aula, el riesgo de altura había desaparecido.
- **Restricciones de diseño heredadas de §4, ninguna bloqueante:** la exportación COMPONE
  `GeneradorHorarioService.proyectar()` con la jornada y el catálogo de profesores —la
  proyección no lleva ni el nombre del profesor ni las horas— y NO escribe un tercer mapeo
  desde `Sesion` (`D-proyeccion-instancia-espejo`) ni se fía de la colección inversa
  `horario.getSesiones()` (`D-post-horario-sin-sesiones`). Dos decisiones quedan para sus
  Cambios: el motor de PDF —el criterio favorece generarlo en el servidor; iText (AGPL)
  descartado por licencia, PDFBox y OpenPDF viables— y el CSV para Excel con configuración
  regional española (separador y BOM), que se verifica en Windows dentro de H4. **MATIZADO en S148:** la composición con la jornada y el catálogo la exige el PDF, no el CSV; `C-exportacion-csv` consume el `HorarioProyeccionDTO` tal cual y no añade acceso a datos. La decisión del CSV para Excel quedó en `;` con BOM UTF-8, CRLF y escape RFC 4180, sin línea `sep=` —hace que Excel ignore el BOM— y sin librería: cero dependencias nuevas.
- **Aviso para el Cambio del PDF:** el precedente del centro no se transfiere solo. El
  centro imprime códigos de aula cortos («A12In») en Courier, unos 15 caracteres por
  columna; los nuestros llegan a 22 («Taller 1 Aula Plástica»). Se maqueta 1ºA ANTES que
  nada: si no cabe en una página, se sabe pronto.
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

### H4 — Instalar y pasar de curso

#### O-instalación — "La aplicación se instala, arranca y se cierra en un Windows limpio sin ayuda técnica." ✔ TERMINADO (S156)
- **Propósito:** que un usuario no técnico lleve la aplicación a su Windows y la use sin
  Java, sin consola y sin permisos de administrador. Es el paso 1 del guion de §1. Abierto
  en S151 como primer objetivo de H4; la duplicación de curso (Fase 10) y la integración
  continua (Fase 12) serán objetivos propios.
- **Por qué va primero dentro de H4:** es el riesgo mayor y el menos medido (el «~10 %» de
  §2 no se sostenía), y decide dónde viven los datos del usuario, de lo que depende la
  Fase 10. **Un objetivo y no dos:** la prueba en Windows limpio verifica a la vez el
  empaquetado y el arranque; separarlos obliga a construir y probar el bundle dos veces.
  El techo de §7 vigila: si pasa de ocho sesiones, se parte.
- **Terminado cuando** — ESCRITO en S151 sobre medición: M2 en Linux (clon limpio y
  app-image con JDK portable), en la máquina Windows de construcción y en un Windows 11
  Home sin Java ni Node. PRECISA los cuatro criterios de Fase 11 y absorbe el criterio 4
  de Fase 9, que no se marcan hasta que se cumpla este. O-instalación termina cuando:
  1. **Construcción reproducible.** ✔ CUMPLIDA (S152). **REESCRITA en S152, y la razón se
     midió antes:** `jpackage` no construye para otra plataforma, y la máquina Windows no
     tiene código, ni Git, ni Maven. El texto original —«un guion… sin ningún paso fuera del
     guion»— describía algo irrealizable sin llevar el repo a Windows, que el usuario
     descarta porque su plataforma de desarrollo es Linux. El jar SÍ es el mismo en las dos.
     Queda así: **dos guiones versionados en el repo, uno por plataforma.** El de Linux
     construye el jar desde `mvn clean` y deja en una carpeta de entrega el jar, su sha256 y
     el guion de Windows. El de Windows, sin más requisitos que PowerShell y esa carpeta,
     verifica el sha256, usa un JDK 17 portable fijado por versión y sha256, y produce el
     app-image y su zip. Ningún paso manual salvo el traspaso de la carpeta entre las dos
     máquinas y la invocación de cada guion. El jar lleva un único `main-*.js`, el que cita
     `index.html`. **Cumplida por `scripts/empaquetar-linux.sh` y
     `scripts/empaquetar-windows.ps1`**, ejecutados de punta a punta en las dos máquinas; el
     guion de Linux ABORTA si el `main-*.js` no es único o no es el citado.
  2. **Tamaño.** La carpeta del app-image de Windows ocupa menos de 250 MB (10^6 bytes),
     medida como suma de los bytes de sus ficheros. Se mide la carpeta y no el zip porque
     es lo que ocupa en el disco del usuario (decisión del usuario en S151). Hoy:
     291.502.931 B. Palancas medidas y no aplicadas: los nativos de OR-Tools de Linux y
     macOS (69.453.720 B, dependencias runtime transitivas de `ortools-java`, así que un
     recorte global rompería el desarrollo en Linux) y las herramientas de desarrollo del
     runtime (`jdk.compiler`, `jdk.jshell`, `jdk.javadoc`, `jdk.jlink`, `jdk.jpackage`…).
     Sin los primeros, la estimación es 222,0 MB: es una resta, no un bundle construido.
     **✔ CUMPLIDA en S152, y por una palanca que esta ficha no contemplaba: el RUNTIME.**
     `jpackage` sin `--add-modules` monta 64 módulos; la lista real son **14**, y con ella la
     carpeta de Windows baja a **231.691.863 B** (margen 18,3 MB), con el runtime de
     134.690.396 a 74.879.328 B, un 44,4 % menos. Tres corridas, el mismo número al byte. La
     poda de nativos NO se aplica (R-terminado): queda medida en `docs/empaquetado.md` §5
     —**69.453.720 B** sobrantes para un bundle de Windows, no 60.869.367, que es la resta
     desde Linux— por si el bundle vuelve a acercarse al límite.
  3. **Windows limpio.** ✔ CUMPLIDA (S156). **REESCRITA en S156** por decisión del usuario:
     decía «En Windows 10 y en Windows 11» y dejaba el medio de entrega a su Cambio. En
     Windows 11, sin Java ni Node y con una cuenta ESTÁNDAR (no miembro de Administradores),
     el bundle llegado por USB se extrae y arranca con doble clic sin ningún diálogo que pida
     una decisión de seguridad o credenciales. Sobre una copia del banco, las cuatro descargas
     hechas desde la interfaz pasan el oráculo (1285/0/0, 835/0/0, 819/0/0 y CSV OK), y el CSV
     se abre en Excel con columnas separadas y tildes correctas. Es la verificación final de
     las demás condiciones. **Se reabre** si aparece en el centro un equipo con Windows 10
     (según el usuario, todos están en Windows 11; no verificado en el centro), si la
     aplicación pasa a distribuirse por descarga o correo, o si un equipo del centro bloquea
     el ejecutable sin firmar. **Cumplida por `C-windows-limpio`** en un Windows 11 Home con
     la cuenta `educhronos-prueba`: ningún diálogo al arrancar, Control inteligente de
     aplicaciones desactivado, oráculo a cero en las cuatro descargas y CSV correcto en
     Excel. El procedimiento está en `docs/empaquetado.md`; el detalle, en la entrada de S156.
  4. **Arranque visible e instancia única.** ✔ CUMPLIDA (S154). Al lanzarla, se abre el navegador en
     la aplicación. Un segundo lanzamiento no deja otra instancia viva: lleva a la que ya corre. Nace
     de la prueba del usuario en S151, que relanzó la aplicación porque no veía nada. **Cumplida por
     `C-arranque-cierre`**: un candado (`educhronos.lock`, `FileChannel.tryLock`) en la carpeta de
     datos, tomado en `main` ANTES de construir Spring; si está cogido, la segunda instancia espera a
     que el puerto responda (hasta 60 s), abre el navegador y sale con 0 sin tocar la base. El
     navegador se abre en `http://127.0.0.1:8080` cuando la aplicación está lista, no al lanzar.
     Verificada en Windows: navegador a los ~7 s; relanzar con la pestaña cerrada la reabre con
     código 0 y los mismos dos procesos. El doble clic rápido se midió en Linux con marcas de tiempo
     (la segunda esperó 6,88 s y salió 174 ms después de que la primera escuchara); en Windows no
     dio pestaña de error, pero ahí no discrimina.
  5. **Cierre sin Administrador de tareas.** ✔ CUMPLIDA (S154). El usuario cierra la aplicación desde
     ella misma o desde un elemento visible de Windows. El cierre es ordenado (el log lo registra) y
     después no queda ningún proceso `Educhronos` ni el puerto ocupado. Este texto decía «hoy sólo se
     para matando el lanzador y su JVM»: deja de ser cierto. **Cumplida por** un icono en la bandeja
     con «Abrir Educhronos» y «Salir»; «Salir» cierra el contexto en un hilo propio no daemon, por el
     mismo apagado ordenado que un SIGTERM. Verificada en Windows: el icono se retira, el log registra
     el cierre de Tomcat, JPA y Hikari, el lanzador termina con código 0, no queda ningún proceso y el
     8080 queda libre.
  6. **Resultado visible.** ✔ CUMPLIDA (S154). Si el arranque falla (puerto ocupado, base
     inaccesible), el usuario ve un mensaje comprensible, y existe un log en una ubicación fija y
     documentada. Este texto decía «hoy el lanzador se desengancha de la consola y ningún error se
     ve»: deja de ser cierto. **Cumplida por** `educhronos.log` junto a la base
     (`%LOCALAPPDATA%\Educhronos` en Windows, el directorio de datos XDG en el resto), documentado en
     `docs/empaquetado.md`, y por un diálogo nativo en todo fallo de arranque: puerto ocupado, fallo
     anterior a Spring (carpeta imposible, candado ilegible) y cualquier otro, este con la ruta del
     log. Sin pantalla, el mensaje va a stderr y al log, y el proceso no se cuelga. Verificada en
     Windows con el 8080 ocupado: UN solo diálogo («No se puede abrir Educhronos: otro programa está
     usando el puerto 8080…»), sin el «Failed to launch JVM» del lanzador detrás.
  7. **Datos del usuario.** ✔ CUMPLIDA (S153). La base vive en una carpeta del usuario que
     no depende de desde dónde se lance ni de dónde esté el programa: lanzar desde dos sitios
     usa la misma base, y sustituir la carpeta del programa por otra versión no pierde datos.
     Este texto decía además que «hoy se crea en el directorio de trabajo, que con doble clic
     es la carpeta del programa; en S151 una copia por USB se llevó la base dentro»: deja de
     ser cierto. **Cumplida por `C-datos-usuario`**: un `EnvironmentPostProcessor` en
     `es.yaroki.educhronos.app.config`, registrado en `META-INF/spring.factories`, con una
     regla ÚNICA de no-op —si `spring.datasource.url` ya viene resuelta, no hace nada—, que
     permite retirar esa clave del `application.properties` de main. Resuelve
     `%LOCALAPPDATA%\Educhronos`, con respaldo en `%USERPROFILE%\AppData\Local`, en Windows;
     y `$XDG_DATA_HOME/educhronos`, con respaldo en `~/.local/share/educhronos`, en el resto.
     Si la carpeta no se puede crear, el arranque FALLA nombrando la ruta: NADA de volver al
     directorio de trabajo, que es el defecto que esta condición elimina. Verificada en el
     app-image de Linux y en Windows (cuenta de dominio `llozano`), con la base en
     `C:\Users\llozano\AppData\Local\Educhronos\educhronos.db`, el dato sobreviviendo a
     lanzar desde dos directorios de trabajo distintos y a mover la carpeta del programa, y
     cero `.db` dentro de las carpetas del programa. Es la decisión de la que depende la
     Fase 10.
  8. **Sólo local.** ✔ CUMPLIDA (S154). La aplicación escucha sólo en la interfaz de bucle local.
     Este texto decía «hoy escucha en todas (`*:8080` en Linux, `::` en Windows)»: deja de ser cierto.
     **Cumplida por** `server.address=127.0.0.1` en el `application.properties` de main, también en
     desarrollo. Verificada por la dirección de escucha, que es lo que discrimina: `127.0.0.1` en
     Linux y en Windows (humo y arranque), y desde la LAN rechazada en Linux. En la máquina de
     construcción la prueba desde otro equipo NO discrimina: con el bundle viejo, escuchando en `::`,
     tampoco entraba, por el cortafuegos del dominio. `localhost` sigue cargando en el navegador. La
     hipótesis del aviso del cortafuegos queda para la condición 3.
  9. **Rutas de la SPA.** ✔ CUMPLIDA (S155). F5 y una URL directa a una vista la sirven. Salda
     `D-spa-sin-fallback-de-rutas` — **SALDADA en S155.** **Cumplida por `C-rutas-spa`**: `ResolvedorRutasSpa`
     (subclase de `PathResourceResolver`) sirve `index.html` a toda ruta que no sea un fichero, salvo las que
     empiezan por `api/` o tienen extensión en su último segmento, que siguen dando 404; lo registra
     `RutasSpaConfig` sobre `/**`, y `spring.web.resources.add-mappings=false` hace que sea el único manejador.
     Una URL que no es ninguna vista la redirige Angular a la portada (`{ path: '**', redirectTo: '' }`), para
     que el reenvío no convierta un 404 en una pantalla en blanco. Verificada en Linux sobre el jar: rutas
     profundas y con query → 200 `text/html` con el mismo cuerpo que `/`; `/api/no-existe` → 404 en JSON;
     `/main-inexistente.js` → 404; en navegador, F5 y URL directa en `/horario/1` y `/configuracion/jornada`, y
     `/loquesea` → portada. No se verifica en Windows por decisión de alcance: el servido vive en el jar, igual
     en las dos plataformas, y la condición 3 lo re-verifica.
- **ESTADO en S151: 0 de 9.** Evidencia parcial que NO marca nada: la exportación pasa el
  oráculo con el bundle en dos Windows, pero ninguno es limpio con cuenta estándar (la
  máquina de construcción tiene cuenta de dominio y un JDK en disco; el Windows 11 Home no
  tiene Java ni Node, pero su cuenta es de Administradores con UAC). Windows 10, sin probar.
- **ESTADO en S152: 2 de 9.** Cumplidas la 1 y la 2 por `C-construccion-reproducible`. Las
  siete restantes intactas, y tres de ellas REPRODUCIDAS sobre el bundle nuevo sin marcar
  nada: la base se crea en el directorio de trabajo (7), escucha en `::` (8) y el navegador
  no se abre solo (4). La máquina de construcción sigue siendo de dominio, así que nada de
  lo medido en S152 vale para la condición 3.
- **ESTADO en S153: 3 de 9.** Cumplida la 7 por `C-datos-usuario`. Las condiciones 1 y 2 se
  RE-VERIFICAN sobre el bundle nuevo y siguen cumplidas: la carpeta de Windows mide
  **231.695.946 B**, 4.083 B más que los 231.691.863 de S152, y ese delta es exactamente lo
  que crece el jar al añadir el post-procesador, así que el margen sobre los 250 MB no se
  mueve de forma apreciable. Las seis restantes, intactas. La máquina de construcción sigue
  teniendo cuenta de dominio, así que **nada de lo medido en S153 vale para la condición 3**.
- **ESTADO en S154: 7 de 9.** Cumplidas la 4, la 5, la 6 y la 8 por `C-arranque-cierre`. Las
  condiciones 1 y 2 se RE-VERIFICAN: la carpeta de Windows mide **231.715.485 B**, 19.539 B más que
  en S153, casi todo del jar (+18.957 B); el margen sigue en 18,3 MB. Quedan la 9 y la 3. La máquina
  de construcción sigue siendo de dominio: **nada de lo medido en S154 vale para la condición 3**.
- **ESTADO en S155: 8 de 9.** Cumplida la 9 por `C-rutas-spa`. Queda sólo la 3. Cinco sesiones (S151–S155)
  frente al techo de ocho de §7. La condición 3 sigue sin máquina: la de construcción es de dominio, y el criterio
  pide además Windows 10, que no se ha probado en ningún equipo.
- **ESTADO en S156: 9 de 9, TERMINADO.** Cumplida la 3 por `C-windows-limpio`, con la condición reescrita a Windows 11 y USB. Seis sesiones (S151–S156) frente al techo de ocho de §7, y seis Cambios. NO se re-midieron en Windows 11, y se apoyan en S153–S154 con el mismo jar: el recuento de procesos, la dirección de escucha y el diálogo de puerto ocupado. El tamaño de la carpeta se leyó como tamaño en disco (231.985.152 B), que es cota superior de la suma de bytes que pide la condición 2.
- **Medido al abrir (S151):** tamaños en la ficha de notas técnicas de Fase 11 del plan;
  cada lanzamiento en Windows son dos procesos, el lanzador (~9 MB) y su JVM hija
  (~270 MB); `GET /api/jornada` da 08:00 en Windows; en el Windows 11 Home apareció el
  aviso del cortafuegos, «Permitir» no pidió credenciales y dejó dos reglas de entrada
  `Allow` con perfil `Public`; SmartScreen no apareció con la copia por USB; el navegador
  no se abre solo.
- **Depende de:** H1, H2 y H3 cerrados. No depende de generar: se verifica sobre copia del
  banco `educhronos-s137.db` (`dfa4c0774a842d8eb6b7a941e23df2a9`).
- **Valor:** es la condición de entrega (§2).
- **Cambios que agrupa:** `C-alcance-instalacion` ✔ HECHO (S151), el M0 y las mediciones
  que escribieron este criterio; `C-construccion-reproducible` ✔ HECHO (S152), los dos
  guiones de empaquetado, `docs/empaquetado.md` y las condiciones 1 y 2; `C-datos-usuario`
  ✔ HECHO (S153), el post-procesador de la ruta de la base y la condición 7; `C-arranque-cierre` ✔ HECHO (S154), el modo escritorio (instancia única, navegador, bandeja, diálogos y log) y la escucha local, condiciones 4, 5, 6 y 8; `C-rutas-spa` ✔ HECHO (S155), el reenvío de las rutas de la SPA a `index.html` y la ruta comodín de Angular, condición 9. `C-windows-limpio` ✔ HECHO (S156): `-HuellaJar` y la huella del zip en los guiones de empaquetado, la prueba final en Windows 11 con cuenta estándar y las órdenes del oráculo escritas en `docs/empaquetado.md`, condición 3. Los demás se deciden en sus M0. Agrupación orientativa,
  NO decidida: construcción y tamaño (1, 2); arranque, cierre, errores y escucha local
  (4, 5, 6, 8); datos (7); rutas (9); y la prueba final en Windows limpio (3).
- **Decisiones que quedan para sus Cambios:** el medio de entrega y la firma del
  ejecutable (condicionan SmartScreen) — **DECIDIDAS en S156**: USB y sin firma (en la máquina de prueba el USB no dispara SmartScreen y el Control inteligente de aplicaciones está desactivado), con sus disparadores de reapertura escritos en la condición 3; el mecanismo de arranque y cierre (un icono en la
  bandeja del sistema es candidato) — **DECIDIDO en S154**: modo escritorio activado por la propiedad `educhronos.escritorio` que pone el lanzador, bandeja con «Abrir» y «Salir», candado antes de Spring y diálogo nativo para los fallos. **La carpeta de datos ya no está pendiente: DECIDIDA en
  S153** por `C-datos-usuario` —`%LOCALAPPDATA%\Educhronos` en Windows y el directorio de
  datos XDG en el resto—, que es la decisión de la que dependía la Fase 10. **CORREGIDO en S152:** este
  paréntesis decía que elegir la bandeja impediría recortar `java.desktop`, y la medición lo
  deshace por el otro lado — `java.desktop` NO es recortable en ningún caso, porque lo exige
  el enlazador de propiedades de Spring Boot (`java.beans`) y sin él la aplicación ni
  arranca. La decisión de arranque y cierre no tiene coste de tamaño.
- **Fuera de este objetivo:** `D-hora-tramo-dependiente-de-zona` (Fase 12, runner en UTC)
  y la duplicación de curso (Fase 10).
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

#### O-curso — "El centro empieza el curso siguiente sin perder el anterior." ✔ TERMINADO (S163)
- **Propósito:** que el jefe de estudios empiece un curso académico a partir del anterior, cambiando sólo
  lo que cambia, y conserve el anterior para consultarlo. Es el paso 6 del guion de §1, el único eslabón
  que falla. Abierto en S158 como segundo objetivo de H4.
- **Por qué va ahora:** orden decidido en S157 (Fase 10 antes que Fase 12): la CI tiene que proteger la
  cadena entera y hoy la condiciona `D-e2e-centro-minimo-rojo`. La dependencia que frenaba este objetivo,
  dónde viven los datos, la resolvió la condición 7 de `O-instalación` (S153).
- **Terminado cuando** — ESCRITO en S158 sobre medición: bancos copiados a `/tmp`, jar de HEAD `b154bb9`,
  sin tocar el repo. SUSTITUYE a los cuatro criterios de la Fase 10, que no se marcan hasta que se cumpla
  éste, y descarta su «selector de curso activo al iniciar la aplicación» (ver Decisiones). O-curso termina
  cuando:
  1. **Duplicar.** Desde la interfaz, con un curso activo, una acción crea un curso nuevo cuyo nombre es su
     año académico en formato `AAAA/AAAA+1`, propuesto por defecto como el siguiente al del curso activo,
     editable y no repetible. La barra muestra el nombre del curso activo. Oráculo sobre una copia de
     `educhronos-s137.db`: las 17 tablas de configuración son idénticas fila a fila al origen, y
     `horario_generado`, `sesion`, `sesion_bloqueada` y `aula_bloqueada` quedan vacías. La copia es
     coherente aunque la aplicación esté abierta (candidato: `VACUUM INTO`, no medido con sqlite-jdbc).
  2. **Curso nuevo editable.** La actividad 20 del banco, que en el origen responde 409 al DELETE (medido en
     S158), se edita y se borra en el curso nuevo.
  3. **Anterior en solo lectura.** Sobre el curso archivado, toda petición de escritura se rechaza con un 4xx
     propio cuyo mensaje llega al cliente y la interfaz muestra, y la interfaz marca el curso como de solo
     lectura. El md5 del archivado no cambia ni al intentar escribir en él ni al escribir en el activo, lo que
     cubre también «los cambios en el curso activo no afectan a los archivados».
  4. **Horario archivado accesible.** En el curso archivado se ven las vistas de su ÚLTIMO horario generado y
     sus cuatro descargas pasan el oráculo (1285/0/0, 835/0/0, 819/0/0, CSV OK).
  5. **Cambiar de curso.** Desde la barra (hueco de D18), sin cerrar la aplicación. El curso elegido se
     mantiene al relanzar, y la instancia única y «Salir» siguen funcionando después de un cambio.
  6. **Actualizar sin perder datos.** Una carpeta de datos con la `educhronos.db` de la versión anterior,
     abierta con la nueva, pasa a ser el curso activo sin ninguna intervención. Si no tiene nombre, el
     diálogo de duplicar lo pide junto al del curso nuevo.
  7. **Desarrollo intacto.** Con `spring.datasource.url` explícita (desarrollo, tests, e2e), nada se escribe
     en la carpeta de datos del usuario. Medido en S158: hoy no se escribe.
  8. **e2e del eslabón 6.** Un spec en verde que duplica, cambia de curso y ve rechazada una escritura sobre
     el archivado, sin generar horario (decisión C). Decide `D-e2e-aislamiento`, porque es el segundo spec
     que escribe.
  9. **Windows.** Las condiciones 1 a 5 se verifican en el bundle, en Windows 11 con cuenta estándar.
- **ESTADO en S158: 0 de 9.**
- **ESTADO en S159: 2 de 9** (cumplidas la 2 y la 7). Con el backend hecho y sin su interfaz: la 1, la 3
  y la 6, cuyas mitades de interfaz pasan al Cambio del selector.
- **ESTADO en S160: 6 de 9** (cumplidas la 1, la 2, la 3, la 5, la 6 y la 7, en Linux). Quedan la 4, la 8 y
  la 9.
- **ESTADO en S161: 7 de 9** (se suma la 4, en Linux). Quedan la 8 y la 9.
- **ESTADO en S162: 8 de 9** (se suma la 8, en Linux). Queda la 9 (Windows).
- **ESTADO en S163: 9 de 9, TERMINADO** (se suma la 9). Las condiciones 1 a 5, verificadas en el bundle en Windows 11 Pro con cuenta estándar: oráculos en Linux sobre los ficheros traídos (17 tablas 0/0 contra el banco, md5 del archivado igual antes y después de escribir en el activo, descargas 1285/0/0, 835/0/0, 819/0/0 y CSV OK) y el log (tres arranques y tres «Salir» en el orden previsto, sin errores). Seis sesiones (S158–S163) y seis Cambios, dentro del techo de §7.
- **Medido al abrir (S158):** (1) Congelación: `DELETE /api/actividades/20` → 409 sobre una copia, md5
  intacto. La única FK hacia el horario es `sesion.horario_id`, con CASCADE; los pines cuelgan de
  `(actividad, indice)` y la guarda `ActividadService.exigirSinDependientes` los cuenta. No existe ni DELETE
  ni listado de horarios. El cuerpo del 409 llega sin `message` pese a `include-message=always`. (2) Solo
  lectura de fichero, inservible: con `open_mode=1` la aplicación no arranca (HikariCP llama a `setReadOnly`
  y sqlite-jdbc 3.53.2.0 lo rechaza); con `chmod 444` arranca y lee, y responde 500 genérico a las
  escrituras. Hay 60 handlers: 26 GET, todos de solo lectura, y 34 no-GET, ninguno de lectura. (3) El
  candado es de la carpeta de datos, no de la base, y no se suelta hasta que muere el proceso; `main` no
  conserva el contexto, y «Salir» usa el que capturó la bandeja. Arranque de unos 6 s. D18 reserva altura
  (unos 27 px), no ancho. (4) e2e: dos specs y ningún mecanismo de siembra; `centro-minimo` falla al generar.
  (5) `journal_mode=delete`, y nadie lo configura. (6) Cuatro cambios de `schema.sql`, ninguna migración,
  `user_version` 0.
- **Decisiones (S158, del usuario salvo la F, que sale de medir):** (A) Los pines NO pasan al curso nuevo: son
  ajustes de un horario concreto y congelarían actividades; las preferencias estables del centro van en
  restricciones. (B) El curso archivado muestra su último horario generado. (C) El e2e no genera horario:
  la visibilidad del horario archivado queda en la JVM y en el M4. (D) El año académico nombra el CURSO, no
  cada horario: cada base es un año. En la copia los horarios se eliminan igualmente, porque la guarda del
  409 no mira etiquetas. (E) El selector vive dentro de la aplicación y no antes de Spring: el candado
  sobrevive a reconstruir el contexto. **PRECISADA en S160:** el contexto no se rehace; el
  curso se cambia en caliente, sustituyendo el pool de la base bajo un datasource estable, porque el e2e
  corre con URL explícita y «Salir» y la bandeja guardan el contexto. (F) El solo lectura lo impone la aplicación, porque el de fichero no
  arranca o da 500 y no es portable a Windows. (G) La ejecución de la cadena entera de §1 sale de este
  objetivo: será un objetivo propio de aceptación, posterior.
- **Guarda genérica (S159):** la guarda de solo lectura de la condición 3 se aplica a TODA petición que no
  sea GET, por un mecanismo genérico, y no a una lista de endpoints: así cubre también los que se añadan
  después, empezando por el PUT de restricciones que usará `O-disponibilidad`.
- **Términos y requisitos del selector (S159):** curso ACTIVO = el editable, como mucho uno; curso ABIERTO = el
  que la aplicación tiene cargado, que puede ser un archivado. El Cambio del selector debe (a) listar los cursos
  de la carpeta de datos, porque un puntero roto abre `educhronos.db`, que puede estar archivado, y la salida
  existe (`/api/cursos` está exento de la guarda) pero no tiene pantalla; y (b) permitir duplicar un archivado
  cuando no queda ningún curso activo, que hoy `CURSO_ARCHIVADO` impide, o el centro quedaría en solo lectura.
  Limitaciones documentadas en el Javadoc de `DuplicadorCurso`: dos ventanas de corte de milisegundos.
- **Deudas que pasan a bloquear:** `D-horario-id-a-fuego` (condición 4) y `D-e2e-aislamiento` (condición 8).
  NO bloquean, medido: `D-horario-irreversible`, porque el curso nuevo no hereda la congelación si la
  duplicación no copia horarios ni pines; y `D-e2e-centro-minimo-rojo`, por la decisión C. **SALDADA en S161:** `D-horario-id-a-fuego`. Sólo queda bloqueando `D-e2e-aislamiento`. **SALDADA en S162:** `D-e2e-aislamiento`. Ninguna deuda bloquea ya el objetivo.
- **Depende de:** `O-instalación` (carpeta de datos, candado, bandeja). Se verifica sobre una copia de
  `educhronos-s137.db` (`dfa4c0774a842d8eb6b7a941e23df2a9`).
- **Valor:** cierra el último eslabón de §1 que falla.
- **Cambios que agrupa:** `C-alcance-curso` ✔ HECHO (S158): el M0, las seis mediciones y este criterio.
  Agrupación orientativa, NO decidida: duplicado y guarda de solo lectura en el backend (1, 2, 3, 6, 7);
  selector y cambio de contexto (5); horario vigente del curso (4); e2e (8); Windows (9). **`C-duplicado-guarda` ✔ HECHO (S159, `915b18e` + `116f44e`):** duplicado por `VACUUM INTO`,
  tabla `curso` de fila única, puntero `curso-abierto` y guarda de solo lectura por filtro (403
  `CURSO_SOLO_LECTURA`). Las mitades de interfaz de 1, 3 y 6 pasan al Cambio del selector (5). **`C-selector-curso` ✔ HECHO (S160, `254e9d2` + `e6b15ae` + `bc0355a` + `7825c38`):** cambio de curso en
  caliente por datasource conmutable, lista y apertura de cursos, duplicar abre el nuevo y duplica un
  archivado sin activos, nombre no repetible por contenido; barra con el curso abierto y la marca de solo
  lectura, y diálogo de cursos. Cumple la 1, la 3, la 5 y la 6. **`C-horario-vigente` ✔ HECHO (S161, `f43f00e` + `653b638` + `3ef960d`):** `GET /api/horarios/vigente` (200 con el id mayor, o 204) y ruta `/horario`, que resuelve el último horario del curso abierto al navegar; barra y landing enlazan `/horario`, y un curso sin horario muestra un aviso con «Generar». Cumple la 4 y salda `D-horario-id-a-fuego` y, de paso, `D-vista-horario-sin-horario`. **`C-e2e-curso` ✔ HECHO (S162, `fae5470`):** `curso.spec.ts` duplica una base sin nombre, da de alta un nivel en el curso nuevo como control positivo, abre el archivado y ve en el formulario el rechazo de la guarda; sin reintentos. La suite pasa a correr en serie (`workers: 1`) con la base en `app/target/e2e/`, que se borra entera en cada corrida, y con un invariante de orden: el spec que archiva la base de arranque corre después de todo spec que escriba. Cumple la 8 y salda `D-e2e-aislamiento` y `D-playwright-cita-seed-difunto`. **`C-windows-curso` ✔ HECHO (S163, sin cambio de código):** bundle de HEAD `d85c79b` construido en una máquina virtual nueva (VirtualBox, Windows 11 Pro) sin tocar el procedimiento de `docs/empaquetado.md`, y probado con una cuenta estándar. Cumple la 9. Hallazgo que no afecta al criterio: `D-curso-pestanas-desfasadas` (§4).
- **Fuera de este objetivo:** la cadena entera de §1 (su orden respecto a la Fase 12 se decide al cerrar
  éste); `D-horario-irreversible`; la migración de esquema entre versiones; `D-e2e-centro-minimo-rojo`
  (Fase 12); `D-curso-sin-borrado` (mejora futura; se revisa al cerrar éste). **Revisada en S163:** sigue como mejora futura y pasa a colgar del objetivo de aceptación (§4).
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

#### O-aceptación — "La cadena entera de §1 pasa en un Windows limpio." ✔ TERMINADO (S172, con salvedad)
- **Propósito:** ejecutar el guion de §1 como UNA sola cadena, de punta a punta, en el bundle sobre un
  Windows limpio, ejecutado por un humano, sin tocar la base de datos ni editar ningún JSON. Es el estado
  final del proyecto (§1). Abierto en S168 como tercer objetivo de H4.
- **Por qué en H4 (S168, usuario a propuesta del arquitecto):** la aceptación no tenía hito. H4 es «la
  condición de entrega» y el guion corre sobre el bundle de Windows. Un H5 contradiría la definición de hito
  de §2, porque no añade ninguna capacidad para el usuario. El nombre de H4 no la describe: está aquí por su
  valor, no por instalar ni pasar de curso.
- **Por qué va ahora:** orden de S163, `O-disponibilidad` → aceptación → Fase 12. `O-disponibilidad`
  terminó en S167.
- **Medición (S168, M2 de solo lectura, Claude Code, HEAD `4d3b39c`, bancos con md5 intacto):**
  (a) Los pasos 2, 3 y 4 no se han ejecutado nunca en el bundle de Windows: S156 y S163 partieron de una
  copia del banco y no generaron. El nativo de OR-Tools no se ha cargado nunca en Windows; en Linux sí, sobre
  el runtime recortado (S152). (b) Con una actividad de 2 tramos, el CSV sale con una sola fila, el PDF la
  pinta sólo en su tramo de inicio y deja el segundo vacío, y `oraculo-exportacion.py` da OK sobre los dos
  ficheros porque sólo mira el tramo de inicio (`:72-74`); es `D-proyeccion-sin-duracion`. (c) Los oráculos
  son genéricos en los datos: `VerificadorSolucion`, por `GET /api/horarios/{id}/diagnostico`, y los scripts
  `oraculo-exportacion.py` y `verificar-leyenda-pdf.py` sacan lo esperado de la base que reciben. Los dos
  scripts fijan la maqueta del PDF: 5 días y un recreo. (d) No hay ningún centro sintético versionado: el
  e2e crea 5 de los 10 elementos del paso 2 (faltan desdobles, agrupamientos, PDC, tutores y disponibilidad),
  y los centros de S166 y S167 viven en /tmp.
- **Terminado cuando** — ESCRITO en S168 sobre esa medición. O-aceptación termina cuando:
  1. **Guion.** Existe un guion versionado, `docs/guion-aceptacion.md`, con el centro de aceptación y, para
     cada paso de §1, la acción, el resultado esperado y el oráculo que lo juzga. El centro de aceptación
     contiene al menos una vez cada elemento del paso 2 —profesores, aulas, asignaturas, grupos, currículo,
     un desdoble, un agrupamiento, un PDC, tutores y disponibilidad DURA y BLANDA— y una actividad de 2
     tramos. Su jornada tiene 5 días y un recreo. **✔ CUMPLIDA en S171** (`C-guion-aceptacion`, `0356a80` y `f91fa1c`): el centro de aceptación tiene 4 profesores, 3 aulas, 7 asignaturas, dos grupos ordinarios y un PDC, un desdoble, un agrupamiento, tutores, disponibilidad DURA y BLANDA y una actividad de 2 tramos, sobre una jornada de lunes a viernes con un recreo. Cada paso trae acción, resultado esperado, oráculo y captura; §7 da los oráculos a posteriori y §8 la plantilla del acta. Predicho por API y ensayado por un humano por pantalla en Linux: los pasos 2 a 6 pasan su oráculo; el paso 1 queda para la condición 4. El ensayo corrigió cuatro defectos del texto y destapó `D-vista-horario-estado-rancio`, que el guion declara como defecto conocido.
  2. **Generación en Windows.** El bundle genera un horario sobre un centro sintético en un Windows 11 con
     cuenta estándar, y el diagnóstico da 0 violaciones duras. **✔ CUMPLIDA en S169** (`C-windows-generacion`): bundle de `65222f0`, centro de S167 sembrado en Linux y llevado como base sin horario; OPTIMAL, objetivo 1.0 y 0 violaciones, igual que la predicción de Linux con el mismo jar y que el diagnóstico recalculado sobre la base traída. Las tres DLL de Visual C++ no están en `System32` y el proceso las carga de `runtime\bin`.
  3. **Bloques en la exportación.** Una actividad de más de un tramo aparece en todos los tramos que ocupa en
     la rejilla, en el CSV y en los PDF de grupo, profesor y aula. El oráculo de exportación comprueba todos
     los tramos que cubre cada sesión, y se demuestra por mutación que detecta un bloque pintado en una sola
     celda. **✔ CUMPLIDA en S170** (`C-exportacion-bloques`, `7270209`, `c634f87`, `5031753` y `4c07553`): la proyección lleva la duración y `SesionVistaDTO.tramosCubiertos()` es la única definición en Java de los tramos que ocupa una sesión; el CSV da una fila por tramo cubierto, con la cabecera intacta, y el de s137 sale idéntico byte a byte; los PDF pintan la entrada en cada celda; la rejilla la pinta en todos sus tramos, y la continuación no se arrastra ni lleva candado ni aviso de coste. El oráculo expande cada sesión por `actividad.duracion_tramos` y aborta ante un bloque que se sale del día o cruza el recreo. La mutación es la salida del código anterior (`2fd0b58`): falla en el CSV y en los tres PDF con el lunes, tramo 2 como único faltante. Verificado en Linux con las cuatro descargas hechas desde la vista y con el bloque movido a mano: cruzar el recreo y salirse del día se rechazan con `BLOQUE_IMPOSIBLE`, y moverlo a martes 1 se acepta.
  4. **Cadena.** Un humano ejecuta el guion entero en un Windows 11 limpio con cuenta estándar, sin Java ni
     Node, con el bundle construido desde el commit que se acepta, sin tocar la base ni editar ningún JSON, y
     cada paso pasa su oráculo. Si un paso falla, se arregla en un Cambio y la cadena se repite completa desde
     el paso 1 en un Windows limpio. La corrida válida deja un acta versionada con el commit, el sha256 del
     bundle, la fecha y el resultado de cada paso. **✔ CUMPLIDA en S172 CON SALVEDAD, por decisión del usuario** (`C-corrida-aceptacion`; acta `docs/actas-aceptacion/acta-s172.md`, `f0cc718`): commit aceptado `8d9a74a`, jar `7b65bd48…`, zip `76e363d8…`, carpeta 231.770.158 B; Windows 11 Pro 10.0.26200.9457 en la máquina virtual de S163, cuenta estándar nueva `prueba2` sin Java, Node ni las DLL de Visual C++ en `System32`. Pasan los pasos 1 a 5 y los bloques §7.2 a §7.4 enteros. Salvedades: el paso 6 falla en §7.5 tal como está escrito —el curso nuevo tiene un horario porque el ejecutor pulsó «Generar horario» tras ver el mensaje de curso sin horario, que no quedó capturado; el duplicado conserva las 17 tablas sin diferencias y no copia horario ni pines—; cuatro capturas faltan o no muestran lo exigido (3b, 4a-1, 4a-2 y 4d), y el recálculo de §7.3 cubre sus resultados; y dos desviaciones de ejecución en el paso 2 (PDC antes que el tutor; «Profesor» por «Profesora» en P2 y P4), sin efecto en §7.2.
- **Decisiones (S168, usuario a propuesta del arquitecto):** (A) El centro de aceptación es nuevo, pequeño
  y versionado; su tamaño lo fija lo que exige el paso 2, no la escala. (B) El guion incluye una actividad
  de 2 tramos, y `D-proyeccion-sin-duracion` pasa a bloquear la condición 3. Se descarta declararlo como
  limitación: el producto deja crear bloques, el solver los resuelve y la disponibilidad los respeta (S166);
  el origen del centro real tiene un bloque de FPB de 3 tramos (Hallazgo G); y un PDF que pinta una clase de
  dos horas como si fuera de una contradice «presentable» (S136). (C) Los oráculos se usan tal cual, salvo
  que el de exportación aprende la duración antes de usarse; la maqueta fija queda fuera del criterio.
  (D) `D-jornada-congelada-por-disponibilidad`, `D-curso-pestanas-desfasadas` y `D-curso-sin-borrado` quedan
  fuera del criterio y no bloquean. (E) El primer Cambio es generar en el bundle de Windows, porque es el
  riesgo mayor y menos medido (precedente S151); incluye comprobar que la máquina virtual de S163 construye
  desde HEAD y cuánto mide el bundle. (F) «Pasa entero»: un fallo se arregla en un Cambio y la cadena se
  repite completa; una corrida válida deja su acta. **(G) S172, usuario, a propuesta del arquitecto:** «Windows limpio» se concreta como una cuenta estándar NUEVA en la máquina virtual de S163, con la ausencia de Java, Node y las DLL de Visual C++ comprobada antes de instalar, y no como un sistema recién instalado; un script de desinstalación se descartó porque sólo borra lo que se conoce. El acta de cada corrida vive en `docs/actas-aceptacion/`, y las capturas fuera del repo, identificadas por su sha256 en el acta. **(H) S172, usuario, CONTRA la recomendación del arquitecto:** la corrida se acepta con salvedad y no se repite, aunque la regla F lo exigía, porque el fallo es de ejecución y no del producto y el ejecutor vio el resultado del paso 6. El arquitecto recomendó repetir la cadena con una cuenta nueva. No sienta precedente: la regla F sigue vigente para cualquier corrida futura.
- **Fuera del criterio, con motivo (S168):** el centro real, porque teclear unas 816 escrituras a mano no es
  una prueba repetible y la escala ya se demostró por la API en S135; jornadas con otro número de días o más
  de un recreo, porque los dos scripts fijan la maqueta del PDF y el centro real tiene 5 días y un recreo; el
  e2e y `D-e2e-centro-minimo-rojo`, porque §1 define la aceptación como ejecución humana y su sede es la
  Fase 12; `D-jornada-congelada-por-disponibilidad`, porque el guion define la jornada primero y el paso 6
  conserva la configuración; `D-curso-pestanas-desfasadas`, porque con una sola pestaña no ocurre y la
  secuencia con dos está deducida, no reproducida; `D-curso-sin-borrado`, porque nada muestra que haga falta;
  y mejorar los guiones de empaquetado, porque los rehará la Fase 12 (R-invalidación).
- **Deudas que pasan a bloquear:** `D-proyeccion-sin-duracion` (condición 3), SALDADA en S170.
- **Depende de:** `O-instalación`, `O-curso` y `O-disponibilidad`, y de la máquina virtual de construcción de
  S163.
- **Valor:** es el estado final del proyecto; mientras no pase, el proyecto no está terminado (§1).
- **Cambios que agrupa:** `C-alcance-aceptacion` ✔ HECHO (S168): el M0, el M2 y este criterio.
  `C-windows-generacion` ✔ HECHO (S169): condición 2, con la construcción desde HEAD y la medida del bundle.
  `C-exportacion-bloques` ✔ HECHO (S170): condición 3, en cuatro fases (oráculo, backend, rejilla y M4).
  `C-guion-aceptacion` ✔ HECHO (S171): condición 1, con la predicción por API y el ensayo humano en Linux.
  `C-corrida-aceptacion` ✔ HECHO (S172): condición 4, con salvedad.
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

#### O-ci — "Cada cambio se prueba solo, y cada versión sale construida y aceptada." TERMINADO (S177)
- **Propósito:** automatizar en GitHub Actions las suites en cada push y la construcción del bundle de Windows por tag, y aceptar ese bundle con la cadena de §1. Es la Fase 12 del plan, cuarto objetivo de H4 y último del hito. Abierto en S173.
- **Por qué va ahora:** orden de S163, `O-disponibilidad` → aceptación → Fase 12. `O-aceptación` terminó en S172.
- **Medición (S173, M2 de solo lectura, Claude Code, HEAD `b04069c`):**
  (a) El remoto es `github.com/LuisLozano/EduChronos` y es público (API anónima 200, control 404); no existe `.github/`.
  (b) Versiones fijadas en el repo: `maven.compiler.release` 17 (`pom.xml:24`), OR-Tools 9.11.4210 (`pom.xml:26`), Node v22.23.1 (`app/pom.xml:117` y `.nvmrc`), npm 10.9.8 (`packageManager`). Angular se construye en `prepare-package`, fuera de `mvn test`.
  (c) Suites en local: `mvn test` 52 s (116 + 558), vitest 12 s (570 en 58 ficheros), e2e 64 s con `centro-minimo` en rojo (0 `.instancia` frente a 3). `ng test` no entra en modo watch sin TTY.
  (d) `TZ=UTC mvn test` pasa entero, con el cambio de zona comprobado en las marcas del log.
  (e) El `.ps1` no usa la red ni WiX, sólo PowerShell y el JDK de la entrega; el JDK de Windows lo baja `empaquetar-linux.sh`. Lo manual es el traspaso entre máquinas y copiar `-HuellaJar` de la consola.
  (f) Un solo tag, ajeno a versiones; sin `project.build.outputTimestamp`.
  (g) `playwright.config.ts:29` da `retries: 2` con `CI`.
- **Terminado cuando** — ESCRITO en S173 sobre esa medición. `O-ci` termina cuando:
  1. **Tests en cada push.** Un workflow versionado en `.github/workflows/` ejecuta, en cada push a `main`, `mvn test` (solver y app), vitest y el e2e en un runner Linux, y cualquier fallo deja la ejecución en rojo. Se verifica con una ejecución en verde y con un fallo provocado que la pone en rojo. **CUMPLIDA en S176** con `C-pipeline-push`: `.github/workflows/tests.yml` (`29bb081`) corre en cada push a `main` tres jobs independientes en `ubuntu-24.04` —`mvn test`, vitest y el e2e—. La ejecución #1 salió en verde (116 + 558, 570 en 58 ficheros, e2e 3 de 3), y las #2 a #4, lanzadas a mano sobre ramas desechables con un mutante por suite, pusieron en rojo sólo el job de su suite, por su motivo, leído en el log por el usuario.
  2. **e2e en verde.** `centro-minimo` pasa en local y en CI, y en CI el e2e corre sin reintentos. **CUMPLIDA EN LOCAL en S175** con `C-e2e-verde`: el spec confirma el diálogo de generación, que S145 hizo obligatorio, y espera el POST; el e2e corre sin reintentos también con `CI`. La mitad «en CI» se verifica con la condición 1, porque hasta entonces ningún workflow corre el e2e. **CUMPLIDA EN CI en S176:** en la ejecución #1 de `tests.yml` el e2e pasa 3 de 3 sin reintentos (0 coincidencias de `retry` en el log).
  3. **Bundle por tag.** Un tag `v*` dispara la construcción: un job Linux produce el jar, el JDK de Windows y la huella del jar, y un job Windows ejecuta `empaquetar-windows.ps1` con `-HuellaJar` tomada de la salida del job anterior. El zip y su sha256 se publican como asset de la Release del tag, con la carpeta del app-image dentro del límite de 250.000.000 B. Se verifica con un tag real. `docs/empaquetado.md` describe la vía CI como principal y la manual como respaldo. **CUMPLIDA en S174** con `C-bundle-por-tag`: tag `v0.1.0-rc.1`, Release pre-release con `Educhronos-win.zip` y su `.sha256`, carpeta del app-image 231.770.158 B.
  4. **El bundle de CI pasa la aceptación.** Un humano ejecuta `docs/guion-aceptacion.md` entero sobre el zip descargado de la Release, en un Windows 11 limpio según la decisión G de `O-aceptación`, sin Java ni Node, y cada paso pasa su oráculo. Rige la regla F de `O-aceptación` sin excepciones. El acta, en `docs/actas-aceptacion/`, lleva el tag, el commit y el sha256 del zip de la Release. **AMPLIADA en S175 (usuario, a propuesta del arquitecto):** además, en la cuenta de la corrida de S172, sustituir la carpeta del programa por la del zip de la Release conserva el curso activo y los archivados, y el horario se genera sin tocar la base a mano. Motivo: la guía de distribución incluirá ese paso, no se ha ejecutado nunca y `O-ci` es el último objetivo planificado, así que fuera del criterio no se haría nunca. Medido en S175: `schema.sql` es idéntico en `8d9a74a` (el bundle de S172) y en HEAD, así que `D-esquema-sin-version` no muerde en esta actualización. **CUMPLIDA en S177** con `C-aceptacion-ci`: un humano ejecuta el guion entero sobre el zip de la Release `v0.1.0` (commit `a7846b6`, sha256 `7afc5b8c…9fb170`) en Windows 11 Pro con la cuenta estándar nueva `prueba4`, y los seis pasos pasan su oráculo sin salvedad; en `prueba2`, sustituir la carpeta de S172 por la de `v0.1.0` conserva los dos cursos al byte y el activo genera sin tocar la base (§7.6). Acta en `docs/actas-aceptacion/acta-s177.md`.
- **Decisiones (S173, usuario a propuesta del arquitecto):**
  (A) El repositorio es público: los runners estándar son gratuitos y sin cupo, así que el coste no condiciona el diseño.
  (B) El bundle se publica como asset de la Release del tag, no como artefacto de Actions, que caduca (90 días como máximo en repos públicos).
  (C) La Release es fuente interna: el usuario descarga el zip desde Linux y lo entrega al centro por USB. La decisión de S156, sin firma de código, sigue en pie.
  (D) La huella del jar pasa del job Linux al job Windows como salida de job, no dentro del artefacto. Conserva la defensa de S156: la huella viaja por un canal distinto de la entrega.
  (E) La condición 4 es el guion de aceptación completo, porque el bundle de CI es un artefacto distinto del aceptado en S172 y §1 exige la cadena sobre el bundle que se entrega. Se descartó probar sólo instalar, generar y exportar.
  (F) El primer Cambio es el job de Windows, porque es el riesgo mayor y no está medido: la prueba de humo del `.ps1` abre el navegador y la bandeja (`ps1:238-243`), y no se sabe si eso funciona en un runner. `-SinHumo` es la salida. **CORREGIDO en S174:** decía «(precedente: decisión E de `O-aceptación`)», pero la decisión E no menciona `-SinHumo`: su precedente es sólo que el primer Cambio ataca el riesgo mayor. El riesgo se resolvió en S174 por diseño, no por medición (decisión G).
  (G) La CI empaqueta con `-SinHumo` (S174). La condición 3 no pide humo; sobre una base vacía el humo no llega al solver; dos de sus fallos («quedan procesos», «puerto ocupado») terminan en 0; y el arranque del bundle lo prueba la condición 4.
  (H) Se ensaya con `workflow_dispatch`, que construye sin publicar, y la condición 3 se verifica con un tag real de pre-release, `v0.1.0-rc.1` (S174). No es el tag que aceptará la condición 4, porque las condiciones 2 y 1 cambiarán código.
  (I) La pipeline de push (S176): runner fijado en `ubuntu-24.04`, porque un workflow que corre en cada push sólo debe cambiar de imagen con un commit; tres jobs independientes, para que el rojo de una suite no oculte el de otra; el job e2e instala el solver (`mvn -pl solver -am install -DskipTests`) y compila `app` antes de Playwright, porque el backend del e2e arranca con `mvn -pl app spring-boot:run` sin `-am` y los 120 s del `webServer` deben medir sólo el arranque; sin caché (`package-manager-cache: false`), por «Fuera del criterio»; y, si falla el e2e, su traza se sube como artefacto de 7 días. `bundle.yml` conserva `ubuntu-latest` (S174, observación (e)).
- **Fuera del criterio, con motivo:** el bundle de Linux, porque §1 sólo exige Windows; la firma de código, por la decisión C; optimizar cachés o tiempos, porque las suites tardan unos dos minutos; y las pull requests desde forks, porque el proyecto no las recibe.
- **Deudas que pasan a bloquear:** `D-e2e-centro-minimo-rojo` y `D-e2e-retry-bd` (condición 2). Saldadas las dos en S175 (`C-e2e-verde`).
- **Deudas que cuelgan de aquí sin bloquear:** `D-jar-no-reproducible` (la identidad la dan el tag y el sha256 de la Release), `D-reenvio-spa-sin-guarda-automatica`, `D-contrato-dto-mide-jackson2`, `D-props-main-invisibles-en-tests` y `D-guarda-escritura-sin-caso`. `D-ps1-palanca-de-linux` se saldó en S174.
- **Para la condición 4 (nota de S174):** `docs/guion-aceptacion.md:36-43` construye el bundle a mano, pide anotar la huella del jar y guardar el jar aparte. Con la vía CI el zip es el de la Release, la huella está en el log del job `linux` y el jar va dentro del zip (`Educhronos\app\`). Lo ajusta el Cambio de la condición 4, con su M2. **Nota de S175:** la guía de distribución paso a paso (tag → Release → verificar el sha256 en Linux → USB → extraer y abrir en Windows → actualizar) se escribe en `docs/empaquetado.md` dentro de ese Cambio, y el paso 1 del guion remite a ella, para que la corrida la ejecute. Parte puede existir ya por la condición 3; lo mide su M2. Hay un borrador en el chat de S174, fuera de la documentación. **Nota de S176:** el M1 de S176 buscó el anuncio del paso de `ubuntu-latest` a 26.04 en los issues «Announcement» de `actions/runner-images`: lo encontró: "[Ubuntu] `ubuntu-latest` label will use Ubuntu 26.04 in November 2026" (https://github.com/actions/runner-images/issues/14748, abierto el 2026-09-17), que da como fecha del cambio "beginning October 19, 2026". `tests.yml` fija `ubuntu-24.04` y no depende de esa fecha; `docs/empaquetado.md` la repite desde S174 y se revisa en este Cambio, que reescribe el documento.
- **Depende de:** `O-instalación`, `O-aceptación` (guion, regla F y actas) y la máquina virtual de S163 para la condición 4.
- **Valor:** cierra H4 y, con él, el último hito. La versión que se entrega sale de un procedimiento fijo y aceptado, no de una máquina concreta.
- **Cambios que agrupa:** `C-alcance-ci` ✔ HECHO (S173): el M0, el M2 y este criterio. `C-bundle-por-tag` ✔ HECHO (S174): `.github/workflows/bundle.yml` y los ajustes de `empaquetar-windows.ps1` (condición 3). `C-e2e-verde` ✔ HECHO (S175): `centro-minimo` confirma la generación y espera el POST, y el e2e corre sin reintentos y guarda la traza al fallar (condición 2, en local). `C-pipeline-push` ✔ HECHO (S176): `.github/workflows/tests.yml` (condición 1 y la mitad en CI de la condición 2). `C-aceptacion-ci` ✔ HECHO (S177): runners de `bundle.yml` fijados, guion y guía de distribución ajustados, tag `v0.1.0` y la corrida con su acta (condición 4).
- **Cierre (S177):** TERMINADO, 4 de 4, en cinco sesiones (S173–S177) y cinco Cambios. Las deudas que colgaban de aquí sin bloquear (`D-jar-no-reproducible`, `D-reenvio-spa-sin-guarda-automatica`, `D-contrato-dto-mide-jackson2`, `D-props-main-invisibles-en-tests` y `D-guarda-escritura-sin-caso`) quedan sin sede: no hay objetivo planificado después de H4, y se reasignan al planificar lo siguiente.
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

### H5 — Un profesor hace el horario 2026/2027 con Educhronos

#### O-demo-bundle — "El centro ve su curso en Educhronos y dice qué le falta." ✔ TERMINADO (S189, con salvedad)
- **Propósito:** que la persona que hoy elabora los horarios del centro —la secretaria, que también es profesora y recopila los datos que el centro entrega a una empresa externa— vea el curso 2025/2026 cargado en Educhronos y lo maneje ella sobre el bundle de Windows, y que de ese uso salgan las primeras incidencias y un inventario de lo que Educhronos no puede representar. Deja preparada la base de partida de la prueba del profesor. Primer objetivo de H5, abierto en S180.
- **Por qué va ahora:** orden de S178 (§2): demo → carga de los PDF de 2026/2027 → preparación → prueba del profesor → comparación.
- **Medido al abrir (S180, M2 de solo lectura, Claude Code, HEAD `5c82bc3`, jar de la Release `v0.1.0` en Linux sobre copias en `/tmp`):**
  (a) Esquema. `educhronos-s137.db` y `educhronos-s137-centro-completo.db` tienen 21 tablas, `user_version` 0 y ninguna tabla `curso`, con `integrity_check` y `foreign_key_check` limpios. Sus 21 tablas son idénticas en columnas, claves foráneas y DDL a las que crea `v0.1.0`. El único cambio de `schema.sql` posterior a S137 es `915b18e` (tabla `curso`), y de `a7846b6` a HEAD no cambia.
  (b) Apertura. `v0.1.0` abre los dos bancos en modo escritorio, crea la tabla `curso` vacía y los adopta como curso activo sin nombre (condición 6 de `O-curso`). Abrir un banco cambia su md5 aunque no cambie ningún dato.
  (c) `s137.db`: horario vigente id 1, FEASIBLE, 819 sesiones; las cuatro descargas pasan el oráculo (1285/0/0, 835/0/0, 819/0/0, CSV OK).
  (d) Centro completo: una generación con el presupuesto por defecto agota los 600 s (601,65 s), FEASIBLE, objetivo 234, 0 violaciones duras (211 ventanas + 23 consecutivas); sus descargas pasan el oráculo. Se reproduce `D-post-horario-sin-sesiones` sobre el jar de `v0.1.0`.
  (e) Duplicado a 2026/2027: sin `nombreActual`, 400 `NOMBRE_INVALIDO`, por diseño; con él, 201. Las 17 tablas de configuración no tienen ninguna diferencia, las 4 de horario quedan vacías y el curso archivado rechaza escribir con 403 `CURSO_SOLO_LECTURA`.
  (f) Cargador: sus 12 llamadas existen igual en `a7846b6`, y la guarda de solo lectura no afecta a una carga sobre base vacía.
  (g) Windows: la carpeta es `%LOCALAPPDATA%\Educhronos` y la base, `educhronos.db` o la que nombre el puntero `curso-abierto`. Una base con otro nombre se ignora sin aviso: se crea una `educhronos.db` vacía al lado y «Cursos…» no la lista. `docs/empaquetado.md:448-450` dice en una línea que el banco se coloca como `educhronos.db`; no hay procedimiento.
- **Decisiones (S180, del usuario a propuesta del arquitecto salvo donde se indica):**
  (A) La base es `educhronos-s137.db`: trae su horario, medido en `v0.1.0`, y su md5 está citado desde S137. Un horario nuevo del centro completo saldría equivalente (234 frente a 235, sin disponibilidad cargada) y sería una base más que citar.
  (B) El curso 2025/2026 queda sin nombre. Nombrarlo exigiría editar la base por SQL, que es la migración propia que R-invalidación excluye; el diálogo de duplicar pide el nombre por diseño.
  (C) Asiste la secretaria del centro, que elabora hoy los horarios (usuario). La demo se hace en la máquina virtual de S163, que reproduce su máquina (usuario), con una cuenta estándar nueva, porque las de S177 conservan estado.
  (D) No se construye un 2026/2027 completo: sería la prueba (4) hecha con ayuda y contaminaría la comparación (5) con lo que sabemos del horario oficial. El ejercicio usa cambios de muestra.
  (E) Ella opera y nosotros observamos. La consigna es de tareas de resultado, sin menús ni botones, se escribe antes y se lee igual en el ensayo y en la demo. Ella piensa en voz alta; cada pregunta se anota antes de responderla; el umbral de atasco, de unos 5 minutos, se fija antes. Cada ayuda es una incidencia candidata.
  (F) El inventario va después del ejercicio, para no darle antes el vocabulario del modelo. Se hace por tipo de dato con el paquete que entregó a la empresa para 2026/2027, si lo trae —el de 2025/2026 no está disponible (usuario)—, o a partir de lo que ella cuente si no. Sus valores no se cargan.
  (G) La demo no instala la base en la máquina del profesor: deja la base de partida con su md5 citado, y dónde se instala para la prueba lo decide el objetivo (3).
  (H) S181. Los Cambios se agrupan por su dependencia de la fecha con la secretaria, no por la agrupación orientativa: la condición 3 dice «ella» y va con la demo, y la 6 no depende de ella. `C-preparacion-demo` cubre 1, 2 y 6; la demo, 3, 4, 5 y 7.
  (I) S181. Cambios de muestra, sobre el M2 de S181 (HEAD `1fde345`): con el horario del banco no se puede editar ninguna actividad (409 hasta duplicar; no hay forma de descartar un horario) y los 28 grupos están a 30 de 30 horas. BYG1 se va y BYG4, nueva, asume sus cuatro actividades, lo que hace visible que no existe «sustituir profesor»; en 4ºC, Latín pasa de 3 a 2 horas y Geografía e Historia de 3 a 4, para que el grupo siga en 30; ING1 «No puede» los viernes y FIS2 «Prefiere no» a primera hora. La consigna no avisa del 409. **Nota S188:** editar BYG1 (código y nombre) conserva sus plazas y resuelve una sustitución completa; la premisa «no existe sustituir profesor» sólo vale cuando las clases se reparten entre varios. Lo señaló la secretaria en la demo (T5); por lectura de `v0.2.0`, el formulario y `ProfesorService.editar` admiten cambiar los dos (`ProfesorService.java:88-99`, M2 de S188).
  (J) S181. Una pregunta sobre QUÉ pide una tarea es una ACLARACIÓN, defecto de la consigna; una sobre CÓMO se hace es una AYUDA, incidencia candidata, y sólo se responde pasado el umbral o si ella lo pide.
  (K) S181. Transporte: de Linux a Windows sólo por USB, porque la carpeta compartida marca los ficheros y SmartScreen detiene el zip (S177); de Windows a Linux, por la carpeta compartida. Instantáneas `s177-estado` (red de seguridad) y `s181-demo-lista`, de la que parten el ensayo y la demo. La base de partida sale de la primera apertura, antes de duplicar.
  (L) S182, del usuario, con la recomendación contraria del arquitecto escrita. Antes de la demo se abre `O-pre-demo`, para que la secretaria no se lleve una impresión equivocada, con tres arreglos: poder aumentar el tiempo de cálculo (`D-generacion-no-reproducible`); bloquear la aplicación con una barra de progreso mientras se genera (`D-generacion-sin-exclusion` en su parte de concurrencia y `D-generacion-sin-movimiento`); y filtro en los selectores de entidades de más de 10 elementos (`D-selectores-sin-busqueda`). El arquitecto recomendaba no arreglar nada antes: la demo mide qué le falta a ella, y cada arreglo exige versión, transporte, instantánea y ensayo nuevos. Consecuencias: la demo correrá sobre la versión que salga de `O-pre-demo`; la condición 1 se rehace sobre ella (preparación, transporte, instantánea y ensayo de las tareas afectadas); la 6 sigue valiendo si esa versión no cambia el esquema; `O-demo-bundle` queda ABIERTO esperando la versión, por decisión y no por bloqueo externo (precedente de dos objetivos abiertos: O-demo con O-diseño, S106).
  (M) S188. Las condiciones 3 y 4 quedan medidas y no cumplidas; la decisión entre salvedad y repetición se aplaza hasta tener el inventario (condición 5), porque un segundo encuentro con la secretaria cubriría las tres (las descargas de T3, la tarea 5 y el inventario).
  (N) S189, del usuario a propuesta del arquitecto. Las condiciones 3 y 4 se aceptan con salvedad y no se repiten. Su propósito, que era verla usar la aplicación y sacar incidencias, se cumplió; lo que no hizo es resultado de la medición. Repetir T5 con quien ya recibió la ayuda del 409 no mide lo mismo, y las descargas pasan el oráculo en el ensayo de S187 sobre la misma versión (y en el de S182 sobre `v0.1.0`). Si alguien sin ayuda completa las descargas y una sustitución lo medirá la prueba del profesor (4), con una persona nueva. Cierra la decisión M. A diferencia de S172 (decisión H de `O-aceptación`), la salvedad es a propuesta del arquitecto.
  (O) S189. Inventario. Fuente: la lista que la secretaria dio directamente al usuario (decisión F), sin paquete; la prematrícula 2025/26 se aportó solo como ayuda y no se usó (usuario); el horario de guardias 2025/2026, no versionado, es la prueba de la fila 9. Criterio: «meter» es por pantalla, porque la prueba del profesor es sin ayuda técnica; «ya está» quiere decir representado y presente en la base de partida; «se puede meter», representable por pantalla y ausente; «no se puede», ese dato no se guarda por pantalla, anotando si su efecto se expresa de otro modo. Las reuniones de tutores y de equipo directivo caen dentro de las seis horas lectivas (usuario). Pruebas en `s189/f1/` (HEAD `fb99e6b`, `src/` idéntico a `v0.2.0`).
- **Terminado cuando** — ESCRITO en S180 sobre esa medición. `O-demo-bundle` termina cuando:
  1. **Preparación en la VM.** En la máquina virtual de S163, con una cuenta estándar nueva, el zip de la Release `v0.2.0` con su sha256 verificado (`be072519…a2a228a7`; hasta S186, `v0.1.0`, `7afc5b8c…9fb170`, decisión L) y una copia de `educhronos-s137.db` (`dfa4c0774a842d8eb6b7a941e23df2a9`) colocada como `%LOCALAPPDATA%\Educhronos\educhronos.db` con la aplicación cerrada y sin `curso-abierto`. Se toma una instantánea antes de la demo. La consigna de la decisión E se escribe antes y se ensaya entera en la VM, y el procedimiento de transporte queda escrito en el acta. **EN CURSO en S181** (`C-preparacion-demo`): cuenta estándar nueva `demo`; zip y base llegados por USB, con su sha256 y su md5 verificados en Windows y sin marca de Internet; instantánea `s181-demo-lista`; consigna y procedimiento de transporte escritos en `docs/actas-aceptacion/acta-demo-bundle.md`. Falta el ensayo. **CUMPLIDA en S182 sobre `v0.1.0`:** ensayo entero de la consigna en la VM desde `s181-demo-lista`, sin aclaraciones ni cambios en la consigna; la condición 3 ensayada en verde; generación 1 de 3; detalle en la sección «Ensayo (F4)» del acta. Por la decisión L se rehará sobre la versión de `O-pre-demo`. **CUMPLIDA en S187 sobre `v0.2.0`** (`C-preparacion-demo-v020`): en la VM, desde `s181-demo-lista`, `v0.1.0` sustituida por `v0.2.0` en la misma ruta (zip `be072519…` verificado en Windows y sin marca de Internet; base `dfa4c077…` sin abrir y sin `curso-abierto`); instantánea `s187-demo-lista`, de la que parte la demo; ensayo entero de la consigna, sin aclaraciones, ayudas ni cambios en la consigna, con la condición 3 ensayada en verde y horario al primer intento con 10 minutos; actualización y pistas revisadas en el acta («Ensayo sobre `v0.2.0` (S187)»).
  2. **Arranque.** «Cursos…» muestra un único curso, sin nombre y activo, y `/horario` muestra su horario. **CUMPLIDA en S181:** en la VM, con `demo`, «Cursos…» lista un único curso, «Sin nombre», activo y abierto ahora, y `/horario/1` muestra el horario del banco (capturas en el acta).
  3. **Consulta de 2025/2026.** Ella ve las vistas y hace las cuatro descargas; los ficheros, traídos a Linux, pasan el oráculo (1285/0/0, 835/0/0, 819/0/0, CSV OK). **MEDIDA en S188, NO CUMPLIDA:** una descarga de cuatro (grupos, oráculo 1285/0/0). **✔ CUMPLIDA en S189 CON SALVEDAD, por decisión del usuario (decisión N):** vio las vistas, con dos ayudas, e hizo una de las cuatro descargas (grupos, 1285/0/0); las otras tres no se hicieron y no se repiten.
  4. **Ejercicio de muestra.** Ella, con la consigna, duplica a 2026/2027, hace los cambios de muestra (una baja, un alta, las horas de una materia y la disponibilidad de dos profesores) y genera. Se anotan el tiempo de generación en la VM y cada pregunta y cada ayuda, según la decisión E. **MEDIDA en S188, NO CUMPLIDA:** T5 sin reasignación (4 actividades borradas, BYG4 sin plazas); duplicado, horas, disponibilidad y generación hechos; registro con las desviaciones del acta. **✔ CUMPLIDA en S189 CON SALVEDAD, por decisión del usuario (decisión N):** duplicado, horas, disponibilidad y generación hechos; la baja y el alta (T5) no se cumplieron, porque se borraron cuatro actividades en vez de reasignarlas, y no se repiten; el hallazgo queda en `D-borrado-sin-control-de-horas`.
  5. **Inventario de huecos.** Después del ejercicio, una lista escrita de lo que ella entrega a la empresa, por tipo de dato, con cada tipo clasificado como «ya está en Educhronos», «se puede meter» o «no se puede». Cada «no se puede» y cada ayuda de la condición 4 se da de alta en §4 por R-incidencia, anotando que sale de `O-demo-bundle`. **PENDIENTE:** no se hizo en S188. **✔ CUMPLIDA en S189** (decisión O): 16 filas, 6 «ya está», 3 «se puede meter» y 7 «no se puede», en el acta. Los siete «no se puede» tienen ficha en §4: `D-matricula-fuera` (filas 2b, 3 y 5), `D-totales-sin-contraste` (8), `D-guardias-sin-modelo` (9), `D-S103-compat` (11, existente) y `D-cargos-sin-modelo` (12); las ayudas de la condición 4 se dieron de alta en S188.
  6. **Base de partida.** Una copia de `educhronos-s137.db` abierta una vez con el bundle `v0.1.0` en Windows, con sus 21 tablas idénticas al banco (0/0) y su md5 citado como base de partida de la prueba (4). Dónde se instala lo decide el objetivo (3). **CUMPLIDA en S181:** abierta una vez por `demo` en la VM y cerrada con «Salir», sin duplicar; 21 tablas con el mismo DDL y 0/0 frente al banco, con control positivo, más `curso` vacía; `integrity_check` ok. md5 `ba0db78259de85c819fe507cbf0c2496`, en `/home/luis/educhronos-aceptacion/s181/base-partida/`.
  7. **Acta.** En `docs/actas-aceptacion/`, el acta de la demo: fecha, asistentes por rol, versión (tag y sha256 del zip), procedimiento de transporte, consigna, tiempos, lo enseñado, el inventario y cada incidencia con su `D-*`, o «ninguna». Sin datos personales: tipos y recuentos, ni nombres ni disponibilidades. **EN CURSO en S188:** acta escrita, falta el inventario. **✔ CUMPLIDA en S189:** inventario añadido al acta.
- **ESTADO en S180: 0 de 7.**
- **ESTADO en S181: 2 de 7** (condiciones 2 y 6); la 1, a falta del ensayo.
- **ESTADO en S182: 3 de 7** (condiciones 1, 2 y 6); la 1 se rehará sobre la versión de `O-pre-demo` (decisión L). `C-preparacion-demo` HECHO.
- **ESTADO en S186: 3 de 7**, objetivo activo del proyecto. Su versión ya existe: `v0.2.0` (`O-pre-demo` TERMINADO en S186), zip con sha256 `be072519…a2a228a7` y sin cambios en `schema.sql` desde `v0.1.0`, así que la condición 6 sigue valiendo; la 1 se rehace sobre `v0.2.0` (decisión L).
- **ESTADO en S187: 3 de 7** (condiciones 1, 2 y 6), la 1 ya sobre `v0.2.0`. `C-preparacion-demo-v020` HECHO. Queda la demo con la secretaria (condiciones 3, 4, 5 y 7), que depende de su fecha (decisión H) y arranca de `s187-demo-lista`.
- **ESTADO en S188: 3 de 7** (condiciones 1, 2 y 6), objetivo activo del proyecto. Lo anterior, como en S187: la 1 sobre `v0.2.0`, la 2 y la 6 de S181. Demo con la secretaria hecha el 03/10/2026 desde `s187-demo-lista` (`C-demo-secretaria` EN CURSO): las condiciones 3 y 4, medidas y no cumplidas (decisión M); la 5, sin hacer; la 7, en curso.
- **ESTADO en S189: 7 de 7, TERMINADO con salvedad** (condiciones 3 y 4 con salvedad, decisión N). Inventario hecho (decisión O), con cuatro huecos nuevos en §4. `C-demo-secretaria` HECHO.
- **Deudas:** `D-esquema-sin-version` y `D-version-invisible` NO bloquean este objetivo (§4, nota de S180) y siguen en el objetivo (3).
- **Fuera del criterio, con motivo:** cargar valores de 2026/2027 (decisión D); escribir una guía de uso o cambiar la interfaz por lo que salga, porque lo decide el objetivo (3) y lo rehará (R-invalidación); instalar en la máquina real (decisión G); resolver las incidencias, que esperan o pasan al objetivo que bloqueen (R-incidencia). Desde la decisión L (S182), tres arreglos de interfaz y generación se adelantan a `O-pre-demo`.
- **Depende de:** `O-ci` (Release `v0.1.0`), `O-curso` (adopción de una base anterior y duplicado), `O-disponibilidad` (tarea de disponibilidad) y la máquina virtual de S163. Desde S182, de `O-pre-demo`, abierto en S183 (la versión de la demo, decisión L).
- **Valor:** las primeras incidencias del uso real y el inventario de lo que la secretaria entrega a la empresa frente a lo que Educhronos representa, que es la medida directa de la dependencia que el proyecto quiere eliminar.
- **Cambios que agrupa:** `C-alcance-demo-bundle` ✔ HECHO (S180): el M0, el M2 y este criterio. `C-preparacion-demo` ✔ HECHO (S182): preparación en la VM, arranque, base de partida y ensayo (condiciones 1, 2 y 6). `C-preparacion-demo-v020` ✔ HECHO (S187): la condición 1 rehecha sobre `v0.2.0` (decisión L): sustitución en la VM, instantánea `s187-demo-lista` y ensayo entero. `C-demo-secretaria` ✔ HECHO (S188 + S189): la demo con la secretaria (condiciones 3, 4, 5 y 7, decisión H); 3 y 4 con salvedad (decisión N); inventario en S189 (decisión O).
- **Métrica de §7:** si pasa de ocho sesiones, esconde dos objetivos.

#### O-pre-demo — "La secretaria elige cuánto espera, ve que se está generando y encuentra lo que busca en las listas." ✔ TERMINADO (S186)
- **Propósito:** los tres arreglos de la decisión L de `O-demo-bundle` (S182), para que la demo no deje una impresión equivocada: elegir el tiempo de cálculo, bloquear la aplicación con una barra de progreso mientras genera y filtrar los selectores de entidades de más de 10 elementos. Produce la versión sobre la que corre la demo. Abierto en S183, intercalado en H5 antes de la demo.
- **Por qué va ahora:** decisión L de `O-demo-bundle` (S182, del usuario): la demo corre sobre la versión que salga de aquí.
- **Medido al abrir (S183, Claude Code, HEAD `01bb851`; material con manifiesto en `/home/luis/educhronos-aceptacion/s183/`):**
  (a) Selectores (lectura). 13 controles eligen entidades y ninguno filtra. Pasan de 10 opciones 9: en `actividad-form.html`, asignatura (100), asignatura de plaza (100), aula fija (44), aulas candidatas (44), profesores (59) y subgrupos (334, todos, sin depender del grupo); grupos del subgrupo en `subgrupo-form.html` (28); tutor en `tutoria-dialogo.html` (59); y la entidad de la vista en `horario-view.html` (28, 59 o 35 según la vista, con horario). Nivel (8) y grupo hermano (máximo 3) no llegan; la vía de reparto y la lista de cursos, sin medir. `catalogo/busqueda.ts` (`coincide`, `normaliza`) filtra las siete listas con un mismo patrón y ningún select.
  (b) Guarda (lectura). `GuardaSoloLectura` es un filtro de servlet sobre `/api/` salvo `/api/cursos`; rechaza por cambio de curso (503), duplicado (403) y archivado (403) con `RechazoCursoDTO(causa, message)`, y no mira `generando`. `EstadoCurso.generando` es un contador que admite dos generaciones a la vez y se libera en un `finally`. Rechazar en la guarda no toca `guardar()`. La vista de horario no muestra esos rechazos: `mensajeGeneracion()` lee `mensaje` y no `message`, y toma todo 503 por presupuesto agotado.
  (c) Tiempo (lectura). El frontend envía `{}`; `maxSegundos` no tiene tope superior y vale 600 por defecto. No hay timeouts configurados: controlador síncrono, Tomcat 11.0.22, cliente Angular sin `timeout`, y el bundle abre el navegador del sistema. El presupuesto tiene dos espejos: `MINUTOS_ANUNCIADOS` y el texto de `confirmar-generacion.html`.
  (d) Curva de soluciones (arnés desechable en Linux, 3 workers, 1200 s, semilla 42, n=3, sobre la base del ensayo de S182, `curso-2026-2027.db`, md5 `cbf65999e1bd02e74f3613e5887660df`). Las tres FEASIBLE. Primera solución a los 243, 30 y 470 s, con objetivo 744, 575 y 776; final, 202, 201 y 195; a los 600 s, 230, 216 y 254. La cota inferior no se mueve de 0 y, con la misma semilla, las tres trayectorias difieren en todo. Parar tras 60 s sin mejora ahorra tiempo en dos de tres con un horario un 8 a 14 % peor; con 180 s o más, ninguna para antes del tope.
  (e) VM `Win11` (3 CPU, `v0.1.0`, misma base, 1200 s): 3 de 3 FEASIBLE, objetivos 249, 222 y 188; una desde la interfaz (Edge, 1203 s, sin corte) y dos por la API (1201 s). En S182, a 600 s, 1 de 3.
- **Decisiones (S183, del usuario a propuesta del arquitecto salvo donde se indica):**
  (M) El usuario elige el tiempo y el solver no se toca. Se descartan parar en la primera solución (horario de 3 a 4 veces peor, en un momento imprevisible) y parar al dejar de mejorar (poco ahorro a cambio de calidad), por la medida (d).
  (N) Del usuario, con la recomendación contraria del arquitecto (máximo de 20 minutos, lo medido de punta a punta). Rango de 10 a 60 minutos con opciones fijas: 10, 20, 30 y 60, con 10 por defecto. Como por encima de 20 minutos no hay medida, el máximo se verifica en la VM (condición 6); si no termina con resultado, el máximo baja al mayor valor verificado y se anota.
  (O) La segunda generación se rechaza en `intentarIniciarGeneracion()`, que es sincronizado, y no en el filtro, para no dejar hueco entre comprobar y marcar; las escrituras, en `GuardaSoloLectura` mientras dure una generación; las lecturas pasan.
  (P) El bloqueo cubre toda la aplicación en la pestaña; la barra avanza con el tiempo transcurrido sobre el elegido. Recargar la página pierde el estado en la interfaz, pero el backend sigue protegiendo: limitación escrita, no se arregla aquí.
  (Q) Un solo componente de filtro, que reutiliza `coincide()` y aparece cuando la lista pasa de 10 opciones; filtrar no cambia la selección.
  (R) El rango es de la interfaz: el backend sigue aceptando cualquier `maxSegundos` mayor que 0, porque los tests y el humo usan 5 s.
- **Terminado cuando** — ESCRITO en S183 sobre esa medición. `O-pre-demo` termina cuando:
  1. **Tiempo elegible.** Al confirmar la generación se elige entre 10, 20, 30 y 60 minutos, con 10 por defecto; el valor viaja como `maxSegundos` y todo texto de espera sale de él, sin ningún «10 minutos» fijo (salda `D-presupuesto-anunciado-espejo`).
  2. **Exclusión en el backend.** Una segunda generación y cualquier escritura durante una generación se rechazan con causa propia, y las lecturas pasan. Con tests, y con la tabla de `EstadoCurso` al día.
  3. **Bloqueo y barra.** Mientras se genera no se puede navegar ni editar; la barra muestra el tiempo transcurrido sobre el total y se cierra con el resultado o con el error.
  4. **Mensajes legibles.** La vista de horario muestra el texto del servidor en los rechazos de la guarda, y sólo dice «se agotó el tiempo» cuando la causa es `PRESUPUESTO_AGOTADO`.
  5. **Filtro.** Los 9 selectores de la medida (a) filtran con `coincide()` cuando pasan de 10 opciones, sin perder la selección de lo que queda oculto. Con tests.
  6. **Versión.** Release nueva publicada por CI, con el bundle de Windows y su sha256 citado, sin cambios en `schema.sql` (así sigue valiendo la condición 6 de `O-demo-bundle`). En la VM, desde la interfaz y con esa versión, una generación con el máximo termina con resultado (decisión N). **CUMPLIDA en S186** (`C-version-pre-demo`): Release definitiva `v0.2.0`, construida por CI desde `e556849` (`bundle` #9, 36739349592), con `Educhronos-win.zip` de 176.080.092 B y sha256 `be07251907a1cbdd0bb9917fb9fb3ca45977d161c89d18d488f363a1a2a228a7`; el jar del zip (`7e243fb5…e4b155b5`) es el de las huellas de los jobs `linux` y `windows`, y `schema.sql` es idéntico al de `v0.1.0`. En la VM, cuenta `demo`, base del ensayo (`cbf65999…`) y Edge, con 60 minutos la barra pasó de «0:08 de 60:00» a «59:26 de 60:00» y el horario 3 salió FEASIBLE, objetivo 181, 819 sesiones, en 3602,690 s. El máximo de la decisión N se mantiene.
- **ESTADO en S183: 0 de 6.**
- **ESTADO en S184: 4 de 6** (condiciones 1 a 4, `C-generacion-controlada`).
- **ESTADO en S185: 5 de 6** (condición 5, `C-filtro-selectores`).
- **ESTADO en S186: 6 de 6. TERMINADO** (condición 6, `C-version-pre-demo`).
- **Fuera del criterio, con motivo:** parar antes del tope (decisión M); las pistas de partida (decisión L de `O-demo-bundle`); la atomicidad del guardado (objetivo 3); recuperar el estado tras recargar (decisión P); el botón de generar habilitado en un curso archivado (`D-generar-en-solo-lectura`; con la condición 4 su mensaje se lee).
- **Deudas:** cierra `D-generacion-sin-movimiento`, `D-selectores-sin-busqueda` y `D-presupuesto-anunciado-espejo`; `D-generacion-sin-exclusion` en su parte de concurrencia (la atomicidad sigue en el objetivo 3); `D-generacion-no-reproducible` en su parte de tiempo (la irreproducibilidad sigue en la prueba del profesor).
- **Depende de:** `O-demo-bundle` (decisión L), `O-ci` (Release) y la máquina virtual de S163.
- **Valor:** que la secretaria vea la aplicación que va a usar: una espera que ella controla y que se ve, y listas en las que encuentra lo que busca.
- **Cambios que agrupa:** `C-alcance-pre-demo` ✔ HECHO (S183): el M0, la medición y este criterio. `C-generacion-controlada` ✔ HECHO (S184): condiciones 1 a 4 (`2c6465d`, `e056918`, `39efbc9`, `2692f62`). `C-filtro-selectores` ✔ HECHO (S185): condición 5 (`630bb71`, `fc25486`, `7c9f6b0`). `C-version-pre-demo` ✔ HECHO (S186): tag `v0.2.0`, Release por CI y generación de 60 minutos verificada en la VM (condición 6).
- **Cierre (S186):** TERMINADO, 6 de 6, en cuatro sesiones (S183–S186) y cuatro Cambios, dentro de la métrica de §7 (cinco). Produce la versión de la demo, `v0.2.0`. Deja dos deudas nuevas: `D-tiempo-elegido-no-se-recuerda` (mejora futura, sin sede) y `D-suspension-durante-generacion` (objetivo 3).
- **Métrica de §7:** si pasa de cinco sesiones, esconde dos objetivos.

**Replanificación de H5 (S190, del usuario a propuesta del arquitecto).** Sustituye el mapa de cinco objetivos de S178. Los nombres son provisionales salvo el primero; cada uno escribe su criterio al abrirse, sobre medición, y es un objetivo propio por la vara de ocho sesiones (§7). Estimación del conjunto: de 25 a 35 sesiones (sin medir).
1. `O-base-tecnica`: esquema versionado, versión visible, rastro de generación y guardado atómico. ABIERTO en S190, TERMINADO en S193.
2. `O-carga-2026`: los PDF de 2026/2027 en una base aparte. Su M2 mide el formato frente a 2025/2026, si sirve la cadena de S115 y S116 (volcado fiel, catálogo derivado y cargador) y si la asignación oficial de aulas se explica con la hoja 1 del Excel de aulas. Si hay volcado, va a `docs/horario-referencia/2026-2027/`. Antes de versionar nada con nombres reales hay que determinar la visibilidad del repo (sin determinar desde S189). **S195:** ABIERTO. La visibilidad la resolvió el usuario (decisión B de su ficha) y la medida de la hoja 1 del Excel pasa a `O-aulas` (decisión C). **S199:** TERMINADO, 5 de 5.
3. `O-datos-centro`: datos que no tocan el solver: totales declarados con aviso en los dos sentidos, y reuniones y cargos. **S200:** ABIERTO. CORREGIDO por medición: toca el solver, solo para admitir una plaza sin aula (decisión E de su ficha); incluye también las funciones y proyectos (S199), los tipos de actividad y el cargo del profesor.
4. `O-aulas`: el sistema elige el aula. Reutiliza `PlazaAulaCandidata` y la elección del solver entre candidatas. Datos: las reglas de la hoja 1 del Excel «Distribución aulas 2026-27 def» (aula de referencia de cada grupo, aulas reservadas a una materia, aulas no disponibles, preferencias y capacidad) y los alumnos por subgrupo, para descartar candidatas por capacidad. Según la secretaria, ella y la empresa solo usaron la hoja 1; aun así, los alumnos entran porque descartan aulas de forma más fiable que a ojo (usuario). Sin medir: si la interfaz deja meter candidatas. Va antes de las guardias porque sin aulas no se puede hacer 2026/2027, y las guardias tienen el rodeo de «No puede». **S195:** su M2 mide primero si la hoja 1 explica la asignación oficial de aulas, sobre el volcado de `O-carga-2026` (decisión C de su ficha). **S198:** las 32 aulas del catálogo de 2026/2027 llevan el tipo ORDINARIA sin fuente (decisión `aulas-tipo-neutro`). El solver no lee el tipo, pero la validación I3 (`ActividadService.java:415-425`) lo usa en cuanto una asignatura tiene compatibilidades de aula: este objetivo fija los tipos antes de declararlas, o esas plazas darán 400.
5. `O-guardias`: el generador reparte las guardias (regla en `D-guardias-sin-modelo`).
6. `O-interfaz`: lista cerrada: `D-actividad-ux`, `D-entidad-sin-actividades`, `D-error-poco-visible`, `D-vista-horario-no-se-descubre`, `D-tiempo-generacion-poco-claro` y `D-filtro-por-codigo`. **S201:** se añade `D-incoherencia-como-404`. Va después de los datos porque los formularios que cambian con ellos no se pulen antes (R-invalidación).
7. `O-prueba-secretaria`: la prueba sin guion ni ayuda técnica en la VM `Win11`, desde una instantánea, con una cuenta estándar nueva y una instalación por procedimiento escrito (base de partida, acceso directo).
8. `O-comparacion`: el horario de la secretaria contra el oficial de 2026/2027, con métricas fijadas antes de mirar. Es el más prescindible del mapa.

La carga (2) se puede intercalar en cuanto termine el (1). Las sedes de §4 se reasignaron en S190 con estos nombres.

#### O-base-tecnica — "Cada versión adapta las bases anteriores, dice qué versión es y deja rastro de lo que hace." ✔ TERMINADO (S193)
- **Propósito:** H5 va a cambiar el esquema varias veces y habrá incidencias de la secretaria que diagnosticar. Antes de eso, la aplicación tiene que adaptar una base antigua sin perderla, decir qué versión es y registrar cada generación.
- **Por qué va ahora:** es el primero del mapa de S190. Todo objetivo posterior que añada datos cambia el esquema.
- **Medido al abrir:** M2 de solo lectura de S190 (HEAD `087808d`; fuera de docs/, idéntico a `v0.2.0`; material en `s190/f1/`). Sin medir y a resolver en sus Cambios: si Boot 4.1 permite colocar un paso propio entre el inicializador de scripts y JPA; el formato de `--app-version`; VACUUM INTO con la librería de la aplicación. **S191:** el primero no hacía falta, porque el paso propio sustituye al inicializador de Boot (decisión H); `VACUUM INTO` conserva `user_version` con sqlite-jdbc 3.53.2.0 (sonda de F1). El formato de `--app-version` queda para `C-version-y-rastro`. **S192:** el formato de `--app-version` no se resuelve: es opcional por el criterio y queda fuera (decisión J).
- **Decisiones:**
  (A) S190. Migraciones propias, no solo rechazar: de los cambios de `schema.sql`, `6a3a3f9` y `d301b64` no consistieron en añadir una tabla, y con `create table if not exists` no llegan a una base que ya existe. Flyway sigue descartado.
  (B) S190. La versión, del tag, en la interfaz y en el log; hoy el pom dice `0.1.0-SNAPSHOT` también en `v0.2.0`.
  (C) S190. Rastro en el log de cada generación; hoy ni un 503 ni un 422 dejan nada.
  (D) S190. Guardado en una transacción real: los `@Transactional` de `guardar()` y `cargarProblema()` no se aplican porque se llaman sobre `this`.
  (E) y (F) de S190 (incidencias de la demo e instalación por procedimiento) se aceptaron y pasan a `O-datos-centro`, `O-interfaz` y `O-prueba-secretaria` (bloque de replanificación).
  (G) S191, del usuario a propuesta del arquitecto. `schema.sql` sigue siendo el esquema vigente completo y solo se ejecuta sobre una base vacía. Una base con `user_version` 0 y tablas es la versión 1: se le aplica `esquema/001.sql`, copia congelada de las sentencias del `schema.sql` de S191, y desde ahí las migraciones `esquema/NNN.sql`, cada una en una transacción con su sello. Así los `@DataJpaTest` y los helpers que leen `schema.sql` no cambian, y un test de equivalencia impide olvidar una migración. Descartado congelar `schema.sql` y llevarlo todo por migraciones, porque obligaría a los slices a correr la cadena.
  (H) S191, del usuario a propuesta del arquitecto. La versión se comprueba antes de cualquier script, no «entre el script y JPA» como planteaba S190: si el script corre primero, sus `create table if not exists` pueden escribir en una base posterior. Al arrancar, `InicializadorEsquema` sustituye al inicializador de Boot, que se retira por `@ConditionalOnMissingBean(ApplicationScriptDatabaseInitializer)`, y JPA espera a todo `DataSourceScriptDatabaseInitializer` (medido sobre el bytecode de 4.1.0). Una base posterior se rechaza con 409 `CURSO_VERSION_POSTERIOR` al cambiar de curso, y con su motivo en el diálogo de arranque (`FalloArranque`).
  (I) S192, del usuario a propuesta del arquitecto. La llamada interna a `guardar()` va dentro de una `TransactionTemplate` (`eddad66`): una llamada sobre `this` no atraviesa el proxy, así que su `@Transactional` no se aplicaba. `guardar()` conserva la anotación para quien lo llama desde fuera. Descartado moverlo a otro bean: el mismo efecto con más cambio. El test entra por `POST /api/horarios`, el camino de producción, porque fuera de una petición `cargarProblema()` no tiene sesión (ver «Fuera del criterio»).
  (J) S192, del usuario a propuesta del arquitecto. La versión es `${revision}` en `pom.xml`, con `0.0.0-dev` por defecto; desde un tag `v*`, `bundle.yml` pasa el nombre sin la «v» a `empaquetar-linux.sh --version`, que se la da a Maven como `-Drevision`. El objetivo `build-info` del plugin de Spring Boot la mete en el jar; `VersionEnvironmentPostProcessor` la publica como `educhronos.version` («desconocida» si falta) y escribe «Educhronos versión X» por el log diferido, antes de que arranque el contexto, así que consta también si el arranque falla; `GET /api/version` la expone. `flatten-maven-plugin` 1.8.0 (`resolveCiFriendliesOnly`) instala los poms con la versión resuelta: sin él, `-pl app` sin `-am` (CI y Playwright) no resolvía el padre de `solver` (medido en S192). Descartados pasar `-Drevision` en cada orden y volver a una versión fija en el pom con la del tag aparte. `--app-version` queda fuera. Commit `94373c2`.
  (K) S192, del usuario a propuesta del arquitecto. Rastro en `GeneradorHorarioService`, en líneas `clave=valor`: al admitir la generación, «Generación iniciada» con versión y presupuesto; al terminar, «Generación terminada» con `desenlace` OK (INFO: horario, estado, objetivo, sesiones y `duracionMs`), SIN_HORARIO (WARN: estado del solver; cubre el 503 y el 422 del solver), PREVALIDACION (WARN: motivo) o EXCEPCION (ERROR, con la traza). Se registra el estado del solver y no el código HTTP, que traduce `MapeoFalloSolver` en `web/`. El número de sesiones sale de la lista que se guarda, no de `horario.getSesiones()` (`D-post-horario-sin-sesiones`). Fuera: la semilla (S183 midió que con la misma semilla las trayectorias difieren en todo) y el 409 de generación en curso, que no llega a empezar. Commit `e83355e`.
  (L) S193, del usuario; el ajuste del relleno, a propuesta del arquitecto. La versión va en la barra, debajo de la marca: «versión X» tal como llega de `GET /api/version`, o «versión no disponible» si la petición falla, con una sola petición al cargar; si cae en el 503 de cambio de curso, «no disponible» hasta recargar (limitación aceptada). La barra no crece (criterio 4 de O-navegación): el grupo de marca y versión mide 30 px con interlineado 1, la letra pequeña va en el propio componente y el relleno vertical de la barra baja de 12 a 10,5 px, 10,5 + 30 + 10,5 = 51 (comentario en `app.css`). Descartados un pie, que resta el mismo alto, y la versión en la misma fila, que el usuario rechazó sobre capturas. Commit `1bf9855`.
  (M) S193, del usuario a propuesta del arquitecto. Tag `v0.3.0` definitivo y no `-rc`: es la primera versión que sella `user_version`, de la que migrarán las siguientes; `bundle.yml` solo marca prerelease si el tag lleva guion.
  (N) S193, del usuario a propuesta del arquitecto. Verificación en la VM `Win11` desde `s187-demo-lista`, sustituyendo `v0.2.0` por `v0.3.0` en la misma ruta (procedimiento de S187): el primer arranque abre la base `dfa4c077…`, que es el camino de actualización de la secretaria. El 503 se provoca sobre esa base con `maxSegundos=1`, y no sobre el banco del centro completo que proponía S192: las dos son el centro real, y lo que se mide es la línea del log.
- **Terminado cuando** — ESCRITO en S190 sobre esa medición. `O-base-tecnica` termina cuando:
  1. **Esquema versionado.** Al arrancar y al cambiar de curso, antes de que JPA use la base: `user_version` 0 se adopta como el esquema actual y se sella; un número menor se migra con scripts SQL numerados; un número mayor se rechaza con un mensaje claro y sin tocar la base. El duplicado conserva el número, medido con la librería de la aplicación. Con tests, entre ellos una migración de prueba. **PRECISADO en S191 (decisión G):** «0 se adopta como el esquema actual» solo vale mientras la versión es 1; la regla es que 0 es la versión 1 y desde ahí se migra. **✔ CUMPLIDA en S191** (`C-esquema-versionado`, `b57d4bc`): al arrancar (`InicializadorEsquema`) y al cambiar de curso (`FabricaDeBases`), versión 1; una base posterior se rechaza sin cambiar su md5, en los dos caminos; migración de prueba de 1 a 2, y una migración rota no deja nada; el duplicado conserva la versión (sonda de F1 y test). 16 tests; 7 mutantes, los 7 caen.
  2. **Versión visible.** La versión sale del tag y se ve siempre en la interfaz, en una línea fija con su número (petición del usuario, S190). Se escribe en el log al arrancar. Se verifica sobre un bundle construido por el workflow desde un tag. `--app-version` en el exe es opcional. **EN CURSO en S192** (decisión J, `94373c2`): la versión sale del tag, llega al jar y se escribe en el log al arrancar; medido con `-Drevision=9.9.9-prueba` sobre el jar arrancado, en el log y en `GET /api/version`. Faltan la línea fija en la interfaz y la verificación sobre un bundle construido desde un tag. **✔ CUMPLIDA en S193** (`C-version-y-rastro`, `1bf9855`, decisiones L, M y N): «versión X» debajo de la marca, con la barra en sus 51 px y un test de presencia; Release `v0.3.0` construida por CI desde su tag (`bundle` #10), zip `1b75eafd…0a93`, jar con `build.version=0.3.0`; en la VM `Win11`, con cuenta estándar, la barra dice «versión 0.3.0» (captura) y el log «Educhronos versión 0.3.0» al arrancar. La misma corrida mide el 503 por la vía real que la decisión K dejó sin test: «Generación terminada desenlace=SIN_HORARIO estado=UNKNOWN» con `maxSegundos=1`.
  3. **Rastro de cada generación.** El log registra al empezar la versión y el presupuesto, y al terminar el estado, el objetivo, la duración y el número de sesiones, o la causa en los desenlaces 503, 422 y excepción. Con tests. **✔ CUMPLIDA en S192** (`C-version-y-rastro`, `e83355e`, decisión K): líneas de inicio y de fin en los cuatro desenlaces, con tests sobre el log capturado (`OutputCaptureExtension`); cuatro mutantes, los cuatro caen. El 503 comparte rama con el 422 del solver y no tiene test por la vía real: se mide en la verificación sobre el bundle.
  4. **Guardado atómico.** Un fallo durante `guardar()` no deja ninguna cabecera nueva, y el horario vigente sigue siendo el anterior. Con un test que inyecta el fallo. **✔ CUMPLIDA en S192** (`C-version-y-rastro`, `eddad66`, decisión I): `GuardadoAtomicoGeneracionTest` hace fallar `saveAll` por `POST /api/horarios` y comprueba que no queda cabecera nueva y que el vigente es el anterior; rojo antes del arreglo (2 filas en vez de 1) y dos mutantes que caen. Mide el punto 9 que S190 dejó sin medir.
- **ESTADO en S190:** —
- **ESTADO en S191:** 1 de 4 (condición 1, `C-esquema-versionado` HECHO).
- **ESTADO en S192:** 3 de 4 (condiciones 1, 3 y 4); la 2, a medias. `C-version-y-rastro` EN CURSO.
- **ESTADO en S193:** 4 de 4. TERMINADO (condición 2, `C-version-y-rastro` HECHO).
- **Fuera del criterio, con motivo:** una migración que reconstruya una tabla necesita `foreign_keys` apagado, y en SQLite ese pragma no se cambia dentro de una transacción: lo resolverá el primer objetivo que la necesite (aviso en el javadoc de `PreparadorEsquema`, S191). Abrir los bancos reales con la versión nueva: el test usa una base construida con `schema.sql`, cuyo DDL es el de los bancos según S180 (a). **S192:** `cargarProblema()` tiene el mismo caso que `guardar()`: su `@Transactional(readOnly = true)` no se aplica porque se llama sobre `this`, y solo funciona porque open-in-view abre la sesión en cada petición (punto 7 de lo que S190 dejó sin medir; en S192, un test sin transacción dio `LazyInitializationException`). No bloquea ninguna condición; por eso apagar open-in-view, el arreglo natural de `D-post-horario-sin-sesiones`, rompería hoy la generación. Y el 503 por la vía real, sin test propio (decisión K). **S193:** el rastro del esquema al abrir una base; la condición 1 no lo pide (`D-migracion-sin-rastro`).
- **Deudas:** cierra `D-esquema-sin-version`, `D-version-invisible` y `D-generacion-sin-rastro`, y `D-generacion-sin-exclusion` en su parte de atomicidad. **S191:** `D-esquema-sin-version` CERRADA. **S192:** `D-generacion-sin-rastro` y `D-generacion-sin-exclusion` CERRADAS; `D-version-invisible`, a medias con la condición 2. **S193:** `D-version-invisible` CERRADA. Nace `D-migracion-sin-rastro`.
- **Depende de:** —
- **Valor:** que cuando la secretaria diga «no me sale» o «se ha roto», se sepa con qué versión le pasó y qué ocurrió en esa generación; que un fallo al guardar no deje como vigente un horario vacío; y que una base de un curso anterior se abra con una versión nueva sin perderse (S192).
- **Cambios que agrupa:** `C-alcance-base-tecnica` ✔ HECHO (S190): la apertura; `C-esquema-versionado` ✔ HECHO (S191): condición 1 (`b57d4bc`); `C-version-y-rastro` ✔ HECHO (S192 + S193): condición 4 (`eddad66`), condición 3 (`e83355e`) y la 2 (`94373c2`, `1bf9855` y la verificación de `v0.3.0` en la VM).
- **Cierre (S193):** TERMINADO, 4 de 4, en cuatro sesiones (S190–S193) y tres Cambios, una más que la estimación de 3 y dentro del límite de 8. Produce `v0.3.0`, la primera versión que sella el esquema, dice qué versión es y deja rastro de cada generación. Deja `D-migracion-sin-rastro` (sede `O-datos-centro`).
- **Métrica de §7:** estimación de 3 sesiones; límite de 8 (§7). **S192:** tres sesiones consumidas (S190 a S192), las de la estimación, con la condición 2 pendiente; límite de 8. **S193:** cuatro sesiones (S190 a S193), una más que la estimación; límite 8.

#### O-carga-2026 — "El horario oficial de 2026/2027 entra en Educhronos, en una base aparte que la secretaria no ve."
- **Propósito:** llevar el horario oficial de 2026/2027 del centro a Educhronos por la cadena de S115 y S116 (volcado fiel, catálogo derivado y cargador), en una base que no es la de la secretaria. Sirve para contrastar el curso que ella configure (`O-prueba-secretaria`) y para la comparación final (`O-comparacion`), y su volcado es la entrada del M2 de `O-aulas`. Segundo objetivo del mapa de S190, abierto en S195.
- **Por qué va ahora:** es el (2) del mapa de S190 y `O-base-tecnica` está terminado. Los PDF llegaron en S190, y el centro entregó versiones posteriores hasta el 30/09.
- **Medido al abrir (S195, Claude Code, HEAD `eccc875`, solo lectura; material con manifiesto en `/home/luis/educhronos-aceptacion/s195/`, carpetas `f1b`, `f1c` y `f2`):**
  (a) Material (`f1b`). 24 ficheros en `docs_extra/Ejemplos_SJ/2026-2027/`: los 11 de S190, todos presentes, y 13 nuevos, en «Horarios 24 sept 26/» y sus subcarpetas `Papelera/` y `Retoques I love pdf/`. Los 20 PDF tienen capa de texto. Los del programa del centro (Virtual Print Engine) son del 24 y el 25/09; los retoques hechos en Word, del 28 al 30/09, cambian solo aulas y no se aplicaron a todas las vistas. No se entregó el horario completo por aulas.
  (b) Versiones (`f1b`). Grupos: P05 (24/09) y P04 (25/09), que reimprime solo cinco grupos de 4º ESO. Profesores: P13, P07 (25/09) y el retoque P20 (30/09). Desde el 25/09 desaparece la EXPRE en paralelo en C01 de 4ºBDi y 4ºCDi, igual en grupos y en profesores. Por recuento de códigos, los retoques de profesores cambian B12 por A12 en 14 sesiones y TALL1 por A4 y A5 en 4; la vista de grupos no lo recoge.
  (c) Cadena (`f2`, que comprueba la pasada de origen desconocido de `s195/f1`: 16 de 20 afirmaciones confirmadas, una en parte y tres no comprobables). El extractor de S115 no existe ni ha existido en el repo; su método está en `RESUMEN-EXTRACCION.md`. El catálogo derivado no tiene productor: entró a mano (`894cf1e`). `cargar-centro.py` existe y sus 19 rutas están en los controladores; no carga aulas compatibles ni restricciones. `REGLAS_CODIGO` (`verificar-conservacion.py`) casa 24 de los 30 títulos de grupo de P05: faltan los seis de 1ºBACH con modalidad. `OFICIALES` y las rutas fijas de `extraer-nombres.py` y `cargar-centro.py` son de 2025/2026.
  (d) Formato (`f2`). Mismo productor que en 2025/2026. Paso de columnas 69,05 frente a 72,00, constante dentro de cada fichero. Las mismas etiquetas de hora, salvo P04, que no imprime la franja del recreo. Leyendas por página en grupos (30 de 32 en P05; las otras dos son continuación). P07 trae solo leyenda de asignaturas: sus grupos y aulas se clasifican con otra fuente o por la forma del código, comprobado en 3 de sus 62 páginas.
- **Decisiones (S195, del usuario a propuesta del arquitecto salvo donde se indica):**
  (A) Referencia (sobre `f1b`). La base es la salida del programa del centro; cada entidad se toma de su impresión más reciente; los retoques en Word se tratan como un parche de aulas escrito sesión a sesión, no como fuente. Grupos: P05 con los cinco grupos de P04; profesores: P07 más el parche. Composición y copias, con manifiesto, en `docs_extra/Ejemplos_SJ/2026-2027-referencia/REFERENCIA.md` (`f1c`); los originales no se tocan. El parche se confirma con la secretaria sin bloquear el objetivo.
  (B) Del usuario. Los horarios del centro son públicos, así que los datos de 2026/2027 siguen el tratamiento de los de 2025/2026: los PDF del programa de la referencia y los volcados se versionan en `docs/horario-referencia/2026-2027/`, con el repo público. A propuesta del arquitecto: no se versionan los retoques en Word ni el Excel de aulas, que son documentos internos; del parche se versionan solo los cambios derivados (sesión, aula anterior y aula nueva). La razón descansa en la afirmación del usuario. Cierra `D-nombres-reales-en-repo` (§4).
  (C) La pregunta del mapa de S190 —si la hoja 1 del Excel de aulas explica la asignación oficial— sale de este objetivo: necesita el volcado y es materia de `O-aulas`, cuyo M2 la mide primero.
  (D) El catálogo se deriva con un script versionado, no a mano: el centro sacó tres versiones en una semana, y otra obligaría a rehacerlo.
  (E) Sin horario completo por aulas, la reconciliación de S115 (grupos con aulas) se hace entre grupos y profesores.
- **Terminado cuando** — ESCRITO en S195 sobre esa medición. `O-carga-2026` termina cuando:
  1. **Volcado de grupos.** Un extractor versionado vuelca la vista de grupos de la referencia (P05, con los cinco grupos de P04) a `docs/horario-referencia/2026-2027/`, en el esquema de S115: columnas leídas de la cabecera de cada página, tramos por el valor de la etiqueta de hora y páginas de continuación unidas a la anterior. Cuadre de tokens con 0 perdidos y 0 duplicados, y tests sobre páginas conocidas. **✔ CUMPLIDA en S196** (`C-volcado-grupos`: `tools/carga-centro/extraer-horario.py` con 12 tests y el volcado de 30 grupos y 1421 celdas, 0 tokens perdidos y 0 duplicados, en `ad8ef83`).
  2. **Volcado de profesores y cruce.** El extractor vuelca P07, y el cruce entre grupos y profesores, sesión a sesión, lista cada discrepancia. El parche de aulas (P20 frente a P07) queda escrito sesión a sesión, con la confirmación de la secretaria anotada o pendiente. **✔ CUMPLIDA en S197** (`C-volcado-profesores`: modo `profesores` de `tools/carga-centro/extraer-horario.py` y volcado de los 62 profesores; cruce con 1421 claves por lado, 0 discrepancias, en `INFORME-CRUCE-GRUPOS-PROFESORES.md`; `parche-aulas.json` con 18 cambios y la confirmación pendiente; 44 tests; de `a591f16` a `92c1c51`).
  3. **Catálogo derivado.** Un script versionado deriva el catálogo 2026/2027 de los volcados según `ESPECIFICACION-CATALOGO.md`, con regla de código para todos los grupos. La conservación de la carga se comprueba clave a clave, y cada divergencia queda explicada. **✔ CUMPLIDA en S198** (`C-catalogo-2026`: `tools/carga-centro/derivar-catalogo.py` reproduce byte a byte el catálogo de 2025/2026 hecho a mano y deriva el de 2026/2027 con 1421 claves por lado y 0 divergencias, con el parche de aulas aplicado en 12 plazas; lo que el volcado no contiene va en `decisiones-catalogo.json` de cada curso, cinco entradas «sin fuente» en 2026/2027; `extraer-nombres.py` por argumentos; especificación al día; 94 tests; de `50d6a47` a `765b50c`).
  4. **Base aparte.** Desde base vacía, `cargar-centro.py`, con las rutas de 2026/2027, puebla una base fuera de git y fuera de la VM; todas las familias cuadran por GET, y una generación produce horario sin violaciones duras. md5 citado. **✔ CUMPLIDA en S199** (`C-carga-2026`: `cargar-centro.py` con catálogo y nombres por argumento y las escrituras previstas calculadas del catálogo; `verificar-conservacion.py` con volcados por argumento y las reglas de `codigos_grupo.py`; 101 tests. Base `/home/luis/educhronos-aceptacion/s199/f2/datos/curso-2026-2027-oficial.db` desde fichero inexistente, con un jar construido desde `ae1f2fa`: 852 de 852 escrituras; diez familias iguales por GET, comprobadas con un script independiente del cargador; generación de 600 s FEASIBLE con 796 sesiones y 0 violaciones; conservación con 1421 = 1421 y 0 divergencias; md5 `f6c569472c4452ad0be3d7be9ea3318d`; de `132c791` a `24bec92`).
  5. **Lo que no se expresa.** Una lista de lo que el horario oficial contiene y el modelo no representa, cada entrada con su `D-*`, existente o nueva por R-incidencia, sin forzarlo en el catálogo. **✔ CUMPLIDA en S199** (`docs/horario-referencia/2026-2027/INFORME-NO-REPRESENTABLE.md`: guardias, reuniones, funciones y proyectos, y el co-tutor de las PDC, con `D-guardias-sin-modelo`, `D-cargos-sin-modelo` y `D-cargador-tutoria-pisa`; ninguna ficha nueva; lo que el PDF no trae, remitido a `decisiones-catalogo.json`; `2b559ad` y `15d3a8c`).
- **ESTADO en S195: 0 de 5.** **S196: 1 de 5** (condición 1). **S197: 2 de 5** (condición 2). **S198: 3 de 5** (condición 3). **S199: 5 de 5, TERMINADO** (condiciones 4 y 5).
- **Fuera del criterio, con motivo:** si la hoja 1 explica la asignación oficial de aulas (decisión C); cargar aulas compatibles, restricciones, totales, reuniones o cargos, que son de los objetivos siguientes (R-invalidación); el horario completo por aulas, que no se entregó; y cargar el horario oficial como horario de la base: la comparación se hace contra el volcado, y esa vía no se ha medido.
- **Para las condiciones 3 y 5 (S197):** `tools/carga-centro/codigos_grupo.py` da la forma corta de los 30 grupos de 2026/2027 (las cinco reglas de `REGLAS_CODIGO` y la de 1ºBACH: letra e inicial de la modalidad en minúscula); `verificar-conservacion.py` sigue con las cinco de 2025/2026, y si pasa a importar el módulo lo decide la condición 3. El informe del cruce reúne lo que la condición 5 tiene que revisar: 194 celdas de actividades sin grupo en 12 códigos (guardias, `G` y `Gbibl`; reuniones, `RED` y `conv`; funciones y proyectos, `ORYCA`, `RT12`, `RT34`, `FOREI`, `APSTE`, `REYR`, `PROAR` y `HUERT`; la leyenda no dice qué son `RT12` y `RT34`), 38 de guardia de recreo y tres páginas de profesor sin código. `GrecB` ocupa el sitio del aula en las guardias de recreo de biblioteca y no tiene entrada en la leyenda. **S198:** `verificar-conservacion.py` no se toca en la condición 3: compara una base con el PDF (exige `--db`), así que su adaptación es de la condición 4. Candidatos a la condición 5 medidos al derivar, en `/home/luis/educhronos-aceptacion/s198/f2/candidatos-condicion-5.md`: 197 celdas sin grupo en 12 códigos, 38 guardias de recreo (`GrecB` es el aula de las 5 de biblioteca), 3 profesores sin código, los tipos de aula, y los tutores de Bachillerato y los `requiereTutor` sin fuente. **S199:** `verificar-conservacion.py` importa las reglas de `codigos_grupo.py` (condición 4).
- **Deudas:** cierra `D-nombres-reales-en-repo` (decisión B). Nace `D-codigo-profesor-no-es-identidad` (S198, sede `O-comparacion`). **S199:** `D-cargos-sin-modelo` amplía su alcance a funciones y proyectos y `D-cargador-tutoria-pisa` pasa a `O-comparacion` (decisiones del usuario); notas en `D-guardias-sin-modelo` y `D-censo-profesores-80-59`.
- **Depende de:** `O-base-tecnica` (la base aparte nace con el esquema sellado) y el material del centro en `docs_extra/`.
- **Valor:** el horario oficial de 2026/2027 dentro de Educhronos, con el que contrastar lo que haga la secretaria y medir qué no se puede expresar.
- **Cambios que agrupa:** `C-alcance-carga-2026` ✔ HECHO (S195): el M0, la medición y este criterio. `C-volcado-grupos` ✔ HECHO (S196): condición 1 (`edf6287`, `63d96d3` y `ad8ef83`). `C-volcado-profesores` ✔ HECHO (S197): condición 2 (`a591f16`, `7513956`, `c4e9b00`, `ccef5d7` y `92c1c51`). `C-catalogo-2026` ✔ HECHO (S198): condición 3 (`50d6a47`, `70d9b2e`, `bf6c9d1` y `765b50c`). `C-carga-2026` ✔ HECHO (S199): condiciones 4 y 5 (`132c791`, `ae1f2fa`, `24bec92`, `2b559ad`, `15d3a8c` y `6ae2fa4`).
- **Métrica de §7:** estimación de 5 a 7 sesiones; límite de 8. Si al cerrar la condición 2 se ve que se pasa, se parte en dos objetivos. **S196:** dos sesiones (S195 y S196); límite 8. **S197:** tres sesiones (S195 a S197); límite 8. Al cerrar la condición 2 no se parte (decisión del usuario a propuesta del arquitecto): quedan dos Cambios para las condiciones 3 a 5, estimados en 2 a 4 sesiones, dentro de la estimación; la condición 4 es la menos medida (generación sobre un catálogo nuevo). **S198:** cuatro sesiones (S195 a S198); límite 8. Queda `C-carga-2026` (condiciones 4 y 5), estimado en 1 a 3 sesiones. **S199:** cinco sesiones (S195 a S199), dentro de la estimación; `C-carga-2026` en una, con un tope de dos fijado en el M0 (decisión del usuario).

#### O-datos-centro — "La secretaria declara los totales del centro y ve si cuadran, y mete reuniones, funciones y cargos sin rodeos."
- **Propósito:** que lo que el centro sabe de sí mismo —las horas de cada profesor y de cada grupo, las reuniones, las funciones y proyectos y los cargos— entre en Educhronos como dato, y que un descuadre se vea antes de generar. Tercer objetivo del mapa de S190, abierto en S200.
- **Por qué va ahora:** `O-carga-2026` terminó en S199 y este es el siguiente del mapa. Va antes de `O-aulas` y de `O-interfaz` porque cambia el editor de Actividad y los formularios de profesor y de grupo (R-invalidación).
- **Medido al abrir (S200, Claude Code, HEAD `a118f0c`, solo lectura; material en `/home/luis/educhronos-aceptacion/s200/m2/`, 23 ficheros, manifiesto `bf5a2093…`; el anexo de `informe.md`, líneas 360 a 508, es lectura sin ejecutar):**
  (a) Totales. Ni los tres PDF de la referencia (`docs/horario-referencia/2026-2027/pdf/`) ni el Excel del centro (por los nombres de sus tres hojas; las celdas no se abrieron) traen totales por profesor ni por grupo, ni cargos. No se abrieron «Horarios de guardias», «Profesores comunes» ni los compactos. Los totales los teclea la secretaria; el oráculo son las horas del volcado oficial de 2026/2027.
  (b) Avisos. Las horas solo viven en `actividad` (duración y repeticiones), y `actividad` no tiene nombre, solo código (`schema.sql:37`). La prevalidación tiene cuatro ERROR y un AVISO. Una plaza sin subgrupos suma horas a sus profesores y a ningún grupo. El diálogo de generar se abre siempre, pero solo recibe los ERROR y los presenta como motivo de que el servidor rechazará la generación (`confirmar-generacion.html:42`): ningún AVISO llega a él. La prevalidación solo se ve en la vista de horario.
  (c) Plaza sin subgrupos, asignatura y aula. Una plaza sin subgrupos la aceptan el servicio, el formulario y el solver, que la deja fuera de los no-solapes de subgrupo y de grupo. Por lectura, sin ejecutar: se ve en las vistas y PDF de profesor y de aula y en el CSV, y no en las de grupo; con tutor, da el AVISO `TUTORIA_SIN_TUTOR` con recurso nulo. La asignatura de la plaza es obligatoria en el esquema, el servicio y el solver (`schema.sql:46`, `ActividadService.java:466-469`, `solver/domain/Plaza.java:25`), y de ella sacan el nombre las vistas, los PDF y el CSV. El aula de la plaza admite nulos en el esquema (`plaza.aula_fija_id`, `schema.sql:46`; las candidatas, en tabla de enlace); la exigen el servicio (`ActividadService.java:276-287`) y el solver (`solver/domain/Plaza.java:40-41`). La de la sesión es NOT NULL (`schema.sql:53`, `persistence/Sesion.java:53-54`). Nueve puntos en ocho ficheros leen el aula de la sesión sin proteger el nulo: `GeneradorHorarioService.java:508`, `MovimientoInstanciaService.java:478`, `SolucionMapper.java:257`, `HorarioCsv.java:73` (con `:103`), `VistaPdf.java:131` (profesor) y `:70` (grupo, solo con subgrupos), `proyeccion.ts:21`, `horario-grid.ts:307` y `horario-grid.html:147`; el PDF de aula sí descarta el nulo (`VistaPdf.java:182`). Un pin no necesita horario, pero la interfaz solo lo crea desde una sesión pintada.
  (d) Reuniones y funciones de 2026/2027. RED (7 celdas), RT12 (11) y RT34 (9) son reuniones conjuntas: cada una en un solo tramo, con todos sus profesores. ORYCA (10 celdas, 5 profesores) y APSTE, FOREI, HUERT, PROAR y REYR (2 cada una, 1 profesor) son horas sueltas de cada profesor. Ninguna imprime aula ni grupo. La leyenda del PDF de profesores explica 12 de los 14 códigos de la sección A de `INFORME-NO-REPRESENTABLE.md` (10 de los 12 sin grupo y los dos de recreo, `GR` y `GRBib`); `RT12` y `RT34` solo repiten su código.
  (e) Esquema. Solo existe la migración `001`. Ni `PreparadorEsquema` ni `InicializadorEsquema` escriben en el log, y las líneas que rechazan una base posterior al abrir un curso tampoco (`CursoService.java:321-329`, `CursoController.java:111-115`). El duplicado copia el fichero entero (`VACUUM INTO`), así que lleva lo nuevo sin tocar su código.
- **Decisiones (S200, del usuario a propuesta del arquitecto salvo donde se indica):**
  (A) Totales declarados, opcionales: horas de clase por profesor y horas semanales por grupo. Sin declarar no hay aviso; el aviso salta en los dos sentidos.
  (B) Las guardias, fuera: nada de lo configurado las cuenta, y su modelo es de `O-guardias`.
  (C) El aviso se ve en las listas de profesores y de grupos («14 de 18») y llega al diálogo de generar. El borrado no lleva aviso propio.
  (D) Del usuario, con un ajuste del arquitecto. La actividad lleva tipo: CLASE, REUNION o FUNCION (función o proyecto). Las que no son CLASE no tienen alumnos: pueden no tener subgrupos, no cuentan como horas de clase y no piden tutor; conservan la asignatura de su plaza, que les da el nombre, como hace la leyenda del programa del centro (RED, «Reunión de Equipo Direct…»). Una CLASE sin subgrupos da 400. El tipo se elige en el editor y se ve en la lista de Actividades. Ajuste del arquitecto: no hay tipo GUARDIA, porque la regla de S190 —el generador reparte las guardias según el número de cada profesor— no casa con una plaza de profesores fijos, y su modelo es de `O-guardias` (R-invalidación). Rectificada dos veces en S200: la primera propuesta dejaba la actividad sin asignatura, lo que obligaba a reconstruir `plaza` y a cambiar el solver; la segunda tenía un solo tipo sin alumnos, y el usuario pidió distinguirlos para editar mejor.
  (E) Del usuario. Una actividad que no es CLASE puede no tener aula: así son las reuniones y las funciones del horario oficial. El aula es opcional, no prohibida (la guardia de biblioteca tiene aula). Se mantiene sabiendo su coste (lectura de S200): reconstruir `sesion` con `foreign_keys` apagado, cambiar el solver (`solver/domain/Plaza.java:40-41` y la asignación de aula del modelo) y proteger el nulo en los nueve puntos de (c). **CORREGIDO en S201 (M2 y sonda):** ninguna tabla apunta a `sesion`, así que se reconstruye dentro de la transacción de la migración, con las claves foráneas encendidas, renombrando primero la tabla vieja; del solver solo cae la comprobación de `Plaza.java:40-42`, y su modelo no cambia; a los nueve puntos se suma `SolucionMapper.aSesiones` (`:136-142`).
  (F) Del usuario. La hora de las reuniones la pone el generador. Lo ideal es que no cambie: el horario es semanal, y entre regeneraciones lo garantiza el pin que ya existe. Fijarla antes de generar es una mejora opcional.
  (G) Del usuario. Cargo del profesor, de un enumerado: Profesor/a (por defecto), Jefe/a de Estudios, Director/a, Vicedirector/a y Secretario/a. Varios profesores pueden tener el mismo cargo (el centro tiene tres jefes de estudio).
  (H) La primera migración real añade el rastro en el log y se verifica sobre copias de los bancos.
  (I) La verificación con datos reales amplía la cadena versionada (`derivar-catalogo.py` y `cargar-centro.py`), porque `O-prueba-secretaria` y `O-comparacion` contrastarán contra esa base.
  (J) S202, del usuario a propuesta del arquitecto, tras el M2 de `C-totales-y-cargo`: las horas configuradas salen de la misma agregación que el ERROR de sobrecarga, con un filtro «solo CLASE», y ese ERROR sigue contando todas las actividades, porque una reunión ocupa al profesor; el tipo y los totales llegan a la prevalidación por un record de la capa app, y el solver no cambia; una regla AVISO de cuadre por entidad, que se emite si configuradas ≠ declaradas; los PDC admiten total, editable desde su diálogo; se cuentan tramos y se rotulan «horas». Resto en el contrato de `s202/b2`.
- **Terminado cuando** — ESCRITO en S200 sobre esa medición. `O-datos-centro` termina cuando:
  1. **Tipos de actividad.** La actividad lleva tipo CLASE, REUNION o FUNCION, y las existentes quedan como CLASE. Una que no es CLASE se guarda sin subgrupos y sin aula, y se rechaza si pide tutor; una CLASE sin subgrupos da 400. El editor de Actividad permite elegir el tipo y la lista de Actividades lo muestra. El solver coloca las que no son CLASE sin solapar a sus profesores. Se ven con el nombre de su asignatura en la vista y el PDF de profesor y en el CSV; sin aula, ni la celda ni el CSV pintan un aula, y no aparecen en las vistas ni el PDF de aula. Con tests y campaña de mutación.
  2. **Totales declarados.** El profesor y el grupo admiten un total declarado opcional. Lo configurado es la demanda que ya calcula la prevalidación, contando solo actividades CLASE. Las listas de profesores y de grupos pintan «configuradas de declaradas» y marcan el descuadre. La prevalidación da un AVISO por descuadre en los dos sentidos, y ese aviso se ve en el diálogo de generar antes de confirmar. Con tests y campaña de mutación. **✔ CUMPLIDA en S203** (`f381bdd`, `7db1ae6`, `7137833` y `875ea3f`; la CI del último la comprueba el usuario): total declarado en profesor, grupo y PDC; AVISO de cuadre por entidad en los dos sentidos, contando solo CLASE; columna «Horas» en las listas, con «(no cuadra)»; los AVISO llegan al diálogo de generar.
  3. **Cargo.** El profesor lleva un cargo del enumerado de (G), «Profesor/a» por defecto, editable en su formulario y visible en su lista. **✔ CUMPLIDA en S203** (`f381bdd` y `7137833`).
  4. **Migración con rastro.** Las migraciones llevan una base de versión 1 a la vigente sin perder nada. El log escribe una línea al preparar una base (versión de partida, de llegada y ruta) y otra al rechazar una base posterior al abrir un curso. Verificado sobre copias de `dfa4c077…` y de `f6c56947…`: se migran, los recuentos por GET no cambian y generan sin violaciones duras. **✔ CUMPLIDA en S202** (`s202/a1a2` y `s202/a3`: copias nuevas de `dfa4c077…` (versión 0) y de `f6c56947…` (versión 1) llegan a la 2 por el arranque y por la apertura de curso, idénticas byte a byte por los dos caminos; recuentos por GET iguales; una línea INFO por base preparada; una base de versión 99 se rechaza con 409 y su WARN sin tocar un byte; generación de 600 s FEASIBLE con 0 violaciones en las dos, dfa al segundo intento tras un 503 reintentable. Prueba en navegador del usuario en `s202/a4`). **S203:** el esquema vigente pasa a 3 (`003.sql`, solo columnas añadidas), con su test de migración con datos (`unaBaseV2ConDatosLlegaALa3SinPerderNada`). Decisión del usuario a propuesta del arquitecto: la medición de S202 (A1 a A3, sobre copias nuevas de `dfa4c077…` y `f6c56947…`) se repite con el esquema 3 dentro de `C-carga-datos-centro`; hasta entonces, cumplida con esa reconfirmación pendiente.
  5. **Datos reales.** La cadena versionada carga desde base vacía el horario oficial de 2026/2027 con sus reuniones (RED, RT12 y RT34, una actividad REUNION conjunta por código) y sus funciones (ORYCA, APSTE, FOREI, HUERT, PROAR y REYR, como FUNCION con horas sueltas de cada profesor), sin aula, y con los totales declarados sacados del volcado. Una generación sale sin violaciones duras; las 47 celdas se conservan por profesor y código, y las 1421 de clase como hasta ahora; los totales dan cero avisos; y borrar una actividad de clase da su aviso de profesor y de grupo en el diálogo de generar. md5 citado. `INFORME-NO-REPRESENTABLE.md` deja de listar lo que ya se representa.
- **ESTADO en S200: 0 de 5.**
- **ESTADO en S201: 1 de 5.** Condición 1 cumplida (`8537348`, `fb71cb4`, `d1c328d` y `074e940`; la CI del último la comprueba el usuario). Condición 4: migración `002`, guarda de huérfanos y rastro en el log, con tests y campaña; falta su verificación sobre copias de `dfa4c077…` y `f6c56947…`.
- **ESTADO en S202: 2 de 5.** Condiciones 1 y 4 cumplidas. `C-actividad-sin-alumnos` hecho. `C-totales-y-cargo`: contrato cerrado y cotejado (`/home/luis/educhronos-aceptacion/s202/b2/contrato.md`), sin código.
- **ESTADO en S203: 4 de 5.** Condiciones 1 a 4 cumplidas; la 4, con su reconfirmación sobre el esquema 3 pendiente. `C-totales-y-cargo` hecho. Falta la condición 5.
- **Fuera del criterio, con motivo:** las guardias, su número y su reparto, el tipo GUARDIA y `conv`, que S199 contó con las guardias (150 = 139 + 9 + 2): son de `O-guardias`. Fijar la hora de una reunión antes de generar (F). El cargo en los PDF. Aulas candidatas y tipos de aula (`O-aulas`). Versión, Release y VM, que llegan con `O-prueba-secretaria`. Un e2e nuevo (R-e2e). Sin medir y a resolver en sus Cambios: la reconstrucción de `sesion` con `foreign_keys` apagado, que SQLite no deja cambiar dentro de una transacción (aviso de `O-base-tecnica`); el cambio en la asignación de aula del modelo del solver; y si una sesión sin grupo se puede fijar desde la vista de profesor. **S201:** resueltos los tres en `C-actividad-sin-alumnos`: los dos primeros, como dice la corrección de (E); el tercero, sí, por lectura (el pin viaja sin aula), pendiente de verlo en el navegador. `RT12` y `RT34` siguen pendientes de confirmar con la secretaria; el volcado apoya que sean reuniones, y no bloquea.
- **Deudas:** salda `D-totales-sin-contraste`, `D-borrado-sin-control-de-horas`, `D-cargos-sin-modelo` y `D-migracion-sin-rastro`. Toma sede y salda `D-plaza-sin-subgrupos` (antes `O-estructura`) y `D-aviso-fuera-del-dialogo` (antes sin sede).
- **Depende de:** `O-base-tecnica` (migraciones) y `O-carga-2026` (base oficial y cadena).
- **Valor:** la secretaria apunta las horas del centro y ve al momento si algo no cuadra, también después de borrar; mete reuniones y funciones como lo que son, sin asignaturas ni aulas inventadas; y sabe quién tiene cada cargo.
- **Cambios que agrupa:** `C-alcance-datos-centro` ✔ HECHO (S200): el M0, la medición y este criterio. `C-actividad-sin-alumnos` ✔ HECHO (S201-S202): condiciones 1 y 4. `C-totales-y-cargo` ✔ HECHO (S202-S203): condiciones 2 y 3. `C-carga-datos-centro`: condición 5 y reconfirmación de la 4 sobre el esquema 3.
- **Métrica de §7:** estimación de 5 a 7 sesiones (subida en S200 por el coste del aula); límite de 8.

---

## 4. Clasificación del trabajo pendiente

La deuda deja de ser la unidad de planificación y pasa a ser un MECANISMO DE
SEGUIMIENTO. Cada elemento pendiente se clasifica en una de cuatro categorías con
disposición distinta:

- **Deuda técnica real** — algo mal hecho que habrá que corregir. Cuelga de un
  objetivo; se paga solo si BLOQUEA su criterio de terminado.
- **Mejora futura** — algo que falta pero no está mal. Cuelga de un objetivo
  futuro; espera a que se abra.
- **Decisión arquitectónica consciente** — se eligió así con razón. NO es
  pendiente: sale de la cola de trabajo, se conserva como registro permanente.
- **Limitación conocida** — no se hará, y se sabe por qué. NO es pendiente: se
  documenta el "no se hará y por qué".

Solo las dos primeras son "trabajo pendiente". Las otras dos salen de la cola por
reclasificación.

### Clasificación de las deudas vivas actuales

Referencia cruzada con la sección "Deuda consciente VIVA" de
`plan_trabajo_horarios.md`. Cada deuda conserva allí su texto íntegro; aquí se le
asigna categoría, objetivo y disposición. Una fila tachada (deuda cerrada) lleva «—»
en «¿Bloquea?»; si decía algo más que «No», su disposición lo conserva tras «**Bloqueo:**» (S194).

#### Objetivos disfrazados de deuda → se PROMUEVEN a objetivo (§3)
| Deuda | Se convierte en |
|---|---|
| D-UI-shell | O-shell |
| D-seed-demo | parte de O-demo |
| D-demo-cliente | parte de O-demo |

#### Deuda técnica real, colgada de su objetivo
| Deuda | Objetivo | ¿Bloquea? | Disposición |
|---|---|---|---|
| ~~D-plan-duplicado~~ (copia caducada de `plan_trabajo_horarios.md` en `docs_extra/old/`) **CERRADA S157** | Transversal, sesión de Higiene/Método | — | Descubierta en el cierre de S131 al localizar rutas en vez de suponerlas. El documento vivo es `docs/`, que es el que leen `verificar-cierre.py` y `regenerar-indice.py`; la copia de `docs_extra/old/` no se actualiza desde hace sesiones y ningún guion la mantiene. Riesgo real y barato de materializar: un `grep -r` sobre el repo la lee igual que a la viva, y una afirmación de estado vivo tomada de ahí es falsa sin que nada avise. Decidir en su sesión si se borra o se marca como archivo histórico; borrar sin criterio no es la opción por defecto **SEGUNDA INSTANCIA en S135, en otro fichero y PAGADA en el camino (`494554e`):** `INFORME-RECONCILIACION.md` existía en `docs/` y en `docs/horario-referencia/`, y la nota de cierre de S119 había aterrizado en la copia huérfana, de modo que la copia que acompaña a los datos siguió dieciséis sesiones diciendo «correspondencia incierta» sobre un caso cerrado en S115. Cadena reconstruida por `git`: `0023c8f` anunció mover y copió; `62ad6a2` nombró `horario-referencia` en su mensaje y escribió en `docs/`. Se pagó AQUÍ y no en Higiene porque el instrumento de la capa 2 necesitaba ese mapa: escribir en el código una regla que su fuente niega es la enfermedad, no el arreglo. La deuda original (la copia de `docs_extra/old/`) sigue VIVA **CERRADA en S157 por extinción del objeto:** medido que `docs_extra/old/` ya no contiene el plan (seis ficheros, ninguno es él), que `find` sólo encuentra `docs/plan_trabajo_horarios.md` y que `docs_extra` está en `.gitignore` sin historia en git, así que ni `git grep` la leía. Nadie la borró en esta sesión: ya no estaba. **CERRADA** |
| D-instrumento-criterio4-ciego-al-ancho (el recuento de marcas `+N` mide alto, y del ancho no se ocupa nadie) | Transversal, con la sesión de Higiene/Método | No | Nace en S132. El recuento de marcas `+N` es EL instrumento con que se verifica el criterio 4 de O-navegación y sólo mide ALTURA: devuelve el mismo número mientras el texto se pierde de lado. **El encuadre que proponía el traspaso —«el instrumento es ciego a lo que el criterio pide»— es incorrecto**: el criterio 4 pide literalmente «sin scroll VERTICAL», así que el instrumento mide lo que se le pide. Lo que no existe es instrumento para el ANCHO, y por eso S132 introdujo un corte lateral sin detectarlo (`D-corte-lateral-a-1280`). Es la familia de S122 por el EFECTO —una dimensión que nadie mira— y no por la causa. Sede en Higiene/Método y NO en O-navegación, que está TERMINADO: colgarla de un objetivo cerrado equivale a decidir que no se paga nunca. La descripción del criterio 4 queda corregida en su ficha para que diga qué no cubre |
| D-corte-lateral-a-1280 (el padding lateral de la vista mueve el umbral de corte de la rejilla de 1200 a 1280 px) | O-diseño | No | Nace en S132 y la introduce el propio Cambio (`82ec04c`, punto 2 del acta): dar padding lateral a `.app__contenido` mueve el umbral de corte lateral de la rejilla de 1200 a 1280 px —6 líneas cortadas en 4ºA, «Taller 1 Aula Plástica»—. Da igual `--e4` que `--e5`: lo que cuesta es DARLO. Medido con `donde-corta.mjs`: 0 líneas cortadas a 1920 y a 1440 en el estado final. NO rompe nada declarado, porque 1280 está excluido del criterio 4 desde S123 (limitación conocida del portátil), pero el dato que hay que conservar no es «corta a 1280» sino que **el margen se estrechó 80 px**. Sede O-diseño y no O-navegación: lo introduce trabajo de O-diseño y O-navegación está cerrado. No se paga: revertir el padding contradiría la decisión 3 y el juicio del arquitecto (R-terminado) |
| D-deuda-sin-sede-en-el-plan (cinco deudas nacidas en S130 y S131 no tienen texto íntegro en el plan) | Transversal, con la sesión de Higiene/Método | No | Nace en el M1 de S132 al buscar dónde cerrar dos deudas. Esta sección declara al final que NO reescribe el texto íntegro de cada deuda —«ese vive en `plan_trabajo_horarios.md`, que sigue siendo su fuente»— y desde S130 las altas se escriben SÓLO aquí: `D-jerarquia-declarada-sin-aplicar`, `D-avisos-como-bloque-fijo`, `D-declarado-sin-artefacto`, `D-plan-duplicado` y el renombrado de `D-select-nativo-desparejo` a `D-controles-nativos-sin-neutralizar` no aparecen en la sección «Deuda consciente VIVA» del plan. Dos daños medidos y no supuestos: quien lea el plan para conocer el estado de una deuda no la encuentra, y la misma deuda tiene DOS nombres según el documento, que es R4 por el lado de la definición viva. NO se salda en S132 (R-deuda: son cinco fichas que escribir y ninguna bloquea el objetivo activo); las tres deudas que S132 da de alta nacen en LAS DOS sedes para no engordarla **INSTANCIA de S158:** `D-horario-irreversible` (S111) tiene fila en §4 y ninguna entrada propia en el plan; el token solo aparece citado dentro de otras dos entradas. Detectada al anexarle la nota de `O-curso`. No se crea la entrada en S158 (R-deuda). **INSTANCIAS de S168:** `D-jornada-msg409` y `D-jornada-asimetria` (S107) tienen fila en §4 y ninguna ficha en el plan, y desde S168 se citan en la ficha de `D-jornada-congelada-por-disponibilidad`. No se crean (R-deuda). `D-proyeccion-sin-duracion` sale de la familia: recibe ficha en S168 al pasar a bloquear. |
| ~~D-insignia-sin-leyenda~~ (la insignia de coste blando se pintaba como un número desnudo con signo) **CERRADA S128** | O-diseño, en C-identidad | — | Nace en S122 al medir qué son las insignias `1`/`-1` de la rejilla (`docs/diseno-navegacion.md` §A1, §3-H-1). El `<span class="badge">` de `horario-grid.html:34` no lleva `title` ni `aria-label`, a diferencia del candado, que sí los lleva dos líneas más abajo (`horario-grid.html:40-41`). El usuario ve un `-1` en la esquina de una celda y no tiene forma de saber que significa «esta clase está tapando un hueco»: es el dato más denso de la rejilla y el único sin rótulo. Arreglo natural: el mismo par tooltip + etiqueta accesible que ya usa el candado. NO se paga en S122, que no toca `app/`. Cuidado al redactar el texto: el número es un delta CONTRAFACTUAL con signo y no tiene por qué cuadrar con `Totales` (`models/diagnostico.model.ts:58-65`), así que la leyenda no debe prometer que sea un coste absoluto. **PAGADA Y CERRADA en S128**, en su sede escrita, con el mismo par `title` + `aria-label` del candado y una leyenda que dice qué significa el signo sin prometer que cuadre con `Totales`. El argumento que decidió pagarla aquí no lo tenía la ficha de S122: desde S127 esa esquina tiene DOS números con signo —el `+N` de desbordamiento y el coste blando— con significados sin relación y sólo uno con explicación, así que dejó de ser «un dato sin rótulo» y pasó a ser ambigüedad activa creada por el Cambio anterior. Coste en altura: cero. **CERRADA** |
| D-asignatura-sin-nivel (una asignatura no sabe a qué nivel pertenece) | Sin sede | No | Nace en S122 al leer el catálogo para la maqueta. `Asignatura` es `(id, codigo, nombre_completo)` y nada más: la relación asignatura↔nivel solo se DEDUCE recorriendo Actividad→Plaza→Subgrupo→Grupo→Nivel, es decir, existe únicamente para las asignaturas que ya están usadas en alguna actividad. Consecuencia en la UI: el selector de asignatura del formulario de actividad de un grupo de 1ºESO ofrece las 100 asignaturas del centro, incluidas las de 3º y 4º, sin forma de acotarlas. Emparenta con `D-selectores-sin-busqueda` (que es de ESCALA) pero no es la misma: aquí falta el DATO con el que filtrar, no el filtro. Sin sede porque el arreglo natural toca modelo y esquema, y eso no cae en O-navegación ni en O-diseño. No se paga ahora |
| ~~D-F8.6-ii-b~~ (no hay gesto de despinar) **CERRADA S83 — la fila mentía hasta S135** | O-ajuste-cierre | — | Medido en el M0 de S134: bloque 8.6-iii-B1 marcado `[x]`, la propia ficha de la deuda remite a S83 y la bitácora lo confirma; S84 le añadió cobertura de test. Este documento la afirmaba VIVA Y BLOQUEANTE en tres sitios —fila de H1 en §2, esta fila y el «Terminado cuando» de O-ajuste-cierre— más el recuento de esta sección, de modo que O-ajuste-cierre llevaba sesiones ofreciéndose como candidato por una deuda muerta. **Los cuatro sitios CORREGIDOS en S135**, que es el pago R5 que S134 dejó anotado sin hacer |
| D-F8.5-D2a-a (I4 sin red) | O-catálogo | Sí, dentro de O-catálogo | Medido en S101: es de `ProfesorTutoria` (tutoría), NO del CRUD de Profesor. Su activación escrita («otra vía de escritura») NO la cumple un form que escribe por el REST existente. Se paga con el formulario de tutoría (roza O-estructura) |
| D-F8.5-E-b (unicidad profesor-tramo sin red) | O-disponibilidad (cerrado S167; reasignada en S159, era O-catálogo) | No | Medido en S101: es de `ProfesorRestriccionHoraria` (disponibilidad, sub-recurso), NO del CRUD de Profesor. Se paga con el formulario de restricción horaria, no antes **CORREGIDA en S159:** decía «Sí, dentro de O-catálogo», incoherente con un objetivo terminado. Su condición de pago es «índice único si aparece otra vía de escritura», y no la cumplen ni la rejilla de `O-disponibilidad` (escribe por el mismo PUT, con su validación) ni el duplicado de `O-curso` (copia filas, no las crea). |
| D-F8.5-D2a-b (incoherencia 404/400 FK) | O-catálogo | No bloquea | Se evalúa dentro de O-catálogo |
| D18 (condiciones necesarias de factibilidad) | O-estructura | No | Ya cubierto en backend (8.4-A); resto en presentación |
| D-F8.6-iiiB1-c, -iiiB2a-a (superficie de error) | O-ajuste-cierre (cerrado S146) | No | Se evalúan al abrir; probablemente limitación conocida aceptable |
| ~~D-F8.6-ii-a~~ (el `reason` de los 400/409 no llega al navegador) **CERRADA S167** | O-estructura (reasignada en S109; era O-ajuste-cierre) | — | **Bloqueo:** No bloquea el criterio, pero DEGRADA todo lo entregado. **CERRADA en S167** (`6a29f65`), dentro de `C-rejilla-disponibilidad` porque bloqueaba la condición 4 de `O-disponibilidad`: `spring.web.error.include-message=always` sustituye a la clave de Boot 3 y `MensajeDeErrorHttpTest` lee el cuerpo por HTTP real. Verificada en navegador en un sub-recurso y en un formulario de catálogo. Texto histórico: AMPLIADA y RECLASIFICADA en S109 a técnica real TRANSVERSAL. La redacción de S81 decía que `server.error.include-message` no estaba en `application.properties`: hoy SÍ está y aun así el cuerpo llega sin `message` (medido por curl en tres endpoints, fuera de la UI). Todos los formularios pintan «Bad Request» en vez del motivo, y el 409 del PUT de actividad construido en S109 queda mudo. La causa (cambio de comportamiento en Spring Boot 4) es HIPÓTESIS no medida, y elegir el arreglo —reactivar la clave, `ProblemDetail`, o traducir en cada controlador— exige su propio M2: por eso no se pagó en S109. Hallazgo de método asociado: los tests de endpoint asertan `status().reason()`, que lee el `MockHttpServletResponse` y no el cuerpo de red — verde en test, mudo en producción. AFINADA en S110, medido en NAVEGADOR: el mensaje accionable NO se pierde —viaja como REASON PHRASE— y lo que falta es la clave `message` en el cuerpo; leer `statusText` en cliente NO es la solución (HTTP/2 no transporta reason phrases). AFINADA en S111 con un dato que su M2 debe usar como punto de partida: hay CONTRADICCIÓN DOCUMENTAL en el repo —el javadoc de `asignatura-lista.ts` afirma que `server.error.include-message=always` y el de `horario-view.ts` afirma que está DESACTIVADO—, y el comportamiento observado en navegador (el 409 de borrado de nivel pinta «Conflict» crudo) da la razón al segundo. Confirmada además en el octavo formulario: la lista de niveles nace muda. AFINADA en S112, y ESTE es el punto de partida de su M2, no el de S111: medido por lectura literal de `application.properties`, la clave `server.error.include-message=always` SÍ ESTÁ, con comentario propio que explica por qué se puso y por qué los tests no lo notan. El javadoc de `horario-view.ts` describe bien el SÍNTOMA y mal la CAUSA. La hipótesis viva pasa a ser que la clave está puesta y no surte efecto; su M2 debe EMPEZAR comprobando eso en ejecución, porque si se confirma, la opción «reactivar la clave» desaparece del abanico de tres. **COMPROBADO en S113, y con ello su M2 arranca un paso más adelante:** la clave está en `application.properties` Y en `target/classes`, sigue existiendo en la versión de Boot en uso, y aun así el cuerpo llega sin `message` (evidencia en crudo sobre `POST /api/grupos/{id}/pdc`). «Reactivar la clave» queda DESCARTADA por medición, no por hipótesis; el abanico se reduce a `ProblemDetail` o traducir en cada controlador. Superficie ampliada: los seis mensajes del flujo del PDC son genéricos, cinco «Bad Request» y un «Conflict» que pierde el desglose «referenciada por N plaza(s)». El arreglo es GLOBAL —el CRUD plano se comporta igual—, no del diálogo ni de las guardas de S113. **FALSADA EN S116 LA AFIRMACIÓN DEL REASON PHRASE, y con ella se cae el fundamento que esta ficha y la de O-demo venían usando desde S110.** Medido con `curl --http1.1 -v -i` en tres rechazos: la línea de estado llega `HTTP/1.1 400 ` con la cadena VACÍA y el cuerpo trae cuatro claves sin `message`; NINGUNA información distingue «ya existe» de «payload inválido» desde el cliente. La salvedad del HTTP/2 es irrelevante (Tomcat sirvió HTTP/1.1, donde el reason phrase sí existe, y llega vacío). SUPERFICIE AMPLIADA: tampoco llega en un 400 de mensaje conocido (`{"maxSegundos":-1}`), luego afecta a TODAS las traducciones vía `ResponseStatusException`. NO se paga en S116 por R-deuda —la prevalidación en seco garantiza que el cargador no reciba ningún 400, y la regla «cualquier no-2xx es FATAL» resuelve el Cambio activo—, pero su ALCANCE cambia de grado: hasta ahora degradaba formularios; con el 422 de generación degrada la operación central del producto delante del usuario final. Se decide junto a D-motivo-rechazo-sin-registro. **CAUSA REAL MEDIDA EN S118, y cambia el abanico:** en Spring Boot 4 la clave se llama `spring.web.error.include-message`; la que vive en `application.properties` es sintaxis de Boot 3 y está MUERTA desde la migración a 4.1. Probado en las dos direcciones (con la clave del fichero el 400 llega sin `message`; arrancando con la nueva, llega con él). Todas las mediciones previas eran correctas —la clave no surte efecto— pero ninguna preguntó POR QUÉ, y de ahí se extendió una conclusión que los hechos no sostenían: «reactivar la clave» NO estaba descartada, estaba viva bajo otro nombre y su arreglo es de UNA LÍNEA. Dos consecuencias nuevas: el comentario de `application.properties` describe un efecto que no ocurre, y `mensaje()` del frontend lee `err.error.message`, que nunca llega, así que TODOS los rechazos de pines llevan degradados desde la migración. NO se paga en S118 (cambia el cuerpo de error de toda la superficie REST y sus asertos), y S118 se hizo INMUNE a ella construyendo el cuerpo del fallo en el controlador. Cuando se pague, el primer paso ya no es un M2 de tres opciones: es probar la clave correcta y medir qué tests caen **Reproducida en S150** (M3-1): el 400 de vista desconocida y el 404 del PDF llegan sin `message`, igual que el 404 del CSV. **INSTANCIA de S158:** Medido en el M0 de S158: `DELETE /api/actividades/20` responde 409 con cuerpo `{timestamp, status, error, path}`, sin `message`, pese a `server.error.include-message=always` (`application.properties:42`); el desglose de referentes que construye `ReferenciaEntranteException` no llega al cliente por esa vía. La causa no es nueva: está medida en S118 (en Boot 4 la clave es `spring.web.error.include-message`, y la del fichero es sintaxis de Boot 3, muerta desde la migración). Importa a `O-curso` porque su condición 3 exige que el mensaje de la guarda de solo lectura llegue al cliente, y esa guarda puede caer en el mismo mecanismo: su M2 empieza por esta causa. No bloquea por sí misma. **INSTANCIA de S159:** la guarda de solo lectura de `O-curso` la esquiva escribiendo su propio cuerpo (`RechazoCursoDTO`, con `message`) desde el filtro, como S118; verificado en navegador que los formularios muestran el mensaje. Sigue sin pagarse. **CERRADA** |
| ~~D-plaza-sin-subgrupos~~ (una plaza con cero subgrupos se acepta) **CERRADA S201** | `O-datos-centro` (S200; antes O-estructura) | — | Detectada por el M2 de S109: `validarPlazas` comprueba XOR, I7 e I2, pero acepta `subgrupos` nulo o vacío y devuelve 201. Agujero de dominio (la población de la plaza SON sus subgrupos). DECISIÓN de S109: el formulario refleja el contrato y NO añade el validador solo en cliente; hay un spec que se pondría rojo si alguien lo añadiera. El arreglo es simétrico a I7 (≈10 líneas y un test). No se paga ahora **S189:** el inventario (fila 13) mide que aceptar una plaza sin subgrupos es hoy el único rodeo para meter reuniones de profesores (`D-cargos-sin-modelo`). Pagarla tal como está escrita lo cerraría: antes hay que decidir si una actividad sin alumnos es un caso legítimo. **Saldada en S201** (`d1c328d`, `074e940`): una CLASE sin subgrupos da 400 y el formulario no la envía; una REUNION o una FUNCION va sin subgrupos. |
| D-i2-dedup-cliente (la deduplicación intra-plaza del validador I2 no la cubre ningún test) | O-estructura | No | Nace en S110 de la campaña de mutación: quitar el `Set` por fila del validador `subguposDisjuntos` no pone rojo nada. El escenario es INALCANZABLE desde la UI (un `<select multiple>` no repite opción; el GET proyecta desde un `Set`), así que la regla existe por fidelidad con `validarPlazas` y no porque haya camino que la ejercite. Deuda de TEST, hermana de D-jornada-flush-test. Escribir el caso exigiría fabricar un estado que el sistema no produce. No se paga ahora |
| D-horario-irreversible (un horario generado no se puede borrar ni reemplazar) | O-estructura | No bloquea el criterio, pero es un CALLEJÓN SIN SALIDA para el usuario | Nace en S111, medida en navegador y confirmada en código. No existe `DELETE /api/horarios/{id}` ni ningún borrado programático de `sesion`; cada `POST /api/horarios` ACUMULA (alta pura, sin consulta previa ni reemplazo), y el 409 del PUT/DELETE de actividad cuenta `sesion(es)` entre sus referentes. Consecuencia: en cuanto se genera un horario, las actividades que usa quedan congeladas para editar y borrar de forma PERMANENTE por la vía UI/API; la única salida es tocar SQLite a mano. El javadoc de `ActividadService.editar` prescribe «el usuario borra el horario y luego reconfigura», salida que NO existe. El `on delete cascade` de `sesion.horario_id` ya está en el esquema: el mecanismo está preparado y nadie lo dispara. Afecta al e2e solo si éste necesitara rehacer algo tras generar: MEDIDO en S112 y NO le afecta, porque cada corrida parte de una BD borrada y genera una sola vez. REEVALUADA en S115 y BAJA de presión sin cerrarse: la carga del centro real entra por script contra la API, así que la base se rehace en minutos y la congelación deja de ser callejón sin salida. C-borrado-horario sale del camino crítico de O-demo. Vuelve a subir si algún día la carga deja de ser repetible. La estimación de S115 queda escrita por si se paga: el `on delete cascade` de `sesion.horario_id` ya está y el diálogo `confirmar-borrado` es reutilizable, pero NO existe `GET /api/horarios`, así que un botón de borrar solo alcanzaría al horario que se está viendo. EJERCITADA en S116 sin morder: la corrida de diagnóstico con 600 s generó horario y creó 770 sesiones, y la base se devolvió a `horario_generado 0` / `sesion 0` restaurando una copia previa, exactamente la salida que S115 preveía. Confirma la rebaja de presión y confirma también su precio: la salida sigue siendo rehacer, no borrar, y depende de que alguien se acuerde de copiar antes. Sigue sin pagarse **CUANTIFICADA en S142:** la copia de trabajo acumuló CUATRO horarios y **3276 filas de `sesion`** (819 × 4) en una sola sesión. Reconfirmado en código y no supuesto: no existe `DELETE /api/horarios/{id}` —los trece controladores del proyecto no incluyen ninguno que escriba sobre `sesion`, y la única escritura sobre `sesionRepository` en todo `src/main` es el `saveAll` de `GeneradorHorarioService:277`—, y el `on delete cascade` de `sesion.horario_id` sigue en el esquema sin que nada pueda dispararlo desde la API. La única salida sigue siendo SQL a mano **MEDIDO en S158, no bloquea `O-curso`:** el curso nuevo no hereda la congelación si la duplicación no copia horarios ni pines, y la única FK hacia el horario es `sesion.horario_id`, con CASCADE. Sigue viva en el uso normal, igual que antes. **S188:** en la demo, el observador paró a la secretaria cuando iba a generar en el 2026/2027 antes de configurar: habría dejado sin poder editarse ni borrarse las actividades de las tareas 5 y 6, sin salida (M2 de S188: `HorarioController` sólo tiene `POST` y `GET`, `HorarioController.java:88,170,177,191,212,250`). |
| D-error-generacion-pin (un fallo de generación se anuncia como fallo de pin) | O-estructura | No | Nace en S111. `lanzarGeneracion` reutiliza el helper `mensaje()` escrito para los pines, cuyo degradado es «El servidor rechazó el pin (N).»; ante un horario infactible (422) el usuario lee literalmente eso. Hermana de D-F8.6-ii-a: el texto del backend, que sí nombra el recurso culpable, se pierde por configuración y no por diseño del componente, así que las dos primeras ramas del `\|\|` fallan siempre. Arreglo trivial (un degradado propio) pero encuadrado con esa deuda. **PREDICCIÓN CORREGIDA en S116:** en el camino del 422 de generación la deuda NO se cumple, porque el cuerpo trae `"error":"Unprocessable Content"` y `mensaje()` (`horario-view.ts:267`) prefiere ese término sobre el degradado; el texto del pin solo saldría si faltaran `message` Y `error`. Lo que el arquitecto vio no fue el mensaje equivocado sino NINGÚN mensaje, y la causa es otra: D-generacion-sin-indicador. La deuda sigue viva para los cuerpos sin ninguna de las dos claves. No se paga ahora |
| D-molde-mensaje-cubierto-en-form (la precedencia de `mensaje()` en las listas de catálogo no la cubre nadie) | O-catálogo (CERRADO en S106) | No | Nace en S111 al destaparlo la mutación M6. El javadoc de `asignatura-lista.spec` y hermanas afirma que el orden interno de `mensaje()` está «cubierto en el form, misma función»: es FALSO —hay dos funciones copiadas a propósito y no compartidas, así que el caso del formulario no puede cubrir a la de la lista—. En niveles se cerró añadiendo la clave `error` al cuerpo flusheado del caso del 409, sin caso nuevo; las cuatro entidades de O-catálogo siguen con el hueco y con el comentario falso. NO se paga: R-terminado, el objetivo está cerrado. Cuando se toque una de esas listas por otro motivo, es una línea de fixture |
| D-doble-proyeccion-compartido (el doble de `getProyeccion` es un Subject compartido) | O-ajuste-cierre (cerrado S146) | No | Nace en S112. Es el ÚLTIMO doble compartido de `horario-view.spec.ts`: sus tres hermanos de escritura (`guardar`, `borrar`, `generar`) migraron a fresco por invocación en S94, y `bloqueos.listar` en S99. La forma compartida impide encadenar FALLO → RECARGA, porque un Subject cerrado por `.error()` redispara al re-suscribirse. Mordió en el caso (40), que arranca con la proyección en 404 —el escenario real de BD vacía—. Parcheado con re-stub LOCAL al caso, no homogeneizando el doble: migrarlo tocaría los 25 casos vigentes que lo consumen (R-terminado). Deja de ser aplazable con el segundo caso que necesite lo mismo. No se paga ahora |
| ~~D-e2e-retry-bd~~ (un reintento de Playwright correría sobre la BD del intento fallido) **CERRADA S175** | `O-ci` (H4), condición 2 | — | Nace en S112. `retries: 2` en CI, y el `rm -f app/educhronos-e2e.db*` vive en el `command` del `webServer`, que corre UNA VEZ por corrida, no por test ni por reintento. Un reintento encontraría el centro ya creado y moriría con un 400 de código duplicado, es decir, por causa distinta de la original: esconde el diagnóstico. Hoy no bloquea porque NO HAY CI (Fase 12 sin abrir). Se resuelve al abrirla. No se paga ahora Nota de S162: la base del e2e vive ahora en `app/target/e2e/`. **PASA A BLOQUEAR en S173:** medido que `playwright.config.ts:29` sigue dando `retries: 2` con `CI`; la condición 2 de `O-ci` exige el e2e sin reintentos en CI. **Saldada en S175** (`b09b4c5`): `retries: 0` en todos los casos y `trace: 'retain-on-failure'`, porque con cero reintentos `on-first-retry` no graba nunca; un comentario en `playwright.config.ts` avisa de que, si vuelven los reintentos, el borrado de la base debe pasar a ser por test. |
| ~~D-e2e-aislamiento~~ (la suite e2e corre en paralelo sin aislamiento entre specs) **CERRADA S162** | O-curso (H4), condición 8 | — | **Bloqueo:** **Sí desde S158**: es la condición 8 de `O-curso`. Nace en S112. `fullyParallel: true` sin `workers` reparte los specs entre workers que atacan el mismo backend y la misma BD. Inocuo HOY por una razón concreta y no por suerte: `humo` solo lee (la landing no llama a `/api`) y `centro-minimo` es el único que escribe. El riesgo llega con el TERCER spec: dos writers sobre un SQLite único chocarán por los `unique` de código de forma no determinista, que es la clase de fallo intermitente que desprestigia una suite entera. Se decide al escribir el segundo spec que escriba. No se paga ahora **PASA A BLOQUEAR en S158:** es la condición 8 de `O-curso`. El spec de duplicar curso es el segundo que escribe, que es cuando esta ficha dice que se decide. **SALDADA en S162** por `C-e2e-curso` (`fae5470`): suite en serie, carpeta de datos propia e invariante de orden. **CERRADA** |
| ~~D-e2e-centro-minimo-rojo~~ (`centro-minimo.spec.ts:265` falla: 0 instancias tras generar) **CERRADA S175** | `O-ci` (H4), condición 2 | — | Nace en S154, TÉCNICA REAL. Medido sobre `1e5baaf` limpio, en un worktree con su propio `npm ci`: falla idéntico, así que es PREEXISTENTE y ajeno a `C-arranque-cierre`; no se sabe desde qué sesión. IPv6 descartado con tres medidas (escucha `[::ffff:127.0.0.1]:8080`, `curl localhost` 200, `fetch` de Node 200). NO bloquea `O-instalación`: la condición 3 verifica descargas sobre copia del banco y su ficha dice «no depende de generar». Pero es el eslabón «crear centro → generar» de R-e2e: un e2e rojo que nadie mira es una señal perdida sobre hitos cerrados, y bloquea la Fase 12, que no puede montar CI con él en rojo. La causa no se investigó (fuera de alcance). **PASA A BLOQUEAR en S173:** reproducido en el M2 sobre `b04069c` (0 `.instancia` frente a 3, `centro-minimo.spec.ts:267`); la causa sigue sin investigar. **Saldada en S175** (`4f390cb`). Causa medida: desde S145 (`498cf8c`) toda generación abre `ConfirmarGeneracion`, y el spec no lo confirmaba, así que el POST no salía y la base quedaba sin horario. El spec confirma el diálogo, espera el POST y exige un estado 2xx con el cuerpo en el mensaje. Dos mutantes en rojo por su motivo: diálogo que cierra con `false` (tiempo agotado esperando el POST) y POST con 500 (aserción de estado). |
| D-props-test-obsoleto (el `application.properties` de test afirma que `schema.sql` dropea) | O-estructura | No | Nace en S112. Dice «schema.sql dropea y recrea, de modo que varios contextos Spring sobre este mismo fichero recrean el esquema con FK sin petar»; falso desde S109. Es la MISMA falsedad que S112 corrigió en `playwright.config.ts`, cuya hermana quedó viva. Efecto de lectura, no de ejecución (la suite de backend se limpia por otra vía), pero por R5 es estado vivo equivocado: hace que el siguiente lector decida sobre una premisa falsa. Se corrige al tocar ese fichero. No se paga ahora |
| D-pdc-lista-rancia (la lista de subgrupos no se entera del alta ni del borrado de un PDC) | O-estructura | No | Nace en S113 y la abre el propio Cambio: el alta de un PDC toca DOS catálogos (crea el grupo y su subgrupo mono-Di) pero el contrato del molde —«el diálogo cierra con `true` y recarga quien lo abrió»— solo alcanza a `GrupoLista`. MEDIDO en navegador: la sección de subgrupos seguía diciendo «No hay subgrupos todavía» con el subgrupo ya en la BD; simétrico al borrar. NO se paga aquí, con razón escrita: no es del género de las guardas (vista desactualizada, no destrucción de datos), arreglarla exige coordinar componentes hermanos dentro de `Configuracion` —que ES la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio propio— y no bloquea el criterio, cosa que el M4 demuestra: el §6.2 se reprodujo entero con la lista rancia de por medio. Se resuelve en el Cambio que decida la navegación; no se parchea con un `EventEmitter` ad hoc, que fijaría el molde por la puerta de atrás. **CERRADA en S123 por C-rutas-hijas, y por construcción.** El bug quedó localizado —`grupo-lista.ts:121-128` recarga solo grupos tras el alta de PDC y nadie avisa a `SubgrupoLista`—; con destino propio, entrar en subgrupos remonta el componente y su `ngOnInit` recarga. No se escribió ningún `EventEmitter` |
| D-pdc-vinculo-por-cadena (el agregado PDC localiza su subgrupo por código derivado) | O-estructura | No | Nace en S113. `PdcService.borrar` resuelve el mono-Di con `findByCodigo(codigo + "-Completo")`: el agregado que su javadoc dice poseer no estaba protegido fuera de sus tres métodos, y cualquier rename por otra vía dejaba el DELETE del PDC en 404 permanente. G2 CONTIENE la deuda cerrando el único camino que existía (el CRUD plano de subgrupos); vuelve a morder con un tercer camino de escritura hacia `Subgrupo`. Familia de D-F8.5-D2a-a y D-F8.2b-iv-a (validación de aplicación sin espejo en la base). Convertir la convención en referencia real es cambio de ESQUEMA, no una guarda: se evalúa cuando algo más toque `schema.sql` en esta zona. No se paga ahora |
| D-tokens-inexistentes (la familia `D-nueva-*` se cita en nueve sitios y no existe) | Transversal, sin objetivo asignado | No | Nace en S113 al auditar en R4 los tokens que la sesión introducía. `D-nueva`, `D-nueva-1` … `D-nueva-5` aparecen en `GrupoService`, `GrupoDTO`, `GrupoRequest`, `GrupoEndpointTest`, `grupo-form.ts` y la cabecera de `grupo.model.ts`, y ninguno tiene definición viva en este documento ni en el plan. Incumple R4 en su forma más simple; el daño es que el lector busca el token, no lo encuentra y no sabe si la regla sigue vigente. PREEXISTENTE: S113 corrigió solo el que ella misma introdujo (`D-nueva-2`) y registró el resto, porque mapear nueve citas a sus deudas reales exige leer nueve contextos y es trabajo propio, no un arreglo en caliente. Sesión de Higiene/Método, junto con el script de R4 que falta desde S101. **UN CONTEXTO LEÍDO EN S133 sin abrir la sesión:** el javadoc de `GrupoDTO.java` cita `D-nueva-2` para afirmar que el tipo «siempre será ORDINARIO», y es falso; `grupo.model.ts` ya registra lo contrario y el frontend discrimina por tipo. Desfase de documentación en el DTO Java, no de comportamiento. La sesión hereda uno de los nueve contextos resuelto |
| D-log-aplicacion (no hay logging estructurado en ninguna de las dos capas) | Transversal, sin objetivo asignado | No | Propuesta del arquitecto en S111 tras el recorrido en navegador, donde diagnosticar un fallo exigió leer código en vez de logs. Backend sin configuración de logging a fichero (solo consola); frontend sin ninguna traza, con `ngx-logger` mencionado como candidato pero NO evaluado. Mejora FUTURA: se registra para que no se pierda, no planifica y no cuelga de ningún objetivo vivo. **REENCUADRADA en S116, con evidencia y no con impresión.** El arquitecto propuso una sesión dedicada a introducir logging; se argumentó en contra que instrumentar antes de diagnosticar es instrumentar a ciegas y que había instrumentos gratis (pestaña Red, stdout). Los gratis BASTARON, pero solo porque el tiempo de respuesta era observable desde fuera y permitía descartar ramas; medido en la misma sesión que el motivo del 422 no se registra en NINGUNA parte (ver D-motivo-rechazo-sin-registro), luego con un fallo cuyo síntoma externo no variase no habría habido forma de diagnosticar sin leer código. Pasa de propuesta razonable a deuda con un caso medido detrás. Sigue sin abrir sesión por R-deuda. Distinción que ordena el asunto y que conviene no perder: los logs son para el DESARROLLADOR y los mensajes en pantalla para el USUARIO; lo que hace falta delante del centro es D-F8.6-ii-a, no ésta |
| D-timeout-como-infactible (un agotamiento de tiempo del solver se comunica como «no hay horario factible») | O-demo (C-generación) | No bloquea, pero AFIRMA ALGO FALSO | Nace en S116 al diagnosticar el 422 del centro real. `SolverHorario.java:117-125` devuelve el resultado si el estado de CP-SAT es OPTIMAL o FEASIBLE y lanza `HorarioInfactibleException` en cualquier otro caso, así que UNKNOWN —se acabó el presupuesto— cae por la misma rama que INFEASIBLE. MEDIDO: 5 s → 422 en 5,7 s; 30 s (defecto, `GeneradorHorarioService.java:201`) → 422 en 31,1 s; 600 s → **200** con `estadoSolver: FEASIBLE`, objetivo 188.0. Que el tiempo escale con el presupuesto en vez de terminar antes demuestra UNKNOWN y no INFEASIBLE, porque la infactibilidad se prueba y se devuelve al detectarla. Tres piezas SEPARABLES: calibrar o exponer el presupuesto (decisión A MEDIR, no un número a subir a ojo), distinguir UNKNOWN de INFEASIBLE (el arreglo de esta deuda), y que el motivo llegue al cliente (D-F8.6-ii-a). Es el M2 de C-generación. **AFINADA en S117: de RAZONADA a MEDIDA.** El barrido leyó el estado directamente de la excepción en 27 rechazos: **27 UNKNOWN, CERO INFEASIBLE**. El catálogo no se demostró imposible ni una vez. Superficie ampliada: con el defecto de 30 s la tasa de éxito es 0/4 y a 60 s es 1/9, luego la afirmación falsa es el camino NORMAL en producción, no un caso raro. La pieza (1) deja de ser «a medir» y pasa a ser decisión tomable. No se paga en S117 (la sesión no escribió producto). **PAGADA Y CERRADA EN S118, exactamente como preveía: DE PASO, por caer en el camino de fallo del Cambio.** El estado de CP-SAT pasa a ser CAMPO de la excepción y `mapear` lo traduce a tres desenlaces: INFEASIBLE → 422 CATALOGO_INFACTIBLE, UNKNOWN → 503 con `Retry-After: 0` y causa PRESUPUESTO_AGOTADO, el resto → 500. Verificado en ejecución sobre HTTP real: 30 s → 503 en 31,15 s con `estado: UNKNOWN` y `segundos: 30` en el cuerpo. La pieza (3) se resolvió SIN pagar D-F8.6-ii-a, construyendo el cuerpo del fallo en el controlador. **CERRADA** |
| D-motivo-rechazo-sin-registro (el motivo de un rechazo no se escribe ni en el log) | Transversal; se decide con D-F8.6-ii-a y D-log-aplicacion | No | Nace en S116. Hermana de D-F8.6-ii-a y peor que ella: el mensaje se construye correctamente en `SolverHorario.java:124`, con el estado de CP-SAT dentro, y se pierde ENTERO —no en el cuerpo, no en la línea de estado y NO en el stdout—. Medido: durante la petición que devuelve 422, `grep -inE "horario\|solver\|infactib\|INFEASIBLE\|cp-sat\|ortools\|422"` sobre el log completo da CERO coincidencias. Consecuencia: diagnosticar el fallo central del producto exigió deducirlo por el TIEMPO DE RESPUESTA desde fuera, único canal observable, y leer código. Es la evidencia que reencuadra D-log-aplicacion. El arreglo mínimo (un `log.warn` con el estado antes de lanzar) es barato y NO sustituye a ninguna de las dos deudas mayores. **MATIZ de S117:** se planteó pagarla como instrumento del barrido y se descartó POR MEDICIÓN, no por regla —el mensaje lleva el estado dentro y un arnés en proceso lo recibe como excepción, no como cuerpo HTTP—. Muerde al USUARIO, no al desarrollador que instrumenta desde dentro de la JVM. No se paga ahora |
| D-generacion-sin-indicador (durante la generación la pantalla no cambia) | O-diseño, o el Cambio que toque la vista de horario | No | Nace en S116. El POST tarda por diseño lo que dure el presupuesto del solver —y sobre el centro real ese presupuesto se AGOTA siempre— y en todo ese tiempo no hay spinner, ni estado «generando», ni botón deshabilitado (`horario-view.html:23` solo lo deshabilita si no se ha prevalidado); el `<p>Cargando…</p>` de `horario-view.html:61` es el estado por defecto de la rejilla, no un indicador. Medido en código; la parte razonada y NO medida es que ésta explica por qué el arquitecto no vio mensaje alguno al generar (miró antes de que llegara la respuesta). Es acabado de interacción, pero muerde EN LA DEMO. **AGRAVADA en S117 y ya no son 30 s:** el presupuesto defendible está en CIENTOS de segundos (600 s es el único punto sin fallos observados), luego la pantalla inmóvil dura diez minutos. Deja de ser acabado y pasa a ser condición práctica de la cláusula «presentable al centro» del criterio de O-demo. Sigue sin bloquear —el horario válido se produce— y sin abrir sesión, pero su arreglo cae en el mismo camino de fallo que el cierre de C-generación debe tocar. **PAGADA Y CERRADA EN S118, de paso.** Señal `generando` que cierra el botón (añadiendo `\|\| generando()` sin sustituir la condición de prevalidación), texto de estado visible y apagado en las DOS ramas; cuatro tests de vitest incluido el de rehabilitación tras error. Marcado mínimo y cero estilo por R-invalidación (O-diseño rehará el aspecto, no el estado). VERIFICADO EN NAVEGADOR durante diez minutos reales. Deja tras de sí D-presupuesto-anunciado-espejo. **CERRADA** |
| D-generacion-no-reproducible (dos generaciones idénticas dan resultados distintos, y a veces ninguno) | O-demo (C-generación) · S182: `O-pre-demo` (tiempo de cálculo) | No bloquea, pero hace IMPREDECIBLE la operación central | Nace en S117 del barrido de presupuesto. `SolverHorario` fija `setMaxTimeInSeconds` y `setRandomSeed` y NO fija el paralelismo: el grep de `num_search_workers`, `setNumSearchWorkers` y `MaxDeterministicTime` sobre `solver/src/main` y `app/src/main` sale VACÍO. CP-SAT usa todos los núcleos y corta por RELOJ DE PARED, así que la semilla no da reproducibilidad. EVIDENCIA: 600 s y semilla 42 dan objetivo 188.0 en S116 y 208.0 en S117; a 45 s, 1 éxito de 3; a 60 s, 1 de 9. Efecto de secuencia dentro de una JVM DESCARTADO (0 de 3 siendo las primeras corridas de un proceso nuevo). Consecuencia de producto: regenerar da otro horario sin haber cambiado nada, y con presupuesto corto puede dar horario una vez y 422 la siguiente. Consecuencia de método: toda medición sobre el solver necesita n>1; una corrida única no es una medida. Tres caminos separables (fijar workers, cortar por tiempo determinista, o dimensionar el presupuesto para tasa de éxito alta) y no se elige aquí. **AFINADA en S118 con la evidencia más limpia hasta la fecha:** dos corridas CONSECUTIVAS sobre la misma base y el mismo presupuesto de 600 s dieron UNKNOWN (curl, 17:38) y FEASIBLE (UI, 17:47, objetivo 192.0); la fallida corrió con la JVM al 687 % de CPU, así que no fue estrangulada. Consecuencia: «600 s es el único punto sin fallos observados» deja de ser cierta y la tasa acumulada a 600 s pasa a **3/4**. Segunda consecuencia, que es argumento de diseño: si el mismo botón unas veces da horario y otras no, el 503 reintentable de S118 no es un borde sino la mitad del comportamiento normal. Y un argumento MÁS para no tocarla a la ligera: fijar `num_search_workers` invalidaría la base empírica con que se calibró el defecto de 600 s. No se paga ahora **S182:** en la VM de la demo (3 CPU, 8192 MB), con los cambios de muestra y la semilla 42, 1 de 3 intentos a 600 s dio horario (FEASIBLE, objetivo 219, 602,8 s); los otros dos, 503 al agotar el presupuesto. No separa el efecto de los núcleos del de los cambios. Por lectura: `maxSegundos` se admite en el POST sin tope (sólo rechaza ≤ 0) y la interfaz no lo ofrece; las pistas de partida existen en el solver (`ModeloCpSat.sembrarHint`, `SolverHorario.resolverOptimizandoConSemilla`, de D23, con test sobre el instituto completo) y la aplicación no las usa. Decisión L: el tiempo de cálculo va a `O-pre-demo`, midiendo antes con `maxSegundos` en la VM; lo demás, a la prueba del profesor, que probablemente bloquea. S183: `O-pre-demo` abierto. S184: la parte de tiempo la cubre la condición 1 de `O-pre-demo`; la irreproducibilidad sigue. S187: otra corrida a 600 s en la VM, FEASIBLE con objetivo 233 (219 en S182). **S190:** sede de la replanificación de H5: sin cambio de sede. La irreproducibilidad que quedaba «en la prueba» pasa a `O-prueba-secretaria` y `O-comparacion`. |
| D-prevalidacion-ciega-a-holgura-cero (la prevalidación da vía libre ante el caso más difícil) | O-demo (C-generación) | No | Nace en S117. Sobre el catálogo real la prevalidación devuelve **ERROR=0 y AVISO=0**, y sin embargo 26 de los 28 grupos deben llenar sus 30 tramos EXACTOS. La causa es de diseño: `GRUPO_SOBRECARGADO` exige demanda MAYOR que los tramos disponibles, y 30 sobre 30 no lo supera. Distingue «imposible» de «posible», pero no «cómodo» de «al límite», que es la distinción que gobierna si habrá solución en el presupuesto dado. Se registra por su efecto COMBINADO: vía libre → generar → sin señal durante la espera (D-generacion-sin-indicador) → «no hay horario factible» que es falso (D-timeout-como-infactible). El arreglo natural es un AVISO cuando la holgura de un grupo es cero, y su valor depende de que alguien lo lea: se decide junto con la señal de espera. **AFINADA en S118 por dos vías.** (1) FOTOGRAFIADA: la generación que el arquitecto lanzó desde el navegador mostraba «Catálogo sano: sin hallazgos de pre-validación» sobre el centro donde 26 de 28 grupos van a 30/30; deja de ser dato de arnés y pasa a ser lo que el usuario lee. (2) SUPERFICIE AMPLIADA: sobre el catálogo VACÍO la prevalidación también devuelve `[]` y solo lo caza `ModeloCpSat` al construir el modelo, luego no es ciega solo a la holgura cero, es ciega al centro recién instalado. S118 resolvió el DESENLACE de ese caso (422 CONFIGURACION_INCOMPLETA en vez de un 500 accidental), no el aviso. La mitad del «efecto combinado» que la ficha registraba se ha deshecho: sus dos hermanas están CERRADAS. **FOTOGRAFIADA A LA INVERSA en S129, y esta ficha resultó ser además el DISPARADOR que otra deuda daba por desconocido.** El M4 de S129 necesitaba provocar un hallazgo, y S121 había registrado en `D-prevalidacion-contraste-sin-ver` que «nadie sabe qué lo dispara»: la respuesta llevaba escrita aquí desde S117 —`GRUPO_SOBRECARGADO` salta cuando la demanda supera los tramos—. Al meter una actividad de más, el panel reportó **`1B-A 37 / 30`**, luego el grupo estaba EXACTAMENTE en 30 y la actividad sumó 7: es evidencia directa y fotografiada de lo que esta ficha afirmaba por conteo. Lección de método asociada: una ficha de deuda es documentación consultable, no sólo un registro de lo pendiente. No se paga ahora **MEDIDA POR TERCERA VEZ en S142, sobre el centro completo y contra el endpoint vivo:** `GET /api/prevalidacion` devuelve `[]` y el panel pinta «Catálogo sano: sin hallazgos de pre-validación» sobre el mismo centro que a continuación **agota los 600 s enteros y termina en `FEASIBLE` con cota inferior 0**. La misma sesión midió la consecuencia de UI que la ficha no registraba: con la lista vacía, `generar()` (`horario-view.ts:342`) va directo a `lanzarGeneracion()`, porque el diálogo `ConfirmarGeneracion` sólo se abre si hay algún aviso de severidad `ERROR`. Sobre este centro ese diálogo es INALCANZABLE, así que todo lo que asevera `confirmar-generacion.spec.ts` es cierto e inejercitable con estos datos **Y LO QUE LA FICHA NO DECÍA, medido en S142:** `PrevalidacionService` comprueba EXACTAMENTE TRES reglas —`REGLA_PROFESOR_SOBRECARGADO`, `REGLA_REPETICIONES_EXCEDEN_DIAS` y `REGLA_GRUPO_SOBRECARGADO`, `PrevalidacionService:64-70`—, y **S8 NO está entre ellas** pese a ser propiedad del CATÁLOGO y no del horario y a ser, según `VerificadorSolucion:43,56`, la ÚNICA comprobación que no usa la solución: es por tanto la única invariante verificable ANTES de generar, y sólo aflora DESPUÉS, por el diagnóstico. Eso convierte el «catálogo sano» en una afirmación sobre lo que la prevalidación mira, no sobre el catálogo. **Es la condición 3 del criterio nuevo de `O-ajuste-cierre` (§3).** **ANOTADA en S146, sin cerrarse:** la condición 3 se cumplió pagando sólo la mitad S8 —la prevalidación comprueba ya la única invariante verificable sin solución— y el panel dejó de decir «Catálogo sano»: sobre este centro dice ahora que no hay hallazgos y que eso no garantiza horario en el tiempo previsto. La ceguera a holgura cero SIGUE VIVA; lo que cambia es que la pantalla la declara en vez de ocultarla |
| ~~D-presupuesto-anunciado-espejo~~ (la espera anunciada en pantalla es un espejo manual del presupuesto real) **CERRADA S184** | O-pre-demo (condición 1) | — | Nace en S118 como precio honesto de haber hecho el presupuesto configurable. El texto del estado de espera dice «puede tardar hasta 10 minutos» y esos minutos salen de una constante `MINUTOS_ANUNCIADOS` del componente, no del backend: no existe ningún endpoint que publique `educhronos.solver.max-segundos`. Basta arrancar con `--educhronos.solver.max-segundos=N` para que la pantalla mienta EN SILENCIO, sin que nada falle ni ningún test se ponga rojo; medido en el propio M4, donde la corrida de 30 s anunciaba diez minutos. Documentado en el código como COTA ANUNCIADA y no como promesa. El arreglo —exponer el presupuesto por API y que la vista lo lea— es superficie NUEVA, no un ajuste. No se paga ahora. S183: segundo espejo en `confirmar-generacion.html`. **CERRADA en S184** (`e056918`, condición 1 de `O-pre-demo`): el diálogo de confirmación ofrece 10, 20, 30 y 60 minutos, la interfaz envía `maxSegundos` y los textos de espera salen del valor elegido; `MINUTOS_ANUNCIADOS` se elimina. |
| ~~D-guion-exit-enmascarado~~ (un guion anunció como éxito un BUILD FAILURE) **CERRADA S157** | Transversal, con el script de R4 pendiente desde S101 | — | Nace en S117 y la corrigió Claude Code dentro de la sesión: la plantilla capturaba `EXIT=$?` después de un `echo`, midiendo el código de salida del `echo` y no el del comando. El daño no fue más allá porque la salida se leyó entera, pero la plantilla viene de sesiones anteriores y nadie ha auditado cuáles la usaron. Misma familia que el hallazgo de S109 (los tests de endpoint asertaban sobre `MockHttpServletResponse` y no sobre el cuerpo de red): un instrumento que mide otra cosa distinta de la que se cree. → sesión de Higiene/Método, junto al script de R4, al que añade un caso concreto. No se paga ahora  **CUARTO Y QUINTO CASO en S121, los dos detectados por Claude Code y no por el asistente que escribió los guiones.** (4) Un comprobador de «el `@import` del CDK sigue siendo la primera regla» usaba `^\s*[@.:a-zA-Z*]`, que casa con las líneas de continuación de un comentario `/* */`: daba verde sin medir nada. (5) El recuento de cierre de C-sustitución contaba `src/**/*.css` incluido `styles.css`, donde los literales DEBEN vivir, así que su objetivo declarado («0 al terminar») era inalcanzable por construcción. Se añade el caso más peligroso de la familia, señalado por Claude Code y no sufrido: **`var(--token-inexistente)` NO rompe el build** —la declaración se descarta en el navegador—, así que un build en verde no prueba que los tokens referenciados existan. De ahí sale el comprobador de cuatro vías que S121 deja vivo para el resto de O-diseño (literales, `font-size` sin `var()`, `var(` mal formado, y tokens referenciados contra los definidos en `styles.css`, quitando comentarios antes de buscar). **SEGUNDA INSTANCIA, hallada en S123 durante el propio M1 que el script certifica:** `scripts/verificar-cierre.py:171-173` imprime las entradas de índice descuadradas pero NO las suma a `problemas`, y el `return` de la 178 solo mira esa variable; el script salió con 0 teniendo 30 entradas rotas entre los dos documentos. Quien encadene `verificar-cierre.py && commit` se lo traga. Detectada leyendo el fuente del verificador, no confiando en su código de salida. NO se corrige en S123 (R-deuda: no bloquea el criterio de O-navegación) y la corrección no es de dos líneas: subir §4 a fallo duro exige comprobar antes que los documentos pasan ese listón, y eso es cambio de método. → misma sesión de Higiene/Método. **TERCERA instancia en S124, y por el reverso:** no un EXIT que enmascara un fallo, sino guardas que miden lo que no toca. Un volcado salió VACÍO con EXIT=0 por suponer marcadores `INDICE:INICIO/FIN` en `diseno-navegacion.md`, que no los tiene; y otro imprimió números de línea RELATIVOS al fragmento tubado, que leídos como absolutos habrían editado la ficha equivocada. La guarda de vacío añadida después sí disparó, y disparó bien. Lección: la guarda mide el vacío, no la corrección. **SIETE INSTANCIAS MÁS en S129 —de la (6) a la (12)—, todas del arquitecto y todas con la MISMA causa raíz, que aquí se nombra por primera vez: patrones y rangos escritos DE MEMORIA teniendo a tres secciones de distancia el fichero que los define.** (6) `grep -rn -- '--e[1-6]' --include='*.css'`: el `--` desactivó el parseo de opciones, `grep` devolvió 2, `pipefail` lo propagó y el `\|\|` imprimió «cero usos» **justo debajo de los quince usos que acababa de listar** —variante peor de la familia: no enmascara un fallo, publica una conclusión falsa—. (7) Un `echo` de conclusión escrito ANTES de medir, y falso. (8) Buscar `--(peso\|linea)` cuando los tokens se llaman `--lh-*`: la sección calló el interlineado sin decir que no lo había buscado. (9) Un patrón con el orden invertido respecto a como se escribe TypeScript, con siete «cero coincidencias» falsos. (10) Buscar `title=` literal, perdiendo `[title]` y `[attr.aria-label]`, justo donde vivía el molde buscado. (11) Una regex que casaba también el comentario de la tabla y **borró cinco líneas de más en las siete plantillas**, cazada por Claude Code en el diff y no por la suite. (12) **REINCIDENCIA:** el `grep` sin coincidencias bajo `pipefail` volvió a matar el proceso dos guiones después de haberlo diagnosticado. **Segundo corolario operativo:** un guion de lectura no busca por nombres recordados; los deriva de lo que él mismo acaba de volcar. Y de (12): una lección escrita no basta si el siguiente guion se escribe sin releerla. **CUARTA Y QUINTA INSTANCIA en S128, las dos del arquitecto y las dos en guiones de LECTURA, que es variante nueva.** (4) Un volcado de inventario pedía `sed -n '40,80p' ruta-inexistente \|\| find …`: el `sed` falló, **el `\|\|` salvó la salida** y el volcado se vio bien, así que una ruta falsa sobrevivió tres turnos hasta reventar en un `git add`. (5) La guarda `grep -rl "EstadoLista" --include=*-lista.ts \| wc -l` casa con `estado-lista.ts`: el componente se contaba a sí mismo y el «1» que devolvió significaba CERO listas cableadas. Corolario operativo nuevo: **fallback silencioso en un guion de lectura, nunca**. Sigue viva **S130 SUMA SIETE INSTANCIAS EN UNA SOLA SESIÓN, seis del arquitecto**, y el dato que importa no es el siete: es que la séptima fue un `\|\|` de rescate escrito en un guion de lectura DESPUÉS de haber contado cinco en la misma sesión, contra el corolario que esa misma sesión llevaba citando desde el M0. Las otras seis: `^Results:` contra un Maven que prefija `[INFO]`; `^\| $D` contra filas tachadas; un patrón de espaciado que ignoraba la exclusión que la propia ficha declara; un `sed` que recortó la lista prometida justo antes del `ABORTA` que explicaba el vacío; un delimitador `^### ` sobre una entrada anterior sin cabecera, que volcó 376 líneas bajo un rótulo que prometía una; y `S129` escrito de memoria contra un documento que dice «Sesión 129». Sigue sin bloquear el objetivo activo: R-deuda aguanta por décima vez **INSTANCIA DE S132, y es la SEGUNDA FORMA —la de S123— reincidiendo dentro del M1 que el script certifica:** tras el primer guion de cierre el índice de este documento quedó con 14 entradas descuadradas y `verificar-cierre.py` salió con RETORNO 0, porque su §4 las imprime y no las suma a `problemas`. La detectó Claude Code por LEER la salida, no por el código de retorno. Es el mismo defecto que S123 diagnosticó leyendo el fuente del verificador y que sigue sin pagarse; lo nuevo es que ya no es un riesgo descrito sino un fallo consumado en un cierre real. R-deuda aguanta por undécima vez **RECUENTO ACTUALIZADO en S142: la ficha se quedaba en los CINCO de S121 y no registraba las TRES de S140 ni la de S141.** S142 no suma casos propios a esta ficha —los suyos son de `D-guion-busca-token-esperado`—, pero sí una VARIANTE nueva que conviene tener nombrada: **un filtro que parece discriminante y casa SIEMPRE**. `grep -i "bloqueo\|pin"` aplicado a líneas que contienen `Mapping` devuelve los catorce controladores del proyecto, porque «ma**ppin**g» contiene «pin»; y `grep "put(\|delete("` sobre el frontend SOBRE-casa `setInput(` («In**put(**») mientras SUB-casa las llamadas HTTP reales, porque `this.http.delete<void>(…)` rompe el patrón con el genérico. Un filtro que no filtra es un `exit 0` que no mide **DOS INSTANCIAS EN S152, las dos del asistente y las dos cazadas por Claude Code.** (1) Un `cmp` de comparación entre dos variantes del bundle corrió cuando una de ellas no había llegado a producir ficheros: imprimió cuatro veces «DISTINTOS» con el tamaño del segundo operando vacío, es decir, PUBLICÓ UNA DIFERENCIA MEDIDA donde sólo había un fichero ausente. Variante de la (6) de S129 —no enmascara un fallo, publica una conclusión falsa— y la lección propia es que la guarda tiene que ir sobre la EXISTENCIA de los dos operandos, no sobre el retorno de `cmp`. (2) Un guion de comprobación escribió `echo "--- $solver declarado…"`: con `set -u`, bash expandió su propia variable inexistente y mató el proceso ANTES de las dos comprobaciones que venían detrás, de modo que el guion se suicidó antes de medir. Hermana del `pipefail` de S129 y S130 por el efecto. **TERCERA, en el producto y no en un guion de lectura:** el `-Base` del `.ps1` aceptaba ruta relativa, así que un token suelto encadenado en la misma línea llegó como valor del primer parámetro posicional y el guion escribió toda su salida en una carpeta inventada, en silencio; pagada en el sitio con una guarda de ruta absoluta **CERRADA en S157, en dos mitades.** La del verificador, PAGADA: los índices descuadrados o ausentes suman ya a `problemas` (comprobación A8b y un mutante) y el recuento de la autoprueba se calcula en vez de escribirse a mano. La de la forma de escribir guiones pasa a Deuda de MÉTODO, integrada en M-guion de `metodo.md` (normas 1, 2 y 4). **CERRADA** |
| D-resize-observer-jsdom (el `ResizeObserver` de la rejilla lanza `ReferenceError` en cada fixture, en silencio) | Transversal, con la sesión de Higiene/Método | No | Nace en S127, destapada al leer la sección `stderr` de una corrida CON FALLOS: jsdom no define `ResizeObserver`, así que el callback de `afterNextRender` de `horario-grid.ts` revienta en TODOS los fixtures de la rejilla. Es PREEXISTENTE —viene de S126, `git show` lo sitúa en la línea 167 del fichero de entonces— y es invisible en verde, porque vitest sólo imprime `stderr` cuando algo falla. Daño real y acotado: `repartirAltura()` nunca corre en tests, luego `altoCelda` nunca se fija y **el disparador por `altoCelda` del `afterRenderEffect` de D11 está muerto en jsdom**. Los casos (28) y (29) pasan por el OTRO disparador, `celdas()`, así que de los dos caminos que el cableado declara la suite sólo ejercita uno; el que compensa que el `ResizeObserver` corra fuera del ciclo de render lo verificó M4 y nada más. Arreglo natural: un doble de `ResizeObserver` en el setup de tests, que además haría comprobable ese segundo camino. NO se paga en S127 por R-terminado —no bloquea el criterio 4, y su verificación la aporta M4—, y tocar el setup global de tests dentro de un Cambio sin cerrar añade riesgo a cambio de nada que el objetivo pida. Es de la familia de `D-guion-exit-enmascarado`: un instrumento que no mide lo que se cree que mide |
| ~~D-hallazgo-E-refutado~~ (el Hallazgo E afirmaba dos cosas que el centro y los datos desmienten) **CERRADA S137** | O-demo | — | Nace en S134. GH6 no es tutor de 1º Bach A (dicho por el centro) y PTVE no lo imparte el tutor (Mejías Márquez lo da a 1B-B y 1B-D). Documento de dominio con estado falso: familia de `D-tokens-inexistentes`. No se corrige sin saber quién es el tutor real; espera a `D-tutores-bachillerato`. **S136 mide su huella en la BASE:** el `requiere_tutor = 1` de las 6 actividades `Bloque-PTEV/PTVE_Relig` desciende de este Hallazgo y es falso en sí; bajarlo a 0 NO depende de saber quién es el tutor real, pero el orden sí importa |
| ~~D-tutores-bachillerato~~ (faltaban los tutores de los 7 grupos de Bachillerato) **CERRADA S137** | O-demo | — | Nace en S134. Deuda de REQUISITOS: el dato vive en el sistema del centro y en ningún PDF entregado. La heurística PTVE está refutada. Se cierra preguntando. **S136 la mide y la ENCARECE:** las 7 filas actuales son PORTANTES para S8 en las seis actividades de bloque, así que la corrección es un par indivisible (reescribir las 7 + bajar `requiere_tutor` en las 6) y lo segundo exige base sin horario y REGENERAR. La ausencia no es cobertura parcial del PDF: 80 profesores frente a 59. **Es la única cosa que separa a O-demo de su criterio, y no es trabajo técnico** |
| ~~D-nombres-sin-codigo~~ (el PDF de profesores usa nombres completos; el catálogo, códigos) **CERRADA S136** | O-demo, en C-presentable | — | Nace en S134. Resuelta por medición en S136 y, sobre todo, ESCRITA en la cabecera de esa sesión, que es lo que la cierra: 16 pares nombre↔código para los ordinarios de ESO y FPB, y los 5 PDC heredando el del padre. Casado por PREFIJO, porque varios `nombre_completo` llegan truncados a 24 caracteres. **CERRADA** |
| D-censo-profesores-80-59 (80 páginas de profesor frente a 59 profesores cargados) | O-demo | No | Nace en S134. Hipótesis SIN MEDIR: las 21 de más serían personal sin horario lectivo. Se anota como hipótesis, no como explicación. **S136 la debilita:** ninguno de los 59 carece de plaza —incluida ORI1—, luego los 21 restantes no son personal sin horario de ESTA base sino gente ausente del catálogo. No afecta al cruce de tutorías: los 17 con línea `Tutor:` están los 17 en la base **S199:** en 2026/2027, 62 páginas de profesor frente a 59 profesores; las 3 de más no tienen ninguna clase con grupo (2 celdas de guardia, 1 de reunión y una vacía). Apoya la hipótesis para este curso, sin demostrarla. |
| D-itinerario-como-grupo (un itinerario entero concentrado en un grupo administrativo, tercera forma de partición no modelada) | Sin sede (S141 cerró O-particiones) | No | Nace en S134 al medir 4º ESO: Humanidades/CCSS vive en 4ºC y por eso Latín y Economía son clase de grupo completo. No impide cargar ni generar; importa al CONSTRUIR un 4º por la UI. **Remodelar 4º ESO no es trabajo de O-demo** **S189:** el inventario (fila 3) da el hueco general: no hay concepto de itinerario; ver `D-matricula-fuera`. |
| ~~D-taller5-inexistente~~ (el aula Taller 5 no está en el catálogo) **CERRADA S135** | O-demo, en C-centro-completo | — | Nace en S134. **PAGADA en `beb044a`** dentro del Cambio que cargó las 11 de FPB, tal como su ficha preveía. Tipo `TALLER_FPB` elegido por DESCRIPCIÓN y no copiando el de Taller 4, para no heredar sin examinar el criterio que denuncia `D-aula-tipo-sin-uso-real`. El catálogo pasa a 44 aulas y el horario de S135 le asigna 25 sesiones, todas de 2FPB. **CERRADA** |
| D-fuente-tercera-sin-usar (`Horarios de profesores.pdf` es probablemente mejor fuente que las dos cruzadas en S116) | Sin sede | No | Nace en S134. Trae profesor, asignatura, aula y grupos en la misma celda. Decidir si compensa rederivar. **AFINADA en S135:** los once están ya extraídos a texto y consultados para una pregunta concreta, así que «ninguno está analizado» deja de ser cierto; lo que sigue sin decidir es la rederivación |
| D-aula-tipo-sin-uso-real (la tipificación de `aula.tipo` no sigue el uso real del centro) | O-demo | No | Nace en S135. Taller 2 y Taller 4 tipados `TALLER_FPB` con la rejilla VACÍA; Taller 3, el único que imparte FPB de verdad, tipado `ORDINARIA`. Medido que hoy no decide nada: el solver no ve el campo (`TipoAula` vive sólo en persistencia) y `AsignaturaAulaCompatible` tiene CERO filas. Arreglarlo obliga a revisar los 43 tipos y no bloquea; el riesgo es el día que esa tabla se llene **S189:** fila 10 del inventario: la secretaria entrega el uso de las aulas, que está en el tipo de las 44, con esta ficha como salvedad. La capacidad (fila 10b) se guarda, pero nada la usa. |
| ~~D-guion-busca-token-esperado~~ (los guiones de lectura buscan el token esperado en vez del vigente) **CERRADA S157** | Transversal, con la sesión de Higiene/Método | — | Nace en S135 con TRES instancias medidas en una sola sesión: el `grep` que pasó por encima de `ESCRITURAS_CARGA_COMPLETA = 804`, el SQL contra `horario`/`tramo_id` cuando el esquema dice `horario_generado`/`tramo_inicio_id`, y la lectura del conteo de suites por «vitest 316» (vigente: 419). Familia de `D-guion-exit-enmascarado` y causa raíz nombrada en S129, aparecida en instrumentos de LECTURA. Contención practicada y candidata a norma: gatear toda escritura con corrida en seco revisada **RECUENTO ACTUALIZADO en S142: la ficha se quedaba en las TRES de S135 y no registraba las CINCO de S141 ni las TRES nuevas de S142.** Las tres de S142 son **del asistente** y las cazó **Claude Code**: (1) y (2) dos `grep -rn` contra `app/frontend/src/app/components/horario`, ruta que NO EXISTE —los directorios reales son `horario-grid/` y `horario-view/`—, con `2>/dev/null` tapando el `exit 2`, de modo que dos secciones enteras («¿existe selección de celda?» y «¿dónde vive el mensaje de conflicto del arrastre?») midieron CERO y no lo parecieron; (3) un `having n>=1` que lo cumple toda fila y que además contaba FILAS DEL JOIN en vez de sesiones distintas, así que los `n` de 4 y 2 eran el abanico de subgrupos×grupos y no colisiones —misma forma que la bolsa fetch-joineada de S139—. Se anota una CUARTA de la misma familia, de tipo distinto: el chequeo de «los grupos que responde la API deben ser 28» NO discriminaba, porque la base de REFERENCIA también tiene 28; lo que sí discriminó fue leer el fichero que el proceso tenía abierto en `/proc/<pid>/fd` **INSTANCIA DE S144, del asistente y cazada por Claude Code:** un ancla de sustitución copiada del texto renderizado, con los `**` de negrita pegados al literal en el fichero crudo — cero ocurrencias. Variante nueva: aparece en un guion de ESCRITURA sobre documentación, no de lectura, donde una sustitución a ciegas habría editado el bloque equivocado. **CERRADA en S157: integrada en M-guion** de `metodo.md` (normas 3, 4 y 8). Instancia de la propia sesión, del asistente: un ancla escrita desde la copia de `metodo.md` del Project, anterior a S152; la guarda de ancla única la paró sin escribir nada, y de ahí la precisión de la norma 3. **CERRADA** |
| D-cargador-tutoria-pisa (el cargador emite un PUT por entrada y el PUT es reemplazo total) | `O-comparacion` | No | Nace en S136. `cargar-centro.py` recorre `catalogo["tutorias"]` y envía un PUT con lista de UN elemento; dos entradas del mismo grupo se PISAN, la segunda borra a la primera. Hoy no muerde porque no hay co-tutores en el catálogo; el día que se añadan los cinco de ORI1, los PDC quedarían sin `TUTOR_PRINCIPAL` y S8 se rompería en cinco actividades. La prevalidación no lo ve (valida grupo, profesor, rol y «>1 principal», no «grupo repetido»); sólo lo cazaría el informe final, después del hecho. Arreglo: un PUT por grupo con la lista completa; el oráculo de 816 no se mueve. Familia de `D-guion-exit-enmascarado` **S199 (decisión del usuario):** sede de `O-demo`, cerrado, a `O-comparacion`, el único objetivo planificado que lee los datos oficiales. Instancia en 2026/2027: las 5 PDC imprimen un segundo tutor (`ORI1`) que el catálogo no lleva. **S199 (tras F1):** las escrituras previstas ya no son la constante 816: `cargar-centro.py` las calcula del catálogo (816 en 2025/2026 y 852 en 2026/2027, con test). El arreglo tiene que contar un PUT por grupo; la cifra no cambia mientras no haya co-tutores. |
| ~~D-catalogo-meta-enganosa~~ (el `_meta` del catálogo declaraba «no hay importador» y sí lo hay) **CERRADA S137** | Transversal | — | Nace en S136. La frase es literalmente cierta —la app no lee el fichero— y prácticamente engañosa: `cargar-centro.py` lo lee y escribe con él el centro entero, y S136 midió que es la SEDE ÚNICA de la estructura. Un lector concluye que el fichero es inerte cuando es la fuente de la base y la sede donde aterriza cualquier corrección de catálogo. Familia de `D-tokens-inexistentes` |
| D-preguntas-sin-cola (las preguntas que deja abiertas un entregable no entran en ninguna cola) | Transversal, con la sesión de Higiene/Método | No | Nace en S136 con el caso que la nombra: la ambigüedad **A5** de `ESPECIFICACION-CATALOGO.md:530` (S115) pidió la lista oficial de tutores por ser «de las cosas más baratas de confirmar» y señaló que afectaba a S8 en 22 actividades. Nadie la pidió en veinte sesiones, y S136 gastó cinco pasadas en volver a formularla. No es olvido: esa recomendación no es deuda de §4 ni tarea de ningún Cambio, luego nada la vuelve a poner delante. Tercera instancia en dos sesiones junto a la nota invisible de `INFORME-RECONCILIACION.md` (S135) y a `D-catalogo-meta-enganosa`. Arreglo candidato: que las «Preguntar:» de un entregable nazcan como deuda clasificada en el mismo cierre |
| D-meta-invariantes-a-mano (los invariantes que el catálogo declara son prosa que nadie recalcula, y nada verifica S8 antes del hecho) | Transversal, con la sesión de Higiene/Método | No | Nace en S137 con su primera instancia medida. `_meta.verificacionInvariantes` afirma «S8: OK - 22 actividades requiereTutor…» y sobrevive intacto a los cambios que lo vuelven falso; `cargar-centro.py --prevalidar` valida I2, I4 e I7 pero NO S8, que sólo se mira al enviar. Si el par indivisible de S137 se hubiera aplicado a medias, ni la herramienta ni el fichero lo habrían delatado: la red existe, pero está en la capa 1, es decir después de 816 escrituras y diez minutos de solver. S137 escribió el recorrido plazas → subgrupos → grupos → tutor como arnés desechable y DISCRIMINA (6 fallos exactos en el escenario a medias), así que el arreglo tiene punto de partida. Familia de `D-declarado-sin-artefacto` **ANOTADA en S146, sin cerrarse:** la aplicación comprueba ya S8 ANTES de generar (`GET /api/prevalidacion`, como AVISO), así que la red deja de estar después de los diez minutos de solver; sigue estando después de las 816 escrituras, porque `cargar-centro.py --prevalidar` sigue sin mirarla y `_meta` sigue siendo prosa. Viva por esa mitad |
| D-catalogo-candidatos-huerfano (el campo `candidatos` del catálogo no tiene semántica documentada ni consumidor, y ha quedado heterogéneo) | Transversal, con la sesión de Higiene/Método | No | Nace en S137. Residuo del derivador de S115: significaba «quién dedujimos que podría ser tutor» y no lo lee nadie. En S137 quedó VACÍO en las 7 entradas de Bachillerato, porque contenían GH6, FIL2 y EFI3, material del Hallazgo E refutado; se decidió vaciar y NO sustituir por el código oficial, porque rellenar un campo sin semántica escrita es la misma clase de error que la sesión deshacía. Consecuencia: 7 entradas a `[]` y 21 con el valor derivado. Definir qué significa antes de rellenarlo. Familia de `D-tokens-inexistentes` |
| ~~D-pin-ocupada-no-persiste~~ (el mismo gesto de arrastre persiste el pin sobre celda vacía y no sobre celda ocupada) **CERRADA S145** | O-ajuste-cierre | — | Nace en S127 de una DISCREPANCIA entre las dos pasadas de su M4, y se registra sin diagnóstico porque no lo hay. Primera pasada, arrastre sobre celda OCUPADA: la interfaz mostró «1 pines sin aplicar — regenerar» y la base no tenía ni una fila nueva —`sesion_bloqueada` y `aula_bloqueada` a 0, y la comparación tabla por tabla contra el original no encontró ninguna diferencia de contenido en las 21 tablas; el md5 sí cambió, pero sólo por el `change_counter` de la cabecera SQLite, 883 → 885, dos transacciones que no escribieron ninguna fila—. Segunda pasada, arrastre sobre celda VACÍA en 1FPB: el aviso **sobrevive al F5 y al cambio de grupo**, luego ahí sí está persistido en el servidor. Las dos explicaciones plausibles —que el primer drop no llegara a completarse, o que ocupada y vacía se comporten distinto— NO se han medido, y elegir una sería inventar. Lo establecido son los dos hechos. Es comportamiento del ajuste manual, no de la navegación, así que su sede es O-ajuste-cierre y no O-navegación (R-terminado). Quien la pague empieza por reproducir la primera pasada con la pestaña Red abierta. **CERRADA en S145 POR EXTINCIÓN DEL CAMINO, y sin diagnóstico: se dice así y no se finge otra cosa.** La discrepancia de S127 nunca se explicó. Lo que ha ocurrido es que el gesto que la producía ya no existe: soltar sobre celda ocupada dejó de emitir un pin y pasa a emitir un intercambio, y crear un pin tiene ahora gesto propio. El camino nuevo se midió sobre celda OCUPADA —un candado vive sobre una instancia, luego sobre celda ocupada por definición— y PERSISTE: `POST /api/bloqueos` 200, el aviso pasó de 1 a 2 pines, `sesion_bloqueada` ganó su fila, y el despinado la devolvió a 1. La hipótesis que quedaba viva —que la causa estuviera en el cliente y mordiera también al intercambio— queda descartada por medición: el intercambio sobre celda ocupada salió 200 y escribió |
| ~~D-vista-horario-sin-horario~~ (la vista de horario recibe a un centro recién configurado con dos mensajes de error) **CERRADA S161** | O-demo | — | **Bloqueo:** No bloquea, pero MUERDE EN LA DEMO. Nace en S120, medida en navegador sobre una base recién poblada a mano por la interfaz: antes de generar nada la vista pinta «No se pudo cargar el diagnóstico.» y «No se pudo cargar el horario 1 (404).» junto al mensaje correcto de prevalidación. El 404 es la respuesta CORRECTA del backend —no hay horario todavía— y lo que está mal es que el cliente trate «aún no hay horario» como fallo en vez de como estado inicial. Cae de lleno en la cláusula «presentable al centro» del criterio de O-demo: es lo primero que ve un usuario que acaba de configurar su centro, y por tanto lo primero que vería el jefe de estudios en la demo. Hermana del hallazgo de S118 sobre D-prevalidacion-ciega-a-holgura-cero, que tampoco distingue el centro recién instalado. Arreglo natural: distinguir 404 de error y pintar estado vacío. Es COMPORTAMIENTO y no aspecto, así que O-diseño no lo cubre (mismo criterio que S118 aplicó al estado de espera). No se paga en S120: no bloquea el criterio y la sesión no escribió producto. **DECIDIDO EXPRESAMENTE en el M0 de S121, como pedía el encuadre de S120: queda FUERA del criterio de O-diseño** y sigue colgando de O-demo, por el corte comportamiento/aspecto que la propia ficha ya establecía. Se registra el incómodo que eso deja: O-demo está bloqueado por un correo, así que si nadie la mueve la demo se hace con dos mensajes de error en la primera pantalla. Queda anotada como CANDIDATA A CAMBIO CORTO DE O-DEMO antes de la demo, y se hace constar que su clasificación «No bloquea» es discutible en lectura estricta del texto del criterio («presentable al centro»); reclasificarla es decisión del arquitecto y en S121 no se reclasifica **CERRADA en S161, de paso, por `C-horario-vigente`** (`653b638`): entrando por «Horario», un curso sin horario ve el aviso «Este curso todavía no tiene horario» con «Generar» habilitado, sin el error del diagnóstico ni el 404 (M4 de S161, U4). El 404 queda sólo para una URL directa a un id que no existe, donde es la respuesta correcta. **CERRADA** |
| ~~D-selectores-sin-busqueda~~ (los selectores de entidades son multiselect nativo sin buscador ni filtro) **CERRADA S185** | O-diseño, o el objetivo que rehaga el formulario de actividad · S182: `O-pre-demo` (decisión L) | — | Nace en S120 al construir a mano la actividad de seis plazas: poner dos subgrupos en una plaza se hace con ctrl+clic sobre una lista plana, igual que profesores y aulas. Con los 13 subgrupos del ejercicio funciona sin fricción y el arquitecto lo resolvió sin dudar; el problema es de ESCALA y está cuantificado: el centro real tiene 334 subgrupos, 59 profesores y 43 aulas. No es hueco funcional —todo se puede construir— y por eso NO abrió un C-hueco-*. Mejora futura; no se paga (R-terminado: no cambia el criterio de O-demo) **S182:** se amplía a todos los selectores de entidades con más de 10 elementos, incluidos los desplegables Grupo, Profesor y Aula de la vista de horario, que son `<select>` sin filtro (`horario-view.html:11-27`; ensayo, T2). Reutilizar la normalización de `catalogo/busqueda.ts`. S183: `O-pre-demo` abierto. **CERRADA en S185** (`630bb71`, `fc25486`, `7c9f6b0`, condición 5 de `O-pre-demo`): `FiltroOpciones` en `components/filtro-opciones/`, con `coincide()`, en los 9 selectores de la medida (a); aparece con más de 10 opciones, muestra «N de M» y nunca oculta lo elegido. El Ctrl+clic del multiselect nativo sigue: pasa a `D-selectores-combobox`. |
| D-actividad-forma-implicita (la forma de una actividad se deduce en vez de declararse) | O-diseño | No | Nace en S120 de dos observaciones del mismo formulario que son la misma cosa. (1) La opción «— varias (una por plaza) —» del selector de asignatura se ENCONTRÓ pero resultó CONFUSA, y es lo que distingue un bloque de destinos alternativos de una actividad ordinaria. (2) La co-docencia no se declara: se obtiene poniendo dos profesores en una plaza y nada nombra el concepto. La propuesta del arquitecto (un check explícito que, apagado, limite el selector a un profesor) queda registrada como una opción entre varias, no como diseño decidido. Absorbe la tercera observación del mismo paso: con seis plazas rellenas la legibilidad baja a «regular». Mejora futura; no se paga |
| D-arranque-no-literal (la orden de arranque contra otra base no está escrita, y lo que se escribió sobre ella falló al ejecutarse) | Transversal, con la sesión de Higiene/Método | No | Nace en S120 con dos hechos medidos. (1) El literal exacto de la invocación con ruta absoluta no existe en el repo: el plan la DESCRIBE y no la CITA, así que S117, S118, S119 y S120 la han reconstruido cada una por su cuenta y en S120 esa reconstrucción costó un intento fallido. (2) El plan describe que un segundo argumento se añade dentro del mismo `-Dspring-boot.run.arguments=` separado por comas, y al teclearlo así la coma NO separó nada: la base se creó con el nombre literal `educhronos-s120-manual.db,--educhronos.solver.max-segundos=60`. NO se afirma que el plan describa mal el mecanismo —no se ha investigado si la sintaxis exige otro escapado— y suponerlo sería inventar; lo establecido es que la forma tecleada no funciona y cuál sí. Tercer caso concreto para la sesión de Higiene/Método, junto a D-guion-exit-enmascarado y D-tokens-inexistentes: el literal que funciona se escribe, no se describe. No se paga ahora  **TERCER HECHO en S121, y de la misma familia aunque no sea de arranque:** el asistente pidió los `git add` del cierre dando por hecho un árbol sin commitear que había reconstruido de un turno anterior en vez de leerlo, y produjo dos commits cuyos mensajes no describían su contenido (uno duplicaba palabra por palabra el asunto de otro anterior). Nada se perdió y se corrigió con `reword` antes de pushear, pero la lección es idéntica: **el estado se lee, no se reconstruye.** **CUARTO HECHO en S129, y el primero que PAGA el corolario sin cerrar la deuda:** el grep confirmó que sigue sin existir ningún `.sh` ni bloque ejecutable con la orden y que la única cita del plan conserva un `<ruta …>` en el hueco, así que la reconstrucción se hizo por quinta sesión consecutiva. Esta vez la orden se PROBÓ y se ESCRIBIÓ literal en la entrada de S129, con su prerrequisito (`mvn -pl solver install -DskipTests`). Escribir el literal no cierra la deuda —su arreglo es un `.sh` versionado, superficie nueva fuera del alcance del tramo— pero deja de obligar a la sexta reconstrucción **INSTANCIA NUEVA en S130, y es la propia deuda mordiéndose la cola.** La orden literal que esta deuda obligó a escribir levanta el backend SIN frontend desde un árbol recién limpiado: los cuatro plugins que construyen y copian el bundle (`install-node-and-npm`, `npm-ci`, `npm-run-build`, `copiar-frontend-a-static`) cuelgan de `prepare-package`, y `spring-boot:run` para en `test-compile`, así que `target/classes/static/` no existe. En S129 funcionó porque el directorio estaba poblado por casualidad de un empaquetado anterior de esa misma sesión: se escribió el literal que funcionaba ese día, no el que funciona. Corregida a TRES pasos y probada en S130, con el `mvn -pl app package -DskipTests` intermedio; el literal vive en la entrada de S130, HOY ARCHIVADA en `bitacora-sesiones.md:8812` (el puntero caducó en S140, misma forma que la deuda) **SEDE CORREGIDA en S157:** el script de R4 ya no está pendiente; lo que queda de esta ficha es el `.sh` de arranque versionado, que es trabajo de Higiene y no del verificador. **INSTANCIA de S179:** la orden de `scripts/verificar-cierre.py` no está escrita en ningún documento; se leyó del fuente. |
| D-sin-puntos-de-ruptura (no hay un solo `@media` en todo el frontend) | O-diseño, como mejora futura | No | Nace en el M2 de S121 al inventariar la superficie visual: CERO media queries en `src`, luego la aplicación no es responsive por CSS en absoluto. Se DEJA FUERA del criterio de O-diseño con argumento, no por olvido: añadir puntos de ruptura duplica el trabajo del objetivo y no sirve al producto real —bundle de escritorio, jefe de estudios en portátil, rejilla 6×5 y tablas de configuración sin diseño móvil pensado—. El criterio pide en su lugar que las vistas no se rompan en UNA resolución declarada, la de la demo. Si algún día hay uso en tableta, esta es la sede |
| ~~D-controles-nativos-sin-neutralizar~~ (antes `D-select-nativo-desparejo`: ningún control con adorno nativo del navegador se había neutralizado) **CERRADA S132** | O-diseño, C-revisión (tramo 3) | — | Nace en el M4 de S121 sobre los `<select>`. **CORREGIDA en S129** (su texto decía que «nadie los estila» y era falso: llevan la clase de su formulario) y **MEDIDA POR FIN en el M4 de S130**, que era lo único que le quedaba vivo: un `<select>` con la clase de un `<input>` SÍ sigue pintando su fondo gris y su flecha nativa —visto en «Editar aula» y «Editar actividad», en la misma columna que `<input>` blancos—, luego alcanza a los **13** y no a los 2. El censo de S129 decía 14 porque contaba una mención dentro de un comentario HTML (`tutoria-dialogo.html:33`): son 14 coincidencias de `<select` y 13 elementos, y queda escrito para que el siguiente que lo mida no vuelva a tropezar. **AMPLIADA en S130 y por eso RENOMBRADA**: los cinco `input[type=number]` enseñan sus flechas de contador, y hay dos `radio` y dos `checkbox` en la misma situación. Lo común NO es la clase que falta —todos la llevan— sino que el proyecto nunca ha neutralizado el adorno nativo, y eso no se arregla con `border-radius`: exige `appearance: none` y dibujar el adorno. El lado de RADIO quedó cerrado por el tramo 2. Sede: **C-revisión, TRAMO 3**. **PAGADA Y CERRADA en S132**, y con una corrección a esta ficha: la premisa de que había que «dibujar el adorno» era falsa para radio y casilla, que se resuelven con `accent-color`. Lo aplicado: 13 `<select>` con `appearance: none` y galón en SVG —no `::after`, que un elemento reemplazado no admite—, 5 `input[type=number]` sin flecha doble, radio y casilla con `accent-color`, y las diez reglas `__input` intactas. Deja un literal `#5A6673` dentro del SVG del galón, anotado en la decisión 1, que no rompe el criterio 1 porque éste prohíbe literales en el CSS de componente y no en `styles.css`. Costura pendiente: `plan_trabajo_horarios.md` conserva esta deuda con su nombre de S121 (ver `D-deuda-sin-sede-en-el-plan`). **CERRADA** |
| ~~D-horario-id-a-fuego~~ (la barra y la landing enlazan `/horario/1` con el id literal) **CERRADA S161** | O-curso (H4), condición 4 | — | **Bloqueo:** **Sí desde S158**: es la condición 4 de `O-curso`, y R-deuda permite pagarla dentro del objetivo. Nace en S128 al leer `app.html:5` y `landing.html:8` para reestilar la barra. Dos consecuencias, y la segunda no se había visto: en cuanto se genera un segundo horario el enlace sigue apuntando al primero, y **sobre `/horario/2` el `routerLinkActive` tampoco marca «Horario»** porque el prefijo no casa, así que el id a fuego apaga además el indicador de sección que el criterio 1 de O-navegación acaba de verificar. NO se arregla en S128 por razón de CONTRATO y no de alcance: `D-horario-irreversible` ya midió que no existe `GET /api/horarios`, luego el cliente no tiene forma de saber cuál es el horario vigente. Cualquier arreglo empieza por ese endpoint. Se paga en O-demo, que es donde se bautizan y se listan horarios de verdad **PASA A BLOQUEAR en S158:** es la condición 4 de `O-curso`. Los horarios se acumulan y el curso archivado debe mostrar el último; con el enlace fijo mostraría el primero. Medido en S158 que sigue sin existir `GET /api/horarios`. **CERRADA en S161 por `C-horario-vigente`** (`f43f00e`, `653b638`): `GET /api/horarios/vigente` y la ruta `/horario`, que resuelve el vigente al navegar; la barra y la landing enlazan `/horario`, y «Horario» se marca también sobre `/horario/N`. Verificado en navegador con dos horarios: desde Configuración «Horario» lleva a `/horario/2`, también en el curso archivado. **CERRADA** |
| ~~D-contador-se-apaga-con-error~~ (el contador de la cabecera desaparecía ante un error de borrado, con la tabla llena debajo) **CERRADA S129** | O-diseño, C-revisión (tramo 1) | — | Nace en el M4 de S128 sobre Asignaturas: con el 409 de borrado en pantalla y las filas cargadas, el rótulo pierde su número. La causa es `[mostrarContador]="!cargando() && !error()"`, que viene de S124 y que C-identidad NO tocó: **es la misma mentira que S124 revirtió en la tabla, viva en el contador**, y hermana de `D-vacio-miente-con-error`, cerrada en la misma sesión por el mismo argumento aplicado a otro elemento. No se paga aquí porque el arreglo exige distinguir error de CARGA de error de ACCIÓN, y eso cambia el contrato de `app-cabecera-lista` (dos señales en vez de una), fuera del alcance declarado de C-identidad. Se paga en C-revisión. **PAGADA Y CERRADA en S129, en su sede, y con un arreglo MÁS BARATO que el que esta ficha proponía:** no hacen falta dos señales, basta cambiar el CRITERIO a `!cargando() && <entidad>().length > 0`. De las dos opciones se eligió cambiar las siete plantillas y no mover la decisión dentro de `cabecera-lista`, porque lo que está mal es el criterio y no dónde vive —la alternativa habría derogado un javadoc deliberado para arreglar otra cosa, que es lo que S128 declaró fuera de alcance—. La mitad no obvia: la condición va sobre la lista CARGADA y NO sobre `visibles()`, porque con la filtrada una búsqueda sin resultados escondería el contador en vez de decir «0 de N». Esa mitad la destapó la campaña de mutación —el mutante `visibles()` SOBREVIVIÓ, y la supervivencia se predijo antes de correrla— y se cerró con un aserto en el caso (8). **CERRADA** |
| ~~D-desbordamiento-sin-etiqueta~~ (las dos marcas condensadas de la rejilla llevaban `title` y ninguna etiqueta accesible) **NACE Y CIERRA EN S129** | O-diseño, C-revisión (tramo 1) | — | Destapada por el censo de S129 al buscar el molde de `D-insignia-sin-leyenda` para reutilizarlo: la marca `+N` de plazas ocultas —el mecanismo que S127 construyó precisamente para que esas plazas dejaran de ser mudas— vivía a quince líneas de la insignia que S128 sí dotó del par completo, y un lector de pantalla sólo oía «más N». Ampliada por decisión del arquitecto a `.grupos`, mismo defecto en el mismo fichero: dejar la mitad arreglada obligaba a volver. El `aria-label` REPITE el `title` literalmente y no mejora su redacción —las cadenas son de S127 y de D6, y cambiarlas habría sido afirmar algo nuevo sobre lo que devuelven `marcaOcultas`, `detalleInstancia` y `marcaGrupos`—. Cubierta por el caso (30) de `horario-grid.spec.ts`, que asevera la IGUALDAD con el `title` y no un literal, con no-nulo previo porque borrar los dos atributos dejaría `null === null` en verde. Coste en altura: cero. **CERRADA** |
| D-tokens-sin-uso (tres tokens de `styles.css` no los usa nadie) | O-diseño, C-revisión (tramo 3) | No | Nace en el M2 de S129 al recorrer los 46 tokens de `:root` uno a uno: `--radio-s`, `--fuente-datos`, `--color-ok-fondo` y `--color-info-fondo` no tienen un solo consumidor. NO se retiran, y la razón es de método: borrar es un cambio sin criterio detrás, y dos de ellos completan parejas de la tabla de la decisión 1 —`ok` tiene la tinta viva y el fondo muerto, `info` sólo tiene fondo—. Se marcan en el fichero como «sin uso hoy», que es estado vivo correcto (R5). El caso que hay que decidir con cuidado es `--fuente-datos`: `.cuenta` de `panel-prevalidacion` es su candidato natural, y estrenarla ahí EN SOLITARIO dejaría una familia tipográfica que aparece una sola vez en toda la aplicación, peor que no usarla. La decisión es binaria y no se toma de paso: o se usa donde toca —todos los códigos y horas— o se retira. `--radio-s` se decide junto con `D-select-nativo-desparejo`, cuyo censo lo destapó. No se paga ahora **ACTUALIZADA en S130: baja de cuatro tokens a TRES.** `--radio-s` deja de estar sin uso: el tramo 2 lo redefine de 3 a 4 px y le da 21 consumidores, que es la decisión binaria resuelta por el lado de usarlo y no por el de retirarlo. Quedan `--fuente-datos`, `--color-ok-fondo` y `--color-info-fondo`, verificados sin un solo `var()` en todo el CSS. El caso delicado sigue siendo `--fuente-datos`, por la misma razón de siempre. Sede: **C-revisión, TRAMO 3**. **RESUELTA EN S132 SALVO UN TOKEN.** `--fuente-datos` se resuelve por el lado de USARLA: gobierna con la clase `.dato` en 9 sitios —la columna Código de las siete listas y las dos horas de jornada— y excluye la rejilla, como cambio de la decisión 2 de `styles.css`; el binario que esta ficha declaraba se rompe con criterio medido y no de paso. `--color-ok-fondo` SALE DE LA COLA como decisión consciente: no tiene trabajo porque la decisión 5 se lo dio a la tinta. Queda vivo **`--color-info-fondo`**, que nombra un trabajo inexistente; conservarlo lo decidió Claude Code dentro del bucle y se ratifica en el cierre, con su retirada anotada como trabajo de una línea para la próxima vez que algo toque `:root`. Baja de TRES tokens a UNO |
| D-748-sin-derivacion (el número que gobernó una pregunta abierta durante tres sesiones nunca se escribió con su derivación) | Transversal, con la sesión de Higiene/Método | No | Nace en S128 al intentar cerrar los «32 px sin explicar» de S126. La causa candidata está medida —`.app__contenido` declara `padding: 1rem 0`, 32 px exactos, en el contenedor de la vista que ni S126 ni S127 miraron— y coincide al píxel, pero **no hay contra qué contrastarla**: el plan dice «los 748 que el arquitecto había calculado» y en ningún documento del repo consta cómo se calcularon; reconstruirlos desde el viewport no cuadra. Misma lección que `D-arranque-no-literal` por el lado del número: lo que se describe y no se cita, no se puede verificar después. Arreglo de una línea —escribir la fórmula al lado del número la próxima vez que se calcule un presupuesto—. Los 32 px NO se reclaman (R-terminado: subiría `--alto-celda` ~4,5 px y cambiaría el recorte de un criterio cumplido) |
| D-declarado-sin-artefacto (un documento afirma haber declarado algo que no está escrito en ninguna parte) | Transversal, con la sesión de Higiene/Método | No | Nace en S130 al buscar el precedente de declaración de redondeos de S128. La regla escrita en `styles.css` exige que un redondeo se DECLARE; el único precedente dice DOS VECES que la cumplió —esta ficha y `plan_trabajo_horarios.md`— y no existe el artefacto: ninguna lista en `styles.css`, ninguna en los documentos, y ningún commit del repo tiene cuerpo, así que el mensaje de commit tampoco era sede posible. El coste NO es teórico: al reconstruirlos desde `cd6b43f..94b97df` resultaron ser CUATRO y no tres, un dato falso que sobrevivió dos sesiones porque nadie podía contarlo. Tercera de la familia junto a `D-arranque-no-literal` (el literal que se describe y no se cita) y `D-748-sin-derivacion` (el número sin su derivación). Arreglo de una línea de método: «se DECLARA» significa que existe una lista enumerada con fichero, propiedad, literal y delta, no una frase que diga que se declaró. **SEGUNDA INSTANCIA, medida en S145 y con el daño ya materializado.** La entrada de S144 afirma «queda escrito el censo completo para quien monte el gesto» refiriéndose a los cinco pares multiplaza, y enumera UNO: el que la propia S144 usó. El arnés que los produjo (`ArnesIntercambioS144Test.java`) se borró antes del cierre, así que los otros cuatro no existen en ninguna parte —comprobado por grep sobre plan, bitácora, `scripts/` y `tools/`—. El daño no es hipotético: S145 fue a buscarlos para el M4 y tuvo que gastar el turno en comprobar que no estaban. Es la misma familia que `D-arranque-no-literal`: se describe un artefacto y no se cita, así que el siguiente que lo necesite lo vuelve a producir. NO se re-barre en S145 (R-deuda: no bloquea, y el par documentado bastaba y era el mejor de los cinco). Lo que sí se paga aquí es la afirmación falsa, corregida en la entrada de S144 **S154:** Claude Code anunció haber «guardado en memoria» el fallo del e2e; la sede es `D-e2e-centro-minimo-rojo`. **INSTANCIA de S179, registrada en S180:** en el cierre de S179, Claude Code guardó en su memoria dos notas —dónde acaba una ficha que contiene un bloque de código, y cómo se cuenta el contador de Higiene/Método—; como en S154, la memoria de Claude Code no es sede recuperable. El umbral del contador sí está escrito (tabla de tipos de `metodo.md`: 20 fichas de §4 con sede Higiene/Método o script de R4); lo que no está escrito es cómo se cuenta: qué columna se lee y que las filas tachadas no cuentan (S179: 23 filas, 12 tachadas, 11). No se paga ahora (R-deuda). |
| ~~D-jerarquia-declarada-sin-aplicar~~ (`styles.css` declaraba la jerarquía de acciones como aplicada, y no lo estaba) **CERRADA S132** | O-diseño, C-revisión (tramo 3) | — | Nace en el M4 de S130. `styles.css:59-63`, dentro de la decisión 1, dice «JERARQUÍA DE ACCIONES, por relleno y no por color (aplicado, se declara)». Medido: en toda la aplicación hay **un solo** `background: var(--color-acento)` y es la barra; ningún botón lleva relleno de acento; `__cancelar` y `__guardar` comparten una sola regla en los siete formularios; los siete `__borrar` de fila llevan sólo `color`, sin superficie ni borde; y en `pdc-dialogo` principal y destructiva son idénticas. Censo de botones: 10 reglas con caja y radio, 9 con una sola propiedad, 8 sin ninguna regla —los siete `__editar` de fila y `.cabecera-lista__nuevo` salen con el botón por defecto del navegador—. NO es diseño nuevo: los tres tokens existen y la regla está escrita desde S129; falta ejecutarla. Arrastra dos consecuencias: el criterio 3 no puede darse por cumplido mientras el fichero afirme «aplicado» sobre algo falso, y la pregunta del verde que S129 zanjó con «ya se resuelve por relleno» no está resuelta sino cerrada. Es la familia de `D-declarado-sin-artefacto`, salvo que aquí lo que falta no es el registro sino el producto. **PAGADA Y CERRADA en S132**, en su sede: reglas globales sobre `button` en `styles.css` y 32 bloques retirados de 20 hojas de componente —14 `.accion-principal`, 9 `.accion-destructiva`, secundaria por defecto, `:disabled` como acento apagado— más `.accion-compacta` como nivel de TAMAÑO en las 16 acciones de fila, que es la variante que eligió el arquitecto en el navegador. **El censo de esta ficha era CORTO**, remedido sobre `82ec04c`: 54 `<button>` en 24 plantillas y no 34 en 10; caja y radio 10 ✓; una sola propiedad 10 y no 9; 15 clases sin regla y no 8. Con ella se levanta el bloqueo del criterio 3 y la deuda bloqueante del proyecto vuelve a 1 (`D31-a`). **CERRADA** |
| D-avisos-como-bloque-fijo (los avisos del horario ocupan una banda fija en vez de plegarse tras un indicador de estado) | Transversal, sin objetivo asignado; SIN SEDE desde S141, que cerró O-particiones | No | Nace en el M4 de S130, propuesta del arquitecto con argumento propio: sustituir la banda de texto por un botón con icono de estado —verde, aviso, error— que abra el detalle en un panel aparte liberaría alto para la rejilla, que es el presupuesto medido del criterio 4 de O-navegación. Queda FUERA de O-diseño por R-terminado y R-invalidación a la vez: cambia la INTERACCIÓN y no el acabado, y O-particiones toca frontend después. Se registra con su argumento para que no se pierda |
| ~~D-prevalidacion-contraste-sin-ver~~ (aviso y error del panel se distinguían SOLO por color de texto, y no se habían visto juntos) **CERRADA S129** | O-diseño, C-revisión (tramo 1) | — | Nace en el M4 de S121 por un riesgo que la sesión NO pudo cerrar. C-sustitución colapsó cuatro ámbares (`#c80`, `#a60`, `#b8860b`, `#8a6100`) en `--color-aviso` (#8A5A00) y tres rojos (`#b00`, `#b00020`, `#c33`) en `--color-error` (#A32014), y en `panel-prevalidacion` los mensajes de severidad se distinguen solo por `color:`, sin fondo. Para verlos juntos hay que provocar un hallazgo, y ni la documentación del proyecto ni el asistente saben qué lo dispara: se registró como NO SABIDO en vez de mandar a probar a ciegas. Se verifica en C-revisión leyendo el componente antes. Si se confunden, el arreglo es un cambio de valor en `:root` y se propaga solo, que es justo lo que C-tokens compró. **CERRADA en S129 POR CONSTRUCCIÓN, con dos correcciones a esta ficha.** (1) **La verificación que pedía no era difícil: es IMPOSIBLE.** Medido en el backend: `Severidad` tiene dos valores y las tres reglas de `PrevalidacionService` emiten `ERROR`; **nadie emite `AVISO`**, ni en `main` ni en los tests, y el propio enum documenta que se conserva por contrato y como candidato del palomar de aulas. Los dos colores no coexisten, así que no se pueden ver juntos. (2) **Tenía DOS caras y la ficha sólo veía una:** además de las filas, los dos contadores de la cabecera eran números desnudos y consecutivos distinguidos sólo por la tinta, y **medido con la fórmula WCAG los dos tokens tienen 1,28:1 ENTRE SÍ** cuando a un elemento no textual portador de información se le piden 3:1; en el estado colapsado —el habitual— eran lo único en pantalla, luego el color no era redundante, era la información. El arreglo NO fue el cambio de valor en `:root` que esta ficha preveía: rótulo VISIBLE junto a cada número, severidad pintada como TEXTO en cada fila con el valor crudo del enum, y marco del panel tomando la severidad máxima con contrastes medidos antes de escribirlos (6,35:1 / 4,97:1 / 13,16:1 sobre `--color-error-fondo`). Al dejar el color de ser el único canal, la verificación imposible deja de ser condición para cerrarla: mismo mecanismo que mató a `D-vacio-miente-con-error` en S128. **CERRADA** |
| D-subgrupos-di-sin-api (18 subgrupos de los grupos PDC sin ninguna vía de gestión) | Sin sede | No | Nace en S138. `PdcService` crea y borra exactamente UN subgrupo por PDC, `<codigo>-Completo`, y son 5 en la base. Pero el predicado `esMonoDiDePdc` que veta PUT/DELETE en `/api/subgrupos` casa «un solo grupo Y ese grupo es `DIVERSIFICACION_PDC`», y eso son 23: los 5 troncos más **18 desdobles de Religión/Atención Educativa** (`3ºCDi-ATED`, `3ºCDi-Rel` y hermanos) que **no posee nadie**. `/api/subgrupos` los rechaza con un 400 que remite a `/api/grupos/{idPadre}/pdc`, y ese sub-recurso sólo conoce el `-Completo`: **el mensaje de error manda al usuario a una puerta donde el recurso no está.** La ironía es medible: el predicado excluye con un párrafo de javadoc el caso del subgrupo con varios grupos PDC —el «tronco A8 compartido», del que hay CERO instancias— mientras deja fuera de la API 18 subgrupos que sí existen. Sin sede: el arreglo toca el agregado PDC y no cae en O-particiones (cuyo gesto de prueba es un grupo ORDINARIO) ni en O-ajuste-cierre; colgarlo de O-estructura, TERMINADO en S114, equivaldría a decidir que no se paga nunca |
| D-borrado-pdc-integridad-500 (violación de FK latente al borrar un grupo PDC) | Sin sede | No | Nace en S138. `PdcService.borrar` guarda sólo por las plazas del `-Completo` y no mira los otros 18 subgrupos de `D-subgrupos-di-sin-api`. Reconstruido el escenario sobre COPIA con `foreign_keys=ON`: borrado el mono-Di y hecho `flush()`, quedan 2 filas de `subgrupo_grupo` apuntando al grupo, y el DELETE del PDC revienta con `FOREIGN KEY constraint failed`. **Medido en la capa SQL, NO en la respuesta HTTP**: lo que llega al usuario depende de cómo traduzca Spring esa `DataIntegrityViolationException`, y lo probable es un 500 donde debería haber un 409 modelado. **Hoy NO es alcanzable** —los cinco `-Completo` tienen 10 plazas cada uno, así que el 409 de plazas salta antes—, pero deja de serlo en cuanto un PDC se quede sin usar su tronco. Se registra con el escenario reconstruido para que quien lo pague no lo redescubra |
| D-codigo-actividad-tilde (`EFis-4ºA+4ºADi` frente a `EFís-4ºD+4ºDDi`) | Transversal, con la sesión de Higiene/Método | No | Nace en S138 de paso. La misma asignatura aparece con y sin tilde en el código de dos actividades. `codigo` es UNIQUE, así que no rompe nada hoy; el daño es de ORDEN —ordena mal en cualquier listado— y sobre todo de DERIVACIÓN: el día que algo agrupe por prefijo de código, estas dos caen en cubos distintos sin que nada avise. Misma familia que la circularidad del mapa de códigos de grupo que S119 eliminó |
| D-i3-sin-datos (la invariante I3 no está ejercitada por el centro completo) | Transversal, con la sesión de Higiene/Método | No | Nace en S138. `asignatura_aula_compatible` tiene **0 filas** en la base de referencia, luego la validación I3 de `ActividadService` (compatibilidad asignatura↔tipo de aula) no la ejercita ningún dato del centro real; sólo hay 84 filas de `plaza_aula_candidata`. Es la familia de `D-meta-invariantes-a-mano`: una invariante declarada viva que el juego de datos de referencia no toca, de modo que un fallo en ella no lo destaparía ninguna carga ni ninguna generación. No se decide aquí si el arreglo es poblar la tabla o retirar la invariante: eso es trabajo de su sesión |
| D-javadoc-plazas-caducado (el javadoc de plazas describía una regeneración de códigos que la reconciliación no hace) | O-particiones | No | Nace y se PAGA en S139, en tres sedes: el javadoc de clase de `ActividadService` y el de `Actividad.actualizar`, ambos con un `{@link}` a `limpiarPlazas`, método que ya no existe —lo sustituyó `reconciliarPlazas`, que precisamente CONSERVA los códigos—, y la contradicción entre `PlazaRequest` («inestable entre ediciones») y el ctor de `Plaza` («ESTABLE: no cambia mientras la plaza sobreviva»). Se paga en el camino y no en Higiene porque MORDIÓ en su propia sesión: esa contradicción llevó a escribir en el contrato de `C-replicación-alta` que las decisiones nombraran plazas por código, y el contraste de M4 tuvo que corregirlo a `id`. Mismo argumento con que S137 pagó `D-catalogo-meta-enganosa`. **CERRADA** |
| D-post-horario-sin-sesiones (el POST de generación devuelve la proyección sin sesiones) | O-ajuste-cierre (cerrado S146) | No | Nace en S114, medido en red. El horario se persiste bien (3 filas en `sesion`) y el `GET /{id}/proyeccion` las devuelve, pero el cuerpo del POST llega con `sesiones: []`. Inocuo HOY por una razón concreta y no por suerte: la UI recarga por `paramMap` y nunca lee el cuerpo del POST (decisión de S93, recargar por GET fresco). Muerde a cualquier cliente futuro que se fíe de la respuesta —incluido un e2e que quisiera asertar sobre ella—. **CONFIRMADA A ESCALA REAL en S116:** el POST sobre el centro completo devolvió `sesiones: []` con 770 filas en `sesion`. La ficha lo predecía desde S114; deja de ser hipótesis. No se paga ahora **CAUSA MEDIDA en S142, y deja de ser sólo síntoma.** Son dos piezas y hacen falta las dos. (a) `guardar()` (`GeneradorHorarioService:256`) persiste la cabecera y luego `sesionRepository.saveAll(sesiones)`, pero NUNCA mantiene el lado inverso: la colección es `@OneToMany(mappedBy = "horario", fetch = LAZY)` sobre un `HorarioGenerado` creado con `new`, así que su `ArrayList` sigue vacío en memoria. (b) `open-in-view` NO está declarado en ningún `application.properties`, luego vale su defecto `true` —de ahí el `WARN` del arranque—, y toda la petición HTTP comparte UN contexto de persistencia, de modo que el `findById` de `proyectar()` devuelve la instancia cacheada en primer nivel en vez de releer de la base. **RASTRO que lo prueba:** `fechaGeneracion` viaja como `…892639871Z` en el POST (nanos, el `Instant.now()` en memoria) y como `…892Z` en el GET (milis, truncado por SQLite). **TERCERA Y CUARTA CONFIRMACIÓN a escala real en S142**, sobre el centro completo y con 819 filas de `sesion` persistidas en cada caso. **REFUTA el comentario de `GenerarHorarioEndpointTest:58-66`**, que atribuye la colección vacía a «artefacto del harness, no de producción, donde cada método `@Transactional` abre su propio contexto»: es FALSO —con OSIV activo producción se comporta IGUAL que el `@DataJpaTest`, no al contrario—. Y por eso la suite no puede verlo: el test `post_conCatalogoFactible_devuelve200YProyeccionConSesiones` promete en su nombre más de lo que mide, porque sobre el cuerpo del POST sólo asevera `$.id` y `$.estadoSolver`; el `$.sesiones` isNotEmpty se comprueba en un GET posterior, tras `flush()+clear()`. **EL ARREGLO YA ESTÁ ESCRITO EN EL REPO**, en el hermano que sí funciona: `BloqueoService.aulasDe()` reconsulta el repositorio (`aulaBloqueadaRepository.findByActividadAndIndice`) en vez de fiarse de una colección inversa, y por eso el POST de `/api/bloqueos` sí devuelve su colección completa. No se paga **QUINTA CONFIRMACIÓN en S143, y la ficha deja de poder decir «inocuo hoy»:** durante el desarrollo de `C-mover-sesion-backend` la colección inversa vacía produjo 404 en TODOS los movimientos e `INSTANCIA_SIN_COLOCAR` en el diagnóstico, y costó una corrida entera antes de identificarse. Es la primera confirmación que cuesta trabajo en vez de describir un cuerpo de respuesta raro. El servicio nuevo lo esquiva reconsultando en TRES puntos —localizar las filas, reconstruir la solución y responder—, es decir, el arreglo escrito en `BloqueoService.aulasDe()` se ha aplicado por tercera vez a mano en lugar de pagarse en su causa. Sigue sin pagarse: tocar `open-in-view` es transversal y no bloquea el objetivo activo (R-deuda) **RECLASIFICADA en S144, decidida en su M0 y ejecutada aquí.** Pasa de «Mejora futura» a deuda técnica real por la definición de §4: mejora futura es «algo que falta pero no está mal», y aquí hay algo mal hecho y medido —`guardar()` no mantiene el lado inverso y `open-in-view` corre con su defecto—, con coste ya cobrado en S143. **SEXTA confirmación en S144:** el servicio del intercambio vuelve a esquivarla reconsultando en los tres puntos, con lo que el arreglo escrito en `BloqueoService.aulasDe()` se ha copiado a mano por CUARTA vez. **Sigue sin pagarse** y NO bloquea: tocar `open-in-view` cambia el comportamiento de toda la superficie REST, y eso no es «de paso» (R-deuda). **S175:** vista otra vez en el M3 de `C-e2e-verde` (POST 200 OPTIMAL con `sesiones: []` sobre el centro mínimo). Absorbe el duplicado `D-post-generacion-sin-sesiones` (S166). **S184:** otra vez en el M4 de F1 (POST 200 con `sesiones` vacía y 819 por GET del mismo horario); sin cambio. **NOTA de S192:** apagar open-in-view rompe hoy la generación: `cargarProblema()` se llama sobre `this` y solo tiene sesión gracias a él (ficha de `O-base-tecnica`, «Fuera del criterio»). Quien la salde por esa vía tiene que pasar antes `cargarProblema()` por una transacción. |
| D-espejo-vía-sin-coincidencia (el desplegable de reparto ofrece todas las vías del bloque sin relacionarlas con el nombre del espejo) | Sin sede | No | Nace en el M4 de S141, fotografiada: el selector de `4ºE-DIG` ofrece las CINCO vías del bloque `DIG_EXPRE_FOPP_TEC`, incluidas las de EXPRE, FOPP y TEC. Ni la pantalla ni el servidor relacionan el nombre del espejo con la asignatura de la vía —`validarAsignaciones` sólo exige que la plaza pertenezca al bloque del espejo—, así que el arquitecto, conduciendo el recorrido por primera vez, contestó bajando por la lista y mandó `4ºE-TEC` a una vía de FOPP, aceptado EN SILENCIO. Medido en 4º ESO: de las 4 decisiones, TRES son deducibles del propio nombre del espejo y sólo UNA (cuál de las dos vías de EXPRE) depende de la matrícula, que es el dato no deducible que S138 midió. Arreglo candidato: preseleccionar la vía cuya asignatura coincide, o advertir cuando no coincide; es superficie NUEVA y el criterio no la pedía (R-terminado). Sin sede porque O-particiones cerró en la misma sesión, y colgarla de un objetivo cerrado equivale a decidir que no se paga nunca |
| D-via-sin-quien-ni-donde (una vía de reparto llega al cliente sin profesor ni aula, y con el CÓDIGO de asignatura en vez del nombre) | Sin sede | No | Nace en el M4 de contraste de S141. `ViaDTO` es (plazaId, plazaCodigo, asignatura, gruposActuales, hermanoPresente); `asignatura` se construye con `plaza.getAsignatura().getCodigo()` pudiendo mandar `nombreCompleto`. NO se paga en S141 con argumento de riesgo y no de alcance: el dato está a mano —`planificar` es `@Transactional(readOnly=true)` y `Plaza` expone `getProfesores()` y `getAulaFija()`— pero traerlo cuesta agrandar un N+1 existente o ensanchar el fetch join de `findConPoblacionPorGrupo`, que es EL gesto que produjo el defecto de S140 (bolsa duplicada, 30 de 219 actividades mal clasificadas). **PRESIÓN MEDIDA A LA BAJA EN EL PROPIO M4:** las dos vías de EXPRE sí se distinguieron por `gruposActuales` (4ºA+4ºB frente a 4ºC+4ºD), luego la carencia no impidió decidir. Vuelve a subir si aparece un desdoble donde dos vías compartan asignatura Y grupos |
| ~~D-censo-r4-cuenta-menciones~~ (el censo de R4 cuenta menciones igual que citas, así que una nota sobre un token huérfano lo rescata) **CERRADA S157** | Transversal, con el script de R4 pendiente desde S101 | — | Nace en el cierre de S141 y se mide sola: el bullet de R4 registró que tres tokens bajaban a una cita tras el archivado, y al NOMBRARLOS les devolvió la segunda, con lo que el censo volvió de 29 a 26 en el mismo acto de documentarlo. El censo cuenta apariciones en el corpus vivo sin distinguir definición, cita de trabajo y mención incidental. Dos daños, y el segundo es el grave: una cifra registrada deja de ser reproducible con el instrumento que la produjo (familia de `D-declarado-sin-artefacto`), y **una mención en una entrada de sesión enmascara para siempre la orfandad futura de ese token**, que es justo lo que R4 existe para detectar. Es la familia de `D-guion-exit-enmascarado`: un instrumento que mide un conjunto que SE PARECE al que se cree medir. Arreglo candidato, para el script de R4: clasificar la aparición por su sede —definición en §4, cita de trabajo en código o en una ficha viva, mención en entrada de sesión— y no contar la tercera. Le da a ese script su caso más concreto desde S101 **PAGADA Y CERRADA en S157:** el censo cuenta sobre el plan SIN su entrada de sesión (de `### Sesión` a `Última fase completada`, abortando si una marca no aparece una vez) y lista aparte los tokens que sólo viven en ella. Sobre los documentos de HEAD pasó de 33 a 40 sospechosos, y seis de los siete nuevos son los que S155 y S156 declararon rescatados por su línea de R4. Comprobaciones A5, A5b y A5c y dos mutantes. La serie de cifras anterior no es comparable con la nueva. **CERRADA** |
| D-parametrizacion-muerta-en-ui (la parametrización de la generación existe en la API y es inalcanzable desde la aplicación) | Sin sede | No | Nace en S142, medido en código. `horario.service.ts:27` manda un cuerpo `{}` FIJO (`this.http.post<HorarioProyeccion>('/api/horarios', {})`), así que `nombre`, `maxSegundos`, `semilla` y `via` existen en `GenerarHorarioRequest` —y su javadoc da `D29` por cerrada para la vía de OPTIMIZACIÓN— pero NINGUNO es alcanzable desde la interfaz: el diálogo `ConfirmarGeneracion` sólo devuelve `true`/`false` y no recoge parámetros, y `lanzarGeneracion()` llama a `this.service.generar()` sin argumentos. Implementado en la API y muerto en la UI. Familia de `D-declarado-sin-artefacto`. Efecto lateral medido en la misma sesión: una generación disparada desde el navegador queda registrada con el nombre por defecto (`"Horario " + Instant.now()`), que es lo que permitió identificar el horario 3 de la copia como ajeno al guion. No se paga |
| D-bloqueo-id-no-estable (el `id` de un bloqueo no es estable por contrato) | Sin sede | No | Nace en S142. `BloqueoService.guardar()` implementa el reemplazo (D-4/D-5) BORRANDO el pin previo —`aulaBloqueadaRepository.deleteByActividadAndIndice` más `sesionBloqueadaRepository.delete`, con `flush()` para no chocar con la restricción única `(actividad_id, indice)`— y reinsertando, así que el `id` que devuelve el `BloqueoDTO` NO es el mismo objeto lógico entre dos altas de la misma instancia. En S142 volvió a salir `1` tras mutar el pin de tramo, pero eso es **reutilización de `rowid` de SQLite**, no una garantía del contrato: un cliente que cachee ese id acierta por suerte, y el `DELETE /api/bloqueos/{id}` es precisamente quien lo consume. No se paga |
| D-cast-sin-comprobacion-drop (`alSoltar` castea el `data` del CDK sin comprobarlo) | O-ajuste-cierre (cerrado S146) | No | Nace en S142, y el riesgo lo declara el propio código: `horario-grid.ts:454` hace `evento.item.data as InstanciaCelda`, un cast SIN comprobación que sólo es válido mientras el `cdkDropListGroup` de esa plantilla conecte únicamente celdas de esta rejilla, todas con `[cdkDragData]` de ese tipo. Si algún día se conecta otra fuente de arrastre —una paleta lateral, otra rejilla—, el cast pasa a ser mentira y falla en runtime sin que el compilador avise. Sede `O-ajuste-cierre` porque el punto exacto que toca es el del gesto de arrastre. No se paga |
| D-proyeccion-instancia-espejo (`releerInstancia` duplica el mapeo `Sesion` -> `SesionVistaDTO` de `GeneradorHorarioService.proyectar`) | O-ajuste-cierre (cerrado S146) | No | Nace en S143 y la declara el propio ejecutor sin que se le pregunte. El servicio del movimiento necesita devolver la instancia releída, y para eso mapea `Sesion` a `SesionVistaDTO` por su cuenta en vez de reutilizar `proyectar`. Es un espejo DELIBERADO y con precedente escrito: añadirle lógica a `GeneradorHorarioService` significa tocar el servicio de 13 repositorios (`D-F8.2b-iii-A-a`), que es exactamente la razón por la que `DiagnosticoService` vive aparte, y `BloqueoService` ya documenta el mismo trato como «espejo frágil, deliberado y consciente». Compensado con un test de contrato que compara ambas salidas —`elCuerpoDel200CoincideConLaProyeccionDeEsaInstancia`—, que es lo que convierte la divergencia en fallo visible en vez de silenciosa. Pero es duplicación real y divergirá si alguien cambia la proyección sin mirar aquí. Sede O-ajuste-cierre porque el objetivo está ABIERTO y es el que la introduce; colgarla de uno cerrado equivale a decidir que no se paga nunca. No se paga ahora **SEGUNDO CONSUMIDOR en S144:** `intercambiar()` llama a `releerInstancia` DOS veces por respuesta, así que el espejo pasa de un consumidor a tres llamadas y su divergencia sería más cara. Compensado igual que en S143, con el caso 16 de `IntercambioInstanciasEndpointTest`, que es el hermano de `elCuerpoDel200CoincideConLaProyeccionDeEsaInstancia` para las dos listas del cuerpo. Sigue sin pagarse. |
| ~~D-censo-r4-ciego-a-la-extincion~~ (el censo marca el token con UNA aparición y no ve el que cae a CERO) **CERRADA S157** | Transversal, sesión de Higiene/Método | — | Nace en S143, encontrada por Claude Code al cerrar y declarada por él. `scripts/verificar-cierre.py` lista como sospechosos los identificadores con exactamente UNA aparición en el corpus vivo, de modo que un token que pierde su última cita **sale de la lista en silencio**: deja de ser sospechoso por haber empeorado. Medido en vivo: al archivar S141, `C-alcance-particiones` cayó a cero apariciones en el corpus vivo —conserva 5 en la bitácora— y desapareció del censo, mientras cuatro tokens que sólo bajaron a una entraron en él. Es FAMILIA de `D-censo-r4-cuenta-menciones` pero NO la misma: aquella dice que el instrumento no distingue una definición de una mención; esta dice que es ciego al caso peor. El arreglo natural es que el censo cuente también los ceros, lo que exige una lista de tokens esperados y no sólo un recuento sobre lo que aparece. No se paga ahora (R-deuda) **PAGADA Y CERRADA en S157:** el censo compara contra `git show HEAD:` y lista los tokens vivos en HEAD que ya no lo están, como informe que no suma a fallos. Primera detección real en su propio cierre: el ejemplo literal de encabezado de objetivo en `metodo.md` (M-doc, punto 6), retirado a propósito al reescribirlo con `<nombre>`. Comprobación A6 y un mutante. **CERRADA** |
| D-candados-en-tabulacion (el candado permanente añade 30 paradas de tabulación por vista, 29 de ellas invisibles) | O-ajuste-cierre (cerrado S146) | No | Nace en S145 y la introduce el propio Cambio. Para que exista gesto de pin hacía falta afordancia, y el candado pasó de renderizarse solo sobre instancias pinadas a estar siempre en el DOM, oculto por `opacity` —no por `display:none` ni `visibility:hidden`, que lo harían infocalizable— y visible en `:hover` y `:focus-visible`. Coste MEDIDO y no estimado sobre el banco: una vista de grupo renderiza 30 instancias y añade 30 paradas, idéntico en los 28 grupos (min = mediana = max = 30, porque todos tienen clase en los 30 tramos); la vista de aula llega también a 30 y la de profesor a 19. El delta es de 0–1 a 30, y 29 o 30 de esos botones son invisibles mientras se tabula hacia ellos si el navegador no los desplaza a la vista. **Sede O-ajuste-cierre y NO O-diseño**, por el precedente escrito de `D-corte-lateral-a-1280`: la sede la pone quien la introduce, y colgarla de un objetivo cerrado equivale a decidir que no se paga nunca. La exclusión de accesibilidad que la ficha ya tenía se refiere al `title` sin foco, que es preexistente. **NO roza el criterio 4 de O-navegación**, comprobado y no supuesto: ese criterio pide «sin scroll vertical», que es altura, `.adornos` es `position:absolute` y los casos (27) y (28) de D11 siguieron verdes sin tocarlos. Salida natural el día que se pague: `tabindex="-1"` en los candados libres con navegación por teclado propia de la rejilla, que es rediseño de interacción y no un parche. No se paga (R-deuda) |
| ~~D-spa-sin-fallback-de-rutas~~ (el jar no reenvía las rutas profundas de la SPA al index.html) **SALDADA S155** | O-instalación (H4), condición 9 | — | **Bloqueo:** **Sí desde S151**: es la condición 9 de `O-instalación`, y R-deuda permite pagarla dentro del objetivo. Nace medida en el M4 de S145 y es PREEXISTENTE: no la introduce este Cambio. `http://localhost:8080/horario/1` devuelve 404 con la Whitelabel Error Page; hay que entrar por `/` y navegar desde la landing. Dos daños: cualquier M4 futuro que intente abrir una URL profunda pierde el tiempo antes de entenderlo, y no se pueden compartir enlaces a una vista concreta. **Sede H4** porque lo que falla es el empaquetado del jar servido, que es su superficie; H4 no está descompuesto en objetivos todavía, así que la deuda espera a su apertura. Queda además anotado en la nota de arranque de la bitácora: todo M4 entra por `/`. **REPRODUCIDA en S151 sobre el app-image**, en Linux y en Windows: F5 en la vista del horario da la Whitelabel Error Page, así que el daño no es sólo compartir enlaces, es recargar. Pasa a ser la condición 9 de `O-instalación`. **SALDADA en S155** por `C-rutas-spa`: `ResolvedorRutasSpa` y `RutasSpaConfig` en `es.yaroki.educhronos.app.config`, `spring.web.resources.add-mappings=false` en el `application.properties` de main y la ruta comodín en `app.routes.ts` |
| D-hora-tramo-dependiente-de-zona (el valor de hora persistido cambia con la zona horaria de la JVM que lo lee) | Sin sede desde S173: medido que no afecta a la CI (era H4, Fase 12) | No: MEDIDO en el M2-B de S149 con dos JVM sobre el mismo fichero. `GET /api/jornada` devuelve 08:00–14:30 con recreo 11:00–11:30, que es lo que imprime el centro; la condición 1 no queda bloqueada. Lo que la ficha decía —«las horas se guardan desplazadas respecto a las que el centro imprime»— era FALSO: el valor persistido, leído en la zona del centro, da la hora correcta, y el 07:00 de S147 fue el `sqlite3` leyendo el entero crudo como UTC. El defecto real, ahora medido, es de PORTABILIDAD: un `LocalTime` persistido como instante cambia de valor con la zona de la JVM que lo lee, y las 70 horas se desplazan 60 minutos exactos entre Europe/Madrid y UTC. SEDE REASIGNADA a H4 y Fase 12 en su día; **desde S173, sin sede**: medido que la CI en UTC no la hace fallar. No se paga en O-exportación: `/api/jornada` ya lo expone desde que existe el formulario de jornada, luego es preexistente y R-deuda manda | Nace en el M2 de S147. `tramo_semanal` guarda 07:00–13:30 con recreo 10:00–10:30 en el horario de referencia; la plantilla de `JornadaService` y el PDF del centro dicen 8:00–14:30 con recreo 11:00–11:30. Desfase exacto de 1 h en los 35 tramos. **Hipótesis muy probable, NO verificada:** `LocalTime` se persiste como instante en la zona horaria de la JVM que escribe. Encaja con la nota técnica de Fase 6 del plan —«vuelve intacto», medido leyendo con la MISMA JVM que escribió, prueba ciega a esto—. Consecuencias: el oráculo SQL no vale para las horas, que se comprueban contra el PDF del centro; y una JVM en otra zona leería otras horas —los runners de CI de Fase 12 corren en UTC por defecto—. Es técnica real y no mejora futura porque guardar una hora de reloj como un instante desplazado es una representación incorrecta aunque hoy nadie la vea. Se mide en el M2 del primer Cambio de O-exportación que imprima horas: primero qué devuelve `GET /api/jornada` sobre una copia, después cómo mapea el driver la columna. No se paga ahora. **S173:** `TZ=UTC mvn test` pasa entero (116 + 558), con el cambio de zona comprobado en las marcas del log. La suite escribe y lee con la misma JVM, así que es ciega a este defecto, como ya decía esta ficha: es de portabilidad de la base entre zonas, no de CI, y sale de la Fase 12. |
| ~~D-censo-r4-ciego-a-los-objetivos~~ (el censo de R4 no comprueba jamás un identificador `O-*`) **CERRADA S157** | Transversal, con el script de R4 pendiente desde S101 | — | Nace en el cierre de S147 y se da de alta en S148. El patrón del censo de `scripts/verificar-cierre.py` sólo captura `D-*`, `D\d` y `C-*`, así que ningún objetivo entra en el corpus: un `O-*` que se quede con una sola cita —o con cero— no lo ve nadie, y en S147 se comprobó sobre el caso concreto (`O-exportación` tenía 17 apariciones y su ausencia de la lista no probaba nada). El hueco no es sólo del guion: la ESPECIFICACIÓN de `metodo.md` lista `D-*`, `C*` y `§*`, y NO se sabe si el script cubre `§*`. Se paga en la sesión de Higiene/Método y con el aviso de `D-censo-r4-ciego-a-la-extincion` delante: una lista más corta puede ser peor que una larga. **PAGADA Y CERRADA en S157:** el patrón cuenta `O-*`, y la especificación de R4 en `metodo.md` deja de prometer `§x.y` y `Cx`: medidos, añadían nueve sospechosos y ninguno era un identificador del sistema (secciones y criterios de otros documentos). Comprobación A7 y un mutante. **CERRADA** |
| ~~D-proyeccion-sin-duracion~~ (la proyección no transporta la duración, así que sólo se conoce el tramo de inicio) **SALDADA S170** | `O-aceptación` (condición 3) | — | **Bloqueo:** Sí: condición 3 de `O-aceptación`, hasta S170. Nace en el M3 de S148, medida y no supuesta. `SesionVistaDTO` no lleva la duración y la tabla `sesion` sólo guarda `tramo_inicio_id`, de modo que la proyección —y con ella el CSV y los PDF que la consuman— sitúa cada sesión en UNA celda y no en el rango que ocupa. La vista tampoco la obtiene por otra vía: `grep -rni duracion` sobre `app/frontend/src` da 20 coincidencias, TODAS del catálogo de actividades, y 0 en `horario-view`, `horario-grid` y `horario.service.ts`. NO bloquea el criterio porque las 219 actividades del centro de referencia duran 1 tramo, pero el producto admite cualquier N —Hallazgo G del plan: el centro tiene un bloque de FPB de 3 tramos en el origen—, y en cuanto un centro use N>1 un PDF pintará una clase de dos horas como si fuera de una. **Instancia de S167:** primera vez vista en la interfaz; en el M4, un bloque de 2 tramos aparece sólo en su fila de inicio y la segunda se ve vacía. Sigue sin bloquear. **S168:** con un bloque de 2 tramos, el CSV sale con una fila, el PDF pinta sólo el tramo de inicio y el oráculo de exportación da OK sobre los dos ficheros. Pasa a bloquear por la decisión B de `O-aceptación` y se salda dentro de él. Ficha del plan escrita en S168. **SALDADA en S170** (`C-exportacion-bloques`): la duración viaja en la proyección y la rejilla, el CSV, los PDF y el oráculo cubren todos los tramos de cada sesión. Ficha del plan cerrada. |
| D-javadoc-csv-separador-sin-fuente (el Javadoc de `HorarioCsv` justifica el separador `/` con un campo que nunca llega al fichero) | O-exportación (cerrado S150) | No | Nace en el M2-C de S149. El comentario dice que se evita la coma porque `profesor.nombre_completo` la lleva («Apellidos, Nombre», 58 de 59, medido en S148): el dato es cierto pero ese campo NO viaja al CSV, que lleva códigos de profesor, y cero códigos contienen coma. La decisión del `/` sigue bien tomada —los grupos sí podrían llevarla—; lo falso es la razón escrita. Se arregla el comentario, no el código |
| D-pdf-error-de-maqueta-como-404 (un fallo al componer el PDF sale como «horario no encontrado») | O-exportación (cerrado S150) | No | Nace de la campaña de mutación de S149, mutante 16, SUPERVIVIENTE a propósito. El `catch` del endpoint atrapa `IllegalArgumentException` para traducir el id inexistente a 404; `HorarioPdf` lanza `IllegalStateException` en sus guardas justamente para no colarse por ahí, pero cualquier otra `IAE` que naciera dentro de la composición saldría como 404 sobre un horario que existe. Familia de `D-vacio-miente-con-error`. Arreglarlo bien es rediseñar el contrato de excepciones del controlador; no bloquea ninguna condición viva |
| ~~D-guion-pkill-casa-su-propio-envoltorio~~ (un guion que para la aplicación con `pkill -f` se mata a sí mismo) **CERRADA S157** | Transversal, con la sesión de Higiene/Método | — | Nace en el M2-B de S149 y ya costó una corrida: `pkill -f 'spring-boot:run'` casa con el `bash -c '<texto>'` que envuelve el propio guion, porque el patrón está dentro de esa cadena. Se llevó el guion por delante y dejó una JVM huérfana. El arreglo es guardar el PID al lanzar y matar por PID; el defecto vive en la forma de escribir los guiones, no en un guion concreto. **DOS VARIANTES MÁS en S151, las dos de COMPROBACIÓN:** `ps -eo pid,args \| grep -i '[E]duchronos'` dio cuatro falsos positivos, porque el truco del corchete no salva nada cuando el directorio o el comando llevan la ruta del repo; y `pgrep -a java` NO ve la aplicación empaquetada, cuyo proceso se llama `Educhronos` y no `java`, así que la guarda «ninguna JVM» da un falso limpio con el bundle vivo. Se comprueba por PID guardado, por `/proc/<pid>/exe` y por el puerto **S154, otra instancia:** en el cierre de un guion de verificación, `pgrep -f 'http.server 8080'` devolvió el PID del propio shell de Claude Code, pese a que el guion prohibía `pgrep` por patrón; comprobado con `ps -p`, sin daño. **CERRADA en S157: integrada en M-guion** de `metodo.md` (norma 5). **CERRADA** |
| D-jar-no-reproducible (dos construcciones del mismo commit dan jars distintos) | `O-ci` (Fase 12) | No | Nace en S152, medido por Claude Code: tres pasadas del mismo commit, tres sha256 distintos y el MISMO tamaño exacto. De las 399 entradas del jar, 201 llevan la hora de pared de la construcción y sólo las 153 del cargador de Spring Boot van normalizadas a 1980; el `MANIFEST.MF` no lleva marca de tiempo, así que el contenido es idéntico y lo que baila son las fechas del zip, que son de ancho fijo. `project.build.outputTimestamp` no está declarada en ninguno de los tres `pom` y es la palanca estándar. NO se paga en S152 (R-terminado: la condición 1 no pide reproducibilidad bit a bit) y su arreglo no es de una línea: antes del empaquetado hay un `npm ci` y un build de Angular, así que declarar la propiedad puede no bastar y hay que medirlo. Consecuencia práctica ya escrita en `docs/empaquetado.md` §7: el `SHA256SUMS` de la entrega verifica el TRANSPORTE y no identifica una versión, así que relanzar el guion de Linux invalida una `build/` ya copiada a Windows aunque no haya cambiado una línea. La identidad de versión la da el commit del `LEEME.txt`. Sube de presión el día que la Fase 12 quiera publicar artefactos por tag |
| D-jackson-dos-ramas (el jar lleva las dos ramas mayores de Jackson a la vez) | Sin sede | No | Nace en S152 de paso, al inventariar el peso del jar: en `BOOT-INF/lib` conviven `jackson-databind-3.1.4` y `jackson-databind-2.21.4`, 3,6 MB. No es sólo peso: dos ramas mayores de la misma librería en el classpath es riesgo de comportamiento, porque qué clase gana depende del orden de carga. NO se ha medido quién arrastra cada una ni si alguna está inerte, y suponerlo sería inventar; lo establecido es que las dos están. Quien lo pague empieza por un `dependency:tree` sobre `com.fasterxml.jackson`. No bloquea ninguna condición viva |
| D-guarda-escritura-sin-caso (la guarda `Files.isWritable` de `crearCarpeta` no la ejercita ningun caso) | `O-ci` (Fase 12; era O-instalación) | No | Nace en el M3 de S153, medida por mutación y no supuesta: suprimir la condicion `if (!Files.isWritable(carpeta))` de `crearCarpeta` SOBREVIVE a los 14 casos del spec. El único caso de carpeta imposible usa un fichero como padre, y ahí revienta antes `createDirectories`, así que esa línea no llega a ejecutarse nunca en la suite. NO se cubre, por dos razones medidas: un `chmod 0555` sobre un `@TempDir` no discrimina si la suite corre como root —y la Fase 12 traerá runners—, y en Windows `isWritable` no significa lo mismo que en POSIX, porque mira el atributo de solo lectura y no los permisos efectivos, de modo que puede dar por escribible una carpeta que no lo es. La guarda SE QUEDA porque su diagnóstico nombra la carpeta: sin ella el fallo sale después como SQLITE_CANTOPEN dentro de `DataSourceScriptDatabaseInitializer.runScripts`, sin decir dónde. Queda escrito en el javadoc de `crearCarpeta`. Se paga cuando la Fase 12 traiga runners, o con un doble del sistema de ficheros |
| ~~D-guion-escribe-donde-no-se-dijo~~ (un parámetro de ruta admite valor relativo y el guion escribe donde no se dijo) **CERRADA S157** | Transversal, con la sesión de Higiene/Método | — | Nace en S153 con TRES instancias medidas en dos sesiones, y ninguna la detectó el guion que la cometía. (1) El `-Base` de `empaquetar-windows.ps1` en S152 escribió en una carpeta inventada. (2) El `--salida` de `empaquetar-linux.sh` en S153: con ruta relativa, el volcado de la huella del JDK se resolvía DENTRO de un `cd` posterior, así que `SHA256SUMS` salía con una sola entrada y la entrega viajó 190 MB hasta que abortó el `.ps1` en la otra máquina. (3) Medida al demostrar el arreglo: sin normalizar, `--salida ./x` no cuelga del directorio desde el que se lanza sino de la raíz del repo, por un `cd "$RAIZ"` previo, y dejó 332 MB de entrega dentro del repo con `exit 0` y sin que nada fallase. NO es lo mismo que `D-guion-exit-enmascarado`, que es sobre la SEÑAL —un instrumento que dice verde cuando hay rojo—: aquí el guion termina bien y la operación aterriza en otra ruta. La instancia (2) pertenece a las dos familias y se cita en ambas fichas. Arreglado en `empaquetar-linux.sh` (S153): `--salida` se normaliza a absoluta nada más leerse, y el guion aborta si su propia entrega no queda con dos entradas. Queda VIVA porque el `-Base` del `.ps1` sigue sin normalizar y nadie ha auditado qué otros parámetros de ruta aceptan valor relativo → sesión de Higiene/Método. No se paga ahora **CERRADA en S157: integrada en M-guion** de `metodo.md` (normas 6 y 7). El resto de código que esta ficha daba por vivo no existía: el `-Base` del `.ps1` aborta con ruta relativa desde S152 (`IsPathRooted`), y la auditoría de S157 midió que no hay más parámetros de ruta que ése y el `--salida` del `.sh`, ya normalizado. **CERRADA** |
| ~~D-entrega-caducada-indetectable~~ (el `.ps1` verificaba la entrega contra el `SHA256SUMS` de su misma carpeta, así que una entrega vieja pasaba sus propias huellas) **SALDADA S156** | O-instalación, el Cambio de la condición 3 (medio de entrega) | — | Nace en S154 y costó una ronda entera de M4 en Windows: se construyó la entrega del principio de la sesión (jar `71d1299f…`, código de S153) en lugar de la v3 (`68af23ae…`); el guion dio las dos huellas por buenas y el bundle salió sin modo escritorio. Lo delataron tres pruebas ajenas al guion: el tamaño idéntico al byte al de S153, la línea `java-options` ausente y el `.cfg` sin la propiedad. El arreglo va con el medio de entrega: que la huella esperada viaje por un canal distinto de la propia entrega, o que se compare con la que imprime el guion de Linux. **SALDADA en S156** por `C-windows-limpio`: el `.ps1` exige `-HuellaJar`, que el guion de Linux imprime en consola y que no viaja en la carpeta; verificado en Windows con una huella ajena, que pasa `SHA256SUMS` y aborta. La defensa es de procedimiento, porque la huella del jar sigue en `SHA256SUMS`; quitarla de ahí la haría estructural (anotado, no aplicado) |
| D-reenvio-spa-sin-guarda-automatica (el cableado del reenvío de la SPA no lo protege ningún test automático) | `O-ci` (Fase 12) | No | Nace en S155. La LÓGICA del resolvedor sí está cubierta (7 casos, cuatro mutantes cazados), pero el registro de `ResolvedorRutasSpa` por `RutasSpaConfig` no: el proyecto no tiene ningún test con contexto de Spring, `mvn test` corre antes de que exista el bundle (se construye en `prepare-package`) y el e2e va contra `ng serve`. Previsiblemente, retirar `RutasSpaConfig` dejaría la suite verde y devolvería el 404 al pulsar F5 en el bundle (no medido como mutante). Hoy sólo lo verifica un M4 a mano. Arreglo barato: dos `curl` sobre el jar en CI o en el humo del empaquetado (`/horario/1` → 200 `text/html`, `/api/no-existe` → 404). No se paga ahora |
| ~~D-esquema-sin-version~~ (una base de una versión anterior no se adapta al esquema de la nueva) **CERRADA S191** | `O-base-tecnica` | — | **Bloqueo:** **Sí desde S178**: bloquea H5. **S178:** pasa a bloquear H5: durante la prueba del profesor habrá versiones nuevas, y la primera que cambie `schema.sql` dejaría su base sin adaptar; además, la base de la que parte, la del centro 2025/2026, es anterior al cambio de esquema de S159. Nace en S158, en el M0 de `O-curso`. `schema.sql` son 21 `create table if not exists`, así que una base existente no recibe columnas ni restricciones nuevas. No hay ningún `ALTER TABLE` ejecutable ni mecanismo de migración; `user_version` vale 0 y nadie lo lee ni lo escribe. `schema.sql` ha cambiado cuatro veces, la última en `d301b64` (2026-08-16). **CORREGIDO en S175:** el último cambio de `schema.sql` es `915b18e` (S159, duplicado del curso por `VACUUM INTO`), no `d301b64`, medido con `git log`. Al nacer la ficha, en S158, la cifra era exacta: `915b18e` es posterior y suma un quinto cambio. Hoy no muerde porque ningún centro ha recibido todavía un bundle. Muerde en la primera actualización que cambie el esquema: la base activa, y con `O-curso` también los cursos archivados, que viven años, se abrirían con una versión que espera otro esquema. Flyway sigue descartado por decisión escrita. Arreglo mínimo candidato, no medido: sellar la versión en `user_version` y negarse a abrir una base incompatible con un mensaje claro. Sube de presión con el primer cambio de `schema.sql` posterior a la primera entrega. No se pagaba; desde S178 bloquea H5. **S177:** `v0.1.0` es el primer bundle aceptado para entrega. La guía de distribución exige desde S177 un `git diff` vacío de `schema.sql` entre la versión instalada y la nueva antes de actualizar. **S180:** medido que NO bloquea `O-demo-bundle`: los bancos de S137 tienen las mismas 21 tablas que crea `v0.1.0`, con DDL idéntico, y el arranque solo añade la tabla `curso` (abrir un banco cambia su md5). Sigue bloqueando H5 desde el objetivo (3). **S190:** sede de la replanificación de H5: `O-base-tecnica`. Criterio, condición 1. **CERRADA en S191** (`b57d4bc`, condición 1 de `O-base-tecnica`): `PreparadorEsquema` sella la base, la migra con `esquema/NNN.sql` o la rechaza si es posterior, al arrancar y al cambiar de curso; una base posterior da 409 `CURSO_VERSION_POSTERIOR` o su motivo en el diálogo de arranque. |
| ~~D-version-invisible~~ (la aplicación no dice qué versión es) **CERRADA S193** | `O-base-tecnica` | — | **S178:** pasa a bloquear H5: cada incidencia del profesor tiene que decir qué versión corre. Nace en S177, MEJORA FUTURA. Ni la interfaz, ni el zip, ni `Educhronos.exe` llevan la versión: `jpackage` se llama sin `--app-version`, el pom dice `0.1.0-SNAPSHOT` y el tag sólo vive en el nombre de la Release. En la aceptación, que la actualización corría sobre el programa nuevo sólo pudo probarse por la huella del jar instalado. La guía pide anotar fuera qué tag se entregó a cada centro. Arreglo candidato, no medido: `--app-version` derivada del tag y la versión visible en la barra. No bloqueaba: el criterio de `O-ci` no la pedía (R-terminado); desde S178 bloquea H5. **S180:** NO bloquea `O-demo-bundle`: en la demo el bundle lo instalamos nosotros y la versión queda en el acta por el tag y el sha256 del zip. Sigue bloqueando H5 desde el objetivo (3), donde el profesor informa solo. **S190:** sede de la replanificación de H5: `O-base-tecnica`. Criterio, condición 2. **S192:** condición 2 a medias: la versión del tag llega al jar, al log y a `GET /api/version` (`94373c2`); faltan la línea en pantalla y la verificación sobre un bundle. **S193:** CERRADA con la condición 2: «versión X» debajo de la marca (`1bf9855`), verificada en `v0.3.0`, construida por CI desde su tag, en la VM. **CERRADA** |
| ~~D-playwright-cita-seed-difunto~~ (un comentario de `playwright.config.ts` explica una clase que no existe) **CERRADA S162** | O-curso, condición 8 | — | Nace en S158. `app/frontend/playwright.config.ts:42-43` explica que `SeedCatalogoRunner` no corre porque no se activa el perfil `seed`, pero la clase no existe ni hay ningún `@Profile` en el árbol, y tres javadoc del backend ya la llaman «el difunto» (`JornadaService.java:85`, `dto/JornadaDTO.java:13`, `JornadaEndpointTest.java:38`). Es un comentario caducado sin efecto en la ejecución. Se paga cuando el spec de duplicar curso toque ese fichero. **SALDADA en S162** por `C-e2e-curso` (`fae5470`). **CERRADA** |
| D-educhronos-props-sin-agrupar (la segunda clave `educhronos.*` llegó sin migrar a `@ConfigurationProperties`) | Sin objetivo asignado | No | Nace en S159. El precedente de `application.properties:76` pide migrar a un record `@ConfigurationProperties` al llegar la segunda clave `educhronos.*`; S159 añade `educhronos.datos.carpeta` (publicada por el post-procesador, no configurable) y la lee con `@Value` opcional. No se migró por R-deuda: tocaba `GeneradorHorarioService` y su test de presupuesto, fuera de `C-duplicado-guarda`. |
| ~~D-censo-r4-rescate-por-indice~~ (el índice generado rescata del censo los tokens de la cabecera de sesión) **CERRADA S179** | Transversal, con la sesión de Higiene/Método | — | Nace en el M1 de S158, medida por Claude Code. El censo de R4 retira del corpus la entrada de sesión (la viva y la previa degradada) pero NO su reflejo en el índice generado del plan, que reproduce la cabecera de la entrada. Así, un token nombrado en esa cabecera gana una aparición falsa y sale de la lista de sospechosos: en S158, `C-alcance-curso` pasó de 1 a 2 apariciones solo por la línea del índice. Es el mismo modo de fallo que S157 pagó con `D-censo-r4-cuenta-menciones`, por una puerta que aquella sesión no miró. Arreglo candidato, no medido: retirar también del corpus las líneas del índice que apuntan a las entradas retiradas. No se paga ahora. **CERRADA en S179** (`9c2898c`): el censo retira también, dentro del bloque de índice, las líneas que copian un encabezado del tramo retirado, casando por texto (caso A9 de la autoprueba). |
| ~~D-verificar-cierre-ciego-a-la-ventana~~ (`verificar-cierre.py` no comprueba la frase de ventana del plan) **CERRADA S179** | Transversal, con la sesión de Higiene/Método | — | Nace en el M1 de S160. El verificador contrasta los censos de la bitácora con la crónica de archivado, pero NO la frase «El plan conserva ahora Sxxx (degradada a formato compacto) y Syyy como única cabecera H3 viva»: su comprobación cuenta cabeceras H3 y da 1 correctamente, sin contrastar la prosa que las describe. El cierre de S158 la dejó en S156/S157 y lo detectó A MANO el de S159; el propio paréntesis de la frase avisa de que ya pasó en S118 y en S137. Arreglo candidato, no medido: que el verificador DERIVE la afirmación del fichero en vez de mirarla. Hasta entonces, cada M1 la comprueba a mano. No se paga ahora. **CERRADA en S179** (`9c2898c`): la sección 5 del verificador deriva la frase de la cabecera viva y de la previa y es fallo duro si no cuadra, falta o se repite a principio de línea. Deja de comprobarse a mano. |
| ~~D-verificar-cierre-ciego-a-las-tablas~~ (una fila con más celdas que su cabecera esconde su última celda al renderizar) **CERRADA S179** | Transversal, con la sesión de Higiene/Método | — | Nace en S168: tres filas de «Mejora futura» tenían cuatro celdas y su nota no se veía, corregidas en `b912d22`; y una fila tachada de la tabla de deuda técnica real se partía por una barra sin escapar dentro de código, corregida en el cierre. `verificar-cierre.py` no cuenta celdas. Texto íntegro en el plan. **CERRADA en S179, AMPLIADA** (`9c2898c`): la sección 6 cuenta las celdas de cada fila contra su cabecera, con la barra sin escapar dentro de código como separador, y detecta filas de tabla fuera de toda tabla, en los cuatro ficheros; fallo duro. La quinta fila (L1284) se escapó en el mismo commit, y la medida destapó 40 filas de §4 de gestión fuera de tabla desde S141 por una línea en blanco de `5c58437`, quitada también. |
| ~~D-disparador-higiene-ambiguo~~ (el segundo disparador de Higiene/Método admite dos lecturas) **CERRADA S179** | Transversal, con la sesión de Higiene/Método | — | Nace en S178. El registro de S177 y el M0 y el M1 de S178 lo leyeron de forma distinta; texto íntegro en el plan. **CERRADA en S179** (`b62e050`): una sola lectura, en §6 de gestión y en la tabla de tipos de `metodo.md`: el mismo defecto de instrumento que cuesta trabajo por segunda vez en una misma sesión; dos defectos distintos no lo disparan. |
| D-curso-pestanas-desfasadas (una pestaña sigue mostrando el curso que tenía al cargarse mientras el backend ya tiene abierto otro) | Sin sede desde S173: no la toca `O-ci`, y no hay objetivo planificado después de H4 (era `O-aceptación`) | No | Nace en S163, en la prueba de Windows, pero no es de Windows: el backend tiene un solo curso abierto para todo el proceso (decisión E de `O-curso`), cada pestaña recuerda el suyo y el propio producto abre una pestaña nueva en cada segundo lanzamiento. El archivado no corre riesgo: la guarda actúa sobre la base realmente abierta. El caso malo es una pestaña que muestra el archivado con el activo abierto: sus descargas, «Generar» y ediciones irían al activo. Deducido del diseño, NO medido. Se mide y se decide al abrir la aceptación. **S168:** medida por lectura; con una pestaña no ocurre y con dos sí, deducido. Fuera del criterio de `O-aceptación` (D). Detalle en su ficha del plan. |
| ~~D-ps1-palanca-de-linux~~ (el texto de palanca de `empaquetar-windows.ps1` da la cifra de Linux) **CERRADA S174** | `O-ci` (Fase 12) | — | Nace en S163 (M2, Claude Code): el `.ps1` cita 60.869.367 B, que `docs/empaquetado.md` da como la cifra de Linux; la de Windows es 69.453.720 B. Sólo se ve en un NO CUMPLE. Cuelga de la Fase 12, que rehará la construcción. **Saldada en S174** (`4a445e6`, F1 de `C-bundle-por-tag`): `ps1:200` da 69.453.720 B. Se pagó dentro del Cambio porque, desde S174, un NO CUMPLE aborta y ese texto es el diagnóstico del job rojo de la CI. |
| D-contrato-dto-mide-jackson2 (el test de contrato del DTO de la proyección serializa con Jackson 2 y la aplicación sirve JSON con Jackson 3) | `O-ci` (Fase 12) | No | Nace en S170 (F3, lectura, Claude Code): `ProyeccionDtoContratoTest` usa `MappingJackson2HttpMessageConverter`, y la aplicación, con Spring Boot 4.1, el `JsonMapper` de Jackson 3; Jackson 2 sólo entra por el solver. No mordió en S170: el JSON real de `/proyeccion` se midió con el jar arrancado. Cuelga de la Fase 12, con la calidad de las suites. Ficha en el plan. **S176:** la CI lo muestra en cada ejecución como anotación de aviso (`ProyeccionDtoContratoTest.java:27`, `MappingJackson2HttpMessageConverter` marcado para retirada). Sin cambio de clasificación. |
| D-vista-horario-estado-rancio (la vista de horario muestra estado que una acción ya ha invalidado) | Sin sede (vista de horario; candidata a Saneamiento) | No | Nace en S171, en el ensayo del guion de aceptación, TÉCNICA REAL. Tres síntomas con causa medida: insignias y contornos que no cambian tras un ajuste aceptado (F5 lo corrige), el aviso «N pines sin aplicar» que cuenta pines ya aplicados (el pin sí se aplica) y el rechazo de un ajuste que sobrevive a regenerar. El guion los declara y el paso 4(a) usa F5, así que no bloquea `O-aceptación`. Texto íntegro en el plan. No se paga ahora. |
| D-jornada-congelada-por-disponibilidad (una sola restricción horaria congela la edición de la jornada de todo el centro) | Sin sede desde S173: no la toca `O-ci`, y no hay objetivo planificado después de H4 (era `O-aceptación`) | No | Nace en el M2 de S164 (Claude Code, lectura): `JornadaService.comprobarSinDependientes` (`:267-275`) rechaza con 409 el PUT de `/api/jornada` si existe una sola restricción, sesión o pin, porque `ordenEnDia` es posicional y reordenar la malla movería de fila las restricciones. Hoy ya lo provoca cualquier horario generado, y el orden natural del centro es definir la jornada primero. Muerde al pasar de curso si cambia la jornada: el duplicado copia las restricciones, y habría que vaciar la disponibilidad de todo el profesorado para editarla. Se mide en la aceptación, que recorre ese caso. No bloquea `O-disponibilidad` (R-deuda). **S168:** medida por HTTP; un PUT idéntico al vigente ya da 409; el mensaje de borrar es `D-jornada-msg409`. Fuera del criterio de `O-aceptación` (D). Detalle en su ficha del plan. |
| ~~D-indisp-solo-tramo-de-inicio~~ (el solver sólo aplica DURA y BLANDA al tramo de inicio de cada sesión) **CERRADA S166** | `O-disponibilidad` (`C-dura-completa`) | — | **Bloqueo:** Sí: bloquea la condición 5 de `O-disponibilidad` y se salda dentro de `C-dura-completa`. Mientras viva, el cortafuegos `RESTRICCION_HORARIA_CON_BLOQUE` impide la combinación. En el cierre de S165 se reescribió la condición para darla por cumplida y se revocó en el mismo cierre (decisión F). Nace en S165 (M2, T1/T4, medido). S165 no toca el solver y lo sustituye por el cortafuegos `RESTRICCION_HORARIA_CON_BLOQUE` de la prevalidación, que SÓLO se retira junto con el arreglo del solver para duración > 1, nunca por separado. Puerta del arreglo: huella canónica en los 44 fixtures e invariante de indexación de tramos (S165, fase 0, 0.3 y 0.2). El arreglo NO reutiliza `VerificadorSolucion.tramosOcupados`: el verificador sigue siendo oráculo independiente. Texto íntegro en «Deuda consciente VIVA». **SALDADA en S166** por `C-dura-completa` (`1638ee0`, `3738a23`, `206b754`): el modelo veta y penaliza todos los tramos ocupados, con tests que fallaban antes; la invariante de tramos la valida `ProblemaHorario`; el cortafuegos se retiró tras el arreglo, y la punta a punta con mutante lo confirma. **CERRADA** |
| D-ventanas-consecutivas-ciegas-a-bloques (ventanas y consecutivas no ven los tramos interiores de un bloque, en solver y verificador) | Sin objetivo asignado | No | Nace en S165 por lectura. No depende de las restricciones horarias: afecta ya a todo catálogo con actividades de más de un tramo. Requiere que el usuario cierre la semántica antes de implementar; la primera sesión será de análisis. Texto íntegro en «Deuda consciente VIVA». |
| D-modelo-no-determinista-entre-jvm (el `CpModelProto` de un mismo problema cambia de orden entre procesos) | Con `D-generacion-no-reproducible` | No | Nace en S165, fase 0, 0.3, medido en tres JVM: hash crudo distinto en 13 de 44 fixtures con los mismos recuentos; la huella canónica es estable. Causa probable `Map.copyOf`/`Set.copyOf`, no probada. Texto íntegro en «Deuda consciente VIVA». |
| ~~D-post-generacion-sin-sesiones~~ (la respuesta del POST de generación trae `sesiones: []` aunque el horario tiene sesiones) **CERRADA S175** | Sin objetivo asignado | — | Nace en S166 (fase 5, HTTP real, centro mínimo): `POST /api/horarios` responde 200 OPTIMAL con `"sesiones":[]` y `GET /api/horarios/{id}/proyeccion` justo después lista la sesión. La vista no lo nota: descarta la proyección del POST y recarga por GET (S93). Causa no medida. Texto íntegro en «Deuda consciente CERRADA (histórico)». **Cerrada en S175 por duplicado** de `D-post-horario-sin-sesiones` (S114), viva, cuya causa midió S142. La afirmación «causa no medida» era falsa. |
| D-props-main-invisibles-en-tests (ningún test lee las claves de `application.properties` de main) | `O-ci` (Fase 12) | No | Nace en la fase 3 de S167, medida: el `application.properties` de test tiene el mismo nombre y tapa al de main, así que la configuración de producción (`server.address`, `max-segundos`, la clave de mensajes de error) no la ha vigilado nunca ningún test. `MensajeDeErrorHttpTest` carga a mano las claves de main, y un mutante que anula esa carga lo demuestra. Misma familia que el hallazgo de S109 sobre `status().reason()`. |
| ~~D-tipo-apertura-sin-fila~~ (la sesión que abre un objetivo no tiene fila en la tabla de tipos de `metodo.md`) **CERRADA S194** | Transversal, con la sesión de Higiene/Método | — | Nace en S180, en el M0 de `O-demo-bundle`. La tabla de tipos tiene Desarrollo, Saneamiento, Configuración/UI, Higiene/Método y la sesión de acabado visual, y ninguno describe la sesión que abre un objetivo: M0, M2 de solo lectura, decisiones, alta documental con corrida en seco y M1, sin código de la aplicación. La práctica se cita por precedente —los Cambios de alcance de S147 a S173 y `C-alcance-demo-bundle`; la bitácora la nombra «APERTURA DE OBJETIVO» en S168 y S173—, así que el tipo se deduce de los precedentes y no de la tabla. Instancia de S181: «Desarrollo reducido» tampoco tiene fila; se cita por precedente desde S156 (S163, S169, S171, S172, S177 y S181). Arreglo candidato, no medido: una fila más en la tabla por cada tipo, con su ritual. No se paga ahora (R-deuda). **S182:** otra instancia «Desarrollo reducido» (ensayo en la VM, sin código). **S183:** apertura de `O-pre-demo` (`C-alcance-pre-demo`), por el precedente S168, S173 y S180. **S186:** otra instancia «Desarrollo reducido» (tag, Release y verificación en la VM, sin código; precedente S177). **S187:** otra instancia «Desarrollo reducido» (actualización a `v0.2.0` y ensayo en la VM, sin código; precedente S182). **S188:** otra instancia «Desarrollo reducido» (demo con la secretaria en la VM, sin código; precedentes S182 y S187). **S189:** otra instancia «Desarrollo reducido» (inventario y cierre de `O-demo-bundle`, sin código y sin VM; precedentes S182, S187 y S188). **S190:** otra instancia, de apertura de objetivo (`O-base-tecnica`; precedentes S168, S173, S180 y S183), convertida en replanificación de H5 por decisión del usuario (precedente S178), sin código y sin VM. **S193:** otra instancia, de Configuración/UI para la línea de versión y, en la misma sesión, una fase sin código (tag, Release y VM; precedente S186). **CERRADA en S194** (`ed4ed5c`): filas «Apertura de objetivo» y «Desarrollo reducido» de la tabla de tipos de `metodo.md` y párrafo «Tipos combinados». |
| ~~D-generacion-sin-exclusion~~ (mientras se genera, nada impide editar la configuración ni lanzar otra generación, y el guardado no es atómico) **CERRADA S192** | `O-base-tecnica` (solo la atomicidad) | — | Nace en S182, por lectura de código en el F4 del ensayo, sin medir. Hermana de `D-post-horario-sin-sesiones` (mismo `guardar()` y OSIV activo). Texto íntegro en el plan. S183: `O-pre-demo` abierto. S184: la concurrencia está cubierta (`2c6465d`: segunda generación y escrituras, 409 `GENERACION_EN_CURSO`); sigue VIVA por la atomicidad, en el objetivo (3). **S190:** sede de la replanificación de H5: `O-base-tecnica` (solo la atomicidad). Criterio, condición 4. **CERRADA en S192:** la atomicidad, con `C-version-y-rastro` (`eddad66`, decisión I de `O-base-tecnica`). **CERRADA** |
| D-entidad-sin-actividades (no hay camino de un profesor o una asignatura a sus actividades, y el 409 de borrado no dice qué plazas lo impiden) | `O-interfaz` | No | Nace en S182, ensayo (T5): sin la ayuda del ensayo no había forma de saber qué cuatro actividades reasignar. Variante más barata: que el 409 liste las plazas. Texto íntegro en el plan. S187: reaparece en el ensayo sobre `v0.2.0` (T5); sin ayuda, el rodeo fue volver al curso archivado y mirar en su horario las clases de BYG1 (pista 4). S188: en la demo (T5) usó el PDF de grupos para saber qué daba BYG1, y pidió navegar desde profesor, asignatura, grupo y subgrupo a sus actividades. **S190:** sede de la replanificación de H5: `O-interfaz`. |
| D-filtro-por-codigo (el filtro de Actividades busca en el texto, no por pertenencia, y omite sin aviso) | `O-interfaz` | No | Nace en S182, ensayo (T6), confirmado por código: «4ºC» da 11 de 219 y omite un bloque con plazas de 4ºC. Texto íntegro en el plan. S185: los 9 selectores reutilizan `coincide()` sobre el texto visible de la entidad. El ordinal escrito ya discrimina en listas y selectores («1ºB» no encuentra «1B-A…», de Bachillerato; `7c9f6b0`); sigue el casado por partes: «4º C» casa «4º» y «C» por separado. S187: en `v0.2.0`, «4ºC» sigue dando 11 de 219 sin el bloque (ensayo, T6). S188: en la demo (T6), «Latín» no encuentra «LAT»: el texto de fila lleva el código de la asignatura. **S190:** sede de la replanificación de H5: `O-interfaz`. |
| ~~D-generacion-sin-rastro~~ (una generación no deja rastro: ni log, ni fila si falla; estado y objetivo no se ven) **CERRADA S192** | `O-base-tecnica` | — | Nace en S182 (F2 y F4): los dos 503 del ensayo sólo constan en las notas del usuario. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-base-tecnica`. Criterio, condición 3. **CERRADA en S192:** condición 3 de `O-base-tecnica` (`e83355e`, decisión K). El estado y el objetivo siguen sin verse en la interfaz, y un intento fallido sigue sin dejar fila: no lo pide el criterio. **CERRADA** |
| D-sin-acceso-directo (tras extraer el zip no hay acceso directo al programa) | `O-prueba-secretaria` | No | Nace en S182, ensayo (T1): «¿dónde está instalado Educhronos?». Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-prueba-secretaria`. El procedimiento de instalación crea el acceso; sigue viva en el producto. **S193:** el procedimiento de transporte del acta describe los pasos pero no escribe las órdenes, que viven en el material de S186 y S187 (`s186/f3/a/03-usb.txt`, `s187/f1/`); el procedimiento escrito de instalación de `O-prueba-secretaria` tiene que escribirlas. |
| D-generar-en-solo-lectura (en un curso archivado, «Generar horario» sigue visible y habilitado) | Sin sede | No | Nace en S182, ensayo (T9); pulsarlo debería dar 403 `CURSO_SOLO_LECTURA`, no se probó. Texto íntegro en el plan. S183: la condición 4 de `O-pre-demo` hace legible su mensaje; el botón sigue fuera. S184: el 403 ya se lee (`mensajeGeneracion()` muestra el `message`); el botón habilitado sigue fuera. |
| ~~D-huella-captura-del-chat~~ (el arquitecto escribe en los guiones huellas de fichero de capturas que no son las de lo conservado) **CERRADA S194** | Transversal, con la sesión de Higiene/Método | — | Nace en S182: mismo defecto que el (5) de S181, repetido entre sesiones pese al aviso del prompt y a la norma 3 de M-guion. Remedio propuesto: sólo huellas de píxeles en los guiones; las de fichero, siempre de Claude Code. Texto íntegro en el plan. **CERRADA en S194** (`ed4ed5c`): norma 14 de M-guion en `metodo.md`. |
| D-humo-localhost (el humo de Windows llama a localhost y el servidor sólo escucha en 127.0.0.1) | Sin sede | No | Nace en S183, por lectura (F1 de `C-alcance-pre-demo`). La CI corre el humo con `-SinHumo`; si `localhost` falla en Windows, SIN MEDIR. |
| D-suspension-durante-generacion (sin medir: un Windows con suspensión podría dormirse a mitad de una generación de 20 a 60 minutos) | Sin sede | No | Nace en S186 (F3 de `C-version-pre-demo`): la VM tiene el plan «Equilibrado» con «Suspender tras» 900 s en corriente alterna, pero su firmware no admite ningún estado de suspensión, así que allí no actúa. En un PC real, sin medir. No afecta a la demo, que corre en la VM. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: sin sede. La prueba es en la VM, cuyo firmware no suspende (S186). |
| D-borrado-sin-control-de-horas (borrar una actividad con profesor se permite y nada avisa de que un grupo queda por debajo de sus horas) | `O-datos-centro` | No hoy; candidata a bloquearlo, lo decide su criterio | Nace en S188, demo con la secretaria (T5); sale de `O-demo-bundle` (R-incidencia): las cuatro actividades de ByG de 1º se borraron en vez de reasignarse y el horario salió FEASIBLE con 807 sesiones frente a 819 sin que nadie lo advirtiera. Por lectura de `v0.2.0` (M2 de S188): la confirmación es genérica, las plazas caen en cascada y la prevalidación sólo salta por exceso. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-datos-centro`. Caso particular de los totales; el aviso tiene que verse antes de generar, no solo existir (`D-aviso-fuera-del-dialogo`). **S203:** con total declarado, borrar una actividad de clase ya da AVISO de cuadre en el diálogo de generar. Sigue VIVA hasta la condición 5, que lo verifica con los totales del volcado: sin totales declarados, nada avisa. |
| ~~D-nombres-reales-en-repo~~ (el repositorio de GitHub, público, contiene nombres reales del profesorado) **CERRADA S195** | `O-carga-2026` | — | **Bloqueo:** No (no bloquea `O-base-tecnica`); bloqueaba versionar los PDF o los volcados de 2026/2027. Nace en S190: lo informa el usuario. Medido al cerrar S190: la API de GitHub responde 200 sin autenticar (accesible sin autenticar: público al medir); 8 ficheros versionados con alguno de cinco apellidos reales (`s190/cierre/nombres-en-repo.txt`); versionados tres PDF de `docs/horario-referencia/pdf/`, ninguna base ni hoja. Pasar el repo a privado no retira lo ya publicado. Lo decide `O-carga-2026`. Texto íntegro en el plan. **S191:** sigue público (medido el 2026-10-04 sin credenciales: web 200 y `git ls-remote` con `HEAD` en `2d11b96`); «público hasta S190» era falso. **CERRADA en S195 por decisión del usuario** (decisión B de `O-carga-2026`): pasa a «Decisión arquitectónica consciente». |
| ~~D-guion-lista-ordenes-incompleta~~ (la lista de órdenes permitidas de un guion no cubre todo lo que el guion manda hacer) **CERRADA S194** | Transversal, con la sesión de Higiene/Método | — | Nace en S191. Mismo defecto en S190 —(1), (4) y (5)— y en S191: `git status` en F1-bis y F1-ter, `mkdir` dentro del repo y `seq` en F2. En S190 costó dos paradas y disparó Higiene/Método, aplazada en S191 porque `O-base-tecnica` está abierto; en S191 no costó ninguna. Se escapan las órdenes que un paso implica sin nombrarlas y las de un procedimiento al que se remite. Arreglo candidato, no medido: una norma de M-guion o un instrumento que derive la lista de los pasos. Primera candidata de Higiene/Método al cerrar `O-base-tecnica`. Texto íntegro en el plan. **Instancias de S192**, sin coste: `seq` en un bucle de espera, `mvn -v`, `mv` y `> /dev/null`. **Instancias de S193**, con coste: `nohup` en el procedimiento de `docs/guion-aceptacion.md` al que remitía F2.6 (una parada), y el paso 4 de F4b, que remitía a un procedimiento del acta que no escribe órdenes (una vuelta con el usuario); las dos, por remitir a un procedimiento sin leerlo. Sin coste: `lsblk`, `udisksctl`, `findmnt` y `df` en F4a, listadas antes de ejecutar. Con dos costes en la misma sesión, el disparador salta (lectura de S179); coincide con el cierre de `O-base-tecnica`, que ya la hacía primera candidata. **CERRADA en S194** (`ed4ed5c`): normas 10 y 11 de M-guion en `metodo.md`. |
| ~~D-guion-salida-sin-tope~~ (un guion de medición no limita lo que vuelve a la conversación) **CERRADA S194** | Transversal, con la sesión de Higiene/Método | — | Nace en S192. El M2 pedía copiar métodos y ficheros enteros y mostrar el informe entero en la conversación, y devolvió 142 kB (del orden de 35.000 a 40.000 tokens) cuando lo decisivo cabía en un 10 %. Desde la fase 1 de S192 los guiones fijan un tope de 25 a 40 líneas y dejan lo literal en el informe en disco, con su manifiesto; no hubo más casos. Arreglo candidato, no medido: una norma de M-guion que separe la evidencia en disco de la respuesta a la conversación. Texto íntegro en el plan. **CERRADA en S194** (`ed4ed5c`): norma 13 de M-guion en `metodo.md`. |
| ~~D-guion-parada-por-rutas~~ (la condición de parada de un guion se acota por rutas y no por lo que protege) **CERRADA S194** | Transversal, con la sesión de Higiene/Método | — | Nace en S193 (F2.6): una lista de rutas permitidas atrapó las escrituras de la JVM y del build en `/tmp` y en carpetas ignoradas; una parada sin riesgo detrás. Corregida en la sesión con una condición por intención. Arreglo candidato, no medido: una norma de M-guion. Texto íntegro en el plan. **CERRADA en S194** (`ed4ed5c`): norma 12 de M-guion en `metodo.md`. |
| ~~D-migracion-sin-rastro~~ (al abrir una base, el log no dice nada del esquema) **CERRADA S201** | `O-datos-centro` | — | Nace en S193 (F4, VM): `v0.3.0` adopta una base de versión 0 sin ninguna línea en el log; `PreparadorEsquema` no escribe log. Fuera del criterio de `O-base-tecnica` (R-terminado). Pesa en cuanto llegue a la secretaria la primera migración real. Arreglo candidato, no medido: una línea INFO por base preparada. Texto íntegro en el plan. **Saldada en S201** (`8537348`): una línea INFO por base preparada y una WARN por base posterior rechazada, en el arranque y en la apertura de curso. |
| D-codigo-profesor-no-es-identidad (el código de profesor no identifica a la persona entre cursos) | `O-comparacion` | No | Nace en S198 (M2 de `C-catalogo-2026`): 25 de los 56 códigos de profesor comunes a 2025/2026 y 2026/2027 corresponden a otra persona. Pesa al comparar el curso de la secretaria, que parte de un duplicado de 2025/2026, con el oficial de 2026/2027. No afecta al catálogo de 2026/2027, que toma los nombres de sus propios PDF. Arreglo candidato, no medido: casar por nombre o por una correspondencia escrita, fijada antes de mirar. Texto íntegro en el plan. |
| D-incoherencia-como-404 (una incoherencia de datos llega al cliente como «no existe») | `O-interfaz` (S201) | No | Nace en S201 (T3, mutante N9): `HorarioController` convierte toda `IllegalArgumentException` en 404 en el diagnóstico, la proyección y el CSV, así que una corrupción de datos se presenta como un horario inexistente. Anterior a S201. Amplía la lista de `O-interfaz` (decisión del usuario). Texto íntegro en el plan. |
| D-unica-declarada-sin-esquema (dos restricciones únicas que el código afirma y el esquema no tiene) | `O-datos-centro` (S201) | No | Nace en S201 (M2 y contraste de F3): `Sesion` declara una única (horario_id, plaza_id, indice) y `BloqueoService` habla de otra en `sesion_bloqueada`; ninguna está en `schema.sql`, y con `ddl-auto=none` la anotación no hace nada. La unicidad la sostiene el código. Texto íntegro en el plan. |

#### Mejora futura, cuelga y espera
| Deuda(s) | Objetivo | Nota |
|---|---|---|
| D-arranque-sin-aviso-de-espera (entre el doble clic y el navegador no hay ninguna señal de que la aplicación esté arrancando) | Sin objetivo asignado | Nace en el M4 de S156, propuesta del usuario: en el Windows 11 limpio el navegador tarda en abrirse —el log mide unos 16 s desde que arranca el proceso— y en ese tiempo nada indica que la aplicación se esté iniciando, así que parece un fallo e invita a pulsar otra vez. No es defecto de la condición 4 de `O-instalación`, que pide que el navegador se abra, y se abre; y la instancia única ya evita los procesos duplicados. Candidatas, sin decidir: (1) la pantalla de presentación nativa de la JVM como opción del lanzador, la más barata y NO verificada con el lanzador de `jpackage`; (2) adelantar el icono de la bandeja antes de Spring, con «Iniciando…»; (3) una ventana propia que se cierre al abrir el navegador. Es la misma razón que dio origen a la condición 4 en S151 S188: en la demo (T1) relanzó el programa creyendo que no arrancaba: unos 13 s sin señal, y un segundo lanzamiento abre otra pestaña sin escribir en el log. |
| ~~D-generacion-sin-movimiento~~ (mientras se genera, la pantalla sólo muestra un texto fijo) **CERRADA S184** | `O-pre-demo` (S182, decisión L) | Nace en el M4 de S161, propuesta del usuario: la generación dura minutos en el centro real y la vista sólo muestra el texto del estado «generando» (S118, `D-generacion-sin-indicador`), sin nada en movimiento que diga que se sigue trabajando. Dos variantes: (1) una animación indeterminada, barata y honesta; (2) una barra de progreso, que NO puede medir avance real —el solver no sabe qué fracción del trabajo lleva: optimiza hasta agotar el presupuesto o probar el óptimo— y sólo podría mostrar el tiempo transcurrido frente al tope, lo que exige publicar el presupuesto por API (`D-presupuesto-anunciado-espejo`). Medido de paso en S161: el log no registra la duración del solve. No cambia el criterio de ningún objetivo (R-terminado) **S182:** vista en el ensayo (T8): «no se sabe si la aplicación se ha quedado colgada o está trabajando». Va a `O-pre-demo` con una barra de tiempo transcurrido frente al presupuesto que bloquee la interacción, junto a `D-generacion-sin-exclusion`. S183: `O-pre-demo` abierto. **CERRADA en S184** (`39efbc9`, `2692f62`, condición 3 de `O-pre-demo`): barra de tiempo transcurrido sobre el elegido en `GenerandoDialogo`, topada en el total y con «Terminando…» al pasarlo; es la variante (2) acotada. |
| D-seleccion-de-vista-fuera-de-la-url (la vista del horario no guarda en la URL qué grupo, profesor o aula se está mirando) | Transversal, sin objetivo asignado | Nace en el M4 de S155: con F5, que desde S155 funciona, la vista vuelve al primer grupo (1B-A) aunque se estuviera mirando otro. No es defecto de la condición 9, que pide que la vista se sirva, y se sirve. La selección vive en dos señales del componente, `vista` y `entidad` (`horario-view.ts:58-59`), que no se leen ni se escriben en la URL —la ruta sólo lleva el id del horario (`paramMap`, `horario-view.ts:218`)—, y al cargar la proyección `entidad` se reinicia a la primera entidad de la lista (`horario-view.ts:307`). Consecuencia: no se puede marcar ni compartir un enlace a un grupo concreto. Arreglo previsible: llevar la selección a un parámetro de la URL |
| D-tutor-invisible-en-grupos (la tabla de grupos no puede enseñar el tutor porque el dato no llega al cliente) | O-demo | Nace en el recorrido de S133, de una propuesta del arquitecto, y se mide antes de clasificarla: `grupo-lista.html` pinta cuatro `<th>` (Código, Nivel, Tipo y acciones) y `Grupo` (TS) es espejo exacto de `GrupoDTO` (Java) con `id`, `codigo`, `nivel` y `tipo`. El tutor NO viaja, así que no es una columna: obliga a ampliar el contrato o a pedir la tutoría fila por fila. Deja de ser acabado y pasa a ser superficie nueva, fuera de O-diseño por el mismo corte aspecto/comportamiento de su ficha desde S121. Sede O-demo, donde el centro real se enseña y ver quién tutoriza cada grupo tiene valor para un jefe de estudios |
| D-plazas-ocultas-solo-al-arrastrar (las plazas que no caben en una celda sólo se ven todas al empezar un arrastre) | Transversal, sin objetivo asignado; SIN SEDE desde S141, que cerró O-particiones | Nace en el recorrido de S133, de una propuesta del arquitecto y con su argumento. NO es un defecto: el mecanismo actual es la marca `+N` con el detalle en el `title`, decidido en S127 y con etiqueta accesible desde S129, así que la propuesta cambia una decisión escrita. Y cambia INTERACCIÓN sobre la única superficie donde el alto es presupuesto medido: cualquier mecanismo que ocupe alto realimenta la medición de la que depende que la rejilla no tenga scroll, que es el argumento con que D11 hizo obligatoria la expansión. Mismo encuadre y misma razón que `D-avisos-como-bloque-fijo` |
| D-nombre-horario-instante (el horario se llama `"Horario " + Instant.now()`, 38 caracteres con nanosegundos) | O-demo | Nace en S126 al fundir el título con los controles (D9): el nombre que el backend pone por defecto (`GeneradorHorarioService.java:187-188`, porque el POST del frontend va con cuerpo vacío) no cabe en una fila junto a dos selectores y un botón, y además no es un nombre sino una marca de tiempo. CONTENIDA en la pintura: `horario/titulo.ts` compone el rótulo desde `fechaGeneracion` y cae al nombre crudo si la fecha no parsea. El arreglo REAL es mandar un nombre legible en el POST, que `HorarioController.java:64` ya acepta, y no se hace porque es una escritura nueva y el criterio 5 de O-navegación las excluye. Se paga en O-demo, que es donde se bautizan horarios de verdad |
| D-jornada-zona-servidor (las horas de la jornada dependen de la zona del proceso del servidor) | O-demo | MEDIDA en S126 contra el backend real: `tramo_semanal` guarda 25200000 ms (7:00) y `GET /api/jornada` devuelve `08:00`. El resultado es correcto en este equipo y su corrección viene de cómo el driver lee el entero, luego depende del despliegue: en UTC la jornada entera se mostraría una hora antes. Es la razón por la que D8 sigue sin pintar la hora en la fila de recreo, y NO es la razón que `diseno-navegacion.md` §4-D8 escribió: allí el hueco era «no sabemos si la conversión es correcta»; aquí es «lo es, pero por el despliegue». No la introduce S126 y no la puede arreglar el frontend, que pinta la cadena que le dan. Se decide donde se despliega para el centro |
| ~~D-vacio-miente-con-error~~ (cuando fallaba la CARGA de una lista convivían el mensaje de error y «No hay X todavía», que era falso) **CERRADA S128** | C-revisión → pagada antes, en C-identidad | Nace en S124 al medir las dos caras del encadenado de ramas. S124 probó encadenar `error()` con el resto y lo REVIRTIÓ: encadenado, un 409 de borrado hacía desaparecer la tabla entera (medido: 8 filas → 0), y eso es un camino de uso normal que S113 introdujo a propósito, mientras que el fallo de carga es raro. El defecto es PREEXISTENTE, no lo introdujo S124. Arreglo ya escrito: condicionar la rama del vacío a `x().length === 0 && !error()`. No se paga (R-terminado: no cambia el criterio 3); su sede era C-revisión, que va a repasar esta UI y donde un mensaje que miente es exactamente lo que toca mirar. **CERRADA POR CONSTRUCCIÓN en S128**, antes de su sede y sin abrir sesión: al fundir las cuatro ramas de estado de las siete listas en `app-estado-lista`, la precedencia pasa a existir en UN sitio, igual que `D-pdc-lista-rancia` murió en S123 sin que nadie escribiera un `EventEmitter`. Sólo el VACÍO se condiciona al error —encadenar el error con la tabla es lo que S124 revirtió— y la precedencia vive en un `computed` con un `'ninguno'` explícito y no en un `@else if`, porque escrito como rama el caso caía al hermano `coincidencias === 0` y el vacío dejaba de mentir para que mintiera «Ningún resultado para «»». Verificada en navegador. **CERRADA** |
| D-F8.6 de cobertura (iiiB1-a, ivB-a-bis, ivD-a, ivA-a, ivA-c, ivB-b, ivB-c, iiiA-b, B-a) | O-ajuste-cierre (cerrado S146) | Cobertura de la vista de horario. La mayoría se RECLASIFICA a limitación conocida en cuanto O-shell reubique la vista (su contexto de test cambiará). NO se pagan ahora |
| D-F8.4-A-a, -A-b, -B1-a (~~-A-c~~ **CERRADA S146**) | O-ajuste-cierre (cerrado S146) | Cobertura de prevalidación. **`-A-c` CERRADA en S146** por su propia condición escrita —«hasta que exista un productor de `AVISO`»—: el productor es S8, cuarta regla de `PrevalidacionService`, y la severidad discrimina, medido por el mutante A2 de S146 (AVISO -> ERROR en el mapeo de S8, cuatro víctimas). Las otras tres siguen vivas |
| D-S101-num (numeración global de tests colisionada) | O-ajuste-cierre (cerrado S146) | Detectada S101: la secuencia (N) de la capa componentes/servicios tiene colisiones preexistentes —(27),(28-30),(35-37) con contenidos distintos en dos ficheros— que rompen la atribución por (N) en campañas de mutación. Es superficie de specs de H1 (cerrado). Los specs de O-catálogo la esquivan abriendo secuencia propia por fichero. Arreglarla no bloquea nada (R-terminado): no se paga ahora |
| D-F8.5-D2b2-a, -D2b2-b (diseño/cosmética) | — | Sin objetivo urgente |
| D-F8.5-C3-a, -C3-b, -C2a-a | O-catálogo | Semántica/dominio de catálogo, a resolver con datos. C3-a CONTENIDA en UI desde S102 (COMUN fuera del selector del form de Aula); sigue viva a nivel de esquema. C3-b: los códigos por currículo (Mat/LCL usados en specs de S103 son reales de este catálogo) siguen sin UI para poblar compatibilidades (ver D-S103-compat) |
| D-S103-compat (CRUD de asignatura no alcanza `aulas-compatibles`) | Cambio de compatibilidad (tras Grupo, o dentro de O-estructura) | Detectada S103: el backend expone `GET/PUT /{id}/aulas-compatibles` pero el CRUD plano no lo alcanza. NO bloquea O-catálogo (semántica S75: 0 filas ⇒ irrestricta; un centro mínimo corre sin poblar compatibilidades). Incluye decidir la no-atomicidad POST→PUT. Estirar el molde con el sub-recurso es Cambio propio. No se paga ahora **S189:** fila 11 del inventario: la secretaria entrega qué asignaturas exigen un tipo de aula, y por pantalla no se puede meter (`asignatura-form.ts:27-29`). Es el «no se puede» de esa fila. |
| D-jornada-msg409 (mensaje del 409 dice «No se puede borrar» al guardar jornada) | O-estructura | Detectada S107: `ReferenciaEntranteException` se escribió para los DELETE de catálogo; su mensaje se reutiliza en el PUT de jornada y el usuario lee «No se puede borrar: referenciada por…» cuando intenta GUARDAR. Cosmético, NO bloquea (el desglose «N sesiones, M restricciones… antes de reconfigurar» sí es correcto). Se corrige con un mensaje propio del caso PUT cuando O-estructura vuelva a tocar el backend de jornada; no se paga ahora (R-terminado, M3 cerrado). ACTUALIZADA S109: baja de coste sin cerrarse — la fase 1 de S109 parametrizó el verbo de `ReferenciaEntranteException` (ctor de un argumento delega en «borrar», ctor de dos toma el verbo), así que corregir jornada pasa a ser cambiar de ctor en `JornadaService`, que sigue usando el de un argumento |
| D-jornada-asimetria (contrato GET(35)≠PUT(7 día tipo)) | O-estructura | Detectada S107, consecuencia consciente de «el backend expande»: el GET devuelve la malla completa, el PUT acepta un día tipo. Nota de diseño de API, no deuda bloqueante: la UI convive sin fricción real (pinta un día, manda un día). Reconsiderar `diaTipo` en el GET solo si un futuro cliente lo pide |
| D-jornada-flush-test (`put_dosVecesLaMismaMalla_idempotente` no discrimina el flush) | O-estructura | Detectada S107: falta `UNIQUE(dia,orden)` en `schema.sql`, así que sin el `flush()` el resultado sería el mismo y el test no lo prueba. El `flush()` es defensivo/preventivo (correcto: fuerza DELETE antes de INSERT). Si algún día se añade la constraint, el test pasa a discriminar. Deuda de test, no de código |
| D-actividad-ux (asperezas del editor de Actividad) | `O-interfaz` | Detectada S109 al conducir el formulario con Playwright, tres asperezas de presentación: los dos `<select formControlName="asignatura"` del formulario (la de la actividad y la de la plaza) no tienen `id` ni `label for` y se anuncian igual a un lector de pantalla; el error de servidor viejo sigue pintado mientras se muestra un error de campo nuevo (`error()` solo se limpia al empezar una petición); y el aviso de multiplaza vive dentro de la celda del recuento, mezclando dato y aviso. Ninguna impide configurar nada. RECORTADA en S110: el tercer síntoma se CERRÓ de paso al retirar el trozo B la guarda de multiplaza y con ella el aviso; sobreviven los dos primeros. No se paga ahora S188: en la demo le resultó engorroso el manejo de actividades, y los nombres de las actividades, confusos. **S190:** sede de la replanificación de H5: `O-interfaz`. Lo engorroso de S188. |
| D-subgrupo-ux-multiselect (el campo «grupos» del form de subgrupo es un `<select multiple>` nativo) | O-estructura (o O-diseño si absorbe el acabado) | Detectada S108, DECISIÓN CONSCIENTE de alcance: se eligió la mínima desviación del molde (`<select multiple>` nativo) y la UX rica —chips, búsqueda, casillas— se aplaza a una fase de mejora de UX de subgrupos ya prevista al abrir el Cambio. NO es deuda técnica (el componente funciona, valida I6 en cliente, 12 tests) ni bloquea el criterio de O-estructura (la población se elige, solo sin comodidad). El `.subgrupo-form__multiple` y el handler `alSeleccionar` son el punto de sustitución. No se paga ahora |
| D-pdc-sin-edicion (el sub-recurso PDC no tiene PUT ni PATCH) | O-estructura | Medido en S113: un PDC no se renombra, se borra y se recrea, y si su subgrupo está retenido por una plaza el borrado da 409. El diálogo REFLEJA el contrato y no ofrece «Editar»: exponer un botón sin backend detrás sería que la UI mintiera (mismo criterio que D-F8.5-E-a con `peso`). No bloquea: el §6.2 se reproduce sin renombrar nada y el código lo escribe el usuario en el alta (D1-3, S76). Si se paga, va junto con D-pdc-vinculo-por-cadena: un rename recalcularía mal el código derivado del subgrupo |
| D-pdc-sufijo-completo (`-Completo` significa lo contrario en el backend y en el modelo) | O-estructura | Medido en S113. El backend deriva el subgrupo del PDC como `codigo + "-Completo"` con población SOLO el PDC (regla S23); en el cuerpo de §6.2 del modelo «3ºA-Completo» es el subgrupo que enlaza el ordinario Y su Di. Dos convenciones incompatibles en un espacio de códigos único. No urgente (el cuerpo de §6.2 está marcado como SUPERADO y la Nota S23 no usa el sufijo), pero es estado vivo confuso (R5). Probablemente se salde con una línea en el modelo, no renombrando la derivación |
| D-monodi-botones-inertes (el subgrupo mono-Di ofrece Editar y Borrar que siempre fallan) | O-estructura (o O-diseño) | DECISIÓN CONSCIENTE de S113: no se ocultan. Hacerlo exigiría que `SubgrupoDTO` transportara el tipo de los grupos de su población —mover el contrato por comodidad de pintura— y el precio de no hacerlo es acotado, porque con G2 los botones fallan en vez de destruir, que era el problema real. Si se paga, con la información en el DTO y no adivinando por el sufijo del código, que es el acoplamiento que lamenta D-pdc-vinculo-por-cadena |
| D-s8-muda (el resalte de una violación S8 no dice qué falta) | O-ajuste-cierre (cerrado S146) | Nace en S114, medida en navegador. La `descripcion` del `ViolacionDTO` llega al cliente con el texto exacto («Actividad MAT-1ESOA requiere tutor, pero ningún profesor suyo es TUTOR_PRINCIPAL…») y NO se pinta en ningún sitio: la rejilla solo dibuja un filete rojo de 2px (`horario-grid.css:122`), sin texto, tooltip ni lista. Medido: con la violación activa, el texto de la página no nombra `TUTORIA_SIN_TUTOR` ni la palabra «tutor». Es PEOR que un mensaje genérico por una razón propia de S8: como es la única regla cuyo origen es el CATÁLOGO y no la colocación, el resalte cae sobre celdas perfectamente colocadas y arrastrarlas NO lo quita nunca, así que el usuario intentará moverlas indefinidamente. Es superficie de la vista de horario (familia 8.6/H1), no de O-estructura: mismo criterio con que S113 dejó fuera D1-8 y D1-10. Familia de D-F8.6-ii-a. No se paga ahora |
| D-diagnostico-no-es-foto (el diagnóstico de un horario recalcula contra el catálogo vivo) | O-ajuste-cierre (cerrado S146) | Nace en S114 al no poder medirse el paso 9 del M4 como estaba planteado. `DiagnosticoService` verifica contra el catálogo ACTUAL, y `verificarTutorias` no mira la solución (`VerificadorSolucion.java:56`), así que para S8 el diagnóstico responde «¿esto sería válido AHORA?» y no «¿lo era al generar?». Consecuencia medida: el horario #1, generado con la violación real, se presenta hoy impecable; y el #2, generado limpio, se pinta en rojo si alguien quita el tutor después. Afecta solo a S8 (las demás reglas sí leen la solución). No bloquea nada hoy y el registro histórico no existe como requisito en ningún criterio. Se decide al abrir la vista de diagnóstico, junto con D-s8-muda. No se paga ahora. **AFINADA A LA BAJA en S119, que la usó como INSTRUMENTO:** la capa 1 del contraste se midió invocando `DiagnosticoService` (vía `GET /api/horarios/{id}/diagnostico`) contra el horario de S118, y aquí la deuda resultó VACUA —la base m4 no se ha tocado desde el instante de generar, así que el catálogo vivo ES el del momento del solve—. Queda escrito para que nadie lea aquel «cero violaciones» como si la deuda no existiera: sobre una base que sí hubiera cambiado entre generar y diagnosticar, el mismo verde respondería a otra pregunta. Consecuencia práctica y barata para cualquier medición futura: si se diagnostica un horario histórico, hay que decir sobre qué estado del catálogo se hizo. Sigue sin pagarse |
| D-tutor-pdc-desincronizado (la herencia del tutor al PDC corre solo en el alta) | O-estructura (cerrado) | Nace en S114. `PdcService.heredarTutorPrincipal` se invoca únicamente desde el alta (`PdcService.java:110`): si después se cambia el tutor del padre con el PUT, el PDC conserva el antiguo en silencio. No es un bug del código actual —nadie prometió resincronización— pero C-tutores lo hace VISIBLE por primera vez: se verán dos grupos emparentados con tutores distintos y nada explicará por qué. No bloqueaba el criterio (el §6 no exige reasignar tutores) y por eso el objetivo cierra con ella viva. Arreglarla es decisión de dominio, no una guarda —¿copia o referencia?—, familia de D-pdc-vinculo-por-cadena. **MUERDE en O-demo, confirmado en S115:** el centro real tiene cinco PDC y el orden natural de carga (grupos → PDC → tutores) los dejaría a todos sin tutor, porque `heredarTutorPrincipal` corre solo en el alta y nada resincroniza después. CONTENIDA sin pagarla, invirtiendo el orden de carga —los tutores de los padres se asignan antes de crear sus PDC— o asignando el tutor a cada PDC por el sub-recurso, que acepta cualquier grupo. **S116 la CONTIENE por otra vía y mide que aquí era vacía:** el cargador asigna las 28 tutorías DESPUÉS de crear los PDC, porque el PUT del sub-recurso es reemplazo total idempotente y acepta grupos PDC; y los 5 PDC del catálogo tienen el MISMO tutor que su padre, luego no había nada que sobreescribir. Verificado además que la herencia solo ocurre si el padre ya tiene tutor en el instante del alta. La deuda sigue viva: el arreglo real es decidir copia o referencia. No se paga ahora |
| D-dialogo-foco-perdido (al salir del estado «cargando» el foco cae fuera del diálogo) | O-diseño | Nace en S114, medida en los tres diálogos. El CDK enfoca el botón de la rama `cargando`; cuando el `@switch` cambia de rama ese elemento se destruye y el foco cae a `<body>`, fuera del diálogo. `GrupoForm` (sin estados) conserva el foco dentro; `PdcDialogo` y `TutoriaDialogo` no. NO la introduce C-tutores: `PdcDialogo` hace lo mismo desde S113. Para teclado y lector de pantalla el diálogo queda abierto sin foco dentro. Arrastra una consecuencia de andamio: la barrera `:focus` con que `centro-minimo.spec.ts` evita la carrera del portal no sirve en diálogos con estados, así que si algún e2e futuro abre uno de estos dos habrá que sustituirla por una espera al contenido. Es acabado de interacción, transversal a las vistas: cuelga de O-diseño. No se paga ahora |
| D-bundle-presupuesto (el bundle inicial excede el techo declarado) | O-diseño | Preexistente desde antes de S112 (507,66 kB frente a 500 kB en `angular.json`, verificado sobre HEAD limpio); S113 lo lleva a 514,42 kB al entrar `PdcDialogo` en el grafo de dependencias. NO se toca `angular.json`: subir el techo es configuración de build, no está en el criterio de ningún objetivo vivo, y hacerlo «de paso» convierte un aviso útil en un número que nadie vuelve a mirar. Cuelga de O-diseño, que tendrá delante el bundle completo y las vistas congeladas y podrá elegir entre subir el techo, rutas perezosas o recortar. Hasta entonces, anotar el delta en cada sesión que compile. **SALDADA EN S121 dentro de C-tokens, de paso y no por sesión propia.** No bloqueaba (el `maximumError` está en 1 MB y ningún trabajo de color puede romper el build), pero O-diseño era su sede designada y caía en el camino. Se midió que el aviso YA estaba encendido antes de tocar nada —521,03 kB contra 500—, así que no era señal sino ruido permanente: un aviso que nadie vuelve a mirar es exactamente lo que la ficha temía al negarse a subir el techo en S113. El pago es subir `maximumWarning` a 550 kB dejando el error en 1 MB, con lo que un aviso nuevo vuelve a significar algo. Delta de la sesión: 521,03 → 524,94 kB (+2,06 kB por C-tokens y la tanda 1, +0,77 kB por la tanda 2; el crecimiento son los nombres de token, más largos que los hex, y el comprimido incluso BAJA de 118,11 a 118,01 kB porque los `var(--color-*)` repetidos comprimen mejor). Margen restante: ~25 kB. **CERRADA** |
| D-gh6-tutor-contradictorio (el modelo se contradice sobre de qué grupo es tutor GH6) | O-demo | No \| Nace en S115 al derivar las tutorías del centro real. `modelo_datos_fase1.md` §6.1 registra `ProfesorTutoria(GH6, 1ºESO A)` y el Hallazgo E del mismo documento dice que GH6 es tutor de 1º Bach A; las dos derivaciones son correctas contra los volcados (GH6 imparte tutoría en ambos grupos) y la contradicción es del TEXTO del modelo, no de los datos. No viola I4, que acota los principales por grupo y no los grupos por profesor. Claude Code hizo bien en no resolverla: fijar uno de los dos por criterio propio sería inventar un dato del centro. Se cierra con la respuesta del jefe de estudios sobre los tutores reales, en la misma consulta que la ambigüedad A5 de `ESPECIFICACION-CATALOGO.md`. Hermana de D31: deuda de REQUISITOS |
| D-configuracion-monolitica (la pantalla de configuración es un solo scroll con ocho listas) | O-diseño, o el Cambio que decida la navegación | No \| Nace en S115 del recorrido de la UI contra el tamaño del centro real: 59 profesores, 100 asignaturas, 43 aulas, 334 subgrupos y 219 actividades en un único componente, sin pestañas, sin filtro y sin búsqueda. ACOTADA en la misma sesión y por eso NO abre Cambio: la carga es append-only, así que localizar filas solo duele al corregir; y el riesgo grave que se le atribuyó al proponerla —el clic sin Ctrl del `<select multiple>` de subgrupos, que reemplaza la población entera— es INALCANZABLE con estos datos, porque los 334 subgrupos derivados son todos mono-grupo. Se resuelve donde ya espera D-pdc-lista-rancia: en el Cambio que decida ruta-hija-vs-contenedor, aplazado desde S101. No se paga ahora. **CERRADA en S123 por C-rutas-hijas.** Ocho destinos con URL propia bajo `/configuracion`, índice vertical derivado de las rutas, la página sin scroll y cada lista desplazándose dentro de su panel con la cabecera clavada. Queda para C-listas-filtradas lo que esta deuda también nombraba —el filtro y la búsqueda—, que es el criterio 3 |
| D5, D6, D9, D11, D16, D17, D21, D27, D29 | Fase 5/8 según su asignación en el plan | Deuda de solver/dominio ya asignada; se reevalúa al abrir su objetivo |
| D-ajuste-ciego-fuera-de-vista-grupo (el gesto decide mover o intercambiar contando lo que la vista muestra, y la vista no muestra el horario entero) | O-ajuste-cierre (cerrado S146) | Nace en S145, declarada por el ejecutor en el tipo `AjusteInstancia` antes de que nadie preguntara, y MEDIDA en el M4. Es la misma ceguera que el javadoc de `slotsOcupados` documenta: en vista por profesor, «celda vacía» significa *este profesor está libre*, no *no hay clase*. Medido sobre el centro real: 18 de 30 celdas vacías para BYG1, y soltar en una manda `mover`, que sobre este centro fracasa siempre (29 de 29, S143); el 409 devuelto nombra `Ing-1ºA #4`, una clase que el usuario NO VE en esa vista. **No es un fallo y la disposición es correcta**: el veredicto lo emite el servidor, que ve el horario entero, así que el gesto nunca miente ni escribe algo inválido. Lo que queda es que el gesto solo es cómodo desde la vista por grupo, y que el mensaje de rechazo puede nombrar lo invisible. No bloquea. Mejora futura, no se paga |
| D-lado-instancia-en-prosa (el 404 del ajuste nombra cuál de las dos instancias falta interpolándolo en el texto, no en un campo) | O-ajuste-cierre (cerrado S146) | Nace en S145. `mensajeAjuste()` para `INSTANCIA_INEXISTENTE` MUESTRA el mensaje del servidor con un prefijo propio delante; verificado que NO lo parsea —ni `indexOf`, ni `match`, ni `split`, ni comparación contra 'primera'/'segunda'—, así que si el servidor reescribe la frase la salida cambia de redacción pero no se rompe ni miente. Por eso NO es deuda técnica real: nada está mal hecho. Lo que queda es acoplamiento de la copia de la UI a prosa del servidor que ningún test del cliente vigila. Arreglo si algún día hace falta: un campo `lado` en el cuerpo. No bloquea. Mejora futura, no se paga |
| ~~D-aviso-fuera-del-dialogo~~ (un AVISO de pre-validación no llega al diálogo de confirmar la generación) **CERRADA S203** | `O-datos-centro` (S200; antes sin sede) | Nace en S146. `generar()` (`horario-view.ts:571`) filtra por `severidad === 'ERROR'` antes de abrir `ConfirmarGeneracion`, así que el AVISO de S8 —el primero con productor— sólo se ve en el panel, que arranca colapsado cuando no hay ERROR. Medido en el M4 de S146 sobre el centro real: panel colapsado, contador «otros hallazgos» a 2 y diálogo idéntico al de un catálogo sin hallazgos. No es deuda técnica real: el diseño de AVISO de S92 es exactamente este y nada está mal hecho, y perderse el aviso cuesta poco, porque S8 no depende de la colocación y se corrige cambiando el tutor sin regenerar. Sin sede porque O-ajuste-cierre cierra en la misma sesión, y colgarla de un objetivo cerrado equivale a decidir que no se paga nunca (precedente de S141). Arreglo si algún día hace falta: pasar también los AVISO al diálogo, con su propio rótulo **S203:** su cita `horario-view.ts:571` es hoy `:612` (filtro) y `:614` (apertura). **Saldada en S203** (`875ea3f`): `generar()` pasa errores y avisos por separado y el diálogo muestra el bloque «Avisos (no impiden generar)»; el botón depende solo de los ERROR. |
| D-curso-sin-borrado (un curso no se puede eliminar desde la aplicación) | Sin sede desde S173: no la toca `O-ci`, y no hay objetivo planificado después de H4 (era `O-aceptación`) | Nace en el M1 de S160, tras el selector: la aplicación lista, abre y duplica cursos, pero no borra, así que un duplicado con el año equivocado sólo se quita borrando el fichero en la carpeta de datos. No está en el criterio de `O-curso` ni en el paso 6 de §1, y es destructivo sobre el histórico del centro, con preguntas sin decidir: qué se puede borrar (¿el activo?, ¿el último?) y con qué confirmación. La unicidad del nombre por contenido (S160, `bc0355a`) quita el caso más probable. Disposición: se revisa al cerrar `O-curso`, y o pasa a condición del criterio o queda como mejora. **Revisada en S163 al cerrar `O-curso`:** queda como MEJORA FUTURA, por decisión del usuario. Duplicar por error no pierde datos, y un curso mal nombrado sólo estorba en la lista y se quita borrando su fichero con la aplicación cerrada. Pasa a colgar del objetivo de aceptación y se reabre si ésta muestra que hace falta. **S168:** fuera del criterio de `O-aceptación` (D). **S188:** lo pidió la secretaria en la demo (T9). |
| D-curso-sin-renombrar (un curso no se puede renombrar) | Sin objetivo asignado | Nace en S167, medida en la fase 6-B a petición del usuario: el nombre se fija una vez, al duplicar una base que aún no lo tiene; `DuplicadorCurso` rechaza con 400 otro nombre y no hay endpoint ni acción. Pregunta abierta: si un curso archivado admite renombrarse, cuando la guarda de solo lectura rechaza hoy toda escritura sobre él. |
| D-bundle-sobre-techo-550 (el aviso de presupuesto del bundle vuelve a estar encendido) | Sin objetivo asignado (O-diseño, su sede anterior, terminó en S133) | Nace en S167: `D-bundle-presupuesto` se cerró en S121 con el techo en 550 kB, y en `fae2168` el bundle ya medía 571,67 kB sin que nadie lo registrara; S167 lo lleva a 588,34 kB (+16,67 por el diálogo). `maximumError` sigue en 1 MB. Anotar el delta en cada sesión que compile. S185: 597,54 kB (+5,13 por `FiltroOpciones`, frente a 592,41 kB en `630bb71`); entre los 588,34 de S167 y los 592,41 no hay registro. **S193:** 598,68 kB: 597,63 en HEAD `3850982` (+0,09 frente a S185, sin registro entre medio) y +1,05 por la línea de versión (`1bf9855`). **S203:** 608,65 kB (+7,87 frente a 600,78 en `7db1ae6`), por las columnas, formularios y diálogos de `C-totales-y-cargo`. |
| D-selectores-combobox (el filtro de los selectores es un campo aparte y el multiselect nativo exige Ctrl+clic) | Sin objetivo asignado; se revisa con lo que diga la demo | Nace en el M4 de S185, del usuario: prefiere un combobox con el filtro en el propio control. Candidato Angular Aria, en developer preview y con cambio incompatible de API en mayo de 2026; coste sin medir. Texto íntegro en el plan. S188: AYUDA en la demo (T2): con el filtro del selector no veía que la lista cambiaba, y se le dijo que abriera el desplegable; el filtro no abre la lista ni cambia la selección. |
| D-tiempo-elegido-no-se-recuerda (un clic fuera cancela el diálogo de generación sin aviso y, al reabrirlo, el tiempo vuelve a 10 minutos) | Sin objetivo asignado; se revisa con lo que diga la demo | Nace en S186 (F3 de `C-version-pre-demo`): le pasó al usuario, y la primera generación de la VM corrió 10 minutos en vez de 60. Las dos piezas son de diseño (S145 y S184); el único indicio es «de 10:00» en la barra. Texto íntegro en el plan. S188: revisada con la demo: no se dio. |
| D-vista-horario-no-se-descubre (la secretaria no descubrió que los horarios se ven en pantalla) | `O-interfaz` | Nace en S188, demo con la secretaria (T2); sale de `O-demo-bundle` (R-incidencia): descargó el PDF de grupos y buscó en él; AYUDA. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-interfaz`. |
| D-error-poco-visible (los errores se muestran como un párrafo rojo sin fondo que no llama la atención y no se entiende) | `O-interfaz` | Nace en S188, demo con la secretaria (T5); sale de `O-demo-bundle` (R-incidencia): el 409 de borrar BYG1 no le llamó la atención ni lo entendió; AYUDA. Es `app-estado-lista`, el mismo en las siete listas. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-interfaz`. |
| D-tiempo-generacion-poco-claro (el diálogo de generación explica los tiempos en una línea y no bastó) | `O-interfaz` | Nace en S188, demo con la secretaria (T8); sale de `O-demo-bundle` (R-incidencia): con horario ya obtenido en 10 minutos preguntó para qué sirven los otros tiempos; AYUDA, sin atasco. Distinta de `D-tiempo-elegido-no-se-recuerda`. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-interfaz`. |
| D-matricula-fuera (la matrícula no entra en Educhronos: ni alumnos por optativa, ni itinerarios, ni preferencias) | `O-aulas` (solo alumnos por subgrupo) | Nace en S189, inventario de huecos (filas 2b, 3 y 5); sale de `O-demo-bundle` (R-incidencia). HUECO FUNCIONAL, no bloqueante hoy; lo decide el criterio del objetivo (3). Sin medir: si es la empresa quien convierte la matrícula en grupos de optativa. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-aulas` (solo alumnos por subgrupo). Pregunta abierta CERRADA: los grupos de optativa los decide el instituto (usuario). Los alumnos por subgrupo entran como dato para descartar aulas por capacidad (hojas 2 y 3 del Excel de aulas); el resto de la matrícula queda sin sede. |
| D-guardias-sin-modelo (las guardias no son un dato: ni su número por profesor, ni su cobertura por tramo, ni las de recreo) | `O-guardias` | Nace en S189, inventario de huecos (fila 9); sale de `O-demo-bundle` (R-incidencia). HUECO FUNCIONAL, no bloqueante hoy. Pesa en la comparación (5). Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-guardias`. **S190 (usuario):** regla: las guardias se reparten entre los profesores según el número de cada uno; en cada tramo lectivo hay al menos 4 profesores de guardia; una guardia nunca coincide con una clase del profesor. Cuadra con el horario de guardias de 2026/2027, que tiene de 20 a 25 nombres por franja en cinco días (recuento por lectura en S190). Las guardias de recreo, biblioteca y R.Convivencia no tienen regla todavía; se preguntan al abrir O-guardias. **S199 (condición 5 de `O-carga-2026`):** en el horario oficial de 2026/2027, 150 celdas de guardia lectiva (`G` 139, `Gbibl` 9, `conv` 2) y 38 de recreo (`GR` 33 sin aula, `GRBib` 5 en `GrecB`), en `INFORME-NO-REPRESENTABLE.md`. |
| ~~D-totales-sin-contraste~~ (los totales que entrega el centro no se pueden declarar ni contrastar con lo configurado) **CERRADA S203** | `O-datos-centro` | Nace en S189, inventario de huecos (fila 8) y propuesta del usuario de avisar cuando lo redundante no cuadra; sale de `O-demo-bundle` (R-incidencia). HUECO FUNCIONAL, no bloqueante hoy. Caso particular: `D-borrado-sin-control-de-horas`. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-datos-centro`. **Saldada en S203** (`f381bdd`, `7db1ae6` y `7137833`): total declarado opcional en profesor, grupo y PDC, AVISO de cuadre en los dos sentidos y columna «Horas» en las listas. |
| ~~D-cargos-sin-modelo~~ (no hay cargos, y sus reuniones solo entran montadas a mano) **CERRADA S203** | `O-datos-centro` | Nace en S189, inventario de huecos (filas 12 y 13); sale de `O-demo-bundle` (R-incidencia). HUECO FUNCIONAL, no bloqueante hoy. El rodeo de las reuniones depende de `D-plaza-sin-subgrupos`. Texto íntegro en el plan. **S190:** sede de la replanificación de H5: `O-datos-centro`. Reuniones como actividad de primera clase reutilizando la plaza sin subgrupos; el cargo como dato del profesor. **S199 (condición 5 de `O-carga-2026`, decisión del usuario):** el alcance se amplía a las funciones y proyectos con horas fijas sin alumnos (`ORYCA`, `APSTE`, `FOREI`, `HUERT`, `PROAR` y `REYR`, 20 celdas en 2026/2027), con el mismo arreglo que las reuniones. En 2026/2027, además, `RED` (7 celdas) y `RT12` y `RT34` (20), estas como reuniones de tutores por hipótesis sin medir, pendiente de la secretaria. En `INFORME-NO-REPRESENTABLE.md`. **Saldada en S203** (`f381bdd` y `7137833`): cargo del profesor, de un enumerado, editable en su formulario y visible en su lista; las reuniones y funciones son actividades de primera clase desde S201. El cargo en los PDF queda fuera del criterio. |

#### Decisión arquitectónica consciente → sale de la cola
| Deuda | Razón (ya escrita en el plan) |
|---|---|
| Las tres palancas de altura de celda medidas en S122 y NO aplicadas | Nacen y mueren en el mismo sitio: D11 de `docs/diseno-navegacion.md`. Para que una celda de seis plazas dejara de recortarse haría falta bajar de 154,2 px, y hay tres formas medidas de conseguirlo, cada una a cambio de retirar algo: **(a)** la banda del badge, 16 px fijos por instancia —retira el sitio del badge y del rótulo de bloque—; **(b)** el `padding` y el `margin-bottom` de cada plaza de bloque, ~4,2 px por plaza, 25 px en una celda de seis —retira separación entre plazas simultáneas—; **(c)** bajar `.asig` de `--tam-s` a `--tam-xs` en modo bloque, 15 px —retira jerarquía tipográfica justo en la celda más densa—. **NO se aplica ninguna**, y la razón es que la pregunta caducó: perseguir cero recortes dejó de tener sentido en cuanto el mecanismo de expansión se hizo OBLIGATORIO (criterio 4 de O-navegación). Con expansión, recortar 22 celdas de 791 es densidad; sin ella era pérdida de información. Quedan registradas por si al implementar C-rejilla-densidad se busca holgura: son opciones MEDIDAS, no ideas. **Corrección de S123: son DOS palancas disponibles, no tres.** La (a) choca con D4, que pone el rótulo común del bloque en la banda que el badge reservaba: aplicarla borra a la vez el badge de coste blando y el rótulo, y eso es retirar una señal existente, que es exactamente lo que la invariante del encargo prohíbe y por lo que D6 rechazó borrar la cuarta línea. El razonamiento de «no se aplica ninguna» SIGUE EN PIE: con el portátil fuera del criterio (S123), el criterio se verifica a escala 100 %, donde los recortes siguen siendo 22 de 791. Queda registrado, por si alguna vez se reabre la limitación del portátil, que a 585 px las tres juntas NO bastarían: la celda de seis baja a 98,00 px contra un alto de fila de 87,73. `diseno-navegacion.md` §5 lista esta cuestión como SIN DECIDIR; manda esta tabla |
| D-F8.6-B-b | "ACEPTADA POR DISEÑO": el aviso de ocupación es ciego a propósito |
| D-F8.2b-4B | "condicional, inerte": la poda que defendería está muerta en todo camino vivo |
| 8.5-D3 | "APLAZADO INDEFINIDAMENTE, decisión explícita" con criterio de reapertura escrito |
| D-F8.2b-iii-A-a | Decisión consciente de S62 (no refactorizar los 12 repos en un bloque funcional) |
| D-F8.2b-iv-a | Espejo de validación aceptado conscientemente, con test de contrato que lo vigila |
| D-S104-tipo (Grupo `tipo` fijo a ORDINARIO en la UI) | Decisión de S104: el CRUD plano de catálogo crea grupos ORDINARIOS. PDC y virtuales de optativa (`DIVERSIFICACION_PDC`/`VIRTUAL_OPTATIVA`) son de O-estructura, no de O-catálogo, y ya tienen vía propia (PDC vive en `/api/grupos/{idPadre}/pdc` desde S76). El backend impone la lista blanca (`validarTipo`→400); la UI no expone el campo y el form inyecta `tipo:'ORDINARIO'` en el cuerpo. No es deuda: es la frontera correcta entre O-catálogo y O-estructura, la misma con que S103 dejó `aulas-compatibles` fuera |
| Frontera Fase 2→3 en Subgrupo | Corregida en S14; es nota de diseño, no pendiente |
| Sin firma de código del ejecutable (S156) | Decidido por el usuario en `C-windows-limpio`: la entrega es por USB, que no marca el fichero como descargado y por eso no dispara SmartScreen, y en la máquina de prueba el Control inteligente de aplicaciones está desactivado. Se reabre si la aplicación pasa a distribuirse por descarga o correo, o si un equipo del centro bloquea el ejecutable |
| D-nombres-reales-en-repo (S195) | Decidido por el usuario al abrir `O-carga-2026` (decisión B): los horarios del centro son públicos, así que los de 2026/2027 siguen el tratamiento de los de 2025/2026 —PDF del programa y volcados versionados en `docs/horario-referencia/`, con el repo público—, y la misma razón cubre los 8 ficheros publicados que midió S190. La razón descansa en la afirmación del usuario. No se versionan los retoques en Word ni el Excel de distribución de aulas, que son documentos internos; del parche de aulas, solo los cambios derivados. Se reabre si algún documento versionado resulta no ser público |

#### Limitación conocida → sale de la cola, se documenta el "no se hará"
| Deuda | Razón |
|---|---|
| El horario del centro real no cabe sin scroll en el portátil del jefe de estudios (S123) | MEDIDO: panel 1920×1080 con escala de Windows al 150 %, viewport CSS de 1280×585. `scripts/calcular-recortes.py` da 87,73 px de alto de fila y **109 celdas recortadas de 791 (13,8 %)** frente a las 22 (2,8 %) del escenario de criterio; alcanzan a toda celda de tres plazas o más. Las tres palancas de S122 juntas no lo salvan: faltan 10,27 px. DECISIÓN CONSCIENTE de S123: el portátil sale del criterio 4, que se verifica sobre el sobremesa donde se enseña la demo. REABRE si el uso diario pasa a ese equipo, o si se fija su escala al 100 % —que lo resolvería entero, a cambio de texto del sistema a ~11-12 px físicos— |
| D-F8.5-E-c | "de FRAMEWORK": el dialecto de comunidad no clasifica los fallos; depende de Hibernate |
| D-F8.5-E-a | `peso` es superficie muerta en tres capas; no se activa hasta que el solver lo lea **NOTA de S159:** `O-disponibilidad` no expondrá `peso`; su rejilla tendrá tres estados, `{sin fila, BLANDA, DURA}`. |
| D2, D3, D8, D12, D22(parcial) | Simplificaciones de Fase 1 condicionadas a datos reales o a fases futuras concretas |
| D25 (contención de CPU en -Pescala) | No bloquea la suite rápida; se aborda solo antes de usar -Pescala como gate de CI (Fase 12) |
| Puerto 8080 fijo (S154) | Educhronos escucha siempre en `127.0.0.1:8080`: si otro programa lo ocupa, no arranca, aunque lo dice con un diálogo claro (medido en Linux y en Windows). Elegir un puerto libre obligaría a que la segunda instancia supiera cuál usó la primera y a que el usuario no pudiera guardar la dirección. Se reevalúa si aparece en uso real; en la prueba de la condición 3 (S156) no apareció |
| Windows 10 fuera del criterio de instalación (S156) | Decisión del usuario en `C-windows-limpio`: según él, todos los equipos del centro están en Windows 11. Es un dato de segunda mano, no verificado en el centro, y la aplicación sólo se ha probado en Windows 11. Se reabre si aparece en el centro un equipo con Windows 10 |
| D-apagado-espera-generacion (S184: con una generación en vuelo, la aplicación tarda de 35 a 40 s en pararse) | Medido en desarrollo en S184 (F2, F3 y F3-bis, SIGTERM a la JVM): el apagado ordenado de Tomcat espera 30 s a la petición activa, la aborta y cierra el pool; la generación no se guarda. En el bundle, «Salir» desde la bandeja pasaría por el mismo apagado: sin medir. Con la interfaz bloqueada, la bandeja es la única salida a mitad de una generación; cerrar el navegador no para el backend. Sin sede; se revisa en el objetivo (3). Texto íntegro en el plan. **S190:** sede de la replanificación de H5: sin sede. Revisada en S190: no bloquea. |

#### Deuda de MÉTODO → se integra en `metodo.md`, no en el producto
| Deuda | Destino |
|---|---|
| D-F8.0-a | Ya cerrada (motivó escribir el método) |
| D-F8.6-a (aviso de oportunidad de mockup) | Integrada en M-mockup de `metodo.md` |
| D-F8.6-ivA-b, -ivB-b (de método y cobertura) | Integradas en M3 de `metodo.md` |
| D-higiene-sin-apertura (la sesión de Higiene/Método no puede abrirse con las reglas vigentes) | Pendiente de decisión del usuario, en `metodo.md` y §6. Nace en S151. `metodo.md` la admite «cuando el plan lo pide», y el plan la pide desde hace sesiones (18 fichas con esa sede en §4); pero R-apertura exige nombrar Cambio, Objetivo e Hito, que una sesión de método no tiene, y cada M0 la descarta con razón (S121, S142, S147, S151). Es la forma que esta misma sección critica al colgar deudas de objetivos cerrados: una sede que nunca se abre equivale a no pagar. **DECIDIDA en S152 por el usuario: excepción escrita a R-apertura** (§6), con disparador medible. El reparto se descarta con argumento medido: 16 de las 18 fichas están clasificadas como transversales precisamente porque no tienen objetivo natural —defectos de guiones, censo de R4, tokens inexistentes—, y las pocas con sede la tendrían en objetivos cerrados, que es la forma que esta misma sección critica. Repartirlas sería no pagarlas con otro nombre. **CERRADA** |
| D-guion-exit-enmascarado (mitad de método), D-guion-busca-token-esperado, D-guion-pkill-casa-su-propio-envoltorio, D-guion-escribe-donde-no-se-dijo | Integradas en S157 en la sección M-guion de `metodo.md`, ocho normas con la ficha de origen citada en cada una. **CERRADAS** |

#### Deuda ya CERRADA (histórico, no pendiente)
D-F8.6-ivD-b (S99), D-F8.4-B2-a (S94), D-F8.5-D1-a (S77), D-F8.5-D2b1-a/b (S91),
D-F8.5-A-a (S73/S74), D-F8.6-iiiA-a (S85), D-cabecera-lista-duplicada (S124), y las condensadas en la sección de
deuda cerrada del plan. Se conservan como registro con remisión a la bitácora.

**Resultado agregado:** de las ~54 deudas vivas (S113 añade siete, S114 cinco y S115 dos), tras
reclasificar, **0 son bloqueantes** (**CORREGIDO en S135**: esta línea decía «1 es bloqueante ahora, D-F8.6-ii-b y solo al abrir O-ajuste-cierre», y esa deuda está CERRADA desde S83; la última bloqueante real, D31-a, se saldó en S134), ~11 son deuda técnica real que se paga DENTRO de su objetivo
cuando llegue, y el resto (~35) sale de la cola de trabajo activo como mejora
futura que espera, decisión consciente o limitación conocida. La cola de "deudas
que me obligan a abrir sesión" sigue en ~1. Nota sobre la tendencia, visible
desde S109: la cola CRECE sesión a sesión y eso no es alarma por sí solo —el
crecimiento es casi todo de mejora futura y de deuda registrada al medir, no de
deuda técnica real acumulándose sin pagarse—; la métrica que sí hay que vigilar
es "deuda bloqueante abierta" (§7), que lleva en 1 desde que existe el mapa. Confirmado en S115: de las dos deudas que podían morder por primera vez en O-demo, una BAJA de presión (D-horario-irreversible, por la carga repetible) y la otra se CONTIENE con el orden de carga (D-tutor-pdc-desincronizado); ninguna abre sesión. RATIFICADO en S116 con las dos ejercitadas de verdad y tres deudas nuevas nacidas (D-timeout-como-infactible, D-motivo-rechazo-sin-registro, D-generacion-sin-indicador): ninguna bloquea el criterio del objetivo activo y ninguna abre sesión. La sesión tensó R-deuda en su caso más difícil —el hecho en que se apoyaba la ficha de D-F8.6-ii-a resultó FALSO— y la regla aguantó porque la conclusión se sostenía por otro argumento, medido: la prevalidación en seco garantiza que el cargador no reciba ningún 400. Cambiar el fundamento de una deuda no es lo mismo que pagarla. RATIFICADO otra vez en S117, con dos deudas nuevas más (D-generacion-no-reproducible, D-prevalidacion-ciega-a-holgura-cero) y una de método (D-guion-exit-enmascarado): ninguna bloquea el criterio del objetivo activo. La sesión tensó R-deuda por el lado contrario al de S116 —aquí la tentación era PAGAR D-motivo-rechazo-sin-registro como instrumento de la medición— y la regla aguantó porque la medición demostró que el instrumento no hacía falta: el estado de CP-SAT viaja dentro del mensaje de la excepción y un arnés en proceso lo lee. Se registra además el caso inverso y honesto: si el cierre de C-generación toca `GeneradorHorarioService:201`, distinguir UNKNOWN de INFEASIBLE cae en el mismo camino de fallo y la deuda se cubre DE PASO. Encontrarse una deuda haciendo el trabajo del Cambio no es abrir una sesión para ella. RATIFICADO en S118, y es la sesión donde ese corolario se COBRA: dos deudas —D-timeout-como-infactible y D-generacion-sin-indicador— quedan CERRADAS sin que ninguna abriera sesión, pagadas de paso porque caían en el camino de fallo que C-generación tenía que tocar. La regla se tensó además por un lado nuevo: la sesión midió que el arreglo de D-F8.6-ii-a es de UNA LÍNEA (la clave de Boot 4) y aun así NO se pagó, porque cambia el cuerpo de error de toda la superficie REST y no bloquea el criterio; barato no es lo mismo que en alcance. Deuda bloqueante abierta: sigue en 1 (las aulas de FPB, D31-a). RATIFICADO en S119 y en S120 sin novedad, y la de S120 por el lado más fácil de todos: nacen cuatro tokens —D-vista-horario-sin-horario, D-selectores-sin-busqueda, D-actividad-forma-implicita, D-arranque-no-literal— y ninguno se paga, porque la sesión no tocó ningún camino de fallo y por tanto tampoco hubo nada que cubrir DE PASO. Lo que S120 sí tensó fue R-apertura contra R-deuda en la elección de sesión: el script de R4 tenía su mejor caso hasta la fecha (S119 midió que la verificación manual YA falló una vez) y perdió igualmente, porque no bloquea el criterio del objetivo activo y el Cambio competidor sí podía nombrar los tres términos del mapa. **RATIFICADO en S121 con el caso más interesante hasta la fecha, porque por primera vez se COBRA una deuda dentro del objetivo que la tenía asignada.** D-bundle-presupuesto se salda en C-tokens sin abrir sesión y sin bloquear: cae en el camino del Cambio, O-diseño era su sede escrita, y el hecho nuevo que justifica el pago es que el aviso ya estaba encendido antes de tocar nada, luego el techo no era una red de seguridad sino ruido. Al mismo tiempo la regla aguantó por el otro lado: siete deudas colgadas de O-diseño —las seis de UX más D-dialogo-foco-perdido— se dejan expresamente FUERA de su criterio en el M0, para que el objetivo de acabado no se convierta en rehacer la UI. Deuda bloqueante abierta: sigue en 1 (D31-a). Nacen tres deudas (D-sin-puntos-de-ruptura, D-select-nativo-desparejo, D-prevalidacion-contraste-sin-ver), ninguna bloquea, y dos de ellas tienen sede dentro del propio criterio, en C-revisión. **RATIFICADO en S134 en su caso más fácil hasta la fecha**: nacen SIETE deudas de una sentada —`D-hallazgo-E-refutado`, `D-tutores-bachillerato`, `D-nombres-sin-codigo`, `D-censo-profesores-80-59`, `D-itinerario-como-grupo`, `D-taller5-inexistente` y `D-fuente-tercera-sin-usar`— y ninguna abre sesión ni bloquea nada. Y la sesión cierra la única que bloqueaba: **`D31-a` SALDADA, con lo que la deuda bloqueante abierta pasa de 1 a 0 por primera vez desde que existe el mapa.** La regla se tensó además por un lado nuevo, el de la deuda que resulta NO EXISTIR: el M0 de la sesión midió que `D-F8.6-ii-b` está CERRADA desde S83 y que este documento la afirma viva y bloqueante en tres sitios, de modo que O-ajuste-cierre llevaba sesiones ofreciéndose como candidato por una deuda muerta. Corregirlo NO se hizo en S134 y queda pendiente. **HECHO en S135**, en su cierre y como pago R5: los cuatro sitios corregidos. **RATIFICADO en S135 por los DOS lados el mismo día.** Se COBRA de paso la segunda instancia de `D-plan-duplicado` —el informe de reconciliación duplicado— porque caía de lleno en el camino del Cambio: el instrumento de la capa 2 tomaba su mapa de códigos de ese fichero, y escribir en el código una regla que su fuente niega es la enfermedad y no el arreglo. Y se DENIEGA en el mismo turno la tipificación de las 43 aulas (`D-aula-tipo-sin-uso-real`), que estaba igual de a mano y no cae en el camino: se da de alta Taller 5 con criterio propio y no se toca ninguna de las otras. Nacen dos deudas y ninguna abre sesión. Deuda bloqueante abierta: **0**. 
**RATIFICADO en S128, y es la segunda vez que se COBRAN deudas dentro del objetivo que las tenía asignadas.**
`D-insignia-sin-leyenda` se paga en C-identidad, que era su sede escrita, con un hecho nuevo que la ficha de S122
no podía tener: desde S127 esa esquina de la celda tiene DOS números con signo y sólo uno llevaba explicación.
`D-vacio-miente-con-error` se cierra POR CONSTRUCCIÓN y ANTES de su sede (era C-revisión), sin abrir sesión y sin
que nadie decidiera pagarla: centralizar la precedencia la deja sin sitio donde ocurrir, igual que
`D-pdc-lista-rancia` en S123. La regla aguantó por el otro lado en el mismo M4: `D-contador-se-apaga-con-error`
—la misma mentira, viva en el contador— se REGISTRA y no se paga, porque su arreglo cambia el contrato de
`app-cabecera-lista` y eso está fuera del alcance declarado. Nacen tres deudas (`D-horario-id-a-fuego`,
`D-contador-se-apaga-con-error`, `D-748-sin-derivacion`), ninguna bloquea, y las tres tienen sede. La sesión de
Higiene/Método vuelve a perder por R-deuda —van ocho— y en esta engorda dos veces: `D-guion-exit-enmascarado` suma
su cuarta y quinta instancia, las dos en guiones de LECTURA. Deuda bloqueante abierta: sigue en 1 (D31-a).
RATIFICADO en S119, y es el caso más limpio hasta la fecha porque **no nace ninguna deuda nueva**: la sesión
midió sin escribir producto y todo lo que encontró ya estaba registrado —el hueco 219/208 es D31-a, el
`indispBlanda = 0` vacuo es la limitación de datos que O-demo declara desde S115—. Solo se afina, y a la
baja, D-diagnostico-no-es-foto, por haberla usado como instrumento y medir que aquí era vacua. La regla se
tensó por un lado nuevo y aguantó: las cuatro deudas que el prompt de apertura puso sobre la mesa
(D-generacion-no-reproducible, D-prevalidacion-ciega-a-holgura-cero, D-presupuesto-anunciado-espejo y
D-F8.6-ii-a) se encuadraron una a una con argumento propio y ninguna abrió sesión; la primera muerde en el
COSTE de la sesión —si hay que regenerar son diez minutos y una tirada de 3/4— y no en la aserción, que es
la distinción que evitó confundir «esta deuda me molesta» con «esta deuda me bloquea». Deuda bloqueante
abierta: sigue en 1 (D31-a).
**RATIFICADO en S129, y es la TERCERA vez que se cobran deudas dentro del objetivo que las tenía asignadas —y la
primera en que una nace y muere en la misma sesión.** `D-contador-se-apaga-con-error` y
`D-prevalidacion-contraste-sin-ver` se pagan en C-revisión, su sede escrita, y las dos con un hecho nuevo que sus
fichas no tenían: la primera, que el arreglo NO exige las dos señales que su ficha proponía; la segunda, que la
verificación pendiente desde S121 no era difícil sino IMPOSIBLE, porque nadie emite `AVISO`.
`D-desbordamiento-sin-etiqueta` nace y cierra en S129 al destaparla el censo mientras se buscaba el molde de una
deuda ya cerrada: encontrarse una deuda haciendo el trabajo del Cambio, otra vez, no es abrir una sesión para
ella. La regla aguantó por el otro lado en la misma sesión y cinco veces: no se funden las nueve reglas `__input`
idénticas —la fusión de S128 estaba dentro de una de las seis decisiones y «tratamiento de controles» no es
ninguna—, no se maquetan los cuatro ganchos sin regla de `panel-prevalidacion`, no se estrena `--fuente-datos` en
solitario, no se unifican las cuatro convenciones de nombre de clases de estado, y no se abre caso para un
`!cargando()` que la sesión no cambia. Nace UNA deuda (`D-tokens-sin-uso`), se corrigen dos fichas cuyo texto era
falso o incompleto y `D-guion-exit-enmascarado` engorda de golpe con SIETE instancias, todas del arquitecto y
todas con la misma causa raíz —patrones escritos de memoria—, una de ellas reincidencia sobre un fallo
diagnosticado dos guiones antes. La sesión de Higiene/Método vuelve a perder por R-deuda y van NUEVE. Deuda
bloqueante abierta: sigue en 1 (D31-a).
**RATIFICADO en S144, y la regla se tensa por un lado nuevo: el de la cobertura que se paga
DENTRO del Cambio sin ser deuda de cobertura.** La campaña de mutación dejó vivo `m3` —comparar
las violaciones por posición en vez de por multiconjunto pasaba los 30 casos existentes—, hueco
abierto desde S143 y no introducido aquí. Se pagó, y el argumento es que `soloNuevas` decide QUÉ
se le nombra al usuario, que es literalmente la condición (2) del criterio activo: no es la
familia F8.6 que el propósito del objetivo excluye por escrito, es la salida normal de un M3
sobre el método que este Cambio acaba de generalizar y compartir. Deuda bloqueante del
proyecto: **0**.

**[LAGUNA]** Este documento asigna categoría, objetivo y disposición a cada
deuda. NO reescribe el texto íntegro de cada una: ese vive en
`plan_trabajo_horarios.md`, que sigue siendo su fuente. Si al abrir un objetivo
una deuda concreta necesita re-lectura, se lee del plan.

---

## 5. Revisión del roadmap: por qué H2 va primero

**AVISO DE VIGENCIA (S143):** esta sección narra el diagnóstico con el que se decidió
priorizar H2, y sus cifras son de ENTONCES. Hoy son falsas y se conservan por registro,
no como estado. «H2 al 0%» caducó en S141, cuando H2 CERRÓ. El «H1 al 90%» lo declaró
insostenible §2 en S142 y S143 lo agrava: la condición 1 del criterio de O-ajuste-cierre
no es sólo incumplida, es NO PRODUCIBLE sobre el centro real —28 grupos a 30/30, 29 de 29
destinos rechazados en dos instancias de tamaño distinto—, de modo que un porcentaje no
es defendible mientras la mitad que falta no tenga siquiera una forma verificable
acordada. El estado vivo de H1 está en §2 y en la ficha de O-ajuste-cierre de §3, no aquí.

El roadmap original ejecutó la Fase 8 en orden de DEPENDENCIA TÉCNICA DEL BACKEND
(8.1 vía REST → 8.2 solver de pines → 8.3 diagnóstico → 8.6 vista → tests de la
vista). Ese orden es impecable desde el código y contraproducente desde el
producto: construyó toda la maquinaria de AJUSTAR (H1) antes de tocar la de CREAR
(H2), cuando crear es el prerequisito de valor. Resultado medido: ~43 sesiones en
Fase 8, H1 al 90%, H2 al 0% (dato de entonces; ver aviso de vigencia).

**Orden nuevo: O-shell → O-catálogo → O-estructura → O-demo → O-particiones (todo H2),
luego O-ajuste-cierre (H1), luego O-diseño (acabado visual, con las vistas ya
congeladas), luego H3, luego H4.**

**REVISADO en S115**, con dos cambios que no alteran el argumento de fondo. (1) Nace
O-particiones: el criterio 6 de Fase 8 se midió y no tenía constructor, así que H2 cierra
con dos objetivos por delante y no con uno. (2) La salvedad de prioridad de O-diseño se
ACTIVA (hay demo al centro), y su dependencia se estrecha de «H2 cerrado» a «O-demo»,
porque la razón original —O-estructura reorganiza Configuración entera— se consumió al
cerrar O-estructura en S114. Orden operativo hacia la demo: **O-demo → O-diseño → demo →
O-particiones → cierre de H2.**

**EJECUTADO en S121:** O-diseño ABRE, con O-demo todavía abierto y sin trabajo
ejecutable (bloqueado por D31-a, que espera un correo al centro). Las dos
alternativas que competían quedan descartadas con argumento: la Higiene/Método del
script de R4 pierde por tercera vez porque no puede nombrar los tres términos y
R-deuda excluye sus casos; y O-particiones no se adelanta porque la demo no lo
necesita, arrastra cuatro preguntas de dominio sin resolver y no hay razón nueva
para invertir un orden decidido en S115.

- **Qué desbloquea:** O-shell desbloquea todo lo demás (formularios y vista
  necesitan carcasa). Hacer H2 primero hace que H1 y H3 operen sobre datos reales
  creados por UI, no sembrados a mano.
- **Qué evita rehacer:** cerrar H1 DESPUÉS del shell evita pulir una vista que el
  shell reubica. Toda la deuda de cobertura de F8.6 que hoy tienta a pagarse se
  vuelve irrelevante o se reescribe UNA sola vez, bajo el shell definitivo.
- **Qué reduce riesgo:** O-estructura valida el modelo unificado contra el
  usuario. Es el mayor riesgo del proyecto. Hacerlo antes que exportación y
  empaquetado significa descubrir el riesgo grande temprano, con margen.
- **Qué acorta el tiempo:** elimina el trabajo de pulido de H1 que el orden
  actual invita a hacer (la familia F8.6) y evita la reescritura post-shell. No
  es una estimación numérica —no hay datos para cuantificarla—; es la eliminación
  de una categoría entera de trabajo.

**Contraargumento honesto:** H1 está al 90% (dato de entonces; ver aviso de vigencia) y terminarlo da
sensación de cierre.
Pero terminar H1 antes de H2 es terminar la mitad que no se puede usar. La
sensación de progreso es la trampa que tiende el orden por dependencias. Por eso
H1 se marca explícitamente "EN PAUSA al 90%, suficiente" (dato de entonces; ver aviso de vigencia) y se salta
a H2.

**Decisión reversible:** el orden H2-primero y el grano de cuatro hitos se
adoptan como base argumentada. Si al ejecutar se revela una razón para otro orden
o grano, es un cambio localizado en este documento, no un rehacer.

---

## 6. Reglas estratégicas

Estas reglas hacen que las buenas decisiones se desprendan de la estructura, no
del criterio puntual del arquitecto en cada sesión. Son parte del método (viven
operativamente en `metodo.md` como M0 y M5); se enuncian aquí porque su
justificación es de gestión.

**R-deuda — La deuda nunca planifica; solo registra.**
Ninguna sesión se abre para cerrar una deuda, salvo que esa deuda BLOQUEE el
criterio de terminado del objetivo activo. La deuda se registra colgada de un
objetivo y se salda dentro de él.

**R-invalidación — No refinar lo que será rehecho.**
No refines una pieza más allá del criterio de terminado de su objetivo si un
objetivo POSTERIOR YA PLANIFICADO va previsiblemente a rehacerla. El anclaje es
"ya planificado en el roadmap", no hipotético. En este sistema es casi redundante
(si solo trabajas el objetivo activo en orden de dependencia, no tocas zona de
objetivos futuros), pero se conserva como red para el caso límite.

**R-terminado — Un objetivo termina cuando cumple su criterio.**
Un objetivo termina cuando cumple su criterio de terminado. Las mejoras conocidas
que no cambian ese criterio ni desbloquean el siguiente objetivo se registran
como mejora futura y NO se ejecutan dentro de este objetivo. Es el freno que las
sesiones S84–S99 no tuvieron: convierte "¿sigo puliendo?" de juicio subjetivo en
pregunta binaria — ¿cambia el criterio de terminado del objetivo? No → para.

**R-apertura — Toda sesión nombra su lugar en el mapa.**
Antes de abrir una sesión hay que poder responder: ¿qué Cambio avanza? ¿qué
Objetivo avanza? ¿qué Hito acerca? ¿toca trabajo que un objetivo planificado
invalidará? Si no hay respuesta a las tres primeras, la sesión no se abre. (Vive
como M0 en `metodo.md`.)

**Excepción de Higiene/Método a R-apertura (S152, decidida por el usuario).**
Una sesión de tipo Higiene/Método puede abrirse sin nombrar Cambio, Objetivo ni
Hito, con dos condiciones. **Cuándo:** sólo ENTRE objetivos, nunca con un objetivo
activo a medias, para que no compita con el trabajo de producto. **Con qué
disparador:** cuando las fichas de §4 cuya sede sea Higiene/Método o el script de
R4 lleguen a VEINTE, o cuando en una misma sesión un mismo defecto de instrumento cueste
trabajo medible por segunda vez. Lo que avanza no es un hito: es la fiabilidad de
los instrumentos con que se mide todo lo demás, y por eso R-apertura no le encaja.
La sesión cierra con el recuento actualizado, que es su criterio de terminado.
**PRECISADO en S157:** el recuento por sí solo no es criterio, porque se cumple sin saldar nada. El M0 de la sesión fija qué fichas salda y con qué cifra de salida, y la sesión cierra al alcanzarla. S157 fijó 19 → 11 y lo alcanzó.
Nace de `D-higiene-sin-apertura` (S151), que midió que con las reglas vigentes esa
sesión no podía abrirse nunca y que cuatro M0 la descartaron con razón.

**AMPLIADA en S178 (decisión del usuario):** cuando ninguna sesión puede abrirse con
las reglas vigentes —ningún objetivo planificado y ningún disparador—, el usuario
puede abrir por decisión suya una sesión de Higiene/Método para decidir qué se
planifica; su criterio de terminado lo fija su M0. S178 es la primera: el disparador
que el registro de S177 daba por saltado no saltó, porque hubo un solo fallo con
coste.

**PRECISADO en S179 (decisión del usuario, a propuesta del arquitecto):** el segundo
disparador se lee como EL MISMO defecto de instrumento costando trabajo medible por
segunda vez en una misma sesión; dos defectos distintos con un coste cada uno no lo
disparan. Es lo que dice la letra y lo que el disparador quiere detectar: un
instrumento que ya se ha demostrado roto. La otra lectura saltaría con dos errores
sueltos del asistente que no son instrumentos. Un guion es un instrumento (M-guion).
Con esta lectura, S177 no lo disparó y S178 sí, por su defecto (3): el mismo ancla
costó un intento abortado y un parche que no casó. Nace de
`D-disparador-higiene-ambiguo`.

**R-e2e — El e2e de navegador cubre el guion de aceptación, no la lógica.**
La suite e2e de navegador (Playwright, `app/frontend/e2e/`) verifica ÚNICAMENTE los
eslabones del guion de aceptación de §1: crear centro por UI → generar → ajustar con
drag & drop → exportar → duplicar curso. Un e2e por eslabón, no por caso. La lógica
—casos límite, ramas de error, validaciones, prevalidación— se prueba en la capa
JVM/unidad (donde ya vive: `GenerarHorarioEndpointTest`, ~35 tests de `SolverHorario`,
round-trips, MockMvc del controller) o en unidad de frontend (vitest), NUNCA en
navegador. Un e2e nuevo se justifica solo si verifica un eslabón del guion no cubierto
ya; no se añade "por si acaso" (análoga a R-deuda). Razón: los e2e de navegador son los
tests más caros de mantener y más frágiles; sin este techo la suite crece por inercia
hasta ralentizar el desarrollo. El guion de §1 tiene ~6 eslabones ⇒ la suite tiende a
~6 tests. Si crece mucho más, es señal de que cubre lógica que no le toca: se recorta.

**R-incidencia — El uso real ordena el trabajo (S178, decidida por el usuario).**
Una incidencia es un fallo o una petición que sale del uso del producto por el
centro: la demo, la prueba del profesor o la comparación, objetivos de H5. Se
registra como ficha de §4, con nombre `D-*` para que la vean los instrumentos de R4,
anotando de qué objetivo sale. Antes de darla de alta se busca en §4 si ya existe; si
existe, no se crea un nombre nuevo: la deuda existente toma la sede y la prioridad de
la incidencia. Si bloquea el criterio del objetivo siguiente, se convierte en Cambio
de ese objetivo, o en objetivo propio si no cabe en ninguno; si no, espera. Una
capacidad nueva entra en el roadmap sólo si el profesor la necesita para hacer su
horario; las demás esperan a un estado final posterior. Es R-deuda aplicada al uso:
las deudas que cuelgan de objetivos cerrados no se pagan por turno, sino cuando el
uso real las encuentra.

---

## 7. Métricas del sistema

Miden el SISTEMA, no la productividad individual. Todas se calculan del registro
sin instrumentación nueva.

| Métrica | Cómo se mide | Qué diagnostica |
|---|---|---|
| **Sesiones por objetivo** | Sesiones entre apertura y cierre de cada O | Si los objetivos están bien dimensionados (>8 sesiones ⇒ probablemente esconde varios objetivos, como Fase 8 escondía H1+H2) |
| **% de sesiones que avanzan un hito** | Sesiones cuyo Cambio pertenece al hito activo / total | Cuánta actividad es "producto" vs. tangente. Es la métrica estrella: traduce "el avance parece lento" en número |
| **% tiempo desarrollo vs. gestión** | Sesiones de Desarrollo+Config / (Higiene+Método) | Si el overhead de proceso vuelve a comerse el avance |
| **Evolución de deuda por categoría** | Conteo en las 4 categorías, por sesión | Distingue "deuda técnica real crece" (malo) de "cola total crece por mejoras futuras registradas" (inofensivo) |
| **Deuda bloqueante abierta** | Deudas técnicas reales que bloquean el objetivo activo | El trabajo verdaderamente urgente. Debería estar cerca de 0 casi siempre; un pico dice "el objetivo activo está atascado" |
| **Cambios por objetivo cerrado** | Cambios que cerró cada O | Granularidad real del trabajo |

**Baseline [LAGUNA]:** el sistema viejo no registraba estas métricas. El baseline
se reconstruye aproximadamente del historial (clasificar retroactivamente S57–S99
en "avanzó H1 / avanzó H2 / ninguno"). Es aproximado, no exacto. No se afirma un
número de mejora esperado: sería especular.

---

## 8. El sistema respondiendo a las preguntas clave

Test de si el diseño funciona: las buenas decisiones deben CAER de la estructura.

- **¿Por qué se abre esta sesión?** → Porque avanza el Cambio C, siguiente porción
  por dependencias del Objetivo activo O. Si no hay respuesta, no se abre.
- **¿Qué objetivo hace avanzar?** → O, nombrado en la apertura (M0). Obligatorio.
- **¿Qué hito acerca?** → El hito del que cuelga O, visible en §2.
- **¿Qué trabajo invalida?** → Lo responde R-invalidación al fijar alcance.
- **¿Cuándo dejar de refinar?** → Cuando el criterio de terminado de O se cumple
  (R-terminado). Binario, no opinable.

Ninguna depende ya del criterio puntual del arquitecto en la sesión: todas se leen
del mapa Hito→Objetivo→Cambio.
