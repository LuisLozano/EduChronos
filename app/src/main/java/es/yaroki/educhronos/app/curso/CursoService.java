package es.yaroki.educhronos.app.curso;

import com.zaxxer.hikari.HikariDataSource;
import es.yaroki.educhronos.app.config.BaseConmutable;
import es.yaroki.educhronos.app.config.CarpetaDatos;
import es.yaroki.educhronos.app.config.FabricaDeBases;
import es.yaroki.educhronos.app.web.dto.CursoListadoDTO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Los cursos de la carpeta: cuáles hay, cuál está abierto, cómo se abre otro y cómo se crea
 * el siguiente (O-curso, S159 fase B; S160, C-selector-curso fase A).
 *
 * <p><b>De dónde sale la ruta de la base.</b> De {@link BaseConmutable#baseAbierta()}, que es
 * la única fuente de verdad desde S160. Hasta entonces salía de un {@code @Value} sobre
 * {@code spring.datasource.url}, que se fija en el constructor y por tanto MIENTE en cuanto
 * se cambia de curso: la aplicación estaría trabajando sobre un fichero y duplicando otro.
 *
 * <p><b>Por qué el puntero puede no existir.</b> Con {@code --spring.datasource.url} el
 * post-procesador no hace nada y no publica {@code educhronos.datos.carpeta}: no hay carpeta
 * de datos que gobernar y NO se escribe puntero (condición 7, invariante I4). Duplicar y
 * cambiar de curso siguen funcionando —los ficheros viven junto a la base abierta—, pero
 * quién se abre la próxima vez lo sigue decidiendo quien puso la URL.
 *
 * <p><b>Un solo monitor para las dos operaciones largas.</b> {@link #duplicar} y
 * {@link #abrir} son {@code synchronized} sobre esta misma instancia, que es un singleton.
 * No es una precaución: duplicar termina abriendo el curso que acaba de crear, y si otro hilo
 * pudiera colarse entre las dos mitades, el usuario acabaría en un curso que no pidió. Los
 * indicadores de {@link EstadoCurso} resuelven un problema DISTINTO —que no haya un solve o
 * un cambio a la vez, desde cualquier hilo de Tomcat—, y los dos hacen falta.
 *
 * <p><b>Qué se considera un curso de la carpeta.</b> Ver {@link #listar()}: la base abierta
 * más {@code educhronos.db} y los {@code curso-*.db} de su carpeta. Deliberadamente NO es
 * «todos los {@code .db}»: la carpeta de desarrollo de este proyecto tiene veintiséis bases
 * sueltas de sesiones viejas, y ofrecerlas como cursos sería invitar a abrir un banco de
 * pruebas creyendo que es el curso del centro.
 */
@Service
public class CursoService {

    /** El curso pedido no está en la carpeta. */
    public static final String CURSO_NO_EXISTE = "CURSO_NO_EXISTE";

    /** Hay un solve o un duplicado en marcha: el cambio de curso no puede empezar. */
    public static final String CURSO_OCUPADO = "CURSO_OCUPADO";

    /** La base pedida no se ha podido abrir; se sigue en la anterior. */
    public static final String CURSO_NO_ABRE = "CURSO_NO_ABRE";

    /** Prefijo de los ficheros de curso archivado o creado: {@code curso-2026-2027.db}. */
    static final String PREFIJO_CURSO = "curso-";

    /** Extensión de un fichero de base. */
    static final String EXTENSION = ".db";

    /**
     * Cuánto se espera a que el pool vigente se quede sin conexiones en uso antes de tirar de
     * él. Diez segundos: lo que puede tardar en terminar una consulta pesada del catálogo,
     * no lo que tarda un solve —para eso está el indicador de generación, que ya ha dicho que
     * no antes de llegar aquí—. Si vence, el cambio NO se hace: sustituir el pool con alguien
     * dentro le cortaría la conexión a mitad de una lectura.
     */
    static final Duration ESPERA_POOL_LIBRE = Duration.ofSeconds(10);

    /** Cada cuánto se vuelve a mirar si el pool ya está libre. */
    static final Duration SONDEO_POOL = Duration.ofMillis(50);

    private final EstadoCurso estado;

    private final BaseConmutable base;

    private final FabricaDeBases fabrica;

    /**
     * Se construye aquí y NO se inyecta: {@link DuplicadorCurso} no tiene una sola anotación
     * de Spring a propósito (no puede correr bajo {@code @Transactional}), y hacerlo un bean
     * lo dejaría a un {@code @Transactional} de distancia del error que evita. Sin estado,
     * así que una instancia basta.
     */
    private final DuplicadorCurso duplicador = new DuplicadorCurso();

    /**
     * Carpeta de datos que gobierna el post-procesador, vacía cuando no la publicó nadie.
     *
     * <p>Se lee con un {@code @Value} suelto, como {@code educhronos.solver.max-segundos}.
     * El {@code application.properties} deja escrito que a la SEGUNDA clave
     * {@code educhronos.*} se migra a un record {@code @ConfigurationProperties}; esta es la
     * segunda y la migración sigue sin hacerse en S160, porque tocaría el servicio del
     * solver, que no tiene nada que ver con O-curso. Es la deuda
     * D-educhronos-props-sin-agrupar, y el invariante I5 de esta fase dice explícitamente que
     * no se añade ninguna clave más.
     */
    private final String carpetaDeDatos;

    public CursoService(
            EstadoCurso estado,
            BaseConmutable base,
            FabricaDeBases fabrica,
            @Value("${educhronos.datos.carpeta:}") String carpetaDeDatos) {
        this.estado = estado;
        this.base = base;
        this.fabrica = fabrica;
        this.carpetaDeDatos = carpetaDeDatos;
    }

    // ────────────────────────────────────────────────────────────────────────────── listar

    /**
     * Los cursos de la carpeta de la base abierta, para el selector.
     *
     * <p><b>Qué entra</b>: la base ABIERTA y la base DE ARRANQUE, se llamen como se llamen
     * —pueden ser una {@code a.db} pasada por {@code --spring.datasource.url}—, más
     * {@code educhronos.db} y los {@code curso-*.db} de esa misma carpeta. Queda fuera el
     * temporal {@code .curso-….db.tmp} de una duplicación —que es una copia a medias, no un
     * curso— y cualquier otro {@code .db}.
     *
     * <p><b>La de arranque entra aunque ya no esté abierta, y no es un capricho.</b> Si sólo
     * entrara la abierta, una base con nombre no canónico desaparecería del listado en cuanto
     * se abriera otro curso, y no habría forma de volver a ella salvo reiniciar la aplicación
     * con la URL a mano. Es el caso del arranque de desarrollo y del e2e.
     *
     * <p><b>La abierta se describe desde {@link EstadoCurso} y las demás desde su fichero.</b>
     * No es una optimización: la abierta puede haber cambiado de identidad hace un
     * milisegundo —duplicar la archiva— y quien lo sabe es el estado en memoria, que es
     * además lo que contesta el {@code GET /api/curso}. Leer su fichero por JDBC daría dos
     * respuestas posibles a la misma pregunta según el momento.
     *
     * <p><b>No escribe nada</b> (invariante I6): las demás se abren con el {@code leerCurso}
     * del duplicador, que hace un {@code select} y cierra. Una base que no se pueda leer no
     * tumba el listado: sale con {@code nombre} nulo, porque el selector tiene que poder
     * pintarse aunque una de las bases esté rota.
     *
     * <p><b>Orden</b>: por nombre descendente —el curso más reciente arriba, que es el que se
     * busca—, los que no tienen nombre al final, y a igualdad, por fichero.
     */
    public List<CursoListadoDTO> listar() {
        Path abierta = base.baseAbierta();
        Path carpeta = abierta.getParent();
        String nombreAbierta = abierta.getFileName().toString();

        // La abierta va primero y se describe desde el estado en memoria; las demás salen de
        // su propio fichero. El conjunto evita que la de arranque se cuele dos veces cuando
        // resulta ser también la abierta, que es el caso corriente.
        List<CursoListadoDTO> cursos = new ArrayList<>();
        cursos.add(new CursoListadoDTO(nombreAbierta, estado.nombre(), estado.archivado(), true));
        Set<String> vistos = new LinkedHashSet<>();
        vistos.add(nombreAbierta);

        List<Path> otros = new ArrayList<>();
        otros.add(base.baseDeArranque());
        otros.addAll(candidatos(carpeta));

        for (Path fichero : otros) {
            String nombreFichero = fichero.getFileName().toString();
            if (!vistos.add(nombreFichero) || !Files.isRegularFile(fichero)) {
                continue;
            }
            Optional<DuplicadorCurso.FilaCurso> fila = leerSinRomper(fichero);
            cursos.add(
                    new CursoListadoDTO(
                            nombreFichero,
                            fila.map(DuplicadorCurso.FilaCurso::nombre).orElse(null),
                            fila.map(DuplicadorCurso.FilaCurso::archivado).orElse(false),
                            false));
        }
        cursos.sort(
                Comparator.comparing(
                                CursoListadoDTO::nombre,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CursoListadoDTO::fichero));
        return List.copyOf(cursos);
    }

    /**
     * Los ficheros de la carpeta que PUEDEN ser un curso, por su nombre: {@code educhronos.db}
     * y {@code curso-*.db}. El {@code .tmp} de una duplicación a medias queda fuera por no
     * terminar en {@code .db}, y un fichero oculto tampoco pasaría el prefijo.
     */
    private static List<Path> candidatos(Path carpeta) {
        if (carpeta == null || !Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> listado = Files.list(carpeta)) {
            return listado.filter(Files::isRegularFile)
                    .filter(f -> esNombreDeCurso(f.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede listar la carpeta de cursos: " + carpeta, e);
        }
    }

    /** {@code educhronos.db} o {@code curso-<algo>.db}, y nada más. */
    static boolean esNombreDeCurso(String nombre) {
        if (nombre.equals(CarpetaDatos.NOMBRE_FICHERO)) {
            return true;
        }
        return nombre.startsWith(PREFIJO_CURSO)
                && nombre.endsWith(EXTENSION)
                && nombre.length() > PREFIJO_CURSO.length() + EXTENSION.length();
    }

    /**
     * La fila de curso de un fichero que no está abierto, o vacío si no se puede leer. Un
     * fichero ilegible —corrupto, a medio copiar, sin permisos— no puede tumbar el selector:
     * sale sin nombre, y si alguien intenta abrirlo, será entonces cuando se le diga que no.
     */
    private Optional<DuplicadorCurso.FilaCurso> leerSinRomper(Path fichero) {
        try {
            return duplicador.leerCurso(fichero);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────── abrir

    /**
     * Abre otro curso de la carpeta, en caliente.
     *
     * <p>Rechazos, en este orden: un nombre que no sea simple es 400 {@code NOMBRE_INVALIDO}
     * —se comprueba ANTES de mirar la carpeta, porque {@code ../../algo} no es un curso que
     * falte, es una ruta que no se va a resolver—; un fichero que no esté en {@link #listar()}
     * es 404 {@code CURSO_NO_EXISTE}.
     *
     * <p>Pedir el curso que YA está abierto no hace nada y responde bien: es idempotente a
     * propósito, porque un doble clic en el selector no es un error y tirar del pool para
     * volver a poner el mismo fichero sería trabajo y riesgo a cambio de nada.
     *
     * @param fichero nombre simple del fichero, tal como salió del listado
     * @throws RechazoCursoException con su causa y su texto, para cualquiera de los rechazos
     */
    public synchronized void abrir(String fichero) {
        if (fichero == null || fichero.isBlank() || !CarpetaDatos.esNombreSimple(fichero)) {
            throw new RechazoCursoException(
                    HttpStatus.BAD_REQUEST, DuplicadorCurso.NOMBRE_INVALIDO,
                    "«" + fichero + "» no es el nombre de un fichero de curso.");
        }
        if (fichero.equals(base.baseAbierta().getFileName().toString())) {
            return;
        }
        boolean existe = listar().stream().anyMatch(c -> c.fichero().equals(fichero));
        if (!existe) {
            throw new RechazoCursoException(
                    HttpStatus.NOT_FOUND, CURSO_NO_EXISTE,
                    "No hay ningún curso «" + fichero + "» en la carpeta de datos.");
        }
        cambiarA(base.baseAbierta().getParent().resolve(fichero));
    }

    /**
     * El cambio de base propiamente dicho, con sus siete pasos. Lo llaman {@link #abrir} y
     * {@link #duplicar}, los dos ya dentro del monitor.
     *
     * <p><b>El orden está elegido por lo que pasa si cada paso falla</b> (invariante I3: un
     * fallo deja la aplicación sobre la base anterior, con {@link EstadoCurso} intacto):
     *
     * <ol>
     *   <li>Se toma el turno, o 409: mirar y marcar en el mismo bloque, sin ventana.
     *   <li>Se espera a que el pool vigente quede sin conexiones en uso, o 503.
     *   <li>Se construye el pool nuevo, con los ajustes del arranque.
     *   <li>Se le pasa {@code schema.sql}. Esto es lo que ABRE de verdad el fichero, y por
     *       tanto lo que descubre que no es una base de SQLite. Va ANTES de sustituir a
     *       propósito: el fallo caro ocurre mientras nadie ha cambiado de suelo todavía.
     *   <li>Se sustituye, se recarga la identidad y se cierra el pool viejo. Si la recarga
     *       fallara con el pool ya puesto, se deshace la sustitución y se cierra el nuevo: es
     *       la única forma de que un fallo aquí no deje la aplicación hablando de un curso
     *       sobre una base que es otra.
     *   <li>El puntero, sólo si hay carpeta de datos que gobernar.
     *   <li>{@code terminarCambio()} en un {@code finally}, pase lo que pase: si sólo se
     *       llamara al salir bien, un fichero corrupto dejaría la aplicación devolviendo 503
     *       a todo hasta que alguien la reiniciara.
     * </ol>
     */
    private void cambiarA(Path destino) {
        if (!estado.intentarIniciarCambio()) {
            throw new RechazoCursoException(
                    HttpStatus.CONFLICT, CURSO_OCUPADO,
                    "Hay un horario generándose o un curso duplicándose: espera a que termine"
                            + " y vuelve a intentarlo.");
        }
        try {
            if (!esperarPoolLibre()) {
                throw new RechazoCursoException(
                        HttpStatus.SERVICE_UNAVAILABLE, EstadoCurso.CURSO_CAMBIANDO,
                        "La base actual sigue ocupada tras "
                                + ESPERA_POOL_LIBRE.toSeconds()
                                + " s: no se ha cambiado de curso. Vuelve a intentarlo.");
            }
            Path anterior = base.baseAbierta();
            HikariDataSource nuevo = fabrica.poolPara(destino);
            try {
                // Esto es lo que ABRE el fichero de verdad, y por tanto lo que descubre que
                // no es una base de SQLite. Con el pool viejo todavía puesto: si revienta,
                // nadie ha cambiado de suelo.
                fabrica.prepararEsquema(nuevo);
            } catch (RuntimeException e) {
                nuevo.close();
                throw noAbre(destino, e);
            }

            HikariDataSource viejo = base.sustituir(nuevo, destino);
            try {
                estado.recargar();
            } catch (RuntimeException e) {
                // Ya se había sustituido: se vuelve a poner el pool anterior CON su fichero,
                // se cierra el que no sirvió y se relee la identidad de vuelta. Sin esto, la
                // aplicación se quedaría hablando de un curso sobre una base que es otra.
                base.sustituir(viejo, anterior);
                nuevo.close();
                recargarSinRomper();
                throw noAbre(destino, e);
            }
            viejo.close();
            Path puntero = puntero();
            if (puntero != null) {
                duplicador.escribirPuntero(puntero, destino.getFileName().toString());
            }
        } finally {
            estado.terminarCambio();
        }
    }

    /** El rechazo de un fichero que no se ha podido abrir, con la causa de verdad dentro. */
    private static RechazoCursoException noAbre(Path destino, RuntimeException fallo) {
        return new RechazoCursoException(
                HttpStatus.INTERNAL_SERVER_ERROR, CURSO_NO_ABRE,
                "No se ha podido abrir " + destino.getFileName() + ": " + fallo
                        + ". Se sigue en el curso anterior.");
    }

    /**
     * Relee la identidad sobre el pool ya restaurado, tragándose un segundo fallo. Si la base
     * anterior tampoco contesta no hay nada mejor que hacer aquí: lo que tiene que llegar al
     * usuario es el fallo del cambio que él provocó, no el de la marcha atrás.
     */
    private void recargarSinRomper() {
        try {
            estado.recargar();
        } catch (RuntimeException e) {
            // Sin nada que añadir: el rechazo que va a subir ya lo cuenta.
        }
    }

    /**
     * Espera a que nadie tenga una conexión del pool vigente. Sondeo y no un cierre forzado:
     * {@code HikariDataSource.close()} corta las conexiones en uso, y hacerlo abortaría a
     * media lectura la petición de otro usuario.
     *
     * <p><b>VENTANA CONOCIDA, de microsegundos y NO cubierta</b> (S160), hermana de las dos
     * ventanas de corte que documenta el javadoc de {@link DuplicadorCurso}. «Cero conexiones
     * activas» no quiere decir «nadie está escribiendo»: quiere decir que nadie tiene una
     * conexión TOMADA en este instante. Una petición de escritura que ya pasó por
     * {@link GuardaSoloLectura} —cuando {@code cambiando} todavía era falso— y aún no ha
     * pedido su conexión al pool cae del otro lado del cambio: se ejecuta contra la base
     * NUEVA habiendo sido aprobada contra el estado de la ANTERIOR. Si la nueva estuviera
     * archivada, esa escritura entraría en un curso de solo lectura.
     *
     * <p>No se cierra, y la decisión es deliberada. Cerrarla exigiría contar las peticiones
     * en vuelo en el filtro —un contador que sube al entrar y baja al salir, con su propia
     * espera— y eso pone estado compartido en el camino de TODAS las peticiones para un
     * riesgo que aquí no se materializa: Educhronos es de un solo usuario en su ordenador
     * (condición 8 de O-instalación: escucha sólo en el bucle local), y el cambio de curso se
     * lanza desde un diálogo modal, de modo que no hay nadie más pulsando botones mientras
     * dura. Queda escrito para que, si alguna vez hay concurrencia real, se sepa dónde
     * mirar.
     *
     * @return {@code true} si el pool quedó libre dentro del plazo
     */
    private boolean esperarPoolLibre() {
        long limite = System.nanoTime() + ESPERA_POOL_LIBRE.toNanos();
        while (true) {
            if (base.conexionesActivas() == 0) {
                return true;
            }
            if (System.nanoTime() >= limite) {
                return false;
            }
            try {
                Thread.sleep(SONDEO_POOL.toMillis());
            } catch (InterruptedException e) {
                // Que interrumpan este hilo significa que el proceso se va. Se restaura la
                // marca y se contesta lo único cierto: no, no llegó a quedar libre.
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────── duplicar

    /**
     * Crea el curso siguiente a partir del abierto, archiva el abierto y DEJA ABIERTO EL
     * NUEVO (S160).
     *
     * <p><b>Por qué abre el nuevo en el acto.</b> Hasta S159 duplicar dejaba la aplicación
     * dentro del curso recién archivado, es decir, en solo lectura: el centro creaba el curso
     * del año siguiente y lo primero que veía era un 403 en cuanto tocaba algo. Nadie duplica
     * para quedarse donde estaba.
     *
     * <p><b>Si la apertura falla, el duplicado QUEDA HECHO.</b> No se deshace: el fichero
     * nuevo es correcto y el origen ya está archivado, y borrar un curso recién creado por un
     * fallo al abrirlo sería destruir lo único que salió bien. Lo que sale es el error de la
     * apertura —{@code CURSO_NO_ABRE} o {@code CURSO_OCUPADO}—, y la aplicación se queda
     * sobre el origen archivado, desde donde el curso nuevo se abre a mano con
     * {@code POST /api/cursos/abrir}. Es un estado raro pero honesto: el listado lo enseña.
     *
     * <p><b>El requisito (b)</b>: de un curso archivado sí se duplica, pero sólo si no queda
     * ningún curso ACTIVO en la carpeta. Es la salida de un callejón —un centro que archivó
     * su único curso no puede quedarse sin poder crear ninguno—, y la condición se calcula
     * aquí, con {@link #listar()}, y viaja al duplicador como un booleano. Un curso SIN fila
     * cuenta como activo: una base de antes de S159 es un curso en marcha, no un archivo.
     *
     * <p><b>El nombre no puede repetir el de NINGÚN curso de la carpeta, y eso se comprueba
     * por CONTENIDO</b> (corrección de S160). {@link DuplicadorCurso} ya rechazaba dos casos
     * —que el nombre sea el del curso abierto, y que el fichero de destino exista—, pero los
     * dos miran el NOMBRE DE FICHERO, y el fichero no siempre se llama como el curso: una
     * base anterior a S159 se llama {@code educhronos.db} y por dentro puede decir
     * {@code 2025/2026}. Medido en el M4 de esta sesión por el arquitecto: desde un
     * {@code 2026/2027} activo se creó un {@code curso-2025-2026.db} habiendo ya un
     * {@code educhronos.db} llamado {@code 2025/2026}, y el centro se quedó con DOS cursos
     * del mismo nombre y sin forma de distinguirlos en el selector. El hallazgo estaba
     * anotado en el M2 («no se abre ningún otro fichero de curso para leer su tabla curso») y
     * se quedó en nota; aquí se cierra.
     *
     * <p>Se comprueba AQUÍ y no en el duplicador porque exige mirar todos los ficheros de la
     * carpeta, que es lo que {@link #listar()} ya hace; el duplicador sigue sin conocer más
     * base que la suya. Y va ANTES de tomar el turno y de escribir un solo byte: un rechazo
     * no deja fichero, ni archivado, ni puntero, ni indicador levantado.
     *
     * @param nombreNuevo nombre del curso a crear
     * @param nombreActual nombre del curso actual, necesario sólo si la base no lo trae
     * @return la ruta del fichero creado
     * @throws RechazoCursoException si la operación se rechaza por una razón prevista
     */
    public synchronized Path duplicar(String nombreNuevo, String nombreActual) {
        Path origen = base.baseAbierta();
        String nombreArchivado = estado.nombre() != null ? estado.nombre() : nombreActual;
        // UNA sola lectura de la carpeta para las dos preguntas que dependen de ella: si el
        // nombre ya está cogido y si queda algún curso activo. Dos llamadas a listar()
        // podrían ver carpetas distintas y contestar cosas incompatibles.
        List<CursoListadoDTO> cursos = listar();
        exigirNombreLibre(cursos, nombreNuevo);
        boolean permitirArchivado = sinNingunCursoActivo(cursos);
        if (!estado.intentarIniciarDuplicado()) {
            throw new RechazoCursoException(
                    HttpStatus.CONFLICT, CURSO_OCUPADO,
                    "Hay un horario generándose o un curso abriéndose: espera a que termine y"
                            + " vuelve a intentarlo.");
        }
        Path destino;
        try {
            destino = duplicador.duplicar(
                    origen, nombreActual, nombreNuevo, puntero(), permitirArchivado);
            estado.marcarArchivado(nombreArchivado);
        } finally {
            // En un finally, también cuando el duplicado se rechaza: un nombre mal escrito
            // no puede dejar la aplicación sin aceptar escrituras.
            estado.terminarDuplicado();
        }
        // Dentro del mismo synchronized y con el indicador de duplicado ya apagado, que es lo
        // que el cambio exige para poder empezar. Nadie puede colarse entre las dos mitades.
        cambiarA(destino);
        return destino;
    }

    /**
     * ¿No queda ningún curso activo en la carpeta? Un curso sin nombre cuenta como ACTIVO:
     * ver la nota del requisito (b) en {@link #duplicar}.
     */
    private static boolean sinNingunCursoActivo(List<CursoListadoDTO> cursos) {
        return cursos.stream().noneMatch(curso -> !curso.archivado());
    }

    /**
     * Rechaza si ya hay un curso con ese nombre en la carpeta, sea cual sea su fichero.
     *
     * <p>Compara contra el nombre LEÍDO de cada base, que es lo que distingue esta
     * comprobación de las dos del duplicador: ver la nota de {@link #duplicar}. Un curso sin
     * nombre no compite con nadie —{@code null} no es igual a ningún nombre— y por eso el
     * filtro lo deja fuera antes de comparar.
     *
     * <p>El {@code message} nombra el FICHERO además del curso: sin él, el usuario ve «ya
     * existe un curso 2025/2026» mirando un selector donde el único 2025/2026 se llama
     * {@code educhronos.db}, y no sabe cuál es.
     *
     * @throws RechazoCursoException 409 {@code CURSO_YA_EXISTE} si el nombre está cogido
     */
    private static void exigirNombreLibre(List<CursoListadoDTO> cursos, String nombreNuevo) {
        if (nombreNuevo == null) {
            // La forma la valida el duplicador, que es quien tiene el texto del rechazo.
            return;
        }
        cursos.stream()
                .filter(curso -> nombreNuevo.equals(curso.nombre()))
                .findFirst()
                .ifPresent(
                        curso -> {
                            throw new RechazoCursoException(
                                    HttpStatus.CONFLICT, DuplicadorCurso.CURSO_YA_EXISTE,
                                    "Ya existe un curso " + nombreNuevo
                                            + " en la carpeta de datos (" + curso.fichero()
                                            + ").");
                        });
    }

    // ─────────────────────────────────────────────────────────────────────────────── común

    /** El fichero de la base abierta, tal como lo ve el pool. */
    Path baseAbierta() {
        return base.baseAbierta();
    }

    /** El fichero de puntero, o {@code null} si nadie gobierna la carpeta de datos. */
    Path puntero() {
        if (carpetaDeDatos == null || carpetaDeDatos.isBlank()) {
            return null;
        }
        return Path.of(carpetaDeDatos).resolve(CarpetaDatos.NOMBRE_PUNTERO);
    }
}
