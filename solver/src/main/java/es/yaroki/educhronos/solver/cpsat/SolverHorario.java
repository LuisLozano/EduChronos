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
 * <p>Con un modelo sin objetivo, CP-SAT devuelve {@code OPTIMAL} en cuanto
 * encuentra una solución factible (no hay nada que optimizar) e
 * {@code INFEASIBLE} si demuestra que no existe. La {@code randomSeed} fija
 * hace reproducible la solución concreta devuelta entre ejecuciones.
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
}
