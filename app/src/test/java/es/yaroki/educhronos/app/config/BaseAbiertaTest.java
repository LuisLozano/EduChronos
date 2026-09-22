package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Spec del puntero de curso abierto: {@link CarpetaDatos#baseAbierta} y su efecto en el
 * post-procesador (O-curso, S159).
 *
 * <p><b>Fichero aparte del spec de 14 casos de S153, que no se toca.</b> Aquel fija la
 * DECISIÓN DE CARPETA y la regla de no-op; esto fija QUÉ BASE de esa carpeta se abre, que es
 * una pregunta que en S153 no existía. Mezclarlos haría que un cambio en el puntero moviera
 * asertos de la carpeta.
 */
class BaseAbiertaTest {

    /** Fábrica de logs que no difiere nada, igual que en el spec de S153. */
    private static final DeferredLogFactory LOGS_INMEDIATOS = Supplier::get;

    /**
     * (11) Con un puntero que nombra un fichero que está, se abre ESE y no la base por
     * defecto. Es la condición que hace que duplicar un curso cambie de verdad el curso que
     * se abre la próxima vez.
     */
    @Test
    void unPunteroValidoDecideLaBaseQueSeAbre(@TempDir Path carpeta) throws Exception {
        Files.createFile(carpeta.resolve("educhronos.db"));
        Files.createFile(carpeta.resolve("curso-2026-2027.db"));
        Files.writeString(
                carpeta.resolve("curso-abierto"), "curso-2026-2027.db", StandardCharsets.UTF_8);

        assertThat(CarpetaDatos.baseAbierta(carpeta))
                .isEqualTo(carpeta.resolve("curso-2026-2027.db"));
    }

    /**
     * (12) Un puntero a un fichero que no está NO impide arrancar: se abre la base por
     * defecto y se avisa. Fallar dejaría al centro fuera por un fichero de una línea; abrir
     * en silencio le escondería que su curso no aparece.
     */
    @Test
    void unPunteroRotoAbreLaBasePorDefectoYAvisa(@TempDir Path carpeta) throws Exception {
        Files.createFile(carpeta.resolve("educhronos.db"));
        Files.writeString(
                carpeta.resolve("curso-abierto"), "curso-2030-2031.db", StandardCharsets.UTF_8);
        List<String> avisos = new ArrayList<>();

        Path base = CarpetaDatos.baseAbierta(carpeta, avisos::add);

        assertThat(base).isEqualTo(carpeta.resolve("educhronos.db"));
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0))
                .as("el aviso nombra el fichero que falta y la base que se abre en su lugar")
                .contains("curso-2030-2031.db")
                .contains("educhronos.db");
    }

    /**
     * (12-bis) Un puntero con una RUTA y no un nombre se descarta sin resolverlo: el fichero
     * lo escribe un programa, pero lo puede editar cualquiera, y {@code ../..} abriría una
     * base de fuera de la carpeta de datos.
     */
    @Test
    void unPunteroConRutaSeDescarta(@TempDir Path carpeta) throws Exception {
        Files.createFile(carpeta.resolve("educhronos.db"));
        // El destino EXISTE y aun así se descarta: lo que se rechaza es la forma del
        // contenido, no que el fichero falte. Va dentro del @TempDir a propósito, para que
        // el caso no escriba ni un byte fuera de su carpeta.
        Path sub = Files.createDirectory(carpeta.resolve("sub"));
        Files.createFile(sub.resolve("ajena.db"));
        Files.writeString(
                carpeta.resolve("curso-abierto"), "sub/ajena.db", StandardCharsets.UTF_8);
        List<String> avisos = new ArrayList<>();

        Path base = CarpetaDatos.baseAbierta(carpeta, avisos::add);

        assertThat(base).isEqualTo(carpeta.resolve("educhronos.db"));
        assertThat(avisos).hasSize(1);
    }

    /** (13) Sin puntero se abre {@code educhronos.db}: es el caso de toda base anterior a S159. */
    @Test
    void sinPunteroSeAbreLaBasePorDefecto(@TempDir Path carpeta) {
        assertThat(CarpetaDatos.baseAbierta(carpeta))
                .isEqualTo(carpeta.resolve("educhronos.db"));
    }

    /**
     * (14) Con {@code spring.datasource.url} explícita el post-procesador sigue sin hacer
     * NADA, aunque haya un puntero en la carpeta: ni lo lee, ni publica la carpeta de datos.
     * De esa regla vive la condición 7 —el e2e y los slices no escriben puntero— y el hecho
     * de que un arranque con URL propia no toque la carpeta del usuario.
     */
    @Test
    void conUrlExplicitaElPunteroNoSeMiraNiSePublicaLaCarpeta(@TempDir Path raiz) throws Exception {
        Path carpeta = Files.createDirectories(raiz.resolve("datos-xdg/educhronos"));
        Files.createFile(carpeta.resolve("curso-2026-2027.db"));
        Files.writeString(
                carpeta.resolve("curso-abierto"), "curso-2026-2027.db", StandardCharsets.UTF_8);
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources()
                .addFirst(
                        new MapPropertySource(
                                "fija",
                                Map.of("spring.datasource.url", "jdbc:sqlite:propia.db")));

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno,
                        "Linux",
                        entorno(Map.of("XDG_DATA_HOME", raiz.resolve("datos-xdg").toString())),
                        raiz.toString());

        assertThat(entorno.getPropertySources().contains("educhronos-ruta-datos"))
                .as("la regla de no-op manda: no se publica fuente alguna")
                .isFalse();
        assertThat(entorno.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:propia.db");
        assertThat(entorno.getProperty(RutaBaseDatosEnvironmentPostProcessor.CLAVE_CARPETA))
                .as("sin carpeta publicada no se escribe puntero al duplicar (condición 7)")
                .isNull();
    }

    /**
     * (14-bis) En la rama que SÍ calcula, la carpeta de datos se publica junto a la URL y en
     * la misma fuente. Sin esa clave, duplicar no sabría qué puntero reescribir.
     */
    @Test
    void enLaRamaQueCalculaSePublicaLaCarpetaJuntoALaUrl(@TempDir Path raiz) throws Exception {
        Path datos = raiz.resolve("datos-xdg");
        StandardEnvironment entorno = new StandardEnvironment();

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno, "Linux", entorno(Map.of("XDG_DATA_HOME", datos.toString())),
                        raiz.toString());

        Path carpeta = datos.resolve("educhronos");
        assertThat(entorno.getPropertySources().get("educhronos-ruta-datos")
                        .getProperty(RutaBaseDatosEnvironmentPostProcessor.CLAVE_CARPETA))
                .isEqualTo(carpeta.toAbsolutePath().toString());
        assertThat(entorno.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:sqlite:" + carpeta.resolve("educhronos.db"));
    }

    /**
     * (14-ter) Y con un puntero en esa carpeta, la URL publicada es la del curso apuntado:
     * el post-procesador y {@link CarpetaDatos#baseAbierta} están de verdad cableados.
     */
    @Test
    void laUrlPublicadaObedeceAlPuntero(@TempDir Path raiz) throws Exception {
        Path carpeta = Files.createDirectories(raiz.resolve("datos-xdg/educhronos"));
        Files.createFile(carpeta.resolve("curso-2026-2027.db"));
        Files.writeString(
                carpeta.resolve("curso-abierto"), "curso-2026-2027.db", StandardCharsets.UTF_8);
        StandardEnvironment entorno = new StandardEnvironment();

        new RutaBaseDatosEnvironmentPostProcessor(LOGS_INMEDIATOS)
                .postProcessEnvironment(
                        entorno,
                        "Linux",
                        entorno(Map.of("XDG_DATA_HOME", raiz.resolve("datos-xdg").toString())),
                        raiz.toString());

        assertThat(entorno.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:sqlite:" + carpeta.resolve("curso-2026-2027.db"));
    }

    /** Lectura de variables de entorno desde un mapa, como en el spec de S153. */
    private static UnaryOperator<String> entorno(Map<String, String> variables) {
        return variables::get;
    }
}
