package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * La red de flujo de la pre-validación (S209), ya en su clase (S213). Lo que pide el reparto de
 * guardias: el identificador de cada arista, el flujo que pasa por ella, y que una segunda llamada a
 * {@code flujoMaximo} solo añada flujo sobre lo que dejó la primera.
 */
class RedDeFlujoTest {

    /**
     * La red de Cormen et al. (fuente 0, sumidero 5): flujo máximo 23. Su único corte mínimo es
     * {0, 1, 2, 4} | {3, 5}, así que las aristas que lo cruzan van saturadas en todo flujo máximo y
     * su flujo está determinado.
     */
    private static final class Cormen {
        final RedDeFlujo red = new RedDeFlujo(6);
        final int a01 = red.arista(0, 1, 16);
        final int a02 = red.arista(0, 2, 13);
        final int a12 = red.arista(1, 2, 10);
        final int a21 = red.arista(2, 1, 4);
        final int a13 = red.arista(1, 3, 12);
        final int a32 = red.arista(3, 2, 9);
        final int a24 = red.arista(2, 4, 14);
        final int a43 = red.arista(4, 3, 7);
        final int a35 = red.arista(3, 5, 20);
        final int a45 = red.arista(4, 5, 4);
    }

    @Test
    void casoConocido_flujoMaximo23() {
        Cormen c = new Cormen();

        assertThat(c.red.flujoMaximo(0, 5)).isEqualTo(23);
    }

    @Test
    void lasAristasSeNumeranPorOrdenDeAlta() {
        Cormen c = new Cormen();

        assertThat(new int[] {c.a01, c.a02, c.a12, c.a21, c.a13, c.a32, c.a24, c.a43, c.a35, c.a45})
                .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void flujoPorArista_lasDelCorteVanSaturadasYElSumideroRecibeLoQueSale() {
        Cormen c = new Cormen();
        c.red.flujoMaximo(0, 5);

        assertThat(c.red.flujo(c.a13)).isEqualTo(12);
        assertThat(c.red.flujo(c.a43)).isEqualTo(7);
        assertThat(c.red.flujo(c.a45)).isEqualTo(4);
        assertThat(c.red.flujo(c.a35)).isEqualTo(19);
        assertThat(c.red.flujo(c.a01) + c.red.flujo(c.a02)).isEqualTo(23);
    }

    @Test
    void antesDeAumentar_todoFlujoEsCero() {
        Cormen c = new Cormen();

        assertThat(c.red.flujo(c.a01)).isZero();
        assertThat(c.red.flujo(c.a45)).isZero();
    }

    @Test
    void segundaLlamada_soloAnadeYNoBajaLasAristasYaSaturadas() {
        // fuente 0 → nodo 1 (5); 1 → sumidero 2 (2). La primera llamada satura 1 → 2.
        RedDeFlujo red = new RedDeFlujo(3);
        int entrada = red.arista(0, 1, 5);
        int saturada = red.arista(1, 2, 2);
        assertThat(red.flujoMaximo(0, 2)).isEqualTo(2);
        assertThat(red.flujo(saturada)).isEqualTo(2);

        // Una arista paralela al sumidero abre sitio: la segunda llamada añade 3, no recalcula 5.
        int paralela = red.arista(1, 2, 10);
        assertThat(red.flujoMaximo(0, 2)).isEqualTo(3);
        assertThat(red.flujo(saturada)).isEqualTo(2);
        assertThat(red.flujo(paralela)).isEqualTo(3);
        assertThat(red.flujo(entrada)).isEqualTo(5);
    }

    @Test
    void segundaLlamadaSinNadaNuevo_noAnadeNada() {
        Cormen c = new Cormen();
        c.red.flujoMaximo(0, 5);

        assertThat(c.red.flujoMaximo(0, 5)).isZero();
        assertThat(c.red.flujo(c.a45)).isEqualTo(4);
    }

    @Test
    void alcanzablesTrasElFlujo_elLadoDeLaFuenteDelCorteMinimo() {
        Cormen c = new Cormen();
        c.red.flujoMaximo(0, 5);

        assertThat(c.red.alcanzables(0)).containsExactly(true, true, true, false, true, false);
    }
}
