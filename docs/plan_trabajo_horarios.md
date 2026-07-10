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

Fase actual: 8 — UI: configuración y ajuste manual (EN CURSO desde S57). Bloque 8.2b-ii CERRADO en
  S61 (persistencia JPA de los bloqueos §4.7: entidades SesionBloqueada + AulaBloqueada + repos +
  BloqueoMapper de entrada + cableado del placeholder de CatalogoMapper; NO toca src/main del solver;
  8.2b-iii REST + cableado del servicio siguen abiertos). Bloque 8.2b-i CERRADO en S60 (pin de aula
  por-plaza en el solver + rediseño §4.7/S5). Bloque 8.2a CERRADO en S58
  (pin de instancia a tramo en el solver: SesionBloqueada estructural + restricción dura +
  verificador + I/O de test; cierra el criterio 5 de Fase 3; pin de aula y persistencia/REST -> 8.2b).
  Bloque 8.1 CERRADO en S57 (vía REST de generación+persistencia, D29 cerrada parcialmente,
  SeedHorarioRunner partido en SeedCatalogoRunner). Fase 7 CERRADA en S56 (7A backend de lectura
  en S55 + 7B frontend Angular en S56). Fase 6 CERRADA en S54.
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
Última fase completada (previa): 5 — Solver: instituto completo (criterios 1-2
  cerrados en S36 por factibilidad pura; criterios 3-4 cerrados en S44 como decisión
  de producto gemela de D23, con respaldo descriptivo a escala)
Última sesión registrada: Sesión 61 — Fase 8, Bloque 8.2b-ii: PERSISTENCIA JPA de los bloqueos
  manuales (SesionBloqueada + AulaBloqueada, §4.7) + mapper de entrada + cableado del placeholder
  de CatalogoMapper. Modo híbrido (diseño y documentación en el Project, código en Claude Code).
  Cierra la deuda (ii) de 8.2b: materializa la forma normalizada de §4.7 que el rediseño de S60 dejó
  especificada. Alcance cortado con el usuario: 8.2b-ii SOLO; 8.2b-iii (entrada del bloqueo por REST,
  body de POST /api/horarios vs endpoint propio) sigue ABIERTO y aparte. NO se tocó src/main del
  solver ni el modelo (§4.7 ya correcto de S60). Decisiones cerradas antes de teclear D-F8.2b-ii-1..5:
  (1) el ensamblado JPA→dominio vive en clase nueva app.mapper.BloqueoMapper (final, ctor privado,
  estático), espejo de SolucionMapper; NO dentro de CatalogoMapper. (2) aProblemaHorario gana DOS
  parámetros (List<SesionBloqueada>, List<AulaBloqueada>), coherente con las siete listas de entidades
  JPA que ya recibe; el cruce por (actividad_codigo, indice) para agregar el Map<Plaza,Aula> vive en
  BloqueoMapper. (3) repos triviales JpaRepository vacíos, findAll() sin filtro (espejo de
  Sesion/HorarioGeneradoRepository). (4) bloqueo GLOBAL del centro (SIN FK a HorarioGenerado): §4.7 fija
  PK = instancia / (instancia, plaza), sin horario_id; el pin es ENTRADA del solver, precede al horario
  que genera. (5) validaciones de entrada replicadas en BloqueoMapper con IllegalArgumentException (NO
  se importa ProblemaInvalidoException de solver.io: frontera de capas, CatalogoMapper tampoco la
  importa): actividad/plaza/aula huérfana, aula ∉ aulasCandidatas, pin sobre aula fija, y pin de aula
  huérfano (aula sin su pin de tramo → abort, el record de dominio no soporta pin de aula suelto).
  Naming: entidades JPA en app.catalog con los nombres de §4.7; el record de dominio homónimo se
  referencia por FQN en BloqueoMapper (igual que CatalogoMapper distingue domain.Plaza de catalog.Plaza).
  Puente de tramo REUTILIZADO: BloqueoMapper recibe el Map<TramoSemanal,Tramo> (IdentityHashMap) que
  aProblemaHorario ya construye (tramosMapeados.porEntidad()), NO una cuarta copia de la renumeración;
  D30 sigue viva, no se agrava. Precondición anotada: la catalog.SesionBloqueada.getTramoInicio() debe
  ser la MISMA instancia TramoSemanal que la lista pasada a aProblemaHorario (se cumple en una
  transacción JPA única, caché de primer nivel). Entregado (6 commits de una línea, código/doc
  separados): entidades JPA 4a0c754, repos caa2d03, BloqueoMapper fe3d5a6, cableado de aProblemaHorario
  (firma +2 params, índices actividadesPorCodigo/plazasPorCodigo, llamada a BloqueoMapper) f61300b,
  actualización de los 7 llamadores de aProblemaHorario (servicio + 6 en CatalogoMapperProblemaTest,
  bloqueos vacíos) 785f6c2, IT de round-trip a285adf. IT BloqueoPinRoundTripTest sobre un DESDOBLE
  (multi-plaza): persistir catálogo + pin de tramo + pin de aula sobre una plaza variable, montar
  ProblemaHorario vía CatalogoMapper, resolver con new SolverHorario(10,42) (patrón D-B10-7), guardar →
  recargar; assert (a) las N plazas de la instancia caen en el tramo pinado (simultaneidad gratis por
  instancia), assert (b) la plaza variable respeta el aula pinada. ORO: desactivar submapa.put(plaza,
  aula) del Paso 1 de BloqueoMapper tira SOLO el assert (b) (expected "A2" but was "A1": sin el pin el
  solver colocaba en A1, el pin fuerza A2 — el pin CAMBIA la solución, no es un no-op), assert (a) verde;
  restaurado, verde. Suite: solver 71 intacto (no se tocó solver/src/main → referencia-codigo-solver.md
  NO regenerado), app 46 → 47 (+1 IT). BUILD SUCCESS con mvn clean test desde la raíz; working tree
  limpio. Deuda VIVA que 8.2b-ii deja: (a) cableado del servicio a los repos de bloqueo —
  GeneradorHorarioService.cargarProblema() pasa List.of(), List.of(): la vía de generación (POST
  /api/horarios) NO lee aún los bloqueos vigentes de la BD, persistir un bloqueo hoy no afecta a un
  solve por REST; complemento natural de 8.2b-iii o mini-bloque previo—; (b) 8.2b-iii sigue abierto
  (entrada del bloqueo por REST, decisión de contrato sin cerrar); (c) deuda de test menor: el caso de
  pin de aula huérfano (abort) no tiene test negativo propio, el IT solo ejercita la ruta feliz.
  Siguiente: 8.2b-iii (REST del bloqueo + cableado del servicio) o el candidato que se decida al abrir
  sesión.
Última sesión registrada (previa): Sesión 60 — Fase 8, Bloque 8.2b-i: PIN DE AULA por-plaza en el solver
  + rediseño §4.7/S5 del modelo. Modo híbrido (diseño y documentación en el Project, código en
  Claude Code). Cierra la deuda (i) de 8.2b (pin de aula por-plaza); persistencia y REST del bloqueo
  siguen diferidos a 8.2b-ii/iii. Decisiones cerradas antes de teclear D-F8.2b-1..4: (1) forma 1A —
  SesionBloqueada de dominio gana 3er componente Map<Plaza,Aula> aulasPinadas (vacío = solo pin de
  tramo, retrocompat 8.2a); ProblemaHorario NO cambia de firma. (2) 2A — pinar aula de plaza aulaFija
  es entrada inválida (ProblemaInvalidoException en io, IllegalArgumentException de salvaguarda en
  cpsat, separación de capas de 8.2a respetada). (3) restriccionAulaBloqueada() en construir() tras
  restriccionSesionBloqueada(): addEquality(opcion.presencia(), 1) sobre la AulaOpcion casada por
  aula.equals; verificador gemelo contarAulasBloqueadasVioladas sin OR-Tools. (4) 4C validada en el
  mapper (aula pinada ∈ aulasCandidatas); 4B (pin×poda) REBAJADA a deuda documental D-F8.2b-4B,
  condicionada al grep de Claude Code —que salió VACÍO (sin llamadores de construirConObjetivo(true)):
  la poda está muerta en todo camino vivo, 4B defendería un caso imposible—. Rediseño de MODELO:
  §4.7 corrige el error de cardinalidad (aula NO cuelga de la instancia: una instancia de desdoble
  tiene N plazas con N aulas); split en SesionBloqueada (pin de tramo, por instancia) + AulaBloqueada
  (pin de aula, por plaza, PK (instancia, plaza)) como forma normalizada para la persistencia de
  8.2b-ii; el dominio la agrega en el Map. S5 reformulada (pin de tramo sobre todas las Sesion de la
  instancia; pin de aula por plaza; solo aula variable; aula ∈ candidatas). Réplica del rediseño
  §4.7/S5 validada contra el código tecleado (el Map<Plaza,Aula> del dominio == AulaBloqueada
  normalizada). ORO (criterio 5): comentar restriccionAulaBloqueada() tira EXACTAMENTE los tests de
  respeto y desdoble, deja verde el sin-pin; restaurada, ModeloCpSat idéntico salvo la adición
  (46 insertions, 0 deletions). Entregado (6 commits de una línea, código y doc separados): dominio
  da33330, cpsat restricción 25ae7fb, verificador c9e76e7, io+schema (AulaPinDto(plaza,aula) +
  SesionBloqueadaDto gana List<AulaPinDto>) f30347f, tests+fixtures (3 cpsat respeto/desdoble/sin-pin
  + 3 io positiva/2A/4C) 00291af, índice regenerado 271a1a4. Suite: solver 65→71 (+6); app 46 sin
  cambio (NO se tocó app/, ni persistencia, ni REST, ni el List.of() de CatalogoMapper, que sigue
  placeholder hasta 8.2b-ii). BUILD SUCCESS con mvn clean test desde la raíz. src/main del solver SÍ
  tocado → referencia-codigo-solver.md regenerado. Deuda de test menor anotada: la salvaguarda
  IllegalArgumentException de cpsat (pin de plaza fija en un ProblemaHorario montado a mano, sin pasar
  por el mapper) no tiene test propio; el mapper cubre la ruta de usuario. Deuda VIVA para 8.2b-ii:
  persistencia JPA de SesionBloqueada + AulaBloqueada (§4.7) y cableado del List.of() de
  CatalogoMapper:135; para 8.2b-iii: entrada del bloqueo por REST (body de POST /api/horarios vs
  endpoint propio, decisión abierta). Siguiente: 8.2b-ii (persistencia de bloqueos) o el candidato
  que se decida al abrir sesión.
