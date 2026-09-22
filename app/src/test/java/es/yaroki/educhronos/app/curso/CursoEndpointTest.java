package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import es.yaroki.educhronos.app.config.BaseConmutable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Contrato HTTP de {@code /api/curso} y {@code /api/cursos} (O-curso, S159).
 *
 * <p><b>Contexto y base PROPIOS, y es obligatorio.</b> Duplicar archiva la base abierta: si
 * este test corriera sobre la {@code educhronos-test.db} que comparte el resto de la suite,
 * la dejaría marcada como archivada y de solo lectura, y los demás tests fallarían según el
 * orden en que corrieran. De ahí el {@code @TempDir} estático y el
 * {@code @DynamicPropertySource}, que son el PRIMER {@code @SpringBootTest} y el primer
 * {@code @DynamicPropertySource} de la suite: los tests de endpoint del proyecto son
 * {@code @DataJpaTest} + {@code standaloneSetup}, que aquí no sirve porque lo que se prueba
 * incluye de dónde saca el servicio la ruta de la base.
 *
 * <p><b>Los asertos van sobre el CUERPO</b>, con {@code jsonPath}, y nunca sobre
 * {@code status().reason()}: el {@code reason} se lee del {@code MockHttpServletResponse} y
 * no del cuerpo que viaja por la red (D-F8.6-ii-a), de modo que un verde sobre él no
 * probaría que el navegador recibe la causa.
 *
 * <p><b>Orden fijo, y no por gusto.</b> El caso que duplica cambia la base que el contexto
 * tiene abierta —desde S160 deja abierto el curso NUEVO—, así que va el último: los casos
 * anteriores hablan de la base de partida. Con el orden por defecto de JUnit el resultado
 * dependería de nombres de método.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CursoEndpointTest {

    /** Carpeta de la base de este contexto. Estática: la necesita el registro de propiedades. */
    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    /**
     * El {@code MockMvc} se construye a mano sobre el contexto web. {@code
     * @AutoConfigureMockMvc} habría sido lo natural, pero vive en
     * {@code spring-boot-webmvc-test}, que este proyecto no trae entre sus dependencias;
     * {@code webAppContextSetup} da lo mismo —el {@code DispatcherServlet} real, con el
     * binding y la serialización de Boot— sin añadir un jar al pom para un solo test.
     */
    @Autowired private WebApplicationContext contexto;

    /** El datasource conmutable, para aseverar a qué FICHERO se fue el pool y no sólo qué dice el estado. */
    @Autowired private BaseConmutable base;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /**
     * (15) Una base recién creada no tiene curso: {@code nombre} null, no archivada y sin
     * propuesta. Es la condición 6 vista desde la interfaz —una base de antes de S159 se abre
     * sin intervención— y la razón de que el GET no devuelva 404 nunca.
     */
    @Test
    @Order(1)
    void get_baseNueva_sinNombreNiPropuesta() throws Exception {
        mockMvc.perform(get("/api/curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value((Object) null))
                .andExpect(jsonPath("$.archivado").value(false))
                .andExpect(jsonPath("$.propuestaSiguiente").value((Object) null));
    }

    /**
     * (17) Un nombre con guion se rechaza con la causa en el CUERPO y un texto para el
     * usuario. Manda también {@code nombreActual} para que lo que falle sea la FORMA del
     * nombre nuevo y no la falta del actual, que es otro rechazo.
     */
    @Test
    @Order(2)
    void post_nombreConFormaInvalida_400ConCausaYMensajeEnElCuerpo() throws Exception {
        mockMvc.perform(
                        post("/api/cursos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombreNuevo\":\"2026-2027\","
                                                + "\"nombreActual\":\"2025/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.causa").value("NOMBRE_INVALIDO"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /**
     * (18, S160) El listado tiene la base abierta antes de que exista ningún otro curso: una
     * sola entrada, marcada como abierta y sin nombre, que es la condición 6 vista desde el
     * selector.
     */
    @Test
    @Order(3)
    void get_cursos_soloLaBaseAbierta() throws Exception {
        mockMvc.perform(get("/api/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fichero").value("educhronos.db"))
                .andExpect(jsonPath("$[0].abierto").value(true))
                .andExpect(jsonPath("$[0].archivado").value(false))
                .andExpect(jsonPath("$[0].nombre").value((Object) null));
    }

    /**
     * (16, reescrito en S160) El curso se duplica: 201 con el nombre y el fichero, el fichero
     * existe de verdad y NO se escribe puntero, porque este contexto arranca con URL
     * explícita (condición 7).
     *
     * <p><b>(T11) Y el curso NUEVO queda ABIERTO.</b> Es el cambio de S160: hasta S159 el GET
     * decía aquí {@code 2025/2026} archivado, o sea que el centro acababa de crear el curso
     * del año siguiente y se quedaba atrapado en el anterior, en solo lectura. Ahora el GET
     * dice el curso nuevo y {@code archivado: false}, y el listado enseña los dos con el
     * origen ya archivado.
     */
    @Test
    @Order(4)
    void post_valido_creaElFicheroYDejaAbiertoElCursoNuevo() throws Exception {
        mockMvc.perform(
                        post("/api/cursos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nombreNuevo\":\"2026/2027\","
                                                + "\"nombreActual\":\"2025/2026\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("2026/2027"))
                .andExpect(jsonPath("$.fichero").value("curso-2026-2027.db"));

        assertThat(carpeta.resolve("curso-2026-2027.db")).isRegularFile();
        assertThat(carpeta.resolve("curso-abierto"))
                .as("con URL explícita no hay carpeta de datos que gobernar")
                .doesNotExist();

        mockMvc.perform(get("/api/curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("2026/2027"))
                .andExpect(jsonPath("$.archivado").value(false))
                .andExpect(jsonPath("$.propuestaSiguiente").value("2027/2028"));

        assertThat(base.baseAbierta())
                .as("el pool tiene puesto el fichero nuevo, no sólo el estado en memoria")
                .isEqualTo(carpeta.resolve("curso-2026-2027.db"));

        mockMvc.perform(get("/api/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fichero").value("curso-2026-2027.db"))
                .andExpect(jsonPath("$[0].abierto").value(true))
                .andExpect(jsonPath("$[0].archivado").value(false))
                .andExpect(jsonPath("$[1].fichero").value("educhronos.db"))
                .andExpect(jsonPath("$[1].abierto").value(false))
                .andExpect(jsonPath("$[1].archivado").value(true))
                .andExpect(jsonPath("$[1].nombre").value("2025/2026"));
    }

    /**
     * (19, S160) Se vuelve al curso archivado por {@code POST /api/cursos/abrir}, y la
     * respuesta es el estado resultante. Es el gesto que cierra el ciclo del selector: se
     * duplica, se entra en el nuevo y se puede volver a consultar el viejo.
     */
    @Test
    @Order(5)
    void post_abrir_devuelveAlCursoArchivado() throws Exception {
        mockMvc.perform(
                        post("/api/cursos/abrir")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fichero\":\"educhronos.db\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("2025/2026"))
                .andExpect(jsonPath("$.archivado").value(true));

        assertThat(base.baseAbierta()).isEqualTo(carpeta.resolve("educhronos.db"));

        mockMvc.perform(get("/api/curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("2025/2026"))
                .andExpect(jsonPath("$.archivado").value(true));
    }

    /**
     * (20, S160) Los dos rechazos de abrir, por la red y con la causa en el CUERPO: una ruta
     * en lugar de un nombre es 400 y un fichero que no está es 404.
     */
    @Test
    @Order(6)
    void post_abrir_rutaEs400YInexistenteEs404() throws Exception {
        mockMvc.perform(
                        post("/api/cursos/abrir")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fichero\":\"../x.db\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.causa").value("NOMBRE_INVALIDO"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(
                        post("/api/cursos/abrir")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fichero\":\"curso-2030-2031.db\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.causa").value("CURSO_NO_EXISTE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /** La base del contexto es la del TempDir y no la que comparte la suite. */
    @Test
    @Order(7)
    void laBaseDelContextoEsLaDelTempDir() {
        assertThat(Files.exists(carpeta.resolve("educhronos.db")))
                .as("el contexto abrió la base de este @TempDir")
                .isTrue();
    }
}
