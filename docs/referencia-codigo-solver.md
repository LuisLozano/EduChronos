# Referencia de código: módulo `solver/`

**Generado:** 2026-06-02  
**Commit:** `7cc8c80`  
**Alcance:** `solver/src/main/java` — paquetes `domain`, `cpsat`, `io`, `cli`

---

## Paquete `domain`

### `Tramo` — record public

Franja horaria de un día concreto; unidad atómica del calendario sobre la que el solver asigna sesiones.

**Componentes:**

| Campo | Tipo | Restricción |
|---|---|---|
| `codigo` | `String` | no null |
| `diaSemana` | `int` | 1=lunes … 5=viernes |
| `ordenEnDia` | `int` | 1..6 |

---

### `Aula` — record public

Identificador de un espacio físico docente.

**Componentes:** `String codigo`, `String nombre`

---

### `Asignatura` — record public

Identificador de una materia curricular.

**Componentes:** `String codigo`, `String nombre`

---

### `Profesor` — record public

Identificador de un docente.

**Componentes:** `String codigo`, `String nombre`

---

### `TipoGrupo` — enum public

Categorías de grupo administrativo.

**Constantes:** `ORDINARIO`, `DIVERSIFICACION_PDC`

---

### `GrupoAdministrativo` — record public

Grupo-clase en el sistema administrativo; puede tener un grupo padre (PDC).

**Componentes:**

| Campo | Tipo | Restricción |
|---|---|---|
| `codigo` | `String` | no null |
| `tipo` | `TipoGrupo` | no null |
| `grupoPadre` | `Optional<GrupoAdministrativo>` | obligatorio para `DIVERSIFICACION_PDC` (invariante I5) |

Lanza `IllegalArgumentException` en construcción si `tipo == DIVERSIFICACION_PDC` y `grupoPadre` está vacío.

---

### `Subgrupo` — record public

División de alumnos dentro de un `GrupoAdministrativo`.

**Componentes:** `String codigo`, `GrupoAdministrativo grupo`

---

### `PatronTemporal` — enum public

Preferencia de distribución temporal de las sesiones de una actividad.

**Constantes:** `DISTRIBUIDA`, `AGRUPADA`, `NEUTRA`

---

### `Plaza` — record public

Franja de docencia dentro de una actividad: asignatura, profesores, aula y alumnos cubiertos.

**Componentes:**

| Campo | Tipo | Restricción |
|---|---|---|
| `codigo` | `String` | no null |
| `asignatura` | `Asignatura` | no null |
| `profesores` | `Set<Profesor>` | mínimo 1 (invariante I7); copia defensiva |
| `aulaFija` | `Optional<Aula>` | mutuamente excluyente con `aulasCandidatas` |
| `aulasCandidatas` | `Set<Aula>` | mutuamente excluyente con `aulaFija`; debe haber al menos un aula en alguno de los dos; copia defensiva |
| `subgrupos` | `Set<Subgrupo>` | copia defensiva |

Lanza `IllegalArgumentException` si `profesores` está vacío, si `aulaFija` y `aulasCandidatas` coexisten, o si ambos están ausentes.

---

### `Actividad` — record public

Unidad que el solver coloca en el tiempo; genera tantas `ActividadInstancia` como `repeticionesPorSemana`.

**Componentes:**

| Campo | Tipo | Restricción |
|---|---|---|
| `codigo` | `String` | no null |
| `asignatura` | `Optional<Asignatura>` | vacío en actividades de bloque (ej. Religión/ATED) |
| `repeticionesPorSemana` | `int` | ≥ 1 |
| `duracionTramos` | `int` | ≥ 1 |
| `patronTemporal` | `PatronTemporal` | no null |
| `plazas` | `List<Plaza>` | mínimo 1; copia defensiva |

---

### `ActividadInstancia` — record public

Una ocurrencia concreta de una `Actividad` en la semana.

**Componentes:**

| Campo | Tipo | Restricción |
|---|---|---|
| `actividad` | `Actividad` | no null |
| `indice` | `int` | 1..`actividad.repeticionesPorSemana()` |

