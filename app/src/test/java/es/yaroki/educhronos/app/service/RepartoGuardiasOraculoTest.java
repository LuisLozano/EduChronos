package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Asignatura;
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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * El reparto contra un oráculo exhaustivo (S220, C-reparto-equilibrado, condición 3 de
 * O-guardias-especiales). Casos pequeños al azar con semillas fijas: de 2 a 4 profesores, de 3 a 5
 * columnas, disponibilidad al azar, mínimos de 0 a 2 y cada profesor con al menos tantas columnas
 * libres como guardias. El oráculo enumera TODAS las asignaciones válidas —cada profesor con
 * exactamente sus guardias, como mucho una por columna y solo en las libres, y cada columna en su
 * mínimo o por encima— y se queda con la mínima suma de los cuadrados de las guardias por columna.
 *
 * <p>El oráculo es código de test, separado del reparto: ni flujo ni red, solo combinaciones.
 */
class RepartoGuardiasOraculoTest {

    private static final Asignatura MAT = new Asignatura("Mat", "Matemáticas");

    /** Un caso: guardias y columnas libres por profesor, columnas y mínimo. */
    private record Caso(List<Integer> guardias, List<Set<Integer>> libres, int columnas, int minimo) { }

    /** La mínima suma de cuadrados entre las asignaciones válidas, o -1 si no hay ninguna. */
    private static long oraculo(Caso caso) {
        long[] mejor = {-1};
        enumerar(caso, 0, new int[caso.columnas()], mejor);
        return mejor[0];
    }

    private static void enumerar(Caso caso, int profesor, int[] porColumna, long[] mejor) {
        if (profesor == caso.guardias().size()) {
            long suma = 0;
            for (int x : porColumna) {
                if (x < caso.minimo()) {
                    return;
                }
                suma += (long) x * x;
            }
            if (mejor[0] < 0 || suma < mejor[0]) {
                mejor[0] = suma;
            }
            return;
        }
        List<Integer> libres = new ArrayList<>(caso.libres().get(profesor));
        libres.sort(null);
        subconjuntos(libres, caso.guardias().get(profesor), 0, new ArrayList<>(), elegidas -> {
            for (int t : elegidas) {
                porColumna[t]++;
            }
            enumerar(caso, profesor + 1, porColumna, mejor);
            for (int t : elegidas) {
                porColumna[t]--;
            }
        });
    }

    private static void subconjuntos(List<Integer> de, int k, int desde, List<Integer> actual,
                                     java.util.function.Consumer<List<Integer>> visita) {
        if (actual.size() == k) {
            visita.accept(actual);
            return;
        }
        for (int i = desde; i < de.size(); i++) {
            actual.add(de.get(i));
            subconjuntos(de, k, i + 1, actual, visita);
            actual.remove(actual.size() - 1);
        }
    }

    private static Caso casoAlAzar(Random azar, boolean todasLibres, int maxColumnas) {
        int profesores = 2 + azar.nextInt(3);
        int columnas = 3 + azar.nextInt(maxColumnas - 2);
        int minimo = azar.nextInt(3);
        List<Integer> guardias = new ArrayList<>();
        List<Set<Integer>> libres = new ArrayList<>();
        for (int p = 0; p < profesores; p++) {
            Set<Integer> suyas = new HashSet<>();
            for (int t = 0; t < columnas; t++) {
                if (todasLibres || azar.nextInt(10) < 7) {
                    suyas.add(t);
                }
            }
            libres.add(suyas);
            guardias.add(azar.nextInt(suyas.size() + 1));
        }
        return new Caso(guardias, libres, columnas, minimo);
    }

    private static String codigo(int p) {
        return "P" + p;
    }

    // ------------------------------------------------------------------ ordinarias

    /** El caso como problema: tramos del lunes y un «No puede» en cada columna no libre. */
    private static RepartoGuardias.Resultado repartirOrdinarias(Caso caso) {
        List<Tramo> tramos = new ArrayList<>();
        for (int t = 0; t < caso.columnas(); t++) {
            tramos.add(new Tramo("L" + (t + 1), 1, t + 1));
        }
        List<Profesor> profesores = new ArrayList<>();
        List<RestriccionHoraria> duras = new ArrayList<>();
        Map<String, Integer> guardias = new HashMap<>();
        for (int p = 0; p < caso.guardias().size(); p++) {
            Profesor profesor = new Profesor(codigo(p), "Profesor " + p);
            profesores.add(profesor);
            guardias.put(codigo(p), caso.guardias().get(p));
            for (int t = 0; t < caso.columnas(); t++) {
                if (!caso.libres().get(p).contains(t)) {
                    duras.add(new RestriccionHoraria(profesor, tramos.get(t), TipoRestriccion.DURA, 1, Optional.empty()));
                }
            }
        }
        ProblemaHorario problema = new ProblemaHorario(tramos, List.of(), List.of(MAT), profesores, List.of(),
                List.of(), List.of(), duras, List.of(), List.of());
        return RepartoGuardias.repartir(problema, new SolucionHorario(Map.of()), guardias, caso.minimo());
    }

