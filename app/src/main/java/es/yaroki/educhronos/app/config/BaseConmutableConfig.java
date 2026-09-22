package es.yaroki.educhronos.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Declara el {@code DataSource} del proyecto: una {@link BaseConmutable} en vez del pool que
 * autoconfigura Spring Boot (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>{@code before = DataSourceAutoConfiguration.class} es obligatorio.</b> El pool de
 * Boot se declara bajo {@code @ConditionalOnMissingBean(DataSource.class)}, y esa condición
 * se resuelve en el orden en que se registran las autoconfiguraciones: sin el {@code before},
 * la de Boot correría primero, pondría su {@code HikariDataSource} y la de aquí se retiraría
 * sola, dejando la aplicación con un pool que no se puede sustituir y sin un solo error.
 *
 * <p><b>{@link DataSourceProperties} sigue existiendo</b> aunque el pool de Boot se retire:
 * lo registra el {@code @EnableConfigurationProperties} de la autoconfiguración RAÍZ, cuyo
 * único {@code @ConditionalOnMissingBean} es sobre {@code io.r2dbc.spi.ConnectionFactory}
 * (verificado sobre el bytecode de Boot 4.1.0 en el M2 de S160). Por eso se puede pedir por
 * parámetro y por eso el pool inicial nace con los ajustes de siempre.
 *
 * <p><b>Registrada SÓLO en el {@code .imports} de {@code main}</b>, y no en el de los
 * {@code @DataJpaTest}. Los treinta y cinco slices abren una base y no la cambian nunca: no
 * necesitan conmutar, y meterles un {@code DataSource} distinto del que tienen hoy sería
 * cambiarles el suelo a cambio de nada. {@code SqliteForeignKeysConfig} sí sigue en los dos,
 * porque las FK sí las necesitan.
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
public class BaseConmutableConfig {

    @Bean
    FabricaDeBases fabricaDeBases(
            DataSourceProperties propiedades,
            SqlInitializationProperties inicializacion,
            ResourceLoader cargador) {
        return new FabricaDeBases(propiedades, inicializacion, cargador);
    }

    /**
     * El pool inicial y su envoltura conmutable.
     *
     * <p>Aquí NO se corre {@code schema.sql}: de eso sigue encargándose Boot, con su propio
     * inicializador, sobre este mismo bean. Correrlo también aquí lo ejecutaría dos veces en
     * cada arranque.
     *
     * <p>{@code destroyMethod = "close"} cierra el pool VIGENTE al cerrar el contexto, que es
     * el que puede no ser el inicial.
     */
    @Bean(destroyMethod = "close")
    BaseConmutable dataSource(FabricaDeBases fabrica, DataSourceProperties propiedades) {
        java.nio.file.Path fichero = BaseConmutable.ficheroDeUrl(propiedades.determineUrl());
        HikariDataSource pool = fabrica.poolPara(fichero);
        return new BaseConmutable(pool, fichero);
    }
}
