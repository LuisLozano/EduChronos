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
 * <p><b>Orden fijo, y no por gusto.</b> El caso que duplica es DESTRUCTIVO para el contexto
 * —deja el curso archivado—, así que va el último: después de él, cualquier escritura
 * responde que el curso es de solo lectura. Con el orden por defecto de JUnit el resultado
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
     * (16) El curso se duplica: 201 con el nombre y el fichero, el fichero existe de verdad,
     * el GET pasa a decir que el curso abierto está archivado y NO se escribe puntero, porque
     * este contexto arranca con URL explícita (condición 7).
     */
    @Test
    @Order(3)
    void post_valido_creaElFicheroArchivaElAbiertoYNoEscribePuntero() throws Exception {
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
                .andExpect(jsonPath("$.nombre").value("2025/2026"))
                .andExpect(jsonPath("$.archivado").value(true))
                .andExpect(jsonPath("$.propuestaSiguiente").value("2026/2027"));
    }

    /** La base del contexto es la del TempDir y no la que comparte la suite. */
    @Test
    @Order(4)
    void laBaseDelContextoEsLaDelTempDir() {
        assertThat(Files.exists(carpeta.resolve("educhronos.db")))
                .as("el contexto abrió la base de este @TempDir")
                .isTrue();
    }
}
