package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.ActividadService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.ActividadController;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Test de integración del CRUD {@code /api/actividades} (Fase 8, Bloque 8.5-C1) POR LA RED
 * ({@code standaloneSetup} + {@code ActividadService} real sobre {@code @DataJpaTest}).
 * Actividad es un AGREGADO: se ejerce con sus plazas embebidas. Los asertos discriminantes
 * clavan cada regla de validación (XOR de aula, I7, I2 cruzando plazas, referencias por
 * código), el round-trip por CONTENIDO del bloque de 6 plazas copiado del seed, la
 * regeneración total de códigos {@code -P{n}} con {@code orphanRemoval} al reducir plazas, y
 * la exclusión de la unicidad por id en la edición.
 *
 * <p>{@link #setUp} siembra el catálogo que las plazas referencian por código (asignaturas,
 * profesores, aulas y subgrupos sobre un nivel/grupos), replicando en bucle la multiplicidad
 * del seed para poder reproducir el bloque CyR/OyD/RefMt fielmente.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ActividadService.class)
class ActividadEndpointTest {

    /**
     * Plazas de la actividad de RELLENO que el test de la travesía siembra para desalinear el id
     * de la plaza real por encima de los ids de sesion/horario/aula_bloqueada. Ver
     * {@link #borrado_actividadConTravesia_409YDesgloseSinDuplicarAulaBloqueada}.
     */
    private static final int RELLENO_PLAZAS = 3;

