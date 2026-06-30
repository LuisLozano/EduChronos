package es.yaroki.educhronos.app.catalog;

/**
 * Día lectivo de la semana (§4.1, TramoSemanal). La capa JPA lo modela como
 * enum; el dominio del solver usa un entero 1..5.
 */
public enum Dia {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES
}
