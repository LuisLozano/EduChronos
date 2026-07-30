/**
 * Espejo del contrato REST de catálogo de asignaturas.
 *
 * Fuente: `app/src/main/java/.../web/dto/AsignaturaDTO.java` y `AsignaturaRequest.java`.
 * OJO: existe un `AsignaturaDto` homónimo en el módulo solver
 * (`solver/io/AsignaturaDto.java`) con campos DISTINTOS (`codigo`, `nombre`).
 * Ese es el DTO de importación JSON del solver, otro contrato. Aquí se calca el
 * de `app/web`, que es el que sirven los endpoints `/api/asignaturas`.
 */

/** Espejo de `AsignaturaDTO(Long id, String codigo, String nombreCompleto)`. */
export interface Asignatura {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  nombreCompleto: string;
}

/**
 * Espejo de `AsignaturaRequest(String codigo, String nombreCompleto)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 */
export interface AsignaturaRequest {
  codigo: string;
  nombreCompleto: string;
}
