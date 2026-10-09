package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.MovimientoInstanciaService;
import es.yaroki.educhronos.app.web.MovimientoInstanciaController;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Las guardias tras un ajuste a mano (S214, C-ajuste-guardias, condición 5 de O-guardias): mover o
 * intercambiar una instancia recalcula el reparto entero sobre la solución resultante, o rechaza el
 * ajuste con {@code GUARDIAS_SIN_REPARTO} sin escribir nada.
 *
 * <p><b>Base X</b> (casos a, b, c, e): un lunes de tres tramos; A (de PA) en L1 y B (de PB) en L2, del
 * mismo subgrupo; PG sin clases. Una guardia cada uno y mínimo 1. <b>Base Y</b> (caso d): un lunes de
 * dos tramos; A (PA) y C (PC) en L1, B (PB) en L2, en grupos distintos; guardias 1, 1 y 0.
 *
 * <p>Todo se lee de la tabla tras un {@code clear()}, como en una petición real: además, las guardias
 * del montaje no pueden quedar en la sesión de Hibernate, porque el ajuste las borra de golpe y SQLite
 * puede dar los mismos id a las nuevas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class, MovimientoInstanciaService.class})
class AjusteGuardiasTest {

    private static final String MENSAJE = "No se puede hacer este cambio: después de él no hay forma de repartir"
            + " las guardias. Tramos que no llegan al mínimo de profesores de guardia: ";

    @Autowired private EntityManager entityManager;
    @Autowired private DiagnosticoService diagnosticoService;
    @Autowired private MovimientoInstanciaService movimientoService;

    @Autowired private ConfiguracionRepository configuracionRepository;
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
    @Autowired private GuardiaRepository guardiaRepository;

    private MockMvc mockMvc;
    private Long horarioId;
    private final Map<Integer, TramoSemanal> tramos = new HashMap<>();
    private final Map<String, Profesor> profesores = new HashMap<>();

