package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.curso.EstadoCurso.Admision;
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

        assertThat(estado.intentarIniciarGeneracion()).isEqualTo(Admision.CONCEDIDA);
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

        assertThat(estado.intentarIniciarGeneracion()).isEqualTo(Admision.CONCEDIDA);
        assertThat(estado.intentarIniciarGeneracion())
                .as("dos solves SÍ conviven")
                .isEqualTo(Admision.CONCEDIDA);
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

        assertThat(estado.intentarIniciarGeneracion())
                .as("generar")
                .isEqualTo(Admision.HAY_CAMBIO);
        assertThat(estado.intentarIniciarDuplicado()).as("duplicar").isFalse();
        assertThat(estado.intentarIniciarCambio()).as("otro cambio").isFalse();
        assertThat(estado.generando()).as("el intento fallido no cuenta un solve").isZero();

        estado.terminarCambio();
        assertThat(estado.cambiando()).isFalse();
        assertThat(estado.intentarIniciarGeneracion())
                .as("al terminar, todo vuelve")
                .isEqualTo(Admision.CONCEDIDA);
    }

    /**
     * (T3.d) Con un duplicado en marcha NO empieza nada: ni un cambio, ni otro duplicado, ni
     * un solve.
     *
     * <p><b>La tercera casilla es la corrección de S160.</b> Antes valía «sí», con el
     * argumento de que {@code GuardaSoloLectura} ya rechaza el {@code POST /api/horarios}
     * mientras se duplica. Eso repartía la regla en dos sitios, y entre los dos cabía una
     * petición: la que pasa la guarda con {@code duplicando} todavía falso y entra al
     * servicio cuando ya es cierto. El final de esa carrera era un horario escrito por JPA en
     * el curso recién archivado. La regla vive aquí, donde se mira y se marca de una vez.
     *
     * <p>El aserto sobre el CONTADOR es la otra mitad: un rechazo no puede dejar contado un
     * solve que no existe, porque entonces el duplicado que lo provocó no podría abrir después
     * el curso nuevo.
     */
    @Test
    void duplicandoBloqueaLasTresCosas() {
        EstadoCurso estado = nuevo();

        assertThat(estado.intentarIniciarDuplicado()).isTrue();
        assertThat(estado.duplicando()).isTrue();

        assertThat(estado.intentarIniciarCambio()).as("cambiar").isFalse();
        assertThat(estado.intentarIniciarDuplicado()).as("otro duplicado").isFalse();
        assertThat(estado.intentarIniciarGeneracion())
                .as("generar, y el rechazo NOMBRA al duplicado")
                .isEqualTo(Admision.HAY_DUPLICADO);
        assertThat(estado.generando())
                .as("un rechazo no cuenta un solve que no ha empezado")
                .isZero();

        estado.terminarDuplicado();
        assertThat(estado.intentarIniciarCambio()).isTrue();
        estado.terminarCambio();
        assertThat(estado.intentarIniciarGeneracion())
                .as("al terminar el duplicado, generar vuelve a entrar")
                .isEqualTo(Admision.CONCEDIDA);
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

        assertThat(estado.intentarIniciarGeneracion()).isEqualTo(Admision.CONCEDIDA);
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
