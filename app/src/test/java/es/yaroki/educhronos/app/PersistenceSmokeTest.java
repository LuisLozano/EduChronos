package es.yaroki.educhronos.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Humo de persistencia (Fase 6, Bloque 1): levanta el contexto Spring completo
 * y verifica que Hibernate ha creado el fichero SQLite en disco, en ruta
 * relativa al working dir.
 */
@SpringBootTest
class PersistenceSmokeTest {

    /** Debe coincidir con la URL de application.properties de test. */
    private static final Path DB_FILE = Path.of("educhronos-test.db");

    @Test
    void contextoArrancaYCreaLaBaseDeDatosEnDisco() {
        assertThat(Files.exists(DB_FILE))
                .as("el fichero SQLite debe existir tras arrancar el contexto")
                .isTrue();
    }

    @AfterAll
    static void limpiarBaseDeDatos() throws IOException {
        Files.deleteIfExists(DB_FILE);
    }
}
