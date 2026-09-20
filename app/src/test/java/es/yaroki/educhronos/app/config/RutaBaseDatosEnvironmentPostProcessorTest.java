package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Dónde decide {@link RutaBaseDatosEnvironmentPostProcessor} que vive la base, y qué hace
 * cuando esa carpeta no se puede usar. Secuencia de casos PROPIA de este fichero desde (1)
 * (D-S101-num: la global tiene colisiones preexistentes).
 *
 * <p>La resolución se prueba llamando al método puro con un sistema operativo y un entorno
 * fingidos, que es la única forma de cubrir la rama de Windows desde Linux. Lo que toca
 * disco —crear la carpeta— se prueba aparte, sobre {@code @TempDir}.
 *
 * <p>El camino que PUBLICA la URL también se prueba, por la sobrecarga de paquete de
 * {@code postProcessEnvironment} que recibe el sistema, el entorno y el directorio personal:
 * los casos (9) a (11) aseveran el prefijo, la clave, el nombre de la fuente y el nombre del
 * fichero sobre un {@code @TempDir}. Sin esa costura habría que tocar {@code user.home} por
 * {@code System.setProperty} y se escribiría en la carpeta de datos de quien corriera la
 * suite; y sin esos asertos, un cambio en cualquiera de esas cuatro cadenas no lo delataría
 * ningún test.
 *
 * <p>Lo único que sigue sin cubrirse aquí es la lectura de {@code System.getProperty} y
 * {@code System.getenv} que hace la sobrecarga pública, de una línea. Eso se verifica
 * arrancando la aplicación.
 */
class RutaBaseDatosEnvironmentPostProcessorTest {

    /**
     * Fábrica de logs que no difiere nada: devuelve el {@code Log} que le den. En producción
     * la pone Spring Boot y sí difiere, porque el post-procesador corre antes de que el
     * sistema de logging exista.
     */
    private static final DeferredLogFactory LOGS_INMEDIATOS = Supplier::get;

    /** Entorno fingido: lo que no esté en el mapa se lee como variable no definida. */
    private static UnaryOperator<String> entorno(Map<String, String> variables) {
        return variables::get;
    }

    // ------------------------------------------------------------------ resolución: Windows

