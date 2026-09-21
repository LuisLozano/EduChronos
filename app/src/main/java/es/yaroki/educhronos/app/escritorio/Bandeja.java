package es.yaroki.educhronos.app.escritorio;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * El icono junto al reloj: la forma que tiene el usuario de volver a la pestaña y, sobre
 * todo, de CERRAR Educhronos sin el Administrador de tareas (condición 5 de O-instalación).
 *
 * <p><b>Por qué la bandeja y no una ventana.</b> La aplicación es un servidor web que se usa
 * desde el navegador: no tiene ventana propia que cerrar. Sin un icono, la única forma de
 * pararla es matar el proceso, que es exactamente lo que la condición 5 prohíbe pedirle al
 * usuario.
 *
 * <p><b>Si no hay bandeja, se sigue.</b> {@link SystemTray#isSupported()} es falso en buena
 * parte de los escritorios de Linux modernos —GNOME la quitó— y en cualquier sesión sin
 * pantalla. Quedarse sin icono empeora el cierre, pero no impide usar el programa, así que se
 * avisa por el log NOMBRANDO la alternativa y se continúa. Medido en el paso 7a de S154: sin
 * {@code DISPLAY} sale por aquí.
 */
public final class Bandeja {

    private static final Logger LOG = LoggerFactory.getLogger(Bandeja.class);

    /** Lo que se lee al pasar el ratón por encima. */
    static final String TOOLTIP = "Educhronos";

    /** Lado del icono, en píxeles. 16 es lo que piden casi todas las bandejas. */
    private static final int LADO = 16;

    /** El icono instalado, para poder quitarlo al cerrarse el contexto. */
    private static TrayIcon iconoInstalado;

    private Bandeja() {}

    /**
     * Pone el icono, si este sistema tiene bandeja.
     *
     * @param contexto contexto ya arrancado, que es lo que «Salir» cierra
     * @param puerto puerto real, para que «Abrir Educhronos» vaya al sitio correcto
     */
    public static synchronized void instalar(ConfigurableApplicationContext contexto, int puerto) {
        if (!Pantalla.disponible() || !soportada()) {
            LOG.warn(
                    "Este escritorio no tiene bandeja de sistema: Educhronos no puede poner su "
                            + "icono, así que para cerrarlo hay que parar el proceso (SIGTERM, o "
                            + "Ctrl+C si lo lanzaste desde una consola).");
            return;
        }
        PopupMenu menu = new PopupMenu();

        MenuItem abrir = new MenuItem("Abrir Educhronos");
        abrir.addActionListener(evento -> Navegador.abrir(puerto));
        menu.add(abrir);

        MenuItem salir = new MenuItem("Salir");
        salir.addActionListener(evento -> salir(contexto));
        menu.add(salir);

        TrayIcon icono = new TrayIcon(dibujarIcono(), TOOLTIP, menu);
        icono.setImageAutoSize(true);
        icono.addActionListener(evento -> Navegador.abrir(puerto));
        try {
            SystemTray.getSystemTray().add(icono);
            iconoInstalado = icono;
            LOG.info("Educhronos está en la bandeja del sistema. Para cerrarlo: Salir.");
        } catch (AWTException e) {
            LOG.warn("No se ha podido poner el icono en la bandeja: {}", e.toString());
        }
    }

    /**
     * {@link SystemTray#isSupported()} por sí solo puede reventar con un {@link Error} si el
     * entorno gráfico no está: medido en el paso 7 de S154 con AWT sin servidor X.
     */
    private static boolean soportada() {
        try {
            return SystemTray.isSupported();
        } catch (Throwable e) {
            LOG.warn("No se ha podido consultar la bandeja del sistema: {}", e.toString());
            return false;
        }
    }

    /** Quita el icono. Se llama al cerrarse el contexto, venga el cierre de donde venga. */
    public static synchronized void quitar() {
        if (iconoInstalado == null) {
            return;
        }
        try {
            SystemTray.getSystemTray().remove(iconoInstalado);
            LOG.info("Icono de la bandeja retirado.");
        } catch (Throwable e) {
            LOG.warn("No se ha podido retirar el icono de la bandeja: {}", e.toString());
        }
        iconoInstalado = null;
    }

    /**
     * Cierra la aplicación entera.
     *
     * <p><b>Por qué un hilo aparte y por qué NO daemon.</b> Esto se dispara en el hilo de
     * AWT, y cerrar el contexto de Spring desde ahí bloquea el escritorio del usuario
     * mientras Tomcat termina sus peticiones y Hikari cierra el pool —y si algo de ese cierre
     * necesitara al hilo de AWT, se quedarían esperándose el uno al otro—. En un hilo propio
     * el escritorio queda libre al instante. Y NO daemon porque un hilo daemon no impide que
     * la JVM muera: si el resto de hilos terminasen antes, el proceso se iría a mitad del
     * cierre ordenado, que es justo lo que se quiere evitar.
     *
     * <p>{@link SpringApplication#exit} cierra el contexto y calcula el código de salida
     * consultando a los {@code ExitCodeGenerator} que haya; {@link System#exit} lo usa. Así
     * el cierre por «Salir» pasa por el mismo apagado ordenado que un SIGTERM.
     */
    private static void salir(ConfigurableApplicationContext contexto) {
        Thread cierre = new Thread(() -> System.exit(SpringApplication.exit(contexto)), "educhronos-salir");
        cierre.setDaemon(false);
        cierre.start();
    }

    /**
     * El icono, dibujado en código: un cuadro redondeado con una «E».
     *
     * <p>Se dibuja en vez de cargarse de un recurso para no meter un binario en el jar por
     * 16x16 píxeles, y porque un PNG suelto habría que versionarlo, empaquetarlo y volver a
     * mirar la condición 1 del empaquetado, que cuenta lo que hay dentro del jar.
     */
    static Image dibujarIcono() {
        BufferedImage imagen = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lapiz = imagen.createGraphics();
        lapiz.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lapiz.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        lapiz.setColor(new Color(0x1F, 0x4E, 0x79));
        lapiz.fillRoundRect(0, 0, LADO, LADO, 4, 4);
        lapiz.setColor(Color.WHITE);
        lapiz.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lapiz.drawString("E", 4, 12);
        lapiz.dispose();
        return imagen;
    }
}
