package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.curso.BancoDeCursos;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.sqlite.SQLiteDataSource;

/**
 * Spec de {@link PreparadorEsquema} (O-base-tecnica, S191, C-esquema-versionado, condición 1).
 *
 * <p><b>Dos juegos de scripts.</b> El REAL —{@code schema.sql} y {@code esquema/}— sólo tiene
 * hoy la versión 1, así que con él no se puede ver una migración de verdad: los casos 1 a 5
 * y el 11 lo usan para fijar lo que pasa con las bases que existen. El de PRUEBA
 * ({@code esquema-prueba/}, versión 2) es el que ejerce el bucle de migraciones, y el ROTO
 * ({@code esquema-roto/}) el que mide que una migración que falla no deja nada a medias.
 *
 * <p>Sin Spring: un {@link SQLiteDataSource} sobre un fichero de un {@code @TempDir} y un
 * {@link DefaultResourceLoader}. Cada conexión es nueva, así que lo que se lee después de
 * preparar es lo que quedó en el fichero, no lo que recuerda una conexión.
 */
class PreparadorEsquemaTest {

    private static final DefaultResourceLoader CARGADOR = new DefaultResourceLoader();

    private static PreparadorEsquema real() {
        return new PreparadorEsquema(
                CARGADOR,
                PreparadorEsquema.ESQUEMA_VIGENTE,
                PreparadorEsquema.MIGRACIONES,
                PreparadorEsquema.VERSION_ESQUEMA);
    }

    private static PreparadorEsquema deprueba() {
        return new PreparadorEsquema(
                CARGADOR, "classpath:esquema-prueba/actual.sql", "classpath:esquema-prueba/", 2);
    }

    private static PreparadorEsquema roto() {
        return new PreparadorEsquema(
                CARGADOR, "classpath:esquema-roto/actual.sql", "classpath:esquema-roto/", 2);
    }

    // ─────────────────────────────────────────────────────────────── juego real

    /**
     * (1) Una base vacía recibe el esquema vigente entero y queda sellada. El DDL se compara
     * con el de una base levantada a mano desde {@code schema.sql}: tablas, columnas y
     * restricciones, que es lo que guarda {@code sqlite_master}.
     */
    @Test
    void unaBaseVaciaRecibeElEsquemaVigenteYQuedaSellada(@TempDir Path carpeta) throws Exception {
        Path base = carpeta.resolve("vacia.db");
        Path referencia = BancoDeCursos.fabricar(carpeta.resolve("referencia.db"), null, false);

        real().preparar(origen(base));

        assertThat(version(base)).isEqualTo(1);
        assertThat(maestro(base))
                .as("el DDL es el de schema.sql")
                .isEqualTo(maestro(referencia))
                .isNotEmpty();
    }

    /**
     * (2) Una base de antes de S159 —sin tabla {@code curso}, sin número— gana la tabla por
     * {@code 001.sql}, conserva sus datos y queda en la versión 1.
     */
    @Test
    void unaBaseAnteriorAS159GanaCursoYConservaSusDatos(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricar(carpeta.resolve("antigua.db"), null, false);
        ejecutar(
                base,
                "drop table curso",
                "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')");
        assertThat(BancoDeCursos.tieneTabla(base, "curso")).as("de partida no la tiene").isFalse();
        assertThat(version(base)).isZero();

        real().preparar(origen(base));

        assertThat(BancoDeCursos.tieneTabla(base, "curso")).isTrue();
        assertThat(BancoDeCursos.filas(base, "profesor")).as("la fila sigue").isOne();
        assertThat(version(base)).isEqualTo(1);
    }

    /**
     * (3) Una base de {@code v0.2.0} —esquema entero, sin número— no cambia en nada salvo el
     * sello: {@code sqlite_master} es el mismo antes y después.
     */
    @Test
    void unaBaseDeV020SoloGanaElSello(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricar(carpeta.resolve("v020.db"), null, false);
        ejecutar(base, "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')");
        List<String> antes = maestro(base);
        assertThat(version(base)).isZero();

        real().preparar(origen(base));

        assertThat(maestro(base)).as("sqlite_master, idéntico").isEqualTo(antes);
        assertThat(BancoDeCursos.filas(base, "profesor")).as("la fila sigue").isOne();
        assertThat(version(base)).isEqualTo(1);
    }

