package es.yaroki.educhronos.app.web.dto;

/** Respuesta de {@code GET /api/horarios/vigente} (S161): sólo el id, porque el cliente
 *  navega a {@code /horario/{id}} y la vista carga lo demás. */
public record HorarioVigenteDTO(Long id) {}
