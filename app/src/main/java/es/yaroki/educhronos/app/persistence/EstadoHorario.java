package es.yaroki.educhronos.app.persistence;

/**
 * Ciclo de vida editorial de un {@link HorarioGenerado} (§4.7). Lo decide el
 * usuario, NO el solver: es distinto del veredicto {@code estadoSolver}. Por
 * defecto un horario recién generado nace en {@code BORRADOR}.
 */
public enum EstadoHorario {
    BORRADOR,
    DEFINITIVO,
    DESCARTADO
}
