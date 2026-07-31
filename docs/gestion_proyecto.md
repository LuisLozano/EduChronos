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
| **H2 — Configurar un centro desde cero** | Crear profesores, aulas, grupos, currículo, desdobles, PDC, tutores por formularios y llegar a un horario válido sin tocar la BD | ~15% (O-shell hecho S100; O-catálogo TERMINADO S104, criterio precisado S106: 4 de 4 entidades CRUD por UI — Profesor (S101), Aula (S102), Asignatura (S103), Grupo (S104). El e2e UI→solver, antes 2ª mitad de O-catálogo, se reasignó a O-estructura en S106 al medirse que depende de currículo/jornada. O-estructura ABIERTO S107, 2 piezas hechas: C-jornada (S107, backend REST `/api/jornada` + formulario singleton, dimensión temporal del solve) y C-subgrupos (S108, CRUD de subgrupos por UI sobre `/api/subgrupos`, con multiselect de grupos; desbloquea el editor de actividades porque las Plazas referencian subgrupos por código); faltan C-actividades (editor de Plazas, backend ya existe), desdobles, PDC, tutores y el e2e UI→solver. O-demo pendiente) | Criterios 5–6 de Fase 8: "configurar centro desde cero → horario válido" y "crear grupo nuevo se incorpora a las particiones" |
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

#### O-estructura — "Expreso la complejidad real del centro."
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
  política e2e de §6.
- **Depende de:** O-catálogo.
- **Valor:** valida el MODELO UNIFICADO contra el usuario real. Es el objetivo de
  mayor riesgo del proyecto (si el modelo Actividad→Plaza→Subgrupo no se puede
  configurar de forma usable, hay que rediseñarlo) y por eso debe abordarse
  temprano, con margen.
- **Cambios que agrupa:** editor de demanda curricular, asistente de
  desdoble/agrupamiento (D1, D10), editor de PDC (D7), asignación de tutores,
  configuración de estructura de jornada (D22).
- **Progreso (S108):** ABIERTO, 2 piezas hechas. **C-jornada** (S107, D22):
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
  exige. NO cierra el objetivo: falta C-actividades (el editor de Plazas, backend ya
  existe —siguiente Cambio, mayor riesgo del objetivo—), desdobles, PDC, tutores y el
  e2e UI→solver heredado.
- **Absorbe:** D1, D7, D10, D22, D30, D-F8.5-D1-b, y las deudas de subgrupos
  compartidos. D22 saldada de facto (C-jornada, S107). Nace y cuelga aquí
  D-subgrupo-ux-multiselect (S108): la UX del `<select multiple>` de subgrupos es
  mejora PLANIFICADA (fase de mejora de UX de subgrupos), no deuda técnica ni
  bloqueante (ver §4 y el plan).

#### O-demo — "El centro real funciona de punta a punta."
- **Propósito:** cargar el IES de Sevilla por la UI y generar su horario.
- **Terminado cuando:** el guion de aceptación de H2 pasa sobre datos reales.
- **Depende de:** O-estructura.
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
| D-F8.6-ii-a, -iiiB1-c, -iiiB2a-a (superficie de error) | O-ajuste-cierre | No | Se evalúan al abrir; probablemente limitación conocida aceptable |

