# Referencia de código — módulo `solver/`

Índice de API construido a partir de las firmas del código fuente. Solo
miembros con visibilidad `public`, `protected` o package-private (sin
modificador); los miembros `private` se omiten.

- Fecha: 2026-06-20
- Commit: `532c95e`

Ámbito: todos los `.java` de `solver/src/main/java` y `solver/src/test/java`.
Orden: paquete (`domain`, `cpsat`, `io`, `cli`); dentro de cada paquete,
alfabético. "Consume:" lista los tipos `es.yaroki.educhronos.solver.*` que
aparecen en las firmas o imports del tipo.

---

## Paquete `domain`

### `Actividad`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Optional<Asignatura> asignatura`
  - `int repeticionesPorSemana`
  - `int duracionTramos`
  - `PatronTemporal patronTemporal`
  - `List<Plaza> plazas`
- Constructores/métodos:
  - `public Actividad {...}` (constructor compacto)
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `Actividad actividad`
  - `int indice`
- Constructores/métodos:
  - `public ActividadInstancia {...}` (constructor compacto)
- Consume: `Actividad`

### `Asignatura`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores/métodos:
  - `public Asignatura {...}` (constructor compacto)
- Consume: —

### `Aula`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores/métodos:
  - `public Aula {...}` (constructor compacto)
- Consume: —

### `GrupoAdministrativo`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `TipoGrupo tipo`
  - `Optional<GrupoAdministrativo> grupoPadre`
- Constructores/métodos:
  - `public GrupoAdministrativo {...}` (constructor compacto)
- Consume: `TipoGrupo`

### `PatronTemporal`
- `public enum` · paquete `es.yaroki.educhronos.solver.domain`
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### `Plaza`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Asignatura asignatura`
  - `Set<Profesor> profesores`
  - `Optional<Aula> aulaFija`
  - `Set<Aula> aulasCandidatas`
  - `Set<Subgrupo> subgrupos`
- Constructores/métodos:
  - `public Plaza {...}` (constructor compacto)
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### `ProblemaHorario`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `List<Tramo> tramos`
  - `List<Aula> aulas`
  - `List<Asignatura> asignaturas`
  - `List<Profesor> profesores`
  - `List<GrupoAdministrativo> grupos`
  - `List<Subgrupo> subgrupos`
  - `List<Actividad> actividades`
- Constructores/métodos:
  - `public ProblemaHorario {...}` (constructor compacto)
  - `public int indiceDeTramo(Tramo tramo)`
- Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`

### `Profesor`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `String nombre`
- Constructores/métodos:
  - `public Profesor {...}` (constructor compacto)
- Consume: —

### `SolucionHorario`
- `public class` · paquete `es.yaroki.educhronos.solver.domain`
- Constructores/métodos:
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
  - `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`
  - `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
  - `public Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`, `Plaza`, `Aula`

### `Subgrupo`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `Set<GrupoAdministrativo> grupos`
- Constructores/métodos:
  - `public Subgrupo {...}` (constructor compacto)
  - `public boolean equals(Object o)`
  - `public int hashCode()`
- Consume: `GrupoAdministrativo`

### `TipoGrupo`
- `public enum` · paquete `es.yaroki.educhronos.solver.domain`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### `Tramo`
- `public final record` · paquete `es.yaroki.educhronos.solver.domain`
- Componentes:
  - `String codigo`
  - `int diaSemana`
  - `int ordenEnDia`
- Constructores/métodos:
  - `public Tramo {...}` (constructor compacto)
- Consume: —

---

## Paquete `cpsat`

### `AulaOpcion`
- `package-private record` · paquete `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `Aula aula`
  - `BoolVar presencia`
  - `IntervalVar intervalo`
- Constructores/métodos:
  - `AulaOpcion {...}` (constructor compacto)
- Consume: `Aula`

### `Expansion`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

### `HorarioInfactibleException`
- `public class` (extends `RuntimeException`) · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `public HorarioInfactibleException(String mensaje)`
- Consume: —

### `InstanciaProgramada`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
  - `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`
- Consume: `ActividadInstancia`, `Plaza`, `AulaOpcion`

### `ModeloCpSat`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `ModeloCpSat(ProblemaHorario problema)`
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `ModeloCpSat construirConObjetivo()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`, `AulaOpcion`, `InstanciaProgramada`, `Expansion`, `HorarioInfactibleException`

### `ResultadoVerificacion`
- `public final record` · paquete `es.yaroki.educhronos.solver.cpsat`
- Componentes:
  - `List<String> violaciones`
- Constructores/métodos:
  - `public ResultadoVerificacion {...}` (constructor compacto)
  - `public boolean esValida()`
- Consume: —

### `SolverHorario`
- `public final class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `public SolverHorario()`
  - `public SolverHorario(double maxSegundos, int semilla)`
  - `public SolucionHorario resolver(ProblemaHorario problema)`
  - `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`, `HorarioInfactibleException`, `VerificadorSolucion`

