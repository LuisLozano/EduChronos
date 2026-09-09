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
 * Tests del {@code PUT /api/horarios/{id}/instancias} (S143, C-mover-sesion-backend):
 * recolocar una instancia con el veredicto emitido por el SERVIDOR.
 *
 * <p>Vive en {@code app.catalog} por el ctor {@code protected} de {@code Actividad}/
 * {@code Plaza}, y corre sobre SQLite real ({@code replace = NONE}) dentro de la
 * transacción única de {@code @DataJpaTest}: esa única sesión de Hibernate es la que
 * preserva la IDENTIDAD DE OBJETO de {@code TramoSemanal} que la reconstrucción de la
 * solución necesita (S62).
 *
 * <p>Las sesiones se persisten A MANO, sin pasar por el solver. Es deliberado: permite
 * colocar el horario en estados que el solver nunca produciría —en particular uno con
 * una violación PREEXISTENTE— y hace los casos deterministas, sin depender del azar
 * del presolve (D-generacion-no-reproducible).
 *
 * <p>Malla: LUNES y MARTES, 3 tramos lectivos cada uno (ordenEnDia 1..3).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, DiagnosticoService.class, MovimientoInstanciaService.class})
class MovimientoInstanciaEndpointTest {

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
    /** DISTRIBUIDA con 2 repeticiones: instancia 1 en lunes, instancia 2 en martes. */
    private static final String EF = "EF-1A";
    /** Desdoble de 6 plazas: la instancia grande que se mueve entera. */
    private static final String DESD = "DESD-6";
    /** Comparte profesor con MAT; solo la usa el caso de violación preexistente. */
    private static final String HIS = "HIS-1A";

    private Long horarioId;

