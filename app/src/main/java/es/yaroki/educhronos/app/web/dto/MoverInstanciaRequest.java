package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code PUT /api/horarios/{horarioId}/instancias} (S143): recolocar
 * una INSTANCIA completa en otro tramo.
 *
 * <p>La instancia se identifica por el par ({@code actividadCodigo},
 * {@code indice}) —la misma clave de negocio que ya usan {@code CeldaRef},
 * {@code SesionVistaDTO} y el alta de bloqueos—, nunca por {@code sesionId}: una
 * instancia son de 1 a 6 filas de {@code sesion} y se mueven todas o ninguna.
 *
 * <p>El destino va por el par natural ({@code dia} 1..5, {@code orden} = ordenEnDia
 * 1..6, recreos excluidos) que ve la rejilla, nunca por {@code TramoSemanal.id}
 * (mismo criterio que {@code BloqueoRequest}, D-2).
 *
 * <p>NO lleva aula: el movimiento CONSERVA el aula de cada fila. Cambiar de aula es
 * otra operación y no la pide este criterio.
 */
public record MoverInstanciaRequest(String actividadCodigo, int indice, int dia, int orden) {
}
