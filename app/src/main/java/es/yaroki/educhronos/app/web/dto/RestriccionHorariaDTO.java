package es.yaroki.educhronos.app.web.dto;

/**
 * Una restricción horaria tal como sale del
 * {@code GET/PUT /api/profesores/{id}/restricciones-horarias} (§4.3, Bloque 8.5-E):
 * el tipo como nombre del enum y el tramo por el par natural (dia 1..5,
 * ordenEnDia 1..6) que ve la UI, NUNCA por {@code TramoSemanal.id} (D-2 de S-bloqueos).
 *
 * <p>El profesor no se repite en cada elemento: lo identifica la URL del sub-recurso
 * (mismo criterio que {@link TutoriaDTO} con el grupo).
 *
 * <p><b>{@code peso} NO viaja.</b> Es un detalle del solver que el servicio fija a 1;
 * exponerlo aquí invitaría a creer que se puede elegir. Simétrico con
 * {@link RestriccionHorariaRequest}, que tampoco lo lleva.
 */
public record RestriccionHorariaDTO(
        String tipo,
        int dia,
        int ordenEnDia,
        String motivo) {
}
