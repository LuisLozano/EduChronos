package es.yaroki.educhronos.app.catalog;

/**
 * Tipo de aula (§4.1 del modelo). No existe equivalente en el dominio del
 * solver: vive solo en la capa de persistencia.
 */
public enum TipoAula {
    ORDINARIA,
    LAB_CIENCIAS,
    INFORMATICA,
    TALLER_TEC,
    TALLER_PLASTICA,
    GIMNASIO,
    PISTA,
    TALLER_FPB,
    COMUN
}
