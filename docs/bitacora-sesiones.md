# Bitácora de sesiones — Educhronos

Registro detallado e histórico de las sesiones de trabajo S10–S42. Archivado
desde `plan_trabajo_horarios.md` en la Sesión 44 (higiene documental) para
aligerar el plan de trabajo, conservando la traza completa de decisiones.

**Jerarquía de autoridad**: el estado VIVO del proyecto (fase actual, criterios,
deuda consciente abierta, decisiones permanentes, bloques de fase) está en
`plan_trabajo_horarios.md`. Esta bitácora es histórico de solo lectura: no se
consulta para conocer el estado actual, sino para entender por qué se tomó una
decisión pasada. El plan conserva además las 4 últimas cabeceras compactas de
sesión (S41–S44) por estar referenciadas por deuda abierta.

Orden: cronológico ascendente (S10 → S42). Los formatos difieren según la época
de registro (entradas detalladas con cabecera de sección para S10–S31, entradas
de párrafo para S32–S42); se conservan tal como se escribieron.

---

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

### Sesión 25 — Fase 5, Bloque 6b: indisponibilidades horarias DURA + oro fuerte de ventanas.

Elegido el Bloque 6b entre cinco candidatos (6c más blandas, 4ºESO, Bachillerato,
FPB, y el propio 6b). Decisión: 6b, por ser el único candidato que cierra una
deuda de validación concreta y comprometida en S24 (la oro fuerte de ventanas)
añadiendo exactamente UNA capa. Descartados: 6c (desmonta la distribución dura en
mal orden, activa D17 antes de validar el primer término); escala 4º/Bach/FPB
(cambia de eje dejando Fase 5 a medias, con D4 o el cabo de aulas FPB sin resolver).

Decomposición del trabajo en cuatro turnos verificables (disciplina S24):
- I/O + dominio: la indisponibilidad entra como dato, se valida, el solver la
  IGNORA. Decisiones cerradas con el usuario: (1) el solver consume solo DURA en
  6b; BLANDA se carga y valida pero su consumo (término del objetivo) se difiere a
  6c; (2) el loader acepta y guarda ambas con forma completa (tipo+peso+motivo),
  ensanchando I/O una sola vez; (3) forma B en el JSON — array top-level
  `restriccionesHorarias` — + forma 2 en el dominio — colección en
  `ProblemaHorario`, `Profesor` queda como record puro sin arista a `Tramo`. Esta
  última corrige la inclinación inicial (embeber en `profesor`): leído el mapper,
  embeber costaba una arista de dominio Profesor→Tramo no deseada; la colección en
  el agregado raíz es además la forma fiel al modelo (`ProfesorRestriccionHoraria`
  como tabla con FK a ambos). (4) peso en DURA: ignorado, no error.
- Consumo en el solver: `restriccionIndisponibilidadProfesor` en `construir()`
  (es restricción DURA → aplica en ambos regímenes; al estar en `construir()`,
  `construirConObjetivo` la hereda). Mecanismo: `addLinearExpressionInDomain` sobre
  el `tramoIndex` de cada instancia que use al profesor, con el dominio
  complementario de los tramos vetados — el mismo patrón ya presente en
  `objetivoVentanasProfesor` y `restriccionDistribucionPorDia`, cero APIs nuevas de
  OR-Tools. Helper `complementoDe(Set, numTramos)` junto al `complemento(int,...)`
  existente. Aplicación a nivel de instancia (no plaza): todas las plazas comparten
  `tramoIndex`. Profesor con todos los tramos vetados → dominio vacío → INFEASIBLE,
  respuesta correcta (deuda D18). Test SolverHorarioIndisponibilidadProfesorTest:
  redirige (1 instancia, veto → cae en el tramo libre), infactibiliza (2 instancias,
  palomar tras veto → INFEASIBLE), discrimina (mismo problema sin veto → factible).
- Oro fuerte de ventanas: cierra la deuda S24. Fixture validado por enumeración en
  diseño ANTES de ejecutar: MAT8, 2 clases, día de 5 tramos, vetado en pos 2 y 4 →
  posiciones disponibles {1,3,5} → colocaciones {1,3}=1, {3,5}=1, {1,5}=3 ventanas.
  Óptimo determinista = 1 (estrictamente positivo), con alternativa factible más
  cara (3): un optimizador que solo buscara factibilidad podría devolver 3;
  minimizar obliga a 1. Es lo que 6a NO podía probar (allí el óptimo alcanzable era
  0). Aseverado vía `contarVentanasProfesor` (recomputo independiente de OR-Tools)
  sobre la solución devuelta — más fuerte que leer el objetivo del solver (que no
  se expone, decisión 2a). Discriminación: sin veto, óptimo 0. Un solo término en
  el objetivo → D17 NO se activa (cotas tensadas siguen válidas).
  Test SolverHorarioOroFuerteVentanasTest (2 casos).

Entregado (4 commits, árbol limpio):
- I/O (fb136de): records RestriccionHoraria + enum TipoRestriccion (domain),
  RestriccionHorariaDto (io), campo en ProblemaHorario + ProblemaHorarioDto,
  resolución en el mapper, $def + top-level opcional en el schema, 3 tests del
  loader. VerificadorSolucionGrupoTest ajustado (constructor de ProblemaHorario
  gana un argumento). Suite 37 verde.
- Solver: restriccionIndisponibilidadProfesor + complementoDe en ModeloCpSat,
  3 fixtures + SolverHorarioIndisponibilidadProfesorTest. Suite 40 verde.
- Oro fuerte (1685554): 2 fixtures + SolverHorarioOroFuerteVentanasTest.
  Suite 42 verde, BUILD SUCCESS.
- Índice de código regenerado (src/main cambió: ProblemaHorario, mapper,
  ModeloCpSat + records/DTO nuevos). Modelo actualizado (§4.3: consumo DURA + D18).

Método: cero intentos fallidos (igual que S24). Se pidió y leyó el cuerpo de TODA
entidad tocada ANTES de modelar (Profesor, ProfesorDto, mapper, ProblemaHorario,
Tramo, ProblemaHorarioJsonLoaderTest, VerificadorSolucionGrupoTest, ModeloCpSat,
InstanciaProgramada, SolverHorario, SolucionHorario, VerificadorSolucion). Riesgos
de firma de OR-Tools marcados antes de compilar: ninguno nuevo (se reutilizan APIs
ya presentes).

Criterios de Fase 5: el criterio 4 pasa de "inabordable" (pre-6a) a PARCIAL —
mecanismo implementado y validado INCLUYENDO oro fuerte; falta umbral (sin datos
del centro) y validación a escala. Criterios 1-2 sin avance (exigen instituto
completo). Criterio 3 sin avance (necesita más términos blandos, 6c+).

Deudas tocadas: D11 (indisponibilidades) AVANZADA — variante DURA consumida;
BLANDA y preferencias positivas para 6c. D17 REVISADA — sigue inactiva (6b no
añade segundo término al objetivo; las cotas tensadas siguen válidas). D18 NUEVA
(INFEASIBLE no diagnostica la causa; ver §4.3 del modelo). Deuda de fixture-inline
de ProblemaHorarioJsonLoaderTest: NO reabierta (6b añadió casos nuevos al fichero,
pero el schema cambió de forma compatible —campo opcional— y los text blocks
previos siguen válidos sin migración).

### Sesión 26 — Fase 5, Bloque 6c: indisponibilidad BLANDA + revisión de requisitos (D18/D19/D20).

Dos trabajos, dos commits separados (código y documentación). Decidido al inicio
con el usuario: (a) abordar la revisión de requisitos destapada en S25 como
trabajo de documentación, y (b) el bloque de solver siguiente = 6c, empezando por
la indisponibilidad BLANDA (el sub-bloque más limpio: el dato ya entra desde 6b,
solo falta consumirlo). Se descartó conscientemente empezar por escala (4º/Bach/
FPB), que ataca los criterios 1-2 pero arrastra D4 o el cabo de aulas FPB; 6c
avanza el criterio 3, el eje en curso.

Revisión de requisitos (commit de docs, hecho primero para no mezclar capas):
- D18 AMPLIADA de "indisponibilidad imposible de profesor" a "condiciones
  necesarias baratas de factibilidad" (chequeos de conteo/palomar que detectan
  ALGUNAS infactibilidades seguras con mensaje accionable; NO un validador de
  factibilidad — demostrarla es imposible, solo el solver la decide). Lógica en
  capa de configuración (Fase 6/8), hermana de DemandaCurricular y D3. Modelo §4.3
  reescrito + alta en la lista de deuda del plan (faltaba: el plan llegaba a D17,
  D18 solo vivía en el modelo).
- D19 NUEVA: atribución de reglas duras Y blandas por celda, sobre el horario YA
  generado (no solo durante el drag). La maquinaria existe (VerificadorSolucion,
  contarVentanasProfesor); ningún requisito de UI la exponía celda a celda. Fase
  7/8. Modelo §8 + plan + criterio de Fase 8 ampliado ("qué lo causa" incluye
  blandas).
- D20 NUEVA (separada de D18 por decisión del usuario): UI de los avisos de
  pre-validación (presentación de las condiciones necesarias al usuario). Fase
  6/8. Modelo §8 + plan.

Bloque 6c (commit de código, dos turnos verificables por separado):
- Turno A — el término muerde. `objetivoIndisponibilidadBlandaProfesor()` añadido
  a `construirConObjetivo()` entre ventanas y el ensamblado; constante
  `PESO_INDISP_BLANDA=1`; recomputo gemelo `contarPenalizacionIndisponibilidadBlanda`
  en el verificador (independiente de OR-Tools). Fixture
  problema-6c-indisp-blanda-discriminacion.json (validado por enumeración: 1
  actividad NEUTRA, día de 2 tramos, vetado-blando en L1 → óptimo 0 en L2,
  alternativa de coste 1 en L1). Test asevera DOS cosas: penalización 0 y que la
  instancia quedó en L2 (la posición cierra el agujero de "0 por azar"). Suite 43.
