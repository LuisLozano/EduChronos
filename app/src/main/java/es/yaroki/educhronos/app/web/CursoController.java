package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.curso.CursoService;
import es.yaroki.educhronos.app.curso.EstadoCurso;
import es.yaroki.educhronos.app.curso.NombreCurso;
import es.yaroki.educhronos.app.curso.RechazoCursoException;
import es.yaroki.educhronos.app.web.dto.CursoCreadoDTO;
import es.yaroki.educhronos.app.web.dto.CursoDTO;
import es.yaroki.educhronos.app.web.dto.DuplicarCursoRequest;
import es.yaroki.educhronos.app.web.dto.RechazoCursoDTO;
import java.nio.file.Path;
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
 * del molde: el estado del curso abierto es un singleton ({@code /api/curso}) y crear uno
 * nuevo es añadir a una colección ({@code /api/cursos}). Meterlos bajo una raíz común
 * obligaría a que uno de los dos mintiera sobre su cardinalidad.
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
     * Crea el curso siguiente a partir del abierto y archiva el abierto. 201 porque crea un
     * recurso nuevo: un fichero de curso que antes no existía.
     */
    @PostMapping("/api/cursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CursoCreadoDTO duplicar(@RequestBody DuplicarCursoRequest peticion) {
        Path destino = service.duplicar(peticion.nombreNuevo(), peticion.nombreActual());
        return new CursoCreadoDTO(peticion.nombreNuevo(), destino.getFileName().toString());
    }

    @ExceptionHandler(RechazoCursoException.class)
    ResponseEntity<RechazoCursoDTO> rechazo(RechazoCursoException e) {
        return ResponseEntity.status(e.status())
                .body(new RechazoCursoDTO(e.causa(), e.getMessage()));
    }
}
