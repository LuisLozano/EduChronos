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
                        actividad("Ing-desdoble", 3, 1, PatronTemporal.NEUTRA,
                                plaza("Ing-desdoble-P1", ing1, desd1),
                                plaza("Ing-desdoble-P2", ing2, desd2)),
                        actividad("Mat-1ºA", 4, 1, PatronTemporal.NEUTRA,
                                plaza("Mat-1ºA-P1", mat1, completo))),
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
                List.of(actividad("Mat-desdoble", 3, 1, PatronTemporal.NEUTRA,
                        plaza("Mat-desdoble-P1", mat1, desd1), plaza("Mat-desdoble-P2", mat1, desd2))),
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

    // ------------------------------------------------------------------- helpers

    private static DatosCuadre datos(
            Map<String, Integer> profesores, Map<String, Integer> grupos, Set<String> noClase) {
        return new DatosCuadre(profesores, grupos, noClase);
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
        return new Plaza(codigo, MAT, Set.of(profesor), Optional.of(A1), Set.of(), Set.of(subgrupo));
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
        return new ProblemaHorario(tramos, List.of(A1), List.of(MAT), profesores, grupos,
                subgrupos, actividades, restricciones, List.of(), tutorias);
    }

    /** Con PINES de tramo, que solo la regla (f) necesita. Sin tutorías. */
    private static ProblemaHorario problemaConPines(
            List<Tramo> tramos, List<Profesor> profesores, List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos, List<Actividad> actividades,
            List<RestriccionHoraria> restricciones, List<SesionBloqueada> pines) {
        return new ProblemaHorario(tramos, List.of(A1), List.of(MAT), profesores, grupos,
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
