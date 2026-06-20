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
- [ ] Puedes bloquear manualmente un tramo y el solver lo respeta
      — DIFERIDO: no existe mecanismo de bloqueo manual de tramo en el
      modelo actual. Requiere trabajo estructural propio (DTO + schema +
      dominio + restricción de pin/prohibición de tramo). Movido a Fase 4
      o commit estructural intercalado. No es validación de lo construido
      en Fase 3, sino funcionalidad nueva.

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

> **Estado (S23):** fase EN CURSO, subdividida en bloques internos (como Fase 2).
> Bloques 1-5 CERRADOS — ver "### Bloques de Fase 5" y los registros de Sesiones
> 19-23. Ninguno de los criterios de verificación de abajo está cerrado. Hay DOS
> razones distintas, según el criterio:
>   - Criterios 1 y 2 (tiempo < 10 min, 0 duras): faltan porque exigen el
>     INSTITUTO COMPLETO (≈28 grupos + Bach + FPB), aún no abordado. Lo medido
>     hasta ahora son escalones (máx. 11 grupos). Hay evidencia parcial
>     acumulada: curva de escala (criterio 1, factibilidad pura) con TRES puntos
>     — 7 grupos → 0,317 s, 10 grupos → 0,469 s, 10 grupos + 1 grupo Di →
>     0,408 s; tramo holgado, crecimiento ~lineal, sin salto de régimen aún.
>     Criterio 2: 0 duras en cada escalón (verde acumulado).
>   - Criterios 3 y 4 (calidad comparable al real, profesor sin ventanas
>     excesivas): el solver YA tiene régimen de optimización desde S24
>     (Bloque 6a): `SolverHorario.resolverOptimizando` minimiza una función
>     objetivo de penalizaciones blandas. El primer término es las ventanas
>     (huecos) del profesorado. El MECANISMO del criterio 4 está implementado
>     y validado en discriminación (S24), pero el criterio sigue SIN cerrar por
>     dos razones: (a) "excesivas" exige un umbral que no se fija sin datos del
>     centro ni del instituto completo (decisión consciente S24); (b) solo está
>     probado en un fixture de discriminación, no a escala. El criterio 3
>     (calidad) necesita más términos blandos (distribución, primeras/últimas
>     horas) aún no implementados. D11 (preferencias horarias) es el Bloque 6b,
>     donde además vivirá la comprobación de oro fuerte de las ventanas (un
>     hueco inevitable solo es construible con indisponibilidades de profesor).
> Naturaleza de los bloques cerrados: Bloque 1 fue prerrequisito de tipología
> (Tipo 4); Bloque 4 fue estructural (Lectura B, Tipo 7); Bloques 2, 3 y 5 son
> escala (no cierran criterios). El Tipo 5 (Diversificación/PDC) quedó validado a
> ESCALA en el Bloque 5 (S23), reforzando lo cerrado en Fase 4.

### Lo que se añade
- Todos los grupos restantes: 2ºESO, 3ºESO, 4ºESO, 1ºBach, 2ºBach, FPB
- Bachillerato con optativas compartidas entre grupos
- FPB con bloques de 2 horas consecutivas
- Atención Educativa (ATED/ATEDU) simultánea

### Criterios de verificación
- [ ] El solver termina en menos de 10 minutos para el instituto completo
- [ ] Cero restricciones duras violadas
- [ ] El horario generado es comparable en calidad al horario real de los PDFs 
      (no necesita ser idéntico, pero debe ser razonable)
- [ ] Un profesor con muchos grupos (ej. REL1, INF1, TEC3) tiene 
      un horario sin ventanas excesivas

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
- CRUD básico para todos los datos de configuración
- Guardado y carga de horarios generados

### Criterios de verificación
- [ ] Puedes cerrar la aplicación, volver a abrirla y el horario está intacto
- [ ] Puedes modificar un profesor y relanzar el solver sin perder otros datos
- [ ] La base de datos funciona en Windows sin instalación adicional
- [ ] El solver sigue funcionando con el mismo rendimiento que en Fase 5
      (señal de que el mapper no introduce regresiones)

---

## FASE 7 — UI: visualización de horarios
**Objetivo:** Ver los horarios generados en pantalla de forma útil.

### Entregable
Interfaz con las tres vistas: por grupo, por profesor, por aula.

### Criterios de verificación
- [ ] Las tres vistas muestran la misma información que los PDFs de ejemplo
- [ ] Los desdobles se visualizan correctamente (dos entradas en el mismo tramo)
- [ ] La vista de aula muestra correctamente los grupos que la usan
- [ ] La navegación entre grupos/profesores/aulas es fluida

---

## FASE 8 — UI: configuración y ajuste manual
**Objetivo:** El usuario puede configurar el centro y ajustar el horario 
generado manualmente.

> **Antes de empezar:** revisar `modelo_datos_fase1.md` §8 (deuda consciente), 
> especialmente **D1** (plantillas de generación de subgrupos) y **D7** (UX de 
> subgrupos compartidos entre particiones). Ambas afectan al diseño de los 
> formularios y al flujo de creación/edición de grupos y particiones.

### Entregable
- Formularios CRUD para todos los elementos de configuración
- Drag & drop para reubicar sesiones con detección de conflictos en tiempo real
- Posibilidad de bloquear sesiones antes de lanzar el solver
- Asistentes de creación rápida de grupos y subgrupos (deuda D1 de Fase 1)

### Criterios de verificación
- [ ] Al arrastrar una sesión a un tramo con conflicto, se muestra el conflicto 
      claramente (qué lo causa)
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

Fase actual: 5 — Solver: instituto completo (en curso, subdividida en bloques;
  Bloques 1-6a cerrados)
Última fase completada: 4 — Solver: grupos PDC/Diversificación
  (criterios 1-4 cerrados; validados con fixture real 3ºA/3ºADi)
Última sesión registrada: Sesión 24 — Fase 5, Bloque 6a cerrado (función
  objetivo: ventanas del profesorado). Cambio de RÉGIMEN del solver: de
  factibilidad pura a optimización, sin tocar el camino de factibilidad
  (`construir()` intacto; nuevo `construirConObjetivo()` y
  `resolverOptimizando()`). Penalización de ventanas modelada con Forma A
  (cotas tensadas: huecos = último − primero + 1 − nClases por profesor y día;
  el recreo no cuenta porque no es Tramo). Andamiaje genérico de términos
  ponderados, estrenado con un único término (peso constante, decisión 6a).
  `VerificadorSolucion.contarVentanasProfesor` recomputa el conteo de forma
  independiente del solver. Fixture de discriminación
  problema-6-ventanas-profesor.json (5 sesiones, 5 tramos, 1 grupo; óptimo 0
  alcanzable, espacio con hasta 3 ventanas) + SolverHorarioVentanasProfesorTest
  (2 casos: el optimizador elimina ventanas; el contador detecta ventanas en una
  colocación manual con huecos conocidos — comprobación de oro Opción I). Suite
  34 verde. Ningún criterio formal de Fase 5 marcado: el criterio 4 tiene
  mecanismo pero falta umbral y escala. Nueva deuda D17. Hallazgo de S24: un
  hueco inevitable (óptimo > 0) NO es construible sin indisponibilidades de
  profesor (Bloque 6b); la comprobación de oro fuerte estilo S9 se difiere a 6b.

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
- [ ] (pendientes de definir) 4ºESO con PDC nuevo (4ºADi/4ºDDi; verificar si 4º
      ordinario es separable del Di, como se hizo con 3º); FPB (bloques 2-3
      tramos, D12; resolver antes el hueco de aulas FPB); Bachillerato a ESCALA
      (Lectura B ya soportada; falta incorporar grupos de Bach al fixture);
      EF con Gim/Pista a escala (D3/D4, donde se espera el salto de régimen).
      Orden a decidir.

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
| Backend | Spring Boot + Java 17 |
| Base de datos | SQLite + Hibernate |
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
  resulta insuficiente
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
- **D13**: el IntVar de tramo usa índice plano [0, |tramos|). Con
  duracionTramos > 1, un IntervalVar podría cruzar la frontera entre el
  último tramo de un día y el primero del siguiente. En Fase 2 toda
  duración es 1, no aplica. Fase 5 (bloques de 2-3 tramos de FPB): el
  modelo de intervalos debe impedir el cruce de día.
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
  del signo del objetivo. Se verá venir al introducir el segundo término (6b+).
  No afecta al verificador independiente (`contarVentanasProfesor` cuenta sobre
  la solución concreta, sin cotas). Revisar al añadir términos que compitan.

### Notas técnicas validadas en Fase 0

- OR-Tools CP-SAT en Java funciona en Windows sin recompilar desde Linux
- El jar de ortools-java incluye los nativos Windows (.dll) embebidos; 
  Loader.loadNativeLibraries() los extrae en runtime automáticamente
- jpackage debe ejecutarse SIN --add-modules para incluir el JRE completo;
  con módulos mínimos falla por dependencias transitivas de OR-Tools/Protobuf
- Bundle resultante: ~200-250MB. Optimización de módulos diferida a Fase 11
- Distribución: zip del app-image. Sin permisos de administrador en Windows

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

### Sesión 10 — Fase 2, Bloque 3 completado.

Carga del ProblemaHorario desde JSON. Entregado: schema JSON de contrato (problema-horario.schema.json, draft 2020-12, no validado en runtime), 9 DTOs en es.yaroki.educhronos.solver.io, ProblemaInvalidoException, ProblemaHorarioMapper (DTO→dominio, dos pasadas, resolución de referencias por código de negocio, verificado contra los 13 POJOs reales), ProblemaHorarioJsonLoader (única clase con dependencia Jackson), fixture problema-minimo.json (3 ordinarias + 1 co-docencia LCL) y ProblemaHorarioJsonLoaderTest (1 caso válido + 5 negativos). Dependencia añadida: Jackson 2.21.3 en el módulo solver. Integrados SonarQube (sonar-maven-plugin 5.6.0.6792) y JaCoCo (0.8.14) en el parent pom.

