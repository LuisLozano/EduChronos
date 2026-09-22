package es.yaroki.educhronos.app.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

/**
 * El {@code DataSource} del proyecto: uno estable, que por debajo cambia de pool cuando se
 * cambia de curso (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>Por qué un pool sustituible y no un contexto nuevo.</b> Cambiar de curso es abrir
 * OTRO fichero SQLite. La alternativa —rehacer el contexto de Spring— tiene dos víctimas
 * medidas en el M2 de S160: el arranque de escritorio guarda el {@code ApplicationContext}
 * en el listener del menú de la bandeja, de modo que «Salir» cerraría un contexto muerto, y
 * el e2e arranca con {@code --spring.datasource.url}, donde el post-procesador no actúa.
 * Ninguna de las dos cosas tiene un solo test. Aquí, en cambio, lo único que cambia es a
 * quién delega este objeto.
 *
 * <p><b>Hereda de {@code ForeignKeysEnforcingDataSource}, y no es un detalle.</b> Es lo que
 * hace cierto el invariante I1 —{@code PRAGMA foreign_keys=ON} en TODA conexión, también
 * después de un cambio— sin escribir el pragma por segunda vez: el {@code getConnection()}
 * que se usa es el de la superclase, que lo enciende sobre lo que devuelva
 * {@link #getTargetDataSource()}, y eso es el pool VIGENTE. Además, el post-procesador de
 * {@link SqliteForeignKeysConfig} no la envuelve —su condición ya excluía lo que ya fuera un
 * {@code ForeignKeysEnforcingDataSource}, para no envolver dos veces—, así que el bean del
 * contexto es esta misma instancia y {@code CursoService} la puede inyectar por su tipo para
 * llamar a {@link #sustituir}.
 *
 * <p><b>El destino va en un {@link AtomicReference} y no en el campo de
 * {@code DelegatingDataSource}</b>, que no es volátil: quien sustituye es el hilo que atiende
 * el {@code POST /api/cursos/abrir} y quien lee son todos los hilos de Tomcat. El pool y el
 * fichero viajan JUNTOS en un solo registro porque son un solo hecho: un lector no puede ver
 * nunca el pool nuevo con el nombre viejo.
 *
 * <p>Sustituir NO cierra el pool saliente: lo devuelve. Cerrarlo es decisión de quien
 * cambia, que es el único que sabe si el cambio salió bien (ver I3 en {@code CursoService}).
 */
public class BaseConmutable extends SqliteForeignKeysConfig.ForeignKeysEnforcingDataSource
        implements Closeable {

    /** Prefijo de la URL JDBC de SQLite, el mismo literal que usa {@code DuplicadorCurso}. */
    public static final String PREFIJO_URL = "jdbc:sqlite:";

    /** El pool abierto y el fichero que sirve, indivisibles. */
    private record Abierta(HikariDataSource pool, Path fichero) {}

    private final AtomicReference<Abierta> abierta;

    /**
     * La base con la que ARRANCÓ la aplicación, que no cambia nunca.
     *
     * <p><b>Existe porque si no habría cursos sin billete de vuelta.</b> El listado de cursos
     * reconoce {@code educhronos.db} y los {@code curso-*.db} por su nombre, pero la base de
     * arranque puede llamarse de cualquier forma: el e2e y el arranque de desarrollo la pasan
     * por {@code --spring.datasource.url}. Sin recordarla, en cuanto se abriera otro curso
     * esa base desaparecería del listado y no habría manera de volver a ella salvo reiniciar
     * la aplicación con la URL a mano. Medido con el primer pase de {@code CursoAperturaTest},
     * donde el contexto arranca sobre una {@code abierta.db} a propósito.
     */
    private final Path deArranque;

    /**
     * @param inicial pool con el que arranca la aplicación
     * @param fichero base que ese pool abre
     */
    public BaseConmutable(HikariDataSource inicial, Path fichero) {
        // El destino de la superclase se fija una vez y no se vuelve a tocar: quien manda
        // es getTargetDataSource(), que lee la referencia atómica. Se pasa aquí porque
        // DelegatingDataSource.afterPropertiesSet() exige que haya uno y porque un objeto a
        // medio construir no debe existir ni un instante.
        super(inicial);
        this.abierta = new AtomicReference<>(new Abierta(inicial, fichero));
        this.deArranque = fichero;
    }

    /**
     * El pool vigente. Lo llama {@code DelegatingDataSource.obtainTargetDataSource()} en cada
     * {@code getConnection()}, que es lo que hace que un cambio surta efecto en la conexión
     * siguiente sin tocar a nadie más.
     */
    @Override
    public DataSource getTargetDataSource() {
        return abierta.get().pool();
    }

    /** El fichero de base que está abierto ahora mismo. */
    public Path baseAbierta() {
        return abierta.get().fichero();
    }

    /** El fichero con el que arrancó la aplicación. Ver el campo: es el billete de vuelta. */
    public Path baseDeArranque() {
        return deArranque;
    }

    /**
     * Pone el pool nuevo y devuelve el que había, SIN cerrarlo.
     *
     * @param nuevo pool ya construido y ya probado contra su fichero
     * @param fichero base que abre ese pool
     * @return el pool saliente, que el llamador tiene que cerrar (o volver a poner)
     */
    public HikariDataSource sustituir(HikariDataSource nuevo, Path fichero) {
        return abierta.getAndSet(new Abierta(nuevo, fichero)).pool();
    }

    /**
     * Conexiones en uso del pool vigente. Es la señal de «ya no hay nadie dentro» que espera
     * el cambio de curso antes de tirar del pool viejo.
     *
     * <p>Devuelve 0 si el pool todavía no tiene MXBean: Hikari lo publica al crear la piscina,
     * y un pool que aún no la tiene no puede tener conexiones en uso.
     */
    public int conexionesActivas() {
        HikariPoolMXBean piscina = abierta.get().pool().getHikariPoolMXBean();
        return piscina == null ? 0 : piscina.getActiveConnections();
    }

    /** Cierra el pool vigente. Lo llama Spring al cerrar el contexto. */
    @Override
    public void close() {
        abierta.get().pool().close();
    }

    /**
     * El fichero que hay dentro de una URL {@code jdbc:sqlite:<ruta>}, sin los parámetros que
     * pueda traer detrás ({@code ?foreign_keys=on} y compañía), que no son parte de la ruta.
     *
     * @param url URL JDBC tal como la ve el pool
     */
    public static Path ficheroDeUrl(String url) {
        String ruta = url.startsWith(PREFIJO_URL) ? url.substring(PREFIJO_URL.length()) : url;
        int parametros = ruta.indexOf('?');
        if (parametros >= 0) {
            ruta = ruta.substring(0, parametros);
        }
        return Path.of(ruta).toAbsolutePath();
    }
}
