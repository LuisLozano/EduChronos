package es.yaroki.educhronos.solver.cpsat;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de verificar una {@code SolucionHorario}: la lista de violaciones
 * de restricciones duras encontradas. Lista vacía significa solución válida.
 */
public record ResultadoVerificacion(List<String> violaciones) {

    public ResultadoVerificacion {
        Objects.requireNonNull(violaciones, "violaciones no puede ser null");
        violaciones = List.copyOf(violaciones);
    }

    public boolean esValida() {
        return violaciones.isEmpty();
    }
}
