# Referencia de código — módulo `solver/`

> Índice de API puro, leído directamente de las firmas del código fuente.
> Solo miembros `public` / `protected` / package-private (sin modificador).
> Todo miembro `private` queda omitido.

- Fecha: 2026-06-22
- Commit (`git rev-parse --short HEAD`): `38d0d18`

---

## Paquete `domain`

`es.yaroki.educhronos.solver.domain`

### Actividad
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Constructores:
  - `public Actividad(String codigo, Optional<Asignatura> asignatura, int repeticionesPorSemana, int duracionTramos, PatronTemporal patronTemporal, List<Plaza> plazas)` (compacto)
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### ActividadInstancia
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Constructores:
  - `public ActividadInstancia(Actividad actividad, int indice)` (compacto)
- Consume: `Actividad`

### Asignatura
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Asignatura(String codigo, String nombre)` (compacto)
- Consume: —

### Aula
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Aula(String codigo, String nombre)` (compacto)
- Consume: —

### GrupoAdministrativo
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Constructores:
  - `public GrupoAdministrativo(String codigo, TipoGrupo tipo, Optional<GrupoAdministrativo> grupoPadre)` (compacto)
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### PatronTemporal
- Visibilidad: `public` · Tipo: `enum`
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### Plaza
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Constructores:
  - `public Plaza(String codigo, Asignatura asignatura, Set<Profesor> profesores, Optional<Aula> aulaFija, Set<Aula> aulasCandidatas, Set<Subgrupo> subgrupos)` (compacto)
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### ProblemaHorario
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `List<Tramo> tramos`
  - `List<Aula> aulas`
  - `List<Asignatura> asignaturas`
  - `List<Profesor> profesores`
  - `List<GrupoAdministrativo> grupos`
  - `List<Subgrupo> subgrupos`
  - `List<Actividad> actividades`
  - `List<RestriccionHoraria> restriccionesHorarias`
- Constructores:
  - `public ProblemaHorario(List<Tramo> tramos, List<Aula> aulas, List<Asignatura> asignaturas, List<Profesor> profesores, List<GrupoAdministrativo> grupos, List<Subgrupo> subgrupos, List<Actividad> actividades, List<RestriccionHoraria> restriccionesHorarias)` (compacto)
- Métodos:
  - `public int indiceDeTramo(Tramo tramo)`
- Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`, `RestriccionHoraria`

### Profesor
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Profesor(String codigo, String nombre)` (compacto)
- Consume: —

### RestriccionHoraria
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Constructores:
  - `public RestriccionHoraria(Profesor profesor, Tramo tramo, TipoRestriccion tipo, int peso, Optional<String> motivo)` (compacto)
- Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### SolucionHorario
- Visibilidad: `public` · Tipo: `class`
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### Subgrupo
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Constructores:
  - `public Subgrupo(String codigo, Set<GrupoAdministrativo> grupos)` (compacto)
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### TipoGrupo
- Visibilidad: `public` · Tipo: `enum`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### TipoRestriccion
- Visibilidad: `public` · Tipo: `enum`
- Constantes: `DURA`, `BLANDA`
- Consume: —

### Tramo
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Constructores:
  - `public Tramo(String codigo, int diaSemana, int ordenEnDia)` (compacto)
- Consume: —

---

## Paquete `cpsat`

`es.yaroki.educhronos.solver.cpsat`

### AulaOpcion
- Visibilidad: package-private · Tipo: `record`
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Constructores:
  - `AulaOpcion(Aula aula, BoolVar presencia, IntervalVar intervalo)` (compacto)
- Consume: `Aula`

### Expansion
- Visibilidad: package-private · Tipo: `final class`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `ActividadInstancia`, `Actividad`, `ProblemaHorario`

### HorarioInfactibleException
- Visibilidad: `public` · Tipo: `class` (extends `RuntimeException`)
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### InstanciaProgramada
- Visibilidad: package-private · Tipo: `final class`
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### ModeloCpSat
- Visibilidad: package-private · Tipo: `final class`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `SolucionHorario`, `Subgrupo`, `Tramo`, `InstanciaProgramada`, `AulaOpcion`, `Expansion`, `HorarioInfactibleException`

