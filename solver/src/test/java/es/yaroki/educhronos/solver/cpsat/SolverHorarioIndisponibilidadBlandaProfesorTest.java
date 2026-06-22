package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * DISCRIMINACIÓN del término de indisponibilidad BLANDA del profesorado
 * (Fase 5, Bloque 6c, Turno A).
 *
 * <p>Demuestra que el solver EVITA un tramo vetado-blando cuando puede, eligiendo
 * la solución de menor coste entre varias factibles. Gemelo blando de la
 * discriminación de 6a/6b: donde la indisponibilidad DURA prohíbe el tramo, la
 * BLANDA solo lo penaliza, y el optimizador debe preferir no pagarla.
 *
 * <p>Fixture (validado por enumeración en diseño): P1 con 1 actividad NEUTRA de
 * 1 repetición en un día de 2 tramos (L1=pos1, L2=pos2). Restricción BLANDA:
 * P1 vetado-blando en L1. Espacio completo de soluciones:
 * <ul>
 *   <li>instancia=L1 → penalización 1 (cae en el tramo vetado-blando)</li>
 *   <li>instancia=L2 → penalización 0 (ÓPTIMO)</li>
 * </ul>
 * El óptimo es 0 y existe una alternativa factible más cara (1, instancia=L1).
 * Un solver que ignorara la blanda podría devolver L1; minimizar obliga a L2.
 *
 * <p>Qué NO prueba: minimización con óptimo estrictamente positivo (aquí la
 * blanda es evitable → óptimo 0). Eso es el Turno B (oro fuerte), donde la
 * blanda se hace inevitable.
 *
 * <p>El test asevera DOS cosas, no una: (a) la penalización recomputada es 0, y
 * (b) la instancia quedó en L2, no en L1. Solo (a) sería débil —un solver que
 * eligiera L2 por azar también daría 0—; (b) cierra el agujero comprobando la
 * colocación concreta que evita el tramo vetado. El recuento se hace vía
 * {@link VerificadorSolucion#contarPenalizacionIndisponibilidadBlanda},
 * independiente de OR-Tools, porque {@link SolverHorario#resolverOptimizando}
 * no expone el valor del objetivo (decisión 2a).
 *
 * <p>Términos aislados: 1 sola clase ⇒ 0 ventanas posibles, el término de
 * ventanas es idénticamente 0. El único coste es la blanda, así que el peso
 * relativo entre términos no afecta a lo que este test demuestra.
 */
class SolverHorarioIndisponibilidadBlandaProfesorTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();
    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    @Test
    void elOptimizadorEvitaElTramoVetadoBlandoCuandoPuede() throws Exception {
        ProblemaHorario problema =
                cargar("/fixtures/problema-6c-indisp-blanda-discriminacion.json");

        SolucionHorario sol = solver.resolverOptimizando(problema);

        // (a) penalización blanda recomputada = 0 (óptimo alcanzable).
        int penalizacion =
                verificador.contarPenalizacionIndisponibilidadBlanda(problema, sol);
        assertThat(penalizacion)
                .as("la blanda es evitable: el óptimo es 0, el solver no debe pagarla")
                .isEqualTo(0);

        // (b) la instancia quedó en L2 (pos 2), no en L1 (el tramo vetado-blando).
        // Cierra el agujero de (a): comprueba la colocación concreta que evita el
        // veto, no que el coste sea 0 por casualidad.
        ActividadInstancia instancia = Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals("ACT-A"))
                .findFirst().orElseThrow();
        Optional<Tramo> tramo = sol.tramoDeInstancia(instancia);
        assertThat(tramo).isPresent();
        assertThat(tramo.get().codigo())
                .as("el solver debe colocar la instancia en L2, evitando el vetado-blando L1")
                .isEqualTo("L2");
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