package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * El guardado de una generación es atómico por la vía real (C-version-y-rastro, condición 4,
 * S192): si {@code sesionRepository.saveAll} falla, no queda una cabecera sin sesiones y el
 * horario vigente sigue siendo el de antes.
 *
 * <p><b>Por la petición HTTP y sin transacción de test.</b> Dentro de la transacción de un
 * {@code @DataJpaTest} todo acabaría en el mismo rollback y no se mediría nada. Y sin ella,
 * {@code generar()} llamado a pelo no tiene sesión de Hibernate: {@code cargarProblema()} se
 * invoca sobre {@code this} y su {@code @Transactional} no se aplica (medido en S192). En
 * producción la sesión la pone open-in-view al entrar la petición, así que el caso entra por el
 * mismo sitio: {@code POST /api/horarios} con el contexto completo.
 *
 * <p><b>Base propia.</b> Las escrituras llegan a la base de verdad, así que la clase usa un
 * fichero en un {@code @TempDir}, como {@code GeneracionDuranteCambioTest}, y no la de la URL
 * del {@code application.properties} de test (desde S201, un fichero nuevo por contexto de
 * Spring en {@code target/}; antes, una {@code educhronos-test.db} fija y compartida).
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class GuardadoAtomicoGeneracionTest {

    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    private static final String CUERPO = "{\"maxSegundos\": 10, \"semilla\": 42}";

    @Autowired private WebApplicationContext contexto;
    @Autowired private GeneradorHorarioService service;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @MockitoSpyBean private SesionRepository sesionRepository;

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
    void unFalloAlGuardarLasSesionesNoDejaCabeceraNiCambiaElVigente(CapturedOutput salida) throws Exception {
        poblarCatalogo();

        mockMvc.perform(post("/api/horarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
        long filasAntes = horarioRepository.count();
        Optional<Long> vigenteAntes = service.idVigente();
        assertThat(vigenteAntes).isPresent();

        RuntimeException provocado = new RuntimeException("saveAll provocado por el test");
        doThrow(provocado).when(sesionRepository).saveAll(any());

        // El controlador no captura una RuntimeException cualquiera: sale de MockMvc envuelta.
        assertThatThrownBy(() -> mockMvc.perform(
                        post("/api/horarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO)))
                .rootCause()
                .isSameAs(provocado);

        assertThat(horarioRepository.count())
                .as("un saveAll fallido no deja cabecera en horario_generado")
                .isEqualTo(filasAntes);
        assertThat(service.idVigente())
                .as("el vigente sigue siendo el de antes del fallo")
                .isEqualTo(vigenteAntes);

        // El rastro del fallo (C-version-y-rastro, condición 3, S192): línea ERROR de fin con la
        // traza de la excepción del spy debajo.
        assertThat(salida.getOut())
                .containsPattern("ERROR.*Generación terminada desenlace=EXCEPCION duracionMs=\\d+\\R+"
                        + "java\\.lang\\.RuntimeException: saveAll provocado por el test\\R\\s+at ");
    }

    /** Catálogo mínimo de una plaza y cinco tramos de lunes (el de PinTramoGeneracionRoundTripTest, sin el pin). */
    private void poblarCatalogo() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor prof = profesorRepository.save(new Profesor("MAT1", "Profesor MAT1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        for (int orden = 1; orden <= 5; orden++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
        }

        Actividad act = new Actividad();
        act.setCodigo("MAT-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza p = new Plaza();
        p.setCodigo("MAT-1ESO-P1");
        p.setActividad(act);
        p.setAsignatura(mat);
        p.setProfesores(Set.of(prof));
        p.setAulaFija(a1);
        p.setSubgrupos(Set.of(sg));
        act.getPlazas().add(p);
        actividadRepository.save(act);
    }
}
