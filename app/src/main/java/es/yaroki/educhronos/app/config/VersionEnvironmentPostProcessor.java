package es.yaroki.educhronos.app.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Publica la versión con la que se construyó el jar como {@code educhronos.version} y la
 * escribe en el log al arrancar (C-version-y-rastro, condición 2, S192).
 *
 * <p>La versión sale de {@code META-INF/build-info.properties}, que escribe el objetivo
 * {@code build-info} del plugin de Spring Boot con la {@code ${revision}} de la construcción:
 * la del tag en el bundle de CI, {@code 0.0.0-dev} en cualquier otra. Si el fichero no está
 * —un arranque desde un IDE que no pase por Maven— o no trae {@code build.version}, vale
 * {@link #DESCONOCIDA}: el arranque no falla por no saber su versión.
 *
 * <p>Mismo mecanismo que {@link RutaBaseDatosEnvironmentPostProcessor}, y por los mismos
 * motivos: se registra en {@code META-INF/spring.factories}, corre con
 * {@link Ordered#LOWEST_PRECEDENCE} y escribe por el log diferido, porque corre antes de que
 * exista el sistema de logging. La fuente va al FINAL del entorno: cualquier
 * {@code educhronos.version} que ya venga definida —un test, la línea de órdenes— manda.
 */
public class VersionEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Propiedad que se publica. */
    public static final String CLAVE_VERSION = "educhronos.version";

    /** Valor cuando no se sabe con qué versión se construyó. */
    public static final String DESCONOCIDA = "desconocida";

    /** Nombre de la fuente que se añade al entorno, para que se reconozca en un volcado. */
    static final String NOMBRE_FUENTE = "educhronos-version";

    /** Fichero que escribe el objetivo {@code build-info}, en la raíz del classpath. */
    static final String UBICACION_BUILD_INFO = "META-INF/build-info.properties";

    private final Log log;

    /** El log llega diferido por constructor; ver {@link RutaBaseDatosEnvironmentPostProcessor}. */
    public VersionEnvironmentPostProcessor(DeferredLogFactory fabricaDeLogs) {
        this.log = fabricaDeLogs.getLog(VersionEnvironmentPostProcessor.class);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        postProcessEnvironment(environment, new ClassPathResource(UBICACION_BUILD_INFO));
    }

    /**
     * Lo que de verdad hace el post-procesador, con el {@code build-info} ENTRANDO POR
     * PARÁMETRO para poder probarlo con un fichero de prueba o con ninguno.
     */
    void postProcessEnvironment(ConfigurableEnvironment environment, Resource buildInfo) {
        environment.getPropertySources()
                .addLast(new MapPropertySource(NOMBRE_FUENTE, Map.of(CLAVE_VERSION, leerVersion(buildInfo))));
        log.info("Educhronos versión " + environment.getProperty(CLAVE_VERSION));
    }

    /** {@code build.version} del fichero, o {@link #DESCONOCIDA} si no está, no se lee o no la trae. */
    static String leerVersion(Resource buildInfo) {
        if (!buildInfo.exists()) {
            return DESCONOCIDA;
        }
        Properties propiedades = new Properties();
        try (InputStream entrada = buildInfo.getInputStream()) {
            propiedades.load(entrada);
        } catch (IOException e) {
            return DESCONOCIDA;
        }
        String version = propiedades.getProperty("build.version");
        return version == null || version.isBlank() ? DESCONOCIDA : version.trim();
    }
}