### Sesión 11 — Fase 2, Bloque 4 completado.

Construcción del modelo CP-SAT. Entregado el paquete es.yaroki.educhronos.solver.cpsat: InstanciaProgramada (envuelve ActividadInstancia + IntVar de tramo + IntervalVar), Expansion (Actividad→instancias, compartida por modelo y verificador), ModeloCpSat, SolverHorario (fachada pública, carga de nativos en bloque estático), HorarioInfactibleException, ResultadoVerificacion, VerificadorSolucion y SolverHorarioTest (4 tests, JUnit5+AssertJ). Restricciones duras de Fase 2 implementadas: tres addNoOverlap (profesor, aula, subgrupo) y distribución por día para actividades DISTRIBUIDA. La distribución se modela SIN addElement: un BoolVar enDia por (instancia, día) reificado iff con addLinearExpressionInDomain sobre Domain.fromValues, más addAtMostOne por (actividad, día) — elegido por robustez frente a la versión de OR-Tools. VerificadorSolucion es verificación independiente del solver, sin dependencia de OR-Tools, reutilizable en el Bloque 5. Fixture de resolución problema-solver-minimo.json: sintético, 12 tramos / 3 días, 2 subgrupos, diseñado para ejercitar las cuatro restricciones de forma independiente (MAT8 y A5 compartidos entre dos subgrupos; LEN2 compartido entre la co-docencia LCL-1A y Ref-1B). El fixture del Bloque 3 (problema-minimo.json) es infactible como entrada de solver y se conserva solo para el test del loader. Dependencias añadidas al módulo solver: ortools-java 9.11.4210 y assertj-core. Notas técnicas: confirmada la firma newFixedSizeIntervalVar(LinearArgument, long, String) en ortools-java 9.11.4210 (resuelve una incertidumbre que se había flagueado). VerificadorSolucion vive en el paquete cpsat por simplicidad; candidato a paquete propio verificacion en Fase 5, ya que no arrastra dependencias de OR-Tools.

### Sesión 12 — Fase 2, Bloque 5 completado.

Output a consola. Entregado el paquete es.yaroki.educhronos.solver.cli:
Main (entrada con args[]→ejecutar(args,out,err) para testabilidad), CodigoSalida
(enum OK=0, INFACTIBLE=1, ENTRADA_INVALIDA=2, VIOLACIONES_DURAS=3),
SesionMaterializada (record interno de presentación: tramo + instancia + plaza),
Materializador (aplanado SolucionHorario → List<SesionMaterializada>,
N-correcto sobre plazas por actividad desde Fase 2 aunque solo se ejercite N=1),
VistaHorario<K> (interfaz genérica de vista), VistaPorGrupo, VistaPorProfesor,
FormatoCelda ("Asignatura·Profesores·Aula" con '+' separando profesores en
co-docencia), HorarioPrinter (plantilla genérica sub-tabla 6×5 con anchos
dinámicos), VerificacionPrinter (contador + lista plana de violaciones),
y MainTest (6 tests JUnit5+AssertJ: args inválidos, fichero inexistente,
fixture mínimo end-to-end, presencia de cabeceras y códigos clave).

Decisiones del bloque:
- Output siempre con dos vistas (grupo + profesor). Vista por aula descartada
  para el MVP; la plantilla VistaHorario<K> la soporta sin cambios cuando entre.
- Vista por grupo agrupada por GrupoAdministrativo (no por Subgrupo).
- Argumento posicional único: `java -jar solver.jar <ruta-al-problema.json>`.
  Sin librería CLI (picocli, etc.) hasta que haya 3-4 flags reales.
- Códigos de salida 0/1/2/3 con semántica fija. 3 es defensivo: solver
  FEASIBLE + violaciones del verificador = bug del modelo CP-SAT.
- Recreo no se imprime: Tramo solo lleva ordenEnDia 1..6 sin hora real;
  insertar columna "RECREO" entre T3 y T4 sería conocimiento del centro de
  referencia. La cabecera muestra T1..T6 y el usuario reconoce la jornada.
- Formato de celda con '·' (U+00B7) y PrintStream UTF-8 explícito.
- Separadores de tabla en ASCII puro ('|', '-', '+') para portabilidad.
- No fat-jar en Fase 2: ejecución vía `mvn exec:java` o classpath manual.
  El maven-shade-plugin entrará en Fase 11 (empaquetado Windows).

API del loader confirmada en esta sesión: ProblemaHorarioJsonLoader.cargar(InputStream),
no Path. Main abre el InputStream con try-with-resources sobre Files.newInputStream.

Tres warnings de build del Bloque 4 limpiados de paso: assertj-core duplicado
en el pom del módulo solver y maven-jar-plugin sin versión.

### Sesión 13 — Fase 2 cerrada. Bloque 6 completado.

Cierre del Bloque 6 con el dataset real de 1ºESO ordinarias + co-docencia
LCL de los 4 grupos (A, B, C, D). Con esto se cierran los 7 criterios de
verificación de Fase 2 y los 3 criterios del Bloque 6.

Entregado:

- `solver/src/test/resources/fixtures/problema-1eso-ordinarias.json`:
  fixture conforme al schema (validado), 4 grupos, 4 subgrupos
  `*-Completo`, 16 profesores, 5 aulas, 9 asignaturas, 30 tramos,
  36 actividades, 100 actividad-instancias semanales. Holgura por grupo:
  5 huecos. Profesor más cargado: MAT8 con 17.
- `SolverHorario1EsoOrdinariasTest` (paquete `cpsat`): carga el fixture,
  resuelve y reutiliza `VerificadorSolucion` para afirmar 0 violaciones
  de las cuatro restricciones duras. `@Timeout(60 s)`; pasa muy por
  debajo del límite.
- `Main1EsoOrdinariasTest` (paquete `cli`): ejecuta `Main.ejecutar`
  end-to-end y verifica código de salida OK=0, marcador "Violaciones"
  en stdout y stderr vacío. `@Timeout(60 s)`.

Diseño del dataset:

- Regla de filtrado aplicada: una sola aula por celda del horario del
  grupo y todos los profesores cubriendo el grupo completo (Hallazgo J).
  Quedan fuera desdobles (CyR), agrupamientos transversales
  (RefMt, Fr2/ALCT, OyD) y Religión multi-grupo + ATED.
- Inventario revisado en sesión cruzada de Claude Opus 4.7 sin contexto
  previo para neutralizar el sesgo de continuación. Veredicto "apto con
  correcciones puntuales" aplicado: `duracionTramos=1` explícito en las
  36 actividades; `requiereTutor` no se emite (no existe en el schema
  actual; la intención queda registrada en el modelo de datos).

Cambios documentales aplicados antes de la transcripción del dataset:

- `plan_trabajo_horarios.md`: D8 ampliada con cuatro puntos operativos,
  añadiendo la omisión sistemática de co-profesores en el horario por
  aulas (verificado en 16 celdas de LCL de 1ºESO) y la reverificación
  de la inconsistencia EFI2/EFI3 (no se reproduce en los PDFs actuales
  del proyecto; las 12 sesiones de EF de 1ºESO salen EFI2 en ambos
  listados).
- `modelo_datos_fase1.md` §8: D8 sincronizada con la versión del plan.
- `modelo_datos_fase1.md` §2: nuevo Hallazgo K — bloques de asignaturas
  alternativas intra-grupo no son siempre transversales. Tres patrones
  conviven en 1ºESO: CyR/OyD/RefMt transversal sobre N grupos,
  Religión/ATED transversal por parejas, Fr2/ALCT per-grupo.
- `modelo_datos_fase1.md` §6.1: reescritura del bloque Fr2/ALCT como
  cuatro actividades independientes per-grupo con sus cuatro particiones
  correspondientes.

Trabajo lateral identificado, sin impacto en Fase 2 y pendiente para Fase 3:

- Modelo §6.1 Religión/ATED de 1ºA está descrito como per-grupo, pero los
  PDFs muestran transversalidad por parejas con Religión multi-grupo
  (A+B en A5 jueves 12:30; C+D en A3 martes 13:30). Corrección pendiente
  cuando se aborde Religión multi-grupo en Fase 3.

Aclaración documental aplicada en línea: el schema
`problema-horario.schema.json` sí estaba en el repo desde Sesión 10
(entrega correcta del Bloque 3). El intercambio inicial de esta sesión
dio lugar a una propuesta errónea de retirar la mención en el plan; se
retractó al confirmar la existencia del fichero.

### Sesión 14 — Fase 3, commit 1 completado (no-solape por grupo).

Apertura de Fase 3. Antes de tocar código se discutió el diseño con el código
real delante (no solo la documentación), y eso corrigió dos supuestos.

Corrección de alcance (la decisión de diseño de la sesión):

- El supuesto de que Fase 3 necesita "Subgrupo multi-grupo" era erróneo. Se
  separó en dos lecturas: (A) una plaza lista subgrupos de varios grupos
  —transversalidad—, y (B) un subgrupo cuya población son alumnos de varios
  grupos. El dataset de Fase 3 (CyR/RefMt/OyD de 1ºESO) es todo Lectura A, con
  subgrupos mono-grupo, y el schema + el modelo CP-SAT ya la soportan. La
  Lectura B (subgrupo multi-grupo, `SubgrupoGrupo` N:M) es Fase 5 (optativas de
  Bachillerato). Frontera Fase 2→3 corregida en la deuda.

