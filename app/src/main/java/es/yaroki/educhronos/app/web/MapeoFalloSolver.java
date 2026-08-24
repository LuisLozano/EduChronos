package es.yaroki.educhronos.app.web;

import org.springframework.http.HttpStatus;

/**
 * Traduce el veredicto CP-SAT de un solve fallido al par (status HTTP, causa) que
 * verá el cliente. Función PURA y sin estado: no toca Spring, no toca el solver.
 *
 * <p><b>Por qué existe.</b> Hasta S118 los tres desenlaces posibles de un solve sin
 * solución —catálogo imposible, presupuesto agotado y modelo inválido— llegaban al
 * navegador como el MISMO {@code 422} con el mismo cuerpo. Son hechos distintos y
 * piden reacciones distintas: al primero se le corrige el catálogo, al segundo se
 * le da más tiempo y al tercero se le abre un parte de error. Un único status no
 * permite a la UI decir cuál de los tres ocurrió, y por eso la vista no puede
 * ofrecer "reintenta" sin arriesgarse a proponerlo ante un catálogo imposible.
 *
 * <p><b>La tabla.</b> {@code INFEASIBLE} es el único que conserva el 422 de antes,
 * porque es el único en que 422 dice la verdad: CP-SAT PROBÓ que no hay solución.
 * {@code UNKNOWN} no probó nada —se le acabó el tiempo— y va a 503, el status que
 * autoriza a reintentar. Un estado ausente significa que no hubo solve y también es
 * 422, con causa propia. Todo lo demás es defecto nuestro: 500.
 */
final class MapeoFalloSolver {

    private MapeoFalloSolver() {
    }

    /**
     * Respuesta que corresponde a un solve fallido: el status HTTP y una causa
     * estable y legible por máquina.
     *
     * <p>La {@code causa} es un símbolo del contrato, NO un texto para el usuario:
     * la vista decide con ella qué decir y en qué idioma. Por eso no se deriva del
     * mensaje de la excepción, que es prosa y cambia sin avisar.
     */
    record RespuestaFallo(HttpStatus status, String causa) {
    }

    /**
     * @param estado nombre del {@code CpSolverStatus} con que terminó el solve.
     * @return el par (status, causa) con que responder.
     */
    static RespuestaFallo mapear(String estado) {
        // ANTES del switch, y no como una rama más de él: un estado ausente NO es un
        // estado desconocido. Significa que NO HUBO SOLVE —se abortó construyendo el
        // modelo, sin que llegara a existir un CpSolver del que sacar un veredicto—,
        // y eso es un catálogo al que le falta algo, no un fallo interno. Mandarlo a
        // la rama por defecto lo convertiría en un 500 sobre el caso más probable de
        // una instalación recién estrenada: un centro sin jornada definida.
        if (estado == null) {
            return new RespuestaFallo(
                    HttpStatus.UNPROCESSABLE_ENTITY, "CONFIGURACION_INCOMPLETA");
        }
        return switch (estado) {
            case "INFEASIBLE" ->
                    new RespuestaFallo(HttpStatus.UNPROCESSABLE_ENTITY, "CATALOGO_INFACTIBLE");
            case "UNKNOWN" ->
                    new RespuestaFallo(HttpStatus.SERVICE_UNAVAILABLE, "PRESUPUESTO_AGOTADO");
            // MODEL_INVALID entra por aquí igual que cualquier estado que OR-Tools
            // añada en el futuro: ambos son "esto no lo previmos nosotros".
            default ->
                    new RespuestaFallo(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO");
        };
    }
}
