package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.BloqueoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.BloqueoController;
import es.yaroki.educhronos.app.web.dto.AulaPinDTO;
import es.yaroki.educhronos.app.web.dto.BloqueoRequest;
import es.yaroki.educhronos.app.web.dto.TramoRefDTO;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
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
 * Test de integración del endpoint {@code /api/bloqueos} (Fase 8, Bloque 8.2b-iv).
 * Ejerce el alta/reemplazo, listado y borrado POR LA RED ({@code standaloneSetup} +
 * {@code BloqueoService} real sobre {@code @DataJpaTest}), más el test de contrato
 * (test 6) que ata el validador del alta con {@code BloqueoMapper} vía
 * {@code cargarProblema()}. Vive en {@code app.catalog} para construir entidades
 * {@code Actividad}/{@code Plaza} (ctor {@code protected}), igual que
 * {@code GenerarHorarioEndpointTest}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BloqueoService.class, GeneradorHorarioService.class})
class BloqueoEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private BloqueoService bloqueoService;
    @Autowired private GeneradorHorarioService generadorService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private SesionBloqueadaRepository sesionBloqueadaRepository;
    @Autowired private AulaBloqueadaRepository aulaBloqueadaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BloqueoController(bloqueoService)).build();
        poblarCatalogo();
        entityManager.flush();
    }

    @Test
    void post_pinDeTramoSinAulas_seCreaSeListaYSeBorra() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/bloqueos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1, "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadCodigo").value("ActVar"))
                .andExpect(jsonPath("$.aulas.length()").value(0))
                .andReturn();

        mockMvc.perform(get("/api/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actividadCodigo").value("ActVar"))
                .andExpect(jsonPath("$[0].tramo.dia").value(1))
                .andExpect(jsonPath("$[0].tramo.orden").value(1));

        Number id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/bloqueos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void post_pinConAulaSobrePlazaVariable_devuelveElAula() throws Exception {
        mockMvc.perform(post("/api/bloqueos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1,
                                "[{\"plazaCodigo\":\"PVar\",\"aulaCodigo\":\"AV1\"}]")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aulas.length()").value(1))
                .andExpect(jsonPath("$[0].aulas[0].plazaCodigo").value("PVar"))
                .andExpect(jsonPath("$[0].aulas[0].aulaCodigo").value("AV1"));
    }

    @Test
    void post_entradasInvalidas_devuelven400() throws Exception {
        // aula sobre plaza de AULA FIJA
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActFija", 1, 1, 1,
                                "[{\"plazaCodigo\":\"PFija\",\"aulaCodigo\":\"A1\"}]")))
                .andExpect(status().isBadRequest());

        // aula NO candidata de la plaza
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1,
                                "[{\"plazaCodigo\":\"PVar\",\"aulaCodigo\":\"A1\"}]")))
                .andExpect(status().isBadRequest());

        // plazaCodigo de OTRA actividad
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1,
                                "[{\"plazaCodigo\":\"PFija\",\"aulaCodigo\":\"AV1\"}]")))
                .andExpect(status().isBadRequest());

        // indice 0 (el dominio es 1-based)
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 0, 1, 1, "[]")))
                .andExpect(status().isBadRequest());

        // (dia, orden) inexistente (no hay 9º tramo)
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 3, 9, "[]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_dosVecesMismaInstancia_reemplazaElTramo() throws Exception {
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1, "[]")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 2, 1, "[]")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tramo.dia").value(2))
                .andExpect(jsonPath("$[0].tramo.orden").value(1));
    }

    @Test
    void post_reemplazoSinAulas_borraLasAulasPrevias() throws Exception {
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1,
                                "[{\"plazaCodigo\":\"PVar\",\"aulaCodigo\":\"AV1\"}]")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ActVar", 1, 1, 1, "[]")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].aulas.length()").value(0));
    }

    @Test
    void humo_altaValidaLlegaAlProblemaHorario() {
        bloqueoService.guardar(new BloqueoRequest(
                "ActVar", 1, new TramoRefDTO(1, 1), List.of(new AulaPinDTO("PVar", "AV1"))));
        entityManager.flush();

        // Humo (camino feliz): un alta valida termina en el ProblemaHorario con su pin de
        // aula. NO es el test de contrato: no discrimina si los dos validadores divergen.
        ProblemaHorario problema = generadorService.cargarProblema();

        var bloqueo = problema.bloqueos().stream()
                .filter(b -> b.instancia().actividad().codigo().equals("ActVar")
                        && b.instancia().indice() == 1)
                .findFirst();
        assertThat(bloqueo).isPresent();
        assertThat(bloqueo.get().tramo()).isNotNull();
        assertThat(bloqueo.get().aulasPinadas()).hasSize(1);
        var pin = bloqueo.get().aulasPinadas().entrySet().iterator().next();
        assertThat(pin.getKey().codigo()).isEqualTo("PVar");
        assertThat(pin.getValue().codigo()).isEqualTo("AV1");
    }

    @Test
    void contrato_pinQueElAltaRechazaLoRechazaTambienElMapper() {
        // Inyecta DIRECTAMENTE en BD (saltandose BloqueoService) un pin que el alta
        // rechazaria por la regla (e): un pin de aula cuya aula (A1) NO es candidata de la
        // plaza variable PVar. Se acompana de su pin de tramo para que no sea huerfano.
        Actividad actVar = actividadRepository.findByCodigo("ActVar").orElseThrow();
        Plaza pVar = actVar.getPlazas().stream()
                .filter(p -> p.getCodigo().equals("PVar")).findFirst().orElseThrow();
        Aula a1 = aulaRepository.findByCodigo("A1").orElseThrow();
        TramoSemanal tramo = tramoRepository.findAll().get(0);

        sesionBloqueadaRepository.save(new SesionBloqueada(actVar, 1, tramo));
        aulaBloqueadaRepository.save(new AulaBloqueada(actVar, 1, pVar, a1));
        entityManager.flush();

        // El oro del bloque: cargarProblema() pasa por BloqueoMapper, que DEBE cubrir la
        // misma frontera (e) que el alta. Si NO lanzara aqui, el test quedaria ROJO: ese
        // seria el hallazgo (validadores divergidos), no un fallo del test.
        assertThatThrownBy(() -> generadorService.cargarProblema())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidata");
    }

    /** {@code {"actividadCodigo":..,"indice":..,"tramo":{"dia":..,"orden":..},"aulas":..}} */
    private static String body(String actividad, int indice, int dia, int orden, String aulas) {
        return ("{\"actividadCodigo\":\"" + actividad + "\",\"indice\":" + indice
                + ",\"tramo\":{\"dia\":" + dia + ",\"orden\":" + orden + "},\"aulas\":" + aulas + "}");
    }

    /**
     * Catálogo mínimo con dos actividades de una repetición: {@code ActVar} (plaza
     * {@code PVar} de aula VARIABLE, candidatas AV1/AV2) y {@code ActFija} (plaza
     * {@code PFija} de aula FIJA A1); cinco tramos lectivos, uno por día
     * (ordenEnDia=1 cada día, recreos excluidos).
     */
    private void poblarCatalogo() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("G1", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo completo = subgrupoRepository.save(new Subgrupo("G1-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("P8", "Docente"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matematicas"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula av1 = aulaRepository.save(new Aula("AV1", TipoAula.ORDINARIA, null, null, null, null));
        Aula av2 = aulaRepository.save(new Aula("AV2", TipoAula.ORDINARIA, null, null, null, null));

        Dia[] dias = {Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES};
        for (int i = 0; i < dias.length; i++) {
            tramoRepository.save(new TramoSemanal(
                    dias[i], LocalTime.of(8, 0), LocalTime.of(9, 0), true, i + 1, null));
        }

        Actividad actVar = new Actividad();
        actVar.setCodigo("ActVar");
        actVar.setAsignatura(mat);
        actVar.setRepeticionesPorSemana(1);
        actVar.setDuracionTramos(1);
        actVar.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        Plaza pVar = new Plaza();
        pVar.setCodigo("PVar");
        pVar.setActividad(actVar);
        pVar.setAsignatura(mat);
        pVar.setProfesores(Set.of(prof));
        pVar.setSubgrupos(Set.of(completo));
        pVar.setAulasCandidatas(Set.of(av1, av2));
        actVar.getPlazas().add(pVar);
        actividadRepository.save(actVar);

        Actividad actFija = new Actividad();
        actFija.setCodigo("ActFija");
        actFija.setAsignatura(mat);
        actFija.setRepeticionesPorSemana(1);
        actFija.setDuracionTramos(1);
        actFija.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        Plaza pFija = new Plaza();
        pFija.setCodigo("PFija");
        pFija.setActividad(actFija);
        pFija.setAsignatura(mat);
        pFija.setProfesores(Set.of(prof));
        pFija.setSubgrupos(Set.of(completo));
        pFija.setAulaFija(a1);
        actFija.getPlazas().add(pFija);
        actividadRepository.save(actFija);
    }
}
