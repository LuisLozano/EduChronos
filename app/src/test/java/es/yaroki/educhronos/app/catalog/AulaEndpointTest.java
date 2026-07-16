package es.yaroki.educhronos.app.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.AulaService;
import es.yaroki.educhronos.app.web.AulaController;
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
 * Test de integración del CRUD {@code /api/aulas} (Fase 8, Bloque 8.5-A', réplica
 * del piloto {@code AsignaturaEndpointTest}). Ejerce alta/consulta/listado/edición/
 * borrado POR LA RED ({@code standaloneSetup} + {@code AulaService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes. Además del par crítico de
 * unicidad-en-edición, blinda las decisiones propias del aula:
 * <ul>
 *   <li>D-3: {@link #alta_tipoInvalido_400ConValorEnMensaje} — el 400 NOMBRA el valor
 *       inválido;
 *   <li>D-4: {@link #alta_sinLosCuatroNullable_201YLosDevuelveNull} — los cuatro
 *       campos opcionales llegan/persisten/viajan null;
 *   <li>{@link #alta_seisCamposPoblados_201YRoundTrip} — round-trip del enum (como
 *       String, {@code TipoAula.name()}) y de los Integer.
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AulaService.class)
class AulaEndpointTest {

    @Autowired private AulaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AulaController(service)).build();
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "ORDINARIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("A1"))
                .andExpect(jsonPath("$.tipo").value("ORDINARIA"));
    }

    @Test
    void getPorId_devuelveElAula() throws Exception {
        long id = crear(bodySoloTipo("LAB1", "LAB_CIENCIAS"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("LAB1"))
                .andExpect(jsonPath("$.tipo").value("LAB_CIENCIAS"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/aulas/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(bodySoloTipo("B", "ORDINARIA"));
        crear(bodySoloTipo("A", "ORDINARIA"));
        crear(bodySoloTipo("C", "ORDINARIA"));

        mockMvc.perform(get("/api/aulas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaTipoManteniendoCodigo_200() throws Exception {
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(put("/api/aulas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "INFORMATICA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("A1"))
                .andExpect(jsonPath("$.tipo").value("INFORMATICA"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/aulas/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "ORDINARIA")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "INFORMATICA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtra_400() throws Exception {
        crear(bodySoloTipo("A", "ORDINARIA"));
        long idB = crear(bodySoloTipo("B", "ORDINARIA"));

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/aulas/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A", "ORDINARIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear(bodySoloTipo("A", "ORDINARIA"));

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/aulas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A", "GIMNASIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.tipo").value("GIMNASIO"));
    }

    @Test
    void alta_tipoInvalido_400ConValorEnMensaje() throws Exception {
        // D-3: un tipo que no existe en TipoAula → 400 cuyo mensaje NOMBRA el valor malo.
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "CHUCHE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("CHUCHE")));
    }

    @Test
    void alta_tipoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("   ", "ORDINARIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_sinLosCuatroNullable_201YLosDevuelveNull() throws Exception {
        // D-4: capacidad/edificio/planta/sector AUSENTES en el body → 201, y el GET
        // posterior los devuelve como null explícito (contrato de serialización de 7A).
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ORDINARIA"))
                .andExpect(jsonPath("$.capacidad").value(nullValue()))
                .andExpect(jsonPath("$.edificio").value(nullValue()))
                .andExpect(jsonPath("$.planta").value(nullValue()))
                .andExpect(jsonPath("$.sector").value(nullValue()));
    }

    @Test
    void alta_seisCamposPoblados_201YRoundTrip() throws Exception {
        // Round-trip completo: el enum viaja como String (TipoAula.name()) y los dos
        // Integer conservan su valor a través del alta y del GET posterior.
        long id = crear(bodyCompleto("LAB1", "LAB_CIENCIAS", 30, "A", 1, "Norte"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LAB1"))
                .andExpect(jsonPath("$.tipo").value("LAB_CIENCIAS"))
                .andExpect(jsonPath("$.capacidad").value(30))
                .andExpect(jsonPath("$.edificio").value("A"))
                .andExpect(jsonPath("$.planta").value(1))
                .andExpect(jsonPath("$.sector").value("Norte"));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(delete("/api/aulas/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrado_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/aulas/9999"))
                .andExpect(status().isNotFound());
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"tipo":..}} — los cuatro campos nullable AUSENTES (D-4). */
    private static String bodySoloTipo(String codigo, String tipo) {
        return "{\"codigo\":\"" + codigo + "\",\"tipo\":\"" + tipo + "\"}";
    }

    /** {@code {"codigo":..,"tipo":..,"capacidad":..,"edificio":..,"planta":..,"sector":..}} */
    private static String bodyCompleto(String codigo, String tipo, int capacidad,
                                       String edificio, int planta, String sector) {
        return "{\"codigo\":\"" + codigo + "\",\"tipo\":\"" + tipo + "\""
                + ",\"capacidad\":" + capacidad
                + ",\"edificio\":\"" + edificio + "\""
                + ",\"planta\":" + planta
                + ",\"sector\":\"" + sector + "\"}";
    }
}