    /**
     * (4) Una base de una versión más nueva se rechaza con los dos números en el mensaje, y
     * NO SE TOCA: el aserto es sobre su md5, y además la tabla que le falta sigue faltando.
     */
    @Test
    void unaBasePosteriorSeRechazaSinTocarla(@TempDir Path carpeta) throws Exception {
        int posterior = PreparadorEsquema.VERSION_ESQUEMA + 1;
        Path base = BancoDeCursos.fabricar(carpeta.resolve("posterior.db"), null, false);
        ejecutar(base, "drop table curso", "PRAGMA user_version = " + posterior);
        String huella = BancoDeCursos.huella(base);

        assertThatThrownBy(() -> real().preparar(origen(base)))
                .isInstanceOfSatisfying(
                        EsquemaPosteriorException.class,
                        e -> {
                            assertThat(e.getDeLaBase()).isEqualTo(posterior);
                            assertThat(e.getMaximo()).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
                            assertThat(e.getMessage())
                                    .contains("esquema " + posterior)
                                    .contains("hasta el esquema " + PreparadorEsquema.VERSION_ESQUEMA);
                        });

        assertThat(BancoDeCursos.huella(base)).as("la base, intacta").isEqualTo(huella);
        assertThat(BancoDeCursos.tieneTabla(base, "curso")).as("no se ha migrado nada").isFalse();
    }

    /**
     * (5) Preparar dos veces la misma base no escribe la segunda vez: el md5 tras la segunda
     * es el de tras la primera. Es lo que pasa en cada arranque sobre una base al día.
     */
    @Test
    void prepararDosVecesNoEscribeLaSegunda(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricar(carpeta.resolve("dos-veces.db"), null, false);

        real().preparar(origen(base));
        String trasLaPrimera = BancoDeCursos.huella(base);
        real().preparar(origen(base));

        assertThat(BancoDeCursos.huella(base)).isEqualTo(trasLaPrimera);
    }

    /**
     * (11) Las migraciones del juego real están completas y no sobra ninguna: existe
     * {@code esquema/NNN.sql} para cada número hasta {@link PreparadorEsquema#VERSION_ESQUEMA}
     * y no existe el siguiente. Quien suba la versión sin añadir el script, o añada el script
     * sin subir la versión, cae aquí.
     */
    @Test
    void lasMigracionesRealesLleganJustoHastaLaVersion() {
        for (int n = 1; n <= PreparadorEsquema.VERSION_ESQUEMA; n++) {
            assertThat(migracionReal(n).exists()).as("esquema/%03d.sql", n).isTrue();
        }
        int siguiente = PreparadorEsquema.VERSION_ESQUEMA + 1;
        assertThat(migracionReal(siguiente).exists())
                .as("esquema/%03d.sql no debe existir todavía", siguiente)
                .isFalse();
    }

    // ─────────────────────────────────────────────────────────────── juego de prueba

    /** (6) Base vacía con el juego de prueba: el esquema vigente y el sello 2. */
    @Test
    void deprueba_unaBaseVaciaQuedaEnLaVersion2(@TempDir Path carpeta) throws Exception {
        Path base = carpeta.resolve("vacia.db");

        deprueba().preparar(origen(base));

        assertThat(version(base)).isEqualTo(2);
        assertThat(BancoDeCursos.tieneTabla(base, "uno")).isTrue();
        assertThat(BancoDeCursos.tieneTabla(base, "dos")).isTrue();
    }

    /**
     * (7) Una base sin número con la tabla de la versión 1 y una fila pasa por {@code 001.sql}
     * y {@code 002.sql}: queda en 2, con la tabla nueva y la fila.
     */
    @Test
    void deprueba_unaBaseSinNumeroPasaPorLasDosMigraciones(@TempDir Path carpeta) throws Exception {
        Path base = baseConUno(carpeta.resolve("v0.db"), 0);

        deprueba().preparar(origen(base));

        assertThat(version(base)).isEqualTo(2);
        assertThat(BancoDeCursos.tieneTabla(base, "dos")).isTrue();
        assertThat(BancoDeCursos.filas(base, "uno")).as("la fila sigue").isOne();
    }

