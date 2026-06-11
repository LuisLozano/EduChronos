package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cierre de Fase 3 sobre el dataset REAL del bloque coordinado CyR/OyD/RefMt
 * de 1ºESO (§6.1 del modelo de datos), con cuatro actividades testigo (una
 * Mat por grupo) para que el bloqueo de grupo (S9) tenga algo que expulsar.
 *
 * <p>Ejercita commit 1 (no-solape por grupo) y commit 2 (elección de aula
 * entre candidatas) juntos. Valida los criterios 1-4 de Fase 3. El criterio 5
 * (bloqueo manual de tramo) se difiere: no hay mecanismo en el modelo actual.
 *
 * <p>El bloque es UNA actividad de seis plazas con repeticionesPorSemana=2,
 * patrón NEUTRA y asignatura nula. Las seis plazas comparten tramo por
 * construcción (un IntVar de tramo por instancia). Las dos instancias caen en
 * tramos distintos porque comparten profesores, subgrupos y (la fija) aula:
 * S1/S2/S3/S9 lo fuerzan, no la distribución por día.
 */
class SolverHorarioCierreFase3Test {

    private static final String FIXTURE =
            "/fixtures/problema-3-cierre-cyr-refmt.json";

    private static final String BLOQUE = "Bloque-CyR_OyD_RefMt-1ESO";