    @Autowired private ActividadService service;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AsignaturaAulaCompatibleRepository compatibilidadRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private AulaBloqueadaRepository aulaBloqueadaRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ActividadController(service)).build();

        for (String cod : List.of("CyR", "OyD", "RefMt", "Mat")) {
            asignaturaRepository.save(new Asignatura(cod, "Asignatura " + cod));
        }
        for (String cod : List.of("TEC3", "INF1", "FIL3", "MAT6", "MAT7", "MAT4", "MATA")) {
            profesorRepository.save(new Profesor(cod, "Profesor " + cod));
        }
        for (String cod : List.of("A5", "B07", "A12In", "A11", "A3", "A14", "A10", "A1")) {
            aulaRepository.save(new Aula(cod, TipoAula.ORDINARIA, null, null, null, null));
        }
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        List<String> sufijos = List.of("CyR-Tec", "CyR-Inf", "OyD",
                "RefMt-MAT6", "RefMt-MAT7", "RefMt-MAT4");
        for (String letra : List.of("A", "B", "C", "D")) {
            String cg = "1º" + letra;
            GrupoAdministrativo g = grupoRepository.save(
                    new GrupoAdministrativo(cg, eso1, TipoGrupo.ORDINARIO, null));
            subgrupoRepository.save(new Subgrupo(cg + "-Completo", java.util.Set.of(g)));
            for (String suf : sufijos) {
                subgrupoRepository.save(new Subgrupo(cg + "-" + suf, java.util.Set.of(g)));
            }
        }
    }

    // ─────────────────────────────────────────────────────── CRUD básico

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividadMat("Mat-1ºA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("Mat-1ºA"))
                .andExpect(jsonPath("$.patronTemporal").value("DISTRIBUIDA"))
                .andExpect(jsonPath("$.plazas.length()").value(1))
                .andExpect(jsonPath("$.plazas[0].codigo").value("Mat-1ºA-P1"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/actividades/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(actividadMat("B"));
        crear(actividadMat("A"));
        crear(actividadMat("C"));

        mockMvc.perform(get("/api/actividades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(actividadMat("Mat-1ºA"));

        mockMvc.perform(delete("/api/actividades/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/actividades/" + id))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────── asertos discriminantes

    @Test
    void xorA_aulaFijaYCandidatas_400() throws Exception {
        // XOR-a: una plaza con aula fija Y aulas candidatas no vacías → 400.
        String plaza = plazaJson("Mat", "A1", List.of("A5"), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void xorB_sinAulaFijaNiCandidatas_400() throws Exception {
        // XOR-b: una plaza sin aula fija Y con candidatas vacías → 400.
        String plaza = plazaJson("Mat", null, List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void i7_plazaSinProfesores_400() throws Exception {
        // I7: una plaza sin profesores → 400.
        String plaza = plazaJson("Mat", "A1", List.of(), List.of(), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void i2_mismoSubgrupoEnDosPlazas_400ConCodigoEnReason() throws Exception {
        // I2: el MISMO código de subgrupo en dos plazas distintas → 400 que lo NOMBRA.
        String p1 = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        String p2 = plazaJson("Mat", "A5", List.of(), List.of("MAT6"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA", p1, p2)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("1ºA-Completo")));
    }

    @Test
    void ref_profesorInexistente_400ConCodigoEnReason() throws Exception {
        // REF: un código de profesor inexistente → 400, reason contiene ese código.
        String plaza = plazaJson("Mat", "A1", List.of(), List.of("NOEXISTE"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("NOEXISTE")));
    }

    @Test
    void patronTemporalInvalido_400ConValorEnReason() throws Exception {
        String plaza = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "MALO", plaza)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("MALO")));
    }

    @Test
    void sinPlazas_400() throws Exception {
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("X", null, "DISTRIBUIDA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roundTrip_bloqueSeisPlazas_201YContenidoPorPlaza() throws Exception {
        // ROUND-TRIP: bloque CyR/OyD/RefMt copiado del seed (6 plazas, asignatura de
        // actividad null). GET asevera POR PLAZA los códigos con containsInAnyOrder, no el
        // tamaño; y las dos ramas del XOR salen bien (fija sin candidatas, candidatas sin fija).
        long id = crear(bloqueSeisPlazas());

        MvcResult res = mockMvc.perform(get("/api/actividades/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignatura").doesNotExist())
                .andExpect(jsonPath("$.plazas.length()").value(6))
                // P1 = primera del body (CyR-Tec): rama candidatas, sin aula fija.
                .andExpect(jsonPath("$.plazas[0].codigo").value("Bloque-P1"))
                .andExpect(jsonPath("$.plazas[0].aulaFija").doesNotExist())
                .andExpect(jsonPath("$.plazas[0].aulasCandidatas", containsInAnyOrder("A5", "B07")))
                .andExpect(jsonPath("$.plazas[0].profesores", containsInAnyOrder("TEC3")))
                .andExpect(jsonPath("$.plazas[0].subgrupos",
                        containsInAnyOrder("1ºA-CyR-Tec", "1ºB-CyR-Tec", "1ºC-CyR-Tec", "1ºD-CyR-Tec")))
                // P2 = segunda del body (CyR-Inf): rama aula fija, candidatas vacías.
                .andExpect(jsonPath("$.plazas[1].codigo").value("Bloque-P2"))
                .andExpect(jsonPath("$.plazas[1].aulaFija").value("A12In"))
                .andExpect(jsonPath("$.plazas[1].aulasCandidatas", empty()))
                .andExpect(jsonPath("$.plazas[1].profesores", containsInAnyOrder("INF1")))
                .andReturn();

        // La plaza de asignatura propia lleva su asignatura, aunque la actividad sea null.
        String asigP1 = JsonPath.read(res.getResponse().getContentAsString(), "$.plazas[0].asignatura");
        org.junit.jupiter.api.Assertions.assertEquals("CyR", asigP1);
    }

    @Test
    void putReduccion_deSeisADosPlazas_200SinColisionYSupervivientesConservanCodigo() throws Exception {
        // PUT-REDUCCIÓN: alta con 6 plazas, PUT de la MISMA con 2. La reconciliación posicional
        // borra las 4 sobrantes (orphanRemoval) y actualiza el contenido de las 2 primeras SIN
        // reasignar su código → sin violación de UNIQUE. GET posterior → exactamente 2, con los
        // MISMOS códigos que las 2 primeras plazas del POST (no basta con "hay 2").
        long id = crear(bloqueSeisPlazas());
        List<String> codigosPost = codigosDePlazas(id);   // [Bloque-P1 .. Bloque-P6]

        String p1 = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        String p2 = plazaJson("Mat", "A5", List.of(), List.of("MAT6"), List.of("1ºB-Completo"));
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("Bloque", null, "NEUTRA", p1, p2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas.length()").value(2));

        List<String> codigosPut = codigosDePlazas(id);
        org.junit.jupiter.api.Assertions.assertEquals(2, codigosPut.size());
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(codigosPost.get(0), codigosPost.get(1)), codigosPut,
                "las 2 supervivientes conservan el codigo de las 2 primeras del POST");
    }

    @Test
    void putEstabilidad_editarContenidoNoRegeneraCodigos() throws Exception {
        // PUT-ESTABILIDAD (corazón de la reevaluación): POST 3 plazas; PUT 3 en las MISMAS
        // posiciones pero cambiando el profesor de la plaza 2. Los 3 códigos son IDÉNTICOS a
        // los del POST y el contenido de la plaza 2 refleja el cambio → editar contenido NO
        // regenera códigos.
        String a1 = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        String a2 = plazaJson("Mat", "A5", List.of(), List.of("MAT6"), List.of("1ºB-Completo"));
        String a3 = plazaJson("Mat", "A10", List.of(), List.of("MAT7"), List.of("1ºC-Completo"));
        long id = crear(actividad("Bloque", null, "NEUTRA", a1, a2, a3));
        List<String> codigosPost = codigosDePlazas(id);

        // Plaza 2 (posición 1) cambia de profesor MAT6 → MAT4; posiciones 0 y 2 iguales.
        String b2 = plazaJson("Mat", "A5", List.of(), List.of("MAT4"), List.of("1ºB-Completo"));
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("Bloque", null, "NEUTRA", a1, b2, a3)))
                .andExpect(status().isOk());

        List<String> codigosPut = codigosDePlazas(id);
        org.junit.jupiter.api.Assertions.assertEquals(codigosPost, codigosPut,
                "editar contenido no debe regenerar los codigos de plaza");
        mockMvc.perform(get("/api/actividades/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas[1].codigo").value("Bloque-P2"))
                .andExpect(jsonPath("$.plazas[1].profesores", containsInAnyOrder("MAT4")));
    }

    @Test
    void putCrecimiento_deDosACuatro_conservaDosYCreaDosFrescas() throws Exception {
        // PUT-CRECIMIENTO simple: POST 2 → PUT 4 → 200, GET 4; las 2 primeras conservan código
        // (P1,P2) y las 2 nuevas tienen códigos frescos sin colisión (P3,P4).
        String a1 = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        String a2 = plazaJson("Mat", "A5", List.of(), List.of("MAT6"), List.of("1ºB-Completo"));
        long id = crear(actividad("Bloque", null, "NEUTRA", a1, a2));

        String a3 = plazaJson("Mat", "A10", List.of(), List.of("MAT7"), List.of("1ºC-Completo"));
        String a4 = plazaJson("Mat", "A11", List.of(), List.of("MAT4"), List.of("1ºD-Completo"));
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("Bloque", null, "NEUTRA", a1, a2, a3, a4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas.length()").value(4));

        mockMvc.perform(get("/api/actividades/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas.length()").value(4))
                .andExpect(jsonPath("$.plazas[0].codigo").value("Bloque-P1"))
                .andExpect(jsonPath("$.plazas[1].codigo").value("Bloque-P2"))
                .andExpect(jsonPath("$.plazas[*].codigo",
                        containsInAnyOrder("Bloque-P1", "Bloque-P2", "Bloque-P3", "Bloque-P4")));
    }

    @Test
    void putReusoDeHueco_trasBorrarP3_laNuevaReusaP3() throws Exception {
        // PUT-REUSO-DE-HUECO (sustituye al requisito inventado anti-reuso): POST 3 (P1,P2,P3);
        // PUT 2 → quedan P1,P2 (P3 borrada); PUT 3 → la nueva reusa el hueco: máximo vivo
        // {1,2}+1 = 3 → {cod}-P3. GET: 3 plazas, la nueva es Bloque-P3, códigos únicos.
        String a1 = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        String a2 = plazaJson("Mat", "A5", List.of(), List.of("MAT6"), List.of("1ºB-Completo"));
        String a3 = plazaJson("Mat", "A10", List.of(), List.of("MAT7"), List.of("1ºC-Completo"));
        long id = crear(actividad("Bloque", null, "NEUTRA", a1, a2, a3));

        // PUT a 2: quedan P1,P2; P3 borrada.
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("Bloque", null, "NEUTRA", a1, a2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas.length()").value(2));

        // PUT a 3: la nueva (posición 2) reusa el hueco P3.
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("Bloque", null, "NEUTRA", a1, a2, a3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plazas.length()").value(3));

        List<String> codigos = codigosDePlazas(id);
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of("Bloque-P1", "Bloque-P2", "Bloque-P3"), codigos,
                "la nueva reusa el hueco P3 y los codigos vivos son unicos");
    }

    @Test
    void unicidadEdicion_mismoCodigo_200() throws Exception {
        // UNICIDAD-EDICIÓN: PUT sobre una actividad con su MISMO código → 200 (no
        // tautológico: sin la exclusión por id, findByCodigo se pisaría y daría 400).
        long id = crear(actividadMat("Mat-1ºA"));

        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividadMat("Mat-1ºA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("Mat-1ºA"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/actividades/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividadMat("Mat-1ºA")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(actividadMat("Mat-1ºA"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividadMat("Mat-1ºA")))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────── borrado amable (8.5-C2b)

    /**
     * CASO PROPIO del borrado amable (8.5-C2b): una actividad NO se borra si alguien retiene sus
     * PLAZAS desde fuera del agregado, aunque las plazas mismas cascadeen. Monta la actividad con
     * 1 plaza, y sobre esa plaza una {@link Sesion} (FK RESTRICT {@code sesion.plaza_id}) y una
     * {@link AulaBloqueada} (que apunta a la actividad por DOS FK: {@code actividad_id} y
     * {@code plaza_id}). El DELETE devuelve 409 con el desglose de la travesía.
     *
     * <p><b>Dos discriminantes, verificados por mutación (D-C2b-5).</b>
     * <ul>
     *   <li><b>Travesía de sesion.</b> El conteo sale de {@code plaza_id in (select id from plaza
     *       where actividad_id=:id)}. Como {@link #RELLENO_PLAZAS} desalinea el id de la plaza real
     *       de los ids de la actividad, la sesion y el horario, una consulta que apuntara a la
     *       columna equivocada contaría 0 y el test caería (comprobado apuntándola a otra columna).
     *   <li><b>Fusión de aula_bloqueada.</b> La única fila de {@code aula_bloqueada} satisface las
     *       dos FK a la vez; el {@code or} de la {@code @Query} las une y el {@code count} la cuenta
     *       UNA vez. El aserto exige conteo 1, NO 2: si el {@code or} fuese una suma de dos counts,
     *       daría 2 y {@code containsExactly} caería.
     * </ul>
     * El {@code containsExactly} cierra además el flanco negativo: {@code sesion(es) bloqueada(s)}
     * (0 filas) NO aparece, y ningún referente sobra.
     */
    @Test
    void borrado_actividadConTravesia_409YDesgloseSinDuplicarAulaBloqueada() throws Exception {
        // Catálogo del setUp reutilizable: la asignatura "Mat" y el aula "A5" ya existen.
        Asignatura mat = asignaturaRepository.findByCodigo("Mat").orElseThrow();
        Aula aula = aulaRepository.findByCodigo("A5").orElseThrow();
        TramoSemanal tramo = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));

        // DESALINEADO (lección del test 1): una actividad de relleno con RELLENO_PLAZAS plazas
        // empuja el id de la plaza real por encima de los ids (bajos) de sesion/horario/
        // aula_bloqueada, para que ninguna @Query que apunte a la columna equivocada acierte por
        // colisión. La precondición de más abajo lo verifica en runtime, no se fía de la aritmética.
        Actividad relleno = new Actividad("RELLENO", mat, 1, 1, PatronTemporal.NEUTRA, false);
        for (int i = 1; i <= RELLENO_PLAZAS; i++) {
            relleno.agregarPlaza("RELLENO-P" + i, mat, aula, Set.of(), Set.of(), Set.of());
        }
        actividadRepository.save(relleno);

        // Actividad real: 1 plaza con aula fija = A5.
        Actividad actividad = new Actividad("ACT", mat, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza plaza = actividad.agregarPlaza("ACT-P1", mat, aula, Set.of(), Set.of(), Set.of());
        actividadRepository.save(actividad);
        entityManager.flush();

        // 1 Sesion (travesía) y 1 AulaBloqueada (fusión), AMBAS sobre la plaza real.
        HorarioGenerado horario = horarioRepository.save(
                new HorarioGenerado("H", Instant.now(), "OPTIMAL", 0.0, 0.0));
        Sesion sesion = sesionRepository.save(new Sesion(horario, plaza, 1, tramo, aula));
        AulaBloqueada aulaBloqueada = aulaBloqueadaRepository.save(
                new AulaBloqueada(actividad, 1, plaza, aula));
        entityManager.flush();

        // Precondición del desalineado: idAct e idPlaza no colisionan entre sí ni con los ids de
        // sesion/horario/aula_bloqueada. Si un cambio futuro los realineara, esto cae ruidosamente.
        assertThat(actividad.getId()).isNotEqualTo(plaza.getId());
        assertThat(List.of(sesion.getId(), horario.getId(), aulaBloqueada.getId()))
                .doesNotContain(actividad.getId(), plaza.getId());

        // (a) 409, no el 500 del mordisco de las FK RESTRICT sesion.plaza_id / aula_bloqueada.*
        mockMvc.perform(delete("/api/actividades/" + actividad.getId()))
                .andExpect(status().isConflict());

        // (b) Desglose: la sesion (travesía) y el aula_bloqueada (contado UNA vez pese a las 2 FK).
        ReferenciaEntranteException error = catchThrowableOfType(
                () -> service.borrar(actividad.getId()), ReferenciaEntranteException.class);
        assertThat(error).isNotNull();
        assertThat(error.getReferencias()).containsExactly(
                new Referencia("sesion(es)", 1L),
                new Referencia("aula(s) bloqueada(s)", 1L));
    }

    // ───────────────── I3: compatibilidad asignatura↔tipo de aula (§4.7, Bloque 8.5-C3)

    /**
     * Fixture SINTÉTICO de I3 (no del seed): asignatura {@code X} compatible SOLO con
     * {@code INFORMATICA}, un aula {@code ORD} de tipo {@code ORDINARIA} y un aula {@code INF}
     * de tipo {@code INFORMATICA}. Reutiliza el profesor {@code MATA} y el subgrupo
     * {@code 1ºA-Completo} del {@code setUp}.
     */
    private void fixtureI3() {
        Asignatura x = asignaturaRepository.save(new Asignatura("X", "Asignatura X"));
        aulaRepository.save(new Aula("ORD", TipoAula.ORDINARIA, null, null, null, null));
        aulaRepository.save(new Aula("INF", TipoAula.INFORMATICA, null, null, null, null));
        compatibilidadRepository.save(new AsignaturaAulaCompatible(x, TipoAula.INFORMATICA));
    }

    @Test
    void i3_aulaFijaIncompatible_400ConAsignaturaAulaYTipo() throws Exception {
        // X admite solo INFORMATICA; aula fija ORD es ORDINARIA → 400 que nombra X, ORD y ORDINARIA.
        fixtureI3();
        String plaza = plazaJson("X", "ORD", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActX", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("X")))
                .andExpect(status().reason(containsString("ORD")))
                .andExpect(status().reason(containsString("ORDINARIA")));
    }

    @Test
    void i3_aulaFijaCompatible_201() throws Exception {
        fixtureI3();
        String plaza = plazaJson("X", "INF", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActX", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isCreated());
    }

    @Test
    void i3_candidatasConUnaMala_400() throws Exception {
        // Candidatas {INF, ORD}: una mala (ORD) basta para abortar.
        fixtureI3();
        String plaza = plazaJson("X", null, List.of("INF", "ORD"), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActX", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("ORD")));
    }

    @Test
    void i3_candidatasTodasCompatibles_201() throws Exception {
        fixtureI3();
        String plaza = plazaJson("X", null, List.of("INF"), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActX", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isCreated());
    }

    @Test
    void i3_asignaturaSinCompatibilidades_aulaOrdinaria_201_DISCRIMINANTE_C() throws Exception {
        // ASERTO DISCRIMINANTE DE LA SEMÁNTICA (C): "Y" no declara compatibilidades → irrestricta →
        // una plaza con aula ORDINARIA pasa (201). Si alguien cambiara la semántica a
        // "vacío = nada permitido", este 201 se volvería 400 y ESTE test caería. Es su razón de ser.
        asignaturaRepository.save(new Asignatura("Y", "Asignatura Y sin compatibilidades"));
        aulaRepository.save(new Aula("ORD", TipoAula.ORDINARIA, null, null, null, null));
        String plaza = plazaJson("Y", "ORD", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActY", null, "DISTRIBUIDA", plaza)))
                .andExpect(status().isCreated());
    }

    @Test
    void i3_muerdeEnElPut_noSoloEnElPost() throws Exception {
        // I3 vive en el punto único que atraviesan alta y edición: un PUT que cambia la plaza a
        // un aula incompatible (aplicarContenido) muerde igual que el POST.
        fixtureI3();
        long id = crear(actividad("ActX", null, "DISTRIBUIDA",
                plazaJson("X", "INF", List.of(), List.of("MATA"), List.of("1ºA-Completo"))));
        String malo = plazaJson("X", "ORD", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        mockMvc.perform(put("/api/actividades/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actividad("ActX", null, "DISTRIBUIDA", malo)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("ORDINARIA")));
    }

    // ─────────────────────────────────────────────────── helpers de fixture

    /** Actividad Mat de 1 plaza fija (A1, prof MATA, subgrupo 1ºA-Completo). */
    private static String actividadMat(String codigo) {
        String plaza = plazaJson("Mat", "A1", List.of(), List.of("MATA"), List.of("1ºA-Completo"));
        return actividad(codigo, "Mat", "DISTRIBUIDA", plaza);
    }

    /** El bloque CyR/OyD/RefMt del seed: 6 plazas, asignatura de actividad null, NEUTRA. */
    private static String bloqueSeisPlazas() {
        String p1 = plazaJson("CyR", null, List.of("A5", "B07"), List.of("TEC3"), across("CyR-Tec"));
        String p2 = plazaJson("CyR", "A12In", List.of(), List.of("INF1"), across("CyR-Inf"));
        String p3 = plazaJson("OyD", null, List.of("A11", "A5"), List.of("FIL3"), across("OyD"));
        String p4 = plazaJson("RefMt", null, List.of("A3", "A11"), List.of("MAT6"), across("RefMt-MAT6"));
        String p5 = plazaJson("RefMt", null, List.of("A14", "A3"), List.of("MAT7"), across("RefMt-MAT7"));
        String p6 = plazaJson("RefMt", null, List.of("A10", "A14"), List.of("MAT4"), across("RefMt-MAT4"));
        return actividad("Bloque", null, "NEUTRA", p1, p2, p3, p4, p5, p6);
    }

    /** Los 4 subgrupos 1ºA..1ºD para un sufijo (equivale a {@code across(...)} del seed). */
    private static List<String> across(String sufijo) {
        return List.of("1ºA-" + sufijo, "1ºB-" + sufijo, "1ºC-" + sufijo, "1ºD-" + sufijo);
    }

    private static String plazaJson(String asignatura, String aulaFija, List<String> aulasCandidatas,
            List<String> profesores, List<String> subgrupos) {
        return "{\"asignatura\":" + quotedOrNull(asignatura)
                + ",\"aulaFija\":" + quotedOrNull(aulaFija)
                + ",\"aulasCandidatas\":" + arr(aulasCandidatas)
                + ",\"profesores\":" + arr(profesores)
                + ",\"subgrupos\":" + arr(subgrupos) + "}";
    }

    private static String actividad(String codigo, String asignatura, String patron, String... plazas) {
        return "{\"codigo\":\"" + codigo + "\""
                + ",\"asignatura\":" + quotedOrNull(asignatura)
                + ",\"duracionTramos\":1"
                + ",\"repeticionesPorSemana\":1"
                + ",\"patronTemporal\":\"" + patron + "\""
                + ",\"requiereTutor\":false"
                + ",\"plazas\":[" + String.join(",", plazas) + "]}";
    }

    private static String arr(List<String> xs) {
        return xs.stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private static String quotedOrNull(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    /** Códigos de las plazas de una actividad, en el orden (por código) que devuelve el GET. */
    private List<String> codigosDePlazas(long id) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/actividades/" + id))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.plazas[*].codigo");
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
