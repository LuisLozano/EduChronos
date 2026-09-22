package es.yaroki.educhronos.app.curso;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nombre de un curso académico: {@code 2026/2027}. Utilidades PURAS, sin estado y sin tocar
 * disco, para poder probar la forma del nombre sin montar nada (O-curso, S159).
 *
 * <p><b>Dos años consecutivos, no dos años cualesquiera.</b> {@code 2026/2028} tiene la
 * forma correcta y no es un curso; aceptarlo dejaría al centro con un nombre que nadie
 * volvería a mirar y con una propuesta de curso siguiente sin sentido.
 */
public final class NombreCurso {

    /** Forma del nombre: cuatro dígitos, barra, cuatro dígitos. */
    private static final Pattern FORMA = Pattern.compile("^(\\d{4})/(\\d{4})$");

    /** Prefijo del fichero de un curso archivado o recién creado. */
    static final String PREFIJO_FICHERO = "curso-";

    /** Extensión del fichero, la misma que usa la base abierta. */
    static final String EXTENSION_FICHERO = ".db";

    private NombreCurso() {}

    /**
     * ¿Es un nombre de curso? Exige la forma Y que el segundo año sea el siguiente del
     * primero.
     *
     * @param nombre nombre a comprobar; {@code null} no es válido
     */
    public static boolean valido(String nombre) {
        if (nombre == null) {
            return false;
        }
        Matcher m = FORMA.matcher(nombre);
        if (!m.matches()) {
            return false;
        }
        return Integer.parseInt(m.group(2)) == Integer.parseInt(m.group(1)) + 1;
    }

    /**
     * El curso siguiente: {@code 2025/2026} → {@code 2026/2027}. Es lo que el {@code GET}
     * ofrece como propuesta para no obligar a teclearlo.
     *
     * @throws IllegalArgumentException si el nombre no es válido
     */
    public static String siguiente(String nombre) {
        exigirValido(nombre);
        int primero = Integer.parseInt(nombre.substring(0, 4));
        return (primero + 1) + "/" + (primero + 2);
    }

    /**
     * Nombre del fichero que guarda ese curso: {@code 2026/2027} →
     * {@code curso-2026-2027.db}. La barra no vale en un nombre de fichero en Windows ni es
     * cómoda en POSIX, así que va como guion.
     *
     * @throws IllegalArgumentException si el nombre no es válido
     */
    public static String fichero(String nombre) {
        exigirValido(nombre);
        return PREFIJO_FICHERO + nombre.replace('/', '-') + EXTENSION_FICHERO;
    }

    private static void exigirValido(String nombre) {
        if (!valido(nombre)) {
            throw new IllegalArgumentException("No es un nombre de curso: " + nombre);
        }
    }
}
