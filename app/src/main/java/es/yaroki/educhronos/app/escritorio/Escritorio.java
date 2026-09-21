package es.yaroki.educhronos.app.escritorio;

import es.yaroki.educhronos.app.EduchronosApplication;
import es.yaroki.educhronos.app.config.CarpetaDatos;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * El arranque del bundle de escritorio: instancia única, log en fichero, navegador y bandeja
 * (condiciones 4, 5 y 6 de O-instalación).
 *
 * <p>Sólo se entra aquí cuando {@link ModoEscritorio} está activo. El arranque de desarrollo
 * —{@code java -jar}, {@code mvn spring-boot:run}, el e2e y toda la suite— no pasa por esta
 * clase y se comporta exactamente igual que antes de S154.
 */
public final class Escritorio {

    private static final Logger LOG = LoggerFactory.getLogger(Escritorio.class);

    /** Fichero de log, dentro de la carpeta de datos, junto a la base y al candado. */
    static final String NOMBRE_LOG = "educhronos.log";

    /** Clave con la que Spring Boot decide escribir el log en un fichero. */
    static final String CLAVE_FICHERO_LOG = "logging.file.name";

    /**
     * Cuánto espera una segunda instancia a que la primera empiece a servir. 60 s es
     * holgado a propósito: el arranque medido en esta máquina es de unos 6 s, pero el
     * ordenador de un centro con un antivirus de por medio puede tardar bastante más, y lo
     * que se juega es una pestaña fallida contra esperar de más.
     */
    static final Duration ESPERA_A_LA_OTRA_INSTANCIA = Duration.ofSeconds(60);

    private Escritorio() {}

    /**
     * Arranca en modo escritorio. No vuelve nunca si había otra instancia o si el arranque
     * falla: en los dos casos termina el proceso con su código.
     *
     * @param args los argumentos de la línea de órdenes, que se pasan tal cual a Spring
     */
    public static void arrancar(String[] args) {
        Path carpeta;
        try {
            carpeta = CarpetaDatos.resolverYCrear();
            if (!InstanciaUnica.intentarTomar(carpeta)) {
                volverALaInstanciaAbierta();
                return;
            }
        } catch (Throwable e) {
            // TODO lo anterior a Spring: resolver la carpeta, crearla, comprobar que se
            // puede escribir, tomar el candado. Si algo de esto revienta, hasta S154 salía
            // una traza por una consola que el bundle NO TIENE y el usuario veía un programa
            // que no hace nada al pincharlo. Ahora se comporta igual que un fallo de Spring:
            // un diálogo y código 1. Aquí no se puede nombrar el log porque todavía no
            // existe; ver FalloArranque.mensajeAntesDeSpring.
            LOG.error("Educhronos no ha podido arrancar (antes de Spring).", e);
            Dialogos.error(FalloArranque.mensajeAntesDeSpring(e));
            System.exit(1);
            return;
        }

        Path ficheroDeLog = carpeta.resolve(NOMBRE_LOG);
        SpringApplication aplicacion = new SpringApplication(EduchronosApplication.class);

        // Con pantalla: lo contrario del defecto de Spring Boot, que fuerza
        // java.awt.headless=true y dejaría sin bandeja y sin diálogos a todo el modo
        // escritorio.
        aplicacion.setHeadless(false);

        // Por defecto y no como propiedad fija, para que un --logging.file.name en la línea
        // de órdenes siga mandando. Los defaults entran en el entorno dentro de
        // prepareEnvironment, o sea ANTES de que el sistema de logging se inicialice, que es
        // lo que hace que el fichero llegue a existir.
        aplicacion.setDefaultProperties(Map.of(CLAVE_FICHERO_LOG, ficheroDeLog.toString()));

        aplicacion.addListeners(
                (ApplicationReadyEvent evento) -> sinTumbarLaAplicacion(() -> alEstarLista(evento)),
                (ContextClosedEvent evento) -> sinTumbarLaAplicacion(Bandeja::quitar));

        try {
            aplicacion.run(args);
        } catch (Throwable e) {
            LOG.error("Educhronos no ha podido arrancar.", e);
            Dialogos.error(FalloArranque.mensaje(e, ficheroDeLog));
            System.exit(1);
        }
    }

