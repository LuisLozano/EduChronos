package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo y salida de {@code GET} y {@code PUT /api/configuracion-guardias} (S212,
 * C-dato-guardias): el mínimo de profesores de guardia por tramo de clase del centro.
 * {@code Integer} y no {@code int} para que un cuerpo sin el campo llegue como null y el servicio
 * lo rechace con 400, en vez de leerlo como 0.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2).
 */
public record ConfiguracionGuardiasDTO(Integer minimoPorTramo) {
}
