package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La ventana de arranque (S230, condición 5 de O-acabado-pre-prueba). Se prueba la LÓGICA con una
 * vista y un temporizador de mentira: ningún caso instancia AWT, y ninguno toca la ventana
 * estática del proceso ({@code abrir}, {@code cerrarPor}), que es la de producción.
 */
class VentanaArranqueTest {

    /** Vista que apunta lo que le piden, en orden, en una lista compartida con el diálogo. */
    private static final class VistaApuntada implements VentanaArranque.Vista {
        final List<String> eventos;
        boolean fallarAlMostrar;
        boolean fallarAlCerrar;

        VistaApuntada(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public void mostrar() {
            if (fallarAlMostrar) {
                throw new Error("AWT sin servidor X");
            }
            eventos.add("mostrar");
        }

        @Override
        public void cerrar() {
            if (fallarAlCerrar) {
                throw new IllegalStateException("roto");
            }
            eventos.add("cerrar");
        }
    }

    /** Temporizador que guarda la tarea y la espera; la tarea se dispara a mano. */
    private static final class TemporizadorManual implements VentanaArranque.Temporizador {
        Duration espera;
        Runnable tarea;

        @Override
        public void programar(Duration espera, Runnable tarea) {
            this.espera = espera;
            this.tarea = tarea;
        }
    }

    private final List<String> eventos = new ArrayList<>();
    private final VistaApuntada vista = new VistaApuntada(eventos);
    private final TemporizadorManual temporizador = new TemporizadorManual();
    private final VentanaArranque ventana =
            new VentanaArranque(vista, temporizador, Duration.ofSeconds(60));

    /** (1) Se muestra una sola vez aunque se pida dos, y programa su cierre con su espera. */
    @Test
    void seMuestraUnaSolaVezYProgramaElCierrePorTiempo() {
        ventana.mostrar();
        ventana.mostrar();

        assertThat(eventos).containsExactly("mostrar");
        assertThat(temporizador.espera).isEqualTo(Duration.ofSeconds(60));
        assertThat(temporizador.tarea).isNotNull();
    }

    /** (2) El navegador la cierra. */
    @Test
    void elNavegadorLaCierra() {
        ventana.mostrar();
        ventana.cerrar(VentanaArranque.Motivo.NAVEGADOR);

        assertThat(eventos).containsExactly("mostrar", "cerrar");
    }

    /** (3) El temporizador la cierra si nada lo ha hecho antes. */
    @Test
    void elTiempoLaCierra() {
        ventana.mostrar();
        temporizador.tarea.run();

        assertThat(eventos).containsExactly("mostrar", "cerrar");
    }

    /**
     * (4) Antes de un diálogo, se cierra y DESPUÉS sale el diálogo: el diálogo nunca la encuentra
     * en pantalla. Es la única garantía de orden de la decisión X.
     */
    @Test
    void antesDeUnDialogoSeCierraYDespuesSaleElDialogo() {
        ventana.mostrar();
        ventana.antesDe(() -> eventos.add("diálogo"));

        assertThat(eventos).containsExactly("mostrar", "cerrar", "diálogo");
    }

    /** (5) DISCRIMINANTE de (4): ya cerrada, el diálogo sale igual y no se cierra dos veces. */
    @Test
    void conLaVentanaYaCerradaElDialogoSaleIgual() {
        ventana.mostrar();
        ventana.cerrar(VentanaArranque.Motivo.NAVEGADOR);
        ventana.antesDe(() -> eventos.add("diálogo"));

        assertThat(eventos).containsExactly("mostrar", "cerrar", "diálogo");
    }

    /** (6) Cerrar es idempotente: el primer motivo gana y el resto no hace nada. */
    @Test
    void elSegundoCierreNoHaceNada() {
        ventana.mostrar();
        ventana.cerrar(VentanaArranque.Motivo.NAVEGADOR);
        temporizador.tarea.run();
        ventana.cerrar(VentanaArranque.Motivo.ERROR);

        assertThat(eventos).containsExactly("mostrar", "cerrar");
    }

    /** (7) Sin haberse mostrado no hay nada que cerrar. */
    @Test
    void sinMostrarseNoSeCierra() {
        ventana.cerrar(VentanaArranque.Motivo.ERROR);

        assertThat(eventos).isEmpty();
    }

