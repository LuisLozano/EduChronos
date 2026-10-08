package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.ConfiguracionGuardiasService;
import es.yaroki.educhronos.app.web.dto.ConfiguracionGuardiasDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA del mínimo de profesores de guardia por tramo del centro (S212,
 * C-dato-guardias), con el molde de {@link JornadaController}: recurso SINGLETON, sin id, solo
 * {@code GET} y un {@code PUT} de reemplazo. Nunca 404: sin valor guardado, el {@code GET}
 * responde el de por defecto. La validación vive en {@link ConfiguracionGuardiasService};
 * {@link IllegalArgumentException} → {@code 400}.
 */
@RestController
@RequestMapping("/api/configuracion-guardias")
public class ConfiguracionGuardiasController {

    private final ConfiguracionGuardiasService service;

    public ConfiguracionGuardiasController(ConfiguracionGuardiasService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracionGuardiasDTO obtener() {
        return service.obtener();
    }

    @PutMapping
    public ConfiguracionGuardiasDTO reemplazar(@RequestBody ConfiguracionGuardiasDTO peticion) {
        try {
            return service.reemplazar(peticion);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
