package es.yaroki.educhronos.app.catalog;

/**
 * Patrón de distribución temporal de las repeticiones de una {@link Actividad},
 * en la capa de persistencia (§4.6 del modelo).
 *
 * <p>Enum PROPIO de {@code app.catalog}, NO reutilización de
 * {@code solver.domain.PatronTemporal} (decisión D-B5-6). Mantiene la frontera
 * "entidad JPA con su forma, modelo del solver con la suya", igual que
 * {@code TipoGrupo} y {@code Dia}: una entidad de persistencia no debe importar
 * {@code solver.domain}. Hoy las tres constantes coinciden con las del dominio;
 * el {@code CatalogoMapper.aPatronTemporal} traduce entre ambos. Como se persiste
 * con {@code @Enumerated(STRING)}, el nombre de la constante se escribe en la BD:
 * ser propio evita que un renombrado interno del solver fuerce una migración.
 */
public enum PatronTemporal {
    DISTRIBUIDA,
    AGRUPADA,
    NEUTRA
}