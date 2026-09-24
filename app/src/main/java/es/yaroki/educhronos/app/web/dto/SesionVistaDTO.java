package es.yaroki.educhronos.app.web.dto;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Proyección plana de una {@code Sesion} para las vistas de Fase 7 (grupo,
 * profesor, aula). Solo datos: todo el mapeo desde las entidades JPA lo hace
 * {@code GeneradorHorarioService.proyectar} dentro de su transacción; este record
 * no navega relaciones, y su única lógica es {@link #tramosCubiertos()}.
 *
 * <p>{@code dia} es 1..5 (lunes..viernes) y {@code tramo} es el ordenEnDia 1..6
 * (recreos excluidos), igual que {@code solver.domain.Tramo}: es el tramo de INICIO.
 * {@code duracion} es el número de tramos que ocupa la sesión, {@code >= 1}, tomado de
 * {@code actividad.duracionTramos}. Una plaza en
 * co-docencia se proyecta en UNA sola entrada con varios {@code profesores}
 * (D-F7-2); {@code grupos} es la unión sin duplicados de los grupos de todos los
 * subgrupos de la plaza (D-F7-1). Las listas van ordenadas para salida estable.
 *
 * <p>{@code aulaCodigo} NUNCA es null: {@code Sesion.aula} es {@code optional=false}
 * (D-F7B-6). El "aula null" que aparece en el PDF de referencia es un artefacto de
 * la extracción de la co-docencia, no del modelo: toda plaza colocada ocupa un aula.
 */
public record SesionVistaDTO(
        Long sesionId,
        int indice,
        int dia,
        int tramo,
        int duracion,
        String asignaturaCodigo,
        String asignaturaNombre,
        List<String> profesores,
        String aulaCodigo,
        List<String> subgrupos,
        List<String> grupos,
        String actividadCodigo,
        String plazaCodigo) {

    /**
     * Los tramos que ocupa la sesión: {@code tramo, tramo+1, …, tramo+duracion-1}.
     *
     * <p>Es la ÚNICA definición en Java de los tramos que ocupa una sesión en la
     * exportación; quien pinte o escriba una sesión en varios tramos la toma de aquí.
     * La numeración es la lectiva del día, que excluye el recreo: el tramo siguiente al
     * último antes del recreo es el primero después de él. Que un bloque no cruce el
     * recreo ni desborde el día lo garantizan el solver y el verificador, no este método,
     * que se limita a contar.
     */
    public List<Integer> tramosCubiertos() {
        return IntStream.range(tramo, tramo + duracion).boxed().toList();
    }
}
