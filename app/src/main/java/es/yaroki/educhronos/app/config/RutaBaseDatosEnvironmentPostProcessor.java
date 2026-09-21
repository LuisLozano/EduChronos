package es.yaroki.educhronos.app.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Sitúa la base de datos en una carpeta del USUARIO, no junto al ejecutable ni en el
 * directorio desde el que se lance el programa (condición 7 de O-instalación).
 *
 * <p><b>Por qué un {@code EnvironmentPostProcessor} y no una clase de configuración.</b>
 * El driver de SQLite no crea los directorios padre de la base: si la carpeta no existe
 * cuando Spring toca el {@code DataSource}, la aplicación MUERE antes de levantar Tomcat.
 * Medido en S153 con una URL cuyo padre no existía: {@code
 * org.sqlite.SQLiteException [SQLITE_CANTOPEN] Unable to open the database file}, brotando
 * dentro de {@code DataSourceScriptDatabaseInitializer.runScripts} al ejecutar
 * {@code schema.sql}, a 1,85 s del arranque y con cero líneas {@code Tomcat started}. La
 * carpeta hay que crearla ANTES de que exista el {@code DataSource}, y un EPP es el único
 * punto que corre suficientemente pronto: cualquier {@code @Bean} o {@code @AutoConfiguration}
 * llega tarde.
 *
 * <p><b>Por qué {@link EnvironmentPostProcessor} de {@code org.springframework.boot} y no
 * el homónimo de {@code org.springframework.boot.env}.</b> En Spring Boot 4.1 conviven las
 * dos interfaces con la MISMA firma; la del paquete {@code ...boot.env} está
 * {@code @Deprecated(since = "4.0.0")} y no la invoca nadie, de modo que implementarla
 * compila sin un solo aviso y el post-procesador queda mudo. La viva es esta, y es la que
 * el propio Boot declara en su {@code META-INF/spring.factories}. Este se registra por esa
 * misma vía: los ficheros {@code META-INF/spring/*.imports} NO sirven para descubrir EPPs
 * en 4.1 (sí para {@code AutoConfiguration}, que es otro mecanismo).
 *
 * <p><b>Regla de no-op.</b> Si {@code spring.datasource.url} ya trae un valor, este
 * post-procesador no hace NADA: ni resuelve, ni crea carpeta, ni publica propiedad. Esa es
 * la única condición, y de ella viven dos cosas ya existentes sin tocarlas: el
 * {@code application.properties} de test (que fija la suya y deja los {@code @DataJpaTest}
 * exactamente donde estaban) y el arranque con {@code --spring.datasource.url}, que el e2e
 * usa. No se miran perfiles ni nombres de clase de test: quedan fuera por tener URL propia,
 * no por ser tests.
 *
 * <p><b>Orden.</b> {@link Ordered#LOWEST_PRECEDENCE} para correr DESPUÉS del
 * {@code ConfigDataEnvironmentPostProcessor}, que es quien carga los
 * {@code application.properties}. Sin eso la regla de no-op leería un entorno donde el
 * fichero de test aún no está y le pisaría la URL a los slices.
 *
 * <p><b>Si la carpeta no se puede crear</b> se lanza {@link IllegalStateException} con la
 * ruta en el mensaje. NO hay vuelta atrás al directorio de trabajo: ese fallback silencioso
 * es justamente el defecto que la condición 7 elimina, y reintroducirlo dejaría al usuario
 * con la base en un sitio distinto según desde dónde arrancase, sin enterarse.
 */
public class RutaBaseDatosEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Propiedad que se publica, y también la que dispara el no-op si ya viene puesta. */
    static final String CLAVE_URL = "spring.datasource.url";

    /** Nombre de la fuente que se añade al entorno, para que se reconozca en un volcado. */
    static final String NOMBRE_FUENTE = "educhronos-ruta-datos";

    /** Nombre del fichero de base de datos dentro de la carpeta de datos. */
    static final String NOMBRE_FICHERO = CarpetaDatos.NOMBRE_FICHERO;

    /** Prefijo de la URL JDBC que se publica; lo exige el driver de SQLite. */
    static final String PREFIJO_URL = "jdbc:sqlite:";

    /** Carpeta bajo {@code %LOCALAPPDATA%}: en Windows los nombres van capitalizados. */
    static final String CARPETA_WINDOWS = CarpetaDatos.CARPETA_WINDOWS;

    /** Carpeta bajo el directorio de datos XDG: en el resto de sistemas, en minúsculas. */
    static final String CARPETA_POSIX = CarpetaDatos.CARPETA_POSIX;

    private final Log log;

    /**
     * El log llega por constructor y es DIFERIDO, no un {@code LogFactory.getLog} estático.
     * MEDIDO en S153: con el log corriente la traza NO SALE por ningún lado, porque un
     * post-procesador de entorno corre antes de que Spring Boot inicialice el sistema de
     * logging. Spring Boot ofrece el {@link DeferredLogFactory} como parámetro de
     * constructor justo para esto: guarda las líneas y las reproduce cuando el logging ya
     * está en pie.
     */
    public RutaBaseDatosEnvironmentPostProcessor(DeferredLogFactory fabricaDeLogs) {
        this.log = fabricaDeLogs.getLog(RutaBaseDatosEnvironmentPostProcessor.class);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        postProcessEnvironment(
                environment,
                System.getProperty("os.name"),
                System::getenv,
                System.getProperty("user.home"));
    }

    /**
     * Lo que de verdad hace el post-procesador, con el sistema operativo, el entorno y el
     * directorio personal ENTRANDO POR PARÁMETRO. Mismo motivo que en
     * {@link #resolverCarpeta}: así el camino que publica la URL se puede probar entero
     * —prefijo, clave, nombre de la fuente y nombre del fichero— sobre un directorio
     * temporal, sin depender de en qué máquina corra la suite ni escribir en la carpeta de
     * datos de quien la lance.
     *
     * @param environment entorno al que se añade la fuente, si procede
     * @param nombreSistema valor de {@code os.name}; {@code null} se trata como no-Windows
     * @param entorno lectura de variables de entorno, normalmente {@code System::getenv}
     * @param directorioPersonal valor de {@code user.home}
     */
    void postProcessEnvironment(
            ConfigurableEnvironment environment,
            String nombreSistema,
            UnaryOperator<String> entorno,
            String directorioPersonal) {
        String urlExistente = environment.getProperty(CLAVE_URL);
        if (urlExistente != null && !urlExistente.isBlank()) {
            return;
        }

        Path carpeta = resolverCarpeta(nombreSistema, entorno, directorioPersonal);
        crearCarpeta(carpeta);

        Path fichero = carpeta.resolve(NOMBRE_FICHERO);
        String url = PREFIJO_URL + fichero;
        environment.getPropertySources()
                .addLast(new MapPropertySource(NOMBRE_FUENTE, Map.of(CLAVE_URL, url)));
        log.info("Base de datos de Educhronos en " + fichero);
    }

    /**
     * Decide la carpeta de datos. Delega en {@link CarpetaDatos#resolver}, donde vive desde
     * S154 porque el modo escritorio la necesita sin {@code Environment}. Se conserva aquí
     * porque el spec de 14 casos de S153 la llama por este nombre y ese spec no se toca.
     *
     * @param nombreSistema valor de {@code os.name}; {@code null} se trata como no-Windows
     * @param entorno lectura de variables de entorno, normalmente {@code System::getenv}
     * @param directorioPersonal valor de {@code user.home}
     * @throws IllegalStateException si el sistema no ofrece ninguna de sus dos vías
     */
    static Path resolverCarpeta(
            String nombreSistema, UnaryOperator<String> entorno, String directorioPersonal) {
        return CarpetaDatos.resolver(nombreSistema, entorno, directorioPersonal);
    }

    /**
     * Crea la carpeta con todos sus padres y comprueba que se puede escribir en ella. Delega
     * en {@link CarpetaDatos#crear} por el mismo motivo que {@link #resolverCarpeta}.
     *
     * @throws IllegalStateException nombrando la ruta, si no se puede crear o no es escribible
     */
    static void crearCarpeta(Path carpeta) {
        CarpetaDatos.crear(carpeta);
    }

}
