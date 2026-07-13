package es.yaroki.educhronos.solver.cpsat;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de verificar una {@code SolucionHorario}: la lista de violaciones
 * de restricciones duras encontradas. Lista vacía significa solución válida.
 *
 * <p>Desde el Bloque 8.3-A, cada violación es una {@link Violacion} estructurada
 * (regla + recurso + celdas culpables), no un {@code String}. El texto humano no
 * se pierde: se deriva de {@link Violacion#descripcion()}.
 */
public record ResultadoVerificacion(List<Violacion> violaciones) {

    public ResultadoVerificacion {
        Objects.requireNonNull(violaciones, "violaciones no puede ser null");
        violaciones = List.copyOf(violaciones);
    }

    public boolean esValida() {
        return violaciones.isEmpty();
    }
}
