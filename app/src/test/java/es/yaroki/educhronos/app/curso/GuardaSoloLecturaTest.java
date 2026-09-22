package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spec de {@link GuardaSoloLectura} (O-curso, S159, fase B de C-duplicado-guarda).
 *
 * <p><b>Unitario y SIN Spring, a propósito.</b> Un filtro montado por MockMvc sólo se
 * ejercita si se registra explícitamente con {@code .addFilters(...)}, y un test que se
 * olvide de eso pasa en verde sin haber ejecutado ni una línea de la guarda. Aquí el filtro
 * se invoca a mano con {@code MockHttpServletRequest}, {@code MockHttpServletResponse} y un
 * {@code MockFilterChain}, así que no hay manera de que no corra. La integración se prueba
 * aparte, en {@code GuardaEndpointTest}.
 *
 * <p>Que la cadena se llamara o no se mide por {@code MockFilterChain.getRequest()}: es
 * {@code null} hasta que alguien la invoca.
 *
 * <p>El {@link EstadoCurso} es el real, construido sin repositorio: nadie llama a
 * {@code afterSingletonsInstantiated}, así que arranca como una base sin nombre de curso y
 * se lleva al estado que cada caso necesita por sus propios métodos.
 */
class GuardaSoloLecturaTest {

    /** El mapper del proyecto: el JsonMapper de Jackson 3 que autoconfigura Boot 4.1. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Nombre del curso archivado, que tiene que aparecer en el mensaje. */
    private static final String ARCHIVADO = "2025/2026";

    /** (1) Con el curso activo la guarda no se interpone: la escritura sigue su camino. */
    @Test
    void cursoActivo_laEscrituraPasaSinTocarLaRespuesta() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        guarda(activo()).doFilter(peticion("POST", "/api/niveles"), respuesta, cadena);

