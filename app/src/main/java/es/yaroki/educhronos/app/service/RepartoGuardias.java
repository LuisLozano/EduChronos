package es.yaroki.educhronos.app.service;

import com.google.ortools.Loader;
import com.google.ortools.graph.MinCostFlow;
import com.google.ortools.graph.MinCostFlowBase;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reparto de las guardias sobre un horario ya resuelto: las ordinarias sobre los tramos de clase (S213,
 * C-reparto-guardias, condición 2 de O-guardias) y, desde S220 (C-reparto-equilibrado, condiciones 2 y 3
 * de O-guardias-especiales), las de recreo sobre los recreos. Función pura y determinista: no es un
 * bean ni guarda estado.
 *
 * <p><b>Columnas.</b> Las dos familias pasan por el mismo núcleo, {@link #repartirPorColumnas}: cada
 * profesor con guardias tiene que hacer exactamente las suyas, como mucho una por columna y solo en
 * las que tiene libres, y cada columna necesita un mínimo. Para las ordinarias, las columnas son los
 * tramos del problema, que ya son todos lectivos (E2), y un profesor no está libre donde lo ocupa
 * una instancia en la que figura ni donde tiene un «No puede». Para el recreo, las columnas son los
 * recreos y todos están libres: el problema no los trae y la disponibilidad no puede nombrarlos. El
 * profesor se identifica por su código y la columna por su posición; los nodos van en ese orden,
 * código y posición, para que el resultado no dependa de nada más (E3).
 *
 * <p><b>Fase 1: el mínimo (E11 de S213).</b> Flujo máximo con {@link RedDeFlujo}: fuente → profesor,
 * con capacidad sus guardias; profesor → columna, con capacidad 1 si está libre; columna → sumidero,
 * con capacidad el mínimo. Si alguna columna no llega, el reparto falla y dice cuáles y con cuántas.
 *
 * <p><b>Fase 2: el equilibrio (S220).</b> Si la fase 1 llega, se reparten TODAS las guardias con un
 * flujo de coste mínimo de OR-Tools sobre una red nueva: la misma fuente, profesores y columnas, cada
 * columna con una demanda del mínimo y, hacia el sumidero, una arista de capacidad 1 y coste 2k − 1
 * por cada guardia k = mínimo + 1 … L (L, los profesores con guardias libres en esa columna). Como
 * Σ (2k − 1) entre mínimo + 1 y x vale x² − mínimo², el óptimo es el reparto con cada columna en su
 * mínimo o por encima que hace mínima la suma de los cuadrados de las guardias de cada columna.
 *
 * <p><b>La ocupación se calcula aquí</b>, a partir de la {@link SolucionHorario}: todo tramo que
 * cubre cada instancia colocada, contando su duración, para cada profesor de cualquiera de sus
 * plazas. No usa {@code VerificadorSolucion.tramosOcupados} ni {@code SesionVistaDTO.tramosCubiertos}:
 * el diagnóstico comprueba el reparto por ese otro camino (decisión G de O-disponibilidad).
 *
 * <p><b>Invariante, no comprobación.</b> Si la pre-validación no da GUARDIAS_SIN_HUECO, la segunda
 * fase coloca todas las guardias: cada profesor tiene libres los tramos lectivos menos sus «No
 * puede» y menos lo que ocupa, y eso es lo que aquella regla compara con sus guardias. Si aun así
 * quedara alguna sin colocar, o el flujo de coste mínimo no diera un óptimo, es un error de
 * programación y se lanza {@link IllegalStateException}.
 */
final class RepartoGuardias {

    static {
        // Carga los binarios nativos de OR-Tools, como SolverHorario. Debe ocurrir antes de
        // crear un MinCostFlow: el ajuste a mano reparte sin haber pasado por el solver, y
        // olvidarlo provoca UnsatisfiedLinkError. Un bloque estático garantiza una sola carga.
        Loader.loadNativeLibraries();
    }

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

    /**
     * Una guardia de recreo (S220): el profesor, por código; el recreo, por su posición en la lista
     * ordenada de recreos de la jornada; y si es de biblioteca.
     */
    record AsignacionRecreo(String profesorCodigo, int recreo, boolean biblioteca) { }

    /** Un recreo que no llega al mínimo en la primera fase: cuántas guardias alcanzó y el mínimo. */
    record DeficitRecreo(int recreo, int alcanzadas, int minimo) { }

    /** Un profesor con más guardias de recreo que recreos: como mucho hace una en cada uno. */
    record Excedido(String profesorCodigo, int guardias, int recreos) { }

    /**
     * Las guardias de recreo repartidas o, si el reparto falla, por qué: los recreos que no llegan al
     * mínimo, los profesores excedidos o un mínimo de biblioteca mayor que el de recreo. Los dos
     * últimos se detectan antes de armar la red; los déficits, solo si ellos no fallan.
     */
    record ResultadoRecreo(List<AsignacionRecreo> asignaciones, List<DeficitRecreo> deficits,
                           List<Excedido> excedidos, boolean bibliotecaSuperaRecreo) {

        ResultadoRecreo {
            asignaciones = List.copyOf(asignaciones);
            deficits = List.copyOf(deficits);
            excedidos = List.copyOf(excedidos);
        }

        boolean fallo() {
            return !deficits.isEmpty() || !excedidos.isEmpty() || bibliotecaSuperaRecreo;
        }
    }

    /**
     * Lo que devuelve el núcleo: las guardias como {profesor, columna}, con el profesor por su posición
     * en la lista recibida, o las columnas que no llegan al mínimo como {columna, alcanzadas}.
     */
    private record Columnas(List<int[]> asignaciones, List<int[]> deficits) { }

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
        Map<String, Integer> conGuardias = conGuardias(guardiasPorProfesor);
        List<String> profesores = List.copyOf(conGuardias.keySet());
        List<Set<Integer>> ocupados = new ArrayList<>(profesores.size());
        for (String codigo : profesores) {
            ocupados.add(noLibres.getOrDefault(codigo, Set.of()));
        }

        Columnas columnas = repartirPorColumnas(List.copyOf(conGuardias.values()), tramos.size(), ocupados, minimo);
        List<Deficit> deficits = new ArrayList<>();
        for (int[] deficit : columnas.deficits()) {
            deficits.add(new Deficit(tramos.get(deficit[0]), deficit[1], minimo));
        }
        List<Asignacion> asignaciones = new ArrayList<>();
        for (int[] guardia : columnas.asignaciones()) {
            asignaciones.add(new Asignacion(profesores.get(guardia[0]), guardia[1]));
        }
        return new Resultado(asignaciones, deficits);
    }

    /**
     * Reparte las guardias de recreo de cada profesor sobre los recreos (S220), como mucho una por
     * recreo, con al menos {@code minimoRecreo} en cada uno, equilibradas como las ordinarias. Después
     * marca la biblioteca (decisión M): recreo a recreo, en su orden, EXACTAMENTE
     * {@code minimoBiblioteca} de los asignados, los que menos bibliotecas llevan, y a igualdad el de
     * código menor.
     *
     * @param recreos número de recreos de la jornada; las columnas son sus posiciones
     * @param guardiasRecreo guardias de recreo por código de profesor; un profesor con 0 no entra
     */
    static ResultadoRecreo repartirRecreo(int recreos, Map<String, Integer> guardiasRecreo,
                                          int minimoRecreo, int minimoBiblioteca) {
        Map<String, Integer> conGuardias = conGuardias(guardiasRecreo);
        boolean bibliotecaSupera = minimoBiblioteca > minimoRecreo;
        List<Excedido> excedidos = new ArrayList<>();
        conGuardias.forEach((codigo, guardias) -> {
            if (guardias > recreos) {
                excedidos.add(new Excedido(codigo, guardias, recreos));
            }
        });
        if (bibliotecaSupera || !excedidos.isEmpty()) {
            return new ResultadoRecreo(List.of(), List.of(), excedidos, bibliotecaSupera);
        }

        List<String> profesores = List.copyOf(conGuardias.keySet());
        // Ningún profesor tiene un recreo ocupado: el problema no trae recreos.
        List<Set<Integer>> ninguno = new ArrayList<>(profesores.size());
        for (int p = 0; p < profesores.size(); p++) {
            ninguno.add(Set.of());
        }
        Columnas columnas = repartirPorColumnas(List.copyOf(conGuardias.values()), recreos, ninguno, minimoRecreo);
        if (!columnas.deficits().isEmpty()) {
            List<DeficitRecreo> deficits = new ArrayList<>();
            for (int[] deficit : columnas.deficits()) {
                deficits.add(new DeficitRecreo(deficit[0], deficit[1], minimoRecreo));
            }
            return new ResultadoRecreo(List.of(), deficits, List.of(), false);
        }

        // La biblioteca: por recreo, los asignados por posición de profesor, que es el orden del código.
        List<List<Integer>> asignados = new ArrayList<>(recreos);
        for (int r = 0; r < recreos; r++) {
            asignados.add(new ArrayList<>());
        }
        for (int[] guardia : columnas.asignaciones()) {
            asignados.get(guardia[1]).add(guardia[0]);
        }
        int[] acumuladas = new int[profesores.size()];
        Set<Long> enBiblioteca = new HashSet<>();
        for (int r = 0; r < recreos; r++) {
            List<Integer> candidatos = new ArrayList<>(asignados.get(r));
            if (candidatos.size() < minimoBiblioteca) {
                throw new IllegalStateException("El recreo " + r + " tiene " + candidatos.size()
                        + " guardias y el mínimo de biblioteca es " + minimoBiblioteca);
            }
            candidatos.sort(Comparator.comparingInt((Integer p) -> acumuladas[p]).thenComparingInt(p -> p));
            for (int i = 0; i < minimoBiblioteca; i++) {
                int p = candidatos.get(i);
                acumuladas[p]++;
                enBiblioteca.add(clave(p, r));
            }
        }
        List<AsignacionRecreo> asignaciones = new ArrayList<>();
        for (int[] guardia : columnas.asignaciones()) {
            asignaciones.add(new AsignacionRecreo(profesores.get(guardia[0]), guardia[1],
                    enBiblioteca.contains(clave(guardia[0], guardia[1]))));
        }
        return new ResultadoRecreo(asignaciones, List.of(), List.of(), false);
    }

    private static long clave(int profesor, int columna) {
        return ((long) profesor << 32) | columna;
    }

    /** Los profesores con alguna guardia, por código (TreeMap: orden natural del código). */
    private static Map<String, Integer> conGuardias(Map<String, Integer> guardiasPorProfesor) {
        Map<String, Integer> conGuardias = new TreeMap<>();
        guardiasPorProfesor.forEach((codigo, guardias) -> {
            if (guardias != null && guardias > 0) {
                conGuardias.put(codigo, guardias);
            }
        });
        return conGuardias;
    }

    /**
     * El núcleo de las dos familias: las dos fases sobre {@code columnas} columnas. Los profesores van
     * por posición, con sus guardias en {@code guardias} y las columnas que NO tienen libres en
     * {@code noLibres}.
     */
    private static Columnas repartirPorColumnas(List<Integer> guardias, int columnas,
                                                List<Set<Integer>> noLibres, int minimo) {
        int profesores = guardias.size();
        int total = guardias.stream().mapToInt(Integer::intValue).sum();
        boolean[][] libre = new boolean[profesores][columnas];
        for (int p = 0; p < profesores; p++) {
            for (int t = 0; t < columnas; t++) {
                libre[p][t] = !noLibres.get(p).contains(t);
            }
        }

        int fuente = 0;
        int primeraColumna = 1 + profesores;
        int sumidero = primeraColumna + columnas;
        RedDeFlujo red = new RedDeFlujo(sumidero + 1);
        for (int p = 0; p < profesores; p++) {
            red.arista(fuente, 1 + p, guardias.get(p));
            for (int t = 0; t < columnas; t++) {
                if (libre[p][t]) {
                    red.arista(1 + p, primeraColumna + t, 1);
                }
            }
        }
        int[] aristaMinimo = new int[columnas];
        for (int t = 0; t < columnas; t++) {
            aristaMinimo[t] = red.arista(primeraColumna + t, sumidero, minimo);
        }

        // Fase 1: el mínimo de cada columna.
        red.flujoMaximo(fuente, sumidero);
        List<int[]> deficits = new ArrayList<>();
        for (int t = 0; t < columnas; t++) {
            int alcanzadas = red.flujo(aristaMinimo[t]);
            if (alcanzadas < minimo) {
                deficits.add(new int[] {t, alcanzadas});
            }
        }
        if (!deficits.isEmpty()) {
            return new Columnas(List.of(), deficits);
        }
        if (total == 0) {
            return new Columnas(List.of(), List.of());
        }
        return equilibrar(guardias, columnas, libre, minimo, total);
    }

    /**
     * Fase 2: todas las guardias, con cada columna en su mínimo o por encima y la suma de los
     * cuadrados mínima, por flujo de coste mínimo. Solo se llama si la fase 1 llega y hay guardias.
     */
    private static Columnas equilibrar(List<Integer> guardias, int columnas, boolean[][] libre,
                                       int minimo, int total) {
        int profesores = guardias.size();
        int fuente = 0;
        int primeraColumna = 1 + profesores;
        int sumidero = primeraColumna + columnas;
        MinCostFlow flujo = new MinCostFlow();
        try {
            int[][] arcoGuardia = new int[profesores][columnas];
            int[] libres = new int[columnas];
            for (int p = 0; p < profesores; p++) {
                flujo.addArcWithCapacityAndUnitCost(fuente, 1 + p, guardias.get(p), 0);
                for (int t = 0; t < columnas; t++) {
                    if (libre[p][t]) {
                        arcoGuardia[p][t] = flujo.addArcWithCapacityAndUnitCost(1 + p, primeraColumna + t, 1, 0);
                        libres[t]++;
                    } else {
                        arcoGuardia[p][t] = -1;
                    }
                }
            }
            for (int t = 0; t < columnas; t++) {
                for (int k = minimo + 1; k <= libres[t]; k++) {
                    flujo.addArcWithCapacityAndUnitCost(primeraColumna + t, sumidero, 1, 2L * k - 1);
                }
            }
            flujo.setNodeSupply(fuente, total);
            for (int t = 0; t < columnas; t++) {
                flujo.setNodeSupply(primeraColumna + t, -minimo);
            }
            flujo.setNodeSupply(sumidero, -(total - (long) minimo * columnas));

            MinCostFlowBase.Status estado = flujo.solve();
            if (estado != MinCostFlowBase.Status.OPTIMAL) {
                throw new IllegalStateException("El reparto de guardias no encontró el reparto equilibrado ("
                        + estado + ") después de llegar al mínimo de cada columna");
            }
            List<int[]> asignaciones = new ArrayList<>();
            for (int p = 0; p < profesores; p++) {
                for (int t = 0; t < columnas; t++) {
                    if (arcoGuardia[p][t] >= 0 && flujo.getFlow(arcoGuardia[p][t]) == 1) {
                        asignaciones.add(new int[] {p, t});
                    }
                }
            }
            if (asignaciones.size() != total) {
                throw new IllegalStateException("El reparto de guardias colocó " + asignaciones.size()
                        + " de " + total + " sin que la pre-validación diera GUARDIAS_SIN_HUECO");
            }
            return new Columnas(asignaciones, List.of());
        } finally {
            flujo.delete();
        }
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
