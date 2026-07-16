package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code POST /api/subgrupos} y del {@code PUT /api/subgrupos/{id}}: el
 * estado deseado de un subgrupo (§4.2, Bloque 8.5-B). SIN {@code id}: en el alta lo
 * asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>{@code grupos} es una lista de CÓDIGOS de grupo administrativo (String), NO ids
 * ni objetos anidados: {@code SubgrupoService} resuelve cada código con
 * {@code GrupoAdministrativoRepository.findByCodigo} (D-nueva-4) y devuelve un
 * {@code 400} que nombra el primer código no resoluble. Debe traer ≥1 grupo
 * (D-nueva-1, invariante I6 del dominio del solver replicada aquí): vacío o null → 400.
 *
 * <p>En la edición el conjunto se REEMPLAZA por completo (D-nueva, no unión ni deltas).
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code SubgrupoService}.
 */
public record SubgrupoRequest(
        String codigo,
        List<String> grupos) {
}
