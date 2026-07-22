package es.yaroki.educhronos.solver.domain;

/**
 * Rol de un profesor en la tutoría de un grupo (§4.1, invariante I4).
 *
 * <p>Gemelo de dominio del enum homónimo de {@code app.catalog}: el solver no depende
 * de {@code app}, así que la frontera se cruza con un mapper, no reutilizando el tipo.
 * I4 distingue los roles de forma asimétrica: {@code TUTOR_PRINCIPAL} es único por
 * grupo; {@code CO_TUTOR} admite varios. Quien hace cumplir esa unicidad es la capa de
 * escritura, no este enum.
 */
public enum RolTutoria {
    TUTOR_PRINCIPAL,
    CO_TUTOR
}
