package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * COMPROBACIÓN DE ORO FUERTE del término de SESIONES CONSECUTIVAS MÁXIMAS del
 * profesorado (Fase 5, Bloque 6d-c, Turno B).
 *
 * <p>Cierra lo que el Turno A (discriminación) no podía: que el optimizador
 * minimiza el exceso de sesiones consecutivas cuando el óptimo es ESTRICTAMENTE
 * POSITIVO —cuando encadenar más de {@code MAX_CONSECUTIVAS} es inevitable, el
 * solver encadena lo menos posible—, rechazando una alternativa factible más cara.
 * Gemelo del oro fuerte de ventanas (S25) y de la indisponibilidad blanda (6c).
 *
 * <p>Fixture (validado por enumeración exhaustiva en diseño, N={@code MAX_CONSECUTIVAS}=3):
 * P1 con 7 actividades NEUTRA de 1 repetición, mismo subgrupo y misma aula, en dos
 * días de 4 tramos cada uno (día1 L1..L4, día2 M1..M4; posiciones 1..4 en ambos).
 * El no-solape obliga a las 7 instancias a 7 tramos distintos de los 8 (uno queda
 * libre); el solver elige el reparto. Con 7 sesiones y bloques sin exceso de tamaño
 * máximo 3 solo caben 6 (3+3): la séptima fuerza a un día a tener 4 sesiones, y
 * 4 sesiones en un día de 4 tramos están saturadas (sin hueco posible) → racha de 4
 * → consecutivas ≥ 1. El óptimo es 1, estrictamente positivo.
 *
 * <p>Espacio completo (por enumeración): el reparto factible es {4,3} ó {3,4}.
 * El día de 4 está saturado → racha de 4 → consecutivas 1, ventanas 0. El día de 3
 * admite colocación sin hueco ({1,2,3} ó {2,3,4}) → consecutivas 0, ventanas 0; o
 * con hueco → ventanas 1 (peor). Óptimo total = 1 (consecutivas 1 + ventanas 0);
 * existe alternativa factible de coste 2 (día de 3 con hueco, ó las dos vías de
 * exceso). Un solver que ignorara el objetivo podría devolver una de coste 2;
 * minimizar obliga a 1.
 *
 * <p><b>La vía-ventanas a coste 1 NO existe (clave del aislamiento):</b> partir la
 * racha para llevar consecutivas a 0 obliga a dejar un hueco, que cuesta ventanas
 * ≥ 1, y como algún día ha de tener 4 sesiones (racha de 4, consecutivas ≥ 1
 * inevitable), partir SUMA en vez de sustituir → coste ≥ 2. La enumeración
 * exhaustiva confirma: ninguna solución de coste total ≤ 1 tiene consecutivas 0.
 * Por tanto, en cualquier óptimo, {@code contarPenalizacionConsecutivasProfesor}
 * vale exactamente 1; no puede ser un coste 1 aportado por ventanas con
 * consecutivas 0. El término de consecutivas se prueba sin contaminación del de
 * ventanas (decisión de diseño 6d-c: aislar saturando los días).
 *
 * <p><b>El test asevera el COSTE, no la posición.</b> El óptimo (1) se alcanza de
 * varias formas (qué día se satura, cómo se colocan las 3 del otro día): la
 * colocación óptima NO es única, así que aseverar "tal tramo quedó vacío" sería
 * incorrecto. Lo determinista es el coste de consecutivas = 1. Mismo criterio que
 * el oro fuerte de ventanas y de la indisponibilidad blanda. El recuento es
 * independiente de OR-Tools
 * ({@link VerificadorSolucion#contarPenalizacionConsecutivasProfesor}), porque
 * {@link SolverHorario#resolverOptimizando} no expone el valor del objetivo.
 */
class SolverHorarioOroFuerteConsecutivasTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void elOptimizadorMinimizaLasConsecutivasCuandoEncadenarEsInevitable() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6d-consecutivas-oro-fuerte.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        int penalizacion =
                verificador.contarPenalizacionConsecutivasProfesor(problema, sol);

        // Óptimo determinista = 1. El solver debe rechazar las colocaciones
        // factibles de coste 2 (saturar un día con racha de 5 imposible aquí; o
        // partir y pagar ventana además del exceso inevitable). Si devolviera 2,
        // esto fallaría.
        assertThat(penalizacion)
                .as("encadenar es inevitable: el óptimo es exactamente 1, no 0 ni 2")
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
