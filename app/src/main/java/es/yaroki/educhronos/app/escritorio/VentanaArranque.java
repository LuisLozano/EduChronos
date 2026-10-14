package es.yaroki.educhronos.app.escritorio;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La ventana que dice que Educhronos se está iniciando (S230, C-ventana-arranque, condición 5 de
 * O-acabado-pre-prueba; decisión X de S229). Entre el doble clic y el navegador pasan unos
 * segundos sin nada a la vista, y en la demo de S188 eso hizo relanzar el programa
 * (D-arranque-sin-aviso-de-espera).
 *
 * <p>Se muestra después de ganar la instancia única y antes de arrancar Spring, y se cierra, lo
 * que pase primero: al ir a abrirse el navegador, a los {@link #SEGUNDOS_POR_DEFECTO} segundos
 * (o los de {@link #PROPIEDAD_SEGUNDOS}) o antes de cualquier diálogo. Una segunda instancia no
 * la crea nunca: sale antes de llegar aquí.
 *
 * <p><b>Aquí no hay AWT.</b> La lógica —mostrar una vez, cerrar una vez y por el primer motivo,
 * el temporizador, cerrar antes del diálogo— va en esta clase, y lo que pinta va detrás de
 * {@link Vista} ({@link VistaArranqueSwing} en producción). Así se prueba sin pantalla, como el
 * resto del paquete prueba sus funciones puras.
 *
 * <p><b>Nada de esto puede tumbar el arranque</b> (regla de S154, ver {@link Pantalla}): cada
 * llamada a la vista va dentro de un {@code catch (Throwable)} y un fallo solo se registra.
 */
public final class VentanaArranque {

    private static final Logger LOG = LoggerFactory.getLogger(VentanaArranque.class);

    /** Propiedad de sistema con los segundos máximos de la ventana. Solo -D: se lee antes de
     *  Spring, así que en application.properties no la leería nadie (enmienda 1 de S230-F3). */
    public static final String PROPIEDAD_SEGUNDOS = "educhronos.arranque.segundos-ventana";

    /** Lo que dura la ventana como mucho si nada la cierra antes (decisión X). */
    static final int SEGUNDOS_POR_DEFECTO = 60;

    /** Primera línea, literal de la decisión X (los puntos suspensivos son U+2026). */
    static final String LINEA_1 = "Educhronos se está iniciando…";

    /** Segunda línea, literal de la decisión X. */
    static final String LINEA_2 = "El navegador se abrirá en unos segundos.";

    /** Por qué se cierra. El nombre es lo que sale en el log. */
    public enum Motivo {
        NAVEGADOR("navegador"),
        TIEMPO("tiempo"),
        ERROR("error");

        private final String nombre;

        Motivo(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    /** Lo que pinta. Las dos llamadas pueden fallar: quien las hace lo captura. */
    interface Vista {
        void mostrar() throws Exception;

        void cerrar() throws Exception;
    }

    /** Ejecuta una tarea pasado un tiempo, en otro hilo que no retiene la JVM. */
    interface Temporizador {
        void programar(Duration espera, Runnable tarea);
    }

    /** La ventana de este proceso, o {@code null} si no se ha creado. */
    private static VentanaArranque actual;

    private final Vista vista;
    private final Temporizador temporizador;
    private final Duration espera;
    private boolean mostrada;
    private boolean cerrada;

    VentanaArranque(Vista vista, Temporizador temporizador, Duration espera) {
        this.vista = vista;
        this.temporizador = temporizador;
        this.espera = espera;
    }

    /**
     * Abre la ventana de este proceso, si procede. La llama {@link Escritorio} justo antes de
     * arrancar Spring.
     *
     * @param modoEscritorio si el arranque es el del bundle
     * @param instanciaGanada si este proceso tiene el candado de la carpeta de datos
     */
    public static void abrir(boolean modoEscritorio, boolean instanciaGanada) {
        try {
            VentanaArranque ventana = crear(
                    modoEscritorio,
                    instanciaGanada,
                    Pantalla::disponible,
                    VistaArranqueSwing::new,
                    VentanaArranque::temporizadorDeHilo,
                    System.getProperty(PROPIEDAD_SEGUNDOS));
            if (ventana != null) {
                synchronized (VentanaArranque.class) {
                    actual = ventana;
                }
                ventana.mostrar();
            }
        } catch (Throwable e) {
            LOG.warn("No se ha podido abrir la ventana de arranque: {}", e.toString());
        }
    }

    /** Cierra la ventana de este proceso, si la hay y sigue abierta. */
    public static void cerrarPor(Motivo motivo) {
        VentanaArranque ventana;
        synchronized (VentanaArranque.class) {
            ventana = actual;
        }
        if (ventana != null) {
            ventana.cerrar(motivo);
        }
    }

    /** Cierra la ventana de este proceso por error y DESPUÉS muestra el diálogo. */
    static void antesDeDialogo(Runnable dialogo) {
        VentanaArranque ventana;
        synchronized (VentanaArranque.class) {
            ventana = actual;
        }
        if (ventana != null) {
            ventana.antesDe(dialogo);
        } else {
            dialogo.run();
        }
    }

    /**
     * Crea la ventana si procede, sin mostrarla. Ni la fábrica de la vista ni la pantalla se
     * tocan si no hace falta.
     *
     * @return la ventana, o {@code null} si no procede
     */
    static VentanaArranque crear(
            boolean modoEscritorio,
            boolean instanciaGanada,
            BooleanSupplier pantalla,
            Supplier<Vista> fabrica,
            Supplier<Temporizador> temporizador,
            String valorSegundos) {
        if (!procede(modoEscritorio, instanciaGanada, pantalla)) {
            return null;
        }
        return new VentanaArranque(
                fabrica.get(), temporizador.get(), Duration.ofSeconds(segundos(valorSegundos)));
    }

    /**
     * Función PURA salvo por la pantalla, que solo se sondea si lo demás se cumple: sondearla
     * inicializa AWT, y eso no debe pasar en el arranque de desarrollo ni en una segunda
     * instancia.
     */
    static boolean procede(boolean modoEscritorio, boolean instanciaGanada, BooleanSupplier pantalla) {
        return modoEscritorio && instanciaGanada && pantalla.getAsBoolean();
    }

    /**
     * Función PURA sobre el valor de la propiedad: los segundos, o
     * {@link #SEGUNDOS_POR_DEFECTO} si el valor falta, no es un entero o no es positivo.
     */
    static int segundos(String valor) {
        if (valor == null) {
            return SEGUNDOS_POR_DEFECTO;
        }
        try {
            int n = Integer.parseInt(valor.trim());
            return n > 0 ? n : SEGUNDOS_POR_DEFECTO;
        } catch (NumberFormatException e) {
            return SEGUNDOS_POR_DEFECTO;
        }
    }

    /** Muestra la ventana una sola vez y programa su cierre por tiempo. */
    void mostrar() {
        synchronized (this) {
            if (mostrada) {
                return;
            }
            mostrada = true;
        }
        try {
            vista.mostrar();
        } catch (Throwable e) {
            synchronized (this) {
                cerrada = true;
            }
            LOG.warn("No se ha podido mostrar la ventana de arranque: {}", e.toString());
            return;
        }
        LOG.info(
                "Ventana de arranque mostrada; se cerrará al abrirse el navegador o a los {} s.",
                espera.toSeconds());
        temporizador.programar(espera, () -> cerrar(Motivo.TIEMPO));
    }

    /** Cierra la ventana una sola vez, por el primer motivo que llegue. Sin mostrar, nada. */
    void cerrar(Motivo motivo) {
        synchronized (this) {
            if (!mostrada || cerrada) {
                return;
            }
            cerrada = true;
        }
        try {
            vista.cerrar();
            LOG.info("Ventana de arranque cerrada: {}.", motivo);
        } catch (Throwable e) {
            LOG.warn("No se ha podido cerrar la ventana de arranque ({}): {}", motivo, e.toString());
        }
    }

    /** Cierra por error y, ya cerrada, ejecuta el diálogo. El diálogo se ejecuta siempre. */
    void antesDe(Runnable dialogo) {
        cerrar(Motivo.ERROR);
        dialogo.run();
    }

    /** El temporizador de producción: un hilo daemon que duerme y cierra. */
    private static Temporizador temporizadorDeHilo() {
        return (espera, tarea) -> {
            Thread hilo = new Thread(() -> {
                try {
                    Thread.sleep(espera.toMillis());
                    tarea.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "educhronos-ventana-arranque");
            hilo.setDaemon(true);
            hilo.start();
        };
    }
}
