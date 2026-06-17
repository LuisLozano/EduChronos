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
 * Verifica que {@link VerificadorSolucion} comprueba el no-solape por GRUPO
 * (S9), de forma aislada: se le entregan {@link SolucionHorario} fabricadas a
 * mano (validas e invalidas) sin pasar por el solver.
 *
 * <p>Es complementario a {@code RestriccionNoSolapeGrupoTest}, que prueba que
 * la restriccion del modelo CP-SAT muerde. Aqui se prueba la red de seguridad
 * independiente: que el verificador detecte el solape que el solver impide,
 * para que un bug del modelo no pase desapercibido.
 */
class VerificadorSolucionGrupoTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");

    // No-DISTRIBUIDA para no activar verificarDistribucion. AJUSTAR al valor
    // real del enum si no es AGRUPADA.
    private static final PatronTemporal NO_DISTRIBUIDA = PatronTemporal.AGRUPADA;

    private static final GrupoAdministrativo G_1A =
            new GrupoAdministrativo("1A", TipoGrupo.ORDINARIO, Optional.empty());

    private static final Tramo LUN_1 = new Tramo("LUN-1", 1, 1);
    private static final Tramo LUN_2 = new Tramo("LUN-2", 1, 2);

    /** Actividad mono-plaza, mono-instancia, mono-subgrupo, aula fija. */
    private static Actividad actividad(String cod, Subgrupo sg, Profesor prof, Aula aula) {
        Plaza plaza = new Plaza(
                cod + "-P1", ASIG, Set.of(prof),
                Optional.of(aula), Set.of(), Set.of(sg));
        return new Actividad(cod, Optional.of(ASIG), 1, 1, NO_DISTRIBUIDA, List.of(plaza));
    }

    private static ProblemaHorario problema(List<Actividad> actividades) {
        return new ProblemaHorario(
                List.of(LUN_1, LUN_2),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                actividades);
    }

    /** La (unica) instancia esperada de una actividad, segun la expansion real. */
    private static ActividadInstancia instanciaDe(ProblemaHorario problema, String codActividad) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codActividad))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no expandida: " + codActividad));
    }

    @Test
    void reportaSolapeDeGrupoEntreActividadesDistintas() {
        Subgrupo sgX = new Subgrupo("1A-X", Set.of(G_1A));
        Subgrupo sgY = new Subgrupo("1A-Y", Set.of(G_1A)); // mismo grupo, subgrupo distinto

        Actividad actX = actividad("Act-X", sgX, new Profesor("P1", "P1"), new Aula("A1", "A1"));
        Actividad actY = actividad("Act-Y", sgY, new Profesor("P2", "P2"), new Aula("A2", "A2"));
        ProblemaHorario problema = problema(List.of(actX, actY));

        // Ambas instancias en el MISMO tramo: dos instancias distintas que tocan
        // 1A -> grupo contado 2 veces -> debe reportarse.
        SolucionHorario sol = new SolucionHorario(Map.of(
                instanciaDe(problema, "Act-X"), LUN_1,
                instanciaDe(problema, "Act-Y"), LUN_1));

        ResultadoVerificacion r = new VerificadorSolucion().verificar(problema, sol);

        assertThat(r.violaciones())
                .as("solape de grupo 1A en LUN-1 debe reportarse")
                .anyMatch(v -> v.contains("Grupo 1A") && v.contains("2 veces"));
    }

    @Test
    void noReportaGrupoEnTramosDistintos() {
        Subgrupo sgX = new Subgrupo("1A-X", Set.of(G_1A));
        Subgrupo sgY = new Subgrupo("1A-Y", Set.of(G_1A));

        Actividad actX = actividad("Act-X", sgX, new Profesor("P1", "P1"), new Aula("A1", "A1"));
        Actividad actY = actividad("Act-Y", sgY, new Profesor("P2", "P2"), new Aula("A2", "A2"));
        ProblemaHorario problema = problema(List.of(actX, actY));

        SolucionHorario sol = new SolucionHorario(Map.of(
                instanciaDe(problema, "Act-X"), LUN_1,
                instanciaDe(problema, "Act-Y"), LUN_2)); // tramos distintos

        ResultadoVerificacion r = new VerificadorSolucion().verificar(problema, sol);

        assertThat(r.violaciones())
                .as("en tramos distintos no hay solape de grupo")
                .noneMatch(v -> v.contains("Grupo"));
    }

    @Test
    void desdobleNoSeReportaComoSolapeDeGrupo_regresion() {
        // Regresion: protege el colapso por Set-por-instancia. UNA actividad con
        // dos plazas, dos subgrupos del mismo grupo (desdoble), mismo tramo. El
        // grupo se cuenta una vez. Si una refactorizacion contara por plaza en
        // vez de por instancia, este test lo cazaria.
        Subgrupo sg1 = new Subgrupo("1A-CyR-Tec", Set.of(G_1A));
        Subgrupo sg2 = new Subgrupo("1A-CyR-Inf", Set.of(G_1A));

        Plaza p1 = new Plaza("CYR-P1", ASIG, Set.of(new Profesor("P1", "P1")),
                Optional.of(new Aula("A1", "A1")), Set.of(), Set.of(sg1));
        Plaza p2 = new Plaza("CYR-P2", ASIG, Set.of(new Profesor("P2", "P2")),
                Optional.of(new Aula("A2", "A2")), Set.of(), Set.of(sg2));
        Actividad cyr = new Actividad("CyR", Optional.of(ASIG), 1, 1, NO_DISTRIBUIDA, List.of(p1, p2));
        ProblemaHorario problema = problema(List.of(cyr));

        SolucionHorario sol = new SolucionHorario(Map.of(
                instanciaDe(problema, "CyR"), LUN_1));

        ResultadoVerificacion r = new VerificadorSolucion().verificar(problema, sol);

        assertThat(r.violaciones())
                .as("un desdoble es una sola actividad: el grupo se ocupa una vez")
                .noneMatch(v -> v.contains("Grupo"));
    }
}