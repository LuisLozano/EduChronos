package es.yaroki.educhronos.app.curso;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Duplica el curso abierto en un fichero nuevo y archiva el de origen (O-curso, S159,
 * fase A de C-duplicado-guarda).
 *
 * <p><b>Sin Spring y sin JPA, a propósito.</b> Va por JDBC directo con
 * {@link DriverManager}, fuera del pool y fuera del {@code EntityManager}, por tres razones
 * medidas:
 *
 * <ul>
 *   <li><b>Nunca {@code @Transactional}.</b> {@code VACUUM INTO} falla dentro de una
 *       transacción: medido en el M0 de S159 con sqlite-jdbc 3.53.2.0,
 *       {@code [SQLITE_ERROR] cannot VACUUM from within a transaction}. No es una
 *       degradación, es un error, así que la copia no puede colgar de un método
 *       transaccional ni de una conexión que traiga una transacción abierta.
 *   <li>Las conexiones del pool nacen con {@code PRAGMA foreign_keys=ON} (ver
 *       {@code SqliteForeignKeysConfig}), y el vaciado del horario en la copia necesita las
 *       FK APAGADAS. Una conexión de {@link DriverManager} las trae apagadas por defecto,
 *       que es el comportamiento de SQLite.
 *   <li>El destino y el temporal son ficheros que no están en el {@code Environment}: el
 *       pool no sabe abrirlos.
 * </ul>
 *
 * <p><b>Coste.</b> El {@code VACUUM INTO} de los dos bancos del centro real tarda 2 ms
 * (medido en S159) y produce una copia bit a bit consistente: las 21 tablas con
 * {@code EXCEPT} cero en los dos sentidos e {@code integrity_check ok}. Una transacción de
 * escritura ajena sin confirmar no lo bloquea y su contenido no entra en la copia.
 *
 * <p><b>El orden de los pasos es la decisión de diseño, y tiene dos ventanas de corte.</b>
 * El puntero se escribe ANTES de archivar el origen:
 *
 * <ul>
 *   <li>Un corte entre {@code d} (el {@code move} del destino) y {@code e}/{@code f} deja el
 *       fichero nuevo creado y el origen SIN archivar. El reintento con el mismo nombre
 *       responde «ya existe un curso X» y no destruye nada: el usuario ve el nombre ocupado
 *       y sabe dónde está.
 *   <li>Un corte entre {@code e} y {@code f} deja el puntero apuntando al curso nuevo y el
 *       origen sin archivar. Al reabrir se entra en el curso nuevo, que es lo que el usuario
 *       pidió; el viejo queda escribible, que es el defecto benigno. Al revés —archivar
 *       primero y morir antes de escribir el puntero— se abriría un curso archivado y de
 *       solo lectura sin que nadie lo hubiera pedido, y eso deja al centro sin poder
 *       trabajar.
 * </ul>
 *
 * <p><b>Ventana de escrituras.</b> Entre {@code b} (la copia) y {@code f} (el archivado del
 * origen) la aplicación sigue aceptando escrituras, y las que entren en ese hueco se quedan
 * en el curso viejo sin llegar al nuevo. Es una ventana de milisegundos y la cierra la fase
 * B de C-duplicado-guarda, que es la que pone la guarda de solo lectura; aquí se deja
 * escrita y no se finge resuelta.
 */
public class DuplicadorCurso {

    /** El curso de origen está archivado: es de solo lectura y no se duplica. */
    public static final String CURSO_ARCHIVADO = "CURSO_ARCHIVADO";

    /** Un nombre de curso no tiene la forma {@code 2026/2027} o falta. */
    public static final String NOMBRE_INVALIDO = "NOMBRE_INVALIDO";

    /** El curso que se pide crear ya es el actual, o su fichero ya está en la carpeta. */
    public static final String CURSO_YA_EXISTE = "CURSO_YA_EXISTE";

    /** Prefijo de la URL JDBC que exige el driver de SQLite. */
    static final String PREFIJO_URL = "jdbc:sqlite:";

    /**
     * Espera máxima por un candado de otro proceso, en milisegundos. Sin esto un
     * {@code SQLITE_BUSY} instantáneo abortaría la duplicación por una escritura que iba a
     * terminar en el mismo milisegundo.
     */
    static final int ESPERA_OCUPADO_MS = 5000;

