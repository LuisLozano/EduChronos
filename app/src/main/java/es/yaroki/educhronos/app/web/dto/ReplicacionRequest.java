package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code POST /api/grupos/{id}/replicacion} (Bloque S139): de qué hermano se
 * replica y, para cada subgrupo espejo que participe en un bloque de REPARTO, a qué vía va.
 *
 * <p>{@code asignaciones} debe traer EXACTAMENTE UNA entrada por cada espejo de reparto: ni
 * una menos (el reparto no se adivina) ni una repetida. Esa unicidad es, además, lo que hace
 * imposible violar I2 por esta vía —un espejo no puede acabar en dos plazas de la misma
 * actividad—. Los espejos de bloques REPLICADOS no se nombran aquí: su vía la deduce el
 * servicio.
 */
public record ReplicacionRequest(
        String hermano,
        List<AsignacionRequest> asignaciones) {
}
