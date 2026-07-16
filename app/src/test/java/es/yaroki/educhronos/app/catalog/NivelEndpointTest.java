package es.yaroki.educhronos.app.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.NivelService;
import es.yaroki.educhronos.app.web.NivelController;
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
 * Test de integración del CRUD {@code /api/niveles} (Fase 8, Bloque 8.5-A', réplica
 * del piloto {@code AsignaturaEndpointTest}). Ejerce alta/consulta/listado/edición/
 * borrado POR LA RED ({@code standaloneSetup} + {@code NivelService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes. Además del par crítico de
 * unicidad-en-edición, blinda D-1: {@link #listar_ordenaPorOrdenNoPorCodigo} inserta
 * el {@code orden} numérico CRUZADO con el orden alfabético del código, de modo que
 * un {@code sorted(by codigo)} copiado del piloto NO sobreviviría.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NivelService.class)
class NivelEndpointTest {

    @Autowired private NivelService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NivelController(service)).build();
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/niveles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("1ESO"))
                .andExpect(jsonPath("$.orden").value(1));
    }

    @Test
    void getPorId_devuelveElNivel() throws Exception {
        long id = crear("1BACH", 5);

        mockMvc.perform(get("/api/niveles/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("1BACH"))
                .andExpect(jsonPath("$.orden").value(5));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/niveles/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_ordenaPorOrdenNoPorCodigo() throws Exception {
        // D-1: orden numérico CRUZADO con el alfabético del código. Por 'orden' sale
        // 1ESO(1), 2ESO(2), 1BACH(5); alfabético daría 1BACH, 1ESO, 2ESO — DISTINTO.
        crear("2ESO", 2);
        crear("1BACH", 5);
        crear("1ESO", 1);

        mockMvc.perform(get("/api/niveles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("1ESO"))
                .andExpect(jsonPath("$[1].codigo").value("2ESO"))
                .andExpect(jsonPath("$[2].codigo").value("1BACH"));
    }

    @Test
    void edicion_cambiaOrdenManteniendoCodigo_200() throws Exception {
        long id = crear("1ESO", 1);

        mockMvc.perform(put("/api/niveles/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("1ESO"))
                .andExpect(jsonPath("$.orden").value(9));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/niveles/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear("1ESO", 1);

        mockMvc.perform(post("/api/niveles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtro_400() throws Exception {
        crear("1ESO", 1);
        long idB = crear("2ESO", 2);

        // PUT sobre 2ESO pidiendo el código de 1ESO → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/niveles/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear("1ESO", 1);

        // PUT sobre 1ESO con su MISMO código: la unicidad debe excluirse a sí misma →
        // 200, NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/niveles/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1ESO", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("1ESO"))
                .andExpect(jsonPath("$.orden").value(3));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear("1ESO", 1);

        mockMvc.perform(delete("/api/niveles/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/niveles/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrado_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/niveles/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/niveles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ", 1)))
                .andExpect(status().isBadRequest());
    }

    /** Da de alta por la red y devuelve el id sintético asignado. */
    private long crear(String codigo, int orden) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/niveles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(codigo, orden)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"orden":..}} */
    private static String body(String codigo, int orden) {
        return "{\"codigo\":\"" + codigo + "\",\"orden\":" + orden + "}";
    }
}
