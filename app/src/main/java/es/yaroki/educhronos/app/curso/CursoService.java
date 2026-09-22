package es.yaroki.educhronos.app.curso;

import es.yaroki.educhronos.app.config.CarpetaDatos;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Duplicar el curso abierto, desde dentro de la aplicación (O-curso, S159).
 *
 * <p><b>De dónde sale la ruta de la base.</b> De {@code spring.datasource.url}, que es la
 * única fuente de verdad: la calcula el {@code RutaBaseDatosEnvironmentPostProcessor} o la
 * trae la línea de órdenes, y en los dos casos es la base que el pool tiene abierta. No se
 * recalcula la carpeta de datos aquí, porque entonces habría dos respuestas posibles a la
 * misma pregunta.
 *
 * <p><b>Por qué el puntero puede no existir.</b> Con {@code --spring.datasource.url} el
 * post-procesador no hace nada y no publica {@code educhronos.datos.carpeta}: no hay carpeta
 * de datos que gobernar y NO se escribe puntero (condición 7). Duplicar sigue funcionando
 * —el fichero nuevo nace junto al origen—, pero quién se abre la próxima vez lo sigue
 * decidiendo quien puso la URL.
 *
 * <p><b>{@code synchronized}.</b> Duplicar mueve ficheros y reescribe la identidad de dos
 * bases; dos a la vez podrían pisarse el temporal. Es una operación de una vez al año: el
 * candado más simple que funciona.
 */
@Service
public class CursoService {

    /** Prefijo que hay que quitar de la URL para quedarse con el fichero. */
    static final String PREFIJO_URL = "jdbc:sqlite:";

    private final EstadoCurso estado;

    /**
     * Se construye aquí y NO se inyecta: {@link DuplicadorCurso} no tiene una sola anotación
     * de Spring a propósito (no puede correr bajo {@code @Transactional}), y hacerlo un bean
     * lo dejaría a un {@code @Transactional} de distancia del error que evita. Sin estado,
     * así que una instancia basta.
     */
    private final DuplicadorCurso duplicador = new DuplicadorCurso();

    /** URL de la base abierta, tal como la ve el pool. */
    private final String url;

    /**
     * Carpeta de datos que gobierna el post-procesador, vacía cuando no la publicó nadie.
     *
     * <p>Se lee con un {@code @Value} suelto, como {@code educhronos.solver.max-segundos}.
     * El {@code application.properties} deja escrito que a la SEGUNDA clave
     * {@code educhronos.*} se migra a un record {@code @ConfigurationProperties}; esta es la
     * segunda y la migración no se hace aquí, porque tocaría el servicio del solver, que no
     * tiene nada que ver con O-curso. Queda como deuda a dar de alta en el cierre de S159.
     */
    private final String carpetaDeDatos;

    public CursoService(
            EstadoCurso estado,
            @Value("${spring.datasource.url}") String url,
            @Value("${educhronos.datos.carpeta:}") String carpetaDeDatos) {
        this.estado = estado;
        this.url = url;
        this.carpetaDeDatos = carpetaDeDatos;
    }

    /**
     * Crea el curso siguiente a partir del abierto y archiva el abierto.
     *
     * @param nombreNuevo nombre del curso a crear
     * @param nombreActual nombre del curso actual, necesario sólo si la base no lo trae
     * @return la ruta del fichero creado
     * @throws RechazoCursoException si la operación se rechaza por una razón prevista
     */
    public synchronized Path duplicar(String nombreNuevo, String nombreActual) {
        Path origen = baseAbierta();
        String nombreArchivado = estado.nombre() != null ? estado.nombre() : nombreActual;
        Path destino = duplicador.duplicar(origen, nombreActual, nombreNuevo, puntero());
        estado.marcarArchivado(nombreArchivado);
        return destino;
    }

    /**
     * El fichero de la base abierta. Quita el prefijo del driver y los parámetros de la URL
     * ({@code ?foreign_keys=on} y compañía), que no son parte de la ruta.
     */
    Path baseAbierta() {
        String ruta = url.startsWith(PREFIJO_URL) ? url.substring(PREFIJO_URL.length()) : url;
        int parametros = ruta.indexOf('?');
        if (parametros >= 0) {
            ruta = ruta.substring(0, parametros);
        }
        return Path.of(ruta).toAbsolutePath();
    }

    /** El fichero de puntero, o {@code null} si nadie gobierna la carpeta de datos. */
    Path puntero() {
        if (carpetaDeDatos == null || carpetaDeDatos.isBlank()) {
            return null;
        }
        return Path.of(carpetaDeDatos).resolve(CarpetaDatos.NOMBRE_PUNTERO);
    }
}
