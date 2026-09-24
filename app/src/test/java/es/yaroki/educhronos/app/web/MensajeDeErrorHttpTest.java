package es.yaroki.educhronos.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.EduchronosApplication;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * El motivo de un rechazo VIAJA EN EL CUERPO: un {@code ResponseStatusException} lanzado
 * con un texto tiene que llegar al cliente como la clave {@code "message"} del JSON de
 * error, que es lo único que lee la interfaz (`message || error || degradado`). Salda
 * D-F8.6-ii-a (S167).
 *
 * <p><b>Por la red y leyendo el CUERPO, no {@code status().reason()}.</b> Los tests de
 * endpoint con MockMvc leen {@code reason()}, que sale de {@code getErrorMessage()} de la
 * respuesta y NO de los atributos de error que gobierna la clave de configuración. Por eso
 * dieron verde desde julio mientras el cuerpo llegaba mudo: la clave era la de Boot 3
 * ({@code server.error.include-message}), que Boot 4 da por muerta. Aquí el servidor
 * arranca de verdad en un puerto aleatorio y la petición pasa por Tomcat y por
 * {@code /error}, que es donde se decide si el texto viaja.
 *
 * <p><b>El {@code application.properties} de MAIN se carga a mano, y es el punto entero de
 * este fichero.</b> El de {@code src/test/resources} se llama igual y lo TAPA en el
 * classpath de test: Boot lee sólo el primero que encuentra. Sin la carga explícita de
 * {@link #propiedadesDeMain}, este test mediría la configuración de test y no la que
 * se distribuye. Se localiza por el directorio de clases de la aplicación, no por el
 * directorio de trabajo, para que no dependa de desde dónde se lance.
 *
 * <p>Los dos rechazos se eligieron porque se validan ANTES de tocar datos, así que no hay
 * nada que preparar: el 404 de un profesor inexistente
 * ({@code RestriccionHorariaService.reemplazar}) y el 400 de un {@code maxSegundos} no
 * positivo, que es la primera instrucción de {@code GeneradorHorarioService.generar}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MensajeDeErrorHttpTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir static Path carpeta;

    /**
     * Base propia, como {@code GuardaEndpointTest}, y las claves del fichero de MAIN. Van
     * aquí y no en un {@code @TestPropertySource} porque la ruta del fichero se calcula.
     */
    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws IOException {
        registro.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
        propiedadesDeMain().forEach((clave, valor) -> registro.add((String) clave, () -> valor));
    }

    /** El {@code application.properties} que se empaqueta, leído junto a las clases de main. */
    private static Properties propiedadesDeMain() throws IOException {
        Path clases;
        try {
            clases = Path.of(EduchronosApplication.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        Properties propiedades = new Properties();
        try (InputStream entrada = Files.newInputStream(clases.resolve("application.properties"))) {
            propiedades.load(entrada);
        }
        return propiedades;
    }

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void un404DelServicioLlevaSuMotivoEnElCuerpo() throws Exception {
        HttpResponse<String> respuesta =
                enviar("PUT", "/api/profesores/999999/restricciones-horarias", "[]");

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(mensaje(respuesta)).isEqualTo("No existe profesor con id 999999");
    }

    @Test
    void un400DelServicioLlevaSuMotivoEnElCuerpo() throws Exception {
        HttpResponse<String> respuesta = enviar("POST", "/api/horarios", "{\"maxSegundos\":-1}");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(mensaje(respuesta))
                .isEqualTo("maxSegundos debe ser > 0 si se especifica; recibido -1");
    }

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo)
            throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    /** La clave {@code message} del cuerpo; el cuerpo entero en el fallo si no está. */
    private static String mensaje(HttpResponse<String> respuesta) {
        JsonNode cuerpo = JSON.readTree(respuesta.body());
        assertThat(cuerpo.has("message"))
                .as("el cuerpo de error trae la clave \"message\"; cuerpo recibido: %s",
                        respuesta.body())
                .isTrue();
        return cuerpo.get("message").asString();
    }
}