---

### `ProblemaHorario` — record public

Entrada completa del solver: agrupa todas las colecciones del dominio.

**Componentes:**

| Campo | Tipo | Nota |
|---|---|---|
| `tramos` | `List<Tramo>` | ordenados por `(diaSemana, ordenEnDia)` |
| `aulas` | `List<Aula>` | |
| `asignaturas` | `List<Asignatura>` | |
| `profesores` | `List<Profesor>` | |
| `grupos` | `List<GrupoAdministrativo>` | |
| `subgrupos` | `List<Subgrupo>` | |
| `actividades` | `List<Actividad>` | |

**Métodos públicos adicionales:**

| Firma | Qué hace |
|---|---|
| `int indiceDeTramo(Tramo tramo)` | Devuelve la posición del tramo en la lista ordenada; lanza `IllegalArgumentException` si el tramo no pertenece al problema. |

---

### `SolucionHorario` — class public

Salida del solver: mapa inmutable `ActividadInstancia → Tramo`.

**Métodos públicos:**

| Firma | Qué hace |
|---|---|
| `SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones)` | Constructor; copia defensiva del mapa. |
| `Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia)` | Devuelve el tramo asignado a la instancia, o vacío si no está asignada. |
| `Map<ActividadInstancia, Tramo> asignaciones()` | Devuelve el mapa completo de asignaciones (inmutable). |

---

## Paquete `cpsat`

### `HorarioInfactibleException` — class public (extends RuntimeException)

Señala que el solver no encontró solución factible o que el modelo CP-SAT es inválido.

**Constructor:**

| Firma | Qué hace |
|---|---|
| `HorarioInfactibleException(String mensaje)` | Construye la excepción con un mensaje descriptivo. |

---

### `ResultadoVerificacion` — record public

Resultado de la verificación de una `SolucionHorario`: lista de descripciones de violaciones de restricciones duras.

**Componentes:** `List<String> violaciones` — inmutable; vacía si la solución es válida.

**Métodos públicos adicionales:**

| Firma | Qué hace |
|---|---|
| `boolean esValida()` | Devuelve `true` si `violaciones` está vacía. |

---

### `SolverHorario` — final class public

Punto de entrada público del solver: construye el modelo CP-SAT, lo resuelve en modo factibilidad pura y devuelve una `SolucionHorario`.

**Métodos públicos:**

| Firma | Qué hace |
|---|---|
| `SolverHorario()` | Constructor con límite de 120 s y semilla 42. |
| `SolverHorario(double maxSegundos, int semilla)` | Constructor con tiempo límite y semilla configurables. |
| `SolucionHorario resolver(ProblemaHorario problema)` | Resuelve el problema; lanza `HorarioInfactibleException` si no hay solución factible en el tiempo límite. |

---

### `VerificadorSolucion` — final class public

Verifica de forma independiente de OR-Tools que una `SolucionHorario` cumple las restricciones duras del modelo.

**Métodos públicos:**

| Firma | Qué hace |
|---|---|
| `ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion)` | Comprueba que todas las instancias están colocadas y que no hay solapes de profesor, aula, subgrupo ni grupo; devuelve `ResultadoVerificacion` con la lista de violaciones. |

---

### `Expansion` — final class package-private

Autoridad única sobre la expansión de una `Actividad` en sus `ActividadInstancia`; compartida por `ModeloCpSat` y `VerificadorSolucion` para evitar desincronización entre ambas.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `static List<ActividadInstancia> instanciasDe(Actividad actividad)` | Devuelve las instancias de una actividad con índices 1..`repeticionesPorSemana`. |
| `static List<ActividadInstancia> todas(ProblemaHorario problema)` | Devuelve todas las instancias de todas las actividades del problema. |

---

### `ModeloCpSat` — final class package-private

