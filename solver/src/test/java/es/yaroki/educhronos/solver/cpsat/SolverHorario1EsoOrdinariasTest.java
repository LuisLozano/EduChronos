package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de cierre del Bloque 6 de Fase 2. Dataset real de 1ºESO con las
 * sesiones ordinarias + co-docencia LCL de los 4 grupos (A, B, C, D),
 * según problema-1eso-ordinarias.json. Verifica que:
 * <ol>
 *   <li>El solver produce una solución factible (no lanza
 *       HorarioInfactibleException).</li>
 *   <li>La verificación independiente de la solución no encuentra
 *       violaciones de las cuatro restricciones duras de Fase 2:
 *       no solape de profesor, de aula, de subgrupo, y distribución
 *       por día para actividades DISTRIBUIDA.</li>
 * </ol>
 *
 * El test NO compara la colocación celda a celda contra el PDF: el
 * solver de Fase 2 es de factibilidad pura y devuelve UNA de las
 * muchas soluciones factibles.
 */
class SolverHorario1EsoOrdinariasTest {

    private static final String FIXTURE_PATH =
            "/fixtures/problema-1eso-ordinarias.json";

    @Test
    @DisplayName("Dataset 1ºESO ordinarias + LCL: factible y 0 violaciones")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void resuelveFactibleSinViolaciones() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
            assertThat(in)
                    .as("fixture %s en classpath", FIXTURE_PATH)
                    .isNotNull();
            problema = new ProblemaHorarioJsonLoader().cargar(in);
        }

        // Sanity check del dataset cargado
        assertThat(problema.actividades()).hasSize(36);
        assertThat(problema.tramos()).hasSize(30);
        assertThat(problema.subgrupos()).hasSize(4);
        assertThat(problema.profesores()).hasSize(16);

        // Resolver. Lanza HorarioInfactibleException si no factible.
        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(solucion)
                .as("el solver debe devolver una solución factible")
                .isNotNull();

        // Verificación independiente del resultado: 0 violaciones de las
        // restricciones duras de Fase 2.
        ResultadoVerificacion resultado =
                new VerificadorSolucion().verificar(problema, solucion);

        assertThat(resultado.violaciones())
                .as("violaciones de restricciones duras")
                .isEmpty();
    }
}