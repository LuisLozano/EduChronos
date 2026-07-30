# Plan de Trabajo — Aplicación de Horarios Escolares

## Cómo usar este documento en el Project de Claude

Este es uno de tres documentos con responsabilidad separada:
- **`gestion_proyecto.md`** — planificación: el mapa Hito→Objetivo→Cambio, la
  clasificación del trabajo pendiente y las reglas estratégicas. Responde «¿por
  qué esta sesión?».
- **`metodo.md`** — método: cómo se ejecuta una sesión (M0–M5, R4/R5, tipos de
  sesión). Referencia estable; no se relee entero en cada apertura.
- **`plan_trabajo_horarios.md`** (este) — registro: qué pasó (progreso,
  decisiones permanentes de stack, fases completadas, notas técnicas) y la deuda
  con su texto íntegro.

Al iniciar cada sesión, la apertura sigue M0 de `metodo.md`: nombrar el Cambio,
el Objetivo y el Hito que la sesión avanza (leídos de `gestion_proyecto.md`). La
planificación NO gira ya alrededor de «la fase» ni de «la deuda», sino del
objetivo activo. Este plan aporta el registro histórico y el detalle de la deuda;
el mapa que decide qué se abre vive en `gestion_proyecto.md`.

---

## Principios de avance

Estos principios rigieron las Fases 0–7 (solver e infraestructura, ya cerradas).
Desde la Fase 8 la planificación se rige por `gestion_proyecto.md` (mapa
Hito→Objetivo→Cambio) y `metodo.md` (M0–M5). Se conservan como contexto histórico;
los principios vivos hoy son las reglas estratégicas de `gestion_proyecto.md` §6:

- **R-terminado** — un objetivo termina al cumplir su criterio; las mejoras que no
  lo cambian ni desbloquean el siguiente objetivo no se ejecutan dentro de él.
- **R-deuda** — la deuda nunca planifica; solo se salda si bloquea el objetivo
  activo.
- **R-invalidación** — no refinar lo que un objetivo ya planificado va a rehacer.
- **R-apertura** — toda sesión nombra su Cambio, Objetivo e Hito (M0).

Principios originales (Fases 0–7, cumplidos): no pasar de fase sin criterios de
verificación; el solver primero (ninguna UI antes de la Fase 4); complejidad
incremental (cada fase una capa nueva); criterio de bloqueo (más de 2 sesiones
atascado ⇒ replantear, no parchear).

---

## FASE 0 — Decisión de stack tecnológico
**Objetivo:** Elegir y justificar el stack completo antes de escribir una línea de código.

### Entregable
Documento de decisión con: lenguaje/framework backend, solver elegido, 
base de datos, tecnología UI, empaquetado Windows.

### Criterios de verificación
- [x] Has evaluado al menos Timefold y OR-Tools para el solver
- [x] El stack elegido puede representar los 8 tipos de sesión del modelo
- [x] Existe un "Hello World" del solver funcionando en local (no tu código, 
      el ejemplo oficial del solver)
- [x] Has confirmado que el empaquetado para Windows es viable con ese stack

### Señal de que está mal
Elegir stack por familiaridad sin haber probado el solver con un ejemplo mínimo.

---

## FASE 1 — Modelo de datos validado en papel
**Objetivo:** Diseñar el esquema de tablas y validarlo contra los datos reales 
ANTES de crear ninguna base de datos.

### Entregable
Diagrama entidad-relación (puede ser texto estructurado) que represente:
profesores, aulas, grupos, grupos virtuales PDC, asignaturas, 
los 8 tipos de sesión, franjas horarias, restricciones.

### Criterios de verificación
- [x] Puedes representar manualmente en el modelo el horario completo de 1ºA 
      (incluyendo su desdoble de CyR y su agrupamiento de RefMt)
- [x] Puedes representar el subgrupo 3ºADi (PDC) y sus sesiones compartidas 
      con 3ºA y sus sesiones propias en A8
- [x] Puedes representar TICO de 1ºBach (4 grupos comparten aula y profesor)
- [x] El modelo puede expresar "estas 3 sesiones deben ocurrir simultáneamente 
      y bloquean estos 4 grupos"
- [x] Puedes representar el horario completo de 1ºBACH B (Sesión 4): troncales, 
      bloques de optativas transversales sobre 4 grupos (OPT1, OPT2), bloque de 
      modalidad sobre 2 grupos (Bio/TecIn), Religión/PTVE intra-grupo, y el caso 
      de una optativa (DTec) cuya población aparece en dos bloques distintos
- [x] Puedes representar el horario completo de 1ºFPB (Sesión 5): bloques 
      obligatorios de 2 y 3 tramos consecutivos, sesiones sueltas de la misma 
      asignatura en otra actividad, tutoría sin asignatura "TUT" explícita, 
      y aula específica de una sola asignatura (CS en TALL3) frente al resto 
      del horario del grupo

### Señal de que está mal
Si necesitas una tabla o columna nueva para cada tipo especial de sesión 
en lugar de un modelo unificado.

### Resultado de la fase
Modelo unificado documentado en `modelo_datos_fase1.md`. La complejidad de los
8 tipos de sesión del enunciado se resuelve con la estructura
`Actividad → Plaza → Subgrupo` y particiones de alumnos persistentes.
Diagramas ER (catálogo del centro y planificación del horario) generados aparte.

---

## FASE 2 — Solver MVP: problema mínimo
**Objetivo:** El solver genera un horario válido para un subconjunto pequeño 
y simple. Sin UI, sin base de datos, sin desdobles ni agrupamientos.
**Sí incluye co-docencia intra-aula** (LCL de 1ºESO): un grupo, dos
profesores, una aula. Estructuralmente es una sola plaza con
`|PlazaProfesor|=2`, no un desdoble.

### Subconjunto de datos
4 grupos de 1º ESO (A, B, C, D), sesiones ordinarias 
(Matemáticas, Lengua [LCL en co-docencia LEN2+LEN8 / LEN3+LEN9 según
grupo], Inglés, Biología, Geografía, Música, Plástica, EF, Religión/Valores), 
con sus profesores y aulas reales del instituto de ejemplo.

### Entregable
Programa ejecutable por consola que:
1. Carga los datos del subconjunto (hardcoded o JSON/CSV)
2. Lanza el solver
3. Imprime el horario resultante
4. Imprime las restricciones violadas (debe ser 0 duras)

### Criterios de verificación
- [x] Ningún profesor aparece en dos sitios a la vez
- [x] Ninguna aula tiene dos sesiones simultáneas
- [x] Ningún grupo tiene dos sesiones en el mismo tramo
- [x] El recreo (11:00-11:30) está vacío
- [x] El solver termina en menos de 2 minutos
- [x] Las sesiones de cada asignatura están distribuidas a lo largo
      de la semana (no dos Matemáticas el mismo día)
- [x] En las sesiones de LCL de 1ºESO, ambos profesores de la plaza
      aparecen en sus respectivos horarios por profesor en el mismo
      tramo, y ninguno tiene conflicto con otra sesión en ese tramo
      (validación del soporte de co-docencia desde el MVP)

### Señal de que está mal
El solver tarda más de 5 minutos o viola restricciones duras en la solución final.

### Decisiones tácticas

Decisiones de alcance local a esta fase. Se levantan o promocionan a permanentes
en fases posteriores según el plan:

- **Aulas en el solver:** solo se ejercita la rama `aulaFija` de `Plaza`. El DTO
  y el POJO de `Plaza` soportan también `aulasCandidatas`, pero el cargador
  rechaza con error explícito cualquier dataset que las use hasta Fase 3. La
  rama de candidatas en el solver entra junto con los desdobles y agrupamientos
  en Fase 3.
- **DTO de plaza multi-profesor (Hallazgo J):** el campo `profesor` (string
  con código único) NO existe. Desde el primer commit el DTO usa
  `profesores: [<codigo>]` (array no vacío). El cargador rechaza arrays
  vacíos (validación de invariante I7). En el modelo de dominio, `Plaza`
  expone un `Set<Profesor>` (o equivalente, no una referencia única). En
  el modelo CP-SAT, el `AddNoOverlap` por profesor agrupa intervals por
  pertenencia al conjunto (`p ∈ plaza.profesores`), no por igualdad a una
  FK. Esto evita una migración del schema y del modelo en cuanto se
  introduzca cualquier dataset con co-docencia (LCL de 1ºESO ya lo
  introduce en esta misma fase).
- **Estructura del repositorio en Fase 2:** se crea únicamente el módulo
  `solver/` dentro del parent pom. El módulo `app/` se declarará en el parent
  cuando se entre en Fase 6 (no antes, para no fijar prematuramente una versión
  de Spring Boot que se oxidará).
- **Bloques de trabajo de Fase 2 (sub-fases internas):** (1) setup del
  repositorio, (2) diseño de los POJOs del dominio, (3) schema JSON + DTOs +
  mapper, (4) construcción del modelo CP-SAT, (5) output a consola,
  (6) dataset real 1ºESO ordinarias + co-docencia LCL + verificación
  contra los PDFs.

- **POJOs del dominio — identidad:** el módulo `solver` no usa `id`
  sintético. Cada entidad tiene un campo `String codigo` como clave
  natural (ej. "MAT8", "A12In", "1ºA"). Sin `long id`, sin `UUID`.

- **POJOs del dominio — inmutabilidad:** entidades de configuración
  (Profesor, Aula, GrupoAdministrativo, Asignatura, Tramo, Subgrupo,
  Plaza, Actividad) se implementan como `record`. ActividadInstancia
  también es un `record` puro de domain: solo (Actividad, indice), sin
  variables CP-SAT. Las variables CP-SAT (IntVar de tramo, IntervalVar)
  NO viven en domain: las porta una clase propia del paquete `cpsat`
  que envuelve una ActividadInstancia. Así domain permanece libre de
  OR-Tools (decisión permanente). Corregido en Sesión 11: la redacción
  anterior atribuía variables CP-SAT a ActividadInstancia (y mencionaba
  una clase PlazaAsignacion sin correlato en los POJOs reales), lo que
  contradecía la decisión de aislar OR-Tools en `cpsat`.

- **POJOs del dominio — grafo de referencias:** referencias directas
  entre objetos. La resolución de códigos ocurre una sola vez en el
  mapper (Bloque 3). El modelo CP-SAT trabaja con referencias Java,
  no con códigos String.

- **POJOs del dominio — enums vs Strings:** enums donde el solver
  hace comparaciones o switch (PatronTemporal, EstadoHorario). String
  donde el valor solo se imprime (nombres legibles, etiquetas UI).

- **POJOs del dominio — agregado raíz:** no existe `Centro` en el
  módulo `solver`. El solver recibe un `ProblemaHorario` (colecciones
  de entrada) y devuelve un `SolucionHorario`. La entidad `Centro`
  aparecerá en la capa JPA del módulo `app` en Fase 6.

- **POJOs del dominio — paquetes:** `domain/` (POJOs puros),
  `cpsat/` (modelo CP-SAT), `io/` (DTOs + mapper), `cli/` (entrada).
  Dependencias unidireccionales: cpsat e io dependen de domain;
  domain no importa nada de cpsat ni de io.

- **Distribución temporal en Fase 2 (restricción dura, no blanda):** el
  criterio de verificación "sesiones de una asignatura distribuidas a lo
  largo de la semana" se modela como restricción DURA (AllDifferent sobre
  el día de las ActividadInstancia de la misma Actividad), aplicada solo a
  actividades con patronTemporal=DISTRIBUIDA y con guarda
  repeticionesPorSemana ≤ nº de días distintos. En el enunciado original
  es blanda; se promueve a dura en Fase 2 para mantener factibilidad pura
  sin función objetivo. Decidido en Sesión 11.

### Criterios de verificación por bloque

Criterios operativos de cierre de cada bloque interno de Fase 2.
Complementan, no sustituyen, los criterios de verificación de la fase.
Los Bloques 1-3 se cerraron antes de existir este apartado.

**Bloque 4 — Construcción del modelo CP-SAT**
- [x] El modelo CP-SAT se construye desde un ProblemaHorario sin excepción
- [x] El paquete `domain` no importa nada de OR-Tools; el acoplamiento a
      CP-SAT está aislado en el paquete `cpsat`
- [x] El test carga problema-solver-minimo.json, resuelve y obtiene estado
      FEASIBLE u OPTIMAL
- [x] La verificación independiente de la solución (VerificadorSolucion)
      confirma 0 solapes de profesor, 0 de aula y 0 de subgrupo
- [x] Toda ActividadInstancia queda colocada en exactamente un Tramo
- [x] En la co-docencia LCL, ambos profesores de la plaza aparecen
      ocupados en el mismo tramo (el no-solape de profesor cuenta a
      TODOS los profesores de la plaza, no solo a uno)

**Bloque 5 — Output a consola**
- [x] El programa imprime el horario resuelto de forma legible
- [x] Imprime el recuento de restricciones duras violadas, que debe ser 0
- [x] La comprobación de violaciones reutiliza VerificadorSolucion (misma
      lógica que el test del Bloque 4, no una segunda implementación)

**Bloque 6 — Dataset real 1ºESO + verificación contra los PDFs**
- [x] El dataset de 1ºESO transcribe fielmente los hechos ESTRUCTURALES
      del PDF: qué actividades, co-docencias, profesores, aulas y
      repeticiones por semana
- [x] La SALIDA del solver se verifica contra las restricciones duras del
      checklist de Fase 2, NO contra la colocación celda a celda del PDF
- [x] Se cumplen todos los criterios de verificación de Fase 2

Nota sobre el Bloque 6: el solver de Fase 2 es de factibilidad pura, sin
función objetivo. Devuelve UNA de las muchas soluciones factibles. El
horario del PDF es un punto concreto que un humano eligió equilibrando
preferencias blandas implícitas; no es la salida esperada del solver.
Exigir reproducción celda a celda sería un malentendido del alcance.
El PDF es la fuente del dataset de entrada y de la verdad estructural,
no la salida de referencia. Decidido en Sesión 11.
---

## FASE 3 — Solver: desdobles y agrupamientos
**Objetivo:** Añadir al solver los tipos de sesión más complejos.

### Lo que se añade
- Desdoble de CyR para los 4 grupos de 1ºESO (TEC3 + INF1 simultáneos)
- Agrupamiento de RefMt (4 grupos → 3 subgrupos con 3 profesores simultáneos)
- Agrupamiento de OyD/VEtic (multi-grupo, un profesor, una aula)

### Criterios de verificación
- [x] Las dos sesiones del desdoble de CyR aparecen en el mismo tramo
- [x] Ninguno de los 4 grupos tiene otra sesión en el tramo de RefMt
- [x] Los 3 profesores del RefMt están libres en ese tramo (no aparecen
      en otras sesiones)
- [x] Las aulas del desdoble/agrupamiento son distintas y correctas
      (A12In para INF1, A5 para TEC3, etc.)
- [x] Puedes bloquear manualmente un tramo y el solver lo respeta
      — CERRADO en S58 (Bloque 8.2a). Mecanismo implementado como pin de
      instancia a tramo: record domain.SesionBloqueada(instancia, tramo), 9º
      componente de ProblemaHorario, restricción dura restriccionSesionBloqueada()
      en construir() (addEquality sobre el tramoIndex), verificador independiente
      contarBloqueosViolados, entrada por JSON (SesionBloqueadaDto + schema). El
      desdoble se pina simultáneo por el tramoIndex compartido. Validado por ORO
      (comentar la restricción hace caer 3 de 4 tests del pin). Pin de AULA diferido
      a 8.2b (requiere el par plaza,aula). Persistencia/REST del bloqueo = 8.2b.

### Señal de que está mal
El solver "resuelve" el problema metiendo las sesiones en tramos distintos 
para evitar el conflicto en lugar de forzar la simultaneidad.

---

## FASE 4 — Solver: grupos PDC/Diversificación
**Objetivo:** El solver gestiona correctamente los grupos virtuales PDC.

### Lo que se añade
- Subgrupo 3ºADi (Diversificación): sus sesiones propias en A8 
  (ÁmbSL, ÁmbCM, IngDi) y las sesiones compartidas con 3ºA (EF, EPVA, Tec)

### Criterios de verificación
- [x] El subgrupo 3ºADi no tiene sesiones solapadas con su grupo ordinario 3ºA. 0 violaciones de grupo del VerificadorSolucion (S9 prohíbe dos instancias distintas tocando el mismo grupo en un tramo).
- [x] Las sesiones compartidas (EF, EPVA, Tec) aparecen en el mismo tramo
      para 3ºA y 3ºADi → aserción de que cada instancia compartida toca a la vez 3ºA y 3ºADi.
- [x] LEN2 (profesor de ÁmbSL) no aparece en otro sitio cuando está con 3ºADi → LEN2 exclusivo de AmbSL-3ºADi + 0 colisiones de profesor.
- [x] El horario impreso de 3ºA muestra correctamente qué alumnos están
      en diversificación en cada tramo → validado por aserción programática (tramos compartidos ≥1 y divergentes ≥1; ningún grupo tocado por dos instancias distintas en un tramo).

---

## FASE 5 — Solver: instituto completo
**Objetivo:** El solver funciona con todos los grupos reales del instituto.
**Estado (S44): FASE 5 CERRADA.** Criterios 1-2 cerrados en S36 (factibilidad
pura, instituto completo, < 10 min, 0 duras). Criterios 3-4 cerrados en S44 como
decisión de producto gemela de D23 (umbral de calidad/ventanas requiere datos del
centro, inexistentes en desarrollo), con respaldo descriptivo medido a escala. Ver
el detalle en cada criterio. Siguiente: Fase 6 (persistencia).
> **Estado (S29):** fase EN CURSO, subdividida en bloques internos (como Fase 2).
> Bloques 1-6d-c, Bloque 7 (escala 4ºESO ordinario) y Bloque 8 (escala 4ºESO
> completo, +2 PDC) CERRADOS — ver "### Bloques de Fase 5"
> y los registros de Sesiones 19-28. Ninguno de los criterios de verificación de
> abajo está cerrado del todo
> (el criterio 4 está PARCIAL desde S25). Las razones difieren según el criterio:
>   - Criterios 1 y 2 (tiempo < 10 min, 0 duras): faltan porque exigen el
>     INSTITUTO COMPLETO (≈28 grupos + Bach + FPB), aún no abordado. Lo medido
>     hasta ahora son escalones (máx. 11 grupos). Hay evidencia parcial
>     acumulada: curva de escala (criterio 1, factibilidad pura) con TRES puntos
>     — 7 grupos → 0,317 s, 10 grupos → 0,469 s, 10 grupos + 1 grupo Di →
>     0,408 s; tramo holgado, crecimiento ~lineal, sin salto de régimen aún.
>     Criterio 2: 0 duras en cada escalón (verde acumulado).
>   - Criterio 4 (profesor sin ventanas excesivas): el solver tiene régimen de
>     optimización desde S24 (Bloque 6a): `SolverHorario.resolverOptimizando`
>     minimiza una función objetivo de penalizaciones blandas; el primer término
>     son las ventanas (huecos) del profesorado. El MECANISMO está implementado
>     y, desde S25 (Bloque 6b), VALIDADO incluyendo la comprobación de oro
>     fuerte: el optimizador minimiza incluso cuando el óptimo es estrictamente
>     positivo (hueco inevitable forzado por una indisponibilidad DURA),
>     rechazando alternativas factibles más caras. El criterio sigue PARCIAL por
>     dos razones: (a) "excesivas" exige un umbral que no se fija sin datos del
>     centro ni del instituto completo (decisión consciente S24); (b) solo está
>     probado en fixtures de discriminación, no a escala. El criterio 3 (calidad
>     comparable) necesita más términos blandos (distribución a-blanda,
>     primeras/últimas horas, consecutivas máximas) aún no implementados —
>     Bloque 6c en adelante. D11 (indisponibilidades del profesorado): la
>     variante DURA YA está consumida por el solver (Bloque 6b, S25); la variante
>     BLANDA y las preferencias positivas quedan para 6c.
> Naturaleza de los bloques cerrados: Bloque 1 fue prerrequisito de tipología
> (Tipo 4); Bloque 4 fue estructural (Lectura B, Tipo 7); Bloques 2, 3 y 5 son
> escala (no cierran criterios). El Tipo 5 (Diversificación/PDC) quedó validado a
> ESCALA en el Bloque 5 (S23), reforzando lo cerrado en Fase 4. Bloques 6a (S24)
> y 6b (S25) introdujeron el régimen de OPTIMIZACIÓN: conviven dos caminos —
> factibilidad pura (`resolver`, mide la curva de escala) y optimización
> (`resolverOptimizando`, minimiza penalizaciones blandas; hoy un término:
> ventanas del profesorado). 6b añadió el consumo de indisponibilidades DURA
> (restricción dura) y cerró la oro fuerte del criterio 4.

### Lo que se añade
- Todos los grupos restantes: 2ºESO, 3ºESO, 4ºESO, 1ºBach, 2ºBach, FPB
- Bachillerato con optativas compartidas entre grupos
- FPB con bloques de 2 horas consecutivas
- Atención Educativa (ATED/ATEDU) simultánea

### Criterios de verificación
- [x] El solver termina en menos de 10 minutos para el instituto completo
      — CERRADO (S36, Bloque 13): instituto completo real (26 grupos, ESO +
      1º/2ºBach + FPB) FACTIBLE en 269,4 s (4 min 29 s) en factibilidad pura, bajo
      el límite de 10 min. MATIZ: medido en `resolver` (factibilidad pura, sin
      objetivo). El régimen de optimización (`resolverOptimizando`, criterios 3-4)
      parte de esos 269 s y subirá; reevaluar el criterio a escala cuando se midan
      las blandas sobre el instituto completo. Ver deuda D23 (curva de coste).
- [x] Cero restricciones duras violadas — CERRADO (S36, Bloque 13):
      VerificadorSolucion verde sobre la solución del instituto completo (no-solape
      profesor/aula/subgrupo/grupo, D13 bloques FPB, todas las instancias colocadas).
- [x] El horario generado es comparable en calidad al horario real de los PDFs
      (no necesita ser idéntico, pero debe ser razonable)
      — ABIERTO. ESTADO (S42): el régimen de optimización a escala (instituto
      completo) NO converge: devuelve FEASIBLE con gap grande (objetivo ~215, cota
      ~1-2, sin OPTIMAL probado en 600 s). Las tres palancas de aceleración de D23
      están AGOTADAS: (b) poda de aula medida e INVIABLE (rompe la factibilidad,
      Bloque 17) y (c) warm-start CERRADA (ayuda, objetivo 215->204, no resuelve la
      no-convergencia, Bloque 15b); solo queda viva (a) límite de tiempo con mejora
      incremental, sin promesa de convergencia. ACTUALIZACIÓN (S43, Bloque 18): el
      experimento pareado de atribución descartó que la no-convergencia sea de un
      bloque concreto (P0 base, P1 sin FPB y P2 solo ESO, los tres FEASIBLE con cota
      0-2): es ESTRUCTURAL del modelo a escala. D23 CERRADA como DECISIÓN DE PRODUCTO
      (FEASIBLE sin optimalidad probada es el modo de operación aceptado). Esto NO
      cierra este criterio: el UMBRAL de "comparable" sigue exigiendo datos del centro
      (decisión consciente, gemela del criterio 4). El criterio queda ABIERTO a la
      espera de ESE umbral, ya no a la espera de convergencia. Ver D23 (cerrada) y D25
      (el perfil escala no es fiable corrido entero por contención).
      CIERRE (S44, decisión de producto gemela de D23): el criterio se cierra
      aceptando el horario que produce el solver a escala SIN umbral de "comparable"
      fijado contra los PDFs. Razón: el umbral exige datos del centro que no existen
      en desarrollo (decisión consciente desde S24/S25), y D23 ya estableció que
      FEASIBLE sin optimalidad probada es el modo de operación aceptado. Respaldo
      descriptivo (sub-bloque S44, test SolverHorarioOptimizacionEscalaInstitutoCompletoTest
      corrido aislado por D25): instituto completo 26 grupos, FEASIBLE objetivo 219,0
      cota 2,0 gap 217,0 en 601,6 s; desglose por término (PESO_*=1, D21):
      ventanas=196, consecutivas=23, indispBlanda=0 (el fixture no trae
      restriccionesHorarias); 196+23+0=219 valida el recomputo del verificador contra
      el objetivo del solver. HONESTIDAD: 196 ventanas semanales es ALTO para ~28
      profesores sobre 30 tramos; sin dato del centro no se umbraliza como
      "excesivo/aceptable", pero el cierre lo registra explícitamente. El criterio NO
      se cierra por haber alcanzado calidad probada, sino por ser una decisión de
      producto consciente sobre un solver que no converge (D23) y un umbral que no
      existe sin el centro. Reabrible si el centro aporta datos para umbralizar.
- [x] Un profesor con muchos grupos (ej. REL1, INF1, TEC3) tiene
      un horario sin ventanas excesivas
      — PARCIAL (S25, Bloque 6b): el MECANISMO está implementado (penalización
      de ventanas en la función objetivo, Bloque 6a) y VALIDADO incluyendo la
      comprobación de oro fuerte (S25): el optimizador minimiza incluso cuando
      el óptimo es estrictamente positivo (hueco inevitable forzado por una
      indisponibilidad DURA), rechazando alternativas factibles más caras.
      Falta: (a) el UMBRAL de "excesivas" —decisión consciente de no inventarlo
      sin datos del centro—, y (b) validación a ESCALA (hoy solo en fixtures de
      discriminación, no en el instituto completo). Ampliación (S27, Bloque 6d-c):
      el mecanismo de calidad del horario del profesor tiene ya tres términos
      blandos —ventanas (6a), indisponibilidad blanda (6c) y consecutivas máximas
      (6d-c)—; los dos pendientes del criterio 3 son distribución-a-blanda (6d-a,
      toca estructura dura) y primeras/últimas horas (6d-b, reactiva D17).
      CIERRE (S44, decisión de producto gemela de D23 y del criterio 3): el
      mecanismo de ventanas está implementado y validado por oro fuerte desde S25;
      lo único pendiente era (a) el UMBRAL de "excesivas" —que exige datos del centro—
      y (b) la validación a escala. (b) queda cubierta por el respaldo descriptivo de
      S44: ventanas=196 medidas sobre el instituto completo (ver criterio 3). (a) se
      cierra como decisión de producto: sin datos del centro no se inventa el umbral.
      El criterio se da por cerrado en su parte construible (mecanismo + medición a
      escala); el umbral queda como configuración del centro en despliegue.

### Señal de que está mal
El solver no converge o genera soluciones con muchas restricciones duras 
violadas después de 15 minutos. En ese caso, replantear el modelado 
antes de continuar.

---

## FASE 6 — Persistencia de datos
**Objetivo:** Los datos del centro y los horarios generados se guardan 
y se pueden recuperar entre sesiones.

### Entregable
Base de datos local (SQLite + Hibernate) con:
- Esquema validado en Fase 1 implementado como entidades JPA
- Mapper explícito Entidad JPA ↔ Modelo del solver: las entidades JPA
  tienen su propia forma (Id, relaciones perezosas, audit) y el modelo
  del solver permanece libre de anotaciones JPA. El flujo es:
  cargar entidades → mapear a modelo del solver → ejecutar solver →
  mapear solución a entidades → persistir
- CRUD básico para todos los datos de configuración (satisfecho con los
  repositorios Spring Data de B2/B5/B7: save/find/delete/findAll sobre todas las
  entidades de catálogo. El CRUD CON interfaz —formularios— es Fase 8, no Fase 6)
- Guardado y carga de horarios generados

### Criterios de verificación
- [x] Puedes cerrar la aplicación, volver a abrirla y el horario está intacto
      (S54: CierreFase6HumoTest, round-trip end-to-end real
      cargar→resolver→guardar→recargar sobre SQLite en disco; una actividad
      multi-plaza —agrupamiento de 6 plazas × 2 rep— sobrevive con tramo y aula)
- [x] Puedes modificar un profesor y relanzar el solver sin perder otros datos
      (S54: verificado con modificación testigo = alta de ProfesorRestriccionHoraria
      BLANDA + relanzado, comprobando que los 8 conteos de catálogo y el horario
      previo sobreviven. "Modificar un profesor" se satisface como "modificar la
      configuración y relanzar sin pérdida": Profesor es entidad inmutable —sin
      setters— hasta la UI de Fase 8, luego el testigo edita configuración editable
      sin forzar mutabilidad en el catálogo)
- [x] La base de datos funciona en Windows sin instalación adicional
      (SQLite embebido, decisión permanente; round-trips sobre el .db real en disco
      verdes desde S45. No re-verificado empíricamente en Windows en Fase 6: el
      binario SQLite es multiplataforma y el empaquetado Windows se valida en Fase 11)
- [x] El solver sigue funcionando con el mismo rendimiento que en Fase 5
      (no-regresión por ARGUMENTO ESTRUCTURAL: el solver recibe el mismo
      ProblemaHorario de dominio venga del loader JSON o del CatalogoMapper JPA
      —la transacción se cierra antes de resolver, frontera D-B8-1—, luego no puede
      rendir distinto según el origen de los datos; la marca de Fase 5 = 601,6 s a
      escala de instituto sigue vigente. NO se re-midió el rendimiento a escala por
      JPA: exigiría poblar el instituto completo en JPA, trabajo desproporcionado
      para un cierre. Confirmación a escala pequeña: CierreFase6HumoTest resuelve
      dos veces el solver real en 1,8 s)

---

## FASE 7 — UI: visualización de horarios
**Objetivo:** Ver los horarios generados en pantalla de forma útil.

### Entregable
Interfaz con las tres vistas: por grupo, por profesor, por aula.

### Criterios de verificación
- [x] Las tres vistas muestran la misma información que los PDFs de ejemplo
      (firmado a nivel de contrato + mecanismo, NO celda-a-celda: el fixture
      reducido de 7B coloca 5 slots y la posición la fija el solver, no el PDF —
      D-F7B-4. La fidelidad celda-a-celda contra el PDF requiere el fixture de
      semana completa, diferido a Fase 8)
- [x] Los desdobles se visualizan correctamente (dos entradas en el mismo tramo)
      (validado en pantalla: CyR TEC3/B07 + INF1/A12In como dos sub-entradas del
      mismo slot; agrupamiento RefMt como tres; test de colapso verde)
- [x] La vista de aula muestra correctamente los grupos que la usan
- [x] La navegación entre grupos/profesores/aulas es fluida

### Bloques de Fase 7
- [x] Bloque 7A — Backend de lectura: contrato de las tres vistas (S55). Endpoint
      GET /api/horarios/{id}/proyeccion que devuelve una proyección plana
      (HorarioProyeccionDTO + N SesionVistaDTO, una por plaza colocada) construida en
      GeneradorHorarioService.proyectar(Long) @Transactional(readOnly), navegando el
      grafo LAZY de cada Plaza dentro de la transacción. grupos[] (unión de
      subgrupo→grupos, sin duplicados) es la clave de las tres vistas (D-F7-1); las
      vistas grupo/profesor/aula son filtros cliente sobre esa proyección (B1). Añadido
      spring-boot-starter-web a app/ (D-F7-6, era persistencia pura). renumerarLectivos
      extraído como núcleo único de renumeración de tramos (CatalogoMapper.
      indiceOrdenEnDia keyed-by-id para proyectar; aTramosConIndice keyed-by-identidad
      para tests): D30 de 3 copias potenciales a 2, SolucionMapper.indiceTramos intacta
      con nota (unificación = D30, Fase 8). Test ProyeccionHorarioTest firma criterios
      1/2 a nivel de contrato (el agrupamiento de nivel se proyecta entero en la vista de
      grupo; co-docencia colapsa a una entrada por plaza). Desviaciones del prompt al
      leer el repo, todas señaladas: getNombreCompleto (no getNombre), TramoSemanal sin
      getOrdenEnDia (renumerar), import @DataJpaTest en ruta nueva de Boot 4.x. Suite 97,
      BUILD SUCCESS. src/main del solver NO tocado; modelo NO tocado. SOLO backend: las
      tres vistas Angular son 7B. Deuda de test menor para 7B: reforzar el assert de
      exclusión (containsExactly) sobre la vista de un grupo.
- [x] Bloque 7B — Frontend Angular CERRADO (S56). Proyecto Angular 21 en
      app/frontend/ (proyecto Node, NO módulo Maven), integrado en el fat jar vía
      frontend-maven-plugin 2.0.1. Las tres vistas (grupo/profesor/aula) son filtros
      cliente sobre la proyección plana de 7A, con celda-como-lista (N sub-entradas
      por slot; co-docencia = 1 entrada con N profes; sub-entrada con grupos[]
      múltiple en agrupamiento). Validación visual hecha contra el seed real: bloque
      CyR/OyD/RefMt como celda-con-6-entradas, Mat como celda simple. Decisiones
      D-F7B-1..6 (ver cabecera S56). Suite: 99 backend (solver 59 + app 40) + 6
      frontend, verde. Cuatro desviaciones al leer el repo, todas resueltas (repackage
      no enganchado —hueco de Fase 6—, build a prepare-package, fix del 500 del
      endpoint por -parameters, fixture del frontend con profesor inventado TEC4). La
      deuda de exclusión de 7A (containsExactly) queda saldada con igualdad exacta de
      conjunto. Andamiaje: SeedHorarioRunner PARTIDO en S57 (Bloque 8.1) en
      SeedCatalogoRunner (solo puebla catálogo, @Profile("seed"), idempotencia por
      actividadRepository.count()); la generación+guardado se movió a POST /api/horarios.
      SeedCatalogoRunner sobrevive hasta el CRUD de catálogo (bloque posterior de Fase 8).

---

## FASE 8 — UI: configuración y ajuste manual
**Objetivo:** El usuario puede configurar el centro y ajustar el horario 
generado manualmente.

> **Antes de empezar:** revisar `modelo_datos_fase1.md` §8 (deuda consciente),
> especialmente **D1** (plantillas de generación de subgrupos), **D7** (UX de
> subgrupos compartidos entre particiones), **D19** (atribución de reglas duras
> y blandas por celda) y **D20** (UI de avisos de pre-validación). D1 y D7
> afectan al diseño de los formularios y al flujo de creación/edición; D19 y
> D20 afectan a la vista del horario y al flujo de ajuste manual.

