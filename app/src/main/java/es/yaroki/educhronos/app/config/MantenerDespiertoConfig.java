package es.yaroki.educhronos.app.config;

import com.sun.jna.Native;
import es.yaroki.educhronos.app.escritorio.MantenerDespierto;
import es.yaroki.educhronos.app.escritorio.MantenerDespiertoNulo;
import es.yaroki.educhronos.app.escritorio.MantenerDespiertoWindows;
import es.yaroki.educhronos.app.escritorio.MantenerDespiertoWindows.Kernel32Energia;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El único {@link MantenerDespierto} de la aplicación (S238, C-sin-suspension): el de Windows en
 * Windows y el nulo en cualquier otro sistema. Lo recibe {@code GeneradorHorarioService}.
 *
 * <p>Los {@code @DataJpaTest} no escanean esta clase: el servicio recibe un {@code Optional}
 * vacío y se queda con el nulo, así que ningún slice necesita importarla.
 */
@Configuration
public class MantenerDespiertoConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MantenerDespiertoConfig.class);

    @Bean
    MantenerDespierto mantenerDespierto() {
        return elegir(System.getProperty("os.name"), () -> Native.load("kernel32", Kernel32Energia.class));
    }

    /**
     * Windows o nulo según {@code os.name}, con la misma comprobación que {@code CarpetaDatos}.
     * El cargador solo se llama en Windows; si falla —JNA que no carga, {@code kernel32} que no
     * se encuentra—, se avisa y queda el nulo: sin la petición se genera igual.
     *
     * @param osName valor de {@code os.name}; {@code null} se trata como no-Windows
     * @param cargador lo que carga {@code kernel32}; entra por parámetro para poder probarlo
     */
    public static MantenerDespierto elegir(String osName, Supplier<Kernel32Energia> cargador) {
        if (osName != null && osName.toLowerCase(Locale.ROOT).contains("windows")) {
            try {
                return new MantenerDespiertoWindows(cargador.get());
            } catch (LinkageError | RuntimeException e) {
                LOG.warn("No se ha podido cargar kernel32 con JNA: el equipo puede suspenderse mientras se genera", e);
            }
        }
        return new MantenerDespiertoNulo();
    }
}
