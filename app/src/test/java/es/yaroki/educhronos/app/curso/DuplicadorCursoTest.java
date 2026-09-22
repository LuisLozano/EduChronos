package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

/**
 * Spec de {@link DuplicadorCurso} (O-curso, S159, fase A de C-duplicado-guarda).
 *
 * <p><b>Sobre bases FABRICADAS, no sobre los bancos del centro.</b> El origen se levanta
 * ejecutando {@code classpath:schema.sql} sentencia a sentencia en un {@code @TempDir} y
 * poblando una fila de {@code profesor} y una de cada tabla del horario. Así el spec no
 * depende de ningún fichero de datos ni puede tocarlo: los bancos son de solo lectura y un
 * test que los copiase acabaría, tarde o temprano, escribiendo en uno.
 *
 * <p><b>El {@code schema.sql} se parte quitando primero los comentarios.</b> Su cabecera
 * contiene tres {@code ;} dentro de líneas {@code --} (medido en S159), así que un
 * {@code split(";")} sobre el fichero entero partiría la primera sentencia por la mitad.
 *
 * <p>Lo que estos diez casos fijan, y que ningún otro sitio ve: que el curso nuevo hereda
 * el catálogo ENTERO y nada del horario, que el origen queda archivado conservando sus
 * datos, y que los seis rechazos ocurren ANTES de escribir un byte.
 */
class DuplicadorCursoTest {

    /** Nombre del curso que traen las bases fabricadas. */
    private static final String ACTUAL = "2025/2026";

    /** Nombre del curso que se pide crear. */
    private static final String NUEVO = "2026/2027";

    /** Fichero que le corresponde a {@link #NUEVO}. */
    private static final String FICHERO_NUEVO = "curso-2026-2027.db";

    /**
     * Lo que el llamador responde cuando SÍ queda algún curso activo en la carpeta, que es el
     * caso corriente y el que estos diez casos fijan. La decisión no es de esta clase desde
     * S160 (requisito (b)); quien la toma es {@code CursoService.sinNingunCursoActivo()}, y su
     * spec está en {@code CursoAperturaTest}. Aquí entra como constante con nombre para que se
     * lea qué se está diciendo y no un {@code false} suelto repetido siete veces.
     */
    private static final boolean NO_ARCHIVADO = false;

    private final DuplicadorCurso duplicador = new DuplicadorCurso();

    // ─────────────────────────────────────────────────────────────── qué hereda el curso nuevo

    /**
     * (1) El curso nuevo es el catálogo ENTERO y nada del horario. El aserto recorre las
     * tablas de la base, no una lista escrita a mano: una tabla nueva que el duplicado se
     * dejara atrás cae aquí sin que nadie tenga que acordarse de añadirla.
     */
    @Test
    void elCursoNuevoHeredaTodoElCatalogoYNadaDelHorario(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);

        Path destino = duplicador.duplicar(origen, null, NUEVO, null, NO_ARCHIVADO);

