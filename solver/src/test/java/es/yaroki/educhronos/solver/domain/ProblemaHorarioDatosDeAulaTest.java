package es.yaroki.educhronos.solver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Capacidad de aula y alumnos de subgrupo en el {@link ProblemaHorario} (S207,
 * C-deduccion-aulas, C2). Van por código, fuera de {@link Aula} y {@link Subgrupo}, para no tocar
 * su identidad. Sin ellos el problema es el de antes; con ellos se leen por código.
 */
class ProblemaHorarioDatosDeAulaTest {

    private static final GrupoAdministrativo G =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Aula A1 = new Aula("A1", "A1");
    private static final Aula A2 = new Aula("A2", "A2");
    private static final Subgrupo SG1 = new Subgrupo("G-1", Set.of(G));
    private static final Subgrupo SG2 = new Subgrupo("G-2", Set.of(G));

    private static ProblemaHorario con(Map<String, Integer> capacidades, Map<String, Integer> alumnos) {
        return new ProblemaHorario(List.of(), List.of(A1, A2), List.of(), List.of(), List.of(G),
                List.of(SG1, SG2), List.of(), List.of(), List.of(), List.of(), capacidades, alumnos);
    }

    @Test
    void elConstructorDeDiezListas_noTraeNingunDato() {
        ProblemaHorario p = new ProblemaHorario(List.of(), List.of(A1), List.of(), List.of(),
                List.of(G), List.of(SG1), List.of(), List.of(), List.of(), List.of());

        assertThat(p.capacidadesDeAula()).isEmpty();
        assertThat(p.alumnosDeSubgrupo()).isEmpty();
        assertThat(p.capacidadDe(A1)).isEmpty();
        assertThat(p.alumnosDe(SG1)).isEmpty();
    }

    @Test
    void losDatosSeLeenPorCodigo_yUnCodigoSinDatoEsVacio() {
        ProblemaHorario p = con(Map.of("A1", 20), Map.of("G-1", 12, "G-2", 0));

        assertThat(p.capacidadDe(A1)).contains(20);
        assertThat(p.capacidadDe(A2)).as("A2 sin capacidad conocida").isEmpty();
        // Otra instancia con el mismo código: el dato va por código, no por identidad de objeto.
        assertThat(p.capacidadDe(new Aula("A1", "otro nombre"))).contains(20);
        assertThat(p.alumnosDe(SG1)).contains(12);
        assertThat(p.alumnosDe(SG2)).as("cero alumnos es un dato, no ausencia").contains(0);
    }

    @Test
    void losDatosNoCambianLaIdentidadDeAulaNiDeSubgrupo() {
        ProblemaHorario sin = con(Map.of(), Map.of());
        ProblemaHorario conDatos = con(Map.of("A1", 20), Map.of("G-1", 12));

        assertThat(conDatos.aulas()).isEqualTo(sin.aulas());
        assertThat(conDatos.subgrupos()).isEqualTo(sin.subgrupos());
        assertThat(conDatos.aulas().get(0)).isEqualTo(A1).hasSameHashCodeAs(A1);
    }

    @Test
    void unaCapacidadDeUnAulaQueNoEstaEnElProblema_seRechaza() {
        assertThatThrownBy(() -> con(Map.of("Z9", 20), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Z9");
    }

    @Test
    void unosAlumnosDeUnSubgrupoQueNoEstaEnElProblema_seRechazan() {
        assertThatThrownBy(() -> con(Map.of(), Map.of("G-9", 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("G-9");
    }

    @Test
    void unValorNegativo_seRechaza() {
        assertThatThrownBy(() -> con(Map.of("A1", -1), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A1");
        assertThatThrownBy(() -> con(Map.of(), Map.of("G-1", -2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("G-1");
    }
}
