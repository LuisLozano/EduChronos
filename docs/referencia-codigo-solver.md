# Referencia de código — módulo `solver/`

- **Fecha:** 2026-06-29
- **Commit (`git rev-parse --short HEAD`):** `e6f6cb5`

Índice de API del módulo `solver/`, construido exclusivamente a partir de las
firmas del código fuente actual (`solver/src/main/java` y `solver/src/test/java`).
Solo miembros `public`, `protected` o package-private (sin modificador). Todo
miembro `private` queda omitido. Las firmas se transcriben tal cual aparecen en
el código. La línea **Consume** lista los demás tipos del módulo
(`es.yaroki.educhronos.solver.*`) que aparecen en firmas o imports del tipo.

---

## Paquete `domain`

Paquete: `es.yaroki.educhronos.solver.domain`. Todos los tipos están en
`solver/src/main/java`.

### `Actividad` — `public record`
Componentes:
- `String codigo`
- `Optional<Asignatura> asignatura`
- `int repeticionesPorSemana`
- `int duracionTramos`
- `PatronTemporal patronTemporal`
- `List<Plaza> plazas`

Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia` — `public record`
Componentes:
- `Actividad actividad`
- `int indice`

Consume: `Actividad`

### `Asignatura` — `public record`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `Aula` — `public record`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `GrupoAdministrativo` — `public record`
Componentes:
- `String codigo`
- `TipoGrupo tipo`
- `Optional<GrupoAdministrativo> grupoPadre`

Consume: `TipoGrupo`, `GrupoAdministrativo`

### `PatronTemporal` — `public enum`
Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`

Consume: (ninguno)

### `Plaza` — `public record`
Componentes:
- `String codigo`
- `Asignatura asignatura`
- `Set<Profesor> profesores`
- `Optional<Aula> aulaFija`
- `Set<Aula> aulasCandidatas`
- `Set<Subgrupo> subgrupos`

Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### `ProblemaHorario` — `public record`
Componentes:
- `List<Tramo> tramos`
- `List<Aula> aulas`
- `List<Asignatura> asignaturas`
- `List<Profesor> profesores`
- `List<GrupoAdministrativo> grupos`
- `List<Subgrupo> subgrupos`
- `List<Actividad> actividades`
- `List<RestriccionHoraria> restriccionesHorarias`

Métodos:
- `public int indiceDeTramo(Tramo tramo)`

Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`, `RestriccionHoraria`

### `Profesor` — `public record`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `RestriccionHoraria` — `public record`
Componentes:
- `Profesor profesor`
- `Tramo tramo`
- `TipoRestriccion tipo`
- `int peso`
- `Optional<String> motivo`

Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### `SolucionHorario` — `public class`
Constructores:
- `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
- `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`

Métodos:
- `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
- `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
- `public Map<ActividadInstancia, Tramo> asignaciones()`

Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### `Subgrupo` — `public record`
Componentes:
- `String codigo`
- `Set<GrupoAdministrativo> grupos`

Métodos:
- `public boolean equals(Object o)`
- `public int hashCode()`

Consume: `GrupoAdministrativo`

### `TipoGrupo` — `public enum`
Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`

Consume: (ninguno)

### `TipoRestriccion` — `public enum`
Constantes: `DURA`, `BLANDA`

Consume: (ninguno)

### `Tramo` — `public record`
Componentes:
- `String codigo`
- `int diaSemana`
- `int ordenEnDia`

Consume: (ninguno)

---

## Paquete `cpsat`

Paquete: `es.yaroki.educhronos.solver.cpsat`.

### `AulaOpcion` — package-private `record` (`solver/src/main/java`)
Componentes:
- `Aula aula`
- `BoolVar presencia`
- `IntervalVar intervalo`

Consume: `Aula`

### `Expansion` — package-private `final class` (`solver/src/main/java`)
Métodos:
- `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
- `static List<ActividadInstancia> todas(ProblemaHorario problema)`

Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

### `HorarioInfactibleException` — `public class` extends `RuntimeException` (`solver/src/main/java`)
Constructores:
- `public HorarioInfactibleException(String mensaje)`

Consume: (ninguno)

### `InstanciaProgramada` — package-private `final class` (`solver/src/main/java`)
Constructores:
- `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`

Métodos:
- `ActividadInstancia instancia()`
- `IntVar tramoIndex()`
- `IntervalVar intervalo()`
- `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`

Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### `ModeloCpSat` — package-private `final class` (`solver/src/main/java`)
Constructores:
- `ModeloCpSat(ProblemaHorario problema)`

Métodos:
- `CpModel model()`
- `ModeloCpSat construir()`
- `ModeloCpSat construirConObjetivo()`
- `ModeloCpSat sembrarHint(SolucionHorario semilla)`
- `SolucionHorario extraerSolucion(CpSolver solver)`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `SolucionHorario`, `Subgrupo`, `Tramo`, `AulaOpcion`, `InstanciaProgramada`, `Expansion`, `HorarioInfactibleException`

