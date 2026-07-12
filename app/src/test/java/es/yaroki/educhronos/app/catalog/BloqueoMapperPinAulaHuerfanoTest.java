package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.mapper.BloqueoMapper;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Test unitario del pin de aula HUÉRFANO en {@link BloqueoMapper} (deuda (c) del
 * Bloque 8.2b-iii): una {@link AulaBloqueada} cuya instancia (actividad, índice)
 * NO tiene su {@link SesionBloqueada} de tramo correspondiente es entrada
 * incoherente —el record {@code domain.SesionBloqueada} no soporta el pin de aula
 * suelto— y {@code aBloqueos} debe ABORTAR. Cubre la validación final del mapper
 * (el submapa de aulas que ningún pin de tramo consume).
 *
 * <p>No es un IT: no toca Spring ni la BD. Vive en {@code app.catalog} (no
 * {@code app.mapper}) por el mismo motivo que {@code BloqueoPinRoundTripTest}:
 * construye entidades JPA {@code Actividad}/{@code Plaza}, de constructor
 * {@code protected} accesible solo desde este paquete. {@code aBloqueos} no
 * persiste: solo lee sus getters. Los índices de dominio se montan a mano, como
 * haría {@code CatalogoMapper}.
 */
class BloqueoMapperPinAulaHuerfanoTest {

    @Test
    void pinDeAulaSinSuPinDeTramoAborta() {
        // Entidades JPA mínimas: solo se leen sus getters de código/índice.
        Actividad actividad = new Actividad();
        actividad.setCodigo("ACT");
        Aula aulaJpa = new Aula("A1", TipoAula.ORDINARIA, null, null, null, null);
        Plaza plazaJpa = new Plaza();
        plazaJpa.setCodigo("P1");

        // Pin de aula para la instancia ACT#1, SIN su pin de tramo (pinesTramo vacío).
        AulaBloqueada pinAula = new AulaBloqueada(actividad, 1, plazaJpa, aulaJpa);

        // Índices de dominio: la plaza es de aula variable con A1 candidata, para que
        // el pin de aula supere sus validaciones previas y llegue a la de huérfano.
        es.yaroki.educhronos.solver.domain.Aula aulaDom =
                new es.yaroki.educhronos.solver.domain.Aula("A1", "A1");
        es.yaroki.educhronos.solver.domain.Plaza plazaDom =
                new es.yaroki.educhronos.solver.domain.Plaza(
                        "P1",
                        new es.yaroki.educhronos.solver.domain.Asignatura("MAT", "Matematicas"),
                        Set.of(new es.yaroki.educhronos.solver.domain.Profesor("MAT1", "Profesor MAT1")),
                        Optional.empty(),
                        Set.of(aulaDom),
                        Set.of());

        Map<String, es.yaroki.educhronos.solver.domain.Actividad> actividadesPorCodigo = Map.of();
        Map<String, es.yaroki.educhronos.solver.domain.Plaza> plazasPorCodigo = Map.of("P1", plazaDom);
        Map<String, es.yaroki.educhronos.solver.domain.Aula> aulasPorCodigo = Map.of("A1", aulaDom);
        Map<TramoSemanal, Tramo> tramosPorEntidad = new IdentityHashMap<>();

        assertThatThrownBy(() -> BloqueoMapper.aBloqueos(
                List.<SesionBloqueada>of(), List.of(pinAula),
                actividadesPorCodigo, plazasPorCodigo, aulasPorCodigo, tramosPorEntidad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pin de aula sin pin de tramo")
                .hasMessageContaining("ACT#1");
    }
}
