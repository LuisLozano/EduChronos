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
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.sqlite.SQLiteDataSource;

/**
 * Spec de {@link PreparadorEsquema} (O-base-tecnica, S191, C-esquema-versionado, condición 1).
 *
 * <p><b>Varios juegos de scripts.</b> El REAL —{@code schema.sql} y {@code esquema/}— tiene
 * desde S201 la versión 2, con una migración de verdad ({@code 002.sql}, que reconstruye
 * {@code sesion}): los casos 1 a 5, el 11 y los de S201 lo usan para fijar lo que pasa con
 * las bases que existen. El de PRUEBA ({@code esquema-prueba/}, versión 2) es el que ejerce el
 * bucle de migraciones, el ROTO ({@code esquema-roto/}) el que mide que una migración que
 * falla no deja nada a medias, y el HUÉRFANO ({@code esquema-huerfano/}) el que mide la
 * guarda de filas huérfanas.
 *
 * <p>Sin Spring: un {@link SQLiteDataSource} sobre un fichero de un {@code @TempDir} y un
 * {@link DefaultResourceLoader}. Cada conexión es nueva, así que lo que se lee después de
 * preparar es lo que quedó en el fichero, no lo que recuerda una conexión.
 */
class PreparadorEsquemaTest {

    private static final DefaultResourceLoader CARGADOR = new DefaultResourceLoader();

    /** Las sesiones campo a campo, en orden estable. */
    private static final String SESIONES =
            "select indice, aula_id, horario_id, id, plaza_id, tramo_inicio_id from sesion order by id";

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

    private static PreparadorEsquema huerfano() {
        return new PreparadorEsquema(
                CARGADOR, "classpath:esquema-huerfano/actual.sql", "classpath:esquema-huerfano/", 2);
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

        assertThat(version(base)).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
        assertThat(maestro(base))
                .as("el DDL es el de schema.sql")
                .isEqualTo(maestro(referencia))
                .isNotEmpty();
    }

    /**
     * (2) Una base de antes de S159 —sin tabla {@code curso}, sin número— gana la tabla por
     * {@code 001.sql}, conserva sus datos y llega a la versión vigente.
     */
    @Test
    void unaBaseAnteriorAS159GanaCursoYConservaSusDatos(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("antigua.db"), null, false);
        ejecutar(
                base,
                "drop table curso",
                "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')");
        assertThat(BancoDeCursos.tieneTabla(base, "curso")).as("de partida no la tiene").isFalse();
        assertThat(version(base)).isZero();

        real().preparar(origen(base));

