package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * IT del cableado de tutorías en {@link GeneradorHorarioService#cargarProblema()}
 * (Bloque 8.5-D2b-2, cierra D-F8.5-D2b1-a): persiste sobre la SQLite real dos
 * {@link ProfesorTutoria} —una TUTOR_PRINCIPAL y una CO_TUTOR, con profesores y grupos
 * divergentes— y comprueba que el {@link ProblemaHorario} que devuelve el servicio las
 * trae de la BD, con su rol correcto, en vez del {@code List.of()} placeholder anterior.
 *
 * <p>Este es el aserto que convierte el cableado en transporte VERIFICADO: los
 * {@code @ManyToOne(LAZY)} de la PK de {@code ProfesorTutoria} se navegan dentro de la
 * {@code @Transactional} del servicio; fuera de ella el mapeo por identidad fallaría.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class GeneradorHorarioTutoriaRoundTripTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private ProfesorTutoriaRepository tutoriaRepository;

    @Test
    void cargarProblemaTraeLasTutoriasDeLaBd() {
        Profesor pMat = profesorRepository.save(new Profesor("P-MAT", "Prof Mates"));
        Profesor pLen = profesorRepository.save(new Profesor("P-LEN", "Prof Lengua"));
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g1 = grupoRepository.save(
                new GrupoAdministrativo("G-1ESO", nivel, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo g2 = grupoRepository.save(
                new GrupoAdministrativo("G-2ESO", nivel, TipoGrupo.ORDINARIO, null));

        tutoriaRepository.save(new ProfesorTutoria(pMat, g1, RolTutoria.TUTOR_PRINCIPAL));
        tutoriaRepository.save(new ProfesorTutoria(pLen, g2, RolTutoria.CO_TUTOR));
        entityManager.flush();
        entityManager.clear();

        ProblemaHorario problema = service.cargarProblema();

        assertThat(problema.tutorias())
                .as("cargarProblema debe traer las tutorías de la BD, no List.of()")
                .extracting(t -> t.profesor().codigo() + "/" + t.grupo().codigo() + "/" + t.rol())
                .containsExactlyInAnyOrder(
                        "P-MAT/G-1ESO/TUTOR_PRINCIPAL",
                        "P-LEN/G-2ESO/CO_TUTOR");
    }
}
