package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Lo que el {@code DELETE /api/grupos/{id}/replicacion} acaba de hacer (Bloque S140,
 * C-alta-reversible): el parte de salida, simétrico al {@link PlanReplicacionDTO} del POST.
 *
 * <p><b>No hay parte "de lo que haría"</b> —no hay GET del deshacer— y por eso este record
 * describe SIEMPRE hechos consumados, nunca un plan. Lo que el POST necesitaba enseñar antes
 * de actuar (las vías, para que el usuario eligiera) aquí no existe: el deshacer no toma
 * ninguna decisión, lee el estado.
 *
 * <p><b>{@code plazasDescableadas} es la lista COMPLETA, una entrada por par (plaza, subgrupo),
 * no un conteo.</b> Un conteo dice "se han soltado 41 filas" y obliga a abrir la base para
 * saber cuáles; la lista dice de qué actividades ha salido el grupo, que es exactamente lo que
 * hay que comprobar tras deshacer. Puede tener más entradas que {@code subgruposBorrados}
 * (un subgrupo en varias plazas) o menos (el espejo del {@code -Completo} nace sin plazas y se
 * borra sin descablear nada).
 *
 * <p>Ambas listas VACÍAS con {@code grupo} relleno es la respuesta legítima al deshacer de un
 * grupo que ya está pelado: el DELETE es idempotente y llamarlo dos veces no es un error.
 */
public record ParteDeshacerDTO(
        String grupo,
        List<String> subgruposBorrados,
        List<PlazaDescableadaDTO> plazasDescableadas) {
}
