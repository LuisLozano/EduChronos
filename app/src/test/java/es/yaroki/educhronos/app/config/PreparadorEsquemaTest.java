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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.sqlite.SQLiteDataSource;

/**
 * Spec de {@link PreparadorEsquema} (O-base-tecnica, S191, C-esquema-versionado, condición 1).
 *
 * <p><b>Varios juegos de scripts.</b> El REAL —{@code schema.sql} y {@code esquema/}— tiene
 * desde S201 migraciones de verdad ({@code 002.sql}, que reconstruye {@code sesion}) y desde
 * S203 la versión 3 ({@code 003.sql}, totales declarados y cargo): los casos 1 a 5, el 11 y
 * los de S201 y S203 lo usan para fijar lo que pasa con las bases que existen. El de PRUEBA
 * ({@code esquema-prueba/}, versión 2) es el que ejerce el bucle de migraciones, el ROTO
 * ({@code esquema-roto/}) el que mide que una migración que falla no deja nada a medias, y el
 * HUÉRFANO ({@code esquema-huerfano/}) el que mide la guarda de filas huérfanas.
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
     * (S201, a) Una base de la versión 1 CON DATOS —coherentes, sin huérfanos— llega a la vigente
     * sin perder nada (hasta S203, a la 2). {@code 002.sql} reconstruye {@code sesion}
     * (renombrar, crear, copiar, borrar) y añade {@code actividad.tipo}: el esquema queda IGUAL
     * al de una base nueva, las sesiones idénticas campo a campo, todas las actividades como
     * CLASE y ningún huérfano.
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
        assertThat(version(base)).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
    }

    /**
     * (S203) Una base de la versión 2 CON DATOS llega a la vigente sin perder nada (hasta S206, a
     * la 3), calcado de (S201, a). La base se levanta con las migraciones reales hasta la 2 ({@code 001.sql} y
     * {@code 002.sql}) y se sella a mano. {@code 003.sql} añade tres columnas: el esquema queda
     * IGUAL al de una base nueva, los profesores y los grupos —un ordinario y su PDC— idénticos
     * campo a campo, todos los profesores como PROFESOR y nadie con total declarado.
     */
    @Test
    void unaBaseV2ConDatosLlegaALaVigenteSinPerderNada(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v2.db"), null, false);
        try (Connection conexion = BancoDeCursos.conectar(base)) {
            ScriptUtils.executeSqlScript(conexion, migracionReal(2));
        }
        ejecutar(
                base,
                "PRAGMA user_version = 2",
                "insert into profesor (id, codigo, nombre_completo) values (1, 'MAT1', 'Uno')",
                "insert into profesor (id, codigo, nombre_completo) values (2, 'LEN1', 'Dos')",
                "insert into nivel (id, codigo, orden) values (1, 'ESO3', 3)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo, grupo_padre_id)"
                        + " values (1, '3A', 1, 'ORDINARIO', null)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo, grupo_padre_id)"
                        + " values (2, '3ADI', 1, 'DIVERSIFICACION_PDC', 1)");
        String profesores = "select id, codigo, nombre_completo from profesor order by id";
        String grupos =
                "select id, codigo, nivel_id, tipo, grupo_padre_id from grupo_administrativo order by id";
        List<String> profesoresAntes = consulta(base, profesores);
        List<String> gruposAntes = consulta(base, grupos);
        assertThat(version(base)).as("precondición: versión 2").isEqualTo(2);
        assertThat(profesoresAntes).as("precondición: dos profesores").hasSize(2);
        assertThat(gruposAntes).as("precondición: dos grupos").hasSize(2);
        Path vacia = carpeta.resolve("vacia.db");
        real().preparar(origen(vacia));

        real().preparar(origen(base));

        assertThat(maestro(base)).as("sqlite_master, el de una base nueva").isEqualTo(maestro(vacia));
        assertThat(consulta(base, profesores))
                .as("los profesores, campo a campo")
                .isEqualTo(profesoresAntes);
        assertThat(consulta(base, grupos)).as("los grupos, campo a campo").isEqualTo(gruposAntes);
        assertThat(consulta(base, "select id, cargo, total_declarado from profesor order by id"))
                .as("todos PROFESOR y sin total")
                .containsExactly("1|PROFESOR|null", "2|PROFESOR|null");
        assertThat(consulta(base, "select id, total_declarado from grupo_administrativo order by id"))
                .as("ningún grupo con total")
                .containsExactly("1|null", "2|null");
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("ningún huérfano").isEmpty();
        assertThat(version(base)).isEqualTo(PreparadorEsquema.VERSION_ESQUEMA);
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

    /**
     * (S206, S212, S213, S219, S220, S250) Una base de la versión 0, 1, 3, 4, 5, 6, 7 u 8 CON DATOS en todas
     * las tablas de la 1 llega a la 9 sin perder nada. La base se levanta con {@code 001.sql}, se puebla con
     * dos profesores y, para la 3 en adelante, se le pasan {@code 002.sql} y {@code 003.sql} reales (y
     * {@code 004.sql} desde la 4, con una fila en {@code asignatura_aula}, {@code 005.sql} desde la 5,
     * {@code 006.sql} desde la 6, {@code 007.sql} desde la 7 y {@code 008.sql} para la 8) antes de sellarla. {@code 004.sql} añade el
     * aula de referencia del grupo, los alumnos del subgrupo, el aula en uso, la tabla
     * {@code asignatura_aula} y las cuatro únicas; {@code 005.sql}, las guardias ordinarias del
     * profesor; {@code 006.sql}, la tabla {@code guardia}; {@code 007.sql}, las guardias de recreo del
     * profesor; {@code 008.sql}, el tipo de la guardia; {@code 009.sql}, el código corto y el nivel de la
     * asignatura, que aquí es de un solo nivel y no se parte. Cada tabla conserva sus filas, todas las aulas quedan en uso, las columnas nuevas
     * nulas, {@code asignatura_aula} como estaba, los cuatro índices únicos presentes, las columnas
     * {@code guardias_ordinarias} y {@code guardias_recreo} a 0 en cada profesor, {@code guardia} vacía
     * y con su única, y el esquema IGUAL al de una base nueva. Las de la versión 6 y 7 ya traen la tabla
     * {@code guardia}, vacía: es la única tabla que la precondición deja vacía.
     */
    @ParameterizedTest(name = "desde la versión {0}")
    @ValueSource(ints = {0, 1, 3, 4, 5, 6, 7, 8})
    void unaBaseConDatosLlegaALa9SinPerderNada(int desde, @TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v" + desde + ".db"), "2025/2026", false);
        poblarTodasLasTablasDeLaVersion1(base);
        ejecutar(base, "insert into profesor (id, codigo, nombre_completo) values (2, 'LEN1', 'Dos')");
        if (desde >= 3) {
            try (Connection conexion = BancoDeCursos.conectar(base)) {
                ScriptUtils.executeSqlScript(conexion, migracionReal(2));
                ScriptUtils.executeSqlScript(conexion, migracionReal(3));
                if (desde >= 4) {
                    ScriptUtils.executeSqlScript(conexion, migracionReal(4));
                }
                if (desde >= 5) {
                    ScriptUtils.executeSqlScript(conexion, migracionReal(5));
                }
                if (desde >= 6) {
                    ScriptUtils.executeSqlScript(conexion, migracionReal(6));
                }
                if (desde >= 7) {
                    ScriptUtils.executeSqlScript(conexion, migracionReal(7));
                }
                if (desde == 8) {
                    ScriptUtils.executeSqlScript(conexion, migracionReal(8));
                }
            }
        }
        if (desde >= 4) {
            ejecutar(base, "insert into asignatura_aula (asignatura_id, aula_id, rol) values (1, 1, 'EXCLUSIVA')");
        }
        ejecutar(base, "PRAGMA user_version = " + desde);
        List<String> tablas = consulta(base,
                "select name from sqlite_master where type = 'table' and name not like 'sqlite_%' order by name");
        List<String> filasAntes = new ArrayList<>();
        for (String tabla : tablas) {
            filasAntes.add(tabla + "=" + BancoDeCursos.filas(base, tabla));
        }
        assertThat(version(base)).as("precondición: versión %d", desde).isEqualTo(desde);
        if (desde < 4) {
            assertThat(tablas).as("precondición: sin la tabla nueva").doesNotContain("asignatura_aula");
        }
        assertThat(consulta(base, "select count(*) from pragma_table_info('profesor')"
                        + " where name = 'guardias_ordinarias'"))
                .as("precondición: con la columna de guardias solo desde la 5")
                .containsExactly(desde >= 5 ? "1" : "0");
        assertThat(consulta(base, "select count(*) from pragma_table_info('profesor')"
                        + " where name = 'guardias_recreo'"))
                .as("precondición: con la columna de guardias de recreo solo desde la 7")
                .containsExactly(desde >= 7 ? "1" : "0");
        if (desde < 6) {
            assertThat(tablas).as("precondición: sin la tabla de guardias").doesNotContain("guardia");
        }
        assertThat(filasAntes)
                .as("precondición: ninguna tabla vacía, salvo guardia desde la 6")
                .noneMatch(f -> f.endsWith("=0") && !(desde >= 6 && f.equals("guardia=0")));
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("precondición: sin huérfanos").isEmpty();
        Path vacia = carpeta.resolve("vacia.db");
        real().preparar(origen(vacia));

        real().preparar(origen(base));

        assertThat(version(base)).isEqualTo(9);
        assertThat(maestro(base)).as("sqlite_master, el de una base nueva").isEqualTo(maestro(vacia));
        List<String> filasDespues = new ArrayList<>();
        for (String tabla : tablas) {
            filasDespues.add(tabla + "=" + BancoDeCursos.filas(base, tabla));
        }
        assertThat(filasDespues).as("las filas de cada tabla que ya existía").isEqualTo(filasAntes);
        assertThat(consulta(base, "select count(*), sum(en_uso = 1) from aula"))
                .as("todas las aulas, en uso")
                .containsExactly("2|2");
        assertThat(consulta(base, "select count(*) from grupo_administrativo where aula_referencia_id is not null"))
                .as("ningún grupo con aula de referencia")
                .containsExactly("0");
        assertThat(consulta(base, "select count(*) from subgrupo where alumnos is not null"))
                .as("ningún subgrupo con alumnos")
                .containsExactly("0");
        assertThat(BancoDeCursos.filas(base, "asignatura_aula"))
                .as("asignatura_aula, como estaba")
                .isEqualTo(desde >= 4 ? 1 : 0);
        assertThat(consulta(base, "select count(*), sum(guardias_ordinarias = 0) from profesor"))
                .as("los dos profesores, con 0 guardias")
                .containsExactly("2|2");
        assertThat(consulta(base, "select count(*), sum(guardias_recreo = 0) from profesor"))
                .as("los dos profesores, con 0 guardias de recreo")
                .containsExactly("2|2");
        assertThat(indicesUnicos(base, "sesion")).contains("uk_sesion_horario_plaza_indice");
        assertThat(indicesUnicos(base, "sesion_bloqueada")).contains("uk_sesion_bloqueada_actividad_indice");
        assertThat(indicesUnicos(base, "aula_bloqueada")).contains("uk_aula_bloqueada_actividad_indice_plaza");
        assertThat(indicesUnicos(base, "asignatura_aula_compatible"))
                .contains("uk_asignatura_aula_compatible_asignatura_tipo");
        assertThat(BancoDeCursos.filas(base, "guardia")).as("guardia, vacía").isZero();
        assertThat(indicesUnicos(base, "guardia")).contains("uk_guardia_horario_profesor_tramo");
        assertThat(consulta(base, "select codigo || '|' || codigo_corto || '|' || nivel_id from asignatura"))
                .as("BIO, de un solo nivel, con su nivel y el código compuesto")
                .containsExactly("BIO-ESO3|BIO|1");
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("ningún huérfano").isEmpty();
    }

    /**
     * (S219, T-f) Una base de la versión 6 con profesores que ya tienen guardias ordinarias llega a la
     * vigente (la 9 desde S250) con las guardias de recreo a 0 en todas las filas y las ordinarias intactas: 3 y 0, no 0 y 0,
     * para que un {@code 007.sql} que reescribiera la columna vecina caiga.
     */
    @Test
    void unaBaseDeLa6ConGuardiasLlegaALa9ConLasDeRecreoACero(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v6.db"), "2025/2026", false);
        poblarTodasLasTablasDeLaVersion1(base);
        ejecutar(base, "insert into profesor (id, codigo, nombre_completo) values (2, 'LEN1', 'Dos')");
        try (Connection conexion = BancoDeCursos.conectar(base)) {
            for (int n = 2; n <= 6; n++) {
                ScriptUtils.executeSqlScript(conexion, migracionReal(n));
            }
        }
        ejecutar(base, "update profesor set guardias_ordinarias = 3 where id = 1", "PRAGMA user_version = 6");
        assertThat(version(base)).as("precondición: versión 6").isEqualTo(6);

        real().preparar(origen(base));

        assertThat(version(base)).isEqualTo(9);
        assertThat(consulta(base, "select codigo || '|' || guardias_ordinarias || '|' || guardias_recreo"
                        + " from profesor order by id"))
                .as("ordinarias intactas y de recreo a 0")
                .containsExactly("MAT1|3|0", "LEN1|0|0");
    }

    /**
     * (S220) Una base de la versión 7 con guardias repartidas llega a la vigente (la 9 desde S250) con todas ellas como
     * ORDINARIA, que es lo que eran: un {@code 008.sql} con otro valor por defecto caería aquí. Y el
     * {@code check} de la columna nueva rechaza un tipo que no es de los tres.
     */
    @Test
    void unaBaseDeLa7ConGuardiasLlegaALa9ConTodasOrdinarias(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricarHistorica(carpeta.resolve("v7.db"), "2025/2026", false);
        poblarTodasLasTablasDeLaVersion1(base);
        ejecutar(base, "insert into profesor (id, codigo, nombre_completo) values (2, 'LEN1', 'Dos')");
        try (Connection conexion = BancoDeCursos.conectar(base)) {
            for (int n = 2; n <= 7; n++) {
                ScriptUtils.executeSqlScript(conexion, migracionReal(n));
            }
        }
        ejecutar(base,
                "insert into guardia (horario_id, profesor_id, tramo_id) values (1, 1, 1)",
                "insert into guardia (horario_id, profesor_id, tramo_id) values (1, 2, 2)",
                "PRAGMA user_version = 7");
        assertThat(version(base)).as("precondición: versión 7").isEqualTo(7);

        real().preparar(origen(base));

        assertThat(version(base)).isEqualTo(9);
        assertThat(consulta(base, "select profesor_id || '|' || tramo_id || '|' || tipo from guardia order by id"))
                .as("las dos guardias, intactas y ORDINARIA")
                .containsExactly("1|1|ORDINARIA", "2|2|ORDINARIA");
        assertThatThrownBy(() -> ejecutar(base,
                        "insert into guardia (horario_id, profesor_id, tramo_id, tipo) values (1, 2, 1, 'OTRA')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("CHECK");
        ejecutar(base, "insert into guardia (horario_id, profesor_id, tramo_id, tipo) values (1, 2, 1, 'BIBLIOTECA')");
        assertThat(BancoDeCursos.filas(base, "guardia")).isEqualTo(3);
    }

    /**
     * (S213) Como mucho una guardia por profesor y tramo en un horario: lo garantiza la única de
     * {@code guardia} y no el diagnóstico. Una base nueva rechaza la segunda fila igual.
     */
    @Test
    void laUnicaDeGuardiaRechazaLaMismaGuardiaDosVeces(@TempDir Path carpeta) throws Exception {
        Path base = carpeta.resolve("vacia.db");
        real().preparar(origen(base));
        ejecutar(base,
                "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')",
                "insert into tramo_semanal (id, dia, hora_inicio, hora_fin, es_lectivo, orden)"
                        + " values (1, 'LUNES', 0, 1, 1, 1)",
                "insert into horario_generado (id, nombre, estado, estado_solver, fecha_generacion)"
                        + " values (1, 'H', 'BORRADOR', 'OPTIMAL', 0)",
                "insert into guardia (horario_id, profesor_id, tramo_id) values (1, 1, 1)");

        assertThatThrownBy(() -> ejecutar(base,
                        "insert into guardia (horario_id, profesor_id, tramo_id) values (1, 1, 1)"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("UNIQUE");
        assertThat(BancoDeCursos.filas(base, "guardia")).isOne();
    }

    // ──────────────────────────────── esquema 9: asignaturas partidas por nivel (S250)

    /**
     * (S250, b) BIO tiene dos plazas de A1 y una de B2 (más una mezclada y una sin subgrupos, que
     * no cuentan): la principal es A1 y conserva el id 1; nace una fila de B2. Las dos llevan el
     * código compuesto y el mismo corto y nombre.
     */
    @Test
    void partir_laPrincipalEsElNivelConMasPlazasYConservaElId(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select (id = 1), codigo, codigo_corto, nombre_completo, nivel_id"
                        + " from asignatura where codigo_corto = 'BIO' order by id"))
                .containsExactly("1|BIO-A1|BIO|Biología|1", "0|BIO-B2|BIO|Biología|2");
        assertThat(version(base)).isEqualTo(9);
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("ningún huérfano").isEmpty();
    }

    /**
     * (S250, c) Cada plaza pasa a la parte de su nivel; la mezclada (A1 y B2) y la que no tiene
     * subgrupos se quedan en la principal. Las de las demás asignaturas, en la suya.
     */
    @Test
    void partir_cadaPlazaEnLaParteDeSuNivel(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select p.codigo, a.codigo from plaza p"
                        + " join asignatura a on a.id = p.asignatura_id order by p.id"))
                .containsExactly(
                        "BIO-A-P1|BIO-A1", "BIO-A-P2|BIO-A1", "BIO-B-P1|BIO-B2",
                        "BIO-X-P1|BIO-A1", "BIO-X-P2|BIO-A1",
                        "TEC-B-P1|TEC-B2", "TEC-C-P1|TEC-C0", "GUA-1-P1|GUA", "LAT-A-P1|LAT-A1");
    }

    /**
     * (S250, d) Una actividad de un solo nivel pasa a la parte de ese nivel (BIO-B, TEC-B); la que
     * no tiene nivel —su plaza mezclada no cuenta y la otra no tiene subgrupos— se queda en la
     * principal (BIO-X).
     */
    @Test
    void partir_cadaActividadDeUnNivelEnSuParte(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select ac.codigo, a.codigo from actividad ac"
                        + " join asignatura a on a.id = ac.asignatura_id order by ac.id"))
                .containsExactly(
                        "BIO-A|BIO-A1", "BIO-B|BIO-B2", "BIO-X|BIO-A1",
                        "TEC-B|TEC-B2", "TEC-C|TEC-C0", "GUA-1|GUA", "LAT-A|LAT-A1");
    }

    /**
     * (S250, e) TEC tiene una plaza de B2 (orden 2, id 2) y otra de C0 (orden 0, id 3): en el empate
     * gana el menor orden, C0, aunque su id sea mayor. La principal (id 2) queda en C0.
     */
    @Test
    void partir_enEmpateGanaElNivelDeMenorOrden(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select id, codigo, nivel_id from asignatura where codigo_corto = 'TEC' order by id"))
                .containsExactly("2|TEC-C0|3", "7|TEC-B2|2");
    }

    /** (S250, f) Las reglas de aula de BIO, de las dos tablas, se copian a su parte nueva. */
    @Test
    void partir_lasReglasDeAulaSeCopianALaParteNueva(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select a.codigo, au.codigo, x.rol from asignatura_aula x"
                        + " join asignatura a on a.id = x.asignatura_id join aula au on au.id = x.aula_id"
                        + " order by a.codigo"))
                .containsExactly("BIO-A1|LAB|EXCLUSIVA", "BIO-B2|LAB|EXCLUSIVA");
        assertThat(consulta(base, "select a.codigo, x.tipo_aula from asignatura_aula_compatible x"
                        + " join asignatura a on a.id = x.asignatura_id order by a.codigo"))
                .containsExactly("BIO-A1|LAB_CIENCIAS", "BIO-B2|LAB_CIENCIAS");
    }

    /**
     * (S250, g y h) GUA solo tiene plazas sin subgrupos y SOLA no tiene plazas: las dos se quedan
     * sin nivel y con su código. LAT es de un solo nivel: gana el nivel y el código compuesto, y
     * no se parte. No nace ninguna fila más que las dos partes nuevas.
     */
    @Test
    void partir_sinPartesSeQuedaSinNivelYDeUnNivelSeCompone(@TempDir Path carpeta) throws Exception {
        Path base = basePartible(carpeta);

        real().preparar(origen(base));

        assertThat(consulta(base, "select codigo, codigo_corto, nivel_id from asignatura"
                        + " where codigo_corto in ('GUA', 'SOLA', 'LAT') order by id"))
                .containsExactly("GUA|GUA|null", "LAT-A1|LAT|1", "SOLA|SOLA|null");
        assertThat(BancoDeCursos.filas(base, "asignatura")).isEqualTo(7);
    }

    /**
     * (S250, i) Si un código compuesto choca con uno existente, la migración para sin tocar la
     * base: sigue en la 8, con su misma huella, y el mensaje nombra la comprobación que lo vio.
     * MAT tiene una plaza de A1 y ya existe una asignatura MAT-A1 sin nivel.
     */
    @Test
    void partir_unChoqueParaLaMigracionYNoTocaLaBase(@TempDir Path carpeta) throws Exception {
        Path base = baseEnLa8(carpeta.resolve("choque.db"));
        ejecutar(base,
                "insert into nivel (id, codigo, orden) values (1, 'A1', 1)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo) values (1, 'GA', 1, 'ORDINARIO')",
                "insert into subgrupo (id, codigo) values (1, 'SA')",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (1, 1)",
                "insert into asignatura (id, codigo, nombre_completo) values (1, 'MAT', 'Matemáticas')",
                "insert into asignatura (id, codigo, nombre_completo) values (2, 'MAT-A1', 'Sin nivel')",
                "insert into actividad (id, codigo, asignatura_id, duracion_tramos, repeticiones_por_semana,"
                        + " patron_temporal, requiere_tutor) values (1, 'MAT-A', 1, 1, 1, 'NEUTRA', 0)",
                "insert into plaza (id, codigo, actividad_id, asignatura_id) values (1, 'MAT-A-P1', 1, 1)",
                "insert into plaza_subgrupo (plaza_id, subgrupo_id) values (1, 1)");
        String huella = BancoDeCursos.huella(base);

        assertThatThrownBy(() -> real().preparar(origen(base)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("009.sql")
                .hasMessageContaining("codigo_con_nivel_repetido");

        assertThat(version(base)).as("se queda en la 8").isEqualTo(8);
        assertThat(BancoDeCursos.huella(base)).as("la base, intacta").isEqualTo(huella);
    }

    // ─────────────────────────────────────────────────────────────── andamio

    /** Una base en la 8 sin datos de catálogo: {@code 001.sql} y las migraciones reales de la 2 a la 8. */
    private static Path baseEnLa8(Path base) throws Exception {
        BancoDeCursos.fabricarHistorica(base, "2025/2026", false);
        try (Connection conexion = BancoDeCursos.conectar(base)) {
            for (int n = 2; n <= 8; n++) {
                ScriptUtils.executeSqlScript(conexion, migracionReal(n));
            }
        }
        ejecutar(base, "PRAGMA user_version = 8");
        return base;
    }

    /**
     * Una base en la 8 con asignaturas para partir (S250). Tres niveles: A1 (orden 1), B2 (orden 2)
     * y C0 (orden 0, id 3, para que el desempate por orden y por id den distinto). Un grupo y un
     * subgrupo por nivel, y SMIX en A1 y B2 a la vez.
     * <ul>
     *   <li>BIO: dos plazas de A1 (BIO-A), una de B2 (BIO-B), una mezclada y una sin subgrupos
     *       (las dos de BIO-X), una regla de aula y una de tipo;
     *   <li>TEC: una plaza de B2 (TEC-B) y una de C0 (TEC-C), empatadas;
     *   <li>GUA: una plaza sin subgrupos; LAT: una de A1; SOLA, ninguna.
     * </ul>
     */
    private static Path basePartible(Path carpeta) throws Exception {
        Path base = baseEnLa8(carpeta.resolve("partible.db"));
        List<String> ordenes = new ArrayList<>(List.of(
                "insert into aula (id, codigo, tipo) values (1, 'LAB', 'LAB_CIENCIAS')",
                "insert into nivel (id, codigo, orden) values (1, 'A1', 1)",
                "insert into nivel (id, codigo, orden) values (2, 'B2', 2)",
                "insert into nivel (id, codigo, orden) values (3, 'C0', 0)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo) values (1, 'GA', 1, 'ORDINARIO')",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo) values (2, 'GB', 2, 'ORDINARIO')",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo) values (3, 'GC', 3, 'ORDINARIO')",
                "insert into subgrupo (id, codigo) values (1, 'SA')",
                "insert into subgrupo (id, codigo) values (2, 'SB')",
                "insert into subgrupo (id, codigo) values (3, 'SC')",
                "insert into subgrupo (id, codigo) values (4, 'SMIX')",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (1, 1)",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (2, 2)",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (3, 3)",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (4, 1)",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (4, 2)",
                "insert into asignatura (id, codigo, nombre_completo) values (1, 'BIO', 'Biología')",
                "insert into asignatura (id, codigo, nombre_completo) values (2, 'TEC', 'Tecnología')",
                "insert into asignatura (id, codigo, nombre_completo) values (3, 'GUA', 'Guardia')",
                "insert into asignatura (id, codigo, nombre_completo) values (4, 'LAT', 'Latín')",
                "insert into asignatura (id, codigo, nombre_completo) values (5, 'SOLA', 'Sin plazas')",
                "insert into asignatura_aula (asignatura_id, aula_id, rol) values (1, 1, 'EXCLUSIVA')",
                "insert into asignatura_aula_compatible (id, asignatura_id, tipo_aula) values (1, 1, 'LAB_CIENCIAS')"));
        String[][] actividades = {
            {"1", "BIO-A", "1"}, {"2", "BIO-B", "1"}, {"3", "BIO-X", "1"},
            {"4", "TEC-B", "2"}, {"5", "TEC-C", "2"}, {"6", "GUA-1", "3"}, {"7", "LAT-A", "4"}};
        for (String[] a : actividades) {
            ordenes.add("insert into actividad (id, codigo, asignatura_id, duracion_tramos, repeticiones_por_semana,"
                    + " patron_temporal, requiere_tutor) values (" + a[0] + ", '" + a[1] + "', " + a[2]
                    + ", 1, 1, 'NEUTRA', 0)");
        }
        // id, código, actividad, asignatura y subgrupo (0 = sin subgrupos).
        String[][] plazas = {
            {"1", "BIO-A-P1", "1", "1", "1"}, {"2", "BIO-A-P2", "1", "1", "1"}, {"3", "BIO-B-P1", "2", "1", "2"},
            {"4", "BIO-X-P1", "3", "1", "4"}, {"5", "BIO-X-P2", "3", "1", "0"},
            {"6", "TEC-B-P1", "4", "2", "2"}, {"7", "TEC-C-P1", "5", "2", "3"},
            {"8", "GUA-1-P1", "6", "3", "0"}, {"9", "LAT-A-P1", "7", "4", "1"}};
        for (String[] p : plazas) {
            ordenes.add("insert into plaza (id, codigo, actividad_id, asignatura_id) values (" + p[0] + ", '"
                    + p[1] + "', " + p[2] + ", " + p[3] + ")");
            if (!p[4].equals("0")) {
                ordenes.add("insert into plaza_subgrupo (plaza_id, subgrupo_id) values (" + p[0] + ", " + p[4] + ")");
            }
        }
        ejecutar(base, ordenes.toArray(String[]::new));
        assertThat(consulta(base, "PRAGMA foreign_key_check")).as("precondición: sin huérfanos").isEmpty();
        return base;
    }

    /**
     * Una o dos filas coherentes en cada tabla de {@code 001.sql} (la de {@code curso} la pone
     * {@link BancoDeCursos#fabricarHistorica}). Las columnas son las de la versión 1: la sesión
     * lleva aula y la actividad no tiene tipo.
     */
    private static void poblarTodasLasTablasDeLaVersion1(Path base) throws SQLException {
        ejecutar(
                base,
                "insert into configuracion (clave, valor) values ('centro', 'IES')",
                "insert into aula (id, codigo, tipo, capacidad) values (1, 'A1', 'ORDINARIA', 30)",
                "insert into aula (id, codigo, tipo) values (2, 'LAB', 'LAB_CIENCIAS')",
                "insert into tramo_semanal (id, dia, orden, hora_inicio, hora_fin, es_lectivo)"
                        + " values (1, 'LUNES', 1, '08:00:00', '09:00:00', 1)",
                "insert into tramo_semanal (id, dia, orden, hora_inicio, hora_fin, es_lectivo,"
                        + " siguiente_inmediato_id) values (2, 'LUNES', 2, '09:00:00', '10:00:00', 1, null)",
                "insert into nivel (id, codigo, orden) values (1, 'ESO3', 3)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo, grupo_padre_id)"
                        + " values (1, '3A', 1, 'ORDINARIO', null)",
                "insert into grupo_administrativo (id, codigo, nivel_id, tipo, grupo_padre_id)"
                        + " values (2, '3ADI', 1, 'DIVERSIFICACION_PDC', 1)",
                "insert into subgrupo (id, codigo) values (1, '3A-Completo')",
                "insert into subgrupo_grupo (subgrupo_id, grupo_id) values (1, 1)",
                "insert into profesor (id, codigo, nombre_completo) values (1, 'MAT1', 'Uno')",
                "insert into profesor_tutoria (profesor_id, grupo_id, rol) values (1, 1, 'TUTOR_PRINCIPAL')",
                "insert into profesor_restriccion_horaria (id, profesor_id, tramo_id, tipo, peso)"
                        + " values (1, 1, 1, 'DURA', 0)",
                "insert into asignatura (id, codigo, nombre_completo) values (1, 'BIO', 'Biología')",
                "insert into asignatura_aula_compatible (id, asignatura_id, tipo_aula)"
                        + " values (1, 1, 'LAB_CIENCIAS')",
                "insert into actividad (id, codigo, asignatura_id, duracion_tramos,"
                        + " repeticiones_por_semana, patron_temporal, requiere_tutor)"
                        + " values (1, 'BIO-3A', 1, 1, 2, 'NEUTRA', 0)",
                "insert into plaza (id, codigo, actividad_id, asignatura_id, aula_fija_id)"
                        + " values (1, 'BIO-3A-P1', 1, 1, null)",
                "insert into plaza_aula_candidata (plaza_id, aula_id) values (1, 2)",
                "insert into plaza_profesor (plaza_id, profesor_id) values (1, 1)",
                "insert into plaza_subgrupo (plaza_id, subgrupo_id) values (1, 1)",
                "insert into sesion_bloqueada (id, actividad_id, indice, tramo_inicio_id) values (1, 1, 1, 1)",
                "insert into aula_bloqueada (id, actividad_id, indice, plaza_id, aula_id) values (1, 1, 1, 1, 2)",
                "insert into horario_generado (id, nombre, estado, estado_solver, fecha_generacion)"
                        + " values (1, 'H1', 'BORRADOR', 'OPTIMAL', '2026-10-07 10:00:00')",
                "insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id)"
                        + " values (1, 1, 1, 1, 1, 2)",
                "insert into sesion (id, horario_id, plaza_id, indice, tramo_inicio_id, aula_id)"
                        + " values (2, 1, 1, 2, 2, 2)");
    }

    /** Los nombres de los índices ÚNICOS de {@code tabla}, por {@code PRAGMA index_list}. */
    private static List<String> indicesUnicos(Path base, String tabla) throws SQLException {
        List<String> nombres = new ArrayList<>();
        try (Connection conexion = BancoDeCursos.conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery("PRAGMA index_list(" + tabla + ")")) {
            while (fila.next()) {
                if (fila.getInt("unique") == 1) {
                    nombres.add(fila.getString("name"));
                }
            }
        }
        return nombres;
    }

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