#### Mejora futura, cuelga y espera
| Deuda(s) | Objetivo | Nota |
|---|---|---|
| D-F8.6 de cobertura (iiiB1-a, ivB-a-bis, ivD-a, ivA-a, ivA-c, ivB-b, ivB-c, iiiA-b, B-a) | O-ajuste-cierre | Cobertura de la vista de horario. La mayoría se RECLASIFICA a limitación conocida en cuanto O-shell reubique la vista (su contexto de test cambiará). NO se pagan ahora |
| D-F8.4-A-a, -A-b, -A-c, -B1-a | O-ajuste-cierre | Cobertura de prevalidación |
| D-S101-num (numeración global de tests colisionada) | O-ajuste-cierre | Detectada S101: la secuencia (N) de la capa componentes/servicios tiene colisiones preexistentes —(27),(28-30),(35-37) con contenidos distintos en dos ficheros— que rompen la atribución por (N) en campañas de mutación. Es superficie de specs de H1 (cerrado). Los specs de O-catálogo la esquivan abriendo secuencia propia por fichero. Arreglarla no bloquea nada (R-terminado): no se paga ahora |
| D-F8.5-D2b2-a, -D2b2-b (diseño/cosmética) | — | Sin objetivo urgente |
| D-F8.5-C3-a, -C3-b, -C2a-a | O-catálogo | Semántica/dominio de catálogo, a resolver con datos. C3-a CONTENIDA en UI desde S102 (COMUN fuera del selector del form de Aula); sigue viva a nivel de esquema. C3-b: los códigos por currículo (Mat/LCL usados en specs de S103 son reales de este catálogo) siguen sin UI para poblar compatibilidades (ver D-S103-compat) |
| D-S103-compat (CRUD de asignatura no alcanza `aulas-compatibles`) | Cambio de compatibilidad (tras Grupo, o dentro de O-estructura) | Detectada S103: el backend expone `GET/PUT /{id}/aulas-compatibles` pero el CRUD plano no lo alcanza. NO bloquea O-catálogo (semántica S75: 0 filas ⇒ irrestricta; un centro mínimo corre sin poblar compatibilidades). Incluye decidir la no-atomicidad POST→PUT. Estirar el molde con el sub-recurso es Cambio propio. No se paga ahora |
| D-jornada-msg409 (mensaje del 409 dice «No se puede borrar» al guardar jornada) | O-estructura | Detectada S107: `ReferenciaEntranteException` se escribió para los DELETE de catálogo; su mensaje se reutiliza en el PUT de jornada y el usuario lee «No se puede borrar: referenciada por…» cuando intenta GUARDAR. Cosmético, NO bloquea (el desglose «N sesiones, M restricciones… antes de reconfigurar» sí es correcto). Se corrige con un mensaje propio del caso PUT cuando O-estructura vuelva a tocar el backend de jornada; no se paga ahora (R-terminado, M3 cerrado) |
| D-jornada-asimetria (contrato GET(35)≠PUT(7 día tipo)) | O-estructura | Detectada S107, consecuencia consciente de «el backend expande»: el GET devuelve la malla completa, el PUT acepta un día tipo. Nota de diseño de API, no deuda bloqueante: la UI convive sin fricción real (pinta un día, manda un día). Reconsiderar `diaTipo` en el GET solo si un futuro cliente lo pide |
| D-jornada-flush-test (`put_dosVecesLaMismaMalla_idempotente` no discrimina el flush) | O-estructura | Detectada S107: falta `UNIQUE(dia,orden)` en `schema.sql`, así que sin el `flush()` el resultado sería el mismo y el test no lo prueba. El `flush()` es defensivo/preventivo (correcto: fuerza DELETE antes de INSERT). Si algún día se añade la constraint, el test pasa a discriminar. Deuda de test, no de código |
| D-subgrupo-ux-multiselect (el campo «grupos» del form de subgrupo es un `<select multiple>` nativo) | O-estructura (o O-diseño si absorbe el acabado) | Detectada S108, DECISIÓN CONSCIENTE de alcance: se eligió la mínima desviación del molde (`<select multiple>` nativo) y la UX rica —chips, búsqueda, casillas— se aplaza a una fase de mejora de UX de subgrupos ya prevista al abrir el Cambio. NO es deuda técnica (el componente funciona, valida I6 en cliente, 12 tests) ni bloquea el criterio de O-estructura (la población se elige, solo sin comodidad). El `.subgrupo-form__multiple` y el handler `alSeleccionar` son el punto de sustitución. No se paga ahora |
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

**Resultado agregado:** de las ~40 deudas vivas, tras reclasificar, **1 es
bloqueante ahora** (D-F8.6-ii-b, y solo al abrir O-ajuste-cierre), ~8 son deuda
técnica real que se paga DENTRO de su objetivo cuando llegue, y el resto (~30)
sale de la cola de trabajo activo como mejora futura que espera, decisión
consciente o limitación conocida. La cola de "deudas que me obligan a abrir
sesión" pasa de ~40 a ~1.

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
