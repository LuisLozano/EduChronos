package es.yaroki.educhronos.app.web.dto;

/**
 * Un elemento del cuerpo del {@code PUT /api/grupos/{id}/tutoria}: qué profesor tutoriza
 * el grupo y con qué rol (§4.1, invariante I4, Bloque 8.5-D2a). El grupo NO viaja en el
 * body: lo lleva la URL.
 *
 * <p>{@code profesor} viaja como el CÓDIGO de negocio del profesor (String), NO como su
 * id sintético, igual que {@code GrupoRequest.nivel} (D-nueva: la FK viaja como clave
 * natural).
 *
 * <p>{@code rol} entra como {@code String} (no como {@code RolTutoria}) a propósito: así
 * {@code TutoriaService} puede parsearlo y devolver un {@code 400} que NOMBRA el valor
 * recibido y lista los válidos, en vez del error opaco de deserialización de un enum
 * (mismo patrón que {@code GrupoRequest.tipo} y {@code parseTipoAula}).
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code TutoriaService}.
 */
public record TutoriaRequest(
        String profesor,
        String rol) {
}
