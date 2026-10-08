package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de un {@code Profesor} persistido (§4.1, Bloque 8.5-A'),
 * SIMÉTRICA a {@link ProfesorRequest} más el {@code id} sintético que necesitan el
 * {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>{@code totalDeclarado} es null si no se ha declarado; {@code cargo} viaja como el
 * {@code name()} del {@code Cargo} (S203). {@code guardiasOrdinarias} (S212) siempre viene, 0 si
 * el profesor no tiene.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code ProfesorService}.
 */
public record ProfesorDTO(
        Long id,
        String codigo,
        String nombreCompleto,
        Integer totalDeclarado,
        String cargo,
        Integer guardiasOrdinarias) {
}
