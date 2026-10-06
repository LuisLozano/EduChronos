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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Una plaza SIN AULA —una reunión, sin aula fija ni candidatas y sin subgrupos— entra en el
 * modelo y en la solución (S201, C-actividad-sin-alumnos, T2). El solver no sabe qué es una
 * reunión: sólo ve una plaza sin aula, que no crea opciones de aula ni entra en el no solape de
 * aulas, pero sí ocupa a su profesor.
 *
 * <p>Problema construido en Java, con el patrón de {@code SolverHorarioIndisponibilidadBloqueTest}:
 * así no cambia el conjunto de fixtures JSON de la huella canónica.
 *
 * <p><b>Por qué exactamente dos tramos.</b> El profesor P imparte la clase y asiste a la
 * reunión. Con dos tramos sólo caben repartidas, una en cada uno: si el modelo no contara la
 * reunión en el no solape de profesores, nada impediría ponerlas juntas.
 */
class SolverHorarioPlazaSinAulaTest {

    private static final Asignatura MAT = new Asignatura("MAT", "Matemáticas");
    private static final Asignatura RED = new Asignatura("RED", "Reunión de departamento");
    private static final GrupoAdministrativo G =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo S = new Subgrupo("S", Set.of(G));
    private static final Aula A1 = new Aula("A1", "Aula 1");
    private static final Profesor P = new Profesor("P", "Profesor");

    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);

    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void unaReunionSinAulaSeColocaEnOtroTramoQueLaClaseDeSuProfesor() {
        Plaza plazaCla = new Plaza("CLA-P1", MAT, Set.of(P), Optional.of(A1), Set.of(), Set.of(S));
        Plaza plazaReu = new Plaza("REU-P1", RED, Set.of(P), Optional.empty(), Set.of(), Set.of());
        Actividad cla = new Actividad("CLA", Optional.of(MAT), 1, 1,
                PatronTemporal.NEUTRA, List.of(plazaCla), false);
        Actividad reu = new Actividad("REU", Optional.of(RED), 1, 1,
                PatronTemporal.NEUTRA, List.of(plazaReu), false);
        ProblemaHorario p = new ProblemaHorario(List.of(L1, L2), List.of(A1), List.of(MAT, RED),
                List.of(P), List.of(G), List.of(S), List.of(cla, reu), List.of(), List.of(),
                List.of());
        ActividadInstancia instCla = new ActividadInstancia(cla, 1);
        ActividadInstancia instReu = new ActividadInstancia(reu, 1);

        SolucionHorario sol = solver.resolverOptimizando(p);

        assertThat(sol.tramoDeInstancia(instCla)).as("la clase, colocada").isPresent();
        assertThat(sol.tramoDeInstancia(instReu)).as("la reunión, colocada").isPresent();
        assertThat(sol.tramoDeInstancia(instReu))
                .as("P no puede estar en dos sitios: tramos distintos")
                .isNotEqualTo(sol.tramoDeInstancia(instCla));
        assertThat(sol.aulaElegida(instReu, plazaReu)).as("la reunión, sin aula").isEmpty();
        assertThat(sol.aulaElegida(instCla, plazaCla)).as("la clase, en su aula fija").contains(A1);
        assertThat(verificador.verificar(p, sol).violaciones()).as("violaciones duras").isEmpty();
    }
}
