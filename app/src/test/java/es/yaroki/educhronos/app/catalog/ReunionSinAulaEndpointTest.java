package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.ActividadService;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.MovimientoInstanciaService;
import es.yaroki.educhronos.app.web.ActividadController;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.MovimientoInstanciaController;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * Una REUNIÓN sin aula, de la API a las salidas (S201, C-actividad-sin-alumnos, T3): se da de
 * alta por {@code POST /api/actividades}, se genera el horario y su sesión recorre la base, la
 * proyección, el CSV, el movimiento y el diagnóstico sin aula y sin que nada la confunda con
 * una clase.
 *
 * <p>Mismo montaje que {@code GenerarHorarioEndpointTest} —{@code @DataJpaTest} con los
 * servicios reales y {@code standaloneSetup} sobre los controladores reales—, y las mismas
 * cautelas de transacción: se hace {@code flush()+clear()} entre el POST y las lecturas. El
 * catálogo base (tramos, aula, asignaturas, profesores, grupo y subgrupo) se siembra con
 * repositorios, como allí; las dos ACTIVIDADES entran por la API, para que la reunión pase por
 * la validación de tipo de {@code ActividadService}.
 *
 * <p>Cuatro tramos, uno por día. P1 da la clase y asiste a la reunión, así que caen en tramos
 * distintos y quedan dos libres para moverla.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class,
        MovimientoInstanciaService.class, ActividadService.class})
class ReunionSinAulaEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService generadorService;
    @Autowired private DiagnosticoService diagnosticoService;
    @Autowired private MovimientoInstanciaService movimientoService;
    @Autowired private ActividadService actividadService;
    @Autowired private SesionRepository sesionRepository;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;

    private MockMvc mockMvc;

    /** Centro sin guardias (S212): con el mínimo 4 por defecto, generar daría GUARDIAS_INSUFICIENTES. */
    @BeforeEach
    void centroSinGuardias(@Autowired ConfiguracionRepository configuraciones) {
        MinimoGuardias.fijar(configuraciones, 0);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ActividadController(actividadService),
                        new HorarioController(generadorService, diagnosticoService,
                                mock(ExportacionHorarioService.class)),
                        new MovimientoInstanciaController(movimientoService))
                .build();
    }

    /** g1. En base, la sesión de la reunión no tiene aula y la de la clase está en A1. */
    @Test
    void g1_enBaseLaSesionDeLaReunionNoTieneAula() throws Exception {
        long horario = generar();

        List<Sesion> sesiones = sesionRepository.findByHorarioId(horario);
        assertThat(sesiones).hasSize(2);
        assertThat(sesionDe(sesiones, "REU").getAula()).as("la reunión, sin aula").isNull();
        assertThat(sesionDe(sesiones, "CLA").getAula().getCodigo()).as("la clase, en A1").isEqualTo("A1");
    }

    /**
     * g2. La proyección trae la reunión con su asignatura, sus dos profesores, sin aula y sin
     * grupos; y ninguna otra entrada sin aula.
     */
    @Test
    void g2_laProyeccionTraeLaReunionSinAulaNiGrupos() throws Exception {
        long horario = generar();

        List<Map<String, Object>> sesiones = proyeccion(horario);

        Map<String, Object> reu = entradaDe(sesiones, "REU");
        assertThat(reu.get("asignaturaCodigo")).isEqualTo("RED");
        assertThat(reu.get("aulaCodigo")).isNull();
        assertThat(reu.get("profesores")).isEqualTo(List.of("P1", "P2"));
        assertThat(reu.get("grupos")).isEqualTo(List.of());
        assertThat(sesiones)
                .filteredOn(s -> s.get("aulaCodigo") == null)
                .as("sólo la reunión va sin aula")
                .extracting(s -> s.get("actividadCodigo"))
                .containsOnly("REU");
        assertThat(entradaDe(sesiones, "CLA").get("aulaCodigo")).isEqualTo("A1");
    }

    /** g3. En el CSV, la fila de la reunión lleva vacía la columna Aula. */
    @Test
    void g3_elCsvDejaVaciaLaColumnaAulaDeLaReunion() throws Exception {
        long horario = generar();

        MvcResult res = mockMvc.perform(get("/api/horarios/" + horario + "/csv"))
                .andExpect(status().isOk())
                .andReturn();
        String texto = new String(res.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String[] lineas = texto.replace("﻿", "").split("\r\n");
        List<String> cabecera = Arrays.asList(lineas[0].split(";", -1));
        int colAula = cabecera.indexOf("Aula");
        int colActividad = cabecera.indexOf("Actividad");
        assertThat(colAula).as("la cabecera tiene columna Aula").isNotNegative();
        assertThat(colActividad).as("la cabecera tiene columna Actividad").isNotNegative();

        List<String[]> filasReu = Arrays.stream(lineas).skip(1)
                .map(l -> l.split(";", -1))
                .filter(campos -> campos[colActividad].equals("REU"))
                .toList();
        assertThat(filasReu).as("una fila de la reunión").hasSize(1);
        assertThat(filasReu.get(0)[colAula]).as("su Aula, vacía").isEmpty();
    }

    /** g4. Mover la reunión a un tramo libre: 2xx, y su sesión sigue sin aula en el tramo nuevo. */
    @Test
    void g4_moverLaReunionLaDejaSinAulaEnElTramoNuevo() throws Exception {
        long horario = generar();
        List<Map<String, Object>> sesiones = proyeccion(horario);
        int diaCla = (Integer) entradaDe(sesiones, "CLA").get("dia");
        int diaReu = (Integer) entradaDe(sesiones, "REU").get("dia");
        int libre = java.util.stream.IntStream.rangeClosed(1, 4)
                .filter(d -> d != diaCla && d != diaReu)
                .findFirst().orElseThrow();

        mockMvc.perform(put("/api/horarios/" + horario + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividadCodigo\":\"REU\",\"indice\":1,\"dia\":" + libre
                                + ",\"orden\":1}"))
                .andExpect(status().is2xxSuccessful());

        entityManager.flush();
        entityManager.clear();
        Sesion reu = sesionDe(sesionRepository.findByHorarioId(horario), "REU");
        assertThat(reu.getTramoInicio().getDia().ordinal() + 1).as("en el tramo nuevo").isEqualTo(libre);
        assertThat(reu.getAula()).as("y sigue sin aula").isNull();
    }

    /** g5. El diagnóstico responde, sin violaciones duras y sin instancias por colocar. */
    @Test
    void g5_elDiagnosticoNoVeViolacionesNiInstanciasSinColocar() throws Exception {
        long horario = generar();

        MvcResult res = mockMvc.perform(get("/api/horarios/" + horario + "/diagnostico"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        List<String> reglas = JsonPath.read(res.getResponse().getContentAsString(), "$.violaciones[*].regla");
        assertThat(reglas).as("violaciones duras").isEmpty();
        assertThat(reglas).doesNotContain("INSTANCIA_SIN_COLOCAR");
    }

    // ------------------------------------------------------------------ andamio

    /** Siembra el catálogo, da de alta CLA y REU por la API, genera y devuelve el id del horario. */
    private long generar() throws Exception {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g =
                grupoRepository.save(new GrupoAdministrativo("G", eso1, TipoGrupo.ORDINARIO, null));
        subgrupoRepository.save(new Subgrupo("S", Set.of(g)));
        profesorRepository.save(new Profesor("P1", "Profesor Uno"));
        profesorRepository.save(new Profesor("P2", "Profesor Dos"));
        asignaturaRepository.save(new Asignatura("MAT", "Matemáticas"));
        asignaturaRepository.save(new Asignatura("RED", "Reunión de departamento"));
        aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Dia[] dias = {Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES};
        for (int i = 0; i < dias.length; i++) {
            tramoRepository.save(new TramoSemanal(
                    dias[i], LocalTime.of(8, 0), LocalTime.of(9, 0), true, i + 1, null));
        }
        entityManager.flush();

        alta("{\"codigo\":\"CLA\",\"asignatura\":\"MAT\",\"duracionTramos\":1,"
                + "\"repeticionesPorSemana\":1,\"patronTemporal\":\"NEUTRA\",\"requiereTutor\":false,"
                + "\"plazas\":[{\"asignatura\":\"MAT\",\"aulaFija\":\"A1\",\"aulasCandidatas\":[],"
                + "\"profesores\":[\"P1\"],\"subgrupos\":[\"S\"]}]}");
        alta("{\"codigo\":\"REU\",\"tipo\":\"REUNION\",\"asignatura\":\"RED\",\"duracionTramos\":1,"
                + "\"repeticionesPorSemana\":1,\"patronTemporal\":\"NEUTRA\",\"requiereTutor\":false,"
                + "\"plazas\":[{\"asignatura\":\"RED\",\"aulaFija\":null,\"aulasCandidatas\":[],"
                + "\"profesores\":[\"P1\",\"P2\"],\"subgrupos\":[]}]}");
        entityManager.flush();

        MvcResult res = mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxSegundos\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        Number id = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        // Contexto fresco (nota de transacciones de GenerarHorarioEndpointTest).
        entityManager.flush();
        entityManager.clear();
        return id.longValue();
    }

    private void alta(String cuerpo) throws Exception {
        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());
    }

    private List<Map<String, Object>> proyeccion(long horario) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/horarios/" + horario + "/proyeccion"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.sesiones");
    }

    private static Map<String, Object> entradaDe(List<Map<String, Object>> sesiones, String actividad) {
        return sesiones.stream()
                .filter(s -> actividad.equals(s.get("actividadCodigo")))
                .reduce((a, b) -> {
                    throw new AssertionError("más de una entrada de " + actividad);
                })
                .orElseThrow(() -> new AssertionError("ninguna entrada de " + actividad));
    }

    private static Sesion sesionDe(List<Sesion> sesiones, String actividad) {
        return sesiones.stream()
                .filter(s -> s.getPlaza().getActividad().getCodigo().equals(actividad))
                .reduce((a, b) -> {
                    throw new AssertionError("más de una sesión de " + actividad);
                })
                .orElseThrow(() -> new AssertionError("ninguna sesión de " + actividad));
    }
}
