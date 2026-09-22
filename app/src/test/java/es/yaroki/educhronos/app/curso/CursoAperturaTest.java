package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.config.BaseConmutable;
import es.yaroki.educhronos.app.web.dto.CursoListadoDTO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Listar y abrir cursos sobre un contexto de verdad (O-curso, S160, C-selector-curso fase A).
 *
 * <p><b>Contexto y carpeta propios, como {@code CursoEndpointTest}.</b> Abrir cambia la base
 * que el pool tiene puesta: hacerlo sobre la {@code educhronos-test.db} que comparte la suite
 * dejaría a los demás tests apuntando a otro fichero según el orden en que corrieran. La base
 * de este contexto se llama {@code abierta.db} a propósito y no {@code educhronos.db}: así el
 * listado tiene que incluir la base ABIERTA se llame como se llame, que es el caso del
 * arranque con {@code --spring.datasource.url} y el que el e2e usa.
 *
 * <p>Los casos que ABREN otro curso dejan la aplicación donde la encontraron —vuelven a abrir
 * {@code abierta.db} al final— porque el contexto se comparte entre ellos. No se usa
 * {@code @DirtiesContext}: rehacer el contexto entero por cada caso multiplicaría el tiempo
 * de la suite, y lo que hace falta es una línea al terminar.
 */
@SpringBootTest
class CursoAperturaTest {

    /** Carpeta de este contexto. Estática: la necesita el registro de propiedades. */
    @TempDir static Path carpeta;

