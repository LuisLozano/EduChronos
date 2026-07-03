package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Nivel;
import es.yaroki.educhronos.app.catalog.NivelRepository;
import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHoraria;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHorariaRepository;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.catalog.TipoRestriccion;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Test de integración de {@link GeneradorHorarioService#cargarProblema()} (Fase 6,
 * Bloque 8) sobre la SQLite real ({@code replace = NONE}). Mismo patrón que los
 * round-trip de catálogo ({@code @DataJpaTest}), más {@code @Import} del servicio,
 * que no es un componente que {@code @DataJpaTest} escanee por sí solo.
 *
 * <p>Persiste un catálogo mínimo que EJERCITA el riesgo dominante del bloque —la
 * resolución por identidad de objeto sobre relaciones LAZY, que fuera de la
 * sesión de Hibernate fallaría (silenciosamente en el caso de {@code grupoPadre})—
 * incluyendo obligatoriamente un grupo PDC con {@code grupoPadre} no nulo y una
 * restricción horaria que apunta a un {@code TramoSemanal} real. Verifica que el
 * {@link ProblemaHorario} ensamblado reconstruye ambos enlaces correctamente. No
 * corre el solver: el valor está en la carga y la frontera transaccional.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class GeneradorHorarioServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GeneradorHorarioService service;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;

    @Test
    void cargarProblemaEnlazaGrupoPadreYRestriccionPorReferencia() {
        // --- grupo PDC con grupo padre (I5): ejercita grupoPadre LAZY ---
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo ordinario =
                grupoRepository.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        grupoRepository.save(
                new GrupoAdministrativo("3ºADi", eso1, TipoGrupo.DIVERSIFICACION_PDC, ordinario));
        subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(ordinario)));

        // --- profesor + tramos + restricción sobre un tramo real ---
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        TramoSemanal lunes1 = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), true, 2, null));
        restriccionRepository.save(new ProfesorRestriccionHoraria(
                prof, lunes1, TipoRestriccion.DURA, 0, "No disponible a primera hora"));

        entityManager.flush(); // asegura que los findAll del servicio ven las filas

        ProblemaHorario problema = service.cargarProblema();

        // (a) el grupo PDC tiene su grupoPadre correctamente enlazado.
        es.yaroki.educhronos.solver.domain.GrupoAdministrativo pdc = problema.grupos().stream()
                .filter(g -> g.codigo().equals("3ºADi"))
                .findFirst()
                .orElseThrow();
        assertThat(pdc.grupoPadre()).isPresent();
        assertThat(pdc.grupoPadre().orElseThrow().codigo()).isEqualTo("1ºA");

        // (b) la restricción apunta al Tramo correcto (resuelto por identidad).
        assertThat(problema.restriccionesHorarias()).singleElement().satisfies(r -> {
            assertThat(r.profesor().codigo()).isEqualTo("MAT8");
            assertThat(r.tramo().codigo()).isEqualTo("L1"); // lunes, 1er tramo lectivo
        });
    }
}
