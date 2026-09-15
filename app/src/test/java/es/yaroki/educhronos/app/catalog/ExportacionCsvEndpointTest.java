package es.yaroki.educhronos.app.catalog;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.HorarioController;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests del {@code GET /api/horarios/{id}/csv} (S148, C-exportacion-csv) POR LA RED.
 *
 * <p>Mismas anotaciones y mismo montaje que {@code MovimientoInstanciaEndpointTest},
 * y por las mismas razones: vive en {@code app.catalog} por el ctor {@code protected}
 * de {@code Actividad}/{@code Plaza}, corre sobre SQLite real
 * ({@code replace = NONE}) y persiste las sesiones A MANO, sin pasar por el solver,
 * para que los casos sean deterministas (D-generacion-no-reproducible). Clase propia
 * y no un método más en aquélla: aquélla prueba el PUT de instancias, y un fichero
 * de descarga no es un caso de ese endpoint.
 *
 * <p>Malla: LUNES con 3 tramos lectivos (ordenEnDia 1..3).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, DiagnosticoService.class})
class ExportacionCsvEndpointTest {

    private static final String CABECERA =
            "Día;Tramo;Asignatura;Nombre asignatura;Profesores;Aula;Grupos;Subgrupos;"
                    + "Actividad;Plaza;Índice;Sesión";

    private static final String MAT = "MAT-1A";
    private static final String LEN = "LEN-1A";

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService generadorService;
    @Autowired private DiagnosticoService diagnosticoService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;

    private MockMvc mockMvc;
    private Long horarioId;

