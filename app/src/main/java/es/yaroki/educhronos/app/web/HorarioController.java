package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import es.yaroki.educhronos.app.web.dto.GenerarHorarioRequest;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA de horarios. Genera (Fase 8, Bloque 8.1) y proyecta (Fase 7,
 * Bloque 7A) horarios persistidos; no lista ni edita. Toda la orquestación y la
 * persistencia siguen en {@link GeneradorHorarioService}: el controlador solo
 * enruta y traduce excepciones a códigos HTTP (no hay {@code @ControllerAdvice}
 * global; cada controlador traduce las suyas, patrón de 7A).
 *
 * <p>Traducciones: {@code IllegalArgumentException} de {@link GeneradorHorarioService#proyectar}
 * (id inexistente) → {@code 404}; {@code HorarioInfactibleException} → {@code 422}
 * (problema bien formado sin solución); {@code IllegalArgumentException} de la
 * generación (p. ej. {@code maxSegundos} no positivo) → {@code 400}. El resto
 * (errores de integridad del catálogo) se deja propagar.
 */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final GeneradorHorarioService service;
    private final DiagnosticoService diagnosticoService;

    public HorarioController(GeneradorHorarioService service, DiagnosticoService diagnosticoService) {
        this.service = service;
        this.diagnosticoService = diagnosticoService;
    }

    /**
     * Genera y persiste un horario, y devuelve su proyección plana (misma forma que
     * el GET). Acepta cuerpo ausente o vacío ({@code {}}): todos los parámetros caen
     * a sus valores por defecto (ver {@link GenerarHorarioRequest}).
     */
    @PostMapping
    public HorarioProyeccionDTO generar(@RequestBody(required = false) GenerarHorarioRequest peticion) {
        GenerarHorarioRequest req = peticion != null
                ? peticion
                : new GenerarHorarioRequest(null, null, null, null);
        try {
            HorarioGenerado horario =
                    service.generar(req.maxSegundos(), req.semilla(), req.via(), req.nombre());
            return service.proyectar(horario.getId());
        } catch (HorarioInfactibleException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{id}/proyeccion")
    public HorarioProyeccionDTO proyeccion(@PathVariable("id") Long id) {
        try {
            return service.proyectar(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    /**
     * Diagnostica un horario generado (Fase 8, Bloque 8.3-C): violaciones duras y
     * penalizaciones blandas atribuidas por celda, más los totales blandos. {@code 404}
     * si el id no existe (misma traducción que {@link #proyeccion}).
     */
    @GetMapping("/{id}/diagnostico")
    public DiagnosticoDTO diagnostico(@PathVariable("id") Long id) {
        try {
            return diagnosticoService.diagnosticar(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
}