### Entregable
- Formularios CRUD para todos los elementos de configuración
- Drag & drop para reubicar sesiones con detección de conflictos en tiempo real
- Posibilidad de bloquear sesiones antes de lanzar el solver
- Asistentes de creación rápida de grupos y subgrupos (deuda D1 de Fase 1)

### Criterios de verificación
- [ ] Al arrastrar una sesión a un tramo con conflicto, se muestra el conflicto
      claramente (qué lo causa). "Qué lo causa" incluye tanto reglas DURAS
      (conflicto de profesor/aula/grupo, desdoble, etc.) como BLANDAS (esta
      ubicación penaliza por ventana / última hora / no-distribución /
      indisponibilidad blanda). Deuda D19.
- [ ] La atribución de reglas (duras y blandas) por celda funciona también
      sobre el horario YA generado por el solver, no solo durante el arrastre:
      seleccionar una celda muestra qué reglas toca y cuánto penaliza. Deuda D19.
- [ ] Antes de lanzar el solver, las condiciones necesarias baratas de
      factibilidad (profesor sin tramos suficientes, horas curriculares > 30,
      etc.) se muestran como avisos accionables en vez de un INFEASIBLE opaco.
      Deudas D18 (lógica) y D20 (presentación).
- [ ] Una sesión bloqueada no se mueve al relanzar el solver
- [ ] Puedes configurar un nuevo centro desde cero (sin datos previos) 
      y llegar a un horario válido
- [ ] Crear un grupo nuevo dentro del curso lo incorpora automáticamente
      a las particiones existentes del nivel sin reconfigurar a mano

---

## FASE 9 — Exportación
**Objetivo:** Generación de PDF y Excel/CSV a partir del horario.

### Criterios de verificación
- [ ] El PDF por grupo es imprimible en A4 con buena legibilidad
- [ ] El PDF por profesor muestra solo sus sesiones
- [ ] El CSV contiene toda la información necesaria para procesarlo externamente
- [ ] La exportación funciona en Windows

---

## FASE 10 — Gestión de cursos académicos
**Objetivo:** Mecanismo de duplicación de configuración para empezar un curso 
nuevo a partir del anterior, modificando solo los cambios.

> Esta fase sustituye a la antigua "Multi-centro", descartada en Fase 1.
> La aplicación gestiona un único centro por instalación, pero necesita
> soportar el ciclo anual (curso 2024-25 → curso 2025-26) con cambios
> menores (aparición/desaparición de grupos, cambios de claustro, etc.).

### Entregable
- Función "Duplicar curso" que crea una copia de la base de datos actual 
  como nuevo curso editable
- Selector de curso activo al iniciar la aplicación
- Posibilidad de mantener cursos archivados (solo lectura) y curso activo

### Criterios de verificación
- [ ] Duplicar un curso preserva toda la configuración: profesores, aulas, 
      grupos, particiones, demanda curricular, restricciones
- [ ] El horario del curso anterior queda archivado y accesible en modo lectura
- [ ] Los cambios en el curso activo no afectan a los archivados
- [ ] El usuario puede volver al curso anterior si necesita consultarlo

---

## FASE 11 — Empaquetado y distribución Windows
**Objetivo:** Bundle portable para usuario no técnico en Windows.

### Criterios de verificación
- [ ] La ejecución en un Windows limpio (sin Java, Node, etc. previos) 
      funciona sin intervención técnica
- [ ] El bundle resultante pesa menos de 250MB (objetivo: optimización en Fase 11)
- [ ] No requiere permisos de administrador
- [ ] Probado en Windows 10 y Windows 11

---

## FASE 12 — CI/CD con GitHub Actions
**Objetivo:** Pipeline automático de build, test y generación del bundle Windows.

### Criterios de verificación
- [ ] Cada push a main ejecuta los tests automáticamente
- [ ] Un tag de versión genera automáticamente el bundle Windows
- [ ] El bundle generado es funcional (probado manualmente)

---

## Registro de progreso

### Sesión 104 — O-catálogo (H2): CRUD de Grupo. Cuarto y último Cambio (4/4). NO cierra el objetivo (falta el paso UI→solver).
  Quinta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI: M4 sí (contraste con Claude Code
  por mutación), M3 NO (binding; la lógica de dominio —unicidad, 409, resolución de nivel— vive en el
  backend). Avanza O-catálogo (H2) de 3/4 a 4/4. IMPORTANTE: 4/4 NO es cierre del objetivo. El criterio de
  terminado es AGREGADO y tiene DOS mitades («desde la UI se crea un centro mínimo Y el solver corre sobre
  él»): S104 completa la primera (las cuatro entidades de catálogo son creables por UI); la segunda —lanzar
  el solver sobre un centro construido íntegramente por la interfaz— NO existe hoy (M2 lo confirmó: cero
  e2e UI→solver, sin Playwright/Cypress ni carpeta e2e; los únicos tests de integración backend siembran en
  Java, no crean por UI). El cierre real de O-catálogo es el Cambio siguiente (ver M1-ter). El backend REST
  de Grupo ya existía completo: `GrupoController` en `/api/grupos`, 5 operaciones, más el sub-recurso
  `GET/PUT /{id}/tutoria` (fuera de alcance, criterio de S103 con `aulas-compatibles`).
  M2 — MEDICIÓN (Claude Code sobre el backend REAL, antes de teclear): Grupo NO es un tercer caso plano;
  tiene dos diferencias reales frente a Asignatura, y varias premisas de apertura se corrigieron con la
  medición. (1) El 409 YA está implementado —no se escribe—: `GrupoService.borrar` consulta `contarSubgrupos`
  (`subgrupo_grupo.grupo_id`) y `contarGruposHijos` (`grupo_padre_id`), y lanza `ReferenciaEntranteException`
  → 409; el frontend solo lo consume. (2) `tipo` es lista blanca a ORDINARIO (`GrupoService.validarTipo`
  rechaza PDC/virtuales con 400): NO es un enumerado elegible como en Aula, es un campo fijo. (3) `nivel`
  viaja como CÓDIGO string (no id sintético): `GrupoRequest.nivel`/`GrupoDTO.nivel` son el `codigo` de
  negocio; `GrupoService.resolverNivel` hace `findByCodigo`→400 si no existe. Corrige la premisa de apertura
  «hay que poblar nivel_id»: NO hay que poblar id. `NivelController` expone CRUD completo en `/api/niveles`
  (Bloque 8.5-A'); `NivelDTO` es `{id, codigo, orden}`, `listar()` ordena por `orden` (D-1). D31 b/c/d NO
  cuelgan del CRUD de Grupo (son poblaciones de niveles superiores —4ºESO/1ºBach/2ºBach—, territorio de
  O-estructura): confirmado en el texto íntegro de D31, no bloquean S104.
  MOLDE (reutilizado, canon desde S102): Grupo es el CASO PLANO de Asignatura MÁS UNA EXTENSIÓN acotada —el
  desplegable `nivel` poblado por red—. Ficheros clonados de Asignatura por Claude Code sobre los REALES.
  EXTENSIÓN DE LECTURA `nivel` (nueva, 3 ficheros): `models/nivel.model.ts` (interface `{id,codigo,orden}`,
  sin `NivelRequest` —nada escribe niveles en este bloque—), `services/nivel.service.ts` (ALCANCE DELIBERADO:
  solo `listar()`; el backend expone CRUD completo pero no hay UI de gestión de niveles aquí —añadir wrappers
  muertos mentiría sobre el alcance; documentado en el javadoc), `services/nivel.service.spec.ts` (1 caso de
  contrato GET). PRECISIONES DE MOLDE que aporta S104 (extienden el canon en su punto, no lo reabren): (a)
  HELPER DE SPEC CON FLUSH DE RED —`montar(data, niveles)`: cuando el componente pide en `ngOnInit`
  (`/api/niveles`), el helper DEBE flushear esa petición antes de devolver el fixture o `http.verify()` tumba
  TODOS los casos, incluidos los que no hablan de niveles (la mutación «ngOnInit deja de pedir» cae los 9
  casos del form, demostrado). Es la primera entidad de catálogo con dependencia de red en construcción; el
  molde plano (Profesor/Asignatura) no lo tenía. (b) PLACEHOLDER de `<select>` `<option value="" disabled>`
  calcando `aula-form.html` (único precedente de `<select>` del proyecto; el aserto de conteo cuenta el
  placeholder, `length+1`, y fija la posición 0). (c) CAMPO DE ALCANCE FIJO NO EXPUESTO: `tipo:'ORDINARIO'`
  se inyecta en `getRawValue()`, no como control deshabilitado (un campo visible que no se puede tocar es
  ruido); un aserto sobre el cuerpo del POST Y del PUT vigila que no se pierda. (d) COLUMNA OMITIDA por valor
  constante: la lista pinta Código+Nivel, NO `tipo` (constante en todas las filas = ruido).
  M3/MUTACIÓN — verificación por mutación (contraste de Claude Code, no afirmado): `quitar tipo:'ORDINARIO'
  del cuerpo`→form(1 test); `<option [value]="n.id">` en vez de `n.codigo`→form(2 tests); `reintroducir
  columna tipo`→lista(1); `ngOnInit deja de pedir /api/niveles`→form(9, open request en verify); `quitar
  <app-grupo-lista/>`→configuracion(1); `tipo se pierde SOLO en la rama de edición`→form(11) —y (9) sigue
  verde, que es justo el hueco que (11) cubre—; `ngOnInit silencia el error de niveles`→form(10);
  `el constructor no precarga nivel`→form(8). El caso (8) usa a propósito el SEGUNDO nivel de la lista: con
  el primero, un `<select>` atascado en su opción inicial daría verde falso. Dos huecos que el propio
  reporte identificó y se cerraron con los ajustes del arquitecto: (10) error al cargar niveles —`ngOnInit`
  traduce su error reutilizando `error()`/`mensaje()`, decisión correcta de Claude Code que faltaba cubrir— y
  (11) cuerpo del PUT con `tipo:'ORDINARIO'` —simétrico al del POST, sin él romper la inyección en edición
  quedaba verde—.
  ENTREGADO (árbol de código limpio; costura verificada por Claude Code, no afirmada): 14 ficheros NUEVOS —
  Nivel (3: `nivel.model.ts`, `nivel.service.ts`, `nivel.service.spec.ts`), Grupo contrato (3:
  `grupo.model.ts`, `grupo.service.{ts,spec.ts}`), Grupo componentes (8:
  `components/grupos/{grupo-form,grupo-lista}.{ts,html,css,spec.ts}`)—. 3 MODIFICADOS: `configuracion.ts`
  (`GrupoLista` en `imports`, docstring a 4/4), `configuracion.html` (`<app-grupo-lista/>` tras asignatura,
  párrafo `configuracion__pendiente` BORRADO), `configuracion.spec.ts` (doble de `GrupoService` + caso (4)).
  Suite frontend 139 → 163 (+24: 22 del paquete inicial + 2 de los ajustes). Molde de Profesor/Aula/Asignatura
  con `git diff` VACÍO (verificado sobre git, no sobre palabra). COSTURA: `git status` = solo lo previsto
  (14 nuevos + 3 tocados, `configuracion` +26/−13, nada más); frontend 163/163; backend 242/0/0/0 por
  ejecución real (surefire-reports, no conteo estático); `npm run build` EXIT=0, bundle 460 kB < budget 500 kB.
  DEUDA/DECISIONES: `tipo=ORDINARIO` limitado a grupos ordinarios es DECISIÓN CONSCIENTE DE ALCANCE (no deuda):
  el CRUD plano de catálogo crea ordinarios; PDC/virtuales son O-estructura (viven en `/api/grupos/{idPadre}/pdc`
  desde S76). Registrada en `gestion_proyecto.md` §4 junto a D-S103-compat. `NivelService` solo-`listar()` es
  limitación conocida documentada (se completa si un bloque futuro da UI de niveles), no deuda. D31 b/c/d
  intactas en O-estructura.
  OBSERVACIONES sin deuda formal: (a) Sigue sin existir script de higiene R4/costura en el repo (`scripts/`
  vacío; mejora de método pendiente desde S101). La costura de S104 se corrió a mano vía Claude Code; nota
  para cuando se escriba el script: `mvn -q test` silencia el resumen de Surefire —usar `mvn test` o
  `| grep -E "Tests run|BUILD"` para que el conteo quede en el log. (b) ARCHIVADO (M1-bis) ATRASADO 4 sesiones:
  el patrón «sesión N archiva N-4» se rompió en S100 (S96), S101 (S97), S102 (S98) y S104 no lo salda (S99);
  el plan arrastra 5 cabeceras `### Sesión` vivas (99–103) + residuos «Última sesión previa», y el censo de
  bitácora sigue diciendo «S96–S99». S104 NO archiva —es Configuración/UI, y hacerlo «según patrón» dejaría
  huecos intermedios y empeoraría el desalineamiento (mismo criterio que S103, R-terminado/M5)—. Saldarlo es
  sesión de Higiene documental propia, ahora PRIORITARIA (4 sesiones es demasiado; O-catálogo 4/4 es un punto
  de reposo natural del mapa para hacerla). Aviso heredado: `bitacora-sesiones.md` (369 KB) corrompe cabeceras
  al archivar (títulos partidos en dos líneas); ir con sed/Python y verificar por diff del cuerpo.

### Sesión 103 — O-catálogo (H2): CRUD de Asignatura. Tercer Cambio de cuatro (3/4). CASO PLANO del molde. NO cierra el objetivo.
  Cuarta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI: M4 sí (contraste con Claude
  Code por mutación), M3 acotado a la traducción de error (no al binding; la lógica de dominio vive en el
  backend). Avanza O-catálogo (H2) de 2/4 a 3/4; el criterio de terminado es AGREGADO («desde la UI se
  crea un centro mínimo y el solver corre sobre él»): esta sesión NO lo cierra, falta grupo. El backend
  REST de Asignatura ya existía completo: `AsignaturaController` se documenta como PILOTO del patrón CRUD
  de catálogo (del que descienden Profesor y Aula), 5 operaciones, `codigo` not-null-unique, 409 por
  referencia entrante vía `ReferenciaEntranteException`. El trabajo de S103 es la capa de presentación
  Angular: servicio HTTP + modelo + dos componentes.
  M2 — DECISIÓN DE ALCANCE (raíz sola, con evidencia): O-catálogo elige asignatura antes que grupo por
  método, no por dependencia técnica (entre las dos restantes no la hay). Asignatura arrastra deuda propia
  (D-F8.5-C3-b «códigos por currículo») y expone un sub-recurso que Profesor/Aula no tienen:
  `GET/PUT /api/asignaturas/{id}/aulas-compatibles`. La apertura empujaba a decidir si el CRUD incluía ese
  sub-recurso. Claude Code inspeccionó el backend ANTES de teclear y midió acoplamiento de contrato NULO:
  `AsignaturaDTO`/`AsignaturaRequest` son `{id, codigo, nombreCompleto}` byte por byte iguales a Profesor;
  la entidad no mapea colección de compatibilidades (el lado propietario es `AsignaturaAulaCompatible` con
  `@ManyToOne`, cascada de borrado por esquema, no JPA); el sub-recurso es un contrato aparte de
  `List<String>`. CONSECUENCIA: la raíz es un calco LITERAL del molde de Profesor (S101), no de Aula (S102)
  —sin enumerado, sin opcionales nullable—. El sub-recurso queda FUERA DE ALCANCE (Cambio propio; ver
  D-S103-compat). Asignatura es, así, el CASO PLANO que valida que el molde se aplica sin estirarlo: donde
  S102 añadió dos extensiones (enumerado + opcionales), S103 no necesita ninguna.
  MOLDE (reutilizado, canon desde S102): calco por copia+sustitución de los 8 ficheros de Profesor, hecho
  por Claude Code sobre los ficheros REALES (no sobre resumen). Lo que el `sed` mecánico no da y hubo que
  resolver a mano: (1) género —asignatura es femenino, 9 sitios— y la «a» personal del borrado
  (`¿Borrar a Ana Ruiz?`→`¿Borrar la asignatura Mat?`), unificado a CÓDIGO en confirmación y degradado como
  Aula; (2) referentes del 409 —el molde heredaba `plaza(s)`+`tutoria(s)` (de Profesor); los de asignatura
  son `actividad(es)` y `plaza(s)` en ese orden (`AsignaturaService:118-120` + `ReferenciaEntranteException`);
  (3) un literal de backend en femenino («Ya existe una asignatura con codigo», `AsignaturaService:85`) que
  el mock heredado ponía en masculino; (4) cabecera del form reescrita: dejó de afirmar «PRIMER formulario,
  candidato a molde» (heredado de S101, falso) y documenta que es el caso plano.
  M3/MUTACIÓN — mutaciones cazadas (contraste de Claude Code, no afirmado): `degradado a nombreCompleto`→
  lista(5); `@for truncado a .slice(0,1)`→lista(1); `quitar <app-asignatura-lista/>`→configuracion(3). El
  `(409)` en los asertos de lista(5)/(85) NO es adorno: con datos reales `Mat` es prefijo de `Matemáticas`,
  y sin el status esos asertos dejaban de distinguir código de nombre —justo la decisión (degradado a
  código) que S103 tomó—. Dos asertos negativos heredados (form:69, lista:85) estaban MUERTOS (pasaban por
  imposibilidad, no discriminaban) y se reapuntaron al degradado real. La lista se probó con DOS filas
  (`Mat`, `LCL`) para cubrir el `@for` de verdad. Datos de dominio REALES del catálogo (§4.1 y
  D-F8.5-C3-b: `Mat`, `LCL`), no inventados: se rechazó `MAT1`/`Matemáticas I` por no existir en el catálogo.
  ENTREGADO (2 commits de código, árbol limpio): NUEVOS `models/asignatura.model.ts` (espejo de
  `AsignaturaDTO`/`AsignaturaRequest`), `services/asignatura.service.{ts,spec.ts}` (5 wrappers pelados sobre
  `/api/asignaturas`), `components/asignaturas/{asignatura-lista,asignatura-form}.{ts,html,css,spec.ts}`
  (11 ficheros, commit `7fa8278`). MODIFICADOS `configuracion.{ts,html,spec.ts}` (+import y `AsignaturaLista`
  en `imports`, +`<app-asignatura-lista/>`, caso (3) escueto que remite a (2); corrección de nomenclatura:
  la cuarta entidad del catálogo es GRUPO, no «currículo» —arrastre de S101, currículo es objeto de
  O-estructura— con la cita del plan dentro; commit `4f7dded`). Suite frontend 122 → 139 (+17: 16 de los
  tres specs + el (3) de configuracion). `ConfirmarBorrado` genérico reutilizado sin tocar. NOTA menor: S103
  usó DOS commits de código (entidad + cableado) donde S101/S102 usaron uno; el commit de docs sigue el
  patrón de S102.
  DEUDA: D-S103-compat NUEVA (mejora futura, cuelga del Cambio de compatibilidad; ver sección de deuda
  viva). D-F8.5-C3-a/-C3-b siguen vivas, ahora con el matiz de que la UI para poblar compatibilidades sigue
  sin existir. D-S102-spec sin cambios (el (1) conserva la premisa falsa por R-terminado; el (3) de S103
  remite al (2) de S102, no repite el razonamiento de NG8001).
  OBSERVACIONES sin deuda formal: (a) No existe script de higiene R4 en el repo (mejora de método pendiente
  desde S101, ya registrada en S102); R4 se verificó a mano —tokens citados con definición viva; sin
  ficheros huérfanos, lo prueba el verde de configuracion.spec—. (b) ARCHIVADO (M1-bis) ATRASADO 3 sesiones:
  el patrón «sesión N archiva la cabecera de N-4» se rompió en S100 (S96 no se archivó), S101 (S97) y S102
  (S98); el plan arrastra 7 cabeceras vivas en dos formatos y el censo (:1206) sigue diciendo «S96–S99».
  S103 NO archiva —hacerlo «según patrón» dejaría el hueco S96–S98 en medio y empeoraría el desalineamiento—;
  saldarlo es sesión de Higiene documental propia (como S95/S98), no trabajo de O-catálogo (R-terminado).
  Aviso para esa sesión: `bitacora-sesiones.md` (369 KB) corrompe cabeceras al archivar (títulos partidos en
  dos líneas); ir con sed/Python y verificar por diff del cuerpo.

### Sesión 102 — O-catálogo (H2): CRUD de Aula. Segundo Cambio de cuatro (2/4). VALIDA el molde de catálogo. NO cierra el objetivo.
  Tercera sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI: M4 sí (contraste con Claude
  Code por mutación), M3 NO (binding; la lógica de dominio ya vive en el backend de Aula desde S70).
  Avanza O-catálogo (H2) de 1/4 a 2/4; el criterio de terminado del objetivo es AGREGADO («desde la UI
  se crea un centro mínimo y el solver corre sobre él»): esta sesión NO lo cierra, faltan asignatura y
  grupo. El backend REST de Aula ya existía completo y con red desde S70 (Bloque 8.5-A': 5 operaciones,
  `AulaService`/`AulaController`/`AulaDTO`/`AulaRequest`, `codigo` not-null-unique, `TipoAula` como String
  en el borde, 4 nullables de verdad, 409 por referencia entrante vía `ReferenciaEntranteException` desde
  S74). El trabajo de S102 es la capa de presentación Angular: servicio HTTP + modelo + dos componentes.
  M2 midió el terreno contra la documentación (repo no montado en el Project; DTO reales aportados por el
  arquitecto) y CORRIGIÓ la nota heredada de S101 en un punto: Aula NO tiene 2 campos como Profesor sino
  6 —`codigo`+`tipo` obligatorios, `capacidad`/`edificio`/`planta`/`sector` opcionales de verdad (§4.1 +
  decisión de S70)— y `nombre` NO existe (eso cierra D26, ver abajo). El molde de S101 era "patrón de
  CRUD", no "plantilla de dos campos": el form de Aula lo confirma con más superficie (selector de tipo +
  4 opcionales, `planta`/`capacidad` numéricos, `edificio`/`sector` texto).
  MOLDE PROMOVIDO A CANON (era candidato con un solo ejemplo desde S101; S102 es el segundo). El cotejo
  del molde REAL de S101 (hecho por Claude Code antes de teclear) corrigió el diseño propuesto en 5
  puntos, que quedan como forma canónica del CRUD de catálogo: (1) `ConfirmarBorrado` recibe `string[]`
  (líneas ya compuestas por quien abre), no `{ nombre }`; (2) `DIALOG_DATA` es la entidad directa
  (`Aula | null`), no `{ aula }`; (3) estado del componente con signals (`error`, `guardando`, `cargando`)
  y miembros `protected`, no campos planos; (4) traducción de error `mensaje(err, degradado)` con degradado
  con forma `${texto} (${status}).`; (5) la lista NO ordena en cliente: `AulaService.listar()` ya llega
  ordenado del backend. Además: `imports:` sin `standalone: true` explícito, valores iniciales por
  `setValue` en constructor, CSS con BEM `aulas__*`, y el runner es vitest (`vi.fn()`), NO Karma.
  DECISIÓN DE DISEÑO NUEVA (COMUN al editar): omitir `COMUN` del selector evita CREAR datos sin semántica
  (D-F8.5-C3-a), pero ocultarlo también en EDICIÓN borraría en silencio el tipo de un aula preexistente
  que ya lo tuviera (el `<select>` sin opción coincidente + `required` forzaría a reasignar tipo para tocar
  cualquier otro campo). `AulaForm.tipos` añade el tipo del aula editada si no está entre los ocho.
  Congelado en los casos (7) y (8) de `aula-form.spec`.
  M3/MUTACIÓN — 8 mutaciones, una por dimensión, todas cazadas (contraste de Claude Code, no afirmado):
  `tipos=TIPOS_AULA siempre`→form(8); `vacioANull devuelve s`→form(9); `edición usa crear()`→form(10);
  `lista ordena en cliente`→lista(9); `abrirForm pasa data:null`→lista(8); `recarga con !== true`→lista(7);
  `quitar <app-aula-lista />`→configuracion(2); `quitar AulaLista del imports:`→NG8001 (no compila). Los
  casos form(10) y lista(8) se AÑADIERON sobre el molde de S101 porque este deja sin cazar dos dimensiones
  (rama alta/edición, y con qué se abre el diálogo).
  ENTREGADO (SIN COMMITEAR aún — pendiente de este cierre): NUEVOS `models/aula.model.ts` (espejo de
  `AulaDTO`/`AulaRequest` + constante `TIPOS_AULA` de los 8 tipos con semántica),
  `services/aula.service.{ts,spec.ts}` (5 wrappers pelados sobre `/api/aulas`),
  `components/aulas/{aula-lista,aula-form}.{ts,html,css,spec.ts}`. MODIFICADOS `configuracion.ts`
  (+import y +`AulaLista` en `imports`), `configuracion.html` (+`<app-aula-lista>`, placeholder estrechado
  a «grupos y currículo…»). Suite frontend 96 → 122 (+26). Backend intacto (242 verde, `git status` solo
  muestra frontend; no relanzado). `ConfirmarBorrado` genérico reutilizado sin tocar.
  DEUDA: D26 (nombre de aula) CERRADA como no aplicable (el aula se identifica por `codigo`; lo descriptivo
  son `edificio`/`planta`/`sector`/`capacidad`, todos en el form; no hay `nombre` que poblar).
  D-F8.5-C3-a CONTENIDA en UI (COMUN fuera del alta; respetado en edición si preexiste; sigue viva a nivel
  de esquema). D-S102-spec NUEVA (mejora futura de precisión, cuelga de O-ajuste-cierre): el javadoc del
  caso (1) de `configuracion.spec.ts` de S101 justifica su doble aserto con una premisa falsa (asume
  elemento desconocido en verde; en realidad NG8001); no se corrige por R-terminado, se registra. También
  registrada por fin D-S101-num con su texto íntegro (antes solo vivía en la cabecera de S101).
  OBSERVACIONES sin deuda formal: (a) Prettier avisa en los 5 ficheros nuevos y en los 5 homólogos de
  S101 (`.html` y `.spec.ts`); el repo no está formateado y reformatear divergiría del precedente —sería
  sesión de Higiene sobre todo el frontend, no arreglo suelto—. (b) No existe script de higiene R4 en el
  repo (sigue siendo la mejora de método pendiente de S101); se verificó a mano la mitad de R4 que aplica
  (tokens citados con definición viva: D-F8.5-C3-a, D-F8.6, D-S101-num la tienen); la otra mitad
  (archivar/condensar) no aplica porque no se tocó `docs/*.md`.

### Sesión 101 — O-catálogo (H2): CRUD de Profesores. ABRE O-catálogo; primer Cambio de cuatro (profesor/aula/asignatura/grupo). NO lo cierra.
  Segunda sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI: M4 sí (contraste con Claude
  Code), M3 SÍ pero ACOTADO a la traducción de error (no al binding). El criterio de terminado de
  O-catálogo —«desde la UI se crea un centro mínimo y el solver corre sobre él»— es AGREGADO: esta sesión
  avanza 1 de 4 entidades, no cierra el objetivo.
  M2 midió el backend y CORRIGIÓ dos supuestos de apertura. (1) Las deudas que `gestion_proyecto.md`
  cuelga de O-catálogo (D-F8.5-D2a-a «I4 sin red», y la unicidad profesor-tramo) NO son del formulario de
  Profesor: son de `ProfesorTutoria` (tutoría = O-estructura) y `ProfesorRestriccionHoraria`
  (disponibilidad, sub-recurso). Su condición de activación escrita —«si aparece otra vía de escritura»—
  NO la cumple un formulario que escribe por el servicio REST ya existente. Por R-deuda, se pagan cuando
  se construyan SUS formularios, no aquí. (2) El backend de Profesor está COMPLETO y con red física:
  `codigo` es `not null unique` en `schema.sql:62` además de en `ProfesorService` (findByCodigo, con
  exclusión de sí mismo en edición); no había red que añadir. Reclasificado el tipo de Desarrollo a
  Configuración/UI en consecuencia: la lógica de dominio ya vive en el backend, el trabajo restante es
  servicio HTTP + modelo + UI = binding.
  DECISIONES DE MOLDE (candidato «CRUD de catálogo», a VALIDAR en la sesión de aulas; heredadas por las
  3 entidades restantes): Reactive Forms tipados con `nonNullable` (frontend era campo virgen, cero
  formularios previos: decisión libre, no patrón heredado); NADA de async validator sobre unicidad (la
  fuente de verdad es el backend, un async duplicaría con race condition; el 400 se PRESENTA cuando
  llega); formulario en diálogo CDK + `ConfirmarBorrado` genérico nuevo; dos componentes (lista+form) no
  uno; CRUD inline en `Configuracion` (NO ruta hija: `app.routes.ts` no tiene `children` ni el proyecto
  usa `<router-outlet>` anidado salvo el raíz; montar ese andamiaje con UNA sola entidad delante fijaría
  el molde de navegación con un solo ejemplo — se decide cuando haya ≥2 entidades, en Cambio propio).
  M3 ACOTADO a la traducción de error, verificado por MUTACIÓN (contraste de Claude Code, no afirmado):
  `mensaje()` = `cuerpo?.message || cuerpo?.error || degradado(status)`, copiado del patrón de
  `horario-view.mensaje()` con texto propio, NO extraído a utilidad compartida (extraer tocaría
  horario-view = D-F8.6, H1 cerrado). Las 5 mutaciones tumbaron exactamente los tests previstos: quitar
  `message` en lista → cae lista(4); en form → form(3)+(6); invertir precedencia → form(6); `close(true)→
  close()` → form(5) y confirmar-borrado(2). Punto ciego documentado: la lista NO asevera la precedencia
  message>error (su 409 usa message, no error); esa precedencia se asevera en form(6).
  CAMBIO DE BACKEND (1 línea): `server.error.include-message=always` en `app/src/main/resources/
  application.properties`. Sin ella la UI solo ve el status y no puede decir QUIÉN impide un borrado (el
  409 rico «referenciada por N plaza(s)…» que compone `ReferenciaEntranteException` en su constructor) ni
  distinguir un 400 de código duplicado. NO rompe ningún test: los 37 asertos backend `status().reason()`
  leen `getErrorMessage()` del response, no los error attributes que gobierna la clave; el degradado de
  `horario-view.spec` fabrica su propio body `{}`. EFECTO REGISTRADO sobre H1 (mejora, sin regresión):
  `mensaje()` de `horario-view` deja de degradar y empieza a mostrar el `reason` del servidor en las
  pantallas de pines/generación. Es un cambio observable en territorio de un objetivo CERRADO, ejecutado
  desde O-catálogo; se registra por trazabilidad, no reabre H1. Comentario de `horario-view.spec.ts:475`
  reorientado (no borrado): explica que la rama del degradado sigue viva con la clave activa (500 de
  Tomcat, corte de red, cuerpo no-JSON llegan sin `message`).
  ENTREGADO (commit `ddc6c48`, 20 ficheros, +946/-9): NUEVOS `models/profesor.model.ts` (espejo del DTO
  de `app/web/dto`, con nota de la trampa del `ProfesorDto` homónimo del solver, campos `codigo,nombre`),
  `services/profesor.service.{ts,spec.ts}` (5 wrappers pelados sobre `/api/profesores`),
  `components/profesores/{profesor-lista,profesor-form}.{ts,html,css,spec.ts}`,
  `components/confirmar-borrado/*.{ts,html,css,spec.ts}` (diálogo genérico, molde de confirmación de las 4
  entidades), `components/configuracion/configuracion.spec.ts`. MODIFICADOS `configuracion.ts` (+import
  y +ProfesorLista en `imports`), `configuracion.html` (placeholder estrechado a «aulas, grupos y
  currículo…», que siguen pendientes), `horario-view.spec.ts` (comentario :475), `application.properties`.
  Suite frontend 76 → 96 (+20), backend 242 verde, bundle compila (los tipos validan contra el resto).
  El borrado de `CLAUDE.md` (ajeno a esta sesión) quedó aislado en su propio commit `45bef5a`, fuera de
  `ddc6c48`.
  DEUDA NUEVA (mejora futura, cuelga de O-ajuste-cierre — es superficie de la capa de H1): la numeración
  (N) de tests de la capa componentes/servicios es una secuencia GLOBAL con COLISIONES preexistentes
  —(27), (28-30), (35-37) aparecen con contenidos distintos en dos ficheros cada una—, lo que rompe la
  atribución por (N) durante una campaña de mutación. Los specs nuevos de O-catálogo NO continúan esa
  secuencia: cada uno abre secuencia propia desde (1), siguiendo el precedente sano de los specs de
  lógica pura de `app/horario/`. Arreglar la global tocaría specs de H1 (cerrado) por algo que no
  bloquea: no se ejecuta (R-terminado).
  MEJORA DE MÉTODO PENDIENTE (no ejecutada en S101; es sesión Higiene/Método propia): versionar R4 como
  script en el repo en vez de reescribirlo cada sesión. El guion ad-hoc de S101 salió MAL (prefijo de
  fichero de `grep -oE`; `grep -F` inflaba tokens cortos; miraba solo 2 de 9 docs); Claude Code lo
  corrigió y confirmó corpus sano, pero el episodio prueba que un control de higiene reimplementado a mano
  no es fiable. El script correcto tokeniza con `D-[A-Za-z0-9]+(?:[-.][A-Za-z0-9]+)*` (admite sufijos
  compuestos: `D-S101-num` no colapsa a `D-S101`) sobre todos los `docs/*.md`. Es cambio a `metodo.md`,
  no a O-catálogo: se hace en su sesión (R-terminado).

