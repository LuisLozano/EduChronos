# Plan de Trabajo — Aplicación de Horarios Escolares

## Cómo usar este documento en el Project de Claude

Pégalo como instrucción de contexto en el Project. Al iniciar cada sesión de trabajo, 
indica en qué fase estás y qué has completado. El advisor usará este plan para orientarte.

---

## Principios de avance

1. **No pasar de fase sin cumplir los criterios de verificación**
2. **El solver va primero. Ninguna UI antes de la Fase 4**
3. **Complejidad incremental: cada fase añade UNA capa nueva al solver**
4. **Criterio de bloqueo: si llevas más de 2 sesiones atascado en algo, 
   pídele al advisor que replantee el enfoque, no que lo parchee**

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

Fase actual: 8 — UI: configuración y ajuste manual (EN CURSO desde S57). Bloques 8.6-i + 8.6-ii
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
### Sesión 81 — Fase 8, Bloques 8.6-i + 8.6-ii: cliente de bloqueos y arrastre que pina.
  Modo híbrido. 1 commit de código (solo `app/frontend/`) + doc aparte. §A DE MEDICIÓN sobre el
  ESTADO REAL DEL FRONTEND (lectura literal de ficheros + `find`; instrumento más barato,
  precedente S77-S80).
  SALIDA DE §A: cero librerías de d&d y cero `@angular/cdk` —tampoco transitivo, porque no hay
  Material que lo arrastre—; CONFIRMA la casilla de 8.6 y ELIMINA el atajo que supuse al abrir.
  Angular 21. DATO NO PREVISTO Y DECISIVO: el runner es **Vitest + jsdom**, que NO implementa la
  HTML Drag and Drop API — cualquier librería basada en `dragstart`/`drop` haría el gesto
  INTESTABLE. Es el criterio que eligió CDK, no la limpieza. El cliente de 7B conocía UN solo
  endpoint (`GET /api/horarios/{id}/proyeccion`): cero campos de atribución (8.3-C) y cero de
  bloqueo (8.2b-iv) en el modelo TS.
  HALLAZGO FAVORABLE: `SesionVista` ya lleva (`actividadCodigo`, `indice`), la clave que
  `BloqueoDTO` declara para el cruce (D-6). 8.6-i NO toca backend ni `SesionVistaDTO`: la
  predicción de S66 se cumple, medida.
  8.6 PARTIDO en i/ii/iii. El antiguo iii (candado) se FUNDE en i: el `GET /api/bloqueos` es
  precondición del CANDADO (sin él solo se conocen los pines de esta sesión de navegador), no del
  POST. Mi justificación inicial de la fusión —«sin GET cada arrastre borra los pines anteriores»—
  era FALSA y la corrigió la lectura literal del javadoc: el POST es idempotente POR INSTANCIA, no
  reemplazo de colección. D-F8.6-ii-2 RETIRADA antes de implementarse.
  DECISIONES CERRADAS: D-F8.6-i-1 (`BloqueoService` TS propio; `HorarioService` se declara de solo
  lectura en su javadoc y no se contamina), D-F8.6-i-2 (`bloqueo.model.ts` nuevo, espejo de los 4
  records), D-F8.6-i-3 (`Set` de claves `${actividadCodigo}|${indice}`), D-F8.6-ii-1 (envoltorio de
  celda por PAR actividad+índice; `agruparPorActividad` NUEVA, construida sobre `agruparPorSlot`,
  que queda intacta con su test), D-F8.6-ii-3 (el arrastre pina SOLO tramo, `aulas: []`; el aula es
  otro gesto), D-F8.6-ii-5 (sin movimiento optimista: el candado NO se pinta hasta el OK del POST;
  en 400 el `Set` no se toca y no hay nada que revertir), D-F8.6-ii-6 (`@angular/cdk@21.2.14`,
  `DragDropModule`, verificado por instalación limpia sin peer warnings ANTES de cerrar contrato).
  DOS ERRORES DE ESPECIFICACIÓN DEL ARQUITECTO, ambos destapados por Claude Code AL PREGUNTAR ANTES
  DE TECLEAR: (1) especifiqué el fixture de 2b como «desdoble CyR + OyD = dos actividades
  distintas»: FALSO — `proyeccion-1eso.fixture.ts:26-41` las modela como SEIS PLAZAS de la única
  actividad `Bloque-CyR_OyD_RefMt-1ESO`, que es lo correcto por dominio (S5 obliga a que las plazas
  simultáneas compartan tramo; el modelo unificado existe para no confundir «seis destinos» con
  «seis actividades»). Ningún slot del fixture real tiene dos actividadCodigo distintos. Resuelto
  con FIXTURE LOCAL en el spec, sin contaminar `PROYECCION_1ESO` (oro del centro, patrón S41).
  (2) especifiqué «en ERROR revierte»: SIN SUJETO — sin movimiento optimista no existe estado que
  revertir. CUARTA SESIÓN CONSECUTIVA (S78, S79, S80, S81) con el mismo género de error.
  TERCER ERROR MÍO, ESTE DESTAPADO POR RÉPLICA (no por ejecución): al ver que `agruparPorActividad`
  reutiliza `clavePin` —buena decisión, una sola fuente de D-6— DI POR CUBIERTA la mitad «índice»
  de la clave. NO lo estaba: el fixture local tenía todos los `indice` a 1, y el test que tocaba dos
  índices los ponía en SLOTS DISTINTOS, donde los separa `claveSlot`, no `clavePin`. Mutar `clavePin`
  ponía rojo SOLO `pines.spec.ts`. Corregido con `SLOT_DOS_INDICES` (misma actividad, índices 1 y 2,
  MISMO slot) y test (6). Es cobertura fantasma: reutilizar una función no hereda su test.
  DECISIÓN SOBRE EL FIXTURE DEFENSIVO: `SLOT_DOS_INDICES` modela un estado que el DOMINIO PROHÍBE
  (dos repeticiones de una actividad solaparían al grupo). Se mantiene a propósito: `agruparPorActividad`
  es función PURA y su contrato es agrupar por el par, no validar horarios; la invariante la
  garantiza el solver. Si el fixture solo tuviera estados válidos, el test NO discriminaría (práctica
  (c)). La alternativa —atacar `clavePin` desde `proyeccion.spec.ts`— es peor: no sobreviviría a un
  refactor que dejara de usar `clavePin`. Declarado en el TSDoc del fixture.
  ASERTOS: TRES MUTACIONES, las tres caen y restauran verde — `clavePin` ignorando el índice (rojo en
  `pines.spec.ts` Y en `proyeccion.spec.ts` (6) tras la ampliación), agrupado plano en
  `agruparPorActividad` (rojo en (5)), y `claveSlot` ignorando el tramo (rotura trivial de
  demostración de suite no-vacía). Suite frontend 6 → 13.
  Decisiones de implementación de Claude Code, revisadas y aceptadas: `agruparPorActividad` reutiliza
  `clavePin` (una sola fuente de D-6); `cdkDropListGroup` conecta los 30 `cdkDropList`; soltar en el
  mismo slot no emite.
  PRETTIER NO APLICADO a propósito: el repo no está formateado (marca ficheros preexistentes que el
  bloque no tocó) y aplicarlo mezclaría ruido de formato con el diff del bloque. Sería commit propio.
  `package.json`/`package-lock.json` van en el commit de CÓDIGO, no aparte: sin la dependencia el
  `import` de `DragDropModule` no compila y el commit no construiría (la disciplina separa código de
  DOCUMENTACIÓN, no código de su manifiesto).
  Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva).
  DEUDA NUEVA: D-F8.6-ii-a (el `reason` de `ResponseStatusException` no viaja al cliente).
  Siguiente: 8.6-iii (badge de delta blando + resalte de violación; MOCKUP PREVIO OBLIGATORIO y
  cableado de 8.3-C, que el cliente no conoce), 8.4-B (MOCKUP PREVIO) u 8.5-D2b (solver), a decidir
  al abrir sesión.
