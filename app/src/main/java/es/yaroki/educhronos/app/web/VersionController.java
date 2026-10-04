package es.yaroki.educhronos.app.web;

import es.yaroki.educhronos.app.config.VersionEnvironmentPostProcessor;
import es.yaroki.educhronos.app.web.dto.VersionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La versión de la aplicación (C-version-y-rastro, condición 2, S192). Primer endpoint de
 * información general: no es de catálogo ni de horario.
 *
 * <p>La lee de {@code educhronos.version}, que publica {@link VersionEnvironmentPostProcessor}
 * desde el {@code build-info} del jar. El valor por defecto sólo cubre un contexto donde ese
 * post-procesador no haya corrido.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final String version;

    public VersionController(
            @Value("${" + VersionEnvironmentPostProcessor.CLAVE_VERSION + ":"
                    + VersionEnvironmentPostProcessor.DESCONOCIDA + "}") String version) {
        this.version = version;
    }

    @GetMapping
    public VersionDTO version() {
        return new VersionDTO(version);
    }
}
