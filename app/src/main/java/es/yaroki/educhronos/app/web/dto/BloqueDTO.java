package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Un BLOQUE —actividad con más de una plaza— en el plan de replicación (Bloque S139).
 * Aparece en {@code PlanReplicacionDTO.replicados} o en {@code .reparto} según cómo se
 * comporten sus vías, nunca en las dos.
 *
 * <p>{@code vias} son TODAS las plazas del bloque, no solo las que tocan al hermano: el
 * usuario elige entre ellas, y para elegir necesita ver también las que hoy no le tocan.
 */
public record BloqueDTO(
        String actividad,
        List<ViaDTO> vias) {
}
