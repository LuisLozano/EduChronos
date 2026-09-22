package es.yaroki.educhronos.app.config;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.ApplicationDataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.core.io.ResourceLoader;

/**
 * Cómo se abre una base de Educhronos: el pool y el esquema, con LOS MISMOS ajustes con los
 * que la abre Spring Boot al arrancar (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>Existe para que no haya dos respuestas a la misma pregunta.</b> Cambiar de curso
 * construye un pool y corre {@code schema.sql} sobre él, que es exactamente lo que hace el
 * arranque; si eso se escribiera a mano se tendría un segundo juego de ajustes —tamaño de
 * piscina, tiempos de espera, modo de inicialización, codificación de los scripts— que
 * envejecería en cuanto alguien tocara el {@code application.properties} y nadie se
 * enteraría hasta que una base abierta en caliente se comportara distinto de la misma base
 * abierta al arrancar. Aquí se delega en las dos clases de Boot que gobiernan esas dos cosas:
 * {@link DataSourceProperties} y {@link SqlInitializationProperties}.
 *
 * <p><b>Por qué {@code schema.sql} también al abrir.</b> Es la condición 6 aplicada al
 * cambio de curso: una base de antes de S159 no tiene tabla {@code curso}, y abrirla sin
 * pasar el esquema dejaría a {@code EstadoCurso} consultando una tabla que no existe. El
 * script es idempotente desde S109 —veintidós {@code create table if not exists} y cero
 * {@code drop}—, así que correrlo sobre una base al día no cambia nada.
 */
public class FabricaDeBases {

    private final DataSourceProperties propiedades;

    private final SqlInitializationProperties inicializacion;

    private final ResourceLoader cargador;

    public FabricaDeBases(
            DataSourceProperties propiedades,
            SqlInitializationProperties inicializacion,
            ResourceLoader cargador) {
        this.propiedades = propiedades;
        this.inicializacion = inicializacion;
        this.cargador = cargador;
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
     * Corre sobre {@code base} la inicialización de esquema del arranque, con el mismo
     * {@link ApplicationDataSourceScriptDatabaseInitializer} que instancia Boot y las mismas
     * propiedades {@code spring.sql.init.*}.
     *
     * <p>El {@link ResourceLoader} se le pasa explícitamente: sin él, el inicializador usa
     * uno por defecto, y lo que aquí se quiere es que {@code classpath:schema.sql} se
     * resuelva como se resuelve en el arranque, también dentro del jar.
     *
     * @param base pool ya construido sobre el fichero a preparar
     */
    public void prepararEsquema(DataSource base) {
        ApplicationDataSourceScriptDatabaseInitializer inicializador =
                new ApplicationDataSourceScriptDatabaseInitializer(base, inicializacion);
        inicializador.setResourceLoader(cargador);
        inicializador.initializeDatabase();
    }
}