Hallazgo del código (lo que Fase 3 SÍ añade y no estaba en el plan):

- Al partir un grupo en varios subgrupos, el no-solape por subgrupo (S3) deja
  de garantizar I1: el solver no sabe que `1ºA-Completo` y `1ºA-CyR-Tec` son el
  mismo grupo, porque las particiones no viajan en el JSON del solver. Hace
  falta una restricción dura nueva, no prevista en el plan: no-solape por
  grupo (S9). El bloqueo de los 4 grupos por el bloque CyR/OyD/RefMt depende de
  ella; con solo S3 no ocurre.

Confirmado leyendo el código real (no inferido de la documentación):

- La variable de tramo es por `ActividadInstancia` (un `IntVar` + un
  `IntervalVar` en `InstanciaProgramada`), no por plaza. Las plazas cuelgan
  del dominio. Consecuencia: la simultaneidad de las N plazas de una actividad
  (los desdobles/agrupamientos) es ESTRUCTURAL y gratis —comparten el mismo
  intervalo, el solver no puede separarlas—. El criterio de fallo de Fase 3
  ("el solver mete las sesiones en tramos distintos para evitar el conflicto")
  es imposible por construcción. Justifica CP-SAT sobre Timefold en la práctica.
- `cubreSubgrupo`/`usaProfesor`/`usaAula` ya recorren TODAS las plazas de la
  actividad y añaden el intervalo una sola vez. La transversalidad
  (plaza con N subgrupos) no requirió tocar las restricciones existentes.
- `Subgrupo` del dominio tiene referencia directa a `GrupoAdministrativo`
  (no por código); S9 puede leer `subgrupo.grupo()` sin tocar dominio ni mapper.
- El mapper NO valida cobertura (I1) ni disjunción de subgrupos a nivel de
  grupo; I2 se valida solo POR ACTIVIDAD. Por eso el fixture infactible (dos
  subgrupos solapados del mismo grupo en actividades distintas) carga sin error
  y la infactibilidad emerge en el solver, no en la carga.
- `ProblemaHorarioJsonLoader` activa `FAIL_ON_UNKNOWN_PROPERTIES`. Los fixtures
  NO pueden llevar claves extra (un campo `_comentario` rompería la carga).

Entregado:

- `ModeloCpSat`: restricción dura `restriccionNoSolapeGrupo()` + helper
  `tocaGrupo()`, gemela del no-solape por subgrupo, agrupa por
  `subgrupo.grupo()`, ciega al `grupoPadre`. Import nuevo de
  `GrupoAdministrativo`. No se elimina ninguna restricción; coexiste con el
  no-solape por subgrupo (que sigue siendo necesario para subgrupos en varias
  particiones / subgrupo multi-grupo futuro).
- Fixtures sintéticos en `solver/src/test/resources/fixtures/`:
  `problema-noSolapeGrupo-factible.json` (2 tramos, dos actividades que tocan
  1A por subgrupos distintos con profesores y aulas fijas distintas → solo S9
  puede separarlas) y `problema-noSolapeGrupo-infactible.json` (idéntico con un
  único tramo → S9 lo vuelve infactible).
- `RestriccionNoSolapeGrupoTest` (paquete `cpsat`): caso factible (0
  violaciones del verificador + aserción explícita de tramos distintos) y
  contra-test infactible (espera `HorarioInfactibleException`).
- Suite completa del módulo en verde (20 tests). `SolverHorario1EsoOrdinariasTest`
  sigue pasando: la restricción nueva no rompe el dataset real de Fase 2
  (todo `*-Completo`, donde grupo ⟺ subgrupo).

Comprobación de oro realizada: comentando la llamada a
`restriccionNoSolapeGrupo()`, AMBOS tests fallan (el infactible deja de lanzar
la excepción; el factible coloca las dos actividades en el mismo tramo). Esto
demuestra que S9 es la causa del comportamiento correcto y que los fixtures la
aíslan. Detalle revelador: el `VerificadorSolucion` NO protestó en el caso
degradado —no comprueba solape de grupo—; solo la aserción explícita de tramos
distintos lo detectó. De ahí la deuda D14.

Estado de los 5 criterios de verificación de Fase 3: ninguno cerrado todavía.
El mecanismo del bloqueo simultáneo de los 4 grupos (criterios 2 y 3) ya está
en su sitio (S9 + variable de tramo por instancia), pero se valida con el
fixture real de CyR/RefMt en el cierre de fase, no con los sintéticos del
commit 1.

Pendiente para las siguientes sesiones de Fase 3, en orden:

1. Commit intermedio (Sesión 15): tapar el agujero del `VerificadorSolucion`
   (deuda D14). Añadir verificación independiente de solape de grupo, gemela
   de la de subgrupo. Lógica pura sin OR-Tools.
2. Commit 2: `aulasCandidatas` con intervalos opcionales (el grueso de la
   fase). Toca `mapearPlaza` (dejar de rechazar candidatas), `ModeloCpSat`
   (`newOptionalIntervalVar` + `addExactlyOne` + bifurcación de `usaAula`) y
   posiblemente la estructura de `InstanciaProgramada`.
3. Cierre de fase: fixture real con el desdoble de CyR y el agrupamiento de
   RefMt de 1ºESO; valida commit 1 + commit 2 juntos y los 5 criterios.
4. Antes de modelar Religión multi-grupo: corregir §6.1 (Religión/ATED de 1ºA
   descrito como per-grupo; los PDFs muestran transversalidad por parejas).
   Trabajo lateral heredado de Sesión 13.

### Sesión 15 — Fase 3, commit intermedio: verificador de no-solape por grupo (cierra D14).

Commit acotado, sin cambios de firma pública. Antes de tocar código se leyó el
código real del verificador y de los records que consume (no la documentación),
y eso corrigió un supuesto del diseño del test.

Decisión de diseño (verificación):

- La comprobación de grupo encaja como cuarto `Map<GrupoAdministrativo, Integer>`
  dentro de `verificarNoSolapes`, reutilizando `reportarColisiones`, no como
  método aparte. Es el gemelo estructural de los conteos de profesor/aula/
  subgrupo, fiel a cómo S9 modela el solver: agrupa por `subgrupo.grupo()`,
  ciego al `grupoPadre`.
- El conteo de grupo se deriva de un `Set<GrupoAdministrativo>` construido a
  partir del `Set` de subgrupos POR INSTANCIA. Consecuencia: un desdoble (una
  sola actividad con N plazas, N subgrupos del mismo grupo) colapsa a un único
  grupo en el conteo de esa instancia → cuenta 1 → no se reporta. El colapso es
  gratis, por la misma mecánica que protege a los otros tres recursos.

Corrección de un supuesto inicial (registrada porque cambió el test):

- El primer diseño de test factible ("un desdoble no debe reportarse") no
  protegía nada: el colapso por Set-por-instancia hace que ese caso pase
  SIEMPRE, verifique o no el grupo correctamente. El solape de grupo que S9
  detecta —y que el verificador debe detectar— es el de DOS actividades-
  instancia distintas que tocan el mismo grupo en el mismo tramo. El test
  infactible se rehízo sobre ese caso.

Entregado:

- `VerificadorSolucion.verificarNoSolapes`: cuarto conteo por grupo +
  `reportarColisiones("Grupo", …)`. Import de `GrupoAdministrativo`. Javadoc de
  clase actualizado: deja de decir "restricciones duras de Fase 2" y enumera
  "profesor, aula, subgrupo y grupo", con nota de ceguera al `grupoPadre`.
  Sin cambios de firma.
- `VerificadorSolucionGrupoTest` (paquete `cpsat`), clase nueva, 3 tests sobre
  `SolucionHorario` fabricada a mano (sin pasar por el solver): solape real
  entre actividades distintas (reporta), tramos distintos (no reporta), desdoble
  como regresión del colapso por instancia (no reporta). Separada de
  `RestriccionNoSolapeGrupoTest` a propósito: aquélla prueba el solver, ésta el
  verificador.
- Corregido de paso un desfase preexistente en `referencia-codigo-solver.md`
  (flujo principal): `ModeloCpSat.construir()` aplica cinco restricciones duras
  (la quinta, no-solape por grupo, entró en Sesión 14), no cuatro. El índice
  arrastraba el conteo antiguo.

Comprobación de oro realizada: comentando el conteo de grupo en el verificador,
el test `reportaSolapeDeGrupoEntreActividadesDistintas` falla (la lista de
violaciones queda vacía) y los otros dos siguen en verde. Demuestra que el test
depende de la lógica que protege.

Suite completa del módulo en verde (23 tests; 20 previos + 3 nuevos).
`RestriccionNoSolapeGrupoTest` sigue pasando: el fixture factible de S9 no
escondía ningún solape de grupo latente que el verificador, ahora más estricto,
pudiera destapar.

Estado de los 5 criterios de verificación de Fase 3: sin cambios respecto a
Sesión 14 (ninguno cerrado todavía; se cierran con el fixture real de CyR/RefMt
al final de la fase). Este commit no toca criterios: completa la red de
seguridad independiente antes del commit 2 (aula variable).

### Sesión 16 — Fase 3, commit 2: aulasCandidatas con intervalos opcionales.

El grueso de la fase. Hasta aquí toda `Plaza` usaba `aulaFija` y el cargador
RECHAZABA cualquier dataset con `aulasCandidatas` no vacío. Este commit levanta
esa restricción y enseña al solver a ELEGIR aula entre varias candidatas,
garantizando no-solape sobre el aula efectivamente elegida (S2). Commit único,
estructural (firmas nuevas/cambiadas). Antes de tocar código se leyó el código
real de cada fichero y se verificaron las firmas de OR-Tools 9.11 contra el
Javadoc, no de memoria.

