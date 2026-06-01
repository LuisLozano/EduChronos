package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifica la restriccion dura de no-solape por GRUPO introducida en Fase 3
 * (commit 1). Esta restriccion traslada al solver la invariante I1: dos
 * sesiones que toquen al mismo {@code GrupoAdministrativo} no pueden caer en
 * el mismo tramo, salvo que pertenezcan a la misma actividad (caso de los
 * desdobles/agrupamientos, cuyo intervalo se anade una sola vez por grupo).
 *
 * <p>En Fase 2 esta restriccion no existia: todos los subgrupos eran
 * {@code *-Completo} (uno por grupo) y el no-solape por SUBGRUPO bastaba.
 * Al partir un grupo en varios subgrupos (Fase 3), el no-solape por subgrupo
 * deja de ver "1A-Completo" y "1A-Sub1" como el mismo grupo, y hace falta el
 * no-solape por grupo para impedir que el solver los apile en el mismo tramo.
 *
 * <p>Los dos fixtures estan disenados para que la restriccion de GRUPO sea la
 * UNICA que decide la factibilidad: profesores distintos, aulas fijas
 * distintas y subgrupos distintos en cada actividad, de modo que ni el
 * no-solape de profesor, ni el de aula, ni el de subgrupo, ni la distribucion
 * por dia intervienen.
 */
class RestriccionNoSolapeGrupoTest {

    private static ProblemaHorario cargar(String fixture) throws IOException {
        String ruta = "/fixtures/" + fixture;
        try (InputStream in = RestriccionNoSolapeGrupoTest.class.getResourceAsStream(ruta)) {
            assertThat(in)
                    .as("fixture en el classpath: " + ruta)
                    .isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    /** Tramo asignado a la (unica) instancia de una actividad por su codigo. */
    private static Tramo tramoDe(ProblemaHorario problema,
                                 SolucionHorario solucion,
                                 String codigoActividad) {
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (inst.actividad().codigo().equals(codigoActividad)) {
                return solucion.tramoDeInstancia(inst)
                        .orElseThrow(() -> new AssertionError(
                                "instancia sin colocar: " + codigoActividad));
            }
        }
        throw new AssertionError("actividad no encontrada: " + codigoActividad);
    }

    @Test
    @Timeout(60)
    void dosActividadesDelMismoGrupoCaenEnTramosDistintos() throws IOException {
        ProblemaHorario problema = cargar("problema-noSolapeGrupo-factible.json");

        // Punto de ajuste: nombre del metodo de resolucion de la fachada
        // publica SolverHorario. Si no se llama 'resolver', es el unico cambio
        // pendiente (no se dispuso de SolverHorario.java al escribir el test).
        SolucionHorario solucion = new SolverHorario().resolver(problema);

        // Verificacion independiente del solver: 0 violaciones de las
        // restricciones duras que VerificadorSolucion comprueba.
        ResultadoVerificacion verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.violaciones())
                .as("la solucion factible no debe violar ninguna restriccion dura")
                .isEmpty();

        // Criterio de la fase: las dos actividades que tocan 1A estan en
        // tramos distintos.
        Tramo tramoX = tramoDe(problema, solucion, "Act-X");
        Tramo tramoY = tramoDe(problema, solucion, "Act-Y");
        assertThat(tramoX)
                .as("Act-X y Act-Y tocan 1A: deben ocupar tramos distintos")
                .isNotEqualTo(tramoY);
    }

    @Test
    @Timeout(60)
    void mismoGrupoEnUnUnicoTramoEsInfactible() throws IOException {
        ProblemaHorario problema = cargar("problema-noSolapeGrupo-infactible.json");

        // Con un solo tramo, las dos actividades que tocan 1A no se pueden
        // separar: la restriccion de grupo hace el problema infactible.
        // Si la restriccion NO existiera, el solver devolveria FEASIBLE
        // apilandolas en el unico tramo. Este contra-test es lo que demuestra
        // que la restriccion muerde.
        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }
}