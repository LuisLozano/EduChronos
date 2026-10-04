package es.yaroki.educhronos.app.web.dto;

/** Respuesta de {@code GET /api/version} (S192): la versión con la que se construyó el jar. */
public record VersionDTO(String version) {}
