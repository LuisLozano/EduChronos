package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * La tabla de exclusiones de {@link EstadoCurso} (O-curso, S160, C-selector-curso fase A).
 *
 * <p>Es la spec del invariante I2: generar, duplicar y cambiar de curso no pueden solaparse
 * como quieran. Cada caso comprueba las dos direcciones de un par —si A está en marcha, B no
 * puede empezar— y, además, que el indicador vuelve a su sitio, porque una exclusión que no
 * se levanta es un bloqueo permanente disfrazado de invariante.
 *
 * <p>Unitario y SIN repositorio: el {@link EstadoCurso} se construye con {@code null} y nadie
 * llama a {@code recargar()}, así que arranca como una base sin nombre de curso. Lo que se
 * mide aquí son los tres indicadores, que no tocan la base.
 */
class EstadoCursoTest {

    /**
     * (T3.a) Con un solve en marcha no se puede cambiar de curso ni duplicar; al terminarlo,
     * sí. Es la mitad del invariante que impide que un horario se guarde en una base distinta
     * de la que lo alimentó.
     */
    @Test
    void generandoBloqueaElCambioYElDuplicado() {
        EstadoCurso estado = nuevo();

        assertThat(estado.intentarIniciarGeneracion()).isTrue();
        assertThat(estado.generando()).isOne();

        assertThat(estado.intentarIniciarCambio()).as("cambiar con un solve dentro").isFalse();
        assertThat(estado.intentarIniciarDuplicado()).as("duplicar con un solve dentro").isFalse();
        assertThat(estado.cambiando()).as("un intento fallido NO marca nada").isFalse();
        assertThat(estado.duplicando()).as("un intento fallido NO marca nada").isFalse();

        estado.terminarGeneracion();
        assertThat(estado.generando()).isZero();
        assertThat(estado.intentarIniciarCambio()).as("con la casa vacía, sí").isTrue();
    }

    /**
     * (T3.b) EL CONTADOR: dos solves a la vez, y el cambio sigue bloqueado hasta que sale el
     * SEGUNDO. Con un booleano en vez de un contador, el primero en terminar apagaría el
     * indicador con el otro aún dentro y un cambio de curso se colaría a mitad de un solve,
     * que es exactamente el defecto que esto evita.
     */
    @Test
    void dosSolvesALaVez_elCambioSigueBloqueadoHastaQueSaleElSegundo() {
        EstadoCurso estado = nuevo();

        assertThat(estado.intentarIniciarGeneracion()).isTrue();
        assertThat(estado.intentarIniciarGeneracion()).as("dos solves SÍ conviven").isTrue();
        assertThat(estado.generando()).isEqualTo(2);

        estado.terminarGeneracion();
        assertThat(estado.generando()).isOne();
        assertThat(estado.intentarIniciarCambio())
                .as("todavía queda uno dentro: NO se cambia")
                .isFalse();

        estado.terminarGeneracion();
        assertThat(estado.generando()).isZero();
        assertThat(estado.intentarIniciarCambio()).as("ahora sí").isTrue();
    }

    /**
     * (T3.c) Con un cambio de curso en marcha no empieza nada: ni un solve, ni un duplicado,
     * ni otro cambio. Es la fila más estricta de la tabla, porque el cambio va a tirar del
     * pool que los demás usarían.
     */
    @Test
    void cambiandoBloqueaLasTresCosas() {
        EstadoCurso estado = nuevo();

        assertThat(estado.intentarIniciarCambio()).isTrue();
        assertThat(estado.cambiando()).isTrue();

        assertThat(estado.intentarIniciarGeneracion()).as("generar").isFalse();
        assertThat(estado.intentarIniciarDuplicado()).as("duplicar").isFalse();
        assertThat(estado.intentarIniciarCambio()).as("otro cambio").isFalse();
        assertThat(estado.generando()).as("el intento fallido no cuenta un solve").isZero();

        estado.terminarCambio();
        assertThat(estado.cambiando()).isFalse();
        assertThat(estado.intentarIniciarGeneracion()).as("al terminar, todo vuelve").isTrue();
    }

    /**
     * (T3.d) Con un duplicado en marcha no se cambia de curso ni se duplica otra vez. Generar
     * SÍ se deja pasar aquí a propósito: quien lo impide es {@code GuardaSoloLectura}, que
     * rechaza el {@code POST /api/horarios} con un 403 mientras dura el duplicado, y
     * duplicarlo en este nivel sería una segunda regla que nadie podría observar por separado.
     */
    @Test
    void duplicandoBloqueaElCambioYOtroDuplicado() {
        EstadoCurso estado = nuevo();

        assertThat(estado.intentarIniciarDuplicado()).isTrue();
        assertThat(estado.duplicando()).isTrue();

        assertThat(estado.intentarIniciarCambio()).as("cambiar").isFalse();
        assertThat(estado.intentarIniciarDuplicado()).as("otro duplicado").isFalse();
        assertThat(estado.intentarIniciarGeneracion())
                .as("generar NO lo bloquea este objeto: lo bloquea la guarda")
                .isTrue();
        estado.terminarGeneracion();

        estado.terminarDuplicado();
        assertThat(estado.intentarIniciarCambio()).isTrue();
    }

    /**
     * (T3.e) La cuenta de solves no baja de cero. Un {@code terminarGeneracion()} de más
     * —un {@code finally} que corriera dos veces por un camino que nadie ha visto— dejaría el
     * contador negativo, y entonces un solve en marcha ya no bloquearía el cambio: el
     * invariante se caería en silencio, que es la peor forma de caerse.
     */
    @Test
    void laCuentaDeSolvesNoBajaDeCero() {
        EstadoCurso estado = nuevo();

        estado.terminarGeneracion();
        estado.terminarGeneracion();
        assertThat(estado.generando()).isZero();

        assertThat(estado.intentarIniciarGeneracion()).isTrue();
        assertThat(estado.generando()).isOne();
        assertThat(estado.intentarIniciarCambio())
                .as("un solve real sigue bloqueando tras los terminar de más")
                .isFalse();
    }

    /** Estado de una base sin nombre de curso, sin repositorio y sin nada en marcha. */
    private static EstadoCurso nuevo() {
        return new EstadoCurso(null);
    }
}
