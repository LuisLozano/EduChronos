package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import es.yaroki.educhronos.app.web.dto.CuadreDTO;
import es.yaroki.educhronos.app.web.dto.CuadreEntidadDTO;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.ProfesorTutoria;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.RolTutoria;
import es.yaroki.educhronos.solver.domain.SesionBloqueada;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests del NÚCLEO de la pre-validación (Fase 8, Bloque 8.4-A, deuda D18):
 * {@link PrevalidacionService#prevalidar(ProblemaHorario, DatosCuadre)}, el método estático puro.
 * Se ejercita con {@code ProblemaHorario} construidos a mano —no hay JPA, ni Spring, ni
 * solver— porque es exactamente la entrada que el servicio recibe en producción tras
 * {@code cargarProblema()}.
 *
 * <p>La cuarta regla (S8, tutorías) no es aritmética: se delega en
 * {@link VerificadorSolucion}. Sus fixtures se calibran por otra vía —el resto de reglas
 * DEBE quedar callado— para que el hallazgo aseverado no pueda venir de otra comprobación.
 *
 * <p>Los fixtures están CALIBRADOS: en cada uno, la magnitud que se asevera cambia si la
 * regla se implementa mal (frontera {@code <} en vez de {@code <=}, conteo por plaza en
 * vez de por actividad, o (d) sin filtrar por patrón). Un fixture que pasara con ambas
 * implementaciones no discriminaría nada.
 */
class PrevalidacionServiceTest {

    private static final Asignatura MAT = new Asignatura("Mat", "Matemáticas");
    private static final Aula A1 = new Aula("A1", "A1");
    private static final Aula A2 = new Aula("A2", "A2");
    private static final Aula A3 = new Aula("A3", "A3");

    // ---------------------------------------------------------------- (a) profesor

    /**
     * (A1) FRONTERA de (a): demanda EXACTAMENTE IGUAL a disponible NO es un fallo. La
     * comparación es {@code demanda > disponible}, no {@code >=}. Con 5 tramos lectivos y
     * 1 restricción DURA el profesor dispone de 4, y una actividad de 4 tramos los llena
     * justo: cero avisos. Este caso cae si alguien escribe {@code >=}.
     */
    @Test
    void profesorConDemandaIgualADisponible_noProduceAviso() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 1);

        ProblemaHorario problema = problema(
                tramos, List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of(dura(mat1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    /**
     * (A1, hermano) Un tramo más de demanda —5 contra los mismos 4 disponibles— y sí
     * falla. Asevera los DOS números del aviso, no solo que la lista no esté vacía: 5 de
     * demanda y 4 de disponible (los 5 tramos lectivos menos la restricción DURA). Si la
     * resta de las DURA se omitiera, {@code disponible} valdría 5 y no habría aviso.
     */
    @Test
    void profesorConDemandaUnoPorEncima_produceErrorConLosDosNumeros() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 1);

        ProblemaHorario problema = problema(
                tramos, List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 5, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of(dura(mat1, tramos.get(0))));

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_PROFESOR_SOBRECARGADO);
            assertThat(a.entidadCodigo()).isEqualTo("MAT1");
            assertThat(a.demanda()).isEqualTo(5);
            assertThat(a.disponible()).isEqualTo(4);
        });
    }

    /**
     * (A2) NO TAUTOLÓGICO: dos profesores, uno sobrecargado y otro no. El aserto exige que
     * el aviso nombre a MAT1 y que LEN1 NO aparezca en ningún aviso de la regla. Una
     * implementación que marcara a todos los profesores, o que emitiera el aviso sin
     * atribuirlo, pasaría un "la lista no está vacía" pero cae aquí.
     */
    @Test
    void conDosProfesores_soloSeñalaAlSobrecargadoYPorSuCodigo() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo g1 = grupo("1ºA");
        GrupoAdministrativo g2 = grupo("1ºB");
        Subgrupo sgA = new Subgrupo("1ºA-Completo", Set.of(g1));
        Subgrupo sgB = new Subgrupo("1ºB-Completo", Set.of(g2));

        ProblemaHorario problema = problema(
                tramosEnDias(5, 1), List.of(mat1, len1), List.of(g1, g2), List.of(sgA, sgB),
                List.of(
                        actividad("Mat-1ºA", 6, 1, PatronTemporal.NEUTRA,
                                plaza("Mat-1ºA-P1", mat1, sgA)),
                        actividad("Len-1ºB", 3, 1, PatronTemporal.NEUTRA,
                                plaza("Len-1ºB-P1", len1, sgB))),
                List.of());

        List<AvisoPrevalidacion> deProfesor = soloRegla(
                PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO),
                PrevalidacionService.REGLA_PROFESOR_SOBRECARGADO);

        assertThat(deProfesor).singleElement()
                .extracting(AvisoPrevalidacion::entidadCodigo).isEqualTo("MAT1");
        assertThat(deProfesor).extracting(AvisoPrevalidacion::entidadCodigo)
                .doesNotContain("LEN1");
    }

    // ------------------------------------------------------------------- (c) grupo

    /**
     * (A3) EL ASERTO DEL BLOQUE: la deduplicación POR ACTIVIDAD de (c).
     *
     * <p>Fixture calibrado para que las dos cuentas den valores DISTINTOS y AMBAS superen
     * el techo, de modo que el aserto pueda mirar el VALOR y no la mera presencia:
     * <ul>
     *   <li>{@code Ing-desdoble}: UNA actividad, 3 repeticiones, DOS plazas con subgrupos
     *       DISTINTOS (1ºA-Desd1 y 1ºA-Desd2) del MISMO grupo 1ºA. Por actividad aporta
     *       3; por plaza aportaría 3+3 = 6.</li>
     *   <li>{@code Mat-1ºA}: una actividad ordinaria de 4 repeticiones sobre el mismo
     *       grupo. Aporta 4 en cualquiera de las dos cuentas.</li>
     * </ul>
     * Por actividad: 3 + 4 = <b>7</b>. Por plaza: 3 + 3 + 4 = <b>10</b>. Con 5 tramos
     * lectivos las dos superan el techo y hay aviso en ambos casos, así que asertar "hay
     * aviso" NO discriminaría; lo que discrimina es {@code demanda == 7}.
     *
     * <p>Los tres profesores (ING1, ING2, MAT1) están por debajo de su techo a propósito:
     * el test aísla (c) y no puede pasar por casualidad gracias a un error de (a).
     */
    @Test
    void grupoConDesdoble_cuentaLaActividadUnaVezAunqueTengaDosPlazas() {
        Profesor ing1 = new Profesor("ING1", "Uno");
        Profesor ing2 = new Profesor("ING2", "Dos");
        Profesor mat1 = new Profesor("MAT1", "Tres");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo desd1 = new Subgrupo("1ºA-Desd1", Set.of(grupo));
        Subgrupo desd2 = new Subgrupo("1ºA-Desd2", Set.of(grupo));
        Subgrupo completo = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(5, 1), List.of(ing1, ing2, mat1), List.of(grupo),
                List.of(desd1, desd2, completo),
                List.of(
                        // Cada plaza del desdoble en su aula (S207): con las dos en A1 el reparto de
                        // aulas sería imposible y este test dejaría de aislar la regla (c).
                        actividad("Ing-desdoble", 3, 1, PatronTemporal.NEUTRA,
                                plaza("Ing-desdoble-P1", ing1, desd1),
                                plazaEn("Ing-desdoble-P2", ing2, desd2, A2)),
                        // Y Mat en otra aula (S209, T4): con 3 + 4 tramos en A1, la carga de aulas
                        // también saltaría y el test dejaría de aislar (c).
                        actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                                plazaEn("Mat-1ºA-P1", mat1, completo, A3))),
                List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(soloRegla(avisos, PrevalidacionService.REGLA_GRUPO_SOBRECARGADO))
                .singleElement().satisfies(a -> {
                    assertThat(a.entidadCodigo()).isEqualTo("1ºA");
                    // 7 = 3 (la actividad de desdoble UNA vez) + 4. Por plaza daría 10.
                    assertThat(a.demanda()).isEqualTo(7);
                    assertThat(a.disponible()).isEqualTo(5);
                    assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
                });
        // AISLAMIENTO: ese es el ÚNICO hallazgo. Ni (a) ni (d) dicen nada con este
        // fixture (los tres profesores caben, ninguna actividad es DISTRIBUIDA), así que
        // el aserto de valor de arriba no puede quedar satisfecho por un aviso ajeno.
        assertThat(avisos).hasSize(1);
    }

    // -------------------------------------------------------------- (d) repeticiones

    /**
     * (A5) (d): una actividad DISTRIBUIDA con más repeticiones que días lectivos es ERROR
     * y el aviso NOMBRA a la actividad. Fixture con 6 tramos repartidos en 3 días, para
     * que el techo de (d) —3 días— y el de (a)/(c) —6 tramos— sean DISTINTOS: así el
     * único aviso posible es el de (d) y el test no puede pasar por un fallo de otra regla.
     */
    @Test
    void actividadDistribuidaConMasRepeticionesQueDias_produceErrorQueNombraLaActividad() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(3, 2), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.DISTRIBUIDA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_REPETICIONES_EXCEDEN_DIAS);
            assertThat(a.entidadCodigo()).isEqualTo("Mat-1ºA");
            assertThat(a.demanda()).isEqualTo(4);
            assertThat(a.disponible()).isEqualTo(3);
            assertThat(a.descripcion()).contains("Mat-1ºA");
        });
    }

    /**
     * (A5, hermano) La MISMA aritmética con patrón NEUTRA no produce nada: (d) solo mira
     * las DISTRIBUIDA, porque {@code ModeloCpSat:1161} descarta el resto antes de llegar a
     * la guarda anti-palomar de {@code :1164}. Repetir 4 veces en 3 días es legal para una
     * NEUTRA (dos repeticiones comparten día a propósito). Este test es el que cae si
     * alguien "simplifica" quitando el filtro de patrón.
     */
    @Test
    void actividadNeutraConMasRepeticionesQueDias_noProduceAviso() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(3, 2), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    /** Catálogo sano: ninguna de las tres reglas dispara. */
    @Test
    void catalogoSano_noProduceNingunAviso() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(5, 6), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.DISTRIBUIDA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    // ----------------------------------------------------------------- (S8) tutorías

    /**
     * (P1) S8 violada: la actividad {@code requiereTutor} la imparte LEN1, que no es
     * TUTOR_PRINCIPAL de nada. Asevera los SEIS campos del hallazgo, no su mera presencia.
     *
     * <p>Fixture calibrado para que sea el ÚNICO posible: 5 tramos en 5 días contra una
     * sola actividad de 1×1, así que (a) ve 1≤5, (c) ve 1≤5 y (d) ni mira (NEUTRA). El
     * {@code hasSize(1)} final lo fija: el aviso aseverado no puede venir de otra regla.
     *
     * <p>La descripción esperada se PIDE AL VERIFICADOR en vez de copiarse como literal:
     * así el aserto comprueba que el mapeo la pasa TAL CUAL, y no se convierte en un
     * espejo del texto del solver que habría que mantener a mano.
     */
    @Test
    void actividadRequiereTutorSinTutorPrincipal_produceAvisoQueNombraLaActividad() {
        Profesor len1 = new Profesor("LEN1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(5, 1), List.of(len1), List.of(grupo), List.of(sg),
                List.of(actividadTutorial("Tut-1ºA", len1, sg)),
                List.of(), List.of());

        String descripcionDelSolver = new VerificadorSolucion()
                .verificarTutorias(problema).get(0).descripcion();

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.AVISO);
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_TUTORIA_SIN_TUTOR);
            // La entidad es la ACTIVIDAD, no el grupo "1ºA" que lleva el recursoCodigo.
            assertThat(a.entidadCodigo()).isEqualTo("Tut-1ºA");
            assertThat(a.demanda()).isEqualTo(1);
            assertThat(a.disponible()).isEqualTo(0);
            assertThat(a.descripcion()).isEqualTo(descripcionDelSolver);
        });
    }

    /**
     * (P2) ORDEN: con un ERROR de (d) y un AVISO de S8 en el mismo catálogo, S8 va LA
     * ÚLTIMA. Se asevera por ÍNDICE, no por contenido del conjunto: un orden invertido
     * pasaría cualquier aserto de pertenencia y cae aquí.
     *
     * <p>Calibrado para que (a) y (c) callen: 6 tramos en 3 días; MAT1 demanda 4≤6, LEN1
     * demanda 1≤6, y el grupo 1ºA acumula 4+1 = 5 ≤ 6. Los dos únicos hallazgos son los
     * buscados, y el {@code hasSize(2)} lo fija.
     */
    @Test
    void conUnErrorYUnaS8_laS8VaLaUltima() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(3, 2), List.of(mat1, len1), List.of(grupo), List.of(sg),
                List.of(
                        actividad("Mat-1ºA", 4, 1, PatronTemporal.DISTRIBUIDA,
                                plaza("Mat-1ºA-P1", mat1, sg)),   // (d): 4 repeticiones > 3 días
                        actividadTutorial("Tut-1ºA", len1, sg)),  // S8: LEN1 no es tutor
                List.of(), List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(avisos).hasSize(2);
        assertThat(avisos.get(0).regla())
                .isEqualTo(PrevalidacionService.REGLA_REPETICIONES_EXCEDEN_DIAS);
        assertThat(avisos.get(0).severidad()).isEqualTo(Severidad.ERROR);
        assertThat(avisos.get(1).regla())
                .isEqualTo(PrevalidacionService.REGLA_TUTORIA_SIN_TUTOR);
        assertThat(avisos.get(1).severidad()).isEqualTo(Severidad.AVISO);
    }

    /**
     * (P3, hermano de P1) MISMO fixture salvo la fila de tutoría: LEN1 SÍ es
     * TUTOR_PRINCIPAL de 1ºA. Ningún hallazgo. Es el par discriminante de P1: sin él,
     * una regla que avisara de TODA actividad {@code requiereTutor} pasaría P1.
     */
    @Test
    void actividadRequiereTutorConTutorPrincipal_noProduceAviso() {
        Profesor len1 = new Profesor("LEN1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));

        ProblemaHorario problema = problema(
                tramosEnDias(5, 1), List.of(len1), List.of(grupo), List.of(sg),
                List.of(actividadTutorial("Tut-1ºA", len1, sg)),
                List.of(),
                List.of(new ProfesorTutoria(len1, grupo, RolTutoria.TUTOR_PRINCIPAL)));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    // ------------------------------------ restricciones horarias con bloques (S166)

    /**
     * GUARDA (S166): restricciones horarias y bloques conviven. MAT1 tiene una DURA y una
     * BLANDA e imparte Mat-1ºA de 2 tramos seguidos, y la pre-validación no da NINGÚN
     * ERROR. Hasta S166 lo impedía el cortafuegos RESTRICCION_HORARIA_CON_BLOQUE (S165),
     * que se retiró cuando el solver pasó a vetar y penalizar todos los tramos que ocupa
     * cada sesión. Calibrado como el resto del bloque: 30 tramos en 5 días; MAT1 demanda
     * 2×2 = 4 ≤ 30 − 1; el grupo, 4 ≤ 30; NEUTRA.
     */
    @Test
    void profesorConDuraYBlandaQueImparteUnBloque_noProduceNingunError() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);

        ProblemaHorario problema = problema(
                tramos, List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 2, 2, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))),
                List.of(dura(mat1, tramos.get(1)), blanda(mat1, tramos.get(3))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO))
                .filteredOn(a -> a.severidad() == Severidad.ERROR)
                .isEmpty();
    }

    /**
     * (E6) ORDEN: un ERROR va ANTES que S8 (AVISO), que sigue la última. Se asevera por
     * índice. Hasta S166 el ERROR lo ponía el cortafuegos de restricciones con bloques;
     * retirado, lo pone un pin de Mat-1ºA #1 sobre la DURA de MAT1 (regla (f)), que se
     * computa antes que S8. LEN1 imparte una tutoría sin ser tutor. Grupo 1ºA:
     * 2×2 + 1 = 5 ≤ 30; MAT1 demanda 4 ≤ 30 − 1.
     */
    @Test
    void conUnErrorYUnaS8_elErrorVaAntesQueS8() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad mat = actividad("Mat-1ºA", 2, 2, PatronTemporal.NEUTRA,
                plaza("Mat-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1, len1), List.of(grupo), List.of(sg),
                List.of(mat, actividadTutorial("Tut-1ºA", len1, sg)),
                List.of(dura(mat1, tramos.get(0))),
                List.of(pin(mat, 1, tramos.get(0))));

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(avisos).extracting(AvisoPrevalidacion::regla).containsExactly(
                PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA,
                PrevalidacionService.REGLA_TUTORIA_SIN_TUTOR);
    }

    // ------------------------------------------------------ (f) pin sobre tramo DURA

    /**
     * (F1) Pin sobre un tramo DURA de su profesor: UN error que señala a MAT1 y cuya
     * descripción nombra la sesión (Mat-1ºA #1) y el tramo (D1T1). Demanda 1 (el tramo que
     * pide el pin) contra disponible 0. Calibrado para que el resto calle: 30 tramos,
     * MAT1 demanda 1 ≤ 30 − 1, grupo 1 ≤ 30, duración 1.
     */
    @Test
    void pinSobreTramoDuraDeSuProfesor_produceErrorQueNombraProfesorSesionYTramo() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad mat = actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                plaza("Mat-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(mat),
                List.of(dura(mat1, tramos.get(0))),
                List.of(pin(mat, 1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA);
            assertThat(a.entidadCodigo()).isEqualTo("MAT1");
            assertThat(a.demanda()).isEqualTo(1);
            assertThat(a.disponible()).isEqualTo(0);
            assertThat(a.descripcion()).contains("'Mat-1ºA' #1").contains("D1T1")
                    .contains("'MAT1'");
        });
    }

    /** (F2) Pin sobre un tramo LIBRE (la DURA está en D1T2 y el pin en D1T1): nada. */
    @Test
    void pinSobreTramoLibre_noProduceAviso() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad mat = actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                plaza("Mat-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(mat),
                List.of(dura(mat1, tramos.get(1))),
                List.of(pin(mat, 1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    /**
     * (F3) Pin sobre un tramo BLANDA: nada. La blanda es preferencia; el pin la incumple
     * a sabiendas y el optimizador lo paga en el objetivo, no es infactible.
     */
    @Test
    void pinSobreTramoBlanda_noProduceAviso() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad mat = actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                plaza("Mat-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(mat),
                List.of(blanda(mat1, tramos.get(0))),
                List.of(pin(mat, 1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    /**
     * (F4) Co-docencia: MAT1 y LEN1 en la MISMA plaza, pin en D1T1, y solo LEN1 tiene
     * DURA ahí. UN error, a nombre de LEN1 (no de MAT1). Cada uno demanda 1 ≤ 30 − 1.
     */
    @Test
    void pinEnCoDocenciaConDuraSoloDeUno_errorANombreDelProfesorVetado() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Plaza coDocencia = new Plaza("Amb-1ºA-P1", MAT, Set.of(mat1, len1),
                Optional.of(A1), Set.of(), Set.of(sg));
        Actividad amb = actividad("Amb-1ºA", 1, 1, PatronTemporal.NEUTRA, coDocencia);

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1, len1), List.of(grupo), List.of(sg), List.of(amb),
                List.of(dura(len1, tramos.get(0))),
                List.of(pin(amb, 1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).singleElement().satisfies(a -> {
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA);
            assertThat(a.entidadCodigo()).isEqualTo("LEN1");
            assertThat(a.descripcion()).contains("'LEN1'").doesNotContain("MAT1");
        });
    }

    /**
     * (F5) Tramos OCUPADOS, no solo el del pin: un bloque de 2 fijado en D1T1 ocupa D1T1
     * y D1T2, y la DURA está en D1T2. UN error del pin que nombra D1T2, y es el ÚNICO
     * hallazgo: hasta S166 lo precedía el cortafuegos de restricciones con bloques, ya
     * retirado. MAT1 demanda 2 ≤ 30 − 1.
     */
    @Test
    void pinDeUnBloqueCuyoTramoInteriorEsDura_produceErrorQueNombraElInterior() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad bloque = actividad("Lab-1ºA", 1, 2, PatronTemporal.NEUTRA,
                plaza("Lab-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(bloque),
                List.of(dura(mat1, tramos.get(1))),
                List.of(pin(bloque, 1, tramos.get(0))));

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO);

        assertThat(avisos).extracting(AvisoPrevalidacion::regla).containsExactly(
                PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA);
        assertThat(avisos.get(0).descripcion()).contains("D1T1").contains("D1T2");
    }

    /**
     * (F6) Dos filas DURA iguales (MAT1, D1T1) y un pin ahí: UN error, no dos. El veto es
     * el hecho, no la fila; mismo criterio que el verificador y la regla (a).
     */
    @Test
    void pinSobreDuraRepetida_unSoloError() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Actividad mat = actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                plaza("Mat-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(mat),
                List.of(dura(mat1, tramos.get(0)), dura(mat1, tramos.get(0))),
                List.of(pin(mat, 1, tramos.get(0))));

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO),
                PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA)).hasSize(1);
    }

    /**
     * (F7) Bloque IMPOSIBLE fijado por un pin: Lab-1ºA de 2 tramos fijado en D1T6, el
     * último del día, desborda (no existe D1T7). Sin tramos ocupados que reconstruir, se
     * mira el tramo del pin, y MAT1 tiene DURA en D1T6 → UN error del pin. Si el respaldo
     * fuera «ningún tramo», este pin contradictorio pasaría callado.
     */
    @Test
    void pinDeUnBloqueImposibleSobreDura_miraElTramoDelPin() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 6);
        Tramo d1t6 = tramos.get(5);
        Actividad bloque = actividad("Lab-1ºA", 1, 2, PatronTemporal.NEUTRA,
                plaza("Lab-1ºA-P1", mat1, sg));

        ProblemaHorario problema = problemaConPines(
                tramos, List.of(mat1), List.of(grupo), List.of(sg), List.of(bloque),
                List.of(dura(mat1, d1t6)),
                List.of(pin(bloque, 1, d1t6)));

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO),
                PrevalidacionService.REGLA_PIN_SOBRE_TRAMO_DURA))
                .singleElement().extracting(AvisoPrevalidacion::descripcion)
                .asString().contains("D1T6");
    }

    // ------------------------------------------------- (S203) cuadre de horas declaradas

    private static final String PROF_DESCUADRADO = PrevalidacionService.REGLA_PROFESOR_HORAS_DESCUADRADAS;
    private static final String GRUPO_DESCUADRADO = PrevalidacionService.REGLA_GRUPO_HORAS_DESCUADRADAS;

    /**
     * (C1) EXCESO: MAT1 tiene 4 horas de clase configuradas y declara 3. AVISO a su nombre con
     * las configuradas como demanda y las declaradas como disponible. 5 tramos en 5 días y una
     * NEUTRA de 4: ninguna otra regla habla, y el {@code hasSize(1)} lo fija.
     */
    @Test
    void cuadre_exceso_avisaConConfiguradasYDeclaradas() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(
                problema, datos(Map.of("MAT1", 3), Map.of(), Set.of()));

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.AVISO);
            assertThat(a.regla()).isEqualTo(PROF_DESCUADRADO);
            assertThat(a.entidadCodigo()).isEqualTo("MAT1");
            assertThat(a.demanda()).isEqualTo(4);
            assertThat(a.disponible()).isEqualTo(3);
        });
    }

    /** (C2) DEFECTO: 2 configuradas y 3 declaradas también avisa. Mata «!=» → «>». */
    @Test
    void cuadre_defecto_avisa() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 2, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        assertThat(PrevalidacionService.prevalidar(problema, datos(Map.of("MAT1", 3), Map.of(), Set.of())))
                .singleElement().satisfies(a -> {
                    assertThat(a.regla()).isEqualTo(PROF_DESCUADRADO);
                    assertThat(a.demanda()).isEqualTo(2);
                    assertThat(a.disponible()).isEqualTo(3);
                });
    }

    /** (C3) La igualdad cuadra: 3 y 3 no avisan, ni el profesor ni el grupo. */
    @Test
    void cuadre_igualdad_noAvisa() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 3, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        assertThat(PrevalidacionService.prevalidar(
                problema, datos(Map.of("MAT1", 3), Map.of("1ºA", 3), Set.of()))).isEmpty();
    }

    /** (C4) SIN DECLARAR no avisa: sin total no hay con qué cuadrar, tenga las horas que tenga. */
    @Test
    void cuadre_sinDeclarar_noAvisa() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        assertThat(PrevalidacionService.prevalidar(problema, datos(Map.of(), Map.of(), Set.of())))
                .isEmpty();
    }

    /**
     * (C5) DECLARADO SIN ACTIVIDADES avisa con 0 configuradas: LEN1 y el grupo 1ºB declaran
     * horas y no figuran en ninguna plaza. Mata recorrer solo las entidades con actividades.
     */
    @Test
    void cuadre_declaradoSinActividades_avisaConCero() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo grupoA = grupo("1ºA");
        GrupoAdministrativo grupoB = grupo("1ºB");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupoA));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1, len1),
                List.of(grupoA, grupoB), List.of(sg),
                List.of(actividad("Mat-1ºA", 3, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(
                problema, datos(Map.of("LEN1", 18), Map.of("1ºB", 30), Set.of()));

        assertThat(avisos).extracting(AvisoPrevalidacion::regla, AvisoPrevalidacion::entidadCodigo,
                        AvisoPrevalidacion::demanda, AvisoPrevalidacion::disponible)
                .containsExactly(
                        tuple(PROF_DESCUADRADO, "LEN1", 0, 18),
                        tuple(GRUPO_DESCUADRADO, "1ºB", 0, 30));
    }

    /**
     * (C6) SOLO CUENTA CLASE: MAT1 da 3 de clase y 2 de reunión de departamento, y declara 3.
     * La reunión le ocupa —(a) la cuenta— pero no es hora de clase: el cuadre casa. Con
     * «todas» serían 5 ≠ 3. La reunión no tiene subgrupos ni aula (S201).
     */
    @Test
    void cuadre_soloCuentaClase() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg),
                List.of(actividad("Mat-1ºA", 3, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg)),
                        actividad("Reu-Dpto", 2, 1, PatronTemporal.NEUTRA, plazaSinAlumnos("Reu-Dpto-P1", mat1))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(
                problema, datos(Map.of("MAT1", 3), Map.of("1ºA", 3), Set.of("Reu-Dpto")))).isEmpty();
    }

    /**
     * (C7) UNA VEZ POR ACTIVIDAD con plazas simultáneas: MAT1 está en las dos plazas de un
     * desdoble de 3 repeticiones y 1ºA en sus dos subgrupos. Profesor y grupo ocupan 3, no 6, y
     * declaran 3: nada que avisar. Contando por plaza los dos avisarían con 6.
     */
    @Test
    void cuadre_unaVezPorActividadConPlazasSimultaneas() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo desd1 = new Subgrupo("1ºA-Desd1", Set.of(grupo));
        Subgrupo desd2 = new Subgrupo("1ºA-Desd2", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(desd1, desd2),
                // Cada plaza en su aula (S207), para no disparar el reparto de aulas imposible.
                List.of(actividad("Mat-desdoble", 3, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-desdoble-P1", mat1, desd1), plazaEn("Mat-desdoble-P2", mat1, desd2, A2))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(
                problema, datos(Map.of("MAT1", 3), Map.of("1ºA", 3), Set.of()))).isEmpty();
    }

    /**
     * (C8) LA SOBRECARGA SIGUE CONTANDO LAS REUNIONES: con 5 tramos, MAT1 da 4 de clase y 2 de
     * reunión: (a) ve 6 &gt; 5 aunque los datos de cuadre marquen la reunión como no CLASE. Mata
     * que (a) use el filtro «solo CLASE» (vería 4 ≤ 5 y callaría).
     */
    @Test
    void sobrecarga_sigueContandoLasReuniones() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg)),
                        actividad("Reu-Dpto", 2, 1, PatronTemporal.NEUTRA, plazaSinAlumnos("Reu-Dpto-P1", mat1))),
                List.of());

        assertThat(soloRegla(PrevalidacionService.prevalidar(
                        problema, datos(Map.of(), Map.of(), Set.of("Reu-Dpto"))),
                PrevalidacionService.REGLA_PROFESOR_SOBRECARGADO))
                .singleElement().satisfies(a -> {
                    assertThat(a.entidadCodigo()).isEqualTo("MAT1");
                    assertThat(a.demanda()).isEqualTo(6);
                    assertThat(a.disponible()).isEqualTo(5);
                });
    }

    /**
     * (C9) ORDEN Y SEVERIDAD: un ERROR de (d), un AVISO de S8 y un descuadre de profesor y otro
     * de grupo. Por índice: el ERROR, S8, el profesor y el grupo; los tres últimos AVISO. Mata
     * las reglas de cuadre como ERROR y el orden invertido entre ellas o respecto de S8.
     *
     * <p>Calibrado como (P2): 6 tramos en 3 días; MAT1 4 de una DISTRIBUIDA (4 &gt; 3 días) y
     * LEN1 1 tutorial sin ser tutor; el grupo suma 5 ≤ 6. MAT1 declara 2 y 1ºA declara 4.
     */
    @Test
    void cuadre_esAvisoYVaDespuesDeS8_profesorAntesQueGrupo() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(
                tramosEnDias(3, 2), List.of(mat1, len1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.DISTRIBUIDA, plaza("Mat-1ºA-P1", mat1, sg)),
                        actividadTutorial("Tut-1ºA", len1, sg)),
                List.of(), List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(
                problema, datos(Map.of("MAT1", 2), Map.of("1ºA", 4), Set.of()));

        assertThat(avisos).extracting(AvisoPrevalidacion::regla, AvisoPrevalidacion::severidad)
                .containsExactly(
                        tuple(PrevalidacionService.REGLA_REPETICIONES_EXCEDEN_DIAS, Severidad.ERROR),
                        tuple(PrevalidacionService.REGLA_TUTORIA_SIN_TUTOR, Severidad.AVISO),
                        tuple(PROF_DESCUADRADO, Severidad.AVISO),
                        tuple(GRUPO_DESCUADRADO, Severidad.AVISO));
    }

    /** (C10) Las dos descripciones, literales (T2.4): {código}: {c} … {d} declaradas. */
    @Test
    void cuadre_descripcionesLiterales() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        assertThat(PrevalidacionService.prevalidar(
                        problema, datos(Map.of("MAT1", 3), Map.of("1ºA", 5), Set.of())))
                .extracting(AvisoPrevalidacion::descripcion)
                .containsExactly(
                        "MAT1: 4 horas de clase configuradas y 3 declaradas.",
                        "1ºA: 4 horas configuradas y 5 declaradas.");
    }

    /**
     * (C11) UN PDC CUENTA SOLO SUS ACTIVIDADES: 1ºA tiene 4 horas propias y su PDC 1ºADi 2
     * propias; cada uno declara las suyas y ninguno avisa. Si el PDC heredara las del padre
     * vería 6 ≠ 2.
     */
    @Test
    void cuadre_pdcConTotal_cuentaSoloSusActividades() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        GrupoAdministrativo padre = grupo("1ºA");
        GrupoAdministrativo pdc = new GrupoAdministrativo(
                "1ºADi", TipoGrupo.DIVERSIFICACION_PDC, Optional.of(padre));
        Subgrupo sgPadre = new Subgrupo("1ºA-Completo", Set.of(padre));
        Subgrupo sgPdc = new Subgrupo("1ºADi-Completo", Set.of(pdc));
        ProblemaHorario problema = problema(tramosEnDias(5, 2), List.of(mat1, len1), List.of(padre, pdc),
                List.of(sgPadre, sgPdc),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sgPadre)),
                        actividad("Amb-1ºADi", 2, 1, PatronTemporal.NEUTRA, plaza("Amb-1ºADi-P1", len1, sgPdc))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(
                problema, datos(Map.of(), Map.of("1ºA", 4, "1ºADi", 2), Set.of()))).isEmpty();
    }

    /**
     * (C12) EL CUADRE DEL GET lista TODAS las entidades, en el orden del problema: las que no
     * tienen total salen con {@code declaradas} null y {@code descuadre} false; las que lo
     * tienen, con su marca. Igualdad completa de los registros.
     */
    @Test
    void cuadreGet_listaTodasLasEntidades_lasSinTotalConNullYFalse() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        Profesor ing1 = new Profesor("ING1", "Tres");
        GrupoAdministrativo grupoA = grupo("1ºA");
        GrupoAdministrativo grupoB = grupo("1ºB");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupoA));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1, len1, ing1),
                List.of(grupoA, grupoB), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        CuadreDTO cuadre = PrevalidacionService.cuadre(
                problema, datos(Map.of("MAT1", 3, "ING1", 0), Map.of("1ºA", 4), Set.of()));

        assertThat(cuadre.profesores()).containsExactly(
                new CuadreEntidadDTO("MAT1", 4, 3, true),
                new CuadreEntidadDTO("LEN1", 0, null, false),
                new CuadreEntidadDTO("ING1", 0, 0, false));
        assertThat(cuadre.grupos()).containsExactly(
                new CuadreEntidadDTO("1ºA", 4, 4, false),
                new CuadreEntidadDTO("1ºB", 0, null, false));
    }

    /**
     * (C13) EL GET DE CUADRE COINCIDE CON LOS AVISO: en un catálogo con exceso, defecto, cuadre
     * exacto, sin total y sin actividades, las entidades marcadas son exactamente las que
     * avisan, con las mismas cifras (configuradas = demanda, declaradas = disponible).
     */
    @Test
    void cuadreGet_coincideConLosAvisos() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        Profesor len1 = new Profesor("LEN1", "Dos");
        Profesor ing1 = new Profesor("ING1", "Tres");
        Profesor fis1 = new Profesor("FIS1", "Cuatro");
        GrupoAdministrativo grupoA = grupo("1ºA");
        GrupoAdministrativo grupoB = grupo("1ºB");
        Subgrupo sgA = new Subgrupo("1ºA-Completo", Set.of(grupoA));
        Subgrupo sgB = new Subgrupo("1ºB-Completo", Set.of(grupoB));
        ProblemaHorario problema = problema(tramosEnDias(5, 2), List.of(mat1, len1, ing1, fis1),
                List.of(grupoA, grupoB), List.of(sgA, sgB),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sgA)),
                        actividad("Len-1ºB", 2, 1, PatronTemporal.NEUTRA, plaza("Len-1ºB-P1", len1, sgB))),
                List.of());
        DatosCuadre datos = datos(Map.of("MAT1", 3, "LEN1", 2, "FIS1", 5), Map.of("1ºA", 5), Set.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, datos);
        CuadreDTO cuadre = PrevalidacionService.cuadre(problema, datos);

        List<String> marcadosProfesor = cuadre.profesores().stream().filter(CuadreEntidadDTO::descuadre)
                .map(e -> e.codigo() + "|" + e.configuradas() + "|" + e.declaradas()).toList();
        List<String> marcadosGrupo = cuadre.grupos().stream().filter(CuadreEntidadDTO::descuadre)
                .map(e -> e.codigo() + "|" + e.configuradas() + "|" + e.declaradas()).toList();
        assertThat(marcadosProfesor).containsExactly("MAT1|4|3", "FIS1|0|5");
        assertThat(marcadosGrupo).containsExactly("1ºA|4|5");
        assertThat(soloRegla(avisos, PROF_DESCUADRADO).stream()
                .map(a -> a.entidadCodigo() + "|" + a.demanda() + "|" + a.disponible()).toList())
                .isEqualTo(marcadosProfesor);
        assertThat(soloRegla(avisos, GRUPO_DESCUADRADO).stream()
                .map(a -> a.entidadCodigo() + "|" + a.demanda() + "|" + a.disponible()).toList())
                .isEqualTo(marcadosGrupo);
    }

    /** {@code descuadra} sola, en sus cuatro casos: sin total nunca; con total, solo si difiere. */
    @Test
    void descuadra_sinTotalNunca_conTotalSoloSiDifiere() {
        assertThat(PrevalidacionService.descuadra(null, 0)).isFalse();
        assertThat(PrevalidacionService.descuadra(null, 7)).isFalse();
        assertThat(PrevalidacionService.descuadra(3, 3)).isFalse();
        assertThat(PrevalidacionService.descuadra(3, 2)).isTrue();
        assertThat(PrevalidacionService.descuadra(3, 4)).isTrue();
    }

    // ------------------------------------------------- (S207) aulas: B1 y B2

    /** (B1) Una plaza sin aula posible da UN ERROR que nombra actividad, plaza y motivo. */
    @Test
    void claseSinAulaPosible_unErrorConActividadPlazaYMotivo() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());
        DatosAulas aulas = new DatosAulas(List.of(new DatosAulas.PlazaSinAula(
                "Mat-1ºA", "Mat-1ºA-P1", "sin aula de grupo ni aulas de la asignatura")));

        List<AvisoPrevalidacion> avisos =
                PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, aulas);

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_CLASE_SIN_AULA_POSIBLE);
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.entidadCodigo()).isEqualTo("Mat-1ºA");
            assertThat(a.demanda()).isEqualTo(1);
            assertThat(a.disponible()).isZero();
            assertThat(a.descripcion())
                    .contains("Mat-1ºA-P1").contains("Mat-1ºA")
                    .contains("sin aula de grupo ni aulas de la asignatura");
        });
    }

    /** (B1, negativo) Sin plazas sin aula, ni la regla de dos argumentos ni la de tres dicen nada. */
    @Test
    void claseSinAulaPosible_sinPlazasSinAula_nada() {
        Profesor mat1 = new Profesor("MAT1", "Uno");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(mat1), List.of(grupo),
                List.of(sg), List.of(actividad("Mat-1ºA", 1, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-1ºA-P1", mat1, sg))), List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO)).isEmpty();
        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO)).isEmpty();
    }

    /** (B2) Dos plazas de una sesión fijas en la misma aula: ERROR con la actividad y el aula. */
    @Test
    void reparto_dosPlazasFijasEnLaMismaAula_error() {
        Profesor p1 = new Profesor("P1", "Uno");
        Profesor p2 = new Profesor("P2", "Dos");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo d1 = new Subgrupo("1ºA-D1", Set.of(grupo));
        Subgrupo d2 = new Subgrupo("1ºA-D2", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(p1, p2), List.of(grupo),
                List.of(d1, d2), List.of(actividad("Ing-desd", 1, 1, PatronTemporal.NEUTRA,
                        plaza("Ing-desd-P1", p1, d1), plaza("Ing-desd-P2", p2, d2))), List.of());

        List<AvisoPrevalidacion> avisos = soloRegla(
                PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO),
                PrevalidacionService.REGLA_REPARTO_DE_AULAS_IMPOSIBLE);

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.entidadCodigo()).isEqualTo("Ing-desd");
            assertThat(a.demanda()).isEqualTo(2);
            assertThat(a.disponible()).isEqualTo(1);
            assertThat(a.descripcion()).contains("Ing-desd").contains("A1");
        });
    }

    /**
     * (B2) Desdoble con una sola aula posible común: P1 y P2 solo pueden ir a A1 (una fija, otra
     * candidata única) y P3 tiene A1 o A2. Caben 2 de 3. El conteo de aulas distintas (A1, A2:
     * dos para tres plazas) también lo vería; el caso de abajo distingue el emparejamiento.
     */
    @Test
    void reparto_desdobleConUnaSolaAulaComun_error() {
        Profesor p1 = new Profesor("P1", "Uno");
        Profesor p2 = new Profesor("P2", "Dos");
        Profesor p3 = new Profesor("P3", "Tres");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo d1 = new Subgrupo("1ºA-D1", Set.of(grupo));
        Subgrupo d2 = new Subgrupo("1ºA-D2", Set.of(grupo));
        Subgrupo d3 = new Subgrupo("1ºA-D3", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(p1, p2, p3), List.of(grupo),
                List.of(d1, d2, d3), List.of(actividad("Ing-desd", 1, 1, PatronTemporal.NEUTRA,
                        plaza("Ing-desd-P1", p1, d1),
                        plazaCandidatas("Ing-desd-P2", p2, d2, A1),
                        plazaCandidatas("Ing-desd-P3", p3, d3, A1, A2))), List.of());

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO),
                PrevalidacionService.REGLA_REPARTO_DE_AULAS_IMPOSIBLE))
                .singleElement().satisfies(a -> {
                    assertThat(a.demanda()).isEqualTo(3);
                    assertThat(a.disponible()).isEqualTo(2);
                    assertThat(a.descripcion()).contains("A1").contains("A2");
                });
    }

    /**
     * (B2, negativo y discriminante) Tres plazas, tres aulas, pero el reparto solo sale por
     * caminos de aumento: P1 {A1, A2}, P2 {A1}, P3 {A2, A3}. Un reparto voraz que diera A1 a P1
     * dejaría a P2 sin aula; el emparejamiento máximo encuentra P1→A2, P2→A1, P3→A3.
     */
    @Test
    void reparto_posibleSoloConCaminosDeAumento_nada() {
        Profesor p1 = new Profesor("P1", "Uno");
        Profesor p2 = new Profesor("P2", "Dos");
        Profesor p3 = new Profesor("P3", "Tres");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo d1 = new Subgrupo("1ºA-D1", Set.of(grupo));
        Subgrupo d2 = new Subgrupo("1ºA-D2", Set.of(grupo));
        Subgrupo d3 = new Subgrupo("1ºA-D3", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(p1, p2, p3), List.of(grupo),
                List.of(d1, d2, d3), List.of(actividad("Ing-desd", 1, 1, PatronTemporal.NEUTRA,
                        plazaCandidatas("Ing-desd-P1", p1, d1, A1, A2),
                        plazaCandidatas("Ing-desd-P2", p2, d2, A1),
                        plazaCandidatas("Ing-desd-P3", p3, d3, A2, A3))), List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO)).isEmpty();
        assertThat(PrevalidacionService.emparejamientoMaximo(List.of(
                Set.of(A1, A2), Set.of(A1), Set.of(A2, A3)))).isEqualTo(3);
    }

    /**
     * (B2, discriminante del voraz) Dos casos simétricos: P1 {A1, A2} y, después, P2 y P3 que
     * necesitan el aula que P1 NO debe quedarse. Un reparto voraz sin caminos de aumento da a P1 la
     * primera aula que encuentre al recorrer su conjunto; sea cual sea, en uno de los dos casos es
     * la que necesita P2 y el voraz se queda en 2. El emparejamiento máximo da 3 en los dos.
     * (Mata el mutante B2-2 de S207 T2, que el caso de arriba dejaba vivo según el orden de
     * iteración del conjunto.)
     */
    @Test
    void reparto_elEmparejamientoReasigna_seaCualSeaLaPrimeraAulaQueTomeP1() {
        List<Set<Aula>> casoA1 = List.of(new HashSet<>(List.of(A1, A2)), new HashSet<>(List.of(A1)),
                new HashSet<>(List.of(A2, A3)));
        List<Set<Aula>> casoA2 = List.of(new HashSet<>(List.of(A1, A2)), new HashSet<>(List.of(A2)),
                new HashSet<>(List.of(A1, A3)));

        assertThat(PrevalidacionService.emparejamientoMaximo(casoA1)).isEqualTo(3);
        assertThat(PrevalidacionService.emparejamientoMaximo(casoA2)).isEqualTo(3);
    }

    /** (B2) Las plazas sin aula (reuniones) no necesitan aula: dos en una actividad, nada. */
    @Test
    void reparto_plazasSinAulaNoCuentan() {
        Profesor p1 = new Profesor("P1", "Uno");
        Profesor p2 = new Profesor("P2", "Dos");
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(p1, p2), List.of(), List.of(),
                List.of(actividad("Reu", 1, 1, PatronTemporal.NEUTRA,
                        plazaSinAlumnos("Reu-P1", p1), plazaSinAlumnos("Reu-P2", p2))), List.of());

        assertThat(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO)).isEmpty();
    }

    /**
     * (B2) Una actividad con una plaza sin aula posible no se evalúa: ya tiene su B1. Sus otras
     * dos plazas, fijas en A1, darían B2 si se evaluara.
     */
    @Test
    void reparto_noSeEvaluaEnUnaActividadQueYaTieneUnaClaseSinAula() {
        Profesor p1 = new Profesor("P1", "Uno");
        Profesor p2 = new Profesor("P2", "Dos");
        Profesor p3 = new Profesor("P3", "Tres");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo d1 = new Subgrupo("1ºA-D1", Set.of(grupo));
        Subgrupo d2 = new Subgrupo("1ºA-D2", Set.of(grupo));
        Subgrupo d3 = new Subgrupo("1ºA-D3", Set.of(grupo));
        Plaza sinAula = new Plaza("Ing-desd-P3", MAT, Set.of(p3), Optional.empty(), Set.of(), Set.of(d3));
        ProblemaHorario problema = problema(tramosEnDias(5, 1), List.of(p1, p2, p3), List.of(grupo),
                List.of(d1, d2, d3), List.of(actividad("Ing-desd", 1, 1, PatronTemporal.NEUTRA,
                        plaza("Ing-desd-P1", p1, d1), plaza("Ing-desd-P2", p2, d2), sinAula)), List.of());
        DatosAulas aulas = new DatosAulas(List.of(new DatosAulas.PlazaSinAula(
                "Ing-desd", "Ing-desd-P3", "sin aula de grupo ni aulas de la asignatura")));

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, aulas);

        assertThat(avisos).extracting(AvisoPrevalidacion::regla)
                .containsExactly(PrevalidacionService.REGLA_CLASE_SIN_AULA_POSIBLE);
        assertThat(soloRegla(PrevalidacionService.prevalidar(problema, DatosCuadre.VACIO, DatosAulas.VACIO),
                PrevalidacionService.REGLA_REPARTO_DE_AULAS_IMPOSIBLE))
                .as("discriminante: sin el B1 la actividad sí se evalúa").hasSize(1);
    }

    // ------------------------------------------------- guardias (S212, C-dato-guardias)

    private static final String INSUFICIENTES = PrevalidacionService.REGLA_GUARDIAS_INSUFICIENTES;
    private static final String SIN_HUECO = PrevalidacionService.REGLA_GUARDIAS_SIN_HUECO;

    /** P1, frontera: con m = 2 y T = 5 hacen falta 10, y 10 guardias bastan. Mata «<» → «<=». */
    @Test
    void guardiasInsuficientes_sumaIgualAlMinimoPorTramos_nada() {
        ProblemaHorario problema = problemaSinActividades(tramosEnDias(5, 1),
                new Profesor("MAT1", "Ana Ruiz"), new Profesor("LEN1", "Luis Gil"));

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema,
                guardias(2, Map.of("MAT1", 5, "LEN1", 5))), INSUFICIENTES)).isEmpty();
    }

    /** P1: una guardia por debajo, un ERROR del centro con los tres números en el texto. */
    @Test
    void guardiasInsuficientes_unaPorDebajo_unErrorDelCentroConLosTresNumeros() {
        ProblemaHorario problema = problemaSinActividades(tramosEnDias(5, 1),
                new Profesor("MAT1", "Ana Ruiz"), new Profesor("LEN1", "Luis Gil"));

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema,
                guardias(2, Map.of("MAT1", 5, "LEN1", 4))), INSUFICIENTES)).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.entidadCodigo()).isEqualTo("CENTRO");
            assertThat(a.demanda()).isEqualTo(10);
            assertThat(a.disponible()).isEqualTo(9);
            assertThat(a.descripcion()).isEqualTo("Las guardias ordinarias de los profesores suman 9 y hacen"
                    + " falta 10: 2 profesores de guardia en cada uno de los 5 tramos de clase. Añade guardias"
                    + " a los profesores o baja el mínimo por tramo.");
        });
    }

    /** P1: mínimo 0 es un centro sin guardias, y con 0 guardias no sale nada de nada. */
    @Test
    void guardiasInsuficientes_minimoCeroYNingunaGuardia_nada() {
        ProblemaHorario problema = problemaSinActividades(tramosEnDias(5, 1), new Profesor("MAT1", "Ana Ruiz"));

        assertThat(PrevalidacionService.prevalidar(problema, guardias(0, Map.of("MAT1", 0)))).isEmpty();
    }

    /**
     * P2, frontera: O = 2, L = 5 − 1 DURA = 4 y g = 2, así que O + g = L y no sale. Mata «>» → «>=».
     */
    @Test
    void guardiasSinHueco_ocupadasMasGuardiasIgualALibres_nada() {
        Profesor mat1 = new Profesor("MAT1", "Ana Ruiz");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 1);
        ProblemaHorario problema = problema(tramos, List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 2, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of(dura(mat1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, guardias(0, Map.of("MAT1", 2)))).isEmpty();
    }

    /**
     * P2: con g = 3, O + g = 5 y L = 4. Un ERROR del profesor, por su código y con su nombre. La L
     * resta la DURA: sin restarla sería 5 y no saldría (mata «sin restar las No puede»).
     */
    @Test
    void guardiasSinHueco_unaPorEncima_unErrorDelProfesorConSuNombre() {
        Profesor mat1 = new Profesor("MAT1", "Ana Ruiz");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        List<Tramo> tramos = tramosEnDias(5, 1);
        ProblemaHorario problema = problema(tramos, List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 2, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of(dura(mat1, tramos.get(0))));

        assertThat(PrevalidacionService.prevalidar(problema, guardias(0, Map.of("MAT1", 3))))
                .singleElement().satisfies(a -> {
                    assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
                    assertThat(a.regla()).isEqualTo(SIN_HUECO);
                    assertThat(a.entidadCodigo()).isEqualTo("MAT1");
                    assertThat(a.demanda()).isEqualTo(5);
                    assertThat(a.disponible()).isEqualTo(4);
                    assertThat(a.descripcion()).isEqualTo("Ana Ruiz tiene 2 horas ocupadas y 3 guardias"
                            + " ordinarias (5), pero solo 4 tramos de clase sin “No puede”.");
                });
    }

    /**
     * P2: «ocupado» es cualquier actividad. Dos clases y una reunión dan O = 3; con g = 2 y L = 4
     * sale. Contando solo las CLASE, O = 2 y no saldría.
     */
    @Test
    void guardiasSinHueco_unaReunionCuentaComoOcupada() {
        Profesor mat1 = new Profesor("MAT1", "Ana Ruiz");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(4, 1), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 2, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg)),
                        actividad("RED", 1, 1, PatronTemporal.NEUTRA, plazaSinAlumnos("RED-P1", mat1))),
                List.of());
        DatosCuadre datos = new DatosCuadre(Map.of(), Map.of(), Set.of("RED"), 0, Map.of("MAT1", 2));

        assertThat(soloRegla(PrevalidacionService.prevalidar(problema, datos), SIN_HUECO))
                .extracting(AvisoPrevalidacion::entidadCodigo, AvisoPrevalidacion::demanda,
                        AvisoPrevalidacion::disponible)
                .containsExactly(tuple("MAT1", 5, 4));
    }

    /** P2: sin guardias no hay nada que no quepa, aunque el profesor esté justo lleno (O = L). */
    @Test
    void guardiasSinHueco_sinGuardias_nada() {
        Profesor mat1 = new Profesor("MAT1", "Ana Ruiz");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(4, 1), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        assertThat(PrevalidacionService.prevalidar(problema, guardias(0, Map.of("MAT1", 0)))).isEmpty();
    }

    /**
     * P2 con O > L: avisa PROFESOR_SOBRECARGADO y no GUARDIAS_SIN_HUECO, para no repetir el aviso.
     * Mata la guarda O ≤ L quitada.
     */
    @Test
    void guardiasSinHueco_conElProfesorYaSobrecargado_soloSaleLaSobrecarga() {
        Profesor mat1 = new Profesor("MAT1", "Ana Ruiz");
        GrupoAdministrativo grupo = grupo("1ºA");
        Subgrupo sg = new Subgrupo("1ºA-Completo", Set.of(grupo));
        ProblemaHorario problema = problema(tramosEnDias(4, 1), List.of(mat1), List.of(grupo), List.of(sg),
                List.of(actividad("Mat-1ºA", 5, 1, PatronTemporal.NEUTRA, plaza("Mat-1ºA-P1", mat1, sg))),
                List.of());

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema, guardias(0, Map.of("MAT1", 2)));

        assertThat(soloRegla(avisos, PrevalidacionService.REGLA_PROFESOR_SOBRECARGADO)).hasSize(1);
        assertThat(soloRegla(avisos, SIN_HUECO)).isEmpty();
    }

    // ------------------------------------------------------------------- helpers

    private static DatosCuadre datos(
            Map<String, Integer> profesores, Map<String, Integer> grupos, Set<String> noClase) {
        return new DatosCuadre(profesores, grupos, noClase);
    }

    /** Sin totales y todo CLASE, con el mínimo por tramo y las guardias de cada profesor (S212). */
    private static DatosCuadre guardias(int minimo, Map<String, Integer> porProfesor) {
        return new DatosCuadre(Map.of(), Map.of(), Set.of(), minimo, porProfesor);
    }

    /** Solo tramos y profesores, sin grupos ni actividades (S212: las guardias del centro). */
    private static ProblemaHorario problemaSinActividades(List<Tramo> tramos, Profesor... profesores) {
        return problema(tramos, List.of(profesores), List.of(), List.of(), List.of(), List.of());
    }

    /** Plaza de una reunión o una función (S201): sin subgrupos y sin aula. */
    private static Plaza plazaSinAlumnos(String codigo, Profesor profesor) {
        return new Plaza(codigo, MAT, Set.of(profesor), Optional.empty(), Set.of(), Set.of());
    }

    private static List<Tramo> tramosEnDias(int dias, int porDia) {
        List<Tramo> tramos = new ArrayList<>();
        for (int dia = 1; dia <= dias; dia++) {
            for (int orden = 1; orden <= porDia; orden++) {
                tramos.add(new Tramo("D" + dia + "T" + orden, dia, orden));
            }
        }
        return tramos;
    }

    private static GrupoAdministrativo grupo(String codigo) {
        return new GrupoAdministrativo(codigo, TipoGrupo.ORDINARIO, Optional.empty());
    }

    private static Plaza plaza(String codigo, Profesor profesor, Subgrupo subgrupo) {
        return plazaEn(codigo, profesor, subgrupo, A1);
    }

    private static Plaza plazaEn(String codigo, Profesor profesor, Subgrupo subgrupo, Aula aula) {
        return new Plaza(codigo, MAT, Set.of(profesor), Optional.of(aula), Set.of(), Set.of(subgrupo));
    }

    private static Plaza plazaCandidatas(String codigo, Profesor profesor, Subgrupo subgrupo,
                                         Aula... aulas) {
        return new Plaza(codigo, MAT, Set.of(profesor), Optional.empty(), Set.of(aulas), Set.of(subgrupo));
    }

    private static Actividad actividad(
            String codigo, int repeticiones, int duracion, PatronTemporal patron, Plaza... plazas) {
        return new Actividad(codigo, Optional.of(MAT), repeticiones, duracion, patron, List.of(plazas), false);
    }

    private static RestriccionHoraria dura(Profesor profesor, Tramo tramo) {
        return new RestriccionHoraria(profesor, tramo, TipoRestriccion.DURA, 0, Optional.empty());
    }

    private static RestriccionHoraria blanda(Profesor profesor, Tramo tramo) {
        return new RestriccionHoraria(profesor, tramo, TipoRestriccion.BLANDA, 1, Optional.empty());
    }

    /** Actividad tutorial de 1x1 con {@code requiereTutor = true} y un solo profesor. */
    private static Actividad actividadTutorial(String codigo, Profesor profesor, Subgrupo sg) {
        return new Actividad(codigo, Optional.of(MAT), 1, 1, PatronTemporal.NEUTRA,
                List.of(plaza(codigo + "-P1", profesor, sg)), true);
    }

    private static ProblemaHorario problema(
            List<Tramo> tramos, List<Profesor> profesores, List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos, List<Actividad> actividades,
            List<RestriccionHoraria> restricciones) {
        return problema(tramos, profesores, grupos, subgrupos, actividades, restricciones,
                List.of());
    }

    /** Sobrecarga con TUTORÍAS, que las tres primeras reglas no necesitaban. */
    private static ProblemaHorario problema(
            List<Tramo> tramos, List<Profesor> profesores, List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos, List<Actividad> actividades,
            List<RestriccionHoraria> restricciones, List<ProfesorTutoria> tutorias) {
        return new ProblemaHorario(tramos, List.of(A1, A2, A3), List.of(MAT), profesores, grupos,
                subgrupos, actividades, restricciones, List.of(), tutorias);
    }

    /** Con PINES de tramo, que solo la regla (f) necesita. Sin tutorías. */
    private static ProblemaHorario problemaConPines(
            List<Tramo> tramos, List<Profesor> profesores, List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos, List<Actividad> actividades,
            List<RestriccionHoraria> restricciones, List<SesionBloqueada> pines) {
        return new ProblemaHorario(tramos, List.of(A1, A2, A3), List.of(MAT), profesores, grupos,
                subgrupos, actividades, restricciones, pines, List.of());
    }

    /** Pin de tramo de la instancia {@code indice} de {@code actividad}, sin pin de aula. */
    private static SesionBloqueada pin(Actividad actividad, int indice, Tramo tramo) {
        return new SesionBloqueada(new ActividadInstancia(actividad, indice), tramo, Map.of());
    }

    private static List<AvisoPrevalidacion> soloRegla(List<AvisoPrevalidacion> avisos, String regla) {
        return avisos.stream().filter(a -> a.regla().equals(regla)).toList();
    }
}
