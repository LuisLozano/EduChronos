package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SesionBloqueada;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Término «clase fuera de su aula preferida» del objetivo (S208, C-preferencias-aulas, C1-C3).
 * Coste: la duración en tramos de la sesión por cada (instancia, plaza) colocada en un aula que
 * no está entre las preferidas efectivas de la plaza; 0 en las plazas sin preferidas efectivas.
 *
 * <p>Cada caso aísla el término: cada clase tiene su propio profesor y su propio grupo, y una
 * sola sesión, así que el modelo no crea ventanas (piden ≥ 2 sesiones del profesor), ni
 * consecutivas (piden más de 3), ni hay restricciones BLANDA. El {@code objetivo} ES el término.
 * Problemas construidos en Java, como {@code SolverHorarioIndisponibilidadBloqueTest}, para no
 * tocar los fixtures JSON de la huella canónica.
 */
class SolverHorarioAulaPreferidaTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura");
    private static final GrupoAdministrativo GX =
            new GrupoAdministrativo("GX", TipoGrupo.ORDINARIO, Optional.empty());
    private static final GrupoAdministrativo GY =
            new GrupoAdministrativo("GY", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SGX = new Subgrupo("GX-1", Set.of(GX));
    private static final Subgrupo SGY = new Subgrupo("GY-1", Set.of(GY));
    private static final Profesor PX = new Profesor("PX", "Equis");
    private static final Profesor PY = new Profesor("PY", "Ye");
    private static final Aula PREF = new Aula("PREF", "Preferida");
    private static final Aula OX = new Aula("OX", "Otra de X");
    private static final Aula OY = new Aula("OY", "Otra de Y");

    /** Lunes, tres tramos antes del recreo: un bloque de 2 cabe en L1-L2. */
    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);
    private static final Tramo L3 = new Tramo("L3", 1, 3);

    private final SolverHorario solver = new SolverHorario(10.0, 42);
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    /**
     * (a) Una plaza con dos aulas posibles, una preferida, sin ningún otro coste: el óptimo
     * la pone en la preferida y el objetivo es 0.
     */
    @Test
    void a_unaPlazaConUnaPreferida_vaALaPreferidaConObjetivoCero() {
        Actividad x = clase("X", 1, OX, SGX, PX);
        ProblemaHorario p = problema(List.of(x), List.of(), Map.of("X-P1", Set.of("PREF")));

        ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);

        assertThat(r.esOptimo()).isTrue();
        assertThat(r.objetivo()).isEqualTo(0.0);
        assertThat(aulaDe(r.solucion(), x)).isEqualTo(PREF);
    }

    /**
     * (b) Dos clases pinadas en el mismo tramo, las dos con PREF como única preferida y otra
     * aula posible cada una. Solo una cabe en PREF: la otra paga 1.
     */
    @Test
    void b_dosClasesCompitenPorLaPreferida_objetivoUno() {
        ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(problemaCompetido(1));

        assertThat(r.esOptimo()).isTrue();
        assertThat(r.objetivo()).isEqualTo(1.0);
    }

    /**
     * (c) Como (b) con las dos clases de duración 2: la desplazada, sea cual sea, ocupa dos
     * tramos fuera de su preferida y paga 2.
     */
    @Test
    void c_comoBConDuracionDos_objetivoDos() {
        ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(problemaCompetido(2));

        assertThat(r.esOptimo()).isTrue();
        assertThat(r.objetivo()).isEqualTo(2.0);
    }

    /**
     * (d) Plazas sin preferidas efectivas, sin entrada o con el conjunto vacío, pinadas en
     * cada una de sus aulas: coste 0 en todas.
     */
    @Test
    void d_plazaSinPreferidas_enCualquieraDeSusAulas_costeCero() {
        Actividad x = clase("X", 1, OX, SGX, PX);
        Plaza plazaX = x.plazas().get(0);
        for (Map<String, Set<String>> preferidas : List.of(
                Map.<String, Set<String>>of(), Map.of("X-P1", Set.<String>of()))) {
            for (Aula aula : List.of(PREF, OX)) {
                SesionBloqueada pin = new SesionBloqueada(
                        new ActividadInstancia(x, 1), L1, Map.of(plazaX, aula));
                ProblemaHorario p = problema(List.of(x), List.of(pin), preferidas);

                ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);

                assertThat(aulaDe(r.solucion(), x)).isEqualTo(aula);
                assertThat(r.objetivo()).as("preferidas %s, aula %s", preferidas, aula.codigo())
                        .isEqualTo(0.0);
                assertThat(verificador.contarPenalizacionAulaNoPreferida(p, r.solucion()))
                        .as("verificador, preferidas %s, aula %s", preferidas, aula.codigo())
                        .isZero();
            }
        }
    }

    /**
     * (e) Recomposición: sobre las soluciones de (b) y (c), el objetivo de CP-SAT es la suma de
     * los cuatro contadores del verificador, como en el {@code conc_} de S166. El de aula no
     * preferida no es 0 (vale 1 y 2), así que la igualdad lo pone a prueba.
     */
    @Test
    void e_elObjetivoEsLaSumaDeLosCuatroContadores() {
        for (int duracion : List.of(1, 2)) {
            ProblemaHorario p = problemaCompetido(duracion);
            ResultadoOptimizacion r = solver.resolverOptimizandoConDetalle(p);
            SolucionHorario sol = r.solucion();

            int ventanas = verificador.contarVentanasProfesor(p, sol).values().stream()
                    .mapToInt(Integer::intValue).sum();
            int consecutivas = verificador.contarPenalizacionConsecutivasProfesor(p, sol);
            int indispBlanda = verificador.contarPenalizacionIndisponibilidadBlanda(p, sol);
            int aulaNoPreferida = verificador.contarPenalizacionAulaNoPreferida(p, sol);

            assertThat(aulaNoPreferida).as("aula no preferida, duración %d", duracion)
                    .isEqualTo(duracion);
            assertThat(r.objetivo()).as("objetivo frente a la recomposición, duración %d", duracion)
                    .isEqualTo((double) (ventanas + consecutivas + indispBlanda + aulaNoPreferida));
        }
    }

    // ============================================================= fixtures

    /** Clase de una plaza con PREF y {@code otra} como aulas posibles. */
    private static Actividad clase(String cod, int duracion, Aula otra, Subgrupo sg, Profesor prof) {
        Plaza plaza = new Plaza(cod + "-P1", ASIG, Set.of(prof), Optional.empty(),
                Set.of(PREF, otra), Set.of(sg));
        return new Actividad(cod, Optional.of(ASIG), 1, duracion, PatronTemporal.NEUTRA,
                List.of(plaza), false);
    }

    /** (b) y (c): X e Y pinadas en L1, las dos con PREF como única preferida. */
    private static ProblemaHorario problemaCompetido(int duracion) {
        Actividad x = clase("X", duracion, OX, SGX, PX);
        Actividad y = clase("Y", duracion, OY, SGY, PY);
        return problema(List.of(x, y),
                List.of(new SesionBloqueada(new ActividadInstancia(x, 1), L1, Map.of()),
                        new SesionBloqueada(new ActividadInstancia(y, 1), L1, Map.of())),
                Map.of("X-P1", Set.of("PREF"), "Y-P1", Set.of("PREF")));
    }

    private static ProblemaHorario problema(List<Actividad> actividades,
                                            List<SesionBloqueada> bloqueos,
                                            Map<String, Set<String>> preferidas) {
        return new ProblemaHorario(List.of(L1, L2, L3), List.of(PREF, OX, OY), List.of(ASIG),
                List.of(PX, PY), List.of(GX, GY), List.of(SGX, SGY), actividades, List.of(),
                bloqueos, List.of(), Map.of(), Map.of(), preferidas);
    }

    private static Aula aulaDe(SolucionHorario sol, Actividad actividad) {
        return sol.aulaElegida(new ActividadInstancia(actividad, 1), actividad.plazas().get(0))
                .orElseThrow();
    }
}
