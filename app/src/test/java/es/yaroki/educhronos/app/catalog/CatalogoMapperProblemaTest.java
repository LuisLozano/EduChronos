package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link CatalogoMapper#aProblemaHorario} (Fase 6, Bloque 6):
 * ensamblado de la entrada completa del solver a partir de las siete listas de
 * entidades JPA del catálogo. Puros: las entidades se construyen en memoria con
 * {@code new}+setters, sin contexto Spring ni BD.
 *
 * <p>Vive en {@code app.catalog} (no {@code app.mapper}) porque {@code Actividad}
 * y {@code Plaza} solo exponen constructor {@code protected}: instanciarlas con
 * {@code new}+setters solo es posible desde su propio paquete. Reutiliza el estilo
 * y los fixtures de {@code CatalogoMapperActividadTest}.
 */
class CatalogoMapperProblemaTest {

    // ── Caso 1: ensamblado feliz mínimo (con recreo intercalado) ──────────
    @Test
    void ensambladoFelizMinimo() {
        // ≥2 tramos con un recreo (orden 2) intercalado para ejercitar el
        // filtrado y la renumeración de aTramos: quedan 2 tramos lectivos.
        List<TramoSemanal> tramos = List.of(
                tramo(Dia.LUNES, 1, true),
                tramo(Dia.LUNES, 2, false), // recreo, se excluye
                tramo(Dia.LUNES, 3, true));

        Aula aula = aula("A5");
        Asignatura asig = asig("Mat", "Matemáticas");
        Profesor prof = prof("MAT8", "María Martínez");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = subgrupo("1ºA-Completo", grupo);
        Actividad act = actividad("ACT-MAT", asig,
                plaza("ACT-MAT-P1", asig, prof, aula, sg));

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                tramos, List.of(aula), List.of(asig), List.of(prof),
                List.of(grupo), List.of(sg), List.of(act));

        assertThat(problema.tramos()).hasSize(2); // el recreo se filtró
        assertThat(problema.aulas()).hasSize(1);
        assertThat(problema.asignaturas()).hasSize(1);
        assertThat(problema.profesores()).hasSize(1);
        assertThat(problema.grupos()).hasSize(1);
        assertThat(problema.subgrupos()).hasSize(1);
        assertThat(problema.actividades()).hasSize(1);
        assertThat(problema.restriccionesHorarias()).isEmpty();

