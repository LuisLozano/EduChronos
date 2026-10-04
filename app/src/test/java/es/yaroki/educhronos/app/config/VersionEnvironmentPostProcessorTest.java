package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

/**
 * Qué versión publica {@link VersionEnvironmentPostProcessor} y qué escribe en el log
 * (C-version-y-rastro, condición 2, S192). Secuencia de casos propia desde (1).
 *
 * <p>Mismo patrón que {@code RutaBaseDatosEnvironmentPostProcessorTest}: un
 * {@code StandardEnvironment} a mano y la sobrecarga de paquete que recibe lo que en
 * producción viene de fuera, aquí el {@code build-info}. La fábrica de logs no difiere y
 * entrega un {@code Log} simulado, para poder verificar la línea.
 */
class VersionEnvironmentPostProcessorTest {

    private final Log log = mock(Log.class);

    private final DeferredLogFactory logs = destino -> log;

    private static ByteArrayResource buildInfo(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** (1) Con un {@code build-info} que trae versión, se publica esa y se escribe en el log. */
    @Test
    void conBuildInfoPublicaSuVersionYLaEscribeEnElLog() {
        StandardEnvironment entorno = new StandardEnvironment();

        new VersionEnvironmentPostProcessor(logs).postProcessEnvironment(
                entorno, buildInfo("build.artifact=app\nbuild.version=1.2.3-prueba\n"));

        assertThat(entorno.getProperty("educhronos.version")).isEqualTo("1.2.3-prueba");
        verify(log).info("Educhronos versión 1.2.3-prueba");
    }

    /** (2) Sin fichero, la versión es «desconocida» y así se dice en el log; el arranque sigue. */
    @Test
    void sinFicheroPublicaDesconocidaYLoDice() {
        StandardEnvironment entorno = new StandardEnvironment();

        new VersionEnvironmentPostProcessor(logs).postProcessEnvironment(
                entorno, new ClassPathResource("META-INF/no-existe-build-info.properties"));

        assertThat(entorno.getProperty("educhronos.version")).isEqualTo("desconocida");
        verify(log).info("Educhronos versión desconocida");
    }

    /** (3) Un fichero sin {@code build.version} cuenta como si no estuviera. */
    @Test
    void unBuildInfoSinVersionEsDesconocida() {
        StandardEnvironment entorno = new StandardEnvironment();

        new VersionEnvironmentPostProcessor(logs).postProcessEnvironment(
                entorno, buildInfo("build.artifact=app\n"));

        assertThat(entorno.getProperty("educhronos.version")).isEqualTo("desconocida");
    }

    /**
     * (4) Una {@code educhronos.version} ya definida no se pisa: la fuente del post-procesador
     * va al final, y el log dice la versión que va a regir.
     */
    @Test
    void unaVersionYaDefinidaNoSePisa() {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources()
                .addFirst(new MapPropertySource("fija", Map.of("educhronos.version", "5.5.5-fijada")));

        new VersionEnvironmentPostProcessor(logs).postProcessEnvironment(
                entorno, buildInfo("build.version=1.2.3-prueba\n"));

        assertThat(entorno.getProperty("educhronos.version")).isEqualTo("5.5.5-fijada");
        assertThat(entorno.getPropertySources().contains("educhronos-version"))
                .as("la fuente se añade igualmente, pero detrás")
                .isTrue();
        verify(log).info("Educhronos versión 5.5.5-fijada");
    }
}