Decisiones de diseño (con el código delante):

- El intervalo opcional de aula NO lleva variable de tramo propia: su `start`
  es el mismo `IntVar tramoIndex` de la instancia (CP-SAT admite reutilizar un
  `IntVar` como `start` de varios intervalos). El aula es lo único que varía por
  candidata; el tramo ya está fijado por la instancia y compartido por las N
  plazas del desdoble. Atarlo así evita el problema de "soluciones duplicadas"
  de los intervalos opcionales con start libre (issue google/or-tools#3605).
- `addExactlyOne` es POR PLAZA, no por actividad: cada plaza con candidatas
  elige su aula independientemente. En un desdoble con dos plazas variables,
  cada una resuelve su propio `addExactlyOne`, ambas en el mismo tramo.
- Dónde viven los intervalos opcionales: ampliando `InstanciaProgramada` con un
  `Map<Plaza, List<AulaOpcion>>` (opción A sobre estructura aparte), con un
  `record AulaOpcion(Aula, BoolVar presencia, IntervalVar)` paquete-privado.
  `extraerSolucion` y `restriccionNoSolapeAula` leen las opciones del mismo
  objeto que ya recorren.
- `SolucionHorario` transporta el aula elegida en un segundo mapa
  `Map<ActividadInstancia, Map<Plaza, Aula>>`, retrocompatible: el constructor
  de un argumento se mantiene (delega con mapa vacío), así los tests que
  fabrican soluciones a mano no se tocan. Accessor unificado
  `aulaElegida(inst, plaza)`: devuelve la elegida si la plaza es variable, o su
  `aulaFija` si es fija. Punto único de verdad para verificador y (futuro)
  materializador.
- `restriccionNoSolapeAula` mezcla, por aula, los intervalos fijos (como antes)
  y los intervalos opcionales cuyas candidatas apuntan a esa aula (Forma 1:
  bucle externo por aula, simétrico a los otros tres no-solapes). `addNoOverlap`
  admite la lista mezclada; los opcionales no presentes no restringen.

Confirmado leyendo el modelo autoritativo (resolvió una pregunta de diseño):

- Pregunta: ¿dos plazas de la misma actividad pueden compartir aula
  legítimamente? Respuesta inequívoca por S2 + glosario de
  `modelo_datos_fase1.md`: NO. El uso compartido de aula (Tipo 4, Religión
  multi-grupo) se modela como UNA plaza con varios profesores/subgrupos, no como
  varias plazas con un aula. Cuando una actividad tiene varias plazas son
  sesiones físicas distintas con aulas distintas (desdoble). Por tanto, dos
  plazas de la misma instancia con la misma aula en el mismo tramo es colisión
  (viola S2), no uso compartido. Origen de la deuda D15.

Entregado:

- `AulaOpcion` (nuevo, `cpsat`): `record` con aula + literal de presencia +
  intervalo opcional.
- `InstanciaProgramada`: campo `Map<Plaza, List<AulaOpcion>> opcionesDeAula`
  (solo plazas con candidatas; vacío si ninguna), copia defensiva anidada,
  constructor de 4 argumentos. Cambio de firma.
- `ModeloCpSat`: helper `crearOpcionesDeAula` (intervalos opcionales +
  `addExactlyOne` por plaza), `restriccionNoSolapeAula` mezcla fijos+opcionales
  con helper `intervalosOpcionalesEn`, `extraerSolucion` lee el aula elegida vía
  `solver.booleanValue(presencia)` y la pasa al constructor de 2 args de
  `SolucionHorario`. Javadoc de la rama de aula y comentario de `usaAula`
  actualizados.
- `SolucionHorario`: segundo constructor + accessor `aulaElegida`. Constructor
  de 1 arg retrocompatible. Cambio de API pública (ampliación, no ruptura).
- `VerificadorSolucion.verificarNoSolapes`: lee `solucion.aulaElegida(inst,
  plaza)` en vez de `plaza.aulaFija()`. Comentario del Set-por-instancia
  ampliado para documentar la debilidad de aula (D15) en el propio código.
- `ProblemaHorarioMapper.mapearPlaza`: elimina el rechazo de `aulasCandidatas`;
  las resuelve a `Set<Aula>` con `LinkedHashSet` (dedupe en silencio, como
  profesores/subgrupos). El XOR lo sigue validando el constructor de `Plaza`.
  Javadoc de clase actualizado.
- `SolverHorarioAulaCandidataTest` (nuevo, `cpsat`): tres tests — candidatas con
  aula fija rival (elige la libre, A6), desdoble mixto fija+candidatas en el
  mismo tramo (la variable elige A6; ejercita la rama de no-solape que mezcla
  intervalo fijo y opcional), e infactible por candidata única compartida. Los
  dos factibles afirman el aula elegida explícitamente, no solo 0 violaciones.
- `ProblemaHorarioJsonLoaderTest`: el test `rechazaAulasCandidatasHastaFase3`
  (obsoleto: verificaba la restricción que este commit elimina) reemplazado por
  `cargaAulasCandidatasResueltas`, que verifica que el mapper carga y resuelve
  las candidatas a entidades de dominio. Lo cazó la suite, no el análisis previo:
  al eliminar una validación hay que buscar el test que la afirmaba.
- Tres fixtures `problema-aulaCandidata-{factible,mixta,infactible}.json`.

Schema y DTO NO requirieron cambios: `problema-horario.schema.json` ya declara
`aulasCandidatas` como propiedad válida y `PlazaDto` ya la expone (se leía para
rechazarla). El schema no valida el XOR; lo valida `Plaza` en runtime.

Suite completa del módulo en verde (26 tests; 23 previos + 3 nuevos; un test
del loader reemplazado, no sumado).

Estado de los 5 criterios de verificación de Fase 3: ninguno cerrado todavía. El
solver ya elige aula entre candidatas (mecanismo del commit 2 en su sitio), pero
los criterios se cierran con el fixture real de CyR/RefMt al final de la fase.
Pendiente, en orden: cierre de fase (fixture real, valida commit 1 + commit 2 +
los 5 criterios), y antes de Religión multi-grupo, corregir §6.1 (Religión/ATED
descrito como per-grupo; los PDFs muestran transversalidad por parejas).

### Sesión 17 — Fase 3 cerrada. Fixture real CyR/OyD/RefMt + criterios 1-4.

Cierre de fase, no commit estructural. Antes de diseñar nada se leyó el
código real (VerificadorSolucion, ModeloCpSat, los tests de S16 y Fase 2,
el loader) y el schema; eso fijó tres decisiones y descubrió un matiz.

Decisiones de diseño (con el código delante):
- Alcance: cierre de 4 criterios + C5 diferido. C5 (bloqueo manual de
  tramo) no tiene mecanismo en el modelo; implementarlo es trabajo
  estructural, no validación de Fase 3. Decidido con el usuario.
- Fixture nuevo e independiente (no se acopla al de Fase 2). Bloque de 6
  plazas literal de §6.1 + 4 Mat testigo. Aulas de los testigos aisladas
  (A1,A2,A4,A7) de las candidatas del bloque: el único acoplamiento
  testigo↔bloque es por grupo (S9), para aislar la causa del C2.
- Holgura amplia a propósito (testigos de 3 reps): el cierre valida el
  resultado, no estresa S9 (su estrés ya está en RestriccionNoSolapeGrupoTest,
  S14). Decidido con el usuario.

Matiz descubierto leyendo el código (no la doc):
- C1 es estructuralmente trivial: las 6 plazas comparten tramo por ser una
  Actividad (tramo por instancia, no por plaza). No se puede "leer el tramo
  de CyR-TEC3 vs CyR-INF1" por separado. La evidencia honesta es "por
  construcción", y así se registra. La señal de alarma del plan (el solver
  separa en tramos distintos para evitar el conflicto) NO puede ocurrir
  intra-actividad.
- Confirmado en ModeloCpSat: las 4 restricciones de no-solape iteran sobre
  TODAS las instancias sin excluir las de la misma actividad, luego las 2
  instancias del bloque (que comparten INF1, A12In, subgrupos) se separan
  por S1/S2/S3/S9. Esto valida usar NEUTRA en el bloque.

Sobre D15: el fixture la toca de lleno (3 plazas RefMt de la misma
instancia con candidatas solapadas). El verificador no detectaría una
colisión de aula intra-instancia. Se tapó con aserción explícita de aulas
distintas en el test; D15 NO se cierra en código (sigue abierta). D16 no
se tocó.

Entregado:
- solver/src/test/resources/fixtures/problema-3-cierre-cyr-refmt.json:
  4 grupos, 28 subgrupos (4 Completo + 24 transversales), 10 profesores,
  11 aulas, 4 asignaturas, 30 tramos, 5 actividades (bloque de 6 plazas +
  4 Mat testigo). Validado contra schema, XOR, I2 e integridad referencial.
- SolverHorarioCierreFase3Test (cpsat): un test con sanity check del
  dataset + 0 violaciones del verificador + aserciones de C1-C4. @Timeout 120s,
  resuelve en ~30ms.
- Suite: 27 tests en verde (26 + 1). Sin cambios de src/main: el índice de
  código NO se regenera.

Trabajo lateral heredado (Religión/ATED §6.1 per-grupo vs transversal por
parejas): NO tocado. Pertenece a antes de Fase 4 (Religión multi-grupo,
Tipo 4). Sigue pendiente.

### Sesión 18 — Fase 4 cerrada. Fixture real PDC 3ºA/3ºADi + criterios 1-4. D15 cerrada.

