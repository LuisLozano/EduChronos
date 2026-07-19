package es.yaroki.educhronos.app.web.dto;

/**
 * Una tutoría tal como sale del {@code GET/PUT /api/grupos/{id}/tutoria} (§4.1,
 * invariante I4, Bloque 8.5-D2a): el profesor por su CÓDIGO de negocio (simétrico con
 * {@link TutoriaRequest}, no el id sintético) y su rol como nombre del enum.
 *
 * <p>El grupo no se repite en cada elemento: lo identifica la URL del sub-recurso.
 */
public record TutoriaDTO(
        String profesor,
        String rol) {
}
