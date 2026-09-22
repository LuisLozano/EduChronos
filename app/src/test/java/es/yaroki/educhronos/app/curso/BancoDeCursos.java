package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.config.BaseConmutable;
import es.yaroki.educhronos.app.config.FabricaDeBases;
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
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Andamio de los tests de curso de S160: fabricar bases, leerlas y montar una
 * {@link FabricaDeBases} de verdad fuera de Spring.
 *
 * <p><b>Por qué una clase aparte y no un método en cada spec.</b> Cinco ficheros de prueba
 * necesitan exactamente lo mismo —una base con el esquema real y una identidad de curso—, y
 * la alternativa era copiar el troceado de {@code schema.sql} en todos. Ese troceado tiene
 * una trampa medida en S159 (la cabecera del fichero lleva {@code ;} dentro de líneas
 * {@code --}), y una copia que se olvidara de filtrar los comentarios partiría la primera
 * sentencia por la mitad y fallaría de una forma que no se parece a su causa.
 *
 * <p>{@code DuplicadorCursoTest} NO usa esta clase: es de S159, pasa en verde y su propio
 * andamio es parte de lo que ese spec fija. Tocarlo para compartir código habría metido un
 * cambio sin medición en un fichero que no lo necesita.
 */
public final class BancoDeCursos {

    private BancoDeCursos() {}

    /**
     * Levanta una base con el esquema real y, si se pide, una fila de curso.
     *
     * @param base ruta del fichero a crear
     * @param nombreCurso nombre del curso, o {@code null} para una base sin fila (condición 6)
     * @param archivado si esa fila nace archivada
     */
    public static Path fabricar(Path base, String nombreCurso, boolean archivado) throws Exception {
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement()) {
            for (String orden : sentenciasDelEsquema()) {
                sentencia.executeUpdate(orden);
            }
            if (nombreCurso != null) {
                sentencia.executeUpdate(
                        "insert into curso (id, nombre, archivado) values (1, '"
                                + nombreCurso + "', " + (archivado ? 1 : 0) + ")");
            }
        }
        return base;
    }

    /**
     * Una {@link FabricaDeBases} igual que la del contexto, montada a mano.
     *
     * <p>Los dos {@code …Properties} son POJOs de configuración y se instancian con
     * {@code new}: lo que hacen es llevar valores por defecto, y los defectos son los mismos
     * que tiene la aplicación salvo el {@code spring.sql.init.mode}, que aquí no se consulta
     * porque {@code initializeDatabase()} se llama directamente.
     */
    public static FabricaDeBases fabrica() throws Exception {
        DataSourceProperties propiedades = new DataSourceProperties();
        propiedades.setDriverClassName("org.sqlite.JDBC");
        propiedades.setUrl(BaseConmutable.PREFIJO_URL + ":memory:");
        propiedades.setBeanClassLoader(BancoDeCursos.class.getClassLoader());
        propiedades.afterPropertiesSet();
        return new FabricaDeBases(
                propiedades, new SqlInitializationProperties(), new DefaultResourceLoader());
    }

    /** Una {@link BaseConmutable} abierta sobre {@code fichero}, con su pool recién hecho. */
    public static BaseConmutable conmutableSobre(FabricaDeBases fabrica, Path fichero) {
        return new BaseConmutable(fabrica.poolPara(fichero), fichero);
    }

    /** La fila única de curso como {@code nombre|archivado}, o {@code (sin fila)}. */
    public static String curso(Path base) throws SQLException {
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

    /** ¿Existe esa tabla en el fichero? Se pregunta a {@code sqlite_master}, no al esquema. */
    public static boolean tieneTabla(Path base, String tabla) throws SQLException {
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas =
                        sentencia.executeQuery(
                                "select count(*) from sqlite_master where type = 'table'"
                                        + " and name = '" + tabla + "'")) {
            filas.next();
            return filas.getInt(1) > 0;
        }
    }

    /** Filas de una tabla, por JDBC directo sobre el fichero y no por el pool. */
    public static int filas(Path base, String tabla) throws SQLException {
        try (Connection conexion = conectar(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("select count(*) from " + tabla)) {
            filas.next();
            return filas.getInt(1);
        }
    }

    /** El md5 de un fichero, para aseverar que NO se ha tocado. */
    public static String huella(Path fichero) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(fichero)));
    }

    public static Connection conectar(Path base) throws SQLException {
        return DriverManager.getConnection(BaseConmutable.PREFIJO_URL + base.toAbsolutePath());
    }

    /**
     * Las sentencias de {@code schema.sql}, sin los comentarios. El filtro es obligatorio: la
     * cabecera lleva {@code ;} dentro de líneas {@code --} (medido en S159).
     */
    private static List<String> sentenciasDelEsquema() throws IOException {
        try (InputStream entrada = BancoDeCursos.class.getResourceAsStream("/schema.sql")) {
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
}
