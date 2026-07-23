# Referencia de código — módulo `solver/`

Índice de API generado exclusivamente a partir del código fuente.

- Fecha: 2026-07-23
- Commit: `dc2ab65`

Visibilidad: `public`, package-private (sin modificador). Se omiten todos los
miembros `private`. La línea **Consume** lista los tipos del módulo
(`es.yaroki.educhronos.solver.*`) que aparecen en firmas o imports del tipo.

---

## Paquete `domain`

### `Actividad` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `Optional<Asignatura> asignatura`
- `int repeticionesPorSemana`
- `int duracionTramos`
- `PatronTemporal patronTemporal`
- `List<Plaza> plazas`
- `boolean requiereTutor`

Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `Actividad actividad`
- `int indice`

Consume: `Actividad`

### `Asignatura` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `Aula` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `GrupoAdministrativo` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `TipoGrupo tipo`
- `Optional<GrupoAdministrativo> grupoPadre`

Consume: `TipoGrupo`

### `PatronTemporal` — public enum
Paquete: `es.yaroki.educhronos.solver.domain`
Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`

Consume: (ninguno)

### `Plaza` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `Asignatura asignatura`
- `Set<Profesor> profesores`
- `Optional<Aula> aulaFija`
- `Set<Aula> aulasCandidatas`
- `Set<Subgrupo> subgrupos`

Consume: `Asignatura`, `Aula`, `Profesor`, `Subgrupo`

### `ProblemaHorario` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `List<Tramo> tramos`
- `List<Aula> aulas`
- `List<Asignatura> asignaturas`
- `List<Profesor> profesores`
- `List<GrupoAdministrativo> grupos`
- `List<Subgrupo> subgrupos`
- `List<Actividad> actividades`
- `List<RestriccionHoraria> restriccionesHorarias`
- `List<SesionBloqueada> bloqueos`
- `List<ProfesorTutoria> tutorias`

Métodos:
- `public int indiceDeTramo(Tramo tramo)`

Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `Profesor`, `ProfesorTutoria`, `RestriccionHoraria`, `SesionBloqueada`, `Subgrupo`, `Tramo`

### `Profesor` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `ProfesorTutoria` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `Profesor profesor`
- `GrupoAdministrativo grupo`
- `RolTutoria rol`

Consume: `GrupoAdministrativo`, `Profesor`, `RolTutoria`

### `RestriccionHoraria` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `Profesor profesor`
- `Tramo tramo`
- `TipoRestriccion tipo`
- `int peso`
- `Optional<String> motivo`

Consume: `Profesor`, `TipoRestriccion`, `Tramo`

### `RolTutoria` — public enum
Paquete: `es.yaroki.educhronos.solver.domain`
Constantes: `TUTOR_PRINCIPAL`, `CO_TUTOR`

Consume: (ninguno)

### `SesionBloqueada` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `ActividadInstancia instancia`
- `Tramo tramo`
- `Map<Plaza, Aula> aulasPinadas`

Consume: `ActividadInstancia`, `Aula`, `Plaza`, `Tramo`

### `SolucionHorario` — public class
Paquete: `es.yaroki.educhronos.solver.domain`
Constructores:
- `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
- `public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones, Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas)`

Métodos:
- `public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
- `public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza)`
- `public Map<ActividadInstancia, Tramo> asignaciones()`
- `public Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas()`

Consume: `ActividadInstancia`, `Aula`, `Plaza`, `Tramo`

### `Subgrupo` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `Set<GrupoAdministrativo> grupos`

Métodos:
- `public boolean equals(Object o)`
- `public int hashCode()`

Consume: `GrupoAdministrativo`

### `TipoGrupo` — public enum
Paquete: `es.yaroki.educhronos.solver.domain`
Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`

Consume: (ninguno)

### `TipoRestriccion` — public enum
Paquete: `es.yaroki.educhronos.solver.domain`
Constantes: `DURA`, `BLANDA`

