package es.yaroki.educhronos.solver.cpsat;

/**
 * Categoría tipada de una regla BLANDA (término del objetivo que el solver
 * minimiza), para atribución CONTRAFACTUAL por celda (Fase 8, Bloque 8.3-B).
 * Espejo de los tres términos blandos que {@code ModeloCpSat} añade a su objetivo
 * y que {@link VerificadorSolucion} recomputa de forma independiente:
 * <ul>
 *   <li>{@code VENTANA_PROFESOR} — huecos de un profesor en un día
 *       ({@link VerificadorSolucion#contarVentanasProfesor}). Propiedad de la
 *       configuración DEL DÍA, no de un tramo: {@code tramoCodigo} null.</li>
 *   <li>{@code INDISPONIBILIDAD_BLANDA} — sesión en un tramo con preferencia de no
 *       impartir ({@link VerificadorSolucion#contarPenalizacionIndisponibilidadBlanda}).
 *       Única regla LOCAL a un tramo: {@code tramoCodigo} no-null.</li>
 *   <li>{@code EXCESO_CONSECUTIVAS} — rachas de más de {@code MAX_CONSECUTIVAS}
 *       sesiones seguidas de un profesor en un día
 *       ({@link VerificadorSolucion#contarPenalizacionConsecutivasProfesor}).
 *       Propiedad de la configuración DEL DÍA: {@code tramoCodigo} null.</li>
 * </ul>
 *
 * <p>A diferencia de {@link ReglaDura}, una regla blanda NO invalida la solución:
 * es un coste que el optimizador minimiza. Su atribución no es culpabilidad sino
 * una DERIVADA (¿cuánto cambia el coste si esta celda no estuviera?), por eso su
 * {@link Penalizacion#delta()} lleva signo.
 */
public enum ReglaBlanda {
    VENTANA_PROFESOR,
    INDISPONIBILIDAD_BLANDA,
    EXCESO_CONSECUTIVAS
}
