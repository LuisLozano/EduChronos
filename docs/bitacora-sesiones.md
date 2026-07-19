# Bitácora de sesiones — Educhronos

Registro detallado e histórico de las sesiones de trabajo S10–S73. Archivado
desde `plan_trabajo_horarios.md` en la Sesión 44 (higiene documental) para
aligerar el plan de trabajo, conservando la traza completa de decisiones.

**Jerarquía de autoridad**: el estado VIVO del proyecto (fase actual, criterios,
deuda consciente abierta, decisiones permanentes, bloques de fase) está en
`plan_trabajo_horarios.md`. Esta bitácora es histórico de solo lectura: no se
consulta para conocer el estado actual, sino para entender por qué se tomó una
decisión pasada. Las cabeceras vivas de sesión las conserva el plan; aquí se
archivan conforme salen de su ventana.

Orden: cronológico ascendente (S10 → S73). Los formatos difieren según la época
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


### Sesión 44 — Fase 5, CIERRE DE FASE. Sub-bloque de respaldo
  descriptivo + cierre de criterios 3 y 4 como decisión de producto gemela de D23.
  Con D23 cerrada (S43), los criterios 3 (calidad comparable) y 4 (ventanas no
  excesivas) quedaban bloqueados por una MISMA causa: no hay umbral sin datos del
  centro; no es trabajo técnico pendiente. Sub-bloque de respaldo (Variante A): el
  test SolverHorarioOptimizacionEscalaInstitutoCompletoTest ya recomputaba los tres
  términos blandos —no hizo falta código nuevo, solo correrlo aislado (D25) y
  registrar la salida. EVIDENCIA (instituto completo real, 26 grupos, no el recorte
  en memoria de S43): FEASIBLE objetivo 219,0 cota 2,0 gap 217,0 en 601,6 s; ventanas
  =196, consecutivas=23, indispBlanda=0; 196+23+0=219 valida el recomputo del
  verificador contra el objetivo del solver (PESO_*=1, D21). Lectura honesta: 196
  ventanas es ALTO para ~28 profesores sobre 30 tramos; sin dato del centro no se
  umbraliza, pero se registra. DECISIÓN: criterios 3 y 4 CERRADOS como decisión de
  producto (umbral = configuración del centro en despliegue, no requisito de
  desarrollo); FASE 5 CERRADA. Sin código nuevo: src/main NO tocado -> índice NO
  regenerado; modelo NO tocado; pom NO tocado. Solo documentación. Siguiente: Fase 6
  (persistencia). Test corrido aislado por D25, BUILD SUCCESS, 10:04 min.


### Sesión 45 — Fase 6, Bloque 1: ANDAMIAJE DEL MÓDULO app/ +
  HUMO DE PERSISTENCIA. Arranque de Fase 6 en modo híbrido (decisión y cierre en el
  Project; teclear/compilar en Claude Code). Se crea el módulo Maven app/ (declarado
  en el pom raíz, como anticipaba la decisión táctica de Fase 2: el módulo app/ se
  declara al entrar en Fase 6, no antes). Stack de persistencia fijado y validado:
  Spring Boot 4.1.0 (GA) + Hibernate 7.4.1 + driver SQLite + dialecto de comunidad
  org.hibernate.community.dialect.SQLiteDialect. El contexto Spring arranca, abre
  SQLite en fichero local (ruta relativa al working dir vía application.properties:
  spring.datasource.url=jdbc:sqlite:educhronos.db; SQLite 3.53.2) e Hibernate genera
  el esquema vía hbm2ddl sobre una HumoEntity desechable. RIESGO CERRADO EN POSITIVO:
  el dialecto de comunidad (best-effort, sin dialecto SQLite oficial en Hibernate)
  funciona sobre Hibernate 7.4 sin tocar nada. Frontera respetada: dependencia de
  módulo app -> solver (unidireccional; solver no toca app); el modelo del solver
  permanece libre de JPA (HumoEntity es desechable, NO es del modelo real). NO entran
  en este bloque entidades del modelo real, mapper ni CRUD (Bloque 2 en adelante).
  Test de integración: levanta el contexto, verifica que el .db se crea en disco y lo
  limpia en @AfterAll (working tree sin .db residuales; .gitignore ignora *.db). suite
  rápida verde: solver 59 + app 1, BUILD SUCCESS. src/main del solver NO tocado ->
  índice NO regenerado; modelo NO tocado (la entidad de humo no cuenta como entidad de
  dominio). Commits separados código/doc, de una línea. Siguiente: Bloque 2 (catálogo
  del centro como entidades JPA + repos + mapper entidad->dominio; alcance a acordar).


