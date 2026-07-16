package es.yaroki.educhronos.app.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.SubgrupoService;
import es.yaroki.educhronos.app.web.SubgrupoController;
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
 * Test de integración del CRUD {@code /api/subgrupos} (Fase 8, Bloque 8.5-B, réplica
 * del piloto {@code AulaEndpointTest}). Ejerce alta/consulta/listado/edición/borrado
 * POR LA RED ({@code standaloneSetup} + {@code SubgrupoService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes sobre la relación N:M con grupos:
 * <ul>
 *   <li>D-nueva-5: {@link #alta_conDosGrupos_201YRoundTripDeCodigos} — round-trip POR
 *       CONTENIDO ({@code containsInAnyOrder}), no por tamaño;
 *   <li>reemplazo total: {@link #edicion_reemplazaGruposTotal_200} — el PUT SUSTITUYE
 *       el set, no lo une;
 *   <li>D-nueva-3 (borrado fuerte): {@link #borrado_limpiaJoinTableYNoBorraGrupos} —
 *       el cascade limpia {@code subgrupo_grupo} (0 filas) y NO borra los grupos.
 * </ul>
 * Da de alta grupos de apoyo ("G_A", "G_B", "G_C") sobre un nivel en {@link #setUp}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SubgrupoService.class)
class SubgrupoEndpointTest {

    @Autowired private SubgrupoService service;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubgrupoController(service)).build();
        // Grupos de apoyo (ordinarios): el subgrupo los referencia por CÓDIGO.
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        grupoRepository.save(new GrupoAdministrativo("G_A", nivel, TipoGrupo.ORDINARIO, null));
        grupoRepository.save(new GrupoAdministrativo("G_B", nivel, TipoGrupo.ORDINARIO, null));
        grupoRepository.save(new GrupoAdministrativo("G_C", nivel, TipoGrupo.ORDINARIO, null));
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("SG1"));
    }

    @Test
    void getPorId_devuelveElSubgrupo() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("SG1"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A")));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/subgrupos/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(body("B", "G_A"));
        crear(body("A", "G_A"));
        crear(body("C", "G_A"));

        mockMvc.perform(get("/api/subgrupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaCodigoManteniendoGrupos_200() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1_BIS", "G_A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("SG1_BIS"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A")));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/subgrupos/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(body("SG1", "G_A"));

        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_B")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtro_400() throws Exception {
        crear(body("A", "G_A"));
        long idB = crear(body("B", "G_A"));

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/subgrupos/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "G_A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear(body("A", "G_A"));

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "G_B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_B")));
    }

    @Test
    void alta_conDosGrupos_201YRoundTripDeCodigos() throws Exception {
        // D-nueva-5 (aserto más importante del bloque): el round-trip fija QUÉ DOS
        // códigos, no solo cuántos. Un length()==2 pasaría con grupos equivocados.
        long id = crear(body("SG1", "G_A", "G_B"));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SG1"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A", "G_B")));
    }

    @Test
    void grupoInexistenteEnAlta_400ConCodigoEnMensaje() throws Exception {
        // D-nueva-4: si ALGÚN código de grupo no resuelve → 400 que lo NOMBRA.
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A", "NOEXISTE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("NOEXISTE")));
    }

    @Test
    void gruposVacio_400() throws Exception {
        // D-nueva-1: un subgrupo necesita ≥1 grupo (invariante I6). grupos=[] → 400.
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"SG1\",\"grupos\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_reemplazaGruposTotal_200() throws Exception {
        // Reemplazo TOTAL, no unión: subgrupo con [G_A,G_B]; PUT con [G_C] → el GET solo
        // ve G_C. Si actualizar() uniese, el GET tendría 3 códigos y esto caería.
        long id = crear(body("SG1", "G_A", "G_B"));

        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_C")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_C")));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_C")));
    }

    @Test
    void borrado_limpiaJoinTableYNoBorraGrupos() throws Exception {
        // D-nueva-3 (borrado fuerte): dar de alta SG1 con [G_A,G_B], contar la join
        // table ANTES (2), borrar, y verificar que (a) queda a 0 filas para ese
        // subgrupo y (b) los grupos G_A/G_B SIGUEN existiendo.
        long id = crear(body("SG1", "G_A", "G_B"));

        assertEquals(2L, contarJoinTable(id), "el alta debe crear 2 filas en subgrupo_grupo");

        mockMvc.perform(delete("/api/subgrupos/" + id))
                .andExpect(status().isNoContent());

        // (a) el cascade del lado propietario limpia la join table.
        assertEquals(0L, contarJoinTable(id),
                "el borrado del subgrupo debe dejar 0 filas en subgrupo_grupo");
        // (b) los grupos, entidades independientes, sobreviven al borrado del subgrupo.
        assertTrue(grupoRepository.findByCodigo("G_A").isPresent(), "G_A no debe borrarse");
        assertTrue(grupoRepository.findByCodigo("G_B").isPresent(), "G_B no debe borrarse");
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(delete("/api/subgrupos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isNotFound());
    }

    /**
     * Cuenta las filas de la join table {@code subgrupo_grupo} para un subgrupo dado.
     * Query NATIVA: no hay lado inverso del @ManyToMany (es unidireccional) y tras el
     * borrado el Subgrupo ya no existe, así que es el único camino. El nombre
     * {@code subgrupo_grupo} es parte del contrato de la entidad (@JoinTable name).
     */
    private long contarJoinTable(long subgrupoId) {
        Number filas = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM subgrupo_grupo WHERE subgrupo_id = ?")
                .setParameter(1, subgrupoId)
                .getSingleResult();
        return filas.longValue();
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"grupos":["..",..]}} a partir de los códigos de grupo. */
    private static String body(String codigo, String... grupos) {
        StringBuilder sb = new StringBuilder("{\"codigo\":\"").append(codigo).append("\",\"grupos\":[");
        for (int i = 0; i < grupos.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(grupos[i]).append("\"");
        }
        return sb.append("]}").toString();
    }
}
