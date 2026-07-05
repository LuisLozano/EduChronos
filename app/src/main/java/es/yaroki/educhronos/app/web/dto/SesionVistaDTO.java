package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Proyección plana de una {@code Sesion} para las vistas de Fase 7 (grupo,
 * profesor, aula). Solo datos: todo el mapeo desde las entidades JPA lo hace
 * {@code GeneradorHorarioService.proyectar} dentro de su transacción; este record
 * no navega relaciones ni contiene lógica.
 *
 * <p>{@code dia} es 1..5 (lunes..viernes) y {@code tramo} es el ordenEnDia 1..6
 * (recreos excluidos), igual que {@code solver.domain.Tramo}. Una plaza en
 * co-docencia se proyecta en UNA sola entrada con varios {@code profesores}
 * (D-F7-2); {@code grupos} es la unión sin duplicados de los grupos de todos los
 * subgrupos de la plaza (D-F7-1). Las listas van ordenadas para salida estable.
 */
public record SesionVistaDTO(
        Long sesionId,
        int indice,
        int dia,
        int tramo,
        String asignaturaCodigo,
        String asignaturaNombre,
        List<String> profesores,
        String aulaCodigo,
        List<String> subgrupos,
        List<String> grupos,
        String actividadCodigo,
        String plazaCodigo) {
}
