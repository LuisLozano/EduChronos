package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Qué arranque se elige (condiciones 4, 5 y 6 de O-instalación: de esta decisión cuelgan las
 * tres).
 *
 * <p>Se prueba la función PURA sobre el valor, no la lectura de {@code System.getProperty}:
 * tocar las propiedades de sistema dentro de la suite las deja tocadas para el resto de los
 * tests de la misma JVM.
 */
class ModoEscritorioTest {

    /** (1) El valor exacto, y sólo ese, activa el modo. */
    @Test
    void elValorTrueActivaElModoEscritorio() {
        assertThat(ModoEscritorio.activo("true")).isTrue();
    }

    /**
     * (2) Todo lo demás lo deja apagado, incluidas las variantes que un humano escribiría.
     * DECISIÓN de S154, documentada en la clase: la comparación es exacta —distingue
     * mayúsculas y no recorta espacios— porque este valor no lo teclea nadie, lo copia el
     * guion de empaquetado de la constante {@link ModoEscritorio#PROPIEDAD}. El caso corriente
     * es {@code null}: la propiedad sin definir, que es como arrancan la suite, el e2e y
     * {@code mvn spring-boot:run}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "false", "FALSE", "TRUE", "True", "true ", " true", "1", "sí"})
    void cualquierOtroValorDejaElModoApagado(String valor) {
        assertThat(ModoEscritorio.activo(valor)).isFalse();
    }

    /** (3) Sin la propiedad definida, modo apagado: es el arranque de siempre. */
    @Test
    void sinLaPropiedadDefinidaElModoEstaApagado() {
        assertThat(ModoEscritorio.activo(null)).isFalse();
    }

    /**
     * (4) El nombre de la propiedad es parte del contrato con el empaquetado: el
     * {@code scripts/empaquetar-windows.ps1} lleva este mismo literal en su
     * {@code --java-options}. Si se renombra aquí y no allí, el bundle arranca en modo
     * servidor sin dar ningún error, y el usuario se queda sin navegador, sin bandeja y sin
     * forma de cerrar.
     */
    @Test
    void elNombreDeLaPropiedadEsElQueUsaElEmpaquetado() {
        assertThat(ModoEscritorio.PROPIEDAD).isEqualTo("educhronos.escritorio");
    }
}
