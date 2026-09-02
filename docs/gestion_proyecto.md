# Sistema de gestión del proyecto — Educhronos
<!-- INDICE:INICIO -->
<!-- Generado en M1 (M-doc-3). Líneas INDICATIVAS; manda el texto. -->

- L56 — ## 1. Estado final del proyecto
- L82 — ## 2. Hitos
- L96 — ### Hitos: valor, dependencias, orden
- L119 — ## 3. Objetivos técnicos
- L129 — ### H2 — Configurar un centro desde cero
- L131 — #### O-shell — "La aplicación es navegable." ✔ TERMINADO (S100)
- L147 — #### O-catálogo — "Creo los elementos simples del centro." ✔ TERMINADO (S106)
- L219 — #### O-estructura — "Expreso la complejidad real del centro." ✔ TERMINADO (S114)
- L469 — #### O-demo — "El centro real funciona de punta a punta." (ABIERTO S115)
- L725 — #### O-particiones — "Un grupo nuevo entra en el curso sin reconfigurar a mano." (ESBOZADO S115)
- L758 — ### H1 — Ajustar (cierre)
- L760 — #### O-ajuste-cierre — "El ajuste manual está completo y verificado."
- L773 — #### O-diseño — "La aplicación tiene un aspecto cuidado y coherente." (ABIERTO en S121)
- L993 — #### O-navegación — "La aplicación se maneja como una aplicación de escritorio." ✔ TERMINADO (S127)
- L1177 — ## 4. Clasificación del trabajo pendiente
- L1195 — ### Clasificación de las deudas vivas actuales
- L1201 — #### Objetivos disfrazados de deuda → se PROMUEVEN a objetivo (§3)
- L1208 — #### Deuda técnica real, colgada de su objetivo
- L1258 — #### Mejora futura, cuelga y espera
- L1288 — #### Decisión arquitectónica consciente → sale de la cola
- L1300 — #### Limitación conocida → sale de la cola, se documenta el "no se hará"
- L1309 — #### Deuda de MÉTODO → se integra en `metodo.md`, no en el producto
- L1316 — #### Deuda ya CERRADA (histórico, no pendiente)
- L1376 — ## 5. Revisión del roadmap: por qué H2 va primero
- L1430 — ## 6. Reglas estratégicas
- L1477 — ## 7. Métricas del sistema
- L1498 — ## 8. El sistema respondiendo a las preguntas clave

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
   grupos, currículo, desdobles, agrupamientos, PDC, tutores.
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

Hoy el guion falla en el paso 2: no existe interfaz para crear un centro.

---

## 2. Hitos

Un hito es un resultado VISIBLE para el usuario: algo que se le puede enseñar
funcionando. Los cuatro hitos se derivan del estado final y de reagrupar los
seis criterios de verificación de la Fase 8 (que mezclaban "ajustar" y
"configurar" bajo un mismo título) por VALOR DE USUARIO.

| Hito | El usuario puede… | Estado real | Criterio de terminado |
|---|---|---|---|
| **H1 — Ajustar un horario existente** | Ver un horario, moverlo con drag & drop, ver conflictos duros y blandos, bloquear sesiones, relanzar | ~90% | Criterios 1–4 de Fase 8 (drag con conflicto, atribución sobre horario generado, prevalidación, bloqueo). Cumplidos salvo verificación de cadena y el gesto de despinar |
| **H2 — Configurar un centro desde cero** | Crear profesores, aulas, grupos, currículo, desdobles, PDC, tutores por formularios y llegar a un horario válido sin tocar la BD | ~70% (O-shell hecho S100; O-catálogo TERMINADO S104, criterio precisado S106: 4 de 4 entidades CRUD por UI — Profesor (S101), Aula (S102), Asignatura (S103), Grupo (S104). El e2e UI→solver, antes 2ª mitad de O-catálogo, se reasignó a O-estructura en S106 al medirse que depende de currículo/jornada. O-estructura ABIERTO S107, 7 piezas hechas: C-jornada (S107, backend REST `/api/jornada` + formulario singleton, dimensión temporal del solve), C-subgrupos (S108, CRUD de subgrupos por UI sobre `/api/subgrupos`, con multiselect de grupos), C-actividades COMPLETO (trozo A en S109 —editor de Actividad de una plaza + guarda 409 del PUT + fin del vaciado de la BD en cada arranque— y trozo B en S110 —lista de plazas variable con alta/baja e I2 en cliente, con lo que desdobles, agrupamientos y bloques de optativas quedan construibles por UI—), C-niveles (S111, CRUD de Nivel por UI: cerraba el hueco medido en S109 —sin niveles por UI no hay grupos ni subgrupos— y con él las nueve filas del centro mínimo son construibles por pantalla) y C-e2e (S112, el e2e de navegador que crea el centro mínimo por la UI y verifica que el solver produce horario: TERCERA PATA del criterio, CUMPLIDA; incluyó arreglar un hueco funcional real de la primera generación) y C-pdc (S113, alta/consulta/borrado del grupo PDC por UI desde la fila de su padre + dos guardas de backend que impiden que el CRUD plano deshaga el agregado: con él el caso §6.2 del modelo —en su versión válida, la Nota (S23)— se construye íntegramente por pantalla y el solver produce horario sobre él, que es la SEGUNDA PATA demostrada en su caso más difícil) y C-tutores (S114, la asignación del tutor por UI sobre el sub-recurso que existía desde S77: su M0 midió que SÍ hacía falta —tres casos del §6 registran `ProfesorTutoria` en su configuración— y su M4 verificó en navegador que `TUTORIA_SIN_TUTOR` aparece sin tutor y desaparece con él; con ella las TRES PATAS quedan cumplidas y O-estructura CIERRA). «Desdobles y agrupamientos» dejó de ser trabajo propio al medirse que son actividades multiplaza. **O-estructura ✔ TERMINADO S114**, 8 piezas. **O-demo ABIERTO S115** y descompuesto en cinco Cambios; la misma sesión midió que el criterio 6 de Fase 8 no tiene constructor y lo sacó a objetivo propio, **O-particiones**, así que H2 pasa a cerrar con DOS objetivos por delante y no uno. **2 piezas: C-derivación (S115) y C-cargador (S116), con el que el IES completo —804 escrituras por la API REST— está en la base creado por las vías legítimas del producto; y, fuera de alcance, la primera prueba de que ese centro GENERA horario (FEASIBLE, objetivo 188.0), que despeja el mayor riesgo abierto del objetivo). C-generación EN CURSO desde S117, que resolvió dos de sus tres preguntas: la reconciliación de `sesion` (770/819 filas frente a 632 instancias: dos magnitudes distintas, no una discrepancia) y la caracterización del presupuesto (no gobierna la calidad sino la probabilidad de obtener horario; el defecto de 30 s es indefendible). **SEGUNDA PARTE en S118 (primera sesión de Desarrollo desde S114): la PIEZA DE PRODUCTO cerrada —presupuesto configurable `educhronos.solver.max-segundos` con defecto 600, separación 503 PRESUPUESTO_AGOTADO / 422 CATALOGO_INFACTIBLE / 422 CONFIGURACION_INCOMPLETA, y estado de espera en la vista— y el PRIMER HORARIO DEL CENTRO REAL generado por la vía de producción DESDE LA INTERFAZ (770 sesiones, el oráculo de S117 clavado). **C-generación CIERRA en S119 con la TERCERA parte: el contraste con el PDF.** Su M0 fijó qué se asevera como «válido» —el PDF no juzga la validez, es oráculo de CONTENIDO— en tres capas, y las tres se midieron sobre el horario de S118: **cero violaciones de regla dura sobre 770 sesiones** a escala real; **conservación de la carga con 526 claves, 11 divergentes todas de FPB y delta 49 idéntico al déficit predicho antes de mirar**; y los blandos recomponiendo el objetivo sin residuo (174 + 0 + 18 = 192). De paso se detectó y eliminó una circularidad en el mapa de códigos de grupo. **C-carga-manual-1eso, tras TRES sesiones sin decidirse, se decide y se ejecuta en S120** en versión recortada por ancho: el caso §6.1 —bloque de seis destinos alternativos con subgrupos de DOS grupos, y co-docencia de LCL— tecleado A MANO por la interfaz sobre base vacía, 38 envíos, veinte minutos, horario válido en 1 s y **ningún caso inexpresable**; con él la nota de alcance del criterio 5 se ESTRECHA (queda sin demostrar por UI la escala y el descubrimiento del modelado, porque el guion decía qué construir) y O-demo se queda **SIN TRABAJO EJECUTABLE**. **O-demo sigue ABIERTO**: faltan las 11 actividades de FPB (D31-a), que dependen de un correo al centro** | Criterios 5–6 de Fase 8: "configurar centro desde cero → horario válido" (O-demo) y "crear grupo nuevo se incorpora a las particiones" (O-particiones, §3). NOTA DE ALCANCE (S115): con la carga del centro real entrando por la API REST, el criterio 5 queda demostrado por UI a escala del centro mínimo (e2e de S112) y no a escala real; ver la nota escrita en la ficha de O-demo |
| **H3 — Exportar** | Obtener PDF por grupo/profesor/aula y CSV | 0% | Los 4 criterios de Fase 9 |
| **H4 — Instalar y pasar de curso** | Instalar en Windows limpio; duplicar curso | ~10% (Fase 0 validó empaquetado una vez) | Criterios de Fases 10, 11 y 12 |

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

**Orden recomendado: H2 → cierre de H1 → H3 → H4.** Justificación en §5. (El
acabado visual transversal —O-diseño, §3— no es un hito funcional. **CORREGIDO en
S121:** este paréntesis decía «va tras cerrar H1»; la revisión de S115 en §5 lo
dejó desfasado y el M0 de S121 lo detectó como costura. El orden vigente hacia la
demo es O-demo → O-diseño → demo → O-particiones → cierre de H2, y O-diseño abrió
en S121 sin esperar a H1.)

---

## 3. Objetivos técnicos

Un objetivo se define por el AVANCE QUE PRODUCE sobre el producto, no por el
componente de código que toca. Cada objetivo tiene propósito, criterio de
terminado, dependencias, valor y los cambios que agrupa.

Se desarrollan los objetivos de H1 y H2 (los calientes). H3 y H4 se descomponen
al abrirse; su descomposición es de bajo riesgo y está acotada por los criterios
de las Fases 9–12.

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

#### O-demo — "El centro real funciona de punta a punta." (ABIERTO S115)
- **Propósito:** cargar el IES de Sevilla completo y generar su horario.
- **Terminado cuando:** el criterio 5 de Fase 8 —«configurar un centro desde cero y llegar
  a un horario válido»— pasa sobre los datos reales del IES: el centro entero está en la
  base creado por las vías legítimas del producto, el solver produce un horario válido
  sobre él y el resultado es presentable al centro. PRECISADO en S115 en dos puntos: (1) el
  criterio 6 de Fase 8 NO forma parte de este objetivo (ver O-particiones); (2) «datos
  reales» significa el IES COMPLETO —28 grupos— y no una rebanada representativa, decisión
  del arquitecto en S115 motivada por la demo al centro.
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

#### O-particiones — "Un grupo nuevo entra en el curso sin reconfigurar a mano." (ESBOZADO S115)
- **Propósito:** cumplir el criterio 6 de Fase 8, que hoy no tiene constructor: crear un
  grupo dentro del curso lo incorpora automáticamente a las particiones existentes de su
  nivel.
- **Terminado cuando:** con el centro real cargado, crear un grupo nuevo lo deja
  incorporado a las particiones de su nivel sin edición manual subgrupo a subgrupo. El
  gesto de prueba lo propuso el arquitecto en S115: cargar el centro y meter después un
  grupo que participe en particiones reales del nivel (un 1º ESO o un 4º ESO; NO un FPB,
  que no comparte partición con nadie y haría pasar la prueba sin probar nada).
- **Depende de:** O-demo (necesita un centro real con particiones densas delante). Cuidado
  de ORDEN: la prueba se hace ANTES de generar, o después de que exista un borrado de
  horario; ampliar la población de un subgrupo toca actividades que quizá ya tengan
  sesiones, y `ActividadService.exigirSinDependientes` las bloquea con 409.
- **Valor:** es el segundo de los dos criterios que cierran H2. Sin él, H2 no termina.
- **Por qué es objetivo y no un Cambio de O-demo (medido en S115):** exige materializar
  `Particion`, que NO existe por decisión explícita (D-a, S48; declarado en el javadoc de
  `Subgrupo.java:29`). Hoy `GrupoService.crear` no importa siquiera `SubgrupoRepository`, la
  relación grupo↔subgrupo vive solo del lado del subgrupo (`subgrupo_grupo`) y el grupo no
  tiene lado inverso: no hay nada que un grupo nuevo pueda heredar. Toca dominio,
  persistencia (migración de `schema.sql`), servicio, API y frontend. Y el coste real no es
  el código sino cuatro preguntas de dominio abiertas: (1) a qué subgrupo de un bloque de 6
  vías va el grupo entrante —RefMt tiene 3 vías simultáneas y elegir una al azar es tan
  malo como meterlo en las tres—; (2) qué es «la partición del nivel» cuando hay actividades
  multi-grupo que cruzan grupos y niveles; (3) qué pasa con plazas y actividades que ya
  tienen sesiones; (4) si el automatismo es reversible, cuando hoy `GrupoService.borrar`
  rechaza con 409 un grupo que esté en algún subgrupo. Meterlo dentro de O-demo habría hecho
  que H2 no cerrara hasta resolverlo y que O-demo dejara de ser lo que es.
