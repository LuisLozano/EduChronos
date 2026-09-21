package es.yaroki.educhronos.app.escritorio;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Impide que se abran dos Educhronos a la vez (condición 4 de O-instalación).
 *
 * <p><b>Por qué un candado de fichero y no el puerto.</b> Mirar si el 8080 responde diría
 * «hay algo escuchando», no «hay otro Educhronos»: cualquier otro programa en ese puerto
 * daría el mismo positivo, y además la ventana entre comprobar y arrancar deja pasar dos
 * instancias lanzadas a la vez. El candado del sistema operativo no tiene esa ventana: o lo
 * tienes o no.
 *
 * <p><b>Por qué el canal y el candado NO se cierran nunca.</b> Los guarda un campo estático
 * fuerte durante toda la vida del proceso, a propósito, para que nadie —ni el recolector, ni
 * un {@code close} bienintencionado— los suelte mientras la aplicación vive. Liberarlos es
 * trabajo del sistema operativo cuando el proceso muere, y lo hace también si lo matan a la
 * brava: medido en el paso 7d de S154 con {@code kill -KILL}, tras el cual el siguiente
 * arranque toma el candado sin problema. Un candado huérfano que obligara a borrar un
 * fichero a mano sería peor defecto que el que esto arregla.
 */
public final class InstanciaUnica {

    /** Fichero del candado, dentro de la carpeta de datos, junto a la base y al log. */
    public static final String NOMBRE_FICHERO = "educhronos.lock";

    /**
     * Canal y candado del dueño. Estáticos y fuertes: ver la nota de clase. No se tocan más
     * que aquí dentro, y sólo para ponerlos una vez.
     */
    private static FileChannel canalDelDueno;

    private static FileLock candadoDelDueno;

    private InstanciaUnica() {}

    /**
     * Intenta hacerse con el candado de la carpeta dada.
     *
     * @param carpeta carpeta de datos, que ya tiene que existir
     * @return {@code true} si esta JVM es ahora la dueña; {@code false} si ya hay otra
     *     instancia, venga de otro proceso o de esta misma JVM
     * <p>No hay atajo por el campo estático —«si ya lo tengo, devuelve false»— a propósito:
     * eso ataría la respuesta a la PRIMERA carpeta que se hubiera pedido en la JVM y haría
     * que un intento sobre otra distinta fallara sin mirarla. Quien decide es el sistema de
     * cerrojos, siempre, sobre el fichero que toque.
     *
     * @throws IllegalStateException si el fichero del candado no se puede ni abrir, que no es
     *     «está ocupado» sino un problema de permisos o de disco y merece decirse
     */
    public static synchronized boolean intentarTomar(Path carpeta) {
        Path fichero = carpeta.resolve(NOMBRE_FICHERO);
        FileChannel canal;
        try {
            canal = FileChannel.open(
                    fichero, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se puede abrir el fichero de control de Educhronos: " + fichero, e);
        }
        try {
            FileLock candado = canal.tryLock();
            if (candado == null) {
                // Lo tiene OTRO proceso. Este canal no llegó a poner ningún cerrojo, así que
                // cerrarlo no suelta nada de nadie.
                cerrarSinRuido(canal);
                return false;
            }
            canalDelDueno = canal;
            candadoDelDueno = candado;
            return true;
        } catch (OverlappingFileLockException e) {
            // Lo tiene ESTA MISMA JVM por otro canal. El canal recién abierto se queda
            // ABIERTO a propósito: en Linux los cerrojos son POSIX y cerrar CUALQUIER
            // descriptor del mismo fichero suelta los del proceso entero, de modo que un
            // `close` de cortesía aquí le quitaría el candado al dueño legítimo.
            return false;
        } catch (IOException e) {
            cerrarSinRuido(canal);
            throw new IllegalStateException(
                    "No se puede comprobar si ya hay otro Educhronos abierto: " + fichero, e);
        }
    }

    private static void cerrarSinRuido(FileChannel canal) {
        try {
            canal.close();
        } catch (IOException e) {
            // No hay nada que hacer ni nada que informar: el canal no tenía cerrojo.
        }
    }
}
