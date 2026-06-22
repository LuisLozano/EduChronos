package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * COMPROBACIÓN DE ORO FUERTE del término de indisponibilidad BLANDA del
 * profesorado (Fase 5, Bloque 6c, Turno B).
 *
 * <p>Cierra lo que el Turno A (discriminación) no podía: que el optimizador
 * minimiza la penalización blanda cuando el óptimo es ESTRICTAMENTE POSITIVO
 * —cuando incumplir alguna preferencia es inevitable, el solver incumple las
 * menos posibles—, rechazando una alternativa factible más cara. Gemelo del oro
 * fuerte de ventanas de S25.
 *
 * <p>Fixture (validado por enumeración en diseño): P1 con 2 actividades NEUTRA
 * de 1 repetición, en 3 días de 1 tramo lectivo cada uno (L1, L2, L3). El
 * no-solape de profesor obliga a las 2 instancias a 2 de los 3 tramos (uno queda
 * libre); el solver elige cuáles. Restricciones BLANDA: P1 vetado-blando en L1 y
 * en L2 (el tramo limpio es L3). Espacio completo (elección = qué tramo se vacía):
 * <ul>
 *   <li>vacío L1 → ocupa {L2,L3} → 1 vetado (L2) → penalización 1 (ÓPTIMO)</li>
 *   <li>vacío L2 → ocupa {L1,L3} → 1 vetado (L1) → penalización 1 (ÓPTIMO)</li>
 *   <li>vacío L3 → ocupa {L1,L2} → 2 vetados → penalización 2 (factible, PEOR)</li>
 * </ul>
 * Con 2 vetados, 1 limpio y 2 instancias, al menos una cae en vetado: óptimo ≥ 1,
 * estrictamente positivo. El óptimo es 1; existe alternativa factible de coste 2.
 * Un solver que ignorara la blanda trataría los 3 tramos como equivalentes y
 * podría devolver {L1,L2} (coste 2); minimizar obliga a 1.
 *
 * <p><b>Ventanas aisladas:</b> cada tramo es el único lectivo de su día, así que
 * cada clase está sola en su día → 0 ventanas en TODA colocación. El término de
 * ventanas es idénticamente 0; el único coste es la blanda. El oro fuerte prueba
 * la blanda sin contaminación del otro término (decisión de diseño 6c: aislar).
 *
 * <p><b>El test asevera el COSTE, no la posición.</b> El óptimo (1) se alcanza de
 * DOS formas (vaciar L1 o L2): la colocación óptima NO es única, así que aseverar
 * "tal tramo quedó vacío" sería incorrecto (el solver puede devolver cualquiera de
 * las dos). Lo determinista es el coste = 1. Mismo criterio que el oro fuerte de
 * ventanas, que asevera el conteo y no la disposición. El recuento es
 * independiente de OR-Tools ({@link VerificadorSolucion#contarPenalizacionIndisponibilidadBlanda}),
 * porque {@link SolverHorario#resolverOptimizando} no expone el valor del objetivo.
 */
class SolverHorarioOroFuerteIndispBlandaTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void elOptimizadorMinimizaLaBlandaCuandoIncumplirEsInevitable() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6c-indisp-blanda-oro-fuerte.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        int penalizacion =
                verificador.contarPenalizacionIndisponibilidadBlanda(problema, sol);

        // Óptimo determinista = 1. El solver debe rechazar la colocación factible
        // de coste 2 (ocupar L1 y L2). Si devolviera 2, esto fallaría.
        assertThat(penalizacion)
                .as("incumplir es inevitable: el óptimo es exactamente 1, no 0 ni 2")
                .isEqualTo(1);
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