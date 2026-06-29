# Referencia de código — módulo `solver/`

Índice de API puro, construido a partir de las firmas del código fuente actual
(`solver/src/main/java` y `solver/src/test/java`). Solo miembros con visibilidad
public, protected o package-private; todo miembro `private` queda excluido.

- Fecha: 2026-06-29
- Commit (`git rev-parse --short HEAD`): `ceb6e5e`

---

## Paquete `es.yaroki.educhronos.solver.domain`

### Actividad
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### ActividadInstancia
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Consume: `Actividad`

### Asignatura
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### Aula
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### GrupoAdministrativo
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### PatronTemporal
- public enum (`es.yaroki.educhronos.solver.domain`)
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### Plaza
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### ProblemaHorario
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `List<Tramo> tramos`
  - `List<Aula> aulas`
  - `List<Asignatura> asignaturas`
  - `List<Profesor> profesores`
  - `List<GrupoAdministrativo> grupos`
  - `List<Subgrupo> subgrupos`
  - `List<Actividad> actividades`
  - `List<RestriccionHoraria> restriccionesHorarias`
- Métodos:
  - `public int indiceDeTramo(Tramo tramo)`
- Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`, `RestriccionHoraria`

### Profesor
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### RestriccionHoraria
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### SolucionHorario
- public class (`es.yaroki.educhronos.solver.domain`)
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### Subgrupo
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### TipoGrupo
- public enum (`es.yaroki.educhronos.solver.domain`)
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### TipoRestriccion
- public enum (`es.yaroki.educhronos.solver.domain`)
- Constantes: `DURA`, `BLANDA`
- Consume: —

### Tramo
- public record (`es.yaroki.educhronos.solver.domain`)
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Consume: —

---

## Paquete `es.yaroki.educhronos.solver.cpsat`

### AulaOpcion
- package-private record (`es.yaroki.educhronos.solver.cpsat`)
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Consume: `Aula`

### Expansion
- package-private final class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `ActividadInstancia`, `Actividad`, `ProblemaHorario`

### HorarioInfactibleException
- public class (`es.yaroki.educhronos.solver.cpsat`) extends `RuntimeException`
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### InstanciaProgramada
- package-private final class (`es.yaroki.educhronos.solver.cpsat`)
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### ModeloCpSat
- package-private final class (`es.yaroki.educhronos.solver.cpsat`)
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `ModeloCpSat sembrarHint(SolucionHorario semilla)`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `SolucionHorario`, `Subgrupo`, `Tramo`

### ResultadoOptimizacion
- public record (`es.yaroki.educhronos.solver.cpsat`)
- Componentes:
  - `SolucionHorario solucion`
  - `CpSolverStatus estado`
  - `double objetivo`
  - `double cotaInferior`
- Métodos:
  - `public boolean esOptimo()`
- Consume: `SolucionHorario`

### ResultadoVerificacion
- public record (`es.yaroki.educhronos.solver.cpsat`)
- Componentes:
  - `List<String> violaciones`
- Métodos:
  - `public boolean esValida()`
- Consume: —

### RestriccionNoSolapeGrupoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorario
- public final class (`es.yaroki.educhronos.solver.cpsat`)
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
  - `public ResultadoOptimizacion resolverOptimizandoConDetalle(ProblemaHorario problema)`
  - `public ResultadoOptimizacion resolverOptimizandoConSemilla(ProblemaHorario problema, SolucionHorario semilla)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ResultadoOptimizacion`

### SolverHorario1EsoOrdinariasTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioAulaCandidataTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioBloqueD13Test
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void bloqueDuracion2CabeEnInicioValido() throws IOException`
  - `void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException`
  - `void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase3Test
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase4Test
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioConsecutivasProfesorTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioDetalleOptimizacionTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void detalleReportaOptimoProbadoYObjetivoPositivoConcordante() throws Exception`
  - `void detalleReportaOptimoCeroSinVeto() throws Exception`
  - `void resolverOptimizandoClasicoDevuelveLaMismaSolucionQueElDetalle() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscala1BachTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscala1FpbTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscala2FpbTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscala4EsoDiTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void escala4EsoCompleto() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscala4EsoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void escala4EsoOrdinario() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscalaInstitutoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioFusion34EsoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void fusion34Eso() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioFusionEsoCompletaTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void fusionEsoCompleta() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioFusionInstitutoCompletoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void fusionInstitutoCompleto() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadBlandaProfesorTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadProfesorTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioLecturaBTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`