Consume: (ninguno)

### `Tramo` — public record
Paquete: `es.yaroki.educhronos.solver.domain`
Componentes:
- `String codigo`
- `int diaSemana`
- `int ordenEnDia`

Consume: (ninguno)

---

## Paquete `cpsat`

### `AtribucionBlanda` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `Map<CeldaRef, List<Penalizacion>> porCelda`

Métodos:
- `public int deltaTotal(CeldaRef celda)`

Consume: `CeldaRef`, `Penalizacion`

### `AulaOpcion` — package-private record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `Aula aula`
- `BoolVar presencia`
- `IntervalVar intervalo`

Consume: `Aula`

### `CeldaRef` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `String actividadCodigo`
- `int indice`
- `String plazaCodigo`

Consume: (ninguno)

### `Expansion` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
- `static List<ActividadInstancia> todas(ProblemaHorario problema)`

Consume: `Actividad`, `ActividadInstancia`, `ProblemaHorario`

### `HorarioInfactibleException` — public class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Extiende: `RuntimeException`
Constructores:
- `public HorarioInfactibleException(String mensaje)`

Consume: (ninguno)

### `InstanciaProgramada` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Constructores:
- `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo, Map<Plaza, List<AulaOpcion>> opcionesDeAula)`

Métodos:
- `ActividadInstancia instancia()`
- `IntVar tramoIndex()`
- `IntervalVar intervalo()`
- `Map<Plaza, List<AulaOpcion>> opcionesDeAula()`

Consume: `ActividadInstancia`, `AulaOpcion`, `Plaza`

### `ModeloCpSat` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Constructores:
- `ModeloCpSat(ProblemaHorario problema)`

Métodos:
- `CpModel model()`
- `ModeloCpSat construir()`
- `ModeloCpSat construirConObjetivo()`
- `ModeloCpSat construirConObjetivo(boolean podar)`
- `ModeloCpSat sembrarHint(SolucionHorario semilla)`
- `SolucionHorario extraerSolucion(CpSolver solver)`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `RestriccionHoraria`, `SesionBloqueada`, `SolucionHorario`, `Subgrupo`, `TipoRestriccion`, `Tramo`

### `Penalizacion` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `ReglaBlanda regla`
- `String recursoCodigo`
- `String tramoCodigo`
- `int delta`
- `String descripcion`

Consume: `ReglaBlanda`

### `ReglaBlanda` — public enum
Paquete: `es.yaroki.educhronos.solver.cpsat`
Constantes: `VENTANA_PROFESOR`, `INDISPONIBILIDAD_BLANDA`, `EXCESO_CONSECUTIVAS`

Consume: (ninguno)

### `ReglaDura` — public enum
Paquete: `es.yaroki.educhronos.solver.cpsat`
Constantes: `INSTANCIA_SIN_COLOCAR`, `BLOQUE_IMPOSIBLE`, `SOLAPE_PROFESOR`, `SOLAPE_AULA`, `SOLAPE_SUBGRUPO`, `SOLAPE_GRUPO`, `DISTRIBUCION_MISMO_DIA`, `TUTORIA_SIN_TUTOR`

Consume: (ninguno)

### `ResultadoOptimizacion` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `SolucionHorario solucion`
- `CpSolverStatus estado`
- `double objetivo`
- `double cotaInferior`

Métodos:
- `public boolean esOptimo()`

Consume: `SolucionHorario`

### `ResultadoVerificacion` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `List<Violacion> violaciones`

Métodos:
- `public boolean esValida()`

Consume: `Violacion`

### `SolverHorario` — public final class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Constructores:
- `public SolverHorario()`
- `public SolverHorario(double maxSegundos, int semilla)`

Métodos:
- `public SolucionHorario resolver(ProblemaHorario problema)`
- `public SolucionHorario resolverOptimizando(ProblemaHorario problema)`
- `public ResultadoOptimizacion resolverOptimizandoConDetalle(ProblemaHorario problema)`
- `public ResultadoOptimizacion resolverOptimizandoConSemilla(ProblemaHorario problema, SolucionHorario semilla)`

