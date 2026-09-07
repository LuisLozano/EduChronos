package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReplicacionService;
import es.yaroki.educhronos.app.web.dto.ParteDeshacerDTO;
import es.yaroki.educhronos.app.web.dto.PlanReplicacionDTO;
import es.yaroki.educhronos.app.web.dto.ReplicacionRequest;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA del sub-recurso REPLICACIÓN (Bloque S139, C-replicación-alta): poblar un
 * grupo ordinario nuevo copiando la estructura de un hermano, en
 * {@code /api/grupos/{id}/replicacion}. Controlador SEPARADO de {@link GrupoController}
 * —molde de {@link PdcController}— para no alterar su ctor ni su contrato; sus rutas de dos
 * segmentos no colisionan con las de un segmento de aquél.
 *
 * <p>Toda la lógica y la validación viven en {@link ReplicacionService}; el controlador solo
 * enruta y traduce excepciones a códigos HTTP POR TIPO (sin {@code @ControllerAdvice}, patrón
 * vigente): {@link NoSuchElementException} (grupo o hermano inexistente) → {@code 404};
 * {@link IllegalArgumentException} (validación del par, de los códigos derivados o de las
 * asignaciones) → {@code 400} con el mensaje en el reason;
 * {@link ReferenciaEntranteException} (alguna actividad afectada tiene dependientes) →
 * {@code 409} con el desglose.
 *
 * <p>El {@code POST} traduce las TRES, el {@code GET} solo las dos primeras —el plan se puede
 * consultar aunque haya un horario colgando, porque consultarlo no escribe— y el
 * {@code DELETE} (Bloque S140, C-alta-reversible: deshacer la replicación) la primera y la
 * tercera, porque no recibe cuerpo y no tiene nada que validar. Traducir por TIPO y no por
 * endpoint es lo que hace que esas diferencias sean del SERVICIO —cada método lanza lo que le
 * corresponde— y no de una tabla de códigos por ruta.
 */
@RestController
@RequestMapping("/api/grupos/{id}/replicacion")
public class ReplicacionController {

    private final ReplicacionService service;

    public ReplicacionController(ReplicacionService service) {
        this.service = service;
    }

    @GetMapping
    public PlanReplicacionDTO planificar(@PathVariable("id") Long id,
            @RequestParam("hermano") String hermano) {
        try {
            return service.planificar(id, hermano);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanReplicacionDTO replicar(@PathVariable("id") Long id,
            @RequestBody ReplicacionRequest peticion) {
        try {
            return service.replicar(id, peticion);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Deshace la replicación: deja el grupo sin subgrupos y devuelve el parte (Bloque S140).
     *
     * <p><b>{@code 200} con parte VACÍO si el grupo ya está pelado</b>, no 404 ni 409: el
     * {@code DELETE} es idempotente y llamarlo dos veces no puede ser un error. El 404 queda
     * para lo único que sí lo es, que el grupo no exista.
     *
     * <p>No traduce {@link IllegalArgumentException}: el deshacer no valida cuerpo alguno —no
     * lo tiene—, así que un 400 por esta ruta no describiría nada que el cliente pueda
     * corregir. Las dos que sí lanza el servicio siguen traduciéndose por TIPO, igual que en
     * las otras dos rutas.
     */
    @DeleteMapping
    public ParteDeshacerDTO deshacer(@PathVariable("id") Long id) {
        try {
            return service.deshacer(id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }
}
