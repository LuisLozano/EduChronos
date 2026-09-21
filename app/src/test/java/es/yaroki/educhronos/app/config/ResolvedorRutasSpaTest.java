package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Qué devuelve {@link ResolvedorRutasSpa} para cada forma de ruta que puede llegarle. Secuencia de
 * casos PROPIA de este fichero desde (1) (D-S101-num: la global tiene colisiones preexistentes).
 *
 * <p>Se llama al {@code getResource} protegido, que es la costura donde vive la decisión; por eso el
 * test está en el mismo paquete que la clase. La ubicación es un {@code @TempDir} con los tres
 * ficheros que distinguen los casos —el índice, un fichero con extensión que existe y un bundle con
 * nombre versionado— y NO el {@code classpath:/static/} de producción: ese no existe durante la fase
 * {@code test}, porque el pom copia el bundle de Angular en {@code prepare-package} (medido en S155).
 *
 * <p>La barra final de la ubicación no es cosmética: {@code createRelative} de un
 * {@link FileSystemResource} sin ella resolvería como hermano del directorio y todos los casos
 * darían el índice por el camino equivocado.
 *
 * <p>Lo que aquí NO se cubre es el cableado: que este resolvedor sea el del ÚNICO manejador de
 * {@code /**} y que la ruta le llegue sin barra inicial depende de {@link RutasSpaConfig} y de
 * {@code spring.web.resources.add-mappings=false}. Eso se verifica sobre el jar, en el M4.
 */
class ResolvedorRutasSpaTest {

    private static final String CONTENIDO_INDICE = "INDICE";

    /** (1) Una vista de la SPA no es un fichero: recibe el índice para que el router la resuelva. */
    @Test
    void unaRutaDeVistaDevuelveElIndice(@TempDir Path dir) throws IOException {
        Resource resuelto = resolver("horario/1", dir);

        assertThat(resuelto).isNotNull();
        assertThat(resuelto.getFilename()).isEqualTo(ResolvedorRutasSpa.INDICE);
        assertThat(contenido(resuelto)).isEqualTo(CONTENIDO_INDICE);
    }

    /**
     * (2) También las rutas de varios segmentos. Es el caso del F5 sobre una pestaña de
     * configuración, que es por donde se entra a todo el catálogo.
     */
    @Test
    void unaRutaDeVistaDeVariosSegmentosDevuelveElIndice(@TempDir Path dir) throws IOException {
        Resource resuelto = resolver("configuracion/jornada", dir);

        assertThat(resuelto).isNotNull();
        assertThat(contenido(resuelto)).isEqualTo(CONTENIDO_INDICE);
    }

    /**
     * (3) Un fichero que SÍ existe se sirve tal cual. Si el reenvío se comiera también estos, el
     * navegador recibiría HTML donde espera un icono y la portada quedaría sin sus recursos.
     */
    @Test
    void unFicheroQueExisteSeDevuelveTalCual(@TempDir Path dir) throws IOException {
        Resource resuelto = resolver("favicon.ico", dir);

        assertThat(resuelto).isNotNull();
        assertThat(resuelto.getFilename()).isEqualTo("favicon.ico");
        assertThat(contenido(resuelto)).isEqualTo("ICONO");
    }

    /** (4) Y el bundle de Angular, que es el fichero del que vive la aplicación entera. */
    @Test
    void elBundleQueExisteSeDevuelveTalCual(@TempDir Path dir) throws IOException {
        Resource resuelto = resolver("main-abc.js", dir);

        assertThat(resuelto).isNotNull();
        assertThat(resuelto.getFilename()).isEqualTo("main-abc.js");
        assertThat(contenido(resuelto)).isEqualTo("JS");
    }

    /**
     * (5) Un fichero con extensión que NO existe sigue dando 404. Devolverle el índice a un
     * {@code <script src>} roto cambiaría un 404 legible en la consola por un error de sintaxis
     * dentro de lo que el navegador cree que es JavaScript.
     */
    @Test
    void unFicheroConExtensionQueNoExisteDevuelveNulo(@TempDir Path dir) throws IOException {
        assertThat(resolver("main-inexistente.js", dir)).isNull();
    }

    /**
     * (6) Un endpoint inexistente de la API sigue dando 404, con el cuerpo de error de Boot. El
     * Accept no vale para distinguirlo de una vista: medido en S155, {@code /api/jornada} con
     * {@code Accept: text/html} responde 406, así que un cliente que pida HTML no marca la
     * diferencia.
     */
    @Test
    void unaRutaDeLaApiQueNoExisteDevuelveNulo(@TempDir Path dir) throws IOException {
        assertThat(resolver("api/no-existe", dir)).isNull();
    }

    /**
     * (7) El prefijo lleva barra: {@code apiario/x} es una vista, no la API. Sin la barra en
     * {@link ResolvedorRutasSpa#PREFIJO_API}, cualquier ruta que empezara por esas tres letras
     * perdería el reenvío en silencio.
     */
    @Test
    void unaRutaQueSoloEmpiezaPorEsasLetrasNoEsDeLaApi(@TempDir Path dir) throws IOException {
        Resource resuelto = resolver("apiario/x", dir);

        assertThat(resuelto).isNotNull();
        assertThat(contenido(resuelto)).isEqualTo(CONTENIDO_INDICE);
    }

    /** Resuelve una ruta sobre una ubicación recién poblada, como haría la cadena de recursos. */
    private static Resource resolver(String ruta, Path dir) throws IOException {
        Files.writeString(dir.resolve(ResolvedorRutasSpa.INDICE), CONTENIDO_INDICE);
        Files.writeString(dir.resolve("favicon.ico"), "ICONO");
        Files.writeString(dir.resolve("main-abc.js"), "JS");
        return new ResolvedorRutasSpa().getResource(ruta, new FileSystemResource(dir.toString() + "/"));
    }

    private static String contenido(Resource recurso) throws IOException {
        return new String(recurso.getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