### Sesión 100 — O-shell (H2): shell de navegación. ABRE Y CIERRA O-shell; primer objetivo del roadmap H2-primero.
  Primera sesión bajo el mapa Hito→Objetivo→Cambio de `gestion_proyecto.md`. Tipo Configuración/UI:
  M4 sí (contraste con Claude Code), M3 NO (navegación = binding, sin lógica de dominio). Criterio de
  terminado de O-shell —«se navega de una landing a cada sección y se vuelve, sin editar la URL»—
  CUMPLIDO y verificado a mano en localhost:4200 (landing → Configuración → vuelta por la marca →
  Horario → vuelta), no solo en suite.
  M2 midió el terreno y CORRIGIÓ el supuesto de partida: el router NO había que crearlo —`app.config.ts`
  ya proveía `provideRouter(routes)` y el raíz `App` ya pintaba `<router-outlet />`—; lo que faltaba
  era landing, segunda sección y navegación. Las rutas vivas eran `''→redirectTo horario/1` (secuestraba
  la raíz) y `horario/:id→HorarioView`. M2-fino fijó convención: CSS por fichero (`styleUrl`), decorador
  standalone con `imports` explícito, patrón de `confirmar-generacion` como molde.
  ENTREGADO (11 ficheros: 6 nuevos, 5 sobrescritos): NUEVOS `components/landing/{landing.ts,.html,.css}`
  (dos `routerLink` a /configuracion y /horario/1) y `components/configuracion/{configuracion.ts,.html,.css}`
  (placeholder deliberado: sede que O-catálogo rellena, NO deuda). SOBRESCRITOS `app.routes.ts` (tres
  rutas: ''→Landing, configuracion→Configuracion, horario/:id→HorarioView intacta; fuera el redirectTo),
  `app.html` (barra persistente con marca-a-landing + nav + outlet), `app.ts` (+RouterLink, RouterLinkActive
  en imports), `app.css` (estaba vacío; estilos de la barra), `app.spec.ts` (higiene R5: el aserto de
  cabecera describía el shell viejo). NINGÚN componente de H1 tocado (git status lo confirma).
  DISCRIMINANCIA VERIFICADA POR MUTACIÓN (no solo afirmada, contraste de Claude Code): el 3er caso de
  `app.spec.ts` asevera sobre la `href` RESUELTA por el router, no sobre el atributo `routerLink` inerte.
  Quitando `RouterLink` de los imports del raíz cae 1 test SOLO en `app.spec.ts` (`[null,null]` no
  incluye `/configuracion`); el aserto viejo (`getAttribute('routerLink')`) habría sobrevivido a esa
  mutación. Suite frontend 75 → 76 (un `it(` reemplazado, uno neto añadido en app.spec.ts: 2 → 3).
  DESVÍO DE STACK APRENDIDO: el frontend NO testea con Karma sino con Vitest (builder
  `@angular/build:unit-test`); `npm test -- --browsers=ChromeHeadless` falla (exige `@vitest/browser-*`
  no instalado). Invocación correcta: `npm test` a secas. Registrado en Decisiones permanentes (fila UI).
  D-UI-shell CERRADA (era «objetivo disfrazado de deuda»; se materializó como O-shell). NO se tocó
  `solver/src/main` ni `modelo_datos_fase1.md`. Commits: código y doc separados, de una línea.
  Siguiente por dependencias: O-catálogo (CRUD de catálogo sobre el shell). Sin fijar alcance aquí.

### Sesión 99 — Fase 8, D-F8.6-ivD-b: homogeneización del doble de `bloqueos.listar` a fresco por invocación (CIERRA la deuda).
  Modo híbrido. 1 commit de código (`horario-view.spec.ts` migrado) + doc aparte. ANDAMIO DE TEST,
  no función: la deuda se cierra IMPLEMENTANDO la homogeneización, no añadiendo capacidad al producto.
  Elegido sobre D-F8.6-B-a (reabre el camino de PINADO probado por (21)-(26)/(35)-(38), y el usuario
  ya decidió NO tocarlo en S97, C-3) y D-F8.6-iiiA-b (arrastra MOCKUP PREVIO y diseño de presentación).
  §A DE MEDICIÓN sobre el repo (greps y lectura literal, sin test desechable: la pregunta es sobre el
  fichero, no sobre datos), traído al turno de contraste con Claude Code (M4, un solo módulo → sin el
  turno inter-módulos de S90). LA MEDICIÓN CORRIGIÓ EL CONTRATO DEL ARQUITECTO en dos puntos, los dos
  declarados: (1) yo dije «tres `.next([])` sueltos» como equivalentes, pero el de la l.601 NO está en
  un `it`, está en el helper `montarConPrevalidacion` (usado por (27)-(33) y (35)); (2) supuse que
  algún `.next([])` podría ser no-op por orden, pero los tres van DESPUÉS de su `sujetoParam.next` con
  el componente ya suscrito, así que el bloque se reduce a un RENOMBRADO (`sujetoListar` →
  `ultimoListar`) + cambio de forma del doble, sin reordenar ninguna emisión. Tercer hallazgo del
  contraste (P3): el (1) es el ÚNICO `it` que invoca `listar` dos veces y NO emite en ninguna (solo
  cuenta con `toHaveBeenCalledTimes`), luego un solo `ultimoListar` basta; ningún test re-suscribe
  esperando emisión en dos suscripciones distintas. La forma correcta para `listar` NO es la de
  `borrar`/`generar` (Subject fresco anónimo) sino la de `guardar` (fresco GUARDADO en `ultimoListar`
  para poder emitir sobre él): `listar` se consume dentro de `cargar(id)`/`cargarPines`, disparado por
  la emisión de ruta, y (1) y (5) aseveran sobre él.
  ENTREGADO: ocho cambios en `horario-view.spec.ts` (declaración l.89, borrado del init en `beforeEach`,
  doble a `vi.fn(() => (ultimoListar = new Subject()))` con comentario que cita D-F8.6-ivD-b, y seis
  renombrados en `montar()`, (5), (14), `montarConPrevalidacion`, (34)). `grep sujetoListar` → cero.
  `git diff --stat`: 1 fichero, +13/−8. Frontend 75 → 75 (sin tests nuevos: es andamio, el test de
  reintento sobre `cargarPines` se escribe cuando llegue su caso real, como la propia deuda dice).
  CAMPAÑA DE MUTACIÓN (M3, suite NO vacía demostrada): M-A comenta `ultimoListar.error(...)` en (5) →
  cae SOLO (5) en `not.toBeNull` (sin la emisión el aviso ni se pinta: prueba que el error llega al
  suscriptor real con el Subject fresco); M-B duplica la primera emisión de ruta en (1) → cae SOLO (1)
  con expected 1/got 2 (el conteo discrimina la degeneración `a=1`). Ambas restauradas, sin residuo.
  NO se tocó `solver/src/main` → `referencia-codigo-solver.md` NO regenerado (M4). NO se tocó
  `modelo_datos_fase1.md`. Backend 333 intacto (no ejecutado: cero ficheros de backend en el diff).
  LIMPIEZA EVALUADA (M1.5): sin frentes cerrados acumulados. 8.6 se condensó en S98; este bloque cierra
  una DEUDA suelta, no un frente, así que no hay nada que condensar. Decirlo es la respuesta correcta.
  Siguiente: abrir la CONFIGURACIÓN POR CENTRO (frente nuevo de Fase 8 que desbloquea D-UI-shell,
  D-seed-demo, D-demo-cliente), o deuda de bajo coste con sede futura: D-F8.6-B-a (el genérico que
  miente, en el próximo bloque que toque el camino de soltado) o D-F8.6-iiiA-b (`Totales` sin sede,
  con MOCKUP PREVIO por D-F8.6-a), a decidir al abrir.
Estado (S100): la planificación pasa a regirse por el mapa Hito→Objetivo→Cambio de `gestion_proyecto.md`.
  H2 en curso; O-shell CERRADO, O-catálogo es el siguiente objetivo. La antigua «Fase 8» queda subsumida:
  H1 (ajuste) en PAUSA al 90%; el trabajo vivo es H2. La línea «Fase actual: 8…» de abajo es registro de S99.
Fase actual: 8 — UI: configuración y ajuste manual (EN CURSO desde S57). Bloque 8.6-B CERRADO
  en S97, y con él el FRENTE 8.6 ENTERO (AVISO DE OCUPACIÓN AL INICIAR EL ARRASTRE, elegido tras
  CUATRO sesiones nombrado y descartado siempre por el mismo motivo; descartarlo una quinta vez era
  aplazamiento circular. M2 INVERTIDO y declarado: el contrato se fijó ANTES de medir porque «¿qué es
  un conflicto en el cliente?» no lo responde el árbol. El hueco es de DATOS: los índices de S82/S87
  se construyen sobre el diagnóstico ACTUAL desde BD y 8.6-B pregunta por un estado HIPOTÉTICO, así
  que el aviso se alimenta de la PROYECCIÓN. Opción B —«aquí ya hay clase EN LA VISTA ACTUAL»— sobre
  la A, que es el camino al CUARTO ESPEJO que la casilla prohíbe y que el arquitecto desaconsejó por
  escrito antes de que el usuario eligiera. El aviso NO PUEDE INTENTAR SER CORRECTO: en cuanto se le
  pide que acierte, se acaba portando `verificarNoSolapes`. B-4 queda resuelto POR LA ESTRUCTURA
  —`agruparPorActividad` ya devuelve `InstanciaCelda[]` por slot—, no por código nuevo. `background`
  LIBRE sobre el `<td>`, medido aparte y NO heredado de S88, cuyos anclajes eran `<div>` hijos.
  Campaña de 5: M1→T2, M2→T4, M4→los cuatro sin discriminar, M5→T3 y T2; M3 SUPERVIVIENTE DECLARADA
  Y ANTICIPADA (el Set deduplica y la dimensión no es observable en un booleano sobre el `<td>`).
  T3 pasaba POR LA RAZÓN EQUIVOCADA y se corrigió en la misma sesión: su slot tenía DOS
  `InstanciaCelda` y el nombre prometía un escenario que nunca se examinaba; por R5 un test cuyo
  nombre miente es estado vivo equivocado. Frontend 71 → 75; backend 333 intacto. DEUDA NUEVA:
  D-F8.6-B-a, D-F8.6-B-b). Deuda D-F8.6-ivB-a
  CERRADA ENTERA en S96 (el INVARIANTE DEL `<select>`, punto (b) y último resto: tests (37) y (38)
  en `horario-view.spec.ts` aseveran que `bloqueos.listar` SIGUE EN 1 LLAMADA tras `cambiarVista` y
  tras `cambiarEntidad`. El invariante del TSDoc de `cargarPines` (125-132) se sostenía sobre una
  AUSENCIA de llamada —un solo call site, l.189 dentro de `cargar`— y por eso la campaña es POR
  ADICIÓN, no por supresión: la mutación INSERTA `this.cargarPines()` en el gesto, que es el error
  que cometerá quien "arregle" un filtro que parece no refrescarse. Género de mutación sin precedente
  escrito en M3, declarado como novedad. El aserto NO es sobre `pinadas` —un doble que devuelve la
  misma lista la deja idéntica y la mutación quedaría verde— sino sobre el COLABORADOR, con el
  precedente de (25a) en S89. DOS gestos y DOS tests, no uno: `cambiarVista` (304-307) y
  `cambiarEntidad` (309-311) son métodos distintos y cubrir uno dejaría la deuda entreabierta.
  El contraste DESMINTIÓ el riesgo principal que el arquitecto declaró sin medir: `cambiarEntidad`
  es un SETTER PURO que no consulta `entidades()`, así que el fixture vacío no estorbaba. Campaña de
  2, cada una cae SOLO en su test y POR ASERTO B; gestos no acoplados. Suite frontend 69 → 71,
  backend 333 intacto; `horario-view.ts` con diff VACÍO. DEUDA NUEVA: D-F8.6-ivD-b). HIGIENE DOCUMENTAL
  en S95 (condensación de DOS frentes CERRADOS —8.4 entero, 67 → 27 líneas; y el SUB-frente 8.6-iv,
  57 → 31—, más el archivado de S91. La distinción 8.6 / 8.6-iv es del criterio de S63/S80: 8.6-B
  sigue ABIERTO, así que solo el sub-frente era condensable. Cinco tokens tenían UN SOLO citante y era
  la línea a reescribir, luego cada línea condensada conserva sus tokens LITERALMENTE. Tres commits
  con verificación de R4 y costura ENTRE cada uno. Sin código; plan 2630 → 2538). Bloque 8.6-iv-D CERRADO
  en S94 (LOS DOS `set(null)` DE REINTENTO, cubiertos JUNTOS como S93 exigía: (35) para
  `lanzarGeneracion` y (36) para `alDespinar`, ambos encadenando fallo → reintento y aseverando la
  fase INTERMEDIA —error a `null` ANTES de que responda el segundo Subject—, que es la única que
  discrimina el `set`. ANDAMIO: los dobles de `bloqueos.borrar` y `horario.generar` migran de
  Subject COMPARTIDO a FRESCO POR INVOCACIÓN, forma que `guardar` ya tenía desde S89; los tres
  dobles del contenedor quedan homogéneos y `sujetoBorrar`/`sujetoGenerar` desaparecen. Campaña de
  2, cada una cae en su test y POR ASERTO —no por excepción—, y ninguna la mata ningún test previo,
  lo que confirma que las deudas no estaban ya cubiertas. `horario-view.ts` INTACTO. Frontend
  67 → 69, backend 333 intacto. CIERRA D-F8.4-B2-a y el punto (a) de D-F8.6-ivB-a, que SOBREVIVE
  acotada a su punto (b). DEUDA NUEVA: D-F8.6-ivD-a). Bloque 8.4-B2 CERRADO
  en S93 (GESTO DE GENERAR + guarda con diálogo, y CIERRE del frente 8.4 entero: `generar()` en
  `horario.service.ts` con body `{}` —el backend hace TODO el defaulting—, `ConfirmarGeneracion`
  como PRIMER diálogo del repo sobre `@angular/cdk/dialog`, y navegación a `/horario/:id` SINGULAR
  tras el 200, DESCARTANDO la proyección que devuelve el POST para que la recarga la dispare
  `paramMap` y rejilla, pines y diagnóstico no puedan pertenecer a horarios distintos (opción A,
  elegida por el usuario). La PARTICIÓN que el plan recomendaba se RETIRÓ por medición: el POST
  acepta body vacío y el CDK ya estaba en `dependencies`. El contraste desmintió CUATRO premisas
  del arquitecto —ruta singular, `avisosPrevalidacion()` y no `avisos()`, el CDK es primitivo SIN
  estilo, y los 25 tests caen por `Router` y no por `Dialog`— y una QUINTA fue inalcanzable: los
  dos 422 llegan con body seco IDÉNTICO (`include-message` off), así que D5 se REVOCA y el DIÁLOGO
  pasa a enumerar los errores por adelantado, que es donde la información vale. `styles.css` gana
  `overlay-prebuilt.css`, primera hoja global del CDK. 11 tests en dos tandas, los tres huecos de
  la primera campaña cerrados en la misma sesión; frontend 56 → 67, backend 333 intacto. DEUDA
  NUEVA: D-F8.4-B2-a. CORRIGE por R5 la casilla de 8.4-A). Bloque 8.4-B1 CERRADO
  en S92 (PANEL DE PRE-VALIDACIÓN en el frontend, y 8.4-B PARTIDO en B1/B2 por MEDICIÓN: el contraste
  midió que NO EXISTE gesto de generar en el frontend —cero `<button>` salvo el candado, cero `POST
  /api/horarios`— así que la guarda de dos de los seis asertos no tenía nada que envolver. Antes, el
  §A había desmentido la recomendación del arquitecto de MATAR `AVISO`: el javadoc de `Severidad`
  documenta el criterio de cuándo una regla nace no-bloqueante, el string viaja en el contrato REST,
  y los asertos tautológicos de A3/A4 NO caerían al borrarlo porque aseveran `ERROR`. Entregado:
  modelo + servicio + `panel-prevalidacion` con CUATRO RAMAS de clases DISJUNTAS y `avisos()` como
  `signal<T[]|null>` con inicial `null`, que es la señal que separa «no ejecutado» de «ejecutado y
  vacío»; el precedente correcto es el `@if` de tres ramas de `horario-view.html:33-47`, NO
  `errorDiagnostico`, que es el contraejemplo. 4 tests (27)-(30), campaña de 5 sin colaterales, M6
  superviviente declarada; frontend 52 → 56, backend 333 intacto. DEUDA NUEVA: D-F8.4-B1-a;
  D-F8.4-A-c REENCUADRADA). Bloque 8.5-D2b-2 CERRADO
  en S91 (S8 VERIFICABLE por el solver, y CIERRE del frente 8.5-D2b entero: `ReglaDura.TUTORIA_SIN_TUTOR`
  + `VerificadorSolucion.verificarTutorias` —quinto acumulador, ÚNICO método del fichero que no usa
  `solucion`, porque S8 es propiedad del CATÁLOGO— + ONCEAVO parámetro de `aProblemaHorario` con el
  `findAll()` en `GeneradorHorarioService.cargarProblema`, dentro de su `@Transactional`. El §A contra
  el ÁRBOL —cambio propuesto por el usuario— desmintió DOS afirmaciones del arquitecto invisibles en la
  referencia: `Violacion` ya admitía `tramoCodigo=null`, y el mapper es `static` puro sin repositorios.
  Tercer hallazgo: ningún fixture vivo llevaba `requiereTutor`, luego S8 era VACUAMENTE cierta en los
  43. La partición 2a/2b se propuso y se RETIRÓ ante la objeción del usuario sobre el coste documental,
  respaldada por la medición y con la condición de medir el llamador antes de escribir `app`. 13 tests,
  campaña de 12, suite 321 → 333; referencia regenerada; `modelo_datos_fase1.md` corregido por R5.
  CIERRA D-F8.5-D2b1-a y D-F8.5-D2b1-b). Bloque 8.5-D2b-1 CERRADO
  en S90 (transporte de la tutoría al solver, PRIMER BLOQUE DE BACKEND desde S79: `requiereTutor`
  al dominio + DÉCIMA lista `ProblemaHorario.tutorias` + `ProfesorTutoria`/`RolTutoria` propios de
  solver; el corte de S89 se DESMINTIÓ en §A —dejaba la mitad-1 sin consumidor— y 8.5-D2b se parte
  en transporte (D2b-1) y verificación de S8 (D2b-2); el contraste invalidó el contrato: había DOS
  caminos a `domain.Actividad` y «solo io» era inalcanzable, así que `CatalogoMapper` PROPAGA y
  D-B5-5 se REVOCA; 6 tests con fixture defensivo y campaña de 7; suite 315 → 321;
  referencia regenerada. CIERRA D-B5-5). Deuda D-F8.6-ivB-a
  REDUCIDA en S89 (cobertura del camino de PINADO en el contenedor: seis tests (21)-(26) sobre
  `alSoltar`, que no tenía ni un aserto pese a los 11 `it` del fichero; `guardar` pasa a devolver un
  Subject FRESCO POR INVOCACIÓN —un Subject cerrado tras `.error()` redispara síncronamente y hacía
  inobservable la fase «errorPin a null»—; nace (26), preservación del índice previo, que el
  contraste destapó y ninguno de los 46 cubría; campaña de 7 con siete víctimas reales distintas;
  `horario-view.ts` intacto; la deuda NO se cierra, sobrevive acotada). MÉTODO ESCRITO en S86
  (sección «Método de trabajo (procedimiento vigente)», M1-M4, tras el criterio R4/R5: procedimiento
  de cierre de sesión con el archivado como paso verificado, §A de medición, campaña de mutación y
  contraste previo; CIERRA D-F8.0-a; sin código). Bloque 8.6-iii-B2-b CERRADO
  en S88 (los DOS resaltes de violación: input ÚNICO `violaciones` y dos predicados con `.some()`
  que resuelven la asimetría D15 AL PINTAR —la rejilla es la única capa que enumera sub-entradas con
  sus plazas—; resalte SOLO por `outline` tras medir que `background` estaba ocupado en dos capas y
  corregir el mockup de S87 en sus dos sedes vivas; `diagnostico.ts` NO tocado, la capa pura estaba
  completa desde S82 y le faltaba CONSUMIDOR; T5 del desdoble añadido por el contraste; campaña de 6
  con la sexta destapada al ver que T2 no tenía killer; CIERRA el frente 8.6-iii entero y D19/D20 en
  frontend). Bloque 8.6-iii-B2-a CERRADO
  en S87 (cableado del diagnóstico + badge del delta blando: la capa de diagnóstico llevaba desde
  S82 construida y probada pero DESCONECTADA —`DiagnosticoService` e `indiceViolaciones` solo los
  referenciaban sus propios specs—; B2 PARTIDO en B2-a (cable + badge) y B2-b (los dos resaltes);
  nace `sumaDeltasPorInstancia` en la capa PURA, no en el contenedor; suma CON SIGNO y las claves
  de suma 0 NO se emiten; `errorDiagnostico` con selector propio que no gatea la rejilla; 6 tests
  con campaña de 6 mutaciones; backend intacto). Bloque 8.6-iv-A CERRADO
  en S85 (specs de los TRES servicios REST del frontend: `horario.service.spec.ts` +
  `bloqueo.service.spec.ts` + `diagnostico.service.spec.ts`, 5 tests con campaña de 6 mutaciones;
  ESTRENA `provideHttpClientTesting` + `HttpTestingController`, hoy sin uso efectivo en el repo;
  alcance HONESTO declarado —los cinco métodos son wrappers pelados, se congela el CONTRATO DE
  ENDPOINTS, no se cubre lógica—; cierra D-F8.6-iiiA-a con matiz medido; cero dependencias nuevas;
  backend intacto). Bloque 8.6-iv-B CERRADO
  en S84 (capa de test de COMPONENTE del frontend: `horario-view.spec.ts` + `horario-grid.spec.ts`,
  7 tests con campaña de 8 mutaciones; dobles por `useValue`, sin `HttpTestingController`; 8.6-iv
  ABIERTO como bloque que no tenía casilla y PARTIDO en iv-A/iv-B; cierra D-F8.6-iiiB1-a, deja viva
  D-F8.6-iiiA-a; cero dependencias nuevas; backend intacto). Bloque 8.6-iii-B1 CERRADO
  en S83 (gesto de despinar + `indicePines` de `Set<clave>` a `Map<clave, id>`; el candado pasa a
  `<button>` y la rejilla emite la CLAVE, no el id; `listar()` MOVIDO del constructor a `cargar(id)`;
  8.6-iii-B partido en B1/B2, el badge y los resaltes van a B2; backend intacto). Bloque 8.6-iii-A CERRADO
  en S82 (contrato de lectura del diagnóstico en el cliente: modelo TS espejo de los 5 DTOs con la
  asimetría D15 copiada, servicio propio y dos índices puros por `clavePin`; 8.6-iii partido en A/B,
  la pintura y el MOCKUP van a iii-B; backend intacto). Bloques 8.6-i + 8.6-ii
  CERRADOS en S81 (cliente REST de bloqueos + arrastre CDK que pina la instancia; 8.6 partido en
  i/ii/iii; sin movimiento optimista; backend intacto). HIGIENE DOCUMENTAL en S80
  (condensación de los 8 sub-bloques CERRADOS de 8.5 a una línea cada uno, con los tokens de deuda
  conservados dentro; 8.5-D2b y 8.5-D3 quedan íntegros por estar ABIERTOS; sin código). Bloque 8.4-A CERRADO en S79
  (pre-validación por condiciones necesarias, D18: tres reglas ERROR sobre el catálogo, deduplicando
  por ACTIVIDAD; `GET /api/prevalidacion` + guarda en `generar()` → 422 distinguible del infactible del
  solver; (c) subida a ERROR por hallazgo sobre `ModeloCpSat:1046-1074`; 8.4 partido en A/B, D20 va a
  8.4-B). Bloque 8.5-E CERRADO en S78
  (CRUD REST de ProfesorRestriccionHoraria: sub-recurso GET/PUT con reemplazo total; la cadena JPA→
  dominio→CP-SAT ya existía y solo faltaba la superficie de escritura; `peso` NO se expone porque
  ModeloCpSat nunca lo lee). CON ÉL SE CIERRA 8.5: cero sub-bloques vivos (D2b es de solver/, D3
  aplazado). Bloque 8.5-D2a CERRADO en S77
  (ProfesorTutoria + I4 en escritura + herencia PDC←padre por copia; 8.5-D2 partido en D2a/D2b).
  Bloque 8.5-D1 CERRADO en S76 (alta/consulta/borrado de grupo PDC
  como sub-recurso /api/grupos/{idPadre}/pdc + subgrupo mono-Di automático; 8.5-D partido en D1/D2/D3
  tras medir que D2 y D3 exigen esquema nuevo). Bloque 8.5-C3 CERRADO en S75
  (I3 en escritura + CRUD de compatibilidades asignatura↔tipo de aula: semántica (C) opt-in por
  asignatura, sub-recurso con reemplazo total, funnel único resolverContenido; revierte la Referencia
  de compatibilidades que S74 había puesto en AsignaturaService.borrar, reclasificadas como población
  propia; tipificación incidental B07/A12In). Bloque 8.5-C2b CERRADO en S74
  (borrado amable de catálogo: 409 en referencia entrante; cierra D-F8.5-A-a). Bloque 8.5-C2a-DDL CERRADO en S73
  (integridad referencial de esquema: schema.sql + FK + pragma; 8.5-C partido en C1/C2a-DDL/C2b/C3).
  Bloque 8.5-C1 CERRADO en S72 (CRUD de Actividad agregado). Bloque 8.2b-iv CERRADO en S66
  (entrada del bloqueo por REST: /api/bloqueos, POST idempotente
  con reemplazo total, tramo por (dia, ordenEnDia); deuda nueva D-F8.2b-iv-a, espejo de
  validación alta↔BloqueoMapper, mitigada por test de contrato).Bloque 8.3-B CERRADO en S65
  (atribución CONTRAFACTUAL de reglas blandas por celda: delta CON SIGNO = cuánto cambia la penalización
  si la celda no estuviera; tipos ReglaBlanda/Penalizacion/AtribucionBlanda; la fórmula de ventanas y
  consecutivas se EXTRAE a funciones puras que llaman tanto el gemelo como el atribuidor. D19 BACKEND
  CERRADA). Bloque 8.3-A CERRADO en S64
  (atribución ESTRUCTURADA de reglas duras por celda: ResultadoVerificacion pasa de List<String> a
  List<Violacion>; tipos nuevos ReglaDura/CeldaRef/Violacion; una violación conoce QUIÉNES la causan.
  8.3-B —blandas— DIFERIDO por decisión de diseño: los recomputos gemelos valen por ser independientes
  del modelo CP-SAT). Bloque 8.2b-iii-A CERRADO en S62 (cableado del servicio a los repos de bloqueo:
  GeneradorHorarioService.cargarProblema() lee los pines de la BD; el lazo bloqueo→BD→solve por
  POST /api/horarios queda CERRADO end-to-end.
  Cierra la deuda (a) y (c) de 8.2b-ii. 8.2b-iv —entrada del bloqueo por REST— sigue ABIERTO, con
  contrato PRE-CERRADO en S62: endpoint propio /api/bloqueos, NO body de POST /api/horarios).
  Bloque 8.2b-ii CERRADO en S61 (persistencia JPA de los bloqueos §4.7: entidades SesionBloqueada +
  AulaBloqueada + repos + BloqueoMapper de entrada + cableado del placeholder de CatalogoMapper).
  Bloque 8.2b-i CERRADO en S60 (pin de aula por-plaza en el solver + rediseño §4.7/S5). Bloque 8.2a
  CERRADO en S58 (pin de instancia a tramo en el solver: SesionBloqueada estructural + restricción
  dura + verificador + I/O de test; cierra el criterio 5 de Fase 3). Bloque 8.1 CERRADO en S57 (vía
  REST de generación+persistencia, D29 cerrada parcialmente, SeedHorarioRunner partido en
  SeedCatalogoRunner). Fase 7 CERRADA en S56 (7A backend de lectura en S55 + 7B frontend Angular en
  S56). Fase 6 CERRADA en S54.
Última fase completada: 6 — Persistencia de datos (CERRADA en S54: los 4 criterios
  firmados con evidencia ejecutable. El cierre NO fue un bloque de persistencia
  nuevo sino un test de humo end-to-end —CierreFase6HumoTest— que ejercita el
  pipeline completo repos JPA → CatalogoMapper → solver real → SolucionMapper →
  persistencia → recarga. Bloques 1-9 (S45-S53) construyeron las piezas; S54 las
  ensambla y verifica de una tirada. Deuda que la fase deja viva y asignada:
  D26/D27 (nombre de aula, código de tramo) Fase 7/8; D29 (parametrización del solver)
  Fase 8 — CIERRE PARCIAL en S57 (Bloque 8.1): tiempo/semilla/vía expuestos por POST /api/horarios,
  vía = OPTIMIZACION únicamente; FACTIBILIDAD y warm-start NO expuestos (ver nota abajo);
  D30 (renumeración de tramos duplicada) Fase 8; C5 (bloqueo manual de tramo / SesionBloqueada §4.7)
  sin mecanismo en el solver, diferido)
Última sesión registrada (previa): Sesión 98 — Fase 8, HIGIENE DOCUMENTAL: condensación del frente 8.6 ENTERO y archivado de ventana.
  Sin código. Modo interactivo. DOS operaciones sobre `docs/`, en commits separados con verificación
  de R4 y de COSTURA ENTRE cada uno y no solo al final (criterio de S94/S95). El frente 8.6 es el más
  largo del plan y su condición habilitante —que S95 y S96 declararon pendiente de EXACTAMENTE el
  cierre de 8.6-B— se cumplió en S97; el criterio de S93 impide condensarlo en la sesión que lo
  cierra, no en ésta. Primera ACUMULACIÓN REAL desde que se declaró pendiente.
  §A DE MEDICIÓN, y CORRIGIÓ EL PROPIO ALCANCE DEL ARQUITECTO: la medición inicial situó el fin del
  frente en la l.1399 (fin de la casilla iv-E), pero la casilla 8.6-B se extendía hasta la l.1426; el
  frente real era 1286–1426, 141 líneas y no 114. El error se cazó al ver que la costura inferior
  dejaba el cuerpo original de 8.6-B tras la línea condensada, ANTES de consolidar el commit. Rango
  corregido y re-sustituido.
  HALLAZGO QUE FIJÓ EL CONTENIDO DE LAS LÍNEAS CONDENSADAS (regla de S63/S95): se midió citante por
  citante que NINGÚN token del frente es solo-frente —todos tienen definición viva fuera (sección de
  deuda del plan o bitácora)—, luego condensar no crea huérfanos SIEMPRE que cada línea condensada
  conserve LITERALMENTE los tokens que hoy cita. `D-F8.6-A-2` es el caso de S95 (citante único
  global, def. en bitácora): conservado literal. `D-F8.6-ii-5` y `-ii-a` son IDENTIFICADORES DE
  DECISIÓN definidos in situ en su casilla, no deudas con línea de negrita; un chequeo de R4 que solo
  busca el patrón de deuda los marca como falsos huérfanos, y se descartó por lectura.
  DECISIÓN DEL USUARIO sobre las tres marcas «bitácora Sxx (futura)» (iv-D/S94, iv-E/S96, B/S97):
  quitar «(futura)» SOLO en la casilla de S94 —que esta misma sesión archiva— y CONSERVARLO en S96 y
  S97, que siguen en la ventana viva y aún no están en la bitácora. Es más fiel al estado real que la
  opción A pura de S95 (conservar la marca tal cual), habilitada porque el archivado ocurre en esta
  sesión. Se aplica en el Commit 2, no en el 1: en el Commit 1 (solo condensación) las tres conservan
  «(futura)» porque S94 aún no está archivada.
  ENTREGADO: frente 8.6 de 141 → 65 líneas (−76, −54 %), doce casillas a 2–5 líneas cada una con el
  formato «qué (Sxx) → deuda/decisión superviviente; Detalle: bitácora Sxx». R4 GLOBAL verificado por
  censo de tokens (110 únicos en el original, los mismos 110 en el nuevo, cero desaparecidos) y por
  costura (8.5-E intacto arriba, «Diferibles» intacto abajo con su única línea en blanco). Archivado
  de S94 con las TRES rotaciones de M1-bis (nace S98, sale S94 PROMOVIDA a `### Sesión 94`, degrada
  S97) y los dos censos de la bitácora, la crónica y la frase de ventana actualizados.
  NO se tocó código, ni `modelo_datos_fase1.md`, ni `referencia-codigo-solver.md` (no hay `solver/
  src/main` en esta sesión). Suites NO ejecutadas: no procede, cero ficheros de código en el diff.
  LIMPIEZA EVALUADA PARA LA PRÓXIMA (M1.5): sin frentes cerrados acumulados. Con 8.6 condensado, no
  queda ningún frente de Fase 8 cerrado y sin condensar; la Fase 8 no tiene bloque vivo (8.5-D3
  aplazado). Decirlo es la respuesta correcta.
  Siguiente: D-F8.6-B-a (el genérico de `CdkDragDrop` que miente, en el próximo bloque que toque el
  camino de soltado), D-F8.6-ivD-b (homogeneizar el doble de `listar`, en el próximo que toque
  `horario-view.spec.ts`) o D-F8.6-iiiA-b (`Totales` sin sede, con MOCKUP PREVIO por D-F8.6-a), a
  decidir al abrir.
