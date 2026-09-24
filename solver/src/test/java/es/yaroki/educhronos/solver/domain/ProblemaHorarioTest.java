package es.yaroki.educhronos.solver.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Invariante de indexación de {@link ProblemaHorario} (S166): los tramos van ordenados por
 * {@code (diaSemana, ordenEnDia)} y no hay dos con el mismo par. El modelo la necesita porque
 * sus intervalos cubren los índices {@code t..t+d−1} de un bloque que empieza en {@code t}, y
 * eso solo son los tramos siguientes del mismo día si los índices de un día son contiguos y
 * crecen con {@code ordenEnDia}. NO se exige {@code ordenEnDia} sin huecos: el hueco lo cubre
 * la lista blanca de inicios del modelo, que no deja arrancar un bloque que lo cruce.
 */
class ProblemaHorarioTest {

    private static ProblemaHorario con(Tramo... tramos) {
        return new ProblemaHorario(List.of(tramos), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void tramosDesordenadosDentroDeUnDia_seRechazan() {
        assertThatThrownBy(() -> con(new Tramo("L2", 1, 2), new Tramo("L1", 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deben ir ordenados")
                .hasMessageContaining("L1")
                .hasMessageContaining("L2");
    }

    @Test
    void diasIntercalados_seRechazan() {
        assertThatThrownBy(() -> con(
                new Tramo("L1", 1, 1), new Tramo("M1", 2, 1), new Tramo("L2", 1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deben ir ordenados")
                .hasMessageContaining("M1")
                .hasMessageContaining("L2");
    }

    @Test
    void dosTramosConElMismoDiaYOrden_seRechazan() {
        assertThatThrownBy(() -> con(new Tramo("L1", 1, 1), new Tramo("OTRO", 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("un solo tramo")
                .hasMessageContaining("L1")
                .hasMessageContaining("OTRO");
    }

    @Test
    void ordenEnDiaConHuecosPeroOrdenado_seAcepta() {
        ProblemaHorario p = con(new Tramo("L1", 1, 1), new Tramo("L3", 1, 3),
                new Tramo("M2", 2, 2));

        assertThat(p.tramos()).extracting(Tramo::codigo).containsExactly("L1", "L3", "M2");
    }
}