Última sesión registrada (previa): Sesión 80 — HIGIENE DOCUMENTAL: condensación de 8.5 y archivado de ventana (sin código).
  Modo interactivo (documentación; el repo NO se tocó). 2 commits de doc, ninguno de código.
  ALCANCE elegido sobre cuatro candidatos (8.6, 8.4-B, 8.5-D2b, limpieza): la limpieza de 8.5
  llevaba desplazada TRES veces (S78, S79) y su condición habilitante se cumple desde S78. El
  motivo por el que S79 la desplazó —concentrar dos operaciones de riesgo sobre el mismo fichero—
  desaparece por construcción en una sesión sin código. Los tres candidatos de código NO estaban
  tan listos como su casilla sugiere: 8.6 arrastra una elección de librería de d&d no tomada
  («no hay librería de d&d en el frontend hoy»); 8.4-B dibujaría dos severidades cuando
  D-F8.4-A-c dice que solo hay una viva; 8.5-D2b debe INVERTIR `CatalogoMapperActividadTest:136`,
  que hoy asevera el olvido de D-B5-5.
  §A DE MEDICIÓN (greps sobre el plan; instrumento más barato, precedente S77/S78/S79). Universo:
  los 40 tokens citados textualmente en las casillas de 8.5. Se simuló el escenario CONJUNTO
  (condensar + archivar S76 en la misma sesión). SALIDA: ningún token caía a cero, pero TRES
  quedaban como definición SIN NINGÚN CITANTE —D-F8.5-C3-a, D-F8.5-C3-b y D-F8.5-D1-b—. El caso
  agudo es D-F8.5-D1-b: sus dos citantes eran la cabecera de S76 (que se archiva) y la casilla de
  8.5-D1 (que se condensa), y S80 se los quitaba A LA VEZ. Es el patrón exacto que costó a S62 la
  descomposición de Fase 8.
  CORRIGE UNA PREVISIÓN DE APERTURA: dije que las deudas D-F8.5-* estaban definidas en «Deuda
  consciente VIVA» y que condensar las casillas no las mataría. Cierto para NUEVE de doce, FALSO
  para tres. La diferencia solo se vio por grep.
  DECISIÓN F80-1: R4 se lee LITERAL («ni citante vivo NI definición viva» ⇒ definición basta),
  pero no hizo falta interpretarlo: el formato de condensación acordado —«qué (Sxx) →
  deuda/decisión superviviente; Detalle: bitácora Sxx»— YA reserva sitio al token en «deuda
  superviviente». Coste cero. Medido después: C3-a 2→2, C3-b 2→2, D1-b 3→3.
  DECISIÓN F80-2: la NOTA del seed sale de la casilla de 8.5-E y va al párrafo «Diferibles»,
  pegada a D22, que es donde el seed muere. Gana el nombre de clase real `SeedCatalogoRunner`,
  que la nota original omitía (R5 pide mecanismo de src/main, no perífrasis).
  DECISIÓN F80-3: 8.5-D2b y 8.5-D3 quedan ÍNTEGROS. Están ABIERTOS y R5 dice que lo PENDIENTE es
  estado vivo: D2b lleva la NOTA DE DISEÑO (S8 no va en ModeloCpSat) y D3 su CRITERIO DE
  REAPERTURA, sin el cual «aplazado» degenera en «olvidado». Se condensan SIETE casillas cerradas,
  no nueve.
  CONSERVADO POR R5 dentro de las líneas condensadas, pese a alargarlas: el funnel único
  `resolverContenido` (C3), la asimetría cascade/409 de la tutoría (D2a), `PESO_INDISP_BLANDA`
  (E) y «sin @ControllerAdvice» (C2b) —este último porque D-F8.5-E-c apunta explícitamente a esa
  decisión y sin ella la deuda apuntaría al vacío—. PERDIDO A PROPÓSITO (vive en bitácora): los
  recuentos de suite por bloque (la suite viva es 315), «Flyway descartado», «opción b / opción
  2A», el falso positivo de Subgrupo, la tipificación B07/A12In y las decisiones de mockup de E.
  HALLAZGO COLATERAL: la casilla 8.5-A/A'/B ya era una línea, sin deuda ni remisión. NO se
  condensa: GANA texto («→ sin deuda viva; Detalle: bitácora S69/S70/S71»), porque le faltaba la
  remisión que el formato exige.
  PARADA REGISTRADA: el prompt manda «seguir el PROTOCOLO DE ARCHIVADO del plan». Ese protocolo
  NO existe como sección nombrada en el plan ni en la bitácora —verificado por grep de
  «protocolo»/«archivad» en ambos—. Se archivó siguiendo el PRECEDENTE OBSERVABLE (formato de las
  entradas S69-S75) más R4/R5. Si se quiere protocolo, hay que escribirlo; hoy es costumbre, no norma.
  VERIFICACIÓN: R4 por grep, 40 tokens ANTES vs DESPUÉS, cero huérfanos; seis tokens bajan una
  unidad y se comprobó UNO A UNO que conservan citante vivo. Diff del cuerpo: tres regiones
  tocadas y ninguna otra. Plan 1869 → 1831 (−38) por la condensación, y −46 más por el archivado.
  Costura revisada: 8.4-B intacto encima, 8.5-D2b/D3 intactos, 8.6/8.6-B sin tocar.
  Código NO tocado: solver/ ni app/ ni frontend/ → referencia-codigo-solver.md NO regenerada,
  modelo_datos_fase1.md NO tocado, suite sin cambio (app 315, solver 78).
  DEUDA NUEVA: D-F8.0-a (el PROTOCOLO DE ARCHIVADO se invoca pero no existe escrito).
  Siguiente: 8.6 (Angular, contrato cerrado en S67; empezar por medir el estado real de frontend/
  y elegir librería de d&d), 8.4-B (MOCKUP PREVIO) u 8.5-D2b (solver, regenera la referencia).
