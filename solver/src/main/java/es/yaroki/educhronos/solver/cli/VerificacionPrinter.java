package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.Violacion;

import java.io.PrintStream;

/**
 * Imprime un ResultadoVerificacion: contador y, si hay, lista de violaciones.
 *
 * Desde 8.3-A cada violación es una {@link Violacion} estructurada (regla +
 * celdas culpables); la CLI imprime su {@code descripcion()}, con lo que la
 * salida a consola es idéntica a la de antes. La categoría tipada queda
 * disponible para consumidores que quieran agrupar sin parsear texto.
 */
final class VerificacionPrinter {

    private VerificacionPrinter() { }

    static void imprimir(PrintStream out, ResultadoVerificacion resultado) {
        int n = resultado.violaciones().size();
        out.println("Violaciones de restricciones duras: " + n);
        if (n > 0) {
            for (Violacion v : resultado.violaciones()) {
                out.println("  - " + v.descripcion());
            }
        }
    }
}
