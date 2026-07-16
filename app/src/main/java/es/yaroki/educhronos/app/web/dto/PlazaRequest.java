package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Una plaza dentro del cuerpo de {@link ActividadRequest} (§4.6, Bloque 8.5-C1).
 * Actividad es un AGREGADO: la plaza viaja EMBEBIDA, no tiene endpoint propio.
 *
 * <p><b>SIN {@code codigo}</b>: el código de plaza no lo teclea el usuario; lo DERIVA
 * {@code ActividadService} como {@code {codigoActividad}-P{n}} (n = índice 1-based en
 * orden de llegada). Es inestable entre ediciones (identificador técnico interno).
 *
 * <p>Todas las referencias por CÓDIGO (String), patrón de 8.5-B:
 * <ul>
 *   <li>{@code asignatura}: OBLIGATORIA aunque la de la actividad sea null (§4.6);
 *   <li>{@code aulaFija} / {@code aulasCandidatas}: XOR, exactamente una rama (aulaFija
 *       presente Y candidatas vacías, o aulaFija ausente Y ≥1 candidata);
 *   <li>{@code profesores}: ≥1 (invariante I7);
 *   <li>{@code subgrupos}: población de la plaza; ningún código puede repetirse en dos
 *       plazas de la misma actividad (invariante I2).
 * </ul>
 * Cualquier código no resoluble → 400 que lo nombra. Solo datos: la validación vive en
 * {@code ActividadService}.
 */
public record PlazaRequest(
        String asignatura,
        String aulaFija,
        List<String> aulasCandidatas,
        List<String> profesores,
        List<String> subgrupos) {
}