### `VerificadorSolucion`
- `public final class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
  - `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ResultadoVerificacion`, `Expansion`

---

## Paquete `io`

### `ActividadDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `Integer repeticionesPorSemana`
  - `Integer duracionTramos`
  - `String patronTemporal`
  - `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### `AsignaturaDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `AulaDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `GrupoDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String tipo`
  - `String grupoPadre`
- Consume: —

### `PlazaDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String asignatura`
  - `List<String> profesores`
  - `String aulaFija`
  - `List<String> aulasCandidatas`
  - `List<String> subgrupos`
- Consume: —

### `ProblemaHorarioDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
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
- `public final class` · paquete `es.yaroki.educhronos.solver.io`
- Constructores/métodos:
  - `public ProblemaHorarioJsonLoader()`
  - `public ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`, `ProblemaHorarioDto`, `ProblemaHorarioMapper`, `ProblemaInvalidoException`

### `ProblemaHorarioMapper`
- `public final class` · paquete `es.yaroki.educhronos.solver.io`
- Constructores/métodos:
  - `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `Subgrupo`, `TipoGrupo`, `Tramo`, `ProblemaHorarioDto`, `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`, `PlazaDto`, `ProblemaInvalidoException`

### `ProblemaInvalidoException`
- `public class` (extends `RuntimeException`) · paquete `es.yaroki.educhronos.solver.io`
- Constructores/métodos:
  - `public ProblemaInvalidoException(String mensaje)`
  - `public ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### `ProfesorDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `String nombre`
- Consume: —

### `SubgrupoDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `List<String> grupos`
- Consume: —

### `TramoDto`
- `public final record` · paquete `es.yaroki.educhronos.solver.io`
- Componentes:
  - `String codigo`
  - `Integer diaSemana`
  - `Integer ordenEnDia`
- Consume: —

---

## Paquete `cli`

### `CodigoSalida`
- `package-private enum` · paquete `es.yaroki.educhronos.solver.cli`
- Constantes: `OK(0)`, `INFACTIBLE(1)`, `ENTRADA_INVALIDA(2)`, `VIOLACIONES_DURAS(3)`
- Constructores/métodos:
  - `CodigoSalida(int valor)`
  - `int valor()`
- Consume: —

### `FormatoCelda`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `Profesor`, `SesionMaterializada`

### `HelloOrTools`
- `public class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `public static void main(String[] args)`
- Consume: —

### `HorarioPrinter`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### `Main`
- `public final class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `public static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `SesionMaterializada`, `Materializador`, `VistaPorGrupo`, `VistaPorProfesor`, `VerificacionPrinter`, `HorarioPrinter`, `CodigoSalida`

### `Materializador`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### `SesionMaterializada`
- `package-private record` · paquete `es.yaroki.educhronos.solver.cli`
- Componentes:
  - `Tramo tramo`
  - `ActividadInstancia instancia`
  - `Plaza plaza`
- Constructores/métodos:
  - `SesionMaterializada {...}` (constructor compacto)
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### `VerificacionPrinter`
- `package-private final class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### `VistaHorario`
- `package-private interface` (`VistaHorario<K>`) · paquete `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo`
- `package-private final class` (`implements VistaHorario<GrupoAdministrativo>`) · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `public String titulo()`
  - `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(GrupoAdministrativo clave)`
  - `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

### `VistaPorProfesor`
- `package-private final class` (`implements VistaHorario<Profesor>`) · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `public String titulo()`
  - `public List<Profesor> filas(ProblemaHorario problema)`
  - `public String etiquetaFila(Profesor clave)`
  - `public Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `public String contenidoCelda(SesionMaterializada sesion)`
- Consume: `Profesor`, `ProblemaHorario`, `VistaHorario`, `SesionMaterializada`, `FormatoCelda`

---

## Tests (`solver/src/test/java`)

### Paquete `cli`

#### `Main1EsoOrdinariasTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception`
- Consume: —

#### `MainTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cli`
- Constructores/métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: —

### Paquete `cpsat`

#### `RestriccionNoSolapeGrupoTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorario1EsoOrdinariasTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioAulaCandidataTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void candidatasEligeAulaLibre() throws Exception`
  - `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
  - `void candidataUnicaCompartidaEsInfactible() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioCierreFase3Test`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void cierreDeFase3() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioCierreFase4Test`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void cierreDeFase4() throws Exception`
- Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioEscalaInstitutoTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void escala1y2y3ESOconPDC() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioLecturaBTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void bloqueOptativasMultigrupoEsFactible() throws Exception`
  - `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioReligionParejasTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void cierreBloque1Fase5() throws Exception`
- Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `SolverHorarioVentanasProfesorTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
  - `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

#### `VerificadorSolucionGrupoTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.cpsat`
- Constructores/métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

### Paquete `io`

#### `ProblemaHorarioJsonLoaderTest`
- `package-private class` · paquete `es.yaroki.educhronos.solver.io`
- Constructores/métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void cargaAulasCandidatasResueltas() throws Exception`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- Consume: `Actividad`, `Aula`, `Plaza`, `Profesor`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`