    /** La base con la que arranca el contexto. Nombre deliberadamente NO canónico. */
    private static final String ABIERTA = "abierta.db";

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve(ABIERTA).toAbsolutePath());
    }

    @Autowired private CursoService servicio;

    @Autowired private EstadoCurso estado;

    @Autowired private BaseConmutable base;

    /**
     * Devuelve al contexto el estado exacto con el que arrancó: la base de partida abierta,
     * su fila de curso BORRADA —una base sin nombre, condición 6— y ningún otro fichero en la
     * carpeta.
     *
     * <p><b>Borrar la fila es obligatorio y lo destapó un fallo.</b> Los casos comparten
     * contexto, y los que escriben la identidad de {@code abierta.db} la dejaban puesta para
     * el siguiente: el caso del ORDEN veía un nombre que no había puesto él y ordenaba
     * distinto según qué corriera antes. Un estado inicial que depende del orden de ejecución
     * no es un estado inicial.
     */
    @BeforeEach
    void prepararCarpeta() throws Exception {
        servicio.abrir(ABIERTA);
        borrarTodoMenos(ABIERTA);
        try (var conexion = base.getConnection();
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("delete from curso");
            sentencia.executeUpdate("delete from nivel");
        }
        estado.recargar();
    }

    // ────────────────────────────────────────────────────────────────────────────── listar

    /**
     * (T6) El listado es la base abierta —con su nombre raro— más {@code educhronos.db} y los
     * {@code curso-*.db}, y NADA más: ni el temporal de una duplicación a medias ni un
     * {@code .db} cualquiera de la carpeta.
     *
     * <p><b>Los dos ficheros excluidos son los que importan.</b> El {@code .tmp} es una copia
     * a medio hacer y ofrecerlo como curso sería ofrecer una base corrupta; el {@code .db}
     * suelto es el caso real de la carpeta de desarrollo de este proyecto, que tiene
     * veintiséis bancos de sesiones viejas junto a la base de trabajo.
     *
     * <p>El aserto final es el invariante I6: listar NO escribe. Se mide por md5 de los
     * ficheros que no están abiertos, no por «no ha fallado».
     */
    @Test
    void listar_soloLaAbiertaEduchronosYLosCursoGuion_ySinTocarNada() throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("educhronos.db"), "2024/2025", true);
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        BancoDeCursos.fabricar(carpeta.resolve("banco-viejo.db"), "2020/2021", false);
        Files.writeString(
                carpeta.resolve(".curso-2027-2028.db.tmp"), "copia a medias",
                StandardCharsets.UTF_8);
        String huellaEduchronos = BancoDeCursos.huella(carpeta.resolve("educhronos.db"));
        String huellaCurso = BancoDeCursos.huella(carpeta.resolve("curso-2026-2027.db"));

        List<CursoListadoDTO> cursos = servicio.listar();

        assertThat(cursos)
                .extracting(CursoListadoDTO::fichero)
                .as("la abierta con su nombre raro, educhronos.db y el curso-*; nada más")
                .containsExactlyInAnyOrder(ABIERTA, "educhronos.db", "curso-2026-2027.db");

        assertThat(cursos)
                .filteredOn(c -> c.fichero().equals("curso-2026-2027.db"))
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.nombre()).isEqualTo("2026/2027");
                            assertThat(c.archivado()).isFalse();
                            assertThat(c.abierto()).isFalse();
                        });
        assertThat(cursos)
                .filteredOn(c -> c.fichero().equals("educhronos.db"))
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.nombre()).isEqualTo("2024/2025");
                            assertThat(c.archivado()).as("se lee del fichero").isTrue();
                            assertThat(c.abierto()).isFalse();
                        });
        assertThat(cursos)
                .filteredOn(CursoListadoDTO::abierto)
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.fichero()).isEqualTo(ABIERTA);
                            assertThat(c.nombre())
                                    .as("la abierta se describe desde EstadoCurso")
                                    .isEqualTo(estado.nombre());
                        });

        assertThat(BancoDeCursos.huella(carpeta.resolve("educhronos.db")))
                .as("listar no escribe (I6)")
                .isEqualTo(huellaEduchronos);
        assertThat(BancoDeCursos.huella(carpeta.resolve("curso-2026-2027.db")))
                .as("listar no escribe (I6)")
                .isEqualTo(huellaCurso);
    }

    /**
     * (T6.b) El orden: por nombre descendente, los que no tienen nombre al final. El curso
     * más reciente arriba, que es el que se busca en un selector.
     */
    @Test
    void listar_ordenaPorNombreDescendenteYLosSinNombreAlFinal() throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("curso-2024-2025.db"), "2024/2025", true);
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        BancoDeCursos.fabricar(carpeta.resolve("educhronos.db"), null, false);

        assertThat(servicio.listar())
                .extracting(CursoListadoDTO::fichero)
                .containsExactly(
                        "curso-2026-2027.db", "curso-2024-2025.db", ABIERTA, "educhronos.db");
    }

    // ─────────────────────────────────────────────────────────────────────────────── abrir

    /**
     * (T7) Abrir cambia la base de verdad: lo que se escriba DESPUÉS cae en el fichero nuevo
     * y no en el viejo, y {@link EstadoCurso} pasa a hablar del curso nuevo.
     *
     * <p>El aserto que cuenta es el de los dos ficheros leídos por JDBC directo, fuera del
     * pool: que el estado en memoria diga el nombre nuevo no prueba que las escrituras hayan
     * cambiado de sitio, y es justo lo que un cambio mal hecho dejaría roto en silencio.
     */
    @Test
    void abrir_lasEscriturasPosterioresCaenEnElFicheroNuevo() throws Exception {
        Path destino = BancoDeCursos.fabricar(
                carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        Path origen = carpeta.resolve(ABIERTA);
        insertarNivel("ANTES");

        servicio.abrir("curso-2026-2027.db");

        assertThat(base.baseAbierta()).isEqualTo(destino);
        assertThat(estado.nombre()).isEqualTo("2026/2027");
        assertThat(estado.archivado()).isFalse();

        insertarNivel("DESPUES");

        assertThat(codigosDeNivel(destino))
                .as("lo escrito después está en la base NUEVA")
                .containsExactly("DESPUES");
        assertThat(codigosDeNivel(origen))
                .as("y la anterior conserva lo suyo, sin lo nuevo")
                .containsExactly("ANTES");
    }

    /**
     * (T7.c) Abrir CIERRA el pool anterior: después del cambio no queda ni un descriptor
     * abierto sobre el fichero viejo.
     *
     * <p><b>Este caso existe porque el mutante lo pidió.</b> En el M3 de S160, quitar el
     * {@code viejo.close()} sólo tumbaba un caso, y lo tumbaba de rebote —por un fichero que
     * el pool huérfano recreaba y se colaba en un listado—, no por medir la fuga. Un aserto
     * que caza por accidente deja de cazar en cuanto alguien toca el andamio.
     *
     * <p>Se mide por {@code /proc/self/fd}, que es lo único que dice la verdad sobre un pool
     * que nadie cerró: un {@code HikariDataSource} huérfano no es observable desde la API del
     * servicio, y lo que hace daño de verdad —descriptores que se acumulan cambio tras
     * cambio, y un fichero que el sistema no puede soltar— sí lo es. Fuera de Linux
     * {@code /proc} no existe y el caso lo dice en vez de fingir que mide.
     */
    @Test
    void abrir_cierraElPoolAnterior(@TempDir Path otra) throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        Path anterior = base.baseAbierta();
        insertarNivel("FUERZA_UNA_CONEXION");
        long antes = descriptoresSobre(anterior);
        assumeQueSePuedeMedir(antes);
        assertThat(antes).as("precondición: el pool viejo tiene el fichero abierto").isPositive();

        servicio.abrir("curso-2026-2027.db");

        assertThat(descriptoresSobre(anterior))
                .as("el pool anterior quedó cerrado: ni un descriptor sobre %s", anterior)
                .isZero();
        assertThat(descriptoresSobre(base.baseAbierta()))
                .as("y el nuevo sí está abierto")
                .isPositive();
    }

    /**
     * (T7.b) Con carpeta de datos se escribe el puntero; sin ella, no (invariante I4). Este
     * contexto arranca con {@code spring.datasource.url} explícita, así que
     * {@code educhronos.datos.carpeta} llega vacía y NO debe aparecer ningún puntero: es la
     * condición 7, que dice que el arranque de desarrollo no toca la carpeta del usuario.
     */
    @Test
    void abrir_sinCarpetaDeDatosNoEscribePuntero() throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);

        servicio.abrir("curso-2026-2027.db");

        assertThat(servicio.puntero()).as("no hay carpeta que gobernar").isNull();
        assertThat(carpeta.resolve("curso-abierto")).doesNotExist();
    }

    /**
     * (T8) Abrir una base SIN tabla {@code curso} —una de antes de S159, condición 6— la
     * prepara: la tabla existe después y el nombre es null. Sin el {@code schema.sql} del
     * paso 4, {@code EstadoCurso.recargar()} consultaría una tabla que no existe y el cambio
     * moriría dejando la aplicación en la base anterior por un motivo evitable.
     */
    @Test
    void abrir_unaBaseSinTablaCurso_laGanaVaciaYElNombreEsNull() throws Exception {
        Path antigua = carpeta.resolve("curso-2019-2020.db");
        crearBaseSinTablaCurso(antigua);
        assertThat(BancoDeCursos.tieneTabla(antigua, "curso"))
                .as("de partida NO la tiene")
                .isFalse();

        servicio.abrir("curso-2019-2020.db");

        assertThat(BancoDeCursos.tieneTabla(antigua, "curso")).as("ahora sí").isTrue();
        assertThat(BancoDeCursos.filas(antigua, "curso")).as("y está vacía").isZero();
        assertThat(estado.nombre()).isNull();
        assertThat(estado.archivado()).isFalse();
        assertThat(base.baseAbierta()).isEqualTo(antigua);
    }

    /**
     * (T9.a) Un fichero que no es un nombre simple es 400, y se rechaza ANTES de mirar la
     * carpeta: {@code ../x.db} no es un curso que falte, es una ruta que no se va a resolver.
     * Sin esta guarda, un {@code resolve} abriría una base de FUERA de la carpeta de datos.
     */
    @Test
    void abrir_conRutaEnLugarDeNombre_400YNoCambiaNada() {
        Path antes = base.baseAbierta();

        for (String malo : new String[] {"../x.db", "sub/x.db", "..\\x.db", "..", "", "   "}) {
            assertThatThrownBy(() -> servicio.abrir(malo))
                    .as("%s", malo)
                    .isInstanceOfSatisfying(
                            RechazoCursoException.class,
                            e -> {
                                assertThat(e.causa())
                                        .isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
                                assertThat(e.status().value()).isEqualTo(400);
                                assertThat(e.getMessage()).isNotEmpty();
                            });
        }
        assertThat(base.baseAbierta()).as("la base no ha cambiado").isEqualTo(antes);
    }

    /** (T9.b) Un fichero que no está en el listado es 404, y la base sigue donde estaba. */
    @Test
    void abrir_ficheroInexistente_404YNoCambiaNada() {
        Path antes = base.baseAbierta();

        assertThatThrownBy(() -> servicio.abrir("curso-2030-2031.db"))
                .isInstanceOfSatisfying(
                        RechazoCursoException.class,
                        e -> {
                            assertThat(e.causa()).isEqualTo(CursoService.CURSO_NO_EXISTE);
                            assertThat(e.status().value()).isEqualTo(404);
                        });
        assertThat(base.baseAbierta()).isEqualTo(antes);
    }

    /**
     * (T9.c) Con un solve en marcha, abrir es 409 {@code CURSO_OCUPADO} y la base NO cambia.
     * El solve se simula dando de alta la generación en {@link EstadoCurso}, que es
     * exactamente lo que hace {@code GeneradorHorarioService} al entrar: montar un solve de
     * verdad metería minutos en la suite para medir la misma línea.
     */
    @Test
    void abrir_conUnSolveEnMarcha_409YNoCambiaNada() throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        Path antes = base.baseAbierta();
        assertThat(estado.intentarIniciarGeneracion()).isTrue();

        try {
            assertThatThrownBy(() -> servicio.abrir("curso-2026-2027.db"))
                    .isInstanceOfSatisfying(
                            RechazoCursoException.class,
                            e -> {
                                assertThat(e.causa()).isEqualTo(CursoService.CURSO_OCUPADO);
                                assertThat(e.status().value()).isEqualTo(409);
                                assertThat(e.getMessage()).contains("generándose");
                            });
            assertThat(base.baseAbierta()).isEqualTo(antes);
            assertThat(estado.cambiando()).as("y no queda marcado como cambiando").isFalse();
        } finally {
            estado.terminarGeneracion();
        }
    }

    /**
     * (T9.d) Abrir el curso que YA está abierto no hace nada y no falla. Es idempotente a
     * propósito: un doble clic en el selector no es un error, y tirar del pool para volver a
     * poner el mismo fichero sería trabajo y riesgo a cambio de nada.
     */
    @Test
    void abrir_elQueYaEstaAbierto_noHaceNada() {
        Path antes = base.baseAbierta();
        String nombreAntes = estado.nombre();

        servicio.abrir(ABIERTA);

        assertThat(base.baseAbierta()).isEqualTo(antes);
        assertThat(estado.nombre()).isEqualTo(nombreAntes);
        assertThat(estado.cambiando()).isFalse();
    }

    /**
     * (T10, invariante I3) Un fichero que NO es una base de SQLite falla con un mensaje, y la
     * aplicación sigue sobre la base anterior: el estado no cambia y una lectura posterior
     * funciona.
     *
     * <p>El último aserto es el que de verdad mide I3. Que la excepción salga bien es fácil;
     * lo difícil es que el pool no se haya quedado a medio sustituir, y eso sólo se ve
     * escribiendo y leyendo después del fallo.
     */
    @Test
    void abrir_unFicheroQueNoEsUnaBase_falla_yLaAplicacionSigueEnLaAnterior() throws Exception {
        Path basura = carpeta.resolve("curso-2026-2027.db");
        Files.writeString(basura, "esto no es una base de datos", StandardCharsets.UTF_8);
        Path antes = base.baseAbierta();
        String nombreAntes = estado.nombre();

        assertThatThrownBy(() -> servicio.abrir("curso-2026-2027.db"))
                .isInstanceOfSatisfying(
                        RechazoCursoException.class,
                        e -> {
                            assertThat(e.causa()).isEqualTo(CursoService.CURSO_NO_ABRE);
                            assertThat(e.getMessage())
                                    .as("el message nombra el fichero y dice que se sigue")
                                    .contains("curso-2026-2027.db")
                                    .contains("curso anterior");
                        });

        assertThat(base.baseAbierta()).as("la base NO ha cambiado").isEqualTo(antes);
        assertThat(estado.nombre()).as("el estado, intacto").isEqualTo(nombreAntes);
        assertThat(estado.cambiando()).as("el indicador se levanta en el finally").isFalse();

        insertarNivel("DESPUES_DEL_FALLO");
        assertThat(codigosDeNivel(antes))
                .as("la aplicación sigue leyendo y escribiendo en la base anterior")
                .contains("DESPUES_DEL_FALLO");
    }

    // ───────────────────────────────────────────────────────────── requisito (b) y duplicar

    /**
     * (T12.a) Con un curso ACTIVO además del archivado, duplicar el archivado sigue siendo
     * 409 {@code CURSO_ARCHIVADO}: es el comportamiento de S159 y no se toca.
     */
    @Test
    void duplicar_unArchivadoHabiendoUnActivo_409() throws Exception {
        BancoDeCursos.fabricar(carpeta.resolve("curso-2026-2027.db"), "2026/2027", false);
        archivarLaBaseAbierta("2025/2026");

        assertThatThrownBy(() -> servicio.duplicar("2027/2028", null))
                .isInstanceOfSatisfying(
                        RechazoCursoException.class,
                        e -> {
                            assertThat(e.causa()).isEqualTo(DuplicadorCurso.CURSO_ARCHIVADO);
                            assertThat(e.status().value()).isEqualTo(409);
                        });
        assertThat(carpeta.resolve("curso-2027-2028.db")).doesNotExist();
    }

    /**
     * (T12.b, requisito (b)) Con SOLO cursos archivados, duplicar el archivado SÍ se permite,
     * y el curso nuevo queda ABIERTO y activo. Es la salida del callejón: un centro que
     * archivó su único curso no puede quedarse sin poder crear ninguno.
     */
    @Test
    void duplicar_unArchivadoSinNingunActivo_201YQuedaAbiertoElNuevo() throws Exception {
        archivarLaBaseAbierta("2025/2026");

        Path destino = servicio.duplicar("2026/2027", null);

        assertThat(destino).isRegularFile().hasFileName("curso-2026-2027.db");
        assertThat(base.baseAbierta()).as("queda abierto el NUEVO").isEqualTo(destino);
        assertThat(estado.nombre()).isEqualTo("2026/2027");
        assertThat(estado.archivado()).as("y activo, que es el punto entero").isFalse();
        assertThat(BancoDeCursos.curso(carpeta.resolve(ABIERTA)))
                .as("el de origen sigue archivado")
                .isEqualTo("2025/2026|true");
    }

    /**
     * (T11.b) Duplicar un curso ACTIVO archiva el origen y deja abierto el nuevo. Hasta S159
     * dejaba la aplicación dentro del curso recién archivado, es decir, en solo lectura:
     * el centro creaba el curso del año siguiente y lo primero que veía era un 403.
     */
    @Test
    void duplicar_dejaAbiertoElCursoNuevoYElOrigenArchivado() throws Exception {
        ponerNombreALaBaseAbierta("2025/2026", false);

        Path destino = servicio.duplicar("2026/2027", null);

        assertThat(base.baseAbierta()).isEqualTo(destino);
        assertThat(estado.nombre()).isEqualTo("2026/2027");
        assertThat(estado.archivado()).isFalse();
        assertThat(BancoDeCursos.curso(carpeta.resolve(ABIERTA))).isEqualTo("2025/2026|true");
        assertThat(servicio.listar())
                .extracting(CursoListadoDTO::fichero)
                .containsExactlyInAnyOrder(ABIERTA, "curso-2026-2027.db");
    }

    // ────────────────────────────────────────────────────────────────────────────── andamio

    /** Escribe la identidad de la base ABIERTA por el pool vigente y recarga el estado. */
    private void ponerNombreALaBaseAbierta(String nombre, boolean archivado) throws Exception {
        try (var conexion = base.getConnection();
                var sentencia =
                        conexion.prepareStatement(
                                "insert or replace into curso (id, nombre, archivado)"
                                        + " values (1, ?, ?)")) {
            sentencia.setString(1, nombre);
            sentencia.setBoolean(2, archivado);
            sentencia.executeUpdate();
        }
        estado.recargar();
    }

    private void archivarLaBaseAbierta(String nombre) throws Exception {
        ponerNombreALaBaseAbierta(nombre, true);
    }

    /** Escribe un nivel POR EL POOL, que es lo que dice a qué fichero van las escrituras. */
    private void insertarNivel(String codigo) throws Exception {
        try (var conexion = base.getConnection();
                var sentencia =
                        conexion.prepareStatement(
                                "insert into nivel (codigo, orden) values (?, 1)")) {
            sentencia.setString(1, codigo);
            sentencia.executeUpdate();
        }
    }

    /** Lee los niveles de un fichero por JDBC directo, FUERA del pool. */
    private static List<String> codigosDeNivel(Path fichero) throws Exception {
        List<String> codigos = new java.util.ArrayList<>();
        try (var conexion = BancoDeCursos.conectar(fichero);
                var sentencia = conexion.createStatement();
                var filas = sentencia.executeQuery("select codigo from nivel order by codigo")) {
            while (filas.next()) {
                codigos.add(filas.getString(1));
            }
        }
        return codigos;
    }

    /** Una base con el esquema MENOS la tabla curso: una de antes de S159 (condición 6). */
    private static void crearBaseSinTablaCurso(Path fichero) throws Exception {
        BancoDeCursos.fabricar(fichero, null, false);
        try (var conexion = BancoDeCursos.conectar(fichero);
                var sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("drop table curso");
        }
    }

    /**
     * Cuántos descriptores del proceso apuntan a ese fichero. {@code -1} si el sistema no
     * publica {@code /proc/self/fd}, que es la forma de decir «aquí no se puede medir».
     */
    private static long descriptoresSobre(Path fichero) throws Exception {
        Path fds = Path.of("/proc/self/fd");
        if (!Files.isDirectory(fds)) {
            return -1;
        }
        try (var listado = Files.list(fds)) {
            return listado.filter(fd -> apuntaA(fd, fichero)).count();
        }
    }

    private static boolean apuntaA(Path descriptor, Path fichero) {
        try {
            return Files.readSymbolicLink(descriptor).toString().equals(fichero.toString());
        } catch (Exception e) {
            // Un descriptor que se cierra mientras se lee el listado: no es el que se busca.
            return false;
        }
    }

    /** Sin {@code /proc} no hay medición posible; se dice y se para, no se finge un verde. */
    private static void assumeQueSePuedeMedir(long cuenta) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                cuenta >= 0, "este sistema no publica /proc/self/fd: el cierre no se puede medir");
    }

    /** Deja en la carpeta sólo el fichero dado, para que cada caso parta de lo mismo. */
    private static void borrarTodoMenos(String superviviente) throws Exception {
        try (var listado = Files.list(carpeta)) {
            for (Path fichero : listado.toList()) {
                if (!fichero.getFileName().toString().equals(superviviente)) {
                    Files.deleteIfExists(fichero);
                }
            }
        }
    }
}
