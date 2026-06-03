# Índice de API — módulo `solver/`

- **Fecha:** 2026-06-02
- **Commit:** `405aa69`

Recorre `solver/src/main/java` y `solver/src/test/java`. Ordenado por paquete y, dentro de cada paquete, alfabéticamente. "Consume:" lista los tipos de `es.yaroki.educhronos.solver.*` presentes en firmas o imports.

---

## Paquete `domain`

### `Actividad`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `Optional<Asignatura> asignatura`, `int repeticionesPorSemana`, `int duracionTramos`, `PatronTemporal patronTemporal`, `List<Plaza> plazas`
- Consume: `Asignatura`, `PatronTemporal`, `Plaza`

### `ActividadInstancia`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `Actividad actividad`, `int indice`
- Consume: `Actividad`

### `Asignatura`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `Aula`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `GrupoAdministrativo`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `TipoGrupo tipo`, `Optional<GrupoAdministrativo> grupoPadre`
- Consume: `TipoGrupo`, `GrupoAdministrativo`

### `PatronTemporal`
- public enum · `es.yaroki.educhronos.solver.domain`
- Constantes: `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`
- Consume: —

### `Plaza`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `Asignatura asignatura`, `Set<Profesor> profesores`, `Optional<Aula> aulaFija`, `Set<Aula> aulasCandidatas`, `Set<Subgrupo> subgrupos`
- Consume: `Asignatura`, `Profesor`, `Aula`, `Subgrupo`

### `ProblemaHorario`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `List<Tramo> tramos`, `List<Aula> aulas`, `List<Asignatura> asignaturas`, `List<Profesor> profesores`, `List<GrupoAdministrativo> grupos`, `List<Subgrupo> subgrupos`, `List<Actividad> actividades`
- Métodos:
  - `int indiceDeTramo(Tramo tramo)`
- Consume: `Tramo`, `Aula`, `Asignatura`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Actividad`

### `Profesor`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `SolucionHorario`
- public class · `es.yaroki.educhronos.solver.domain`
- Constructores:
  - `SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)`
- Métodos:
  - `Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)`
  - `Map<ActividadInstancia, Tramo> asignaciones()`
- Consume: `ActividadInstancia`, `Tramo`

### `Subgrupo`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `GrupoAdministrativo grupo`
- Consume: `GrupoAdministrativo`

### `TipoGrupo`
- public enum · `es.yaroki.educhronos.solver.domain`
- Constantes: `ORDINARIO`, `DIVERSIFICACION_PDC`
- Consume: —

### `Tramo`
- public final record · `es.yaroki.educhronos.solver.domain`
- Componentes: `String codigo`, `int diaSemana`, `int ordenEnDia`
- Consume: —

---

## Paquete `cpsat`

### `Expansion`
- package-private final class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `static List<ActividadInstancia> instanciasDe(Actividad actividad)`
  - `static List<ActividadInstancia> todas(ProblemaHorario problema)`
- Consume: `ActividadInstancia`, `Actividad`, `ProblemaHorario`

### `HorarioInfactibleException`
- public class (extends `RuntimeException`) · `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `HorarioInfactibleException(String mensaje)`
- Consume: —

### `InstanciaProgramada`
- package-private final class · `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo)`
- Métodos:
  - `ActividadInstancia instancia()`
  - `IntVar tramoIndex()`
  - `IntervalVar intervalo()`
- Consume: `ActividadInstancia`

### `ModeloCpSat`
- package-private final class · `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `ModeloCpSat(ProblemaHorario problema)`
- Métodos:
  - `CpModel model()`
  - `ModeloCpSat construir()`
  - `SolucionHorario extraerSolucion(CpSolver solver)`
- Consume: `ProblemaHorario`, `SolucionHorario`

### `ResultadoVerificacion`
- public final record · `es.yaroki.educhronos.solver.cpsat`
- Componentes: `List<String> violaciones`
- Métodos:
  - `boolean esValida()`
- Consume: —

### `RestriccionNoSolapeGrupoTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException`
  - `void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `SolverHorario`
