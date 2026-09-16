package es.yaroki.educhronos.app.exportacion;

import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Por qué recurso se pagina el PDF de horario, y todo lo que ese recurso decide de la
 * página (S150, C-exportacion-pdf-profesor-aula).
 *
 * <p><b>Qué es una vista.</b> La rejilla, la jornada, las fuentes, las bandas y las
 * constantes de maqueta son las MISMAS para cualquier paginación: un horario impreso es
 * un horario impreso. Lo que cambia de una vista a otra es SEIS cosas, ni una más, y son
 * exactamente las que este enum declara: por qué se agrupa, cómo se llama el parámetro,
 * qué dice la clave de lectura, qué texto lleva una entrada, qué rótulo lleva la línea
 * bajo el título y qué bloques tiene la leyenda. {@link HorarioPdf} pregunta; no sabe
 * cuál es la respuesta.
 *
 * <p><b>Por qué métodos abstractos por constante y no un {@code switch} sobre
 * {@code this}.</b> Con métodos abstractos, una constante nueva NO COMPILA hasta
 * implementarlos todos: el compilador es quien exige que una vista se añada entera. Un
 * {@code switch} —con {@code default}, o con una rama olvidada— dejaría añadir una vista
 * a medias y el hueco saldría en tiempo de ejecución, o peor, como una página plausible y
 * equivocada. El precio es un cuerpo por constante, que es donde de todos modos hay que
 * leer qué hace cada vista.
 *
 * <p>Hoy hay UNA sola constante, {@link #GRUPO}, y su comportamiento es EXACTAMENTE el
 * que S149 cableó dentro de {@link HorarioPdf}: los literales no se han reescrito, se han
 * mudado. Ese es el contrato de este paso; añadir vistas es el siguiente.
 */
public enum VistaPdf {

    /**
     * Una página por GRUPO administrativo. La que existe desde S149.
     *
     * <p>La línea bajo el título es el TUTOR del grupo, y la leyenda lleva los profesores
     * a la izquierda y las asignaturas a la derecha: en la página de un grupo el profesor
     * es un dato que hay que traducir, porque la celda solo trae su código.
     */
    GRUPO("grupo") {

        @Override
        public List<String> recursosDe(SesionVistaDTO sesion) {
            return sesion.grupos();
        }

        @Override
        public String claveDeLectura() {
            return "Asignatura - Profesor - Aula";
        }

        @Override
        public String textoDeEntrada(SesionVistaDTO sesion) {
            return sesion.asignaturaCodigo()
                    + " " + String.join(SEPARADOR_LISTA, sesion.profesores())
                    + " " + sesion.aulaCodigo();
        }

        @Override
        public String rotuloDeLinea() {
            return "Tutor: ";
        }

        @Override
        public List<BloqueLeyenda> leyendaDe(List<SesionVistaDTO> sesiones,
                                             Map<String, String> nombresDeProfesor) {
            Set<String> profesores = new TreeSet<>();
            Map<String, String> asignaturas = new TreeMap<>();
            for (SesionVistaDTO s : sesiones) {
                profesores.addAll(s.profesores());
                asignaturas.put(s.asignaturaCodigo(), s.asignaturaNombre());
            }

            List<String> izquierda = new ArrayList<>(profesores.size());
            for (String codigo : profesores) {
                String nombre = nombresDeProfesor.get(codigo);
                izquierda.add(nombre == null ? codigo : entrada(codigo, nombre));
            }
            List<String> derecha = new ArrayList<>(asignaturas.size());
            for (Map.Entry<String, String> e : asignaturas.entrySet()) {
                derecha.add(entrada(e.getKey(), e.getValue()));
            }
            return List.of(new BloqueLeyenda("Profesores", izquierda),
                    new BloqueLeyenda("Asignaturas", derecha));
        }
    };

    /**
     * Une los elementos de una lista dentro del texto de UNA entrada (los profesores de
     * una co-docencia hoy). No es un separador de campos: la entrada se imprime como una
     * sola cadena sin separadores, que es lo que obliga a la clave de lectura.
     */
    static final String SEPARADOR_LISTA = "/";

    /** Separa el código del nombre en la leyenda. Raya, no guion. */
    static final String SEPARADOR_LEYENDA = " — ";

    private final String parametro;

    VistaPdf(String parametro) {
        this.parametro = parametro;
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

    /** Los recursos a cuya página pertenece una sesión. Uno solo, varios, o ninguno. */
    public abstract List<String> recursosDe(SesionVistaDTO sesion);

    /** Dice cómo se lee el texto de una celda, que va sin separadores. */
    public abstract String claveDeLectura();

    /** El texto de UNA entrada de celda: una cadena, nunca campos que luego se partan. */
    public abstract String textoDeEntrada(SesionVistaDTO sesion);

    /**
     * Rótulo de la línea que va bajo el título, delante del valor que trae
     * {@link ContextoPdf#lineaPorRecurso()}. Incluye su separador: se concatena tal cual.
     */
    public abstract String rotuloDeLinea();

    /** Los bloques de la leyenda de una página, con los códigos de ESA página. */
    public abstract List<BloqueLeyenda> leyendaDe(List<SesionVistaDTO> sesiones,
                                                  Map<String, String> nombresDeProfesor);

    /** Una entrada de leyenda, {@code "CÓDIGO — Nombre"}. */
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