    @Test
    void ordinarias_doscientosCasosContraElOraculo() {
        Random azar = new Random(220);
        int conReparto = 0;
        int sinReparto = 0;
        for (int i = 0; i < 240; i++) {
            Caso caso = casoAlAzar(azar, false, 5);
            long optimo = oraculo(caso);
            RepartoGuardias.Resultado r = repartirOrdinarias(caso);

            assertThat(r.fallo()).as("caso %d %s: falla si y solo si no hay asignación válida", i, caso)
                    .isEqualTo(optimo < 0);
            if (optimo < 0) {
                sinReparto++;
                continue;
            }
            conReparto++;
            int[] porColumna = new int[caso.columnas()];
            Map<String, Integer> porProfesor = new TreeMap<>();
            Set<String> vistas = new HashSet<>();
            for (RepartoGuardias.Asignacion a : r.asignaciones()) {
                int p = Integer.parseInt(a.profesorCodigo().substring(1));
                assertThat(caso.libres().get(p)).as("caso %d: %s solo en sus libres", i, a).contains(a.tramo());
                assertThat(vistas.add(a.profesorCodigo() + "@" + a.tramo())).as("caso %d: sin repetir %s", i, a).isTrue();
                porColumna[a.tramo()]++;
                porProfesor.merge(a.profesorCodigo(), 1, Integer::sum);
            }
            for (int p = 0; p < caso.guardias().size(); p++) {
                assertThat(porProfesor.getOrDefault(codigo(p), 0)).as("caso %d: guardias de P%d", i, p)
                        .isEqualTo(caso.guardias().get(p));
            }
            long suma = 0;
            for (int x : porColumna) {
                assertThat(x).as("caso %d: cada columna en su mínimo", i).isGreaterThanOrEqualTo(caso.minimo());
                suma += (long) x * x;
            }
            assertThat(suma).as("caso %d %s: la suma de cuadrados es la mínima", i, caso).isEqualTo(optimo);
        }
        assertThat(conReparto).as("casos con reparto").isGreaterThanOrEqualTo(100);
        assertThat(sinReparto).as("casos sin reparto").isGreaterThanOrEqualTo(20);
    }

    @Test
    void ordinarias_laMismaEntradaDosVecesDaLaMismaLista() {
        Random azar = new Random(2201);
        for (int i = 0; i < 50; i++) {
            Caso caso = casoAlAzar(azar, false, 5);
            assertThat(repartirOrdinarias(caso)).as("caso %d", i).isEqualTo(repartirOrdinarias(caso));
        }
    }

    // ------------------------------------------------------------------ recreo

    private static RepartoGuardias.ResultadoRecreo repartirRecreo(Caso caso, int biblioteca) {
        Map<String, Integer> guardias = new HashMap<>();
        for (int p = 0; p < caso.guardias().size(); p++) {
            guardias.put(codigo(p), caso.guardias().get(p));
        }
        return RepartoGuardias.repartirRecreo(caso.columnas(), guardias, caso.minimo(), biblioteca);
    }

    @Test
    void recreo_cientoVeinteCasosContraElOraculo() {
        Random azar = new Random(2202);
        int conReparto = 0;
        int sinReparto = 0;
        for (int i = 0; i < 120; i++) {
            Caso caso = casoAlAzar(azar, true, 5);
            int biblioteca = azar.nextInt(caso.minimo() + 1);
            long optimo = oraculo(caso);
            RepartoGuardias.ResultadoRecreo r = repartirRecreo(caso, biblioteca);

            assertThat(r.fallo()).as("caso %d %s: falla si y solo si no hay asignación válida", i, caso)
                    .isEqualTo(optimo < 0);
            if (optimo < 0) {
                sinReparto++;
                assertThat(r.deficits()).as("caso %d: el fallo es de mínimo", i).isNotEmpty();
                continue;
            }
            conReparto++;
            int[] porRecreo = new int[caso.columnas()];
            int[] enBiblioteca = new int[caso.columnas()];
            Map<String, Integer> porProfesor = new TreeMap<>();
            Set<String> vistas = new HashSet<>();
            for (RepartoGuardias.AsignacionRecreo a : r.asignaciones()) {
                assertThat(vistas.add(a.profesorCodigo() + "@" + a.recreo())).as("caso %d: una por recreo", i).isTrue();
                porRecreo[a.recreo()]++;
                if (a.biblioteca()) {
                    enBiblioteca[a.recreo()]++;
                }
                porProfesor.merge(a.profesorCodigo(), 1, Integer::sum);
            }
            for (int p = 0; p < caso.guardias().size(); p++) {
                assertThat(porProfesor.getOrDefault(codigo(p), 0)).as("caso %d: guardias de P%d", i, p)
                        .isEqualTo(caso.guardias().get(p));
            }
            long suma = 0;
            for (int r2 = 0; r2 < caso.columnas(); r2++) {
                assertThat(porRecreo[r2]).as("caso %d: el mínimo del recreo %d", i, r2)
                        .isGreaterThanOrEqualTo(caso.minimo());
                assertThat(enBiblioteca[r2]).as("caso %d: exactamente el mínimo de biblioteca en %d", i, r2)
                        .isEqualTo(biblioteca);
                suma += (long) porRecreo[r2] * porRecreo[r2];
            }
            assertThat(suma).as("caso %d %s: la suma de cuadrados es la mínima", i, caso).isEqualTo(optimo);
        }
        assertThat(conReparto).as("casos con reparto").isGreaterThanOrEqualTo(60);
        assertThat(sinReparto).as("casos sin reparto").isGreaterThanOrEqualTo(10);
    }

    @Test
    void recreo_laMismaEntradaDosVecesDaLaMismaLista() {
        Random azar = new Random(2203);
        for (int i = 0; i < 50; i++) {
            Caso caso = casoAlAzar(azar, true, 5);
            int biblioteca = azar.nextInt(caso.minimo() + 1);
            assertThat(repartirRecreo(caso, biblioteca)).as("caso %d", i).isEqualTo(repartirRecreo(caso, biblioteca));
        }
    }
}
