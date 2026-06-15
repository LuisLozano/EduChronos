package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5 — medición de escala: el solver resuelve 1ºESO + 2ºESO (7 grupos
 * reales del instituto: 4 de 1º + 3 de 2º), dataset construido desde el volcado
 * fiel y verificado plaza a plaza contra él.
 *
 * <p>Objetivo de este test: ATACAR EL CRITERIO 1 DE FASE 5 (el solver termina en
 * menos de 10 minutos para el instituto completo) midiendo el tiempo sobre un
 * subconjunto creciente, y el CRITERIO 2 (cero restricciones duras violadas).
 * No es un test de discriminación: no aísla una restricción, acumula volumen.
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver}, que
 * incluye construcción del modelo + solve + extracción (el tiempo que el
 * usuario percibe). El solver corre en modo factibilidad pura (sin objetivo):
 * lo medido es tiempo hasta PRIMERA solución factible, no hasta óptimo. La
 * semilla fija (42) hace la medición reproducible.
 *
 * <p>El {@code @Timeout} de 660 s actúa como red: si el solver no termina en
 * ~11 min, el test falla (señal de Fase 5: "no converge tras 15 min").
 */
class SolverHorarioEscalaInstitutoTest {

    private static final String FIXTURE = "/fixtures/problema-5-escala-instituto.json";

    /** Límite del solver: 600 s = 10 min (criterio 1 de Fase 5). */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName("Escala 1º+2º ESO (7 grupos): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void escala1y2ESO() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset cargado (cuadra con el fixture verificado).
        assertThat(problema.grupos()).as("7 grupos: 4 de 1º + 3 de 2º").hasSize(7);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Criterio 1 de Fase 5: < 10 minutos (el solver internamente ya está
        // capado a 600 s; si hubiera agotado el límite sin factible, resolver()
        // habría lanzado HorarioInfactibleException y el test fallaría antes).
        System.out.printf(
                "[ESCALA] 1º+2º ESO (7 grupos): solución factible en %.3f s (límite %.0f s)%n",
                segundos, MAX_SEGUNDOS);
        assertThat(segundos)
                .as("tiempo hasta primera solución factible < 600 s (criterio 1 Fase 5)")
                .isLessThan(MAX_SEGUNDOS);

        // Criterio 2 de Fase 5: cero restricciones duras violadas.
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioEscalaInstitutoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
