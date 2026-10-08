package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
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
 * La generación reparte las guardias ordinarias y las guarda con el horario (S213,
 * C-reparto-guardias, K9 b): una fila por guardia, cada profesor con las suyas, ninguna en un tramo
 * en que da clase, y el diagnóstico del horario recién generado sin violaciones de guardia.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class, DiagnosticoService.class})
class RepartoGuardiasGeneracionTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private DiagnosticoService diagnosticoService;
    @Autowired private GuardiaRepository guardiaRepository;
    @Autowired private SesionRepository sesionRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;

    @Test
    void generarConGuardias_dejaUnaFilaPorGuardiaYElDiagnosticoSinViolacionesDeGuardia() {
        poblar();
        MinimoGuardias.fijar(configuracionRepository, 1);
        entityManager.flush();
        entityManager.clear();

        Long id = service.generar(10, 42, null, "s213-guardias").getId();
        entityManager.flush();
        entityManager.clear();

        List<Guardia> guardias = guardiaRepository.findByHorarioId(id);
        Map<String, Long> porProfesor = guardias.stream().collect(
                Collectors.groupingBy(g -> g.getProfesor().getCodigo(), Collectors.counting()));
        assertThat(porProfesor).isEqualTo(Map.of("MAT1", 2L, "GUA1", 4L));

        Long tramoDeLaClase = sesionRepository.findByHorarioId(id).get(0).getTramoInicio().getId();
        assertThat(guardias)
                .filteredOn(g -> g.getProfesor().getCodigo().equals("MAT1"))
                .noneMatch(g -> g.getTramo().getId().equals(tramoDeLaClase));

        assertThat(diagnosticoService.diagnosticar(id).violacionesGuardia()).isEmpty();
    }

    /** Cinco tramos de lunes; MAT1 da una clase y tiene 2 guardias; GUA1 no da clase y tiene 4. */
    private void poblar() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor pmat = new Profesor("MAT1", "Profesor MAT1");
        pmat.setGuardiasOrdinarias(2);
        profesorRepository.save(pmat);
        Profesor gua = new Profesor("GUA1", "Profesor GUA1");
        gua.setGuardiasOrdinarias(4);
        profesorRepository.save(gua);
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        for (int orden = 1; orden <= 5; orden++) {
            tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
        }

        Actividad act = new Actividad();
        act.setCodigo("MAT-1ESO");
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza plaza = new Plaza();
        plaza.setCodigo("MAT-1ESO-P1");
        plaza.setActividad(act);
        plaza.setAsignatura(mat);
        plaza.setProfesores(Set.of(pmat));
        plaza.setAulaFija(a1);
        plaza.setSubgrupos(Set.of(sg));
        act.getPlazas().add(plaza);
        actividadRepository.save(act);
    }
}