- Turno B — oro fuerte. Sin tocar código de producción (la maquinaria del Turno A
  basta): solo fixture + test. problema-6c-indisp-blanda-oro-fuerte.json (validado
  por enumeración: 2 actividades NEUTRA de P1, 3 días de 1 tramo cada uno, vetado-
  blando en L1 y L2, tramo limpio L3 → no-solape de profesor fuerza 2 de 3 tramos
  ocupados; óptimo determinista 1, alcanzable de dos formas; alternativa factible
  de coste 2 ocupando ambos vetados). Ventanas idénticamente 0 (1 clase por día):
  blanda AISLADA, decisión de diseño para que el óptimo no dependa del peso
  relativo entre términos. El test asevera el COSTE (=1), NO la posición: el óptimo
  no es único en colocación (vaciar L1 o L2), igual que el oro fuerte de ventanas
  asevera el conteo y no la disposición. Suite 44 verde, BUILD SUCCESS.

Método: cero intentos fallidos (como S24/S25). Se pidió y leyó el cuerpo de TODA
entidad tocada ANTES de modelar (ModeloCpSat completo, RestriccionHoraria,
ProblemaHorario, VerificadorSolucion, SolucionHorario, y el fixture y el test de
6a/6b como plantilla del formato). Riesgos de firma de OR-Tools: ninguno nuevo
(se reutilizan addLinearExpressionInDomain, Domain.fromValues, complemento,
LinearExpr.term, todos presentes desde 6a/6b); compiló a la primera. Lección de
enumeración exhaustiva en diseño aplicada a ambos fixtures: el primer diseño del
oro fuerte (4 tramos, 3 actividades) se desechó al enumerar y descubrir que metía
ventanas que contaminaban la aserción; se rediseñó a 3 días de 1 tramo para
aislar la blanda.

Dictamen D17 (registrado en el Javadoc de objetivoVentanasProfesor): las cotas
tensadas NO necesitan migrar a addMinEquality/addMaxEquality. El término blando
es separable del de ventanas — penaliza el tramoIndex de instancias concretas, no
toca primero/ultimo/huecos/span; no hay holgura por la que minimizar la blanda
infle el span. La deuda D17 permanece VIVA para términos FUTUROS que sí miren
posiciones (primeras/últimas horas).

Criterios de Fase 5: el criterio 3 (calidad comparable) AVANZA — segundo término
blando incorporado y validado (discriminación + oro fuerte). NO cerrado: faltan
los términos restantes (distribución-a-blanda, primeras/últimas horas,
consecutivas máximas) y la validación a escala. Criterio 4 sin cambio respecto a
S25 (PARCIAL: falta umbral + escala). Criterios 1-2 sin avance (exigen instituto
completo).

Deudas tocadas: D11 (indisponibilidades) AVANZADA — variante BLANDA ahora
consumida como término del objetivo; las preferencias POSITIVAS siguen sin modelar
(decisión de Fase 1). D17 RESUELTA para el primer competidor, VIVA para futuros.
D18 AMPLIADA, D19 y D20 NUEVAS (revisión de requisitos). DEUDA NUEVA: parametrización
y calibración de pesos blandos (ambos pesos a configuración + valores relativos con
datos reales y un fixture multi-término; hoy ambos hardcodeados a 1). D3/D4 sin
tocar.

src/main SÍ tocado (ModeloCpSat, VerificadorSolucion) → referencia-codigo-solver.md
regenerado al cierre.

### Sesión 27 — Fase 5, Bloque 6d-c: sesiones consecutivas máximas del profesorado.

Decidido al inicio con el usuario entre dos vías: 6d (tres términos blandos
restantes del criterio 3) y escala (4º/Bach/FPB, que ataca los criterios 1-2 y donde
se espera el salto de régimen D4). Se eligió 6d-c (consecutivas máximas) por dos
razones: es la única capa LIMPIA que queda del criterio 3 —6d-a (distribución-a-
blanda) toca la restricción dura que vive en dos sitios con guarda D12; 6d-b
(primeras/últimas horas) reactiva D17 porque mira posiciones—, y cerrar la capa
limpia antes de abrir escala deja el criterio 3 más completo si escala diera
problemas de régimen. Tres decisiones de diseño tomadas con el usuario antes de
codear: (A) definición = suma de excesos sobre N por racha; (1) formulación =
ventanas deslizantes con addBoolAnd (sin APIs nuevas); (i) N hardcodeado con deuda
(opción consciente de no inventar I/O como efecto colateral de un término blando, ni
calibrar N sin datos del centro).

Código (un commit): constantes `MAX_CONSECUTIVAS=3` y `PESO_CONSECUTIVAS=1` +
`objetivoConsecutivasProfesor()` en `construirConObjetivo()` (tras la blanda, antes
del ensamblado) en ModeloCpSat; recomputo gemelo `contarPenalizacionConsecutivasProfesor`
en VerificadorSolucion. El gemelo cuenta rachas maximales; el modelo cuenta ventanas
deslizantes de N+1: equivalencia validada por enumeración exhaustiva de los 256
subconjuntos de un día de 8 tramos (cero discrepancias) antes de codear.

Tests + fixtures (un commit): turno A discriminación (SolverHorarioConsecutivasProfesorTest:
4 actividades NEUTRA de P1, día de 4 tramos + día de 1 → óptimo 0 repartiendo ≤3 al
día1; asevera coste 0 Y día1≤3, la posición cierra el "0 por azar") y turno B oro
fuerte (SolverHorarioOroFuerteConsecutivasTest: 7 actividades de P1, dos días de 4
tramos → algún día fuerza 4 seguidas ⇒ óptimo 1 inevitable, partir cuesta ventana ⇒
2 rechazado; asevera el COSTE =1, no la posición). Ambos fixtures validados por
enumeración exhaustiva en diseño, con la comprobación EXPLÍCITA de no-contaminación:
ventanas vale 0 en óptimo y alternativa, y ningún óptimo de coste total ≤1 tiene
consecutivas 0 (la vía-ventanas a coste 1 no existe) → el aserto sobre el verificador
de consecutivas es robusto. Se desecharon en diseño varias configuraciones de fixture
al enumerar y ver que ventanas contaminaba (lección S26 reforzada).

Método: cero intentos fallidos (como S24/S25/S26). Se pidió y leyó el cuerpo de TODA
entidad tocada ANTES de modelar (ModeloCpSat completo —objetivoVentanasProfesor como
plantilla del patrón de conteo por profesor-día—, VerificadorSolucion, y los fixtures
+ tests de 6c como plantilla del formato). Riesgos de firma OR-Tools: ninguno nuevo
(addBoolOr/addBoolAnd(...).onlyEnforceIf idénticos a los de ventanas); compiló a la
primera. No-regresión confirmada por ejecución: ventanas (6a) e indisponibilidad
blanda (6c) verdes, los tres términos conviven sin interferencia; D17 intacta como se
afirmó.

Criterios de Fase 5: el criterio 3 (calidad comparable) AVANZA — tercer término
blando incorporado y validado (discriminación + oro fuerte). NO cerrado: faltan
distribución-a-blanda (6d-a, estructura dura), primeras/últimas horas (6d-b, reactiva
D17) y validación a escala. Criterio 4 sin cambio (PARCIAL). Criterios 1-2 sin avance
(exigen instituto completo).

Deudas tocadas: D21 AMPLIADA — tres pesos hardcodeados (añadido PESO_CONSECUTIVAS) +
el parámetro N=3 (MAX_CONSECUTIVAS) sin calibrar + micro-deuda del `n=3` duplicado en
el verificador (espejo frágil de la constante privada, se resuelve cuando N pase a
configuración). D17 NO reactivada (separable). D3/D4 sin tocar (siguen esperando a
escala). El modelo NO se tocó (6d-c no añade entidad ni invariante; la constante es
interna del solver).

src/main SÍ tocado (ModeloCpSat, VerificadorSolucion) → referencia-codigo-solver.md
regenerado al cierre.

### Sesión 28 — Fase 5, Bloque 7: escala 4ºESO ordinario (4A–4D, sin Di).

Primer escalón de escala de 4ºESO. Linaje de escala AISLADO (fixture propio
problema-5-escala-4ESO.json + SolverHorarioEscala4EsoTest), SEPARADO del de
escala-instituto: 4º trae el salto de régimen de D4 y se observa sin el ruido de
1º/2º/3º; la fusión de niveles es paso posterior. Escala pura: ningún cambio de
dominio ni de src/main.

Trabajo previo de dominio (decidido con el usuario antes de modelar, leyendo los
volcados fieles de 4º y la prematrícula 2025/26):
- En 4º la optatividad es el ESQUELETO del horario, no un añadido: MatAp/MatAc son
  las dos vías troncales excluyentes (Mates A/B de la prematrícula), no optativas.
  El troceo "ordinario sin optatividad" se desechó (irreal); se modeló 4º ordinario
  COMPLETO con su estructura de plazas compartidas, sin Di (Turno 1).
- Granularidad por EVIDENCIA (Hallazgo K), enumerando tramo a tramo qué grupos
  coinciden: 3 bloques transversales sobre los 4 grupos (DT+RefMt+CeH+AFAVS en M5;
  DT+RefLe+CeH+AFAVS en J3; Rel+ATEDU en V4), 2 sobre {A,B,D} (FQ+DIG;
  Biol+TEC+FOPP), 1 sobre {A,B} (mates partidas), 1 sobre los 4 con EXPRE
  desdoblada. C es grupo aparte (itinerario de letras: LAT, ECO), D híbrido (mixto).