- **Absorbe:** D1 (generación automática de subgrupos por plantilla), que O-estructura
  declaraba absorber y cerró sin construirla —correctamente, porque no estaba en el texto de
  su criterio (R-terminado)—. También la invariante de población de D31: hoy I1 no la hace
  cumplir ningún componente, y materializar `Particion` es la sede natural para decidir si
  eso cambia.

### H1 — Ajustar (cierre)

#### O-ajuste-cierre — "El ajuste manual está completo y verificado."
- **Propósito:** cerrar los HUECOS FUNCIONALES reales del ajuste, no la cobertura
  de tests.
- **Terminado cuando:** existe el gesto de despinar (hoy hueco funcional real,
  D-F8.6-ii-b) y los conflictos se ven sobre horario generado (ya cumplido).
- **Depende de:** O-shell (dónde vive la vista).
- **Valor:** cierra H1 de verdad.
- **Cambios que agrupa:** el gesto de despinar; cualquier hueco funcional que el
  guion de aceptación de H1 destape.
- **Absorbe:** la única deuda FUNCIONAL de F8.6 (D-F8.6-ii-b). El resto de la
  familia F8.6 NO entra aquí (ver §4): es cobertura o superficie de error, no
  hueco funcional.

#### O-diseño — "La aplicación tiene un aspecto cuidado y coherente." (ABIERTO en S121)
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
    - *Tramo 3 — el juicio.* **A MEDIAS**. El recorrido se hizo en S130 sobre la UI aplicada y produjo
      trabajo que su previsión no contemplaba: aplicar de verdad la jerarquía de acciones a los 34
      botones (`D-jerarquia-declarada-sin-aplicar`), el `padding` lateral de `.app__contenido`, la
      flecha nativa de `D-select-nativo-desparejo` y `D-tokens-sin-uso`. Cierra el objetivo.
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
     caja, ordinales y todo lo que no sea letra o dígito), casado por partes y contador «n de N».
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
asigna categoría, objetivo y disposición.

#### Objetivos disfrazados de deuda → se PROMUEVEN a objetivo (§3)
| Deuda | Se convierte en |
|---|---|
| D-UI-shell | O-shell |
| D-seed-demo | parte de O-demo |
| D-demo-cliente | parte de O-demo |

