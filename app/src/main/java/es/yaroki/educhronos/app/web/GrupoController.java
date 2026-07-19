package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.GrupoService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.TutoriaService;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.GrupoRequest;
import es.yaroki.educhronos.app.web.dto.TutoriaDTO;
import es.yaroki.educhronos.app.web.dto.TutoriaRequest;
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
 * Capa REST FINA del CRUD de {@code GrupoAdministrativo} (§4.1, Bloque 8.5-B, réplica
 * del piloto {@code AulaController}). Toda la lógica y la validación viven en
 * {@link GrupoService}; el controlador solo enruta y traduce excepciones a códigos
 * HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce las suyas).
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link NoSuchElementException} (id inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación: código, tipo no ordinario/no
 * parseable, nivel inexistente, unicidad) → {@code 400} con el mensaje en el reason.
 * En el {@code PUT} ambos son posibles y hay que distinguirlos por tipo.
 *
 * <p>Aloja además el sub-recurso {@code /{id}/tutoria} (I4, Bloque 8.5-D2a), delegado en
 * {@link TutoriaService}: sub-recurso INLINE en el controlador del padre (patrón S75 de
 * {@code AsignaturaController} con {@code /aulas-compatibles}), no controlador aparte
 * como {@code PdcController}. Consecuencia asumida: el ctor gana un colaborador y los
 * {@code standaloneSetup} de los tests lo reflejan.
 */
@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService service;
    private final TutoriaService tutoriaService;

    public GrupoController(GrupoService service, TutoriaService tutoriaService) {
        this.service = service;
        this.tutoriaService = tutoriaService;
    }

    @GetMapping
    public List<GrupoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public GrupoDTO obtener(@PathVariable("id") Long id) {
        try {
            return service.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoDTO crear(@RequestBody GrupoRequest peticion) {
        try {
            return service.crear(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}")
    public GrupoDTO editar(@PathVariable("id") Long id, @RequestBody GrupoRequest peticion) {
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

    // --------------------------------- sub-recurso: tutoría del grupo (I4, Bloque 8.5-D2a)

    @GetMapping("/{id}/tutoria")
    public List<TutoriaDTO> tutoria(@PathVariable("id") Long id) {
        try {
            return tutoriaService.obtener(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}/tutoria")
    public List<TutoriaDTO> reemplazarTutoria(
            @PathVariable("id") Long id, @RequestBody List<TutoriaRequest> peticiones) {
        try {
            return tutoriaService.reemplazar(id, peticiones);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