- Optativas de nivel = plazas compartidas Tipo 7 (lectura confirmada): un profesor
  y un aula únicos por plaza, NO clonados; verificado físicamente celda a celda que
  ningún (profesor, aula) compartido aparece dos veces en el mismo tramo. EXPRE se
  desdobla por capacidad de taller dentro de la misma actividad-bloque.
- Se modela el HORARIO REAL (volcados), no la prematrícula: el reparto de alumnos a
  optativas es decisión humana previa al solver; la app coloca plazas predefinidas,
  no decide la oferta. La prematrícula entró como documentación de dominio.
- EFis canónico sin tilde (el volcado de 4ºD traía EFís; normalizado).
- Nombrado de subgrupos: partición por profesor dentro de una actividad cuando hay
  varias plazas de la misma asignatura (RefMt×2, AFAVS×2, MatAp×2, ATEDU×4 = pobla-
  ciones distintas); reuso de subgrupo ENTRE actividades distintas solo en
  DT/CeH/AFAVS (misma partición de alumnos demostrada por el volcado en M5/J3, que
  vía S3 fuerza que esos dos bloques no coincidan); DIG/TEC/FOPP con población
  propia por bloque (sin reuso) por la deuda de las 6h (ver abajo).

Construcción y verificación: fixture generado programáticamente desde los volcados
(no a mano), validado contra el schema real del mapper (integridad referencial,
I2, I7, XOR de aula, aulas fijas disjuntas por actividad, patronTemporal,
cuadre 30/30 por grupo). El loader NO se tocó (es genérico; carga cualquier fixture
por getResourceAsStream). El test calca SolverHorarioEscalaInstitutoTest (misma red
de 600 s, semilla 42, factibilidad pura, VerificadorSolucion como red independiente);
sanity check propio (4 grupos, 30 tramos, 96 subgrupos, 31 actividades).

Resultado: 4º ordinario FACTIBLE en 0,126 s, 0 violaciones duras. Suite 47 verde
(46 previos + el nuevo), BUILD SUCCESS, sin regresiones (escala-instituto sigue en
0,388 s). D4 a saturación (AFAVS Gim+Pista para los 4 grupos en M5/J3) y el cuello
INF1/A12In (DIG 6h en aula única) NO rompen en 4º aislado. Lectura honesta: esto
cierra UN escalón, no la pregunta de fondo —D4 al FUSIONAR niveles (Gim/Pista
compartido entre cursos) sigue sin probarse—.

NO cierra criterios de Fase 5: 4º aislado no prueba D4 fusionado; criterios 1-2
exigen el instituto completo. Criterio 3 sin cambios (no es término del objetivo).

Deuda nueva (DIG/TEC/FOPP de 4º): suman 6h entre dos bloques de perfil distinto, lo
que no encaja con la optativa única de 3h de la prematrícula. Por precaución se
modelan como población propia por bloque (sin reuso de subgrupo, sin acople S3):
¿son la misma optativa de 6h o franjas distintas? Pendiente del centro. Forma parte
de la invariante de población viva. TUT4 modelada como actividad ordinaria: el DTO
no transporta tutor obligatorio (S8 no ejercitada en 4º).

src/main NO tocado (escala pura; el log confirma "Nothing to compile") →
referencia-codigo-solver.md NO regenerado. modelo_datos_fase1.md NO tocado (no
añade entidad ni invariante; la deuda de las 6h vive en este plan). D3/D4 sin tocar
(siguen esperando a la fusión de niveles).

### Sesión 29 — Fase 5, Bloque 8: escala 4ºESO completo (4A–4D + 2 PDC).

Turno 2 del trabajo de 4º: se cierra 4º como linaje incorporando los dos grupos de
diversificación al fixture aislado de 4º. Decisión de alcance tomada con el usuario:
4º COMPLETO en un único fixture (ordinario + PDC), no PDC aislado — el PDC aislado
mediría factibilidad falsa porque comparte EF/tutoría/V4 con el ordinario.

Trabajo previo de dominio (leyendo los seis volcados fieles grupo-4-ESO-{A,B,C,D,
A-PDC,D-PDC}.json y cruzándolos por código antes de modelar):
- Cabo abierto resuelto: son DOS grupos Di (4APDC pág.15, 4DPDC pág.19), adscritos a
  4A y 4D. NO separables del ordinario.
- Hallazgo central por enumeración: los 26 ámbitos de los dos PDC son IDÉNTICOS
  tramo a tramo (mismo día/asignatura/profesor/aula, todos en B04). Los Di de 4A y
  4D cursan el ámbito JUNTOS, igual que el 3PDC de S23 reunía a los Di de 3A/3B/3C
  en A8. → ámbito = UNA actividad con subgrupo compartido {4APDC,4DPDC}.
- EF y tutoría sí son específicas de cada Di con su grupo de origen (verificado como
  plaza física compartida: mismo profesor/aula/tramo en la ficha del Di y la del
  ordinario): 4APDC↔4A (EFI1/Gim), 4DPDC↔4D (EFI3/Pista). Plaza única conjunta
  {4X,4XPDC} (decisión A del usuario). V4 (Rel+ATEDU) ampliado a los 6 grupos, el Di
  repartido en las 5 bandas como los ordinarios (decisión B; deuda de población).

Regla S23 aplicada y confirmada de la forma dura: el subgrupo de ámbito lista SÓLO
los Di, no los padres. El segundo INFEASIBLE del bloque vino justo de violar el
espíritu de esto por otra vía (duplicar B04), no de listar los padres; pero el
principio es el mismo: no inflar las horas que tocaGrupo imputa.

Construcción y verificación: fixture generado programáticamente desde los volcados,
ampliando el de S28 (no reconstruido). Validado contra el schema real y las
invariantes del mapper: integridad referencial, UNICIDAD de códigos (subgrupo,
actividad, plaza, grupo, asignatura, profesor, aula), I2, I7, XOR de aula
(Plaza.java), aulas fijas disjuntas por actividad, cuadre 30/30 por los 6 grupos
vía tocaGrupo, y CARGA por aula fija y por profesor ≤30 (validación nueva). El
loader NO se tocó (genérico). Test calca SolverHorarioEscala4EsoTest; sanity check
propio (6 grupos, 30 tramos, 116 subgrupos, 39 actividades).

Resultado: 4º completo FACTIBLE en 0,232 s, 0 violaciones duras. Suite 48 verde
(47 previos + el nuevo), BUILD SUCCESS, sin regresión (escala-instituto 0,368 s;
4º ordinario 0,098 s).

Dos INFEASIBLE durante la construcción, ambos por error de modelado (no del
dataset), ambos diagnosticados y corregidos antes del cierre:
  1. Carga rechazada: código de subgrupo duplicado (4APDC-EXPRE). Causa: agrupar
     plazas por (asignatura,profesor,aula) fragmenta EXPRE, que rota entre C01 y
     TALL1 según el día. Corrección: agrupar por (asignatura,profesor) y usar
     aulasCandidatas (patrón de S28). Es el aprendizaje de S28 que no se aplicó.
  2. Solver INFEASIBLE: B04 demandaba 47h en 30 tramos. Causa: ámbitos modelados
     como dos linajes separados (uno por PDC), duplicando el aula. Corrección:
     ámbito compartido {4APDC,4DPDC} (el cruce ya lo indicaba desde el principio).

Aprendizaje de método (registrado como deuda de proceso): el cuadre aritmético
30/30 es condición NECESARIA pero NO suficiente de factibilidad; no detecta
sobrecarga de aula/profesor. La batería pre-entrega de fixtures incorpora ahora:
unicidad de códigos, carga por recurso (aula fija y profesor) ≤30, y verificación
de que las plazas compartidas no se dupliquen.

NO cierra criterios de Fase 5: 4º completo NO agrava D4 respecto a 4º ordinario (el
PDC hace EF reintegrado en su grupo, sin presión nueva sobre Gim/Pista). D4 sigue
sin probarse hasta la fusión de niveles. Criterios 1-2 exigen el instituto completo.
Criterio 3 sin cambio. Lectura honesta: cierra el linaje de 4º y valida el Tipo 5 a
escala con dos Di, pero no toca la pregunta de fondo del proyecto.

Deuda nueva: invariante de población del Di en el V4 (reparto en las 5 bandas
calcado del patrón ordinario, sin que el volcado lo demuestre; a confirmar con el
centro). La deuda DIG/TEC/FOPP de 6h de 4º ordinario (S28) sigue viva, el PDC no la
toca.

src/main NO tocado (escala pura; "Nothing to compile") → referencia-codigo-solver.md
NO regenerado. modelo_datos_fase1.md NO tocado (no añade entidad ni invariante; el
Tipo 5 y la regla S23 ya están en §6.2/§6.5; las deudas nuevas viven en este plan).
D3/D4 sin tocar (siguen esperando a la fusión de niveles).

### Sesión 30 — Fase 5, Bloque 9: fusión de niveles 3º+4º ESO (D4 ejercitado).

