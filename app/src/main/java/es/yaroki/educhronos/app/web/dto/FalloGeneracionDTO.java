package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo de un {@code POST /api/horarios} que falla en el solver (S118).
 *
 * <p><b>Lo construimos nosotros, a propósito.</b> El mecanismo de error de Spring
 * está medido como mudo en este proyecto (D-F8.6-ii-a): lo que puebla el
 * {@code reason} de un {@code ResponseStatusException} se lee del
 * {@code MockHttpServletResponse}, no del cuerpo que viaja por la red, de modo que
 * un test verde sobre él no prueba que el navegador reciba nada. Este record es lo
 * que el navegador recibe.
 *
 * @param causa    símbolo estable del hecho ({@code CATALOGO_INFACTIBLE},
 *                 {@code PRESUPUESTO_AGOTADO}, {@code CONFIGURACION_INCOMPLETA},
 *                 {@code ERROR_INTERNO}). Es lo que la vista lee para decidir qué
 *                 decir; NO es texto para el usuario y no se traduce.
 * @param mensaje  la prosa del solver, para el log y para quien depure. La vista no
 *                 la usa para decidir nada.
 * @param estado   {@code CpSolverStatus} de la corrida, o null si no hubo solve.
 * @param segundos presupuesto con que se corrió, o null si no hubo solve. Es lo que
 *                 hace accionable un {@code PRESUPUESTO_AGOTADO}: sin él, "se agotó
 *                 el tiempo" no dice cuánto tiempo era.
 */
public record FalloGeneracionDTO(String causa, String mensaje, String estado, Integer segundos) {
}
