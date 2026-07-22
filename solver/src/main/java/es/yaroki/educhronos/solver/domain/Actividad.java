package es.yaroki.educhronos.solver.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Unidad que el solver coloca en el tiempo.
 * Una actividad produce tantas ActividadInstancia como repeticionesPorSemana.
 * asignatura puede ser empty en actividades de bloque (ej. Religión/ATED).
 *
 * requiereTutor marca las actividades que un tutor del grupo debe impartir (§4.6,
 * invariante S8). El solver aún no lo consume: es dato transportado para la Fase 8.
 */
public record Actividad(
        String codigo,
        Optional<Asignatura> asignatura,
        int repeticionesPorSemana,
        int duracionTramos,
        PatronTemporal patronTemporal,
        List<Plaza> plazas,
        boolean requiereTutor) {

    public Actividad {
        Objects.requireNonNull(codigo,               "codigo no puede ser null");
        Objects.requireNonNull(asignatura,           "asignatura no puede ser null (usa Optional.empty())");
        Objects.requireNonNull(patronTemporal,       "patronTemporal no puede ser null");
        Objects.requireNonNull(plazas,               "plazas no puede ser null");

        if (repeticionesPorSemana < 1)
            throw new IllegalArgumentException(
                    "repeticionesPorSemana debe ser >= 1: " + codigo);
        if (duracionTramos < 1)
            throw new IllegalArgumentException(
                    "duracionTramos debe ser >= 1: " + codigo);
        if (plazas.isEmpty())
            throw new IllegalArgumentException(
                    "Una actividad debe tener al menos una plaza: " + codigo);

        plazas = List.copyOf(plazas);
    }
}