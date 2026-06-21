package es.yaroki.educhronos.solver.domain;

import java.util.List;
import java.util.Objects;

/**
 * Entrada completa del solver. Agrupa todas las colecciones del dominio.
 * Los tramos deben estar ordenados por (diaSemana, ordenEnDia).
 */
public record ProblemaHorario(
        List<Tramo> tramos,
        List<Aula> aulas,
        List<Asignatura> asignaturas,
        List<Profesor> profesores,
        List<GrupoAdministrativo> grupos,
        List<Subgrupo> subgrupos,
        List<Actividad> actividades,
        List<RestriccionHoraria> restriccionesHorarias) {

    public ProblemaHorario {
        Objects.requireNonNull(tramos,      "tramos no puede ser null");
        Objects.requireNonNull(aulas,       "aulas no puede ser null");
        Objects.requireNonNull(asignaturas, "asignaturas no puede ser null");
        Objects.requireNonNull(profesores,  "profesores no puede ser null");
        Objects.requireNonNull(grupos,      "grupos no puede ser null");
        Objects.requireNonNull(subgrupos,   "subgrupos no puede ser null");
        Objects.requireNonNull(actividades, "actividades no puede ser null");
        Objects.requireNonNull(restriccionesHorarias, "restriccionesHorarias no puede ser null");

        tramos      = List.copyOf(tramos);
        aulas       = List.copyOf(aulas);
        asignaturas = List.copyOf(asignaturas);
        profesores  = List.copyOf(profesores);
        grupos      = List.copyOf(grupos);
        subgrupos   = List.copyOf(subgrupos);
        actividades = List.copyOf(actividades);
        restriccionesHorarias = List.copyOf(restriccionesHorarias);
    }

    /** Índice del tramo en la lista ordenada. Usado por el solver para construir IntVar. */
    public int indiceDeTramo(Tramo tramo) {
        int idx = tramos.indexOf(tramo);
        if (idx < 0)
            throw new IllegalArgumentException("Tramo no pertenece al problema: " + tramo.codigo());
        return idx;
    }
}