    /**
     * Tablas del horario, en orden de borrado (de la hoja a la raíz). El curso nuevo hereda
     * el catálogo y NO el horario: un horario generado pertenece al curso en que se generó.
     */
    static final List<String> TABLAS_DEL_HORARIO =
            List.of("sesion_bloqueada", "aula_bloqueada", "sesion", "horario_generado");

    /**
     * Duplica el curso de {@code origen} en un fichero nuevo de su misma carpeta y archiva
     * el origen.
     *
     * @param origen fichero de la base abierta, que tiene que existir
     * @param nombreActual nombre que el cliente dice que tiene el curso actual; sólo hace
     *     falta cuando el origen no lo trae escrito, y puede ser {@code null}
     * @param nombreNuevo nombre del curso a crear, con la forma {@code 2026/2027}
     * @param punteroONull fichero de puntero a reescribir, o {@code null} para no escribir
     *     ninguno (arranque con URL explícita, condición 7)
     * @return la ruta del fichero creado
     * @throws RechazoCursoException si la operación se rechaza por una razón prevista
     */
    public Path duplicar(Path origen, String nombreActual, String nombreNuevo, Path punteroONull) {
        Path carpeta = origen.toAbsolutePath().getParent();

        // a) Identidad del origen y todos los rechazos, ANTES de escribir un solo byte.
        Optional<FilaCurso> fila = leerCurso(origen);
        if (fila.map(FilaCurso::archivado).orElse(false)) {
            throw new RechazoCursoException(
                    HttpStatus.CONFLICT, CURSO_ARCHIVADO,
                    "Este curso está archivado y es de solo lectura; no se puede duplicar.");
        }
        String nombreOrigen = nombreEfectivoDelOrigen(fila, nombreActual);
        if (!NombreCurso.valido(nombreNuevo)) {
            throw new RechazoCursoException(HttpStatus.BAD_REQUEST, NOMBRE_INVALIDO, FORMA_ESPERADA);
        }
        if (nombreNuevo.equals(nombreOrigen)) {
            throw new RechazoCursoException(
                    HttpStatus.CONFLICT, CURSO_YA_EXISTE,
                    "El curso " + nombreNuevo + " ya es el actual.");
        }
        Path destino = carpeta.resolve(NombreCurso.fichero(nombreNuevo));
        if (Files.exists(destino)) {
            throw new RechazoCursoException(
                    HttpStatus.CONFLICT, CURSO_YA_EXISTE,
                    "Ya existe un curso " + nombreNuevo + " en la carpeta de datos.");
        }

        // b) La copia se hace sobre un temporal oculto de la MISMA carpeta: así el move del
        // paso d es atómico (mismo sistema de ficheros) y un corte antes de él no deja
        // ningún fichero con nombre de curso a medio escribir.
        Path temporal = carpeta.resolve("." + NombreCurso.fichero(nombreNuevo) + ".tmp");
        borrar(temporal);
        copiar(origen, temporal);

        // c) El curso nuevo: sin horario y con su propia identidad.
        prepararCopia(temporal, nombreNuevo);

        // d) Publicación del fichero nuevo.
        mover(temporal, destino);

        // e) El puntero, ANTES de archivar (ver la nota de clase).
        if (punteroONull != null) {
            escribirPuntero(punteroONull, destino.getFileName().toString());
        }

        // f) El origen pasa a archivado, conservando su nombre.
        escribirCurso(origen, nombreOrigen, true);
        return destino;
    }

    /** Texto único del rechazo por forma: el usuario necesita ver la forma, no la regla. */
    private static final String FORMA_ESPERADA =
            "El nombre del curso debe tener la forma 2026/2027, con dos años consecutivos.";

    /**
     * El nombre que el origen tiene o el que el cliente aporta. Un origen sin fila no es un
     * error: es una base de antes de S159 (condición 6). Lo que sí es un error es no saber
     * cómo se llama, porque ese nombre es el que se queda archivado.
     */
    private String nombreEfectivoDelOrigen(Optional<FilaCurso> fila, String nombreActual) {
        Optional<String> escrito = fila.map(FilaCurso::nombre);
        if (escrito.isPresent()) {
            String nombre = escrito.get();
            if (nombreActual != null && !nombreActual.equals(nombre)) {
                throw new RechazoCursoException(
                        HttpStatus.BAD_REQUEST, NOMBRE_INVALIDO,
                        "El curso actual ya se llama " + nombre + ".");
            }
            return nombre;
        }
        if (nombreActual == null) {
            throw new RechazoCursoException(
                    HttpStatus.BAD_REQUEST, NOMBRE_INVALIDO,
                    "El curso actual no tiene nombre: indícalo junto al del curso nuevo.");
        }
        if (!NombreCurso.valido(nombreActual)) {
            throw new RechazoCursoException(HttpStatus.BAD_REQUEST, NOMBRE_INVALIDO, FORMA_ESPERADA);
        }
        return nombreActual;
    }

