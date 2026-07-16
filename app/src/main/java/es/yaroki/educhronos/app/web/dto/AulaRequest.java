package es.yaroki.educhronos.app.web.dto;

/**
 * Cuerpo del {@code POST /api/aulas} y del {@code PUT /api/aulas/{id}}: el estado
 * deseado de un aula (§4.1, Bloque 8.5-A'). SIN {@code id}: en el alta lo asigna
 * JPA; en la edición lo lleva la URL, no el body.
 *
 * <p>{@code tipo} entra como {@code String} (no como {@code TipoAula}) a propósito:
 * así {@code AulaService} puede parsearlo con {@code TipoAula.valueOf(...)} y, si
 * el valor no existe, devolver un {@code 400} accionable que NOMBRA el valor
 * recibido y lista los válidos, en vez del error opaco de deserialización que daría
 * un enum. La salida ({@link AulaDTO#tipo}) también es String, pero por coherencia
 * con el patrón de borde de 7A (ver javadoc de {@link AulaDTO}).
 *
 * <p>{@code capacidad}/{@code edificio}/{@code planta}/{@code sector} son opcionales
 * de verdad (D-4): pueden llegar null, se persisten null y NO se validan. Únicos
 * obligatorios: {@code codigo} (no en blanco) y {@code tipo} (no en blanco y
 * parseable a {@code TipoAula}).
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A/8.2): toda la validación vive
 * en {@code AulaService}.
 */
public record AulaRequest(
        String codigo,
        String tipo,
        Integer capacidad,
        String edificio,
        Integer planta,
        String sector) {
}
