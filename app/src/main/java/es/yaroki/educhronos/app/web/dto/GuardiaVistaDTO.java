package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de una guardia ordinaria repartida (S215, C-exportacion-guardias): el
 * profesor que está de guardia y su tramo. Solo datos; lo ensambla
 * {@code GeneradorHorarioService.proyectar} con la MISMA conversión de tramo que usa para
 * las sesiones, así que {@code dia} es 1..5 (lunes..viernes) y {@code tramo} el ordenEnDia
 * lectivo 1..6, igual que en {@link SesionVistaDTO}.
 */
public record GuardiaVistaDTO(String profesorCodigo, int dia, int tramo) {
}