    @BeforeEach
    void montar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MovimientoInstanciaController(movimientoService),
                        new HorarioController(generadorService, diagnosticoService))
                .build();
    }

    // ------------------------------------------------------------------ casos verdes

    @Test
    void movimientoValido200YLaFilaQuedaEnElTramoNuevo() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 2, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dia").value(2))
                .andExpect(jsonPath("$[0].tramo").value(2))
                .andExpect(jsonPath("$[0].actividadCodigo").value(MAT));

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s -> {
            assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.MARTES);
            assertThat(s.getTramoInicio().getOrden()).isEqualTo(5); // MARTES-2 en orden global
        });
    }

    @Test
    void instanciaDeSeisPlazasSeMueveEnteraSinDejarNingunaFilaAtras() throws Exception {
        poblar();
        assertThat(filasDe(DESD, 1)).hasSize(6); // premisa del caso, no un adorno

        MvcResult res = mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(DESD, 1, 2, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andReturn();

        // Las 6 filas del cuerpo, en el destino; ninguna se queda.
        List<Integer> dias = JsonPath.read(res.getResponse().getContentAsString(), "$[*].dia");
        List<Integer> tramos = JsonPath.read(res.getResponse().getContentAsString(), "$[*].tramo");
        assertThat(dias).containsOnly(2);
        assertThat(tramos).containsOnly(2);

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(DESD, 1)).hasSize(6).allSatisfy(s -> {
            assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.MARTES);
            assertThat(s.getTramoInicio().getOrden()).isEqualTo(5);
        });
    }

    /**
     * Idempotencia (regla 6): mover al tramo que ya ocupa devuelve 200 y deja el estado
     * intacto. Lo que se asevera es que NADA cambia —misma fila, mismo id, mismo tramo,
     * mismo recuento—; no que no se emitiera un UPDATE, que desde aquí no es observable.
     */
    @Test
    void moverAlMismoTramoDevuelve200YNoCambiaNada() throws Exception {
        poblar();
        Sesion antes = filasDe(MAT, 1).get(0);
        Long idAntes = antes.getId();
        Long tramoAntes = antes.getTramoInicio().getId();
        long filasAntes = sesionRepository.count();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 1, 1))) // LUNES-1: donde ya está
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dia").value(1))
                .andExpect(jsonPath("$[0].tramo").value(1));

        entityManager.flush();
        entityManager.clear();
        assertThat(sesionRepository.count()).isEqualTo(filasAntes);
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(idAntes);
            assertThat(s.getTramoInicio().getId()).isEqualTo(tramoAntes);
        });
    }

    /**
     * EL CASO QUE ASEVERA EL DIFF. El horario arrastra un SOLAPE_PROFESOR preexistente
     * (MAT e HIS comparten profesor y están en el mismo tramo desde el principio). Mover
     * una instancia AJENA a esa violación debe pasar: el usuario responde de lo que su
     * movimiento causa, no de lo que ya estaba roto. Un veredicto por FILTRO —"¿hay
     * violaciones?"— devolvería 409 aquí y dejaría el horario congelado para siempre.
     */
    @Test
    void unaViolacionPreexistenteAjenaAlMovimientoNoLoRechaza() throws Exception {
        poblar();
        anadirSolapeDeProfesorPreexistente();

        // La violación existe ANTES de mover: sin esto el test no mediría nada.
        assertThat(diagnosticoService.diagnosticar(horarioId).violaciones())
                .as("el horario debe partir CON una violación dura preexistente")
                .anySatisfy(v -> assertThat(v.regla()).isEqualTo("SOLAPE_PROFESOR"));

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(DESD, 1, 2, 2))) // DESD no toca ni a MAT ni a HIS
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(DESD, 1)).allSatisfy(s ->
                assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.MARTES));
    }

    // ------------------------------------------------------------------ casos rojos

    @Test
    void destinoQueProvocaSolapeDeGrupoDevuelve409NombrandoElRecurso() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 1, 2))) // LUNES-2: donde está LEN, mismo grupo
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"))
                .andExpect(jsonPath("$.violaciones[?(@.regla=='SOLAPE_GRUPO')].recursoCodigo")
                        .value("1ºA"));

        entityManager.flush();
        entityManager.clear();
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getOrden()).isEqualTo(1)); // sigue en LUNES-1
    }

    /**
     * El caso que justifica que el veredicto lo emita el SERVIDOR: la rejilla es ciega a
     * la distribución por día. El destino está libre —ningún solape de nada—, y aun así
     * el movimiento es ilegal porque EF es DISTRIBUIDA y ya tiene una sesión ese lunes.
     */
    @Test
    void destinoLibreQueRompeLaDistribucionPorDiaDevuelve409() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        // LUNES-1: allí solo está MAT, que no comparte profesor, aula ni
                        // subgrupo con EF. El destino está libre PARA EF; lo único que
                        // rompe es que EF#1 ya ocupa ese lunes.
                        .content(cuerpo(EF, 2, 1, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"))
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("DISTRIBUCION_MISMO_DIA"));
    }

    @Test
    void instanciaPinadaDevuelve409YElPinQuedaIntacto() throws Exception {
        poblar();
        Actividad mat = actividadRepository.findByCodigo(MAT).orElseThrow();
        TramoSemanal lunes1 = tramoDe(Dia.LUNES, 1);
        pinTramoRepository.save(new SesionBloqueada(mat, 1, lunes1));
        entityManager.flush();
        long pinesAntes = pinTramoRepository.count();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 2, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_PINADA"))
                .andExpect(jsonPath("$.violaciones.length()").value(0));

        entityManager.flush();
        entityManager.clear();
        assertThat(pinTramoRepository.count()).isEqualTo(pinesAntes);
        assertThat(pinTramoRepository.findByActividadAndIndice(
                        actividadRepository.findByCodigo(MAT).orElseThrow(), 1))
                .as("el pin sigue ahí y sigue apuntando a LUNES-1")
                .hasValueSatisfying(p -> assertThat(p.getTramoInicio().getOrden()).isEqualTo(1));
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getOrden()).isEqualTo(1));
    }

    @Test
    void tramoFueraDeLaMallaDevuelve400() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 5, 6))) // viernes no existe en esta malla
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.causa").value("TRAMO_INEXISTENTE"));
    }

    @Test
    void horarioInexistenteDevuelve404() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/9999/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 2, 2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("HORARIO_INEXISTENTE"));
    }

    @Test
    void instanciaQueNoEstaEnEseHorarioDevuelve404() throws Exception {
        poblar();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 7, 2, 2))) // MAT solo tiene la repetición 1
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_INEXISTENTE"));
    }

    // ------------------------------------------------- el rechazo no deja rastro en la base

    /**
     * Un 409 por regla dura no puede haber escrito NADA. El veredicto se emite sobre una
     * solución candidata construida en memoria, así que las filas deben seguir en su
     * tramo original —leídas de la TABLA, con el contexto de Hibernate vaciado antes, no
     * de las entidades que el servicio tuvo en la mano—.
     *
     * <p>El recuento total de {@code sesion} va aparte a propósito: sin él, un borrado
     * parcial (mover = borrar y reinsertar, y fallar entre medias) pasaría por bueno,
     * porque "la fila que queda está en su tramo original" seguiría siendo cierto.
     */
    @Test
    void rechazoPorReglaDuraNoEscribeNadaEnLaBase() throws Exception {
        poblar();
        long filasAntes = sesionRepository.count();
        Long tramoOriginal = filasDe(MAT, 1).get(0).getTramoInicio().getId();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 1, 2))) // LUNES-2: donde está LEN, mismo grupo
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"));

        // flush() fuerza a la BD lo que hubiera pendiente; clear() garantiza que lo que
        // se lee después viene de la tabla y no del contexto de persistencia.
        entityManager.flush();
        entityManager.clear();

        assertThat(sesionRepository.count())
                .as("ni una fila de más ni de menos en toda la tabla")
                .isEqualTo(filasAntes);
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoOriginal));
    }

    /**
     * Lo mismo para el 409 por pin: la instancia sigue en su tramo original y la tabla
     * conserva sus filas. El pin se comprueba antes del veredicto, así que este caso
     * cubre una rama de salida distinta de la anterior.
     */
    @Test
    void rechazoPorPinNoEscribeNadaEnLaBase() throws Exception {
        poblar();
        long filasAntes = sesionRepository.count();
        Long tramoOriginal = filasDe(MAT, 1).get(0).getTramoInicio().getId();
        pinTramoRepository.save(new SesionBloqueada(
                actividadRepository.findByCodigo(MAT).orElseThrow(), 1, tramoDe(Dia.LUNES, 1)));
        entityManager.flush();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 2, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("INSTANCIA_PINADA"));

        entityManager.flush();
        entityManager.clear();

        assertThat(sesionRepository.count()).isEqualTo(filasAntes);
        assertThat(filasDe(MAT, 1)).singleElement().satisfies(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoOriginal));
    }

    // ------------------------------------------------------------------ el aula no cambia

    /**
     * El movimiento cambia el tramo y SOLO el tramo. Sobre el desdoble de 6 plazas, que
     * es donde un fallo se vería: tras el 200, las 6 filas están en el tramo nuevo y
     * llevan EXACTAMENTE las mismas 6 aulas que antes.
     *
     * <p>La comparación es de MULTICONJUNTO, no de lista ordenada ni de conjunto: una
     * permutación de las aulas entre plazas es un fallo real —cada plaza tiene la suya—,
     * y un {@code Set} taparía además que dos filas acabaran compartiendo aula. Por eso
     * se comparan los pares (plaza, aula), que es lo que fija cada aula a su fila.
     */
    @Test
    void elMovimientoConservaElAulaDeCadaFila() throws Exception {
        poblar();
        List<String> aulasAntes = filasDe(DESD, 1).stream()
                .map(s -> s.getPlaza().getCodigo() + "->" + s.getAula().getCodigo())
                .sorted().toList();
        assertThat(aulasAntes).hasSize(6); // premisa del caso

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(DESD, 1, 2, 2)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        List<Sesion> despues = filasDe(DESD, 1);
        assertThat(despues).hasSize(6).allSatisfy(s -> {
            assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.MARTES);
            assertThat(s.getTramoInicio().getOrden()).isEqualTo(5); // MARTES-2, orden global
        });
        assertThat(despues.stream()
                        .map(s -> s.getPlaza().getCodigo() + "->" + s.getAula().getCodigo())
                        .sorted().toList())
                .as("cada plaza conserva SU aula")
                .containsExactlyElementsOf(aulasAntes);
    }

    // ------------------------------------------------------------------ contrato de forma

    /**
     * El cuerpo del 200 tiene la MISMA forma que las {@code sesiones} de
     * {@code GET /{id}/proyeccion} para esa instancia. El mapeo de
     * {@code MovimientoInstanciaService.releerInstancia} es un espejo deliberado del de
     * {@code GeneradorHorarioService.proyectar} (no se le añade lógica a ese servicio,
     * D-F8.2b-iii-A-a); este test es lo que impide que los dos diverjan en silencio.
     */
    @Test
    void elCuerpoDel200CoincideConLaProyeccionDeEsaInstancia() throws Exception {
        poblar();

        String movido = mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(MAT, 1, 2, 2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        String proyeccion = mockMvc.perform(get("/api/horarios/" + horarioId + "/proyeccion"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Object delMovimiento = JsonPath.read(movido, "$[0]");
        // Un filtro de JsonPath devuelve SIEMPRE una lista, aunque case un solo
        // elemento: el índice se toma en Java, no dentro de la expresión.
        List<Object> deLaProyeccion = JsonPath.read(proyeccion,
                "$.sesiones[?(@.actividadCodigo=='" + MAT + "' && @.indice==1)]");
        assertThat(deLaProyeccion).hasSize(1);
        assertThat(delMovimiento).isEqualTo(deLaProyeccion.get(0));
    }

    // ------------------------------------------------------------------ fixture

    private static String cuerpo(String actividadCodigo, int indice, int dia, int orden) {
        return "{\"actividadCodigo\":\"" + actividadCodigo + "\",\"indice\":" + indice
                + ",\"dia\":" + dia + ",\"orden\":" + orden + "}";
    }

    private List<Sesion> filasDe(String actividadCodigo, int indice) {
        return sesionRepository.findParaInstancia(horarioId, actividadCodigo, indice);
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
     * movimiento, no el fixture.
     */
    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g1a = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo g1b = grupoRepository.save(
                new GrupoAdministrativo("1ºB", eso1, TipoGrupo.ORDINARIO, null));

        // sgA1 y sgA2 son subgrupos DISTINTOS del MISMO grupo: juntarlos en un tramo da
        // SOLAPE_GRUPO sin SOLAPE_SUBGRUPO, que es justo lo que discrimina ese caso.
        Subgrupo sgA1 = subgrupoRepository.save(new Subgrupo("1ºA-s1", Set.of(g1a)));
        Subgrupo sgA2 = subgrupoRepository.save(new Subgrupo("1ºA-s2", Set.of(g1a)));
        Subgrupo sgB = subgrupoRepository.save(new Subgrupo("1ºB-s1", Set.of(g1b)));

        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Asignatura len = asignaturaRepository.save(new Asignatura("LEN", "Lengua"));
        Asignatura ef = asignaturaRepository.save(new Asignatura("EF", "Educacion Fisica"));
        Asignatura des = asignaturaRepository.save(new Asignatura("DES", "Desdoble"));

        Profesor pMat = profesorRepository.save(new Profesor("P-MAT", "Profesor MAT"));
        Profesor pLen = profesorRepository.save(new Profesor("P-LEN", "Profesor LEN"));
        Profesor pEf = profesorRepository.save(new Profesor("P-EF", "Profesor EF"));

        Aula aMat = aulaRepository.save(new Aula("A-MAT", TipoAula.ORDINARIA, null, null, null, null));
        Aula aLen = aulaRepository.save(new Aula("A-LEN", TipoAula.ORDINARIA, null, null, null, null));
        Aula aEf = aulaRepository.save(new Aula("A-EF", TipoAula.ORDINARIA, null, null, null, null));

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
                "Horario de S143", Instant.parse("2026-09-09T08:00:00Z"), "OPTIMAL", 0.0, 0.0));

        colocar(horario, actMat, 1, tramoDe(Dia.LUNES, 1));
        colocar(horario, actLen, 1, tramoDe(Dia.LUNES, 2));
        colocar(horario, actEf, 1, tramoDe(Dia.LUNES, 3));
        colocar(horario, actEf, 2, tramoDe(Dia.MARTES, 3));
        colocar(horario, actDesd, 1, tramoDe(Dia.MARTES, 1));
        horarioId = horario.getId();
        entityManager.flush();
        // clear() para que todo se lea de la TABLA, como haría una petición real: las
        // filas se han insertado sin tocar la colección inversa del horario, y sin esto
        // el fixture dejaría en memoria un horario con cero sesiones
        // (D-post-horario-sin-sesiones).
        entityManager.clear();
    }

    /**
     * Añade HIS —que comparte profesor con MAT— en el MISMO tramo que MAT, creando un
     * SOLAPE_PROFESOR que ya está ahí antes de cualquier movimiento.
     */
    private void anadirSolapeDeProfesorPreexistente() {
        Asignatura his = asignaturaRepository.save(new Asignatura("HIS", "Historia"));
        Aula aHis = aulaRepository.save(new Aula("A-HIS", TipoAula.ORDINARIA, null, null, null, null));
        GrupoAdministrativo g1c = grupoRepository.save(new GrupoAdministrativo(
                "1ºC", nivelRepository.findByCodigo("1ESO").orElseThrow(),
                TipoGrupo.ORDINARIO, null));
        Subgrupo sgC = subgrupoRepository.save(new Subgrupo("1ºC-s1", Set.of(g1c)));
        Profesor pMat = profesorRepository.findByCodigo("P-MAT").orElseThrow();

        Actividad actHis = actividad(HIS, 1, PatronTemporal.NEUTRA);
        actHis.getPlazas().add(plaza(HIS + "-P1", actHis, his, Set.of(pMat), aHis, Set.of(sgC)));
        actividadRepository.save(actHis);
        entityManager.flush();

        HorarioGenerado horario = horarioRepository.findById(horarioId).orElseThrow();
        colocar(horario, actHis, 1, tramoDe(Dia.LUNES, 1)); // el tramo de MAT: solape de P-MAT
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

    /** Plaza de aula FIJA: el movimiento conserva el aula, así que no hay elección que hacer. */
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
