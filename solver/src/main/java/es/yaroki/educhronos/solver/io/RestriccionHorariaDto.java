package es.yaroki.educhronos.solver.io;

/**
 * DTO de una restricción horaria del profesorado (sección top-level
 * 'restriccionesHorarias' del JSON). Espejo de ProfesorRestriccionHoraria (§4.3).
 *
 * profesor / tramo: referencias por código de negocio (resueltas en el mapper).
 * peso / motivo: opcionales en el JSON (Jackson los deja null si se omiten);
 *                el mapper aplica el default de peso y traduce motivo a Optional.
 */
public record RestriccionHorariaDto(
        String profesor,
        String tramo,
        String tipo,
        Integer peso,
        String motivo) { }