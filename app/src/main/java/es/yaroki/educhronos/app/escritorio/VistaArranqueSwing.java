package es.yaroki.educhronos.app.escritorio;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * Lo que pinta {@link VentanaArranque}: una ventana sin marco ni botones, centrada, con las dos
 * líneas de la decisión X y una barra indeterminada sin porcentaje (S230).
 *
 * <p>{@link JWindow} y no {@code JFrame}: no tiene marco, ni botones, ni entrada en la barra de
 * tareas. La fuente es la lógica {@link Font#DIALOG}, que Java resuelve a la del sistema, sin
 * tocar el look and feel —cambiarlo cambiaría también los diálogos de {@link Dialogos}—.
 *
 * <p>Todo pasa por el hilo de Swing, y de forma SÍNCRONA ({@code invokeAndWait}) fuera de él:
 * cuando {@link #cerrar()} vuelve, la ventana ya no existe, que es lo que permite que el diálogo
 * de error se muestre después y solo.
 */
final class VistaArranqueSwing implements VentanaArranque.Vista {

    /** Ancho de la barra; el resto lo da el texto. */
    private static final int ANCHO_BARRA = 320;

    private JWindow ventana;

    @Override
    public void mostrar() throws Exception {
        enHiloSwing(() -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(20, 28, 22, 28)));

            JLabel primera = new JLabel(VentanaArranque.LINEA_1);
            primera.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
            JLabel segunda = new JLabel(VentanaArranque.LINEA_2);
            segunda.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            JProgressBar barra = new JProgressBar();
            barra.setIndeterminate(true);
            barra.setStringPainted(false);
            barra.setPreferredSize(new Dimension(ANCHO_BARRA, barra.getPreferredSize().height));
            barra.setMaximumSize(barra.getPreferredSize());

            for (JComponent c : new JComponent[] {primera, segunda, barra}) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            panel.add(primera);
            panel.add(Box.createVerticalStrut(6));
            panel.add(segunda);
            panel.add(Box.createVerticalStrut(16));
            panel.add(barra);

            JWindow nueva = new JWindow();
            nueva.setContentPane(panel);
            nueva.pack();
            nueva.setLocationRelativeTo(null);
            nueva.setVisible(true);
            ventana = nueva;
        });
    }

    @Override
    public void cerrar() throws Exception {
        enHiloSwing(() -> {
            if (ventana != null) {
                ventana.setVisible(false);
                ventana.dispose();
                ventana = null;
            }
        });
    }

    private static void enHiloSwing(Runnable tarea) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            tarea.run();
        } else {
            SwingUtilities.invokeAndWait(tarea);
        }
    }
}
