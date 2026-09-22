package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
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
 * La guarda de solo lectura, cableada de verdad: contexto real, base real y la escritura
 * pasando de 201 a 403 por haber duplicado el curso (O-curso, S159, fase B).
 *
 * <p><b>El {@code .addFilters(...)} es obligatorio y es el punto entero de este fichero.</b>
 * {@code webAppContextSetup} NO registra los filtros del contexto: sin esa línea, este test
 * pasaría en verde con la guarda sin ejecutarse ni una vez, y el 403 nunca llegaría. Se toma
 * el bean del contexto, no una instancia nueva, para que lo que se ejercite sea el mismo
 * {@link EstadoCurso} que usa el servicio al duplicar.
 *
 * <p>Contexto y base propios por la misma razón que {@code CursoEndpointTest}: este test
 * archiva la base que abre, y hacerlo sobre la {@code educhronos-test.db} compartida dejaría
 * al resto de la suite escribiendo contra un curso de solo lectura.
 *
 * <p><b>Desde S160 hace falta un gesto más para llegar al 403.</b> Duplicar ya no deja la
 * aplicación dentro del curso archivado: deja abierto el NUEVO, que está activo y acepta
 * escrituras. Para ver la guarda hay que volver al archivado con
 * {@code POST /api/cursos/abrir}, y eso es lo que el caso hace. El contrato que fija no
 * cambia —una escritura sobre un curso archivado es 403 con causa y nombre—; lo que cambia es
 * cómo se llega a estar en uno, y el caso lo recorre entero.
 */
@SpringBootTest
class GuardaEndpointTest {

    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    @Autowired private WebApplicationContext contexto;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(contexto)
                        .addFilters(contexto.getBean(GuardaSoloLectura.class))
                        .build();
    }

    /**
     * (10) La misma petición, en los tres estados por los que pasa el centro: 201 en el curso
     * activo, 201 todavía en el curso NUEVO recién duplicado, y 403 con causa y mensaje
     * cuando se vuelve al archivado. Y la lectura sigue funcionando, que es la mitad del
     * contrato: un curso archivado se consulta, no se modifica.
     *
     * <p>El 201 del medio es el aserto que S160 añade, y es el que mide el cambio: si
     * duplicar dejara abierto el curso archivado —como hasta S159—, esa escritura sería un
     * 403 y el caso caería ahí.
     */
    @Test
    void laEscrituraPasaDe201A403AlVolverAlCursoArchivado() throws Exception {
        mockMvc.perform(
                        post("/api/niveles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"codigo\":\"1ESO\",\"orden\":1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/cursos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombreNuevo\":\"2026/2027\","
                                                + "\"nombreActual\":\"2025/2026\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/niveles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"codigo\":\"2ESO\",\"orden\":2}"))
                .andExpect(status().isCreated())
                .andDo(
                        r ->
                                assertThat(r.getResponse().getStatus())
                                        .as("en el curso NUEVO se escribe: está activo")
                                        .isEqualTo(201));

        mockMvc.perform(
                        post("/api/cursos/abrir")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fichero\":\"educhronos.db\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivado").value(true));

        mockMvc.perform(
                        post("/api/niveles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"codigo\":\"3ESO\",\"orden\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.causa").value("CURSO_SOLO_LECTURA"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("2025/2026")));

        // El recurso de cursos sigue llegando al servicio con el curso YA archivado, y lo
        // rechaza ÉL con un 409, no el filtro con un 403. Este aserto es el que mide la
        // exención en integración: sin él, quitar la exención no tumbaba este caso, porque
        // cuando aquí se duplica el curso todavía está activo y la guarda dejaría pasar la
        // petición de todos modos. Sigue siendo 409 CURSO_ARCHIVADO y no el permiso del
        // requisito (b) porque curso-2026-2027.db está ahí y está ACTIVO.
        mockMvc.perform(
                        post("/api/cursos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombreNuevo\":\"2027/2028\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.causa").value("CURSO_ARCHIVADO"));

        mockMvc.perform(get("/api/niveles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("1ESO"));

        assertThat(carpeta.resolve("curso-2026-2027.db"))
                .as("el curso nuevo existe: el 403 llega después de duplicar, no en su lugar")
                .isRegularFile();
    }
}
