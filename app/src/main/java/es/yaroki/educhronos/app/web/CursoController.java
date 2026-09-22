package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.curso.CursoService;
import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.curso.NombreCurso;
import es.yaroki.educhronos.app.curso.RechazoCursoException;
import es.yaroki.educhronos.app.web.dto.CursoCreadoDTO;
import es.yaroki.educhronos.app.web.dto.AbrirCursoRequest;
import es.yaroki.educhronos.app.web.dto.CursoDTO;
import es.yaroki.educhronos.app.web.dto.CursoListadoDTO;
import es.yaroki.educhronos.app.web.dto.DuplicarCursoRequest;
import es.yaroki.educhronos.app.web.dto.RechazoCursoDTO;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El curso abierto y la creación del siguiente (O-curso, S159).
 *
 * <p>Calca a {@link JornadaController}: recurso de UNA sola instancia, {@code GET} que
 * <b>no devuelve 404 nunca</b> —una base sin nombre de curso no es «no encontrada», es una
 * base de antes de S159 y responde 200 con {@code nombre: null}— y traducción de excepciones
 * aquí mismo, sin {@code @ControllerAdvice} global.
 *
 * <p><b>Dos rutas y no un {@code @RequestMapping} de clase</b>, que es la única desviación
 * del molde: el estado del curso abierto es un singleton ({@code /api/curso}) y la colección
 * de cursos de la carpeta es otra cosa ({@code /api/cursos}). Meterlos bajo una raíz común
 * obligaría a que uno de los dos mintiera sobre su cardinalidad.
 *
 * <p><b>{@code /api/cursos/abrir} es un POST y no un PUT sobre {@code /api/curso}</b> (S160).
 * Abrir un curso no es escribir el estado del curso abierto: es una operación con turno,
 * espera y marcha atrás, que puede contestar 409, 503 o 500 por razones que no tienen nada
 * que ver con lo que se envía. Un verbo bajo la colección lo dice; un {@code PUT} sobre el
 * singleton prometería que el cuerpo enviado es el estado resultante, y no lo es.
 *
 * <p>El recurso {@code /api/cursos} —y todo lo que cuelga de él— está EXENTO de
 * {@code GuardaSoloLectura} por segmento, que es lo que permite salir de un curso archivado.
 *
 * <p><b>Traducción por tipo, con cuerpo propio.</b> {@link RechazoCursoException} trae ya su
 * status y su causa, y sale como {@link RechazoCursoDTO} —no como
 * {@code ResponseStatusException}— porque el {@code reason} de Spring no llega al navegador
 * de forma que un test pueda aseverarla (D-F8.6-ii-a). Todo lo que no sea un rechazo
 * previsto sube sin tocar y acaba en 500.
 */
@RestController
public class CursoController {

    private final EstadoCurso estado;

    private final CursoService service;

    public CursoController(EstadoCurso estado, CursoService service) {
        this.estado = estado;
        this.service = service;
    }

    /** El curso abierto, con la propuesta de nombre para el siguiente si se puede derivar. */
    @GetMapping("/api/curso")
    public CursoDTO obtener() {
        String nombre = estado.nombre();
        return new CursoDTO(
                nombre,
                estado.archivado(),
                nombre == null ? null : NombreCurso.siguiente(nombre));
    }

    /**
     * Los cursos de la carpeta de datos, con cuál está abierto (S160). Nunca 404 y nunca
     * vacío: la base abierta siempre es uno de ellos.
     */
    @GetMapping("/api/cursos")
    public List<CursoListadoDTO> listar() {
        return service.listar();
    }

    /**
     * Crea el curso siguiente a partir del abierto, archiva el abierto y deja abierto el
     * nuevo. 201 porque crea un recurso nuevo: un fichero de curso que antes no existía.
     *
     * <p>El cuerpo nombra el curso CREADO, no el abierto. Desde S160 los dos coinciden —el
     * nuevo queda abierto—, salvo si la apertura falla después del duplicado, y en ese caso
     * lo que sale no es este cuerpo sino el rechazo de la apertura.
     */
    @PostMapping("/api/cursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoCreadoDTO duplicar(@RequestBody DuplicarCursoRequest peticion) {
        Path destino = service.duplicar(peticion.nombreNuevo(), peticion.nombreActual());
        return new CursoCreadoDTO(peticion.nombreNuevo(), destino.getFileName().toString());
    }

    /**
     * Abre otro curso de la carpeta y devuelve el estado resultante, con la misma forma que
     * el {@code GET /api/curso} (S160). 200 y no 201: no se crea nada.
     *
     * <p>El estado se lee DESPUÉS de abrir, por el mismo método que sirve el GET, para que un
     * cliente que llame a los dos no pueda ver dos respuestas distintas.
     */
    @PostMapping("/api/cursos/abrir")
    public CursoDTO abrir(@RequestBody AbrirCursoRequest peticion) {
        service.abrir(peticion.fichero());
        return obtener();
    }

    @ExceptionHandler(RechazoCursoException.class)
    ResponseEntity<RechazoCursoDTO> rechazo(RechazoCursoException e) {
        return ResponseEntity.status(e.status())
                .body(new RechazoCursoDTO(e.causa(), e.getMessage()));
    }
}
