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
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Dos plazas de la misma sesión nunca comparten aula (S207, C-deduccion-aulas, C1). Las plazas
 * de una actividad ocurren a la vez, así que el aula se cuenta por plaza y no por instancia.
 *
 * <p>Una actividad de dos plazas con subgrupos disjuntos y una repetición, en una jornada de
 * tres tramos: el tiempo sobra y lo único que puede hacer infactible el problema es el aula.
 * Los problemas se construyen en el dominio, sin el cargador JSON, que ya rechaza por su cuenta
 * dos aulas fijas iguales en una actividad: la ruta de la aplicación no pasa por él.
 */
class SolverHorarioAulasDistintasEnSesionTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG1 = new Subgrupo("G-1", Set.of(GRUPO));
    private static final Subgrupo SG2 = new Subgrupo("G-2", Set.of(GRUPO));
    private static final Profesor P1 = new Profesor("P1", "Uno");
    private static final Profesor P2 = new Profesor("P2", "Dos");
    private static final Aula X = new Aula("X", "X");
    private static final Aula Y = new Aula("Y", "Y");
    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);
    private static final Tramo L3 = new Tramo("L3", 1, 3);

    private static Plaza fija(String cod, Profesor p, Subgrupo sg, Aula aula) {
        return new Plaza(cod, ASIG, Set.of(p), Optional.of(aula), Set.of(), Set.of(sg));
    }

    private static Plaza candidatas(String cod, Profesor p, Subgrupo sg, Aula... aulas) {
        return new Plaza(cod, ASIG, Set.of(p), Optional.empty(), Set.of(aulas), Set.of(sg));
    }

    private static ProblemaHorario problema(Plaza primera, Plaza segunda) {
        Actividad act = new Actividad("A", Optional.of(ASIG), 1, 1, PatronTemporal.NEUTRA,
                List.of(primera, segunda), false);
        return new ProblemaHorario(
                List.of(L1, L2, L3), List.of(X, Y), List.of(ASIG), List.of(P1, P2),
                List.of(GRUPO), List.of(SG1, SG2), List.of(act),
                List.of(), List.of(), List.of());
    }

    private static Aula aulaDe(ProblemaHorario problema, SolucionHorario sol, Plaza plaza) {
        ActividadInstancia inst = new ActividadInstancia(problema.actividades().get(0), 1);
        return sol.aulaElegida(inst, plaza).orElseThrow(() -> new AssertionError("sin aula: " + plaza.codigo()));
    }

    /** (a) Candidatas {X,Y} y {X,Y}: resuelve y cada plaza recibe un aula distinta. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void dosPlazasConLasMismasDosCandidatasRecibenAulasDistintas() {
        Plaza p1 = candidatas("A-P1", P1, SG1, X, Y);
        Plaza p2 = candidatas("A-P2", P2, SG2, X, Y);
        ProblemaHorario problema = problema(p1, p2);

        SolucionHorario sol = new SolverHorario().resolver(problema);

        assertThat(aulaDe(problema, sol, p1)).isNotEqualTo(aulaDe(problema, sol, p2));
        assertThat(new VerificadorSolucion().verificar(problema, sol).violaciones()).isEmpty();
    }

    /** (b) Candidatas {X} y {X}: las dos plazas necesitan X en el mismo tramo, infactible. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void dosPlazasConLaMismaCandidataUnicaSonInfactibles() {
        ProblemaHorario problema = problema(candidatas("A-P1", P1, SG1, X), candidatas("A-P2", P2, SG2, X));

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }

    /** (c) Fija X y candidatas {X,Y}: la plaza variable tiene que irse a Y. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void fijaYCandidatasQueLaIncluyen_laVariableRecibeLaOtra() {
        Plaza p1 = fija("A-P1", P1, SG1, X);
        Plaza p2 = candidatas("A-P2", P2, SG2, X, Y);
        ProblemaHorario problema = problema(p1, p2);

        SolucionHorario sol = new SolverHorario().resolver(problema);

        assertThat(aulaDe(problema, sol, p1)).isEqualTo(X);
        assertThat(aulaDe(problema, sol, p2)).isEqualTo(Y);
        assertThat(new VerificadorSolucion().verificar(problema, sol).violaciones()).isEmpty();
    }

    /**
     * (d) Fija X y fija X: no hay horario. El dominio no lo rechaza al construir el problema
     * (solo lo hace el cargador JSON), así que es el modelo quien lo declara infactible.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void dosPlazasConLaMismaAulaFijaSonInfactibles() {
        ProblemaHorario problema = problema(fija("A-P1", P1, SG1, X), fija("A-P2", P2, SG2, X));

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }
}
