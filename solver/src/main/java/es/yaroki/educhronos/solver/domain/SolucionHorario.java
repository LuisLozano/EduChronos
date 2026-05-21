package es.yaroki.educhronos.solver.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Salida del solver: asignación de cada ActividadInstancia a un Tramo.
 */
public class SolucionHorario {

    private final Map<ActividadInstancia, Tramo> asignaciones;

    public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones) {
        Objects.requireNonNull(asignaciones, "asignaciones no puede ser null");
        this.asignaciones = Map.copyOf(asignaciones);
    }

    public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia) {
        return Optional.ofNullable(asignaciones.get(instancia));
    }

    public Map<ActividadInstancia, Tramo> asignaciones() {
        return asignaciones;
    }
}