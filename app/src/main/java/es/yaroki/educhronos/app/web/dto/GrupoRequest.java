package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/grupos} y del {@code PUT /api/grupos/{id}}: el estado
 * deseado de un grupo administrativo (§4.1, Bloque 8.5-B). SIN {@code id}: en el alta
 * lo asigna JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>{@code nivel} viaja como el CÓDIGO de negocio del nivel (String), NO como su id
 * sintético: {@code GrupoService} lo resuelve con {@code NivelRepository.findByCodigo}
 * y devuelve un {@code 400} accionable que nombra el código si no existe.
 *
 * <p>{@code tipo} entra como {@code String} (no como {@code TipoGrupo}) a propósito:
 * así {@code GrupoService} puede parsearlo con {@code TipoGrupo.valueOf(...)} y, si el
 * valor no existe, devolver un {@code 400} que NOMBRA el valor recibido en vez del
 * error opaco de deserialización de un enum. Además solo se acepta {@code ORDINARIO}
 * (D-nueva-2, lista blanca): los tipos no ordinarios se crean por otros flujos
 * (PDC → 8.5-D, virtuales de optativa → 8.5-C+) y aquí producen un 400 que nombra el
 * tipo rechazado.
 *
 * <p>Solo datos, sin lógica: toda la validación vive en {@code GrupoService}.
 */
public record GrupoRequest(
        String codigo,
        String nivel,
        String tipo) {
}
