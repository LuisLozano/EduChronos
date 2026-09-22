package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import es.yaroki.educhronos.app.curso.BancoDeCursos;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec de {@link BaseConmutable}: el {@code DataSource} que cambia de pool por debajo
 * (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>Unitario y sin Spring</b>, con pools de Hikari construidos a mano por la misma
 * {@link FabricaDeBases} que usa la aplicación. Lo que aquí se mide es el mecanismo —a qué
 * fichero va una conexión y qué pasa con el pool saliente—, y montar un contexto entero para
 * eso escondería justo lo que se quiere ver.
 *
 * <p>La pregunta «¿a qué fichero fui?» no se responde mirando configuración: se escribe una
 * marca en cada base y se lee por la conexión. Es la única forma de que el caso no pueda pasar
 * en verde con la delegación rota.
 */
class BaseConmutableTest {

    /**
     * (T1) Tras sustituir, las conexiones NUEVAS van al fichero nuevo, {@code baseAbierta()}
     * lo dice, y el pool que se devuelve es el viejo y sigue vivo hasta que alguien lo cierre.
     *
     * <p>El aserto de que el viejo sigue abierto al devolverlo es tan importante como el
     * resto: {@code sustituir} NO cierra, porque quien cambia de curso necesita poder volver
     * a ponerlo si el paso siguiente falla (invariante I3).
     */
    @Test
    void trasSustituirLasConexionesVanAlFicheroNuevo(@TempDir Path carpeta) throws Exception {
        FabricaDeBases fabrica = BancoDeCursos.fabrica();
        Path una = BancoDeCursos.fabricar(carpeta.resolve("una.db"), "2025/2026", false);
        Path otra = BancoDeCursos.fabricar(carpeta.resolve("otra.db"), "2026/2027", false);

        BaseConmutable conmutable = BancoDeCursos.conmutableSobre(fabrica, una);
        assertThat(conmutable.baseAbierta()).isEqualTo(una);
        assertThat(nombreDelCurso(conmutable)).as("antes de sustituir").isEqualTo("2025/2026");

        HikariDataSource nuevo = fabrica.poolPara(otra);
        HikariDataSource viejo = conmutable.sustituir(nuevo, otra);

        assertThat(conmutable.baseAbierta()).isEqualTo(otra);
        assertThat(nombreDelCurso(conmutable))
                .as("la conexión siguiente sale del pool nuevo")
                .isEqualTo("2026/2027");
        assertThat(viejo.isClosed()).as("sustituir NO cierra el que devuelve").isFalse();

        viejo.close();
        assertThat(viejo.isClosed()).as("y el llamador sí puede cerrarlo").isTrue();
        assertThat(nombreDelCurso(conmutable))
                .as("cerrar el viejo no afecta al vigente")
                .isEqualTo("2026/2027");
        conmutable.close();
        assertThat(nuevo.isClosed()).as("close() cierra el pool VIGENTE").isTrue();
    }

    /**
     * (T2, invariante I1) {@code PRAGMA foreign_keys} devuelve 1 en una conexión sacada del
     * bean DESPUÉS de un cambio.
     *
     * <p>Es el caso que justifica que {@link BaseConmutable} herede de
     * {@code ForeignKeysEnforcingDataSource} en vez de ser un delegado cualquiera envuelto por
     * el post-procesador: con la herencia, el pragma lo pone el {@code getConnection()} de la
     * superclase sobre el pool vigente, sea cual sea. El aserto de «antes» está para que el
     * caso no pueda pasar por casualidad sobre una base que ya lo trajera encendido.
     *
     * <p>Se aparea con una escritura que VIOLA una clave ajena: que el pragma valga 1 y que la
     * base lo haga cumplir son dos hechos distintos, y el que importa es el segundo.
     */
    @Test
    void lasFkSiguenEncendidasDespuesDeUnCambio(@TempDir Path carpeta) throws Exception {
        FabricaDeBases fabrica = BancoDeCursos.fabrica();
        Path una = BancoDeCursos.fabricar(carpeta.resolve("una.db"), "2025/2026", false);
        Path otra = BancoDeCursos.fabricar(carpeta.resolve("otra.db"), "2026/2027", false);

        BaseConmutable conmutable = BancoDeCursos.conmutableSobre(fabrica, una);
        assertThat(pragmaFk(conmutable)).as("antes del cambio").isOne();

        conmutable.sustituir(fabrica.poolPara(otra), otra).close();

        assertThat(pragmaFk(conmutable)).as("después del cambio").isOne();
        assertThatThrownBy(() -> insertarGrupoHuerfano(conmutable))
                .as("y la base las HACE CUMPLIR, que es lo que el pragma compra")
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("FOREIGN KEY");
    }

    /**
     * Una URL de SQLite con parámetros detrás: la ruta sale limpia. El caso existe porque
     * {@code CursoService} resolvía esto mismo antes de S160 y la regla se mudó aquí; sin
     * ella, la base abierta sería un fichero con un {@code ?} en el nombre.
     */
    @Test
    void laRutaSaleDeLaUrlSinPrefijoYSinParametros() {
        assertThat(BaseConmutable.ficheroDeUrl("jdbc:sqlite:/tmp/x/educhronos.db"))
                .isEqualTo(Path.of("/tmp/x/educhronos.db"));
        assertThat(BaseConmutable.ficheroDeUrl("jdbc:sqlite:/tmp/x/a.db?foreign_keys=on"))
                .isEqualTo(Path.of("/tmp/x/a.db"));
    }

    // ─────────────────────────────────────────────────────────────────────────────── andamio

    /** Lee el nombre del curso POR EL DATASOURCE, que es lo que dice a qué fichero se fue. */
    private static String nombreDelCurso(BaseConmutable conmutable) throws SQLException {
        try (Connection conexion = conmutable.getConnection();
                Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("select nombre from curso where id = 1")) {
            return filas.next() ? filas.getString(1) : null;
        }
    }

    private static int pragmaFk(BaseConmutable conmutable) throws SQLException {
        try (Connection conexion = conmutable.getConnection();
                Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("PRAGMA foreign_keys")) {
            filas.next();
            return filas.getInt(1);
        }
    }

    /** Un grupo que apunta a un nivel que no existe: con las FK activas, esto no entra. */
    private static void insertarGrupoHuerfano(BaseConmutable conmutable) throws SQLException {
        try (Connection conexion = conmutable.getConnection();
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(
                    "insert into grupo_administrativo (id, codigo, nivel_id, tipo)"
                            + " values (1, 'G1', 999, 'ORDINARIO')");
        }
    }
}