Cierre de fase con un commit estructural intercalado (D15), decidido con el
usuario antes de tocar nada. Antes de diseñar se leyó el código real
(ModeloCpSat S2/S9, VerificadorSolucion, ProblemaHorarioMapper, Plaza, Aula,
GrupoAdministrativo) y el schema; el cruce de los PDFs de 3ºA ordinario
(pág. 8) y 3ºA PDC (pág. 9) fijó qué sesiones son compartidas y cuáles propias.

Decisiones de alcance (con el usuario):
- Orden de la sesión: candidato 1 (PDC/3ºADi), por ser el objetivo de la fase
  y por ejercitar por primera vez con dataset real la ceguera de S9 al
  grupoPadre. Candidatos 2 (C5 bloqueo de tramo) y 3 (§6.1 Religión/ATED
  transversal) NO abordados; siguen pendientes.
- D15 cerrada en código en esta fase (no diferida). Vía A para la cara de
  configuración: validación en el mapper, sin tocar el modelo CP-SAT.
- Fixture recortado a 3ºA + 3ºADi. Rel/ATED dejado FUERA de Fase 4 (no aporta
  nada que EF/Tec no validen ya sobre la ceguera de S9); su transversalidad
  por parejas (A+B) se valida en Fase 5.
- Criterio 4 validado por ASERCIÓN PROGRAMÁTICA sobre la solución, no por
  inspección del CLI. La impresión del horario con marca de diversificación
  sigue sin construirse: es D16, fuera de Fase 4.

Hallazgo del cruce de PDFs (corrige el modelo, ver más abajo):
- Las sesiones compartidas reales de 3ºA/3ºADi son TUT3, EF, EPVA, Tec y el
  bloque Rel/ATED. El modelo §6.2 listaba solo "EF, EPVA, Tec": incompleto.
- Rel/ATED de 3ºA y de 3ºADi son el MISMO bloque (V 10-11, idéntico en ambos
  PDFs: REL1 B01 + FIL1 B05 + ING3 A7), no dos sesiones distintas.

D15 reformulada y cerrada (era de DOBLE cara, no solo verificador):
- Cara verificador: VerificadorSolucion contaba el aula por instancia (Set),
  enmascarando dos plazas de la misma instancia con la misma aula. Cerrada:
  el aula se cuenta por plaza. Profesor y subgrupo siguen por instancia
  (correcto: S1 permite un profesor en varias plazas; co-docencia es varios
  profesores en UNA plaza).
- Cara solver/config (NO documentada en la nota original de D15): la rama
  aulaFija de restriccionNoSolapeAula cuenta el intervalo por instancia, así
  que dos aulaFija idénticas en una instancia tampoco las prevenía el
  addNoOverlap. Cerrada por vía A: nueva validación de configuración
  verificarAulasFijasDisjuntas en ProblemaHorarioMapper (rechaza dos plazas
  de la misma actividad con la misma aulaFija antes del solver). No toca el
  modelo CP-SAT. No aplica a aulasCandidatas (el solver elige aulas distintas).

Entregables (commiteados):
- src/main: ProblemaHorarioMapper (método verificarAulasFijasDisjuntas + su
  llamada tras verificarI2) y VerificadorSolucion (aula por plaza).
- Fixture problema-4-pdc-3a-3adi.json (src/test/resources/fixtures): par
  3ºA/3ºADi, 2 días/12 tramos, 6 actividades, 14 instancias. Compartidas
  EF/Tec como una plaza con subgrupos de ambos grupos; propias Di (AmbSL/LEN2,
  AmbCM/MAT4 en A8); propias ordinarias (FQ, Mat en B01).
- SolverHorarioCierreFase4Test (paquete cpsat): criterios 1-4.

Validación del fixture en diseño (antes de enseñarlo, como en S17): valida
contra el schema; cumple invariantes de carga; factible (OPTIMAL); fuerza
≥1 tramo donde 3ºA y 3ºADi divergen en instancias distintas. PRUEBA NEGATIVA:
con S9 fundida al grupoPadre el problema es INFEASIBLE — el fixture discrimina
la ceguera, no solo la "pasa".

Suite: 28 tests verdes (los previos + SolverHorarioCierreFase4Test). Build
recompila 44 clases sin romper ninguno previo (incluido VerificadorSolucionGrupoTest,
que el cambio de conteo de aula podría haber afectado y no afectó).

Índice de código (referencia-codigo-solver.md): REGENERADO. Esta sesión cambia
src/main (mapper gana método privado nuevo; verificador cambia cuerpo, no firma).

### Sesión 19 — Fase 5, Bloque 1: Religión multi-grupo por parejas (Tipo 4).

Fase 5 subdividida en bloques internos (como Fase 2), decidido con el usuario:
no abordar la escala completa de golpe. Bloque 1 = validar el Tipo 4 (Religión
multi-grupo / actividad coordinada que bloquea varios grupos completos) ANTES de
escalar, por ser el último tipo de sesión transversal sin ejercitar en el solver.
La medición de tiempo de solver a escala (criterio 1 de Fase 5) se difiere a un
bloque posterior.

Antes de tocar nada se leyó el código real (ModeloCpSat: tocaGrupo/cubreSubgrupo
y las 5 restricciones; ProblemaHorarioMapper; Subgrupo y SubgrupoDto), lo que
confirmó dos cosas: (1) Tipo 4 es Lectura A (subgrupos mono-grupo en una plaza
que lista varios), no fuerza Lectura B; (2) un Subgrupo pertenece a UN solo grupo
(1:1), así que la notación de §6.4 con subgrupos multi-grupo no era representable.

Entregado:
- solver/src/test/resources/fixtures/problema-5-religion-parejas-1eso.json:
  4 grupos de 1ºESO, dos actividades NEUTRA (Relig_ATED-1AB y Relig_ATED-1CD),
  cada una con 3 plazas (Religión multi-grupo + ATED per-grupo), subgrupos
  mono-grupo; Mate testigo por grupo en aula aislada (A99). Fr2 de FRA1 NO
  incluidas (pertenecen a un bloque posterior). Validado contra schema, XOR, I2,
  integridad referencial, factibilidad y prueba de discriminación de S9.
- SolverHorarioReligionParejasTest (paquete cpsat): 0 violaciones del verificador
  + REL1 reparte AB/CD en tramos distintos (S1) + S9 expulsa las Mate del tramo
  de su bloque + Tipo 4 toca ambos grupos del par.
- Suite 28 → 29 en verde, BUILD SUCCESS (módulo y reactor). Sin cambios de
  src/main: el índice de código NO se regenera.

Validación del Tipo 4: prueba de discriminación de S9 en un escenario reducido
— sin S9 el solape grupo-bloque es FACTIBLE; con S9 es INFACTIBLE. Confirma que
es S9 (y solo S9) la que hace que un bloque multi-grupo bloquee los grupos
completos del par.

Procedencia de los datos (verificada, no OCR): las 6 plazas confirmadas plaza a
plaza contra el volcado fiel de los PDF. Par A+B jueves T5 (Relig REL1/A5 —1ºA y
1ºB comparten A5—; ATED 1ºA→ING6/A17, 1ºB→GH5/A11). Par C+D martes T6 (Relig
REL1/A3 —aula-A3.json lista "1ºC 1ºD" juntos—; ATED 1ºC→GH2/C00, 1ºD→FRA1/A14).
Cada grupo va a Religión en el aula del titular de su pareja.

Documentación de referencia (decidido con el usuario): extracción determinista
sin OCR de los 3 PDF a docs/horario-referencia/ (28 grupo-*.json + 43 aula-*.json
+ INFORME-RECONCILIACION.md + RESUMEN-EXTRACCION.md + README.md), versionada en
git. Jerarquía de autoridad: PDF → volcado fiel → modelo §6.x. Los volcados NO
están en el Project; se piden al usuario por nivel cuando se necesiten. Añadidas
3 líneas a las instrucciones del Project para que esto se sepa entre sesiones.

Correcciones de modelo aplicadas (modelo_datos_fase1.md): §6.1 (Religión/ATED de
1ºA pasa a vista parcial con nota de multi-grupo por parejas; deuda pendiente
desde Sesión 13, cerrada); §6.4 (notación Lectura B → Lectura A, con nota de que
el reparto de ATED de 3ESO es ilustrativo no verificado); Hallazgo K (ATED con
las 6 plazas verificadas de 1ºESO).

Hallazgos nuevos (para Fase 5, NO Bloque 1):
- FPB sin cobertura de aulas en el horario por aulas (65 celdas con aula omitida,
  informe §1). Decidir aulas de FPB por otra vía al modelar ese bloque.
- 3ºPDC ↔ 3ºCDi no mapeable de forma determinista (informe, nota de
  normalización). Reconciliación manual al abordar 3ºESO.
- Celda LU/LEN1 "1B-C 1B-D" en aula-A3.json: primer indicio en datos fieles de
  Lectura B real (subgrupo multi-grupo) en optativas de 1ºBachillerato. A
  resolver en el bloque de Bachillerato.

Deudas: D12, D3, D4, C5, D16 sin tocar (siguen abiertas para bloques posteriores
de Fase 5). §6.1 Religión/ATED per-grupo: CERRADA en esta sesión.

Observación S20 (escala 1º+2º): Gim alcanza 18 slots como recurso compartido
  inter-nivel (EF de 1º + 2º). Aún holgado (<30); no fuerza modelado dedicado
  todavía. Reevaluar al añadir más niveles con EF.

### Sesión 20 — Fase 5, Bloque 2: escala 1º+2º ESO (7 grupos). Medición de solver.

