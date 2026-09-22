package es.yaroki.educhronos.app.web.dto;

/**
 * Identidad del curso abierto, tal como la ve la interfaz ({@code GET /api/curso}, S159).
 *
 * @param nombre nombre del curso, o {@code null} si la base no lo trae (condición 6: una
 *     base de antes de S159 se abre sin intervención y sin nombre)
 * @param archivado si el curso abierto es de solo lectura
 * @param propuestaSiguiente el nombre que la interfaz ofrece para el curso nuevo, o
 *     {@code null} cuando no hay nombre del que derivarlo. Va aquí y no se calcula en el
 *     navegador para que la regla «dos años consecutivos» viva en UN sitio
 */
public record CursoDTO(String nombre, boolean archivado, String propuestaSiguiente) {
}
