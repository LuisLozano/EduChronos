package es.yaroki.educhronos.solver.cpsat;

import java.util.Objects;

/**
 * Aportación de UNA celda a UNA regla blanda del objetivo, atribuida de forma
 * CONTRAFACTUAL (Fase 8, Bloque 8.3-B). Responde a lo que la UI necesita: "¿qué
 * gano si muevo esta sesión?".
 *
 * <p><b>{@link #delta()} LLEVA SIGNO</b> — no es una penalización, es una derivada
 * {@code penalizacion_actual − penalizacion_si_esta_celda_no_estuviera}:
 * <ul>
 *   <li>{@code delta > 0} — mover la celda MEJORA (baja el coste).</li>
 *   <li>{@code delta < 0} — la celda está TAPANDO un hueco; moverla EMPEORA. Es un
 *       valor correcto y esperado (p.ej. {@code {1,2,3}} = 0 ventanas; quitar la 2 →
 *       {@code {1,3}} = 1 ventana → delta = 0 − 1 = −1).</li>
 *   <li>{@code delta = 0} — indiferente; en ese caso NO se emite {@code Penalizacion}
 *       (una celda sin aportación no aparece en {@link AtribucionBlanda}).</li>
 * </ul>
 *
 * @param regla         categoría blanda (no-null).
 * @param recursoCodigo código del profesor implicado (los tres términos son
 *                      por-profesor); no-null.
 * @param tramoCodigo   NULLABLE. No-null SOLO en {@link ReglaBlanda#INDISPONIBILIDAD_BLANDA}
 *                      (el tramo vetado). En {@code VENTANA_PROFESOR} y
 *                      {@code EXCESO_CONSECUTIVAS} es null: penalizan una
 *                      configuración DE DÍA, no de tramo; rellenarlo mentiría.
 *                      Asimetría deliberada, igual que en {@link Violacion}.
 * @param delta         aportación CON SIGNO (ver arriba); nunca 0 (no se emite si lo es).
 * @param descripcion   texto humano legible (no-null).
 */
public record Penalizacion(
        ReglaBlanda regla,
        String recursoCodigo,
        String tramoCodigo,
        int delta,
        String descripcion) {

    public Penalizacion {
        Objects.requireNonNull(regla, "regla no puede ser null");
        Objects.requireNonNull(recursoCodigo, "recursoCodigo no puede ser null");
        Objects.requireNonNull(descripcion, "descripcion no puede ser null");
        if (delta == 0) {
            throw new IllegalArgumentException(
                    "delta no puede ser 0: una aportación nula no se emite");
        }
        // tramoCodigo intencionadamente nullable (no-null solo en INDISPONIBILIDAD_BLANDA).
    }
}
