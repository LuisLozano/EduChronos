package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de una {@code Asignatura} persistida (§4.1, Bloque 8.5-A),
 * SIMÉTRICA a {@link AsignaturaRequest} más el {@code id} sintético que necesitan
 * el {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code AsignaturaService}.
 */
public record AsignaturaDTO(
        Long id,
        String codigo,
        String nombreCompleto) {
}
