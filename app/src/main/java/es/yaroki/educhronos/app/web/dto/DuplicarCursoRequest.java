package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/cursos} (S159).
 *
 * @param nombreNuevo nombre del curso a crear, con la forma {@code 2026/2027}
 * @param nombreActual nombre del curso actual. Sólo hace falta cuando la base abierta no
 *     trae nombre —viene de antes de S159—, y entonces es obligatorio: ese nombre es el que
 *     se queda archivado y nadie más lo sabe. Si la base ya lo trae, se puede omitir; si se
 *     manda y no coincide, la petición se rechaza en vez de renombrar por sorpresa
 */
public record DuplicarCursoRequest(String nombreNuevo, String nombreActual) {
}
