package es.yaroki.educhronos.app.web.dto;

/**
 * Proyección plana de un {@code GrupoAdministrativo} persistido (§4.1, Bloque 8.5-B),
 * SIMÉTRICA a {@link GrupoRequest} más el {@code id} sintético que necesitan el
 * {@code GET/{id}}, el {@code PUT/{id}} y el {@code DELETE/{id}}.
 *
 * <p>{@code nivel} viaja como el CÓDIGO de negocio del nivel (String), no como su id:
 * el consumidor razona en códigos, coherente con {@link GrupoRequest#nivel}.
 * {@code tipo} viaja como {@code String} (el {@code name()} del {@code TipoGrupo}),
 * por coherencia con el patrón de borde de 7A/8.5-A' (ver {@code AulaDTO}). En este
 * flujo siempre será {@code "ORDINARIO"} (D-nueva-2).
 *
 * <p>{@code grupoPadre} NO se expone: en este bloque es siempre null (los PDC que lo
 * usan son 8.5-D).
 *
 * <p>Solo datos, sin lógica: lo ensambla {@code GrupoService}.
 */
public record GrupoDTO(
        Long id,
        String codigo,
        String nivel,
        String tipo) {
}
