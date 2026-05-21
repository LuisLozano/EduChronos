package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

/**
 * Una ocurrencia concreta de una Actividad en la semana.
 * indice: 1..actividad.repeticionesPorSemana()
 */
public record ActividadInstancia(Actividad actividad, int indice) {

    public ActividadInstancia {
        Objects.requireNonNull(actividad, "actividad no puede ser null");
        if (indice < 1 || indice > actividad.repeticionesPorSemana())
            throw new IllegalArgumentException(
                    "indice " + indice + " fuera de rango para actividad " + actividad.codigo());
    }
}