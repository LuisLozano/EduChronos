package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/asignaturas} y del {@code PUT /api/asignaturas/{id}}:
 * el estado deseado de una asignatura (§4.1, Bloque 8.5-A). SIN {@code id}: en el
 * alta lo asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>Desde S250 llega el código CORTO y el {@code nivel}, que es el código de un nivel o
 * {@code null} sin nivel: el código de la asignatura lo compone el servicio ({@code Mat-1ESO}).
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): la validación (corto y
 * nombre no vacíos, nivel existente, unicidad del código compuesto) vive en {@code AsignaturaService}.
 */
public record AsignaturaRequest(
        String codigoCorto,
        String nombreCompleto,
        String nivel) {
}
