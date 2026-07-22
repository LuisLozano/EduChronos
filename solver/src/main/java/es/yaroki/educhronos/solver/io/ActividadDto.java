// ActividadDto.java
package es.yaroki.educhronos.solver.io;

import java.util.List;

public record ActividadDto(
        String codigo,
        String asignatura,                // nullable: null si las plazas difieren en asignatura
        Integer repeticionesPorSemana,
        Integer duracionTramos,
        String patronTemporal,            // DISTRIBUIDA | AGRUPADA | NEUTRA
        List<PlazaDto> plazas,
        Boolean requiereTutor) { }        // wrapper: ausente/null lo traduce el mapper a false