Última sesión registrada (previa): Sesión 79 — Fase 8, Bloque 8.4-A: PRE-VALIDACIÓN por condiciones necesarias (D18).
  Modo híbrido. 4 commits de código (1ea9103 + 838f944 producción/tests; 357d7c2 + 6cf44cb la
  corrección de (c)) + doc. §A DE MEDICIÓN sobre CÓDIGO (greps + lectura literal) y §A-bis sobre
  los VOLCADOS.
  SALIDA DE §A: el hueco es TOTAL, no parcial. Las cuatro condiciones de D18 dan NO; `DemandaCurricular`
  (§4.5) NO está materializada. Lo único que se validaba antes del solve era integridad referencial y
  nulidad; CERO aritmética de capacidad. CORRIGE MI SUPOSICIÓN DE APERTURA: dije que el INFEASIBLE
  podía salir opaco o 500; es FALSO —`HorarioController:59` ya traducía a 422 con test que lo cubre—.
  Lo que faltaba no era el status sino el CONTENIDO: `HorarioInfactibleException` lleva UN SOLO String.
  `DiagnosticoService` NO sirve de base: exige `HorarioGenerado` persistido (404 si no), y 8.4 corre
  ANTES de que exista horario; comparten solo `cargarProblema()`. Patrón heredable = `ReferenciaEntranteException`.
  SALIDA DE §A-bis (volcados): CERO guardias, reducciones u ocupación no docente, y NO PUEDE HABERLAS
  —las fuentes son rejillas por grupo y por aula, y una guardia no ocupa ninguno de los dos—. No hay
  volcado de profesor ni lo habrá. CONSECUENCIA: el numerador de (a) es medible desde los volcados; el
  DENOMINADOR no lo es por ninguna fuente del proyecto. Que guardias y reducciones se declaren DURA es
  DECISIÓN DEL ARQUITECTO (S79), no dato derivado; si el centro no las declara, (a) produce falsos
  NEGATIVOS, nunca positivos. Escrito literal en el javadoc.
  DECISIONES CERRADAS (F84-1..5): 8.4 PARTIDO en A/B —D20 no se puede dibujar antes de que existan
  avisos que enseñar, luego el MOCKUP PREVIO se retira de esta sesión y va a 8.4-B—; excepción propia
  en app/ con carga estructurada, sin tocar solver/; (b) palomar FUERA por riesgo de falso positivo;
  una implementación, dos llamadores.
  DOS ERRORES DE ESPECIFICACIÓN DEL ARQUITECTO, ambos destapados por Claude Code al preguntar antes de
  teclear (registrados, no tapados). (1) Especifiqué (a) «suma por plaza»: MAL. Las plazas de una
  actividad son SIMULTÁNEAS (S5); un profesor en dos plazas de la misma actividad consume UN tramo.
  Contar por plaza sobrestima → falso positivo. (a) y (c) deduplican por actividad: helper único, dos
  ejes. (2) Especifiqué (d) sin distinguir patrón: una NEUTRA con `repeticiones > días` NO es
  infactible (7 caben en 5 días con 6 tramos). (d) se PARTE en (d1) solo DISTRIBUIDA y (d2)
  `rep × dur > tramos totales` para todas, con `regla` distinta y mensaje distinto.
  HALLAZGO QUE INVIRTIÓ UNA DECISIÓN MÍA (medido en el código por Claude Code, no razonado): (c) estaba
  fijada como AVISO con el motivo «si el centro configura CyR/OyD como actividades separadas, la suma
  sobrestima». FALSO bajo el modelo actual: `ModeloCpSat:1046-1074` impone `addNoOverlap` por grupo con
  `tocaGrupo` deduplicando por INSTANCIA DE ACTIVIDAD —misma unidad que (c)—, así que si se configuran
  separadas el solver TAMPOCO las deja compartir tramo. Es infactibilidad GARANTIZADA. (c) SUBE A ERROR.
  TERCERA SESIÓN CONSECUTIVA (S77, S78, S79) con el mismo género de error: aplicar un criterio sin
  comprobar el precedente en el código. Las tres veces lo destapó la ejecución, no el razonamiento.
  ASERTOS: 10 tests. A3 es el ORO y su calibración no es gratuita: el fixture hace que AMBAS cuentas
  superen el techo (7 por actividad, 10 por plaza, techo 5) para que la PRESENCIA del aviso no
  discrimine y el único aserto posible sea el VALOR. MUTACIÓN (Set→List): cae SOLO A3, por el aserto de
  valor (7 vs 10), +3 = la contribución duplicada de la segunda plaza. Los tres tests HTTP siguen verdes
  bajo la mutación → D-F8.4-A-b. A4 INVERTIDO al subir (c) a ERROR, y su aserto más fuerte no es el 422
  sino `mocked.constructed()).isEmpty()`: el solver NO SE CONSTRUYE, que es el propósito entero del
  bloque y ningún status podía demostrarlo. Frontera `>` vs `>=` verificada por mutación (A1a) y con
  peso real: `PinTramoGeneracionRoundTripTest` (1 tramo, demanda 1) pasa PRECISAMENTE por ella.
  DOS TESTS QUE MENTÍAN, corregidos: `GenerarHorarioEndpointTest` seguía verde pero su fixture (2,1) ya
  lo capturaba la pre-validación antes del solve —sustituido por uno infactible SOLO por aula, que de
  paso documenta el hueco de (b)—; y el `noneMatch(ERROR)` de A3 se volvió FALSO (no tautológico) al
  subir (c), sustituido por `hasSize(1)`, que conserva la propiedad útil sin afirmar nada falso.
  A6 quedó SOBRE-DETERMINADO (su fixture dispara las tres reglas); el aislamiento limpio de (d) vive en
  A5. Declarado, no tapado.
  Suite 305 → 315 (+10), solver 78 intacto. solver/ NO tocado → referencia NO regenerada. modelo §4.3
  SÍ tocado (nota de implementación de D18, commit aparte).
  DEUDA NUEVA: D-F8.4-A-a (garantía de (c) derivada del no-solape por grupo), D-F8.4-A-b (la
  deduplicación la protege UN solo test), D-F8.4-A-c (severidad tautológica, `AVISO` sin productor).
  Siguiente: LIMPIEZA DE 8.5 (desplazada de S79, sesión en frío), 8.6 (Angular, contrato cerrado en
  S67), 8.4-B (presentación, MOCKUP PREVIO) u 8.5-D2b (solver), a decidir al abrir sesión.
