package es.yaroki.educhronos.solver.cpsat;

/**
 * Categoría tipada de una restricción DURA violada, para atribución estructurada
 * (Fase 8, Bloque 8.3-A). Sustituye a la clasificación por texto libre: permite
 * agrupar y filtrar violaciones sin parsear descripciones.
 *
 * <p>Cada valor es espejo de una comprobación de {@link VerificadorSolucion}:
 * <ul>
 *   <li>{@code INSTANCIA_SIN_COLOCAR} — una instancia esperada no tiene tramo.</li>
 *   <li>{@code BLOQUE_IMPOSIBLE} — un bloque {@code duracion>1} desborda el día o
 *       cruza el recreo (D13).</li>
 *   <li>{@code SOLAPE_PROFESOR}, {@code SOLAPE_AULA}, {@code SOLAPE_SUBGRUPO},
 *       {@code SOLAPE_GRUPO} — dos o más celdas comparten un recurso en un tramo
 *       (S9). El aula se cuenta por plaza (D15); el resto, por instancia.</li>
 *   <li>{@code DISTRIBUCION_MISMO_DIA} — una actividad DISTRIBUIDA repite día.</li>
 *   <li>{@code TUTORIA_SIN_TUTOR} — una actividad {@code requiereTutor} no la
 *       imparte ningún TUTOR_PRINCIPAL de un grupo que cubre (§4.6, invariante S8).
 *       Es propiedad del CATÁLOGO, no de la solución: no depende del tramo.</li>
 * </ul>
 */
public enum ReglaDura {
    INSTANCIA_SIN_COLOCAR,
    BLOQUE_IMPOSIBLE,
    SOLAPE_PROFESOR,
    SOLAPE_AULA,
    SOLAPE_SUBGRUPO,
    SOLAPE_GRUPO,
    DISTRIBUCION_MISMO_DIA,
    TUTORIA_SIN_TUTOR
}
