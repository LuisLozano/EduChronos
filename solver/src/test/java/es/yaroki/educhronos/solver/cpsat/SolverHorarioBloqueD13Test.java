package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Prueba de discriminación de D13: un bloque de {@code duracionTramos > 1} no
 * puede arrancar en un tramo desde el que desbordaría el día ni cruzar el
 * recreo. La lista blanca de inicios de {@link ModeloCpSat} restringe el
 * dominio del inicio a las posiciones físicamente posibles; si el único inicio
 * geométrico de un fixture cae fuera de la lista blanca, el problema es
 * INFEASIBLE.
 *
 * <p>Los tres fixtures comparten estructura (1 grupo, 1 subgrupo, 1 profesor,
 * 1 aula fija, 1 actividad de {@code duracionTramos=2}) y solo difieren en la
 * GEOMETRÍA de los tramos disponibles, de modo que la posición del bloque es la
 * única variable que decide la factibilidad:
 * <ul>
 *   <li><b>valido</b>: tramos {@code ordenEnDia} 1 y 2 → el bloque cabe en 1-2
 *       (mismo día, no cruza recreo) → FEASIBLE.</li>
 *   <li><b>desborde-dia</b>: un único tramo {@code ordenEnDia=1} → el bloque de
 *       2 necesitaría el orden 2, que no existe → lista blanca vacía →
 *       INFEASIBLE.</li>
 *   <li><b>cruce-recreo</b>: tramos {@code ordenEnDia} 3 y 4 → el único inicio
 *       (orden 3) ocuparía 3-4, que cruzan el recreo → lista blanca vacía →
 *       INFEASIBLE.</li>
 * </ul>
 *
 * <p>El control positivo es lo que prueba que los dos INFEASIBLE vienen de D13
 * y no de un defecto estructural del fixture: con la misma forma y un bloque
 * que SÍ cabe, el solver lo coloca y el verificador no reporta violaciones.
 * Sin D13, el caso <b>cruce-recreo</b> sería FEASIBLE (el índice plano trata
 * los órdenes 3 y 4 como contiguos), que es exactamente el bug que D13 cierra.
 */
class SolverHorarioBloqueD13Test {

    private static ProblemaHorario cargar(String fixture) throws IOException {
        String ruta = "/fixtures/" + fixture;
        try (InputStream in = SolverHorarioBloqueD13Test.class.getResourceAsStream(ruta)) {
            assertThat(in)
                    .as("fixture en el classpath: " + ruta)
                    .isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    @Test
    @Timeout(60)
    void bloqueDuracion2CabeEnInicioValido() throws IOException {
        // Control positivo: tramos ordenEnDia 1 y 2; el bloque de 2 cabe en 1-2.
        ProblemaHorario problema = cargar("problema-d13-bloque-valido.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        ResultadoVerificacion verificacion =
                new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.violaciones())
                .as("un bloque que cabe no debe violar ninguna restricción dura")
                .isEmpty();
    }

    @Test
    @Timeout(60)
    void bloqueDuracion2QueDesbordaElDiaEsInfactible() throws IOException {
        // Un único tramo (ordenEnDia=1): el bloque de 2 no tiene un segundo tramo
        // en el día. Sin D13 el IntervalVar [0,2) desbordaría sin que nada lo
        // impida; con D13 la lista blanca de inicios queda vacía → INFEASIBLE.
        ProblemaHorario problema = cargar("problema-d13-desborde-dia.json");

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }

    @Test
    @Timeout(60)
    void bloqueDuracion2QueCruzaElRecreoEsInfactible() throws IOException {
        // Tramos ordenEnDia 3 y 4: el único inicio posible (orden 3) ocuparía
        // 3-4, que cruzan el recreo. Es el caso que el índice plano NO distingue
        // por sí solo (3 y 4 son contiguos en índice); D13 lo prohíbe.
        ProblemaHorario problema = cargar("problema-d13-cruce-recreo.json");

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }
}