package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5 — OPTIMIZACIÓN A ESCALA (Bloque 14): primera medición de
 * {@link SolverHorario#resolverOptimizando} sobre el INSTITUTO COMPLETO. Reutiliza
 * el mismo fixture que cerró los criterios 1-2 en factibilidad pura
 * ({@code problema-5-fusion-instituto-completo.json}, 26 grupos, S36/Bloque 13),
 * sin tocarlo: el cambio es el RÉGIMEN (objetivo vs. factibilidad), no el dato.
 *
 * <p>POR QUÉ ESTE BLOQUE. El Bloque 13 midió {@code resolver} (factibilidad pura,
 * sin objetivo): 269,4 s, FACTIBLE, 0 duras. Los tres términos blandos del criterio
 * 3 (ventanas 6a, indisponibilidad blanda 6c, consecutivas 6d-c) están validados
 * solo en fixtures de DISCRIMINACIÓN, en aislamiento. Lo que ningún bloque ha medido
 * es {@code resolverOptimizando} A ESCALA. La deuda D23 (curva de coste NO LINEAL:
 * ×78 en tiempo por ×1,53 en grupos) advierte que el régimen de optimización parte
 * de esos 269 s y SUBE, con riesgo de que el criterio 1 (&lt; 10 min) deje de
 * cumplirse al optimizar. Este test produce el dato que D23 reclama.
 *
 * <p>QUÉ MIDE (y qué no). Mide: (a) el TIEMPO wall-clock de optimizar a escala;
 * (b) el VALOR de cada término blando RECOMPUTADO de forma independiente sobre la
 * solución, vía los tres gemelos del {@link VerificadorSolucion}; (c) 0 duras
 * (no-regresión). NO mide el objetivo interno de CP-SAT ni distingue OPTIMAL de
 * FEASIBLE: {@code resolverOptimizando} devuelve una {@link SolucionHorario} pelada
 * (decisión 2a, ver Javadoc de {@link SolverHorario}). Por D23 lo esperable es que
 * CP-SAT agote el límite buscando óptimo y devuelva FEASIBLE; el valor recomputado
 * es real (cuenta sobre la solución concreta), pero su interpretación es ciega al
 * estado del solver. Exponer estado/objetivo es un bloque posterior (tocaría
 * {@code src/main}).
 *
 * <p>DIFERENCIA DE CONTRATO CON EL TEST DE FACTIBILIDAD (Bloque 13). En
 * factibilidad pura, CP-SAT para en cuanto encuentra una solución (no hay nada que
 * optimizar) y el tiempo medido es pequeño frente al límite, por eso aquel test
 * asevera {@code tiempo < 600}. En optimización, CP-SAT NO para al hallar factible:
 * agota el límite mejorando el objetivo. Consumir el límite NO es regresión: es el
 * comportamiento esperado del modo optimización. Por eso este test NO asevera
 * {@code tiempo < 600}; asevera que termina dentro del {@code @Timeout} con solución
 * factible y 0 duras, y REGISTRA tiempo + términos para alimentar la decisión de D23.
 *
 * <p>INDISPONIBILIDAD BLANDA = 0 POR CONSTRUCCIÓN. El fixture del instituto completo
 * NO incluye {@code restriccionesHorarias} (las disponibilidades reales del
 * profesorado no se infieren del PDF; hay que pedírselas al centro). El mecanismo
 * está, el dato no: {@code contarPenalizacionIndisponibilidadBlanda} devuelve 0
 * porque no hay restricciones BLANDA que iterar. No es un término roto; es la
 * ausencia del dato. Meter disponibilidades reales endurece el problema y es alcance
 * de un bloque posterior.
 */
@Tag("escala")
class SolverHorarioOptimizacionInstitutoCompletoTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-instituto-completo.json";

    /** Misma red de 600 s que el linaje de factibilidad; aquí el solver la AGOTA por diseño. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Optimización instituto completo 26 grupos (criterio 3 a escala): factible, 0 duras, tiempo y términos blandos medidos")
    @Timeout(660)
    void optimizacionInstitutoCompleto() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset (idéntico al Bloque 13: mismo fixture).
        assertThat(problema.grupos())
                .as("26 grupos: 17 ESO + 4 1ºBach + 3 2ºBach + 1 1ºFPB + 1 2ºFPB")
                .hasSize(26);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("341 subgrupos: 232 + 65 + 42 + 1 + 1")
                .hasSize(341);
        assertThat(problema.actividades())
                .as("229 actividades: 155 + 30 + 23 + 11 + 10")
                .hasSize(229);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolverOptimizando(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Términos blandos RECOMPUTADOS de forma independiente sobre la solución.
        // Con PESO_VENTANAS = PESO_INDISP_BLANDA = PESO_CONSECUTIVAS = 1 (D21), el
        // conteo sin ponderar es el valor real de cada término hoy.
        VerificadorSolucion verificador = new VerificadorSolucion();
        Map<Profesor, Integer> ventanasPorProfesor =
                verificador.contarVentanasProfesor(problema, solucion);
        int ventanas = ventanasPorProfesor.values().stream().mapToInt(Integer::intValue).sum();
        int indispBlanda =
                verificador.contarPenalizacionIndisponibilidadBlanda(problema, solucion);
        int consecutivas =
                verificador.contarPenalizacionConsecutivasProfesor(problema, solucion);

        System.out.printf(
                "[OPT-INSTITUTO] 26 grupos: optimización en %.3f s (límite %.0f s) | "
                        + "ventanas=%d, indispBlanda=%d (sin dato), consecutivas=%d%n",
                segundos, MAX_SEGUNDOS, ventanas, indispBlanda, consecutivas);

        // Contrato de optimización: termina dentro del @Timeout con solución
        // factible (un INFEASIBLE/timeout sin solución habría lanzado
        // HorarioInfactibleException antes de aquí). NO se asevera tiempo < 600:
        // optimizar agota el límite por diseño (ver Javadoc de la clase).

        // No-regresión dura: la solución óptima/mejor-hallada respeta todas las
        // restricciones duras (red independiente del solver).
        var verificacion = verificador.verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();

        // Indisponibilidad blanda: 0 por construcción (el fixture no trae
        // restriccionesHorarias). Se asevera para fijar el hecho, no como calidad.
        assertThat(indispBlanda)
                .as("sin restriccionesHorarias en el fixture, la indisponibilidad blanda es 0")
                .isZero();

        // Los otros dos términos son ≥ 0 por definición; se registran, no se
        // umbralizan (el umbral de "calidad comparable" del criterio 3 exige datos
        // del centro, decisión consciente). Aserción mínima de cordura.
        assertThat(ventanas).as("ventanas ≥ 0").isGreaterThanOrEqualTo(0);
        assertThat(consecutivas).as("consecutivas ≥ 0").isGreaterThanOrEqualTo(0);
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioOptimizacionInstitutoCompletoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