- public final class · `es.yaroki.educhronos.solver.cpsat`
- Constructores:
  - `SolverHorario()`
  - `SolverHorario(double maxSegundos, int semilla)`
- Métodos:
  - `SolucionHorario resolver(ProblemaHorario problema)`
- Consume: `ProblemaHorario`, `SolucionHorario`

### `SolverHorario1EsoOrdinariasTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveFactibleSinViolaciones() throws Exception`
- Consume: `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`

### `SolverHorarioTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void resuelveElFixtureMinimoSinViolaciones() throws Exception`
  - `void todasLasInstanciasQuedanColocadas() throws Exception`
  - `void laCoDocenciaOcupaAAmbosProfesores() throws Exception`
  - `void elVerificadorDetectaUnSolapeDeProfesor() throws Exception`
- Consume: `ActividadInstancia`, `ProblemaHorario`, `SolucionHorario`, `Tramo`, `ProblemaHorarioJsonLoader`

### `VerificadorSolucion`
- public final class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)`
- Consume: `ResultadoVerificacion`, `ProblemaHorario`, `SolucionHorario`

### `VerificadorSolucionGrupoTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cpsat`
- Métodos:
  - `void reportaSolapeDeGrupoEntreActividadesDistintas()`
  - `void noReportaGrupoEnTramosDistintos()`
  - `void desdobleNoSeReportaComoSolapeDeGrupo_regresion()`
- Consume: `Actividad`, `ActividadInstancia`, `Aula`, `Asignatura`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `Profesor`, `ProblemaHorario`, `SolucionHorario`, `Subgrupo`, `TipoGrupo`, `Tramo`

---

## Paquete `io`

### `ActividadDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String asignatura`, `Integer repeticionesPorSemana`, `Integer duracionTramos`, `String patronTemporal`, `List<PlazaDto> plazas`
- Consume: `PlazaDto`

### `AsignaturaDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `AulaDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `GrupoDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String tipo`, `String grupoPadre`
- Consume: —

### `PlazaDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String asignatura`, `List<String> profesores`, `String aulaFija`, `List<String> aulasCandidatas`, `List<String> subgrupos`
- Consume: —

### `ProblemaHorarioDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `List<TramoDto> tramos`, `List<AulaDto> aulas`, `List<AsignaturaDto> asignaturas`, `List<ProfesorDto> profesores`, `List<GrupoDto> grupos`, `List<SubgrupoDto> subgrupos`, `List<ActividadDto> actividades`
- Consume: `TramoDto`, `AulaDto`, `AsignaturaDto`, `ProfesorDto`, `GrupoDto`, `SubgrupoDto`, `ActividadDto`

### `ProblemaHorarioJsonLoader`
- public final class · `es.yaroki.educhronos.solver.io`
- Constructores:
  - `ProblemaHorarioJsonLoader()`
- Métodos:
  - `ProblemaHorario cargar(InputStream entrada)`
- Consume: `ProblemaHorario`

### `ProblemaHorarioJsonLoaderTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.io`
- Métodos:
  - `void cargaDatasetMinimoValidoSinExcepciones() throws Exception`
  - `void rechazaReferenciaAProfesorInexistente()`
  - `void rechazaPlazaSinProfesores()`
  - `void rechazaAulasCandidatasHastaFase3()`
  - `void rechazaGrupoPdcSinGrupoPadre()`
  - `void rechazaSubgrupoEnDosPlazasDeLaMismaActividad()`
- Consume: `Actividad`, `Plaza`, `Profesor`, `ProblemaHorario`

### `ProblemaHorarioMapper`
- public final class · `es.yaroki.educhronos.solver.io`
- Métodos:
  - `static ProblemaHorario aDominio(ProblemaHorarioDto dto)`
- Consume: `ProblemaHorarioDto`, `Actividad`, `Asignatura`, `Aula`, `GrupoAdministrativo`, `PatronTemporal`, `Plaza`, `ProblemaHorario`, `Profesor`, `Subgrupo`, `TipoGrupo`, `Tramo`

