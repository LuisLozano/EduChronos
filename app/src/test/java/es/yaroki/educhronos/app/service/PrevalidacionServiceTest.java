package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests del NÚCLEO de la pre-validación (Fase 8, Bloque 8.4-A, deuda D18):
 * {@link PrevalidacionService#prevalidar(ProblemaHorario)}, el método estático puro.
 * Se ejercita con {@code ProblemaHorario} construidos a mano —no hay JPA, ni Spring, ni
 * solver— porque es exactamente la entrada que el servicio recibe en producción tras
 * {@code cargarProblema()}.
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

        assertThat(PrevalidacionService.prevalidar(problema)).isEmpty();
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

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema);

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
                PrevalidacionService.prevalidar(problema),
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

        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(problema);

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

        assertThat(PrevalidacionService.prevalidar(problema)).singleElement().satisfies(a -> {
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

        assertThat(PrevalidacionService.prevalidar(problema)).isEmpty();
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

        assertThat(PrevalidacionService.prevalidar(problema)).isEmpty();
    }

    // ------------------------------------------------------------------- helpers

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
        return new Actividad(codigo, Optional.of(MAT), repeticiones, duracion, patron, List.of(plazas));
    }

    private static RestriccionHoraria dura(Profesor profesor, Tramo tramo) {
        return new RestriccionHoraria(profesor, tramo, TipoRestriccion.DURA, 0, Optional.empty());
    }

    private static ProblemaHorario problema(
            List<Tramo> tramos, List<Profesor> profesores, List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos, List<Actividad> actividades,
            List<RestriccionHoraria> restricciones) {
        return new ProblemaHorario(tramos, List.of(A1), List.of(MAT), profesores, grupos,
                subgrupos, actividades, restricciones, List.of());
    }

    private static List<AvisoPrevalidacion> soloRegla(List<AvisoPrevalidacion> avisos, String regla) {
        return avisos.stream().filter(a -> a.regla().equals(regla)).toList();
    }
}
