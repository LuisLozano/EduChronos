package es.yaroki.educhronos.app.escritorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sun.jna.Pointer;
import es.yaroki.educhronos.app.config.MantenerDespiertoConfig;
import es.yaroki.educhronos.app.escritorio.MantenerDespiertoWindows.Kernel32Energia;
import es.yaroki.educhronos.app.escritorio.MantenerDespiertoWindows.ReasonContext;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * La petición de energía de Windows con un {@code kernel32} doble, sin Windows ni Spring (S238,
 * C-sin-suspension, T4 a T6). Las constantes se comprueban con su VALOR (SYSTEM=1, DISPLAY=0,
 * Version 0, Flags 1), no con las de la clase: son las que pasaron en la sonda, y una constante
 * cambiada por error no debe arrastrar al test con ella.
 */
class MantenerDespiertoWindowsTest {

    private static final String MOTIVO = "Educhronos está generando un horario";

    /** (T4) pedir crea la petición con el motivo y pone SYSTEM y DISPLAY; close lo deshace una vez. */
    @Test
    void pedirPoneLasDosYCloseLasQuitaYCierraUnaSolaVez() {
        Kernel32Doble k = new Kernel32Doble();

        MantenerDespierto.Peticion peticion = new MantenerDespiertoWindows(k).pedir(MOTIVO);

        assertThat(k.version).isZero();
        assertThat(k.flags).isEqualTo(1);
        assertThat(k.motivo).isEqualTo(MOTIVO);
        assertThat(k.llamadas).containsExactly("PowerCreateRequest", "PowerSetRequest 1", "PowerSetRequest 0");
        assertThat(k.handles).allSatisfy(h -> assertThat(h).isEqualTo(k.handle));

        peticion.close();
        assertThat(k.llamadas).containsExactly("PowerCreateRequest", "PowerSetRequest 1", "PowerSetRequest 0",
                "PowerClearRequest 1", "PowerClearRequest 0", "CloseHandle");
        assertThat(k.handles).allSatisfy(h -> assertThat(h).isEqualTo(k.handle));

        peticion.close();
        assertThat(k.llamadas).hasSize(6);
    }

    /** (T5) PowerCreateRequest devuelve INVALID_HANDLE_VALUE (-1): no lanza y no llama a nada más. */
    @Test
    void conHandleInvalidoNoLanzaNiLlamaANadaMas() {
        Kernel32Doble k = new Kernel32Doble();
        k.handle = new Pointer(-1L);

        assertThatCode(() -> new MantenerDespiertoWindows(k).pedir(MOTIVO).close()).doesNotThrowAnyException();

        assertThat(k.llamadas).containsExactly("PowerCreateRequest");
    }

    /** (T5) PowerCreateRequest devuelve null: lo mismo. */
    @Test
    void conHandleNuloNoLanzaNiLlamaANadaMas() {
        Kernel32Doble k = new Kernel32Doble();
        k.handle = null;

        assertThatCode(() -> new MantenerDespiertoWindows(k).pedir(MOTIVO).close()).doesNotThrowAnyException();

        assertThat(k.llamadas).containsExactly("PowerCreateRequest");
    }

    /** (T5) Windows no acepta DISPLAY: no lanza, quita el SYSTEM ya puesto y cierra el handle. */
    @Test
    void siFallaUnPowerSetRequestQuitaLoPuestoYCierra() {
        Kernel32Doble k = new Kernel32Doble();
        k.tipoQueFalla = 0;

        MantenerDespierto.Peticion peticion = new MantenerDespiertoWindows(k).pedir(MOTIVO);

        assertThat(k.llamadas).containsExactly("PowerCreateRequest", "PowerSetRequest 1", "PowerSetRequest 0",
                "PowerClearRequest 1", "CloseHandle");
        assertThatCode(peticion::close).doesNotThrowAnyException();
        assertThat(k.llamadas).hasSize(5);
    }

    /** (T6) En Windows, el de Windows con lo que dé el cargador. */
    @Test
    void elegirEnWindowsDevuelveElDeWindows() {
        Kernel32Doble k = new Kernel32Doble();

        assertThat(MantenerDespiertoConfig.elegir("Windows 11", () -> k)).isInstanceOf(MantenerDespiertoWindows.class);
    }

    /** (T6) Fuera de Windows, el nulo sin llamar siquiera al cargador. */
    @Test
    void elegirFueraDeWindowsDevuelveElNuloSinCargar() {
        List<String> cargas = new ArrayList<>();

        MantenerDespierto elegido = MantenerDespiertoConfig.elegir("Linux", () -> {
            cargas.add("cargado");
            return new Kernel32Doble();
        });

        assertThat(elegido).isInstanceOf(MantenerDespiertoNulo.class);
        assertThat(cargas).isEmpty();
    }

    /** (T6) Si JNA no carga en Windows, el nulo y sin lanzar. */
    @Test
    void elegirConUnCargadorQueFallaDevuelveElNulo() {
        MantenerDespierto elegido = MantenerDespiertoConfig.elegir("Windows 11", () -> {
            throw new UnsatisfiedLinkError("sin jnidispatch");
        });

        assertThat(elegido).isInstanceOf(MantenerDespiertoNulo.class);
    }

    /** {@code kernel32} doble: apunta cada llamada, con el tipo, y el handle que recibe. */
    private static final class Kernel32Doble implements Kernel32Energia {

        final List<String> llamadas = new ArrayList<>();
        final List<Pointer> handles = new ArrayList<>();
        Pointer handle = new Pointer(0x1234L);
        Integer tipoQueFalla;
        int version = -1;
        int flags = -1;
        String motivo;

        @Override
        public Pointer PowerCreateRequest(ReasonContext contexto) {
            llamadas.add("PowerCreateRequest");
            version = contexto.Version;
            flags = contexto.Flags;
            motivo = contexto.Razon.getWideString(0);
            return handle;
        }

        @Override
        public boolean PowerSetRequest(Pointer peticion, int tipo) {
            llamadas.add("PowerSetRequest " + tipo);
            handles.add(peticion);
            return tipoQueFalla == null || tipoQueFalla != tipo;
        }

        @Override
        public boolean PowerClearRequest(Pointer peticion, int tipo) {
            llamadas.add("PowerClearRequest " + tipo);
            handles.add(peticion);
            return true;
        }

        @Override
        public boolean CloseHandle(Pointer objeto) {
            llamadas.add("CloseHandle");
            handles.add(objeto);
            return true;
        }
    }
}
