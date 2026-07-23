package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.ProfesorTutoria;
import es.yaroki.educhronos.solver.domain.RolTutoria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica la comprobación de S8 (§4.6) de {@link VerificadorSolucion}: una actividad
 * {@code requiereTutor} debe estar impartida por un TUTOR_PRINCIPAL de un grupo que
 * cubre. S8 es propiedad del catálogo, no del horario: los problemas se fabrican a mano
 * y se verifican con una {@link SolucionHorario} VACÍA (las instancias sin colocar las
 * reporta otra comprobación; aquí solo se mira la regla {@code TUTORIA_SIN_TUTOR}).
 *
 * <p>Códigos de profesor y grupo divergentes a propósito ({@code P-*} vs {@code G-*}):
 * un lookup que cruce las dimensiones resuelve a algo distinto en vez de acertar por
 * casualidad.
 */
class VerificadorSolucionTutoriaTest {

    private static final Asignatura ASIG = new Asignatura("TUT", "Tutoria");
    private static final Aula AULA = new Aula("AULA-X", "Aula X");
    private static final Tramo LUN_1 = new Tramo("LUN-1", 1, 1);

    private static final GrupoAdministrativo G_UNO =
            new GrupoAdministrativo("G-UNO", TipoGrupo.ORDINARIO, Optional.empty());
    private static final GrupoAdministrativo G_DOS =
            new GrupoAdministrativo("G-DOS", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG_UNO = new Subgrupo("SG-UNO", Set.of(G_UNO));
    private static final Subgrupo SG_DOS = new Subgrupo("SG-DOS", Set.of(G_DOS));

    private static final Profesor P_ALFA = new Profesor("P-ALFA", "Profesor Alfa");
    private static final Profesor P_BETA = new Profesor("P-BETA", "Profesor Beta");

    /** Actividad mono-plaza sobre un subgrupo, con N profesores en la (única) plaza. */
    private static Actividad actividad(String cod, Subgrupo sg, boolean requiereTutor,
                                       Profesor... profesores) {
        Plaza plaza = new Plaza(cod + "-P1", ASIG, Set.of(profesores),
                Optional.of(AULA), Set.of(), Set.of(sg));
        return new Actividad(cod, Optional.of(ASIG), 1, 1,
                PatronTemporal.NEUTRA, List.of(plaza), requiereTutor);
    }

    private static ProblemaHorario problema(List<Actividad> actividades,
                                            List<ProfesorTutoria> tutorias) {
        return new ProblemaHorario(
                List.of(LUN_1),
                List.of(AULA), List.of(ASIG),
                List.of(P_ALFA, P_BETA),
                List.of(G_UNO, G_DOS), List.of(SG_UNO, SG_DOS),
                actividades,
                List.of(),   // restriccionesHorarias
                List.of(),   // bloqueos
                tutorias);
    }

    /** Solución vacía: S8 no la usa; las demás comprobaciones se filtran fuera. */
    private static List<Violacion> tutoriaViolaciones(ProblemaHorario problema) {
        return new VerificadorSolucion().verificar(problema, new SolucionHorario(Map.of()))
                .violaciones().stream()
                .filter(v -> v.regla() == ReglaDura.TUTORIA_SIN_TUTOR)
                .toList();
    }

    // T1: profesor ES TUTOR_PRINCIPAL del grupo cubierto -> NO viola.
    @Test
    void tutorPrincipalDelGrupoCubierto_noViola() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, true, P_ALFA)),
                List.of(new ProfesorTutoria(P_ALFA, G_UNO, RolTutoria.TUTOR_PRINCIPAL)));

        assertThat(tutoriaViolaciones(p)).isEmpty();
    }

    // T2: mismo caso pero la tutoría es CO_TUTOR (mismo profesor, mismo grupo) -> SÍ viola.
    // Par discriminante del rol junto con T1: CO_TUTOR no satisface S8.
    @Test
    void mismoProfesorPeroCoTutor_viola() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, true, P_ALFA)),
                List.of(new ProfesorTutoria(P_ALFA, G_UNO, RolTutoria.CO_TUTOR)));

        assertThat(tutoriaViolaciones(p)).hasSize(1);
    }

    // T3: profesor tutor de OTRO grupo, distinto del cubierto por sus subgrupos -> SÍ viola.
    // Mata "ignoro el grupo y solo miro que el profesor sea tutor de algo".
    @Test
    void tutorPrincipalDeOtroGrupo_viola() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, true, P_ALFA)), // cubre G-UNO
                List.of(new ProfesorTutoria(P_ALFA, G_DOS, RolTutoria.TUTOR_PRINCIPAL))); // tutor de G-DOS

        assertThat(tutoriaViolaciones(p)).hasSize(1);
    }

    // T4: actividad requiereTutor=FALSE sin ninguna tutoría -> NO viola.
    // Mata "verifico todas las actividades".
    @Test
    void requiereTutorFalse_noViola() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, false, P_ALFA)),
                List.of());

        assertThat(tutoriaViolaciones(p)).isEmpty();
    }

    // T5: co-docencia: plaza con DOS profesores, solo UNO es TUTOR_PRINCIPAL -> NO viola.
    // Mata "exijo que TODOS los profesores de la plaza sean tutores": basta uno.
    @Test
    void coDocenciaConUnSoloTutor_noViola() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, true, P_ALFA, P_BETA)),
                List.of(new ProfesorTutoria(P_BETA, G_UNO, RolTutoria.TUTOR_PRINCIPAL))); // solo P-BETA

        assertThat(tutoriaViolaciones(p)).isEmpty();
    }

    // T6: FORMA de la violación emitida: tramoCodigo == null y recursoCodigo == grupo cubierto.
    @Test
    void violacionLlevaTramoNullYRecursoElGrupoCubierto() {
        ProblemaHorario p = problema(
                List.of(actividad("ACT", SG_UNO, true, P_ALFA)), // cubre G-UNO
                List.of(new ProfesorTutoria(P_ALFA, G_UNO, RolTutoria.CO_TUTOR))); // viola

        List<Violacion> violaciones = tutoriaViolaciones(p);
        assertThat(violaciones).hasSize(1);
        Violacion v = violaciones.get(0);
        assertThat(v.tramoCodigo()).isNull();
        assertThat(v.recursoCodigo()).isEqualTo("G-UNO");
    }

    // Fixture discriminante problema-8-5-s8.json: una actividad que SÍ cumple S8 y otra
    // con CO_TUTOR sobre un grupo cubierto (y el mismo profesor TUTOR_PRINCIPAL de OTRO
    // grupo) -> exactamente una violación, la de ACT-VIOLA, con recurso G-DOS.
    @Test
    void fixtureS8_soloViolaLaActividadCoTutor() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/problema-8-5-s8.json")) {
            problema = new ProblemaHorarioJsonLoader().cargar(in);
        }

        List<Violacion> violaciones = tutoriaViolaciones(problema);

        assertThat(violaciones).hasSize(1);
        assertThat(violaciones.get(0).recursoCodigo()).isEqualTo("G-DOS");
        assertThat(violaciones.get(0).celdas())
                .allMatch(c -> c.actividadCodigo().equals("ACT-VIOLA"));
    }
}
