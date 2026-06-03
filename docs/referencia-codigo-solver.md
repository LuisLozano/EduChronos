# Referencia de código — módulo `solver/`

Índice de API construido exclusivamente a partir de las firmas del código fuente.

- Fecha: 2026-06-03
- Commit (`git rev-parse --short HEAD`): `4478c9e`

Cobertura: todos los `.java` de `solver/src/main/java` y `solver/src/test/java`.
Orden: por paquete (`domain`, `cpsat`, `io`, `cli`); dentro de cada paquete,
alfabético. Los tipos de `src/test/java` se marcan con `(test)`.

---

## Paquete `es.yaroki.educhronos.solver.domain`

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
- Constructores:
  - `public Actividad { ... }` (constructor compacto)
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Constructores:
  - `public ActividadInstancia { ... }` (constructor compacto)
- Consume: `Actividad`

### `Asignatura`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Asignatura { ... }` (constructor compacto)
- Consume: —

### `Aula`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Aula { ... }` (constructor compacto)
- Consume: —

### `GrupoAdministrativo`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Constructores:
  - `public GrupoAdministrativo { ... }` (constructor compacto)
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
- Constructores:
  - `public Plaza { ... }` (constructor compacto)
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
- Constructores:
  - `public ProblemaHorario { ... }` (constructor compacto)
- Métodos:
  - `public int indiceDeTramo(Tramo tramo)`
- Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`

### `Profesor`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores:
  - `public Profesor { ... }` (constructor compacto)
- Consume: —

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
  - `GrupoAdministrativo grupo`
- Constructores:
  - `public Subgrupo { ... }` (constructor compacto)
- Consume: `GrupoAdministrativo`

### `TipoGrupo`
- Visibilidad: public · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.domain`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### `Tramo`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Constructores:
  - `public Tramo { ... }` (constructor compacto)
- Consume: —

---

## Paquete `es.yaroki.educhronos.solver.cpsat`

### `AulaOpcion`
- Visibilidad: package-private · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Constructores:
  - `AulaOpcion { ... }` (constructor compacto)
- Consume: `Aula`

### `Expansion`
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `private Expansion()`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

### `HorarioInfactibleException`
- Visibilidad: public · Tipo: class (extends `RuntimeException`)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### `InstanciaProgramada`
- Visibilidad: package-private · Tipo: class (final)
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
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `private void crearVariables()`
  - `private Map<Plaza, List<AulaOpcion>> crearOpcionesDeAula(ActividadInstancia inst, IntVar tramoIndex, long duracion, String nombre)`
  - `private void restriccionNoSolapeProfesor()`
  - `private void restriccionNoSolapeAula()`
  - `private List<IntervalVar> intervalosOpcionalesEn(InstanciaProgramada ip, Aula aula)`
  - `private void restriccionNoSolapeSubgrupo()`
  - `private void restriccionNoSolapeGrupo()`
  - `private boolean tocaGrupo(InstanciaProgramada ip, GrupoAdministrativo grupo)`
  - `private boolean usaProfesor(InstanciaProgramada ip, Profesor profesor)`
  - `private boolean usaAula(InstanciaProgramada ip, Aula aula)`
  - `private boolean cubreSubgrupo(InstanciaProgramada ip, Subgrupo subgrupo)`
  - `private void restriccionDistribucionPorDia()`
  - `private Map<Actividad, List<InstanciaProgramada>> instanciasPorActividad()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`, `AulaOpcion`, `InstanciaProgramada`, `HorarioInfactibleException`, `Expansion`

### `ResultadoVerificacion`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `List<String> violaciones`
- Constructores:
  - `public ResultadoVerificacion { ... }` (constructor compacto)
- Métodos:
  - `public boolean esValida()`
- Consume: —

