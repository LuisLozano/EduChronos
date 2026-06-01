package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;

import java.io.PrintStream;

/**
 * Imprime un ResultadoVerificacion: contador y, si hay, lista de violaciones.
 *
 * El ResultadoVerificacion expone violaciones como List<String> sin
 * categorías tipadas: no es posible agrupar por categoría sin parsear
 * strings, así que el output se limita al contador y al listado.
 */
final class VerificacionPrinter {

    private VerificacionPrinter() { }

    static void imprimir(PrintStream out, ResultadoVerificacion resultado) {
        int n = resultado.violaciones().size();
        out.println("Violaciones de restricciones duras: " + n);
        if (n > 0) {
            for (String v : resultado.violaciones()) {
                out.println("  - " + v);
            }
        }
    }
}
