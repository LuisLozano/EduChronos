package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SesionBloqueada;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * D-indisp-solo-tramo-de-inicio (S166, fase 3): el modelo aplica DURA y BLANDA solo al tramo
 * de INICIO de cada sesión, y un bloque de {@code duracionTramos > 1} puede cubrir un tramo
 * vetado sin coste. Estos casos piden que el SOLVER mire los tramos OCUPADOS, que es lo que ya
 * hace {@link VerificadorSolucion} desde S165.
 *
 * <p>Problemas construidos en Java (patrón de {@code VerificadorSolucionIndisponibilidadTest})
 * y NO como fixtures JSON: así el conjunto de los 44 fixtures de la huella canónica no cambia.
 * Los tramos ocupados se calculan con {@link VerificadorSolucion#tramosOcupados}, no con nada
 * del modelo (decisión G).
 *
 * <p><b>Desempates.</b> Mirando solo el inicio, el término BLANDA del modelo nunca es MAYOR
 * que el real, así que en un caso «evitable» la colocación mala cuesta hoy 0, igual que la
 * buena: el test pasaría o no según por dónde busque el solver. Los casos «evitables» (T1, T4)
 * añaden por eso un coste que HOY hace estrictamente mejor la colocación mala y que, arreglado
 * el modelo, deja estrictamente mejor la buena. Cada caso lo cuenta en su comentario.
 */
class SolverHorarioIndisponibilidadBloqueTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("SG", Set.of(GRUPO));
    private static final Aula AULA = new Aula("AU", "Aula");
    private static final Profesor P1 = new Profesor("P1", "Uno");
    private static final Profesor P2 = new Profesor("P2", "Dos");

    /** Lunes, seis tramos: 1-2-3 | recreo | 4-5-6 (la frontera la fija el modelo, D22). */
    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);
    private static final Tramo L3 = new Tramo("L3", 1, 3);
    private static final Tramo L4 = new Tramo("L4", 1, 4);
    private static final Tramo L5 = new Tramo("L5", 1, 5);
    private static final Tramo L6 = new Tramo("L6", 1, 6);

    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    // ================================================================== DURA

    /**
     * T1. Bloque B (d=2, P1) en {L1, L2, L3}: arranca en L1 (ocupa L1-L2) o en L2 (L2-L3).
     * DURA de P1 en L3, el INTERIOR de la colocación L2. Hay hueco: L1-L2 no toca L3.
     *
     * <p>Desempate: BLANDA de P1 en L1. Hoy L2 cuesta 0 (la DURA no ve L3, la BLANDA no ve
     * L1) y L1 cuesta 1, así que el optimizador elige L2 y cubre L3. Arreglado, L2 queda
     * prohibido y solo cabe L1.
     */
    @Test
    void t1_duraEnTramoInteriorConHueco_elBloqueNoCubreElTramoVetado() {
        Actividad b = actividad("B", 2, 1, P1);
        ProblemaHorario p = problema(List.of(L1, L2, L3), List.of(b),
                List.of(dura(P1, L3), blanda(P1, L1)), List.of());

        SolucionHorario sol = solver.resolverOptimizando(p);

        assertThat(ocupados(p, sol, b)).as("tramos que ocupa B").doesNotContain(L3);
        assertThat(violacionesDura(p, sol)).isEmpty();
    }

    /**
     * T1b. Bloque B (d=2, P1) con solo {L1, L2}: su ÚNICA colocación es L1-L2, y P1 tiene DURA
     * en L2, el interior. No hay horario que la respete: INFEASIBLE. Hoy el solver lo coloca.
     */
    @Test
    void t1b_duraEnTramoInteriorDeLaUnicaColocacion_esInfactible() {
        Actividad b = actividad("B", 2, 1, P1);
        ProblemaHorario p = problema(List.of(L1, L2), List.of(b),
                List.of(dura(P1, L2)), List.of());

        assertThatThrownBy(() -> solver.resolver(p))
                .isInstanceOf(HorarioInfactibleException.class)
                .hasMessageContaining("INFEASIBLE");
    }

    // ================================================================ BLANDA

    /**
     * T4. BLANDA en el interior de un bloque, EVITABLE. Lunes de seis tramos. B (d=2) en
     * co-docencia de P1 y P2; W (d=1, P1) pinada en L3. B cabe en L1-L2, L4-L5 o L5-L6
     * (L2-L3 choca con W). P1 y P2 tienen BLANDA en L5: dos restricciones, así que cubrir L5
     * cuesta 2.
     *
     * <p>Desempate, por la ventana de P1, que el modelo calcula sobre INICIOS: B en L1 deja a
     * P1 con inicios {1, 3} y una ventana; en L4, {3, 4} y ninguna. Hoy: L1 = 1, L4 = 0 (la
     * BLANDA no ve L5 porque L5 es interior), L5-L6 = 1 + 2 = 3 → elige L4 y cubre L5.
     * Arreglado: L1 = 1, L4 = 0 + 2 = 2, L5-L6 = 3 → elige L1 y la BLANDA es 0. Si algún día
     * las ventanas también contaran la ocupación, L1 bajaría a 0 y seguiría ganando.
     */
    @Test
    void t4_blandaEnTramoInteriorEvitable_elSolverLaEvitaYLaPenalizacionEsCero() {
        Actividad b = actividad("B", 2, 1, P1, P2);
        Actividad w = actividad("W", 1, 1, P1);
        ProblemaHorario p = problema(List.of(L1, L2, L3, L4, L5, L6), List.of(b, w),
                List.of(blanda(P1, L5), blanda(P2, L5)),
                List.of(new SesionBloqueada(new ActividadInstancia(w, 1), L3, Map.of())));

        SolucionHorario sol = solver.resolverOptimizando(p);

        assertThat(ocupados(p, sol, b)).as("tramos que ocupa B").doesNotContain(L5);
        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, sol)).isZero();
    }

    /**
     * T4b. BLANDA en el interior, INEVITABLE. B (d=2, P1) con solo {L1, L2}: única colocación
     * L1-L2, BLANDA de P1 en L2. La penalización del MODELO debe ser 1; hoy es 0.
     *
     * <p>El {@code objetivo} ES el término BLANDA: P1 tiene una sola sesión, y el modelo no
     * crea términos de ventanas (pide ≥ 2 sesiones) ni de consecutivas (pide > 3).
     */
    @Test
    void t4b_blandaEnTramoInteriorInevitable_penalizaUno() {
        ProblemaHorario p = problemaT4b();

        ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);

        assertThat(r.esOptimo()).isTrue();
        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, r.solucion()))
                .as("recuento real (verificador)").isEqualTo(1);
        assertThat(r.objetivo()).as("término BLANDA del modelo").isEqualTo(1.0);
    }

    /**
     * T4c. BLANDA en DOS tramos cubiertos por el mismo bloque, inevitable. B (d=3, P1) con solo
     * {L1, L2, L3}: única colocación L1-L3, BLANDA de P1 en L2 y en L3 (los dos interiores).
     * Penalización del modelo 2; hoy 0. Mismo argumento que T4b para leerla en el objetivo.
     */
    @Test
    void t4c_blandaEnDosTramosDelMismoBloqueInevitable_penalizaDos() {
        ProblemaHorario p = problemaT4c();

        ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);

        assertThat(r.esOptimo()).isTrue();
        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, r.solucion()))
                .as("recuento real (verificador)").isEqualTo(2);
        assertThat(r.objetivo()).as("término BLANDA del modelo").isEqualTo(2.0);
    }

    /**
     * CONC. Sobre las soluciones de T4b y T4c, el objetivo del modelo es igual a lo que el
     * verificador cuenta para los mismos términos. Ventanas y consecutivas se exigen a 0 en el
     * verificador, de modo que la igualdad compara el término BLANDA del modelo con el recuento
     * BLANDA del verificador. Hoy divergen: 0 frente a 1, y 0 frente a 2.
     */
    @Test
    void conc_terminoBlandaDelModeloIgualAlRecuentoDelVerificador() {
        for (ProblemaHorario p : List.of(problemaT4b(), problemaT4c())) {
            ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);
            SolucionHorario sol = r.solucion();

            int ventanas = verificador.contarVentanasProfesor(p, sol).values().stream()
                    .mapToInt(Integer::intValue).sum();
            assertThat(ventanas).isZero();
            assertThat(verificador.contarPenalizacionConsecutivasProfesor(p, sol)).isZero();
            assertThat(r.objetivo())
                    .as("objetivo del modelo frente al recuento BLANDA del verificador")
                    .isEqualTo((double) verificador.contarPenalizacionIndisponibilidadBlanda(p, sol));
        }
    }

    // ============================================================= fixtures

    private static ProblemaHorario problemaT4b() {
        return problema(List.of(L1, L2), List.of(actividad("B", 2, 1, P1)),
                List.of(blanda(P1, L2)), List.of());
    }

    private static ProblemaHorario problemaT4c() {
        return problema(List.of(L1, L2, L3), List.of(actividad("B", 3, 1, P1)),
                List.of(blanda(P1, L2), blanda(P1, L3)), List.of());
    }

    private static Actividad actividad(String cod, int duracion, int repeticiones,
                                       Profesor... profesores) {
        Plaza plaza = new Plaza(cod + "-P1", ASIG, Set.of(profesores),
                Optional.of(AULA), Set.of(), Set.of(SG));
        return new Actividad(cod, Optional.of(ASIG), repeticiones, duracion,
                PatronTemporal.NEUTRA, List.of(plaza), false);
    }

    private static RestriccionHoraria dura(Profesor p, Tramo t) {
        return new RestriccionHoraria(p, t, TipoRestriccion.DURA, 1, Optional.empty());
    }

    private static RestriccionHoraria blanda(Profesor p, Tramo t) {
        return new RestriccionHoraria(p, t, TipoRestriccion.BLANDA, 1, Optional.empty());
    }

    private static ProblemaHorario problema(List<Tramo> tramos, List<Actividad> actividades,
                                            List<RestriccionHoraria> restricciones,
                                            List<SesionBloqueada> bloqueos) {
        return new ProblemaHorario(tramos, List.of(AULA), List.of(ASIG), List.of(P1, P2),
                List.of(GRUPO), List.of(SG), actividades, restricciones, bloqueos, List.of());
    }

    /** Tramos que ocupa la primera instancia de {@code actividad}, por el verificador. */
    private static List<Tramo> ocupados(ProblemaHorario p, SolucionHorario sol,
                                        Actividad actividad) {
        ActividadInstancia inst = Expansion.todas(p).stream()
                .filter(i -> i.actividad().equals(actividad))
                .findFirst().orElseThrow();
        Tramo inicio = sol.tramoDeInstancia(inst).orElseThrow();
        return VerificadorSolucion.tramosOcupados(inicio, actividad.duracionTramos(), p)
                .orElseThrow();
    }

    private List<Violacion> violacionesDura(ProblemaHorario p, SolucionHorario sol) {
        return verificador.verificar(p, sol).violaciones().stream()
                .filter(v -> v.regla() == ReglaDura.INDISPONIBILIDAD_PROFESOR)
                .toList();
    }
}
