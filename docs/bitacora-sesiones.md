# Bitácora de sesiones — Educhronos

Registro detallado e histórico de las sesiones de trabajo S10–S136. Archivado
desde `plan_trabajo_horarios.md` en la Sesión 44 (higiene documental) para
aligerar el plan de trabajo, conservando la traza completa de decisiones.

**Jerarquía de autoridad**: el estado VIVO del proyecto (fase actual, criterios,
deuda consciente abierta, decisiones permanentes, bloques de fase) está en
`plan_trabajo_horarios.md`. Esta bitácora es histórico de solo lectura: no se
consulta para conocer el estado actual, sino para entender por qué se tomó una
decisión pasada. Las cabeceras vivas de sesión las conserva el plan; aquí se
archivan conforme salen de su ventana.

Orden: cronológico ascendente (S10 → S136). Los formatos difieren según la época
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

### Sesión 74 — Fase 8, Bloque 8.5-C2b: BORRADO AMABLE DE CATÁLOGO (409 en
  referencia entrante, en vez de la SQLException de FK cruda). Modo híbrido. 2 commits de código
  (e60680a producción 22 ficheros + 2293d31 tests 4 ficheros, solo app/) + 1 commit de doc (este).
  Cierra la mitad de aplicación de D-F8.5-A-a (la de esquema la cerró S73) → D-F8.5-A-a CERRADA entera.
  DECISIONES DE DISEÑO (cerradas con el arquitecto antes de teclear): (2A) traducción a 409 vía
  excepción de dominio nueva ReferenciaEntranteException lanzada por el Service + catch por endpoint en
  cada Controller; NO se introduce @ControllerAdvice (fiel al patrón vigente S70: cada controller
  traduce las suyas). (b) consulta inversa por @Query NATIVA en el repo de cada raíz, NO derivados
  countBy* que exigirían un PlazaRepository: Plaza es agregado de Actividad (D-C1-A/S72), abrir su repo
  para un chequeo de borrado rompería el agregado; el SQL nativo se acopla a nombres de tabla/columna
  pero schema.sql (en el repo) es su autoridad y el test lo delata si se desvía. (C) híbrido: la
  consulta inversa da el 409 legible, la FK física de S73 es la red dura por debajo. Alcance: 7 raíces
  (Tramo NO tiene CRUD → fuera, D22), Actividad como caso propio.
  QUÉ SE CONSTRUYÓ: ReferenciaEntranteException (record Referencia(String,long), filtra a conteo>0,
  mensaje que nombra referente+conteo) + @Query nativas de conteo inverso en los 7 repos + guarda en
  cada Service.borrar (consulta inversa ANTES de delete; si hay referencias, 409) + catch→409 en cada
  Controller. Caso Actividad: NO cuenta sus plazas (cascadean); cuenta sesion_bloqueada.actividad_id,
  la FUSIÓN aula_bloqueada (actividad_id OR plaza_id IN plazas-de-la-actividad, unidas con OR porque
  una fila satisface ambas FK y contarlas por separado la duplicaría — única @Query no-1:1 del bloque)
  y la TRAVESÍA sesion (plaza_id IN plazas-de-la-actividad; sesion no tiene actividad_id).
  HALLAZGO (cazado por la suite completa, no por los tests nuevos): el §B del contrato contó por error
  subgrupo_grupo.subgrupo_id como referente entrante de Subgrupo. Es su POBLACIÓN PROPIA: Subgrupo es
  owner del @ManyToMany grupos, así que Hibernate emite DELETE de subgrupo_grupo al borrar el subgrupo
  ANTES del delete → no bloquea. Contarla hacía que NINGÚN subgrupo fuera borrable (el endpoint exige
  ≥1 grupo). Corregido: SubgrupoService.borrar cuenta solo plaza_subgrupo (owner Plaza = externo real).
  Asimetría confirmatoria: GrupoService SÍ cuenta subgrupo_grupo.grupo_id porque Grupo NO es owner
  (Hibernate no lo limpia al borrar un grupo). REGLA DERIVADA: referencia entrante = FK que un TERCERO
  controla, no toda FK que apunte a mi id; las FK del propio agregado (que Hibernate limpia al borrarme)
  no cuentan. El mapa de FK de S73 era CORRECTO (ya listaba solo plaza_subgrupo para Subgrupo); el error
  fue del §B de esta sesión, no del insumo. Verificación cruzada de las 7 raíces (grep de
  @OneToMany/@ManyToMany, no de memoria): Subgrupo era el único falso positivo.
  ASERTOS DISCRIMINANTES (4 tests nuevos, cada uno VERIFICADO POR MUTACIÓN — romper el @Query pone el
  test rojo, restaurarlo verde; el desalineado de ids es lo que hace la mutación detectable, porque con
  ids colisionando una @Query desviada a otra columna cuenta por accidente): test 1 Aula (2 plazas por
  aula_fija → 409, containsExactly cierra las otras 3 FK a 0); test 3 Actividad (Sesion+AulaBloqueada
  sobre su plaza → 409, travesía probada por M1, deduplicación del OR probada por M2c=suma da 2;
  M2a/M2b —quitar una rama del OR— indetectables con datos legítimos y es honesto que así sea, reportado
  no tapado); test 4 Nivel (réplica); test positivo de Subgrupo (usado por plaza → 409, containsExactly
  blinda que subgrupo_grupo NO reaparezca). Suite app 169 verde (+4 sobre 165), demostrada no-vacía
  (break-restore de AulaService.borrar → Failures:1 → restaurado verde). solver/ intacto → referencia
  NO regenerada. modelo NO tocado (§4.7 ya tenía la nota de integridad de S73; sin entidad ni invariante
  nueva). Tres cortes de servicio de Claude Code en sesión (2 «mid-response» + 1 529), ninguno dejó el
  árbol sucio (diagnosticado cada vez); mitigados fragmentando los turnos (un test por turno).
  DEUDA NUEVA: ninguna. Vivos para cerrar 8.5: C3 (I3 + AsignaturaAulaCompatible) y D/E (MOCKUP PREVIO).
  Siguiente: 8.5-C3 (I3 + compatibilidades) o 8.5-D/E, a decidir al abrir sesión.

### Sesión 75 — Fase 8, Bloque 8.5-C3: I3 + CRUD DE COMPATIBILIDADES
  asignatura↔tipo de aula. Modo híbrido. 4 commits de código (por concern + tests aparte) + doc.
  MEDICIÓN PREVIA (§A, instrumento efímero descartado, patrón Op-A/S73) que REENCUADRÓ el bloque:
  M1=0 filas en asignatura_aula_compatible; M2=0 asignaturas restringidas; M3/M4=0 pero
  TAUTOLÓGICOS (con 0 compatibilidades ninguna plaza puede ser incompatible: ceros por
  consecuencia lógica, no por evidencia); M5 = 11 aulas, TODAS `ORDINARIA`, porque
  SeedCatalogoRunner las creaba con el tipo HARDCODEADO. HALLAZGO: `TipoAula` era COLUMNA MUERTA
  → I3 no tenía un lado, tenía cero. La medición desmintió mi propia recomendación de apertura
  («riesgo bajo y acotado»), que era suposición sobre el estado de los datos.
  DECISIONES CERRADAS antes de teclear: (C) semántica de tabla vacía POR ASIGNATURA (0 filas ⇒
  irrestricta; ≥1 ⇒ solo esos tipos) — descartadas «todo permitido» global (pierde granularidad)
  y «nada permitido» (obliga a poblar el catálogo entero antes de que el sistema arranque, para
  un usuario no técnico). Sub-recurso GET/PUT con REEMPLAZO TOTAL (no CRUD de fila con id
  sintético): la unidad de la operación coincide con la del gesto del usuario y el id sintético
  —que la entidad documenta como concesión a JPA— no sale nunca al API; precedente de idempotencia
  en POST /api/bloqueos (S66). Compatibilidades = POBLACIÓN PROPIA de Asignatura (cascade), lo que
  REVIERTE la Referencia de S74. I3 valida aulaFija Y TODAS las candidatas (una candidata
  incompatible da un horario que viola I3 cuando el solver la elige).
  PUNTO ÚNICO (lo más delicado del bloque): `crearPlaza` y `aplicarContenido` NO compartían funnel
  —resolvían las 5 piezas por separado hacia sinks distintos (agregarPlaza vs plaza.actualizar)—.
  Se CREÓ el funnel `resolverContenido(PlazaRequest, Map cacheI3)` → record `ContenidoPlaza`.
  Descartado un `validarI3(...)` suelto llamado desde ambos: la lógica estaría una vez pero serían
  DOS CALL-SITES, y un tercer camino futuro podría olvidarlo sin que la suite se pusiera roja. Con
  el funnel la validación es inevitable POR CONSTRUCCIÓN (quien quiera contenido de plaza pasa por
  ahí), no por disciplina — que es justo lo que D-F8.2b-iv-a lamenta no tener. Caché
  Map<Long,Set<TipoAula>> LOCAL a la operación (no campo: el servicio es singleton y quedaría rancia).
  TIPIFICACIÓN INCIDENTAL (2 líneas del seed, no sub-bloque propio: el seed muere en 8.5):
  B07→TALLER_TEC DERIVADO del volcado aula-B07.json (Tec/TEC/TecIn/CyR); A12In→INFORMATICA por
  DECISIÓN DEL ARQUITECTO contra el PDF «A12 Informática» —no derivado: el seed usa `A12In` y el
  volcado `codigo_crudo` = `A12 Informática`, único de los 11 que no casa exacto (roza D8-1)—. Las
  otras 9 son aulas ordinarias de grupo, verificado contra sus volcados. NO se amplió el seed a las
  35 aulas reales (invertir en componente condenado) → oro SINTÉTICO, patrón S41.
  HALLAZGO COLATERAL del test de cascada: en `@DataJpaTest` de una sola transacción las filas hijas
  quedan MANAGED y Hibernate no conoce la cascada de BD; hizo falta flush()+clear() para desligarlas
  y flush() explícito tras el DELETE (el autoflush ante un count() sobre la hija no dispara el DELETE
  del padre). Sin eso el test observaría la caché L1, no la base. En producción no aplica (PUT y
  DELETE son transacciones distintas). Mismo género que S73 (pragma que no se propaga) y S74 (falso
  positivo de Subgrupo): el framework media entre lo que crees probar y lo que pruebas.
  ASERTOS: 8 tests de compatibilidades + cascada VERIFICADA POR MUTACIÓN (quitar `on delete cascade`
  → SQLITE_CONSTRAINT_FOREIGNKEY en el DELETE del padre → restaurar → verde) + 6 de I3, entre ellos
  el DISCRIMINANTE DE (C): asignatura sin compatibilidades + aulaFija ORDINARIA → 201; si la
  semántica pasara a «vacío = nada permitido» ese 201 sería 400 y el test caería. Suite 184 verde,
  demostrada no-vacía (neutralizar validarI3 → 3 rojos esperados → restaurar → verde).
  ALCANCE HONESTO: el bloque entrega MECANISMO Y SUPERFICIE, no una restricción activa: el catálogo
  real sigue con 0 compatibilidades declaradas y poblarlas es trabajo de usuario en la UI.
  solver/ intacto → referencia NO regenerada. modelo §4.1/I3 SÍ tocado (commit aparte).
  DEUDA NUEVA: D-F8.5-C3-a (COMUN sin semántica), D-F8.5-C3-b (códigos por currículo).
  Vivos para cerrar 8.5: D y E, ambos MOCKUP PREVIO.
  Siguiente: 8.5-D o 8.5-E (el primero de los dos empieza por MOCKUP, no por código), o 8.4.

### Sesión 76 — Fase 8, Bloque 8.5-D1: alta de grupo PDC como sub-recurso del padre.
  Modo híbrido. 3 commits de código (ef14331 producción, 789a6c3 tests, 150e127 corrección) + doc aparte.
  REENCUADRE DEL BLOQUE (hallazgo de la §A): 8.5-D estaba MAL DIMENSIONADO en el corte de S69, que lo
  etiquetó como bloque de UI. La medición demostró que arrastra ESQUEMA: Particion/SubgrupoParticion NO
  existen en JPA (ausencia deliberada, D-a del Bloque 4/S48) y ProfesorTutoria NO existe (solo el
  booleano Actividad.requiereTutor). 8.5-D se PARTE en D1 (PDC sobre entidades existentes, esta sesión),
  D2 (tutoría, entidad nueva) y D3 (particiones, dos tablas que el solver NO lee).
  §A DE MEDICIÓN (patrón S73/S75): el instrumento propuesto —test JUnit sobre la BD— resultó INVIABLE
  (mediría el vacío: el seed no crea ningún PDC). Se reorientó a los volcados del centro. SALIDA:
  5 PDC, todos 1:1 con su padre, emparejamiento inequívoco por diagonal. Compartidas/propias 8/22 en
  3ºESO pero 4/26 en 4ºESO → el ratio NO es constante del dominio. PARCIALES = 0 en los 5 (una celda es
  copia exacta del padre o completamente propia, nunca a medias). Tutoría heredada CONFIRMADA en los 5
  (misma celda: tramo, profesor y aula). El 8/22 de S23 queda validado por el centro y a la vez
  generalizado mal: medir solo 3º habría bastado para equivocarse.
  DECISIONES CERRADAS: (D1-1) sin flag de «sesión compartida», se DERIVA en cliente cruzando el campo
  `grupos` que SesionVistaDTO ya trae (PARCIALES=0 lo justifica) → NO se tocó SesionVistaDTO ni
  proyectar(); (D1-2) un PDC por padre → 400; (D1-3) código del PDC lo escribe el usuario (medido:
  «3º ESO PDC» no contiene la letra del padre); (D1-4) subgrupo mono-Di automático con grupos={PDC}
  únicamente (regla S23); (D1-5) I5 validada en escritura; (D1-7) ruta de entrada única desde la ficha
  del padre → SUB-RECURSO /api/grupos/{idPadre}/pdc (patrón del sub-recurso de S75), NO se relajó la
  lista blanca ORDINARIO de GrupoService; (D1-8) celda heredada = barra lateral sin tinte, el fondo
  queda para las capas de 8.6; (D1-9) el PDC vive en una SECCIÓN de la ficha del padre, no en pestaña
  (una pestaña estaría en los 23 de 28 grupos sin PDC); (D1-10) el resumen «N propias · M heredadas»
  debe contemplar el estado SIN horario generado, que es el inicial de toda instalación.
  DECISIÓN DEL ARQUITECTO (no derivada de dato): código del subgrupo mono-Di DERIVADO como
  codigo+"-Completo" (convención de SeedCatalogoRunner); colisión → 400 que NOMBRA el código derivado.
  CORRECCIÓN DE PATRÓN durante el contrato: D1-2 pasó de 409 a 400 — 409 está reservado en todo el
  proyecto a ReferenciaEntranteException; «el padre ya tiene un PDC» es validación de entrada, no
  referencia entrante.
  HALLAZGO DE REVISIÓN (por juicio, tras el primer turno): la guarda de D1-2 usaba contarGruposHijos,
  que cuenta CUALQUIER hijo por grupo_padre_id, no solo los PDC. Acoplamiento accidental a un hecho
  actual —mismo género que el falso positivo de subgrupo_grupo en S74: contar la FK correcta por la
  razón equivocada—. Corregido a findByGrupoPadre_Id + filtro por tipo. NO cambió ningún test verde
  (hoy ambas cuentas coinciden), y por eso había que arreglarlo mientras era visible.
  ASERTOS: 5 discriminantes. El (2) es el que protege S23 —contenido del Set (contains/doesNotContain),
  no size()—, con flush+clear para leer la BASE y no la caché L1. El (4), por mutación, reveló que la
  guarda de aplicación y la FK física protegen lo mismo: al quitarla el test cae por 500
  (TransientPropertyValueException), no por 204 — patrón híbrido de S74, javadoc corregido para no
  mentir. Suite 184 → 196 (+12).
  El mockup NO se versiona (D-F8.6-a: es una pregunta dibujada); sobreviven sus decisiones, arriba.
  solver/ intacto → referencia NO regenerada. modelo_datos_fase1.md NO tocado (ni entidad ni invariante
  nueva: I5 ya está descrita en §5.1).
  DEUDA NUEVA: D-F8.5-D1-a (tutoría del PDC no modelada), D-F8.5-D1-b (1:1 es decisión del arquitecto
  sobre 5 casos).
  Siguiente: 8.5-D2 (tutoría: entidad nueva ProfesorTutoria + I4 + herencia PDC←padre), 8.5-D3
  (particiones, evaluar si procede), 8.5-E (MOCKUP PREVIO) o 8.4.

### Sesión 77 — Fase 8, Bloque 8.5-D2a: ProfesorTutoria, I4 en
  escritura y herencia PDC←padre.
  Modo híbrido. 2 commits de código (producción + tests, solo app/) + 2 de doc (plan, modelo §4.3).
  §A DE MEDICIÓN sobre CÓDIGO Y SEED (no sobre datos: el estado a medir era el del repo, y un test
  habría medido lo mismo con más ceremonia). SALIDA: `requiereTutor` está VIVO como superficie y
  MUERTO como semántica —entra por REST (ActividadRequest/ActividadDTO), lo persiste
  ActividadService, y MUERE en el mapper: `CatalogoMapper:247` documenta que NO se propaga al
  dominio, y `CatalogoMapperActividadTest:136` es un test que ASEVERA el olvido (D-B5-5)—. Cero
  apariciones de tutoria/tutor en src/main fuera de esa columna; el seed no marca NINGUNA tutoría
  (la entidad nace vacía, sin migración de datos); `GrupoAdministrativo` no tiene campo de tutor.
  Línea base: solver 78 + app 196.
  REENCUADRE POR LA MEDICIÓN: la casilla del plan decía que D2 «habilita que el solver consuma S8»,
  y eso resultó ser TRES cosas encadenadas, no una: (1) la entidad en JPA —barato, app/—; (2)
  propagar requiereTutor al dominio + transportar ProfesorTutoria al ProblemaHorario —rompe D-B5-5,
  toca solver/src/main—; (3) verificar S8. 8.5-D2 se PARTE en D2a (catálogo, esta sesión) y D2b
  (solver, diferido). NOTA DE DISEÑO que sobrevive al corte: S8 NO es restricción de scheduling —no
  depende del tramo elegido, es verificable sobre el catálogo sin resolver nada—, luego NO va en
  ModeloCpSat; meterla en CP-SAT sería un error de diseño.
  DECISIONES CERRADAS: (D2a-1) @IdClass con PK compuesta (profesor_id, grupo_id), coherente con el
  catálogo, que no usa value objects; consecuencia aceptada: la PK permite que un profesor sea
  TUTOR_PRINCIPAL de N grupos —§4.3 no lo prohíbe e I4 tampoco—, no se añade guarda que el modelo no
  pide. (D2a-2) I4 en escritura, segundo TUTOR_PRINCIPAL → 400 (validación de entrada, no
  ReferenciaEntranteException; patrón corregido en S76). (D2a-3) sub-recurso GET/PUT
  /api/grupos/{id}/tutoria con REEMPLAZO TOTAL idempotente, patrón literal de
  AsignaturaService.reemplazarAulasCompatibles (deleteAll + flush ANTES de insertar); I4 se valida
  sobre la lista ENTRANTE, antes de tocar la base. (D2a-4) herencia PDC←padre por COPIA en el alta,
  no por derivación en lectura: lo que el centro dijo («mismo tutor») es un hecho de HOY, no una
  regla del dominio, y la derivación haría IMPOSIBLE lo que la copia solo hace incómodo (el PDC
  puede editar su tutoría después, D2a-7); si el padre NO tiene tutor, el PDC nace sin tutoría y NO
  es error —el orden de alta no está garantizado—; los co-tutores NO se heredan (lo medido en S76 es
  la herencia de una CELDA, no de una estructura de co-tutoría). (D2a-5) ASIMETRÍA DELIBERADA de
  borrado: la tutoría es POBLACIÓN PROPIA del Grupo (FK ON DELETE CASCADE, GrupoService.borrar NO se
  toca, criterio de S75 con las compatibilidades) y REFERENCIA ENTRANTE del Profesor (409), porque
  borrar un profesor dejaría grupos sin tutor EN SILENCIO. (D2a-6) schema.sql: FK profesor RESTRICT,
  FK grupo CASCADE, CHECK sobre rol.
  CORRECCIÓN DE ARRASTRE (fuera del alcance nominal, hecha porque estaba delante): PdcService.obtener
  y PdcService.borrar usaban findByGrupoPadre_Id SIN filtrar por tipo. S76 corrigió eso SOLO en
  crear(); el mismo argumento —el método devuelve CUALQUIER hijo, hoy coinciden por accidente—
  aplicaba a los otros dos, y en borrar() era peor: habría borrado un hijo no-PDC creyendo que lo era.
  ASERTOS: 9 discriminantes + 1 que añadió Claude Code y cazó un javadoc que MENTÍA (decía que
  profesor y rol se validaban en pasadas separadas; el código hacía un bucle único, y con rol malo en
  el elemento 1 y profesor inexistente en el 2 daba 400 en vez de 404 — se corrigió el CÓDIGO para
  cumplir el contrato, no el javadoc para excusar el código). El (A2) discrimina I4 «uno por grupo»
  de una regla más fuerte que nadie pidió: el mismo profesor TUTOR_PRINCIPAL de dos grupos → 200.
  A5 POR MUTACIÓN: neutralizada la guarda de I4, el fallo llega por 200, NO por 500 de constraint —y
  esa es la diferencia con S76, donde guarda y FK física protegían lo mismo: aquí la guarda es LO
  ÚNICO que existe, porque la PK compuesta no puede expresar «un principal por grupo». El javadoc ya
  lo afirmaba antes de mutar, así que no hubo que corregirlo. A6 POR MUTACIÓN: quitado el CASCADE →
  SQLITE_CONSTRAINT_FOREIGNKEY en el DELETE del grupo → restaurado → verde.
  HALLAZGO DE FRAMEWORK (familia S73/S74/S75/S76): A8/A9 fallaban con TransientPropertyValueException
  porque en @DataJpaTest todo comparte transacción y la tutoría recién escrita seguía MANAGED al
  borrar su grupo. El arreglo fue del TEST (flush+clear para modelar producción), no de producción:
  verificado por juicio que no hay camino real en que eso ocurra —GrupoService.borrar nunca carga
  tutorías, y la única transacción que escribe tutoría y toca grupos a la vez es PdcService.crear,
  que CREA, no borra—.
  Suite 196 → 212 (+16), solver 78 intacto. Demostrada no-vacía (comparador invertido → rojo →
  restaurar). solver/ intacto → referencia NO regenerada. modelo §4.3 SÍ tocado (commit aparte).
  DEUDA NUEVA: D-F8.5-D2a-a (I4 sin red bajo la aplicación), D-F8.5-D2a-b (incoherencia 404/400 en FK
  por clave natural, defecto de especificación del arquitecto). CIERRA D-F8.5-D1-a.
  8.5-D3 APLAZADO INDEFINIDAMENTE con criterio de reapertura explícito (no es arrastre).
  Siguiente: 8.5-E (MOCKUP PREVIO), 8.4 (pre-validación, D18/D20) o 8.5-D2b (solver), a decidir al
  abrir sesión.

### Sesión 78 — Fase 8, Bloque 8.5-E: CRUD REST de ProfesorRestriccionHoraria (CIERRA 8.5).
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

### Sesión 79 — Fase 8, Bloque 8.4-A: PRE-VALIDACIÓN por condiciones necesarias (D18).
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

### Sesión 80 — HIGIENE DOCUMENTAL: condensación de 8.5 y archivado de ventana (sin código).
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

### Sesión 82 — Fase 8, Bloque 8.6-iii-A: contrato de lectura del diagnóstico en el cliente.
  Modo híbrido. 3 commits de código (0e0bffd producción, a876e5d tests, 8e8cb71 el `it` (9)) + doc
  aparte. §A DE MEDICIÓN sobre el CONTRATO REAL DEL SERVIDOR (lectura literal de los 5 DTOs +
  controller; instrumento más barato, precedente S77-S81).
  8.6-iii PARTIDO en A/B al abrir. Razón: su casilla pedía TRES concerns —cablear 8.3-C, pintar tres
  capas, y el gesto de despinar de D-F8.6-ii-b, que además lleva mockup—. iii-A es la parte que NO
  necesita mockup y entrega contrato verificable; iii-B es la pintura y CONSERVA el MOCKUP PREVIO
  OBLIGATORIO. Orden elegido a propósito: medir ANTES de dibujar, porque un mockup sobre datos
  imaginados fija un contrato que el backend puede no poder alimentar.
  SALIDA DE §A: NO existe `DiagnosticoController` —el endpoint es `GET /api/horarios/{id}/diagnostico`
  en `HorarioController:85`, colgado del controller de horarios—. HALLAZGO FAVORABLE: `CeldaRefDTO`
  lleva (`actividadCodigo`, `indice`), la clave de D-6 que `SesionVista` ya trae y que `clavePin` ya
  computa → iii-A NO toca backend ni `SesionVistaDTO`. Los tipos homónimos de `solver/cpsat` son pares
  espejo al otro lado de la frontera dura: `solver/src/main` NO se tocó (solo se leyó) → referencia NO
  regenerada.
  HALLAZGO ESTRUCTURAL, que es el núcleo del bloque: los dos lados del DTO NO tienen la misma forma.
  `CeldaRefDTO` lleva `plazaCodigo` NULLABLE (no-null solo en `SOLAPE_AULA`); `PenalizacionDTO` NO lo
  lleva EN ABSOLUTO y APLANA la celda a (`actividadCodigo`, `indice`), con el javadoc argumentando que
  «un campo siempre null es un campo que miente». La asimetría D15 se COPIA al TS, no se normaliza:
  son dos índices distintos, no uno.
  DECISIONES CERRADAS: (iiiA-1) `diagnostico.model.ts` NUEVO en `models/`, no ampliar
  `horario.model.ts`, que acota su alcance a Fase 7 en su cabecera (precedente literal
  `bloqueo.model.ts:6-7`). (iiiA-2) espejo FIEL con la asimetría copiada: `regla` es `string` y no
  unión de literales (patrón `horario.model.ts:31-32`, donde `estado` es string pese a ser enum),
  `Totales` son tres `number` y NUNCA `number | null` —ensanchar es tan infiel como estrechar—.
  (iiiA-3) `DiagnosticoService` TS propio. (iiiA-4) DOS funciones puras en `horario/diagnostico.ts`,
  reutilizando `clavePin` de `pines.ts` (fuente única de D-6); `indicePenalizaciones` SIN sumatorio de
  `delta`, porque el javadoc de `TotalesDTO` dice que los totales NO tienen por qué coincidir con la
  suma de los delta contrafactuales y acumularlos invitaría a contrastarlos y a ver un bug donde no lo
  hay. (iiiA-5) el servicio devuelve el `Observable` crudo y DOCUMENTA el 404 en TSDoc, sin traducir.
  TRES ERRORES DEL ARQUITECTO, los tres destapados por Claude Code al contrastar el contrato ANTES de
  teclear (registrados, no tapados). (1) Omití `ViolacionDTO.descripcion` del espejo: omisión, no
  exclusión deliberada; entra. (2) La razón que di para (iiiA-3) —«`HorarioService` es de solo
  lectura»— NO DISCRIMINA: `DiagnosticoService` también lo es. El argumento correcto, que es el que va
  al TSDoc, es «cada contrato/bloque tiene su cliente» (precedente `BloqueoService`, S81). Conclusión
  correcta, porqué mal enunciado. (3) Di por supuesto un patrón de manejo de error HTTP «mirando
  `horario.service.ts` y sus specs»: NO TIENE SPECS, y NINGÚN servicio del frontend los tiene. Afirmé
  cómo estaba algo sin comprobarlo — mismo género que S81, QUINTA sesión consecutiva.
  ANOMALÍA DE FORMATO DESTAPADA POR LA MEDICIÓN DE R4, no por criterio: las 4 cabeceras vivas del plan
  NO son homogéneas (la más reciente es `### Sesión NN`, las otras tres son texto plano con prefijo
  «Última sesión registrada (previa):»), mientras que TODAS las entradas de la bitácora son
  `### Sesión NN`. Archivar exige PROMOVER la línea, no solo moverla. Es costumbre observable
  (entradas S69-S77), no norma escrita: D-F8.0-a sigue viva.
  ASERTOS: 8 tests + el (9) del desdoble. TRES MUTACIONES sobre `diagnostico.ts`, todas caen y
  restauran verde. M1 (clave solo por `actividadCodigo`) pone rojo `diagnostico.spec.ts` (1)(6), NO
  solo `pines.spec.ts`: el requisito crítico de S81 —reutilizar una función no hereda su cobertura—
  se cumple, MEDIDO, gracias al fixture de misma actividad con índices 1 y 2. M2 (indexar solo
  `celdas[0]`) y M3 (propagar `celdas[0].plazaCodigo`) NO son disjuntas y NO PUEDEN SERLO: detectar M3
  exige una entrada cuyo `plazaCodigo` difiera del de `celdas[0]`, y esa entrada procede de una celda
  que M2 elimina. M3 ⊂ M2 por construcción del operador, no por fixture flojo. Se rechazó redefinir M2
  para forzar disjunción: sería una mutación que nadie escribiría, y perseguir la métrica no es
  perseguir cobertura. CONCLUSIÓN REGISTRADA: la mutación M3 no aporta sobre M2; el TEST (3) sí, y es
  el único que asevera la propagación de plaza que iii-B necesitará.
  DISTINCIÓN NUEVA, aportada por Claude Code al corregir su propia predicción: CAER ante una mutación
  ≠ DISCRIMINAR la dimensión que ataca. El `it` (9) cae ante M1 por ACOPLAMIENTO a `clavePin` (la
  lectura del test deja de casar con la escritura del índice), no por discriminar índice-vs-actividad
  —sus dos celdas comparten actividad E índice—. El mérito de esa dimensión sigue siendo del (1). La
  tabla de mutaciones NO se lee como matriz de cobertura.
  MEDICIÓN ADICIONAL sobre `solver/` (lectura, sin tocar), disparada por una pregunta de Claude Code
  que el contrato no cubría: ¿emite el verificador dos `CeldaRef` de la MISMA instancia con plazas
  distintas? SÍ, y está en tres eslabones —`VerificadorSolucion:603-612` añade una `CeldaRef` POR
  PLAZA al `ArrayList` de la clave `Aula`; `reportarColisiones:646-655` emite UNA sola `Violacion` con
  la lista entera; `Violacion:39` usa `List.copyOf`, que NO deduplica—. Los comentarios 593-600 lo
  declaran deliberado (por S2, dos plazas de una instancia en la misma aula son colisión, no uso
  compartido). Como SALIDA DEL SOLVER es estado IMPOSIBLE (lo bloquean `restriccionNoSolapeAula` por
  candidatas y `ProblemaHorarioMapper:315-326` por `aulaFija`), pero el diagnóstico NO verifica lo que
  el solver acaba de emitir: verifica lo RECONSTRUIDO DESDE PERSISTENCIA (`SolucionMapper`), donde
  ninguna de las dos barreras interviene. El `it` (9) se escribe como FIXTURE DEFENSIVO DECLARADO
  (precedente S81: una función pura se prueba contra su contrato, no contra la validez del horario) y
  fija que la `Violacion` repetida bajo una clave es contrato, no bug: deduplicar es del consumidor.
  HALLAZGO COLATERAL sobre el solver, no arreglado aquí: el ÚNICO test de `SOLAPE_AULA`
  (`VerificadorSolucionAtribucionTest:109-139`) usa dos actividades mono-plaza; el caso intra-instancia
  NO está cubierto por ningún test del solver.
  Suite frontend 13 → 22 (+9), backend intacto (app 315, solver 78). Demostrada no-vacía por las tres
  mutaciones. Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO
  regenerada, `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva).
  `PROYECCION_1ESO` NO contaminado: fixtures LOCALES en el spec (patrón S41/S81).
  DEUDA NUEVA: D-F8.6-iiiA-a (ningún servicio del frontend tiene test), D-F8.6-iiiA-b (`Totales` sale
  modelado y sin consumidor), D-F8.6-iiiA-c (S2 sobre `aulaFija` validada SOLO en la ruta JSON).
  Siguiente: 8.6-iii-B (MOCKUP PREVIO OBLIGATORIO; hereda D-F8.6-ii-b y D-F8.6-iiiA-b), 8.4-B (MOCKUP
  PREVIO) u 8.5-D2b (solver, regenera la referencia), a decidir al abrir sesión.

### Sesión 83 — Fase 8, Bloque 8.6-iii-B1: gesto de despinar e índice de pines con id.
  Modo híbrido. 2 commits de código (3d96b7c producción, 98aab08 tests) + doc aparte. §A DE MEDICIÓN
  sobre el ESTADO REAL DEL CLIENTE (lectura literal de los 10 ficheros de `frontend/` implicados +
  `find`/`ls`; instrumento más barato, precedente S77-S82).
  8.6-iii-B PARTIDO en B1/B2 al abrir. B1 = mockup + estructura de estado + gesto de despinar +
  recarga del índice; B2 = badge del delta y los dos resaltes de violación. Razón del corte: el
  despinar cambia la ESTRUCTURA de estado del cliente (`Set`→`Map`) y pintar encima de una estructura
  que va a mutar es rehacer trabajo.
  SALIDA DE §A: el estado del pin vive en `HorarioView` (`signal<ReadonlySet<string>>`), NO en la
  rejilla, que solo lo consulta por `input`. `Bloqueo.id` EXISTE y es `number | null` por decisión
  deliberada de S81 (espejo del DTO), pero `BloqueoService.borrar(id: number)` exige no-nulo: la
  costura estaba oculta porque `borrar()` tenía CERO llamadas (confirmado por grep).
  DECISIÓN (B) sobre (A)/(C): el índice pasa a `Map<string, number | null>` —no se descartan los
  `id` nulos al indexar ni se mide el backend para saber si el null es alcanzable, que es lo que S81
  declaró impropio de un bloque de frontend—.
  HALLAZGO DEL MOCKUP (las 4 capas dibujadas juntas, aunque B1 solo implemente la de pinado, para que
  B2 no tenga que recolocar): pinado y violación de profesor/subgrupo COMPETÍAN por el mismo borde.
  Resuelto liberando el `border-left` en B1 (α).
  HALLAZGO QUE CIERRA UNA PREGUNTA DE UX SIN DIBUJARLA: el gesto de despinar NO puede ser «soltar en
  el origen» —ese slot es donde la instancia sigue pintada, porque la proyección no se recarga al
  pinar— ni «botón en el aviso» —el aviso es un `<p>` global que no conoce la instancia—. Solo el
  candado tiene acceso a la instancia: la medición resolvió la alternativa que D-F8.6-ii-b dejaba
  abierta, no el dibujo.
  TRES ERRORES DEL ARQUITECTO, destapados por el turno de CONTRASTE de Claude Code ANTES de teclear
  (patrón S82: contrato criticado en un turno propio, sin escribir código): (1) C7 especificaba CSS
  YA IMPLEMENTADO en S81 —`position: relative` (`horario-grid.css:27`), candado absoluto (:41-47) y
  fondo tenue (:36-39)—, y peor: hizo confirmar al usuario una decisión INEXISTENTE («borde vs fondo»
  cuando ya era fondo). La decisión REAL, que el error tapaba, era si se libera el `border-left`.
  (2) C6 decía «también desde `cargar(id)`» conservando la del constructor: como `paramMap` emite en
  init, habrían quedado DOS `GET /api/bloqueos`. (3) el contrato NO nombraba `pines.spec.ts:24`, que
  `Map` rompe por `toEqual` entre constructores distintos. SEXTA SESIÓN CONSECUTIVA (S78-S83) con el
  mismo género de error: afirmar cómo está el código sin comprobarlo —y esta vez tras haber PEDIDO
  el fichero y escribir igualmente sin leerlo—.
  OBJECIÓN SUYA ACEPTADA (O2), que cambió el contrato: `despinar` emite la CLAVE (`output<string>`),
  no el `id`. La rejilla habla identidad de DOMINIO (`clavePin`, D-6); el `id` es identidad de FILA, y
  meterlo en la presentación la obligaba a ramificar `<button>`/`<span>` por un detalle de
  persistencia. `HorarioView` resuelve clave→id contra su Map y hace no-op si es null: la rejilla no
  conoce nulls y el candado es SIEMPRE botón. Consecuencia asumida: el «estado 3» del mockup (candado
  inerte) desaparece como estado visual.
  F1 RESUELTA POR EVIDENCIA, no por criterio: `cdkDragHandle` NO hace falta. El CDK no registra
  ningún listener de `click` (grep sobre `drag-drop.mjs`: cero coincidencias) y el arrastre no arranca
  hasta superar `dragStartThreshold: 5` px (`drag-drop.mjs:589`, :896-901), así que un click sin
  desplazamiento no puede reinterpretarse como arrastre.
  TESTBED RECHAZADO A PROPÓSITO, contra la propuesta de Claude Code: estrenar la primera
  infraestructura de test de componente del repo dentro de un bloque cuyo valor es otro es el patrón
  que D-F8.6-iiiA-a ya declaró (misma decisión que S62 con los 12 repos y S75 con el mapper). La
  evidencia del CDK zanja el riesgo que motivaba el test. Coste declarado, no tapado: T3/T4/T5 van a
  producción con CERO tests (ver D-F8.6-iiiB1-a).
  ASERTOS: los tres `it` de `pines.spec.ts` INVERTIDOS, no borrados (práctica (f)), más un `it` (4)
  para `id: null`. DOS mutaciones, y la SEGUNDA la añadió Claude Code para someter a prueba una
  afirmación mía: (A) valor constantizado (`b.id`→0) tumba (1), (2)-vía-`.get` y (4), y respeta (3);
  (B) ids barajados tumba (1) y (2)-`.get`. (B) DESMIENTE mi afirmación de que (1) era «el único que
  asevera que el id correcto va con la clave correcta»: no lo es. Lectura del solapamiento, que NO se
  fuerza a disjunción: (1), (2)-`.get` y (4) leen la MISMA dimensión (el valor del mapa) a tres
  granularidades —mapa entero, punto concreto, caso borde—; es un detector con tres lupas, y el
  solapamiento es ESTRUCTURAL. El único aserto de dimensión distinta es (3), la cardinalidad. Que
  (2)-`.has` sobreviva a (A) es el dato que valida el `.get` añadido: sin él, ese `it` no vería la
  dimensión nueva. (4) es ciego al emparejamiento POR CONSTRUCCIÓN (un solo elemento: invertir es la
  identidad). Lo que (1) aporta en exclusiva es la EXHAUSTIVIDAD (una entrada de más solo cae ahí);
  eso se afirma como RAZONAMIENTO, no como medición: ninguna de las dos mutaciones lo aísla.
  DESVIACIÓN DE ALCANCE APROBADA: T5 decía «eliminar `border-left-color`. Nada más», y Claude Code
  añadió además el reset de `<button>` (`padding`, `border`, `background`, `cursor`) en `.candado`.
  NO es mejora inventada: sin él, T3 deja un recuadro gris del agente de usuario en cada instancia
  pinada. El T5 del arquitecto estaba mal especificado; el reset va CON el commit que lo necesita.
  Suite frontend 22 → 23 (+1), backend intacto (app 315, solver 78). `ng build` limpio. Backend NO
  tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva).
  DEUDA NUEVA: D-F8.6-iiiB1-a (T3/T4/T5 sin ningún test; el doble GET corregido queda sin red),
  D-F8.6-iiiB1-b (la recarga es correcta POR ACCIDENTE), D-F8.6-iiiB1-c (`mensaje()` miente en el
  DELETE). CIERRA D-F8.6-ii-b.
  Siguiente: 8.6-iii-B2 (badge + resaltes; el borde ya está liberado y el mockup dibujado), 8.4-B
  (MOCKUP PREVIO; arrastra la contradicción de severidades de D-F8.4-A-c) u 8.5-D2b (solver, regenera
  la referencia), a decidir al abrir sesión.

### Sesión 84 — Fase 8, Bloque 8.6-iv-B: capa de test de componente (despinar bajo red).
  Modo híbrido. 1 commit de código (7c8f742, solo tests) + doc aparte. §A DE MEDICIÓN sobre el
  ENTORNO DE TEST (lectura literal de `angular.json`, `tsconfig.spec.json`, `package.json` y los 4
  specs; instrumento más barato, precedente S77-S83).
  BLOQUE SIN CASILLA, abierto en S84: 8.6-iv es el bloque que D-F8.6-iiiA-a y D-F8.6-iiiB1-a
  reclamaban las dos («la misma capa de test que nadie ha estrenado») y que no existía en «Bloques
  de Fase 8». PARTIDO en iv-A (specs de los tres servicios, capa HTTP) y iv-B (componente), y se
  eligió iv-B PRIMERO contra el orden por capas: la deuda nombra el aserto concreto por el que hay
  que empezar (el conteo de `listar()`), el riesgo vive en la coordinación y no en los wrappers de
  `HttpClient`, y testear `HorarioView` ANTES de 8.6-iii-B2 evita calibrar los tests contra una
  superficie ya crecida. Consecuencia asumida y registrada: iv-A se desplaza, D-F8.6-iiiA-a NO se
  cierra.
  ERROR DE ENRUTADO DEL PROPIO INSTRUMENTO: la §A se lanzó contra `frontend/`, que NO EXISTE —el
  frontend vive en `app/frontend/`— y los comandos fallaron enteros. Mismo género que S82/S83
  (suponer la ubicación de un directorio), esta vez cometido DENTRO de la medición que existe para
  no suponer.
  SALIDA DE §A, que DESMIENTE la premisa de apertura: TestBed NO se estrena. `app.spec.ts` ya usa
  `TestBed.configureTestingModule` + `createComponent` + `await fixture.whenStable()`, y el patrón
  zoneless está fijado y es copiable. Lo NO estrenado es (1) la capa HTTP de test
  (`HttpTestingController`: CERO usos en el repo) y (2) el test de un componente con `input`/`output`
  y colaboradores inyectados. El nombre que la deuda usaba —«el bloque de TestBed»— inducía a error.
  MEDIDO ADEMÁS: runner Vitest vía `@angular/build:unit-test` SIN bloque `options` (no hay
  `vitest.config.ts` que tocar); zoneless POR OMISIÓN (sin `zone.js`, sin `provideZonelessChangeDetection`);
  `vitest/globals`; `@angular/common/http/testing` disponible sin instalar. CERO dependencias nuevas
  —a diferencia de S81, donde el runner SÍ decidió una dependencia—.
  DECISIONES DE CONTRATO cerradas antes de teclear: (C1) dobles por `useValue` con `vi.fn()`, SIN
  `HttpTestingController` —iv-B prueba COORDINACIÓN, no transporte; con el doble HTTP el conteo de
  `listar()` pasaría a ser conteo de peticiones, que es otra dimensión, e invadiría iv-A—. (C4)
  fixtures LOCALES y mínimos: NO se usa `PROYECCION_1ESO` (25 sesiones harían que un aserto pase por
  ACUMULACIÓN y no por precisión, y su cabecera declara que no es espejo puro del backend) ni se
  extrae `pin()` de `pines.spec.ts` (no se toca un fichero commiteado y verde por dos specs nuevos);
  precedente S41/S81/S82/S83. (C5) `pinadas` se observa por el INPUT PÚBLICO de la hija
  (`By.directive(HorarioGrid)`), NO por cast `as any`: el input es la frontera real del contrato y
  `protected` declara privado lo otro. (C6) `Subject` PELADO, nunca `of()` ni `BehaviorSubject`.
  (C7) `provideRouter([])` FUERA: `HorarioView` solo usa `ActivatedRoute`; divergencia deliberada
  respecto al patrón de `app.spec.ts`, comentada en el TSDoc.
  DOS TURNOS DE CONTRASTE, no uno (patrón S82/S83). El PRIMERO se envió con el marcador
  «[pega aquí las dos tablas]» SIN PEGAR LAS TABLAS: Claude Code paró, dijo exactamente qué le
  faltaba y entregó solo la extracción literal. Séptima sesión seguida con un defecto de
  especificación del arquitecto, esta vez por contrato MUTILADO y no por afirmar el estado del código.
  TRES AFIRMACIONES MÍAS DESMENTIDAS POR EL CONTRASTE, todas por matriz cruzada y no por opinión:
  (1) «el Map NO cambia» en los asertos de guarda es COBERTURA FINGIDA tal como yo lo escribí —con
  el doble sin emitir, queda VERDE bajo AMBAS mutaciones—; se salva solo emitiendo en `sujetoBorrar`
  tras comprobar que no se llamó. Lo había añadido yo para corregir un aserto incompleto y era el
  mismo defecto que critico en D-F8.4-A-c. (2) el antiguo aserto 1 (una emisión ⇒ 1 llamada) NO
  aporta mutación propia: el 2 caza las dos. Mi argumento era de EDICIÓN DEL CÓDIGO y no de
  DISCRIMINACIÓN DEL ASERTO, que son cosas distintas; se fusionaron en un test de DOS FASES que
  conserva los dos puntos de medida (determinan a=0,b=1 unívocamente). (3) declaré el solapamiento
  4↔5 como ESTRUCTURAL y era PREMATURO: la matriz demuestra discriminación perfecta —`id===undefined`
  mata solo el de valor null, `id===null` mata solo el de clave ausente—. Retirado.
  HALLAZGO DE LA CAMPAÑA, que DEGRADA lo que dos asertos valen: la mutación `id === undefined` NO ES
  EXPRESABLE en TypeScript —`id` queda `number | null` y `borrar(id: number)` da TS2345—. El
  compilador es una barrera ANTERIOR al test. Se corrió como 3′ añadiendo el `as number` que un
  desarrollador escribiría para silenciar el error, que es la única vía por la que ese bug llega a
  producción. Consecuencia honesta: esos dos asertos protegen contra guarda-más-cast, NO contra un
  despiste de guarda a secas.
  DECISIONES DE RÉPLICA (juicio, no ejecución): se BORRAN los dos `not.toHaveBeenCalledWith` del
  aserto de la rejilla —implicados por `toHaveBeenCalledTimes(1)` + `toHaveBeenCalledWith`, no pueden
  ponerse rojos, y su contenido pasa al TSDoc como prosa—; se MANTIENE el literal `'Mat-1ºA|2'`
  frente a `clavePin(...)`, porque `pines.spec.ts` asevera con la propia función y ese literal es el
  ÚNICO punto del repo que fija el formato de la clave; se MANTIENE el índice 2 en la instancia
  pinada (con 1, una implementación que fijara `|1` a mano seguiría verde); se ACEPTA omitir el
  aserto «0 llamadas antes de emitir» —con `Subject` pelado ese estado no existe en producción y
  habría matado 1a y 1b en la misma línea, destruyendo su discriminación—.
  ENTREGADO: `horario-view.spec.ts` (5 tests) y `horario-grid.spec.ts` (2 tests). Suite frontend
  23 → 30 (+7), 4 → 6 ficheros. CAMPAÑA DE 8 MUTACIONES, todas por la vía esperada y cada una
  poniendo rojo EXACTAMENTE el test que declara: 1a/1b discriminan las dos fases por separado; 2
  cae en la mitad ANTES (el borrado optimista de D-F8.6-ii-5 daría el MISMO estado final); 3′ y 4
  se dejan verde la una a la otra. `ng build` limpio. CERO dependencias nuevas.
  Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva). Backend intacto (app 315,
  solver 78).
  DEUDA NUEVA: D-F8.6-ivB-a (`alSoltar` y tres ramas más del contenedor, sin cobertura),
  D-F8.6-ivB-b (el compilador tapa dos mutaciones), D-F8.6-ivB-c (dos observaciones del código
  destapadas al medir y que ninguna deuda recogía). CIERRA D-F8.6-iiiB1-a. D-F8.6-iiiA-a sigue
  VIVA ENTERA: iv-B no ha testeado ni un servicio.
  Siguiente: 8.6-iv-A (specs de los tres servicios, estrena `HttpTestingController`; cierra
  D-F8.6-iiiA-a), 8.6-iii-B2 (badge + resaltes; borde liberado y mockup dibujado), 8.4-B (MOCKUP
  PREVIO; arrastra la contradicción de severidades de D-F8.4-A-c) u 8.5-D2b (solver, regenera la
  referencia), a decidir al abrir sesión.

### Sesión 85 — Fase 8, Bloque 8.6-iv-A: specs de los tres servicios REST, estreno de HttpTestingController.
  Modo híbrido. 1 commit de código (b11617d, solo tests, 3 ficheros nuevos, 299 inserciones, cero
  borrados) + doc aparte. §A DE MEDICIÓN sobre el UNIVERSO A TESTEAR y su ENTORNO (lectura literal
  de los 3 servicios + `bloqueo.model.ts` + `app.config.ts`, `ls` de `services/`/`testing/`/`models/`
  y greps de `HttpTestingController`/`provideHttpClient`; instrumento más barato, precedente S77-S84).
  RUTA VERIFICADA ANTES DE MEDIR (corolario que S84 añadió tras fallar en esto): se midió contra
  `app/frontend/`, no contra `frontend/`; los comandos salieron a la primera.
  SALIDA DE §A, que REENCUADRA EL BLOQUE ENTERO: los cinco métodos de los tres servicios son
  WRAPPERS PELADOS —`return this.http.<verbo><T>(url[, body])` directo, sin `.pipe`, sin
  `catchError`, sin transformación, sin `options`—. `provideHttpClient()` va SIN `withFetch` (backend
  XHR, `HttpTestingController` intercepta sin ajuste). DESMIENTE MI SUPOSICIÓN DE APERTURA sobre
  `app/frontend/src/app/testing/`: NO es infraestructura de test —contiene un único fichero,
  `proyeccion-1eso.fixture.ts`, que es datos de dominio—, así que NO HABÍA HELPER QUE COPIAR.
  Y PRECISA la casilla del bloque: `HttpTestingController` no tenía CERO apariciones sino UNA, dentro
  de un comentario de `horario-view.spec.ts:15` que declara que NO se usa; cero usos EFECTIVOS, sí.
  DECISIÓN DE ALCANCE HONESTO, tomada con la medición delante y contra la lectura literal de la
  deuda: iv-A NO es cobertura de lógica y no se finge que lo sea. Lo que entrega es (a) CONGELACIÓN
  DEL CONTRATO DE ENDPOINTS (verbo + URL, las dos únicas dimensiones sin red del compilador) y (b)
  el ESTRENO de la capa `provideHttpClientTesting` + `HttpTestingController`, para que el primer
  servicio con lógica real no tenga que inventarse el patrón bajo presión. Se rechazó a propósito
  inflar el bloque con asertos sobre el tipo genérico, la propagación del 404 o «devuelve lo que
  llega»: un aserto implicado o no-reddable es prosa, y su sitio es el TSDoc (precedente S84).
  TURNO DE CONTRASTE, el más productivo de la fase: CINCO afirmaciones del arquitecto desmentidas,
  las cinco aceptadas y ninguna rechazada por reflejo.
  (1) `toBe` → `toEqual` en el aserto del cuerpo de `guardar`. Claude Code VERIFICÓ por lectura del
  fuente instalado (`_module-chunk.mjs:449`, `:562`; `serializeBody()` solo lo invoca el backend
  real, `http.mjs:183`) que `toBe` SÍ discrimina —el body no se clona ni se serializa en la ruta de
  testing—, y aun así objetó que NO DEBE: `{...peticion}` produce el mismo JSON en el cable, así que
  la mutación M3b no es un defecto sino una refactorización inocua, y un aserto que la pinta de rojo
  discrimina un ESTILO DE ESCRITURA, no un contrato. Remate que cierra el argumento: NINGUNA de las
  dos variantes caza el defecto real —que el servicio mute el objeto del llamante in situ—, porque
  entonces `PETICION` sería el objeto ya mutado y ambas pasarían. M3b ELIMINADA de la campaña.
  (2) LA RAZÓN ESCRITA DE UNA DECISIÓN ERA FALSA EN DOS DE SUS TRES ITEMS. Dejé fuera tres cosas
  «porque no pueden ponerse rojo». Cierto solo del tipo genérico (se borra en runtime). La
  propagación del 404 y «devuelve lo que llega» SÍ son reddables, con mutaciones que compilan
  (`catchError(() => of(null))` y `map(x => ({...x}))`). Importa porque `diagnostico.service.ts:23-28`
  dedica SEIS LÍNEAS de TSDoc a defender que el 404 se propaga sin traducir: dejar escrito que eso
  es intestable lo condenaba a no verificarse nunca. CORREGIDA la razón: quedan fuera POR ALCANCE.
  (3) CAMPAÑA INCOMPLETA EN UNA DIMENSIÓN VIVA. D4 exige aseverar verbo + URL en los cinco tests,
  pero la campaña solo movía el verbo en T3: en T1/T2/T4/T5 el componente `method` del matcher no se
  ponía a prueba y podría haberse borrado sin que nada cayera. NO es un aserto muerto (es reddable)
  sino una MUTACIÓN QUE FALTA —distinción que importa y que Claude Code hizo explícita—. Añadida M4b
  (`delete<void>` → `get<void>` en `borrar`), la más barata que demuestra la dimensión: el DELETE es
  el único verbo no repetido. Medida: cae (11) con URL recibida `/api/bloqueos/7` IDÉNTICA a la
  esperada, difiriendo solo el verbo ⇒ la mitad `method` está VIVA y no es adorno.
  (4) UNA JUSTIFICACIÓN QUE DESCRIBÍA UN ESCENARIO IMPOSIBLE. Escribí que `id = 7` en las tres rutas
  interpoladas evitaba que un cruce de rutas entre servicios cayera por el número en vez de por la
  ruta. Ese cruce NO PUEDE OCURRIR: tres ficheros, tres `TestBed`, tres `HttpTestingController`
  independientes; la petición de un spec no es visible desde otro. La DECISIÓN no cambia (7 y no 1,
  porque con 1 una implementación que incrustara el id a mano seguiría verde, mismo criterio que el
  «índice 2 y no 1» de `horario-grid.spec.ts:52-56`); la RAZÓN se reescribió.
  (5) «DOBLE FALLO» ERA CASCADA, y lo destapó la campaña, no el razonamiento. Hice escribir en el
  TSDoc que `verify()` produciría un doble fallo por una sola causa. FALSO en un fichero de varios
  tests: `verify()` lanza dentro del `afterEach`, eso impide el reset del TestBed, y el `beforeEach`
  del test SIGUIENTE revienta con «Cannot configure the test module when the test module has already
  been instantiated». MEDIDO: M2, que solo toca la URL de `listar()`, tumbó (9), (10) y (11).
  Importa porque la campaña se apoya en «qué test cae» como señal de ATRIBUCIÓN, y con cascada
  «cayeron tres» ya no atribuye. Se salva por el MENSAJE, y esa regla de lectura quedó escrita:
  víctima REAL = falla por `expectOne`; COLATERAL = falla por «Cannot configure the test module».
  DECISIÓN sobre la cascada, entre tres salidas que Claude Code presentó sin elegir por su cuenta:
  se corrige el TSDoc, NO el código. Descartado blindar el `afterEach` con
  `try/finally + resetTestingModule()` (mete maquinaria permanente en tres ficheros y estrena un
  patrón con cero apariciones en el repo, para un síntoma que solo existe bajo mutación) y descartado
  partir `bloqueo.service.spec.ts` en varios `describe` (rompería «un describe por fichero», que es
  el patrón vivo de los dos specs de referencia; sería dejar que el instrumento de medición dicte la
  estructura del test). En VERDE `verify()` no lanza nunca: la patología solo aparece cuando ya
  estás leyendo los mensajes con lupa. `verify()` se declara RED, no aserto: no es independientemente
  reddable bajo la campaña, pero caza la petición no contemplada (p. ej. un método que dispare dos).
  DESVIACIÓN DE CLAUDE CODE, ACEPTADA: no usó `compileComponents()` ni `beforeEach` async pese a
  figurar en el «patrón a copiar». Razón suya, correcta: en los specs de referencia existe para
  compilar COMPONENTES y aquí no hay ninguno —`TestBed.inject` sobre un servicio es síncrono—;
  añadirlo sería copiar la forma sin la causa. Lo que había que copiar era el TSDoc, la forma de
  `describe`/`it`, la numeración correlativa y el estilo de aserto, no la mecánica de arranque.
  ENTREGADO: 3 specs junto a su fuente en `services/`, 5 tests numerados (8)..(12) continuando la
  numeración correlativa del repo. Suite frontend 30 → 35 (+5), 6 → 9 ficheros. Fixture LOCAL al
  spec, calibrado (`indice: 2` y no 1; `dia` ≠ `orden` para que un swap de campos fuera detectable;
  `aulas` NO vacío, porque el vacío es el caso trivial y D-5 lo documenta como significativo);
  `PROYECCION_1ESO` NO contaminado (patrón S41/S81/S82/S83/S84).
  CAMPAÑA DE 6 MUTACIONES, las seis caen por `expectOne` y NINGUNA deja de compilar. M4 en
  particular compila —deja `id` sin usar— lo que VERIFICA sobre el árbol real que
  `noUnusedParameters` no está activo ni se hereda de `strict`; si algún día se activa, M4 deja de
  ser expresable y hay que sustituirla por `/api/bloqueos/${id}` → `/api/bloqueo/${id}`. Dato de
  método REGISTRADO POR CLAUDE CODE SIN QUE SE LE PIDIERA: la campaña se corrió sobre el estado
  ANTERIOR a la corrección del TSDoc; sobre el commit final solo se redemostró UNA de las seis
  (M4b, verde-rojo-verde). Como los cambios fueron solo comentarios ninguna mutación cambia de
  resultado, y se acepta así en vez de dar las seis por buenas sobre el estado nuevo.
  HALLAZGO DEL MATCHER, no previsto: `expectOne` compara contra `urlWithParams` por igualdad
  ESTRICTA de string, y sus guardas son `!match.method` / `!match.url`, así que un campo omitido NO
  restringe —que es lo que da contenido real a la regla «nunca matchear solo por URL»—. Hoy es
  indiferente (ningún método pasa `params`), pero si alguien añade `params` a `listar()`, T2 se
  pondrá rojo y ESE ROJO SERÁ CORRECTO. Escrito en el TSDoc para que no se lea como falso positivo.
  Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva). Backend intacto (app 315,
  solver 78). `ng build` limpio. CERO dependencias nuevas: `@angular/common/http/testing` resuelve
  por exports map, VERIFICADO por `require.resolve` sobre la 21.2.17 instalada, no supuesto.
  DEUDA NUEVA: D-F8.6-ivA-a (el 404 que el TSDoc de `DiagnosticoService` defiende no lo verifica
  nadie, y SÍ es reddable), D-F8.6-ivA-b (la cascada de `verify()` rompe la atribución de una
  campaña de mutación), D-F8.6-ivA-c (`TramoRef.orden` vs `SesionVista.tramo`, NO MEDIDO).
  CIERRA D-F8.6-iiiA-a con MATIZ MEDIDO.
  Siguiente: 8.6-iii-B2 (badge + resaltes; borde liberado y mockup dibujado desde S83; cierra
  D19/D20 en frontend), 8.4-B (MOCKUP PREVIO; arrastra la contradicción de severidades de
  D-F8.4-A-c) u 8.5-D2b (solver, INVIERTE `CatalogoMapperActividadTest:136` y regenera la
  referencia), a decidir al abrir sesión.

### Sesión 86 — MÉTODO: se escribe el procedimiento que se invocaba y no existía (CIERRA D-F8.0-a).
  Modo híbrido (greps en Claude Code, redacción y decisiones en el Project). Sin código: solo `docs/`.
  §A DE MEDICIÓN sobre los DOS DOCUMENTOS (13 greps + lectura literal; instrumento más barato,
  precedente S77-S85), corrida ANTES de redactar una línea de norma, porque el riesgo específico de
  esta sesión era canonizar de memoria una costumbre de dos sesiones.
  SALIDA DE §A, en tres partes. (1) PASOS REALES DEL ARCHIVADO: ocho, de los cuales solo el 6 (R4/R5)
  estaba escrito; los otros siete son precedente observable S69-S85. (2) MÉTODO ATRAPADO EN CABECERAS:
  cuatro candidatos vigentes sin sede —§A de medición, campaña de mutación, contraste con Claude Code
  y regla de artefactos derivados— más uno que YA tiene sede y NO se duplica (MOCKUP PREVIO, D-F8.6-a:
  se remite, no se reescribe). (3) DOS FALLOS HISTÓRICOS REALES del archivado, no hipotéticos: la copia
  truncada y duplicada de S55 (S59, corregida en S60) y el censo desalineado (S68, corregido en S69);
  los dos son de transcripción o censo, no de criterio. Eso es lo que justifica que M1-bis lleve
  verificación propia en vez de ser un bullet más.
  ENTREGADO: sección «Método de trabajo (procedimiento vigente)» (82 líneas) insertada TRAS el criterio
  R4/R5 y no en sección aparte —R4/R5 ya es método y vive bajo «Decisiones permanentes»; separarlas
  obligaría a duplicar la referencia—. M1 (ocho pasos del cierre), M1-bis (el archivado, con PROMOVER /
  INSERTAR / COMPROBAR), M1-ter (el prompt de la sesión siguiente: no fija alcance, no copia el plan,
  se poda si pasa de ~60 líneas), M2 (§A de medición), M3 (campaña de mutación y lo que un aserto vale),
  M4 (contraste antes de teclear + artefactos derivados).
  DOS COSAS MARCADAS COMO DECISIÓN DEL ARQUITECTO, no como norma medida, porque la evidencia no las
  sostiene: que PROMOVER sea obligatorio (S82 lo registró como ANOMALÍA observada, no como regla
  querida) y el umbral de ~60 líneas del prompt (cifra sin precedente). Marcarlas es el punto: una
  sección de método que no distingue lo medido de lo decidido miente sobre su propia autoridad.
  DESCARTADO POR R5, pese a estar en las cabeceras que se archivarán: recuentos de suite por bloque,
  las mutaciones concretas de cada sesión y los hallazgos de framework de S73-S76. Son historia con
  moraleja, no mecanismo vigente; una sección de método que narre sesiones es lo que R5 prohíbe.
  PRIMERA PRUEBA REAL DEL PROCEDIMIENTO: el archivado de S82 se ejecutó SIGUIENDO M1-bis recién
  escrito, no el precedente. Resultado: los tres sub-pasos se aplicaron sin fricción y el paso de
  COMPROBAR (diff del cuerpo archivado contra el que salió del plan) resultó NO decorativo —es el
  control que habría cazado el fallo de S59—. El procedimiento no necesitó corrección al aplicarlo.
  CONSECUENCIA ASUMIDA Y REGISTRADA: S86 no toca código y desplaza por sexta vez 8.6-iii-B2, y
  también 8.4-B y 8.5-D2b. Se acepta porque D-F8.0-a llevaba seis sesiones viva y el coste del prompt
  de apertura sin sede era recurrente, no puntual.
  Código NO tocado: `solver/`, `app/` ni `frontend/` → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado, suite sin cambio (app 315, solver 78, frontend 35).
  DEUDA NUEVA: ninguna. CIERRA D-F8.0-a.
  Siguiente: 8.6-iii-B2 (badge + resaltes; borde liberado y mockup dibujado desde S83; cierra D19/D20
  en frontend), 8.4-B (MOCKUP PREVIO; arrastra la contradicción de severidades de D-F8.4-A-c) u
  8.5-D2b (solver, INVIERTE `CatalogoMapperActividadTest:136` y regenera la referencia), a decidir al
  abrir sesión.

### Sesión 87 — Fase 8, Bloque 8.6-iii-B2-a: cableado del diagnóstico y badge del delta blando.
  Modo híbrido. 3 commits de código (2047349 capa pura, 71c4fb6 rejilla, 709b58a contenedor) + doc
  aparte. §A DE MEDICIÓN sobre el ESTADO REAL DEL FRONTEND (lectura literal de los ficheros de
  `app/frontend/` implicados + `ls`/`grep`; instrumento más barato, precedente S77-S86).
  PRIMERA APLICACIÓN DE M1-M4 COMO NORMA ESCRITA (S86 los redactó sin probarlos en una sesión de
  código).
  SALIDA DE §A, que REENCUADRA EL BLOQUE: la capa de diagnóstico llevaba desde S82 construida y
  probada pero DESCONECTADA —las únicas referencias a `DiagnosticoService`, `indiceViolaciones` e
  `indicePenalizaciones` eran sus propios specs—. B2 NO era pintura: incluía el cableado. PARTIDO en
  B2-a (cable + badge) y B2-b (los dos resaltes).
  LA MEDICIÓN DESMIENTE AL PLAN, no solo una suposición de apertura: la cabecera de S83 afirma que el
  `border-left` quedó «liberado en `.instancia.pinada .entrada` para que B2 pinte ahí la violación».
  FALSO como se lee: lo liberado fue el `border-left-color` BAJO el estado pinada; el `border-left`
  de `.entrada` está OCUPADO (3px `#4a7`) y es estructural en TODA entrada. El arquitecto repitió la
  afirmación del plan como si fuera medida. Es un caso que M2 no contempla (ver corrección al método,
  abajo).
  RUTAS DEL PROPIO GUION DE MEDICIÓN, MAL: los componentes viven en `src/app/components/horario-{view,grid}/`,
  no en `src/app/horario/`, que solo tiene lógica pura; y el runner es `ng test`, no `npx vitest run`
  a pelo (sin la config de Angular no hay globals). El corolario de S84 (verificar la ruta ANTES de
  medir) lo cazó en el comando 0 en vez de tumbar la medición entera.
  MOCKUP (D-F8.6-a) sobre el CSS MEDIDO y no el supuesto, que es la razón de rehacerlo: el resalte de
  violación NO va al `border-left`. [CORREGIDO EN S88: la mitad «`background`» de este mockup se
  escribió SIN medir el `background`, que está ocupado en dos capas —`.entrada` y
  `.instancia.pinada .entrada`, esta última la señal de pinada—. El resalte va SOLO a `outline`. Es
  el mismo defecto que esta sesión denunció de S83, una capa más abajo.] `outline` no ocupa layout, así que las
  dos granularidades se leen solas (sobre `.instancia` = profesor/subgrupo, sobre `.entrada` = aula):
  la asimetría D15 se pinta sin aplanarse. Desalojar el verde estructural haría que la ausencia de
  violación fuese ausencia de borde y desmontaría la celda de seis entradas.
  DECISIÓN DE PRODUCTO sobre el número del badge, entre tres: (a) SUMA CON SIGNO de los deltas de la
  instancia. Descartadas (b) cardinalidad —pierde el signo, que es el hallazgo caro de S65: `delta<0`
  significa que mover EMPEORA— y (c) máximo absoluto. El signo agregado es lo que responde a «¿qué
  gano si muevo esto?», que es el propósito con el que S65 definió el contrafactual (delta =
  penalización_actual − penalización_si_esa_celda_no_estuviera; detalle: bitácora S65).
  DECISIÓN sobre el edge suma-0, que el contraste destapó como agujero: las claves de suma 0 NO SE
  EMITEN. Semántica de S65 (delta 0 = indiferente y el backend tampoco lo emite); un badge «0»
  prometería información que no hay. Predicado del hueco = `has(clave)`, sin comparación con 0.
  EL CONTRASTE PREVIO (M4) CAZÓ CUATRO ERRORES DE ESPECIFICACIÓN DEL ARQUITECTO, el registro más alto
  del proyecto junto con las cinco de S85. (1) T1 PASABA SU PROPIA DEGENERACIÓN: «contador = 1 tras
  dos ciclos de detección de cambios» da por bueno `getDiagnostico` en el constructor (a=1, b=0), que
  es justo lo que existe para cazar; además usaba vocabulario que el arnés zoneless no tiene.
  Reescrito al modelo lineal por EMISIÓN DE RUTA (una emisión ⇒ 1, dos ⇒ 2), que fija a=0, b=1. Es el
  corolario de S66 incumplido con la regla delante: se enunció el PROPÓSITO y salió el camino feliz.
  (2) LA AGREGACIÓN ESTABA EN EL SITIO EQUIVOCADO, contra el TSDoc del propio `horario-view.ts` («este
  componente solo orquesta señales y delega»); el precedente que el arquitecto invocó (`pinadas`) dice
  lo CONTRARIO, porque se construye con `indicePines`, que es función pura. (3) «Fila flex CON el
  candado» era IMPOSIBLE: un `position:absolute` no participa del flex de su padre; hace falta wrapper.
  (4) El hueco condicional metía DE CONTRABANDO un cambio visual a las celdas pin-only cerradas en B1.
  RECHAZADA la mitad «candado» de esa cuarta objeción: la salida correcta no es declarar el cambio
  sino NO hacerlo —el predicado es `tieneBadge`, no `badge || candado`; el candado lleva desde B1
  solapando sin reservar y no es un problema reportado—.
  ENTREGADO: `sumaDeltasPorInstancia` en la capa PURA (`horario/diagnostico.ts`), hermana de los dos
  índices, en DOS PASADAS porque el signo puede cancelarse a mitad; wrapper `.adornos` (absolute, flex
  por dentro) con badge + candado, `[class.con-badge]` reservando 16px solo por badge; `getDiagnostico`
  dentro de `cargar(id)` y NO por analogía con `cargarPines()`, que es GLOBAL (D-F8.6-iiiB1-b) —la
  asimetría va escrita en TSDoc—; `errorDiagnostico` con selector propio `.error-diagnostico` que NO
  gatea la rejilla (si el diagnóstico falla, el horario sigue pintado), y sin selector propio la pata
  «error vacío» de T4 sería ilegible por DOM sin el cast que el TSDoc del spec prohíbe. TSDoc
  obligatorio de que la suma NO es `Totales` y no debe «arreglarse» (javadoc de `TotalesDTO`).
  ASERTOS REDISTRIBUIDOS POR FRONTERA, consecuencia de mover la agregación: puros T2/T3/T5 en
  `diagnostico.spec.ts` (sin componente, sin la dimensión sesión que `PROYECCION_VACIA` evita a
  propósito), wiring T1/T4 en `horario-view.spec.ts`, render T6 en `horario-grid.spec.ts`. T2
  calibrado para que −2 no lo dé cardinalidad (2), ni máximo absoluto (−5), ni suma de absolutos (8),
  ni primero (3), ni último (−5), ni `abs(suma)` (2).
  DESVIACIÓN DE CLAUDE CODE, ACEPTADA Y CORRECTA: intercambió el orden de commits 2↔3. El contenedor
  liga el input de la rejilla, así que la rejilla debe aterrizar antes o el commit intermedio no
  compila. El orden del arquitecto estaba mal.
  Suite frontend 35 → 41 (+6). CAMPAÑA DE 6 MUTACIONES, todas caen por el aserto previsto y ninguna
  necesitó cast: `clavePin(actividad,1)` deja T2 VERDE y cae solo T3 (dimensión índice aislada), y la
  degeneración de constructor cae solo en T1. `horario-view.spec.ts` (verde, commiteado) EDITADO para
  el doble de `DiagnosticoService`: dependencia nueva, no conveniencia; código y spec en el mismo
  commit. Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO
  regenerada, `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva). Backend intacto
  (app 315, solver 78).
  CORRECCIÓN AL MÉTODO, pendiente de decidir: M2 dice que la medición desmiente «una suposición de
  apertura del arquitecto» y no contempla que desmienta el PLAN (aquí, la cabecera archivada de S83).
  R5 dice que el mecanismo vivo es estado vivo, luego una cabecera que describe mal el CSS actual es
  estado vivo equivocado y habría que corregirla. NO se ha escrito como norma: escribir norma sin
  decidirla con el usuario es lo que S86 marcó como «decisión del arquitecto». Se traslada a S88.
  DEUDA NUEVA: D-F8.6-iiiB2a-a (tercer canal de error en el mismo componente, sin política global).
  Siguiente: 8.6-iii-B2-b (los dos resaltes; es lo ÚNICO que queda del frente 8.6-iii, con el dónde
  ya decidido en mockup), 8.5-D2b (solver, INVIERTE `CatalogoMapperActividadTest:136` y regenera la
  referencia) u 8.4-B (MOCKUP PREVIO; D-F8.4-A-c es trabajo de BACKEND y abrirlo puede ser abrir dos
  bloques), a decidir al abrir sesión.

### Sesión 88 — Fase 8, Bloque 8.6-iii-B2-b: los dos resaltes de violación (CIERRA el frente 8.6-iii).
  Modo híbrido. 2 commits de código (2c48f9b rejilla, 15c8f31 contenedor) + doc aparte.
  §A DE MEDICIÓN sobre el CSS y el estado del cable (lectura literal de los tres ficheros de
  `horario-grid/` + greps de `indiceViolaciones` y del contenedor + suite de base).
  §A CONFIRMA AL PLAN EN LO CENTRAL: `indiceViolaciones` seguía SIN CABLEAR (8 apariciones, 7 en su
  spec y 1 en su definición; cero en componentes). Primera vez en el frente que la afirmación del
  plan resiste la medición.
  PERO §A DESMIENTE AL PLAN EN EL MOCKUP, y es el caso que estrena la corrección de método: el
  mockup de S87 decía «`background` + `outline`». La mitad `background` se escribió SIN MEDIRLA:
  `.entrada` la tiene en `#fafafa` y `.instancia.pinada .entrada` la sobrescribe con `#fff8ec`, que
  ES la señal de pinada. Un resalte por fondo o la pisa o queda pisado según el orden de las reglas.
  DECISIÓN: el resalte va SOLO a `outline`, única propiedad libre en ambos anclajes (`border-left`
  ocupado 3px `#4a7` estructural, `background` ocupado en dos capas). El `outline` del CDK
  (`.cdk-drop-list-dragging`) vive en el `<td>`: otro elemento, sin colisión de cascada.
  CORREGIDA LA AFIRMACIÓN VIVA en sus DOS sedes (casilla del bloque y cabecera de S87), no solo
  anotada. La copia archivada de S83 en la bitácora NO se toca: es histórico de solo lectura y
  borrarla eliminaría la evidencia de que el error existió.
  PATRÓN QUE ESTO DESTAPA, y que motiva la segunda mitad de la norma nueva: la afirmación falsa la
  escribió la sesión ANTERIOR aplicando el método, para corregir a una sesión previa. S87 midió el
  `border-left` con rigor, acertó, y en la MISMA FRASE escribió `background` sin medirlo. El rigor
  fue parcial y la frase no distinguía la mitad medida de la supuesta.
  HUECO DE MI PROPIA §A, declarado: el guion midió el componente y la capa pura pero NO
  `horario.model.ts` ni `proyeccion.ts`, que son justo los dos ficheros que responden a la pregunta
  más cara del bloque (¿es pintable la granularidad de aula?). Que la respuesta saliera favorable no
  lo convierte en acierto.
  EL CONTRASTE PREVIO (M4) CAZÓ CUATRO ERRORES DE ESPECIFICACIÓN DEL ARQUITECTO, todos aceptados.
  (1) PREGUNTA 0 EN VERDE: `SesionVista.plazaCodigo` existe y NO es nullable
  (`horario.model.ts:24`), `agruparPorActividad` lo conserva sin transformar (`proyeccion.ts:97-99`)
  y la plantilla ya itera con `e` en mano ⇒ el matching plaza↔sub-entrada es directo, sin heurística
  de `aulaCodigo` (dos plazas pueden compartir aula) y SIN TOCAR backend ni el DTO de Fase 7. El
  bloque NO se parte. (2) T1 ERA REDUNDANTE Y CAÍA POR ACOPLAMIENTO: la mutación que declaré
  —indexar por violación en vez de por celda— es de la CAPA PURA y ya la mata `diagnostico.spec (2)`;
  reencuadrado a WIRING (el `computed` no liga → 0 claves), gemelo del T1 de badges que S87 reescribió
  por lo mismo. (3) T3 SOBRE-DECLARABA: su fixture tiene plaza no-null, luego no puede ejercer la
  dirección instancia→aula (esa es de T2); posee {aula→instancia, marcar-todas, plaza-equivocada}.
  (4) LA PATA DEL `border-left` DE T4 ERA VACUA EN SU PROPIO FIXTURE —con la clave ausente no hay
  clase aplicada, luego el borde queda intacto pase lo que pase— y además exigiría `getComputedStyle`
  sobre hoja de estilos bajo encapsulación `_ngcontent`, sin precedente en la suite. ELIMINADA; la
  ortogonalidad `outline`/`border` se garantiza POR CONSTRUCCIÓN y su sitio es el TSDoc, no un aserto.
  (5) DIMENSIÓN VIVA SIN CUBRIR, añadida como T5: el DESDOBLE. `diagnostico.spec (9)` ya fija que un
  `SOLAPE_AULA` puede tener dos celdas = misma instancia, dos plazas distintas; en la rejilla son dos
  sub-entradas y AMBAS deben marcarse. T3 solo probaba «casa una de dos»; nadie probaba «casan las
  dos, cada una por separado». No es hipotético: el desdoble es el caso estructural del dominio.
  ENTREGADO: input ÚNICO `violaciones` (se RECHAZA pasar dos mapas pre-separados: aplanaría la
  asimetría D15 justo donde el modelo declara que no se normaliza, y además no ahorraría trabajo
  porque el matching plaza→sub-entrada SOLO es expresable en la rejilla, la única capa que enumera
  sub-entradas con sus plazas); dos predicados con `.some()` en ambos, para que cada sub-entrada se
  evalúe por separado (T5); CSS estrictamente ADITIVO, dos reglas nuevas y ninguna existente tocada.
  `horario/diagnostico.ts` NO TOCADO: la capa pura estaba completa desde S82 y lo que faltaba era
  CONSUMIDOR.
  CAMPAÑA DE 6 MUTACIONES, no 5: la sexta la destapó Claude Code al notar que NINGUNA de las cinco
  propuestas mataba a T2, que quedaba indistinguible de peso muerto. M6 (`tieneViolacionAula` casa
  también con `plazaCodigo === null`) cae SOLO en T2 ⇒ T2 es independientemente reddable. DOS
  ATRIBUCIONES MÍAS CORREGIDAS POR LA MEDIDA: M2 cae en T3 y NO en T2 (el fixture de T2 es ciego a
  M2: una violación null marca la instancia bajo código correcto Y mutado), luego T3 es load-bearing
  para esa dimensión y borrarlo dejaría M2 viva; y M5 cae en T4 y ADEMÁS en T3, que asevera
  legítimamente «instancia no marcada» —compartido, no acoplamiento—. Ninguna mutación necesitó cast.
  Suite frontend 41 → 46 (+5), 9 ficheros. `ng build` limpio. CERO dependencias nuevas.
  Backend NO tocado (`app/src/main` ni `solver/`) → `referencia-codigo-solver.md` NO regenerada,
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva). Backend intacto (app 315,
  solver 78).
  CIERRA D19/D20 EN FRONTEND y CIERRA EL FRENTE 8.6-iii ENTERO (cero sub-bloques vivos, sexto
  sub-bloque desde S82). D-F8.6-iiiA-b SIGUE VIVA, con destino más claro: `Totales` se dejó FUERA a
  propósito —es dato del horario entero, no cruza por `clavePin`, no tiene sitio en la rejilla y su
  trampa documentada exige decidir dónde vive; es cabecera o panel, no resalte de celda—.
  DEUDA NUEVA: ninguna.
  Siguiente: 8.5-D2b (solver, INVIERTE `CatalogoMapperActividadTest:136` y regenera la referencia),
  8.4-B (MOCKUP PREVIO; D-F8.4-A-c es trabajo de BACKEND y abrirlo puede ser abrir dos bloques),
  8.6-B (aviso durante el arrastre) o la cobertura de D-F8.6-ivB-a, a decidir al abrir sesión.

### Sesión 89 — Fase 8, D-F8.6-ivB-a: cobertura del camino de PINADO en el contenedor.
  Modo híbrido. 1 commit de código (solo tests, un único fichero modificado) + doc aparte.
  §A DE MEDICIÓN con TRES FALLOS DE INSTRUMENTO ENCADENADOS, todos del arquitecto y todos del mismo
  género —dar por sabido el terreno en vez de leerlo—: (1) el guion apuntó a `frontend/` cuando el
  frontend vive en `app/frontend/` (nueve comandos fallidos); (2) la tanda siguiente falló ENTERA
  sobre rutas ya verificadas porque el cwd de la sesión de Claude Code era
  `.../app/frontend` y las rutas eran relativas a la raíz; (3) la suite se invocó con
  `npx vitest run`, que salta la configuración del builder de Angular 21 y da
  `describe is not defined` en 9/9 ficheros —el script real es `ng test`
  (`angular.json`: `"test": {"builder": "@angular/build:unit-test"}`, sin `setupFiles` ni `include`)—.
  Ninguno tocó el contrato, pero costaron tres tandas. CORRECTIVO ADOPTADO: rutas ABSOLUTAS en todo
  guion de medición, y leer la invocación de la suite en `package.json` antes de escribirla.
  §A CONFIRMA AL PLAN EN LO CENTRAL: `alSoltar` no tenía NI UN ASERTO. El spec tenía 11 `it`, pero
  (13)/(14) son de diagnóstico y (20) de cableado de `violaciones`; `guardar` aparecía en
  `horario-view.ts:161` y en `bloqueo.service.spec.ts:128`, en NINGÚN punto del spec del contenedor.
  Segunda vez consecutiva que la afirmación del plan resiste la medición.
  PERO §A DESMIENTE LA ENUMERACIÓN DE LA DEUDA en dos puntos. (a) La deuda cuenta como rama propia
  la `error:` de `alDespinar`, pero su `errorPin.set(this.mensaje(err))` (l.194) invoca EL MISMO
  `mensaje()` que la de `alSoltar` (l.170): cubrir una cubre la función, y duplicarla sería cobertura
  fingida. Eran dos ramas y son una y media. (b) La deuda enumeró ramas SUPONIENDO EL ANDAMIO LISTO,
  y no lo estaba: `guardar: vi.fn()` devolvía `undefined` y `alSoltar` hace
  `.guardar(...).subscribe(...)`, así que el primer test que lo disparase habría reventado.
  TURNO DE CONTRASTE (M4), el que más valor añadió del bloque: CINCO correcciones, CUATRO del
  arquitecto, y una de ellas de FIXTURE y no de redacción.
  (1) DOS MUTACIONES DE MI CAMPAÑA ERAN INEXPRESABLES EN ESTE ÁRBOL. `aulas: []` → omitir el campo NO
  compila (`BloqueoRequest.aulas` es obligatorio, TS2741); y mover el poblado del `next` «tal cual»
  tampoco, porque usa `b`, que solo existe dentro del `next` ("Cannot find name 'b'"). La forma
  compilable del alta optimista usa `s` y valor `null`, y es contra ESA contra la que hay que medir.
  Escribir una mutación que nadie podría teclear es exactamente el vicio que M3 rechaza.
  (2) EL ANDAMIO QUE PROPUSE ERA INCOMPATIBLE CON EL ESCENARIO QUE DEBÍA MEDIR. Un `Subject` que ya
  hizo `.error()` queda CERRADO, y re-suscribirse redispara el error de forma SÍNCRONA: con un
  sujeto compartido, el segundo `soltar` de (25) repoblaría `errorPin` nada más suscribirse y la fase
  «`errorPin` a null antes de responder» sería INOBSERVABLE —el test habría pasado o fallado por la
  razón equivocada—. `guardar` devuelve un Subject FRESCO POR INVOCACIÓN, con el porqué documentado
  en el propio doble porque los otros tres dobles del fichero NO lo son.
  (3) UNA RAMA SIN RED QUE MIS CINCO TESTS NO TOCABAN, aportada por el contraste: `new Map()` en vez
  de `new Map(this.pinadas())` en el `next` borraría TODOS los pines previos al añadir uno nuevo.
  Compila, es un desliz clásico y ninguno de los 46 lo detectaba: (22) y (23) arrancan con `pinadas`
  VACÍO, donde «mapa copiado» y «mapa desde cero» dan idéntico resultado, y los únicos que parten de
  mapa no vacío —(2),(3),(4)— son de DESPINAR. Nació (26), único test del camino de alta que arranca
  con índice poblado.
  ENTREGADO: SEIS tests (21)-(26) en `horario-view.spec.ts`, un solo fichero tocado, cero ficheros
  nuevos, `horario-view.ts` INTACTO. (21) el cuerpo del POST se arma desde la suelta con `aulas: []`
  y el tramo sin permutar —esperado LITERAL, nunca compuesto desde `s`, y `dia=3`≠`orden=4` porque
  con `dia===orden` la permutación sería invisible—; (22) la clave sale de la RESPUESTA y no de la
  suelta, con FIXTURE DEFENSIVO DECLARADO (suelta `Mat-1ºA|2` vs respuesta `LCL-1ºA|1`, divergencia
  imposible en producción y deliberada: sin ella `s` y `b` coinciden y la mutación queda verde;
  precedente del it (9) de `diagnostico.spec`, S82); (23) sin alta optimista, con las DOS fases;
  (24) el error no pina y `mensaje()` degrada a `El servidor rechazó el pin (400).`, leído por el
  `<p class="error">` que aquí es inequívocamente `errorPin` (mismo razonamiento que el (5));
  (25) el alta OK NO recarga la proyección y un nuevo intento limpia el error previo ANTES de
  responder; (26) el alta PRESERVA los pines previos.
  CAMPAÑA DE 7 MUTACIONES (M25 desdoblada en M25/M25b), todas compilables, SIETE VÍCTIMAS REALES
  DISTINTAS, ninguna superviviente ni huérfana. M23 arrastra tres colaterales porque su forma
  compilable vacía el `next` y toca a la vez clave, estado-tras-error y preservación; la víctima real
  sigue cayendo por su propio aserto de «antes» (mismo patrón cascada/colateral que
  `bloqueo.service.spec` documenta desde S85). M24 → (25) es colateral fina y esperable: al vaciar el
  mensaje, `errorPin('')` deja de renderizar el párrafo.
  UN REPARO DEL ARQUITECTO RETIRADO TRAS VER EL DIFF, y se registra por simetría con los aceptados:
  objeté que (25b) podría pasar por la razón equivocada si `errorPin` se pusiera a `''` en vez de a
  `null`. Es cierto en abstracto e IRRELEVANTE aquí —`@if (errorPin(); as msg)` trata ambos como
  falsy, así que la diferencia no tiene consecuencia observable en ninguna parte de la aplicación—.
  No todo reparo del contraste sobrevive al fichero.
  Suite frontend 46 → 52 (+6); backend intacto (app 315, solver 78). `horario-view.ts` NO tocado →
  `referencia-codigo-solver.md` NO regenerada, `modelo_datos_fase1.md` NO tocado.
  DEUDA NUEVA: D-F8.6-ivB-a-bis (precedencia interna de `mensaje()`).
  D-F8.6-ivB-a NO SE CIERRA: se REDUCE a su alcance superviviente (ver su entrada). Cerrarla
  obligaría a abrir una hermana con el resto y se perdería la traza; precedente D-F8.6-iiiA-a en S84,
  que siguió viva con el encuadre corregido por la medición.
  Siguiente: 8.5-D2b (solver, ÚNICO candidato de backend, desplazado desde S77; INVIERTE
  `CatalogoMapperActividadTest:136` y regenera la referencia; recomendable PARTIRLO), 8.6-B (aviso
  durante el arrastre; su pregunta es sobre un estado HIPOTÉTICO que ningún índice actual responde,
  luego su contrato hay que decidirlo ANTES de medir) u 8.4-B (MOCKUP PREVIO; D-F8.4-A-c es trabajo
  de BACKEND y abrirlo puede ser abrir dos bloques), a decidir al abrir sesión.

### Sesión 90 — Fase 8, Bloque 8.5-D2b-1: transporte de la tutoría al solver (CIERRA D-B5-5).
  Modo híbrido. 3 commits (333dfb8 solver, d0f5e9b app, a363e72 doc). PRIMER BLOQUE DE BACKEND
  desde S79: 8.5-D2b llevaba DOCE sesiones desplazado (desde S77), perdiendo cada vez contra
  candidatos de frontend más baratos. Se abre PARTIDO en D2b-1 (transporte) y D2b-2 (verificación
  de S8), y el corte NO es el que S89 recomendó: ver abajo.
  §A DE MEDICIÓN sobre `referencia-codigo-solver.md` y los tres documentos, declarada COMO
  RAZONAMIENTO SOBRE EL DERIVADO y no como medición del árbol —el Project no contiene `solver/`—.
  Salida: cero apariciones de `requiereTutor`/`tutor`/`Tutoria` en toda la referencia (confirma
  D-B5-5 en el derivado); `Actividad` 6 componentes, `ActividadDto` gemelo exacto de 6;
  `ProblemaHorario` 9 listas; `Profesor` 2 componentes sin tutoría; `ReglaDura` 7 constantes,
  ninguna de tutoría.
  §A DESMIENTE EL CORTE QUE S89 RECOMENDÓ, y es el hallazgo que reordena el bloque. S89 proponía
  D2b-1 = «§A + propagar `requiereTutor` + inversión del test». Medido: eso deja la mitad-1 SIN
  CONSUMIDOR —un campo que nadie lee es D-B5-5 una capa más abajo, con otro nombre—. CORTE NUEVO:
  D2b-1 = las DOS patas de transporte (`requiereTutor` + `ProfesorTutoria` al `ProblemaHorario`),
  sin verificar S8; D2b-2 = `ReglaDura` + verificador. Criterio que lo justifica: cada mitad debe
  entregar algo que un test pueda aseverar. Segundo hallazgo del §A, que el plan no decía: S8 exige
  además resolver «grupo cubierto por los subgrupos de P», tercera pieza y no dos —el mecanismo ya
  existe en el verificador vía S9—.
  TURNO DE CONTRASTE (M4): SEIS correcciones, CINCO del arquitecto, y la primera INVALIDÓ EL
  CONTRATO, no un detalle.
  (1) «SOLO io» ERA INALCANZABLE. Hay DOS caminos que construyen `domain.Actividad`, no uno:
  `ProblemaHorarioMapper:149` (el que yo tenía en la cabeza) y `CatalogoMapper:284` (app, olvidado).
  Añadir el 7.º componente rompe la compilación del segundo y FUERZA la decisión semántica que el
  bloque existe para tomar. Mi «fuera de alcance» no era una decisión: era un descuido que ocultaba
  la decisión. RESUELTO: `CatalogoMapper` PROPAGA `isRequiereTutor()`. Clavar `false` produciría el
  estado incoherente entidad=true/dominio=false por la puerta de entrada REAL de configuración
  (el CRUD de 8.5-C1), y dejaría S8 verificable en fixtures e inverificable en producción —la misma
  asimetría que D-F8.6-iiiA-c documenta en otra capa—.
  (2) ORDEN DE ARGUMENTOS DEL RECORD, corregido contra mi propuesta: escribí
  `(grupo, profesor, rol)` por analogía con la PROSA de S8 en el modelo, que no es una firma. La
  entidad JPA gemela de S77 es `(profesor, grupo, rol)`. Dos gemelos con los dos primeros invertidos
  es footgun sin contrapartida. ADOPTADO `(Profesor, GrupoAdministrativo, RolTutoria)`.
  (3) TRES RAZONES DE ÁRBOL para la décima lista que yo no tenía, además de la mía (el `rol` se
  perdería dentro del grupo): `GrupoAdministrativo` se usa como elemento de `Set` y como clave de
  mapa, así que meterle tutores envenenaría su `equals`; la 2.ª pasada ya tiene profesores y grupos
  construidos, luego una lista décima calca `bloqueos` sin ciclos; y `resolverGrupo` es recursivo con
  detección de ciclos por `grupoPadre`, donde inyectar refs a `Profesor` acopla dos catálogos.
  (4) LOS CALL SITES QUE MEDÍ ERAN LOS EQUIVOCADOS. Pedí los de `Actividad` (7: 2 main + 5 test) y
  no los de `ProblemaHorario`, que son OTROS 7. Consecuencia mecánica de la décima lista, reparada
  sin parar y ACEPTADA: no es alcance nuevo, es lo que «añadir un componente a un record» significa.
  (5) SIN TESTS DE ROUND-TRIP EL BLOQUE ERA TRANSPORTE NO VERIFICADO POR CONSTRUCCIÓN. Cuatro
  mutaciones compilables e invisibles hoy (cruce de lookup profesor/grupo, default invertido
  `null→true` sobre 43 fixtures que omiten el campo, lista sin cablear, `CatalogoMapper` clavando
  `false`). El criterio con el que defendí este corte —«cada mitad entrega algo aseverable»— se me
  había olvidado materializar en el contrato. Los tests entraron por el contraste.
  ENTREGADO: `domain` gana `Actividad.requiereTutor` (7.º, `boolean` primitivo), `RolTutoria`
  PROPIO de solver (no se reutiliza el de `app/`: solver no depende de app), `ProfesorTutoria` y la
  DÉCIMA lista `ProblemaHorario.tutorias`; `io` gana `ActividadDto.requiereTutor` (`Boolean`
  WRAPPER, porque los 43 fixtures vivos omiten el campo y el mapper colapsa `null→false`),
  `ProfesorTutoriaDto` y la décima lista del DTO; el mapper resuelve en 2.ª pasada calcando
  `bloqueos` (idiom `== null ? List.of()`, helper `resolver`, helper `construir` para el enum);
  schema con ambos campos OPCIONALES; `CatalogoMapper.aActividad` propaga y su Javadoc REVOCA
  D-B5-5.
  SEIS TESTS con asertos discriminantes: T1 round-trip con FIXTURE DEFENSIVO obligatorio
  (`problema-8-5-tutorias.json`, MAT8/1ESO-A + LEN2/1ESO-B: códigos divergentes y un segundo par,
  para que un lookup cruzado resuelva a algo DISTINTO en vez de fallar por casualidad; mata la
  mutación del cruce); T2 sin `tutorias` → lista vacía; T3 el PAR ausente→false / presente→true;
  T4 rol, profesor y grupo desconocidos → `ProblemaInvalidoException`; T5 sustituye el Caso 4 de
  `CatalogoMapperActividadTest:136` —que aseveraba «el flag se ignora»— por el PAR true→true /
  false→false, renombrado `aActividad_propagaRequiereTutor`: con un solo `isTrue()` no se discrimina
  contra un `true` clavado. El aserto de lista cableada NO se duplicó: T1 lo mata por construcción.
  CAMPAÑA DE 7 MUTACIONES, cero supervivientes. UNA OBJECIÓN DEL ARQUITECTO A SU PROPIA CAMPAÑA,
  registrada como deuda en vez de reabrir el bloque: M6 («el mapper ignora el rol y clava
  TUTOR_PRINCIPAL») cae por T4a, es decir por el test del rol INVÁLIDO, no por ninguno que asevere
  que un rol VÁLIDO se transporta. Como el fixture de T1 lleva `TUTOR_PRINCIPAL`, clavarlo es
  indistinguible del acierto. La mutación honesta exige un fixture con `CO_TUTOR` y un aserto sobre
  `rol()`.
  Suite solver 78 → 84 (+6), app 237 (Caso 4 sustituido, neto 0), total 315 → 321.
  `solver/src/main` TOCADO → `referencia-codigo-solver.md` REGENERADA (commit aparte, M4).
  `modelo_datos_fase1.md` no se toca por entidad ni invariante nueva (S8 ya existía, aquí solo se
  transporta), PERO su nota de S77 (§ «Estado de implementación de `ProfesorTutoria`») queda como
  ESTADO VIVO EQUIVOCADO por R5 y se corrige en la misma sesión.
  CIERRA D-B5-5. DEUDA NUEVA: D-F8.5-D2b1-a (la ruta JPA clava `tutorias` vacía), D-F8.5-D2b1-b
  (el rol no tiene aserto de transporte).
  MÉTODO CORREGIDO EN LA MISMA SESIÓN, a raíz del fallo (1) del contraste y por decisión explícita
  del usuario de no dejarlo solo en el prompt de apertura: tercera precisión de M2 (un tipo
  compartido se mide en TODOS los módulos; la pregunta es «quién más lo construye o consume», y si
  el §A no puede verlo desde el Project la enumeración se pide en el contraste y el contrato NO se
  cierra hasta tenerla) y precisión de ORDEN en M4 (bloque multi-módulo → el contraste mide ANTES
  del contrato, no después). Ninguna nace como M5: son precisiones a procedimientos que ya existen
  y ya funcionaron —el contraste cazó esto—, y un apartado nuevo sugeriría procedimiento nuevo.
  Sede PERMANENTE y no prompt: un prompt no se conserva, que es D-F8.0-a otra vez. Se anota además
  el criterio de acumulación de M2 (una cuarta precisión obliga a condensar, no a añadir).
  LO QUE ESTAS NORMAS NO HACEN, escrito para que nadie lo dé por hecho: estrechan la rendija de
  «tipo compartido entre módulos», no cierran la clase «afirmar sobre terreno no leído», que lleva
  desmintiéndose desde S75 y produjo cuatro fallos de instrumento en S89. La red que hace el trabajo
  sigue siendo el contraste.
  Siguiente: 8.5-D2b-2 (verificación de S8: `ReglaDura` + `VerificadorSolucion` + cableado del
  `ProfesorTutoriaRepository`, que es la deuda D-F8.5-D2b1-a y NO puede quedar fuera o S8 sería
  inverificable en producción), 8.6-B (contrato por decidir ANTES de medir) u 8.4-B (MOCKUP PREVIO;
  D-F8.4-A-c es trabajo de backend), a decidir al abrir sesión.

### Sesión 91 — Fase 8, Bloque 8.5-D2b-2: S8 VERIFICABLE por el solver (CIERRA D-F8.5-D2b1-a y -b).
  Modo híbrido, DOS turnos de Claude Code. 4 commits (solver: verificador; app: cableado; doc:
  referencia regenerada). CIERRA el frente 8.5-D2b entero, abierto en S77 y desplazado doce sesiones.
  §A DE MEDICIÓN CONTRA EL ÁRBOL, no contra el derivado: cambio propuesto por el USUARIO al cerrar
  S90, porque `referencia-codigo-solver.md` lista FIRMAS y no CONSUMIDORES, y por eso el §A había
  fallado en S89 y S90. DESMINTIÓ DOS AFIRMACIONES DEL ARQUITECTO, ninguna visible en la referencia:
  (1) el supuesto ANCLAJE DE `tramoCodigo`, que el arquitecto declaró «problema estructural» sobre la
  referencia: FALSO, dos de las siete reglas (`INSTANCIA_SIN_COLOCAR`, `DISTRIBUCION_MISMO_DIA`) ya
  ponen `tramoCodigo=null` y tres ponen `recursoCodigo=null`; `Violacion` YA admite violaciones no
  ancladas a tramo y S8 entra sin forma nueva. (2) `CatalogoMapper.aProblemaHorario` NO inyecta
  repositorios: es `static` puro con las listas por parámetro, luego «cablear el repositorio en el
  mapper» —como lo describía D-F8.5-D2b1-a y como lo escribió el arquitecto— era IMPOSIBLE: el
  cableado es un PARÁMETRO y el `findAll()` vive en el llamador.
  TERCER HALLAZGO, el que reordena el bloque: NINGÚN fixture `.json` vivo lleva `requiereTutor` (solo
  el schema y un JSON inline en `ProblemaHorarioJsonLoaderTest:374`), así que S8 era VACUAMENTE cierta
  en los 43. Encender el verificador sin fixture nuevo habría dejado los 84 tests verdes midiendo aire.
  FALLO DEL §A, del arquitecto: NO midió los consumidores de `aProblemaHorario`. Es la tercera
  precisión de M2 incumplida UNA SESIÓN después de escribirla. Se reparó metiendo la enumeración (ML1-
  ML4) en el PRIMER turno de código, antes de contratar la mitad de `app` — que es la precisión de
  orden de M4 aplicada dentro de la sesión.
  PARTICIÓN PROPUESTA Y RETIRADA: el arquitecto propuso partir en 2a/2b por «superficie desconocida»
  del frente `app`. El USUARIO objetó el coste documental de partir por enésima vez. La objeción se
  aceptó porque la MEDICIÓN la respaldaba —`ReglaDura` cruza a `app` como `String` (`ViolacionDTO.regla`),
  no como tipo, así que añadir constante no propaga; y el cableado resultó ser un parámetro—, con
  UNA condición del método: el contraste mide el llamador ANTES de escribir `app`. Medido: UN solo
  llamador en main (`GeneradorHorarioService.cargarProblema:126`), `@Transactional(readOnly=true)`, con
  los `@ManyToOne(LAZY)` de la PK navegables en sesión. El riesgo de `LazyInitializationException` que
  motivaba la cautela estaba cubierto por el diseño existente.
  DECISIONES DEL CONTRATO, ninguna derivable de la medición: (D1) SEDE en `VerificadorSolucion` y no
  en pre-validación —el modelo dejaba la disyuntiva abierta; pre-validación es superficie REST distinta
  y abriría D-F8.4-A-c—; (D2) forma de la `Violacion` calcando `DISTRIBUCION_MISMO_DIA`, con
  `recursoCodigo`=código del GRUPO (lo que falta es un profesor, no lo hay sobrante) y `tramoCodigo=null`;
  (D4) el transporte JPA lleva AMBOS roles y el filtro vive en el verificador —filtrar `CO_TUTOR` en el
  mapper reproduce la asimetría con la que S90 justificó propagar en `aActividad`—; (D6) cobertura
  grupo←subgrupo CIEGA al `grupo_padre`, reutilizando el criterio de S9. NO fue decisión sino LECTURA
  que el verificador filtre `TUTOR_PRINCIPAL`: el enunciado de S8 lo dice literal, aunque el prompt de
  apertura lo presentaba como abierto.
  ENTREGADO: `ReglaDura.TUTORIA_SIN_TUTOR` (no propagó: nadie hace `switch` exhaustivo);
  `VerificadorSolucion.verificarTutorias` como QUINTO acumulador y ÚNICO método del fichero que NO
  recibe ni usa `solucion` —S8 es propiedad del CATÁLOGO, no de la solución, y eso va en TSDoc para que
  nadie lo «uniforme»—; fixture 44.º `problema-8-5-s8.json` con tutoría TRAMPA (P-BETA es
  `TUTOR_PRINCIPAL`, pero de OTRO grupo: mata a la vez «ignoro el rol» e «ignoro el grupo»);
  `aProblemaHorario` gana ONCEAVO parámetro `List<app.catalog.ProfesorTutoria>` (sigue `static` puro),
  con resolución por IDENTIDAD contra los índices ya materializados y traducción de enum por nombre que
  ABORTA si no existe en destino; `GeneradorHorarioService` inyecta el repo y pasa el `findAll()` dentro
  de la misma transacción; SIETE call sites de test actualizados (el arquitecto contó SEIS y eran
  siete: transcribió mal una medición correcta, inocuo porque Claude Code fue al árbol y no al guion).
  TRECE TESTS: T1-T7 en solver (T2 es el par de T1 que discrimina el ROL, que es lo que D-F8.5-D2b1-b
  exigía y S90 no pudo dar), B-T1..B-T5 en app. CAMPAÑA DE 12 (7+5), cero supervivientes.
  DOS ASIMETRÍAS REGISTRADAS, no tapadas: (a) M6 («el acumulador no se invoca») lo matan T2/T3/T6/T7 y
  SOBREVIVEN T1/T4/T5 —los tres casos NEGATIVOS—, porque un método que nunca corre hace pasar todos los
  «no viola» trivialmente: T1/T4/T5 NO cubren el cableado, y leer la tabla como matriz de cobertura es
  lo que M3 prohíbe desde S82; (b) N5 («`cargarProblema` pasa `List.of()`») lo mata B-T5 EN EXCLUSIVA,
  ningún unitario lo caza: es exactamente la asimetría fixtures/producción que D-F8.5-D2b1-a describía,
  y la razón de que el test de integración no fuera opcional.
  Suite solver 84 → 91, app 237 → 242, total 321 → 333. `solver/src/main` TOCADO →
  `referencia-codigo-solver.md` REGENERADA (commit aparte, M4). `modelo_datos_fase1.md` TOCADO: la nota
  de §«Estado de implementación de `ProfesorTutoria`» declaraba S8 sin verificar por (a) nadie lee
  `tutorias()` y (b) la ruta JPA las clava vacías; las DOS son falsas desde esta sesión, y por R5 eso
  es estado vivo equivocado.
  CIERRA D-F8.5-D2b1-a (BLOQUEANTE) y D-F8.5-D2b1-b. DEUDA NUEVA: D-F8.5-D2b2-a (triplicación del
  predicado de cobertura de grupo), D-F8.5-D2b2-b (javadoc de `CatalogoMapper`, pre-existente).
  PRUEBA DE PROCEDIMIENTO (S90-S91), NO canonizada: cinco pasos con el §A ejecutado por Claude Code
  contra el árbol y un paso 3 de CONTRATO con la medición delante. Medidas: M-a=4 (D1, D2, D4, D6, cada
  una con fuente señalable), M-b=1 (Claude Code devolvió la pregunta del predicado con tres opciones en
  vez de decidir), M-c=0, M-d=2 (igual que S90: el paso 3 no costó turno extra). Veredicto FAVORABLE
  con el sesgo declarado en pie —el arquitecto puntúa su propio paso y el sujeto le era favorable—, y
  con la limitación de que M-a mide que el paso 3 APORTÓ decisiones, no que cuatro pasos no las
  hubieran aportado igual. Lo que NO depende del juicio del arquitecto: el §A contra el árbol desmintió
  dos afirmaciones suyas invisibles en la referencia, y eso valida el cambio del USUARIO, no el paso 3.
  NO se escribe en el plan como método: canonizar un procedimiento con una sola ejecución es el vicio
  que S86 identificó.
  Siguiente: 8.4-B (MOCKUP PREVIO; arrastra la contradicción de severidades de D-F8.4-A-c, que es
  trabajo de BACKEND y puede ser abrir dos bloques), 8.6-B (cruce de índices; su contrato hay que
  decidirlo ANTES de medir, orden inverso a M2) o D-F8.6-iiiA-b (`Totales` sin sede), a decidir al
  abrir sesión.

### Sesión 92 — Fase 8, Bloque 8.4-B1: panel de pre-validación (8.4-B PARTIDO por medición).
  Modo híbrido. 1 commit de código (3a82744, amendado sobre 1787001 para absorber el movimiento
  del modelo a `models/`; sin pushear al cerrar) + doc aparte.
  DECISIÓN DE APERTURA — D-F8.4-A-c: el arquitecto recomendó MATAR `AVISO` y la MEDICIÓN LE
  DESMINTIÓ. El §A destapó tres cosas invisibles desde el Project: (1) el javadoc de `Severidad`
  documenta un CRITERIO DE DISEÑO —«una regla que pudiera sobrestimar la demanda debe quedarse en
  AVISO»— y nombra el palomar de aulas como candidato natural, luego el valor no está huérfano por
  descuido; (2) el string `"AVISO"` viaja en el contrato REST (`AvisoPrevalidacionDTO` serializa
  `.name()`), así que borrarlo estrecha un contrato publicado aunque el frontend no conozca el enum;
  (3) el beneficio que el arquitecto atribuía a la muerte —que caerían los asertos tautológicos de
  A3/A4— es FALSO: ambos aseveran `ERROR`, valor que sobrevive. Nació la RAMA C: `AVISO` se queda,
  el panel se diseña con dos niveles desde el principio (el frontend se construía desde cero, medido:
  `grep` de prevalidacion en `.ts/.html/.scss/.css` dio EXIT=1), y el bloque queda de UN SOLO MÓDULO.
  EL CONTRASTE (M4) TUMBÓ EL CONTRATO POR PREMISA, no por detalle: NO EXISTE gesto de generar en el
  frontend. Cero `<button>` en todo `src` salvo el candado de `horario-grid.html:31,36`; cero `POST
  /api/horarios` (el único POST del árbol es `bloqueo.service.ts:30`); `horario.service.ts` son 21
  líneas con un solo método de lectura. Dos de los seis asertos —los que sostenían la decisión del
  usuario sobre el botón— NO ERAN ESCRIBIBLES. Causa declarada: el §A midió el endpoint y el DTO pero
  NO el CONSUMIDOR del gesto, mismo hueco que M2 documenta de S90 (medir `Actividad` y no
  `ProblemaHorario`). 8.4-B se PARTE en B1 (panel, esta sesión) y B2 (guarda + diálogo), y la
  partición es DESCUBIERTA POR MEDICIÓN, no planificada: el usuario había objetado —con razón— contra
  particionar de antemano por el coste documental fijo de M1, y esa objeción sigue en pie.
  SEGUNDO ERROR DEL CONTRASTE, de precedente: el arquitecto citó `errorDiagnostico` (S87) como modelo
  de «no ejecutado vs vacío». Es el CONTRAEJEMPLO —distingue error de no-error, y `badges()`/
  `violaciones()` colapsan `diagnostico() === null` y cargado-vacío en el mismo `new Map()`
  (`horario-view.ts:68-82`)—. El discriminante real es el `@if/@else if/@else` de
  `horario-view.html:33-47`, donde `proyeccion() === null` cae en «Cargando…» y una proyección vacía
  monta la rejilla igual. Caveat que el propio contraste declaró: ese precedente funde «cargando» con
  «aún null» y no tiene estado semántico nombrado, así que se EXTIENDE, no se copia.
  TERCER FALLO, de forma y del arquitecto: el primer guion se entregó con referencias a «las clases
  disjuntas de arriba» y «según la tabla» en un texto que se pega SOLO. Claude Code PARÓ en vez de
  inventar los nombres de clase, que es lo correcto —inventarlos habría roto los asertos que se
  aseveran por `querySelector('.clase-exacta')`—. Norma nueva del usuario: TODO GUION VA EN BLOQUE
  COPIABLE Y AUTOCONTENIDO.
  ENTREGADO: `models/prevalidacion.model.ts` (seis campos espejo del DTO, `severidad: string`),
  `services/prevalidacion.service.ts` (wrapper pelado, gemelo de `diagnostico.service.ts:30-32`),
  `components/panel-prevalidacion/` (.ts+.html+.css) y el cableado en `horario-view`. CUATRO RAMAS
  con clases DISJUNTAS: `.prevalidacion-error` / `-pendiente` / `-limpia` / `-panel`, más
  `.contador-errores` / `.contador-avisos` / `.aviso-entrada` / `.es-error` dentro de la cuarta.
  `avisos()` es `signal<AvisoPrevalidacion[]|null>` con inicial `null`, y esa señal es la que separa
  «no ejecutado» de «ejecutado y vacío». `getPrevalidacion` dentro de `cargar(id)` y NO en el
  constructor; `errorPrevalidacion` es señal propia que NO gatea la rejilla (criterio de S87).
  MOCKUP (D-F8.6-a) hecho y VALIDADO con el usuario: panel colapsable con cabecera siempre visible,
  línea verde para el caso limpio, y dos niveles distinguidos por TRES señales (icono, borde, y sobre
  todo el TEXTO DE LA CONSECUENCIA: «imposible de resolver» vs «puede ser sobrestimación»), no solo
  por color. El diálogo del mockup salió del alcance con B2.
  FICHERO TOCADO NO PREVISTO, señalado por Claude Code: `horario-view.spec.ts`. Al inyectar
  `HorarioView` el nuevo servicio, su TestBed debía proveer un doble o los 14 tests reventaban por
  inyección. NO es test nuevo (de ahí 52+4=56, no +5). Patrón que se repetirá: un componente que gana
  dependencia obliga a tocar su spec previo.
  CAMPAÑA DE 5, cada una por su vía prevista, SIN COLATERALES y ninguna dejó de compilar. M2 y M3
  caen por el MISMO aserto atacando dimensiones distintas (estado inicial vs estructura de ramas):
  no es cobertura duplicada, es el único punto donde ambas se observan. M6 (`=== 'ERROR'` →
  `!== 'AVISO'`) NO SE EJECUTA: SUPERVIVIENTE DECLARADA, equivalente con dos valores en el enum; un
  test que la matara cubriría un caso imposible hoy.
  Suite frontend 52 → 56 (11 ficheros de test, antes 9); backend 333 INTACTO. No se tocó
  `solver/src/main` → `referencia-codigo-solver.md` NO regenerada; `modelo_datos_fase1.md` NO tocado.
  DEUDA NUEVA: D-F8.4-B1-a (la caída de M2 depende de que (28) no llame a `setInput` en su primera
  mitad). D-F8.4-A-c REENCUADRADA, no cerrada.
  Siguiente: 8.4-B2 (guarda + diálogo, pero antes hay que crear el gesto de generar), 8.6-B (aviso
  durante el arrastre; contrato ANTES de medir, orden inverso a M2), D-F8.6-ivB-a (resto: el
  `errorPin.set(null)` de `alDespinar`) o D-F8.6-iiiA-b (`Totales` sigue sin sede), a decidir al abrir.

### Sesión 93 — Fase 8, Bloque 8.4-B2: gesto de generar + guarda con diálogo (CIERRA el frente 8.4).
  Modo híbrido. 2 commits de código (37d1ba9 producción 8 ficheros, 6b6e88c tests 3 ficheros; sin
  pushear al cerrar) + doc aparte. CIERRA 8.4 ENTERO (A en S79, B1 en S92, B2 aquí).
  PARTICIÓN PROPUESTA Y RETIRADA POR MEDICIÓN, y esta vez el desmentido fue AL ARQUITECTO. El plan
  declaraba 8.4-B2 bloqueado y candidato a partirse en «gesto» y «guarda», con el argumento de que el
  diálogo sería el PRIMER modal del repo y decidiría la arquitectura de diálogos para siempre. El
  USUARIO objetó partir de antemano (misma objeción que en S91, por el coste documental fijo de M1) y
  propuso medir primero. Medido, las dos premisas del corte cayeron: (P2) `POST /api/horarios` acepta
  BODY VACÍO —`@RequestBody(required=false)` y defaulting íntegro en `GeneradorHorarioService:179-203`:
  via→OPTIMIZACION, maxSegundos→30, semilla→42, nombre→timestamp—, luego el botón es un botón y no un
  formulario, que era mi preocupación mayor; (P5) `@angular/cdk` YA está en `dependencies` (^21.2.14)
  con el entry point `dialog` presente, luego no había librería que elegir. UN SOLO BLOQUE. Lo que
  mató la partición fue P2, no P5: el diálogo siguió costando (ver C2 abajo).
  EL §A MIDIÓ LOS CONSUMIDORES, no solo el endpoint —correctivo explícito de la lección de S92, que
  midió endpoint y DTO y no el gesto que el contrato daba por existente—. Se enumeraron SEIS
  supuestos del contrato antes de escribirlo (endpoint, consumidor del resultado, fuente del id,
  señal de la guarda, superficie del diálogo, parámetros de D29) y dos de ellos lo reformaron.
  DOS HALLAZGOS DEL §A QUE CAMBIARON EL CONTRATO, no detalles: (1) el POST devuelve
  `HorarioProyeccionDTO` ENTERO, no un id, así que «generar y recargar» era redundante; (2) NO HAY
  selector de horario: el id sale de `paramMap` (`horario-view.ts:109`), y los dos `<select>` de la
  plantilla emiten vista y entidad, no horario. Generar crea un horario NUEVO con id nuevo que no es
  el de la ruta. DECISIÓN DE PRODUCTO que no estaba tomada y que S92 no podía prever: opción A
  (navegar) frente a B (pintar en sitio). ELEGIDA A por el usuario con recomendación del arquitecto:
  B dejaría rejilla, pines y diagnóstico pudiendo pertenecer a horarios DISTINTOS, clase de bug que
  ningún test de este bloque detectaría. Coste asumido y declarado: un GET redundante, porque la
  proyección que devuelve el POST se DESCARTA y la recarga la dispara `paramMap`.
  EL CONTRASTE (M4) DESMINTIÓ CUATRO PREMISAS DEL ARQUITECTO, todas del mismo género —afirmar sobre
  terreno no leído—: (1) la ruta declarada es `horario/:id` SINGULAR (`app.routes.ts:7`), no
  `horarios/:id`: `navigate(['/horarios', id])` no casa con ninguna ruta y navegaría a ninguna parte;
  (2) la señal del contenedor es `avisosPrevalidacion()` (`horario-view.ts:71`), NO `avisos()`, que es
  el input del panel HIJO —el arquitecto citó la cabecera de S92, es decir el DERIVADO, en vez del
  árbol: es la trampa que M2 documenta, cometida sobre el propio registro de la sesión anterior—;
  (3) `@angular/cdk/dialog` es el PRIMITIVO SIN ESTILO, no un equivalente de `@angular/material`: hay
  que aportar el componente de confirmación y su CSS enteros, así que «cero fricción» era medio falso;
  (4) los 25 tests de `horario-view.spec.ts` caen por `Router`, NO por `Dialog` —`Dialog` es
  `providedIn:'root'` y se inyecta solo—, y el TestBed omitía `provideRouter` CON RAZÓN DOCUMENTADA en
  su cabecero (l.35-38: «añadir el router real metería un colaborador que el componente no usa»). Este
  bloque INVALIDA esa razón: el comentario se corrige, porque por R5 una descripción equivocada del
  mecanismo actual es estado vivo equivocado.
  C6 DESMENTIDO COMO INALCANZABLE, y es el hallazgo que reencuadró el bloque. El contrato prometía
  «mensaje distinto para el 422 de pre-validación y para el infactible», citando la casilla de 8.4-A.
  MEDIDO: los dos 422 llegan al frontend con BODY SECO IDÉNTICO. `HorarioController:64-67` lanza
  `ResponseStatusException(422, e.getMessage())`; el `reason` solo viajaría con
  `server.error.include-message` activo, y `application.properties` no lo define (default `never`,
  D-F8.6-ii-a). `PrevalidacionFallidaException` lleva `getAvisos()` estructurado DENTRO, pero el
  controller descarta esa lista. La distinción existe EN EL BACKEND y no en el cable. Tres salidas
  evaluadas: (A) cortar C6 y registrar deuda; (B) meter el backend, que convierte esto en bloque
  MULTI-MÓDULO y toca la política global de errores que D-F8.6-ii-a y D-F8.6-iiiA-c dicen que solo
  tiene sentido decidir GLOBALMENTE; (C) discriminar por `status`, descartada porque ambos son 422.
  ELEGIDA A. D5 REVOCADA. Y el desmentido obligó a AJUSTAR D4: si el usuario no va a poder distinguir
  la causa del rechazo, el DIÁLOGO se lo dice por adelantado —enumera los errores concretos que
  `avisosPrevalidacion()` ya tiene y advierte de que el servidor rechazará sin detalle—. La información
  existe en el cliente ANTES de enviar; es ahí donde vale. Sin ese ajuste, la guarda avisa de un error,
  el usuario confirma, y el texto resultante es el mismo que si el solver no encontrara solución.
  CORRECCIÓN POR R5 EN SEDE VIVA: la casilla de 8.4-A afirmaba «422 distinguible del infactible del
  solver». Es FALSO desde el cliente y se corrige en su casilla. Lo archivado en la bitácora NO se
  toca (histórico de solo lectura: borrar el error eliminaría la evidencia de que existió).
  ENTREGADO: `horario.service.ts` gana `generar()` (POST con body `{}`, wrapper pelado, gemelo de
  `getProyeccion`); `components/confirmar-generacion/` (.ts+.html+.css) como PRIMER diálogo del repo;
  `horario-view` inyecta `Router` y `Dialog` y gana el gesto con sus TRES estados —sin ERROR genera
  directo; con ERROR abre diálogo SIN escapatoria real; `avisosPrevalidacion() === null` deja el botón
  DESHABILITADO, porque no se ha ejecutado la pre-validación y no hay nada sobre lo que guardar—;
  `errorGeneracion` es señal propia que NO gatea la rejilla (criterio de S87, mismo que
  `errorPrevalidacion` en S92).
  `styles.css` GANA `@import '@angular/cdk/overlay-prebuilt.css'`: PRIMERA HOJA GLOBAL DEL CDK en el
  repo, y es MECANISMO VIVO, no anécdota. Sin ella el overlay del `Dialog` se monta sin centrar ni
  backdrop. Es la tercera cara del error de C2: la dependencia estaba instalada, pero «instalada» no
  es «lista», y el §A no lo vio porque midió la API (`providedIn:'root'`, sin provider) y no el
  montaje. Lo destapó Claude Code al integrar, no un usuario abriendo el diálogo. Queda importada
  para cualquier uso futuro de overlay/tooltip del CDK.
  DOS PARADAS DE CLAUDE CODE, las dos correctas y las dos por omisión del arquitecto: (1) el guion de
  commits enumeraba ficheros y OLVIDABA `horario-view.css` (clase `.error-generacion`) y `styles.css`;
  paró en vez de decidir el reparto por su cuenta. Van al commit de CÓDIGO: ninguno es test y ambos
  son parte del mismo gesto —un tercer commit para CSS separaría un `<p>` de su estilo y un componente
  de lo que lo hace visible, y el criterio de M4 es que un commit CONSTRUYA, no que agrupe por
  extensión—. (2) Antes, en el turno de contraste, devolvió la pregunta de C6 con tres salidas en vez
  de elegir.
  TESTS: 7 en el primer turno (T1-T6 en contenedor + 1 de servicio) y 4 en el segundo (T7-T9), suite
  frontend 56 → 63 → 67 (12 ficheros de test, antes 11). CAMPAÑA declarada en dos tandas. Los TRES
  huecos que la primera campaña destapó se CERRARON en la misma sesión, con criterio explícito de por
  qué esos tres y no el cuarto: (h1) la rama «no ejecutado» es el TERCER ESTADO de D4, contratado, y
  su ausencia dejaba sin red media decisión de producto; (h2) el `data` del `open` es el AJUSTE D4'
  ENTERO —si llega la lista completa o vacía, la razón por la que se eligió la opción A no existe y
  nadie se entera—; (h3) `ConfirmarGeneracion` tenía CERO cobertura y la mutación de intercambiar
  `true`/`false` en confirmar/cancelar invierte la guarda entera quedando verde.
  DOS FIXTURES DIVERGENTES POR DISEÑO, mismo criterio que el (22) de S89: T5 navega a id 99 siendo 1
  el de la ruta —con id igual, «navegó» y «no navegó» son indistinguibles—; T8 lleva un aviso ERROR y
  otro AVISO con textos distintos —sin el no-ERROR, «pasar todo» y «filtrar» dan el mismo `data`—; T9
  enumera DOS errores —con uno solo, «pinta el primero» y «pinta todos» coinciden—.
  MATIZ DE T7 REGISTRADO Y NO TAPADO, que es lo que M3 exige: borrar la guarda a secas da
  `null.filter` → TypeError, es decir ROJO POR EXCEPCIÓN y no por aserto. Sigue siendo rojo y el test
  cumple, pero la mutación honesta contra esa dimensión es la que devuelve TRAS abrir/generar, y esa
  sí cae limpia. Acoplamiento declarado también en T8 (el `toHaveBeenCalledWith` reddearía si `open`
  no se llamara, pero de esa se encarga `open times(1)`/`generar times(0)` en el mismo test: el
  `withArgs` no es quien la ataca) y en T9 (asevera presencia en `textContent`, no posición ni
  estructura: reordenar o reestilar no reddea, y queda fuera de alcance a propósito).
  Backend 333 INTACTO. No se tocó `solver/src/main` → `referencia-codigo-solver.md` NO regenerada;
  `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva).
  DEUDA NUEVA: D-F8.4-B2-a (el `errorGeneracion.set(null)` de reintento sin test, gemelo de lo que
  (25b) cubrió para los pines en S89; se deja fuera POR COHERENCIA con D-F8.6-ivB-a resto, que tiene
  el gemelo abierto por el mismo motivo, no por descuido).
  LIMPIEZA EVALUADA Y DESCARTADA (M1.5): 8.4 queda CERRADO entero y es candidato natural a
  condensación, pero su casilla B2 es hoy lo ÚNICO que documenta el gesto y se acaba de escribir.
  Se condensa en la próxima sesión de higiene, no en la que lo cierra.
  Siguiente: 8.6-B (aviso durante el arrastre; cruce de índices, contrato ANTES de medir, orden
  inverso a M2), D-F8.6-ivB-a resto + D-F8.4-B2-a (los dos `set(null)` de reintento, ahora gemelos
  declarados), D-F8.6-iiiA-b (`Totales` sin sede) o HIGIENE (condensar 8.4), a decidir al abrir.

### Sesión 94 — Fase 8, Bloque 8.6-iv-D: los `set(null)` DE REINTENTO, los dos juntos (CIERRA D-F8.4-B2-a y el punto (a) de D-F8.6-ivB-a).
  Modo híbrido. 1 commit de código (solo `horario-view.spec.ts`) + doc aparte. Bloque BARATO por
  criterio ya escrito: las dos deudas venían DECLARADAS GEMELAS en S93 y asignadas a cubrirse
  JUNTAS con un test que encadenara dos invocaciones. Esta es la sesión que las cierra a la vez.
  §A DE MEDICIÓN CONTRA EL ÁRBOL (Claude Code), y DESMINTIÓ CUATRO AFIRMACIONES DEL ARQUITECTO,
  todas del mismo género —afirmar sobre terreno no leído—, tres de ellas sobre el PROPIO PLAN:
  (1) la RUTA que el arquitecto puso en el guion (`app/frontend/src/app/horario/`) NO EXISTE: los
  ficheros viven en `app/frontend/src/app/components/horario-view/`. La escribió sin medirla, dentro
  del guion que existe para no suponer (mismo género que el error de enrutado de S84);
  (2) la LÍNEA 183 que el plan fijaba para el `errorPin.set(null)` de `alDespinar` estaba RANCIA:
  es la 236. La fijó S89 y S92/S93 tocaron el fichero en medio. Estado vivo equivocado por R5;
  (3) «LOS DOS `set(null)`» DESCRIBE MAL EL FICHERO: el grep devuelve NUEVE, en seis métodos. Lo que
  las deudas nombran no son «los `set(null)`» sino los DE REINTENTO (236 y 295); los otros siete son
  limpieza de carga —nadie reintenta `cargarDiagnostico`—. La distinción se sostiene, pero el nombre
  que el plan usaba no la llevaba dentro y por eso se pudo leer como «hay dos en el fichero»;
  (4) EL ANDAMIO DE S89 NO SERVÍA, y es el hallazgo que REENCUADRÓ EL COSTE. El plan trataba «el
  andamio» como uno solo y son TRES colaboradores con TRES formas: `guardar` es fresco por
  invocación desde S89, pero `borrar` y `generar` eran Subject COMPARTIDO. Un Subject que ya emitió
  `.error()` queda cerrado: al re-suscribirse redispara el error SÍNCRONAMENTE, lo que hace
  inobservable la fase discriminante, y un `next` de éxito en el segundo intento es imposible.
  Sin cambiar eso, el test que las deudas piden es INESCRIBIBLE.
  EN VERDE, medido: los siete providers estaban puestos (no faltaba ninguno), y `lanzarGeneracion`
  es alcanzable SIN diálogo por la vía directa de `generar()` cuando no hay avisos `ERROR`, así que
  la guarda de S93 no estorbaba —supuesto del arquitecto que la medición descartó por infundado—.
  CLAUDE CODE DEVOLVIÓ LA PREGUNTA SIN RESOLVERLA (versión fuerte vs. débil del «fallo → reintento»)
  en vez de decidirla, que es lo correcto. ELEGIDA LA FUERTE y no por gusto de rigor: la débil
  asevera `toHaveBeenCalledTimes(2)`, y contra la mutación de borrar el `set(null)` ese contador
  sigue dando 2 y el test queda VERDE. Habría cerrado dos deudas dejándolas sin red, que es PEOR que
  dejarlas abiertas, porque la casilla diría que están cubiertas.
  DECISIÓN DE ANDAMIO (A frente a B), del usuario con recomendación del arquitecto: (A) migrar los
  DOS dobles y adaptar los tests que emitían sobre ellos, dejando las tres formas homogéneas;
  (B) añadir la fábrica fresca solo donde hiciera falta, sin tocar ningún test. ELEGIDA A: la forma
  compartida no es una elección sino lo que había ANTES de que S89 descubriera que no servía, y
  mantenerla conserva un estado ya sabido equivocado; B dejaría dos formas de doble conviviendo para
  el mismo servicio y el próximo que escriba un test tendría que averiguar cuál toca —deuda de
  andamio, y de la que no se ve—. COSTE DECLARADO Y REVISADO AL ALZA ANTES DE ELEGIR: el arquitecto
  había dicho «bajo, un solo fichero»; con la medición delante pasó a MEDIO, porque A toca asertos
  commiteados y verdes para escribir dos nuevos.
  EL RECUENTO DE TESTS A ADAPTAR TAMBIÉN ERA DEL ARQUITECTO Y TAMBIÉN FALLÓ: listó (30) entre los
  que emiten sobre `sujetoGenerar` leyendo la lista de `it()` sin abrir el cuerpo. (30) NO emite:
  dispara vía `sujetoCerrado.next(true)` y solo asevera que `generar` fue llamado. Claude Code no lo
  tocó y lo señaló.
  ENTREGADO: `sujetoBorrar` y `sujetoGenerar` ELIMINADOS; `borrar` y `generar` pasan a
  `vi.fn(() => (ultimo… = new Subject…))` con sus variables `ultimoBorrar`/`ultimoGenerar` junto a
  `ultimoGuardar`; adaptados (2), (31) y (32) de forma mecánica y SIN tocar un solo aserto; (35) y
  (36) nuevos, cada uno con su ASERTO A (error poblado tras el primer fallo), su A-bis donde aplica
  (en (36), que el pin SIGA en el índice tras el fallo: si saliera, el segundo gesto se iría por el
  `return` de la guarda y el test mediría el NO-OP en vez del reintento) y su ASERTO B discriminante.
  CAMPAÑA DE 2, con la vía declarada como M3 exige tras el matiz de T7 en S93: M1 (borrar el
  `errorGeneracion.set(null)`) cae SOLO en (35); M2 (borrar el `errorPin.set(null)`) cae SOLO en
  (36); las dos POR ASERTO —un `expect(...).toBeNull()` recibe el `<p>` de error superviviente—, sin
  `TypeError` ni otra excepción. NINGÚN test previo cae con ninguna de las dos, que es la
  comprobación que M3 pide desde S82 y la que confirma que el bloque no sobraba.
  Suite frontend 67 → 69 (12 ficheros, sin cambio); backend 333 INTACTO. `horario-view.ts` idéntico
  a HEAD al cerrar (diff vacío): no hay producción en este bloque. No se tocó `solver/src/main` →
  `referencia-codigo-solver.md` NO regenerada; `modelo_datos_fase1.md` NO tocado.
  DEUDA NUEVA: D-F8.6-ivD-a (la capa defensiva perdida en (3) y (4)). CIERRA D-F8.4-B2-a y el punto
  (a) de D-F8.6-ivB-a, que SOBREVIVE acotada a su punto (b).
  CORRIGE POR R5, en todas sus sedes vivas: la línea 183 → 236, y «los dos `set(null)`» → «los
  `set(null)` DE REINTENTO».
  LIMPIEZA EVALUADA Y DESCARTADA (M1.5): 8.4 sigue siendo candidato natural a condensación y ahora
  se le suma 8.6-iv, pero esta sesión ya archiva ventana, y concentrar condensación y archivado
  sobre el mismo fichero es el motivo por el que S79 desplazó una limpieza. Queda para S95 con DOS
  frentes acumulados, no uno.
  Siguiente: HIGIENE (condensar 8.4 y quizá 8.6-iv, dos frentes cerrados acumulados), 8.6-B (aviso
  durante el arrastre; ÚNICO bloque de frontend abierto, contrato ANTES de medir, orden inverso a
  M2) o D-F8.6-iiiA-b (`Totales` sigue sin sede, con la trampa de los conteos sin signo), a decidir
  al abrir.

### Sesión 95 — Fase 8, HIGIENE DOCUMENTAL: condensación de DOS frentes cerrados (8.4 y 8.6-iv) y archivado de ventana.
  Sin código. Modo interactivo. TRES operaciones sobre `docs/`, en commits separados con verificación
  de R4 y de COSTURA ENTRE cada una y no solo al final —criterio adoptado por el fallo de S94, donde
  un `str_replace` mal anclado descabezó una deuda vecina y lo cazó el grep de M1.6, no la lectura—.
  DESBLOQUEA UN APLAZAMIENTO CIRCULAR: S93 aplazó 8.4 por ser la sesión que lo cerró y S94 lo aplazó
  otra vez por concentrar condensación y archivado sobre el mismo fichero (motivo de S79). El
  argumento de S94 se cumple en TODA sesión —todas archivan ventana—, luego sostenerlo una tercera vez
  equivalía a no condensar nunca. Se rompe una vez, con la mitigación escrita arriba.
  ALCANCE FIJADO POR MEDICIÓN, y el criterio de S63/S80 obligó a una distinción fina: 8.6 NO está
  cerrado entero —8.6-B sigue ABIERTO—, así que condensar «8.6» habría violado el criterio; lo cerrado
  es el SUB-FRENTE 8.6-iv, cuyas cuatro casillas están todas `[x]`. Medido con `grep '^- \[ \]'` sobre
  Fase 8: solo dos casillas abiertas, 8.5-D3 (aplazado indefinidamente) y 8.6-B.
  DOS FALLOS DEL §A DEL ARQUITECTO, los dos suyos y los dos declarados en vez de tapados: (1) el `sed`
  con que midió los tokens de 8.6-iv usaba el patrón `/Bloque 8.6-iv-A/` como ancla de inicio, y la
  PRIMERA coincidencia no es la casilla (l.1407) sino la CABECERA DE VENTANA (l.679): barrió 785
  líneas y devolvió 97 tokens, entre ellos `D-B5-5` y `D-B10-1..9`, que no pueden vivir en cuatro
  casillas de tests de frontend. El dato se DESCARTÓ y se remidió con rangos numéricos explícitos.
  (2) el censo de citantes contaba con `grep -c` sin ancla, que agrega la forma con punto final
  (`D-F8.4-A-c` y `D-F8.4-A-c.`): correcto para contar, trampa para verificar.
  EL HALLAZGO QUE FIJÓ EL CONTENIDO DE LAS LÍNEAS CONDENSADAS: cinco tokens tenían UN SOLO citante y
  era justo la línea a reescribir —`D-F8.4-A-a` y `D-F8.4-A-b` (ambos en l.1242), `D-F8.6-ivA-a` y
  `D-F8.6-ivA-c` (l.1422), `D-F8.6-ivB-c` (l.1431)—. Condensar sin conservarlos los dejaba HUÉRFANOS
  y rompía R4 en el mismo commit. Regla aplicada, heredada de S63: cada línea condensada conserva
  LITERALMENTE sus tokens dentro. `D-F8.4-B2-a` aparece en AMBOS frentes (8.4 y la casilla iv-D que la
  cierra), lo que refuerza la separación en dos commits: un fallo en el primero sería indistinguible
  de uno en el segundo.
  DECISIÓN DEL USUARIO (A frente a B) sobre las remisiones «bitácora Sxx (futura)»: (A) condensar
  igual conservando la marca, (B) condensar solo las casillas cuyas sesiones ya estén archivadas.
  ELEGIDA A: la marca ya está en el plan y no se inventa nada; B fragmentaba el criterio de S63 —que
  habla de frentes CERRADOS, no de sesiones archivadas— y garantizaba una cuarta visita a lo mismo.
  Quedan tres remisiones a futuro (S92, S93, S94), que se resuelven solas al rotar la ventana.
  ENTREGADO: frente 8.4 de 67 → 27 líneas (−40), frente 8.6-iv de 57 → 31 (−26), archivado de S91
  con las TRES rotaciones de M1-bis (nace S95, sale S91 PROMOVIDA a `### Sesión 91`, degrada S94) y
  los dos censos de la bitácora, la crónica y la frase de ventana actualizados. Plan 2630 → 2538
  líneas (−92, −3,5 %; la condensación quita 66 y el registro de esta sesión devuelve parte);
  bitácora 4067 → 4144 (+77 por S91). Estructura viva intacta: 8.6-B y 8.5-D3
  siguen ABIERTOS y sin tocar, cero tokens huérfanos, ninguna deuda reescrita.
  NO se tocó código, ni `modelo_datos_fase1.md`, ni `referencia-codigo-solver.md` (no hay `solver/
  src/main` en esta sesión). Suites NO ejecutadas: no procede, cero ficheros de código en el diff.
  LIMPIEZA EVALUADA PARA LA PRÓXIMA (M1.5): sin frentes cerrados acumulados. 8.6 quedará condensable
  cuando cierre 8.6-B, su único sub-bloque vivo; hasta entonces no hay nada que condensar y decirlo
  es la respuesta correcta (S81-S85 la descartaron cinco veces por este mismo motivo).
  Siguiente: 8.6-B (aviso durante el arrastre; ÚNICO bloque de frontend abierto, contrato ANTES de
  medir, orden inverso a M2, y solo defendible si la sesión se dedica a DISEÑO), D-F8.6-iiiA-b
  (`Totales` sin sede, con MOCKUP PREVIO por D-F8.6-a) o el punto (b) de D-F8.6-ivB-a (invariante del
  `<select>`, barato, cierra una deuda entera), a decidir al abrir.


### Sesión 96 — Fase 8, D-F8.6-ivB-a punto (b): el invariante del `<select>` (CIERRA la deuda ENTERA).
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

### Sesión 97 — Fase 8, Bloque 8.6-B: aviso de ocupación al iniciar el arrastre (CIERRA 8.6-B y el frente 8.6 ENTERO).
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

### Sesión 98 — Fase 8, HIGIENE DOCUMENTAL: condensación del frente 8.6 ENTERO y archivado de ventana.
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
  ENTREGADO (commiteado en el cierre de S103; en S102 el árbol quedó listo): NUEVOS `models/aula.model.ts` (espejo de
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

### Sesión 105 — Higiene documental (M1+R4/R5): salda el archivado M1-bis atrasado 4 sesiones. NO avanza el mapa.
  Tipo Higiene/Método: sin código, sin M2/M3/M4; solo M1 y verificación R4/costura. Elegida sobre el cierre de
  O-catálogo (candidato B) por el cierre de S104: prioritaria (4 sesiones de atraso), barata, y O-catálogo 4/4
  es punto de reposo natural del mapa para hacerla antes de abrir el frente de riesgo del e2e UI→solver.
  ESTADO MEDIDO AL ABRIR (M2 barato, greps): el plan arrastraba 9 sesiones vivas, no 6 —el grep de `### Sesión`
  daba 6 porque S96–S98 ya estaban en formato compacto y S99–S104 en `### Sesión`—. Ninguna de S96–S100 estaba
  aún en la bitácora (acababa en S95). Doble desfase confirmado: los dos censos de la bitácora decían «S95» y la
  crónica de archivado del plan se cortaba en «S95 en la Sesión 99».
  EJECUTADO: archivadas S96, S97, S98, S99 y S100 a la bitácora (promovidas a `### Sesión NN`, orden ascendente,
  cuerpo verificado IDÉNTICO por diff origen→destino); degradadas S101–S103 a prefijo compacto; S104 quedó como
  única cabecera H3. Corregidos los dos censos de la bitácora (→ S10–S100) y extendida la crónica de archivado
  del plan con la rotación S96–S100. Restaurada la invariante H3 (`grep -c "^### Sesión" plan` = 1).
  AVISO HEREDADO ATENDIDO (S103/S104): la bitácora (~370 KB) había corrompido cabeceras al archivar (títulos
  partidos en dos líneas: S32, S45, S46). Se archivó con Python (no sed) y se verificó por diff de cuerpo; cero
  cabeceras partidas nuevas. Las tres históricas ya partidas NO se tocaron (histórico de solo lectura).
  COSTURA: cada sesión S96–S100 en la bitácora 1 vez y 0 en el plan; S101–S104 al revés. Sin duplicados ni
  pérdidas. Balance de tamaño coherente (plan −42 KB, bitácora +42 KB).
  DEUDA: saldada la deuda de método M1-bis (archivado atrasado 4 sesiones). No nace deuda nueva.
  LIMPIEZA (M1.5): sin frentes cerrados acumulados que condensar; el registro queda limpio para abrir el cierre
  de O-catálogo. R4/costura verificada por grep+diff (el script oficial conviene correrlo en el entorno real).
  NOTA: el registro de esta S105 se añadió en un segundo paso; el cierre inicial lo omitió (fallo de M1 paso 1).
  O-catálogo (H2) sigue ACTIVO 4/4, NO cerrado: falta el paso UI→solver. Siguiente: cierre de O-catálogo (ver M1-ter).

### Sesión 106 — Cierre de O-catálogo por recorte de alcance (M0+M2+gestión): mide, cierra O-catálogo, reasigna e2e a O-estructura, instala andamiaje Playwright.
  Sexta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo MIXTO Desarrollo/Método: abrió como Desarrollo
  (cierre de O-catálogo vía el e2e UI→solver, candidato dominante del cierre de S105), pero su M2 midió
  que el objetivo estaba MAL ACOTADO y la sesión se convirtió en gestión: recorta el criterio de
  O-catálogo, lo cierra, reasigna el e2e a O-estructura y añade la regla R-e2e. Produjo además andamiaje
  Playwright (código real, commiteado). No avanza un Cambio de producto nuevo: corrige el mapa y cierra
  un objetivo.
  M0 — apertura verificada contra `gestion_proyecto.md`: Cambio = cierre de O-catálogo (2ª mitad del
  criterio agregado, el paso UI→solver); Objetivo = O-catálogo (H2); Hito = H2. R-invalidación sin
  conflicto (el e2e no lo rehace ningún objetivo posterior; O-demo lo hereda). AVISO DE ESTADO corregido
  al abrir: el prompt de apertura describía la ventana viva como S101–S104 / S104 única cabecera H3; el
  plan ya reflejaba la rotación de S105 (ventana S102–S105, S105 única H3). Discrepancia de prompt, no de
  registro; el registro estaba correcto.
  M2 — MEDICIÓN (Claude Code sobre el backend y el frontend REALES, tres investigaciones encadenadas antes
  de teclear nada de test). (1) Invocación del solver HOY: `POST /api/horarios` (`HorarioController`) NO
  recibe id de centro ni JSON —lee el catálogo íntegro de la BD vía `GeneradorHorarioService.cargarProblema()`
  (`findAll` sobre 11 repos)—; cuerpo `{}` válido, defaults maxSegundos=30/semilla=42/vía=OPTIMIZACION. El
  frontend ya lo llama (`horario.service.generar()` → POST con `{}`), gateado por prevalidación. Único
  e2e-navegador previo: NINGUNO (sin Playwright/Cypress ni carpeta e2e). (2) El "centro mínimo" que pasa
  prevalidación y produce solve son 9 FILAS IRREDUCIBLES (medido sobre `GenerarHorarioEndpointTest.
  poblarCatalogoMinimo` + las invariantes de `ActividadService`/dominio): Nivel, Grupo, Subgrupo, Profesor,
  Asignatura, Aula, ≥1 TramoSemanal lectivo, Actividad, Plaza. Las 3 reglas ERROR de `PrevalidacionService`
  (PROFESOR_SOBRECARGADO, REPETICIONES_EXCEDEN_DIAS, GRUPO_SOBRECARGADO) se satisfacen con holgura a
  1 tramo/1 repetición; el palomar de aulas no se prevalida (decisión S79, lo caza el solver → 422).
  (3) CRÍTICO — traducibilidad a UI: de las 9 filas, SOLO 4 tienen formulario (Profesor, Aula, Asignatura,
  Grupo). Las otras 5 no son clicables hoy: Nivel es bloqueo BLANDO (endpoint existe, la UI solo hace
  `listar()`, con BD limpia el desplegable de grupo sale vacío); TramoSemanal es bloqueo DURO (NO existe
  controller REST —sin rejilla `tramosLectivos=0` ⇒ PROFESOR/GRUPO SOBRECARGADO ⇒ 422; los tramos solo
  los crea `SeedCatalogoRunner` bajo `-Pseed`); Subgrupo, Actividad y Plaza (la demanda curricular) solo
  tienen API. Conclusión del M2: un e2e "centro creado íntegramente por la UI" es IMPOSIBLE hoy, y no por
  la herramienta sino porque O-catálogo no construye la mitad del centro mínimo. Esas piezas —currículo/
  demanda y jornada— son O-estructura por diseño (§3 O-estructura; §4 D22; frontera S103/S104).
  DECISIÓN DE GESTIÓN (aprobada por el arquitecto): el criterio agregado de O-catálogo se redactó (≤S104)
  sin haber medido esa dependencia. Se RECORTA a lo que O-catálogo realmente cierra —«las 4 entidades de
  catálogo se crean, listan, editan y borran desde la UI, y su escritura llega al backend REST»—, CUMPLIDO
  desde S104. O-catálogo pasa a ✔ TERMINADO. La verificación e2e «el solver corre sobre un centro creado
  íntegramente por UI» se REASIGNA a O-estructura, como parte de su criterio de terminado (es quien
  construirá Nivel/Subgrupo/demanda/jornada, y por tanto quien podrá ejecutarla de verdad; O-demo la
  hereda sobre el IES real). Cambio localizado en `gestion_proyecto.md`, previsto por §5 (decisión
  reversible de grano). Descartadas: e2e híbrido ahora (sembrar 5/9 por HTTP no prueba «la UI crea el
  centro», solo la disfraza) y expandir O-catálogo con los formularios que faltan (sería meter medio
  O-estructura dentro, viola la frontera de §4).
  R-e2e NUEVA (regla estratégica, `gestion_proyecto.md` §6): la suite e2e de navegador cubre ÚNICAMENTE los
  ~6 eslabones del guion de aceptación de §1 (crear→generar→ajustar→exportar→duplicar), uno por eslabón; la
  lógica se prueba en capa JVM/unidad (donde ya vive: `GenerarHorarioEndpointTest`, ~35 de `SolverHorario`,
  round-trips, MockMvc) o vitest, NUNCA en navegador. Un e2e nuevo se justifica solo si cubre un eslabón no
  cubierto (análoga a R-deuda). Motivada por el riesgo —planteado por el arquitecto— de que el coste de la
  suite e2e supere al del desarrollo si crece sin techo.
  ENTREGADO — andamiaje Playwright (commit `test(e2e): instala andamiaje Playwright con humo verde en
  app/frontend/e2e`): @playwright/test 1.62.0 (solo chromium), `app/frontend/playwright.config.ts`
  (`webServer` con dos servidores: backend `mvn -pl app spring-boot:run` sin perfil seed —schema.sql hace
  drop+create, BD del e2e = `app/educhronos.db`, aislada de la raíz—, frontend `npm start`; readiness por
  `/api/prevalidacion` y `:4200`), `app/frontend/e2e/humo.spec.ts` (1 test: la landing carga, selector
  `getByText('Elige por dónde empezar.')` —exclusivo de Landing, no del header del shell—). MODIFICADOS
  `package.json` (+script `e2e`, +devDep), `package-lock.json`, `.gitignore` del frontend (+4 patrones de
  artefactos runtime de Playwright). Humo VERDE (1 passed, 11.1s); backend arranca con BD vacía sin error
  (`GET /api/prevalidacion` → 200 `[]`). Suite vitest INTACTA 163/163 (Playwright no la toca: `testDir ./e2e`
  queda fuera de `tsconfig.spec.json`); `npm run build` verde. Dos correcciones obligadas por el repo,
  medidas por Claude Code: el command de backend es `mvn -pl app spring-boot:run` (el pom raíz es agregador
  sin main class) y la BD limpia es automática (schema.sql), con el filo de que un backend de dev vivo en
  `:8080` sería REUTILIZADO por `reuseExistingServer` en local —no correr el e2e con el backend de trabajo
  levantado—.
  COMMITS (dos, código y doc separados, de una línea): `docs(gestion): recorta O-catálogo a CRUD por UI
  (TERMINADO S104), reasigna e2e UI→solver a O-estructura y añade R-e2e`; `test(e2e): instala andamiaje
  Playwright con humo verde en app/frontend/e2e`. La BD de prueba (`app/educhronos.db`) confirmada IGNORADA
  (no entra en el commit).
  DEUDA: no nace deuda nueva. El andamiaje Playwright NO es deuda —es trabajo que O-estructura hereda por
  criterio explícito—. R-terminado respetada: no se pulió nada de O-catálogo más allá de su criterio (de
  hecho se recortó). El e2e reasignado no es deuda: es criterio de terminado de otro objetivo.
  LIMPIEZA (M1.5): sin frentes cerrados acumulados que condensar; el recorte de O-catálogo se documentó
  en su sitio (`gestion_proyecto.md` §2/§3/§6), no en el plan. R4/costura: los dos censos de la bitácora,
  la crónica de archivado y la frase de ventana actualizados por la rotación de S102 (ver M1-bis abajo);
  script oficial de R4 sigue sin existir en el repo (mejora de método pendiente desde S101), verificado a
  mano.
  O-catálogo (H2) ✔ TERMINADO. Desbloquea O-estructura (siguiente objetivo por dependencias). Siguiente:
  abrir O-estructura (ver M1-ter).

### Sesión 107 — O-estructura (H2): C-jornada, primer Cambio. Backend REST de jornada (Desarrollo) + formulario singleton (Config/UI). Abre O-estructura; NO lo cierra.
  Séptima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo MIXTO: M0 largo (descomposición de O-estructura,
  su primer M2 por diseño) + un Cambio completo, C-jornada, de tipo Desarrollo (backend con lógica) y
  Configuración/UI (formulario). PRIMER Cambio de O-estructura: lo ABRE, no lo cierra. O-estructura sigue
  ACTIVO — su criterio (8 tipos de sesión configurables + casos de validación §6 del modelo + e2e UI→solver
  heredado) exige aún currículo/Actividades, desdobles, PDC, tutores y el e2e. C-jornada entrega UNA pieza:
  la dimensión temporal, que era el prerequisito del solve por camino crítico (sin ≥1 TramoSemanal lectivo
  el centro mínimo da 422; ninguna otra UI de estructura desbloquea el solve sin ella).
  M0 — apertura verificada contra `gestion_proyecto.md`: Objetivo = O-estructura (H2), candidato dominante
  por dependencias (O-catálogo ✔ S106; O-estructura `depende de O-catálogo`). Hito = H2. Cambio = no
  prenombrado en la doc: el primer M2 de O-estructura ES descomponerlo, y así se hizo. R-invalidación sin
  conflicto (nada por encima lo rehace: O-diseño depende de H2 cerrado, O-demo de O-estructura).
  M2 — MEDICIÓN (Claude Code sobre backend y frontend REALES, cinco investigaciones encadenadas antes de
  teclear). (1) Censo REST: 11 controllers; de las piezas de estructura, Nivel/Subgrupo/Actividad ya tienen
  CRUD, Plaza viaja embebida en el agregado Actividad (por diseño), y SOLO la jornada/tramos carece de
  puerta REST —cero controllers—. Corrige el inventario declarado de O-catálogo (decía Subgrupo/Actividad
  «solo API» y Nivel «solo listar»: desactualizado). `DemandaCurricular`/`Particion` NO existen como clases
  en el repo (divergencia modelo-código, registrada). (2) Jornada es el camino crítico del solve y es
  temporalmente AGNÓSTICA respecto a Actividad (`ActividadRequest` no referencia tramos): un CRUD de jornada
  no toca Actividad. (3) El hueco real NO es «falta un CRUD»: la clave pública del tramo es posicional y
  derivada (`(dia, ordenEnDia)`, no persistida, renumerada por `CatalogoMapper.renumerarLectivos`, invertida
  en 3 servicios); editar la jornada renumeraría en silencio los tramos e invalidaría bloqueos/restricciones
  por FK. (4) Techo de dominio DURO: `domain.Tramo` valida `diaSemana 1..5`, `ordenEnDia 1..6` en compact
  constructor → 7 tramos o sábado revientan al montar el problema. (5) El seed puebla 8 repos, todos con
  puerta salvo TramoSemanal; cero tests atados a `-Pseed` → borrable si M3 entrega el CRUD.
  DECISIONES DE PRODUCTO (del arquitecto, tras medir): jornada FIJA durante el curso (sin edición en caliente
  tramo-a-tramo) ⇒ el nudo de renumeración se ESQUIVA por diseño, no se resuelve. Reemplazo TOTAL con guarda
  409 (si hay cualquier dependiente, el PUT se rechaza; el usuario borra horarios/restricciones y reconfigura;
  reconciliar-por-id descartado, reintroducía la pregunta de producto ya cerrada). Horas reales visibles.
  Un recreo, posición elegible. Malla idéntica los 5 días. Techo conservador: la UI topa ≤6 lectivos/día, el
  dominio NO se toca (M3 conservador; levantar el rango sería tocar el solver, fuera de alcance).
  M3 — ENTREGADO (Desarrollo, backend). `TramoSemanalRepository` (3 @Query agregadas de conteo de FK
  entrantes + listado + deleteAll), `JornadaService` (validación pre-BD: techo, horas coherentes sin solape,
  días; guarda 409 con desglose `Referencia(referente,conteo)` molde `GrupoService.borrar`; reemplazo
  deleteAll→flush()→insert molde `AsignaturaService`), `JornadaController` (GET|PUT `/api/jornada` singleton,
  400/409, sin 404), DTOs `JornadaDTO`/`TramoJornadaDTO`/`JornadaRequest`/`TramoJornadaRequest`. GET sintetiza
  la malla de referencia (la que sabía el seed) con flag `persistida=false` cuando la tabla está vacía
  (propuesta EFÍMERA, no sembrada: evita jornada-fantasma). `siguienteInmediato` = null (NO derivado: tocar
  esa FK es semántica del solver, invariante S6, fuera de alcance — frenado 2 veces, deuda registrada).
  Retirado `SeedCatalogoRunner`. DOS RETOQUES de M3: (a) records anidados → ficheros sueltos (alinear con el
  precedente del repo, PlazaRequest/AulaPinDTO; suite intacta 351); (b) frontera del PUT movida a «un día
  tipo, el backend expande a 5 días y numera» (evita un segundo generador de malla en frontend, mantiene la
  lógica de numeración testada en JVM; suite 351→350 —dos casos de error dejaron de ser representables al
  salir `dia` del contrato, +1 test de expansión con `orden` continuo cruzando el día; no es pérdida de
  cobertura). Suite app 242→259, solver 91 intacto, `domain.Tramo`/Actividad/solver sin tocar. Commit:
  `feat(app): endpoint de jornada REST singleton con reemplazo total y guarda de dependientes (retira SeedCatalogoRunner)`.
  M4 — ENTREGADO (Configuración/UI, frontend). `jornada.model.ts` (PRIMER modelo del repo con DTO≠Request
  divergentes de verdad), `jornada.service.ts` (wrappers pelados), componente `jornada` SINGLETON (no par
  lista/form): GET al init → filtra al primer día → FormArray editable (FIJA: sin añadir/quitar filas, el nº
  de tramos lo fija la propuesta del GET, no el frontend); recorta a día tipo antes del PUT. 409 y 400 son
  caminos DISTINTOS: 400 inline («corrige la malla»), 409 aviso rojo + solo lectura («borra tus horarios»,
  con desglose). Confirmación previa al PUT destructivo. TRES desviaciones del molde, todas justificadas:
  (a) `ConfirmarReemplazo` nuevo (reutilizar `ConfirmarGeneracion` obligaba a título mentiroso; generalizarlo
  tocaba horario-view/H1-cerrado); (b) botón «Volver a cargar la jornada» en el aviso 409 —corrige un
  callejón sin salida del diseño del arquitecto: sin él, salir de solo-lectura exigía recargar el navegador—;
  (c) FormArray estrenado en el repo (necesita FormGroup contenedor; `disable()/enable()` da solo-lectura y
  `getRawValue()` lee los deshabilitados). Control `esRecreo` (polaridad de la columna del usuario), invertido
  a `esLectivo` al salir. FormArray ESTRENA mecanismo (nota de patrón para el próximo componente con listas
  de campos). Suite vitest 163→180 (30 ficheros), `ng build` limpio; horario-view y backend sin tocar; helper
  `mensaje()` copiado, no extraído (D-F8.6/H1). Commit:
  `feat(frontend): configura la jornada del centro por UI (formulario singleton de tramos con horas y recreo)`.
  DEUDA — nace deuda registrada (R-deuda: ninguna bloquea el criterio de C-jornada, no se pagan ahora):
  (1) D-jornada-msg409: el mensaje del 409 empieza «No se puede borrar: referenciada por…» (heredado de
  `ReferenciaEntranteException`, escrita para DELETE de catálogo) y el usuario lo lee al GUARDAR jornada.
  Cosmético; se corrige con un mensaje propio del caso PUT cuando O-estructura vuelva a tocar el backend de
  jornada. Cuelga de O-estructura. (2) D-jornada-asimetria: el contrato GET(35 tramos)≠PUT(7, día tipo);
  nota de diseño de API, no bloquea (la UI convive sin fricción real: pinta y manda un día). (3)
  D-jornada-flush-test: `put_dosVecesLaMismaMalla_idempotente` no discrimina el `flush()` porque falta
  `UNIQUE(dia,orden)` en schema.sql; el flush es defensivo/preventivo (correcto), su test es de contrato.
  Arrastradas ya registradas: `siguienteInmediato` sin derivar (O-estructura), `Configuracion` tabla
  huérfana, divergencia modelo-código `DemandaCurricular`/`Particion` (la afronta el Cambio de currículo).
  R-terminado RESPETADA: no se pulió fuera de criterio (frenado `siguienteInmediato` ×2, descartado el
  constructor-por-parámetros de la UI, NO reabierto M3 por el mensaje cosmético del 409).
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el
  repo (mejora de método pendiente desde S101); verificado a mano que los dos commits de código separan
  backend/frontend y que `app/educhronos.db` sigue ignorada.
  O-estructura (H2) ABIERTO, 1 pieza (jornada) de varias. Siguiente: segundo Cambio de O-estructura, probable
  currículo/Actividades —backend CRUD ya existe—, pero lo fija su propio M0 (ver M1-ter).

### Sesión 108 — O-estructura (H2): C-subgrupos, segundo Cambio. CRUD de subgrupos por UI (Config/UI, M3 en el multiselect). Incluye higiene M1-bis (archiva S103–S106). NO cierra el objetivo.
  Octava sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI con M3 real (el multiselect de
  grupos), más higiene documental M1-bis al cierre. SEGUNDO Cambio de O-estructura: lo AVANZA, no lo cierra.
  O-estructura sigue ACTIVO —su criterio (8 tipos de sesión configurables + casos de validación §6 del modelo
  + e2e UI→solver heredado) exige aún currículo/Actividades (el editor de plazas), desdobles, PDC, tutores y
  el e2e—. C-subgrupos entrega la pieza que faltaba para que las Plazas de una Actividad puedan referenciar
  subgrupos creados por UI: sin subgrupos creables, el editor de actividades (siguiente Cambio) no tendría a
  qué apuntar.
  M0 — apertura verificada contra `gestion_proyecto.md`: Objetivo = O-estructura (H2), candidato dominante por
  dependencias (O-catálogo ✔ S106; O-estructura activo desde S107). Hito = H2. Cambio = currículo, no
  prenombrado en la doc: el M2 lo acotó a subgrupos (ver abajo). R-invalidación sin conflicto (nada por encima
  lo rehace: O-diseño depende de H2 cerrado, O-demo de O-estructura). DECISIÓN DE ALCANCE del arquitecto tras
  el M2: Opción A (currículo = subgrupos + actividades sin materializar Particion), y primer Cambio = subgrupos
  (dependencia previa de actividades). Particion NO se materializa: el solver no la consume (javadoc de
  `Subgrupo`, decisión D-a S48; el JSON del solver no transporta particiones) y expresar el §6.1 no la exige;
  materializarla sería modelo que nadie lee aguas abajo. Reversible si la UX de actividades la reclama.
  M2 — MEDICIÓN (Claude Code sobre backend y frontend REALES, dos investigaciones encadenadas antes de teclear).
  (1) Currículo persistido: `Subgrupo` (@Entity, tabla `subgrupo` + `subgrupo_grupo` como @JoinTable @ManyToMany,
  repo, servicio con validación I6 ≥1 grupo, controller `/api/subgrupos` CRUD 5-endpoints, tests) y `Actividad`
  (agregado con `Plaza` embebida, cascade+orphanRemoval, controller `/api/actividades`) YA EXISTEN completos en
  backend. `Particion`/`SubgrupoParticion`/`DemandaCurricular` NO existen como clase, tabla ni repo —Particion
  por decisión registrada (S48), DemandaCurricular solo vive en el doc de modelo (0 apariciones en código)—.
  Corrige mi apuesta de apertura: NO falta backend de currículo, falta UI. (2) La Plaza referencia subgrupos por
  CÓDIGO de subgrupos ya existentes (400 si no resuelve, no alta implícita) ⇒ los subgrupos deben ser creables
  ANTES que las actividades: dependencia técnica que fija el orden de los dos Cambios. (3) Frontend a cero: ni
  `subgrupo.model.ts` ni `subgrupo.service.ts` ni `components/subgrupos/`; `app.routes.ts` con 3 rutas, CRUD
  inline en `Configuracion`. Tipo de sesión que dicta el M2: Config/UI (backend hecho), con M3 en la única
  desviación real —el multiselect—.
  DESVIACIÓN DEL MOLDE (el M3): el campo `grupos` es selección MÚLTIPLE, no el `<select>` único de `nivel` en
  GrupoForm. Tres consecuencias medidas: (a) control `FormControl<string[]>`, no string; (b) `Validators.required`
  da por válido un array vacío ⇒ validator propio `arrayNoVacio` que replica I6 en cliente; (c) el `<select
  multiple>` de Reactive Forms NO vincula el array por `formControlName` ni reconcilia la preselección como el
  `<select>` único: se lee `selectedOptions` en un handler `(change)`→`setValue` y el HTML refleja la selección
  con `[selected]` por opción. Decisión de producto del arquitecto: `<select multiple>` nativo (mínima desviación
  del canon), con la UX rica (chips, buscar) APLAZADA a una fase posterior de mejora de UX de subgrupos —mejora
  planificada, no deuda técnica (ver D-subgrupo-ux-multiselect)—. Riesgo despejado en fase 2: jsdom refleja
  `[selected]`/`selectedOptions` como un navegador; la cadena handler→control→body pasa sin adaptación.
  M4 — ENTREGADO (Configuración/UI, frontend, 4 fases con verificación entre cada una, todas verdes). FASE 1
  (commit `b5d11fa`): `models/subgrupo.model.ts` (par `Subgrupo`/`SubgrupoRequest`, simétricos salvo `id`, molde
  de `grupo.model` no `nivel.model` porque subgrupo escribe), `services/subgrupo.service.{ts,spec.ts}` (5 wrappers
  pelados sobre `/api/subgrupos`, spec de contrato 5 casos). FASE 2 (commit `6846c3e`): `components/subgrupos/
  subgrupo-form.{ts,html,css,spec.ts}` —el multiselect, `arrayNoVacio`, handler `alSeleccionar`, precarga en
  edición reflejada con `[selected]`; spec 12 casos con la mezcla A1 (DOM real para poblado/preselección de DOS
  grupos, evita verde falso) + A2 (`setValue` para las ramas de guardado); `.subgrupo-form__multiple` con
  `min-height` para que el `<select multiple>` no colapse—. FASE 3 (commit `53b46ac`): `subgrupo-lista.
  {ts,html,css,spec.ts}` —molde plano de lista; única desviación la columna «Grupos» pintada `grupos.join(', ')`;
  spec 8 casos, hereda de aula-lista el caso de recarga tras guardado (7) y con-qué-se-abre-el-diálogo (8)—.
  FASE 4 (commit `29f0f84`): cablea `SubgrupoLista` en `Configuracion` (+import, +`imports:`, +`<app-subgrupo-lista
  />` tras grupo) y su caso (6) en `configuracion.spec.ts` (+doble de `SubgrupoService`). Runner del proyecto:
  `npx ng test` (no `npx vitest run`, que rompe con «describe is not defined»: el builder de Angular 21 inyecta
  los globals). Suite frontend 180 → 206 (+26: 5+12+8+1), 30 → 33 ficheros; `ng build` limpio (bundle 469→478 kB,
  +8.2 kB = la cadena de subgrupo entra en producción, antes fuera por tree-shaking); backend sin tocar. La
  sección es visible y usable en `/configuracion`, bajo Grupos.
  DEUDA — nace una, registrada (R-deuda: no bloquea el criterio de C-subgrupos, no se paga ahora):
  D-subgrupo-ux-multiselect (S108, VIVA, MEJORA PLANIFICADA no bloqueante) — el `<select multiple>` nativo es UX
  pobre para elegir varios grupos (Ctrl+click no obvio, sin buscar ni chips). Es DECISIÓN CONSCIENTE del
  arquitecto con fase futura ya prevista, no descuido: cuelga de O-estructura y se aborda en la fase de mejora de
  UX de subgrupos. NO es deuda técnica (el componente funciona y está testado); es acabado de presentación
  aplazado, hermano de lo que O-diseño trata a nivel transversal. Arrastradas ya registradas y sin cambio:
  D-jornada-msg409, D-jornada-asimetria, D-jornada-flush-test (S107, O-estructura); `siguienteInmediato` sin
  derivar; `Configuracion` tabla huérfana; divergencia modelo-código `DemandaCurricular`/`Particion` —el M2 de
  S108 la MIDIÓ (Particion decisión S48, DemandaCurricular solo en doc) y confirmó que NO bloquea el currículo:
  se construye entero sin ella; lo único que no habría sin DemandaCurricular es la validación de cobertura de
  horas, función pendiente, no bloqueante—.
  R-terminado RESPETADA: no se pulió fuera de criterio. La UX del multiselect se FRENÓ a deuda planificada en vez
  de resolverla dentro del Cambio; Particion se descartó por medición, no se materializó «ya que estábamos».
  HIGIENE (M1-bis, Opción 2 elegida por el arquitecto para dejar la doc limpia): archivadas S103, S104, S105 y
  S106 a `bitacora-sesiones.md` (promovidas a `### Sesión NN`, orden ascendente tras S102, cuerpo verificado
  idéntico por diff origen→destino); degradada S107 a «Última sesión previa» compacta; S108 queda como única
  cabecera H3 viva. Corregidos los dos censos de la bitácora (→ S10–S106) y extendida la crónica de archivado del
  plan. Restaurada la invariante H3 (`grep -c "^### Sesión" plan` = 1). Salda la deuda de método M1-bis que venía
  atrasada desde S106. Aviso heredado atendido: la bitácora (~370 KB) había corrompido cabeceras al archivar en
  el pasado; se archivó con Python y se verificó por diff de cuerpo, cero cabeceras partidas nuevas.
  R4/costura: script oficial sigue sin existir en el repo (mejora de método pendiente desde S101); verificado a
  mano que los cuatro commits de código separan por fase y que `app/educhronos.db` sigue ignorada.
  O-estructura (H2) ACTIVO, 2 piezas hechas (jornada S107, subgrupos S108) de varias. Siguiente: tercer Cambio de
  O-estructura, C-actividades —el editor de Actividad con Plazas embebidas, backend `/api/actividades` ya existe;
  es el formulario de mayor riesgo del objetivo (valida si Actividad→Plaza→Subgrupo es configurable por un
  humano), ahora desbloqueado porque los subgrupos ya son creables por UI—. Lo fija su propio M0 (ver M1-ter).

### Sesión 109 — O-estructura (H2): C-actividades, tercer Cambio, entra TROCEADO (trozo A). Suelo de persistencia + guarda 409 del PUT (Desarrollo) + editor de Actividad de una plaza (Config/UI, M3 en XOR y multiselects). NO cierra el objetivo.
  Novena sesión bajo el mapa Hito→Objetivo→Cambio. Tipo MIXTO: Desarrollo (fases 0 y 1, backend con lógica)
  + Configuración/UI con M3 real (fase 2, el XOR de aula y los tres multiselects). TERCER Cambio de
  O-estructura: lo AVANZA, no lo cierra. O-estructura sigue ACTIVO —su criterio (8 tipos de sesión
  configurables + casos de validación §6 del modelo + e2e UI→solver heredado) exige aún el trozo B de
  actividades, PDC, tutores, el CRUD de Nivel por UI (hueco descubierto en esta sesión) y el e2e—.
  M0 — apertura verificada contra `gestion_proyecto.md`: Objetivo = O-estructura (H2), ACTIVO desde S107 con
  2 piezas hechas (C-jornada S107, C-subgrupos S108). Hito = H2. Cambio = C-actividades, nombrado por el
  M1-ter de S108 y correspondiente al «editor de demanda curricular» de §3. R-invalidación sin conflicto
  (O-diseño depende de H2 cerrado, O-demo de O-estructura). R-deuda: ninguna deuda abre la sesión.
  DOS AVISOS levantados en el propio M0 y confirmados después por medición: (1) posible hueco de Nivel sin UI
  con el seed ya retirado; (2) hipótesis de que «desdobles/agrupamientos» no es un Cambio propio sino
  contenido de una Actividad multiplaza.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL, dos investigaciones encadenadas antes de teclear; informe
  íntegro consumido y volcado aquí, el fichero suelto `informe-m2-s109.md` NO se commiteó).
  (1) CONTRATO: `ActividadRequest(codigo, asignatura?, duracionTramos, repeticionesPorSemana, patronTemporal,
  requiereTutor, plazas[])` y `PlazaRequest(asignatura, aulaFija, aulasCandidatas[], profesores[],
  subgrupos[])`, todas las referencias por CÓDIGO, `patronTemporal` como String por el patrón de borde D-3.
  `PlazaRequest` SIN id y SIN codigo a propósito: el código de plaza lo deriva el servicio como
  `{codigoActividad}-P{n}` y es inestable entre ediciones. 21 validaciones en `ActividadService`, todas
  medidas con su mensaje literal y su HTTP. 26 tests en `ActividadEndpointTest` = catálogo de lo que el
  formulario debe poder producir. (2) HUECO REAL DEL CONTRATO: NO existe validación de mínimo de subgrupos —
  una plaza con cero subgrupos devuelve 201—. (3) `roundTrip_bloqueSeisPlazas` CONFIRMA el aviso (2) del M0:
  un desdoble/agrupamiento ES una actividad multiplaza, no una entidad ni un campo `tipo` (§4.6 del modelo lo
  dice explícitamente). (4) HUECO DE NIVEL CONFIRMADO: sin seed, sin `data.sql`, sin migración, sin runner y
  sin `insert` en `schema.sql`; `nivel.service.ts` solo tiene `listar()`; no existe `components/niveles/`.
  Con BD vacía NO se puede crear un Grupo por UI ⇒ ni Subgrupo ⇒ las plazas se quedan sin población. Bloquea
  el e2e, que es la tercera pata del criterio de O-estructura. (5) Los cuatro listados que el formulario
  necesita (asignaturas, profesores, aulas, subgrupos) ya existen con cliente Angular; no hace falta ningún
  endpoint nuevo. `aulas-compatibles` devuelve TIPOS de aula, no aulas, y no está cableado. (6) No existe
  precedente de `FormArray` con alta/baja de filas: el único del repo (`jornada`) es de longitud FIJA.
  MEDICIÓN EN SEGUNDA VUELTA (tres comprobaciones pedidas tras el primer informe, dos de lectura y una de
  ejecución): (7) el `PUT /api/actividades/{id}` NO tenía guarda ante dependientes —los tres conteos y el 409
  solo estaban en `borrar`—; (8) `Sesion` y `AulaBloqueada` referencian a `Plaza` POR ID DE FILA
  (`sesion.plaza_id`, `aula_bloqueada.plaza_id`, ambas RESTRICT), nunca por código; (9) MEDIDO EN EJECUCIÓN:
  un nivel creado por API sobrevivía al apagado y DESAPARECÍA en el siguiente arranque — la aplicación vaciaba
  la BD en cada inicio.
  DECISIONES DEL ARQUITECTO (tras medir): (a) el corte de C-actividades es en DOS trozos, no tres: trozo A =
  cadena completa (modelo, servicio, lista, formulario) con `FormArray` de longitud FIJA 1 desde el principio,
  para que el trozo B sea un delta y no una reescritura; trozo B = abrir el array + I2 en cliente. Se descartó
  el corte «lista sola primero»: una lista de actividades sin formulario muestra una tabla permanentemente
  vacía, no es valor utilizable, y rompe el molde del repo (lista y form se han entregado siempre juntos).
  (b) RECONCILIACIÓN POSICIONAL DEL PUT: primero se aceptó y luego se REVOCÓ al medir (8). El razonamiento
  inicial —«el código de plaza es interno, nada externo empareja por él»— era cierto e irrelevante: los
  dependientes emparejan por id de fila, y la reconciliación MUTA las filas vivas conservando id, así que
  eliminar una plaza intermedia deja una `Sesion` describiendo una plaza cuyo contenido ha cambiado, sin error
  ni aviso. Se aplica el precedente de C-jornada (S107): guarda 409 ante cualquier dependiente. Reconciliar por
  identidad (añadir `id` a `PlazaRequest`) DESCARTADO: más trabajo, reabre una pregunta de producto ya cerrada
  y seguiría necesitando la guarda por las FK RESTRICT. (c) plaza con cero subgrupos: el formulario REFLEJA el
  contrato y NO añade un validador que el backend no tiene (deuda registrada). (d) no se filtran las aulas por
  I3 en cliente: habla el 400 del backend, que ya nombra asignatura, aula, tipo y tipos compatibles. (e)
  C-niveles FUERA de esta sesión: se registra como Cambio de O-estructura y se ejecuta pegado al e2e, que es
  quien lo necesita; los niveles de prueba se crean por `curl`.
  FASE 0 — SUELO DE PERSISTENCIA (Desarrollo). `schema.sql` deja de ser idempotente POR DEMOLICIÓN y pasa a
  serlo por `if not exists`: eliminadas las 21 sentencias `drop table if exists`, las 21 `create table` pasan a
  `create table if not exists`, cuerpos idénticos byte a byte y en el mismo orden (verificado por diff de los
  21 cuerpos). Cero índices/vistas/triggers en el fichero, así que no había más DDL que convertir. Corregidos
  los comentarios de `schema.sql` y de `application.properties` que describían la demolición.
  `spring.sql.init.mode=always` se CONSERVA (SQLite no se detecta como BD embebida; `never` dejaría sin esquema
  una instalación limpia). PRECIO CONSCIENTE: la demolición era también el mecanismo de migración de facto; sin
  ella un cambio de esquema deja de aplicarse solo (borrar el `.db` en desarrollo; la migración real es asunto
  de H4). Verificado en EJECUCIÓN: dato creado → reinicio → sigue ahí; y con el `.db` apartado, la aplicación
  levanta y crea el esquema desde cero. Suite verde a la primera (app 259, solver 91): el riesgo anunciado de
  contaminación entre tests por dejar de dropear NO se materializó (`@DataJpaTest` aísla por transacción).
  Commit: `fix(app): schema.sql deja de dropear las tablas en cada arranque, la BD ya no se vacia al iniciar`
  (`d301b64`).
  FASE 1 — GUARDA 409 EN EL PUT DE ACTIVIDAD (Desarrollo, M3 real). La comprobación de dependientes que vivía
  inline en `borrar` se EXTRAE a `exigirSinDependientes(id, accion)` y se llama también desde `editar`,
  INMEDIATAMENTE tras el `findById` que da el 404 y ANTES de cualquier validación: si la actividad no se puede
  editar, ningún arreglo del cuerpo lo cambia, luego el 409 domina sobre el 400 y el 404 domina sobre todo.
  `ReferenciaEntranteException` llevaba el verbo «borrar» HARDCODEADO en el mensaje: se parametriza la acción
  (ctor de un argumento delega en «borrar», mensaje del DELETE intacto y sus tests verdes; ctor de dos toma el
  verbo), y el PUT emite «No se puede editar: referenciada por…». HUBO QUE TOCAR EL CONTROLADOR: el 409 estaba
  cableado SOLO en `borrar` mediante try/catch (no hay `@ControllerAdvice`, convención de S74), así que en el
  PUT la excepción habría escapado como 500. Reescritos los javadoc de `editar` y de `reconciliarPlazas`: el
  emparejamiento posicional deja de ser «decisión de UX provisional a confirmar en Fase 8.6» y pasa a ser
  decisión tomada, segura PORQUE la guarda impide que existan referentes cuando se reconcilia. Guarda ROMA a
  propósito (rechaza también un renombrado inocuo), igual que la de jornada. Suite app 259→261. Commit:
  `feat(app): el PUT de actividad rechaza con 409 si tiene horario o bloqueos colgando` (`0e716c1`).
  VERIFICACIÓN POR MUTACIÓN de la fase 1 (contraste de Claude Code, no afirmado): quitar la guarda de `editar`
  → caen los dos tests nuevos (y el PUT «inocuo» sobre actividad con travesía devolvía 200 mutando la plaza
  referenciada: el agujero era real, no teórico); mover la guarda al final de las validaciones → cae SOLO el
  test de orden, que es el que demuestra que la precedencia está protegida; guarda contando solo sesiones →
  caen dos por el desglose, luego el aserto no es laxo. Desviación consciente y aceptada: no se escribió test
  nuevo de PUT sobre id inexistente porque `edicion_inexistente_404` ya lo es, y ese caso NO discrimina la
  posición de la guarda (un id inexistente no tiene dependientes: los conteos darían 0 y el 404 saldría igual).
  FASE 2 — C-ACTIVIDADES TROZO A (Configuración/UI, M3 en XOR y multiselects). 10 ficheros nuevos:
  `models/actividad.model.ts` (4 interfaces simétricas a los records Java + `PATRONES_TEMPORALES`),
  `services/actividad.service.{ts,spec.ts}` (5 wrappers pelados, molde `subgrupo.service.ts`),
  `components/actividades/actividad-lista.{ts,html,css,spec.ts}` y `actividad-form.{ts,html,css,spec.ts}`.
  Cabecera con asignatura OPCIONAL (opción vacía → null) y patrón como `<select>` de tres valores. La plaza
  nace DENTRO de un `FormArray` de longitud fija 1 (molde `jornada`), sin botones de alta/baja: eso es el
  trozo B. XOR resuelto con un control de UI `modoAula` ('FIJA'|'CANDIDATAS') que NO viaja al backend, decide
  qué rama valida y limpia la contraria al cambiar; en edición se DERIVA del dato (`aulaFija != null`). Tres
  multiselects molde `subgrupo-form`: profesores con `arrayNoVacio` (I7), candidatas con `arrayNoVacio` solo
  en su rama, subgrupos SIN validador (decisión (c)). GUARDA DE MULTIPLAZA en la lista, que es la pieza que
  hace honesto entregar el trozo A por separado: si `plazas.length > 1` el botón de editar va deshabilitado Y
  el método `editar()` no abre el diálogo (dos capas), porque un formulario de una plaza que abriera una
  actividad de seis enviaría una plaza y la reconciliación posicional BORRARÍA las otras cinco. El borrado sí
  se permite en multiplaza (es íntegro). Cableado en Configuración con una línea en `imports:` y una etiqueta,
  sin tocar los hermanos. Suite vitest 206→239 (+33: servicio 5, lista 11, formulario 16, configuración 1);
  backend intacto 261/91. Commits: `feat(frontend): contrato de actividad, modelo y servicio sobre
  /api/actividades` (`e0e41fe`), `feat(frontend): lista y formulario de actividad de una plaza con XOR de aula
  y multiselects` (`9ef160f`), `feat(frontend): monta la lista de actividades en la seccion de Configuracion`
  (`85f93e2`).
  VERIFICACIÓN POR MUTACIÓN de la fase 2 (siete mutaciones, todas tumbaron tests): quitar `arrayNoVacio` de
  profesores; enviar siempre ambas ramas del XOR; no limpiar la rama anterior al cambiar de modo; AÑADIR
  `arrayNoVacio` a subgrupos (cae el caso que demuestra que cero subgrupos es aceptable: la decisión (c) está
  medida, no solo escrita); quitar la guarda de multiplaza (caen DOS, una por capa); `ngOnInit` deja de pedir
  `/api/aulas` (caen los 16 del formulario ⇒ el helper de montaje flushea bien las CUATRO peticiones de red).
  La séptima (perder `requiereTutor` en el PUT) la ataja el TIPO antes de compilar —cobertura del compilador,
  no del spec, mismo fenómeno que el NG8001 de S102—; la variante que sí compila cae en un test.
  M4 — VERIFICACIÓN MECÁNICA conducida con Playwright EFÍMERO (script fuera del repo, en /tmp, borrado al
  terminar y árbol verificado limpio: la suite e2e permanente es el Cambio que CIERRA O-estructura, no
  andamiaje de una sesión; R-e2e respetada). 18 puntos, 17 PASAN. Lo verificado que importa: el XOR va y
  vuelve con `aulaFija: null` EXACTO (no cadena vacía) y el `<select multiple>` de candidatas reconcilia el
  array al reabrir —el punto que costó trabajo en S108—; ningún cuerpo lleva la clave `modoAula`; el clic
  FORZADO sobre el editar deshabilitado no abre el formulario ni emite petición (las dos capas sostienen); sin
  profesor NO sale ninguna petición; sin subgrupos SÍ sale y responde 200; y el dato sobrevive al reinicio del
  backend, que hasta esta sesión era imposible. `repeticiones = 0` lo corta el cliente (`Validators.min(1)`),
  así que la validación equivalente del backend queda como segunda línea inalcanzable desde la UI.
  VERIFICACIÓN DE USABILIDAD (la única parte no delegable, porque es la pregunta de riesgo de O-estructura):
  el arquitecto define «plaza» y entiende duración/repeticiones sin ayuda, y considera soportables los
  multiselects y útil el aviso de multiplaza. DOS IMPRECISIONES en su definición que señalan dónde la pantalla
  calla: dijo «grupo» donde el modelo dice SUBGRUPO, y situó el tramo en la Plaza cuando el tiempo lo pone la
  Actividad. CAVEAT REGISTRADO: probar la usabilidad con el AUTOR del modelo es evidencia débil; la pregunta
  «¿es configurable por un humano?» no se cierra aquí, se cierra en O-demo con el IES real. Lo que sí queda
  demostrado es que el modelo SE PUEDE EXPRESAR por formulario sin contorsiones.
  DEUDA — nace deuda registrada (R-deuda: ninguna bloquea el criterio del trozo A, no se pagan ahora):
  (1) D-plaza-sin-subgrupos (TÉCNICA REAL, no bloqueante): el backend acepta una plaza con cero subgrupos y
  devuelve 201; una plaza sin población es una sesión a la que no asiste nadie. Cuelga de O-estructura. Se
  decidió NO poner el validador solo en cliente: una regla que el contrato no tiene diverge y engaña.
  (2) D-actividad-ux (MEJORA PLANIFICADA): dos `<select formControlName="asignatura"` en el mismo formulario
  sin `id` ni `label for` —se anuncian igual a un lector de pantalla y hay que desambiguarlos por ancestro—;
  el error de servidor viejo convive con un error de campo nuevo (`error()` solo se limpia al empezar una
  petición); el aviso de multiplaza vive dentro de la celda del recuento. Candidata a absorberse en O-diseño.
  (3) AMPLIADA D-F8.6-ii-a (ver su texto en la sección de deuda viva): los 400/409 llegan al navegador SIN
  `message` y la UI pinta «Bad Request». Medido por curl en tres endpoints distintos, fuera de la UI. Afecta a
  TODOS los formularios existentes y deja MUDO el 409 que esta sesión acaba de construir. La causa (cambio de
  comportamiento de `server.error.include-message` en Spring Boot 4) es HIPÓTESIS NO MEDIDA. Hallazgo de
  MÉTODO asociado: los tests de endpoint asertan `status().reason()`, que lee el `MockHttpServletResponse` y
  no el cuerpo que viaja por la red — verde en test, mudo en producción, durante meses.
  (4) D-jornada-msg409 NO se cierra pero baja de coste: la fase 1 parametrizó el verbo de
  `ReferenciaEntranteException`, así que corregir el mensaje de jornada pasa a ser cambiar el ctor de uno a
  dos argumentos. `JornadaService` sigue usando el de un argumento y el usuario sigue leyendo «No se puede
  borrar» al GUARDAR.
  (5) NACE C-niveles como Cambio de O-estructura (no deuda): sin UI de niveles no hay grupos, ni subgrupos, ni
  población para las plazas, y el e2e del criterio de terminado es inejecutable. Backend completo desde S70,
  molde plano de catálogo, el más barato del repo. NO se metió aquí para no diluir el foco de riesgo.
  CORRECCIÓN DOCUMENTAL — una sola, y NO las dos que se anunciaron. Se comprobó contra el fichero que el plan
  YA estaba correcto en las otras dos: la integridad referencial figura ACTIVA desde S73/S74 con
  `D-F8.5-A-a` CERRADA (la afirmación de que las FK de SQLite estaban apagadas vive solo en la entrada
  ARCHIVADA de S72, y lo archivado no se corrige), y el conteo del solver que registra el plan (91) es el
  correcto frente al grep de 97 (que cuenta también los `@Tag("escala")`, fuera del perfil por defecto). La
  corrección real es la de D-F8.6-ii-a: decía que `server.error.include-message` «no está en
  `application.properties`», y hoy sí está sin que el mensaje viaje.
  R-terminado RESPETADA: no se pulió fuera de criterio. Se frenó el filtrado I3 en cliente, se frenó el
  validador de subgrupos, se frenó la guarda 409 «fina» (comparar la forma de la lista de plazas para permitir
  renombrar con horario vivo), se frenó C-niveles y se frenó el arreglo de D-F8.6-ii-a pese a que degrada el
  409 recién construido: elegir entre reactivar la clave, `ProblemDetail` o traducir en cada controlador
  necesita su propio M2. La petición del arquitecto de mejorar el aspecto de la aplicación se remitió a
  O-diseño (§3), que existe precisamente para que la maquetación no se cuele a trozos dentro de otros
  objetivos; se le recordó su salvedad de prioridad (si hay demo a cliente o al IES antes de cerrar H2, sube).
  HIGIENE (M1-bis): archivada S107 a `bitacora-sesiones.md` (promovida a `### Sesión 107`, insertada al final
  en orden ascendente, cuerpo íntegro); degradada S108 a «Última sesión registrada (previa)» compacta; S109
  queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S107), la crónica de
  archivado y la frase de ventana del plan.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado que los cinco commits de código separan por fase
  (`git log --oneline 29f0f84..HEAD`), que documentación y código van en commits distintos y que ningún `.db`
  ni el `informe-m2-s109.md` suelto entraron en el árbol.
  NOTA TÉCNICA para el e2e que cerrará O-estructura: la aplicación es ZONELESS (Angular 21 sin
  `provideZoneChangeDetection` ni polyfill de zone.js), así que el DOM repinta en el frame siguiente al clic;
  leer el DOM en el mismo tick da falsos rojos. Los asertos deben ser con reintento (`expect(...).toHaveCount()`).
  Además, `testDir` apuntando a `/tmp` hace que Playwright escanee el directorio entero y muera con EACCES, y
  no sigue symlinks al descubrir tests.
  O-estructura (H2) ACTIVO, 3 piezas hechas (jornada S107, subgrupos S108, actividades trozo A S109) de
  varias. Siguiente: candidatos vivos son el trozo B de actividades, D-F8.6-ii-a (que dejaría de ser mudo el
  409 recién construido) y C-niveles; lo fija su propio M0 (ver M1-ter).

### Sesión 110 — O-estructura (H2): C-actividades, trozo B. CIERRA C-actividades. Lista de plazas variable con alta/baja e I2 en cliente (Config/UI, M3 real). NO cierra el objetivo.
  Décima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI con M3 REAL (alta/baja de filas y
  validación cruzada I2 son lógica, no binding), UN SOLO MÓDULO (frontend), sin turno previo de medición
  inter-módulos en M4. CUARTA pieza de O-estructura y CIERRE de C-actividades, que S109 entregó troceado.
  O-estructura sigue ACTIVO —su criterio (8 tipos de sesión configurables + casos de validación §6 del modelo
  + e2e UI→solver heredado) exige aún C-niveles, PDC, tutores y el e2e—. Lo que ENTREGA el trozo B: con la
  lista de plazas abierta, desdobles, agrupamientos y bloques de optativas son construibles por UI, porque el
  M2 de S109 midió que un desdoble ES una actividad multiplaza (§4.6 del modelo, `roundTrip_bloqueSeisPlazas`);
  y se retira la guarda que impedía EDITAR toda actividad de más de una plaza, hueco funcional que S109 abrió a
  sabiendas.
  M0 — apertura verificada contra `gestion_proyecto.md`: Objetivo = O-estructura (H2), ACTIVO desde S107 con 3
  piezas hechas. Hito = H2. Cambio = C-actividades trozo B, nombrado por el M1-ter de S109. R-invalidación sin
  conflicto (O-diseño depende de H2 cerrado, O-demo de O-estructura); consecuencia práctica registrada: NO se
  toca el acabado visual del formulario, que es O-diseño. TRES candidatos vivos se pesaron y DOS se descartaron
  por regla, no por criterio: (a) D-F8.6-ii-a NO abre sesión —R-deuda: su ficha de §4 dice que no bloquea el
  criterio, solo lo degrada—, aunque queda anotada la grieta de que la 2ª pata del criterio («los casos de
  validación del §6 se reproducen desde la UI») es discutible si la pantalla dice «Bad Request» en vez del
  motivo: se reabre cuando llegue ese Cambio o el e2e; (b) C-niveles fuera por la decisión (e) de S109, que la
  fijó pegada al e2e, y no desbloquea el trozo B (los niveles de prueba se crean por curl). Trozo B gana por
  tres razones: es la única pieza que ataca la pata 1 del criterio, cierra el hueco funcional de la guarda, y es
  delta puro sobre el molde recién construido.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL antes de teclear). La sospecha de apertura del arquitecto era
  que `modoAula` fuera un control global, lo que habría convertido el delta en refactor: DESMENTIDA por
  medición —`modoAula` vive DENTRO del FormGroup de cada plaza (`actividad-form.ts:47`, `:170`) y la suscripción
  del XOR se crea POR FILA en `crearPlaza()`, con la fila capturada en el closure—. El trozo A cumplió lo que su
  javadoc prometía. Lo que la medición SÍ cambió: (1) `precargar` no reconstruía nada —leía `plazas[0]` y escribía
  sobre `at(0)`—, así que hay que rehacerlo, no ampliarlo, con el molde `jornada.rellenar` (`clear()` + `push` en
  bucle); (2) NO existe en todo el frontend ningún FormArray con alta/baja dirigida por el usuario: el único array
  variable (`jornada`) se rehace entero desde el dato y no tiene botones, luego los botones son diseño nuevo, no
  copia; (3) I2 en el backend (`ActividadService:247-263`) cruza CÓDIGOS con `HashSet<String>` SIN normalizar
  mayúsculas y DEDUPLICA dentro de cada plaza antes de cruzar (un repetido intra-plaza NO es error), mensaje
  literal «el subgrupo X aparece en mas de una plaza de la actividad», HTTP 400; (4) el mínimo de 1 plaza SÍ es
  regla del contrato (400 «una actividad necesita al menos una plaza») y NO existe máximo; (5) backend con CERO
  trabajo: los cuatro tests de reconciliación (`putReduccion_deSeisADosPlazas`, `putEstabilidad_editarContenidoNoRegeneraCodigos`,
  `putCrecimiento_deDosACuatro`, `putReusoDeHueco_trasBorrarP3`) ya cubren crecer, reducir y reusar hueco.
  DECISIÓN derivada de (3): el validador de cliente replica la semántica del backend CON sus dos rarezas —no
  normaliza caja, deduplica por fila—, porque normalizar haría que el formulario rechazara cuerpos que la API
  acepta: la misma familia de error que D-plaza-sin-subgrupos.
  M4 — CONTRASTE ANTES DE TECLEAR (Claude Code, con bancos desechables en /tmp ejecutados y borrados). Tumbó
  UNA parte del contrato y confirmó el resto. (a) RIESGO PRINCIPAL medido en ejecución: con `track $index` y
  `removeAt` de una fila intermedia, el DOM sigue mostrando la fila BORRADA y lo que el usuario teclea entra en
  un FormGroup ya extraído del array —`aRequest()` enviaría contenido viejo, sin error visible—; trackear el
  CONTROL lo arregla y basta, sin migrar a `[formGroup]="fila"`. (b) TUMBADO: el harness del aserto del track tal
  como estaba especificado NO es implementable —mutar un FormArray no marca la vista sucia en zoneless, así que
  `detectChanges()` lanza NG0100 y pone rojo un código correcto, y `whenStable()` a secas no repinta y no
  discrimina—; la acción tiene que ser por CLICK en el botón, que es el camino real. (c) el validador entra en la
  CONSTRUCCIÓN del array, no por `setValidators` posterior. (d) NO hay bucle ni fuga: `aplicarModo` nunca escribe
  `modoAula`; el coste del validador es cuadrático en filas y son 1,5 ms con 6 plazas; las filas que `clear()`
  deja atrás no fugan porque la referencia va de la huérfana al componente y nunca al revés (condición: no
  guardar punteros a filas retiradas). (e) el contraste señaló CINCO huecos de aserto: tres entraron como casos
  propios (26)(27)(28) y dos se absorbieron en (20) y (21). (f) la lista de ficheros del contrato estaba
  INCOMPLETA en los dos CSS del propio componente.
  FASE 1 — CÓDIGO (siete ficheros de producción, frontend puro). `actividad-form.ts`: fuera la señal
  `multiplaza` y sus tres guardas; `precargar` reconstruye el array con una fila por plaza del dato y un
  `volcar(fila, plaza)` extraído, que asigna `modoAula` PRIMERO porque `aplicarModo` limpia la rama contraria;
  `anadirPlaza()` y `quitarPlaza(i)` con la guarda del mínimo EN EL MÉTODO además del `[disabled]` (misma
  doctrina de dos capas que la guarda retirada); validador de array `subguposDisjuntos` hermano de
  `arrayNoVacio`, instalado en la construcción del array, que devuelve el CÓDIGO del repetido para poder
  nombrarlo; `aRequest`/`aPlazaRequest` intactos —ya mapeaban `plazas.controls.map(...)`, que es la prueba de
  que el trozo A dejó el delta preparado—. `actividad-form.html`: `track` por CONTROL manteniendo
  `[formGroupName]="i"`, `<legend>Plaza {{ i + 1 }}</legend>`, botones de añadir/quitar, y hueco de error de I2
  condicionado al error CONCRETO (`plazas.errors['subgrupoRepetido']`, vía getter) y NO a `plazas.invalid` —los
  errores de las filas propagan al array, y con `invalid` el aviso saldría cuando lo que falta es un profesor—.
  `actividad-lista.{ts,html}`: retiradas las dos capas de la guarda de multiplaza y el aviso de la celda;
  `esMultiplaza` SE CONSERVA porque lo usa la confirmación de borrado. CSS: mínimo estructural para separar los
  botones de fila de los del diálogo; el acabado es O-diseño. Commits: `feat(frontend): el editor de actividad
  admite N plazas con alta, baja y validacion I2 en cliente` (`e609a6c`) y `test(frontend): congela la lista
  variable de plazas, el track del FormArray y la invariante I2` (`6a38d0d`).
  FASE 2 — TESTS. Seis casos rotos por la firma del helper (`rellenarPlaza(indice, modo, extra)`), el (13) de la
  guarda borrado con su helper `montarSinRed`, los (7) y (8) de la lista fundidos en un (12) que congela lo
  CONTRARIO (toda actividad es editable y el aviso ya no existe), y DOCE casos nuevos (17)-(28). Suite vitest
  239 → 249; backend intacto 261/91. INCIDENCIA DE ESTRUCTURA cazada antes de mutar: una llave desplazada dejaba
  los doce casos nuevos ANIDADOS dentro del `it` del caso (1) —la sangría uniforme lo ocultaba—, así que nunca
  se habían ejecutado; se detectó contando llaves, no por inspección visual, y la hipótesis inicial del
  arquitecto sobre qué caso los envolvía era errónea.
  M3 — CAMPAÑA DE MUTACIÓN (14 mutaciones + demostración de suite no vacía, cada una con restauración
  verificada). NUEVE mutaciones murieron, CINCO sobrevivieron. Discriminan de pleno: M2/M3 (las dos capas del
  mínimo, cada una matando SU aserto dentro del mismo caso (21)), M4 (`removeAt(0)` → (20) por el DOM y (27)
  por el cuerpo), M5 (fila clonada → (19)), M6 (normalización `toLowerCase` → (24)), M7 (validador retirado →
  (22)), M8 (aviso condicionado a `plazas.invalid` → (25)), M9 (precarga degenerada → (17)), M11 (orden de
  plazas invertido → (18) y (27), la dimensión mejor cubierta y la que más lo necesitaba por ser posicional la
  reconciliación), M12 (XOR gobernando la fila 0 → (26), que era justo el hueco que el contraste señaló).
  Caídas por ACOPLAMIENTO declaradas y no maquilladas: (23) bajo M5 y M8, (18) bajo M9, (28) bajo M7 (muere en
  su precondición), (18) y (27) bajo M12.
  HALLAZGO CARO DE LA CAMPAÑA: M1 (`track fila` → `track i`) NO tumbaba NADA, y el caso (20) estaba escrito
  precisamente para protegerlo. La causa, medida con un probe: el `@if` del XOR CURA el desalineamiento —al
  cambiar el contexto de la fila, la rama se destruye y se recrea, y el `FormControlName` nuevo re-resuelve
  contra la posición actual—, así que todos los nodos que (20) miraba (aula fija, candidatas, `<span>` de error)
  son testigos inservibles. Los únicos válidos son los nodos FUERA de todo `@if` enlazados por
  `formControlName`, cuya directiva persiste con su `[formGroupName]` de siempre. (20) se REESCRIBIÓ sobre el
  select de asignatura de la plaza, con testigo de LECTURA (el valor pintado) y de ESCRITURA (teclear en la fila
  visible llega al modelo vivo), más los legends. Re-ejecutadas M1 y la mutación del número de plaza: ahora
  ambas mueren, y M4 sigue muriendo en su aserto de siempre, sin regresión. IMPORTANTE: el hueco era del TEST,
  no del componente —el código de producción llevaba el `track` correcto desde el principio—.
  SUPERVIVIENTES ACEPTADOS, los tres deliberados: M10 (mover `modoAula` al final de `volcar`) es MUTANTE
  EQUIVALENTE con datos válidos del contrato —el XOR garantiza que la rama contraria ya viene vacía, así que
  limpiarla no borra nada—; M13 (quitar la deduplicación intra-plaza del validador) no lo mata nadie porque el
  escenario es INALCANZABLE desde la UI (un `<select multiple>` no selecciona dos veces la misma opción y el GET
  proyecta desde un `Set`) → nace deuda de test; M14 (`onlySelf` en `aplicarModo`) predicho y confirmado.
  M4 — VERIFICACIÓN MECÁNICA con Playwright EFÍMERO sobre Chromium REAL (script fuera del repo en `/tmp`,
  borrado al terminar, árbol verificado limpio: la suite e2e permanente es el Cambio que CIERRA O-estructura,
  no andamiaje de una sesión; R-e2e respetada). 18 puntos, 17 PASAN y 1 NO MEDIDO. Lo verificado que importa:
  se construye una actividad de SEIS plazas desde cero con contenido distinto por fila alternando las dos ramas
  del XOR, el POST sale 201 y el GET devuelve las seis en el MISMO orden con códigos -P1..-P6 y ramas intactas;
  el VIAJE DE VUELTA pinta las seis filas con su rama derivada del dato y los `<select multiple>` con su
  selección REALMENTE marcada en el DOM —el punto que costó trabajo en S108 y que ninguna prueba de unidad
  había visto en un navegador—; el round-trip sin tocar nada devuelve exactamente lo mismo; quitar la plaza del
  medio deja CINCO con los códigos -P1..-P5 y el desplazamiento previsto (el código -P3 pasa a describir lo que
  era la plaza 4: la reconciliación posicional funcionando, y la razón de que el PUT exija 409); las dos capas
  del mínimo sostienen con clic forzado sobre el botón deshabilitado; e I2 avisa nombrando el subgrupo, no emite
  petición, y el aviso se limpia tanto corrigiendo el subgrupo como quitando la fila culpable. El catálogo de
  prueba (ASG-A..F, PRF-A..F, AUL-A..C, SBG-A..F) se sembró por curl y SE CONSERVA: es población útil para el
  e2e ahora que la BD sobrevive al arranque (S109).
  VERIFICACIÓN DE USABILIDAD (parte no delegable, porque es la pregunta de riesgo de O-estructura): llegar a
  seis plazas es TOLERABLE; el número de plaza en el `<legend>` se considera útil, no ruido; quitar una fila del
  medio hace lo esperado sin que el usuario vea códigos ni ids; y el texto del aviso de I2 se entiende sin
  releerlo. Las DOS imprecisiones que S109 detectó en la definición del arquitecto —decir «grupo» donde el
  modelo dice SUBGRUPO, y situar el tramo en la Plaza cuando el tiempo lo pone la Actividad— quedan hoy
  RESUELTAS por la pantalla. La tosquedad visual se declara consciente y aplazada a O-diseño, por el propio
  arquitecto. CAVEAT REGISTRADO, intacto desde S109: probar la usabilidad con el AUTOR del modelo es evidencia
  débil; la pregunta «¿es configurable por un humano?» se cierra en O-demo con el IES real. Lo que sí queda
  demostrado es que el caso MULTIPLAZA —el interesante— se expresa por formulario sin contorsiones.
  DEUDA — nace UNA, se afina otra y se cierra un tercio de una tercera (R-deuda: ninguna bloquea el criterio,
  no se pagan ahora):
  (1) D-i2-dedup-cliente NUEVA (deuda de TEST, no de código): la deduplicación intra-plaza del validador de I2
  no la cubre ningún caso (M13 superviviente), y el escenario es inalcanzable desde la UI. Molde
  D-jornada-flush-test. Cuelga de O-estructura.
  (2) D-F8.6-ii-a AFINADA con la medición E1 de esta sesión, la PRIMERA desde el navegador y no por curl: el
  mensaje accionable NO se pierde —viaja como REASON PHRASE de la respuesta HTTP— y lo que falta es la clave
  `message` en el CUERPO. Es más específico que lo que dejó escrito S109. Deja una pista para su M2 (el cliente
  podría leer `statusText`) que NO es la solución: HTTP/2 no transporta reason phrases.
  (3) D-actividad-ux RECORTADA: su síntoma (3) —el aviso de multiplaza dentro de la celda del recuento,
  mezclando dato y aviso— queda CERRADO de paso al retirar la guarda. Sobreviven los dos primeros: los dos
  `<select formControlName="asignatura"` sin `id` ni `label for`, y el error de servidor viejo que convive con
  un error de campo nuevo.
  (4) D-plaza-sin-subgrupos SIN CAMBIO: el trozo B no la toca, y su spec (8) sigue siendo el que se pondría
  rojo si alguien añadiera el validador solo en cliente.
  CORRECCIÓN DOCUMENTAL — dos javadoc que la campaña demostró FALSOS se corrigieron en el código, porque un
  comentario que afirma algo incierto hace que el siguiente lector decida sobre una premisa falsa: (a) el de
  `aplicarModo` decía que `{ onlySelf: true }` «desactivaría I2 en silencio», y M14 midió que no —los dos
  `updateValueAndValidity()` siguientes propagan igual—; se conserva la prohibición, pero declarando que la
  inocuidad DEPENDE de esas dos líneas. (b) el de `volcar` decía que asignar `modoAula` al final borraría el
  valor recién escrito, y M10 midió que con datos válidos del contrato eso no puede pasar; se conserva el orden
  declarando que es defensa y que deja de ser equivalente si el backend devolviera las dos ramas llenas.
  R-terminado RESPETADA: no se pulió fuera de criterio. Se frenó el acabado visual (O-diseño, y el arquitecto
  lo encuadró él mismo), se frenó el arreglo de D-F8.6-ii-a pese a tener su alcance recién medido, se frenó
  C-niveles, y se frenó escribir un test para el escenario inalcanzable de M13. Lo que SÍ se hizo dentro del
  Cambio y no era «pulir»: reescribir (20), porque un caso que parece cubrir la dimensión más cara y no la
  cubre es peor que no tenerlo.
  HALLAZGO DE MÉTODO (condensado en M3 de `metodo.md`, no añadido como quinta precisión): en una campaña de
  mutación, la restauración se verifica por DIFF contra una copia previa, NUNCA por el verde de la suite. En
  esta sesión un `cp` falló en silencio y el `npm test` posterior dio 249 verdes igualmente, porque la mutación
  era SUPERVIVIENTE: el conteo confirmó una restauración que no había ocurrido.
  HIGIENE (M1-bis): archivada S108 a `bitacora-sesiones.md` (promovida a `### Sesión 108`, insertada al final en
  orden ascendente, cuerpo verificado idéntico por diff, 78 líneas); degradada S109 a «Última sesión registrada
  (previa)» compacta; S110 queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora
  (→ S10–S108), la crónica de archivado y la frase de ventana del plan.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado que los dos commits de código separan producción de tests
  (`git show --stat` de ambos: 6 ficheros sin ningún `.spec.ts` en el primero, los 2 specs solos en el segundo),
  que documentación y código van en commits distintos, y que el árbol quedó limpio sin ficheros nuevos ni `.db`.
  NOTA TÉCNICA para quien vuelva a tocar este formulario: el `track` del `@for` de plazas va POR CONTROL y no
  por índice, y el caso (20) es su único guardián —pero solo si sus testigos siguen siendo nodos FUERA de todo
  `@if`; reescribirlo mirando el aula fija devolvería el hueco sin que nadie se entere—. Y todo aserto que
  accione alta/baja de filas debe hacerlo por CLICK: en zoneless, mutar el FormArray por método no marca la
  vista sucia, y `detectChanges()` sobre un índice desplazado lanza NG0100.
  O-estructura (H2) ACTIVO, 4 piezas hechas (jornada S107, subgrupos S108, actividades S109+S110) de varias.
  C-actividades CERRADO. Siguiente: candidatos vivos son C-niveles (el más barato, BLOQUEA el e2e del criterio
  de terminado), D-F8.6-ii-a (que sigue dejando mudos todos los formularios entregados), PDC y tutores; lo fija
  su propio M0 (ver M1-ter).

### Sesión 111 — O-estructura (H2): C-niveles. CRUD de Nivel por UI + recorrido completo del centro mínimo en navegador (Config/UI, sin M3). NO cierra el objetivo.
  Undécima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI SIN M3 (no hay lógica: dos campos y
  binding; la única regla de cliente es un `required`), UN SOLO MÓDULO (frontend), backend con CERO trabajo.
  QUINTA pieza de O-estructura. Lo que ENTREGA: con niveles creables por pantalla, las NUEVE filas del centro
  mínimo —Nivel, Grupo, Subgrupo, Profesor, Asignatura, Aula, ≥1 tramo lectivo, Actividad, Plaza— son
  construibles por UI y el solver produce horario a partir de ellas. Hasta hoy la cadena se rompía en la
  PRIMERA fila. O-estructura sigue ACTIVO: su criterio exige aún PDC, tutores y el e2e UI→solver heredado.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-estructura (H2), ACTIVO desde S107 con 4
  piezas hechas. Hito = H2. Cambio = C-niveles. R-invalidación sin conflicto (O-diseño depende de H2 cerrado y
  repinta, no reestructura). Los CUATRO candidatos vivos se pesaron por dependencias: C-niveles gana por ser el
  ÚNICO del que dependen los demás —el e2e necesita las nueve filas creables y Nivel era la única sin
  formulario; PDC y tutores no desbloquean nada—. D-F8.6-ii-a queda fuera por R-deuda (su ficha dice que no
  bloquea el criterio) y no la rescata el e2e, porque R-e2e prohíbe probar ramas de error en navegador.
  DECISIÓN DE ENCUADRE: se REVISA la decisión (e) de S109, que fijó C-niveles pegada al e2e, y se separan. Tres
  razones, la primera es dependencia técnica real: (1) el e2e no puede escribirse antes de que exista el
  formulario cuyos selectores y flujo necesita; (2) perfiles de riesgo opuestos —molde validado cinco veces
  frente al primer test de navegador del proyecto, con dos trampas ya documentadas—, y pegarlos deja que el
  desbordamiento del segundo se lleve por delante la entrega del primero; (3) ni juntos cierran el objetivo.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL antes de teclear, informe por bloques A-F). Confirmó lo
  esperado y afinó lo que decidía el alcance. (1) El CRUD de Nivel es réplica fiel del molde plano de
  Asignatura y MÁS BARATO: cinco endpoints, 13 casos en `NivelEndpointTest` con servicio real sobre SQLite,
  guarda 409 en el borrado, y SIN el sub-recurso `aulas-compatibles` que Asignatura arrastra. Backend con cero
  trabajo. (2) NO hay enum ni lista blanca: el formulario son dos campos, descartado el molde de Aula. (3) UN
  ÚNICO referente, `grupo_administrativo.nivel_id`, verificado por FK en `schema.sql` y por relación JPA: el 409
  muestra un solo conteo. (4) DESVIACIÓN REAL DEL MOLDE: `orden` es `int` PRIMITIVO sin ninguna validación de
  servidor —ni rango, ni positividad, ni unicidad—, así que un cuerpo sin la clave deserializa a 0 EN SILENCIO
  y nadie lo detecta; el servidor no puede distinguir «ausente» de «cero». (5) `listar()` ordena por `orden`
  (D-1) en el servicio, no en el repositorio, y el test `listar_ordenaPorOrdenNoPorCodigo` lo blinda con orden
  numérico cruzado con el alfabético. (6) `nivel.model.ts` y `nivel.service.ts` YA EXISTÍAN con javadoc que
  declaraba explícitamente que el CRUD no se expone en la UI y que añadir wrappers sería «código muerto».
  DECISIÓN sobre `orden`, la única de la sesión: el formulario pone `Validators.required` y NADA MÁS —ni `min`,
  ni rango, ni unicidad—. No contradice la decisión (c) de S109 (el formulario refleja el contrato): lo que
  aquélla prohíbe es RECHAZAR cuerpos que la API acepta, y `required` no restringe qué valor es válido (0 sigue
  siendo aceptable), solo impide enviar uno que el usuario no ha elegido. Derivada: el control es
  `FormControl<number | null>` y NO `nonNullable`, para que el campo NAZCA VACÍO —el equivalente numérico del
  `''` de un campo de texto es `null`; un inicial de 0 o de 1 sería un orden decidido por el formulario—.
  DECISIÓN sobre la asimetría PUT/DELETE: el M2 la levantó (el DELETE guarda ante dependientes, el PUT no) y se
  cierra el juicio en contra de tocarla: renombrar el código de un nivel con grupos colgando NO corrompe nada
  porque la FK es por id, es lo que un renombrado debe hacer, y el molde comparte la asimetría (Asignatura
  tampoco guarda en el PUT). No es deuda; es decisión consciente.
  FASE 1 — MODELO Y SERVICIO. `nivel.model.ts` gana `NivelRequest`; `nivel.service.ts` pasa de un wrapper a los
  cinco. Los javadoc de AMBOS se reescriben: afirmaban lo contrario de lo que el Cambio entrega, y un
  comentario falso hace que el siguiente lector decida sobre una premisa falsa (hallazgo aplicado de S110).
  `nivel.service.spec.ts` de 1 a 5 casos, con el (1) conservando la intención original —congela que el cliente
  NO reordena— sobre un fixture cuyo orden pedagógico contradice al alfabético. Suite 249 → 253.
  `grupo-form.spec.ts` intacto en 11: el contrato HTTP no se movió, solo creció la superficie del cliente.
  Commits `71e5305` (producción) y `be9110f` (tests).
  FASE 2 — FORMULARIO Y LISTA. Ocho ficheros nuevos en `components/niveles/`, calcados del molde salvo la
  desviación de `orden`. La lista NO ordena en cliente y aquí no es solo regla de molde: reordenar por `codigo`
  pondría 1BACH antes que 1ESO y destruiría el único criterio que el campo `orden` existe para expresar. Suite
  253 → 266. Commits `b6dbd6d` (producción, 6 ficheros sin ningún `.spec.ts`) y `06fb70c` (los 2 specs solos).
  FASE 3 — CABLEADO. Tres líneas en `Configuracion` (import, entrada en `imports:`, etiqueta) más un caso (8),
  con la lista de niveles ANTES de la de grupos porque el orden de la plantilla sigue el orden de alta. Salen
  DOS desviaciones respecto a lo previsto, ambas medidas por Claude Code y no supuestas: (a) `configuracion.ts`
  tenía un javadoc de 35 líneas que el dictado no incluía —se conservó íntegro y se corrigió por separado—; (b)
  `configuracion.spec.ts` NO usa `HttpTestingController` ni helper de montaje compartido: dobla los servicios
  por `useValue` con `of([])`, así que la cascada de `http.verify()` que se anticipó no podía ocurrir. Suite
  266 → 267. Commits de producción, de test y `docs(frontend)` para el javadoc: TRES commits en vez de los dos
  del patrón de S110, por haber descubierto el javadoc desfasado después de dictar el fichero.
  DECISIÓN sobre el javadoc de `Configuracion`: niveles NO es una quinta entidad de O-catálogo. Ese objetivo
  cerró en S106 con su censo de cuatro y reabrirlo violaría R-terminado; la sección de niveles es un Cambio de
  O-estructura que resulta tener la misma forma de UI. Por eso las dos frases del javadoc que declaran el censo
  de cuatro y el 4/4 se conservaron LITERALES y la corrección fue aditiva.
  M4 — CAMPAÑA DE MUTACIÓN (Claude Code, siete mutaciones, protocolo de restauración verificada por diff).
  Cuatro mueren: quitar el `required` de `orden` (lo mata el (7) del form, que no aserta ninguna petición y se
  apoya en el `http.verify()` del `afterEach`), nacer con 0 en vez de null, añadir `min(1)`, y ordenar la lista
  por `codigo` en cliente. Tres sobreviven, y el análisis de los tres CORRIGE la predicción del arquitecto:
  (a) M6 —invertir la precedencia de `mensaje()` en la lista— no podía morir por el caso del formulario, porque
  hay DOS funciones `mensaje()` copiadas a propósito y NO compartidas: son funciones distintas. La premisa
  falsa venía del javadoc de `asignatura-lista.spec` («cubierto en el form, misma función»), que se propagó sin
  verificar. Se arregla añadiendo la clave `error` al cuerpo flusheado del caso (4), que pasa a medir texto rico
  Y precedencia sin caso nuevo; re-verificado por mutación dirigida: muere, y solo ese caso. (b) M5 (`=== true`
  laxo) y M7 (`track $index`) son MUTACIONES EQUIVALENTES y se dejan vivas con razón escrita en el javadoc del
  spec: `NivelForm` cierra con `true` o sin argumento, nunca con `false`, y las filas de la lista no tienen
  estado en el DOM. Matarlas exigiría fabricar escenarios que el sistema no produce —mismo criterio que
  D-i2-dedup-cliente—. Lo que SÍ se añadió, por laguna de cobertura frente al molde S108 y no por mutación:
  los casos (6) y (7), que ejercitan `abrirForm`, código que este Cambio entrega y que nadie tocaba. Suite
  267 → 269. Commit `287c7d1`.
  HALLAZGO DE MÉTODO: con una mutación que deja peticiones HTTP abiertas, el `afterEach` que lanza deja el
  TestBed instanciado y CONTAMINA lo que corre después en el mismo worker. La misma mutación dio 44 fallos en 7
  ficheros en una pasada y 3 en 2 en la siguiente, con baseline verde 3 de 3. Consecuencia: en esa clase de
  mutación el conteo de fallos NO es reproducible y hay que leer el NOMBRE del caso, no el número.
  M4-bis — RECORRIDO COMPLETO DEL CENTRO MÍNIMO EN NAVEGADOR (conducido por el arquitecto, no delegable). Se
  eligió el recorrido completo sobre el corto precisamente para que el reconocimiento del terreno lo pague esta
  sesión y no la del e2e. RESULTADO: las nueve filas se crean por UI y el solver devuelve horario (tres sesiones
  de MAT sobre el centro de prueba). Las tres comprobaciones propias de niveles pasan: la tabla lista 1ESO antes
  que 1BACH (pedagógico, no alfabético), el desplegable de Grupo trae los dos niveles y refleja el código
  editado, el 409 aparece al borrar un nivel con grupo colgando, y un alta con el orden vacío se detiene en
  cliente con «El orden es obligatorio» sin llegar al servidor. C-niveles CUMPLE.
  DATO DE ENTORNO fijado para el e2e: `spring.datasource.url=jdbc:sqlite:educhronos.db` es RELATIVA al
  directorio desde el que se lanza Spring Boot. La base viva es `app/educhronos.db` (se arranca desde `app/`);
  la `educhronos.db` de la raíz era un residuo de un arranque de julio. Para partir de cero hay que renombrar o
  borrar `app/educhronos.db` con la aplicación parada, porque desde S109 `schema.sql` ya no demuele.
  HALLAZGOS DEL RECORRIDO, ninguno ejecutado en esta sesión (R-deuda y R-terminado: ninguno bloquea el criterio
  y ninguno es C-niveles). Nacen clasificados en §4: (1) D-horario-irreversible, el más grave —no existe
  endpoint que borre o reemplace un horario, cada generación ACUMULA, y como el 409 del PUT de actividad cuenta
  `sesion(es)` entre sus referentes, una actividad usada por un horario queda congelada para PUT y DELETE de
  forma permanente por la vía UI/API—; (2) D-error-generacion-pin, el degradado de `errorGeneracion` dice «El
  servidor rechazó el pin» ante un horario infactible porque reutiliza el helper escrito para los pines; (3)
  D-molde-mensaje-cubierto-en-form, deuda de test heredada de O-catálogo que este M4 destapó; (4) D-log-aplicacion,
  mejora futura propuesta por el arquitecto (no hay logging estructurado en ninguna de las dos capas).
  CONTRADICCIÓN DOCUMENTAL medida, que AFINA D-F8.6-ii-a y vale más que su tamaño: el javadoc de
  `asignatura-lista.ts` afirma que `server.error.include-message=always`, y el de `horario-view.ts` afirma que
  está DESACTIVADO. El comportamiento observado en navegador —«Conflict» crudo en el 409 de nivel— da la razón
  al segundo. El M2 de esa deuda debe partir de este dato y no del javadoc de O-catálogo.
  R-terminado RESPETADA: no se pulió fuera de criterio. Se frenó D-F8.6-ii-a con su alcance ya medido dos veces,
  se frenaron los cuatro hallazgos del recorrido, se frenó arreglar el aserto ambiguo del caso (8) de
  `Configuracion` (mide lo que le toca y su ambigüedad es del molde, no de niveles), y se frenó tocar los specs
  de O-catálogo que arrastran el mismo comentario falso destapado por M6.
  HIGIENE (M1-bis): archivada S109 a `bitacora-sesiones.md` (promovida a `### Sesión 109`, insertada al final en
  orden ascendente, cuerpo verificado idéntico por diff); degradada S110 a «Última sesión registrada (previa)»
  compacta; S111 queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S109), la
  crónica de archivado y la frase de ventana del plan.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado que los commits de código separan producción de tests en
  las tres fases, que documentación y código van en commits distintos, y que el árbol quedó limpio —los dos
  `*.db.pre-s111` del recorrido se borraron y `git status --short` no devuelve nada—.
  NOTA TÉCNICA PARA EL e2e (amplía la de S109 sobre ZONELESS y `testDir`, medida en esta sesión sobre
  `horario-view.ts`): (1) el id del horario está CLAVADO a `/horario/1` en el enlace de la landing; no hay
  endpoint de «último horario» ni lista, así que entrar por la landing lleva siempre al horario 1, exista o no.
  (2) Con base vacía el GET de proyección da 404 y el frontend lo trata como error FATAL —vacía la rejilla—,
  pero la pantalla sigue usable porque los controles viven fuera del `@if (error())`: el camino «entrar sin
  horario → generar el primero → ver la rejilla» funciona. (3) El botón «Generar horario» NACE DESHABILITADO
  (`avisosPrevalidacion() === null`) y si la prevalidación falla se queda deshabilitado PARA SIEMPRE, sin
  reintento. (4) El `next` del POST NO consume la proyección devuelta: navega a la ruta del id nuevo y la
  recarga la dispara la emisión de `paramMap`. Para el e2e eso significa esperar el POST **y** cuatro GET
  (bloqueos, prevalidación, diagnóstico, proyección, concurrentes y sin orden de llegada garantizado), y usar el
  CAMBIO DE URL como indicador de éxito, más fiable que el contenido de la rejilla. (5) Dos señales de error
  distintas comparten la clase `.error` (`error` y `errorPin`): un `locator('.error')` puede resolver a dos
  nodos. Las otras tres tienen clase propia.
  O-estructura (H2) ACTIVO, 5 piezas hechas (jornada S107, subgrupos S108, actividades S109+S110, niveles S111).
  C-niveles CERRADO y con ello el e2e DESBLOQUEADO. Suites: vitest 269, backend intacto 261/91. Siguiente:
  candidatos vivos son el e2e UI→solver (tercera pata del criterio, ya sin bloqueo y con reconocimiento hecho),
  PDC, tutores y D-F8.6-ii-a; lo fija su propio M0 (ver M1-ter).

### Sesión 112 — O-estructura (H2): C-e2e. El e2e UI→solver del centro mínimo (Desarrollo + e2e) y el arreglo del hueco de recarga tras la PRIMERA generación. CIERRA la tercera pata del criterio. NO cierra el objetivo.
  Duodécima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo MIXTO: Desarrollo con M3 real (la bifurcación de
  `lanzarGeneracion`) + el e2e, para el que M3 no aplica y se sustituye por un control de vacuidad de tres
  escenarios. SEXTA pieza de O-estructura. Lo que ENTREGA: la tercera pata del criterio —«un e2e de navegador
  crea un centro mínimo íntegramente por la UI y el solver corre sobre él»— queda CUMPLIDA y verificada.
  O-estructura sigue ACTIVO: su criterio exige aún PDC y tutores (patas 1 y 2).
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-estructura (H2), ACTIVO desde S107 con 5
  piezas hechas. Hito = H2. Cambio = C-e2e. R-invalidación sin conflicto. Los CUATRO candidatos vivos se
  pesaron. Gana el e2e por tres razones, y la primera es la decisiva: PDC y tutores NO lo invalidan —el centro
  mínimo no tiene PDC, y por R-e2e no se añade un e2e por cada tipo de estructura—, así que escribirlo ahora no
  crea trabajo que rehacer; (2) el reconocimiento de S111 es PERECEDERO y lo pagó ella para esta sesión; (3) es
  la única pata sin empezar y la de mayor riesgo. D-F8.6-ii-a fuera por R-deuda, y se CIERRA la grieta que S110
  dejó anotada: la 2ª pata habla del §6 del MODELO (validación contra los horarios reales del centro), no de la
  superficie de error de la UI, así que «Bad Request» degrada pero no impide reproducir los casos.
  HALLAZGO DEL M0 que reordena lo pendiente: PDC no es opcional, es pata 1 Y pata 2 —el §6.2 del modelo ES el
  caso 3ºADi— y su alta vive en el sub-recurso `/api/grupos/{idPadre}/pdc` (S76), que el CRUD de Grupo por UI no
  alcanza. Sin ese formulario O-estructura no puede cerrar. Tutores, en cambio, PUEDE estar más cubierto de lo
  que parece (`requiereTutor` ya es campo del form de Actividad desde S109; lo que no existe es `ProfesorTutoria`):
  no se afirma, exige su propio M2.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL, dos investigaciones encadenadas antes de teclear; informes
  consumidos aquí, sin fichero suelto commiteado). Confirmó la apuesta de apertura y destapó lo que decidió la
  sesión. (1) `schema.sql` tiene CERO `drop`: son 21 `create table if not exists` desde S109. El comentario de
  `playwright.config.ts` afirmaba literalmente lo contrario —«hace drop table if exists de las 21 tablas»—: una
  afirmación viva y falsa DENTRO del fichero que gobierna el e2e. Con `reuseExistingServer` en local, el e2e
  habría corrido contra la BD de trabajo, que ya tiene un horario generado y actividades congeladas por
  D-horario-irreversible. El andamiaje de S106 se apoyaba en una premisa que S109 invalidó. (2) La prevalidación
  del centro mínimo devuelve `[]`: las tres reglas (profesor 3/30, repeticiones 3/5, grupo 3/30) pasan holgadas,
  luego el botón se habilita y `ConfirmarGeneracion` no llega a abrirse. (3) `server.error.include-message=always`
  SÍ está en `application.properties`, con comentario propio: AFINA D-F8.6-ii-a y desmiente la conclusión de S111
  (que dio la razón al javadoc de `horario-view`); su M2 debe partir de aquí.
  HUECO FUNCIONAL DESTAPADO POR EL M4 Y ARREGLADO EN SESIÓN (no estaba registrado como deuda: nace y muere aquí).
  `provideRouter(routes)` sin features ⇒ `onSameUrlNavigation` = 'ignore' (leído del bundle instalado de
  @angular/router 21.2.17, no de memoria). Tras el POST, `router.navigate(['/horario', dto.id])` desde
  `/horario/1` emite `NavigationSkipped`, `paramMap` NO reemite, `cargar()` no vuelve a correr: la rejilla no se
  refresca y, como la carga inicial dio 404 sobre BD vacía, `error()` gatea el `@else if` y `<app-horario-grid>`
  ni se monta. Se manifiesta SOLO en la primera generación de una instalación nueva —después el id cambia y la
  URL difiere—, es decir, exactamente en el criterio 5 de Fase 8 que define H2, y es invisible en el uso manual
  repetido (por eso S111 no lo vio). DECISIÓN DE ALCANCE del arquitecto: el arreglo ENTRA, porque sin él la
  pata 3 solo se cumpliría con un `page.reload()` en el test, es decir, con el e2e tapando el agujero que existe
  para detectar. Vía elegida: bifurcación en el `next` (si el id devuelto es el ya cargado, `cargar()`; si no,
  navegar). Se DESCARTAN las otras dos: tocar el router es global y no está medido que `paramMap` reemita con el
  mismo componente y los mismos params; consumir la proyección del POST revertiría la decisión de S93 (rejilla,
  pines y diagnóstico no pueden pertenecer a horarios distintos), que esta vía conserva intacta al recargar por
  GET fresco. RECTIFICACIÓN DE MÉTODO EN SESIÓN: el M2 declaró M3 no aplicable «porque no habría lógica de
  producción»; con el arreglo la hay, y M3 volvió al ritual.
  M3 — campaña de 2 mutaciones sobre la bifurcación. M-A (navegar siempre, el estado previo) cae SOLO (40), vía
  `router.navigate` llamado cuando el aserto exige lo contrario. M-B (`cargar` siempre) cae (39) por 0 llamadas
  a navigate, y ADEMÁS el preexistente (31), que guarda esa misma rama desde antes: segundo guardián legítimo,
  no acoplamiento entre los nuevos. Cada caso guarda su rama. Restauraciones verificadas por DIFF contra copia
  previa, nunca por el verde (regla de S110).
  M4 — el contraste corrigió OCHO puntos del contrato antes de teclear, tres de ellos sobre el aserto: el código
  de actividad NO se pinta en la rejilla (solo como `aria-label` del candado, y solo si está pinada), así que el
  contable es `div.instancia`; «celdas» era impreciso (hay 30 `<td>` siempre y `td.ocupado` solo se enciende
  durante un arrastre); el value es `DISTRIBUIDA`. Y cinco de ejecución: el `orden` del nivel arranca vacío; el
  orden de creación es requisito duro porque cada diálogo puebla sus desplegables al abrirse; `getByRole` con
  `exact:true` (el `name` es substring y «Guardar» casaría con «Guardar jornada»); los multiselect por
  `selectOption`, que dispara el `change` que el handler escucha; y todo aserto con reintento por ZONELESS.
  BLOQUEO DEL INSTRUMENTO, reportado por Claude Code en el PASO 0 y resuelto con la opción MENOS invasiva: el
  doble de `getProyeccion` es un Subject COMPARTIDO, así que el 404 inicial del (40) lo cierra y la recarga
  redispararía el error. Se elige re-stub LOCAL al caso frente a homogeneizar el doble a fresco —eso tocaría los
  25 casos vigentes que lo consumen, superficie probada que este Cambio no pide (R-terminado)— y frente a quitar
  el 404 inicial, que habría acomodado el test al instrumento en vez de al problema: el 404 ES el escenario.
  Dos premisas del arquitecto corregidas por la medición del instrumento: este fichero no tiene `verify()` ni
  `HttpTestingController`, luego los Subjects pendientes no tumban casos ajenos.
  HALLAZGO DEL E2E, medido en la corrida y no previsto por nadie: las dos primeras corridas fallaron en pasos
  DISTINTOS con la misma firma —primer campo del diálogo vacío y `touched`, segundo campo bien—. Es la carrera
  entre el `fill` y el cableado del ControlValueAccessor: `cdk-dialog-container` entra en el DOM en cuanto el CDK
  lo crea, el contenido del portal se adjunta un tick después, y un `fill` en esa ventana lo borra el
  `writeValue('')` de la directiva al registrarse; el `required` bloquea entonces el submit y el diálogo no
  cierra. Barrera elegida: esperar a que el foco esté DENTRO del diálogo (`:focus`), válida porque el CDK atrapa
  el foco (`autoFocus: 'first-tabbable'`) solo con el contenido ya adjunto, estrictamente después del cableado.
  Se descarta `waitForTimeout`: haría lo mismo a ojo y sin declarar de qué depende. Documentado en el javadoc de
  `abrir()` como hallazgo medido. Tres corridas seguidas en verde después.
  CONTROL DE VACUIDAD (sustituye a M3 en el e2e, obligatorio): C1 repeticiones 3→2 → rojo en el aserto principal
  (3 vs 2): mide cantidad, no presencia. C2 omitir la jornada → rojo ANTES del aserto, en la precondición
  (`.prevalidacion-limpia` ausente): el test recorre la cadena entera. C3, el que importa: revertir el arreglo de
  `lanzarGeneracion` → rojo, `.instancia` en 0 tras 45 s, con el snapshot mostrando «No se pudo cargar el horario
  1 (404).» y sin rejilla. Ninguno falló por vía distinta de la prevista; las tres restauraciones por diff, la de
  C3 además contra HEAD.
  ENTREGADO en tres commits de código. `test(e2e): aisla el e2e en educhronos-e2e.db y corrige la premisa
  caducada del andamiaje` (`playwright.config.ts`: `rm -f app/educhronos-e2e.db*` encadenado al command
  —`shell:true` verificado en el runner—, `-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:educhronos-e2e.db`,
  `reuseExistingServer:false` SOLO en backend, y el párrafo falso reescrito). `5d63fd4` fix(horario)
  (`idCargado` como campo plano con su porqué, asignado como primera línea de `cargar`, bifurcación en el `next`,
  javadoc reescrito con las dos ramas y la causa). `88919d2` test(horario) (casos (39) y (40), con el re-stub
  local y su javadoc, y precondición añadida al (40) —sin ella el tercer aserto podía medir un estado limpio de
  nacimiento—). `a69734a` test(e2e) (`app/frontend/e2e/centro-minimo.spec.ts`, UN SOLO test por R-e2e).
  Suites: vitest 269 → 271 (+2); e2e 1 → 2 (humo 298 ms, centro mínimo 2,8 s); backend INTACTO 261/91, no
  ejecutado (no se tocó `app/src`). `npm run build` verde; el warning de presupuesto (507,66 kB) es PREEXISTENTE
  —verificado construyendo con los ficheros en stash: HEAD ya daba 507,59 kB—, el cambio aporta 70 bytes.
  `solver/src/main` NO tocado ⇒ `referencia-codigo-solver.md` NO regenerada. `modelo_datos_fase1.md` NO tocado.
  AISLAMIENTO VERIFICADO, que es lo que hace repetible el e2e: tras nueve corridas, `app/educhronos.db` conserva
  mtime y tamaño byte a byte (14:01:08.613238078, 155648), y `app/educhronos-e2e.db` refleja el centro mínimo
  (1 nivel, 1 grupo, 1 subgrupo, 1 profesor, 1 asignatura, 1 aula, 1 actividad, 1 plaza, 1 horario, 3 sesiones,
  35 tramos). El `.gitignore` no se tocó: `*.db` ya cubría la BD nueva (medido antes de editar). Matiz anotado y
  no arreglado: `*.db` no cubre `-journal/-wal/-shm`, hueco PREEXISTENTE e idéntico para la BD de trabajo.
  DEUDA: nacen CUATRO, ninguna se paga (R-deuda: ninguna bloquea el criterio). D-doble-proyeccion-compartido
  (de test, cuelga de O-ajuste-cierre), D-e2e-retry-bd, D-e2e-aislamiento y D-props-test-obsoleto (las tres de
  O-estructura). Y D-F8.6-ii-a se AFINA por tercera sesión consecutiva.
  R-terminado RESPETADA: no se pagó D-F8.6-ii-a pese a tener un dato nuevo sobre ella, ni D-horario-irreversible
  (con BD limpia por corrida no muerde), ni D-error-generacion-pin (el `error:` de `lanzarGeneracion` se dejó
  intacto a propósito), ni se homogeneizó el doble de `getProyeccion`, ni se tocó el hueco del `.gitignore`, ni
  el acabado visual de ninguna pantalla (O-diseño). Lo único que entró fuera del test es el arreglo del hueco,
  y entró porque sin él la pata 3 no se cumple.
  HIGIENE (M1-bis): archivada S110 a `bitacora-sesiones.md` (promovida a `### Sesión 110`, insertada al final en
  orden ascendente, cuerpo verificado idéntico por diff); degradada S111 a «Última sesión registrada (previa)»
  compacta; S112 queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S110), la
  crónica de archivado y la frase de ventana del plan.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado que los tres commits de código separan andamiaje,
  producción y tests, que documentación y código van en commits distintos, y que el árbol quedó limpio (ningún
  `.db` entra: `*.db` los tapa). COSTURA SALDADA que el M0 detectó: `gestion_proyecto.md` §2 y §3 seguían
  diciendo «4 piezas hechas / faltan C-niveles, PDC, tutores y el e2e», desactualizado desde S111.
  NOTA TÉCNICA PARA QUIEN ESCRIBA EL PRÓXIMO e2e (sustituye a la de S111, que queda archivada con ella): (1) la
  BD del e2e es `app/educhronos-e2e.db`, la borra el `command` del `webServer` y NO la demuele `schema.sql`;
  correr el e2e exige el backend de dev PARADO (`reuseExistingServer:false`). (2) Tras abrir un diálogo del CDK
  hay que esperar el foco DENTRO de él antes de teclear, o el `writeValue('')` del ControlValueAccessor borra el
  primer campo en silencio. (3) `getByRole(..., { name })` es SUBSTRING: sin `exact:true`, «Guardar» casa con
  «Guardar jornada», y el texto del botón de alta se repite en el `<h2>` del diálogo. (4) Los cuatro
  `<select multiple>` no llevan `formControlName`: van por clase y solo aceptan `selectOption`, que dispara el
  `change` que el handler escucha. (5) El código de actividad NO se pinta en la rejilla; lo contable es
  `div.instancia`.
  O-estructura (H2) ACTIVO, 6 piezas hechas (jornada S107, subgrupos S108, actividades S109+S110, niveles S111,
  e2e S112). TERCERA PATA DEL CRITERIO CUMPLIDA. Suites: vitest 271, e2e 2, backend intacto 261/91. Siguiente:
  PDC es el candidato dominante —única pieza de la que dependen las dos patas restantes—, con tutores detrás y
  pendiente de medir; lo fija su propio M0 (ver M1-ter).

### Sesión 113 — O-estructura (H2): C-pdc. Alta/consulta/borrado del grupo PDC por UI desde su padre (Config/UI, M3 real) + dos guardas de backend que impiden que el CRUD plano deshaga el agregado (Desarrollo). SÉPTIMA pieza. NO cierra el objetivo.
  Decimotercera sesión bajo el mapa Hito→Objetivo→Cambio. Tipo MIXTO: Desarrollo con M3 real (las dos guardas
  de backend, campaña de 4 mutaciones) + Configuración/UI con M3 real (los tres estados del diálogo, gobernados
  por la respuesta del servidor y no por `DIALOG_DATA`, campaña de 3 mutaciones + 3 en el cableado). SÉPTIMA
  pieza de O-estructura. Lo que ENTREGA, y es lo que importa del Cambio: el caso §6.2 del modelo —en su versión
  válida, la Nota (Sesión 23)— se construye ÍNTEGRAMENTE por pantalla y el solver produce horario sobre él.
  Con eso la SEGUNDA PATA del criterio queda demostrada en su caso más difícil. O-estructura sigue ACTIVO:
  falta cerrar tutores, cuya exigencia por el criterio NO está medida (ver M0).
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-estructura (H2), ACTIVO desde S107 con 6
  piezas hechas. Hito = H2. Cambio = C-pdc, que es el «editor de PDC (D7)» de la lista de Cambios que agrupa el
  objetivo. R-invalidación sin conflicto (O-diseño depende de H2 cerrado, O-demo de O-estructura). R-deuda:
  ninguna deuda abre la sesión; las cuatro nacidas en S112 y D-F8.6-ii-a siguen sin bloquear el criterio.
  DECISIÓN DE ORDEN, contra el empate aparente de los dos candidatos: PDC no compite con tutores. §3 dice
  literalmente que las dos patas vivas dependen de PDC; tutores aparece en el PROPÓSITO del objetivo y en sus
  «Cambios que agrupa», pero NO en el criterio de terminado, así que por R-terminado no puede decidir el orden.
  Tutores tendrá su propio M0 y su propio M2. DOS AVISOS levantados en el M0 y ambos resueltos por la medición:
  (1) la ficha de S69 anunciaba MOCKUP PREVIO para la creación del PDC «por depender de cómo se gesticula» —
  VERIFICADO que ya no aplica: S76 dibujó ese mockup y cerró sus decisiones gestuales (D1-7 ruta única desde el
  padre, D1-9 sección y no pestaña, D1-8 marcado de celda), y su propia cabecera dice «el mockup NO se versiona;
  sobreviven sus decisiones»—; (2) la lista blanca de `GrupoService` bloquea DOS tipos, no uno, así que se
  levantó la duda de si la pata 2 exige además crear grupos `VIRTUAL_OPTATIVA`.
  ALCANCE FIJADO EN M0, con dos recortes explícitos. DENTRO: alta, consulta y borrado del PDC por UI desde su
  grupo padre, hasta reproducir el §6.2 por formulario en su versión VÁLIDA —la Nota (Sesión 23), no el cuerpo
  de la sección, marcado como superado: UN solo grupo Di con padre, subgrupo con `grupos={PDC}`, compartidas
  que se quedan en el ordinario—. FUERA por R-terminado: el marcado de celda heredada (D1-8), el resumen «N
  propias · M heredadas» (D1-10) y la derivación de sesiones compartidas en cliente (D1-1), las tres VISTA DE
  HORARIO (familia 8.6/H1) y no criterio de O-estructura. FUERA por R-e2e: ningún e2e nuevo; la suite de
  navegador se queda en 2.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL, cinco investigaciones encadenadas antes de teclear, con las
  cinco premisas de partida declaradas para que la medición las confirmara o las desmintiera; informe consumido
  aquí, sin fichero suelto commiteado). Dos premisas desmentidas, y una de ellas CAMBIÓ el tipo de sesión.
  (P4) CONFIRMADA y RECORTA el alcance: ningún caso del §6 exige `VIRTUAL_OPTATIVA`. Tres medidas
  independientes coinciden —la cadena no aparece ni una vez en todo el §6 del modelo; los 44 fixtures del
  solver declaran 0 grupos de ese tipo; y estructuralmente no podrían, porque `solver.domain.TipoGrupo` no
  tiene la constante y `CatalogoMapper` lanza si se la encuentra—. Los casos que uno esperaría que la
  necesitaran (§6.3, §6.4) están modelados con subgrupos multi-grupo, no con grupos virtuales. La constante
  del enum de `app` está hoy SIN NINGÚN CONSUMIDOR. Consecuencia: la pata 2 no necesita ese formulario.
  (P2) CONFIRMADA sin matices: no hay ninguna vista de detalle de entidad de catálogo donde alojar la «sección
  en la ficha del padre» que decidió D1-9. `app.routes.ts` tiene tres rutas planas sin `children`, las ocho
  secciones de `Configuracion` son lista + diálogo, y la única ruta con id es `horario/:id`.
  (P1) DESMENTIDA EN PARTE: el sub-recurso está bien construido —alta/consulta/borrado transaccionales, con las
  cinco validaciones en un solo sitio, el subgrupo mono-Di con población `{PDC}` por la regla S23, la herencia
  del TUTOR_PRINCIPAL del padre, cascada explícita en el borrado y 11 tests de endpoint—, pero (a) NO EXISTE
  EDICIÓN (`PdcController` no tiene PUT ni PATCH: un PDC no se renombra, se borra y se recrea) y (b) el vínculo
  PDC↔subgrupo mono-Di es una CONVENCIÓN DE CADENA, no una referencia: `PdcService.borrar` localiza el subgrupo
  por `pdc.getCodigo() + "-Completo"`.
  (P3) DESMENTIDA, con matiz que sí importa: ya hay dos llamadas vivas a sub-recursos anidados
  (`/api/horarios/{id}/proyeccion` y `/{id}/diagnostico`), así que el molde de cliente EXISTE y se copia. Lo que
  PDC estrena es otra cosa: es el primer sub-recurso de CATÁLOGO, el primero con ESCRITURA y el primero cuyo id
  sale de una fila elegida en una lista. Los dos sub-recursos de catálogo con backend (`/{id}/tutoria`,
  `/{id}/aulas-compatibles`) siguen sin cablear y declarados fuera de alcance en el propio código.
  (P5) DESMENTIDA, y es el hallazgo que decidió la sesión. Construible sí es —ningún selector filtra: el
  subgrupo del PDC aparece en el multiselect de plaza, el grupo PDC aparece en el de `subgrupo-form`—, pero el
  PDC creado conviviría con DOS listas de catálogo que no saben que existe y que pueden deshacerlo por CUATRO
  caminos, todos con los botones que la pantalla ya ofrece: (1) la fila del PDC en la lista de grupos trae
  «Editar», y basta abrir el diálogo y pulsar Guardar sin tocar nada para degradarlo a ORDINARIO —el formulario
  inyecta `tipo:'ORDINARIO'` fijo, `validarTipo` solo mira el tipo del REQUEST y `entidad.actualizar` conserva
  `grupoPadre`—, quedando un grupo ordinario con padre, invisible para el sub-recurso; (2) renombrar el
  subgrupo mono-Di deja el DELETE del PDC en 404 PERMANENTE (convención de cadena); (3) borrarlo deja el PDC
  huérfano y, de rebote, borrable por el CRUD plano porque `contarSubgrupos` cae a 0; (4) añadir el grupo padre
  a la población del mono-Di es ACEPTADO por el backend y produce el INFEASIBLE que la regla S23 existe para
  evitar (52 h en 30 tramos) — y es el peor de los cuatro porque no rompe nada visible: el catálogo queda
  coherente y lo único que pasa es que el solve deja de tener solución, sin una sola pista. HALLAZGO MENOR
  derivado de (1), medido: con el hijo degradado, la guarda «un PDC por padre» deja de verlo, se puede crear un
  segundo hijo, y entonces `findByGrupoPadre_Id` (que devuelve `Optional`) revienta con 500 en vez de 400.
  DECISIÓN DE ALCANCE tras el M2, la única de peso de la sesión: las dos guardas ENTRAN, y NO se registran como
  deuda. Razón: R-deuda protege lo que ya existía, y los cuatro caminos son HOY INALCANZABLES por UI —sin PDC
  creado por pantalla no hay fila de PDC en la lista de grupos ni subgrupo mono-Di en la de subgrupos—. Los
  abre este Cambio, luego los tapa este Cambio. Sede: BACKEND, no cliente, siguiendo la decisión de S109 (una
  regla que el contrato no tiene diverge y engaña). El tipo de sesión pasó de Configuración/UI a MIXTO por esto.
  FASE 0 — LAS DOS GUARDAS. **G1** (`GrupoService.editar`): rechaza con 400 si LA ENTIDAD EXISTENTE no es
  ORDINARIO, comprobación sobre la entidad cargada y no sobre el request, que es justo la dimensión que
  `validarTipo` no cubría. **G2** (`SubgrupoService`, helper `esMonoDiDePdc` invocado desde `editar` y desde
  `borrar`): un subgrupo cuya población es EXACTAMENTE UN grupo `DIVERSIFICACION_PDC` pertenece al agregado PDC
  y no se toca por el CRUD plano. El «exactamente uno» NO es detalle de implementación y se corrigió en
  sesión: la primera formulación del arquitecto era «población que INCLUYA un grupo PDC», y habría roto el
  ámbito compartido de 4ºESO que S29 modeló como un subgrupo con los DOS Di dentro, caso legítimo del §6 que el
  criterio exige poder construir. Código 400 y NO 409: 409 está reservado en todo el proyecto a
  `ReferenciaEntranteException`, y esto es validación de entrada (misma corrección que S76 aplicó a «un PDC por
  padre»). DESVIACIÓN NECESARIA detectada por Claude Code y no dictada: el DELETE de `SubgrupoController` solo
  traducía `NoSuchElementException` y `ReferenciaEntranteException`, así que G2 en borrado se escapaba como 500;
  se añadió el `catch (IllegalArgumentException)` del patrón del PUT, antes del de referencia entrante (que
  extiende `RuntimeException`, no `IllegalArgumentException`, así que el orden no captura de más). NO se tocó
  `GrupoService.borrar`: su 409 «por accidente afortunado» deja de serlo con G2 puesta, porque el mono-Di ya no
  se puede borrar y el conteo nunca cae a 0; lo único defectuoso es el TEXTO del 409, cosmético → deuda.
  M3 DE BACKEND — 4 mutaciones, una por dimensión, todas cazadas y ninguna arrastrando tests ajenos: quitar G1
  cae (1) y (2) y nada más; quitar G2 de `editar` cae solo (4); quitarla de `borrar` cae solo (5); y relajar el
  predicado a «contiene algún PDC» cae SOLO el caso del ámbito compartido, que es exactamente su cometido. Dos
  decisiones de los tests que cambian lo que miden: (1) manda `tipo:"ORDINARIO"` en el cuerpo, que es lo que
  manda la UI —un test que enviara `DIVERSIFICACION_PDC` seguiría verde con G1 quitada, lo pararía la lista
  blanca de siempre y no discriminaría nada—; y (3) edita también EL PADRE de un PDC, mejora sobre lo dictado
  (se pidió «un ordinario suelto»): es ahí donde se pone roja una G1 mal formulada sobre la RELACIÓN
  («tiene hijos») en vez de sobre el TIPO, que bloquearía renombrar 3ºA tras darle su Di, justo lo que §6.2
  pide poder hacer. Suite app 261 → 268. Commits `d32c14b` (producción) y `8e9e64a` (tests).
  FASE 1 — MODELO Y SERVICIO. Se DESCARTA `pdc.model.ts`, anunciado en el plan de fases y corregido antes de
  teclear: el sub-recurso devuelve `GrupoDTO`, luego su tipo de cliente es `Grupo`, que ya existe; un fichero
  aparte obligaría a duplicarlo o reexportarlo. `PdcRequest` (un solo campo, `codigo`) vive en `grupo.model.ts`.
  `pdc.service.ts` SÍ es propio, y no por simetría: el precedente exacto son `horario.service` y
  `diagnostico.service`, dos servicios separados para dos sub-recursos de `/api/horarios`. Tres wrappers
  pelados. DECISIÓN: el servicio NO traduce el 404 de `obtener` —es un ESTADO legítimo («este padre no tiene
  PDC»), pero traducirlo aquí sacaría la lógica del componente que la va a probar—; escrita en el javadoc con
  su motivo y congelada por el caso (2). AMBIGÜEDAD ASUMIDA A CONCIENCIA: el backend devuelve 404 tanto para
  «no tiene PDC» como para «el padre no existe», indistinguibles desde el cliente; con el padre recién listado
  en pantalla el segundo caso exige que lo borren en otra pestaña, y no se inventa una distinción que el
  contrato no da. Suite vitest 271 → 275. Commits `a3083b3` y `c5f91b9`.
  FASE 2 — EL DIÁLOGO, donde está el M3 de la parte de UI. `pdc-dialogo` recibe como `DIALOG_DATA` el GRUPO
  PADRE y deriva su estado de la RESPUESTA DEL SERVIDOR, que es lo que lo saca del molde de form de catálogo:
  el molde tiene dos ramas gobernadas por el dato de entrada (alta si `null`, edición si viene entidad) y aquí
  la segunda rama no es un formulario sino una ficha con un botón de borrar, porque el backend no tiene
  edición. Se reutiliza todo lo demás (diálogo CDK, `ConfirmarBorrado`, signals `protected`, `mensaje()`
  propio, cierre con `true`). TRES ESTADOS Y NO DOS: además de `sin-pdc` (404) y `con-pdc` (200) está
  `cargando`, y es estado INICIAL a propósito —con la app zoneless y una petición en vuelo, pintar el alta
  mientras el GET viaja enseña «este grupo no tiene PDC» a un grupo que sí lo tiene: un parpadeo que miente—.
  Cuarto estado `error` para cualquier status que no sea 404, que NO ofrece el formulario de alta (no se sabe
  si hay PDC, y ofrecerlo llevaría a un 400 confuso). Modelado con un tipo unión `EstadoPdc` y UN signal, no
  con booleanos: cuatro booleanos independientes admiten 16 combinaciones de las que 12 son imposibles, y el
  spec tendría que enumerar las que no deben darse en vez de afirmar una igualdad; la plantilla usa `@switch`,
  así que las ramas son excluyentes por construcción y no por disciplina. DECISIÓN sobre el aviso de borrado:
  NO nombra `{codigo}-Completo`. Ese sufijo es una derivación del backend (`PdcService.SUFIJO_SUBGRUPO`) y
  reproducirlo en la UI sería una segunda fuente de verdad que se desincroniza sola; el texto dice qué se
  borra sin escribir la convención. M3 — 3 mutaciones, las tres cazadas: estado inicial `sin-pdc` cae (2);
  discriminar por `err` en vez de `err.status === 404` cae (5); cerrar con `true` cuando el alta falla cae (7).
  DESVIACIÓN DE MOLDE JUSTIFICADA: el spec dobla `PdcService` con `vi.fn()` en vez de usar
  `HttpTestingController` como `grupo-form.spec`, porque el escenario de (2) exige un observable PENDIENTE
  (`Subject` que nunca emite) y con un `flush` no se puede montar «la petición aún no ha respondido». Suite
  vitest 275 → 284. Commits `dff0283` y `4a7a87b`.
  FASE 3 — CABLEADO. Vuelve la columna `Tipo` a la lista de grupos (pintada legible: «Ordinario» / «PDC», con
  el valor crudo si llega uno desconocido) y entra la tercera acción por fila. DECISIÓN sobre el punto de
  entrada, tomada en M2 y ejecutada aquí: acción por fila que abre un diálogo, NO ruta hija. Una ruta
  `/grupos/:id` obligaría a resolver aquí la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio propio
  —y a fijarla con un solo ejemplo delante, exactamente lo que aquella sesión se negó a hacer—. La acción por
  fila es el molde canónico sin estirar y cumple D1-7 y D1-9 en su intención (entrada única colgada del padre,
  sin ocupar sitio permanente en los 23 de 28 grupos sin PDC): la «ficha» de S76 era el envase que el mockup
  tenía a mano, no el requisito. Las filas de PDC no ofrecen NINGUNA de las tres acciones, porque las tres
  fallarían (Editar → 400 de G1; Borrar → 409 del mono-Di; PDC → 400, el sub-recurso exige padre ORDINARIO) y
  no se pierde capacidad: no hay edición en backend y el borrado vive en el diálogo del padre. Tipo desconocido
  → sin acciones, el lado seguro. Corregidos además DOS COMENTARIOS QUE HOY SON FALSOS —el de `grupo.model.ts`
  («siempre ORDINARIO en este flujo») y el de `grupo-lista.ts` («si algún día esta pantalla pasara a listar
  también PDC…»)—: no es pulcritud, es el hallazgo aplicado de S110 (un comentario falso hace que el siguiente
  lector decida sobre una premisa falsa). Suite vitest 284 → 290. Commits `c278202` y `d9a0d6a`.
  HALLAZGO DE MÉTODO EN LA CAMPAÑA DE LA FASE 3, reportado por Claude Code en vez de darse por bueno: la
  mutación M3 (`cambiado !== undefined` en lugar de `=== true`) NO puso rojo NADA en el primer intento, y era
  culpa del test, que cerraba el diálogo con `undefined`. El valor que separa las dos formulaciones es `false`,
  justo el que emite el botón de cerrar del propio diálogo. El caso se REHIZO —no se maquilló— ejerciendo los
  dos valores de salida sin escritura (`false` y `undefined`), con `toHaveBeenCalledTimes(2)` para que un botón
  inerte no dé el mismo verde; de paso cubre la relajación simétrica (`!== false`), que la versión original
  tampoco cazaba. Mismo género que el hallazgo de S111 sobre M6: un caso que parece cubrir una dimensión y no
  la cubre es peor que no tenerlo.
  M4 — VERIFICACIÓN EN NAVEGADOR con Playwright EFÍMERO (andamiaje en `/tmp/m4-s113/`, NO en
  `app/frontend/e2e/`; backend contra una BD dedicada fuera del repo; borrado al terminar y árbol verificado
  limpio — R-e2e respetada, la suite permanente sigue en 2). Cuatro bloques.
  (B1) EL CASO §6.2, CONSTRUIDO ÍNTEGRAMENTE POR LA UI. 16 entidades por formulario más las 2 que el backend
  crea por su cuenta: jornada, nivel 3ESO, grupos 3A/3B/3C, el PDC 3PDC creado por el diálogo desde la fila de
  3C (que arrastra `3PDC-Completo` con población `{3PDC}`), subgrupos No-Di, profesores, asignaturas, aulas, y
  las actividades del tronco alternativo sobre el subgrupo del Di más UNA compartida que se queda en el
  ordinario, que es el punto 3 de la Nota S23. El horario SALE: prevalidación limpia y 8 sesiones, con el
  tronco entero en 3PDC y la compartida en 3C — exactamente el reparto que decide la Nota. NADA DEL CASO
  RESULTÓ INEXPRESABLE POR FORMULARIO. Esto es la segunda pata del criterio, demostrada en su caso más difícil.
  (B2) Los tres estados del diálogo, confirmados en navegador, incluido que un SEGUNDO PDC sobre el mismo padre
  no es alcanzable por UI (en `con-pdc` el diálogo no pinta ni un `<input>`: la guarda de backend existe pero
  la pantalla no puede provocarla).
  (B3) LAS DOS GUARDAS, desde el navegador: la fila del PDC pinta 0 botones y la del padre 3; el mono-Di sigue
  ofreciendo Editar y Borrar en su lista y AMBOS son rechazados sin destruir nada; el DISCRIMINANTE de G2
  —añadir el grupo padre a la población del mono-Di— queda rechazado, que era el camino que dejaba el catálogo
  coherente y el solve sin solución; y el CASO LEGÍTIMO sigue vivo (un subgrupo de dos grupos no PDC se edita
  con normalidad). (B4) El borrado del PDC se lleva el grupo Y su subgrupo, y falla sin destruir nada cuando
  una plaza retiene al mono-Di.
  DEUDA — nacen SEIS, ninguna se paga (R-deuda: ninguna bloquea el criterio), más una AMPLIACIÓN.
  D-pdc-lista-rancia (técnica real de UX, la más visible: la lista de subgrupos no se entera del alta ni del
  borrado de un PDC), D-pdc-sin-edicion, D-pdc-vinculo-por-cadena, D-pdc-sufijo-completo, D-monodi-botones-inertes
  y D-bundle-presupuesto. Nace además D-tokens-inexistentes, que NO cuelga de O-estructura: es transversal y de
  costura (ver R4). Y D-F8.6-ii-a se AFINA por CUARTA sesión consecutiva, esta vez con la medición que su propia
  ficha pedía por adelantado.
  POR QUÉ D-pdc-lista-rancia NO SE PAGA, aunque la abra este Cambio (el razonamiento importa más que la deuda):
  no es del mismo género que las guardas —aquéllas cerraban destrucción silenciosa de datos, ésta es una vista
  desactualizada que F5 resuelve, sin nada corrupto en la BD—; arreglarla exige coordinar componentes hermanos
  dentro de `Configuracion`, que ES la decisión de navegación que S101 aplazó a Cambio propio, y resolverla de
  paso dentro de una sesión de PDC es justo lo que aquella decisión existe para impedir; y no bloquea el
  criterio, cosa que B1 demuestra (el §6.2 se reprodujo entero con la lista rancia de por medio). Mismo
  razonamiento que S112 aplicó a D-F8.6-ii-a.
  MEDICIÓN DE D-F8.6-ii-a QUE SU M2 YA NO TENDRÁ QUE HACER: la ficha de S112 decía que su M2 debía EMPEZAR
  comprobando en ejecución si `server.error.include-message=always` surte efecto. Comprobado aquí sin gastar una
  sesión: la clave ESTÁ en `application.properties` y en `target/classes`, sigue existiendo en la versión de Boot
  en uso, y AUN ASÍ el cuerpo del 400 llega sin `message`. Seis textos literales capturados en pantalla, cinco de
  ellos «Bad Request» o «Conflict». Consecuencia registrada: la primera de las tres opciones del abanico
  («reactivar la clave») queda DESCARTADA por medición, no por hipótesis.
  R-terminado RESPETADA: se frenó D-F8.6-ii-a pese a tener medición fresca; se frenó el arreglo de la lista
  rancia; se frenó tocar `angular.json` por el presupuesto de bundle; se frenó dar edición al PDC (no está en el
  criterio y no hay backend); se frenaron el marcado de celda heredada, el resumen «N propias · M heredadas» y la
  derivación de compartidas, los tres de la vista de horario; y se frenó corregir los nueve `D-nueva-*`
  preexistentes, que se registran en vez de arreglarse en caliente.
  ENTREGADO en NUEVE commits de código (producción y tests separados en las cuatro fases, más `fef6fd9` de
  corrección) y documentación aparte. Suites: app 261 → 268 (+7), vitest 271 → 290 (+19), solver 91 INTACTO,
  e2e 2 INTACTA. `ng build` limpio; el warning de presupuesto pasa de 507,66 kB a 514,42 kB: el cableado suma
  6,76 kB, que es lo que pesa `PdcDialogo` al entrar por fin en el grafo de dependencias (hasta ahora el
  tree-shaking lo descartaba por no estar importado). `solver/src/main` NO tocado ⇒ `referencia-codigo-solver.md`
  NO regenerada. `modelo_datos_fase1.md` NO tocado (ni entidad ni invariante nueva; el §6.2 se REPRODUJO, no se
  modificó).
  HIGIENE (M1-bis): archivada S111 a `bitacora-sesiones.md` (promovida a `### Sesión 111`, insertada al final en
  orden ascendente, cuerpo íntegro); degradada S112 a «Última sesión registrada (previa)» compacta; S113 queda
  como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S111), la crónica de archivado
  y la frase de ventana del plan.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado que los commits de código separan producción de tests en
  las cuatro fases, que documentación y código van en commits distintos y que el árbol quedó limpio (ningún
  `.db` nuevo, ningún informe suelto, `git status --porcelain` vacío). COSTURA DETECTADA Y NO SALDADA EN
  CALIENTE: la familia de tokens `D-nueva`, `D-nueva-1` … `D-nueva-5` se cita en NUEVE sitios de `src` sin
  tener definición en ninguno de los dos documentos de gestión. Es incumplimiento de R4 (todo token citado
  tiene definición viva), PREEXISTENTE y ajeno a este Cambio; se registra como D-tokens-inexistentes en vez de
  corregirse a ojo, porque decidir a qué deuda real corresponde cada cita exige leer los nueve contextos. Lo
  que SÍ se corrigió en sesión es el único token inventado que introdujo esta sesión (`D-nueva-2`, en el
  comentario nuevo de `grupo.model.ts`), sustituido por la regla nombrada en prosa.
  NOTA TÉCNICA PARA QUIEN TOQUE EL AGREGADO PDC: (1) el vínculo PDC↔subgrupo mono-Di es por CÓDIGO DERIVADO
  (`codigo + "-Completo"`), no por FK; G2 tapa hoy el único camino de rename que había, pero un tercer camino de
  escritura hacia `Subgrupo` volvería a morder. (2) El sufijo `-Completo` significa en el backend lo CONTRARIO
  que en el cuerpo de §6.2 del modelo (allí «3ºA-Completo» es ordinario + Di juntos; aquí es SOLO el Di): dos
  convenciones incompatibles en el mismo espacio de códigos. (3) `VIRTUAL_OPTATIVA` existe en el enum de `app`,
  está bloqueado por la misma lista blanca que el PDC y NO tiene ningún consumidor: no lo pide el §6, no aparece
  en ningún fixture y el dominio del solver ni siquiera tiene la constante. (4) El sub-recurso NO tiene edición.
  O-estructura (H2) ACTIVO, 7 piezas hechas (jornada S107, subgrupos S108, actividades S109+S110, niveles S111,
  e2e S112, PDC S113). TERCERA PATA CUMPLIDA (S112) y SEGUNDA PATA demostrada en su caso más difícil (S113, el
  §6.2 por UI con horario generado). Suites: app 268, solver 91, vitest 290, e2e 2. Siguiente: tutores es el
  único candidato de Cambio que queda nombrado en el objetivo, y lo primero que debe hacer su M0 es MEDIR si el
  criterio de terminado lo exige —`requiereTutor` es campo del form de Actividad desde S109 y `ProfesorTutoria`
  existe en JPA desde S77, así que puede estar más cubierto de lo que parece—; si no lo exige, lo que toca es
  cerrar el objetivo. Lo fija su propio M0 (ver M1-ter).

### Sesión 114 — O-estructura (H2): C-tutores. Asignación del tutor del grupo por UI sobre el sub-recurso que existía desde S77 (Config/UI, M3 real). OCTAVA pieza. **CIERRA O-estructura.**
  Decimocuarta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI con M3 real (la conservación de
  los co-tutores al guardar y la derivación de estados por longitud son lógica, no binding), UN SOLO MÓDULO
  (frontend; el backend no se toca en toda la sesión), sin e2e nuevo. OCTAVA pieza de O-estructura y CIERRE del
  objetivo: con ella las TRES PATAS del criterio quedan cumplidas y H2 se queda con O-demo como único objetivo
  pendiente. Lo que ENTREGA: el vínculo `ProfesorTutoria` —que tres de los seis casos del §6 del modelo
  registran en su configuración— pasa a ser introducible por pantalla, y con él la invariante S8 se satisface
  por la vía que el modelo describe, verificado en navegador contra el backend real.
  M0 — apertura verificada contra `gestion_proyecto.md`, y su primera tarea NO era proponer alcance sino
  responder si quedaba algo por construir. Objetivo = O-estructura (H2), ACTIVO desde S107 con 7 piezas. Hito =
  H2. Cambio = C-tutores, la «asignación de tutores» de los «Cambios que agrupa». R-invalidación sin conflicto
  (O-diseño depende de H2 cerrado, O-demo de O-estructura). R-deuda: ninguna deuda abre la sesión; las siete
  nacidas en S113 y D-F8.6-ii-a siguen sin bloquear.
  LA MEDICIÓN QUE DECIDIÓ LA SESIÓN, hecha en el propio M0 sobre el §6 del modelo y no sobre la lista de
  Cambios: tutores figura en el PROPÓSITO del objetivo y en sus «Cambios que agrupa» pero NO en el texto del
  criterio, así que por R-terminado la pregunta era si algún caso del §6 lo exige. Lo exige. La PATA 1 no —los
  ocho tipos de sesión se sustituyen por combinaciones K plazas × N grupos (§1 del modelo) y una tutoría es una
  actividad de una plaza, expresable desde S109—, pero la PATA 2 sí: §6.1 (GH6 tutor de 1ºESO A), §6.5
  (`ProfesorTutoria(1ºBach B) = FIL2`, TUTOR_PRINCIPAL) y §6.6 (`ProfesorTutoria(PAU2, 1ºFPB)`) incluyen el
  registro en su configuración y lo usan en su tabla de verificación para declarar S8 ✅. Reproducir un caso es
  poder introducir SU CONFIGURACIÓN por formulario, y esa fila no tenía pantalla: mismo razonamiento con que
  S106 recortó O-catálogo (9 filas irreducibles, 4 con formulario) y con que S113 justificó el PDC. No cae en el
  recorte de S112, que excluyó la superficie de ERROR de la UI: `ProfesorTutoria` es dato del centro. Segundo
  argumento, menor pero real: `requiereTutor` está en el form de Actividad desde S109, así que la UI ya dejaba
  marcar la casilla sin ofrecer forma alguna de satisfacerla. Conclusión del M0: la sesión NO es de cierre de
  objetivo por vacío, es un Cambio con precedente directo (el sub-recurso de catálogo que estrenó S113) que
  cierra el objetivo al terminar.
  M2 — MEDICIÓN (Claude Code sobre el repo REAL, cinco investigaciones, con seis premisas declaradas para que
  la medición las confirmara o las desmintiera; informe consumido aquí, sin fichero suelto commiteado). Cinco
  confirmadas, una PARCIAL, y el hallazgo importante no estaba en ninguna. (P1) Contrato: `GET|PUT
  /api/grupos/{id}/tutoria` inline en `GrupoController`, reemplazo total idempotente, DTO `TutoriaDTO(profesor,
  rol)` con el profesor por CÓDIGO y el grupo en la URL, 17 tests en `TutoriaEndpointTest`. Matiz que corrige la
  ficha de §4.3: I4 se hace cumplir como «COMO MUCHO un principal», nunca «exactamente uno». Tres detalles que
  el formulario debía respetar y que no se deducen del molde: el GET de un grupo sin tutoría devuelve 200 con
  LISTA VACÍA (no 404), no hay DELETE ni PATCH (borrar es PUT con `[]`), y en ESCRITURA un código de profesor
  inexistente da 404 y no 400, contraintuitivo en un PUT sobre un grupo que sí existe. (P3) CONFIRMADA y
  DEBILITA un argumento de apertura: S8 no es restricción de scheduling —`ModeloCpSat` no menciona tutorías y
  `PrevalidacionService` tampoco—, se verifica en `VerificadorSolucion.verificarTutorias` y `verificar()` solo
  se invoca desde `DiagnosticoService` y el CLI, así que generar devuelve 200 y el `HorarioProyeccionDTO` no
  transporta violaciones. El usuario, HOY, no ve que falte el tutor: ve un filete rojo indistinguible del de un
  solape. El argumento del «caso degradado» pierde fuerza; el argumento principal —el dato no es introducible—
  queda intacto. (P4) CONFIRMADA: `PdcService.java:110` hereda el principal del padre en el alta, solo el
  principal. (P5) PARCIAL, y la lectura literal es falsa: no hay `tutoria.model`/`service`/componentes, pero el
  frontend YA escribe `requiereTutor`, YA pinta la violación S8 y YA enseña un 409 que dice «tutoria(s)».
  ALCANCE FIJADO tras el M2, con cuatro decisiones de diseño y tres recortes. El gesto es botón por fila en
  `grupo-lista` + diálogo, molde `PdcDialogo`, porque S113 ya midió que no existe vista de detalle donde alojar
  una sección en la ficha del padre. (1) El diálogo edita el PRINCIPAL pero guarda la LISTA ENTERA: los
  co-tutores se cargan, se pintan en solo lectura y se reenvían intactos, porque el PUT es reemplazo total y un
  formulario que solo conociera al principal los borraría en silencio. (2) TRES estados y no cuatro: el molde de
  S113 deriva el vacío de un 404 y aquí el GET da 200 con `[]`, luego se deriva de `length === 0`. (3) I4 NO se
  replica en cliente: con un único desplegable el escenario es inalcanzable y el validador sería código muerto
  (familia D-i2-dedup-cliente). (4) «— sin tutor —» seleccionable y control sin `required`, porque elegirla ES
  el gesto de quitar el tutor. FUERA por R-terminado: alta/baja de co-tutores (ningún caso del §6 los pide), la
  columna «Tutor» en la lista (exigiría que `GrupoDTO` transportara la tutoría: mover el contrato por comodidad
  de pintura, el error que D-monodi-botones-inertes decidió no cometer) y el resalte mudo de S8 en la rejilla
  (vista de horario, familia 8.6/H1). FUERA por R-e2e: ningún e2e nuevo; la suite de navegador se queda en 2.
  EJECUCIÓN en cuatro fases, con suite verde y commits separados en cada una. F1 modelo + servicio (vitest
  290→294): la unión de literales para `RolTutoria` ESTRENA construcción en `models/` —el precedente es `string`
  pelado— y se documentó como desviación consciente con el argumento que la sostiene y que queda como
  precedente: campo de LECTURA cuyo enum puede crecer sin avisar → `string`; campo que se ESCRIBE desde un
  desplegable cerrado → unión de literales. Se descartó un helper privado de URL porque el molde (`PdcService`,
  `DiagnosticoService`) interpola la plantilla en cada método. F2 el diálogo (294→307). F3 el cableado
  (307→310). F4 el M4 en navegador.
  M3 — CAMPAÑA DE MUTACIÓN, y las dos mutaciones que valieron algo fueron las NO pedidas. En el diálogo, las
  cuatro planificadas murieron (descartar co-tutores al guardar; derivar el vacío de un 404; `[value]="p.id"`;
  pintar sin esperar al profesorado), pero la quinta SOBREVIVIÓ a los doce casos: cerrar con `close(true)` al
  CANCELAR. El contrato de cierre es asimétrico —`true` significa «hubo escritura»— y un cancelar mentiroso
  provocaría una recarga fantasma; se escribió el caso (13), que afirma `close()` sin argumento y niega que se
  haya llamado a `reemplazar`. En el cableado murieron las tres planificadas y las dos extra, y la segunda extra
  justifica un aserto que faltaba: el botón «Tutoría» abriendo `PdcDialogo` pasaba entero mientras el aserto
  solo mirase el DATO y no el COMPONENTE. Nota de cascada: mutar el cableado para que recargue rompe 4 ficheros
  de test, no 1, por el `verify()` fallido que impide el reset del TestBed (documentado en
  `bloqueo.service.spec.ts`); el rojo que cuenta es el primero.
  CORRECCIÓN DE MÉTODO, y la registra el arquitecto contra sí mismo: el guion de F2 afirmaba como MEDIDO que
  «pintar un `<select>` antes de tener las opciones pierde la preselección». Es falso para el `<select>` único
  —Angular reconcilia, y `grupo-form.spec.ts:174` lo congela desde S104— y era analogía indebida con el
  `<select multiple>` de S108, donde el problema sí es real. Claude Code lo desmintió ANTES de escribir, en el
  paso de medición del guion, y conservó el `forkJoin` por el argumento que sí lo sostiene (el gating de
  estados, con precedente en `PdcDialogo`) reescribiendo el TSDoc con un párrafo explícito sobre lo que NO
  arregla. Es exactamente el rendimiento que M2 promete —«una afirmación sobre el estado del repo que no se ha
  medido se declara como RAZONAMIENTO»— aplicado a una afirmación del arquitecto, no del plan.
  M4 — VERIFICACIÓN EN NAVEGADOR sobre el centro mínimo creado íntegramente por UI, guion desechable en `/tmp`
  (R-e2e), nada commiteado. Orden obligatorio y no casual: `requiereTutor` se marca ANTES de la primera
  generación, porque con un horario ya generado el PUT de la actividad da 409 y D-horario-irreversible dejaría
  el M4 bloqueado sin salida por UI. EL CONTRASTE, que es la prueba del Cambio: horario #1 generado sin tutor →
  `TUTORIA_SIN_TUTOR` con `recursoCodigo: "1ESOA"`, `tramoCodigo: null`, las tres celdas y la descripción que
  nombra la actividad y el grupo; se asigna el tutor por el diálogo; horario #2 → `"violaciones": []`. Verificado
  además: el diálogo abre en «cargado» y no en error con lista vacía (5a), el desplegable trae los profesores
  del centro (5b), «— sin tutor —» no está `disabled` (5c), el tutor llega PRESELECCIONADO al reabrir contra el
  backend real (6), y el gesto de quitar el tutor deja `GET .../tutoria` en `[]` con 200 (11).
  HALLAZGO DEL M4 que obligó a reformular un paso del guion: el paso 9 —«comprueba que el filete rojo del
  horario #1 desaparece»— NO era medible como estaba escrito, porque el guion daba por hecho que el diagnóstico
  es una FOTO del momento de generación y no lo es. `DiagnosticoService` recalcula contra el catálogo VIVO y
  `verificarTutorias` no mira la solución, así que con el tutor ya asignado el horario #1 —el generado sin
  tutor— se presenta hoy impecable. Se midió en la única ventana en que es observable, durante el gesto de
  quitar el tutor, y con los dos horarios a la vez: SIN tutor 3/3 celdas en violación en AMBOS, CON tutor 0/3 en
  AMBOS. El resalte sigue al catálogo, no al horario que se está mirando. Nace de aquí D-diagnostico-no-es-foto.
  DEUDA — nacen CINCO y ninguna bloqueaba el criterio, por lo que el objetivo cierra con ellas vivas
  (R-terminado): D-tutor-pdc-desincronizado (cuelga de O-estructura, cerrado: la herencia del principal al PDC
  corre solo en el alta, y este Cambio la hace visible por primera vez); D-s8-muda, D-diagnostico-no-es-foto y
  D-post-horario-sin-sesiones (las tres de O-ajuste-cierre, superficie de la vista de horario, mismo criterio
  con que S113 dejó fuera D1-8 y D1-10); y D-dialogo-foco-perdido (O-diseño; NO la introduce C-tutores,
  `PdcDialogo` hace lo mismo desde S113, y arrastra que la barrera `:focus` de `centro-minimo.spec.ts` no sirve
  en diálogos con estados). D-bundle-presupuesto anotada: 514,42→520,22 kB al entrar `TutoriaDialogo` en el
  grafo; el techo de 500 kB llevaba desbordado desde antes de la sesión, medido apartando los ficheros nuevos.
  R-terminado RESPETADA en tres sitios: no se construyó el alta/baja de co-tutores, no se añadió la columna
  «Tutor» y no se pintó el mensaje de la violación S8, las tres con razón escrita.
  EL CIERRE, con su parte débil declarada. Las tres patas quedan cumplidas, pero el apoyo de la segunda es un
  ARGUMENTO ESTRUCTURAL y no una reproducción exhaustiva: los seis casos del §6 no se han tecleado uno a uno.
  Lo demostrado es que la única fila que faltaba a §6.1/§6.5/§6.6 ya es introducible y que S8 se satisface por
  la vía del modelo; §6.2 se construyó entero en S113; §6.3 y §6.4 se apoyan en el recorte medido en S113 (usan
  subgrupos multi-grupo, es decir actividades multiplaza, demostradas en S110). Se propuso al arquitecto con el
  hueco a la vista, no a pesar de él, y la alternativa —una sesión más construyendo §6.1 completo por UI— se
  descartó porque no podía descubrir ninguna pieza sin demostrar. Si O-demo destapara un caso inexpresable, es
  hueco funcional de H2 y se afronta allí.
  LIMPIEZA (M1.5): sin frentes cerrados que condensar. R4/costura: script oficial sigue sin existir en el repo
  (mejora de método pendiente desde S101); verificado a mano que los seis commits de código separan feat de
  test, que documentación y código van en commits distintos, que el guion desechable del M4 no entró en el árbol
  y que `app/educhronos-e2e.db` sigue ignorada por `.gitignore`.
  O-estructura (H2) ✔ TERMINADO, 8 piezas (jornada S107, subgrupos S108, actividades S109+S110, niveles S111,
  e2e S112, PDC S113, tutores S114). Desbloquea O-demo, ÚNICO objetivo entre H2 y su cierre. Suites: app 268,
  solver 91, vitest 310, e2e 2. Siguiente: abrir O-demo (ver M1-ter).

### Sesión 115 — O-demo (H2): C-derivación. Descomposición del objetivo y derivación del catálogo del IES real desde los volcados (M0 + M2, entregable documental). ABRE O-demo; NO lo cierra.
  Decimoquinta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo SIN ENCAJE EXACTO en los cuatro de
  `metodo.md`, y se dice en vez de forzar la etiqueta: ritual M0 + M2 completo + M1, sin M3 ni M4, porque
  no hay código que mutar ni contrato de UI que contrastar. Ningún fichero de `app/` ni de `solver/` se
  toca en toda la sesión; las cuatro suites quedan intactas. Lo que ENTREGA: O-demo pasa de ficha sin
  descomponer a objetivo con Cambios nombrados y con el catálogo del centro real derivado, medido y
  escrito, y de paso el argumento estructural con que cerró O-estructura recibe su primera prueba
  externa.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), único objetivo entre H2
  y su cierre desde S114. Hito = H2. Cambio = C-derivación, y AQUÍ EL M0 TUVO QUE CREARLO: la ficha de
  O-demo no tenía «Cambios que agrupa», así que el Cambio no podía leerse de la documentación y
  descomponer el objetivo era trabajo de la apertura, como el M0 de S107 con O-estructura. R-invalidación
  sin conflicto. R-deuda: ninguna deuda abre la sesión; D-horario-irreversible y D-tutor-pdc-desincronizado
  se nombran como candidatas a morder por primera vez y se miden, no se presuponen.
  TRES DECISIONES DE ALCANCE, tomadas por el arquitecto con las consecuencias delante. (1) IES COMPLETO y
  no rebanada representativa, porque hay demo al centro en el horizonte; con ello la salvedad de prioridad
  de O-diseño queda ACTIVADA (ver §3 de `gestion_proyecto.md`). (2) El CRITERIO 6 de Fase 8 SALE de
  O-demo a objetivo propio —O-particiones— después de que el M2 midiera su tamaño; el motivo está en su
  ficha y no es de comodidad: exige materializar `Particion`, aplazada por decisión explícita (D-a, S48).
  (3) La carga de datos entra por la API REST y NO por SQL contra la base. La opción de script contra la
  BD la propuso el arquitecto y se descartó con argumento: las invariantes I1–I7 viven ENTERAS en la capa
  de aplicación y el esquema no las replica (D-F8.2b-iv-a), así que un INSERT no comprueba I2, I7, I5 ni
  el XOR del aula; con 219 actividades derivadas por inferencia, los errores no aparecerían al insertar
  sino como INFEASIBLE opaco o —peor— como horario válido y equivocado. Precedente escrito:
  `SeedCatalogoRunner`, el intento anterior de poblar por debajo de la aplicación, que D-seed-demo declara
  muerto. Por la API cada invariante se comprueba una a una y el error llega identificado y en el momento.
  M2 — MEDICIÓN EN DOS PASADAS (Claude Code sobre el repo real; informes consumidos aquí, sin fichero
  suelto commiteado en la primera). PRIMERA PASADA, con seis premisas declaradas: cinco confirmadas y una
  DESMENTIDA. P1 (el `3º ESO PDC` de los volcados es el PDC de 3ºC) CONFIRMADA por tres vías
  independientes: cruce 31/31 exacto contra `3ºCDi`, correspondencia forzosa 5-a-5 entre códigos `*Di` y
  páginas `* PDC`, 9 celdas idénticas con 3ºC y 0 con A y B, y aula secundaria B03 = aula base de 3ºC. La
  duda que el INFORME-RECONCILIACION dejaba abierta como «correspondencia incierta» queda cerrada por
  medición y no hay que preguntarla al centro. P3, P4, P5 y P6 confirmadas (ver abajo). P2 DESMENTIDA: la
  dicotomía «co-docencia u optativa» no cubre 81 de los 208 slots múltiples —hay 12 slots con misma
  asignatura, profesores distintos y DOS AULAS FÍSICAS distintas, que son desdoble y no co-docencia, y 69
  slots mixtos con asignaturas distintas y a la vez una asignatura repetida en varias vías—.
  CORRECCIÓN DEL ARQUITECTO A LA PRIMERA PASADA, y es lo que salvó la derivación: la unidad de carga que
  usó Claude Code —agrupar por `(asignatura, profesor, conjunto de grupos)`, 308 «actividades»— mide
  CLASES, y la unidad del modelo es la ACTIVIDAD. Lo impone S9 (§5.2 del modelo): dos sesiones del mismo
  tramo no pueden tocar el mismo grupo, así que las K vías simultáneas de un bloque tienen que ser UNA
  actividad con K plazas; como K actividades, S9 les prohibiría coincidir en tramo y la demanda semanal
  del grupo se dispararía muy por encima de sus 30 slots, infactible por construcción. Y el «cuarto patrón
  desconocido» de los 69 slots mixtos NO era desconocido: es literalmente el caso §6.1 de
  `modelo_datos_fase1.md`, escrito con estos mismos datos (bloque CyR/OyD/RefMt, seis plazas, subgrupos
  `1ºA-CyR-Tec`, `1ºA-RefMt-MAT6`…). La segunda pasada se guionizó con §5 y §6.1 como referencia normativa
  obligatoria y con once reglas de derivación explícitas, entre ellas la de tres cláusulas que separa
  desdoble de co-docencia por el AULA y no por la asignatura (aulas distintas y no nulas ⇒ desdoble; una
  nombrada y otra nula con celda hermana ⇒ co-docencia; nula sin hermana ⇒ aula DESCONOCIDA, los 49 casos
  de FPB).
  ENTREGABLES de la segunda pasada, dos ficheros nuevos y dos commits, nada existente tocado:
  `docs/horario-referencia/ESPECIFICACION-CATALOGO.md` (el centro en orden de tecleo, con las ambigüedades
  numeradas) y `docs/horario-referencia/catalogo-derivado.json` (los mismos datos legibles por máquina).
  LAS CIFRAS DEL CENTRO REAL, que son las que dimensionan el resto de O-demo: 815 envíos de formulario en
  total —jornada 1, niveles 8, asignaturas 100, profesores 59, aulas 43, grupos 23, PDC 5, tutores 28,
  subgrupos 329 y actividades 219—, de los que subgrupos y actividades son 548, el 67 %. 334 subgrupos
  (28 `{grupo}-Completo` + 306 parciales), de los que 329 se teclean porque el alta de cada PDC crea el
  suyo sola. 219 actividades con 316 plazas, que describen 632 sesiones semanales y CUBREN LOS 840 SLOTS
  del horario real (28 grupos × 30 tramos) sin huecos ni solapes, con S9 verificada. 38 actividades con
  `asignatura = NULL`, 59 multi-grupo, 4 plazas de co-docencia y 37 plazas con aulas candidatas.
  VERIFICACIÓN CONTRA §6.1, y es el resultado que más vale de la sesión: coincidencia estructural
  COMPLETA, sin ninguna divergencia. El bloque CyR/OyD/RefMt sale como una actividad de 6 plazas, rep=2,
  24 subgrupos, con los seis conjuntos de aulas candidatas idénticos a los escritos en el modelo; el
  bloque Fr2/ALCT sale como cuatro actividades independientes con los profesores, aulas y tramos no
  coincidentes de §6.1 y del Hallazgo K, es decir el criterio de derivación REPRODUCE el hallazgo sin
  ayuda; y 1º ESO A da 30 sesiones/semana, igual que el modelo. Las dos únicas diferencias son
  ortográficas o de preferencia blanda.
  EL CASO «INEXPRESABLE» NO LO ERA, y el arquitecto lo corrigió en sesión. `ActividadService.validarXor`
  rechaza con 400 toda plaza sin aula fija ni candidatas, y las 11 plazas de FPB derivadas no se pueden
  teclear; pero eso no es hueco funcional de H2: la plaza sin aula es configuración inválida por diseño y
  el dato falta en la FUENTE (el horario por aulas no cubre los talleres de FPB, ya registrado en
  INFORME-RECONCILIACION §1). No se toca `validarXor` ni el formulario; se pregunta al centro. TODO LO
  DEMÁS CABE, comprobado uno a uno contra la UI existente: asignatura NULL, plaza con dos profesores,
  actividad de 6 plazas, plaza con subgrupos de seis grupos, `duracionTramos > 1`, PDC con padre y recreo
  no lectivo. Con esto, el argumento estructural con que O-estructura cerró en S114 —«cada pieza que los
  casos del §6 necesitan está demostrada como expresable»— pasa su examen contra el centro real, que era
  el juez natural que la propia ficha de O-demo le señalaba.
  RECLASIFICACIÓN de la ambigüedad A1 del informe («la población real de los 306 subgrupos parciales no es
  derivable»): NO es ambigüedad, es la limitación conocida que D31 registra desde S63 —el dominio modela
  SUBGRUPOS, no ALUMNOS, y que un subgrupo sea partición real disjunta y exhaustiva no lo verifica ningún
  componente, por diseño—. Los volcados no pueden decir qué alumno va a qué vía y el modelo no lo pide. Lo
  que sí importa es que la estructura cuadre, y cuadra: 632 sesiones cubren 840 slots sin huecos ni
  solapes. Sale de la lista de preguntas al centro.
  RETIRADA DEL CAMINO CRÍTICO de un Cambio que el propio arquitecto había propuesto en esta sesión:
  C-configuracion-navegable. El argumento con que lo propuso —el clic sin Ctrl en el `<select multiple>`
  de subgrupos borra la población entera y el PUT lo acepta— exige un subgrupo MULTI-GRUPO que editar, y
  la derivación mide que los 334 subgrupos son TODOS mono-grupo. El riesgo no existe con estos datos.
  Queda la fricción de una página con ocho listas y sin búsqueda, pero la carga es append-only y localizar
  filas solo duele al corregir: pasa a mejora futura (D-configuracion-monolitica), no a camino crítico.
  Es el segundo autoengaño que la medición desmonta en esta sesión.
  DEUDA — nace UNA y ninguna abre sesión: D-gh6-tutor-contradictorio (documental: §6.1 del modelo dice que
  GH6 es tutor de 1ºESO A y el Hallazgo E dice que lo es de 1º Bach A; las dos derivaciones son correctas
  contra el volcado y la contradicción es del modelo, no de los datos). Nace también, del hallazgo (f) de
  la primera pasada, D-configuracion-monolitica (mejora futura). Dos deudas vivas cambian de presión sin
  cerrarse: D-horario-irreversible BAJA de prioridad —con la carga por API la base se rehace en minutos,
  así que la congelación de actividades deja de ser callejón sin salida y C-borrado-horario sale del
  camino crítico— y D-F8.6-ii-a NO muerde al cargador, porque el motivo del rechazo viaja como reason
  phrase y un cliente HTTP lo lee aunque el navegador no.
  NOTA DE ALCANCE QUE QUEDA ESCRITA, no en la memoria de nadie: con la carga por API, el criterio 5 de
  Fase 8 —«configurar un centro desde cero POR LA INTERFAZ»— no queda demostrado a escala real. El e2e de
  S112 lo demuestra sobre el centro mínimo, y la propuesta abierta para cerrarlo a escala es teclear 1º ESO
  completo a mano (cuatro grupos, ~80–100 envíos, contiene el bloque de seis plazas del §6.1 y la
  co-docencia de LCL, es decir lo más difícil del centro). PENDIENTE de decisión del arquitecto al abrir
  la siguiente sesión.
  LO QUE HAY QUE PREGUNTAR AL CENTRO antes de la carga completa, y es la primera vez que D31 tiene
  preguntas concretas en vez de dudas genéricas: (1) las aulas reales de las 11 plazas de FPB —lo único
  que BLOQUEA—; (2) los tutores reales, porque la heurística «tutor = quien imparte TUT» saca a FIL2 como
  principal de cinco grupos, cosa que no viola I4 (que acota principales por grupo, no grupos por profesor)
  pero es implausible: lo probable es que imparta la tutoría lectiva de grupos de los que no es tutor; y
  (3) los itinerarios de 4º ESO, 1º Bach y 2º Bach, que deciden si un subgrupo se reutiliza entre bloques
  (I6) y que son exactamente D31 (b), (c) y (d), vivas desde S28/S32/S36.
  LIMPIEZA (M1-bis): archivada S113 a `bitacora-sesiones.md` (promovida a `### Sesión 113`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S114 a «Última sesión registrada (previa)»
  compacta; S115 queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S113),
  la crónica de archivado y la frase de ventana del plan. R4/costura: script oficial sigue sin existir en
  el repo (mejora de método pendiente desde S101); verificado a mano que los dos commits de esta sesión
  son de documentación y que ningún fichero de código entró en el árbol.
  O-demo (H2) ACTIVO desde esta sesión, 1 pieza (C-derivación). Suites INTACTAS, ningún módulo tocado: app
  268, solver 91, vitest 310, e2e 2. Siguiente: C-cargador, el script que lee `catalogo-derivado.json` y
  puebla el centro por la API REST, condicionado a que el centro responda las aulas de FPB; y la decisión
  pendiente sobre 1º ESO a mano. Lo fija su propio M0 (ver M1-ter).

### Sesión 116 — O-demo (H2): C-cargador. El centro real del IES entra en la base por la API REST (M0 + M2 + M4 + implementación + ejecución; M3 sustituido por la corrida). SEGUNDA pieza. NO cierra O-demo.
  Decimosexta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo DESARROLLO con una salvedad declarada en vez de
  forzar la etiqueta: M3 NO aplica en su forma canónica porque el entregable es utillaje de un solo uso, no
  producto, y su lógica —orden de dependencias, mapeo código→id, idempotencia— queda verificada por la CORRIDA
  REAL contra el backend, instrumento más fuerte y más barato que una campaña de mutación sobre un script.
  Lo verificado en su lugar quedó declarado por adelantado: los conteos leídos POR LA API al terminar coinciden
  con el catálogo derivado. M2 y M4 completos. Ningún fichero de `app/`, `solver/` ni `app/frontend/` se toca
  en toda la sesión; las cuatro suites quedan intactas. Lo que ENTREGA: el IES de Sevilla completo está en la
  base creado por las vías legítimas del producto, y —fuera del alcance previsto, por iniciativa del
  arquitecto— la primera prueba de que ese centro GENERA horario.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), ABIERTO en S115 con 1 pieza.
  Hito = H2. Cambio = C-cargador, leído de los «Cambios que agrupa» que S115 creó. R-invalidación sin
  conflicto, y el sentido importa: O-particiones va DESPUÉS y necesita el centro cargado delante, así que el
  cargador es INSUMO suyo y no algo que vaya a rehacer. R-deuda: ninguna deuda abre la sesión.
  EL M0 CORRIGIÓ LA FICHA. C-cargador constaba «BLOQUEADO hasta que el centro responda las aulas de FPB», y la
  medición contra las propias cifras de la ficha demostró que el bloqueo es PARCIAL: de los 815 envíos, 596 no
  dependen del dato (jornada, niveles, asignaturas, profesores, aulas, grupos, PDC, tutores, subgrupos) y de
  las 219 actividades solo caen las que contienen las 11 plazas sin aula. Como `Plaza` es sub-recurso EMBEBIDO
  en `POST /api/actividades` (no existe `/api/plazas`), la unidad de rechazo es la actividad entera, luego el
  techo era 11 actividades y el suelo cargable 804/815 = 98,7 %. Lo que la respuesta del centro bloquea es
  declarar el centro COMPLETO, requisito de C-generación, NO construir ni ejecutar el cargador. La sesión se
  abrió con alcance «carga completa menos FPB».
  DECISIÓN APLAZADA CON ARGUMENTO: C-carga-manual-1eso no se decide en esta sesión. Se creyó que exigía
  decidirse antes para no duplicar datos, y no es cierto: la base se reconstruye en minutos y el ejercicio
  manual puede correr sobre una base de usar y tirar. Al no haber coste por esperar, se decide DESPUÉS de la
  primera corrida, que puede destapar un C-hueco-* y cambiar qué trozo merece teclearse. La nota de alcance
  del criterio 5 sigue escrita y declarada mientras tanto.
  M2 — medición del repo por Claude Code, seis frentes, toda de solo lectura. (1) Las 11 plazas sin aula están
  en 11 ACTIVIDADES distintas (una mala por actividad), todas de 1FPB y 2FPB: AMO-1FPB, CA-1FPB, CA-2FPB,
  ELE-2FPB, IPE-1FPB, MEC-2FPB, MECSO-1FPB, PI-2FPB, PS-1FPB, Tut-1FPB, Tut-2FPB. El techo del M0 se confirma
  exacto. (2) NO existe utillaje HTTP reutilizable en el repo: cero scripts, cero clientes Java, y el e2e
  `centro-minimo.spec.ts` va íntegro por UI sin un solo helper de API; `SeedCatalogoRunner` está BORRADO del
  árbol y solo sobrevive citado como difunto. El cargador se escribe desde cero. (3) Los ocho POST de creación
  devuelven 201 CON cuerpo y el `id` como primer campo del record, así que el encadenamiento código→id es
  directo; `PUT /api/grupos/{id}/tutoria` y `PUT /api/jornada` no devuelven id (tabla de unión y singleton).
  (4) NO existe NINGUNA anotación de validación de Jakarta en `app/src/main` (cero `@NotNull`, cero
  `jakarta.validation`, sin `spring-boot-starter-validation` en ningún pom): toda la validación es imperativa
  dentro de los `*Service`. (5) Todos los duplicados dan 400, NUNCA 409; el 409 solo lo produce
  `ReferenciaEntranteException` en los DELETE, el PUT de actividad y el PUT de jornada. (6) La BD NO se vacía
  al arrancar, comprobado por cuatro vías (sin runners ni `@PostConstruct`, sin `data.sql`, `schema.sql` con
  21 `create table if not exists` y cero DROP desde S109, `ddl-auto=none`): la carga puede correr por partes y
  reanudarse. Esta sexta pregunta NO estaba en el plan de la sesión y se añadió a propósito: es barata de
  medir y cara de descubrir tarde, porque un vaciado al arranque evaporaría 815 envíos.
  M2 DESMINTIÓ EL M0 EN UN PUNTO MAYOR QUE EL BLOQUEO DE FPB. `AsignaturaService.java:201-205` y
  `ProfesorService.java:121,:124` exigen `nombreCompleto` no nulo, y el catálogo derivado trae los 100 y los
  59 a `null`: 159 envíos que fallarían con 400 seguro contra 11 por FPB, y en el PRIMER eslabón de la carga.
  El dato falta en la FUENTE igual que las aulas de FPB, así que no es hueco funcional de H2, pero exigía
  decisión antes de escribir una línea. El arquitecto aportó el camino: los nombres están en las leyendas del
  PDF de grupos. Verificado además que los volcados JSON NO los contienen —`RESUMEN-EXTRACCION.md` explica que
  la leyenda se usó como «vocabulario autorizado» para clasificar tokens, pero el esquema de `celdas` solo
  guarda códigos—, luego el PDF es la única fuente y no hay atajo.
  SALVEDAD MEDIDA SOBRE LOS NOMBRES, que acota lo que se puede prometer: el PDF los trae TRUNCADOS y el
  truncamiento está EN EL ORIGEN, no en la extracción; ninguna técnica de lectura los recupera. Cortes
  medidos: 24 caracteres en la leyenda a dos columnas, 35 en la línea `Tutor:`. Dos hallazgos que sí ayudan:
  la línea `Tutor:` da más texto que la leyenda para los 17 profesores que son tutores (GH6 pasa de «Jiménez
  Montes, María de» a «…María de los Ángele»), y el ancho de corte NO es constante entre páginas (`GeH` sale
  «Geografía e Historia» en 4ºESO B y «Geografía e Hist» en 4ºESO D), luego cruzar las 28 páginas recupera
  texto real. Lo que no recupera nadie: los códigos cuya leyenda es el propio código.
  M4 — contraste del contrato ANTES de escribir código, con el contrato viajando dentro del guion para que
  Claude Code intentara FALSARLO y no confirmarlo. CINCO puntos cayeron, y el primero era una afirmación que
  la documentación daba por buena desde S110.
  M4 (1) — **LA AFIRMACIÓN DEL REASON PHRASE ES FALSA, MEDIDA EN EJECUCIÓN.** `gestion_proyecto.md` y la
  ficha de O-demo afirmaban que el motivo del rechazo «viaja como REASON PHRASE» y que un cliente HTTP lo lee
  aunque el navegador no. Medido con `curl --http1.1 -v -i` sobre tres rechazos distintos (nombreCompleto
  nulo, nivel duplicado, XOR de aula roto): la línea de estado llega literalmente `HTTP/1.1 400 ` con la
  cadena VACÍA, y el cuerpo trae exactamente cuatro claves —`timestamp`, `status`, `error`, `path`— sin
  `message`. `error` es solo el texto canónico del código, idéntico en los tres. Ninguna información distingue
  «ya existe» de «payload inválido» desde el cliente. La cuestión del HTTP/2 es irrelevante: Tomcat sirvió
  HTTP/1.1, donde el reason phrase SÍ existe en el protocolo, y aun así llega vacío. Corregido en las tres
  sedes vivas (ficha de D-F8.6-ii-a en §4, ficha de O-demo, y esta cabecera); lo archivado no se toca.
  M4 (1-bis) — POR QUÉ ESO NO ABRE SESIÓN NI PAGA LA DEUDA, que es la decisión estratégica de la sesión. El
  hecho en que se apoyaba la ficha es falso, pero su CONCLUSIÓN —«no muerde al cargador»— se sostiene por otro
  argumento y más fuerte: la prevalidación en seco del catálogo da EXACTAMENTE 11 violaciones, todas del XOR
  de FPB y todas omitidas por diseño, luego un cargador que respete el orden de dependencias y salte lo ya
  existente por listado previo NO DEBERÍA RECIBIR NI UN SOLO 400. Un 400 deja de ser un caso a clasificar y
  pasa a ser un bug del cargador. De ahí la regla que entra en el contrato: cualquier respuesta no-2xx es
  FATAL, el cargador para en seco y vuelca petición y respuesta. R-deuda se aplica en su literalidad: la deuda
  no bloquea el criterio del objetivo activo y hay camino alternativo, así que no se paga aquí.
  M4 (2) y (3) — LA REGLA DE NOMBRES DEL CONTRATO SE SUSTITUYE ENTERA. «La variante más larga» resultó (a) NO
  DETERMINISTA en empate —`CyR` tiene dos variantes de 22 caracteres exactos y el ganador cambiaba entre
  ejecuciones por la aleatorización de hash de Python, demostrado en 6 corridas—, inaceptable en un fichero
  que se versiona; y (b) PREMIA LA PÉRDIDA DE TILDES: `EF` elegía «Ed. Fisica» (10, sin tilde) sobre
  «Ed.Física» (9, con tilde) porque el espacio suma uno. Regla nueva, sobre variantes NORMALIZADAS
  (minúsculas, sin diacríticos, espacios colapsados, sin espacio junto a puntuación): prefijo gana a prefijado
  (el truncamiento), luego más diacríticos, luego orden lexicográfico, y si ninguna es prefijo de otra es
  CONFLICTO REAL, se elige lexicográficamente y se MARCA.
  M4 (4) — LA CLÁUSULA DEL TUTOR NO DECÍA CÓMO EMPAREJAR nombre con código, y el PDF da el tutor sin código.
  Especificada: casa si la normalizada de la leyenda es PREFIJO de la línea `Tutor:` y el emparejamiento es
  ÚNICO; si casan dos, no se empareja y se marca. Es donde se juega la corrección de 13 nombres.
  M4 (5) — «CAMPO A CAMPO PORQUE EL JSON NO SE PUEDE REENVIAR»: la premisa no se sostiene. Jackson IGNORA
  todos los campos extra en primer nivel y anidados (`_meta`, `_referencia`, `_aulaDesconocida`, `_nota`,
  `orden`, `tramoVolcado`, `creadoAutomaticamentePorPDC`), medido con POST reales. Se conserva la práctica por
  control explícito del payload; cambia la JUSTIFICACIÓN, no el diseño.
  M4 (6) — LA FASE DE IDEMPOTENCIA SE SIMPLIFICA. El contraste señalaba 8 listados + 51 GET individuales
  (tutorías y PDC no tienen listado, y `GrupoDTO` no expone `grupoPadre`). Se resolvió a 8 listados y CERO GET
  sueltos: las 28 tutorías se envían SIEMPRE porque el PUT es reemplazo total idempotente y el estado final no
  depende de lo anterior (verificado: dos PUT iguales y un tercero distinto, sin residuo), y los PDC se
  detectan por su propio código en `GET /api/grupos`.
  M4 (7) — EL ORDEN «TUTORÍAS DESPUÉS DE PDC» ES SEGURO PERO SU RAZÓN NO APLICA AQUÍ. Verificado que la
  herencia solo ocurre si el padre YA tiene tutor en el instante del alta del PDC, y medido que los 5 PDC del
  catálogo tienen EL MISMO tutor que su padre (3ºADi/MAT6, 3ºBDi/BYG2, 3ºCDi/BYG3, 4ºADi/ING6, 4ºDDi/EFI3):
  no hay nada que sobreescribir. Con esto D-tutor-pdc-desincronizado deja de ser condición de orden para esta
  carga. Verificado también que el sub-recurso de tutoría ACEPTA grupos PDC (no hay lista blanca de tipo como
  en `POST /api/grupos`; `TutoriaService.java:92-93` solo hace `findById`).
  M4 (8) y (9) — La base `educhronos-demo.db` a secas nace DENTRO del repo, junto a la base de trabajo, porque
  la ruta es relativa al working directory y con `-pl app` ése es `app/`; se pasa a ruta ABSOLUTA. Y la
  bandera de truncamiento es HEURÍSTICA con falsos negativos: `PTVE | Proyecto Transversal en` está cortado
  por palabra y el ancho no lo detecta. Queda declarado en el propio fichero.
  M4 — LO QUE SÍ SE SOSTUVO, verificado: cobertura 59/59 profesores y 100/100 asignaturas en el PDF de grupos;
  prevalidación en seco con 11 violaciones y ninguna otra familia; coincidencia EXACTA entre la marca
  `_aulaDesconocida` y la regla XOR (mismo conjunto, cero diferencias); los 5 subgrupos automáticos con el
  código y la población que `PdcService.java:100-109` genera; la jornada encajando sin más transformación que
  descartar campos (7 tramos entran, 35 salen, recreo con `ordenEnDia: null`); encoding intacto de ida y
  vuelta con tildes, `º`, espacios y `+`; y el PDF de aulas PRESCINDIBLE por medición (subconjunto estricto:
  cero códigos nuevos, cero variantes más largas, cero líneas `Tutor:`).
  DECISIÓN DE FUENTE, con argumento propio: `Distribución materias ESO.pdf` se miró y NO se incorporó. De los
  15 nombres truncados solo 1 quedaría completo, y es normativa LOMCE de 2016 cuya nomenclatura ya no es la
  del centro. Un nombre truncado REAL vale más que uno completo de otra fuente. `Prematrículas Borrador
  2025.pdf` no se abrió: puede contener datos de alumnos.
  IMPLEMENTACIÓN — `tools/` NACE en esta sesión (no existía; ubicación elegida por el arquitecto).
  `tools/carga-centro/extraer-nombres.py` produce `docs/horario-referencia/nombres-derivados.json`, fichero
  SEPARADO y no parche sobre `catalogo-derivado.json`, por dos razones escritas: ése es el entregable
  commiteado de C-derivación y no conviene ensuciar su diff, y sobre todo la PROCEDENCIA es distinta (el
  catálogo se derivó de la rejilla por geometría; los nombres salen de la leyenda, que es el dato de peor
  calidad). Mezclarlos borraría esa frontera. `tools/carga-centro/cargar-centro.py` hace el join por código.
  EXTRACCIÓN, resultados medidos: 59/59 y 100/100, claves idénticas a las del catálogo. Determinismo
  verificado por md5 en TRES corridas idénticas. Seis códigos con más de una variante y todos resueltos por la
  regla nueva: `CyR`→«Computación y Robótica» (diacríticos), `EF`→«Ed.Física» (diacríticos), `GeH` y `Geogr`→
  «Geografía e Historia» (prefijo), `Tec`→«Tecnología y Digitalizac» (prefijo), y `TPMAR` como ÚNICO conflicto
  real —«Tutoría Orientación» vs «Tutoría diversificación»—, que NO es truncamiento sino el centro usando un
  código con dos rótulos según el nivel; queda marcado, no resuelto. 13 profesores mejoran por la línea
  `Tutor:`, cero ambiguos. 7 asignaturas caen en la cláusula final (ALCT, FOPP, IPE, Latín, PEPA, PTEV, TICO).
  39 marcados como truncados. Erratas conservadas tal cual: `TEC1 | Jiméez López, Juan`. Dato de otra
  naturaleza, señalado y transcrito igual: `REV | Religión Evangélica`, un código de profesor cuya leyenda es
  el nombre de una materia.
  ADVERTENCIA DE MANTENIMIENTO sobre la regla: el caso `EF` sale bien por un mecanismo algo accidental —«Ed.
  Fisica» y «Ed.Física» normalizan igual SOLO porque la regla elimina el espacio junto a la puntuación, y de
  ahí el paso de diacríticos rescata la tilde—. Funciona, pero si alguien toca la normalización ese caso
  cambia de rama sin avisar.
  CARGA — ejecutada contra base nueva. Prevalidación en seco: 11 violaciones, todas XOR de FPB, ninguna otra.
  Informe final leído POR GET, esperado vs. leído, diez familias en verde: niveles 8, asignaturas 100,
  profesores 59, aulas 43, grupos 28, subgrupos 334 (329 enviados + 5 creados por el alta de los PDC),
  actividades 208, plazas 305, tutorías 28, escrituras HTTP 804. Cero errores en el stdout de la aplicación.
  Omitidos y registrados: los 5 subgrupos `*-Completo` de los PDC y las 11 actividades de FPB. Consecuencia
  declarada de la omisión, no error: quedan cargados y SIN USO 9 asignaturas (AMO, CA, ELE, IPE, MEC, MECSO,
  PI, PS, Tut) y 3 profesores (PAU1, PAU2, TEC1). **C-cargador CUMPLE su criterio.**
  IDEMPOTENCIA VERIFICADA, y no estaba en el guion: Claude Code corrió `--cargar` dos veces más a propósito.
  La segunda corrida envía 0 altas y solo los 28 PUT de tutoría, con estado final idéntico. Sin eso la
  propiedad estaba diseñada pero no probada.
  HALLAZGO FUERA DE ALCANCE, por iniciativa del arquitecto y contra el consejo del arquitecto senior: pulsó
  generar. **EL IES REAL GENERA HORARIO.** El mayor riesgo abierto de O-demo —que el centro completo no fuera
  resoluble— queda DESPEJADO. El camino hasta ahí destapó lo demás.
  EL 422, DIAGNOSTICADO. `POST /api/horarios` con cuerpo `{}` respondía 422 sin mensaje alguno en pantalla.
  Motivo medido: el solver AGOTA su presupuesto por defecto de 30 s (`GeneradorHorarioService.java:201`) sin
  encontrar solución, y `SolverHorario.java:117-125` traduce cualquier estado distinto de OPTIMAL/FEASIBLE a
  `HorarioInfactibleException` → 422. Evidencia que descarta la rama de prevalidación y sostiene la del
  solver: el tiempo de respuesta ESCALA con el presupuesto (5 s → 5,7 s → 422; 30 s → 31,1 s → 422; 600 s →
  601 s → **200**, `estadoSolver: FEASIBLE`, objetivo 188.0, cota inferior 0.0, 770 sesiones en la base). Como
  el solver agota el tiempo en vez de terminar antes, el estado NO es INFEASIBLE sino UNKNOWN: **el problema
  es factible y solo necesita más tiempo**; llamarlo infactible es una afirmación FALSA sobre el problema.
  NO ES DEL CATÁLOGO, Y NO TIENE NADA QUE VER CON FPB —descartado con medición, no con hipótesis—. No existe
  ninguna restricción de cobertura total de tramos (`ModeloCpSat.java:166-173`; el único `addExactlyOne`,
  `:938`, elige un aula por plaza, no cubre slots), así que dejar huecos está permitido; y `GRUPO_SOBRECARGADO`
  dispara con demanda > tramos lectivos, muy lejos de FPB. Las entidades sin plaza no generan variable ni
  restricción. El dato de las aulas de FPB SIGUE siendo «bloquea el criterio 5 al completo» y NO asciende a
  «bloquea cualquier generación».
  LA DIFICULTAD REAL, medida, y es dato de primer orden para C-generación: **26 de los 28 grupos deben llenar
  sus 30 tramos EXACTOS, holgura cero.** Es un empaquetado perfecto, y no viene del recorte de FPB —1FPB y
  2FPB son justamente los dos únicos con holgura, +24 y +25—, viene de que el catálogo es un horario REAL
  donde cada grupo tiene la semana completa. Aviso metodológico registrado por Claude Code: la demanda debe
  contarse DEDUPLICADA POR ACTIVIDAD, como hace el no-solape (`ModeloCpSat.java:1046`); sumar por plaza da
  40–73 tramos por grupo y es la sobrestimación que el propio código advierte en
  `PrevalidacionService.java:253-256`.
  POR QUÉ LA PANTALLA NO DIJO NADA. El código SÍ contempla la rama de error y SÍ la pinta
  (`horario-view.ts:329`, `horario-view.html:30-32`), y D-error-generacion-pin NO se cumple en este camino: el
  cuerpo trae `"error":"Unprocessable Content"`, así que `mensaje()` (`horario-view.ts:267`) mostraría ese
  texto y no «El servidor rechazó el pin». La explicación es OTRA y se declara como RAZONAMIENTO, no medición:
  durante los ~30 s del POST la pantalla no cambia en absoluto —sin spinner, sin estado «generando», botón
  habilitado (`horario-view.html:23` solo lo deshabilita si no se ha prevalidado)—, así que lo más probable es
  que se mirara antes de que llegara la respuesta.
  EL MOTIVO NO ESTÁ EN NINGUNA PARTE, y esto CONTRADICE la premisa con que se escribió el guion de
  diagnóstico. El mensaje se construye correctamente en `SolverHorario.java:124` con el estado de CP-SAT
  dentro, y se pierde ENTERO: no viaja en el cuerpo, no viaja en la línea de estado y NO SE REGISTRA EN EL
  LOG (cero coincidencias de `horario|solver|infactib|INFEASIBLE|cp-sat|ortools|422` en el stdout completo
  durante la petición). El arquitecto senior había afirmado que el motivo estaría en el stdout, apoyándose en
  que la carga se validó leyendo ese log; eso solo demostraba ausencia de errores, no que un 422 se registre.
  Corrección registrada. Comprobado además que NO es específico del 422: un 400 con mensaje conocido
  (`maxSegundos: -1`) tampoco lleva `message`, luego afecta a TODAS las traducciones vía
  `ResponseStatusException`, y `server.error.include-message=always` sigue puesto y sin surtir efecto.
  REENCUADRE DE D-log-aplicacion, con evidencia y no con intuición. El arquitecto propuso una sesión dedicada
  a introducir logging; el arquitecto senior argumentó que instrumentar antes de diagnosticar es instrumentar
  a ciegas y que había instrumentos gratis (pestaña Red del navegador, stdout). Los gratis BASTARON esta vez,
  pero solo porque el tiempo de respuesta era medible desde fuera y permitía descartar ramas; con un fallo
  menos obliging no habrían bastado. La deuda pasa de «propuesta razonable» a deuda CON EVIDENCIA DETRÁS. Se
  registra la distinción que ordena el asunto: **los logs son para nosotros; los mensajes en pantalla son para
  el usuario**, y delante del jefe de estudios ningún log salva —lo que hace falta es que el motivo se lea en
  la pantalla, y eso es D-F8.6-ii-a, no D-log-aplicacion—. Ninguna de las dos abre sesión hoy.
  M5 — DOS COSAS QUE NO SE HICIERON, y por qué. (1) `GET /api/prevalidacion` contra el centro cargado es de
  solo lectura y no congela nada, pero es la primera medición del M2 de C-generación: adelantarla habría sido
  empezar el siguiente Cambio con el ritual del anterior. (Acabó midiéndose de todas formas al diagnosticar el
  422, y dio `[]`.) (2) Subir el presupuesto de 30 s es un arreglo de un minuto y una decisión SIN MEDIR: no
  se sabe si bastan 90 o 200, ni cuánto mejora el objetivo con más tiempo. Es el M2 de C-generación, que ahora
  arranca con una pregunta concreta en vez de un frente abierto.
  PREGUNTA ABIERTA, declarada como tal y no como anomalía: el catálogo describe 632 sesiones semanales para
  219 actividades y la corrida de 600 s dejó **770 filas en `sesion`** con 208 actividades. No está medido
  cómo mapea una fila de `sesion` (¿por actividad y repetición? ¿por plaza y repetición?), así que no puede
  decirse si 770 está bien o mal. Es exactamente la cifra que hay que reconciliar contra los volcados usados
  como ORÁCULO DE REGRESIÓN, y es trabajo de C-generación.
  D-post-horario-sin-sesiones CONFIRMADA A ESCALA REAL: el POST devolvió `sesiones: []` con 770 sesiones en la
  base. Su ficha lo predecía desde S114; ya no es hipótesis.
  D-horario-irreversible NO MUERDE en el estado final: la corrida de 600 s sí generó y creó 770 sesiones, y
  Claude Code RESTAURÓ la copia limpia, así que la base queda con `horario_generado 0` y `sesion 0` y
  O-particiones conserva su punto de partida sin generar. La salvaguarda previa al diagnóstico fue lo que lo
  hizo posible.
  LIMPIEZA (M1-bis): archivada S114 a `bitacora-sesiones.md` (promovida a `### Sesión 114`, insertada al final
  en orden ascendente, cuerpo íntegro); degradada S115 a «Última sesión registrada (previa)» compacta; S116
  queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S114), la crónica de
  archivado y la frase de ventana del plan. R4/costura: script oficial SIGUE sin existir en el repo (mejora de
  método pendiente desde S101); verificado a mano que los dos commits de código y datos van separados
  (`a440331` feat(tools) y `f3d772b` docs(horario-referencia)), que ningún guion desechable entró en el árbol
  y que `git status` quedó limpio salvo UN PUNTO que esta cabecera dejó sin nombrar y que S117 corrige aquí: `app/src/main/resources/application.properties` quedó MODIFICADO SIN COMMITEAR apuntando a la base de demo, y las bases sueltas del árbol quedaron sin consolidar. Lo saldó S117 (`git restore` del fichero por el arquitecto; medición de las CINCO bases, ninguna trackeada por git; `educhronos-demo.db` declarada canónica) junto con la regla que evita la recaída: la apuntada a otra base no se hace editando el fichero, sino sobreescribiendo la propiedad en el arranque.
  O-demo (H2) ACTIVO, 2 piezas (C-derivación S115, C-cargador S116). Suites INTACTAS, ningún módulo tocado:
  app 268, solver 91, vitest 310, e2e 2. Siguiente: C-generación, que arranca con tres preguntas ya
  formuladas —qué presupuesto de tiempo necesita de verdad el centro real, cómo se reconcilian las 770
  sesiones contra las 632 del catálogo, y qué dice el contraste con el horario del PDF— y con dos deudas
  nuevas nacidas en su terreno. Lo fija su propio M0 (ver M1-ter).

### Sesión 117 — O-demo (H2): C-generación, primera parte. Caracterización del solve sobre el IES real: reconciliación de `sesion` y barrido de presupuesto en cuatro pasadas (M0 + M2 completo, sin M3 ni M4 canónicos). TERCERA pieza EN CURSO. NO cierra el Cambio ni el objetivo.
  Decimoséptima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo SIN ENCAJE EXACTO en los cuatro de
  `metodo.md`, y se dice en vez de forzar la etiqueta: ritual M0 + M2 completo + M1, sin M3 ni M4 canónicos,
  porque la sesión entera es MEDICIÓN sobre un catálogo ya cargado y no se escribió ni una línea de producto.
  Ningún fichero de `app/src/main`, `solver/src/main` ni `app/frontend` se toca; las cuatro suites quedan
  intactas (app 268, solver 91, vitest 310, e2e 2). Lo que ENTREGA: las dos primeras de las tres preguntas
  que C-generación heredaba de S116 quedan RESUELTAS, y la respuesta a la segunda no es un número sino un
  cambio de forma de la pregunta.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), 2 piezas hechas. Hito = H2.
  Cambio = C-generación, leído de los «Cambios que agrupa» de la ficha, con su enunciado propio
  —factibilidad, tiempo y contraste con el PDF— y sus tres preguntas ya escritas. No había otro candidato:
  C-hueco-* no se planifica y no hay ninguno conocido, C-borrado-horario y C-configuracion-navegable están
  retirados del camino crítico, y C-carga-manual-1eso es una decisión y no la siguiente porción por
  dependencias.
  EL M0 REGISTRÓ UN HUECO DOCUMENTAL DE S116 Y LO CORRIGE ESTA SESIÓN. La cabecera de S116 cerraba su R4 con
  «`git status` quedó limpio salvo el punto de abajo» y ese punto NO EXISTÍA en el documento: el referente se
  perdió al redactar. La higiene que describía —`application.properties` modificado sin commitear apuntando a
  la base de demo, y varias bases sueltas— vivía solo en el prompt de apertura, es decir, en la memoria de
  alguien. Es incumplimiento de R4 en su forma más simple y se corrige en la cabecera degradada de S116, que
  sigue siendo sede viva.
  ALCANCE FIJADO EN LA APERTURA, con una pregunta dejada fuera a propósito: entran la reconciliación
  770↔632 y la calibración del presupuesto; SALE el contraste con el horario del PDF, porque exige decidir
  antes qué se asevera como «válido» y es Cambio de sesión propia. Se declaró además que O-demo NO PUEDE
  CERRAR en esta sesión pase lo que pase: su criterio exige el centro ENTERO y siguen faltando las 11
  actividades de FPB a la espera de la respuesta del centro (D31-a).
  R-invalidación con una condición de orden que la ficha de O-particiones nombra y que se respetó: su prueba
  se hace ANTES de generar o después de que exista un borrado de horario, porque `ActividadService.exigirSinDependientes`
  bloquea con 409 las actividades que ya tengan sesiones. Toda la medición corrió sobre COPIA y sin llamar
  nunca a `guardar(...)`; verificado al cerrar cada pasada: `horario_generado 0` y `sesion 0` en la copia de
  trabajo, y md5 intacto de las dos bases canónicas. O-particiones conserva su punto de partida.
  R-deuda: ninguna deuda abre la sesión. Las dos nacidas en el terreno del Cambio (D-timeout-como-infactible
  y D-motivo-rechazo-sin-registro) se encuadran sin darlas por pagadas y NINGUNA bloquea el criterio, con
  argumento medido y no de comodidad (ver más abajo, «lo que la medición decidió sobre la deuda»).
  LA HIGIENE HEREDADA, MEDIDA Y RESUELTA, y con una corrección al estado que traía la apertura. El arquitecto
  saldó el `application.properties` modificado con `git restore`, lo que dejó el árbol limpio y —efecto no
  buscado— la aplicación apuntando otra vez a la base de juguete: `spring.datasource.url=jdbc:sqlite:educhronos.db`
  es relativa al working dir y resuelve a `app/educhronos.db`, que tiene 1 grupo y 1 actividad. Medido además
  que son CINCO bases y no cuatro, y que NINGUNA está trackeada por git (las cinco caen bajo `*.db` de
  `.gitignore:12`), luego la consolidación nunca fue problema de repositorio sino de orden en disco:
  `educhronos.db` (juguete), `educhronos-demo.db` y `educhronos-demo-pruebas.db` (las dos con el centro real),
  `educhronos-e2e.db` (Playwright) y `educhronos-test.db` (vacía, la que declara el `application.properties`
  de test). Las dos del centro real tienen el VOLCADO SQL IDÉNTICO byte a byte —el md5 difiere solo en el
  contador de páginas de la cabecera SQLite, byte 28— y ambas cuadran con el estado final de S116
  (8/100/59/43/28/334/208/305/28, `sesion` 0). **DECISIÓN: `educhronos-demo.db` es la base CANÓNICA del
  centro real; `educhronos-demo-pruebas.db` se conserva sin borrar y sin uso.** Y decisión de método asociada:
  la apuntada a una base distinta NO se hace editando `application.properties` —así fue como el fichero acabó
  modificado sin commitear—, sino sobreescribiendo la propiedad en el arranque o, como aquí, con `properties`
  inline del arnés. La apuntada vive en el guion, queda escrita y no puede colarse en un commit.
  M2 — MEDICIÓN EN CUATRO PASADAS, todas por Claude Code, todas sobre copia y sin persistir. La primera es de
  solo lectura sobre el repo; las tres siguientes ejecutan el solver. El instrumento se decidió CON DATOS y
  no a priori (ver «el instrumento»).
  M2 (1) — QUÉ ES UNA FILA DE `sesion`, Y CON ELLO LA PREGUNTA 770 vs 632 QUEDA CERRADA. Una fila es una
  PLAZA de una INSTANCIA en un tramo, leído del anidamiento de `SolucionMapper.aSesiones`: `for actividad`
  (:113) × `for indice = 1..repeticionesPorSemana` (:114) × `for plaza : actividad.plazas()` (:129), con el
  `add` en el cuerpo del bucle más interno (:149). El tramo se resuelve UNA vez por instancia (:117, fuera
  del bucle de plazas), así que todas las plazas de una instancia comparten `tramo_inicio_id` y lo que las
  distingue es `plaza_id`; la clave única `(horario_id, plaza_id, indice)` lo confirma. El javadoc de
  `Sesion.java` ya lo decía —«una fila por plaza, no por instancia»— y nadie lo había cruzado con las cifras.
  **Fórmula: filas = Σ (repeticionesPorSemana × nº de plazas).** `duracionTramos` NO multiplica (solo se
  escribe el tramo de INICIO) y además todas las actividades del catálogo tienen `duracionTramos: 1`.
  Resultados sobre `catalogo-derivado.json`: **770 filas con las 208 actividades cargadas y 819 con las 219
  del catálogo completo.** Y **632 es OTRA MAGNITUD**: es Σ repeticionesPorSemana sobre las 219, es decir el
  número de INSTANCIAS semanales, idéntico a Σ |instanciasEnElHorarioReal| (las celdas del PDF), que es de
  donde se derivó. La diferencia 819 − 632 = 187 son las plazas adicionales de las actividades multiplaza
  —bloques de optativas, desdobles, agrupamientos—, que producen varias filas en el mismo tramo. **Nunca hubo
  discrepancia: había dos números midiendo cosas distintas y nadie lo había escrito.** La cifra 632 está
  GRABADA como dato en `catalogo-derivado.json` (`_meta.resumen.sesionesSemanales`) y no la calcula ningún
  script del repo.
  SALVEDAD SOBRE LO QUE 770 DEMUESTRA, para no darle un valor que no tiene: `aSesiones` LANZA excepción si
  alguna instancia no está colocada (:118-121), luego un `guardar` que termina bien implica colocación total
  por construcción. El número confirma que el guardado fue COMPLETO; no dice nada sobre la CALIDAD del
  horario. Eso lo dirá el contraste, que es la tercera pregunta y sigue fuera.
  LO QUE QUEDA COMO ACTIVO: **819 es el oráculo aritmético para cuando lleguen las aulas de FPB.** Si ese día
  el conteo no da 819 exactos, hay fallo de carga o de colocación, y se detecta con un `SELECT count(*)`.
  M2 (2) — EL INSTRUMENTO DEL BARRIDO, decidido con la medición delante y no a ojo. NO existía ningún
  instrumento capaz de resolver el catálogo real sin persistir: cero runners, y los cuatro `@Tag("escala")`
  del solver cargan fixtures JSON, no la base (su fixture son 26 grupos/229 actividades, que NO es este
  centro). Pero las tres piezas están SEPARADAS en el código —`cargarProblema()` es público y de solo lectura,
  `resolverOptimizandoConDetalle()` no toca JPA, y `guardar()` es método aparte—, así que encadenar las dos
  primeras sin la tercera es un solve sin escribir una fila. Se descartó la vía HTTP con
  `--spring.datasource.url` por tres razones acumulativas: cada corrida con éxito escribe 770 filas y congela
  actividades (D-horario-irreversible), obligando a restaurar y reiniciar ENTRE cada punto de medida; cada
  422 devuelve solo texto; y el `ProblemaHorario` se recarga en cada petición. Arnés DESECHABLE en
  `app/src/test`, con dos salvaguardas: corre sobre COPIA (porque `spring.sql.init.mode=always` abre la base
  en escritura, aunque `schema.sql` solo tenga `create table if not exists` desde S109) y NO entra en el
  árbol. Detalle que resultó ser el que evita el accidente: la clase se llamó `BarridoPresupuestoS117`, SIN
  sufijo `Test`, de modo que ninguna convención de Surefire la recoge y solo corre con `-Dtest=` explícito.
  GUARDA DE CATÁLOGO en el arnés, y es lo que hace la medición defendible: aserta `actividades == 208` antes
  de medir nada. Las cuatro pasadas la superaron con 208/28/305/334 y **30 tramos, no 35**: `CatalogoMapper`
  filtra los no lectivos (los cinco recreos), que es la renumeración documentada en D30 y cuadra con los 30
  tramos del fixture de escala. Pre-validación **ERROR=0 y también AVISO=0** en las cuatro pasadas.
  M2 (3) — PASADA 1, SEIS PUNTOS A n=1, Y LA LECTURA QUE HUBO QUE RETIRAR. Vía OPTIMIZACION, semilla 42:
  60 s→312.0, 90 s→279.0, 120 s→258.0, 180 s→247.0, 300 s→217.0, 600 s→208.0, todas FEASIBLE, todas con cota
  0.0 y todas excediendo el presupuesto en 0,6–1,7 s. Se leyó como una curva presupuesto→calidad. **No lo era.**
  La grieta la abrió el contraste con la documentación: S116 midió objetivo **188.0** con 600 s y esta pasada
  da **208.0** con 600 s, mismo catálogo, mismo presupuesto y misma semilla —la 42 es el defecto de la capa de
  aplicación, así que el POST de S116 usó exactamente la misma—. Veinte puntos de diferencia entre dos
  corridas que deberían ser idénticas.
  M2 (4) — PASADA 2, Y LA CAUSA. Repeticiones con la misma semilla, no semillas distintas: medir sensibilidad
  a la semilla antes de saber si dos corridas iguales coinciden es medir la segunda pregunta primero.
  Resultado: **1 éxito en 17 corridas** (30 s ×3, 40 s ×3, 45 s ×3, 50 s ×3, 60 s ×5; el único éxito a 45 s
  con objetivo 386.0). La causa está en un grep que salió VACÍO: no hay `num_search_workers`, ni
  `setNumSearchWorkers`, ni `MaxDeterministicTime` en `solver/src/main` ni en `app/src/main`; las únicas
  llamadas a `getParameters()` son `setMaxTimeInSeconds` y `setRandomSeed`. **Sin fijar el paralelismo,
  CP-SAT corre en todos los núcleos (8 en esta máquina) y corta por RELOJ DE PARED, así que `setRandomSeed(42)`
  no hace la ejecución reproducible.** Las seis filas de la pasada 1 son seis muestras únicas de una variable
  aleatoria, no una curva; el 312.0 de 60 s era una tirada afortunada.
  M2 (5) — PASADA 3, TRES BLOQUES EN TRES JVM SEPARADAS, para neutralizar un confundido que se detectó a
  tiempo: las cinco corridas de 60 s de la pasada 2 fueron las ÚLTIMAS de 17 solves en un mismo proceso, y la
  de la pasada 1 la PRIMERA de seis, así que un efecto de secuencia (heap, GC, memoria nativa de OR-Tools)
  habría sido indistinguible del solver. **Bloque A** (60 s ×3, primeras corridas de una JVM nueva): 0 de 3.
  El efecto de secuencia queda DESCARTADO y la no-reproducibilidad confirmada. **Bloque C** (120 s ×3 → fallo,
  286.0, 240.0; 180 s ×3 → fallo, 222.0, 262.0): los rangos se solapan por completo, luego entre 120 s y 180 s
  no hay diferencia de calidad distinguible. Lo que cambia con el presupuesto NO es la calidad, es la TASA DE
  ÉXITO.
  M2 (6) — BLOQUE B: LA HIPÓTESIS DEL ARQUITECTO SENIOR, FALSADA, Y ES EL HALLAZGO ESTRUCTURAL DE LA SESIÓN.
  Se pidió medir la vía de FACTIBILIDAD PURA (`SolverHorario.resolver`, modelo sin objetivo) con el argumento
  de que si lo caro es la primera solución, quitar el objetivo debería devolver antes; y con la expectativa
  declarada de que sería rápida. Resultado: **0 de 6** (30 s ×3, 60 s ×3), todas UNKNOWN y todas consumiendo
  el presupuesto ENTERO, ni una terminó antes. **El cuello de botella de este catálogo es SATISFACER LAS
  RESTRICCIONES DURAS, no optimizar**, lo que es coherente con los 26 grupos de 28 que empaquetan sus 30
  tramos exactos. Quitar el objetivo no acelera nada.
  M2 (7) — PASADA 4: EL CRONÓMETRO, Y LA RESPUESTA. Todas las corridas anteriores agotaban el reloj por
  diseño, así que nunca se había visto cuánto tarda DE VERDAD en aparecer la primera solución; la vía sin
  objetivo sí devuelve en cuanto la encuentra, luego con presupuesto largo deja de ser semáforo y pasa a ser
  cronómetro. Factibilidad pura, 900 s, 5 repeticiones: **5 de 5 con solución, en 844–872 s.** Rango de 28 s
  sobre una media de 860: dispersión del 3,3 %, el resultado más limpio de las cuatro pasadas. **El catálogo
  del centro real TIENE solución factible y el solver la encuentra de forma FIABLE; el tiempo de primera
  solución de este modelo está en torno a los 14,3 minutos.** El desenlace malo —«el centro real está en el
  límite de lo que este solver resuelve tal como está modelado»— queda DESCARTADO.
  PRECISIÓN QUE EVITA UNA CONCLUSIÓN FALSA, y corrige la lectura natural del informe de la pasada 4: los 860 s
  NO son «el tiempo de primera solución» a secas y las corridas de las dos vías NO son muestras de una misma
  distribución. `resolver` construye el modelo SIN objetivo (`construir()`) y `resolverOptimizandoConDetalle`
  construye otro CON objetivo (`construirConObjetivo()`): son dos modelos distintos resueltos por dos
  búsquedas distintas. Los 860 s describen el primero y no dicen nada sobre el segundo. Puestos uno al lado
  del otro, el resultado va en la dirección CONTRARIA a la esperada: la vía con objetivo da solución 2 de 3
  veces a 120 s, y la vía sin objetivo 0 de 6 a 30 y 60 s. No se afirma que la factibilidad pura sea MÁS
  lenta —no está medida a 120–180 s—; lo que se afirma es que **no es la vía barata a una semilla**, que era
  la hipótesis con que se pidió.
  TASA DE ÉXITO ACUMULADA POR LA VÍA DE PRODUCCIÓN (OPTIMIZACION, único valor de `ViaSolver` y lo único
  expuesto por la API), sumando S116 y las tres pasadas de S117: **5 s 0/1 · 30 s 0/4 · 40 s 0/3 · 45 s 1/3 ·
  50 s 0/3 · 60 s 1/9 · 90 s 1/1 · 120 s 3/4 · 180 s 3/4 · 300 s 1/1 · 600 s 2/2.** Total 35 corridas de
  optimización más 11 de factibilidad pura.
  CONCLUSIÓN SOBRE EL PRESUPUESTO, que es la respuesta a la pregunta (1) que S116 dejó formulada: **el
  presupuesto no es la variable que gobierna la calidad, sino la PROBABILIDAD de obtener horario.** El defecto
  de producción de 30 s (`GeneradorHorarioService.java:201`) es INDEFENDIBLE: 0 de 4, y no por mala suerte
  sino porque nunca tuvo ocasión. Por debajo de 120 s el sistema es una moneda al aire. Lo defendible con
  estos datos es **600 s**, único punto sin ningún fallo observado, con la salvedad honesta de que son 2
  corridas, una sola máquina de 8 núcleos y una sola semilla. Y ahí la pregunta deja de ser un número: 600 s
  son diez minutos de pantalla inmóvil delante del jefe de estudios (D-generacion-sin-indicador), y una
  corrida que caiga en la cola mala dice «no hay horario factible», que es FALSO (D-timeout-como-infactible).
  LO QUE LA MEDICIÓN DECIDIÓ SOBRE LA DEUDA, y es la decisión estratégica de la sesión. **Ninguna de las dos
  se paga y ninguna bloquea el criterio**, pero el fundamento cambia. D-motivo-rechazo-sin-registro se
  descartó como instrumento POR MEDICIÓN y no por regla: el mensaje de `HorarioInfactibleException` lleva el
  estado de CP-SAT DENTRO del texto (`SolverHorario.java:124-126`) y el arnés lo recibe como excepción y no
  como cuerpo HTTP, así que la deuda muerde al usuario final, no a esta medición. De regalo, eso convirtió en
  MEDICIÓN lo que era inferencia: **27 rechazos observados, 27 UNKNOWN, CERO INFEASIBLE**, es decir el
  catálogo no se demostró imposible ni una sola vez y el sistema dijo veintisiete veces que no había horario
  factible. D-timeout-como-infactible pasa de RAZONADA a MEDIDA sin pagar nada.
  LO QUE SE PIERDE Y QUEDA DECLARADO: en el instante del rechazo (`SolverHorario.java:124`) el objeto
  `CpSolver` está vivo y completo —`objectiveValue`, `bestObjectiveBound`, `wallTime`, `responseStats`— y el
  `throw` conserva solo el nombre del estado interpolado en el texto. En un fallo no hay solución, luego el
  objetivo no significa nada, pero la COTA diría cuánto había avanzado CP-SAT. No se recupera sin tocar el
  solver, y tocarlo es el arreglo de la deuda.
  **C-GENERACIÓN NO CIERRA, y es decisión con argumento y no falta de tiempo.** Su enunciado incluye
  «tiempo», y declararlo terminado dejando el defecto en 30 s sería declarar terminado justo lo que la
  sesión ha demostrado que está mal. Le queda una pieza de PRODUCTO, pequeña y localizada, con las tres
  ediciones en el mismo camino de fallo: calibrar el defecto de presupuesto, distinguir UNKNOWN de INFEASIBLE
  en `SolverHorario`, y dar señal durante la espera. Matiz de R-deuda que conviene no perder: si el Cambio
  toca `GeneradorHorarioService:201`, distinguir UNKNOWN de INFEASIBLE cae en el mismo camino y la deuda se
  cubre DE PASO; eso no es abrir una sesión para una deuda, es encontrársela. El contraste con el PDF sigue
  fuera y es la tercera pregunta, intacta.
  DEUDA — nacen TRES y ninguna abre sesión. **D-generacion-no-reproducible** (técnica real, la más relevante
  del Cambio): sin fijar el paralelismo, dos generaciones idénticas dan resultados distintos y a veces
  ninguna. **D-prevalidacion-ciega-a-holgura-cero** (técnica real menor): con 26 grupos a 30/30 la
  prevalidación da ERROR=0 y AVISO=0, porque `GRUPO_SOBRECARGADO` exige demanda MAYOR que los tramos; el
  usuario recibe vía libre justo ante el caso más difícil que el sistema puede plantearse, y no hay ninguna
  otra señal que corrija esa lectura. **D-guion-exit-enmascarado** (de método): un guion cuyo `EXIT=$?` mide
  el `echo` y no el `mvn` anunció como éxito un BUILD FAILURE; detectado y corregido por Claude Code dentro
  de la sesión, pero la plantilla es la de sesiones anteriores. Se AFINAN dos vivas: D-timeout-como-infactible
  (de razonada a medida) y D-generacion-sin-indicador (su gravedad sube: el presupuesto viable está en
  cientos de segundos, no en decenas). Y se matiza D23 sin reabrirla: el arranque en caliente
  (`resolverOptimizandoConSemilla`, :156) toma como parámetro una `SolucionHorario`, no un entero, y su
  javadoc espera obtenerla de `resolver`, que cuesta ~860 s; el warm start no es palanca aplicable tal cual
  en este catálogo salvo que la semilla se cachee, y eso es objetivo propio, no palanca.
  NO SE MIDIÓ, y se dice: el arranque en caliente sembrado con la solución de 860 s, que es lo único que
  podría convertir el esquema en dos fases (una lenta cacheada, otra rápida) en algo operativo. Queda fuera
  por R-terminado: el criterio de O-demo pide horario VÁLIDO, no bueno.
  HIPÓTESIS PLAUSIBLE PERO NO ESTABLECIDA, anotada para que no se pierda ni se ascienda a hecho: si cada
  corrida es una tirada independiente, tres intentos de 120 s podrían batir a uno de 360 s. Comparar 3/4
  contra n=1 no es comparar; queda como opción de diseño con la forma de medirla escrita.
  LÍMITES DE TODO LO MEDIDO, declarados de una vez: una sola máquina de 8 núcleos, una sola semilla
  aleatoria, y n pequeño en cada punto (n=3 o menos salvo en la pasada 4). Cualquier presupuesto por defecto
  que acabe en producción llevará margen explícito sobre el umbral medido y la máquina nombrada; un «bastan
  N segundos» sin esa salvedad es una promesa que el portátil del centro puede desmentir.
  C-CARGA-MANUAL-1ESO: recomendación registrada, decisión NO tomada. El arquitecto senior recomendó hacerlo
  RECORTADO —el bloque de seis plazas del §6.1 y la co-docencia de LCL sobre una base de usar y tirar, en vez
  de 1º ESO entero— porque ahí está todo el valor probatorio y los ~80-100 envíos completos añaden volumen y
  no evidencia; y hacerlo DESPUÉS de C-generación, que no lo bloquea. El arquitecto confirmó el alcance de la
  sesión sin pronunciarse sobre esto, así que el Cambio sigue PROPUESTO y la nota de alcance del criterio 5
  sigue escrita y declarada.
  LIMPIEZA (M1-bis): archivada S115 a `bitacora-sesiones.md` (promovida a `### Sesión 115`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S116 a «Última sesión registrada (previa)» compacta y
  CORREGIDA en su frase de R4 (el «punto de abajo» inexistente); S117 queda como única cabecera H3 viva.
  Actualizados los dos censos de la bitácora (→ S10–S115), la crónica de archivado y la frase de ventana del
  plan. R4/costura: el script oficial SIGUE sin existir en el repo (mejora de método pendiente desde S101, y
  D-guion-exit-enmascarado le añade un caso); verificado a mano que el árbol quedó limpio, que no hay restos
  del arnés (`.java` borrado y también el `.class` de `app/target/test-classes/`, que aun estando bajo
  `target/` permitiría que un `-Dtest=` futuro lo ejecutara) y que ninguna de las cinco bases está trackeada.
  Esta sesión NO produce commits de código: solo documentación.
  O-demo (H2) ACTIVO, 3 piezas (C-derivación S115, C-cargador S116, C-generación S117 EN CURSO). Suites
  INTACTAS, ningún módulo tocado: app 268, solver 91, vitest 310, e2e 2. Siguiente: cerrar C-generación con
  su pieza de producto —presupuesto por defecto, UNKNOWN vs INFEASIBLE y señal de espera—, o el contraste con
  el PDF, que es la tercera pregunta y no ha empezado. Lo fija su propio M0 (ver M1-ter).

### Sesión 118 — O-demo (H2): C-generación, segunda parte. La PIEZA DE PRODUCTO: presupuesto configurable con defecto 600 s, separación de PRESUPUESTO_AGOTADO (503) / CATALOGO_INFACTIBLE (422) / CONFIGURACION_INCOMPLETA (422), y estado de espera en la vista (Desarrollo completo: M0 + M2 + M3 + M4 + M1). TERCERA pieza, SIGUE EN CURSO. NO cierra el Cambio ni el objetivo.
  Decimoctava sesión bajo el mapa Hito→Objetivo→Cambio, y **primera de tipo DESARROLLO desde S114**: M0 + M2 +
  M3 (tests primero, rojo verificado) + M4 (verificación en ejecución sobre HTTP real) + M1. Lo que ENTREGA, y
  es más de lo que el alcance prometía: la pieza de producto que C-generación necesitaba para cerrar su
  enunciado en la parte de «tiempo», y **el PRIMER HORARIO DEL CENTRO REAL generado por la vía de producción
  desde la interfaz** —clic en el navegador, POST de 600 s, 770 sesiones escritas—, que hasta hoy solo se había
  logrado por arnés en proceso (S117) o por `curl` (S116). Suites: app 268 → **282**, vitest 310 → **316**,
  solver 91 y e2e 2 intactas.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), 3 piezas. Hito = H2.
  Cambio = C-generación, EN CURSO desde S117. **EL M0 CORRIGIÓ EL PROMPT DE APERTURA en un punto que cambia lo
  que la sesión puede prometer:** el prompt afirmaba que la pieza de producto era «lo único que impide cerrar
  el Cambio», y la ficha dice otra cosa —el enunciado de C-generación es *factibilidad, tiempo y contraste con
  el horario del PDF*, y el cierre de S117 registra que faltan DOS cosas—. El contraste no es un extra
  opcional: está en el enunciado. Consecuencia declarada por adelantado y cumplida: esta sesión, saliendo
  perfecta, NO cierra C-generación.
  LA ELECCIÓN ENTRE LOS DOS FRENTES, y no fue preferencia sino DEPENDENCIA. Se decidió la pieza de producto
  antes que la pregunta (3) porque el contraste necesita un horario generado y persistido, y la base canónica
  estaba en `sesion 0`: empezar por el contraste obligaba a resucitar el arnés desechable que S117 borró o a
  lanzar corridas a la moneda al aire con el defecto de 30 s (0 de 4 medido), es decir a producir un horario
  que NO vino por el camino que el criterio 5 pide demostrar. Al revés no había coste: definir qué se asevera
  como «válido» no depende de nada de esta sesión.
  R-invalidación con una condición REAL y no formularia: O-diseño va detrás de O-demo y rehará la vista de
  horario, así que la edición del estado de espera entra como COMPORTAMIENTO con marcado mínimo y CERO
  esfuerzo de estilo —sin spinner, sin animación, sin CSS nuevo—. Lo que O-diseño rehará es el aspecto, no el
  estado. R-deuda: ninguna deuda abre la sesión; dos se cubren DE PASO por caer en el camino de fallo que el
  Cambio tenía que tocar (ver abajo).
  M2 — MEDICIÓN EN DOS GUIONES DE SOLO LECTURA, ambos por Claude Code, y lo primero que se midió fue lo que
  podía TUMBAR EL ALCANCE: si algún timeout corta un POST de 600 s antes de llegar al navegador, calibrar el
  defecto deja de ser cambiar una constante y pasa a ser rediseñar el endpoint a asíncrono con sondeo.
  RESULTADO: **nada lo corta.** Cero claves de timeout en todo `app/src` (`connection-timeout`,
  `async.request-timeout`, `keep-alive`, `read-timeout` sin una sola coincidencia), `proxy.conf.json` sin
  timeout, y el cliente Angular sin `timeout(...)`, sin `HttpInterceptor` y sin `withInterceptors`. El único
  techo de 30 s del proyecto resultó ser el timeout de test por defecto de Playwright, que no tiene nada que
  ver con el presupuesto del solver.
  M2 (2) — EL PRESUPUESTO YA ERA PARAMETRIZABLE DE EXTREMO A EXTREMO SALVO POR UN `{}`. `maxSegundos` viaja en
  `GenerarHorarioRequest` (record de 4 campos) y `<= 0` aborta con 400; el defecto de 30 vive solo en
  `GeneradorHorarioService:201` y el constructor sin argumentos del solver (120 s) nunca se usa desde la app.
  Lo que faltaba no era contrato: es que `horario.service.ts:27` manda `{}` fijo, así que por la UI el
  presupuesto era siempre 30 s y no había palanca. **El defecto NO estaba bajo ningún test.**
  M2 (3) — TRES HECHOS DISTINTOS SALÍAN POR EL MISMO 422. `HorarioInfactibleException` es un único tipo para
  INFEASIBLE, UNKNOWN y MODEL_INVALID, y `HorarioController:63-65` lo traduce al mismo status y a la misma
  forma de cuerpo que `PrevalidacionFallidaException`. No hay `@ControllerAdvice` en ningún sitio (los 12 hits
  del grep son javadoc explicando su ausencia).
  M2 (4) — NO EXISTÍA NINGÚN ESPACIO DE NOMBRES PROPIO DE PROPIEDADES: cero `@Value` y cero
  `@ConfigurationProperties` en `app` y en `solver`, y las cuatro claves vivas de `application.properties` son
  todas `spring.*` / `server.*`. Hacer el presupuesto configurable INAUGURA el primer espacio de nombres del
  proyecto, así que es decisión de PRECEDENTE y se elevó al arquitecto antes de escribir código.
  M2 (5) — EL OVERRIDE EXTERNO NO HABÍA QUE INVENTARLO: `playwright.config.ts` ya arranca el backend con
  `--spring.datasource.url=...` en `spring-boot.run.arguments`, luego añadir `--educhronos.solver.max-segundos=N`
  es el MISMO mecanismo ya en producción. Boot 4.1.0 pone los argumentos de programa por encima de
  `application.properties` en el orden de precedencia.
  DECISIÓN DEL ARQUITECTO — PROPERTY CON NOMBRE PROPIO, y no constante compilada. Tres razones y la tercera
  manda: el mecanismo ya está en uso; el valor es DEPENDIENTE DE LA MÁQUINA por medición explícita; y H4
  entrega un bundle a un centro cuyo portátil no conocemos, donde ajustar una constante compilada significa
  recompilar. Forma: `@Value("${educhronos.solver.max-segundos:600}")`, defecto en la anotación para que la
  aplicación arranque aunque falte la clave, y NO `@ConfigurationProperties` por ser una sola clave. **REGLA DE
  PRECEDENTE escrita junto a la clave para que no dependa del criterio de nadie: a la SEGUNDA clave
  `educhronos.*`, se migra a un record `@ConfigurationProperties`.** Y la regla de uso, hermana de la de S117
  sobre `spring.datasource.url`: probar otro presupuesto NO se hace editando el fichero trackeado, se hace en
  el arranque.
  EL VALOR: 600 s, y NO se sube. Subirlo a 900 o 1200 se paga en CADA corrida —sobre el centro real el
  presupuesto se consume ENTERO siempre— a cambio de una ganancia que nadie ha medido; y el hallazgo del M4
  (ver abajo) va en la dirección contraria a subirlo: si ningún presupuesto razonable garantiza el éxito, el
  arreglo no es un número mayor, es el 503 reintentable que esta sesión construye.
  M3 — TESTS PRIMERO, CON UNA SALVEDAD DE MÉTODO QUE CONVIENE NO PERDER. En Java «test primero» no es puro: un
  test que referencia un campo inexistente NO FALLA, no compila, y un fallo de compilación tumba la suite
  entera sin decir nada sobre la lógica. Por eso el M3 introdujo las FIRMAS mínimas (campo en la excepción,
  método extraído, función de mapeo) con el mapeo DELIBERADAMENTE sin implementar —devolviendo siempre
  422/«INFACTIBLE», la conducta de hoy—, de modo que los tests compilaran y fallaran POR ASERCIÓN. Rojo
  verificado: **11 aserciones cayendo donde debían** (5 de `MapeoFalloSolverTest`, 4 de `MapeoFalloEndpointTest`,
  2 de frontend), ninguna por compilación.
  PARADA DEL M3, PROVOCADA POR EL GUION Y CORRECTA. El guion decía «actualiza la única llamada en
  `SolverHorario`; si hay más de una, PARA». Había CUATRO: tres en `SolverHorario` (:71, :124, :175) con
  prefijos de mensaje DISTINTOS, y una cuarta en `ModeloCpSat:887` («El problema no tiene tramos») que se lanza
  CONSTRUYENDO EL MODELO, antes de que exista ningún `CpSolver`. Un constructor único que fabricara el mensaje
  era imposible sin cambiar textos que los tests de endpoint asertan por `status().reason()`. Solución
  adoptada: DOS constructores, el viejo intacto (estado y segundos a `null`).
  LA DECISIÓN SOBRE EL CUARTO CASO, que era una regresión silenciosa esperando a ocurrir: con el estado a
  `null` cayendo en la rama por defecto, «el problema no tiene tramos» habría pasado de 422 a **500** justo en
  el caso más probable de un centro recién instalado. Se rechazaron las dos opciones ofrecidas —ni `null` a 500
  ni un `"SIN_TRAMOS"` inventado—: `mapear` comprueba el `null` ANTES del switch y devuelve 422 con causa
  **CONFIGURACION_INCOMPLETA**. Razón: la rama por defecto existe para un `CpSolverStatus` que no conocemos, y
  un estado nulo no es un estado desconocido, es que NO HUBO SOLVE; si los dos cayeran por la misma rama, el
  500 dejaría de significar «bug nuestro». Y meter `"SIN_TRAMOS"` en un campo que documenta ser el estado de
  CP-SAT es escribir un dato falso en el sitio donde se lee la verdad.
  EL MAPEO RESULTANTE, cuatro ramas y la cuarta no es relleno: `INFEASIBLE` → 422 CATALOGO_INFACTIBLE;
  `UNKNOWN` → **503 + `Retry-After: 0`** PRESUPUESTO_AGOTADO; `null` (no hubo solve) → 422
  CONFIGURACION_INCOMPLETA; `MODEL_INVALID` y cualquier otro → 500 ERROR_INTERNO.
  **POR QUÉ 503 Y NO 408, corrección del arquitecto a su propia propuesta.** Se propuso 408 y se verificó antes
  de fijarlo: RFC 9110 dice que 408 es que el servidor no recibió el mensaje de petición completo a tiempo, es
  decir **el cliente fue lento**, y arrastra la convención de que la conexión puede cerrarse. Nuestro caso es
  el contrario: la petición llegó entera y rápida y el servidor se quedó sin tiempo CALCULANDO. 503 dice
  literalmente eso, y `Retry-After` ES la señal estándar de reintentable, en cabecera en vez de en una
  convención propia. El valor **0** es deliberado y lleva comentario en el código: significa «reintenta ya», es
  exacto porque cada corrida es una tirada independiente, y sin el comentario alguien lo leería como un
  descuido y lo «arreglaría».
  DECISIÓN DE DISEÑO QUE HACE LA SOLUCIÓN INMUNE A D-F8.6-ii-a: el cuerpo del fallo lo construimos NOSOTROS
  (`ResponseEntity` con `FalloGeneracionDTO`: `causa`, `mensaje`, `estado`, `segundos`) en vez de delegar en el
  mecanismo de error de Spring, que está MEDIDO como mudo. Así el motivo llega al cliente sin abrir la deuda
  global. El método del controlador pasó de `HorarioProyeccionDTO` a `ResponseEntity<Object>`; el 200 sigue
  devolviendo la misma proyección y `PrevalidacionFallidaException` queda INTACTA —mismo status, mismo cuerpo,
  mismo orden de catch—, que era la única regresión posible de este cambio.
  TRES DECISIONES DE CLAUDE CODE SOBRE LA MARCHA, las tres correctas y registradas porque enseñan algo.
  (1) Los dos casos de 400 (`maxSegundos` 0 y -1) NO fueron a `MapeoFalloEndpointTest`, donde el servicio es un
  `@Mock` que no tiene la guarda —el test habría pasado con la guarda BORRADA, justo lo contrario de cubrirla—,
  sino a `GenerarHorarioEndpointTest`, que monta el servicio real. Cero y negativo van separados a propósito:
  con la guarda relajada a `< 0` el cero se cuela y solo el primero lo caza. (2) El test `(32)` del frontend
  fijaba el texto literal del 422 para el mismo escenario que el `(42)` nuevo; se le quitó el aserto de texto y
  se cedió al test donde el texto se vuelve DISCRIMINANTE frente al 503, en vez de duplicarlo. De rebote
  desapareció un mensaje que hablaba de «el pin» en la vía de generación. (3)
  `post_conCatalogoInfactible_devuelve422DelSolver` leía `getResolvedException()` para distinguir el 422 del
  solver del de prevalidación; al dejar de lanzarse la excepción se sustituyó por `$.causa` + `$.estado`, que
  es MÁS fuerte porque discrimina por el cuerpo que ve el cliente —exactamente lo contrario del hallazgo de
  método de S109—.
  M4 — VERIFICACIÓN EN EJECUCIÓN, TODO SOBRE COPIA, y con una trampa atajada antes de arrancar. **El jar del
  solver en `~/.m2` era del 31 de julio y su excepción solo tenía el constructor de un argumento**, así que
  `mvn -pl app spring-boot:run` —que resuelve el solver desde el repositorio local y NO del árbol— habría
  arrancado con el código de julio y reventado con `NoSuchMethodError` al primer fallo, dando un 500 espurio en
  vez del mapeo. Afecta también al e2e, que arranca con el mismo `-pl app`. Se publicó el actual
  (`mvn -pl solver install -DskipTests`) y se verificó en el bytecode.
  LOS TRES DESENLACES, medidos sobre HTTP real contra la copia del centro real:
  **503** con presupuesto de 30 s → `Retry-After: 0`, `{"causa":"PRESUPUESTO_AGOTADO","estado":"UNKNOWN","segundos":30}`,
  31,15 s. Es exactamente lo que antes salía como un 422 «no tiene solución», que era falso. Que `segundos`
  diga 30 prueba de paso que el override por arranque llegó hasta el solver: **la decisión de hacerlo
  configurable queda verificada en ejecución, no por lectura**.
  **422** sobre catálogo vacío → `{"causa":"CONFIGURACION_INCOMPLETA","estado":null,"segundos":null}`. Dato que
  confirma por qué esta rama importaba: **la prevalidación devuelve `[]` sobre el catálogo vacío y NO lo
  detecta**; la petición la atraviesa entera y solo la caza `ModeloCpSat`. Sin la decisión del `null`, una
  instalación recién estrenada habría recibido un 500.
  **400** con `maxSegundos` 0 y -1. La guarda funcionaba; solo estaba sin vigilar.
  **200 — EL RESULTADO DE LA SESIÓN, y lo obtuvo el arquitecto DESDE EL NAVEGADOR.** Con el defecto de 600 s,
  clic en «Generar horario» en `localhost:4200`: el botón se cerró, apareció «Generando horario… puede tardar
  hasta 10 minutos», la pantalla no se cayó, y a los ~10 minutos se pintó el horario. **`horarios 1`,
  `sesion 770`, `plazas usadas 305`.** Las 770 filas son EXACTAMENTE el oráculo aritmético que S117 dejó
  escrito para 208 actividades cargadas, y las 305 plazas están todas colocadas; cero FK nulas; reparto por
  día 159/151/151/155/154. Con esto quedan verificados de una sola vez el POST largo llegando vivo al
  navegador —el único riesgo que podía tumbar el alcance—, el estado de espera funcionando con su texto, y el
  camino feliz existiendo POR LA UI.
  SALVEDAD SOBRE LA CALIDAD, para no darle a ese 200 un valor que no tiene: `estado_solver` es **FEASIBLE**,
  objetivo **192.0** y **cota inferior 0.0**, es decir agotó los 600 s con una solución en mano y el gap
  completamente abierto. Es un horario VÁLIDO y COMPLETO —las 770 sesiones cumplen las restricciones duras—,
  pero no es óptimo ni se sabe cuán lejos está del óptimo. El criterio de O-demo pide horario válido, no bueno
  (R-terminado).
  **LA MEJOR EVIDENCIA QUE EXISTE DE D-GENERACION-NO-REPRODUCIBLE, y llegó regalada.** La corrida por `curl` de
  Claude Code (17:38) y la del arquitecto por la UI (17:47) fueron CONSECUTIVAS, con el mismo presupuesto de
  600 s y la misma base: la primera dio **UNKNOWN** y la segunda **FEASIBLE**. Con la JVM al 687 % de CPU en la
  fallida, no fue una corrida estrangulada. Lo que hay entre las dos es el azar del presolve, no el tiempo.
  **Consecuencia sobre la afirmación de S117:** «600 s es el único punto sin fallos observados» deja de ser
  cierta. La tasa acumulada a 600 s pasa a **3/4** (S116 ✔, S117 ✔, S118 ✘ por curl, S118 ✔ por UI). 600 no es
  un umbral seguro: es un punto con probabilidad de fallo baja y NO nula. El comentario de
  `application.properties` se corrigió en esta misma sesión para no citar la medición como si lo fuera.
  **Y VALIDA EL DISEÑO DE LA SESIÓN, que es lo que convierte el hallazgo en buena noticia:** si el mismo botón,
  con la misma configuración, unas veces devuelve horario y otras se queda sin tiempo, entonces devolver 422
  «no tiene solución» era estructuralmente erróneo y no un caso raro, y el 503 con reintento más el estado de
  espera con minutos son la respuesta correcta a un solver que a veces no termina.
  E2E CRONOMETRADO, que era la última pregunta abierta: **2/2 en 14,6 s** (`humo` 391 ms, `centro-minimo` 2,7 s).
  El centro mínimo resuelve al instante y sale por el 200 de siempre, luego el 503 no interfiere con el caso
  feliz y la suite no hereda el presupuesto largo. Valida además indirectamente el arreglo de `~/.m2`.
  INTEGRIDAD DE LA BASE CANÓNICA, verificada al cerrar: `app/educhronos-demo.db` en `f5b542eb…` con `sesion 0`,
  intacta desde el principio. Toda la generación ocurrió sobre `educhronos-demo-m4.db`. Importa porque
  `ActividadService.exigirSinDependientes` bloquea con 409 las actividades que ya tengan sesiones: si se
  hubiera persistido en la canónica, **O-particiones habría perdido su punto de partida**. Es la misma
  condición de orden que S117 respetó.
  **CORRECCIÓN MAYOR SOBRE D-F8.6-ii-a, Y EL ARQUITECTO SE EQUIVOCÓ AQUÍ: la causa real estaba sin medir.** El
  arquitecto insistió tres veces en que «reactivar la clave» estaba DESCARTADA por medición, apoyándose en
  S113 y S116. El hecho era correcto —la clave del fichero no surte efecto— pero la conclusión que arrastraba
  no: **en Boot 4 la clave se llama `spring.web.error.include-message`**, y la que está en
  `application.properties` es la sintaxis de Boot 3, muerta desde la migración a 4.1. Probado en las dos
  direcciones por Claude Code: con la clave del fichero el 400 llega sin `message`; arrancando con la nueva,
  llega con él. La opción que se daba por muerta está VIVA bajo otro nombre y su arreglo es de UNA LÍNEA. Dos
  consecuencias que no había visto nadie: el comentario de `application.properties` describe un efecto que no
  ocurre, y `mensaje()` del frontend lee `err.error.message`, que nunca llega, así que **todos los rechazos de
  pines llevan degradados a «El servidor rechazó el pin (N).» desde la migración**. NO se paga en S118: cambia
  el cuerpo de error de TODA la superficie REST, no bloquea el criterio de O-demo, y esta sesión se hizo
  deliberadamente inmune a ella construyendo el body nosotros (R-deuda). Lección de método: el arquitecto
  defendió con firmeza un hecho bien medido y lo extendió a una conclusión que el hecho no sostenía; la
  medición ORIGINAL seguía siendo válida y lo que faltaba era preguntarse POR QUÉ.
  DEUDA — DOS SE PAGAN DE PASO, UNA NACE, TRES SE AFINAN. **PAGADAS**, y no por decisión de pagarlas sino por
  caer en el camino de fallo que el Cambio tenía que tocar: **D-timeout-como-infactible** (el UNKNOWN ya no se
  llama infactible: sale por 503 con su causa) y **D-generacion-sin-indicador** (hay estado de espera, botón
  cerrado y minutos anunciados, verificado en navegador durante diez minutos reales). Encontrarse una deuda
  haciendo el trabajo del Cambio no es abrir una sesión para ella. **NACE D-presupuesto-anunciado-espejo**
  (técnica real menor): los «10 minutos» del texto de espera son una constante `MINUTOS_ANUNCIADOS` en el
  componente, espejo MANUAL de `educhronos.solver.max-segundos`; el backend no publica el presupuesto por
  ningún endpoint, así que si allí se cambia el valor, aquí se desincroniza en silencio. Es el precio honesto
  de haber hecho el presupuesto configurable, y está documentado en el código como cota anunciada y no como
  promesa. **AFINADAS**: D-generacion-no-reproducible (con la evidencia más fuerte hasta la fecha, dos corridas
  consecutivas opuestas), D-prevalidacion-ciega-a-holgura-cero (superficie ampliada: además de la holgura cero,
  no detecta el catálogo VACÍO; y su efecto quedó FOTOGRAFIADO en la pantalla del arquitecto, con «Catálogo
  sano: sin hallazgos de pre-validación» sobre un centro donde 26 de 28 grupos van a 30/30) y D-F8.6-ii-a (la
  causa real, medida).
  NOTA TÉCNICA DE MÉTODO, que corrige una invocación que estaba en uso: **`mvn -pl app test` NO sirve en este
  árbol.** Compila `app` contra el solver INSTALADO en `~/.m2`, no contra el del working tree, así que un
  cambio reciente en `solver/src/main` da errores de constructor falsos. Hay que correr desde la RAÍZ para que
  los dos módulos entren en el reactor —que es lo que la regla de invocación de suites ya decía—, y si se usa
  `spring-boot:run` con `-pl app` (como hacen el arranque manual y `playwright.config.ts`), hay que instalar el
  solver antes.
  C-GENERACIÓN SIGUE EN CURSO, y es lo único que la sesión NO cierra: su pieza de producto está HECHA y
  verificada en ejecución, pero la pregunta (3) —el contraste con el horario del PDF, que NO exige igualdad
  sino validez— no ha empezado. O-demo tampoco cierra: siguen faltando las 11 actividades de FPB a la espera
  de la respuesta del centro (D31-a). C-carga-manual-1eso sigue PROPUESTO y sin decidir, con la recomendación
  de S117 registrada.
  LIMPIEZA (M1-bis): archivada S116 a `bitacora-sesiones.md` (promovida a `### Sesión 116`, insertada al final
  en orden ascendente, cuerpo íntegro); degradada S117 a «Última sesión registrada (previa)» compacta; S118
  queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S116), la crónica de
  archivado y la frase de ventana del plan. R4/costura: el script oficial SIGUE sin existir en el repo (mejora
  de método pendiente desde S101); verificado a mano que el árbol queda limpio tras los cuatro commits de
  código, que ninguna de las bases está trackeada y que la canónica conserva su md5.
  O-demo (H2) ACTIVO, 3 piezas (C-derivación S115, C-cargador S116, C-generación S117+S118 EN CURSO). Suites:
  **app 282, solver 91, vitest 316, e2e 2**. Siguiente: el contraste con el horario del PDF, única pregunta
  viva de C-generación, que exige decidir antes qué se asevera como «válido». Lo fija su propio M0 (ver M1-ter).

### Sesión 119 — O-demo (H2): C-generación, tercera parte y CIERRE del Cambio. El contraste con el horario del PDF: definición de «válido» en tres capas y medición de las tres sobre el horario de S118 (M0 + M2 en cuatro pasadas + M1, sin M3 ni M4 canónicos). TERCERA pieza CERRADA. NO cierra el objetivo.
  Decimonovena sesión bajo el mapa Hito→Objetivo→Cambio, y del mismo tipo SIN ENCAJE EXACTO que S117: ritual
  M0 + M2 completo + M1, sin M3 ni M4 canónicos, porque la sesión entera es MEDICIÓN sobre un artefacto ya
  producido y no se escribe una línea de producto. Ningún fichero de `app/src/main`, `solver/src/main` ni
  `app/frontend` se toca; las cuatro suites quedan intactas (app 282, solver 91, vitest 316, e2e 2). Lo que
  ENTREGA: la tercera y última pregunta de C-generación queda RESUELTA y **el Cambio CIERRA**, con la
  aserción de validez del horario del centro real medida por tres vías independientes.
  M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), 3 piezas. Hito = H2.
  Cambio = C-generación, EN CURSO desde S117, con una sola pregunta viva: la (3), el contraste con el PDF.
  **EL TRABAJO REAL DEL M0 FUE ENCUADRAR QUÉ SE ASEVERA COMO «VÁLIDO», que era lo que S117 dejó escrito como
  condición previa y nadie había tomado.** La decisión, y es la que gobierna toda la sesión: **el PDF NO puede
  juzgar la validez del horario generado.** Los dos horarios son distintos POR DISEÑO —los volcados no
  contienen ninguna disponibilidad de profesor (medido en S115) y el centro sí las tuvo al construir el
  suyo—, así que comparar colocaciones solo puede producir diferencias, y todas esperadas. Lo que el PDF SÍ
  es, como registró S115, es un ORÁCULO DE CONTENIDO. De ahí que «válido» se parta en TRES CAPAS con oráculos
  distintos:
  **CAPA 1 — validez formal contra el modelo:** cero violaciones de las ocho `ReglaDura`. Oráculo:
  `VerificadorSolucion`, que ya existe (reutilización, no validador nuevo). Reserva declarada por adelantado:
  verificar la solución EN MEMORIA sería parcialmente circular, porque CP-SAT impuso esas mismas
  restricciones; el valor está en verificar RELEYENDO las 770 filas persistidas, porque `SolucionMapper` no
  está bajo el modelo.
  **CAPA 2 — conservación de la carga contra el PDF:** para cada grupo, los pares (asignatura, profesor) y su
  número de tramos semanales coinciden con lo que imprime `grupo-*.json`, descontadas las 11 actividades de
  FPB no cargadas. Es lo único que el PDF puede adjudicar, y prueba la cadena entera: PDF → volcados →
  catálogo derivado → carga por API → problema → solución → `sesion`. Declarado también su límite: lo que
  esta capa ejercita de verdad es C-derivación, no el solver —dado un catálogo correcto, la conservación se
  sigue de que `aSesiones` lanza si alguna instancia no está colocada—, y no debe venderse como validación
  del solver.
  **CAPA 3 — calidad: SE MIDE Y NO SE CORRIGE.** El objetivo 192.0 con cota 0.0 no dice qué lleva dentro.
  Se descompone con instrumentos que ya existen. Justificación frente a R-terminado, que la regla resiste:
  el criterio de O-demo contiene la cláusula «presentable al centro», y medir no es mejorar; si sale mal, es
  información para el cierre de O-demo, no trabajo de esta sesión.
  DIVERGENCIAS ESPERADAS, DECLARADAS ANTES DE MEDIR para que un desajuste fuera hallazgo y una coincidencia no
  fuera racionalización: día/tramo distintos (sin disponibilidades); aulas no comparables (el PDF omite 65
  celdas y el solver elige libremente); tutorías heredando la heurística implausible de D31-b; y el déficit
  exacto de las 11 plazas de FPB. Y una coincidencia numérica marcada para NO leerla como cuadre: el PDF de
  aulas tiene 770 entradas y `sesion` tiene 770 filas; son magnitudes distintas.
  R-invalidación, con tres condiciones reales y no formularias: (a) O-particiones necesita la canónica con
  `sesion 0`, así que todo corre sobre COPIA y al cerrar se verifica md5 + `sesion 0`; (b) H3 construirá la
  exportación del horario, así que todo volcado de esta sesión es ARNÉS DESECHABLE y no producto; (c)
  O-diseño rehará la vista, así que no se toca frontend.
  R-deuda: ninguna deuda abre la sesión, y el argumento no es de comodidad en ninguna de las cuatro que el
  prompt de apertura puso sobre la mesa. **D-generacion-no-reproducible** afecta a la PROBABILIDAD de volver a
  obtener un horario, no a la validez del que ya está obtenido: muerde en el coste de la sesión, no en la
  aserción. **D-prevalidacion-ciega-a-holgura-cero** es una advertencia ANTES de generar y no interviene en
  juzgar lo generado. **D-presupuesto-anunciado-espejo** es texto de pantalla, ajeno al contraste.
  **D-F8.6-ii-a** es de una línea y aun así fuera de alcance: esta sesión no toca ningún camino de fallo
  (mismo criterio que S118, barato no es en alcance). La única bloqueante sigue siendo **D31-a**, que bloquea
  O-demo y no C-generación; declarado por adelantado, como en S117 y S118: **esta sesión no podía cerrar
  O-demo pase lo que pase.**
  M2 — CUATRO PASADAS, todas por Claude Code, todas de solo lectura o sobre copia, ninguna con JVM salvo la
  cuarta.
  M2 (1) — INVENTARIO. **El horario de S118 SOBREVIVE**: `app/educhronos-demo-m4.db` (24 ago 17:47) con
  `horario_generado 1` FEASIBLE, objetivo 192.0, cota 0.0, `sesion` **770**, 305 `plaza_id` distintas —las 305
  de la base, todas colocadas— y cero FK nulas. Con eso la sesión no necesita solver y no paga los diez
  minutos ni la tirada de 3/4. Medido además que el lector inverso YA EXISTE y no hay que construirlo:
  `SolucionMapper.aSolucionHorario` (:191, documentado en :157 como inverso de `aSesiones`, con la
  correspondencia de tramos tomada de `indiceTramos` y NO recalculada), y `DiagnosticoService.diagnosticar`
  ya encadena `cargarProblema()` → `cargarHorario(id)` → `aSolucionHorario` → `verificar` + `atribuirBlandas`
  + los tres contadores, anotado `@Transactional(readOnly = true)` y expuesto en
  `GET /api/horarios/{id}/diagnostico` (`HorarioController:115`). **Consecuencia: la capa 1 y la capa 3 se
  hacen POR LA VÍA DE PRODUCCIÓN y el arnés desechable que se había previsto se RETIRA por reutilización.**
  DOS CORRECCIONES DEL ARQUITECTO SENIOR AL INFORME DE ESA PASADA, y conviene que consten porque las dos
  habrían torcido la capa 2. (1) El hueco 219/208 se registró como «11 actividades por explicar»: NO es
  incógnita. Son las 11 de FPB cuya plaza no tiene aula, el dato falta EN LA FUENTE (el PDF de aulas no cubre
  los talleres: `Taller 2`, `Taller 4`… salen con `celdas: []`), `ActividadService.validarXor` las rechaza por
  diseño y el cargador de S116 las OMITIÓ A PROPÓSITO —prevalidación en seco con exactamente 11 violaciones,
  todas XOR de FPB, y coincidencia EXACTA entre la marca `_aulaDesconocida` y la regla XOR—. Es D31-a.
  (2) «770 sesiones, una por instancia» contradice lo que S117 midió: una fila es una PLAZA de una INSTANCIA,
  `filas = Σ (repeticiones × plazas)`, y que haya 305 `plaza_id` distintas en 770 filas ya lo demuestra.
  Importa porque es la trampa de unidades sobre la que se construye la capa 2.
  M2 (2) — CAPA 2, SIN JVM. Unidad de comparación fijada de antemano para disolver los dos desajustes de
  unidades: **entrada = (grupo, día, tramo, asignatura, profesor)** —lo que imprime una línea del PDF y lo que
  se obtiene expandiendo una fila de `sesion` por `plaza_subgrupo → subgrupo_grupo` y por `plaza_profesor`,
  que es como el PDF imprime la co-docencia de LCL—, y **el agregado comparado es (grupo, asignatura,
  profesor) → nº de entradas semanales**, sin día ni tramo. El lado izquierdo sale de la BASE m4, no del
  catálogo derivado: comparar catálogo contra PDF solo re-examinaría C-derivación.
  SUPUESTOS COMPROBADOS EN VEZ DE ASUMIDOS: `duracion_tramos` es 1 en TODAS las actividades de la base (lo que
  S117 midió sobre el catálogo queda medido ahora sobre lo cargado), luego celda y sesión son la misma unidad
  y la expansión por duración es vacía AQUÍ; cero plazas sin asignatura, sin profesor o sin subgrupo, luego
  ningún JOIN perdió filas en silencio; 30 tramos lectivos y 5 no lectivos.
  **RESULTADO DE LA CAPA 2: 526 claves (grupo, asignatura, profesor), 11 divergentes, TODAS en FPB, CERO
  divergentes fuera de FPB, delta total 1301 − 1252 = 49.** Y las 11 son, una a una y con la repetición
  exacta, las 11 que el guion había marcado como `_aulaDesconocida` ANTES de mirar el lado generado:
  AMO/PAU2 6 · CA/TEC1 4 · IPE/FOL3 3 · MECSO/PAU2 5 · PS/PAU2 5 · Tut/PAU2 1 (1FPB) — CA/FIS3 4 · ELE/PAU1 7 ·
  MEC/PAU1 11 · PI/FOL3 2 · Tut/PAU1 1 (2FPB). Suma 49. **Predicción independiente y observación coinciden al
  entero.** Segunda medida por otra vía que apunta al mismo agujero: 26 grupos con 30/30 slots ocupados, 1FPB
  con 6 y 2FPB con 5, y 30−24=6 y 30−25=5 cuadran con el déficit por grupo.
  **LA CIRCULARIDAD DEL MAPA DE CÓDIGOS, DETECTADA Y ELIMINADA, y es el control metodológico que más vale de
  esta sesión.** Los dos universos no emparejan literalmente (`1º ESO A` ↔ `1ºA`, `1ºBACH A` ↔ `1B-A`,
  `3º ESO A PDC` ↔ `3ºADi`) y Claude Code derivó el mapa maximizando el solapamiento de (asignatura,
  profesor) —que es EXACTAMENTE el dato que luego se comparaba—. Circularidad de grado bajo pero real, y peor
  justo donde menos evidencia había: 1FPB y 2FPB emparejaban con 0.14 y 0.17 y son los dos grupos donde vive
  toda la divergencia. Se disolvió barato porque el mapa ya estaba escrito de forma determinista y ajena a la
  medición: la tabla de normalización de `INFORME-RECONCILIACION.md` más el caso `3º ESO PDC`→`3ºCDi` que
  S115 cerró aparte. **Aplicada esa regla sin que el código mirase ni una vez la lista de
  `grupo_administrativo`: los 28 códigos tienen regla aplicable, CERO diferencias frente al mapa por
  solapamiento, regla inyectiva e imagen coincidente con la base en los dos sentidos.** El déficit de 49 no
  descansa sobre un mapa ajustado a posteriori.
  M2 (3) — INVENTARIO DE LA CADENA DE LA CAPA 1 y verificación del mapa. Registró además un hueco documental:
  `docs/horario-referencia/INFORME-RECONCILIACION.md` sigue diciendo «`NºPDC` (sin letra; correspondencia
  incierta)» y excluyendo esas 31 celdas de los cruces, cuando S115 CERRÓ la correspondencia por tres vías
  independientes. Es estado vivo equivocado (R5) en la fuente que un lector consultaría primero. Se corrige en
  este M1 añadiendo la remisión, sin tocar el cuerpo del volcado —es el entregable de una extracción
  determinista y su cuerpo no se reescribe—.
  M2 (4) — CAPAS 1 y 3 POR LA VÍA DE PRODUCCIÓN, sobre copia de la m4 y con la aplicación arrancada de
  verdad. La copia se inyectó por línea de órdenes con RUTA ABSOLUTA y no editando `application.properties`
  (regla de S117), y el arranque se blindó publicando antes el solver en `~/.m2` (trampa medida en S118).
  Comprobado ANTES de diagnosticar que la proyección devolvía 770 sesiones y que el log nombraba la base
  correcta, para que un 404 o un verde sobre una base vacía no pudiera pasar por resultado.
  **CAPA 1: CERO VIOLACIONES DE REGLA DURA sobre las 770 sesiones**, `GET /api/horarios/1/diagnostico` en HTTP
  200. **El horario del centro real generado en S118 por la vía de producción desde la interfaz es VÁLIDO a
  escala real.** Lo que esto añade sobre `DiagnosticoRoundTripTest` es la ESCALA y no la propiedad: el test ya
  prueba el ida y vuelta, la fidelidad de `aulasElegidas`, la guarda de corrupción de `aulaFija` y el
  cross-check de ventanas, pero sobre 2 actividades, 5 tramos y un grupo; aquí la misma cadena atraviesa 208
  actividades, 305 plazas, 28 grupos y 30 tramos sin romperse ni delatar una sola dura.
  **CAPA 3: ventanas 174, consecutivas 18, indisponibilidad blanda 0.** Y el hallazgo que convierte la
  medición en un tercer cotejo independiente: los tres pesos valen 1 (`ModeloCpSat.java:69,80,104`) y el
  objetivo es su suma minimizada (`:301`), luego **174 + 0 + 18 = 192, exactamente el `objetivo` 192.0 que
  CP-SAT escribió en `horario_generado`**. `VerificadorSolucion` recompone el objetivo desde la solución
  RECONSTRUIDA, con código distinto del que construyó el modelo, y da la misma cifra sin residuo.
  **ADVERTENCIA SOBRE EL `indispBlanda = 0`, que hay que blindar por escrito porque invita a la lectura
  contraria:** `profesor_restriccion_horaria` y `sesion_bloqueada` están VACÍAS. Es el cero de «no había nada
  que incumplir», no el de «no se incumplió», y no informa de nada sobre las preferencias del profesorado. Es
  la misma limitación que O-demo declara desde S115; lo nuevo es que ahora existe una cifra citable.
  SEGUNDA ADVERTENCIA, sobre las 465 filas de `penalizaciones`: el DTO las documenta como CONTRAFACTUALES por
  celda («qué pasaría si»), no como descomposición del total. Su suma de deltas es −73, que no cuadra con 192
  ni debe cuadrar. Sirven para señalar dónde apretar (`Bloque-DT_EST_MIT-2BACH` +5, `LCL-3ºB` +3, `Tec-2ºB`
  +3…), no para reconstruir el objetivo.
  INTEGRIDAD: la copia conserva su md5 tras el paso de Hibernate, luego el `@Transactional(readOnly = true)`
  se sostiene en la práctica y no solo en la anotación; la canónica sigue en `f5b542eb…` con `sesion 0`;
  árbol limpio, proceso parado, puerto cerrado.
  **LO QUE SE ASEVERA, con las palabras exactas: el horario del centro real generado en S118 por la vía de
  producción desde la interfaz es VÁLIDO —cero violaciones de las ocho reglas duras sobre 770 sesiones— y
  CONSERVA LA CARGA del horario que el centro imparte —526 claves, 11 divergentes, todas FPB, delta 49
  idéntico al déficit calculado de antemano—. CON ESO, C-GENERACIÓN CIERRA.**
  LÍMITES DE LA ASERCIÓN, declarados de una vez: **n = 1** —un solo horario, y no se generó un segundo porque
  cuesta diez minutos, tiene un cuarto de probabilidad de fallo (D-generacion-no-reproducible) y añade poco:
  la capa 2 es aritmética e invariante entre corridas y la capa 1 verifica un conjunto fijo de restricciones
  sobre una colocación concreta—; **`FEASIBLE` con cota inferior 0.0**, sin óptimo demostrado, y el criterio
  pide válido y no bueno (R-terminado); la comparación es de MULTICONJUNTOS y no dice nada sobre la
  colocación, que difiere por diseño; y no cubre aula ni las blandas más allá de medirlas.
  **O-DEMO NO CIERRA.** Siguen faltando las 11 actividades de FPB a la espera de la respuesta del centro
  (D31-a) y la nota de alcance del criterio 5 sigue escrita y declarada.
  DECISIÓN DEL ARQUITECTO SOBRE LAS BASES EN DISCO, hermana de la de S117 sobre la canónica:
  **`app/educhronos-demo-m4.db` queda declarada HORARIO DE REFERENCIA DE S118** y no es un residuo temporal —es
  el único horario del centro real que existe y rehacerlo cuesta diez minutos con un cuarto de probabilidad de
  fallo—. Se borran `educhronos-demo-m4-capa1.db` y `educhronos-demo-m4-vacia.db`, reproducibles en segundos.
  Ninguna está trackeada (`.gitignore:12`).
  DEUDA — NO NACE NINGUNA, y ninguna se paga. Nada de lo medido es «algo mal hecho»: el hueco 219/208 es
  D31-a ya registrada, el `0` vacuo es limitación de datos ya declarada y el resto son hallazgos. **AFINADA
  D-diagnostico-no-es-foto**, y a la baja: esta sesión usó `DiagnosticoService` como instrumento de la capa 1
  y la deuda resultó VACUA aquí, porque la m4 no se ha tocado desde el 24 de agosto a las 17:47 y el catálogo
  vivo ES el del momento de generar. Queda escrito para que nadie lea el verde de la capa 1 como si la deuda
  no existiera: sobre una base que sí hubiera cambiado, el mismo diagnóstico respondería a otra pregunta.
  NOTAS DE MÉTODO PARA EL SACO QUE ESPERA CON EL SCRIPT DE R4 (pendiente desde S101) Y
  D-guion-exit-enmascarado, sin token nuevo (lección de D-tokens-inexistentes): un `pkill -f "spring-boot:run"`
  mata también al guion que lo invoca, porque su propia línea de órdenes contiene la cadena —usar
  `kill "$APP_PID"` y `wait`—; y un bucle de espera con `sleep` no sirve en este entorno, donde funciona
  `curl --retry N --retry-delay M --retry-connrefused`. Y un fallo de redacción del arquitecto senior,
  registrado porque es del mismo género que los anteriores: un guion titulado «capas 1 y 3» cuyo cuerpo solo
  inventariaba, porque los pasos de ejecución se escribieron en prosa FUERA del bloque copiable. Claude Code
  hizo bien en señalarlo en vez de improvisar el arranque.
  **INCUMPLIMIENTO DE R4 DE S118, DETECTADO EN ESTE M1 Y CORREGIDO AQUÍ:** la cabecera de S118 afirma haber
  actualizado «la frase de ventana del plan» y no lo hizo —seguía diciendo «el plan conserva ahora S116
  (degradada) y S117 como única cabecera H3 viva» con la ventana viva en S118—. La crónica de archivado sí se
  actualizó; la frase de ventana no. Misma familia que el «punto de abajo» inexistente de S116 que corrigió
  S117: una afirmación de higiene que el documento no sostiene.
  C-CARGA-MANUAL-1ESO: SIGUE SIN DECIDIRSE, y es la TERCERA sesión consecutiva (S117, S118, S119). Se registra
  como patrón, no como reproche: con C-generación cerrado es lo único que puede avanzar O-demo mientras el
  centro no responda lo de FPB, así que el objetivo queda esperando un correo si no se decide. La
  recomendación de S117 sigue en pie —versión RECORTADA: el bloque de seis plazas del §6.1 y la co-docencia de
  LCL sobre base de usar y tirar, en vez de 1º ESO entero, porque ahí está todo el valor probatorio—.
  LIMPIEZA (M1-bis): archivada S117 a `bitacora-sesiones.md` (promovida a `### Sesión 117`, insertada al final
  en orden ascendente, cuerpo íntegro); degradada S118 a «Última sesión registrada (previa)» compacta; S119
  queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S117), la crónica de
  archivado y la frase de ventana del plan —esta última CORREGIDA, ver arriba—. R4/costura: el script oficial
  SIGUE sin existir en el repo; verificado que el árbol quedó limpio, que ninguna de las bases está trackeada,
  que la canónica conserva su md5 y que esta sesión NO produce commits de código: solo documentación.
  O-demo (H2) ACTIVO, 3 piezas: C-derivación (S115), C-cargador (S116) y **C-generación (S117+S118+S119)
  CERRADA**. Suites INTACTAS, ningún módulo tocado: **app 282, solver 91, vitest 316, e2e 2**. Siguiente: no
  hay Cambio en curso. Los candidatos vivos son C-carga-manual-1eso (propuesto, sin decidir desde S117) y la
  respuesta del centro sobre las aulas de FPB, sin la cual O-demo no cierra. Lo fija su propio M0 (ver M1-ter).

### Sesión 120 — O-demo (H2): C-carga-manual-1eso en su versión RECORTADA POR ANCHO, y CIERRE del Cambio. El caso §6.1 tecleado A MANO por la interfaz sobre base vacía —dos grupos, 38 envíos, horario válido en 1 s— (M0 + M2 por Claude Code + M4 en navegador + M1, sin M3; cero líneas de producto). CUARTA pieza CERRADA. NO cierra el objetivo.

Vigésima sesión bajo el mapa Hito→Objetivo→Cambio, y del mismo tipo SIN ENCAJE EXACTO que S117 y S119 en un
punto —no escribe una línea de producto— pero distinta de las dos en otro: aquí SÍ hay M4, y el M4 ES la
sesión. Ritual M0 + M2 (una pasada de solo lectura por Claude Code) + M4 (tecleo en navegador por el
arquitecto) + M1, sin M3 porque no hay lógica que mutar. Ningún fichero de `app/src/main`,
`solver/src/main` ni `app/frontend` se toca; las cuatro suites quedan intactas (app 282, solver 91,
vitest 316, e2e 2). Lo que ENTREGA: la única distancia que O-demo tenía DECLARADA POR ESCRITO desde S115
queda cerrada en su parte difícil, y con ella el último Cambio ejecutable del objetivo.
M0 — apertura verificada contra `gestion_proyecto.md`. Objetivo = O-demo (H2), 3 piezas hechas al abrir.
Hito = H2. Cambio = C-carga-manual-1eso, PROPUESTO desde S115 y SIN DECIDIR durante tres sesiones
consecutivas (S117, S118, S119). No había Cambio en curso.
**EL M0 CORRIGIÓ EL PROMPT DE APERTURA en el punto que gobierna lo que la sesión puede prometer.** El prompt
afirmaba que C-carga-manual-1eso «es lo único que puede avanzar O-demo». Es más estrecho: el criterio de
terminado de O-demo pide el centro creado por las VÍAS LEGÍTIMAS DEL PRODUCTO, y S115 declaró la API REST
como vía legítima y no rediscutible. Lo que este Cambio cierra NO es el criterio de O-demo: es la NOTA DE
ALCANCE de S115 —la distancia entre lo demostrado y el paso 2 del guion de aceptación de §1, que exige
«crear un centro desde cero POR LA INTERFAZ»—. Consecuencia declarada por adelantado y cumplida: esta
sesión, saliendo perfecta, NO cierra O-demo.
LA ELECCIÓN, entre las dos que el prompt puso sobre la mesa (este Cambio o una sesión de Higiene/Método por
el script de R4 pendiente desde S101). Se eligió el Cambio, con cuatro argumentos y uno decide: **(1)** es
el único de los dos que puede nombrar los tres términos de R-apertura; **(2)** R-deuda excluye al otro
—ni D-guion-exit-enmascarado ni D-tokens-inexistentes bloquean el criterio del objetivo activo, y el
«Siguiente» que S119 dejó escrito no pide sesión de higiene—; **(3)** el que decide: teclear por UI es lo
único que puede DESTAPAR un hueco funcional de H2 a escala, y ése es exactamente el contenido de la
dependencia que O-diseño tiene sobre O-demo; si O-demo cerrara sin esto, esa comprobación no la haría ya
nadie, porque el e2e de S112 cubre nueve filas y el centro real entró por script; **(4)** la versión
recortada es barata. CONTRAARGUMENTO REGISTRADO Y PERDEDOR, para que conste que se pesó: el script de R4
tiene el mejor caso que ha tenido nunca, porque S119 midió que la verificación manual YA FALLÓ una vez (la
cabecera de S118 afirmaba una higiene que no hizo). Perdió por no bloquear.
DECISIÓN DE ALCANCE DEL ARQUITECTO SENIOR, y no es la que S117 recomendaba: **el bloque se teclea con DOS
grupos, 1ºA y 1ºB, no con uno ni con los cuatro.** Razón medida sobre el catálogo: cada una de las seis
plazas del bloque referencia CUATRO subgrupos, uno por grupo; con 1ºA solo, cada plaza se quedaría con UN
subgrupo y desaparecería el caso de «plaza que agrega subgrupos de varios grupos», que es la interacción de
UI más difícil y uno de los casos que S115 declaró expresable LEYENDO. Con los cuatro grupos serían
dieciocho altas más de lo mismo: volumen, no evidencia. Se recorta el ANCHO, no la cadena.
R-invalidación, dos condiciones reales: (a) todo corre sobre base de USAR Y TIRAR
(`app/educhronos-s120-manual.db`), porque la canónica debe conservar `sesion 0` para O-particiones y
`educhronos-demo-m4.db` es el único horario del centro real que existe; (b) O-diseño rehará el ASPECTO de
las vistas, no su comportamiento, así que ninguna mejora visual que aparezca se ejecuta aquí (R-terminado).
R-deuda: ninguna deuda abre la sesión. La única bloqueante sigue siendo D31-a, que bloquea O-demo y no este
Cambio; declarado por adelantado como en S117, S118 y S119.
M2 — UNA PASADA DE SOLO LECTURA POR CLAUDE CODE, sin arrancar nada y sin abrir ninguna `.db`. Su encargo NO
era ejecutar el ejercicio sino reunir los datos exactos para que no se tecleara un solo dato inventado.
Entregó `/tmp/m2-s120-informe.md` (740 líneas) con los códigos literales, los campos obligatorios de cada
servicio, el orden de creación y las huellas de las nueve bases en disco. TRES RESULTADOS QUE CAMBIARON EL
GUION: **(1)** la nomenclatura real de los subgrupos de CyR es `1ºA-CyR-INF1` / `1ºA-CyR-TEC3` (sufijo =
código del profesor) y NO `-Inf` / `-Tec` como escribe `modelo_datos_fase1.md` §6.1; manda el catálogo
derivado, que es lo que se cargó en la base real. **(2)** Seis de los ocho profesores y la asignatura `LCL`
vienen TRUNCADOS del PDF de origen y se teclean truncados, o dejan de coincidir con la base real. **(3)** El
código del aula de informática es literalmente `A12 Informática`, con espacio y con tilde.
**EL M2 CORRIGIÓ TAMBIÉN LA ESTIMACIÓN DEL ARQUITECTO SENIOR, dos veces y las dos a la baja**: 38 envíos y no
los «45-60» anunciados —no hacen falta tutorías, porque ninguna de las dos actividades pide tutor, ni ningún
PDC—, y veinte minutos y no «una a dos horas». La segunda falla importa más que la primera: **el coste
percibido era lo que llevaba tres sesiones frenando esta decisión.**
M4 — EL TECLEO. Base vacía creada por el arranque (21 tablas, cero filas), aplicación en `localhost:4200`,
38 envíos en el orden de dependencias: jornada (1), nivel (1), asignaturas (4), profesores (8), aulas (7),
grupos (2), subgrupos (13), actividades (2). Y generación al final.
**RESULTADO: NINGÚN CASO RESULTÓ INEXPRESABLE POR LA INTERFAZ.** El bloque de seis destinos alternativos se
construyó como UNA actividad de seis plazas, con dos subgrupos por plaza (multiselect por ctrl+clic), una
plaza con aula fija y cinco con candidatas; la co-docencia de LCL se construyó como UNA plaza con DOS
profesores. Los dos casos a la primera. El horario se generó en **1 segundo**.
**VERIFICACIÓN INDEPENDIENTE DEL HORARIO por el arquitecto senior, sobre las capturas y no sobre el «salió
bien»:** repeticiones correctas (LCL cuatro veces en cuatro días distintos, bloque dos veces en dos días,
las dos actividades DISTRIBUIDA); el bloque cae ENTERO en un solo tramo con sus seis plazas simultáneas y
seis aulas distintas (A12/A5/A11/A10/A3/A14 el jueves, A12/B07/A5/A14/A11/A3 el viernes); cada plaza dentro
de su lista de candidatas declarada; LCL pintando `LEN2, LEN8` en la misma celda; 1ºA sin dos cosas a la vez;
y la aritmética de S117 cuadrando a esta escala, **2 × 6 + 4 × 1 = 16 filas de `sesion`**.
**EL RIESGO QUE NO SE LE DIJO AL ARQUITECTO PARA NO DIRIGIR SU RESPUESTA, y que resultó ser el hallazgo
técnico de la sesión:** `1ºA-Completo` CONTIENE a los seis subgrupos del bloque, así que si LCL y el bloque
hubieran caído en el mismo tramo los alumnos estarían en dos sitios a la vez. No ocurrió, y se comprobó que
**no fue suerte**: el solver tiene regla dura propia de solape por grupo (`SOLAPE_GRUPO`, con
`RestriccionNoSolapeGrupo` y su test `mismoGrupoEnUnUnicoTramoEsInfactible`, más el de regresión
`desdobleNoSeReportaComoSolapeDeGrupo`). Dato de valor: **con el centro completo esa regla no se ejercita**,
porque los 26 grupos van a 30/30 y no queda hueco donde el conflicto pueda darse; este horario de juguete la
ejercita de verdad. Es evidencia que S119 no podía producir.
LO QUE SE ASEVERA, con las palabras exactas: **el caso más difícil del centro real —bloque de seis destinos
alternativos con subgrupos de dos grupos, y co-docencia— es CONSTRUIBLE A MANO POR LA INTERFAZ sobre una
base vacía, en 38 envíos y veinte minutos, y el solver produce horario válido sobre él.**
LÍMITES DE LA ASERCIÓN, declarados de una vez y sin adornar: **el guion decía QUÉ construir.** Que el bloque
de seis destinos es UNA actividad con seis plazas y no seis actividades, el arquitecto lo sabía porque se lo
dieron hecho. Lo demostrado es que **los formularios EXPRESAN el caso**, no que un usuario averigüe cómo
MODELARLO; esa segunda mitad sigue sin demostrar y no la demuestra ningún ejercicio cuyo guion escriba el
arquitecto senior. Además: n = 1 persona, y esa persona conoce el modelo; dos grupos de cuatro; 38 envíos de
los 815 del centro completo; sin PDC, sin tutorías y sin currículo ordinario. **La nota de alcance de S115 se
ESTRECHA, no desaparece**: lo demostrado por UI a escala real sigue sin estarlo.
HALLAZGOS DE PRODUCTO, los cinco. **(1) LA VISTA DE HORARIO RECIBE A UN CENTRO RECIÉN CONFIGURADO CON DOS
MENSAJES DE ERROR** —«No se pudo cargar el diagnóstico» y «No se pudo cargar el horario 1 (404)»— cuando lo
correcto es que todavía no haya horario. Nace D-vista-horario-sin-horario, y muerde en la cláusula
«presentable al centro» del criterio de O-demo: es literalmente lo primero que vería el jefe de estudios tras
configurar. Hermana del hallazgo de S118 sobre la prevalidación ciega al centro recién instalado.
**(2)** Los selectores de subgrupos, profesores y aulas son multiselect nativo por ctrl+clic, sin buscador ni
filtro: con 13 subgrupos va bien y con los 334 del centro real no (D-selectores-sin-busqueda, mejora futura).
**(3)** La etiqueta «— varias (una por plaza) —» se encontró pero resultó CONFUSA, y en el punto más difícil
del formulario; y la co-docencia no se declara, se deduce de poner dos profesores. Las dos son la misma cosa
—la forma de la actividad es implícita— y nace D-actividad-forma-implicita (mejora futura).
**(4)** Con seis plazas rellenas la pantalla del formulario queda «regular» de legible; se registra dentro de
la anterior y no se abre token propio. **(5)** Tras guardar la jornada, la pantalla sigue mostrando el DÍA
TIPO y no la semana expandida; no bloqueó nada y queda como observación, no como deuda: no se ha leído el
componente y afirmar que está mal sería suponer.
DOS ERRORES DEL ARQUITECTO SENIOR EN EL GUION, registrados porque son del mismo género que los de S119.
**(1)** El paso 10 mandaba «prevalidar y luego generar»: **ese botón no existe**. Se extrapoló de la nota de
la ficha de D-generacion-sin-indicador que dice que el botón de generar se deshabilita si no se ha
prevalidado. El mensaje «Catálogo sano» aparece solo. Queda ABIERTO si la prevalidación corre al cargar la
vista o si la nota está desfasada: NO se ha leído `horario-view.ts` y no se afirma.
**(2)** La orden de arranque que el guion daba llevaba DOS argumentos separados por coma dentro del mismo
`-Dspring-boot.run.arguments=`, tal como `plan:859` describe el mecanismo, y **la coma no separó nada**: la
base se creó con el nombre literal `educhronos-s120-manual.db,--educhronos.solver.max-segundos=60`. Se
rehízo con un solo argumento y el presupuesto se dejó en su defecto de 600 s, irrelevante para un problema
de 16 sesiones. Nace D-arranque-no-literal (de método).
R-DEUDA, RATIFICADA UNA VEZ MÁS Y POR EL LADO MÁS FÁCIL DE TODOS: nacen tres deudas y una mejora, y ninguna
se paga. Ninguna bloquea el criterio de O-demo. La sesión NO tocó ningún camino de fallo, así que tampoco
hubo nada que pagar DE PASO —a diferencia de S118—. Deuda bloqueante abierta: sigue en 1 (D31-a).
PREDICCIONES DECLARADAS ANTES DE TECLEAR, y su desenlace honesto. Se cumplieron la (2) —el paso de trece
subgrupos fue tedioso y funcionó, tres minutos—, la (3) —la plaza de `A12 Informática` no dio problema de
compatibilidad, como preveía el M2 por la rama `compatibles.isEmpty()`— y la (5) —generación instantánea—.
La (4) no se ejercitó: el orden se respetó. **LA (1) QUEDA EJERCITADA PERO NO OBSERVADA, y así se registra
en vez de darla por confirmada:** hubo uno o dos rechazos, el arquitecto los atribuye a erratas propias
—saltarse un paso por descuido mientras atendía otra tarea— y NO se anotó si la pantalla decía por qué. La
deuda D-F8.6-ii-a no gana ni pierde evidencia en esta sesión.
DECISIÓN SOBRE LA BASE DEL EJERCICIO: `app/educhronos-s120-manual.db` es DE USAR Y TIRAR y se borra al
cerrar, a diferencia de `educhronos-demo-m4.db` (declarada horario de referencia en S119). Rehacerla cuesta
veinte minutos de tecleo, pero no contiene nada que no esté en este registro y en el guion. Se borra también
la base con el nombre corrupto del intento fallido. Ninguna está trackeada (`.gitignore:12`).
INTEGRIDAD: la canónica `app/educhronos-demo.db` sigue en `f5b542ebeef40144f448bb24245a4b2d` con `sesion 0`
—huella tomada por el M2 ANTES de teclear y verificada al cerrar— y `educhronos-demo-m4.db` en
`ea1a70a0337831dddccdbcd322f48e9b`. Ninguna de las dos se abrió en toda la sesión.
**C-CARGA-MANUAL-1ESO CIERRA** en su versión recortada por ancho, que es la que S117 recomendó y la que S120
ejecutó con la corrección de los dos grupos. **O-DEMO NO CIERRA**: sigue esperando las 11 actividades de FPB
(D31-a). Con este Cambio cerrado, **O-demo se queda SIN TRABAJO EJECUTABLE**: los cinco Cambios de su ficha
están hechos o retirados y lo único pendiente es un correo al centro.
LIMPIEZA (M1-bis): archivada S118 a `bitacora-sesiones.md` (promovida a `### Sesión 118`, insertada al final
en orden ascendente, cuerpo íntegro); degradada S119 a «Última sesión registrada (previa)» compacta; S120
queda como única cabecera H3 viva. Actualizados los dos censos de la bitácora (→ S10–S118), la crónica de
archivado y la frase de ventana del plan. R4/costura: el script oficial SIGUE sin existir en el repo (mejora
de método pendiente desde S101, y esta sesión le añade un tercer caso concreto, D-arranque-no-literal);
verificado que los cuatro tokens nuevos tienen definición viva en este plan y citante vivo en
`gestion_proyecto.md` §4 (lección de D-tokens-inexistentes), que el árbol queda limpio, que ninguna base está
trackeada y que esta sesión NO produce commits de código: solo documentación.
O-demo (H2) ACTIVO, 4 piezas: C-derivación (S115), C-cargador (S116), C-generación (S117+S118+S119) y
**C-carga-manual-1eso (S120) CERRADA**. Suites INTACTAS, ningún módulo tocado: **app 282, solver 91,
vitest 316, e2e 2**. Siguiente: O-demo no tiene trabajo ejecutable hasta que el centro responda, así que lo
que toca es **abrir O-diseño**, cuyo criterio de terminado está POR DEFINIR y cuya dependencia sobre O-demo
—«que O-demo destape un caso inexpresable»— queda consumida por esta sesión. Lo fija su propio M0 (ver M1-ter).

---

### Sesión 121 — O-diseño (transversal): APERTURA del objetivo, definición de su criterio de terminado y sus cuatro Cambios; C-tokens y C-sustitución HECHOS. Todo el color y el tamaño del frontend pasa a una capa de tokens en `styles.css`, con paleta azul institucional elegida por el jefe de estudios (M0 + M2 + C-tokens + C-sustitución en dos tandas + M4 en navegador + M1, sin M3). DOS de cuatro piezas. NO cierra el objetivo.
  Vigesimoprimera sesión bajo el mapa Hito→Objetivo→Cambio, y la primera de tipo Configuración/UI del mapa:
  M4 sí, M3 no, porque no hay lógica que mutar —la sesión reescribe CSS y no toca una línea de TypeScript ni
  de Java—. Las cuatro suites quedan intactas (app 282, solver 91, vitest 316, e2e 2); vitest se corre porque
  el frontend cambia, y pasa sin tocar un solo test.
  M0 — LA APERTURA, que es el trabajo de verdad de esta sesión. **O-diseño ABRE** con O-demo todavía ABIERTO,
  primera vez que dos objetivos conviven en el mapa; se declara expresamente que no es una pausa por
  conveniencia sino un bloqueo externo (D31-a espera un correo al centro) y que nada del método lo prohíbe.
  La dependencia «O-demo puede destapar un caso inexpresable» se verifica CONSUMIDA por tres vías y no por
  una: S115 (catálogo completo contra la UI, por lectura), S116 (804 escrituras por los mismos servicios que
  respaldan los formularios) y S120 (el caso más difícil tecleado a mano). Lo que le falta a O-demo son 11
  actividades de FPB con aula, de la misma forma que las 208 ya cargadas. Las dos alternativas se descartan
  con argumento escrito: la Higiene/Método del script de R4 pierde por TERCERA vez —no puede nombrar los tres
  términos y R-deuda excluye sus casos— y O-particiones no se adelanta —la demo no lo necesita y arrastra
  cuatro preguntas de dominio sin resolver—.
  M0 (2) — EL CRITERIO DE TERMINADO, que la ficha tenía POR DEFINIR desde S106. Cuatro cláusulas, tres
  verificables por grep y una de juicio del arquitecto (ver `gestion_proyecto.md` §3). **La decisión de
  diseño que gobierna todo lo demás: el criterio se define como SISTEMA y no como maquetación vista a
  vista**, y la razón es de invalidación, no de gusto: la ficha de O-particiones dice que toca frontend, así
  que si O-diseño maqueta a mano, la UI que traiga O-particiones invalida ese trabajo —exactamente el riesgo
  que motivó la dependencia sobre O-demo—. Con el criterio como sistema, la UI nueva nace aplicándolo. Ésa
  fue la alternativa a invertir el orden de §5, y se eligió por ser más barata y menos arriesgada.
  M0 (3) — LO QUE QUEDA FUERA, decidido expresamente para que el objetivo pueda terminar. Las seis deudas de
  UX que la tabla §4 le cuelga (D-selectores-sin-busqueda, D-actividad-forma-implicita,
  D-configuracion-monolitica, D-actividad-ux, D-subgrupo-ux-multiselect, D-monodi-botones-inertes) más
  D-dialogo-foco-perdido: si entran, el acabado se convierte en rehacer la UI. Fuera también el responsive
  (nace D-sin-puntos-de-ruptura) y **D-vista-horario-sin-horario, que era la decisión que S120 dejó
  encargada al M0 de esta sesión**: queda FUERA por el corte comportamiento/aspecto, sigue colgando de
  O-demo y se anota como candidata a Cambio corto antes de la demo, haciendo constar que su clasificación
  «No bloquea» es discutible en lectura estricta de la cláusula «presentable al centro».
  M2 — INVENTARIO DE LA SUPERFICIE VISUAL, por Claude Code y de solo lectura, porque el criterio no se podía
  escribir desde la documentación: no había en ningún documento un censo de la superficie. Medido: 26
  componentes, 1.346 líneas de HTML y 1.541 de CSS en 26 hojas; **uniformidad total** —los 26 con
  `templateUrl` + `styleUrl` externos, cero plantillas inline, cero estilos inline, cero `style=` en
  plantillas—, así que no había nada que desenredar antes de tocar el aspecto. **CERO tokens**: ni una
  definición `--x:` ni un `var(--x)` en todo `src`; el `styles.css` global eran 5 líneas y solo servía para
  importar el `overlay-prebuilt.css` del CDK. 26 hex distintos, dominados por `#b00` (48 usos) y `#666` (36);
  cinco grises casi indistinguibles conviviendo. **Ningún `font-family` en ninguna parte** —se heredaba el
  default del navegador— y 10 tamaños distintos. **Cero `@media`.** Sin librería de UI (solo `@angular/cdk`
  para el diálogo). Lo único sólido de partida: la convención BEM, 230 de 252 selectores con prefijo por
  componente y sin colisiones. Bundle de partida: 522,11 kB, con el aviso de budget YA encendido.
  ELECCIÓN DE PALETA. Se descartó Claude Design y se descartó buscar referencias fuera: se entregó un HTML
  autocontenido y desechable (NO entra en el repo) con las mismas piezas reales de producto —cabecera, lista
  de configuración, formulario de actividad con multiselect y campo en error, los cinco tipos de aviso,
  rejilla 6×5 con co-docencia, bloque de seis destinos, celda en conflicto y celda fijada, y un diálogo—
  conmutables entre paletas. El asistente recomendó «oliva y cobre» por una razón práctica (deja el rojo
  entero libre para el error y el ámbar libre para el aviso); **el arquitecto eligió AZUL INSTITUCIONAL
  porque lo prefiere el jefe de estudios**, y el asistente registró que no hay objeción técnica: el azul
  cumple la misma propiedad que motivaba su recomendación. Se ofreció una quinta variante (azul sobre papel
  cálido) para reconciliar el «prefiero cálidos» del arquitecto con el azul del cliente; se rechazó. Manda
  el cliente, y queda escrito.
  **C-TOKENS HECHO.** `styles.css` pasa de 5 a ~90 líneas con la capa completa: 17 tokens de color, la
  tipografía (**fuente del sistema, NO web font autohospedada: decisión tomada por el asistente y
  justificada por H4 —el bundle debe funcionar en un Windows limpio y sin red—**), la escala de tamaños, el
  espaciado, los radios y la sombra. Restricción de CSS respetada y documentada en el propio fichero: **el
  `@import` del CDK debe seguir siendo la PRIMERA regla**, o el navegador lo descarta y el diálogo pierde
  centrado y fondo. Se paga de paso **D-bundle-presupuesto** (`maximumWarning` 500 → 550 kB, error intacto
  en 1 MB): no bloqueaba, pero su sede era O-diseño y el hecho nuevo lo justificaba —el aviso ya estaba
  encendido antes de tocar nada, luego era ruido y no señal—. Delta de la sesión: **522,11 → 524,94 kB**,
  ~25 kB de margen; el comprimido incluso BAJA (118,11 → 118,01 kB) porque los `var(--color-*)` repetidos
  comprimen mejor que hex variados.
  **C-SUSTITUCIÓN HECHO, en dos tandas por decisión de riesgo.** Tanda 1: los 22 ficheros de chasis
  (formularios, listas, diálogos), 145 líneas sustituidas, mecánicas y con el mismo patrón. Tanda 2: los
  cuatro ficheros donde vive la semántica (`horario-grid`, `horario-view`, `panel-prevalidacion`,
  `jornada`), 47 sustituciones ancladas LÍNEA A LÍNEA con guarda que aborta si el fichero no está como el
  mapa dice, porque ahí el mismo hex significa cosas distintas según dónde esté. Resultado: **cero literales
  de color y cero `font-size` sin token fuera de `styles.css`**, que es la cláusula 1 del criterio.
  LAS TRES DECISIONES DE MAPEO QUE NO ERAN MECÁNICAS, con su argumento, porque son las que se podrían
  querer revisar. **(1)** `#b00` (44 usos), `#b00020` y `#c33` colapsan todos en `--color-error`: la
  distinción que justificaría un `--color-peligro` aparte —error de validación frente a acción
  destructiva— no existía en el CSS, los dos usaban el mismo hex, y un segundo token sin diferencia visual
  confunde más de lo que aclara. Efecto visible: el rojo se oscurece de `#bb0000` a `#a32014` en 44 sitios.
  **(2)** `#666` se DESDOBLA según la propiedad: en `border` va a `--color-borde`, en `color` va a
  `--color-tinta-suave`. Es el cambio visual más grande de la sesión —los bordes de todos los inputs se
  aclaran de golpe— y se declaró por adelantado antes de aplicarlo. **(3)** El mismo `#4a7` va a DOS tokens
  distintos en el mismo fichero, y es intencional: en `.cdk-drop-list-dragging` es un veredicto («aquí
  puedes soltar») y va a `--color-ok`; en `.entrada` es la barra estructural que llevan TODAS las tarjetas
  y va a `--color-acento`, porque mandarla a `--color-ok` le daría un significado que no tiene y un horario
  entero de barras verdes se leería como «todo validado», arruinando además la señal cuando algo SÍ esté
  correcto. Que compartieran hex era el accidente que el objetivo venía a deshacer.
  EL BADGE MORADO: el par `#ece4f4`/`#402a52` de `horario-grid` era la única familia sin token equivalente,
  y se le da uno propio (`--color-marca` / `--color-marca-fondo`) CONSERVANDO el morado exacto, en vez de
  colapsarlo en acento. Razón: la línea 100 ya manda la barra de `.entrada` a `--color-acento`, así que un
  badge azul pondría dos señales distintas del mismo color a diez píxeles una de otra. **QUEDA SIN SABER
  QUÉ NUMERAN el `1` y el `-1` del badge**; no está en la documentación y el asistente se negó a
  inventarlo. Si resulta que significan algo que el jefe de estudios debe entender, es material de
  C-identidad.
  AMPLIACIÓN DE `:root` DURANTE EL TRABAJO, cuatro tokens que el inventario no podía prever y la
  sustitución destapó: `--color-superficie-tenue` (la capa 1 de `.entrada`, que NO puede ir a
  `--color-superficie-alt` sin aplanar la señal de `td.ocupado` —lo advertía el propio comentario de
  `horario-grid.css:119` sobre las dos capas de `background` ocupadas—), `--sombra-suave`, `--tam-xxs`
  (0,6875rem, para que los `0.65rem` y `0.7rem` de la rejilla no engorden subiendo a `--tam-xs`) y el par
  `--color-marca`. Se rechazó ampliar más: los seis literales ámbar de la tanda 2 son SEIS TONOS PARA UNA
  IDEA, acumulados por sesiones sin coordinación, y colapsarlos es el objetivo, no forzar el mapa. **Un
  token existe porque hay un significado distinto, no porque haya un hex distinto.**
  M4 EN NAVEGADOR, sobre COPIA de `educhronos-demo-m4.db` en `/tmp` y no sobre la base de referencia, con
  md5 verificado antes y después. Las tres capturas pasan: en la rejilla el badge morado, la barra azul y el
  fondo de pinada siguen distinguiéndose entre sí; en el formulario el rojo nuevo se lee bien y los bordes
  claros mejoran; en jornada la fila de recreo sigue distinguiéndose con `--color-superficie-alt`, que era
  el riesgo de aplanar cuatro grises en uno. Ninguna señal se comió a otra. **Lo que el M4 NO pudo ver, y se
  registra en vez de disimularse:** el panel de prevalidación con hallazgos, porque nadie —ni la
  documentación ni el asistente— sabe qué dispara un AVISO o un ERROR, y se rechazó mandar al arquitecto a
  probar a ciegas. Nace D-prevalidacion-contraste-sin-ver, con sede en C-revisión.
  CINCO CASOS NUEVOS DE DEUDAS DE MÉTODO, **todos detectados por Claude Code revisando guiones del asistente,
  y ninguno sufrido**. Cuarto y quinto de D-guion-exit-enmascarado: un comprobador del `@import` cuyo patrón
  casaba con líneas de continuación de comentario (verde sin medir nada) y un recuento de cierre que incluía
  `styles.css`, donde los literales DEBEN vivir, haciendo su objetivo inalcanzable por construcción. El más
  importante de los cinco no llegó a fallar: **`var(--token-inexistente)` NO rompe el build** —la
  declaración se descarta en el navegador—, así que un build en verde no prueba nada sobre los tokens. De
  ahí sale el **comprobador de cuatro vías** que queda vivo para el resto de O-diseño (literales,
  `font-size` sin `var()`, `var(` mal formado, y tokens referenciados contra los definidos, quitando
  comentarios antes de buscar). Tercer hecho de D-arranque-no-literal, y éste sí es un error del asistente:
  pidió los `git add` del cierre dando por hecho un árbol sin commitear que había RECONSTRUIDO de un turno
  anterior en vez de leerlo, y produjo dos commits cuyos mensajes no describían su contenido. Nada se perdió
  y se corrigió con `reword` antes de pushear. La lección es la misma de siempre: **el estado se lee, no se
  reconstruye.**
  LIMPIEZA (M1-bis): archivada S119 a `bitacora-sesiones.md` (promovida a `### Sesión 119`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S120 a «Última sesión registrada (previa)»; S121
  queda como única cabecera H3 viva. R4/costura: el script oficial SIGUE sin existir en el repo (mejora de
  método pendiente desde S101, y esta sesión le añade dos casos más); verificado que los tokens nuevos de
  esta sesión tienen definición viva en este plan y citante vivo en `gestion_proyecto.md` §4 (lección de
  D-tokens-inexistentes), y se registra que el comentario de `horario-grid.css:119` se actualizó porque
  citaba un literal que ya no existe —un comentario que cita algo inexistente es la misma familia que
  D-tokens-inexistentes, luego era costura R4 y no cosmética—.
  O-diseño (transversal, abierto en S121) ACTIVO, 4 Cambios: **C-tokens (S121) HECHO**, **C-sustitución
  (S121) HECHO**, C-identidad PENDIENTE, C-revisión PENDIENTE. O-demo (H2) sigue ABIERTO y sin trabajo
  ejecutable. Suites INTACTAS: **app 282, solver 91, vitest 316, e2e 2**. Siguiente: **C-identidad**, que es
  donde está lo que más se nota en una demo —la cabecera, la marca, la landing, el aspecto de los estados
  transversales y el título del horario, que hoy pinta un timestamp ISO con nanosegundos crudo
  (`Horario 2026-08-24T15:37:39.317184258Z — grupo: 1B-A`)—, y después C-revisión. Se decidió NO empezar
  C-identidad en S121 con el presupuesto de contexto a la mitad: no es trabajo mecánico, exige enseñar
  capturas y discutirlas, y dejarlo a medias obliga a reconstruir contexto en la sesión siguiente. Lo fija
  su propio M0 (ver M1-ter).

### Sesión 122 — O-navegación (transversal): FUNDACIÓN del objetivo por medición sobre el centro real y diseño de la navegación en maqueta viva. Configuración pasa a rutas hijas, la rejilla a altura de fila constante y el recreo se hace visible; 18 decisiones escritas con su alternativa descartada (M0 + M2 sobre la base del centro + M4 en navegador iterando maqueta + M1, sin M3). Cero líneas de producto. NO cierra el objetivo: lo ABRE.
  Vigesimosegunda sesión bajo el mapa Hito→Objetivo→Cambio, y la segunda —tras S120— cuyo M4 ES la sesión.
  Ritual M0 + M2 (medición por Claude Code sobre `app/educhronos-demo-m4.db` en SOLO LECTURA) + M4 (siete
  iteraciones de maqueta en navegador, juzgadas por el arquitecto) + M1, sin M3 porque no hay lógica que
  mutar: **no se toca un solo fichero de `app/`**. Las cuatro suites se EJECUTAN —no se declaran intactas de
  memoria— y salen verdes: **app 282, solver 91, vitest 316, e2e 2** (e2e no se corre: levanta backend y
  reescribe `educhronos-e2e.db`; su cifra es trivial de verificar en fuente). Integridad: la base de
  referencia queda en `ea1a70a0337831dddccdbcd322f48e9b`, idéntico antes y después.
  LO QUE ENTREGA: `docs/diseno-navegacion.md`, la medición que FUNDA **O-navegación**, objetivo nuevo y
  transversal, hermano de O-diseño —uno hace el ACABADO, el otro el MANEJO—. Su ficha entra en
  `gestion_proyecto.md` §3 con criterio de seis puntos y tres Cambios PROPUESTOS a ratificar en la apertura
  (C-rutas-hijas, C-listas-paginadas, C-rejilla-densidad). Con él se toma por fin **la decisión
  ruta-hija-vs-contenedor aplazada desde S101**: rutas hijas con `router-outlet`, ocho destinos con URL
  enlazable, `/configuracion` redirige a `jornada` y `loadComponent` queda APLAZADO, no descartado. Absorbe
  `D-configuracion-monolitica` y `D-pdc-lista-rancia`, que llevaban desde S115 y S113 remitiendo las dos a
  «el Cambio que decida la navegación»; la segunda muere por construcción, sin el `EventEmitter` ad hoc que
  su ficha desaconsejaba.
  MEDICIÓN (M2), y lo que desmintió. Las insignias `1`/`-1` de la rejilla son la **suma CON SIGNO del coste
  blando de la INSTANCIA** —`horario-grid.ts:50-57`, `horario/diagnostico.ts:76-89`—, no un contador de
  entradas: `>0` mover mejora, `<0` tapa un hueco. Queda RESUELTA la pregunta que S121 dejó abierta. La
  vista de horario no pinta el recreo porque la proyección numera 1..6 con recreos EXCLUIDOS
  (`SesionVistaDTO.java:11`), aunque el dato ya viaja en `GET /api/jornada`. Y **tres premisas del encargo
  cayeron**: 1B-A tiene celdas de CINCO sub-entradas y no de cuatro; el peor caso del centro no está en
  1B-A sino en **1ºA**, el grupo que el arquitecto había descartado por fácil, con una celda de SEIS; y
  `D-generacion-sin-indicador` está CERRADA desde S118, no viva.
  DISEÑO (M4). La palanca que más devuelve es comprimir la sub-entrada de cuatro líneas a dos, no el
  colapso; después, altura de fila CONSTANTE derivada del presupuesto de 1920×1080, con la densidad
  moviéndose DENTRO de la celda —una clase suelta a dos líneas, un bloque a una línea por plaza bajo un
  rótulo común—. El recreo entra como fila propia, sin hora de reloj: en `tramo_semanal` las horas son
  enteros cuya zona no se ha verificado, y no se inventa una. La cuarta línea (`grupos`) se condensa en una
  marca `+N` consultable, medido que repetía el grupo que ya se está mirando en 23 de 51 sub-entradas de
  1B-A. Título y controles se funden en una fila, que fue lo que eliminó el último resto de scroll.
  **D11 SE INVIRTIÓ, y ése es el hallazgo caro de la sesión.** El mecanismo de expansión iba a NO
  construirse porque el contador daba 0 celdas recortadas. Calculado sobre los 28 grupos y sus 791 celdas,
  son **22 de 791** —las de seis plazas, en 11 grupos de 28, dos por grupo, producidas por cinco bloques de
  optativas—, y no se arregla con presupuesto: cero recortes exigiría 154,2 px de fila y dejaría 96,2 px de
  cromo total, inalcanzable. Que 1ºA «cupiera sin scroll» no probaba que cupiese: cabía **porque el
  prototipo la recortaba**. El mecanismo pasa a OBLIGATORIO (criterio 4). Las tres palancas que bajarían de
  154,2 px quedan registradas como decisión consciente, medidas y NO aplicadas: la pregunta caducó al
  hacerse obligatorio el mecanismo.
  LECCIÓN DE MÉTODO, que se lleva a `metodo.md`. El mismo error apareció TRES veces en esta sesión, dos de
  ellas al corregir la anterior: el contador de desbordes medía el `<td>`, que crece con su contenido y
  jamás desborda; el recuento de tests del solver contaba ANOTACIONES (97) creyendo contar tests (91); y la
  cifra «ejecutada» de `app` dio 283 al sumar `target/surefire-reports` sin ver que un informe era un
  huérfano del 24-ago (`BarridoPresupuestoS117`, arnés desechable cuyo fuente ya no existe) — **app son
  282 y las cuatro cifras de referencia del proyecto eran correctas**. La forma es siempre la misma: el
  instrumento se apoya en algo que SE PARECE al conjunto medido y no lo es. La regla se CONDENSA en la
  segunda precisión de M2 (no se añade una quinta: lo prohíbe su propia nota de acumulación), citando los
  tres precedentes S117/S121/S122, con `scripts/calcular-recortes.py` y sus cinco mutaciones como ejemplar.
  DEUDAS NUEVAS (R-deuda: se registran, no se planifican): **D-insignia-sin-leyenda** (la insignia de coste
  blando es un número desnudo con signo, sin `title` ni `aria-label`, a diferencia del candado; sede
  C-identidad) y **D-asignatura-sin-nivel** (`Asignatura` no tiene nivel; la relación solo se deduce por
  Actividad→Plaza→Subgrupo→Grupo→Nivel, y el selector de asignatura de una actividad de 1ºESO ofrece las de
  3º y 4º; sin sede, toca modelo). Se registra además que la decisión 3 de las seis de O-diseño
  —densidad/espaciado— **SIGUE SIN SEDE**: el espaciado no está tokenizado y `padding-top: 16px` en
  `horario-grid.css:43-45` sobrevivió entero a C-sustitución, que solo cubrió color y `font-size`.
  DECLARACIONES NUEVAS: la **resolución objetivo se fija en 1920×1080**, valor que el criterio de O-diseño
  venía exigiendo desde S121 sin que nadie lo hubiera escrito en ningún documento; la del portátil queda
  como PARÁMETRO SIN FIJAR del que depende todo el cálculo de altura.
  COSTURA CERRADA: las tres normas de documentación que este cierre empujó al método —**M-doc**, **M-doc-2**
  y **M-doc-3**— se titulan «(S122)» y esta cabecera es la sesión que las respalda. La anotación que quedó
  al empujarlas, con S122 aún sin existir en el registro, se salda aquí.
  LIMPIEZA (M1-bis): archivada S120 a `bitacora-sesiones.md` (promovida a `### Sesión 120`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S121 a «Última sesión registrada (previa)»; S122
  queda como única cabecera H3 viva. **Los DOS censos de la bitácora estaban descuadrados ENTRE SÍ al
  empezar** —cabecera en S119 y línea de orden en S118—: S121 actualizó uno y no el otro. Los dos pasan
  ahora a S120, y el script nuevo los compara, que es la razón de que se detectara.
  R4/R5: **el script oficial de cierre YA EXISTE**, por fin, tras estar pendiente desde S101 y haber sido
  registrado como ausente en S121: `scripts/verificar-cierre.py`, con las cuatro comprobaciones de
  §Automatización del cierre y con AUTOPRUEBA propia (se le inyectan cuatro defectos que debe detectar y
  aborta si no los ve; sería incoherente escribir la precisión de M2 y saltársela en el mismo cierre).
  Reporta, no corrige. Censo R4: 178 tokens vivos, 28 con una sola aparición en el corpus vivo; los nuevos
  de esta sesión quedan con definición y citante, y los históricos se reportan sin tocar.
  O-navegación (transversal, ABIERTO en S122) con 3 Cambios PROPUESTOS y ninguno hecho. O-diseño
  (transversal, abierto en S121) ACTIVO: C-tokens y C-sustitución HECHOS, C-identidad y C-revisión
  PENDIENTES. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro. Orden
  nuevo, que sustituye al de la ficha de O-diseño: sistema (hecho) → **O-navegación** → C-identidad y
  C-revisión sobre la UI definitiva → demo. Suites INTACTAS y EJECUTADAS: **app 282, solver 91, vitest 316,
  e2e 2**. Siguiente: la APERTURA de O-navegación, donde el M0 ratifica los tres Cambios en vez de
  deliberarlos —salen uno a uno de los criterios 2, 3 y 4— y elige por cuál empezar. Lo fija su propio M0
  (ver M1-ter).

### Sesión 123 — O-navegación (transversal): APERTURA del objetivo y C-rutas-hijas HECHO. Configuración pasa de una pantalla con ocho listas apiladas a ocho destinos con URL propia, índice vertical derivado de las rutas, cabecera fija y contador inline; la medición del portátil real desmiente el presupuesto de altura de S122 y lo saca del criterio (M0 + M2 con el instrumento de S122 + M4 en navegador + M1, sin M3). UNO de tres Cambios. NO cierra el objetivo.
  Vigesimotercera sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI: M4 sí, M3 no —enrutado y
  binding, sin lógica que mutar—. Suites: **app 282, solver 91, vitest 314, e2e 2**. vitest BAJA de 316:
  mueren los 8 casos de `configuracion.spec.ts` y entran 6 nuevos; app y solver no se tocan.
  M0 — LA APERTURA RATIFICA, NO DELIBERA. Los tres Cambios de la ficha salen uno a uno de los criterios 2, 3
  y 4 y se ratifican los tres. **`C-listas-paginadas` se RENOMBRA a `C-listas-filtradas`**: el criterio 3
  admite paginador o filtro, pero el que tiene evidencia medida es el segundo, y D16 no diseña paginador
  ninguno. Un nombre que apunta a la opción sin evidencia acaba construyéndola. El token viejo se conserva
  vivo por equivalencia en la ficha de §3, porque el cuerpo de S122 en el plan sigue citándolo y el registro
  histórico no se reescribe. Se empieza por **C-rutas-hijas** por dependencia estructural: el filtro del
  criterio 3 actúa sobre las listas DE UN DESTINO, y los destinos no existen hasta que este Cambio los crea;
  filtrar antes sería construir contra el monolito y rehacerlo (R-invalidación).
  LA MEDICIÓN QUE LO CAMBIA TODO. `OBJETIVO.alto` no era 1080. El portátil del jefe de estudios tiene panel
  1920×1080 **con escala de Windows al 150 %**, y Chrome maximizado reporta un viewport de **1280×585**
  (`devicePixelRatio` 1.5, escritorio 1280×720). No es «1080 menos un poco»: es la mitad. Es la misma forma
  de error que S122 llevó a M2 —el instrumento apoyado en algo que SE PARECE al conjunto medido—: «1920×1080»
  se parece a la altura disponible y no lo es.
  EL INSTRUMENTO CONFIRMA EL DAÑO. `scripts/calcular-recortes.py`, conservado en S122 para esto, corrido con
  su autoprueba y con **escenario de control**: a 945 px (escala 100 %) reproduce clavado lo que S122
  escribió —22 celdas de 791, 2,8 %, solo las de seis plazas—, lo que valida el instrumento antes de creerle.
  A **585 px son 109 de 791, el 13,8 %**, y alcanzan a todas las celdas de tres plazas o más; a 720 px
  (escala 125 %) son 72, el 9,1 %. Se cae con ello el argumento escrito de D11: al 14 % el colapso deja de
  ser «el caso excepcional» y pasa a ser la interacción habitual.
  Y LAS PALANCAS NO LLEGAN, ni son tres. Sumadas las tres medidas en S122 —badge 16 px, padding y margen de
  plaza 25,2 px, `.asig` a `--tam-xs` 15 px— la celda de seis baja de 154,20 a 98,00 px, contra un alto de
  fila de 87,73: **faltan 10,27 px**. Además la palanca (a) **choca con D4**, que pone el rótulo común del
  bloque en la banda que el badge reservaba: aplicarla borra a la vez el badge de coste blando y el rótulo, y
  eso es eliminar una señal existente, que es justo lo que la invariante del encargo prohíbe y por lo que D6
  rechazó borrar la cuarta línea. **Son dos palancas disponibles, no tres.**
  HALLAZGO, con sus palabras: **el diseño de §4 de S122 es correcto, y lo es SOLO a escala 100 %**. No estaba
  mal medido; estaba medido sobre un supuesto de escalado que nadie declaró. La medición no invalida S122:
  acota dónde vale.
  DECISIÓN — el portátil SALE del criterio 4. D0-1 declaró 1920×1080 como la resolución de LA DEMO, y la demo
  se enseña en un ordenador de sobremesa, verificado con el jefe de estudios en esta sesión. El criterio 4 se
  verifica donde se juzga el producto. **D0-2 queda CERRADO por medición y exclusión**, no diferido: deja de
  ser «parámetro sin fijar» y pasa a «medido en 1280×585 con escala 150 %; excluido del criterio». Descartada
  la alternativa de forzar el portátil a 100 % (texto del sistema a ~11-12 px físicos: decisión sobre la
  vista del usuario, no sobre CSS) y la de rediseñar la celda con la mitad de presupuesto (sesión de
  medición completa, y ningún criterio la exige). Pendiente antes de C-rejilla-densidad: MEDIR el sobremesa,
  no suponerlo.
  C-rutas-hijas — HECHO, criterio 2 CUMPLIDO. Ocho destinos con URL enlazable bajo `/configuracion`,
  `redirectTo` a jornada, índice VERTICAL **derivado de `routeConfig.children`** y no de un array aparte —el
  rótulo vive en `data.titulo` de cada ruta, de modo que un noveno destino es una entrada nueva y no dos
  ediciones—, cabecera fija por `sticky` y contador inline en las siete listas CRUD. Jornada queda sin
  contador ni cabecera fija: es singleton y su «Guardar jornada» va al pie, así que clavar su cabecera
  dejaría el guardar fuera (D17 otra vez). Cerrada la decisión **ruta-hija-vs-contenedor aplazada desde
  S101**, veintidós sesiones.
  DEUDAS CERRADAS. **D-configuracion-monolitica**: la página ya no hace scroll; cada destino se desplaza
  dentro de su panel con la cabecera clavada. **D-pdc-lista-rancia**, por construcción y no con parche: el
  bug existía y queda localizado —`grupo-lista.ts:121-128` recarga solo grupos tras el alta de PDC y nadie
  avisa a `SubgrupoLista`—; con destino propio, entrar en subgrupos remonta y recarga. Su ficha desaconsejaba
  expresamente el `EventEmitter` ad hoc, y no se ha escrito ninguno.
  DECISIÓN CON SEDE AQUÍ, y va más allá del Cambio: el scroll pasa del documento a `.app__contenido` en TODA
  la aplicación, con la barra permanentemente visible también en landing y horario. Era inevitable —no hay
  forma de quitarle el scroll a la página sin gobernar el shell— y se acepta por lo que devuelve: con la
  barra fuera del flujo, **`OBJETIVO.alto` de D1 pasa a ser derivable en CSS** (`100dvh` menos la barra, con
  cadena flex y sin un solo literal de píxeles) en vez de un número que alguien tenía que medir y escribir.
  DEUDAS NUEVAS (R-deuda: se registran, no se planifican). **D-cabecera-lista-duplicada** (mejora futura): al
  descartar el componente de cabecera compartido, las siete reglas `__contador` se suman a los siete CSS ya
  duplicados. Deliberado: el filtro de C-listas-filtradas es quien conoce la forma que la cabecera necesita, y
  extraerla antes sería diseñarla a ciegas; su sede es ese Cambio, donde la extracción devuelve más. Se
  registra como **LIMITACIÓN CONOCIDA** que el horario del centro real no cabe sin scroll en el portátil a
  escala 150 %, con criterio de reapertura escrito: reabre si el uso diario pasa a ese equipo o si se fija su
  escala al 100 %. NO se abre deuda por el contador dentro del `h2` —«Subgrupos 334» sin separación para
  lector de pantalla—: el barrido de accesibilidad de C-identidad ya está convocado por
  `D-insignia-sin-leyenda` y pasará por esta cabecera; queda dicho aquí y no como token.
  COSTURA CERRADA: `diseno-navegacion.md` §5 lista las palancas como SIN DECIDIR y `gestion_proyecto.md` §4
  las clasifica como decisión consciente FUERA de la cola. Manda la segunda —es la autoridad de planificación
  y es posterior—, y la corrección de «tres palancas» a dos se aplica en §4.
  MÉTODO — la precisión de M2 de S122 funcionando cuatro veces en una sesión, y las cuatro autodetectadas por
  el ejecutor: dos sondas de navegador que leían la URL antes de que el router la actualizara y que probaban
  el `sticky` con el relleno como hermano en vez de dentro; una banda de oclusión de altura cero que contaba
  como intrusa cualquier fila que cruzase la línea; y la búsqueda de «la primera fila enteramente por debajo»,
  que medía el resto de la fila tapada y no un hueco. Ninguna era defecto del código. Se resolvieron con
  oclusión real por `elementFromPoint`. **No se añade nada a `metodo.md`**: la regla ya está escrita y esta
  sesión es su evidencia de uso, no una regla nueva.
  HALLAZGO DE GUION, para todo cierre futuro: **desde que S122 introdujo el índice de M-doc-3, ninguna ancla
  sobre una cabecera es única en los documentos grandes** —el índice la duplica—. Un `replace(..., 1)` habría
  degradado la entrada del índice y dejado viva la cabecera real: dos H3 vivas y un índice mentiroso, sin
  error visible. Lo detectó la guarda de conteo del guion de cierre en su ensayo en seco. Regla que hereda el
  guion de la próxima sesión: acotar toda búsqueda de cuerpo a partir de `<!-- INDICE:FIN -->`.
  PRUEBA DE MUTACIÓN, como parte del contrato del test. El caso (3) del `configuracion.spec.ts` nuevo —el que
  afirma que el índice se deriva de las rutas— se validó rompiendo a propósito la derivación: caen (3) y (4)
  y **siguen verdes (1), (2), (5) y (6)**, que son los que un array copiado a mano engañaría. Mutación
  deshecha con md5 idéntico. Un test que no se pone rojo cuando rompes lo que dice proteger es decoración; el
  criterio 2 queda vigilado por uno que sí.
  E2E: siete navegaciones intercaladas **por clic en el índice y no por `goto`**, porque con `goto` el e2e
  nunca tocaría el índice y su rotura no pondría nada rojo. Helpers `abrir`/`guardar`, localizadores de
  formulario y diálogos intactos. Aparece una colisión que no estaba prevista y sí importa: **«Grupos» casa
  dos entradas del índice sin `exact`, por estar contenido en «Subgrupos»** —aviso directo para el filtro por
  subcadena de C-listas-filtradas—. El enlace «Horario» no colisionaba, medido y no supuesto: 11 enlaces en
  la página y una sola coincidencia. Bases verificadas antes y después: `educhronos-demo-m4.db` intacta en
  `ea1a70a0…f48e9b`, `educhronos-e2e.db` reescrita como debía.
  ERRORES DEL ARQUITECTO EN ESTA SESIÓN, registrados porque son los mismos que la sesión caza: afirmé que no
  había precedente de `provideRouter` en ningún spec y sí lo hay (`app.spec.ts:11`, `horario-view.spec.ts:38`)
  —extendí un dato medido sobre `children` a algo que no se había medido—; estimé el alto de fila a 585 px en
  ~73 px cuando el instrumento da 87,73, acertando el recuento por el camino equivocado; y escribí de memoria
  tres anclas del guion de cierre que el fichero desmintió en el ensayo en seco.
  LIMPIEZA (M1-bis): archivada S121 a `bitacora-sesiones.md` (promovida a `### Sesión 121`, insertada al final
  en orden ascendente, cuerpo íntegro verificado por comparación); degradada S122 a «Última sesión registrada
  (previa)»; S123 queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S120 a S121.
  O-navegación (transversal, abierto en S122) ACTIVO: **C-rutas-hijas HECHO**, C-listas-filtradas y
  C-rejilla-densidad PENDIENTES. O-diseño (transversal, abierto en S121): C-tokens y C-sustitución HECHOS,
  C-identidad y C-revisión PENDIENTES, DESPUÉS de O-navegación. O-demo (H2) sigue ABIERTO y sin trabajo
  ejecutable, bloqueado por el correo al centro. Siguiente: **C-listas-filtradas**, el criterio 3, con el
  hueco del filtro ya montado en las siete cabeceras y la frontera con este Cambio ya cortada. Lo fija su
  propio M0 (ver M1-ter).

### Sesión 124 — O-navegación (transversal): C-listas-filtradas HECHO, criterio 3 CUMPLIDO. Las siete listas de Configuración estrenan búsqueda normalizada con contador «n de N» y estado propio de «sin resultados», sobre una cabecera compartida que sustituye a las siete duplicadas de S123 (M0 + M2 + M3 con mutación + M4 en navegador + M1). DOS de tres Cambios. NO cierra el objetivo.
  Vigesimocuarta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI **con M3**, y ahí está la
  diferencia con S123: allí era enrutado y binding y M3 no tocaba; aquí hay un predicado de casado, que es
  lógica real, y `metodo.md` lo dice con esas palabras. Suites: **app 282, solver 91, vitest 356, e2e 2**.
  vitest sube de 314 a 356 (+42) sin que se modifique NI UN aserto preexistente; app y solver intactos, y el
  e2e se corre —única suite sin correr en toda la sesión— y pasa sin tocarse.
  M0 — LA APERTURA RATIFICA. El Cambio salía del criterio 3 y la frontera se había cortado en S123. El
  tercer término del mapa se responde con lo que la ficha ya dice: **no hay hito**; O-navegación es
  transversal, acerca la demo y va antes de C-identidad y C-revisión. La apertura resolvió las cuatro
  preguntas que dejó S123 y CORRIGIÓ una premisa del arquitecto: la colisión «Grupos»/«Subgrupos» medida en
  S123 es un defecto de LOCALIZADOR, no de filtro. Un localizador que casa dos elementos está roto; un filtro
  que devuelve un superconjunto funciona bien. Lo que sí hereda el Cambio es la trampa en sus propios tests
  —de ahí el placeholder genérico «Buscar…», idéntico en las siete— y un argumento más para no filtrar el
  índice. El problema real de la subcadena con datos reales resultó ser otro: la NORMALIZACIÓN.
  D16, PEDIDO DOS VECES Y MENOS DECISIVO DE LO ESPERADO. El arquitecto insistió en leer la fuente primaria
  antes de diseñar la cabecera. D16 resultó ser una decisión de COLOCACIÓN —la caja va dentro de la cabecera
  fija, junto al contador y al botón de alta— y no de diseño: no dice nada de semántica del contador, casado
  ni normalización. Confirmó el contrato del componente y descartó que el filtro fuera suelto bajo la
  cabecera, que era variante real; pero §5 tampoco lista esas tres cosas entre lo SIN DECIDIR, así que el
  diseño no las resolvió: no las vio. Se registra como error de apertura, no como pérdida: la fuente primaria
  se pide siempre, y lo que se calibró mal fue cuánto iba a decidir.
  MEDICIÓN (M2), y lo que decidió. Las siete cabeceras son molde EXACTO salvo rótulo, prefijo BEM y texto de
  botón —más la concordancia de género de aulas, `__nueva`/`nueva()`—, y los siete CSS son copias byte a
  byte, con el comentario de cuatro líneas de S123 repetido palabra por palabra siete veces. **Cero cobertura
  unitaria de la cabecera entera**: ni `__contador`, ni `__cabecera`, ni `__titulo`, ni `__nuevo` aparecen en
  los siete specs. La extracción no podía romper ningún test, y eso NO era una vía libre sino el riesgo:
  «suites verdes» no probaba nada, así que el componente nació con su spec y con un aserto por lista. Sin
  virtualización ni paginación en ninguna de las siete: las 334 filas de Subgrupos ya se montan enteras,
  luego filtrar en memoria es despreciable y **`debounce` queda descartado por medición**, que era una de las
  tres incógnitas por las que S123 aplazó la extracción. Con datos reales: una consulta de una letra acota
  poco (`A` deja 184 de 334 en Subgrupos), pero NO se pone longitud mínima —no reaccionar a la primera
  pulsación se lee como roto—; el contador es quien informa de que hay que seguir escribiendo.
  DECISIONES DEL CAMBIO. Casado por subcadena, sin `exact` ni límites de palabra: arrastrar el padre con sus
  hijos (`3ºA` trae `3ºA` y `3ºADi-Completo`) es lo correcto, porque el usuario acota, no selecciona.
  Normalización que se come tildes, caja, ordinales y **todo lo que no sea letra o dígito**: la lista a
  medida se rompe sola —comer espacios pero no guiones deja `3ºA Di` sin encontrar `3ºADi-Completo`—.
  Consulta partida por espacios con casado de TODAS las partes, que es lo que salva `mat 1eso` de morir al
  comerse los espacios. El texto de fila se compone de lo que **la plantilla pinta**, no de lo que el modelo
  tiene: `grupos` busca por «Ordinario»/«PDC» y no por `DIVERSIFICACION_PDC`. Limitación aceptada y escrita
  en el javadoc de las siete: nada vigila que la composición siga a la plantilla, y no se encontró forma
  barata de vigilarlo.
  **LA COSTURA: los contadores en el índice se CAEN.** La frontera de S123 se los asignaba «íntegros» a este
  Cambio. La medición dice que `Configuracion` es presentacional pura y sin servicios de dominio, con esa
  invariante declarada en su javadoc y protegida por su spec, y que ocho contadores le obligarían a consultar
  los ocho servicios al entrar —deshaciendo las ocho cargas que C-rutas-hijas acababa de separar—. Leído
  después `diseno-navegacion.md` entero: no los diseña en ningún sitio. No se declaran fuera de alcance ni
  pasan a deuda; **se corrige la frase de §3**, porque no tenía respaldo. Un token nuevo para algo que nadie
  va a construir sólo ensucia el censo de R4.
  VOCABULARIO OCUPADO. «Filtro por tipo» ya significa otra cosa en `grupos/`: es el `@if (esOrdinario(grupo))`
  que decide qué botones se pintan por fila. El ejecutor lo detectó al desmentir una afirmación suya previa.
  El código nuevo no usa un solo token `filtr*`: `busqueda.ts`, `normaliza()`, `coincide()`, señal `busqueda`,
  lista `visibles()`, clase `cabecera-lista__busqueda`. «Filtro» se queda en la planificación y en D16.
  CUATRO PASOS, cada uno verificado antes del siguiente. (1) Extraer `cabecera-lista` sin filtro, con las
  suites verdes y el paseo visual por los siete destinos, que es lo único que ve el CSS. (2) El predicado
  puro con M3 y mutación, sin tocar una línea de UI. (3) Cablearlo en las siete, con el contador de dos
  estados y el estado de «ningún resultado», que es mensaje NUEVO y distinto del de lista vacía. (4)
  Revertir y cerrar. El paso 1 obligó a mover la clase `cabecera-lista-fija` al **host**: un
  `:host { display: block }` bienintencionado dejaba el recorrido del `sticky` en cero (medido: y 84 → −1416),
  porque el host acotaba el contenedor de bloque. Lo sujeta el caso (8), que es lo único que impide devolver
  la clase al `div` sin enterarse.
  EL ENCADENADO DEL ERROR, INTRODUCIDO Y REVERTIDO. El paso 3 encadenó `@if (error())` con el resto de ramas
  y eso arreglaba un defecto real —error y «No hay aulas todavía» conviviendo—, pero el ejecutor MIDIÓ la
  otra cara en vez de suponerla: con un 409 de borrado la tabla entera desaparecía (8 filas → 0). Es un
  camino de uso normal que S113 introdujo a propósito, frente a un fallo de carga que es raro. Se revierte en
  el paso 4 y se comprueba en navegador: con 43 aulas y un 409, el error se pinta, la tabla sigue entera y
  la búsqueda sigue viva bajo el error. El defecto preexistente que el encadenado tapaba queda registrado
  como deuda con sede en C-revisión y su arreglo ya escrito. **R-terminado en su caso incómodo: es una línea
  y aun así no se paga**, misma forma que el arreglo de una línea de D-F8.6-ii-a que S118 midió y no pagó.
  MÉTODO — el hallazgo de la sesión, que va a `metodo.md` condensado y no como precisión nueva (lo prohíbe la
  nota de acumulación de M2). Dos formas de mutación engañosa, las dos medidas aquí: una mutación puede
  compilar y no mutar nada (se apuntó a `\p{Sk}` creyendo que el signo de grado era símbolo modificador,
  cuando es `\p{So}`); y una mutación que SÍ muta y no pone nada rojo puede estar delatando código INERTE en
  vez de un test decoración. Ese fue el caso de `normaliza()`: un `replace` de marcas diacríticas que no
  hacía nada porque el barrido posterior ya se las llevaba. Se BORRÓ el paso en vez de blindarlo con un test,
  y se ancló en su lugar el `normalize('NFD')`, que es donde vive de verdad la mitad que faltaba.
  UBICACIÓN, mejorada por el ejecutor. El arquitecto propuso `components/cabecera-lista/busqueda.ts` como
  respaldo; el ejecutor midió que el repo ya tiene el patrón —`horario/` con funciones puras sueltas y su
  spec al lado— y colocó el módulo en `catalogo/`, hermano suyo y nombrado como el backend ya nombra a estas
  siete entidades (`app.catalog.*`). Evita que siete componentes importen de la carpeta de un octavo.
  DEUDAS. **CERRADA: `D-cabecera-lista-duplicada`**, en su sede escrita, saldada de paso y sin abrir sesión.
  NUEVA: `D-vacio-miente-con-error` (mejora futura, sede C-revisión). `D-guion-exit-enmascarado` recibe su
  TERCERA instancia, esta vez por el reverso: guardas que miden el vacío y no la corrección. Deuda bloqueante
  abierta: sigue en 1 (D31-a, las aulas de FPB).
  ERRORES DEL ARQUITECTO, registrados porque son la clase que esta sesión persigue: supuse marcadores
  `INDICE:INICIO/FIN` en `diseno-navegacion.md` extendiendo un dato medido sobre otros dos ficheros; calibré
  mal el peso de D16 y lo pedí dos veces esperando que decidiera lo que no decide; inventé la ruta
  `components/grupo-lista.html` sin el directorio de entidad; escribí un `grep -v '^\s*//'` que nunca podía
  casar, porque `grep -rn` antepone `fichero:num:` y el ancla `^` no llega; supuse que la sección de deuda
  cerrada era una tabla cuando es prosa; y prometí «cuatro suites verdes» como garantía del paso 1 cuando la
  propia medición decía que ningún test ve la cabecera. Esa última es la peor: era una garantía vacía y la
  corrigió la medición, no yo.
  Y TRES MÁS EN EL PROPIO M1, que son la misma familia y la razón de que esta nota exista. El guion de
  cierre abortó tres veces por anclas mal acotadas: `find('<!-- INDICE:FIN -->')` casaba una mención entre
  backticks en `metodo.md`, que no tiene índice generado; `### Sesión 123 — ` casaba TAMBIÉN su entrada del
  índice, y el `replace(..., 1)` habría insertado las 109 líneas de S124 dentro del índice; y
  `split('### Sesión ')[-1]` tomaba la última MENCIÓN y no la última cabecera (129 frente a 111). Lo grave
  no es el fallo: es que S123 dejó escrita la regla —acotar el cuerpo a partir de `INDICE:FIN`— y la
  mitigación que construí sobre ella estaba rota. **Regla que hereda el guion de la próxima sesión, y que
  esta vez no depende del fichero: anclar a principio de línea con `re.M`, y pasar TODA ancla por la guarda
  de unicidad antes de escribir, incluidas las que van a `replace(..., 1)`.**
  **CUARTA instancia, en el guion que escribía esa misma nota:** un ancla con dos espacios de sangría
  PARECÍA de principio de línea y no lo era —iba en mitad de la línea 788— y no pasó por la guarda de
  unicidad. Cortó antes de escribir, que es lo único que funcionó. Precisión que hereda la regla: la
  sangría no hace que un ancla empiece línea; si no se ha verificado con `re.M`, no lo es.
  ERRORES DEL EJECUTOR, todos autodetectados y reportados: dos asertos del paseo mal calibrados (ancho contra
  el panel en vez de contra `.subgrupos` menos su padding; desplazamiento cero cuando `sticky` debe subir los
  16 px del padding), un diagnóstico falso por bundle rancio de `ng serve` que se corrigió tras reiniciar, y
  una afirmación desmentida por él mismo («grupo-lista ya tiene un filtro por tipo»). Nota operativa que
  merece sobrevivir: una tanda de mutación que hace `rm -rf` del árbol de fuentes se lleva el watcher, así
  que toda comprobación visual posterior exige reiniciar `ng serve` primero.
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b` antes y después;
  `educhronos-e2e.db` reescrita por el e2e, como debía.
  LIMPIEZA (M1-bis): archivada S122 a `bitacora-sesiones.md` (promovida a `### Sesión 122`, insertada al
  final en orden ascendente, cuerpo íntegro verificado por comparación); degradada S123 a «Última sesión
  registrada (previa)»; S124 queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S121
  a S122.
  O-navegación (transversal, abierto en S122) ACTIVO: **C-rutas-hijas y C-listas-filtradas HECHOS**,
  C-rejilla-densidad PENDIENTE. O-diseño (transversal, abierto en S121): C-tokens y C-sustitución HECHOS,
  C-identidad y C-revisión PENDIENTES, DESPUÉS de O-navegación. O-demo (H2) sigue ABIERTO y sin trabajo
  ejecutable, bloqueado por el correo al centro. Siguiente: **C-rejilla-densidad**, el criterio 4, que NO se
  abre sin MEDIR antes el sobremesa con `innerWidth/innerHeight` —pendiente desde S123 y no supuesto—. Lo
  fija su propio M0 (ver M1-ter).

### Sesión 125 — O-navegación (transversal): APERTURA de C-rejilla-densidad y MEDICIÓN del presupuesto de altura. La superficie de verificación del criterio 4 pasa del sobremesa del centro al equipo de desarrollo y se fija en Firefox por ser el peor caso (viewport 1920×887): el recorte es 50 de 791, el 6,3 %, y CUMPLE, mientras Chrome mantendría 22 bajo condición; el «~1920×945» heredado se manejaba con la barra de la aplicación a cero (M0 + M2 con el instrumento parametrizado + M1, sin M3 ni M4). Cero líneas de producto. NO abre el tramo 1.
  Vigesimoquinta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo apertura/medición: sin M3 ni M4 porque no se
  toca un solo fichero de `app/`. **Las suites NO se ejecutan y no se declaran verdes de memoria**: se hereda
  la cifra de S124 —app 282, solver 91, vitest 356, e2e 2— y la PRIMERA acción del tramo 1 es correrlas.
  M0 — EL MAPA, y una premisa del arquitecto que la apertura acepta con dos precisiones. Cambio:
  C-rejilla-densidad, criterio 4, el último con trabajo ejecutable del objetivo (el criterio 1 no abre Cambio:
  la barra existe y lo que le falta es estilo). Hito: NINGUNO funcional, y ésa es la respuesta escrita en la
  ficha, no un hueco. R-invalidación limpio y en la dirección buena: la ficha manda que O-navegación vaya
  ANTES de C-identidad y C-revisión, así que el riesgo es adelantar trabajo de ésos, no que ésos rehagan éste.
  **CAMBIO DE PREMISA ACEPTADO: no se mide el sobremesa del centro.** No hay uno solo, la aplicación correrá
  en varias máquinas y D11 absorbe la variación. Las dos precisiones que la apertura le añade: (1) el criterio
  carga el NÚMERO, no el nombre de la máquina —«se verifica en el equipo de desarrollo» es un panel, mismo
  vicio que «1920×1080»—; y (2) «D11 absorbe» es cierto hasta un umbral ya medido, el 13,8 % de S123, que
  queda escrito como criterio de reapertura en el ~10 %.
  LA MEDICIÓN, en tres intentos y dos navegadores. Las dos primeras lecturas dieron **394** con DevTools
  acoplado; el diagnóstico fue aritmético y no de inspección: `outerHeight − innerHeight` = 642 px, contra los
  ~90 que cuesta el cromo de un navegador. La anchura salía intacta (1920) porque el panel estaba abajo, que
  es justo lo que enmascara el problema. Se resolvió con un snippet **diferido 10 s que vuelca el resultado
  sobre la página y en el `document.title`**, medido con DevTools CERRADO, y con un campo `limpio` que hace
  saltar el propio caso que se estaba sufriendo. Resultados: **Firefox 1920×887, cromo 149, contenido 835;
  Chrome 1920×946, cromo 90, contenido 894**; los dos con `devicePixelRatio` 1, `availHeight` 1036 (1080
  físicos menos ~44 de barra de tareas) y ventana maximizada, no F11.
  **DATO DURO NUEVO: la barra de la aplicación mide 52 px**, idéntica en los dos navegadores (946−894 y
  887−835). Es la magnitud que faltaba y la que desmonta el uso del presupuesto heredado.
  HALLAZGO 1 — **el «~1920×945» del criterio 4 se manejaba con el cromo a CERO.** Reconstruida la pasada de
  S123 desde el 87,73 registrado: `(585 − 31,0 − 23,6 − 4)/6` sale exacto sólo con cromo 0. Es decir, el
  control de 945 supone barra de aplicación y cabecera de vista de altura NULA, y la barra mide 52 px
  medidos. El número no era falso —Chrome da 946 de viewport, clavado— pero su uso sí lo era: se comparaba
  un VIEWPORT contra un presupuesto que exige CONTENIDO NETO. Misma familia que el error que S123 descubrió,
  un escalón más abajo.
  INFERENCIA FALSA DEL ARQUITECTO, registrada porque se marcó como hipótesis y salió falsa. Deduje que «945
  no era un viewport sino `util`», y que por tanto ninguna máquina real con Windows y navegador podría
  alcanzarlo. **Chrome lo desmiente: 946 de viewport CSS, clavado.** 945 SÍ era un viewport; lo que estaba
  mal era el cromo a cero, no la magnitud. La inferencia iba etiquetada como no verificada y la dirimió la
  medición, que es como debía ser.
  HALLAZGO 2 — **el escalón, y dónde cae en cada navegador.** Censo del centro por plazas de la instancia:
  1→599 (62,9 px), 2→83 (67,4), 3→37 (89,1), 4→22 (110,8), 5→28 (132,5), 6→22 (154,2). Las de cinco exigen
  **853,6 px de alto neto**. En el eje común —el viewport— el presupuesto disponible para barra MÁS cabecera
  de vista es `887 − 853,6 = 33,4` px en Firefox y `946 − 853,6 = 92,4` en Chrome. Con la barra medida en 52:
  **Firefox se queda 18,6 px corto aun con cabecera de altura cero y cae a 50 de 791, el 6,3 %; Chrome se
  queda 40,4 px por encima y mantiene las 22, el 2,8 %, mientras la cabecera de D9 quepa en esos 40,4 px.**
  Los dos navegadores NO dan el mismo veredicto, y su diferencia es de **59 px** (946−887 y 894−835), no de
  52 —52 es la barra de la aplicación, que es otra magnitud—. Lo que sí es común: la frontera no se cruza
  afinando el número de entrada, se cruza con presupuesto de celda.
  DECISIÓN — **Firefox es la superficie de verificación, por ser el peor caso medido, y sobre ella el 6,3 %
  CUMPLE el criterio 4.** El «22 de 791 / 2,8 %» era descripción de lo medido en S122, no un tope; lo que el
  criterio exige es que quepa sin scroll con la expansión activa, y una celda de cada dieciséis sigue siendo
  densidad. El umbral contrario está medido en el 13,8 % (S123, «al 14 % el colapso deja de ser el caso
  excepcional») y no estamos cerca. El 2,8 % sigue siendo alcanzable en Chrome bajo condición, y NO se
  persigue: verificar en el peor caso disponible es lo que convierte el número en un SUELO y lo que sostiene
  el cambio de premisa de esta sesión —prescindir de medir las máquinas del centro—. Fijarlo en Chrome haría
  el criterio dependiente del navegador y frágil.
  **PRESUPUESTO NUEVO QUE HEREDA EL CAMBIO: la fila única de D9 debe caber en 111 px.** El escalón de las
  celdas de cuatro plazas cae en cromo de vista 111,6; por encima entran 22 celdas más y el recorte salta a
  72 de 791, el 9,1 %. D9 deja de ser una decisión de aseo: es lo que impide ese salto.
  LAS PALANCAS AHORA SÍ LLEGARÍAN, Y NO SE APLICAN. A 835 neto faltan 18,6 px y las dos disponibles suman
  ~40 (padding y margen de plaza ~25,2; `.asig` a `--tam-xs` 15; la del badge la anuló D4 en S123) — a
  diferencia del portátil, donde faltaban 10,27 px incluso con las tres. No se aplican: retiran señales
  existentes, que es lo que la invariante del encargo prohíbe y por lo que D6 rechazó borrar la cuarta línea;
  y gastarlas para bajar de 6,3 % a 2,8 % persigue un número que el criterio no pide (R-terminado). Quedan
  como holgura medida por si el tramo 1 la necesita.
  EL INSTRUMENTO, validado antes de creerle y parametrizado. `scripts/calcular-recortes.py` **no tenía
  interfaz**: la altura estaba empotrada como literal 1080 (`:187`, `:194`), `TRAMOS = 6` en `:44` y la lista
  de cromos en `:186`; `--help` no era bandera, se interpretaba como ruta y el `FileNotFoundError` imprimía
  el uso por accidente. Parametrizado con `--alto`, `--cromos` y `--tramos`, **con los valores actuales como
  defectos y verificado por diff: ninguna invocación previa cambia de salida**. Su autoprueba de cinco
  mutaciones se lanza sola en `main()` y aborta; no se puede saltar. **DOS CONTROLES HISTÓRICOS
  INDEPENDIENTES antes de la pasada real**: a `--alto 945 --cromos 0` da 147,73 y **22 de 791, 6p:22/22**
  (reproduce S122); a `--alto 835 --cromos 250` —585 neto— da 87,73 y **109 de 791, 3p:37 4p:22 5p:28
  6p:22** (reproduce S123 y su frase «toda celda de tres plazas o más»). El instrumento y el volcado quedan
  validados contra dos pasadas históricas antes de que se les crea una sola cifra nueva.
  VOLCADO. `/tmp/datos-maqueta.json` **no existía**: artefacto desechable de S122, declarado fuera del repo
  en `diseno-navegacion.md:540`. Regenerado como `/tmp/volcado-s125.json` en SOLO LECTURA
  (`file:...?mode=ro`): **28 grupos y 791 celdas**, por `sesion → plaza → plaza_subgrupo → subgrupo_grupo →
  grupo_administrativo`, con `tramo_semanal` mapeado a día 1-5 y tramo lectivo 1-6 —los cinco recreos
  (`es_lectivo=0`, posición 4 de cada día) quedan fuera, de ahí `TRAMOS=6` más una fila de recreo— y todas
  las actividades de un solo tramo, así que no hay sesiones a caballo de dos celdas.
  D11 CONFIRMADO POR MEDICIÓN, y por más margen del que suponía. Su texto estima la barra en ~56 px y el
  cromo del navegador en ~100 para concluir que el cromo máximo admisible de 96,2 px «es inalcanzable».
  Medido: barra 52, cromo 90 en Chrome y 149 en Firefox. La conclusión se sostiene con holgura.
  ALCANCE FIJADO — dos tramos, por dependencia real. **Tramo 1, geometría y presupuesto** (D1-D6, D7, D8,
  D9, D10): todo lo que consume altura, verificado en navegador contra 50 de 791 y no en hoja de cálculo;
  hay lógica que mutar (la marca `+N`, la clasificación de instancia a modo bloque), así que M3 aplica, y
  aquí cae `centro-minimo.spec.ts:213` por D6. **Tramo 2, el mecanismo de expansión** (D11). FUERA por
  R-terminado: las palancas de altura, la leyenda de la insignia (C-identidad), tokenizar el espaciado (H-2,
  sin sede) y la hora de reloj del recreo (D8 declara el hueco y no lo rellena).
  POR QUÉ NO SE EMPIEZA EL TRAMO 1 AQUÍ. Precedente de S121, que decidió no abrir C-identidad con el
  presupuesto de contexto a la mitad: toca siete ficheros, no es trabajo mecánico y dejarlo a medias obliga
  a reconstruir contexto. Decisión del arquitecto sobre recomendación del asistente.
  COSTURAS CERRADAS. §5 de `diseno-navegacion.md` listaba como SIN DECIDIR dos cosas ya decididas —la
  resolución del portátil (D0-2, cerrado en S123) y qué señal se sacrifica de las palancas (§4 de
  `gestion_proyecto.md` decidió «ninguna»)—; se corrigen las dos filas. El criterio 6 de la ficha llevaba
  «vitest 314» de S123 y son 356 desde S124; corregido.
  EL ERROR GRAVE DEL ARQUITECTO, y dónde apareció: **mezclé ejes**, en la simulación del propio M1 que
  registra esta familia de errores, y lo detectó el ejecutor. Escribí que los dos navegadores daban el mismo
  veredicto «salvo que el cromo de la aplicación sea ≤33,4 px (Firefox) o ≤40,4 (Chrome)», pero el 33,4 está
  calculado sobre el VIEWPORT de Firefox —barra más cabecera— y el 40,4 sobre el CONTENIDO de Chrome, que ya
  lleva la barra descontada. No son comparables; en el eje común son 33,4 y 92,4, y la conclusión se
  invierte: Chrome da 22, no 50. De ahí salió además reutilizar los 52 px de la barra como diferencia entre
  navegadores, que son 59. Es exactamente la forma que la precisión de M2 persigue —un número que se parece
  a la magnitud y no lo es— cometida al redactar el registro de esa misma forma. La decisión de la sesión no
  cae: se REFUERZA, porque si los navegadores dieran lo mismo, elegir el peor caso no compraría nada.
  OTROS ERRORES DEL ARQUITECTO. La inferencia falsa de «945 es `util`», ya descrita —el acierto fue
  etiquetarla como hipótesis, no haberla hecho—. Los cuatro textos normativos del guion de cierre se
  escribieron SIN TILDES para documentos acentuados, y los habría degradado visiblemente. El `re.sub` de
  líneas en blanco era global y tocaba dos sitios ajenos al punto de corte. Y el guion no llamaba a
  `regenerar-indice.py`, con lo que la sesión habría cerrado con el índice del plan descuadrado y el
  verificador diciendo OK, que es literalmente la deuda que esta misma entrada registra. Las cuatro las
  detectó el ejecutor en simulación, antes de escribir.
  NOTA OPERATIVA que merece sobrevivir: la protección antiautopega de DevTools en Chrome exige teclear
  «permitir pegado» antes de aceptar un snippet; se lee como fallo del código y no lo es.
  DEUDA. **Ninguna nueva.** `D-guion-exit-enmascarado` sigue viva y el §4 de `verificar-cierre.py` sigue
  imprimiendo las descuadradas del índice sin sumarlas al EXIT: se lee su salida, no su código de salida.
  Deuda bloqueante abierta: sigue en 1 (D31-a, las aulas de FPB).
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b` antes y después.
  LIMPIEZA (M1-bis): archivada S123 a `bitacora-sesiones.md` (promovida a `### Sesión 123`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S124 a «Última sesión registrada (previa)»; S125
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S122 a S123.
  O-navegación (transversal, abierto en S122) ACTIVO: **C-rutas-hijas y C-listas-filtradas HECHOS**,
  C-rejilla-densidad PENDIENTE y partido en dos tramos, con su criterio 4 ya corregido y su presupuesto
  medido. O-diseño (transversal, abierto en S121): C-tokens y C-sustitución HECHOS, C-identidad y C-revisión
  PENDIENTES, DESPUÉS de O-navegación. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado por el
  correo al centro. Siguiente: **el TRAMO 1 de C-rejilla-densidad**. Lo fija su propio M0 (ver M1-ter).

### Sesión 126 — O-navegación (transversal): C-rejilla-densidad, TRAMO 1 HECHO. La celda pasa a dos líneas con modo bloque y marca `+N`, el título se funde con los controles, la fila de recreo se deriva de la jornada y la altura se reparte MIDIENDO el hueco en runtime: el scroll vertical desaparece en los tres grupos verificados y el recorte medido es 72 de 791, el 9,1 %, bajo el umbral de reapertura del ~10 % (M0 + M2 + M3 con mutación + M4 en navegador + M1). Seis commits. El criterio 4 queda A MEDIAS POR DISEÑO: la expansión es el tramo 2. NO cierra el objetivo.
  Vigesimosexta sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Desarrollo, con M3 y M4 completos. Suites:
  línea base heredada de S124 **corrida antes de tocar nada y verde** (app 282, solver 91, vitest 356, y el
  e2e 2, que era la única sin línea base propia y se corrió también); al cerrar, **app 282, solver 91,
  vitest 381, e2e 2**. vitest sube +25 y ni uno de los 356 heredados se modifica.
  M0 — EL MAPA, Y UNA COSTURA DEL SISTEMA DE TRES DOCUMENTOS. Cambio: C-rejilla-densidad, tramo 1. Objetivo:
  O-navegación, criterio 4. Hito: ninguno funcional, que es la respuesta escrita en la ficha. R-invalidación
  limpio y en la dirección buena. **La apertura se bloqueó**: `docs/diseno-navegacion.md`, que es la AUTORIDAD
  de D1–D11, no estaba en el Project. Se pidió antes de afirmar nada en vez de reconstruir los diseños de
  memoria. Queda anotado: el sistema de tres documentos remite a un cuarto que no viajaba con ellos.
  LA ARITMÉTICA DEL PRESUPUESTO, RECONSTRUIDA Y CUADRADA. `alto_de_fila = (neto − thead 31,0 − recreo 23,6 −
  4) / 6`. El «cromo de vista» de esa fórmula es la fila de D9 con su padding y **NO incluye la barra de la
  aplicación**, que ya está descontada en los 835. De ahí sale el techo de 111 px del criterio 4: es el último
  entero por encima del umbral, y el acantilado está entre 110,00 y 110,83 px de fila.
  **D3 LLEVABA DOS CAMBIOS PRESENTADOS COMO UNO.** El texto dice «de cuatro líneas a dos»; su geometría medida
  incluye además `--lh-apretado` y `--tam-xs` en la línea 2, que el texto no menciona. Sin ese apretón la celda
  de cuatro plazas nace en 124,8 px en vez de 110,8 y el acantilado se cruza sin que nadie lo note. Quien lea
  D3 sin correr el instrumento implementa la mitad y no se entera.
  DECISIONES DEL TRAMO. **D6 sólo condensa donde hay grupo implícito**: la rejilla recibe `grupoActual`
  (defecto `null`) y en las vistas de profesor y aula pinta la lista entera, porque ahí el grupo es la única
  señal que lo nombra y condensarlo sería retirar información existente —A5 ya había medido que la densidad
  sólo se juega en la vista por grupo—. **`.bloque` es clase aparte de `.con-badge`** aunque escriban el mismo
  `padding-top`: hay un test que afirma que una instancia pinada sin badge no lleva esa clase, y reutilizarla
  habría dejado su comentario mintiendo. **`.entrada--fila` no cambia el DOM**: gira el eje, y las dos líneas
  pasan a ir una al lado de otra, así que el modo bloque no duplica plantilla. **El título sale de
  `fechaGeneracion`**, no de parsear el nombre. **El recreo no lleva hora** (D8).
  **D9 ADMITÍA DOS DIRECCIONES Y UNA ROMPE EL PRODUCTO.** El documento no dice cuál. Medidas las dos por
  mutación: bajar los controles a la rama `@else if (proyeccion())` tira el caso (40) y deja al usuario sin
  botón «Generar» en el arranque real —base sin horario, proyección en 404—, que es justo donde hace falta. Se
  implementa subiendo el `<h2>`. El caso (47), nuevo, sujeta esa dirección para siempre.
  **D1 SE MIDE EN RUNTIME, Y EL ARQUITECTO SE EQUIVOCÓ DOS VECES ANTES DE ACERTAR.** Primera propuesta: un
  `calc(100dvh − constantes)`. RETIRADA al saber que entre la cabecera y la tabla cuelgan seis bloques
  condicionales: con constantes, la fórmula es correcta sólo en el caso feliz y silenciosamente falsa en
  cuanto aparece un aviso. Segunda: que midiera la vista. RETIRADA también: la rejilla es el ítem flexible que
  absorbe el sobrante, así que su propio hueco ya lleva descontado todo lo de arriba y nadie necesita enumerar
  el cromo. Lo publica el componente sobre su host con `ResizeObserver`, y **no hay bucle** porque el cálculo
  depende del hueco, del `thead` y del recreo, nunca de lo que mide la tabla. Verificado en navegador: cero
  avisos de `ResizeObserver loop` y la cadena de altura copia el patrón de `configuracion.css` (envoltorio con
  `height: 100%`), sin introducir un `:host` que sería un segundo patrón conviviendo con el único que hay.
  **DOS VECES SE DESCRIBIÓ MAL EL FALLO DEL REPARTO ESTÁTICO, Y LAS DOS SE CORRIGIÓ.** «Se recorta una fila
  más» es falso: si el presupuesto se calcula una vez y luego aparece un aviso, `--alto-celda` no cambia, el
  hueco encoge y lo que vuelve es el SCROLL. La segunda vez el error venía con un número al lado (4,9 px de
  margen), que es lo que lo hacía convincente: esos 4,9 px son la distancia al siguiente umbral de recorte, no
  margen contra el scroll.
  M4 — LO QUE LA MEDICIÓN DESMINTIÓ. `thead` 30 y `tr.recreo` 24, exactos, desviación 0. **El hueco es 716, no
  los 748 que el arquitecto había calculado**, y esos 32 px SIGUEN SIN EXPLICACIÓN. El reparto cae a 110 px de
  fila contra los 110,8 que pide una celda de cuatro plazas: **el acantilado se cruza por 0,8 px** y el recorte
  es 72 de 791 (9,1 %) en vez de 50 (6,3 %). El criterio 4 NO se rediscute: su exigencia es ausencia de scroll
  —verificada en 1ºA, 4ºA (que desbordaba 71 px) y 1B-A— y su umbral de reapertura es el ~10 %. El 50 era una
  predicción, no un requisito. Perseguir los 32 px es higiene y no rescate: el aviso de pines cuesta 62 px de
  hueco (~10 px por fila), así que con un aviso en pantalla 72 es inevitable a esta geometría.
  **EL INSTRUMENTO SALE VALIDADO Y NO SE TOCA.** Recalibrado contra el navegador en cinco tamaños de celda
  (63,0 / 67,0 / 111,0 / 132–133 / 154–155 frente a 62,9 / 67,4 / 110,8 / 132,5 / 154,2): **ninguna desviación
  supera 1 px**, sobre cientos de muestras. Sesgo conocido y conservador: cobra la banda del badge a todas las
  instancias, y 303 de las 599 celdas de una plaza no la llevan. `scripts/volcar-sesiones-por-grupo.py` se
  COMMITEA: el volcado se había perdido dos veces (S125 y S126) y un instrumento sin su entrada no es
  reproducible, es un ritual.
  MÉTODO — EL HALLAZGO DE LA SESIÓN. **Una mutación tiene que AISLAR el aserto que se quiere medir, no sólo
  poner el test en rojo.** Al verificar el test de reparto, la mutación canónica lo tumbaba por el `toBe`, que
  se evalúa primero y aborta antes de llegar a la cota nueva: «el test cae» no demostraba que la cota
  discriminara. Hicieron falta dos mutaciones más, ajustando el `toBe` al valor mutado, para exponer cada cota.
  ERRORES DEL ARQUITECTO, ADEMÁS DE LOS DOS DE D1. Un ancla inventada (`tituloDeProyeccion`) sobre un fichero
  que no había leído —la escribió un encargo abierto, no un guion, y sólo la guarda de unicidad evitó el
  destrozo—. Un test cuyo comentario prometía cazar una mutación que no cazaba (el (2) del recreo: mover el
  recreo desplaza también su `orden`, y lo que rompe la confusión es que el día no empiece en 1). Y el aserto
  (1) de `reparto.spec.ts`, que documentaba como MEDIDAS unas cifras que el navegador desmintió; corregido en
  commit propio, y ahora documenta el acantilado cruzado en vez de negarlo.
  R-TERMINADO, aplicado tres veces. No se abren las palancas de altura (ratificadas dos veces ya). No se paga
  la hora de reloj del recreo pese a que la medición de la API la puso al alcance. Y no se manda un nombre
  legible en el POST del horario, que arreglaría el título en origen, porque es una escritura nueva y el
  criterio 5 las excluye.
  DEUDA. **DOS NUEVAS**, las dos con sede en O-demo: `D-nombre-horario-instante` (mejora futura) y
  `D-jornada-zona-servidor` (limitación conocida). `D-guion-exit-enmascarado` y `D-vacio-miente-con-error`
  siguen vivas y sin pagarse. Deuda bloqueante abierta: sigue en 1 (D31-a, las aulas de FPB).
  **CONDICIÓN DE SALIDA, EXPLÍCITA: el repositorio queda ocultando 72 celdas de 791 sin marca y sin forma de
  verlas.** Es el estado intermedio que S125 aceptó al partir el Cambio, no una entrega, y **sólo lo cierra el
  tramo 2 (D11)**. Va en la primera línea del prompt siguiente.
  NOTAS TÉCNICAS QUE SOBREVIVEN. `mvn test` sin `clean` no borra informes huérfanos de surefire:
  `TEST-…BarridoPresupuestoS117.xml` sigue en `app/target` y hace que sumar los XML dé 283 en vez de 282.
  `npx vitest run <fichero>` NO funciona en este repo (`describe is not defined`): la suite corre bajo el
  builder `@angular/build:unit-test` y hay que lanzar `npx ng test --watch=false` entera (~11 s) y filtrar por
  grep. Sin cubrir: el arrastre sobre celda VACÍA (4ºA tiene el horario lleno) y los 32 px de hueco. La
  verificación de M4 se hizo con el Firefox de Playwright, no con el del arquitecto; el recalibrado coincide,
  pero el hueco conviene confirmarlo una vez en el navegador propio.
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b` antes y después, incluido el e2e
  y el M4 (que corrió contra una copia, porque el arrastre escribe un bloqueo).
  LIMPIEZA (M1-bis): archivada S124 a `bitacora-sesiones.md` (promovida a `### Sesión 124`, insertada al final
  en orden ascendente, cuerpo íntegro); degradada S125 a «Última sesión registrada (previa):»; S126 queda como
  única cabecera H3 viva. Los dos censos de la bitácora pasan de S123 a S124.
  O-navegación (transversal, abierto en S122) ACTIVO: **C-rutas-hijas y C-listas-filtradas HECHOS, y
  C-rejilla-densidad con su TRAMO 1 HECHO**; queda el tramo 2. O-diseño (transversal, abierto en S121):
  C-tokens y C-sustitución HECHOS, C-identidad y C-revisión PENDIENTES, DESPUÉS de O-navegación. O-demo (H2)
  sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro. Siguiente: **el TRAMO 2 de
  C-rejilla-densidad**, que es lo único que cierra la condición de salida. Lo fija su propio M0 (ver M1-ter).

### Sesión 127 — O-navegación (transversal): C-rejilla-densidad, TRAMO 2 (D11) HECHO, criterio 4 CUMPLIDO y **OBJETIVO TERMINADO**. Las 72 celdas que el tramo 1 recortaba en silencio dejan de ser mudas: marca `+N` en la banda del rótulo con el detalle en el `title`, y la regla es una FRACCIÓN —una plaza cuenta como oculta si se ve menos de la mitad—, así que se marcan 50 y las 22 que sólo se pasan 1,23 px no mienten (M0 + M2 + M3 con mutación + M4 en navegador + M1). Tres commits. Cierra el objetivo y descarga la condición de salida de S126.
  Vigesimoséptima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Desarrollo, con M3 y M4 completos. Suites:
  línea base heredada de S126 **corrida antes de tocar nada y verde** (app 282, solver 91, vitest 381, e2e 2);
  al cerrar, **app 282, solver 91, vitest 403, e2e 2**. vitest sube +22 y **ni uno de los 381 heredados se
  modifica**: el tramo 2 sólo añade.
  M0 — EL MAPA. Cambio: C-rejilla-densidad, tramo 2. Objetivo: O-navegación, criterio 4 en su mitad
  pendiente, y de paso el criterio 6. Hito: ninguno funcional, que es la respuesta escrita en la ficha.
  R-invalidación limpio y en la dirección buena: C-identidad y C-revisión van DESPUÉS, así que el riesgo era
  adelantar su trabajo puliendo el aspecto de la marca, no que ellos rehagan éste.
  **LA COSTURA QUE EL M0 DESTAPÓ: D11 exigía el mecanismo y NO DECÍA CUÁL ERA.** Leídos §4 entero y §5 de
  `diseno-navegacion.md`: D11 decide que el mecanismo es obligatorio y descarta «recortar sin expansión», pero
  no elige entre expansión en sitio, popover, fila que crece o gesto alguno; y §5 no lo listaba como
  pendiente. La frase de la ficha «ya sin parámetros abiertos tras cerrarse D0-2» habla de la GEOMETRÍA, no
  del mecanismo. La única decisión de bulto del tramo 2 no tenía sede escrita; se cierra escribiéndola.
  **SEGUNDA COSTURA: D11 está redactado contra 22 celdas y la realidad son 72.** Su nota de proporción dice
  explícitamente que la proporción «cambia qué clase de mecanismo hace falta», y la corrección 1 de S126 la
  había movido al 9,1 % sin volver sobre el texto de D11. Quien lo lea sin la corrección diseña para el caso
  equivocado.
  LA DECISIÓN — MARCA `+N` CON EL DETALLE EN EL `title`, y no se estrena patrón. Medido en el repo antes de
  diseñar nada: cero `<details>`, cero popover propio, cero tooltip propio; lo único que existe es `title`
  nativo, y D4 y D6 ya lo usan **para exactamente esto** —condensar y dar forma de recuperar—. Descartada la
  expansión en sitio porque en una tabla crecer una celda estira su fila y devuelve el scroll que el tramo 1
  acaba de quitar; descartado el overlay del CDK por estrenar patrón para el caso que el propio diseño llama
  excepcional; descartado `MatDialog` por modal.
  **LA MARCA VA DENTRO DE LA BANDA DEL RÓTULO, Y NO ES ESTÉTICA: ES LO QUE IMPIDE EL BUCLE.** `.instancia.bloque`
  reserva `padding-top: 16px` y `.rotulo` es `position: absolute` dentro, así que la marca no ocupa un píxel de
  alto. Importa porque la medición que la produce depende del alto disponible: una marca que ocupara alto
  oscilaría —aparece, empuja, la última plaza deja de estar oculta, desaparece, vuelve a estarlo— y **la guarda
  `altoPublicado` NO la detendría**, porque sólo impide reescribir el MISMO valor y esto son valores distintos
  alternándose. La marca lleva `title` propio: el de `.rotulo` es el único sitio donde se lee el código completo
  de la actividad y ocuparlo habría retirado una señal existente.
  **LA REGLA ES UNA FRACCIÓN, NO UN UMBRAL EN PÍXELES, Y ESO ES LO QUE DECIDE 50 EN VEZ DE 72.** Con la fila en
  109,57 px el déficit no es homogéneo: la de cuatro plazas pide 110,8 y se pasa **1,23 px** (se ve el 94 % de su
  última línea); la de cinco esconde una línea entera y la de seis, dos. Marcar las 72 habría puesto una señal
  que miente en 22 celdas, al lado de la marca `+N` verdadera de D6: familia de `D-vacio-miente-con-error`. La
  fracción y no la constante, por la misma razón por la que D1 deriva la altura en vez de escribirla. Que el 50
  coincida con la predicción de S125 es casualidad aritmética: aquel 50 salía de otro reparto.
  **EL DISPARADOR: `afterRenderEffect` con fase `read`, y NO el `ResizeObserver` que ya existía.** Ese observador
  sólo despierta con cambios de TAMAÑO, y con `table-layout: fixed` y seis filas cambiar de grupo repinta la
  rejilla sin mover un píxel: las marcas del grupo anterior habrían sobrevivido al cambio. Estrena patrón —no
  había ningún `afterRenderEffect` ni `effect()` en producción— y se asume, porque reutilizar el observador
  habría sido reutilizar el disparador equivocado. Verificado ANTES de escribirlo, con una sonda desechable, que
  la fase corre en jsdom y se re-dispara al cambiar una señal leída dentro (1 → 2). Efecto lateral que obligó a
  una línea más: `repartirAltura` publica `--alto-celda` con `setProperty` desde FUERA del ciclo de render, así
  que sin un espejo en señal del tope el primer pintado mide la celda antes de que tenga tope y no marca nada.
  El espejo es disparador, no fuente de verdad.
  **LA PARTICIÓN DE S125 SALE VALIDADA.** La dependencia era real y se notó en cada medida: el umbral de la
  marca se decidió contra la geometría que el tramo 1 había dejado, no contra la prevista.
  M3 — TRES CAPAS Y CADA UNA CON SU RED. `horario/oculto.ts`, hermano de `reparto.ts`, `recreo.ts` y `titulo.ts`:
  `plazasOcultas` (la regla) y `ocultasEnCelda` (el adaptador DOM→regla). Diez casos para la primera y ocho para
  el segundo, más cuatro en el spec del componente. **El barrido destapó tres tests que no discriminaban nada y
  se corrigieron los tres antes de commitear**: (5) y (6) construían su entrada como `20 * FRACCION_VISIBLE_MINIMA`,
  o sea derivada de la constante que pretendían proteger —con el umbral a 1 seguían pasando—, y pasan a literales;
  el `Math.max(0, visible)` era INERTE (el umbral es positivo, una fracción negativa y una de 0 dan el mismo
  veredicto) y **se borró en vez de blindarlo**, mismo criterio que S124 aplicó a `normaliza()`; y el nombre de
  (10) prometía comprobar el negativo y afirmaba otra cosa.
  **UNA GUARDA QUE SOBREVIVE A TODA MUTACIÓN Y SE CONSERVA A PROPÓSITO**, con eso escrito en el código: `alto <= 0`
  no la discrimina ninguna entrada, porque sin ella una línea de alto 0 daría `0/0 = NaN` y `NaN < 0.5` es `false`.
  No es redundancia: es que la corrección dependería de cómo compara `NaN`. Se documenta como intención y **no se
  le escribe un test que aparente sujetarla**, que habría sido cobertura fingida.
  EL CASO QUE HACE EL TRABAJO, y es de la lección de S126: el aserto (15) del adaptador, que sitúa la celda a 3000
  px de la página. Las tres mutaciones de relativización son INDISTINGUIBLES del código bueno con la celda en el
  origen; sin ese caso, siete de los ocho tests darían idéntico. Y (18) —el conteo no depende del orden— no es
  tautológico: detiene la optimización razonable de «las plazas vienen en orden, corto en la primera que se sale».
  M4 — LO QUE SE MIDIÓ EN EL FIREFOX DEL ARQUITECTO, no en el de Playwright. Viewport 1920×887, dpr 1, hueco **716**,
  `--alto-celda` 101. Marcas contra el cálculo del volcado y CUADRAN: 1B-A 4, 4ºA 3 —dos `+2` de seis plazas y un
  `+1` de cinco—, 2B-B 0 sobre seis celdas de cuatro plazas. **4ºA es el caso que decide**, porque sus tres marcadas
  y sus tres sin marcar conviven en la misma pantalla. Marcas correctas al cambiar de grupo y volver. **Consola sin
  un solo aviso de `ResizeObserver loop`**, que es la forma exacta en que se habría manifestado la realimentación que
  el diseño descarta por construcción.
  **CONFIRMACIÓN COLATERAL QUE VALE POR SÍ SOLA:** con el aviso «1 pines sin aplicar» en pantalla la celda de seis
  pasa a `+3` y **el scroll no reaparece**. Son los 62 px que S126 midió, absorbidos en caliente: la prueba de que el
  reparto en runtime de D1 compró algo real, porque con la fórmula de constantes que S126 retiró dos veces aquí habría
  vuelto la barra.
  **DOS PENDIENTES DE S126 CERRADOS DE PASO.** (1) El hueco de 716 px queda confirmado en el navegador del arquitecto;
  los 32 px contra los 748 calculados siguen SIN EXPLICAR, pero ya no son sospechosos de ser artefacto del entorno de
  prueba. (2) **El arrastre sobre celda VACÍA funciona**, verificado en 1FPB. No se había probado porque no había dónde:
  medido ahora, las 49 celdas libres de las 840 posibles están TODAS en 1FPB (24) y 2FPB (25), y ninguno de esos dos
  grupos tiene una sola celda de cinco o seis plazas, así que **no existe un grupo donde verificar marcas y hueco vacío
  de una sola pasada**.
  DATO DE ESTRUCTURA, medido y no supuesto: **las 791 celdas del centro tienen exactamente UNA instancia cada una**. Es
  propiedad de estos datos y no del modelo —`agruparPorActividad` devuelve una lista y `slotsOcupados` cuenta instancias
  precisamente porque puede haber varias—, así que la implementación mide por instancia contra su celda, que funciona
  igual si algún día hay dos apiladas. Queda escrito en el código como limitación declarada, no descubierta después.
  R-TERMINADO, aplicado dos veces. No se arregla el `ResizeObserver` de jsdom pese a ser una línea en el setup de tests
  y desbloquear cobertura real: no bloquea el criterio y tocar el setup global dentro de un Cambio sin cerrar añade
  riesgo a cambio de nada que el objetivo pida. Y no se persigue el aspecto de la marca: existe y se ve, y que se vea
  bonita es C-identidad.
  MÉTODO — EL COMMIT ESPERA AL JUEZ. El javadoc del cableado afirmaba «Verificado en navegador (M4)» antes de que M4
  existiera, y **el commit se retuvo hasta que la afirmación fue cierta**, en vez de escribir «se espera» para
  reescribirlo después. Cuando la suite no puede probar algo y el único juez es el navegador, el commit va detrás del
  navegador. El texto final no promete: enumera viewport, hueco, `--alto-celda`, los tres recuentos y la ausencia del
  aviso de bucle.
  MÉTODO — EL LÍMITE DE LOS TESTS, DICHO EN UN ASERTO Y NO EN UN COMENTARIO. Que en jsdom `getBoundingClientRect`
  devuelva ceros explicaba por qué no se podía probar la MEDICIÓN, y se había extendido indebidamente a que tampoco se
  podía probar el BINDING. Dos mutaciones lo desmintieron: borrar `[attr.data-clave]` entero o la marca completa dejaba
  la suite en 399 verdes. El caso (27) afirma ahora esa invariante —sin stub no hay marca— en vez de dejarla en prosa.
  ERRORES DEL ARQUITECTO, la familia de siempre. **Afirmé que la medición «cuelga de la pasada que ya existe» y era
  falso**: `repartirAltura` sólo corre desde el `ResizeObserver`, que no ve cambios de contenido; hizo falta un
  disparador nuevo. **Afirmé que la superficie del tramo 2 no rompía la invariante de no-bucle del javadoc de
  `repartirAltura`, y sí la rompe**: la marca se mide sobre la tabla y se pinta dentro de la tabla, que es observada;
  lo que corta no es la invariante vieja sino que la marca no ocupe alto. Registré la deuda del pin como «avisa sin
  persistir» **cuando la propia medición del arquitecto la había desmentido** —sobrevive al F5—; corregido antes de
  escribirla. Y le puse a la deuda sede O-demo cuando el ajuste manual tiene objetivo propio, O-ajuste-cierre.
  **Y UNA SÉPTIMA INSTANCIA DE LA FAMILIA DE ANCLAS DE S124, cometida por el asistente al escribir el propio M1:** un
  ancla de sustitución copiada con CUATRO espacios de sangría donde el fichero tiene cinco. La guarda de unicidad la
  cazó y el guion murió sin escribir. Precisión que se suma a la regla: **la sangría copiada a ojo no es la sangría del
  fichero; si el ancla la incluye, hay que anclar sin ella.**
  ERRORES DEL EJECUTOR, todos autodetectados y reportados. Un `TS18046` por `inject(ElementRef<HTMLElement>)`, que tipa
  el TOKEN y no el genérico: devuelve `ElementRef<any>` y sobre `any` la inferencia de `Array.from` cae a `unknown`.
  Resuelto anclando el tipo UNA vez. Es exactamente la forma que este proyecto persigue: algo que se parece a un tipo y
  no lo es. Y un heurístico que se auto-bloqueó —buscaba una constante «después de `describe`» y la encontró en el
  comentario que el propio guion acababa de insertar—, **sexta instancia**: un ancla que se cumple sobre texto que el
  guion mismo introdujo.
  DEUDA. **DOS NUEVAS**: `D-resize-observer-jsdom` (técnica real, transversal, → sesión de Higiene/Método) y
  `D-pin-ocupada-no-persiste` (técnica real, → O-ajuste-cierre), esta última registrada SIN diagnóstico porque no lo hay:
  se escriben los dos hechos y se dice que elegir entre las dos explicaciones plausibles sería inventar.
  `D-guion-exit-enmascarado` y `D-vacio-miente-con-error` siguen vivas y sin pagarse. Deuda bloqueante abierta: sigue en
  1 (D31-a, las aulas de FPB).
  NOTAS TÉCNICAS QUE SOBREVIVEN. `npx vitest run <fichero>` sigue sin funcionar (`describe is not defined`): la suite corre
  bajo el builder `@angular/build:unit-test` y hay que lanzar `npx ng test --watch=false` entera y filtrar por grep.
  **Corolario nuevo y con precio: un barrido de mutación hecho con `npx vitest run` no mide nada** —hay que comprobar que
  la base pasa antes de creerse un mutante caído—. `mvn test` sin `clean` deja informes huérfanos de surefire; con `clean`
  el conteo de `app` da 282 y no 283. `@angular/core` instalado 21.2.17, CDK 21.2.14: `afterRenderEffect` es `@publicApi`,
  no developer preview, y sus tipos viven en `node_modules/@angular/core/types/core.d.ts`, no en un `index.d.ts`.
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b` antes y después, incluido el M4, que corrió
  contra una copia porque el arrastre escribe un bloqueo. Copia borrada sin residuos `-wal`, `-shm` ni `-journal`.
  LIMPIEZA (M1-bis): archivada S125 a `bitacora-sesiones.md` (promovida a cabecera de sesión, insertada al final en orden
  ascendente, cuerpo íntegro); degradada S126 a «Última sesión registrada (previa):»; S127 queda como única cabecera H3
  viva. Los dos censos de la bitácora pasan de S124 a S125.
  **O-navegación (transversal, abierto en S122) TERMINADO**: C-rutas-hijas (S123), C-listas-filtradas (S124) y
  C-rejilla-densidad en sus dos tramos (S126 y S127). Sus seis criterios cumplidos, con la salvedad escrita en el criterio
  1: nunca abrió Cambio y su verificación real cae en C-identidad, que reestila esa barra. O-diseño (transversal, abierto
  en S121): C-tokens y C-sustitución HECHOS, **C-identidad y C-revisión PENDIENTES y ya sin nada delante**. O-demo (H2)
  sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro. Siguiente: **C-identidad**, sobre la UI
  definitiva que O-navegación acaba de fijar. Lo fija su propio M0.


### Sesión 128 — O-diseño (transversal): **C-identidad HECHO**. La barra pasa a fondo de acento con el sitio del selector de curso de Fase 10 reservado y medido, la landing estrena el primer `<h1>` del proyecto, la insignia de coste blando deja de ser el único adorno mudo de la celda, y las cuatro ramas de estado que las siete listas repetían carácter a carácter se funden en `app-estado-lista` (M0 + M2 + M3 con mutación + M4 en navegador + M1). Seis commits. Cierra la salvedad del criterio 1 de O-navegación, `D-insignia-sin-leyenda` y `D-vacio-miente-con-error`. Queda C-revisión.
  Vigesimoctava sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI, con M4 obligatorio y M3 sólo
  donde hubo lógica real (la precedencia de ramas del bloque 2; el bloque 1 no tocó ni un `.ts`). Suites:
  línea base heredada de S127 **corrida antes de tocar nada y verde** (app 282, solver 91, vitest 403); al
  cerrar, **app 282, solver 91, vitest 414, e2e 2**. vitest sube +11 (9 del componente nuevo, 2 de los asertos
  de texto) y de los 403 heredados sólo se REESCRIBEN 40 selectores, ninguno de contenido. Bundle de
  producción 540,71 → **542,03 kB crudos / 121,81 transferidos**, con el aviso en 550: margen 8 kB.
  M0 — EL MAPA, Y LA PRIMERA APERTURA EN CINCO SESIONES QUE ELIGE EN VEZ DE CONTINUAR. Cambio: C-identidad.
  Objetivo: O-diseño, criterio 4 (decisiones de identidad) y preparación del 3. Hito: ninguno funcional, que
  es la respuesta escrita en la ficha —O-diseño es el único objetivo puramente de acabado—; lo que acerca es
  la DEMO. R-invalidación limpio: O-particiones tocará frontend, y por eso el criterio se definió como
  SISTEMA; Fase 10 traerá el selector de curso, y reservarle sitio no es refinar lo que se rehará sino lo que
  el criterio 1 pide. Los tres candidatos vivos se encuadraron uno a uno: O-demo bloqueado por un correo
  externo y sin trabajo ejecutable; O-ajuste-cierre habría abierto objetivo nuevo con dos Cambios pendientes
  sin nada delante; la sesión de Higiene/Método vuelve a perder por R-deuda, y van ocho.
  **LA DECISIÓN QUE EL M0 TENÍA QUE TOMAR: la sede de la decisión 3 (densidad/espaciado), sin sede desde
  S122.** Se resuelve escribiéndola en `styles.css` —la escala `--e1..--e6` existía desde C-tokens sin regla
  de uso, y sin regla una escala no gobierna nada— y aplicándola sólo donde C-identidad y C-revisión tocan.
  **NO se abre un Cambio de tokenización de espaciado**, con argumento medido: el color tenía equivalencia
  exacta (`#666` → `var(--color-borde)`, mismo valor) y el espaciado no —tokenizar `padding: 13px` obliga a
  elegir escalón y mueve píxeles—, y hay una zona donde mover píxeles rompe algo medido. De ahí la
  **EXCLUSIÓN EXPRESA de la geometría de `horario-grid.css`**, que es presupuesto medido contra el criterio 4
  de O-navegación y no elección de densidad. Los **cuatro** literales que no caían en un escalón se
  redondearon; su declaración **no llegó a escribirse en S128** y se recupera en S130 desde el diff
  `cd6b43f..94b97df`, verificada línea a línea sobre los blobs: `app.css` `padding: 0.75rem 1.25rem`
  → `var(--e3) var(--e5)` (+4 px, sólo el horizontal), `app.css` `gap: 1.25rem` → `var(--e5)` (+4 px),
  `landing.css` `gap: 0.4rem` → `var(--e2)` (+1,6 px) y `landing.css` `padding: 1.25rem` →
  `var(--e5)` (+4 px). El «tres» que este registro afirmó durante dos sesiones era falso, y lo era
  porque nadie enumeró; la afirmación de S128 de que ninguno tocaba la altura de la rejilla SÍ era
  cierta —el vertical de la barra cae exacto en `--e3`— pero no era comprobable. Nace
  `D-declarado-sin-artefacto`. **Los 24 redondeos del tramo 2 (S130) están medidos y reproducidos por
  dos derivaciones independientes, y quedan PENDIENTES DE ENUMERAR en la entrada de S130 del plan;
  mientras esa lista no exista, no se consideran declarados.**
  **LA RESTRICCIÓN QUE GOBIERNA EL BLOQUE 1, Y NO ESTABA ESCRITA EN NINGUNA PARTE: LA BARRA PUEDE ENCOGER, NO
  CRECER.** El javadoc del marco flex de `styles.css` promete que «si la barra cambia de alto, el reparto se
  rehace solo», y eso describe el LAYOUT y no el CRITERIO: `.app__contenido` lleva `flex: 1`, así que lo que
  la barra engorda sale del hueco de la rejilla, que es el presupuesto del criterio 4. El reparto quedó a 110
  px de fila contra los 110,8 de una celda de cuatro plazas (S126) y esas 22 celdas se pasan hoy 1,23 px sin
  llegar a marcarse (S127): cada ~7 px de barra bajan `alto-celda` ~1 px, y al cruzar 1,23 las 50 marcas
  pasan a 72, que es regresión visible sobre un criterio cerrado. La barra NO declara `height` ni lo hará: su
  alto sale de padding más interlineado de la marca. Medido, **51 px, uno menos que los 52 de partida**, y el
  contenido ganó ese píxel. El aviso queda escrito en `app.css` y en `styles.css`, junto al javadoc que lo
  invitaba a leerse mal.
  **LOS 32 PX DE S126 TIENEN CAUSA CANDIDATA, Y NO SE TOCAN.** `.app__contenido` declara `padding: 1rem 0`, es
  decir 16 + 16 = 32 px exactos, en el contenedor de la vista, que ni S126 ni S127 miraron por estar midiendo
  dentro de `horario-grid`. Coincide al píxel con la diferencia entre los 748 calculados y los 716 medidos. Se
  declara **CANDIDATA y no probada**, y la razón es honesta: **la derivación de los 748 no está escrita en
  ningún documento del repo** —el plan dice «los 748 que el arquitecto había calculado»—, así que la hipótesis
  no se puede confirmar ni refutar contra documentación, y reconstruirla desde el viewport no cuadra. Hallazgo
  de método asociado: un número que gobernó una pregunta abierta durante tres sesiones nunca se escribió con
  su derivación al lado, misma familia que `D-arranque-no-literal`. NO se reclaman por R-terminado: devolverlos
  subiría `alto-celda` ~4,5 px y cambiaría el recorte de un criterio cumplido sin que ningún criterio lo pida.
  **LA SALVEDAD DEL CRITERIO 1 DE O-NAVEGACIÓN QUEDA VERIFICADA, y era trabajo, no nota al pie.** S127 cerró
  ese criterio sobre una decisión escrita, sin Cambio y sin verificación formal. Aquí se comprueban sus dos
  mitades: (1) **entrada activa distinguible** —por CUATRO canales: color pleno, peso, subrayado y
  `aria-current="page"`, que es el que faltaba y el que la hacía indistinguible para un lector de pantalla;
  medido en navegador, `page` en la activa y `null` en la otra—; y (2) **sitio reservado para el selector de
  curso de Fase 10**, que hasta hoy era 1.500 px de barra vacía y ahora es `.app__curso` con `margin-left:
  auto` y un `min-height` igual a la caja de la marca. Lo que se reserva es una ALTURA CONOCIDA, no una
  promesa de que todo quepa: un control de 27 px o menos entra sin mover la barra; uno mayor la engorda y
  obliga a Fase 10 a recontar las marcas.
  BLOQUE 2 — **EL `<ng-content>` DENTRO DE UN `@if` NO ES PEREZOSO, Y ESO CAMBIÓ EL DISEÑO.** La primera forma
  propuesta era que el componente absorbiera también la tabla por proyección. Se midió con una sonda
  desechable en vitest: con la rama apagada, el constructor del contenido proyectado **corre igual**
  (`construido=1`, `dom=""`). El contenido lo crea el PADRE en su sitio de declaración y el `@if` del hijo
  sólo decide si lo inserta. Proyectar la tabla habría evaluado su `@for` mientras carga y tras un error, en
  estados donde hoy ni existe. La alternativa —`ngTemplateOutlet`— se descartó por ser un mecanismo
  estructural sin precedente en el repo, con el mismo criterio con que `styles.css` se niega a introducir un
  `:host` que sería «un segundo patrón conviviendo con el único que hay», y porque su pereza real tampoco
  estaba medida. **La tabla se queda en la lista**, con el complemento exacto de la cadena escrito idéntico en
  las siete y verificable por grep.
  **EL COMPLEMENTO NO LLEVA `!error()`, Y ESA ES LA MITAD DEL TRABAJO.** El instinto —encadenar el error como
  primera rama— es exactamente lo que S124 probó y REVIRTIÓ midiendo que un 409 de borrado hacía desaparecer
  la tabla entera, 8 filas → 0, siendo ese 409 camino de uso normal desde S113. El arreglo escrito de
  `D-vacio-miente-con-error` es más estrecho: sólo el VACÍO lleva `&& !error()`. Y escrito tal cual en un
  `@else if` deja una trampa nueva: el caso cae al hermano siguiente, `coincidencias === 0`, que también es
  cierto, y el vacío deja de mentir para que mienta «Ningún resultado para «»». Por eso la precedencia vive en
  un `computed` que devuelve UNA cadena con un `'ninguno'` explícito, no en cuatro ramas de plantilla —mismo
  argumento que `cabecera-lista` escribe para `textoContador`—.
  M3 — CAMPAÑA DE MUTACIÓN SOBRE LA PRECEDENCIA, cinco mutantes, **los cinco muertos** y cada uno en el caso
  que le tocaba, con la base verde comprobada antes de creerse nada (la lección que S127 pagó con `npx vitest
  run`). La más informativa es la que adelanta `sin-resultados` a `vacio`: tira TRES casos y uno de ellos es
  el que también mata la mutación del error, lo que dice que precedencia del error y orden vacío/sin-resultados
  son las dos mitades del mismo argumento. Se anota el precio del caso (9), que cae con tres mutaciones
  distintas por ser de transición: si algún día falla, su nombre no dirá cuál de las tres se rompió.
  **EL CABLEADO CREA UNA SUPERFICIE DE MUTACIÓN QUE ANTES NO EXISTÍA**, y se cubre. Las siete listas aseveran
  la PRESENCIA del nodo y nunca su contenido: mientras el texto vivía en la plantilla no podía viajar mal, y
  desde que viaja por `input` nadie vigila el trayecto. Se añaden DOS asertos de texto y no siete —cinco
  copias del mismo caso no discriminan nada—: **niveles**, porque rompe el molde con una segunda frase sobre
  la dependencia con Grupos, y **aulas**, por el femenino. Los dos con `toBe` sobre la cadena entera; con
  `toContain` ninguna de las dos mutaciones se habría visto, y se mutaron para comprobarlo.
  M4 — EN NAVEGADOR, y el caso que importa NO lo cubre ninguna suite. (1) Lista normal, tabla y contador. (2)
  Consulta sin coincidencias: «Ningún resultado para «zzz».» y la tabla desaparece. (3) **409 de borrado en
  Asignaturas: el error se pinta Y LA TABLA SIGUE ENTERA** —la reversión de S124, ahora protegida por
  construcción en un sitio en vez de siete veces a mano—. (4) Error de carga (500) con el catálogo lleno: el
  mensaje aparece SIN «No hay asignaturas todavía» al lado, que es `D-vacio-miente-con-error` vista en la
  pantalla donde nació.
  R-TERMINADO, aplicado cuatro veces. No se reclaman los 32 px. No se generaliza el ancho de 60 rem de la
  landing a las demás vistas (eso exige verlas todas: es C-revisión). No se arregla el `/horario/1` a fuego.
  Y no se tokeniza el espaciado de las 27 hojas.
  ERRORES DEL ARQUITECTO, CUATRO, Y TODOS DE LA MISMA FAMILIA. (1) Una ruta inventada —
  `components/horario/horario-grid.html`, que no existe— que sobrevivió tres turnos **porque el guion que
  debía leerla llevaba un `|| find …`: el `sed` falló, el `find` salvó la salida y el volcado se vio bien**.
  La detuvo Claude Code en el `git add`. (2) Un `grep -rl … --include=*-lista.ts` que casa con
  `estado-lista.ts`: el componente se contaba a sí mismo, así que el «1» significaba cero. (3) Los contrastes
  de `--color-sobre-acento` se escribieron en el javadoc **antes de medirlos** (resultaron correctos: 8,67:1 y
  5,76:1, medidos después). (4) Se afirmó que `<ng-content>` dentro de `@if` era perezoso y que por tanto el
  componente TENÍA que absorber la tabla; la sonda lo refutó. Las dos primeras son instancias nuevas de
  `D-guion-exit-enmascarado` —un instrumento que mide otra cosa distinta de la que se cree—; la tercera y la
  cuarta son la misma lección de `D-arranque-no-literal` por el lado del mecanismo: lo que no se mide, no se
  afirma. Corolario operativo escrito: **fallback silencioso en un guion de lectura, nunca**.
  DEUDA. **TRES NUEVAS**: `D-horario-id-a-fuego` (O-demo), `D-contador-se-apaga-con-error` (C-revisión) y
  `D-748-sin-derivacion` (Higiene/Método). **DOS CERRADAS**: `D-insignia-sin-leyenda`, pagada en su sede
  escrita y con un argumento que su ficha no podía tener —desde S127 esa esquina tiene DOS números con signo,
  el `+N` de desbordamiento y el coste blando, con significados sin relación y sólo uno con explicación—; y
  `D-vacio-miente-con-error`, muerta POR CONSTRUCCIÓN al centralizar la precedencia, igual que
  `D-pdc-lista-rancia` murió en S123 sin que nadie escribiera un `EventEmitter`. Deuda bloqueante abierta:
  sigue en 1 (D31-a, las aulas de FPB).
  SIN CONDICIÓN DE SALIDA. Los seis commits son coherentes uno a uno; se rehízo con `--soft` un reparto que
  dejaba el primero en rojo por su cuenta, porque un commit intermedio que no compila rompe `git bisect`.
  NOTAS TÉCNICAS QUE SOBREVIVEN. `npx vitest run <fichero>` sigue sin funcionar (`describe is not defined`);
  la suite corre bajo `@angular/build:unit-test` y se lanza entera con `npx ng test --watch=false`. `mvn test`
  sin `clean` deja informes huérfanos de surefire y da 283 en vez de 282. **El bundle de `ng test` NO sirve
  para juzgar `D-bundle-presupuesto`**: dio 705 y luego 709,51 kB cuando el de producción estaba en 540,71;
  el delta real de la sesión es +1,32 kB crudos. Los componentes son standalone, así que un componente nuevo
  exige entrar en el `imports` de cada consumidor: el bloque 2 rompió el build por olvidarlo, y una receta
  escrita en prosa no es un paso ejecutado.
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b`; el M4 corrió contra una copia.
  LIMPIEZA (M1-bis): archivada S126 a `bitacora-sesiones.md` (promovida a cabecera de sesión, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S127 a «Última sesión registrada (previa):»; S128
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S125 a S126.
  O-diseño (transversal, abierto en S121) ACTIVO: **C-tokens, C-sustitución y C-identidad HECHOS; C-revisión
  PENDIENTE y es el último Cambio del objetivo**. O-navegación TERMINADO en S127, y su criterio 1 deja de
  llevar salvedad. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro.
  Siguiente: **C-revisión**, la pasada por las tres vistas y los tres diálogos con el arquitecto como juez.
  Lo fija su propio M0 (ver M1-ter).


### Sesión 129 — O-diseño (transversal): **C-revisión, TRAMO 1 HECHO y criterio 4 CUMPLIDO**. Las seis decisiones de identidad pasan de estar aplicadas a estar ESCRITAS —cuatro nacen en esta sesión—, los 23 literales de peso entran en la escala, y tres deudas de «el color es la única señal» mueren juntas: la severidad de la prevalidación viaja como texto, las dos marcas de la rejilla dejan de ser mudas para un lector de pantalla y el contador deja de apagarse ante un error de acción (M0 + M2 + M3 con mutación + M4 en navegador + M1). Siete commits. **NO cierra O-diseño**: C-revisión resultó del tamaño de un objetivo pequeño y se PARTE EN TRES TRAMOS.
  Vigesimonovena sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI, con M4 obligatorio y M3 sólo
  donde hubo lógica real (la condición del contador; las cuatro decisiones y la tokenización de pesos no tocan
  un `.ts`). Suites: línea base heredada de S128 **corrida antes de tocar nada y verde** (app 282, solver 91,
  vitest 414, e2e 2); al cerrar, **vitest 419**. Se hace constar que `mvn clean test` NO se volvió a correr al
  cerrar —la sesión no toca una línea de Java— y que la e2e tampoco: los 282, los 91 y los 2 son la medición de
  apertura, no la de cierre. vitest sube +5 (dos del panel, uno de la rejilla, dos del contador) y de los 414
  heredados **ni uno se modifica**; sólo el caso (8) de `asignatura-lista.spec.ts` gana un aserto.
  M0 — EL MAPA. Cambio: C-revisión. Objetivo: O-diseño, criterios 3 y 4. Hito: ninguno funcional, que es la
  respuesta escrita en la ficha; lo que acerca es la DEMO. R-invalidación limpio por el mismo argumento de
  S121: el criterio se definió como SISTEMA para que la UI que traiga O-particiones nazca aplicándolo. Los dos
  candidatos rivales caen solos: O-demo sigue bloqueado por el correo al centro y O-ajuste-cierre abriría
  objetivo nuevo con O-diseño a un Cambio de cerrar. La sesión de Higiene/Método pierde por NOVENA vez.
  **LAS DOS DECISIONES QUE EL M0 TUVO QUE TOMAR, y ninguna estaba en la ficha.**
  (1) **«Las tres vistas» del criterio 3 es un recuento CADUCADO desde S123.** Cuando se escribió, Configuración
  era una pantalla; hoy son ocho destinos con URL propia, más la landing y la vista de horario con sus tres
  modos. Los tres diálogos siguen siendo tres. No se reinterpreta el criterio: se registra que el número está
  obsoleto y se lee la cláusula por lo que pide —cobertura transversal—, que es lo que el criterio 1 ya
  verifica por grep sobre las hojas enteras.
  (2) **C-REVISIÓN NO ERA UNA PASADA DE JUICIO, y tratarla como tal habría dejado el criterio 4 sin cumplir.**
  El M2 midió que de las seis decisiones de identidad sólo UNA estaba escrita y rotulada (la 3, de S128), una
  escrita fuera de `styles.css` sin rótulo (la 4, en `app.css`), una a medias (la 2: la familia sí, la regla de
  uso de la escala no) y **tres sin constancia en ninguna parte** (1 paleta, 5 estados, 6 tablas y rejilla).
  Eso no es documentación pendiente: es el criterio 4 sin cumplir. De ahí la partición.
  **C-REVISIÓN SE PARTE EN TRES TRAMOS, por dependencia real y no por tamaño.** *Tramo 1 (esta sesión):* el
  criterio 4 —las cuatro decisiones que faltaban, escritas y aplicadas donde su escritura destapa una
  incoherencia— más las tres deudas que son su aplicación. *Tramo 2:* el criterio 3 —los 195 valores de
  espaciado, el radio de los once controles y `D-select-nativo-desparejo`—, gobernado por decisiones ya
  escritas. *Tramo 3:* el juicio del arquitecto sobre las once pantallas y los tres diálogos, que cierra el
  objetivo. La dependencia es la misma que validó la partición de S125: **no se puede juzgar el acabado de algo
  que todavía no se ha aplicado**, y el veredicto del juez genera trabajo. Queda escrito que si el tramo 2
  resulta tan mecánico como promete el 88 % de exactos, los tramos 2 y 3 pueden fundirse —el M4 de la
  aplicación ES el recorrido por todas las pantallas—; lo decide su propio M0 con la UI aplicada delante.
  M2 — EL CENSO, por Claude Code y de solo lectura, y es lo que dimensionó los tres tramos. **29 hojas CSS**
  (28 de componente más `styles.css`), no las 28 que la ficha esperaba: `estado-lista.css` nació en S128 y el
  recuento era anterior. **192 literales reales de espaciado** repartidos por 26 hojas, de los que **171 de 195
  (88 %) caen EXACTOS en un escalón** —65 en `--e4`, 65 en `--e2`, 22 en `--e1`, 11 en `--e5`, 8 en `--e3`— y
  24 exigen redondeo declarado. Ese dato matiza a la baja el argumento de S128 que se negó a abrir un Cambio de
  tokenización («el espaciado no tiene equivalencia exacta»): la tiene en el 88 % de los casos. **NO se reabre
  la decisión de sede**, que sigue siendo correcta; lo que baja es el riesgo del tramo 2. Y un riesgo se retira
  solo: `app.css` ya está limpia, así que el espaciado no toca la barra.
  PIEZA 1 — **LAS CUATRO DECISIONES QUE FALTABAN, ESCRITAS.** No son cuatro del mismo género, y esa distinción
  gobernó el trabajo. La **1 (paleta)** era transcripción: la decisión existía desde S121 —azul institucional
  elegido por el jefe de estudios sobre maqueta viva, con la alternativa descartada— y lo que faltaba era que
  viviera donde gobierna; se escribe con la regla **UN COLOR, UN TRABAJO** y el papel de los 22 tokens (no 17:
  esa cifra es anterior a C-identidad). La **2 (tipografía)** era casi transcripción: faltaba la regla de uso
  de la escala. La **5 (estados)** fue PROMOCIÓN y no invención: el criterio ya existía escrito pero LOCAL a
  `panel-prevalidacion.css` —rojo fuerte para el fallo, gris apagado para lo no ejecutado, verde para lo sano,
  caja de aviso para los hallazgos— y se sube a sistema tal cual. La **6 (tablas y rejilla)** era la única que
  había que decidir de verdad, y se resuelve declarando lo que ya es cierto: censadas, **OCHO hojas repiten el
  molde de tabla carácter a carácter** y sólo `grupo-lista` añade un gris de columna, sin rayado alterno, sin
  realce al pasar el ratón y sin fondo de cabecera; la rejilla usa otra gramática y se declara excepción
  correcta con su razón. **Cero reglas CSS cambian**: `styles.css` pasa de 179 a ~285 líneas, todas comentario.
  **LA JERARQUÍA DE ACCIONES SE DECLARA DE PASO, y resuelve una pregunta del arquitecto.** Preguntado si el
  verde debía pasar a ser el color de guardar, se argumentó en contra: el verde significa VERIFICADO y es la
  señal de la prevalidación sana y del destino válido de un arrastre; extenderlo a guardar dejaría la decisión
  5 sin color para «hecho y correcto» y obligaría a repintar ocho formularios y tres diálogos. La captura de
  los cuatro botones reales lo zanjó mejor que el argumento: la jerarquía ya está resuelta **por relleno y no
  por color** —principal en acento pleno, secundaria en superficie con borde, destructiva con tinta de error,
  en curso en acento apagado—, y eso estaba aplicado sin estar escrito. Se declara. El arquitecto eligió que el
  verde se quede en verificado.
  **UN CENSO DE TOKENS QUE EL GUION NO BUSCABA: cuatro están MUERTOS.** `--radio-s`, `--fuente-datos`,
  `--color-ok-fondo` y `--color-info-fondo` no los usa nadie. Se marcan como sin uso y **no se retiran**:
  borrar es un cambio sin criterio detrás. El de `--radio-s` cierra además una pregunta abierta del censo: las
  diez reglas `__input` repiten `border-radius: 4px` LITERAL, que no es `--radio-s` (3) ni `--radio-m` (6), y
  por eso `.cabecera-lista__busqueda` —la única que usa el token— es hoy el único control con las esquinas
  distintas a los otros diez. Se unifica en el tramo 2, en `--radio-m` por decisión del arquitecto
  («la mayor uniformidad posible»): el infractor es el literal, no el token.
  PIEZA 2 — **LOS 23 LITERALES DE `font-weight` PASAN A LA ESCALA.** 19 de `600` y 4 de `700`, con equivalencia
  EXACTA y cero píxeles movidos, en 9 hojas. El guion mide antes de tocar y aborta si el mapa no cuadra
  (patrón de la tanda 2 de S121); vigila además dos casos que el recuento no cubría —el atajo `font:` con el
  peso dentro, y valores como `bold` o `500`— y no había ninguno. Reparto resultante: **22 `--peso-medio`, 6
  `--peso-fuerte`, 1 `--peso-normal`**. Hallazgo de método asociado: **el 79 % de los usos de peso ignoraba la
  escala y la verificación binaria del criterio 1 no lo detecta**, porque sólo mira color y `font-size`; el
  comprobador de cuatro vías de S121 gana una quinta —peso literal—. Los tres `line-height: 1` de la rejilla no
  son sustituibles (no hay escalón de valor 1) y su exclusión queda declarada.
  PIEZA 3 — **LA SEVERIDAD DE LA PREVALIDACIÓN DEJA DE VIAJAR SÓLO POR COLOR, y la deuda tenía DOS caras.** La
  conocida son los contadores; la que S121 no vio son las filas, donde `[class.es-error]` sólo cambia el color.
  **El dato que convierte la mejora en defecto medido: `--color-error` y `--color-aviso` tienen 1,28:1 ENTRE
  SÍ**, cuando a un elemento no textual portador de información se le piden 3:1. Individualmente los dos pasan
  sobre el fondo del panel (6,80:1 y 5,33:1); el problema es que no se distinguen uno de otro. Y en el estado
  habitual —sin ERROR el panel arranca colapsado— los dos números son lo ÚNICO en pantalla, luego el color no
  es redundante: **es la información**. Arreglo en tres partes. (a) Rótulo VISIBLE junto a cada número, no sólo
  `title`, y **fuera** del `<span>` del número para no tumbar el aserto que lee su `textContent`. (b) La
  severidad se pinta **como texto** en la fila, con el valor CRUDO del enum: un tercer valor futuro mostraría
  su nombre en vez de disfrazarse de AVISO, que es lo que `[class.es-error]` hace hoy. (c) El marco del panel
  toma la severidad MÁXIMA, con contrastes medidos sobre `--color-error-fondo` antes de escribirlos (6,35:1,
  4,97:1 y 13,16:1, los tres sobre 4,5:1).
  **EL RÓTULO NO PUEDE DECIR «AVISOS», y la razón es de contrato.** `numAvisos` cuenta el RESTO
  (`severidad !== 'ERROR'`), y `prevalidacion.model.ts` deja `severidad` como `string` a propósito para que un
  tercer valor futuro degrade en vez de romper el parseo. Prometer «avisos» mentiría el día que el enum crezca,
  así que el rótulo es **«otros hallazgos»**, más torpe de leer y honesto. Es el mismo razonamiento con que
  S128 se negó a que la leyenda de la insignia prometiera cuadrar con `Totales`.
  **NO SE DECLARA QUE UN ERROR ABORTE LA GENERACIÓN EN LOS `title`.** Se dijo en el análisis y no estaba
  verificado desde el cliente (`horario-view.html:23` sólo deshabilita el botón si no se ha prevalidado), así
  que los `title` dicen qué severidad cuenta cada número y nada más.
  PIEZA 4 — **LAS DOS MARCAS DE LA REJILLA DEJAN DE SER MUDAS.** `.oculta` —la marca `+N` que S127 construyó
  para que las plazas ocultas dejaran de serlo— llevaba `[title]` y ninguna etiqueta accesible, a quince líneas
  de la insignia que S128 sí dotó del par completo: un lector de pantalla oía «más 4» y nada más. El
  `aria-label` **repite el `title` LITERALMENTE y no mejora su redacción**, porque la cadena es de S127 y
  cambiarla aquí sería afirmar algo nuevo sobre lo que devuelven `marcaOcultas` y `detalleInstancia`. A
  propuesta del asistente y por decisión del arquitecto entra también `.grupos`, que tiene el mismo defecto en
  el mismo fichero: dejar la mitad arreglada obliga a volver. Coste en altura: cero, son atributos.
  PIEZA 5 — **EL CONTADOR DEJA DE APAGARSE ANTE UN ERROR DE ACCIÓN, y la ficha de la deuda pedía un arreglo más
  caro del necesario.** Decía «dos señales en vez de una» (error de carga frente a error de acción); el M2
  midió que basta cambiar el CRITERIO: `!cargando() && <entidad>().length > 0` en lugar de
  `!cargando() && !error()`. Se eligió **A** —el criterio en las siete plantillas— frente a **B** —mover la
  decisión dentro de `cabecera-lista`—, y el argumento es cuál de las dos cosas está mal: lo que falla es el
  criterio, no dónde vive. B derogaría un javadoc deliberado (`cabecera-lista.ts:9-13`: «no decide cuándo se ve
  el contador») para arreglar otra cosa, y sería exactamente lo que S128 declaró fuera de alcance al negarse a
  tocar ese contrato. La centralización no se pierde: la regla vive UNA vez en la decisión 5 de `styles.css`, y
  siete plantillas que la aplican no son siete decisiones.
  **LA CONDICIÓN VA SOBRE LA LISTA CARGADA Y NO SOBRE `visibles()`, y esa es la mitad no obvia.** La simetría
  con la tabla es atractiva y equivocada: con `visibles()`, una búsqueda sin resultados escondería el contador
  —hoy dice «0 de N»— e inutilizaría el instrumento en el único momento en que hace falta leerlo. Sería además
  una REGRESIÓN de comportamiento existente disfrazada de arreglo, porque hoy ese caso sí se ve. La propiedad
  que el javadoc temía sigue cubierta: si la carga falla desde vacío el array está vacío y no se pinta ningún
  «0». Efecto declarado y aceptado: un catálogo vacío bien cargado deja de mostrar el «0» junto al título.
  M3 — CAMPAÑA DE MUTACIÓN SOBRE LA CONDICIÓN DEL CONTADOR, con la base verde comprobada antes de creerse nada.
  Cuatro mutantes: `"true"`, volver a `!error()`, usar `visibles()` y cambiar `> 0` por `>= 0`. **Tres mueren y
  UNO SOBREVIVE**, y el que sobrevive es precisamente la decisión que el comentario nuevo argumenta: nadie
  ejercía el camino de `visibles()` porque el caso (8) hace la búsqueda sin resultados y no mira el contador.
  **La supervivencia se PREDIJO antes de correr la campaña**, no se descubrió al leerla. Se cierra con un
  ASERTO —no un caso— en el (8), que ya monta el escenario: tras `buscar('zzz')`, el contador dice «0 de 2».
  Con él mueren los cuatro. Las otras seis listas quedan cubiertas por grep de la línea 6, que es el mecanismo
  que el comentario de la tabla ya usa.
  M4 — EN NAVEGADOR, sobre COPIA de la base demo, y con el disparador que S121 dio por desconocido. **La
  verificación que `D-prevalidacion-contraste-sin-ver` pedía desde S121 —ver un error y un aviso juntos— no era
  difícil: es IMPOSIBLE.** Medido en el backend: `Severidad` tiene dos valores y las tres reglas de
  `PrevalidacionService` emiten `ERROR`; **nadie emite `AVISO` ni en `main` ni en los tests**, y el propio enum
  documenta que se conserva por contrato y como candidato natural del palomar de aulas si algún día entra. La
  deuda cierra POR CONSTRUCCIÓN —la severidad ya viaja como texto— y no por medición del contraste.
  **EL DISPARADOR DE HALLAZGOS ESTABA ESCRITO, en la ficha de una deuda hermana.** S121 lo registró como NO
  SABIDO; la respuesta llevaba desde S117 en `D-prevalidacion-ciega-a-holgura-cero`: `GRUPO_SOBRECARGADO` salta
  cuando la demanda supera los tramos. En la práctica se usó la regla hermana, más barata: una actividad
  `DISTRIBUIDA` con 7 repeticiones y 5 días lectivos dispara `REPETICIONES_EXCEDEN_DIAS` ella sola, sin tocar
  la demanda de ningún grupo, porque el formulario no conoce los días lectivos y la acepta sin protestar.
  **LAS CINCO COMPROBACIONES.** (1) Rejilla intacta antes de tocar: 1B-A / 4ºA / 2B-B en **4 / 3 / 0** marcas
  `+N`, los números de S128. (2) Contador con 409 de borrado en Asignaturas: el error en pantalla, las 100
  filas debajo y el rótulo **conservando su número**; y con `zzz` en el buscador, **«0 de 100»**. (3)
  Prevalidación con hallazgos: panel **en rojo** y no en ámbar, «2 errores» y «0 otros hallazgos» con sus
  rótulos, y `ERROR` como primera columna de cada fila. (4) **El panel NO cuesta altura**: 1B-A sigue en 4
  marcas con el detalle desplegado Y plegado, tres estados medidos sobre la misma ventana. (5) La sospecha del
  `:host { display: block }` **no se reproduce** y se archiva.
  **UN HALLAZGO QUE EL M4 NO BUSCABA:** `GRUPO_SOBRECARGADO` reportó `1B-A 37 / 30`, luego la actividad nueva
  sumó 7 tramos sobre un grupo que estaba EXACTAMENTE en 30. Es evidencia directa y fotografiada de lo que
  `D-prevalidacion-ciega-a-holgura-cero` afirmaba por conteo. Integridad: `educhronos-demo-m4.db` en
  `ea1a70a0337831dddccdbcd322f48e9b` antes y después; el M4 corrió sobre `educhronos-s129-m4.db`, desechable.
  R-TERMINADO, aplicado cinco veces. No se funden las nueve reglas `__input` idénticas —la fusión de S128
  estaba dentro de una de las seis decisiones y «tratamiento de controles de formulario» no es ninguna—. No se
  maquetan los cuatro ganchos sin regla de `panel-prevalidacion` ni su botón sin clase. No se estrena
  `--fuente-datos` en `.cuenta` pese a ser el candidato natural: dar uso a un token muerto en UN sitio es peor
  que dejarlo sin uso, y esa decisión es del tramo 3. No se unifican las cuatro convenciones de nombre de
  clases de estado: la decisión 5 decide el TRATAMIENTO, no la nomenclatura. Y no se abre caso para el
  `!cargando()`, que la sesión no cambia.
  ERRORES DEL ARQUITECTO, SIETE, Y TODOS DE LA MISMA FAMILIA: patrones y rangos escritos **de memoria** teniendo
  el fichero que los define a tres secciones de distancia. (1) En el censo, `grep -rn -- '--e[1-6]' --include=…`:
  el `--` convirtió el patrón en operando, `grep` devolvió 2, `pipefail` lo propagó y el `||` imprimió «cero
  usos fuera de styles.css» **justo debajo de los quince usos que acababa de listar**. Es la variante peor de la
  familia: no enmascara un fallo, PUBLICA UNA CONCLUSIÓN FALSA. (2) En el mismo censo, un `echo` que afirmaba
  «NADIE estila select» antes de medirlo, y era falso. (3) Buscar `--(peso|linea)` cuando los tokens se llaman
  `--lh-*`: la sección informó los pesos y **calló el interlineado sin decir que no lo había buscado**. (4) Un
  patrón `(signal|computed)[^;]*(cargando|error…)` con el orden invertido respecto a como se escribe
  TypeScript, que devolvió siete «cero coincidencias» falsos. (5) Buscar `title=` literal, perdiendo `[title]`
  y `[attr.aria-label]`, que es justo donde vivía el molde que se buscaba. (6) Los rangos de línea de
  `PrevalidacionService` dados a ojo y cruzados: lo rotulado «regla (c)» era la (d) y viceversa. (7) Una regex
  para sustituir el comentario del contador que casaba también el comentario de la tabla y **borró cinco líneas
  de más en las siete plantillas**; lo cazó Claude Code en el diff, no la suite, porque ese borrado no ponía
  rojo nada. Se añade además el error de haber metido un dato ajeno sin verificar dentro de un mensaje de
  aborto («el censo decía once», cuando eran nueve). Corolario operativo nuevo: **un guion de lectura no busca
  por nombres recordados; los deriva de lo que él mismo acaba de volcar, o el volcado va primero y el patrón se
  escribe después.**
  **UNA REINCIDENCIA QUE MERECE NOMBRE PROPIO:** el `grep` sin coincidencias bajo `pipefail` matando el proceso
  volvió a ocurrir en el guion de la campaña de mutación, dos guiones después de que el asistente lo
  diagnosticara en el censo. La lección escrita no basta si el siguiente guion se escribe sin releerla.
  DEUDA. **TRES CERRADAS**: `D-contador-se-apaga-con-error` (S128 → S129) en su sede escrita y con un arreglo
  más barato que el que su ficha proponía; `D-prevalidacion-contraste-sin-ver` (S121 → S129) por construcción,
  con la corrección de que su verificación pendiente era imposible y no difícil; y
  `D-desbordamiento-sin-etiqueta`, que **nace y muere en la misma sesión**, ampliada de una marca a las dos.
  **UNA NUEVA**: `D-tokens-sin-uso`, sede C-revisión tramo 3. `D-select-nativo-desparejo` se AMPLÍA y se
  CORRIGE: su texto era falso. Deuda bloqueante abierta: sigue en 1 (D31-a, las aulas de FPB).
  SIN CONDICIÓN DE SALIDA. Los siete commits son coherentes uno a uno y con los asuntos todos distintos,
  verificado en el cierre. Dos incidentes de historia, los dos
  corregidos antes de publicar: un commit del spec que **no compilaba** —un `});` adelantado que sacaba dos
  casos del `describe`, culpa de un ancla ambigua del asistente— se enmendó tras verificar los 416, porque un
  intermedio roto rompe `git bisect`; y tres asuntos casi idénticos consecutivos se fundieron en uno. Es la
  misma disciplina que S128 aplicó con `--soft`.
  NOTAS TÉCNICAS QUE SOBREVIVEN. `npx vitest run <fichero>` sigue sin funcionar; la suite se lanza entera con
  `npx ng test --watch=false`. `mvn clean test` sin `clean` sigue dando 283 en vez de 282. **La orden de
  arranque contra otra base, LITERAL y probada en esta sesión** (paga el corolario de `D-arranque-no-literal`
  sin abrir su sesión): `mvn -pl solver install -DskipTests` primero —o se compila contra el jar de `~/.m2`— y
  después `mvn -pl app spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:/home/luis/desarrollo/educhronos/app/educhronos-s129-m4.db"`,
  con UN solo argumento. Los comentarios de CSS global se minifican fuera del artefacto: `styles.css` conservó
  su hash tras +189 líneas de comentario, lo que **también significa que un hash idéntico no prueba que el
  fichero haya cambiado** —el asistente leyó esa igualdad como evidencia de minificación cuando la explicación
  simple era que el fichero aún no estaba escrito—. El esquema real llama `grupo_administrativo` a la tabla de
  grupos y `sesion` enlaza por `plaza_id`, no por `actividad_id`.
  **CORRECCIÓN AL BUNDLE REGISTRADO, y es de la familia de `D-748-sin-derivacion`.** `plan:705` anotaba 542,03
  kB crudos; medido sobre HEAD limpio en `94b97df` son **539,86 kB / 121,68 transferidos**, con margen de 10,14
  kB hasta el aviso de 550. La hipótesis de que aquella cifra se tomó en un punto intermedio del bloque de S128
  es razonable y **no está probada**, y no se reconstruye. Tras la tokenización de pesos: **540,18 kB / 121,77**
  (+0,32 kB crudos, exactamente los 23 nombres de token más largos que los literales), margen 9,82 kB. Se hace
  constar el hueco: **las piezas 3, 4 y 5 no se volvieron a medir**; añaden comentarios y atributos, así que el
  delta esperado es pequeño, pero no está medido. Lección tomada: un número de bundle se anota con el commit
  sobre el que se midió.
  LIMPIEZA (M1-bis): archivada S127 a `bitacora-sesiones.md` (promovida a cabecera de sesión, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S128 a «Última sesión registrada (previa):»; S129
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S126 a S127.
  O-diseño (transversal, abierto en S121) ACTIVO: **C-tokens, C-sustitución y C-identidad HECHOS; C-revisión
  PARTIDO EN TRES TRAMOS, con el TRAMO 1 HECHO**. Criterios 1, 2 y 4 CUMPLIDOS; el 3 es el tramo 2. O-demo (H2)
  sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro. Siguiente: **el TRAMO 2 de
  C-revisión**, el espaciado y los radios sobre decisiones ya escritas. Lo fija su propio M0 (ver M1-ter).

### Sesión 130 — O-diseño (transversal): **C-revisión, TRAMO 2 APLICADO**. Los 195 valores de espaciado de las hojas de componente pasan a la escala `--e1..--e6` con los 24 redondeos DECLARADOS uno a uno, y el radio se unifica en dos niveles sobre 36 reglas: `--radio-s` sube de 3 a 4 px y estrena 21 usos, `--radio-m` cubre los 15 contenedores. Al buscar el precedente de declaración de S128 resulta que NO EXISTE y que su recuento era falso: nace `D-declarado-sin-artefacto`. El M4 en navegador destapa que la jerarquía de acciones que `styles.css` declara «aplicada» NO lo está (M0 + M2 + M4 en navegador + M1, sin M3: el tramo no toca un solo `.ts`). Cinco commits. **NO cierra O-diseño**: el tramo 3 queda a medias y su trabajo restante pasa a S131.
  Trigésima sesión bajo el mapa Hito→Objetivo→Cambio. Tipo Configuración/UI, con M4 obligatorio y sin M3: el
  tramo 2 no toca un solo `.ts`. Suites: línea base heredada de S129 **corrida antes de tocar nada y verde**
  (solver 91, app 282, vitest 419 en 49 ficheros); tras aplicar, **solver 91, app 282, vitest 419**, sin un
  solo caso reescrito. Que no se reescribiera ninguno se PREDIJO antes de aplicar y no se descubrió al
  correr: el único `__input` que un spec consume es `jornada.spec.ts:104` y la sustitución no cambia ningún
  nombre de clase. **e2e NO corrido todavía.**
  BUNDLE, atado a su commit como manda la lección de S129, y medido en worktree aparte para poder AISLAR el
  tramo. Base `0e45c9b`: **541,21 kB crudos / 121,89 transferidos**. Tras el tramo 2, `ffdf966`: **542,39 /
  121,84**. El tramo cuesta **+1,18 kB crudos y −0,05 transferidos**: repetir `var(--e2)` comprime mejor que
  una dispersión de literales, así que la métrica que llega al usuario MEJORA mientras el crudo sube. Margen
  hasta el aviso de 550: **7,61 kB**. Todo el crecimiento está en `main-*.js`, donde Angular incrusta el CSS
  de componente; `styles-*.css` se queda clavado en 3,33 kB. De paso se tapa el hueco que S129 declaró
  abierto —sus piezas 3, 4 y 5 nunca se remidieron sobre los 540,18 de media sesión—: costaron **+1,03 kB**.
  **LOS 24 REDONDEOS DEL TRAMO 2, DECLARADOS UNO A UNO.** Reparten en 21 líneas de 8 hojas —tres
  líneas llevan dos redondeos, una por valor—; las otras 16 hojas tocadas caen exactas y no
  necesitan declaración. Los números de línea son válidos ANTES y DESPUÉS: la sustitución es
  línea a línea y no mueve ninguna. Ninguno toca `horario-grid.css` (excluida expresamente) ni
  `app.css` (ya limpia de literales de espaciado, luego el trabajo NO tocó la barra y la
  restricción heredada de S128 no llegó a gobernar nada; verificado, no supuesto).
  *actividades/actividad-form.css:102* `padding: 0.2rem 0.6rem` → `var(--e1) var(--e2)` (+0,8 / −1,6).
  *cabecera-lista/cabecera-lista.css:39* `padding: 0.375rem 0.5rem` → `var(--e1) var(--e2)` (−2,0; el
  segundo valor ya era exacto).
  *confirmar-generacion/confirmar-generacion.css:6* `padding: 1rem 1.2rem` → `var(--e4) var(--e4)` (−3,2);
  *:14* y *:22* `margin: 0 0 0.6rem` → `var(--e2)` (−1,6 cada una); *:26* `margin: 0 0 0.8rem` →
  `var(--e3)` (−0,8); *:42* `gap: 0.6rem` → `var(--e2)` (−1,6).
  *grupos/tutoria-dialogo.css:56* `padding: 0.15rem 0` → `var(--e1) 0` (+1,6).
  *horario-view/horario-view.css:32* `gap: 0.4rem` → `var(--e2)` (+1,6); *:58* `padding: 0.4rem 0.6rem`
  → `var(--e2) var(--e2)` (+1,6 / −1,6); *:59* `margin-bottom: 0.6rem` → `var(--e2)` (−1,6).
  *jornada/confirmar-reemplazo.css:5* `padding: 1rem 1.2rem` → `var(--e4) var(--e4)` (−3,2); *:13* y *:18*
  `margin: 0 0 0.6rem` → `var(--e2)` (−1,6 cada una); *:23* `gap: 0.6rem` → `var(--e2)` (−1,6).
  *jornada/jornada.css:19* `padding: 0.15rem 0.5rem` → `var(--e1) var(--e2)` (+1,6); *:88*
  `padding: 0.35rem` → `var(--e1)` (−1,6).
  *panel-prevalidacion/panel-prevalidacion.css:20* `padding: 0.4rem 0.6rem` → `var(--e2) var(--e2)`
  (+1,6 / −1,6); *:21* `margin-bottom: 0.6rem` → `var(--e2)` (−1,6); *:43* `gap: 0.6rem` → `var(--e2)`
  (−1,6); *:73* `margin: 0.4rem 0 0` → `var(--e2) 0 0` (+1,6).
  **REPARTO POR MAGNITUD:** 19 de los 24 se mueven 1,6 px; dos menos de 1 px (+0,8 en
  `actividad-form:102`, −0,8 en `confirmar-generacion:26`); uno 2,0 px (el padding del buscador de
  las listas); y dos 3,2 px (el padding de las dos cajas de confirmación).
  **CUATRO REDONDEOS CAMBIAN LA FORMA, NO SÓLO EL TAMAÑO, y por eso se destacan aparte:** en
  `confirmar-generacion:6`, `confirmar-reemplazo:5`, `horario-view:58` y `panel-prevalidacion:20` los
  dos valores del padding eran DISTINTOS —`1rem 1.2rem` y `0.4rem 0.6rem`— y caen los dos en el mismo
  escalón, así que el padding pasa de asimétrico a simétrico. Quedan escritos `var(--e4) var(--e4)` y
  `var(--e2) var(--e2)`, redundantes a propósito: colapsarlos a un solo valor borraría del código la
  huella de que ahí hubo un redondeo. Son los cuatro primeros sitios que hay que mirar en el M4.
  **EL RADIO SE UNIFICA EN DOS NIVELES, y el censo de S129 que lo describía se quedaba en la mitad.** La
  ficha hablaba de «las diez reglas `__input`» y del buscador; medido en el tramo 2, el literal
  `border-radius: 4px` estaba en **35 reglas**, doce de ellas en contenedores. Unificar sólo las once habría
  dejado el diálogo contenedor a 4 px junto a un control a 6 px: la incoherencia se mueve, no se cierra. Se
  aplica una regla de dos niveles —`--radio-s` en controles (campos, botones, buscador), `--radio-m` en lo
  que los contiene (formularios, diálogos, el marco de plaza y los bloques de mensaje)— sobre **36 sitios:
  21 controles y 15 contenedores**. `--radio-s` sube de 3 a 4 px A PROPÓSITO, para que los 20 controles que
  ya estaban en 4 px literal NO se muevan: **el único píxel de radio que cambia en toda la aplicación es el
  buscador de las listas, de 6 a 4 px**, que es justo el síntoma que la deuda describía. Consecuencia sobre
  `D-tokens-sin-uso`: baja de cuatro tokens a **tres** y pierde su parte de radio, que era su única sede
  compartida con el tramo 3.
  **LA DECLARACIÓN DE S128 NO EXISTÍA, Y SU RECUENTO ERA FALSO.** La regla de `styles.css` exige que un
  redondeo se DECLARE; S128 afirma DOS VECES haberlo hecho —ficha y plan— y no hay artefacto en ninguna de
  las cuatro sedes posibles: ni lista en `styles.css`, ni en los documentos, ni en el mensaje de commit,
  que además no era sede posible porque **ningún commit del repo tiene cuerpo**. Reconstruidos desde
  `cd6b43f..94b97df` con un derivador validado antes contra los 24 de este tramo —reprodujo 24 de 24—,
  resultaron **CUATRO y no tres**, verificados línea a línea sobre los blobs. Nace
  `D-declarado-sin-artefacto`, tercera de la familia junto a `D-arranque-no-literal` y
  `D-748-sin-derivacion`. La afirmación de S128 de que ninguno tocaba la altura de la rejilla SÍ era cierta
  —el vertical de la barra cae exacto en `--e3`—, pero no era comprobable por nadie.
  D-GUION-EXIT-ENMASCARADO, **SIETE INSTANCIAS**, seis del arquitecto y **una del asistente**: (1) `^Results:`
  no casa nunca porque Maven prefija `[INFO] `, y la sección omitió el recuento sin decir que no había
  encontrado nada; (2) `^| $D` no casa las filas tachadas, y dos deudas CERRADAS en S129 se informaron como
  «NO APARECE en §4»; (3) el patrón de espaciado sin la exclusión que la propia ficha ordena, que dio 214 y
  parecía contradecir los 195 de S129; (4) la lista de redondeos recortada por un `sed` justo antes del
  `ABORTA` que la explicaba, publicando un encabezado con cero líneas debajo; (5) un delimitador `^### ` que
  volcó S129, S128 y el bloque de fases bajo el rótulo «REGISTRO REAL DE S129, ÍNTEGRO»; (6) una bandera
  `--listar-redondeos` que el transformador no tiene, cuyo `||` de respaldo dumpeó las hojas enteras bajo el
  rótulo de la lista; y (7) **del asistente**, un `grep -E` con retrorreferencia `\2` que `ugrep` no soporta,
  cuyo error se fue a la salida y cuyo `||` publicó «ninguna» cuando había cuatro.
  R-TERMINADO. No se colapsan a un solo valor los cuatro `padding` que el redondeo dejó simétricos: la
  redundancia es la huella de que ahí hubo un redondeo. No se toca `.confirmar-reemplazo__confirmar` ni los
  botones de `confirmar-generacion`, que no declaran `border-radius` y pintan con el del navegador: el tramo
  2 SUSTITUYE literales, y añadir una declaración donde no había ninguna es otra cosa; van a la lista del
  tramo 3. No se retira `--radio-s` ni ningún otro token: esa decisión es del tramo 3.
  INTEGRIDAD: `educhronos-demo-m4.db` en `ea1a70a0337831dddccdbcd322f48e9b` verificado antes de copiar; el
  M4 correrá sobre `educhronos-s130-m4.db`, copia con el mismo md5.
  HALLAZGOS DEL M4, EN CRUDO. Medidos y anotados sin veredicto: el juicio es del tramo 3.
  **(a) LA JERARQUÍA DE ACCIONES DE `styles.css:59-63` SE DECLARA «aplicado, se declara» Y NO LO
  ESTÁ.** Lo escrito: principal = acento pleno con tinta `sobre-acento`; secundaria = superficie con
  borde; destructiva = superficie con borde y tinta de error; inactiva o en curso = acento apagado.
  Medido contra el CSS, cuatro discrepancias: (1) `__guardar` lleva `background:
  var(--color-superficie)`, que es el relleno que la regla asigna a la SECUNDARIA; (2) `__cancelar` y
  `__guardar` comparten UNA SOLA regla agrupada en los siete formularios, luego son indistinguibles;
  (3) los siete `__borrar` de fila de lista llevan sólo `color: var(--color-error)`, sin superficie y
  sin borde; (4) en `pdc-dialogo`, `__guardar` y `__borrar` comparten regla, luego principal y
  destructiva se pintan igual. El dato que cierra la medición: en toda la aplicación hay **un solo
  `background: var(--color-acento)`, y es la barra** (`app.css:25`); ningún botón lleva relleno de
  acento. `styles.css` no estila los elementos `button`, `input`, `select` ni `textarea` —cero reglas
  de elemento—, así que no hay una capa base que lo aporte. La jerarquía se declaró en S129 al zanjar
  la pregunta del verde con el argumento de que «ya se resuelve por relleno y no por color».
  **(b) CENSO DE BOTONES: 10 CON CAJA, 9 SIN RADIO, 8 SIN NINGUNA REGLA.** Con caja: las diez reglas
  agrupadas `__cancelar, __guardar` de siete formularios y tres diálogos, byte a byte iguales entre
  sí y ahora en `--radio-s`. Sin radio y con una sola propiedad: los siete `__borrar` de fila
  (`color: var(--color-error)`), `.confirmar-reemplazo__confirmar` y `.jornada__guardar` (sólo
  `font-weight`). Sin NINGUNA regla CSS, luego pintando con el estilo de agente de usuario: los siete
  `__editar` de fila y `.cabecera-lista__nuevo`; `.confirmar-reemplazo__cancelar` tampoco tiene
  regla. El tramo 2 unificó el radio de los controles que YA tenían caja; esta segunda familia nunca
  la tuvo y no cabía en el tramo 2, que SUSTITUYE literales y no crea declaraciones.
  **(c) `D-select-nativo-desparejo` QUEDA CERRADA DE RADIO Y ABIERTA DE FLECHA.** Alcance remedido:
  14 coincidencias de `<select`, de las que UNA es una mención dentro de un comentario HTML
  (`grupos/tutoria-dialogo.html:33`), luego **13 elementos reales**: 11 con la clase `__input` de su
  formulario y 2 sin clase ninguna (`horario-view/horario-view.html:13` y `:22`). Sigue NO SABIDO si
  un `<select>` con clase de `<input>` pinta su flecha nativa. **El censo de S129 no contemplaba una
  familia con el mismo problema: los cinco `input[type="number"]`** (`aula-form.html:30` y `:40`,
  `actividad-form.html:32` y `:45`, `nivel-form.html:17`), que SÍ llevan la clase del formulario y
  tienen adorno nativo propio —las flechas del contador—, exactamente el caso de la flecha del
  `<select>`. Tampoco contemplaba los dos `type="radio"` sin clase de `actividad-form.html` ni los
  dos `type="checkbox"` (uno sin clase en `actividad-form.html`, otro con `jornada__toggle`).
  **(d) EL PADDING LATERAL DE `.app__contenido` NO CRUZA EL CRITERIO 4.** La regla es `padding:
  var(--e4) 0`; el cero lateral es PREEXISTENTE —era `1rem 0`, que cae exacto en `--e4`, así que el
  tramo 2 no movió ahí ningún píxel— y contrasta con los `var(--e5)` laterales de la barra.
  `horario-grid.css` no declara ningún ancho absoluto: `width: 100%` en la tabla, `width: 3rem` en la
  columna de horas, `min-width: 0`. Y `diseno-navegacion.md:132` deja escrito que «el ancho **no**
  era el problema: `.rejilla { width:100%; table-layout:fixed }`»: el presupuesto MEDIDO del criterio
  4 es de altura, no de anchura. Dato adyacente que se anota sin conclusión: `diseno-navegacion.md:162`
  registra cinco aulas de nombre largo como «agravante de ancho no previsto».
  EL M4 EN NAVEGADOR, Y SU VEREDICTO. Recorrido sobre `educhronos-s130-m4.db`, copia de la demo, con la UI
  del tramo 2 servida y VERIFICADA en el artefacto y no sólo en el fuente: el bundle de `static/` contiene 21
  `var(--radio-s)`, 17 `var(--radio-m)` y CERO `border-radius:4px`, los mismos números que el transformador
  midió sobre las hojas. Veredicto por parada: (1) las dos cajas de diálogo y las dos de aviso aguantan el
  `padding` simétrico, y el riesgo real —romper una simetría deliberada entre hermanas— no se materializó,
  porque cada pareja partía del mismo literal y cae en el mismo token, así que siguen siendo idénticas entre
  sí; (2) el buscador de las listas NO desentona: comparte borde, radio y `box-sizing` con las diez `__input`,
  y sus dos diferencias —`padding: var(--e1) var(--e2)` frente a `var(--e2)`, y `font-size: var(--tam-s)`— son
  deliberadas, para caber en la cabecera. El arquitecto lo ve «un poco más pequeño» y PREFIERE ese tamaño; se
  deja como está, porque igualarlos borraría una intención igual que la habría borrado colapsar los cuatro
  `padding` asimétricos; (3) `D-select-nativo-desparejo` queda MEDIDA por fin, y la respuesta es que SÍ: en
  «Editar aula» y «Editar actividad» los `<select>` con la clase de su formulario salen grises con la flecha
  nativa al lado de `<input>` blancos de la misma columna. Alcanza a los 13, no a los 2.
  LO QUE EL M4 DESTAPÓ Y NADIE BUSCABA, que es el hallazgo de la sesión: `styles.css:59-63` declara la
  jerarquía de acciones como «aplicado, se declara» y NO lo está. Un solo `background: var(--color-acento)` en
  toda la aplicación, y es la barra. Esto no es una deuda de acabado pendiente: es el criterio 3 declarado
  cumplido sobre un hecho falso, y arrastra algo peor —S129 zanjó la pregunta del verde con el argumento de que
  «la jerarquía ya se resuelve por relleno»; el argumento sigue siendo bueno, el hecho no era cierto, luego esa
  pregunta no está resuelta sino cerrada—. Tercera vez en esta sesión que una afirmación escrita no tiene
  detrás lo que dice tener, y la primera en que lo que falta no es el registro sino el PRODUCTO.
  R-TERMINADO, CON LO QUE SE DEJA FUERA Y POR QUÉ. (a) Rediseñar los avisos del horario como un botón con
  icono de estado que abra el detalle en un panel aparte —propuesta del arquitecto en el M4, con buen
  argumento: liberaría espacio para la rejilla—. Cambia la INTERACCIÓN, no el acabado, y O-particiones toca
  frontend después: R-terminado y R-invalidación a la vez. Nace `D-avisos-como-bloque-fijo`. (b) Igualar el
  tamaño de todos los controles, descartado por la medición y no por la regla: el buscador no es incoherente,
  es denso a propósito. (c) El `padding` lateral cero de `.app__contenido`: NO lo causó el tramo 2 —era
  `1rem 0`, que cae exacto en `--e4`—, pero el M4 lo desbloquea, porque `horario-grid.css` no declara ningún
  ancho absoluto y `diseno-navegacion.md:132` deja escrito que el presupuesto medido del criterio 4 es de
  ALTURA y no de anchura. Entra en el tramo 3, no en el tramo 2, porque crear una declaración no es sustituir
  un literal.
  LO QUE ENTRA EN EL TRAMO 3 Y SE EJECUTA EN S131, y la razón de que quepa en O-diseño: aplicar la jerarquía
  de acciones a los 34 botones y darle padding lateral a la vista NO es diseñar. Los tres tokens existen, la
  regla está escrita en la decisión 1 desde S129 y sólo falta ejecutarla; y el criterio 3 no puede darse por
  cumplido mientras `styles.css` afirme «aplicado» sobre algo que no lo está. Cabe en el objetivo; no cabía en
  esta sesión, y eso se dijo antes de intentarlo en vez de trocearlo mal para que entrase.
  DEUDA NUEVA, cuatro altas y una instancia: `D-declarado-sin-artefacto` (§4), `D-jerarquia-declarada-sin-aplicar`,
  `D-avisos-como-bloque-fijo`, la ampliación de `D-select-nativo-desparejo` a los cinco `input[type=number]`,
  los dos radio y los dos checkbox —su título se le ha quedado corto: lo común no es la clase que falta, es que
  NINGÚN control con adorno nativo se ha neutralizado—, y una instancia nueva de `D-arranque-no-literal`: la
  orden literal del plan levanta el backend SIN frontend desde un árbol recién limpiado, porque los cuatro
  plugins que construyen el bundle cuelgan de `prepare-package` y `spring-boot:run` para en `test-compile`. En
  S129 funcionó porque `static/` estaba poblado por casualidad de esa misma sesión. La orden corregida son TRES
  pasos, y está escrita más abajo, probada hoy.
  D-GUION-EXIT-ENMASCARADO: SIETE INSTANCIAS EN UNA SOLA SESIÓN, SEIS DEL ARQUITECTO. Tres en el M0 (`^Results:`
  contra un Maven que prefija `[INFO]`; `^| $D` contra filas tachadas; un patrón de espaciado que ignoraba la
  exclusión que la propia ficha declara, y cuyo 214 pareció una regresión hasta aplicarla), una por recortar
  con `sed` la lista de redondeos justo antes del `ABORTA` que explicaba el vacío, una por delimitar la entrada
  de S129 con `^### ` sin ver que la anterior no tiene cabecera, una por escribir `S129` teniendo delante un
  documento que dice «Sesión 129», y un `||` de rescate en un guion de lectura —el corolario que esta misma
  sesión llevaba citando desde el M0—. Es el peor dato que la deuda tiene desde que nació, y el que importa no
  es el siete: es que el `||` se escribió DESPUÉS de haber contado cinco. Dos errores de medición de Claude
  Code, los dos declarados por él antes de que nadie los viese: un `grep -E` con retrorreferencia que `ugrep`
  no soporta y cuyo error se fue a la salida bajo un `||`, y un `(cond and A or B)[k]=1` con `A` vacío, que es
  falsy, mandando los 30 botones al cubo equivocado. R-deuda aguanta por décima vez: no bloquea el criterio.
  UNA AFIRMACIÓN NO MEDIDA, PROPAGADA POR LOS DOS. Se dijo «cuatro commits locales sin pushear» y era falso —el
  push había ocurrido entre turnos, `origin/main` estaba en `05abdb6`—, y el arquitecto lo repitió y dio una
  orden encima sin verificarlo. La misma familia que el resto de la sesión, en su forma más barata: repetir un
  número que otro afirmó.
  ARRANQUE DEL M4, LITERAL Y PROBADO HOY, que sustituye al de la entrada de S129: `mvn -pl solver install
  -DskipTests`, luego `mvn -pl app package -DskipTests` (este paso FALTABA y es el que dispara `npm ci`,
  `ng build` y la copia a `target/classes/static`), y luego `mvn -pl app spring-boot:run
  "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:/home/luis/desarrollo/educhronos/app/educhronos-s130-m4.db"`,
  con UN SOLO argumento, que es donde S120 se estrelló. Corolario: `spring-boot:run` NO reconstruye el
  frontend, así que tocar CSS durante un M4 obliga a repetir el `package` o el navegador seguirá viendo lo
  anterior.
  R4 / COSTURA. `scripts/verificar-cierre.py` daba un fallo duro al escribir esta entrada —dos cabeceras
  `### Sesión` vivas—, consecuencia directa de abrir S130 con S129 todavía viva, y lo descarga el M1-bis de
  aquí abajo. Los 27 sospechosos restantes son preexistentes (identificadores `D-B*`, `D-F7*`, `D-F8.*` que
  aparecen una sola vez); ninguno lo introdujo esta sesión. **e2e NO CORRIDO**: Playwright levanta su propio
  servidor y el puerto 8080 estaba ocupado por la aplicación del M4, que además corre contra la base de la
  sesión, así que `reuseExistingServer` habría contaminado el recorrido. Es lo PRIMERO que hace S131, con la
  aplicación parada.
  DEUDA BLOQUEANTE ABIERTA: pasa de 1 a 2. Sigue `D31-a` (las aulas de FPB) y entra
  `D-jerarquia-declarada-sin-aplicar`, que bloquea el criterio 3 de O-diseño por una razón sencilla: el
  criterio no puede darse por cumplido mientras `styles.css` afirme «aplicado» sobre algo que la medición
  desmiente.
  LIMPIEZA (M1-bis): archivada S128 a `bitacora-sesiones.md` (promovida a cabecera de sesión, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S129 a «Última sesión registrada (previa):»; S130
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S127 a S128.
  O-diseño (transversal, abierto en S121) ACTIVO: **C-tokens, C-sustitución y C-identidad HECHOS; C-revisión
  con el TRAMO 1 y el TRAMO 2 HECHOS y el TRAMO 3 A MEDIAS**. Criterios 1, 2 y 4 CUMPLIDOS; el 3 tiene la
  aplicación hecha y el juicio a medias. Los tramos 2 y 3 SE FUNDIERON, y la fusión se cumplió —el M4 de la
  aplicación fue el recorrido—; lo que no se cumplió fue su previsión, porque el recorrido encontró trabajo que
  no contemplaba. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro.
  Siguiente: **el resto del TRAMO 3**, sobre decisiones ya escritas y no sobre diseño nuevo. Lo fija su propio
  M0 (ver M1-ter).

### Sesión 131 — Higiene/Método: **el bucle de acabado visual pasa a Claude Code**. Nacen `M-respuesta` —el turno del modelo principal empieza por la decisión y el reparto, y si hay varios pasos por lado el reparto va como FLUJO NUMERADO en orden de ejecución— y `M-visual` —la sesión de acabado arranca en Claude Code con acta CERRADA, itera sobre `ng serve`, registra en `styles.css` y vuelve con un traspaso—, y M1-ter gana su primera excepción: para ese tipo de sesión el prompt SÍ lleva alcance, TRANSCRITO del registro y no decidido en el cierre. NO avanza el mapa: `docs/metodo.md` 323 → 413 líneas y ni un fichero de `app/` o `solver/` tocado. Dos commits, los dos de documentación.
  MOTIVO, DECLARADO POR EL ARQUITECTO Y NO DEDUCIDO. Dos problemas del procedimiento, no del producto. (1) El
  modelo principal escribe de más: el arquitecto declara que lee las conclusiones y las preguntas, y que con el
  resto hace de mensajero hacia Claude Code. (2) En los cambios de aspecto ese papel de mensajero era el propio
  ciclo de trabajo —medición a Code, respuesta a la web, otra medición, y al final un fichero que el arquitecto
  editaba a mano—, cuando quien tiene el servidor y puede enseñar el resultado en el navegador es Claude Code.
  QUÉ SE DECIDIÓ, y qué se descartó con argumento. El registro de lo visual va a `styles.css`, que es donde el
  criterio 4 de O-diseño lo verifica por grep desde S129, y NO a `diseno-navegacion.md`: ese documento es
  medición de geometría de O-navegación, y una segunda sede para la misma afirmación es la familia de
  `D-declarado-sin-artefacto`. Claude Code propone 2-3 variantes SOLO donde la decisión sea de gusto; si la
  regla ya está escrita, se aplica sin preguntar. Los hallazgos fuera del acta se anotan y no se ejecutan, que
  es R-terminado convertido en mecanismo después de que el recorrido de S130 partiera el tramo 3 por segunda
  vez. Y hay puertas de salida: si el trabajo toca un `.ts`, lógica o un tipo compartido, sale del bucle y
  vuelve al procedimiento normal con M3 y M4.
  LO QUE SE ACEPTA PERDER, dicho antes de perderlo. En Acabado visual desaparece el turno de contraste entre
  dos modelos sobre el contrato, que es el mecanismo que más errores de especificación ha cazado (dos en S79,
  tres en S81/S82/S83, cinco en S85). Se acepta AQUÍ Y SÓLO AQUÍ, porque en aspecto el oráculo fuerte es el ojo
  del arquitecto sobre la aplicación corriendo. En Desarrollo M4 sigue entero. Contrapeso escrito en M-visual:
  M2 no se omite —lo ejecuta Claude Code dentro del bucle— y su salida literal viaja al traspaso CON el comando
  que la produjo, porque sin eso el M1 del modelo principal sería narrativa sobre una sesión que no ha visto:
  la afirmación no medida y propagada que registra S130.
  UNA REGLA QUE NO SE RELAJA. `M-respuesta` acorta el RAZONAMIENTO, no lo que hay que juzgar: M2 sigue trayendo
  la salida literal de una medición sin interpretar. Viajar entera y no ser parafraseada de vuelta son cosas
  distintas, y confundirlas rompería el mecanismo que desmintió una suposición de apertura del arquitecto todas
  las veces en S75–S85.
  HALLAZGO DEL M1, Y ES DE M1-BIS. S130 dejó SIN HACER su paso M1.4: archivó S128 pero no extendió la crónica
  de archivado —terminaba en «la de S127 en la Sesión 129»— ni actualizó la frase de ventana, que seguía
  nombrando S128 como degradada y S129 como única H3 viva. Los dos censos de la bitácora sí se actualizaron, de
  modo que la incoherencia quedó entre los censos y la crónica, que es exactamente el contraste que
  `verificar-cierre.py` dice hacer desde S122. Por eso este M1-bis lleva DOS rotaciones de crónica y no una.
  VEREDICTO DEL VERIFICADOR sobre esa incoherencia, corrido ANTES de tocar nada con `python3
  scripts/verificar-cierre.py` y retorno capturado inmediatamente: **RETORNO 1**, y su §3 imprimió literalmente
  «censo 1 (cabecera 'sesiones de trabajo'): S128 / censo 2 (orden 'cronológico ascendente'): S128 / crónica
  del plan, última archivada: S127 / -> FALLO: no coinciden», con el resumen «comprobaciones duras con fallo:
  1». Si no la detectó, es instancia nueva de la familia «el instrumento mide otra cosa» y nace como deuda de
  método; si la detectó, lo que falló en S130 fue leer su salida. LA DETECTÓ, y con retorno distinto de cero:
  no nace deuda de método, y lo que falló en S130 fue leer la salida de su propio verificador.
  TIPO Y RITUAL: Higiene/Método. M1 + R4/R5, sin M2, M3 ni M4 de producto —no hay código—. La sesión no nombra
  Cambio, Objetivo ni Hito, y la tabla de Tipos la exime: registrarla igual sigue el precedente de S105, que
  tampoco avanzaba el mapa. Se registra además porque `metodo.md` marca tres veces «(S131)» y un número sin
  sesión registrada detrás es un identificador sin referente vivo.
  DEUDA: ninguna nueva de producto. O-diseño sigue ACTIVO con el tramo 3 de C-revisión a medias y
  `D-jerarquia-declarada-sin-aplicar` bloqueando su criterio 3; la deuda bloqueante del proyecto sigue en 2 con
  `D31-a`. La próxima sesión es la primera de Acabado visual y estrena M-visual sobre ese tramo.
  DEUDA NUEVA, y la encontró el propio guion de cierre al exigir que las rutas se localizaran en vez de
  suponerse: existen DOS `plan_trabajo_horarios.md`, el vivo en `docs/` y una copia antigua en `docs_extra/old/`.
  El M1-bis trabajó sobre el vivo —el que leen `verificar-cierre.py` y `regenerar-indice.py`— y la copia sigue
  con contenido caducado. Nace `D-plan-duplicado`: no bloquea nada, pero un `grep -r` no distingue una de otra
  y afirmar estado vivo desde la copia rancia es la familia de `D-tokens-inexistentes`.

### Sesión 132 — O-diseño (transversal): **C-revisión, TRAMO 3 APLICADO**, y primera sesión de **Acabado visual** bajo `M-visual`. El acta de cuatro puntos se agota entera en Claude Code: la jerarquía de acciones pasa a reglas globales sobre `button` y se retiran 32 bloques de 20 hojas, los 13 `<select>`, los 5 `input[type=number]` y los radios y casillas dejan de enseñar su adorno nativo, `--fuente-datos` estrena 9 usos y `.app__contenido` gana padding lateral. Cierran `D-jerarquia-declarada-sin-aplicar` y `D-controles-nativos-sin-neutralizar`, y la deuda bloqueante del proyecto baja de 2 a 1. Cuatro commits, 44 ficheros, +333/−243. **NO cierra O-diseño**: el criterio 3 queda con la aplicación completa y el juicio pendiente.
  TIPO Y RITUAL: Acabado visual. Acta heredada del M1-ter de S131, M2 dentro del bucle, M4 en navegador, M1
  aquí con el traspaso delante. Sin M0 —el acta viene del cierre anterior— y sin M3: no se tocó un solo `.ts`.
  El modelo principal NO vio la sesión; todo lo que este registro afirma sale del traspaso o de la
  documentación, y lo que no estaba en ninguno de los dos se pidió antes de escribirlo.
  QUÉ SE APLICÓ, un commit por punto del acta. `82ec04c`, punto 2: `.app__contenido` de `var(--e4) 0` a
  `var(--e4) var(--e5)`, sin variante porque la decisión 3 ya asigna `--e5`, asimétrico a propósito y con el
  porqué escrito en `app.css`. `50ad1d5`, punto 1: la jerarquía por relleno, 43 ficheros, 14
  `.accion-principal`, 9 `.accion-destructiva`, secundaria por defecto, `:disabled` como acento apagado, más
  los tres `font-weight` que decían jerarquía por peso contra la decisión 2. `9a38e03`, punto 3: sólo
  `styles.css`. `d7a427a`, punto 4: 9 ficheros.
  LA VARIANTE QUE SE OFRECIÓ Y LA QUE ELEGISTE. La regla global engorda el botón de fila —41 px hoy, 53 sin
  nivel de tamaño—, y ahí el alto es presupuesto medido. Elegida la B: `.accion-compacta` en las 16 acciones
  de fila, que las deja en 43. Es el único punto del acta que admitía variante; los otros tres tenían la
  regla escrita y se aplicaron.
  MEDICIONES, sobre la aplicación real en :4200 (`ng serve` + proxy) con el backend contra una copia de
  `educhronos-demo-m4.db` (md5 `ea1a70a…f48e9b`, verificado), que sirve el centro completo: 43 aulas, 100
  asignaturas, 208 actividades, 334 subgrupos y el horario 1 con 770 sesiones. `medir-recorte.mjs` a
  1920×887: 1ºA 2, 1B-A 4, 4ºA 3, 2B-B 0, sin desborde, idéntico en los cuatro commits y coincidente con lo
  que S127 cerró. `donde-corta.mjs`: 0 líneas cortadas a 1920 y a 1440 en el estado final.
  SUITES Y BUNDLE. `mvn clean test` → solver 91, app 282, BUILD SUCCESS. `npx ng test --watch=false` → 419 en
  49 ficheros, sin un solo caso reescrito. `npm run e2e` → 2 pasados, y vuelve a medirse: llevaba sin correr
  desde S130, que lo dejó pendiente por tener el 8080 ocupado. Bundle sobre `d7a427a`, atado a su commit como
  manda la lección de S129: **540,83 kB crudos / 122,17 transferidos**, contra 542,39 / 121,84 de `ffdf966`.
  El acabado CUESTA −1,56 kB crudos —los 32 bloques retirados— y +0,33 transferidos. Aviso en 550, no se
  alcanza.
  POR QUÉ O-DISEÑO NO CIERRA, contra la suposición con que se abrió este M1. El acta se agotó entera, pero el
  criterio 3 pide sistema APLICADO **y** verificado en navegador, y el único recorrido del registro es el de
  S130, que juzgó el estado anterior a estos cuatro commits y es quien los generó. Dar el criterio por
  cumplido porque la lista se acabó sería cerrarlo por agotamiento y no por juicio: el mismo argumento con
  que S129 partió C-revisión en tres tramos. Queda vivo exactamente una cosa, la pasada del arquitecto por
  las tres vistas y los tres diálogos sobre el estado actual.
  M-VISUAL, VEREDICTO DE SU PRIMERA SESIÓN: **funciona**. El acta aguantó sin desvíos, la variante sirvió
  para decidir y el traspaso trajo lo que el M1 necesita —commits con hash, ficheros, mediciones con su
  comando, suites, bundle atado a commit y hallazgos ya clasificados—. Pide tres ajustes, escritos en
  `metodo.md`: el acta fija QUÉ se toca y no CÓMO (el punto 3 pedía redibujar el adorno y la solución era
  `accent-color`); una decisión no escrita que no sea de gusto NO se toma en el bucle (la de
  `--color-info-fondo` la tomó Claude Code y la ratifica este cierre); y un punto que mueve geometría se mide
  en las DOS dimensiones. Y deja escrito un LÍMITE: el recorrido de juicio no puede ser una sesión de acabado
  visual, porque produce el acta en vez de consumirla.
  CORRECCIÓN DE AFIRMACIÓN VIVA (M2). El censo de botones que S130 escribió era corto. Remedido sobre
  `82ec04c`: 54 `<button>` en 24 plantillas y no «34 botones en 10 componentes»; caja y radio 10, correcto;
  una sola propiedad 10 y no 9; 15 clases sin regla y no 8 —faltaban `cancelar` de confirmar-generación,
  `grupos__pdc`, `grupos__tutoria`, `generar`, `confirmar-reemplazo__cancelar`, `jornada__recargar` y el
  botón sin clase de `panel-prevalidacion`—. Se corrige en las sedes VIVAS (la ficha de O-diseño y la de la
  deuda); la entrada de S130 se archiva tal como se escribió, porque lo archivado no se corrige.
  DEUDA. Cerradas dos: `D-jerarquia-declarada-sin-aplicar`, que era la que bloqueaba el criterio 3, y
  `D-controles-nativos-sin-neutralizar`. Actualizada una: `D-tokens-sin-uso` baja de tres tokens a uno
  —`--fuente-datos` se resuelve usándola en 9 sitios, `--color-ok-fondo` sale de la cola como decisión
  consciente y queda `--color-info-fondo`—. Nacen tres: `D-instrumento-criterio4-ciego-al-ancho`,
  `D-corte-lateral-a-1280` y `D-deuda-sin-sede-en-el-plan`. **Deuda bloqueante abierta: baja de 2 a 1**, sólo
  `D31-a`.
  DOS RECLASIFICACIONES SOBRE LO QUE PROPONÍA EL TRASPASO, y las dos con argumento. (1) El instrumento del
  criterio 4 no es ciego a lo que el criterio pide: el criterio pide «sin scroll VERTICAL» y eso es lo que
  mide. Lo que falta es instrumento para el ANCHO, y su sede es la sesión de Higiene/Método y no
  O-navegación, que está TERMINADO —colgarla de un objetivo cerrado es decidir que no se paga nunca—. (2)
  `D-corte-lateral-a-1280` cuelga de O-diseño, que es quien la introduce, por la misma razón.
  HALLAZGO DEL M1, y es de costura documental. `D-jerarquia-declarada-sin-aplicar`,
  `D-avisos-como-bloque-fijo`, `D-declarado-sin-artefacto`, `D-plan-duplicado` y el renombrado de
  `D-select-nativo-desparejo` NO tienen texto íntegro en la sección «Deuda consciente VIVA» de este
  documento, pese a que `gestion_proyecto.md` §4 declara que su fuente es este plan. La misma deuda tiene dos
  nombres según el documento que se lea. Nace `D-deuda-sin-sede-en-el-plan`; no se salda aquí (R-deuda), y
  las tres deudas nuevas de S132 nacen en LAS DOS sedes para no engordarla.
  R4 / COSTURA, medido al cerrar y no al abrir. `scripts/verificar-cierre.py`: 0 comprobaciones duras con
  fallo, invariante H3 en 1, los tres censos en S130 y los dos índices con 0 entradas descuadradas. Tokens
  sospechosos: **26, medidos sobre `e03a243`, contra los 30 con que la sesión llegó
  al M1**. El número va atado a su commit y no a la sesión, que es la lección del bundle de S129 aplicada a
  otro instrumento: cada edición del corpus lo mueve, y este párrafo hubo que corregirlo DOS veces por
  anotarlo antes de terminar de editar —la segunda, después de haber escrito la corrección de la primera—.
  LA HIPÓTESIS QUE ESTE CIERRE ESCRIBIÓ SOBRE ESOS TRES ERA FALSA, y se corrige aquí en vez de dejarla. Se
  supuso que los tres de más eran deudas recién nacidas citadas una sola vez; medido comparando las dos
  salidas del mismo verificador, **las tres deudas de S132 nunca estuvieron en la lista** —nacieron en las dos
  sedes, así que arrancan con 4, 8 y 6 apariciones—. Los que desaparecen son `D-F7`, `D-F8`, `D-F8.5` y
  `D-controles-nativos-sin-neutralizar`. De ése último la causa está MEDIDA: pasa de 1 a 8 apariciones porque
  el renombrado del M1-bis le dio por fin texto en el plan, que es `D-deuda-sin-sede-en-el-plan` cobrándose
  por el lado bueno. De `D-F7`, `D-F8` y `D-F8.5` la causa **queda sin determinar** y se escribe así: el grep con que se
  intentó se contamina con subcadenas (`D-F8` casa dentro de `D-F8.5`) y no es el tokenizador del script, de
  modo que concluir con él sería la familia de S122. No se persigue (R-deuda: no bloquea), y se registra que
  el número bajó sin que tres cuartos de la bajada tengan explicación.
  DOS INSTANCIAS QUE PRODUCE ESTE PROPIO M1. (1) `D-guion-exit-enmascarado` vuelve a morder en su segunda
  forma, la de S123: tras el primer guion el índice de `gestion_proyecto.md` quedó con 14 entradas
  descuadradas y `verificar-cierre.py` salió con **retorno 0**, porque su §4 imprime las descuadradas y no las
  suma a `problemas`. Lo detectó Claude Code por leer la salida y no el código de retorno, que es exactamente
  la lección que la ficha lleva escrita desde S123 y que sigue sin pagarse. (2) Los guiones de este cierre
  dejaron los respaldos `.bak-s132` DENTRO de `docs/`, que es literalmente la forma de `D-plan-duplicado`:
  copias caducadas de un documento vivo dentro del repo, que un `grep -r` lee igual que a las vivas. El fallo
  es de `M-doc`, que decía «hace copia de seguridad» sin decir dónde; queda corregido allí, junto con el paso
  de regenerar el índice que los guiones tampoco traían y hubo que suplir a mano tres veces.
  LIMPIEZA (M1-bis): archivada S130 a `bitacora-sesiones.md` (promovida a `### Sesión 130`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S131 a «Última sesión registrada (previa):»; S132
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S129 a S130.
  O-diseño (transversal, abierto en S121) ACTIVO: **C-tokens, C-sustitución y C-identidad HECHOS; C-revisión
  con los TRAMOS 1 y 2 HECHOS y el TRAMO 3 APLICADO Y SIN JUZGAR**. Criterios 1, 2 y 4 CUMPLIDOS; el 3 con la
  aplicación completa y el juicio pendiente. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado
  por el correo al centro. Siguiente: **el recorrido de juicio del criterio 3**, que lo hace el arquitecto y
  cuya salida es el acta de la próxima sesión de acabado. Lo fija su propio M0 (ver M1-ter).

### Sesión 133 — O-diseño (transversal): **RECORRIDO DE JUICIO DEL CRITERIO 3, y el objetivo CIERRA**. El arquitecto recorre la aplicación levantada sobre el centro real en seis paradas; las cuatro de producto salen limpias y las dos observaciones caen fuera del criterio. El acta de la siguiente sesión de acabado sale VACÍA, y por tanto esa sesión no existe. Cierra C-revisión y con él O-diseño, abierto en S121. Nacen dos mejoras futuras de propuestas del arquitecto y se corrige D18 de `diseno-navegacion.md`, que afirmaba un rótulo inexistente. Ni un fichero de `app/` o `solver/` tocado.
  QUÉ SE JUZGÓ Y SOBRE QUÉ, para que el veredicto sea reproducible. Commit `4ba1fd4` con el árbol limpio,
  bundle servido por `mvn -pl solver install -DskipTests` + `mvn -pl app package -DskipTests` +
  `spring-boot:run` contra `app/educhronos-s133-m4.db`, copia de `educhronos-demo-m4.db` con md5
  `ea1a70a0337831dddccdbcd322f48e9b` verificado ANTES y DESPUÉS: la base de referencia queda intacta. Firefox
  maximizado a 1920x1080, zoom 100 %. Vehículo el BUNDLE y no `ng serve`, decidido con argumento: en el
  recorrido no se itera, así que `ng serve` sólo habría aportado su riesgo de servir un bundle rancio (S124).
  SEIS PARADAS: landing, barra de aplicación, los ocho destinos de configuración —Actividades a fondo por ser
  la lista más densa, las otras seis en pasada de divergencias, Jornada aparte por no tener forma de lista—,
  los estados de lista, formularios y diálogos, y la vista de horario en sus tres ámbitos.
  VEREDICTO: CRITERIO 3 CUMPLIDO, y con él O-diseño ENTERO. Las paradas 3, 4, 5 y 6 salen limpias, incluidas
  las dos cosas que S132 aplicó y nadie había juzgado: la jerarquía de acciones por relleno y la
  neutralización de los controles nativos. Las suites NO se recorren y no es un atajo: el guion de cierre
  comprobó que entre el último commit de producto de S132 y `4ba1fd4` no hay un solo fichero de `app/` ni de
  `solver/`, así que las 91 + 282 de Maven y las 419 de vitest medidas entonces siguen midiendo este árbol.
  LO QUE NO SE VERIFICÓ, dicho para que la entrada no se lea más completa de lo que fue. El estado CARGANDO
  de las listas, por transitorio y no sostenible a mano; y el estado VACÍO, inalcanzable con el centro
  completo cargado sin fabricar datos. Los dos son ramas del componente compartido de estados, y las otras
  dos —sin coincidencias y error de acción— sí se comprobaron.
  EL ERROR DEL GUION DEL RECORRIDO, Y ES DEL MODELO PRINCIPAL. El guion mandó buscar en la landing la leyenda
  de la insignia de coste blando, que no vive allí: vive en la propia insignia de la rejilla, como par
  `title` + `aria-label`, desde que S128 cerró `D-insignia-sin-leyenda`. El arquitecto la buscó, no la
  encontró y lo reportó como hallazgo. Es la causa raíz que la familia de `D-guion-exit-enmascarado` nombra
  desde S129 —escribir de memoria teniendo el documento a tres secciones de distancia— aparecida en un
  instrumento nuevo: un guion de recorrido humano en vez de un guion de shell. Un recorrido que manda buscar
  lo inexistente produce dos falsos: el que no está y el que se deja de mirar mientras se busca. Corolario
  escrito en `M-visual`.
  LA RESERVA DEL SELECTOR DE CURSO: LA APLICACIÓN ESTÁ BIEN Y EL DOCUMENTO DECÍA DOS COSAS FALSAS. El
  arquitecto reportó que no ve reserva alguna a la derecha de la barra. Medido sobre `app.html` y `app.css`:
  el hueco `app__curso` EXISTE, está vacío a propósito y reserva ALTO —`min-height` de un interlineado, ~27
  px—, no ancho, y no lleva rótulo. D18 de `diseno-navegacion.md` decía «rotulado como reserva» y justificaba
  la decisión con que «el ancho de la barra se reparte una vez»: ninguna de las dos describe lo construido.
  Se CORRIGE allí y NO nace deuda, porque la corrección es la deuda saldada. La consecuencia, en positivo y
  escrita para quien llegue a la Fase 10: como lo reservado es el alto, añadir el selector NO hará crecer la
  barra, que es exactamente lo que protege el presupuesto de la rejilla —51 px bajo la restricción «puede
  encoger, no crecer» del criterio 4 de O-navegación—. Lo que no está reservado es el ancho.
  DEUDA NUEVA: DOS MEJORAS FUTURAS, de propuestas del arquitecto durante el recorrido, y ninguna es acabado.
  `D-tutor-invisible-en-grupos` y `D-plazas-ocultas-solo-al-arrastrar`, con texto íntegro aquí y
  clasificación en `gestion_proyecto.md` §4. Las DOS sedes a propósito: escribirlas en una sola es
  exactamente lo que `D-deuda-sin-sede-en-el-plan` reprocha a S130 y S131.
  UN CASO DE `D-tokens-inexistentes` RESUELTO POR ADELANTADO, sin abrir su sesión. La medición de la
  propuesta del tutor destapó que el javadoc de `GrupoDTO.java` afirma que el tipo «siempre será ORDINARIO»
  citando `D-nueva-2`, y el TSDoc de `grupo.model.ts` ya registra lo contrario: `GET /api/grupos` hace
  `findAll()` sin filtrar, y el frontend discrimina acciones por tipo, lo que sólo tiene sentido si llega más
  de un valor. No se toca —R-deuda, y tirar del hilo es mapear las nueve citas, que su ficha declara trabajo
  propio y no arreglo en caliente—, pero la sesión de Higiene/Método hereda uno de los nueve contextos ya
  leído y con veredicto.
  TIPO Y RITUAL: M0 + M4 en navegador + M1. La tabla de Tipos NO tiene fila para el recorrido de juicio y no
  se inventa una en el cierre; lo que sí está escrito es su límite en `M-visual` desde S132, y se cumplió: la
  sesión no arrancó en Claude Code, porque la lista era su salida y no su entrada.
  MAPA: **O-diseño TERMINADO (S133)**, con sus cuatro criterios cumplidos y C-revisión cerrado en sus tres
  tramos. O-demo (H2) sigue ABIERTO y sin trabajo ejecutable, bloqueado por el correo al centro; deuda
  bloqueante del proyecto: 1 (`D31-a`). Con O-diseño fuera, **O-ajuste-cierre queda disponible** y es el
  único objetivo vivo con deuda que bloquea su propio criterio. La elección la hace el M0 siguiente (M1-ter).
  UN DEFECTO DE M-doc DESTAPADO POR LA PROPIA GUARDA, y no por un fallo consumado. El guion de cierre abortó
  en su primera pasada con dos anclas no únicas, `### Sesión 132` y `#### O-diseño`: la segunda aparición de
  cada una es su entrada del ÍNDICE GENERADO. No era ambigüedad del cuerpo, era el índice citando el
  encabezado. Lo resolvió Claude Code anclando las dos a principio de línea, sin tocar el texto insertado ni
  la semántica. Afecta a TODOS los cierres, porque el M1 siempre inserta antes de la cabecera H3 viva: queda
  escrito en `M-doc` como punto 6, junto al 5, que cubría regenerar el índice pero no que el índice hace
  fracasar las anclas. La guarda funcionó: paró el guion con los cinco ficheros intactos.
  LIMPIEZA (M1-bis): archivada S131 a `bitacora-sesiones.md` (promovida a `### Sesión 131`, insertada al
  final en orden ascendente, cuerpo íntegro); degradada S132 a «Última sesión registrada (previa):»; S133
  queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S130 a S131.

### Sesión 134 — O-demo (H2): **ENTREVISTA EN VIVO CON EL JEFE DE ESTUDIOS. `D31-a` SALDADA y la deuda bloqueante del proyecto baja a CERO por primera vez desde que existe el mapa.** El correo redactado no llega a enviarse: el arquitecto sienta al jefe de estudios delante y la consulta se convierte en tres rondas de preguntas. Se cierran `D31-a` (las aulas de FPB), la parte (a) original de `D31` y media `D-gh6-tutor-contradictorio`; se REFUTA `D31 (b)`; y un tercer PDF aportado en la propia entrevista resuelve 21 de los 28 tutores. Nacen siete deudas y ninguna abre sesión. Ni un fichero de `app/` o `solver/` tocado.
  TIPO Y RITUAL: recogida de requisitos. **No hay fila en la tabla de Tipos que la describa**, igual que le pasó al recorrido de juicio de S133: dos seguidas. Se anota como observación de método y no como deuda; un tercer caso sí sería patrón. Sin M2, M3 ni M4: no hubo código que medir ni que mutar.
  M0 IRREGULAR, y se registra como tal. La sesión se abrió para hacer el M0 de O-ajuste-cierre y NUNCA llegó a fijar alcance: la condición que bloqueaba O-demo —la respuesta del centro— se cumplió en vivo a mitad de turno. Lo que sí produjo esa apertura fallida es una medición que sigue pendiente de aplicar: **`D-F8.6-ii-b` está CERRADA desde S83** (bloque 8.6-iii-B1 marcado `[x]`, la propia ficha de la deuda dice «→ CERRADA en S83» y la bitácora de S83 lo confirma; S84 le añadió cobertura de test), y `gestion_proyecto.md` la afirma VIVA Y BLOQUEANTE en tres sitios: la fila de H1 en §2, su fila de §4 y el «Terminado cuando» de la ficha de O-ajuste-cierre. Ese objetivo NO estaba bloqueado por deuda alguna; su problema es que su criterio está escrito contra una deuda muerta. La corrección NO se hace en esta sesión.
  LO QUE EL CENTRO RESPONDIÓ, y qué cierra cada cosa. **(1) Aulas de FPB (`D31-a`)**: lo que el horario no imprime va al **Taller 4** (1º FPB) y al **Taller 5** (2º FPB), uso exclusivo de FPB y tutoría incluida. Es una REGLA, no once datos sueltos, y corrobora por contraste lo medido: las únicas horas de FPB con aula impresa son CS/CyS en TALL3, y las cuentas cierran exactas (24 + 6 = 30 en 1FPB, 25 + 5 = 30 en 2FPB). El oráculo de S117 queda listo para cobrarse: la tabla `sesion` debe pasar de 770 a **819** filas, delta 49 = 24 + 25. **(2) Tutores**: el tutor se introduce a mano y es independiente de que exista hora de tutoría —los bachilleratos tienen tutor y no tienen TUT—, luego la heurística «tutor = quien imparte TUT» era falsa en los DOS sentidos y lo cargado hoy es incorrecto, no incompleto. El modelo ya lo preveía (`requiere_tutor` como flag de actividad, no nombre de asignatura) y no hay que cambiarlo: lo que falla es la derivación. **(3) Estructura de 4º ESO**: cuatro itinerarios cerrados, dos materias de modalidad de 3 h por alumno, sin cambio de itinerario a mitad de curso.
  EL HALLAZGO DE LA SESIÓN, y contradice la frase del propio jefe de estudios con su propio ejemplo. Preguntado por si los alumnos se recombinan entre franjas, responde que sí, pero el ejemplo que da demuestra lo contrario: FQ se imparte a la vez por DOS profesores (FIS3 y FIS4) y el reparto está alineado con la franja siguiente —los de FIS3 van a Biología, los de FIS4 van a TEC—. Es decir, parten FQ en dos precisamente para que cada mitad case con su itinerario. Con el itinerario cerrado y sin cambios durante el curso, **el conjunto que se mueve junto es el itinerario y SÍ se reutiliza entre bloques**: exactamente una actividad con dos plazas, cada plaza con su subgrupo. **`D31 (b)` queda REFUTADA**, no confirmada: las optativas de 4º NO son población propia por bloque, como se asumió en S28. Confirmado por él a la pregunta directa.
  TRES CAPAS MEDIDAS EN 4º ESO, sobre los conjuntos de grupos que acompañan a cada asignatura en el PDF de profesores (dato inequívoco; el encaje día/tramo NO se da por bueno, exige la extracción por geometría de S116). **Modalidad → `{4ºA, 4ºB, 4ºD}`, nunca 4ºC**: FQ (Carrasco y Marín), Biología (Crespo), TEC (Redondo), DIG (Barba), FOPP (Fuentes). **Latín y Economía → `4ºC` entero**, clase de grupo completo y no bloque. **Optativas de 3 h y de 2 h → los cuatro grupos**, y Religión/ATEDU además con 4ºADi y 4ºDDi. La consecuencia es una TERCERA forma de partición que el modelo no contempla: el itinerario de Humanidades/CCSS no se resuelve con un bloque sino METIENDO A ESOS ALUMNOS EN UN GRUPO. Nace `D-itinerario-como-grupo`. De paso queda medido que 4º tiene CUATRO grupos —Religión de 4º cubre A, B, C y D—, así que «los 3 4º» que dijo el jefe de estudios fue un lapsus.
  EL TERCER PDF, y lo que trae que nadie esperaba. `Horarios de profesores.pdf`, aportado en la entrevista después de que el centro dijera no tener la lista de tutores, la trae IMPRESA en la cabecera de cada página. Medido sobre el fichero, no leído por encima: **80 profesores, 17 con línea `Tutor:`, que cubren 21 de los 28 grupos** —los 16 ordinarios de ESO y FPB más los 5 de diversificación—. Y contradice dos respuestas del propio centro: Pilar Guerrero Serrano figura como tutora de CINCO grupos PDC y los cinco Di llevan DOS tutores, el de su grupo padre y ella. Confirmado por el arquitecto ante el jefe de estudios: **el caso de diversificación es especial y la orientadora puede ser tutora de todos los grupos de diver**, luego la regla «un profesor, un grupo» rige para los ordinarios y no para los PDC, y la figura del co-tutor SÍ existe ahí. Contesta de rebote la pregunta que no llegó a responderse.
  LO QUE EL TERCER PDF NO TRAE: ni una sola línea `Tutor:` para los siete grupos de Bachillerato, que son justo los que tienen tutor sin hora de tutoría. Y el sustituto que ofrecía el modelo tampoco vale: el **Hallazgo E** de `modelo_datos_fase1.md` afirma que PTVE lo imparte el tutor de cada grupo, y Mejías Márquez da PTVE a 1B-B y a 1B-D y PTEV a 2B-A y 2B-B juntos. Con la respuesta del centro sobre GH6 —«no es tutor de 1º Bach A»—, el Hallazgo E acumula DOS afirmaciones que los datos o el centro desmienten. Nace `D-hallazgo-E-refutado`.
  DEUDA. Cerradas: `D31-a`; `D31 (a)` original —el refuerzo/ATED de 3º ESO queda validado por respuesta directa («la optativa es elegida para todo el curso») y no ya por analogía como en S69—; y `D-gh6-tutor-contradictorio` a medias, con la mitad OPUESTA a la supuesta. Refutada: `D31 (b)`. Nacen siete: `D-hallazgo-E-refutado`, `D-tutores-bachillerato`, `D-nombres-sin-codigo`, `D-censo-profesores-80-59`, `D-itinerario-como-grupo`, `D-taller5-inexistente` y `D-fuente-tercera-sin-usar`. **R-deuda ratificado en su caso más fácil**: siete deudas de golpe y ninguna abre sesión.
  DOS DISCREPANCIAS DE `metodo.md` CON LA PRÁCTICA, anotadas y no tocadas: M1.2 dice que el plan conserva las CUATRO últimas cabeceras y la práctica desde hace sesiones conserva DOS (una H3 viva y una previa); y la tabla de Tipos no tiene fila para las dos últimas sesiones. Ninguna bloquea; van a la lista de la sesión de Higiene/Método.
  Y UNA NORMA MUERTA EN EL PROMPT DE APERTURA: la NORMA DE DOCUMENTACIÓN del prompt («pide los ficheros y devuélvelos enteros») fue DEROGADA por `M-doc` en S122 con la medición delante. El cierre se hace por guion para Claude Code, como manda `metodo.md`.
  LIMPIEZA (M1-bis): archivada S132 a `bitacora-sesiones.md` (promovida a `### Sesión 132`, insertada al final en orden ascendente, cuerpo íntegro); degradada S133 a «Última sesión registrada (previa):»; S134 queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S131 a S132. R4/costura: sin código en el árbol; los siete tokens nuevos nacen con definición viva en el plan y citante vivo en `gestion_proyecto.md` §4.
  O-demo (H2, abierto en S115) ACTIVO y **DESBLOQUEADO**: `D31-a` saldada, trabajo ejecutable por primera vez desde S120. Falta cargar las 11 actividades de FPB —antes hay que dar de alta el aula Taller 5, que no existe en el catálogo— y declarar el centro completo contra el oráculo de 819 filas. O-ajuste-cierre (H1) sigue vivo, sin deuda que lo bloquee y con su criterio pendiente de reescribir. **Deuda bloqueante del proyecto: 0.**
### Sesión 135 — O-demo (H2): **EL CENTRO REAL ESTÁ COMPLETO Y GENERA HORARIO. Las 11 actividades de FPB cargadas, 819 sesiones clavando el oráculo de S117, y las TRES CAPAS de S119 en verde con delta CERO.** Primer trabajo ejecutable de O-demo desde S120. Se crea y se cierra el Cambio `C-centro-completo`: alta del aula Taller 5, aula fija en las 11 plazas, carga del centro entero DESDE BASE VACÍA (816 escrituras previstas, 816 escritas) y horario válido al primer intento. Se cierra `D-taller5-inexistente`, se paga de paso una segunda instancia de `D-plan-duplicado` y la capa 2 deja de ser arnés desechable. Cuatro commits; ni un fichero de `app/` ni de `solver/` tocado.
  TIPO Y RITUAL: Saneamiento/herramienta — M0 + M2 en cinco pasadas + M1. Sin M3 (no hay lógica de producto que mutar) y sin M4 (no cambia ninguna vista): lo escrito son dos guiones de `tools/` y el catálogo derivado. ROMPE la racha de S133 y S134: esta sesión sí tiene fila en la tabla de Tipos, así que la observación de método de aquéllas se queda en dos casos y NO asciende a patrón.
  M0. Cambio `C-centro-completo`, Objetivo O-demo, Hito H2. El Cambio NO existía en la ficha —la lista se cerró en S115 con cinco piezas, todas hechas o retiradas— y crearlo fue trabajo de esta apertura, precedente escrito en la propia ficha (el M0 de S115, como el de S107 con O-estructura). R-invalidación: NINGUNA, las 11 entran de la misma forma que las 208 y nada planificado las rehace. Dos vallas fijadas en el M0 y respetadas: no se toca 4º ESO (`D-itinerario-como-grupo`, candidata a O-particiones) y no se rederiva el catálogo desde los once PDF nuevos (`D-fuente-tercera-sin-usar`). El M0 también midió la trampa que habría invalidado el oráculo: `POST /api/horarios` es alta pura y ACUMULA (`D-horario-irreversible`), así que generar sobre `educhronos-demo-m4.db` —que ya tiene el horario de S118— habría dado 1589 filas y no 819. De ahí que se cargara desde vacío.
  LA PROCEDENCIA DEL DATO, Y SUS RESERVAS, ESCRITAS ANTES DE COBRARLO. Las aulas de FPB son palabra del jefe de estudios en la entrevista de S134 y NO tienen segunda fuente. La sesión extrajo los ONCE PDF del centro a texto y midió: **Taller 5 no aparece en ninguno de los once**, y Taller 4 sí es una de las 43 aulas de `Horarios de aulas.pdf` pero **con la rejilla completamente en blanco**, igual que Taller 2. Lo único que hay es corroboración POR CONTRASTE —el volcado imprime aula sólo en las 11 celdas de FPB que van a Taller 3 y deja las otras 49 en blanco—, y eso es compatible con la regla sin probarla: cualquier par de aulas de uso exclusivo de FPB produciría el mismo volcado. **La medición no contradice la regla; tampoco la prueba.** Lo que sí aporta, y era lo que hacía falta saber antes de cargar, es que ambas aulas están LIBRES, luego asignarles 24 + 25 horas no colisiona con nadie. Aritmética verificada: 1FPB = 24 sin aula + 6 en Taller 3 = 30; 2FPB = 25 + 5 = 30.
  LO QUE SE ESCRIBIÓ, y por qué fueron OCHO puntos de edición y no dos. El parche del catálogo (`beb044a`) va en guion propio, `tools/carga-centro/aplicar-regla-aulas-fpb.py`, mismo patrón que `extraer-nombres.py`, para que el dato de entrevista quede con procedencia ejecutable y no como edición manual de un JSON de 219 actividades: alta de Taller 5, `aulaFija` en las 11 plazas marcadas `_aulaDesconocida`, `_meta` de 43 a 44 aulas y de 815 a 816 envíos. Tres defectos del guion los cazó Claude Code en corrida EN SECO antes de escribir —reescribía el fichero entero (24 877 líneas de diff para un cambio de 100), dejaba `usadaPorCatalogo` mintiendo en dos aulas y metía un `.bak` dentro del repo— y se corrigieron: `indent=1` medido del original en tiempo de ejecución, `usadaPorCatalogo` RECALCULADO para las 44 (validado antes reproduciendo las 43 respuestas del original) y sin respaldo dentro del árbol. Diff final: 36 inserciones, 48 borrados, un fichero.
  LA GUARDA QUE PROTEGÍA EL ESTADO VIEJO SE CONVIERTE EN EL OBSTÁCULO DEL NUEVO, y es el hallazgo de método de la sesión. `son_las_11_esperadas` exigía exactamente 11 violaciones de prevalidación; con el catálogo parcheado hay 0, luego `--cargar` abortaba sin enviar nada. Se reescribe como `prevalidacion_limpia`, que exige CERO violaciones sean del XOR o de lo que sean —descartada la alternativa de derivar la expectativa del propio catálogo, porque toleraría un marcador superviviente en vez de rechazarlo—, y con ella el salto de las 11 se retira entero por inalcanzable. Verificada por MUTACIÓN, no por lectura: con una plaza de FPB sin aula aborta con rc 1, y con un profesor inexistente TAMBIÉN, que es lo que la guarda vieja sólo detectaba de rebote si alteraba el recuento. La red no se adaptó, se mejoró. **El punto que ningún `grep` de la sesión encontró:** `ESCRITURAS_CARGA_COMPLETA = 804`, que no contiene ni el marcador ni el literal 11 y habría hecho que `main` devolviera 1 sobre una carga correcta. Pasa a 816 y se mantiene A MANO a propósito, con el porqué escrito en el fichero: derivada del catálogo sería una tautología incapaz de detectar un error del catálogo. `4d11a49`, 60 inserciones y 51 borrados.
  LA CARGA, Y LA CONVERGENCIA DE DOS CUENTAS QUE LLEVABAN DIVERGIENDO DESDE S116. Se carga sobre `app/educhronos-s135.db`, base NUEVA y VACÍA, y no incremental sobre copia de la canónica: cargar desde cero demuestra en un solo acto lo que el criterio pide y es lo único que ejercita de verdad la constante nueva. Esquema creado sobre fichero vacío (21 tablas a cero), 816 escrituras previstas y **816 escritas, sin un solo desajuste en las diez familias**. Conteos por GET (vías del producto) idénticos a los de SQL: 44 aulas, 219 actividades, 316 plazas, 334 subgrupos, 28 grupos. El reparto de las 11 leído en la base —Taller 3 → 2 plazas (CS y CyS, las que el volcado sí imprimía), Taller 4 → 6, Taller 5 → 5— materializa la regla del centro. Idempotencia reverificada: la segunda pasada envía sólo los 28 PUT de tutoría y el informe lo clasifica como corrida idempotente, con 0 altas. Y el aviso de huérfanas NO se imprime: 0 asignaturas y 0 profesores sin uso, cuando antes eran 9 y 3. Ese silencio es la cicatriz de la omisión cerrándose. **Las 816 coinciden exactamente con `_meta.enviosDeFormulario.TOTAL`**: antes divergían —815 documental frente a 804 del cargador— porque `_meta` contaba las 219 del tecleo manual y el cargador enviaba 208; convergen por primera vez, y esa coincidencia es en sí una comprobación de que las dos cuentas hablan ya del mismo centro.
  LA GENERACIÓN, AL PRIMER INTENTO Y CONTRA PRONÓSTICO. `HTTP 200` en 601 s, `FEASIBLE`, objetivo 203.0, `sesion` **819**, un solo horario. El M0 avisó de que el riesgo subía: S117 midió 3/4 de éxito a 600 s con 26 grupos a holgura cero, y ahora son **28 de 28** porque las 49 horas nuevas llenan precisamente los dos que tenían holgura. Salió a la primera. El oráculo de 819 no se heredó de S117: se REDERIVÓ del catálogo de hoy —Σ(repeticiones × plazas) sobre las 219— y de paso reprodujo 770 sin las 11, con delta 49 = 24 + 25, así que la cifra la sostienen dos vías independientes y no una cita.
  LAS TRES CAPAS DE S119, REMEDIDAS SOBRE EL CENTRO COMPLETO. **CAPA 1, validez formal: cero violaciones de regla dura sobre 819 sesiones** (`VerificadorSolucion` releyendo las filas persistidas) y los 28 grupos a 30/30 sin excepción, con 1FPB y 2FPB teniendo horario por primera vez. **CAPA 2, conservación de la carga: 526 claves, CERO divergentes, 1301 = 1301, delta 0.** Es el resultado que convierte «completo» en «correcto»: el recuento de 819 demuestra que hay once actividades colocadas, no que sean las once que el centro imparte, y esta capa cruza terna a terna (grupo, asignatura, profesor) contra los 28 `grupo-*.json`. En S119 divergían 11 con delta 49; hoy no diverge ninguna. **CAPA 3, calidad: 188 + 0 + 15 = 203, la identidad exacta**, recompuesta por código distinto del que construyó el modelo, con los tres pesos verificados a 1 en el fuente (`ModeloCpSat:69,80,104` y `ensamblarObjetivo:296-301`). Se mide y NO se corrige (R-terminado). **ADVERTENCIA QUE NO SE HEREDA SINO QUE SE REMIDIÓ:** el `indispBlanda = 0` es VACUO —`profesor_restriccion_horaria`, `sesion_bloqueada` y `aula_bloqueada` están las tres vacías en ESTA base—, y las 479 penalizaciones son contrafactuales por celda (suma de deltas −86), no una descomposición del total. **EL 203.0 NO ES COMPARABLE CON EL 192.0 DE S118**, y leerlo como regresión sería el error: son instancias distintas, 49 sesiones y dos aulas más. Dato que lo confirma: las consecutivas BAJAN de 18 a 15 mientras las ventanas suben de 174 a 188; una degradación no bajaría ninguno de los dos términos.
  EL INSTRUMENTO DE LA CAPA 2 DEJA DE SER DESECHABLE, y su validación es el mejor resultado metodológico de la sesión. S119 lo declaró arnés y hubo que reconstruirlo desde la especificación en prosa de la bitácora, que es la SEGUNDA vez que se paga ese peaje. Queda versionado en `tools/carga-centro/verificar-conservacion.py` (`e09ba3a`, 264 líneas, `--db` obligatorio sin defecto para obligar a decir sobre qué base se mide, salida 0 sólo con cero divergentes, cero supuestos rotos y mapa verificado). **No vale porque diga «cero»: vale porque discrimina.** Corrido contra la m4 reproduce las ONCE divergencias de S119 una a una, con su terna y su repetición exacta (AMO/PAU2 6, CA/TEC1 4, IPE/FOL3 3, MECSO/PAU2 5, PS/PAU2 5, Tut/PAU2 1, CA/FIS3 4, ELE/PAU1 7, MEC/PAU1 11, PI/FOL3 2, Tut/PAU1 1), obtenidas hace dieciséis sesiones por código que ya no existe. El mapa de códigos de grupo está escrito A MANO y el comentario más largo del fichero dice por qué, con un NO SUSTITUIR ESTAS REGLAS POR UNA DERIVACIÓN AUTOMÁTICA: en S119 se dedujo maximizando el solapamiento de (asignatura, profesor), que es el dato que la capa compara después, y la circularidad era peor justo en 1FPB y 2FPB (0.14 y 0.17). Decisión de diseño que la especificación no traía y hubo que tomar: se cuentan entradas DISTINTAS a ambos lados, porque una plaza que agrupa dos subgrupos del mismo grupo imprime UNA celda en el PDF; que la m4 dé 1252 exacto valida la decisión contra un resultado previo obtenido sin ella a la vista. Lo que el instrumento declara NO aseverar: no valida el solver, no dice nada de la colocación y no compara aulas.
  UNA SEGUNDA INSTANCIA DE `D-plan-duplicado`, PAGADA EN EL CAMINO. `INFORME-RECONCILIACION.md` existía DOS veces —`docs/` y `docs/horario-referencia/`— y la nota de cierre de S119 que resuelve `Nº ESO PDC → 3ºCDi` había aterrizado en la copia huérfana. Cadena del fallo, reconstruida por `git`: `0023c8f` anunció «se mueven a horario-referencia» y COPIÓ (275 inserciones, cero borrados), y `62ad6a2`, cuyo mensaje dice `docs(horario-referencia)`, escribió en `docs/`. Resultado: durante dieciséis sesiones la copia que acompaña a los datos —la que un lector abre primero— siguió diciendo «correspondencia incierta» y «estas celdas se EXCLUYEN de los cruces» sobre un caso cerrado en S115 por tres vías. Se paga AQUÍ y no en la sesión de Higiene porque cae de lleno en el camino: el instrumento de la capa 2 necesita ese mapa, y escribir en el código la sexta regla mientras su fuente la niega es la enfermedad, no el arreglo. Unificado en `494554e`: la canónica es `docs/horario-referencia/`, recibe la nota (+9 líneas, cero borrados) y la copia de `docs/` desaparece; cero citas huérfanas. **La tabla de normalización NO se toca a propósito** —el informe declara que su cuerpo es entregable de una extracción determinista y no se reescribe—, así que el mapa sigue siendo cinco reglas de la tabla más la sexta de la nota, y ahora las seis viven en el mismo fichero.
  LAS BASES, DECISIÓN DE ARQUITECTO gemela de la que S119 tomó con la m4. `app/educhronos-s135-centro-completo.db` (`dfb30839…`, `sesion` 0) pasa a ser la BASE DE REFERENCIA DEL CENTRO, porque es la única del árbol con las 219 actividades y sin horario, que es lo que O-particiones necesita como punto de partida. `app/educhronos-s135.db` (`e072383a…`) es el HORARIO DE REFERENCIA de S135. `app/educhronos-demo.db` (`f5b542eb…`, intacta y verificada antes y después) DEJA de ser canónica: contiene un centro incompleto de 208 actividades y seguir llamándola así induce a error. **No se renombra ningún fichero**: renombrar rompería la trazabilidad con las actas que ya los citan por nombre. Nota técnica actualizada abajo.
  DEUDA. **Cerrada: `D-taller5-inexistente`** (el alta va en `beb044a`; nació en S134 exactamente para esto). Pagada de paso: la segunda instancia de `D-plan-duplicado`. Nacen DOS: `D-aula-tipo-sin-uso-real` y `D-guion-busca-token-esperado`. Se afina `D-fuente-tercera-sin-usar`: los once PDF están ahora extraídos a texto y consultados para una pregunta concreta, así que deja de estar sin analizar del todo, y la decisión de rederivar sigue sin tomarse. **R-deuda ratificado**: dos deudas nuevas y ninguna abre sesión; y la sesión tensó la regla por el lado del pago DE PASO, que se cobró una vez (el informe duplicado, en el camino del Cambio) y se denegó otra (la tipificación de las 43 aulas, que no lo está). **CORRECCIÓN R5 APLICADA EN ESTE CIERRE, pendiente desde S134:** `D-F8.6-ii-b` está CERRADA desde S83 y `gestion_proyecto.md` la afirmaba viva y bloqueante en TRES sitios —la fila de H1 en §2, su fila de §4 y el «Terminado cuando» de la ficha de O-ajuste-cierre—, más el recuento de §4. Corregidos los cuatro. Consecuencia: O-ajuste-cierre no está bloqueado por deuda alguna y su criterio queda pendiente de reescribir, que es trabajo de su propio M0. **Deuda bloqueante del proyecto: 0.**
  OBSERVACIÓN DE MÉTODO, con TRES instancias medidas en una sola sesión y por eso nace token. Los guiones que escribí buscaban el TOKEN ESPERADO en vez del vigente y fallaron tres veces: `grep` de `_aulaDesconocida` y del literal `11` que pasó por encima de `ESCRITURAS_CARGA_COMPLETA = 804`; SQL contra `horario` y `tramo_id` cuando el esquema dice `horario_generado` y `tramo_inicio_id`; y lectura del conteo de suites por «vitest 316», cifra de S118 muerta desde entonces (la vigente es **419**). Es la familia de `D-guion-exit-enmascarado` y la causa raíz que S129 nombró —escribir de memoria teniendo el documento a un comando de distancia— en un instrumento nuevo. Las tres las cazó Claude Code midiendo, no leyendo. Nace `D-guion-busca-token-esperado`. Corolario que la sesión sí aplicó bien y conviene conservar: **gatear toda escritura con una corrida en seco revisada se ha pagado sola** —tres defectos del parche del catálogo interceptados antes de tocar el repo—.
  LIMPIEZA (M1-bis): archivada S133 a `bitacora-sesiones.md` (promovida a `### Sesión 133`, insertada al final en orden ascendente, cuerpo íntegro); degradada S134 a «Última sesión registrada (previa):»; S135 queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S132 a S133. R4/costura: `git diff --stat 552d522..HEAD -- app/src solver/src app/frontend/src` **vacío**, así que las suites medidas en S132 —91 + 282 de Maven y 419 de vitest— siguen midiendo este árbol y NO se recorren, con el mismo argumento explícito de S133 y ahora comprobado para este tramo. Los dos tokens nuevos nacen con definición viva aquí y citante vivo en `gestion_proyecto.md` §4.
  O-demo (H2, abierto en S115) ACTIVO, con **`C-centro-completo` HECHO** y las DOS primeras patas del criterio 5 de Fase 8 demostradas A ESCALA REAL: el centro entero creado por las vías legítimas del producto y un horario válido sobre él que además conserva sin residuo la carga del horario que el centro imparte. **O-demo NO cierra**: falta juzgar la tercera, «presentable al centro», y el obstáculo conocido son los 28 tutores, que S134 midió INCORRECTOS y no meramente incompletos. Si eso cae dentro o fuera del criterio es decisión del M0 siguiente, no de éste. O-ajuste-cierre (H1) sigue vivo, ya sin la deuda falsa que lo describía, y con su criterio pendiente de reescribir. **Deuda bloqueante del proyecto: 0.** Commits: `beb044a` (catálogo), `4d11a49` (cargador), `494554e` (informe unificado), `e09ba3a` (instrumento de la capa 2). Árbol de la generación: `4d11a491ac1c2db06717f859c2dea6ba985eadf3`.
### Sesión 136 — O-demo (H2): **LA TERCERA PATA DEL CRITERIO 5 QUEDA DEFINIDA Y MEDIDA, y el diagnóstico de S134 se corrige: los 28 tutores NO son incorrectos. 21 están BIEN —los 16 ordinarios de ESO y FPB casan 16/16—, faltan 5 co-tutorías y los 7 de Bachillerato son ANDAMIO que sostiene S8 en seis actividades.** Sesión de MEDICIÓN en cinco pasadas de solo lectura: ni una fila escrita en ninguna base. Se crea `C-presentable`, que ESCRIBE el criterio de la tercera pata antes de medirlo y entrega `docs/salvedades-demo.md`. Se cierra `D-nombres-sin-codigo`. Nacen tres deudas y ninguna abre sesión. Tres commits, los tres de documentación; ni un fichero de `app/` ni de `solver/` tocado.
  TIPO Y RITUAL: MEDICIÓN — M0 + M2 en cinco pasadas + M1. Sin M3 (no se muta lógica de producto) y sin M4 (no cambia ninguna vista). **No hay fila en la tabla de Tipos que la describa**, como en S133 y S134; S135 rompió la racha con un tipo que sí la tiene. Tercer caso en cuatro sesiones. NO nace token a propósito: si el recuento es acumulativo y no consecutivo esto ya es patrón, y decidirlo es trabajo de la sesión de Higiene/Método y no de este cierre.
  EL M0 ESCRIBIÓ EL CRITERIO QUE FALTABA, y es su aportación principal. «Presentable al centro» no era juzgable: no se puede aprobar ni suspender por evidencia. Definido ANTES de medir, en la forma con que S119 fijó qué asevera «válido»: **el resultado es presentable cuando ningún dato identificativo mostrado contradice lo que el centro dijo de sí mismo, y lo que no tiene fuente aparece como ausente y no como inventado.** LECTURA ESTRECHA, decidida con argumento: la lectura ancha —«el centro lo aceptaría como horario usable»— mete la calidad dentro del criterio y O-demo no terminaría nunca, contra lo que S119 ya decidió con R-terminado detrás. Quedan FUERA por decisión explícita la calidad (188 + 0 + 15 = 203, se mide y no se corrige) y los nombres truncados, que vienen así del origen y no son falsos sino incompletos.
  LAS PREDICCIONES, declaradas antes de medir y UNA DE ELLAS FALSA. P1 —28 filas todas `TUTOR_PRINCIPAL`, exactamente una por grupo— SE CUMPLE. P2 —`requiere_tutor` en menos de 28 actividades— acierta el número (22) y falla el razonamiento: se predijo que Bachillerato no tendría ninguna y tiene SEIS. **P3 es FALSA**: se predijo que divergirían bastantes de los 21 con fuente porque FIL2 sale como tutor de cinco grupos, y FIL2 tutoriza cinco grupos DE BACHILLERATO, que no están entre los 21. Se registra el fallo porque el mecanismo de predecir antes de medir sólo vale si el desajuste se anota.
  EL CRUCE, y con él CIERRA `D-nombres-sin-codigo`. `Horarios de profesores.pdf` trae 80 cabeceras `Profesor:` y 17 con línea `Tutor:`, que cubren 21 grupos: los 16 ordinarios de ESO y FPB más los 5 de diversificación, éstos como líneas de continuación bajo el tutor de su grupo padre. **Los 16 ordinarios casan 16/16 con la base, sin una sola divergencia**: 1ºA/GH6 Jiménez Montes, 1ºB/MAT8 Campanario Canales, 1ºC/LEN9 García Quintana, 1ºD/EFI2 García Granados, 2ºA/MAT5 López Ruiz, 2ºB/MAT1 Ríos Palomo, 2ºC/GH2 Torres Cubillo, 3ºA/MAT6 Gutiérrez Sánchez, 3ºB/BYG2 Afán Herencia, 3ºC/BYG3 Crespo Saborido, 4ºA/ING6 Jiménez Morgaz, 4ºB/GH4 Fedriani Freixinet, 4ºC/LEN6 García Rodríguez, 4ºD/EFI3 Pérez Reyes, 1FPB/PAU2 Millán Erencia y 2FPB/PAU1 Martínez Martínez. Los 5 PDC casan también en el principal, que es el del padre (`PdcService.heredarTutorPrincipal`, 8.5-D2a). El mapa nombre↔código queda ESCRITO AQUÍ, que es lo que permite cerrar la deuda: haberlo cruzado en una sesión no basta si el resultado no sobrevive a la sesión. Detalle de método: `^Profesor:` sólo casa 1 de las 80 cabeceras porque `pdftotext` antepone el salto de página, y hace falta `^\f?Profesor:`.
  POR QUÉ LA DERIVACIÓN ACIERTA DONDE ACIERTA Y FALLA DONDE FALLA, que es el resultado que reordena el objetivo. La heurística de S115 es «el tutor es quien imparte la sesión tutorial», y en este centro eso es CIERTO en ESO y FPB. Donde no hay TUT de la que tirar —los 7 de Bachillerato, que tienen tutor sin hora de tutoría— la derivación colgó el tutor del bloque `PTVE/PTEV_Relig` apoyándose en el **Hallazgo E**, que S134 refutó. **El diagnóstico de S134 —«los 28 son incorrectos, no incompletos»— era demasiado fuerte: califica la derivación, no cuenta los fallos, y los fallos son SIETE.**
  LAS DOS DIVERGENCIAS, y sólo una miente. **(A) Faltan 5 filas `CO_TUTOR`**: ORI1 Guerrero Serrano, Pilar figura en el PDF como tutora de los cinco PDC junto al tutor del padre, y la tabla tiene CERO filas con ese rol. La UI ya sabe mostrarlas y las CONSERVA al guardar (`tutoria-dialogo.ts:156-158` reenvía `...this.coTutores()` tal cual llegaron), pero no permite darlas de alta: su javadoc lo declara fuera de alcance. **(B) Sobran los 7 `TUTOR_PRINCIPAL` de Bachillerato**, sin respaldo en ninguna fuente entregada y sin que la ausencia se explique por cobertura parcial: el PDF trae 80 profesores y el catálogo 59.
  LAS 7 SON PORTANTES, medido y no supuesto. Cada actividad 40–45 (`Bloque-PTEV/PTVE_Relig`) tiene exactamente DOS plazas: una de REL1, que no aporta tutor, y otra con el profesor que resulta ser `TUTOR_PRINCIPAL` del grupo cubierto —FIL2 en cinco, GH6 en 1B-A, EFI3 en 1B-C—. `VerificadorSolucion.verificarTutorias` exige cobertura del grupo por IDENTIDAD (ciego a `grupoPadre`) y `CO_TUTOR` NO satisface S8. Borrar cualquiera de las 7 dispara `TUTORIA_SIN_TUTOR` y tumba la capa 1 de S135. Corolario que conviene no perder: que la fila del PDC exista con el mismo profesor que su padre NO es redundante, es lo que hace pasar S8 sobre el subgrupo del Di en `TUT3-3ºA+3ºADi` y sus hermanas.
  LA RECETA DE LA CORRECCIÓN DE B, escrita ahora con el coste medido para que la sesión que la ejecute no la redescubra. Cuando el centro entregue la lista oficial: **(1)** reescribir las 7 tutorías NO basta —el tutor real no imparte el bloque, luego S8 se rompe en las seis actividades—; **(2)** hay que bajar `requiere_tutor` a 0 en esas 6, que es lo correcto en sí, porque Bachillerato tiene tutor y no tiene hora de tutoría y el flag lo puso el Hallazgo E; **(3)** eso está BLOQUEADO sobre `educhronos-s135.db`: `ActividadService.exigirSinDependientes` devuelve 409 y hay 12 sesiones sobre las plazas de 40–45, así que la corrección exige la base sin horario y **REGENERAR** (601 s, 3/4 de probabilidad) y remedir las tres capas. **Asimetría medida y a favor:** `TutoriaService.reemplazar` NO tiene guarda de dependientes —borra, `flush()`, inserta—, así que una tutoría sí se reescribe sobre una base con horario ya generado. Y existe re-verificación SIN regenerar: `GET /api/horarios/{id}/diagnostico` reconstruye `ProblemaHorario` y `SolucionHorario` desde las `Sesion` persistidas y las pasa por el MISMO `VerificadorSolucion` que usa el CLI. Medido también que I4 es «COMO MUCHO uno» y no «exactamente uno»: el PUT con lista vacía es legítimo y un grupo puede vivir sin tutor.
  LA DIVERGENCIA A NO SE EJECUTA (R-terminado), y es la medición la que lo decide, no una preferencia. Un co-tutor ausente no es falso: el bloque se pinta con `@if (coTutores().length > 0)`, así que la pantalla no afirma que no los haya. Y no es un PUT: `cargar-centro.py` emite un PUT por ENTRADA de `catalogo["tutorias"]` y el PUT es reemplazo total, luego dos entradas del mismo grupo se pisan; añadir a ORI1 exige cambiar la FORMA del catálogo, el bucle de carga y la prevalidación, más una recarga y una generación. Queda registrada con receta y FUERA del objetivo. De ahí nace `D-cargador-tutoria-pisa`.
  EL ENTREGABLE: `docs/salvedades-demo.md`, que cumple la SEGUNDA MITAD del criterio —lo ausente aparece como ausente— y de paso es la petición de datos al centro. Su línea más importante NO son los tutores sino las DISPONIBILIDADES DEL PROFESORADO: `profesor_restriccion_horaria` tiene CERO filas y los volcados no las traen, así que el horario generado no se parece al que el centro imparte, y sin declararlo por escrito la demo se convierte en una discusión sobre por qué su horario está mal. **NO se sube al Project** (decisión del arquitecto en el propio cierre): es entregable hacia fuera y no fuente de decisión, y una copia de solo lectura en el Project sería una segunda instancia de `D-plan-duplicado` en cuanto el fichero cambie. El plan lo nombra con su ruta para que una sesión futura sepa pedirlo.
  LA OBSERVACIÓN DE MÉTODO DE LA SESIÓN, y es la más grave: **el proyecto ya sabía esto y no consiguió recordarlo.** `ESPECIFICACION-CATALOGO.md:530` —ambigüedad **A5**, escrita en S115— identifica la divergencia B con el mismo diagnóstico y la misma lista de profesores, y cierra recomendando literalmente pedir al centro la lista oficial de tutores por ser «de las cosas más baratas de confirmar», señalando que afecta a S8 en 22 actividades. Nadie la pidió en veinte sesiones, y S136 gastó cinco pasadas de medición en volver a formular la misma pregunta. La causa no es olvido: esa recomendación no entró en ninguna cola —no es deuda de §4 ni tarea de ningún Cambio—, así que ningún mecanismo la vuelve a poner delante. Es la tercera instancia de la misma enfermedad en dos sesiones, junto a la nota de `INFORME-RECONCILIACION.md` invisible durante dieciséis (S135) y al `_meta` del catálogo que declara «no hay importador» mientras `cargar-centro.py` lo lee. Nace `D-preguntas-sin-cola`. **Lo que M2 SÍ añade sobre A5**, y A5 no podía ver: que las 7 filas son PORTANTES —A5 sospechaba que sobraban—, y que las 5 co-tutorías SÍ están en el volcado. A5 escribió que «puede que el Di tenga su propio tutor y el volcado no lo muestre»; lo muestra, en la cabecera de la página de ORI1, y la regla de derivación no podía verlo porque Pilar Guerrero no imparte ninguna actividad TUT.
  DEUDA. **Cerrada: `D-nombres-sin-codigo`** —el mapa nombre↔código de los 21 queda escrito en esta entrada, que es lo que la cierra—. AFINADAS tres: `D-tutores-bachillerato` (ahora con la receta y el coste de la corrección, y con la medición de que las 7 filas son portantes); `D-hallazgo-E-refutado` (el `requiere_tutor` de las 6 actividades de bloque desciende de él y debe ir a 0, lo cual es cierto con independencia de quién sea el tutor real); y `D-censo-profesores-80-59` (medido que NINGUNO de los 59 carece de plaza, incluida ORI1, luego los 21 restantes no son «personal sin horario lectivo» de esta base sino gente ausente del catálogo). Nacen TRES: `D-cargador-tutoria-pisa`, `D-catalogo-meta-enganosa` y `D-preguntas-sin-cola`. **R-deuda ratificado**: tres deudas nuevas y ninguna abre sesión. **Deuda bloqueante del proyecto: 0.**
  LIMPIEZA (M1-bis): archivada S134 a `bitacora-sesiones.md` (promovida a `### Sesión 134`, insertada al final en orden ascendente, cuerpo íntegro); degradada S135 a «Última sesión registrada (previa):»; S136 queda como única cabecera H3 viva. Los dos censos de la bitácora pasan de S133 a S134. R4/costura: ni un fichero de `app/src`, `solver/src` ni `app/frontend/src` tocado en toda la sesión —los tres commits son de documentación: `e78e14e` la hoja de salvedades y `b11404c` + `058d978` el propio M1—, así que las suites medidas en S132 —91 + 282 de Maven y 419 de vitest— siguen midiendo este árbol y NO se recorren, con el mismo argumento explícito de S133 y S135. Los tres tokens nuevos nacen con definición viva aquí y citante vivo en `gestion_proyecto.md` §4.
  O-demo (H2, abierto en S115) ACTIVO, con `C-centro-completo` HECHO y **`C-presentable` EN CURSO**: la mitad DECLARATIVA de la tercera pata está cumplida por `docs/salvedades-demo.md`; la mitad «nada falso» NO lo está, y no depende de trabajo técnico sino de que el centro entregue los 7 tutores de Bachillerato. **O-demo vuelve a quedarse SIN TRABAJO EJECUTABLE**, como entre S120 y S134 y por la misma clase de causa: una dependencia de REQUISITOS, no de deuda. O-ajuste-cierre (H1) sigue vivo, sin deuda que lo bloquee y con su criterio pendiente de reescribir, que es trabajo de su propio M0. O-particiones (H2, esbozado S115) conserva el punto de partida material de S135 y sigue sin constructor. **Deuda bloqueante del proyecto: 0.** Commits: `e78e14e` (`docs/salvedades-demo.md`), `b11404c` (plan y bitácora) y `058d978` (gestión); 209 inserciones y 68 borrados en 4 ficheros, todos de documentación. Árbol limpio al cierre en `058d978ed506052506e1f4505bcbef82d4586f95`, con `1c0ae8d` como antepasado: el historial avanzó, no se reescribió. Las dos bases de referencia verificadas por md5 al cerrar y sin movimiento: `dfb30839…` y `e072383a…`.

