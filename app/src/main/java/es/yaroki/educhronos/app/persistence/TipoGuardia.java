package es.yaroki.educhronos.app.persistence;

/**
 * Tipo de una {@link Guardia} repartida (S220, C-reparto-equilibrado): ORDINARIA en un tramo de clase,
 * RECREO o BIBLIOTECA en un recreo. La de BIBLIOTECA es una guardia de recreo que además está en la
 * biblioteca: cuenta para el mínimo del recreo y para el de biblioteca.
 *
 * <p>Se persiste con {@code @Enumerated(STRING)} en {@code guardia.tipo}, cuyo {@code check} enumera
 * estas tres constantes ({@code schema.sql} y {@code esquema/008.sql}).
 */
public enum TipoGuardia {
    ORDINARIA,
    RECREO,
    BIBLIOTECA
}
