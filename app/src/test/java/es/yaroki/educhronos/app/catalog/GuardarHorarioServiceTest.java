package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.app.persistence.EstadoHorario;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Test de integración de {@link GeneradorHorarioService#guardar} y
 * {@link GeneradorHorarioService#cargarHorario} (Fase 6, Bloque 9) sobre la SQLite
 * real ({@code replace = NONE}). Persiste un catálogo, lo carga a
 * {@link ProblemaHorario}, fabrica a mano una {@link SolucionHorario} y verifica
 * que la salida se materializa en {@link Sesion} recuperables.
 *
 * <p>Vive en {@code app.catalog} (no {@code app.service}) porque construye las
 * entidades {@code Actividad}/{@code Plaza}, que solo exponen constructor
 * {@code protected}: {@code new}+setters solo es posible desde su paquete.
 *
 * <p>El caso central es un DESDOBLE: una actividad con DOS plazas (misma
 * instancia), que debe producir DOS sesiones en el mismo tramo. Los tramos
 * incluyen un recreo intercalado ({@code esLectivo=false}) para ejercitar la
 * renumeración del puente {@code Tramo → TramoSemanal}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class GuardarHorarioServiceTest {

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

    @Test
    void guardaYRecuperaUnDesdobleDeDosPlazasConSuTramoYAula() {
        // --- catálogo: grupo + 2 subgrupos (mitades del desdoble) ---
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo mitadA = subgrupoRepository.save(new Subgrupo("1ºA-A", Set.of(grupo)));
        Subgrupo mitadB = subgrupoRepository.save(new Subgrupo("1ºA-B", Set.of(grupo)));

        Profesor prof1 = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        Profesor prof2 = profesorRepository.save(new Profesor("FIS3", "Luis López"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a5 = aulaRepository.save(new Aula("A5", TipoAula.ORDINARIA, null, null, null, null));
        Aula a6 = aulaRepository.save(new Aula("A6", TipoAula.ORDINARIA, null, null, null, null));

        // Tramos con recreo (orden 2) intercalado: lectivos orden 1 y 3 -> L1, L2.
        TramoSemanal lunes1 = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(9, 0), LocalTime.of(9, 30), false, 2, null));
        tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(9, 30), LocalTime.of(10, 30), true, 3, null));

        // Actividad de 2 plazas (desdoble), aula fija por plaza.
        Actividad actividad = new Actividad();
        actividad.setCodigo("DES");
        actividad.setAsignatura(mat);
        actividad.setRepeticionesPorSemana(1);
        actividad.setDuracionTramos(1);
        actividad.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza p1 = plazaConAulaFija("DES-P1", actividad, mat, prof1, a5, mitadA);
        Plaza p2 = plazaConAulaFija("DES-P2", actividad, mat, prof2, a6, mitadB);
        actividad.getPlazas().add(p1);
        actividad.getPlazas().add(p2);
        actividadRepository.save(actividad);

        entityManager.flush();

        // --- carga a dominio + solución fabricada a mano ---
        ProblemaHorario problema = service.cargarProblema();
        es.yaroki.educhronos.solver.domain.Actividad actDom = problema.actividades().get(0);
        ActividadInstancia instancia = new ActividadInstancia(actDom, 1);
        Tramo l1 = problema.tramos().get(0); // (LUNES, ordenEnDia 1)
        // Ambas plazas tienen aula fija -> aulaElegida las resuelve sin registrar nada.
        SolucionHorario solucion = new SolucionHorario(Map.of(instancia, l1));
        ResultadoOptimizacion resultado =
                new ResultadoOptimizacion(solucion, CpSolverStatus.OPTIMAL, 3.0, 3.0);

        // --- guardar + recargar ---
        Long id = service.guardar(resultado, problema, "Horario de prueba").getId();
        entityManager.flush();
        entityManager.clear();

        HorarioGenerado recargado = service.cargarHorario(id);

        // (c) metadata del solve
        assertThat(recargado.getNombre()).isEqualTo("Horario de prueba");
        assertThat(recargado.getEstado()).isEqualTo(EstadoHorario.BORRADOR);
        assertThat(recargado.getEstadoSolver()).isEqualTo("OPTIMAL");
        assertThat(recargado.getObjetivo()).isEqualTo(3.0);
        assertThat(recargado.getCotaInferior()).isEqualTo(3.0);
        assertThat(recargado.getFechaGeneracion()).isNotNull();

        // (a) 2 filas Sesion (una por plaza), con su tramo y aula correctos
        Map<String, Sesion> porPlaza = recargado.getSesiones().stream()
                .collect(Collectors.toMap(s -> s.getPlaza().getCodigo(), s -> s));
        assertThat(porPlaza).containsOnlyKeys("DES-P1", "DES-P2");

        Sesion s1 = porPlaza.get("DES-P1");
        assertThat(s1.getIndice()).isEqualTo(1);
        assertThat(s1.getTramoInicio().getId()).isEqualTo(lunes1.getId());
        assertThat(s1.getAula().getCodigo()).isEqualTo("A5");

        Sesion s2 = porPlaza.get("DES-P2");
        assertThat(s2.getIndice()).isEqualTo(1);
        assertThat(s2.getTramoInicio().getId()).isEqualTo(lunes1.getId());
        assertThat(s2.getAula().getCodigo()).isEqualTo("A6");
    }

    @Test
    void guardarAbortaSiUnaPlazaNoTieneAulaAsignada() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));
        Aula a5 = aulaRepository.save(new Aula("A5", TipoAula.ORDINARIA, null, null, null, null));
        tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));

        // Plaza con aula VARIABLE (aulasCandidatas, sin aula fija).
        Actividad actividad = new Actividad();
        actividad.setCodigo("NEG");
        actividad.setAsignatura(mat);
        actividad.setRepeticionesPorSemana(1);
        actividad.setDuracionTramos(1);
        actividad.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("NEG-P1");
        plaza.setActividad(actividad);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(prof));
        plaza.setAulasCandidatas(Set.of(a5)); // variable, no fija
        plaza.setSubgrupos(Set.of(sg));
        actividad.getPlazas().add(plaza);
        actividadRepository.save(actividad);
        entityManager.flush();

        ProblemaHorario problema = service.cargarProblema();
        ActividadInstancia instancia = new ActividadInstancia(problema.actividades().get(0), 1);
        // Instancia colocada en un tramo, pero SIN registrar el aula elegida ->
        // aulaElegida devuelve empty -> guardar aborta (D-B9-4).
        SolucionHorario solucion = new SolucionHorario(Map.of(instancia, problema.tramos().get(0)));
        ResultadoOptimizacion resultado =
                new ResultadoOptimizacion(solucion, CpSolverStatus.OPTIMAL, 0.0, 0.0);

        assertThatThrownBy(() -> service.guardar(resultado, problema, "Infactible"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NEG-P1");
    }

    /** Plaza JPA con aula fija (rama A del XOR), un profesor y un subgrupo. */
    private static Plaza plazaConAulaFija(
            String codigo, Actividad actividad, Asignatura asignatura,
            Profesor profesor, Aula aulaFija, Subgrupo subgrupo) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        plaza.setProfesores(Set.of(profesor));
        plaza.setAulaFija(aulaFija);
        plaza.setSubgrupos(Set.of(subgrupo));
        return plaza;
    }
}