    @BeforeEach
    void montar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(generadorService, diagnosticoService,
                        mock(ExportacionHorarioService.class)))
                .build();
    }

    // ------------------------------------------------------------------ E1

    @Test
    void devuelve200ConLasCabecerasDeDescargaYUnRegistroPorSesion() throws Exception {
        poblar();
        // GUARDA: sin esto, una fixture que dejara el horario vacío haría pasar el
        // recuento de abajo con 1 == 1 + 0 sin exportar ni una sesión.
        long sesiones = sesionRepository.count();
        assertThat(sesiones).as("el horario de la fixture tiene sesiones").isPositive();

        entityManager.flush();
        entityManager.clear();

        byte[] csv = mockMvc.perform(get("/api/horarios/" + horarioId + "/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"horario-" + horarioId + ".csv\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new byte[] {csv[0], csv[1], csv[2]})
                .containsExactly((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(registros(csv)).hasSize((int) sesiones + 1);
    }

    // ------------------------------------------------------------------ E2

    /**
     * Hermano de {@code elCuerpoDel200CoincideConLaProyeccionDeEsaInstancia}: lo que
     * impide que el CSV y la proyección JSON diverjan en silencio.
     *
     * <p>El aplanado esperado se construye AQUÍ, a partir del JSON, sin llamar a
     * {@code HorarioCsv}: si lo llamara, el test compararía la función consigo misma
     * y no podría fallar. Ningún valor de la fixture lleva {@code ;}, comillas ni
     * saltos, así que el escape no interviene y la unión por {@code ";"} basta; el
     * escape se prueba aparte, en {@code HorarioCsvTest}.
     */
    @Test
    void losRegistrosCoincidenConLasSesionesDeLaProyeccionEnOrdenYCampoACampo() throws Exception {
        poblar();
        entityManager.flush();
        entityManager.clear();

        String json = mockMvc.perform(get("/api/horarios/" + horarioId + "/proyeccion"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();

        byte[] csv = mockMvc.perform(get("/api/horarios/" + horarioId + "/csv"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<Map<String, Object>> sesiones = JsonPath.read(json, "$.sesiones");
        List<String> esperados = new ArrayList<>();
        for (Map<String, Object> s : sesiones) {
            esperados.add(String.join(";",
                    String.valueOf(s.get("dia")),
                    String.valueOf(s.get("tramo")),
                    (String) s.get("asignaturaCodigo"),
                    (String) s.get("asignaturaNombre"),
                    unir(s.get("profesores")),
                    (String) s.get("aulaCodigo"),
                    unir(s.get("grupos")),
                    unir(s.get("subgrupos")),
                    (String) s.get("actividadCodigo"),
                    (String) s.get("plazaCodigo"),
                    String.valueOf(s.get("indice")),
                    String.valueOf(s.get("sesionId"))));
        }

        List<String> registros = registros(csv);
        assertThat(registros).hasSize(esperados.size() + 1);
        assertThat(registros.get(0)).isEqualTo(CABECERA);
        assertThat(registros.subList(1, registros.size()))
                .containsExactlyElementsOf(esperados);
    }

    // ------------------------------------------------------------------ E3

    @Test
    void conIdInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/horarios/9999/csv"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    private static String unir(Object lista) {
        return String.join("/", (List<String>) lista);
    }

    /** Las líneas del fichero, sin BOM y sin el vacío que deja el CRLF final. */
    private static List<String> registros(byte[] csv) {
        String texto = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(texto).endsWith("\r\n");
        return List.of(texto.substring(0, texto.length() - 2).split("\r\n", -1));
    }

    // ------------------------------------------------------------------ fixture

    private TramoSemanal tramoDe(int ordenEnDia) {
        return tramoRepository.findAll().stream()
                .filter(t -> t.getDia() == Dia.LUNES && t.getOrden() == ordenEnDia)
                .findFirst().orElseThrow();
    }

    /** Catálogo mínimo y un horario con las dos instancias colocadas. */
    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g1a = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sgA1 = subgrupoRepository.save(new Subgrupo("1ºA-s1", Set.of(g1a)));
        Subgrupo sgA2 = subgrupoRepository.save(new Subgrupo("1ºA-s2", Set.of(g1a)));

        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Asignatura len = asignaturaRepository.save(new Asignatura("LEN", "Lengua"));

        Profesor pMat = profesorRepository.save(new Profesor("P-MAT", "Profesor MAT"));
        Profesor pLen = profesorRepository.save(new Profesor("P-LEN", "Profesor LEN"));

        Aula aMat = aulaRepository.save(
                new Aula("A-MAT", TipoAula.ORDINARIA, null, null, null, null));
        Aula aLen = aulaRepository.save(
                new Aula("A-LEN", TipoAula.ORDINARIA, null, null, null, null));

        for (int i = 1; i <= 3; i++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(8 + i, 0), LocalTime.of(9 + i, 0), true, i, null));
        }

        Actividad actMat = actividad(MAT);
        actMat.getPlazas().add(plaza(MAT + "-P1", actMat, mat, Set.of(pMat), aMat, Set.of(sgA1)));
        actividadRepository.save(actMat);

        Actividad actLen = actividad(LEN);
        actLen.getPlazas().add(plaza(LEN + "-P1", actLen, len, Set.of(pLen), aLen, Set.of(sgA2)));
        actividadRepository.save(actLen);
        entityManager.flush();

        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "Horario de S148", Instant.parse("2026-09-11T08:00:00Z"), "OPTIMAL", 0.0, 0.0));
        sesionRepository.save(new es.yaroki.educhronos.app.persistence.Sesion(
                horario, actMat.getPlazas().get(0), 1, tramoDe(1), aMat));
        sesionRepository.save(new es.yaroki.educhronos.app.persistence.Sesion(
                horario, actLen.getPlazas().get(0), 1, tramoDe(2), aLen));
        horarioId = horario.getId();
        entityManager.flush();
        // clear() para que todo se lea de la TABLA: las filas se insertaron sin tocar
        // la colección inversa del horario (mismo motivo que en la fixture hermana).
        entityManager.clear();
    }

    private static Actividad actividad(String codigo) {
        Actividad a = new Actividad();
        a.setCodigo(codigo);
        a.setRepeticionesPorSemana(1);
        a.setDuracionTramos(1);
        a.setPatronTemporal(PatronTemporal.NEUTRA);
        return a;
    }

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
