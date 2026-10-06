package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST} y del {@code PUT /api/grupos/{idPadre}/pdc} (Fase 8, Bloque 8.5-D1;
 * {@code PUT} desde S203): lo que se pide para dar de alta un grupo de Diversificación (PDC)
 * como sub-recurso de su grupo ordinario padre, o para cambiar su total declarado.
 *
 * <p>El {@code codigo} del grupo PDC y, opcional, su {@code totalDeclarado} (S203; ausente o
 * null es «sin total»). En el {@code PUT} el {@code codigo} no cambia: tiene que ser el del PDC.
 * El PADRE viaja en la URL (no en el body) y el
 * {@code nivel} del PDC se HEREDA del padre (I5): por eso ni {@code nivel} ni {@code tipo}
 * aparecen aquí, a diferencia de {@link GrupoRequest}. El {@code tipo} es siempre
 * {@code DIVERSIFICACION_PDC}, fijado por el flujo, no por el cliente.
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code PdcService}.
 */
public record PdcRequest(String codigo, Integer totalDeclarado) {
}
