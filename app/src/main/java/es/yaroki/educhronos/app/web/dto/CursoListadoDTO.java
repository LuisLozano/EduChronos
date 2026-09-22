package es.yaroki.educhronos.app.web.dto;

/**
 * Un curso de la carpeta de datos, tal como lo ve el selector (O-curso, S160,
 * C-selector-curso).
 *
 * <p>El {@code fichero} es la IDENTIDAD —un nombre simple, sin ruta— y es lo que el cliente
 * devuelve en el {@code POST /api/cursos/abrir}: el {@code nombre} puede faltar (una base de
 * antes de S159, condición 6) y no sirve para nombrar a nadie.
 *
 * @param fichero nombre del fichero dentro de la carpeta, p. ej. {@code curso-2026-2027.db}
 * @param nombre nombre del curso, o {@code null} si esa base no lo trae escrito
 * @param archivado si ese curso es de solo lectura
 * @param abierto si es el curso que la aplicación tiene abierto ahora mismo
 */
public record CursoListadoDTO(String fichero, String nombre, boolean archivado, boolean abierto) {
}
