package es.yaroki.educhronos.app.escritorio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * El puerto y la dirección en los que escucha Educhronos, en UN solo sitio.
 *
 * <p>Hasta S154 el 8080 no estaba escrito en ninguna parte del código: era el valor por
 * defecto de Spring Boot y aparecía suelto en el {@code playwright.config.ts} y en el guion
 * de Windows (medido en el M2-A de esta misma sesión: cero apariciones en {@code app/src}).
 * El modo escritorio obliga a nombrarlo, porque tiene que construir la URL que abre el
 * navegador ANTES de que exista servidor al que preguntársela —el caso de la segunda
 * instancia, que ni levanta contexto—.
 *
 * <p>La dirección va emparejada con {@code server.address} del
 * {@code application.properties} (condición 8): lo que hay aquí es la misma decisión vista
 * desde el cliente. Si una cambia, la otra también.
 */
public final class Red {

    /** Puerto en el que escucha la aplicación; es también el defecto de Spring Boot. */
    public static final int PUERTO = 8080;

    /** Dirección de escucha: sólo el bucle local (condición 8 de O-instalación). */
    public static final String DIRECCION_LOCAL = "127.0.0.1";

    /** Cada cuánto se vuelve a intentar mientras se espera a que alguien escuche. */
    private static final Duration SONDEO = Duration.ofMillis(250);

    /** Tope de cada intento suelto. Corto: sobre el bucle local, o entra ya o no hay nadie. */
    private static final Duration INTENTO = Duration.ofMillis(250);

    private Red() {}

    /** La URL con la que se abre Educhronos en el navegador. */
    public static String url(int puerto) {
        return "http://" + DIRECCION_LOCAL + ":" + puerto;
    }

    /**
     * Espera a que alguien acepte conexiones TCP en {@code 127.0.0.1:puerto}.
     *
     * <p>Lo usa la segunda instancia antes de abrir el navegador. Comprueba el PUERTO, no la
     * aplicación: un {@code connect} que entra sólo dice que hay un socket escuchando, no
     * que {@code /api/jornada} responda 200. Es lo que hace falta aquí y es lo único que se
     * puede hacer barato — pedir un HTTP metería un cliente y un cuerpo de respuesta en un
     * camino que sólo quiere saber si vale la pena abrir una pestaña.
     *
     * <p>Sin efectos de escritorio a propósito: ni abre, ni avisa, ni registra nada. Devuelve
     * un booleano y ya. Así se puede probar con un {@code ServerSocket} de verdad en un
     * puerto efímero, que es como está probado.
     *
     * @param puerto puerto a sondear; entra por parámetro para poder probarlo
     * @param espera cuánto se insiste antes de rendirse
     * @return {@code true} si en algún momento aceptó la conexión
     */
    public static boolean esperarEscucha(int puerto, Duration espera) {
        long limite = System.nanoTime() + espera.toNanos();
        while (true) {
            if (aceptaConexion(puerto)) {
                return true;
            }
            if (System.nanoTime() >= limite) {
                return false;
            }
            try {
                Thread.sleep(SONDEO.toMillis());
            } catch (InterruptedException e) {
                // Que interrumpan este hilo significa que el proceso se está yendo. Se
                // restaura la marca —quien mande arriba tiene que poder verla— y se contesta
                // lo único cierto: no, no ha llegado a escuchar.
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static boolean aceptaConexion(int puerto) {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(DIRECCION_LOCAL, puerto), (int) INTENTO.toMillis());
            return true;
        } catch (IOException e) {
            // Rechazada, agotada o inalcanzable: para lo que aquí se pregunta, las tres son
            // «todavía no».
            return false;
        }
    }
}
