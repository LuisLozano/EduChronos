package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista del horario agrupada por GrupoAdministrativo.
 *
 * Una sesión pertenece a la fila de cada grupo administrativo cubierto por
 * los subgrupos de su plaza. En Fase 2 una plaza cubre subgrupos de un
 * único grupo; en Fase 3 una plaza de agrupamiento transversal cubrirá
 * subgrupos de varios grupos y la misma sesión aparecerá en varias filas
 * sin cambio en este código.
 */
final class VistaPorGrupo implements VistaHorario<GrupoAdministrativo> {

    @Override
    public String titulo() {
        return "Horario por grupo";
    }

    @Override
    public List<GrupoAdministrativo> filas(ProblemaHorario problema) {
        return problema.grupos().stream()
                .sorted(Comparator.comparing(GrupoAdministrativo::codigo))
                .toList();
    }

    @Override
    public String etiquetaFila(GrupoAdministrativo clave) {
        return clave.codigo();
    }

    @Override
    public Set<GrupoAdministrativo> filasDe(SesionMaterializada sesion) {
        return sesion.plaza().subgrupos().stream()
                .flatMap(sg -> sg.grupos().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String contenidoCelda(SesionMaterializada sesion) {
        return FormatoCelda.formatear(sesion);
    }
}