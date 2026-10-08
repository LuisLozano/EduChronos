package es.yaroki.educhronos.solver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Preferidas efectivas de cada plaza en el {@link ProblemaHorario} (S208, C-preferencias-aulas,
 * C2). Mismo patrón que la capacidad y los alumnos de S207: van por código, fuera de
 * {@link Plaza}, que es clave de mapas en el modelo, el verificador y la solución. Las calcula la
 * aplicación; el problema solo comprueba que sean de una plaza suya y estén dentro de sus aulas
 * posibles (caso g del contrato).
 */
class ProblemaHorarioPreferidasTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura");
    private static final GrupoAdministrativo G =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("G-1", Set.of(G));
    private static final Profesor PR = new Profesor("PR", "Profe");
    private static final Aula A1 = new Aula("A1", "A1");
    private static final Aula A2 = new Aula("A2", "A2");
    private static final Aula A3 = new Aula("A3", "A3");

    private static Plaza plaza(String cod, Optional<Aula> fija, Set<Aula> candidatas) {
        return new Plaza(cod, ASIG, Set.of(PR), fija, candidatas, Set.of(SG));
    }

    private static final Plaza CANDIDATAS = plaza("CAND-P1", Optional.empty(), Set.of(A1, A2));
    private static final Plaza FIJA = plaza("FIJA-P1", Optional.of(A3), Set.of());
    private static final Plaza SIN_AULA = plaza("SIN-P1", Optional.empty(), Set.of());

    private static Actividad actividad(String cod, Plaza... plazas) {
        return new Actividad(cod, Optional.of(ASIG), 1, 1, PatronTemporal.NEUTRA,
                List.of(plazas), false);
    }

    private static final List<Actividad> ACTIVIDADES = List.of(
            actividad("CAND", CANDIDATAS), actividad("FIJA", FIJA), actividad("SIN", SIN_AULA));

    private static ProblemaHorario con(List<Actividad> actividades, Map<String, Set<String>> preferidas) {
        return new ProblemaHorario(List.of(), List.of(A1, A2, A3), List.of(ASIG), List.of(PR),
                List.of(G), List.of(SG), actividades, List.of(), List.of(), List.of(),
                Map.of(), Map.of(), preferidas);
    }

    @Test
    void losConstructoresSinElDato_noTraenPreferidas() {
        ProblemaHorario diez = new ProblemaHorario(List.of(), List.of(A1, A2, A3), List.of(ASIG),
                List.of(PR), List.of(G), List.of(SG), ACTIVIDADES, List.of(), List.of(), List.of());
        ProblemaHorario doce = new ProblemaHorario(List.of(), List.of(A1, A2, A3), List.of(ASIG),
                List.of(PR), List.of(G), List.of(SG), ACTIVIDADES, List.of(), List.of(), List.of(),
                Map.of("A1", 20), Map.of());

        assertThat(diez.preferidasDePlaza()).isEmpty();
        assertThat(doce.preferidasDePlaza()).isEmpty();
        assertThat(doce.preferidasDe(CANDIDATAS)).isEmpty();
    }

    @Test
    void lasPreferidasSeLeenPorCodigoDePlaza() {
        ProblemaHorario p = con(ACTIVIDADES, Map.of("CAND-P1", Set.of("A2"), "FIJA-P1", Set.of("A3")));

        assertThat(p.preferidasDe(CANDIDATAS)).containsExactly("A2");
        assertThat(p.preferidasDe(FIJA)).containsExactly("A3");
        assertThat(p.preferidasDe(SIN_AULA)).as("plaza sin entrada").isEmpty();
    }

    @Test
    void unConjuntoVacio_esUnaPlazaSinPreferidas() {
        ProblemaHorario p = con(ACTIVIDADES, Map.of("CAND-P1", Set.of()));

        assertThat(p.preferidasDe(CANDIDATAS)).isEmpty();
    }

    @Test
    void elDatoEsInmutable() {
        java.util.Set<String> mutable = new java.util.HashSet<>(Set.of("A1"));
        ProblemaHorario p = con(ACTIVIDADES, new java.util.HashMap<>(Map.of("CAND-P1", mutable)));
        mutable.add("A2");

        assertThat(p.preferidasDe(CANDIDATAS)).containsExactly("A1");
        assertThatThrownBy(() -> p.preferidasDe(CANDIDATAS).add("A2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void unaPreferidaFueraDeLasCandidatas_seRechaza() {
        assertThatThrownBy(() -> con(ACTIVIDADES, Map.of("CAND-P1", Set.of("A1", "A3"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CAND-P1")
                .hasMessageContaining("A3");
    }

    @Test
    void unaPreferidaDistintaDelAulaFija_seRechaza() {
        assertThatThrownBy(() -> con(ACTIVIDADES, Map.of("FIJA-P1", Set.of("A1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIJA-P1")
                .hasMessageContaining("A1");
    }

    @Test
    void unaPreferidaEnUnaPlazaSinAula_seRechaza() {
        assertThatThrownBy(() -> con(ACTIVIDADES, Map.of("SIN-P1", Set.of("A1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SIN-P1");
    }

    @Test
    void preferidasDeUnaPlazaQueNoEstaEnElProblema_seRechazan() {
        assertThatThrownBy(() -> con(ACTIVIDADES, Map.of("OTRA-P1", Set.of("A1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTRA-P1");
    }

    @Test
    void preferidasDeUnCodigoDePlazaRepetido_seRechazan() {
        Plaza gemela = plaza("CAND-P1", Optional.empty(), Set.of(A1, A2));
        List<Actividad> conRepetida = List.of(actividad("CAND", CANDIDATAS), actividad("OTRA", gemela));

        assertThatThrownBy(() -> con(conRepetida, Map.of("CAND-P1", Set.of("A1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CAND-P1");
    }
}