Última sesión registrada (previa): Sesión 78 — Fase 8, Bloque 8.5-E: CRUD REST de ProfesorRestriccionHoraria (CIERRA 8.5).
  Modo híbrido. 2 commits de código (producción + tests, solo app/) + doc. §A DE MEDICIÓN sobre CÓDIGO
  (greps + lectura literal, el instrumento MÁS BARATO que respondía: la pregunta era sobre el repo, no
  sobre datos; precedente S77). SALIDA que REENCUADRÓ el bloque: la cadena entera JPA → CatalogoMapper
  → dominio → CP-SAT YA EXISTÍA y está consumida —entidad `ProfesorRestriccionHoraria` + repo +
  `schema.sql` (CHECK DURA|BLANDA) + record `RestriccionHoraria` + `ProblemaHorario.restriccionesHorarias`
  + `ModeloCpSat` (dura en `restriccionIndisponibilidadProfesor`, blanda en
  `objetivoIndisponibilidadBlandaProfesor`)—. Lo ÚNICO que faltaba era la SUPERFICIE DE ESCRITURA: sin
  CRUD, poblar restricciones exigía SQL a mano (el seed crea CERO). El bloque es MÁS PEQUEÑO que su
  casilla, y NO toca solver/ (lo que confirmó la elección de E sobre D2b al abrir).
  HALLAZGO DE LA MEDICIÓN, decisivo para la UX: `ModeloCpSat` NUNCA lee `r.peso()`; usa la constante
  `PESO_INDISP_BLANDA=1L` como coeficiente de un BoolVar puro. El javadoc del record afirmaba que `peso`
  era «la penalización en la función objetivo»: FALSO. Y es peor de lo que el javadoc admitía —decía
  «se ignora en DURA»; se ignora TAMBIÉN en BLANDA—. `peso` es columna `not null` en tres capas que
  ningún consumidor lee.
  MOCKUP PREVIO ejecutado (D-F8.6-a), como exige la casilla: rejilla 5 días × 6 tramos, tres estados,
  panel de detalle. NO se versiona (es una pregunta dibujada); sobreviven sus decisiones.
  DECISIONES CERRADAS: (E-0) `peso` NO entra en la superficie —ni en el DTO de entrada ni en la rejilla—;
  el service lo escribe SIEMPRE a 1. Descartadas «dibujarlo inerte» (la UI mentiría: mover un control
  que no cambia nada) y «hacerlo funcionar» (es D21(a), toca solver/ y activaría un campo SIN calibrar,
  peor que inerte). (E-1) click SELECCIONA; el estado se cambia desde el panel de detalle, MÁS acciones
  de FILA y COLUMNA (cabecera de tramo → los 5 días; cabecera de día → los 6 tramos). El arquitecto
  eligió primero la variante sin complemento y el arquitecto-mentor la DEVOLVIÓ con su coste medido
  (20 celdas = 40 clicks; la fila de 13:30 y el «día libre» son los patrones REALES del dominio, y son
  exactamente fila y columna): se acordó (c) fila/columna sobre (b) arrastre por resolver mejor el caso
  frecuente y costar menos. (E-2) volver a `puede` BORRA la fila y su motivo, SILENCIOSO (confirmar cada
  despintado en 30 celdas es insufrible). (E-3) sub-recurso GET/PUT
  /api/profesores/{id}/restricciones-horarias con REEMPLAZO TOTAL idempotente (patrón literal S75/S77:
  deleteAll+flush ANTES de insertar); validación sobre la lista ENTRANTE. (E-4) rejilla MONO-PROFESOR.
  (E-5) los tres estados son {ausencia de fila, BLANDA, DURA}: NO hay enum nuevo, `TipoRestriccion` ya
  existe y el CHECK de `schema.sql` los ancla. (E-6) el recreo NO es celda sino separador: `TramoSemanal`
  no lo contiene (los 6 tramos numeran 1..6 sin hueco, ver `ORDEN_TRAS_RECREO`).
  Tramo referenciado por (dia, ordenEnDia) INVIRTIENDO `CatalogoMapper.indiceOrdenEnDia` (precedente
  8.2b-iv/S66: fuente única, D30 no gana otro espejo). Tramo inexistente → 400.
  ERROR DE ESPECIFICACIÓN DEL ARQUITECTO (registrado, no tapado): E-3 fijaba que la restricción horaria
  era POBLACIÓN PROPIA del profesor (cascade, no 409). El código dice lo contrario desde `e60680a`
  (S74/8.5-C2b) y el código TIENE RAZÓN: el criterio «población propia» se aplica al lado que POSEE la
  población, no al lado REFERENCIADO, y la FK es RESTRICT en `schema.sql`. Verificado por `git log -S`,
  no por conjetura: el 409 preexistía, 8.5-E no lo tocó. MISMO GÉNERO QUE D-F8.5-D2a-b —especificar sin
  comprobar el precedente en el código—, dos sesiones seguidas. NO genera deuda: no hay incoherencia en
  el código, había un error en mi contrato. La asimetría de `profesor_tutoria` (cascade por el lado del
  Grupo, 409 por el del Profesor) es la de D2a-5, deliberada y vigente.
  ASERTOS: 15 tests nuevos. (A2) reemplazo≠merge por `doesNotContain` del par previo. (A4) duplicado en
  body → 400 CON LA BASE INTACTA, y el duplicado se elige DISTINTO de lo previo a propósito, o
  `containsExactly` no distinguiría «no se tocó» de «se borró y reescribió idéntico» —evita el
  acoplamiento accidental que S74/S76 cazaron tarde—. (A5) POR MUTACIÓN: neutralizada la pasada 3, cae
  por la VÍA ESPERADA (400→200, no 500), y un test desechable confirmó que quedan DOS filas y el flush
  pasa: NO existe UNIQUE(profesor,tramo), la guarda del service es lo ÚNICO que sostiene la regla —el
  javadoc decía la verdad, no hubo que corregirlo—. (A6) borrado de profesor: 409 en servicio y
  RESTRICT real en esquema, coherente con la cabecera de `schema.sql`.
  HALLAZGO DE FRAMEWORK (familia S73/S74/S75/S76/S77): la violación de FK NO llega como
  `DataIntegrityViolationException` sino como `GenericJDBCException`, por DOS causas medidas —
  `TestEntityManager.flush()` no cruza la frontera del repositorio donde Spring traduce a su jerarquía
  DAO, y el SQLiteDialect community 7.4.1 no clasifica el error de FK como violación de constraint—.
  flush()+clear() necesario (no decorativo) en A1/A2/A3/A4 y en los de lista vacía y salto de recreo.
  Efecto colateral asumido: el ctor de `ProfesorController` ganó un colaborador → `ProfesorEndpointTest`
  y `TutoriaEndpointTest` reciben el servicio real vía `@Import`, NO un `null` (un null haría que esos
  tests mintieran sobre el objeto que construyen).
  Suite 212 → 227 (+15), solver 78 intacto. Demostrada no-vacía (PESO_POR_DEFECTO=99 → cae A3 →
  restaurar → verde). solver/ intacto → referencia NO regenerada. modelo §4.3 SÍ tocado (nota de
  implementación de `peso` inerte, commit aparte).
  DEUDA NUEVA: D-F8.5-E-a (`peso` superficie muerta en tres capas), D-F8.5-E-b (unicidad
  (profesor,tramo) sin red bajo la aplicación), D-F8.5-E-c (el dialecto no clasifica FK como
  DataIntegrityViolation), D-F8.5-E-d (conteos inversos de Profesor en dos ubicaciones).
  8.5 QUEDA CERRADO: cero sub-bloques vivos (D2b es de solver/, D3 aplazado con criterio de reapertura).
  Siguiente: 8.4 (pre-validación, D18/D20; arrastra MOCKUP PREVIO por D20), 8.5-D2b (solver, regenera la
  referencia) u 8.6 (Angular, contrato ya cerrado en S67), a decidir al abrir sesión.
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
S68 en la Sesión 72, la de S69 en la Sesión 73, la de S70 en la Sesión 74, la de S71 en la Sesión 75, la de S72 en la Sesión 76, la de S73 en la Sesión 77, la de S74 en la Sesión 78 la de S75 en la Sesión 79 la de S76 en la Sesión 80 y la de S77 en la Sesión 81 (misma higiene documental; en S60 se corrigió además una copia
truncada y duplicada de S55 que la operación de archivado de S59 dejó en la bitácora; en S69 se corrigió
el censo de la bitácora, que S68 había dejado en S63 pese a contener ya S64). El plan conserva las 4
últimas cabeceras compactas (S78–S81). El detalle histórico de cualquier sesión anterior —incluida S42
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
- [x] Bloque 5 — Entidades JPA `Actividad` (agregado raíz, cascade+orphanRemoval a `Plaza`) y `Plaza` (`aula_fija` `@ManyToOne` + 3 `@ManyToMany`) + `CatalogoMapper.aActividad`/`aPlaza`; enum `app.catalog.PatronTemporal` propio. (S49) → `ActividadInstancia` NO se materializa como tabla (artefacto derivado, D-B5-1); el XOR aula_fija/candidatas lo valida el record de dominio (D-B5-2); `requiereTutor` persiste pero el solver no lo consume (D-B5-5, S8). Detalle: bitácora S49.
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
- [x] Bloque 8.4-A — Pre-validación por CONDICIONES NECESARIAS (S79). 8.4 partido en A (backend,
      esta sesión) y B (presentación, D20). Tres reglas, todas ERROR, en `PrevalidacionService`
      (núcleo ESTÁTICO `prevalidar(ProblemaHorario)`: inyectar el bean daría ciclo Spring con
      `GeneradorHorarioService`; el endpoint carga y `generar()` reutiliza el problema ya cargado,
      sin doble lectura ni decimotercera dependencia). (a) demanda de profesor > tramos lectivos −
      DURA; (c) demanda de grupo > tramos lectivos; (d1) `repeticiones > días` SOLO en DISTRIBUIDA;
      (d2) `repeticiones × duracion > tramos totales` en TODAS. (a) y (c) DEDUPLICAN POR ACTIVIDAD,
      no por plaza (las plazas de una actividad son simultáneas por S5): helper único, dos ejes.
      Superficie: `GET /api/prevalidacion` → 200 con la lista completa; `generar()` aborta con
      `PrevalidacionFallidaException` → 422, distinguible de `HorarioInfactibleException` por
      aserto de causa. (b) palomar de aulas FUERA del alcance por riesgo de falso positivo.
      Suite 305 → 315. Deuda: D-F8.4-A-a, D-F8.4-A-b, D-F8.4-A-c. Detalle: bitácora S79.
