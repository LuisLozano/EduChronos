package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica la ATRIBUCIÓN ESTRUCTURADA de {@link VerificadorSolucion} (Fase 8,
 * Bloque 8.3-A): que cada {@link Violacion} sepa QUIÉNES la causan
 * ({@link Violacion#celdas()}), no solo que existe. Es lo que la implementación
 * anterior —un contador que tiraba las instancias culpables— era incapaz de
 * producir; este test es la prueba de que la instrumentación funciona.
 *
 * <p>Se fabrican {@link SolucionHorario} a mano (sin solver), como en
 * {@code VerificadorSolucionGrupoTest}. Cubre además la asimetría D15: la celda
 * de {@link ReglaDura#SOLAPE_AULA} lleva {@code plazaCodigo} no-null (el aula se
 * cuenta por plaza), mientras que la de {@link ReglaDura#SOLAPE_PROFESOR} lo
 * lleva null (se cuenta por instancia).
 */
class VerificadorSolucionAtribucionTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final PatronTemporal NO_DISTRIBUIDA = PatronTemporal.AGRUPADA;

    // Grupos distintos: así el solape de PROFESOR/AULA no arrastra un solape de
    // grupo, y cada escenario produce EXACTAMENTE una violación.
    private static final GrupoAdministrativo G_1A =
            new GrupoAdministrativo("1A", TipoGrupo.ORDINARIO, Optional.empty());
    private static final GrupoAdministrativo G_1B =
            new GrupoAdministrativo("1B", TipoGrupo.ORDINARIO, Optional.empty());

    private static final Tramo LUN_1 = new Tramo("LUN-1", 1, 1);
    private static final Tramo LUN_2 = new Tramo("LUN-2", 1, 2);

    /** Actividad mono-plaza, mono-instancia, mono-subgrupo, aula fija. */
    private static Actividad actividad(String cod, Subgrupo sg, Profesor prof, Aula aula) {
        Plaza plaza = new Plaza(
                cod + "-P1", ASIG, Set.of(prof),
                Optional.of(aula), Set.of(), Set.of(sg));
        return new Actividad(cod, Optional.of(ASIG), 1, 1, NO_DISTRIBUIDA, List.of(plaza), false);
    }

    private static ProblemaHorario problema(List<Actividad> actividades) {
        return new ProblemaHorario(
                List.of(LUN_1, LUN_2),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                actividades,
                List.of(),    // restriccionesHorarias
                List.of(),    // bloqueos
                List.of());   // tutorias
    }

    private static ActividadInstancia instanciaDe(ProblemaHorario problema, String codActividad) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codActividad))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no expandida: " + codActividad));
    }

    @Test
    void solapeDeProfesorAtribuyeLasDosCeldasCulpablesConPlazaNull() {
        Profesor prof = new Profesor("MAT8", "Mates");
        Subgrupo sgA = new Subgrupo("1A-X", Set.of(G_1A));
        Subgrupo sgB = new Subgrupo("1B-Y", Set.of(G_1B)); // grupo distinto: no arrastra SOLAPE_GRUPO

        // Aulas distintas: no arrastran SOLAPE_AULA. Solo colisiona el profesor.
        Actividad actX = actividad("Act-X", sgA, prof, new Aula("A1", "A1"));
        Actividad actY = actividad("Act-Y", sgB, prof, new Aula("A2", "A2"));
        ProblemaHorario problema = problema(List.of(actX, actY));

        SolucionHorario sol = new SolucionHorario(Map.of(
                instanciaDe(problema, "Act-X"), LUN_1,
                instanciaDe(problema, "Act-Y"), LUN_1)); // mismo tramo

        List<Violacion> violaciones =
                new VerificadorSolucion().verificar(problema, sol).violaciones();

        assertThat(violaciones).hasSize(1);
        Violacion v = violaciones.get(0);
        assertThat(v.regla()).isEqualTo(ReglaDura.SOLAPE_PROFESOR);
        assertThat(v.recursoCodigo()).isEqualTo("MAT8");
        assertThat(v.tramoCodigo()).isEqualTo("LUN-1");
        assertThat(v.celdas())
                .as("las 2 celdas culpables, por INSTANCIA (plazaCodigo null)")
                .containsExactlyInAnyOrder(
                        new CeldaRef("Act-X", 1, null),
                        new CeldaRef("Act-Y", 1, null));
        assertThat(v.celdas())
                .as("D15: en SOLAPE_PROFESOR el aula no interviene → plazaCodigo null")
                .allMatch(c -> c.plazaCodigo() == null);
    }

    @Test
    void solapeDeAulaAtribuyeCeldasConPlazaNoNull() {
        Aula aulaComun = new Aula("A12In", "Aula de informática");
        Subgrupo sgA = new Subgrupo("1A-X", Set.of(G_1A));
        Subgrupo sgB = new Subgrupo("1B-Y", Set.of(G_1B)); // grupo distinto

        // Profesores distintos: no arrastran SOLAPE_PROFESOR. Solo colisiona el aula.
        Actividad actP = actividad("Act-P", sgA, new Profesor("P1", "P1"), aulaComun);
        Actividad actQ = actividad("Act-Q", sgB, new Profesor("P2", "P2"), aulaComun);
        ProblemaHorario problema = problema(List.of(actP, actQ));

        SolucionHorario sol = new SolucionHorario(Map.of(
                instanciaDe(problema, "Act-P"), LUN_1,
                instanciaDe(problema, "Act-Q"), LUN_1)); // mismo tramo, misma aula

        List<Violacion> violaciones =
                new VerificadorSolucion().verificar(problema, sol).violaciones();

        assertThat(violaciones).hasSize(1);
        Violacion v = violaciones.get(0);
        assertThat(v.regla()).isEqualTo(ReglaDura.SOLAPE_AULA);
        assertThat(v.recursoCodigo()).isEqualTo("A12In");
        assertThat(v.tramoCodigo()).isEqualTo("LUN-1");
        assertThat(v.celdas())
                .as("D15: en SOLAPE_AULA la celda se atribuye por PLAZA → plazaCodigo no-null")
                .allMatch(c -> c.plazaCodigo() != null);
        assertThat(v.celdas())
                .containsExactlyInAnyOrder(
                        new CeldaRef("Act-P", 1, "Act-P-P1"),
                        new CeldaRef("Act-Q", 1, "Act-Q-P1"));
    }
}
