package es.yaroki.educhronos.app.catalog;

/**
 * Tipo de grupo administrativo (capa de persistencia, §4.1 del modelo).
 *
 * <p>Enum PROPIO de la capa JPA, distinto del {@code TipoGrupo} reducido del
 * solver (que solo conoce ORDINARIO y DIVERSIFICACION_PDC). El mapper de
 * Bloque 3 reconcilia ambos mundos.
 */
public enum TipoGrupo {
    ORDINARIO,
    DIVERSIFICACION_PDC,
    VIRTUAL_OPTATIVA
}
