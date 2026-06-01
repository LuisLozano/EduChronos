package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import es.yaroki.educhronos.solver.io.ProblemaInvalidoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Punto de entrada CLI del solver Educhronos.
 *
 * Uso:   java -jar solver.jar <ruta-al-problema.json>
 *
 * Códigos de salida en {@link CodigoSalida}.
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int codigo = ejecutar(args, out, err);
        out.flush();
        err.flush();
        System.exit(codigo);
    }

    /**
     * Lógica del CLI con streams inyectables, para testabilidad.
     * Visible a nivel de paquete: los tests del paquete cli/ la invocan
     * directamente sin pasar por System.exit.
     */
    static int ejecutar(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("ERROR: argumentos inválidos");
            err.println("Uso: java -jar solver.jar <ruta-al-problema.json>");
            return CodigoSalida.ENTRADA_INVALIDA.valor();
        }
        Path ruta = Path.of(args[0]);
        if (!Files.isRegularFile(ruta)) {
            err.println("ERROR: el fichero no existe o no es legible: " + ruta);
            return CodigoSalida.ENTRADA_INVALIDA.valor();
        }

        // 1. Carga
        ProblemaHorario problema;
        try (InputStream is = Files.newInputStream(ruta)) {
            problema = new ProblemaHorarioJsonLoader().cargar(is);
        } catch (ProblemaInvalidoException e) {
            err.println("ERROR: problema inválido");
            err.println("  " + e.getMessage());
            return CodigoSalida.ENTRADA_INVALIDA.valor();
        } catch (IOException e) {
            err.println("ERROR: no se pudo leer el fichero: " + e.getMessage());
            return CodigoSalida.ENTRADA_INVALIDA.valor();
        }

        // 2. Cabecera
        out.println("=== Educhronos — Solver MVP ===");
        out.println("Problema cargado: " + ruta);
        imprimirResumenProblema(out, problema);

        // 3. Resolución
        SolucionHorario solucion;
        long t0 = System.currentTimeMillis();
        try {
            solucion = new SolverHorario().resolver(problema);
        } catch (HorarioInfactibleException e) {
            long tFin = System.currentTimeMillis();
            out.println("Solver: INFACTIBLE en " + (tFin - t0) + " ms");
            out.println("  Causa: " + e.getMessage());
            return CodigoSalida.INFACTIBLE.valor();
        }
        long tFin = System.currentTimeMillis();
        out.println("Solver: FEASIBLE en " + (tFin - t0) + " ms");
        out.println();

        // 4. Verificación (REUTILIZA VerificadorSolucion, no reimplementa)
        ResultadoVerificacion verificacion =
                new VerificadorSolucion().verificar(problema, solucion);
        VerificacionPrinter.imprimir(out, verificacion);
        out.println();

        // 5. Vistas
        List<SesionMaterializada> sesiones = Materializador.materializar(solucion);
        HorarioPrinter.imprimir(out, problema, sesiones, new VistaPorGrupo());
        out.println();
        HorarioPrinter.imprimir(out, problema, sesiones, new VistaPorProfesor());

        return verificacion.esValida()
                ? CodigoSalida.OK.valor()
                : CodigoSalida.VIOLACIONES_DURAS.valor();
    }

    private static void imprimirResumenProblema(PrintStream out, ProblemaHorario p) {
        int instancias = p.actividades().stream()
                .mapToInt(a -> a.repeticionesPorSemana()).sum();
        out.println("  · " + instancias + " ActividadInstancia"
                + ", " + p.grupos().size() + " grupos"
                + ", " + p.profesores().size() + " profesores"
                + ", " + p.aulas().size() + " aulas"
                + ", " + p.tramos().size() + " tramos");
    }
}