- [ ] Bloque 8.4-B — Presentación de los avisos (D20). Arrastra MOCKUP PREVIO (D-F8.6-a): la
      pregunta «¿bloquean o advierten?» resultó tener respuesta ESTRUCTURAL en 8.4-A —bloquea lo
      que es condición necesaria demostrable— así que 8.4-B decide cómo se PINTAN, no cuáles
      abortan. Incluye la validación amable del bloqueo contradictorio, diferida desde 8.2a
      (hoy da INFEASIBLE seco): NO la cubre 8.4-A.
- [x] Bloque 8.5-A/A'/B — CRUD de catálogo: raíces + grupos/subgrupos (S69/S70/S71) → sin deuda viva; Detalle: bitácora S69/S70/S71.
- [x] Bloque 8.5-C1 — CRUD de Actividad como AGREGADO, Plaza embebida sin /api/plazas (S72) → XOR aula, I7, I2 validadas en ActividadService; Detalle: bitácora S72.
- [x] Bloque 8.5-C2a-DDL — Integridad referencial de ESQUEMA: `schema.sql` + `ddl-auto=none` + 27 FK + `PRAGMA foreign_keys=ON` por conexión (S73) → D-F8.5-C2a-a (.db preexistente con PK NULL); Detalle: bitácora S73.
- [x] Bloque 8.5-C2b — Borrado amable: `ReferenciaEntranteException` + conteos inversos → 409 (S74) → CIERRA D-F8.5-A-a entera; cada controller traduce sus propias excepciones, sin @ControllerAdvice; Detalle: bitácora S74.
- [x] Bloque 8.5-C3 — I3 + CRUD de compatibilidades asignatura↔tipo de aula, semántica (C): 0 filas ⇒ irrestricta (S75) → validada en el funnel único `resolverContenido`; D-F8.5-C3-a (COMUN sin semántica), D-F8.5-C3-b (códigos por currículo); Detalle: bitácora S75.
- [x] Bloque 8.5-D1 — PDC como sub-recurso `/api/grupos/{idPadre}/pdc` + subgrupo mono-Di automático (S76) → un PDC por padre → 400; D-F8.5-D1-b (el 1:1 es decisión del arquitecto); Detalle: bitácora S76.
- [x] Bloque 8.5-D2a — `ProfesorTutoria` (@IdClass) + I4 en escritura + herencia PDC←padre por copia (S77) → CIERRA D-F8.5-D1-a; tutoría = población propia del Grupo (cascade) y referencia entrante del Profesor (409); D-F8.5-D2a-a (I4 sin red en BD), D-F8.5-D2a-b (incoherencia 404/400); Detalle: bitácora S77.
- [ ] Bloque 8.5-D2b — S8 verificable por el SOLVER. Bloque de solver/, no de catálogo:
      propagar requiereTutor al dominio (romper D-B5-5) + transportar ProfesorTutoria al
      ProblemaHorario + verificar S8. NOTA DE DISEÑO: S8 NO es restricción de scheduling
      (no depende del tramo elegido); va en VerificadorSolucion o en validación de catálogo,
      NO en ModeloCpSat. Regenera referencia-codigo-solver.md.
