package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.ProfesorService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.RestriccionHorariaService;
import es.yaroki.educhronos.app.web.dto.ProfesorDTO;
import es.yaroki.educhronos.app.web.dto.ProfesorRequest;
import es.yaroki.educhronos.app.web.dto.RestriccionHorariaDTO;
import es.yaroki.educhronos.app.web.dto.RestriccionHorariaRequest;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA del CRUD de {@code Profesor} (§4.1, Bloque 8.5-A', réplica del
 * piloto {@code AsignaturaController}). Toda la lógica y la validación viven en
 * {@link ProfesorService}; el controlador solo enruta y traduce excepciones a
 * códigos HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce
 * las suyas).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación) → {@code 400}. En el {@code PUT}
 * ambos son posibles y hay que distinguirlos por tipo.
 *
 * <p>Aloja además el sub-recurso {@code /{id}/restricciones-horarias} (§4.3, Bloque
 * 8.5-E), delegado en {@link RestriccionHorariaService}: sub-recurso INLINE en el
 * controlador del padre (patrón S75 de {@code AsignaturaController} con
 * {@code /aulas-compatibles} y S77 de {@code GrupoController} con {@code /tutoria}), no
 * controlador aparte como {@code PdcController}. Consecuencia asumida: el ctor gana un
 * colaborador y los {@code standaloneSetup} de los tests lo reflejan.
 */
@RestController
@RequestMapping("/api/profesores")
public class ProfesorController {

    private final ProfesorService service;
    private final RestriccionHorariaService restriccionService;

    public ProfesorController(
            ProfesorService service, RestriccionHorariaService restriccionService) {
        this.service = service;
        this.restriccionService = restriccionService;
    }

    @GetMapping
    public List<ProfesorDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProfesorDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesorDTO crear(@RequestBody ProfesorRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public ProfesorDTO editar(@PathVariable("id") Long id, @RequestBody ProfesorRequest peticion) {
        try {
            return service.editar(id, peticion);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable("id") Long id) {
        try {
            service.borrar(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    // ------------------- sub-recurso: restricciones horarias del profesor (Bloque 8.5-E)

    @GetMapping("/{id}/restricciones-horarias")
    public List<RestriccionHorariaDTO> restriccionesHorarias(@PathVariable("id") Long id) {
        try {
            return restriccionService.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    /**
     * Reemplazo TOTAL de las restricciones horarias del profesor. Un cuerpo vacío
     * ({@code []}) las borra todas y devuelve {@code 200} con lista vacía: es un PUT
     * legítimo, no un error.
     */
    @PutMapping("/{id}/restricciones-horarias")
    public List<RestriccionHorariaDTO> reemplazarRestriccionesHorarias(
            @PathVariable("id") Long id, @RequestBody List<RestriccionHorariaRequest> peticiones) {
        try {
            return restriccionService.reemplazar(id, peticiones);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
