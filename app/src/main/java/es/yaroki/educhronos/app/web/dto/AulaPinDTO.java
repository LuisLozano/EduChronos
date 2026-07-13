package es.yaroki.educhronos.app.web.dto;

/**
 * Pin de aula de una PLAZA concreta dentro de un bloqueo (§4.7, Bloque 8.2b-iv):
 * fija una plaza de aula VARIABLE a una de sus aulas candidatas. Espejo plano de
 * una fila {@code aula_bloqueada}, referida por códigos naturales.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A). La validación —que la plaza
 * pertenezca a la actividad, no sea de aula fija y el aula sea candidata suya— la
 * hace el servicio, no el DTO.
 */
public record AulaPinDTO(String plazaCodigo, String aulaCodigo) {
}
