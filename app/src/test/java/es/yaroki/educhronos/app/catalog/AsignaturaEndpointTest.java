package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
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
    @Autowired private AsignaturaAulaCompatibleRepository compatibilidades;
    @Autowired private TestEntityManager entityManager;

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

    // ─────────────────── sub-recurso aulas-compatibles (§4.7, Bloque 8.5-C3)

    @Test
    void compat_getInicial_200ListaVacia() throws Exception {
        long id = crear("ByG", "Biologia");
        mockMvc.perform(get("/api/asignaturas/" + id + "/aulas-compatibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void compat_putDosTipos_200YGetLosDevuelveEnOrdenDeEnum() throws Exception {
        long id = crear("ByG", "Biologia");
        // Enviados en orden inverso al del enum: deben volver en orden natural
        // (LAB_CIENCIAS ordinal 1 antes que INFORMATICA ordinal 2).
        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"INFORMATICA\",\"LAB_CIENCIAS\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("LAB_CIENCIAS"))
                .andExpect(jsonPath("$[1]").value("INFORMATICA"));

        mockMvc.perform(get("/api/asignaturas/" + id + "/aulas-compatibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("LAB_CIENCIAS"))
                .andExpect(jsonPath("$[1]").value("INFORMATICA"));
    }

    @Test
    void compat_putListaVacia_borraTodas() throws Exception {
        long id = crear("ByG", "Biologia");
        putCompat(id, "[\"LAB_CIENCIAS\"]");

        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/asignaturas/" + id + "/aulas-compatibles"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void compat_putValorNoParseable_400ConValorEnReason() throws Exception {
        long id = crear("ByG", "Biologia");
        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"NO_EXISTE\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("NO_EXISTE")));
    }

    @Test
    void compat_putDuplicado_400ConTipoEnReason() throws Exception {
        long id = crear("ByG", "Biologia");
        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"LAB_CIENCIAS\",\"LAB_CIENCIAS\"]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("LAB_CIENCIAS")));
    }

    @Test
    void compat_getIdInexistente_404() throws Exception {
        mockMvc.perform(get("/api/asignaturas/9999/aulas-compatibles"))
                .andExpect(status().isNotFound());
    }

    @Test
    void compat_putIdInexistente_404() throws Exception {
        mockMvc.perform(put("/api/asignaturas/9999/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"LAB_CIENCIAS\"]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void compat_putDosVecesLoMismo_idempotente() throws Exception {
        long id = crear("ByG", "Biologia");
        String body = "[\"LAB_CIENCIAS\",\"INFORMATICA\"]";
        putCompat(id, body);
        // Segunda vez idéntica: 200 y misma lista, sin violar la UNIQUE (delete+flush+insert).
        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("LAB_CIENCIAS"))
                .andExpect(jsonPath("$[1]").value("INFORMATICA"));
    }

    @Test
    void compat_borrarAsignaturaConCompatibilidades_204YCascadaBorraFilas() throws Exception {
        // Cascada D3 (verificada por mutación del schema, ver la bitácora del bloque): una
        // asignatura CON compatibilidades ya NO es referencia entrante (8.5-C3) → 204, no 409;
        // y el `on delete cascade` del schema se lleva sus filas hijas.
        long id = crear("ByG", "Biologia");
        putCompat(id, "[\"LAB_CIENCIAS\",\"INFORMATICA\"]");
        // Desliga las filas hijas del contexto: en producción el PUT y el DELETE son peticiones
        // (transacciones) distintas; sin esto, el @DataJpaTest de una sola transacción las
        // mantendría managed y el autoflush chocaría con la cascada, que Hibernate no conoce.
        entityManager.flush();
        entityManager.clear();
        assertThat(compatibilidades.count()).isEqualTo(2);

        mockMvc.perform(delete("/api/asignaturas/" + id))
                .andExpect(status().isNoContent());

        // Fuerza el DELETE del padre: el autoflush ante un count() sobre la tabla HIJA no lo
        // dispararía (Hibernate no ve la dependencia; la cascada es solo del schema). Ya en la BD,
        // el `on delete cascade` se lleva las filas hijas.
        entityManager.flush();
        entityManager.clear();
        assertThat(compatibilidades.count()).isEqualTo(0);
        mockMvc.perform(get("/api/asignaturas/" + id))
                .andExpect(status().isNotFound());
    }

    /** PUT de compatibilidades esperando 200 (helper de fixture). */
    private void putCompat(long id, String jsonArray) throws Exception {
        mockMvc.perform(put("/api/asignaturas/" + id + "/aulas-compatibles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray))
                .andExpect(status().isOk());
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
