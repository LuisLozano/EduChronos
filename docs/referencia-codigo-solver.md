# Referencia de código — módulo `solver/`

Índice de API construido directamente de las firmas del código fuente.

- Fecha: 2026-06-17
- `git rev-parse --short HEAD`: `efa19c3`

Convenciones: cada tipo indica visibilidad (`public` / package-private), clase de
tipo y paquete. Se listan constructores y métodos públicos y package-private con
sus firmas exactas (se omiten los `private`). La línea `Consume:` lista los tipos
del módulo (`es.yaroki.educhronos.solver.*`) que aparecen en imports o firmas.

---

## Paquete `domain`

### `es.yaroki.educhronos.solver.domain` — código principal

#### `Actividad`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Constructor compacto: `public Actividad { … }`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

#### `ActividadInstancia`
- Visibilidad: `public` · record
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Constructor compacto: `public ActividadInstancia { … }`
- Consume: `Actividad`

#### `Asignatura`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Asignatura { … }`
- Consume: (ninguno)

#### `Aula`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Aula { … }`
- Consume: (ninguno)

#### `GrupoAdministrativo`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Constructor compacto: `public GrupoAdministrativo { … }`
- Consume: `GrupoAdministrativo`, `TipoGrupo`

#### `PatronTemporal`
- Visibilidad: `public` · enum
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: (ninguno)

#### `Plaza`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Constructor compacto: `public Plaza { … }`
- Consume: `Asignatura`, `Aula`, `Profesor`, `Subgrupo`

#### `ProblemaHorario`
- Visibilidad: `public` · record
- Componentes:
  - `List<Tramo> tramos`
  - `List<Aula> aulas`
  - `List<Asignatura> asignaturas`
  - `List<Profesor> profesores`
  - `List<GrupoAdministrativo> grupos`
  - `List<Subgrupo> subgrupos`
  - `List<Actividad> actividades`
- Constructor compacto: `public ProblemaHorario { … }`
- Métodos:
  - `public int indiceDeTramo(Tramo tramo)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `Profesor`, `Subgrupo`, `Tramo`

#### `Profesor`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Profesor { … }`
- Consume: (ninguno)

#### `SolucionHorario`
- Visibilidad: `public` · class
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Aula`, `Plaza`, `Tramo`

#### `Subgrupo`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Constructor compacto: `public Subgrupo { … }`
- Métodos:
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

#### `TipoGrupo`
- Visibilidad: `public` · enum
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: (ninguno)

#### `Tramo`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Constructor compacto: `public Tramo { … }`
- Consume: (ninguno)

---

## Paquete `cpsat`

### `es.yaroki.educhronos.solver.cpsat` — código principal

#### `AulaOpcion`
- Visibilidad: package-private · record
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Constructor compacto: `AulaOpcion { … }`
- Consume: `Aula`

#### `Expansion`
- Visibilidad: package-private · `final class`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

#### `HorarioInfactibleException`
- Visibilidad: `public` · class (extends `RuntimeException`)
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: (ninguno)

#### `InstanciaProgramada`
- Visibilidad: package-private · `final class`
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `AulaOpcion`, `Plaza`

#### `ModeloCpSat`
- Visibilidad: package-private · `final class`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `AulaOpcion`, `Expansion`, `GrupoAdministrativo`, `HorarioInfactibleException`, `InstanciaProgramada`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`

#### `ResultadoVerificacion`
- Visibilidad: `public` · record
- Componentes:
  - `List<String> violaciones`
- Constructor compacto: `public ResultadoVerificacion { … }`
- Métodos:
  - `public boolean esValida()`
- Consume: (ninguno)

#### `SolverHorario`
- Visibilidad: `public` · `final class`
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
- Consume: `HorarioInfactibleException`, `ModeloCpSat`, `ProblemaHorario`, `SolucionHorario`

#### `VerificadorSolucion`
- Visibilidad: `public` · `final class`
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Expansion`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `ResultadoVerificacion`, `SolucionHorario`, `Subgrupo`, `Tramo`

### `es.yaroki.educhronos.solver.cpsat` — tests

#### `RestriccionNoSolapeGrupoTest`
- Visibilidad: package-private · class
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorario1EsoOrdinariasTest`
- Visibilidad: package-private · class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioAulaCandidataTest`
- Visibilidad: package-private · class
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioCierreFase3Test`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioCierreFase4Test`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioEscalaInstitutoTest`
- Visibilidad: package-private · class
- Métodos:
  - `void escala1y2y3ESO() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioLecturaBTest`