### `ResultadoOptimizacion` — `public record` (`solver/src/main/java`)
Componentes:
- `SolucionHorario solucion`
- `CpSolverStatus estado`
- `double objetivo`
- `double cotaInferior`

Métodos:
- `public boolean esOptimo()`

Consume: `SolucionHorario`

### `ResultadoVerificacion` — `public record` (`solver/src/main/java`)
Componentes:
- `List<String> violaciones`

Métodos:
- `public boolean esValida()`

Consume: (ninguno)

### `SolverHorario` — `public final class` (`solver/src/main/java`)
Constructores:
- `public SolverHorario()`
- `public SolverHorario(double maxSegundos, int semilla)`

Métodos:
- `public SolucionHorario resolver(ProblemaHorario problema)`
- `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- `public ResultadoOptimizacion resolverOptimizandoConDetalle(ProblemaHorario problema)`
- `public ResultadoOptimizacion resolverOptimizandoConSemilla(ProblemaHorario problema, SolucionHorario semilla)`

Consume: `ProblemaHorario`, `SolucionHorario`, `ModeloCpSat`, `ResultadoOptimizacion`, `HorarioInfactibleException`, `VerificadorSolucion`

### `VerificadorSolucion` — `public final class` (`solver/src/main/java`)
Métodos:
- `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
- `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `SolucionHorario`, `Subgrupo`, `TipoRestriccion`, `Tramo`, `Expansion`, `ResultadoVerificacion`

### `RestriccionNoSolapeGrupoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
- `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`

Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorario1EsoOrdinariasTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioAulaCandidataTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void candidatasEligeAulaLibre() throws Exception`
- `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
- `void candidataUnicaCompartidaEsInfactible() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioBloqueD13Test` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void bloqueDuracion2CabeEnInicioValido() throws IOException`
- `void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException`
- `void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioCierreFase3Test` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void cierreDeFase3() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioCierreFase4Test` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void cierreDeFase4() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioConsecutivasProfesorTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioDetalleOptimizacionTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void detalleReportaOptimoProbadoYObjetivoPositivoConcordante() throws Exception`
- `void detalleReportaOptimoCeroSinVeto() throws Exception`
- `void resolverOptimizandoClasicoDevuelveLaMismaSolucionQueElDetalle() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscala1BachTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscala1FpbTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscala2FpbTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscala4EsoDiTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void escala4EsoCompleto() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscala4EsoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void escala4EsoOrdinario() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscalaInstitutoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void escala1y2y3ESOconPDC() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioFusion34EsoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void fusion34Eso() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioFusionEsoCompletaTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void fusionEsoCompleta() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioFusionInstitutoCompletoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void fusionInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioIndisponibilidadBlandaProfesorTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioIndisponibilidadProfesorTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
- `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
- `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioLecturaBTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void bloqueOptativasMultigrupoEsFactible() throws Exception`
- `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`

Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioOptimizacionInstitutoCompletoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void optimizacionInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioOroFuerteConsecutivasTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioOroFuerteIndispBlandaTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioOroFuerteVentanasTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
- `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`

Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioReligionParejasTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void cierreBloque1Fase5() throws Exception`

Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
- `void todasLasInstanciasQuedanColocadas() throws Exception`
- `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
- `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioVentanasProfesorTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
- `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioWarmStartInstitutoCompletoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void warmStartInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `VerificadorSolucionGrupoTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void reportaSolapeDeGrupoEntreActividadesDistintas()`
- `void noReportaGrupoEnTramosDistintos()`
- `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Paquete `io`

Paquete: `es.yaroki.educhronos.solver.io`.

### `ActividadDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String asignatura`
- `Integer repeticionesPorSemana`
- `Integer duracionTramos`
- `String patronTemporal`
- `List<PlazaDto> plazas`

Consume: `PlazaDto`

### `AsignaturaDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `AulaDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `GrupoDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String tipo`
- `String grupoPadre`

Consume: (ninguno)

### `PlazaDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String asignatura`
- `List<String> profesores`
- `String aulaFija`
- `List<String> aulasCandidatas`
- `List<String> subgrupos`

Consume: (ninguno)

### `ProblemaHorarioDto` — `public record` (`solver/src/main/java`)
Componentes:
- `List<TramoDto> tramos`
- `List<AulaDto> aulas`
- `List<AsignaturaDto> asignaturas`
- `List<ProfesorDto> profesores`
- `List<GrupoDto> grupos`
- `List<SubgrupoDto> subgrupos`
- `List<ActividadDto> actividades`
- `List<RestriccionHorariaDto> restriccionesHorarias`

Consume: `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `RestriccionHorariaDto`

### `ProblemaHorarioJsonLoader` — `public final class` (`solver/src/main/java`)
Constructores:
- `public ProblemaHorarioJsonLoader()`

Métodos:
- `public ProblemaHorario cargar(InputStream entrada)`

Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaHorarioMapper`, `ProblemaInvalidoException`

