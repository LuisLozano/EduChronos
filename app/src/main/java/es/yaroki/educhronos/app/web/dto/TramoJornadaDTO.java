package es.yaroki.educhronos.app.web.dto;

/**
 * Un tramo de la malla horaria de salida, sub-elemento de {@link JornadaDTO}
 * (C-jornada M3). Mismo patrón que {@link PlazaDTO} dentro de {@link ActividadDTO}: el
 * sub-record vive en su propio fichero, no anidado.
 *
 * <p>Lleva los dos enteros de ordenación ya resueltos: {@code orden} es la posición
 * global 1..N (persistida en la entidad {@code TramoSemanal}) y {@code ordenEnDia} es el
 * derivado que ve la UI —1..6, recreos EXCLUIDOS—, el mismo que llevan
 * {@link TramoRefDTO} y {@link SesionVistaDTO}.
 *
 * <p>{@code ordenEnDia} es {@code Integer} y no {@code int} a propósito: vale <b>null</b>
 * en los tramos no lectivos. Un recreo no tiene sitio en la numeración 1..6, y devolver
 * un 0 o un -1 obligaría a la UI a conocer un centinela.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2).
 */
public record TramoJornadaDTO(
        String dia,
        String horaInicio,
        String horaFin,
        boolean esLectivo,
        int orden,
        Integer ordenEnDia) {
}
