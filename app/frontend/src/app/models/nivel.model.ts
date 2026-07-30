/**
 * Espejo del contrato REST del catálogo de niveles.
 *
 * Fuente: `app/src/main/java/.../web/dto/NivelDTO.java`.
 *
 * <p>SOLO LECTURA en el frontend. `/api/niveles` expone el CRUD completo en backend
 * (Bloque 8.5-A'), pero nivel NO es una de las cuatro entidades de O-catálogo
 * (profesor/aula/asignatura/grupo) y no tiene sección propia en Configuración. Entra
 * aquí únicamente porque `GrupoForm` necesita la lista de códigos para su desplegable.
 * Por eso no hay `NivelRequest` gemelo: nada en el frontend escribe niveles.
 */

/** Espejo de `NivelDTO(Long id, String codigo, int orden)`. */
export interface Nivel {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  /**
   * Entero de ordenación (D-1). El backend YA sirve la lista ordenada por él, así
   * que el consumidor no debe reordenar: el orden que llega es el pedagógico
   * (1ESO, 2ESO, …), no el alfabético.
   */
  orden: number;
}
