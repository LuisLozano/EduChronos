package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/niveles} y del {@code PUT /api/niveles/{id}}: el
 * estado deseado de un nivel (§4.1, Bloque 8.5-A'). SIN {@code id}: en el alta lo
 * asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): la validación (código
 * no vacío, unicidad de código) vive en {@code NivelService}. El {@code orden} es
 * un {@code int} sin regla de validación: solo fija el criterio de ordenación de
 * la lista (D-1).
 */
public record NivelRequest(
        String codigo,
        int orden) {
}
