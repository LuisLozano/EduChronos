package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * El reparto de las guardias de recreo (S220, C-reparto-equilibrado, condición 2 de
 * O-guardias-especiales): la biblioteca, los fallos y sus textos (decisión N). El equilibrio y los
 * mínimos, contra el oráculo, en {@link RepartoGuardiasOraculoTest}.
 */
class RepartoGuardiasRecreoTest {

    private static Map<String, Long> bibliotecasPorProfesor(RepartoGuardias.ResultadoRecreo r) {
        return r.asignaciones().stream().filter(RepartoGuardias.AsignacionRecreo::biblioteca)
                .collect(Collectors.groupingBy(RepartoGuardias.AsignacionRecreo::profesorCodigo, TreeMap::new,
                        Collectors.counting()));
    }

    private static Map<Integer, Long> bibliotecasPorRecreo(RepartoGuardias.ResultadoRecreo r) {
        return r.asignaciones().stream().filter(RepartoGuardias.AsignacionRecreo::biblioteca)
                .collect(Collectors.groupingBy(RepartoGuardias.AsignacionRecreo::recreo, TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * Decisión M: tres profesores en los cinco recreos, mínimo 3 y biblioteca 1. Cada recreo, una
     * biblioteca; y como va a quien menos lleva, ningún profesor lleva más de una de diferencia.
     */
    @Test
    void laBibliotecaSeRepartePareja() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(5, Map.of("A", 5, "B", 5, "C", 5), 3, 1);

        assertThat(r.fallo()).isFalse();
        assertThat(bibliotecasPorRecreo(r)).isEqualTo(Map.of(0, 1L, 1, 1L, 2, 1L, 3, 1L, 4, 1L));
        Map<String, Long> porProfesor = bibliotecasPorProfesor(r);
        assertThat(porProfesor.keySet()).containsExactly("A", "B", "C");
        long max = porProfesor.values().stream().mapToLong(Long::longValue).max().orElseThrow();
        long min = porProfesor.values().stream().mapToLong(Long::longValue).min().orElseThrow();
        assertThat(max - min).isLessThanOrEqualTo(1);
    }

    @Test
    void cadaRecreoLlevaExactamenteElMinimoDeBiblioteca() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(3,
                Map.of("A", 3, "B", 3, "C", 3, "D", 3), 3, 2);

        assertThat(r.fallo()).isFalse();
        assertThat(bibliotecasPorRecreo(r)).isEqualTo(Map.of(0, 2L, 1, 2L, 2, 2L));
        assertThat(bibliotecasPorProfesor(r).values()).allMatch(n -> n >= 1 && n <= 2);
    }

    @Test
    void unProfesorConMasGuardiasQueRecreos_falloConSuCodigo() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(2, Map.of("A", 3, "B", 1), 1, 0);

        assertThat(r.fallo()).isTrue();
        assertThat(r.excedidos()).containsExactly(new RepartoGuardias.Excedido("A", 3, 2));
        assertThat(r.asignaciones()).isEmpty();
        assertThat(r.deficits()).isEmpty();
        assertThat(r.bibliotecaSuperaRecreo()).isFalse();
    }

    @Test
    void unaBibliotecaMayorQueElRecreo_fallo() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(2, Map.of("A", 1, "B", 1), 1, 2);