Última sesión registrada (previa): Sesión 97 — Fase 8, Bloque 8.6-B: aviso de ocupación al iniciar el arrastre (CIERRA 8.6-B y el frente 8.6 ENTERO).
  Modo híbrido. 3 commits de código (be9b4cf producción, d01c5ec tests, ec2a8b2 corrección de
  fixture) + doc aparte. SESIÓN DE DISEÑO por decisión de apertura: el bloque llevaba CUATRO
  sesiones nombrado y descartado siempre por el mismo motivo —«el contrato hay que decidirlo antes
  de medir, orden inverso a M2»—, que no es un obstáculo sino EL TRABAJO. Descartarlo una quinta vez
  era aplazamiento circular, el patrón que S95 rompió a la fuerza con 8.4.
  M2 INVERTIDO Y DECLARADO: la pregunta «¿qué es un conflicto en el cliente?» no la responde el
  árbol, así que el contrato se fijó primero y la medición vino después, con preguntas concretas.
  Lo medible se midió igual, en DOS §A: uno de estructuras (proyección, CDK, contenedor) y otro de
  CSS antes de dibujar nada.
  EL HUECO QUE REENCUADRA EL BLOQUE, y es de DATOS y no de lógica: `indiceViolaciones` e
  `indicePenalizaciones` (S82/S87) se construyen sobre el diagnóstico ACTUAL desde BD y responden a
  «¿esta celda, donde está, tiene un problema?». 8.6-B pregunta por un estado HIPOTÉTICO —«si suelto
  ahí, ¿chocaría?»— que ninguna capa viva produce. Conclusión: el aviso NO se alimenta del
  diagnóstico sino de la PROYECCIÓN, que es la que sabe qué hay ya colocado en cada tramo.
  TRES OPCIONES DE CONTRATO, con la elección razonada y no por coste. (A) aviso de recurso completo
  —profesor/aula/subgrupo ocupados en el destino— es lo que el usuario esperaría de la palabra
  «conflicto» y es EL CAMINO AL CUARTO ESPEJO que la casilla prohíbe: son las tres reglas de
  `verificarNoSolapes` con D15 dentro. RECHAZADA, y el arquitecto declaró que la desaconsejaba por
  escrito antes de que el usuario eligiera. (C) marcar solo destinos pinados es baratísimo pero
  responde a una pregunta que casi nadie se hace. ELEGIDA (B): marcar los tramos que YA TIENEN algo
  en la vista actual. No dice «esto viola una restricción», dice «aquí ya hay clase».
  CONSECUENCIA DE LA GUILLOTINA DE LA CASILLA, escrita para que no se pierda: EL AVISO NO PUEDE
  INTENTAR SER CORRECTO. En cuanto se le pide que acierte, se acaba portando `verificarNoSolapes`.
  El diseño parte de que es barato, incompleto y HONESTO SOBRE SERLO.
  §A-1 (estructuras) DESMINTIÓ UNA PREVISIÓN DEL ARQUITECTO Y CONFIRMÓ OTRA. Confirmada: no hay
  NINGÚN handler de entrada, solo `(cdkDropListDropped)` (`horario-grid.html:15`) → el bloque
  necesita cablear un evento que no existe, es PRODUCCIÓN y no un `computed`; coste al alza.
  DESMENTIDA, y en dirección favorable: dije que esperaba que `proyeccion.ts` no tuviera nada
  indexado por tramo y TIENE DOS —`agruparPorSlot` (:58) y `agruparPorActividad` (:89)—, ambas con
  clave `claveSlot(dia, tramo)`. La mitad de datos del bloque ya existía y estaba probada.
  TERCER HALLAZGO, no pedido, que va a DEUDA: `horario-grid.ts:150` anota
  `CdkDragDrop<{dia, orden}>` pero NINGÚN `<td>` lleva `[cdkDropListData]`; el día y el tramo llegan
  por argumentos del template. Es una firma que MIENTE sobre el mecanismo → D-F8.6-B-a. NO se
  arregló aquí por decisión explícita del usuario (C-3): tocarlo exige cambiar cómo `alSoltar`
  obtiene el tramo, que es camino de pinado ya cubierto por (21)-(26) y (35)-(38).
  EL CONTRATO SE SIMPLIFICA POR LA ESTRUCTURA, no por código nuevo: B-4 («un desdoble ocupa el slot
  UNA vez») queda RESUELTO por `agruparPorActividad`, que devuelve `InstanciaCelda[]` por slot con
  las entradas dentro. NO hay que escribir la lógica de D15 aquí: la capa que ya existe la resolvió.
  B-5, obligado por la medición: el aviso LEE el índice que la rejilla ya consume, no crea uno nuevo
  —sin estructura nueva no hay tentación de meterle reglas—.
  §A-2 (CSS) Y UN RIESGO MAL DIAGNOSTICADO POR EL ARQUITECTO, en la misma familia que la lección de
  S96: declaré que la colisión con el CDK sería «peor que la de S88 porque competirían
  simultáneamente». FALSO, medido: `.cdk-drop-list-dragging` (:90-93, `outline 2px dashed #4a7`) la
  aplica el CDK SOLO al dropList sobrevolado, uno de treinta, mientras la marca de (ii) va sobre
  todos los ocupados. Coexisten y solo se cruzan en un `<td>`, donde además informan de cosas
  distintas. Declarar el riesgo no eximió de tenerlo mal formulado. `outline` se descarta igual, pero
  por LEGIBILIDAD —misma propiedad y mismo verde para dos señales— y no por colisión destructiva.
  HALLAZGO QUE FIJA LA PROPIEDAD: `background` está LIBRE sobre el `<td>` (`.rejilla td` :8-13 solo
  declara `border`, `padding`, `vertical-align`; el `#f2f2f2` es de `.rejilla thead th, .tramo`, que
  son `<th>`). Lo que S88 midió ocupado —`.entrada` `#fafafa`, `.instancia.pinada .entrada`
  `#fff8ec`— cae sobre `<div>` HIJOS, no sobre el `<td>`: elementos distintos, medidos aparte y no
  heredados de S88. Color `#eef2f6` (gris frío) elegido por el usuario sobre un ámbar que rozaba el
  `#fff8ec` de pinada.
  DECISIÓN C-1 DEL USUARIO, que se aparta de la LETRA de la casilla y por eso se preguntó en vez de
  decidirse: (ii) `cdkDragStarted` marcando TODOS los slots ocupados de golpe, frente a (i)
  `cdkDropListEntered` celda a celda. Razón: (i) informa cuando ya has decidido dónde vas, (ii)
  informa MIENTRAS decides. Un solo evento en vez de uno por celda.
  ENTREGADO: señal privada `arrastrando: signal<string | null>` con la `clavePin` de la instancia
  arrastrada; `computed slotsOcupados: Set<string>` que en reposo devuelve Set VACÍO y arrastrando
  incluye el slot si queda ≥1 `InstanciaCelda` distinta de la arrastrada; `claveSlot` expuesto
  `protected readonly` (patrón de `dias`/`tramos`) para no cambiar firmas; `(cdkDragStarted)` /
  `(cdkDragEnded)` en el `<div.instancia>`; `[class.ocupado]` en el `<td>` —PRIMER binding de clase
  del `<td>`, que hasta hoy no tenía ninguno—; un ÚNICO bloque CSS nuevo
  `.rejilla td.ocupado { background: #eef2f6 }`, el 21.º del fichero.
  TSDoc OBLIGATORIO en `slotsOcupados` con las TRES afirmaciones, porque una incompletitud no
  declarada es una promesa falsa (familia de D-F8.6-iiiB1-c): (a) dice «hay clase EN LA VISTA
  ACTUAL», no un veredicto; (b) es CIEGO por construcción a los recursos que la vista no muestra —en
  vista por grupo no ve profesor ni aula—; (c) NO es una verificación y NO debe crecer hacia una.
  CINCO TESTS DE CAMPAÑA SOBRE CUATRO TESTS. M1 (no excluir el origen) cae SOLO en T2; M2 (no
  limpiar en `cdkDragEnded`) cae SOLO en T4; M4 (Set vacío siempre) cae en los cuatro y NO discrimina
  en exclusiva, registrado como tal. M3 (contar entradas en vez de instancias) SUPERVIVIENTE
  DECLARADA Y ANTICIPADA ANTES DE CORRERLA: el Set deduplica, así que «una vez» y «tres veces» dan la
  misma clase en el mismo `<td>`; la dimensión NO es observable en un booleano sobre el `<td>` y NO
  se inventó aserto artificial para taparla. T3 documenta la intención, no la asevera.
  T3 PASABA POR LA RAZÓN EQUIVOCADA Y SE CORRIGIÓ EN LA MISMA SESIÓN, no se dejó como deuda. El
  arquitecto pidió transcribir el fixture de T3 y la primera entrega no lo hizo; al pedirlo se vio
  que el slot examinado tenía DOS `InstanciaCelda` (el desdoble `Mat-1ºA|2` MÁS el `LCL-1ºA|1` que
  se arrastraba), de modo que el nombre del test —«un slot con desdoble»— prometía un escenario que
  NUNCA se examinaba. El desdoble era portante y el test no estaba roto, pero una mutación
  `instancias.length > 1` lo dejaba VERDE mientras rompía en producción todo slot con un desdoble
  solitario. Corregido con el patrón que T2 ya usaba (`enSlot(LCL_SIN_PIN, 2, 3)`) y nace M5, que
  con el fixture viejo SOBREVIVÍA y con el nuevo CAE en T3 por `toContain('ocupado')` —y también en
  T2, cuyo testigo es igualmente de una instancia: M5 tampoco discrimina en exclusiva—. Por R5, un
  test cuyo nombre miente es estado vivo equivocado, igual que un mockup mal medido (S87/S88).
  Suite frontend 71 → 75 (12 ficheros); `horario-grid.spec.ts` 7 → 11 `it()` (contados con
  `grep -cE "^\s*it\("`, forma anclada); backend 333 INTACTO. No se tocó `solver/src/main` →
  `referencia-codigo-solver.md` NO regenerada; `modelo_datos_fase1.md` NO tocado (ni entidad ni
  invariante nueva).
  DEUDA NUEVA: D-F8.6-B-a (el genérico que miente), D-F8.6-B-b (el aviso es ciego a dos de los tres
  recursos, por diseño). CIERRA el Bloque 8.6-B y con él el frente 8.6 ENTERO: cero sub-bloques
  vivos.
  LIMPIEZA EVALUADA PARA LA PRÓXIMA (M1.5): AHORA SÍ HAY ACUMULACIÓN. Con 8.6-B cerrado, el frente
  8.6 queda cerrado entero y es condensable —condición que S95 y S96 declararon pendiente de
  exactamente este bloque—. Es el candidato natural de la próxima sesión de higiene, y no se condensa
  en la que lo cierra (criterio de S93).
  Siguiente: HIGIENE (condensar 8.6, cuya condición habilitante se cumple HOY por primera vez),
  D-F8.6-iiiA-b (`Totales` sin sede, con MOCKUP PREVIO por D-F8.6-a), D-F8.6-ivD-b (homogeneizar el
  doble de `listar`) o D-F8.6-B-a (el genérico que miente), a decidir al abrir.
Última sesión registrada (previa): Sesión 96 — Fase 8, D-F8.6-ivB-a punto (b): el invariante del `<select>` (CIERRA la deuda ENTERA).
  Modo híbrido. 1 commit de código (solo `horario-view.spec.ts`) + doc aparte. Bloque elegido entre
  tres candidatos por ser el ÚNICO que podía cerrar una deuda entera hoy: el argumento que la
  mantenía abierta —«cerrarla obligaría a abrir una hermana con ese resto»— MURIÓ en S94 al no
  quedar resto. El plan lo tenía «asignado al bloque que retome el gesto de cambio de vista», que es
  preferencia de encaje y no prohibición.
  §A DE MEDICIÓN (M2) CONTRA EL ÁRBOL, y su hallazgo REENCUADRÓ el bloque: `cargarPines` tiene UN
  SOLO call site en `main` (l.189, dentro de `cargar`) y `cambiarVista` toca exactamente dos señales
  —`vista` y `entidad`—, ninguna de ellas `pinadas` ni `bloqueos`. El invariante que el TSDoc
  (125-132) declara NO SE DEFIENDE CON LÓGICA: se cumple porque nadie escribió la llamada. De ahí que
  la campaña sea POR ADICIÓN y no por supresión, género sin precedente escrito en M3 y declarado
  como novedad en vez de colarlo.
  EL §A CORRIGIÓ TAMBIÉN EL ASERTO QUE EL ARQUITECTO TRAÍA. Aseverar sobre `pinadas` NO discrimina:
  un doble de `listar` que devuelve la misma lista deja la señal con contenido idéntico y la
  mutación queda VERDE. El aserto discriminante es sobre el COLABORADOR —`toHaveBeenCalledTimes(1)`
  sobre `bloqueos.listar`—, con precedente en el propio fichero: (25a) hizo lo mismo con
  `getProyeccion` tras pinar (S89).
  DOS GESTOS, DOS TESTS. `cambiarVista` (304-307) y `cambiarEntidad` (309-311) son métodos
  DISTINTOS y el TSDoc nombra los dos; cubrir uno solo dejaba la deuda entreabierta y volvería a
  abrir una hermana, que es justo lo que S94 evitó al cerrar (a) y su gemela JUNTAS. Coste revisado
  al alza ANTES de elegir, con la medición delante: de BAJO a BAJO-MEDIO, porque no era «un test».
  EL CONTRASTE (M4) DESMINTIÓ EL RIESGO PRINCIPAL, y era el que el arquitecto había declarado
  explícitamente como no medido: supuso que (38) necesitaba `entidades()` con dos elementos para
  tener destino distinto, y `cambiarEntidad` es un SETTER PURO que no consulta ese computed. Basta
  pasar una entidad distinta de la actual (`''`); el fixture `PROYECCION_VACIA` no se tocó. Acertó
  al declararlo sin medir y erró en el CONTENIDO: queda como error de especificación, no como falso
  positivo. En verde, medido: los `protected` se invocan con el precedente de cast de la l.776, el
  helper `montar([])` (181-187) da el montaje mínimo, y NINGÚN test previo asevera sobre
  `bloqueos.listar`, luego no había cobertura por acoplamiento que confundiera la campaña.
  HALLAZGO NO PEDIDO POR EL CONTRATO y que va a deuda: `bloqueos.listar` es Subject COMPARTIDO, la
  ÚNICA de las cuatro llamadas del contenedor que quedó en la forma vieja tras la homogeneización de
  S94. No estorba a (37) ni a (38) —ninguno reintenta— pero es la asimetría de andamio «de la que no
  se ve» que S94 nombró.
  ENTREGADO: tests (37) y (38), cada uno con ASERTO A (precondición: `listar` llamado 1 vez ANTES
  del gesto, para no medir un no-op), ASERTO B discriminante (sigue en 1 DESPUÉS) y ASERTO C
  (`vista()`/`entidad()` cambió de verdad, para que B no mida una llamada que nunca ocurrió).
  CAMPAÑA DE 2 POR ADICIÓN: M1 (insertar `this.cargarPines()` en `cambiarVista`) cae SOLO en (37);
  M2 (lo mismo en `cambiarEntidad`) cae SOLO en (38); las dos POR ASERTO B —recibe 2 esperando 1—,
  sin excepción. Los gestos NO están acoplados y ningún test previo cae con ninguna.
  Suite frontend 69 → 71 (12 ficheros); `horario-view.spec.ts` 23 → 25 `it()`; backend 333 INTACTO.
  `horario-view.ts` con diff VACÍO contra HEAD: cero producción. No se tocó `solver/src/main` →
  `referencia-codigo-solver.md` NO regenerada; `modelo_datos_fase1.md` NO tocado.
  NOTA DE INSTRUMENTO: el recuento de `it()` del fichero es 23, no 36; `grep -c "it("` contaba
  subcadenas (`emit(`, `split(`). Misma clase de trampa que la lección de S95 sobre `grep -c` sin
  ancla. Los 69 del prompt son la suite ENTERA, no este fichero.
  DEUDA NUEVA: D-F8.6-ivD-b. CIERRA D-F8.6-ivB-a ENTERA.
  LIMPIEZA EVALUADA PARA LA PRÓXIMA (M1.5): sin frentes cerrados acumulados. 8.6 seguirá siendo
  condensable solo cuando cierre 8.6-B, su único sub-bloque vivo; decirlo es la respuesta correcta.
  Siguiente: 8.6-B (aviso durante el arrastre; ÚNICO bloque de frontend abierto, contrato ANTES de
  medir, orden inverso a M2, y solo defendible si la sesión se dedica a DISEÑO), D-F8.6-iiiA-b
  (`Totales` sin sede, con MOCKUP PREVIO por D-F8.6-a) o D-F8.6-ivD-b (homogeneizar el doble de
  `listar`), a decidir al abrir.
Última fase completada (previa): 5 — Solver: instituto completo (criterios 1-2
  cerrados en S36 por factibilidad pura; criterios 3-4 cerrados en S44 como decisión
  de producto gemela de D23, con respaldo descriptivo a escala)
Las cabeceras compactas de S37–S43 y el registro detallado de S10–S42 se
archivaron en `docs/bitacora-sesiones.md` en sesiones anteriores; las cabeceras
de S44, S45 y S46 se archivaron en la Sesión 50, la de S47 en la Sesión 51, la de S48
en la Sesión 52, la de S49 en la Sesión 53, la de S50 en la Sesión 54, las de S51, S52,
S53 y S54 en la Sesión 58, la de S55 en la Sesión 59, la de S56 en la Sesión 60, la de S57
en la Sesión 61, la de S58 en la Sesión 62, la de S59 en la Sesión 63, la de S60 en la
Sesión 64, la de S61 en la Sesión 65, la de S62 en la Sesión 66, la de S63 en la Sesión 67, la de S64 en
la Sesión 68, la de S65 en la Sesión 69, la de S66 en la Sesión 70, la de S67 en la Sesión 71 y la de
S68 en la Sesión 72, la de S69 en la Sesión 73, la de S70 en la Sesión 74, la de S71 en la Sesión 75, la de S72 en la Sesión 76, la de S73 en la Sesión 77, la de S74 en la Sesión 78 la de S75 en la Sesión 79 la de S76 en la Sesión 80, la de S77 en la Sesión 81, la de S78 en la Sesión 82 la de S79 en la Sesión 83 la de S80 en la Sesión 84, la de S81 en la Sesión 85 la de S82 en la Sesión 86 la de S83 en la Sesión 87 la de S84 en la Sesión 88, la de S85 en la Sesión 89, la de S86 en la Sesión 90, la de S87 en la Sesión 91 la de S88 en la Sesión 92, la de S89 en la Sesión 93 la de S90 en la Sesión 94 la de S91 en la Sesión 95, la de S92 en la Sesión 96, la de S93 en la Sesión 97, la de S94 en la Sesión 98 y la de S95 en la Sesión 99 (misma higiene documental; en S60 se corrigió además una copia
truncada y duplicada de S55 que la operación de archivado de S59 dejó en la bitácora; en S69 se corrigió
el censo de la bitácora, que S68 había dejado en S63 pese a contener ya S64). El plan conserva las 4
últimas cabeceras compactas (S96–S99). El detalle histórico de cualquier sesión anterior —incluida S42
(citada por la deuda abierta D25) y S43 (citada por el cierre de D23)— está en la bitácora.

<!-- Registro detallado de S32–S42 archivado en docs/bitacora-sesiones.md (S44). -->

### Bloques de Fase 2
- [x] Bloque 1 — Setup del repositorio
- [x] Bloque 2 — POJOs del dominio
- [x] Bloque 3 — Schema JSON + DTOs + mapper
- [x] Bloque 4 — Construcción del modelo CP-SAT
- [x] Bloque 5 — Output a consola
- [x] Bloque 6 — Dataset real 1ºESO ordinarias + verificación PDFs

### Bloques de Fase 5
- [x] Bloque 1 — Religión multi-grupo por parejas (Tipo 4), validada con 1ºESO real. (S19) → —. Detalle: bitácora S19.
- [x] Bloque 2 — Escala 1º+2ºESO (7 grupos); primer punto de la curva de escala, inaugura el linaje de fixture escala-instituto. (S20) → —. Detalle: bitácora S20.
- [x] Bloque 3 — Escala 3ºESO ordinario (10 grupos), segundo punto de la curva. (S21) → deuda D31 (particiones de refuerzo/ATED de 3º sin validar por el centro). Detalle: bitácora S21.
- [x] Bloque 4 — Lectura B (SubgrupoGrupo N:M): trabajo estructural que desbloquea el Tipo 7. (S22) → dominio Subgrupo→grupos N:M (decisión permanente); modelo §6.3/§6.4/§6.5 y S9 actualizados. Detalle: bitácora S22.
- [x] Bloque 5 — PDC de 3º a escala, modelado como grupo de Diversificación propio (mono-Di), no como Lectura B. (S23) → regla "PDC = grupo administrativo propio con padre (I5), subgrupo mono-Di; compartidas en el ordinario para evitar el doble conteo que activaría D3". Detalle: bitácora S23.
- [x] Bloque 6a — Nace el régimen de OPTIMIZACIÓN (vía separada de la de factibilidad) con el término de ventanas del profesorado. (S24) → el solver tiene dos vías (factibilidad / optimización); deuda D17. Detalle: bitácora S24.
- [x] Bloque 6b — El solver consume las indisponibilidades DURA del profesorado + oro fuerte de ventanas. (S25) → indisponibilidad DURA consumida en ambos regímenes; modelo §4.3 actualizado; deuda D18. Detalle: bitácora S25.
- [x] Bloque 6c — Indisponibilidad BLANDA del profesorado como segundo término del objetivo. (S26) → el solver consume BLANDA (2º término); D17 resuelta para este término; pesos hardcodeados (D21). Detalle: bitácora S26.
- [x] Bloque 6d-c — Sesiones consecutivas máximas (MAX_CONSECUTIVAS=3) como tercer término del objetivo. (S27) → tercer término blando; deuda D21 ampliada (pesos y N hardcodeados). Detalle: bitácora S27.
- [x] Bloque 7 — Escala 4ºESO ordinario (4A-4D, linaje aislado). (S28) → deuda D31 (optativas DIG/TEC/FOPP de 4º modeladas como población propia por bloque); S8 (tutor obligatorio) aún no la consume el solver. Detalle: bitácora S28.
- [x] Bloque 8 — Escala 4ºESO COMPLETO (+2 PDC); cierra 4º como linaje. (S29) → aplica la regla S23 (ámbito = una actividad con subgrupo compartido de los dos Di). Detalle: bitácora S29.
- [x] Bloque 9 — Fusión de niveles 3º+4º ESO; primer ejercicio de D4 (Gim/Pista compartido entre cursos). (S30) → D4 no muerde en el par, baja de severidad. Detalle: bitácora S30.
- [x] Bloque 10 — Fusión ESO completa 1º-4º; D4 a competencia real (Gim 26/30). (S31) → D4 rebajada a residual; primera señal de coste no-lineal (D23). Detalle: bitácora S31.
- [x] Bloque 11 — Escala 1ºBach completo; primera validación a escala del Tipo 7 (optativas transversales). (S32) → deuda D31 (población "1 subgrupo por opción"); modela subgrupos mono-grupo en plaza, no Lectura B N:M (divergencia consciente con §6.5, estilo linaje instituto/frontera S14). Detalle: bitácora S32.
- [x] Bloque 12 (Sub-bloque A de FPB) — D13 en src/main: lista blanca de inicios para que un bloque de 2-3 tramos no cruce día ni recreo. (S33) → CIERRA D13; deuda D22 (frontera de recreo hardcodeada). Detalle: bitácora S33.
- [x] Sub-bloque B de FPB — fixture 1ºFPB real a escala; D13 ejercitada. (S34) → —. Detalle: sin entrada propia en la bitácora (S34 no se archivó en su día); contexto en bitácora S33 y S35.
- [x] Sub-bloque C de FPB — fixture 2ºFPB real a escala; CIERRA el nivel FPB. (S35) → —. Detalle: bitácora S35.
- [x] Bloque 13 — FUSIÓN INSTITUTO COMPLETO (26 grupos), factible en 269,4 s < 10 min, 0 duras. (S36) → CIERRA los criterios 1-2 de Fase 5; deuda D23 (coste no lineal); deuda D31 (poblaciones de modalidades/optatividad de 2ºBach). Detalle: bitácora S36.
- [x] Bloque 16 — Poda de aula (D23 palanca b): mecanismo + discriminación. (S41) → mecanismo LATENTE, se desactiva en B17 (ver D23). Detalle: bitácora S41.
- [x] Bloque 17 — Poda de aula medida a escala: INVIABLE, poda OFF por defecto. (S42) → CIERRA la palanca (b) de D23; ALTA D25. Detalle: bitácora S42.
- [x] Bloque 18 — Experimento pareado de atribución: la no-convergencia a escala es ESTRUCTURAL, no de un bloque. (S43) → CIERRA D23 como decisión de producto (FEASIBLE sin optimalidad probada a escala). Detalle: bitácora S43.

### Bloques de Fase 6
- [x] Bloque 1 — Andamiaje del módulo `app/` + humo de persistencia sobre SQLite en disco. (S45) → stack de persistencia (Spring Boot 4.1.0 / Hibernate 7.4.1 / SQLite + dialecto de comunidad), fijado en Decisiones permanentes. Detalle: bitácora S45.
- [x] Bloque 2 — Catálogo del centro como 8 entidades JPA (§4.1) en `app.catalog` + 3 enums + 8 repositorios; round-trip sobre SQLite real. (S46) → `LocalTime` persiste intacto en SQLite (ver Notas técnicas Fase 6). Detalle: bitácora S46.
- [x] Bloque 3 — `CatalogoMapper` JPA→dominio (Aula/Asignatura/Profesor/Grupo/Tramo): `VIRTUAL_OPTATIVA` aborta con excepción explícita, recreo excluido del Tramo. (S47) → deudas D26 (`Aula.nombre = codigo`) y D27 (código de Tramo sintetizado L1..V6). Detalle: bitácora S47.
- [x] Bloque 4 — Entidad JPA `Subgrupo` (§4.2, `@ManyToMany` a `GrupoAdministrativo` vía `subgrupo_grupo`) + `SubgrupoRepository` + `CatalogoMapper.aSubgrupo` (población por identidad, aborta ante grupo huérfano). (S48) → `Particion`/`SubgrupoParticion` diferidas a Fase 8 (deudas D1/D7). Detalle: bitácora S48.
- [x] Bloque 5 — Entidades JPA `Actividad` (agregado raíz, cascade+orphanRemoval a `Plaza`) y `Plaza` (`aula_fija` `@ManyToOne` + 3 `@ManyToMany`) + `CatalogoMapper.aActividad`/`aPlaza`; enum `app.catalog.PatronTemporal` propio. (S49) → `ActividadInstancia` NO se materializa como tabla (artefacto derivado, D-B5-1); el XOR aula_fija/candidatas lo valida el record de dominio (D-B5-2); `requiereTutor` persiste pero el solver no lo consume (D-B5-5, S8) → **D-B5-5 CERRADA en S90** (8.5-D2b-1): `CatalogoMapper.aActividad` propaga `entidad.isRequiereTutor()` al 7.º componente del record de dominio y su Javadoc revoca la decisión. Detalle: bitácora S49 y S90.
- [x] Bloque 6 — `CatalogoMapper.aProblemaHorario` (7 listas JPA → `ProblemaHorario` completo); índices por código con `toMap` que ABORTA ante código duplicado. (S50) → catálogo COMPLETO sin poda —huérfanos entran, el mapper traduce, no poda— (D-B6-1); dejó `restriccionesHorarias` vacía hasta B7 (D28). Detalle: bitácora S50.
- [x] Bloque 7 — Entidad JPA `ProfesorRestriccionHoraria` (§4.3) + enum `TipoRestriccion` + `CatalogoMapper.aRestriccionHoraria`; refactor `aTramosConIndice` (produce la lista de Tramo y el `IdentityHashMap<TramoSemanal,Tramo>` en un bucle). CIERRA D28. (S51) → el tramo se resuelve por REFERENCIA de objeto, no por el código sintético L1..V6 (no reabre D27, D-B7-2); el peso DEFAULT 1 de §4.3 no se materializa en la entidad (política de UI/Fase 8, D-B7-6). Detalle: bitácora S51.
- [x] Bloque 8 — Servicio de aplicación: orquestación repos → mapper → solver (S52).
      GeneradorHorarioService (es.yaroki.educhronos.app.service, @Service, PRIMER bean
      de servicio del módulo; constructor injection, 8 repos private final; commit
      f72ca82). Dos métodos: cargarProblema() @Transactional(readOnly=true) —los 8
      findAll() + CatalogoMapper.aProblemaHorario(...) en una MISMA transacción, para
      mantener la sesión Hibernate viva (sin LazyInitializationException) y la identidad
      de objeto que la resolución por referencia del mapper exige (grupoPadre, población
      de subgrupo, TramoSemanal de restricciones)— y generar() SIN transacción, que llama
      a cargarProblema() (la transacción se cierra ahí; el ProblemaHorario es ya un POJO
      desligado de JPA) e invoca fuera de transacción
      new SolverHorario().resolverOptimizandoConDetalle(problema), devolviendo
      ResultadoOptimizacion. Cuatro decisiones cerradas antes de construir (D-B8-1
      transacción en cargarProblema, solver fuera; D-B8-2 8× findAll() sin
      @EntityGraph/fetch-joins, N+1 irrelevante a esta escala; D-B8-3 dos métodos, salida
      ResultadoOptimizacion por la vía de optimización; D-B8-4 el servicio NO valida
      integridad —la aborta el mapper, deja propagar— ni guarda catálogo vacío, Fase 8).
      Constructor por defecto del solver (120 s / semilla 42, confirmado en
      SolverHorario.java): a escala de instituto completo la optimización no converge en
      ese presupuesto (S38/S42), luego generar() sobre un centro real dará
      ResultadoOptimizacion en estado FEASIBLE, no OPTIMAL —no es fallo: el record porta
      estado+objetivo+cotaInferior para distinguirlo; parametrización de tiempo/semilla y
      elección de vía diferidas a Fase 8, deuda D29—. Test de integración
      GeneradorHorarioServiceTest (@DataJpaTest + @Import(GeneradorHorarioService.class)
      sobre SQLite real; commit 44694d1): catálogo mínimo con un grupo PDC (3ºADi →
      grupoPadre 1ºA no nulo) y una restricción horaria (referencia al TramoSemanal L1),
      recargado por el servicio, verificando que el ProblemaHorario enlaza padre y tramo
      por identidad —ejercita el fallo SILENCIOSO por identidad de objeto, el riesgo
      dominante del bloque—. Caso opcional de generar() OMITIDO conscientemente (exigiría
      un catálogo garantizado factible con actividades/plazas: más código y riesgo de
      lentitud; el valor del bloque está en cargarProblema() y la frontera transaccional,
      no en re-testear el solver). src/main del solver NO tocado → referencia-codigo-
      solver.md NO regenerado; modelo NO tocado. Suite: solver 59 + app 33 (32 previos +
      1), BUILD SUCCESS con mvn clean test desde la raíz. Commits separados código/tests
      f72ca82/44694d1, de una línea. Siguiente: Bloque 9 (a decidir).
- [x] Bloque 9 — Persistencia de la solución (§4.7): entidades `HorarioGenerado` + `Sesion` (una por PLAZA colocada, `@UniqueConstraint(horario,plaza,indice)`) + repos + `SolucionMapper` (aula por código, tramo por el puente de renumeración —D30—, aborta ruidoso ante código/tramo/aula sin correspondencia); el servicio gana `guardar()` `@Transactional` y `cargarHorario()`. (S53) → cierra el criterio "reabrir y el horario intacto" de Fase 6; `Sesion = plaza colocada` (Opción A) corrigió el UNIQUE de §4.7 que imposibilitaba el desdoble. Detalle: bitácora S53.
- [x] CIERRE DE FASE — test de humo end-to-end `CierreFase6HumoTest` (pipeline completo: repos JPA → `CatalogoMapper` → solver real → `SolucionMapper` → guardar → recargar); firma los 4 criterios y Fase 6 queda CERRADA. (S54) → un Bloque 10 de persistencia no acercaba el cierre (C5 no la consume el solver); D30 sigue sin verificar el emparejamiento exacto del tramo. Detalle y decisiones D-B10-1..9: bitácora S54.

### Bloques de Fase 8

Descomposición por dependencias fijada al abrir la fase (S57) y refinada después.
Se registra aquí porque la cabecera de S57 —donde vivía— ya está archivada en la
bitácora, y el plan debe conservar lo que FALTA, no solo lo hecho.

- [x] Bloque 8.1 — Vía REST de generación + persistencia (S57). POST /api/horarios;
      raíz de la que cuelga el resto. D29 cerrada PARCIALMENTE (tiempo/semilla/vía
      expuestos; vía = OPTIMIZACION únicamente, FACTIBILIDAD y warm-start NO expuestos).
      SeedHorarioRunner partido en SeedCatalogoRunner.
- [x] Bloque 8.2a — Pin de instancia a TRAMO en el solver (S58). SesionBloqueada
      estructural + restricción dura + verificador + I/O de test. Cierra el criterio 5
      de Fase 3 (diferido desde S17).
- [x] Bloque 8.2b-i — Pin de AULA por-plaza en el solver (S60) + rediseño §4.7/S5 del
      modelo (corrige el error de cardinalidad: el aula no cuelga de la instancia).
- [x] Bloque 8.2b-ii — Persistencia JPA de los bloqueos (S61). Entidades SesionBloqueada
      + AulaBloqueada + repos + BloqueoMapper de entrada + cableado del placeholder de
      CatalogoMapper.
- [x] Bloque 8.2b-iii-A — Cableado del servicio a los repos de bloqueo (S62). Cierra el
      lazo end-to-end: un solve por POST /api/horarios respeta los pines persistidos.
      Deuda (a) y (c) de 8.2b-ii CERRADAS.
- [x] Bloque 8.2b-iv — Entrada del bloqueo por REST (S66). BloqueoController + BloqueoService
      + 4 DTOs. Contrato de S62 respetado (endpoint propio /api/bloqueos). El pin de tramo es
      el recurso y los pines de aula su contenido: no hay endpoint de aula suelto porque el
      dominio no lo admite (BloqueoMapper aborta) y la API no debe poder escribir estados que
      el consumidor rechaza. POST idempotente con REEMPLAZO TOTAL (PUT semántico: body sin
      "aulas" deja el pin sin aulas; no hay merge parcial). El tramo se referencia por
      (dia, ordenEnDia), el mismo par que ya lleva SesionVistaDTO, INVIRTIENDO
      CatalogoMapper.indiceOrdenEnDia (fuente única: NO se reimplementa la renumeración, D30
      no se agrava con un tercer espejo). Solo app/: sin tocar solver/src/main.
- [x] Bloque 8.3-A — Atribución ESTRUCTURADA de reglas DURAS por celda (S64, D19 backend
      parcial). ResultadoVerificacion: List<String> → List<Violacion>. Tipos ReglaDura +
      CeldaRef(actividadCodigo, indice 1-based, plazaCodigo nullable) + Violacion(regla,
      recursoCodigo, tramoCodigo, celdas, descripcion). Una violación = N celdas. Asimetría D15
      protegida por test (aula por PLAZA, resto por INSTANCIA). Solo solver/: sin REST.
