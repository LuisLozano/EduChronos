package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Plaza;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *       inicio es {@code tramoIndex}; lo consumen los {@code addNoOverlap} de
 *       profesor, subgrupo y grupo (que no dependen del aula).</li>
 *   <li>{@code opcionesDeAula}: por cada plaza con {@code aulasCandidatas}, la
 *       lista de {@link AulaOpcion} entre las que el solver elige exactamente
 *       una. Las plazas con {@code aulaFija} no aparecen en el mapa: su aula no
 *       es variable. Vacío si ninguna plaza de la actividad tiene candidatas.</li>
 * </ul>
 */
final class InstanciaProgramada {

    private final ActividadInstancia instancia;
    private final IntVar tramoIndex;
    private final IntervalVar intervalo;
    private final Map<Plaza, List<AulaOpcion>> opcionesDeAula;

    InstanciaProgramada(ActividadInstancia instancia,
                        IntVar tramoIndex,
                        IntervalVar intervalo,
                        Map<Plaza, List<AulaOpcion>> opcionesDeAula) {
        this.instancia  = Objects.requireNonNull(instancia, "instancia no puede ser null");
        this.tramoIndex = Objects.requireNonNull(tramoIndex, "tramoIndex no puede ser null");
        this.intervalo  = Objects.requireNonNull(intervalo, "intervalo no puede ser null");
        Objects.requireNonNull(opcionesDeAula, "opcionesDeAula no puede ser null (usa Map.of())");
        // Copia defensiva anidada: Map.copyOf no congela las listas interiores.
        Map<Plaza, List<AulaOpcion>> copia = new LinkedHashMap<>();
        for (Map.Entry<Plaza, List<AulaOpcion>> e : opcionesDeAula.entrySet()) {
            copia.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.opcionesDeAula = Map.copyOf(copia);
    }

    ActividadInstancia instancia() { return instancia; }

    IntVar tramoIndex() { return tramoIndex; }

    IntervalVar intervalo() { return intervalo; }

    /**
     * Opciones de aula por plaza, solo para plazas con {@code aulasCandidatas}.
     * Las plazas con {@code aulaFija} no están presentes. Inmutable.
     */
    Map<Plaza, List<AulaOpcion>> opcionesDeAula() { return opcionesDeAula; }
}
