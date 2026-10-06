/**
 * Espejo del contrato REST de catálogo de profesores.
 *
 * Fuente: `app/src/main/java/.../web/dto/ProfesorDTO.java` y `ProfesorRequest.java`.
 * OJO: existe un `ProfesorDto` homónimo en el módulo solver
 * (`solver/io/ProfesorDto.java`) con campos DISTINTOS (`codigo`, `nombre`).
 * Ese es el DTO de importación JSON del solver, otro contrato. Aquí se calca el
 * de `app/web`, que es el que sirven los endpoints `/api/profesores`.
 */

/** Cargo de un profesor (S203). Espejo del enum `Cargo` del backend; por defecto PROFESOR. */
export type Cargo = 'PROFESOR' | 'JEFE_ESTUDIOS' | 'DIRECTOR' | 'VICEDIRECTOR' | 'SECRETARIO';

/** Texto visible de cada cargo, para el select del formulario y la columna de la lista. */
export const ETIQUETA_CARGO: Record<Cargo, string> = {
  PROFESOR: 'Profesor/a',
  JEFE_ESTUDIOS: 'Jefe/a de Estudios',
  DIRECTOR: 'Director/a',
  VICEDIRECTOR: 'Vicedirector/a',
  SECRETARIO: 'Secretario/a',
};

/**
 * Espejo de `ProfesorDTO(Long id, String codigo, String nombreCompleto, Integer totalDeclarado,
 * String cargo)`.
 *
 * <p>`totalDeclarado` y `cargo` (S203) son OPCIONALES en el tipo aunque el servidor los mande
 * siempre: los specs anteriores construyen profesores con los tres campos de antes, y como
 * obligatorios dejarían de compilar. `totalDeclarado` null = sin total declarado.
 */
export interface Profesor {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  nombreCompleto: string;
  totalDeclarado?: number | null;
  cargo?: Cargo;
}

/**
 * Espejo de `ProfesorRequest(String codigo, String nombreCompleto, Integer totalDeclarado,
 * String cargo)`. Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 *
 * <p>El PUT es reemplazo total: ausentes, el backend deja el profesor sin total y como
 * PROFESOR. Por eso `ProfesorForm` manda siempre los dos (vacío como null). Opcionales en el
 * tipo por la misma razón que en {@link Profesor}.
 */
export interface ProfesorRequest {
  codigo: string;
  nombreCompleto: string;
  totalDeclarado?: number | null;
  cargo?: Cargo;
}
