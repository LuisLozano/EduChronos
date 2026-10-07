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
 * Aula fuera de las reglas en el VERIFICADOR, {@link ReglaDura#AULA_FUERA_DE_REGLAS} (S207,
 * C-deduccion-aulas, C4): el aula de una plaza en la solución tiene que ser su aula fija o una de
 * sus candidatas en el problema. Un horario guardado con unas reglas y leído con otras puede
 * tener aulas que ya no están permitidas: la regla lo nombra en vez de abortar.
 *
 * <p>Soluciones fabricadas a mano (molde de {@link VerificadorSolucionIndisponibilidadTest}).
 * El aula de una plaza fija distinta de la suya se fabrica poniendo la plaza en
 * {@code aulasElegidas}, que {@link SolucionHorario} no valida contra el dominio.
 */
class VerificadorSolucionAulaFueraDeReglasTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("G-1", Set.of(GRUPO));
    private static final Aula X = new Aula("X", "X");
    private static final Aula Y = new Aula("Y", "Y");
    private static final Aula Z = new Aula("Z", "Z");
    private static final Profesor P1 = new Profesor("P1", "Uno");
    private static final Tramo L1 = new Tramo("L1", 1, 1);

    private final VerificadorSolucion verificador = new VerificadorSolucion();

    private static Plaza fija(Aula aula) {
        return new Plaza("ACT-P1", ASIG, Set.of(P1), Optional.of(aula), Set.of(), Set.of(SG));
    }

    private static Plaza candidatas(Aula... aulas) {
        return new Plaza("ACT-P1", ASIG, Set.of(P1), Optional.empty(), Set.of(aulas), Set.of(SG));
    }

    /** Plaza sin aula: una reunión o una función, sin subgrupos (S201). */
    private static Plaza sinAula() {
        return new Plaza("ACT-P1", ASIG, Set.of(P1), Optional.empty(), Set.of(), Set.of());
    }

    private static ProblemaHorario problema(Plaza plaza) {
        Actividad act = new Actividad("ACT", Optional.of(ASIG), 1, 1, PatronTemporal.NEUTRA,
                List.of(plaza), false);
        return new ProblemaHorario(
                List.of(L1), List.of(X, Y, Z), List.of(ASIG), List.of(P1), List.of(GRUPO),
                List.of(SG), List.of(act), List.of(), List.of(), List.of());
    }

    private static ActividadInstancia inst(ProblemaHorario problema) {
        return Expansion.todas(problema).get(0);
    }

    /** La única instancia en L1 y, si {@code aula} no es null, esa aula registrada para la plaza. */
    private static SolucionHorario en(ProblemaHorario problema, Aula aula) {
        ActividadInstancia i = inst(problema);
        Plaza plaza = i.actividad().plazas().get(0);
        return aula == null
                ? new SolucionHorario(Map.of(i, L1))
                : new SolucionHorario(Map.of(i, L1), Map.of(i, Map.of(plaza, aula)));
    }

    private List<Violacion> fuera(ProblemaHorario problema, SolucionHorario sol) {
        return verificador.verificar(problema, sol).violaciones().stream()
                .filter(v -> v.regla() == ReglaDura.AULA_FUERA_DE_REGLAS)
                .toList();
    }

    @Test
    void fijaXYSolucionEnY_unaViolacionConAulaTramoYCelda() {
        ProblemaHorario problema = problema(fija(X));

        List<Violacion> vs = fuera(problema, en(problema, Y));

        assertThat(vs).hasSize(1);
        Violacion v = vs.get(0);
        assertThat(v.regla()).isEqualTo(ReglaDura.AULA_FUERA_DE_REGLAS);
        assertThat(v.recursoCodigo()).isEqualTo("Y");
        assertThat(v.tramoCodigo()).isEqualTo("L1");
        assertThat(v.celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));
        assertThat(v.descripcion()).contains("Y").contains("ACT-P1").contains("X");
    }

    @Test
    void candidatasXYYSolucionEnZ_violacion() {
        ProblemaHorario problema = problema(candidatas(X, Y));

        List<Violacion> vs = fuera(problema, en(problema, Z));

        assertThat(vs).hasSize(1);
        assertThat(vs.get(0).recursoCodigo()).isEqualTo("Z");
        assertThat(vs.get(0).celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));
    }

    @Test
    void fijaXYSolucionEnX_sinViolacion_registradaONo() {
        ProblemaHorario problema = problema(fija(X));

        assertThat(fuera(problema, en(problema, X))).as("X registrada").isEmpty();
        assertThat(fuera(problema, en(problema, null))).as("sin registrar: cae a la fija").isEmpty();
    }

    @Test
    void candidatasXYYSolucionEnY_sinViolacion() {
        ProblemaHorario problema = problema(candidatas(X, Y));

        assertThat(fuera(problema, en(problema, Y))).isEmpty();
        assertThat(fuera(problema, en(problema, X))).isEmpty();
    }

    /** Plaza sin aula en el problema y sin aula en la solución (FUNCION, REUNION): nunca viola. */
    @Test
    void plazaSinAulaEnElProblemaNiEnLaSolucion_nuncaViola() {
        ProblemaHorario problema = problema(sinAula());

        assertThat(fuera(problema, en(problema, null))).isEmpty();
    }

    /**
     * S207 T2b: una plaza que el problema deja sin ningún aula —una clase cuyas aulas posibles
     * desaparecieron tras generar— y que en la solución SÍ trae un aula viola: cualquier aula
     * está fuera de un dominio vacío. Sustituye a la mitad «con un aula en la solución» del caso
     * de T1, que decía lo contrario.
     */
    @Test
    void plazaSinAulaEnElProblemaConAulaEnLaSolucion_violacion() {
        ProblemaHorario problema = problema(sinAula());

        List<Violacion> vs = fuera(problema, en(problema, X));

        assertThat(vs).singleElement().satisfies(v -> {
            assertThat(v.recursoCodigo()).isEqualTo("X");
            assertThat(v.tramoCodigo()).isEqualTo("L1");
            assertThat(v.celdas()).containsExactly(new CeldaRef("ACT", 1, "ACT-P1"));
            assertThat(v.descripcion()).contains("ACT-P1").contains("X")
                    .contains("no tiene ninguna aula posible");
        });
    }

    @Test
    void instanciaSinColocar_noDuplicaElAvisoDeSinColocar() {
        ProblemaHorario problema = problema(fija(X));

        assertThat(fuera(problema, new SolucionHorario(Map.of()))).isEmpty();
    }
}
