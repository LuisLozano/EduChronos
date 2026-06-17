# Referencia de código — módulo `solver/`

Fecha: 2026-06-12
Commit: `d75618f`

Índice de API construido a partir de las firmas del código fuente de
`solver/src/main/java` y `solver/src/test/java`. Para cada tipo se listan su
visibilidad, clase de tipo, paquete, miembros (componentes / constantes /
firmas de constructores y métodos públicos y package-private) y la línea
`Consume:` con los demás tipos del módulo (`es.yaroki.educhronos.solver.*`)
que aparecen en sus firmas o imports. Se omiten los miembros `private`.

---

## Paquete `domain`

Paquete: `es.yaroki.educhronos.solver.domain` (todos en `solver/src/main/java`).

### `Actividad`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Constructor compacto: `public Actividad { … }`
- Consume: Asignatura, PatronTemporal, Plaza

### `ActividadInstancia`
- Visibilidad: public — record
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Constructor compacto: `public ActividadInstancia { … }`
- Consume: Actividad

### `Asignatura`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Asignatura { … }`
- Consume: (ninguno)

### `Aula`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Aula { … }`
- Consume: (ninguno)

### `GrupoAdministrativo`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Constructor compacto: `public GrupoAdministrativo { … }`
- Consume: TipoGrupo

### `PatronTemporal`
- Visibilidad: public — enum
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: (ninguno)

### `Plaza`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Constructor compacto: `public Plaza { … }`
- Consume: Asignatura, Aula, Profesor, Subgrupo

### `ProblemaHorario`
- Visibilidad: public — record
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
- Consume: Actividad, Asignatura, Aula, GrupoAdministrativo, Profesor, Subgrupo, Tramo

### `Profesor`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructor compacto: `public Profesor { … }`
- Consume: (ninguno)

### `SolucionHorario`
- Visibilidad: public — class
- Constructores:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
- Métodos:
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: ActividadInstancia, Aula, Plaza, Tramo

### `Subgrupo`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `GrupoAdministrativo grupo`
- Constructor compacto: `public Subgrupo { … }`
- Consume: GrupoAdministrativo

### `TipoGrupo`
- Visibilidad: public — enum
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: (ninguno)

### `Tramo`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Constructor compacto: `public Tramo { … }`
- Consume: (ninguno)

---

## Paquete `cpsat`

Paquete: `es.yaroki.educhronos.solver.cpsat`.

### `AulaOpcion` — `solver/src/main/java`
- Visibilidad: package-private — record
- Componentes:
  - `Aula aula`
  - `BoolVar presencia` (com.google.ortools.sat.BoolVar)
  - `IntervalVar intervalo` (com.google.ortools.sat.IntervalVar)
- Constructor compacto: `AulaOpcion { … }`
- Consume: Aula

### `Expansion` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: Actividad, ActividadInstancia, ProblemaHorario

### `HorarioInfactibleException` — `solver/src/main/java`
- Visibilidad: public — class (extends RuntimeException)
- Constructores:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: (ninguno)

### `InstanciaProgramada` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: ActividadInstancia, AulaOpcion, Plaza

### `ModeloCpSat` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: Actividad, ActividadInstancia, Aula, GrupoAdministrativo, PatronTemporal, Plaza, ProblemaHorario, Profesor, SolucionHorario, Subgrupo, Tramo, AulaOpcion, Expansion, HorarioInfactibleException, InstanciaProgramada

### `ResultadoVerificacion` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `List<String> violaciones`
- Constructor compacto: `public ResultadoVerificacion { … }`
- Métodos:
  - `public boolean esValida()`
- Consume: (ninguno)

### `SolverHorario` — `solver/src/main/java`
- Visibilidad: public — final class
- Constructores:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `public SolucionHorario resolver(ProblemaHorario problema)`
- Consume: HorarioInfactibleException, ModeloCpSat, ProblemaHorario, SolucionHorario

### `VerificadorSolucion` — `solver/src/main/java`
- Visibilidad: public — final class
- Métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: Actividad, ActividadInstancia, Aula, GrupoAdministrativo, PatronTemporal, Plaza, ProblemaHorario, Profesor, SolucionHorario, Subgrupo, Tramo, Expansion, ResultadoVerificacion

### `RestriccionNoSolapeGrupoTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: ActividadInstancia, ProblemaHorario, SolucionHorario, Tramo, ProblemaHorarioJsonLoader, Expansion, SolverHorario, ResultadoVerificacion, VerificadorSolucion, HorarioInfactibleException

### `SolverHorario1EsoOrdinariasTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: ProblemaHorario, SolucionHorario, ProblemaHorarioJsonLoader, SolverHorario, ResultadoVerificacion, VerificadorSolucion

### `SolverHorarioAulaCandidataTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: Actividad, ActividadInstancia, Aula, Plaza, ProblemaHorario, SolucionHorario, ProblemaHorarioJsonLoader, SolverHorario, VerificadorSolucion, HorarioInfactibleException

### `SolverHorarioCierreFase3Test` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: Actividad, ActividadInstancia, Aula, Plaza, ProblemaHorario, SolucionHorario, Tramo, ProblemaHorarioJsonLoader, SolverHorario, VerificadorSolucion

