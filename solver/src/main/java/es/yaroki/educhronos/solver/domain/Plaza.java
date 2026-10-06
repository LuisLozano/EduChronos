package es.yaroki.educhronos.solver.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Una franja de docencia dentro de una Actividad.
 * - profesores: 1..N (I7). Caso normal: 1. Co-docencia: ≥2.
 * - aulaFija: presente si el aula está fijada de antemano.
 * - aulasCandidatas: presente (no vacío) si el solver elige entre varias.
 *   aulaFija y aulasCandidatas: a lo sumo una de las dos; una plaza sin aula no ocupa
 *   ningún aula (reuniones y funciones, S201): el modelo no le crea opciones de aula ni
 *   entra en el no solape de aulas.
 * - subgrupos: alumnos cubiertos por esta plaza.
 */
public record Plaza(
        String codigo,
        Asignatura asignatura,
        Set<Profesor> profesores,
        Optional<Aula> aulaFija,
        Set<Aula> aulasCandidatas,
        Set<Subgrupo> subgrupos) {

    public Plaza {
        Objects.requireNonNull(codigo,          "codigo no puede ser null");
        Objects.requireNonNull(asignatura,       "asignatura no puede ser null");
        Objects.requireNonNull(profesores,       "profesores no puede ser null");
        Objects.requireNonNull(aulaFija,         "aulaFija no puede ser null (usa Optional.empty())");
        Objects.requireNonNull(aulasCandidatas,  "aulasCandidatas no puede ser null");
        Objects.requireNonNull(subgrupos,        "subgrupos no puede ser null");

        // I7: al menos un profesor
        if (profesores.isEmpty())
            throw new IllegalArgumentException(
                    "Una plaza debe tener al menos un profesor: " + codigo);

        // aulaFija y aulasCandidatas mutuamente excluyentes
        if (aulaFija.isPresent() && !aulasCandidatas.isEmpty())
            throw new IllegalArgumentException(
                    "Plaza " + codigo + ": aulaFija y aulasCandidatas no pueden coexistir");

        // copias defensivas para garantizar inmutabilidad real
        profesores      = Set.copyOf(profesores);
        aulasCandidatas = Set.copyOf(aulasCandidatas);
        subgrupos       = Set.copyOf(subgrupos);
    }
}