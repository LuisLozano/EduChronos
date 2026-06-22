# Referencia de código — módulo `solver/`

Fecha: 2026-06-22
`git rev-parse --short HEAD`: `4f684c4`

Índice de API construido a partir de las firmas del código fuente en
`solver/src/main/java` y `solver/src/test/java`. Solo miembros con visibilidad
`public`, `protected` o package-private (sin modificador); los miembros `private`
se omiten. Paquete base: `es.yaroki.educhronos.solver`.

---

## Paquete `domain`

### Actividad
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### ActividadInstancia
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Consume: `Actividad`

### Asignatura
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### Aula
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### GrupoAdministrativo
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### PatronTemporal
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · enum
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### Plaza
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### ProblemaHorario
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
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
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### RestriccionHoraria
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### SolucionHorario
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · class
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### Subgrupo
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### TipoGrupo
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · enum
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### TipoRestriccion
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · enum
- Constantes: `DURA`, `BLANDA`
- Consume: —

### Tramo
- Paquete: `es.yaroki.educhronos.solver.domain`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Consume: —

---

## Paquete `cpsat`

### AulaOpcion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · record
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Consume: `Aula`

### Expansion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · final class
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

### HorarioInfactibleException
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: public · class (extends RuntimeException)
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### InstanciaProgramada
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · final class
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### ModeloCpSat
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · final class
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `InstanciaProgramada`, `AulaOpcion`, `HorarioInfactibleException`, `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `Tramo`

### ResultadoVerificacion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: public · record
- Componentes:
  - `List<String> violaciones`
- Métodos:
  - `public boolean esValida()`
- Consume: —

### SolverHorario
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: public · final class
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `HorarioInfactibleException`, `VerificadorSolucion` (import)

### VerificadorSolucion
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: public · final class
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ResultadoVerificacion`, `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `RestriccionHoraria`, `Subgrupo`, `TipoRestriccion`, `Tramo`, `Expansion`

### RestriccionNoSolapeGrupoTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `Expansion`, `SolverHorario`, `VerificadorSolucion`, `ResultadoVerificacion`, `HorarioInfactibleException`, `ProblemaHorarioJsonLoader`

### SolverHorario1EsoOrdinariasTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `ResultadoVerificacion`, `ProblemaHorarioJsonLoader`

### SolverHorarioAulaCandidataTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`, `ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase3Test *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorarioJsonLoader`

### SolverHorarioCierreFase4Test *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorarioJsonLoader`

### SolverHorarioEscalaInstitutoTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadBlandaProfesorTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `Expansion`, `ProblemaHorarioJsonLoader`

### SolverHorarioIndisponibilidadProfesorTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `SolverHorario`, `HorarioInfactibleException`, `Expansion`, `ProblemaHorarioJsonLoader`

### SolverHorarioLecturaBTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`, `Expansion`, `ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteIndispBlandaTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorarioJsonLoader`

### SolverHorarioOroFuerteVentanasTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorarioJsonLoader`

### SolverHorarioReligionParejasTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `Expansion`, `ProblemaHorarioJsonLoader`

### SolverHorarioTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `ResultadoVerificacion`, `Expansion`, `ProblemaHorarioJsonLoader`

### SolverHorarioVentanasProfesorTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `SolverHorario`, `VerificadorSolucion`, `ResultadoVerificacion`, `Expansion`, `ProblemaHorarioJsonLoader`

### VerificadorSolucionGrupoTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Visibilidad: package-private · class
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`, `Expansion`, `VerificadorSolucion`, `ResultadoVerificacion`

---

## Paquete `io`

### ActividadDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### AsignaturaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### AulaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### GrupoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: —

### PlazaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: —

### ProblemaHorarioDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
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
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · final class
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`, `ProblemaInvalidoException`, `ProblemaHorarioMapper`, `ProblemaHorarioDto`

### ProblemaHorarioMapper
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · final class
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `ProblemaHorarioDto`, `ProblemaHorario`, `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaInvalidoException`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `RestriccionHorariaDto`

### ProblemaInvalidoException
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · class (extends RuntimeException)
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### ProfesorDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### RestriccionHorariaDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: —

### SubgrupoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: —

### TramoDto
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: public · record
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

### ProblemaHorarioJsonLoaderTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.io`
- Visibilidad: package-private · class
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
- Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `RestriccionHoraria`, `TipoRestriccion`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

---

## Paquete `cli`

### CodigoSalida
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · enum
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: —

### FormatoCelda
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `SesionMaterializada`, `Profesor`

### HelloOrTools
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: public · class
- Métodos:
  - `public static void main(String[] args)`
- Consume: —

### HorarioPrinter
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### Main
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: public · final class
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

### Materializador
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### SesionMaterializada
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · record
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### VerificacionPrinter
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### VistaHorario
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · interface (`VistaHorario<K>`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### VistaPorGrupo
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class (implements `VistaHorario<GrupoAdministrativo>`)
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `SesionMaterializada`, `VistaHorario`, `FormatoCelda`

### VistaPorProfesor
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · final class (implements `VistaHorario<Profesor>`)
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `SesionMaterializada`, `VistaHorario`, `FormatoCelda`

### Main1EsoOrdinariasTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · class
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: `Main`

### MainTest *(test)*
- Paquete: `es.yaroki.educhronos.solver.cli`
- Visibilidad: package-private · class
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: `Main`