Consume: `ProblemaHorario`, `ResultadoOptimizacion`, `SolucionHorario`

### `VerificadorSolucion` — public final class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
- `public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema, SolucionHorario solucion)`
- `public AtribucionBlanda atribuirBlandas(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarBloqueosViolados(ProblemaHorario problema, SolucionHorario solucion)`
- `public int contarAulasBloqueadasVioladas(ProblemaHorario problema, SolucionHorario solucion)`
- `static int ventanasDe(Set<Integer> posicionesDelDia)`
- `static int excesoConsecutivasDe(Set<Integer> posicionesDelDia, int n)`

Consume: `Actividad`, `ActividadInstancia`, `AtribucionBlanda`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `ProfesorTutoria`, `RestriccionHoraria`, `ResultadoVerificacion`, `RolTutoria`, `SesionBloqueada`, `SolucionHorario`, `Subgrupo`, `TipoRestriccion`, `Tramo`

### `Violacion` — public record
Paquete: `es.yaroki.educhronos.solver.cpsat`
Componentes:
- `ReglaDura regla`
- `String recursoCodigo`
- `String tramoCodigo`
- `List<CeldaRef> celdas`
- `String descripcion`

Consume: `CeldaRef`, `ReglaDura`

---

## Paquete `io`

### `ActividadDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String asignatura`
- `Integer repeticionesPorSemana`
- `Integer duracionTramos`
- `String patronTemporal`
- `List<PlazaDto> plazas`
- `Boolean requiereTutor`

Consume: `PlazaDto`

### `AsignaturaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `AulaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `AulaPinDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String plaza`
- `String aula`

Consume: (ninguno)

### `GrupoDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String tipo`
- `String grupoPadre`

Consume: (ninguno)

### `PlazaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String asignatura`
- `List<String> profesores`
- `String aulaFija`
- `List<String> aulasCandidatas`
- `List<String> subgrupos`

Consume: (ninguno)

### `ProblemaHorarioDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `List<TramoDto> tramos`
- `List<AulaDto> aulas`
- `List<AsignaturaDto> asignaturas`
- `List<ProfesorDto> profesores`
- `List<GrupoDto> grupos`
- `List<SubgrupoDto> subgrupos`
- `List<ActividadDto> actividades`
- `List<RestriccionHorariaDto> restriccionesHorarias`
- `List<SesionBloqueadaDto> bloqueos`
- `List<ProfesorTutoriaDto> tutorias`

Consume: `ActividadDto`, `AsignaturaDto`, `AulaDto`, `GrupoDto`, `ProfesorDto`, `ProfesorTutoriaDto`, `RestriccionHorariaDto`, `SesionBloqueadaDto`, `SubgrupoDto`, `TramoDto`

### `ProblemaHorarioJsonLoader` — public final class
Paquete: `es.yaroki.educhronos.solver.io`
Constructores:
- `public ProblemaHorarioJsonLoader()`

Métodos:
- `public ProblemaHorario cargar(InputStream entrada)`

Consume: `ProblemaHorario`

### `ProblemaHorarioMapper` — public final class
Paquete: `es.yaroki.educhronos.solver.io`
Métodos:
- `public static ProblemaHorario aDominio(ProblemaHorarioDto dto)`

Consume: `Actividad`, `ActividadInstancia`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioDto`, `Profesor`, `ProfesorTutoria`, `RestriccionHoraria`, `RolTutoria`, `SesionBloqueada`, `Subgrupo`, `TipoGrupo`, `TipoRestriccion`, `Tramo`

### `ProblemaInvalidoException` — public class
Paquete: `es.yaroki.educhronos.solver.io`
Extiende: `RuntimeException`
Constructores:
- `public ProblemaInvalidoException(String mensaje)`
- `public ProblemaInvalidoException(String mensaje, Throwable causa)`

Consume: (ninguno)

### `ProfesorDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `String nombre`