    /**
     * Ya hay un Educhronos abierto: se devuelve al usuario a su pestaña y se termina con 0.
     *
     * <p>Ni se crea contexto, ni se toca la base, ni se escribe en el log del que está
     * corriendo. Sale con 0 porque esto NO es un error: el usuario ha pedido abrir
     * Educhronos y Educhronos se abre.
     *
     * <p><b>Se espera a que el otro escuche antes de abrir el navegador.</b> El caso que lo
     * obliga es el doble clic: dos instancias lanzadas con un segundo de diferencia. La
     * primera tarda unos 6 s en levantar Tomcat, así que la segunda gana el candado-carrera
     * y abriría el navegador contra un puerto muerto —una pestaña con «no se puede
     * conectar», que es peor que no abrir nada—. Si tras la espera sigue sin responder, se
     * dice y se sale igualmente con 0: el otro proceso existe, el usuario no tiene que hacer
     * nada distinto salvo esperar.
     */
    private static void volverALaInstanciaAbierta() {
        if (Red.esperarEscucha(Red.PUERTO, ESPERA_A_LA_OTRA_INSTANCIA)) {
            Navegador.abrir(Red.PUERTO);
        } else {
            LOG.warn(
                    "Hay otro Educhronos abierto, pero no responde en {} tras {} s.",
                    Red.url(Red.PUERTO),
                    ESPERA_A_LA_OTRA_INSTANCIA.toSeconds());
            Dialogos.informacion(
                    "Educhronos ya se está abriendo, pero todavía no responde. Espera un "
                            + "momento y vuelve a abrirlo.");
        }
        System.exit(0);
    }

    /**
     * Corre un adorno del escritorio y se traga lo que pase.
     *
     * <p><b>Esto lo obligó una medición, no una manía.</b> En el paso 7 de S154 el listener
     * de {@code ApplicationReadyEvent} dejó escapar un {@code AWTError} y Spring lo trató
     * como lo que parecía: un arranque fallido. Resultado: Tomcat ya había levantado, la
     * aplicación ya servía {@code /api/jornada}… y aun así el proceso se apagó entero y se
     * murió. La regla es al revés: una vez que la aplicación sirve, NADA de esta capa
     * —navegador, icono, ventanas— puede tumbarla. Lo peor que puede pasar es quedarse sin
     * adornos, y eso se dice por el log.
     */
    private static void sinTumbarLaAplicacion(Runnable adorno) {
        try {
            adorno.run();
        } catch (Throwable e) {
            LOG.warn("Fallo en la capa de escritorio, la aplicación sigue en pie: {}", e.toString());
        }
    }

    /**
     * Ya está sirviendo: se abre el navegador y se pone el icono, en ese orden. Primero lo
     * que el usuario está esperando ver.
     */
    private static void alEstarLista(ApplicationReadyEvent evento) {
        int puerto = puertoReal(evento.getApplicationContext());
        // CADA UNO con su propia red, y no los dos bajo la misma. Si el navegador falla
        // —que es lo que pasa sin escritorio, y lo que MIDIÓ el paso 7— con una sola red
        // compartida se perdería también el icono, que es justamente la otra forma de abrir
        // la aplicación y la única de cerrarla. Son independientes y se instalan por
        // separado.
        sinTumbarLaAplicacion(() -> Navegador.abrir(puerto));
        sinTumbarLaAplicacion(() -> Bandeja.instalar(evento.getApplicationContext(), puerto));
    }

    /**
     * El puerto en el que Tomcat escucha DE VERDAD.
     *
     * <p>Se pregunta al servidor y no a la configuración porque son cosas distintas: con
     * {@code server.port=0} la propiedad vale cero y el servidor escucha en uno cualquiera.
     * Verificado en los jars de Boot 4.1 durante S154: la propiedad {@code local.server.port}
     * NO existe en producción —sólo la publica {@code spring-boot-test} para los
     * {@code @SpringBootTest} de puerto aleatorio—, así que la vía viva es esta.
     *
     * <p>Si el contexto no fuese de servidor web, se cae al puerto configurado, que es lo
     * único sensato que se puede decir.
     */
    static int puertoReal(ApplicationContext contexto) {
        if (contexto instanceof WebServerApplicationContext web && web.getWebServer() != null) {
            return web.getWebServer().getPort();
        }
        return Red.PUERTO;
    }
}
