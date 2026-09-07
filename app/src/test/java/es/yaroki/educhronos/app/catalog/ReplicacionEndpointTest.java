package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.ActividadService;
import es.yaroki.educhronos.app.service.ReplicacionService;
import es.yaroki.educhronos.app.web.ReplicacionController;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del sub-recurso {@code /api/grupos/{id}/replicacion} (Fase 8, Bloque
 * S139, C-replicación-alta) POR LA RED ({@code standaloneSetup} + {@code ReplicacionService}
 * real sobre {@code @DataJpaTest}), molde de {@code PdcEndpointTest}.
 *
 * <p>El fixture de {@link #setUp} reproduce EN PEQUEÑO las tres formas que la base real tiene
 * (medidas en S139 sobre el centro completo), porque la clasificación solo tiene sentido si
 * las tres conviven:
 * <ul>
 *   <li>{@code BLOQ-REPL}: bloque REPLICADO de dos vías. Cada vía cubre {1ºA, 1ºB}, así que
 *       toda plaza es exactamente U y no hay nada que decidir.
 *   <li>{@code BLOQ-REP}: bloque de REPARTO. Una vía cubre {1ºA} y la otra {1ºB}: ambas son
 *       subconjuntos ESTRICTOS de U = {1ºA, 1ºB}.
 *   <li>{@code ACT-UNICA}: actividad de UNA plaza. No se toca, y su espejo nace huérfano.
 * </ul>
 * El hermano es {@code 1ºA} y el grupo nuevo {@code 1ºE}; {@code 1ºB} existe solo para que U
 * tenga más de un elemento y la distinción replicado/reparto sea observable.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReplicacionService.class, ActividadService.class})
class ReplicacionEndpointTest {

