package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link VistaPdf#desdeParametro}, que es la ÚNICA lógica que este enum tiene: lo demás
 * son datos por constante, y un test que los repitiera solo diría dos veces lo mismo.
 *
 * <p>Es la traducción del query param a vista, o sea la frontera entre lo que teclea
 * alguien y lo que el serializador entiende. Los tres casos que la definen son el valor
 * bueno, el valor inventado y la ausencia de valor.
 */
class VistaPdfTest {

    @Test
    void elParametroDeUnaVistaDevuelveEsaVista() {
        assertThat(VistaPdf.desdeParametro("grupo")).contains(VistaPdf.GRUPO);
    }

    /**
     * Un valor que no nombra ninguna vista no es una vista por defecto: es vacío. Si
     * cayera al defecto, pedir una vista mal escrita devolvería un PDF de otra cosa con
     * un 200 y nadie se enteraría.
     */
    @Test
    void unParametroDesconocidoNoDevuelveNingunaVista() {
        assertThat(VistaPdf.desdeParametro("trimestre")).isEmpty();
    }

    /** {@code null} se trata como el valor inventado, y NO revienta con un NPE. */
    @Test
    void unParametroNuloNoDevuelveNingunaVistaYNoLanza() {
        assertThat(VistaPdf.desdeParametro(null)).isEmpty();
    }
}
