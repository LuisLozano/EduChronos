package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.MovimientoInstanciaService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.MovimientoInstanciaController;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests del {@code PUT /api/horarios/{id}/instancias/intercambio} (S144,
 * C-intercambiar-instancias): PERMUTAR los tramos de dos instancias.
 *
 * <p>Hermano de {@code MovimientoInstanciaEndpointTest} y con su mismo molde: vive en
 * {@code app.catalog} por el ctor {@code protected} de {@code Actividad}/{@code Plaza},
 * corre sobre SQLite real ({@code replace = NONE}) dentro de la transacción única de
 * {@code @DataJpaTest} —la sesión de Hibernate que preserva la IDENTIDAD DE OBJETO de
 * {@code TramoSemanal} (S62)— y persiste las sesiones A MANO, sin pasar por el solver.
 *
 * <p>Malla: LUNES y MARTES, 3 tramos lectivos cada uno (ordenEnDia 1..3).
 *
 * <p>Colocación de partida, sin ninguna violación dura:
 * <pre>
 *   LUNES-1: MAT#1 (1ºA-s1)  +  CO#1 (1ºC, recursos propios: coexisten sin pisarse)
 *   LUNES-2: LEN#1 (1ºA-s2)
 *   LUNES-3: EF#1  (1ºB)          MARTES-1: DESD#1 (6 plazas, 1ºB-d1..6)
 *   MARTES-3: EF#2 (1ºB)
 * </pre>
 * MAT y LEN comparten GRUPO (1ºA) por subgrupos distintos: intercambiarlas es legal
 * —el grupo sigue ocupando los mismos dos tramos, uno cada una— y es justo el caso que
 * un veredicto ingenuo, emitido tras mover solo una, rechazaría.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, DiagnosticoService.class, MovimientoInstanciaService.class})
class IntercambioInstanciasEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService generadorService;
    @Autowired private DiagnosticoService diagnosticoService;
    @Autowired private MovimientoInstanciaService movimientoService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private SesionBloqueadaRepository pinTramoRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;

    private MockMvc mockMvc;

    /** MAT y LEN comparten GRUPO (subgrupos distintos del mismo 1ºA), no subgrupo. */
    private static final String MAT = "MAT-1A";
    private static final String LEN = "LEN-1A";
    /** DISTRIBUIDA con 2 repeticiones: #1 el lunes, #2 el martes. */
    private static final String EF = "EF-1B";
    /** Desdoble de 6 plazas: la instancia multifila con la que se mide el aula. */
    private static final String DESD = "DESD-6";
    /** Convive con MAT en LUNES-1 sin pisarla: da el caso "mismo tramo". */
    private static final String CO = "CO-1C";
    /** Comparte profesor con LEN; solo la usa el caso de violación preexistente. */
    private static final String HIS = "HIS-1D";

    private Long horarioId;

    @BeforeEach
    void montar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MovimientoInstanciaController(movimientoService),
                        new HorarioController(generadorService, diagnosticoService))
                .build();
    }

    // ------------------------------------------------------------------ 1-4: verdes

    /** (1) El 200 devuelve cada instancia en el tramo que ocupaba la otra. */
    @Test
    void intercambioValido200YCadaUnaApareceEnElTramoDeLaOtra() throws Exception {
        poblar();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primera.length()").value(1))
                .andExpect(jsonPath("$.segunda.length()").value(1))
                .andExpect(jsonPath("$.primera[0].actividadCodigo").value(MAT))
                .andExpect(jsonPath("$.primera[0].dia").value(1))
                .andExpect(jsonPath("$.primera[0].tramo").value(2)) // donde estaba LEN
                .andExpect(jsonPath("$.segunda[0].actividadCodigo").value(LEN))
                .andExpect(jsonPath("$.segunda[0].dia").value(1))
                .andExpect(jsonPath("$.segunda[0].tramo").value(1)); // donde estaba MAT
    }

    /**
     * (2) Y la PERMUTA está en la tabla, no solo en el cuerpo. Leído tras
     * {@code flush()/clear()}: de la tabla, no del contexto de persistencia.
     */
    @Test
    void lasFilasDeAmbasQuedanPersistidasEnElTramoPermutado() throws Exception {
        poblar();
        Long tramoMat = filasDe(MAT, 1).get(0).getTramoInicio().getId();
        Long tramoLen = filasDe(LEN, 1).get(0).getTramoInicio().getId();
        assertThat(tramoMat).isNotEqualTo(tramoLen); // premisa del caso

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoLen));
        assertThat(filasDe(LEN, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoMat));
    }

    /**
     * (3) El intercambio cambia el tramo y SOLO el tramo. Sobre el desdoble de 6 plazas,
     * que es donde un fallo se vería.
     *
     * <p>La comparación es de pares (plaza → aula) ORDENADOS, no de multiconjunto de
     * aulas: una permutación de las aulas ENTRE plazas de la misma instancia conserva el
     * multiconjunto y sería invisible (lección de S143), pero cada plaza tiene la suya.
     */
    @Test
    void elIntercambioConservaElAulaDeCadaFilaDeAmbasInstancias() throws Exception {
        poblar();
        List<String> aulasDesdAntes = paresPlazaAula(DESD, 1);
        List<String> aulasMatAntes = paresPlazaAula(MAT, 1);
        assertThat(aulasDesdAntes).hasSize(6); // premisa del caso

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(DESD, 1, MAT, 1)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(paresPlazaAula(DESD, 1))
                .as("cada plaza del desdoble conserva SU aula")
                .containsExactlyElementsOf(aulasDesdAntes);
        assertThat(paresPlazaAula(MAT, 1))
                .as("y la del otro lado también")
                .containsExactlyElementsOf(aulasMatAntes);
        assertThat(filasDe(DESD, 1)).hasSize(6).allSatisfy(s ->
                assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.LUNES));
    }

    /**
     * (4) Un intercambio no crea ni destruye filas. Va aparte a propósito: si mover
     * fuese "borrar y reinsertar", un borrado parcial dejaría igualmente las filas
     * supervivientes en su tramo correcto y los casos 2 y 3 pasarían.
     */
    @Test
    void elRecuentoTotalDeFilasNoCambiaTrasElIntercambio() throws Exception {
        poblar();
        long filasAntes = sesionRepository.count();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(DESD, 1, MAT, 1)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(sesionRepository.count()).isEqualTo(filasAntes);
        assertThat(filasDe(DESD, 1)).hasSize(6);
        assertThat(filasDe(MAT, 1)).hasSize(1);
    }

    // ------------------------------------------------------------------ 5-9: cuerpo y 404

    /** (5) */
    @Test
    void horarioInexistenteDevuelve404() throws Exception {
        poblar();

        mockMvc.perform(put(url(9999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("HORARIO_INEXISTENTE"));
    }

    /**
     * (6) El 404 dice CUÁL de las dos falta. Con dos referencias en el cuerpo, un "no
     * existe" a secas obliga a quien llama a adivinar.
     */
    @Test
    void primeraInstanciaInexistenteDevuelve404YLaNombra() throws Exception {
        poblar();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 7, LEN, 1))) // MAT solo tiene la repetición 1
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_INEXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("primera")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(MAT)));
    }

    /** (7) El mismo 404 por el otro lado, nombrando 'segunda'. */
    @Test
    void segundaInstanciaInexistenteDevuelve404YLaNombra() throws Exception {
        poblar();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 7)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_INEXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("segunda")))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(LEN)));
    }

    /**
     * (8) Un índice fuera de rango sale por un rechazo TIPADO, no por el 500 que daría
     * el ctor canónico de {@code ActividadInstancia} —que lanza
     * {@code IllegalArgumentException} con {@code indice < 1}— si se le dejara construir.
     */
    @Test
    void indiceFueraDeRangoDaRechazoTipadoYNo500() throws Exception {
        poblar();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 0, LEN, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_INEXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("primera")));

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, -3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_INEXISTENTE"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("segunda")));
    }

    /** (9) Intercambiar una instancia consigo misma no es una operación: 400. */
    @Test
    void lasDosInstanciasIgualesDevuelve400() throws Exception {
        poblar();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, MAT, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.causa").value("INSTANCIAS_IGUALES"))
                .andExpect(jsonPath("$.violaciones.length()").value(0));
    }

    // ------------------------------------------------------------------ 10-13: pin y no-op

    /** (10) */
    @Test
    void primeraInstanciaPinadaDevuelve409YNoSeMueveNada() throws Exception {
        poblar();
        pinar(MAT, 1, tramoDe(Dia.LUNES, 1));
        Long tramoLenAntes = filasDe(LEN, 1).get(0).getTramoInicio().getId();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_PINADA"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("primera")));

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(LEN, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoLenAntes));
    }

    /** (11) El pin del OTRO lado rechaza igual: no basta con mirar la primera. */
    @Test
    void segundaInstanciaPinadaDevuelve409YNoSeMueveNada() throws Exception {
        poblar();
        pinar(LEN, 1, tramoDe(Dia.LUNES, 2));
        Long tramoMatAntes = filasDe(MAT, 1).get(0).getTramoInicio().getId();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_PINADA"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("segunda")));

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoMatAntes));
    }

    /**
     * (12) Dos instancias del MISMO tramo: permutarlas no es un intercambio. 200 y el
     * estado intacto —mismos ids, mismo tramo, mismo recuento—. Que no se emitiera un
     * UPDATE no es observable desde aquí y no se asevera.
     */
    @Test
    void ambasEnElMismoTramoDevuelve200YNoCambiaNada() throws Exception {
        poblar();
        Sesion matAntes = filasDe(MAT, 1).get(0);
        Sesion coAntes = filasDe(CO, 1).get(0);
        Long idMat = matAntes.getId();
        Long tramoComun = matAntes.getTramoInicio().getId();
        assertThat(coAntes.getTramoInicio().getId()).isEqualTo(tramoComun); // premisa
        long filasAntes = sesionRepository.count();

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, CO, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primera[0].tramo").value(1))
                .andExpect(jsonPath("$.segunda[0].tramo").value(1));

        entityManager.flush();
        entityManager.clear();
        assertThat(sesionRepository.count()).isEqualTo(filasAntes);
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(idMat);
            assertThat(s.getTramoInicio().getId()).isEqualTo(tramoComun);
        });
        assertThat(filasDe(CO, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoComun));
    }

    /**
     * (13) EL DESEMPATE. Las dos en el mismo tramo Y una pinada: manda el PIN, 409 y no
     * 200. Una instancia pinada está clavada, y "clavada" es un estado del recurso, no
     * del movimiento; es el mismo orden que fijó S143 para {@code mover}.
     */
    @Test
    void mismoTramoConUnaPinadaDevuelve409YNoElNoOp() throws Exception {
        poblar();
        pinar(CO, 1, tramoDe(Dia.LUNES, 1));

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, CO, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_PINADA"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("segunda")));
    }

    // ------------------------------------------------------------------ 14-15: regla dura

    /**
     * (14) El destino de cada una está LIBRE y el intercambio es ilegal igual: EF es
     * DISTRIBUIDA y llevar EF#2 al lunes la pone el mismo día que EF#1. Es el caso que
     * justifica que el veredicto lo emita el servidor.
     *
     * <p>El horario arrastra ADEMÁS un SOLAPE_PROFESOR preexistente ajeno (HIS y LEN
     * comparten profesor en LUNES-2), que el intercambio ni causa ni arregla. La lista
     * del 409 tiene que traer UNA violación —la nueva—, no dos: el veredicto es por
     * DIFERENCIA de multiconjuntos, no por filtro de "¿hay violaciones?".
     */
    @Test
    void intercambioQueRompeLaDistribucionPorDiaDevuelve409ConSoloLaViolacionNueva()
            throws Exception {
        poblar();
        anadirSolapeDeProfesorPreexistente();
        assertThat(diagnosticoService.diagnosticar(horarioId).violaciones())
                .as("el horario debe partir CON una violación dura preexistente")
                .anySatisfy(v -> assertThat(v.regla()).isEqualTo("SOLAPE_PROFESOR"));

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(EF, 2, MAT, 1))) // EF#2 (MARTES-3) <-> MAT#1 (LUNES-1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"))
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("DISTRIBUCION_MISMO_DIA"));
    }

    /**
     * (15) Un rechazo no escribe NADA. Se comprueban las tres cosas que un fallo movería
     * por separado: el tramo de cada lado, los pares plaza→aula, y el recuento total de
     * la tabla —sin el último, un borrado parcial pasaría por bueno—.
     */
    @Test
    void unRechazoNoEscribeNiTramoNiAulaNiFilas() throws Exception {
        poblar();
        long filasAntes = sesionRepository.count();
        Long tramoEf = filasDe(EF, 2).get(0).getTramoInicio().getId();
        Long tramoMat = filasDe(MAT, 1).get(0).getTramoInicio().getId();
        List<String> aulasEf = paresPlazaAula(EF, 2);
        List<String> aulasMat = paresPlazaAula(MAT, 1);

        mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(EF, 2, MAT, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"));

        entityManager.flush();
        entityManager.clear();
        assertThat(sesionRepository.count())
                .as("ni una fila de más ni de menos en toda la tabla")
                .isEqualTo(filasAntes);
        assertThat(filasDe(EF, 2)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoEf));
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoMat));
        assertThat(paresPlazaAula(EF, 2)).containsExactlyElementsOf(aulasEf);
        assertThat(paresPlazaAula(MAT, 1)).containsExactlyElementsOf(aulasMat);
    }

    // ------------------------------------------------------------------ 16: contrato de forma

    /**
     * (16) Hermano de {@code elCuerpoDel200CoincideConLaProyeccionDeEsaInstancia}: cada
     * lado del cuerpo tiene la MISMA forma que las {@code sesiones} de
     * {@code GET /{id}/proyeccion} para esa instancia. El mapeo de
     * {@code releerInstancia} es un espejo deliberado del de
     * {@code GeneradorHorarioService.proyectar} (D-proyeccion-instancia-espejo, que con
     * este endpoint pasa a usarse DOS veces por respuesta); esto es lo que impide que
     * diverjan en silencio.
     */
    @Test
    void elCuerpoDel200CoincideConLaProyeccionDeLasDosInstancias() throws Exception {
        poblar();

        MvcResult res = mockMvc.perform(put(url(horarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, LEN, 1)))
                .andExpect(status().isOk())
                .andReturn();
        String intercambiado = res.getResponse().getContentAsString();

        entityManager.flush();
        String proyeccion = mockMvc.perform(get("/api/horarios/" + horarioId + "/proyeccion"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((Object) JsonPath.read(intercambiado, "$.primera[0]"))
                .isEqualTo(deLaProyeccion(proyeccion, MAT, 1));
        assertThat((Object) JsonPath.read(intercambiado, "$.segunda[0]"))
                .isEqualTo(deLaProyeccion(proyeccion, LEN, 1));
    }

    /**
     * Un filtro de JsonPath devuelve SIEMPRE una lista, aunque case un solo elemento: el
     * índice se toma en Java, no dentro de la expresión.
     */
    private static Object deLaProyeccion(String proyeccion, String actividadCodigo, int indice) {
        List<Object> casadas = JsonPath.read(proyeccion,
                "$.sesiones[?(@.actividadCodigo=='" + actividadCodigo + "' && @.indice=="
                        + indice + ")]");
        assertThat(casadas).hasSize(1);
        return casadas.get(0);
    }

    // ------------------------------------------------------------------ fixture

    private static String url(Long horarioId) {
        return "/api/horarios/" + horarioId + "/instancias/intercambio";
    }

    private static String cuerpo(String actA, int indiceA, String actB, int indiceB) {
        return "{\"primera\":{\"actividadCodigo\":\"" + actA + "\",\"indice\":" + indiceA + "},"
                + "\"segunda\":{\"actividadCodigo\":\"" + actB + "\",\"indice\":" + indiceB + "}}";
    }

    private List<Sesion> filasDe(String actividadCodigo, int indice) {
        return sesionRepository.findParaInstancia(horarioId, actividadCodigo, indice);
    }

    /** Pares (plaza → aula) ORDENADOS: es lo que fija cada aula a SU fila. */
    private List<String> paresPlazaAula(String actividadCodigo, int indice) {
        return filasDe(actividadCodigo, indice).stream()
                .map(s -> s.getPlaza().getCodigo() + "->" + s.getAula().getCodigo())
                .sorted().toList();
    }

    private void pinar(String actividadCodigo, int indice, TramoSemanal tramo) {
        pinTramoRepository.save(new SesionBloqueada(
                actividadRepository.findByCodigo(actividadCodigo).orElseThrow(), indice, tramo));
        entityManager.flush();
    }

    private TramoSemanal tramoDe(Dia dia, int ordenEnDia) {
        int ordenGlobal = (dia == Dia.LUNES ? 0 : 3) + ordenEnDia;
        return tramoRepository.findAll().stream()
                .filter(t -> t.getDia() == dia && t.getOrden() == ordenGlobal)
                .findFirst().orElseThrow();
    }

    /**
     * Catálogo mínimo + un horario con TODAS las instancias colocadas y SIN ninguna
     * violación dura: cualquier violación que aparezca en un test la causa el
     * intercambio, no el fixture.
     */
    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g1a = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo g1b = grupoRepository.save(
                new GrupoAdministrativo("1ºB", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo g1c = grupoRepository.save(
                new GrupoAdministrativo("1ºC", eso1, TipoGrupo.ORDINARIO, null));

        // sgA1 y sgA2 son subgrupos DISTINTOS del MISMO grupo: MAT y LEN comparten grupo
        // y no subgrupo, que es lo que hace su intercambio interesante.
        Subgrupo sgA1 = subgrupoRepository.save(new Subgrupo("1ºA-s1", Set.of(g1a)));
        Subgrupo sgA2 = subgrupoRepository.save(new Subgrupo("1ºA-s2", Set.of(g1a)));
        Subgrupo sgB = subgrupoRepository.save(new Subgrupo("1ºB-s1", Set.of(g1b)));
        Subgrupo sgC = subgrupoRepository.save(new Subgrupo("1ºC-s1", Set.of(g1c)));

        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Asignatura len = asignaturaRepository.save(new Asignatura("LEN", "Lengua"));
        Asignatura ef = asignaturaRepository.save(new Asignatura("EF", "Educacion Fisica"));
        Asignatura des = asignaturaRepository.save(new Asignatura("DES", "Desdoble"));
        Asignatura tec = asignaturaRepository.save(new Asignatura("TEC", "Tecnologia"));

        Profesor pMat = profesorRepository.save(new Profesor("P-MAT", "Profesor MAT"));
        Profesor pLen = profesorRepository.save(new Profesor("P-LEN", "Profesor LEN"));
        Profesor pEf = profesorRepository.save(new Profesor("P-EF", "Profesor EF"));
        Profesor pCo = profesorRepository.save(new Profesor("P-CO", "Profesor CO"));

        Aula aMat = aulaRepository.save(new Aula("A-MAT", TipoAula.ORDINARIA, null, null, null, null));
        Aula aLen = aulaRepository.save(new Aula("A-LEN", TipoAula.ORDINARIA, null, null, null, null));
        Aula aEf = aulaRepository.save(new Aula("A-EF", TipoAula.ORDINARIA, null, null, null, null));
        Aula aCo = aulaRepository.save(new Aula("A-CO", TipoAula.ORDINARIA, null, null, null, null));

        // Malla: LUNES orden global 1..3, MARTES 4..6 -> ordenEnDia 1..3 en cada día.
        for (int i = 1; i <= 3; i++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(8 + i, 0), LocalTime.of(9 + i, 0), true, i, null));
        }
        for (int i = 1; i <= 3; i++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.MARTES, LocalTime.of(8 + i, 0), LocalTime.of(9 + i, 0), true, 3 + i, null));
        }

        Actividad actMat = actividad(MAT, 1, PatronTemporal.NEUTRA);
        actMat.getPlazas().add(plaza(MAT + "-P1", actMat, mat, Set.of(pMat), aMat, Set.of(sgA1)));
        actividadRepository.save(actMat);

        Actividad actLen = actividad(LEN, 1, PatronTemporal.NEUTRA);
        actLen.getPlazas().add(plaza(LEN + "-P1", actLen, len, Set.of(pLen), aLen, Set.of(sgA2)));
        actividadRepository.save(actLen);

        Actividad actEf = actividad(EF, 2, PatronTemporal.DISTRIBUIDA);
        actEf.getPlazas().add(plaza(EF + "-P1", actEf, ef, Set.of(pEf), aEf, Set.of(sgB)));
        actividadRepository.save(actEf);

        // Recursos enteramente propios: convive con MAT en LUNES-1 sin pisarla, y así
        // existe un par de instancias en el MISMO tramo (casos 12 y 13).
        Actividad actCo = actividad(CO, 1, PatronTemporal.NEUTRA);
        actCo.getPlazas().add(plaza(CO + "-P1", actCo, tec, Set.of(pCo), aCo, Set.of(sgC)));
        actividadRepository.save(actCo);

        // Desdoble de 6 plazas: profesor, aula y subgrupo propios en cada una, para que
        // moverlo entero no choque con nada por sí mismo.
        Actividad actDesd = actividad(DESD, 1, PatronTemporal.NEUTRA);
        for (int i = 1; i <= 6; i++) {
            Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºB-d" + i, Set.of(g1b)));
            Profesor p = profesorRepository.save(new Profesor("P-D" + i, "Profesor D" + i));
            Aula a = aulaRepository.save(
                    new Aula("A-D" + i, TipoAula.ORDINARIA, null, null, null, null));
            actDesd.getPlazas().add(
                    plaza(DESD + "-P" + i, actDesd, des, Set.of(p), a, Set.of(sg)));
        }
        actividadRepository.save(actDesd);
        entityManager.flush();

        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "Horario de S144", Instant.parse("2026-09-09T08:00:00Z"), "OPTIMAL", 0.0, 0.0));

        colocar(horario, actMat, 1, tramoDe(Dia.LUNES, 1));
        colocar(horario, actCo, 1, tramoDe(Dia.LUNES, 1)); // MISMO tramo que MAT, sin pisarla
        colocar(horario, actLen, 1, tramoDe(Dia.LUNES, 2));
        colocar(horario, actEf, 1, tramoDe(Dia.LUNES, 3));
        colocar(horario, actEf, 2, tramoDe(Dia.MARTES, 3));
        colocar(horario, actDesd, 1, tramoDe(Dia.MARTES, 1));
        horarioId = horario.getId();
        entityManager.flush();
        // clear() para que todo se lea de la TABLA, como haría una petición real
        // (D-post-horario-sin-sesiones).
        entityManager.clear();
    }

    /**
     * Añade HIS —que comparte profesor con LEN— en el MISMO tramo que LEN, creando un
     * SOLAPE_PROFESOR que ya está ahí antes de cualquier intercambio y que el
     * intercambio de EF#2 con MAT#1 ni causa ni arregla.
     */
    private void anadirSolapeDeProfesorPreexistente() {
        Asignatura his = asignaturaRepository.save(new Asignatura("HIS", "Historia"));
        Aula aHis = aulaRepository.save(new Aula("A-HIS", TipoAula.ORDINARIA, null, null, null, null));
        GrupoAdministrativo g1d = grupoRepository.save(new GrupoAdministrativo(
                "1ºD", nivelRepository.findByCodigo("1ESO").orElseThrow(),
                TipoGrupo.ORDINARIO, null));
        Subgrupo sgD = subgrupoRepository.save(new Subgrupo("1ºD-s1", Set.of(g1d)));
        Profesor pLen = profesorRepository.findByCodigo("P-LEN").orElseThrow();

        Actividad actHis = actividad(HIS, 1, PatronTemporal.NEUTRA);
        actHis.getPlazas().add(plaza(HIS + "-P1", actHis, his, Set.of(pLen), aHis, Set.of(sgD)));
        actividadRepository.save(actHis);
        entityManager.flush();

        HorarioGenerado horario = horarioRepository.findById(horarioId).orElseThrow();
        colocar(horario, actHis, 1, tramoDe(Dia.LUNES, 2)); // el tramo de LEN: solape de P-LEN
        entityManager.flush();
        entityManager.clear();
    }

    /** Una fila de {@code sesion} por plaza de la actividad, todas en el mismo tramo. */
    private void colocar(HorarioGenerado horario, Actividad actividad, int indice, TramoSemanal tramo) {
        for (Plaza plaza : actividad.getPlazas()) {
            sesionRepository.save(new Sesion(horario, plaza, indice, tramo, plaza.getAulaFija()));
        }
    }

    private static Actividad actividad(String codigo, int repeticiones, PatronTemporal patron) {
        Actividad a = new Actividad();
        a.setCodigo(codigo);
        a.setRepeticionesPorSemana(repeticiones);
        a.setDuracionTramos(1);
        a.setPatronTemporal(patron);
        return a;
    }

    /** Plaza de aula FIJA: el intercambio conserva el aula, no hay elección que hacer. */
    private static Plaza plaza(String codigo, Actividad actividad, Asignatura asignatura,
            Set<Profesor> profesores, Aula aulaFija, Set<Subgrupo> subgrupos) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(profesores);
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(subgrupos);
        return plaza;
    }
}
