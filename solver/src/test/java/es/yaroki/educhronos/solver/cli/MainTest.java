package es.yaroki.educhronos.solver.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void sinArgumentosSaleConCodigo2() {
        Resultado r = ejecutar();
        assertThat(r.codigo).isEqualTo(2);
        assertThat(r.err).contains("Uso:");
    }

    @Test
    void demasiadosArgumentosSaleConCodigo2() {
        Resultado r = ejecutar("uno", "dos");
        assertThat(r.codigo).isEqualTo(2);
        assertThat(r.err).contains("Uso:");
    }

    @Test
    void ficheroInexistenteSaleConCodigo2() {
        Resultado r = ejecutar("/ruta/que/no/existe/problema.json");
        assertThat(r.codigo).isEqualTo(2);
        assertThat(r.err).contains("no existe");
    }

    @Test
    void problemaMinimoResolubleSaleConCodigo0() throws Exception {
        Path fixture = rutaDeRecurso("/fixtures/problema-solver-minimo.json");
        Resultado r = ejecutar(fixture.toString());
        assertThat(r.codigo).isEqualTo(0);
        assertThat(r.out)
                .contains("=== Educhronos — Solver MVP ===")
                .contains("Violaciones de restricciones duras: 0")
                .contains("Horario por grupo")
                .contains("Horario por profesor");
    }

    @Test
    void problemaMinimoMuestraCabecerasDeDiasYTramos() throws Exception {
        Path fixture = rutaDeRecurso("/fixtures/problema-solver-minimo.json");
        Resultado r = ejecutar(fixture.toString());
        assertThat(r.codigo).isEqualTo(0);
        assertThat(r.out)
                .contains("Lun").contains("Mar").contains("Mié")
                .contains("T1").contains("T2");
    }

    @Test
    void problemaMinimoMuestraCodigosClaveDelFixture() throws Exception {
        // El fixture comparte MAT8 y A5 entre dos subgrupos, y LEN2 entre
        // la co-docencia LCL-1A y Ref-1B. Verificamos que esos códigos
        // aparecen en la salida.
        Path fixture = rutaDeRecurso("/fixtures/problema-solver-minimo.json");
        Resultado r = ejecutar(fixture.toString());
        assertThat(r.out).contains("MAT8");
        assertThat(r.out).contains("LEN2");
        assertThat(r.out).contains("A5");
    }

    // -- utilidades -----------------------------------------------------

    private record Resultado(int codigo, String out, String err) { }

    private Resultado ejecutar(String... args) {
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baosOut, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(baosErr, true, StandardCharsets.UTF_8);
        int codigo = Main.ejecutar(args, out, err);
        return new Resultado(
                codigo,
                baosOut.toString(StandardCharsets.UTF_8),
                baosErr.toString(StandardCharsets.UTF_8));
    }

    private Path rutaDeRecurso(String recurso) throws Exception {
        return Path.of(getClass().getResource(recurso).toURI());
    }
}