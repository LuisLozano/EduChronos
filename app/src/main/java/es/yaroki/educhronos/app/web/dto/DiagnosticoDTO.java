package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Diagnóstico de un horario generado (Fase 8, Bloque 8.3-C): las violaciones DURAS
 * atribuidas por celda ({@code verificar}), las penalizaciones BLANDAS contrafactuales
 * por celda ({@code atribuirBlandas}) y los totales blandos del horario. Lo ensambla
 * {@code DiagnosticoService.diagnosticar} y lo devuelve el endpoint
 * {@code GET /api/horarios/{id}/diagnostico}.
 */
public record DiagnosticoDTO(
        List<ViolacionDTO> violaciones,
        List<PenalizacionDTO> penalizaciones,
        TotalesDTO totales) {
}
