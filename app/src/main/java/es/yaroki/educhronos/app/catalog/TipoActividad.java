package es.yaroki.educhronos.app.catalog;

/**
 * Qué es una {@link Actividad} (S201, C-actividad-sin-alumnos). Sólo una CLASE tiene alumnos:
 * sus plazas llevan subgrupos y aula. Una REUNIÓN o una FUNCIÓN ocupa a sus profesores, no
 * tiene subgrupos y puede no tener aula.
 *
 * <p>Lo conoce sólo la app: el solver no sabe de tipos, ve plazas con o sin aula. Se persiste
 * con {@code @Enumerated(STRING)} en {@code actividad.tipo}, cuyo {@code check} enumera estas
 * tres constantes ({@code schema.sql} y {@code esquema/002.sql}).
 */
public enum TipoActividad {
    CLASE,
    REUNION,
    FUNCION
}
