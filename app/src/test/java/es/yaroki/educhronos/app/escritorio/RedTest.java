package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * La espera a que alguien escuche, que es de lo que vive el doble clic (condición 4 de
 * O-instalación): una segunda instancia no abre el navegador hasta que la primera acepta
 * conexiones.
 *
 * <p>Se prueba con {@link ServerSocket} de verdad sobre 127.0.0.1 y puertos EFÍMEROS —los
 * que da el sistema al atar el puerto 0—, nunca el 8080. Un puerto fijo en la suite es una
 * cita a ciegas con lo que esté corriendo en la máquina: si alguien tuviera Educhronos
 * abierto, el caso del puerto cerrado daría verde por el motivo equivocado.
 */
class RedTest {

    /** (1) Con alguien escuchando, se entera enseguida. */
    @Test
    void conAlguienEscuchandoDevuelveVerdadero() throws IOException {
        try (ServerSocket servidor = servidorLocal()) {
            boolean escucha = Red.esperarEscucha(servidor.getLocalPort(), Duration.ofSeconds(5));

            assertThat(escucha).isTrue();
        }
    }

    /**
     * (2) Con el puerto CERRADO se rinde y devuelve falso. El puerto se obtiene atando un
     * efímero y cerrándolo: así es un puerto real que nadie usa, y no un número inventado
     * que podría estar ocupado por cualquier cosa.
     *
     * <p>La espera es de 1 s para que el caso sea rápido, y se comprueba que la llamada NO se
     * pasa de ahí: el tope tiene que ser un tope, no una sugerencia.
     */
    @Test
    void conElPuertoCerradoDevuelveFalsoYRespetaElTope() throws IOException {
        int puertoLibre = puertoQueNadieUsa();
        long antes = System.nanoTime();

        boolean escucha = Red.esperarEscucha(puertoLibre, Duration.ofSeconds(1));

        long milisegundos = (System.nanoTime() - antes) / 1_000_000;
        assertThat(escucha).isFalse();
        assertThat(milisegundos)
                .as("se rinde al agotar la espera, sin pasarse")
                .isBetween(900L, 5_000L);
    }

    /**
     * (3) EL CASO DEL DOBLE CLIC. Cuando se empieza a esperar no hay nadie escuchando; el
     * servidor aparece medio segundo después y la espera tiene que enterarse. Sin el sondeo
     * —si sólo se mirase una vez, al principio— este caso daría falso.
     */
    @Test
    void siElServidorApareceMasTardeTambienSeEntera() throws Exception {
        int puerto = puertoQueNadieUsa();
        Thread tardio = new Thread(() -> {
            try {
                Thread.sleep(500);
                try (ServerSocket servidor = new ServerSocket()) {
                    servidor.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), puerto));
                    Thread.sleep(3_000);
                }
            } catch (Exception e) {
                // El caso caerá por su propio aserto, que dice más que reventar aquí.
            }
        });
        tardio.setDaemon(true);
        tardio.start();

        boolean escucha = Red.esperarEscucha(puerto, Duration.ofSeconds(10));

        assertThat(escucha).as("apareció a los 500 ms y la espera lo vio").isTrue();
    }

    private static ServerSocket servidorLocal() throws IOException {
        ServerSocket servidor = new ServerSocket();
        servidor.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
        return servidor;
    }

    /** Ata un puerto efímero, anota el número y lo suelta: queda libre y es real. */
    private static int puertoQueNadieUsa() throws IOException {
        try (ServerSocket efimero = servidorLocal()) {
            return efimero.getLocalPort();
        }
    }
}
