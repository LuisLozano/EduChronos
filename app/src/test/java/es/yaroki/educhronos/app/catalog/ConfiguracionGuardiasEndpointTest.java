package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.ConfiguracionGuardiasService;
import es.yaroki.educhronos.app.web.ConfiguracionGuardiasController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * T3 de C-dato-guardias (S212): el mínimo de profesores de guardia por tramo, POR LA RED
 * ({@code standaloneSetup} + {@link ConfiguracionGuardiasService} real sobre la base del test),
 * con el molde de {@code JornadaEndpointTest}. La base de cada contexto nace vacía, así que el
 * {@code GET} sin tocar nada mide el valor por defecto de producción.
 *
 * <p>Los {@code PUT} se miran también en la fila de {@code configuracion}, por su clave literal:
 * un {@code GET} que lee lo que escribió el {@code PUT} cuadraría aunque los dos usaran otra clave.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConfiguracionGuardiasService.class)
class ConfiguracionGuardiasEndpointTest {

    private static final String URL = "/api/configuracion-guardias";

    @Autowired private ConfiguracionGuardiasService service;
    @Autowired private ConfiguracionRepository configuraciones;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConfiguracionGuardiasController(service)).build();
    }

    /** Con Configuracion vacía, el mínimo por defecto: 4. */
    @Test
    void get_sinConfiguracion_devuelveCuatro() throws Exception {
        assertThat(configuraciones.count()).as("precondición: Configuracion vacía").isZero();

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoPorTramo").value(4));
    }

    @Test
    void put_seis_elGetDevuelveSeisYLaFilaLoGuarda() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(6, 0, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoPorTramo").value(6));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(URL)).andExpect(jsonPath("$.minimoPorTramo").value(6));
        assertThat(configuraciones.findById("guardias.minimoPorTramo").orElseThrow().getValor()).isEqualTo("6");
    }

    /** 0 es un valor, no un error: el centro no usa guardias. */
    @Test
    void put_cero_elGetDevuelveCero() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(0, 0, 0)))
                .andExpect(status().isOk());

        mockMvc.perform(get(URL)).andExpect(jsonPath("$.minimoPorTramo").value(0));
    }

    /** −1: 400 que nombra el campo, y no se guarda nada. */
    @Test
    void put_negativo_400SinGuardar() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimoPorTramo\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoPorTramo")));

        assertThat(configuraciones.count()).isZero();
        mockMvc.perform(get(URL)).andExpect(jsonPath("$.minimoPorTramo").value(4));
    }

    /** Sin el campo: 400, no un 0 silencioso. */
    @Test
    void put_sinElCampo_400() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoPorTramo")));

        assertThat(configuraciones.count()).isZero();
    }

    // ───────────────────────────── S219, C-dato-guardias-especiales: los dos mínimos de recreo

    /** Con Configuracion vacía, los tres por defecto: 4 por tramo y 0 y 0 por recreo. */
    @Test
    void get_sinConfiguracion_devuelveCuatroCeroCero() throws Exception {
        assertThat(configuraciones.count()).as("precondición: Configuracion vacía").isZero();

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoPorTramo").value(4))
                .andExpect(jsonPath("$.minimoPorRecreo").value(0))
                .andExpect(jsonPath("$.minimoBibliotecaPorRecreo").value(0));
    }

    /**
     * Tres valores distintos, para que un cruce de campos o de claves falle; las filas se miran por su
     * clave literal.
     */
    @Test
    void put_tresSieteUno_elGetDevuelveLosTresYLasFilasLosGuardan() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(3, 7, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoPorTramo").value(3))
                .andExpect(jsonPath("$.minimoPorRecreo").value(7))
                .andExpect(jsonPath("$.minimoBibliotecaPorRecreo").value(1));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$.minimoPorTramo").value(3))
                .andExpect(jsonPath("$.minimoPorRecreo").value(7))
                .andExpect(jsonPath("$.minimoBibliotecaPorRecreo").value(1));
        assertThat(configuraciones.findById("guardias.minimoPorTramo").orElseThrow().getValor()).isEqualTo("3");
        assertThat(configuraciones.findById("guardias.minimoPorRecreo").orElseThrow().getValor()).isEqualTo("7");
        assertThat(configuraciones.findById("guardias.minimoBibliotecaPorRecreo").orElseThrow().getValor())
                .isEqualTo("1");
    }

    /** Sin el mínimo por recreo: 400 que lo nombra, y no se guarda ninguno de los otros dos. */
    @Test
    void put_sinMinimoPorRecreo_400SinGuardar() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minimoPorTramo\":3,\"minimoBibliotecaPorRecreo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoPorRecreo")));

        assertThat(configuraciones.count()).isZero();
    }

    /** Sin el mínimo de biblioteca: 400 que lo nombra, y no se guarda ninguno de los otros dos. */
    @Test
    void put_sinMinimoBiblioteca_400SinGuardar() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minimoPorTramo\":3,\"minimoPorRecreo\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoBibliotecaPorRecreo")));

        assertThat(configuraciones.count()).isZero();
    }

    /** Mínimo por recreo negativo: 400 que lo nombra, sin guardar nada. */
    @Test
    void put_minimoPorRecreoNegativo_400SinGuardar() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(3, -1, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoPorRecreo")));

        assertThat(configuraciones.count()).isZero();
    }

    /** Mínimo de biblioteca negativo: 400 que lo nombra, sin guardar nada. */
    @Test
    void put_minimoBibliotecaNegativo_400SinGuardar() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(3, 7, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("minimoBibliotecaPorRecreo")));

        assertThat(configuraciones.count()).isZero();
    }

    /** Biblioteca mayor que recreo se guarda: lo juzga la prevalidación, no el PUT. */
    @Test
    void put_bibliotecaMayorQueRecreo_200() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(3, 1, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoPorRecreo").value(1))
                .andExpect(jsonPath("$.minimoBibliotecaPorRecreo").value(2));
    }

    /** El cuerpo del {@code PUT} con los tres mínimos. */
    private static String cuerpo(int porTramo, int porRecreo, int biblioteca) {
        return "{\"minimoPorTramo\":" + porTramo + ",\"minimoPorRecreo\":" + porRecreo
                + ",\"minimoBibliotecaPorRecreo\":" + biblioteca + "}";
    }
}