        assertThat(cadena.getRequest()).as("la cadena se llamó").isNotNull();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(respuesta.getContentAsString()).isEmpty();
    }

    /**
     * (2) Con el curso archivado, las tres escrituras se rechazan con la causa en el cuerpo y
     * el nombre del curso en el mensaje, y la cadena NO se llama: el rechazo no puede ser un
     * 403 escrito después de haber borrado algo.
     */
    @Test
    void archivado_postPutYDelete_403ConCausaYNombreYSinLlamarALaCadena() throws Exception {
        for (String metodo : new String[] {"POST", "PUT", "DELETE"}) {
            MockFilterChain cadena = new MockFilterChain();
            MockHttpServletResponse respuesta = new MockHttpServletResponse();

            guarda(archivado()).doFilter(peticion(metodo, "/api/niveles/1"), respuesta, cadena);

            assertThat(cadena.getRequest()).as("%s: la cadena NO se llama", metodo).isNull();
            assertThat(respuesta.getStatus()).as("%s", metodo).isEqualTo(403);
            assertThat(causa(respuesta)).as("%s", metodo).isEqualTo("CURSO_SOLO_LECTURA");
            assertThat(mensaje(respuesta)).as("%s", metodo).contains(ARCHIVADO);
        }
    }

    /** (3) Los métodos seguros pasan aunque el curso esté archivado: leer siempre se puede. */
    @Test
    void archivado_metodosSegurosPasan() throws Exception {
        for (String metodo : new String[] {"GET", "HEAD", "OPTIONS"}) {
            MockFilterChain cadena = new MockFilterChain();
            MockHttpServletResponse respuesta = new MockHttpServletResponse();

            guarda(archivado()).doFilter(peticion(metodo, "/api/niveles"), respuesta, cadena);

            assertThat(cadena.getRequest()).as("%s pasa", metodo).isNotNull();
            assertThat(respuesta.getStatus()).as("%s", metodo).isEqualTo(200);
        }
    }

    /**
     * (4) El recurso de cursos está exento —por él se sale de un curso archivado—, y la
     * exención es por SEGMENTO: {@code /api/cursosX} no es ese recurso y no queda exento. Con
     * un {@code startsWith} a secas, cualquier ruta que empezara por esas letras se colaría.
     */
    @Test
    void archivado_cursosExentoPeroNoLoQueSoloEmpiezaIgual() throws Exception {
        for (String ruta : new String[] {"/api/cursos", "/api/cursos/algo"}) {
            MockFilterChain cadena = new MockFilterChain();

            guarda(archivado())
                    .doFilter(peticion("POST", ruta), new MockHttpServletResponse(), cadena);

            assertThat(cadena.getRequest()).as("%s exento", ruta).isNotNull();
        }

        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        guarda(archivado()).doFilter(peticion("POST", "/api/cursosX"), respuesta, cadena);

        assertThat(cadena.getRequest()).as("/api/cursosX NO está exento").isNull();
        assertThat(respuesta.getStatus()).isEqualTo(403);
    }

    /**
     * (5) Una ruta que no existe y un método sin mapping también se rechazan. Es la razón de
     * ser del filtro frente a un interceptor: sin esto, un curso archivado contestaría cosas
     * distintas según si la URL resuelve a un handler.
     */
    @Test
    void archivado_rutaSinHandlerYMetodoSinMapping_403() throws Exception {
        MockFilterChain sinRuta = new MockFilterChain();
        MockHttpServletResponse respuestaSinRuta = new MockHttpServletResponse();

        guarda(archivado()).doFilter(peticion("PUT", "/api/inexistente"), respuestaSinRuta, sinRuta);

        assertThat(sinRuta.getRequest()).isNull();
        assertThat(respuestaSinRuta.getStatus()).isEqualTo(403);
        assertThat(causa(respuestaSinRuta)).isEqualTo("CURSO_SOLO_LECTURA");

        MockFilterChain sinMetodo = new MockFilterChain();
        MockHttpServletResponse respuestaSinMetodo = new MockHttpServletResponse();

        guarda(archivado())
                .doFilter(peticion("PATCH", "/api/niveles/1"), respuestaSinMetodo, sinMetodo);

        assertThat(sinMetodo.getRequest()).isNull();
        assertThat(respuestaSinMetodo.getStatus()).isEqualTo(403);
    }

    /** (6) Lo que no es API no se guarda: el bundle de Angular se sirve igual. */
    @Test
    void archivado_fueraDeLaApiPasa() throws Exception {
        MockFilterChain cadena = new MockFilterChain();

        guarda(archivado())
                .doFilter(peticion("POST", "/fuera-de-api"), new MockHttpServletResponse(), cadena);

        assertThat(cadena.getRequest()).isNotNull();
    }

    /**
     * (7) Mientras se crea un curso nuevo se rechaza igual, con causa PROPIA y sin hablar de
     * solo lectura: el curso no está archivado todavía y decir lo contrario mandaría al
     * usuario a buscar un curso activo que es este mismo.
     */
    @Test
    void duplicando_seRechazaConCausaPropia() throws Exception {
        EstadoCurso estado = activo();
        estado.iniciarDuplicado();
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        guarda(estado).doFilter(peticion("POST", "/api/niveles"), respuesta, cadena);

        assertThat(cadena.getRequest()).isNull();
        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(causa(respuesta)).isEqualTo("CURSO_DUPLICANDOSE");
        assertThat(mensaje(respuesta)).contains("Vuelve a intentarlo");
        assertThat(estado.archivado()).as("duplicar no archiva por sí solo").isFalse();
    }

    /**
     * (8) El cuerpo sale en UTF-8 y el Content-Type lo dice. El aserto va sobre los BYTES
     * decodificados como UTF-8, no sobre {@code getContentAsString()}: con el charset por
     * defecto del contenedor los bytes de «está» son otros y el navegador muestra un rombo.
     */
    @Test
    void elCuerpoSaleEnUtf8YElContentTypeLoDice() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        guarda(archivado())
                .doFilter(peticion("POST", "/api/niveles"), respuesta, new MockFilterChain());

        assertThat(respuesta.getContentType()).contains("application/json");
        assertThat(respuesta.getContentType().toUpperCase()).contains("UTF-8");
        assertThat(respuesta.getCharacterEncoding().toUpperCase()).isEqualTo("UTF-8");
        String desdeBytes =
                new String(respuesta.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(desdeBytes).contains("está").contains(ARCHIVADO);
    }

    // ─────────────────────────────────────────────────────────────────────────────── andamio

    private static GuardaSoloLectura guarda(EstadoCurso estado) {
        return new GuardaSoloLectura(estado, JSON);
    }

    /** Estado de una base sin nombre de curso: ni archivada ni duplicando. */
    private static EstadoCurso activo() {
        return new EstadoCurso(null);
    }

    private static EstadoCurso archivado() {
        EstadoCurso estado = new EstadoCurso(null);
        estado.marcarArchivado(ARCHIVADO);
        return estado;
    }

    private static MockHttpServletRequest peticion(String metodo, String ruta) {
        return new MockHttpServletRequest(metodo, ruta);
    }

    private static String causa(MockHttpServletResponse respuesta) {
        return campo(respuesta, "causa");
    }

    private static String mensaje(MockHttpServletResponse respuesta) {
        return campo(respuesta, "message");
    }

    /** Lee un campo del cuerpo por el árbol, para no atarse al orden de los campos. */
    private static String campo(MockHttpServletResponse respuesta, String nombre) {
        try {
            String cuerpo =
                    new String(respuesta.getContentAsByteArray(), StandardCharsets.UTF_8);
            return JSON.readTree(cuerpo).path(nombre).asString();
        } catch (Exception e) {
            throw new IllegalStateException("cuerpo no parseable", e);
        }
    }
}
