package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * COMPROBACIÓN DE ORO FUERTE de las ventanas del profesorado (Fase 5, Bloque 6b).
 *
 * <p>Cierra la deuda comprometida en S24: el test de 6a demostró que el
 * optimizador alcanza 0 ventanas cuando 0 es alcanzable, pero NO que minimice
 * cuando el óptimo es estrictamente positivo (un hueco inevitable no era
 * construible sin un dato que prohibiera tramos — las indisponibilidades DURA
 * de 6b son ese dato).
 *
 * <p>Fixture (validado por enumeración en diseño): MAT8 con 2 clases en un día
 * de 5 tramos (pos 1..5), vetado en pos 2 y pos 4. Posiciones disponibles:
 * {1,3,5}. Colocaciones factibles del par y su coste en ventanas:
 * <ul>
 *   <li>{1,3} → 1 ventana (ÓPTIMO)</li>
 *   <li>{3,5} → 1 ventana (ÓPTIMO)</li>
 *   <li>{1,5} → 3 ventanas (factible pero PEOR)</li>
 * </ul>
 * El óptimo es 1 y es estrictamente positivo; existe una alternativa factible
 * más cara (3). Un optimizador que solo buscara factibilidad podría devolver
 * {1,5} con 3 ventanas; minimizar obliga a 1. Eso es lo que este test prueba,
 * y que 6a no podía probar.
 *
 * <p>El óptimo se asevera vía {@link VerificadorSolucion#contarVentanasProfesor}
 * —recomputo independiente de OR-Tools sobre la solución devuelta— porque
 * {@link SolverHorario#resolverOptimizando} no expone el valor del objetivo
 * (decisión 2a). Aseverar sobre la solución concreta es más fuerte que leer el
 * objetivo del solver: comprueba que la solución DEVUELTA tiene 1 ventana, no
 * que el solver creyera tenerla.
 *
 * <p>Un solo término en el objetivo (ventanas, peso > 0): las cotas tensadas
 * siguen siendo válidas (D17 no se activa hasta que entre un segundo término).
 */
class SolverHorarioOroFuerteVentanasTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void elOptimizadorAlcanzaElOptimoPositivoForzadoPorElVeto() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6b-oro-fuerte-ventana-inevitable.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        Map<Profesor, Integer> ventanas = verificador.contarVentanasProfesor(problema, sol);
        Profesor mat8 = problema.profesores().stream()
                .filter(p -> p.codigo().equals("MAT8")).findFirst().orElseThrow();

        // Óptimo determinista = 1. El optimizador debe rechazar la alternativa
        // factible de 3 ventanas ({1,5}); si devolviera 3, esto fallaría.
        assertThat(ventanas.get(mat8))
                .as("hueco inevitable: el óptimo es exactamente 1 ventana, no 0 ni 3")
                .isEqualTo(1);
    }

    @Test
    void sinElVetoElOptimoEsCero_discriminacion() throws Exception {
        // Mismo problema sin restriccionesHorarias: MAT8 puede poner sus 2 clases
        // contiguas -> 0 ventanas. Demuestra que el hueco del caso anterior lo
        // causa el veto, no la estructura del problema.
        ProblemaHorario problema = cargar("/fixtures/problema-6b-oro-fuerte-sin-veto.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        Map<Profesor, Integer> ventanas = verificador.contarVentanasProfesor(problema, sol);
        Profesor mat8 = problema.profesores().stream()
                .filter(p -> p.codigo().equals("MAT8")).findFirst().orElseThrow();

        assertThat(ventanas.get(mat8))
                .as("sin veto, el óptimo de ventanas es 0 (clases contiguas)")
                .isEqualTo(0);
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