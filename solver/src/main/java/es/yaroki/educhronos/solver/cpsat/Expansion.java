package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import java.util.ArrayList;
import java.util.List;

/**
 * Expande cada {@link Actividad} en sus {@code repeticionesPorSemana}
 * {@link ActividadInstancia}.
 *
 * <p>Es la única definición de "cómo se expande una actividad". La usan tanto
 * {@link ModeloCpSat} (para crear variables) como {@link VerificadorSolucion}
 * (para conocer el conjunto de instancias esperadas). Compartirla evita que
 * las dos lógicas se desincronicen.
 */
final class Expansion {

    private Expansion() {
    }

    /** Instancias de una actividad: índices 1..repeticionesPorSemana. */
    static List<ActividadInstancia> instanciasDe(Actividad actividad) {
        List<ActividadInstancia> out = new ArrayList<>(actividad.repeticionesPorSemana());
        for (int i = 1; i <= actividad.repeticionesPorSemana(); i++) {
            out.add(new ActividadInstancia(actividad, i));
        }
        return out;
    }

    /** Todas las instancias de todas las actividades del problema. */
    static List<ActividadInstancia> todas(ProblemaHorario problema) {
        List<ActividadInstancia> out = new ArrayList<>();
        for (Actividad actividad : problema.actividades()) {
            out.addAll(instanciasDe(actividad));
        }
        return out;
    }
}
