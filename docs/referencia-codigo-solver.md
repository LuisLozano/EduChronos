# Referencia de código — módulo `solver/`

Índice de API construido a partir del código fuente. Solo superficie
public / protected / package-private; todo miembro `private` queda excluido.

- Fecha: 2026-06-25
- Commit (`git rev-parse --short HEAD`): `297a85b`

---

## Paquete `domain`

`solver/src/main/java/es/yaroki/educhronos/solver/domain/`

### Actividad
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### ActividadInstancia
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Consume: `Actividad`

### Asignatura
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### Aula
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### GrupoAdministrativo
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### PatronTemporal
- Visibilidad: public — `enum` — `es.yaroki.educhronos.solver.domain`
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: (ninguno)

### Plaza
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### ProblemaHorario
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
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
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### RestriccionHoraria
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Profesor profesor`
  - `Tramo tramo`
  - `TipoRestriccion tipo`
  - `int peso`
  - `Optional<String> motivo`
- Consume: `Profesor`, `Tramo`, `TipoRestriccion`

### SolucionHorario
- Visibilidad: public — `class` — `es.yaroki.educhronos.solver.domain`
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### Subgrupo
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### TipoGrupo
- Visibilidad: public — `enum` — `es.yaroki.educhronos.solver.domain`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: (ninguno)

### TipoRestriccion
- Visibilidad: public — `enum` — `es.yaroki.educhronos.solver.domain`
- Constantes: `DURA`, `BLANDA`
- Consume: (ninguno)

### Tramo
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Consume: (ninguno)

---

## Paquete `cpsat`

`solver/src/main/java/es/yaroki/educhronos/solver/cpsat/`

### AulaOpcion
- Visibilidad: package-private — `record` — `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `Aula aula`
  - `BoolVar presencia` (OR-Tools)
  - `IntervalVar intervalo` (OR-Tools)
- Consume: `Aula`

### Expansion
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `ActividadInstancia`, `Actividad`, `ProblemaHorario`

### HorarioInfactibleException
- Visibilidad: public — `class` (extends `RuntimeException`) — `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: (ninguno)

### InstanciaProgramada
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### ModeloCpSat
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `SolucionHorario`, `Subgrupo`, `Tramo`, `InstanciaProgramada`, `AulaOpcion`, `Expansion`, `HorarioInfactibleException`

### ResultadoVerificacion
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `List<String> violaciones`
- Métodos:
  - `public boolean esValida()`
- Consume: (ninguno)

### SolverHorario
- Visibilidad: public — `final class` — `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ModeloCpSat`, `HorarioInfactibleException`, `VerificadorSolucion`

### VerificadorSolucion
- Visibilidad: public — `final class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
  - `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `SolucionHorario`, `Subgrupo`, `TipoRestriccion`, `Tramo`, `Expansion`, `ResultadoVerificacion`

### RestriccionNoSolapeGrupoTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorario1EsoOrdinariasTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

### SolverHorarioAulaCandidataTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioBloqueD13Test (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void bloqueDuracion2CabeEnInicioValido() throws IOException`
  - `void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException`
  - `void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioCierreFase3Test (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioCierreFase4Test (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioConsecutivasProfesorTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioEscala1BachTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioEscala4EsoDiTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void escala4EsoCompleto() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioEscala4EsoTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void escala4EsoOrdinario() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioEscalaInstitutoTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioFusion34EsoTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void fusion34Eso() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioFusionEsoCompletaTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void fusionEsoCompleta() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioIndisponibilidadBlandaProfesorTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `Expansion`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioIndisponibilidadProfesorTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
  - `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
  - `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioLecturaBTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException`

### SolverHorarioOroFuerteConsecutivasTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioOroFuerteIndispBlandaTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioOroFuerteVentanasTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
  - `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `Profesor`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioReligionParejasTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### SolverHorarioTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

### SolverHorarioVentanasProfesorTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `Profesor`, `ProblemaHorarioJsonLoader`, `SolverHorario`, `VerificadorSolucion`

### VerificadorSolucionGrupoTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `Subgrupo`, `Profesor`, `Aula`, `ProblemaHorario`, `SolucionHorario`, `ResultadoVerificacion`, `VerificadorSolucion`

---

## Paquete `io`

`solver/src/main/java/es/yaroki/educhronos/solver/io/`

### ActividadDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### AsignaturaDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### AulaDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### GrupoDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: (ninguno)

### PlazaDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: (ninguno)

### ProblemaHorarioDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
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
- Visibilidad: public — `final class` — `es.yaroki.educhronos.solver.io`
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaHorarioMapper`, `ProblemaInvalidoException`

### ProblemaHorarioMapper
- Visibilidad: public — `final class` — `es.yaroki.educhronos.solver.io`
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `TipoRestriccion`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `RestriccionHorariaDto`, `ProblemaInvalidoException`

### ProblemaInvalidoException
- Visibilidad: public — `class` (extends `RuntimeException`) — `es.yaroki.educhronos.solver.io`
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: (ninguno)

### ProfesorDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### RestriccionHorariaDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String profesor`
  - `String tramo`
  - `String tipo`
  - `Integer peso`
  - `String motivo`
- Consume: (ninguno)

### SubgrupoDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: (ninguno)

### TramoDto
- Visibilidad: public — `record` — `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: (ninguno)

### ProblemaHorarioJsonLoaderTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.io`
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

`solver/src/main/java/es/yaroki/educhronos/solver/cli/`

### CodigoSalida
- Visibilidad: package-private — `enum` — `es.yaroki.educhronos.solver.cli`
- Constantes: `OK(0)`, `INFACTIBLE(1)`, `ENTRADA_INVALIDA(2)`, `VIOLACIONES_DURAS(3)`
- Métodos:
  - `int valor()`
- Consume: (ninguno)

### FormatoCelda
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `Profesor`, `SesionMaterializada`

### HelloOrTools
- Visibilidad: public — `class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public static void main(String[] args)`
- Consume: (ninguno)

### HorarioPrinter
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### Main
- Visibilidad: public — `final class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `SesionMaterializada`, `Materializador`, `VerificacionPrinter`, `VistaPorGrupo`, `VistaPorProfesor`, `HorarioPrinter`, `CodigoSalida`

### Materializador
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### SesionMaterializada
- Visibilidad: package-private — `record` — `es.yaroki.educhronos.solver.cli`
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### VerificacionPrinter
- Visibilidad: package-private — `final class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### VistaHorario
- Visibilidad: package-private — `interface` (genérica `<K>`) — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### VistaPorGrupo
- Visibilidad: package-private — `final class` (implements `VistaHorario<GrupoAdministrativo>`) — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### VistaPorProfesor
- Visibilidad: package-private — `final class` (implements `VistaHorario<Profesor>`) — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### Main1EsoOrdinariasTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: `Main`

### MainTest (test)
- Visibilidad: package-private — `class` — `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: `Main`
