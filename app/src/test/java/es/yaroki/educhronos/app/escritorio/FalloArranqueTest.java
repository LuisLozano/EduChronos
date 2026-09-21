package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.BindException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.PortInUseException;

/**
 * Qué se le dice al usuario cuando Educhronos no arranca (condición 6 de O-instalación).
 *
 * <p>La frase del puerto ocupado es el único diagnóstico que el programa se atreve a dar, y
 * lo da porque el usuario puede actuar sobre él. Lo que estos casos fijan es que se dé
 * CUANDO toca —por hondo que esté el fallo en la cadena— y no cuando no toca.
 */
class FalloArranqueTest {

    private static final Path LOG = Path.of("/datos/educhronos/educhronos.log");

    /** (1) La excepción de Spring Boot para esto, como causa directa. */
    @Test
    void conPortInUseExceptionDirectaSeDiceQueElPuertoEstaOcupado() {
        String mensaje = FalloArranque.mensaje(new PortInUseException(8080), LOG);

        assertThat(mensaje)
                .isEqualTo(
                        "No se puede abrir Educhronos: otro programa está usando el puerto 8080. "
                                + "Cierra ese programa y vuelve a intentarlo.");
    }

    /**
     * (2) EL CASO REAL, y el que justifica recorrer la cadena entera. Reproduce la anidación
     * medida en el M2-A de S154 con el 8080 cogido: la {@code BindException} viaja cuatro
     * causas por debajo de lo que llega arriba. Si sólo se mirase la causa directa, el único
     * fallo que sabemos explicar saldría con el mensaje genérico.
     */
    @Test
    void conBindExceptionCuatroCausasAbajoTambienSeDiceQueElPuertoEstaOcupado() {
        Throwable comoEnProduccion =
                new IllegalStateException(
                        "Failed to start bean 'webServerStartStop'",
                        new RuntimeException(
                                "Unable to start embedded Tomcat server",
                                new IllegalArgumentException(
                                        "standardService.connector.startFailed",
                                        new IllegalStateException(
                                                "Protocol handler start failed",
                                                new BindException(
                                                        "La dirección ya se está usando")))));

        assertThat(FalloArranque.mensaje(comoEnProduccion, LOG))
                .isEqualTo(FalloArranque.PUERTO_OCUPADO);
    }

    /**
     * (3) Una {@code BindException} pelada, SIN {@code PortInUseException} en ninguna parte,
     * basta. Es lo que de verdad llegó en la medición: Tomcat no lanzó la excepción de Boot.
     */
    @Test
    void conBindExceptionYSinPortInUseExceptionTambienCuenta() {
        assertThat(FalloArranque.esPuertoOcupado(new BindException("Address already in use")))
                .isTrue();
    }

    /** (4) Cualquier otro fallo: mensaje genérico, y NOMBRANDO el fichero del log. */
    @Test
    void conUnFalloAjenoSeRemiteAlLogPorSuRuta() {
        String mensaje = FalloArranque.mensaje(new IllegalStateException("la base está rota"), LOG);

        assertThat(mensaje)
                .isEqualTo("Educhronos no ha podido arrancar. El detalle está en: " + LOG);
        assertThat(mensaje).contains(LOG.toString());
    }

    /**
     * (5) Una cadena con CICLO termina. Sin la protección esto no sería un mensaje feo sino un
     * cuelgue dentro del manejador de errores: la aplicación no arrancaría y tampoco moriría,
     * que es la peor de las dos cosas.
     */
    @Test
    void unaCadenaDeCausasConCicloNoCuelga() {
        Exception primera = new IllegalStateException("una");
        Exception segunda = new IllegalStateException("otra", primera);
        primera.initCause(segunda);

        assertThat(FalloArranque.esPuertoOcupado(primera)).isFalse();
        assertThat(FalloArranque.mensaje(primera, LOG)).contains(LOG.toString());
    }

    /** (6) Sin excepción no se inventa un diagnóstico: mensaje genérico. */
    @Test
    void sinExcepcionSeDaElMensajeGenerico() {
        assertThat(FalloArranque.mensaje(null, LOG)).contains(LOG.toString());
    }

    // ------------------------------------------------ antes de que Spring exista

    /**
     * (7) El fallo PREVIO a Spring enseña el mensaje de la excepción, que en este camino ya
     * viene escrito para el usuario —lo escribe {@code CarpetaDatos}— y nombra la ruta y la
     * salida. Y NO nombra ningún log: en ese punto todavía no hay fichero de log.
     */
    @Test
    void antesDeSpringSeEnsenaElMensajeDeLaExcepcion() {
        String delUsuario =
                "No se puede crear la carpeta de datos de Educhronos: /datos/x. Compruebe los "
                        + "permisos o arranque con --spring.datasource.url=jdbc:sqlite:<ruta>";

        String mensaje = FalloArranque.mensajeAntesDeSpring(new IllegalStateException(delUsuario));

        assertThat(mensaje).isEqualTo("Educhronos no ha podido arrancar: " + delUsuario);
        assertThat(mensaje)
                .as("aquí no hay log que nombrar: aún no existe")
                .doesNotContain("educhronos.log");
    }

    /**
     * (8) Sin mensaje se da el nombre de la clase. Una frase que termina en dos puntos y nada
     * —«Educhronos no ha podido arrancar: null»— es peor que un nombre técnico: el nombre al
     * menos se puede buscar.
     */
    @Test
    void antesDeSpringSinMensajeSeDaElNombreDeLaClase() {
        assertThat(FalloArranque.mensajeAntesDeSpring(new NullPointerException()))
                .isEqualTo("Educhronos no ha podido arrancar: java.lang.NullPointerException");
    }

    /** (9) Un mensaje en blanco cuenta como ausente: espacios no informan de nada. */
    @Test
    void antesDeSpringUnMensajeEnBlancoCuentaComoAusente() {
        assertThat(FalloArranque.mensajeAntesDeSpring(new IllegalStateException("   ")))
                .isEqualTo("Educhronos no ha podido arrancar: java.lang.IllegalStateException");
    }
}
