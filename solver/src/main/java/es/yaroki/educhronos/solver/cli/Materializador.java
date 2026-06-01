package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.SolucionHorario;

import java.util.List;

/**
 * Aplana una SolucionHorario en una lista de SesionMaterializada,
 * una por (ActividadInstancia, Plaza). En Fase 2 cada actividad tiene
 * una sola plaza; el aplanado es N-correcto desde el principio para
 * cuando entren desdobles y agrupamientos en Fase 3.
 */
final class Materializador {

    private Materializador() { }

    static List<SesionMaterializada> materializar(SolucionHorario solucion) {
        return solucion.asignaciones().entrySet().stream()
                .flatMap(entrada -> entrada.getKey().actividad().plazas().stream()
                        .map(plaza -> new SesionMaterializada(
                                entrada.getValue(),
                                entrada.getKey(),
                                plaza)))
                .toList();
    }
}