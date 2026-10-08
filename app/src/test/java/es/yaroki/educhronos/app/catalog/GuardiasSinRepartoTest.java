package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Un reparto de guardias que no llega al mínimo no deja nada escrito (S213, C-reparto-guardias,
 * K6 y K9 b): 422 {@code GUARDIAS_SIN_REPARTO}, con los tramos en el mensaje, y el mismo número de
 * filas en {@code horario_generado}, {@code sesion} y {@code guardia} antes y después.
 *
 * <p><b>Por la petición HTTP y sin transacción de test</b>, como {@code GuardadoAtomicoGeneracionTest}
 * y por lo mismo: dentro de un {@code @DataJpaTest} todo acabaría en el mismo rollback y el recuento no
 * mediría nada. Base propia en un {@code @TempDir}.
 *
 * <p><b>El caso.</b> Cinco tramos y tres profesores: MAT1 y LEN1 dan juntos una clase de una
 * repetición en una sola plaza; GUA1 no da clase. Guardias 4, 4 y 5. Con mínimo 2 la pre-validación
 * pasa (13 ≥ 2 × 5; 1 + 4 ≤ 5), pero en el tramo de la clase, sea cual sea el que elija el solver,
 * solo queda libre GUA1: 1 de 2. Con mínimo 1 el mismo catálogo sí se reparte.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class GuardiasSinRepartoTest {

    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    private static final String CUERPO = "{\"maxSegundos\": 10, \"semilla\": 42}";

    @Autowired private WebApplicationContext contexto;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;
    @Autowired private GuardiaRepository guardiaRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void sinRepartoPosible_422ConLosTramosYNingunaFilaNueva(CapturedOutput salida) throws Exception {
        poblarCatalogo();

        // Primero un horario bueno, con mínimo 1: deja filas en las tres tablas.
        MinimoGuardias.fijar(configuracionRepository, 1);
        mockMvc.perform(post("/api/horarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
        List<Long> antes = List.of(horarioRepository.count(), sesionRepository.count(), guardiaRepository.count());
        assertThat(antes).as("precondición: un horario con sesiones y 13 guardias").containsExactly(1L, 1L, 13L);

        MinimoGuardias.fijar(configuracionRepository, 2);
        mockMvc.perform(post("/api/horarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.causa").value("GUARDIAS_SIN_REPARTO"))
                .andExpect(jsonPath("$.mensaje").value(matchesPattern(
                        "No se pueden repartir las guardias ordinarias: en estas horas no se llega al mínimo"
                                + " de 2 profesores de guardia: tramo L[1-5] \\(día 1, tramo [1-5]\\): 1 de 2\\."
                                + " Los profesores libres en esas horas no tienen guardias suficientes\\. Baja el"
                                + " mínimo, da más guardias a esos profesores o deja libres a más profesores en"
                                + " esas horas\\.")))
                .andExpect(jsonPath("$.estado").doesNotExist())
                .andExpect(jsonPath("$.segundos").doesNotExist());

        assertThat(List.of(horarioRepository.count(), sesionRepository.count(), guardiaRepository.count()))
                .as("horario_generado, sesion y guardia, como antes del fallo")
                .isEqualTo(antes);
        assertThat(salida.getOut())
                .containsPattern("WARN.*Generación terminada desenlace=GUARDIAS_SIN_REPARTO tramos=L[1-5]:1/2"
                        + " duracionMs=\\d+");
    }

    /** Cinco tramos de lunes; MAT1 y LEN1 juntos en una plaza (aula fija); GUA1 sin clase. */
    private void poblarCatalogo() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor mat1 = profesor("MAT1", 4);
        Profesor len1 = profesor("LEN1", 4);
        profesor("GUA1", 5);
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        for (int orden = 1; orden <= 5; orden++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
        }

        Actividad act = new Actividad();
        act.setCodigo("JUNTOS-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("JUNTOS-1ESO-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(mat1, len1));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }

    private Profesor profesor(String codigo, int guardias) {
        Profesor profesor = new Profesor(codigo, "Profesor " + codigo);
        profesor.setGuardiasOrdinarias(guardias);
        return profesorRepository.save(profesor);
    }
}
