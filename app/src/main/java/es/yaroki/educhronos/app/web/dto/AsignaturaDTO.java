package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de una {@code Asignatura} persistida (§4.1, Bloque 8.5-A),
 * SIMÉTRICA a {@link AsignaturaRequest} más el {@code id} sintético que necesitan
 * el {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>Desde S250 lleva además el código compuesto ({@code codigo}), que no viaja en la petición;
 * el {@code nivel} es el código del nivel o {@code null} si la asignatura no tiene.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code AsignaturaService}.
 */
public record AsignaturaDTO(
        Long id,
        String codigo,
        String codigoCorto,
        String nombreCompleto,
        String nivel) {
}