### SolverHorarioOptimizacionInstitutoCompletoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void optimizacionInstitutoCompleto() throws Exception`
- Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteConsecutivasTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteIndispBlandaTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteVentanasTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioPodaAulaTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void podaMantieneFactibleYRespetaRecorte() throws Exception`
  - `void podaInsuficienteSaturaYEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### SolverHorarioReligionParejasTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioVentanasProfesorTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### SolverHorarioWarmStartInstitutoCompletoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void warmStartInstitutoCompleto() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### VerificadorSolucion
- public final class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `ResultadoVerificacion`, `ProblemaHorario`, `SolucionHorario`, `Profesor`

### VerificadorSolucionGrupoTest
- package-private class (`es.yaroki.educhronos.solver.cpsat`)
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Paquete `es.yaroki.educhronos.solver.io`

### ActividadDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### AsignaturaDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### AulaDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### GrupoDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: —

### PlazaDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: —

### ProblemaHorarioDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `List<TramoDto> tramos`
  - `List<AulaDto> aulas`
  - `List<AsignaturaDto> asignaturas`
  - `List<ProfesorDto> profesores`
  - `List<GrupoDto> grupos`
  - `List<SubgrupoDto> subgrupos`
  - `List<ActividadDto> actividades`
  - `List<RestriccionHorariaDto> restriccionesHorarias`
- Consume: `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `RestriccionHorariaDto`

### ProblemaHorarioJsonLoader
- public final class (`es.yaroki.educhronos.solver.io`)
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`

### ProblemaHorarioJsonLoaderTest
- package-private class (`es.yaroki.educhronos.solver.io`)
- Métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void cargaAulasCandidatasResueltas() throws Exception`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
  - `void cargaRestriccionHorariaDuraResuelta() throws Exception`
  - `void cargaProblemaSinRestriccionesHorarias() throws Exception`
  - `void rechazaRestriccionHorariaConTramoInexistente()`
- Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `RestriccionHoraria`, `TipoRestriccion`

### ProblemaHorarioMapper
- public final class (`es.yaroki.educhronos.solver.io`)
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`

### ProblemaInvalidoException
- public class (`es.yaroki.educhronos.solver.io`) extends `RuntimeException`
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### ProfesorDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### RestriccionHorariaDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: —

### SubgrupoDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: —

### TramoDto
- public record (`es.yaroki.educhronos.solver.io`)
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

---

## Paquete `es.yaroki.educhronos.solver.cli`

### CodigoSalida
- package-private enum (`es.yaroki.educhronos.solver.cli`)
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: —

### FormatoCelda
- package-private final class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `SesionMaterializada`, `Profesor`

### HelloOrTools
- public class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `public static void main(String[] args)`
- Consume: —

### HorarioPrinter
- package-private final class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### Main
- public final class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

### Main1EsoOrdinariasTest
- package-private class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: —

### MainTest
- package-private class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: —

### Materializador
- package-private final class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### SesionMaterializada
- package-private record (`es.yaroki.educhronos.solver.cli`)
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### VerificacionPrinter
- package-private final class (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### VistaHorario
- package-private interface (`es.yaroki.educhronos.solver.cli`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### VistaPorGrupo
- package-private final class (`es.yaroki.educhronos.solver.cli`) implements `VistaHorario<GrupoAdministrativo>`
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `SesionMaterializada`, `VistaHorario`

### VistaPorProfesor
- package-private final class (`es.yaroki.educhronos.solver.cli`) implements `VistaHorario<Profesor>`
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `SesionMaterializada`, `VistaHorario`
