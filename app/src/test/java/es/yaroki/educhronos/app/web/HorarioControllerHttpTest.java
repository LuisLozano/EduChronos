package es.yaroki.educhronos.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.exportacion.VistaPdf;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
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

    @Mock
    private ExportacionHorarioService exportacionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(
                        service, diagnosticoService, exportacionService)).build();
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

    // ------------------------------------------------------------------ S149: el PDF

    /**
     * El 200 del PDF: tipo, cabecera de descarga y CUERPO. Los bytes se afirman como los
     * que devolvió el servicio y no solo «algo no vacío»: un controlador que escribiera el
     * fichero por su cuenta —o que devolviera el cuerpo de otra ruta— pasaría un aserto
     * de longitud.
     */
    @Test
    void getPdf_conIdExistente_devuelve200ConTipoPdfYCabeceraDeDescarga() throws Exception {
        byte[] esperado = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
        when(exportacionService.pdf(1L, VistaPdf.GRUPO)).thenReturn(esperado);

        byte[] cuerpo = mockMvc.perform(get("/api/horarios/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"horario-1-grupo.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(cuerpo).isEqualTo(esperado);
    }

    /** {@code vista=grupo} explícito es el mismo caso: el defecto no es una ruta distinta. */
    @Test
    void getPdf_conVistaGrupoExplicita_devuelve200() throws Exception {
        when(exportacionService.pdf(1L, VistaPdf.GRUPO)).thenReturn(new byte[] {'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/horarios/1/pdf").param("vista", "grupo"))
                .andExpect(status().isOk());
    }

    /**
     * Una vista que no existe es 400 y NO 404: el horario está, lo que falta es la vista.
     * El valor era {@code profesor} hasta S150, cuando esa vista pasó a existir; ahora es
     * uno que no nombra nada, que es lo que este test siempre quiso decir.
     */
    @Test
    void getPdf_conVistaDesconocida_devuelve400YNoLlegaAProyectar() throws Exception {
        mockMvc.perform(get("/api/horarios/1/pdf").param("vista", "trimestre"))
                .andExpect(status().isBadRequest());

        // El rechazo es ANTES de trabajar: el servicio no llega a ser llamado.
        verifyNoInteractions(exportacionService);
    }

    /**
     * La vista de profesor enruta a su propia llamada y se lleva su nombre al fichero: un
     * controlador que ignorase el parámetro daría un 200 con el PDF de grupo dentro y un
     * nombre que además lo desmentiría.
     */
    @Test
    void getPdf_conVistaProfesor_devuelve200YFilenameDeProfesor() throws Exception {
        byte[] esperado = {'%', 'P', 'D', 'F', '-', 'p'};
        when(exportacionService.pdf(1L, VistaPdf.PROFESOR)).thenReturn(esperado);

        byte[] cuerpo = mockMvc.perform(get("/api/horarios/1/pdf").param("vista", "profesor"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"horario-1-profesor.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(cuerpo).isEqualTo(esperado);
    }

    /** La tercera vista, con su propio nombre de fichero. */
    @Test
    void getPdf_conVistaAula_devuelve200YFilenameDeAula() throws Exception {
        byte[] esperado = {'%', 'P', 'D', 'F', '-', 'a'};
        when(exportacionService.pdf(1L, VistaPdf.AULA)).thenReturn(esperado);

        byte[] cuerpo = mockMvc.perform(get("/api/horarios/1/pdf").param("vista", "aula"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"horario-1-aula.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(cuerpo).isEqualTo(esperado);
    }

    @Test
    void getPdf_conIdInexistente_devuelve404() throws Exception {
        when(exportacionService.pdf(9999L, VistaPdf.GRUPO))
                .thenThrow(new IllegalArgumentException("No existe HorarioGenerado con id 9999"));

        mockMvc.perform(get("/api/horarios/9999/pdf"))
                .andExpect(status().isNotFound());
    }
}
