package es.yaroki.educhronos.app.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.HorarioController;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
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
 * Test de integración HTTP del endpoint {@code POST /api/horarios} (Fase 8,
 * Bloque 8.1). Ejerce la vía completa POR LA RED: binding de la ruta + cuerpo →
 * {@link GeneradorHorarioService#generar} (carga, solve, persiste) → proyección,
 * y la traducción de {@code HorarioInfactibleException} a {@code 422}.
 *
 * <p>Como {@code GuardarHorarioServiceTest} / {@code CierreFase6HumoTest}, vive en
 * {@code app.catalog} para construir entidades {@code Actividad}/{@code Plaza} (ctor
 * {@code protected}). Combina el servicio real ({@code @DataJpaTest} +
 * {@code @Import}) con {@code MockMvcBuilders.standaloneSetup} sobre el controlador
 * real, montando el mismo binding de producción sin el slice web de Boot.
 *
 * <p>Presupuesto BAJO a propósito (lección D24/D25: nada de solves largos en la
 * suite rápida): los catálogos son mínimos y se resuelven —o se prueban
 * infactibles— en milisegundos. El caso 200 usa cuerpo vacío ({@code {}}, todos
 * los parámetros por defecto) porque el problema trivial demuestra optimalidad al
 * instante; el caso 422 acota además con {@code maxSegundos} pequeño.
 *
 * <p><b>Nota sobre transacciones.</b> {@code @DataJpaTest} envuelve el test en UNA
 * transacción (rollback al final, y con {@code replace = NONE} eso evita ensuciar
 * {@code educhronos.db}). Por eso las tres llamadas del servicio (carga, guardar,
 * proyectar) comparten contexto de persistencia y {@code proyectar}, dentro del
 * POST, ve la colección inversa {@code sesiones} aún vacía —artefacto del harness,
 * no de producción, donde cada método {@code @Transactional} abre su propio
 * contexto—. Para verificar que las sesiones SÍ se persistieron y son proyectables,
 * el caso 200 hace {@code flush()+clear()} (mismo patrón que {@code CierreFase6HumoTest})
 * y las lee por un GET posterior sobre contexto fresco.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class GenerarHorarioEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new HorarioController(service)).build();
    }

    @Test
    void post_conCatalogoFactible_devuelve200YProyeccionConSesiones() throws Exception {
        poblarCatalogoMinimo(1, 5); // 1 repetición, 5 tramos lectivos: factible.
        entityManager.flush();

        MvcResult respuesta = mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.estadoSolver").isNotEmpty())
                .andReturn();

        // Contexto fresco (ver nota de transacciones): la proyección lee de BD las
        // sesiones ya persistidas por el POST.
        Number id = JsonPath.read(respuesta.getResponse().getContentAsString(), "$.id");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/horarios/" + id + "/proyeccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesiones").isNotEmpty());
    }

    @Test
    void post_conCatalogoInfactible_devuelve422() throws Exception {
        // 2 repeticiones de la MISMA actividad (mismo profesor/subgrupo/aula) pero un
        // ÚNICO tramo lectivo: las dos instancias no caben en el mismo tramo -> INFEASIBLE.
        poblarCatalogoMinimo(2, 1);
        entityManager.flush();

        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxSegundos\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Catálogo mínimo: una actividad de una plaza (aula fija) con {@code repeticiones}
     * repeticiones y {@code lectivos} tramos lectivos (lunes..). Solvable si
     * {@code lectivos >= repeticiones}; infactible en caso contrario.
     */
    private void poblarCatalogoMinimo(int repeticiones, int lectivos) {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo completo = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        Dia[] dias = {Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES};
        for (int i = 0; i < lectivos; i++) {
            tramoRepository.save(new TramoSemanal(
                    dias[i % dias.length], LocalTime.of(8, 0), LocalTime.of(9, 0), true, i + 1, null));
        }

        Actividad act = new Actividad();
        act.setCodigo("Mat-1ºA");
        act.setAsignatura(mat);
        act.setRepeticionesPorSemana(repeticiones);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("Mat-1ºA-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(prof));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(completo));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
