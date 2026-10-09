package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.MovimientoInstanciaService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.MovimientoInstanciaController;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Un horario guardado con unas reglas de aulas y leído con otras (S207, C-deduccion-aulas, D).
 * Hasta S207 la reconstrucción abortaba y el diagnóstico salía 404, y mover o intercambiar,
 * 500; ningún test lo fijaba. Ahora el aula guardada se conserva, el diagnóstico la marca
 * {@code AULA_FUERA_DE_REGLAS} y mover e intercambiar rechazan solo violaciones NUEVAS.
 *
 * <p>Molde de {@code MovimientoInstanciaEndpointTest}: sesiones persistidas a mano, sin solver.
 * Fixture: MAT es una clase de 1ºA SIN aula escrita, así que su aula sale del aula de grupo de
 * 1ºA (A-R); se guarda en A-R y después la regla cambia (el aula de 1ºA pasa a A-R2). LEN es de
 * 1ºA (otro subgrupo), EF de 1ºB y HIS de 1ºC con el mismo profesor que MAT. Malla: LUNES y
 * MARTES, 3 tramos cada uno. MAT en L1, LEN en L2, EF y HIS en L3.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class,
        MovimientoInstanciaService.class})
class HorarioConAulaFueraDeReglasTest {

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
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;

    private MockMvc mockMvc;
    private Long horarioId;

    /**
     * Centro sin guardias (S214, D1): desde C-ajuste-guardias el ajuste reparte las guardias, y con el
     * mínimo 4 por defecto y ningún profesor de guardia todo ajuste aceptado se rechazaría.
     */
    @BeforeEach
    void centroSinGuardias(@Autowired ConfiguracionRepository configuraciones) {
        MinimoGuardias.fijar(configuraciones, 0);
    }

