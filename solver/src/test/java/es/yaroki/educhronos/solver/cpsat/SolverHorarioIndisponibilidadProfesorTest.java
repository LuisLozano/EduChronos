package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Verifica que la indisponibilidad horaria DURA del profesorado (Fase 5,
 * Bloque 6b) muerde en el modelo CP-SAT, en modo FACTIBILIDAD PURA
 * ({@link SolverHorario#resolver}).
 *
 * <p>Dos propiedades distintas, más una prueba de discriminación ejecutada:
 * <ul>
 *   <li>REDIRIGE: con una sola instancia y un tramo vetado, la instancia se
 *       coloca forzosamente en el tramo no vetado.</li>
 *   <li>INFACTIBLE: dos instancias del mismo profesor (no-solape S1/S9) y un
 *       solo tramo restante tras el veto → palomar → INFEASIBLE.</li>
 *   <li>DISCRIMINA: el MISMO problema del caso infactible, pero SIN la sección
 *       de restricciones, es FACTIBLE. La infactibilidad depende causalmente
 *       del veto, no de un artefacto del fixture (patrón S20/S22).</li>
 * </ul>
 *
 * <p>El objetivo (ventanas) NO entra aquí: la indisponibilidad es DURA y vive en
 * {@code construir()}, así que actúa también en factibilidad pura. La prueba de
 * oro fuerte de ventanas (óptimo > 0 por hueco inevitable) es el turno siguiente.
 */
class SolverHorarioIndisponibilidadProfesorTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    // Límite bajo y semilla fija: problemas diminutos, resolución inmediata.
    private final SolverHorario solver = new SolverHorario(10.0, 42);

    @Test
    void elVetoRedirigeLaInstanciaAlTramoNoVetado() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-6b-indisponibilidad-redirige.json");

        SolucionHorario sol = solver.resolver(problema);

        ActividadInstancia inst = instanciaDe(problema, "Mat-1A");
        Tramo asignado = sol.tramoDeInstancia(inst).orElseThrow();
        assertThat(asignado.codigo())
                .as("MAT8 vetado en LUN-1: su unica instancia debe caer en LUN-2")
                .isEqualTo("LUN-2");
    }

    @Test
    void elVetoVuelveInfactibleElPalomarDeProfesor() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-6b-indisponibilidad-infactible.json");

        assertThatThrownBy(() -> solver.resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class)
                .hasMessageContaining("INFEASIBLE");
    }

    @Test
    void sinElVetoElMismoProblemaEsFactible_discriminacion() throws Exception {
        // Mismo problema que el infactible, pero sin restriccionesHorarias.
        ProblemaHorario problema =
                cargar("/fixtures/problema-6b-indisponibilidad-factible-sin-veto.json");

        SolucionHorario sol = solver.resolver(problema);

        // Las dos instancias caen en tramos distintos (S1/S9), ambas validas.
        ActividadInstancia x = instanciaDe(problema, "Mat-1A-X");
        ActividadInstancia y = instanciaDe(problema, "Mat-1A-Y");
        assertThat(sol.tramoDeInstancia(x).orElseThrow())
                .as("sin veto, el problema es factible y las instancias se separan")
                .isNotEqualTo(sol.tramoDeInstancia(y).orElseThrow());
    }

    private ProblemaHorario cargar(String ruta) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("fixture no encontrado: " + ruta);
            }
            return loader.cargar(in);
        }
    }

    private static ActividadInstancia instanciaDe(ProblemaHorario problema, String codActividad) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codActividad))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no expandida: " + codActividad));
    }
}