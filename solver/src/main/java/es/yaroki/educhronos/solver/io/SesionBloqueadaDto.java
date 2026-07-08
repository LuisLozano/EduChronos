package es.yaroki.educhronos.solver.io;

import java.util.List;

/**
 * DTO de un bloqueo manual (sección top-level 'bloqueos' del JSON). Espejo de
 * {@code domain.SesionBloqueada} (Fase 8, Bloques 8.2a y 8.2b).
 *
 * actividad / tramo: referencias por código de negocio (resueltas en el mapper).
 * indice: la ocurrencia de la actividad a bloquear (1..repeticionesPorSemana); el
 *         mapper la valida al construir la {@code ActividadInstancia}.
 * aulasPinadas: pin opcional de aula por plaza (Bloque 8.2b). Ausente o vacío =
 *         pin de solo tramo (retrocompat 8.2a). El mapper resuelve códigos y valida
 *         que cada plaza sea de aula variable y el aula esté entre sus candidatas.
 */
public record SesionBloqueadaDto(String actividad, int indice, String tramo,
                                 List<AulaPinDto> aulasPinadas) { }
