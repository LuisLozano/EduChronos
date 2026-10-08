package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.ViolacionGuardiaDTO;
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

/**
 * El diagnóstico comprueba las guardias de un horario guardado (S213, C-reparto-guardias,
 * condición 3 de O-guardias; K7 y K9 c). Cada regla con un caso que la dispara y otro que no.
 *
 * <p><b>Cómo.</b> Se genera un horario sin guardias (mínimo 0, nadie tiene guardias) y después se
 * escriben a mano las filas de {@code guardia}, el mínimo y las guardias de cada profesor que pide
 * cada caso: el diagnóstico compara lo guardado con el catálogo actual. La jornada del lunes tiene
 * un recreo entre el segundo y el tercer tramo lectivo, para la regla del tramo no lectivo (E2).
 * MAT1 da una clase; GUA1 no da ninguna.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class})
class DiagnosticoGuardiasTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private DiagnosticoService diagnosticoService;
    @Autowired private GuardiaRepository guardiaRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private SesionRepository sesionRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;

    private Long horarioId;

    @BeforeEach
    void horarioSinGuardias() {
        poblar();
        MinimoGuardias.fijar(configuracionRepository, 0);
        entityManager.flush();
        entityManager.clear();
        horarioId = service.generar(10, 42, null, "s213-diagnostico-guardias").getId();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void sinGuardiasYConMinimoCero_ningunaViolacionDeGuardia() {
        assertThat(guardiaRepository.findByHorarioId(horarioId)).isEmpty();

        assertThat(diagnostico()).isEmpty();
    }

    @Test
    void numeroDistinto_soloElProfesorQueNoTieneLasQueLeCorresponden() {
        guardias("MAT1", 2);
        guardias("GUA1", 1);
        guardia("GUA1", lectivo(1));
        guardia("MAT1", tramoLibreDeMat1());

        assertThat(de(DiagnosticoService.REGLA_GUARDIAS_NUMERO_DISTINTO))
                .extracting(ViolacionGuardiaDTO::profesorCodigo, ViolacionGuardiaDTO::tramoCodigo,
                        ViolacionGuardiaDTO::mensaje)
                .containsExactly(tuple("MAT1", null, "MAT1 tiene 1 guardias y le corresponden 2."));
    }

    @Test
    void bajoMinimo_soloElTramoLectivoQueNoLlega() {
        MinimoGuardias.fijar(configuracionRepository, 1);
        guardias("GUA1", 3);
        guardia("GUA1", lectivo(1));
        guardia("GUA1", lectivo(2));
        guardia("GUA1", lectivo(3));

        assertThat(de(DiagnosticoService.REGLA_GUARDIAS_BAJO_MINIMO))
                .extracting(ViolacionGuardiaDTO::profesorCodigo, ViolacionGuardiaDTO::tramoCodigo,
                        ViolacionGuardiaDTO::mensaje)
                .containsExactly(tuple(null, "L4",
                        "tramo L4 (día 1, tramo 4): 0 profesores de guardia y el mínimo es 1."));
    }

    @Test
    void tramoOcupado_laGuardiaDelQueDaClaseAhiSiYLaDelQueNoNo() {
        TramoSemanal clase = tramoDeLaClase();
        guardia("MAT1", clase);
        guardia("GUA1", clase);
        int orden = ordenLectivo(clase);

        assertThat(de(DiagnosticoService.REGLA_GUARDIA_EN_TRAMO_OCUPADO))
                .extracting(ViolacionGuardiaDTO::profesorCodigo, ViolacionGuardiaDTO::tramoCodigo,
                        ViolacionGuardiaDTO::mensaje)
                .containsExactly(tuple("MAT1", "L" + orden, "MAT1 tiene guardia en tramo L" + orden
                        + " (día 1, tramo " + orden + "), donde tiene MAT-1ESO."));
    }

    @Test
    void noPuede_laGuardiaEnElNoPuedeSiYEnOtroTramoNo() {
        Profesor gua = profesorRepository.findByCodigo("GUA1").orElseThrow();
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                gua, lectivo(2), TipoRestriccion.DURA, 0, "no puede"));
        guardia("GUA1", lectivo(2));
        guardia("GUA1", lectivo(3));

        assertThat(de(DiagnosticoService.REGLA_GUARDIA_EN_NO_PUEDE))
                .extracting(ViolacionGuardiaDTO::profesorCodigo, ViolacionGuardiaDTO::tramoCodigo,
                        ViolacionGuardiaDTO::mensaje)
                .containsExactly(tuple("GUA1", "L2",
                        "GUA1 tiene guardia en tramo L2 (día 1, tramo 2), marcado como \"No puede\"."));
    }

    /** E2: el reparto nunca la produce; se escribe a mano una guardia sobre el recreo. */
    @Test
    void tramoNoLectivo_laGuardiaEnElRecreoSiYEnUnTramoLectivoNo() {
        guardia("GUA1", recreo());
        guardia("GUA1", lectivo(1));

        assertThat(de(DiagnosticoService.REGLA_GUARDIA_EN_TRAMO_NO_LECTIVO))
                .extracting(ViolacionGuardiaDTO::profesorCodigo, ViolacionGuardiaDTO::tramoCodigo,
                        ViolacionGuardiaDTO::mensaje)
                .containsExactly(tuple("GUA1", "LUNES 10:00-10:30",
                        "GUA1 tiene guardia en tramo LUNES 10:00-10:30, que no es hora de clase."));
    }

    // ─────────────────────────────────────────────────────────────── andamio

    private List<ViolacionGuardiaDTO> diagnostico() {
        entityManager.flush();
        entityManager.clear();
        return diagnosticoService.diagnosticar(horarioId).violacionesGuardia();
    }

    private List<ViolacionGuardiaDTO> de(String regla) {
        return diagnostico().stream().filter(v -> v.regla().equals(regla)).toList();
    }

    private void guardias(String codigo, int n) {
        Profesor profesor = profesorRepository.findByCodigo(codigo).orElseThrow();
        profesor.setGuardiasOrdinarias(n);
        profesorRepository.save(profesor);
    }

    private void guardia(String codigo, TramoSemanal tramo) {
        HorarioGenerado horario = horarioRepository.findById(horarioId).orElseThrow();
        guardiaRepository.save(new Guardia(horario, profesorRepository.findByCodigo(codigo).orElseThrow(), tramo));
    }

    /** El tramo lectivo {@code n} del lunes (1..4), saltando el recreo. */
    private TramoSemanal lectivo(int n) {
        return tramoRepository.findAllByOrderByOrdenAsc().stream().filter(TramoSemanal::isEsLectivo)
                .toList().get(n - 1);
    }

    private TramoSemanal recreo() {
        return tramoRepository.findAllByOrderByOrdenAsc().stream().filter(t -> !t.isEsLectivo())
                .findFirst().orElseThrow();
    }

    private TramoSemanal tramoDeLaClase() {
        return sesionRepository.findByHorarioId(horarioId).get(0).getTramoInicio();
    }

    private int ordenLectivo(TramoSemanal tramo) {
        List<TramoSemanal> lectivos = tramoRepository.findAllByOrderByOrdenAsc().stream()
                .filter(TramoSemanal::isEsLectivo).toList();
        for (int i = 0; i < lectivos.size(); i++) {
            if (lectivos.get(i).getId().equals(tramo.getId())) {
                return i + 1;
            }
        }
        throw new IllegalStateException("tramo no lectivo");
    }

    private TramoSemanal tramoLibreDeMat1() {
        Long clase = tramoDeLaClase().getId();
        return tramoRepository.findAllByOrderByOrdenAsc().stream()
                .filter(t -> t.isEsLectivo() && !t.getId().equals(clase)).findFirst().orElseThrow();
    }

    /** Lunes: 8-9, 9-10, recreo 10:00-10:30, 10:30-11:30 y 11:30-12:30. */
    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor mat1 = profesorRepository.save(new Profesor("MAT1", "Profesor MAT1"));
        profesorRepository.save(new Profesor("GUA1", "Profesor GUA1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        tramoRepository.save(new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        tramoRepository.save(new TramoSemanal(Dia.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), true, 2, null));
        tramoRepository.save(new TramoSemanal(Dia.LUNES, LocalTime.of(10, 0), LocalTime.of(10, 30), false, 3, null));
        tramoRepository.save(new TramoSemanal(Dia.LUNES, LocalTime.of(10, 30), LocalTime.of(11, 30), true, 4, null));
        tramoRepository.save(new TramoSemanal(Dia.LUNES, LocalTime.of(11, 30), LocalTime.of(12, 30), true, 5, null));

        Actividad act = new Actividad();
        act.setCodigo("MAT-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("MAT-1ESO-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(mat1));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
