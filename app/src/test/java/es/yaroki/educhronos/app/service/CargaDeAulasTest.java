package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * CARGA DE AULAS EXCEDIDA (S209, T4): flujo máximo de las plazas de CLASE a las aulas de su dominio,
 * con los tramos lectivos de cada aula como capacidad. Un aviso ERROR por componente del lado de
 * la fuente del corte mínimo cuya demanda supera lo disponible. Puros: el problema en memoria.
 */
class CargaDeAulasTest {

    private static final Asignatura MAT = new Asignatura("Mat", "Matemáticas");
    private static final Aula A1 = new Aula("A1", "A1");
    private static final Aula A2 = new Aula("A2", "A2");
    private static final Aula A3 = new Aula("A3", "A3");
    private int profesores = 0;

    private static List<Tramo> tramos(int n) {
        List<Tramo> tramos = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            tramos.add(new Tramo("T" + i, 1 + (i - 1) % 5, 1 + (i - 1) / 5));
        }
        return tramos;
    }

    /** Plaza con su profesor propio, sin subgrupos: aquí solo cuentan sus aulas. */
    private Plaza plaza(String codigo, Aula fija, Aula... candidatas) {
        Profesor p = new Profesor("P" + (++profesores), "Profesor " + profesores);
        return new Plaza(codigo, MAT, Set.of(p), Optional.ofNullable(fija), Set.of(candidatas), Set.of());
    }

    private static Actividad actividad(String codigo, int repeticiones, int duracion, Plaza... plazas) {
        return new Actividad(codigo, Optional.of(MAT), repeticiones, duracion, PatronTemporal.NEUTRA,
                List.of(plazas), false);
    }

    private static ProblemaHorario problema(int tramos, List<Aula> aulas, Actividad... actividades) {
        List<Profesor> profesores = Arrays.stream(actividades).flatMap(a -> a.plazas().stream())
                .flatMap(p -> p.profesores().stream()).distinct().toList();
        return new ProblemaHorario(tramos(tramos), aulas, List.of(MAT), profesores, List.of(), List.of(),
                List.of(actividades), List.of(), List.of(), List.of());
    }

    private static List<AvisoPrevalidacion> carga(ProblemaHorario problema, DatosCuadre datos) {
        return PrevalidacionService.prevalidar(problema, datos, DatosAulas.VACIO).stream()
                .filter(a -> a.regla().equals(PrevalidacionService.REGLA_CARGA_DE_AULAS_EXCEDIDA))
                .toList();
    }

    private static List<AvisoPrevalidacion> carga(ProblemaHorario problema) {
        return carga(problema, DatosCuadre.VACIO);
    }

    @Test
    void sinDeficit_ningunAviso() {
        ProblemaHorario p = problema(5, List.of(A1),
                actividad("ACT1", 2, 1, plaza("ACT1-P1", A1)), actividad("ACT2", 3, 1, plaza("ACT2-P1", A1)));

        assertThat(carga(p)).isEmpty();
    }

    @Test
    void dosAulasFijasIgualesQueSumanMasQueLosTramos_unAvisoExacto() {
        ProblemaHorario p = problema(5, List.of(A1),
                actividad("ACT2", 3, 1, plaza("ACT2-P1", A1)), actividad("ACT1", 3, 1, plaza("ACT1-P1", A1)));

        assertThat(carga(p)).singleElement().satisfies(a -> {
            assertThat(a.severidad()).isEqualTo(Severidad.ERROR);
            assertThat(a.entidadCodigo()).isEqualTo("A1");
            assertThat(a.demanda()).isEqualTo(6);
            assertThat(a.disponible()).isEqualTo(5);
            assertThat(a.descripcion()).isEqualTo("Las aulas A1 tienen 5 horas de clase y las clases que solo"
                    + " pueden ir a ellas suman 6. Actividades: ACT1, ACT2.");
        });
    }

    @Test
    void dosComponentesIndependientesConDeficit_dosAvisos() {
        ProblemaHorario p = problema(4, List.of(A1, A2),
                actividad("ACT1", 3, 1, plaza("ACT1-P1", A1)), actividad("ACT2", 2, 1, plaza("ACT2-P1", A1)),
                actividad("ACT3", 5, 1, plaza("ACT3-P1", A2)));

        assertThat(carga(p)).extracting(AvisoPrevalidacion::entidadCodigo, AvisoPrevalidacion::demanda,
                AvisoPrevalidacion::disponible).containsExactly(tuple("A1", 5, 4), tuple("A2", 5, 4));
    }

    /**
     * Junto a un aula con déficit, otra exactamente llena (demanda = tramos) no se informa. Con el
     * corte mínimo tomado del lado de la fuente esa aula ni siquiera es alcanzable; el aserto
     * protege que solo salgan los conjuntos con déficit.
     */
    @Test
    void unConjuntoJustoNoSeInforma() {
        ProblemaHorario p = problema(5, List.of(A1, A2),
                actividad("ACT1", 6, 1, plaza("ACT1-P1", A1)), actividad("ACT2", 5, 1, plaza("ACT2-P1", A2)));

        assertThat(carga(p)).extracting(AvisoPrevalidacion::entidadCodigo).containsExactly("A1");
    }

    /**
     * Se nombra el conjunto que no da abasto, no su vecino con sitio: A1 solo puede recibir 6 tramos
     * de ACT1 y tiene 5; ACT2 (A1 o A2, 5 tramos) cabe entero en A2. El aviso es de A1, 6 de 5, y
     * no de «A1, A2», 11 de 10, que es lo que saldría si las aulas tuvieran un tramo menos.
     */
    @Test
    void seNombraElConjuntoQueNoDaAbasto_noLaUnionConSuVecino() {
        ProblemaHorario p = problema(5, List.of(A1, A2),
                actividad("ACT1", 6, 1, plaza("ACT1-P1", A1)), actividad("ACT2", 5, 1, plaza("ACT2-P1", null, A1, A2)));

        assertThat(carga(p)).extracting(AvisoPrevalidacion::entidadCodigo, AvisoPrevalidacion::demanda,
                AvisoPrevalidacion::disponible).containsExactly(tuple("A1", 6, 5));
    }

    /**
     * Los avisos salen ordenados por sus aulas, también cuando la raíz interna de un componente
     * ({A1, A3}, que acaba en A3) va detrás de la del otro ({A2}).
     */
    @Test
    void losAvisosSalenOrdenadosPorSusAulas() {
        ProblemaHorario p = problema(5, List.of(A1, A2, A3),
                actividad("ACT1", 11, 1, plaza("ACT1-P1", null, A1, A3)), actividad("ACT2", 6, 1, plaza("ACT2-P1", A2)));

        assertThat(carga(p)).extracting(AvisoPrevalidacion::entidadCodigo, AvisoPrevalidacion::demanda,
                AvisoPrevalidacion::disponible).containsExactly(tuple("A1, A3", 11, 10), tuple("A2", 6, 5));
    }

    @Test
    void laDuracionMultiplicaLaDemanda() {
        ProblemaHorario p = problema(5, List.of(A1), actividad("ACT1", 2, 3, plaza("ACT1-P1", A1)));

        assertThat(carga(p)).singleElement().satisfies(a -> {
            assertThat(a.demanda()).isEqualTo(6);
            assertThat(a.disponible()).isEqualTo(5);
        });
    }

    @Test
    void unaPlazaConDominioVacioNoEntra() {
        ProblemaHorario p = problema(5, List.of(A1),
                actividad("ACT1", 5, 1, plaza("ACT1-P1", A1)), actividad("ACT2", 3, 1, plaza("ACT2-P1", null)));

        assertThat(carga(p)).isEmpty();
    }

    @Test
    void lasPlazasDeUnaActividadCuentanCadaUna_yLaActividadSaleUnaVez() {
        ProblemaHorario p = problema(5, List.of(A1, A2),
                actividad("ACT1", 3, 1, plaza("ACT1-P1", A1), plaza("ACT1-P2", null, A1, A2)),
                actividad("ACT2", 5, 1, plaza("ACT2-P1", A2)));

        assertThat(carga(p)).singleElement().satisfies(a -> {
            assertThat(a.entidadCodigo()).isEqualTo("A1, A2");
            assertThat(a.demanda()).isEqualTo(11);
            assertThat(a.disponible()).isEqualTo(10);
            assertThat(a.descripcion()).endsWith("Actividades: ACT1, ACT2.");
        });
    }

    @Test
    void lasActividadesQueNoSonClaseNoCuentan() {
        ProblemaHorario p = problema(5, List.of(A1),
                actividad("ACT1", 5, 1, plaza("ACT1-P1", A1)), actividad("RED", 1, 1, plaza("RED-P1", A1)));
        DatosCuadre reunion = new DatosCuadre(Map.of(), Map.of(), Set.of("RED"));

        assertThat(carga(p, reunion)).isEmpty();
        assertThat(carga(p)).singleElement().extracting(AvisoPrevalidacion::demanda).isEqualTo(6);
    }

    /**
     * El caso de s209/t4/scripts/caso-oraculo.py, cuyo resultado da el prototipo independiente
     * s209/m2b/scripts/carga-aulas.py (salida en s209/t4/oraculo/p5-carga-A.txt): {X, Y} 11 de 10
     * (ACT1, ACT2, ACT3) y {Z} 6 de 5 (ACT4, ACT5); W, justa, no sale; ACT7, sin aula, no entra.
     */
    @Test
    void casoDelOraculo() {
        Aula x = new Aula("X", "X");
        Aula y = new Aula("Y", "Y");
        Aula z = new Aula("Z", "Z");
        Aula w = new Aula("W", "W");
        ProblemaHorario p = problema(5, List.of(x, y, z, w),
                actividad("ACT1", 3, 1, plaza("ACT1-P1", x)),
                actividad("ACT2", 2, 2, plaza("ACT2-P1", null, x, y)),
                actividad("ACT3", 4, 1, plaza("ACT3-P1", y)),
                actividad("ACT4", 3, 1, plaza("ACT4-P1", z)),
                actividad("ACT5", 1, 3, plaza("ACT5-P1", z)),
                actividad("ACT6", 5, 1, plaza("ACT6-P1", w)),
                actividad("ACT7", 1, 1, plaza("ACT7-P1", null)));

        assertThat(carga(p)).extracting(AvisoPrevalidacion::entidadCodigo, AvisoPrevalidacion::demanda,
                AvisoPrevalidacion::disponible, AvisoPrevalidacion::descripcion).containsExactly(
                tuple("X, Y", 11, 10, "Las aulas X, Y tienen 10 horas de clase y las clases que solo pueden ir a"
                        + " ellas suman 11. Actividades: ACT1, ACT2, ACT3."),
                tuple("Z", 6, 5, "Las aulas Z tienen 5 horas de clase y las clases que solo pueden ir a ellas"
                        + " suman 6. Actividades: ACT4, ACT5."));
    }
}
