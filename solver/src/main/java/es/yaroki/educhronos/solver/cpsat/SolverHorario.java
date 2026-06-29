package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import java.util.Objects;

/**
 * Punto de entrada del solver. Construye el modelo CP-SAT, lo resuelve en
 * modo factibilidad pura (sin función objetivo) y devuelve una
 * {@link SolucionHorario}.
 *
 * <p>Con un modelo sin objetivo ({@link #resolver}), CP-SAT devuelve
 * {@code OPTIMAL} en cuanto encuentra una solución factible (no hay nada que
 * optimizar) e {@code INFEASIBLE} si demuestra que no existe. La
 * {@code randomSeed} fija hace reproducible la solución concreta devuelta.
 *
 * <p>Con objetivo ({@link #resolverOptimizando}), {@code OPTIMAL} significa
 * optimalidad <i>probada</i> —puede no alcanzarse dentro del límite de tiempo—
 * y {@code FEASIBLE} es la mejor solución hallada al agotar el tiempo. Ambos se
 * aceptan; solo {@code INFEASIBLE}/desconocido lanzan excepción. El valor de la
 * función objetivo no se devuelve por {@link #resolverOptimizando} (decisión
 * 2a): lo recomputa de forma independiente
 * {@link VerificadorSolucion#contarVentanasProfesor}. Para razonar sobre la
 * calidad a escala (deuda D23), {@link #resolverOptimizandoConDetalle} sí
 * devuelve estado y objetivo en un {@link ResultadoOptimizacion}.
 */
public final class SolverHorario {

    static {
        // Carga los binarios nativos de OR-Tools. Debe ocurrir antes de
        // cualquier llamada a CpSolver#solve. Olvidarlo provoca
        // UnsatisfiedLinkError. Un bloque estático garantiza una sola carga.
        Loader.loadNativeLibraries();
    }

    private final double maxSegundos;
    private final int semilla;

    /** Configuración por defecto: 120 s de límite, semilla 42. */
    public SolverHorario() {
        this(120.0, 42);
    }

    public SolverHorario(double maxSegundos, int semilla) {
        this.maxSegundos = maxSegundos;
        this.semilla = semilla;
    }

    /**
     * Resuelve el problema.
     *
     * @throws HorarioInfactibleException si el solver no encuentra solución
     *         factible dentro del límite de tiempo, o si el modelo es inválido.
     */
    public SolucionHorario resolver(ProblemaHorario problema) {
        Objects.requireNonNull(problema, "problema no puede ser null");

        ModeloCpSat modelo = new ModeloCpSat(problema).construir();

        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(maxSegundos);
        solver.getParameters().setRandomSeed(semilla);

        CpSolverStatus estado = solver.solve(modelo.model());
        if (estado == CpSolverStatus.OPTIMAL || estado == CpSolverStatus.FEASIBLE) {
            return modelo.extraerSolucion(solver);
        }
        throw new HorarioInfactibleException(
                "El solver no encontró un horario factible. Estado CP-SAT: " + estado);
    }

    /**
     * Resuelve el problema en modo OPTIMIZACIÓN: aplica las restricciones duras
     * y minimiza la función objetivo de penalizaciones blandas (Fase 5,
     * Bloque 6a: ventanas del profesorado).
     *
     * <p>A diferencia de {@link #resolver}, aquí {@code maxSegundos} sí acota una
     * búsqueda de óptimo que puede no terminar: al agotarse, CP-SAT devuelve la
     * mejor solución encontrada ({@code FEASIBLE}). El contrato de retorno es el
     * mismo (una {@link SolucionHorario}); el valor del objetivo se recomputa con
     * {@link VerificadorSolucion}.
     *
     * @throws HorarioInfactibleException si no hay solución factible o el modelo
     *         es inválido.
     */
    public SolucionHorario resolverOptimizando(ProblemaHorario problema) {
        return resolverOptimizandoConDetalle(problema).solucion();
    }

    /**
     * Igual que {@link #resolverOptimizando} pero devuelve un
     * {@link ResultadoOptimizacion}: además de la solución, el estado del solver
     * ({@code OPTIMAL} = óptimo probado; {@code FEASIBLE} = mejor solución al
     * agotar el tiempo) y el valor del objetivo con su cota inferior.
     *
     * <p>Existe para razonar sobre la CALIDAD a escala (deuda D23): la vía
     * clásica no permite distinguir una optimalidad probada de un timeout, ni
     * medir el gap al óptimo. {@code resolverOptimizando} se mantiene intacto
     * (mismo contrato de retorno) delegando aquí y descartando el detalle.
     *
     * @throws HorarioInfactibleException si no hay solución factible o el modelo
     *         es inválido.
     */
    public ResultadoOptimizacion resolverOptimizandoConDetalle(ProblemaHorario problema) {
        Objects.requireNonNull(problema, "problema no puede ser null");

        ModeloCpSat modelo = new ModeloCpSat(problema).construirConObjetivo();

        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(maxSegundos);
        solver.getParameters().setRandomSeed(semilla);

        CpSolverStatus estado = solver.solve(modelo.model());
        if (estado == CpSolverStatus.OPTIMAL || estado == CpSolverStatus.FEASIBLE) {
            return new ResultadoOptimizacion(
                    modelo.extraerSolucion(solver),
                    estado,
                    solver.objectiveValue(),
                    solver.bestObjectiveBound());
        }
        throw new HorarioInfactibleException(
                "El solver no encontró un horario factible (modo optimización). "
                        + "Estado CP-SAT: " + estado);
    }
}
