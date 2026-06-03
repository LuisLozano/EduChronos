package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del commit 2 de Fase 3: aulasCandidatas con intervalos opcionales.
 * El solver elige un aula entre las candidatas de cada plaza, garantizando
 * no-solape sobre el aula efectivamente elegida.
 *
 * <ul>
 *   <li>factible: una plaza con candidatas [A5,A6] y otra actividad con
 *       aulaFija A5 en el mismo (único) tramo; la candidata debe resolver a A6.</li>
 *   <li>mixta: un desdoble (una actividad, dos plazas, mismo tramo) con una
 *       plaza fija A5 y otra con candidatas [A5,A6]; la variable debe resolver
 *       a A6. Ejercita la rama de no-solape de aula que mezcla un intervalo
 *       fijo y uno opcional en la misma aula.</li>
 *   <li>infactible: dos actividades de grupos distintos en el único tramo,
 *       ambas con candidata única [A5]; colisión inevitable sobre el intervalo
 *       opcional de A5.</li>
 * </ul>
 */
class SolverHorarioAulaCandidataTest {

    @Test
    @DisplayName("Candidatas + aulaFija rival: elige A6 y 0 violaciones")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void candidatasEligeAulaLibre() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-aulaCandidata-factible.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);
        assertThat(solucion).as("solución factible").isNotNull();

        assertThat(new VerificadorSolucion().verificar(problema, solucion).violaciones())
                .as("violaciones de restricciones duras").isEmpty();

        Aula elegida = aulaElegidaDe(problema, solucion, "Act-Cand", "Act-Cand-P1");
        assertThat(elegida.codigo())
                .as("la candidata debe evitar A5 (ocupada por la plaza fija)")
                .isEqualTo("A6");
    }

    @Test
    @DisplayName("Desdoble fija+candidatas mismo tramo: variable elige A6, 0 violaciones")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void mixtaEnMismoTramoEligeAulaLibre() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-aulaCandidata-mixta.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);
        assertThat(solucion).as("solución factible").isNotNull();

        assertThat(new VerificadorSolucion().verificar(problema, solucion).violaciones())
                .as("violaciones de restricciones duras").isEmpty();

        Aula elegida = aulaElegidaDe(problema, solucion, "CyR-1A", "CyR-1A-Cand");
        assertThat(elegida.codigo())
                .as("la plaza variable debe evitar A5 (ocupada por la plaza fija del desdoble)")
                .isEqualTo("A6");
    }

    @Test
    @DisplayName("Dos candidatas únicas a la misma aula, mismo tramo: infactible")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void candidataUnicaCompartidaEsInfactible() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-aulaCandidata-infactible.json");

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }

    // ---- helpers ----

    private ProblemaHorario cargar(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s en classpath", path).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    /**
     * Aula que el solver eligió para la plaza {@code codigoPlaza} de la
     * actividad {@code codigoActividad}, en su instancia única (índice 1).
     * Falla la aserción si la actividad o la plaza no existen, o si no hay aula.
     */
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