Bloque de ESCALA, no estructural. Objetivo: atacar el riesgo no medido del
proyecto (criterio 1 de Fase 5, <10 min) midiendo tiempo de solver sobre un
escalón mayor que todo lo anterior. Se eligió 2ºESO como primer nivel nuevo
(volumen, no estructura) frente a 4ºESO (que habría mezclado escala + PDC nuevo);
1ºESO se reconstruyó completo (no existía entero en un solo fixture, solo
troceado en fixtures de discriminación).

Decisiones de diseño:
- Linaje NUEVO de "fixture de escala" (problema-5-escala-instituto.json), distinto
  de los fixtures de discriminación (que NO se tocan y NO crecen). El de escala
  crece nivel a nivel hasta el instituto. Su test mide tiempo + 0 duras.
- Códigos de grupo normalizados sin "º" (1A..2C) para alinearse con los fixtures
  que el solver ya digirió y eliminar una variable no probada.
- Aulas variables de ordinarias de 2º (FyQ→{aula grupo, A6 laboratorio};
  Tec→{aula grupo, B07}) modeladas como aulasCandidatas (decisión confirmada).
- PEPA/Fr2/CyR de 2º: dos bloques rep=1 (mié y vie), NO una actividad rep=2,
  porque las instancias difieren en plazas (RefMt el miércoles, RefLe el viernes)
  y Actividad exige plazas idénticas entre repeticiones. La continuidad del
  alumno de optativa se modela con subgrupo de optativa COMPARTIDO entre ambos
  bloques (Hallazgo D / I6) + S3. La población real de cada subgrupo de refuerzo
  es una de las particiones posibles; a confirmar con el centro (salvedad análoga
  a la de ATED en §6.4).

Patrones nuevos de 2º respecto a 1º (todos Lectura A, sin Lectura B):
- Religión Tipo 4 multi-grupo a 3 grupos (Rel REL1/A1 + RelEV REV/A18 comunes a
  A/B/C; ATEDU per-grupo). En 1º era por parejas; aquí los 3 juntos.
- Asignaturas nuevas: FyQ, GeH, Tec, VEtic, PEPA, RefLe, ATEDU, RelEV.
- Confirmado que 2º NO necesita Lectura B (subgrupo multi-grupo); sigue siendo
  prerrequisito solo de Bachillerato.

Verificación (toda programática, contra volcado fiel docs/horario-referencia/):
- 1º (4 grupos) y 2º (3 grupos): 30/30 slots ocupados cada grupo; demanda
  curricular idéntica al volcado asignatura por asignatura.
- Fixture combinado válido: schema + XOR de aula por plaza (regla confirmada
  leyendo Plaza.java en S20: aulaFija XOR aulasCandidatas, ambas guardas) +
  I2 + integridad referencial. Comprobación manual de 2ºC cotejada celda a celda.
- Factibilidad necesaria (no suficiente): carga ≤30 por grupo/profesor/aula fija.
  Recursos inter-nivel ya cargados: Gim 18 slots, profesores-puente (TEC3, EFI2,
  MUS1, FRA1, REL1) entre 1º y 2º. Ninguno saturado de entrada.

Entregado y commiteado:
- solver/src/test/resources/fixtures/problema-5-escala-instituto.json: 7 grupos,
  37 profesores, 16 aulas, 27 asignaturas, 77 subgrupos, 76 actividades, 101
  plazas (10 con aulasCandidatas).
- SolverHorarioEscalaInstitutoTest (cpsat): wall-clock alrededor de resolver()
  con SolverHorario(600.0, 42); afirma <600 s y 0 violaciones duras. @Timeout 660 s.
- Medición: solución factible en 0,317 s. Suite 30 en verde, BUILD SUCCESS.

Lectura honesta del resultado: 0,317 s a 7 grupos descarta el escenario
catastrófico ("ya tarda minutos a 7 grupos"), pero NO permite extrapolar al
instituto completo (CP es no lineal; un punto no define curva). El problema actual
está holgado (recursos lejos de saturar), lo que favorece tiempos bajos; al
añadir niveles la holgura baja y el régimen puede cambiar. Criterio 1 de Fase 5:
primer punto de curva, NO cerrado. Criterio 2: evidencia parcial (0 duras a 7
grupos), NO cerrado (exige instituto completo).

src/main NO tocado → referencia-codigo-solver.md NO regenerado; sigue válido sobre
su commit hash. Modelo NO modificado: 3º no aportó capacidad estructural nueva
(reutiliza §6.4 y Hallazgo F); decisión consciente de no añadir §6.x, análoga a la
de S20 con 2º.

Hallazgo S21 (subgrupo ≠ alumno; precisión de deuda añadida en S22): al verificar
"subgrupos disjuntos del nivel" hubo que leer el cuerpo de VerificadorSolucion.java
(el índice de API y los nombres de test eran sugerentes pero contradictorios; no
bastaban). Confirmado en código: el verificador comprueba no-solape de Profesor,
Aula, Subgrupo y Grupo por tramo, pero el conteo de GRUPO usa un Set POR INSTANCIA
de actividad — varios subgrupos del mismo grupo dentro de UNA misma actividad
coordinada/desdoble colapsan a "grupo contado 1 vez", luego no hay violación (así
coexisten legítimamente las 6 plazas de las coordinadas de 3º). Consecuencia: el
dominio modela SUBGRUPOS, no ALUMNOS; no existe entidad Alumno. Que la unión de los
subgrupos de un grupo sea una partición real (disjunta y exhaustiva) de sus alumnos
NO lo verifica ningún componente — es invariante de población, responsabilidad del
constructor del fixture y a confirmar con el centro. Lo que SÍ se verifica: la
disjunción estructural intra-actividad (I2) y el no-solape de grupo entre
actividades distintas.

---

### Sesión 21 — Fase 5, Bloque 3: escala 3ºESO ordinario (10 grupos).

Bloque de ESCALA, no estructural. Tercer escalón del fixture de escala (que crece
in situ: NO se creó fichero nuevo, se amplió problema-5-escala-instituto.json).
Se eligió 3ºESO ordinario (3A–3C) frente a Lectura B (Bachillerato, estructural) y
FPB (cabo de datos: aulas no cubiertas en el horario por aulas), siguiendo el
principio de "una capa por bloque": seguir la curva de escala barata antes de
meter estructura nueva, decidido con datos.

Dictamen de separabilidad ordinario/Di (verificado contra volcado fiel ANTES de
construir): 3ºA–3ºC ordinario es separable del subgrupo Di sin distorsionar la
demanda. Evidencia: el horario por grupos de 3ºA/B/C tiene 30/30 tramos ocupados
(cero huecos), cero referencias a A8 (aula PDC), cero asignaturas de ámbito
(ÁmbSL/ÁmbCM/…). Las sesiones compartidas ord+Di (TUT3, EF, EPVA, Tec, Rel/ATED)
las hace igualmente el grupo ordinario con mismo profesor/aula/tramo; modelarlas
con subgrupo {3X} en vez de {3X, 3XDi} NO altera la demanda del ordinario (el Di
es capacidad adicional del aula, no demanda). PDC a escala (3ºADi/3ºBDi/3ºCDi,
reconciliación 3ºPDC↔3ºCDi) queda diferido a bloque posterior explícito.

Decisiones de diseño:
- Fixture de escala AMPLIADO in situ (no nuevo), conforme al linaje S20. Punto de
  7 grupos deja de ser reproducible desde el fichero (ya tiene 10): la curva se
  traza en ESTE registro del plan, no reejecutando datasets históricos. El test
  se MUTA (Opción A), no se duplica; la suite sigue en 30 tests.
- Dos coordinadas de nivel rep=1 (Bloque-3ESO-MAR/JUE), K=6 cada una (CyR×2
  desdoblado + refuerzo×3 + BioNu). Mayor densidad de simultaneidad por tramo que
  cualquier bloque de 1º/2º, pero mismo mecanismo de dominio (§6.4). CyR y BioNu
  comparten subgrupo entre MAR y JUE (continuidad del alumno, Hallazgo D / I6); el
  refuerzo rota (RefLe martes / RefMt jueves) — patrón análogo a PEPA/Fr2 de 2º.
- Religión partida en dos actividades multi-grupo (Hallazgo F): AB (3A+3B, viernes;
  las dos plazas ATED reparten ambos grupos, fiel al volcado) y C (3C sola, lunes).
- Geo→Geogr normalizado en 3º (divergencia de extracción del volcado de 3ºB; misma
  materia, 3 h/sem en los tres grupos). Catálogo de asignaturas sigue la convención
  S20: código crudo por nivel, sin unificación semántica entre niveles.

Verificación (toda programática, contra volcado fiel docs/horario-referencia/):
- 3º (3 grupos): 125/125 celdas del volcado cubiertas por el fixture; 30/30
  sesiones por grupo; demanda curricular idéntica en los tres (Biol 2, EF 2,
  EPVA 2, FQ 3, Geogr 3, Ingl 4, LCL 4, Mat 4, TUT3 1, Tec 2 = 27 mono + 2 coord
  + 1 relig). Coherencia grupos↔aulas: 123/123 celdas monogrupo cuadran.
- Fixture combinado válido: schema (loader estricto) + XOR de aula por plaza + I2
  (subgrupos disjuntos intra-actividad) + integridad referencial + códigos únicos.
- Factibilidad necesaria (no suficiente): carga ≤30 por recurso. Máx profesor TEC3
  18/30; aulas de 3º (B01/B03/B05) 19/30; Gim 20/30 (D4 sigue EN OBSERVACIÓN, no
  fuerza modelado dedicado aún; reevaluar al añadir 4º/Bach con más EF).

