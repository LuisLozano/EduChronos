/**
 * Espejo del contrato REST de catálogo de asignaturas.
 *
 * Fuente: `app/src/main/java/.../web/dto/AsignaturaDTO.java` y `AsignaturaRequest.java`.
 * OJO: existe un `AsignaturaDto` homónimo en el módulo solver
 * (`solver/io/AsignaturaDto.java`) con campos DISTINTOS (`codigo`, `nombre`).
 * Ese es el DTO de importación JSON del solver, otro contrato. Aquí se calca el
 * de `app/web`, que es el que sirven los endpoints `/api/asignaturas`.
 */

/**
 * Espejo de `AsignaturaDTO(Long id, String codigo, String codigoCorto, String nombreCompleto,
 * String nivel)`.
 *
 * Desde S250 (C-asignatura-con-nivel), `codigo` es el COMPUESTO: el corto, un guion y el código
 * del nivel (`Biol-3ESO`), o el corto solo si la asignatura no tiene nivel. Es el que siguen
 * usando como referencia las actividades y las plazas.
 */
export interface Asignatura {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  codigoCorto: string;
  nombreCompleto: string;
  /** Código del nivel, o `null` si la asignatura no es de ningún nivel. */
  nivel: string | null;
}

/**
 * Espejo de `AsignaturaRequest(String codigoCorto, String nombreCompleto, String nivel)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo. Tampoco lleva el código
 * compuesto: lo compone el backend con el corto y el nivel.
 */
export interface AsignaturaRequest {
  codigoCorto: string;
  nombreCompleto: string;
  /** Código del nivel, o `null` sin nivel. */
  nivel: string | null;
}
