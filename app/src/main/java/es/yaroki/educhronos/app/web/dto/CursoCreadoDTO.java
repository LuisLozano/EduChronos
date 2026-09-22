package es.yaroki.educhronos.app.web.dto;

/**
 * Curso recién creado ({@code POST /api/cursos}, S159).
 *
 * @param nombre nombre del curso nuevo
 * @param fichero nombre del fichero que lo guarda, sin carpeta. Se devuelve porque es lo
 *     que el jefe de estudios tiene que buscar si algún día copia sus cursos a un disco
 */
public record CursoCreadoDTO(String nombre, String fichero) {
}
