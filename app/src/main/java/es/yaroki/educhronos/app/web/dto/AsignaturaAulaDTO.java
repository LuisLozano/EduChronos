package es.yaroki.educhronos.app.web.dto;

/**
 * Un aula de una asignatura tal como sale del {@code GET/PUT /api/asignaturas/{id}/aulas}
 * (S206): el aula por su CÓDIGO (simétrico con {@link AsignaturaAulaRequest}) y su rol como
 * nombre del enum. La asignatura no se repite: la identifica la URL.
 */
public record AsignaturaAulaDTO(
        String aula,
        String rol) {
}