- [x] Bloque 8.3-B — Atribución CONTRAFACTUAL de reglas BLANDAS por celda (S65). CIERRA D19
      BACKEND. La decisión de diseño que S64 dejó abierta se resolvió mostrando que la pregunta
      estaba mal planteada: los tres gemelos no son del mismo tipo (indisponibilidad blanda es
      LOCAL —su bucle ya sabe qué instancia penaliza—; ventanas y consecutivas son NO-LOCALES —la
      penalización es propiedad de un CONJUNTO de posiciones, ninguna celda la "causa"—). La
      atribución blanda NO es culpabilidad sino CONTRAFACTUAL: delta = penalización_actual −
      penalización_si_esa_celda_no_estuviera, CON SIGNO (delta<0 = la celda tapa un hueco; mover
      empeora). El atribuidor USA al gemelo como oráculo: las fórmulas se EXTRAEN a funciones
      puras ventanasDe/excesoConsecutivasDe que llaman AMBOS — una sola definición, coste O(1)
      por celda, independencia de CP-SAT conservada por construcción. Tipos ReglaBlanda +
      Penalizacion + AtribucionBlanda; ResultadoVerificacion/Violacion NO se tocan (una
      penalización blanda no es una violación) y por tanto D15 queda fuera del radio. Solo
      solver/: sin REST.
- [x] Bloque 8.3-C — REST de atribución: GET /api/horarios/{id}/diagnostico (S67). La pregunta
      abierta de S66 ("cómo llega la solución a verificar") se resolvió al fijar el modelo de
      interacción (D-F8.6-A-1, vía C): NO hay solución candidata que transportar, luego la
      solución se RECONSTRUYE desde BD. SolucionMapper.aSolucionHorario es el INVERSO de
      aSesiones: obtiene la correspondencia Tramo↔TramoSemanal INVIRTIENDO el mapa que
      indiceTramos ya construye —D30 gana un CONSUMIDOR, no un espejo—. CORRIGE A S66: el plan
      afirmaba que reconstruir exigiría "un TERCER espejo de la renumeración"; es FALSO, y S66 lo
      dedujo sin haber leído indiceTramos. aulasElegidas se reconstruye OMITIENDO las plazas con
      aulaFija (fidelidad, no equivalencia: D-F8.3-C-3), con guarda de corrupción si el aula
      persistida contradice la fija. SolucionHorario gana un getter aulasElegidas() —única línea
      de solver/src/main tocada, estrictamente aditiva— porque sin él la fidelidad solo era
      verificable por reflexión, y un invariante que exige violar el encapsulamiento para
      comprobarse no es un invariante (D-F8.3-C-6). DTO: violaciones + penalizaciones + totales;
      las duras vienen VACÍAS en un horario del solver (red de seguridad visible, no diagnóstico).
      SesionVistaDTO NO tocado. D19 CERRADA salvo su parte de UI (8.6).
- [x] Bloque 8.4-A — Pre-validación por CONDICIONES NECESARIAS (S79). 8.4 partido en A (backend) y
      B (presentación, D20). Tres reglas ERROR en `PrevalidacionService` (núcleo ESTÁTICO
      `prevalidar(ProblemaHorario)`: inyectar el bean daría ciclo Spring), deduplicando POR ACTIVIDAD
      y no por plaza. `GET /api/prevalidacion` + guarda en `generar()` → 422. MECANISMO VIVO que S93
      midió y esta casilla debe conservar: los DOS 422 llegan al frontend con BODY SECO IDÉNTICO
      —`HorarioController:64-67` lanza `ResponseStatusException(422, e.getMessage())` y el `reason`
      solo viajaría con `server.error.include-message`, que `application.properties` no define
      (D-F8.6-ii-a)—; la distinción prevalidación↔infactible es real en la capa de excepciones y NO
      observable desde el cable. (b) palomar de aulas FUERA por riesgo de falso positivo.
      → D-F8.4-A-a, D-F8.4-A-b, D-F8.4-A-c (reencuadrada en S92); D-F8.6-iiiA-c. Detalle: bitácora S79.
- [x] Bloque 8.4-B1 — Panel de pre-validación en el frontend (S92). 8.4-B PARTIDO en B1/B2 por
      MEDICIÓN: no existía gesto de generar que envolver. MECANISMO VIVO: `avisos()` es
      `signal<T[]|null>` con inicial `null` —la señal que separa «no ejecutado» de «ejecutado y
      vacío»—, `severidad` viaja como `string` y no union type (un tercer valor degrada, no rompe), y
      CUATRO RAMAS con clases DISJUNTAS. → D-F8.4-B1-a; D-F8.4-A-c REENCUADRADA. Detalle: bitácora S92 (futura).
- [x] Bloque 8.4-B2 — GESTO DE GENERAR + guarda con diálogo (S93). CIERRA el frente 8.4 entero.
      La partición recomendada se RETIRÓ por medición (el POST acepta body vacío; el CDK ya estaba en
      `dependencies`). MECANISMO VIVO: `generar()` en `horario.service.ts` con body `{}` —el backend
      hace TODO el defaulting—; `components/confirmar-generacion/` es el PRIMER diálogo del repo sobre
      `@angular/cdk/dialog`, PRIMITIVO SIN ESTILO, y `styles.css` importa `overlay-prebuilt.css`
      (PRIMERA hoja global del CDK), sin la cual el overlay se monta sin centrar ni backdrop; TRES
      ESTADOS del gesto, con `avisosPrevalidacion() === null` dejando el botón DESHABILITADO; tras el
      200, `router.navigate(['/horario', dto.id])` —ruta SINGULAR— DESCARTANDO la proyección que
      devuelve el POST, para que la recarga la dispare `paramMap` y rejilla, pines y diagnóstico no
      puedan pertenecer a horarios distintos (opción A del usuario; coste: un GET redundante). D5
      REVOCADA: el diálogo enumera los errores por adelantado. La validación amable del bloqueo
      contradictorio NO entra (familia de D-F8.6-ii-a y D-F8.6-iiiA-c). → D-F8.4-B2-a. Detalle: bitácora S93 (futura).
- [x] Bloque 8.5-A/A'/B — CRUD de catálogo: raíces + grupos/subgrupos (S69/S70/S71) → sin deuda viva; Detalle: bitácora S69/S70/S71.
- [x] Bloque 8.5-C1 — CRUD de Actividad como AGREGADO, Plaza embebida sin /api/plazas (S72) → XOR aula, I7, I2 validadas en ActividadService; Detalle: bitácora S72.
- [x] Bloque 8.5-C2a-DDL — Integridad referencial de ESQUEMA: `schema.sql` + `ddl-auto=none` + 27 FK + `PRAGMA foreign_keys=ON` por conexión (S73) → D-F8.5-C2a-a (.db preexistente con PK NULL); Detalle: bitácora S73.
- [x] Bloque 8.5-C2b — Borrado amable: `ReferenciaEntranteException` + conteos inversos → 409 (S74) → CIERRA D-F8.5-A-a entera; cada controller traduce sus propias excepciones, sin @ControllerAdvice; Detalle: bitácora S74.
- [x] Bloque 8.5-C3 — I3 + CRUD de compatibilidades asignatura↔tipo de aula, semántica (C): 0 filas ⇒ irrestricta (S75) → validada en el funnel único `resolverContenido`; D-F8.5-C3-a (COMUN sin semántica), D-F8.5-C3-b (códigos por currículo); Detalle: bitácora S75.
- [x] Bloque 8.5-D1 — PDC como sub-recurso `/api/grupos/{idPadre}/pdc` + subgrupo mono-Di automático (S76) → un PDC por padre → 400; D-F8.5-D1-b (el 1:1 es decisión del arquitecto); Detalle: bitácora S76.
- [x] Bloque 8.5-D2a — `ProfesorTutoria` (@IdClass) + I4 en escritura + herencia PDC←padre por copia (S77) → CIERRA D-F8.5-D1-a; tutoría = población propia del Grupo (cascade) y referencia entrante del Profesor (409); D-F8.5-D2a-a (I4 sin red en BD), D-F8.5-D2a-b (incoherencia 404/400); Detalle: bitácora S77.
- [x] Bloque 8.5-D2b-1 — TRANSPORTE de la tutoría al solver (S90). CIERRA D-B5-5. `Actividad` gana
      7.º componente `boolean requiereTutor` (primitivo; el WRAPPER `Boolean` vive solo en el DTO,
      porque los 43 fixtures vivos omiten el campo y el mapper colapsa `null→false`);
      `ProblemaHorario` gana DÉCIMA lista `tutorias`; `ProfesorTutoria(Profesor, GrupoAdministrativo,
      RolTutoria)` y `RolTutoria` son PROPIOS de solver —no se reutiliza el enum de `app/`: solver no
      depende de app— y con el ORDEN DE LA ENTIDAD JPA de S77, no el de la prosa de S8.
      La décima lista se prefiere a meter tutores en `GrupoAdministrativo` por CUATRO razones: el
      `rol` se perdería, `GrupoAdministrativo` se usa como elemento de `Set` y clave de mapa (su
      `equals` quedaría envenenado), la 2.ª pasada ya tiene ambos catálogos construidos, y
      `resolverGrupo` es recursivo con detección de ciclos donde inyectar `Profesor` acopla dos
      catálogos. `CatalogoMapper.aActividad` PROPAGA `isRequiereTutor()`: clavar `false` dejaría
      entidad=true/dominio=false por la puerta de entrada real de configuración y haría S8
      verificable en fixtures e inverificable en producción. D-F8.5-D2b1-a, D-F8.5-D2b1-b.
      Detalle: bitácora S90.
- [x] Bloque 8.5-D2b-2 — S8 VERIFICABLE por el solver (S91). CIERRA D-F8.5-D2b1-a y -b, y CIERRA
      el frente 8.5-D2b entero. SEDE: `VerificadorSolucion`, NO validación de catálogo —el modelo
      dejaba la disyuntiva abierta; pre-validación es superficie REST distinta (422 propio) y
      abriría D-F8.4-A-c—. `ReglaDura.TUTORIA_SIN_TUTOR` (no propaga: `ViolacionDTO.regla` es un
      `String` con `.name()`, así que el enum NO cruza a `app` como tipo, y nadie hace `switch`
      exhaustivo). `verificarTutorias` es el QUINTO acumulador y el ÚNICO método del fichero que no
      recibe ni usa `solucion`: S8 es propiedad del CATÁLOGO, no de la solución, y va en TSDoc para
      que nadie lo «uniforme». `Violacion` NO necesitó forma nueva —dos de las siete reglas ya ponen
      `tramoCodigo=null`—: S8 calca `DISTRIBUCION_MISMO_DIA`, con `recursoCodigo`=código del GRUPO
      (lo que falta es un profesor, no lo hay sobrante). Cobertura grupo←subgrupo CIEGA al
      `grupo_padre`, criterio de S9: un PDC que no heredó `TUTOR_PRINCIPAL` falla S8 legítimamente.
      El CABLEADO no era inyectar un repositorio —`aProblemaHorario` es `static` puro— sino un
      ONCEAVO PARÁMETRO, con el `findAll()` en `GeneradorHorarioService.cargarProblema`, dentro de
      su `@Transactional(readOnly=true)` (los `@ManyToOne(LAZY)` de la PK se navegan en sesión).
      Se transportan AMBOS roles y el filtro `TUTOR_PRINCIPAL` vive en el verificador: filtrar
      `CO_TUTOR` en el mapper reproduciría la asimetría con la que S90 justificó propagar en
      `aActividad`. Fixture 44.º `problema-8-5-s8.json` con tutoría TRAMPA. 13 tests, campaña de 12.
      Suite 321 → 333. Referencia regenerada. D-F8.5-D2b2-a, D-F8.5-D2b2-b. Detalle: bitácora S91.
- [ ] Bloque 8.5-D3 — Particion/SubgrupoParticion + UX de subgrupo compartido (D7, I1, I6).
      APLAZADO INDEFINIDAMENTE (S77), decisión explícita, no arrastre. Son dos tablas que el
      SOLVER NO LEE; existirían solo para que I1 sea verificable y para la UX de D7. Su
      no-materialización fue deliberada (D-a del Bloque 4/S48) y nada desde entonces la ha
      contradicho. CRITERIO DE REAPERTURA: solo si (a) la UX de D7 pasa a requisito real de
      usuario, o (b) I1 falla en producción sin que nadie lo detecte. Consecuencia asumida:
      I1 sigue sin verificador, igual que hoy.
- [x] Bloque 8.5-E — CRUD REST de `ProfesorRestriccionHoraria`, sub-recurso GET/PUT con reemplazo total (S78). CIERRA 8.5 → `peso` NO se expone (ModeloCpSat usa la constante `PESO_INDISP_BLANDA` y nunca lee `r.peso()`); D-F8.5-E-a, D-F8.5-E-b, D-F8.5-E-c, D-F8.5-E-d; Detalle: bitácora S78.
- [x] Bloque 8.6-i — Cliente REST de bloqueos (S81): `bloqueo.model.ts` + `BloqueoService` TS + `pines.ts`
      (`clavePin`, `indicePines`); `HorarioService` intacto. El `GET /api/bloqueos` es precondición del
      CANDADO, no del POST (idempotente por instancia). NO toca backend: `SesionVista` ya lleva la clave
      de cruce (`actividadCodigo`, `indice`) de D-6. Detalle: bitácora S81.
- [x] Bloque 8.6-ii — Arrastre que pina (S81): `@angular/cdk@21` `DragDropModule` (pointer events, el
      runner Vitest+jsdom no implementa HTML5 DnD); envoltorio de celda por PAR vía `agruparPorActividad`
      NUEVA. `cdkDrag` en la INSTANCIA, nunca en la sub-entrada (contrato S67, D-F8.6-A-2). SIN movimiento
      optimista (D-F8.6-ii-5). Pin solo de tramo. D-F8.6-ii-a, D-F8.6-ii-b. Detalle: bitácora S81.
- [x] Bloque 8.6-iii-A — Contrato de lectura del diagnóstico en el cliente (S82): `diagnostico.model.ts`
      (espejo de los 5 DTOs con la ASIMETRÍA D15 copiada), `DiagnosticoService` TS y `horario/diagnostico.ts`
      con `indiceViolaciones` e `indicePenalizaciones` SIN sumatorio de `delta`. `Observable` crudo, sin
      traducir el 404. NO toca backend (`CeldaRefDTO` ya trae la clave de D-6). D-F8.6-iiiA-a, D-F8.6-iiiA-b,
      D-F8.6-iiiA-c. Detalle: bitácora S82.
- [x] Bloque 8.6-iii-B1 — Gesto de despinar e índice de pines con `id` (S83): `indicePines` pasa a
      `Map<clave, number | null>`; candado `<span>`→`<button>`, `despinar = output<string>()` con la CLAVE
      (identidad de dominio D-6, no de fila); `alDespinar` resuelve clave→id y hace no-op si null; `listar()`
      MOVIDO a `cargar(id)`. `border-left` liberado para B2. CIERRA D-F8.6-ii-b. D-F8.6-iiiB1-a, D-F8.6-iiiB1-b,
      D-F8.6-iiiB1-c. Detalle: bitácora S83.
- [x] Bloque 8.6-iii-B2-a — Cableado del diagnóstico + badge del delta blando (S87): la capa de diagnóstico
      llevaba desde S82 construida y probada pero DESCONECTADA (medido en §A), así que B2 NO era pintura.
      `sumaDeltasPorInstancia` NACE EN LA CAPA PURA, suma CON SIGNO y no emite las claves de suma 0 (semántica
      de S65). `getDiagnostico` dentro de `cargar(id)` y NO por analogía con `cargarPines()` global
      (D-F8.6-iiiB1-b); `errorDiagnostico` con selector propio que no gatea la rejilla. D-F8.6-iiiB2a-a.
      Detalle: bitácora S87.
- [x] Bloque 8.6-iii-B2-b — Los DOS resaltes de violación (S88): aula por SUB-ENTRADA, profesor/subgrupo por
      CELDA (asimetría D15 pintada, no aplanada). §A DESMINTIÓ el mockup de S87: el resalte va SOLO a `outline`
      —`border-left` OCUPADO (3px `#4a7` estructural) y `background` OCUPADO en dos capas, la segunda es la
      señal de pinada—. Input ÚNICO `violaciones` con predicados `.some()` por sub-entrada. `horario/diagnostico.ts`
      NO tocado. Hereda D-F8.6-iiiA-b (`Totales` sin sede; los totales NO son la suma de los `delta`). Suite
      41 → 46. CIERRA el frente 8.6-iii y D19/D20 en frontend. Detalle: bitácora S88.
- [x] Bloque 8.6-iv-A — Specs de los TRES servicios del frontend (S85): tests (8)..(12) junto a su fuente en
      `services/`; ESTRENA la capa HTTP de test (`provideHttpClientTesting` + `HttpTestingController`). ALCANCE
      HONESTO: wrappers pelados, CONGELACIÓN DEL CONTRATO DE ENDPOINTS, no cobertura de lógica. Suite 30 → 35.
      CIERRA D-F8.6-iiiA-a con MATIZ MEDIDO. → D-F8.6-ivA-a, D-F8.6-ivA-b, D-F8.6-ivA-c. Detalle: bitácora S85.
- [x] Bloque 8.6-iv-B — Capa de test de COMPONENTE (S84): `horario-view.spec.ts` (5) + `horario-grid.spec.ts` (2);
      dobles por `useValue` con `Subject` PELADO (sin él no existe la mitad «ANTES» que discrimina el borrado
      optimista de D-F8.6-ii-5). Suite 23 → 30. CIERRA D-F8.6-iiiB1-a. → D-F8.6-ivB-a, D-F8.6-ivB-b, D-F8.6-ivB-c.
      Detalle: bitácora S84.
- [x] Bloque 8.6-iv-C — Cobertura del camino de PINADO en el contenedor (S89; D-F8.6-ivB-a REDUCIDA, no cerrada):
      seis tests (21)-(26), `horario-view.ts` INTACTO. ANDAMIO VIVO: `guardar` devuelve un Subject FRESCO POR
      INVOCACIÓN (uno compartido queda CERRADO tras `.error()` y redispara síncronamente, haciendo INOBSERVABLE
      la fase «`errorPin` a null»). Campaña de 7, sin supervivientes. Suite 46 → 52. → D-F8.6-ivB-a-bis.
      Detalle: bitácora S89.
- [x] Bloque 8.6-iv-D — Los `set(null)` DE REINTENTO, los dos JUNTOS (S94). CIERRA D-F8.4-B2-a y el punto (a) de
      D-F8.6-ivB-a. Tests (35) y (36) en `horario-view.spec.ts`, `horario-view.ts` INTACTO; ambos aseveran la
      fase INTERMEDIA (error a `null` ANTES del segundo Subject), lo ÚNICO que discrimina el `set`. ANDAMIO
      (opción A): `borrar` y `generar` migran a FRESCO POR INVOCACIÓN, los tres dobles quedan homogéneos.
      Suite 67 → 69; backend 333 intacto. → D-F8.6-ivD-a. Detalle: bitácora S94.
- [x] Bloque 8.6-iv-E — El INVARIANTE DEL `<select>` (S96). CIERRA D-F8.6-ivB-a ENTERA (punto (b) y último
      resto). Tests (37) y (38) en `horario-view.spec.ts`, `horario-view.ts` con diff VACÍO: aseveran que
      `bloqueos.listar` SIGUE EN 1 LLAMADA tras `cambiarVista` y tras `cambiarEntidad` (métodos DISTINTOS, dos
      tests); aserto sobre el COLABORADOR y no sobre `pinadas`. CAMPAÑA POR ADICIÓN (el invariante se sostiene
      sobre una AUSENCIA de llamada: un solo call site en `cargar`). Suite 69 → 71; backend 333 intacto.
      → D-F8.6-ivD-b. Detalle: bitácora S96 (futura).
- [x] Bloque 8.6-B — Aviso de OCUPACIÓN al iniciar el arrastre (S97). CIERRA el frente 8.6 ENTERO.
      GUARDARRAÍL VIGENTE: es cruce de índices, NO verificación. Si se propone portar el verificador a TS,
      PARAR: sería un cuarto espejo de la lógica de solapes, sin el test que protege D15. OPCIÓN B (aviso sobre
      la PROYECCIÓN, «hay clase en la vista actual») sobre A (recurso completo = el cuarto espejo, prohibido) y C
      (solo pinados, insuficiente): el aviso NO PUEDE INTENTAR SER CORRECTO —en cuanto acierta, porta
      `verificarNoSolapes`—, es barato, incompleto y honesto sobre serlo. `arrastrando: signal<string | null>` +
      `computed slotsOcupados: Set<string>` que EXCLUYE el origen y cuenta `InstanciaCelda` vía `agruparPorActividad`
      (la lógica de D15 NO se reescribe). `background: #eef2f6` sobre `.rejilla td.ocupado`, LIBRE en el `<td>`
      (medido aparte, NO heredado de S88). TSDoc de `slotsOcupados` OBLIGATORIO con las tres afirmaciones (ciego
      por construcción, no es verificación). Campaña de 5 sobre 4 tests; M3 SUPERVIVIENTE declarada. T3 corregido
      en la misma sesión por R5. Suite 71 → 75. → D-F8.6-B-a, D-F8.6-B-b. Detalle: bitácora S97 (futura).

Diferibles a lo largo de la fase: D21, D22, D26/D27 (nombre de aula, código de tramo),
D30 (renumeración de tramos duplicada). D-F8.2b-4B: condicional e INERTE (la poda que
defendería está muerta en todo camino vivo). El seed (`SeedCatalogoRunner`) muere en el
bloque de configuración de jornada (D22), no en 8.5: sobrevive porque `TramoSemanal`
sigue sin CRUD.

### Fases completadas

**Fase 0 — Decisión de stack tecnológico** (cerrada).
Stack: OR-Tools CP-SAT (Java bindings) + Spring Boot Java 17 + SQLite/Hibernate 
+ Angular + jpackage app-image. Validado con "Hello World" del solver y prueba
de empaquetado Windows.

**Fase 1 — Modelo de datos validado en papel** (cerrada en Sesión 8).
Modelo unificado documentado en `modelo_datos_fase1.md`. Los 8 tipos de sesión
del enunciado original se sustituyen por la estructura
`Actividad → Plaza → Subgrupo` con particiones persistentes. Validado contra
**6 criterios** sobre datos reales del IES de Sevilla: horario completo de
1ºESO A, subgrupo PDC 3ºADi, TICO multi-grupo de 1ºBach, bloque de 3 sesiones
simultáneas que bloquean 4 grupos, horario completo de 1ºBACH B
(Sesión 4, §6.5 del modelo) y horario completo de 1ºFPB (Sesión 5,
§6.6 del modelo). En Sesión 8 se re-validó el primer criterio mediante
extracción directa del PDF (no solo del system prompt): cubierto al 100%
sin regresión.

Hallazgos posteriores al cierre inicial, registrados en el modelo:

- **Hallazgo D** (Sesión 4): un subgrupo puede aparecer en varias particiones.
  Impacto: `Subgrupo` pasa a entidad de primera clase con identidad propia,
  relación N:M con `Particion` vía `SubgrupoParticion`. Nueva invariante I6.
- **Hallazgo E** (Sesión 4): la "tutoría" puede no llamarse TUT.
  Impacto: nuevo flag `Actividad.requiere_tutor`. Invariante S8 reformulada
  para depender del flag, no del nombre de la asignatura.
- **Hallazgo F** (Sesión 4): el bloque Religión/PTVE puede ser per-grupo o
  transversal según el centro. No requiere cambios; el modelo ya soporta
  ambas configuraciones.
- **Hallazgo G** (Sesión 5): los bloques obligatorios en FPB pueden tener
  duración mayor que 2 tramos (PS martes 8–11, 3 tramos consecutivos). No
  requiere cambios; `Actividad.duracion_tramos` ya admite cualquier entero
  ≥ 1 y S6 funciona genéricamente. La UI de Fase 8 debe permitir introducir
  cualquier N sin techo predefinido.
- **Hallazgo H** (Sesión 5): los PDFs de horarios omiten aulas en celdas
  cuyo profesor es titular del grupo, y el listado por aulas no incluye el
  taller físico donde se imparten las asignaturas técnicas de FPB. No afecta
  al modelo; sí al importador de datos y a la UI de configuración (deuda D8).
- **Hallazgo I** (Sesión 5): el centro programa pares de sesiones de la
  misma asignatura cruzando el recreo (CS lunes y miércoles 10–11 + 11:30–12:30).
  Son sesiones independientes a efectos del modelo. Decisión diferida a Fase 5
  (deuda D9).
- **Hallazgo J** (Sesión 7): patrón de co-docencia intra-aula
  (un grupo, una aula, dos o más profesores en la misma asignatura).
  Confirmado como estructural en LCL de 1ºESO (todos los grupos A/B/C/D).
  Impacto: pluralización de `Plaza ↔ Profesor` de 1:1 a M:N simétrica
  vía nueva tabla `PlazaProfesor`. Nueva invariante I7 ("toda plaza tiene
  al menos un profesor"). Reformulación textual de S1 y S8 (semántica
  equivalente, cómputo vía tabla M:N). Reescritura del bloque LCL en
  §6.1 del modelo: la antigua `Particion "LCL-1ESO"` se elimina (no
  había tal partición; el grupo no se divide). LCL en 1ºA queda en
  4 sesiones de co-docencia, total semanal sigue siendo 30. Nueva
  deuda D10 (UX Fase 8).

**Fase 2 — Solver MVP: problema mínimo** (cerrada en Sesión 13).
Solver CP-SAT en producción para 4 grupos de 1ºESO con sesiones
ordinarias + co-docencia LCL. Los 7 criterios de verificación se cumplen
mecánicamente sobre el dataset real, comprobados por
`SolverHorario1EsoOrdinariasTest` (factibilidad + 0 violaciones de
restricciones duras) y `Main1EsoOrdinariasTest` (end-to-end con código
OK). Co-docencia (M:N `Plaza↔Profesor`) soportada desde el MVP, no
añadida después. Distribución temporal modelada como restricción dura
(`AllDifferent` por día sobre actividades DISTRIBUIDA), promovida de
blanda a dura para mantener factibilidad pura sin función objetivo.
`VerificadorSolucion` es la fuente única de verdad para violaciones,
reutilizado por tests del solver y por el `Main`. Estructura:
módulo `solver/` con paquetes `domain` (records puros, sin OR-Tools),
`cpsat` (OR-Tools aislado), `io` (loader Jackson + mapper) y `cli`
(`Main`). Sesiones: 6–8 (replanteo del dominio y Hallazgo J), 9
(sub-plan táctico de la fase), 10–13 (Bloques 3–6, uno por sesión).

**Fase 3 — Solver: desdobles y agrupamientos** (cerrada en Sesión 17,
con una salvedad). Criterios 1-4 validados sobre dataset REAL: bloque
coordinado CyR/OyD/RefMt de 1ºESO (§6.1 del modelo), una actividad de 6
plazas (repeticiones=2, NEUTRA) cuyos 24 subgrupos transversales cubren
los 4 grupos, más 4 actividades Mat testigo (una por grupo) para que S9
tenga algo que expulsar. Ejercita commit 1 (no-solape por grupo, S9) y
commit 2 (elección de aula entre candidatas) juntos.

Evidencia por criterio:
- C1 (mismo tramo desdoble CyR): cubierto POR CONSTRUCCIÓN — las 6 plazas
  son una sola Actividad, un IntVar de tramo por instancia. El test
  documenta colocación de las 2 instancias y su separación en tramos
  distintos (forzada por S1/S2/S3/S9 sobre recursos compartidos, no por
  distribución por día).
- C2 (ningún grupo con otra sesión en el tramo del bloque): ninguna Mat
  testigo de los 4 grupos cae en los tramos del bloque (S9).
- C3 (3 profes RefMt libres): MAT6/MAT7/MAT4 solo en el bloque + 0
  colisiones de profesor del verificador independiente.
- C4 (aulas distintas y correctas): A12In fija para INF1, TEC3 en
  candidata válida {A5,B07}, 3 aulas RefMt distintas, 6 aulas del bloque
  distintas — afirmado por instancia. Cubre D15 explícitamente para este
  fixture (el verificador no lo haría: cuenta aula por instancia con Set).

Salvedad: C5 (bloqueo manual de tramo) DIFERIDO — sin mecanismo en el
modelo. No marcar la fase como cerrada al 100% sin esta nota.

Entregado: fixture problema-3-cierre-cyr-refmt.json (validado contra
schema) + SolverHorarioCierreFase3Test. Suite: 27 tests en verde. Sin
cambios de API (no se regenera el índice de código).

**Fase 4 completada.

### Cierre del modelo — Sesión 8

Validación contra el horario real de 1ºESO A del PDF (extracción directa
del documento, no solo del system prompt). Resultado: el modelo cubre el
100% de la complejidad estructural del grupo. No se detectaron casos no
modelables. Los 6 criterios de verificación de Fase 1 siguen vigentes
sin regresión.

Cambios aplicados a `modelo_datos_fase1.md` en esta sesión:

- **Aplicación del Hallazgo J al esquema** (estaba pendiente desde
  Sesión 7): eliminada la FK `Plaza.profesor_id`, añadida la tabla M:N
  `PlazaProfesor` en §4.6, añadida la invariante I7 en §5.1,
  reformuladas las invariantes S1 y S8 en §5.2 para operar sobre el
  conjunto `Plaza.profesores`. Hallazgo J registrado en §2 del modelo
  junto con A–I. Nueva entrada en glosario para "Co-docencia intra-aula".
- **Eliminación del campo `Actividad.tipo`** (decisión nueva,
  Sesión 8): el discriminador estructural se infiere del contenido
  (número de plazas, profesores por plaza, subgrupos cubiertos). Una
  etiqueta redundante puede desincronizarse del contenido real. La UI
  calcula etiquetas legibles a partir del contenido en la capa de
  presentación. Limpieza de las 11 referencias `tipo=...` en los
  ejemplos de §6.2–§6.5.
- **Corrección de §6.1 (LCL co-docencia)**: eliminada la
  `Particion "LCL-1ESO"` obsoleta y los subgrupos
  `1ºA-LCL-LEN2`/`1ºA-LCL-LEN8`. Reescrita la actividad LCL-1ºA como
  una sola plaza con `profesores={LEN2, LEN8}` y aula A5, subgrupo
  "1ºA-Completo".
- **Corrección de EF en §6.1**: `repeticiones=2` → `repeticiones=3`.
  Las tres celdas de EF del horario de 1ºA aparecen con el grupo
  completo en Gim. El conteo total de 30 sesiones se reconcilia con
  esta corrección (la versión anterior tenía una incoherencia interna
  entre las actividades enumeradas y el resumen).
- **Ampliación de la deuda D8** con tres puntos numerados: omisión de
  aulas en horario por grupo, omisión de aulas físicas en horario por
  aula, e **inconsistencia profesor↔plaza entre los dos PDFs**
  (detectada: EF mié 9:00 aparece como EFI2 en el horario del grupo
  1ºA y como EFI3 en el horario del aula Gim). El importador debe
  cruzar los dos listados y reportar inconsistencias al usuario.
- **Nueva deuda D10** en `modelo_datos_fase1.md` (ya existía en este
  plan): UX de configuración de plazas multi-profesor en Fase 8.

Decisiones permanentes nuevas añadidas a la tabla siguiente: campo
`Actividad.tipo` eliminado.

Tras estos cambios, el modelo de datos cierra como referencia
autoritativa de Fase 1 y queda listo para empezar Fase 2.

### Decisiones permanentes (no reabrir sin razón de peso)

| Capa | Decisión |
|---|---|
| Solver | OR-Tools CP-SAT — bindings Java (ortools-java 9.11.4210) |
| Backend | Spring Boot 4.1.0 (GA) + Java 17. Versión fijada en S45 (Fase 6, Bloque 1) al declarar el módulo app/ |
| Base de datos | SQLite + Hibernate 7.4.1, dialecto de comunidad org.hibernate.community.dialect.SQLiteDialect (Hibernate no trae dialecto SQLite oficial; el best-effort funciona sobre Hibernate 7.4, verificado en S45). **Esquema vía `schema.sql` + `ddl-auto=none` desde S73** (antes hbm2ddl; reabierto conscientemente el "sin Flyway por ahora" y también descartado hbm2ddl para el esquema, porque el dialecto de comunidad NO emite FK en el DDL, rompe `ddl-auto=validate` y genera la PK `id` sin tipo —inservible como destino de FK en SQLite—; ver Notas técnicas Fase 6 y 8.5-C2a-DDL). Integridad referencial ACTIVA = dos piezas: FK declaradas en `schema.sql` + `PRAGMA foreign_keys=ON` por conexión vía customizer (ni `connection-init-sql` ni el parámetro de URL lo propagan en este stack). Flyway se evaluó y se descartó (peso sin retorno: una BD por centro, esquema casi estático); queda como upgrade futuro si hiciera falta migrar esquemas en caliente. Fichero local, ruta relativa al working dir |
| UI | Angular 21 (standalone components, `signal`). Router ya provisto (`provideRouter`). **Tests del frontend con Vitest** (builder `@angular/build:unit-test`), NO Karma: invocación `npm test` a secas; `--browsers=ChromeHeadless` u otros flags de navegador fallan (exigen `@vitest/browser-*` no instalado). Shell de navegación (landing + secciones) desde S100, O-shell |
| Empaquetado Windows | jpackage app-image (bundle portable, sin instalador) |
| Estructura del repositorio | Maven multimódulo. Módulo `solver` (POJOs + OR-Tools, sin Spring ni Hibernate) y módulo `app` (Spring Boot, persistencia, REST) introducido en Fase 6. Frontend Angular en directorio adyacente, integrado vía `frontend-maven-plugin` en Fase 7-8 (Sesión 6, decisión 1) |
| Multi-centro | Descartado. Una BD = un centro. Curso nuevo = duplicación de BD (Fase 10) |
| Unidad atómica del solver | Subgrupo de alumnos, no grupo administrativo |
| Asignación profesor↔plaza | Configuración humana, no decisión del solver |
| Plaza ↔ Profesor | M:N simétrica vía `PlazaProfesor` (sin rol titular/apoyo). Una plaza tiene 1..N profesores. Co-docencia intra-aula = `|PlazaProfesor|≥2`. Invariante I7: al menos un profesor por plaza (Sesión 7, Hallazgo J) |
| Campo `Actividad.tipo` | **Eliminado.** La naturaleza estructural (ordinaria, co-docencia, desdoble, agrupamiento, bloque de optativas, PDC transversal) es inferible del contenido (número de plazas, profesores por plaza, subgrupos cubiertos). La UI calcula etiquetas legibles a partir del contenido en la capa de presentación, no como columna persistida (Sesión 8) |
| Asignación aula↔plaza | Fija por defecto, candidatas opcionales |
| Variables del solver | `IntVar` + `IntervalVar` por `ActividadInstancia` con `AddNoOverlap` por profesor, aula y subgrupo. Los `BoolVar` reified se introducen puntualmente cuando una restricción blanda concreta lo requiera (Sesión 6, decisión 2) |
| PDC | GrupoAdministrativo virtual con grupo padre y subgrupo persistente |
| Identidad de Subgrupo | Entidad de primera clase, independiente de Particion. Relación N:M vía SubgrupoParticion (Sesión 4, Hallazgo D) |
| Marca de tutoría | Flag `Actividad.requiere_tutor`, no nombre de la asignatura (Sesión 4, Hallazgo E) |
| Religión/PTVE | Configurable per-grupo o transversal según el centro (Sesión 4, Hallazgo F) |
| Persistencia en Fases 2-5 | POJOs puros, datos en JSON. Sin Hibernate hasta Fase 6. Builders Java disponibles únicamente para fixtures de tests unitarios (Sesión 6, decisión 3) |
| Formato de entrada del solver en Fases 2-5 | JSON con DTOs y mapper a dominio. Referencias entre entidades por código de negocio, no por id. Validación de invariantes I1–I3, I6 e I7 en la carga, antes de invocar al solver (Sesión 6, decisión 3; I7 añadida en Sesión 7, Hallazgo J) |
| Modelo del solver vs modelo de persistencia | Entidades JPA y modelo del solver son clases distintas conectadas por mapper explícito en Fase 6 |
| Restricciones del profesorado | Tabla única con tipo (DURA/BLANDA) y peso |
| Distancia entre aulas | Fórmula sobre (edificio, planta, sector) + excepciones |
| Distribución temporal | Campo `patron_temporal` en Actividad (DISTRIBUIDA/AGRUPADA/NEUTRA) |

