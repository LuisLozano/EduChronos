package es.yaroki.educhronos.app.escritorio;

import java.net.BindException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.boot.web.server.PortInUseException;

/**
 * Traduce el fallo de arranque a una frase que le sirva al usuario (condición 6 de
 * O-instalación).
 *
 * <p>La única distinción que se hace es la que el usuario puede ARREGLAR: el puerto ocupado.
 * Es además el fallo esperable en un ordenador de centro, donde el 8080 se lo lleva cualquier
 * otro programa. Todo lo demás —permisos, base corrupta, disco lleno— produce el mismo
 * mensaje, que no explica nada pero dice DÓNDE está escrito el detalle: inventar diagnósticos
 * a partir del tipo de excepción sería adivinar.
 *
 * <p><b>Por qué se mira la cadena entera y no la excepción de arriba.</b> Medido en el M2-A
 * de S154 sobre esta misma aplicación: con el 8080 ocupado, lo que llega arriba es
 * {@code ApplicationContextException: Failed to start bean 'webServerStartStop'}, y la
 * {@code BindException} está CUATRO causas más abajo, detrás de {@code WebServerException},
 * {@code IllegalArgumentException} y {@code LifecycleException}. Mirar sólo la causa directa
 * daría el mensaje genérico justo en el único caso que sabemos explicar.
 *
 * <p><b>Por qué se contemplan dos tipos.</b> {@link PortInUseException} es la excepción que
 * Spring Boot tiene para esto, pero en la medición citada Tomcat NO la lanzó: llegó la
 * {@link BindException} pelada del sistema operativo. Se aceptan las dos porque cuál de
 * ellas aparece depende del servidor y de la versión, y este mensaje no debería volver a
 * romperse por eso.
 */
public final class FalloArranque {

    /** Lo que se dice cuando el puerto está cogido; es el único caso accionable. */
    static final String PUERTO_OCUPADO =
            "No se puede abrir Educhronos: otro programa está usando el puerto "
                    + Red.PUERTO + ". Cierra ese programa y vuelve a intentarlo.";

    private FalloArranque() {}

    /**
     * Función PURA: mismo fallo y misma ruta, mismo texto. No lee disco ni el entorno.
     *
     * @param fallo lo que sea que haya tumbado el arranque; {@code null} se trata como fallo
     *     desconocido
     * @param rutaDelLog fichero donde está el detalle, que se nombra en el mensaje genérico
     */
    public static String mensaje(Throwable fallo, Path rutaDelLog) {
        if (esPuertoOcupado(fallo)) {
            return PUERTO_OCUPADO;
        }
        return "Educhronos no ha podido arrancar. El detalle está en: " + rutaDelLog;
    }

    /**
     * El mensaje para un fallo ANTERIOR a Spring: resolver o crear la carpeta de datos, o
     * tomar el candado de instancia única.
     *
     * <p><b>No nombra ningún log, porque todavía no hay log que nombrar</b>: el fichero lo
     * crea Spring al inicializar su sistema de logging, y en este punto Spring ni siquiera ha
     * empezado. Remitir a una ruta vacía o inexistente sería peor que no remitir a nada.
     *
     * <p>En su lugar se enseña el mensaje de la excepción tal cual. Se puede porque los
     * {@code IllegalStateException} que brotan por aquí YA están escritos para el usuario y
     * nombran la ruta y la salida: «No se puede crear la carpeta de datos de Educhronos:
     * &lt;ruta&gt;. Compruebe los permisos o arranque con --spring.datasource.url=…». Si
     * algún día llegara una excepción de otra procedencia, con un mensaje técnico, se vería
     * fea pero seguiría diciendo algo; y si no trae mensaje, se da el nombre de la clase,
     * que es lo único que queda y es mejor que una frase que se corta.
     *
     * @param fallo lo que impidió llegar a Spring; {@code null} se trata como desconocido
     */
    public static String mensajeAntesDeSpring(Throwable fallo) {
        return "Educhronos no ha podido arrancar: " + detalle(fallo);
    }

    private static String detalle(Throwable fallo) {
        if (fallo == null) {
            return "causa desconocida.";
        }
        String mensaje = fallo.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return fallo.getClass().getName();
        }
        return mensaje;
    }

    /**
     * Recorre la cadena de causas buscando un fallo de puerto.
     *
     * <p>La protección contra ciclos no es un adorno: una excepción puede tenerse a sí misma
     * por causa, o dos pueden apuntarse mutuamente, y entonces esto sería un bucle infinito
     * dentro del manejador de errores del arranque —o sea, un programa colgado en lugar de un
     * mensaje—. Se lleva cuenta de las ya vistas POR IDENTIDAD, no por {@code equals}: dos
     * excepciones distintas pueden ser iguales y no por eso hay ciclo.
     */
    static boolean esPuertoOcupado(Throwable fallo) {
        Set<Throwable> vistas = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable actual = fallo; actual != null; actual = actual.getCause()) {
            if (!vistas.add(actual)) {
                return false;
            }
            if (actual instanceof PortInUseException || actual instanceof BindException) {
                return true;
            }
        }
        return false;
    }
}
