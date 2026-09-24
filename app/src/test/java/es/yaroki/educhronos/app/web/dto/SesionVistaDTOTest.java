package es.yaroki.educhronos.app.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link SesionVistaDTO#tramosCubiertos()} (S170, C-exportacion-bloques F2): la única
 * definición en Java de los tramos que ocupa una sesión exportada. El tramo de inicio es 4
 * y no 1 a propósito: con inicio 1, un método que devolviera {@code 1..duracion} sin
 * sumar el inicio pasaría el caso de duración 1.
 */
class SesionVistaDTOTest {

    @Test
    void conDuracionUnoCubreSoloSuTramo() {
        assertThat(sesion(4, 1).tramosCubiertos()).containsExactly(4);
    }

    @Test
    void conDuracionDosCubreSuTramoYElSiguiente() {
        assertThat(sesion(4, 2).tramosCubiertos()).containsExactly(4, 5);
    }

    @Test
    void conDuracionTresCubreSuTramoYLosDosSiguientes() {
        assertThat(sesion(4, 3).tramosCubiertos()).containsExactly(4, 5, 6);
    }

    private static SesionVistaDTO sesion(int tramo, int duracion) {
        return new SesionVistaDTO(1L, 1, 2, tramo, duracion, "Tec", "Tecnologia",
                List.of("TEC1"), "T1", List.of("1ºA-Completo"), List.of("1ºA"),
                "Tec-1ºA", "Tec-1ºA-P1");
    }
}
