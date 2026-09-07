package es.yaroki.educhronos.app.web.dto;

/**
 * La decisión del usuario para UN subgrupo espejo en un bloque de REPARTO (Bloque S139).
 *
 * <p>{@code subgrupo} es el código del ESPEJO —el que aparece en
 * {@code PlanReplicacionDTO.subgruposACrear}—, no el del original del hermano: el cliente
 * decide sobre lo que se va a crear, que es lo que el plan le ha enseñado.
 *
 * <p><b>{@code plaza} null es LEGÍTIMO</b> y no es un error: significa "no cablear", el grupo
 * nuevo no tiene alumnos en esa vía. El espejo se crea igual y se queda sin plazas. Lo que sí
 * es error es OMITIR la asignación de un espejo de reparto, o mandarla dos veces.
 */
public record AsignacionRequest(
        String subgrupo,
        Long plaza) {
}