### `ProblemaHorarioMapper` — `public final class` (`solver/src/main/java`)
Métodos:
- `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`

Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `RestriccionHorariaDto`, `ProblemaInvalidoException`

### `ProblemaInvalidoException` — `public class` extends `RuntimeException` (`solver/src/main/java`)
Constructores:
- `public ProblemaInvalidoException(String mensaje)`
- `public ProblemaInvalidoException(String mensaje, Throwable causa)`

Consume: (ninguno)

### `ProfesorDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `RestriccionHorariaDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String profesor`
- `String tramo`
- `String tipo`
- `Integer peso`
- `String motivo`

Consume: (ninguno)

### `SubgrupoDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `List<String> grupos`

Consume: (ninguno)

### `TramoDto` — `public record` (`solver/src/main/java`)
Componentes:
- `String codigo`
- `Integer diaSemana`
- `Integer ordenEnDia`

Consume: (ninguno)

### `ProblemaHorarioJsonLoaderTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
- `void rechazaReferenciaAProfesorInexistente()`
- `void rechazaPlazaSinProfesores()`
- `void cargaAulasCandidatasResueltas() throws Exception`
- `void rechazaGrupoPdcSinGrupoPadre()`
- `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- `void cargaRestriccionHorariaDuraResuelta() throws Exception`
- `void cargaProblemaSinRestriccionesHorarias() throws Exception`
- `void rechazaRestriccionHorariaConTramoInexistente()`

Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `RestriccionHoraria`, `TipoRestriccion`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

---

## Paquete `cli`

Paquete: `es.yaroki.educhronos.solver.cli`.

### `CodigoSalida` — package-private `enum` (`solver/src/main/java`)
Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`

Métodos:
- `int valor()`

Consume: (ninguno)

### `FormatoCelda` — package-private `final class` (`solver/src/main/java`)
Métodos:
- `static String formatear(SesionMaterializada sesion)`

Consume: `Profesor`, `SesionMaterializada`

### `HelloOrTools` — `public class` (`solver/src/main/java`)
Métodos:
- `public static void main(String[] args)`

Consume: (ninguno)

### `HorarioPrinter` — package-private `final class` (`solver/src/main/java`)
Métodos:
- `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`

Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### `Main` — `public final class` (`solver/src/main/java`)
Métodos:
- `public static void main(String[] args)`
- `static int ejecutar(String[] args, PrintStream out, PrintStream err)`

Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `CodigoSalida`, `SesionMaterializada`, `Materializador`, `VerificacionPrinter`, `HorarioPrinter`, `VistaPorGrupo`, `VistaPorProfesor`

### `Materializador` — package-private `final class` (`solver/src/main/java`)
Métodos:
- `static List<SesionMaterializada> materializar(SolucionHorario solucion)`

Consume: `SolucionHorario`, `SesionMaterializada`

### `SesionMaterializada` — package-private `record` (`solver/src/main/java`)
Componentes:
- `Tramo tramo`
- `ActividadInstancia instancia`
- `Plaza plaza`

Consume: `ActividadInstancia`, `Plaza`, `Tramo`

### `VerificacionPrinter` — package-private `final class` (`solver/src/main/java`)
Métodos:
- `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`

Consume: `ResultadoVerificacion`

### `VistaHorario` — package-private `interface` (`solver/src/main/java`)
Parámetro de tipo: `<K>`

Métodos:
- `String titulo()`
- `List<K> filas(ProblemaHorario problema)`
- `String etiquetaFila(K clave)`
- `Set<K> filasDe(SesionMaterializada sesion)`
- `String contenidoCelda(SesionMaterializada sesion)`

Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo` — package-private `final class` implements `VistaHorario<GrupoAdministrativo>` (`solver/src/main/java`)
Métodos:
- `public String titulo()`
- `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
- `public String etiquetaFila(GrupoAdministrativo clave)`
- `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
- `public String contenidoCelda(SesionMaterializada sesion)`

Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `VistaPorProfesor` — package-private `final class` implements `VistaHorario<Profesor>` (`solver/src/main/java`)
Métodos:
- `public String titulo()`
- `public List<Profesor> filas(ProblemaHorario problema)`
- `public String etiquetaFila(Profesor clave)`
- `public Set<Profesor> filasDe(SesionMaterializada sesion)`
- `public String contenidoCelda(SesionMaterializada sesion)`

Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `Main1EsoOrdinariasTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`

Consume: `Main`, `CodigoSalida`

### `MainTest` — package-private `class` (`solver/src/test/java`)
Métodos:
- `void sinArgumentosSaleConCodigo2()`
- `void demasiadosArgumentosSaleConCodigo2()`
- `void ficheroInexistenteSaleConCodigo2()`
- `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
- `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
- `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`

Consume: `Main`