Primer bloque que FUNDE dos linajes de fixture antes independientes. Decisión de
alcance tomada con el usuario al arrancar: de los cuatro candidatos (fusión de
niveles, Bach a escala, FPB, cerrar criterio 3), se eligió FUSIÓN porque es el único
que ataca la pregunta de fondo —D4 (Gim/Pista compartido entre cursos)— y porque ya
no quedan niveles sueltos baratos que la aplacen sin acoplar. Par 3º+4º elegido sobre
fusión total para atribución limpia: 4º ya satura Gim+Pista internamente (AFAVS en
J3/M5), así que si 3º compite por esos espacios, un INFEASIBLE sería D4 puro y no
ruido de un fixture grande. Acoplamiento escala+estructura aceptado conscientemente
(la fusión no es capa limpia: exige unificar catálogos antes de medir).

Trabajo previo (cruce POR CÓDIGO de los dos fixtures, aplicando el aprendizaje de
S29 "el cruce por código manda sobre la lectura a ojo"):
- 16 profesores con código compartido entre 3º y 4º. Resueltos por convención del
  proyecto (código = persona; los fixtures de 4º traen nombre placeholder, así que el
  nombre no discrimina — la identidad ES el código). Carga tras fusión ≤17h (GH4);
  ninguno satura los 30 tramos.
- 7 aulas compartidas (A12In, A6, A9, B07, Gim, Pista, TALL1): misma definición
  física; las únicas diferencias son cosméticas en el nombre ("Aula 10" vs "Aula
  A10"), unificadas al nombre del linaje instituto.
- 0 colisiones de código de grupo/subgrupo/actividad (los linajes usan prefijo de
  nivel); tramos idénticos entre linajes.
- Cruce físico de Gim/Pista por tramo: demanda combinada 8h (Gim) y 10h (Pista) sobre
  30. La tensión real no es de volumen sino de coincidencia: los dos NEUTRA de 4º
  toman Gim+Pista simultáneos en J3/M5, y 3º trae EF-3A/EF-3C clavadas a Pista
  (aulaFija). Ahí es donde D4 mordería si no hubiera holgura.

Decisión de modelado (Opción B, conservador): fundir SIN relajar nada. La EF de 3º
conserva su aulaFija (no se convierte a aulasCandidatas [Gim,Pista]). Deliberado: un
FACTIBLE conseguido relajando a la vez no distinguiría si la fija habría bastado; un
INFEASIBLE con modelado conservador probaría D4 con atribución perfecta. El segundo
turno (relajar a candidatas, Opción A) sólo se abriría si este test saliera INFEASIBLE.

Construcción y verificación: fixture problema-5-fusion-3-4-eso.json generado
programáticamente desde los dos fixtures (extracción de las 40 actividades de 3º que
tocan {3A,3B,3C,3PDC} vía tocaGrupo + las 39 de 4º; catálogos deduplicados por
código; nombres unificados priorizando el no-placeholder). 10 grupos, 30 tramos, 155
subgrupos (39 de 3º + 116 de 4º, sin solape de códigos), 79 actividades (40 + 39).
Validado contra el schema real y la batería completa: integridad referencial,
unicidad de códigos (incl. plazas global), I2, I7, XOR de aula, aulas fijas disjuntas
por actividad, cuadre por grupo, y carga por recurso ≤30 (máx: profesor 17, aula fija
25 = A10). Test calca SolverHorarioEscala4EsoDiTest (getResourceAsStream, red 600 s,
semilla 42, factibilidad pura + VerificadorSolucion); sanity check propio (10/30/155/
79).

Falso positivo cazado en la validación: el script aplicó 30/30 a TODOS los grupos y
marcó 3PDC=22 como error. No lo es: es la opción 2 de S23 (las 8 sesiones compartidas
ord+Di se imputan al ordinario vía tocaGrupo, no al subgrupo Di). Divergencia
consciente con el PDC de 4º (que cuadra a 30 porque EF/tutoría son plaza conjunta
{4X,4XPDC}): son DOS estilos de modelado de PDC distintos, ambos válidos, ambos
fundidos tal cual. La fusión NO los reconcilia (no era su objetivo). Criterio de
cuadre corregido (22 para 3PDC, 30 para el resto) → batería verde.

Resultado: FACTIBLE en 0,300 s, 0 violaciones duras. Suite 49 verde (48 previos + el
nuevo), BUILD SUCCESS, sin regresión (escala-instituto 0,368 s; 4º completo 0,187 s).
El solver ni se despeina (0,300 s para 10 grupos vs 0,368 para 11), señal de que la
fusión total podría ser computacionalmente abordable.

Lectura honesta del resultado (lo más importante del bloque): D4 NO muerde en el par
3º+4º, ni con modelado conservador. PERO precisión sobre qué se probó: SÍ se probó
que Gim/Pista caben para dos niveles adyacentes aunque 4º los sature simultáneamente
y 3º traiga EF rígida (la holgura existe porque 8h y 10h están lejos de 30). NO se
probó D4 a escala de instituto: la demanda de Pista escala con el nº de grupos, y con
toda la ESO + Bach los 30 tramos podrían dejar de bastar. El verde de hoy dice "D4 no
es un muro entre dos niveles", no "D4 está resuelto". Por eso D4 baja de severidad
(rebajada en su entrada de deuda) pero NO se cierra: la prueba definitiva es la fusión
total, no un par.

NO cierra criterios de Fase 5: criterios 1-2 exigen el instituto completo (el par no
lo es); criterio 3 sin cambio (la fusión no es término del objetivo).

src/main NO tocado (escala+fusión de fixtures, sin dominio nuevo; el log confirma
"Nothing to compile") → referencia-codigo-solver.md NO regenerado. modelo_datos_
fase1.md NO tocado (la fusión no añade entidad ni invariante; D4 vive como deuda en
este plan). D4 ejercitado y rebajado de severidad; D3 sin tocar.

### Sesión 31 — Fase 5, Bloque 10: fusión ESO completa 1º-4º (D4 a escala, residual).

Segundo bloque que funde linajes, y la prueba que el proyecto esperaba desde Fase 1:
D4 (Gim/Pista compartido entre cursos) sometido a competencia REAL. Decisión de
alcance tomada con el usuario al arrancar: de los cuatro candidatos (fusión a mayor
escala, Bach a escala, FPB, cerrar criterio 3), se eligió FUSIÓN A MAYOR ESCALA por
ser el único que ataca la pregunta de fondo, y dentro de ella ESO COMPLETA de golpe
(no par a par): el par 3º+4º de S30 salió en 0,300 s con Gim a 8h sobre 30 —holgura,
no competencia—, y el menor salto que pone Gim/Pista en competencia real es toda la
ESO (la demanda de Pista/Gim escala con el nº de grupos de EF simultáneos). De golpe
sobre par a par porque S30 demostró que el coste dominante es el cruce de catálogos,
no el solver, y trocear lo duplicaría sin ganar atribución. Acoplamiento escala+
estructura aceptado conscientemente (la fusión no es capa limpia).

Trabajo previo (cruce POR CÓDIGO de los dos fixtures, no a ojo):
- 22 profesores con código compartido entre el linaje 1º-3º y el de 4º (vs 16 en el
  par 3º+4º de S30: al sumar 1º/2º aparecen más solapes, esperable). Resueltos por
  convención código=persona; en diferencias de nombre se prioriza el no-placeholder
  (8 venían con placeholder en el fixture de 4º). Carga tras fusión ≤18h; ninguno
  satura los 30 tramos.
- 12 aulas compartidas (A5, A6, A9, A10, A11, A14, A12In, B07, C00, Gim, Pista,
  TALL1): misma definición física; diferencias sólo cosméticas de nombre ("Aula 10"
  vs "Aula A10"), unificadas al nombre del linaje instituto. 4 aulas sólo en 4º
  (A2, A15, B04, C01).
- 0 colisiones de código de grupo/subgrupo/actividad; tramos idénticos. Los códigos
  de PLAZA no son clave de identidad (el mapper no los deduplica): el linaje
  instituto reutiliza B2-PEPA/B2-CyR/B2-Fr2 entre las dos actividades de bloque de 2º
  (Bloque-2ESO-MIE / Bloque-2ESO-VIE); legítimo.
- Cruce físico de Gim por tramo: demanda combinada 26h sobre 30 (vs 8h en el par).
  Pista 10h. La tensión es de margen y coincidencia, no de volumen sumado: Gim deja
  4 tramos libres para 26 sesiones distintas, y los dos NEUTRA de 4º (AFAVS-EFI1 en
  Gim + AFAVS-EFI3 en Pista, J3/M5) están rígidamente acoplados a tramos donde 3º
  trae EF-3A/EF-3C clavadas a Pista (aulaFija). Ahí mordería D4 si fuera a morder.

Decisión de modelado (Opción B, conservador, igual que S30): fundir SIN relajar nada.
La EF de 3º conserva su aulaFija (no se convierte a aulasCandidatas [Gim,Pista]). Un
FACTIBLE conseguido relajando a la vez no distinguiría si la fija habría bastado; un
INFEASIBLE conservador probaría D4 con atribución perfecta. El segundo turno (relajar
a candidatas) sólo se abriría si este test saliera INFEASIBLE.

Construcción y verificación: fixture problema-5-fusion-eso-completa.json generado
programáticamente desde los dos fixtures (catálogos deduplicados por código, nombres
unificados priorizando el no-placeholder; planificación concatenada). 17 grupos, 30
tramos, 232 subgrupos (116+116, sin solape de códigos), 155 actividades (116+39), 217
plazas. Validado contra el schema real y la batería completa: integridad referencial,
unicidad de las 7 colecciones que el mapper deduplica (plaza NO: el mapper no la
impone), I2, I7, XOR de aula, aulas fijas disjuntas por actividad, cuadre por grupo
(16×30 + 3PDC=22 por la opción 2 de S23), y carga por recurso ≤30 (máx: profesor 18,
aula fija 26 = Gim). El loader NO se tocó (genérico). Test calca
SolverHorarioFusion34EsoTest (getResourceAsStream, red 600s, semilla 42, factibilidad
pura + VerificadorSolucion); sanity check propio (17/30/232/155).

Falso positivo de método cazado y corregido durante la construcción: la batería marcó
"unicidad global de plaza" como error (B2-PEPA/CyR/Fr2 duplicados). Se pidió y leyó el
mapper real (ProblemaHorarioMapper): NO deduplica códigos de plaza —ni global ni
intra-actividad—; las plazas se acumulan en List sin comprobarNoDuplicado. La frase
del registro de S29 ("unicidad de códigos incl. plazas global") describía la batería
de aquel bloque, donde el duplicado era síntoma real (EXPRE multi-aula), no una
invariante del sistema. Corrección: la batería se alineó a lo que el mapper valida de
verdad. Aprendizaje: la autoridad es el código, no la lectura del plan.

Resultado: FACTIBLE en 2,110 s, 0 violaciones duras. Suite 50 verde (49 previos + el
nuevo), BUILD SUCCESS, sin regresión (escala-instituto 0,368 s; fusión 3º+4º 0,298 s;
4º completo 0,281 s).

Lectura honesta del resultado: D4 NO muerde ni a escala de ESO completa, ni con Gim a
26/30, ni con modelado conservador. La pregunta de fondo de D4 (¿Gim/Pista compiten de
verdad y rompen?) tiene respuesta: no. D4 baja a RESIDUAL (rebajada en su entrada de
deuda), en observación hasta el instituto completo. NO se cierra del todo: el "instituto
completo" literal de los criterios 1-2 incluye Bach + FPB, aún no fundidos, donde la
demanda combinada de Gim/Pista puede crecer.

Señal nueva (la más importante para vigilar): primer punto de la curva de escala con
crecimiento NO lineal. 0,298 s (10 grupos, par 3º+4º) → 2,110 s (17 grupos, ESO
completa) = ×7 en tiempo por ×1,7 en grupos. Sigue lejísimos del límite (600 s) y del
criterio de 10 min, así que no es alarma hoy; pero conviene medir la pendiente al sumar
Bach y FPB, por si el régimen cambia cerca del instituto completo.

NO cierra criterios de Fase 5: criterios 1-2 exigen el instituto completo (la ESO no lo
es); criterio 3 sin cambio (la fusión no es término del objetivo).

src/main NO tocado (escala+fusión de fixtures, sin dominio nuevo; "Nothing to compile")
→ referencia-codigo-solver.md NO regenerado. modelo_datos_fase1.md NO tocado (la fusión
no añade entidad ni invariante; D4 vive como deuda en este plan). D4 rebajada a residual;
D3 sin tocar.

### Sesión 32 — Fase 5, Bloque 11 cerrado (ESCALA 1ºBACH
  COMPLETO, aislado). Fixture problema-5-escala-1bach.json: 4 grupos ordinarios
  (1BA/1BB/1BC/1BD), 30 tramos, 65 subgrupos, 30 actividades, 16 aulas, 28
  profesores. Primera validación a escala real de las optativas transversales
  Tipo 7 sobre los 4 grupos (OPT1/OPT2 con DTec=4h compartida entre bloques, I6) +
  tres bloques de modalidad sobre subconjuntos (ciencias {A,B}, humanidades {C,D}
  con HMC de profesor distinto por grupo, ECO de dos plazas, GRI sólo en D) +
  Rel/PTVE intra-grupo por grupo. Modelado con subgrupos mono-grupo al estilo
  linaje instituto (no Lectura B N:M de §6.5; divergencia consciente para fusión
  limpia). Población 1 subgrupo/opción (deuda a confirmar con el centro). Cribado
  de aulas y cruce grupo↔aula POR CÓDIGO: 0 celdas sin aula, 0 inconsistencias
  (Hallazgo H y D8 no muerden en Bach). Construido programáticamente desde 4
  volcados de grupo + 11 de aula; población del listado por aula. Validado contra
  schema real + batería completa (integridad, unicidad, I2, I7, XOR aula, aulas
  fijas disjuntas, cuadre 30/30, carga ≤30: máx profesor 12, aula fija 24).
  FACTIBLE, 0 duras. Suite 51 verde, BUILD SUCCESS. INFEASIBLE de método cazado
  durante construcción (conteo de grupo por plaza vs por instancia; corregido).
  NO cierra criterios 1-2 (faltan 2ºBach, FPB y la fusión con ESO). src/main NO
  tocado → índice NO regenerado.

