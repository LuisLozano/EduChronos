package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.JornadaService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.JornadaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA de la JORNADA del centro (§4.1, C-jornada M3). Toda la lógica y la
 * validación viven en {@link JornadaService}; el controlador solo enruta y traduce
 * excepciones a códigos HTTP (sin {@code @ControllerAdvice} global; cada controlador
 * traduce las suyas, patrón de 7A que ya siguen {@code AsignaturaController} y
 * {@code BloqueoController}). El {@code @Transactional} vive en el servicio, no aquí.
 *
 * <p><b>Recurso SINGLETON</b>: la jornada del centro es una y no tiene id, así que no hay
 * {@code /{id}} ni {@code POST} ni {@code DELETE} —solo {@code GET} y un {@code PUT} de
 * reemplazo total—. De ahí la ausencia notable frente al molde CRUD: <b>este controlador
 * no devuelve 404 nunca</b>. Una jornada sin configurar no es "no encontrada": el
 * {@code GET} responde 200 con la malla de referencia y {@code persistida=false}.
 *
 * <p>Traducciones por TIPO de excepción, no por endpoint:
 * {@link IllegalArgumentException} (validación: día o horas malas, solapes, más de 6
 * lectivos en un día) → {@code 400}; {@link ReferenciaEntranteException} (hay horarios,
 * restricciones o bloqueos apuntando a la malla actual) → {@code 409}. En el {@code PUT}
 * ambas son posibles y hay que distinguirlas por tipo.
 */
@RestController
@RequestMapping("/api/jornada")
public class JornadaController {

    private final JornadaService service;

    public JornadaController(JornadaService service) {
        this.service = service;
    }

    @GetMapping
    public JornadaDTO obtener() {
        return service.obtenerJornada();
    }

    @PutMapping
    public JornadaDTO reemplazar(@RequestBody JornadaRequest peticion) {
        try {
            return service.reemplazarJornada(peticion);
        } catch (ReferenciaEntranteException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
