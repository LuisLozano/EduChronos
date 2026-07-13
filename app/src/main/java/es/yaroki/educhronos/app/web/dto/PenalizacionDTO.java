package es.yaroki.educhronos.app.web.dto;

/**
 * Espejo plano de {@code solver.cpsat.Penalizacion}: la aportación CONTRAFACTUAL de
 * una celda a un término blando del objetivo (Fase 8, Bloque 8.3-C). {@code delta}
 * LLEVA SIGNO ({@code >0} mover mejora, {@code <0} la celda tapa un hueco).
 *
 * <p>NO lleva {@code plazaCodigo}: la atribución blanda es POR INSTANCIA (el javadoc
 * de {@code atribuirBlandas} garantiza {@code CeldaRef.plazaCodigo == null} en las
 * blandas). Se aplana la celda a {@code (actividadCodigo, indice)} deliberadamente:
 * un campo siempre null es un campo que miente. {@code tramoCodigo} es nullable
 * (no-null solo en {@code INDISPONIBILIDAD_BLANDA}).
 */
public record PenalizacionDTO(
        String regla,
        String actividadCodigo,
        int indice,
        String tramoCodigo,
        int delta) {
}
