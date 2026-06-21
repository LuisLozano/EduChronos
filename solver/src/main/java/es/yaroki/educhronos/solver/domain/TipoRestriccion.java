package es.yaroki.educhronos.solver.domain;

/**
 * Naturaleza de una {@link RestriccionHoraria}.
 *
 * DURA:   prohíbe al profesor ocupar el tramo. El solver la trata como
 *         restricción dura (veta la asignación en ese tramo).
 * BLANDA: penaliza la ocupación del tramo con un peso. Entra en la función
 *         objetivo. NO consumida por el solver en el Bloque 6b (solo se carga
 *         y valida; el consumo como término del objetivo se difiere a 6c).
 */
public enum TipoRestriccion {
    DURA,
    BLANDA
}