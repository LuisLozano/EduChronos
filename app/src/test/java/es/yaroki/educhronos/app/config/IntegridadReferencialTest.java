package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Prueba que la integridad referencial está REALMENTE activa (Bloque 8.5-C2a-DDL):
 * las dos piezas juntas, el {@code schema.sql} con FK y el pragma por conexión del
 * pool ({@link SqliteForeignKeysConfig}). No basta con que el contexto arranque.
 *
 * <p>Criterio de éxito: una conexión SACADA DEL POOL lee {@code PRAGMA
 * foreign_keys}=1 y un INSERT colgante (FK a fila inexistente) LANZA
 * {@code SQLITE_CONSTRAINT_FOREIGNKEY} (código primario 19).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IntegridadReferencialTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void elPragmaEstaEncendidoEnUnaConexionDelPool() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("PRAGMA foreign_keys")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).as("foreign_keys debe estar ON en la conexion del pool").isEqualTo(1);
        }
    }

    @Test
    void unInsertColganteVictimaDeFkLanzaConstraintForeignkey() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThatExceptionOfType(SQLException.class)
                    .isThrownBy(() -> statement.executeUpdate(
                            "insert into asignatura_aula_compatible (asignatura_id, tipo_aula) "
                                    + "values (999999, 'ORDINARIA')"))
                    .satisfies(ex -> {
                        assertThat(ex.getMessage()).containsIgnoringCase("FOREIGN KEY constraint failed");
                        assertThat(ex.getErrorCode()).as("codigo primario SQLITE_CONSTRAINT").isEqualTo(19);
                    });
        }
    }
}
