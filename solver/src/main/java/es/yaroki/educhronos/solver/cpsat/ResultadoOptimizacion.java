package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import java.util.Objects;

/**
 * Resultado enriquecido de una resolución en modo OPTIMIZACIÓN: además de la
 * {@link SolucionHorario}, transporta el estado del solver y el valor del
 * objetivo, datos que {@link SolverHorario#resolverOptimizando} descarta
 * (decisión 2a, mantenida por compatibilidad).
 *
 * <p>Lo devuelve {@link SolverHorario#resolverOptimizandoConDetalle}. Sirve para
 * razonar sobre la CALIDAD de una solución a escala (Fase 5, deuda D23): a
 * diferencia de la vía clásica, aquí se puede distinguir una optimalidad
 * <i>probada</i> ({@code estado == OPTIMAL}) de una mejor-solución-al-agotar-el-
 * tiempo ({@code estado == FEASIBLE}), y medir cuán lejos quedó esta última del
 * óptimo mediante el gap entre {@link #objetivo()} y {@link #cotaInferior()}.
 *
 * <p><b>Semántica del estado en modo optimización</b> (difiere de factibilidad
 * pura): {@code OPTIMAL} = óptimo demostrado dentro del límite de tiempo;
 * {@code FEASIBLE} = se agotó el tiempo y esta es la mejor solución hallada,
 * no necesariamente óptima.
 *
 * <p><b>Semántica de los valores numéricos</b>:
 * <ul>
 *   <li>{@code objetivo}: valor de la función objetivo de la solución devuelta
 *       ({@code CpSolver#objectiveValue}). Es el coste de ESTA solución.</li>
 *   <li>{@code cotaInferior}: mejor cota inferior probada del óptimo
 *       ({@code CpSolver#bestObjectiveBound}). Si {@code estado == OPTIMAL},
 *       coincide con {@code objetivo}. Si {@code estado == FEASIBLE}, el
 *       intervalo {@code [cotaInferior, objetivo]} acota dónde está el óptimo:
 *       un gap pequeño indica que la FEASIBLE es casi óptima (utilizable); uno
 *       grande, que el solver se cortó lejos del óptimo.</li>
 * </ul>
 *
 * <p>El objetivo de CP-SAT NO sustituye al recomputo independiente del
 * {@link VerificadorSolucion}: este record lo expone para poder MEDIR a escala,
 * pero la autoridad sobre el coste real de una solución sigue siendo el
 * verificador, que cuenta sobre el dominio sin OR-Tools. Un test de
 * concordancia (objetivo de CP-SAT == recuento del verificador, con peso 1)
 * vigila que ambos canales coincidan.
 */
public record ResultadoOptimizacion(
        SolucionHorario solucion,
        CpSolverStatus estado,
        double objetivo,
        double cotaInferior) {

    public ResultadoOptimizacion {
        Objects.requireNonNull(solucion, "solucion no puede ser null");
        Objects.requireNonNull(estado, "estado no puede ser null");
    }

    /** {@code true} si el solver demostró optimalidad dentro del límite de tiempo. */
    public boolean esOptimo() {
        return estado == CpSolverStatus.OPTIMAL;
    }
}