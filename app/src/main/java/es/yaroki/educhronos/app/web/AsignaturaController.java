package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.AsignaturaService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.web.dto.AsignaturaDTO;
import es.yaroki.educhronos.app.web.dto.AsignaturaRequest;
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
 * Capa REST FINA del CRUD de {@code Asignatura} (§4.1, Bloque 8.5-A, piloto del
 * patrón CRUD de catálogo). Toda la lógica y la validación viven en
 * {@link AsignaturaService}; el controlador solo enruta y traduce excepciones a
 * códigos HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce las
 * suyas, patrón de 7A que ya siguen {@code HorarioController} y {@code BloqueoController}).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación) → {@code 400}. En el {@code PUT}
 * ambos son posibles y hay que distinguirlos por tipo.
 */
@RestController
@RequestMapping("/api/asignaturas")
public class AsignaturaController {

    private final AsignaturaService service;

    public AsignaturaController(AsignaturaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AsignaturaDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public AsignaturaDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignaturaDTO crear(@RequestBody AsignaturaRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public AsignaturaDTO editar(@PathVariable("id") Long id, @RequestBody AsignaturaRequest peticion) {
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

    // ----------------------------- sub-recurso: aulas compatibles (§4.7, Bloque 8.5-C3)

    @GetMapping("/{id}/aulas-compatibles")
    public List<String> aulasCompatibles(@PathVariable("id") Long id) {
        try {
            return service.obtenerAulasCompatibles(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}/aulas-compatibles")
    public List<String> reemplazarAulasCompatibles(
            @PathVariable("id") Long id, @RequestBody List<String> tipos) {
        try {
            return service.reemplazarAulasCompatibles(id, tipos);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
