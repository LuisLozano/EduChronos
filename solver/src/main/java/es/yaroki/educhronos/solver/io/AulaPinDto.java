package es.yaroki.educhronos.solver.io;

/**
 * DTO de un pin de aula por plaza dentro de un bloqueo (Fase 8, Bloque 8.2b).
 * Item del array 'aulasPinadas' de {@code SesionBloqueadaDto}.
 *
 * plaza / aula: referencias por código de negocio (resueltas en el mapper). La
 * plaza es una de las plazas de la actividad bloqueada; el aula, una de sus
 * {@code aulasCandidatas}.
 */
public record AulaPinDto(String plaza, String aula) { }