- [ ] Bloque 8.5-D3 — Particion/SubgrupoParticion + UX de subgrupo compartido (D7, I1, I6).
      APLAZADO INDEFINIDAMENTE (S77), decisión explícita, no arrastre. Son dos tablas que el
      SOLVER NO LEE; existirían solo para que I1 sea verificable y para la UX de D7. Su
      no-materialización fue deliberada (D-a del Bloque 4/S48) y nada desde entonces la ha
      contradicho. CRITERIO DE REAPERTURA: solo si (a) la UX de D7 pasa a requisito real de
      usuario, o (b) I1 falla en producción sin que nadie lo detecte. Consecuencia asumida:
      I1 sigue sin verificador, igual que hoy.
- [x] Bloque 8.5-E — CRUD REST de `ProfesorRestriccionHoraria`, sub-recurso GET/PUT con reemplazo total (S78). CIERRA 8.5 → `peso` NO se expone (ModeloCpSat usa la constante `PESO_INDISP_BLANDA` y nunca lee `r.peso()`); D-F8.5-E-a, D-F8.5-E-b, D-F8.5-E-c, D-F8.5-E-d; Detalle: bitácora S78.
- [x] Bloque 8.6-i — Cliente REST de bloqueos (S81): `bloqueo.model.ts` (espejo de los 4 records)
      + `BloqueoService` TS propio + `pines.ts` (`clavePin`, `indicePines`). `HorarioService`
      intacto (se declara de solo lectura). El `GET /api/bloqueos` es precondición del CANDADO, no
      del POST: el POST es idempotente POR INSTANCIA, no reemplaza colección. NO toca backend:
      `SesionVista` ya lleva (`actividadCodigo`, `indice`), la clave de cruce de D-6.
      Detalle: bitácora S81.
