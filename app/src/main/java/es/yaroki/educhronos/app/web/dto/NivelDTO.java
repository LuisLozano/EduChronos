package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de un {@code Nivel} persistido (§4.1, Bloque 8.5-A'),
 * SIMÉTRICA a {@link NivelRequest} más el {@code id} sintético que necesitan el
 * {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code NivelService}.
 */
public record NivelDTO(
        Long id,
        String codigo,
        int orden) {
}
