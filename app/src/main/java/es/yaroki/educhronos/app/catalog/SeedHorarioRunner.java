package es.yaroki.educhronos.app.catalog;

import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ANDAMIAJE TEMPORAL (Fase 7, Bloque 7B). Seed desechable para validación visual.
 * Duplica el builder de CierreFase6HumoTest a propósito. BORRAR EN FASE 8 cuando
 * exista la vía real de poblar la BD (formularios CRUD o loader). No construir
 * nada encima de esta clase.
 *
 * <p>Solo se activa con el perfil {@code seed}
 * ({@code --spring.profiles.active=seed}); el arranque normal no re-siembra.
 * Vive en {@code app.catalog} porque los constructores de {@link Actividad} y
 * {@link Plaza} son {@code protected} (accesibles solo desde su paquete), igual
 * que {@code CierreFase6HumoTest}. La siembra corre en una única transacción de
 * escritura sobre el contexto Spring Boot completo (COMMIT real contra
 * {@code educhronos.db}); el solver resuelve dentro de ella (~10 s).
 */
@Component
@Profile("seed")
public class SeedHorarioRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedHorarioRunner.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final GeneradorHorarioService service;
    private final HorarioGeneradoRepository horarioRepository;
    private final NivelRepository nivelRepository;
    private final GrupoAdministrativoRepository grupoRepository;
    private final SubgrupoRepository subgrupoRepository;
    private final ProfesorRepository profesorRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final AulaRepository aulaRepository;
    private final TramoSemanalRepository tramoRepository;
    private final ActividadRepository actividadRepository;

    public SeedHorarioRunner(
            GeneradorHorarioService service,
            HorarioGeneradoRepository horarioRepository,
            NivelRepository nivelRepository,
            GrupoAdministrativoRepository grupoRepository,
            SubgrupoRepository subgrupoRepository,
            ProfesorRepository profesorRepository,
            AsignaturaRepository asignaturaRepository,
            AulaRepository aulaRepository,
            TramoSemanalRepository tramoRepository,
            ActividadRepository actividadRepository) {
        this.service = service;
        this.horarioRepository = horarioRepository;
        this.nivelRepository = nivelRepository;
        this.grupoRepository = grupoRepository;
        this.subgrupoRepository = subgrupoRepository;
        this.profesorRepository = profesorRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.aulaRepository = aulaRepository;
        this.tramoRepository = tramoRepository;
        this.actividadRepository = actividadRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long existentes = horarioRepository.count();
        if (existentes > 0) {
            String ids = horarioRepository.findAll().stream()
                    .map(h -> String.valueOf(h.getId()))
                    .collect(Collectors.joining(", "));
            log.info("[seed] Ya hay {} horario(s) en la BD (id: {}); NO se re-siembra.", existentes, ids);
            return;
        }

        poblarCatalogo();
        entityManager.flush();

        ProblemaHorario problema = service.cargarProblema();
        ResultadoOptimizacion resultado =
                new SolverHorario(10, 42).resolverOptimizandoConDetalle(problema);
        HorarioGenerado horario = service.guardar(resultado, problema, "Horario seed 7B");

        log.info("[seed] Horario guardado con id={} (estadoSolver={}). "
                        + "URL: http://localhost:8080/api/horarios/{}/proyeccion",
                horario.getId(), resultado.estado().name(), horario.getId());
    }

    // ---------------------------------------------------------- fixture builder
    // DUPLICADO consciente de CierreFase6HumoTest.poblarCatalogo() (decisión con el
    // arquitecto: no extraer a src/main para no tocar el test verde de Fase 6).

    private void poblarCatalogo() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));

        Map<String, Asignatura> asig = new HashMap<>();
        asig.put("CyR", asignaturaRepository.save(new Asignatura("CyR", "Computacion y Robotica")));
        asig.put("OyD", asignaturaRepository.save(new Asignatura("OyD", "Oratoria y Debate")));
        asig.put("RefMt", asignaturaRepository.save(new Asignatura("RefMt", "Refuerzo de Matematicas")));
        asig.put("Mat", asignaturaRepository.save(new Asignatura("Mat", "Matematicas")));

        Map<String, Profesor> prof = new HashMap<>();
        for (String cod : List.of("TEC3", "INF1", "FIL3", "MAT6", "MAT7", "MAT4",
                "MATA", "MATB", "MATC", "MATD")) {
            prof.put(cod, profesorRepository.save(new Profesor(cod, "Profesor " + cod)));
        }

        Map<String, Aula> aula = new HashMap<>();
        for (String cod : List.of("A5", "B07", "A12In", "A11", "A3", "A14",
                "A10", "A1", "A2", "A4", "A7")) {
            aula.put(cod, aulaRepository.save(new Aula(cod, TipoAula.ORDINARIA, null, null, null, null)));
        }

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

        // 6 lectivos/día + 1 recreo/día (esLectivo=false) intercalado tras el 3.er tramo.
        int orden = 1;
        for (Dia dia : List.of(Dia.LUNES, Dia.MARTES, Dia.MIERCOLES, Dia.JUEVES, Dia.VIERNES)) {
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(8, 0), LocalTime.of(9, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(9, 0), LocalTime.of(10, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(10, 0), LocalTime.of(11, 0), true, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(11, 0), LocalTime.of(11, 30), false, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(11, 30), LocalTime.of(12, 30), true, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(12, 30), LocalTime.of(13, 30), true, orden++, null));
            tramoRepository.save(new TramoSemanal(dia, LocalTime.of(13, 30), LocalTime.of(14, 30), true, orden++, null));
        }

        // Actividad de bloque: 6 plazas, asignatura null, rep 2, NEUTRA.
        Actividad bloque = new Actividad();
        bloque.setCodigo("Bloque-CyR_OyD_RefMt-1ESO");
        bloque.setRepeticionesPorSemana(2);
        bloque.setDuracionTramos(1);
        bloque.setPatronTemporal(PatronTemporal.NEUTRA);
        bloque.getPlazas().add(plazaCandidatas("Bloque-CyR-Tec", bloque, asig.get("CyR"),
                Set.of(prof.get("TEC3")), Set.of(aula.get("A5"), aula.get("B07")), across(sub, "CyR-Tec")));
        bloque.getPlazas().add(plazaFija("Bloque-CyR-Inf", bloque, asig.get("CyR"),
                Set.of(prof.get("INF1")), aula.get("A12In"), across(sub, "CyR-Inf")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-OyD", bloque, asig.get("OyD"),
                Set.of(prof.get("FIL3")), Set.of(aula.get("A11"), aula.get("A5")), across(sub, "OyD")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT6", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT6")), Set.of(aula.get("A3"), aula.get("A11")), across(sub, "RefMt-MAT6")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT7", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT7")), Set.of(aula.get("A14"), aula.get("A3")), across(sub, "RefMt-MAT7")));
        bloque.getPlazas().add(plazaCandidatas("Bloque-RefMt-MAT4", bloque, asig.get("RefMt"),
                Set.of(prof.get("MAT4")), Set.of(aula.get("A10"), aula.get("A14")), across(sub, "RefMt-MAT4")));
        actividadRepository.save(bloque);

        crearMat("Mat-1ºA", asig.get("Mat"), prof.get("MATA"), aula.get("A1"), sub.get("1ºA-Completo"));
        crearMat("Mat-1ºB", asig.get("Mat"), prof.get("MATB"), aula.get("A2"), sub.get("1ºB-Completo"));
        crearMat("Mat-1ºC", asig.get("Mat"), prof.get("MATC"), aula.get("A4"), sub.get("1ºC-Completo"));
        crearMat("Mat-1ºD", asig.get("Mat"), prof.get("MATD"), aula.get("A7"), sub.get("1ºD-Completo"));
    }

    private void crearMat(String codigo, Asignatura mat, Profesor profesor, Aula aulaFija, Subgrupo completo) {
        Actividad act = new Actividad();
        act.setCodigo(codigo);
        act.setAsignatura(mat);
        act.setRepeticionesPorSemana(3);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.DISTRIBUIDA);
        act.getPlazas().add(plazaFija(codigo + "-P1", act, mat, Set.of(profesor), aulaFija, Set.of(completo)));
        actividadRepository.save(act);
    }

    private static Set<Subgrupo> across(Map<String, Subgrupo> sub, String sufijo) {
        return Set.of(sub.get("1ºA-" + sufijo), sub.get("1ºB-" + sufijo),
                sub.get("1ºC-" + sufijo), sub.get("1ºD-" + sufijo));
    }

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