### `SolverHorario`
- Visibilidad: public · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ModeloCpSat`, `HorarioInfactibleException`

### `VerificadorSolucion`
- Visibilidad: public · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - (constructor por defecto implícito)
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `private void verificarTodasColocadas(List<ActividadInstancia> esperadas, SolucionHorario solucion, List<String> violaciones)`
  - `private void verificarNoSolapes(List<ActividadInstancia> esperadas, SolucionHorario solucion, List<String> violaciones)`
  - `private <T> void reportarColisiones(String tipo, Tramo tramo, Map<T, Integer> conteo, Function<T, String> codigo, List<String> violaciones)`
  - `private void verificarDistribucion(ProblemaHorario problema, List<ActividadInstancia> esperadas, SolucionHorario solucion, List<String> violaciones)`
  - `private String etiqueta(ActividadInstancia inst)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ResultadoVerificacion`, `Expansion`

### `RestriccionNoSolapeGrupoTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `private static ProblemaHorario cargar(String fixture)`
  - `private static Tramo tramoDe(ProblemaHorario problema, SolucionHorario solucion, String codigoActividad)`
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos()`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible()`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion`, `HorarioInfactibleException` · `es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader`

### `SolverHorario1EsoOrdinariasTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveFactibleSinViolaciones()`
- Consume: `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion` · `es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader`

### `SolverHorarioAulaCandidataTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void candidatasEligeAulaLibre()`
  - `void mixtaEnMismoTramoEligeAulaLibre()`
  - `void candidataUnicaCompartidaEsInfactible()`
  - `private ProblemaHorario cargar(String path)`
  - `private Aula aulaElegidaDe(ProblemaHorario problema, SolucionHorario solucion, String codigoActividad, String codigoPlaza)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`, `HorarioInfactibleException` · `es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader`

### `SolverHorarioTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones()`
  - `void todasLasInstanciasQuedanColocadas()`
  - `void laCoDocenciaOcupaAAmbosProfesores()`
  - `void elVerificadorDetectaUnSolapeDeProfesor()`
  - `private ProblemaHorario cargarFixture()`
  - `private ActividadInstancia buscar(ProblemaHorario problema, String codigoActividad, int indice)`
  - `private java.util.Set<Tramo> tramosDe(ProblemaHorario problema, SolucionHorario solucion, String codigoActividad)`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `Expansion`, `SolverHorario`, `ResultadoVerificacion`, `VerificadorSolucion` · `es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader`

### `VerificadorSolucionGrupoTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `private static Actividad actividad(String cod, Subgrupo sg, Profesor prof, Aula aula)`
  - `private static ProblemaHorario problema(List<Actividad> actividades)`
  - `private static ActividadInstancia instanciaDe(ProblemaHorario problema, String codActividad)`
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`, `Expansion`, `ResultadoVerificacion`, `VerificadorSolucion`

---

## Paquete `es.yaroki.educhronos.solver.io`

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
- Consume: `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`

### `ProblemaHorarioJsonLoader`
- Visibilidad: public · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.io`
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `es.yaroki.educhronos.solver.domain.ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaInvalidoException`, `ProblemaHorarioMapper`

### `ProblemaHorarioMapper`
- Visibilidad: public · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.io`
- Constructores:
  - `private ProblemaHorarioMapper()`
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
  - `private static Plaza mapearPlaza(PlazaDto pl, String ctxActividad, Map<String, Asignatura> asignaturas, Map<String, Profesor> profesores, Map<String, Aula> aulas, Map<String, Subgrupo> subgrupos)`
  - `private static void verificarI2(List<Plaza> plazas, String ctx)`
  - `private static GrupoAdministrativo resolverGrupo(String codigo, Map<String, GrupoDto> dtos, Map<String, GrupoAdministrativo> resueltos, Set<String> enCurso)`
  - `private static <T> List<T> requiereSeccion(List<T> lista, String nombre)`
  - `private static String exigirCodigo(String codigo, String tipo)`
  - `private static String exigirTexto(String valor, String campo, String ctx)`
  - `private static int exigir(Integer valor, String campo, String ctx)`
  - `private static void comprobarNoDuplicado(Map<String, ?> mapa, String codigo, String tipo)`
  - `private static <T> T resolver(Map<String, T> mapa, String codigo, String tipoRef, String ctx)`
  - `private static <T> T construir(Supplier<T> supplier, String ctx)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `ProblemaInvalidoException`

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

