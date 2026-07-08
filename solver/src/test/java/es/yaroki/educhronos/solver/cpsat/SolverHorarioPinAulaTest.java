package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests del pin de AULA por plaza (Fase 8, Bloque 8.2b): el par (plaza, aula) de
 * una {@link es.yaroki.educhronos.solver.domain.SesionBloqueada} fija el aula elegida
 * de una plaza de aula variable como restricción DURA, además del tramo de 8.2a.
 *
 * <p>Fixtures SINTÉTICOS mínimos (2 tramos LUN-1/LUN-2, plazas de aula variable con
 * candidatas {A5,A6}), validados por ENUMERACIÓN antes de correr (S25).
 *
 * <p><b>Prueba ORO (D-F8.2b, criterio 5):</b> comentar la llamada a
 * {@code restriccionAulaBloqueada()} en {@code ModeloCpSat.construir()} hace caer
 * {@link #pinRespetado_laPlazaCaeEnElAulaPinada} y
 * {@link #pinDesdoble_lasDosPlazasCaenEnAulasDistintasPinadas}: sin la restricción el
 * solver es libre de elegir cualquier candidata y el aula pinada deja de estar forzada.
 */
class SolverHorarioPinAulaTest {

    /**
     * Pin de aula respetado. Enumeración: la plaza P de {@code A#1} tiene aula
     * variable con candidatas {@code {A5, A6}} y sin más restricciones el solver
     * puede elegir cualquiera. El pin {@code P→A6} reduce la elección a un valor: la
     * única solución posible coloca la plaza en A6. El gemelo sin pin
     * ({@link #sinPinDeAula_elSolverEsLibre}) demuestra que el problema por sí solo
     * NO fuerza ese aula.
     */
    @Test
    void pinRespetado_laPlazaCaeEnElAulaPinada() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2b-pin-aula-respetado.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(aulaDe(problema, solucion, "A", 1, "P")).isEqualTo("A6");
        assertThat(new VerificadorSolucion().contarAulasBloqueadasVioladas(problema, solucion))
                .isZero();
    }

    /**
     * Pin de aula en desdoble. Enumeración: la actividad {@code D} tiene dos plazas
     * de aula variable (P1 y P2, ambas con candidatas {@code {A5, A6}}), simultáneas
     * en el tramo pinado. El no-solape de aula (S2) exige aulas distintas; los pines
     * {@code P1→A5} y {@code P2→A6} las fijan a candidatas distintas, ambas
     * respetadas a la vez.
     */
    @Test
    void pinDesdoble_lasDosPlazasCaenEnAulasDistintasPinadas() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2b-pin-aula-desdoble.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(aulaDe(problema, solucion, "D", 1, "P1")).isEqualTo("A5");
        assertThat(aulaDe(problema, solucion, "D", 1, "P2")).isEqualTo("A6");
        assertThat(new VerificadorSolucion().contarAulasBloqueadasVioladas(problema, solucion))
                .isZero();
    }

    /**
     * Retrocompat 8.2a: un bloqueo de solo tramo ({@code aulasPinadas} vacío) sobre una
     * plaza de aula variable no restringe el aula; el solver es libre de elegir A5 o
     * A6 y no hay pines de aula que violar (mapa vacío ⇒ 0 violaciones).
     */
    @Test
    void sinPinDeAula_elSolverEsLibre() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-8-2b-pin-aula-sinpin.json");

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        assertThat(problema.bloqueos()).hasSize(1);
        assertThat(problema.bloqueos().get(0).aulasPinadas()).isEmpty();
        assertThat(solucion.aulaElegida(buscar(problema, "A", 1), plaza(problema, "A", "P")))
                .isPresent();
        assertThat(new VerificadorSolucion().contarAulasBloqueadasVioladas(problema, solucion))
                .isZero();
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

    private Plaza plaza(ProblemaHorario problema, String codActividad, String codPlaza) {
        return problema.actividades().stream()
                .filter(a -> a.codigo().equals(codActividad))
                .flatMap(a -> a.plazas().stream())
                .filter(p -> p.codigo().equals(codPlaza))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No existe la plaza " + codActividad + "/" + codPlaza));
    }

    private String aulaDe(ProblemaHorario problema, SolucionHorario solucion,
                          String codActividad, int indice, String codPlaza) {
        return solucion.aulaElegida(buscar(problema, codActividad, indice),
                        plaza(problema, codActividad, codPlaza))
                .map(Aula::codigo)
                .orElseThrow(() -> new AssertionError(
                        "La plaza " + codActividad + "#" + indice + "/" + codPlaza
                                + " no tiene aula asignada"));
    }
}
