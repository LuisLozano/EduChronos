package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.ActividadService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.web.dto.ActividadDTO;
import es.yaroki.educhronos.app.web.dto.ActividadRequest;
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
 * Capa REST FINA del CRUD de {@code Actividad} como AGREGADO (§4.6, Bloque 8.5-C1, réplica
 * del piloto {@code AsignaturaController}). Actividad es la raíz y sus {@code Plaza}s
 * viajan embebidas: NO hay {@code /api/plazas}. Toda la lógica y la validación viven en
 * {@link ActividadService}; el controlador solo enruta y traduce excepciones a códigos HTTP
 * (sin {@code @ControllerAdvice} global; cada controlador traduce las suyas).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación: escalares, patrón, XOR, I7, I2,
 * referencias, unicidad) → {@code 400} con el mensaje en el reason. En el {@code PUT}
 * ambos son posibles y hay que distinguirlos por tipo.
 */
@RestController
@RequestMapping("/api/actividades")
public class ActividadController {

    private final ActividadService service;

    public ActividadController(ActividadService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActividadDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ActividadDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActividadDTO crear(@RequestBody ActividadRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public ActividadDTO editar(@PathVariable("id") Long id, @RequestBody ActividadRequest peticion) {
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
}
