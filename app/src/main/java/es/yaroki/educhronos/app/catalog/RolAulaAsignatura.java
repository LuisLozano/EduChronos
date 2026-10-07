package es.yaroki.educhronos.app.catalog;

/**
 * Cómo usa una asignatura cada una de sus aulas (S206, C-reglas-aulas).
 *
 * <p>{@code EXCLUSIVA}: la asignatura se da SOLO en sus aulas. {@code PREFERIDA}: se da mejor en
 * ellas, si es posible. Una asignatura no mezcla los dos roles; lo valida
 * {@code AsignaturaService} sobre la lista entrante antes de escribir.
 */
public enum RolAulaAsignatura {
    EXCLUSIVA,
    PREFERIDA
}
