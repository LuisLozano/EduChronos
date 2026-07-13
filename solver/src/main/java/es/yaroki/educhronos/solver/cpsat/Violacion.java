package es.yaroki.educhronos.solver.cpsat;

import java.util.List;
import java.util.Objects;

/**
 * Una restricción DURA violada, atribuida de forma ESTRUCTURADA (Fase 8, Bloque
 * 8.3-A). Sustituye al antiguo {@code String} por violación: conserva el texto
 * humano en {@link #descripcion()} y añade la categoría ({@link #regla()}), el
 * recurso implicado y —lo importante— las {@link CeldaRef celdas} culpables.
 *
 * <p><b>Una violación = N celdas</b> (D-3). Un solape de profesor entre dos
 * instancias es UNA {@code Violacion} con {@code celdas().size() == 2}, no dos
 * violaciones: preserva la cardinalidad que asumen los tests.
 *
 * @param regla         categoría (no-null).
 * @param recursoCodigo código del recurso implicado ("MAT8"/"A12In"/"1A"). NULL
 *                      cuando la regla no habla de un recurso concreto
 *                      ({@code INSTANCIA_SIN_COLOCAR}, {@code BLOQUE_IMPOSIBLE},
 *                      {@code DISTRIBUCION_MISMO_DIA}).
 * @param tramoCodigo   código del tramo implicado. NULL si no aplica.
 * @param celdas        celdas culpables; no-null ni vacía ({@code List.copyOf}).
 * @param descripcion   texto humano legible (no-null); es lo que imprime la CLI.
 */
public record Violacion(
        ReglaDura regla,
        String recursoCodigo,
        String tramoCodigo,
        List<CeldaRef> celdas,
        String descripcion) {

    public Violacion {
        Objects.requireNonNull(regla, "regla no puede ser null");
        Objects.requireNonNull(descripcion, "descripcion no puede ser null");
        Objects.requireNonNull(celdas, "celdas no puede ser null");
        if (celdas.isEmpty()) {
            throw new IllegalArgumentException("celdas no puede estar vacía");
        }
        celdas = List.copyOf(celdas);
        // recursoCodigo y tramoCodigo intencionadamente nullables.
    }
}
