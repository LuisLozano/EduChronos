package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link CatalogoMapper#aActividad} y sus helpers (Fase 6,
 * Bloque 5). Puros: las entidades JPA se construyen en memoria con {@code new} +
 * setters, sin contexto Spring ni BD; se invoca el mapper y se verifica el
 * {@code domain.Actividad} resultante.
 *
 * <p>Vive en el paquete {@code app.catalog} (no {@code app.mapper}, donde están
 * los demás tests de mapper) porque {@code Actividad} y {@code Plaza} solo
 * exponen constructor {@code protected}: instanciarlas con {@code new}+setters
 * solo es posible desde su propio paquete. El estilo es el de
 * {@code CatalogoMapperSubgrupoTest}.
 *
 * <p>Regla de construcción de índices (aprendizaje de proceso de B4): los índices
 * {@code Map<String, ...>} que recibe {@code aActividad} se construyen CONSUMIENDO
 * LA SALIDA DEL PROPIO MAPPER ({@code aAsignatura}/{@code aProfesor}/{@code aAula}/
 * {@code aSubgrupo}), no fabricando tipos de {@code solver.domain} con {@code new}.
 */
class CatalogoMapperActividadTest {

    // ── Caso 1: actividad con asignatura propia y una plaza ordinaria ──
    @Test
    void aActividad_conAsignaturaYPlazaOrdinaria() {
        Asignatura jpaMat = new Asignatura("Mat", "Matemáticas");
        Profesor jpaProf = new Profesor("MAT8", "María Martínez");
        Aula jpaAula = new Aula("A5", TipoAula.ORDINARIA, null, null, null, null);
        Subgrupo jpaSg = subgrupoDe("1ºA-Completo", "1ºA");

        Map<String, es.yaroki.educhronos.solver.domain.Asignatura> asignaturas =
                Map.of("Mat", CatalogoMapper.aAsignatura(jpaMat));
        Map<String, es.yaroki.educhronos.solver.domain.Profesor> profesores =
                Map.of("MAT8", CatalogoMapper.aProfesor(jpaProf));
        Map<String, es.yaroki.educhronos.solver.domain.Aula> aulas =
                Map.of("A5", CatalogoMapper.aAula(jpaAula));
        Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> subgrupos =
                indiceSubgrupo(jpaSg);

        Plaza jpaPlaza = plazaConAulaFija("ACT-MAT-P1", jpaMat, jpaProf, jpaAula, jpaSg);
        Actividad jpaActividad = new Actividad();
        jpaActividad.setCodigo("ACT-MAT");
        jpaActividad.setAsignatura(jpaMat);
        jpaActividad.setRepeticionesPorSemana(3);
        jpaActividad.setDuracionTramos(1);
        jpaActividad.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        jpaActividad.setPlazas(List.of(jpaPlaza));

        es.yaroki.educhronos.solver.domain.Actividad dominio =
                CatalogoMapper.aActividad(jpaActividad, asignaturas, profesores, aulas, subgrupos);

        assertThat(dominio.codigo()).isEqualTo("ACT-MAT");
        assertThat(dominio.asignatura()).isPresent();
        assertThat(dominio.asignatura().orElseThrow().codigo()).isEqualTo("Mat");
        assertThat(dominio.repeticionesPorSemana()).isEqualTo(3);
        assertThat(dominio.duracionTramos()).isEqualTo(1);
        assertThat(dominio.patronTemporal())
                .isEqualTo(es.yaroki.educhronos.solver.domain.PatronTemporal.DISTRIBUIDA);

        assertThat(dominio.plazas()).singleElement().satisfies(plaza -> {
            assertThat(plaza.asignatura().codigo()).isEqualTo("Mat");
            assertThat(plaza.profesores()).extracting(
                    es.yaroki.educhronos.solver.domain.Profesor::codigo).containsExactly("MAT8");
            assertThat(plaza.aulaFija()).isPresent();
            assertThat(plaza.aulaFija().orElseThrow().codigo()).isEqualTo("A5");
            assertThat(plaza.subgrupos()).extracting(
                    es.yaroki.educhronos.solver.domain.Subgrupo::codigo).containsExactly("1ºA-Completo");
        });
    }

