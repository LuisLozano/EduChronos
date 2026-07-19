package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.PdcService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.PdcRequest;
import java.util.NoSuchElementException;
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
 * Capa REST FINA del sub-recurso PDC (§4.1, Bloque 8.5-D1): el grupo de Diversificación
 * colgado de {@code /api/grupos/{idPadre}/pdc}. Controlador SEPARADO de
 * {@link GrupoController} para no alterar su ctor ni su contrato; sus rutas de dos
 * segmentos ({@code /{idPadre}/pdc}) no colisionan con las de un segmento de aquél.
 *
 * <p>Toda la lógica y la validación viven en {@link PdcService}; el controlador solo
 * enruta y traduce excepciones a códigos HTTP POR TIPO (sin {@code @ControllerAdvice},
 * patrón vigente):
 * {@link NoSuchElementException} (padre inexistente o sin PDC) → {@code 404};
 * {@link IllegalArgumentException} (validación de alta) → {@code 400} con el mensaje en el
 * reason; {@link ReferenciaEntranteException} (subgrupo mono-Di en uso) → {@code 409}.
 */
@RestController
@RequestMapping("/api/grupos/{idPadre}/pdc")
public class PdcController {

    private final PdcService service;

    public PdcController(PdcService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoDTO crear(@PathVariable("idPadre") Long idPadre, @RequestBody PdcRequest peticion) {
        try {
            return service.crear(idPadre, peticion);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping
    public GrupoDTO obtener(@PathVariable("idPadre") Long idPadre) {
        try {
            return service.obtener(idPadre);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable("idPadre") Long idPadre) {
        try {
            service.borrar(idPadre);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }
}
