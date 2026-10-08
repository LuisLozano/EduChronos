package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Diagnóstico de un horario generado (Fase 8, Bloque 8.3-C): las violaciones DURAS
 * atribuidas por celda ({@code verificar}), las penalizaciones BLANDAS contrafactuales
 * por celda ({@code atribuirBlandas}) y los totales blandos del horario. Lo ensambla
 * {@code DiagnosticoService.diagnosticar} y lo devuelve el endpoint
 * {@code GET /api/horarios/{id}/diagnostico}.
 *
 * <p>Desde S213, {@code violacionesGuardia}: las de las guardias ordinarias repartidas, en lista
 * propia; {@code violaciones} no cambia. El frontend aún no la pinta (va con la condición 6 de
 * O-guardias).
 */
public record DiagnosticoDTO(
        List<ViolacionDTO> violaciones,
        List<PenalizacionDTO> penalizaciones,
        TotalesDTO totales,
        List<ViolacionGuardiaDTO> violacionesGuardia) {
}