    @BeforeEach
    void montar() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new MovimientoInstanciaController(movimientoService),
                        new HorarioController(generadorService, diagnosticoService,
                                mock(ExportacionHorarioService.class)))
                .build();
    }

    // ----------------------------------------------------------------- diagnóstico

    @Test
    void premisa_conLaReglaDeSiempreElHorarioNoTieneViolaciones() throws Exception {
        poblar();

        mockMvc.perform(get("/api/horarios/" + horarioId + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones.length()").value(0));
    }

    @Test
    void diagnostico_reglaCambiada_200ConAulaFueraDeReglasEnEsaPlaza() throws Exception {
        poblar();
        cambiarElAulaDeGrupoDe1A();

        mockMvc.perform(get("/api/horarios/" + horarioId + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("AULA_FUERA_DE_REGLAS"))
                .andExpect(jsonPath("$.violaciones[0].recursoCodigo").value("A-R"))
                .andExpect(jsonPath("$.violaciones[0].celdas[0].actividadCodigo").value("MAT"))
                .andExpect(jsonPath("$.violaciones[0].celdas[0].plazaCodigo").value("MAT-P1"));
    }

    /**
     * S207 T2b: el único aula posible de MAT (A-R, la de su grupo) se marca «No se usa» con MAT ya
     * colocada en ella. En el problema MAT queda sin ningún aula, como una reunión, pero su sesión
     * guardada sigue en A-R: el diagnóstico la marca igualmente.
     */
    @Test
    void diagnostico_unicaAulaPosibleMarcadaNoSeUsa_200ConAulaFueraDeReglasEnEsaPlaza() throws Exception {
        poblar();
        aulaRepository.findByCodigo("A-R").orElseThrow().setEnUso(false);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/horarios/" + horarioId + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("AULA_FUERA_DE_REGLAS"))
                .andExpect(jsonPath("$.violaciones[0].recursoCodigo").value("A-R"))
                .andExpect(jsonPath("$.violaciones[0].celdas[0].plazaCodigo").value("MAT-P1"));
    }

    /** La rama de candidatas: LEN se guardó en una candidata que ya no lo es. */
    @Test
    void diagnostico_candidatasCambiadas_200ConLaViolacionEnEsaPlaza() throws Exception {
        poblar();
        Plaza len = plazaDe("LEN");
        Aula otra1 = aulaRepository.save(aula("A-O1"));
        Aula otra2 = aulaRepository.save(aula("A-O2"));
        len.setAulaFija(null);
        len.setAulasCandidatas(Set.of(otra1, otra2));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/horarios/" + horarioId + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("AULA_FUERA_DE_REGLAS"))
                .andExpect(jsonPath("$.violaciones[0].recursoCodigo").value("A-LEN"))
                .andExpect(jsonPath("$.violaciones[0].celdas[0].plazaCodigo").value("LEN-P1"));
    }

    // ---------------------------------------------------------------------- mover

    /**
     * Mover MAT a un tramo libre: la violación preexistente no cuenta, aunque lleve el tramo y
     * el tramo cambie. El aula guardada se conserva.
     */
    @Test
    void mover_laInstanciaAfectadaAUnTramoLibre_200YConservaSuAula() throws Exception {
        poblar();
        cambiarElAulaDeGrupoDe1A();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMover("MAT", 1, 2, 2)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(sesionRepository.findParaInstancia(horarioId, "MAT", 1)).singleElement()
                .satisfies(s -> {
                    assertThat(s.getTramoInicio().getDia()).isEqualTo(Dia.MARTES);
                    assertThat(s.getAula().getCodigo()).isEqualTo("A-R");
                });
    }

    /**
     * Capacidad que cambia después de guardar: el aula de LEN pasa a tener 5 plazas y su subgrupo
     * 10 alumnos. El diagnóstico marca {@code CAPACIDAD_AULA}, que lleva el tramo pero no depende
     * de él: mover LEN a un tramo libre se acepta. (Mata el mutante D-4 de S207 T2.) Desde T2b
     * marca además {@code AULA_FUERA_DE_REGLAS}: sin sitio en su única aula, LEN se queda sin
     * aulas posibles y la sesión guardada sigue en A-LEN.
     */
    @Test
    void mover_conUnaCapacidadAulaPreexistente_200() throws Exception {
        poblar();
        Aula aLen = aulaRepository.findByCodigo("A-LEN").orElseThrow();
        aLen.actualizar(aLen.getCodigo(), aLen.getTipo(), 5, null, null, null);
        subgrupoRepository.findByCodigo("1ºA-s2").orElseThrow().setAlumnos(10);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/horarios/" + horarioId + "/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violaciones.length()").value(2))
                .andExpect(jsonPath("$.violaciones[*].regla",
                        containsInAnyOrder("CAPACIDAD_AULA", "AULA_FUERA_DE_REGLAS")))
                .andExpect(jsonPath("$.violaciones[*].recursoCodigo", everyItem(is("A-LEN"))));

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMover("LEN", 1, 2, 2)))
                .andExpect(status().isOk());
    }

    /** Mover MAT al tramo de LEN (mismo grupo): violación NUEVA, 409, y solo esa. */
    @Test
    void mover_creandoUnaViolacionNueva_409ConSoloLaNueva() throws Exception {
        poblar();
        cambiarElAulaDeGrupoDe1A();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoMover("MAT", 1, 1, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"))
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("SOLAPE_GRUPO"));
    }

    // ---------------------------------------------------------------- intercambiar

    @Test
    void intercambiar_laInstanciaAfectadaSinViolacionesNuevas_200() throws Exception {
        poblar();
        cambiarElAulaDeGrupoDe1A();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias/intercambio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoIntercambio("MAT", 1, "LEN", 1)))
                .andExpect(status().isOk());
    }

    /** MAT pasa a L3, donde HIS tiene su mismo profesor: SOLAPE_PROFESOR nuevo, 409. */
    @Test
    void intercambiar_creandoUnaViolacionNueva_409ConSoloLaNueva() throws Exception {
        poblar();
        cambiarElAulaDeGrupoDe1A();

        mockMvc.perform(put("/api/horarios/" + horarioId + "/instancias/intercambio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoIntercambio("MAT", 1, "EF", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"))
                .andExpect(jsonPath("$.violaciones.length()").value(1))
                .andExpect(jsonPath("$.violaciones[0].regla").value("SOLAPE_PROFESOR"));
    }

    // -------------------------------------------------------------------- fixture

    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        Aula aR = aulaRepository.save(aula("A-R"));
        aulaRepository.save(aula("A-R2"));
        Aula aLen = aulaRepository.save(aula("A-LEN"));
        Aula aEf = aulaRepository.save(aula("A-EF"));
        Aula aHis = aulaRepository.save(aula("A-HIS"));

        GrupoAdministrativo g1a = new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null);
        g1a.setAulaReferencia(aR);
        g1a = grupoRepository.save(g1a);
        GrupoAdministrativo g1b = grupoRepository.save(
                new GrupoAdministrativo("1ºB", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo g1c = grupoRepository.save(
                new GrupoAdministrativo("1ºC", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sgA1 = subgrupoRepository.save(new Subgrupo("1ºA-s1", Set.of(g1a)));
        Subgrupo sgA2 = subgrupoRepository.save(new Subgrupo("1ºA-s2", Set.of(g1a)));
        Subgrupo sgB = subgrupoRepository.save(new Subgrupo("1ºB-s1", Set.of(g1b)));
        Subgrupo sgC = subgrupoRepository.save(new Subgrupo("1ºC-s1", Set.of(g1c)));

        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Asignatura len = asignaturaRepository.save(new Asignatura("LEN", "Lengua"));
        Asignatura ef = asignaturaRepository.save(new Asignatura("EF", "Educacion Fisica"));
        Asignatura his = asignaturaRepository.save(new Asignatura("HIS", "Historia"));
        Profesor p1 = profesorRepository.save(new Profesor("P1", "Uno"));
        Profesor p2 = profesorRepository.save(new Profesor("P2", "Dos"));
        Profesor p3 = profesorRepository.save(new Profesor("P3", "Tres"));

        for (int i = 1; i <= 3; i++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(8 + i, 0), LocalTime.of(9 + i, 0), true, i, null));
        }
        for (int i = 1; i <= 3; i++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.MARTES, LocalTime.of(8 + i, 0), LocalTime.of(9 + i, 0), true, 3 + i, null));
        }

        Actividad actMat = actividad("MAT", mat, p1, null, sgA1);   // sin aula escrita: A-R
        Actividad actLen = actividad("LEN", len, p2, aLen, sgA2);
        Actividad actEf = actividad("EF", ef, p3, aEf, sgB);
        Actividad actHis = actividad("HIS", his, p1, aHis, sgC);
        entityManager.flush();

        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "Horario de S207", Instant.parse("2026-10-07T08:00:00Z"), "OPTIMAL", 0.0, 0.0));
        colocar(horario, actMat, tramoDe(Dia.LUNES, 1), aR);
        colocar(horario, actLen, tramoDe(Dia.LUNES, 2), aLen);
        colocar(horario, actEf, tramoDe(Dia.LUNES, 3), aEf);
        colocar(horario, actHis, tramoDe(Dia.LUNES, 3), aHis);
        horarioId = horario.getId();
        entityManager.flush();
        entityManager.clear();
    }

    /** La regla cambia: el aula de grupo de 1ºA pasa de A-R a A-R2. MAT sigue guardada en A-R. */
    private void cambiarElAulaDeGrupoDe1A() {
        GrupoAdministrativo g1a = grupoRepository.findByCodigo("1ºA").orElseThrow();
        g1a.setAulaReferencia(aulaRepository.findByCodigo("A-R2").orElseThrow());
        entityManager.flush();
        entityManager.clear();
    }

    private Plaza plazaDe(String actividadCodigo) {
        return actividadRepository.findByCodigo(actividadCodigo).orElseThrow().getPlazas().get(0);
    }

    private static Aula aula(String codigo) {
        return new Aula(codigo, TipoAula.ORDINARIA, null, null, null, null);
    }

    private Actividad actividad(String codigo, Asignatura asignatura, Profesor profesor,
                                Aula aulaFija, Subgrupo subgrupo) {
        Actividad act = new Actividad(codigo, asignatura, 1, 1, PatronTemporal.NEUTRA, false);
        act.agregarPlaza(codigo + "-P1", asignatura, aulaFija, Set.of(profesor), Set.of(), Set.of(subgrupo));
        return actividadRepository.save(act);
    }

    private void colocar(HorarioGenerado horario, Actividad actividad, TramoSemanal tramo, Aula aula) {
        sesionRepository.save(new Sesion(horario, actividad.getPlazas().get(0), 1, tramo, aula));
    }

    private TramoSemanal tramoDe(Dia dia, int ordenEnDia) {
        int ordenGlobal = (dia == Dia.LUNES ? 0 : 3) + ordenEnDia;
        return tramoRepository.findAll().stream()
                .filter(t -> t.getDia() == dia && t.getOrden() == ordenGlobal)
                .findFirst().orElseThrow();
    }

    private static String cuerpoMover(String actividadCodigo, int indice, int dia, int orden) {
        return "{\"actividadCodigo\":\"" + actividadCodigo + "\",\"indice\":" + indice
                + ",\"dia\":" + dia + ",\"orden\":" + orden + "}";
    }

    private static String cuerpoIntercambio(String actA, int indiceA, String actB, int indiceB) {
        return "{\"primera\":{\"actividadCodigo\":\"" + actA + "\",\"indice\":" + indiceA + "},"
                + "\"segunda\":{\"actividadCodigo\":\"" + actB + "\",\"indice\":" + indiceB + "}}";
    }
}
