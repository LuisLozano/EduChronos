package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * IT end-to-end del cableado de los bloqueos manuales (Bloque 8.2b-ii): persiste
 * un catálogo con un DESDOBLE de dos plazas (una de aula variable con dos
 * candidatas), un pin de TRAMO sobre su instancia y un pin de AULA sobre la plaza
 * variable; carga por repositorios, ensambla el {@link ProblemaHorario} con
 * {@link CatalogoMapper#aProblemaHorario} pasando las listas de bloqueo REALES,
 * resuelve y persiste con la vía real, y recarga desde la BD para comprobar que el
 * solver respetó ambos pines.
 *
 * <p>Vive en {@code app.catalog} (no {@code app.mapper}) por el mismo motivo que
 * {@code CierreFase6HumoTest}: construye {@code Actividad}/{@code Plaza}, de
 * constructor {@code protected} accesible solo desde su paquete. Corre sobre la
 * SQLite real ({@code replace = NONE}) dentro de la transacción del test, así que
 * la identidad de objeto del puente de tramo (IdentityHashMap) se conserva.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class BloqueoPinRoundTripTest {

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
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;
    @Autowired private SesionBloqueadaRepository pinTramoRepository;
    @Autowired private AulaBloqueadaRepository pinAulaRepository;

    private static final String ACTIVIDAD = "Desdoble-CyR-1ESO";
    private static final String PLAZA_VARIABLE = "Desdoble-CyR-P1"; // aula variable {A1, A2}
    private static final String PLAZA_FIJA = "Desdoble-CyR-P2";     // aula fija A3
    private static final String AULA_PINADA = "A2";

    @Test
    void elSolverRespetaElPinDeTramoYElPinDeAulaTrasElRoundTrip() {
        TramoSemanal tramoPinado = poblarCatalogoConPines();
        entityManager.flush();

        // Ensamblar con las listas de bloqueo REALES (no las vacías del servicio).
        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                tramoRepository.findAll(),
                aulaRepository.findAll(),
                asignaturaRepository.findAll(),
                profesorRepository.findAll(),
                grupoRepository.findAll(),
                subgrupoRepository.findAll(),
                actividadRepository.findAll(),
                restriccionRepository.findAll(),
                pinTramoRepository.findAll(),
                pinAulaRepository.findAll());

        ResultadoOptimizacion resultado =
                new SolverHorario(10, 42).resolverOptimizandoConDetalle(problema);

        Long id = service.guardar(resultado, problema, "Horario con pines").getId();
        entityManager.flush();
        entityManager.clear();

        HorarioGenerado recargado = service.cargarHorario(id);
        List<Sesion> sesiones = recargado.getSesiones().stream()
                .filter(s -> s.getPlaza().getActividad().getCodigo().equals(ACTIVIDAD))
                .toList();

        // La instancia (rep 1) tiene una Sesion por plaza del desdoble: dos.
        assertThat(sesiones).hasSize(2);

        // (a) PIN DE TRAMO: las N plazas del desdoble caen simultáneas en el tramo pinado.
        assertThat(sesiones).allSatisfy(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoPinado.getId()));

        // (b) PIN DE AULA: la plaza variable pinada ocupa exactamente el aula pinada.
        Sesion sesionVariable = sesiones.stream()
                .filter(s -> s.getPlaza().getCodigo().equals(PLAZA_VARIABLE))
                .findFirst().orElseThrow();
        assertThat(sesionVariable.getAula().getCodigo()).isEqualTo(AULA_PINADA);
    }

    // ------------------------------------------------------------- fixture builder

    /** Persiste el catálogo mínimo + los dos pines y devuelve el tramo pinado. */
    private TramoSemanal poblarCatalogoConPines() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sgA = subgrupoRepository.save(new Subgrupo("1ºA-Desd1", Set.of(g)));
        Subgrupo sgB = subgrupoRepository.save(new Subgrupo("1ºA-Desd2", Set.of(g)));

        Asignatura cyr = asignaturaRepository.save(new Asignatura("CyR", "Computacion y Robotica"));
        Profesor profX = profesorRepository.save(new Profesor("TEC3", "Profesor TEC3"));
        Profesor profY = profesorRepository.save(new Profesor("INF1", "Profesor INF1"));

        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));
        Aula a2 = aulaRepository.save(new Aula("A2", TipoAula.ORDINARIA, null, null, null, null));
        Aula a3 = aulaRepository.save(new Aula("A3", TipoAula.ORDINARIA, null, null, null, null));

        // 5 tramos lectivos en lunes; se pinará el tercero (L3).
        TramoSemanal tramoPinado = null;
        for (int orden = 1; orden <= 5; orden++) {
            TramoSemanal t = tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
            if (orden == 3) {
                tramoPinado = t;
            }
        }

        // Desdoble: 2 plazas simultáneas, rep 1. P1 de aula variable {A1, A2}; P2 fija A3.
        Actividad desdoble = new Actividad();
        desdoble.setCodigo(ACTIVIDAD);
        desdoble.setRepeticionesPorSemana(1);
        desdoble.setDuracionTramos(1);
        desdoble.setPatronTemporal(PatronTemporal.NEUTRA);
        desdoble.getPlazas().add(plazaCandidatas(PLAZA_VARIABLE, desdoble, cyr,
                Set.of(profX), Set.of(a1, a2), Set.of(sgA)));
        desdoble.getPlazas().add(plazaFija(PLAZA_FIJA, desdoble, cyr,
                Set.of(profY), a3, Set.of(sgB)));
        actividadRepository.save(desdoble);

        // Pines: tramo de la instancia #1 a L3, y aula de la plaza variable a A2.
        pinTramoRepository.save(new SesionBloqueada(desdoble, 1, tramoPinado));
        Plaza plazaVariable = desdoble.getPlazas().stream()
                .filter(p -> p.getCodigo().equals(PLAZA_VARIABLE))
                .findFirst().orElseThrow();
        pinAulaRepository.save(new AulaBloqueada(desdoble, 1, plazaVariable, a2));

        return tramoPinado;
    }

    /** Plaza con aula variable (aulasCandidatas). */
    private static Plaza plazaCandidatas(String codigo, Actividad actividad, Asignatura asignatura,
            Set<Profesor> profesores, Set<Aula> aulasCandidatas, Set<Subgrupo> subgrupos) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(profesores);
        plaza.setAulasCandidatas(aulasCandidatas);
        plaza.setSubgrupos(subgrupos);
        return plaza;
    }

    /** Plaza con aula fija. */
    private static Plaza plazaFija(String codigo, Actividad actividad, Asignatura asignatura,
            Set<Profesor> profesores, Aula aulaFija, Set<Subgrupo> subgrupos) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(profesores);
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(subgrupos);
        return plaza;
    }
}
