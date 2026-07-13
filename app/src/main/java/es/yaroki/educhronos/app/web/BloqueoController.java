package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.BloqueoService;
import es.yaroki.educhronos.app.web.dto.BloqueoDTO;
import es.yaroki.educhronos.app.web.dto.BloqueoRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA de bloqueos manuales (§4.7, Bloque 8.2b-iv). Da de alta/reemplaza,
 * lista y borra bloqueos; toda la lógica y la persistencia viven en
 * {@link BloqueoService}. El controlador solo enruta y traduce excepciones a
 * códigos HTTP (sin {@code @ControllerAdvice} global; cada controlador traduce las
 * suyas, patrón de 7A que ya sigue {@code HorarioController}).
 *
 * <p>Traducciones: {@code IllegalArgumentException} del alta (reglas de validación
 * de D-3) → {@code 400}; {@code IllegalArgumentException} del borrado (id
 * inexistente) → {@code 404}. La MISMA excepción se traduce distinto según el
 * endpoint, igual que en {@code HorarioController}.
 */
@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    private final BloqueoService service;

    public BloqueoController(BloqueoService service) {
        this.service = service;
    }

    /**
     * Da de alta o reemplaza un bloqueo (idempotente por instancia, D-4) y devuelve
     * el {@link BloqueoDTO} resultante con {@code 200}. Un fallo de validación
     * (D-3) → {@code 400}.
     */
    @PostMapping
    public BloqueoDTO guardar(@RequestBody BloqueoRequest peticion) {
        try {
            return service.guardar(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<BloqueoDTO> listar() {
        return service.listar();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable("id") Long id) {
        try {
            service.borrar(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