### ResultadoVerificacion
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `List<String> violaciones`
- Constructores:
  - `public ResultadoVerificacion(List<String> violaciones)` (compacto)
- Métodos:
  - `public boolean esValida()`
- Consume: —

### RestriccionNoSolapeGrupoTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorario
- Visibilidad: `public` · Tipo: `final class`
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ModeloCpSat`, `HorarioInfactibleException`, `VerificadorSolucion`

### SolverHorario1EsoOrdinariasTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

### SolverHorarioAulaCandidataTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioCierreFase3Test
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioCierreFase4Test
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioConsecutivasProfesorTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioEscalaInstitutoTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioIndisponibilidadBlandaProfesorTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioIndisponibilidadProfesorTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `HorarioInfactibleException`

### SolverHorarioLecturaBTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioOroFuerteConsecutivasTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioOroFuerteIndispBlandaTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioOroFuerteVentanasTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioReligionParejasTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

### SolverHorarioVentanasProfesorTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

### VerificadorSolucion
- Visibilidad: `public` · Tipo: `final class`
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `SolucionHorario`, `Subgrupo`, `TipoRestriccion`, `Tramo`, `Expansion`, `ResultadoVerificacion`

### VerificadorSolucionGrupoTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`, `Expansion`, `ResultadoVerificacion`, `VerificadorSolucion`

---

## Paquete `io`

`es.yaroki.educhronos.solver.io`

### ActividadDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### AsignaturaDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### AulaDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### GrupoDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: —

### PlazaDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: —

### ProblemaHorarioDto
- Visibilidad: `public` · Tipo: `record`
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
- Visibilidad: `public` · Tipo: `final class`
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaHorarioMapper`, `ProblemaInvalidoException`

### ProblemaHorarioJsonLoaderTest
- Visibilidad: package-private · Tipo: `class`
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

### ProblemaHorarioMapper
- Visibilidad: `public` · Tipo: `final class`
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `RestriccionHorariaDto`, `ProblemaInvalidoException`

### ProblemaInvalidoException
- Visibilidad: `public` · Tipo: `class` (extends `RuntimeException`)
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### ProfesorDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### RestriccionHorariaDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: —

### SubgrupoDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: —

### TramoDto
- Visibilidad: `public` · Tipo: `record`
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

---

## Paquete `cli`

`es.yaroki.educhronos.solver.cli`

### CodigoSalida
- Visibilidad: package-private · Tipo: `enum`
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: —

### FormatoCelda
- Visibilidad: package-private · Tipo: `final class`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `Profesor`, `SesionMaterializada`

### HelloOrTools
- Visibilidad: `public` · Tipo: `class`
- Métodos:
  - `public static void main(String[] args)`
- Consume: —

### HorarioPrinter
- Visibilidad: package-private · Tipo: `final class`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### Main
- Visibilidad: `public` · Tipo: `final class`
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `CodigoSalida`, `VerificacionPrinter`, `SesionMaterializada`, `Materializador`, `HorarioPrinter`, `VistaPorGrupo`, `VistaPorProfesor`

### Main1EsoOrdinariasTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: `Main`

### MainTest
- Visibilidad: package-private · Tipo: `class`
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: `Main`

### Materializador
- Visibilidad: package-private · Tipo: `final class`
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### SesionMaterializada
- Visibilidad: package-private · Tipo: `record`
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Constructores:
  - `SesionMaterializada(Tramo tramo, ActividadInstancia instancia, Plaza plaza)` (compacto)
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### VerificacionPrinter
- Visibilidad: package-private · Tipo: `final class`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### VistaHorario
- Visibilidad: package-private · Tipo: `interface` (genérica `<K>`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### VistaPorGrupo
- Visibilidad: package-private · Tipo: `final class` (implements `VistaHorario<GrupoAdministrativo>`)
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### VistaPorProfesor
- Visibilidad: package-private · Tipo: `final class` (implements `VistaHorario<Profesor>`)
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`