### Sesión 46 — Fase 6, Bloque 2: CATÁLOGO DEL CENTRO COMO
  ENTIDADES JPA + REPOSITORIOS. Implementa el catálogo §4.1 del modelo como entidades
  JPA en el módulo app/ (paquete app.catalog), sin mapper ni capa web. Corte acordado
  en el Project: "una capa por bloque"; el mapper Entidad->modelo del solver se separa
  al Bloque 3 (las entidades deben estar estables antes de mapearlas, misma disciplina
  que dejó el mapper fuera del Bloque 1). Entran 8 entidades: Nivel, GrupoAdministrativo,
  Profesor, Aula (con todos sus campos: tipo, capacidad, edificio, planta, sector),
  Asignatura, AsignaturaAulaCompatible, TramoSemanal, Configuracion; + 3 enums propios
  de la capa JPA (TipoGrupo con 3 valores ORDINARIO/DIVERSIFICACION_PDC/VIRTUAL_OPTATIVA,
  TipoAula con 9, Dia) + 8 repositorios Spring Data. Decisiones de modelado: id sintético
  en todas salvo Configuracion (clave natural); AsignaturaAulaCompatible con id sintético
  + @UniqueConstraint(asignatura, tipo_aula); enums @Enumerated(STRING); FK
  autorreferenciales @ManyToOne(LAZY) en GrupoAdministrativo.grupoPadre y
  TramoSemanal.siguienteInmediato. ASIMETRÍA CONSCIENTE (la vigila el mapper de Bloque 3):
  app.catalog.TipoGrupo tiene 3 valores, solver.domain.TipoGrupo solo 2 (ORDINARIO,
  DIVERSIFICACION_PDC); VIRTUAL_OPTATIVA no existe en el solver. Los enums TipoGrupo y Dia
  de app/ son PROPIOS por diseño (frontera "entidad JPA con su forma, modelo del solver
  con la suya"), NO duplicados a deduplicar; el javadoc del enum ya lo documenta. RIESGO
  CERRADO EN POSITIVO: LocalTime de TramoSemanal viaja a SQLite y vuelve intacto, sin
  fallback a String (era la incógnita del dialecto de comunidad al planificar). Frontera
  respetada: entidades en app/, CERO anotaciones JPA en solver/; dependencia app -> solver
  unidireccional; modelo del solver libre de JPA. HumoEntity y PersistenceSmokeTest del
  Bloque 1 retirados, sustituidos por un test de round-trip (@DataJpaTest + replace=NONE
  sobre la SQLite real) que persiste y recupera filas de cada entidad y verifica relaciones
  (Grupo->Nivel, Grupo->grupoPadre, Tramo->siguienteInmediato, Asignatura<->tipoAula).
  IMPREVISTO DE STACK (ver "### Notas técnicas validadas en Fase 6"): Spring Boot 4
  modularizó los test slices; @DataJpaTest ya no viene en spring-boot-starter-test (hubo
  que añadir spring-boot-starter-data-jpa-test en scope test y corregir paquetes SB4).
  Commit de código 612a70e (una línea). suite rápida verde: solver 59 + app 1 (round-trip),
  BUILD SUCCESS. src/main del solver NO tocado -> índice NO regenerado; modelo NO tocado.
  Commits separados código/doc, de una línea. Siguiente: Bloque 3 (primer tramo del mapper
  Entidad JPA -> modelo del solver, limitado a las entidades de catálogo que el solver
  consume: Aula, Asignatura, Profesor, Grupo, Tramo).


### Sesión 47 — Fase 6, Bloque 3: MAPEO CATÁLOGO ENTIDAD JPA -> MODELO DEL SOLVER (CatalogoMapper).

Construido en modo híbrido (decisión y
  cierre en el Project, código en Claude Code). Entrega CatalogoMapper (app/,
  paquete app.mapper — clase final, ctor privado, métodos estáticos, mismo estilo
  que ProblemaHorarioMapper) con cinco conversiones JPA->domain: aAula,
  aAsignatura, aProfesor (directos; nombreCompleto->nombre), aGrupo (grupoPadre
  resuelto por referencia de objeto recursiva; I5 la valida el record de
  dominio) y aTramos(List<TramoSemanal>) a nivel de LISTA (excluye
  es_lectivo=false/recreo -> domain.Tramo no porta ese flag y el schema JSON ya
  confirmaba ordenEnDia 1..6 sin hueco para el recreo; ordena por día+orden
  global; renumera ordenEnDia 1..6 reiniciando por día; sintetiza codigo
  "L1".."V6"). DECISIÓN CERRADA EN EL PROJECT ANTES DE CONSTRUIR: VIRTUAL_OPTATIVA
  lanza excepción explícita (IllegalArgumentException) en vez de mapear a
  ORDINARIO o filtrar en silencio — ningún caso validado en modelo_datos_fase1.md
  usa GrupoAdministrativo virtual para optativas (el patrón real, Lectura B, ya
  se resuelve con Subgrupo N:M sobre grupos ORDINARIO); fallo ruidoso evita
  pérdida silenciosa de un grupo real si el tipo llega a usarse. Nivel no se
  mapea (el dominio del solver no lo necesita). Aula pierde tipo/capacidad/
  edificio/planta/sector (deuda ya documentada, distancia entre aulas pendiente
  en el solver). Tramo.siguienteInmediato no se mapea (la adyacencia de bloques
  FPB la resuelve ModeloCpSat, D13, no este campo).
  DOS HALLAZGOS DE CÓDIGO REAL que el Project había dejado como pregunta abierta,
  resueltos por Claude Code al leer las entidades JPA reales antes de teclear:
  (1) Aula (JPA, S46) NO tiene campo nombre — ni la entidad ni §4.1 lo listan,
  pero el schema JSON exige Aula.nombre no nulo. Resuelto con nombre=codigo
  (Opción 1), documentado en el mapper como deuda consciente (ALTA D26). (2) el
  código de Tramo ("L1".."V6") es una clave SINTETIZADA por el mapper: el modelo
  no define una convención de código de TramoSemanal reutilizable por
  domain.Tramo (ALTA D27).
  Tests: CatalogoMapperTest, 7 casos unitarios puros (entidades JPA en memoria,
  sin Spring/BD) — un caso por método (incluyendo aGrupo con grupoPadre PDC),
  VIRTUAL_OPTATIVA lanza, y aTramos (recreo excluido, ordenEnDia reiniciado por
  día, código sintetizado). Suite verde: solver 59 + app 8 (CatalogoRoundTripTest
  1 del Bloque 2 + CatalogoMapperTest 7), BUILD SUCCESS. src/main del solver NO
  tocado -> índice NO regenerado; modelo NO tocado (el mapper no añade entidad ni
  invariante al dominio del solver). Commit de código c3ec500
  (una línea). Commits separados código/doc. Siguiente:
  Bloque 4 (a decidir; candidato natural:
  Subgrupo/Actividad como entidades JPA, prerrequisito para poder ensamblar
  ProblemaHorario completo).

### Sesión 48 — Fase 6, Bloque 4: SUBGRUPO COMO ENTIDAD JPA
  (§4.2) + TRAMO DE MAPPER. Construido en modo híbrido (decisión y cierre en el
  Project, código en Claude Code). Candidato tentativo de cierre de B3 ("Subgrupo
  y Actividad juntos") PARTIDO en tres bloques por dependencia técnica: Plaza
  (JPA) referencia Subgrupo, que debe estar estable y con round-trip verde antes
  de mapear Actividad. B4=Subgrupo, B5=Actividad/Plaza, B6=ensamblado de
  ProblemaHorario (el prerrequisito que motivaba el candidato se alcanza en B6
  sobre cimientos probados por capas, no en un B4 monolítico). Cinco decisiones
  cerradas en el Project ANTES de construir: D-a Particion/SubgrupoParticion
  DIFERIDAS a Fase 8 (el dominio del solver no las consume; su UX se diseña con
  la UI, deudas D1/D7); D-b id sintético + codigo único como el resto del
  catálogo; D-c/D-d @ManyToMany unidireccional Subgrupo->GrupoAdministrativo,
  Subgrupo dueño, LAZY (primer @ManyToMany del proyecto); D-e mapper en
  CatalogoMapper (no clase nueva), reutilizando la resolución de grupos.
  ENTREGA: entidad Subgrupo (app.catalog, join table subgrupo_grupo) +
  SubgrupoRepository + CatalogoMapper.aSubgrupo(entidad, gruposPorCodigo) que
  resuelve la población por identidad de objeto (mismo patrón que aGrupo con
  grupoPadre) y aborta con IllegalArgumentException ante grupo huérfano.
  RIESGO CERRADO EN POSITIVO (D-c, gemelo del LocalTime de B2): el @ManyToMany
  sobrevive el round-trip sobre SQLite real con el dialecto de comunidad, tanto
  multi-grupo (Lectura B, 3 filas en subgrupo_grupo) como mono-grupo. Frontera
  app->solver intacta (cero JPA en solver/). Tests: SubgrupoRoundTripTest (2,
  @DataJpaTest sobre SQLite real) + CatalogoMapperSubgrupoTest (2, unitario puro).
  APRENDIZAJE DE PROCESO (reutilizable en B5/B6): (1) fabricar tipos solver.domain
  con `new` en un test de app/ NO es el patrón; el correcto es consumir la salida
  del mapper (CatalogoMapper.aGrupo), como CatalogoMapperTest de B3 — más fiel y
  sin sorpresas de carga de clase. (2) iterar tests con `mvn test -pl app` en
  aislamiento asume el jar de solver ya instalado en ~/.m2; tras un `mvn clean`
  que borra solver/target, app pierde solver.domain en compilación de test. El
  cierre de bloque se valida con `mvn clean test` desde la raíz (reactor completo),
  no con -pl. Suite: solver 59 + app 12 (CatalogoRoundTripTest 1 + CatalogoMapperTest
  7 + SubgrupoRoundTripTest 2 + CatalogoMapperSubgrupoTest 2), BUILD SUCCESS.
  src/main del solver NO tocado -> índice NO regenerado; modelo NO tocado (el
  Subgrupo de dominio y su Set<GrupoAdministrativo> ya existían desde Fase 5). Commit
  de código 09b7b77 (una línea). Commits separados código/doc. Siguiente: Bloque 5

### Sesión 49 — Fase 6, Bloque 5: ACTIVIDAD Y PLAZA COMO ENTIDADES JPA (§4.6) + MAPPER.

Modo híbrido. Entidades Actividad (agregado raíz, @OneToMany cascade+orphanRemoval)
y Plaza (dependiente; @ManyToOne opcional aula_fija + tres @ManyToMany) en app.catalog
+ enum propio PatronTemporal + ActividadRepository + CatalogoMapper.aActividad/aPlaza/
aPatronTemporal (entidad a entidad). Seis decisiones cerradas antes de construir (D-B5-1
a D-B5-6; ver la entrada del Bloque 5 en "### Bloques de Fase 6" del plan). Clave: D-B5-1
ActividadInstancia NO se materializa como tabla (artefacto derivado que expande
cpsat.Expansion en runtime; §4.7 decidirá su identidad persistida); D-B5-6 PatronTemporal
propio de app.catalog, no reutilización del de dominio (el compilador forzó el traductor
aPatronTemporal, validando la frontera). TRES RIESGOS DE PERSISTENCIA CERRADOS EN POSITIVO
por round-trip SQLite real: cascade Actividad→Plaza, densidad aula_fija + tres @ManyToMany
(primera entidad con esta densidad), ambas ramas del XOR. Suite: solver 59 + app 20 (+8:
ActividadRoundTripTest 3 + CatalogoMapperActividadTest 5), BUILD SUCCESS con mvn clean test
desde la raíz (reactor completo, no -pl). src/main del solver NO tocado -> índice NO
regenerado. Commits separados código/tests/doc, de una línea. Siguiente: Bloque 6 (ensamblado
de ProblemaHorario completo).

### Sesión 50 — Fase 6, Bloque 6: ENSAMBLADO DE ProblemaHorario (JPA → dominio del solver).

Modo híbrido (decisión y cierre en el Project, código en Claude Code).
CatalogoMapper.aProblemaHorario ensambla el ProblemaHorario completo desde las siete
listas de entidades JPA del catálogo, construyendo en producción los cinco índices por
código (consumiendo la salida del propio mapper) en orden de dependencia; código
duplicado aborta (IllegalStateException). Cinco decisiones cerradas (D-B6-1 a D-B6-5);
la de más impacto, D-B6-3: restriccionesHorarias diferida con List.of() porque no existe
entidad JPA que las persista (deuda D28 nueva; el dato solo entra por el camino JSON,
RestriccionHorariaDto + ProblemaHorarioMapper.aDominio). Firmas y orden del record
confirmados contra el repo real: sin desajustes, no hubo que parar. CatalogoMapperProblemaTest
(6 casos, app.catalog por ctor protected): feliz con recreo intercalado, coherencia
subgrupo↔grupo por equals estructural (S9), huérfanos en top-level (D-B6-1), restricciones
vacías (centinela D28), aula duplicada y actividad huérfana abortan. Suite solver 59 + app 26,
BUILD SUCCESS con mvn clean test desde la raíz. src/main del solver NO tocado (mapper en app/)
→ índice NO regenerado; modelo NO tocado. Commits separados código/tests be80f90/90da600,
de una línea; commit de doc pendiente. Siguiente: Bloque 7 (candidatos: entidad JPA de
restricciones horarias —cierra D28— o servicio de orquestación repos→mapper→solver).

### Sesión 51 — Fase 6, Bloque 7: ENTIDAD JPA DE RESTRICCIONES HORARIAS (§4.3) + MAPPER.

Modo híbrido (decisión y cierre en el Project, código en Claude Code). Cierra la deuda D28: el camino JPA ensambla ya un ProblemaHorario COMPLETO. Entidad ProfesorRestriccionHoraria (app.catalog, calcada de TramoSemanal: id IDENTITY, dos @ManyToOne LAZY a Profesor y TramoSemanal nullable=false, @Enumerated(STRING) tipo, int peso, String motivo nullable) + enum propio app.catalog.TipoRestriccion {DURA,BLANDA} + ProfesorRestriccionHorariaRepository vacío. CatalogoMapper: aTipoRestriccion (gemelo de aPatronTemporal), aRestriccionHoraria (profesor por código, tramo por REFERENCIA de objeto vía IdentityHashMap, tramo ausente/recreo aborta, motivo → Optional.ofNullable), refactor de aTramos SIN cambio observable (helper aTramosConIndice devuelve List<Tramo> + IdentityHashMap<TramoSemanal,Tramo>; aTramos público delega), y aProblemaHorario gana el 8º parámetro List<ProfesorRestriccionHoraria>, sustituyendo el List.of() centinela. Decisión de alcance tomada al inicio entre los dos candidatos de B7: entidad (esta) frente a servicio de orquestación; se eligió la entidad porque el servicio (B8) debe orquestar un mapper YA completo, no uno con el agujero de D28. El tramo se resuelve por referencia de objeto, NO por el código sintético L1..V6 (D27 no se reabre) ni por (día,ordenEnDia) —TramoSemanal porta orden GLOBAL con recreo, no ordenEnDia—. El peso DEFAULT 1 de §4.3 NO se materializa en la entidad (política de la UI de Fase 8; la entidad exige peso explícito). RIESGO CERRADO EN POSITIVO: primer @ManyToOne a TramoSemanal no autorreferencial sobrevive el round-trip sobre SQLite real. Tests: ProfesorRestriccionHorariaRoundTripTest (@DataJpaTest, DURA+BLANDA) + CatalogoMapperRestriccionTest (unitario, 5 casos incl. recreo aborta) + centinela de D28 en CatalogoMapperProblemaTest reconvertido a verificar restricción real. src/main del solver NO tocado (record domain.RestriccionHoraria ya existía desde S25) → índice NO regenerado; modelo NO tocado. Suite: solver 59 + app 32 (+6), BUILD SUCCESS con mvn clean test desde la raíz. Commits separados código/tests 84a8bed/f2e81a7, de una línea; commit de doc pendiente. Siguiente: Bloque 8 (servicio de aplicación repos → mapper → solver, sobre ensamblado completo).

### Sesión 52 — Fase 6, Bloque 8: SERVICIO DE APLICACIÓN, ORQUESTACIÓN repos → mapper → solver.

Modo híbrido (decisión y cierre en el Project, código en Claude Code). GeneradorHorarioService (app.service, @Service, primer bean de servicio del módulo; constructor injection, 8 repos private final). cargarProblema() @Transactional(readOnly=true) ejecuta los 8 findAll() + el mapeo en una sola transacción (las relaciones LAZY se navegan por identidad de objeto: sin transacción habría LazyInitializationException, y proxies de sesiones distintas romperían la resolución por referencia EN SILENCIO). generar() SIN transacción: llama a cargarProblema() y, sobre el ProblemaHorario ya desligado de JPA, invoca new SolverHorario().resolverOptimizandoConDetalle → ResultadoOptimizacion (mantener la transacción abierta durante la resolución sería antipatrón: el solver tarda). Cuatro decisiones cerradas (D-B8-1 a D-B8-4): la de más impacto, la frontera transaccional entre carga (dentro) y solver (fuera); el servicio no valida integridad (la aborta el mapper, propaga) ni guarda catálogo vacío (Fase 8); constructor por defecto del solver (120 s/semilla 42), que a escala de instituto dará FEASIBLE, no OPTIMAL (deuda D29). Test GeneradorHorarioServiceTest (@DataJpaTest + @Import, SQLite real): catálogo mínimo con grupo PDC (grupoPadre no nulo) y restricción horaria sobre TramoSemanal real, recargado por el servicio, verificando el enlace de padre y tramo por identidad —ejercita el fallo silencioso por identidad de objeto, el riesgo dominante del bloque—. Caso de generar() omitido conscientemente (exigiría catálogo factible garantizado; el valor está en la carga+frontera, no en re-testear el solver). src/main del solver NO tocado → índice NO regenerado; modelo NO tocado. Suite: solver 59 + app 33 (+1), BUILD SUCCESS con mvn clean test desde la raíz. Commits separados código/tests f72ca82/44694d1, de una línea. Siguiente: Bloque 9 (a decidir).

### Sesión 53 — Fase 6, Bloque 9: PERSISTENCIA DE LA SolucionHorario (§4.7).

Modo híbrido (decisión y cierre en el Project, código en Claude Code). Cierra el criterio abierto de Fase 6 ("cerrar la app, reabrir, el horario intacto"): B8 dejó el flujo entrada->solver; B9 cierra solver->persistencia, la segunda mitad del entregable. Entidades nuevas HorarioGenerado y Sesion (§4.7, app.persistence) + enum propio EstadoHorario {BORRADOR,DEFINITIVO,DESCARTADO} + dos repos. Mapper de salida SolucionMapper (dominio->JPA): por cada (instancia, plaza) del ProblemaHorario emite una fila Sesion, resolviendo plaza y aula a entidad JPA por CÓDIGO (dos índices Map<String,_> recargados en la transacción de escritura, no por identidad de objeto: la solución es dominio desligado de JPA tras B8). Servicio: generar() SIGUE puro (sin BD, solver fuera de transacción, respeta D-B8-1); método nuevo guardar(ResultadoOptimizacion, ProblemaHorario, String) @Transactional que persiste HorarioGenerado + N Sesion; cargarHorario(id) devuelve entidades JPA (lo que pinta Fase 7), no reconstruye SolucionHorario de dominio. Ocho decisiones cerradas antes de construir (D-B9-1 a D-B9-8): la de más impacto, la granularidad Sesion = PLAZA colocada (Opción A), que CORRIGE el UNIQUE de §4.7 (era por actividad_instancia, imposibilitaba el desdoble) a (horario_id, plaza_id, indice), y deriva la actividad desde la plaza (sin FK redundante). HorarioGenerado persiste estado_solver (String, sin acoplar a CpSolverStatus) + objetivo/cotaInferior (Double NULLABLE real, no centinela). Aula por vía uniforme aulaElegida().orElseThrow() (aborta ruidoso si empty, bug de solver). Test de integración con actividad MULTI-PLAZA (desdoble) como caso central: guardar->recargar, las 2 filas Sesion sobreviven con tramo y aula. src/main del solver NO tocado -> índice NO regenerado; §4.7 del modelo CORREGIDO (Sesion + HorarioGenerado). Suite: solver 59 + app 35 (+2 del IT), BUILD SUCCESS con mvn clean test desde la raíz. Commits separados código/tests aaa660a/84cdba5, de una línea.

### Sesión 54 — Fase 6, CIERRE DE FASE.

Modo híbrido (decisión y cierre en el Project, código en Claude Code). NO es un bloque de persistencia nuevo: el alcance se decidió al inicio entre (a) cerrar Fase 6 y (b) un Bloque 10 (candidato SesionBloqueada §4.7); se eligió (a) porque un B10 de persistencia no acerca el cierre —SesionBloqueada es inútil sin que el solver la consuma (C5, toca src/main del solver, reabre superficie estabilizada)— y dejaba igualmente sin firmar los criterios 2 y 4. Entregable: un único test de integración de humo end-to-end CierreFase6HumoTest (app.catalog, por los ctor protected de Actividad/Plaza; @DataJpaTest + replace=NONE + @Import(GeneradorHorarioService)). Ejercita el pipeline COMPLETO por primera vez de una tirada: repos JPA → CatalogoMapper.aProblemaHorario → SolverHorario real → SolucionMapper → guardar → recargar. Fixture: builder JPA que TRANSCRIBE problema-3-cierre-cyr-refmt.json (agrupamiento denso de 1ºESO: bloque de 6 plazas CyR/OyD/RefMt rep=2 + 4 Mat) como ESPECIFICACIÓN de referencia; el JSON NO se carga (entraría por el camino JSON, que no ejercita JPA) —se replica a new+save, patrón de GuardarHorarioServiceTest—. El builder añade los 5 recreos (esLectivo=false) que el JSON omite, intercalados por orden global, porque el puente de tramo (SolucionMapper.indiceTramos) los espera para renumerar (D30). Nueve decisiones cerradas antes de construir (D-B10-1 a D-B10-9). Las de más impacto: D-B10-7 el test orquesta cargarProblema()+new SolverHorario(10,42)+guardar() SIN tocar el servicio (evita meter el solver a 120 s en la suite, veneno de D24/D25); D-B10-9 vía de factibilidad; D-B10-8 testigo del criterio 2 = alta de restricción BLANDA (no editar Profesor —inmutable— ni Actividad —alteraría factibilidad—). NOTA de honestidad: objetivo y cotaInferior de ResultadoOptimizacion son double PRIMITIVOS (verificado leyendo el repo), no admiten null; el test recorre por eso resolverOptimizandoConDetalle y persiste objetivo/cota reales —NO se ejercita el caso NULLABLE de §4.7, contra lo previsto—. ModeloCpSat NO restringe por tipo de aula (I3 no participa en CP-SAT): todas las aulas ORDINARIA, sin INFEASIBLE. Cobertura ganada respecto a B9: plaza con aulasCandidatas resuelta por el solver (B9 solo probó aula fija). D30 NO queda verificada: la aserción "toda sesión en tramo lectivo" descarta que una sesión caiga en recreo, pero no que se empareje al TramoSemanal lectivo EXACTO que el solver eligió; D30 sigue viva (Fase 8). Los 4 criterios de Fase 6 firmados (ver "### Criterios de verificación" de Fase 6); nota "CRUD = repos, no formularios" añadida al entregable. Los dos tests resuelven el solver real dos veces en 1,8 s (lejísimos del techo de 10 s). Suite: solver 59 + app 37 (35 previos + 2) = 96 en el reactor completo, BUILD SUCCESS con mvn clean test desde la raíz (no -pl). src/main NO tocado (ni solver ni app): un único fichero de test nuevo → referencia-codigo-solver.md NO regenerado; modelo NO tocado (§4.7 ya estaba correcto de S53). Commit de una línea 549bc92 (solo el test); commit de doc aparte, pendiente de push. Siguiente: Fase 7 (UI de visualización: vistas por grupo, profesor y aula).

### Sesión 55 — Fase 7, Bloque 7A: BACKEND DE LECTURA (contrato de las tres vistas).

Modo híbrido (decisión y contrato en el Project, código en Claude Code). Abre Fase 7 tras cerrar el ALCANCE con el usuario: A1 (Angular servido por el jar vía
  frontend-maven-plugin, mecánica en 7B) + B1 (una proyección plana, la UI pivota) +
  vista-grupo primero + celda-como-lista. Fase 7 = SOLO visualización (drag&drop, D19/D20
  siguen en Fase 8). Seis decisiones cerradas antes de teclear: D-F7-1 (proyección de celda =
  plazas cuyo subgrupo toca el grupo/profesor/aula; (a)=(b) verificado sobre problema-3-
  cierre-cyr-refmt.json: cada subgrupo es MONO-GRUPO, la plaza agrupa los mono-grupo de los 4
  grupos, luego filtrar por "toca 1ºA" devuelve las 6 plazas del bloque = lo que pinta el
  PDF); D-F7-2 (co-docencia = UNA sub-entrada con N profesores, no N); D-F7-3 (validación
  estructural {asignatura, profesor, subgrupos} por slot, aula validada aparte contra vista-
  aula); D-F7-4 (el DTO plano se construye en método @Transactional(readOnly) del servicio, NO
  en el controlador: cargarHorario solo inicializa la lista de Sesion, no el grafo LAZY de
  cada Plaza); D-F7-5 (forma de SesionVistaDTO con grupos[] derivado de subgrupos, clave de
  las tres vistas); D-F7-6 (añadir spring-boot-starter-web a app/, que era persistencia pura).
  Entregado (6 commits de código, uno por artefacto): spring-boot-starter-web en app/pom.xml;
  extracción del núcleo único de renumeración de tramos (renumerarLectivos) +
  CatalogoMapper.indiceOrdenEnDia(List<TramoSemanal>) → Map<Long,Integer> keyed-by-getId();
  records SesionVistaDTO/HorarioProyeccionDTO (app.web.dto); GeneradorHorarioService.
  proyectar(Long) @Transactional(readOnly) que navega el grafo LAZY dentro de la transacción y
  arma el DTO plano; HorarioController GET /api/horarios/{id}/proyeccion (404 vía
  ResponseStatusException); ProyeccionHorarioTest (@DataJpaTest + replace=NONE, solver real
  corto new SolverHorario(10,42), patrón D-B10-7) que firma criterios 1/2 a nivel de contrato:
  el bloque de 6 plazas rep=2 proyecta 6 SesionVistaDTO en el mismo (dia,tramo) y filtrar por
  grupos∋"1ºA" las devuelve todas; cada Mat-1ºX (rep=3) da 3 con grupos==["1ºX"]. Hallazgos al
  leer el repo (desviaciones del prompt, todas señaladas): Asignatura expone getNombreCompleto()
  (no getNombre()); TramoSemanal NO tiene getOrdenEnDia() —ordenEnDia 1..6 no es derivable de un
  tramo aislado, exige renumerar excluyendo recreos (deuda D30)—; aTramosConIndice se llama en
  tests con tramos SIN persistir (getId()==null), por lo que NO puede consumir el helper keyed-
  by-id: ambos comparten el núcleo renumerarLectivos (fuente única) pero indexan distinto (id
  para proyectar, identidad de objeto para el mapper); import de @DataJpaTest en Spring Boot
  4.1.0 es org.springframework.boot.data.jpa.test.autoconfigure (ruta nueva de Boot 4.x,
  verificada contra la API oficial). D30 baja de 3 copias potenciales a 2; SolucionMapper.
  indiceTramos (copia 2, ruta de persistencia) intacta con nota de una línea, su unificación
  sigue siendo D30 (Fase 8). Deuda de test para 7B (anotada, no bloqueante): el assert (b) usa
  containsAll (verifica que la vista de 1ºA contiene las 6 plazas, no que EXCLUYA sesiones
  ajenas); en 7B añadir containsExactly sobre la vista de un grupo. Suite: solver 59 + app 38
  (37 previos + ProyeccionHorarioTest) = 97, BUILD SUCCESS con mvn clean test desde la raíz.
  src/main del solver NO tocado (solo lectura de Tramo.java) → referencia-codigo-solver.md NO
  regenerado; modelo NO tocado. Siguiente: Bloque 7B (frontend Angular: proyecto, integración
  frontend-maven-plugin, las tres vistas con celda-como-lista, validación visual contra los
  volcados de 1ºESO).

### Sesión 56 — Fase 7, Bloque 7B: FRONTEND ANGULAR (las tres vistas) y CIERRE de Fase 7.

Modo híbrido (decisión y contrato en el Project, código en Claude Code). Seis decisiones cerradas antes de teclear: D-F7B-1 (alimentación por fixture conocido {id}, sin endpoint de listado: Fase 7 = solo lectura); D-F7B-2 (Angular 21 + Node v22.23.1, verificado contra angular.dev —matriz A21 ^22.22.0||^24.13.1—; A22 descartado por novedad innecesaria, Node ≥24.15); D-F7B-3 (frontend en app/frontend/, proyecto Node NO módulo Maven; frontend-maven-plugin 2.0.1, versión en pluginManagement del parent, ejecución en app/pom.xml); D-F7B-4 (validación = mecanismo por test + visual manual, NO celda-a-celda contra PDF con el fixture reducido); D-F7B-5 (celda-como-lista: co-docencia = 1 entrada con N profes, sub-entrada con grupos[] múltiple en agrupamiento; confirmado en proyectar() y en pantalla); D-F7B-6 (aulaCodigo NUNCA null —Sesion.aula optional=false—, javadoc de SesionVistaDTO corregido; el "aula null" del PDF es artefacto de extracción de co-docencia). Entregado (Claude Code, commits de una línea por artefacto): scaffold Angular 21; modelos TS + servicio de proyección; rejilla 5×6 + las tres vistas con celda-como-lista; integración frontend-maven-plugin; tests de mecanismo; corrección de javadoc. Cuatro desviaciones al leer el repo, todas señaladas y resueltas: (1) repackage de Spring Boot NO estaba enganchado (app/ hereda de educhronos-parent, no de spring-boot-starter-parent —solo importa la BOM—; hueco preexistente de Fase 6), saldado en 7B: app pasa de librería a aplicación arrancable (fat jar); (2) build del frontend movido de generate-resources a prepare-package (decisión C): mvn test del backend ya NO reconstruye Angular (~29s→~11.7s), el frontend entra en el jar en package, verificado con jar tf ... grep static/index.html; (3) fix preexistente de 7A: el endpoint daba HTTP 500 al ejercerse por HTTP por primera vez (falta -parameters, misma raíz que (1): no hereda del starter-parent), resuelto con <parameters>true</parameters> en el maven-compiler-plugin del parent + @PathVariable("id") explícito + test de integración HTTP (standaloneSetup, sin dependencias nuevas) → cierra la deuda de test web de 7A; (4) el fixture de test del frontend (proyeccion-1eso.fixture.ts) había divergido del contrato: inventaba un profesor TEC4 para fingir co-docencia (el fixture reducido no tiene ninguna plaza multi-profesor), corregido eliminando TEC4 y añadiendo co-docencia REAL LEN2+LEN8 (de los volcados de 1ºESO) como caso legítimo de D-F7-2. Validación visual hecha por el usuario contra el seed real (horario id=1, 24 sesiones, OPTIMAL): el bloque CyR/OyD/RefMt se pinta como celda-con-6-sub-entradas y las Mat como celda simple; la posición del bloque la fija el solver (viernes t4 con seed=42, NO el miércoles t3 del PDF: correcto por D-F7B-4). Andamiaje vivo asignado a Fase 8: SeedHorarioRunner (app.catalog, @Profile("seed"), duplica el builder de CierreFase6HumoTest para poblar la BD; BORRAR EN FASE 8 cuando exista la vía real —CRUD o loader—, marcado en su javadoc). Deuda nueva anotada: el fixture del frontend es un artefacto escrito a mano sin mecanismo que lo ate al DTO real (el TEC4 divergió sin detección) → Fase 8 considerar test de contrato que deserialice un JSON capturado del endpoint real; Node de dev (nvm) y Node del pom (<nodeVersion>v22.23.1</nodeVersion>) deben mantenerse en la misma versión ≥22.22.0 (nvm 0.39.1 tiene el índice LTS caducado, no bloquea). Suite: 99 (solver 59 + app 40) + 6 frontend, BUILD SUCCESS. src/main del solver NO tocado; modelo NO tocado. Siguiente: Fase 8 — UI: configuración y ajuste manual (drag&drop D19/D20, parametrización del solver D29, CRUD del centro).

### Sesión 57 — Fase 8 [ARRANQUE] + Bloque 8.1 CERRADO (backend).

(backend). Modo híbrido: alcance, descomposición y decisiones en el Project;
  código en Claude Code. Se ABRE Fase 8 fijando alcance antes de construir. Fase 8
  descompuesta en bloques por dependencias: 8.1 vía REST generar+guardar (raíz de
  la que cuelga el resto) → 8.2 C5/SesionBloqueada estructural → 8.3 atribución por
  celda (D19 backend) → 8.4 pre-validación (D18/D20) → 8.5+ CRUD de catálogo (D10
  plazas multi-profesor, D1/D7 asistentes) → 8.6+ drag&drop + bloqueo interactivo.
  D21/D22/D30 diferibles a lo largo de la fase.
  Deuda VIVA que 8.2a deja para 8.2b: (i) pin de AULA (contrato por-plaza (plaza, aula) +
  restricción + verificación); (ii) persistencia de SesionBloqueada (entidad JPA §4.7 + schema) y
  entrada del bloqueo por REST (body de POST /api/horarios vs endpoint propio, a decidir); (iii)
  app/CatalogoMapper:135 lleva List.of() como placeholder de bloqueos —cablearlo cuando 8.2b los lea
  de la BD—.
  Bloque 8.2a — decisiones cerradas antes de teclear (D-F8.2-1..6): (1) identidad
  del bloqueo = ActividadInstancia(actividad, indice) de dominio, reutilizada sin materializar
  tabla nueva (coherente con D-B5-1 y con la identidad de Sesion en B9); en JSON se referencia
  por (actividad_codigo, indice). (2) el bloqueo vive como List<SesionBloqueada> bloqueos, 9º
  componente del record ProblemaHorario (forma 2, gemela de restriccionesHorarias); el cambio de
  firma se propagó a 4 constructores —io/ProblemaHorarioMapper (main), app/CatalogoMapper:135
  (main), y 2 tests VerificadorSolucionGrupoTest y SolverHorarioOptimizacionEscalaSubconjuntosTest—.
  (3) restricción DURA restriccionSesionBloqueada() en construir() (aplica en factibilidad y en
  optimización): addEquality(tramoIndex, indiceDeTramo(tramo)); el desdoble se pina simultáneo gratis
  porque las N plazas comparten tramoIndex. Aula contradictoria -> INFEASIBLE; validación amable
  diferida a 8.4. (4) verificador independiente contarBloqueosViolados (recomputo sin OR-Tools) que
  habilitó el ORO. (5) I/O de test: SesionBloqueadaDto(actividad, indice, tramo), array top-level
  "bloqueos" opcional en el schema. (6) REVERTIDA en diseño: el pin de aula NO va en 8.2a; el
  candidato Optional<Aula> en el record se retiró al descubrir en la lectura del repo que el pin de
  aula correcto es por PLAZA (plaza, aula), no por instancia —una instancia de desdoble tiene varias
  plazas—; SesionBloqueada queda (instancia, tramo) sin aula, y el pin de aula (contrato incluido) se
  difiere a 8.2b. Separación de capas respetada: cpsat NO importa io; la salvaguarda de instancia
  inexistente en ModeloCpSat usa IllegalArgumentException, la validación de entrada de usuario (índice
  fuera de rango, actividad desconocida) vive en el mapper con ProblemaInvalidoException.
  Bloque 8.1 — decisiones cerradas antes de teclear (D-F8.1-1..8): (1) endpoint
  síncrono POST /api/horarios, monousuario local; (2) persistir + devolver
  proyección (reutiliza HorarioProyeccionDTO de 7A, que ya porta id/estadoSolver/
  objetivo/cota); (3) D29 = params de solve en body opcional (maxSegundos default
  30, semilla default 42, via default OPTIMIZACION); (4) nombre opcional en body,
  default "Horario "+timestamp; (5) generar() parametrizado, se ELIMINA el sin-args
  (evita segunda puerta con defaults hardcodeados, el patrón que dejó divergir al
  runner); (6) reutilizar proyectar(id) sin tocar su firma; (7) SeedHorarioRunner
  partido; (8) deuda de 7B (atar fixture al contrato real) = D-F8.1-8, DIFERIDA.
  Decisión de alcance clave: vía FACTIBILIDAD sacada del bloque. Motivo confirmado
  en el código: resolver() devuelve SolucionHorario sin estado y ResultadoOptimizacion
  .objetivo/cota son double primitivos (no null); persistir factibilidad por el canal
  de guardar() exigiría o un centinela 0.0 falso o tocar el solver (fuera de alcance).
  El enum ViaSolver arranca solo con OPTIMIZACION; el switch sin default hará que
  añadir FACTIBILIDAD sea error de compilación guía, no olvido silencioso.
  Hallazgos al leer el repo: la generación estaba DUPLICADA (generar() en el servicio
  vs. secuencia inline en el runner con new SolverHorario(10,42)); no hay
  @ControllerAdvice global (patrón 7A = try/catch por método, se sigue). Regresión
  detectada y corregida: el default de 30 s NO estaba cableado —con body por defecto
  se caía al new SolverHorario() (120 s); arreglado en el servicio (seg=30, sem=42 si
  null), test de regresión con mockConstruction capturando args del constructor.
  Nota viva: 30 s es conjetura de UX, NO validada contra el centro real (~28 grupos;
  el instituto completo no converge a óptimo ni en 600 s); constante revisable.
  Episodio de toolchain (frontend): ng test dejó de arrancar por ERR_REQUIRE_ESM —
  Node del sistema v20.5.1 por debajo del mínimo de Angular 21 (@angular/build engines
  ^20.19.0||^22.12.0||>=24.0.0, verificado contra el paquete instalado). Resuelto
  subiendo a Node 22.23.1 vía nvm (el default apuntaba a lts/* sin resolver → caía a
  system; corregido con nvm alias default 22.23.1), .nvmrc en app/frontend, reinstalación
  limpia (package-lock regenerado bajo npm 10, solo subidas de parche). ng test verde
  (vitest+jsdom, 6 specs). NO afecta a 8.1 backend. 5 commits de código (uno por
  artefacto A–E) + fix del default 30 (7ebe21e) + .nvmrc (ece9455) + regen del lock.
  Suite backend: solver 59 + app 43, BUILD SUCCESS con mvn clean test y mvn clean
  package (static/index.html en el jar). src/main del solver NO tocado (referencia-
  codigo-solver.md sin regen); modelo NO tocado. Deuda VIVA que 8.1 deja: D-F8.1-8
  (atar el fixture del frontend proyeccion-1eso.fixture.ts al contrato real, sub-bloque
  de frontend); FACTIBILIDAD por REST (mini-bloque con decisión de estadoSolver
  pendiente); warm-start por REST (si se pide).

### Sesión 58 — Fase 8, Bloque 8.2a: PIN DE INSTANCIA A TRAMO (bloqueo manual) en el solver.

Modo híbrido (decisión y contrato en el Project, código en Claude Code). Cierra el
criterio 5 de Fase 3 (diferido desde S17: "bloquear un tramo y el solver lo respeta").
Trabajo de dominio + cpsat + io de test; NO toca persistencia/REST (8.2b). Alcance
cortado con el usuario: 8.2 partido en 8.2a (solver+modelo, esta sesión) y 8.2b
(persistencia+REST+pin de aula). Decisiones D-F8.2-1..6; la 6 REVERTIDA en diseño (pin
de aula fuera de 8.2a: es por-plaza (plaza, aula), no por-instancia).

El pin es el DUAL de restriccionIndisponibilidadProfesor (addEquality sobre el tramoIndex
en vez de dominio complementario); el desdoble se pina simultáneo gratis por el tramoIndex
compartido por las plazas.

La Fase 0 de lectura del repo (parada incondicional antes de teclear) descubrió tres cosas
que el índice de solver/ no veía: la instancia se localiza recorriendo
List<InstanciaProgramada> por equals de record; un 4º new ProblemaHorario( en
app/CatalogoMapper (resuelto con List.of() placeholder, opción A, cableado real en 8.2b);
y que ProblemaInvalidoException vive en io -> no se puede lanzar desde cpsat sin romper
capas (resuelto: IllegalArgumentException en ModeloCpSat como salvaguarda,
ProblemaInvalidoException en el mapper como validación de entrada).

Entregado (7 commits de una línea): record SesionBloqueada + 9º componente de
ProblemaHorario; restriccionSesionBloqueada() en construir(); contarBloqueosViolados en el
verificador; SesionBloqueadaDto + mapper + schema; 4 fixtures + tests de solver (respeto,
desdoble, infactible, gemelo sin-pin) + 2 de loader. ORO en positivo: comentar la
restricción hace caer 3 de 4 tests del pin; reactivada, ModeloCpSat idéntico a HEAD.

Suite: solver 59->65 (+6), app 43 (sin cambio), BUILD SUCCESS con mvn clean test desde la
raíz, árbol limpio. src/main del solver SÍ tocado -> referencia-codigo-solver.md REGENERADO
(commit 350258b); modelo NO tocado (§4.7 ya correcto de S53). Commits
6ef0c14/7dd9048/1987925/5a144d3/0150e64/350258b.

Deuda VIVA que 8.2a deja para 8.2b: pin de AULA (contrato por-plaza + restricción +
verificación), persistencia de SesionBloqueada (entidad JPA §4.7 + schema) y entrada del
bloqueo por REST, y el List.of() placeholder de app/CatalogoMapper.

### Sesión 59 — Fase 8, deuda D-F8.1-8 CERRADA: test de contrato de serialización de los DTOs de proyección (blinda contra la divergencia silenciosa que en 7B dejó colar el profesor TEC4).

Modo híbrido (decisión y contrato en el Project, código en Claude
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
---
### Sesión 60 — Fase 8, Bloque 8.2b-i: PIN DE AULA por-plaza en el solver
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

### Sesión 61 — Fase 8, Bloque 8.2b-ii: PERSISTENCIA JPA de los bloqueos

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

### Sesión 62 — Fase 8, Bloque 8.2b-iii-A: CABLEADO del servicio a los repos de bloqueo.
  Modo híbrido (diseño en el Project, código en Claude Code). CIERRA EL LAZO end-to-end
  del bloqueo manual: hasta S61 el pin funcionaba en el solver, se persistía y se mapeaba, pero la
  vía REST de generación NO lo leía (cargarProblema() pasaba List.of(), List.of()) — todo lo
  construido en S58/S60/S61 era INERTE en producción. Alcance CORTADO con el usuario al abrir: se
  descartó hacer 8.2b-iii entero (cableado + REST) y se partió en 8.2b-iii-A (cableado, esta sesión)
  + 8.2b-iv (REST, diferido). Razón del corte: el cableado es la pieza que hace útil lo ya
  construido y tiene oro trivial; la superficie REST no tiene consumidor todavía (la UI de drag&drop
  es 8.6) y diseñar su API sin el consumidor delante es diseñar a ciegas.
  Decisiones cerradas antes de teclear (D-F8.2b-iii-A-1..2): (1) inyección DIRECTA de los dos repos
  en el constructor de GeneradorHorarioService (pasa de 10 a 12), patrón vivo; se descartó envolverlos
  en un servicio intermedio (capa sin ganancia: los findAll() son triviales y sin filtro) → deuda
  nueva D-F8.2b-iii-A-a. (2) el ORO es un IT que llama a generar(...) por la VÍA REAL, no a
  cargarProblema() suelta, con maxSegundos=10 EXPLÍCITO (el default de 30 s del servicio es veneno
  para la suite, D24/D25).
  Hallazgo clave de la lectura del repo (el riesgo que se temía NO se materializó): cargarProblema()
  YA era @Transactional(readOnly=true) y su javadoc ya documentaba la razón (identidad de objeto en
  relaciones LAZY). Por tanto añadir los findAll() de bloqueo DENTRO de ese método hace que el
  TramoSemanal de SesionBloqueada.getTramoInicio() salga de la MISMA caché de primer nivel que
  tramoRepository.findAll(), y el IdentityHashMap de S61 funciona. No había decisión que tomar: el
  sitio correcto era el único sitio. Segundo hallazgo: NINGÚN llamador del constructor se rompió —los
  6 tests usan @Import + @DataJpaTest, que auto-inyecta los repos nuevos.
  Entregado (3 commits de una línea, código y tests separados): (TAREA 1) cableado de
  GeneradorHorarioService —2 repos inyectados, los 2 List.of() → findAll(), javadoc de cargarProblema()
  ampliado con la precondición de identidad de instancia y el modo de fallo SILENCIOSO (cargar los
  bloqueos fuera de la transacción daría instancias distintas y el pin se perdería sin excepción);
  (TAREA 2) PinTramoGeneracionRoundTripTest, IT end-to-end: persistir catálogo + pin de tramo →
  service.generar(10, 42, null, "test-pin") → recargar → toda Sesion de la instancia pinada cae en el
  tramo pinado; (TAREA 3) BloqueoMapperPinAulaHuerfanoTest, unitario (sin Spring/BD, en app.catalog por
  los ctor protected), cierra la deuda de test (c) de S61.
  VALIDEZ DEL FIXTURE (procedimiento obligatorio, lección del assert (b) de S61): sonda temporal
  confirmó que SIN pin el solver con semilla 42 coloca la instancia en L1; se pina L5, tramo que el
  solver NO elegiría por su cuenta. ORO NEGATIVO: revertir los dos findAll() a List.of() deja el test
  ROJO (cae en L1, id=1 ≠ 5) — el pin CAMBIA la solución, no es un no-op; restaurado, verde.
  Parada de lectura confirmada antes de teclear: la validación del pin de aula huérfano SÍ estaba
  implementada en BloqueoMapper (lanza IllegalArgumentException "pin de aula sin pin de tramo para …"),
  no solo documentada → TAREA 3 procedía sin cambio de alcance.
  Suite: solver 71 intacto (no se tocó solver/src/main → referencia-codigo-solver.md NO regenerado),
  app 47 → 49 (+2). BUILD SUCCESS con mvn clean test desde la raíz; working tree limpio. Modelo NO
  tocado; REST NO tocado.
  Deuda VIVA que 8.2b-iii-A deja: D-F8.2b-iii-A-a (12 repos en el constructor de
  GeneradorHorarioService: extraer un CatalogoLoader cuando moleste; NO bloqueante). Deuda (a) y (c)
  de 8.2b-ii CERRADAS.
  Siguiente: LIMPIEZA DE FONDO del plan (candidato principal de S63, acordada y pospuesta dos veces),
  o 8.2b-iv (REST de bloqueos, con contrato ya pre-cerrado), o el candidato que se decida al abrir
  sesión.

### Sesión 63 — HIGIENE DOCUMENTAL del plan y la bitácora (sin código, 3 commits).
  Modo interactivo (revisión por secciones con el usuario). DOS operaciones. Op1: archivó la cabecera
  compacta de S59 a la bitácora (la ventana del plan conserva SIEMPRE las 4 últimas) y borró de la
  bitácora el dato duplicado de qué ventana conserva el plan, que había quedado rezagado. Op2 (LIMPIEZA
  DE FONDO): condensó los bloques de fases CERRADAS —Fase 5 (21 bloques → una línea cada uno) y Fase 6
  (bloques 1-7, 9 y CIERRE)— con el formato "qué (Sxx) → deuda/decisión superviviente; Detalle: bitácora
  Sxx", y partió la deuda consciente en dos secciones físicas: VIVA (íntegra) y CERRADA (histórico,
  condensada a una línea que CONSERVA su mecanismo vivo de src/main). Deuda nueva D31 (poblaciones y
  particiones a confirmar con el centro) que absorbe las cuatro deudas dispersas "a confirmar con el
  centro" de B3/B7/B11/B13 + la invariante de población.
  Guardarraíles cristalizados como DECISIÓN PERMANENTE (ver "Criterio de higiene documental del plan"):
  R4 (ningún token —Dxx/D-Bx-y/Cx/§x.y— sin citante vivo ni definición viva, verificado por grep) y R5
  (mecanismo vivo de src/main ≠ historia de sesión). Una PARADA de R4: Fase 6 Bloque 8 queda ÍNTEGRO
  porque D-B8-1 se cita desde el criterio vivo de Fase 6 (l.440) y solo se define ahí. El bloque CIERRE
  de Fase 6 lleva la remisión "Detalle y decisiones D-B10-1..9: bitácora S54" para dar destino a la cita
  viva de D-B10-7 (cabecera de S61). Veredicto D24: CONDENSADA (D25, VIVA, se entiende sola: re-expone
  inline el @Tag de D24 y su insuficiencia). D13/D15/D23/D24/D28 condensadas conservando su mecanismo.
  Resultado: plan 2154 → 1429 líneas (−725, −34 %); bitácora 2242 → 2270 (+28 por S59). Estructura viva
  intacta (Bloques de Fase 8 EN CURSO, Decisiones permanentes, Hallazgos PDFs, criterios de fase). No se
  tocó código, ni modelo_datos_fase1.md, ni nada fuera de docs/. Tres commits de una línea. Árbol limpio.
  Siguiente: 8.2b-iv (REST de bloqueos, contrato pre-cerrado en S62) o el candidato que se decida al abrir sesión.

### Sesión 64 — Fase 8, Bloque 8.3-A: ATRIBUCIÓN ESTRUCTURADA de reglas DURAS por celda.
  Modo híbrido (diseño en el Project, código en Claude Code). 2 commits
  (6e9f68d código + f4df782 referencia). Alcance CORTADO al abrir: 8.3 partido en 8.3-A (duras,
  esta sesión) y 8.3-B (blandas, DIFERIDO). Razón del corte: los "recomputos gemelos" de las
  blandas (contarVentanasProfesor, contarPenalizacionIndisponibilidadBlanda,
  contarPenalizacionConsecutivasProfesor) valen precisamente por ser INDEPENDIENTES del modelo
  CP-SAT —delatan un error del modelo porque cuentan de otra manera—; convertirlos en
  atribuidores por celda amenaza esa independencia. Es una decisión de DISEÑO sin resolver, no
  un pendiente mecánico, y no debía contaminar el trabajo de las duras.
  HALLAZGO que motivó el bloque: ResultadoVerificacion era List<String> y reportarColisiones
  agregaba en Map<T,Integer> —un CONTADOR que TIRABA las instancias culpables—. Sabía QUE había
  colisión y DE QUIÉN, pero no QUIÉNES la causaban: la atribución por celda era imposible sin
  instrumentar. Confirmado por grep que en src/main solo lo consumían VerificacionPrinter y Main
  (CLI, andamiaje): la List<String> no era API de producción que mereciera protección.
  Decisiones cerradas antes de teclear (D-F8.3-A-1..5): (1) ResultadoVerificacion pasa a
  List<Violacion> —se descartó añadir un campo extra conservando las cadenas: dos fuentes de
  verdad que se desincronizan— y se descartó de plano un atribuidor nuevo en app/ que
  reimplementara las comprobaciones (dos definiciones de "qué es un solape" divergen). (2) tipos
  nuevos ReglaDura (enum, 7 valores) + CeldaRef(actividadCodigo, indice, plazaCodigo) +
  Violacion(regla, recursoCodigo, tramoCodigo, celdas, descripcion). (3) UNA violación = N celdas
  (un solape de profesor entre 2 instancias es UNA Violacion con celdas.size()==2, no dos):
  preserva la cardinalidad que los tests ya asumían y es lo que la UI querrá resaltar. (4) el
  puente a sesionId vive en app/, NO en solver/: CeldaRef usa códigos de negocio y
  SesionVistaDTO ya lleva indice+actividadCodigo+plazaCodigo —la clave compuesta ya existe, no
  hubo que tocar el DTO—. (5) alcance ESTRICTO a solver/: sin REST (el endpoint necesita las
  blandas, que son 8.3-B; sacar una superficie solo-duras obligaría a reescribirla en la
  siguiente sesión, el error que S62 evitó).
  CORRECCIÓN AL CONTRATO durante la parada de lectura: D-2 especificaba CeldaRef.indice >= 0;
  el dominio es 1-based (ActividadInstancia rechaza indice < 1), así que la guarda correcta es
  >= 1. Con >= 0, CeldaRef podía representar un estado imposible en el dominio y la validación
  no validaba nada. CeldaRef.indice transporta el índice 1-based TAL CUAL, sin reindexar:
  cualquier traducción sería un bug silencioso en el puente a sesionId.
  ASIMETRÍA D15 PRESERVADA y ahora protegida por test: profesor/subgrupo/grupo se cuentan POR
  INSTANCIA (celda con plazaCodigo=null); el AULA se cuenta POR PLAZA (celda con plazaCodigo
  no-null). El acumulador del aula vive DENTRO del bucle de plazas y los de profesor/subgrupo
  FUERA. Es frágil a la lectura —un refactor "de limpieza" que uniformara la granularidad
  rompería D15 sin que la suite se pusiera roja— y por eso el oro afirma las dos direcciones.
  ORO: VerificadorSolucionAtribucionTest (2 casos). SOLAPE_PROFESOR: exactamente 1 violación,
  regla + recurso + tramo, y celdas() containsExactlyInAnyOrder las 2 CeldaRef culpables — esto
  último es lo que la implementación anterior era INCAPAZ de producir. SOLAPE_AULA: plazaCodigo
  no-null y celdas por plaza. El aislamiento (grupos distintos, aulas distintas / profesores
  distintos) hace que hasSize(1) sea informativo y no accidental.
  verificarDistribucion emite la violación con AMBAS instancias del día (Map primeraDelDia +
  putIfAbsent): salió barato, así que se hizo completo en vez de dejarlo como deuda.
  Roturas colaterales previstas y reparadas: VerificadorSolucionGrupoTest (3 aserciones
  .contains sobre String → sobre el record, quedan MEJOR) y SolverHorarioTest:94 (que la parada
  de lectura de Claude Code cazó y yo había pasado por alto; reforzada además con
  celdas().size()==2). Salida de consola de VerificacionPrinter byte-idéntica.
  Suite: 73 tests, 0 fallos, BUILD SUCCESS. +334/−43. app/ intacto, modelo NO tocado (CeldaRef
  y Violacion son infraestructura de verificación, no entidad ni invariante nueva). src/main del
  solver SÍ tocado → referencia-codigo-solver.md REGENERADO (f4df782). Sin deuda funcional nueva.
  Siguiente: 8.3-B (atribución de las BLANDAS, con la decisión de diseño sobre los gemelos por
  resolver), o el candidato que se decida al abrir sesión.

### Sesión 65 — Fase 8, Bloque 8.3-B: ATRIBUCIÓN CONTRAFACTUAL de reglas BLANDAS por celda.

CIERRA D19 en BACKEND (duras en 8.3-A, blandas aquí). Modo híbrido (diseño en el
Project, código en Claude Code). 3 commits (c74f2b8 código + 1f14e42 referencia + fix de comentario).
HALLAZGO que resuelve la decisión de diseño que S64 dejó ABIERTA ("¿el atribuidor blando ES el gemelo,
o es un tercer camino?"): la pregunta estaba MAL PLANTEADA porque presupone que hay UN atribuidor
blando. Los tres gemelos NO son del mismo tipo. contarPenalizacionIndisponibilidadBlanda es LOCAL: su
bucle YA sabe qué instancia penaliza (atribuir = no tirar el dato que tiene en la mano; mismo hallazgo
que S64 hizo sobre reportarColisiones). contarVentanasProfesor y
contarPenalizacionConsecutivasProfesor son NO-LOCALES: la penalización es propiedad de un CONJUNTO de
posiciones (Set<Integer>), no de ninguna celda — ninguna celda "causa" una ventana, y el dato de qué
instancia puso cada posición no se perdió por descuido sino porque NO HACE FALTA para contar.
Decisiones cerradas antes de teclear (D-F8.3-B-1..4): (1) [D-F8.3-B-1, la que S64 dejó abierta] la
atribución blanda NO es culpabilidad sino CONTRAFACTUAL: delta = penalización_actual −
penalización_si_esa_celda_no_estuviera. Eso responde a lo que la UI necesita ("¿qué gano si muevo
esto?") y CONSERVA la independencia respecto a CP-SAT por construcción: el atribuidor USA al gemelo
como oráculo, sin ser él ni reimplementarlo. Se descartó de plano cualquier reparto de culpa
(proporcional, por primera-instancia): la ventana es propiedad de un conjunto y todo reparto sería una
invención sin correlato en el dominio. (2) Coste vs no-duplicación: se EXTRAEN las fórmulas existentes
a funciones puras estáticas ventanasDe(Set<Integer>) y excesoConsecutivasDe(Set<Integer>, int) que
llaman TANTO el gemelo público COMO el atribuidor — una sola definición de "qué es una ventana", coste
O(1) por celda. Se descartó el oráculo puro (reinvocar el gemelo entero por celda, O(celdas ×
Expansion.todas)): cuando el coste mordiera, alguien duplicaría la fórmula sin decisión consciente. NO
es una segunda definición: es la ÚNICA definición movida a donde ambos la alcanzan. (3) Vehículo
SEPARADO de verificar(): tipos nuevos ReglaBlanda + Penalizacion + AtribucionBlanda; NO se toca
ResultadoVerificacion/Violacion/ReglaDura. Una penalización blanda NO es una Violacion (no invalida la
solución, no tiene recurso violado, y sus N celdas tienen deltas DISTINTOS entre sí). Meterla en
List<Violacion> habría sido el refactor de uniformización que amenaza la asimetría D15 — que por tanto
NO entra en el radio de este bloque, por construcción y no por vigilancia. (4) Alcance solver/
únicamente, sin REST ni app/ (gemelo de 8.3-A: el consumidor real es 8.6).
CORRECCIÓN AL CONTRATO DURANTE EL DISEÑO (el detalle que salva el bloque): delta LLEVA SIGNO y NO se
normaliza. delta>0 = mover la celda MEJORA; delta<0 = la celda está TAPANDO un hueco y moverla EMPEORA
({1,2,3} = 0 ventanas; quitar la del medio deja {1,3} = 1 ventana, delta = −1); delta=0 = indiferente,
y NO se emite (el mapa no se ensucia con ceros). El contrato inicial decía "delta > 0 siempre" y era
FALSO: de haberlo dejado al teclado habría salido un Math.max(0,delta) silencioso que MIENTE y que
sobrevive a una suite verde. tramoCodigo es null en VENTANA_PROFESOR y EXCESO_CONSECUTIVAS (penalizan
una configuración de DÍA, no de tramo; rellenarlo sería mentir) y no-null solo en
INDISPONIBILIDAD_BLANDA. Asimetría deliberada, gemela de la de Violacion.
ORO del bloque: test que asevera delta EXACTAMENTE −1 (isEqualTo(-1), no "<=0" ni "!=0") en la celda
del medio de {1,2,3} — es el único aserto que un clamp a positivo NO sobrevive. Más discriminación con
delta +1 en la punta de {1,3}. Consistencia de los gemelos tras la extracción aseverada contra VALORES
LITERALES conocidos (ventanas=3, consecutivas=1) sobre solución construida a mano y enumerada en
diseño, NO tautológica; se usó fixture propio y no uno existente porque ninguno ejercita ambos gemelos
con valores no triviales sobre una solución DETERMINISTA por día (el papel de no-regresión contra el
comportamiento histórico lo hacen los oro-fuerte de S24/S25/S27, que sí corren sobre los fixtures
reales y quedaron verdes).
REVISIÓN POR JUICIO (reparto de S64): el diff de los dos gemelos se verificó como EXTRACCIÓN PURA
(líneas movidas, no reescritas; única diferencia continue→return 0, correcta al pasar de bucle a
función). Se cazó y corrigió un COMENTARIO ENGAÑOSO en el test del delta −1, que afirmaba que "los
extremos tienen delta 0 porque quitarlos no crea hueco": el aserto pasa, pero la razón es falsa y
CONTRADICE al test hermano — los extremos dan 0 en {1,2,3} porque quitarlos reduce span y nClases en 1
y la resta se cancela, NO porque los extremos nunca aporten (en {1,3,4}, quitar el extremo 1 da delta
+1). Un comentario que miente sobre la semántica es deuda viva en src/test: invita a "optimizar" el
atribuidor saltándose los extremos.
Suite: 122 → 127 (solver 73 → 78; app 49 intacto), 0 fallos, BUILD SUCCESS. ModeloCpSat NO tocado
(probado por git diff --stat) — el atribuidor es independiente de CP-SAT, que es todo el punto.
app/, REST, DTOs, frontend y modelo_datos_fase1.md NO tocados (los tipos de atribución son
infraestructura de verificación, no entidad ni invariante nueva). verificarNoSolapes NO tocado: D15
intacta. src/main del solver SÍ tocado → referencia-codigo-solver.md REGENERADO (1f14e42).
Deuda que deja viva: el n=3 literal de atribuirBlandas es el MISMO espejo frágil de MAX_CONSECUTIVAS
que ya tenía el gemelo; la extracción lo PARAMETRIZA (excesoConsecutivasDe recibe n) pero no resuelve
el origen. Es D21(c), ya registrada, NO ampliada aquí.
Siguiente: 8.4 (pre-validación, D18/D20), 8.2b-iv (REST de bloqueos, contrato pre-cerrado en S62) o el
candidato que se decida al abrir sesión. D19 backend CERRADA; D19 queda viva solo en su parte de UI
(8.6, consumidor real de la atribución).

### Sesión 66 — Fase 8, Bloque 8.2b-iv: ENTRADA DEL BLOQUEO POR REST.

Modo híbrido. 2 commits de código (071852c + 94e2649, el segundo por rehacer el test de
contrato). ALCANCE RECORTADO EN SESIÓN: se confirmó "8.2b-iv + REST de atribución" y se
RETIRÓ la mitad de atribución al leer el repo. Motivo (hecho, no juicio): SolucionHorario NO
es reconstruible desde las filas de Sesion —Sesion guarda (plaza, indice, tramoInicio, aula)
y tramoInicio es un TramoSemanal de orden GLOBAL con recreos, no un domain.Tramo (dia +
ordenEnDia)—; reconstruirla exigiría un INVERSO de SolucionMapper.indiceTramos, es decir un
TERCER espejo de la renumeración de jornada (D30 ya se queja de dos), más un constructor de
SolucionHorario que D-B9-5 decidió NO tener. Y peor: el consumidor real (8.6 drag&drop)
necesita atribución sobre una solución CANDIDATA que llega en el request (el usuario acaba de
mover una celda y NO ha guardado), no sobre un horario recargado de BD —luego GET
/{id}/diagnostico es la forma EQUIVOCADA—. Diseñarla hoy sería diseñar a ciegas antes del
consumidor: el mismo error que S62 evitó con este mismo bloque. La atribución REST se abre
como bloque 8.3-C, con su diseño explícitamente PENDIENTE.
Decisiones cerradas antes de teclear (D-F8.2b-iv-1..7): (1) un recurso = pin de tramo + sus
pines de aula, sin endpoint de aula suelto (el dominio no lo admite: la API no debe poder
escribir lo que el consumidor rechaza); DELETE en cascada por la misma razón. (2) tramo por
(dia, ordenEnDia), NO por TramoSemanal.id, porque la UI no ve ese id —SesionVistaDTO lleva
(dia, tramo)—; implementado INVIRTIENDO CatalogoMapper.indiceOrdenEnDia, no reimplementando
la renumeración: D30 gana un consumidor, no un tercer espejo. (3) el alta valida contra el
catálogo JPA y NO llama a BloqueoMapper (exigiría mapear el catálogo entero para un pin) →
deuda D-F8.2b-iv-a. (4) POST idempotente por instancia (reemplaza, 200; no 409: la
restricción única (actividad, indice) haría reventar un insert ciego, y el gesto de la UI es
"clavar aquí", no "conflicto"). (5) reemplazo TOTAL, PUT semántico: sin merge parcial, no se
distingue null de [] (esa sutileza produce bugs silenciosos). (6) GET plano simétrico; la UI
cruza (actividadCodigo, indice) contra SesionVistaDTO, que YA lleva la clave compuesta →
SesionVistaDTO NO se toca. (7) BloqueoController + BloqueoService nuevos, no ampliar
GeneradorHorarioService (arrastra D-F8.2b-iii-A-a: 12 repos).
HALLAZGO de la parada de lectura, que despeja una duda del contrato: catalog.Plaza SÍ expone
el XOR (getAulaFija() != null vs getAulasCandidatas()) —la validación (e) era implementable
sobre la ENTIDAD, no solo sobre domain.Plaza—.
REVISIÓN POR JUICIO (reparto de S64/S65): el PRIMER test de contrato fue RECHAZADO por el
arquitecto y rehecho. Probaba el CAMINO FELIZ (alta válida → cargarProblema() la mapea), y
eso NO detecta lo que el test existe para detectar: una divergencia entre los dos validadores
NO se manifiesta en el camino feliz —se manifiesta cuando el alta ACEPTA lo que el mapper
RECHAZA—. Con aquel fixture, borrar la comprobación de candidatura del alta dejaba el test
VERDE y el fallo salía como 500 en el solve. Rehecho por VÍA B: inyecta en BD
—SALTÁNDOSE BloqueoService— un pin de aula NO candidata y asevera que cargarProblema()
LANZA. Verificado además que el chequeo de candidatura vive SOLO en BloqueoMapper (el ctor de
domain.SesionBloqueada solo hace null-checks), luego el hasMessageContaining("candidata") no
captura otro throw por accidente. El camino feliz sobrevive RENOMBRADO a
humo_altaValidaLlegaAlProblemaHorario: un test llamado "contrato" que probaba humo era un
nombre que MIENTE, la misma clase de deuda que S65 cazó en un comentario.
Lección de método: el prompt especificó el PROPÓSITO del test ("el que ata los dos
validadores") pero no el ASERTO DISCRIMINANTE, y Claude Code escribió el camino feliz. Es el
mismo aprendizaje que el isEqualTo(-1) de S65: hay que escribir el aserto, no el propósito.
Suite: app 49 → 56 (+7, todos BloqueoEndpointTest); solver 78 intacto. solver/src/main NO
tocado → referencia-codigo-solver.md NO regenerado. modelo_datos_fase1.md NO tocado (el
bloqueo REST no añade entidad ni invariante: §4.7 ya lo describe). SesionVistaDTO, frontend y
HorarioController NO tocados.
Siguiente: 8.4 (pre-validación, D18/D20), 8.3-C (REST de atribución, DISEÑO PENDIENTE: cómo
llega la SolucionHorario candidata) o el candidato que se decida al abrir sesión.

### Sesión 67 — Fase 8, Bloques 8.6-A (contrato de ajuste manual) + 8.3-C (REST de atribución).

Modo híbrido. 3 commits (1772af6 código + ff1efbf tests + 619ef34
referencia regenerada). ALCANCE: se abrió 8.6-A —no 8.4— porque 8.3-C llevaba una decisión de
DISEÑO bloqueada, y bloqueada por su CONSUMIDOR: el plan (S66) ya avisaba de que 8.3-C y 8.6
eran el mismo frente. Confirmado.
HALLAZGO 1 (el que desbloquea el frente): POST /api/bloqueos —construido en S66 creyendo que
era "el endpoint del bloqueo"— YA ES el endpoint del drag&drop. "Mover una celda" = "pinarla
en el tramo destino": mismo payload, misma semántica idempotente, mismo par (dia, ordenEnDia)
que ya lleva SesionVistaDTO. No había que inventar superficie.
D-F8.6-A-1 (vía C, pinar en caliente + re-solve diferido): se descartaron las dos vías puras.
A (solve por gesto) es INVIABLE por latencia —S44: 601 s a escala real, nadie arrastra y espera
diez minutos—. B (editor libre con verificación en cliente) es INACEPTABLE por el CUARTO ESPEJO
—portar verificarNoSolapes a TypeScript, sin el test que protege D15, en otro lenguaje—.
C evita ambas: el gesto pina (ms), el solve es explícito, y el aviso de conflicto en cliente es
un CRUCE DE ÍNDICES sobre datos ya cargados, no una verificación: si se equivoca, no pasa nada,
porque el solver es quien decide y un pin contradictorio dará INFEASIBLE (que es lo que 8.4
existe para hacer amable). Precio asumido: el usuario no controla el resultado final.
D-F8.6-A-2: la sub-entrada de un desdoble NO es arrastrable (S5 obliga a compartir tramo);
arrastrable = la celda (instancia). El cambio de aula es otro gesto, por plaza.
MOCKUP (Claude Design, primera vez en el proyecto): dibujó la celda de desdoble con las tres
granularidades encima. Contestó que se distinguen sin colisión —badge de cabecera = coste
blando (instancia); fondo de sub-entrada = conflicto de aula (plaza); borde de celda = solape
de profesor/subgrupo (instancia)— y que la sub-entrada arrastrable sería una promesa falsa. La
asimetría D15 SE PINTA sin aplanarse. Deuda de método nueva: D-F8.6-a.
HALLAZGO 2 (corrige a S66, y es un hecho, no un juicio): el plan afirmaba que reconstruir
SolucionHorario exigiría "un TERCER espejo de la renumeración". FALSO. indiceTramos YA devuelve
Map<Tramo,TramoSemanal>; el inverso es un for sobre entrySet(). S66 lo dedujo SIN HABER LEÍDO
indiceTramos. Corregido en el bloque 8.3-C.
Decisiones de 8.3-C (D-F8.3-C-1..6): (1) la solución se RECONSTRUYE desde BD, no se transporta
(habilitado por C: no hay candidata). (2) el inverso vive en SolucionMapper —donde YA está la
correspondencia—, invirtiendo el mapa, no recalculando: D30 gana un consumidor. (3) aulasElegidas
OMITE las plazas con aulaFija (FIDELIDAD, no equivalencia) + guarda de corrupción si el aula
persistida contradice la fija. (4) el DTO lleva violaciones + penalizaciones + totales; las duras
vienen VACÍAS en un horario del solver —son RED DE SEGURIDAD VISIBLE ("0 conflictos, verificado
independientemente de CP-SAT"), no diagnóstico—. PenalizacionDTO NO lleva plazaCodigo: la
atribución blanda es por INSTANCIA y un campo siempre-null es un campo que MIENTE. (5)
SesionVistaDTO NO se toca (la UI cruza por (actividadCodigo, indice)). (6) SolucionHorario gana
un getter aulasElegidas().
REVISIÓN POR JUICIO (reparto de S64/S65/S66): el primer Test 1 fue RECHAZADO. Inspeccionaba
aulasElegidas POR REFLEXIÓN del campo privado, con el argumento de que era "la única vía sin
tocar solver/". El argumento era correcto y la conclusión no: si la distinción fija/elegida solo
es observable por reflexión, entonces NO es observable por la API pública, y D-F8.3-C-3
protegería una propiedad que NINGÚN consumidor legítimo puede comprobar. Un invariante que exige
violar el encapsulamiento para verificarse no es un invariante: es un comentario. Y el javadoc de
SolucionHorario YA AFIRMA la propiedad ("las aulas de plazas con aulaFija NO se almacenan aquí").
Si la clase la afirma, la clase debe permitir comprobarla → D-F8.3-C-6: getter público,
ESTRICTAMENTE ADITIVO (+15 líneas, 0 modificadas; constructor, aulaElegida, tramoDeInstancia,
asignaciones, ModeloCpSat y VerificadorSolucion intactos). Fue la ÚNICA excepción al alcance
"solver/src/main no se toca", tomada a sabiendas y con su coste (regeneración de la referencia).
ORO: round-trip que compara asignaciones() por igualdad de MAPAS (no isNotNull ni hasSize) contra
la solución que devolvió el solver, más doesNotContain(plazaFija) y comparación directa de
aulasElegidas() reconstruido vs original. ORO NEGATIVO: con las fijas metidas dentro, Test 1 ROJO
en el aserto (2); revertido, verde. Sin él, D-F8.3-C-3 sería decorativa.
DiagnosticoService DELEGA en GeneradorHorarioService.cargarProblema() por método público —no
hereda sus 12 repos (D-F8.2b-iii-A-a)—, y es OBLIGATORIO, no solo permitido: cargar el catálogo
por su cuenta reproduciría la trampa de S62 (los bloqueos deben leerse DENTRO de la misma
transacción readOnly o el pin se pierde EN SILENCIO, por identidad de objeto de TramoSemanal
contra el IdentityHashMap de BloqueoMapper). El porqué quedó ESCRITO en su javadoc de clase, o el
próximo refactor de "limpieza" lo deshace.
D15 NO tocada (verificarNoSolapes fuera del radio, esta vez POR CONSTRUCCIÓN y verificado por
git diff). D-F8.2b-iv-a NO crece (el bloque no añade reglas de coherencia al bloqueo).
Suite: solver 78 + app 60, sin regresión (recuento ANTES y DESPUÉS idénticos; CierreFase6HumoTest
verde). solver/src/main SÍ tocado → referencia-codigo-solver.md REGENERADO (619ef34).
modelo_datos_fase1.md NO tocado (ni entidad ni invariante nueva). Frontend NO tocado (es 8.6).
Siguiente: 8.6 (drag&drop, con el contrato YA cerrado en 8.6-A: es teclear Angular, no decidir),
8.4 (pre-validación, D18/D20) o el candidato que se decida al abrir sesión.

### Sesión 68 — Fase 8, Bloque 8.5 (CRUD de catálogo): precondición D31, no código.

Modo híbrido, sin tocar el repo. ALCANCE: el prompt proponía abrir 8.5; se PARÓ en su precondición. La decisión de producto de S67 fija que la mitigación de D31 (enseñar el modelo
DIBUJADO al jefe de estudios) es PRECONDICIÓN de teclear el CRUD, no un extra. Esa conversación NO
ha ocurrido, así que 8.5 sigue bloqueado; la sesión se dedicó a construir la herramienta de la
mitigación. ENTREGABLE (no versionado, "pregunta dibujada" en el sentido de D-F8.6-a): lámina HTML
estática de validación, tres hojas, sobre datos de VOLCADO FIEL (no §6.x): (0) horario completo de
1ºA con toggle de resalte de dos colores —rojo «se parte» (desdoble/agrupamiento) / verde «sigue
junto» (co-docencia LCL)—, es prueba de FIDELIDAD; (1) tramo denso de 1ºESO ampliado (desdoble CyR
+ agrupamiento RefMt), es la PREGUNTA conceptual «¿ves un grupo o cajas?»; (2) PDC 3ºADi opcional,
«¿grupo propio o parte de 3ºA?». Segundo toggle: capa técnica de correspondencia con el modelo,
para el arquitecto, oculta por defecto.
HALLAZGO (jerarquía volcado > §6.x): el bloque del miércoles/viernes 10:00 de 1ºESO tiene CUATRO
destinos (CyR desdoblado + RefMt triple + OyD/FIL3), no tres como decía §6.1. El modelo lo absorbe
sin cambios (partición unificada por bloque temporal); nota añadida a §6.1 en commit de doc aparte.
CONTEO de 1ºA verificado contra el volcado por procesamiento, no de memoria: 21 ordinarias + 5 slots
«se parte» (mié/vie 10:00, lun 13:30, mié 11:30, jue 12:30) + 4 co-docencias LCL = 30. Corrige un
conteo verbal previo del arquitecto («4 slots») que era erróneo.
modelo_datos_fase1.md: SOLO nota en §6.1 (sin entidad ni invariante nueva). solver/src/main NO
tocado → referencia NO regenerada. Suite NO tocada (sesión sin código). Frontend real (app/frontend)
NO tocado: la lámina no es entregable.
PENDIENTE DE USUARIO, desbloquea 8.5: la conversación con el jefe de estudios sobre la lámina. Sus
respuestas a las dos preguntas cierran o reabren D31 y fijan el corte de 8.5. Hasta entonces 8.5
sigue en su precondición.
Siguiente: usar la lámina con el centro y traer sus respuestas; alternativa SIN dependencia de D31 =
8.4 (pre-validación, D18/D20), a decidir al abrir sesión.

### Sesión 69 — Fase 8, Bloque 8.5-A: CRUD REST de Asignatura (piloto del patrón CRUD de catálogo).

Modo híbrido (diseño en el Project, código en Claude Code). 1 commit de
código (f5b95f3). PRECONDICIÓN DESBLOQUEADA: la conversación con el jefe de estudios sobre la lámina
de S68 OCURRIÓ; sus respuestas cierran parcialmente D31 (ver deuda) y habilitan teclear el CRUD.
RESPUESTAS DEL CENTRO (las dos preguntas de la lámina): (1) «¿ves un grupo o cajas?» → CAJAS: el jefe
de estudios piensa el tramo denso como alumnos repartidos en cajas, cada una con su profesor y su aula,
y confirma que así monta los horarios. El modelo Actividad→Plaza→Subgrupo queda validado por el dominio
en su punto más difícil. (2) «¿el PDC 3ºADi es grupo propio o parte de 3ºA?» → «parte de 3ºA que a
ratos sale aparte, mismo tutor», PERO «en pantalla deben mostrarse los dos horarios, el de 3ºA y el del
PDC». Lectura: las dos mitades NO se contradicen y apuntan al modelo QUE YA TENEMOS (§6.2/S9: grupo
administrativo propio con grupo_padre I5). El grupo_padre ES la formalización de «sale aparte»; el
requisito de «dos horarios en pantalla» EXIGE identidad propia (un PDC sin identidad sería una columna
dentro de 3ºA, no un horario aparte), luego confirma el modelo, no lo reabre. La creación del PDC
colgando del padre + tutor heredado + sesiones compartidas marcadas es D1/D7, y por depender de CÓMO SE
GESTICULA la creación, arrastra mockup previo (D-F8.6-a) en su bloque (8.5-D).
CORTE DE 8.5 (fijado con el arquitecto, por dependencias/riesgo/tamaño): 8.5-A CRUD plano de las
raíces (Nivel, Asignatura, Profesor-plano, Aula) → 8.5-B GrupoAdministrativo + Subgrupo ordinarios →
8.5-C Actividad + Plaza (XOR aula, N profes/subgrupos; decisión pendiente del ctor protected) → 8.5-D
PDC + subgrupos compartidos + tutoría heredada (MOCKUP PREVIO, D1/D7) → 8.5-E rejilla de
ProfesorRestriccionHoraria «puede/no puede/prefiere-que-no» + peso (MOCKUP PREVIO, D20). 8.5-A/B/C
matan SeedCatalogoRunner (cubren lo que el seed crea); 8.5-D/E son catálogo que el seed NO cubre. La
rejilla de restricciones horarias del profesor se SEPARÓ de 8.5-A a 8.5-E a propuesta del usuario: no
es formulario plano sino rejilla de 30 celdas con tres estados, y su peso/gesto es UX pura.
QUÉ SE CONSTRUYÓ EN 8.5-A: /api/asignaturas con las 5 operaciones (GET lista ordenada por codigo, GET
/{id}, POST 201, PUT /{id} 200, DELETE 204). 6 ficheros (5 nuevos: AsignaturaDTO, AsignaturaRequest,
AsignaturaService, AsignaturaController, AsignaturaEndpointTest; 1 tocado: Asignatura.java +11).
Solo app/: solver/src/main NO tocado → referencia NO regenerada; modelo NO tocado (§4.1 ya describe
Asignatura). Suite app verde (~11 tests nuevos de AsignaturaEndpointTest), solver intacto.
PRECEDENTE 1 (lo hereda el resto del CRUD): las entidades inmutables (solo ctor+getters, como
Asignatura) reciben MÉTODO DE DOMINIO de actualización —Asignatura.actualizar(codigo, nombreCompleto)—,
NO setters, aunque Actividad/Plaza sí usen setters. Los setters de Actividad/Plaza son residuo del
builder de SeedCatalogoRunner (constructor vacío + setX), andamiaje que morirá; no son convención
elegida. El estilo bueno es el de Asignatura; cuando 8.5-C toque Actividad/Plaza, converger hacia
mutación nombrada. Un actualizar(...) además cierra por construcción el riesgo de setId (el id no es
parámetro). VERIFICADO POR JUICIO (arquitecto): diff de Asignatura.java añade solo actualizar, sin
setId ni otros setters.
PRECEDENTE 2 (lo hereda el resto del CRUD): convención de excepciones → HTTP:
NoSuchElementException → 404 (id inexistente), IllegalArgumentException → 400 (validación). El
controller las distingue POR TIPO, no por endpoint —crítico en PUT, donde ambos códigos son posibles
(404 si el id no existe, 400 si el código pisa a otra)—. Es MÁS limpio que el patrón de bloqueo, que
traduce una misma excepción distinto por endpoint. Validación única en el Service (sin espejo que
reflejar, a diferencia del bloqueo): la unicidad-en-edición se excluye a sí misma
(filter(otra -> !otra.getId().equals(id))). VERIFICADO POR JUICIO: los tests 8 y 9
(edicion_codigoQuePisaAOtra_400 / edicion_guardaMismoCodigo_200) son el par discriminante —el 9 edita
con el MISMO código y espera 200; sin la cláusula de exclusión saldría rojo, luego no es tautológico—.
DEUDA NUEVA: D-F8.5-A-a (DELETE de catálogo borra sin comprobar referencias entrantes → 500 opaco por
FK en vez de 400 amable; aplica a las cuatro raíces; se difiere a 8.5-C o al primer borrado con FK).
Siguiente: 8.5-B (GrupoAdministrativo + Subgrupo ordinarios), replicando el patrón piloto de 8.5-A
sobre Nivel/Profesor-plano/Aula primero si se prefiere consolidar las raíces antes de subir a grupos.

### Sesión 70 — Fase 8, Bloque 8.5-A': CRUD REST de las raíces restantes de catálogo (Nivel, Profesor, Aula), replicando el patrón piloto de 8.5-A.
Modo híbrido (diseño en el
Project, código en Claude Code). 1 commit de código (18 ficheros: 15 nuevos + 3 entidades tocadas,
solo app/). CORTE DE 8.5 (recordatorio): 8.5-A (Asignatura, S69) y 8.5-A' (Nivel/Profesor/Aula, esta
sesión) cierran las RAÍCES planas; siguen 8.5-B (Grupo+Subgrupo, N:M) → 8.5-C (Actividad+Plaza) →
8.5-D (PDC, MOCKUP PREVIO) → 8.5-E (rejilla de restricciones, MOCKUP PREVIO).
QUÉ SE CONSTRUYÓ: tres CRUD REST (/api/niveles, /api/profesores, /api/aulas) con las 5 operaciones
cada uno (GET lista, GET /{id}, POST 201, PUT 200, DELETE 204), replicando los DOS PRECEDENTES de
S69 (método de dominio actualizar(...) en las tres entidades inmutables, sin setId; excepción→HTTP
por TIPO, validación única en el Service, unicidad-en-edición excluida por id). Suite app 73 → 114
(+41: Nivel 12, Profesor 13, Aula 16). solver/ intacto → referencia NO regenerada; modelo NO tocado
(§4.1 ya describe las tres entidades). SeedCatalogoRunner NO borrado (aún faltan Grupo/Subgrupo/
Actividad/Plaza; muere en 8.5-C).
DECISIONES CERRADAS (heredables por el resto del CRUD): (D-1) Nivel ordena su listado por 'orden'
(campo de §4.1 para ordenación UI), NO por 'codigo' como Asignatura/Profesor/Aula; el test discrimina
con orden alfabético y numérico CRUZADOS. (D-3) los enum viajan como String en el BORDE del DTO:
AulaRequest.tipo = String (para dar un 400 accionable en TipoAula.valueOf con el valor malo + lista
de válidos), AulaDTO.tipo = String vía getTipo().name(). Se descartó AulaDTO.tipo=TipoAula: la
verificación del repo mostró que 7A ya serializa enums como String (.name()) y ProyeccionDtoContratoTest
lo blinda (isTextual()); dos reglas de serialización de enum en el mismo paquete app.web.dto sería
deuda de coherencia. Sin asimetría entrada/salida final: String en ambos lados. (D-4) los cuatro
campos nullable de Aula (capacidad/edificio/planta/sector) son opcionales de verdad: entran null, se
persisten null, no se validan; únicos obligatorios de Aula = codigo + tipo.
VERIFICADO POR JUICIO (arquitecto): los tres actualizar(...) mutan solo campos editables, ninguno
asigna id; el par de edición de las tres raíces (editar con el MISMO código espera 200) protege la
cláusula de exclusión por id, no es tautológico; el aserto de orden de Nivel usa el cruce
alfabético/numérico; el 400 de tipo inválido de Aula asevera que el mensaje NOMBRA el valor malo
(reason(containsString("CHUCHE"))), no solo el status; los nullable de Aula se verifican null
explícito tras round-trip. D-F8.5-A-a (DELETE sin comprobar refs → 500 opaco por FK) queda intacta y
ahora aplica formalmente a las CUATRO raíces (decisión confirmada: replicar el piloto, no adelantar
la integridad referencial, que merece su bloque en 8.5-C).
Siguiente: 8.5-B (GrupoAdministrativo + Subgrupo ordinarios, N:M de Subgrupo, toca I1/I6), a decidir
al abrir sesión.

### Sesión 71 — Fase 8, Bloque 8.5-B: CRUD REST de GrupoAdministrativo
  (ordinario) + Subgrupo, con el N:M subgrupo_grupo por códigos. Modo híbrido (diseño en el
  Project, código en Claude Code). 2 commits de código (c21d0e2 Grupo + 744b724 Subgrupo), solo app/.
  CORTE DE 8.5 (recordatorio): A (Asignatura, S69), A' (Nivel/Profesor/Aula, S70) y B (esta sesión)
  cierran raíces planas + grupos/subgrupos ordinarios; siguen 8.5-C (Actividad+Plaza) → 8.5-D (PDC,
  MOCKUP PREVIO) → 8.5-E (rejilla de restricciones, MOCKUP PREVIO).
  HALLAZGO QUE RECORTÓ EL ALCANCE (leído del repo antes de teclear): Particion y SubgrupoParticion
  (§4.2) NO están materializadas como entidad JPA —el javadoc de Subgrupo lo dice: no las consume el
  solver, su UX es D1/D7 en Fase 8 UI—. Consecuencia: I1 (cobertura de partición) queda FUERA del
  contrato de 8.5-B, no como deuda diferida sino porque no hay Particion que validar. El único N:M
  existente es subgrupo↔grupos (población, tabla subgrupo_grupo), ya construido y probado desde
  S22/S48; 8.5-B lo CONSUME desde formulario, no lo construye. Por eso NO se partió el bloque.
  QUÉ SE CONSTRUYÓ: dos CRUD REST (/api/grupos, /api/subgrupos) con las 5 operaciones cada uno,
  replicando los DOS PRECEDENTES de S69 (método de dominio actualizar(...) en ambas entidades
  inmutables, sin setId; excepción→HTTP por tipo, validación única en el Service, unicidad-en-edición
  excluida por id). DTOs planos en app.web.dto con referencias por CÓDIGO de negocio: GrupoDTO/Request
  llevan nivel y tipo como String; SubgrupoDTO/Request llevan grupos como array de códigos String.
  DECISIONES CERRADAS (heredables): (D-nueva-1) Subgrupo rechaza grupos vacío (≥1) → 400. (D-nueva-2,
  lista BLANCA) Grupo acepta solo tipo=ORDINARIO; rechaza DIVERSIFICACION_PDC (es 8.5-D) y
  VIRTUAL_OPTATIVA (es 8.5-C+) → 400 cuyo reason NOMBRA el tipo rechazado. (D-nueva-4) resolución
  N:M y FK Nivel en escritura por bucle findByCodigo (Opción 1); código inexistente → 400 que nombra
  el código faltante. (D-nueva-5) aserto discriminante del bloque = alta de Subgrupo con 2+ códigos +
  round-trip que verifica los CÓDIGOS exactos (containsInAnyOrder), no el tamaño.
  D-nueva-3 RESUELTA A FAVOR POR TEST (era comportamiento de framework, no se afirmó de memoria): el
  borrado de un Subgrupo limpia sus filas de subgrupo_grupo (query nativa cuenta 2→0) sin cascade
  configurado —el @ManyToMany unidireccional limpia su join table al borrar el propietario— y los
  GrupoAdministrativo sobreviven. Test borrado_limpiaJoinTableYNoBorraGrupos. Sin hallazgo que reportar.
  VERIFICADO POR JUICIO (arquitecto): los dos actualizar(...) mutan solo campos editables, ninguno
  asigna id (Grupo no toca grupoPadre, Subgrupo reemplaza el set con copia defensiva, no une); el par
  de unicidad-en-edición (editar con el MISMO código espera 200) no es tautológico; el round-trip de
  Subgrupo asevera códigos, no tamaño; el borrado fuerte cuenta la join table de verdad (0 filas).
  Suite app 114 → 143 (+29: Grupo 14, Subgrupo 15). solver/ intacto → referencia NO regenerada;
  modelo NO tocado (§4.1/§4.2 ya describen las entidades y el N:M). SeedCatalogoRunner NO borrado
  (muere en 8.5-C). D-F8.5-A-a intacta: sigue difiriendo a 8.5-C el borde de FK entrante
  (Plaza→Subgrupo, Nivel→Grupo); 8.5-B no comprueba referencias entrantes en el borrado.
  LIMPIEZA DE FONDO del plan: NO ejecutada (seguimos dentro de 8.5; el default es posponer al cierre
  de 8.5 entero para condensar el bloque completo con criterio uniforme, recomendación de S70).
  Siguiente: 8.5-C (Actividad + Plaza; XOR aula, N profes/subgrupos; decisión pendiente del ctor
  protected; aquí muere SeedCatalogoRunner y muerde D-F8.5-A-a), a decidir al abrir sesión.

### Sesión 72 — Fase 8, Bloque 8.5-C1: CRUD REST de Actividad como AGREGADO
  (Plaza embebida). Modo híbrido. 1 commit de código (solo app/). CORTE DE 8.5 REVISADO EN SESIÓN:
  8.5-C se PARTIÓ en C1 (Actividad+Plaza, esta sesión) → C2 (integridad referencial: activar FK +
  borrado amable) → C3 (I3 + CRUD de AsignaturaAulaCompatible); siguen 8.5-D (PDC, MOCKUP PREVIO) →
  8.5-E (rejilla, MOCKUP PREVIO).
  DECISIONES DE ALCANCE (cerradas con el arquitecto antes de teclear): (D-C1-A) Actividad como
  agregado; Plaza es sub-recurso EMBEBIDO en /api/actividades; NO hay /api/plazas ni PlazaController
  ni PlazaRepository (Plaza se persiste/borra por cascade+orphanRemoval vía Actividad). (D-C1-B) I3
  (asignatura↔tipo aula) FUERA de C1 → C3, porque arrastra materializar el CRUD de
  AsignaturaAulaCompatible y una decisión de tabla-vacía; hoy NADIE valida I3 en escritura, así que no
  es regresión. (D-C1-C) el borrado referencial FUERA de C1 → C2.
  QUÉ SE CONSTRUYÓ: /api/actividades con las 5 operaciones. Validación en ActividadService (ninguna
  delegada a la entidad JPA, que es POJO de persistencia): XOR aula por plaza (aulaFija Y candidatas →
  400; ninguna de las dos → 400), I7 (plaza sin profesor → 400), I2 (subgrupo repetido en dos plazas
  de la misma actividad → 400 que nombra el subgrupo), refs por código (asignatura/profesor/aula/
  subgrupo inexistente → 400 que nombra el código), unicidad de codigo excluida por id en edición.
  Convergencia S69: Actividad.actualizar / Plaza.actualizar (mutación nombrada, sin setId).
  HALLAZGO GRAVE (leído del repo, no de memoria): las FK de SQLite NO están activadas
  (application.properties no fuerza PRAGMA foreign_keys=ON; SQLite las tiene OFF por defecto por
  conexión). Consecuencia viva HOY, no solo tarea futura: borrar un catálogo referenciado probablemente
  NO lanza excepción — deja filas huérfanas SILENCIOSAS que solo se manifiestan cuando el mapper del
  solver resuelve la referencia y encuentra null. Esto REESCRIBE D-F8.5-A-a: el problema no es «500
  opaco por FK» (que quizá ni ocurre) sino «sin integridad referencial real». Verificación en EJECUCIÓN
  de qué hace el driver Xerial + SQLiteDialect ante violación de FK con pragma OFF: NO hecha; hipótesis
  por defecto = sin integridad. Reasignada a C2.
  SEED DEGRADADO (corrige al plan, que decía «SeedCatalogoRunner muere en 8.5-C»): el seed NO puebla
  AsignaturaAulaCompatible (verificado línea a línea: no inyecta su repo, todas sus aulas son
  ORDINARIA) — esa tabla ya estaba vacía hoy, matar el seed no la vacía. C1 cubre lo único que el seed
  crea además de las raíces A/A'/B (Actividad+Plaza) SALVO TramoSemanal, que no tiene CRUD en ningún
  bloque de 8.5 (es D22, config de jornada). Por eso el seed NO muere en C1: se degrada a andamiaje de
  tramos; su muerte total → bloque de configuración de jornada (D22). Javadoc del seed actualizado.
  D-C1-E (código de plaza, CORREGIDA DOS VECES en sesión, matiz final crítico): el usuario NO teclea
  código de plaza (Op-2); se DERIVA {codigoActividad}-P{n}. Contra lo que se supuso al abrir, el grep
  probó que plaza.codigo SÍ es clave de correspondencia del solver (GeneradorHorarioService:209 y
  CatalogoMapper:148 hacen toMap(codigo) que aborta ante duplicados; SolucionMapper:130,247 y
  BloqueoMapper:80 emparejan por código). Por tanto el código debe ser ESTABLE. Regla final:
  reconciliación POSICIONAL en el PUT (emparejar entrante↔existente por orden de creación/id; las
  supervivientes CONSERVAN su código vía Plaza.actualizar; las sobrantes se borran por orphanRemoval;
  las nuevas reciben maxSufijoVIVO+1). MATIZ que NO se puede perder: estable ≠ irrepetible-en-el-tiempo.
  El código de una plaza VIVA no cambia (es lo que el solver necesita); un código LIBERADO por borrado
  SÍ puede reasignarse a una plaza futura distinta, y es seguro porque Sesion/AulaBloqueada referencian
  la plaza por plaza_id (FK al id), no por código. Se RETIRÓ una «regla anti-reuso» (high-water) que el
  arquitecto había metido por error: era más estricta de lo que el solver necesita y exigía estado
  persistido fuera de alcance. Emparejamiento posicional = decisión de UX PROVISIONAL (roza D-F8.6-a):
  se confirma cuando exista el formulario de Actividad en 8.6.
  ASERTOS DISCRIMINANTES (verdes, sin flush() explícito): putEstabilidad (editar contenido NO regenera
  códigos: los 3 idénticos); putReduccion (6→2 plazas sin violar UNIQUE, supervivientes conservan
  código); putReusoDeHueco (borrar P3, la nueva reusa P3, códigos vivos únicos); round-trip del bloque
  CyR/OyD/RefMt (containsInAnyOrder de códigos por plaza, no tamaño); XOR/I7/I2/refs. Suite app 143 →
  162 (+19). solver/ intacto → referencia NO regenerada; modelo NO tocado (§4.6 ya describe Actividad/
  Plaza).
  DEUDA NUEVA: setters de Actividad/Plaza NO retirados pese a la obligación S69: 12 tests verdes previos
  los usan y el contrato prohíbe romper la suite. El Service nuevo no usa ninguno (ctor + actualizar +
  agregarPlaza). Retirada → bloque que migre esos 12 tests.
  Siguiente: 8.5-C2 (integridad referencial) o 8.5-C3 (I3 + compatibilidades), a decidir al abrir sesión.

### Sesión 73 — Fase 8, Bloque 8.5-C2a-DDL: INTEGRIDAD REFERENCIAL DE ESQUEMA
  (schema.sql + FK + pragma por conexión). Modo híbrido. 1 commit de código (d27518f, solo app/) +
  1 commit de doc (plan + modelo §4.7). El bloque nació como «8.5-C2 = activar FK + borrado amable» y
  se transformó dos veces al chocar con la realidad medida; el borrado amable se difirió a 8.5-C2b.
  CADENA DE HALLAZGOS (cada uno reescribió el anterior, todos medidos en ejecución vía Claude Code):
  (1) el HALLAZGO GRAVE de S72 («FK OFF») era INCOMPLETO: el community SQLiteDialect 7.4.1 NO emite FK
  en el DDL de hbm2ddl, ni con @OnDelete ni con @ForeignKey ni combinadas (DDL byte-idéntico). El
  agujero no era «FK declaradas, pragma OFF» sino «FK inexistentes». (2) ddl-auto=validate es INUSABLE
  con el dialecto (crea PK integer, valida esperando bigint; falla incluso contra su propio esquema).
  (3) el DDL del dialecto declara la PK `id` SIN tipo en 8 tablas → en SQLite no es alias de rowid → la
  columna queda NULL y NINGUNA FK entrante resuelve; invisible hasta ahora porque nada recargaba por id
  fresco fuera del caché L1. (4) ni connection-init-sql ni el parámetro de URL propagan el pragma en
  este stack (SB4+Hikari 7.0.2+Xerial 3.53.2); solo PRAGMA explícito por conexión aplica.
  DECISIONES DE ALCANCE (cerradas con el arquitecto, varias reconsideradas EN sesión por el usuario):
  (a) partir 8.5-C2 en C2a-DDL (esquema, esta sesión) + C2b (borrado amable, siguiente). (b) medir el
  comportamiento FK-OFF antes de activar nada (test de caracterización efímero, Op-A; descartado del
  repo tras confirmar el hallazgo, DA-8=Op-B′). (c) tras descubrir que el problema era de DDL: NO H2
  (mantener SQLite por inspeccionabilidad universal del .db y estabilidad de formato; H2 tiene formato
  propietario e incompatibilidad entre mayores). (d) el usuario reconsideró Flyway→DDL-3′ (schema.sql +
  ddl-auto=none), más simple y misma integridad para «una BD por centro»: Flyway daba versionado sin
  retorno aquí. (e) cascadas: CASCADE en plaza.actividad_id + las 3 plaza_id de join + sesion.horario_id
  (coherencia BD↔ORM); resto RESTRICT (la red dura; el 409 lo pone C2b).
  QUÉ SE CONSTRUYÓ: schema.sql (20 tablas VERBATIM del DDL de Hibernate + 27 FK inline, DROP…IF EXISTS
  hijo→padre para idempotencia entre contextos), ddl-auto=none, SqliteForeignKeysConfig (DelegatingDataSource
  que ejecuta PRAGMA foreign_keys=ON en cada getConnection), el fichero AutoConfigureDataJpa.imports que
  SB4.1 exige para que el customizer corra en slices @DataJpaTest, y 2 tests de juicio: IntegridadReferencialTest
  (pragma=1 desde el pool + insert colgante → SQLITE_CONSTRAINT_FOREIGNKEY código 19) e IdPrimaryKeyRoundTripTest
  (persist→clear→find prueba que la PK integer se puebla y el puente Long↔INTEGER no trunca).
  AUDITORÍA (el valor del bloque): 1ª pasada 88/164 (76 rojos), TODOS con causa raíz única = la PK `id`
  NULL (hallazgo 3), no «tests que asumían esquema sin integridad». Normalizadas las 8 PK a `id integer`
  (corrección de un defecto del generador, no desviación del verbatim, aprobada por el arquitecto). 2ª
  pasada: 165/165 verde. Rojos (a) «borrado que ahora falla por FK» = NINGUNO: la suite no ejercita
  «borrar padre con hijos», así que el insumo de C2b NO es un test rojo sino el MAPA DE FK RESTRICT
  derivado (en D-F8.5-A-a). solver/ intacto → referencia NO regenerada; modelo §4.7 SÍ tocado (nota de
  integridad referencial física con la semántica de cascadas).
  DEUDA NUEVA: D-F8.5-C2a-a (.db preexistente con PK NULL en 8 tablas; sin producción hoy → teórico,
  pero un .db de pruebas viejo fallará con FK-ON; migración = recreación). D-F8.5-A-a parcialmente
  resuelta: mitad de esquema CERRADA, mitad de aplicación (borrado amable → 409) VIVA en C2b. Setters de
  Actividad/Plaza siguen sin retirar (deuda de S72, intacta).
  Siguiente: 8.5-C2b (borrado amable) o 8.5-C3 (I3 + compatibilidades), a decidir al abrir sesión.
