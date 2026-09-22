package es.yaroki.educhronos.app.curso;

import es.yaroki.educhronos.app.web.dto.RechazoCursoDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Un curso archivado no se modifica, y mientras se está creando uno nuevo no se modifica
 * nada (O-curso, S159, fase B de C-duplicado-guarda).
 *
 * <p><b>Es el PRIMER enganche transversal del proyecto.</b> Hasta S159 no había ni un
 * {@code Filter}, ni un {@code HandlerInterceptor}, ni un {@code @ControllerAdvice}: cada
 * controlador traducía sus propias excepciones y el único {@code WebMvcConfigurer} era el de
 * los recursos estáticos. Esto rompe esa costumbre a propósito, porque la pregunta que
 * responde —«¿se puede escribir?»— no es de ningún controlador en particular.
 *
 * <p><b>Filtro y no interceptor.</b> Un {@code HandlerInterceptor} sólo corre para las
 * peticiones que resuelven a un handler: un {@code PATCH} a una ruta inexistente se iría en
 * un 404 sin pasar por la guarda, y un curso archivado contestaría cosas distintas según si
 * la URL existe. Medido en S159 (Q5): hay 34 handlers no-GET y ninguno queda fuera, pero lo
 * que la guarda tiene que cubrir es el MÉTODO, no la lista de rutas.
 *
 * <p><b>Guarda GENÉRICA, sin lista de endpoints.</b> Todo lo que no sea seguro se rechaza,
 * en vez de enumerar los 34 handlers de escritura. Es la decisión escrita en la ficha de
 * O-curso en S159, y lo que la justifica es el futuro: cubrirá el
 * {@code PUT /api/profesores/{id}/restricciones-horarias} que traerá O-disponibilidad sin
 * que nadie tenga que acordarse de añadirlo aquí.
 *
 * <p><b>Por qué 403 y no otro código.</b> Medido en S159 (M2-7): el frontend ya tiene ramas
 * propias para 400 (validación), 404 («no tiene PDC»), 409 (referencias entrantes), 422 y
 * 503 (fallos del solver). El 403 no lo usa nadie, así que entra sin chocar con nada y cae
 * en el degradado genérico de los diecinueve {@code mensaje()}, que muestran el
 * {@code message} del cuerpo tal cual.
 *
 * <p><b>Por qué {@code /api/cursos} está exento.</b> Un curso archivado tiene que poder
 * salir de sí mismo: el selector de cursos de la condición 5 vivirá en ese recurso, y si la
 * guarda lo tapara, abrir un curso viejo dejaría la aplicación sin salida más que reiniciar.
 * La exención no lo deja indefenso: el propio servicio rechaza duplicar un curso archivado
 * con un 409 {@code CURSO_ARCHIVADO}. La comprobación es por segmento y no por prefijo, para
 * que {@code /api/cursosX} —que no es este recurso— NO quede exento.
 *
 * <p><b>El cuerpo lo escribe el filtro, a mano.</b> No se lanza una excepción para que la
 * traduzca Spring: lo que puebla el {@code reason} de un {@code ResponseStatusException} se
 * lee del {@code MockHttpServletResponse} y no del cuerpo que viaja por la red
 * (D-F8.6-ii-a), de modo que un test verde sobre él no probaría que el navegador recibe la
 * causa. Se serializa {@link RechazoCursoDTO} con el mapper del proyecto —el
 * {@code JsonMapper} de Jackson 3 que autoconfigura Boot 4.1— y se fija el juego de
 * caracteres: sin él, «está» sale en el charset por defecto del contenedor y el navegador lo
 * muestra roto.
 */
@Component
public class GuardaSoloLectura extends OncePerRequestFilter {

    /** Se está creando un curso nuevo: ninguna escritura entra mientras dura. */
    public static final String CURSO_DUPLICANDOSE = "CURSO_DUPLICANDOSE";

    /** El curso abierto está archivado: es de solo lectura. */
    public static final String CURSO_SOLO_LECTURA = "CURSO_SOLO_LECTURA";

    /** Raíz de todo lo que es API: lo de fuera es el bundle de Angular y no se guarda. */
    static final String RAIZ_API = "/api/";

    /** Recurso exento: por él se sale de un curso archivado. */
    static final String RECURSO_CURSOS = "/api/cursos";

    /** Métodos que no modifican nada y por tanto no se miran. */
    static final Set<String> METODOS_SEGUROS =
            Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());

    private final EstadoCurso estado;

    private final ObjectMapper json;

    public GuardaSoloLectura(EstadoCurso estado, ObjectMapper json) {
        this.estado = estado;
        this.json = json;
    }

    /**
     * Lo que la guarda no mira siquiera: los métodos seguros, todo lo que no sea API y el
     * recurso de cursos.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        if (METODOS_SEGUROS.contains(peticion.getMethod())) {
            return true;
        }
        String ruta = ruta(peticion);
        if (!ruta.startsWith(RAIZ_API)) {
            return true;
        }
        return ruta.equals(RECURSO_CURSOS) || ruta.startsWith(RECURSO_CURSOS + "/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (estado.duplicando()) {
            rechazar(
                    respuesta,
                    CURSO_DUPLICANDOSE,
                    "Se está creando un curso nuevo. Vuelve a intentarlo en un momento.");
            return;
        }
        if (estado.archivado()) {
            rechazar(
                    respuesta,
                    CURSO_SOLO_LECTURA,
                    "El curso " + estado.nombre() + " está archivado y es de solo lectura."
                            + " Los cambios se hacen en el curso activo.");
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    /**
     * La ruta de la petición sin el context path: lo que se compara con {@code /api/…} tiene
     * que ser independiente de dónde esté desplegada la aplicación.
     */
    private static String ruta(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String contexto = peticion.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)
                ? uri.substring(contexto.length())
                : uri;
    }

    /** Escribe el 403 y su cuerpo. No se llama a la cadena: la petición muere aquí. */
    private void rechazar(HttpServletResponse respuesta, String causa, String mensaje)
            throws IOException {
        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(json.writeValueAsString(new RechazoCursoDTO(causa, mensaje)));
    }
}
