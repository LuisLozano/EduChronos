package es.yaroki.educhronos.app.web.dto;

/**
 * UNA retirada concreta del deshacer de la replicación (Bloque S140, C-alta-reversible): el
 * subgrupo {@code subgrupo} deja de estar en la plaza {@code plazaId}. Es el grano del parte:
 * una entrada por PAR (plaza, subgrupo), no una por plaza ni una por subgrupo, porque un
 * subgrupo puede estar en varias plazas y una plaza lleva varios subgrupos.
 *
 * <p>Lleva {@code actividad} ADEMÁS de la plaza porque el código de plaza
 * ({@code {actividad}-P{n}}) es identificador técnico interno e INESTABLE entre ediciones —lo
 * documenta {@code PlazaRequest}— y por sí solo no dice de qué se está descableando al grupo.
 * Nombrar la actividad es lo que hace el parte legible sin abrir la base.
 *
 * <p>{@code plazaId} es el identificador de verdad, igual que en {@link ViaDTO}: el código
 * acompaña para leer, no para referenciar.
 */
public record PlazaDescableadaDTO(
        Long plazaId,
        String plazaCodigo,
        String actividad,
        String subgrupo) {
}
