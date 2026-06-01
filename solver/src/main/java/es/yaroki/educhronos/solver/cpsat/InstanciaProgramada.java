package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import java.util.Objects;

/**
 * Envuelve una {@link ActividadInstancia} del dominio con sus variables de
 * decisión CP-SAT. Vive en el paquete {@code cpsat} para que {@code domain}
 * permanezca libre de OR-Tools (decisión permanente del proyecto).
 *
 * <p>No es un {@code record}: porta referencias a objetos del modelo CP-SAT,
 * y el plan reserva los {@code record} para las entidades de configuración
 * inmutables del dominio.
 *
 * <p>Variables:
 * <ul>
 *   <li>{@code tramoIndex}: índice del tramo asignado, dominio [0, |tramos|).</li>
 *   <li>{@code intervalo}: intervalo de tamaño {@code duracionTramos} cuyo
 *       inicio es {@code tramoIndex}; lo consumen los {@code addNoOverlap}.</li>
 * </ul>
 */
final class InstanciaProgramada {

    private final ActividadInstancia instancia;
    private final IntVar tramoIndex;
    private final IntervalVar intervalo;

    InstanciaProgramada(ActividadInstancia instancia, IntVar tramoIndex, IntervalVar intervalo) {
        this.instancia  = Objects.requireNonNull(instancia, "instancia no puede ser null");
        this.tramoIndex = Objects.requireNonNull(tramoIndex, "tramoIndex no puede ser null");
        this.intervalo  = Objects.requireNonNull(intervalo, "intervalo no puede ser null");
    }

    ActividadInstancia instancia() { return instancia; }

    IntVar tramoIndex() { return tramoIndex; }

    IntervalVar intervalo() { return intervalo; }
}
