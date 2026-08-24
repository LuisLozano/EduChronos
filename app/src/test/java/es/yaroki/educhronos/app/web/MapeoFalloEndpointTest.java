package es.yaroki.educhronos.app.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Los cuatro desenlaces de un solve fallido, POR LA RED (S118). Hermano de
 * {@link MapeoFalloSolverTest}: aquel fija la tabla de traducción, éste que el
 * endpoint la USE y que lo traducido salga en la respuesta.
 *
 * <p>Servicio MOCKEADO, no real: lo que se ejerce es el borde HTTP, y montar un
 * catálogo capaz de provocar cada uno de los cuatro estados de CP-SAT sería —para
 * {@code UNKNOWN}— un solve deliberadamente largo dentro de la suite rápida
 * (D24/D25). El mock lanza la excepción con el estado que toca y el borde hace lo
 * suyo. Mismo montaje que {@link HorarioControllerHttpTest}.
 *
 * <p><b>Se asertan STATUS y BODY, nunca {@code status().reason()}.</b> El
 * {@code reason} se lee del {@code MockHttpServletResponse} y no del cuerpo que
 * viaja por la red: con {@code standaloneSetup} va poblado aunque producción no
 * escriba cuerpo alguno, y por eso un test que lo aserta se queda verde con
 * producción muda (hallazgo de S109). El cuerpo lo construimos nosotros y es lo
 * único que el navegador verá.
 */
@ExtendWith(MockitoExtension.class)
class MapeoFalloEndpointTest {

    @Mock private GeneradorHorarioService service;
    @Mock private DiagnosticoService diagnosticoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(service, diagnosticoService)).build();
    }

    /** Lanza desde la generación la excepción dada, sea cual sea el cuerpo. */
    private void elServicioFallaCon(HorarioInfactibleException e) {
        when(service.generar(any(), any(), any(), any())).thenThrow(e);
    }

    private org.springframework.test.web.servlet.ResultActions postGenerar() throws Exception {
        return mockMvc.perform(post("/api/horarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    /**
     * INFEASIBLE: CP-SAT PROBÓ que no hay solución. Único caso en que 422 dice la
     * verdad, y el único cuyo status no cambia respecto a antes de S118. Lo que
     * cambia es que ahora el cuerpo NOMBRA el hecho, que es lo que permite a la
     * vista decir "revisa el catálogo" en vez de un texto genérico.
     */
    @Test
    void infeasible_devuelve422ConCausaEnElCuerpo() throws Exception {
        elServicioFallaCon(new HorarioInfactibleException(
                "El solver no encontró un horario factible (modo optimización). "
                        + "Estado CP-SAT: INFEASIBLE", "INFEASIBLE", 600));

        postGenerar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.causa").value("CATALOGO_INFACTIBLE"));
    }

    /**
     * UNKNOWN: no se probó NADA, solo se acabó el tiempo. Los tres asertos atacan
     * mecanismos independientes y por eso van juntos —borrar uno solo debe poner el
     * test rojo—:
     *
     * <p>(a) el STATUS deja de afirmar "no tiene solución" y pasa a 503, el único
     * que autoriza a la vista a ofrecer un reintento;
     *
     * <p>(b) la cabecera {@code Retry-After}, que es la forma ESTÁNDAR de decirlo:
     * un cliente que no entienda nuestro cuerpo (un proxy, un curl) sigue sabiendo
     * que esto se reintenta. Vale 0 —adelante, ya— y no un número de espera: no hay
     * cola ni backoff, lo que faltó fue presupuesto, no disponibilidad;
     *
     * <p>(c) los SEGUNDOS de la corrida en el cuerpo. Sin ellos "se agotó el tiempo"
     * no dice cuánto tiempo era, y quien lo lee no puede juzgar si pedir más sirve
     * de algo. 600 es el valor de producción y viaja en la excepción desde el solver.
     */
    @Test
    void unknown_devuelve503ConRetryAfterYLosSegundosDeLaCorrida() throws Exception {
        elServicioFallaCon(new HorarioInfactibleException(
                "El solver no encontró un horario factible (modo optimización). "
                        + "Estado CP-SAT: UNKNOWN", "UNKNOWN", 600));

        postGenerar()
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "0"))
                .andExpect(jsonPath("$.causa").value("PRESUPUESTO_AGOTADO"))
                .andExpect(jsonPath("$.segundos").value(600));
    }

    /**
     * MODEL_INVALID: el modelo lo construimos nosotros, así que un modelo inválido es
     * un defecto propio. 4xx se lo achacaría a quien pulsó el botón.
     */
    @Test
    void modelInvalid_devuelve500ConCausaEnElCuerpo() throws Exception {
        elServicioFallaCon(new HorarioInfactibleException(
                "El solver no encontró un horario factible (modo optimización). "
                        + "Estado CP-SAT: MODEL_INVALID", "MODEL_INVALID", 600));

        postGenerar()
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.causa").value("ERROR_INTERNO"));
    }

    /**
     * La excepción de UN argumento: no hubo solve. Es la que lanza {@code ModeloCpSat}
     * al abortar la construcción del modelo (hoy: catálogo sin tramos), y su
     * {@code estado()} es null porque no existió ningún {@code CpSolver}, no porque
     * se perdiera por el camino.
     *
     * <p>Sigue siendo 422, como antes de S118: mandarlo a 500 con la rama por defecto
     * sería una regresión sobre el caso más probable de una instalación nueva. El
     * aserto del status es el que la caza; el de la causa, el que impide que se
     * confunda con un INFEASIBLE de verdad.
     */
    @Test
    void sinSolve_devuelve422YConfiguracionIncompleta() throws Exception {
        elServicioFallaCon(new HorarioInfactibleException("El problema no tiene tramos"));

        postGenerar()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.causa").value("CONFIGURACION_INCOMPLETA"));
    }
}
