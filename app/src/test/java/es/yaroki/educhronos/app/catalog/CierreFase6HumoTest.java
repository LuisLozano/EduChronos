package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Test de humo END-TO-END que cierra la Fase 6 (persistencia). Recorre la tubería
 * completa sobre la SQLite real ({@code replace = NONE}) sin tocar ningún
 * {@code src/main} ni cargar el JSON del fixture: catálogo JPA poblado a mano →
 * {@link GeneradorHorarioService#cargarProblema()} → {@link SolverHorario} →
 * {@link VerificadorSolucion} → {@link GeneradorHorarioService#guardar} →
 * {@link GeneradorHorarioService#cargarHorario}.
 *
 * <p>Vive en {@code app.catalog} (no {@code app.service}) por el mismo motivo que
 * {@code GuardarHorarioServiceTest}: construye entidades {@code Actividad}/{@code Plaza},
 * cuyo constructor es {@code protected} y solo es accesible desde su paquete.
 *
 * <p>El catálogo es una transcripción fiel a entidades JPA del fixture
 * {@code problema-3-cierre-cyr-refmt.json} (28 subgrupos, 10 profesores, 4
 * asignaturas, 11 aulas, 5 actividades: un bloque de 6 plazas rep 2 + 4 Mat de 1
 * plaza rep 3). Los tramos añaden a los 30 lectivos un recreo por día
 * ({@code esLectivo=false}) intercalado tras el 3.er tramo, para ejercitar la
 * renumeración del puente {@code Tramo → TramoSemanal} (deuda D30): si el recreo
 * se colase como lectivo, el emparejamiento se desplazaría en silencio y alguna
 * Sesion caería en un tramo no lectivo — lo que la aserción final vigila.
 *
 * <p><b>Vía del solver.</b> Como {@code ResultadoOptimizacion.objetivo/cotaInferior}
 * son {@code double} primitivos (no admiten {@code null}), no se puede envolver a
 * mano una solución de factibilidad pura con cotas nulas. Se recorre la vía real de
 * optimización, {@link SolverHorario#resolverOptimizandoConDetalle}, y se persisten
 * el objetivo y la cota reales que devuelve — que es además lo que hace
 * {@code GeneradorHorarioService.generar()} en producción.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class CierreFase6HumoTest {

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
    @Autowired private HorarioGeneradoRepository horarioRepository;

    /** Referencias del catálogo que el test 2 necesita tras poblar. */
    private record Fixture(Profesor mat6, TramoSemanal tramoLectivo) { }

    private static final List<String> PLAZAS_BLOQUE = List.of(
            "Bloque-CyR-Tec", "Bloque-CyR-Inf", "Bloque-OyD",
            "Bloque-RefMt-MAT6", "Bloque-RefMt-MAT7", "Bloque-RefMt-MAT4");
    private static final List<String> PLAZAS_MAT = List.of(
            "Mat-1ºA-P1", "Mat-1ºB-P1", "Mat-1ºC-P1", "Mat-1ºD-P1");

    @Test
    void generaGuardaYRecuperaElHorarioCompletoDeLaFase6() {
        poblarCatalogo();
        entityManager.flush();

        // Cargar el catálogo a dominio y resolver con el MISMO problema que luego se
        // guarda (las claves de la solución son ActividadInstancia de este problema).
        ProblemaHorario problema = service.cargarProblema();
        ResultadoOptimizacion resultado = new SolverHorario(10, 42).resolverOptimizandoConDetalle(problema);

        // El solver da una solución sin violaciones duras (verificación independiente).
        ResultadoVerificacion verificacion =
                new VerificadorSolucion().verificar(problema, resultado.solucion());
        assertThat(verificacion.violaciones()).isEmpty();

        // Persistir la salida y recargarla desde la BD.
        Long id = service.guardar(resultado, problema, "Horario de cierre Fase 6").getId();
        entityManager.flush();
        entityManager.clear();

        HorarioGenerado recargado = service.cargarHorario(id);
        List<Sesion> sesiones = new ArrayList<>(recargado.getSesiones());

        // Total: bloque (6 plazas × 2 rep) + 4 Mat (1 plaza × 3 rep) = 12 + 12 = 24.
        assertThat(sesiones).hasSize(24);

        Map<String, Long> porPlaza = sesiones.stream()
                .collect(Collectors.groupingBy(s -> s.getPlaza().getCodigo(), Collectors.counting()));
        for (String plaza : PLAZAS_BLOQUE) {
            assertThat(porPlaza).containsEntry(plaza, 2L); // desdoble del bloque: 6 plazas × 2 rep
        }
        for (String plaza : PLAZAS_MAT) {
            assertThat(porPlaza).containsEntry(plaza, 3L); // cada Mat: 1 plaza × 3 rep
        }

        // Toda Sesion queda con plaza, tramo y aula (FKs no null) y en un tramo lectivo
        // (el puente no emparejó ninguna contra un recreo: D30).
        assertThat(sesiones).allSatisfy(s -> {
            assertThat(s.getPlaza()).isNotNull();
            assertThat(s.getTramoInicio()).isNotNull();
            assertThat(s.getAula()).isNotNull();
            assertThat(s.getTramoInicio().isEsLectivo()).isTrue();
        });
    }

    @Test
    void generarUnSegundoHorarioNoDestruyeElCatalogoNiElHorarioPrevio() {
        Fixture fx = poblarCatalogo();
        entityManager.flush();

        // Horario A.
        ProblemaHorario problemaA = service.cargarProblema();
        ResultadoOptimizacion resA = new SolverHorario(10, 42).resolverOptimizandoConDetalle(problemaA);
        Long idA = service.guardar(resA, problemaA, "Horario A").getId();
        entityManager.flush();

        // Foto de los conteos de catálogo (las 8 colecciones fijas; la restricción
        // que se añade a continuación NO es catálogo: es la entrada nueva del test).
        long nivel0 = nivelRepository.count();
        long grupo0 = grupoRepository.count();
        long subgrupo0 = subgrupoRepository.count();
        long profesor0 = profesorRepository.count();
        long asignatura0 = asignaturaRepository.count();
        long aula0 = aulaRepository.count();
        long tramo0 = tramoRepository.count();
        long actividad0 = actividadRepository.count();

        // Añadir una restricción BLANDA (no altera la factibilidad) sobre MAT6 en un
        // tramo lectivo y volver a generar+guardar como Horario B.
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                fx.mat6(), fx.tramoLectivo(), TipoRestriccion.BLANDA, 1, "test cierre"));
        entityManager.flush();

        ProblemaHorario problemaB = service.cargarProblema();
        ResultadoOptimizacion resB = new SolverHorario(10, 42).resolverOptimizandoConDetalle(problemaB);
        Long idB = service.guardar(resB, problemaB, "Horario B").getId();
        entityManager.flush();
        entityManager.clear();

        // B se generó y persistió sin excepción.
        assertThat(idB).isNotNull();

        // El catálogo queda intacto tras la segunda generación.
        assertThat(nivelRepository.count()).isEqualTo(nivel0);
        assertThat(grupoRepository.count()).isEqualTo(grupo0);
        assertThat(subgrupoRepository.count()).isEqualTo(subgrupo0);
        assertThat(profesorRepository.count()).isEqualTo(profesor0);
        assertThat(asignaturaRepository.count()).isEqualTo(asignatura0);
        assertThat(aulaRepository.count()).isEqualTo(aula0);
        assertThat(tramoRepository.count()).isEqualTo(tramo0);
        assertThat(actividadRepository.count()).isEqualTo(actividad0);

        // Coexisten dos horarios; el Horario A y sus sesiones siguen recuperables.
        assertThat(horarioRepository.count()).isEqualTo(2);
        HorarioGenerado a = service.cargarHorario(idA);
        assertThat(a.getSesiones()).hasSize(24);
    }

    // ------------------------------------------------------------- fixture builder

    /**
     * Transcribe {@code problema-3-cierre-cyr-refmt.json} a entidades JPA (new+save).
     * Devuelve las referencias que el test 2 reutiliza (MAT6 y un tramo lectivo).
     */
    private Fixture poblarCatalogo() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));

        // Asignaturas.
        Map<String, Asignatura> asig = new HashMap<>();
        asig.put("CyR", asignaturaRepository.save(new Asignatura("CyR", "Computacion y Robotica")));
        asig.put("OyD", asignaturaRepository.save(new Asignatura("OyD", "Oratoria y Debate")));
        asig.put("RefMt", asignaturaRepository.save(new Asignatura("RefMt", "Refuerzo de Matematicas")));
        asig.put("Mat", asignaturaRepository.save(new Asignatura("Mat", "Matematicas")));

        // Profesores.
        Map<String, Profesor> prof = new HashMap<>();
        for (String cod : List.of("TEC3", "INF1", "FIL3", "MAT6", "MAT7", "MAT4",
                "MATA", "MATB", "MATC", "MATD")) {
            prof.put(cod, profesorRepository.save(new Profesor(cod, "Profesor " + cod)));
        }

        // Aulas (todas ORDINARIA: el modelo CP-SAT no restringe por tipo de aula).
        Map<String, Aula> aula = new HashMap<>();
        for (String cod : List.of("A5", "B07", "A12In", "A11", "A3", "A14",
                "A10", "A1", "A2", "A4", "A7")) {
            aula.put(cod, aulaRepository.save(new Aula(cod, TipoAula.ORDINARIA, null, null, null, null)));
        }

        // Grupos 1ºA..1ºD (ORDINARIO, sin padre) y sus 7 subgrupos (Completo + 6).
        Map<String, Subgrupo> sub = new HashMap<>();
        List<String> sufijos = List.of("CyR-Tec", "CyR-Inf", "OyD",
                "RefMt-MAT6", "RefMt-MAT7", "RefMt-MAT4");
        for (String letra : List.of("A", "B", "C", "D")) {
            String cg = "1º" + letra;
            GrupoAdministrativo g = grupoRepository.save(
                    new GrupoAdministrativo(cg, eso1, TipoGrupo.ORDINARIO, null));
            sub.put(cg + "-Completo",
                    subgrupoRepository.save(new Subgrupo(cg + "-Completo", Set.of(g))));
            for (String suf : sufijos) {
                sub.put(cg + "-" + suf,
                        subgrupoRepository.save(new Subgrupo(cg + "-" + suf, Set.of(g))));
            }
        }

        // Tramos: 6 lectivos/día + 1 recreo/día (esLectivo=false) intercalado tras el
        // 3.er tramo, con 'orden' global correlativo. La renumeración del puente filtra
        // el recreo; el orden global lo situa entre el 3.er y el 4.º lectivo del día.
        TramoSemanal lunes1 = null;
        int orden = 1;
        for (Dia dia : List.of(Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES)) {
            TramoSemanal l1 = tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(8, 0), LocalTime.of(9, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(9, 0), LocalTime.of(10, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(10, 0), LocalTime.of(11, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal( // recreo
                    dia, LocalTime.of(11, 0), LocalTime.of(11, 30), false, orden++, null));
            tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(11, 30), LocalTime.of(12, 30), true, orden++, null));
            tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(12, 30), LocalTime.of(13, 30), true, orden++, null));
            tramoRepository.save(new TramoSemanal(
                    dia, LocalTime.of(13, 30), LocalTime.of(14, 30), true, orden++, null));
            if (lunes1 == null) {
                lunes1 = l1;
            }
        }

        // Actividad de bloque: 6 plazas, asignatura null, rep 2, NEUTRA.
        Actividad bloque = new Actividad();
        bloque.setCodigo("Bloque-CyR_OyD_RefMt-1ESO");
        bloque.setRepeticionesPorSemana(2);
        bloque.setDuracionTramos(1);
        bloque.setPatronTemporal(PatronTemporal.NEUTRA);
        bloque.getPlazas().add(plazaCandidatas("Bloque-CyR-Tec", bloque, asig.get("CyR"),
                Set.of(prof.get("TEC3")), Set.of(aula.get("A5"), aula.get("B07")),
                across(sub, "CyR-Tec")));
        bloque.getPlazas().add(plazaFija("Bloque-CyR-Inf", bloque, asig.get("CyR"),
                Set.of(prof.get("INF1")), aula.get("A12In"), across(sub, "CyR-Inf")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-OyD", bloque, asig.get("OyD"),
                Set.of(prof.get("FIL3")), Set.of(aula.get("A11"), aula.get("A5")),
                across(sub, "OyD")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT6", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT6")), Set.of(aula.get("A3"), aula.get("A11")),
                across(sub, "RefMt-MAT6")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT7", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT7")), Set.of(aula.get("A14"), aula.get("A3")),
                across(sub, "RefMt-MAT7")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT4", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT4")), Set.of(aula.get("A10"), aula.get("A14")),
                across(sub, "RefMt-MAT4")));
        actividadRepository.save(bloque);

        // 4 actividades Mat: 1 plaza (aula fija), rep 3, DISTRIBUIDA.
        crearMat("Mat-1ºA", asig.get("Mat"), prof.get("MATA"), aula.get("A1"), sub.get("1ºA-Completo"));
        crearMat("Mat-1ºB", asig.get("Mat"), prof.get("MATB"), aula.get("A2"), sub.get("1ºB-Completo"));
        crearMat("Mat-1ºC", asig.get("Mat"), prof.get("MATC"), aula.get("A4"), sub.get("1ºC-Completo"));
        crearMat("Mat-1ºD", asig.get("Mat"), prof.get("MATD"), aula.get("A7"), sub.get("1ºD-Completo"));

        return new Fixture(prof.get("MAT6"), lunes1);
    }

    /** Actividad Mat de una plaza con aula fija, rep 3, DISTRIBUIDA. */
    private void crearMat(String codigo, Asignatura mat, Profesor profesor,
            Aula aulaFija, Subgrupo completo) {
        Actividad act = new Actividad();
        act.setCodigo(codigo);
        act.setAsignatura(mat);
        act.setRepeticionesPorSemana(3);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        act.getPlazas().add(plazaFija(codigo + "-P1", act, mat,
                Set.of(profesor), aulaFija, Set.of(completo)));
        actividadRepository.save(act);
    }

    /** Los 4 subgrupos (uno por grupo 1ºA..1ºD) que comparten un mismo sufijo. */
    private static Set<Subgrupo> across(Map<String, Subgrupo> sub, String sufijo) {
        return Set.of(sub.get("1ºA-" + sufijo), sub.get("1ºB-" + sufijo),
                sub.get("1ºC-" + sufijo), sub.get("1ºD-" + sufijo));
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