Última sesión registrada (previa): Sesión 59 — Fase 8, deuda D-F8.1-8 CERRADA: test de contrato de
  serialización de los DTOs de proyección (blinda contra la divergencia silenciosa que en 7B
  dejó colar el profesor TEC4). Modo híbrido (decisión y contrato en el Project, código en Claude
  Code). Enfoque cerrado = Opción B sola (test JVM que serializa y afirma la FORMA del JSON),
  descartadas A (snapshot capturado a mano, repite la debilidad del TEC4) y C (acoplar builds
  Maven/Node, arrastra el toolchain de 8.1). Nuevo ProyeccionDtoContratoTest (app.web.dto) con el
  ObjectMapper REAL de la ruta MVC (new MappingJackson2HttpMessageConverter().getObjectMapper(),
  el mismo de HorarioControllerHttpTest; SB 4.1 no trae @JsonTest): 3 métodos —(A) SesionVistaDTO
  exactamente 12 claves + tipo JSON de cada una; (B) HorarioProyeccionDTO exactamente 8 claves +
  tipos con objetivo/cota no nulos; (C) objetivo=null/cota=null siguen presentes con isNull()==true,
  confirma que NO hay @JsonInclude(NON_NULL) y honra el contrato con horario.model.ts (number|null
  siempre presente)—. Claves por containsExactlyInAnyOrder derivadas de fieldNames(): añadir un
  campo rompe igual que quitarlo. Lista de claves EXPLÍCITA (no reflexión del record) → un renombrado
  mueve un solo lado y el test salta (verificado en el oro negativo). CENTINELA en cada método: al
  cambiar el DTO, actualizar TAMBIÉN app/frontend/src/app/models/horario.model.ts (interfaz espejo,
  no atada automáticamente). ORO NEGATIVO (hecho y revertido, no commiteado): renombrar
  asignaturaNombre→asignaturaNom deja el test ROJO por containsExactlyInAnyOrder; revertido, verde.
  horario.model.ts NO se toca (hoy es espejo fiel del DTO, sin corrección pendiente). Deuda residual
  anotada: B no detecta que la interfaz TS quede rezagada tras un cambio propagado correctamente en
  Java; el centinela lo mitiga, no lo elimina. Suite: solver 59 + app 46 (43 + 3), BUILD SUCCESS con
  mvn clean test desde la raíz; mvn -pl app test en solitario da NoSuchMethodError por jar rancio de
  solver en .m2 (artefacto conocido de la frontera modular, no regresión). src/main del solver NO
  tocado (referencia-codigo-solver.md sin regen); modelo NO tocado. Commit 9d0fb5f. Siguiente: 8.2b
  (persistencia+REST del bloqueo + pin de aula), con la tensión §4.7 a cerrar antes de teclear (el
  modelo especifica SesionBloqueada con aula_id por-instancia, pero 8.2a fijó que el pin de aula es
  por-plaza; hay que rediseñar §4.7 o llevar el aula a otra entidad antes de persistir); o el
  candidato que decidas al abrir sesión.
