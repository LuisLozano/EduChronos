package es.yaroki.educhronos.app.config;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Deja una base en el esquema de esta versión, o la rechaza (O-base-tecnica, S191,
 * C-esquema-versionado, condición 1).
 *
 * <p>La versión del esquema vive en la propia base, en {@code PRAGMA user_version}. Hasta
 * S191 nadie la escribía: valía 0 en todas las bases, y {@code schema.sql} se pasaba en cada
 * arranque confiando en que sus {@code create table if not exists} bastaran. No bastan para
 * nada que no sea añadir una tabla (medido en el M2 de S190: {@code 6a3a3f9} y
 * {@code d301b64} cambiaron tablas que ya existían, y sobre una base vieja no habrían
 * llegado).
 *
 * <p><b>Las cinco reglas</b>, en el orden en que se comprueban, todas sobre UNA conexión:
 *
 * <ol>
 *   <li>Si la base dice un número MAYOR que el de esta versión, se rechaza con
 *       {@link EsquemaPosteriorException} sin ejecutar ninguna otra sentencia: la base queda
 *       intacta.
 *   <li>Si la base está vacía —ninguna tabla que no sea interna de SQLite—, se crea con el
 *       esquema vigente ({@code schema.sql}) y se sella con el número de esta versión, en una
 *       sola transacción.
 *   <li>Si dice 0 y no está vacía, es una base anterior a S191: se le pasa
 *       {@code esquema/001.sql} y se sella como 1, en una sola transacción.
 *   <li>Desde ahí, cada {@code esquema/NNN.sql} pendiente, uno por uno, cada uno en su
 *       transacción junto con su sello. Si falta el fichero de un número, se para nombrándolo.
 *   <li>Cualquier fallo deshace la transacción en curso y sube con el nombre del script: la
 *       base se queda en el último número que se completó entero.
 * </ol>
 *
 * <p><b>Por qué 0 quiere decir «versión 1» y no «la actual».</b> Un 0 sólo dice que la base
 * es anterior a S191, no a qué esquema llegó: puede ser de antes de S159 (sin tabla
 * {@code curso}) o de {@code v0.2.0}. Tratarla como «la actual» la sellaría con un número
 * cuyas migraciones nunca ha pasado, y la primera migración que dependiera de ellas fallaría
 * sobre esa base y sólo sobre esa. {@code 001.sql} es la foto de S191, con sus
 * {@code if not exists}: lleva cualquier base vieja a la versión 1 sin tocar lo que ya tiene.
 *
 * <p><b>Regla para quien cambie el esquema.</b> Se edita {@code schema.sql} —que es lo que
 * reciben las bases nuevas—, se añade {@code esquema/NNN.sql} con el paso desde la versión
 * anterior —que es lo que reciben las bases existentes— y se sube {@link #VERSION_ESQUEMA} a
 * NNN. {@code 001.sql} NO se toca nunca: es lo que reciben las bases sin número, y cambiarlo
 * cambiaría qué quiere decir «versión 1» para las que ya la tienen.
 *
 * <p><b>Migraciones que reconstruyen una tabla.</b> En SQLite, cambiar una columna obliga a crear la tabla de
 * nuevo y copiar. Cabe dentro de la transacción de este preparador cuando ninguna otra tabla apunta a la
 * reconstruida: se renombra primero la vieja, se crea la nueva con su DDL literal, se copia y se borra la vieja
 * (así lo hace {@code esquema/002.sql} con {@code sesion}). El orden inverso —crear, borrar y renombrar la
 * nueva— deja el nombre entrecomillado en {@code sqlite_master}, y la base migrada ya no coincide con una
 * nueva. Si alguna tabla apuntara a la reconstruida, haría falta {@code PRAGMA foreign_keys} apagado, que
 * SQLite ignora dentro de una transacción: esa migración tendría que resolverlo antes. Las claves foráneas no
 * se dan por encendidas, porque en el cambio de curso la conexión llega sin ellas. Por eso cada migración
 * comprueba al terminar que no deja filas huérfanas nuevas.
 */
public class PreparadorEsquema {

    private static final Logger LOG = LoggerFactory.getLogger(PreparadorEsquema.class);

    /** El esquema que escribe esta versión. Se sube con cada {@code esquema/NNN.sql} nuevo. */
    public static final int VERSION_ESQUEMA = 3;

    /** Lo que reciben las bases vacías. */
    public static final String ESQUEMA_VIGENTE = "classpath:schema.sql";

    /** Carpeta de las migraciones numeradas: {@code 001.sql}, {@code 002.sql}… */
    public static final String MIGRACIONES = "classpath:esquema/";

    private final ResourceLoader cargador;

    private final String esquemaVigente;

    private final String migraciones;

    private final int version;

    /**
     * @param cargador resuelve las ubicaciones, también dentro del jar
     * @param esquemaVigente ubicación del script de las bases vacías
     * @param migraciones ubicación de la carpeta de {@code NNN.sql}, acabada en {@code /}
     * @param version el esquema que deja esta versión
     */
    public PreparadorEsquema(
            ResourceLoader cargador, String esquemaVigente, String migraciones, int version) {
        this.cargador = cargador;
        this.esquemaVigente = esquemaVigente;
        this.migraciones = migraciones;
        this.version = version;
    }

    /**
     * Aplica las cinco reglas sobre {@code base}, con una sola conexión.
     *
     * @throws EsquemaPosteriorException si la base es de una versión más nueva
     * @throws IllegalStateException si falta una migración, si un script falla o si la base
     *     no se puede leer
     */
    public void preparar(DataSource base) {
        try (Connection conexion = base.getConnection()) {
            String ruta = ruta(conexion);
            int actual = leerVersion(conexion);
            if (actual > version) {
                LOG.warn("Esquema posterior rechazado ruta={} versionBase={} versionAplicacion={}",
                        ruta, actual, version);
                throw new EsquemaPosteriorException(actual, version);
            }
            int inicial = actual;
            boolean vacia = estaVacia(conexion);
            boolean autocommit = conexion.getAutoCommit();
            try {
                if (vacia) {
                    aplicar(conexion, esquemaVigente, version);
                } else {
                    if (actual == 0) {
                        aplicar(conexion, migracion(1), 1);
                        actual = 1;
                    }
                    for (int n = actual + 1; n <= version; n++) {
                        aplicar(conexion, migracion(n), n);
                    }
                }
            } finally {
                conexion.setAutoCommit(autocommit);
            }
            // La versión final se LEE de la base, no se deduce de lo que se ha aplicado.
            int versionFinal = leerVersion(conexion);
            LOG.info("Esquema preparado ruta={} versionInicial={} versionFinal={} baseVacia={}",
                    ruta, inicial, versionFinal, vacia);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se ha podido preparar el esquema de la base: " + e.getMessage(), e);
        }
    }

    private String migracion(int numero) {
        return migraciones + String.format("%03d.sql", numero);
    }

    /**
     * Ejecuta un script y sella {@code numero} en la MISMA transacción: o queda todo, o no
     * queda nada y la base sigue en el número que tenía.
     */
    private void aplicar(Connection conexion, String ubicacion, int numero) throws SQLException {
        Resource script = cargador.getResource(ubicacion);
        if (!script.exists()) {
            throw new IllegalStateException(
                    "Falta la migración del esquema " + numero + ": " + ubicacion);
        }
        conexion.setAutoCommit(false);
        try {
            Set<String> huerfanasAntes = filasHuerfanas(conexion);
            ScriptUtils.executeSqlScript(
                    conexion, new EncodedResource(script, StandardCharsets.UTF_8));
            // Sólo cuentan las que ha dejado ESTE script: una base que ya trajera huérfanos de
            // antes tiene que poder migrar.
            Set<String> filasNuevas = filasHuerfanas(conexion);
            filasNuevas.removeAll(huerfanasAntes);
            if (!filasNuevas.isEmpty()) {
                throw new IllegalStateException("La migración deja filas huérfanas: " + filasNuevas);
            }
            try (Statement sentencia = conexion.createStatement()) {
                sentencia.execute("PRAGMA user_version = " + numero);
            }
            conexion.commit();
        } catch (SQLException | RuntimeException e) {
            deshacer(conexion, e);
            throw new IllegalStateException(
                    "No se ha podido aplicar " + ubicacion + " (esquema " + numero + "): "
                            + e.getMessage(),
                    e);
        }
    }

    /** Rollback que no tapa el fallo original: si también falla, va como suprimido. */
    private static void deshacer(Connection conexion, Exception original) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }

    /**
     * Las filas de {@code PRAGMA foreign_key_check}, cada una como {@code tabla|rowid|padre|fkid}.
     * El pragma no depende de que {@code foreign_keys} esté encendido: mide igual por las dos
     * puertas, el arranque y el cambio de curso.
     */
    private static Set<String> filasHuerfanas(Connection conexion) throws SQLException {
        Set<String> filas = new TreeSet<>();
        try (Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery("PRAGMA foreign_key_check")) {
            while (fila.next()) {
                filas.add(fila.getString(1) + "|" + fila.getString(2) + "|" + fila.getString(3)
                        + "|" + fila.getString(4));
            }
        }
        return filas;
    }

    /** El fichero de la base {@code main}, tal como lo dice {@code PRAGMA database_list}. */
    private static String ruta(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery("PRAGMA database_list")) {
            while (fila.next()) {
                if ("main".equals(fila.getString("name"))) {
                    return fila.getString("file");
                }
            }
        }
        return null;
    }

    private static int leerVersion(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet fila = sentencia.executeQuery("PRAGMA user_version")) {
            return fila.next() ? fila.getInt(1) : 0;
        }
    }

    /** Sin ninguna tabla propia: las de SQLite ({@code sqlite_*}) no cuentan. */
    private static boolean estaVacia(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet fila =
                        sentencia.executeQuery(
                                "select count(*) from sqlite_master where type = 'table'"
                                        + " and substr(name, 1, 7) <> 'sqlite_'")) {
            fila.next();
            return fila.getInt(1) == 0;
        }
    }
}
