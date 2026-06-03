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
- [ ] Las dos sesiones del desdoble de CyR aparecen en el mismo tramo
- [ ] Ninguno de los 4 grupos tiene otra sesión en el tramo de RefMt
- [ ] Los 3 profesores del RefMt están libres en ese tramo (no aparecen 
      en otras sesiones)
- [ ] Las aulas del desdoble/agrupamiento son distintas y correctas 
      (A12In para INF1, A5 para TEC3, etc.)
- [ ] Puedes bloquear manualmente un tramo y el solver lo respeta

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
- [ ] El subgrupo 3ºADi no tiene sesiones solapadas con su grupo ordinario 3ºA
- [ ] Las sesiones compartidas (EF, EPVA, Tec) aparecen en el mismo tramo 
      para 3ºA y 3ºADi
- [ ] LEN2 (profesor de ÁmbSL) no aparece en otro sitio cuando está con 3ºADi
- [ ] El horario impreso de 3ºA muestra correctamente qué alumnos están 
      en diversificación en cada tramo

---

## FASE 5 — Solver: instituto completo
**Objetivo:** El solver funciona con todos los grupos reales del instituto.

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

Fase actual: 3 — Solver: desdobles y agrupamientos (en curso)
Última fase completada: 2 — Solver MVP: problema mínimo
Última sesión registrada: Sesión 16 — Fase 3, commit 2 (aulasCandidatas con intervalos opcionales) cerrado. Deudas D15 y D16 abiertas.

### Bloques de Fase 2
- [x] Bloque 1 — Setup del repositorio
- [x] Bloque 2 — POJOs del dominio
- [x] Bloque 3 — Schema JSON + DTOs + mapper
- [x] Bloque 4 — Construcción del modelo CP-SAT
- [x] Bloque 5 — Output a consola
- [x] Bloque 6 — Dataset real 1ºESO ordinarias + verificación PDFs

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
- **D15**: `VerificadorSolucion` cuenta el aula con un `Set` POR INSTANCIA, lo
  que enmascara una violación de S2: dos plazas de la misma instancia que
  eligen (o fijan) la misma aula en el mismo tramo colapsan a un único aula en
  el conteo → cuenta 1 → no se reporta. Por S2 + glosario del modelo, esas dos
  plazas son una colisión (el uso compartido de aula es UNA plaza con varios
  profesores, no varias plazas con un aula), así que el verificador debería
  detectarlo y no lo hace. Matices: (1) es debilidad PREEXISTENTE en `aulaFija`,
  no la introduce el aula variable de la Sesión 16; (2) el solver SÍ la previene
  vía `addNoOverlap` (dos intervalos —fijos u opcionales— sobre la misma aula en
  el mismo tramo se solapan), así que en la práctica no se cuela una solución
  mala; el hueco es solo en la red de seguridad independiente. Reforzar a conteo
  de aula POR PLAZA (solo aula; profesor/subgrupo/grupo siguen por instancia,
  donde el colapso es correcto por S1) queda como ítem propio. Documentada
  también en el comentario del propio `verificarNoSolapes`.
- **D16**: el CLI (`Materializador`, `FormatoCelda`) no pinta el aula ELEGIDA de
  las plazas con aula variable: `FormatoCelda` muestra `?` cuando no hay
  `aulaFija`. La Sesión 16 dejó el CLI fuera a propósito (el commit ya era ancho
  y estructural; pintar el aula es otra preocupación). `SolucionHorario` ya
  expone `aulaElegida(inst, plaza)`, así que el cambio es acotado: el
  materializador debe leer de ahí en vez de `plaza.aulaFija()`. Pendiente de
  commit posterior dentro de Fase 3 o al cierre de fase.

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

---

## Señales globales de alerta

Si alguna de estas situaciones ocurre, para y replantea antes de continuar:

- Llevas más de 3 sesiones de trabajo en la misma fase sin avanzar
- El solver viola restricciones duras en el instituto completo después de Fase 5
- El modelo de datos necesita cambios estructurales después de Fase 6
- El bundle Windows requiere pasos manuales del usuario
