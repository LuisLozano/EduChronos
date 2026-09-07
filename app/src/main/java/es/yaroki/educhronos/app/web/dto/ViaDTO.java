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
 */
public record ViaDTO(
        Long plazaId,
        String plazaCodigo,
        String asignatura,
        List<String> gruposActuales,
        boolean hermanoPresente) {
}
