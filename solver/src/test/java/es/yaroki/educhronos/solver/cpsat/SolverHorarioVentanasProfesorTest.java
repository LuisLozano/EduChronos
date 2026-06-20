package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5, Bloque 6a — función objetivo: penalización de ventanas del profesorado.
 *
 * <p>Fixture de DISCRIMINACIÓN (no de escala): un día, 5 tramos lectivos, un
 * grupo G1 con 5 sesiones (P1 imparte 3, P2 imparte 2). Como las 5 tocan G1, el
 * no-solape de grupo obliga a que ocupen los 5 tramos: toda solución factible es
 * una permutación. El espacio (120 permutaciones, enumerado al diseñar el
 * fixture) contiene soluciones con 0 ventanas (P1 contiguo, P2 contiguo) y con
 * hasta 3 ventanas (P1 y P2 intercalados). El óptimo es 0 y es alcanzable.
 *
 * <p><b>Contrato de la comprobación de oro (decisión 6a):</b> con objetivo se
 * asierta {@code ventanas == 0} de forma determinista (el óptimo es 0 y CP-SAT
 * lo prueba en milisegundos en un fixture de 120 soluciones). El comportamiento
 * SIN objetivo —que puede devolver una solución con 0..3 ventanas según el
 * reloj— NO se asierta automáticamente: sería flaky, porque sin minimizar la
 * holgura es intrínseca y el solver podría dar 0 por azar. Se verifica a mano
 * cambiando {@code resolverOptimizando} por {@code resolver} y observando que el
 * conteo deja de ser fiablemente 0. Esta es la diferencia con la comprobación de
 * oro de S9 (donde comentar la restricción volvía factible un infactible, algo
 * determinista); aquí el objetivo no fuerza ventanas, solo las evita.
 */
class SolverHorarioVentanasProfesorTest {

    private ProblemaHorario cargar(String recurso) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Fixture no encontrado: " + recurso);
            }
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private Profesor profesor(ProblemaHorario p, String codigo) {
        return p.profesores().stream()
                .filter(pr -> pr.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Profesor no encontrado: " + codigo));
    }

    @Test
    @Timeout(60)
    void conObjetivoElSolverEliminaLasVentanasDelProfesorado() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-6-ventanas-profesor.json");

        SolucionHorario solucion = new SolverHorario().resolverOptimizando(problema);

        // Red de seguridad: la solución sigue siendo dura-válida.
        ResultadoVerificacion duras = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(duras.esValida())
                .as("la solución optimizada no debe violar restricciones duras: %s",
                        duras.violaciones())
                .isTrue();

        // Recompute independiente de ventanas (no se fía del solver).
        Map<Profesor, Integer> ventanas =
                new VerificadorSolucion().contarVentanasProfesor(problema, solucion);

        assertThat(ventanas.get(profesor(problema, "P1")))
                .as("P1 sin ventanas tras optimizar")
                .isZero();
        assertThat(ventanas.get(profesor(problema, "P2")))
                .as("P2 sin ventanas tras optimizar")
                .isZero();

        int total = ventanas.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).as("ventanas totales en el óptimo").isZero();
    }

    /**
     * Comprobación de oro (Opción I, decisión registrada en S24): demuestra que
     * el contador de ventanas SÍ detecta huecos cuando los hay, descartando el
     * fallo "el contador siempre devuelve 0" (que dejaría pasar el test anterior
     * de forma espuria). Construye a mano una solución dura-válida con ventanas
     * conocidas y verifica el conteo exacto.
     *
     * <p>NO ejecuta el solver: prueba el verificador de forma aislada. La pieza
     * que falta —que el solver minimice de verdad cuando el óptimo es > 0— no es
     * construible sin indisponibilidades de profesor (dato de Fase 5, Bloque 6b),
     * donde vivirá la comprobación de oro fuerte estilo S9.
     *
     * <p>Colocación: ACT-A→L1, ACT-B→L3, ACT-C→L4 (P1 ocupa pos {1,3,4} → 1
     * ventana en pos 2); ACT-D→L2, ACT-E→L5 (P2 ocupa pos {2,5} → 2 ventanas en
     * pos 3 y 4). Total 3. Es dura-válida: las 5 caen en tramos distintos.
     */
    @Test
    void elContadorDetectaVentanasEnUnaSolucionConHuecos() throws Exception {
        ProblemaHorario problema = cargar("/fixtures/problema-6-ventanas-profesor.json");

        Map<String, Tramo> tramoPorCodigo = new LinkedHashMap<>();
        for (Tramo t : problema.tramos()) {
            tramoPorCodigo.put(t.codigo(), t);
        }

        // Colocación manual por código de actividad (todas con índice 1, reps=1).
        Map<String, String> colocacion = new LinkedHashMap<>();
        colocacion.put("ACT-A", "L1");
        colocacion.put("ACT-B", "L3");
        colocacion.put("ACT-C", "L4");
        colocacion.put("ACT-D", "L2");
        colocacion.put("ACT-E", "L5");

        Map<ActividadInstancia, Tramo> asignaciones = new LinkedHashMap<>();
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            String codTramo = colocacion.get(inst.actividad().codigo());
            assertThat(codTramo)
                    .as("toda instancia del fixture debe tener colocación manual: %s",
                            inst.actividad().codigo())
                    .isNotNull();
            asignaciones.put(inst, tramoPorCodigo.get(codTramo));
        }

        SolucionHorario manual = new SolucionHorario(asignaciones);

        // La colocación es dura-válida (red de seguridad).
        ResultadoVerificacion duras = new VerificadorSolucion().verificar(problema, manual);
        assertThat(duras.esValida())
                .as("la colocación manual debe ser dura-válida: %s", duras.violaciones())
                .isTrue();

        Map<Profesor, Integer> ventanas =
                new VerificadorSolucion().contarVentanasProfesor(problema, manual);

        assertThat(ventanas.get(profesor(problema, "P1")))
                .as("P1 ocupa pos {1,3,4} → 1 ventana").isEqualTo(1);
        assertThat(ventanas.get(profesor(problema, "P2")))
                .as("P2 ocupa pos {2,5} → 2 ventanas").isEqualTo(2);

        int total = ventanas.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).as("ventanas totales de la colocación con huecos").isEqualTo(3);
    }
}