### Sesión 33 — Fase 5, Sub-bloque A de FPB (Bloque 12):
  D13 cerrada en src/main. Lista blanca de inicios de bloque en ModeloCpSat
  (iniciosValidosDeBloque, no-op para duracion=1) + espejo en VerificadorSolucion
  (tramosOcupados, verificarBloquesConsecutivos, verificarNoSolapes por tramo
  ocupado). Decisión (b)+Vía B: D13 cubre cruce de día y de recreo; frontera de
  recreo como constante (deuda nueva D22). SolverHorarioBloqueD13Test (3 casos de
  discriminación). Suite 54 verde, BUILD SUCCESS, sin regresión. src/main tocado →
  índice regenerado. Prerrequisito de FPB; la prueba a escala es el Sub-bloque B.


### Sesión 35 — Fase 5, Sub-bloque C de FPB: fixture 2ºFPB
  real a escala. CIERRA EL NIVEL FPB (junto a 1ºFPB del Sub-bloque B). D13
  ejercitada a MÁS escala que 1ºFPB: 3 bloques de 3 tramos (MEC lun T4-T6, MEC vie
  T4-T6, ELE mié T4-T6) + 5 bloques de 2 + 9 sueltas, sobre 30 sesiones.
  Fixture problema-5-escala-2fpb.json generado programáticamente desde el volcado
  fiel grupo-2-FPB.json (estructura) y aula-Taller-3.json (cruce CyS↔TALL3).
  Aula técnica como TALL_FPB nominal (Opción 1 heredada del Sub-bloque B; Hallazgo
  H confirmado por código: 0 técnicas de 2FPB en TALL3, 25 celdas aula=null). CyS
  en TALL3 real (5 celdas del volcado de grupo coinciden EXACTO con las 5 de 2FPB
  en TALL3). Tutor PAU1 (no PAU2 como en 1FPB). Troceo bloque/suelta por regla
  determinista (validada con el usuario): secuencia contigua maximal misma
  asig+prof sin cruzar recreo = bloque; aisladas = sueltas. El volcado da
  posiciones, no etiqueta el troceo (a diferencia de 1FPB): riesgo Hallazgo K
  asumido. Todo NEUTRA (factibilidad pura; neutraliza D12 —palomar MEC=11, ELE=7—
  y deja D13 como única estructural). Validado contra schema real + batería
  completa (integridad, unicidad de las 7 colecciones que deduplica el mapper,
  I2, I7, XOR aula, aulas fijas disjuntas, cuadre 30/30 por instancia, carga ≤30:
  PAU1 19 cuello, TALL_FPB 25). FACTIBLE en 0,020 s, 0 duras. Suite 56 verde,
  BUILD SUCCESS, sin regresión. src/main NO tocado ("Nothing to compile") →
  índice NO regenerado. NO cierra criterios de Fase 5 (falta 2ºBach y la fusión
  con ESO); SÍ cierra el nivel FPB.


### Sesión 36 — Fase 5, Bloque 13: FUSIÓN INSTITUTO
  COMPLETO (ESO + 1º/2ºBach + FPB en un único fixture). CIERRA CRITERIOS 1-2 de
  Fase 5. 26 grupos (23 ordinarios + 3 PDC), 30 tramos, 341 subgrupos, 229
  actividades, 35 aulas, 59 profesores. 2ºBach plegado dentro de la fusión (no
  bloque aislado previo; aprendizaje S31 de no trocear). Fixture
  problema-5-fusion-instituto-completo.json generado programáticamente: 2ºBach
  derivado de los 3 volcados grupo-2BACH-A/B/C.json (estructura por FIRMA DE
  POSICIÓN: cada bloque NEUTRA ocupa 1 slot del grupo, cuadre 30/30 por
  construcción), fundido con los 4 fixtures cerrados (ESO completa, 1ºBach,
  1ºFPB, 2ºFPB) por unión de catálogos cruzada POR CÓDIGO. 2ºBach NO tiene EF
  (no añade presión D4). Estructura nueva ejercitada: optatividad transversal
  ABC de 4h en dos bloques NEUTRA con DT compartido (I6, como DTec de 1ºBach) +
  modalidades transversales sobre el par B+C (Geogr/Econ/MaCSa) ENTRELAZADAS con
  bloques internos propios de cada grupo (BIOL/Físic de B, HART/Lat2/Gri2 de C);
  plazas de modalidad que rotaban de aula con aulasCandidatas (mecanismo S2 del
  mapper; aulaFija compartida habría sido rechazada). Profesores fundidos por
  código=persona (CLA1/FIL2 en 1º+2ºBach; FIS3 en ESO/Bach/2ºFPB; GH1/FOL3 cruzan
  FPB). HALLAZGO (corrección de datos, no de modelado): TALL_FPB colapsaba los
  talleres de 1ºFPB y 2ºFPB en un código (Hallazgo H; el PDF no detalla talleres
  FPB). Aislados cuadran; fundidos demandan 49 tramos/30 = INFEASIBLE trivial
  (0,1 s). El centro CONFIRMÓ talleres físicos distintos → separados en
  TALL_FPB_1 (24) y TALL_FPB_2 (25). Segunda corrección preventiva: pool de aulas
  de modalidad de 2ºBach ampliado a todas las comunes (holgura de 4→99 tramos)
  para no provocar un INFEASIBLE evitable. CURVA DE COSTE NO LINEAL CONFIRMADA:
  269,4 s (4 min 29 s) sobre los 3,46 s de la ESO completa (S31) = ×78 en tiempo
  por ×1,53 en grupos. Bajo el límite de 10 min (criterio 1) pero con poco techo
  para el régimen de optimización (criterios 3-4). FACTIBLE, 0 duras
  (VerificadorSolucion verde). Suite 57 verde, BUILD SUCCESS. Validado por
  réplica en Python contra loader/mapper/VerificadorSolucion reales ANTES de
  ejecutar (integridad, I2, S2, XOR aula, D13, D12, cuadre 30/30, demanda aula
  fija ≤30, carga profesor ≤23). src/main NO tocado → índice NO regenerado.
  Fixture + SolverHorarioFusionInstitutoCompletoTest.


