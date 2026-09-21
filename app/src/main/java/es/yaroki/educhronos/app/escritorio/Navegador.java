package es.yaroki.educhronos.app.escritorio;

import java.awt.Desktop;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abre Educhronos en el navegador del usuario (condiciones 4 y 6 de O-instalación): es lo
 * que hace visible que la aplicación ha arrancado, y también lo que hace que relanzarla
 * devuelva a la pestaña en vez de abrir un segundo programa.
 *
 * <p><b>Si no se puede abrir, se dice; no se falla.</b> {@link Desktop} no está soportado en
 * cualquier sitio —sin escritorio, sin {@code java.desktop}, o con un
 * {@code xdg-open} que no encuentra navegador—, y que el navegador no abra NO es motivo para
 * tumbar una aplicación que está perfectamente arrancada y sirviendo. El respaldo es decir
 * la URL, que es lo único que le falta al usuario para entrar.
 */
public final class Navegador {

    private static final Logger LOG = LoggerFactory.getLogger(Navegador.class);

    private Navegador() {}

    /**
     * Abre {@code http://127.0.0.1:<puerto>}; si no hay manera, muestra la URL.
     *
     * @param puerto puerto real en el que está escuchando la aplicación
     */
    public static void abrir(int puerto) {
        String url = Red.url(puerto);
        if (!Pantalla.disponible()) {
            // Sin escritorio no hay navegador que abrir NI ventana que mostrar. Se dice por
            // el log y se sigue: la aplicación está sirviendo y se usa desde esa URL.
            LOG.info("Educhronos está sirviendo en {} (sin escritorio, no se abre solo).", url);
            return;
        }
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                LOG.info("Educhronos abierto en el navegador: {}", url);
                return;
            }
            LOG.warn("Este sistema no sabe abrir el navegador (Desktop.BROWSE no soportado).");
        } catch (Throwable e) {
            // Cualquier cosa: IOException del lanzador, UnsupportedOperationException,
            // SecurityException, o el AWTError que MIDIÓ el paso 7 de S154 —un Error, que un
            // catch (Exception) dejaba escapar hasta matar el proceso—. Ninguna justifica
            // tumbar la aplicación, así que se degrada a decir la URL.
            LOG.warn("No se ha podido abrir el navegador en {}: {}", url, e.toString());
        }
        Dialogos.informacion(mensajeDeRespaldo(puerto));
    }

    /** El texto que se muestra cuando el navegador no abre solo. */
    static String mensajeDeRespaldo(int puerto) {
        return "Educhronos está abierto. Ábrelo en tu navegador en " + Red.url(puerto);
    }
}
