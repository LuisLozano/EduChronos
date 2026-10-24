package es.yaroki.educhronos.app.service;

import java.util.List;
import java.util.Objects;

/**
 * Lo que la prevalidación de niveles necesita y el {@code ProblemaHorario} del solver no lleva
 * (S251, C-editor-por-nivel, condición 5 de O-actividad-por-nivel): el nivel de cada asignatura y
 * los niveles de los grupos que asisten a cada actividad. El solver no razona por niveles, así que
 * su problema no los trae. Molde de {@link DatosAulas}.
 *
 * <p>Lo construye {@link GeneradorHorarioService#cargarDatosNiveles()}; lo usan el GET de la
 * prevalidación y la generación, que tienen que dar los mismos AVISO.
 *
 * @param actividades cada actividad con sus asignaturas y sus niveles, en el orden del catálogo
 * @param asignaturas cada asignatura con su código corto, su nombre y su nivel
 */
public record DatosNiveles(List<ActividadNiveles> actividades, List<AsignaturaNivel> asignaturas) {

    /** Sin actividades ni asignaturas: las reglas de niveles no emiten nada. */
    public static final DatosNiveles VACIO = new DatosNiveles(List.of(), List.of());

    public DatosNiveles {
        Objects.requireNonNull(actividades, "actividades no puede ser null");
        Objects.requireNonNull(asignaturas, "asignaturas no puede ser null");
        actividades = List.copyOf(actividades);
        asignaturas = List.copyOf(asignaturas);
    }

    /**
     * Una actividad, por códigos: la asignatura propia (null si no tiene), la de cada plaza en su
     * orden, y los niveles de los grupos de los subgrupos de todas sus plazas, sin repetir y en el
     * orden pedagógico de los niveles. Una reunión o una función, sin subgrupos, no tiene niveles.
     */
    public record ActividadNiveles(
            String codigo, String asignatura, List<String> asignaturasDePlazas, List<String> niveles) {
        public ActividadNiveles {
            Objects.requireNonNull(codigo, "codigo no puede ser null");
            Objects.requireNonNull(asignaturasDePlazas, "asignaturasDePlazas no puede ser null");
            Objects.requireNonNull(niveles, "niveles no puede ser null");
            asignaturasDePlazas = List.copyOf(asignaturasDePlazas);
            niveles = List.copyOf(niveles);
        }
    }

    /**
     * Una asignatura: su código compuesto, que es el que usan las actividades y las plazas, su
     * código corto, su nombre y el código de su nivel. El corto y el nivel pueden faltar (null).
     */
    public record AsignaturaNivel(String codigo, String codigoCorto, String nombre, String nivel) {
        public AsignaturaNivel {
            Objects.requireNonNull(codigo, "codigo no puede ser null");
            Objects.requireNonNull(nombre, "nombre no puede ser null");
        }
    }
}
