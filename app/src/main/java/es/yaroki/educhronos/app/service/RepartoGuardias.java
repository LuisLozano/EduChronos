package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reparto de las guardias ordinarias sobre un horario ya resuelto (S213, C-reparto-guardias,
 * condición 2 de O-guardias). Función pura y determinista: no es un bean ni guarda estado.
 *
 * <p><b>Red.</b> Fuente → profesor, con capacidad sus guardias; profesor → tramo, con capacidad 1,
 * solo si el profesor está libre en ese tramo (no lo ocupa ninguna instancia en la que figura ni
 * lo tiene marcado como «No puede»); tramo → sumidero. Los tramos son los del problema, que ya son
 * todos lectivos (E2). El profesor se identifica por su código y el tramo por su posición en
 * {@code problema.tramos()}; los nodos van en ese orden, código y posición, para que el resultado
 * no dependa de nada más (E3).
 *
 * <p><b>Dos fases sobre la misma red (E11).</b> En la primera, cada tramo → sumidero lleva como
 * capacidad el mínimo de profesores de guardia por tramo: si algún tramo no llega, el reparto falla
 * y dice cuáles y con cuántas. En la segunda se añade a cada tramo una arista paralela al sumidero
 * con capacidad el total de guardias y se sigue aumentando sin rehacer la primera: el flujo que ya
 * pasaba no se pierde, así que ningún tramo baja del mínimo. Las guardias son el flujo de las
 * aristas profesor → tramo.
 *
 * <p><b>La ocupación se calcula aquí</b>, a partir de la {@link SolucionHorario}: todo tramo que
 * cubre cada instancia colocada, contando su duración, para cada profesor de cualquiera de sus
 * plazas. No usa {@code VerificadorSolucion.tramosOcupados} ni {@code SesionVistaDTO.tramosCubiertos}:
 * el diagnóstico comprueba el reparto por ese otro camino (decisión G de O-disponibilidad).
 *
 * <p><b>Invariante, no comprobación.</b> Si la pre-validación no da GUARDIAS_SIN_HUECO, la segunda
 * fase coloca todas las guardias: cada profesor tiene libres los tramos lectivos menos sus «No
 * puede» y menos lo que ocupa, y eso es lo que aquella regla compara con sus guardias. Si aun así
 * quedara alguna sin colocar, es un error de programación y se lanza {@link IllegalStateException}.
 */
final class RepartoGuardias {

    /** Una guardia: el profesor, por código, y el tramo, por su posición en {@code problema.tramos()}. */
    record Asignacion(String profesorCodigo, int tramo) { }

    /** Un tramo que no llega al mínimo en la primera fase: cuántas guardias alcanzó y el mínimo. */
    record Deficit(Tramo tramo, int alcanzadas, int minimo) { }

    /** Las guardias repartidas, o los tramos que no llegan al mínimo si el reparto falla. */
    record Resultado(List<Asignacion> asignaciones, List<Deficit> deficits) {

        Resultado {
            asignaciones = List.copyOf(asignaciones);
            deficits = List.copyOf(deficits);
        }

        boolean fallo() {
            return !deficits.isEmpty();
        }
    }

    private RepartoGuardias() {
    }

