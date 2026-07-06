package es.yaroki.educhronos.solver.io;

/**
 * DTO de un bloqueo manual de instancia a tramo (sección top-level 'bloqueos'
 * del JSON). Espejo de {@code domain.SesionBloqueada} (Fase 8, Bloque 8.2a).
 *
 * actividad / tramo: referencias por código de negocio (resueltas en el mapper).
 * indice: la ocurrencia de la actividad a bloquear (1..repeticionesPorSemana); el
 *         mapper la valida al construir la {@code ActividadInstancia}.
 *
 * <p>Sin campo de aula: el pin de aula se difiere a 8.2b.
 */
public record SesionBloqueadaDto(String actividad, int indice, String tramo) { }
