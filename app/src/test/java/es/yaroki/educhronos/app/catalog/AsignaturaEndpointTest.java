package es.yaroki.educhronos.app.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.AsignaturaService;
import es.yaroki.educhronos.app.web.AsignaturaController;
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
 * Test de integración del CRUD {@code /api/asignaturas} (Fase 8, Bloque 8.5-A,
 * piloto del patrón CRUD de catálogo). Ejerce alta/consulta/listado/edición/borrado
 * POR LA RED ({@code standaloneSetup} + {@code AsignaturaService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes. El par crítico es
 * {@link #edicion_codigoQuePisaAOtra_400} / {@link #edicion_guardaMismoCodigo_200}:
 * la unicidad-en-edición debe excluir a la propia entidad (un {@code findByCodigo}
 * ingenuo rompe el segundo).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AsignaturaService.class)
class AsignaturaEndpointTest {

    @Autowired private AsignaturaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AsignaturaController(service)).build();
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/asignaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mat", "Matematicas")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("Mat"))
                .andExpect(jsonPath("$.nombreCompleto").value("Matematicas"));
    }

    @Test
    void getPorId_devuelveLaAsignatura() throws Exception {
        long id = crear("LCL", "Lengua Castellana y Literatura");

        mockMvc.perform(get("/api/asignaturas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("LCL"))
                .andExpect(jsonPath("$.nombreCompleto").value("Lengua Castellana y Literatura"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/asignaturas/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear("B", "Bravo");
        crear("A", "Alfa");
        crear("C", "Charlie");

        mockMvc.perform(get("/api/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaNombreManteniendoCodigo_200() throws Exception {
        long id = crear("Mat", "Nombre viejo");

        mockMvc.perform(put("/api/asignaturas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mat", "Nombre nuevo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("Mat"))
                .andExpect(jsonPath("$.nombreCompleto").value("Nombre nuevo"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/asignaturas/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mat", "Matematicas")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear("Mat", "Matematicas");

        mockMvc.perform(post("/api/asignaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mat", "Otra Matematicas")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtra_400() throws Exception {
        crear("A", "Alfa");
        long idB = crear("B", "Bravo");

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/asignaturas/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "Bravo renombrado")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear("A", "Alfa");

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/asignaturas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "Alfa reescrito")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.nombreCompleto").value("Alfa reescrito"));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear("Mat", "Matematicas");

        mockMvc.perform(delete("/api/asignaturas/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/asignaturas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrado_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/asignaturas/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/asignaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ", "Matematicas")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_nombreEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/asignaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mat", "")))
                .andExpect(status().isBadRequest());
    }

    /** Da de alta por la red y devuelve el id sintético asignado. */
    private long crear(String codigo, String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/asignaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(codigo, nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"nombreCompleto":..}} */
    private static String body(String codigo, String nombreCompleto) {
        return "{\"codigo\":\"" + codigo + "\",\"nombreCompleto\":\"" + nombreCompleto + "\"}";
    }
}