Última sesión registrada (previa): Sesión 58 — Fase 8, Bloque 8.2a: PIN DE INSTANCIA A TRAMO
  (bloqueo manual) en el solver. Modo híbrido (decisión y contrato en el Project, código en
  Claude Code). Cierra el criterio 5 de Fase 3 (diferido desde S17: "bloquear un tramo y el
  solver lo respeta"). Trabajo de dominio + cpsat + io de test; NO toca persistencia/REST (8.2b).
  Alcance cortado con el usuario: 8.2 partido en 8.2a (solver+modelo, esta sesión) y 8.2b
  (persistencia+REST+pin de aula). Decisiones D-F8.2-1..6 (ver bloque de decisiones de 8.2a arriba);
  la 6 REVERTIDA en diseño (pin de aula fuera de 8.2a: es por-plaza (plaza, aula), no por-instancia).
  El pin es el DUAL de restriccionIndisponibilidadProfesor (addEquality sobre el tramoIndex en vez
  de dominio complementario); el desdoble se pina simultáneo gratis por el tramoIndex compartido por
  las plazas. La Fase 0 de lectura del repo (parada incondicional antes de teclear) descubrió tres
  cosas que el índice de solver/ no veía: la instancia se localiza recorriendo
  List<InstanciaProgramada> por equals de record; un 4º new ProblemaHorario( en app/CatalogoMapper
  (resuelto con List.of() placeholder, opción A, cableado real en 8.2b); y que ProblemaInvalidoException
  vive en io -> no se puede lanzar desde cpsat sin romper capas (resuelto: IllegalArgumentException
  en ModeloCpSat como salvaguarda, ProblemaInvalidoException en el mapper como validación de entrada).
  Entregado (7 commits de una línea): record SesionBloqueada + 9º componente de ProblemaHorario;
  restriccionSesionBloqueada() en construir(); contarBloqueosViolados en el verificador;
  SesionBloqueadaDto + mapper + schema; 4 fixtures + tests de solver (respeto, desdoble, infactible,
  gemelo sin-pin) + 2 de loader. ORO en positivo: comentar la restricción hace caer 3 de 4 tests del
  pin; reactivada, ModeloCpSat idéntico a HEAD. Suite: solver 59->65 (+6), app 43 (sin cambio),
  BUILD SUCCESS con mvn clean test desde la raíz, árbol limpio. src/main del solver SÍ tocado ->
  referencia-codigo-solver.md REGENERADO (commit 350258b); modelo NO tocado (§4.7 ya correcto de S53).
  Commits 6ef0c14/7dd9048/1987925/5a144d3/0150e64/350258b. Deuda VIVA que 8.2a deja para 8.2b: pin de
  AULA (contrato por-plaza + restricción + verificación), persistencia de SesionBloqueada (entidad JPA
  §4.7 + schema) y entrada del bloqueo por REST, y el List.of() placeholder de app/CatalogoMapper.
  Siguiente: 8.2b (persistencia+REST del bloqueo + pin de aula). Antes de teclear, cerrar la tensión
  §4.7: el modelo especifica SesionBloqueada con aula por-instancia, pero 8.2a fijó el pin de aula
  por-plaza; §4.7 debe rediseñarse o el aula moverse a otra entidad. (D-F8.1-8 cerrada en S59.)

Las cabeceras compactas de S37–S43 y el registro detallado de S10–S42 se
archivaron en `docs/bitacora-sesiones.md` en sesiones anteriores; las cabeceras
de S44, S45 y S46 se archivaron en la Sesión 50, la de S47 en la Sesión 51, la de S48
en la Sesión 52, la de S49 en la Sesión 53, la de S50 en la Sesión 54, las de S51, S52,
S53 y S54 en la Sesión 58, la de S55 en la Sesión 59, la de S56 en la Sesión 60, y la de S57
en la Sesión 61 (misma higiene documental; en S60 se corrigió además una copia truncada y
duplicada de S55 que la operación de archivado de S59 dejó en la bitácora). El plan conserva
las 4 últimas cabeceras compactas (S58–S61). El detalle histórico de cualquier sesión
anterior —incluida S42 (citada por la deuda abierta D25) y S43
(citada por el cierre de D23)— está en la bitácora.

<!-- Registro detallado de S32–S42 archivado en docs/bitacora-sesiones.md (S44). -->

### Bloques de Fase 2
- [x] Bloque 1 — Setup del repositorio
- [x] Bloque 2 — POJOs del dominio
- [x] Bloque 3 — Schema JSON + DTOs + mapper
- [x] Bloque 4 — Construcción del modelo CP-SAT
- [x] Bloque 5 — Output a consola
- [x] Bloque 6 — Dataset real 1ºESO ordinarias + verificación PDFs

### Bloques de Fase 5
- [x] Bloque 1 — Religión multi-grupo por parejas (Tipo 4), validado con 1ºESO
      real (S19). Fixture problema-5-religion-parejas-1eso.json +
      SolverHorarioReligionParejasTest. Datos 6/6 verificados contra volcado fiel.
- [x] Bloque 2 — Escala 1ºESO + 2ºESO (7 grupos: 4 de 1º + 3 de 2º), medición de
      tiempo de solver (S20). Fixture problema-5-escala-instituto.json (linaje
      NUEVO de fixture de escala, separado de los de discriminación; crece por
      niveles hasta el instituto) + SolverHorarioEscalaInstitutoTest. 1º y 2º
      verificados plaza a plaza contra volcado fiel. Medición: solución factible
      en 0,317 s, 0 violaciones duras (suite 30 verde). Códigos de grupo
      normalizados sin "º" (1A..2C).
- [x] Bloque 3 — Escala 3ºESO ordinario (3A–3C); 10 grupos totales (S21). Fixture
      problema-5-escala-instituto.json AMPLIADO (mismo linaje, crece in situ) +
      SolverHorarioEscalaInstitutoTest mutado a 10 grupos. 3º verificado plaza a
      plaza contra volcado fiel (125/125). Escala pura: ningún cambio de dominio;
      reutiliza el mecanismo multi-plaza de §6.4 (dos coordinadas de nivel rep=1,
      K=6: CyR desdoblado + refuerzo×3 + BioNu) y el Hallazgo F (Religión partida
      AB/C, no transversal). Sesiones compartidas ord+Di modeladas con subgrupo
      del ordinario solo (Di excluido; verificado que 3º ordinario es separable).
      Medición: factible en 0,469 s, 0 duras (suite 30 verde). Segundo punto de
      la curva de escala. Geo→Geogr normalizado en 3º (ruido de extracción del
      volcado de 3ºB). Particiones de refuerzo/ATED: plausibles, a confirmar con
      el centro (deuda, en el Javadoc del test). Precisión (S22): el dominio
      modela SUBGRUPOS, no ALUMNOS — no existe entidad Alumno. Que cada subgrupo
      se corresponda con una partición real (disjunta y exhaustiva) de los
      alumnos de su grupo NO lo verifica ningún componente del sistema: es
      invariante de población, responsabilidad del constructor del fixture y a
      confirmar con el centro. Confirmado leyendo VerificadorSolucion.java en
      S21: el no-solape de GRUPO se cuenta con un Set POR INSTANCIA de actividad,
      de modo que varios subgrupos del mismo grupo dentro de una misma actividad
      coordinada/desdoble colapsan a "grupo contado 1 vez" (por eso las plazas de
      las coordinadas de 3º coexisten sin violación). SÍ están verificados, en
      cambio, la disjunción estructural intra-actividad (I2) y el no-solape de
      grupo entre actividades distintas.

- [x] Bloque 4 — Lectura B (SubgrupoGrupo N:M): subgrupo cuya población son
      alumnos de varios grupos (S22). TRABAJO ESTRUCTURAL, no escala: el dominio
      pasó de Subgrupo→grupo 1:1 a Subgrupo→grupos N:M (Set<GrupoAdministrativo>).
      Desbloquea el Tipo 7 (optativas multi-grupo de Bachillerato), última
      tipología del enunciado sin soporte de dominio. Tocó: Subgrupo (record +
      equals/hashCode por código), SubgrupoDto, schema (grupo→grupos array),
      mapper, ModeloCpSat.tocaGrupo (sg.grupos().contains), VerificadorSolucion
      (gs.addAll(s.grupos())), VistaPorGrupo (flatMap). Migrados 12 fixtures +
      JSON inline de ProblemaHorarioJsonLoaderTest + VerificadorSolucionGrupoTest.
      Fixture de discriminación PROPIO (no toca el de escala): bloque de optativas
      1ºBach C+D (TICO/DTec/DA), recorte fiel de §6.3, verificado cruzando
      grupo-1BACH-C/D.json. Prueba positiva (problema-5-lecturab-optativas-bach.json:
      factible, 0 duras, el bloque toca C y D) + prueba negativa
      (problema-5-lecturab-optativas-bach-infactible.json: el subgrupo multi-grupo
      bloquea ambos grupos → infactible por palomar). Suite 32 verde (30+2).
      CORRECCIÓN de S20: "1B-C 1B-D" del volcado de aulas NO es Lectura B; es
      notación "1ºBach C + 1ºBach D" (dos grupos enteros) y corresponde a LU =
      Lectura A. La evidencia real de Lectura B es el bloque de optativas, no esa
      celda. Modelo: §6.3/§6.4/§6.5 y la invariante S9 actualizadas (Lectura B
      soportada y validada; Lectura A es el caso particular de conjunto unitario).
- [x] Bloque 5 — PDC de 3º a escala (S23). ESCALA con capa de modelado nueva
      (grupo de Diversificación). El PDC de 3º se modela como UN grupo
      administrativo propio 3PDC (tipo DIVERSIFICACION_PDC, padre 3C por I5), NO
      como subgrupo Lectura B sobre {3A,3B,3C}: el Di saca parte de cada grupo y
      el resto sigue en clase ordinaria simultánea, así que 3PDC solo se bloquea
      a sí mismo (subgrupo 3PDC→{3PDC} mono-grupo). Distinción estructural frente
      a Bach: en Bach el subgrupo lista varios grupos porque los bloquea enteros
      (redistribución total, Lectura B); en PDC no. Tronco A8 de 22 h (ÁmbCM 8 /
      ÁmbSL 7 / OyD 2 / RefMt 2 / IngDi 2 / TPMAR 1), 6 actividades mono-plaza
      DISTRIBUIDA aulaFija A8, verificado sin solapamiento interno contra volcado
      fiel (tronco idéntico en los tres PDC A/B/C). Las 8 compartidas ord+Di se
      quedan en el ordinario (opción 2: evita doble conteo de población
      3PDC ⊂ 3A∪3B∪3C, que activaría D3). Fixture de escala AMPLIADO (mismo
      linaje) + SolverHorarioEscalaInstitutoTest mutado (grupos 11, subgrupos 116,
      actividades 116). Tercer punto de curva: 10 grupos + 1 grupo Di → 0,408 s,
      0 duras, suite 32 verde. NO dispara salto de régimen (A8 exclusiva, profes
      ≤14/30). Coste de proceso: 3 intentos (subgrupo {3A,3B,3C}→sobrecarga 52 h;
      grupoPadre null→viola I5; grupoPadre 3C→OK), por razonar sobre código no
      leído antes de pedirlo. Cierra dos deudas: "PDC a escala (3ºADi/3ºBDi/3ºCDi)"
      (resuelta distinto a lo previsto: un grupo, no tres) y "reconciliación
      3ºPDC↔3ºCDi" (cerrada por identidad: el PDC sin letra del volcado es el Di
      adscrito a 3C, determinable por su envoltura ordinaria).
- [x] Bloque 6a — Función objetivo: ventanas del profesorado (S24). Cambio de
      régimen: el solver pasa de factibilidad pura a optimización por una vía
      separada (`construirConObjetivo`/`resolverOptimizando`), dejando intacto
      el camino de factibilidad (`construir`/`resolver`) para no invalidar la
      curva de escala. Forma A (cotas tensadas) + andamiaje genérico de términos
      ponderados con un único término. `contarVentanasProfesor` en el verificador
      (recomputo independiente). Fixture problema-6-ventanas-profesor.json +
      SolverHorarioVentanasProfesorTest (2 casos). Suite 34 verde. NO cierra
      criterios de Fase 5 (el criterio 4 tiene mecanismo, falta umbral + escala).
      Deuda D17. Comprobación de oro fuerte diferida a 6b (un hueco inevitable
      exige indisponibilidades). Índice de API regenerado (src/main cambió).
- [x] Bloque 6b — Indisponibilidades horarias DURA + oro fuerte de ventanas (S25).
      El solver consume `ProfesorRestriccionHoraria` en su variante DURA
      (`restriccionIndisponibilidadProfesor` en `construir()`, aplica en ambos
      regímenes). BLANDA se carga y valida pero no se consume (difiere a 6c).
      Cierra la comprobación de oro fuerte de ventanas comprometida en S24:
      óptimo determinista > 0 (hueco inevitable forzado por veto), el optimizador
      lo minimiza rechazando alternativas más caras. I/O ensanchado una vez
      (records RestriccionHoraria + TipoRestriccion + DTO; campo en ProblemaHorario;
      array top-level `restriccionesHorarias` en el schema). 4 commits, suite 42
      verde. Índice regenerado, modelo §4.3 actualizado + D18. NO cierra criterios
      de Fase 5 (criterio 4 PARCIAL: falta umbral + escala).
- [x] Bloque 6c — Indisponibilidad BLANDA del profesorado como término del objetivo
      (S26). El solver consume la variante BLANDA de `ProfesorRestriccionHoraria`
      (`objetivoIndisponibilidadBlandaProfesor` en `construirConObjetivo()`):
      penaliza, vía un literal por (restricción blanda × instancia que usa al
      profesor), que la instancia caiga en el tramo vetado-blando. Segundo término
      del objetivo (el primero fue ventanas, 6a). NO toca I/O (el dato entra desde
      6b). Troceado en dos turnos: A — el término muerde (discriminación, óptimo 0
      evitable) + recomputo gemelo `contarPenalizacionIndisponibilidadBlanda` en el
      verificador; B — oro fuerte (incumplir inevitable, óptimo determinista 1 > 0,
      el optimizador rechaza la alternativa de coste 2). Ambos fixtures de linaje
      DISCRIMINACIÓN, validados por enumeración exhaustiva en diseño, con ventanas
      idénticamente 0 (término aislado). D17 RESUELTA para este término: las cotas
      tensadas siguen correctas porque la blanda penaliza el `tramoIndex` y no toca
      span/primero/ultimo/huecos (separable; analizado en el Javadoc de
      `objetivoVentanasProfesor`). Peso `PESO_INDISP_BLANDA=1` hardcodeado (gemelo
      de `PESO_VENTANAS`): decisión consciente de no calibrar sin datos del centro;
      parametrización diferida (deuda nueva de pesos blandos). Suite 44 verde,
      BUILD SUCCESS. Criterio 3 de Fase 5 AVANZADO (segundo término blando), no
      cerrado (faltan distribución-a-blanda / primeras-últimas horas + escala).
- [x] Bloque 6d-c — Sesiones consecutivas máximas del profesorado como término del
      objetivo (S27). El solver penaliza el exceso sobre `MAX_CONSECUTIVAS=3`
      sesiones seguidas de un profesor en un día
      (`objetivoConsecutivasProfesor` en `construirConObjetivo()`): por cada (prof,
      día) y cada ventana deslizante de N+1 posiciones contiguas en `ordenEnDia`, un
      literal `excede` reificado como "las N+1 ocupadas"; la suma penaliza
      `Σ max(0, L−N)` por racha maximal de longitud L. Tercer término del objetivo
      (ventanas 6a, indisponibilidad blanda 6c). NO toca I/O. Decidido al inicio con
      el usuario entre 6d (tres términos blandos) y escala (4º/Bach/FPB): 6d-c es la
      única capa LIMPIA que queda del criterio 3 (6d-a distribución-a-blanda toca
      estructura dura en dos sitios; 6d-b primeras/últimas horas REACTIVA D17). Se
      eligió cerrar la capa limpia antes de abrir escala (donde se espera el salto de
      régimen D4), para que el criterio 3 no quede a medias si escala da problemas.
      Troceado en dos turnos: A — el término muerde (discriminación, óptimo 0
      evitable) + recomputo gemelo `contarPenalizacionConsecutivasProfesor` en el
      verificador (cuenta rachas maximales; equivalencia con las ventanas deslizantes
      del modelo validada por enumeración exhaustiva de los 256 subconjuntos de un
      día de 8 tramos); B — oro fuerte (encadenar inevitable, óptimo determinista 1,
      rechaza alternativa de coste 2). Fixtures: problema-6d-consecutivas-
      discriminacion.json (4 actividades NEUTRA de P1, día de 4 tramos + día de 1 →
      óptimo 0 repartiendo ≤3 al día1, alternativa de coste 1 con las 4 juntas; test
      asevera coste 0 Y día1≤3) y problema-6d-consecutivas-oro-fuerte.json (7
      actividades de P1, dos días de 4 tramos saturados → algún día fuerza 4 seguidas
      ⇒ óptimo 1 inevitable; partir la racha cuesta ventana ⇒ coste 2, rechazado;
      test asevera el COSTE =1, no la posición). Ventanas NO contaminan: enumeración
      confirma que ningún óptimo de coste total ≤1 tiene consecutivas 0 (la vía-
      ventanas a coste 1 no existe). D17 NO reactivada: el término no mira
      primero/ultimo/span (separable como la blanda de 6c); no se migró a
      addMinEquality/addMaxEquality. Sin APIs CP-SAT nuevas (newBoolVar, addBoolOr,
      addBoolAnd, addImplication, addLinearExpressionInDomain, Domain.fromValues,
      complemento, LinearExpr.term, todas desde 6a/6c); compiló a la primera.
      No-regresión de ventanas (6a) e indisponibilidad blanda (6c) confirmada por
      ejecución: los tres términos conviven sin interferencia. Peso
      `PESO_CONSECUTIVAS=1` y N=3 hardcodeados (deuda D21 ampliada). Suite 46 verde,
      BUILD SUCCESS. Índice regenerado. NO cierra criterios de Fase 5 (criterio 3
      AVANZADO con tres términos, faltan distribución-a-blanda + primeras/últimas
      horas + escala).
- [x] Bloque 7 — Escala 4ºESO ordinario (4A–4D), sin Di (S28). Fixture PROPIO
      problema-5-escala-4ESO.json (linaje de escala AISLADO, separado del de
      escala-instituto: 4º introduce un salto de régimen propio —D4— que se
      observa sin el ruido de 1º/2º/3º; la fusión al instituto es paso posterior)
      + SolverHorarioEscala4EsoTest. 4 grupos, 30 tramos, 96 subgrupos, 31
      actividades (7 bloques transversales NEUTRA + 24 ordinarias DISTRIBUIDA),
      construidos desde los volcados fieles grupo-4-ESO-{A,B,C,D}.json y
      verificados por cuadre 30/30 por grupo. Ejercita los TRES patrones de
      agrupamiento del Hallazgo K simultáneos en un nivel: transversal sobre 4
      grupos (DT+Ref+CeH+AFAVS en M5/J3; Rel+ATEDU en V4), sobre 3 {A,B,D}
      (FQ+DIG; Biol+TEC+FOPP), sobre 2 {A,B} (mates partidas MatAp/MatAc); C y D
      con itinerario propio (C letras LAT/ECO; D mixto) como ordinarias.
      Optativas de nivel (TEC/DIG/FOPP) como plazas compartidas Tipo 7 (profesor
      y aula únicos, no clonados; verificado celda a celda); EXPRE desdoblada por
      taller (DIB1/TALL1 A,B; DIB2/C01 C,D) dentro de la misma actividad. D4 a
      saturación (AFAVS Gim+Pista para los 4 grupos en M5/J3) y cuello INF1/A12In
      (DIG 6h en aula única) NO rompen: factible en 0,126 s, 0 duras (suite 47
      verde). Tercer-cuarto punto del seguimiento de escala, pero linaje SEPARADO
      del de escala-instituto (no entra en su curva). Escala pura: ningún cambio
      de dominio ni de src/main. NO cierra criterios de Fase 5 (4º AISLADO no
      prueba D4 al fusionar niveles; criterios 1-2 exigen el instituto completo).
      Deuda nueva: DIG/TEC/FOPP de 4º suman 6h entre dos bloques de perfil
      distinto, lo que no encaja con la optativa única de 3h de la prematrícula;
      modeladas como población propia por bloque (sin reuso de subgrupo, sin
      acople S3), a confirmar con el centro. DT/CeH/AFAVS sí reusan subgrupo entre
      M5 y J3 (misma partición demostrada). TUT4 como actividad ordinaria (el DTO
      no transporta tutor obligatorio; S8 no ejercitada).
- [x] Bloque 8 — Escala 4ºESO COMPLETO: ordinario (4A–4D) + 2 PDC (4APDC, 4DPDC)
      en un único fixture (S29). Cierra 4º como linaje. Fixture problema-5-escala-
      4ESO-Di.json (6 grupos, 30 tramos, 116 subgrupos, 39 actividades) +
      SolverHorarioEscala4EsoDiTest. Mismo linaje AISLADO de 4º (no entra en la
      curva de escala-instituto). DOS grupos Di (resuelto el cabo abierto: son dos,
      no uno) NO separables del ordinario: comparten EF, tutoría y el agrupamiento
      del V4. Modelado validado celda a celda: los 26 ámbitos son IDÉNTICOS en
      ambos PDC → ámbito = UNA actividad con subgrupo compartido {4APDC,4DPDC}
      (mono-Di, sin los padres: regla S23). EF/tutoría específicas de cada Di con
      su grupo de origen, plaza única conjunta {4X,4XPDC}. EXPRE del Di con
      aulasCandidatas [C01,TALL1]. FACTIBLE en 0,232 s, 0 duras (suite 48 verde).
      Tipo 5 validado a escala con dos grupos Di. NO cierra criterios de Fase 5:
      4º completo no agrava D4 (el PDC no añade presión sobre Gim/Pista; D4 sigue
      esperando a la fusión de niveles). Escala pura: ningún cambio de dominio ni
      de src/main. Dos INFEASIBLE durante la construcción (código de subgrupo
      duplicado por EXPRE multi-aula; duplicación de B04 por tratar los ámbitos
      como linajes separados) cazados y corregidos antes del cierre — aprendizaje:
      cuadre 30/30 ≠ factibilidad; validar también unicidad y carga por recurso.
- [x] Bloque 9 — FUSIÓN DE NIVELES 3º+4º ESO (S30). Primer fixture que reúne dos
      linajes antes independientes: 3º (3A/3B/3C + 3PDC, extraído de escala-instituto)
      + 4º completo (de escala-4ESO-Di). Fixture problema-5-fusion-3-4-eso.json (10
      grupos, 30 tramos, 155 subgrupos, 79 actividades) + SolverHorarioFusion34EsoTest.
      OBJETIVO: ejercitar D4 (Gim/Pista compartido entre cursos) por primera vez —
      hasta S29 cada nivel tuvo Gim/Pista para sí. Par 3º+4º por atribución limpia (4º
      satura Gim+Pista en J3/M5; 3º trae EF clavada a Pista por aulaFija). Unificación
      de catálogos cruzada POR CÓDIGO: 16 profesores compartidos (código=persona,
      carga ≤17h), 7 aulas compartidas (misma def. física), 0 colisiones de código,
      tramos idénticos. Modelado Opción B (conservador, EF de 3º con aulaFija sin
      relajar) para atribución limpia de un eventual INFEASIBLE. Validado con batería
      completa (cuadre por grupo respeta la divergencia de estilos de PDC: 3PDC=22 por
      opción 2 de S23, los Di de 4º=30 por plaza conjunta {4X,4XPDC} — la fusión NO
      reconcilia los dos estilos). FACTIBLE en 0,300 s, 0 duras (suite 49 verde).
      RESULTADO: D4 NO muerde en el par; cabe con holgura (Gim 8h, Pista 10h sobre 30).
      Escala+estructura acoplados conscientemente (fusión no es capa limpia). NO cierra
      criterios de Fase 5: el par no prueba D4 a escala de instituto (la demanda de
      Pista crece con el nº de grupos). D4 baja de severidad, NO se cierra. src/main no
      tocado.
- [x] Bloque 10 — Fusión ESO completa: 1º+2º+3º+4º en un único fixture (S31).
      Reúne los dos linajes de fusión: escala-instituto (1º-3º+3PDC) + escala-4ESO-Di
      (4º completo). 17 grupos, 30 tramos, 232 subgrupos (116+116), 155 actividades
      (116+39), 217 plazas. OBJETIVO: someter D4 a competencia REAL (no la holgura del
      par de S30). Logrado: demanda de Gim 26h sobre 30 (vs 8h en el par), con los dos
      NEUTRA de 4º (AFAVS Gim+Pista en J3/M5) compitiendo contra EF-3A/EF-3C clavadas a
      Pista. Unificación cruzada POR CÓDIGO: 22 profesores compartidos (vs 16 en S30 al
      sumar 1º/2º; código=persona, nombre no-placeholder priorizado, carga ≤18h), 12
      aulas compartidas (diferencias cosméticas de nombre → linaje instituto), 0
      colisiones grupo/subgrupo/actividad. Modelado Opción B (conservador): EF de 3º
      conserva aulaFija para atribución limpia. Fixture generado programáticamente y
      validado contra el schema real + batería completa (unicidad de las 7 colecciones
      que el mapper deduplica —plaza NO, el mapper no la impone—, integridad, I2, I7,
      XOR aula, aulas fijas disjuntas, carga ≤30, cuadre 16×30 + 3PDC=22) +
      SolverHorarioFusionEsoCompletaTest (calca el de fusión 3º+4º). FACTIBLE en 2,110s,
      0 duras (suite 50 verde). RESULTADO: D4 NO muerde ni a escala de ESO completa con
      Gim 26/30; D4 rebajada a residual. SEÑAL DE COSTE: primer punto de la curva con
      crecimiento no-lineal (×7 en tiempo por ×1,7 en grupos, 0,298s→2,110s); lejos del
      límite (600s) pero a vigilar al sumar Bach/FPB. Falso positivo de método cazado y
      corregido: la batería marcaba "unicidad global de plaza" como invariante; el
      mapper real (leído) NO deduplica plazas (escala-instituto reutiliza B2-PEPA/CyR/
      Fr2 entre dos actividades de bloque de 2º; legítimo). NO cierra criterios 1-2
      (faltan Bach+FPB). Escala+fusión de fixtures, sin dominio nuevo: src/main no
      tocado.
- [x] Bloque 11 — Escala 1ºBach completo (4 grupos ordinarios A/B/C/D), linaje
      AISLADO (S32). Fixture problema-5-escala-1bach.json (4 grupos, 30 tramos,
      65 subgrupos, 30 actividades, 16 aulas, 28 profesores) +
      SolverHorarioEscala1BachTest. PRIMERA validación a escala real de los dos
      bloques de optativas transversales Tipo 7 sobre los 4 grupos (OPT1: DTec/
      ANAP/TEstI/TICO/DA; OPT2: DTec/TEst2/Lab/CE/Pat), con DTec=4h compartida
      entre ambos bloques (subgrupo único, I6). Tres bloques de modalidad sobre
      subconjuntos: ciencias {A,B} (Bio/TecIn), humanidades {C,D} (Latín/MCCSS;
      HMC con profesor distinto por grupo GH6/GH5; LU; ECO con dos plazas
      paralelas; GRI sólo en oferta de D). Rel/PTVE intra-grupo, un tramo por
      grupo (tutoría implícita en PTVE, S8/Hallazgo E). Aula variable como
      aulasCandidatas (EF de C en Gim/Pista; LU; ECO-a; GRI). Decisión de
      modelado: subgrupos mono-grupo listados en plaza (estilo linaje instituto,
      frontera S14), NO Lectura B N:M de §6.5 — ambas válidas, se calca el
      instituto para fusión limpia (divergencia consciente con §6.5). Población:
      1 subgrupo por opción (plausible), DEUDA a confirmar con el centro
      (invariante de población). Cribado de aulas y cruce grupo↔aula POR CÓDIGO:
      0 celdas sin aula (Hallazgo H no muerde en Bach), 0 inconsistencias
      profesor↔plaza (D8 no muerde). Construido programáticamente desde los 4
      volcados de grupo + 11 volcados de aula; población real tomada del listado
      por aula (no del de grupo). Validado contra schema real + batería completa
      (integridad, unicidad, I2, I7, XOR aula, aulas fijas disjuntas, cuadre
      30/30 los 4 grupos, carga por recurso ≤30: máx profesor 12, aula fija 24).
      INFEASIBLE cazado durante la construcción (verificador Python contaba grupo
      por plaza en vez de por instancia; corregido al conteo por instancia que usa
      VerificadorSolucion, S21/D14) — aprendizaje reconfirmado: la autoridad es el
      modelo real, no la intuición. FACTIBLE, 0 duras (suite 51 verde). 1ºBach
      aislado es trivial para el solver (4 grupos). Escala pura: src/main NO
      tocado. NO cierra criterios 1-2 (faltan 2ºBach y FPB, y la fusión con ESO).
- [x] Bloque 12 (Sub-bloque A de FPB) — D13 en src/main (S33). PRIMER bloque de
      Fase 5 que toca src/main desde 6d-c. Prerrequisito de FPB: los bloques de 2-3
      tramos consecutivos exigen impedir que un IntervalVar cruce la frontera de día
      o de recreo. Cerrada D13 (ver su entrada en Deuda consciente): lista blanca de
      inicios en ModeloCpSat (no-op para duracion=1, sin regresión en ESO/Bach) +
      espejo en VerificadorSolucion (tramosOcupados + verificarBloquesConsecutivos)
      + verificarNoSolapes ahora cuenta por tramo ocupado (cierra ceguera a solapes
      en tramos interiores de bloque, gemela del patrón D14). Decisión de modelado
      (b)+(Vía B): D13 cubre cruce de día Y de recreo, frontera de recreo como
      constante (deuda D22). Probada por discriminación pura (SolverHorarioBloqueD13Test
      3 casos: control FEASIBLE, desborde de día INFEASIBLE, cruce de recreo
      INFEASIBLE). Suite 54 verde, BUILD SUCCESS, sin regresión. NO cierra criterios
      de Fase 5 (es prerrequisito; la prueba a escala es el Sub-bloque B). Índice de
      código regenerado (src/main cambió).
- [x] Sub-bloque B de FPB (S34) — fixture 1ºFPB real a escala. D13 ejercitada a
      escala: 2 bloques-3 (PS, MECSO) + 5 bloques-2 + 9 sueltas, FACTIBLE 0,021 s,
      0 duras. Aula técnica TALL_FPB nominal (Opción 1, Hallazgo H). Hallazgo K
      (MECSO blk-2 + blk-3; §6.6 corregido). src/main NO tocado. NO cierra
      criterios de Fase 5 (1ºFPB aislado). Fixture problema-5-escala-1fpb.json +
      SolverHorarioEscala1FpbTest.
- [x] Sub-bloque C de FPB (S35) — fixture 2ºFPB real a escala. CIERRA el nivel
      FPB. D13 a más escala: 3 bloques-3 + 5 bloques-2 + 9 sueltas, FACTIBLE
      0,020 s, 0 duras. Aula técnica TALL_FPB nominal (Opción 1, Hallazgo H);
      CyS↔TALL3 cruzado exacto. Tutor PAU1. Troceo determinista validado;
      Hallazgo K asumido (el volcado no etiqueta el troceo). Todo NEUTRA
      (neutraliza D12 palomar). src/main NO tocado. NO cierra criterios de Fase 5.
      Fixture problema-5-escala-2fpb.json + SolverHorarioEscala2FpbTest.
- [x] Bloque 13 — FUSIÓN INSTITUTO COMPLETO (S36). ESO completa + 1ºBach +
      2ºBach + 1ºFPB + 2ºFPB en un único fixture: 26 grupos, 341 subgrupos, 229
      actividades, 35 aulas, 59 profesores. CIERRA los criterios 1-2 de Fase 5
      (factible 269,4 s < 10 min; 0 duras). 2ºBach plegado dentro de la fusión
      (no aislado; aprendizaje S31). 2ºBach derivado por FIRMA DE POSICIÓN de los
      volcados grupo-2BACH-A/B/C.json (cada bloque NEUTRA = 1 slot del grupo,
      cuadre 30/30 por construcción); optatividad transversal ABC 4h en dos NEUTRA
      con DT compartido (I6), modalidades transversales B+C entrelazadas con
      bloques internos, aulasCandidatas en plazas que rotan. 2ºBach SIN EF (no
      añade D4). Fusión por unión de catálogos cruzada POR CÓDIGO (profesores
      código=persona, máx 23/30; 0 colisiones grupo/subgrupo/actividad; prefijo
      2BA/2BB/2BC limpio frente a 2A/2B/2C de ESO). HALLAZGO: TALL_FPB colapsaba
      los talleres de 1ºFPB/2ºFPB → 49 tramos/30 INFEASIBLE; el centro confirmó
      talleres distintos → TALL_FPB_1/TALL_FPB_2 (corrección de datos, no de
      modelado). Coste no lineal confirmado (×78 en tiempo por ×1,53 en grupos):
      deuda D23. Validado por réplica Python contra loader/mapper/Verificador
      reales antes de ejecutar. Suite 57 verde, BUILD SUCCESS. src/main NO tocado.
      Fixture problema-5-fusion-instituto-completo.json +
      SolverHorarioFusionInstitutoCompletoTest. Población de subgrupos de 2ºBach
      (qué alumnado cae en cada plaza de modalidad/optatividad): deuda a confirmar
      con el centro, como en 1ºBach (S32).
- [x] Bloque 16 — PODA DE AULA (D23 palanca b), mecanismo + discriminación (S41).
      Poda dura de aulasCandidatas SOLO en optimización (construirConObjetivo): una
      plaza con >UMBRAL_PODA_AULA=8 candidatas se recorta a MAX_AULAS_PODA=8 por orden
      de código (determinista). Medición previa sobre el instituto completo: 21/52
      plazas con candidatas (modalidades 2ºBach) listan 25 y concentran el 92% de las
      1835 presencias de aula — el foco que D23 acusaba. Suelo de saturación medido 3;
      K=8 con margen ×2,6. Fixtures sintéticos problema-poda-aula-factible /
      -oro-saturacion + SolverHorarioPodaAulaTest (positivo: poda 12->8 sigue factible,
      aula ∈ recorte; oro: 9 plazas simultáneas, sin poda factible / con poda 8<9
      INFEASIBLE por palomar — atribución perfecta). Suite 61 verde, BUILD SUCCESS.
      src/main tocado (ModeloCpSat: campos + método private). NO mide aún el efecto
      sobre D23 a escala (bloque siguiente, @Tag escala). NO cierra criterio 3 ni D23.
- [x] Bloque 17 — PODA DE AULA MEDIDA A ESCALA: INVIABLE. CIERRA palanca (b) de D23
      con dato NEGATIVO (S42). El frente era medir la poda de S41 sobre el instituto
      completo; la medición destapó que la poda ROMPE la factibilidad en vez de
      acelerar. Diagnóstico pareado (misma máquina/fixture, aislado, sin JaCoCo): SIN
      poda -> FEASIBLE objetivo 215 cota 2 en 600 s; CON poda K=8 -> UNKNOWN (ni una
      factible en 600 s). Atribución limpia: la poda dio UNKNOWN en las 3 corridas
      (suite, aislada, aislada-sin-JaCoCo); sin poda da FEASIBLE aislada. Fallo de S41:
      el suelo de saturación medía un tramo (necesario, no suficiente para factibilidad
      global). DECISIÓN (opción 2): poda OFF por defecto (construirConObjetivo() delega
      en construirConObjetivo(false)); mecanismo conservado latente y documentado en
      javadoc. Tests reorganizados: eliminados SolverHorarioPodaAulaTest + 2 fixtures y
      el de S37; nuevo SolverHorarioOptimizacionEscalaInstitutoCompletoTest (lee
      ResultadoOptimizacion). Suite 59 verde, BUILD SUCCESS. ALTA D25 (contención del
      perfil -Pescala entero, reactivación de D24). src/main tocado (default poda +
      javadoc) -> índice regenerado; modelo NO tocado. CIERRA palanca (b); empuja D23
      hacia decisión de producto.
- [x] Bloque 18 — EXPERIMENTO PAREADO DE ATRIBUCIÓN: la no-convergencia a escala es
      ESTRUCTURAL, no de un bloque. CIERRA D23 como decisión de producto (S43). Falsar
      si FPB endurece la optimización a escala. Tres puntos sobre el mismo fixture
      (problema-5-fusion-instituto-completo.json) recortado EN MEMORIA por bloque
      académico (catálogo idéntico entre puntos; frontera ESO/Bach/FPB separable sin
      referencias colgantes, verificado por réplica). Cada punto aislado (D25). RESULTADO:
      P0 base 26g FEASIBLE obj 221 cota 0; P1 sin FPB 24g FEASIBLE obj 216 cota 2; P2 solo
      ESO 17g FEASIBLE obj 62 cota 0. ATRIBUCIÓN (sobre estado/cota, no objetivo absoluto):
      FPB inocente (P0->P1 sin cambio de régimen); Bach domina la magnitud (216->62) pero
      quitarlo NO da convergencia (P2 sigue FEASIBLE cota 0); la cota no se cierra ni con
      solo ESO -> propiedad del modelo, no de un bloque. DECISIÓN DE PRODUCTO firmada:
      FEASIBLE sin optimalidad probada es el modo aceptado del solver a escala. Test nuevo
      SolverHorarioOptimizacionEscalaSubconjuntosTest (@Tag escala, recorte en memoria con
      fail-fast de frontera), convive con el de instituto completo. Suite rápida 59 verde,
      BUILD SUCCESS. src/main NO tocado -> índice NO regenerado; modelo NO tocado. NO cierra
      criterio 3 (sigue exigiendo umbral con datos del centro).

### Bloques de Fase 6
- [x] Bloque 1 — Andamiaje del módulo app/ + humo de persistencia (S45). Spring Boot
      4.1.0 + Hibernate 7.4.1 + SQLite + dialecto de comunidad; hbm2ddl sobre
      HumoEntity desechable; test de integración del .db en disco.
- [x] Bloque 2 — Catálogo del centro como entidades JPA + repositorios (S46). 8
      entidades §4.1 en app.catalog + 3 enums propios + 8 repos; round-trip
      @DataJpaTest sobre SQLite real; LocalTime intacto. Mapper fuera (Bloque 3).
- [x] Bloque 3 — Mapeo catálogo Entidad JPA -> modelo del solver (S47).
      CatalogoMapper (Aula, Asignatura, Profesor, Grupo, Tramo); VIRTUAL_OPTATIVA
      lanza excepción explícita; recreo excluido de Tramo; Aula.nombre=codigo
      (D26); código de Tramo sintetizado L1..V6 (D27). Mapea a listas sueltas,
      NO a ProblemaHorario (faltan Subgrupo/Actividad).
- [x] Bloque 4 — Subgrupo como entidad JPA (§4.2) + tramo de mapper (S48).
      Entidad Subgrupo en app.catalog (id sintético + codigo único, mismo estilo
      que GrupoAdministrativo JPA) con @ManyToMany unidireccional a
      GrupoAdministrativo (materializa SubgrupoGrupo; join table subgrupo_grupo,
      Subgrupo dueño, LAZY) + SubgrupoRepository. CatalogoMapper.aSubgrupo(entidad,
      gruposPorCodigo) resuelve la población por identidad de objeto (mismo patrón
      que aGrupo con grupoPadre) y aborta con IllegalArgumentException ante grupo
      no presente en el índice. Particion/SubgrupoParticion DIFERIDAS a Fase 8
      (decisión D-a: el dominio del solver no las consume; su UX se diseña con la
      UI, deudas D1/D7). NO entra Actividad (Bloque 5) ni el ensamblado de
      ProblemaHorario (Bloque 6). Primer @ManyToMany del proyecto: round-trip
      sobre SQLite real (dialecto de comunidad) verde para subgrupo multi-grupo
      (Lectura B, 3 grupos) y mono-grupo. Suite: solver 59 + app 12, BUILD SUCCESS.
- [x] Bloque 5 — Actividad y Plaza como entidades JPA (§4.6) + mapper (S49).
      Entidades Actividad (agregado raíz, @OneToMany cascade+orphanRemoval a
      Plaza) y Plaza (dependiente; @ManyToOne opcional aula_fija + tres
      @ManyToMany: plaza_profesor/plaza_aula_candidata/plaza_subgrupo) en
      app.catalog + enum propio app.catalog.PatronTemporal (D-B5-6) +
      ActividadRepository (Plaza sin repo propio, es dependiente).
      CatalogoMapper.aActividad (entidad a entidad) + helper privado aPlaza +
      resolver genérico + aPatronTemporal (traduce entre los dos PatronTemporal).
      Cinco decisiones cerradas antes de construir: D-B5-1 ActividadInstancia NO
      se materializa (artefacto derivado, lo expande cpsat.Expansion; §4.7 decidirá
      su identidad persistida); D-B5-2 el XOR aula_fija/candidatas lo valida el
      record de dominio, no la entidad JPA; D-B5-3 la política "candidatas hasta
      Fase 3" no aplica (era del mapper del JSON); D-B5-4 Actividad.asignatura FK
      nullable, Plaza.asignatura FK not null; D-B5-5 requiereTutor persiste pero el
      mapper lo ignora (el record domain.Actividad no lo porta, S8 no la consume el
      solver hoy); D-B5-6 PatronTemporal propio, no reutilización del de dominio
      (frontera "entidad JPA con su forma", como TipoGrupo/Dia). NO entra el
      ensamblado de ProblemaHorario (Bloque 6). TRES RIESGOS DE PERSISTENCIA
      CERRADOS EN POSITIVO por round-trip sobre SQLite real (dialecto de comunidad):
      (1) cascade Actividad→Plaza con un solo save; (2) densidad de Plaza (aula_fija
      @ManyToOne + tres @ManyToMany conviviendo) —primera entidad con esta densidad,
      B4 solo validó un @ManyToMany aislado—; (3) ambas ramas del XOR. Modo híbrido:
      esqueletos y decisiones en el Project, tecleo/compilación en Claude Code;
      tests generados en Claude Code. Aprendizaje de proceso reafirmado: índices del
      test del mapper construidos consumiendo la salida del propio mapper
      (aAsignatura/aProfesor/aAula/aSubgrupo), no con new sobre solver.domain.
      CatalogoMapperActividadTest quedó en app.catalog (no app.mapper) porque
      Actividad/Plaza solo tienen constructor protected; asimetría menor registrada.
      Suite: solver 59 + app 20 (12 previos + ActividadRoundTripTest 3 +
      CatalogoMapperActividadTest 5), BUILD SUCCESS con mvn clean test desde la raíz.
      src/main del solver NO tocado. Siguiente: Bloque 6 (ensamblado de
      ProblemaHorario completo sobre las entidades ya mapeadas de B2-B5).
- [x] Bloque 6 — Ensamblado de ProblemaHorario (JPA → dominio del solver) (S50).
      CatalogoMapper.aProblemaHorario(tramos, aulas, asignaturas, profesores,
      grupos, subgrupos, actividades) — siete listas de entidades JPA de
      app.catalog → domain.ProblemaHorario completo. Construye en producción los
      cinco índices Map<String,...> (aulas/asignaturas/profesores/grupos/subgrupos)
      consumiendo la salida del propio mapper (aprendizaje B4/B5 promovido de test
      a producción), en orden de dependencia: tramos → hojas → grupos → subgrupos →
      actividades. Índices con Collectors.toMap de dos argumentos → código
      duplicado ABORTA con IllegalStateException, no gana el último. Listas de
      dominio con stream().map().toList() (preservan orden de entrada). Cinco
      decisiones cerradas antes de construir: D-B6-1 catálogo COMPLETO sin poda
      (grupos/subgrupos huérfanos entran en las listas top-level; el mapper
      traduce, no poda); D-B6-2 índices en producción consumiendo el mapper, nunca
      new sobre solver.domain; D-B6-3 restriccionesHorarias DIFERIDA (List.of(): no
      existe entidad JPA ProfesorRestriccionHoraria; el dato solo entra por el
      camino JSON; deuda D28); D-B6-4 recibe listas ya cargadas, no repositorios —
      mapper puro, la orquestación carga-repos→mapper→solver es un servicio de
      aplicación posterior; D-B6-5 método en CatalogoMapper, no clase nueva.
      Firmas confirmadas contra el repo real; el orden del record ProblemaHorario
      coincidió con la firma del contrato (sin desajustes, no hubo que parar).
      CatalogoMapperProblemaTest en app.catalog (Actividad/Plaza con ctor
      protected, como CatalogoMapperActividadTest): ensamblado feliz con recreo
      intercalado + referencia por valor; coherencia subgrupo↔grupo top-level por
      igualdad estructural (protege S9); catálogo completo con huérfanos (protege
      D-B6-1); restriccionesHorarias vacía por contrato (centinela de D28); código
      de aula duplicado → IllegalStateException; actividad con profesor ausente →
      IllegalArgumentException con el código huérfano. src/main del solver NO tocado
      (el mapper vive en app/) → referencia-codigo-solver.md NO regenerado; modelo
      NO tocado. Suite: solver 59 + app 26 (20 previos + CatalogoMapperProblemaTest
      6), BUILD SUCCESS con mvn clean test desde la raíz. Commits separados
      código/tests be80f90/90da600, de una línea. Cierra el ensamblado del catálogo:
      el camino JPA produce ya un ProblemaHorario completo salvo restricciones
      horarias (D28). Siguiente: Bloque 7 (a decidir; candidatos: entidad JPA de
      restricciones horarias que cierre D28, o servicio de aplicación que orqueste
      repos→mapper→solver).
- [x] Bloque 7 — Entidad JPA de restricciones horarias (§4.3) + mapper (S51).
      Cierra D28: el camino JPA ya ensambla un ProblemaHorario COMPLETO, con
      restricciones horarias incluidas. Entidad ProfesorRestriccionHoraria en
      app.catalog (id sintético IDENTITY; @ManyToOne LAZY a Profesor y a
      TramoSemanal, ambos nullable=false; @Enumerated(STRING) tipo; int peso;
      String motivo nullable; ctor protected + público, sin equals/hashCode,
      calcada de TramoSemanal) + enum propio app.catalog.TipoRestriccion
      {DURA,BLANDA} + ProfesorRestriccionHorariaRepository vacío. En CatalogoMapper:
      aTipoRestriccion (switch, gemelo de aPatronTemporal); aRestriccionHoraria
      (profesor por código vía resolver, tramo por REFERENCIA de objeto,
      motivo → Optional.ofNullable); refactor de aTramos SIN cambio observable
      (helper privado aTramosConIndice produce en un mismo bucle la List<Tramo> y
      un IdentityHashMap<TramoSemanal,Tramo>; aTramos público delega); y
      aProblemaHorario gana el 8º parámetro List<ProfesorRestriccionHoraria>,
      sustituyendo el List.of() centinela por las restricciones mapeadas.
      Seis decisiones cerradas antes de construir (D-B7-1 a D-B7-6, inline abajo).
      Clave: D-B7-1 alcance = entidad (no servicio de
      orquestación, que es B8, porque el servicio debe orquestar un mapper YA
      completo); D-B7-2 el tramo se resuelve por referencia de objeto
      (IdentityHashMap), NO por el código sintético L1..V6 (no reabre D27) ni por
      (día,ordenEnDia) —la entidad TramoSemanal porta orden GLOBAL con recreo, no
      ordenEnDia—; D-B7-3 una restricción sobre un tramo de recreo (ausente del
      índice) ABORTA, no se ignora; D-B7-6 el peso DEFAULT 1 de §4.3 NO se
      materializa en la entidad (exige peso explícito): es política de la capa de
      configuración/UI (Fase 8), no dato de la entidad. RIESGO CERRADO EN POSITIVO:
      primer @ManyToOne JPA a TramoSemanal no autorreferencial sobrevive el
      round-trip sobre SQLite real (dialecto de comunidad). Tests:
      ProfesorRestriccionHorariaRoundTripTest (@DataJpaTest, DURA+BLANDA, ambas FK
      + tipo + peso + motivo intactos) + CatalogoMapperRestriccionTest (unitario:
      DURA con motivo, BLANDA sin motivo, profesor huérfano aborta, tramo de recreo
      aborta, resolución por referencia con varios tramos; índices construidos
      consumiendo aProfesor/aTramos, no `new` sobre solver.domain). El centinela de
      D28 en CatalogoMapperProblemaTest pasa de "verifica vacío" a ensamblar y
      verificar una restricción real (ensamblaUnaRestriccionHorariaReal); los 5
      casos previos ganan List.of() como 8º arg. src/main del solver NO tocado
      (todo en app/; el record domain.RestriccionHoraria ya existía desde S25) →
      referencia-codigo-solver.md NO regenerado; modelo NO tocado. Suite: solver 59
      + app 32 (26 previos + 6), BUILD SUCCESS con mvn clean test desde la raíz.
      Commits separados código/tests 84a8bed/f2e81a7, de una línea. Siguiente:
      Bloque 8 (servicio de aplicación que orqueste repos → CatalogoMapper →
      SolverHorario, ya sobre un ensamblado sin agujeros).
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
- [x] Bloque 9 — Persistencia de la SolucionHorario (§4.7) (S53). Cierra el
      criterio abierto de Fase 6 ("cerrar la app, reabrir, el horario intacto"):
      B8 dejó entrada->solver; B9 cierra solver->persistencia. En app.persistence:
      enum EstadoHorario {BORRADOR,DEFINITIVO,DESCARTADO}; entidad HorarioGenerado
      (Instant fechaGeneracion, estado @Enumerated STRING def. BORRADOR, estadoSolver
      String, objetivo/cotaInferior Double NULLABLE reales, @OneToMany a Sesion);
      entidad Sesion (una por PLAZA colocada; @ManyToOne a horario/plaza/tramoInicio/
      aula, int indice, @UniqueConstraint(horario_id,plaza_id,indice), SIN FK de
      actividad —se deriva por plaza.getActividad()—); repos HorarioGeneradoRepository/
      SesionRepository. Mapper de salida app.mapper.SolucionMapper (final, ctor privado,
      estáticos): aSesiones recorre actividades × índices × plazas del ProblemaHorario,
      resuelve plaza y aula a entidad JPA por CÓDIGO (índices Map<String,_> recargados
      en la transacción de escritura) y el tramo por el puente P1 (indiceTramos, ver
      D30); aula SIEMPRE por solucion.aulaElegida(inst,plaza).orElseThrow (aborta
      ruidoso si empty); código/tramo sin correspondencia aborta ruidoso. Servicio:
      generar() INTACTO (puro, solver fuera de transacción, D-B8-1); guardar(
      ResultadoOptimizacion,ProblemaHorario,String) @Transactional (índices por código
      —plazas derivadas de actividadRepository.findAll().flatMap(getPlazas), NO hay
      PlazaRepository: Plaza es entidad dependiente de Actividad, B5— + puente de tramo,
      crea la cabecera con estadoSolver/objetivo/cotaInferior del resultado, materializa
      las N sesiones); cargarHorario(Long) @Transactional(readOnly) devuelve
      HorarioGenerado + sus Sesion (entidades JPA, lo que consume Fase 7), NO reconstruye
      SolucionHorario de dominio. Ocho decisiones cerradas antes de construir (D-B9-1 a
      D-B9-8): la de más impacto, Sesion = plaza colocada (Opción A), que CORRIGIÓ el
      UNIQUE de §4.7 (era por actividad_instancia, imposibilitaba el desdoble). RIESGO
      PRINCIPAL CERRADO EN POSITIVO: el puente del tramo (domain.Tramo con código
      sintético L1..V6 y ordenEnDia por día vs TramoSemanal con orden GLOBAL con recreo,
      sin código ni ordenEnDia) NO resuelve por código; se replica la renumeración de
      aTramos y se empareja por (diaSemana,ordenEnDia) —deuda D30—. Test de integración
      GuardarHorarioServiceTest (@DataJpaTest+@Import, SQLite real, en app.catalog por
      los ctor protected): desdoble de 2 plazas con recreo intercalado -> 2 filas Sesion
      con tramo/aula/índice correctos + metadata del solve round-trip; caso negativo
      aula ausente -> aborta con el código de la plaza. Cobertura pendiente (deuda de
      test menor, no bloqueante): plaza con aulasCandidatas (rama variable de
      aulaElegida); el test solo ejercita aula fija. src/main del solver NO tocado ->
      referencia-codigo-solver.md NO regenerado; §4.7 del modelo ya corregido en S53
      (Sesion + HorarioGenerado). Suite: solver 59 + app 35 (33 previos + 2 del IT),
      BUILD SUCCESS con mvn clean test desde la raíz. Commits separados código/tests
      aaa660a/84cdba5, de una línea. Siguiente: cierre de Fase 6 (revisar los 4
      criterios) o Bloque 10 (a decidir).
- [x] CIERRE DE FASE (S54). Alcance decidido al inicio entre cerrar Fase 6 y abrir
      un Bloque 10 (candidato SesionBloqueada §4.7); se eligió cerrar, porque un B10
      de persistencia no acercaba el cierre (SesionBloqueada no la consume el solver,
      C5) y dejaba sin firmar los criterios 2 y 4. Entregable: test de humo
      end-to-end CierreFase6HumoTest (app.catalog), que ejercita por primera vez el
      pipeline completo de una tirada: repos JPA → CatalogoMapper → SolverHorario
      real (vía de factibilidad, presupuesto 10 s) → SolucionMapper → guardar →
      recargar. Fixture = builder JPA que transcribe problema-3-cierre-cyr-refmt.json
      (bloque de 6 plazas de 1ºESO rep=2 + 4 Mat) SIN cargarlo, añadiendo los recreos
      que el JSON omite. Test 1 verifica el round-trip (bloque → 12 sesiones, cada Mat
      → 3, total 24, toda Sesion con plaza/tramo/aula no null y en tramo lectivo, 0
      violaciones duras por VerificadorSolucion). Test 2 (criterio 2) da de alta una
      ProfesorRestriccionHoraria BLANDA y relanza, comprobando conteos de catálogo
      intactos y coexistencia de los dos horarios. Nueve decisiones D-B10-1..9
      cerradas antes de construir. Hallazgos al leer el repo: objetivo/cota de
      ResultadoOptimizacion son double primitivos (no null → vía con detalle, persiste
      valores reales); ModeloCpSat no filtra por tipo de aula (todas ORDINARIA).
      Cobertura ganada: plaza con aulasCandidatas resuelta por el solver (B9 solo
      probó aula fija). D30 NO verificada (la aserción vigila que no caiga en recreo,
      no el emparejamiento exacto). Suite: solver 59 + app 37 = 96, BUILD SUCCESS con
      mvn clean test desde la raíz. Un único fichero de test nuevo; src/main NO tocado.
      Commit de código 549bc92 de una línea; commit de doc aparte. Los 4 criterios de
      Fase 6 quedan firmados: la fase se da por CERRADA. Siguiente: Fase 7 (UI).

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
| Base de datos | SQLite + Hibernate 7.4.1, dialecto de comunidad org.hibernate.community.dialect.SQLiteDialect (Hibernate no trae dialecto SQLite oficial; el best-effort funciona sobre Hibernate 7.4, verificado en S45). Esquema vía hbm2ddl en Fase 6; sin Flyway por ahora. Fichero local, ruta relativa al working dir |
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

### Deuda consciente registrada en Fase 1

Las siguientes simplificaciones de Fase 1 se aceptan conscientemente y pueden
requerir cambios en fases futuras. Ver `modelo_datos_fase1.md` sección 8 para
descripción completa.

- **D1**: Generación automática de subgrupos por plantilla → Fase 8 (UI)
- **D2**: Versionado intra-BD de cursos académicos → Fase 10 (si se requiere)
- **D3**: Validación de capacidad de aulas → Fase 5 (evaluar con datos reales)
- **D4**: Modelado explícito de recursos compartidos (Gim, Pista) → Fase 5 si
  resulta insuficiente. CERRADA (S36): el instituto completo literal de los
  criterios 1-2 (ESO + Bach + FPB, 26 grupos) resultó FACTIBLE con modelado
  conservador (EF en aulaFija, sin relajar a aulasCandidatas), 0 duras. Gim a 28h
  sobre 30 tras sumar la EF de 1ºBach (2ºBach no tiene EF); Pista a 14. La válvula
  fue EdFís-1BC con candidatas {Gim,Pista}, que el solver desvió a Pista. La
  pregunta de fondo de D4 (¿Gim/Pista compiten de verdad y rompen a escala total?)
  tiene respuesta definitiva: NO. El segundo turno (relajar EF a aulasCandidatas)
  NO fue necesario. Histórico: ESO completa (S31) ya iba a Gim 26/30 FACTIBLE.
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
- **D13 (CERRADA en Sesión 33, Fase 5 Sub-bloque A)**: el IntVar de tramo usa
  índice plano [0, |tramos|). Con duracionTramos > 1, un IntervalVar podía
  cruzar la frontera entre el último tramo de un día y el primero del siguiente
  (cruce de día) y, además —al no ser el recreo un Tramo y numerar ordenEnDia
  1..6 sin hueco—, podía cruzar el recreo (órdenes 3 y 4 contiguos en índice).
  La D13 registrada solo contemplaba el cruce de día; el cruce de recreo era una
  segunda cara descubierta al leer el código real (decisión (b): D13 cubre ambos,
  alineado con S6 del modelo §6.6 y con los datos —Hallazgo I: los pares que
  cruzan el recreo van como dos sueltas, no como bloque). Mecanismo:
  ModeloCpSat.iniciosValidosDeBloque restringe el dominio del inicio (lista
  blanca) a las posiciones desde las que el bloque cabe entero en el día sin
  cruzar el recreo (no-op para duracion=1). Espejo independiente en
  VerificadorSolucion.tramosOcupados + verificarBloquesConsecutivos; además
  verificarNoSolapes pasó a contar por tramo ocupado (no solo por el de inicio),
  cerrando una ceguera del verificador a solapes en tramos interiores de un
  bloque. Probada por discriminación (SolverHorarioBloqueD13Test: control que
  cabe FEASIBLE; desborde de día y cruce de recreo INFEASIBLE). Probada a escala
  con FPB real en S34 (Sub-bloque B): bloques de 2 y 3 tramos
  conviviendo con sueltas en 1ºFPB completo, FACTIBLE 0 duras; D13 discrimina
  inicios válidos de inválidos sin INFEASIBLE espurio.
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
- **D23 (Sesión 36, Fase 5 Bloque 13)**: curva de coste del solver NO LINEAL,
  confirmada con el instituto completo. Puntos medidos en factibilidad pura
  (`resolver`, sin objetivo): ESO completa 17 grupos → 3,46 s (S31); instituto
  completo 26 grupos → 269,4 s (S36). Un factor ×1,53 en grupos produjo ×78 en
  tiempo: salto de régimen, no crecimiento suave. Causa probable: 2ºBach añade
  mucho acoplamiento combinatorio (modalidades transversales B+C entrelazadas con
  bloques internos + optatividad heterogénea), y las aulasCandidatas ampliadas
  —necesarias para evitar el INFEASIBLE de saturación— ensanchan el espacio de
  búsqueda. IMPLICACIÓN: el criterio 1 (< 10 min) se cumple HOY en factibilidad
  pura con margen (269 s < 600 s), pero el régimen de optimización
  (`resolverOptimizando`, criterios 3-4) parte de esos 269 s y subirá al añadir
  términos blandos sobre el instituto completo. Riesgo de que el criterio 1 deje
  de cumplirse en modo optimización. Acción cuando se aborden los criterios 3-4 a
  escala: medir el tiempo optimizando sobre el instituto completo y, si excede,
  considerar (a) límite de tiempo con primera solución factible + mejora
  incremental, (b) estrechar aulasCandidatas con heurística de aula preferente,
  (c) warm-start desde la solución de factibilidad pura. Severidad: media (no
  bloquea Fase 5 hoy; condiciona el cierre de los criterios 3-4).
  ACTUALIZACIÓN (S40, Bloque 15b): palanca (c) MEDIDA a escala (opción A, igual presupuesto
  de optimización 600 s, instituto completo). El warm-start MEJORA calidad: objetivo 215->204
  (-5,1%) a igual tiempo. Pero NO resuelve la deuda: ambas corridas (fría y caliente) siguen
  FEASIBLE con cota ~1-2; la optimización a escala NO converge ni con hint. Palanca (c)
  cerrada con evidencia (ayuda marginal, no convergencia). Palancas (a) límite de tiempo con
  mejora incremental y (b) estrechar aulasCandidatas siguen abiertas si se busca convergencia
  real. D23 permanece abierta (severidad media): el cierre de los criterios 3-4 a escala sigue
  condicionado por la no-convergencia, no solo por la falta de datos del centro.
  ACTUALIZACIÓN (S41, Bloque 16): palanca (b) CONSTRUIDA y validada en discriminación
  (aún NO medida a escala). Medición que fijó el frente: de 328 plazas, 52 usan
  aulasCandidatas; 21 (todas modalidades NEUTRA de 2ºBach, la estructura que esta deuda
  acusaba) listan 25 candidatas —siempre las 35 aulas menos las 10 especializadas— y
  concentran el 92% de las 1835 presencias de aula. La poda recorta esas colas a K=8
  (UMBRAL_PODA_AULA=8); suelo de saturación medido 3, así que K=8 es seguro con margen
  ×2,6 y el modo de fallo por saturación NO se da en los datos reales (haría falta K<3;
  por eso el oro es sintético). Vive solo en construirConObjetivo (no altera la curva de
  factibilidad pura de S36). Pendiente en el bloque siguiente: medir objetivo/tiempo con
  poda sobre el instituto completo vs. la línea base de S40 (215, 600 s sin converger) y
  decidir si la palanca mueve el régimen o si D23 se reduce a decisión de producto
  (aceptar objetivo relajado). Palanca (a) límite de tiempo con mejora incremental sigue
  abierta. Interacción a vigilar: la semilla de resolver() (sin poda) puede elegir un aula
  que la poda elimina; sembrarHint la siembra 0 en las opciones podadas (benigno).
  ACTUALIZACIÓN (S42, Bloque 17 — CIERRA la palanca b con dato NEGATIVO): la poda NO acelera
  la convergencia; ROMPE la factibilidad a escala. Diagnóstico pareado en la misma máquina y
  fixture (problema-5-fusion-instituto-completo.json), aislado y sin JaCoCo: SIN poda ->
  FEASIBLE objetivo 215 cota 2 en 600 s; CON poda (K=8) -> el solver NO halla ni una solución
  factible en 600 s (UNKNOWN). Atribución limpia: la poda dio UNKNOWN en las TRES corridas
  (suite -Pescala, aislada, aislada-sin-JaCoCo), descartando contención y JaCoCo como causa;
  sin poda la misma vía da FEASIBLE aislada (= línea base S40). Diagnóstico del fallo de S41:
  el "suelo de saturación 3 => K=8 seguro con margen ×2,6" medía la saturación MÁXIMA en UN
  tramo (clique de plazas mutuamente compatibles), que es condición NECESARIA pero NO
  SUFICIENTE de la factibilidad global; recortar 25->8 en las 21 plazas acopladas de 2ºBach
  estrecha el espacio de aulas de forma acoplada en toda la semana y la heurística de CP-SAT
  deja de tropezar con una factible. La poda no es un mecanismo "correcto pero apagado": es
  inviable con K=8 y, si se retoma, será REDISEÑADA (otro K, poda selectiva o poda blanda vía
  hint), no resucitada. DECISIÓN (opción 2 de 3; descartadas: 1 revertir todo — tira el
  mecanismo correcto en sí; 3 rediseñar ahora — frente nuevo, caro, éxito incierto): poda
  DESACTIVADA por defecto (construirConObjetivo() delega en construirConObjetivo(false)),
  mecanismo conservado LATENTE (sobrecarga construirConObjetivo(boolean), constantes
  UMBRAL_PODA_AULA/MAX_AULAS_PODA y candidatasPodadas intactos), documentado en javadoc de
  ModeloCpSat. Producción vuelve a FEASIBLE 215 (estado conocido de S40). Tests: eliminados el
  de poda a escala (S41 medía la vía rota), el de discriminación SolverHorarioPodaAulaTest +
  sus 2 fixtures (sin anclaje a producción tras apagar la poda), y el de S37 (vía pelada);
  nuevo SolverHorarioOptimizacionEscalaInstitutoCompletoTest lee estado/objetivo/cota. ESTADO
  DE D23 TRAS S42: palancas (b) poda CERRADA-inviable y (c) warm-start CERRADA-ayuda-no-resuelve;
  queda viva (a) límite de tiempo con mejora incremental, y el desenlace de fondo que el plan
  dejó legítimo desde el principio: D23 se reduce a DECISIÓN DE PRODUCTO (aceptar objetivo
  relajado FEASIBLE ~215 sin convergencia probada). Recomendación del advisor: con (b) y (c)
  agotadas y (a) sin promesa de convergencia, el siguiente paso natural NO es otra palanca de
  velocidad sino cerrar D23 como decisión de producto explícita. Pendiente de decidir con el
  dueño del proyecto.
  CIERRE (S43, Bloque 18 — D23 CERRADA como DECISIÓN DE PRODUCTO): antes de cerrar, un
  experimento pareado descartó las dos hipótesis localizables de la no-convergencia. Tres
  puntos sobre el mismo fixture recortado en memoria por bloque académico (catálogo idéntico;
  frontera separable sin referencias colgantes, verificado por réplica), cada uno aislado:
  P0 base 26 grupos FEASIBLE obj 221 cota 0; P1 sin FPB 24 grupos FEASIBLE obj 216 cota 2;
  P2 solo ESO 17 grupos FEASIBLE obj 62 cota 0. Lectura sobre estado/cota (no objetivo
  absoluto, varianza ±7 por D25): (1) FPB NO endurece (P0->P1 no cambia de régimen); (2) Bach
  domina la MAGNITUD del objetivo (216->62 al retirarlo) pero retirarlo NO produce convergencia
  (P2 sigue FEASIBLE cota 0); (3) la cota inferior no se cierra ni con solo ESO -> la
  no-convergencia es propiedad del MODELO/objetivo a esta escala, no atribuible a un bloque. Sin
  palanca de velocidad con promesa de convergencia, y siendo una FEASIBLE obj ~221 un horario
  usable, el dueño del proyecto FIRMA la decisión de producto: aceptar FEASIBLE sin optimalidad
  probada como modo de operación del solver a escala. D23 deja de ser deuda técnica abierta. Dato
  útil si alguna vez se reabre la optimización: el esfuerzo de modelado rendiría más concentrado
  en la estructura de Bach (mayor contribuyente al coste). NOTA: esto NO cierra el criterio 3 de
  Fase 5 (calidad comparable), que sigue ABIERTO a la espera del umbral con datos del centro, no
  de convergencia. Test de evidencia: SolverHorarioOptimizacionEscalaSubconjuntosTest (Bloque 18).
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
- **D24 (Sesión 38, Fase 5 Bloque 15a — CERRADA en Sesión 39; ver D25, S42)**: la suite de tests se autoenvenena por
  contención de CPU. Hay dos tests de límite ~600 s (SolverHorarioFusionInstituto-
  CompletoTest y SolverHorarioOptimizacionInstitutoCompletoTest) que corren en CADA
  mvn test. En la misma máquina se quitan núcleos a CP-SAT (multihilo en factibilidad
  pura) y el wall-clock se dispara: en S38 el de fusión dio UNKNOWN a 600 s en la
  suite completa pero FACTIBLE en 86,4 s aislado. No es regresión funcional. Impacto:
  (a) la suite tarda 20 min y crecerá con cada palanca de D23 medida a escala; (b) el
  build es inestable (el orden de Surefire decide quién envenena a quién); (c) rompe
  el criterio de cierre "suite verde" por una razón no funcional. Solución acordada
  (Opción A): @Tag("escala") en los tests pesados + exclusión del mvn test por defecto
  + perfil/job dedicado para correrlos a propósito. Descartadas: bajar maxTimeInSeconds
  (no arregla la contención, falsea el criterio de 10 min) y setNumWorkers(1) (haría
  los tests deterministas pero mucho más lentos y mediría un rendimiento distinto al de
  producción). Severidad: media (no es bug del solver, pero bloquea el cierre limpio de
  bloques a escala). Asignación: bloque inmediato, ANTES del warm-start (15b).
  RESUELTA (S39): @Tag("escala") en los dos tests pesados + exclusión por defecto vía
  property surefire.excluded.groups=escala en el pom del módulo solver + perfil 'escala'
  que invierte las properties (incluye escala, vacía exclusión). mvn test → 59 verde en
  8,6 s; mvn test -Pescala → solo los 2 pesados. Iteración registrada: vaciar
  <excludedGroups> como elemento literal en el perfil daba Tests run: 0 (merge de
  <configuration> vacío no determinista en Maven); la solución por property sí mergea
  limpio. CI inexistente hoy (confirmado); gancho dejado para Fase 12.
- **D14 (CERRADA en Sesión 15)**: `VerificadorSolucion` no comprobaba el
  no-solape por grupo (S9). En Fase 2 no importaba (sin subgrupos partidos,
  solape de grupo ⟺ solape de subgrupo, que sí verifica). Desde Fase 3 son
  distintos: el verificador era ciego a una restricción dura que el solver sí
  impone, así que un bug del modelo CP-SAT que solapara grupos pasaría
  desapercibido por la red de seguridad independiente. Cerrada en Sesión 15
  (commit intermedio de Fase 3), ANTES de meter la complejidad del aula
  variable: `VerificadorSolucion.verificarNoSolapes` añade un cuarto conteo
  por `GrupoAdministrativo` (derivado del Set de subgrupos por instancia, de
  modo que un desdoble cuenta el grupo una sola vez), gemelo de los de
  profesor/aula/subgrupo y ciego al `grupoPadre`. Deuda de vida corta: nació y
  murió dentro de Fase 3.
- **D15 (CERRADA en Sesión 18)**: era de DOBLE cara; la nota original solo veía
  la del verificador y afirmaba erróneamente que el solver prevenía la otra.
  Cara A — verificador: `VerificadorSolucion` contaba el aula con un `Set` POR
  INSTANCIA, lo que enmascaraba una violación de S2 (dos plazas de la misma
  instancia con la misma aula colapsan a un conteo de 1 → no se reporta). Por S2
  + glosario del modelo esas dos plazas son colisión, no uso compartido (el uso
  compartido legítimo es UNA plaza con varios profesores, no varias plazas con
  un aula). CERRADA: el aula se cuenta POR PLAZA; profesor y subgrupo siguen por
  instancia, donde el colapso es correcto por S1.
  Cara B — configuración: la afirmación original "el solver SÍ la previene vía
  `addNoOverlap`" era cierta SOLO para la rama de candidatas. Para dos `aulaFija`
  IGUALES en una misma instancia, la rama `aulaFija` de `restriccionNoSolapeAula`
  añade el intervalo POR INSTANCIA (no por plaza), de modo que el `addNoOverlap`
  ve un único intervalo y tampoco la previene: doble fallo silencioso (ni solver
  ni verificador). CERRADA por vía A (decidida con el usuario): validación de
  configuración `verificarAulasFijasDisjuntas` en `ProblemaHorarioMapper`, junto
  a `verificarI2`, que rechaza dos plazas de la misma actividad con la misma
  `aulaFija` ANTES del solver. No toca el modelo CP-SAT. No aplica a
  `aulasCandidatas` (el solver elige aulas distintas vía `addExactlyOne` +
  no-solape). El comentario de `verificarNoSolapes` se actualizó en consecuencia.
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
- **D28 (Sesión 50, Fase 6 Bloque 6) — CERRADA en S51 (Bloque 7)**: el ensamblado
  CatalogoMapper.aProblemaHorario pasa List.of() al componente
  restriccionesHorarias del ProblemaHorario, porque el catálogo JPA de B2 (§4.1)
  no materializó la entidad ProfesorRestriccionHoraria: no hay tabla, repositorio
  ni tramo de mapper para ella. El dato de indisponibilidades del profesorado
  (DURA/BLANDA, consumido por el solver desde S25, Bloque 6b) solo entra hoy por el
  camino JSON (RestriccionHorariaDto + ProblemaHorarioMapper.aDominio).
  Materializar la entidad JPA (FK a Profesor + FK a TramoSemanal + TipoRestriccion
  + peso + motivo) con su repositorio y su conversión aRestriccionHoraria es un
  bloque propio de Fase 6 (tamaño comparable a B4/B5); resolver la restricción
  contra el Tramo de dominio correcto no es trivial: el código de Tramo es
  sintetizado L1..V6 por aTramos, la entidad TramoSemanal no lo porta (ver D27). El
  test CatalogoMapperProblemaTest fija el vacío como contrato (centinela): cuando
  se materialice la entidad, ese aserto salta y obliga a revisitar esta decisión
  conscientemente. Asignación: Fase 6, bloque futuro.
  CIERRE (S51, Bloque 7): materializada la entidad ProfesorRestriccionHoraria
  (§4.3) con su repositorio y CatalogoMapper.aRestriccionHoraria; aProblemaHorario
  la ensambla. El tramo se resuelve por referencia de objeto (IdentityHashMap
  TramoSemanal→Tramo construido en aTramosConIndice), NO por el código sintético
  L1..V6, de modo que D27 NO se reabre. El centinela saltó como estaba previsto y
  pasó a verificar el round-trip real. Queda VIVA solo la parte de D21 (el campo
  peso por-restricción sigue sin consumirse en el objetivo; el término blando usa
  la constante PESO_INDISP_BLANDA=1); y el default 1 de §4.3 NO se materializa en
  la entidad (exige peso explícito): es política de la capa de configuración/UI
  (Fase 8), no dato de la entidad.
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
