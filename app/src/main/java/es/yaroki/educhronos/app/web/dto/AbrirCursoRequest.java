package es.yaroki.educhronos.app.web.dto;

/**
 * Qué curso abrir (O-curso, S160, C-selector-curso).
 *
 * <p>Por FICHERO y no por nombre: el nombre puede faltar —condición 6— y puede repetirse
 * entre bases si alguien copia ficheros a mano, mientras que el fichero es único dentro de la
 * carpeta por definición del sistema de archivos. Es además el mismo valor que devuelve
 * {@code CursoListadoDTO}, de modo que el cliente no compone nada: devuelve lo que se le dio.
 *
 * @param fichero nombre simple del fichero a abrir, tal como vino en el listado
 */
public record AbrirCursoRequest(String fichero) {
}
