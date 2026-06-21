package es.yaroki.educhronos.solver.io;

import java.util.List;

public record ProblemaHorarioDto(
        List<TramoDto> tramos,
        List<AulaDto> aulas,
        List<AsignaturaDto> asignaturas,
        List<ProfesorDto> profesores,
        List<GrupoDto> grupos,
        List<SubgrupoDto> subgrupos,
        List<ActividadDto> actividades,
        List<RestriccionHorariaDto> restriccionesHorarias) { }
