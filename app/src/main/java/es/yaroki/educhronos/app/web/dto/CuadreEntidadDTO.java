package es.yaroki.educhronos.app.web.dto;

/**
 * Cuadre de horas de un profesor o de un grupo (S203, C-totales-y-cargo, T2.6): las horas de
 * CLASE configuradas en el catálogo frente a las declaradas.
 *
 * @param codigo       código del profesor o del grupo; el frontend empareja por él
 * @param configuradas tramos semanales de CLASE que el catálogo le asigna, contados por actividad
 * @param declaradas   total declarado, o {@code null} si no lo tiene
 * @param descuadre    si las dos cifras no casan; {@code false} sin total declarado
 */
public record CuadreEntidadDTO(
        String codigo,
        int configuradas,
        Integer declaradas,
        boolean descuadre) {
}
