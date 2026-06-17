package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Cierre del Bloque 1 de Fase 5: Religion multi-grupo por parejas (Tipo 4).
 *
 * <p>Dataset real cruzado de los PDFs (Sesion 19): el bloque Religion/ATED de
 * 1ESO se organiza por PAREJAS. Religion reune A+B en un tramo (REL1, A5) y C+D
 * en otro (REL1, A3); la Atencion Educativa es per-grupo en el mismo tramo que
 * la Religion de su pareja (1A->ING6/A17, 1B->GH5/A11, 1C->GH2/C00, 1D->FRA1/A14).
 *
 * <p>Cada bloque es UNA actividad multi-plaza cuyos subgrupos (mono-grupo,
 * Lectura A) cubren los dos grupos del par. Esto ejercita por primera vez con
 * dataset real el Tipo 4: una actividad coordinada que bloquea VARIOS grupos
 * COMPLETOS via S9 (no-solape por grupo, ciega al grupoPadre).
 *
 * <p>Aserciones (equivalentes a la validacion previa en diseno):
 * <ol>
 *   <li>0 violaciones de las restricciones duras (VerificadorSolucion).</li>
 *   <li>REL1 imparte ambos bloques: caen en tramos DISTINTOS (S1 sobre el
 *       profesor compartido los separa sin que el fixture lo fuerce).</li>
 *   <li>S9 mantiene cada Mate testigo FUERA del tramo del bloque de su grupo:
 *       ninguna Mate de 1A/1B en el tramo del bloque AB; ninguna de 1C/1D en el
 *       del bloque CD.</li>
 *   <li>Cada bloque TOCA simultaneamente sus dos grupos (Tipo 4): la unica
 *       instancia del bloque AB cubre subgrupos de 1A y de 1B en el mismo tramo;
 *       idem CD con 1C y 1D.</li>
 * </ol>
 */
class SolverHorarioReligionParejasTest {

    private static final String FIXTURE = "/fixtures/problema-5-religion-parejas-1eso.json";

    @Test
    @Timeout(120)
    @DisplayName("Religion multi-grupo por parejas: 0 violaciones y S9 bloquea ambos grupos del par")
    void cierreBloque1Fase5() throws Exception {
        ProblemaHorario problema = cargar();

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        // (1) Cero violaciones de restricciones duras.
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();

        // Tramo elegido por cada bloque (1 repeticion => indice 0).
        Tramo tramoAB = tramoDeActividad(problema, solucion, "Relig_ATED-1AB");
        Tramo tramoCD = tramoDeActividad(problema, solucion, "Relig_ATED-1CD");

        // (2) REL1 reparte los dos bloques en tramos distintos.
        assertThat(tramoAB)
                .as("REL1 imparte AB y CD; S1 debe separarlos en tramos distintos")
                .isNotEqualTo(tramoCD);

        // (3) Ninguna Mate testigo cae en el tramo del bloque de su grupo (S9).
        for (String grupo : List.of("1A", "1B")) {
            assertThat(tramosDeMate(problema, solucion, grupo))
                    .as("Mate de %s no debe coincidir con el tramo del bloque AB", grupo)
                    .doesNotContain(tramoAB);
        }
        for (String grupo : List.of("1C", "1D")) {
            assertThat(tramosDeMate(problema, solucion, grupo))
                    .as("Mate de %s no debe coincidir con el tramo del bloque CD", grupo)
                    .doesNotContain(tramoCD);
        }

        // (4) Cada bloque toca simultaneamente sus dos grupos (Tipo 4).
        assertThat(gruposTocadosPorActividad(problema, "Relig_ATED-1AB"))
                .as("el bloque AB cubre 1A y 1B")
                .contains("1A", "1B");
        assertThat(gruposTocadosPorActividad(problema, "Relig_ATED-1CD"))
                .as("el bloque CD cubre 1C y 1D")
                .contains("1C", "1D");
    }

    // ----------------------------------------------------------------- helpers

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in = SolverHorarioReligionParejasTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en el classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    /** Tramo de la (unica) instancia de una actividad identificada por su codigo. */
    private static Tramo tramoDeActividad(ProblemaHorario problema, SolucionHorario solucion,
                                          String codigoActividad) {
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (inst.actividad().codigo().equals(codigoActividad)) {
                Optional<Tramo> t = solucion.tramoDeInstancia(inst);
                assertThat(t).as("instancia de %s colocada", codigoActividad).isPresent();
                return t.get();
            }
        }
        throw new AssertionError("no se encontro la actividad " + codigoActividad);
    }

    /** Tramos en los que cae cada repeticion de la Mate de un grupo (Mat-<grupo>). */
    private static List<Tramo> tramosDeMate(ProblemaHorario problema, SolucionHorario solucion,
                                            String grupo) {
        String codigoActividad = "Mat-" + grupo;
        List<Tramo> tramos = new ArrayList<>();
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (inst.actividad().codigo().equals(codigoActividad)) {
                solucion.tramoDeInstancia(inst).ifPresent(tramos::add);
            }
        }
        assertThat(tramos).as("la Mate de %s tiene instancias colocadas", grupo).isNotEmpty();
        return tramos;
    }

    /** Codigos de los grupos cuyos subgrupos aparecen en alguna plaza de la actividad. */
    private static List<String> gruposTocadosPorActividad(ProblemaHorario problema,
                                                          String codigoActividad) {
        List<String> grupos = new ArrayList<>();
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (!inst.actividad().codigo().equals(codigoActividad)) {
                continue;
            }
            for (Plaza plaza : inst.actividad().plazas()) {
                for (Subgrupo sg : plaza.subgrupos()) {
                    for (GrupoAdministrativo g : sg.grupos()) {
                        if (!grupos.contains(g.codigo())) {
                            grupos.add(g.codigo());
                        }
                    }
                }
            }
        }
        return grupos;
    }
}
