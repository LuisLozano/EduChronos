package es.yaroki.educhronos.app.catalog;

/**
 * Rol de un profesor en la tutoría de un grupo (§4.1, invariante I4).
 *
 * <p>I4 distingue los dos roles de forma asimétrica: el {@code TUTOR_PRINCIPAL} es
 * ÚNICO por grupo, mientras que los {@code CO_TUTOR} pueden ser varios. Es el enum
 * el que da nombre a esa asimetría; quien la hace cumplir es el servicio de escritura.
 */
public enum RolTutoria {
    TUTOR_PRINCIPAL,
    CO_TUTOR
}
