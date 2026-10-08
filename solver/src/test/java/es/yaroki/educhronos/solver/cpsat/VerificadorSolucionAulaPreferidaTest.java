package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contador del término «clase fuera de su aula preferida» en el {@link VerificadorSolucion}
 * (S208, C-preferencias-aulas, C4 y C5), sobre una solución construida A MANO: no pasa por
 * {@code ModeloCpSat}, así que el recuento es independiente del modelo.
 */
class VerificadorSolucionAulaPreferidaTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura");
    private static final GrupoAdministrativo G =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("G-1", Set.of(G));
    private static final Profesor PR = new Profesor("PR", "Profe");
    private static final Aula PREF = new Aula("PREF", "Preferida");
    private static final Aula PREF2 = new Aula("PREF2", "Otra preferida");
    private static final Aula OTRA = new Aula("OTRA", "No preferida");
    private static final Aula OTRA2 = new Aula("OTRA2", "No preferida 2");

    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);
    private static final Tramo L3 = new Tramo("L3", 1, 3);
    private static final Tramo M1 = new Tramo("M1", 2, 1);
    private static final Tramo M2 = new Tramo("M2", 2, 2);

    private final VerificadorSolucion verificador = new VerificadorSolucion();

    // Plazas: el código es el de la actividad + "-P1"/"-P2".
    private static final Plaza UNO = plaza("UNO-P1", PREF, OTRA);          // pref {PREF}
    private static final Plaza DOS = plaza("DOS-P1", PREF, OTRA);          // pref {PREF}, d = 2
    private static final Plaza MULTI1 = plaza("MULTI-P1", PREF, OTRA);     // pref {PREF}
    private static final Plaza MULTI2 = plaza("MULTI-P2", PREF2, OTRA2);   // pref {PREF2}
    private static final Plaza BIEN = plaza("BIEN-P1", PREF, OTRA);        // pref {PREF, OTRA}
    private static final Plaza SINPREF = plaza("SINPREF-P1", PREF, OTRA);  // sin entrada
    private static final Plaza VACIA = plaza("VACIA-P1", PREF, OTRA);      // conjunto vacío
    private static final Plaza FIJA = new Plaza("FIJA-P1", ASIG, Set.of(PR), Optional.of(PREF),
            Set.of(), Set.of(SG));                                          // pref {PREF}
    private static final Plaza SUELTA = plaza("SUELTA-P1", PREF, OTRA);    // pref {PREF}, sin colocar

    private static final Actividad A_UNO = actividad("UNO", 2, 1, UNO);
    private static final Actividad A_DOS = actividad("DOS", 1, 2, DOS);
    private static final Actividad A_MULTI = actividad("MULTI", 1, 1, MULTI1, MULTI2);
    private static final Actividad A_BIEN = new Actividad("BIEN", Optional.of(ASIG), 1, 1,
            PatronTemporal.NEUTRA, List.of(BIEN), false);
    private static final Actividad A_SINPREF = actividad("SINPREF", 1, 1, SINPREF);
    private static final Actividad A_VACIA = actividad("VACIA", 1, 1, VACIA);
    private static final Actividad A_FIJA = actividad("FIJA", 1, 1, FIJA);
    private static final Actividad A_SUELTA = actividad("SUELTA", 1, 1, SUELTA);

    private static final ProblemaHorario PROBLEMA = new ProblemaHorario(
            List.of(L1, L2, L3, M1, M2), List.of(PREF, PREF2, OTRA, OTRA2), List.of(ASIG),
            List.of(PR), List.of(G), List.of(SG),
            List.of(A_UNO, A_DOS, A_MULTI, A_BIEN, A_SINPREF, A_VACIA, A_FIJA, A_SUELTA),
            List.of(), List.of(), List.of(), Map.of(), Map.of(),
            Map.of("UNO-P1", Set.of("PREF"), "DOS-P1", Set.of("PREF"),
                    "MULTI-P1", Set.of("PREF"), "MULTI-P2", Set.of("PREF2"),
                    "BIEN-P1", Set.of("PREF", "OTRA"), "VACIA-P1", Set.of(),
                    "FIJA-P1", Set.of("PREF"), "SUELTA-P1", Set.of("PREF")));

    /**
     * Solución a mano (el verificador no mira solapes para contar):
     * <ul>
     *   <li>UNO#1 en OTRA (1) y UNO#2 en PREF (0): las instancias cuentan por separado.</li>
     *   <li>DOS#1, de duración 2, en OTRA: 2.</li>
     *   <li>MULTI#1: P1 en OTRA (1) y P2 en OTRA2 (1): las plazas cuentan por separado.</li>
     *   <li>BIEN#1 en OTRA, que es una de sus dos preferidas: 0.</li>
     *   <li>SINPREF#1 y VACIA#1 en OTRA: sin preferidas efectivas, 0.</li>
     *   <li>FIJA#1 en su aula fija, que es su preferida: 0.</li>
     *   <li>SUELTA#1 sin colocar: no cuenta.</li>
     * </ul>
     * Total 1 + 2 + 2 = 5.
     */
    private static SolucionHorario solucion() {
        Map<ActividadInstancia, Tramo> tramos = new HashMap<>();
        Map<ActividadInstancia, Map<Plaza, Aula>> aulas = new HashMap<>();
        colocar(tramos, aulas, A_UNO, 1, L1, Map.of(UNO, OTRA));
        colocar(tramos, aulas, A_UNO, 2, M1, Map.of(UNO, PREF));
        colocar(tramos, aulas, A_DOS, 1, L1, Map.of(DOS, OTRA));
        colocar(tramos, aulas, A_MULTI, 1, L3, Map.of(MULTI1, OTRA, MULTI2, OTRA2));
        colocar(tramos, aulas, A_BIEN, 1, M2, Map.of(BIEN, OTRA));
        colocar(tramos, aulas, A_SINPREF, 1, L2, Map.of(SINPREF, OTRA));
        colocar(tramos, aulas, A_VACIA, 1, M2, Map.of(VACIA, OTRA));
        colocar(tramos, aulas, A_FIJA, 1, L3, Map.of());
        return new SolucionHorario(tramos, aulas);
    }

    /** (e) El verificador cuenta el término sobre la solución hecha a mano. */
    @Test
    void e_cuentaPorInstanciaPlazaYTramo_sobreUnaSolucionHechaAMano() {
        assertThat(verificador.contarPenalizacionAulaNoPreferida(PROBLEMA, solucion())).isEqualTo(5);
    }

    /** (d) Las plazas sin preferidas efectivas no cuentan en ninguna de sus aulas. */
    @Test
    void d_plazasSinPreferidas_noCuentanEnNingunaAula() {
        for (Aula aula : List.of(PREF, OTRA)) {
            Map<ActividadInstancia, Tramo> tramos = new HashMap<>();
            Map<ActividadInstancia, Map<Plaza, Aula>> aulas = new HashMap<>();
            colocar(tramos, aulas, A_SINPREF, 1, L1, Map.of(SINPREF, aula));
            colocar(tramos, aulas, A_VACIA, 1, L2, Map.of(VACIA, aula));

            assertThat(verificador.contarPenalizacionAulaNoPreferida(
                    PROBLEMA, new SolucionHorario(tramos, aulas)))
                    .as("aula %s", aula.codigo()).isZero();
        }
    }

    /**
     * (f) Sin penalizaciones por celda para la regla nueva (C5): la atribución por celda mide lo
     * que se gana moviendo la sesión de tramo, y moverla no cambia su aula. La misma solución
     * cuenta 5 en el contador, así que la ausencia no es por falta de casos.
     */
    @Test
    void f_laReglaNuevaNoProducePenalizacionesPorCelda() {
        SolucionHorario sol = solucion();
        assertThat(verificador.contarPenalizacionAulaNoPreferida(PROBLEMA, sol)).isEqualTo(5);

        List<Penalizacion> todas = verificador.atribuirBlandas(PROBLEMA, sol).porCelda().values()
                .stream().flatMap(List::stream).toList();

        assertThat(todas).noneMatch(pen -> pen.regla() == ReglaBlanda.AULA_NO_PREFERIDA);
    }

    // ============================================================= fixtures

    private static Plaza plaza(String cod, Aula a, Aula b) {
        return new Plaza(cod, ASIG, Set.of(PR), Optional.empty(), Set.of(a, b), Set.of(SG));
    }

    private static Actividad actividad(String cod, int repeticiones, int duracion, Plaza... plazas) {
        return new Actividad(cod, Optional.of(ASIG), repeticiones, duracion, PatronTemporal.NEUTRA,
                List.of(plazas), false);
    }

    private static void colocar(Map<ActividadInstancia, Tramo> tramos,
                                Map<ActividadInstancia, Map<Plaza, Aula>> aulas,
                                Actividad actividad, int indice, Tramo tramo,
                                Map<Plaza, Aula> aulasDeLaInstancia) {
        ActividadInstancia inst = new ActividadInstancia(actividad, indice);
        tramos.put(inst, tramo);
        if (!aulasDeLaInstancia.isEmpty()) {
            aulas.put(inst, aulasDeLaInstancia);
        }
    }
}
