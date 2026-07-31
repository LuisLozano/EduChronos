/**
 * Espejo del contrato REST de la JORNADA del centro (C-jornada).
 *
 * Fuente: `app/src/main/java/.../web/dto/JornadaDTO.java`, `TramoJornadaDTO.java`,
 * `JornadaRequest.java` y `TramoJornadaRequest.java`.
 *
 * PRIMER contrato del proyecto donde el DTO y el Request NO son casi el mismo objeto.
 * En `aula.model.ts` difieren solo en el `id`; aquí la frontera es asimétrica de verdad
 * y por eso son cuatro interfaces DISTINTAS, no dos con campos opcionales:
 *
 * - **Sale la SEMANA**: `GET /api/jornada` devuelve siempre los 35 tramos (5 días × 7),
 *   cada uno con `dia`, `orden` global 1..35 y `ordenEnDia` derivado.
 * - **Entra UN DÍA TIPO**: `PUT /api/jornada` recibe solo los tramos de un día, con
 *   `horaInicio`, `horaFin` y `esLectivo`. El día, el orden global y la renumeración los
 *   pone el backend al expandir a LUNES..VIERNES (la jornada es idéntica los cinco días).
 *
 * Modelarlo con una sola interfaz de campos opcionales invitaría a reenviar tal cual lo
 * recibido, que es exactamente lo que el contrato NO acepta. El recorte se hace una vez,
 * en `Jornada.aRequest()`, y el tipo lo obliga.
 */

/**
 * Espejo de `TramoJornadaDTO(String dia, String horaInicio, String horaFin,
 * boolean esLectivo, int orden, Integer ordenEnDia)`.
 */
export interface TramoJornadaDTO {
  /** `name()` del enum `Dia`: LUNES..VIERNES. */
  dia: string;
  /** `"HH:mm"`. El backend formatea con `DateTimeFormatter.ofPattern("HH:mm")`. */
  horaInicio: string;
  horaFin: string;
  esLectivo: boolean;
  /** Posición global 1..35, continua entre días (el martes empieza en 8, no en 1). */
  orden: number;
  /** 1..6 en los lectivos; **null** en los recreos, que no entran en la numeración. */
  ordenEnDia: number | null;
}

/**
 * Espejo de `JornadaDTO(boolean persistida, List<TramoJornadaDTO> tramos)`.
 *
 * `persistida=false` significa que la tabla está vacía y el servidor ha SINTETIZADO la
 * malla de referencia: es una propuesta que nadie ha guardado. El recurso nunca responde
 * 404 —una jornada sin configurar no es "no encontrada"—, así que este boolean es el
 * único modo de distinguir "el centro eligió esta malla" de "nadie configuró nada".
 */
export interface JornadaDTO {
  persistida: boolean;
  tramos: TramoJornadaDTO[];
}

/**
 * Espejo de `TramoJornadaRequest(String horaInicio, String horaFin, boolean esLectivo)`.
 *
 * SIN `dia`, SIN `orden`, SIN `ordenEnDia`: los tres los pone el backend. La ausencia es
 * el contrato, no una omisión por brevedad.
 */
export interface TramoJornadaRequest {
  horaInicio: string;
  horaFin: string;
  esLectivo: boolean;
}

/**
 * Espejo de `JornadaRequest(List<TramoJornadaRequest> tramos)`: los tramos de UN DÍA
 * TIPO (7 en la malla de referencia), no los 35 de la semana.
 */
export interface JornadaRequest {
  tramos: TramoJornadaRequest[];
}
