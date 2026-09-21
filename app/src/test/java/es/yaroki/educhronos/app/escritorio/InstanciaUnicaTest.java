package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Que no se abran dos Educhronos a la vez (condición 4 de O-instalación).
 *
 * <p>El caso que importa es ENTRE PROCESOS, y por eso este spec lanza una JVM de verdad: el
 * candado lo lleva el sistema operativo, de modo que dos hilos de la misma JVM no prueban
 * nada de lo que el usuario va a hacer, que es pinchar dos veces en el icono. La JVM hija
 * corre {@link TomadorDeCandado} con el {@code java} de esta misma instalación y el
 * classpath de esta misma corrida.
 *
 * <p><b>Sobre el classpath y surefire.</b> {@code java.class.path} bajo surefire suele ser UN
 * solo jar —el «booter», que no trae clases sino un {@code Class-Path} en su manifiesto—.
 * Pasarlo con {@code -cp} funciona igual, porque la JVM hija sigue ese manifiesto; el caso
 * (1) lo comprueba de la única forma que vale: si el andamio no funcionara, la hija no
 * llegaría a imprimir su línea y el caso caería por el tiempo de espera, no por un falso
 * verde.
 */
class InstanciaUnicaTest {

    /** Lo que se espera a que la hija diga que ya tiene el candado. */
    private static final Duration ESPERA = Duration.ofSeconds(30);

    /**
     * (1) Con OTRO PROCESO dueño del candado, esta JVM no lo consigue; y en cuanto ese
     * proceso muere, lo consigue. Las dos mitades importan: la primera es la condición 4, y
     * la segunda es que el candado no quede huérfano, que sería un defecto peor —un
     * Educhronos que ya no se puede abrir hasta borrar un fichero a mano—.
     */
    @Test
    void conOtroProcesoDuenoDelCandadoNoSeObtieneYAlMorirSeObtiene(@TempDir Path carpeta)
            throws Exception {
        Process hija = lanzarTomador(carpeta);
        try {
            assertThat(esperarPrimeraLinea(hija))
                    .as("la JVM hija tiene que haberse hecho con el candado")
                    .isEqualTo(TomadorDeCandado.LISTO);

            assertThat(InstanciaUnica.intentarTomar(carpeta))
                    .as("con otro proceso dentro, NO se entra")
                    .isFalse();
        } finally {
            hija.destroyForcibly();
            hija.waitFor(ESPERA.toSeconds(), TimeUnit.SECONDS);
        }

        assertThat(hija.isAlive()).as("la hija ya no está").isFalse();
        assertThat(InstanciaUnica.intentarTomar(carpeta))
                .as("muerta la otra instancia, el candado queda libre")
                .isTrue();
    }

    /**
     * (2) Dentro de la MISMA JVM el segundo intento tampoco entra. No es el caso del usuario,
     * pero sí el de un arranque doble por programa, y el sistema de cerrojos de Java lo
     * señala con {@code OverlappingFileLockException} en vez de devolver «ocupado»: si esa
     * excepción no se tratara, saldría propagada desde {@code main} y el segundo arranque
     * moriría con una traza en vez de con una pestaña.
     */
    @Test
    void dentroDeLaMismaJvmElSegundoIntentoNoEntra(@TempDir Path carpeta) {
        assertThat(InstanciaUnica.intentarTomar(carpeta))
                .as("el primero sí")
                .isTrue();

        assertThat(InstanciaUnica.intentarTomar(carpeta))
                .as("el segundo no, y sin que se escape la OverlappingFileLockException")
                .isFalse();
    }

    /** (3) El candado es un fichero con nombre fijo dentro de la carpeta de datos. */
    @Test
    void elCandadoEsUnFicheroDentroDeLaCarpetaDeDatos(@TempDir Path carpeta) {
        InstanciaUnica.intentarTomar(carpeta);

        assertThat(carpeta.resolve("educhronos.lock"))
                .as("nombre fijo: lo miran el usuario y el paso de verificación")
                .isRegularFile();
        assertThat(InstanciaUnica.NOMBRE_FICHERO).isEqualTo("educhronos.lock");
    }

    private static Process lanzarTomador(Path carpeta) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return new ProcessBuilder(
                        java,
                        "-cp",
                        System.getProperty("java.class.path"),
                        TomadorDeCandado.class.getName(),
                        carpeta.toString())
                .redirectErrorStream(true)
                .start();
    }

    /**
     * Lee la primera línea de la hija sin quedarse colgado para siempre: la lectura va en un
     * hilo aparte y aquí se espera con tope. Si la hija no arrancó —classpath mal armado, por
     * ejemplo— esto devuelve lo que haya escrito, que es lo que hace falta para entender el
     * fallo en vez de ver un tiempo de espera pelado.
     */
    private static String esperarPrimeraLinea(Process hija) throws Exception {
        StringBuilder todo = new StringBuilder();
        Thread lector = new Thread(() -> {
            try (BufferedReader salida = new BufferedReader(
                    new InputStreamReader(hija.getInputStream(), StandardCharsets.UTF_8))) {
                String linea = salida.readLine();
                if (linea != null) {
                    todo.append(linea);
                }
            } catch (IOException e) {
                todo.append("fallo leyendo la salida de la hija: ").append(e);
            }
        });
        lector.setDaemon(true);
        lector.start();
        lector.join(ESPERA.toMillis());
        return todo.toString();
    }
}
