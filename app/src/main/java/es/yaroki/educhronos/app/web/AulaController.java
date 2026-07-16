package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.AulaService;
import es.yaroki.educhronos.app.web.dto.AulaDTO;
import es.yaroki.educhronos.app.web.dto.AulaRequest;
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
 * Capa REST FINA del CRUD de {@code Aula} (§4.1, Bloque 8.5-A', réplica del piloto
 * {@code AsignaturaController}). Toda la lógica y la validación viven en
 * {@link AulaService}; el controlador solo enruta y traduce excepciones a códigos
 * HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce las suyas).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación, incluido un {@code tipo} no
 * parseable a {@code TipoAula}) → {@code 400}. En el {@code PUT} ambos son posibles
 * y hay que distinguirlos por tipo.
 */
@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final AulaService service;

    public AulaController(AulaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AulaDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AulaDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AulaDTO crear(@RequestBody AulaRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public AulaDTO editar(@PathVariable("id") Long id, @RequestBody AulaRequest peticion) {
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
        }
    }
}
