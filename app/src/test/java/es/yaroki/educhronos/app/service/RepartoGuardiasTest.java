package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Reparto de las guardias ordinarias (S213, C-reparto-guardias, K5 con la enmienda 1). Puros: el
 * problema y la solución en memoria, sin solver. Los tramos son los del lunes, L1..Ln, y su
 * posición en el problema es {@code n − 1}.
 */
class RepartoGuardiasTest {

    private static final Asignatura MAT = new Asignatura("Mat", "Matemáticas");
    private static final Profesor A = new Profesor("A", "Profesor A");
    private static final Profesor B = new Profesor("B", "Profesor B");
    private static final Profesor C = new Profesor("C", "Profesor C");

    private static List<Tramo> lunes(int n) {
        List<Tramo> tramos = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            tramos.add(new Tramo("L" + i, 1, i));
        }
        return tramos;
    }

    /** Una actividad de una repetición y una plaza sin aula ni subgrupos, con esos profesores. */
    private static Actividad clase(String codigo, int duracion, Profesor... profesores) {
        Plaza plaza = new Plaza(codigo + "-P1", MAT, Set.of(profesores), Optional.empty(), Set.of(), Set.of());
        return new Actividad(codigo, Optional.of(MAT), 1, duracion, PatronTemporal.NEUTRA, List.of(plaza), false);
    }

    private static RestriccionHoraria noPuede(Profesor profesor, Tramo tramo) {
        return new RestriccionHoraria(profesor, tramo, TipoRestriccion.DURA, 1, Optional.empty());
    }

    private static ProblemaHorario problema(List<Tramo> tramos, List<Actividad> actividades,
                                            List<RestriccionHoraria> duras) {
        return new ProblemaHorario(tramos, List.of(), List.of(MAT), List.of(A, B, C), List.of(), List.of(),
                actividades, duras, List.of(), List.of());
    }

    /** Cada actividad colocada en el tramo que se da, en el mismo orden. */
    private static SolucionHorario colocadas(List<Actividad> actividades, Tramo... inicios) {
        Map<ActividadInstancia, Tramo> asignaciones = new HashMap<>();
        for (int i = 0; i < actividades.size(); i++) {
            asignaciones.put(new ActividadInstancia(actividades.get(i), 1), inicios[i]);
        }
        return new SolucionHorario(asignaciones);
    }

    private static Map<String, Integer> guardiasPorProfesor(RepartoGuardias.Resultado r) {
        return r.asignaciones().stream().collect(Collectors.groupingBy(
                RepartoGuardias.Asignacion::profesorCodigo, Collectors.summingInt(a -> 1)));
    }

    private static Map<Integer, Integer> guardiasPorTramo(RepartoGuardias.Resultado r) {
        return r.asignaciones().stream().collect(Collectors.groupingBy(
                RepartoGuardias.Asignacion::tramo, Collectors.summingInt(a -> 1)));
    }

    private static Set<Integer> tramosDe(RepartoGuardias.Resultado r, String profesor) {
        return r.asignaciones().stream().filter(a -> a.profesorCodigo().equals(profesor))
                .map(RepartoGuardias.Asignacion::tramo).collect(Collectors.toSet());
    }

    @Test
    void cadaProfesorRecibeExactamenteSusGuardias() {
        ProblemaHorario p = problema(lunes(5), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()),
                Map.of("A", 3, "B", 2, "C", 0), 1);

        assertThat(r.fallo()).isFalse();
        assertThat(guardiasPorProfesor(r)).isEqualTo(Map.of("A", 3, "B", 2));
    }

    @Test
    void cadaTramoLlegaAlMinimo() {
        ProblemaHorario p = problema(lunes(3), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()),
                Map.of("A", 2, "B", 2, "C", 2), 2);

        assertThat(r.fallo()).isFalse();
        assertThat(guardiasPorTramo(r)).isEqualTo(Map.of(0, 2, 1, 2, 2, 2));
    }

    @Test
    void conMinimoCero_seColocanTodasYNoEsFallo() {
        ProblemaHorario p = problema(lunes(3), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()), Map.of("A", 2), 0);

        assertThat(r.fallo()).isFalse();
        assertThat(r.asignaciones()).hasSize(2);
    }

    @Test
    void sinGuardiasYConMinimoCero_listaVaciaYNoEsFallo() {
        ProblemaHorario p = problema(lunes(3), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()), Map.of("A", 0), 0);

        assertThat(r.fallo()).isFalse();
        assertThat(r.asignaciones()).isEmpty();
        assertThat(r.deficits()).isEmpty();
    }

    @Test
    void unaActividadDeDosTramosOcupaLosDos() {
        List<Tramo> tramos = lunes(4);
        Actividad bloque = clase("BLQ", 2, A);
        ProblemaHorario p = problema(tramos, List.of(bloque), List.of());

        // A da L2 y L3; con dos guardias solo le quedan L1 y L4.
        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of(bloque), tramos.get(1)),
                Map.of("A", 2), 0);

        assertThat(tramosDe(r, "A")).containsExactlyInAnyOrder(0, 3);
    }

    @Test
    void unNoPuedeNoRecibeGuardia() {
        List<Tramo> tramos = lunes(3);
        ProblemaHorario p = problema(tramos, List.of(), List.of(noPuede(A, tramos.get(1))));

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()), Map.of("A", 2), 0);

        assertThat(tramosDe(r, "A")).containsExactlyInAnyOrder(0, 2);
    }

    /**
     * El caso del guion (F3.4 b), sin solver: cinco tramos, mínimo 2; A y B dan juntos una clase en
     * L2 y C no da clase; guardias 4, 4 y 5. La pre-validación pasa (13 ≥ 10; 1 + 4 ≤ 5), y en L2
     * solo queda C: la fase 1 falla en L2, y solo en L2, con 1 de 2.
     */
    @Test
    void falloDeFase1_nombraExactamenteLosTramosConDeficitYSusCifras() {
        List<Tramo> tramos = lunes(5);
        Actividad juntos = clase("JNT", 1, A, B);
        ProblemaHorario p = problema(tramos, List.of(juntos), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of(juntos), tramos.get(1)),
                Map.of("A", 4, "B", 4, "C", 5), 2);

        assertThat(r.fallo()).isTrue();
        assertThat(r.asignaciones()).isEmpty();
        assertThat(r.deficits())
                .extracting(d -> d.tramo().codigo(), RepartoGuardias.Deficit::alcanzadas,
                        RepartoGuardias.Deficit::minimo)
                .containsExactly(tuple("L2", 1, 2));
    }

    @Test
    void elMensajeDelFallo_esElTextoAprobadoConLaListaDeTramos() {
        List<Tramo> tramos = lunes(5);
        GuardiasSinRepartoException e = new GuardiasSinRepartoException(List.of(
                new RepartoGuardias.Deficit(tramos.get(1), 1, 2),
                new RepartoGuardias.Deficit(tramos.get(3), 0, 2)));

        assertThat(e.getMessage()).isEqualTo("No se pueden repartir las guardias ordinarias: en estas horas no"
                + " se llega al mínimo de 2 profesores de guardia: tramo L2 (día 1, tramo 2): 1 de 2;"
                + " tramo L4 (día 1, tramo 4): 0 de 2. Los profesores libres en esas horas no tienen guardias"
                + " suficientes. Baja el mínimo, da más guardias a esos profesores o deja libres a más"
                + " profesores en esas horas.");
        assertThat(e.resumen()).isEqualTo("L2:1/2,L4:0/2");
    }

    /**
     * Mínimo 1 en dos tramos y cuatro guardias: la fase 1 coloca dos, una por tramo, y la fase 2 las
     * otras dos sin quitar ninguna de la fase 1.
     */
    @Test
    void laFase2ColocaTodasSinBajarNingunTramoDelMinimo() {
        ProblemaHorario p = problema(lunes(2), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()),
                Map.of("A", 2, "B", 2), 1);

        assertThat(r.asignaciones()).hasSize(4);
        assertThat(guardiasPorTramo(r)).isEqualTo(Map.of(0, 2, 1, 2));
    }

    @Test
    void laFase2ColocaLasQueSobranDelMinimo() {
        ProblemaHorario p = problema(lunes(3), List.of(), List.of());

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()),
                Map.of("A", 3, "B", 1), 1);

        assertThat(guardiasPorProfesor(r)).isEqualTo(Map.of("A", 3, "B", 1));
        assertThat(guardiasPorTramo(r).values()).allMatch(n -> n >= 1);
    }

    /**
     * Fija el orden de E3: nodos por código de profesor y por posición de tramo. Dos tramos, mínimo 1,
     * una guardia para A y otra para B: A, primero por código, se queda L1, y B, L2. El mapa de
     * entrada va al revés a propósito, para que el orden no salga de él.
     */
    @Test
    void casoPequeno_asignacionExactaPorCodigoYPosicion() {
        ProblemaHorario p = problema(lunes(2), List.of(), List.of());
        Map<String, Integer> guardias = new java.util.LinkedHashMap<>();
        guardias.put("B", 1);
        guardias.put("A", 1);

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p, colocadas(List.of()), guardias, 1);

        assertThat(r.asignaciones()).containsExactly(
                new RepartoGuardias.Asignacion("A", 0), new RepartoGuardias.Asignacion("B", 1));
    }

    @Test
    void dosEjecuciones_mismoResultado() {
        List<Tramo> tramos = lunes(5);
        Actividad juntos = clase("JNT", 2, A, B);
        ProblemaHorario p = problema(tramos, List.of(juntos), List.of(noPuede(C, tramos.get(4))));
        SolucionHorario s = colocadas(List.of(juntos), tramos.get(0));
        Map<String, Integer> guardias = Map.of("A", 2, "B", 3, "C", 3);

        assertThat(RepartoGuardias.repartir(p, s, guardias, 1))
                .isEqualTo(RepartoGuardias.repartir(p, s, guardias, 1));
    }

    /**
     * Invariante de K5 al límite de GUARDIAS_SIN_HUECO: A ocupa dos tramos (L1 y L3), tiene un «No
     * puede» (L4) y dos guardias, así que O + g = 2 + 2 = 4 = L. La pre-validación no avisa, y el
     * reparto le pone las dos en los únicos tramos que le quedan, L2 y L5.
     */
    @Test
    void invariante_sinGuardiasSinHuecoLaFase2ColocaTodas() {
        List<Tramo> tramos = lunes(5);
        Actividad primera = clase("UNO", 1, A);
        Actividad segunda = clase("DOS", 1, A);
        ProblemaHorario p = problema(tramos, List.of(primera, segunda), List.of(noPuede(A, tramos.get(3))));
        Map<String, Integer> guardias = Map.of("A", 2, "B", 5, "C", 0);
        DatosCuadre datos = new DatosCuadre(Map.of(), Map.of(), Set.of(), 1, guardias);

        assertThat(PrevalidacionService.prevalidar(p, datos))
                .noneMatch(a -> a.regla().equals(PrevalidacionService.REGLA_GUARDIAS_SIN_HUECO));

        RepartoGuardias.Resultado r = RepartoGuardias.repartir(p,
                colocadas(List.of(primera, segunda), tramos.get(0), tramos.get(2)), guardias, 1);

        assertThat(r.asignaciones()).hasSize(7);
        assertThat(tramosDe(r, "A")).containsExactlyInAnyOrder(1, 4);
    }
}