    /**
     * Reparte las guardias de cada profesor sobre los tramos del problema.
     *
     * @param guardiasPorProfesor guardias ordinarias por código de profesor; un profesor con 0 no
     *     entra en la red
     * @param minimo profesores de guardia que necesita cada tramo
     */
    static Resultado repartir(ProblemaHorario problema, SolucionHorario solucion,
                              Map<String, Integer> guardiasPorProfesor, int minimo) {
        List<Tramo> tramos = problema.tramos();
        Map<Tramo, Integer> posicion = new HashMap<>();
        for (int t = 0; t < tramos.size(); t++) {
            posicion.put(tramos.get(t), t);
        }
        Map<String, Set<Integer>> noLibres = new HashMap<>();
        ocupacion(problema, solucion, posicion, noLibres);
        for (RestriccionHoraria restriccion : problema.restriccionesHorarias()) {
            if (restriccion.tipo() == TipoRestriccion.DURA) {
                noLibres.computeIfAbsent(restriccion.profesor().codigo(), c -> new HashSet<>())
                        .add(posicion.get(restriccion.tramo()));
            }
        }

        // Profesores con guardias, por código (TreeMap: orden natural del código).
        Map<String, Integer> conGuardias = new TreeMap<>();
        guardiasPorProfesor.forEach((codigo, guardias) -> {
            if (guardias != null && guardias > 0) {
                conGuardias.put(codigo, guardias);
            }
        });
        List<String> profesores = List.copyOf(conGuardias.keySet());
        int total = conGuardias.values().stream().mapToInt(Integer::intValue).sum();

        int fuente = 0;
        int primerTramo = 1 + profesores.size();
        int sumidero = primerTramo + tramos.size();
        RedDeFlujo red = new RedDeFlujo(sumidero + 1);
        int[][] aristaGuardia = new int[profesores.size()][tramos.size()];
        for (int p = 0; p < profesores.size(); p++) {
            String codigo = profesores.get(p);
            red.arista(fuente, 1 + p, conGuardias.get(codigo));
            Set<Integer> ocupados = noLibres.getOrDefault(codigo, Set.of());
            for (int t = 0; t < tramos.size(); t++) {
                aristaGuardia[p][t] = ocupados.contains(t) ? -1 : red.arista(1 + p, primerTramo + t, 1);
            }
        }
        int[] aristaMinimo = new int[tramos.size()];
        for (int t = 0; t < tramos.size(); t++) {
            aristaMinimo[t] = red.arista(primerTramo + t, sumidero, minimo);
        }

        // Fase 1: el mínimo de cada tramo.
        red.flujoMaximo(fuente, sumidero);
        List<Deficit> deficits = new ArrayList<>();
        for (int t = 0; t < tramos.size(); t++) {
            int alcanzadas = red.flujo(aristaMinimo[t]);
            if (alcanzadas < minimo) {
                deficits.add(new Deficit(tramos.get(t), alcanzadas, minimo));
            }
        }
        if (!deficits.isEmpty()) {
            return new Resultado(List.of(), deficits);
        }

        // Fase 2: el resto de las guardias, sin tope por tramo y sin rehacer la fase 1.
        for (int t = 0; t < tramos.size(); t++) {
            red.arista(primerTramo + t, sumidero, total);
        }
        red.flujoMaximo(fuente, sumidero);

        List<Asignacion> asignaciones = new ArrayList<>();
        for (int p = 0; p < profesores.size(); p++) {
            for (int t = 0; t < tramos.size(); t++) {
                if (aristaGuardia[p][t] >= 0 && red.flujo(aristaGuardia[p][t]) == 1) {
                    asignaciones.add(new Asignacion(profesores.get(p), t));
                }
            }
        }
        if (asignaciones.size() != total) {
            throw new IllegalStateException("El reparto de guardias colocó " + asignaciones.size()
                    + " de " + total + " sin que la pre-validación diera GUARDIAS_SIN_HUECO");
        }
        return new Resultado(asignaciones, List.of());
    }

    /**
     * Los tramos que ocupa cada profesor en la solución, por posición: para cada instancia
     * colocada, su tramo de inicio y los {@code duracionTramos − 1} siguientes del mismo día, para
     * cada profesor de cualquiera de sus plazas.
     */
    private static void ocupacion(ProblemaHorario problema, SolucionHorario solucion,
                                  Map<Tramo, Integer> posicion, Map<String, Set<Integer>> noLibres) {
        for (Map.Entry<ActividadInstancia, Tramo> colocada : solucion.asignaciones().entrySet()) {
            Tramo inicio = colocada.getValue();
            int duracion = colocada.getKey().actividad().duracionTramos();
            Set<Integer> cubiertos = new HashSet<>();
            for (Tramo tramo : problema.tramos()) {
                if (tramo.diaSemana() == inicio.diaSemana()
                        && tramo.ordenEnDia() >= inicio.ordenEnDia()
                        && tramo.ordenEnDia() < inicio.ordenEnDia() + duracion) {
                    cubiertos.add(posicion.get(tramo));
                }
            }
            for (Plaza plaza : colocada.getKey().actividad().plazas()) {
                for (Profesor profesor : plaza.profesores()) {
                    noLibres.computeIfAbsent(profesor.codigo(), c -> new HashSet<>()).addAll(cubiertos);
                }
            }
        }
    }
}
