# Referencia de código — módulo `solver/`

Índice de API puro extraído de las firmas del código fuente.

- Fecha: 2026-06-21
- Commit (`git rev-parse --short HEAD`): `1685554`

Ámbito: `solver/src/main/java` y `solver/src/test/java`. Solo miembros con
visibilidad `public`, `protected` o package-private (sin modificador). Todo
miembro `private` se omite. "Consume" lista los tipos de
`es.yaroki.educhronos.solver.*` que aparecen en imports o firmas.

---

## Paquete `domain`

### `Actividad`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Consume: `Actividad`

### `Asignatura`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `Aula`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `GrupoAdministrativo`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### `PatronTemporal`
- Visibilidad: public · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.domain`
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### `Plaza`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### `ProblemaHorario`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
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

### `Profesor`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `RestriccionHoraria`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### `SolucionHorario`
- Visibilidad: public · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.domain`
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### `Subgrupo`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### `TipoGrupo`
- Visibilidad: public · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.domain`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### `TipoRestriccion`
- Visibilidad: public · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.domain`
- Constantes: `DURA`, `BLANDA`
- Consume: —

### `Tramo`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Consume: —

---

## Paquete `cpsat`

### `AulaOpcion`
- Visibilidad: package-private · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Consume: `Aula`

### `Expansion`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `ActividadInstancia`, `Actividad`, `ProblemaHorario`

### `HorarioInfactibleException`
- Visibilidad: public · Tipo: class (extends `RuntimeException`)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### `InstanciaProgramada`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### `ModeloCpSat`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `SolucionHorario`, `Subgrupo`, `Tramo`

### `ResultadoVerificacion`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `List<String> violaciones`
- Métodos:
  - `public boolean esValida()`
- Consume: —

### `SolverHorario`
- Visibilidad: public · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`

### `VerificadorSolucion`
- Visibilidad: public · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ResultadoVerificacion`

### `RestriccionNoSolapeGrupoTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorario1EsoOrdinariasTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioAulaCandidataTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioCierreFase3Test`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioCierreFase4Test`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioEscalaInstitutoTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioIndisponibilidadProfesorTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioLecturaBTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioOroFuerteVentanasTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `Profesor`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioReligionParejasTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorarioVentanasProfesorTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `VerificadorSolucionGrupoTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat` (test)
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Paquete `io`

### `ActividadDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### `AsignaturaDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `AulaDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `GrupoDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: —

### `PlazaDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: —

### `ProblemaHorarioDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
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

### `ProblemaHorarioJsonLoader`
- Visibilidad: public · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.io`
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`

### `ProblemaHorarioMapper`
- Visibilidad: public · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.io`
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`

### `ProblemaInvalidoException`
- Visibilidad: public · Tipo: class (extends `RuntimeException`)
- Paquete: `es.yaroki.educhronos.solver.io`
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### `ProfesorDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `RestriccionHorariaDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: —

### `SubgrupoDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: —

### `TramoDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

### `ProblemaHorarioJsonLoaderTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.io` (test)
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

---

## Paquete `cli`

### `CodigoSalida`
- Visibilidad: package-private · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: —

### `FormatoCelda`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `SesionMaterializada`, `Profesor`

### `HelloOrTools`
- Visibilidad: public · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public static void main(String[] args)`
- Consume: —

### `HorarioPrinter`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### `Main`
- Visibilidad: public · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

### `Materializador`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### `SesionMaterializada`
- Visibilidad: package-private · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.cli`
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### `VerificacionPrinter`
- Visibilidad: package-private · Tipo: final class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### `VistaHorario`
- Visibilidad: package-private · Tipo: interface (`VistaHorario<K>`)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo`
- Visibilidad: package-private · Tipo: final class (implements `VistaHorario<GrupoAdministrativo>`)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `VistaPorProfesor`
- Visibilidad: package-private · Tipo: final class (implements `VistaHorario<Profesor>`)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `Main1EsoOrdinariasTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli` (test)
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: —

### `MainTest`
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli` (test)
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: —