Consume: (ninguno)

### `ProfesorTutoriaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String profesor`
- `String grupo`
- `String rol`

Consume: (ninguno)

### `RestriccionHorariaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String profesor`
- `String tramo`
- `String tipo`
- `Integer peso`
- `String motivo`

Consume: (ninguno)

### `SesionBloqueadaDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String actividad`
- `int indice`
- `String tramo`
- `List<AulaPinDto> aulasPinadas`

Consume: `AulaPinDto`

### `SubgrupoDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `List<String> grupos`

Consume: (ninguno)

### `TramoDto` — public record
Paquete: `es.yaroki.educhronos.solver.io`
Componentes:
- `String codigo`
- `Integer diaSemana`
- `Integer ordenEnDia`

Consume: (ninguno)

---

## Paquete `cli`

### `CodigoSalida` — package-private enum
Paquete: `es.yaroki.educhronos.solver.cli`
Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
Métodos:
- `int valor()`

Consume: (ninguno)

### `FormatoCelda` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `static String formatear(SesionMaterializada sesion)`

Consume: `Profesor`, `SesionMaterializada`

### `HelloOrTools` — public class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `public static void main(String[] args)`

Consume: (ninguno)

### `HorarioPrinter` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`

Consume: `ProblemaHorario`, `SesionMaterializada`, `Tramo`, `VistaHorario`

### `Main` — public final class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `public static void main(String[] args)`
- `static int ejecutar(String[] args, PrintStream out, PrintStream err)`

Consume: `HorarioInfactibleException`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`, `ResultadoVerificacion`, `SolucionHorario`, `SolverHorario`, `VerificadorSolucion`

### `Materializador` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `static List<SesionMaterializada> materializar(SolucionHorario solucion)`

Consume: `SesionMaterializada`, `SolucionHorario`

### `SesionMaterializada` — package-private record
Paquete: `es.yaroki.educhronos.solver.cli`
Componentes:
- `Tramo tramo`
- `ActividadInstancia instancia`
- `Plaza plaza`

Consume: `ActividadInstancia`, `Plaza`, `Tramo`

### `VerificacionPrinter` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`

Consume: `ResultadoVerificacion`, `Violacion`

### `VistaHorario` — package-private interface
Paquete: `es.yaroki.educhronos.solver.cli`
Parámetro de tipo: `<K>`
Métodos:
- `String titulo()`
- `List<K> filas(ProblemaHorario problema)`
- `String etiquetaFila(K clave)`
- `Set<K> filasDe(SesionMaterializada sesion)`
- `String contenidoCelda(SesionMaterializada sesion)`

Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Implementa: `VistaHorario<GrupoAdministrativo>`
Métodos:
- `public String titulo()`
- `public List<GrupoAdministrativo> filas(ProblemaHorario problema)`
- `public String etiquetaFila(GrupoAdministrativo clave)`
- `public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
- `public String contenidoCelda(SesionMaterializada sesion)`

Consume: `GrupoAdministrativo`, `ProblemaHorario`, `SesionMaterializada`, `Subgrupo`, `VistaHorario`

### `VistaPorProfesor` — package-private final class
Paquete: `es.yaroki.educhronos.solver.cli`
Implementa: `VistaHorario<Profesor>`
Métodos:
- `public String titulo()`
- `public List<Profesor> filas(ProblemaHorario problema)`
- `public String etiquetaFila(Profesor clave)`
- `public Set<Profesor> filasDe(SesionMaterializada sesion)`
- `public String contenidoCelda(SesionMaterializada sesion)`

Consume: `ProblemaHorario`, `Profesor`, `SesionMaterializada`, `VistaHorario`

---

## Tests — paquete `cli`

### `Main1EsoOrdinariasTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `void endToEnd_codigoSalidaOk(Path tempDir) throws Exception`

