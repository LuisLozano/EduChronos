# Referencia de código — módulo `solver/`

- Fecha: 2026-06-29
- Commit: `257a463`

Índice de API construido a partir de las firmas del código fuente en
`solver/src/main/java` y `solver/src/test/java`. Solo miembros `public`,
`protected` o package-private (sin modificador); los `private` se omiten.
"Consume:" lista los tipos del módulo (`es.yaroki.educhronos.solver.*`) que
aparecen en imports o firmas.

---

## Paquete `domain`

### Actividad
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Consume: `domain.Asignatura`, `domain.PatronTemporal`, `domain.Plaza`

### ActividadInstancia
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Consume: `domain.Actividad`

### Asignatura
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### Aula
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### GrupoAdministrativo
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `domain.TipoGrupo`, `domain.GrupoAdministrativo`

### PatronTemporal
- Paquete: `es.yaroki.educhronos.solver.domain`
- public enum
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: (ninguno)

### Plaza
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Consume: `domain.Asignatura`, `domain.Profesor`, `domain.Aula`, `domain.Subgrupo`

### ProblemaHorario
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
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
- Consume: `domain.Tramo`, `domain.Aula`, `domain.Asignatura`, `domain.Profesor`, `domain.GrupoAdministrativo`, `domain.Subgrupo`, `domain.Actividad`, `domain.RestriccionHoraria`

### Profesor
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### RestriccionHoraria
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Consume: `domain.Profesor`, `domain.Tramo`, `domain.TipoRestriccion`

### SolucionHorario
- Paquete: `es.yaroki.educhronos.solver.domain`
- public class
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `domain.ActividadInstancia`, `domain.Tramo`, `domain.Plaza`, `domain.Aula`

### Subgrupo
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `domain.GrupoAdministrativo`

### TipoGrupo
- Paquete: `es.yaroki.educhronos.solver.domain`
- public enum
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: (ninguno)

### TipoRestriccion
- Paquete: `es.yaroki.educhronos.solver.domain`
- public enum
- Constantes: `DURA`, `BLANDA`
- Consume: (ninguno)

### Tramo
- Paquete: `es.yaroki.educhronos.solver.domain`
- public record (final)
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Consume: (ninguno)

---

## Paquete `cpsat`

### AulaOpcion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private record (final)
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Consume: `domain.Aula`

