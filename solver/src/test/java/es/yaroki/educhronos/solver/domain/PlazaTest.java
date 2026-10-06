package es.yaroki.educhronos.solver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Lo que el record {@link Plaza} admite sobre el aula (S201, C-actividad-sin-alumnos, T2): a lo
 * sumo una de las dos ramas. Una plaza sin aula —una reunión, una función— es válida; con aula
 * fija y candidatas a la vez, no.
 */
class PlazaTest {

    private static final Asignatura ASIG = new Asignatura("RED", "Reunión de departamento");
    private static final Profesor P = new Profesor("P", "Profesor");
    private static final Aula A1 = new Aula("A1", "Aula 1");

    @Test
    void unaPlazaSinAulaFijaNiCandidatasSeConstruye() {
        Plaza plaza = new Plaza("REU-P1", ASIG, Set.of(P), Optional.empty(), Set.of(), Set.of());

        assertThat(plaza.aulaFija()).isEmpty();
        assertThat(plaza.aulasCandidatas()).isEmpty();
    }

    @Test
    void aulaFijaYCandidatasALaVezSiguenSinPoderse() {
        assertThatThrownBy(() -> new Plaza(
                        "X-P1", ASIG, Set.of(P), Optional.of(A1), Set.of(A1), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pueden coexistir");
    }
}
