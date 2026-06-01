package es.yaroki.educhronos.solver.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end de cierre del Bloque 6 de Fase 2. Ejecuta
 * {@code Main.ejecutar} con el dataset real de 1ºESO ordinarias +
 * co-docencia LCL y comprueba:
 * <ul>
 *   <li>Código de salida {@code 0} (OK): implica resolución factible
 *       Y verificación con 0 violaciones. Los códigos INFACTIBLE=1 y
 *       VIOLACIONES_DURAS=3 quedan descartados por este aserto.</li>
 *   <li>stdout no vacío y con la cabecera del verificador (marcador
 *       textual "Violaciones"), prueba de que el pipeline llegó al
 *       final.</li>
 *   <li>stderr vacío, prueba de que no hubo errores no fatales.</li>
 * </ul>
 *
 * El test NO verifica la colocación celda a celda del horario impreso
 * en stdout: el solver es de factibilidad pura.
 */
class Main1EsoOrdinariasTest {

    private static final String FIXTURE_PATH =
        "/fixtures/problema-1eso-ordinarias.json";

    @Test
    @DisplayName("End-to-end con dataset 1ºESO: código de salida OK y output presente")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void endToEnd_codigoSalidaOk(@TempDir Path tempDir) throws Exception {
        // Extraer el fixture del classpath a un fichero del directorio
        // temporal del test. Main recibe un path en argv, así que necesita
        // un fichero real en disco, no un classpath resource.
        Path fixture = tempDir.resolve("problema-1eso-ordinarias.json");
        try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
            assertThat(in)
                .as("fixture %s en classpath", FIXTURE_PATH)
                .isNotNull();
            Files.copy(in, fixture);
        }

        // Capturar stdout y stderr del Main
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBuf, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBuf, true, StandardCharsets.UTF_8);

        // ASSUMED: Main.ejecutar(String[], PrintStream, PrintStream) devuelve int.
        int codigo = Main.ejecutar(new String[]{fixture.toString()}, out, err);

        out.flush();
        err.flush();

        // CodigoSalida.OK = 0 (decisión permanente, plan_trabajo_horarios.md).
        // Usar el literal evita asumir el nombre del accesor del enum.
        assertThat(codigo)
            .as("código de salida del Main con dataset 1ºESO")
            .isZero();

        String stdout = outBuf.toString(StandardCharsets.UTF_8);
        String stderr = errBuf.toString(StandardCharsets.UTF_8);

        // El pipeline produjo salida (no abortó silenciosamente).
        assertThat(stdout)
            .as("stdout no debería estar vacío en ejecución factible")
            .isNotEmpty();

        // Marcador estructural del VerificacionPrinter. Si el formato real
        // usa otra palabra (p.ej. "Violations", "Conflictos"), ajustar.
        assertThat(stdout)
            .as("stdout debe contener el marcador del verificador")
            .containsIgnoringCase("Violaciones");

        // No hubo errores no fatales.
        assertThat(stderr)
            .as("stderr no debería contener errores en ejecución factible")
            .isEmpty();
    }
}
