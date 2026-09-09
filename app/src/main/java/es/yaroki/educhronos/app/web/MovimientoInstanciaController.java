package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.MovimientoInstanciaService;
import es.yaroki.educhronos.app.service.MovimientoRechazadoException;
import es.yaroki.educhronos.app.web.dto.CeldaRefDTO;
import es.yaroki.educhronos.app.web.dto.FalloMovimientoDTO;
import es.yaroki.educhronos.app.web.dto.IntercambiarInstanciasRequest;
import es.yaroki.educhronos.app.web.dto.MoverInstanciaRequest;
import es.yaroki.educhronos.app.web.dto.ViolacionDTO;
import es.yaroki.educhronos.solver.cpsat.Violacion;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Capa REST FINA del movimiento de instancias (S143, C-mover-sesion-backend):
 * {@code PUT /api/horarios/{horarioId}/instancias}. Toda la lógica y el veredicto
 * viven en {@link MovimientoInstanciaService}; aquí solo se enruta y se traduce la
 * causa del rechazo a su código HTTP.
 *
 * <p><b>Controlador propio y no un método más en {@code HorarioController}</b>, pese a
 * compartir el prefijo {@code /api/horarios} (Spring lo admite: las rutas completas no
 * colisionan). Añadirlo allí obligaba a ampliar su constructor, y eso rompe la
 * compilación de las CINCO clases de test que lo construyen a mano — un coste ajeno a
 * este Cambio, pagado por specs que no tienen nada que ver con mover una instancia.
 * Un controlador por servicio es además el molde ya vigente ({@code BloqueoController},
 * {@code ReplicacionController}).
 *
 * <p><b>NO usa {@code ResponseStatusException} en NINGÚN no-2xx</b>, a diferencia del
 * resto de controladores del proyecto. Esa vía deja el cuerpo en manos del mecanismo de
 * error de Spring, medido aquí como mudo (D-F8.6-ii-a, ver {@code FalloGeneracionDTO}),
 * y este endpoint tiene el caso donde más se nota: el rechazo por regla dura no es un
 * texto, es una LISTA de violaciones que la rejilla tiene que pintar. Solo un
 * {@code ResponseEntity} garantiza que llegue por la red.
 */
@RestController
@RequestMapping("/api/horarios")
public class MovimientoInstanciaController {

    private final MovimientoInstanciaService service;

    public MovimientoInstanciaController(MovimientoInstanciaService service) {
        this.service = service;
    }

    /**
     * Recoloca una instancia en otro tramo. {@code 200} con las filas resultantes
     * —misma forma que las {@code sesiones} de {@code GET /{id}/proyeccion}— si el
     * movimiento es legal o si la instancia ya estaba ahí (idempotente); en otro caso,
     * el código que diga la causa, con {@link FalloMovimientoDTO} de cuerpo.
     */
    @PutMapping("/{horarioId}/instancias")
    public ResponseEntity<Object> mover(
            @PathVariable("horarioId") Long horarioId,
            @RequestBody MoverInstanciaRequest peticion) {
        try {
            return ResponseEntity.ok(service.mover(horarioId, peticion));
        } catch (MovimientoRechazadoException e) {
            return respuestaDeRechazo(e);
        }
    }

    /**
     * INTERCAMBIA los tramos de dos instancias (S144). {@code 200} con las filas de
     * ambas —dos listas, una por lado— si el intercambio es legal o si ambas ya estaban
     * en el mismo tramo; en otro caso, el código que diga la causa.
     *
     * <p>Va en ESTE controlador y no en uno nuevo: usa el mismo
     * {@link MovimientoInstanciaService} y no amplía el constructor, así que no arrastra
     * el coste que justificó separar de {@code HorarioController} en S143.
     */
    @PutMapping("/{horarioId}/instancias/intercambio")
    public ResponseEntity<Object> intercambiar(
            @PathVariable("horarioId") Long horarioId,
            @RequestBody IntercambiarInstanciasRequest peticion) {
        try {
            return ResponseEntity.ok(service.intercambiar(horarioId, peticion));
        } catch (MovimientoRechazadoException e) {
            return respuestaDeRechazo(e);
        }
    }

    /** Traduce la causa del rechazo a su código HTTP y a su cuerpo. */
    private ResponseEntity<Object> respuestaDeRechazo(MovimientoRechazadoException e) {
        HttpStatus status = switch (e.causa()) {
            case TRAMO_INEXISTENTE, INSTANCIAS_IGUALES -> HttpStatus.BAD_REQUEST;
            case HORARIO_INEXISTENTE, INSTANCIA_INEXISTENTE -> HttpStatus.NOT_FOUND;
            case VIOLA_REGLA_DURA, INSTANCIA_PINADA -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(new FalloMovimientoDTO(
                e.causa().name(), e.getMessage(), aViolacionDTO(e.violaciones())));
    }

    /** Aplana las violaciones de dominio a los DTO que ya usa el diagnóstico (8.3-C). */
    private static List<ViolacionDTO> aViolacionDTO(List<Violacion> violaciones) {
        return violaciones.stream()
                .map(v -> new ViolacionDTO(
                        v.regla().name(),
                        v.recursoCodigo(),
                        v.tramoCodigo(),
                        v.celdas().stream()
                                .map(c -> new CeldaRefDTO(
                                        c.actividadCodigo(), c.indice(), c.plazaCodigo()))
                                .toList(),
                        v.descripcion()))
                .toList();
    }
}
