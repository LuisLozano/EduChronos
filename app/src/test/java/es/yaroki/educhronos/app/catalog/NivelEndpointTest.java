package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.NivelService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.NivelController;
import java.util.List;
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
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private TestEntityManager entityManager;

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

    /**
     * RÉPLICA del borrado amable (8.5-C2b) más allá del piloto: un nivel con un
     * {@link GrupoAdministrativo} que lo referencia por {@code nivel_id} NO se borra → 409 con el
     * desglose. Prueba que el patrón replica en una raíz de una sola FK entrante, no solo en Aula.
     *
     * <p><b>Desalineado de ids</b> (lección de los tests 1 y 3): se siembra un nivel de relleno
     * para que el nivel real no comparta id con el grupo. Sin él ambos serían id=1 y una
     * {@code @Query} desviada a otra columna ({@code id}, {@code grupo_padre_id}) contaría por
     * colisión; con el id desalineado cuenta 0 y el test cae. El {@code doesNotContain} lo verifica
     * en runtime; el {@code containsExactly} exige el único referente {@code grupo(s)} con conteo
     * real y ninguno de más.
     */
    @Test
    void borrado_nivelConGrupo_409YDesgloseConElConteoReal() throws Exception {
        nivelRepository.save(new Nivel("RELLENO", 99));   // desalinea: el nivel real no será id=1
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo = grupoRepository.save(
                new GrupoAdministrativo("1ºA", nivel, TipoGrupo.ORDINARIO, null));
        entityManager.flush();

        // Precondición del desalineado: el id del nivel no coincide con el del grupo.
        assertThat(List.of(grupo.getId())).doesNotContain(nivel.getId());

        mockMvc.perform(delete("/api/niveles/" + nivel.getId()))
                .andExpect(status().isConflict());

        ReferenciaEntranteException error = catchThrowableOfType(
                () -> service.borrar(nivel.getId()), ReferenciaEntranteException.class);
        assertThat(error).isNotNull();
        assertThat(error.getReferencias())
                .containsExactly(new Referencia("grupo(s)", 1L));
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
