package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Vista del horario agrupada por Profesor.
 *
 * Una sesión aparece en la fila de cada profesor de la plaza. En el caso
 * de co-docencia (|Plaza.profesores| ≥ 2), la misma sesión aparece en
 * varias filas: ese es el criterio del Bloque 4 sobre que AMBOS profesores
 * de la co-docencia LCL aparezcan ocupados en el mismo tramo.
 */
final class VistaPorProfesor implements VistaHorario<Profesor> {

    @Override
    public String titulo() {
        return "Horario por profesor";
    }

    @Override
    public List<Profesor> filas(ProblemaHorario problema) {
        return problema.profesores().stream()
                .sorted(Comparator.comparing(Profesor::codigo))
                .toList();
    }

    @Override
    public String etiquetaFila(Profesor clave) {
        return clave.codigo();
    }

    @Override
    public Set<Profesor> filasDe(SesionMaterializada sesion) {
        return sesion.plaza().profesores();
    }

    @Override
    public String contenidoCelda(SesionMaterializada sesion) {
        return FormatoCelda.formatear(sesion);
    }
}