#### Deuda técnica real, colgada de su objetivo
| Deuda | Objetivo | ¿Bloquea? | Disposición |
|---|---|---|---|
| ~~D-insignia-sin-leyenda~~ (la insignia de coste blando se pintaba como un número desnudo con signo) **CERRADA S128** | O-diseño, en C-identidad | — | Nace en S122 al medir qué son las insignias `1`/`-1` de la rejilla (`docs/diseno-navegacion.md` §A1, §3-H-1). El `<span class="badge">` de `horario-grid.html:34` no lleva `title` ni `aria-label`, a diferencia del candado, que sí los lleva dos líneas más abajo (`horario-grid.html:40-41`). El usuario ve un `-1` en la esquina de una celda y no tiene forma de saber que significa «esta clase está tapando un hueco»: es el dato más denso de la rejilla y el único sin rótulo. Arreglo natural: el mismo par tooltip + etiqueta accesible que ya usa el candado. NO se paga en S122, que no toca `app/`. Cuidado al redactar el texto: el número es un delta CONTRAFACTUAL con signo y no tiene por qué cuadrar con `Totales` (`models/diagnostico.model.ts:58-65`), así que la leyenda no debe prometer que sea un coste absoluto. **PAGADA Y CERRADA en S128**, en su sede escrita, con el mismo par `title` + `aria-label` del candado y una leyenda que dice qué significa el signo sin prometer que cuadre con `Totales`. El argumento que decidió pagarla aquí no lo tenía la ficha de S122: desde S127 esa esquina tiene DOS números con signo —el `+N` de desbordamiento y el coste blando— con significados sin relación y sólo uno con explicación, así que dejó de ser «un dato sin rótulo» y pasó a ser ambigüedad activa creada por el Cambio anterior. Coste en altura: cero. **CERRADA** |
| D-asignatura-sin-nivel (una asignatura no sabe a qué nivel pertenece) | Sin sede | No | Nace en S122 al leer el catálogo para la maqueta. `Asignatura` es `(id, codigo, nombre_completo)` y nada más: la relación asignatura↔nivel solo se DEDUCE recorriendo Actividad→Plaza→Subgrupo→Grupo→Nivel, es decir, existe únicamente para las asignaturas que ya están usadas en alguna actividad. Consecuencia en la UI: el selector de asignatura del formulario de actividad de un grupo de 1ºESO ofrece las 100 asignaturas del centro, incluidas las de 3º y 4º, sin forma de acotarlas. Emparenta con `D-selectores-sin-busqueda` (que es de ESCALA) pero no es la misma: aquí falta el DATO con el que filtrar, no el filtro. Sin sede porque el arreglo natural toca modelo y esquema, y eso no cae en O-navegación ni en O-diseño. No se paga ahora |
| D-F8.6-ii-b (no hay gesto de despinar) | O-ajuste-cierre | SÍ | Se paga al abrir O-ajuste-cierre. Única deuda funcional de F8.6 |
| D-F8.5-D2a-a (I4 sin red) | O-catálogo | Sí, dentro de O-catálogo | Medido en S101: es de `ProfesorTutoria` (tutoría), NO del CRUD de Profesor. Su activación escrita («otra vía de escritura») NO la cumple un form que escribe por el REST existente. Se paga con el formulario de tutoría (roza O-estructura) |
| D-F8.5-E-b (unicidad profesor-tramo sin red) | O-catálogo | Sí, dentro de O-catálogo | Medido en S101: es de `ProfesorRestriccionHoraria` (disponibilidad, sub-recurso), NO del CRUD de Profesor. Se paga con el formulario de restricción horaria, no antes |
| D-F8.5-D2a-b (incoherencia 404/400 FK) | O-catálogo | No bloquea | Se evalúa dentro de O-catálogo |
| D18 (condiciones necesarias de factibilidad) | O-estructura | No | Ya cubierto en backend (8.4-A); resto en presentación |
| D-F8.6-iiiB1-c, -iiiB2a-a (superficie de error) | O-ajuste-cierre | No | Se evalúan al abrir; probablemente limitación conocida aceptable |
| D-F8.6-ii-a (el `reason` de los 400/409 no llega al navegador) | O-estructura (reasignada en S109; era O-ajuste-cierre) | No bloquea el criterio, pero DEGRADA todo lo entregado | AMPLIADA y RECLASIFICADA en S109 a técnica real TRANSVERSAL. La redacción de S81 decía que `server.error.include-message` no estaba en `application.properties`: hoy SÍ está y aun así el cuerpo llega sin `message` (medido por curl en tres endpoints, fuera de la UI). Todos los formularios pintan «Bad Request» en vez del motivo, y el 409 del PUT de actividad construido en S109 queda mudo. La causa (cambio de comportamiento en Spring Boot 4) es HIPÓTESIS no medida, y elegir el arreglo —reactivar la clave, `ProblemDetail`, o traducir en cada controlador— exige su propio M2: por eso no se pagó en S109. Hallazgo de método asociado: los tests de endpoint asertan `status().reason()`, que lee el `MockHttpServletResponse` y no el cuerpo de red — verde en test, mudo en producción. AFINADA en S110, medido en NAVEGADOR: el mensaje accionable NO se pierde —viaja como REASON PHRASE— y lo que falta es la clave `message` en el cuerpo; leer `statusText` en cliente NO es la solución (HTTP/2 no transporta reason phrases). AFINADA en S111 con un dato que su M2 debe usar como punto de partida: hay CONTRADICCIÓN DOCUMENTAL en el repo —el javadoc de `asignatura-lista.ts` afirma que `server.error.include-message=always` y el de `horario-view.ts` afirma que está DESACTIVADO—, y el comportamiento observado en navegador (el 409 de borrado de nivel pinta «Conflict» crudo) da la razón al segundo. Confirmada además en el octavo formulario: la lista de niveles nace muda. AFINADA en S112, y ESTE es el punto de partida de su M2, no el de S111: medido por lectura literal de `application.properties`, la clave `server.error.include-message=always` SÍ ESTÁ, con comentario propio que explica por qué se puso y por qué los tests no lo notan. El javadoc de `horario-view.ts` describe bien el SÍNTOMA y mal la CAUSA. La hipótesis viva pasa a ser que la clave está puesta y no surte efecto; su M2 debe EMPEZAR comprobando eso en ejecución, porque si se confirma, la opción «reactivar la clave» desaparece del abanico de tres. **COMPROBADO en S113, y con ello su M2 arranca un paso más adelante:** la clave está en `application.properties` Y en `target/classes`, sigue existiendo en la versión de Boot en uso, y aun así el cuerpo llega sin `message` (evidencia en crudo sobre `POST /api/grupos/{id}/pdc`). «Reactivar la clave» queda DESCARTADA por medición, no por hipótesis; el abanico se reduce a `ProblemDetail` o traducir en cada controlador. Superficie ampliada: los seis mensajes del flujo del PDC son genéricos, cinco «Bad Request» y un «Conflict» que pierde el desglose «referenciada por N plaza(s)». El arreglo es GLOBAL —el CRUD plano se comporta igual—, no del diálogo ni de las guardas de S113. **FALSADA EN S116 LA AFIRMACIÓN DEL REASON PHRASE, y con ella se cae el fundamento que esta ficha y la de O-demo venían usando desde S110.** Medido con `curl --http1.1 -v -i` en tres rechazos: la línea de estado llega `HTTP/1.1 400 ` con la cadena VACÍA y el cuerpo trae cuatro claves sin `message`; NINGUNA información distingue «ya existe» de «payload inválido» desde el cliente. La salvedad del HTTP/2 es irrelevante (Tomcat sirvió HTTP/1.1, donde el reason phrase sí existe, y llega vacío). SUPERFICIE AMPLIADA: tampoco llega en un 400 de mensaje conocido (`{"maxSegundos":-1}`), luego afecta a TODAS las traducciones vía `ResponseStatusException`. NO se paga en S116 por R-deuda —la prevalidación en seco garantiza que el cargador no reciba ningún 400, y la regla «cualquier no-2xx es FATAL» resuelve el Cambio activo—, pero su ALCANCE cambia de grado: hasta ahora degradaba formularios; con el 422 de generación degrada la operación central del producto delante del usuario final. Se decide junto a D-motivo-rechazo-sin-registro. **CAUSA REAL MEDIDA EN S118, y cambia el abanico:** en Spring Boot 4 la clave se llama `spring.web.error.include-message`; la que vive en `application.properties` es sintaxis de Boot 3 y está MUERTA desde la migración a 4.1. Probado en las dos direcciones (con la clave del fichero el 400 llega sin `message`; arrancando con la nueva, llega con él). Todas las mediciones previas eran correctas —la clave no surte efecto— pero ninguna preguntó POR QUÉ, y de ahí se extendió una conclusión que los hechos no sostenían: «reactivar la clave» NO estaba descartada, estaba viva bajo otro nombre y su arreglo es de UNA LÍNEA. Dos consecuencias nuevas: el comentario de `application.properties` describe un efecto que no ocurre, y `mensaje()` del frontend lee `err.error.message`, que nunca llega, así que TODOS los rechazos de pines llevan degradados desde la migración. NO se paga en S118 (cambia el cuerpo de error de toda la superficie REST y sus asertos), y S118 se hizo INMUNE a ella construyendo el cuerpo del fallo en el controlador. Cuando se pague, el primer paso ya no es un M2 de tres opciones: es probar la clave correcta y medir qué tests caen |
| D-plaza-sin-subgrupos (una plaza con cero subgrupos se acepta) | O-estructura | No | Detectada por el M2 de S109: `validarPlazas` comprueba XOR, I7 e I2, pero acepta `subgrupos` nulo o vacío y devuelve 201. Agujero de dominio (la población de la plaza SON sus subgrupos). DECISIÓN de S109: el formulario refleja el contrato y NO añade el validador solo en cliente; hay un spec que se pondría rojo si alguien lo añadiera. El arreglo es simétrico a I7 (≈10 líneas y un test). No se paga ahora |
| D-i2-dedup-cliente (la deduplicación intra-plaza del validador I2 no la cubre ningún test) | O-estructura | No | Nace en S110 de la campaña de mutación: quitar el `Set` por fila del validador `subguposDisjuntos` no pone rojo nada. El escenario es INALCANZABLE desde la UI (un `<select multiple>` no repite opción; el GET proyecta desde un `Set`), así que la regla existe por fidelidad con `validarPlazas` y no porque haya camino que la ejercite. Deuda de TEST, hermana de D-jornada-flush-test. Escribir el caso exigiría fabricar un estado que el sistema no produce. No se paga ahora |
| D-horario-irreversible (un horario generado no se puede borrar ni reemplazar) | O-estructura | No bloquea el criterio, pero es un CALLEJÓN SIN SALIDA para el usuario | Nace en S111, medida en navegador y confirmada en código. No existe `DELETE /api/horarios/{id}` ni ningún borrado programático de `sesion`; cada `POST /api/horarios` ACUMULA (alta pura, sin consulta previa ni reemplazo), y el 409 del PUT/DELETE de actividad cuenta `sesion(es)` entre sus referentes. Consecuencia: en cuanto se genera un horario, las actividades que usa quedan congeladas para editar y borrar de forma PERMANENTE por la vía UI/API; la única salida es tocar SQLite a mano. El javadoc de `ActividadService.editar` prescribe «el usuario borra el horario y luego reconfigura», salida que NO existe. El `on delete cascade` de `sesion.horario_id` ya está en el esquema: el mecanismo está preparado y nadie lo dispara. Afecta al e2e solo si éste necesitara rehacer algo tras generar: MEDIDO en S112 y NO le afecta, porque cada corrida parte de una BD borrada y genera una sola vez. REEVALUADA en S115 y BAJA de presión sin cerrarse: la carga del centro real entra por script contra la API, así que la base se rehace en minutos y la congelación deja de ser callejón sin salida. C-borrado-horario sale del camino crítico de O-demo. Vuelve a subir si algún día la carga deja de ser repetible. La estimación de S115 queda escrita por si se paga: el `on delete cascade` de `sesion.horario_id` ya está y el diálogo `confirmar-borrado` es reutilizable, pero NO existe `GET /api/horarios`, así que un botón de borrar solo alcanzaría al horario que se está viendo. EJERCITADA en S116 sin morder: la corrida de diagnóstico con 600 s generó horario y creó 770 sesiones, y la base se devolvió a `horario_generado 0` / `sesion 0` restaurando una copia previa, exactamente la salida que S115 preveía. Confirma la rebaja de presión y confirma también su precio: la salida sigue siendo rehacer, no borrar, y depende de que alguien se acuerde de copiar antes. Sigue sin pagarse |
| D-error-generacion-pin (un fallo de generación se anuncia como fallo de pin) | O-estructura | No | Nace en S111. `lanzarGeneracion` reutiliza el helper `mensaje()` escrito para los pines, cuyo degradado es «El servidor rechazó el pin (N).»; ante un horario infactible (422) el usuario lee literalmente eso. Hermana de D-F8.6-ii-a: el texto del backend, que sí nombra el recurso culpable, se pierde por configuración y no por diseño del componente, así que las dos primeras ramas del `||` fallan siempre. Arreglo trivial (un degradado propio) pero encuadrado con esa deuda. **PREDICCIÓN CORREGIDA en S116:** en el camino del 422 de generación la deuda NO se cumple, porque el cuerpo trae `"error":"Unprocessable Content"` y `mensaje()` (`horario-view.ts:267`) prefiere ese término sobre el degradado; el texto del pin solo saldría si faltaran `message` Y `error`. Lo que el arquitecto vio no fue el mensaje equivocado sino NINGÚN mensaje, y la causa es otra: D-generacion-sin-indicador. La deuda sigue viva para los cuerpos sin ninguna de las dos claves. No se paga ahora |
| D-molde-mensaje-cubierto-en-form (la precedencia de `mensaje()` en las listas de catálogo no la cubre nadie) | O-catálogo (CERRADO en S106) | No | Nace en S111 al destaparlo la mutación M6. El javadoc de `asignatura-lista.spec` y hermanas afirma que el orden interno de `mensaje()` está «cubierto en el form, misma función»: es FALSO —hay dos funciones copiadas a propósito y no compartidas, así que el caso del formulario no puede cubrir a la de la lista—. En niveles se cerró añadiendo la clave `error` al cuerpo flusheado del caso del 409, sin caso nuevo; las cuatro entidades de O-catálogo siguen con el hueco y con el comentario falso. NO se paga: R-terminado, el objetivo está cerrado. Cuando se toque una de esas listas por otro motivo, es una línea de fixture |
| D-doble-proyeccion-compartido (el doble de `getProyeccion` es un Subject compartido) | O-ajuste-cierre | No | Nace en S112. Es el ÚLTIMO doble compartido de `horario-view.spec.ts`: sus tres hermanos de escritura (`guardar`, `borrar`, `generar`) migraron a fresco por invocación en S94, y `bloqueos.listar` en S99. La forma compartida impide encadenar FALLO → RECARGA, porque un Subject cerrado por `.error()` redispara al re-suscribirse. Mordió en el caso (40), que arranca con la proyección en 404 —el escenario real de BD vacía—. Parcheado con re-stub LOCAL al caso, no homogeneizando el doble: migrarlo tocaría los 25 casos vigentes que lo consumen (R-terminado). Deja de ser aplazable con el segundo caso que necesite lo mismo. No se paga ahora |
| D-e2e-retry-bd (un reintento de Playwright correría sobre la BD del intento fallido) | O-estructura | No | Nace en S112. `retries: 2` en CI, y el `rm -f app/educhronos-e2e.db*` vive en el `command` del `webServer`, que corre UNA VEZ por corrida, no por test ni por reintento. Un reintento encontraría el centro ya creado y moriría con un 400 de código duplicado, es decir, por causa distinta de la original: esconde el diagnóstico. Hoy no bloquea porque NO HAY CI (Fase 12 sin abrir). Se resuelve al abrirla. No se paga ahora |
| D-e2e-aislamiento (la suite e2e corre en paralelo sin aislamiento entre specs) | O-estructura | No | Nace en S112. `fullyParallel: true` sin `workers` reparte los specs entre workers que atacan el mismo backend y la misma BD. Inocuo HOY por una razón concreta y no por suerte: `humo` solo lee (la landing no llama a `/api`) y `centro-minimo` es el único que escribe. El riesgo llega con el TERCER spec: dos writers sobre un SQLite único chocarán por los `unique` de código de forma no determinista, que es la clase de fallo intermitente que desprestigia una suite entera. Se decide al escribir el segundo spec que escriba. No se paga ahora |
| D-props-test-obsoleto (el `application.properties` de test afirma que `schema.sql` dropea) | O-estructura | No | Nace en S112. Dice «schema.sql dropea y recrea, de modo que varios contextos Spring sobre este mismo fichero recrean el esquema con FK sin petar»; falso desde S109. Es la MISMA falsedad que S112 corrigió en `playwright.config.ts`, cuya hermana quedó viva. Efecto de lectura, no de ejecución (la suite de backend se limpia por otra vía), pero por R5 es estado vivo equivocado: hace que el siguiente lector decida sobre una premisa falsa. Se corrige al tocar ese fichero. No se paga ahora |
| D-pdc-lista-rancia (la lista de subgrupos no se entera del alta ni del borrado de un PDC) | O-estructura | No | Nace en S113 y la abre el propio Cambio: el alta de un PDC toca DOS catálogos (crea el grupo y su subgrupo mono-Di) pero el contrato del molde —«el diálogo cierra con `true` y recarga quien lo abrió»— solo alcanza a `GrupoLista`. MEDIDO en navegador: la sección de subgrupos seguía diciendo «No hay subgrupos todavía» con el subgrupo ya en la BD; simétrico al borrar. NO se paga aquí, con razón escrita: no es del género de las guardas (vista desactualizada, no destrucción de datos), arreglarla exige coordinar componentes hermanos dentro de `Configuracion` —que ES la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio propio— y no bloquea el criterio, cosa que el M4 demuestra: el §6.2 se reprodujo entero con la lista rancia de por medio. Se resuelve en el Cambio que decida la navegación; no se parchea con un `EventEmitter` ad hoc, que fijaría el molde por la puerta de atrás. **CERRADA en S123 por C-rutas-hijas, y por construcción.** El bug quedó localizado —`grupo-lista.ts:121-128` recarga solo grupos tras el alta de PDC y nadie avisa a `SubgrupoLista`—; con destino propio, entrar en subgrupos remonta el componente y su `ngOnInit` recarga. No se escribió ningún `EventEmitter` |
| D-pdc-vinculo-por-cadena (el agregado PDC localiza su subgrupo por código derivado) | O-estructura | No | Nace en S113. `PdcService.borrar` resuelve el mono-Di con `findByCodigo(codigo + "-Completo")`: el agregado que su javadoc dice poseer no estaba protegido fuera de sus tres métodos, y cualquier rename por otra vía dejaba el DELETE del PDC en 404 permanente. G2 CONTIENE la deuda cerrando el único camino que existía (el CRUD plano de subgrupos); vuelve a morder con un tercer camino de escritura hacia `Subgrupo`. Familia de D-F8.5-D2a-a y D-F8.2b-iv-a (validación de aplicación sin espejo en la base). Convertir la convención en referencia real es cambio de ESQUEMA, no una guarda: se evalúa cuando algo más toque `schema.sql` en esta zona. No se paga ahora |
| D-tokens-inexistentes (la familia `D-nueva-*` se cita en nueve sitios y no existe) | Transversal, sin objetivo asignado | No | Nace en S113 al auditar en R4 los tokens que la sesión introducía. `D-nueva`, `D-nueva-1` … `D-nueva-5` aparecen en `GrupoService`, `GrupoDTO`, `GrupoRequest`, `GrupoEndpointTest`, `grupo-form.ts` y la cabecera de `grupo.model.ts`, y ninguno tiene definición viva en este documento ni en el plan. Incumple R4 en su forma más simple; el daño es que el lector busca el token, no lo encuentra y no sabe si la regla sigue vigente. PREEXISTENTE: S113 corrigió solo el que ella misma introdujo (`D-nueva-2`) y registró el resto, porque mapear nueve citas a sus deudas reales exige leer nueve contextos y es trabajo propio, no un arreglo en caliente. Sesión de Higiene/Método, junto con el script de R4 que falta desde S101 |
| D-log-aplicacion (no hay logging estructurado en ninguna de las dos capas) | Transversal, sin objetivo asignado | No | Propuesta del arquitecto en S111 tras el recorrido en navegador, donde diagnosticar un fallo exigió leer código en vez de logs. Backend sin configuración de logging a fichero (solo consola); frontend sin ninguna traza, con `ngx-logger` mencionado como candidato pero NO evaluado. Mejora FUTURA: se registra para que no se pierda, no planifica y no cuelga de ningún objetivo vivo. **REENCUADRADA en S116, con evidencia y no con impresión.** El arquitecto propuso una sesión dedicada a introducir logging; se argumentó en contra que instrumentar antes de diagnosticar es instrumentar a ciegas y que había instrumentos gratis (pestaña Red, stdout). Los gratis BASTARON, pero solo porque el tiempo de respuesta era observable desde fuera y permitía descartar ramas; medido en la misma sesión que el motivo del 422 no se registra en NINGUNA parte (ver D-motivo-rechazo-sin-registro), luego con un fallo cuyo síntoma externo no variase no habría habido forma de diagnosticar sin leer código. Pasa de propuesta razonable a deuda con un caso medido detrás. Sigue sin abrir sesión por R-deuda. Distinción que ordena el asunto y que conviene no perder: los logs son para el DESARROLLADOR y los mensajes en pantalla para el USUARIO; lo que hace falta delante del centro es D-F8.6-ii-a, no ésta |
| D-timeout-como-infactible (un agotamiento de tiempo del solver se comunica como «no hay horario factible») | O-demo (C-generación) | No bloquea, pero AFIRMA ALGO FALSO | Nace en S116 al diagnosticar el 422 del centro real. `SolverHorario.java:117-125` devuelve el resultado si el estado de CP-SAT es OPTIMAL o FEASIBLE y lanza `HorarioInfactibleException` en cualquier otro caso, así que UNKNOWN —se acabó el presupuesto— cae por la misma rama que INFEASIBLE. MEDIDO: 5 s → 422 en 5,7 s; 30 s (defecto, `GeneradorHorarioService.java:201`) → 422 en 31,1 s; 600 s → **200** con `estadoSolver: FEASIBLE`, objetivo 188.0. Que el tiempo escale con el presupuesto en vez de terminar antes demuestra UNKNOWN y no INFEASIBLE, porque la infactibilidad se prueba y se devuelve al detectarla. Tres piezas SEPARABLES: calibrar o exponer el presupuesto (decisión A MEDIR, no un número a subir a ojo), distinguir UNKNOWN de INFEASIBLE (el arreglo de esta deuda), y que el motivo llegue al cliente (D-F8.6-ii-a). Es el M2 de C-generación. **AFINADA en S117: de RAZONADA a MEDIDA.** El barrido leyó el estado directamente de la excepción en 27 rechazos: **27 UNKNOWN, CERO INFEASIBLE**. El catálogo no se demostró imposible ni una vez. Superficie ampliada: con el defecto de 30 s la tasa de éxito es 0/4 y a 60 s es 1/9, luego la afirmación falsa es el camino NORMAL en producción, no un caso raro. La pieza (1) deja de ser «a medir» y pasa a ser decisión tomable. No se paga en S117 (la sesión no escribió producto). **PAGADA Y CERRADA EN S118, exactamente como preveía: DE PASO, por caer en el camino de fallo del Cambio.** El estado de CP-SAT pasa a ser CAMPO de la excepción y `mapear` lo traduce a tres desenlaces: INFEASIBLE → 422 CATALOGO_INFACTIBLE, UNKNOWN → 503 con `Retry-After: 0` y causa PRESUPUESTO_AGOTADO, el resto → 500. Verificado en ejecución sobre HTTP real: 30 s → 503 en 31,15 s con `estado: UNKNOWN` y `segundos: 30` en el cuerpo. La pieza (3) se resolvió SIN pagar D-F8.6-ii-a, construyendo el cuerpo del fallo en el controlador. **CERRADA** |
| D-motivo-rechazo-sin-registro (el motivo de un rechazo no se escribe ni en el log) | Transversal; se decide con D-F8.6-ii-a y D-log-aplicacion | No | Nace en S116. Hermana de D-F8.6-ii-a y peor que ella: el mensaje se construye correctamente en `SolverHorario.java:124`, con el estado de CP-SAT dentro, y se pierde ENTERO —no en el cuerpo, no en la línea de estado y NO en el stdout—. Medido: durante la petición que devuelve 422, `grep -inE "horario|solver|infactib|INFEASIBLE|cp-sat|ortools|422"` sobre el log completo da CERO coincidencias. Consecuencia: diagnosticar el fallo central del producto exigió deducirlo por el TIEMPO DE RESPUESTA desde fuera, único canal observable, y leer código. Es la evidencia que reencuadra D-log-aplicacion. El arreglo mínimo (un `log.warn` con el estado antes de lanzar) es barato y NO sustituye a ninguna de las dos deudas mayores. **MATIZ de S117:** se planteó pagarla como instrumento del barrido y se descartó POR MEDICIÓN, no por regla —el mensaje lleva el estado dentro y un arnés en proceso lo recibe como excepción, no como cuerpo HTTP—. Muerde al USUARIO, no al desarrollador que instrumenta desde dentro de la JVM. No se paga ahora |
| D-generacion-sin-indicador (durante la generación la pantalla no cambia) | O-diseño, o el Cambio que toque la vista de horario | No | Nace en S116. El POST tarda por diseño lo que dure el presupuesto del solver —y sobre el centro real ese presupuesto se AGOTA siempre— y en todo ese tiempo no hay spinner, ni estado «generando», ni botón deshabilitado (`horario-view.html:23` solo lo deshabilita si no se ha prevalidado); el `<p>Cargando…</p>` de `horario-view.html:61` es el estado por defecto de la rejilla, no un indicador. Medido en código; la parte razonada y NO medida es que ésta explica por qué el arquitecto no vio mensaje alguno al generar (miró antes de que llegara la respuesta). Es acabado de interacción, pero muerde EN LA DEMO. **AGRAVADA en S117 y ya no son 30 s:** el presupuesto defendible está en CIENTOS de segundos (600 s es el único punto sin fallos observados), luego la pantalla inmóvil dura diez minutos. Deja de ser acabado y pasa a ser condición práctica de la cláusula «presentable al centro» del criterio de O-demo. Sigue sin bloquear —el horario válido se produce— y sin abrir sesión, pero su arreglo cae en el mismo camino de fallo que el cierre de C-generación debe tocar. **PAGADA Y CERRADA EN S118, de paso.** Señal `generando` que cierra el botón (añadiendo `|| generando()` sin sustituir la condición de prevalidación), texto de estado visible y apagado en las DOS ramas; cuatro tests de vitest incluido el de rehabilitación tras error. Marcado mínimo y cero estilo por R-invalidación (O-diseño rehará el aspecto, no el estado). VERIFICADO EN NAVEGADOR durante diez minutos reales. Deja tras de sí D-presupuesto-anunciado-espejo. **CERRADA** |
| D-generacion-no-reproducible (dos generaciones idénticas dan resultados distintos, y a veces ninguno) | O-demo (C-generación) | No bloquea, pero hace IMPREDECIBLE la operación central | Nace en S117 del barrido de presupuesto. `SolverHorario` fija `setMaxTimeInSeconds` y `setRandomSeed` y NO fija el paralelismo: el grep de `num_search_workers`, `setNumSearchWorkers` y `MaxDeterministicTime` sobre `solver/src/main` y `app/src/main` sale VACÍO. CP-SAT usa todos los núcleos y corta por RELOJ DE PARED, así que la semilla no da reproducibilidad. EVIDENCIA: 600 s y semilla 42 dan objetivo 188.0 en S116 y 208.0 en S117; a 45 s, 1 éxito de 3; a 60 s, 1 de 9. Efecto de secuencia dentro de una JVM DESCARTADO (0 de 3 siendo las primeras corridas de un proceso nuevo). Consecuencia de producto: regenerar da otro horario sin haber cambiado nada, y con presupuesto corto puede dar horario una vez y 422 la siguiente. Consecuencia de método: toda medición sobre el solver necesita n>1; una corrida única no es una medida. Tres caminos separables (fijar workers, cortar por tiempo determinista, o dimensionar el presupuesto para tasa de éxito alta) y no se elige aquí. **AFINADA en S118 con la evidencia más limpia hasta la fecha:** dos corridas CONSECUTIVAS sobre la misma base y el mismo presupuesto de 600 s dieron UNKNOWN (curl, 17:38) y FEASIBLE (UI, 17:47, objetivo 192.0); la fallida corrió con la JVM al 687 % de CPU, así que no fue estrangulada. Consecuencia: «600 s es el único punto sin fallos observados» deja de ser cierta y la tasa acumulada a 600 s pasa a **3/4**. Segunda consecuencia, que es argumento de diseño: si el mismo botón unas veces da horario y otras no, el 503 reintentable de S118 no es un borde sino la mitad del comportamiento normal. Y un argumento MÁS para no tocarla a la ligera: fijar `num_search_workers` invalidaría la base empírica con que se calibró el defecto de 600 s. No se paga ahora |
| D-prevalidacion-ciega-a-holgura-cero (la prevalidación da vía libre ante el caso más difícil) | O-demo (C-generación) | No | Nace en S117. Sobre el catálogo real la prevalidación devuelve **ERROR=0 y AVISO=0**, y sin embargo 26 de los 28 grupos deben llenar sus 30 tramos EXACTOS. La causa es de diseño: `GRUPO_SOBRECARGADO` exige demanda MAYOR que los tramos disponibles, y 30 sobre 30 no lo supera. Distingue «imposible» de «posible», pero no «cómodo» de «al límite», que es la distinción que gobierna si habrá solución en el presupuesto dado. Se registra por su efecto COMBINADO: vía libre → generar → sin señal durante la espera (D-generacion-sin-indicador) → «no hay horario factible» que es falso (D-timeout-como-infactible). El arreglo natural es un AVISO cuando la holgura de un grupo es cero, y su valor depende de que alguien lo lea: se decide junto con la señal de espera. **AFINADA en S118 por dos vías.** (1) FOTOGRAFIADA: la generación que el arquitecto lanzó desde el navegador mostraba «Catálogo sano: sin hallazgos de pre-validación» sobre el centro donde 26 de 28 grupos van a 30/30; deja de ser dato de arnés y pasa a ser lo que el usuario lee. (2) SUPERFICIE AMPLIADA: sobre el catálogo VACÍO la prevalidación también devuelve `[]` y solo lo caza `ModeloCpSat` al construir el modelo, luego no es ciega solo a la holgura cero, es ciega al centro recién instalado. S118 resolvió el DESENLACE de ese caso (422 CONFIGURACION_INCOMPLETA en vez de un 500 accidental), no el aviso. La mitad del «efecto combinado» que la ficha registraba se ha deshecho: sus dos hermanas están CERRADAS. **FOTOGRAFIADA A LA INVERSA en S129, y esta ficha resultó ser además el DISPARADOR que otra deuda daba por desconocido.** El M4 de S129 necesitaba provocar un hallazgo, y S121 había registrado en `D-prevalidacion-contraste-sin-ver` que «nadie sabe qué lo dispara»: la respuesta llevaba escrita aquí desde S117 —`GRUPO_SOBRECARGADO` salta cuando la demanda supera los tramos—. Al meter una actividad de más, el panel reportó **`1B-A 37 / 30`**, luego el grupo estaba EXACTAMENTE en 30 y la actividad sumó 7: es evidencia directa y fotografiada de lo que esta ficha afirmaba por conteo. Lección de método asociada: una ficha de deuda es documentación consultable, no sólo un registro de lo pendiente. No se paga ahora |
| D-presupuesto-anunciado-espejo (la espera anunciada en pantalla es un espejo manual del presupuesto real) | O-demo | No | Nace en S118 como precio honesto de haber hecho el presupuesto configurable. El texto del estado de espera dice «puede tardar hasta 10 minutos» y esos minutos salen de una constante `MINUTOS_ANUNCIADOS` del componente, no del backend: no existe ningún endpoint que publique `educhronos.solver.max-segundos`. Basta arrancar con `--educhronos.solver.max-segundos=N` para que la pantalla mienta EN SILENCIO, sin que nada falle ni ningún test se ponga rojo; medido en el propio M4, donde la corrida de 30 s anunciaba diez minutos. Documentado en el código como COTA ANUNCIADA y no como promesa. El arreglo —exponer el presupuesto por API y que la vista lo lea— es superficie NUEVA, no un ajuste. No se paga ahora |
| D-guion-exit-enmascarado (un guion anunció como éxito un BUILD FAILURE) | Transversal, con el script de R4 pendiente desde S101 | No | Nace en S117 y la corrigió Claude Code dentro de la sesión: la plantilla capturaba `EXIT=$?` después de un `echo`, midiendo el código de salida del `echo` y no el del comando. El daño no fue más allá porque la salida se leyó entera, pero la plantilla viene de sesiones anteriores y nadie ha auditado cuáles la usaron. Misma familia que el hallazgo de S109 (los tests de endpoint asertaban sobre `MockHttpServletResponse` y no sobre el cuerpo de red): un instrumento que mide otra cosa distinta de la que se cree. → sesión de Higiene/Método, junto al script de R4, al que añade un caso concreto. No se paga ahora  **CUARTO Y QUINTO CASO en S121, los dos detectados por Claude Code y no por el asistente que escribió los guiones.** (4) Un comprobador de «el `@import` del CDK sigue siendo la primera regla» usaba `^\s*[@.:a-zA-Z*]`, que casa con las líneas de continuación de un comentario `/* */`: daba verde sin medir nada. (5) El recuento de cierre de C-sustitución contaba `src/**/*.css` incluido `styles.css`, donde los literales DEBEN vivir, así que su objetivo declarado («0 al terminar») era inalcanzable por construcción. Se añade el caso más peligroso de la familia, señalado por Claude Code y no sufrido: **`var(--token-inexistente)` NO rompe el build** —la declaración se descarta en el navegador—, así que un build en verde no prueba que los tokens referenciados existan. De ahí sale el comprobador de cuatro vías que S121 deja vivo para el resto de O-diseño (literales, `font-size` sin `var()`, `var(` mal formado, y tokens referenciados contra los definidos en `styles.css`, quitando comentarios antes de buscar). **SEGUNDA INSTANCIA, hallada en S123 durante el propio M1 que el script certifica:** `scripts/verificar-cierre.py:171-173` imprime las entradas de índice descuadradas pero NO las suma a `problemas`, y el `return` de la 178 solo mira esa variable; el script salió con 0 teniendo 30 entradas rotas entre los dos documentos. Quien encadene `verificar-cierre.py && commit` se lo traga. Detectada leyendo el fuente del verificador, no confiando en su código de salida. NO se corrige en S123 (R-deuda: no bloquea el criterio de O-navegación) y la corrección no es de dos líneas: subir §4 a fallo duro exige comprobar antes que los documentos pasan ese listón, y eso es cambio de método. → misma sesión de Higiene/Método. **TERCERA instancia en S124, y por el reverso:** no un EXIT que enmascara un fallo, sino guardas que miden lo que no toca. Un volcado salió VACÍO con EXIT=0 por suponer marcadores `INDICE:INICIO/FIN` en `diseno-navegacion.md`, que no los tiene; y otro imprimió números de línea RELATIVOS al fragmento tubado, que leídos como absolutos habrían editado la ficha equivocada. La guarda de vacío añadida después sí disparó, y disparó bien. Lección: la guarda mide el vacío, no la corrección. **SIETE INSTANCIAS MÁS en S129 —de la (6) a la (12)—, todas del arquitecto y todas con la MISMA causa raíz, que aquí se nombra por primera vez: patrones y rangos escritos DE MEMORIA teniendo a tres secciones de distancia el fichero que los define.** (6) `grep -rn -- '--e[1-6]' --include='*.css'`: el `--` desactivó el parseo de opciones, `grep` devolvió 2, `pipefail` lo propagó y el `||` imprimió «cero usos» **justo debajo de los quince usos que acababa de listar** —variante peor de la familia: no enmascara un fallo, publica una conclusión falsa—. (7) Un `echo` de conclusión escrito ANTES de medir, y falso. (8) Buscar `--(peso|linea)` cuando los tokens se llaman `--lh-*`: la sección calló el interlineado sin decir que no lo había buscado. (9) Un patrón con el orden invertido respecto a como se escribe TypeScript, con siete «cero coincidencias» falsos. (10) Buscar `title=` literal, perdiendo `[title]` y `[attr.aria-label]`, justo donde vivía el molde buscado. (11) Una regex que casaba también el comentario de la tabla y **borró cinco líneas de más en las siete plantillas**, cazada por Claude Code en el diff y no por la suite. (12) **REINCIDENCIA:** el `grep` sin coincidencias bajo `pipefail` volvió a matar el proceso dos guiones después de haberlo diagnosticado. **Segundo corolario operativo:** un guion de lectura no busca por nombres recordados; los deriva de lo que él mismo acaba de volcar. Y de (12): una lección escrita no basta si el siguiente guion se escribe sin releerla. **CUARTA Y QUINTA INSTANCIA en S128, las dos del arquitecto y las dos en guiones de LECTURA, que es variante nueva.** (4) Un volcado de inventario pedía `sed -n '40,80p' ruta-inexistente || find …`: el `sed` falló, **el `||` salvó la salida** y el volcado se vio bien, así que una ruta falsa sobrevivió tres turnos hasta reventar en un `git add`. (5) La guarda `grep -rl "EstadoLista" --include=*-lista.ts | wc -l` casa con `estado-lista.ts`: el componente se contaba a sí mismo y el «1» que devolvió significaba CERO listas cableadas. Corolario operativo nuevo: **fallback silencioso en un guion de lectura, nunca**. Sigue viva **S130 SUMA SIETE INSTANCIAS EN UNA SOLA SESIÓN, seis del arquitecto**, y el dato que importa no es el siete: es que la séptima fue un `||` de rescate escrito en un guion de lectura DESPUÉS de haber contado cinco en la misma sesión, contra el corolario que esa misma sesión llevaba citando desde el M0. Las otras seis: `^Results:` contra un Maven que prefija `[INFO]`; `^| $D` contra filas tachadas; un patrón de espaciado que ignoraba la exclusión que la propia ficha declara; un `sed` que recortó la lista prometida justo antes del `ABORTA` que explicaba el vacío; un delimitador `^### ` sobre una entrada anterior sin cabecera, que volcó 376 líneas bajo un rótulo que prometía una; y `S129` escrito de memoria contra un documento que dice «Sesión 129». Sigue sin bloquear el objetivo activo: R-deuda aguanta por décima vez |
| D-resize-observer-jsdom (el `ResizeObserver` de la rejilla lanza `ReferenceError` en cada fixture, en silencio) | Transversal, con la sesión de Higiene/Método | No | Nace en S127, destapada al leer la sección `stderr` de una corrida CON FALLOS: jsdom no define `ResizeObserver`, así que el callback de `afterNextRender` de `horario-grid.ts` revienta en TODOS los fixtures de la rejilla. Es PREEXISTENTE —viene de S126, `git show` lo sitúa en la línea 167 del fichero de entonces— y es invisible en verde, porque vitest sólo imprime `stderr` cuando algo falla. Daño real y acotado: `repartirAltura()` nunca corre en tests, luego `altoCelda` nunca se fija y **el disparador por `altoCelda` del `afterRenderEffect` de D11 está muerto en jsdom**. Los casos (28) y (29) pasan por el OTRO disparador, `celdas()`, así que de los dos caminos que el cableado declara la suite sólo ejercita uno; el que compensa que el `ResizeObserver` corra fuera del ciclo de render lo verificó M4 y nada más. Arreglo natural: un doble de `ResizeObserver` en el setup de tests, que además haría comprobable ese segundo camino. NO se paga en S127 por R-terminado —no bloquea el criterio 4, y su verificación la aporta M4—, y tocar el setup global de tests dentro de un Cambio sin cerrar añade riesgo a cambio de nada que el objetivo pida. Es de la familia de `D-guion-exit-enmascarado`: un instrumento que no mide lo que se cree que mide |
| D-pin-ocupada-no-persiste (el mismo gesto de arrastre persiste el pin sobre celda vacía y no sobre celda ocupada) | O-ajuste-cierre | No | Nace en S127 de una DISCREPANCIA entre las dos pasadas de su M4, y se registra sin diagnóstico porque no lo hay. Primera pasada, arrastre sobre celda OCUPADA: la interfaz mostró «1 pines sin aplicar — regenerar» y la base no tenía ni una fila nueva —`sesion_bloqueada` y `aula_bloqueada` a 0, y la comparación tabla por tabla contra el original no encontró ninguna diferencia de contenido en las 21 tablas; el md5 sí cambió, pero sólo por el `change_counter` de la cabecera SQLite, 883 → 885, dos transacciones que no escribieron ninguna fila—. Segunda pasada, arrastre sobre celda VACÍA en 1FPB: el aviso **sobrevive al F5 y al cambio de grupo**, luego ahí sí está persistido en el servidor. Las dos explicaciones plausibles —que el primer drop no llegara a completarse, o que ocupada y vacía se comporten distinto— NO se han medido, y elegir una sería inventar. Lo establecido son los dos hechos. Es comportamiento del ajuste manual, no de la navegación, así que su sede es O-ajuste-cierre y no O-navegación (R-terminado). Quien la pague empieza por reproducir la primera pasada con la pestaña Red abierta |
| D-vista-horario-sin-horario (la vista de horario recibe a un centro recién configurado con dos mensajes de error) | O-demo | No bloquea, pero MUERDE EN LA DEMO | Nace en S120, medida en navegador sobre una base recién poblada a mano por la interfaz: antes de generar nada la vista pinta «No se pudo cargar el diagnóstico.» y «No se pudo cargar el horario 1 (404).» junto al mensaje correcto de prevalidación. El 404 es la respuesta CORRECTA del backend —no hay horario todavía— y lo que está mal es que el cliente trate «aún no hay horario» como fallo en vez de como estado inicial. Cae de lleno en la cláusula «presentable al centro» del criterio de O-demo: es lo primero que ve un usuario que acaba de configurar su centro, y por tanto lo primero que vería el jefe de estudios en la demo. Hermana del hallazgo de S118 sobre D-prevalidacion-ciega-a-holgura-cero, que tampoco distingue el centro recién instalado. Arreglo natural: distinguir 404 de error y pintar estado vacío. Es COMPORTAMIENTO y no aspecto, así que O-diseño no lo cubre (mismo criterio que S118 aplicó al estado de espera). No se paga en S120: no bloquea el criterio y la sesión no escribió producto. **DECIDIDO EXPRESAMENTE en el M0 de S121, como pedía el encuadre de S120: queda FUERA del criterio de O-diseño** y sigue colgando de O-demo, por el corte comportamiento/aspecto que la propia ficha ya establecía. Se registra el incómodo que eso deja: O-demo está bloqueado por un correo, así que si nadie la mueve la demo se hace con dos mensajes de error en la primera pantalla. Queda anotada como CANDIDATA A CAMBIO CORTO DE O-DEMO antes de la demo, y se hace constar que su clasificación «No bloquea» es discutible en lectura estricta del texto del criterio («presentable al centro»); reclasificarla es decisión del arquitecto y en S121 no se reclasifica |
| D-selectores-sin-busqueda (los selectores de entidades son multiselect nativo sin buscador ni filtro) | O-diseño, o el objetivo que rehaga el formulario de actividad | No | Nace en S120 al construir a mano la actividad de seis plazas: poner dos subgrupos en una plaza se hace con ctrl+clic sobre una lista plana, igual que profesores y aulas. Con los 13 subgrupos del ejercicio funciona sin fricción y el arquitecto lo resolvió sin dudar; el problema es de ESCALA y está cuantificado: el centro real tiene 334 subgrupos, 59 profesores y 43 aulas. No es hueco funcional —todo se puede construir— y por eso NO abrió un C-hueco-*. Mejora futura; no se paga (R-terminado: no cambia el criterio de O-demo) |
| D-actividad-forma-implicita (la forma de una actividad se deduce en vez de declararse) | O-diseño | No | Nace en S120 de dos observaciones del mismo formulario que son la misma cosa. (1) La opción «— varias (una por plaza) —» del selector de asignatura se ENCONTRÓ pero resultó CONFUSA, y es lo que distingue un bloque de destinos alternativos de una actividad ordinaria. (2) La co-docencia no se declara: se obtiene poniendo dos profesores en una plaza y nada nombra el concepto. La propuesta del arquitecto (un check explícito que, apagado, limite el selector a un profesor) queda registrada como una opción entre varias, no como diseño decidido. Absorbe la tercera observación del mismo paso: con seis plazas rellenas la legibilidad baja a «regular». Mejora futura; no se paga |
| D-arranque-no-literal (la orden de arranque contra otra base no está escrita, y lo que se escribió sobre ella falló al ejecutarse) | Transversal, con el script de R4 pendiente desde S101 | No | Nace en S120 con dos hechos medidos. (1) El literal exacto de la invocación con ruta absoluta no existe en el repo: el plan la DESCRIBE y no la CITA, así que S117, S118, S119 y S120 la han reconstruido cada una por su cuenta y en S120 esa reconstrucción costó un intento fallido. (2) El plan describe que un segundo argumento se añade dentro del mismo `-Dspring-boot.run.arguments=` separado por comas, y al teclearlo así la coma NO separó nada: la base se creó con el nombre literal `educhronos-s120-manual.db,--educhronos.solver.max-segundos=60`. NO se afirma que el plan describa mal el mecanismo —no se ha investigado si la sintaxis exige otro escapado— y suponerlo sería inventar; lo establecido es que la forma tecleada no funciona y cuál sí. Tercer caso concreto para la sesión de Higiene/Método, junto a D-guion-exit-enmascarado y D-tokens-inexistentes: el literal que funciona se escribe, no se describe. No se paga ahora  **TERCER HECHO en S121, y de la misma familia aunque no sea de arranque:** el asistente pidió los `git add` del cierre dando por hecho un árbol sin commitear que había reconstruido de un turno anterior en vez de leerlo, y produjo dos commits cuyos mensajes no describían su contenido (uno duplicaba palabra por palabra el asunto de otro anterior). Nada se perdió y se corrigió con `reword` antes de pushear, pero la lección es idéntica: **el estado se lee, no se reconstruye.** **CUARTO HECHO en S129, y el primero que PAGA el corolario sin cerrar la deuda:** el grep confirmó que sigue sin existir ningún `.sh` ni bloque ejecutable con la orden y que la única cita del plan conserva un `<ruta …>` en el hueco, así que la reconstrucción se hizo por quinta sesión consecutiva. Esta vez la orden se PROBÓ y se ESCRIBIÓ literal en la entrada de S129, con su prerrequisito (`mvn -pl solver install -DskipTests`). Escribir el literal no cierra la deuda —su arreglo es un `.sh` versionado, superficie nueva fuera del alcance del tramo— pero deja de obligar a la sexta reconstrucción **INSTANCIA NUEVA en S130, y es la propia deuda mordiéndose la cola.** La orden literal que esta deuda obligó a escribir levanta el backend SIN frontend desde un árbol recién limpiado: los cuatro plugins que construyen y copian el bundle (`install-node-and-npm`, `npm-ci`, `npm-run-build`, `copiar-frontend-a-static`) cuelgan de `prepare-package`, y `spring-boot:run` para en `test-compile`, así que `target/classes/static/` no existe. En S129 funcionó porque el directorio estaba poblado por casualidad de un empaquetado anterior de esa misma sesión: se escribió el literal que funcionaba ese día, no el que funciona. Corregida a TRES pasos y probada en S130, con el `mvn -pl app package -DskipTests` intermedio; el literal vive en la entrada de S130 del plan |
| D-sin-puntos-de-ruptura (no hay un solo `@media` en todo el frontend) | O-diseño, como mejora futura | No | Nace en el M2 de S121 al inventariar la superficie visual: CERO media queries en `src`, luego la aplicación no es responsive por CSS en absoluto. Se DEJA FUERA del criterio de O-diseño con argumento, no por olvido: añadir puntos de ruptura duplica el trabajo del objetivo y no sirve al producto real —bundle de escritorio, jefe de estudios en portátil, rejilla 6×5 y tablas de configuración sin diseño móvil pensado—. El criterio pide en su lugar que las vistas no se rompan en UNA resolución declarada, la de la demo. Si algún día hay uso en tableta, esta es la sede |
| D-controles-nativos-sin-neutralizar (antes `D-select-nativo-desparejo`: ningún control con adorno nativo del navegador se ha neutralizado) | O-diseño, C-revisión (tramo 3) | No | Nace en el M4 de S121 sobre los `<select>`. **CORREGIDA en S129** (su texto decía que «nadie los estila» y era falso: llevan la clase de su formulario) y **MEDIDA POR FIN en el M4 de S130**, que era lo único que le quedaba vivo: un `<select>` con la clase de un `<input>` SÍ sigue pintando su fondo gris y su flecha nativa —visto en «Editar aula» y «Editar actividad», en la misma columna que `<input>` blancos—, luego alcanza a los **13** y no a los 2. El censo de S129 decía 14 porque contaba una mención dentro de un comentario HTML (`tutoria-dialogo.html:33`): son 14 coincidencias de `<select` y 13 elementos, y queda escrito para que el siguiente que lo mida no vuelva a tropezar. **AMPLIADA en S130 y por eso RENOMBRADA**: los cinco `input[type=number]` enseñan sus flechas de contador, y hay dos `radio` y dos `checkbox` en la misma situación. Lo común NO es la clase que falta —todos la llevan— sino que el proyecto nunca ha neutralizado el adorno nativo, y eso no se arregla con `border-radius`: exige `appearance: none` y dibujar el adorno. El lado de RADIO quedó cerrado por el tramo 2. Sede: **C-revisión, TRAMO 3** |
| D-horario-id-a-fuego (la barra y la landing enlazan `/horario/1` con el id literal) | O-demo | No | Nace en S128 al leer `app.html:5` y `landing.html:8` para reestilar la barra. Dos consecuencias, y la segunda no se había visto: en cuanto se genera un segundo horario el enlace sigue apuntando al primero, y **sobre `/horario/2` el `routerLinkActive` tampoco marca «Horario»** porque el prefijo no casa, así que el id a fuego apaga además el indicador de sección que el criterio 1 de O-navegación acaba de verificar. NO se arregla en S128 por razón de CONTRATO y no de alcance: `D-horario-irreversible` ya midió que no existe `GET /api/horarios`, luego el cliente no tiene forma de saber cuál es el horario vigente. Cualquier arreglo empieza por ese endpoint. Se paga en O-demo, que es donde se bautizan y se listan horarios de verdad |
| ~~D-contador-se-apaga-con-error~~ (el contador de la cabecera desaparecía ante un error de borrado, con la tabla llena debajo) **CERRADA S129** | O-diseño, C-revisión (tramo 1) | — | Nace en el M4 de S128 sobre Asignaturas: con el 409 de borrado en pantalla y las filas cargadas, el rótulo pierde su número. La causa es `[mostrarContador]="!cargando() && !error()"`, que viene de S124 y que C-identidad NO tocó: **es la misma mentira que S124 revirtió en la tabla, viva en el contador**, y hermana de `D-vacio-miente-con-error`, cerrada en la misma sesión por el mismo argumento aplicado a otro elemento. No se paga aquí porque el arreglo exige distinguir error de CARGA de error de ACCIÓN, y eso cambia el contrato de `app-cabecera-lista` (dos señales en vez de una), fuera del alcance declarado de C-identidad. Se paga en C-revisión. **PAGADA Y CERRADA en S129, en su sede, y con un arreglo MÁS BARATO que el que esta ficha proponía:** no hacen falta dos señales, basta cambiar el CRITERIO a `!cargando() && <entidad>().length > 0`. De las dos opciones se eligió cambiar las siete plantillas y no mover la decisión dentro de `cabecera-lista`, porque lo que está mal es el criterio y no dónde vive —la alternativa habría derogado un javadoc deliberado para arreglar otra cosa, que es lo que S128 declaró fuera de alcance—. La mitad no obvia: la condición va sobre la lista CARGADA y NO sobre `visibles()`, porque con la filtrada una búsqueda sin resultados escondería el contador en vez de decir «0 de N». Esa mitad la destapó la campaña de mutación —el mutante `visibles()` SOBREVIVIÓ, y la supervivencia se predijo antes de correrla— y se cerró con un aserto en el caso (8). **CERRADA** |
| ~~D-desbordamiento-sin-etiqueta~~ (las dos marcas condensadas de la rejilla llevaban `title` y ninguna etiqueta accesible) **NACE Y CIERRA EN S129** | O-diseño, C-revisión (tramo 1) | — | Destapada por el censo de S129 al buscar el molde de `D-insignia-sin-leyenda` para reutilizarlo: la marca `+N` de plazas ocultas —el mecanismo que S127 construyó precisamente para que esas plazas dejaran de ser mudas— vivía a quince líneas de la insignia que S128 sí dotó del par completo, y un lector de pantalla sólo oía «más N». Ampliada por decisión del arquitecto a `.grupos`, mismo defecto en el mismo fichero: dejar la mitad arreglada obligaba a volver. El `aria-label` REPITE el `title` literalmente y no mejora su redacción —las cadenas son de S127 y de D6, y cambiarlas habría sido afirmar algo nuevo sobre lo que devuelven `marcaOcultas`, `detalleInstancia` y `marcaGrupos`—. Cubierta por el caso (30) de `horario-grid.spec.ts`, que asevera la IGUALDAD con el `title` y no un literal, con no-nulo previo porque borrar los dos atributos dejaría `null === null` en verde. Coste en altura: cero. **CERRADA** |
| D-tokens-sin-uso (tres tokens de `styles.css` no los usa nadie) | O-diseño, C-revisión (tramo 3) | No | Nace en el M2 de S129 al recorrer los 46 tokens de `:root` uno a uno: `--radio-s`, `--fuente-datos`, `--color-ok-fondo` y `--color-info-fondo` no tienen un solo consumidor. NO se retiran, y la razón es de método: borrar es un cambio sin criterio detrás, y dos de ellos completan parejas de la tabla de la decisión 1 —`ok` tiene la tinta viva y el fondo muerto, `info` sólo tiene fondo—. Se marcan en el fichero como «sin uso hoy», que es estado vivo correcto (R5). El caso que hay que decidir con cuidado es `--fuente-datos`: `.cuenta` de `panel-prevalidacion` es su candidato natural, y estrenarla ahí EN SOLITARIO dejaría una familia tipográfica que aparece una sola vez en toda la aplicación, peor que no usarla. La decisión es binaria y no se toma de paso: o se usa donde toca —todos los códigos y horas— o se retira. `--radio-s` se decide junto con `D-select-nativo-desparejo`, cuyo censo lo destapó. No se paga ahora **ACTUALIZADA en S130: baja de cuatro tokens a TRES.** `--radio-s` deja de estar sin uso: el tramo 2 lo redefine de 3 a 4 px y le da 21 consumidores, que es la decisión binaria resuelta por el lado de usarlo y no por el de retirarlo. Quedan `--fuente-datos`, `--color-ok-fondo` y `--color-info-fondo`, verificados sin un solo `var()` en todo el CSS. El caso delicado sigue siendo `--fuente-datos`, por la misma razón de siempre. Sede: **C-revisión, TRAMO 3** |
| D-748-sin-derivacion (el número que gobernó una pregunta abierta durante tres sesiones nunca se escribió con su derivación) | Transversal, con la sesión de Higiene/Método | No | Nace en S128 al intentar cerrar los «32 px sin explicar» de S126. La causa candidata está medida —`.app__contenido` declara `padding: 1rem 0`, 32 px exactos, en el contenedor de la vista que ni S126 ni S127 miraron— y coincide al píxel, pero **no hay contra qué contrastarla**: el plan dice «los 748 que el arquitecto había calculado» y en ningún documento del repo consta cómo se calcularon; reconstruirlos desde el viewport no cuadra. Misma lección que `D-arranque-no-literal` por el lado del número: lo que se describe y no se cita, no se puede verificar después. Arreglo de una línea —escribir la fórmula al lado del número la próxima vez que se calcule un presupuesto—. Los 32 px NO se reclaman (R-terminado: subiría `--alto-celda` ~4,5 px y cambiaría el recorte de un criterio cumplido) |
| D-declarado-sin-artefacto (un documento afirma haber declarado algo que no está escrito en ninguna parte) | Transversal, con la sesión de Higiene/Método | No | Nace en S130 al buscar el precedente de declaración de redondeos de S128. La regla escrita en `styles.css` exige que un redondeo se DECLARE; el único precedente dice DOS VECES que la cumplió —esta ficha y `plan_trabajo_horarios.md`— y no existe el artefacto: ninguna lista en `styles.css`, ninguna en los documentos, y ningún commit del repo tiene cuerpo, así que el mensaje de commit tampoco era sede posible. El coste NO es teórico: al reconstruirlos desde `cd6b43f..94b97df` resultaron ser CUATRO y no tres, un dato falso que sobrevivió dos sesiones porque nadie podía contarlo. Tercera de la familia junto a `D-arranque-no-literal` (el literal que se describe y no se cita) y `D-748-sin-derivacion` (el número sin su derivación). Arreglo de una línea de método: «se DECLARA» significa que existe una lista enumerada con fichero, propiedad, literal y delta, no una frase que diga que se declaró |
| D-jerarquia-declarada-sin-aplicar (`styles.css` declara la jerarquía de acciones como aplicada, y no lo está) | O-diseño, C-revisión (tramo 3) | **SÍ**, bloquea el criterio 3 de O-diseño | Nace en el M4 de S130. `styles.css:59-63`, dentro de la decisión 1, dice «JERARQUÍA DE ACCIONES, por relleno y no por color (aplicado, se declara)». Medido: en toda la aplicación hay **un solo** `background: var(--color-acento)` y es la barra; ningún botón lleva relleno de acento; `__cancelar` y `__guardar` comparten una sola regla en los siete formularios; los siete `__borrar` de fila llevan sólo `color`, sin superficie ni borde; y en `pdc-dialogo` principal y destructiva son idénticas. Censo de botones: 10 reglas con caja y radio, 9 con una sola propiedad, 8 sin ninguna regla —los siete `__editar` de fila y `.cabecera-lista__nuevo` salen con el botón por defecto del navegador—. NO es diseño nuevo: los tres tokens existen y la regla está escrita desde S129; falta ejecutarla. Arrastra dos consecuencias: el criterio 3 no puede darse por cumplido mientras el fichero afirme «aplicado» sobre algo falso, y la pregunta del verde que S129 zanjó con «ya se resuelve por relleno» no está resuelta sino cerrada. Es la familia de `D-declarado-sin-artefacto`, salvo que aquí lo que falta no es el registro sino el producto |
| D-avisos-como-bloque-fijo (los avisos del horario ocupan una banda fija en vez de plegarse tras un indicador de estado) | Transversal, sin objetivo asignado; candidata a O-particiones | No | Nace en el M4 de S130, propuesta del arquitecto con argumento propio: sustituir la banda de texto por un botón con icono de estado —verde, aviso, error— que abra el detalle en un panel aparte liberaría alto para la rejilla, que es el presupuesto medido del criterio 4 de O-navegación. Queda FUERA de O-diseño por R-terminado y R-invalidación a la vez: cambia la INTERACCIÓN y no el acabado, y O-particiones toca frontend después. Se registra con su argumento para que no se pierda |
| ~~D-prevalidacion-contraste-sin-ver~~ (aviso y error del panel se distinguían SOLO por color de texto, y no se habían visto juntos) **CERRADA S129** | O-diseño, C-revisión (tramo 1) | — | Nace en el M4 de S121 por un riesgo que la sesión NO pudo cerrar. C-sustitución colapsó cuatro ámbares (`#c80`, `#a60`, `#b8860b`, `#8a6100`) en `--color-aviso` (#8A5A00) y tres rojos (`#b00`, `#b00020`, `#c33`) en `--color-error` (#A32014), y en `panel-prevalidacion` los mensajes de severidad se distinguen solo por `color:`, sin fondo. Para verlos juntos hay que provocar un hallazgo, y ni la documentación del proyecto ni el asistente saben qué lo dispara: se registró como NO SABIDO en vez de mandar a probar a ciegas. Se verifica en C-revisión leyendo el componente antes. Si se confunden, el arreglo es un cambio de valor en `:root` y se propaga solo, que es justo lo que C-tokens compró. **CERRADA en S129 POR CONSTRUCCIÓN, con dos correcciones a esta ficha.** (1) **La verificación que pedía no era difícil: es IMPOSIBLE.** Medido en el backend: `Severidad` tiene dos valores y las tres reglas de `PrevalidacionService` emiten `ERROR`; **nadie emite `AVISO`**, ni en `main` ni en los tests, y el propio enum documenta que se conserva por contrato y como candidato del palomar de aulas. Los dos colores no coexisten, así que no se pueden ver juntos. (2) **Tenía DOS caras y la ficha sólo veía una:** además de las filas, los dos contadores de la cabecera eran números desnudos y consecutivos distinguidos sólo por la tinta, y **medido con la fórmula WCAG los dos tokens tienen 1,28:1 ENTRE SÍ** cuando a un elemento no textual portador de información se le piden 3:1; en el estado colapsado —el habitual— eran lo único en pantalla, luego el color no era redundante, era la información. El arreglo NO fue el cambio de valor en `:root` que esta ficha preveía: rótulo VISIBLE junto a cada número, severidad pintada como TEXTO en cada fila con el valor crudo del enum, y marco del panel tomando la severidad máxima con contrastes medidos antes de escribirlos (6,35:1 / 4,97:1 / 13,16:1 sobre `--color-error-fondo`). Al dejar el color de ser el único canal, la verificación imposible deja de ser condición para cerrarla: mismo mecanismo que mató a `D-vacio-miente-con-error` en S128. **CERRADA** |

#### Mejora futura, cuelga y espera
| Deuda(s) | Objetivo | Nota |
|---|---|---|
| D-nombre-horario-instante (el horario se llama `"Horario " + Instant.now()`, 38 caracteres con nanosegundos) | O-demo | Nace en S126 al fundir el título con los controles (D9): el nombre que el backend pone por defecto (`GeneradorHorarioService.java:187-188`, porque el POST del frontend va con cuerpo vacío) no cabe en una fila junto a dos selectores y un botón, y además no es un nombre sino una marca de tiempo. CONTENIDA en la pintura: `horario/titulo.ts` compone el rótulo desde `fechaGeneracion` y cae al nombre crudo si la fecha no parsea. El arreglo REAL es mandar un nombre legible en el POST, que `HorarioController.java:64` ya acepta, y no se hace porque es una escritura nueva y el criterio 5 de O-navegación las excluye. Se paga en O-demo, que es donde se bautizan horarios de verdad |
| D-jornada-zona-servidor (las horas de la jornada dependen de la zona del proceso del servidor) | O-demo | MEDIDA en S126 contra el backend real: `tramo_semanal` guarda 25200000 ms (7:00) y `GET /api/jornada` devuelve `08:00`. El resultado es correcto en este equipo y su corrección viene de cómo el driver lee el entero, luego depende del despliegue: en UTC la jornada entera se mostraría una hora antes. Es la razón por la que D8 sigue sin pintar la hora en la fila de recreo, y NO es la razón que `diseno-navegacion.md` §4-D8 escribió: allí el hueco era «no sabemos si la conversión es correcta»; aquí es «lo es, pero por el despliegue». No la introduce S126 y no la puede arreglar el frontend, que pinta la cadena que le dan. Se decide donde se despliega para el centro |
| ~~D-vacio-miente-con-error~~ (cuando fallaba la CARGA de una lista convivían el mensaje de error y «No hay X todavía», que era falso) **CERRADA S128** | C-revisión → pagada antes, en C-identidad | Nace en S124 al medir las dos caras del encadenado de ramas. S124 probó encadenar `error()` con el resto y lo REVIRTIÓ: encadenado, un 409 de borrado hacía desaparecer la tabla entera (medido: 8 filas → 0), y eso es un camino de uso normal que S113 introdujo a propósito, mientras que el fallo de carga es raro. El defecto es PREEXISTENTE, no lo introdujo S124. Arreglo ya escrito: condicionar la rama del vacío a `x().length === 0 && !error()`. No se paga (R-terminado: no cambia el criterio 3); su sede era C-revisión, que va a repasar esta UI y donde un mensaje que miente es exactamente lo que toca mirar. **CERRADA POR CONSTRUCCIÓN en S128**, antes de su sede y sin abrir sesión: al fundir las cuatro ramas de estado de las siete listas en `app-estado-lista`, la precedencia pasa a existir en UN sitio, igual que `D-pdc-lista-rancia` murió en S123 sin que nadie escribiera un `EventEmitter`. Sólo el VACÍO se condiciona al error —encadenar el error con la tabla es lo que S124 revirtió— y la precedencia vive en un `computed` con un `'ninguno'` explícito y no en un `@else if`, porque escrito como rama el caso caía al hermano `coincidencias === 0` y el vacío dejaba de mentir para que mintiera «Ningún resultado para «»». Verificada en navegador. **CERRADA** |
| D-F8.6 de cobertura (iiiB1-a, ivB-a-bis, ivD-a, ivA-a, ivA-c, ivB-b, ivB-c, iiiA-b, B-a) | O-ajuste-cierre | Cobertura de la vista de horario. La mayoría se RECLASIFICA a limitación conocida en cuanto O-shell reubique la vista (su contexto de test cambiará). NO se pagan ahora |
| D-F8.4-A-a, -A-b, -A-c, -B1-a | O-ajuste-cierre | Cobertura de prevalidación |
| D-S101-num (numeración global de tests colisionada) | O-ajuste-cierre | Detectada S101: la secuencia (N) de la capa componentes/servicios tiene colisiones preexistentes —(27),(28-30),(35-37) con contenidos distintos en dos ficheros— que rompen la atribución por (N) en campañas de mutación. Es superficie de specs de H1 (cerrado). Los specs de O-catálogo la esquivan abriendo secuencia propia por fichero. Arreglarla no bloquea nada (R-terminado): no se paga ahora |
| D-F8.5-D2b2-a, -D2b2-b (diseño/cosmética) | — | Sin objetivo urgente |
| D-F8.5-C3-a, -C3-b, -C2a-a | O-catálogo | Semántica/dominio de catálogo, a resolver con datos. C3-a CONTENIDA en UI desde S102 (COMUN fuera del selector del form de Aula); sigue viva a nivel de esquema. C3-b: los códigos por currículo (Mat/LCL usados en specs de S103 son reales de este catálogo) siguen sin UI para poblar compatibilidades (ver D-S103-compat) |
| D-S103-compat (CRUD de asignatura no alcanza `aulas-compatibles`) | Cambio de compatibilidad (tras Grupo, o dentro de O-estructura) | Detectada S103: el backend expone `GET/PUT /{id}/aulas-compatibles` pero el CRUD plano no lo alcanza. NO bloquea O-catálogo (semántica S75: 0 filas ⇒ irrestricta; un centro mínimo corre sin poblar compatibilidades). Incluye decidir la no-atomicidad POST→PUT. Estirar el molde con el sub-recurso es Cambio propio. No se paga ahora |
| D-jornada-msg409 (mensaje del 409 dice «No se puede borrar» al guardar jornada) | O-estructura | Detectada S107: `ReferenciaEntranteException` se escribió para los DELETE de catálogo; su mensaje se reutiliza en el PUT de jornada y el usuario lee «No se puede borrar: referenciada por…» cuando intenta GUARDAR. Cosmético, NO bloquea (el desglose «N sesiones, M restricciones… antes de reconfigurar» sí es correcto). Se corrige con un mensaje propio del caso PUT cuando O-estructura vuelva a tocar el backend de jornada; no se paga ahora (R-terminado, M3 cerrado). ACTUALIZADA S109: baja de coste sin cerrarse — la fase 1 de S109 parametrizó el verbo de `ReferenciaEntranteException` (ctor de un argumento delega en «borrar», ctor de dos toma el verbo), así que corregir jornada pasa a ser cambiar de ctor en `JornadaService`, que sigue usando el de un argumento |
| D-jornada-asimetria (contrato GET(35)≠PUT(7 día tipo)) | O-estructura | Detectada S107, consecuencia consciente de «el backend expande»: el GET devuelve la malla completa, el PUT acepta un día tipo. Nota de diseño de API, no deuda bloqueante: la UI convive sin fricción real (pinta un día, manda un día). Reconsiderar `diaTipo` en el GET solo si un futuro cliente lo pide |
| D-jornada-flush-test (`put_dosVecesLaMismaMalla_idempotente` no discrimina el flush) | O-estructura | Detectada S107: falta `UNIQUE(dia,orden)` en `schema.sql`, así que sin el `flush()` el resultado sería el mismo y el test no lo prueba. El `flush()` es defensivo/preventivo (correcto: fuerza DELETE antes de INSERT). Si algún día se añade la constraint, el test pasa a discriminar. Deuda de test, no de código |
| D-actividad-ux (asperezas del editor de Actividad) | O-estructura (o O-diseño si absorbe el acabado) | Detectada S109 al conducir el formulario con Playwright, tres asperezas de presentación: los dos `<select formControlName="asignatura"` del formulario (la de la actividad y la de la plaza) no tienen `id` ni `label for` y se anuncian igual a un lector de pantalla; el error de servidor viejo sigue pintado mientras se muestra un error de campo nuevo (`error()` solo se limpia al empezar una petición); y el aviso de multiplaza vive dentro de la celda del recuento, mezclando dato y aviso. Ninguna impide configurar nada. RECORTADA en S110: el tercer síntoma se CERRÓ de paso al retirar el trozo B la guarda de multiplaza y con ella el aviso; sobreviven los dos primeros. No se paga ahora |
| D-subgrupo-ux-multiselect (el campo «grupos» del form de subgrupo es un `<select multiple>` nativo) | O-estructura (o O-diseño si absorbe el acabado) | Detectada S108, DECISIÓN CONSCIENTE de alcance: se eligió la mínima desviación del molde (`<select multiple>` nativo) y la UX rica —chips, búsqueda, casillas— se aplaza a una fase de mejora de UX de subgrupos ya prevista al abrir el Cambio. NO es deuda técnica (el componente funciona, valida I6 en cliente, 12 tests) ni bloquea el criterio de O-estructura (la población se elige, solo sin comodidad). El `.subgrupo-form__multiple` y el handler `alSeleccionar` son el punto de sustitución. No se paga ahora |
| D-pdc-sin-edicion (el sub-recurso PDC no tiene PUT ni PATCH) | O-estructura | Medido en S113: un PDC no se renombra, se borra y se recrea, y si su subgrupo está retenido por una plaza el borrado da 409. El diálogo REFLEJA el contrato y no ofrece «Editar»: exponer un botón sin backend detrás sería que la UI mintiera (mismo criterio que D-F8.5-E-a con `peso`). No bloquea: el §6.2 se reproduce sin renombrar nada y el código lo escribe el usuario en el alta (D1-3, S76). Si se paga, va junto con D-pdc-vinculo-por-cadena: un rename recalcularía mal el código derivado del subgrupo |
| D-pdc-sufijo-completo (`-Completo` significa lo contrario en el backend y en el modelo) | O-estructura | Medido en S113. El backend deriva el subgrupo del PDC como `codigo + "-Completo"` con población SOLO el PDC (regla S23); en el cuerpo de §6.2 del modelo «3ºA-Completo» es el subgrupo que enlaza el ordinario Y su Di. Dos convenciones incompatibles en un espacio de códigos único. No urgente (el cuerpo de §6.2 está marcado como SUPERADO y la Nota S23 no usa el sufijo), pero es estado vivo confuso (R5). Probablemente se salde con una línea en el modelo, no renombrando la derivación |
| D-monodi-botones-inertes (el subgrupo mono-Di ofrece Editar y Borrar que siempre fallan) | O-estructura (o O-diseño) | DECISIÓN CONSCIENTE de S113: no se ocultan. Hacerlo exigiría que `SubgrupoDTO` transportara el tipo de los grupos de su población —mover el contrato por comodidad de pintura— y el precio de no hacerlo es acotado, porque con G2 los botones fallan en vez de destruir, que era el problema real. Si se paga, con la información en el DTO y no adivinando por el sufijo del código, que es el acoplamiento que lamenta D-pdc-vinculo-por-cadena |
| D-s8-muda (el resalte de una violación S8 no dice qué falta) | O-ajuste-cierre | Nace en S114, medida en navegador. La `descripcion` del `ViolacionDTO` llega al cliente con el texto exacto («Actividad MAT-1ESOA requiere tutor, pero ningún profesor suyo es TUTOR_PRINCIPAL…») y NO se pinta en ningún sitio: la rejilla solo dibuja un filete rojo de 2px (`horario-grid.css:122`), sin texto, tooltip ni lista. Medido: con la violación activa, el texto de la página no nombra `TUTORIA_SIN_TUTOR` ni la palabra «tutor». Es PEOR que un mensaje genérico por una razón propia de S8: como es la única regla cuyo origen es el CATÁLOGO y no la colocación, el resalte cae sobre celdas perfectamente colocadas y arrastrarlas NO lo quita nunca, así que el usuario intentará moverlas indefinidamente. Es superficie de la vista de horario (familia 8.6/H1), no de O-estructura: mismo criterio con que S113 dejó fuera D1-8 y D1-10. Familia de D-F8.6-ii-a. No se paga ahora |
| D-diagnostico-no-es-foto (el diagnóstico de un horario recalcula contra el catálogo vivo) | O-ajuste-cierre | Nace en S114 al no poder medirse el paso 9 del M4 como estaba planteado. `DiagnosticoService` verifica contra el catálogo ACTUAL, y `verificarTutorias` no mira la solución (`VerificadorSolucion.java:56`), así que para S8 el diagnóstico responde «¿esto sería válido AHORA?» y no «¿lo era al generar?». Consecuencia medida: el horario #1, generado con la violación real, se presenta hoy impecable; y el #2, generado limpio, se pinta en rojo si alguien quita el tutor después. Afecta solo a S8 (las demás reglas sí leen la solución). No bloquea nada hoy y el registro histórico no existe como requisito en ningún criterio. Se decide al abrir la vista de diagnóstico, junto con D-s8-muda. No se paga ahora. **AFINADA A LA BAJA en S119, que la usó como INSTRUMENTO:** la capa 1 del contraste se midió invocando `DiagnosticoService` (vía `GET /api/horarios/{id}/diagnostico`) contra el horario de S118, y aquí la deuda resultó VACUA —la base m4 no se ha tocado desde el instante de generar, así que el catálogo vivo ES el del momento del solve—. Queda escrito para que nadie lea aquel «cero violaciones» como si la deuda no existiera: sobre una base que sí hubiera cambiado entre generar y diagnosticar, el mismo verde respondería a otra pregunta. Consecuencia práctica y barata para cualquier medición futura: si se diagnostica un horario histórico, hay que decir sobre qué estado del catálogo se hizo. Sigue sin pagarse |
| D-post-horario-sin-sesiones (el POST de generación devuelve la proyección sin sesiones) | O-ajuste-cierre | Nace en S114, medido en red. El horario se persiste bien (3 filas en `sesion`) y el `GET /{id}/proyeccion` las devuelve, pero el cuerpo del POST llega con `sesiones: []`. Inocuo HOY por una razón concreta y no por suerte: la UI recarga por `paramMap` y nunca lee el cuerpo del POST (decisión de S93, recargar por GET fresco). Muerde a cualquier cliente futuro que se fíe de la respuesta —incluido un e2e que quisiera asertar sobre ella—. **CONFIRMADA A ESCALA REAL en S116:** el POST sobre el centro completo devolvió `sesiones: []` con 770 filas en `sesion`. La ficha lo predecía desde S114; deja de ser hipótesis. No se paga ahora |
| D-tutor-pdc-desincronizado (la herencia del tutor al PDC corre solo en el alta) | O-estructura (cerrado) | Nace en S114. `PdcService.heredarTutorPrincipal` se invoca únicamente desde el alta (`PdcService.java:110`): si después se cambia el tutor del padre con el PUT, el PDC conserva el antiguo en silencio. No es un bug del código actual —nadie prometió resincronización— pero C-tutores lo hace VISIBLE por primera vez: se verán dos grupos emparentados con tutores distintos y nada explicará por qué. No bloqueaba el criterio (el §6 no exige reasignar tutores) y por eso el objetivo cierra con ella viva. Arreglarla es decisión de dominio, no una guarda —¿copia o referencia?—, familia de D-pdc-vinculo-por-cadena. **MUERDE en O-demo, confirmado en S115:** el centro real tiene cinco PDC y el orden natural de carga (grupos → PDC → tutores) los dejaría a todos sin tutor, porque `heredarTutorPrincipal` corre solo en el alta y nada resincroniza después. CONTENIDA sin pagarla, invirtiendo el orden de carga —los tutores de los padres se asignan antes de crear sus PDC— o asignando el tutor a cada PDC por el sub-recurso, que acepta cualquier grupo. **S116 la CONTIENE por otra vía y mide que aquí era vacía:** el cargador asigna las 28 tutorías DESPUÉS de crear los PDC, porque el PUT del sub-recurso es reemplazo total idempotente y acepta grupos PDC; y los 5 PDC del catálogo tienen el MISMO tutor que su padre, luego no había nada que sobreescribir. Verificado además que la herencia solo ocurre si el padre ya tiene tutor en el instante del alta. La deuda sigue viva: el arreglo real es decidir copia o referencia. No se paga ahora |
| D-dialogo-foco-perdido (al salir del estado «cargando» el foco cae fuera del diálogo) | O-diseño | Nace en S114, medida en los tres diálogos. El CDK enfoca el botón de la rama `cargando`; cuando el `@switch` cambia de rama ese elemento se destruye y el foco cae a `<body>`, fuera del diálogo. `GrupoForm` (sin estados) conserva el foco dentro; `PdcDialogo` y `TutoriaDialogo` no. NO la introduce C-tutores: `PdcDialogo` hace lo mismo desde S113. Para teclado y lector de pantalla el diálogo queda abierto sin foco dentro. Arrastra una consecuencia de andamio: la barrera `:focus` con que `centro-minimo.spec.ts` evita la carrera del portal no sirve en diálogos con estados, así que si algún e2e futuro abre uno de estos dos habrá que sustituirla por una espera al contenido. Es acabado de interacción, transversal a las vistas: cuelga de O-diseño. No se paga ahora |
| D-bundle-presupuesto (el bundle inicial excede el techo declarado) | O-diseño | Preexistente desde antes de S112 (507,66 kB frente a 500 kB en `angular.json`, verificado sobre HEAD limpio); S113 lo lleva a 514,42 kB al entrar `PdcDialogo` en el grafo de dependencias. NO se toca `angular.json`: subir el techo es configuración de build, no está en el criterio de ningún objetivo vivo, y hacerlo «de paso» convierte un aviso útil en un número que nadie vuelve a mirar. Cuelga de O-diseño, que tendrá delante el bundle completo y las vistas congeladas y podrá elegir entre subir el techo, rutas perezosas o recortar. Hasta entonces, anotar el delta en cada sesión que compile. **SALDADA EN S121 dentro de C-tokens, de paso y no por sesión propia.** No bloqueaba (el `maximumError` está en 1 MB y ningún trabajo de color puede romper el build), pero O-diseño era su sede designada y caía en el camino. Se midió que el aviso YA estaba encendido antes de tocar nada —521,03 kB contra 500—, así que no era señal sino ruido permanente: un aviso que nadie vuelve a mirar es exactamente lo que la ficha temía al negarse a subir el techo en S113. El pago es subir `maximumWarning` a 550 kB dejando el error en 1 MB, con lo que un aviso nuevo vuelve a significar algo. Delta de la sesión: 521,03 → 524,94 kB (+2,06 kB por C-tokens y la tanda 1, +0,77 kB por la tanda 2; el crecimiento son los nombres de token, más largos que los hex, y el comprimido incluso BAJA de 118,11 a 118,01 kB porque los `var(--color-*)` repetidos comprimen mejor). Margen restante: ~25 kB. **CERRADA** |
| D-gh6-tutor-contradictorio (el modelo se contradice sobre de qué grupo es tutor GH6) | O-demo | No | Nace en S115 al derivar las tutorías del centro real. `modelo_datos_fase1.md` §6.1 registra `ProfesorTutoria(GH6, 1ºESO A)` y el Hallazgo E del mismo documento dice que GH6 es tutor de 1º Bach A; las dos derivaciones son correctas contra los volcados (GH6 imparte tutoría en ambos grupos) y la contradicción es del TEXTO del modelo, no de los datos. No viola I4, que acota los principales por grupo y no los grupos por profesor. Claude Code hizo bien en no resolverla: fijar uno de los dos por criterio propio sería inventar un dato del centro. Se cierra con la respuesta del jefe de estudios sobre los tutores reales, en la misma consulta que la ambigüedad A5 de `ESPECIFICACION-CATALOGO.md`. Hermana de D31: deuda de REQUISITOS |
| D-configuracion-monolitica (la pantalla de configuración es un solo scroll con ocho listas) | O-diseño, o el Cambio que decida la navegación | No | Nace en S115 del recorrido de la UI contra el tamaño del centro real: 59 profesores, 100 asignaturas, 43 aulas, 334 subgrupos y 219 actividades en un único componente, sin pestañas, sin filtro y sin búsqueda. ACOTADA en la misma sesión y por eso NO abre Cambio: la carga es append-only, así que localizar filas solo duele al corregir; y el riesgo grave que se le atribuyó al proponerla —el clic sin Ctrl del `<select multiple>` de subgrupos, que reemplaza la población entera— es INALCANZABLE con estos datos, porque los 334 subgrupos derivados son todos mono-grupo. Se resuelve donde ya espera D-pdc-lista-rancia: en el Cambio que decida ruta-hija-vs-contenedor, aplazado desde S101. No se paga ahora. **CERRADA en S123 por C-rutas-hijas.** Ocho destinos con URL propia bajo `/configuracion`, índice vertical derivado de las rutas, la página sin scroll y cada lista desplazándose dentro de su panel con la cabecera clavada. Queda para C-listas-filtradas lo que esta deuda también nombraba —el filtro y la búsqueda—, que es el criterio 3 |
| D5, D6, D9, D11, D16, D17, D21, D27, D29 | Fase 5/8 según su asignación en el plan | Deuda de solver/dominio ya asignada; se reevalúa al abrir su objetivo |

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

#### Limitación conocida → sale de la cola, se documenta el "no se hará"
| Deuda | Razón |
|---|---|
| El horario del centro real no cabe sin scroll en el portátil del jefe de estudios (S123) | MEDIDO: panel 1920×1080 con escala de Windows al 150 %, viewport CSS de 1280×585. `scripts/calcular-recortes.py` da 87,73 px de alto de fila y **109 celdas recortadas de 791 (13,8 %)** frente a las 22 (2,8 %) del escenario de criterio; alcanzan a toda celda de tres plazas o más. Las tres palancas de S122 juntas no lo salvan: faltan 10,27 px. DECISIÓN CONSCIENTE de S123: el portátil sale del criterio 4, que se verifica sobre el sobremesa donde se enseña la demo. REABRE si el uso diario pasa a ese equipo, o si se fija su escala al 100 % —que lo resolvería entero, a cambio de texto del sistema a ~11-12 px físicos— |
| D-F8.5-E-c | "de FRAMEWORK": el dialecto de comunidad no clasifica los fallos; depende de Hibernate |
| D-F8.5-E-a | `peso` es superficie muerta en tres capas; no se activa hasta que el solver lo lea |
| D2, D3, D8, D12, D22(parcial) | Simplificaciones de Fase 1 condicionadas a datos reales o a fases futuras concretas |
| D25 (contención de CPU en -Pescala) | No bloquea la suite rápida; se aborda solo antes de usar -Pescala como gate de CI (Fase 12) |

#### Deuda de MÉTODO → se integra en `metodo.md`, no en el producto
| Deuda | Destino |
|---|---|
| D-F8.0-a | Ya cerrada (motivó escribir el método) |
| D-F8.6-a (aviso de oportunidad de mockup) | Integrada en M-mockup de `metodo.md` |
| D-F8.6-ivA-b, -ivB-b (de método y cobertura) | Integradas en M3 de `metodo.md` |

#### Deuda ya CERRADA (histórico, no pendiente)
D-F8.6-ivD-b (S99), D-F8.4-B2-a (S94), D-F8.5-D1-a (S77), D-F8.5-D2b1-a/b (S91),
D-F8.5-A-a (S73/S74), D-F8.6-iiiA-a (S85), D-cabecera-lista-duplicada (S124), y las condensadas en la sección de
deuda cerrada del plan. Se conservan como registro con remisión a la bitácora.

**Resultado agregado:** de las ~54 deudas vivas (S113 añade siete, S114 cinco y S115 dos), tras
reclasificar, **1 es bloqueante ahora** (D-F8.6-ii-b, y solo al abrir
O-ajuste-cierre), ~11 son deuda técnica real que se paga DENTRO de su objetivo
cuando llegue, y el resto (~35) sale de la cola de trabajo activo como mejora
futura que espera, decisión consciente o limitación conocida. La cola de "deudas
que me obligan a abrir sesión" sigue en ~1. Nota sobre la tendencia, visible
desde S109: la cola CRECE sesión a sesión y eso no es alarma por sí solo —el
crecimiento es casi todo de mejora futura y de deuda registrada al medir, no de
deuda técnica real acumulándose sin pagarse—; la métrica que sí hay que vigilar
es "deuda bloqueante abierta" (§7), que lleva en 1 desde que existe el mapa. Confirmado en S115: de las dos deudas que podían morder por primera vez en O-demo, una BAJA de presión (D-horario-irreversible, por la carga repetible) y la otra se CONTIENE con el orden de carga (D-tutor-pdc-desincronizado); ninguna abre sesión. RATIFICADO en S116 con las dos ejercitadas de verdad y tres deudas nuevas nacidas (D-timeout-como-infactible, D-motivo-rechazo-sin-registro, D-generacion-sin-indicador): ninguna bloquea el criterio del objetivo activo y ninguna abre sesión. La sesión tensó R-deuda en su caso más difícil —el hecho en que se apoyaba la ficha de D-F8.6-ii-a resultó FALSO— y la regla aguantó porque la conclusión se sostenía por otro argumento, medido: la prevalidación en seco garantiza que el cargador no reciba ningún 400. Cambiar el fundamento de una deuda no es lo mismo que pagarla. RATIFICADO otra vez en S117, con dos deudas nuevas más (D-generacion-no-reproducible, D-prevalidacion-ciega-a-holgura-cero) y una de método (D-guion-exit-enmascarado): ninguna bloquea el criterio del objetivo activo. La sesión tensó R-deuda por el lado contrario al de S116 —aquí la tentación era PAGAR D-motivo-rechazo-sin-registro como instrumento de la medición— y la regla aguantó porque la medición demostró que el instrumento no hacía falta: el estado de CP-SAT viaja dentro del mensaje de la excepción y un arnés en proceso lo lee. Se registra además el caso inverso y honesto: si el cierre de C-generación toca `GeneradorHorarioService:201`, distinguir UNKNOWN de INFEASIBLE cae en el mismo camino de fallo y la deuda se cubre DE PASO. Encontrarse una deuda haciendo el trabajo del Cambio no es abrir una sesión para ella. RATIFICADO en S118, y es la sesión donde ese corolario se COBRA: dos deudas —D-timeout-como-infactible y D-generacion-sin-indicador— quedan CERRADAS sin que ninguna abriera sesión, pagadas de paso porque caían en el camino de fallo que C-generación tenía que tocar. La regla se tensó además por un lado nuevo: la sesión midió que el arreglo de D-F8.6-ii-a es de UNA LÍNEA (la clave de Boot 4) y aun así NO se pagó, porque cambia el cuerpo de error de toda la superficie REST y no bloquea el criterio; barato no es lo mismo que en alcance. Deuda bloqueante abierta: sigue en 1 (las aulas de FPB, D31-a). RATIFICADO en S119 y en S120 sin novedad, y la de S120 por el lado más fácil de todos: nacen cuatro tokens —D-vista-horario-sin-horario, D-selectores-sin-busqueda, D-actividad-forma-implicita, D-arranque-no-literal— y ninguno se paga, porque la sesión no tocó ningún camino de fallo y por tanto tampoco hubo nada que cubrir DE PASO. Lo que S120 sí tensó fue R-apertura contra R-deuda en la elección de sesión: el script de R4 tenía su mejor caso hasta la fecha (S119 midió que la verificación manual YA falló una vez) y perdió igualmente, porque no bloquea el criterio del objetivo activo y el Cambio competidor sí podía nombrar los tres términos del mapa. **RATIFICADO en S121 con el caso más interesante hasta la fecha, porque por primera vez se COBRA una deuda dentro del objetivo que la tenía asignada.** D-bundle-presupuesto se salda en C-tokens sin abrir sesión y sin bloquear: cae en el camino del Cambio, O-diseño era su sede escrita, y el hecho nuevo que justifica el pago es que el aviso ya estaba encendido antes de tocar nada, luego el techo no era una red de seguridad sino ruido. Al mismo tiempo la regla aguantó por el otro lado: siete deudas colgadas de O-diseño —las seis de UX más D-dialogo-foco-perdido— se dejan expresamente FUERA de su criterio en el M0, para que el objetivo de acabado no se convierta en rehacer la UI. Deuda bloqueante abierta: sigue en 1 (D31-a). Nacen tres deudas (D-sin-puntos-de-ruptura, D-select-nativo-desparejo, D-prevalidacion-contraste-sin-ver), ninguna bloquea, y dos de ellas tienen sede dentro del propio criterio, en C-revisión.
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

**[LAGUNA]** Este documento asigna categoría, objetivo y disposición a cada
deuda. NO reescribe el texto íntegro de cada una: ese vive en
`plan_trabajo_horarios.md`, que sigue siendo su fuente. Si al abrir un objetivo
una deuda concreta necesita re-lectura, se lee del plan.

---

## 5. Revisión del roadmap: por qué H2 va primero

El roadmap original ejecutó la Fase 8 en orden de DEPENDENCIA TÉCNICA DEL BACKEND
(8.1 vía REST → 8.2 solver de pines → 8.3 diagnóstico → 8.6 vista → tests de la
vista). Ese orden es impecable desde el código y contraproducente desde el
producto: construyó toda la maquinaria de AJUSTAR (H1) antes de tocar la de CREAR
(H2), cuando crear es el prerequisito de valor. Resultado medido: ~43 sesiones en
Fase 8, H1 al 90%, H2 al 0%.

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

**Contraargumento honesto:** H1 está al 90% y terminarlo da sensación de cierre.
Pero terminar H1 antes de H2 es terminar la mitad que no se puede usar. La
sensación de progreso es la trampa que tiende el orden por dependencias. Por eso
H1 se marca explícitamente "EN PAUSA al 90%, suficiente" y se salta a H2.

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
