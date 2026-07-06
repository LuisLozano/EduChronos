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
 * Tests del pin de instancia a tramo (Fase 8, Bloque 8.2a): el bloqueo manual de
 * una {@link es.yaroki.educhronos.solver.domain.SesionBloqueada} fija el tramo de
 * su instancia como restricción DURA. Cierra el mecanismo del criterio 5 de Fase 3.
 *
 * <p>Fixtures SINTÉTICOS mínimos (2 tramos LUN-1/LUN-2, una o dos actividades de una
 * plaza), linaje DISCRIMINACIÓN, validados por ENUMERACIÓN antes de correr (S25).
 * El pin de aula está DIFERIDO a 8.2b: aquí solo se pina el tramo.
 */
class SolverHorarioPinTramoTest {

    /**
     * Pin respetado. Enumeración: la instancia {@code A#1} (una actividad, una
     * plaza, sin más restricciones) tiene dominio {@code {LUN-1, LUN-2}} — ambos
     * tramos son factibles y óptimos en factibilidad pura. El pin {@code A#1→LUN-2}
     * reduce el dominio a un único valor: la única solución posible coloca A#1 en
     * LUN-2. El gemelo sin pin ({@link #sinPin_elSolverEsLibre}) demuestra que el
     * problema por sí solo NO fuerza ese tramo.
     */
    @Test
    void pinRespetado_laInstanciaCaeEnElTramoPinado() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2a-pin-respetado.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(tramoDe(problema, solucion, "A", 1)).isEqualTo("LUN-2");
        assertThat(new VerificadorSolucion().contarBloqueosViolados(problema, solucion))
                .isZero();
    }

    /**
     * Discriminación: MISMO problema SIN la sección {@code bloqueos}. El solver es
     * libre de colocar {@code A#1} en cualquiera de los dos tramos; el único
     * requisito verificable sin depender de la heurística es que la instancia queda
     * colocada y no hay bloqueos que violar (lista vacía ⇒ 0 violaciones). Que el
     * pin del test anterior fuerce LUN-2 y este no, es lo que la restricción aporta.
     */
    @Test
    void sinPin_elSolverEsLibre() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2a-pin-libre.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(problema.bloqueos()).isEmpty();
        assertThat(solucion.tramoDeInstancia(buscar(problema, "A", 1))).isPresent();
        assertThat(new VerificadorSolucion().contarBloqueosViolados(problema, solucion))
                .isZero();
    }

    /**
     * Pin de desdoble. Enumeración: la actividad {@code D} tiene DOS plazas (P1 con
     * MAT8/A5/S1, P2 con MAT9/A6/S2) que son la MISMA instancia {@code D#1} y por
     * tanto comparten {@code tramoIndex}. El pin {@code D#1→LUN-2} fija ese índice
     * compartido: las dos plazas caen simultáneas en LUN-2, sin iterar plazas. (Un
     * pin a nivel de instancia no puede separar las plazas de un desdoble.)
     */
    @Test
    void pinDesdoble_lasDosPlazasCaenSimultaneasEnElTramoPinado() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2a-pin-desdoble.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(tramoDe(problema, solucion, "D", 1)).isEqualTo("LUN-2");
        assertThat(new VerificadorSolucion().contarBloqueosViolados(problema, solucion))
                .isZero();
    }

    /**
     * Pin infactible (palomar). Enumeración: {@code A#1} y {@code B#1} comparten el
     * profesor MAT8 y el subgrupo S, así que el no-solape exige que caigan en tramos
     * DISTINTOS. Sin pines, {@code {A→LUN-1, B→LUN-2}} es factible. Los dos pines
     * {@code A#1→LUN-1} y {@code B#1→LUN-1} las fuerzan al MISMO tramo → viola el
     * no-solape → no hay solución factible. El pin es la causa de la infactibilidad,
     * y el solver debe reportarla como {@link HorarioInfactibleException}, no como un
     * silencio.
     */
    @Test
    void pinInfactible_lanzaHorarioInfactibleException() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2a-pin-infactible.json");

        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }

    // --------------------------------------------------------------- helpers

    private ProblemaHorario cargar(String ruta) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ruta)) {
            assertThat(in).as("fixture %s en el classpath de test", ruta).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private ActividadInstancia buscar(ProblemaHorario problema, String codActividad, int indice) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codActividad) && i.indice() == indice)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No existe la instancia " + codActividad + "#" + indice));
    }

    private String tramoDe(ProblemaHorario problema, SolucionHorario solucion,
                           String codActividad, int indice) {
        return solucion.tramoDeInstancia(buscar(problema, codActividad, indice))
                .map(Tramo::codigo)
                .orElseThrow(() -> new AssertionError(
                        "La instancia " + codActividad + "#" + indice + " no quedó colocada"));
    }
}