        assertThat(r.fallo()).isTrue();
        assertThat(r.bibliotecaSuperaRecreo()).isTrue();
        assertThat(r.asignaciones()).isEmpty();
        assertThat(r.excedidos()).isEmpty();
    }

    @Test
    void sinGuardiasDeRecreo_listaVaciaYNoEsFallo() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(3, Map.of("A", 0), 0, 0);

        assertThat(r.fallo()).isFalse();
        assertThat(r.asignaciones()).isEmpty();
        assertThat(RepartoGuardias.repartirRecreo(0, Map.of(), 0, 0).asignaciones()).isEmpty();
    }

    @Test
    void unRecreoSinElMinimo_falloConElDeficit() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(1, Map.of("A", 1), 2, 0);

        assertThat(r.fallo()).isTrue();
        assertThat(r.deficits()).containsExactly(new RepartoGuardias.DeficitRecreo(0, 1, 2));
    }

    // ------------------------------------------------------------------ textos (decisión N)

    private static final List<TramoSemanal> RECREOS = List.of(
            new TramoSemanal(Dia.LUNES, LocalTime.of(11, 0), LocalTime.of(11, 30), false, 4, null),
            new TramoSemanal(Dia.MARTES, LocalTime.of(11, 0), LocalTime.of(11, 30), false, 11, null));

    private static DatosCuadre cuadre(int minimoRecreo, int minimoBiblioteca) {
        return new DatosCuadre(Map.of(), Map.of(), Set.of(), 0, Map.of(), Map.of(),
                Map.of("A", "Ana Alba", "B", "Bea Bravo"), minimoRecreo, minimoBiblioteca, RECREOS.size());
    }

    @Test
    void textoDelDeficitAlGenerarYAlAjustar() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(2, Map.of("A", 1, "B", 1), 2, 0);

        assertThat(r.deficits()).isNotEmpty();
        String lista = r.deficits().stream()
                .map(d -> (d.recreo() == 0 ? "LUNES" : "MARTES") + " 11:00-11:30: " + d.alcanzadas() + " de 2")
                .collect(Collectors.joining("; "));
        assertThat(ViaRepartoGuardias.mensajeRecreoAlGenerar(r, RECREOS, cuadre(2, 0))).isEqualTo(
                "No se pueden repartir las guardias de recreo: en estos recreos no se llega al mínimo de 2"
                        + " profesores de guardia: " + lista + ". Baja el mínimo o da más guardias de recreo a"
                        + " los profesores.");
        assertThat(ViaRepartoGuardias.mensajeRecreoAlAjustar(r, RECREOS, cuadre(2, 0))).isEqualTo(
                "No se puede hacer este cambio: las guardias de recreo no se pueden repartir con la configuración"
                        + " actual. Recreos que no llegan al mínimo de profesores de guardia: " + lista + ".");
    }

    @Test
    void textoDeUnRecreoConcreto() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(1, Map.of("A", 1), 2, 0);

        assertThat(ViaRepartoGuardias.mensajeRecreoAlGenerar(r, RECREOS, cuadre(2, 0))).isEqualTo(
                "No se pueden repartir las guardias de recreo: en estos recreos no se llega al mínimo de 2"
                        + " profesores de guardia: LUNES 11:00-11:30: 1 de 2. Baja el mínimo o da más guardias de"
                        + " recreo a los profesores.");
        assertThat(ViaRepartoGuardias.resumenRecreo(r)).isEqualTo("recreo1:1/2");
    }

    /** T-R3: los textos de la pre-validación, con el nombre completo del profesor y en su orden. */
    @Test
    void textosDeConfiguracionAlGenerarYAlAjustar() {
        RepartoGuardias.ResultadoRecreo r = RepartoGuardias.repartirRecreo(2, Map.of("A", 3, "B", 4), 1, 2);
        String textos = "El mínimo de guardias de biblioteca por recreo (2) es mayor que el mínimo de guardias por"
                + " recreo (1). Las de biblioteca cuentan dentro de las de recreo: sube el mínimo por recreo o baja"
                + " el de biblioteca. Ana Alba tiene 3 guardias de recreo, pero solo hay 2 recreos y no puede hacer"
                + " más de una en cada uno. Bea Bravo tiene 4 guardias de recreo, pero solo hay 2 recreos y no"
                + " puede hacer más de una en cada uno.";

        assertThat(ViaRepartoGuardias.mensajeRecreoAlGenerar(r, RECREOS, cuadre(1, 2))).isEqualTo(textos);
        assertThat(ViaRepartoGuardias.mensajeRecreoAlAjustar(r, RECREOS, cuadre(1, 2))).isEqualTo(
                "No se puede hacer este cambio: las guardias de recreo no se pueden repartir con la configuración"
                        + " actual. " + textos);
        assertThat(ViaRepartoGuardias.resumenRecreo(r)).isEqualTo("configuracion");
    }
}
