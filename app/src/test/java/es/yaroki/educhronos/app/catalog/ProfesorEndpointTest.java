package es.yaroki.educhronos.app.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.ProfesorService;
import es.yaroki.educhronos.app.service.RestriccionHorariaService;
import es.yaroki.educhronos.app.web.ProfesorController;
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
 * Test de integración del CRUD {@code /api/profesores} (Fase 8, Bloque 8.5-A',
 * réplica del piloto {@code AsignaturaEndpointTest}). Ejerce alta/consulta/listado/
 * edición/borrado POR LA RED ({@code standaloneSetup} + {@code ProfesorService} real
 * sobre {@code @DataJpaTest}), con asertos discriminantes. El par crítico es
 * {@link #edicion_codigoQuePisaAOtro_400} / {@link #edicion_guardaMismoCodigo_200}:
 * la unicidad-en-edición debe excluir a la propia entidad (un {@code findByCodigo}
 * ingenuo rompe el segundo).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProfesorService.class, RestriccionHorariaService.class})
class ProfesorEndpointTest {

    @Autowired private ProfesorService service;
    @Autowired private RestriccionHorariaService restriccionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProfesorController(service, restriccionService)).build();
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Ada Lovelace")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("MAT8"))
                .andExpect(jsonPath("$.nombreCompleto").value("Ada Lovelace"));
    }

    @Test
    void getPorId_devuelveElProfesor() throws Exception {
        long id = crear("LEN2", "Miguel de Cervantes");

        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("LEN2"))
                .andExpect(jsonPath("$.nombreCompleto").value("Miguel de Cervantes"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/profesores/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear("B", "Bravo");
        crear("A", "Alfa");
        crear("C", "Charlie");

        mockMvc.perform(get("/api/profesores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaNombreManteniendoCodigo_200() throws Exception {
        long id = crear("MAT8", "Nombre viejo");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Nombre nuevo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("MAT8"))
                .andExpect(jsonPath("$.nombreCompleto").value("Nombre nuevo"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/profesores/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Ada Lovelace")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear("MAT8", "Ada Lovelace");

        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Otra Ada")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtro_400() throws Exception {
        crear("A", "Alfa");
        long idB = crear("B", "Bravo");

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/profesores/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "Bravo renombrado")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear("A", "Alfa");

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "Alfa reescrito")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.nombreCompleto").value("Alfa reescrito"));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");

        mockMvc.perform(delete("/api/profesores/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrado_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/profesores/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ", "Ada Lovelace")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_nombreEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "")))
                .andExpect(status().isBadRequest());
    }

    /** Da de alta por la red y devuelve el id sintético asignado. */
    private long crear(String codigo, String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/profesores")
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
