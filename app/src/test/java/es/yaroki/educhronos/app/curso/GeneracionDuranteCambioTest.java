package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Generar un horario mientras otra operación de curso está en marcha (O-curso, S160,
 * invariante I2).
 *
 * <p>Es la otra mitad de la exclusión que {@code CursoAperturaTest} prueba por su lado: allí,
 * un solve en marcha impide el cambio y el duplicado; aquí, un cambio o un duplicado en marcha
 * impiden el solve. Las dos hacen falta, porque una sola dejaría el orden de llegada
 * decidiendo quién gana.
 *
 * <p><b>El cambio se simula marcando {@link EstadoCurso}</b> y no abriendo un curso de verdad,
 * porque abrir termina en milisegundos y no hay forma fiable de meter una petición dentro de
 * esa ventana sin hilos y esperas que harían el caso inestable. Lo que se mide es la decisión
 * —qué contesta {@code generar} con el indicador puesto—, y esa es exactamente la línea que
 * el cambio real ejecuta.
 *
 * <p>El {@code .addFilters} NO se pone a propósito: lo que se prueba aquí es la guarda del
 * SERVICIO, no la del filtro. Con el filtro montado, la petición moriría en él con un 503 y
 * este caso pasaría en verde sin ejecutar ni una línea de {@code GeneradorHorarioService}.
 */
@SpringBootTest
class GeneracionDuranteCambioTest {

    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    @Autowired private WebApplicationContext contexto;

    @Autowired private EstadoCurso estado;

    @Autowired private GeneradorHorarioService generador;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /** Pase lo que pase en el caso, el contexto no se queda marcado para el siguiente. */
    @AfterEach
    void soltarIndicadores() {
        estado.terminarCambio();
        estado.terminarDuplicado();
        while (estado.generando() > 0) {
            estado.terminarGeneracion();
        }
    }

    /**
     * (T4.a) Con un cambio de curso en marcha, {@code generar} se niega con 409
     * {@code CURSO_CAMBIANDO} y la causa viaja en el CUERPO, no en el {@code reason}
     * (D-F8.6-ii-a).
     *
     * <p>El aserto del contador es la otra mitad: un rechazo no puede dar de alta un solve
     * que nunca existió, porque entonces el cambio que lo provocó se quedaría bloqueado a sí
     * mismo para siempre.
     */
    @Test
    void generarConUnCambioEnMarcha_409ConCausaEnElCuerpo() throws Exception {
        assertThat(estado.intentarIniciarCambio()).isTrue();

        mockMvc.perform(
                        post("/api/horarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("CURSO_CAMBIANDO"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(estado.generando())
                .as("un rechazo NO da de alta un solve")
                .isZero();
    }

    /**
     * (T4.c, corrección de S160) Con un DUPLICADO en marcha, {@code generar} se niega con 409
     * {@code CURSO_OCUPADO} y un mensaje que nombra el duplicado, no el cambio de curso.
     *
     * <p><b>Este caso cubre un defecto que estuvo en el árbol.</b> Hasta la corrección,
     * {@code intentarIniciarGeneracion()} sólo miraba {@code cambiando}, y se justificaba
     * diciendo que {@code GuardaSoloLectura} ya rechazaba el {@code POST} durante el
     * duplicado. Pero eso es comprobar en un sitio y actuar en otro: una petición que pasa la
     * guarda con {@code duplicando} aún falso entra aquí cuando ya es cierto, el duplicado
     * archiva el origen, su apertura del curso nuevo se encuentra {@code generando > 0} y se
     * va con un 409, y el solve acaba escribiendo el horario en el curso ARCHIVADO por JPA,
     * sin pasar por guarda ninguna.
     *
     * <p>Por eso el {@code .addFilters} sigue sin ponerse: con el filtro montado, la petición
     * moriría en él con un 403 y este caso pasaría en verde sin ejecutar la línea que de
     * verdad cierra el agujero.
     *
     * <p>Se comprueba la CAUSA y no sólo el status: un 409 con
     * {@code CURSO_CAMBIANDO} mandaría al usuario a buscar un cambio de curso que nadie ha
     * pedido.
     */
    @Test
    void generarConUnDuplicadoEnMarcha_409CursoOcupado() throws Exception {
        assertThat(estado.intentarIniciarDuplicado()).isTrue();

        mockMvc.perform(
                        post("/api/horarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("CURSO_OCUPADO"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("duplicando")));

        assertThat(estado.generando())
                .as("un rechazo NO da de alta un solve: si lo hiciera, el duplicado que lo"
                        + " provocó no podría abrir después el curso nuevo")
                .isZero();
    }

    /**
     * (T4.b) El contador vuelve a cero aunque {@code generar} lance. El catálogo de esta base
     * está vacío, así que la generación revienta dentro —sin jornada no hay problema que
     * resolver—, y eso es justo lo que hace falta: si {@code terminarGeneracion()} viviera
     * sólo en el camino de éxito, un solve fallido dejaría la cuenta alta y NO se podría
     * volver a cambiar de curso sin reiniciar la aplicación.
     *
     * <p>El caso no dice QUÉ excepción sale —depende del catálogo vacío y no es lo que se
     * mide—; dice que sale alguna y que la cuenta queda en cero.
     */
    @Test
    void elContadorVuelveACeroAunqueGenerarLance() {
        assertThat(estado.generando()).as("de partida").isZero();

        assertThatThrownBy(() -> generador.generar(null, null, null, "prueba"))
                .as("un catálogo vacío no produce horario")
                .isInstanceOf(RuntimeException.class);

        assertThat(estado.generando())
                .as("el finally baja la cuenta también cuando revienta")
                .isZero();
        assertThat(estado.intentarIniciarCambio())
                .as("y por tanto se puede volver a cambiar de curso")
                .isTrue();
    }
}
