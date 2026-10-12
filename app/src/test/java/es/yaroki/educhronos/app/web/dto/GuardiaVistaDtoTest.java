package es.yaroki.educhronos.app.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * El constructor compacto de {@link GuardiaVistaDTO} (S221, C-exportacion-guardias-recreo, D1):
 * una ORDINARIA lleva tramo y no recreo; una de RECREO o BIBLIOTECA, recreo y no tramo; los
 * dos empiezan en 1 y el tipo es uno de los tres. Cualquier otra cosa es
 * {@link IllegalArgumentException}.
 */
class GuardiaVistaDtoTest {

    @Test
    void lasTresFormasValidasSeConstruyen() {
        assertThat(new GuardiaVistaDTO("MAT1", 1, 2))
                .isEqualTo(new GuardiaVistaDTO("MAT1", 1, 2, null, GuardiaVistaDTO.ORDINARIA));
        assertThat(new GuardiaVistaDTO("MAT1", 1, null, 1, GuardiaVistaDTO.RECREO).esDeRecreo()).isTrue();
        assertThat(new GuardiaVistaDTO("MAT1", 1, null, 2, GuardiaVistaDTO.BIBLIOTECA).esDeRecreo()).isTrue();
        assertThat(new GuardiaVistaDTO("MAT1", 1, 2).esDeRecreo()).isFalse();
    }

    @Test
    void unaOrdinariaConRecreoSeRechaza() {
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, 2, 1, GuardiaVistaDTO.ORDINARIA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDINARIA lleva tramo y no recreo");
    }

    @Test
    void unaOrdinariaSinTramoSeRechaza() {
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, null, null, GuardiaVistaDTO.ORDINARIA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaDeRecreoConTramoOSinRecreoSeRechaza() {
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, 2, 1, GuardiaVistaDTO.RECREO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RECREO lleva recreo y no tramo");
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, null, null, GuardiaVistaDTO.BIBLIOTECA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BIBLIOTECA lleva recreo y no tramo");
    }

    @Test
    void tramoORecreoMenorQueUnoSeRechaza() {
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, 0, null, GuardiaVistaDTO.ORDINARIA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empiezan en 1");
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, null, 0, GuardiaVistaDTO.RECREO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empiezan en 1");
    }

    @Test
    void unTipoDesconocidoSeRechaza() {
        assertThatThrownBy(() -> new GuardiaVistaDTO("MAT1", 1, null, 1, "CONVIVENCIA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONVIVENCIA");
    }
}
