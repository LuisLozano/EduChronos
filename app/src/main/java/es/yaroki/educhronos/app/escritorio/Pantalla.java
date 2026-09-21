package es.yaroki.educhronos.app.escritorio;

import java.awt.GraphicsEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ¿Hay de verdad un escritorio con el que hablar?
 *
 * <p><b>Esta clase existe por una medición, no por precaución.</b> En el paso 7 de S154, con
 * {@code env -u DISPLAY -u WAYLAND_DISPLAY}, la aplicación arrancó, levantó Tomcat, sirvió
 * {@code /api/jornada}… y MURIÓ al ir a abrir el navegador, con
 * {@code java.awt.AWTError: Can't connect to X11 window server using ':0.0'}. Dos cosas
 * fallaron a la vez:
 *
 * <ul>
 *   <li>{@link GraphicsEnvironment#isHeadless()} devolvió {@code false}. No miente: sólo
 *       mira la propiedad {@code java.awt.headless}, y el modo escritorio la pone a
 *       {@code false} a propósito para tener bandeja. «No estoy en modo headless» NO
 *       significa «hay pantalla».
 *   <li>Lo que sale de AWT cuando no hay servidor X es un {@link Error}
 *       —{@code AWTError}, y después {@code NoClassDefFoundError} al reintentar—, no una
 *       {@code Exception}. Un {@code catch (Exception)} lo deja pasar y se lleva por delante
 *       el proceso.
 * </ul>
 *
 * <p>Por eso aquí se PRUEBA el entorno gráfico de verdad, una sola vez, capturando
 * {@link Throwable}. Capturar {@code Throwable} es fuerte y se hace a sabiendas: el fallo
 * típico de AWT sin pantalla es un error de inicialización de clase, y la regla de toda esta
 * capa es que nada de lo accesorio —navegador, icono, ventanas— puede tumbar una aplicación
 * que ya está sirviendo horarios.
 *
 * <p>El resultado se recuerda porque el primer intento fallido deja la clase
 * {@code sun.awt.X11.XToolkit} rota para el resto de la vida de la JVM: reintentar no da otra
 * respuesta, sólo otra traza.
 */
final class Pantalla {

    private static final Logger LOG = LoggerFactory.getLogger(Pantalla.class);

    private static Boolean disponible;

    private Pantalla() {}

    /** {@code true} si se puede dibujar algo; {@code false} si hay que conformarse con el log. */
    static synchronized boolean disponible() {
        if (disponible == null) {
            disponible = comprobar();
        }
        return disponible;
    }

    private static boolean comprobar() {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.info("Sin interfaz gráfica (java.awt.headless): Educhronos no abrirá ventanas.");
            return false;
        }
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return true;
        } catch (Throwable e) {
            LOG.warn(
                    "No hay escritorio disponible ({}): Educhronos seguirá funcionando, pero sin "
                            + "navegador automático, sin icono en la bandeja y sin ventanas. Se "
                            + "usa desde {} y se cierra con SIGTERM.",
                    e.toString(),
                    Red.url(Red.PUERTO));
            return false;
        }
    }
}
