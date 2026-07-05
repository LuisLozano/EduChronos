package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de un {@code HorarioGenerado} y sus sesiones para las vistas
 * de Fase 7. Cabecera (metadata del horario) + lista de {@link SesionVistaDTO}.
 * Solo datos: lo ensambla {@code GeneradorHorarioService.proyectar}.
 *
 * <p>{@code estado} es el ciclo editorial ({@code EstadoHorario.name()}) y
 * {@code estadoSolver} el veredicto del solver, campos distintos a propósito
 * (§4.7). {@code objetivo}/{@code cotaInferior} son {@code Double} nullable
 * reales (0.0 es válido, distinto de "no medido"). {@code fechaGeneracion} es el
 * {@code Instant} serializado como texto ISO-8601.
 */
public record HorarioProyeccionDTO(
        Long id,
        String nombre,
        String estado,
        String estadoSolver,
        Double objetivo,
        Double cotaInferior,
        String fechaGeneracion,
        List<SesionVistaDTO> sesiones) {
}
