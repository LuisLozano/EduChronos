# Sistema de gestión del proyecto — Educhronos

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
| **H2 — Configurar un centro desde cero** | Crear profesores, aulas, grupos, currículo, desdobles, PDC, tutores por formularios y llegar a un horario válido sin tocar la BD | ~70% (O-shell hecho S100; O-catálogo TERMINADO S104, criterio precisado S106: 4 de 4 entidades CRUD por UI — Profesor (S101), Aula (S102), Asignatura (S103), Grupo (S104). El e2e UI→solver, antes 2ª mitad de O-catálogo, se reasignó a O-estructura en S106 al medirse que depende de currículo/jornada. O-estructura ABIERTO S107, 7 piezas hechas: C-jornada (S107, backend REST `/api/jornada` + formulario singleton, dimensión temporal del solve), C-subgrupos (S108, CRUD de subgrupos por UI sobre `/api/subgrupos`, con multiselect de grupos), C-actividades COMPLETO (trozo A en S109 —editor de Actividad de una plaza + guarda 409 del PUT + fin del vaciado de la BD en cada arranque— y trozo B en S110 —lista de plazas variable con alta/baja e I2 en cliente, con lo que desdobles, agrupamientos y bloques de optativas quedan construibles por UI—), C-niveles (S111, CRUD de Nivel por UI: cerraba el hueco medido en S109 —sin niveles por UI no hay grupos ni subgrupos— y con él las nueve filas del centro mínimo son construibles por pantalla) y C-e2e (S112, el e2e de navegador que crea el centro mínimo por la UI y verifica que el solver produce horario: TERCERA PATA del criterio, CUMPLIDA; incluyó arreglar un hueco funcional real de la primera generación) y C-pdc (S113, alta/consulta/borrado del grupo PDC por UI desde la fila de su padre + dos guardas de backend que impiden que el CRUD plano deshaga el agregado: con él el caso §6.2 del modelo —en su versión válida, la Nota (S23)— se construye íntegramente por pantalla y el solver produce horario sobre él, que es la SEGUNDA PATA demostrada en su caso más difícil) y C-tutores (S114, la asignación del tutor por UI sobre el sub-recurso que existía desde S77: su M0 midió que SÍ hacía falta —tres casos del §6 registran `ProfesorTutoria` en su configuración— y su M4 verificó en navegador que `TUTORIA_SIN_TUTOR` aparece sin tutor y desaparece con él; con ella las TRES PATAS quedan cumplidas y O-estructura CIERRA). «Desdobles y agrupamientos» dejó de ser trabajo propio al medirse que son actividades multiplaza. **O-estructura ✔ TERMINADO S114**, 8 piezas; queda SOLO O-demo entre H2 y su cierre) | Criterios 5–6 de Fase 8: "configurar centro desde cero → horario válido" y "crear grupo nuevo se incorpora a las particiones" |
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
acabado visual transversal —O-diseño, §3— va tras cerrar H1, sobre vistas ya
congeladas; no es un hito funcional.)

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

#### O-demo — "El centro real funciona de punta a punta."
- **Propósito:** cargar el IES de Sevilla por la UI y generar su horario.
- **Terminado cuando:** el guion de aceptación de H2 pasa sobre datos reales.
- **Depende de:** O-estructura. **DESBLOQUEADO desde S114** (O-estructura ✔ TERMINADO):
  es el ÚNICO objetivo que queda entre H2 y su cierre, y por tanto el candidato dominante
  por dependencias. Aviso para su M0: es también el juez natural del argumento estructural
  con que se cerró O-estructura —si algún caso del centro real resultara inexpresable por
  formulario, aparecerá aquí—, y eso es hueco funcional de H2 que se afronta en este
  objetivo, no reabriendo el anterior.
- **Valor:** prueba de que H2 está terminado.
- **Absorbe:** D-seed-demo, D-demo-cliente (ambos objetivos disfrazados de deuda),
  y cierra la parte VIVA de D31 (validación de poblaciones con el centro).
- **Fuentes de datos:** los horarios reales del IES se extrajeron de los PDFs a
  JSON en una operación previa. Su documentación —`RESUMEN-EXTRACCION.md` (qué se
  extrajo) e `INFORME-RECONCILIACION.md` (cómo se resolvieron las discrepancias
  entre el horario por grupo y por aula, familia D8)— es el insumo de este objetivo
  y el primer paso de su M2: leerlas para saber qué datos hay y en qué estado
  quedaron.

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

