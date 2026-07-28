/**
 * Espejo del contrato REST de catálogo de profesores.
 *
 * Fuente: `app/src/main/java/.../web/dto/ProfesorDTO.java` y `ProfesorRequest.java`.
 * OJO: existe un `ProfesorDto` homónimo en el módulo solver
 * (`solver/io/ProfesorDto.java`) con campos DISTINTOS (`codigo`, `nombre`).
 * Ese es el DTO de importación JSON del solver, otro contrato. Aquí se calca el
 * de `app/web`, que es el que sirven los endpoints `/api/profesores`.
 */

/** Espejo de `ProfesorDTO(Long id, String codigo, String nombreCompleto)`. */
export interface Profesor {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  nombreCompleto: string;
}

/**
 * Espejo de `ProfesorRequest(String codigo, String nombreCompleto)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 */
export interface ProfesorRequest {
  codigo: string;
  nombreCompleto: string;
}