Entregado y commiteado:
- solver/src/test/resources/fixtures/problema-5-escala-instituto.json AMPLIADO:
  10 grupos, 42 profesores, 21 aulas, 33 asignaturas, 115 subgrupos, 110
  actividades, 148 plazas (18 con aulasCandidatas).
- SolverHorarioEscalaInstitutoTest mutado a 10 grupos (sanity checks: 10 grupos /
  30 tramos / 110 actividades; método escala1y2y3ESO). Javadoc documenta el
  escalón y la deuda de particiones.
- Medición: solución factible en 0,469 s. Suite 30 en verde, BUILD SUCCESS.

Lectura honesta del resultado: dos puntos de curva (7→0,317 s, 10→0,469 s) — +43%
de tamaño, +48% de tiempo: tramo holgado, crecimiento aproximadamente lineal. NO
extrapola al instituto completo (CP es no lineal; el salto de régimen llegará al
saturar recursos compartidos, p. ej. Gim/Pista y profes-puente). Además, 0,3–0,5 s
es territorio donde el ruido (JVM, carga JNI, GC) pesa; tendencia fiable, no
precisión al ms. Criterio 1: segundo punto, NO cerrado. Criterio 2: evidencia
parcial (0 duras a 10 grupos), NO cerrado.

src/main NO tocado → referencia-codigo-solver.md NO regenerado; sigue válido sobre
su commit hash. Modelo NO modificado: 3º no aportó capacidad estructural nueva
(reutiliza §6.4 y Hallazgo F); decisión consciente de no añadir §6.x, análoga a la
de S20 con 2º.

---

### Sesión 22 — Fase 5, Bloque 4: Lectura B (SubgrupoGrupo N:M). TRABAJO ESTRUCTURAL.

Elegido el Bloque 4 entre cuatro candidatos (4ºESO, PDC a escala, Lectura B, FPB).
Decisión: Lectura B, contra la pauta heredada de "escala barata", con tres
argumentos: (1) único trabajo que reduce riesgo estructural real (los demás son
escala/validación sobre mecanismos existentes); (2) el coste de cambiar el dominio
sube con el tiempo — hacerlo con 10 grupos estables verdes de regresión es lo más
barato; (3) es una capa limpia (estructura pura, fixture propio de discriminación).
Desbloquea el Tipo 7, última tipología del enunciado sin soporte de dominio.

Cambio de dominio: Subgrupo→grupo (1:1) pasó a Subgrupo→grupos Set<GrupoAdministrativo>
(N:M). Decididas dos cosas de diseño con el código delante (no a ciegas): (a)
Set<GrupoAdministrativo> en el record, NO entidad puente SubgrupoGrupo — el solver
solo pregunta pertenencia booleana (tocaGrupo), una entidad puente no compra nada en
el dominio en memoria; (b) Subgrupo con equals/hashCode SOLO por código (no derivado
de todos los componentes), porque el código es la identidad real y blinda los Set del
modelo frente a cambios de componentes. Constructor compacto rechaza grupos vacío
(invariante nueva: un subgrupo sin grupos sería invisible a tocaGrupo) y hace
Set.copyOf (inmutable + rechaza nulls).

Inventario de ficheros tocados (verificado con el compilador, no con grep — el grep
inicial perdió VistaPorGrupo, que usa Subgrupo::grupo como method reference y no casaba
con "\.grupo()"): src/main → Subgrupo, SubgrupoDto, ModeloCpSat.tocaGrupo
(sg.grupos().contains), VerificadorSolucion (gs.addAll(s.grupos())), VistaPorGrupo
(map→flatMap; su Javadoc ya preveía N:M), ProblemaHorarioMapper, schema (grupo→grupos
array). tests → VerificadorSolucionGrupoTest (firma Set.of(G), sin cambio de lógica),
SolverHorarioCierreFase4Test, SolverHorarioReligionParejasTest, y JSON inline de
ProblemaHorarioJsonLoaderTest. 12 fixtures migrados grupo→grupos con sed verificado
contra el fixture grande antes de disparar (patrón respeta grupoPadre y la sección
grupos top-level). Secuencia de 4 pasos con suite roja a propósito entre el cambio
estructural y la migración, para aislar regresión de migración.

Fixture de discriminación PROPIO (linaje S20: pequeño, quirúrgico, con prueba
negativa; NO toca el de escala): bloque de optativas 1ºBach C+D, recorte fiel de §6.3
(TICO/DTec/DA, profesor y aula distintos entre sí para que la única razón de bloqueo
sea el subgrupo multi-grupo). Subgrupos Opt-{TICO,DTec,DA}-CD → {1BachC, 1BachD}.
Prueba positiva (problema-5-lecturab-optativas-bach.json, 2 tramos): factible, 0
duras, el bloque toca C y D. Prueba negativa (problema-5-lecturab-optativas-bach-
infactible.json, 1 tramo): infactible por palomar — el bloque ocupa C vía el subgrupo
multi-grupo y LCL-1BachC compite por el único tramo. Discriminación: con dominio 1:1
(subgrupo solo en D) C estaría libre y sería factible; la infactibilidad depende de la
pertenencia a C, expresable solo con N:M. Suite 32 verde (30+2), BUILD SUCCESS.

CORRECCIÓN de S20 (hallazgo de esta sesión leyendo el dato fiel): "1B-C 1B-D" del
volcado de aulas NO es Lectura B. Es notación "1ºBach C + 1ºBach D" (dos grupos
enteros), corresponde a LU (LEN1), y es Lectura A multi-grupo — mismo patrón que la
Religión por parejas ya modelada. La evidencia REAL de Lectura B es el bloque de
optativas C+D (cinco materias simultáneas, cada alumno elige una; la población de cada
optativa mezcla alumnos de C y D). Verificado cruzando grupo-1BACH-C.json y
grupo-1BACH-D.json (bloque idéntico en ambos, día 1 t3 y día 2 t2).

Deuda nueva registrada:
- Invariante de población (heredada de Tarea 0): el volcado da las SESIONES de
  optativas, no el reparto nominal de alumnos. La partición concreta se confirma con
  el centro; ningún componente la verifica.
- Fixture-inline JSON en ProblemaHorarioJsonLoaderTest (text blocks): se escapa de
  cualquier migración sobre *.json. Si el schema vuelve a cambiar, migrarlo aparte.
  Deuda de testing.
- La prueba negativa de Lectura B es por palomar, no diferencial directo: demuestra
  infactibilidad real; el argumento "con 1:1 sería factible" vive en el diseño/Javadoc,
  no en una ejecución comparativa (imposible, porque el dominio ya no admite 1:1).

Modelo modificado (a diferencia de S20/S21): Lectura B SÍ aportó capacidad estructural
nueva. §6.4 corregida (el dominio ya soporta N:M; Lectura A es el caso de conjunto
unitario), §6.3 marcada como validada en el solver con enlace al fixture, §6.5 anotada
(diseño conceptual no ejecutado completo; capacidad N:M ya implementada), invariante S9
(líneas 717-721) pasada de predicción en futuro a hecho con precisión del cambio de
dominio.

src/main SÍ tocado → referencia-codigo-solver.md DEBE regenerarse sobre el nuevo HEAD
(cambian Subgrupo, su constructor/equals, SubgrupoDto, tocaGrupo, VistaPorGrupo,
VerificadorSolucion, mapper). Pendiente al cierre de la sesión.

Criterios de Fase 5: NINGUNO cerrado. Lectura B es estructura (desbloquea Tipo 7), no
escala; dos grupos de Bach en un fixture de discriminación ≠ Bachillerato a escala ≠
instituto completo. El Tipo 7 pasa de "sin soporte de dominio" a "soportado y
demostrado con prueba de discriminación".

### Sesión 23 — Fase 5, Bloque 5: PDC de 3º a escala (grupo Di 3PDC).

Elegido el Bloque 5 entre cuatro candidatos (Bachillerato, 4ºESO, PDC a escala,
FPB). Decisión: PDC a escala, por ser el único candidato de "capa limpia" sobre
lo ya construido (3º ordinario ya en el fixture y verificado), por cerrar una
deuda diferida en S21 y S22, y por no disparar D4 (no añade EF en Gim/Pista).
Descartados: Bachillerato (mezcla escala + optatividad densa + Lectura B real +
modalidad, varias capas a la vez); 4ºESO (escala + PDC acoplado, mejor ensayar
el acoplamiento ord↔Di antes sobre 3º conocido); FPB (cabo de datos abierto: el
horario por aulas no cubre FPB).

Lectura del dato fiel ANTES de construir (lección S22), con análisis mecánico:
los seis volcados grupo-3-ESO-*.json muestran que NO hay tres subgrupos Di
independientes. El tronco A8 (22 sesiones: ÁmbCM 8 / ÁmbSL 7 / OyD 2 / RefMt 2 /
IngDi 2 / TPMAR 1) es IDÉNTICO en los tres PDC (A/B/C) y sin solapamiento interno
(una asignatura por tramo) → un único grupo Di, no tres. La deuda
"reconciliación 3ºPDC↔3ºCDi (no determinista)" queda CERRADA por identidad: el
grupo-3-ESO-PDC.json (sin letra) es el Di adscrito a 3C, determinable porque su
envoltura ordinaria (tutoría TUT3/BYG3, Religión, EF) coincide con la de 3C. No
era indeterminismo: era la etiqueta del PDF.

