package es.yaroki.educhronos.app.exportacion;

import es.yaroki.educhronos.app.web.dto.GuardiaVistaDTO;
import java.util.List;
import java.util.Optional;

/**
 * Qué PDF de horario pide el parámetro {@code vista} de {@code GET /api/horarios/{id}/pdf}:
 * el valor del parámetro, el sufijo del fichero y la rama del {@code switch} que compone su
 * contexto en {@code ExportacionHorarioService}. {@link HorarioPdf#escribir} decide por ella
 * el CAMINO de dibujo.
 *
 * <p><b>Dos caminos (S215, C-exportacion-guardias, enmienda ED1).</b> Las vistas de grupo,
 * profesor y aula son una página por recurso y lo que las distingue lo declara
 * {@link PaginaPorRecurso}, con un método abstracto por cosa que cambia. La de
 * {@link #GUARDIAS} es UNA sola página sin recursos ni sesiones: no se pagina por nada, así que
 * no tiene paginación por recurso, y {@link HorarioPdf} la dibuja por un camino propio que
 * reutiliza la rejilla, la columna de horas, el recreo, el título y la leyenda. Por eso los
 * métodos de sesión no están aquí: una constante de guardias tendría que implementarlos con
 * un cuerpo vacío o que lanzara, y ninguno de los dos dice la verdad.
 *
 * <p>Lo que queda en este enum es lo que COMPARTEN los dos caminos: el separador de lista, el
 * formato «CÓDIGO — Nombre», el bloque de leyenda y la palabra con que se nombra una guardia.
 */
public enum VistaPdf {

    /** Una página por grupo administrativo ({@link PaginaPorRecurso#GRUPO}). */
    GRUPO("grupo"),

    /** Una página por profesor, con sus guardias ({@link PaginaPorRecurso#PROFESOR}). */
    PROFESOR("profesor"),

    /** Una página por aula del catálogo ({@link PaginaPorRecurso#AULA}). */
    AULA("aula"),

    /**
     * Una sola página con las guardias del horario (S215; las de recreo, S221): en cada celda,
     * los códigos de los profesores de guardia, en cada recreo los de recreo y, aparte, los de
     * biblioteca, y en la leyenda sus nombres. Ver {@link HorarioPdf} para lo que la dibuja.
     */
    GUARDIAS("guardias");

    /** Título de la página de guardias (D2; «Guardias» desde S221, X4). */
    static final String TITULO_GUARDIAS = "Guardias";

    /**
     * Delante de los códigos de biblioteca en la celda de un recreo de la página de guardias,
     * que van en línea aparte (S221, X3): «Biblioteca: COD».
     */
    static final String PREFIJO_BIBLIOTECA = "Biblioteca: ";

    /** Clave de lectura de la página de guardias (enmienda ED2). */
    static final String CLAVE_GUARDIAS = "Profesores de guardia";

    /** Une los códigos de una celda de la página de guardias (D3). Con espacio: se parte ahí. */
    static final String SEPARADOR_CODIGOS = ", ";

    private final String parametro;

    VistaPdf(String parametro) {
        this.parametro = parametro;
    }

    /**
     * Une los elementos de una lista dentro del texto de UNA entrada (los profesores de
     * una co-docencia, los grupos de una clase juntada). No es un separador de campos: la
     * entrada se imprime como una sola cadena sin separadores, que es lo que obliga a la
     * clave de lectura. {@link HorarioPdf} lo lee para saber dónde puede partir una línea.
     */
    static final String SEPARADOR_LISTA = "/";

    /** Separa el código del nombre en la leyenda y en el título. Raya, no guion. */
    static final String SEPARADOR_LEYENDA = " — ";

    /**
     * La palabra con la que la exportación nombra una guardia ordinaria (S215): el «Nombre
     * asignatura» de su fila en el CSV y su entrada de celda en el PDF de profesor. No es una
     * asignatura y no tiene línea de leyenda.
     */
    static final String ROTULO_GUARDIA = "Guardia";

    /**
     * La palabra con la que la exportación nombra una guardia de BIBLIOTECA (S221, X2), en los
     * mismos sitios que {@link #ROTULO_GUARDIA}. Una de RECREO se nombra como una ordinaria:
     * lo que la distingue es dónde va, un recreo y no un tramo de clase.
     */
    static final String ROTULO_BIBLIOTECA = "Biblioteca";

    /**
     * El rótulo del recreo: el texto de su banda en el PDF y el «Tramo» de una fila de recreo en
     * el CSV, que para el recreo n &gt; 1 lleva el número detrás (S221, X5).
     */
    static final String ROTULO_RECREO = "Recreo";

    /** El rótulo de una guardia según su tipo: «Biblioteca» o «Guardia» (S221, X1 y X2). */
    static String rotuloDe(GuardiaVistaDTO guardia) {
        return GuardiaVistaDTO.BIBLIOTECA.equals(guardia.tipo()) ? ROTULO_BIBLIOTECA : ROTULO_GUARDIA;
    }

    /**
     * Un bloque de la leyenda del pie: su encabezado en negrita y sus líneas ya
     * formateadas. Cada bloque ocupa UNA columna, y son columnas independientes y no una
     * lista partida: con un encabezado encima, una entrada colada bajo el encabezado
     * ajeno sería una mentira tipográfica.
     *
     * @param encabezado rótulo en negrita sobre la columna
     * @param lineas entradas ya compuestas, en el orden en que se imprimen
     */
    public record BloqueLeyenda(String encabezado, List<String> lineas) {
    }

    /** El valor del query param {@code vista} y el sufijo del nombre de fichero. */
    public String parametro() {
        return parametro;
    }

    /** Un código con su nombre, {@code "CÓDIGO — Nombre"}. */
    static String entrada(String codigo, String nombre) {
        return codigo + SEPARADOR_LEYENDA + nombre;
    }

    /**
     * La vista que nombra ese parámetro, si alguna. {@link Optional#empty()} para un valor
     * desconocido Y para {@code null}: quien pregunta es la frontera HTTP, donde el
     * parámetro ausente y el parámetro inventado merecen el mismo trato —ninguna vista— y
     * no una excepción distinta cada uno.
     */
    public static Optional<VistaPdf> desdeParametro(String parametro) {
        for (VistaPdf vista : values()) {
            if (vista.parametro.equals(parametro)) {
                return Optional.of(vista);
            }
        }
        return Optional.empty();
    }
}