    /**
     * (8) Si la vista revienta al mostrarse —AWT sin pantalla lanza un Error—, no sale de aquí, no
     * se programa el tiempo y no se intenta cerrar lo que no se abrió.
     */
    @Test
    void unFalloAlMostrarNoSaleNiDejaNadaProgramado() {
        vista.fallarAlMostrar = true;
        ventana.mostrar();
        ventana.cerrar(VentanaArranque.Motivo.NAVEGADOR);

        assertThat(eventos).isEmpty();
        assertThat(temporizador.tarea).isNull();
    }

    /** (9) Si la vista revienta al cerrarse, no sale de aquí y el diálogo sale igual. */
    @Test
    void unFalloAlCerrarNoImpideElDialogo() {
        ventana.mostrar();
        vista.fallarAlCerrar = true;
        ventana.antesDe(() -> eventos.add("diálogo"));

        assertThat(eventos).containsExactly("mostrar", "diálogo");
    }

    /** (10) Un valor válido de la propiedad manda. */
    @Test
    void unValorPositivoDaEsosSegundos() {
        assertThat(VentanaArranque.segundos("3")).isEqualTo(3);
        assertThat(VentanaArranque.segundos(" 90 ")).isEqualTo(90);
    }

    /** (11) Ausente, no numérica, cero o negativa: 60. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "abc", "3.5", "0", "-5", "99999999999"})
    void unValorAusenteOInvalidoOMenorQueUnoDaSesenta(String valor) {
        assertThat(VentanaArranque.segundos(valor)).isEqualTo(60);
    }

    /** (12) Con la propiedad, la ventana creada espera esos segundos. */
    @Test
    void laVentanaCreadaUsaLosSegundosDeLaPropiedad() {
        VentanaArranque creada = VentanaArranque.crear(true, true, () -> true, () -> vista, () -> temporizador, "3");
        creada.mostrar();

        assertThat(temporizador.espera).isEqualTo(Duration.ofSeconds(3));
    }

    /**
     * (13) Sin modo escritorio, en una segunda instancia o sin pantalla no se crea: ni se llama a
     * la fábrica de la vista. Y sin modo escritorio o sin instancia ganada, ni se sondea la
     * pantalla (sondearla inicializa AWT).
     */
    @Test
    void sinModoEscritorioSinInstanciaOSinPantallaNoSeCrea() {
        AtomicInteger fabricadas = new AtomicInteger();
        AtomicInteger sondeos = new AtomicInteger();
        java.util.function.BooleanSupplier hayPantalla = () -> {
            sondeos.incrementAndGet();
            return true;
        };

        assertThat(VentanaArranque.crear(false, true, hayPantalla, () -> contar(fabricadas), () -> temporizador, null)).isNull();
        assertThat(VentanaArranque.crear(true, false, hayPantalla, () -> contar(fabricadas), () -> temporizador, null)).isNull();
        assertThat(sondeos.get()).isZero();
        assertThat(VentanaArranque.crear(true, true, () -> false, () -> contar(fabricadas), () -> temporizador, null)).isNull();
        assertThat(fabricadas.get()).isZero();
    }

    /** (14) DISCRIMINANTE de (13): con las tres, se crea, con una sola vista. */
    @Test
    void conModoEscritorioInstanciaYPantallaSeCrea() {
        AtomicInteger fabricadas = new AtomicInteger();
        VentanaArranque creada =
                VentanaArranque.crear(true, true, () -> true, () -> contar(fabricadas), () -> temporizador, null);

        assertThat(creada).isNotNull();
        assertThat(fabricadas.get()).isEqualTo(1);
    }

    /** (15) Los textos son los de la decisión X, carácter a carácter. */
    @Test
    void losTextosSonLosDeLaDecisionX() {
        assertThat(VentanaArranque.LINEA_1).isEqualTo("Educhronos se está iniciando…");
        assertThat(VentanaArranque.LINEA_1).endsWith("…");
        assertThat(VentanaArranque.LINEA_2).isEqualTo("El navegador se abrirá en unos segundos.");
    }

    private VentanaArranque.Vista contar(AtomicInteger fabricadas) {
        fabricadas.incrementAndGet();
        return vista;
    }
}
