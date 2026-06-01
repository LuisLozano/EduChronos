package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.Profesor;

import java.util.stream.Collectors;

/**
 * Construcción del texto de una celda de horario.
 * Formato: "Asignatura·Profesor[+Profesor...]·Aula"
 * Los códigos de profesor se concatenan con '+' cuando hay co-docencia.
 */
final class FormatoCelda {

    private FormatoCelda() { }

    static String formatear(SesionMaterializada sesion) {
        String asignatura = sesion.plaza().asignatura().codigo();
        String profesores = sesion.plaza().profesores().stream()
                .map(Profesor::codigo)
                .sorted()
                .collect(Collectors.joining("+"));
        // En Fase 2 solo se ejercita aulaFija (decisión táctica). El "?"
        // es defensivo y no debería activarse hasta Fase 3, cuando entre
        // aulasCandidatas y SolucionHorario tenga que transportar el aula
        // elegida por el solver.
        String aula = sesion.plaza().aulaFija()
                .map(a -> a.codigo())
                .orElse("?");
        return asignatura + "·" + profesores + "·" + aula;
    }
}