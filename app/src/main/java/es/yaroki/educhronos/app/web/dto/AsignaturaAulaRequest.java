package es.yaroki.educhronos.app.web.dto;

/**
 * Un elemento del cuerpo del {@code PUT /api/asignaturas/{id}/aulas}: qué aula usa la asignatura y
 * con qué rol (S206, C-reglas-aulas). La asignatura NO viaja en el body: la lleva la URL.
 *
 * <p>{@code aula} viaja como el CÓDIGO del aula, como las aulas de una plaza en
 * {@code PlazaRequest} y el aula de referencia en {@code GrupoRequest}; un código que no existe es
 * un {@code 400} que lo nombra. {@code rol} entra como {@code String} para que
 * {@code AsignaturaService} pueda devolver un {@code 400} que nombre el valor y liste los válidos
 * ({@code EXCLUSIVA}, {@code PREFERIDA}), como {@code TutoriaRequest.rol}.
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code AsignaturaService}.
 */
public record AsignaturaAulaRequest(
        String aula,
        String rol) {
}
