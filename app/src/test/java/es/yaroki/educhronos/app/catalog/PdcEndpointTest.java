package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.PdcService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.PdcController;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del sub-recurso PDC {@code /api/grupos/{idPadre}/pdc} (Fase 8,
 * Bloque 8.5-D1). Ejerce alta/consulta/borrado POR LA RED ({@code standaloneSetup} +
 * {@code PdcService} real sobre {@code @DataJpaTest}), con asertos DISCRIMINANTES: cada
 * uno cae si se retira la regla que protege. Da de alta un nivel y un grupo ordinario
 * padre ("3A" sobre "1ESO") en {@link #setUp}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PdcService.class)
class PdcEndpointTest {

    @Autowired private PdcService service;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;
    private long padreId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PdcController(service)).build();
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        padreId = grupoRepository.save(
                new GrupoAdministrativo("3A", nivel, TipoGrupo.ORDINARIO, null)).getId();
    }

    // ------------------------------------------------------------------ camino feliz

    @Test
    void alta_devuelve201YGrupoDtoDelPdc_conNivelHeredado() throws Exception {
        mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3ADI")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("3ADI"))
                .andExpect(jsonPath("$.nivel").value("1ESO"))         // HEREDADO del padre
                .andExpect(jsonPath("$.tipo").value("DIVERSIFICACION_PDC"));
    }

    @Test
    void getTrasAlta_devuelveElPdc_200() throws Exception {
        crearPdc("3ADI");

        mockMvc.perform(get("/api/grupos/" + padreId + "/pdc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("3ADI"))
                .andExpect(jsonPath("$.tipo").value("DIVERSIFICACION_PDC"));
    }

    @Test
    void borrado_204_yLuegoGet404() throws Exception {
        crearPdc("3ADI");

        mockMvc.perform(delete("/api/grupos/" + padreId + "/pdc"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/grupos/" + padreId + "/pdc"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------- validaciones adicionales

    @Test
    void alta_padreInexistente_404() throws Exception {
        mockMvc.perform(post("/api/grupos/9999/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3ADI")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_codigoDuplicadoDeOtroGrupo_400() throws Exception {
        // "3A" ya lo usa el padre → colisión de código de grupo.
        mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_codigoDerivadoDeSubgrupoYaExiste_400ConCodigoDerivadoEnMensaje() throws Exception {
        // Un subgrupo "3ADI-Completo" ya existente hace chocar el código DERIVADO del alta.
        // El 400 debe NOMBRAR ese código derivado, no el del grupo.
        subgrupoRepository.save(new Subgrupo("3ADI-Completo",
                Set.of(grupoRepository.findById(padreId).orElseThrow())));

        mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3ADI")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("3ADI-Completo")));
    }

    // ============================ ASERTOS DISCRIMINANTES EXIGIDOS ============================

    /**
     * (1) Segundo PDC del mismo padre → 400. Si se quita la guarda {@code contarGruposHijos}
     * el segundo alta sería 201.
     */
    @Test
    void discriminante1_segundoPdcDelMismoPadre_400() throws Exception {
        crearPdc("3ADI");

        mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3ADI_2")))
                .andExpect(status().isBadRequest());
    }

    /**
     * (2) Tras el POST existe EXACTAMENTE UN subgrupo cuyo {@code getGrupos()} CONTIENE al
     * PDC y NO CONTIENE al padre. Aserto sobre CONTENIDO del Set (contains/doesNotContain),
     * no sobre size(): protege la regla S23 (si alguien enlazara el padre, el solver daría
     * INFEASIBLE y ningún test de tamaño lo vería). AVISO DE FRAMEWORK: se desligan las
     * entidades ({@code flush + clear}) antes de comprobar, para leer la BASE y no la caché
     * L1 de Hibernate (el objeto recién construido en memoria).
     */
    @Test
    void discriminante2_subgrupoMonoDi_contieneAlPdcYnoAlPadre() throws Exception {
        crearPdc("3ADI");

        entityManager.flush();
        entityManager.clear();

        List<Subgrupo> conElPdc = subgrupoRepository.findAll().stream()
                .filter(sg -> sg.getGrupos().stream()
                        .anyMatch(g -> g.getCodigo().equals("3ADI")))
                .toList();
        // EXACTAMENTE un subgrupo referencia al PDC.
        assertThat(conElPdc).hasSize(1);

        List<String> codigosDeSusGrupos = conElPdc.get(0).getGrupos().stream()
                .map(GrupoAdministrativo::getCodigo)
                .toList();
        // CONTENIDO: contiene al PDC, NO contiene al padre "3A" (regla S23).
        assertThat(codigosDeSusGrupos).contains("3ADI");
        assertThat(codigosDeSusGrupos).doesNotContain("3A");
    }

    /**
     * (3) idPadre apuntando a un grupo que ya ES {@code DIVERSIFICACION_PDC} → 400. Colgar un
     * PDC de un PDC viola la lista blanca del padre (solo ORDINARIO puede tener PDC).
     */
    @Test
    void discriminante3_idPadreQueYaEsPdc_400() throws Exception {
        long pdcId = crearPdc("3ADI");

        mockMvc.perform(post("/api/grupos/" + pdcId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("3ADI_HIJO")))
                .andExpect(status().isBadRequest());
    }

    /**
     * (4) POR MUTACIÓN: el subgrupo mono-Di metido en una plaza veta el borrado → 409, y no
     * borra nada. El desglose estructurado ({@code containsExactly} un único referente
     * {@code plaza(s)}) es el aserto que blinda la guarda {@code contarPlazas}: verificado a
     * mano quitando la guarda (pasa a 204) y restaurándola.
     */
    @Test
    void discriminante4_subgrupoMonoDiEnPlaza_delete409() throws Exception {
        crearPdc("3ADI");
        entityManager.flush();
        entityManager.clear();

        Subgrupo subgrupo = subgrupoRepository.findByCodigo("3ADI-Completo").orElseThrow();

        // Una plaza (dentro de una actividad) que referencia el subgrupo mono-Di por su M:N.
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Aula aula = aulaRepository.save(new Aula("AULA_T", TipoAula.ORDINARIA, null, null, null, null));
        Actividad actividad = new Actividad("ACT", mat, 1, 1, PatronTemporal.NEUTRA, false);
        actividad.agregarPlaza("ACT-P1", mat, aula, Set.of(), Set.of(), Set.of(subgrupo));
        actividadRepository.save(actividad);
        entityManager.flush();

        mockMvc.perform(delete("/api/grupos/" + padreId + "/pdc"))
                .andExpect(status().isConflict());

        // El referente es EXACTAMENTE una plaza; ni subgrupo_grupo ni grupos hijos cuentan.
        ReferenciaEntranteException error = catchThrowableOfType(
                () -> service.borrar(padreId), ReferenciaEntranteException.class);
        assertThat(error).isNotNull();
        assertThat(error.getReferencias())
                .containsExactly(new Referencia("plaza(s)", 1L));
    }

    /**
     * (5) GET sobre un padre SIN PDC → 404 (no 200 con null, no lista vacía).
     */
    @Test
    void discriminante5_getSobrePadreSinPdc_404() throws Exception {
        mockMvc.perform(get("/api/grupos/" + padreId + "/pdc"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------------- helpers

    /** Da de alta un PDC por la red bajo el padre de {@link #setUp} y devuelve su id. */
    private long crearPdc(String codigo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/grupos/" + padreId + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(codigo)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":".."}} */
    private static String body(String codigo) {
        return "{\"codigo\":\"" + codigo + "\"}";
    }
}