    @Test
    @DisplayName("Cierre Fase 3: bloque CyR/OyD/RefMt real, criterios 1-4")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void cierreDeFase3() throws Exception {
        ProblemaHorario problema = cargar(FIXTURE);

        // Sanity check del dataset cargado.
        assertThat(problema.actividades()).hasSize(5);
        assertThat(problema.tramos()).hasSize(30);
        assertThat(problema.subgrupos()).hasSize(28);
        assertThat(problema.profesores()).hasSize(10);
        assertThat(problema.grupos()).hasSize(4);

        SolucionHorario solucion = new SolverHorario().resolver(problema);
        assertThat(solucion).as("solución factible").isNotNull();

        // --- Red transversal: 0 violaciones de las 5 restricciones duras. ---
        // El verificador es ciego a D15 (cuenta aula por instancia con Set), así
        // que NO cubre por sí solo el criterio 4: las aulas distintas se afirman
        // explícitamente más abajo.
        assertThat(new VerificadorSolucion().verificar(problema, solucion).violaciones())
                .as("violaciones de restricciones duras")
                .isEmpty();

        // ------------------------------------------------------------------
        // Criterio 1 — las sesiones del desdoble CyR caen en el mismo tramo.
        // Estructural: las seis plazas son una sola actividad, luego comparten
        // el tramo de su instancia por construcción. Aquí se documenta que cada
        // una de las dos instancias del bloque queda colocada y que ambas caen
        // en tramos distintos (consecuencia de S1/S2/S3/S9 sobre los recursos
        // compartidos por las dos instancias).
        // ------------------------------------------------------------------
        Actividad bloque = actividad(problema, BLOQUE);
        Tramo tramoInst1 = tramoDe(solucion, bloque, 1);
        Tramo tramoInst2 = tramoDe(solucion, bloque, 2);
        assertThat(tramoInst1).as("instancia 1 del bloque colocada").isNotNull();
        assertThat(tramoInst2).as("instancia 2 del bloque colocada").isNotNull();
        assertThat(tramoInst1)
                .as("las dos instancias del bloque en tramos distintos")
                .isNotEqualTo(tramoInst2);

        Set<Tramo> tramosBloque = new LinkedHashSet<>();
        tramosBloque.add(tramoInst1);
        tramosBloque.add(tramoInst2);

        // ------------------------------------------------------------------
        // Criterio 2 — ningún grupo tiene otra sesión en el tramo del bloque.
        // Para cada grupo, su actividad Mat testigo (todas las repeticiones) no
        // puede caer en ninguno de los dos tramos del bloque. Es lo que prueba
        // S9: los seis subgrupos de un grupo cubren el grupo, así que el grupo
        // queda bloqueado en ese tramo.
        // ------------------------------------------------------------------
        for (String g : List.of("1ºA", "1ºB", "1ºC", "1ºD")) {
            Actividad mat = actividad(problema, "Mat-" + g);
            for (int idx = 1; idx <= mat.repeticionesPorSemana(); idx++) {
                Tramo t = tramoDe(solucion, mat, idx);
                assertThat(t)
                        .as("Mat-%s#%d no debe caer en un tramo del bloque", g, idx)
                        .isNotIn(tramosBloque);
            }
        }

        // ------------------------------------------------------------------
        // Criterio 3 — los tres profesores de RefMt están libres en el tramo
        // del bloque. MAT6/MAT7/MAT4 solo aparecen en el bloque (no en los
        // testigos, que usan MATA..MATD). Combinado con la ausencia de colisión
        // de profesor del verificador y con el criterio 2 (ninguna otra
        // actividad comparte tramo con el bloque), quedan libres.
        // ------------------------------------------------------------------
        Set<String> profesoresFueraDelBloque = new LinkedHashSet<>();
        for (Actividad a : problema.actividades()) {
            if (a.codigo().equals(BLOQUE)) {
                continue;
            }
            for (Plaza p : a.plazas()) {
                p.profesores().forEach(pr -> profesoresFueraDelBloque.add(pr.codigo()));
            }
        }
        assertThat(profesoresFueraDelBloque)
                .as("los profesores de RefMt no imparten ninguna otra actividad")
                .doesNotContain("MAT6", "MAT7", "MAT4");

        // ------------------------------------------------------------------
        // Criterio 4 — aulas del desdoble/agrupamiento distintas y correctas,
        // para CADA instancia del bloque. Cubre D15 de forma explícita: el
        // verificador no detectaría dos plazas de la misma instancia con la
        // misma aula, así que se afirma la disjunción aquí, plaza a plaza.
        // ------------------------------------------------------------------
        for (int idx = 1; idx <= 2; idx++) {
            // Aula fija de informática: INF1 siempre en A12In.
            assertThat(aulaElegida(solucion, bloque, idx, "Bloque-CyR-Inf").codigo())
                    .as("CyR-INF1 en A12In (fija) — instancia %d", idx)
                    .isEqualTo("A12In");

            // Candidata de tecnología: una de las dos válidas.
            assertThat(aulaElegida(solucion, bloque, idx, "Bloque-CyR-Tec").codigo())
                    .as("CyR-TEC3 en una candidata válida — instancia %d", idx)
                    .isIn("A5", "B07");

            // Las tres aulas de RefMt deben ser distintas entre sí.
            Set<String> aulasRefMt = new LinkedHashSet<>();
            aulasRefMt.add(aulaElegida(solucion, bloque, idx, "Bloque-RefMt-MAT6").codigo());
            aulasRefMt.add(aulaElegida(solucion, bloque, idx, "Bloque-RefMt-MAT7").codigo());
            aulasRefMt.add(aulaElegida(solucion, bloque, idx, "Bloque-RefMt-MAT4").codigo());
            assertThat(aulasRefMt)
                    .as("las tres aulas de RefMt distintas — instancia %d", idx)
                    .hasSize(3);

            // Y las seis plazas del bloque, en seis aulas distintas.
            List<String> seisAulas = new ArrayList<>();
            for (String plaza : List.of("Bloque-CyR-Tec", "Bloque-CyR-Inf", "Bloque-OyD",
                    "Bloque-RefMt-MAT6", "Bloque-RefMt-MAT7", "Bloque-RefMt-MAT4")) {
                seisAulas.add(aulaElegida(solucion, bloque, idx, plaza).codigo());
            }
            assertThat(new LinkedHashSet<>(seisAulas))
                    .as("las seis plazas del bloque en seis aulas distintas — instancia %d", idx)
                    .hasSize(6);
        }
    }

    // ---- helpers ----

    private ProblemaHorario cargar(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s en classpath", path).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private Actividad actividad(ProblemaHorario problema, String codigo) {
        return problema.actividades().stream()
                .filter(a -> a.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("actividad no encontrada: " + codigo));
    }

    private Tramo tramoDe(SolucionHorario solucion, Actividad actividad, int indice) {
        ActividadInstancia inst = new ActividadInstancia(actividad, indice);
        return solucion.tramoDeInstancia(inst)
                .orElseThrow(() -> new AssertionError(
                        "instancia sin tramo: " + actividad.codigo() + "#" + indice));
    }

    private Aula aulaElegida(SolucionHorario solucion, Actividad actividad,
                             int indice, String codigoPlaza) {
        Plaza plaza = actividad.plazas().stream()
                .filter(p -> p.codigo().equals(codigoPlaza))
                .findFirst()
                .orElseThrow(() -> new AssertionError("plaza no encontrada: " + codigoPlaza));
        ActividadInstancia inst = new ActividadInstancia(actividad, indice);
        return solucion.aulaElegida(inst, plaza)
                .orElseThrow(() -> new AssertionError(
                        "sin aula elegida para " + actividad.codigo() + "/" + codigoPlaza
                                + " (instancia " + indice + ")"));
    }
}