### Expansion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private final class
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.ProblemaHorario`

### HorarioInfactibleException
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- public class (extends RuntimeException)
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: (ninguno)

### InstanciaProgramada
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private final class
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `domain.ActividadInstancia`, `domain.Plaza`, `cpsat.AulaOpcion`

### ModeloCpSat
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private final class
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.Aula`, `domain.GrupoAdministrativo`, `domain.PatronTemporal`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.Profesor`, `domain.RestriccionHoraria`, `domain.TipoRestriccion`, `domain.SolucionHorario`, `domain.Subgrupo`, `domain.Tramo`

### ResultadoOptimizacion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- public record (final)
- Componentes:
  - `SolucionHorario solucion`
  - `CpSolverStatus estado`
  - `double objetivo`
  - `double cotaInferior`
- Métodos:
  - `public boolean esOptimo()`
- Consume: `domain.SolucionHorario`

### ResultadoVerificacion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- public record (final)
- Componentes:
  - `List<String> violaciones`
- Métodos:
  - `public boolean esValida()`
- Consume: (ninguno)

### SolverHorario
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- public final class
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
  - `public ResultadoOptimizacion resolverOptimizandoConDetalle(ProblemaHorario problema)`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `cpsat.ResultadoOptimizacion`

### VerificadorSolucion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- public final class
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.Aula`, `domain.GrupoAdministrativo`, `domain.PatronTemporal`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.Profesor`, `domain.RestriccionHoraria`, `domain.SolucionHorario`, `domain.Subgrupo`, `domain.TipoRestriccion`, `domain.Tramo`, `cpsat.ResultadoVerificacion`

### RestriccionNoSolapeGrupoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `domain.ActividadInstancia`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorario1EsoOrdinariasTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioAulaCandidataTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.Aula`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioBloqueD13Test
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void bloqueDuracion2CabeEnInicioValido() throws IOException`
  - `void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException`
  - `void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase3Test
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.Aula`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase4Test
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.GrupoAdministrativo`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Subgrupo`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioConsecutivasProfesorTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioDetalleOptimizacionTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void detalleReportaOptimoProbadoYObjetivoPositivoConcordante() throws Exception`
  - `void detalleReportaOptimoCeroSinVeto() throws Exception`
  - `void resolverOptimizandoClasicoDevuelveLaMismaSolucionQueElDetalle() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscala1BachTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscala1FpbTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscala2FpbTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscala4EsoDiTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void escala4EsoCompleto() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscala4EsoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void escala4EsoOrdinario() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioEscalaInstitutoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioFusion34EsoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void fusion34Eso() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioFusionEsoCompletaTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void fusionEsoCompleta() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioFusionInstitutoCompletoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void fusionInstitutoCompleto() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadBlandaProfesorTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadProfesorTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioLecturaBTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.GrupoAdministrativo`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Subgrupo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioOptimizacionInstitutoCompletoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void optimizacionInstitutoCompleto() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.Profesor`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteConsecutivasTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteIndispBlandaTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteVentanasTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `domain.ProblemaHorario`, `domain.Profesor`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioReligionParejasTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.GrupoAdministrativo`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Subgrupo`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### SolverHorarioVentanasProfesorTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `domain.ActividadInstancia`, `domain.ProblemaHorario`, `domain.Profesor`, `domain.SolucionHorario`, `domain.Tramo`, `io.ProblemaHorarioJsonLoader`

### VerificadorSolucionGrupoTest
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- package-private class
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `domain.Actividad`, `domain.ActividadInstancia`, `domain.Aula`, `domain.Asignatura`, `domain.GrupoAdministrativo`, `domain.PatronTemporal`, `domain.Plaza`, `domain.Profesor`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `domain.Subgrupo`, `domain.TipoGrupo`, `domain.Tramo`

---

## Paquete `io`

### ActividadDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `io.PlazaDto`

### AsignaturaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### AulaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### GrupoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: (ninguno)

### PlazaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: (ninguno)

### ProblemaHorarioDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `List<TramoDto> tramos`
  - `List<AulaDto> aulas`
  - `List<AsignaturaDto> asignaturas`
  - `List<ProfesorDto> profesores`
  - `List<GrupoDto> grupos`
  - `List<SubgrupoDto> subgrupos`
  - `List<ActividadDto> actividades`
  - `List<RestriccionHorariaDto> restriccionesHorarias`
- Consume: `io.TramoDto`, `io.AulaDto`, `io.AsignaturaDto`, `io.ProfesorDto`, `io.GrupoDto`, `io.SubgrupoDto`, `io.ActividadDto`, `io.RestriccionHorariaDto`

### ProblemaHorarioJsonLoader
- Paquete: `es.yaroki.educhronos.solver.io`
- public final class
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `domain.ProblemaHorario`

### ProblemaHorarioMapper
- Paquete: `es.yaroki.educhronos.solver.io`
- public final class
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `domain.Actividad`, `domain.Asignatura`, `domain.Aula`, `domain.GrupoAdministrativo`, `domain.PatronTemporal`, `domain.Plaza`, `domain.ProblemaHorario`, `domain.Profesor`, `domain.RestriccionHoraria`, `domain.TipoRestriccion`, `domain.Subgrupo`, `domain.TipoGrupo`, `domain.Tramo`

### ProblemaInvalidoException
- Paquete: `es.yaroki.educhronos.solver.io`
- public class (extends RuntimeException)
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: (ninguno)

### ProfesorDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### RestriccionHorariaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: (ninguno)

### SubgrupoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: (ninguno)

### TramoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- public record (final)
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: (ninguno)

### ProblemaHorarioJsonLoaderTest
- Paquete: `es.yaroki.educhronos.solver.io`
- package-private class
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
- Consume: `domain.Actividad`, `domain.Aula`, `domain.Plaza`, `domain.Profesor`, `domain.ProblemaHorario`, `domain.RestriccionHoraria`, `domain.TipoRestriccion`

---

## Paquete `cli`

### CodigoSalida
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private enum
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: (ninguno)

### FormatoCelda
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `domain.Profesor`, `cli.SesionMaterializada`

### HelloOrTools
- Paquete: `es.yaroki.educhronos.solver.cli`
- public class
- Métodos:
  - `public static void main(String[] args)`
- Consume: (ninguno)

### HorarioPrinter
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `domain.ProblemaHorario`, `domain.Tramo`, `cli.SesionMaterializada`, `cli.VistaHorario`

### Main
- Paquete: `es.yaroki.educhronos.solver.cli`
- public final class
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `cpsat.HorarioInfactibleException`, `cpsat.ResultadoVerificacion`, `cpsat.SolverHorario`, `cpsat.VerificadorSolucion`, `domain.ProblemaHorario`, `domain.SolucionHorario`, `io.ProblemaHorarioJsonLoader`, `io.ProblemaInvalidoException`

### Materializador
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `domain.SolucionHorario`, `cli.SesionMaterializada`

### SesionMaterializada
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private record (final)
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Consume: `domain.Tramo`, `domain.ActividadInstancia`, `domain.Plaza`

### VerificacionPrinter
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `cpsat.ResultadoVerificacion`

### VistaHorario
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private interface (`VistaHorario<K>`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `domain.ProblemaHorario`, `cli.SesionMaterializada`

### VistaPorGrupo
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class (`implements VistaHorario<GrupoAdministrativo>`)
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `domain.GrupoAdministrativo`, `domain.ProblemaHorario`, `domain.Subgrupo`, `cli.VistaHorario`, `cli.SesionMaterializada`, `cli.FormatoCelda`

### VistaPorProfesor
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private final class (`implements VistaHorario<Profesor>`)
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `domain.Profesor`, `domain.ProblemaHorario`, `cli.VistaHorario`, `cli.SesionMaterializada`, `cli.FormatoCelda`

### Main1EsoOrdinariasTest
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private class
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: (ninguno)

### MainTest
- Paquete: `es.yaroki.educhronos.solver.cli`
- package-private class
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: (ninguno)
