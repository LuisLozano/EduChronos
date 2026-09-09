package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo de un {@code PUT /api/horarios/{id}/instancias} que NO acaba en 2xx (S143).
 *
 * <p><b>Lo construimos nosotros, por la misma razón que {@link FalloGeneracionDTO}.</b>
 * El mecanismo de error de Spring está medido como mudo en este proyecto
 * (D-F8.6-ii-a): el {@code reason} de un {@code ResponseStatusException} se lee del
 * {@code MockHttpServletResponse} y no del cuerpo que viaja por la red. Aquí importa
 * especialmente, porque el rechazo por regla dura NO es un texto: es una lista de
 * violaciones que la rejilla tiene que pintar.
 *
 * @param causa      símbolo estable del hecho ({@code TRAMO_INEXISTENTE},
 *                   {@code HORARIO_INEXISTENTE}, {@code INSTANCIA_INEXISTENTE},
 *                   {@code VIOLA_REGLA_DURA}, {@code INSTANCIA_PINADA}). Es lo que la
 *                   vista lee para decidir qué decir; NO es texto para el usuario.
 * @param mensaje    prosa para el log y para quien depure. La vista no decide con ella.
 * @param violaciones violaciones duras que APARECEN por causa del movimiento y no
 *                   estaban antes. Lista vacía en toda causa que no sea
 *                   {@code VIOLA_REGLA_DURA}; nunca null.
 */
public record FalloMovimientoDTO(String causa, String mensaje, List<ViolacionDTO> violaciones) {
}
