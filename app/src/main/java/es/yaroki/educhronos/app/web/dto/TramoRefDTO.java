package es.yaroki.educhronos.app.web.dto;

/**
 * Referencia a un tramo por el par natural que ve la UI (§4.7, Bloque 8.2b-iv):
 * {@code dia} 1..5 (lunes..viernes) y {@code orden} = ordenEnDia 1..6, recreos
 * EXCLUIDOS —exactamente el mismo par (dia, tramo) que ya lleva
 * {@link SesionVistaDTO}—. NUNCA se referencia el tramo por {@code TramoSemanal.id}
 * (la UI no lo ve). El servicio resuelve este par a la entidad {@code TramoSemanal}
 * invirtiendo {@code CatalogoMapper.indiceOrdenEnDia} (fuente única, deuda D30).
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A).
 */
public record TramoRefDTO(int dia, int orden) {
}
