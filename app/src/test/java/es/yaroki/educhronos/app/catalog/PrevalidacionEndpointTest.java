package es.yaroki.educhronos.app.catalog;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.PrevalidacionService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.PrevalidacionController;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests HTTP de la pre-validación (Fase 8, Bloque 8.4-A): los DOS llamadores del mismo
 * núcleo, sobre el MISMO catálogo persistido.
 * <ul>
 *   <li>{@code GET /api/prevalidacion} → siempre {@code 200} con la lista completa.</li>
 *   <li>{@code POST /api/horarios} → {@code 422} si hay al menos un ERROR, sin llegar a
 *       construir el solver.</li>
 * </ul>
 *
 * <p>Vive en {@code app.catalog} —como {@code GenerarHorarioEndpointTest}— para construir
 * entidades {@code Actividad}/{@code Plaza} (ctor {@code protected}), y monta los
 * controladores reales con {@code standaloneSetup} sobre el servicio real.
 *
 * <p><b>Por qué se intercepta la construcción de {@link SolverHorario}.</b> Los catálogos
 * que violan una condición necesaria son TAMBIÉN infactibles para el solver —es
 * justamente lo que la regla garantiza—, así que un {@code 422} por sí solo no distingue
 * cuál de las dos vías actuó. Se comprueban por eso las dos cosas que sí discriminan: la
 * {@code causa} del CUERPO ({@code PREVALIDACION_FALLIDA}, que el solver no emite nunca), y
 * que {@code mocked.constructed()} quede vacío. Lo segundo es la prueba directa de que no
 * se gastó presupuesto de solve, que es el propósito entero del bloque.
 *
 * <p><b>Se aserta el cuerpo, nunca {@code status().reason()}</b> (S166): el {@code reason}
 * lo puebla {@code standaloneSetup} aunque por la red no viaje nada (D-F8.6-ii-a, medido en
 * la fase 1 de S166). Hasta S166 estos tests miraban la causa de la excepción resuelta;
 * desde que el controlador construye el cuerpo del 422 ya no hay excepción que resolver.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, PrevalidacionService.class})
class PrevalidacionEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService generadorService;
    @Autowired private PrevalidacionService prevalidacionService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;
    @Autowired private SesionBloqueadaRepository pinTramoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new HorarioController(generadorService,
                                new DiagnosticoService(generadorService, tramoRepository),
                                mock(ExportacionHorarioService.class)),
                        new PrevalidacionController(prevalidacionService))
                .build();
    }

    /**
     * (A6) MISMA ENTRADA, DOS SALIDAS. Un catálogo con un ERROR de (d): la generación lo
     * rechaza con {@code 422} sin llegar al solver, y la consulta de pre-validación
     * devuelve {@code 200} con ese mismo hallazgo detallado. Prueba que los dos llamadores
     * comparten núcleo y difieren solo en qué hacen con el resultado.
     */
    @Test
    void mismoCatalogoConError_da422AlGenerarY200AlPrevalidar() throws Exception {
        poblarCatalogoConErrorDeRepeticiones();
        entityManager.flush();

        // La CAUSA importa: 422 de pre-validación, no del solver (que ni se construye).
        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxSegundos\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.causa").value("PREVALIDACION_FALLIDA"));

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.regla=='REPETICIONES_EXCEDEN_DIAS')].severidad")
                        .value(Matchers.hasItem("ERROR")))
                .andExpect(jsonPath("$[?(@.regla=='REPETICIONES_EXCEDEN_DIAS')].entidadCodigo")
                        .value(Matchers.hasItem("Mat-1ºA")));
    }

    /**
     * (A4, INVERTIDO en S79) La sobrecarga de GRUPO es ERROR y SÍ aborta la generación.
     * Antes este test aseveraba lo contrario —que era AVISO y dejaba pasar—; el aserto se
     * volvió falso por contrato al corregir (c), y se invierte en vez de borrarse porque
     * el fixture sigue siendo el discriminante correcto de la regla.
     *
     * <p>Tres asertos, y el tercero es el que de verdad importa:
     * <ol>
     *   <li>el GET muestra el hallazgo con severidad {@code ERROR} (no {@code AVISO});</li>
     *   <li>el POST da {@code 422} con causa {@code PREVALIDACION_FALLIDA} en el cuerpo —NO
     *       {@code CATALOGO_INFACTIBLE}, la de
     *       {@link es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException}—, que es
     *       la distinción que este catálogo hace delicada: es infactible por las DOS vías,
     *       así que sin mirar la causa un 422 no probaría cuál de las dos actuó;</li>
     *   <li>{@code mocked.constructed()} queda VACÍO: el solver ni siquiera se construye.
     *       Ese es el objetivo entero de la pre-validación —no gastar el presupuesto— y
     *       ningún aserto sobre el status puede demostrarlo.</li>
     * </ol>
     */
    @Test
    void grupoSobrecargado_abortaCon422SinLlegarAConstruirElSolver() throws Exception {
        poblarCatalogoConGrupoSobrecargado();
        entityManager.flush();

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.regla=='GRUPO_SOBRECARGADO')].severidad")
                        .value(Matchers.hasItem("ERROR")))
                .andExpect(jsonPath("$[?(@.regla=='GRUPO_SOBRECARGADO')].entidadCodigo")
                        .value(Matchers.hasItem("1ºA")));

        try (MockedConstruction<SolverHorario> mocked =
                     Mockito.mockConstruction(SolverHorario.class)) {

            mockMvc.perform(post("/api/horarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.causa").value("PREVALIDACION_FALLIDA"));

            assertThat(mocked.constructed()).isEmpty();
        }
    }

    /**
     * (E1) S8 por la RED: una actividad {@code requiereTutor} sin ninguna fila de tutoría
     * sale en el JSON con severidad {@code "AVISO"} y regla {@code "TUTORIA_SIN_TUTOR"}.
     * Las dos cadenas se asertan LITERALES porque son contrato HTTP: el enum del servicio
     * no se serializa tal cual (ver {@code AvisoPrevalidacionDTO}), así que un cambio de
     * nombre en el enum no debe pasar inadvertido por el lado del cliente.
     *
     * <p>Fixture calibrado como los demás: 5 tramos en 5 días y una NEUTRA de 3
     * repeticiones, con lo que (a) ve 3≤5, (c) ve 3≤5 y (d) ni mira. El
     * {@code jsonPath("$.length()").value(1)} lo fija: el hallazgo aseverado es el único.
     */
    @Test
    void actividadSinTutorPrincipal_saleEnElJsonComoAvisoDeTutoria() throws Exception {
        poblarCatalogoConTutoriaSinTutor();
        entityManager.flush();

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severidad").value("AVISO"))
                .andExpect(jsonPath("$[0].regla").value("TUTORIA_SIN_TUTOR"))
                .andExpect(jsonPath("$[0].entidadCodigo").value("Tut-1ºA"));
    }

    /**
     * (F-HTTP) Pin sobre un tramo DURA (S165, condición 6 de {@code O-disponibilidad}): el
     * GET lo enseña como el ÚNICO hallazgo, ERROR y a nombre de MAT8; el POST da
     * {@code 422} por la pre-validación sin construir el solver, y el motivo nombra al
     * profesor y al tramo. Mismo patrón que {@code grupoSobrecargado_…}: sin
     * {@code mocked.constructed()} vacío, un 422 no probaría que no se gastó el solve (el
     * solver también lo daría, INFEASIBLE, medido en S165, M2, T3).
     *
     * <p>El motivo se lee del CUERPO (S166, condición 6): {@code $.mensaje} lleva la MISMA
     * descripción que sirve el GET —la que el diálogo de confirmación ya enseñaba—, y además
     * se exigen sueltos el profesor y el tramo por si la descripción cambiara de forma.
     * {@code estado} y {@code segundos} ausentes o nulos: no hubo solve.
     *
     * <p>Calibrado: 5 tramos, uno por día (L1, M1, X1, J1, V1); Mat-1ºA de 1 repetición y
     * duración 1 con MAT8; DURA de MAT8 en L1 y pin de Mat-1ºA #1 en L1. (a) ve 1 ≤ 5 − 1,
     * (c) 1 ≤ 5, (d) no mira NEUTRA, S8 no aplica.
     */
    @Test
    void pinSobreTramoDura_abortaCon422QueNombraProfesorYTramoSinConstruirElSolver()
            throws Exception {
        Contexto ctx = contextoBase(5);
        crearActividad("Mat-1ºA", 1, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
        TramoSemanal lunes1 = tramoRepository.findAll().stream()
                .filter(t -> t.getDia() == Dia.LUNES).findFirst().orElseThrow();
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                ctx.prof1(), lunes1, TipoRestriccion.DURA, 0, "S165"));
        pinTramoRepository.save(new SesionBloqueada(
                actividadRepository.findByCodigo("Mat-1ºA").orElseThrow(), 1, lunes1));
        entityManager.flush();

        String descripcion = descripcionDe(mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].severidad").value("ERROR"))
                .andExpect(jsonPath("$[0].regla").value("PIN_SOBRE_TRAMO_DURA"))
                .andExpect(jsonPath("$[0].entidadCodigo").value("MAT8"))
                .andExpect(jsonPath("$[0].descripcion").value(containsString("L1")))
                .andReturn(), "PIN_SOBRE_TRAMO_DURA");

        try (MockedConstruction<SolverHorario> mocked =
                     Mockito.mockConstruction(SolverHorario.class)) {

            mockMvc.perform(post("/api/horarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.causa").value("PREVALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.mensaje").value(containsString(descripcion)))
                    .andExpect(jsonPath("$.mensaje").value(containsString("'MAT8'")))
                    .andExpect(jsonPath("$.mensaje").value(containsString("tramo L1")))
                    .andExpect(jsonPath("$.estado").doesNotExist())
                    .andExpect(jsonPath("$.segundos").doesNotExist());

            assertThat(mocked.constructed()).isEmpty();
        }
    }

    /**
     * (F-HTTP-2) DOS errores de reglas distintas en el mismo 422: el cuerpo los lleva los
     * dos, no solo el primero (S166, condición 6). Quien lee el mensaje tiene que poder
     * arreglarlo todo de una vez, no a golpe de generación fallida.
     *
     * <p>Calibrado para que haya EXACTAMENTE dos (lo fija {@code $.length()} del GET): 10
     * tramos, dos por día en los cinco días. Mat-1ºA NEUTRA de 1 repetición con MAT8, DURA
     * de MAT8 en el primer tramo del lunes y pin de Mat-1ºA #1 ahí → PIN_SOBRE_TRAMO_DURA.
     * Len-1ºA DISTRIBUIDA de 6 repeticiones con LEN1 → REPETICIONES_EXCEDEN_DIAS (6 &gt; 5
     * días). (a) ve 1 ≤ 10 − 1 y 6 ≤ 10, (c) ve 7 ≤ 10. El pin se computa DESPUÉS de las
     * repeticiones ({@code PrevalidacionService.prevalidar}), así que un cuerpo con solo el
     * primer error perdería justo el del pin.
     */
    @Test
    void dosErroresDePrevalidacion_elCuerpoDel422LlevaLosDos() throws Exception {
        Contexto ctx = contextoBase(10);
        Profesor len1 = profesorRepository.save(new Profesor("LEN1", "Dos"));
        crearActividad("Mat-1ºA", 1, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
        crearActividad("Len-1ºA", 6, PatronTemporal.DISTRIBUIDA, ctx.asignatura(),
                ctx.aula2(), Set.of(len1), Set.of(ctx.completo()));
        TramoSemanal lunes1 = tramoRepository.findAll().stream()
                .filter(t -> t.getDia() == Dia.LUNES).findFirst().orElseThrow();
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                ctx.prof1(), lunes1, TipoRestriccion.DURA, 0, "S166"));
        pinTramoRepository.save(new SesionBloqueada(
                actividadRepository.findByCodigo("Mat-1ºA").orElseThrow(), 1, lunes1));
        entityManager.flush();

        MvcResult prevalidacion = mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();
        String delPin = descripcionDe(prevalidacion, "PIN_SOBRE_TRAMO_DURA");
        String deRepeticiones = descripcionDe(prevalidacion, "REPETICIONES_EXCEDEN_DIAS");

        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.causa").value("PREVALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.mensaje").value(containsString(deRepeticiones)))
                .andExpect(jsonPath("$.mensaje").value(containsString(delPin)));
    }

    /** La descripción del ÚNICO hallazgo de la regla dada en la respuesta del GET. */
    private static String descripcionDe(MvcResult prevalidacion, String regla) throws Exception {
        List<String> descripciones = JsonPath.read(
                prevalidacion.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$[?(@.regla=='" + regla + "')].descripcion");
        assertThat(descripciones).hasSize(1);
        return descripciones.get(0);
    }

    /** Un catálogo sano pre-valida a {@code 200} con lista VACÍA. */
    @Test
    void catalogoSano_devuelve200ConListaVacia() throws Exception {
        poblarCatalogoSano();
        entityManager.flush();

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------- fixtures

    /**
     * 2 tramos lectivos en 2 días y una actividad DISTRIBUIDA de 3 repeticiones: (d)
     * dispara ERROR (3 > 2 días).
     */
    private void poblarCatalogoConErrorDeRepeticiones() {
        Contexto ctx = contextoBase(2);
        crearActividad("Mat-1ºA", 3, PatronTemporal.DISTRIBUIDA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
    }

    /**
     * 5 tramos lectivos y DOS actividades sobre subgrupos distintos del MISMO grupo, de 3
     * repeticiones cada una: el grupo acumula 6 > 5 → ERROR de (c). Ningún profesor se
     * pasa (3 cada uno ≤ 5) y ninguna es DISTRIBUIDA, así que (c) es el ÚNICO hallazgo y
     * el 422 no puede venir de otra regla.
     */
    private void poblarCatalogoConGrupoSobrecargado() {
        Contexto ctx = contextoBase(5);
        Subgrupo desd1 = subgrupoRepository.save(new Subgrupo("1ºA-Desd1", Set.of(ctx.grupo())));
        Subgrupo desd2 = subgrupoRepository.save(new Subgrupo("1ºA-Desd2", Set.of(ctx.grupo())));
        Profesor prof2 = profesorRepository.save(new Profesor("LEN1", "Dos"));

        crearActividad("CyR-1ºA", 3, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(desd1));
        crearActividad("OyD-1ºA", 3, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula2(), Set.of(prof2), Set.of(desd2));
    }

    /** 5 tramos en 5 días, una actividad de 3 repeticiones: nada dispara. */
    private void poblarCatalogoSano() {
        Contexto ctx = contextoBase(5);
        crearActividad("Mat-1ºA", 3, PatronTemporal.DISTRIBUIDA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
    }

    /**
     * 5 tramos en 5 días y UNA actividad {@code requiereTutor} de 3 repeticiones, sin
     * ninguna fila en {@code profesor_tutoria}: MAT8 no es TUTOR_PRINCIPAL de nada, así
     * que S8 dispara. Ninguna otra regla llega a su techo.
     */
    private void poblarCatalogoConTutoriaSinTutor() {
        Contexto ctx = contextoBase(5);
        crearActividad("Tut-1ºA", 3, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()), true);
    }

    private record Contexto(GrupoAdministrativo grupo, Subgrupo completo, Profesor prof1,
                            Asignatura asignatura, Aula aula1, Aula aula2) { }

    /** Catálogo común: un grupo, su subgrupo completo, un profesor, dos aulas, N tramos. */
    private Contexto contextoBase(int tramosLectivos) {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo completo = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "Uno"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));

        Dia[] dias = {Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES};
        for (int i = 0; i < tramosLectivos; i++) {
            tramoRepository.save(new TramoSemanal(
                    dias[i % dias.length], LocalTime.of(8, 0), LocalTime.of(9, 0), true, i + 1, null));
        }
        return new Contexto(grupo, completo, prof, mat, a1, a2);
    }

    private void crearActividad(String codigo, int repeticiones, PatronTemporal patron,
            Asignatura asignatura, Aula aula, Set<Profesor> profesores, Set<Subgrupo> subgrupos) {
        crearActividad(codigo, repeticiones, patron, asignatura, aula, profesores, subgrupos, false);
    }

    /** Sobrecarga con {@code requiereTutor}, que solo S8 necesita. */
    private void crearActividad(String codigo, int repeticiones, PatronTemporal patron,
            Asignatura asignatura, Aula aula, Set<Profesor> profesores, Set<Subgrupo> subgrupos,
            boolean requiereTutor) {
        Actividad act = new Actividad();
        act.setRequiereTutor(requiereTutor);
        act.setCodigo(codigo);
        act.setAsignatura(asignatura);
        act.setRepeticionesPorSemana(repeticiones);
        act.setDuracionTramos(1);
        act.setPatronTemporal(patron);
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo + "-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(profesores);
        plaza.setAulaFija(aula);
        plaza.setSubgrupos(subgrupos);
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
