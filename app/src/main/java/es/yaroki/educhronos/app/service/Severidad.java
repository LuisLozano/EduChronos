package es.yaroki.educhronos.app.service;

/**
 * Gravedad de un {@link AvisoPrevalidacion} (Fase 8, Bloque 8.4-A).
 *
 * <p>La distinción es OPERATIVA, no informativa: {@link #ERROR} aborta la generación
 * ({@code GeneradorHorarioService.generar} lanza {@link PrevalidacionFallidaException}
 * → 422), {@link #AVISO} no aborta nada y solo se reporta.
 *
 * <p><b>Criterio para declarar {@code ERROR}</b>, por DOS vías independientes; basta
 * fallar una para quedarse en {@code AVISO}:
 * <ol>
 *   <li><b>Certeza.</b> La violación debe implicar infactibilidad CIERTA. Una regla que
 *       pudiera sobrestimar la demanda bloquearía problemas resolubles (falso positivo)
 *       y debe quedarse en {@code AVISO}.</li>
 *   <li><b>Dependencia de la colocación.</b> Un {@code ERROR} solo tiene sentido si el
 *       hallazgo impide que EXISTA horario. Si el problema es resoluble y el horario
 *       generado sale válido en todo lo demás —de modo que el hallazgo se corrige
 *       editando el catálogo SIN regenerar—, abortar no protege nada: solo obliga a
 *       falsear el dato para poder generar. Eso es {@code AVISO}.</li>
 * </ol>
 *
 * <p><b>Hoy la produce S8</b>, la cuarta regla (S146): una actividad {@code requiereTutor}
 * sin TUTOR_PRINCIPAL entre sus profesores. Falla la vía (2) —el solver la coloca sin
 * problema y se arregla con un {@code PUT} de tutoría—, así que avisa y no aborta.
 *
 * <p>Las tres primeras —(a) profesor, (c) grupo, (d) repeticiones— siguen siendo
 * condiciones necesarias exactas y son {@code ERROR}. (c) nació como {@code AVISO} y se
 * corrigió a {@code ERROR} en S79 al comprobar que su supuesta sobrestimación no existía
 * (ver el javadoc de {@code PrevalidacionService.sobrecargaGrupo}). El palomar de aulas,
 * si algún día entra, es otro candidato natural a {@code AVISO}, esta vez por la vía (1).
 */
public enum Severidad {

    /** Condición necesaria violada con certeza: el problema no puede tener solución. */
    ERROR,

    /**
     * Hallazgo que NO impide que exista horario: o el cómputo puede estar
     * sobrestimando, o la violación no depende de la colocación y se corrige sin
     * regenerar. No aborta.
     */
    AVISO
}
