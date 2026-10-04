package es.yaroki.educhronos.app.config;

/**
 * La base la guardó una versión de Educhronos más nueva que esta (O-base-tecnica, S191,
 * C-esquema-versionado).
 *
 * <p>La lanza {@link PreparadorEsquema} ANTES de ejecutar una sola sentencia sobre la base:
 * una versión vieja no sabe qué significa un esquema que todavía no existía cuando se
 * construyó, y abrirla igualmente sería escribir con un modelo que no es el suyo. La base
 * queda como estaba.
 *
 * <p>El {@code message} está escrito para el usuario, porque llega tal cual a dos sitios: el
 * diálogo de fallo de arranque ({@code FalloArranque}) y el rechazo al cambiar de curso
 * ({@code CursoService}, causa {@code CURSO_VERSION_POSTERIOR}).
 */
public class EsquemaPosteriorException extends RuntimeException {

    private final int deLaBase;

    private final int maximo;

    /**
     * @param deLaBase el {@code user_version} que trae la base
     * @param maximo el esquema más alto que sabe abrir esta versión
     */
    public EsquemaPosteriorException(int deLaBase, int maximo) {
        super(
                "Esta base de datos la guardó una versión más nueva de Educhronos (esquema "
                        + deLaBase
                        + "), y esta versión solo sabe abrir hasta el esquema "
                        + maximo
                        + ". Instala la última versión de Educhronos para abrirla.");
        this.deLaBase = deLaBase;
        this.maximo = maximo;
    }

    /** El {@code user_version} que trae la base. */
    public int getDeLaBase() {
        return deLaBase;
    }

    /** El esquema más alto que sabe abrir esta versión: {@link PreparadorEsquema#VERSION_ESQUEMA}. */
    public int getMaximo() {
        return maximo;
    }
}
