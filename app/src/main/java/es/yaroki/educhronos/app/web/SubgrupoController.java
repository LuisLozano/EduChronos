package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.SubgrupoService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.web.dto.SubgrupoDTO;
import es.yaroki.educhronos.app.web.dto.SubgrupoRequest;
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
 * Capa REST FINA del CRUD de {@code Subgrupo} (§4.2, Bloque 8.5-B, réplica del piloto
 * {@code AulaController}). Toda la lógica y la validación viven en
 * {@link SubgrupoService}; el controlador solo enruta y traduce excepciones a códigos
 * HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce las suyas).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación: código, grupos vacíos, grupo
 * inexistente, unicidad, subgrupo mono-Di de un PDC) → {@code 400} con el mensaje en el
 * reason; {@link ReferenciaEntranteException} (plazas que lo retienen) → {@code 409}.
 * En el {@code PUT} las dos primeras son posibles y hay que distinguirlas por tipo.
 *
 * <p>El {@code DELETE} traduce las TRES. La de validación es la última en llegar (la
 * añadió la guarda del mono-Di): hasta entonces el borrado solo podía fallar por id
 * inexistente o por referencia entrante, y sin este {@code catch} un rechazo de
 * validación se escapaba como 500 en vez de como el 400 que es.
 */
@RestController
@RequestMapping("/api/subgrupos")
public class SubgrupoController {

    private final SubgrupoService service;

    public SubgrupoController(SubgrupoService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubgrupoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public SubgrupoDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubgrupoDTO crear(@RequestBody SubgrupoRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public SubgrupoDTO editar(@PathVariable("id") Long id, @RequestBody SubgrupoRequest peticion) {
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
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }
}
