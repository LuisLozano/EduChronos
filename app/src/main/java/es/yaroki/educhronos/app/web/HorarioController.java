package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.exportacion.HorarioCsv;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.PrevalidacionFallidaException;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import es.yaroki.educhronos.app.web.dto.FalloGeneracionDTO;
import es.yaroki.educhronos.app.web.dto.GenerarHorarioRequest;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Capa REST FINA de horarios. Genera (Fase 8, Bloque 8.1), proyecta (Fase 7,
 * Bloque 7A) y exporta a CSV (S148, C-exportacion-csv) horarios persistidos; no
 * lista ni edita. La exportación NO es una vía nueva a los datos: aplana la misma
 * proyección con {@link HorarioCsv}. Toda la orquestación y la
 * persistencia siguen en {@link GeneradorHorarioService}: el controlador solo
 * enruta y traduce excepciones a códigos HTTP (no hay {@code @ControllerAdvice}
 * global; cada controlador traduce las suyas, patrón de 7A).
 *
 * <p>Traducciones: {@code IllegalArgumentException} de {@link GeneradorHorarioService#proyectar}
 * (id inexistente) → {@code 404}; {@code HorarioInfactibleException} → lo que diga
 * {@link MapeoFalloSolver} según el veredicto CP-SAT (S118: {@code 422}, {@code 503}
 * o {@code 500}, ya no un 422 único); {@link PrevalidacionFallidaException} →
 * {@code 422} (Bloque 8.4-A): es un hecho detectado ANTES del solve y con el recurso
 * culpable nombrado, y NO pasa por el mapeo —no hay veredicto que mapear—;
 * {@code IllegalArgumentException} de la generación (p. ej. {@code maxSegundos} no
 * positivo) → {@code 400}. El resto (errores de integridad del catálogo) se deja
 * propagar.
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
    public ResponseEntity<Object> generar(@RequestBody(required = false) GenerarHorarioRequest peticion) {
        GenerarHorarioRequest req = peticion != null
                ? peticion
                : new GenerarHorarioRequest(null, null, null, null);
        try {
            HorarioGenerado horario =
                    service.generar(req.maxSegundos(), req.semilla(), req.via(), req.nombre());
            return ResponseEntity.ok(service.proyectar(horario.getId()));
        } catch (PrevalidacionFallidaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        } catch (HorarioInfactibleException e) {
            return respuestaDeFallo(e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Traduce un fallo del solver a su respuesta, con el cuerpo que verá el navegador.
     *
     * <p>NO lanza {@code ResponseStatusException} como las otras ramas: esa vía deja
     * el cuerpo en manos del mecanismo de error de Spring, que aquí está medido como
     * mudo (D-F8.6-ii-a, ver {@link FalloGeneracionDTO}). Un {@code ResponseEntity}
     * es lo único que garantiza que la causa llegue por la red.
     */
    private ResponseEntity<Object> respuestaDeFallo(HorarioInfactibleException e) {
        MapeoFalloSolver.RespuestaFallo fallo = MapeoFalloSolver.mapear(e.estado());
        FalloGeneracionDTO cuerpo = new FalloGeneracionDTO(
                fallo.causa(), e.getMessage(), e.estado(), e.segundos());

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(fallo.status());
        if (fallo.status() == HttpStatus.SERVICE_UNAVAILABLE) {
            // Retry-After: 0 — "reintenta YA", y el 0 es deliberado, no un descuido.
            // No hay cola que drenar ni servicio que se recupere con el tiempo: lo que
            // faltó fue presupuesto. Y cada corrida es una tirada INDEPENDIENTE
            // (D-generacion-no-reproducible), así que esperar no mejora las
            // probabilidades del siguiente intento ni un ápice. Cualquier número mayor
            // que 0 le mentiría al cliente sobre por qué debe esperar.
            respuesta = respuesta.header(HttpHeaders.RETRY_AFTER, "0");
        }
        return respuesta.body(cuerpo);
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

    /**
     * Descarga la MISMA proyección del GET de arriba, aplanada a CSV (S148,
     * C-exportacion-csv). No hay servicio nuevo ni consulta nueva: el controlador
     * pide la proyección y se la pasa a {@link HorarioCsv}, que es una función pura.
     *
     * <p>El {@code try} envuelve SÓLO la llamada al servicio, y no la escritura del
     * CSV, a propósito: el 404 es del id que no existe. La guarda de
     * {@link HorarioCsv} lanza {@code IllegalStateException} —no {@code
     * IllegalArgumentException}—, así que tampoco podría confundirse con un 404 si
     * cayera dentro; pero dejar la escritura fuera hace que la frontera no dependa de
     * qué tipo lance el serializador.
     */
    @GetMapping("/{id}/csv")
    public ResponseEntity<byte[]> csv(@PathVariable("id") Long id) {
        HorarioProyeccionDTO proyeccion;
        try {
            proyeccion = service.proyectar(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }

        ContentDisposition adjunto = ContentDisposition.attachment()
                .filename("horario-" + id + ".csv")
                .build();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, adjunto.toString())
                .body(HorarioCsv.escribir(proyeccion));
    }
}
