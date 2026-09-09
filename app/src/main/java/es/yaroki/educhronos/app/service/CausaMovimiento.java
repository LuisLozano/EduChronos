package es.yaroki.educhronos.app.service;

/**
 * Causa enumerada por la que se rechaza un movimiento de instancia (S143). Es el
 * símbolo estable que viaja en {@code FalloMovimientoDTO.causa} y lo que la rejilla
 * lee para decidir qué decir.
 *
 * <p>Vive en {@code service} y NO conoce {@code HttpStatus}: la traducción a código
 * HTTP es asunto de la capa web, igual que {@code MapeoFalloSolver} traduce el
 * veredicto CP-SAT sin que el solver sepa de HTTP.
 */
public enum CausaMovimiento {
    /** El par (dia, orden) no corresponde a ningún tramo lectivo. → 400 */
    TRAMO_INEXISTENTE,
    /** No existe el horario. → 404 */
    HORARIO_INEXISTENTE,
    /** La instancia no tiene filas en ese horario. → 404 */
    INSTANCIA_INEXISTENTE,
    /** El movimiento haría aparecer violaciones duras que no estaban. → 409 */
    VIOLA_REGLA_DURA,
    /** La instancia está pinada; el pin manda sobre el arrastre. → 409 */
    INSTANCIA_PINADA,
    /**
     * Las dos instancias de un INTERCAMBIO son la misma (S144). No es un 404 —ambas
     * existen— ni un 409 —no hay conflicto con el estado—: es una petición mal formada,
     * porque intercambiar algo consigo mismo no es una operación. → 400
     */
    INSTANCIAS_IGUALES
}
