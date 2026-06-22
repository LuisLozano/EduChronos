package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * DISCRIMINACIÓN del término de SESIONES CONSECUTIVAS MÁXIMAS del profesorado
 * (Fase 5, Bloque 6d-c, Turno A).
 *
 * <p>Demuestra que el solver EVITA encadenar más de {@code MAX_CONSECUTIVAS}
 * sesiones seguidas cuando puede, eligiendo la solución de menor coste entre
 * varias factibles. Gemelo del término de ventanas (6a) y de la indisponibilidad
 * blanda (6c) en el eje de las penalizaciones blandas; aquí la métrica penalizada
 * es la racha de sesiones contiguas en {@code ordenEnDia} que excede el máximo.
 *
 * <p>Fixture (validado por enumeración exhaustiva en diseño, N={@code MAX_CONSECUTIVAS}=3):
 * P1 con 4 actividades NEUTRA de 1 repetición, mismo subgrupo y misma aula, en un
 * día de 4 tramos (L1..L4, posiciones 1..4) más un día de 1 tramo (M1, posición 1).
 * El no-solape de profesor/aula/subgrupo obliga a las 4 instancias a 4 tramos
 * distintos de los 5; el solver elige cuáles. Espacio completo de soluciones (qué
 * tramos quedan ocupados; las 4 actividades son intercambiables a efectos de coste):
 * <ul>
 *   <li>3 al día1 en {1,2,3} ó {2,3,4} + 1 al día2 → racha ≤3, sin hueco →
 *       consecutivas 0, ventanas 0 → ÓPTIMO (coste 0)</li>
 *   <li>4 al día1 ({1,2,3,4}) → racha de 4 → consecutivas 1, ventanas 0 → coste 1</li>
 *   <li>3 al día1 con hueco ({1,2,4} ó {1,3,4}) + 1 al día2 → consecutivas 0,
 *       ventanas 1 → coste 1</li>
 * </ul>
 * El óptimo es 0 y existe una alternativa factible más cara (meter las 4 al día1,
 * consecutivas 1). Un solver que ignorara las consecutivas podría devolver las 4
 * juntas el día1; minimizar obliga a repartir.
 *
 * <p>Qué NO prueba: minimización con óptimo estrictamente positivo (aquí encadenar
 * de más es evitable → óptimo 0). Eso es el Turno B (oro fuerte), donde encadenar
 * más de N se hace inevitable.
 *
 * <p>El test asevera DOS cosas, no una: (a) la penalización recomputada es 0, y
 * (b) el día1 NO contiene las 4 sesiones (queda en ≤3). Solo (a) sería débil —un
 * solver que repartiera por azar también daría 0—; (b) cierra el agujero
 * comprobando que se evitó la racha de 4, que es la única configuración penalizada
 * del fixture. El óptimo NO es único en colocación (hay varias formas de llegar a
 * coste 0), por eso (b) asevera la propiedad "≤3 en el día1", no una posición
 * concreta. El recuento se hace vía
 * {@link VerificadorSolucion#contarPenalizacionConsecutivasProfesor}, independiente
 * de OR-Tools, porque {@link SolverHorario#resolverOptimizando} no expone el valor
 * del objetivo (decisión 2a).
 *
 * <p><b>Ventanas no contamina:</b> en el óptimo (coste 0) ventanas vale 0 Y
 * consecutivas vale 0; en la alternativa rechazada (4 juntas) ventanas vale 0 y
 * consecutivas vale 1. Ventanas vale 0 en ambas ramas relevantes, así que la
 * diferencia de coste (0 vs 1) la aporta solo el término de consecutivas. La
 * enumeración exhaustiva confirma que todos los óptimos tienen ventanas 0.
 */
class SolverHorarioConsecutivasProfesorTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void elOptimizadorEvitaEncadenarDeMasCuandoPuede() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6d-consecutivas-discriminacion.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        // (a) penalización de consecutivas recomputada = 0 (óptimo alcanzable).
        int penalizacion =
                verificador.contarPenalizacionConsecutivasProfesor(problema, sol);
        assertThat(penalizacion)
                .as("encadenar de más es evitable: el óptimo es 0, el solver no debe pagarlo")
                .isEqualTo(0);

        // (b) el día1 (diaSemana=1) NO contiene las 4 sesiones: el solver repartió
        // para no formar la racha de 4. Cierra el agujero de (a): comprueba que se
        // evitó la única configuración penalizada del fixture, no que el coste sea
        // 0 por casualidad. El óptimo no es único en colocación, así que se asevera
        // la propiedad "≤3 sesiones en el día1", no una posición concreta.
        Set<Tramo> ocupadosDia1 = new HashSet<>();
        for (var inst : Expansion.todas(problema)) {
            Optional<Tramo> tramo = sol.tramoDeInstancia(inst);
            if (tramo.isPresent() && tramo.get().diaSemana() == 1) {
                ocupadosDia1.add(tramo.get());
            }
        }
        assertThat(ocupadosDia1.size())
                .as("el solver debe repartir: a lo sumo 3 sesiones en el día de 4 tramos")
                .isLessThanOrEqualTo(3);
    }

    private ProblemaHorario cargar(String ruta) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("fixture no encontrado: " + ruta);
            }
            return loader.cargar(in);
        }
    }
}
