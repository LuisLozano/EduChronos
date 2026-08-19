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
  /**
   * `name()` del `TipoGrupo` del backend. Hoy llegan de verdad `'ORDINARIO'` y
   * `'DIVERSIFICACION_PDC'`: `GET /api/grupos` hace `findAll()` SIN filtrar por tipo,
   * así que los PDC creados por `/api/grupos/{idPadre}/pdc` salen en la misma lista.
   * `'VIRTUAL_OPTATIVA'` existe en el enum pero hoy no lo crea ningún flujo.
   *
   * <p>La LISTA BLANCA (D-nueva-2) que solo admite `'ORDINARIO'` gobierna la
   * ESCRITURA por el CRUD plano —`POST /api/grupos` y `PUT /api/grupos/{id}`—, no la
   * lectura. Quien consuma este campo no puede dar por hecho un único valor: la lista
   * de grupos lo usa precisamente para discriminar qué acciones ofrece cada fila.
   */
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

/**
 * Espejo de `PdcRequest(String codigo)`, el cuerpo del
 * `POST /api/grupos/{idPadre}/pdc` (Bloque 8.5-D1).
 *
 * <p>UN SOLO CAMPO, y los tres que faltan no son un olvido: el PADRE viaja en la
 * URL, el `nivel` lo HEREDA el backend del padre (I5) y el `tipo` es siempre
 * `DIVERSIFICACION_PDC`, fijado por el flujo y no por el cliente. Mandar cualquiera
 * de los tres en el cuerpo no haría nada: el backend no los lee.
 *
 * <p>Vive aquí y no en un `pdc.model.ts` aparte porque la RESPUESTA del sub-recurso
 * es un `GrupoDTO`, o sea {@link Grupo}: separar el request de su response obligaría
 * a duplicar ese tipo o a reexportarlo desde un segundo sitio.
 */
export interface PdcRequest {
  codigo: string;
}
