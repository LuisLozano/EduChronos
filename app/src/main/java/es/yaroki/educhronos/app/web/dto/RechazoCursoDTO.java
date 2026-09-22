package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo de una operación de curso rechazada por una razón prevista (S159).
 *
 * <p><b>Lo construimos nosotros, por el mismo motivo que {@link FalloGeneracionDTO}.</b> El
 * mecanismo de error de Spring está medido como mudo en este proyecto (D-F8.6-ii-a): lo que
 * puebla el {@code reason} de un {@code ResponseStatusException} se lee del
 * {@code MockHttpServletResponse} y no del cuerpo que viaja por la red, así que un test
 * verde sobre él no prueba que el navegador reciba nada. Este record es lo que el navegador
 * recibe, y los tests lo aseveran por {@code jsonPath}.
 *
 * <p><b>El campo se llama {@code message} y está en inglés A PROPÓSITO.</b> Los diecinueve
 * {@code mensaje()} del frontend leen {@code err.error.message} y, si falta,
 * {@code err.error.error} (medido en S159); con cualquier otro nombre el usuario vería sólo
 * «No se pudo … (409)» y la razón se perdería. Es el mismo nombre que pone
 * {@code server.error.include-message=always} en el cuerpo de error de Spring, de modo que
 * la interfaz no tiene que distinguir de dónde viene el rechazo.
 *
 * <p>La guarda de solo lectura de la fase B de C-duplicado-guarda reutilizará este cuerpo:
 * su rechazo es de la misma naturaleza y la interfaz ya sabe mostrarlo.
 *
 * @param causa símbolo estable del hecho ({@code CURSO_ARCHIVADO}, {@code NOMBRE_INVALIDO},
 *     {@code CURSO_YA_EXISTE}). Es lo que la vista puede leer para decidir; NO se traduce
 * @param message texto para el usuario, ya en su idioma
 */
public record RechazoCursoDTO(String causa, String message) {
}