### `ProblemaInvalidoException`
- public class (extends `RuntimeException`) · `es.yaroki.educhronos.solver.io`
- Constructores:
  - `ProblemaInvalidoException(String mensaje)`
  - `ProblemaInvalidoException(String mensaje, Throwable causa)`
- Consume: —

### `ProfesorDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String nombre`
- Consume: —

### `SubgrupoDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `String grupo`
- Consume: —

### `TramoDto`
- public record · `es.yaroki.educhronos.solver.io`
- Componentes: `String codigo`, `Integer diaSemana`, `Integer ordenEnDia`
- Consume: —

---

## Paquete `cli`

### `CodigoSalida`
- package-private enum · `es.yaroki.educhronos.solver.cli`
- Constantes: `OK`, `INFACTIBLE`, `ENTRADA_INVALIDA`, `VIOLACIONES_DURAS`
- Métodos:
  - `int valor()`
- Consume: —

### `FormatoCelda`
- package-private final class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static String formatear(SesionMaterializada sesion)`
- Consume: `SesionMaterializada`, `Profesor`

### `HelloOrTools`
- public class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static void main(String[] args)`
- Consume: —

### `HorarioPrinter`
- package-private final class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)`
- Consume: `ProblemaHorario`, `Tramo`, `SesionMaterializada`, `VistaHorario`

### `Main`
- public final class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static void main(String[] args)`
  - `static int ejecutar(String[] args, PrintStream out, PrintStream err)`
- Consume: `HorarioInfactibleException`, `ResultadoVerificacion`, `SolverHorario`, `VerificadorSolucion`, `ProblemaHorario`, `SolucionHorario`, `ProblemaHorarioJsonLoader`, `ProblemaInvalidoException`

### `Main1EsoOrdinariasTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void endToEnd_codigoSalidaOk(Path tempDir) throws Exception`
- Consume: —

### `MainTest` *(test)*
- package-private class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `void sinArgumentosSaleConCodigo2()`
  - `void demasiadosArgumentosSaleConCodigo2()`
  - `void ficheroInexistenteSaleConCodigo2()`
  - `void problemaMinimoResolubleSaleConCodigo0() throws Exception`
  - `void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception`
  - `void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception`
- Consume: —

### `Materializador`
- package-private final class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static List<SesionMaterializada> materializar(SolucionHorario solucion)`
- Consume: `SolucionHorario`, `SesionMaterializada`

### `SesionMaterializada`
- package-private record · `es.yaroki.educhronos.solver.cli`
- Componentes: `Tramo tramo`, `ActividadInstancia instancia`, `Plaza plaza`
- Consume: `Tramo`, `ActividadInstancia`, `Plaza`

### `VerificacionPrinter`
- package-private final class · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `static void imprimir(PrintStream out, ResultadoVerificacion resultado)`
- Consume: `ResultadoVerificacion`

### `VistaHorario<K>`
- package-private interface · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<K> filas(ProblemaHorario problema)`
  - `String etiquetaFila(K clave)`
  - `Set<K> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `ProblemaHorario`, `SesionMaterializada`

### `VistaPorGrupo`
- package-private final class (implements `VistaHorario<GrupoAdministrativo>`) · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<GrupoAdministrativo> filas(ProblemaHorario problema)`
  - `String etiquetaFila(GrupoAdministrativo clave)`
  - `Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `VistaHorario`, `GrupoAdministrativo`, `ProblemaHorario`, `Subgrupo`, `SesionMaterializada`

### `VistaPorProfesor`
- package-private final class (implements `VistaHorario<Profesor>`) · `es.yaroki.educhronos.solver.cli`
- Métodos:
  - `String titulo()`
  - `List<Profesor> filas(ProblemaHorario problema)`
  - `String etiquetaFila(Profesor clave)`
  - `Set<Profesor> filasDe(SesionMaterializada sesion)`
  - `String contenidoCelda(SesionMaterializada sesion)`
- Consume: `VistaHorario`, `Profesor`, `ProblemaHorario`, `SesionMaterializada`
