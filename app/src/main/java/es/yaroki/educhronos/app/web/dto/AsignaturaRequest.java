package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/asignaturas} y del {@code PUT /api/asignaturas/{id}}:
 * el estado deseado de una asignatura (§4.1, Bloque 8.5-A). SIN {@code id}: en el
 * alta lo asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): la validación (código y
 * nombre no vacíos, unicidad de código) vive en {@code AsignaturaService}.
 */
public record AsignaturaRequest(
        String codigo,
        String nombreCompleto) {
}
