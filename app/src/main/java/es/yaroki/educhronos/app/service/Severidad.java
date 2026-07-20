package es.yaroki.educhronos.app.service;

/**
 * Gravedad de un {@link AvisoPrevalidacion} (Fase 8, Bloque 8.4-A).
 *
 * <p>La distinción es OPERATIVA, no informativa: {@link #ERROR} aborta la generación
 * ({@code GeneradorHorarioService.generar} lanza {@link PrevalidacionFallidaException}
 * → 422), {@link #AVISO} no aborta nada y solo se reporta. El criterio para declarar
 * {@code ERROR} es que la violación implique infactibilidad CIERTA: una regla que
 * pudiera sobrestimar la demanda bloquearía problemas resolubles (falso positivo) y
 * debe quedarse en {@code AVISO}.
 *
 * <p><b>Hoy NINGUNA de las tres reglas produce {@code AVISO}</b>: las tres —(a)
 * profesor, (c) grupo, (d) repeticiones— son condiciones necesarias exactas. (c) nació
 * como {@code AVISO} y se corrigió a {@code ERROR} en S79 al comprobar que su supuesta
 * sobrestimación no existía (ver el javadoc de {@code PrevalidacionService.sobrecargaGrupo}).
 * {@code AVISO} se conserva porque la distinción es parte del contrato del endpoint
 * {@code GET /api/prevalidacion} y porque el palomar de aulas, si algún día entra,
 * es candidato natural a nacer así.
 */
public enum Severidad {

    /** Condición necesaria violada con certeza: el problema no puede tener solución. */
    ERROR,

    /** Indicio de sobrecarga que puede ser una sobrestimación del cómputo. No aborta. */
    AVISO
}
