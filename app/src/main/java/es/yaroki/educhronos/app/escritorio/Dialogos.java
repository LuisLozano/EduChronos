package es.yaroki.educhronos.app.escritorio;

import java.awt.HeadlessException;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lo poco que Educhronos le dice al usuario por ventana: que ya está abierto, o que no ha
 * podido arrancar (condiciones 4 y 6 de O-instalación).
 *
 * <p><b>Nunca cuelga un proceso sin pantalla.</b> Es el requisito que manda aquí. Un
 * {@code JOptionPane} es modal: en una máquina sin escritorio —un servidor, un contenedor, o
 * esta misma sesión con {@code env -u DISPLAY -u WAYLAND_DISPLAY}— bloquearía el arranque sin
 * que nadie pueda cerrarlo. Por eso hay TRES guardas: {@link Pantalla#disponible()} antes de
 * tocar Swing, la captura de {@link HeadlessException} y —porque lo exigió la medición del
 * paso 7 de S154— la de {@link Throwable}, ya que lo que AWT lanza sin servidor X es un
 * {@code Error}. Sin pantalla el mensaje va a {@code stderr} Y al log, que es donde alguien
 * puede leerlo después.
 *
 * <p>Esta es la primera clase de producción del proyecto con un logger propio. Va por
 * {@code org.slf4j}, que es lo que trae {@code spring-boot-starter-logging} y lo que Boot usa
 * internamente. El {@code commons-logging} del post-procesador de la ruta es la excepción, y
 * por un motivo escrito allí: corre antes de que el sistema de logging exista.
 */
public final class Dialogos {

    private static final Logger LOG = LoggerFactory.getLogger(Dialogos.class);

    /** Título de las ventanas, para que se reconozcan en la barra de tareas. */
    static final String TITULO = "Educhronos";

    private Dialogos() {}

    /** Un aviso corriente. Sin pantalla, se escribe y se sigue. */
    public static void informacion(String mensaje) {
        mostrar(mensaje, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Un fallo. Sin pantalla, se escribe y se sigue: quien llama decide si termina. */
    public static void error(String mensaje) {
        mostrar(mensaje, JOptionPane.ERROR_MESSAGE);
    }

    private static void mostrar(String mensaje, int tipo) {
        if (!Pantalla.disponible()) {
            sinPantalla(mensaje);
            return;
        }
        try {
            JOptionPane.showMessageDialog(null, mensaje, TITULO, tipo);
        } catch (HeadlessException e) {
            sinPantalla(mensaje);
        } catch (Throwable e) {
            // MEDIDO en el paso 7 de S154: sin servidor X esto llega como AWTError y como
            // NoClassDefFoundError, que son Error y no Exception. Con un catch más estrecho
            // el proceso se muere AQUÍ, dentro del manejador de errores del arranque, que es
            // el peor sitio posible para morir.
            sinPantalla(mensaje);
            LOG.warn("El entorno gráfico falló al mostrar el mensaje: {}", e.toString());
        }
    }

    /**
     * Sin escritorio el mensaje va a los dos sitios a propósito: a {@code stderr} para que lo
     * vea quien lanzó el proceso desde una consola, y al log para que quede cuando nadie
     * mira. Uno solo no basta: el bundle no tiene consola, y el log puede no existir todavía
     * si el fallo es del arranque.
     */
    private static void sinPantalla(String mensaje) {
        System.err.println(mensaje);
        LOG.warn("Sin pantalla; el mensaje no se ha mostrado en ventana: {}", mensaje);
    }
}
