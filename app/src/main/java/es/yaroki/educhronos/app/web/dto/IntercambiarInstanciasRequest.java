package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code PUT /api/horarios/{horarioId}/instancias/intercambio} (S144,
 * C-intercambiar-instancias): PERMUTAR los tramos de dos instancias.
 *
 * <p>Los nombres {@code primera}/{@code segunda} son deliberadamente simétricos y NO
 * "origen"/"destino": la operación es simétrica y el resultado no depende del orden.
 * El orden sí decide UNA cosa —a cuál de las dos se nombra en el mensaje cuando ambas
 * fallan por el mismo motivo—, y por eso los rechazos dicen siempre de qué lado hablan.
 *
 * <p>NO lleva tramo ni aula: el tramo de cada una sale de dónde está la otra, y el aula
 * se CONSERVA (cada instancia se lleva la suya al tramo destino).
 */
public record IntercambiarInstanciasRequest(
        ReferenciaInstancia primera, ReferenciaInstancia segunda) {
}
