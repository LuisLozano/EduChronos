package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Configuracion;
import es.yaroki.educhronos.app.catalog.ConfiguracionRepository;
import es.yaroki.educhronos.app.web.dto.ConfiguracionGuardiasDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura y reemplazo del mínimo de profesores de guardia por tramo (S212, C-dato-guardias) y,
 * desde S219 (C-dato-guardias-especiales), de los mínimos por recreo y de biblioteca por recreo. La
 * lectura con su valor por defecto es la de {@link ConfiguracionGuardias}, la misma que usa la
 * generación; aquí solo se valida y se guarda.
 *
 * <p>Validación: los tres mínimos son obligatorios y ≥ 0, y se validan los tres antes de guardar
 * ninguno: un 400 no deja nada escrito. Un fallo lanza {@link IllegalArgumentException}, que el
 * controlador traduce a 400, como en {@code JornadaService}. Que el de biblioteca supere al del
 * recreo no se rechaza aquí: lo juzga la prevalidación (S219, condición 4).
 */
@Service
public class ConfiguracionGuardiasService {

    private final ConfiguracionRepository repositorio;

    public ConfiguracionGuardiasService(ConfiguracionRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Los mínimos vigentes: los guardados o, sin fila, {@link ConfiguracionGuardias#MINIMO_POR_DEFECTO}
     * y {@link ConfiguracionGuardias#MINIMO_RECREO_POR_DEFECTO}.
     */
    @Transactional(readOnly = true)
    public ConfiguracionGuardiasDTO obtener() {
        return new ConfiguracionGuardiasDTO(
                ConfiguracionGuardias.minimoPorTramo(repositorio),
                ConfiguracionGuardias.minimoPorRecreo(repositorio),
                ConfiguracionGuardias.minimoBibliotecaPorRecreo(repositorio));
    }

    /**
     * Guarda los tres mínimos. {@link IllegalArgumentException} (→ 400) si falta alguno o es
     * negativo; se validan los tres antes de guardar ninguno.
     */
    @Transactional
    public ConfiguracionGuardiasDTO reemplazar(ConfiguracionGuardiasDTO peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("minimoPorTramo es obligatorio");
        }
        int porTramo = validar(peticion.minimoPorTramo(), "minimoPorTramo");
        int porRecreo = validar(peticion.minimoPorRecreo(), "minimoPorRecreo");
        int biblioteca = validar(peticion.minimoBibliotecaPorRecreo(), "minimoBibliotecaPorRecreo");
        repositorio.save(new Configuracion(ConfiguracionGuardias.CLAVE, Integer.toString(porTramo)));
        repositorio.save(new Configuracion(
                ConfiguracionGuardias.CLAVE_MINIMO_RECREO, Integer.toString(porRecreo)));
        repositorio.save(new Configuracion(
                ConfiguracionGuardias.CLAVE_MINIMO_BIBLIOTECA, Integer.toString(biblioteca)));
        return new ConfiguracionGuardiasDTO(porTramo, porRecreo, biblioteca);
    }

    /** Un mínimo obligatorio y ≥ 0; el mensaje del 400 nombra el campo. */
    private static int validar(Integer valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo: " + valor);
        }
        return valor;
    }
}