    /** (8) Una base sellada a 1 sólo recibe {@code 002.sql}. */
    @Test
    void deprueba_unaBaseSelladaA1SoloRecibeLa2(@TempDir Path carpeta) throws Exception {
        Path base = baseConUno(carpeta.resolve("v1.db"), 1);

        deprueba().preparar(origen(base));

        assertThat(version(base)).isEqualTo(2);
        assertThat(BancoDeCursos.tieneTabla(base, "dos")).isTrue();
    }

    /**
     * (9) Equivalencia: llegar a la versión 2 por migraciones deja el MISMO esquema que
     * crearla vacía. Es lo que la regla de cambio promete, y lo que se rompería si un
     * {@code NNN.sql} y {@code schema.sql} dejaran de decir lo mismo. Se compara
     * {@code sqlite_master} sin {@code rootpage}, que depende del orden de creación.
     */
    @Test
    void deprueba_porMigracionesYDesdeVaciaSeLlegaAlMismoEsquema(@TempDir Path carpeta)
            throws Exception {
        Path vacia = carpeta.resolve("vacia.db");
        Path migrada = baseConUno(carpeta.resolve("migrada.db"), 0);

        deprueba().preparar(origen(vacia));
        deprueba().preparar(origen(migrada));

        assertThat(maestro(migrada)).isEqualTo(maestro(vacia)).isNotEmpty();
    }

    // ─────────────────────────────────────────────────────────────── juego roto

    /**
     * (10) Una migración que falla a mitad no deja NADA: ni la tabla que su primera sentencia
     * llegó a crear ni el sello. La base se queda en la versión 1, que es la última completa.
     */
    @Test
    void roto_unaMigracionQueFallaSeDeshaceEntera(@TempDir Path carpeta) throws Exception {
        Path base = baseConUno(carpeta.resolve("v1.db"), 1);

        assertThatThrownBy(() -> roto().preparar(origen(base)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("002.sql");

        assertThat(version(base)).as("sigue en la última completa").isEqualTo(1);
        assertThat(BancoDeCursos.tieneTabla(base, "dos"))
                .as("la primera sentencia de 002.sql se deshizo")
                .isFalse();
    }

    // ─────────────────────────────────────────────────────────────── andamio

    private static DataSource origen(Path base) {
        SQLiteDataSource fuente = new SQLiteDataSource();
        fuente.setUrl(BaseConmutable.PREFIJO_URL + base.toAbsolutePath());
        return fuente;
    }

    private static org.springframework.core.io.Resource migracionReal(int numero) {
        return CARGADOR.getResource(
                PreparadorEsquema.MIGRACIONES + String.format("%03d.sql", numero));
    }

    /** Una base con la tabla {@code uno} del juego de prueba, una fila y el número dado. */
    private static Path baseConUno(Path base, int numero) throws SQLException {
        ejecutar(
                base,
                "create table if not exists uno (id integer primary key, texto varchar(20))",
                "insert into uno (id, texto) values (1, 'fila')",
                "PRAGMA user_version = " + numero);
        return base;
    }

    private static void ejecutar(Path base, String... ordenes) throws SQLException {
        try (Connection conexion = BancoDeCursos.conectar(base);
                Statement sentencia = conexion.createStatement()) {
            for (String orden : ordenes) {
                sentencia.execute(orden);
            }
        }
    }

    private static int version(Path base) throws SQLException {
        try (Connection conexion = BancoDeCursos.conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery("PRAGMA user_version")) {
            fila.next();
            return fila.getInt(1);
        }
    }

    /**
     * {@code sqlite_master} sin {@code rootpage}, ordenado: tipo, nombre, tabla y DDL de cada
     * objeto. {@code rootpage} se deja fuera porque es la página donde cayó cada tabla, que
     * depende del orden en que se crearon y no del esquema.
     */
    private static List<String> maestro(Path base) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = BancoDeCursos.conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet fila =
                        sentencia.executeQuery(
                                "select type, name, tbl_name, sql from sqlite_master"
                                        + " order by type, name")) {
            while (fila.next()) {
                filas.add(
                        fila.getString(1) + "|" + fila.getString(2) + "|" + fila.getString(3)
                                + "|" + fila.getString(4));
            }
        }
        return filas;
    }
}
