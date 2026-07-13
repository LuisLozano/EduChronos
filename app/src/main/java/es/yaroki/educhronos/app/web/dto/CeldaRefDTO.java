package es.yaroki.educhronos.app.web.dto;

/**
 * Espejo plano de {@code solver.cpsat.CeldaRef}: una celda implicada en una
 * {@link ViolacionDTO} (Fase 8, Bloque 8.3-C). {@code plazaCodigo} es NULLABLE —
 * no-null solo en {@code SOLAPE_AULA} (asimetría D15, el aula se cuenta por plaza);
 * null en el resto (atribución por instancia).
 */
public record CeldaRefDTO(String actividadCodigo, int indice, String plazaCodigo) {
}
