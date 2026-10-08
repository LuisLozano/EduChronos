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

    // ──────────────────────── S203 T1: total declarado y cargo (C-totales-y-cargo, T1.4)

    @Test
    void totalYCargo_altaConLosDos_201YLosDevuelve() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "18", "\"DIRECTOR\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDeclarado").value(18))
                .andExpect(jsonPath("$.cargo").value("DIRECTOR"));
    }

    @Test
    void totalYCargo_altaSinLosCampos_sinTotalYProfesor() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Ada Lovelace")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDeclarado").value(nullValue()))
                .andExpect(jsonPath("$.cargo").value("PROFESOR"));
    }

    @Test
    void totalYCargo_altaConCargoNull_esProfesor() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "null", "null")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDeclarado").value(nullValue()))
                .andExpect(jsonPath("$.cargo").value("PROFESOR"));
    }

    /** D9: el total 0 es un total, no un error. Mata «< 0» → «<= 0». */
    @Test
    void totalYCargo_totalCero_201() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "0", "null")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDeclarado").value(0));
    }

    /** -1, el primer negativo: mata «< 0» → «< -1». El 400 nombra el campo. */
    @Test
    void totalYCargo_altaTotalNegativo_400ConCampoEnMensaje() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "-1", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("totalDeclarado")));
        mockMvc.perform(get("/api/profesores"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void totalYCargo_altaCargoInvalido_400ConValorYLista() throws Exception {
        mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "null", "\"BEDEL\"")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("BEDEL")))
                .andExpect(status().reason(containsString("JEFE_ESTUDIOS")));
    }

    @Test
    void totalYCargo_edicionLosCambia_200() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "20", "\"SECRETARIO\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeclarado").value(20))
                .andExpect(jsonPath("$.cargo").value("SECRETARIO"));
    }

    /**
     * D7: el PUT reemplaza. Sin total ni cargo, un profesor con 18 y DIRECTOR queda sin total y
     * PROFESOR; no conserva lo que tenía. Lo mira la respuesta y lo mira el GET.
     */
    @Test
    void totalYCargo_edicionSinLosCampos_vuelveASinTotalYProfesor() throws Exception {
        long id = crearCompleto("MAT8", "Ada Lovelace", "18", "\"DIRECTOR\"");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Ada Lovelace")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeclarado").value(nullValue()))
                .andExpect(jsonPath("$.cargo").value("PROFESOR"));
        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(jsonPath("$.totalDeclarado").value(nullValue()))
                .andExpect(jsonPath("$.cargo").value("PROFESOR"));
    }

    @Test
    void totalYCargo_edicionTotalNegativo_400() throws Exception {
        long id = crearCompleto("MAT8", "Ada Lovelace", "18", "null");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "-1", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("totalDeclarado")));
    }

    @Test
    void totalYCargo_edicionCargoInvalido_400() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto("MAT8", "Ada Lovelace", "null", "\"director\"")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("director")));
    }

    @Test
    void totalYCargo_getDevuelveLosDos() throws Exception {
        long id = crearCompleto("LEN2", "Miguel de Cervantes", "16", "\"JEFE_ESTUDIOS\"");

        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeclarado").value(16))
                .andExpect(jsonPath("$.cargo").value("JEFE_ESTUDIOS"));
        mockMvc.perform(get("/api/profesores"))
                .andExpect(jsonPath("$[0].totalDeclarado").value(16))
                .andExpect(jsonPath("$[0].cargo").value("JEFE_ESTUDIOS"));
    }

    // ------------------------------------------- guardias ordinarias (S212, C-dato-guardias)

    /** Alta sin el campo: 0 guardias, en la respuesta y en el GET. */
    @Test
    void guardias_altaSinElCampo_cero() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");

        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardiasOrdinarias").value(0));
    }

    /** PUT con 3: el GET devuelve 3. */
    @Test
    void guardias_edicionConTres_elGetDevuelveTres() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConGuardias("MAT8", "Ada Lovelace", "3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardiasOrdinarias").value(3));
        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(jsonPath("$.guardiasOrdinarias").value(3));
    }

    /** PUT con −1: 400 que nombra el campo, y el profesor conserva las que tenía. */
    @Test
    void guardias_edicionNegativa_400ConCampoEnMensaje() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");
        mockMvc.perform(put("/api/profesores/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConGuardias("MAT8", "Ada Lovelace", "2")));

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConGuardias("MAT8", "Ada Lovelace", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("guardiasOrdinarias")));
        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(jsonPath("$.guardiasOrdinarias").value(2));
    }

    /** El PUT reemplaza: sin el campo, un profesor con 3 guardias vuelve a 0. */
    @Test
    void guardias_edicionSinElCampo_vuelveACero() throws Exception {
        long id = crear("MAT8", "Ada Lovelace");
        mockMvc.perform(put("/api/profesores/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyConGuardias("MAT8", "Ada Lovelace", "3")));

        mockMvc.perform(put("/api/profesores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("MAT8", "Ada Lovelace")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardiasOrdinarias").value(0));
        mockMvc.perform(get("/api/profesores/" + id))
                .andExpect(jsonPath("$.guardiasOrdinarias").value(0));
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

    /** Alta con total y cargo, los dos como literales JSON ({@code 18}, {@code "DIRECTOR"}, {@code null}). */
    private long crearCompleto(String codigo, String nombre, String total, String cargo)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/profesores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCompleto(codigo, nombre, total, cargo)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"nombreCompleto":..,"totalDeclarado":..,"cargo":..}}, total y cargo en JSON. */
    private static String bodyCompleto(
            String codigo, String nombreCompleto, String total, String cargo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombreCompleto\":\"" + nombreCompleto + "\""
                + ",\"totalDeclarado\":" + total + ",\"cargo\":" + cargo + "}";
    }

    /** {@code {"codigo":..,"nombreCompleto":..,"guardiasOrdinarias":..}}, las guardias en JSON. */
    private static String bodyConGuardias(String codigo, String nombreCompleto, String guardias) {
        return "{\"codigo\":\"" + codigo + "\",\"nombreCompleto\":\"" + nombreCompleto + "\""
                + ",\"guardiasOrdinarias\":" + guardias + "}";
    }

    /** {@code {"codigo":..,"nombreCompleto":..}} */
    private static String body(String codigo, String nombreCompleto) {
        return "{\"codigo\":\"" + codigo + "\",\"nombreCompleto\":\"" + nombreCompleto + "\"}";
    }
}
