package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Bloque 15a: valida el canal de detalle de la optimización
 * ({@link SolverHorario#resolverOptimizandoConDetalle}), que expone estado del
 * solver, objetivo y cota inferior — prerrequisito para medir el warm-start a
 * escala (deuda D23).
 *
 * <p>La aserción clave es de CONCORDANCIA: el valor del objetivo que devuelve
 * CP-SAT debe coincidir con el recuento independiente del
 * {@link VerificadorSolucion}. No es una segunda forma de medir lo mismo: ata el
 * canal nuevo (objetivo de OR-Tools) a la autoridad ya existente (recomputo
 * sobre el dominio, sin OR-Tools). Si algún día divergen, este test salta antes
 * de que el warm-start se mida con un objetivo en el que no se puede confiar.
 *
 * <p>Reutiliza los fixtures de oro fuerte de ventanas (S25, Bloque 6b), donde
 * el óptimo es conocido, determinista y —en el caso con veto— estrictamente
 * positivo (1). Pequeños: convergen a OPTIMAL probado en milisegundos, así que
 * el test afirma {@code OPTIMAL}, no un FEASIBLE por timeout.
 *
 * <p><b>Frontera de la concordancia objetivo↔verificador:</b> aquí el objetivo
 * de CP-SAT se compara contra la SUMA de ventanas de todos los profesores. Es
 * válido SOLO porque estos fixtures activan un único término blando (ventanas,
 * {@code PESO_VENTANAS == 1}); no es una fórmula general para recomponer el
 * objetivo con varios términos. Con más términos activos, la comparación
 * tendría que sumar también indisp-blanda y consecutivas, cada una por su peso.
 */
class SolverHorarioDetalleOptimizacionTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void detalleReportaOptimoProbadoYObjetivoPositivoConcordante() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6b-oro-fuerte-ventana-inevitable.json");

        ResultadoOptimizacion resultado = solver.resolverOptimizandoConDetalle(problema);

        // Fixture pequeño con óptimo conocido = 1: el solver debe PROBAR optimalidad.
        assertThat(resultado.estado())
                .as("fixture pequeño: el solver prueba optimalidad, no se corta por tiempo")
                .isEqualTo(CpSolverStatus.OPTIMAL);
        assertThat(resultado.esOptimo()).isTrue();

        // Concordancia: el objetivo de CP-SAT == suma de ventanas del verificador.
        // Un solo término activo (ventanas, peso 1), así que objetivo == nº ventanas.
        int ventanasVerificador = sumaVentanas(problema, resultado.solucion());
        assertThat(resultado.objetivo())
                .as("el objetivo de CP-SAT coincide con el recuento independiente del verificador")
                .isEqualTo((double) ventanasVerificador);

        // El óptimo de este fixture es estrictamente positivo (hueco inevitable).
        assertThat(resultado.objetivo())
                .as("hueco inevitable forzado por el veto: óptimo = 1")
                .isEqualTo(1.0);

        // Con OPTIMAL probado, la cota inferior alcanza el objetivo (gap nulo).
        assertThat(resultado.cotaInferior())
                .as("optimalidad probada: la cota inferior cierra contra el objetivo")
                .isEqualTo(resultado.objetivo());
    }

    @Test
    void detalleReportaOptimoCeroSinVeto() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-6b-oro-fuerte-sin-veto.json");

        ResultadoOptimizacion resultado = solver.resolverOptimizandoConDetalle(problema);

        assertThat(resultado.estado()).isEqualTo(CpSolverStatus.OPTIMAL);

        int ventanasVerificador = sumaVentanas(problema, resultado.solucion());
        assertThat(resultado.objetivo())
                .as("el objetivo de CP-SAT coincide con el recuento del verificador")
                .isEqualTo((double) ventanasVerificador);
        assertThat(resultado.objetivo())
                .as("sin veto, el óptimo de ventanas es 0 (clases contiguas)")
                .isEqualTo(0.0);
    }

    @Test
    void resolverOptimizandoClasicoDevuelveLaMismaSolucionQueElDetalle() throws Exception {
        // El método clásico delega en el de detalle: misma semilla, mismo modelo,
        // misma solución. Garantiza que la refactorización no cambió el contrato.
        ProblemaHorario problema =
                cargar("/fixtures/problema-6b-oro-fuerte-ventana-inevitable.json");

        SolucionHorario clasica = solver.resolverOptimizando(problema);
        ResultadoOptimizacion detalle = solver.resolverOptimizandoConDetalle(problema);

        assertThat(sumaVentanas(problema, clasica))
                .as("el método clásico y el de detalle producen el mismo coste")
                .isEqualTo(sumaVentanas(problema, detalle.solucion()));
    }

    private int sumaVentanas(ProblemaHorario problema, SolucionHorario solucion) {
        return verificador.contarVentanasProfesor(problema, solucion)
                .values().stream().mapToInt(Integer::intValue).sum();
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