Consume: (ninguno)

### `MainTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cli`
Métodos:
- `void sinArgumentosSaleConCodigo2()`
- `void demasiadosArgumentosSaleConCodigo2()`
- `void ficheroInexistenteSaleConCodigo2()`
- `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
- `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
- `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`

Consume: (ninguno)

---

## Tests — paquete `cpsat`

### `RestriccionNoSolapeGrupoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
- `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorario1EsoOrdinariasTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioAulaCandidataTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void candidatasEligeAulaLibre() throws Exception`
- `void mixtaEnMismoTramoEligeAulaLibre() throws Exception`
- `void candidataUnicaCompartidaEsInfactible() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioBloqueD13Test` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void bloqueDuracion2CabeEnInicioValido() throws IOException`
- `void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException`
- `void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioCierreFase3Test` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void cierreDeFase3() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorarioCierreFase4Test` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void cierreDeFase4() throws Exception`

Consume: `Actividad`, `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Subgrupo`, `Tramo`

### `SolverHorarioConsecutivasProfesorTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorarioDetalleOptimizacionTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void detalleReportaOptimoProbadoYObjetivoPositivoConcordante() throws Exception`
- `void detalleReportaOptimoCeroSinVeto() throws Exception`
- `void resolverOptimizandoClasicoDevuelveLaMismaSolucionQueElDetalle() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscala1BachTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscala1FpbTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscala2FpbTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void resuelveFactibleSinViolaciones() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscala4EsoDiTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void escala4EsoCompleto() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscala4EsoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void escala4EsoOrdinario() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioEscalaInstitutoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void escala1y2y3ESOconPDC() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioFusion34EsoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void fusion34Eso() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioFusionEsoCompletaTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void fusionEsoCompleta() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioFusionInstitutoCompletoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void fusionInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioIndisponibilidadBlandaProfesorTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorarioIndisponibilidadProfesorTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception`
- `void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception`
- `void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorarioLecturaBTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void bloqueOptativasMultigrupoEsFactible() throws Exception`
- `void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception`

Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Subgrupo`

### `SolverHorarioOptimizacionEscalaInstitutoCompletoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void optimizacionInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `Profesor`, `SolucionHorario`

### `SolverHorarioOptimizacionEscalaSubconjuntosTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void p0base() throws Exception`
- `void p1SinFpb() throws Exception`
- `void p2SoloEso() throws Exception`

Consume: `Actividad`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Subgrupo`

### `SolverHorarioOroFuerteConsecutivasTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioOroFuerteIndispBlandaTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `SolverHorarioOroFuerteVentanasTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception`
- `void sinElVetoElOptimoEsCero_discriminacion() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `Profesor`, `SolucionHorario`

### `SolverHorarioPinAulaTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void pinRespetado_laPlazaCaeEnElAulaPinada() throws Exception`
- `void pinDesdoble_lasDosPlazasCaenEnAulasDistintasPinadas() throws Exception`
- `void sinPinDeAula_elSolverEsLibre() throws Exception`

Consume: `ActividadInstancia`, `Aula`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SesionBloqueada`, `SolucionHorario`

### `SolverHorarioPinTramoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void pinRespetado_laInstanciaCaeEnElTramoPinado() throws Exception`
- `void sinPin_elSolverEsLibre() throws Exception`
- `void pinDesdoble_lasDosPlazasCaenSimultaneasEnElTramoPinado() throws Exception`
- `void pinInfactible_lanzaHorarioInfactibleException() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SesionBloqueada`, `SolucionHorario`, `Tramo`

### `SolverHorarioReligionParejasTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void cierreBloque1Fase5() throws Exception`

