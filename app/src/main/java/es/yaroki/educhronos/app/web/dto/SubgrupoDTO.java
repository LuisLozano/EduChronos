package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de un {@code Subgrupo} persistido (§4.2, Bloque 8.5-B), SIMÉTRICA
 * a {@link SubgrupoRequest} más el {@code id} sintético que necesitan el
 * {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>{@code grupos} viaja como la lista de CÓDIGOS de los grupos administrativos que
 * pueblan el subgrupo (String), no como ids ni objetos anidados, coherente con
 * {@link SubgrupoRequest#grupos}. Como la relación es un {@code Set} sin orden
 * intrínseco, {@code SubgrupoService} ordena los códigos de forma estable (por código)
 * para que la proyección sea determinista.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code SubgrupoService}.
 */
public record SubgrupoDTO(
        Long id,
        String codigo,
        List<String> grupos) {
}
