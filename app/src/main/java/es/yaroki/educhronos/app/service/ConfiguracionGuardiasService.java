package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Configuracion;
import es.yaroki.educhronos.app.catalog.ConfiguracionRepository;
import es.yaroki.educhronos.app.web.dto.ConfiguracionGuardiasDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura y reemplazo del mínimo de profesores de guardia por tramo (S212, C-dato-guardias). La
 * lectura con su valor por defecto es la de {@link ConfiguracionGuardias}, la misma que usa la
 * generación; aquí solo se valida y se guarda.
 *
 * <p>Validación: el mínimo es obligatorio y ≥ 0. Un fallo lanza {@link IllegalArgumentException},
 * que el controlador traduce a 400, como en {@code JornadaService}.
 */
@Service
public class ConfiguracionGuardiasService {

    private final ConfiguracionRepository repositorio;

    public ConfiguracionGuardiasService(ConfiguracionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** El mínimo vigente: el guardado o, sin fila, {@link ConfiguracionGuardias#MINIMO_POR_DEFECTO}. */
    @Transactional(readOnly = true)
    public ConfiguracionGuardiasDTO obtener() {
        return new ConfiguracionGuardiasDTO(ConfiguracionGuardias.minimoPorTramo(repositorio));
    }

    /** Guarda el mínimo. {@link IllegalArgumentException} (→ 400) si falta o es negativo. */
    @Transactional
    public ConfiguracionGuardiasDTO reemplazar(ConfiguracionGuardiasDTO peticion) {
        if (peticion == null || peticion.minimoPorTramo() == null) {
            throw new IllegalArgumentException("minimoPorTramo es obligatorio");
        }
        int minimo = peticion.minimoPorTramo();
        if (minimo < 0) {
            throw new IllegalArgumentException("minimoPorTramo no puede ser negativo: " + minimo);
        }
        repositorio.save(new Configuracion(ConfiguracionGuardias.CLAVE, Integer.toString(minimo)));
        return new ConfiguracionGuardiasDTO(minimo);
    }
}
