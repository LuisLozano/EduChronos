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
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimoPorTramo\":6}"))
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
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"minimoPorTramo\":0}"))
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
}
