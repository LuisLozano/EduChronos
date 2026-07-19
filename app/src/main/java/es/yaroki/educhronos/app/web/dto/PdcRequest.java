package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/grupos/{idPadre}/pdc} (Fase 8, Bloque 8.5-D1): el ÚNICO
 * dato que se pide para dar de alta un grupo de Diversificación (PDC) como sub-recurso
 * de su grupo ordinario padre.
 *
 * <p>Solo el {@code codigo} del grupo PDC. El PADRE viaja en la URL (no en el body) y el
 * {@code nivel} del PDC se HEREDA del padre (I5): por eso ni {@code nivel} ni {@code tipo}
 * aparecen aquí, a diferencia de {@link GrupoRequest}. El {@code tipo} es siempre
 * {@code DIVERSIFICACION_PDC}, fijado por el flujo, no por el cliente.
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code PdcService}.
 */
public record PdcRequest(String codigo) {
}
