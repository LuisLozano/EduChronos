package es.yaroki.educhronos.app.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.GrupoService;
import es.yaroki.educhronos.app.service.TutoriaService;
import es.yaroki.educhronos.app.web.GrupoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del CRUD {@code /api/grupos} (Fase 8, Bloque 8.5-B, réplica del
 * piloto {@code AulaEndpointTest}), RESTRINGIDO a grupos ORDINARIOS. Ejerce
 * alta/consulta/listado/edición/borrado POR LA RED ({@code standaloneSetup} +
 * {@code GrupoService} real sobre {@code @DataJpaTest}), con asertos discriminantes.
 * Además del par crítico de unicidad-en-edición, blinda las decisiones del bloque:
 * <ul>
 *   <li>D-nueva (FK por código): {@link #nivelInexistente_400ConCodigoEnMensaje} — el
 *       400 NOMBRA el código de nivel inexistente;
 *   <li>D-nueva-2 (lista blanca): {@link #tipoDiversificacion_400ConTipoEnMensaje} y
 *       {@link #tipoVirtualOptativa_400ConTipoEnMensaje} — LOS DOS no-ordinarios se
 *       rechazan, con el tipo en el reason.
 * </ul>
 * Da de alta niveles de apoyo ("1ESO", "2ESO") en {@link #setUp}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GrupoService.class, TutoriaService.class})
class GrupoEndpointTest {

    @Autowired private GrupoService service;
    @Autowired private TutoriaService tutoriaService;
    @Autowired private NivelRepository nivelRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GrupoController(service, tutoriaService)).build();
        // Niveles de apoyo: el grupo referencia el nivel por CÓDIGO de negocio.
        nivelRepository.save(new Nivel("1ESO", 1));
        nivelRepository.save(new Nivel("2ESO", 2));
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO_A", "1ESO", "ORDINARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("1ESO_A"))
                .andExpect(jsonPath("$.nivel").value("1ESO"))
                .andExpect(jsonPath("$.tipo").value("ORDINARIO"));
    }

    @Test
    void getPorId_devuelveElGrupo() throws Exception {
        long id = crear(body("1ESO_A", "1ESO", "ORDINARIO"));

        mockMvc.perform(get("/api/grupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("1ESO_A"))
                .andExpect(jsonPath("$.nivel").value("1ESO"))
                .andExpect(jsonPath("$.tipo").value("ORDINARIO"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/grupos/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(body("B", "1ESO", "ORDINARIO"));
        crear(body("A", "1ESO", "ORDINARIO"));
        crear(body("C", "1ESO", "ORDINARIO"));

        mockMvc.perform(get("/api/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaNivelManteniendoCodigo_200() throws Exception {
        long id = crear(body("1ESO_A", "1ESO", "ORDINARIO"));

        mockMvc.perform(put("/api/grupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO_A", "2ESO", "ORDINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("1ESO_A"))
                .andExpect(jsonPath("$.nivel").value("2ESO"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/grupos/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO_A", "1ESO", "ORDINARIO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(body("A", "1ESO", "ORDINARIO"));

        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "2ESO", "ORDINARIO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtro_400() throws Exception {
        crear(body("A", "1ESO", "ORDINARIO"));
        long idB = crear(body("B", "1ESO", "ORDINARIO"));

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/grupos/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "1ESO", "ORDINARIO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear(body("A", "1ESO", "ORDINARIO"));

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/grupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "2ESO", "ORDINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.nivel").value("2ESO"));
    }

    @Test
    void nivelInexistente_400ConCodigoEnMensaje() throws Exception {
        // El nivel viaja como código; uno que no existe → 400 cuyo reason lo NOMBRA.
        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "NOEXISTE", "ORDINARIO")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("NOEXISTE")));
    }

    @Test
    void tipoDiversificacion_400ConTipoEnMensaje() throws Exception {
        // D-nueva-2: DIVERSIFICACION_PDC es un tipo válido de TipoGrupo pero NO se crea
        // por este flujo (es 8.5-D). 400 con el tipo rechazado en el reason.
        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "1ESO", "DIVERSIFICACION_PDC")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("DIVERSIFICACION_PDC")));
    }

    @Test
    void tipoVirtualOptativa_400ConTipoEnMensaje() throws Exception {
        // D-nueva-2 (lista blanca): el SEGUNDO no-ordinario también se rechaza, no solo
        // PDC. VIRTUAL_OPTATIVA es 8.5-C+. 400 con el tipo en el reason.
        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "1ESO", "VIRTUAL_OPTATIVA")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("VIRTUAL_OPTATIVA")));
    }

    @Test
    void tipoInvalido_400ConValorEnMensaje() throws Exception {
        // Un tipo que no existe en TipoGrupo → 400 cuyo mensaje NOMBRA el valor malo.
        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "1ESO", "CHUCHE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("CHUCHE")));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(body("A", "1ESO", "ORDINARIO"));

        mockMvc.perform(delete("/api/grupos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/grupos/" + id))
                .andExpect(status().isNotFound());
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"nivel":..,"tipo":..}} */
    private static String body(String codigo, String nivel, String tipo) {
        return "{\"codigo\":\"" + codigo + "\",\"nivel\":\"" + nivel + "\""
                + ",\"tipo\":\"" + tipo + "\"}";
    }
}