#### O-diseño — "La aplicación tiene un aspecto cuidado y coherente." (ESBOZADO, criterio por definir)
- **Propósito:** trabajar la maquetación y la identidad visual de la aplicación
  —sistema de estilos, tipografía, tokens de color, consistencia entre vistas—,
  transversal a todas las pantallas. NO es maquetar una vista suelta: es el acabado
  visual del conjunto.
- **Terminado cuando:** POR DEFINIR (se concreta al abrirlo, con las vistas ya
  congeladas delante). Candidato: existe un sistema de estilos aplicado de forma
  coherente a todas las vistas (configuración, horario, exportación) y el aspecto
  deja de ser el de andamiaje por defecto.
- **Depende de:** H2 cerrado (O-estructura + O-demo). Razón (R-invalidación): todo
  lo anterior mueve la ESTRUCTURA de la UI —O-estructura añade los formularios
  pesados de currículo/desdobles/PDC/tutores/jornada, que reorganizan Configuración
  entera—; maquetar antes es pulir superficie que se va a reubicar. Se hace una vez,
  sobre vistas estables.
- **Valor:** presentabilidad. Hasta aquí los hitos se definen por función; este es
  el único objetivo puramente de acabado. Registrado en S106 a petición del
  arquitecto: sin sede propia, la maquetación o no se hace nunca o se cuela a trozos
  dentro de otros objetivos violando R-terminado ("pulir CSS ya que estoy en esta
  vista"). Tenerlo como objetivo lo protege por ambos lados.
- **Salvedad de prioridad:** si hay que ENSEÑAR la app a un cliente o al IES antes
  de cerrar H2, el aspecto deja de ser estético y pasa a ser presentabilidad de
  demo, que sí es valor entregable; en ese caso O-diseño (o un subconjunto mínimo)
  sube de prioridad. Decisión del arquitecto según el escenario real.
- **Grano abierto:** objetivo propio y separado, NO colgado de O-demo, porque el
  diseño transversal toca todas las vistas a la vez y no es "parte de" ningún hito
  funcional. Si al abrirlo resulta grande, se parte (métrica de §7).


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
| D-F8.6-ii-b (no hay gesto de despinar) | O-ajuste-cierre | SÍ | Se paga al abrir O-ajuste-cierre. Única deuda funcional de F8.6 |
| D-F8.5-D2a-a (I4 sin red) | O-catálogo | Sí, dentro de O-catálogo | Medido en S101: es de `ProfesorTutoria` (tutoría), NO del CRUD de Profesor. Su activación escrita («otra vía de escritura») NO la cumple un form que escribe por el REST existente. Se paga con el formulario de tutoría (roza O-estructura) |
| D-F8.5-E-b (unicidad profesor-tramo sin red) | O-catálogo | Sí, dentro de O-catálogo | Medido en S101: es de `ProfesorRestriccionHoraria` (disponibilidad, sub-recurso), NO del CRUD de Profesor. Se paga con el formulario de restricción horaria, no antes |
| D-F8.5-D2a-b (incoherencia 404/400 FK) | O-catálogo | No bloquea | Se evalúa dentro de O-catálogo |
| D18 (condiciones necesarias de factibilidad) | O-estructura | No | Ya cubierto en backend (8.4-A); resto en presentación |
| D-F8.6-iiiB1-c, -iiiB2a-a (superficie de error) | O-ajuste-cierre | No | Se evalúan al abrir; probablemente limitación conocida aceptable |
| D-F8.6-ii-a (el `reason` de los 400/409 no llega al navegador) | O-estructura (reasignada en S109; era O-ajuste-cierre) | No bloquea el criterio, pero DEGRADA todo lo entregado | AMPLIADA y RECLASIFICADA en S109 a técnica real TRANSVERSAL. La redacción de S81 decía que `server.error.include-message` no estaba en `application.properties`: hoy SÍ está y aun así el cuerpo llega sin `message` (medido por curl en tres endpoints, fuera de la UI). Todos los formularios pintan «Bad Request» en vez del motivo, y el 409 del PUT de actividad construido en S109 queda mudo. La causa (cambio de comportamiento en Spring Boot 4) es HIPÓTESIS no medida, y elegir el arreglo —reactivar la clave, `ProblemDetail`, o traducir en cada controlador— exige su propio M2: por eso no se pagó en S109. Hallazgo de método asociado: los tests de endpoint asertan `status().reason()`, que lee el `MockHttpServletResponse` y no el cuerpo de red — verde en test, mudo en producción. AFINADA en S110, medido en NAVEGADOR: el mensaje accionable NO se pierde —viaja como REASON PHRASE— y lo que falta es la clave `message` en el cuerpo; leer `statusText` en cliente NO es la solución (HTTP/2 no transporta reason phrases). AFINADA en S111 con un dato que su M2 debe usar como punto de partida: hay CONTRADICCIÓN DOCUMENTAL en el repo —el javadoc de `asignatura-lista.ts` afirma que `server.error.include-message=always` y el de `horario-view.ts` afirma que está DESACTIVADO—, y el comportamiento observado en navegador (el 409 de borrado de nivel pinta «Conflict» crudo) da la razón al segundo. Confirmada además en el octavo formulario: la lista de niveles nace muda. AFINADA en S112, y ESTE es el punto de partida de su M2, no el de S111: medido por lectura literal de `application.properties`, la clave `server.error.include-message=always` SÍ ESTÁ, con comentario propio que explica por qué se puso y por qué los tests no lo notan. El javadoc de `horario-view.ts` describe bien el SÍNTOMA y mal la CAUSA. La hipótesis viva pasa a ser que la clave está puesta y no surte efecto; su M2 debe EMPEZAR comprobando eso en ejecución, porque si se confirma, la opción «reactivar la clave» desaparece del abanico de tres. **COMPROBADO en S113, y con ello su M2 arranca un paso más adelante:** la clave está en `application.properties` Y en `target/classes`, sigue existiendo en la versión de Boot en uso, y aun así el cuerpo llega sin `message` (evidencia en crudo sobre `POST /api/grupos/{id}/pdc`). «Reactivar la clave» queda DESCARTADA por medición, no por hipótesis; el abanico se reduce a `ProblemDetail` o traducir en cada controlador. Superficie ampliada: los seis mensajes del flujo del PDC son genéricos, cinco «Bad Request» y un «Conflict» que pierde el desglose «referenciada por N plaza(s)». El arreglo es GLOBAL —el CRUD plano se comporta igual—, no del diálogo ni de las guardas de S113 |
| D-plaza-sin-subgrupos (una plaza con cero subgrupos se acepta) | O-estructura | No | Detectada por el M2 de S109: `validarPlazas` comprueba XOR, I7 e I2, pero acepta `subgrupos` nulo o vacío y devuelve 201. Agujero de dominio (la población de la plaza SON sus subgrupos). DECISIÓN de S109: el formulario refleja el contrato y NO añade el validador solo en cliente; hay un spec que se pondría rojo si alguien lo añadiera. El arreglo es simétrico a I7 (≈10 líneas y un test). No se paga ahora |
| D-i2-dedup-cliente (la deduplicación intra-plaza del validador I2 no la cubre ningún test) | O-estructura | No | Nace en S110 de la campaña de mutación: quitar el `Set` por fila del validador `subguposDisjuntos` no pone rojo nada. El escenario es INALCANZABLE desde la UI (un `<select multiple>` no repite opción; el GET proyecta desde un `Set`), así que la regla existe por fidelidad con `validarPlazas` y no porque haya camino que la ejercite. Deuda de TEST, hermana de D-jornada-flush-test. Escribir el caso exigiría fabricar un estado que el sistema no produce. No se paga ahora |
| D-horario-irreversible (un horario generado no se puede borrar ni reemplazar) | O-estructura | No bloquea el criterio, pero es un CALLEJÓN SIN SALIDA para el usuario | Nace en S111, medida en navegador y confirmada en código. No existe `DELETE /api/horarios/{id}` ni ningún borrado programático de `sesion`; cada `POST /api/horarios` ACUMULA (alta pura, sin consulta previa ni reemplazo), y el 409 del PUT/DELETE de actividad cuenta `sesion(es)` entre sus referentes. Consecuencia: en cuanto se genera un horario, las actividades que usa quedan congeladas para editar y borrar de forma PERMANENTE por la vía UI/API; la única salida es tocar SQLite a mano. El javadoc de `ActividadService.editar` prescribe «el usuario borra el horario y luego reconfigura», salida que NO existe. El `on delete cascade` de `sesion.horario_id` ya está en el esquema: el mecanismo está preparado y nadie lo dispara. Afecta al e2e solo si éste necesitara rehacer algo tras generar: MEDIDO en S112 y NO le afecta, porque cada corrida parte de una BD borrada y genera una sola vez. Sigue sin pagarse |
| D-error-generacion-pin (un fallo de generación se anuncia como fallo de pin) | O-estructura | No | Nace en S111. `lanzarGeneracion` reutiliza el helper `mensaje()` escrito para los pines, cuyo degradado es «El servidor rechazó el pin (N).»; ante un horario infactible (422) el usuario lee literalmente eso. Hermana de D-F8.6-ii-a: el texto del backend, que sí nombra el recurso culpable, se pierde por configuración y no por diseño del componente, así que las dos primeras ramas del `||` fallan siempre. Arreglo trivial (un degradado propio) pero encuadrado con esa deuda. No se paga ahora |
| D-molde-mensaje-cubierto-en-form (la precedencia de `mensaje()` en las listas de catálogo no la cubre nadie) | O-catálogo (CERRADO en S106) | No | Nace en S111 al destaparlo la mutación M6. El javadoc de `asignatura-lista.spec` y hermanas afirma que el orden interno de `mensaje()` está «cubierto en el form, misma función»: es FALSO —hay dos funciones copiadas a propósito y no compartidas, así que el caso del formulario no puede cubrir a la de la lista—. En niveles se cerró añadiendo la clave `error` al cuerpo flusheado del caso del 409, sin caso nuevo; las cuatro entidades de O-catálogo siguen con el hueco y con el comentario falso. NO se paga: R-terminado, el objetivo está cerrado. Cuando se toque una de esas listas por otro motivo, es una línea de fixture |
| D-doble-proyeccion-compartido (el doble de `getProyeccion` es un Subject compartido) | O-ajuste-cierre | No | Nace en S112. Es el ÚLTIMO doble compartido de `horario-view.spec.ts`: sus tres hermanos de escritura (`guardar`, `borrar`, `generar`) migraron a fresco por invocación en S94, y `bloqueos.listar` en S99. La forma compartida impide encadenar FALLO → RECARGA, porque un Subject cerrado por `.error()` redispara al re-suscribirse. Mordió en el caso (40), que arranca con la proyección en 404 —el escenario real de BD vacía—. Parcheado con re-stub LOCAL al caso, no homogeneizando el doble: migrarlo tocaría los 25 casos vigentes que lo consumen (R-terminado). Deja de ser aplazable con el segundo caso que necesite lo mismo. No se paga ahora |
| D-e2e-retry-bd (un reintento de Playwright correría sobre la BD del intento fallido) | O-estructura | No | Nace en S112. `retries: 2` en CI, y el `rm -f app/educhronos-e2e.db*` vive en el `command` del `webServer`, que corre UNA VEZ por corrida, no por test ni por reintento. Un reintento encontraría el centro ya creado y moriría con un 400 de código duplicado, es decir, por causa distinta de la original: esconde el diagnóstico. Hoy no bloquea porque NO HAY CI (Fase 12 sin abrir). Se resuelve al abrirla. No se paga ahora |
| D-e2e-aislamiento (la suite e2e corre en paralelo sin aislamiento entre specs) | O-estructura | No | Nace en S112. `fullyParallel: true` sin `workers` reparte los specs entre workers que atacan el mismo backend y la misma BD. Inocuo HOY por una razón concreta y no por suerte: `humo` solo lee (la landing no llama a `/api`) y `centro-minimo` es el único que escribe. El riesgo llega con el TERCER spec: dos writers sobre un SQLite único chocarán por los `unique` de código de forma no determinista, que es la clase de fallo intermitente que desprestigia una suite entera. Se decide al escribir el segundo spec que escriba. No se paga ahora |
| D-props-test-obsoleto (el `application.properties` de test afirma que `schema.sql` dropea) | O-estructura | No | Nace en S112. Dice «schema.sql dropea y recrea, de modo que varios contextos Spring sobre este mismo fichero recrean el esquema con FK sin petar»; falso desde S109. Es la MISMA falsedad que S112 corrigió en `playwright.config.ts`, cuya hermana quedó viva. Efecto de lectura, no de ejecución (la suite de backend se limpia por otra vía), pero por R5 es estado vivo equivocado: hace que el siguiente lector decida sobre una premisa falsa. Se corrige al tocar ese fichero. No se paga ahora |
| D-pdc-lista-rancia (la lista de subgrupos no se entera del alta ni del borrado de un PDC) | O-estructura | No | Nace en S113 y la abre el propio Cambio: el alta de un PDC toca DOS catálogos (crea el grupo y su subgrupo mono-Di) pero el contrato del molde —«el diálogo cierra con `true` y recarga quien lo abrió»— solo alcanza a `GrupoLista`. MEDIDO en navegador: la sección de subgrupos seguía diciendo «No hay subgrupos todavía» con el subgrupo ya en la BD; simétrico al borrar. NO se paga aquí, con razón escrita: no es del género de las guardas (vista desactualizada, no destrucción de datos), arreglarla exige coordinar componentes hermanos dentro de `Configuracion` —que ES la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio propio— y no bloquea el criterio, cosa que el M4 demuestra: el §6.2 se reprodujo entero con la lista rancia de por medio. Se resuelve en el Cambio que decida la navegación; no se parchea con un `EventEmitter` ad hoc, que fijaría el molde por la puerta de atrás |
| D-pdc-vinculo-por-cadena (el agregado PDC localiza su subgrupo por código derivado) | O-estructura | No | Nace en S113. `PdcService.borrar` resuelve el mono-Di con `findByCodigo(codigo + "-Completo")`: el agregado que su javadoc dice poseer no estaba protegido fuera de sus tres métodos, y cualquier rename por otra vía dejaba el DELETE del PDC en 404 permanente. G2 CONTIENE la deuda cerrando el único camino que existía (el CRUD plano de subgrupos); vuelve a morder con un tercer camino de escritura hacia `Subgrupo`. Familia de D-F8.5-D2a-a y D-F8.2b-iv-a (validación de aplicación sin espejo en la base). Convertir la convención en referencia real es cambio de ESQUEMA, no una guarda: se evalúa cuando algo más toque `schema.sql` en esta zona. No se paga ahora |
| D-tokens-inexistentes (la familia `D-nueva-*` se cita en nueve sitios y no existe) | Transversal, sin objetivo asignado | No | Nace en S113 al auditar en R4 los tokens que la sesión introducía. `D-nueva`, `D-nueva-1` … `D-nueva-5` aparecen en `GrupoService`, `GrupoDTO`, `GrupoRequest`, `GrupoEndpointTest`, `grupo-form.ts` y la cabecera de `grupo.model.ts`, y ninguno tiene definición viva en este documento ni en el plan. Incumple R4 en su forma más simple; el daño es que el lector busca el token, no lo encuentra y no sabe si la regla sigue vigente. PREEXISTENTE: S113 corrigió solo el que ella misma introdujo (`D-nueva-2`) y registró el resto, porque mapear nueve citas a sus deudas reales exige leer nueve contextos y es trabajo propio, no un arreglo en caliente. Sesión de Higiene/Método, junto con el script de R4 que falta desde S101 |
| D-log-aplicacion (no hay logging estructurado en ninguna de las dos capas) | Transversal, sin objetivo asignado | No | Propuesta del arquitecto en S111 tras el recorrido en navegador, donde diagnosticar un fallo exigió leer código en vez de logs. Backend sin configuración de logging a fichero (solo consola); frontend sin ninguna traza, con `ngx-logger` mencionado como candidato pero NO evaluado. Mejora FUTURA: se registra para que no se pierda, no planifica y no cuelga de ningún objetivo vivo |

#### Mejora futura, cuelga y espera
| Deuda(s) | Objetivo | Nota |
|---|---|---|
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
| D-diagnostico-no-es-foto (el diagnóstico de un horario recalcula contra el catálogo vivo) | O-ajuste-cierre | Nace en S114 al no poder medirse el paso 9 del M4 como estaba planteado. `DiagnosticoService` verifica contra el catálogo ACTUAL, y `verificarTutorias` no mira la solución (`VerificadorSolucion.java:56`), así que para S8 el diagnóstico responde «¿esto sería válido AHORA?» y no «¿lo era al generar?». Consecuencia medida: el horario #1, generado con la violación real, se presenta hoy impecable; y el #2, generado limpio, se pinta en rojo si alguien quita el tutor después. Afecta solo a S8 (las demás reglas sí leen la solución). No bloquea nada hoy y el registro histórico no existe como requisito en ningún criterio. Se decide al abrir la vista de diagnóstico, junto con D-s8-muda. No se paga ahora |
| D-post-horario-sin-sesiones (el POST de generación devuelve la proyección sin sesiones) | O-ajuste-cierre | Nace en S114, medido en red. El horario se persiste bien (3 filas en `sesion`) y el `GET /{id}/proyeccion` las devuelve, pero el cuerpo del POST llega con `sesiones: []`. Inocuo HOY por una razón concreta y no por suerte: la UI recarga por `paramMap` y nunca lee el cuerpo del POST (decisión de S93, recargar por GET fresco). Muerde a cualquier cliente futuro que se fíe de la respuesta —incluido un e2e que quisiera asertar sobre ella—. No se paga ahora |
| D-tutor-pdc-desincronizado (la herencia del tutor al PDC corre solo en el alta) | O-estructura (cerrado) | Nace en S114. `PdcService.heredarTutorPrincipal` se invoca únicamente desde el alta (`PdcService.java:110`): si después se cambia el tutor del padre con el PUT, el PDC conserva el antiguo en silencio. No es un bug del código actual —nadie prometió resincronización— pero C-tutores lo hace VISIBLE por primera vez: se verán dos grupos emparentados con tutores distintos y nada explicará por qué. No bloqueaba el criterio (el §6 no exige reasignar tutores) y por eso el objetivo cierra con ella viva. Arreglarla es decisión de dominio, no una guarda —¿copia o referencia?—, familia de D-pdc-vinculo-por-cadena; se evalúa si O-demo la hace morder con el centro real. No se paga ahora |
| D-dialogo-foco-perdido (al salir del estado «cargando» el foco cae fuera del diálogo) | O-diseño | Nace en S114, medida en los tres diálogos. El CDK enfoca el botón de la rama `cargando`; cuando el `@switch` cambia de rama ese elemento se destruye y el foco cae a `<body>`, fuera del diálogo. `GrupoForm` (sin estados) conserva el foco dentro; `PdcDialogo` y `TutoriaDialogo` no. NO la introduce C-tutores: `PdcDialogo` hace lo mismo desde S113. Para teclado y lector de pantalla el diálogo queda abierto sin foco dentro. Arrastra una consecuencia de andamio: la barrera `:focus` con que `centro-minimo.spec.ts` evita la carrera del portal no sirve en diálogos con estados, así que si algún e2e futuro abre uno de estos dos habrá que sustituirla por una espera al contenido. Es acabado de interacción, transversal a las vistas: cuelga de O-diseño. No se paga ahora |
| D-bundle-presupuesto (el bundle inicial excede el techo declarado) | O-diseño | Preexistente desde antes de S112 (507,66 kB frente a 500 kB en `angular.json`, verificado sobre HEAD limpio); S113 lo lleva a 514,42 kB al entrar `PdcDialogo` en el grafo de dependencias. NO se toca `angular.json`: subir el techo es configuración de build, no está en el criterio de ningún objetivo vivo, y hacerlo «de paso» convierte un aviso útil en un número que nadie vuelve a mirar. Cuelga de O-diseño, que tendrá delante el bundle completo y las vistas congeladas y podrá elegir entre subir el techo, rutas perezosas o recortar. Hasta entonces, anotar el delta en cada sesión que compile |
| D5, D6, D9, D11, D16, D17, D21, D27, D29 | Fase 5/8 según su asignación en el plan | Deuda de solver/dominio ya asignada; se reevalúa al abrir su objetivo |

#### Decisión arquitectónica consciente → sale de la cola
| Deuda | Razón (ya escrita en el plan) |
|---|---|
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
D-F8.5-A-a (S73/S74), D-F8.6-iiiA-a (S85), y las condensadas en la sección de
deuda cerrada del plan. Se conservan como registro con remisión a la bitácora.

**Resultado agregado:** de las ~47 deudas vivas (S113 añade siete), tras
reclasificar, **1 es bloqueante ahora** (D-F8.6-ii-b, y solo al abrir
O-ajuste-cierre), ~11 son deuda técnica real que se paga DENTRO de su objetivo
cuando llegue, y el resto (~35) sale de la cola de trabajo activo como mejora
futura que espera, decisión consciente o limitación conocida. La cola de "deudas
que me obligan a abrir sesión" sigue en ~1. Nota sobre la tendencia, visible
desde S109: la cola CRECE sesión a sesión y eso no es alarma por sí solo —el
crecimiento es casi todo de mejora futura y de deuda registrada al medir, no de
deuda técnica real acumulándose sin pagarse—; la métrica que sí hay que vigilar
es "deuda bloqueante abierta" (§7), que lleva en 1 desde que existe el mapa.

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

**Orden nuevo: O-shell → O-catálogo → O-estructura → O-demo (todo H2), luego
O-ajuste-cierre (H1), luego O-diseño (acabado visual, con las vistas ya
congeladas), luego H3, luego H4.** (O-diseño puede adelantarse si una demo a
cliente/IES lo exige antes de cerrar H2; ver su salvedad de prioridad en §3.)

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
