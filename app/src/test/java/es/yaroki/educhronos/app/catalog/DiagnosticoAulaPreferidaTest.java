package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.dto.TotalesDTO;
import es.yaroki.educhronos.solver.cpsat.ReglaBlanda;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
 * Total de clases fuera de su aula preferida en el diagnóstico (S208, C-preferencias-aulas, A6).
 * Mismo arnés que {@code DiagnosticoRoundTripTest}: SQLite real, transacción única del
 * {@code @DataJpaTest} y el controlador montado a mano.
 *
 * <p>Catálogo: grupos G y H con aulas de grupo A y B; la asignatura MAT tiene L como PREFERIDA.
 * X (de G, profesor PX) e Y (de H, profesor PY) son clases sin aula escrita: sus aulas posibles
 * son {A, L} y {B, L}, y su preferida efectiva, L.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class})
class DiagnosticoAulaPreferidaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private DiagnosticoService diagnosticoService;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AsignaturaAulaRepository asignaturaAulaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private SesionBloqueadaRepository sesionBloqueadaRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HorarioController(service, diagnosticoService,
                        mock(ExportacionHorarioService.class)))
                .build();
    }

    /** Lo que deja poblado {@link #poblar}: lo que cada test necesita a mano. */
    private record Catalogo(Actividad x, Actividad y, Actividad z, Aula a, Aula l,
                            List<TramoSemanal> tramos) {
    }

    /**
     * (k) Horario guardado a mano: X fuera de su preferida (en A, su aula de grupo) e Y dentro
     * (en L). Con X de duración 1 el total nuevo vale 1.
     */
    @Test
    void k_horarioGuardado_claseFueraDeDuracionUno_elTotalNuevoValeUno() {
        assertThat(totalesDeHorarioGuardado(1).aulaNoPreferida()).isEqualTo(1);
    }

    /** (k) Como el anterior con X de duración 2: el total nuevo vale 2. */
    @Test
    void k_horarioGuardado_claseFueraDeDuracionDos_elTotalNuevoValeDos() {
        assertThat(totalesDeHorarioGuardado(2).aulaNoPreferida()).isEqualTo(2);
    }

    private TotalesDTO totalesDeHorarioGuardado(int duracionX) {
        Catalogo c = poblar(duracionX, false);
        HorarioGenerado horario = horarioRepository.save(new HorarioGenerado(
                "k-" + duracionX, Instant.now(), "OPTIMAL", 0.0, 0.0));
        sesionRepository.save(new Sesion(horario, c.x().getPlazas().get(0), 1, c.tramos().get(0), c.a()));
        sesionRepository.save(new Sesion(horario, c.y().getPlazas().get(0), 1, c.tramos().get(0), c.l()));
        entityManager.flush();
        entityManager.clear();
        return diagnosticoService.diagnosticar(horario.getId()).totales();
    }

    /**
     * (l) Recomposición en producción. X e Y pinadas en L1 compiten por L, así que una queda
     * fuera de su preferida (1). Z, de PX y con aula escrita, pinada en L3, deja a PX una ventana
     * en L2 (1). Tras generar, la suma de los cuatro totales del JSON del diagnóstico es el
     * objetivo de CP-SAT guardado, y los dos términos no nulos están en ella.
     */
    @Test
    void l_trasGenerar_laSumaDeLosTotalesEsElObjetivoDeCpSat() throws Exception {
        poblar(1, true);
        entityManager.flush();

        HorarioGenerado horario = service.generar(10, 42, null, "s208-l");
        entityManager.flush();
        entityManager.clear();

        String json = diagnosticoJson(horario.getId());
        int ventanas = JsonPath.read(json, "$.totales.ventanas");
        int consecutivas = JsonPath.read(json, "$.totales.consecutivas");
        int indispBlanda = JsonPath.read(json, "$.totales.indispBlanda");
        int aulaNoPreferida = JsonPath.read(json, "$.totales.aulaNoPreferida");

        assertThat(aulaNoPreferida).as("aula no preferida").isEqualTo(1);
        assertThat(ventanas).as("ventanas").isEqualTo(1);
        assertThat((double) (ventanas + consecutivas + indispBlanda + aulaNoPreferida))
                .as("suma de los totales frente al objetivo de CP-SAT")
                .isEqualTo(horario.getObjetivo());
    }

    /**
     * (m) Ninguna penalización por celda lleva la regla nueva (C5 de T1), aunque el horario tenga
     * una clase fuera de su preferida y otras penalizaciones (la ventana de PX), así que la
     * lista no está vacía por falta de datos.
     */
    @Test
    void m_ningunaPenalizacionPorCeldaLlevaLaReglaNueva() throws Exception {
        poblar(1, true);
        entityManager.flush();

        HorarioGenerado horario = service.generar(10, 42, null, "s208-m");
        entityManager.flush();
        entityManager.clear();

        String json = diagnosticoJson(horario.getId());
        List<String> reglas = JsonPath.read(json, "$.penalizaciones[*].regla");
        int aulaNoPreferida = JsonPath.read(json, "$.totales.aulaNoPreferida");

        assertThat(aulaNoPreferida).isEqualTo(1);
        assertThat(reglas).isNotEmpty().doesNotContain(ReglaBlanda.AULA_NO_PREFERIDA.name());
    }

    // ------------------------------------------------------------------ fixtures

    private String diagnosticoJson(Long id) throws Exception {
        return mockMvc.perform(get("/api/horarios/" + id + "/diagnostico"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Puebla el catálogo. {@code duracionX} es la de X (Y dura 1). Con {@code pines}, X e Y se
     * pinan en L1 y se añade Z (PX, grupo K, aula escrita C) pinada en L3.
     */
    private Catalogo poblar(int duracionX, boolean pines) {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        Aula a = aulaRepository.save(new Aula("A", TipoAula.ORDINARIA, null, null, null, null));
        Aula b = aulaRepository.save(new Aula("B", TipoAula.ORDINARIA, null, null, null, null));
        Aula l = aulaRepository.save(new Aula("L", TipoAula.ORDINARIA, null, null, null, null));
        Aula cAula = aulaRepository.save(new Aula("C", TipoAula.ORDINARIA, null, null, null, null));
        GrupoAdministrativo g = new GrupoAdministrativo("G", eso1, TipoGrupo.ORDINARIO, null);
        g.setAulaReferencia(a);
        g = grupoRepository.save(g);
        GrupoAdministrativo h = new GrupoAdministrativo("H", eso1, TipoGrupo.ORDINARIO, null);
        h.setAulaReferencia(b);
        h = grupoRepository.save(h);
        GrupoAdministrativo k = grupoRepository.save(
                new GrupoAdministrativo("K", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("G-1", Set.of(g)));
        Subgrupo sh = subgrupoRepository.save(new Subgrupo("H-1", Set.of(h)));
        Subgrupo sk = subgrupoRepository.save(new Subgrupo("K-1", Set.of(k)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matemáticas"));
        asignaturaAulaRepository.save(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA));
        Profesor px = profesorRepository.save(new Profesor("PX", "Equis"));
        Profesor py = profesorRepository.save(new Profesor("PY", "Ye"));

        List<TramoSemanal> tramos = new ArrayList<>();
        for (int orden = 1; orden <= 3; orden++) {
            tramos.add(tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null)));
        }

        Actividad x = actividadRepository.save(clase("X", duracionX, mat, px, sg, null));
        Actividad y = actividadRepository.save(clase("Y", 1, mat, py, sh, null));
        Actividad z = null;
        if (pines) {
            z = actividadRepository.save(clase("Z", 1, mat, px, sk, cAula));
            sesionBloqueadaRepository.save(new SesionBloqueada(x, 1, tramos.get(0)));
            sesionBloqueadaRepository.save(new SesionBloqueada(y, 1, tramos.get(0)));
            sesionBloqueadaRepository.save(new SesionBloqueada(z, 1, tramos.get(2)));
        }
        return new Catalogo(x, y, z, a, l, tramos);
    }

    /** Clase de una plaza; sin {@code aulaFija} su aula la deduce la aplicación. */
    private static Actividad clase(String codigo, int duracion, Asignatura asig, Profesor prof,
                                   Subgrupo sg, Aula aulaFija) {
        Actividad act = new Actividad();
        act.setCodigo(codigo);
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(duracion);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo + "-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(asig);
        plaza.setProfesores(Set.of(prof));
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        return act;
    }
}
