package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;

import com.google.ortools.sat.CpSolverStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de la poda de aulas candidatas (Fase 5, Bloque 16, palanca (b) de la
 * deuda D23). La poda recorta a {@code MAX_AULAS_PODA} las candidatas de una
 * plaza de cola larga (más de {@code UMBRAL_PODA_AULA} candidatas) y SOLO actúa
 * en el régimen de optimización ({@code construirConObjetivo} →
 * {@code resolverOptimizando}); en factibilidad pura ({@code resolver}) no.
 *
 * <p>El par de fixtures acota el comportamiento de la poda con atribución
 * perfecta (sin observar el conteo interno de variables, decisión "Opción A"):
 * <ul>
 *   <li><b>factible:</b> plaza con 12 candidatas (&gt;8) que la poda recorta a las
 *       8 primeras por código {@code A01..A08}; una plaza rival con {@code aulaFija
 *       A01} en el único tramo obliga a la podada a elegir en {@code A02..A08}.
 *       La poda no rompe: sigue factible y la elección cae dentro del recorte.</li>
 *   <li><b>oro/saturación:</b> 9 plazas mutuamente compatibles (sin grupo ni
 *       profesor común) en un único tramo, con 12 candidatas compartidas. Sin
 *       poda, 9 plazas en 12 aulas → factible. Con poda (8 aulas) → 9 plazas en 8
 *       aulas → INFEASIBLE por palomar. El contraste factibilidad-pura-FACTIBLE /
 *       optimización-INFEASIBLE prueba que la poda actúa de verdad.</li>
 * </ul>
 */
class SolverHorarioPodaAulaTest {

    @Test
    @DisplayName("Poda 12→8: sigue factible y el aula elegida está en el recorte A01..A08")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void podaMantieneFactibleYRespetaRecorte() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-poda-aula-factible.json");

        // Régimen de optimización: la poda actúa. El objetivo es 0 y demostrable
        // (una sola sesión por profesor, sin ventanas posibles), así que CP-SAT
        // devuelve OPTIMAL; se aceptan OPTIMAL y FEASIBLE por robustez.
        ResultadoOptimizacion resultado =
                new SolverHorario().resolverOptimizandoConDetalle(problema);

        assertThat(resultado.estado())
                .as("estado del solver con poda activa")
                .isIn(CpSolverStatus.OPTIMAL, CpSolverStatus.FEASIBLE);
        assertThat(new VerificadorSolucion().verificar(problema, resultado.solucion()).violaciones())
                .as("violaciones de restricciones duras con poda activa")
                .isEmpty();

        Aula elegida = aulaElegidaDe(problema, resultado.solucion(), "ActPodada", "ActPodada-P");
        // Recorte: 8 primeras por orden de código (lexicográfico == numérico con
        // padding A01..A12). A01 la ocupa la plaza fija, así que la podada elige
        // en A02..A08, todas dentro del recorte.
        assertThat(elegida.codigo())
                .as("el aula elegida tras la poda debe pertenecer al recorte A01..A08")
                .isIn("A02", "A03", "A04", "A05", "A06", "A07", "A08");

        // Refuerzo: sin poda (factibilidad pura) el problema ya era factible; la
        // poda no degradó un caso resoluble.
        assertThat(new SolverHorario().resolver(problema))
                .as("el mismo problema es factible también sin poda")
                .isNotNull();
    }

    @Test
    @DisplayName("Oro: sin poda factible, con poda (8<9) infactible por saturación")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void podaInsuficienteSaturaYEsInfactible() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-poda-aula-oro-saturacion.json");

        // Factibilidad pura: SIN poda, 9 plazas en 12 aulas (un tramo) → factible.
        // Atribución: el problema es resoluble; lo que rompe es exclusivamente la poda.
        SolucionHorario sinPoda = new SolverHorario().resolver(problema);
        assertThat(sinPoda).as("sin poda el problema es factible (9 plazas ≤ 12 aulas)").isNotNull();
        assertThat(new VerificadorSolucion().verificar(problema, sinPoda).violaciones())
                .as("sin poda, 0 violaciones duras").isEmpty();

        // Optimización: CON poda, 9 plazas en 8 aulas (un tramo) → palomar → infactible.
        assertThatThrownBy(() -> new SolverHorario().resolverOptimizando(problema))
                .as("con poda a 8 aulas, 9 plazas simultáneas no caben → infactible")
                .isInstanceOf(HorarioInfactibleException.class);
    }

    // ---- helpers (calcados de SolverHorarioAulaCandidataTest) ----

    private ProblemaHorario cargar(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s en classpath", path).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private Aula aulaElegidaDe(ProblemaHorario problema, SolucionHorario solucion,
                               String codigoActividad, String codigoPlaza) {
        Actividad actividad = problema.actividades().stream()
                .filter(a -> a.codigo().equals(codigoActividad))
                .findFirst()
                .orElseThrow(() -> new AssertionError("actividad no encontrada: " + codigoActividad));
        Plaza plaza = actividad.plazas().stream()
                .filter(p -> p.codigo().equals(codigoPlaza))
                .findFirst()
                .orElseThrow(() -> new AssertionError("plaza no encontrada: " + codigoPlaza));
        ActividadInstancia inst = new ActividadInstancia(actividad, 1);
        return solucion.aulaElegida(inst, plaza)
                .orElseThrow(() -> new AssertionError(
                        "sin aula elegida para " + codigoActividad + "/" + codigoPlaza));
    }
}
