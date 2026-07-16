package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/profesores} y del {@code PUT /api/profesores/{id}}:
 * el estado deseado de un profesor (§4.1, Bloque 8.5-A'). SIN {@code id}: en el
 * alta lo asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): la validación (código y
 * nombre no vacíos, unicidad de código) vive en {@code ProfesorService}.
 */
public record ProfesorRequest(
        String codigo,
        String nombreCompleto) {
}
