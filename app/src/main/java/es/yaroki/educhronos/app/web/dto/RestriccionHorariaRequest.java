package es.yaroki.educhronos.app.web.dto;

/**
 * Un elemento del cuerpo del {@code PUT /api/profesores/{id}/restricciones-horarias}
 * (§4.3, Bloque 8.5-E). El profesor NO viaja en el body: lo lleva la URL.
 *
 * <p>{@code tipo} entra como {@code String} (no como {@code TipoRestriccion}) a
 * propósito: así el servicio puede parsearlo y devolver un {@code 400} que NOMBRA el
 * valor recibido y lista los válidos, en vez del error opaco de deserialización de un
 * enum (mismo patrón que {@code TutoriaRequest.rol} y {@code parseTipoAula}).
 *
 * <p>El tramo se referencia por (dia, ordenEnDia), el par natural de la UI, igual que
 * {@code BloqueoRequest} (D-2): el {@code TramoSemanal.id} sintético no sale al API.
 *
 * <p><b>Sin {@code peso}.</b> El cliente no lo elige: el servicio escribe siempre 1
 * ({@code RestriccionHorariaService.PESO_POR_DEFECTO}). Si algún día el peso pasa a ser
 * configurable, entra por aquí y deja de ser constante allí — hoy no lo es.
 *
 * <p>Solo datos, sin lógica: toda la validación vive en
 * {@code RestriccionHorariaService}.
 */
public record RestriccionHorariaRequest(
        String tipo,
        int dia,
        int ordenEnDia,
        String motivo) {
}
