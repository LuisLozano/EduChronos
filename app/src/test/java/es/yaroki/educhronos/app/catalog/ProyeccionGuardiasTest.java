package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.GuardiaVistaDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Las guardias en la proyección (S215, C-exportacion-guardias, A1-A3 y EA2):
 * {@link GeneradorHorarioService#proyectar} las lee de la tabla, les pone día y tramo con la
 * misma conversión que a las sesiones y las ordena por {@code (dia, tramo, profesorCodigo)}.
 *
 * <p>Sobre SQLite real ({@code replace = NONE}) y con las filas escritas A MANO, sin solver ni
 * reparto: lo que se prueba es la lectura, no quién las puso. Las guardias se insertan en un
 * orden que NO es el de A1 —martes primero, y en el mismo tramo el código mayor antes—, de modo
 * que una proyección que devolviera el orden de la tabla caería en el aserto de orden.
 *
 * <p>Malla: lunes y martes con 08:00, 09:00, un recreo de 10:00 a 10:30 y 10:30; el tramo de
 * las 10:30 es el ordenEnDia 3, porque el recreo no consume número.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GeneradorHorarioService.class, EstadoCurso.class})
class ProyeccionGuardiasTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private HorarioGeneradoRepository horarioRepository;
    @Autowired private GuardiaRepository guardiaRepository;

    private final Map<String, TramoSemanal> tramos = new HashMap<>();
    private final Map<String, Profesor> profesores = new HashMap<>();

    @Test
    void proyectaLasGuardiasConSuDiaYSuTramoEnElOrdenDeA1() {
        poblarMalla();
        HorarioGenerado horario = horario();
        guardia(horario, "LEN1", "MARTES 08:00");
        guardia(horario, "MAT1", "LUNES 10:30");
        guardia(horario, "BIO1", "LUNES 10:30");
        guardia(horario, "MAT1", "LUNES 08:00");
        entityManager.flush();
        entityManager.clear();

        assertThat(service.proyectar(horario.getId()).guardias()).containsExactly(
                new GuardiaVistaDTO("MAT1", 1, 1),
                new GuardiaVistaDTO("BIO1", 1, 3),
                new GuardiaVistaDTO("MAT1", 1, 3),
                new GuardiaVistaDTO("LEN1", 2, 1));
    }

    @Test
    void unHorarioSinGuardiasProyectaUnaListaVaciaYNoNull() {
        poblarMalla();
        HorarioGenerado horario = horario();
        entityManager.flush();
        entityManager.clear();

        assertThat(service.proyectar(horario.getId()).guardias()).isNotNull().isEmpty();
    }

    /**
     * EA2: una guardia en un tramo no lectivo aborta la proyección igual que una sesión, con
     * la misma excepción y el mismo mensaje salvo el nombre del campo y de la fila. No se
     * omite: callarla sería exportar un horario distinto del guardado.
     */
    @Test
    void unaGuardiaEnUnTramoNoLectivoAbortaLaProyeccion() {
        poblarMalla();
        HorarioGenerado horario = horario();
        guardia(horario, "MAT1", "LUNES 08:00");
        guardia(horario, "BIO1", "LUNES 10:00");
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> service.proyectar(horario.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El tramo " + tramos.get("LUNES 10:00").getId()
                        + " de una guardia del horario " + horario.getId())
                .hasMessageContaining("no está en el índice de tramos lectivos");
    }

    // ------------------------------------------------------------------ fixture

    private void poblarMalla() {
        for (String codigo : new String[] {"MAT1", "LEN1", "BIO1"}) {
            profesores.put(codigo, profesorRepository.save(new Profesor(codigo, "Profesor " + codigo)));
        }
        int orden = 1;
        for (Dia dia : new Dia[] {Dia.LUNES, Dia.MARTES}) {
            orden = tramo(dia, 8, 0, 9, 0, true, orden);
            orden = tramo(dia, 9, 0, 10, 0, true, orden);
            orden = tramo(dia, 10, 0, 10, 30, false, orden);
            orden = tramo(dia, 10, 30, 11, 30, true, orden);
        }
    }

    private int tramo(Dia dia, int h0, int m0, int h1, int m1, boolean lectivo, int orden) {
        TramoSemanal t = tramoRepository.save(new TramoSemanal(
                dia, LocalTime.of(h0, m0), LocalTime.of(h1, m1), lectivo, orden, null));
        tramos.put(dia.name() + " " + LocalTime.of(h0, m0), t);
        return orden + 1;
    }

    private HorarioGenerado horario() {
        return horarioRepository.save(new HorarioGenerado(
                "Horario con guardias", Instant.parse("2026-10-09T08:00:00Z"), "OPTIMAL", 0.0, 0.0));
    }

    private void guardia(HorarioGenerado horario, String profesor, String tramo) {
        guardiaRepository.save(new Guardia(horario, profesores.get(profesor), tramos.get(tramo)));
    }
}
