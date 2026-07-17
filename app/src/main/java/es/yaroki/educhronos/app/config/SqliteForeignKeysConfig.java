package es.yaroki.educhronos.app.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Enciende la integridad referencial de SQLite en CADA conexión del pool
 * (Bloque 8.5-C2a-DDL). Es la SEGUNDA mitad, indispensable, de la integridad: el
 * {@code schema.sql} declara las FK, pero SQLite las ignora salvo que la conexión
 * tenga {@code PRAGMA foreign_keys=ON}. Ese pragma es POR CONEXIÓN y no persiste.
 *
 * <p>Vehículo por CÓDIGO, no por property: se midió (8.5-C2a) que ni
 * {@code spring.datasource.hikari.connection-init-sql} ni el parámetro de URL
 * propagaban el pragma en este stack (leían {@code foreign_keys=0}). Aquí se
 * ENVUELVE el {@link DataSource} autoconfigurado en uno que ejecuta el pragma en
 * cada {@code getConnection()}, de modo que toda conexión sacada del pool nace
 * con las FK activas.
 *
 * <p>Es una {@link AutoConfiguration} (registrada en
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * para que la apliquen por igual el arranque normal y los {@code @DataJpaTest}.
 */
@AutoConfiguration
public class SqliteForeignKeysConfig {

    /** Sentencia que se corre al entregar cada conexión del pool. */
    static final String PRAGMA_FK_ON = "PRAGMA foreign_keys=ON";

    /**
     * Envuelve el {@link DataSource} del pool en uno que enciende el pragma en cada
     * checkout. Un {@link BeanPostProcessor} porque el {@code DataSource} lo
     * autoconfigura Spring Boot; aquí solo se lo decora sin sustituir la fábrica.
     */
    @Bean
    static BeanPostProcessor sqliteForeignKeysDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource
                        && !(bean instanceof ForeignKeysEnforcingDataSource)) {
                    return new ForeignKeysEnforcingDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    /**
     * Decorador que ejecuta {@link #PRAGMA_FK_ON} sobre cada conexión antes de
     * entregarla. Corre en el checkout, cuando la conexión aún no está en una
     * transacción (SQLite ignoraría el pragma dentro de una), de modo que las FK
     * quedan activas para toda la vida del checkout.
     */
    static final class ForeignKeysEnforcingDataSource extends DelegatingDataSource {

        ForeignKeysEnforcingDataSource(DataSource delegate) {
            super(delegate);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return encender(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return encender(super.getConnection(username, password));
        }

        private static Connection encender(Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.execute(PRAGMA_FK_ON);
            }
            return connection;
        }
    }
}
