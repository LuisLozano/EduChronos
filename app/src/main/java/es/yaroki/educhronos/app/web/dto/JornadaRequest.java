package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code PUT /api/jornada}: el DÍA TIPO del centro (C-jornada M3). No hay
 * {@code POST} ni {@code DELETE} —la jornada es un singleton— y el {@code PUT} es un
 * reemplazo total, no un parche. Cada tramo es un {@link TramoJornadaRequest}, en fichero
 * propio como {@link PlazaRequest} dentro de {@link ActividadRequest}.
 *
 * <p><b>{@code tramos} es UN DÍA, no la semana.</b> En la malla de referencia son 7
 * (6 lectivos + 1 recreo), no 35: {@code JornadaService} los replica en LUNES..VIERNES y
 * numera el {@code orden} global. La jornada es idéntica los cinco días (decisión de
 * producto), así que mandar la semana entera sería pedirle al cliente que repita cinco
 * veces el mismo dato y que numere; con esta frontera hay UN generador de malla, en el
 * backend y probado en JVM. La SALIDA sí es la semana completa: ver {@link JornadaDTO}.
 *
 * <p><b>Qué NO lleva, y por qué.</b> Ni {@code dia} (lo pone la expansión), ni
 * {@code orden} ni {@code ordenEnDia} (los calcula el servicio: {@code orden} es la
 * posición global 1..N tras expandir; {@code ordenEnDia} es derivado —solo lectivos,
 * 1..6 por día— y aparece únicamente en la salida). Aceptarlos por la red sería ofrecer
 * al cliente la posibilidad de contradecir a la fuente única de la renumeración. Tampoco
 * lleva {@code siguienteInmediato}: esa FK queda siempre a null (deuda registrada,
 * semántica del solver).
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): toda la validación —horas
 * parseables, {@code horaInicio < horaFin}, sin solapes y el techo de 6 lectivos— vive en
 * {@code JornadaService} y se aplica UNA vez sobre el día tipo, no cinco.
 */
public record JornadaRequest(List<TramoJornadaRequest> tramos) {
}
