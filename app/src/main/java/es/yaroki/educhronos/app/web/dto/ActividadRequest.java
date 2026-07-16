package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code POST /api/actividades} y del {@code PUT /api/actividades/{id}}: el
 * estado deseado de una actividad como AGREGADO, con sus {@link PlazaRequest} embebidas
 * (§4.6, Bloque 8.5-C1). SIN {@code id}: en el alta lo asigna JPA; en la edición lo lleva
 * la URL, no el body.
 *
 * <p>{@code asignatura} es una referencia por CÓDIGO OPCIONAL (§4.6): null cuando las
 * plazas tienen distintas asignaturas (bloque CyR/OyD/RefMt); si viene, debe existir.
 *
 * <p>{@code patronTemporal} entra como {@code String} (no como {@code PatronTemporal}) a
 * propósito (patrón D-3 de 8.5-A', igual que {@code AulaRequest.tipo}): así el servicio lo
 * parsea con {@code PatronTemporal.valueOf(...)} y devuelve un 400 accionable que nombra el
 * valor y lista los válidos, en vez del error opaco de deserialización de un enum.
 *
 * <p>{@code duracionTramos} y {@code repeticionesPorSemana} son obligatorios (≥1);
 * {@code plazas} debe traer ≥1 elemento. Solo datos: la validación vive en
 * {@code ActividadService}.
 */
public record ActividadRequest(
        String codigo,
        String asignatura,
        int duracionTramos,
        int repeticionesPorSemana,
        String patronTemporal,
        boolean requiereTutor,
        List<PlazaRequest> plazas) {
}
