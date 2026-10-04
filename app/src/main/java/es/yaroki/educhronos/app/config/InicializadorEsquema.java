package es.yaroki.educhronos.app.config;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * El inicializador de esquema del arranque: el de Boot, sustituido por
 * {@link PreparadorEsquema} (O-base-tecnica, S191, C-esquema-versionado).
 *
 * <p><b>Por qué es una subclase de una clase de Boot y no un bean cualquiera.</b> Por los dos
 * enganches que Boot ya tiene, medidos sobre el bytecode de 4.1.0 en el M2 de S191:
 *
 * <ul>
 *   <li>{@code DataSourceInitializationAutoConfiguration} se retira con
 *       {@code @ConditionalOnMissingBean(ApplicationScriptDatabaseInitializer.class)}.
 *       Implementar esa interfaz es lo que apaga el inicializador de Boot; sin ella habría
 *       dos, y {@code schema.sql} se pasaría además a ciegas sobre cualquier base.
 *   <li>JPA espera a todo bean de tipo {@link DataSourceScriptDatabaseInitializer}: lo
 *       detecta {@code DataSourceScriptDatabaseInitializerDetector} y
 *       {@code JpaDependsOnDatabaseInitializationDetector} hace depender de él al
 *       {@code EntityManagerFactory}. Heredar de esa clase es lo que garantiza que el esquema
 *       esté listo antes de que Hibernate toque la base, sin un {@code @DependsOn} a mano.
 * </ul>
 *
 * <p><b>Por qué se sobrescriben los dos métodos.</b> Boot ejecuta la inicialización desde
 * {@code afterPropertiesSet()}, que llama a {@code initializeDatabase()}; quien llame a este
 * último por su cuenta, como hacía {@code FabricaDeBases} hasta S191, se saltaría el primero.
 * Los dos llevan al mismo sitio y ninguno llama a {@code super}: la lógica de scripts de la
 * superclase —{@code spring.sql.init.*}, ubicaciones, modo— no se usa, y los ajustes que se
 * le pasan son los de por defecto sólo porque el constructor los exige.
 */
public class InicializadorEsquema extends DataSourceScriptDatabaseInitializer
        implements ApplicationScriptDatabaseInitializer {

    private final PreparadorEsquema preparador;

    public InicializadorEsquema(DataSource dataSource, PreparadorEsquema preparador) {
        super(dataSource, new DatabaseInitializationSettings());
        this.preparador = preparador;
    }

    @Override
    public void afterPropertiesSet() {
        preparar();
    }

    @Override
    public boolean initializeDatabase() {
        preparar();
        return true;
    }

    private void preparar() {
        preparador.preparar(getDataSource());
    }
}
