package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lo que {@link CursoService} añade sobre el duplicador: el indicador de duplicado en curso
 * (O-curso, S159, fase B).
 *
 * <p>La base del caso lleva SÓLO la tabla {@code curso}, que es todo lo que el duplicador
 * mira antes de rechazar: aquí no se prueba el duplicado —eso es
 * {@code DuplicadorCursoTest}—, se prueba que el indicador se apaga pase lo que pase.
 */
class CursoServiceTest {

    /**
     * (9) Un duplicado RECHAZADO deja el indicador apagado. Si {@code terminarDuplicado()}
     * viviera sólo en el camino de éxito, un nombre mal escrito dejaría la aplicación
     * rechazando toda escritura con un 403 hasta que alguien la reiniciara: el usuario habría
     * perdido el curso por una errata.
     */
    @Test
    void unDuplicadoRechazadoApagaElIndicador(@TempDir Path carpeta) throws Exception {
        Path base = carpeta.resolve("educhronos.db");
        try (Connection conexion =
                        DriverManager.getConnection("jdbc:sqlite:" + base.toAbsolutePath());
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(
                    "create table curso (id integer not null check (id = 1),"
                            + " nombre varchar(9) not null, archivado boolean not null,"
                            + " primary key (id))");
        }
        EstadoCurso estado = new EstadoCurso(null);
        CursoService servicio =
                new CursoService(estado, "jdbc:sqlite:" + base.toAbsolutePath(), "");

        try {
            servicio.duplicar("2026-2027", "2025/2026");
            fail("se esperaba el rechazo del nombre con guion");
        } catch (RechazoCursoException e) {
            assertThat(e.causa()).isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
        }

        assertThat(estado.duplicando())
                .as("el indicador se apaga en el finally, también cuando se rechaza")
                .isFalse();
        assertThat(estado.archivado()).as("un rechazo no archiva nada").isFalse();
    }
}
