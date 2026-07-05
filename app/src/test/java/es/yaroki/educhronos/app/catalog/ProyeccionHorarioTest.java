package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
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
 * Cierra el criterio 1 de Fase 7 a nivel de CONTRATO: la proyección plana de un
 * horario persistido ({@link GeneradorHorarioService#proyectar}) refleja el
 * agrupamiento de nivel en las tres vistas (D-F7-1) y colapsa la co-docencia en
 * una entrada por plaza (D-F7-2).
 *
 * <p>Recorre la tubería real sobre SQLite ({@code replace = NONE}) sin cargar el
 * JSON del fixture: transcribe a entidades JPA el bloque 1ºESO de
 * {@code problema-3-cierre-cyr-refmt.json} (mismo patrón builder que
 * {@code CierreFase6HumoTest}, con los recreos que el puente de tramo D30
 * espera), resuelve con el solver real (presupuesto corto, patrón D-B10-7: NO se
 * mete el solver de 120 s en la suite), persiste con {@code guardar} y proyecta.
 * Vive en {@code app.catalog} por los constructores {@code protected} de
 * {@code Actividad}/{@code Plaza}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class ProyeccionHorarioTest {

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

    private static final String ACT_BLOQUE = "Bloque-CyR_OyD_RefMt-1ESO";
    private static final List<String> PLAZAS_BLOQUE = List.of(
            "Bloque-CyR-Tec", "Bloque-CyR-Inf", "Bloque-OyD",
            "Bloque-RefMt-MAT6", "Bloque-RefMt-MAT7", "Bloque-RefMt-MAT4");

    @Test
    void proyectaElBloqueDe1ESOReflejandoElAgrupamientoDeNivel() {
        poblarCatalogo();
        entityManager.flush();

        ProblemaHorario problema = service.cargarProblema();
        ResultadoOptimizacion resultado = new SolverHorario(10, 42).resolverOptimizandoConDetalle(problema);
        Long id = service.guardar(resultado, problema, "Proyeccion Fase 7").getId();
        entityManager.flush();
        entityManager.clear();

        HorarioProyeccionDTO proy = service.proyectar(id);
        List<SesionVistaDTO> ses = proy.sesiones();

        // Total: bloque (6 plazas × 2 rep) + 4 Mat (1 plaza × 3 rep) = 12 + 12 = 24.
        assertThat(ses).hasSize(24);

        // (a) El bloque produce, en CADA una de sus 2 repeticiones, 6 entradas en el
        // MISMO (dia,tramo): una por plaza, todas en el tramo de esa instancia.
        Map<Integer, List<SesionVistaDTO>> bloquePorRep = ses.stream()
                .filter(s -> s.actividadCodigo().equals(ACT_BLOQUE))
                .collect(Collectors.groupingBy(SesionVistaDTO::indice));
        assertThat(bloquePorRep).hasSize(2);
        bloquePorRep.forEach((rep, lista) -> {
            assertThat(lista).hasSize(6);
            assertThat(lista).extracting(SesionVistaDTO::dia).containsOnly(lista.get(0).dia());
            assertThat(lista).extracting(SesionVistaDTO::tramo).containsOnly(lista.get(0).tramo());
            assertThat(lista).extracting(SesionVistaDTO::plazaCodigo)
                    .containsExactlyInAnyOrderElementsOf(PLAZAS_BLOQUE);
        });

        // (b) D-F7-1: en el (dia,tramo) de una repetición del bloque, la vista de 1ºA
        // (filtro grupos.contains("1ºA")) contiene las 6 sub-entradas del bloque —cada
        // plaza aporta un subgrupo de 1ºA, así que el agrupamiento se proyecta entero.
        List<SesionVistaDTO> rep1 = bloquePorRep.get(1);
        int diaRep1 = rep1.get(0).dia();
        int tramoRep1 = rep1.get(0).tramo();
        assertThat(rep1).allSatisfy(s -> assertThat(s.grupos()).contains("1ºA"));
        List<SesionVistaDTO> vista1AEnSlot = ses.stream()
                .filter(s -> s.dia() == diaRep1 && s.tramo() == tramoRep1)
                .filter(s -> s.grupos().contains("1ºA"))
                .toList();
        assertThat(vista1AEnSlot).extracting(SesionVistaDTO::plazaCodigo)
                .containsAll(PLAZAS_BLOQUE);

        // (c) Cada Mat-1ºX (rep=3) da 3 entradas, todas con grupos == ["1ºX"].
        for (String letra : List.of("A", "B", "C", "D")) {
            String act = "Mat-1º" + letra;
            List<SesionVistaDTO> mat = ses.stream()
                    .filter(s -> s.actividadCodigo().equals(act)).toList();
            assertThat(mat).hasSize(3);
            assertThat(mat).allSatisfy(s -> assertThat(s.grupos()).containsExactly("1º" + letra));
        }

        // (d) Ninguna entrada sin aula ni sin profesores.
        assertThat(ses).allSatisfy(s -> {
            assertThat(s.aulaCodigo()).isNotNull();
            assertThat(s.profesores()).isNotEmpty();
        });
    }

    // ------------------------------------------------------------- fixture builder
    // Transcripción de problema-3-cierre-cyr-refmt.json a entidades JPA (new+save),
    // espejo del builder de CierreFase6HumoTest.

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
        bloque.setCodigo(ACT_BLOQUE);
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