### `SubgrupoDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String grupo`
- Consume: —

### `TramoDto`
- Visibilidad: public · Tipo: record
- Paquete: `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

### `ProblemaHorarioJsonLoaderTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.io`
- Métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones()`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void cargaAulasCandidatasResueltas()`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
  - `private InputStream recurso(String ruta)`
  - `private static InputStream stream(String json)`
- Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

---

## Paquete `es.yaroki.educhronos.solver.cli`

### `CodigoSalida`
- Visibilidad: package-private · Tipo: enum
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constantes: `OK(0)`, `INFACTIBLE(1)`, `ENTRADA_INVALIDA(2)`, `VIOLACIONES_DURAS(3)`
- Constructores:
  - `CodigoSalida(int valor)`
- Métodos:
  - `int valor()`
- Consume: —

### `FormatoCelda`
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constructores:
  - `private FormatoCelda()`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `Profesor`, `SesionMaterializada`

### `HelloOrTools`
- Visibilidad: public · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public static void main(String[] args)`
- Consume: —

### `HorarioPrinter`
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constructores:
  - `private HorarioPrinter()`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
  - `private static List<Integer> diasPresentesEn(ProblemaHorario problema)`
  - `private static List<Integer> ordenesPresentesEn(ProblemaHorario problema)`
  - `private static Map<DiaOrden, Tramo> indexarTramos(ProblemaHorario problema)`
  - `private static <K> Map<K, Map<Tramo, List<SesionMaterializada>>> construirIndice(List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
  - `private static <K> void imprimirSubTabla(PrintStream out, VistaHorario<K> vista, K clave, List<Integer> diasPresentes, List<Integer> ordenesPresentes, Map<DiaOrden, Tramo> mapaTramos, Map<Tramo, List<SesionMaterializada>> porTramo)`
  - `private static String separador(int anchoEtiqueta, int anchoColumna, int nDias)`
  - `private static String pad(String s, int ancho)`
- Tipos anidados:
  - `private record DiaOrden(int dia, int orden)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### `Main`
- Visibilidad: public · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constructores:
  - `private Main()`
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
  - `private static void imprimirResumenProblema(PrintStream out, ProblemaHorario p)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `SesionMaterializada`, `Materializador`, `HorarioPrinter`, `VistaPorGrupo`, `VistaPorProfesor`, `VerificacionPrinter`, `CodigoSalida`

### `Materializador`
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constructores:
  - `private Materializador()`
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
- Constructores:
  - `SesionMaterializada { ... }` (constructor compacto)
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### `VerificacionPrinter`
- Visibilidad: package-private · Tipo: class (final)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Constructores:
  - `private VerificacionPrinter()`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### `VistaHorario`
- Visibilidad: package-private · Tipo: interface
- Paquete: `es.yaroki.educhronos.solver.cli`
- Parámetro de tipo: `<K>`
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo`
- Visibilidad: package-private · Tipo: class (final, implements `VistaHorario<GrupoAdministrativo>`)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `VistaPorProfesor`
- Visibilidad: package-private · Tipo: class (final, implements `VistaHorario<Profesor>`)
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `Main1EsoOrdinariasTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void endToEnd_codigoSalidaOk(Path tempDir)`
- Consume: `Main`

### `MainTest` (test)
- Visibilidad: package-private · Tipo: class
- Paquete: `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0()`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos()`
  - `void problemaMinimoMuestraCodigosClaveDelFixture()`
  - `private Resultado ejecutar(String... args)`
  - `private Path rutaDeRecurso(String recurso)`
- Tipos anidados:
  - `private record Resultado(int codigo, String out, String err)`
- Consume: `Main`
