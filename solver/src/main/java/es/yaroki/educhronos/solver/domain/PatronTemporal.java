package es.yaroki.educhronos.solver.domain;

public enum PatronTemporal {
    DISTRIBUIDA,  // sesiones repartidas en días distintos
    AGRUPADA,     // sesiones consecutivas o concentradas
    NEUTRA        // sin preferencia
}