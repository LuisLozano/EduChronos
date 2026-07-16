package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de una {@code Plaza} persistida (§4.6, Bloque 8.5-C1), embebida en
 * {@link ActividadDTO}. SIMÉTRICA a {@link PlazaRequest} pero CON {@code id} y CON
 * {@code codigo}: el código derivado ({@code {codigoActividad}-P{n}}) sí sale en la
 * proyección, aunque no entre en el request.
 *
 * <p>Todas las referencias viajan como CÓDIGO (String), no como ids ni objetos anidados.
 * {@code aulaFija} es null si la plaza usa {@code aulasCandidatas}, y viceversa (XOR).
 * Como las relaciones M:N son {@code Set} sin orden intrínseco, {@code ActividadService}
 * ordena los códigos de forma estable para que la proyección sea determinista.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code ActividadService}.
 */
public record PlazaDTO(
        Long id,
        String codigo,
        String asignatura,
        String aulaFija,
        List<String> aulasCandidatas,
        List<String> profesores,
        List<String> subgrupos) {
}
