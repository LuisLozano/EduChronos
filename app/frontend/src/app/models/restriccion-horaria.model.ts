/**
 * Espejo del contrato REST del sub-recurso de RESTRICCIONES HORARIAS de un profesor,
 * `/api/profesores/{idProfesor}/restricciones-horarias` (§4.3, Bloque 8.5-E).
 *
 * Fuente: `app/src/main/java/.../web/dto/RestriccionHorariaDTO.java` y
 * `RestriccionHorariaRequest.java`.
 *
 * <p>El tramo viaja por el par natural `(dia, ordenEnDia)` y NUNCA por el id de
 * `TramoSemanal` (D-2). OJO al `dia`: aquí es un ENTERO 1..5 (1 = lunes), traducido en
 * el backend con `Dia.ordinal() + 1` (`RestriccionHorariaService.java:190, :229`), y NO
 * la cadena `'LUNES'` que sirve `GET /api/jornada`. Los dos contratos hablan del mismo
 * día con dos formas distintas; la traducción vive en un único sitio del frontend,
 * `rejilla-disponibilidad.ts`.
 *
 * <p>El PROFESOR no viaja en el cuerpo: lo lleva la URL del sub-recurso. Y `peso`
 * tampoco: el servicio lo fija a 1 y el API no lo expone (`RestriccionHorariaDTO.java:12-14`).
 */

/**
 * Los dos valores de `TipoRestriccion` del backend (`catalog/TipoRestriccion.java`).
 * `DURA` = no puede; `BLANDA` = prefiere no.
 *
 * <p>UNIÓN DE LITERALES y cerrada, por el mismo motivo que `RolTutoria`: este campo se
 * ESCRIBE desde la interfaz, así que un tipo mal tecleado es un error de compilación y
 * no un 400 en runtime.
 */
export type TipoRestriccion = 'DURA' | 'BLANDA';

/**
 * Espejo de `RestriccionHorariaDTO(String tipo, int dia, int ordenEnDia, String motivo)`:
 * una restricción tal como sale del `GET` y del `PUT`.
 */
export interface RestriccionHoraria {
  tipo: TipoRestriccion;
  /** 1..5, 1 = lunes. Ver la cabecera del fichero: no es el `dia` de la jornada. */
  dia: number;
  /** 1..6 entre los tramos LECTIVOS del día; el recreo no tiene número. */
  ordenEnDia: number;
  /** Texto libre de auditoría; `null` cuando no hay. El `PUT` lo BORRA si no se reenvía. */
  motivo: string | null;
}

/**
 * Espejo de `RestriccionHorariaRequest(String tipo, int dia, int ordenEnDia, String
 * motivo)`: UN elemento del cuerpo del `PUT`.
 *
 * <p>ALIAS y no interfaz aparte, como `TutoriaRequest`: los dos records del backend son
 * idénticos campo a campo. El alias nombra la INTENCIÓN en las firmas.
 */
export type RestriccionHorariaRequest = RestriccionHoraria;
