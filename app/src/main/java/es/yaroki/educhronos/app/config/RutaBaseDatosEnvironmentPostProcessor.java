package es.yaroki.educhronos.app.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
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
    static final String NOMBRE_FICHERO = "educhronos.db";

    /** Prefijo de la URL JDBC que se publica; lo exige el driver de SQLite. */
    static final String PREFIJO_URL = "jdbc:sqlite:";

    /** Carpeta bajo {@code %LOCALAPPDATA%}: en Windows los nombres van capitalizados. */
    static final String CARPETA_WINDOWS = "Educhronos";

    /** Carpeta bajo el directorio de datos XDG: en el resto de sistemas, en minúsculas. */
    static final String CARPETA_POSIX = "educhronos";

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
     * Decide la carpeta de datos. Método PURO: no toca disco ni lee {@code System.*}, todo
     * lo que necesita entra por parámetro. Así las dos ramas de sistema operativo se prueban
     * desde cualquier máquina, que es la única forma de cubrir la de Windows.
     *
     * <p>En Windows manda {@code %LOCALAPPDATA%} y, si falta o viene en blanco,
     * {@code %USERPROFILE%\AppData\Local}. En el resto manda {@code $XDG_DATA_HOME} y, si
     * falta o viene en blanco, {@code <user.home>/.local/share}, que es el valor por defecto
     * que fija el propio estándar XDG: medido en S153, la variable está sin definir incluso
     * en un escritorio Linux corriente, así que el fallback no es el caso raro.
     *
     * @param nombreSistema valor de {@code os.name}; {@code null} se trata como no-Windows
     * @param entorno lectura de variables de entorno, normalmente {@code System::getenv}
     * @param directorioPersonal valor de {@code user.home}
     * @throws IllegalStateException si el sistema no ofrece ninguna de sus dos vías
     */
    static Path resolverCarpeta(
            String nombreSistema, UnaryOperator<String> entorno, String directorioPersonal) {
        if (esWindows(nombreSistema)) {
            String localAppData = entorno.apply("LOCALAPPDATA");
            if (tieneValor(localAppData)) {
                return Path.of(localAppData, CARPETA_WINDOWS);
            }
            String perfilUsuario = entorno.apply("USERPROFILE");
            if (tieneValor(perfilUsuario)) {
                return Path.of(perfilUsuario, "AppData", "Local", CARPETA_WINDOWS);
            }
            throw new IllegalStateException(
                    "No se puede decidir dónde guardar los datos de Educhronos: en Windows hacen "
                            + "falta LOCALAPPDATA o USERPROFILE, y las dos vienen vacías.");
        }

        String datosXdg = entorno.apply("XDG_DATA_HOME");
        if (tieneValor(datosXdg)) {
            return Path.of(datosXdg, CARPETA_POSIX);
        }
        if (tieneValor(directorioPersonal)) {
            return Path.of(directorioPersonal, ".local", "share", CARPETA_POSIX);
        }
        throw new IllegalStateException(
                "No se puede decidir dónde guardar los datos de Educhronos: hacen falta "
                        + "XDG_DATA_HOME o user.home, y las dos vienen vacías.");
    }

    /**
     * Crea la carpeta con todos sus padres y comprueba que se puede escribir en ella. Va
     * aparte de {@link #resolverCarpeta} porque esto sí toca disco.
     *
     * <p><b>La guarda {@link Files#isWritable} NO la cubre ningún caso de prueba</b>
     * (D-guarda-escritura-sin-caso, medido en el M3 de S153: el mutante que la suprime
     * SOBREVIVE a los 14 casos). El único caso de carpeta imposible usa un fichero como
     * padre, y ahí revienta antes {@code createDirectories}, así que esta línea no llega a
     * ejecutarse nunca en la suite. No se cubre por dos razones: un {@code chmod 0555} sobre
     * un directorio temporal no discrimina si la suite corre como root —y la Fase 12 traerá
     * runners—, y en Windows {@code isWritable} no significa lo mismo que en POSIX: mira el
     * atributo de solo lectura, no los permisos efectivos, de modo que una carpeta sin
     * permiso de escritura real puede darlo por escribible. La guarda se queda porque el
     * diagnóstico que produce vale la pena: sin ella, el fallo sale luego como
     * SQLITE_CANTOPEN dentro del inicializador de scripts, sin nombrar la carpeta.
     *
     * @throws IllegalStateException nombrando la ruta, si no se puede crear o no es escribible
     */
    static void crearCarpeta(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se puede crear la carpeta de datos de Educhronos: " + carpeta
                            + ". Compruebe los permisos o arranque con "
                            + "--spring.datasource.url=jdbc:sqlite:<ruta de la base>",
                    e);
        }
        if (!Files.isWritable(carpeta)) {
            throw new IllegalStateException(
                    "La carpeta de datos de Educhronos no permite escribir: " + carpeta
                            + ". Compruebe los permisos o arranque con "
                            + "--spring.datasource.url=jdbc:sqlite:<ruta de la base>");
        }
    }

    private static boolean esWindows(String nombreSistema) {
        return nombreSistema != null && nombreSistema.toLowerCase(Locale.ROOT).contains("windows");
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
