/**
 * Espejo del contrato REST del catálogo de niveles.
 *
 * Fuente: `app/src/main/java/.../web/dto/NivelDTO.java` y `NivelRequest.java`.
 *
 * <p>CRUD COMPLETO desde S111 (C-niveles). Hasta S110 este fichero declaraba que
 * nivel era de SOLO LECTURA en el frontend, porque su único consumidor era el
 * desplegable de `GrupoForm`. Dejó de ser cierto: sin niveles creables por UI no
 * hay grupos, ni subgrupos, ni población para las plazas, y el e2e del criterio de
 * O-estructura era inejecutable. Nivel tiene ahora su propia sección en
 * Configuración, como las cuatro entidades de O-catálogo.
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

/**
 * Espejo de `NivelRequest(String codigo, int orden)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 *
 * <p>`orden` es `int` PRIMITIVO en el backend, no `Integer`: un cuerpo sin la clave
 * deserializa a 0 en silencio y ninguna validación de servidor lo detecta (medido en
 * S111). El servidor no puede distinguir «ausente» de «cero», así que el formulario
 * exige el campo —no restringe su valor, que sigue pudiendo ser 0—. Es la razón por
 * la que este tipo NO declara `orden` opcional.
 */
export interface NivelRequest {
  codigo: string;
  orden: number;
}