### `SolverHorarioCierreFase4Test` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: Actividad, ActividadInstancia, GrupoAdministrativo, Plaza, ProblemaHorario, SolucionHorario, Subgrupo, Tramo, ProblemaHorarioJsonLoader, SolverHorario, VerificadorSolucion

### `SolverHorarioTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: ActividadInstancia, ProblemaHorario, SolucionHorario, Tramo, ProblemaHorarioJsonLoader, Expansion, SolverHorario, ResultadoVerificacion, VerificadorSolucion

### `VerificadorSolucionGrupoTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: Actividad, ActividadInstancia, Aula, Asignatura, GrupoAdministrativo, PatronTemporal, Plaza, Profesor, ProblemaHorario, SolucionHorario, Subgrupo, TipoGrupo, Tramo, Expansion, ResultadoVerificacion, VerificadorSolucion

---

## Paquete `io`

Paquete: `es.yaroki.educhronos.solver.io`.

### `ActividadDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: PlazaDto

### `AsignaturaDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### `AulaDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### `GrupoDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: (ninguno)

### `PlazaDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: (ninguno)

### `ProblemaHorarioDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `List<TramoDto> tramos`
  - `List<AulaDto> aulas`
  - `List<AsignaturaDto> asignaturas`
  - `List<ProfesorDto> profesores`
  - `List<GrupoDto> grupos`
  - `List<SubgrupoDto> subgrupos`
  - `List<ActividadDto> actividades`
- Consume: ActividadDto, AsignaturaDto, AulaDto, GrupoDto, ProfesorDto, SubgrupoDto, TramoDto

### `ProblemaHorarioJsonLoader` — `solver/src/main/java`
- Visibilidad: public — final class
- Constructores:
  - `public ProblemaHorarioJsonLoader()`
- Métodos:
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: ProblemaHorario, ProblemaHorarioDto, ProblemaHorarioMapper, ProblemaInvalidoException

### `ProblemaHorarioMapper` — `solver/src/main/java`
- Visibilidad: public — final class
- Métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: Actividad, Asignatura, Aula, GrupoAdministrativo, PatronTemporal, Plaza, ProblemaHorario, Profesor, Subgrupo, TipoGrupo, Tramo, ProblemaHorarioDto, TramoDto, AulaDto, AsignaturaDto, ProfesorDto, GrupoDto, SubgrupoDto, ActividadDto, PlazaDto, ProblemaInvalidoException

### `ProblemaInvalidoException` — `solver/src/main/java`
- Visibilidad: public — class (extends RuntimeException)
- Constructores:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: (ninguno)

### `ProfesorDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: (ninguno)

### `SubgrupoDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `String grupo`
- Consume: (ninguno)

### `TramoDto` — `solver/src/main/java`
- Visibilidad: public — record
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: (ninguno)

### `ProblemaHorarioJsonLoaderTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void cargaAulasCandidatasResueltas() throws Exception`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- Consume: Actividad, Aula, Plaza, Profesor, ProblemaHorario, ProblemaHorarioJsonLoader, ProblemaInvalidoException

---

## Paquete `cli`

Paquete: `es.yaroki.educhronos.solver.cli`.

### `CodigoSalida` — `solver/src/main/java`
- Visibilidad: package-private — enum
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: (ninguno)

### `FormatoCelda` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: Profesor, SesionMaterializada

### `HelloOrTools` — `solver/src/main/java`
- Visibilidad: public — class
- Métodos:
  - `public static void main(String[] args)`
- Consume: (ninguno)

### `HorarioPrinter` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: ProblemaHorario, Tramo, SesionMaterializada, VistaHorario

### `Main` — `solver/src/main/java`
- Visibilidad: public — final class
- Métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: HorarioInfactibleException, ResultadoVerificacion, SolverHorario, VerificadorSolucion, ProblemaHorario, SolucionHorario, ProblemaHorarioJsonLoader, ProblemaInvalidoException, CodigoSalida, VerificacionPrinter, Materializador, HorarioPrinter, SesionMaterializada, VistaPorGrupo, VistaPorProfesor

### `Materializador` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: SesionMaterializada, SolucionHorario

### `SesionMaterializada` — `solver/src/main/java`
- Visibilidad: package-private — record
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Constructor compacto: `SesionMaterializada { … }`
- Consume: ActividadInstancia, Plaza, Tramo

### `VerificacionPrinter` — `solver/src/main/java`
- Visibilidad: package-private — final class
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: ResultadoVerificacion

### `VistaHorario` — `solver/src/main/java`
- Visibilidad: package-private — interface (`VistaHorario<K>`)
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: ProblemaHorario, SesionMaterializada

### `VistaPorGrupo` — `solver/src/main/java`
- Visibilidad: package-private — final class (implements `VistaHorario<GrupoAdministrativo>`)
- Métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: GrupoAdministrativo, ProblemaHorario, Subgrupo, VistaHorario, SesionMaterializada, FormatoCelda

### `VistaPorProfesor` — `solver/src/main/java`
- Visibilidad: package-private — final class (implements `VistaHorario<Profesor>`)
- Métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: Profesor, ProblemaHorario, VistaHorario, SesionMaterializada, FormatoCelda

### `Main1EsoOrdinariasTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void endToEnd_codigoSalidaOk(Path tempDir) throws Exception`
- Consume: Main

### `MainTest` — `solver/src/test/java`
- Visibilidad: package-private — class
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: Main