Construye el modelo CP-SAT a partir de un `ProblemaHorario` y extrae la `SolucionHorario` tras resolver; es, junto con `SolverHorario`, la única zona del módulo con acoplamiento directo a OR-Tools.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `ModeloCpSat(ProblemaHorario problema)` | Constructor; no construye el modelo todavía. |
| `CpModel model()` | Devuelve el modelo CP-SAT listo para pasar al solver externo. |
| `ModeloCpSat construir()` | Crea variables de decisión y aplica las restricciones duras; devuelve `this` para encadenar. |
| `SolucionHorario extraerSolucion(CpSolver solver)` | Lee los valores de las variables resueltas y construye la `SolucionHorario`. |

---

### `InstanciaProgramada` — final class package-private

Envuelve una `ActividadInstancia` con sus variables de decisión CP-SAT (`IntVar tramoIndex`, `IntervalVar intervalo`); mantiene el paquete `domain` libre de dependencias de OR-Tools.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo)` | Constructor. |
| `ActividadInstancia instancia()` | Devuelve la instancia de dominio asociada. |
| `IntVar tramoIndex()` | Devuelve la variable de índice de tramo. |
| `IntervalVar intervalo()` | Devuelve la variable de intervalo de duración fija. |

---

## Paquete `io`

### DTOs de entrada — records public

Cada DTO corresponde a una sección del JSON de entrada; sin métodos más allá de los accessors de record.

| Clase | Componentes |
|---|---|
| `TramoDto` | `String codigo`, `Integer diaSemana`, `Integer ordenEnDia` |
| `AulaDto` | `String codigo`, `String nombre` |
| `AsignaturaDto` | `String codigo`, `String nombre` |
| `ProfesorDto` | `String codigo`, `String nombre` |
| `GrupoDto` | `String codigo`, `String tipo`, `String grupoPadre` (nullable) |
| `SubgrupoDto` | `String codigo`, `String grupo` (código de `GrupoAdministrativo`) |
| `PlazaDto` | `String codigo`, `String asignatura`, `List<String> profesores`, `String aulaFija` (nullable), `List<String> aulasCandidatas` (rechazado si no vacío), `List<String> subgrupos` |
| `ActividadDto` | `String codigo`, `String asignatura` (nullable), `Integer repeticionesPorSemana`, `Integer duracionTramos`, `String patronTemporal`, `List<PlazaDto> plazas` |
| `ProblemaHorarioDto` | `List<TramoDto> tramos`, `List<AulaDto> aulas`, `List<AsignaturaDto> asignaturas`, `List<ProfesorDto> profesores`, `List<GrupoDto> grupos`, `List<SubgrupoDto> subgrupos`, `List<ActividadDto> actividades` |

---

### `ProblemaInvalidoException` — class public (extends RuntimeException)

Error de carga: JSON sintácticamente válido pero semánticamente inválido como `ProblemaHorario`.

**Constructores:**

| Firma | Qué hace |
|---|---|
| `ProblemaInvalidoException(String mensaje)` | Construye la excepción con mensaje. |
| `ProblemaInvalidoException(String mensaje, Throwable causa)` | Construye la excepción encadenando la causa original. |

---

### `ProblemaHorarioJsonLoader` — final class public (fachada pública de la capa de carga)

Deserializa el JSON de entrada con Jackson y delega al mapper la construcción del dominio.

**Métodos públicos:**

| Firma | Qué hace |
|---|---|
| `ProblemaHorarioJsonLoader()` | Constructor; configura Jackson con `FAIL_ON_UNKNOWN_PROPERTIES`. |
| `ProblemaHorario cargar(InputStream entrada)` | Deserializa y valida el JSON; lanza `ProblemaInvalidoException` si el contenido es inválido. |

---

### `ProblemaHorarioMapper` — final class public

Construye un `ProblemaHorario` de dominio a partir de los DTOs deserializados; valida integridad referencial, unicidad de códigos, invariante I2 y detecta ciclos en `grupoPadre`.

**Métodos públicos:**

| Firma | Qué hace |
|---|---|
| `static ProblemaHorario aDominio(ProblemaHorarioDto dto)` | Traduce el DTO raíz al modelo de dominio; lanza `ProblemaInvalidoException` ante cualquier error de validación. |

Rechaza explícitamente `aulasCandidatas` no vacío con `ProblemaInvalidoException`. Las invariantes de partición I1, I3 e I6 no se validan aquí (Fase 6/8).

---

## Paquete `cli`

### `CodigoSalida` — enum package-private

Define los códigos de salida del proceso CLI.

**Constantes:** `OK(0)`, `INFACTIBLE(1)`, `ENTRADA_INVALIDA(2)`, `VIOLACIONES_DURAS(3)`

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `int valor()` | Devuelve el entero de salida del proceso asociado a la constante. |

---

### `SesionMaterializada` — record package-private

Estructura intermedia de presentación: une una `ActividadInstancia` colocada con su `Tramo` asignado y la `Plaza` correspondiente.

**Componentes:** `Tramo tramo`, `ActividadInstancia instancia`, `Plaza plaza`

---

### `VistaHorario<K>` — interface package-private

Contrato para vistas tabulares del horario; `K` identifica la "fila lógica" (grupo, profesor, …).

**Métodos:**

| Firma | Qué hace |
|---|---|
| `String titulo()` | Devuelve el título de la vista. |
| `List<K> filas(ProblemaHorario problema)` | Devuelve la lista ordenada de claves para las que se genera una sub-tabla. |
| `String etiquetaFila(K clave)` | Devuelve el texto de cabecera de la sub-tabla para una clave. |
| `Set<K> filasDe(SesionMaterializada sesion)` | Devuelve las claves a las que pertenece la sesión (puede ser >1 en co-docencia o agrupamientos). |
| `String contenidoCelda(SesionMaterializada sesion)` | Devuelve el texto de la celda para esa sesión. |

---

### `VistaPorGrupo` — final class package-private (implements `VistaHorario<GrupoAdministrativo>`)

Vista tabular del horario agrupada por `GrupoAdministrativo`.

Implementa los cinco métodos de `VistaHorario`: `titulo()`, `filas(ProblemaHorario)`, `etiquetaFila(GrupoAdministrativo)`, `filasDe(SesionMaterializada)`, `contenidoCelda(SesionMaterializada)`.

---

### `VistaPorProfesor` — final class package-private (implements `VistaHorario<Profesor>`)

Vista tabular del horario agrupada por `Profesor`; en co-docencia la misma sesión aparece en la fila de cada profesor implicado.

Implementa los cinco métodos de `VistaHorario`: `titulo()`, `filas(ProblemaHorario)`, `etiquetaFila(Profesor)`, `filasDe(SesionMaterializada)`, `contenidoCelda(SesionMaterializada)`.

---

### `Materializador` — final class package-private

Aplana una `SolucionHorario` en una lista plana de `SesionMaterializada`, una por par `(ActividadInstancia, Plaza)`.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `static List<SesionMaterializada> materializar(SolucionHorario solucion)` | Produce la lista aplanada de sesiones. |

---

### `FormatoCelda` — final class package-private

Construye el texto de una celda de horario en formato `Asignatura·Profesor[+Profesor...]·Aula`.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `static String formatear(SesionMaterializada sesion)` | Formatea la celda; muestra `?` como placeholder de aula cuando no hay `aulaFija`. |

---

### `HorarioPrinter` — final class package-private

Imprime horarios en formato tabular (filas = tramos T1..T6, columnas = días) para cualquier `VistaHorario`; el ancho de columna es dinámico sobre el contenido real.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `static <K> void imprimir(PrintStream out, ProblemaHorario problema, List<SesionMaterializada> sesiones, VistaHorario<K> vista)` | Imprime una sub-tabla por cada clave de la vista. |

---

### `VerificacionPrinter` — final class package-private

Imprime a un `PrintStream` el recuento de violaciones y su lista.

**Métodos package-private:**

| Firma | Qué hace |
|---|---|
| `static void imprimir(PrintStream out, ResultadoVerificacion resultado)` | Imprime el contador y, si hay violaciones, las lista. |

---

### `Main` — final class public

Punto de entrada CLI del solver: orquesta carga, resolución, verificación y presentación.

**Métodos:**

| Firma | Qué hace |
|---|---|
| `public static void main(String[] args)` | Punto de entrada del proceso; llama `System.exit` con el código resultante. |
| `static int ejecutar(String[] args, PrintStream out, PrintStream err)` | Lógica del CLI con streams inyectables para testabilidad; no llama `System.exit`. |

---

### `HelloOrTools` — class public

Smoke test de integración que verifica que los binarios nativos de OR-Tools están correctamente enlazados; no forma parte del flujo principal.

**Métodos:**

| Firma | Qué hace |
|---|---|
| `public static void main(String[] args)` | Carga OR-Tools y resuelve un modelo mínimo de prueba. |

---

## Flujo principal

Una ejecución típica arranca en `Main.ejecutar`:

1. **`ProblemaHorarioJsonLoader.cargar(InputStream)`** deserializa el JSON en un `ProblemaHorarioDto` vía Jackson y delega inmediatamente en `ProblemaHorarioMapper.aDominio`.
2. **`ProblemaHorarioMapper.aDominio(ProblemaHorarioDto)`** construye el grafo de objetos de dominio (`Tramo`, `Profesor`, `GrupoAdministrativo`, `Subgrupo`, `Plaza`, `Actividad`) validando integridad referencial y unicidad; devuelve el `ProblemaHorario`.
3. **`SolverHorario.resolver(ProblemaHorario)`** instancia `ModeloCpSat`, llama a `construir()` —que usa `Expansion.todas` para crear las `IntVar`/`IntervalVar` y aplica las cinco restricciones duras— y lanza el solver OR-Tools. Si el estado es `OPTIMAL` o `FEASIBLE`, `ModeloCpSat.extraerSolucion` lee los valores de las variables y construye la `SolucionHorario`.
4. **`VerificadorSolucion.verificar(ProblemaHorario, SolucionHorario)`** recorre la solución de forma independiente de OR-Tools (usando también `Expansion.todas`) y devuelve un `ResultadoVerificacion` con la lista de violaciones.
5. **`Materializador.materializar(SolucionHorario)`** aplana la solución en una lista de `SesionMaterializada`, una por par `(ActividadInstancia, Plaza)`.
6. **`HorarioPrinter.imprimir`** (invocado dos veces, con `VistaPorGrupo` y `VistaPorProfesor`) formatea e imprime el horario en consola; `FormatoCelda.formatear` construye el texto de cada celda.

---

## Fuera de alcance actual

### Fase 3

- **`aulasCandidatas`**: la entrada JSON con `aulasCandidatas` no vacío se rechaza explícitamente con `ProblemaInvalidoException` en `ProblemaHorarioMapper.mapearPlaza`. El no-solape de aula en `ModeloCpSat` solo cubre `aulaFija`. `FormatoCelda` muestra `?` como placeholder defensivo cuando no hay `aulaFija`.
- **Desdobles y agrupamientos transversales con asignación de aula**: `Materializador` ya aplana por plaza (N-correcto desde Fase 2), pero la restricción de selección de aula entre candidatas y la exportación del aula elegida en la solución no están implementadas.

### Fase 4

- **Restricciones PDC/padre compartidas**: `ModeloCpSat.restriccionNoSolapeGrupo` y su espejo en `VerificadorSolucion` son ciegos al `grupoPadre`; la modelización de sesiones compartidas entre un grupo PDC y su grupo padre queda pendiente de Fase 4.

### Fase 5

- **Actividades multi-tramo** (`duracionTramos > 1`): `ModeloCpSat` ya usa `newFixedSizeIntervalVar` con tamaño variable, pero en Fase 2 `duracionTramos` siempre es 1. La restricción de distribución por día contiene una guarda anti-palomar (deuda D12) que omite la restricción si `repeticionesPorSemana > numDias`; marcada para revisión en Fase 5.

### Fase 6/8

- **Invariantes de partición I1, I3 e I6**: el JSON del solver no transporta información de particiones; su validación corresponde a la capa de configuración (Fase 6/8) y no se realiza en `ProblemaHorarioMapper`.
