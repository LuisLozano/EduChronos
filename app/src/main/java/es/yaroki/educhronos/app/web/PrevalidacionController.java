package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.service.PrevalidacionService;
import es.yaroki.educhronos.app.web.dto.AvisoPrevalidacionDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Capa REST FINA de la pre-validación del catálogo (Fase 8, Bloque 8.4-A, deuda D18).
 * Solo enruta y adapta al DTO; las tres reglas viven en {@link PrevalidacionService}
 * (sin {@code @ControllerAdvice} global; cada controlador traduce las suyas).
 *
 * <p><b>Este endpoint NO aborta nunca.</b> Siempre responde {@code 200} con la lista
 * COMPLETA de hallazgos —errores y avisos—, incluida la lista vacía si el catálogo está
 * sano. Es una CONSULTA de diagnóstico: quien convierte un ERROR en un 422 es
 * {@code POST /api/horarios}, no este recurso. De ahí que no haya ningún {@code catch}
 * de {@link es.yaroki.educhronos.app.service.PrevalidacionFallidaException}: esta vía
 * no la lanza.
 *
 * <p>Los errores de integridad del catálogo (huérfanos, códigos duplicados, que detecta
 * {@code CatalogoMapper} al cargar) se dejan propagar, misma postura que
 * {@link HorarioController}.
 */
@RestController
@RequestMapping("/api/prevalidacion")
public class PrevalidacionController {

    private final PrevalidacionService service;

    public PrevalidacionController(PrevalidacionService service) {
        this.service = service;
    }

    /**
     * Pre-valida el catálogo actual y devuelve todos los hallazgos, en el orden estable
     * que produce el servicio (profesores, actividades, grupos).
     */
    @GetMapping
    public List<AvisoPrevalidacionDTO> prevalidar() {
        return service.prevalidar().stream().map(AvisoPrevalidacionDTO::de).toList();
    }
}