- [x] Bloque 8.6-ii — Arrastre que pina (S81): `@angular/cdk@21` `DragDropModule` (elegido porque
      usa pointer events y el runner es Vitest+jsdom, que no implementa HTML5 DnD). Envoltorio de
      celda por PAR (`actividadCodigo`, `indice`) vía `agruparPorActividad` NUEVA; `agruparPorSlot`
      intacta. `cdkDrag` en la INSTANCIA, nunca en la sub-entrada (contrato S67, D-F8.6-A-2).
      SIN movimiento optimista: el candado no se pinta hasta el OK del POST. Pin solo de tramo
      (`aulas: []`). D-F8.6-ii-a, D-F8.6-ii-b (sin gesto de despinar). Detalle: bitácora S81.
- [ ] Bloque 8.6-iii — Badge del delta blando por celda + resalte de violación a DOS granularidades
      (aula por sub-entrada, profesor/subgrupo por celda). MOCKUP PREVIO OBLIGATORIO (D-F8.6-a): S67
      dibujó las tres granularidades sobre una celda de desdoble, pero NO con el estado «pinada»
      encima; son tres capas que nadie ha dibujado juntas. REQUIERE CABLEAR 8.3-C: medido en S81
      que el cliente NO lo conoce (cero campos de atribución en `horario.model.ts`), luego no es
      pintar sino ampliar contrato de lectura. Cierra D19/D20 en frontend.
