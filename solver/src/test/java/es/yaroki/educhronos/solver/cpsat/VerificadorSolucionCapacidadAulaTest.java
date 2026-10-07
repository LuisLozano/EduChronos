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
 * Capacidad de aula en el VERIFICADOR, {@link ReglaDura#CAPACIDAD_AULA} (S207,
 * C-deduccion-aulas, C3): ninguna plaza se coloca en un aula con menos plazas que la suma de los
 * alumnos de sus subgrupos. Alumnos desconocidos cuentan 0; capacidad desconocida es sin límite;
 * igual a la capacidad cabe.
 *
 * <p>Soluciones fabricadas a mano, sin solver (molde de
 * {@link VerificadorSolucionIndisponibilidadTest}). Una sola actividad y un solo tramo: no hay
 * solapes que aporten violaciones ajenas a la regla aseverada.
 */
class VerificadorSolucionCapacidadAulaTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG1 = new Subgrupo("G-1", Set.of(GRUPO));
    private static final Subgrupo SG2 = new Subgrupo("G-2", Set.of(GRUPO));
    private static final Subgrupo SG3 = new Subgrupo("G-3", Set.of(GRUPO));
    private static final Aula A = new Aula("A", "A");
    private static final Aula B = new Aula("B", "B");
    private static final Profesor P1 = new Profesor("P1", "Uno");
    private static final Profesor P2 = new Profesor("P2", "Dos");
    private static final Tramo L1 = new Tramo("L1", 1, 1);

    private final VerificadorSolucion verificador = new VerificadorSolucion();

    private static Plaza fija(String cod, Profesor p, Aula aula, Subgrupo... sgs) {
        return new Plaza(cod, ASIG, Set.of(p), Optional.of(aula), Set.of(), Set.of(sgs));
    }

    private static Plaza candidatas(String cod, Profesor p, Set<Aula> aulas, Subgrupo... sgs) {
        return new Plaza(cod, ASIG, Set.of(p), Optional.empty(), aulas, Set.of(sgs));
    }

    /** Alumnos: {@code null} en el mapa de la llamada = subgrupo sin dato (no entra en el problema). */
    private static Map<String, Integer> alumnos(Integer g1, Integer g2, Integer g3) {
        Map<String, Integer> m = new HashMap<>();
        if (g1 != null) m.put("G-1", g1);
        if (g2 != null) m.put("G-2", g2);
        if (g3 != null) m.put("G-3", g3);
        return m;
    }

    private static ProblemaHorario problema(Map<String, Integer> capacidades,
                                            Map<String, Integer> alumnos, Plaza... plazas) {
        Actividad act = new Actividad("ACT", Optional.of(ASIG), 1, 1, PatronTemporal.NEUTRA,
                List.of(plazas), false);
        return new ProblemaHorario(
                List.of(L1), List.of(A, B), List.of(ASIG), List.of(P1, P2), List.of(GRUPO),
                List.of(SG1, SG2, SG3), List.of(act), List.of(), List.of(), List.of(),
                capacidades, alumnos);
    }

    private static ActividadInstancia inst(ProblemaHorario problema) {
        return Expansion.todas(problema).get(0);
    }

    /** La única instancia en L1, con las aulas elegidas que se pasen (plazas con candidatas). */
    private static SolucionHorario en(ProblemaHorario problema, Map<Plaza, Aula> elegidas) {
        ActividadInstancia i = inst(problema);
        return new SolucionHorario(Map.of(i, L1), elegidas.isEmpty() ? Map.of() : Map.of(i, elegidas));
    }

    private List<Violacion> capacidad(ProblemaHorario problema, SolucionHorario sol) {
        return verificador.verificar(problema, sol).violaciones().stream()
                .filter(v -> v.regla() == ReglaDura.CAPACIDAD_AULA)
                .toList();
    }

    // ================================================================ aula fija

    @Test
    void doceMasDiezEnCapacidadVeinte_unaViolacionConAulaTramoYCelda() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG2);
        ProblemaHorario problema = problema(Map.of("A", 20), alumnos(12, 10, null), p);

        List<Violacion> vs = capacidad(problema, en(problema, Map.of()));

        assertThat(vs).hasSize(1);
        Violacion v = vs.get(0);
        assertThat(v.regla()).isEqualTo(ReglaDura.CAPACIDAD_AULA);
        assertThat(v.recursoCodigo()).isEqualTo("A");
        assertThat(v.tramoCodigo()).isEqualTo("L1");
        assertThat(v.celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));
        assertThat(v.descripcion()).contains("A").contains("20").contains("22").contains("ACT-P1");
    }

    @Test
    void doceMasOchoEnCapacidadVeinte_igualALaCapacidadCabe() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG2);
        ProblemaHorario problema = problema(Map.of("A", 20), alumnos(12, 8, null), p);

        assertThat(capacidad(problema, en(problema, Map.of()))).isEmpty();
    }

    @Test
    void capacidadDesconocida_sinViolacionAunqueHayaAlumnos() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG2);
        ProblemaHorario problema = problema(Map.of(), alumnos(120, 100, null), p);

        assertThat(capacidad(problema, en(problema, Map.of()))).isEmpty();
    }

    @Test
    void alumnosDesconocidosCuentanCero_doceMasNadaEnOnce_violacion() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG3);
        ProblemaHorario problema = problema(Map.of("A", 11), alumnos(12, null, null), p);

        assertThat(capacidad(problema, en(problema, Map.of()))).hasSize(1);
    }

    @Test
    void alumnosDesconocidosCuentanCero_diezMasNadaEnOnce_sinViolacion() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG3);
        ProblemaHorario problema = problema(Map.of("A", 11), alumnos(10, null, null), p);

        assertThat(capacidad(problema, en(problema, Map.of()))).isEmpty();
    }

    // ======================================================= aula entre candidatas

    @Test
    void aulaElegidaEntreCandidatas_seJuzgaLaElegida() {
        Plaza p = candidatas("ACT-P1", P1, Set.of(A, B), SG1, SG2);
        ProblemaHorario problema = problema(Map.of("A", 20, "B", 30), alumnos(12, 10, null), p);

        List<Violacion> enA = capacidad(problema, en(problema, Map.of(p, A)));
        assertThat(enA).hasSize(1);
        assertThat(enA.get(0).recursoCodigo()).isEqualTo("A");
        assertThat(enA.get(0).celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));

        assertThat(capacidad(problema, en(problema, Map.of(p, B)))).as("B tiene 30").isEmpty();
    }

    @Test
    void candidatasEnFronteraYConAlumnosDesconocidos_mismasReglasQueLaFija() {
        Plaza p = candidatas("ACT-P1", P1, Set.of(A, B), SG1, SG3);
        ProblemaHorario cabe = problema(Map.of("A", 12), alumnos(12, null, null), p);
        ProblemaHorario noCabe = problema(Map.of("A", 11), alumnos(12, null, null), p);

        assertThat(capacidad(cabe, en(cabe, Map.of(p, A)))).isEmpty();
        assertThat(capacidad(noCabe, en(noCabe, Map.of(p, A)))).hasSize(1);
    }

    // ===================================================================== forma

    @Test
    void dosPlazas_soloLaQueNoCabe_yCadaUnaConSuCelda() {
        Plaza grande = fija("ACT-P1", P1, A, SG1, SG2);   // 12 + 10 = 22 en A (20): no cabe
        Plaza pequena = fija("ACT-P2", P2, B, SG3);       // 5 en B (20): cabe
        ProblemaHorario problema = problema(Map.of("A", 20, "B", 20), alumnos(12, 10, 5),
                grande, pequena);

        List<Violacion> vs = capacidad(problema, en(problema, Map.of()));

        assertThat(vs).hasSize(1);
        assertThat(vs.get(0).celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));
    }

    @Test
    void instanciaSinColocar_noDuplicaElAvisoDeSinColocar() {
        Plaza p = fija("ACT-P1", P1, A, SG1, SG2);
        ProblemaHorario problema = problema(Map.of("A", 20), alumnos(12, 10, null), p);

        assertThat(capacidad(problema, new SolucionHorario(Map.of()))).isEmpty();
    }
}
