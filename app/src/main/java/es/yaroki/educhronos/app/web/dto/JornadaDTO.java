package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Salida de {@code GET /api/jornada} y de {@code PUT /api/jornada} (C-jornada M3): la
 * malla horaria del centro. Cada tramo es un {@link TramoJornadaDTO}, en fichero propio
 * como {@link PlazaDTO} dentro de {@link ActividadDTO}.
 *
 * <p><b>{@code persistida} es el campo que distingue los dos estados del singleton.</b>
 * El recurso siempre responde 200 —nunca 404— porque una jornada sin configurar no es
 * "no encontrada", es "todavía la de por defecto". Con la tabla vacía el servicio
 * sintetiza la MALLA DE REFERENCIA (la que sabía el difunto {@code SeedCatalogoRunner}:
 * 6 lectivos y un recreo por día) y la marca {@code persistida=false}; en cuanto un
 * {@code PUT} la guarda, pasa a {@code true}. Sin este campo la UI no podría distinguir
 * "el centro eligió exactamente esta malla" de "nadie ha configurado nada todavía", que
 * son cosas distintas de cara a avisar al usuario.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2).
 */
public record JornadaDTO(boolean persistida, List<TramoJornadaDTO> tramos) {
}
