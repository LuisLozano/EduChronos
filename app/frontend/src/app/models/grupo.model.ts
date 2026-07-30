/**
 * Espejo del contrato REST del catálogo de grupos administrativos.
 *
 * Fuente: `app/src/main/java/.../web/dto/GrupoDTO.java` y `GrupoRequest.java`.
 *
 * <p>`nivel` viaja como el CÓDIGO de negocio del nivel (string), NO como su id
 * sintético: `GrupoService` (backend) lo resuelve con `NivelRepository.findByCodigo`
 * y devuelve un 400 que NOMBRA el código si no existe. El consumidor razona en
 * códigos, no en ids.
 *
 * <p>`tipo` es siempre `'ORDINARIO'` en este flujo: el backend aplica una LISTA
 * BLANCA (D-nueva-2) y rechaza con 400 cualquier otro valor. PDC y virtuales de
 * optativa se crean por otros flujos (8.5-D, 8.5-C+). `grupoPadre` no está en el
 * contrato: en este bloque es siempre null.
 */

/** Espejo de `GrupoDTO(Long id, String codigo, String nivel, String tipo)`. */
export interface Grupo {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  /** CÓDIGO del nivel (p. ej. '1ESO'), no su id. */
  nivel: string;
  /** Siempre `'ORDINARIO'` en este flujo (lista blanca del backend). */
  tipo: string;
}

/**
 * Espejo de `GrupoRequest(String codigo, String nivel, String tipo)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 *
 * <p>`tipo` es obligatorio en el contrato aunque el formulario no lo exponga:
 * `GrupoForm` lo inyecta con el valor fijo `'ORDINARIO'` al construir el cuerpo.
 */
export interface GrupoRequest {
  codigo: string;
  nivel: string;
  tipo: string;
}
