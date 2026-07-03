package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Round-trip de persistencia de {@link ProfesorRestriccionHoraria} (Fase 6,
 * Bloque 7) sobre la SQLite real ({@code replace = NONE}, no H2). Persiste
 * profesor + tramo + una restricción DURA y una BLANDA, hace {@code flush()+clear()}
 * y verifica que ambas FK ({@code profesor_id}, {@code tramo_id}), el tipo, el peso
 * y el motivo sobreviven la ida y vuelta.
 *
 * <p>Es el PRIMER {@code @ManyToOne} JPA a {@code TramoSemanal} no
 * autorreferencial: valida que el dialecto de comunidad SQLite resuelve esa FK
 * (la autorreferencia {@code siguienteInmediato} ya existía; una FK desde otra
 * tabla es un caso nuevo).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfesorRestriccionHorariaRoundTripTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProfesorRestriccionHorariaRepository restriccionRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private TramoSemanalRepository tramoRepository;

    @Test
    void restriccionesDuraYBlandaSobrevivenElRoundTrip() {
        Profesor prof = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        TramoSemanal lunes1 = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null));
        TramoSemanal lunes2 = tramoRepository.save(
                new TramoSemanal(Dia.LUNES, LocalTime.of(9, 0), LocalTime.of(10, 0), true, 2, null));

        Long duraId = restriccionRepository.save(new ProfesorRestriccionHoraria(
                prof, lunes1, TipoRestriccion.DURA, 0, "No disponible a primera hora")).getId();
        Long blandaId = restriccionRepository.save(new ProfesorRestriccionHoraria(
                prof, lunes2, TipoRestriccion.BLANDA, 7, null)).getId();

        entityManager.flush();
        entityManager.clear();

        ProfesorRestriccionHoraria dura = restriccionRepository.findById(duraId).orElseThrow();
        assertThat(dura.getProfesor().getCodigo()).isEqualTo("MAT8");
        assertThat(dura.getTramo().getId()).isEqualTo(lunes1.getId());
        assertThat(dura.getTipo()).isEqualTo(TipoRestriccion.DURA);
        assertThat(dura.getPeso()).isEqualTo(0);
        assertThat(dura.getMotivo()).isEqualTo("No disponible a primera hora");

        ProfesorRestriccionHoraria blanda = restriccionRepository.findById(blandaId).orElseThrow();
        assertThat(blanda.getProfesor().getCodigo()).isEqualTo("MAT8");
        assertThat(blanda.getTramo().getId()).isEqualTo(lunes2.getId());
        assertThat(blanda.getTipo()).isEqualTo(TipoRestriccion.BLANDA);
        assertThat(blanda.getPeso()).isEqualTo(7);
        assertThat(blanda.getMotivo()).isNull(); // motivo opcional, no persistido
    }
}