    // ── Caso 2: actividad de bloque, sin asignatura propia ──
    @Test
    void aActividad_sinAsignaturaCadaPlazaLlevaLaSuya() {
        Asignatura jpaCyR = new Asignatura("CyR", "Cultura y Religión");
        Asignatura jpaOyD = new Asignatura("OyD", "Optativa de Diseño");
        Profesor jpaProfR = new Profesor("REL1", "Rosa Ruiz");
        Profesor jpaProfD = new Profesor("DIB2", "Diego Díaz");
        Aula jpaAulaR = new Aula("A5", TipoAula.ORDINARIA, null, null, null, null);
        Aula jpaAulaD = new Aula("A6", TipoAula.ORDINARIA, null, null, null, null);
        Subgrupo jpaSgR = subgrupoDe("1ºA-CyR", "1ºA");
        Subgrupo jpaSgD = subgrupoDe("1ºA-OyD", "1ºA");

        Map<String, es.yaroki.educhronos.solver.domain.Asignatura> asignaturas = Map.of(
                "CyR", CatalogoMapper.aAsignatura(jpaCyR),
                "OyD", CatalogoMapper.aAsignatura(jpaOyD));
        Map<String, es.yaroki.educhronos.solver.domain.Profesor> profesores = Map.of(
                "REL1", CatalogoMapper.aProfesor(jpaProfR),
                "DIB2", CatalogoMapper.aProfesor(jpaProfD));
        Map<String, es.yaroki.educhronos.solver.domain.Aula> aulas = Map.of(
                "A5", CatalogoMapper.aAula(jpaAulaR),
                "A6", CatalogoMapper.aAula(jpaAulaD));
        es.yaroki.educhronos.solver.domain.GrupoAdministrativo grupoDom =
                CatalogoMapper.aGrupo(new GrupoAdministrativo("1ºA", null, TipoGrupo.ORDINARIO, null));
        Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> subgrupos = Map.of(
                "1ºA-CyR", CatalogoMapper.aSubgrupo(jpaSgR, Map.of("1ºA", grupoDom)),
                "1ºA-OyD", CatalogoMapper.aSubgrupo(jpaSgD, Map.of("1ºA", grupoDom)));

        Plaza plazaR = plazaConAulaFija("ACT-BLK-R", jpaCyR, jpaProfR, jpaAulaR, jpaSgR);
        Plaza plazaD = plazaConAulaFija("ACT-BLK-D", jpaOyD, jpaProfD, jpaAulaD, jpaSgD);
        Actividad jpaActividad = new Actividad();
        jpaActividad.setCodigo("ACT-BLK");
        jpaActividad.setAsignatura(null);   // bloque: distinta asignatura por plaza
        jpaActividad.setPlazas(List.of(plazaR, plazaD));

        es.yaroki.educhronos.solver.domain.Actividad dominio =
                CatalogoMapper.aActividad(jpaActividad, asignaturas, profesores, aulas, subgrupos);

        assertThat(dominio.asignatura()).isEmpty();
        // Cada plaza SÍ lleva su asignatura (obligatoria en el dominio).
        assertThat(dominio.plazas())
                .extracting(p -> p.asignatura().codigo())
                .containsExactlyInAnyOrder("CyR", "OyD");
    }

    // ── Caso 3: traducción de patronTemporal (rama no-NEUTRA) ──
    @Test
    void aActividad_traducePatronTemporalNoNeutra() {
        Actividad jpaActividad = actividadMinima("ACT-PAT");
        jpaActividad.setPatronTemporal(PatronTemporal.AGRUPADA);

        es.yaroki.educhronos.solver.domain.Actividad dominio =
                CatalogoMapper.aActividad(jpaActividad, indiceAsig(), indiceProf(), indiceAula(), indiceSg());

        assertThat(dominio.patronTemporal())
                .isEqualTo(es.yaroki.educhronos.solver.domain.PatronTemporal.AGRUPADA);
    }

    // ── Caso 4: requiereTutor se propaga al dominio (Bloque 8.5-D2b-1, revoca D-B5-5) ──
    @Test
    void aActividad_propagaRequiereTutor() {
        // Par discriminante: sin la pata 'false' un mapeo que clavase 'true' pasaría.
        Actividad jpaConTutor = actividadMinima("ACT-CON-TUTOR");
        jpaConTutor.setRequiereTutor(true);
        es.yaroki.educhronos.solver.domain.Actividad dominioConTutor =
                CatalogoMapper.aActividad(jpaConTutor, indiceAsig(), indiceProf(), indiceAula(), indiceSg());
        assertThat(dominioConTutor.requiereTutor()).isTrue();

        Actividad jpaSinTutor = actividadMinima("ACT-SIN-TUTOR");
        jpaSinTutor.setRequiereTutor(false);
        es.yaroki.educhronos.solver.domain.Actividad dominioSinTutor =
                CatalogoMapper.aActividad(jpaSinTutor, indiceAsig(), indiceProf(), indiceAula(), indiceSg());
        assertThat(dominioSinTutor.requiereTutor()).isFalse();
    }

