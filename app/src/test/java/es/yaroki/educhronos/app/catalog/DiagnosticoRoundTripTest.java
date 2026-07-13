package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests del Bloque 8.3-C (REST de atribución): el round-trip
 * {@code SolucionHorario → Sesion → SolucionHorario} de
 * {@link SolucionMapper#aSolucionHorario} y el endpoint {@code GET /{id}/diagnostico}.
 *
 * <p>Vive en {@code app.catalog} por el ctor {@code protected} de {@code Actividad}/
 * {@code Plaza}, y corre sobre SQLite real ({@code replace = NONE}) con la transacción
 * única del harness ({@code @DataJpaTest}, rollback al final): esa única sesión de
 * Hibernate es la que preserva la IDENTIDAD DE OBJETO de {@code TramoSemanal} que la
 * inversión de {@code idxTramo} necesita.
 *
 * <p>Las aserciones de FIDELIDAD del mapa {@code aulasElegidas} (D-F8.3-C-3) leen el
 * campo privado por REFLEXIÓN: {@code SolucionHorario} no expone un getter del mapa y
 * {@code solver/src/main} está congelado en este bloque. {@link SolucionHorario#aulaElegida}
 * NO sirve para discriminar "plaza fija en el mapa" de "plaza fija ausente" (devuelve la
 * {@code aulaFija} en ambos casos), justo la propiedad que protege el oro negativo.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, DiagnosticoService.class})
class DiagnosticoRoundTripTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private DiagnosticoService diagnosticoService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;
    @Autowired private SesionBloqueadaRepository sesionBloqueadaRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(service, diagnosticoService))
                .build();
    }

    // ------------------------------------------------------------------ Test 1: ORO

    @Test
    void roundTripReconstruyeAsignacionesYRespetaLaFidelidadDeAulas() {
        poblarFijaYCandidata();
        entityManager.flush();
        entityManager.clear();

        // Vía real (lo que hace generar() por dentro), maxSegundos=10 EXPLÍCITO (D24/D25):
        // se decompone para CAPTURAR la SolucionHorario que devuelve el solver, que
        // generar() no expone.
        ProblemaHorario problemaOrig = service.cargarProblema();
        ResultadoOptimizacion resultado =
                new SolverHorario(10, 42).resolverOptimizandoConDetalle(problemaOrig);
        SolucionHorario solucionOrig = resultado.solucion();
        Long id = service.guardar(resultado, problemaOrig, "diag-roundtrip").getId();

        entityManager.flush();
        entityManager.clear();

        // Reconstrucción como en producción: carga FRESCA del problema (ejercita la
        // igualdad estructural de Actividad entre dos cargas, TAREA 0.c).
        List<Sesion> sesiones = service.cargarHorario(id).getSesiones();
        ProblemaHorario problemaRecon = service.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo =
                SolucionMapper.indiceTramos(problemaRecon, tramoRepository.findAll());
        SolucionHorario recon =
                SolucionMapper.aSolucionHorario(problemaRecon, sesiones, idxTramo);

        // (1) asignaciones() reconstruido IGUAL al original (Map.equals: claves
        // ActividadInstancia y valores Tramo, estructurales).
        assertThat(recon.asignaciones()).isEqualTo(solucionOrig.asignaciones());

        // (2)/(3) fidelidad del mapa de aulas elegidas (vía getter público, D-F8.3-C-6).
        Set<es.yaroki.educhronos.solver.domain.Plaza> plazasConAula = recon.aulasElegidas().values()
                .stream().flatMap(m -> m.keySet().stream()).collect(Collectors.toSet());
        es.yaroki.educhronos.solver.domain.Plaza plazaFija =
                plazaDominio(problemaRecon, "MAT-1ESO-P1");

        // (2) la plaza de aula FIJA no aparece en aulasElegidas().
        assertThat(plazasConAula).doesNotContain(plazaFija);
        // (3) el mapa de aulas elegidas coincide con el del original: la plaza de
        // CANDIDATAS está, con el mismo aula. Comparación DIRECTA contra
        // solucionOrig.aulasElegidas() (sin pasar por aulaElegida()): más discriminante.
        assertThat(recon.aulasElegidas()).isEqualTo(solucionOrig.aulasElegidas());
    }

    // ---------------------------------------------------- Test 3: guarda de corrupción

    @Test
    void aSolucionHorarioAbortaSiElAulaDeUnaPlazaFijaNoCoincideConSuAulaFija() {
        // Catálogo con una plaza de aula FIJA (A1) y un aula distinta (A2).
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor prof = profesorRepository.save(new Profesor("MAT1", "Prof MAT1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));
        TramoSemanal l1 = tramoRepository.save(new TramoSemanal(
                Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));

        Actividad act = new Actividad();
        act.setCodigo("MAT-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("MAT-1ESO-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(prof));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);

        Plaza plazaJpa = actividadRepository.findByCodigo("MAT-1ESO").orElseThrow()
                .getPlazas().stream()
                .filter(p -> p.getCodigo().equals("MAT-1ESO-P1")).findFirst().orElseThrow();

        // Sesion CORRUPTA: aula A2, que NO es la aulaFija (A1) de la plaza. Se inyecta
        // saltándose la vía normal (el solver nunca produciría esto).
        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "corrupto", Instant.now(), "OPTIMAL", 0.0, 0.0));
        Sesion corrupta = sesionRepository.save(new Sesion(horario, plazaJpa, 1, l1, a2));
        entityManager.flush();

        ProblemaHorario problema = service.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo =
                SolucionMapper.indiceTramos(problema, tramoRepository.findAll());

        assertThatThrownBy(() ->
                SolucionMapper.aSolucionHorario(problema, List.of(corrupta), idxTramo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con su aulaFija");
    }

    // ----------------------------------------------------------------- Test 4: endpoint

    @Test
    void endpointDiagnosticoDevuelveViolacionesVaciasPenalizacionesYTotalesCoherentes()
            throws Exception {
        poblarConIndisponibilidadBlandaForzada();
        entityManager.flush();

        Long id = service.generar(10, 42, null, "diag-endpoint").getId();
        entityManager.flush();
        entityManager.clear();

        MvcResult res = mockMvc.perform(get("/api/horarios/" + id + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones").isEmpty())
                .andExpect(jsonPath("$.penalizaciones").isNotEmpty())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        int ventanasDto = JsonPath.read(json, "$.totales.ventanas");

        // Cross-check independiente: totales.ventanas == suma del mapa del gemelo
        // contarVentanasProfesor sobre la solución reconstruida.
        List<Sesion> sesiones = service.cargarHorario(id).getSesiones();
        ProblemaHorario problema = service.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo =
                SolucionMapper.indiceTramos(problema, tramoRepository.findAll());
        SolucionHorario sol = SolucionMapper.aSolucionHorario(problema, sesiones, idxTramo);
        int ventanasEsperado = new VerificadorSolucion()
                .contarVentanasProfesor(problema, sol).values().stream()
                .mapToInt(Integer::intValue).sum();
        assertThat(ventanasDto).isEqualTo(ventanasEsperado);
    }

    @Test
    void endpointDiagnosticoDevuelve404SiElHorarioNoExiste() throws Exception {
        mockMvc.perform(get("/api/horarios/999999/diagnostico"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ fixtures

    /** Una actividad de aula FIJA (MAT) y otra de aulas CANDIDATAS (LEN), un subgrupo
     *  común (se excluyen en el mismo tramo, S9) y 5 tramos: factible. */
    private void poblarFijaYCandidata() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Asignatura len = asignaturaRepository.save(new Asignatura("LEN", "Lengua"));
        Profesor pmat = profesorRepository.save(new Profesor("MAT1", "Prof MAT1"));
        Profesor plen = profesorRepository.save(new Profesor("LEN1", "Prof LEN1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));
        Aula a3 = aulaRepository.save(new Aula("A3", TipoAula.ORDINARIA, null, null, null, null));

        for (int orden = 1; orden <= 5; orden++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
        }

        Actividad matAct = new Actividad();
        matAct.setCodigo("MAT-1ESO");
        matAct.setRepeticionesPorSemana(1);
        matAct.setDuracionTramos(1);
        matAct.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza pm = new Plaza();
        pm.setCodigo("MAT-1ESO-P1");
        pm.setActividad(matAct);
        pm.setAsignatura(mat);
        pm.setProfesores(Set.of(pmat));
        pm.setAulaFija(a1);
        pm.setSubgrupos(Set.of(sg));
        matAct.getPlazas().add(pm);
        actividadRepository.save(matAct);

        Actividad lenAct = new Actividad();
        lenAct.setCodigo("LEN-1ESO");
        lenAct.setRepeticionesPorSemana(1);
        lenAct.setDuracionTramos(1);
        lenAct.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza pl = new Plaza();
        pl.setCodigo("LEN-1ESO-P1");
        pl.setActividad(lenAct);
        pl.setAsignatura(len);
        pl.setProfesores(Set.of(plen));
        pl.setAulasCandidatas(Set.of(a2, a3));
        pl.setSubgrupos(Set.of(sg));
        lenAct.getPlazas().add(pl);
        actividadRepository.save(lenAct);
    }

    /** Una actividad de una plaza (aula fija), 5 tramos, una restricción BLANDA del
     *  profesor sobre L1 y el pin de la instancia a L1: fuerza un incumplimiento blando
     *  inevitable (penalización garantizada, sin violar ninguna dura). */
    private void poblarConIndisponibilidadBlandaForzada() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor prof = profesorRepository.save(new Profesor("MAT1", "Prof MAT1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        TramoSemanal l1 = null;
        for (int orden = 1; orden <= 5; orden++) {
            TramoSemanal t = tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
            if (orden == 1) {
                l1 = t;
            }
        }
        // El profesor prefiere NO impartir en L1 (BLANDA); el pin lo obliga igualmente.
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                prof, l1, TipoRestriccion.BLANDA, 1, "prefiere no L1"));

        Actividad act = new Actividad();
        act.setCodigo("MAT-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("MAT-1ESO-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(prof));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);

        sesionBloqueadaRepository.save(new SesionBloqueada(act, 1, l1));
    }

    // ------------------------------------------------------------------ helpers

    private static es.yaroki.educhronos.solver.domain.Plaza plazaDominio(
            ProblemaHorario problema, String codigo) {
        return problema.actividades().stream()
                .flatMap(a -> a.plazas().stream())
                .filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow();
    }
}
