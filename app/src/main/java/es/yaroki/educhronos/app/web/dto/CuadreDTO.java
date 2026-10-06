package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Respuesta de {@code GET /api/prevalidacion/cuadre} (S203, C-totales-y-cargo, T2.6): el cuadre
 * de TODOS los profesores y de TODOS los grupos (ordinarios y PDC), tengan total declarado o no,
 * en el orden del problema. Lo calcula {@code PrevalidacionService.cuadre}, con la misma
 * agregación que las reglas de cuadre.
 */
public record CuadreDTO(
        List<CuadreEntidadDTO> profesores,
        List<CuadreEntidadDTO> grupos) {

    public CuadreDTO {
        profesores = List.copyOf(profesores);
        grupos = List.copyOf(grupos);
    }
}