    // ── Caso 5: integridad referencial ──
    @Test
    void aActividad_abortaSiUnaPlazaReferenciaUnProfesorAusente() {
        Asignatura jpaMat = new Asignatura("Mat", "Matemáticas");
        Profesor jpaFantasma = new Profesor("GHOST", "No indexado");
        Aula jpaAula = new Aula("A5", TipoAula.ORDINARIA, null, null, null, null);
        Subgrupo jpaSg = subgrupoDe("1ºA-Completo", "1ºA");

        // El índice de profesores NO contiene 'GHOST'.
        Plaza jpaPlaza = plazaConAulaFija("ACT-X-P1", jpaMat, jpaFantasma, jpaAula, jpaSg);
        Actividad jpaActividad = new Actividad();
        jpaActividad.setCodigo("ACT-X");
        jpaActividad.setAsignatura(jpaMat);
        jpaActividad.setPlazas(List.of(jpaPlaza));

        assertThatThrownBy(() -> CatalogoMapper.aActividad(
                jpaActividad,
                Map.of("Mat", CatalogoMapper.aAsignatura(jpaMat)),
                Map.of(),                                        // profesores: vacío
                Map.of("A5", CatalogoMapper.aAula(jpaAula)),
                indiceSubgrupo(jpaSg)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GHOST");
    }

    // ── helpers de fixtures ──────────────────────────────────────────────

    /** Subgrupo JPA mono-grupo con un único grupo ordinario del código dado. */
    private static Subgrupo subgrupoDe(String codigoSubgrupo, String codigoGrupo) {
        GrupoAdministrativo grupo = new GrupoAdministrativo(codigoGrupo, null, TipoGrupo.ORDINARIO, null);
        return new Subgrupo(codigoSubgrupo, Set.of(grupo));
    }

    /** Plaza JPA con aula fija (rama A del XOR), un profesor y un subgrupo. */
    private static Plaza plazaConAulaFija(
            String codigo, Asignatura asignatura, Profesor profesor, Aula aulaFija, Subgrupo subgrupo) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(Set.of(profesor));
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(Set.of(subgrupo));
        return plaza;
    }

    // Fixture mínimo reutilizable (Mat/MAT8/A5/1ºA-Completo) para los casos que
    // solo ejercitan campos de la actividad, no la variedad de plazas.
    private static final Asignatura MAT = new Asignatura("Mat", "Matemáticas");
    private static final Profesor PROF = new Profesor("MAT8", "María Martínez");
    private static final Aula AULA = new Aula("A5", TipoAula.ORDINARIA, null, null, null, null);
    private static final Subgrupo SG = subgrupoDe("1ºA-Completo", "1ºA");

    private static Actividad actividadMinima(String codigo) {
        Actividad actividad = new Actividad();
        actividad.setCodigo(codigo);
        actividad.setAsignatura(MAT);
        actividad.setPlazas(List.of(plazaConAulaFija(codigo + "-P1", MAT, PROF, AULA, SG)));
        return actividad;
    }

    private static Map<String, es.yaroki.educhronos.solver.domain.Asignatura> indiceAsig() {
        return Map.of("Mat", CatalogoMapper.aAsignatura(MAT));
    }

    private static Map<String, es.yaroki.educhronos.solver.domain.Profesor> indiceProf() {
        return Map.of("MAT8", CatalogoMapper.aProfesor(PROF));
    }

    private static Map<String, es.yaroki.educhronos.solver.domain.Aula> indiceAula() {
        return Map.of("A5", CatalogoMapper.aAula(AULA));
    }

    private static Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> indiceSg() {
        return indiceSubgrupo(SG);
    }

    /** Índice {codigoSubgrupo → domain.Subgrupo} obtenido de la salida del mapper. */
    private static Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> indiceSubgrupo(Subgrupo jpaSg) {
        GrupoAdministrativo grupo = jpaSg.getGrupos().iterator().next();
        es.yaroki.educhronos.solver.domain.GrupoAdministrativo grupoDom = CatalogoMapper.aGrupo(grupo);
        return Map.of(jpaSg.getCodigo(),
                CatalogoMapper.aSubgrupo(jpaSg, Map.of(grupo.getCodigo(), grupoDom)));
    }
}
