package es.yaroki.educhronos.app.catalog;

/**
 * Cargo de un {@link Profesor} (S203, C-totales-y-cargo). Por defecto PROFESOR: un profesor sin
 * cargo directivo.
 *
 * <p>Lo conoce sólo la app: el solver no sabe de cargos. Se persiste con
 * {@code @Enumerated(STRING)} en {@code profesor.cargo}, cuyo {@code check} enumera estas cinco
 * constantes ({@code schema.sql} y {@code esquema/003.sql}).
 */
public enum Cargo {
    PROFESOR,
    JEFE_ESTUDIOS,
    DIRECTOR,
    VICEDIRECTOR,
    SECRETARIO
}