        // La plaza referencia las MISMAS entidades top-level (por valor: records
        // con igualdad estructural), no copias divergentes de otro mapeo.
        es.yaroki.educhronos.solver.domain.Plaza plaza =
                problema.actividades().get(0).plazas().get(0);
        assertThat(plaza.asignatura()).isEqualTo(problema.asignaturas().get(0));
        assertThat(plaza.profesores()).containsExactlyElementsOf(problema.profesores());
        assertThat(plaza.aulaFija()).contains(problema.aulas().get(0));
        assertThat(plaza.subgrupos()).containsExactlyElementsOf(problema.subgrupos());
    }

    // ── Caso 2: coherencia subgrupo ↔ grupo ───────────────────────────────
    @Test
    void coherenciaSubgrupoGrupo() {
        Aula aula = aula("A5");
        Asignatura asig = asig("Mat", "Matemáticas");
        Profesor prof = prof("MAT8", "María Martínez");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = subgrupo("1ºA-Completo", grupo);
        Actividad act = actividad("ACT-MAT", asig,
                plaza("ACT-MAT-P1", asig, prof, aula, sg));

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(tramo(Dia.LUNES, 1, true)),
                List.of(aula), List.of(asig), List.of(prof),
                List.of(grupo), List.of(sg), List.of(act));

        // El grupo que cuelga del subgrupo es el mismo que el de la lista
        // top-level. Basta la igualdad por equals (no ==): domain.GrupoAdministrativo
        // es un record con igualdad ESTRUCTURAL, y S9 compara por equals.
        assertThat(problema.subgrupos().get(0).grupos())
                .containsExactlyElementsOf(problema.grupos());
    }

    // ── Caso 3: grupo y subgrupo huérfanos siguen en el top-level (D-B6-1) ─
    @Test
    void grupoYSubgrupoHuerfanosAparecenEnLasListasTopLevel() {
        Aula aula = aula("A5");
        Asignatura asig = asig("Mat", "Matemáticas");
        Profesor prof = prof("MAT8", "María Martínez");
        GrupoAdministrativo g1 = grupo("1ºA");
        GrupoAdministrativo g2 = grupo("1ºB"); // huérfano: ninguna actividad lo usa
        Subgrupo s1 = subgrupo("1ºA-Completo", g1);
        Subgrupo s2 = subgrupo("1ºB-Completo", g2); // huérfano

        Actividad act = actividad("ACT-MAT", asig,
                plaza("ACT-MAT-P1", asig, prof, aula, s1)); // solo referencia s1

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(tramo(Dia.LUNES, 1, true)),
                List.of(aula), List.of(asig), List.of(prof),
                List.of(g1, g2), List.of(s1, s2), List.of(act));

        // El catálogo top-level es exhaustivo: incluye entidades no referenciadas
        // por ninguna actividad. El mapper no las poda.
        assertThat(problema.grupos())
                .extracting(es.yaroki.educhronos.solver.domain.GrupoAdministrativo::codigo)
                .containsExactlyInAnyOrder("1ºA", "1ºB");
        assertThat(problema.subgrupos())
                .extracting(es.yaroki.educhronos.solver.domain.Subgrupo::codigo)
                .containsExactlyInAnyOrder("1ºA-Completo", "1ºB-Completo");
    }

    // ── Caso 4: restriccionesHorarias vacía por contrato (D28, centinela) ──
    @Test
    void restriccionesHorariasVaciaPorContrato() {
        // Centinela deliberado (deuda D28): no existe entidad JPA de restricciones
        // horarias, así que el 8º componente se pasa List.of(). El día que se
        // añada su mapeo, este test obliga a revisar el contrato conscientemente.
        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(tramo(Dia.LUNES, 1, true)),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(problema.restriccionesHorarias()).isEmpty();
    }

    // ── Caso 5: código de aula duplicado en la entrada → aborta ───────────
    @Test
    void codigoDeAulaDuplicadoAborta() {
        Aula a1 = aula("A5");
        Aula a2 = aula("A5"); // mismo código: colisión de clave natural

        // El índice usa Collectors.toMap de dos argumentos, que lanza
        // IllegalStateException ante clave duplicada: no se colapsa en silencio.
        assertThatThrownBy(() -> CatalogoMapper.aProblemaHorario(
                List.of(), List.of(a1, a2), List.of(), List.of(),
                List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Caso 6: plaza que referencia un profesor ausente → aborta ─────────
    @Test
    void plazaConProfesorAusenteAborta() {
        Aula aula = aula("A5");
        Asignatura asig = asig("Mat", "Matemáticas");
        Profesor fantasma = prof("GHOST", "No listado en el catálogo");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = subgrupo("1ºA-Completo", grupo);
        Actividad act = actividad("ACT-X", asig,
                plaza("ACT-X-P1", asig, fantasma, aula, sg));

        // GHOST no está en la lista de profesores → no entra al índice; el
        // resolver de aActividad aborta con el código huérfano en el mensaje.
        assertThatThrownBy(() -> CatalogoMapper.aProblemaHorario(
                List.of(tramo(Dia.LUNES, 1, true)),
                List.of(aula), List.of(asig), List.of(), // profesores: vacío
                List.of(grupo), List.of(sg), List.of(act)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GHOST");
    }

    // ── helpers de fixtures ───────────────────────────────────────────────

    private static TramoSemanal tramo(Dia dia, int orden, boolean esLectivo) {
        return new TramoSemanal(dia, LocalTime.of(8, 0), LocalTime.of(9, 0), esLectivo, orden, null);
    }

    private static Aula aula(String codigo) {
        return new Aula(codigo, TipoAula.ORDINARIA, null, null, null, null);
    }

    private static Asignatura asig(String codigo, String nombre) {
        return new Asignatura(codigo, nombre);
    }

    private static Profesor prof(String codigo, String nombre) {
        return new Profesor(codigo, nombre);
    }

    private static GrupoAdministrativo grupo(String codigo) {
        return new GrupoAdministrativo(codigo, null, TipoGrupo.ORDINARIO, null);
    }

    private static Subgrupo subgrupo(String codigoSubgrupo, GrupoAdministrativo grupo) {
        return new Subgrupo(codigoSubgrupo, Set.of(grupo));
    }

    /** Plaza JPA con aula fija (rama A del XOR), un profesor y un subgrupo. */
    private static Plaza plaza(
            String codigo, Asignatura asignatura, Profesor profesor, Aula aulaFija, Subgrupo subgrupo) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(Set.of(profesor));
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(Set.of(subgrupo));
        return plaza;
    }

    private static Actividad actividad(String codigo, Asignatura asignatura, Plaza... plazas) {
        Actividad actividad = new Actividad();
        actividad.setCodigo(codigo);
        actividad.setAsignatura(asignatura);
        actividad.setRepeticionesPorSemana(1);
        actividad.setDuracionTramos(1);
        actividad.setPatronTemporal(PatronTemporal.NEUTRA);
        actividad.setPlazas(List.of(plazas));
        return actividad;
    }
}