    @BeforeEach
    void montar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MovimientoInstanciaController(movimientoService)).build();
        MinimoGuardias.fijar(configuracionRepository, 1);
    }

    // ------------------------------------------------------------------ (a) intercambio

    /** (a) Intercambio válido que rompería una guardia: 200, y las guardias quedan bien repartidas. */
    @Test
    void intercambioValidoQueRompeUnaGuardia_recalculaYElDiagnosticoNoDaViolacionesDeGuardia() throws Exception {
        poblarX(List.of("PA@2", "PB@1", "PG@3"));
        // Precondición: PA, profesor de A, tiene guardia en el tramo de B. Sin recálculo, tras el
        // intercambio A pasaría a ese tramo y la guardia de PA quedaría debajo de su clase.
        assertThat(guardias()).contains("PA@" + ordenDe("B"));
        assertThat(diagnostico().violacionesGuardia()).as("precondición: guardias bien antes").isEmpty();

        mockMvc.perform(put(urlIntercambio()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("A", "B")))
                .andExpect(status().isOk());

        assertThat(guardias()).hasSize(3).doesNotContain("PA@2", "PB@1");
        DiagnosticoDTO diagnostico = diagnostico();
        assertThat(diagnostico.violaciones()).isEmpty();
        assertThat(diagnostico.violacionesGuardia()).isEmpty();
    }

    // ------------------------------------------------------------------ (b) mover

    /** (b) Mover a un tramo donde el profesor tiene guardia: 200, y las guardias quedan bien repartidas. */
    @Test
    void moverAUnTramoConGuardiaDelProfesor_recalculaYElDiagnosticoNoDaViolacionesDeGuardia() throws Exception {
        poblarX(List.of("PA@3", "PB@1", "PG@2"));
        // Precondición: PA tiene guardia en L3, el destino de A. Sin recálculo quedaría bajo su clase.
        assertThat(guardias()).contains("PA@3");
        assertThat(diagnostico().violacionesGuardia()).as("precondición: guardias bien antes").isEmpty();

        mockMvc.perform(put(urlMover()).contentType(MediaType.APPLICATION_JSON).content(cuerpoMover("A", 3)))
                .andExpect(status().isOk());

        assertThat(ordenDe("A")).isEqualTo(3);
        assertThat(guardias()).hasSize(3).doesNotContain("PA@3");
        DiagnosticoDTO diagnostico = diagnostico();
        assertThat(diagnostico.violaciones()).isEmpty();
        assertThat(diagnostico.violacionesGuardia()).isEmpty();
    }

    // ------------------------------------------------------------------ (c) rechazo por clases

    /**
     * (c) Un rechazo por clases no toca las guardias: mismas filas, con sus id. Un recálculo sobre la
     * candidata (A y B juntas en L2) dejaría a PG como único libre en L2 y cambiaría por fuerza esa
     * guardia, que aquí es de PA: un recálculo indebido se vería.
     */
    @Test
    void rechazoPorClases_noTocaLasGuardias() throws Exception {
        poblarX(List.of("PA@2", "PB@1", "PG@3"));
        List<String> antes = guardiasConId();

        mockMvc.perform(put(urlMover()).contentType(MediaType.APPLICATION_JSON).content(cuerpoMover("A", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("VIOLA_REGLA_DURA"));

        assertThat(guardiasConId()).isEqualTo(antes);
    }

    // ------------------------------------------------------------------ (d) sin reparto

    /**
     * (d) Mover B a L1 deja sin reparto: 409 {@code GUARDIAS_SIN_REPARTO} con el tramo en el mensaje, y
     * sesiones y guardias como estaban.
     */
    @Test
    void moverSinRepartoPosible_rechazaConElTramoYNoEscribeNada() throws Exception {
        poblarY();
        afirmarRepartoImposibleConByAEnL1();
        List<String> sesionesAntes = sesiones();
        List<String> guardiasAntes = guardiasConId();

        mockMvc.perform(put(urlMover()).contentType(MediaType.APPLICATION_JSON).content(cuerpoMover("B", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("GUARDIAS_SIN_REPARTO"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE + "tramo L1 (día 1, tramo 1): 0 de 1"))
                .andExpect(jsonPath("$.violaciones.length()").value(0));

        assertThat(sesiones()).isEqualTo(sesionesAntes);
        assertThat(guardiasConId()).isEqualTo(guardiasAntes);
    }

    /** (d) Lo mismo por el intercambio: B y C cambian de tramo y B acaba en L1. */
    @Test
    void intercambioSinRepartoPosible_rechazaConElTramoYNoEscribeNada() throws Exception {
        poblarY();
        afirmarRepartoImposibleConByAEnL1();
        List<String> sesionesAntes = sesiones();
        List<String> guardiasAntes = guardiasConId();

        mockMvc.perform(put(urlIntercambio()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("B", "C")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("GUARDIAS_SIN_REPARTO"))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE + "tramo L1 (día 1, tramo 1): 0 de 1"))
                .andExpect(jsonPath("$.violaciones.length()").value(0));

        assertThat(sesiones()).isEqualTo(sesionesAntes);
        assertThat(guardiasConId()).isEqualTo(guardiasAntes);
    }

    // ------------------------------------------------------------------ (e) horario sin guardias

    /** (e) Un horario sin filas de guardia recibe las suyas en el primer ajuste aceptado. */
    @Test
    void horarioSinGuardias_lasRecibeEnElPrimerAjusteAceptado() throws Exception {
        poblarX(List.of());
        assertThat(guardias()).as("precondición: sin filas de guardia").isEmpty();
        assertThat(diagnostico().violacionesGuardia()).as("precondición: el diagnóstico lo ve").isNotEmpty();

        mockMvc.perform(put(urlIntercambio()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("A", "B")))
                .andExpect(status().isOk());

        assertThat(guardias()).hasSize(3)
                .extracting(g -> g.substring(0, g.indexOf('@')))
                .containsExactlyInAnyOrder("PA", "PB", "PG");
        assertThat(diagnostico().violacionesGuardia()).isEmpty();
    }

    // ------------------------------------------------------------------ montaje

    /**
     * Precondición de (d): los únicos profesores con guardias son PA y PB, y con B en L1 los dos dan clase
     * ahí (A ya está en L1). L1 se queda sin nadie libre con guardias y el mínimo es 1: no hay reparto,
     * sea cual sea el algoritmo.
     */
    private void afirmarRepartoImposibleConByAEnL1() {
        assertThat(profesorRepository.findAll().stream().filter(p -> p.getGuardiasOrdinarias() > 0)
                .map(Profesor::getCodigo)).containsExactlyInAnyOrder("PA", "PB");
        assertThat(ordenDe("A")).isEqualTo(1);
        assertThat(ordenDe("C")).as("C ocupa L1: en el intercambio, B pasa a L1").isEqualTo(1);
    }

    /** Base X. {@code guardias}: «PROFESOR@orden» guardadas en el horario. */
    private void poblarX(List<String> guardias) {
        malla(3);
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-s", Set.of(grupo("1ºA"))));
        Profesor pa = profesor("PA", 1);
        Profesor pb = profesor("PB", 1);
        profesor("PG", 1);
        Actividad a = actividad("A", pa, sg);
        Actividad b = actividad("B", pb, sg);
        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "Horario de S214", Instant.parse("2026-10-09T08:00:00Z"), "OPTIMAL", 0.0, 0.0));
        colocar(horario, a, 1);
        colocar(horario, b, 2);
        guardar(horario, guardias);
    }

    /** Base Y, con sus guardias guardadas PB@1 y PA@2. */
    private void poblarY() {
        malla(2);
        Profesor pa = profesor("PA", 1);
        Profesor pb = profesor("PB", 1);
        Profesor pc = profesor("PC", 0);
        Actividad a = actividad("A", pa, subgrupoRepository.save(new Subgrupo("1ºA-s", Set.of(grupo("1ºA")))));
        Actividad b = actividad("B", pb, subgrupoRepository.save(new Subgrupo("1ºB-s", Set.of(grupo("1ºB")))));
        Actividad c = actividad("C", pc, subgrupoRepository.save(new Subgrupo("1ºC-s", Set.of(grupo("1ºC")))));
        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "Horario de S214", Instant.parse("2026-10-09T08:00:00Z"), "OPTIMAL", 0.0, 0.0));
        colocar(horario, a, 1);
        colocar(horario, b, 2);
        colocar(horario, c, 1);
        guardar(horario, List.of("PB@1", "PA@2"));
    }

    private void malla(int n) {
        for (int orden = 1; orden <= n; orden++) {
            tramos.put(orden, tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0), true, orden, null)));
        }
    }

    private GrupoAdministrativo grupo(String codigo) {
        Nivel nivel = nivelRepository.findAll().stream().findFirst()
                .orElseGet(() -> nivelRepository.save(new Nivel("1ESO", 1)));
        return grupoRepository.save(new GrupoAdministrativo(codigo, nivel, TipoGrupo.ORDINARIO, null));
    }

    private Profesor profesor(String codigo, int guardias) {
        Profesor profesor = new Profesor(codigo, "Profesor " + codigo);
        profesor.setGuardiasOrdinarias(guardias);
        profesores.put(codigo, profesorRepository.save(profesor));
        return profesores.get(codigo);
    }

    /** Una repetición, un tramo, aula fija propia: nada choca salvo profesor y subgrupo. */
    private Actividad actividad(String codigo, Profesor profesor, Subgrupo subgrupo) {
        Asignatura asignatura = asignaturaRepository.save(new Asignatura("ASIG-" + codigo, "Asignatura " + codigo));
        Aula aula = aulaRepository.save(new Aula("AULA-" + codigo, TipoAula.ORDINARIA, null, null, null, null));
        Actividad actividad = new Actividad();
        actividad.setCodigo(codigo);
        actividad.setRepeticionesPorSemana(1);
        actividad.setDuracionTramos(1);
        actividad.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo + "-P1");
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(Set.of(profesor));
        plaza.setAulaFija(aula);
        plaza.setSubgrupos(Set.of(subgrupo));
        actividad.getPlazas().add(plaza);
        return actividadRepository.save(actividad);
    }

    private void colocar(HorarioGenerado horario, Actividad actividad, int orden) {
        for (Plaza plaza : actividad.getPlazas()) {
            sesionRepository.save(new Sesion(horario, plaza, 1, tramos.get(orden), plaza.getAulaFija()));
        }
    }

    private void guardar(HorarioGenerado horario, List<String> guardias) {
        for (String g : guardias) {
            String[] partes = g.split("@");
            guardiaRepository.save(new Guardia(horario, profesores.get(partes[0]),
                    tramos.get(Integer.parseInt(partes[1]))));
        }
        horarioId = horario.getId();
        entityManager.flush();
        entityManager.clear();
    }

    /** «PROFESOR@orden» de las guardias del horario, leídas de la tabla y ordenadas. */
    private List<String> guardias() {
        return guardiaRepository.findByHorarioId(horarioId).stream()
                .map(g -> g.getProfesor().getCodigo() + "@" + g.getTramo().getOrden())
                .sorted().toList();
    }

    /** Lo mismo con el id de cada fila: «idénticas» incluye no haberlas borrado y vuelto a crear. */
    private List<String> guardiasConId() {
        return guardiaRepository.findByHorarioId(horarioId).stream()
                .map(g -> g.getId() + ":" + g.getProfesor().getCodigo() + "@" + g.getTramo().getOrden())
                .sorted().toList();
    }

    /** «actividad@orden» de las sesiones del horario, leídas de la tabla y ordenadas. */
    private List<String> sesiones() {
        return sesionRepository.findByHorarioId(horarioId).stream()
                .map(s -> s.getId() + ":" + s.getPlaza().getActividad().getCodigo() + "@"
                        + s.getTramoInicio().getOrden())
                .sorted().toList();
    }

    private int ordenDe(String actividad) {
        return sesionRepository.findParaInstancia(horarioId, actividad, 1).get(0).getTramoInicio().getOrden();
    }

    private DiagnosticoDTO diagnostico() {
        return diagnosticoService.diagnosticar(horarioId);
    }

    private String urlMover() {
        return "/api/horarios/" + horarioId + "/instancias";
    }

    private String urlIntercambio() {
        return "/api/horarios/" + horarioId + "/instancias/intercambio";
    }

    private static String cuerpo(String a, String b) {
        return "{\"primera\":{\"actividadCodigo\":\"" + a + "\",\"indice\":1},"
                + "\"segunda\":{\"actividadCodigo\":\"" + b + "\",\"indice\":1}}";
    }

    private static String cuerpoMover(String actividad, int orden) {
        return "{\"actividadCodigo\":\"" + actividad + "\",\"indice\":1,\"dia\":1,\"orden\":" + orden + "}";
    }
}