### Sesión 37 — Fase 5, Bloque 14: OPTIMIZACIÓN A
  ESCALA sobre el instituto completo. PRIMERA medición de resolverOptimizando a 26
  grupos. Reutiliza el fixture del Bloque 13 sin tocarlo (mismo dato, distinto
  régimen: objetivo vs. factibilidad pura). Alcance A1 acordado con el usuario:
  medición pura, sin tocar src/main, sin disponibilidades reales (el fixture no trae
  restriccionesHorarias), sin 6d-a (toca estructura dura) ni 6d-b (reactiva D17).
  RESULTADO: el solver AGOTA los 600 s y devuelve FEASIBLE (NO prueba óptimo) en
  600,9 s wall-clock; ventanas=178, consecutivas=37, indispBlanda=0 (sin dato).
  Factibilidad pura del MISMO fixture en esta máquina: 282,7 s (vs 269,4 s de S36;
  variación de hardware, no de modelo). LECTURA HONESTA: el test es verde pero verde
  NO significa criterio 1 cumplido en optimización — el solver se cortó por TIEMPO,
  no por convergencia; el wall-clock (600,9 s) queda en la frontera de los 600 s SOLO
  porque ahí lo cortamos. CONFIRMA D23 como problema real, no teórico: optimizar el
  instituto completo no converge en 10 min. Los términos (ventanas/consecutivas) son
  el valor en una FEASIBLE cortada por timeout: cota superior pesimista, NO el óptimo;
  no umbralizan el criterio 3 (sigue exigiendo datos del centro). El criterio 3 NO se
  cierra; obtiene su primera medición a escala. Activa las palancas ya previstas en
  D23 para un bloque posterior (candidato natural: warm-start desde la factibilidad
  pura —parte de una buena factible en vez de buscarla desde cero, única palanca que
  no degrada calidad—; alternativas: time-limit con mejora incremental, estrechar
  aulasCandidatas). Suite 58 verde, BUILD SUCCESS, sin regresión. src/main NO tocado
  → índice NO regenerado; modelo NO tocado (el bloque no añade entidad ni invariante).
  Test SolverHorarioOptimizacionInstitutoCompletoTest (calca el de fusión, cambia
  resolver→resolverOptimizando y la aserción de tiempo).

### Sesión 38 — Fase 5, Bloque 15a: OBSERVABILIDAD DEL OBJETIVO (prerrequisito de D23,
  warm-start). Decidido con el usuario partir el frente: 15a expone estado+objetivo
  (esta sesión); 15b será el warm-start. Razón: medir el warm-start exige poder leer
  el objetivo por código y distinguir OPTIMAL probado de FEASIBLE por timeout; sin
  ese canal el verde del Bloque 14 era engañoso.

  Construido (Opción 2, firma vieja intacta): record ResultadoOptimizacion(solucion,
  estado CpSolverStatus, objetivo, cotaInferior) en cpsat; método nuevo
  SolverHorario.resolverOptimizandoConDetalle que lee solver.objectiveValue() y
  solver.bestObjectiveBound(); resolverOptimizando se reimplementa delegando y
  descartando el detalle (cero rotura del contrato). cotaInferior incluida (gratis de
  leer; el gap objetivo−cotaInferior es lo que hace MEDIBLE una FEASIBLE cortada por
  timeout — sin él, una FEASIBLE es un número ciego, el problema del Bloque 14).

  Test SolverHorarioDetalleOptimizacionTest (3 casos, calca SolverHorarioOroFuerte-
  VentanasTest, reutiliza sus dos fixtures): (1) óptimo positivo conocido (=1, hueco
  inevitable por veto) → estado OPTIMAL, objetivo==suma de ventanas del verificador,
  cotaInferior==objetivo; (2) sin veto → óptimo 0 concordante; (3) el método clásico
  y el de detalle dan el mismo coste (la refactorización no cambió el contrato). La
  aserción clave es de CONCORDANCIA: ata el objetivo de CP-SAT al recomputo
  independiente del verificador (autoridad sobre el coste real). Frontera documentada
  en el test: objetivo==suma de ventanas vale SOLO porque estos fixtures activan un
  único término blando (peso 1); no es fórmula general del objetivo. El epsilon del
  bound que se marcó como riesgo NO saltó: isEqualTo exacto valió en OPTIMAL.

  Resultado del bloque (test propio): 3/3 verde en 0,063 s. src/main tocado (firma
  pública nueva) → índice referencia-codigo-solver.md regenerado. modelo NO tocado
  (no añade entidad ni invariante de dominio; ResultadoOptimizacion es tipo de salida
  del solver, no del modelo de datos).

  HALLAZGO (separado de 15a, diferido a bloque de higiene de suite): mvn test completo
  da BUILD FAILURE, pero NO por 15a. Falla SolverHorarioFusionInstitutoCompletoTest
  (no tocado) con Estado CP-SAT: UNKNOWN tras 600 s, en resolver (factibilidad pura).
  Reproducido AISLADO: el MISMO test resuelve FACTIBLE en 86,4 s. La causa es
  contención de CPU: la suite arrastra DOS tests de límite ~600 s (fusión instituto +
  optimización instituto) que, corriendo en la misma máquina, se quitan núcleos a
  CP-SAT (multihilo en factibilidad pura) y el wall-clock se dispara hasta chocar con
  su propio maxTimeInSeconds. No es regresión funcional (código y fixture intactos;
  test aislado verde y más rápido que los 269 s de S36). Es deuda de higiene de la
  suite, agravada por la curva no lineal de D23. SIGUIENTE BLOQUE acordado: etiquetar
  los tests pesados con @Tag("escala") y excluirlos del mvn test por defecto (perfil
  aparte), ANTES del warm-start (15b añadirá otro test pesado; limpiar el banco antes
  de medir sobre él). Ver nueva deuda D24.


### Sesión 39 — Fase 5, Bloque D24: higiene de suite (cierra D24).

  Bloque de higiene previo al warm-start (15b), acordado en S38. Un único cambio
  atómico: etiquetado + exclusión van juntos (etiquetar sin excluir no arregla nada;
  excluir sin etiquetar no tiene a qué apuntar; separarlos dejaría la suite en un
  estado intermedio sin valor). NO toca src/main ni el modelo.

  Diseño (Opción A de D24, parametrizada por property):
  - @Tag("escala") a nivel de clase en SolverHorarioFusionInstitutoCompletoTest y
    SolverHorarioOptimizacionInstitutoCompletoTest, los dos únicos de régimen ~600 s.
    Criterio de etiquetado: límite de tiempo alto + medición a escala, NO discriminación
    rápida. Descartados como escala los tests de varios segundos (FusionEsoCompleta
    3,46 s; EscalaInstituto 0,76 s): no envenenan y son red de regresión barata en cada
    mvn test; sacarlos perdería señal sin ganar tiempo.
  - En el pom del módulo solver: properties surefire.included.groups (vacía) y
    surefire.excluded.groups (=escala); la <configuration> base de Surefire las
    referencia con ${...}; el perfil 'escala' las invierte (incluye escala, vacía la
    exclusión). Mismo cambio en un único fichero (solver/pom.xml).

  Iteración de diseño registrada: el PRIMER intento vaciaba <excludedGroups></excludedGroups>
  como elemento literal dentro de la <configuration> del perfil. FALLÓ — mvn test -Pescala
  dio Tests run: 0. Causa: el merge de un elemento <configuration> vacío sobre la base
  heredada NO es determinista en Maven, y en el JUnit Platform provider la exclusión gana
  a la inclusión (un test a la vez en groups y excludedGroups queda excluido). Corrección:
  parametrizar exclusión/inclusión por property; el merge de properties entre perfil y
  build SÍ es fiable. Aprendizaje: sobrescribir properties, no elementos de plugin.

  Verificación (en la máquina del usuario):
  - mvn test → 59 tests, BUILD SUCCESS, 8,6 s (suite 61 − 2 pesados = 59). Suite rápida
    saneada: ya no se envenena.
  - mvn test -Pescala → 2 tests, BUILD SUCCESS, 11:37 min. Fusión 92,97 s (sana; aislada
    86,4 s en S38, envenenada UNKNOWN/600 s en la suite completa). El contraste confirma
    que el veneno no eran los dos pesados entre sí (juntos dan 93 s), sino esos dos MÁS
    los ~20 rápidos peleando por núcleos al arrancar CP-SAT multihilo. Optimización
    601,007 s, agota el límite por diseño (S37); ventanas=197, consecutivas=29,
    indispBlanda=0 (sin dato) — FEASIBLE por timeout, no comparable término a término
    con los 178/37 de S37.

  CI: confirmado por el usuario que NO hay workflow corriendo mvn test hoy. D24 queda
  local; el diseño deja el gancho para Fase 12 (CI: suite rápida en cada push; perfil
  escala en job nocturno/manual, que alimentará también la cobertura JaCoCo/Sonar de los
  pesados).

  src/main NO tocado → referencia-codigo-solver.md NO regenerado. modelo_datos_fase1.md
  NO tocado (no añade entidad ni invariante; D24 es config de Maven/Surefire + @Tag en
  tests). Suite rápida 59 verde, BUILD SUCCESS. Siguiente frente acordado: warm-start
  (Bloque 15b), la palanca de D23 que no degrada calidad, ahora medible sobre un banco
  de pruebas limpio.


