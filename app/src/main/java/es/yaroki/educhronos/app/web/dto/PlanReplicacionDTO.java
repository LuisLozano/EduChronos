package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Lo que la replicación HARÍA (GET) o lo que acaba de hacer (POST), sobre el grupo nuevo
 * {@code grupo} tomando como molde a {@code hermano} (Bloque S139, C-replicación-alta).
 *
 * <p>{@code subgruposACrear} son los códigos de los subgrupos ESPEJO, uno por cada subgrupo
 * del hermano, derivados como {@code {grupo}-{sufijo}} donde el sufijo es el código del
 * original sin su prefijo {@code {hermano}-}. Es la lista por la que
 * {@link AsignacionRequest} los nombra.
 *
 * <p><b>{@code replicados} y {@code reparto} son la partición de los BLOQUES que tocan al
 * hermano</b>, y solo de ellos. Un bloque es REPLICADO si todas sus vías cubren exactamente
 * el mismo conjunto de grupos: entonces el grupo nuevo entra por la vía del original y no hay
 * nada que decidir. REPARTE si alguna vía cubre un subconjunto estricto: ahí el reparto es
 * información que no está en el catálogo y la decide el usuario, vía a vía.
 *
 * <p>Las actividades de UNA SOLA PLAZA no salen en ninguna de las dos listas y no se tocan:
 * su espejo (típicamente el {@code -Completo}) se crea con CERO plazas. Es intencionado.
 */
public record PlanReplicacionDTO(
        String grupo,
        String hermano,
        List<String> subgruposACrear,
        List<BloqueDTO> replicados,
        List<BloqueDTO> reparto) {
}
