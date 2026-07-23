package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del transporte de tutorías de {@link CatalogoMapper#aProblemaHorario}
 * (Bloque 8.5-D2b-2): las {@code app.catalog.ProfesorTutoria} viajan a
 * {@code domain.ProfesorTutoria} resolviendo profesor y grupo POR IDENTIDAD contra los
 * catálogos ya mapeados, con AMBOS roles (el filtro de S8 vive en el verificador).
 *
 * <p>Puros: entidades en memoria con {@code new} (constructores públicos), sin Spring
 * ni BD. Códigos de profesor ({@code P-*}) y grupo ({@code G-*}) DIVERGENTES a
 * propósito: un lookup que cruce las dos dimensiones no puede resolver por casualidad.
 */
class CatalogoMapperTutoriaTest {

    private static final es.yaroki.educhronos.solver.domain.RolTutoria PRINCIPAL_DOM =
            es.yaroki.educhronos.solver.domain.RolTutoria.TUTOR_PRINCIPAL;
    private static final es.yaroki.educhronos.solver.domain.RolTutoria COTUTOR_DOM =
            es.yaroki.educhronos.solver.domain.RolTutoria.CO_TUTOR;

    private static Profesor prof(String codigo) {
        return new Profesor(codigo, "Nombre de " + codigo);
    }

    private static GrupoAdministrativo grupo(String codigo) {
        return new GrupoAdministrativo(codigo, null, TipoGrupo.ORDINARIO, null);
    }

    /** Ensambla pasando solo profesores, grupos y tutorías; el resto de listas vacías. */
    private static ProblemaHorario mapear(List<Profesor> profesores,
                                          List<GrupoAdministrativo> grupos,
                                          List<ProfesorTutoria> tutorias) {
        return CatalogoMapper.aProblemaHorario(
                List.of(), List.of(), List.of(), profesores,
                grupos, List.of(), List.of(), List.of(),
                List.of(), List.of(), tutorias);
    }

    private static es.yaroki.educhronos.solver.domain.ProfesorTutoria porProfesor(
            ProblemaHorario problema, String codigoProfesor) {
        return problema.tutorias().stream()
                .filter(t -> t.profesor().codigo().equals(codigoProfesor))
                .findFirst().orElseThrow(() ->
                        new AssertionError("sin tutoría para " + codigoProfesor));
    }

    // B-T1: dos tutorías (una PRINCIPAL, una CO_TUTOR) con profesores/grupos divergentes
    // -> las DOS llegan, con su rol, profesor y grupo correctos.
    @Test
    void transportaAmbasTutoriasConSuRolProfesorYGrupo() {
        Profesor pMat = prof("P-MAT");
        Profesor pLen = prof("P-LEN");
        GrupoAdministrativo g1 = grupo("G-1ESO");
        GrupoAdministrativo g2 = grupo("G-2ESO");

        ProblemaHorario problema = mapear(
                List.of(pMat, pLen), List.of(g1, g2),
                List.of(new ProfesorTutoria(pMat, g1, RolTutoria.TUTOR_PRINCIPAL),
                        new ProfesorTutoria(pLen, g2, RolTutoria.CO_TUTOR)));

        assertThat(problema.tutorias()).hasSize(2);

        var tMat = porProfesor(problema, "P-MAT");
        assertThat(tMat.grupo().codigo()).isEqualTo("G-1ESO");
        assertThat(tMat.rol()).isEqualTo(PRINCIPAL_DOM);

        var tLen = porProfesor(problema, "P-LEN");
        assertThat(tLen.grupo().codigo()).isEqualTo("G-2ESO");
        assertThat(tLen.rol()).isEqualTo(COTUTOR_DOM);
    }

    // B-T2: el CO_TUTOR llega como CO_TUTOR, NO colapsado a TUTOR_PRINCIPAL.
    @Test
    void coTutorNoSeColapsaAPrincipal() {
        Profesor pLen = prof("P-LEN");
        GrupoAdministrativo g2 = grupo("G-2ESO");

        ProblemaHorario problema = mapear(
                List.of(pLen), List.of(g2),
                List.of(new ProfesorTutoria(pLen, g2, RolTutoria.CO_TUTOR)));

        assertThat(problema.tutorias()).singleElement()
                .extracting(es.yaroki.educhronos.solver.domain.ProfesorTutoria::rol)
                .isEqualTo(COTUTOR_DOM);
    }

    // B-T3: lista vacía de tutorías -> tutorias() vacía, no null, sin reventar.
    @Test
    void sinTutoriasDevuelveListaVaciaNoNull() {
        ProblemaHorario problema = mapear(List.of(), List.of(), List.of());

        assertThat(problema.tutorias()).isNotNull().isEmpty();
    }

    // B-T4: cruce defensivo. Con un segundo par y códigos divergentes, un lookup que
    // intercambie profesor y grupo resuelve a algo DISTINTO y detectable: el emparejamiento
    // (profesor, grupo, rol) se conserva y el cruzado (P-MAT/G-2ESO) NO aparece.
    @Test
    void elEmparejamientoProfesorGrupoNoSeCruza() {
        Profesor pMat = prof("P-MAT");
        Profesor pLen = prof("P-LEN");
        GrupoAdministrativo g1 = grupo("G-1ESO");
        GrupoAdministrativo g2 = grupo("G-2ESO");

        ProblemaHorario problema = mapear(
                List.of(pMat, pLen), List.of(g1, g2),
                List.of(new ProfesorTutoria(pMat, g1, RolTutoria.TUTOR_PRINCIPAL),
                        new ProfesorTutoria(pLen, g2, RolTutoria.CO_TUTOR)));

        assertThat(problema.tutorias())
                .extracting(t -> t.profesor().codigo() + "/" + t.grupo().codigo() + "/" + t.rol())
                .containsExactlyInAnyOrder(
                        "P-MAT/G-1ESO/TUTOR_PRINCIPAL",
                        "P-LEN/G-2ESO/CO_TUTOR");

        assertThat(problema.tutorias())
                .as("emparejamiento cruzado P-MAT/G-2ESO no debe existir")
                .noneMatch(t -> t.profesor().codigo().equals("P-MAT")
                        && t.grupo().codigo().equals("G-2ESO"));
    }
}