- [ ] Bloque 8.6-B — Aviso de conflicto durante el arrastre. Depende de 8.6. Es cruce de índices,
      NO verificación (ver arriba). Si en algún momento se propone portar el verificador a TS,
      PARAR: sería un cuarto espejo de la lógica de solapes, en otro lenguaje, sin el test que
      protege D15.

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
| UI | Angular |
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

### Deuda consciente VIVA

Deuda técnica y de dominio ABIERTA, aceptada conscientemente y pendiente de resolver en
fases futuras. Incluye las simplificaciones registradas en Fase 1 (D1-D12), cuya
descripción completa vive en `modelo_datos_fase1.md` sección 8, y la deuda surgida en
fases posteriores, descrita aquí. La deuda ya CERRADA se archiva condensada en la sección
siguiente, con remisión a la bitácora.

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
- **D26 (Sesión 47, Fase 6 Bloque 3): `Aula` (JPA) no tiene campo `nombre`.**
  Ni la entidad de catálogo (S46) ni §4.1 de `modelo_datos_fase1.md` definen un
  nombre completo de aula (a diferencia de `Profesor`/`Asignatura`, que sí tienen
  `nombre_completo`). El schema JSON del solver exige `Aula.nombre` no nulo, y
  `CatalogoMapper.aAula` lo resuelve con `nombre = codigo`. Válido para arrancar
  Fase 6 (el solver no distingue código de nombre en el objetivo ni en el
  verificador), pero es una simplificación: si la UI de Fase 8 necesita mostrar
  un nombre descriptivo de aula distinto del código ("Laboratorio de Ciencias"
  vs "A6"), hace falta añadir el campo a la entidad JPA y decidir si el mapper
  deja de derivarlo. Fase 8 (UI) o antes si algún criterio de Fase 6/7 lo exige.
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
  desde otra pestaña. → resolver en 8.6-iii.

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

### Deuda consciente CERRADA (histórico)

Deuda ya resuelta, condensada a una línea; el mecanismo vivo en `src/main` se conserva y
el detalle narrativo vive en la bitácora.

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