        assertThat(BancoDeCursos.tieneTabla(base, "curso")).isTrue();
        assertThat(BancoDeCursos.filas(base, "profesor")).as("la fila sigue").isOne();
        assertThat(version(base)).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
    }

    /**
     * (3) Una base de {@code v0.2.0} —el esquema 1 entero, sin número— llega a la vigente: su
     * {@code sqlite_master} queda IGUAL al de una base vacía preparada, que es lo que la regla
     * de cambio promete (test (b) del contrato de S201). Hasta S201 sólo ganaba el sello.
     */
    @Test
    void unaBaseDeV020LlegaALaVigente(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v020.db"), null, false);
        ejecutar(base, "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')");
        assertThat(version(base)).isZero();
        Path vacia = carpeta.resolve("vacia.db");
        real().preparar(origen(vacia));

        real().preparar(origen(base));

        assertThat(maestro(base)).as("sqlite_master, el de una base nueva").isEqualTo(maestro(vacia));
        assertThat(BancoDeCursos.filas(base, "profesor")).as("la fila sigue").isOne();
        assertThat(version(base)).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
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
        // Histórica, para que la PRIMERA preparación escriba de verdad (001 + 002 + sello).
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("dos-veces.db"), null, false);

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

    /**
     * (S201, a) Una base de la versión 1 CON DATOS —coherentes, sin huérfanos— llega a la 2 sin
     * perder nada. {@code 002.sql} reconstruye {@code sesion} (renombrar, crear, copiar, borrar)
     * y añade {@code actividad.tipo}: el esquema queda IGUAL al de una base nueva, las sesiones
     * idénticas campo a campo, todas las actividades como CLASE y ningún huérfano.
     */
    @Test
    void unaBaseV1ConDatosLlegaALaVigenteSinPerderNada(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v1.db"), null, false);
        ejecutar(
                base,
                "PRAGMA user_version = 1",
                "insert into aula (id, codigo, tipo) values (1, 'A1', 'ORDINARIA')",
                "insert into tramo_semanal (id, dia, orden, hora_inicio, hora_fin, es_lectivo)"
                        + " values (1, 'LUNES', 1, '08:00:00', '09:00:00', 1)",
                "insert into tramo_semanal (id, dia, orden, hora_inicio, hora_fin, es_lectivo)"
                        + " values (2, 'LUNES', 2, '09:00:00', '10:00:00', 1)",
                "insert into asignatura (id, codigo, nombre_completo) values (1, 'MAT', 'Matemáticas')",
                "insert into actividad (id, codigo, asignatura_id, duracion_tramos,"
                        + " repeticiones_por_semana, patron_temporal, requiere_tutor)"
                        + " values (1, 'MAT-1A', 1, 1, 2, 'NEUTRA', 0)",
                "insert into actividad (id, codigo, asignatura_id, duracion_tramos,"
                        + " repeticiones_por_semana, patron_temporal, requiere_tutor)"
                        + " values (2, 'TUT-1A', 1, 1, 1, 'NEUTRA', 1)",
                "insert into plaza (id, codigo, actividad_id, asignatura_id, aula_fija_id)"
                        + " values (1, 'MAT-1A-P1', 1, 1, 1)",
                "insert into horario_generado (id, nombre, estado, estado_solver, fecha_generacion)"
                        + " values (1, 'H1', 'BORRADOR', 'OPTIMAL', '2026-10-06 10:00:00')",
                "insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id)"
                        + " values (1, 1, 1, 1, 1, 1)",
                "insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id)"
                        + " values (2, 1, 1, 2, 2, 1)");
        List<String> sesionesAntes = consulta(base, SESIONES);
        int actividadesAntes = BancoDeCursos.filas(base, "actividad");
        assertThat(sesionesAntes).as("precondición: dos sesiones").hasSize(2);
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("precondición: sin huérfanos").isEmpty();
        Path vacia = carpeta.resolve("vacia.db");
        real().preparar(origen(vacia));

        real().preparar(origen(base));

        assertThat(maestro(base)).as("sqlite_master, el de una base nueva").isEqualTo(maestro(vacia));
        assertThat(consulta(base, SESIONES)).as("las sesiones, campo a campo").isEqualTo(sesionesAntes);
        assertThat(consulta(base, "select count(*) from actividad where tipo <> 'CLASE'"))
                .as("todas las actividades, CLASE")
                .containsExactly("0");
        assertThat(BancoDeCursos.filas(base, "actividad")).isEqualTo(actividadesAntes);
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("ningún huérfano").isEmpty();
        assertThat(version(base)).isEqualTo(2);
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

    // ─────────────────────────────────────────────────────────────── juego huérfano

    /**
     * (S201, c) Una migración que deja una fila huérfana se deshace entera, aunque la conexión
     * llegue con las claves foráneas APAGADAS —como en el cambio de curso—: con ellas apagadas
     * SQLite acepta la fila, y lo único que la para es la guarda del preparador. El
     * {@link DataSource} de {@link #origen} no está envuelto, así que no enciende el pragma.
     */
    @Test
    void unaMigracionQueDejaHuerfanosSeDeshace(@TempDir Path carpeta) throws Exception {
        Path base = carpeta.resolve("v1.db");
        try (Connection conexion = BancoDeCursos.conectar(base)) {
            ScriptUtils.executeSqlScript(
                    conexion, CARGADOR.getResource("classpath:esquema-huerfano/001.sql"));
        }
        ejecutar(base, "PRAGMA user_version = 1");

        assertThatThrownBy(() -> huerfano().preparar(origen(base)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("huérfanas");

        assertThat(version(base)).as("sigue en la última completa").isEqualTo(1);
        assertThat(BancoDeCursos.filas(base, "hija")).as("la fila huérfana se deshizo").isZero();
    }

    /**
     * (S201, c2) Un huérfano que la base YA traía no impide migrar: la guarda cuenta sólo las
     * filas que deja la migración. Si mirara la base entera, una base con un huérfano antiguo
     * no podría pasar nunca a la versión siguiente.
     */
    @Test
    void unHuerfanoAnteriorNoImpideMigrar(@TempDir Path carpeta) throws Exception {
        Path base = baseConUno(carpeta.resolve("v1.db"), 1);
        ejecutar(
                base,
                "PRAGMA foreign_keys = OFF",
                "create table padre (id integer primary key)",
                "create table hija (id integer primary key, padre_id integer references padre(id))",
                "insert into hija (id, padre_id) values (1, 99)");
        assertThat(consulta(base, "PRAGMA foreign_key_check"))
                .as("precondición: un huérfano de antes")
                .hasSize(1);

        deprueba().preparar(origen(base));

        assertThat(version(base)).isEqualTo(2);
        assertThat(BancoDeCursos.filas(base, "hija")).as("el huérfano de antes sigue ahí").isOne();
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

    /** Las filas de una consulta, cada una con sus columnas unidas por {@code |}. */
    private static List<String> consulta(Path base, String sql) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = BancoDeCursos.conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery(sql)) {
            int columnas = fila.getMetaData().getColumnCount();
            while (fila.next()) {
                StringBuilder linea = new StringBuilder();
                for (int i = 1; i <= columnas; i++) {
                    if (i > 1) {
                        linea.append('|');
                    }
                    linea.append(fila.getString(i));
                }
                filas.add(linea.toString());
            }
        }
        return filas;
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