    /** La fila única de {@code curso}, si la base la tiene. */
    private Optional<FilaCurso> leerCurso(Path base) {
        try (Connection conexion = abrir(base);
                Statement sentencia = conexion.createStatement();
                ResultSet filas =
                        sentencia.executeQuery(
                                "select nombre, archivado from curso where id = " + Curso.ID)) {
            if (!filas.next()) {
                return Optional.empty();
            }
            return Optional.of(new FilaCurso(filas.getString(1), filas.getBoolean(2)));
        } catch (SQLException e) {
            throw new IllegalStateException("No se puede leer el curso de " + base, e);
        }
    }

    /**
     * {@code VACUUM INTO}: una copia consistente sin parar la aplicación y sin copiar el
     * fichero a mano, que con un WAL o un journal a medias daría una base corrupta.
     */
    private void copiar(Path origen, Path temporal) {
        try (Connection conexion = abrir(origen);
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("VACUUM INTO '" + temporal.toAbsolutePath() + "'");
        } catch (SQLException e) {
            throw new IllegalStateException("No se puede copiar " + origen + " en " + temporal, e);
        }
    }

    /** Vacía el horario de la copia y le pone su identidad. */
    private void prepararCopia(Path copia, String nombreNuevo) {
        try (Connection conexion = abrir(copia);
                Statement sentencia = conexion.createStatement()) {
            for (String tabla : TABLAS_DEL_HORARIO) {
                sentencia.executeUpdate("delete from " + tabla);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se puede vaciar el horario de " + copia, e);
        }
        escribirCurso(copia, nombreNuevo, false);
    }

    /**
     * Upsert de la fila única. {@code insert or replace} y no {@code insert}: la copia llega
     * con la fila del origen ya dentro —{@code VACUUM INTO} copia todo— y el origen puede
     * traerla o no.
     */
    private void escribirCurso(Path base, String nombre, boolean archivado) {
        try (Connection conexion = abrir(base);
                PreparedStatement sentencia =
                        conexion.prepareStatement(
                                "insert or replace into curso (id, nombre, archivado)"
                                        + " values (?, ?, ?)")) {
            sentencia.setInt(1, Curso.ID);
            sentencia.setString(2, nombre);
            sentencia.setBoolean(3, archivado);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se puede escribir el curso de " + base, e);
        }
    }

    /**
     * Conexión suelta, en autocommit y con espera por ocupado. Autocommit es obligatorio:
     * {@code VACUUM INTO} no corre dentro de una transacción.
     */
    private Connection abrir(Path base) throws SQLException {
        Connection conexion = DriverManager.getConnection(PREFIJO_URL + base.toAbsolutePath());
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("PRAGMA busy_timeout=" + ESPERA_OCUPADO_MS);
        }
        return conexion;
    }

    /**
     * El puntero guarda el NOMBRE del fichero, no su ruta: así la carpeta de datos se puede
     * mover o montar en otro sitio sin que el puntero apunte al vacío. Se escribe en un
     * temporal y se mueve encima, para que un corte no deje un puntero a medias que el
     * arranque siguiente leería como roto.
     */
    private void escribirPuntero(Path puntero, String nombreDeFichero) {
        Path temporal = puntero.resolveSibling("." + puntero.getFileName() + ".tmp");
        try {
            Files.writeString(temporal, nombreDeFichero, StandardCharsets.UTF_8);
            Files.move(
                    temporal, puntero,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede escribir el puntero " + puntero, e);
        }
    }

    private void mover(Path temporal, Path destino) {
        try {
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede publicar " + destino, e);
        }
    }

    /**
     * Borra el temporal si una duplicación anterior murió dejándolo. No se reutiliza: una
     * copia a medias con el nombre correcto es peor que no tener nada.
     */
    private void borrar(Path temporal) {
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede borrar el temporal " + temporal, e);
        }
    }

    /** La fila única de {@code curso}, tal como está en el fichero. */
    private record FilaCurso(String nombre, boolean archivado) {}
}
