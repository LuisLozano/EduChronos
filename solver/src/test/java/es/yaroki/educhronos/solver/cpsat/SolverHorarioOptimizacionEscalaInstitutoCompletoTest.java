package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.ortools.sat.CpSolverStatus;
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
 * Fase 5 — OPTIMIZACIÓN A ESCALA sobre el INSTITUTO COMPLETO. Mide
 * {@link SolverHorario#resolverOptimizandoConDetalle} (vía de optimización SIN poda
 * de aulas; ver más abajo) sobre el fixture de 26 grupos
 * ({@code problema-5-fusion-instituto-completo.json}, S36/Bloque 13). Sustituye al
 * antiguo test de optimización del Bloque 14, leyendo además estado/objetivo/cota
 * vía {@link ResultadoOptimizacion} (canal de S38).
 *
 * <p>SIN PODA. Desde S42 {@code construirConObjetivo()} NO poda aulasCandidatas por
 * defecto: la poda de S41 (palanca b de D23) recortaba 25→8 candidatas en las 21
 * plazas de 2ºBach y se midió que ROMPE la factibilidad a escala (con poda, la vía de
 * optimización devolvía {@code UNKNOWN} en 600 s; sin poda devuelve {@code FEASIBLE}
 * 215 sobre el mismo fixture y máquina). El mecanismo de poda se conserva latente
 * (sobrecarga {@code ModeloCpSat#construirConObjetivo(boolean)}), apagado.
 *
 * <p>QUÉ MIDE. Estado del solver (OPTIMAL probado vs. FEASIBLE por timeout), objetivo
 * y cota inferior (el gap objetivo−cota hace interpretable una FEASIBLE cortada por
 * límite), tiempo wall-clock, y los términos blandos recomputados de forma
 * independiente por {@link VerificadorSolucion}. Por D23, lo esperable es que CP-SAT
 * agote el límite y devuelva FEASIBLE con gap grande (no converge a escala). Verde NO
 * significa "calidad cumplida": el criterio 3 exige datos del centro para umbralizar.
 *
 * <p>INDISPONIBILIDAD BLANDA = 0 por construcción: el fixture no incluye
 * {@code restriccionesHorarias} (no se infieren del PDF).
 */
@Tag("escala")
class SolverHorarioOptimizacionEscalaInstitutoCompletoTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-instituto-completo.json";

    /** Misma red de 600 s que el linaje de optimización; el solver la AGOTA por diseño. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Optimización instituto completo 26 grupos (criterio 3 a escala): factible, 0 duras; estado/objetivo/cota registrados")
    @Timeout(720)
    void optimizacionInstitutoCompleto() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset (mismo fixture que el Bloque 13/14).
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
        ResultadoOptimizacion resultado = solver.resolverOptimizandoConDetalle(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;
        SolucionHorario solucion = resultado.solucion();
        CpSolverStatus estado = resultado.estado();
        double objetivo = resultado.objetivo();
        double cota = resultado.cotaInferior();

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
                "[OPT-ESCALA] 26 grupos (sin poda): %.3f s (límite %.0f s) | estado=%s | "
                        + "objetivo=%.1f cota=%.1f gap=%.1f | "
                        + "ventanas=%d, indispBlanda=%d (sin dato), consecutivas=%d%n",
                segundos, MAX_SEGUNDOS, estado, objetivo, cota, objetivo - cota,
                ventanas, indispBlanda, consecutivas);

        // --- Aserciones de CONTRATO (no de umbral) ---

        // El solver devolvió solución utilizable (UNKNOWN/INFEASIBLE habría lanzado
        // HorarioInfactibleException antes de aquí).
        assertThat(estado)
                .as("estado del solver: óptimo probado o mejor-hallada por timeout")
                .isIn(CpSolverStatus.OPTIMAL, CpSolverStatus.FEASIBLE);

        // Sanity del solver: el objetivo nunca es menor que su cota inferior.
        assertThat(objetivo)
                .as("objetivo (%.1f) ≥ cota inferior (%.1f)", objetivo, cota)
                .isGreaterThanOrEqualTo(cota);

        // No-regresión dura: red independiente del solver.
        var verificacion = verificador.verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();

        // Indisponibilidad blanda: 0 por construcción (el fixture no trae
        // restriccionesHorarias). Se asevera para fijar el hecho, no como calidad.
        assertThat(indispBlanda)
                .as("sin restriccionesHorarias en el fixture, la indisponibilidad blanda es 0")
                .isZero();

        // Los otros dos términos se registran, no se umbralizan (el umbral del
        // criterio 3 exige datos del centro). Aserción mínima de cordura.
        assertThat(ventanas).as("ventanas ≥ 0").isGreaterThanOrEqualTo(0);
        assertThat(consecutivas).as("consecutivas ≥ 0").isGreaterThanOrEqualTo(0);
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                     SolverHorarioOptimizacionEscalaInstitutoCompletoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}