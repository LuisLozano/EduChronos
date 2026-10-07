/**
 * Espejo del contrato REST del catálogo de subgrupos de alumnos.
 *
 * Fuente: `app/src/main/java/.../web/dto/SubgrupoDTO.java` y `SubgrupoRequest.java`.
 *
 * <p>`grupos` viaja como la lista de CÓDIGOS de los grupos administrativos que
 * pueblan el subgrupo (string[]), NO como ids ni objetos anidados: `SubgrupoService`
 * (backend) resuelve cada código con `GrupoAdministrativoRepository.findByCodigo` y
 * devuelve un 400 que NOMBRA el primer código no resoluble. El consumidor razona en
 * códigos, no en ids.
 *
 * <p>La lista debe traer ≥1 grupo (invariante I6): vacía o ausente → 400. El backend
 * devuelve los códigos ordenados de forma estable (alfabética), así que el consumidor
 * no reordena.
 */

/** Espejo de `SubgrupoDTO(Long id, String codigo, List<String> grupos, Integer alumnos, boolean delPdc)`. */
export interface Subgrupo {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  /** CÓDIGOS de los grupos que pueblan el subgrupo (p. ej. ['1ºA']), no sus ids. */
  grupos: string[];
  /** S206. Alumnos del subgrupo, o null si no se han indicado. Opcional en el tipo, como el total de `Grupo`. */
  alumnos?: number | null;
  /**
   * S206. El subgrupo es el mono-Di de un PDC: su código y sus grupos no se cambian por este
   * CRUD, solo sus alumnos. Lo calcula el backend con el predicado de su guarda.
   */
  delPdc?: boolean;
}

/**
 * Espejo de `SubgrupoRequest(String codigo, List<String> grupos, Integer alumnos)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 *
 * <p>En la edición el conjunto de `grupos` se REEMPLAZA por completo (no unión ni
 * deltas), coherente con el contrato del backend.
 */
export interface SubgrupoRequest {
  codigo: string;
  grupos: string[];
  /** S206. `SubgrupoForm` lo manda siempre: ausente, el PUT dejaría los alumnos en null. */
  alumnos?: number | null;
}
