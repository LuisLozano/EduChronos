package es.yaroki.educhronos.app.config;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;

/**
 * Cómo se abre una base de Educhronos: el pool y el esquema, con LOS MISMOS ajustes con los
 * que la abre Spring Boot al arrancar (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>Existe para que no haya dos respuestas a la misma pregunta.</b> Cambiar de curso
 * construye un pool y prepara el esquema sobre él, que es exactamente lo que hace el
 * arranque; si eso se escribiera a mano se tendría un segundo juego de ajustes —tamaño de
 * piscina, tiempos de espera, reglas de esquema— que envejecería en cuanto alguien tocara el
 * {@code application.properties} o las migraciones, y nadie se enteraría hasta que una base
 * abierta en caliente se comportara distinto de la misma base abierta al arrancar. Aquí se
 * delega en lo mismo que usa el arranque: {@link DataSourceProperties} para el pool y el
 * {@link PreparadorEsquema} del contexto para el esquema.
 *
 * <p><b>Por qué el esquema también al abrir.</b> Es la condición 6 aplicada al cambio de
 * curso: una base de antes de S159 no tiene tabla {@code curso}, y abrirla sin prepararla
 * dejaría a {@code EstadoCurso} consultando una tabla que no existe. Desde S191 lo que se le
 * pasa es el preparador, el mismo bean que usa {@link InicializadorEsquema} al arrancar: una
 * base sin número recibe {@code esquema/001.sql}, una de un número anterior sus migraciones,
 * una al día no cambia, y una de una versión más nueva se rechaza sin tocarla.
 */
public class FabricaDeBases {

    private final DataSourceProperties propiedades;

    private final PreparadorEsquema preparador;

    public FabricaDeBases(DataSourceProperties propiedades, PreparadorEsquema preparador) {
        this.propiedades = propiedades;
        this.preparador = preparador;
    }

    /**
     * Un pool nuevo contra {@code fichero}, con los ajustes del {@code application.properties}
     * y sólo la URL cambiada.
     *
     * <p>Se construye por {@code initializeDataSourceBuilder()} y no con un
     * {@code new HikariDataSource()} para heredar el driver y cualquier
     * {@code spring.datasource.*} que llegue a existir. El tipo se fija a
     * {@link HikariDataSource} porque el cambio de curso necesita preguntarle por sus
     * conexiones activas, y eso no está en la interfaz {@link DataSource}.
     *
     * @param fichero base a abrir
     */
    public HikariDataSource poolPara(Path fichero) {
        return propiedades
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .url(BaseConmutable.PREFIJO_URL + fichero.toAbsolutePath())
                .build();
    }

    /**
     * Deja {@code base} en el esquema de esta versión con el mismo {@link PreparadorEsquema}
     * que el arranque.
     *
     * @param base pool ya construido sobre el fichero a preparar
     * @throws EsquemaPosteriorException si la base es de una versión más nueva; queda intacta
     */
    public void prepararEsquema(DataSource base) {
        preparador.preparar(base);
    }
}
