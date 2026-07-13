package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Espejo plano de {@code solver.cpsat.Violacion}: una restricción DURA incumplida,
 * atribuida a N celdas (Fase 8, Bloque 8.3-C). {@code regla} es el nombre del enum
 * {@code ReglaDura}; {@code recursoCodigo} y {@code tramoCodigo} son nullables (null
 * cuando la regla no habla de un recurso/tramo concreto). Las {@link CeldaRefDTO}
 * conservan su {@code plazaCodigo} nullable (asimetría D15).
 */
public record ViolacionDTO(
        String regla,
        String recursoCodigo,
        String tramoCodigo,
        List<CeldaRefDTO> celdas,
        String descripcion) {
}