- Visibilidad: package-private · class
- Métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioReligionParejasTest`
- Visibilidad: package-private · class
- Métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioTest`
- Visibilidad: package-private · class
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `VerificadorSolucionGrupoTest`
- Visibilidad: package-private · class
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Paquete `io`

### `es.yaroki.educhronos.solver.io` — código principal

#### `ActividadDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

#### `AsignaturaDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

#### `AulaDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

#### `GrupoDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: (ninguno)

#### `PlazaDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: (ninguno)

#### `ProblemaHorarioDto`
- Visibilidad: `public` · record
- Componentes:
  - `List<TramoDto> tramos`
  - `List<AulaDto> aulas`
  - `List<AsignaturaDto> asignaturas`
  - `List<ProfesorDto> profesores`
  - `List<GrupoDto> grupos`
  - `List<SubgrupoDto> subgrupos`
  - `List<ActividadDto> actividades`
- Consume: `ActividadDto`, `AsignaturaDto`, `AulaDto`, `GrupoDto`, `ProfesorDto`, `SubgrupoDto`, `TramoDto`

#### `ProblemaHorarioJsonLoader`
- Visibilidad: `public` · `final class`
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaHorarioMapper`, `ProblemaInvalidoException`

#### `ProblemaHorarioMapper`
- Visibilidad: `public` · `final class`
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `Actividad`, `ActividadDto`, `Asignatura`, `AsignaturaDto`, `Aula`, `AulaDto`, `GrupoAdministrativo`, `GrupoDto`, `PatronTemporal`, `Plaza`, `PlazaDto`, `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaInvalidoException`, `Profesor`, `ProfesorDto`, `Subgrupo`, `SubgrupoDto`, `TipoGrupo`, `Tramo`, `TramoDto`

#### `ProblemaInvalidoException`
- Visibilidad: `public` · class (extends `RuntimeException`)
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: (ninguno)

#### `ProfesorDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

#### `SubgrupoDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: (ninguno)

#### `TramoDto`
- Visibilidad: `public` · record
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: (ninguno)

### `es.yaroki.educhronos.solver.io` — tests

#### `ProblemaHorarioJsonLoaderTest`
- Visibilidad: package-private · class
- Métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void cargaAulasCandidatasResueltas() throws Exception`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

---

## Paquete `cli`

### `es.yaroki.educhronos.solver.cli` — código principal

#### `CodigoSalida`
- Visibilidad: package-private · enum
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: (ninguno)

#### `FormatoCelda`
- Visibilidad: package-private · `final class`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `Profesor`, `SesionMaterializada`

#### `HelloOrTools`
- Visibilidad: `public` · class
- Métodos:
  - `public static void main(String[] args)`
- Consume: (ninguno)

#### `HorarioPrinter`
- Visibilidad: package-private · `final class`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `SesionMaterializada`, `Tramo`, `VistaHorario`

#### `Main`
- Visibilidad: `public` · `final class`
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `ResultadoVerificacion`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`

#### `Materializador`
- Visibilidad: package-private · `final class`
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SesionMaterializada`, `SolucionHorario`

#### `SesionMaterializada`
- Visibilidad: package-private · record
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Constructor compacto: `SesionMaterializada { … }`
- Consume: `ActividadInstancia`, `Plaza`, `Tramo`

#### `VerificacionPrinter`
- Visibilidad: package-private · `final class`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

#### `VistaHorario`
- Visibilidad: package-private · `interface` (genérico `VistaHorario<K>`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

#### `VistaPorGrupo`
- Visibilidad: package-private · `final class` (implements `VistaHorario<GrupoAdministrativo>`)
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `SesionMaterializada`, `Subgrupo`, `VistaHorario`

#### `VistaPorProfesor`
- Visibilidad: package-private · `final class` (implements `VistaHorario<Profesor>`)
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `Profesor`, `SesionMaterializada`, `VistaHorario`

### `es.yaroki.educhronos.solver.cli` — tests

#### `Main1EsoOrdinariasTest`
- Visibilidad: package-private · class
- Métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: (ninguno)

#### `MainTest`
- Visibilidad: package-private · class
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: (ninguno)