    @Autowired private ReplicacionService service;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;
    private long nuevoId;
    private long replP1Id;
    private long replP2Id;
    private long repP1Id;
    private long repP2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReplicacionController(service)).build();

        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo hermano = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo otro = grupoRepository.save(
                new GrupoAdministrativo("1ºB", eso1, TipoGrupo.ORDINARIO, null));
        nuevoId = grupoRepository.save(
                new GrupoAdministrativo("1ºE", eso1, TipoGrupo.ORDINARIO, null)).getId();

        Subgrupo aCompleto = sub("1ºA-Completo", hermano);
        Subgrupo aOpt1 = sub("1ºA-Opt1", hermano);
        Subgrupo aOpt2 = sub("1ºA-Opt2", hermano);
        Subgrupo aRep = sub("1ºA-Rep", hermano);
        Subgrupo bOpt1 = sub("1ºB-Opt1", otro);
        Subgrupo bOpt2 = sub("1ºB-Opt2", otro);
        Subgrupo bRep = sub("1ºB-Rep", otro);

        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matematicas"));
        Aula aula = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        // REPLICADO: las dos vías cubren {1ºA, 1ºB}.
        Actividad repl = new Actividad("BLOQ-REPL", null, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza replP1 = repl.agregarPlaza("BLOQ-REPL-P1", mat, aula, Set.of(), Set.of(),
                Set.of(aOpt1, bOpt1));
        Plaza replP2 = repl.agregarPlaza("BLOQ-REPL-P2", mat, aula, Set.of(), Set.of(),
                Set.of(aOpt2, bOpt2));
        actividadRepository.save(repl);

        // REPARTE: una vía cubre {1ºA} y la otra {1ºB}; ambas son subconjuntos estrictos de U.
        Actividad rep = new Actividad("BLOQ-REP", null, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza repP1 = rep.agregarPlaza("BLOQ-REP-P1", mat, aula, Set.of(), Set.of(), Set.of(aRep));
        Plaza repP2 = rep.agregarPlaza("BLOQ-REP-P2", mat, aula, Set.of(), Set.of(), Set.of(bRep));
        actividadRepository.save(rep);

        // Una sola plaza: intocable.
        Actividad unica = new Actividad("ACT-UNICA", mat, 1, 1, PatronTemporal.NEUTRA, false);
        unica.agregarPlaza("ACT-UNICA-P1", mat, aula, Set.of(), Set.of(), Set.of(aCompleto));
        actividadRepository.save(unica);

        entityManager.flush();
        replP1Id = replP1.getId();
        replP2Id = replP2.getId();
        repP1Id = repP1.getId();
        repP2Id = repP2.getId();
        entityManager.clear();
    }

    // ─────────────────────────────────────────────────────────── clasificación y sufijo

    /**
     * (T1) El bloque cuyas vías cubren subconjuntos ESTRICTOS de U sale en {@code reparto} y NO
     * en {@code replicados}. Cae si el predicado "exactamente U" se relaja a "contenido en U",
     * que es cierto por construcción y mandaría los dos bloques a {@code replicados}.
     */
    @Test
    void t1_bloqueDeReparto_apareceEnRepartoYNoEnReplicados() throws Exception {
        mockMvc.perform(get("/api/grupos/" + nuevoId + "/replicacion").param("hermano", "1ºA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reparto[*].actividad", containsInAnyOrder("BLOQ-REP")))
                .andExpect(jsonPath("$.replicados[*].actividad", containsInAnyOrder("BLOQ-REPL")));
    }

    /**
     * (T2) El sufijo se recorta por el PREFIJO del hermano, no por el primer guion: con hermano
     * {@code 1B-A} —cuyo propio código lleva guion— el espejo de {@code 1B-A-DTec} es
     * {@code 1B-E-DTec}. Cae si se usa "lo que sigue al primer guion", que daría
     * {@code 1B-E-A-DTec}.
     */
    @Test
    void t2_hermanoConGuionEnSuCodigo_elSufijoSeRecortaPorElPrefijo() throws Exception {
        Nivel bach = nivelRepository.save(new Nivel("1BACH", 5));
        GrupoAdministrativo ba = grupoRepository.save(
                new GrupoAdministrativo("1B-A", bach, TipoGrupo.ORDINARIO, null));
        long beId = grupoRepository.save(
                new GrupoAdministrativo("1B-E", bach, TipoGrupo.ORDINARIO, null)).getId();
        sub("1B-A-DTec", ba);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/grupos/" + beId + "/replicacion").param("hermano", "1B-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subgruposACrear", containsInAnyOrder("1B-E-DTec")));
    }

    // ─────────────────────────────────────────────────────────────────────── cableado

    /**
     * (T3) En un bloque REPLICADO de dos vías, el espejo de un optativo queda en EXACTAMENTE
     * UNA plaza, y es la que contenía a su original. Cae si se cablea en todas las vías del
     * bloque, que metería al grupo nuevo en una optativa que su hermano no cursa.
     */
    @Test
    void t3_bloqueReplicado_elEspejoQuedaSoloEnLaPlazaDeSuOriginal() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));

        assertThat(subgruposDePlaza(replP1Id)).contains("1ºE-Opt1").doesNotContain("1ºE-Opt2");
        assertThat(subgruposDePlaza(replP2Id)).contains("1ºE-Opt2").doesNotContain("1ºE-Opt1");
        assertThat(subgrupoRepository.contarPlazas(idDe("1ºE-Opt1"))).isEqualTo(1L);
    }

    /**
     * (T6) {@code plaza: null} es una decisión legítima —"el grupo nuevo no tiene alumnos en esa
     * vía"—, no un error: 201, y el espejo existe con CERO plazas. Cae si se trata como error.
     */
    @Test
    void t6_asignacionConPlazaNull_201YEspejoSinPlazas() throws Exception {
        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignacion("1ºE-Rep", null))))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();

        assertThat(subgrupoRepository.findByCodigo("1ºE-Rep")).isPresent();
        assertThat(subgrupoRepository.contarPlazas(idDe("1ºE-Rep"))).isZero();
    }

    /**
     * (T9) Una actividad de UNA SOLA PLAZA conserva EXACTAMENTE los subgrupos que tenía: el
     * conjunto entero, no solo su tamaño. Cae si se quita el filtro "más de una plaza" y el
     * espejo del {@code -Completo} se cuela en el troncal del hermano.
     */
    @Test
    void t9_actividadDeUnaPlaza_conservaExactamenteSusSubgrupos() throws Exception {
        long unicaP1 = plazaDe("ACT-UNICA");
        Set<String> antes = subgruposDePlaza(unicaP1);

        replicar(asignacion("1ºE-Rep", repP1Id));

        assertThat(subgruposDePlaza(unicaP1)).isEqualTo(antes).containsExactly("1ºA-Completo");
    }

    /**
     * (T10) El espejo del {@code -Completo} nace con ≥1 grupo y CERO plazas (paso intencionado:
     * el troncal no se replica), y el grupo nuevo acaba con tantos subgrupos como el hermano.
     */
    @Test
    void t10_espejoCompleto_conGrupoYSinPlazas_yElGrupoNuevoIgualaAlHermano() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));

        Subgrupo completo = subgrupoRepository.findByCodigo("1ºE-Completo").orElseThrow();
        assertThat(completo.getGrupos()).hasSize(1);
        assertThat(subgrupoRepository.contarPlazas(completo.getId())).isZero();

        long hermanoId = grupoRepository.findByCodigo("1ºA").orElseThrow().getId();
        assertThat(grupoRepository.contarSubgrupos(nuevoId))
                .isEqualTo(grupoRepository.contarSubgrupos(hermanoId));
    }

    // ───────────────────────────────────────────────────────────────────── rechazos

    /**
     * (T4) Dos asignaciones para el mismo espejo → 400, y CERO subgrupos creados: la unicidad es
     * lo que impide que un espejo acabe en dos plazas de la misma actividad (I2), invariante que
     * esta ruta no hereda de {@code ActividadService} y que el esquema tampoco impone.
     */
    @Test
    void t4_espejoAsignadoDosVeces_400YCeroEscrituras() throws Exception {
        long antes = subgrupoRepository.count();

        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignacion("1ºE-Rep", repP1Id),
                                asignacion("1ºE-Rep", repP2Id))))
                .andExpect(status().isBadRequest());
        entityManager.clear();

        assertThat(subgrupoRepository.count()).isEqualTo(antes);
    }

    /**
     * (T5) Una actividad afectada con una fila en {@code sesion} → 409, y CERO escrituras. Cae
     * si no se llama a {@code exigirSinDependientes} antes de escribir.
     */
    @Test
    void t5_actividadAfectadaConSesion_409YCeroEscrituras() throws Exception {
        long antes = subgrupoRepository.count();
        sembrarSesionSobre(replP1Id);

        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignacion("1ºE-Rep", repP1Id))))
                .andExpect(status().isConflict());
        entityManager.clear();

        assertThat(subgrupoRepository.count()).isEqualTo(antes);
    }

    /**
     * (T7) Falta la asignación de un bloque de reparto → 400: el reparto no se adivina.
     */
    @Test
    void t7_asignacionDeRepartoAusente_400() throws Exception {
        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hermano\":\"1ºA\",\"asignaciones\":[]}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * (T8) Un subgrupo del hermano que no empieza por {@code {hermano}-} aborta el alta ENTERA
     * con 400 y CERO escrituras, en vez de inventarle un sufijo o saltárselo en silencio.
     */
    @Test
    void t8_subgrupoDelHermanoSinElPrefijo_400YCeroEscrituras() throws Exception {
        GrupoAdministrativo hermano = grupoRepository.findByCodigo("1ºA").orElseThrow();
        sub("RARO", hermano);
        entityManager.flush();
        entityManager.clear();
        long antes = subgrupoRepository.count();

        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignacion("1ºE-Rep", repP1Id))))
                .andExpect(status().isBadRequest());
        entityManager.clear();

        assertThat(subgrupoRepository.count()).isEqualTo(antes);
    }

    // ──────────────────────────────────────────────────────────────────────── helpers

    private Subgrupo sub(String codigo, GrupoAdministrativo grupo) {
        return subgrupoRepository.save(new Subgrupo(codigo, Set.of(grupo)));
    }

    /** POST que debe salir 201, seguido de flush+clear para leer la base y no la sesión. */
    private void replicar(String... asignaciones) throws Exception {
        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignaciones)))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
    }

    private static String body(String... asignaciones) {
        return "{\"hermano\":\"1ºA\",\"asignaciones\":[" + String.join(",", asignaciones) + "]}";
    }

    private static String asignacion(String subgrupo, Long plaza) {
        return "{\"subgrupo\":\"" + subgrupo + "\",\"plaza\":" + plaza + "}";
    }

    /** Códigos de los subgrupos de una plaza, releídos de la base por su actividad. */
    private Set<String> subgruposDePlaza(long plazaId) {
        return actividadRepository.findAll().stream()
                .flatMap(a -> a.getPlazas().stream())
                .filter(p -> p.getId() == plazaId)
                .flatMap(p -> p.getSubgrupos().stream())
                .map(Subgrupo::getCodigo)
                .collect(Collectors.toSet());
    }

    private long plazaDe(String codigoActividad) {
        return actividadRepository.findByCodigo(codigoActividad).orElseThrow()
                .getPlazas().get(0).getId();
    }

    private long idDe(String codigoSubgrupo) {
        return subgrupoRepository.findByCodigo(codigoSubgrupo).orElseThrow().getId();
    }

    /** Una {@code Sesion} sobre la plaza dada: el dependiente que dispara el 409. */
    private void sembrarSesionSobre(long plazaId) {
        Aula aula = aulaRepository.findByCodigo("A1").orElseThrow();
        TramoSemanal tramo = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        HorarioGenerado horario = horarioRepository.save(
                new HorarioGenerado("H", Instant.now(), "OPTIMAL", 0.0, 0.0));
        Plaza plaza = actividadRepository.findAll().stream()
                .flatMap(a -> a.getPlazas().stream())
                .filter(p -> p.getId() == plazaId)
                .findFirst().orElseThrow();
        sesionRepository.save(new Sesion(horario, plaza, 1, tramo, aula));
        entityManager.flush();
        entityManager.clear();
    }
}
