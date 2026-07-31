package es.yaroki.educhronos.app.web.dto;

/**
 * Un tramo del DÍA TIPO entrante, sub-elemento de {@link JornadaRequest} (C-jornada M3).
 * Mismo patrón que {@link PlazaRequest} dentro de {@link ActividadRequest}: el sub-record
 * vive en su propio fichero, no anidado.
 *
 * <p><b>No lleva {@code dia}, y esa ausencia es el contrato.</b> La petición describe un
 * único día tipo que {@code JornadaService} replica en LUNES..VIERNES; el día lo pone el
 * backend, no el cliente. Un {@code dia} aquí permitiría mandar mallas asimétricas que el
 * servicio tendría que rechazar, y volvería a repartir la generación de la malla entre
 * frontend y backend.
 *
 * <p>{@code esLectivo=false} es el recreo: cuenta como tramo de la jornada (ocupa hueco y
 * {@code orden}) pero no recibe {@code ordenEnDia} ni llega al solver.
 *
 * <p><b>Por qué las horas entran como {@code String}.</b> Mismo motivo que
 * {@link AulaRequest#tipo}: un {@code LocalTime} en el record haría que un valor malo
 * muriese en la deserialización de Jackson, con un 400 opaco que no dice qué valor falló.
 * Como {@code String}, {@code JornadaService} las parsea y devuelve un 400 accionable que
 * NOMBRA el valor recibido.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2).
 */
public record TramoJornadaRequest(
        String horaInicio,
        String horaFin,
        boolean esLectivo) {
}