Decisión de modelado (cerrada con el usuario): el Di NO es el patrón Lectura B de
las optativas de Bach. En Bach el subgrupo lista varios grupos porque los bloquea
ENTEROS (todos los alumnos se redistribuyen). En PDC el Di saca solo PARTE de
cada grupo y el resto sigue en clase ordinaria simultánea, así que el Di es un
grupo administrativo INDEPENDIENTE que solo se bloquea a sí mismo. Modelo final:
grupo 3PDC (tipo DIVERSIFICACION_PDC, grupoPadre 3C por I5) + subgrupo 3PDC con
grupos={3PDC} (mono-grupo) + 6 actividades mono-plaza DISTRIBUIDA, aulaFija A8.
Las 8 compartidas ord+Di se quedan en el ordinario (opción 2, decidida con el
usuario): el alumno Di es literalmente alumno de su grupo en esos tramos; añadir
3PDC como participante haría doble conteo (3PDC ⊂ 3A∪3B∪3C) que activaría D3.
Coste: el Di tiene 22 de 30 sesiones modeladas como propias; las 8 restantes se
observan desde el ordinario (presentación, no modelo).

Coste de proceso (aprendizaje para registrar): el modelado correcto costó TRES
intentos, los tres por razonar sobre código no leído antes de pedirlo —
exactamente lo que el handoff advertía. (1) subgrupo 3PDC→{3A,3B,3C}: INFEASIBLE,
porque tocaGrupo bloquea cada grupo listado en el Set → 30 h ordinario + 22 h Di
= 52 h en 30 tramos. (2) grupo propio con grupoPadre null: viola I5 (PDC exige
padre), descubierto al leer GrupoAdministrativo.java. (3) grupoPadre 3C: correcto,
ya con GrupoAdministrativo y ProblemaHorarioMapper leídos (el mapper solo enlaza
el padre y detecta ciclos; no lo propaga; ModeloCpSat es ciego a él, así que 3C
como padre no reacopla). Lección: pedir record + mapper de toda entidad nueva
ANTES del primer modelado, no tras cada fallo. La validación por aritmética
(carga ≤30) detectó el fallo 1 pero no podía detectar 2 ni 3 (invariantes de
dominio, solo visibles leyendo el código).

Entregables (commit 9a74aff, sin push hasta cerrar documentación): fixture de
escala problema-5-escala-instituto.json AMPLIADO (mismo linaje; +4 asignaturas
ÁmbCM/ÁmbSL/IngDi/TPMAR, +1 aula A8, +1 profesor ORI1, +1 grupo 3PDC, +1 subgrupo
3PDC, +6 actividades de tronco) + SolverHorarioEscalaInstitutoTest mutado (grupos
11, subgrupos 116, actividades 116; Javadoc reescrito explicando grupo Di propio
vs Lectura B). NO se regenera referencia-codigo-solver.md: la sesión solo añade
fixture + test, no toca src/main.

Medición: tercer punto de curva de escala — 10 grupos + 1 grupo Di → 0,408 s,
0 violaciones duras (VerificadorSolucion), suite 32 verde, BUILD SUCCESS. Dentro
del ruido de los puntos previos (0,317 s a 7 grupos; 0,469 s a 10 sin Di): NO
dispara salto de régimen (A8 exclusiva del Di, profesores-puente ≤14/30). El
salto por saturación de recurso compartido sigue PENDIENTE de observar en
Bachillerato/4º (Gim/Pista, D4).

Criterios de Fase 5: NINGUNO cerrado. Sigue siendo un escalón, no el instituto
completo. Criterio 1 con tercer punto de curva (tramo holgado ~lineal), criterio
2 con evidencia parcial acumulada.

Deudas tocadas: "PDC a escala (3ºADi/3ºBDi/3ºCDi)" CERRADA (resuelta como un
grupo, no tres). "Reconciliación 3ºPDC↔3ºCDi" CERRADA (por identidad). Invariante
de población: sigue VIVA y más concreta — el Di adscrito a 3C tiene población
real de 3A+3B+3C, no modelada. D3/D4: sin tocar; D4 sigue en observación, a
reevaluar con Bachillerato/4º.

### Sesión 24 — Fase 5, Bloque 6a: función objetivo (ventanas del profesorado).

Elegido el Bloque 6a entre cuatro candidatos (4ºESO, Bachillerato, FPB, función
objetivo). Decisión: función objetivo, por ser el único candidato que ataca los
criterios 3-4 de Fase 5 (bloqueados por la ausencia de objetivo, no por falta de
escala), por introducir el régimen de optimización ANTES del salto de régimen
esperado en Bach/4º (mejor instrumento de medición para cuando llegue), y por ser
capa limpia que no toca I/O. Descartados: Bachillerato y FPB (mezclan varias capas
/ cabo de datos abierto de aulas FPB), 4ºESO (escala + PDC nuevo acoplado).

Descomposición acordada del Bloque 6: 6a = andamiaje de optimización + ventanas
(sin dato nuevo); 6b = D11 indisponibilidades (dato nuevo: amplía Profesor + DTO
+ schema + mapper); 6c+ = distribución blanda, primeras/últimas horas. Una capa
por sub-bloque.

Decisiones de diseño (cerradas con el usuario antes de tocar código):
- 1a: objetivo OPCIONAL. `construir()` se queda en factibilidad pura (no se toca);
  nuevo `construirConObjetivo()` añade el objetivo. Razón: los tests de escala
  miden tiempo hasta primera factible; meter el objetivo en ese camino los
  invalidaría y cegaría la curva justo antes del salto de régimen de Bach/4º.
- 2a: contrato de retorno sin cambios (`SolucionHorario`); el valor del objetivo
  se recomputa en el verificador, fiel a su filosofía de independencia. (2b —
  retorno rico con status + objetivo — diferido a cuando la UI lo pida.)
- Forma A (cotas tensadas) para modelar huecos, sobre Forma B (un booleano por
  tramo): menos variables, formulación estándar. Validada en dos pasos: (1)
  simulación semántica en las 120 soluciones del fixture (óptimo del modelo ==
  conteo del verificador, 0 discrepancias); (2) ejecución real del solver.
- 4=peso constante (un solo término); 5a=método nuevo en el verificador sin tocar
  ResultadoVerificacion; 6a=comprobación de oro por aserto fuerte con objetivo +
  verificación manual sin objetivo (no aserto flaky); 7=cotas tensadas + deuda
  D17 (en vez de addMinEquality/addMaxEquality).

Método (lección S23 aplicada): se pidió y leyó el cuerpo de TODA entidad tocada
ANTES de modelar — ModeloCpSat, SolverHorario, InstanciaProgramada,
VerificadorSolucion, Profesor, ProblemaHorario, Actividad, Plaza, Expansion,
ActividadInstancia y el schema real del repo. Cero intentos fallidos por razonar
sobre código no leído (contraste con los 3 de S23). Los 5 puntos de riesgo de
firma de OR-Tools (LinearExpr.sum, builder addTerm, LinearExpr.term,
addLessOrEqual/addGreaterOrEqual con onlyEnforceIf, addEquality con onlyEnforceIf)
se marcaron como tales antes de compilar; todos válidos en ortools-java 9.11.4210.

Hallazgo de S24 (condiciona 6b): un hueco INEVITABLE (óptimo de ventanas > 0) no
es construible sin prohibir tramos concretos a un profesor, y lo único que prohíbe
tramos es una indisponibilidad horaria (ProfesorRestriccionHoraria, dato de 6b).
Las duras actuales solo crean exclusión mutua entre sesiones, no fijan una sesión
a un tramo; el aula compartida no clava. Por eso la comprobación de oro fuerte
estilo S9 (aserto determinista que falla si el objetivo no actúa) pertenece a 6b.
En 6a se cerró el riesgo realista alternativo (un `contarVentanasProfesor` roto
que siempre devuelva 0) con la Opción I: un segundo caso de test que construye una
colocación manual con ventanas conocidas (P1 pos {1,3,4} → 1; P2 pos {2,5} → 2;
total 3) y verifica que el contador las detecta.

Entregado (src/main): ModeloCpSat (constante PESO_VENTANAS, campo terminosObjetivo,
construirConObjetivo, ensamblarObjetivo, objetivoVentanasProfesor, helper
complemento; construir() intacto); SolverHorario (resolverOptimizando; resolver()
intacto; Javadoc de clase ampliado); VerificadorSolucion (contarVentanasProfesor;
resto intacto). Tests/fixtures: problema-6-ventanas-profesor.json (discriminación,
linaje propio, no escala) + SolverHorarioVentanasProfesorTest (2 casos).

Medición: el test con objetivo prueba optimalidad (óptimo 0) en ~0,03 s. El test
de escala sigue en factibilidad pura (0,8 s, dentro del ruido JVM/JNI/GC; NO salto
de régimen). Linaje de medición de optimización separado del de factibilidad
(decisión 1a). Suite 34 verde, BUILD SUCCESS.

Criterios de Fase 5: NINGUNO cerrado. El criterio 4 pasa de "inabordable (sin
objetivo)" a "mecanismo implementado y validado en discriminación; falta umbral
(decisión consciente: no inventar sin datos del centro) y validación a escala".
Criterio 3 necesita más términos blandos (6c+). Criterios 1-2 sin cambios (exigen
instituto completo).

Deudas tocadas: D11 ligada a 6b (siguiente sub-bloque). Nueva D17 (cotas tensadas).
D3/D4 sin tocar.

src/main SÍ tocado → referencia-codigo-solver.md DEBE regenerarse (cambian
ModeloCpSat, SolverHorario, VerificadorSolucion). Pendiente al cierre.

---

## Señales globales de alerta

Si alguna de estas situaciones ocurre, para y replantea antes de continuar:

- Llevas más de 3 sesiones de trabajo en la misma fase sin avanzar
- El solver viola restricciones duras en el instituto completo después de Fase 5
- El modelo de datos necesita cambios estructurales después de Fase 6
- El bundle Windows requiere pasos manuales del usuario
