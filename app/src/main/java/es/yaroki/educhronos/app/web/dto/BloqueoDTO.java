package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de un bloqueo manual persistido (§4.7, Bloque 8.2b-iv),
 * SIMÉTRICA a {@link BloqueoRequest} más el {@code id} de la {@code SesionBloqueada}
 * que necesita el {@code DELETE}. La UI cruza ({@code actividadCodigo},
 * {@code indice}) contra {@link SesionVistaDTO}, que ya lleva esa clave compuesta
 * (D-6): por eso no se toca {@code SesionVistaDTO}.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code BloqueoService}.
 */
public record BloqueoDTO(
        Long id,
        String actividadCodigo,
        int indice,
        TramoRefDTO tramo,
        List<AulaPinDTO> aulas) {
}
