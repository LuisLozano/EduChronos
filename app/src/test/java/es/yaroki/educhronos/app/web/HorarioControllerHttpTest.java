package es.yaroki.educhronos.app.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración HTTP del endpoint de proyección (Fase 7). Ejerce el
 * controlador POR LA RED (binding de la ruta + traducción de excepciones), no a
 * nivel de servicio: cierra la deuda de 7A, que solo probaba el servicio y por
 * eso no cazó el fallo de resolución de {@code @PathVariable} sin {@code -parameters}.
 *
 * <p>Usa {@code MockMvcBuilders.standaloneSetup} (de {@code spring-test}, ya en el
 * classpath) con el servicio mockeado: no necesita el slice web de Boot (que en
 * SB 4.1 vive en un módulo aparte no presente) ni base de datos. El
 * {@code standaloneSetup} monta el mismo {@code RequestMappingHandlerAdapter} de
 * producción, así que reproduce el binding real de la ruta.
 */
@ExtendWith(MockitoExtension.class)
class HorarioControllerHttpTest {

    @Mock
    private GeneradorHorarioService service;

    @Mock
    private DiagnosticoService diagnosticoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(service, diagnosticoService)).build();
    }

    @Test
    void get_conIdExistente_devuelve200YLasSesiones() throws Exception {
        SesionVistaDTO sesion = new SesionVistaDTO(
                10L, 1, 1, 1, "Mat", "Matematicas", List.of("MATA"), "A1",
                List.of("1ºA-Completo"), List.of("1ºA"), "Mat-1ºA", "Mat-1ºA-P1");
        HorarioProyeccionDTO dto = new HorarioProyeccionDTO(
                1L, "Horario seed 7B", "BORRADOR", "OPTIMAL", 0.0, 0.0,
                "2026-07-05T00:00:00Z", List.of(sesion));
        when(service.proyectar(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/horarios/1/proyeccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sesiones.length()").value(1))
                .andExpect(jsonPath("$.sesiones[0].plazaCodigo").value("Mat-1ºA-P1"));
    }

    @Test
    void get_conIdInexistente_devuelve404() throws Exception {
        when(service.proyectar(9999L))
                .thenThrow(new IllegalArgumentException("No existe HorarioGenerado con id 9999"));

        mockMvc.perform(get("/api/horarios/9999/proyeccion"))
                .andExpect(status().isNotFound());
    }
}