### Sesión 40 — Fase 5, Bloque 15b: WARM-START A ESCALA (deuda D23, palanca que no degrada
  calidad). Alcance acordado con el usuario: opción A (calidad a igual presupuesto de
  optimización, 600 s), bloque entero (sembrado + medición juntos), test autocontenido de
  3 corridas. Por qué A: directamente medible con el canal de S38 sin tocar el criterio de
  parada; B (tiempo a igual calidad) exige maquinaria de parada por umbral que no existe.

  Construido (src/main):
  - ModeloCpSat.sembrarHint(SolucionHorario): inverso exacto de extraerSolucion. Recorre la
    misma lista 'instancias' y siembra via CpModel.addHint las DOS familias de variables de
    decisión primarias: tramoIndex (<- índice del Tramo de la semilla) y presencia de cada
    AulaOpcion (1 en la opción cuya aula coincide con la elegida por la semilla, 0 el resto;
    solo en plazas con aulasCandidatas). Los IntervalVar NO se siembran (anclados a tramoIndex
    y presencia por construcción). Paquete-privado: no abre visibilidad de variables.
  - SolverHorario.resolverOptimizandoConSemilla(problema, semilla): construirConObjetivo()
    .sembrarHint(semilla), mismos params, devuelve ResultadoOptimizacion (estado/objetivo/
    cotaInferior, canal de S38). resolverOptimizando/ConDetalle intactos.
  - Test SolverHorarioWarmStartInstitutoCompletoTest (@Tag escala): mismo fixture del Bloque
    13/14. 3 corridas en el mismo run (la semilla la genera el test vía resolver(), no el
    solver). Aserciones: no-regresión dura sobre la solución caliente (factible + 0 violaciones,
    red independiente) y objetivo caliente <= objetivo frío (un hint factible no debe empeorar
    a igual presupuesto; mejorarlo es deseable pero no se exige por el no-determinismo multihilo
    de CP-SAT).

  RESULTADO (1326 s el test, dentro de @Timeout 1900):
    semilla (factib pura) = 124,8 s
    FRIO:     FEASIBLE  objetivo=215  cota=2  en 600,8 s
    CALIENTE: FEASIBLE  objetivo=204  cota=1  en 600,8 s
  El warm-start MEJORA calidad a igual presupuesto: objetivo 215->204 (-11, -5,1%). La
  aserción <= aguantó holgada. Palanca (c) de D23 confirmada con dato real: funciona, no es
  teórica.

  LECTURA HONESTA (lo más importante del bloque): el warm-start es mejora MARGINAL de calidad,
  NO solución a D23. (1) Ninguna corrida converge: ambas siguen FEASIBLE con cota ~1-2 frente
  a objetivo ~204-215; gap enorme; el hint mejora la solución hallada, no acerca la
  convergencia (la cota 2->1 es ruido multihilo, no degradación). (2) La mejora es pequeña en
  absoluto (-11 sobre 215) y su interpretación es limitada: 215 mezcla ventanas+consecutivas
  (indispBlanda=0, el fixture no trae restriccionesHorarias); no sabemos qué fracción es cada
  término ni si 204 está cerca del óptimo (la cota no da suelo útil; el óptimo real es
  desconocido y probablemente mucho menor). (3) Matiz de producto NO medido: a igual
  presupuesto TOTAL (no de optimización), el warm-start gasta 125 s de semilla que el frío no
  gasta; "-5% a igual presupuesto de optimización" aísla el efecto del hint, no es la decisión
  de producto. Anotado, no concluido.

  NO cierra criterio 3 (sigue exigiendo datos del centro para umbralizar "calidad comparable";
  además ahora se sabe que ni con warm-start se converge). NO cierra D23 (la no-convergencia a
  escala persiste; quedan las palancas: estrechar aulasCandidatas con heurística de aula
  preferente, o aceptar objetivo de calidad relajado). SÍ cierra la palanca (c) de D23 con
  evidencia.

  Verificación (máquina del usuario): mvn test -> 59 verde, 11,4 s (suite rápida intacta).
  mvn test -Pescala -> 3 verde, 34:14 min (fusión 124,7 s; optimización fría agota 600,9 s,
  objetivo 215; warm-start 1326 s con las 3 corridas). src/main tocado -> referencia-codigo-
  solver.md regenerado. modelo_datos_fase1.md NO tocado (sembrarHint es config del solver, no
  añade entidad ni invariante). Commits separados código/doc.


### Sesión 41 — Fase 5, Bloque 16: PODA DE AULA (deuda D23, palanca (b): estrechar
  aulasCandidatas). Frente elegido con el usuario al arrancar entre las rutas vivas que dejó
  abiertas S40: (A) palancas restantes de D23 hacia la convergencia, o (B) camino al criterio 3.
  Se eligió A, y dentro de A la palanca (b) sobre la (a): (b) ataca la causa raíz que D23 nombra
  (espacio de búsqueda ensanchado por candidatas), mientras (a) gestiona el síntoma. La (c)
  warm-start ya se cerró marginal en S40. Diseño: poda DURA (recorta candidatas de verdad,
  reduce variables), no blanda (un hint de aula preferente replicaría el resultado marginal del
  warm-start de S40, que ya siembra esas presencias).

  Medición previa (sobre problema-5-fusion-instituto-completo.json, solo lectura) que fijó el
  alcance ANTES de codear: de 328 plazas, 276 aulaFija y 52 con candidatas (XOR respetado).
  Distribución de tamaño de candidatas: 28 plazas con 2, 2 con 3, 1 con 4, y 21 con 25. Las 21
  de 25 generan el 92% de las 1835 presencias de aula (BoolVar de decisión). Esas 21 son TODAS
  bloques NEUTRA de modalidad/optatividad de 2ºBach — exactamente la estructura que D23 acusaba
  como causa del salto de régimen. Las 25 candidatas son siempre el mismo conjunto: las 35 aulas
  del centro menos las 10 especializadas (aulas comunes genéricas). Conclusión: la palanca (b)
  tiene espacio real y apunta al sitio correcto.

  Suelo de saturación medido (clique máxima de plazas de cola larga mutuamente compatibles, sin
  grupo ni profesor común, que podrían coincidir en un tramo): 3. Implicación: K=8 es seguro con
  margen ×2,6; y el modo de fallo por saturación NO se da en los datos reales del centro (haría
  falta K<3). Por eso el fixture de oro es SINTÉTICO (laboratorio, como los de 6c/6d-c), no
  copiado de 2ºBach: fuerza 9 plazas simultáneas para que K=8 sature.

  Construido (src/main, ModeloCpSat): constantes UMBRAL_PODA_AULA=8 y MAX_AULAS_PODA=8 y campo
  podarAulas (deuda D21 ampliada); flag puesto a true en construirConObjetivo() antes de
  construir(); método private candidatasPodadas(Plaza) que devuelve el conjunto íntegro si poda
  inactiva o |candidatas|<=U, o las K primeras por Comparator.comparing(Aula::codigo) si >U; el
  for de crearOpcionesDeAula itera candidatasPodadas(plaza) en vez de plaza.aulasCandidatas().
  La poda NO toca construir() puro: la curva de escala de factibilidad pura (criterio 1, S36) no
  se altera.

  Fixtures (linaje propio problema-poda-aula-*, padding A01..A12 para que orden lexicográfico ==
  numérico): factible (2 actividades, plaza de 12 candidatas podada a 8, plaza rival con aulaFija
  A01 en el único tramo -> la podada elige en A02..A08 ⊂ recorte) y oro-saturacion (9 actividades
  de grupo/profesor distintos -> mutuamente compatibles -> simultáneas obligadas en 1 tramo; 12
  candidatas cada una; sin poda 9<=12 factible, con poda 8<9 palomar -> infactible). Ambos
  validados contra el schema real y la lógica por enumeración antes de ejecutar.

  Test SolverHorarioPodaAulaTest (paquete cpsat, calca SolverHorarioAulaCandidataTest adaptado a
  la vía de optimización): positivo via resolverOptimizandoConDetalle (estado OPTIMAL/FEASIBLE,
  0 violaciones, aula elegida ∈ {A02..A08}; + resolver() sin poda también factible como
  refuerzo); oro via contraste resolver() factible (sin poda) / resolverOptimizando() lanza
  HorarioInfactibleException (con poda). Confirmado leyendo SolverHorario.java que ambos caminos
  lanzan la excepción ante INFEASIBLE (no devuelven estado): el oro es correcto.

  Verificación (máquina del usuario): mvn test -> 61 verde, BUILD SUCCESS, 11,5 s. Los 2 tests de
  poda en 0,041 s (suite rápida; sin @Tag escala por ser diminutos). Sin regresión. -Pescala NO
  ejecutado esta sesión (no aporta: los dos pesados de instituto completo no se tocaron; su
  medición con poda es el bloque siguiente).

  Lectura honesta (lo más importante): este bloque CONSTRUYE y VALIDA el mecanismo de poda y su
  seguridad (no rompe cuando K basta — positivo; rompe cuando no basta — oro), con atribución
  perfecta. NO prueba que la poda mueva D23: el efecto sobre la convergencia del instituto
  completo (objetivo/tiempo vs. la línea base de S40: 215, 600 s sin converger) se mide en el
  bloque 2 con un test @Tag escala. Interacción poda×warm-start anotada para el bloque 2: la
  semilla de resolver() (sin poda) puede haber elegido un aula que la poda elimina; sembrarHint
  la sembraría 0 en todas las opciones podadas (benigno, no rompe, desaprovecha hint).

  src/main tocado -> referencia-codigo-solver.md regenerado (candidatasPodadas es private: el
  índice no añade firma; regenerado por disciplina, diff solo en fecha/commit). modelo_datos_
  fase1.md NO tocado (la poda es config del solver, no añade entidad ni invariante; mismo criterio
  que warm-start en S40). Commits separados código/test/doc.