        assertThat(destino).isRegularFile().hasFileName(FICHERO_NUEVO);
        for (String tabla : tablas(origen)) {
            if (tabla.equals("curso") || DuplicadorCurso.TABLAS_DEL_HORARIO.contains(tabla)) {
                continue;
            }
            assertThat(diferencia(destino, origen, tabla))
                    .as("filas de %s que están en la copia y no en el origen", tabla)
                    .isZero();
            assertThat(diferencia(origen, destino, tabla))
                    .as("filas de %s que están en el origen y no en la copia", tabla)
                    .isZero();
        }
        for (String tabla : DuplicadorCurso.TABLAS_DEL_HORARIO) {
            assertThat(filas(destino, tabla)).as("%s de la copia", tabla).isZero();
            assertThat(filas(origen, tabla)).as("%s del origen", tabla).isOne();
        }
        assertThat(curso(destino)).isEqualTo(NUEVO + "|false");
    }

    /**
     * (2) El origen queda archivado y con sus datos intactos. Archivar es un cambio de
     * etiqueta, no una mudanza: si el duplicado se llevara los datos, un centro que duplica
     * por error perdería el curso en marcha.
     */
    @Test
    void elOrigenQuedaArchivadoYConservaSusDatos(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);

        duplicador.duplicar(origen, null, NUEVO, null, NO_ARCHIVADO);

        assertThat(curso(origen)).isEqualTo(ACTUAL + "|true");
        assertThat(filas(origen, "profesor")).isOne();
        for (String tabla : DuplicadorCurso.TABLAS_DEL_HORARIO) {
            assertThat(filas(origen, tabla)).as("%s del origen", tabla).isOne();
        }
    }

    // ─────────────────────────────────────────────────────────────── los seis rechazos

    /**
     * (3) Un nombre con guion no es un nombre de curso, y el rechazo ocurre ANTES de tocar
     * el disco: en la carpeta no aparece nada, ni el fichero nuevo ni el temporal.
     */
    @Test
    void nombreConFormaInvalidaSeRechazaSinCrearNada(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);

        RechazoCursoException e = rechazo(origen, null, "2026-2027", null);

        assertThat(e.causa()).isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
        assertThat(e.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e).hasMessageContaining("2026/2027").hasMessageContaining("consecutivos");
        assertThat(ficheros(carpeta)).containsExactly(origen.getFileName().toString());
    }

    /** (4) Dos años que no son consecutivos tienen la forma buena y no son un curso. */
    @Test
    void anosNoConsecutivosSeRechazan(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);

        RechazoCursoException e = rechazo(origen, null, "2026/2028", null);

        assertThat(e.causa()).isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
        assertThat(ficheros(carpeta)).containsExactly(origen.getFileName().toString());
    }

    /** (5) Duplicar el curso en sí mismo no es duplicar: es no haber entendido la pantalla. */
    @Test
    void elNombreNuevoIgualAlActualSeRechaza(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);

        RechazoCursoException e = rechazo(origen, null, ACTUAL, null);

        assertThat(e.causa()).isEqualTo(DuplicadorCurso.CURSO_YA_EXISTE);
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e).hasMessageContaining("ya es el actual");
    }

    /**
     * (6) Si el fichero del curso nuevo ya está, se rechaza y NO SE TOCA: el aserto es sobre
     * su md5, no sobre su existencia. Sobreescribirlo destruiría un curso entero.
     */
    @Test
    void siElDestinoExisteSeRechazaYNoSeToca(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);
        Path destino = carpeta.resolve(FICHERO_NUEVO);
        Files.writeString(destino, "no soy una base de datos", StandardCharsets.UTF_8);
        String antes = huella(destino);

        RechazoCursoException e = rechazo(origen, null, NUEVO, null);

        assertThat(e.causa()).isEqualTo(DuplicadorCurso.CURSO_YA_EXISTE);
        assertThat(e).hasMessageContaining("Ya existe un curso 2026/2027");
        assertThat(huella(destino)).as("el fichero que ya estaba, intacto").isEqualTo(antes);
    }

    /** (9) Un curso archivado es de solo lectura, y de él no sale un curso nuevo. */
    @Test
    void unOrigenArchivadoNoSeDuplica(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, true);

        RechazoCursoException e = rechazo(origen, null, NUEVO, null);

        assertThat(e.causa()).isEqualTo(DuplicadorCurso.CURSO_ARCHIVADO);
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e).hasMessageContaining("solo lectura");
        assertThat(ficheros(carpeta)).containsExactly(origen.getFileName().toString());
    }

    /**
     * (11, S160) Con {@code permitirArchivado}, un origen archivado SÍ se duplica: es la
     * salida del callejón del requisito (b) —un centro que archivó su único curso—. El curso
     * nuevo nace ACTIVO, que es el punto entero: si naciera archivado, el centro seguiría sin
     * poder trabajar y habría gastado un fichero en descubrirlo. El origen se queda archivado
     * como estaba.
     */
    @Test
    void conPermisoUnOrigenArchivadoSiSeDuplicaYElNuevoNaceActivo(@TempDir Path carpeta)
            throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, true);

        Path destino = duplicador.duplicar(origen, null, NUEVO, null, true);

        assertThat(destino).isRegularFile().hasFileName(FICHERO_NUEVO);
        assertThat(curso(destino)).as("el curso nuevo nace activo").isEqualTo(NUEVO + "|false");
        assertThat(curso(origen)).as("el origen sigue archivado").isEqualTo(ACTUAL + "|true");
    }

    // ─────────────────────────────────────────────────────────────── bordes

    /**
     * (7) Un temporal huérfano de una duplicación que murió no bloquea la siguiente: se
     * borra. Reutilizarlo sería peor —una copia a medias con el nombre bueno—, y dejarlo
     * dejaría el curso nuevo imposible de crear para siempre.
     */
    @Test
    void unTemporalHuerfanoNoBloqueaLaDuplicacion(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, ACTUAL, false);
        Path temporal = carpeta.resolve("." + FICHERO_NUEVO + ".tmp");
        Files.writeString(temporal, "restos de una copia muerta", StandardCharsets.UTF_8);

        Path destino = duplicador.duplicar(origen, null, NUEVO, null, NO_ARCHIVADO);

        assertThat(temporal).doesNotExist();
        assertThat(destino).isRegularFile();
        assertThat(curso(destino)).isEqualTo(NUEVO + "|false");
    }

    /**
     * (8) Una base de antes de S159 no tiene nombre de curso (condición 6). Duplicarla exige
     * decir cómo se llamaba, porque ese nombre es el que queda archivado y nadie más lo sabe.
     */
    @Test
    void unOrigenSinNombreExigeElNombreActualYLoConserva(@TempDir Path carpeta) throws Exception {
        Path origen = baseFabricada(carpeta, null, false);

        RechazoCursoException sinNombre = rechazo(origen, null, NUEVO, null);
        assertThat(sinNombre.causa()).isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
        assertThat(sinNombre).hasMessageContaining("no tiene nombre");
        assertThat(ficheros(carpeta)).containsExactly(origen.getFileName().toString());

        Path destino = duplicador.duplicar(origen, ACTUAL, NUEVO, null, NO_ARCHIVADO);

        assertThat(destino).isRegularFile();
        assertThat(curso(origen)).as("el origen queda archivado con el nombre aportado")
                .isEqualTo(ACTUAL + "|true");
        assertThat(curso(destino)).isEqualTo(NUEVO + "|false");
    }

    /**
     * (10) El puntero sólo se escribe si hay uno que escribir: con {@code null} —arranque con
     * URL explícita, condición 7— no aparece ningún fichero de puntero en la carpeta.
     */
    @Test
    void elPunteroSeEscribeSoloCuandoSePide(@TempDir Path carpeta) throws Exception {
        Path conPuntero = Files.createDirectory(carpeta.resolve("con"));
        Path origenConPuntero = baseFabricada(conPuntero, ACTUAL, false);
        Path puntero = conPuntero.resolve("curso-abierto");

        duplicador.duplicar(origenConPuntero, null, NUEVO, puntero, NO_ARCHIVADO);

        assertThat(puntero).isRegularFile();
        assertThat(Files.readString(puntero)).isEqualTo(FICHERO_NUEVO);

        Path sinPuntero = Files.createDirectory(carpeta.resolve("sin"));
        Path origenSinPuntero = baseFabricada(sinPuntero, ACTUAL, false);

        duplicador.duplicar(origenSinPuntero, null, NUEVO, null, NO_ARCHIVADO);

        assertThat(ficheros(sinPuntero))
                .as("sólo las dos bases: ningún puntero")
                .containsExactlyInAnyOrder("educhronos.db", FICHERO_NUEVO);
    }

    // ─────────────────────────────────────────────────────────────── andamio

    /** Levanta una base con el esquema real, una fila de catálogo y una de cada tabla del horario. */
    private Path baseFabricada(Path carpeta, String nombreCurso, boolean archivado)
            throws Exception {
        Path base = carpeta.resolve("educhronos.db");
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement()) {
            for (String orden : sentenciasDelEsquema()) {
                sentencia.executeUpdate(orden);
            }
            // Las FK vienen apagadas en una conexión suelta, que es lo que permite poblar el
            // horario sin arrastrar aulas, plazas ni tramos.
            sentencia.executeUpdate(
                    "insert into profesor (id, codigo, nombre_completo) values (1, 'P1', 'Uno')");
            sentencia.executeUpdate(
                    "insert into horario_generado (id, fecha_generacion, estado, estado_solver,"
                            + " nombre) values (1, '2026-01-01 00:00:00', 'BORRADOR', 'OPTIMAL',"
                            + " 'H1')");
            sentencia.executeUpdate(
                    "insert into sesion (id, indice, aula_id, horario_id, plaza_id,"
                            + " tramo_inicio_id) values (1, 0, 1, 1, 1, 1)");
            sentencia.executeUpdate(
                    "insert into sesion_bloqueada (id, indice, actividad_id, tramo_inicio_id)"
                            + " values (1, 0, 1, 1)");
            sentencia.executeUpdate(
                    "insert into aula_bloqueada (id, indice, actividad_id, aula_id, plaza_id)"
                            + " values (1, 0, 1, 1, 1)");
            if (nombreCurso != null) {
                sentencia.executeUpdate(
                        "insert into curso (id, nombre, archivado) values (1, '" + nombreCurso
                                + "', " + (archivado ? 1 : 0) + ")");
            }
        }
        return base;
    }

    /**
     * Las sentencias de {@code schema.sql}, sin los comentarios. El filtro es obligatorio:
     * la cabecera lleva {@code ;} dentro de líneas {@code --}.
     */
    private static List<String> sentenciasDelEsquema() throws IOException {
        try (InputStream entrada = DuplicadorCursoTest.class.getResourceAsStream("/schema.sql")) {
            String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            String sinComentarios =
                    texto.lines()
                            .filter(linea -> !linea.strip().startsWith("--"))
                            .reduce("", (a, b) -> a + "\n" + b);
            List<String> ordenes = new ArrayList<>();
            for (String trozo : sinComentarios.split(";")) {
                if (!trozo.isBlank()) {
                    ordenes.add(trozo.strip());
                }
            }
            assertThat(ordenes).as("el esquema trae sus 22 tablas").hasSize(22);
            return ordenes;
        }
    }

    private static Connection conectar(Path base) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + base.toAbsolutePath());
    }

    private static List<String> tablas(Path base) throws SQLException {
        List<String> nombres = new ArrayList<>();
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas =
                        sentencia.executeQuery(
                                "select name from sqlite_master where type = 'table'"
                                        + " and name not like 'sqlite_%' order by name")) {
            while (filas.next()) {
                nombres.add(filas.getString(1));
            }
        }
        assertThat(nombres).as("las 22 tablas del esquema").hasSize(22);
        return nombres;
    }

    /** Filas de {@code tabla} que están en {@code una} y no en {@code otra}. */
    private static int diferencia(Path una, Path otra, String tabla) throws SQLException {
        try (Connection conexion = conectar(una);
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("attach '" + otra.toAbsolutePath() + "' as otra");
            try (ResultSet filas =
                    sentencia.executeQuery(
                            "select count(*) from (select * from main." + tabla
                                    + " except select * from otra." + tabla + ")")) {
                filas.next();
                return filas.getInt(1);
            }
        }
    }

    private static int filas(Path base, String tabla) throws SQLException {
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("select count(*) from " + tabla)) {
            filas.next();
            return filas.getInt(1);
        }
    }

    /** La fila única de curso como {@code nombre|archivado}, o {@code (sin fila)}. */
    private static String curso(Path base) throws SQLException {
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas =
                        sentencia.executeQuery("select nombre, archivado from curso where id = 1")) {
            if (!filas.next()) {
                return "(sin fila)";
            }
            return filas.getString(1) + "|" + filas.getBoolean(2);
        }
    }

    private static List<String> ficheros(Path carpeta) throws IOException {
        try (var listado = Files.list(carpeta)) {
            return listado.map(f -> f.getFileName().toString()).sorted().toList();
        }
    }

    private static String huella(Path fichero) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(fichero)));
    }

    /**
     * Ejecuta la duplicación esperando un rechazo PREVISTO y lo devuelve. Con try/catch y no
     * con {@code catchThrowableOfType}, cuyo orden de argumentos cambió entre versiones de
     * AssertJ: aquí lo que se mide es la causa, no la biblioteca.
     */
    private RechazoCursoException rechazo(
            Path origen, String nombreActual, String nombreNuevo, Path puntero) {
        try {
            duplicador.duplicar(origen, nombreActual, nombreNuevo, puntero, NO_ARCHIVADO);
        } catch (RechazoCursoException e) {
            return e;
        }
        return fail("se esperaba un RechazoCursoException y la duplicación terminó bien");
    }
}
