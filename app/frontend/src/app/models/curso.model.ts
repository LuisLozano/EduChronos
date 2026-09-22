/**
 * Espejo del contrato REST de los CURSOS (O-curso: C-duplicado-guarda en S159,
 * C-selector-curso en S160).
 *
 * Fuente: `app/src/main/java/.../web/dto/CursoDTO.java`, `CursoListadoDTO.java`,
 * `DuplicarCursoRequest.java`, `AbrirCursoRequest.java` y `CursoCreadoDTO.java`.
 *
 * <p><b>Un curso es un FICHERO, y su nombre puede faltar.</b> Es lo que separa este
 * contrato de los del catálogo, donde la identidad es un `id` numérico que siempre está:
 * aquí la identidad es el nombre del fichero dentro de la carpeta de datos, y el `nombre`
 * del curso —«2026/2027»— es `null` en cualquier base anterior a S159 (la condición 6 de
 * O-curso). Todo lo que en esta pantalla identifique un curso va por `fichero`; el
 * `nombre` es para leerlo.
 *
 * <p><b>Dos recursos y no uno</b>, igual que en el backend: `/api/curso` es el curso
 * ABIERTO —singleton, como la jornada— y `/api/cursos` es la colección de la carpeta.
 */

/**
 * Espejo de `CursoDTO(String nombre, boolean archivado, String propuestaSiguiente)`: el
 * curso que la aplicación tiene abierto ahora mismo.
 */
export interface CursoDTO {
  /** «2026/2027», o **null** si esta base no lo trae escrito (condición 6). */
  nombre: string | null;
  /** Si es de solo lectura. Con `true`, toda escritura responde 403. */
  archivado: boolean;
  /**
   * El curso siguiente, derivado del abierto: «2026/2027» → «2027/2028». **null** cuando
   * `nombre` lo es, porque no hay de qué derivarlo. Es una PROPUESTA para prefijar el
   * campo, no una obligación: el backend acepta cualquier nombre válido y libre.
   */
  propuestaSiguiente: string | null;
}

/**
 * Espejo de `CursoListadoDTO(String fichero, String nombre, boolean archivado,
 * boolean abierto)`: una fila del selector.
 */
export interface CursoListadoDTO {
  /** Nombre del fichero dentro de la carpeta: `curso-2026-2027.db`, `educhronos.db`. */
  fichero: string;
  /** Nombre del curso, o **null** si esa base no lo trae. */
  nombre: string | null;
  archivado: boolean;
  /** Si es el que la aplicación tiene abierto. Exactamente uno de la lista lo tiene. */
  abierto: boolean;
}

/**
 * Espejo de `DuplicarCursoRequest(String nombreNuevo, String nombreActual)`.
 *
 * <p>`nombreActual` sólo hace falta cuando la base abierta no trae nombre: es el que
 * quedará ARCHIVADO, y en ese caso nadie más lo sabe. Con un curso que sí lo trae, el
 * backend rechaza con 400 si se le manda uno distinto, así que se omite.
 */
export interface DuplicarCursoRequest {
  nombreNuevo: string;
  nombreActual?: string;
}

/** Espejo de `AbrirCursoRequest(String fichero)`. Por fichero y nunca por nombre. */
export interface AbrirCursoRequest {
  fichero: string;
}

/**
 * Espejo de `CursoCreadoDTO(String nombre, String fichero)`: lo que devuelve el 201 de
 * duplicar. Nombra el curso CREADO, que desde S160 queda además abierto.
 */
export interface CursoCreadoDTO {
  nombre: string;
  fichero: string;
}

/**
 * Espejo de `RechazoCursoDTO(String causa, String message)`: el cuerpo de todo rechazo
 * previsto de estos endpoints.
 *
 * <p>La `causa` es un SÍMBOLO estable del contrato y el `message` el texto para el
 * usuario. Aquí sólo se declara el tipo; quien lo lee es el `mensaje()` de cada
 * componente, que ya sabe sacar `message` de cualquier cuerpo de error.
 */
export interface RechazoCursoDTO {
  /**
   * `NOMBRE_INVALIDO`, `CURSO_YA_EXISTE`, `CURSO_ARCHIVADO`, `CURSO_NO_EXISTE`,
   * `CURSO_OCUPADO`, `CURSO_CAMBIANDO` o `CURSO_NO_ABRE`.
   */
  causa: string;
  message: string;
}
