package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Una VÍA de un bloque en el plan de replicación (§4.6, Bloque S139): una plaza del bloque,
 * descrita con lo justo para que el usuario decida a cuál de ellas va el grupo nuevo.
 *
 * <p>Lleva {@code plazaId} ADEMÁS del código porque es el identificador por el que
 * {@link ReplicacionRequest} nombra la plaza. El código de plaza ({@code {actividad}-P{n}})
 * lo documenta {@code PlazaRequest} como identificador técnico interno e INESTABLE entre
 * ediciones; sirve para que el plan sea legible, no para referenciar.
 *
 * <p>{@code gruposActuales} son los códigos de los grupos que la vía ya cubre, ordenados;
 * {@code hermanoPresente} dice si el hermano del que se replica está entre ellos, que es la
 * vía que el grupo nuevo hereda en un bloque replicado.
 *
 * <p><b>{@code espejos} son los códigos de subgrupo ESPEJO cuyos ORIGINALES están en esta
 * vía, ordenados, y son el enlace sin el cual el plan no se puede decidir (Bloque S141,
 * C-alta-por-pantalla).</b> Sin él, un cliente que reciba el plan sabe qué bloques reparten
 * y qué espejos se van a crear, pero NO cuáles de esos espejos participan en cada bloque:
 * {@code gruposActuales} nombra GRUPOS y {@code PlanReplicacionDTO.subgruposACrear} es la
 * lista entera sin atar a nada. Y ese conjunto es exactamente el que
 * {@link ReplicacionRequest} tiene que cubrir sin sobrar ni faltar, así que un cliente que
 * no pueda calcularlo no puede componer un POST válido salvo por tanteo.
 *
 * <p>Se nombran los ESPEJOS y no los originales a propósito: el espejo es la clave por la
 * que {@link AsignacionRequest} nombra al subgrupo, así que dárselos ya derivados evita que
 * el cliente reimplemente la regla de sufijo —quitar el prefijo {@code {hermano}-}— y se
 * convierta en una segunda fuente de verdad que se desincroniza sola. La lista está VACÍA
 * en una vía donde el hermano no tiene población, que es legítimo: {@code BloqueDTO.vias}
 * trae todas las plazas del bloque, no solo las que le tocan.
 *
 * <p>Se rellena igual en los bloques REPLICADOS, donde el usuario no decide nada; ahí es
 * informativa —de qué se trae cada espejo— y sale del mismo camino sin condicional.
 */
public record ViaDTO(
        Long plazaId,
        String plazaCodigo,
        String asignatura,
        List<String> gruposActuales,
        List<String> espejos,
        boolean hermanoPresente) {
}
