package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST de SOLO LECTURA de Fase 7 (Bloque 7A). Expone la proyección plana de
 * un horario ya persistido para las vistas (grupo, profesor, aula). No genera, no
 * lista, no muta: la generación y la persistencia siguen siendo del servicio.
 *
 * <p>El {@code IllegalArgumentException} que lanza
 * {@link GeneradorHorarioService#proyectar} cuando el id no existe se traduce a
 * {@code 404 Not Found}; todo lo demás (errores de integridad) se deja propagar.
 */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final GeneradorHorarioService service;

    public HorarioController(GeneradorHorarioService service) {
        this.service = service;
    }

    @GetMapping("/{id}/proyeccion")
    public HorarioProyeccionDTO proyeccion(@PathVariable Long id) {
        try {
            return service.proyectar(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
