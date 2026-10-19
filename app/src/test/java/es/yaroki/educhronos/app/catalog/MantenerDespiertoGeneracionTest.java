package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.curso.RechazoCursoException;
import es.yaroki.educhronos.app.escritorio.MantenerDespierto;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * La generación pide no suspender el equipo y lo suelta (S238, C-sin-suspension, T1 a T3): una
 * petición por generación concedida, soltada en el finally ANTES de la baja en
 * {@link EstadoCurso}, y ninguna cuando la generación se rechaza.
 *
 * <p>Mismo montaje que {@code GeneradorHorarioServiceTest} ({@code @DataJpaTest} +
 * {@code @Import}), con un {@link MantenerDespierto} doble que apunta cada pedir y cada close
 * junto al {@code generando()} de ese momento. Vive en {@code app.catalog} para construir
 * {@code Actividad}/{@code Plaza}, como {@code GenerarHorarioEndpointTest}, del que copia los
 * dos catálogos: el factible ({@code poblarCatalogoMinimo(1, 5)}) y el infactible solo para el
 * solver, que atraviesa la pre-validación y falla DENTRO del try.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class})
class MantenerDespiertoGeneracionTest {

    /** Lo que apunta el doble: qué se llamó, con qué motivo y cuántas generaciones había. */
    record Llamada(String que, String motivo, int generando) {}

    /** Doble que apunta cada pedir y cada close con el {@code generando()} del momento. */
    static final class MantenerDespiertoDoble implements MantenerDespierto {

        final List<Llamada> llamadas = new ArrayList<>();
        private final EstadoCurso estado;

        MantenerDespiertoDoble(EstadoCurso estado) {
            this.estado = estado;
        }

        @Override
        public Peticion pedir(String motivo) {
            llamadas.add(new Llamada("pedir", motivo, estado.generando()));
            return () -> llamadas.add(new Llamada("close", null, estado.generando()));
        }
    }

    @TestConfiguration
    static class ConDoble {
        @Bean
        MantenerDespiertoDoble mantenerDespierto(EstadoCurso estado) {
            return new MantenerDespiertoDoble(estado);
        }
    }

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private EstadoCurso estadoCurso;
    @Autowired private MantenerDespiertoDoble doble;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;

    /** Centro sin guardias (S212), como {@code GenerarHorarioEndpointTest}; y el doble, limpio. */
    @BeforeEach
    void setUp(@Autowired ConfiguracionRepository configuraciones) {
        MinimoGuardias.fijar(configuraciones, 0);
        doble.llamadas.clear();
    }

    /** (T1) Con éxito: un pedir con el motivo y un close, los dos con la generación contada. */
    @Test
    void conExito_pideUnaVezYSueltaAntesDeLaBaja() {
        poblarCatalogoMinimo(1, 5);
        entityManager.flush();

        HorarioGenerado horario = service.generar(null, null, null, "T1");

        assertThat(horario.getId()).isNotNull();
        assertThat(doble.llamadas).extracting(Llamada::que).containsExactly("pedir", "close");
        assertThat(doble.llamadas.get(0).motivo()).isEqualTo(GeneradorHorarioService.MOTIVO);
        assertThat(doble.llamadas).allSatisfy(l -> assertThat(l.generando()).isPositive());
        assertThat(estadoCurso.generando()).isZero();
    }

    /**
     * (T2) El solver declara el catálogo infactible dentro del try: se suelta igual, y sale la
     * misma {@link HorarioInfactibleException} con estado INFEASIBLE que el vecino traduce a 422.
     */
    @Test
    void conElSolverFallando_sueltaIgual() {
        poblarCatalogoInfactibleSoloParaElSolver();
        entityManager.flush();

        assertThatThrownBy(() -> service.generar(5, null, null, "T2"))
                .isInstanceOfSatisfying(HorarioInfactibleException.class,
                        e -> assertThat(e.estado()).isEqualTo("INFEASIBLE"));

        assertThat(doble.llamadas).extracting(Llamada::que).containsExactly("pedir", "close");
        assertThat(estadoCurso.generando()).isZero();
    }

    /** (T3) Con otra generación ya concedida, el rechazo GENERACION_EN_CURSO no pide nada. */
    @Test
    void conOtraGeneracionEnMarcha_noPideNada() {
        assertThat(estadoCurso.intentarIniciarGeneracion()).isEqualTo(EstadoCurso.Admision.CONCEDIDA);
        try {
            assertThatThrownBy(() -> service.generar(5, null, null, "T3"))
                    .isInstanceOfSatisfying(RechazoCursoException.class, e -> {
                        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.causa()).isEqualTo(EstadoCurso.GENERACION_EN_CURSO);
                        assertThat(e.getMessage()).isEqualTo(EstadoCurso.MENSAJE_GENERACION_EN_CURSO);
                    });
        } finally {
            estadoCurso.terminarGeneracion();
        }

        assertThat(doble.llamadas).isEmpty();
    }

    /** Copia de {@code GenerarHorarioEndpointTest#poblarCatalogoMinimo}: factible con {@code (1, 5)}. */
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

        crearActividadDeUnaPlaza("Mat-1ºA", mat, a1, prof, completo, repeticiones);
    }

    /** Copia de {@code GenerarHorarioEndpointTest#poblarCatalogoInfactibleSoloParaElSolver} (S209, T4). */
    private void poblarCatalogoInfactibleSoloParaElSolver() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupoA =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo grupoB =
                grupoRepository.save(new GrupoAdministrativo("1ºB", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sgA = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupoA)));
        Subgrupo sgB = subgrupoRepository.save(new Subgrupo("1ºB-Completo", Set.of(grupoB)));
        Profesor mat8 = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        Profesor len1 = profesorRepository.save(new Profesor("LEN1", "Luis Lopez"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));

        tramoRepository.save(new TramoSemanal(
                Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        tramoRepository.save(new TramoSemanal(
                Dia.MARTES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 2, null));

        crearActividadDeUnaPlaza("X-1ºA", mat, a1, mat8, sgA, 1);
        crearActividadDeUnaPlaza("Y-1ºB", mat, a1, len1, sgB, 1);
        crearActividadDeUnaPlaza("Z-1ºA", mat, a2, len1, sgA, 1);
    }

    /** Actividad DISTRIBUIDA de una sola plaza, con aula fija y {@code repeticiones} repeticiones. */
    private void crearActividadDeUnaPlaza(String codigo, Asignatura asignatura, Aula aula, Profesor profesor,
                                          Subgrupo subgrupo, int repeticiones) {
        Actividad act = new Actividad();
        act.setCodigo(codigo);
        act.setAsignatura(asignatura);
        act.setRepeticionesPorSemana(repeticiones);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo + "-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(Set.of(profesor));
        plaza.setAulaFija(aula);
        plaza.setSubgrupos(Set.of(subgrupo));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
