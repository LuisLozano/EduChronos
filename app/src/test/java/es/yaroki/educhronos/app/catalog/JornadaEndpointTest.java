package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.JornadaService;
import es.yaroki.educhronos.app.web.JornadaController;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import java.time.LocalTime;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del recurso singleton {@code /api/jornada} (C-jornada M3). Ejerce
 * el {@code GET} y el {@code PUT} POR LA RED ({@code standaloneSetup} + {@link JornadaService}
 * real sobre {@code @DataJpaTest}), y baja al repositorio para los asertos que el JSON no
 * puede ver ({@code siguienteInmediato}).
 *
 * <p><b>La frontera es asimétrica</b>: el {@code PUT} recibe UN DÍA TIPO y el backend lo
 * expande a LUNES..VIERNES; el {@code GET} devuelve siempre la semana completa. Por eso
 * los fixtures del PUT mandan 5 ó 7 tramos y los asertos cuentan 25 ó 35 filas.
 *
 * <p>Los tres asertos críticos son {@link #get_bdVacia_devuelveMallaDeReferenciaNoPersistida}
 * —la propuesta que hereda del difunto {@code SeedCatalogoRunner}, con el {@code orden}
 * global CONTINUO entre días, que un contador reiniciado por día rompería—,
 * {@link #put_diaTipo_seExpandeALosCincoDiasConOrdenContinuo} —el corazón del contrato
 * nuevo— y {@link #put_septimoLectivoEnElDiaTipo_400CitaElTecho}, que es la frontera con
 * el techo duro de {@code solver.domain.Tramo}: sin esa validación la malla se guardaría y
 * el fallo saldría mucho después, al generar horario.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JornadaService.class)
class JornadaEndpointTest {

    @Autowired private JornadaService service;
    @Autowired private TramoSemanalRepository tramos;
    @Autowired private ProfesorRepository profesores;
    @Autowired private ProfesorRestriccionHorariaRepository restricciones;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JornadaController(service)).build();
    }

    // ─────────────────────────────────────────────────────────────────── GET (contrato intacto)

    @Test
    void get_bdVacia_devuelveMallaDeReferenciaNoPersistida() throws Exception {
        mockMvc.perform(get("/api/jornada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistida").value(false))
                .andExpect(jsonPath("$.tramos.length()").value(35))
                // Primer lectivo del lunes.
                .andExpect(jsonPath("$.tramos[0].dia").value("LUNES"))
                .andExpect(jsonPath("$.tramos[0].horaInicio").value("08:00"))
                .andExpect(jsonPath("$.tramos[0].horaFin").value("09:00"))
                .andExpect(jsonPath("$.tramos[0].esLectivo").value(true))
                .andExpect(jsonPath("$.tramos[0].orden").value(1))
                .andExpect(jsonPath("$.tramos[0].ordenEnDia").value(1))
                // El recreo: cuarto tramo del día, sin ordenEnDia.
                .andExpect(jsonPath("$.tramos[3].horaInicio").value("11:00"))
                .andExpect(jsonPath("$.tramos[3].horaFin").value("11:30"))
                .andExpect(jsonPath("$.tramos[3].esLectivo").value(false))
                .andExpect(jsonPath("$.tramos[3].orden").value(4))
                // El lectivo que sigue al recreo retoma la numeración en 4, no en 5.
                .andExpect(jsonPath("$.tramos[4].esLectivo").value(true))
                .andExpect(jsonPath("$.tramos[4].orden").value(5))
                .andExpect(jsonPath("$.tramos[4].ordenEnDia").value(4))
                .andExpect(jsonPath("$.tramos[6].ordenEnDia").value(6))
                // ORDEN GLOBAL CONTINUO: el martes arranca en 8, no en 1. Un contador
                // reiniciado por día pasaría todos los asertos del lunes y caería aquí.
                .andExpect(jsonPath("$.tramos[7].dia").value("MARTES"))
                .andExpect(jsonPath("$.tramos[7].horaInicio").value("08:00"))
                .andExpect(jsonPath("$.tramos[7].orden").value(8))
                .andExpect(jsonPath("$.tramos[7].ordenEnDia").value(1))
                .andExpect(jsonPath("$.tramos[34].dia").value("VIERNES"))
                .andExpect(jsonPath("$.tramos[34].orden").value(35));
    }

    @Test
    void get_bdVacia_recreoSinOrdenEnDia() {
        // El null de ordenEnDia se asevera contra el DTO, no contra el JSON: jsonPath no
        // distingue "campo ausente" de "campo a null" con la claridad que hace falta aquí.
        JornadaDTO jornada = service.obtenerJornada();
        assertThat(jornada.persistida()).isFalse();
        assertThat(jornada.tramos().get(3).esLectivo()).isFalse();
        assertThat(jornada.tramos().get(3).ordenEnDia()).isNull();
        assertThat(jornada.tramos().get(2).ordenEnDia()).isEqualTo(3);
    }

    @Test
    void get_trasPersistir_persistidaTrue() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jornada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistida").value(true))
                // 5 tramos de día tipo × 5 días.
                .andExpect(jsonPath("$.tramos.length()").value(25));
    }

    // ─────────────────────────────────────────────────────────────────── PUT: expansión

    @Test
    void put_diaTipo_seExpandeALosCincoDiasConOrdenContinuo() throws Exception {
        // Corazón del contrato nuevo: entran los 7 tramos de un día, salen las 35 filas de
        // la semana. El fixture es la malla de referencia (6 lectivos + recreo tras el 3.º).
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoDeReferencia()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistida").value(true))
                .andExpect(jsonPath("$.tramos.length()").value(35));

        List<TramoSemanal> persistidos = tramos.findAllByOrderByOrdenAsc();
        assertThat(persistidos).hasSize(35);

        // Los cinco días están, con 7 filas cada uno.
        assertThat(persistidos).extracting(TramoSemanal::getDia)
                .containsOnly(Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES);
        for (Dia dia : Dia.values()) {
            List<TramoSemanal> delDia = persistidos.stream().filter(t -> t.getDia() == dia).toList();
            assertThat(delDia).as("tramos de " + dia).hasSize(7);
            // Cada día es RÉPLICA EXACTA del día tipo, en horas y en lectividad.
            assertThat(delDia).extracting(TramoSemanal::getHoraInicio)
                    .containsExactly(
                            LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                            LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(12, 30),
                            LocalTime.of(13, 30));
            assertThat(delDia).extracting(TramoSemanal::isEsLectivo)
                    .containsExactly(true, true, true, false, true, true, true);
        }

        // ORDEN CONTINUO CRUZANDO EL LÍMITE DE DÍA: el lunes acaba en 7 y el martes empieza
        // en 8. Un contador reiniciado por día daría 1..7 cinco veces y caería aquí.
        assertThat(persistidos).extracting(TramoSemanal::getOrden)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 35).boxed().toList());
        assertThat(persistidos.get(6).getDia()).isEqualTo(Dia.LUNES);
        assertThat(persistidos.get(6).getOrden()).isEqualTo(7);
        assertThat(persistidos.get(7).getDia()).isEqualTo(Dia.MARTES);
        assertThat(persistidos.get(7).getOrden()).isEqualTo(8);

        // El ordenEnDia derivado reinicia en cada día pese al orden global continuo.
        JornadaDTO jornada = service.obtenerJornada();
        assertThat(jornada.tramos().get(6).ordenEnDia()).isEqualTo(6);
        assertThat(jornada.tramos().get(7).dia()).isEqualTo("MARTES");
        assertThat(jornada.tramos().get(7).ordenEnDia()).isEqualTo(1);
    }

    @Test
    void put_diaTipoValido_persisteConOrdenGlobalYOrdenEnDiaDerivado() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistida").value(true))
                .andExpect(jsonPath("$.tramos.length()").value(25))
                // orden global 1..25 tras expandir...
                .andExpect(jsonPath("$.tramos[0].orden").value(1))
                .andExpect(jsonPath("$.tramos[24].orden").value(25))
                // ...y ordenEnDia derivado, saltándose el recreo y reiniciando en el martes.
                .andExpect(jsonPath("$.tramos[0].ordenEnDia").value(1))
                .andExpect(jsonPath("$.tramos[1].ordenEnDia").value(2))
                .andExpect(jsonPath("$.tramos[2].ordenEnDia").value(3))
                .andExpect(jsonPath("$.tramos[4].ordenEnDia").value(4))
                .andExpect(jsonPath("$.tramos[5].dia").value("MARTES"))
                .andExpect(jsonPath("$.tramos[5].orden").value(6))
                .andExpect(jsonPath("$.tramos[5].ordenEnDia").value(1));

        JornadaDTO jornada = service.obtenerJornada();
        // El recreo, otra vez contra el DTO: sin ordenEnDia pero CON orden global.
        TramoJornadaDTO recreo = jornada.tramos().get(3);
        assertThat(recreo.esLectivo()).isFalse();
        assertThat(recreo.orden()).isEqualTo(4);
        assertThat(recreo.ordenEnDia()).isNull();

        List<TramoSemanal> persistidos = tramos.findAllByOrderByOrdenAsc();
        assertThat(persistidos).hasSize(25);
        assertThat(persistidos.get(0).getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        // siguienteInmediato NO se deriva (deuda registrada): null en todos.
        assertThat(persistidos).allSatisfy(
                t -> assertThat(t.getSiguienteInmediato()).isNull());
    }

    @Test
    void put_dosVecesElMismoDiaTipo_idempotente() throws Exception {
        // Guarda de regresión del delete→flush→insert: la segunda pasada reinserta los
        // mismos (dia, orden) que ya existían. Hoy no hay UNIQUE sobre (dia, orden) en
        // schema.sql, así que este test NO discrimina el flush por sí solo; lo que fija es
        // el contrato observable (reemplazo total idempotente, sin filas duplicadas) y
        // quedará listo si mañana se añade esa UNIQUE.
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos.length()").value(25));

        assertThat(tramos.count()).isEqualTo(25);
    }

    // ─────────────────────────────────────────────────────────────────── PUT inválido (400)

    @Test
    void put_septimoLectivoEnElDiaTipo_400CitaElTecho() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lectivosSeguidos(7))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("6")))
                .andExpect(status().reason(containsString("7")));

        assertThat(tramos.count()).as("un dia tipo rechazado no persiste nada").isZero();
    }

    @Test
    void put_seisLectivosEnElDiaTipo_esElLimiteYPasa() throws Exception {
        // Frontera exacta del techo: 6 lectivos SÍ. Un "< 6" en vez de "> 6" caería aquí.
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lectivosSeguidos(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos.length()").value(30))
                .andExpect(jsonPath("$.tramos[5].ordenEnDia").value(6));
    }

    @Test
    void put_septimoTramoNoLectivo_noCuentaParaElTecho() throws Exception {
        // El techo cuenta LECTIVOS, no tramos: 6 lectivos + 1 recreo son 7 filas de día tipo
        // y es legal (es exactamente la malla de referencia).
        String[] seisMasRecreo = new String[7];
        String[] seis = lectivosSeguidos(6);
        System.arraycopy(seis, 0, seisMasRecreo, 0, 6);
        seisMasRecreo[6] = tramo("14:00", "14:30", false);

        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(seisMasRecreo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos.length()").value(35));
    }

    @Test
    void put_horaNoParseable_400NombraElValor() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(tramo("8am", "09:00", true))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("8am")))
                .andExpect(status().reason(containsString("HH:mm")));
    }

    @Test
    void put_horaInicioNoAnteriorAHoraFin_400() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(tramo("10:00", "09:00", true))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("horaInicio")));
    }

    @Test
    void put_horasSolapadasEnElDiaTipo_400() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                tramo("08:00", "09:30", true),
                                tramo("09:00", "10:00", true))))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("solapad")));
    }

    @Test
    void put_tramosEncadenadosSinHueco_noEsSolape() throws Exception {
        // Intervalos semiabiertos [inicio, fin): 09:00-10:00 tras 08:00-09:00 es legal, y es
        // justamente la malla de referencia. Un solape comparado con <= la rechazaría.
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                tramo("08:00", "09:00", true),
                                tramo("09:00", "10:00", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos.length()").value(10));
    }

    @Test
    void put_listaVacia_400() throws Exception {
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tramos\":[]}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────── PUT bloqueado (409)

    @Test
    void put_conRestriccionHorariaViva_409ConDesglose() throws Exception {
        sembrarRestriccionHoraria();

        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isConflict())
                .andExpect(status().reason(containsString("restricciones horarias")))
                .andExpect(status().reason(containsString("1")));

        // La guarda corre ANTES de borrar y de expandir: el tramo sembrado sigue ahí.
        assertThat(tramos.count()).isEqualTo(1);
    }

    @Test
    void put_conRestriccionHorariaViva_desgloseSoloNombraAlReferenteVivo() throws Exception {
        sembrarRestriccionHoraria();

        // Sesiones y sesiones bloqueadas están a cero: ReferenciaEntranteException filtra los
        // conteos 0, así que NO deben aparecer en el mensaje.
        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diaTipoMinimo()))
                .andExpect(status().isConflict())
                .andExpect(status().reason(
                        org.hamcrest.Matchers.not(containsString("sesiones de horario"))));
    }

    @Test
    void put_conDiaTipoInvalidoYDependienteVivo_400NoConflicto() throws Exception {
        // El orden de los pasos es contrato: validar SIEMPRE antes de la guarda, para que un
        // cuerpo malo se conteste 400 aunque además haya dependientes.
        sembrarRestriccionHoraria();

        mockMvc.perform(put("/api/jornada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lectivosSeguidos(7))))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────── helpers

    /** Siembra un tramo + un profesor + una restricción horaria que apunta al tramo. */
    private void sembrarRestriccionHoraria() {
        TramoSemanal tramo = tramos.saveAndFlush(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        Profesor profesor = profesores.saveAndFlush(new Profesor("XX", "Profesor XX"));
        restricciones.saveAndFlush(new ProfesorRestriccionHoraria(
                profesor, tramo, TipoRestriccion.DURA, 0, "no disponible"));
        entityManager.clear();
    }

    /**
     * Día tipo mínimo y discriminante: 3 lectivos + recreo + 1 lectivo, para que
     * {@code ordenEnDia} tenga que saltarse el recreo. Expande a 25 filas (5 × 5 días).
     */
    private static String diaTipoMinimo() {
        return body(
                tramo("08:00", "09:00", true),
                tramo("09:00", "10:00", true),
                tramo("10:00", "11:00", true),
                tramo("11:00", "11:30", false),
                tramo("11:30", "12:30", true));
    }

    /** El día tipo de la MALLA DE REFERENCIA: 6 lectivos con recreo tras el 3.º. Expande a 35. */
    private static String diaTipoDeReferencia() {
        return body(
                tramo("08:00", "09:00", true),
                tramo("09:00", "10:00", true),
                tramo("10:00", "11:00", true),
                tramo("11:00", "11:30", false),
                tramo("11:30", "12:30", true),
                tramo("12:30", "13:30", true),
                tramo("13:30", "14:30", true));
    }

    /** {@code n} tramos lectivos encadenados de 60' desde las 08:00 (sin solapes). */
    private static String[] lectivosSeguidos(int n) {
        String[] resultado = new String[n];
        for (int i = 0; i < n; i++) {
            resultado[i] = tramo(
                    String.format("%02d:00", 8 + i), String.format("%02d:00", 9 + i), true);
        }
        return resultado;
    }

    /** {@code {"horaInicio":..,"horaFin":..,"esLectivo":..}} — sin {@code dia}: lo pone el backend. */
    private static String tramo(String horaInicio, String horaFin, boolean esLectivo) {
        return "{\"horaInicio\":\"" + horaInicio + "\",\"horaFin\":\"" + horaFin
                + "\",\"esLectivo\":" + esLectivo + "}";
    }

    /** {@code {"tramos":[..]}} */
    private static String body(String... tramosJson) {
        return "{\"tramos\":[" + String.join(",", tramosJson) + "]}";
    }
}