Consume: `ActividadInstancia`, `GrupoAdministrativo`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Subgrupo`, `Tramo`

### `SolverHorarioTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
- `void todasLasInstanciasQuedanColocadas() throws Exception`
- `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
- `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`, `Tramo`

### `SolverHorarioVentanasProfesorTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception`
- `void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception`

Consume: `ActividadInstancia`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `Profesor`, `SolucionHorario`, `Tramo`

### `SolverHorarioWarmStartInstitutoCompletoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void warmStartInstitutoCompleto() throws Exception`

Consume: `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `SolucionHorario`

### `VerificadorSolucionAtribucionBlandaTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void ventanaProfesor_celdaDelMedioTapaHueco_deltaExactamenteMenosUno()`
- `void ventanaProfesor_celdaEnPuntaDeHueco_deltaExactamenteMasUno()`
- `void gemelosTrasExtraccion_devuelvenValoresConocidos()`
- `void indisponibilidadBlanda_sesionEnTramoVetado_deltaUnoConTramoNoNull() throws Exception`
- `void celdaSinPenalizacion_noApareceEnElMapa()`

Consume: `Actividad`, `ActividadInstancia`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `Profesor`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

### `VerificadorSolucionAtribucionTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void solapeDeProfesorAtribuyeLasDosCeldasCulpablesConPlazaNull()`
- `void solapeDeAulaAtribuyeCeldasConPlazaNoNull()`

Consume: `Actividad`, `ActividadInstancia`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

### `VerificadorSolucionGrupoTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void reportaSolapeDeGrupoEntreActividadesDistintas()`
- `void noReportaGrupoEnTramosDistintos()`
- `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`

Consume: `Actividad`, `ActividadInstancia`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

### `VerificadorSolucionTutoriaTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.cpsat`
Métodos:
- `void tutorPrincipalDelGrupoCubierto_noViola()`
- `void mismoProfesorPeroCoTutor_viola()`
- `void tutorPrincipalDeOtroGrupo_viola()`
- `void requiereTutorFalse_noViola()`
- `void coDocenciaConUnSoloTutor_noViola()`
- `void violacionLlevaTramoNullYRecursoElGrupoCubierto()`
- `void fixtureS8_soloViolaLaActividadCoTutor() throws Exception`

Consume: `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `ProblemaHorarioJsonLoader`, `Profesor`, `ProfesorTutoria`, `RolTutoria`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Tests — paquete `io`

### `ProblemaHorarioJsonLoaderTest` — package-private class
Paquete: `es.yaroki.educhronos.solver.io`
Métodos:
- `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
- `void rechazaReferenciaAProfesorInexistente()`
- `void rechazaPlazaSinProfesores()`
- `void cargaAulasCandidatasResueltas() throws Exception`
- `void rechazaGrupoPdcSinGrupoPadre()`
- `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- `void cargaRestriccionHorariaDuraResuelta() throws Exception`
- `void cargaProblemaSinRestriccionesHorarias() throws Exception`
- `void rechazaRestriccionHorariaConTramoInexistente()`
- `void cargaBloqueoResuelto() throws Exception`
- `void rechazaBloqueoConIndiceFueraDeRango()`
- `void cargaBloqueoConPinDeAula() throws Exception`
- `void rechazaPinDeAulaSobrePlazaConAulaFija()`
- `void rechazaPinDeAulaFueraDeCandidatas()`
- `void cargaTutoriasResueltasConProfesorYGrupoCorrectos() throws Exception`
- `void cargaProblemaSinTutorias() throws Exception`
- `void requiereTutorAusenteEsFalseYPresenteViaja() throws Exception`
- `void rechazaTutoriaConRolDesconocido()`
- `void rechazaTutoriaConProfesorInexistente()`
- `void rechazaTutoriaConGrupoInexistente()`

Consume: `Actividad`, `Aula`, `Plaza`, `ProblemaHorario`, `Profesor`, `ProfesorTutoria`, `RestriccionHoraria`, `RolTutoria`, `SesionBloqueada`, `TipoRestriccion`