    /**
     * (1) En Windows manda %LOCALAPPDATA%, y la carpeta cuelga directamente de ahí.
     *
     * <p>LÍMITE de este caso y de (2) y (3): corriendo en Linux, {@code Path.of} trata
     * {@code C:\Users\Ana\AppData\Local} como UN solo nombre de fichero, porque la barra
     * invertida no es separador aquí. Lo que se asevera es la COMPOSICIÓN —qué variable
     * manda y qué se le cuelga detrás—, no la ruta que saldrá en Windows. Allí la URL
     * efectiva será {@code jdbc:sqlite:C:\Users\<usuario>\AppData\Local\Educhronos\educhronos.db},
     * y que el driver Xerial acepte esa forma NO está medido: se cierra en el M4 sobre
     * Windows.
     */
    @Test
    void enWindowsConLocalappdataLaCarpetaCuelgaDeLocalappdata() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Windows 11",
                entorno(Map.of(
                        "LOCALAPPDATA", "C:\\Users\\Ana\\AppData\\Local",
                        "USERPROFILE", "C:\\Users\\Ana")),
                "C:\\Users\\Ana");

        assertThat(carpeta).isEqualTo(Path.of("C:\\Users\\Ana\\AppData\\Local", "Educhronos"));
    }

    /**
     * (2) Sin LOCALAPPDATA se reconstruye la ruta desde %USERPROFILE%. Importa que NO caiga
     * al directorio de trabajo ni al user.home pelado: la base tiene que acabar en
     * AppData\Local igual que en el caso corriente.
     *
     * <p>LÍMITE de este caso y de (2) y (3): corriendo en Linux, {@code Path.of} trata
     * {@code C:\Users\Ana\AppData\Local} como UN solo nombre de fichero, porque la barra
     * invertida no es separador aquí. Lo que se asevera es la COMPOSICIÓN —qué variable
     * manda y qué se le cuelga detrás—, no la ruta que saldrá en Windows. Allí la URL
     * efectiva será {@code jdbc:sqlite:C:\Users\<usuario>\AppData\Local\Educhronos\educhronos.db},
     * y que el driver Xerial acepte esa forma NO está medido: se cierra en el M4 sobre
     * Windows.
     */
    @Test
    void enWindowsSinLocalappdataLaCarpetaSeReconstruyeDesdeUserprofile() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Windows 11",
                entorno(Map.of("USERPROFILE", "C:\\Users\\Ana")),
                "C:\\Users\\Ana");

        assertThat(carpeta)
                .isEqualTo(Path.of("C:\\Users\\Ana", "AppData", "Local", "Educhronos"));
    }

    /**
     * (3) Una LOCALAPPDATA en blanco vale lo mismo que no tenerla: se cae a USERPROFILE.
     *
     * <p>LÍMITE de este caso y de (2) y (3): corriendo en Linux, {@code Path.of} trata
     * {@code C:\Users\Ana\AppData\Local} como UN solo nombre de fichero, porque la barra
     * invertida no es separador aquí. Lo que se asevera es la COMPOSICIÓN —qué variable
     * manda y qué se le cuelga detrás—, no la ruta que saldrá en Windows. Allí la URL
     * efectiva será {@code jdbc:sqlite:C:\Users\<usuario>\AppData\Local\Educhronos\educhronos.db},
     * y que el driver Xerial acepte esa forma NO está medido: se cierra en el M4 sobre
     * Windows.
     */
    @Test
    void enWindowsUnaLocalappdataEnBlancoNoSeUsa() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Windows 10",
                entorno(Map.of("LOCALAPPDATA", "   ", "USERPROFILE", "C:\\Users\\Ana")),
                "C:\\Users\\Ana");

        assertThat(carpeta)
                .isEqualTo(Path.of("C:\\Users\\Ana", "AppData", "Local", "Educhronos"));
    }

    // ------------------------------------------------------------------ resolución: el resto

    /** (4) Fuera de Windows manda $XDG_DATA_HOME, y la carpeta va en minúsculas. */
    @Test
    void enLinuxConXdgDataHomeLaCarpetaCuelgaDeXdgDataHome() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Linux",
                entorno(Map.of("XDG_DATA_HOME", "/home/ana/datos")),
                "/home/ana");

        assertThat(carpeta).isEqualTo(Path.of("/home/ana/datos", "educhronos"));
    }

    /**
     * (5) Sin XDG_DATA_HOME se usa el valor por defecto del estándar,
     * {@code <user.home>/.local/share}. No es el caso raro: medido en S153, la variable
     * estaba sin definir en un escritorio Linux corriente.
     */
    @Test
    void enLinuxSinXdgDataHomeSeUsaElDefectoDelEstandar() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Linux", entorno(Map.of()), "/home/ana");

        assertThat(carpeta).isEqualTo(Path.of("/home/ana", ".local", "share", "educhronos"));
    }

    // ------------------------------------------------------------------ no-op

    /**
     * (6) Con {@code spring.datasource.url} ya puesta el post-procesador no toca nada: ni
     * cambia la URL, ni añade su fuente, ni crea carpeta alguna. De esta regla —y solo de
     * ella— viven los {@code @DataJpaTest} y el e2e, que traen su propia URL.
     *
     * <p>Va por la costura, con el directorio personal apuntando al {@code @TempDir}: así el
     * aserto de que NO nace ninguna carpeta se comprueba sobre un sitio vacío y controlado,
     * sin manosear {@code System.setProperty("user.home")} para toda la JVM de la suite.
     */
    @Test
    void conUrlYaResueltaNoSeTocaElEntornoNiSeCreaCarpeta(@TempDir Path directorioPersonal)
            throws IOException {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources()
                .addFirst(new MapPropertySource(
                        "fija", Map.of("spring.datasource.url", "jdbc:sqlite:ya-resuelta.db")));
        List<String> fuentesAntes = nombresDeFuentes(entorno);

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(entorno, "Linux", entorno(Map.of()), directorioPersonal.toString());

        assertThat(entorno.getProperty("spring.datasource.url"))
                .as("la URL que ya estaba puesta manda")
                .isEqualTo("jdbc:sqlite:ya-resuelta.db");
        assertThat(nombresDeFuentes(entorno))
                .as("el entorno queda igual, sin la fuente del post-procesador")
                .isEqualTo(fuentesAntes)
                .doesNotContain("educhronos-ruta-datos");
        try (Stream<Path> contenido = Files.list(directorioPersonal)) {
            assertThat(contenido)
                    .as("no se ha creado ninguna carpeta de datos bajo el directorio personal")
                    .isEmpty();
        }
    }

    // ------------------------------------------------------------------ creación de la carpeta

    /**
     * (7) La carpeta se crea con TODOS sus padres. Es el caso que no ejercitaba ningún test:
     * la suite abre un fichero que ya existe y revierte, así que nunca llegaba a crear nada.
     * Y sin esto el arranque muere con SQLITE_CANTOPEN antes de levantar Tomcat.
     */
    @Test
    void laCarpetaSeCreaConTodosSusPadres(@TempDir Path raiz) {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                "Linux", entorno(Map.of("XDG_DATA_HOME", raiz.resolve("sin/crear").toString())),
                raiz.toString());
        assertThat(carpeta).doesNotExist();

        RutaBaseDatosEnvironmentPostProcessor.crearCarpeta(carpeta);

        assertThat(carpeta).isDirectory();
        assertThat(carpeta.resolve("educhronos.db"))
                .as("la base aún no existe: la crea el driver dentro de la carpeta")
                .doesNotExist();
    }

    /**
     * (8) Si la carpeta no se puede crear, el arranque FALLA y el mensaje NOMBRA la ruta.
     * Nada de volver al directorio de trabajo en silencio: ese fallback es justo el defecto
     * que este post-procesador elimina.
     */
    @Test
    void siLaCarpetaNoSePuedeCrearFallaNombrandoLaRuta(@TempDir Path raiz) throws IOException {
        Path estorbo = Files.createFile(raiz.resolve("esto-es-un-fichero"));
        Path imposible = estorbo.resolve("educhronos");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> RutaBaseDatosEnvironmentPostProcessor.crearCarpeta(imposible))
                .withMessageContaining(imposible.toString());
    }

    // ------------------------------------------------------------------ camino que publica la URL

    /**
     * (9) Sin URL previa se publica una, y se asevera pieza a pieza: el prefijo que exige el
     * driver, la clave que lee Spring, el nombre del fichero y la carpeta resuelta. Un cambio
     * en cualquiera de esas cuatro cadenas tiene que caer aquí; antes no lo veía nadie.
     */
    @Test
    void sinUrlPreviaSePublicaLaUrlDeLaCarpetaDeDatos(@TempDir Path raiz) {
        Path datos = raiz.resolve("datos-xdg");
        StandardEnvironment entorno = new StandardEnvironment();

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno, "Linux",
                        entorno(Map.of("XDG_DATA_HOME", datos.toString())),
                        raiz.toString());

        Path esperado = datos.resolve("educhronos").resolve("educhronos.db");
        assertThat(entorno.getProperty("spring.datasource.url"))
                .as("clave, prefijo, carpeta y nombre del fichero")
                .isEqualTo("jdbc:sqlite:" + esperado);
        assertThat(entorno.getProperty("spring.datasource.url")).startsWith("jdbc:sqlite:");
        assertThat(esperado.getFileName()).hasToString("educhronos.db");
    }

    /**
     * (10) La URL viaja en una fuente PROPIA y con nombre reconocible, añadida al final: si
     * se llamara de otro modo, o se colara delante de las demás, un volcado del entorno ya no
     * diría de dónde sale la ruta y una URL de línea de órdenes podría quedar tapada.
     */
    @Test
    void laUrlSePublicaEnUnaFuentePropiaAlFinal(@TempDir Path raiz) {
        StandardEnvironment entorno = new StandardEnvironment();
        List<String> fuentesAntes = nombresDeFuentes(entorno);

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno, "Linux",
                        entorno(Map.of("XDG_DATA_HOME", raiz.resolve("datos").toString())),
                        raiz.toString());

        List<String> fuentesDespues = nombresDeFuentes(entorno);
        assertThat(fuentesDespues).containsAll(fuentesAntes).contains("educhronos-ruta-datos");
        assertThat(fuentesDespues.get(fuentesDespues.size() - 1))
                .as("se añade con addLast, la última: no tapa a nadie")
                .isEqualTo("educhronos-ruta-datos");
        assertThat(entorno.getPropertySources().get("educhronos-ruta-datos").getProperty(
                        "spring.datasource.url"))
                .as("la clave vive en ESA fuente, no en otra")
                .isNotNull();
    }

    /**
     * (11) El camino feliz CREA la carpeta de verdad, con los padres que hagan falta, y no
     * deja la base hecha: la base la crea después el driver. Si la carpeta no naciera aquí, el
     * arranque moriría con SQLITE_CANTOPEN antes de levantar Tomcat.
     */
    @Test
    void elCaminoQuePublicaLaUrlCreaLaCarpetaConSusPadres(@TempDir Path raiz) {
        Path datos = raiz.resolve("ni/esta/ni/la/de/arriba");
        assertThat(datos).doesNotExist();

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        new StandardEnvironment(), "Linux",
                        entorno(Map.of("XDG_DATA_HOME", datos.toString())),
                        raiz.toString());

        Path carpeta = datos.resolve("educhronos");
        assertThat(carpeta).isDirectory();
        assertThat(carpeta.resolve("educhronos.db"))
                .as("la carpeta sí, el fichero no: ese lo crea el driver")
                .doesNotExist();
    }

    // ------------------------------------------------------------------ bordes del no-op y del sistema

    /**
     * (12) Una URL que son SOLO ESPACIOS no es una URL: el post-procesador resuelve igual que
     * si no hubiera ninguna. Si solo se mirase que la cadena no está vacía, un
     * {@code spring.datasource.url=   } en un properties dejaría la aplicación sin resolver y
     * sin carpeta, y el fallo saldría luego como un SQLITE_CANTOPEN sin relación aparente.
     *
     * <p>El aserto va sobre la FUENTE publicada y no sobre {@code getProperty}, porque la
     * fuente en blanco está por delante y seguiría ganando: lo que se comprueba es que el
     * post-procesador hizo su trabajo, no quién gana la resolución.
     */
    @Test
    void unaUrlDeSoloEspaciosNoCuentaYSeResuelveIgual(@TempDir Path raiz) {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources()
                .addFirst(new MapPropertySource("fija", Map.of("spring.datasource.url", "   ")));
        Path datos = raiz.resolve("datos");

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno, "Linux",
                        entorno(Map.of("XDG_DATA_HOME", datos.toString())),
                        raiz.toString());

        Path carpeta = datos.resolve("educhronos");
        assertThat(entorno.getPropertySources().contains("educhronos-ruta-datos"))
                .as("con la URL en blanco el post-procesador SÍ actúa")
                .isTrue();
        assertThat(entorno.getPropertySources().get("educhronos-ruta-datos")
                        .getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:sqlite:" + carpeta.resolve("educhronos.db"));
        assertThat(carpeta).isDirectory();
    }

    /**
     * (13) Un {@code os.name} nulo se trata como no-Windows, que es lo que promete el javadoc
     * de {@code resolverCarpeta}. Sin la guarda, resolver la ruta reventaría con un NPE dentro
     * de un post-procesador de entorno, o sea antes de que exista nada que informe del fallo.
     */
    @Test
    void unOsNameNuloSeResuelvePorLaRamaNoWindows() {
        Path carpeta = RutaBaseDatosEnvironmentPostProcessor.resolverCarpeta(
                null, entorno(Map.of("XDG_DATA_HOME", "/home/ana/datos")), "/home/ana");

        assertThat(carpeta).isEqualTo(Path.of("/home/ana/datos", "educhronos"));
    }

    /**
     * (14) El post-procesador corre DESPUÉS de {@link ConfigDataEnvironmentPostProcessor}, que
     * es quien carga los {@code application.properties}. De eso vive la regla de no-op: si
     * corriera antes, leería un entorno sin el properties de test y le pisaría la URL a los
     * slices —y la suite seguiría verde, escribiendo en la carpeta de datos del usuario—.
     *
     * <p>Instrumentos, los dos de API pública y sin montar ningún arranque: la constante
     * {@code ConfigDataEnvironmentPostProcessor.ORDER}, comparada como valor y no copiada como
     * literal, y {@link AnnotationAwareOrderComparator}, que es el comparador con el que Boot
     * ordena de verdad los post-procesadores (lo hace
     * {@code SpringFactoriesEnvironmentPostProcessorsFactory}). El segundo va con un
     * {@link Ordered} que solo lleva ese orden, en vez de una instancia real de ConfigData:
     * para ordenar solo cuenta el valor, y así no se depende de sus constructores.
     */
    @Test
    void elPostProcesadorSeAplicaDespuesDeConfigData() {
        RutaBaseDatosEnvironmentPostProcessor nuestro =
                new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS);

        assertThat(nuestro.getOrder())
                .as("orden mayor = se aplica después")
                .isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);

        Ordered comoConfigData = () -> ConfigDataEnvironmentPostProcessor.ORDER;
        List<Object> aplicados = new ArrayList<>(List.of(nuestro, comoConfigData));
        AnnotationAwareOrderComparator.sort(aplicados);

        assertThat(aplicados.get(aplicados.size() - 1))
                .as("tras ordenar como hace Boot, el último en aplicarse es el nuestro")
                .isSameAs(nuestro);
    }

    private static List<String> nombresDeFuentes(StandardEnvironment entorno) {
        return entorno.getPropertySources().stream().map(PropertySource::getName).toList();
    }
}
