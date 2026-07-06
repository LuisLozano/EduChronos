package es.yaroki.educhronos.app.service;

/**
 * Vía de resolución que elige el consumidor de {@link GeneradorHorarioService}
 * (parametrización del solve, deuda D29). Desacopla la capa de aplicación del
 * método concreto de {@code SolverHorario}.
 *
 * <p>En Fase 8, Bloque 8.1 SOLO existe {@link #OPTIMIZACION}: es la única vía cuya
 * salida ({@code ResultadoOptimizacion} con estado, objetivo y cota) encaja en
 * {@link GeneradorHorarioService#guardar} sin inventar centinelas. La vía de
 * FACTIBILIDAD pura ({@code SolverHorario.resolver}, que devuelve una
 * {@code SolucionHorario} SIN estado ni objetivo) queda diferida a un bloque
 * futuro: persistirla exigiría que el solver reportara su {@code CpSolverStatus}
 * —hoy lo descarta— y que la persistencia aceptara objetivo/cota nulos, y tocar
 * el solver está fuera del alcance de 8.1.
 */
public enum ViaSolver {
    OPTIMIZACION
}
