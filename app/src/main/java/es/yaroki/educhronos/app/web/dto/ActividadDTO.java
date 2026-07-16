package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de una {@code Actividad} persistida como AGREGADO (§4.6, Bloque
 * 8.5-C1), SIMÉTRICA a {@link ActividadRequest} más el {@code id} sintético que necesitan
 * el {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}, con sus
 * {@link PlazaDTO} embebidas.
 *
 * <p>{@code asignatura} es el código (String) de la asignatura de la actividad, o null si
 * las plazas la aportan cada una. {@code patronTemporal} viaja como String (el
 * {@code name()} del enum), por coherencia con el patrón de borde de 7A/8.5-A' (ver
 * {@link AulaDTO}).
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code ActividadService}.
 */
public record ActividadDTO(
        Long id,
        String codigo,
        String asignatura,
        int duracionTramos,
        int repeticionesPorSemana,
        String patronTemporal,
        boolean requiereTutor,
        List<PlazaDTO> plazas) {
}