- Separación loader/mapper: Jackson queda aislado en ProblemaHorarioJsonLoader; el ProblemaHorarioMapper es puro y testeable sin I/O.
- Reparto de validación en la carga: los records de dominio auto-validan I5, I7, el XOR aulaFija/aulasCandidatas y los rangos. El mapper posee en exclusiva: integridad referencial por código, códigos duplicados, I2 y la política "aulasCandidatas rechazado hasta Fase 3".
- I1, I3 e I6 no se validan en la carga del JSON del solver (el JSON no transporta particiones). Se validan en la capa de configuración (Fase 6/8). Corrige el alcance de la decisión de Sesión 6, que las incluía antes de existir el dominio reducido.
- **Criterio de higiene documental del plan** (S63). Al archivar o condensar cualquier cosa del
  plan rigen dos reglas, en este orden:
  R4 (guardarraíl de tokens): ningún identificador (Dxx, D-Bx-y, Cx, §x.y) puede quedar sin citante
  vivo NI sin definición viva. Se verifica por grep contra el fichero, no por inspección visual. Si un
  recorte dejaría un token huérfano, NO se recorta: se para y se decide.
  R5 (mecanismo vivo ≠ historia): el texto que describe CÓMO SE COMPORTA EL SISTEMA o QUÉ QUEDA
  PENDIENTE es estado vivo y no se archiva, aunque no tenga identificador. Nombres de clases, métodos y
  comportamientos de src/main son estado vivo. Una deuda se cierra IMPLEMENTANDO algo: su línea
  condensada conserva esa implementación.
  Origen: S62 perdió la descomposición de Fase 8 por archivarla sin comprobar que era la única copia
  viva; S63 estuvo a punto de perder D-B8-1 y el mecanismo de D13.

### Método de trabajo (procedimiento vigente)


El texto íntegro del método (M0–M5, R4/R5, M1-bis, M1-ter, M-mockup, tipos de sesión
y automatización del cierre) vive ahora en `metodo.md`, documento de referencia
estable. Aquí se conservan los TOKENS con una línea cada uno —R4 exige que sigan
definidos porque las cabeceras de sesión y D-F8.0-a los citan—; el detalle está en
`metodo.md`. Origen de que el método se escriba y no viva solo en prompts: D-F8.0-a.

- **M0** — Apertura: la sesión nombra su Cambio, Objetivo e Hito antes de fijar
  alcance; si no puede, no se abre. Una deuda solo abre sesión propia si bloquea el
  objetivo activo (R-deuda).
- **M1** — Cierre de sesión, ocho pasos (M1.0 registrar objetivo/cambio avanzado …
  M1.7 nombre, M1.8 prompt siguiente). **M1-bis**: archivado con tres rotaciones e
  invariante de una sola cabecera H3 viva. **M1-ter**: prompt de la sesión siguiente,
  no fija alcance, ≤~60 líneas. M1.5 = evaluar limpieza; M1.6 = verificar R4/diff/costura.
- **M2** — Medición previa con el instrumento más barato, salida literal sin
  interpretar, antes de fijar alcance. Cuatro precisiones (corregir afirmación viva
  al desmentir el plan; declarar qué se midió; medir tipos compartidos en todos los
  módulos; condensar una quinta precisión en vez de añadirla).
- **M3** — Campaña de mutación: un aserto vale lo que la mutación que lo mata; suite
  NO vacía demostrada. Se aplica donde hay lógica que mutar.
- **M4** — Contraste con Claude Code antes de teclear; aserto discriminante, no
  propósito; turno de medición inter-módulos ANTES del contrato si toca tipo
  compartido (S90). Artefactos derivados: regenerar `referencia-codigo-solver.md` si
  se toca `solver/src/main`; doc en commit aparte del código.
- **M5** — Criterio de terminado y parada: un objetivo termina al cumplir su criterio;
  las mejoras que no lo cambian no se ejecutan dentro de él.
- **Tipos de sesión** (Desarrollo / Saneamiento / Configuración-UI / Higiene-Método):
  ritual proporcional; M2 y M4 en casi todos, M3 solo donde hay lógica. Detalle y
  tabla en `metodo.md`.


### Deuda consciente VIVA

Deuda técnica y de dominio ABIERTA. Esta sección es la FUENTE del texto íntegro de
cada deuda; su CLASIFICACIÓN (deuda técnica real / mejora futura / decisión
arquitectónica consciente / limitación conocida), su objetivo asignado y su
disposición (si bloquea, si se paga ahora, si sale de la cola) viven en
`gestion_proyecto.md` §4. Desde la adopción del sistema de objetivos, la deuda NO
planifica el trabajo: solo se salda si bloquea el objetivo activo (R-deuda). Incluye
las simplificaciones registradas en Fase 1 (D1-D12), cuya descripción completa vive
en `modelo_datos_fase1.md` sección 8, y la deuda surgida en fases posteriores,
descrita aquí. La deuda ya CERRADA se archiva condensada en la sección siguiente,
con remisión a la bitácora.

- **D1**: Generación automática de subgrupos por plantilla → Fase 8 (UI)
- **D2**: Versionado intra-BD de cursos académicos → Fase 10 (si se requiere)
- **D3**: Validación de capacidad de aulas → Fase 5 (evaluar con datos reales)
- **D5**: Asignaturas alternativas dentro del mismo grupo (MCCSS vs Latín) → 
  Fase 5 si resulta incómodo el mini-bloque de optativas
- **D6**: Vínculo entre actividades distintas que deban ir en el mismo tramo →
  Fase posterior si aparece el caso
- **D7**: UX de subgrupos compartidos entre particiones → Fase 6 (persistencia)
  y Fase 8 (UI). El modelo permite que un subgrupo aparezca en varias
  particiones; la capa de aplicación debe gestionar la edición coherente