### Sesión 42 — Fase 5, Bloque 17: PODA DE AULA MEDIDA A ESCALA — INVIABLE. CIERRA palanca (b)
  de D23 con dato NEGATIVO. Frente fijado por S41: medir la poda construida (S41) sobre el
  instituto completo vs. la línea base de S40 (215, 600 s sin converger), con dos desenlaces
  legítimos abiertos (la poda mueve el régimen, o no lo mueve y D23 -> decisión de producto).

  Diseño acordado antes de codear (configuración A): medir SOLO poda en frío vs. línea base
  frío, lectura por RÉGIMEN no por delta (CP-SAT no determinista en multihilo; un delta fino
  no es atribuible — solo un cambio de régimen lo es: OPTIMAL probado, o cota cerrando gap
  cualitativamente). Confirmado leyendo SolverHorario.java que resolverOptimizandoConDetalle
  -> construirConObjetivo() -> poda activa (S41): el test mide POST-poda sin tocar src/main.

  La medición destapó algo que invalidó la premisa de A: la poda no acelera, ROMPE la
  factibilidad. Cadena de diagnóstico (cada corrida ~10 min, las corrió el usuario; el advisor
  no ejecuta Maven):
    1. -Pescala completo: los TRES tests de optimización (S37, poda, warm-start) dan UNKNOWN.
       Sospecha inicial: contención (HALLAZGO S38). 
    2. Poda AISLADA (-Dtest=...): UNKNOWN. Descarta contención.
    3. Poda aislada + -Djacoco.skip=true: UNKNOWN. Descarta JaCoCo.
    4. Factibilidad pura (resolver, sin poda) del mismo fixture: FACTIBLE 86 s (ya medida).
       Descarta "máquina rota / problema base infactible".
    5. Para aislar poda vs. entorno hizo falta lo que la config A evitaba: una vía SIN poda en
       optimización. Cambio mínimo y reversible en src/main (sobrecarga construirConObjetivo
       (boolean) + método diagnóstico temporal) -> test SIN poda aislado: FEASIBLE objetivo 215
       cota 2. CONCLUYENTE: con poda UNKNOWN / sin poda FEASIBLE, misma máquina/fixture/vía.
       Además 215/cota 2 reproduce EXACTAMENTE la base S40 -> el entorno está sano, S40 es
       reproducible, lo único que rompe es la poda.

  Diagnóstico del fallo de S41: el "suelo de saturación 3 => K=8 seguro con margen ×2,6" medía
  la saturación MÁXIMA en UN tramo (clique de plazas mutuamente compatibles). Eso es condición
  NECESARIA pero NO SUFICIENTE de la factibilidad global de 30 tramos × 341 subgrupos con las
  demás duras entrelazadas. Recortar 25->8 en las 21 plazas acopladas de 2ºBach estrecha el
  espacio de aulas de forma acoplada en toda la semana: un espacio más estrecho pero más difícil
  de explorar, donde la heurística de CP-SAT ya no tropieza con una factible en 600 s.

  DECISIÓN (opción 2 de 3, con el usuario; descartadas: 1 revertir todo — tira el mecanismo
  correcto en sí y su test de discriminación; 3 rediseñar la poda ahora — frente nuevo, caro,
  éxito incierto dado que sin poda tampoco converge): poda DESACTIVADA por defecto
  (construirConObjetivo() delega en construirConObjetivo(false)); mecanismo conservado LATENTE
  (sobrecarga construirConObjetivo(boolean), constantes y candidatasPodadas intactos),
  documentado a fondo en javadoc de ModeloCpSat (por qué está apagado, diagnóstico pareado,
  que reactivar exige rediseño). El método diagnóstico sin-poda se eliminó (A1): tras apagar la
  poda por defecto, resolverOptimizandoConDetalle ya construye sin poda, así que el método
  duplicaba; la capacidad con/sin poda la conserva la sobrecarga package-private.

  Reorganización de tests (A1+B2): eliminados (a) el test de poda a escala creado en esta sesión
  (medía la vía rota), (b) SolverHorarioPodaAulaTest + sus 2 fixtures problema-poda-aula-*
  (discriminación de S41; al apagar la poda perdió su anclaje a producción — fallaba el oro de
  saturación porque resolverOptimizando ya no poda; opción Z: el conocimiento queda en javadoc +
  plan, no en un test verde sobre vía artificial), y (c) SolverHorarioOptimizacionInstituto-
  CompletoTest de S37 (vía pelada, javadoc con afirmaciones obsoletas). Nuevo
  SolverHorarioOptimizacionEscalaInstitutoCompletoTest (@Tag escala): lee ResultadoOptimizacion
  (estado/objetivo/cota, canal S38), sustituye al de S37 con instrumentación más rica.

  Verificación: suite rápida 59 verde, BUILD SUCCESS, 11,8 s (el javadoc no cambia
  comportamiento; recompila y pasa). El usuario corrió además -Pescala completo (no requerido):
  confirma D25 — fusión 139 s FACTIBLE, optimización fría 601 s UNKNOWN, warm-start completa
  FEASIBLE (frío 222, caliente 207); el test nuevo es flaky en el perfil entero (verde aislado,
  rojo en suite) por contención, NO por su lógica.

  ALTA de deuda D25 (reactivación agravada de D24): el perfil -Pescala corrido entero no pasa
  por contención de CPU (4 núcleos físicos, i7-4790K; CP-SAT lanza ~8 workers por test). @Tag
  solo separó los pesados de la suite rápida; no resuelve la contención ENTRE ellos. Los tests
  de optimización a escala solo son fiables AISLADOS. Frente futuro propio (forkear por clase /
  limitar workers / serializar); cada intento 30+ min, no se aborda al cierre de sesión.

  ESTADO DE D23 TRAS S42: palancas (b) poda CERRADA-inviable y (c) warm-start CERRADA-ayuda-no-
  resuelve; viva solo (a) límite de tiempo con mejora incremental (sin promesa de convergencia).
  El advisor recomienda cerrar D23 como DECISIÓN DE PRODUCTO (aceptar FEASIBLE ~215 sin
  optimalidad probada) — desenlace que el plan dejó legítimo desde S36 —; pendiente de decidir
  con el dueño del proyecto. src/main tocado (default de poda + javadoc; el método diagnóstico
  no quedó en el árbol commiteado) -> índice regenerado; modelo NO tocado (la poda es config del
  solver, no añade entidad ni invariante). Commits separados: código (fix), índice (docs), plan
  (docs).

### Sesión 43 — Fase 5, Bloque 18: EXPERIMENTO PAREADO DE ATRIBUCIÓN — la no-convergencia
  de la optimización a escala es ESTRUCTURAL, no atribuible a un bloque. CIERRA D23 como
  DECISIÓN DE PRODUCTO. El frente: falsar si FPB endurece la optimización a escala (no la
  factibilidad). Método de S42: una hipótesis, experimento pareado, atribución limpia. Tres
  puntos sobre el mismo fixture (problema-5-fusion-instituto-completo.json) recortado EN
  MEMORIA por bloque académico (catálogo idéntico; frontera ESO/Bach/FPB separable sin
  referencias colgantes, verificado por réplica: 0 subgrupos y 0 actividades cruzan), cada
  uno aislado (D25) tras -Pescala. RESULTADO: P0 base 26 grupos -> FEASIBLE obj 221 cota 0;
  P1 sin FPB 24 grupos -> FEASIBLE obj 216 cota 2; P2 solo ESO 17 grupos -> FEASIBLE obj 62
  cota 0. LECTURA (sobre estado/cota, no objetivo absoluto por varianza D25 ±7): (1) FPB
  inocente — P0->P1 no cambia de régimen; (2) Bach domina la MAGNITUD del objetivo (216->62
  al quitarlo, colapso 3,5x, no ruido) PERO quitarlo NO da convergencia — P2 sigue FEASIBLE
  cota 0 gap 62; (3) la incapacidad de cerrar la cota ya está presente con solo ESO, luego es
  propiedad del MODELO a escala, no de un bloque. Descartadas las dos hipótesis localizables.
  DECISIÓN DE PRODUCTO (firmada por el dueño): FEASIBLE sin optimalidad probada es el modo de
  operación aceptado del solver a escala; un horario con objetivo ~221 es usable, la prueba de
  optimalidad no aporta valor aquí. D23 CERRADA (ya no es deuda con palanca pendiente). NO
  cierra el criterio 3 (sigue exigiendo umbral con datos del centro). Test nuevo
  SolverHorarioOptimizacionEscalaSubconjuntosTest (@Tag escala, 3 métodos p0base/p1SinFpb/
  p2SoloEso, recorte en memoria con fail-fast de frontera), convive con el de instituto
  completo. Suite rápida 59 verde, BUILD SUCCESS, 9,2 s. src/main NO tocado -> índice NO
  regenerado; modelo NO tocado.


