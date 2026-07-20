package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.PrevalidacionFallidaException;
import es.yaroki.educhronos.app.service.PrevalidacionService;
import es.yaroki.educhronos.app.web.HorarioController;
import es.yaroki.educhronos.app.web.PrevalidacionController;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests HTTP de la pre-validación (Fase 8, Bloque 8.4-A): los DOS llamadores del mismo
 * núcleo, sobre el MISMO catálogo persistido.
 * <ul>
 *   <li>{@code GET /api/prevalidacion} → siempre {@code 200} con la lista completa.</li>
 *   <li>{@code POST /api/horarios} → {@code 422} si hay al menos un ERROR, y sigue
 *       adelante hasta el solver si solo hay AVISOs.</li>
 * </ul>
 *
 * <p>Vive en {@code app.catalog} —como {@code GenerarHorarioEndpointTest}— para construir
 * entidades {@code Actividad}/{@code Plaza} (ctor {@code protected}), y monta los
 * controladores reales con {@code standaloneSetup} sobre el servicio real.
 *
 * <p><b>Por qué el caso de solo-AVISO mockea el solver.</b> Un grupo sobrecargado es, con
 * el modelo actual, también infactible para el solver ({@code ModeloCpSat:1046} impone
 * no-solape POR GRUPO). Si se dejara resolver de verdad, el 422 llegaría igualmente pero
 * por la vía del solver, y el test no distinguiría "la pre-validación no abortó" de "la
 * pre-validación abortó". Interceptando la construcción de {@link SolverHorario} —mismo
 * patrón que {@code GenerarHorarioEndpointTest}— el 200 demuestra exactamente lo que se
 * quiere demostrar: que la petición ATRAVESÓ la pre-validación y llegó al solve.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, PrevalidacionService.class})
class PrevalidacionEndpointTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService generadorService;
    @Autowired private PrevalidacionService prevalidacionService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new HorarioController(generadorService,
                                new DiagnosticoService(generadorService, tramoRepository)),
                        new PrevalidacionController(prevalidacionService))
                .build();
    }

    /**
     * (A6) MISMA ENTRADA, DOS SALIDAS. Un catálogo con un ERROR de (d): la generación lo
     * rechaza con {@code 422} sin llegar al solver, y la consulta de pre-validación
     * devuelve {@code 200} con ese mismo hallazgo detallado. Prueba que los dos llamadores
     * comparten núcleo y difieren solo en qué hacen con el resultado.
     */
    @Test
    void mismoCatalogoConError_da422AlGenerarY200AlPrevalidar() throws Exception {
        poblarCatalogoConErrorDeRepeticiones();
        entityManager.flush();

        // La CAUSA importa: 422 de pre-validación, no del solver (que ni se construye).
        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxSegundos\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(resultado -> assertThat(resultado.getResolvedException())
                        .hasCauseInstanceOf(PrevalidacionFallidaException.class));

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.regla=='REPETICIONES_EXCEDEN_DIAS')].severidad")
                        .value(Matchers.hasItem("ERROR")))
                .andExpect(jsonPath("$[?(@.regla=='REPETICIONES_EXCEDEN_DIAS')].entidadCodigo")
                        .value(Matchers.hasItem("Mat-1ºA")));
    }

    /**
     * (A4) La sobrecarga de GRUPO es AVISO y NO impide generar: el POST atraviesa la
     * pre-validación y llega al solver (aquí interceptado, ver javadoc de clase), así que
     * responde {@code 200}. El GET confirma que el aviso está ahí y que su severidad es
     * {@code AVISO}, no {@code ERROR}: si (c) se elevara a ERROR, el POST daría 422 y este
     * test caería.
     */
    @Test
    void grupoSobrecargado_esAvisoYNoImpideGenerar() throws Exception {
        poblarCatalogoConGrupoSobrecargado();
        entityManager.flush();

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.regla=='GRUPO_SOBRECARGADO')].severidad")
                        .value(Matchers.hasItem("AVISO")))
                .andExpect(jsonPath("$[?(@.severidad=='ERROR')]").isEmpty());

        try (MockedConstruction<SolverHorario> mocked = Mockito.mockConstruction(
                SolverHorario.class,
                (mock, context) -> when(mock.resolverOptimizandoConDetalle(any()))
                        .thenAnswer(invocacion -> solucionCompletaDe(invocacion.getArgument(0))))) {

            mockMvc.perform(post("/api/horarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Coloca CADA instancia de CADA actividad en algún tramo, rotando por la rejilla. No
     * pretende ser un horario legal —solapa a propósito—: existe solo para que
     * {@code SolucionMapper.aSesiones} encuentre colocada toda instancia (lanza si alguna
     * falta, {@code SolucionMapper:119}) y el POST pueda completar con 200. El aula sale de
     * {@code plaza.aulaFija()} por el fallback de {@code SolucionHorario.aulaElegida}.
     */
    private static ResultadoOptimizacion solucionCompletaDe(ProblemaHorario problema) {
        Map<ActividadInstancia, Tramo> asignaciones = new HashMap<>();
        int siguiente = 0;
        for (es.yaroki.educhronos.solver.domain.Actividad act : problema.actividades()) {
            for (int indice = 1; indice <= act.repeticionesPorSemana(); indice++) {
                asignaciones.put(new ActividadInstancia(act, indice),
                        problema.tramos().get(siguiente++ % problema.tramos().size()));
            }
        }
        return new ResultadoOptimizacion(
                new SolucionHorario(asignaciones), CpSolverStatus.OPTIMAL, 0.0, 0.0);
    }

    /** Un catálogo sano pre-valida a {@code 200} con lista VACÍA. */
    @Test
    void catalogoSano_devuelve200ConListaVacia() throws Exception {
        poblarCatalogoSano();
        entityManager.flush();

        mockMvc.perform(get("/api/prevalidacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------- fixtures

    /**
     * 2 tramos lectivos en 2 días y una actividad DISTRIBUIDA de 3 repeticiones: (d)
     * dispara ERROR (3 > 2 días).
     */
    private void poblarCatalogoConErrorDeRepeticiones() {
        Contexto ctx = contextoBase(2);
        crearActividad("Mat-1ºA", 3, PatronTemporal.DISTRIBUIDA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
    }

    /**
     * 5 tramos lectivos y DOS actividades sobre subgrupos distintos del MISMO grupo, de 3
     * repeticiones cada una: el grupo acumula 6 > 5 → AVISO. Ningún profesor se pasa (3
     * cada uno ≤ 5) y ninguna es DISTRIBUIDA, así que NO hay ERROR.
     */
    private void poblarCatalogoConGrupoSobrecargado() {
        Contexto ctx = contextoBase(5);
        Subgrupo desd1 = subgrupoRepository.save(new Subgrupo("1ºA-Desd1", Set.of(ctx.grupo())));
        Subgrupo desd2 = subgrupoRepository.save(new Subgrupo("1ºA-Desd2", Set.of(ctx.grupo())));
        Profesor prof2 = profesorRepository.save(new Profesor("LEN1", "Dos"));

        crearActividad("CyR-1ºA", 3, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(desd1));
        crearActividad("OyD-1ºA", 3, PatronTemporal.NEUTRA, ctx.asignatura(),
                ctx.aula2(), Set.of(prof2), Set.of(desd2));
    }

    /** 5 tramos en 5 días, una actividad de 3 repeticiones: nada dispara. */
    private void poblarCatalogoSano() {
        Contexto ctx = contextoBase(5);
        crearActividad("Mat-1ºA", 3, PatronTemporal.DISTRIBUIDA, ctx.asignatura(),
                ctx.aula1(), Set.of(ctx.prof1()), Set.of(ctx.completo()));
    }

    private record Contexto(GrupoAdministrativo grupo, Subgrupo completo, Profesor prof1,
                            Asignatura asignatura, Aula aula1, Aula aula2) { }

    /** Catálogo común: un grupo, su subgrupo completo, un profesor, dos aulas, N tramos. */
    private Contexto contextoBase(int tramosLectivos) {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo completo = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "Uno"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));

        Dia[] dias = {Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES};
        for (int i = 0; i < tramosLectivos; i++) {
            tramoRepository.save(new TramoSemanal(
                    dias[i % dias.length], LocalTime.of(8, 0), LocalTime.of(9, 0), true, i + 1, null));
        }
        return new Contexto(grupo, completo, prof, mat, a1, a2);
    }

    private void crearActividad(String codigo, int repeticiones, PatronTemporal patron,
            Asignatura asignatura, Aula aula, Set<Profesor> profesores, Set<Subgrupo> subgrupos) {
        Actividad act = new Actividad();
        act.setCodigo(codigo);
        act.setAsignatura(asignatura);
        act.setRepeticionesPorSemana(repeticiones);
        act.setDuracionTramos(1);
        act.setPatronTemporal(patron);
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo + "-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(profesores);
        plaza.setAulaFija(aula);
        plaza.setSubgrupos(subgrupos);
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