- **D8**: Aulas implícitas e inconsistencias entre horarios de origen →
  Fase 6 / utilidad de importación. Cuatro problemas operativos:
  (1) el importador no puede asumir que el horario por grupo contenga
  el aula en todas las celdas (se omite cuando el profesor es titular
  del grupo);
  (2) el horario por aula no contiene los talleres físicos de FPB;
  (3) el horario por aula omite sistemáticamente al segundo (y
  sucesivos) profesores en celdas de co-docencia intra-aula —
  verificado en Sesión 13 sobre las 16 celdas de LCL de 1ºESO: el
  horario por grupos lista la pareja LEN2+LEN8 / LEN3+LEN9 / LEN6+LEN9
  / LEN3+LEN6 en cada celda, mientras que el horario por aulas
  A5/A11/A3/A14 lista solo el primer profesor; es patrón de
  presentación del PDF por aulas, no dato erróneo, pero el importador
  debe conciliarlo porque la información completa de profesores por
  celda solo está en el horario por grupos;
  (4) inconsistencias profesor↔plaza entre los dos PDFs: en Sesión 8
  se registró que EF mié 9:00 aparecía como EFI2 en el horario del
  grupo 1ºA y como EFI3 en el horario del aula Gim; la verificación
  de Sesión 13 sobre los PDFs actuales del proyecto NO reproduce esa
  inconsistencia concreta (las 12 sesiones de EF de 1ºESO salen EFI2
  en ambos listados). La política de cruce sigue siendo necesaria
  porque este tipo de error es posible y no detectable sin información
  del centro. El importador debe cruzar los dos listados y reportar
  inconsistencias al usuario para conciliación manual. Tampoco puede
  confiar en la tipificación declarada (TALL3 figura como "Taller
  FPB" pero su uso real es aula teórica de CS). Carga manual de
  revisión obligatoria
- **D9**: Pares de sesiones cruzando el recreo → Fase 5 (instituto completo).
  Si el solver no reproduce naturalmente el patrón (CS de 1ºFPB en
  10–11 + 11:30–12:30), se evaluará restricción blanda "simetría
  alrededor del recreo" o bloque elástico tolerante a tramo no lectivo
- **D10**: UX de configuración de plazas multi-profesor → Fase 8 (UI).
  La UI debe ofrecer un flujo claro y diferenciable para tres casos:
  sesión ordinaria (una plaza, un profesor, un aula), co-docencia (una
  plaza, varios profesores, un aula, mismos subgrupos), y
  desdoble/agrupamiento (varias plazas, profesores y aulas distintos,
  subgrupos disjuntos). No afecta al modelo de datos
- **D11. Restricciones y preferencias horarias del profesorado: fase de
  implementación no asignada.**
  El modelo de Fase 1 prevé `ProfesorRestriccionHoraria` (tipo DURA/BLANDA
  \+ peso) y decidió no modelar preferencias positivas en Fase 1. El roadmap
  de fases del solver (2-5) no asigna explícitamente cuándo el solver
  empieza a consumir estas restricciones. Asignación tentativa: Fase 5
  (instituto completo), porque es la primera fase con claustro real y por
  tanto con indisponibilidades reales que validar. Antes de Fase 5 los
  datasets son subconjuntos de prueba sin indisponibilidades. Confirmar
  esta asignación al planificar Fase 5 en detalle.
- **Frontera Fase 2→3 en Subgrupo (corregido en Sesión 14):** el supuesto
   original era que Fase 3 exigiría Subgrupo multi-grupo. Es incorrecto. El
   dataset de Fase 3 (desdoble CyR, agrupamiento RefMt, OyD de 1ºESO) se
   modela íntegramente como plazas que listan VARIOS subgrupos, cada subgrupo
   perteneciente a UN solo grupo (ver §6.1 del modelo: los 24 subgrupos del
   bloque CyR/OyD/RefMt son mono-grupo). El schema actual ya soporta
   plaza-con-N-subgrupos (`plaza.subgrupos` con `minItems:1` sin techo) y el
   modelo CP-SAT ya lo trata correctamente (las restricciones recorren todas
   las plazas de la actividad). Lo que Fase 3 SÍ añade no es subgrupo
   multi-grupo, sino: (1) el no-solape por grupo S9 —necesario porque al
   partir el grupo en subgrupos, S3 deja de cubrir I1—, y (2) la rama de
   `aulasCandidatas`. El Subgrupo cuya población son alumnos de varios grupos
   (`SubgrupoGrupo` N:M; caso TICO de Bachillerato) se aplaza a Fase 5, que es
   su sitio natural.
- **D12**: AllDifferent de distribución por día es infactible si una
  actividad tiene repeticionesPorSemana > nº de días (palomar). En Fase 2
  ninguna asignatura de 1ºESO llega a 6, y la restricción lleva guarda.
  Fase 5 (FPB, frecuencias altas): revisar si procede relajar a blanda.
- **D22 (Sesión 33, Fase 5 Sub-bloque A)**: la frontera del recreo está
  hardcodeada como constante de estructura de jornada (ORDEN_TRAS_RECREO=4 en
  ModeloCpSat y su espejo en VerificadorSolucion), asumiendo recreo tras el tercer
  tramo. El POJO Tramo no transporta la frontera (solo diaSemana/ordenEnDia, y el
  recreo no es un Tramo), así que no es derivable de los datos; el resto del módulo
  ya asume esta estructura (MAX_CONSECUTIVAS, términos de ventanas). Si un centro
  tuviera otra colocación del recreo habría que parametrizarla. Asignación: Fase 8
  (UI de configuración de jornada). Duplicación consciente de la constante entre
  modelo y verificador (este último es independiente del solver por diseño y no
  puede importarla), frágil del mismo modo que N de consecutivas (D21); se resuelve
  cuando la estructura de jornada pase a configuración, momento en que ambos leerán
  el mismo origen. Hoy ningún dato real la contradice.
- **D-F8.2b-4B (condicional, inerte): pin de aula × poda de aulasCandidatas.**
  Si D23 se reabre y la poda (candidatasPodadas) se reactiva rediseñada, el pin de
  aula debe forzar el aula pinada DENTRO de las candidatas conservadas: hoy la poda
  podría eliminar un aula candidata legítima y dejar el pin inexpresable (sin BoolVar
  de presencia). INERTE mientras podarAulas=false en todos los caminos (verificado en
  8.2b-i: ModeloCpSat 142/202/216-217; grep sin llamadores de construirConObjetivo(true)).
  No se implementa: sería defender un caso imposible con el código actual. Vigilancia
  documental; se activa solo con la poda.
- **D25 (Sesión 42, Fase 5 Bloque 17 — reactivación agravada de D24)**: el perfil -Pescala
  corrido ENTERO no pasa por contención de CPU. D24 se dio por CERRADA en S39 asumiendo que
  @Tag("escala") bastaba; pero @Tag solo SEPARA los pesados de la suite rápida — NO resuelve
  la contención ENTRE ellos cuando corren juntos en -Pescala. Con 3 tests de optimización a
  escala (fusión factibilidad + optimización + warm-start) secuenciales, en una máquina de
  4 núcleos físicos / 8 hilos (Intel i7-4790K), CP-SAT lanza ~8 workers por test y satura los
  núcleos reales. Evidencia (run S42, -Pescala completo, 34:39 min): fusión 139 s FACTIBLE
  (vs. 86 s aislada — la misma factibilidad pura tarda +60% según carga); optimización fría
  601 s UNKNOWN (vs. FEASIBLE 215 aislada); warm-start COMPLETA FEASIBLE (frío 222, caliente
  207). Observación NO explicada (conjetura, no hecho): el test del MEDIO falla y el ÚLTIMO
  pasa, lo que no encaja con contención acumulativa simple; hipótesis = el warm-start arranca
  con su propia semilla (factib pura interna) y es más robusto a la contención que la
  optimización fría. Los objetivos NO son reproducibles entre runs (215/204 en S40, 222/207
  en S42): la varianza de ejecución multihilo es de ±7 puntos, lo que confirma la lectura "por
  régimen, no por delta" de S42. Impacto: los tests de optimización a escala SOLO son fiables
  AISLADOS (mvn test -Pescala -Dtest=NombreTest); el perfil completo es un build rojo
  inestable. Severidad: media (no es bug del solver ni bloquea la suite rápida, que sigue 59
  verde; bloquea correr -Pescala entero como gate). Soluciones candidatas (NO evaluadas, frente
  futuro propio): forkear surefire por clase con reuseForks=false; limitar num_search_workers
  de CP-SAT en el perfil escala; serializar los pesados; o asumir que -Pescala se corre
  test-a-test a mano. Cada intento son 30+ min de corrida -> es su propio bloque, no se aborda
  al cierre de una sesión. Asignación: sin asignar; abordar antes de depender de -Pescala como
  gate de CI (Fase 12).
- **D16**: el CLI (`Materializador`, `FormatoCelda`) no pinta el aula ELEGIDA de
  las plazas con aula variable: `FormatoCelda` muestra `?` cuando no hay
  `aulaFija`. La Sesión 16 dejó el CLI fuera a propósito (el commit ya era ancho
  y estructural; pintar el aula es otra preocupación). `SolucionHorario` ya
  expone `aulaElegida(inst, plaza)`, así que el cambio es acotado: el
  materializador debe leer de ahí en vez de `plaza.aulaFija()`. Pendiente de
  commit posterior dentro de Fase 3 o al cierre de fase.
- **D17 (nueva en S24): el conteo de ventanas por cotas tensadas asume objetivo
  minimizado con peso positivo.** En `ModeloCpSat.objetivoVentanasProfesor`,
  `primero`/`ultimo` (primer y último tramo ocupado por profesor y día) se acotan
  (primero ≤ posición de cada clase; ultimo ≥ posición de cada clase) en vez de
  fijarse con `addMinEquality`/`addMaxEquality`. Es correcto SOLO porque el
  objetivo MINIMIZA las ventanas con peso > 0: el solver tensa `primero` hacia
  arriba y `ultimo` hacia abajo hasta los valores exactos (primera y última
  clase), dando el span real. Si un futuro término blando compitiera y dejara
  estas cotas flojas (p.ej. un término que premiara spans grandes, improbable
  pero no imposible), `huecos` podría tomar un valor falso. Mitigación si llega
  el caso: pasar a `addMinEquality`/`addMaxEquality` explícito, que no depende
  del signo del objetivo. Se verá venir al introducir el segundo término (6c+).
  No afecta al verificador independiente (`contarVentanasProfesor` cuenta sobre
  la solución concreta, sin cotas). Revisar al añadir términos que compitan.
- **D18 (nueva en S25; ampliada en S26): INFEASIBLE no diagnostica la causa →
  condiciones necesarias baratas de factibilidad.** Registrada en S25 en el
  modelo §4.3 pero no en esta lista hasta S26. Un problema infactible (profesor
  con todos sus tramos vetados, aulas insuficientes en un tramo, horas
  curriculares > 30 por grupo, repeticiones > días) da un INFEASIBLE opaco. NO
  es un validador de factibilidad (demostrarla es imposible; solo el solver la
  decide); son CONDICIONES NECESARIAS (no suficientes): chequeos de
  conteo/palomar que detectan ALGUNAS infactibilidades seguras con mensaje
  accionable. Lógica en capa de configuración (Fase 6/8), hermana de la
  validación de DemandaCurricular y de D3 (capacidad de aulas). No toca el
  modelo CP-SAT. La presentación al usuario se separa en D20. Ver modelo §4.3.
- **D19 (nueva en S26): atribución de reglas duras Y blandas por celda.** Fase 8
  contempla hoy solo conflictos en tiempo real durante el drag y solo reglas
  duras. Faltan (a) exponer las BLANDAS por celda (ventana, última hora,
  no-distribución, indisponibilidad blanda) y (b) atribución sobre el horario YA
  generado, no solo durante el arrastre. La maquinaria existe en el solver
  (VerificadorSolucion, contarVentanasProfesor); ningún requisito de UI la expone
  celda a celda. Fase 7 (visualización) / Fase 8 (ajuste manual). Implica ampliar
  el criterio de Fase 8 (ver más abajo). No afecta al modelo de datos. Ver
  modelo §8.
- **D20 (nueva en S26): UI de avisos de pre-validación (condiciones
  necesarias).** Separada de D18. D18 = lógica (chequeos en backend); D20 =
  presentación (cómo se muestran los avisos al usuario, si bloquean o advierten,
  enlace a la entidad a corregir). Fase 6 (si se valida en importación) / Fase 8
  (UI). No afecta al modelo de datos. Ver modelo §8.
- **D21 (nueva en S26; ampliada en S27): parametrización y calibración de pesos
  blandos + el parámetro N de consecutivas.** Hoy `PESO_VENTANAS`,
  `PESO_INDISP_BLANDA` y `PESO_CONSECUTIVAS` (S27) son constantes hardcodeadas a 1
  en `ModeloCpSat`; `MAX_CONSECUTIVAS=3` (S27) y, desde S41, `UMBRAL_PODA_AULA=8` y
  `MAX_AULAS_PODA=8` (poda de aula, D23 palanca b) son otras constantes hardcodeadas
  (conjetura razonable por la estructura 3+3 de la jornada, NO un requisito
  verificado con el centro). Con tres (o más) términos blandos compitiendo, el peso
  relativo determina qué optimiza el solver, pero NO hay datos del centro para
  calibrarlo (gemela de la decisión de no fijar el umbral del criterio 4). Tres
  trabajos diferidos: (a) mover pesos Y `MAX_CONSECUTIVAS` a configuración
  —ampliando `ProblemaHorario`, que hoy NO porta parámetros clave-valor; trabajo
  estructural de I/O—; (b) decidir los valores relativos con datos reales y un
  fixture que ejercite VARIOS términos a la vez (los fixtures de 6c y 6d-c aíslan un
  término cada uno, así que su peso relativo no está validado por ningún test);
  (c) cuando (a) se haga, `contarPenalizacionConsecutivasProfesor` (verificador)
  dejará de duplicar `n=3` —hoy espejo frágil de `MAX_CONSECUTIVAS`, que es privado
  del modelo— y ambos leerán el mismo origen. El campo `peso` por-restricción de
  `ProfesorRestriccionHoraria` (modelo §4.3) es parte de (a): hoy se ignora en el
  término blando, que usa la constante. Relacionada con el criterio 3 de Fase 5
  (calidad). Fase 5 (términos blandos restantes) o Fase 8 (UI de configuración).
  ACTUALIZACIÓN (S42): `UMBRAL_PODA_AULA`/`MAX_AULAS_PODA` ya NO son constantes
  "calibrables vivas": la poda que las usa quedó DESACTIVADA por defecto (D23, palanca b
  inviable con K=8). Siguen en código como mecanismo latente, pero calibrarlas no procede
  hasta un eventual rediseño de la poda; (a)/(b)/(c) de esta deuda aplican solo a los tres
  PESO_* y MAX_CONSECUTIVAS.
- **D26 (Sesión 47, Fase 6 Bloque 3): `Aula` (JPA) no tiene campo `nombre`. CERRADA
  (S102) COMO NO APLICABLE.**
  Ni la entidad de catálogo (S46) ni §4.1 de `modelo_datos_fase1.md` definen un
  nombre completo de aula (a diferencia de `Profesor`/`Asignatura`, que sí tienen
  `nombre_completo`). El schema JSON del solver exige `Aula.nombre` no nulo, y
  `CatalogoMapper.aAula` lo resuelve con `nombre = codigo`. Válido para arrancar
  Fase 6 (el solver no distingue código de nombre en el objetivo ni en el
  verificador), pero es una simplificación: si la UI de Fase 8 necesita mostrar
  un nombre descriptivo de aula distinto del código ("Laboratorio de Ciencias"
  vs "A6"), hace falta añadir el campo a la entidad JPA y decidir si el mapper
  deja de derivarlo. Fase 8 (UI) o antes si algún criterio de Fase 6/7 lo exige.
  CIERRE (S102, form de Aula construido): la condición de pago —«la UI necesita un
  nombre descriptivo distinto del código»— NO se cumple. El modelo identifica el
  aula por `codigo` ("A5", "TALL1", "Gim"); lo descriptivo se expresa con los campos
  de ubicación y aforo que §4.1 ya define (`edificio`, `planta`, `sector`,
  `capacidad`), TODOS expuestos en el formulario. No hay campo `nombre` que poblar ni
  rótulo libre que el usuario reclame. Se cierra por decisión de diseño; si un centro
  real exigiera un rótulo distinto del código, se reabre como mejora futura de
  O-catálogo. No se añade campo a la entidad JPA.
- **D27 (Sesión 47, Fase 6 Bloque 3): código de `Tramo` sintetizado por el
  mapper.** `modelo_datos_fase1.md` no define una convención de código legible
  para `TramoSemanal`; `CatalogoMapper.aTramos` sintetiza `"L1".."V6"` (letra de
  día + ordenEnDia) porque `domain.Tramo.codigo` es un campo obligatorio sin
  fuente en el JPA. Riesgo bajo (es un identificador interno del solver, no se
  muestra al usuario), pero si la UI de Fase 7/8 llega a mostrar este código
  directamente hay que decidir si se mantiene la convención sintética o se
  sustituye por algo más descriptivo (p. ej. "Lunes 8:00-9:00"). Fase 7/8.
- **D29 (Sesión 52, Fase 6 Bloque 8): tiempo/semilla y vía del solver hardcodeados
  al default en el servicio.** GeneradorHorarioService.generar() usa new SolverHorario()
  (120 s, semilla 42) y la vía de optimización (resolverOptimizandoConDetalle). A escala
  de instituto completo la optimización no converge en ese presupuesto (S38/S42: sin
  converger en 600 s), luego generar() sobre un centro real devolverá estado FEASIBLE, no
  OPTIMAL. No es un bug (ResultadoOptimizacion porta estado+objetivo+cotaInferior para
  distinguirlo del óptimo probado), pero el límite de tiempo, la semilla y la elección de
  vía (factibilidad pura resolver / optimización / warm-start resolverOptimizandoConSemilla)
  deben ser configurables desde la UI y no constantes en el servicio. Hermana de D21/D23.
  Fase 8. CIERRE PARCIAL en S57 (Bloque 8.1): maxSegundos/semilla/via expuestos por
  POST /api/horarios (body opcional; defaults 30 s / 42 / OPTIMIZACION). Vía = OPTIMIZACION
  únicamente; FACTIBILIDAD por REST diferida (resolver() no da estado/objetivo y
  ResultadoOptimizacion.objetivo/cota son double primitivos → persistirla exigiría centinela
  falso o tocar el solver); warm-start por REST no expuesto. El default 30 s es conjetura de
  UX no validada contra el centro real, no constante definitiva.
- **D30 (Sesión 53, Fase 6 Bloque 9): la renumeración de tramos está duplicada
  entre el mapper de entrada y el de salida.** `CatalogoMapper.aTramos` (entrada,
  JPA->dominio) y `SolucionMapper.indiceTramos` (salida, dominio->JPA) replican la
  MISMA regla: filtrar `esLectivo`, ordenar por `orden` global, renumerar `ordenEnDia`
  1..N reiniciando por día, y emparejar `diaSemana = Dia.ordinal()+1`. Es un espejo
  frágil (hermano de D21/D22): si una lógica cambia (p. ej. otra estructura de jornada,
  recreo en otra posición) y la otra no, el emparejamiento del tramo de salida diverge
  EN SILENCIO —una Sesion apuntaría a un TramoSemanal equivocado sin error—. La causa
  raíz es D27 (el código de Tramo es sintético y TramoSemanal no lo porta) más la
  ausencia de `ordenEnDia` en la entidad (solo hay `orden` global con recreo). Se
  resuelve cuando la estructura de jornada pase a configuración (Fase 8, donde ya vive
  D22): ambos mappers leerían el mismo origen de numeración, o TramoSemanal ganaría un
  código/ordenEnDia estable que haga innecesaria la síntesis. Riesgo hoy: bajo (ninguna
  jornada real contradice la estructura 6+recreo asumida), pero real. Asignación: Fase 8.
- **D31 — Poblaciones y particiones a confirmar con el centro** (nueva, S63): los
  fixtures de escala asumen particiones y poblaciones PLAUSIBLES pero no validadas
  por el centro: (a) particiones de refuerzo/ATED de 3ºESO (B3/S21); (b) optativas
  DIG/TEC/FOPP de 4ºESO, 6h en dos bloques de perfil distinto, modeladas como
  población propia por bloque (B7/S28); (c) población "1 subgrupo por opción" en las
  optativas de 1ºBach (B11/S32); (d) modalidades y optatividad de 2ºBach (B13/S36).
  Relacionada: la invariante de población (el dominio modela SUBGRUPOS, no ALUMNOS;
  que un subgrupo sea partición real disjunta y exhaustiva NO lo verifica ningún
  componente). → Fase 8 (CRUD de catálogo) o antes, si hay contacto con el centro.
  CIERRE PARCIAL (S69): la conversación con el jefe de estudios (lámina de S68) VALIDÓ dos cosas y
  deja el resto VIVO. Validado: (i) el tramo denso de 1ºESO se piensa como CAJAS (Actividad→Plaza→
  Subgrupo correcto en su punto más difícil); (ii) el PDC de 3º es «parte de 3ºA que sale aparte, mismo
  tutor» con «dos horarios en pantalla» → confirma el modelado como grupo_padre (§6.2/S9), no lo reabre.
  SIGUE VIVO (no puesto ante el centro): las poblaciones/particiones (b) optativas de 4ºESO, (c)
  subgrupos de 1ºBach, (d) modalidades de 2ºBach; y la invariante de población en general. La parte (a)
  —refuerzo/ATED de 3ºESO— queda cubierta por la validación del tramo denso de 1ºESO como caso gemelo,
  pero NO se puso el de 3º explícitamente ante el centro: se considera validada por analogía, no por
  confirmación directa. → resolver (b)/(c)/(d) al abrir el CRUD de esos niveles o antes si hay contacto.
- DECISIÓN DE PRODUCTO (S67): el usuario decide NO hacer demo al cliente hasta terminar
  8.5 (CRUD de catálogo). Razón: mientras el catálogo se defina en SeedCatalogoRunner
  (Java), no hay producto que enseñar, y una demo provocaría "¿cómo meto yo mis datos?"
  sin respuesta. RIESGO ACEPTADO A SABIENDAS: D31 es deuda de REQUISITOS, no de código, y
  solo se cierra hablando con el centro; construir el CRUD encima de un modelo no validado
  con ellos significa que un desencaje se descubriría DESPUÉS de haber construido la UI.
  MITIGACIÓN ACORDADA (barata, no cuesta sesión): antes de teclear el CRUD de 8.5,
  enseñar al jefe de estudios el MODELO DIBUJADO —no la app— y preguntarle si así piensa
  él sus grupos y agrupamientos. Media hora. Es la misma disciplina que el proyecto aplica
  al código (verificar el hecho antes de asumirlo); aquí el hecho vive en la cabeza de una
  persona, no en el repo.
  CONSUMADA (S69): la mitigación se ejecutó —lámina de S68 puesta ante el jefe de estudios— y validó el
  modelo (ver cierre parcial de D31 arriba). El «no demo hasta 8.5» sigue VIVO; lo consumado es solo la
  precondición de mitigación, que ya no bloquea teclear el CRUD.
- **D-F8.2b-iii-A-a** (S62, VIVA, no bloqueante) — `GeneradorHorarioService` tiene 12
  repositorios inyectados en el constructor. Es olor a clase que hace demasiado. Decisión
  consciente de S62: NO refactorizar en un bloque cuyo valor era cerrar un lazo funcional
  (mezclaría capas). Extraer un `CatalogoLoader` (o similar) que agrupe la carga del
  catálogo cuando el constructor moleste de verdad — típicamente al añadir el siguiente
  repo. Sin impacto funcional.
- **D-F8.2b-iv-a** (S66, VIVA) — ESPEJO DE VALIDACIÓN: las reglas de coherencia del bloqueo
  están implementadas DOS veces. `BloqueoService` las valida sobre entidades JPA en el alta;
  `BloqueoMapper` las valida sobre el dominio ya mapeado al generar. Se aceptó
  conscientemente: llamar al mapper desde el alta exigiría cargar y mapear el catálogo
  completo (10 findAll()) para dar de alta un pin, y unificarlos obligaría a reescribir
  BloqueoMapper —refactor de un componente probado dentro de un bloque cuyo valor era abrir
  una superficie (misma decisión que S62 con los 12 repos)—. Si una diverge, el alta acepta
  algo que el solve rechaza con 500. MITIGACIÓN VIVA: el test de contrato
  `contrato_pinQueElAltaRechazaLoRechazaTambienElMapper` (BloqueoEndpointTest) inyecta en BD
  —saltándose BloqueoService— un pin que el alta rechazaría por la regla (e) y asevera que
  cargarProblema() TAMBIÉN lo rechaza. Es el único componente que detecta la divergencia.
  Hermana de D21/D22/D30. → resolver si el número de reglas crece.
- **D-F8.6-a** (S67, VIVA, de MÉTODO, no técnica) — AVISO DE OPORTUNIDAD DE MOCKUP. Cuando un
  bloque de Fase 8 lleve una decisión cuya respuesta dependa de CÓMO SE VE O SE GESTICULA algo
  (no de cómo se computa), el arquitecto debe avisarlo explícitamente AL PROPONER ALCANCE, y
  usar un mockup para resolverla antes de fijar el contrato. Origen: en S67 el mockup de la
  celda de desdoble reveló que las TRES granularidades (coste blando por instancia, conflicto
  de aula por plaza, solape por instancia — asimetría D15) se pintan sin colisión, y que la
  sub-entrada NO puede ser arrastrable; ambas cosas ENTRARON en el contrato. Bloques candidatos
  vivos: 8.6 (drag&drop), 8.5 (UX de subgrupos compartidos entre particiones, D7), 8.4 (D20:
  ¿los avisos de pre-validación bloquean o advierten?). Se cierra cuando esos tres cierren.
  El mockup NO es entregable: el frontend real vive en el repo (7B). Es una pregunta dibujada.
- **D-F8.5-C2a-a** (S73, VIVA, no bloqueante, TEÓRICA hoy) — `.db` preexistente con PK NULL. Cualquier
  base creada por hbm2ddl ANTES de S73 tiene la columna `id` en NULL en 8 tablas (ver Notas técnicas
  Fase 6). El cambio a `schema.sql` arregla las bases NUEVAS, pero NO migra una existente (SQLite no
  soporta `ALTER TABLE` para esto; requeriría recreación). Sin producción hoy → teórico. Riesgo real:
  un `.db` de pruebas viejo fallará al arrancar con FK-ON. Si algún día hay bases en producción antes de
  otro cambio de esquema, hará falta una estrategia de migración (recreación / copia-y-swap). → vigilar.
- **D-F8.5-C3-a** (S75, VIVA, no bloqueante) — `TipoAula.COMUN` SIN SEMÁNTICA DEFINIDA. Existe
  como valor del enum y en los dos CHECK de `schema.sql` (`aula.tipo`, `asignatura_aula_compatible.tipo_aula`),
  con CERO usos en src/main, tests y fixtures (grep concluyente, S75). Nadie sabe qué lo distingue
  de `ORDINARIA`. Conjetura del usuario ("aula compartida con otras actividades") NO confirmada
  con el centro. NO USAR hasta definirlo. Retirarlo del enum tocaría los dos CHECK: coste
  desproporcionado hoy. → definir con el centro (hermana de D31) o retirar en un bloque de esquema.
  CONTENIDA EN UI (S102, form de Aula): el selector de tipo ofrece los OCHO valores con semántica
  (`TIPOS_AULA` en `aula.model.ts`) y OMITE `COMUN` — lectura recta de «NO USAR hasta definirlo»:
  ningún aula con `tipo=COMUN` puede CREARSE desde el formulario. Matiz de edición (casos (7)/(8) de
  `aula-form.spec`): si un aula PREEXISTENTE ya tuviera `COMUN`, `AulaForm.tipos` lo añade a la lista
  para no borrarlo en silencio al editar otro campo (el `required` sobre un `<select>` sin opción
  coincidente forzaría a reasignar tipo). Así se respeta un dato existente sin ofrecer `COMUN` como
  alta nueva. La deuda SIGUE VIVA a nivel de esquema (enum + dos CHECK intactos); solo queda fuera del
  alcance de la UI. Cierre pleno = definir su semántica con el centro o retirarla del esquema.
- **D-F8.5-C3-b** (S75, VIVA, de DOMINIO, no bloqueante) — Los códigos de asignatura del centro
  son POR CURRÍCULO, no por materia: `EF` (EF de ESO) y `EdFís` (EF de Bachillerato) son
  asignaturas DISTINTAS, y lo mismo `Bio`/`Biol`/`BioNu`, `Tec`/`TecIn`, `Mat`/`MatAc`/`MatAp`/`Mate2`.
  CONFIRMADO por el arquitecto en S75 como correcto por dominio (no es sinonimia a normalizar).
  Consecuencia para I3: las compatibilidades se declaran por asignatura-currículo, y poblarlas es
  trabajo de USUARIO en la UI, no de código — el catálogo real tiene hoy 0 compatibilidades (M1=0,
  medición de S75). MENOR y distinto: `EFis` vs `EFís` (solo un acento, mismos grupos de 4ºESO)
  sí parece variante de transcripción del PDF → cae en D8 (importador), no aquí.
- **D-F8.5-D1-b** (S76, VIVA, no bloqueante) — «UN PDC POR PADRE» ES DECISIÓN DEL ARQUITECTO.
  La guarda de 400 en `PdcService.crear` impone 1:1. Los 5 PDC medidos son 1:1, pero 5 casos de
  un centro no prueban que otro no quiera dos PDC de un mismo grupo. Hermana de D31. Relajar la
  guarda no rompería datos existentes. → confirmar con el centro cuando haya contacto.
- **D-F8.5-D2a-a** (S77, VIVA) — I4 SIN RED BAJO LA APLICACIÓN. La unicidad de
  TUTOR_PRINCIPAL por grupo vive SOLO en `TutoriaService`. La PK compuesta
  (profesor_id, grupo_id) no puede expresarla, y no hay índice único parcial. Verificado por
  mutación en S77: neutralizada la guarda, la escritura pasa con 200 y la base queda con dos
  principales. Cualquier inserción por otra vía (test, seed, script) viola I4 en silencio.
  Familia de D-F8.2b-iv-a (validación sin espejo en la BD). → índice único parcial
  `ON profesor_tutoria(grupo_id) WHERE rol='TUTOR_PRINCIPAL'` si aparece otra vía de escritura.
- **D-F8.5-D2a-b** (S77, VIVA, no bloqueante) — INCOHERENCIA 404/400 EN FK POR CLAVE NATURAL.
  `TutoriaService` devuelve 404 si el profesor del body no existe; `GrupoService.resolverNivel`
  devuelve 400 en el caso análogo. Dos respuestas al mismo hecho. Especificado así por el
  arquitecto en S77 sin comprobar el precedente; no se corrigió en caliente para no tocar
  GrupoService fuera de alcance. → unificar al abrir el próximo bloque que toque GrupoService.
- **D-F8.5-E-a** (S78, VIVA, no bloqueante) — `peso` ES SUPERFICIE MUERTA EN TRES CAPAS.
  Columna `not null` en `schema.sql`, campo en la entidad JPA y componente del record
  `RestriccionHoraria`, escrito SIEMPRE a 1 por `RestriccionHorariaService` y **jamás leído por
  ningún consumidor**: `ModeloCpSat.objetivoIndisponibilidadBlandaProfesor` pondera con la
  constante `PESO_INDISP_BLANDA`, no con `r.peso()`, y la variante DURA no lo mira. Medido en S78
  por lectura literal. El javadoc del record afirmaba que era «la penalización en la función
  objetivo»: FALSO, y además decía «se ignora en DURA» cuando se ignora también en BLANDA.
  Por eso 8.5-E NO lo expone (exponer un control inerte haría que la UI mintiera). Familia de
  D21(a) —parametrización de pesos blandos— y hermana de `requiereTutor` (S77: superficie viva,
  semántica muerta). → activarlo es trabajo de D21, con su calibración pendiente de datos del
  centro; activarlo sin calibrar sería peor que dejarlo inerte.
- **D-F8.5-E-b** (S78, VIVA) — UNICIDAD (profesor, tramo) SIN RED BAJO LA APLICACIÓN. Que un
  profesor no tenga dos restricciones sobre el mismo tramo vive SOLO en la validación de
  `RestriccionHorariaService` sobre la lista entrante. NO hay `UNIQUE (profesor_id, tramo_id)` en
  `schema.sql`. Verificado POR MUTACIÓN en S78: neutralizada la guarda, el PUT pasa con 200 y la
  base queda con DOS filas, con el flush sin error. Cualquier inserción por otra vía (test, seed,
  script) viola la regla en silencio. Familia exacta de D-F8.5-D2a-a (I4) y de D-F8.2b-iv-a
  (validación sin espejo en la BD). → índice único si aparece otra vía de escritura.
- **D-F8.5-E-c** (S78, VIVA, de FRAMEWORK, no bloqueante) — EL DIALECTO NO CLASIFICA LOS FALLOS
  DE FK COMO `DataIntegrityViolationException`. Medido en S78 al afinar el aserto de borrado: una
  violación de FK llega como `GenericJDBCException`, por dos causas distintas y acumulativas —
  (1) `TestEntityManager.flush()` no cruza la frontera del repositorio, que es donde Spring
  traduce a su jerarquía DAO; (2) el SQLiteDialect community 7.4.1 no clasifica el error de FK
  como violación de constraint—. CONSECUENCIA QUE EXCEDE AL TEST: un `@ControllerAdvice` que
  pretenda mapear violaciones de FK a HTTP POR TIPO DE EXCEPCIÓN **no puede funcionar con este
  dialecto**. Esto REFUERZA con un argumento técnico la decisión de S74 —hasta ahora sostenida
  solo por estilo— de que cada controller traduzca sus propias excepciones. El discriminante
  fiable hoy es el MENSAJE, no el tipo. → releer antes de proponer centralizar traducción de
  excepciones.
- **D-F8.5-E-d** (S78, VIVA, no bloqueante) — LOS CONTEOS INVERSOS DE `Profesor` VIVEN EN DOS
  UBICACIONES. `contarPlazas` y `contarRestriccionesHorarias` son `@Query` nativas en
  `ProfesorRepository` (patrón S74); `contarTutorias` vive en `ProfesorTutoriaRepository` y llega
  por colaborador inyectado (S77). Mismo tipo de chequeo, misma raíz, dos sitios donde buscarlo.
  No es defecto funcional —los tres se ejecutan en `ProfesorService.borrar`— pero rompe la
  localidad que S74 perseguía. → unificar si se añade un tercer conteo a esta raíz.
- **D-F8.4-A-a** (S79, VIVA) — LA GARANTÍA DE (c) ES DERIVADA, NO ESTRUCTURAL. Que «grupo con más
  tramos de actividad que tramos lectivos ⇒ infactible» sea CIERTO depende de que
  `ModeloCpSat.restriccionNoSolapeGrupo` (1046-1074) siga deduplicando un intervalo por INSTANCIA
  de actividad vía `tocaGrupo` — la misma unidad de conteo que usa la regla. (a) y (d) son
  estructurales; (c) no. Si el no-solape por grupo se relajara para modelar algo que hoy no
  existe, (c) pasaría a producir FALSOS POSITIVOS EN SILENCIO, bloqueando problemas resolubles.
  Citada en el javadoc de `sobrecargaGrupo`. → revisar si se toca el no-solape por grupo.
- **D-F8.4-A-b** (S79, VIVA) — EL PODER DISCRIMINANTE DE LA DEDUPLICACIÓN VIVE EN UN SOLO TEST.
  Medido POR MUTACIÓN en S79: al neutralizar la deduplicación (Set→List) cae ÚNICAMENTE A3
  (`grupoConDesdoble_cuentaLaActividadUnaVezAunqueTengaDosPlazas`, aserto de VALOR demanda==7 vs 10).
  Los tres tests HTTP siguieron VERDES: su fixture (6 de demanda contra 5 disponibles) dispara con
  las dos formas de contar, luego prueban la superficie, no la aritmética. Si alguien «simplifica»
  el fixture de A3 —calibrado a propósito para que AMBAS cuentas superen el techo, de modo que la
  presencia no discrimine y el único aserto posible sea el valor—, la regla se queda sin red y
  ningún otro test lo detectará. → no tocar el fixture de A3 sin rehacer la mutación.
- **D-F8.4-A-c** (S79, VIVA, no bloqueante) — LA SEVERIDAD YA NO DISCRIMINA. Al subir (c) a ERROR,
  ninguna de las reglas produce AVISO, y los asertos de severidad de A3 y A4 quedaron
  TAUTOLÓGICOS: se conservan como red anti-regresión (por si (c) volviera a AVISO) pero NO cuentan
  como cobertura. El enum `Severidad` conserva `AVISO` sin ningún productor. Declarado en la
  réplica de S79, no descubierto después. → si nace una regla heurística, deja de ser deuda.
  REENCUADRADA EN S92, no cerrada: la mitad «el enum conserva `AVISO` sin productor» SALE de la deuda
  —el javadoc de `Severidad` documenta el criterio de cuándo una regla nace no-bloqueante, y el string
  viaja en el contrato REST, luego el valor no está huérfano por descuido—. SOBREVIVE la otra mitad:
  los asertos de severidad de A3 (`PrevalidacionServiceTest:179`) y A4
  (`PrevalidacionEndpointTest:134-135`) siguen TAUTOLÓGICOS hasta que exista un productor de `AVISO`.
  S92 midió además que borrar `AVISO` NO los tocaría: ambos aseveran `ERROR`, valor que sobrevive.
- **Contrato de 8.2b-iv** (S62, decisión tomada; IMPLEMENTADO en S66 — se conserva porque
  documenta el PORQUÉ del endpoint propio, que sigue vivo) —
  la entrada del bloqueo por REST va en **endpoint propio**, NO en el body de
  POST /api/horarios:
      POST   /api/bloqueos       → alta de un pin (tramo + opcionalmente aulas por plaza)
      GET    /api/bloqueos       → lista los pines vigentes (la UI los pinta con candado)
      DELETE /api/bloqueos/{id}  → baja
  Razones: (1) ciclo de vida distinto — el bloqueo es estado PERSISTENTE del centro (S61:
  global, sin FK a HorarioGenerado), los parámetros de solve son EFÍMEROS de una
  invocación; mezclarlos invita a la confusión "¿este pin queda guardado?". (2) La UI de
  ajuste manual pina SIN generar: con el body, pinar exigiría lanzar un solve de 30 s, lo
  que rompe el flujo. (3) Con el body no hay forma de LEER los pines vigentes, y la UI los
  necesita. (4) El body de POST /api/horarios ya arrastra D29; un segundo eje lo
  convertiría en cajón de sastre. Contra asumido: más superficie que mantener — pero es la
  superficie que la UI va a pedir igualmente. El body de POST /api/horarios NO cambia.

- **D-F8.6-ii-a** (S81, VIVA, no bloqueante, DE SUPERFICIE DE ERROR) — EL `reason` DE
  `ResponseStatusException` NO VIAJA AL CLIENTE. `server.error.include-message` no está en
  `application.properties` y su defecto es `never`, así que el mensaje con que
  `BloqueoController` traduce una violación de las reglas D-3 a 400 se pierde: el usuario ve un
  texto degradado, no la regla violada. NO es específico de bloqueos —afecta a todo controller que
  use `ResponseStatusException`, y el patrón «cada controller traduce las suyas, sin
  `@ControllerAdvice`» (S74) lo generaliza—. NO se arregló en S81 a propósito: `include-message=always`
  expone el mensaje de TODAS las excepciones y es decisión de superficie GLOBAL, impropia de un
  bloque de frontend; además convive con los 422 de carga estructurada de 8.4-A y los 409 de 8.5-C2b,
  que sí llevan cuerpo propio. → decidir como política global de errores, no de refilón.

- **D-F8.6-ii-b** (S81, VIVA, HUECO FUNCIONAL, no bloqueante) — NO HAY GESTO DE DESPINAR. El
  arrastre crea pines y el aviso los cuenta, pero la UI no ofrece forma de quitarlos:
  `BloqueoService.borrar()` existe y NADIE lo llama, y soltar en el slot de origen no emite
  (guarda de `alSoltar`, que compara el destino con `inst.entradas[0]`, leído de la proyección,
  que no cambia al pinar). Hueco de ESPECIFICACIÓN del arquitecto, no de implementación: el
  contrato de S81 cerró «arrastrar pina» y no dijo cómo se desmarca. Arreglarlo NO es cablear
  `borrar()`: exige cambiar el estado del cliente de `Set<clave>` a `Map<clave, id>`, porque el
  DELETE necesita el `id` de la `SesionBloqueada` que el `Set` no guarda. Además el gesto es
  pregunta de UX (candado clicable / botón en el aviso) → MOCKUP (D-F8.6-a). HERMANA: el `Set`
  tampoco se recarga tras el arranque (`listar()` solo en el constructor; `cambiarVista` y
  `cargar(id)` no lo tocan), así que el candado miente si el horario se regenera o si se pina
  desde otra pestaña. → CERRADA en S83 (8.6-iii-B1): `Map<clave, id>`, candado `<button>` que emite la
  clave, `alDespinar` en el contenedor y `listar()` movido a `cargar(id)`. La mitad «hermana» queda
  cubierta solo en parte: ver D-F8.6-iiiB1-b.

- **D-F8.0-a** (S80, VIVA, de MÉTODO, no técnica) — EL «PROTOCOLO DE ARCHIVADO» SE INVOCA PERO NO
  ESTÁ ESCRITO. Las instrucciones de cierre de sesión mandan «seguir el PROTOCOLO DE ARCHIVADO del
  plan»; verificado por grep en S80 sobre `plan_trabajo_horarios.md` y `bitacora-sesiones.md`: NO
  existe ninguna sección con ese nombre. Lo que existe es (a) el «Criterio de higiene documental»
  (R4/R5), que cubre qué NO se puede perder pero no el procedimiento, y (b) un precedente
  observable de ~20 sesiones (formato `### Sesión NN` en bitácora, degradar la que deja de ser
  reciente, actualizar los dos censos + crónica + frase de ventana). El archivado de S80 se hizo
  por precedente, no por norma. RIESGO: los cinco pasos se recuerdan de memoria y la crónica y la
  frase de ventana son las que se olvidan (el propio prompt lo advierte cada sesión, lo que es
  síntoma). → escribirlo como sección del plan la próxima vez que se haga higiene, o aceptar
  explícitamente que es costumbre.
  → CERRADA en S86. Escrita la sección «Método de trabajo (procedimiento vigente)» en este plan,
  inmediatamente DESPUÉS del criterio de higiene documental (R4/R5) y dentro de «Decisiones
  permanentes». Contiene: M1 (los OCHO pasos del cierre de sesión, no cinco —la medición contó dos
  más de los que esta deuda suponía: evaluar limpieza y verificar diff/costura—), M1-bis (el
  archivado con PROMOVER/INSERTAR/COMPROBAR y los dos fallos históricos que lo justifican),
  M1-ter (el prompt de la sesión siguiente, con su criterio de poda), M2 (§A de medición), M3
  (campaña de mutación) y M4 (contraste previo + artefactos derivados). Los dos últimos pasos que
  esta deuda no nombraba —proponer el nombre de sesión y entregar el prompt siguiente— vivían SOLO
  en el prompt de apertura y son ahora M1.7 y M1.8. Dos puntos van marcados como DECISIÓN DEL
  ARQUITECTO por no sostenerlos la evidencia: la obligatoriedad de PROMOVER y el umbral de ~60
  líneas.

- **D-F8.6-iiiA-a** (S82, CERRADA en S85, DE COBERTURA, no bloqueante) — NINGÚN SERVICIO DEL FRONTEND TIENE
  TEST. Medido en S82: los cuatro specs del frontend cubren funciones PURAS (`pines`, `proyeccion`,
  `diagnostico`) y el shell (`app`); `horario.service.ts`, `bloqueo.service.ts` y
  `diagnostico.service.ts` no tienen ni uno. iii-A NO lo empeora —siguió el patrón vigente: el
  servicio devuelve el `Observable` crudo, sin `catchError`, y documenta en TSDoc los códigos del
  backend—, pero eso significa que el paso del 404 al consumidor NO está aseverado por nada. Estrenar
  `HttpTestingController` es una CAPA DE TEST NUEVA y se descartó a propósito dentro de un bloque
  cuyo valor era el contrato de lectura (misma decisión que S62 con los 12 repos y S75 con el
  refactor del mapper). Hermana de D-F8.6-ii-a: las dos son superficie de error sin red.
  → SIGUE VIVA ENTERA tras S84: 8.6-iv-B testeó COMPONENTES, no servicios, y no tocó ni un
  `HttpTestingController`. Lo que S84 cambia es solo el encuadre —esta deuda decía «estrenar
  `HttpTestingController` es una capa de test NUEVA», y lo sigue siendo, pero la medición de S84
  desmintió la parte que hablaba de estrenar TestBed: `app.spec.ts` ya lo usaba—. El bloque propio
  que pedía existe ya con casilla: 8.6-iv-A.
  → CERRADA en S85 (8.6-iv-A): los tres servicios tienen spec, con `provideHttpClientTesting` +
  `HttpTestingController` estrenados y 5 tests bajo campaña de 6 mutaciones. MATIZ MEDIDO que esta
  deuda no preveía y que REBAJA lo que el hueco valía: los cinco métodos son WRAPPERS PELADOS —sin
  `.pipe`, sin `catchError`, sin transformación—, así que lo entregado es CONGELACIÓN DEL CONTRATO
  DE ENDPOINTS (verbo + URL), no cobertura de lógica. La frase «el paso del 404 al consumidor NO está
  aseverado por nada» SIGUE SIENDO CIERTA tras el cierre: se dejó fuera POR ALCANCE y se registra
  aparte como D-F8.6-ivA-a, con la medición de que sí es reddable. Lo que esta deuda daba a entender
  —que había riesgo de lógica sin red— la medición no lo confirma: no hay lógica que romper.

- **D-F8.6-iiiA-b** (S82, VIVA, no bloqueante) — `Totales` SALE DE iii-A MODELADO Y SIN CONSUMIDOR.
  Es el único de los 5 DTOs del diagnóstico que el bloque no ejercita: no hay índice, no hay función,
  no hay test. Es CORRECTO para iii-A —`TotalesDTO` es dato del horario ENTERO, no de celda, y no
  encaja en el mecanismo de índices por `clavePin`—, pero deja un cabo suelto. TRAMPA DOCUMENTADA que
  quien lo pinte debe tener delante: el javadoc de `TotalesDTO` dice que los totales son conteos SIN
  signo del coste actual y NO tienen por qué coincidir con la suma de los `delta` contrafactuales;
  por eso `indicePenalizaciones` NO acumula. Presentar el total como «suma de los badges» sería
  mentir. → 8.6-iii-B decide dónde vive y con qué advertencia se pinta.

- **D-F8.6-iiiA-c** (S82, VIVA, POSIBLE HUECO DE VALIDACIÓN, sin medir) — S2 SOBRE `aulaFija` SE
  VALIDA SOLO EN LA RUTA JSON. Que dos plazas de una misma actividad no compartan `aulaFija` lo
  rechaza `ProblemaHorarioMapper.verificarAulasFijasDisjuntas` (315-326), que está en `solver/io`, es
  decir en la ruta JSON/fixtures. En `app/mapper/CatalogoMapper` no apareció equivalente al grepear, y
  `ActividadService` NO SE AUDITÓ: se registra como «NO VISTO», no como «no existe». Importa porque el
  CRUD de Actividad (8.5-C1) es hoy la puerta de entrada real de configuración: si la ruta JPA no
  replica la validación, el catálogo podría admitir una duplicación que el modelo CP-SAT da por
  imposible, y el caso pasaría de teórico a alcanzable por API. NO se midió desde S82 A PROPÓSITO: es
  backend, y arreglar superficie de otra capa desde un bloque de frontend es el error que S81 evitó
  con D-F8.6-ii-a. Nota: `ModeloCpSat.usaAula` (1091-1100) devuelve `boolean` y añade el intervalo UNA
  sola vez, así que el modelo TAMPOCO detectaría dos `aulaFija` iguales — la protección descansa
  ENTERA en el mapper. → auditar `ActividadService` al abrir el próximo bloque que toque backend.

- **D-F8.6-iiiB1-a** (S83, VIVA, DE COBERTURA) — EL GESTO DE DESPINAR ENTERO VA A PRODUCCIÓN SIN UN
  SOLO TEST. La suite subió 22 → 23, y ese +1 cubre `pines.ts` y nada más. Sin aseverar: el `output`
  `despinar`, el `<button>` y su `stopPropagation`, la resolución clave→id de `alDespinar`, el no-op
  con `id` null, el borrado de la clave en el `next` del 204, y el traslado de `listar()` a
  `cargarPines()`. Su única verificación hoy es el typecheck de plantillas de `ng build`. Es
  consecuencia DIRECTA y aceptada de dejar `horario-grid.spec.ts` fuera de alcance (estrenar TestBed
  es bloque propio, D-F8.6-iiiA-a). LO CONCRETO QUE SALE SIN RED, señalado por Claude Code: que
  `cargarPines()` se llame UNA vez y no dos es exactamente el bug que S83 corrigió, y hoy depende de
  que nadie vuelva a añadir la llamada al constructor —ningún test lo impediría—. Hermana de
  D-F8.6-iiiA-a: las dos piden la misma capa de test que nadie ha estrenado.
  → CERRADA en S84 (8.6-iv-B): `horario-view.spec.ts` + `horario-grid.spec.ts` cubren el `output`,
  el `<button>`, la resolución clave→id, las DOS salidas de la guarda, el borrado en el `next` del
  204 y el conteo de `listar()` en dos fases, con campaña de 8 mutaciones. Se empezó por el conteo,
  como esta deuda pedía. MATIZ que S84 mide y que esta deuda no preveía: el `stopPropagation` NO
  queda aseverado (el CDK no escucha `click`, así que ninguna mutación lo pondría rojo), y las dos
  mutaciones de la guarda solo son expresables con un cast — ver D-F8.6-ivB-b.

- **D-F8.6-iiiB1-b** (S83, VIVA, no bloqueante) — LA RECARGA DEL ÍNDICE ES CORRECTA POR ACCIDENTE.
  `pinadas` es estado GLOBAL («en TODO el horario», por javadoc) y `GET /api/bloqueos` no filtra por
  horario; `cargar(id)` es por horario. Llamar a `cargarPines()` desde `cargar(id)` no refresca «los
  pines de este horario»: refresca TODOS, siempre. Funciona —cubre el caso real de regenerar y
  volver— pero no por la razón que el código sugiere, y su valor es menor del que el contrato de S83
  le atribuyó. Lo que NO cubre y nada puede cubrir sin polling: dos pestañas abiertas, donde la que
  no pinó no se entera hasta un cambio de ruta. → releer antes de tocar el ciclo de vida de
  `pinadas`, y no asumir que la recarga está ligada al horario.

- **D-F8.6-iiiB1-c** (S83, VIVA, no bloqueante, DE SUPERFICIE DE ERROR) — `mensaje()` MIENTE EN EL
  DELETE. `alDespinar` reutiliza el `mensaje(err)` de `alSoltar` como exigía el contrato (no inventar
  superficie de error nueva), pero su degradado dice literalmente «El servidor rechazó el pin», que
  para un DELETE fallido —p. ej. 404 por `id` rancio tras un `next` perdido— es falso: no se rechazó
  un pin, no se pudo quitar. El caso del `id` rancio NO está cubierto por nada y se presentaría como
  error genérico, indistinguible de un rechazo real. Familia de D-F8.6-ii-a: las dos son superficie
  de error que solo tiene sentido decidir GLOBALMENTE, no de refilón en un bloque de frontend.
  → parametrizar el degradado cuando se decida la política global de errores.

- **D-F8.6-ivB-a-bis** (S89, VIVA, DE COBERTURA, no bloqueante) — LA PRECEDENCIA INTERNA DE
  `mensaje()` NO ESTÁ EJERCITADA. `mensaje()` devuelve `cuerpo?.message || cuerpo?.error || <texto
  con estado>`. El test (24) solo llega a la TERCERA rama, la del degradado, porque el body del
  fixture es `{}`. Las dos primeras —`message` poblado, `error` poblado— y el ORDEN entre ellas no
  las asevera nadie: invertir a `cuerpo?.error || cuerpo?.message || ...` pasaría en silencio.
  Aportada por el contraste de S89 y dejada fuera a propósito, con el mismo criterio con que se
  aceptó (26) y se rechazó ésta: (26) es una TRANSICIÓN DE ESTADO del pinado (un pin previo
  desaparece), y ésta es el FORMATO de un mensaje. La asimetría del criterio va escrita para que el
  próximo bloque no la redescubra. Nota: hoy el degradado es el camino REAL en producción, porque
  `server.error.include-message` está desactivado (D-F8.6-ii-a), así que las dos ramas sin red son
  las que HOY no se ejecutan nunca. → cubrir cuando se decida la política global de errores
  (D-F8.6-ii-a), que es lo que las pondría en uso.

- **D-F8.4-B1-a** (S92, VIVA, DE COBERTURA, no bloqueante) — LA CAÍDA DE M2 DEPENDE DEL MONTAJE, NO
  DEL ASERTO. La mutación M2 (estado inicial `signal(null)` → `signal([])`) cae por (28) SOLO porque
  la primera mitad del test NO llama a `setInput` y el componente lee su valor por defecto. Lo
  declaró el propio Claude Code al reportar la campaña. El aserto discrimina lo que dice discriminar
  —«null pinta pendiente, no limpia»— pero su capacidad de matar M2 es una propiedad del MONTAJE:
  si alguien añadiera un `setInput(null)` explícito «por claridad», M2 sobreviviría en silencio.
  No es defecto del test sino dependencia implícita, y por eso se registra en vez de parchearse.
  → revisar si el bloque que retome el panel toca el montaje de (28).
- **D-F8.6-ivD-a** (S94, VIVA, DE COBERTURA, no bloqueante) — LA CAPA DEFENSIVA DE (3) Y (4) SE
  PERDIÓ AL MIGRAR EL DOBLE. Los tests (3) —pin con `id` null— y (4) —clave ausente del índice—
  emitían sobre `sujetoBorrar` después de aseverar el no-op. Al pasar `bloqueos.borrar` a fresco por
  invocación, ese sujeto dejó de existir y la emisión no podía sobrevivir: en esos dos casos
  `borrar` NUNCA se invoca, así que `ultimoBorrar` queda `undefined` y emitir sobre él reventaría.
  El discriminante PRIMARIO de ambos sigue intacto y no se tocó ningún `expect`: es
  `expect(bloqueos.borrar).not.toHaveBeenCalled()`, que mata la mutación de la guarda ANTES de
  cualquier emisión. Lo que se pierde es la SEGUNDA capa: el escenario «hay suscripción viva y borra
  la clave del índice» ya no se ejercita en esos dos. No merece mecanismo nuevo —sería andamio para
  cubrir lo que el aserto primario ya cierra— pero se registra porque es una consecuencia colateral
  de la opción A que ni el arquitecto ni el usuario previeron al elegirla, y porque una pérdida de
  cobertura no declarada es indistinguible de un descuido. → reconsiderar solo si `alDespinar` gana
  una rama que el `not.toHaveBeenCalled` deje de cubrir.
- **D-F8.5-D2b2-a** (S91, VIVA, de DISEÑO, no bloqueante) — EL PREDICADO DE COBERTURA
  GRUPO←SUBGRUPO ESTÁ TRIPLICADO. `sg.grupos().contains(g)` vive hoy en tres sitios:
  `ModeloCpSat.tocaGrupo` (privado, sobre `InstanciaProgramada`), `VerificadorSolucion.verificarNoSolapes`
  (L619) y el nuevo `verificarTutorias`. Claude Code lo reportó en el turno de código con TRES opciones
  y sin decidir (M-b de la prueba de S91). ELEGIDA la opción 1 —dejarlo— con razón explícita y no por
  pereza: (a) no es lógica duplicada sino ACCESO A UNA ESTRUCTURA, y extraerlo crearía API de dominio
  para envolver un `contains`; (b) extraerlo a `cpsat/` acoplaría verificador↔modelo, que hoy son
  INDEPENDIENTES POR DISEÑO —el verificador vale precisamente porque no comparte código con
  `ModeloCpSat`: si el modelo tuviera un error de cobertura de grupo, el verificador debe poder
  delatarlo—. Es el mismo argumento con el que S64 cortó 8.3 en A/B para no contaminar los recomputos
  gemelos. → reabrir solo si aparece una CUARTA copia o si el criterio de cobertura deja de ser un
  `contains` (p. ej. si algún día dejara de ser ciego al `grupo_padre`), momento en que la
  triplicación pasaría a ser riesgo real de divergencia.

- **D-F8.6-ivD-b** (S96 → CERRADA S99) — `bloqueos.listar` estaba en Subject COMPARTIDO, la única de
  las cuatro llamadas dobladas del contenedor en forma vieja. Cerrada en S99: el doble migra a FRESCO
  POR INVOCACIÓN guardado en `ultimoListar` —forma de `guardar`, no de `borrar`/`generar` (anónimos):
  `listar` se consume en `cargar`/`cargarPines` y (1)/(5) aseveran sobre él, así que hay que poder
  emitir sobre el último Subject—. Ocho cambios en `horario-view.spec.ts` (renombrado + forma del
  doble); ninguna emisión reordenada (los tres `.next([])` sueltos y los dos helpers ya emitían tras
  la suscripción). Los cuatro dobles del contenedor quedan homogéneos. Suite 75 → 75, `src/main`
  intacto. Detalle: bitácora S99 (futura).

- **D-F8.6-B-a** (S97, VIVA, no bloqueante, DE FIRMA QUE MIENTE) — EL GENÉRICO DE `CdkDragDrop` NO
  SE CORRESPONDE CON NINGÚN DATO REAL. `horario-grid.ts:150` anota
  `CdkDragDrop<{ dia: number; orden: number }>`, pero NINGÚN `<td>` lleva `[cdkDropListData]`
  (`horario-grid.html:15`, medido en S97): el día y el tramo llegan por ARGUMENTOS del template
  —`alSoltar($event, i + 1, t)`— y el cuerpo NO lee `evento.container.data`. El genérico describe una
  forma que el CDK nunca puebla, así que quien lo lea creerá que el tramo destino viaja por el
  dropList. Por R5 es una descripción equivocada del mecanismo actual, es decir estado vivo
  equivocado. NO se arregló en S97 por DECISIÓN EXPLÍCITA del usuario (C-3) y no por descuido:
  arreglarlo bien es poner `[cdkDropListData]` y cambiar cómo `alSoltar` obtiene el tramo, que es
  camino de PINADO ya cubierto por (21)-(26) y (35)-(38) —reabrir superficie probada dentro de un
  bloque de diseño—. Hermana de D-F8.5-D2b2-b (javadoc obsoleto): las dos son documentación que
  contradice el código, no defecto funcional. → corregir en el próximo bloque que toque el camino de
  soltado, poniendo el `[cdkDropListData]` o quitando el genérico, pero no dejando los dos.

- **D-F8.6-B-b** (S97, VIVA, ACEPTADA POR DISEÑO, no bloqueante) — EL AVISO DE OCUPACIÓN ES CIEGO A
  DOS DE LOS TRES RECURSOS. `slotsOcupados` (`horario-grid.ts`) solo ve las sesiones de la ENTIDAD
  de la vista actual: en vista por grupo no ve al profesor ni al aula, en vista por profesor no ve
  el aula ni los otros grupos. NO es un hueco de implementación: es la consecuencia de haber
  rechazado la opción A en S97, que habría exigido los ocupantes de los tres recursos y con ellos
  las tres reglas de `verificarNoSolapes` —el CUARTO ESPEJO que la casilla de 8.6-B prohíbe—. La
  incompletitud va DECLARADA en el TSDoc de `slotsOcupados` porque una incompletitud no declarada es
  una promesa falsa (familia de D-F8.6-iiiB1-c, donde el degradado de `mensaje()` miente en el
  DELETE). Lo que hace tolerable la ceguera es la vía C de D-F8.6-A-1: el aviso no decide nada, el
  solver sí, y un pin contradictorio da INFEASIBLE, que es lo que 8.4 existe para hacer amable.
  → NO se cierra ampliando el aviso. Solo dejaría de importar si el backend expusiera un endpoint de
  «¿qué ocupa este tramo?» que respondiera por los tres recursos sin que el cliente razone; hasta
  entonces, cualquier intento de completarlo en TS es la opción A por la puerta de atrás.

- **D-UI-shell** (S97, VIVA, no bloqueante, DE ALCANCE) — NO HAY MAQUETACIÓN DE CHROME DE
  APLICACIÓN. La UI se ha construido pantalla a pantalla por función; no existe layout global,
  navegación por menú, cabecera con logotipo, tema visual ni página de inicio. Indicios (no
  medición cerrada): `styles.css` son cinco líneas —comentario + `@import` del overlay del CDK para
  el diálogo—, y la navegación que ha aparecido es por ruta puntual (`horario/:id`, `paramMap`), no
  un menú. No bloquea ningún bloque funcional. MEDICIÓN PENDIENTE, deliberadamente dejada DENTRO de
  la deuda (decisión del usuario en S97, no en el prompt): confirmar si hay algún shell mínimo en
  `app.component`/`app.routes` antes de fijar el texto definitivo, o esta deuda mentirá sobre el
  estado actual. → fase propia «shell de aplicación» (layout, navegación global, cabecera con logo,
  tema, página de inicio, estados de carga/error de página), DESPUÉS de completar la configuración
  por centro —el menú lista secciones que aún son deuda (D1, D7, D22)— y ANTES del empaquetado
  (Fase 11), que debe entregar algo que parezca una aplicación terminada. Maquetar el menú antes de
  saber sus entradas obliga a rehacerlo.

- **D-seed-demo** (S97, VIVA, no bloqueante, DE ALCANCE) — NO HAY UN SEED POBLADO DESDE LOS DATOS
  REALES DEL CENTRO. «Partir de la BD con los horarios reales» son DOS pasos, no uno, y confundirlos
  es la trampa: (a) un IMPORTADOR que lea los volcados fieles `grupo-*.json`/`aula-*.json` y
  construya el CATÁLOGO (actividades, plazas, subgrupos, particiones, tutorías, compatibilidades de
  aula) — SOLAPA con D8 y arrastra sus cuatro problemas de conciliación (aulas implícitas, talleres
  FPB ausentes del horario por aula, segundo profesor omitido en co-docencia, inconsistencias
  profesor↔plaza entre PDFs)—; y (b) OPCIONAL, cargar el HORARIO YA RESUELTO del centro como
  `HorarioProyeccion` persistida, para que la demo enseñe un horario pintado sin esperar los ~600 s
  del solver a escala real (dato de S44). DISTINCIÓN QUE NO SE PUEDE PERDER: los volcados son el
  horario RESUELTO (celdas colocadas), NO el catálogo de entrada (actividades sin colocar); son
  cosas distintas y (a) transforma uno en otro. El `SeedCatalogoRunner` actual es SINTÉTICO y MUERE
  en el bloque de configuración de jornada (D22): no es la base de la que partir. → fase o bloque
  DESPUÉS de cerrar la configuración por centro; antes se rehace, porque un importador que puebla un
  catálogo cuyo esquema aún no está cerrado se reescribe. La (b), si se aborda, exige decidir ANTES
  si persistir una proyección «desde fuera» del solver es camino legítimo del modelo o atajo que
  solo debe existir para demo.

- **D-demo-cliente** (S97, VIVA, no bloqueante, DE PROCESO) — MOSTRAR LA APLICACIÓN AL USUARIO DEL
  CENTRO NO ES FASE DE DESARROLLO. Es un evento de validación con interlocutor humano y calendario,
  no trabajo de construcción. Se registra para que su DEPENDENCIA quede escrita y nadie intente
  planificarla antes de tiempo: consume D-seed-demo (una BD que enseñar) + D-UI-shell (algo que
  parezca una aplicación). No tiene entregable de código propio; su «hecho» es la sesión con el
  centro y sus salidas son hallazgos que se convierten en deuda o en confirmaciones de D31
  (poblaciones y particiones a confirmar con el centro). → agendar cuando D-seed-demo y D-UI-shell
  estén cerradas, no antes.

- **D-F8.5-D2b2-b** (S91, VIVA, COSMÉTICA, no bloqueante) — EL JAVADOC DE CLASE DE `CatalogoMapper`
  DICE «siete listas». Es PRE-EXISTENTE, no introducida por S91: ya estaba obsoleto desde que se
  añadieron restricciones y bloqueos, y con el onceavo parámetro de esta sesión lo está más. Claude
  Code lo detectó y lo dejó fuera para no expandir alcance, criterio correcto. (El javadoc de clase de
  `GeneradorHorarioService`, que decía «ocho colecciones», SÍ se actualizó en S91 porque el bloque lo
  tocaba.) → commit de limpieza cuando se toque la clase por otra razón; no merece bloque propio.

- **D-F8.6-ivB-b** (S84, VIVA, de MÉTODO Y COBERTURA, no bloqueante) — EL COMPILADOR TAPA DOS DE
  LAS OCHO MUTACIONES, Y ESO DEGRADA LO QUE DOS ASERTOS VALEN. La guarda de `alDespinar` es
  `if (id === null || id === undefined)`. Reducirla a `id === undefined` deja `id` como
  `number | null` y `borrar(id: number)` no lo acepta: **TS2345**. La mutación NO ES EXPRESABLE en
  TypeScript; el compilador es una barrera ANTERIOR al test. Se corrió como 3′ añadiendo el
  `as number` que un desarrollador escribiría para silenciar el error —la única vía por la que ese
  bug llegaría a producción—. Consecuencia honesta y declarada en la réplica, no descubierta
  después: los dos asertos de guarda protegen contra «despiste de guarda MÁS cast», no contra un
  despiste de guarda a secas. Siguen valiendo (el cast es una edición verosímil, es lo que se hace
  para callar un error de compilación) pero valen MENOS de lo que la tabla del contrato afirmaba.
  Familia de método, no de dominio: es la primera vez en el proyecto que una mutación se topa con
  el sistema de tipos en vez de con un aserto. → leer antes de calibrar una campaña de mutación en
  TypeScript: una mutación que no compila NO es una mutación, y hay que declarar el cast que la
  hace expresable.

- **D-F8.6-ivB-c** (S84, VIVA, no bloqueante, DOS OBSERVACIONES SIN DEUDA PREVIA) — Destapadas al
  leer `horario-view.ts` para el contrato de iv-B; ninguna estaba recogida en ninguna deuda y
  NINGUNA se arregló en S84 (fuera del alcance, que era escribir tests, no cambiar producción).
  (a) `Number(pm.get('id'))`: si el parámetro falta, `pm.get('id')` devuelve `null` y `Number(null)`
  es **0**, no `NaN`, así que se dispara un `getProyeccion(0)` contra el backend en vez de un
  camino de error. No lo cubre ninguno de los 7 tests nuevos. (b) La suscripción del constructor
  —`this.route.paramMap.subscribe(...)`— NO lleva `takeUntilDestroyed` ni se desuscribe. Con el
  `ActivatedRoute` real de Angular esto no suele filtrar, pero es una afirmación que NO SE HA
  MEDIDO y se registra como tal, no como «es inocuo». → decidir (a) al tocar el ciclo de carga del
  horario y (b) si aparece una segunda ruta que reutilice el componente.

- **D-F8.6-ivA-a** (S85, VIVA, DE COBERTURA, no bloqueante) — EL 404 QUE EL TSDoc DEFIENDE NO LO
  VERIFICA NADIE, Y SÍ ES TESTEABLE. `diagnostico.service.ts:23-28` dedica SEIS LÍNEAS a argumentar
  que el 404 se deja PROPAGAR sin traducir, porque «no hay horario» (404) y «horario sin violaciones»
  (200 con listas vacías) son estados DISTINTOS y colapsarlos le quitaría al consumidor la
  información para distinguirlos. Es una afirmación de COMPORTAMIENTO y ningún test la sostiene.
  Queda fuera de 8.6-iv-A POR ALCANCE (iv-A congela endpoints, no cubre lógica), NO por imposibilidad:
  MEDIDO en el turno de contraste de S85 que es reddable con `req.flush('', {status: 404, ...})` +
  `subscribe({error})`, cuya mutación `catchError(() => of(null))` COMPILA. Lo mismo vale para
  «devuelve lo que llega» (mutación `map(x => ({...x}))`, también compila). El único item de los tres
  que de verdad NO es reddable es el tipo genérico, que se borra en runtime. IMPORTA que la razón
  quede escrita bien: con la razón falsa —«no puede ponerse rojo»— quien releyera la decisión
  concluiría que el 404 es intestable y no se verificaría nunca. → cubrir si se abre un bloque de
  cobertura de comportamiento del cliente, o al tocar la política global de errores (familia de
  D-F8.6-ii-a y D-F8.6-iiiB1-c).

- **D-F8.6-ivA-b** (S85, VIVA, de MÉTODO, no bloqueante) — LA CASCADA DE `verify()` ROMPE LA
  ATRIBUCIÓN DE UNA CAMPAÑA DE MUTACIÓN. En un spec con `afterEach → HttpTestingController.verify()`,
  una mutación hace que `verify()` LANCE dentro del `afterEach`; eso impide el reset del TestBed y el
  `beforeEach` del test SIGUIENTE del mismo fichero revienta con «Cannot configure the test module
  when the test module has already been instantiated». MEDIDO en S85: M2, que solo toca la URL de
  `listar()`, tumbó (9), (10) y (11) de `bloqueo.service.spec.ts`. NO es el «doble fallo» que el
  contrato de S85 predijo —esa predicción del arquitecto era FALSA y la desmintió la campaña—: es
  cascada, y afecta a todo el fichero. Importa porque una campaña se apoya en QUÉ TEST CAE como señal
  de atribución, y «cayeron tres» no atribuye nada. REGLA DE LECTURA, que salva la atribución y quedó
  escrita en el TSDoc: la víctima REAL falla por `expectOne`; las COLATERALES fallan por «Cannot
  configure the test module». Se decidió NO blindar con `try/finally + resetTestingModule()` (mete
  maquinaria permanente en tres ficheros y estrena un patrón con cero apariciones en el repo) ni
  partir el fichero en varios `describe` (rompería «un describe por fichero»), porque en VERDE
  `verify()` no lanza nunca y la patología solo existe bajo mutación. Familia de D-F8.6-ivB-b: las dos
  dicen que el instrumento de medición tiene sus propias trampas. → leer ANTES de correr una campaña
  de mutación sobre cualquier spec que use `HttpTestingController`.

- **D-F8.6-ivA-c** (S85, VIVA, no bloqueante, NO MEDIDA) — DOS NOMBRES PARA EL MISMO NÚMERO CRUZANDO
  LA FRONTERA DEL CONTRATO. `TramoRef` (espejo de `TramoRefDTO`) declara `orden`, con el comentario
  explícito «ordenEnDia 1..6, nunca `TramoSemanal.id`»; `SesionVista`, en la proyección, lleva `dia` y
  `tramo` como números sueltos, no un `TramoRef`. Si ambos números significan lo mismo, hay dos
  nombres para un solo concepto a los dos lados de la frontera; si significan cosas distintas, la
  conversión no está en ninguno de los dos ficheros TS. Observada por Claude Code al leer
  `bloqueo.model.ts` en S85 y registrada COMO PREGUNTA, no como defecto: decidirlo exige medir
  `horario.model.ts` y los DTO del backend, que es trabajo de BACKEND y estaba fuera del alcance de un
  bloque de frontend (mismo criterio con el que S81 evitó D-F8.6-ii-a y S83 el null de `Bloqueo.id`).
  → medir al abrir el próximo bloque que toque la proyección o `SesionVistaDTO`.

- **D-F8.6-iiiB2a-a** (S87, VIVA, no bloqueante, DE SUPERFICIE DE ERROR) — TERCER CANAL DE ERROR EN
  EL MISMO COMPONENTE, SIN POLÍTICA GLOBAL. `HorarioView` tiene ya `error`, `errorPin` y ahora
  `errorDiagnostico`, cada uno con su selector y su semántica de gateo (`error` quita la rejilla del
  DOM, los otros dos no). La separación es CORRECTA por construcción —acoplarlos vaciaría la rejilla
  ante un fallo que no lo justifica, y reutilizar `mensaje()` repetiría el degradado que miente de
  D-F8.6-iiiB1-c—, pero tres canales es el punto en que la ausencia de política global empieza a
  costar: un cuarto fallo no tendrá dónde ir sin decidir antes qué es un error de página, uno de
  gesto y uno de dato accesorio. Familia de D-F8.6-ii-a y D-F8.6-iiiB1-c. → decidir la política
  global de errores del frontend antes de añadir el cuarto canal, no después.

- **D-S101-num** (S101, VIVA, mejora futura, no bloqueante) — NUMERACIÓN GLOBAL DE TESTS COLISIONADA
  EN LA CAPA COMPONENTES/SERVICIOS. La secuencia `(N)` de specs de esa capa es global y arrastra
  colisiones preexistentes —`(27)`, `(28-30)`, `(35-37)` aparecen con contenidos distintos en dos
  ficheros cada una—, lo que rompe la atribución por `(N)` durante una campaña de mutación. Es
  superficie de specs de H1 (cerrado). Mitigación adoptada desde S101: cada spec nuevo de O-catálogo
  abre secuencia propia desde `(1)` por fichero (precedente sano de los specs de lógica pura de
  `app/horario/`), así que la deuda NO crece con las entidades nuevas. Arreglar la global tocaría
  specs de H1 por algo que no bloquea → no se paga (R-terminado). Cuelga de O-ajuste-cierre (H1).

- **D-S102-spec** (S102, VIVA, mejora futura de PRECISIÓN, no bloqueante) — EL JAVADOC DEL CASO (1) DE
  `configuracion.spec.ts` JUSTIFICA SU DOBLE ASERTO CON UNA PREMISA FALSA. Afirma que quitar `AulaLista`
  (antes `ProfesorLista`) del `imports:` del componente standalone dejaría el tag como elemento
  desconocido y el `querySelector` seguiría en verde, motivando un segundo aserto de refuerzo. No es así:
  en un standalone, quitar el componente del `imports:` es un error de compilación NG8001, no un elemento
  desconocido tolerado en runtime — el doble aserto no cubre lo que su javadoc dice cubrir. Detectada en
  S102 al cotejar el molde; el caso (2) nuevo de S102 lleva el matiz correcto en su comentario. NO se
  corrigió el javadoc del caso (1) porque es de otra sesión (S101) y tocarlo por precisión no desbloquea
  nada (R-terminado). Cuelga de O-ajuste-cierre (H1), familia de precisión de specs junto a D-S101-num.
  → corregir el javadoc del caso (1) si alguna vez se abre esa spec por otro motivo.

- **D-S103-compat** (S103, VIVA, mejora futura, no bloqueante) — EL CRUD DE ASIGNATURA NO ALCANZA EL
  SUB-RECURSO `aulas-compatibles`. `AsignaturaController` expone `GET/PUT /api/asignaturas/{id}/aulas-compatibles`
  (compatibilidad asignatura↔`TipoAula`, Bloque 8.5-C3), pero el CRUD de S103 se limitó a la entidad raíz
  `{codigo, nombreCompleto}` por decisión de alcance (acoplamiento de contrato nulo, medido en M2). Poblar
  compatibilidades es trabajo de USUARIO en la UI —así lo da por hecho D-F8.5-C3-b, que además fija que el
  catálogo real tiene hoy 0 compatibilidades—, y esa UI no existe. Caso simétrico de D-F8.5-C3-a
  «CONTENIDA EN UI»: aquí es una capacidad del backend que la UI NO alcanza. NO bloquea O-catálogo: por la
  semántica de S75 (0 filas ⇒ irrestricta), un centro mínimo creado por UI corre en el solver sin poblar
  ninguna compatibilidad —cumple el criterio de terminado del objetivo—. Arrastra D-F8.5-C3-a y -C3-b.
  Pendiente además la NO-ATOMICIDAD POST→PUT: en un alta, el id no existe hasta que el POST responde, así
  que guardar compatibilidades exige una segunda petición que puede fallar sola (asignatura creada sin sus
  compatibilidades); decidir entre editar la compatibilidad aparte sobre entidad ya existente (más barato,
  no inventa transaccionalidad) o encadenar POST→PUT presentando el fallo parcial. Cuelga del Cambio de
  compatibilidad (tras Grupo, o dentro de O-estructura según se decida al abrirlo). → construir la UI de
  compatibilidades como Cambio propio; estirar el molde con un sub-recurso es el objeto de esa sesión, no
  un añadido a un CRUD plano.

### Deuda consciente CERRADA (histórico)

Deuda ya resuelta, condensada a una línea; el mecanismo vivo en `src/main` se conserva y
el detalle narrativo vive en la bitácora.

- **D-F8.6-ivB-a** — `alSoltar` y tres ramas más del contenedor sin red. NÚCLEO CUBIERTO en S89 (8.6-iv-C) por (21)-(26): las cuatro ramas de `alSoltar` —`aulas: []` y `tramo` sin permutar (21); la clave desde la RESPUESTA y no desde la suelta (22), la dimensión más peligrosa; el ERROR no puebla el Map y `mensaje()` degrada (23)/(24); `getProyeccion` sigue en 1 llamada tras pinar (25a)—, más el `errorPin.set(null)` de reintento de `alSoltar` (25b) y la preservación del índice previo (26), que la deuda no nombraba y destapó el contraste. La rama `error:` de `alDespinar` NO sobrevivió: invoca el MISMO `mensaje()` que (24) cubre y duplicar el aserto sería cobertura fingida. PUNTO (a) —el `errorPin.set(null)` de reintento de `alDespinar`, en `horario-view.ts:236`— CERRADO en S94 por (36), junto a su gemela D-F8.4-B2-a. PUNTO (b) y último resto —el invariante del TSDoc de `cargarPines` (125-132) de que el índice NO se recarga al cambiar de vista ni de entidad— CERRADO en S96 por (37) y (38), que aseveran `bloqueos.listar` en 1 llamada tras `cambiarVista` y tras `cambiarEntidad`: el aserto va sobre el COLABORADOR y no sobre `pinadas`, porque un doble que devuelve la misma lista la deja idéntica y la mutación quedaría verde. El invariante se sostiene sobre una AUSENCIA de llamada (un solo call site de `cargarPines`, l.189 en `cargar`), luego la campaña fue POR ADICIÓN. Detalle: bitácora S84, S89, S94 y S96 (futura).

- **D-F8.4-B2-a** — el `errorGeneracion.set(null)` con que `lanzarGeneracion` limpia el error previo al reintentar no lo ejercitaba nadie: con una sola generación por test, borrarlo quedaba verde. CERRADA en S94 por el test (35) de `horario-view.spec.ts`, que encadena fallo → reintento y asevera la fase INTERMEDIA —el aviso de error desaparece ANTES de que el segundo Subject responda—, única que discrimina el `set`; la versión débil (`toHaveBeenCalledTimes(2)`) sigue dando 2 con el `set` borrado y se descartó por eso. Exigió migrar el doble de `horario.generar` de Subject compartido a FRESCO POR INVOCACIÓN: uno ya cerrado por `.error()` redispara síncronamente al re-suscribirse y hace la fase inobservable. Se cerró JUNTO con su gemela —el punto (a) de D-F8.6-ivB-a, mismo mecanismo en `alDespinar`— como S93 exigía. Detalle: bitácora S94.
- **D-F8.5-D2b1-a** — la ruta JPA clavaba `tutorias = List.of()`: el `ProfesorTutoriaRepository` existía desde S77 pero no llegaba al dominio, y la ruta JSON transportaba tutorías mientras la de producción las perdía. CERRADA en S91: `CatalogoMapper.aProblemaHorario` gana un ONCEAVO parámetro `List<app.catalog.ProfesorTutoria>` —sigue `static` puro, sin repositorios inyectados— y `GeneradorHorarioService.cargarProblema` pasa `profesorTutoriaRepository.findAll()` dentro de su `@Transactional(readOnly=true)`, que es lo que permite navegar los `@ManyToOne(LAZY)` de la PK. La conversión resuelve profesor y grupo por IDENTIDAD contra los índices ya materializados (nunca `findById`) y traduce el enum por nombre abortando si no existe en destino. Aseverado por B-T5 (integración `@DataJpaTest` sobre SQLite real), que es su ÚNICO killer: ningún unitario caza el cableado del servicio. Detalle: bitácora S91.
- **D-F8.5-D2b1-b** — el `rol` no tenía aserto de transporte: la mutación «el mapper clava `TUTOR_PRINCIPAL`» solo caía por el test del rol INVÁLIDO, y como el fixture de S90 llevaba `TUTOR_PRINCIPAL`, clavarlo era indistinguible del acierto. CERRADA en S91 por DOS pares discriminantes: T1/T2 en solver (mismo profesor y mismo grupo, `TUTOR_PRINCIPAL` no viola / `CO_TUTOR` sí) y B-T1/B-T2 en app (un `CO_TUTOR` llega como `CO_TUTOR` y no colapsado). La campaña distingue además N2 (rol filtrado: ausente) de N3 (rol colapsado: presente pero mal), que son firmas distintas. Detalle: bitácora S91.

- **D4** — Modelado explícito de recursos compartidos (Gim, Pista) por si el solver no bastaba a escala. CERRADA en S36: el instituto completo (26 grupos) resultó FACTIBLE con modelado conservador (EF en aulaFija), 0 duras, Gim 28/30 sin que Gim/Pista compitan hasta romper; no hizo falta relajar EF a aulasCandidatas. La pregunta de fondo (¿Gim/Pista compiten de verdad y rompen a escala total?) tiene respuesta definitiva: NO. Detalle: bitácora S36.
- **D13** — El IntVar de tramo usa índice plano, y con duracionTramos > 1 un bloque podía cruzar la frontera de día y la del recreo (dos caras; la nota original solo veía la de día). CERRADA en S33: `ModeloCpSat.iniciosValidosDeBloque` restringe el dominio del inicio por lista blanca a las posiciones desde las que el bloque cabe entero en el día sin cruzar el recreo (no-op para duracion=1), con espejo independiente en `VerificadorSolucion.tramosOcupados` + `verificarBloquesConsecutivos`; además `verificarNoSolapes` pasó a contar por TRAMO OCUPADO y no solo por el de inicio, cerrando una ceguera a solapes en tramos interiores. Deja viva D22 (frontera de recreo hardcodeada). Detalle: bitácora S33/S34.
- **D14** — `VerificadorSolucion` no comprobaba el no-solape por grupo (S9). CERRADA en S15: `verificarNoSolapes` añade un cuarto conteo por `GrupoAdministrativo` derivado del Set de subgrupos POR INSTANCIA (los subgrupos de un mismo grupo en una actividad coordinada/desdoble colapsan a uno, ciego al `grupoPadre`). Detalle: bitácora S15.
- **D15** — doble ceguera al no-solape de aula: `VerificadorSolucion` contaba el aula con un Set POR INSTANCIA (dos plazas de una misma instancia con la misma aula colapsaban a 1, violación de S2 no reportada) y la rama `aulaFija` de `restriccionNoSolapeAula` añadía el intervalo por instancia (tampoco la prevenía). CERRADA en S18: el aula se cuenta POR PLAZA (profesor y subgrupo siguen por instancia, correcto por S1) + validación `verificarAulasFijasDisjuntas` en `ProblemaHorarioMapper` que rechaza dos plazas de una actividad con la misma `aulaFija` ANTES del solver (no aplica a aulasCandidatas: las separa `addExactlyOne` + no-solape). Detalle: bitácora S18.
- **D23** — curva de coste del solver NO LINEAL a escala (instituto completo ×78 en tiempo por ×1,53 en grupos); la optimización no converge (FEASIBLE con cota abierta). CERRADA en S43 como DECISIÓN DE PRODUCTO (FEASIBLE sin optimalidad probada es el modo aceptado a escala). De las tres palancas: (a) límite de tiempo con mejora incremental sigue viva; (b) poda de aulasCandidatas resultó INVIABLE a escala (S42) y quedó LATENTE y OFF por defecto (`construirConObjetivo()` delega en `construirConObjetivo(false)`; constantes `UMBRAL_PODA_AULA`/`MAX_AULAS_PODA` y `candidatasPodadas` intactas — ver D-F8.2b-4B); (c) warm-start ("Bloque 15b", S40) ayuda (215→204) pero no converge. El experimento pareado (S43) mostró que la no-convergencia es ESTRUCTURAL, no de un bloque. Detalle: bitácora S36/S40/S42/S43.
- **D24** — la suite se autoenvenena por contención de CPU (dos tests de límite ~600 s en cada `mvn test` disparan el wall-clock). CERRADA en S39: `@Tag("escala")` en los pesados + exclusión por defecto vía property `surefire.excluded.groups=escala` + perfil `escala` que la invierte (`mvn test` rápido; `mvn test -Pescala` corre solo los pesados). Reactivada agravada por D25 (VIVA) para el perfil `-Pescala` completo. Detalle: bitácora S38/S39.
- **D28** — `CatalogoMapper.aProblemaHorario` pasaba `List.of()` a restriccionesHorarias porque el catálogo JPA no materializaba la entidad `ProfesorRestriccionHoraria`. CERRADA en S51: materializada la entidad (§4.3) con su repositorio y `CatalogoMapper.aRestriccionHoraria`; el tramo se resuelve por identidad de objeto (IdentityHashMap TramoSemanal→Tramo, no por el código sintético L1..V6, no reabre D27). Queda viva solo la parte de D21 (el peso por-restricción sigue sin consumirse en el objetivo). Detalle: bitácora S51.
- **D-F8.5-A-a** — Integridad referencial del borrado de catálogo. Dos mitades: ESQUEMA (FK reales + pragma) CERRADA en S73 (8.5-C2a-DDL: schema.sql con 27 FK + PRAGMA foreign_keys=ON por conexión → SQLITE_CONSTRAINT_FOREIGNKEY muerde); APLICACIÓN (borrado amable → 409 legible) CERRADA en S74 (8.5-C2b): excepción de dominio ReferenciaEntranteException + @Query nativas de conteo inverso por raíz (opción b: sin PlazaRepository, sin romper el agregado) + guarda en cada Service.borrar (409 antes de tocar la BD) + catch→409 en cada Controller (opción 2A: sin @ControllerAdvice). 7 raíces cubiertas. Hallazgo de S74: el §B de C2b contó por error subgrupo_grupo.subgrupo_id como referente entrante de Subgrupo, pero es su POBLACIÓN PROPIA (Subgrupo es owner del @ManyToMany; Hibernate la limpia al borrar) → ningún subgrupo era borrable; corregido a solo plaza_subgrupo. Regla derivada: referencia entrante = FK que un TERCERO controla, no toda FK que apunte a mi id; las FK del propio agregado no cuentan. El mapa de FK de S73 era correcto (ya listaba solo plaza_subgrupo para Subgrupo). Detalle: bitácora S74.
REVERSIÓN PARCIAL EN S75 (8.5-C3): la Referencia de `asignatura_aula_compatible` que este
bloque había añadido a AsignaturaService.borrar se RETIRA, y su @Query contarCompatibilidadesDeAula
se borra. Razón: las compatibilidades se reclasifican como POBLACIÓN PROPIA de Asignatura
(cascade), no como referente entrante de un tercero — misma relación que Plaza↔Actividad
(D-C1-A). Aplica la propia regla derivada de S74, no la contradice: lo que cambió fue la
clasificación de la relación, no la regla. Las otras dos FK (actividad, plaza) siguen contándose.
- **D-F8.5-D1-a** (S76 → CERRADA S77) — Tutoría del PDC no modelada. Cerrada por 8.5-D2a:
  ProfesorTutoria en JPA + herencia PDC←padre por copia en PdcService.crear. Detalle: bitácora S77.

### Notas técnicas validadas en Fase 0

- OR-Tools CP-SAT en Java funciona en Windows sin recompilar desde Linux
- El jar de ortools-java incluye los nativos Windows (.dll) embebidos; 
  Loader.loadNativeLibraries() los extrae en runtime automáticamente
- jpackage debe ejecutarse SIN --add-modules para incluir el JRE completo;
  con módulos mínimos falla por dependencias transitivas de OR-Tools/Protobuf
- Bundle resultante: ~200-250MB. Optimización de módulos diferida a Fase 11
- Distribución: zip del app-image. Sin permisos de administrador en Windows

### Notas técnicas validadas en Fase 6

- Hibernate 7.4.1 persiste LocalTime en SQLite vía el dialecto de comunidad
  org.hibernate.community.dialect.SQLiteDialect y lo recupera intacto, sin
  fallback a String (verificado S46, round-trip del catálogo)
- Spring Boot 4 modularizó los test slices de persistencia. @DataJpaTest ya NO
  viene en spring-boot-starter-test: hay que añadir spring-boot-starter-data-jpa-test
  en scope test. Los paquetes cambiaron en SB4:
  org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest y
  org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
  (verificado S46; afecta a todo test de slice JPA de Fase 6 en adelante)
- **Límites del community SQLiteDialect 7.4.1 para integridad de esquema (medidos en S73, 8.5-C2a-DDL):**
  (1) NO emite cláusulas FOREIGN KEY en el DDL de hbm2ddl, ni con `@OnDelete`, ni con
  `@ForeignKey(ConstraintMode.CONSTRAINT)`, ni combinadas (verificado: DDL byte-idéntico en las tres
  variantes). (2) `ddl-auto=validate` es INUSABLE: crea la PK de identidad como `integer` (necesario
  para el rowid autoincremental) pero la valida esperando `bigint` (mapeo Long→BIGINT), y falla incluso
  contra un esquema que él mismo generó. (3) Más grave: el DDL que genera declara la PK `id` SIN el tipo
  `integer` en 8 tablas (actividad, aula, aula_bloqueada, nivel, profesor_restriccion_horaria, sesion,
  sesion_bloqueada, tramo_semanal); en SQLite, `id` sin tipo con `primary key(id)` NO es alias de rowid,
  así que la columna queda NULL mientras el rowid oculto sí se autoincrementa. Sin FK y sin recarga por
  id fresco (fuera del caché L1) el defecto era invisible; con FK reales, ninguna FK entrante resuelve.
  Corrección en `schema.sql`: normalizar esas 8 PK a `id integer`. Consecuencia latente: cualquier `.db`
  creado por hbm2ddl (antes de S73) tiene esas 8 columnas `id` en NULL y no se arregla cambiando el
  esquema —requeriría recreación— (deuda D-F8.5-C2a-a; sin producción hoy, teórico).
- **PRAGMA foreign_keys por conexión (S73):** en este stack (SB4 + Hikari 7.0.2 + Xerial 3.53.2) ni
  `spring.datasource.hikari.connection-init-sql` ni el parámetro de URL (`?foreign_keys=on/true`) dejan
  el pragma ON en las conexiones del pool (leído `foreign_keys=0` en las tres vías). Solo aplica un
  `PRAGMA foreign_keys=ON` explícito ejecutado por conexión física: vehículo final = auto-config que
  envuelve el DataSource y lo ejecuta en cada `getConnection()`. Aserto de que muerde =
  `SQLITE_CONSTRAINT_FOREIGNKEY` (getErrorCode()==19). Sin el pragma, declarar FK en el esquema NO basta
  (ambas piezas son necesarias). Además, el slice `@DataJpaTest` de SB4.1 solo carga las auto-configs
  listadas en `META-INF/spring/org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa.imports`;
  hubo que añadir ese fichero en test-resources para que el customizer corriera en los slices.

### Por qué OR-Tools sobre Timefold (no reabrir)

Timefold es metaheurístico: modela la simultaneidad de desdobles y 
agrupamientos como penalizaciones que el solver "intenta" respetar, 
no como constraints estructurales. En este dominio los desdobles y 
agrupamientos son ubicuos y centrales, no casos excepcionales. 
OR-Tools CP-SAT los expresa como igualdad de variables de tramo, 
lo cual es estructuralmente correcto y reduce el espacio de búsqueda.

### Hallazgos del análisis de PDFs (datos reales del centro)

Los PDFs del proyecto son horarios reales de un IES de Sevilla y han
sido analizados. Hallazgos relevantes para el modelo de datos:

- La densidad de simultaneidad es mayor de lo estimado: el 40-50% de
  los tramos de grupos de ESO contienen sesiones coordinadas
- En 1ºESO, el tramo 10:00-11:00 puede contener simultáneamente:
  desdoble CyR (2 sesiones) + agrupamiento RefMt (3 sesiones) + OyD,
  afectando a 4 grupos completos con 6 sesiones coordinadas
- En 1ºBachillerato, hasta 5 optativas simultáneas mezclan alumnos
  de 4 grupos distintos en el mismo tramo
- Los agrupamientos no son casos excepcionales: son estructurales
- Aulas especializadas (A12In, A6, B07, TALL1, C01) aparecen saturadas
  en tramos concretos con restricciones de uso muy estrictas
- Los PDFs sirven como casos de prueba concretos para validar el modelo
- Los desdobles de CyR en ESO cruzan grupos (caso que el modelo unificado
  cubre como un caso particular de actividad multi-plaza multi-subgrupo)
- Los PDC de un nivel se agrupan transversalmente en A8 para currículo
  alternativo, no son satélites aislados de cada grupo ordinario
- La **co-docencia intra-aula** (dos profesores con un grupo en una
  sola aula) es patrón estructural en LCL de 1ºESO del centro de
  referencia. No es desdoble: no hay dos aulas ni dos profesores con
  poblaciones distintas. Discriminador operativo: número de aulas
  listadas en la celda del horario. Una sola aula con varios profesores
  ⇒ co-docencia; varias aulas ⇒ desdoble/agrupamiento (Hallazgo J)

### Registro detallado de sesiones S10–S31

Archivado en `docs/bitacora-sesiones.md` (Sesión 44, higiene documental).
El registro detallado de las sesiones S10 a S42 vive en la bitácora; este
plan conserva solo el estado vivo (criterios, deuda, decisiones permanentes,
bloques de fase) y las 4 últimas cabeceras compactas de sesión.

---

## Señales globales de alerta

Si alguna de estas situaciones ocurre, para y replantea antes de continuar:

- Llevas más de 3 sesiones de trabajo en la misma fase sin avanzar
- El solver viola restricciones duras en el instituto completo después de Fase 5
- El modelo de datos necesita cambios estructurales después de Fase 6
- El bundle Windows requiere pasos manuales del usuario
