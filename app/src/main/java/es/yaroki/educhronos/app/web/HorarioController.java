package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.exportacion.HorarioCsv;
import es.yaroki.educhronos.app.exportacion.HorarioPdf;
import es.yaroki.educhronos.app.exportacion.VistaPdf;
import es.yaroki.educhronos.app.curso.RechazoCursoException;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.service.DiagnosticoService;
import es.yaroki.educhronos.app.service.ExportacionHorarioService;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import es.yaroki.educhronos.app.service.GuardiasSinRepartoException;
import es.yaroki.educhronos.app.service.PrevalidacionFallidaException;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import es.yaroki.educhronos.app.web.dto.FalloGeneracionDTO;
import es.yaroki.educhronos.app.web.dto.GenerarHorarioRequest;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.HorarioVigenteDTO;
import es.yaroki.educhronos.app.web.dto.RechazoCursoDTO;
import es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
 * culpable nombrado, y NO pasa por el mapeo —no hay veredicto que mapear—; desde S166
 * sale con cuerpo {@link FalloGeneracionDTO} y causa {@code PREVALIDACION_FALLIDA};
 * {@code IllegalArgumentException} de la generación (p. ej. {@code maxSegundos} no
 * positivo) → {@code 400}. El resto (errores de integridad del catálogo) se deja
 * propagar.
 *
 * <p><b>{@link RechazoCursoException} → su propio status, con cuerpo {@link RechazoCursoDTO}</b>
 * (S160). Desde C-selector-curso, {@code generar} puede negarse porque se está abriendo otro
 * curso: un 409 {@code CURSO_CAMBIANDO}. El manejador está AQUÍ y no en un
 * {@code @ControllerAdvice} global, siguiendo la costumbre del proyecto —cada controlador
 * traduce las suyas—, y es una copia literal del de {@code CursoController} porque las dos
 * respuestas tienen que ser indistinguibles para el cliente: mismo cuerpo, misma causa.
 */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    /** Causa del 422 de la pre-validación: contrato con la vista, no texto para el usuario. */
    static final String CAUSA_PREVALIDACION_FALLIDA = "PREVALIDACION_FALLIDA";

    /** Causa del 422 del reparto de guardias (S213): hubo solve, pero no se llega al mínimo por tramo. */
    static final String CAUSA_GUARDIAS_SIN_REPARTO = "GUARDIAS_SIN_REPARTO";

    private final GeneradorHorarioService service;
    private final DiagnosticoService diagnosticoService;
    private final ExportacionHorarioService exportacionService;

    public HorarioController(GeneradorHorarioService service,
                             DiagnosticoService diagnosticoService,
                             ExportacionHorarioService exportacionService) {
        this.service = service;
        this.diagnosticoService = diagnosticoService;
        this.exportacionService = exportacionService;
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
            return respuestaDeFallo(e);
        } catch (HorarioInfactibleException e) {
            return respuestaDeFallo(e);
        } catch (GuardiasSinRepartoException e) {
            return respuestaDeFallo(e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Un rechazo previsto del curso sale con su status y su causa en el cuerpo, igual que en
     * {@code CursoController}. No pasa por {@code ResponseStatusException} porque el cliente
     * necesita la {@code causa} como campo propio del cuerpo, que el JSON de error de Spring no
     * tiene; el texto sí llegaría en {@code message} desde S167 (D-F8.6-ii-a saldada).
     */
    @ExceptionHandler(RechazoCursoException.class)
    ResponseEntity<RechazoCursoDTO> rechazoDeCurso(RechazoCursoException e) {
        return ResponseEntity.status(e.status())
                .body(new RechazoCursoDTO(e.causa(), e.getMessage()));
    }

    /**
     * Traduce un fallo del solver a su respuesta, con el cuerpo que verá el navegador.
     *
     * <p>Ni esta rama ni la de la pre-validación (la sobrecarga de abajo) lanzan
     * {@code ResponseStatusException}: aquí el cuerpo lleva campos propios ({@code causa},
     * {@code estado}, {@code segundos}, ver {@link FalloGeneracionDTO}) que el JSON de error de
     * Spring no tiene, y un {@code ResponseEntity} es lo que los hace viajar. El motivo de las
     * {@code ResponseStatusException} del proyecto —el 400 de esta misma generación y los 404 de
     * este controlador— SÍ llega al cliente, en la clave {@code message}, desde que S167 saldó
     * D-F8.6-ii-a con {@code spring.web.error.include-message=always} (lo mide
     * {@code MensajeDeErrorHttpTest}).
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

    /**
     * El 422 de la pre-validación, con su motivo en el cuerpo (S166, condición 6 de
     * {@code O-disponibilidad}). Hasta S166 salía por {@code ResponseStatusException} y
     * llegaba al navegador sin motivo, así que la vista decía «no tiene solución» ante un
     * pin que contradice una DURA, que sí la tiene en cuanto se quita el pin.
     *
     * <p>Mismo cuerpo que el fallo del solver, con causa propia —NO la del solver: aquí no
     * hubo solve, y {@code CONFIGURACION_INCOMPLETA} ya significa «falta la jornada»—.
     * {@code mensaje} lleva las descripciones de TODOS los ERROR, que nombran el recurso
     * culpable; {@code estado} y {@code segundos} van nulos porque no hubo solve.
     */
    private ResponseEntity<Object> respuestaDeFallo(PrevalidacionFallidaException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new FalloGeneracionDTO(CAUSA_PREVALIDACION_FALLIDA, e.getMessage(), null, null));
    }

    /**
     * El 422 del reparto de guardias (S213, C-reparto-guardias), por el mismo camino que el de la
     * pre-validación: causa propia, {@code mensaje} con el texto para el usuario, que nombra los
     * tramos que no llegan al mínimo, y {@code estado} y {@code segundos} nulos. Hubo solve, pero no
     * hay horario: no se guardó nada, y el veredicto del solver no le dice nada al usuario.
     */
    private ResponseEntity<Object> respuestaDeFallo(GuardiasSinRepartoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new FalloGeneracionDTO(CAUSA_GUARDIAS_SIN_REPARTO, e.getMessage(), null, null));
    }

    /**
     * Horario vigente del curso abierto: 200 con su id, o 204 si el curso no tiene
     * ninguno (un curso recién duplicado). Que no haya horario no es un error, por eso
     * no es 404. Es un GET, así que la guarda de solo lectura lo deja pasar en un curso
     * archivado (O-curso, condición 4). Salda D-horario-id-a-fuego en el backend: la
     * barra deja de suponer el id 1 (S161).
     */
    @GetMapping("/vigente")
    public ResponseEntity<HorarioVigenteDTO> vigente() {
        return service.idVigente()
                .map(id -> ResponseEntity.ok(new HorarioVigenteDTO(id)))
                .orElseGet(() -> ResponseEntity.noContent().build());
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

    /**
     * Descarga el horario como PDF, una página A4 vertical por recurso de la vista
     * pedida (S149, C-exportacion-pdf-grupo; S150, C-exportacion-pdf-profesor-aula). La
     * composición de las fuentes —proyección, jornada, nombres de profesor y el catálogo
     * que ordena las páginas— vive en {@link ExportacionHorarioService}; aquí solo se
     * enruta y se traducen los dos errores de frontera.
     *
     * <p><b>El parametro {@code vista} se resuelve ANTES de pedir nada.</b> Un valor que
     * no nombre ninguna {@link VistaPdf} es un 400 y no un 404: el horario existe, lo que
     * no existe es esa vista. Rechazarlo antes de proyectar evita además hacer el trabajo
     * caro para tirarlo después. El mensaje lista las vistas DISPONIBLES derivándolas de
     * {@code VistaPdf.values()}, y no de una enumeración escrita a mano que envejecería
     * en silencio en cuanto naciera una vista nueva.
     *
     * <p>El 404 sigue el patrón de {@link #csv(Long)}: el {@code try} envuelve solo la
     * llamada que puede toparse con un id inexistente. {@code pdf} tiene garantizado por
     * contrato que su única {@code IllegalArgumentException} es la de {@code proyectar}
     * —{@code HorarioPdf} lanza {@code IllegalStateException} para sus guardas—, así que
     * envolverla no ensancha la traducción.
     */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(
            @PathVariable("id") Long id,
            @RequestParam(name = "vista", defaultValue = "grupo") String vista) {

        VistaPdf v = VistaPdf.desdeParametro(vista).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "vista no soportada: '" + vista + "'. Las disponibles son: "
                                + Arrays.stream(VistaPdf.values())
                                        .map(VistaPdf::parametro)
                                        .collect(Collectors.joining(", "))));

        byte[] pdf;
        try {
            pdf = exportacionService.pdf(id, v);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }

        ContentDisposition adjunto = ContentDisposition.attachment()
                .filename("horario-" + id + "-" + v.parametro() + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, adjunto.toString())
                .body(pdf);
    }
}
