package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de un {@code Aula} persistida (§4.1, Bloque 8.5-A'), SIMÉTRICA
 * a {@link AulaRequest} más el {@code id} sintético que necesitan el
 * {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>{@code tipo} viaja como {@code String} (el {@code name()} del {@code TipoAula}
 * del dominio), NO como el enum crudo, por COHERENCIA con el patrón de borde de 7A:
 * los DTO de proyección ({@code HorarioProyeccionDTO}) ya desenumeran a String con
 * {@code .name()} en el servicio, y {@code ProyeccionDtoContratoTest} blinda ese
 * contrato ({@code isTextual()}). Dos reglas de serialización de enum en el mismo
 * paquete {@code app.web.dto} sería deuda de coherencia; aquí se sigue la que 7A ya
 * fijó. La entrada ({@link AulaRequest#tipo}) también es String, por otra razón:
 * poder devolver un 400 accionable cuando el valor no parsea a {@code TipoAula}.
 *
 * <p>{@code capacidad}/{@code edificio}/{@code planta}/{@code sector} son nullable
 * de verdad (D-4): llegan y viajan null sin validarse.
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code AulaService}.
 */
public record AulaDTO(
        Long id,
        String codigo,
        String tipo,
        Integer capacidad,
        String edificio,
        Integer planta,
        String sector) {
}
