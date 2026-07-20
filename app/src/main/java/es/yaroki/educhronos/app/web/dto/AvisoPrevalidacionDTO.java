package es.yaroki.educhronos.app.web.dto;

import es.yaroki.educhronos.app.service.AvisoPrevalidacion;

/**
 * Proyección plana de un {@link AvisoPrevalidacion} para la capa REST (Fase 8,
 * Bloque 8.4-A). La {@code severidad} viaja como {@code String} ({@code "ERROR"} /
 * {@code "AVISO"}), mismo criterio que {@code ViolacionDTO.regla}: el enum del
 * servicio no se serializa tal cual al contrato HTTP.
 */
public record AvisoPrevalidacionDTO(
        String severidad,
        String regla,
        String entidadCodigo,
        int demanda,
        int disponible,
        String descripcion) {

    /** Adapta un aviso del servicio a su forma de transporte. */
    public static AvisoPrevalidacionDTO de(AvisoPrevalidacion aviso) {
        return new AvisoPrevalidacionDTO(
                aviso.severidad().name(),
                aviso.regla(),
                aviso.entidadCodigo(),
                aviso.demanda(),
                aviso.disponible(),
                aviso.descripcion());
    }
}
