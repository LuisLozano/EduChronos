package es.yaroki.educhronos.app.service;

/**
 * Gravedad de un {@link AvisoPrevalidacion} (Fase 8, Bloque 8.4-A).
 *
 * <p>La distinción es OPERATIVA, no informativa: {@link #ERROR} aborta la generación
 * ({@code GeneradorHorarioService.generar} lanza {@link PrevalidacionFallidaException}
 * → 422), {@link #AVISO} no aborta nada y solo se reporta. Una condición se declara
 * {@code AVISO} cuando su cómputo puede SOBRESTIMAR la demanda por cómo esté
 * configurado el catálogo; convertirla en {@code ERROR} bloquearía problemas
 * resolubles (falso positivo).
 */
public enum Severidad {

    /** Condición necesaria violada con certeza: el problema no puede tener solución. */
    ERROR,

    /** Indicio de sobrecarga que puede ser una sobrestimación del cómputo. No aborta. */
    AVISO
}
