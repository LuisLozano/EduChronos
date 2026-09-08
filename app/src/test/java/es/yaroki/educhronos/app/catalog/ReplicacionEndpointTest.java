package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;
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
 *
 * <p>El mismo fixture sirve al DELETE (Bloque S140, C-alta-reversible, T12–T18) sin tocarlo:
 * lo que el deshacer necesita medir es que el POST se pueda revertir sobre las tres formas a
 * la vez, y el {@code -Completo} sin plazas —el caso que descuadra el parte— ya está ahí.
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
     * (T21) Cada vía nombra los ESPEJOS de los originales que contiene, y solo esos (Bloque
     * S141). Es el enlace espejo → bloque sin el cual el plan no se puede decidir desde un
     * cliente: {@code gruposActuales} nombra GRUPOS y {@code subgruposACrear} es la lista
     * entera sin atar a ningún bloque, así que quien reciba el plan no sabría qué espejos
     * cubre cada bloque de reparto —que es exactamente el conjunto que el {@code POST} exige
     * cubrir sin sobrar ni faltar—.
     *
     * <p>Las dos mitades son la misma regla vista por sus dos caras y ninguna sobra: la vía
     * {@code BLOQ-REP-P1} lleva {@code 1ºA-Rep}, del hermano, y nombra su espejo; la
     * {@code -P2} lleva {@code 1ºB-Rep}, que no es del hermano, y va VACÍA. Cae si alguien
     * proyecta los espejos del BLOQUE en todas sus vías —con lo que la pantalla ofrecería
     * decidir sobre vías donde el original no está— o si los deriva de {@code gruposActuales},
     * que no distingue qué subgrupo de ese grupo está en la plaza.
     *
     * <p>La tercera línea fija que el campo se rellena TAMBIÉN en los replicados, donde es
     * informativo: sale del mismo camino de proyección y no de una rama condicional.
     */
    @Test
    void t21_cadaViaNombraLosEspejosDeSusPropiosOriginales() throws Exception {
        mockMvc.perform(get("/api/grupos/" + nuevoId + "/replicacion").param("hermano", "1ºA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reparto[0].vias[0].espejos", contains("1ºE-Rep")))
                .andExpect(jsonPath("$.reparto[0].vias[1].espejos", hasSize(0)))
                .andExpect(jsonPath("$.replicados[0].vias[0].espejos", contains("1ºE-Opt1")));
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

    /**
     * (T11) Con DOS bloques de reparto distintos tocando al hermano, una asignación que nombra
     * el espejo de un subgrupo de A y una vía de B → 400 y CERO subgrupos creados. La otra
     * asignación del cuerpo es correcta, así que el único motivo posible del rechazo es el
     * cruce: cae si la comprobación se relaja a "pertenece a algún bloque de reparto", que era
     * el comportamiento anterior y dejaba al grupo nuevo entrando por una vía de otra actividad.
     */
    @Test
    void t11_plazaDeOtroBloqueDeReparto_400YCeroEscrituras() throws Exception {
        long[] viasB = segundoBloqueDeReparto();
        long antes = subgrupoRepository.count();

        mockMvc.perform(post("/api/grupos/" + nuevoId + "/replicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(asignacion("1ºE-Rep", viasB[0]),      // espejo de A → vía de B
                                asignacion("1ºE-Rep2", viasB[0]))))         // ésta sí es correcta
                .andExpect(status().isBadRequest());
        entityManager.clear();

        assertThat(subgrupoRepository.count()).isEqualTo(antes);
    }

    // ──────────────────────────────── la plaza compartida ordinario + PDC (S140)

    /**
     * (T19) Una actividad de UNA plaza cuya población son DOS grupos —el ordinario y su PDC, la
     * forma de {@code EF-3ºA+3ºADi}, 30 de las 219 del centro— NO es un bloque, y un bloque de
     * dos plazas reporta DOS vías.
     *
     * <p>Las dos mitades del mismo defecto y por eso van juntas: {@code Actividad.plazas} es una
     * bolsa que el {@code left join fetch} de {@code p.subgrupos} infla, así que
     * {@code getPlazas()} repetía cada plaza una vez por subgrupo. Contarla hacía medir 2 a la
     * actividad de plaza única —se colaba como bloque— y proyectarla daba 4 vías donde hay 2.
     * Cae si se quita la deduplicación de {@code analizar}.
     */
    @Test
    void t19_plazaUnicaCompartidaConElPdc_niEsBloqueNiDuplicaVias() throws Exception {
        sembrarActividadCompartidaConPdc();

        mockMvc.perform(get("/api/grupos/" + nuevoId + "/replicacion").param("hermano", "1ºA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replicados[*].actividad",
                        containsInAnyOrder("BLOQ-REPL")))
                .andExpect(jsonPath("$.reparto[*].actividad", containsInAnyOrder("BLOQ-REP")))
                .andExpect(jsonPath("$.replicados[0].vias", hasSize(2)))
                .andExpect(jsonPath("$.reparto[0].vias", hasSize(2)));
    }

    /**
     * (T20) Tras replicar con una actividad de plaza única compartida con el PDC, el espejo del
     * {@code -Completo} sigue teniendo CERO plazas, que es lo que promete el Javadoc de clase.
     *
     * <p>Es la cara CARA del mismo defecto: al colarse esa actividad como bloque salía
     * REPLICADO —todas sus "vías" son la misma plaza, luego todas cubren U— y
     * {@code cablearReplicados} metía ahí el espejo. El grupo nuevo acababa en la clase de
     * Educación Física del hermano y su PDC, sin error ni aviso. {@code T10} no lo veía porque
     * su {@code ACT-UNICA} tiene un solo subgrupo.
     *
     * <p><b>NADA lee las plazas antes del POST, y es deliberado.</b> Leer
     * {@code actividadRepository.findAll()} para capturar un "antes" inicializa
     * {@code Actividad.plazas} LIMPIA en el contexto de persistencia —una consulta sin join a
     * subgrupos—, y el {@code left join fetch} posterior ya no repuebla una colección
     * inicializada: el POST no vería la bolsa inflada y el test pasaría con la corrección y sin
     * ella. Medido: con ese "antes" delante, la mutación que quita la deduplicación NO tumbaba
     * este test. Por eso el estado esperado se escribe literal.
     */
    @Test
    void t20_replicarConPlazaCompartidaConElPdc_noCableaElCompleto() throws Exception {
        long compartida = sembrarActividadCompartidaConPdc();

        replicar(asignacion("1ºE-Rep", repP1Id));

        assertThat(subgrupoRepository.contarPlazas(idDe("1ºE-Completo"))).isZero();
        assertThat(subgruposDePlaza(compartida))
                .containsExactlyInAnyOrder("1ºA-Completo", "1ºADi-Completo");
    }

    /**
     * Una actividad de UNA plaza poblada por el hermano Y su PDC, que es la forma real que el
     * fixture de {@link #setUp} no tiene: su {@code ACT-UNICA} lleva un solo subgrupo, y por eso
     * la bolsa no se inflaba y el defecto no se veía. Devuelve el id de esa plaza.
     */
    private long sembrarActividadCompartidaConPdc() {
        GrupoAdministrativo hermano = grupoRepository.findByCodigo("1ºA").orElseThrow();
        Nivel eso1 = nivelRepository.findByCodigo("1ESO").orElseThrow();
        GrupoAdministrativo pdc = grupoRepository.save(new GrupoAdministrativo(
                "1ºADi", eso1, TipoGrupo.DIVERSIFICACION_PDC, hermano));
        Asignatura mat = asignaturaRepository.findByCodigo("Mat").orElseThrow();
        Aula aula = aulaRepository.findByCodigo("A1").orElseThrow();

        Actividad ef = new Actividad("EF-1ºA+1ºADi", mat, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza plaza = ef.agregarPlaza("EF-1ºA+1ºADi-P1", mat, aula, Set.of(), Set.of(),
                Set.of(subgrupoRepository.findByCodigo("1ºA-Completo").orElseThrow(),
                        sub("1ºADi-Completo", pdc)));
        actividadRepository.save(ef);
        entityManager.flush();
        long id = plaza.getId();
        entityManager.clear();
        return id;
    }

    // ────────────────────────────────────────────────────────── deshacer (S140)

    /**
     * (T12) Ciclo POST → DELETE: las TRES tablas que la replicación toca —{@code subgrupo},
     * {@code subgrupo_grupo} y {@code plaza_subgrupo}— vuelven al recuento EXACTO que tenían
     * antes del POST. Las tres y no solo la primera: borrar los subgrupos sin soltarlos de las
     * plazas dejaría filas en {@code plaza_subgrupo} —y con {@code foreign_keys=ON} ni siquiera
     * llegaría, porque esa FK es NO ACTION—, y soltarlos sin borrarlos dejaría subgrupos
     * huérfanos con su fila de población viva.
     *
     * <p>Las aserciones intermedias existen para que el test no pueda pasar en vacío: si el
     * POST dejara de escribir, el "vuelve a como estaba" sería cierto por no haber hecho nada.
     */
    @Test
    void t12_cicloPostDelete_devuelveLasTresTablasASuRecuentoPrevio() throws Exception {
        long subgruposAntes = contar("subgrupo");
        long poblacionAntes = contar("subgrupo_grupo");
        long cableadoAntes = contar("plaza_subgrupo");

        replicar(asignacion("1ºE-Rep", repP1Id));
        assertThat(contar("subgrupo")).isGreaterThan(subgruposAntes);
        assertThat(contar("subgrupo_grupo")).isGreaterThan(poblacionAntes);
        assertThat(contar("plaza_subgrupo")).isGreaterThan(cableadoAntes);

        deshacer().andExpect(status().isOk());

        assertThat(contar("subgrupo")).isEqualTo(subgruposAntes);
        assertThat(contar("subgrupo_grupo")).isEqualTo(poblacionAntes);
        assertThat(contar("plaza_subgrupo")).isEqualTo(cableadoAntes);
    }

    /**
     * (T13) El parte nombra CUATRO subgrupos borrados y solo TRES descableados, y esa
     * diferencia es el punto: el espejo del {@code -Completo} nace sin plazas (la actividad de
     * una sola plaza no se toca), así que se borra sin soltar nada. Cae si el parte se deriva
     * de las plazas —perdería el {@code -Completo}— o si cuenta subgrupos en vez de PARES
     * (plaza, subgrupo).
     */
    @Test
    void t13_elParteListaLosPares_yNoCoincideConLosSubgruposBorrados() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));

        deshacer()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupo").value("1ºE"))
                .andExpect(jsonPath("$.subgruposBorrados", containsInAnyOrder(
                        "1ºE-Completo", "1ºE-Opt1", "1ºE-Opt2", "1ºE-Rep")))
                .andExpect(jsonPath("$.plazasDescableadas", hasSize(3)))
                .andExpect(jsonPath("$.plazasDescableadas[*].subgrupo", containsInAnyOrder(
                        "1ºE-Opt1", "1ºE-Opt2", "1ºE-Rep")))
                .andExpect(jsonPath("$.plazasDescableadas[*].actividad", containsInAnyOrder(
                        "BLOQ-REPL", "BLOQ-REPL", "BLOQ-REP")));
    }

    /**
     * (T14) G2(a): un subgrupo del grupo que además pertenece a OTRO grupo → 409 y CERO
     * borrados. No es población que este grupo posea —el caso real es la "Lectura B" de
     * Bachillerato, con alumnos de varios grupos del nivel— y borrarlo se llevaría por delante
     * al otro. Cae si la guarda no mira cuántos grupos tiene cada subgrupo.
     */
    @Test
    void t14_subgrupoCompartidoConOtroGrupo_409YCeroBorrados() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));
        long subgruposAntes = contar("subgrupo");
        long cableadoAntes = contar("plaza_subgrupo");

        Subgrupo opt1 = subgrupoRepository.findByCodigo("1ºE-Opt1").orElseThrow();
        opt1.actualizar(opt1.getCodigo(), Set.of(
                grupoRepository.findByCodigo("1ºE").orElseThrow(),
                grupoRepository.findByCodigo("1ºB").orElseThrow()));
        subgrupoRepository.save(opt1);
        entityManager.flush();
        entityManager.clear();

        deshacer().andExpect(status().isConflict());
        entityManager.clear();

        assertThat(contar("subgrupo")).isEqualTo(subgruposAntes);
        assertThat(contar("plaza_subgrupo")).isEqualTo(cableadoAntes);
    }

    /**
     * (T15) G3: una actividad afectada con una fila en {@code sesion} → 409 y CERO borrados. Es
     * la misma guarda ROMA del alta y por la misma razón: si el catálogo del grupo se va a
     * mover, un horario generado sobre él deja de describir lo que hay. Cae si el deshacer no
     * llama a {@code exigirSinDependientes} antes de escribir.
     */
    @Test
    void t15_actividadAfectadaConSesion_409YCeroBorrados() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));
        long subgruposAntes = contar("subgrupo");
        long cableadoAntes = contar("plaza_subgrupo");
        sembrarSesionSobre(replP1Id);

        deshacer().andExpect(status().isConflict());
        entityManager.clear();

        assertThat(contar("subgrupo")).isEqualTo(subgruposAntes);
        assertThat(contar("plaza_subgrupo")).isEqualTo(cableadoAntes);
    }

    /**
     * (T16) G4: una plaza cuya ÚNICA población es un subgrupo del grupo hace reventar el
     * deshacer con {@link IllegalStateException} nombrando la plaza, en vez de dejarla sin
     * subgrupos. Es un ASERTO DE PRODUCCIÓN, no una regla de negocio, y por eso se asevera
     * contra el servicio y no por la red: un 500 es exactamente lo que debe verse.
     *
     * <p>El estado se monta a mano porque la replicación no puede producirlo: toda plaza donde
     * entra un espejo lleva ya al original del hermano, que sobrevive al borrado.
     */
    @Test
    void t16_plazaQueQuedariaSinSubgrupos_revientaConAsertoYNombraLaPlaza() {
        GrupoAdministrativo nuevo = grupoRepository.findById(nuevoId).orElseThrow();
        Asignatura mat = asignaturaRepository.findByCodigo("Mat").orElseThrow();
        Aula aula = aulaRepository.findByCodigo("A1").orElseThrow();

        Actividad solo = new Actividad("ACT-SOLO", mat, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza plaza = solo.agregarPlaza("ACT-SOLO-P1", mat, aula, Set.of(), Set.of(),
                Set.of(sub("1ºE-Solo", nuevo)));
        actividadRepository.save(solo);
        entityManager.flush();
        long plazaId = plaza.getId();
        entityManager.clear();

        assertThatThrownBy(() -> service.deshacer(nuevoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.valueOf(plazaId))
                .hasMessageContaining("ACT-SOLO");
    }

    /**
     * (T17) El DELETE es IDEMPOTENTE: sobre un grupo ya pelado devuelve 200 con el parte vacío,
     * no 404 ni 409. Llamarlo dos veces no puede ser un error, y el 404 se reserva para lo
     * único que sí lo es (que el grupo no exista, T18).
     */
    @Test
    void t17_segundoDelete_200ConParteVacio() throws Exception {
        replicar(asignacion("1ºE-Rep", repP1Id));
        deshacer().andExpect(status().isOk());

        deshacer()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupo").value("1ºE"))
                .andExpect(jsonPath("$.subgruposBorrados").isEmpty())
                .andExpect(jsonPath("$.plazasDescableadas").isEmpty());
    }

    /** (T18) Un id de grupo inexistente → 404, que es el único 404 que este DELETE produce. */
    @Test
    void t18_grupoInexistente_404() throws Exception {
        mockMvc.perform(delete("/api/grupos/999999/replicacion"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────────────────────────────────── helpers

    /**
     * Un SEGUNDO bloque de reparto ({@code BLOQ-REP2}) que también toca al hermano, montado
     * dentro del test que lo necesita y no en {@link #setUp}: sumarlo al fixture común obligaría
     * a todos los demás POST a traer una asignación más, y lo que ellos miden no es esto.
     * Devuelve los ids de sus dos vías.
     */
    private long[] segundoBloqueDeReparto() {
        GrupoAdministrativo hermano = grupoRepository.findByCodigo("1ºA").orElseThrow();
        GrupoAdministrativo otro = grupoRepository.findByCodigo("1ºB").orElseThrow();
        Asignatura mat = asignaturaRepository.findByCodigo("Mat").orElseThrow();
        Aula aula = aulaRepository.findByCodigo("A1").orElseThrow();

        Actividad rep2 = new Actividad("BLOQ-REP2", null, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza p1 = rep2.agregarPlaza("BLOQ-REP2-P1", mat, aula, Set.of(), Set.of(),
                Set.of(sub("1ºA-Rep2", hermano)));
        Plaza p2 = rep2.agregarPlaza("BLOQ-REP2-P2", mat, aula, Set.of(), Set.of(),
                Set.of(sub("1ºB-Rep2", otro)));
        actividadRepository.save(rep2);
        entityManager.flush();
        long[] vias = {p1.getId(), p2.getId()};
        entityManager.clear();
        return vias;
    }

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

    /** DELETE del sub-recurso, seguido de flush+clear para leer la base y no la sesión. */
    private ResultActions deshacer() throws Exception {
        ResultActions resultado = mockMvc.perform(
                delete("/api/grupos/" + nuevoId + "/replicacion"));
        entityManager.flush();
        entityManager.clear();
        return resultado;
    }

    /**
     * Filas de una tabla, contadas por SQL nativo. Las tres que importan al deshacer
     * ({@code subgrupo_grupo}, {@code plaza_subgrupo}) son join tables sin entidad propia: no
     * hay repositorio que las cuente y el {@code count()} de JPA no las ve.
     */
    private long contar(String tabla) {
        return ((Number) entityManager.getEntityManager()
                .createNativeQuery("select count(*) from " + tabla)
                .getSingleResult()).longValue();
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
