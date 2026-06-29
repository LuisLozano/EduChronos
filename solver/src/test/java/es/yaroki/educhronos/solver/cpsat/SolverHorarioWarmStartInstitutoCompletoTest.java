package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5 — WARM-START A ESCALA (Bloque 15b, deuda D23). Mide si arrancar la
 * optimización del INSTITUTO COMPLETO desde una solución factible previa (hint)
 * mejora frente a arrancar en frío, a IGUAL presupuesto de optimización (opción A).
 *
 * <p>CONTEXTO (Bloque 14, S37). Optimizar el instituto completo NO converge: CP-SAT
 * agota el límite de 600 s y devuelve FEASIBLE (no OPTIMAL probado). Encontrar una
 * solución factible cuesta ~270-282 s (factibilidad pura, Bloque 13); el resto del
 * presupuesto se gasta mejorando sin cerrar gap. El warm-start (palanca de D23 que
 * NO degrada calidad) siembra esa factible como punto de partida, para que CP-SAT
 * no la redescubra y dedique el presupuesto íntegro a mejorar.
 *
 * <p>TRES CORRIDAS, MISMA MÁQUINA, MISMA EJECUCIÓN. (1) factibilidad pura para la
 * SEMILLA; (2) optimización SIN hint (baseline en frío, 600 s); (3) optimización CON
 * hint (warm-start, 600 s). Las tres en el mismo test a propósito: comparar contra
 * los números de otra sesión/máquina (S37) es justo la comparación engañosa que el
 * Bloque 14 enseñó a desconfiar (variación de hardware observada: ±13 s en
 * factibilidad pura). Coste ~24 min; por eso {@code @Tag("escala")}, fuera del
 * {@code mvn test} por defecto (D24).
 *
 * <p>QUÉ DECIDE EL RESULTADO (canal honesto = {@link ResultadoOptimizacion}, S38).
 * El warm-start VALE si, a igual tiempo: el estado mejora (FEASIBLE→OPTIMAL), o el
 * objetivo baja, o la cota inferior sube (gap más cerrado). Si los tres coinciden
 * con el baseline, el hint NO aportó: dato válido que cierra la palanca (c) de D23.
 * El test NO impone que el warm-start gane (no sabemos si lo hará): REGISTRA ambos
 * resultados y solo ASEVERA lo que debe cumplirse en todo caso — que la corrida con
 * hint sigue siendo factible y 0 duras (no-regresión), y que el warm-start no
 * EMPEORA el objetivo (un hint factible nunca debería dar peor que el frío a igual
 * presupuesto; si lo hiciera, sería señal de que algo va mal en el sembrado).
 */
@Tag("escala")
class SolverHorarioWarmStartInstitutoCompletoTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-instituto-completo.json";

    /** Presupuesto de OPTIMIZACIÓN (igual al baseline de S37); cada corrida de opt lo agota. */
    private static final double MAX_OPT_SEGUNDOS = 600.0;

    /** Presupuesto de la SEMILLA (factibilidad pura). Holgado sobre los ~270-282 s observados. */
    private static final double MAX_SEMILLA_SEGUNDOS = 600.0;

    private static final int SEMILLA_ALEATORIA = 42;

    @Test
    @DisplayName(
            "Warm-start instituto completo (D23): el hint no empeora el objetivo a igual presupuesto; con/sin se registran")
    @Timeout(1900)
    void warmStartInstitutoCompleto() throws Exception {
        ProblemaHorario problema = cargar();

        assertThat(problema.grupos()).as("26 grupos").hasSize(26);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos()).as("341 subgrupos").hasSize(341);
        assertThat(problema.actividades()).as("229 actividades").hasSize(229);

        // (1) SEMILLA: factibilidad pura.
        SolverHorario solverSemilla = new SolverHorario(MAX_SEMILLA_SEGUNDOS, SEMILLA_ALEATORIA);
        long s0 = System.nanoTime();
        SolucionHorario semilla = solverSemilla.resolver(problema);
        long s1 = System.nanoTime();
        double segSemilla = (s1 - s0) / 1_000_000_000.0;

        // (2) BASELINE en frío: optimización sin hint, mismo presupuesto.
        SolverHorario solverOpt = new SolverHorario(MAX_OPT_SEGUNDOS, SEMILLA_ALEATORIA);
        long b0 = System.nanoTime();
        ResultadoOptimizacion frio = solverOpt.resolverOptimizandoConDetalle(problema);
        long b1 = System.nanoTime();
        double segFrio = (b1 - b0) / 1_000_000_000.0;

        // (3) WARM-START: optimización con hint = semilla, mismo presupuesto.
        SolverHorario solverWarm = new SolverHorario(MAX_OPT_SEGUNDOS, SEMILLA_ALEATORIA);
        long w0 = System.nanoTime();
        ResultadoOptimizacion caliente = solverWarm.resolverOptimizandoConSemilla(problema, semilla);
        long w1 = System.nanoTime();
        double segWarm = (w1 - w0) / 1_000_000_000.0;

        System.out.printf(
                "[WARM-START] semilla(factib pura)=%.3f s | "
                        + "FRIO: estado=%s objetivo=%.1f cota=%.1f en %.3f s | "
                        + "CALIENTE: estado=%s objetivo=%.1f cota=%.1f en %.3f s%n",
                segSemilla,
                frio.estado(), frio.objetivo(), frio.cotaInferior(), segFrio,
                caliente.estado(), caliente.objetivo(), caliente.cotaInferior(), segWarm);

        // No-regresión dura sobre la solución CON hint (red independiente del solver).
        VerificadorSolucion verificador = new VerificadorSolucion();
        var verificacion = verificador.verificar(problema, caliente.solucion());
        assertThat(verificacion.esValida())
                .as("violaciones duras (warm-start): %s", verificacion.violaciones())
                .isTrue();

        // El warm-start NO debe EMPEORAR el objetivo a igual presupuesto: un hint
        // factible nunca debería rendir peor que el arranque en frío. Mejorarlo es
        // lo esperado/deseable, pero no se EXIGE (no sabemos si CP-SAT cerrará gap);
        // empeorarlo sí señalaría un sembrado mal hecho. Minimización: menor es mejor.
        assertThat(caliente.objetivo())
                .as("warm-start no empeora el objetivo frente al frío (frío=%.1f, caliente=%.1f)",
                        frio.objetivo(), caliente.objetivo())
                .isLessThanOrEqualTo(frio.objetivo());
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                     SolverHorarioWarmStartInstitutoCompletoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}