package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
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
 * Cierre de Fase 4 sobre el par 3ºA / 3ºADi (grupo ordinario + subgrupo de
 * Diversificación PDC), traducido del horario real de §6.2 del modelo de datos
 * (PDFs de 3ºA, pág. 8, y 3ºA PDC, pág. 9).
 *
 * <p>Valida que el solver gestiona un grupo PDC y su grupo padre como grupos
 * <b>independientes</b> para S9 (S9 ciega al {@code grupoPadre}): sus sesiones
 * propias pueden caer en tramos distintos, y sus sesiones compartidas se
 * modelan como UNA plaza cuyos subgrupos enlazan ambos grupos.
 *
 * <p>Configuración del fixture:
 * <ul>
 *   <li><b>Compartidas</b> (EF, Tec): una plaza con subgrupos
 *       {@code {3ºA-Completo, 3ºADi-Completo}}. Tocan ambos grupos a la vez;
 *       S9 las cuenta una sola vez por grupo, sin pedir no-solape consigo misma.</li>
 *   <li><b>Propias del Di</b> (AmbSL/LEN2, AmbCM/MAT4 en A8): plaza con solo
 *       {@code 3ºADi-Completo}.</li>
 *   <li><b>Propias del ordinario</b> (FQ, Mat en B01): plaza con solo
 *       {@code 3ºA-Completo}.</li>
 * </ul>
 *
 * <p>El fixture es factible y, por construcción (14 instancias en 12 tramos,
 * con A8 y B01 saturadas), fuerza al menos un tramo donde 3ºA y 3ºADi están
 * ocupados por instancias <b>distintas</b>. Ese cruce es infactible si S9
 * fundiera Di con su padre: es la prueba de que la ceguera al grupoPadre está
 * activa. La discriminación negativa (fundir ⇒ INFEASIBLE) se comprobó en el
 * diseño del fixture; aquí se afirma el lado positivo sobre la solución real.
 */
class SolverHorarioCierreFase4Test {

    private static final String FIXTURE =
            "/fixtures/problema-4-pdc-3a-3adi.json";

    @Test
    @DisplayName("Cierre Fase 4: PDC 3ºA/3ºADi, criterios 1-4")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void cierreDeFase4() throws Exception {
        ProblemaHorario problema = cargar(FIXTURE);

        // Sanity check del dataset cargado.
        assertThat(problema.actividades()).hasSize(6);
        assertThat(problema.tramos()).hasSize(12);
        assertThat(problema.subgrupos()).hasSize(2);
        assertThat(problema.grupos()).hasSize(2);

        GrupoAdministrativo g3A   = grupo(problema, "3ºA");
        GrupoAdministrativo g3ADi = grupo(problema, "3ºADi");
        // Confirma la relación padre/hijo en el dominio (sin esto, el resto del
        // test no probaría "PDC vs padre", solo dos grupos cualesquiera).
        assertThat(g3ADi.grupoPadre()).as("3ºADi declara a 3ºA como padre")
                .contains(g3A);
        assertThat(g3A.grupoPadre()).as("3ºA no tiene padre").isEmpty();

        SolucionHorario solucion = new SolverHorario().resolver(problema);
        assertThat(solucion).as("solución factible").isNotNull();

        // --- Red transversal: 0 violaciones de las restricciones duras. ---
        assertThat(new VerificadorSolucion().verificar(problema, solucion).violaciones())
                .as("violaciones de restricciones duras")
                .isEmpty();

        // ------------------------------------------------------------------
        // Criterio 1 — 3ºADi no tiene sesiones solapadas con su grupo ordinario
        // 3ºA. "Solapada" significa aquí: dos instancias DISTINTAS en el mismo
        // tramo, una tocando 3ºA y otra tocando 3ºADi. Una compartida (que toca
        // ambos) NO es un solape: es la misma instancia. Lo garantiza S9 con la
        // red transversal de arriba (0 violaciones de grupo). Aquí se documenta
        // explícitamente recorriendo la solución por tramo.
        // ------------------------------------------------------------------
        // (cubierto por las 0 violaciones del verificador: S9 prohíbe que dos
        // instancias distintas toquen el mismo grupo en el mismo tramo.)

        // ------------------------------------------------------------------
        // Criterio 2 — las sesiones compartidas (EF, Tec) aparecen en el mismo
        // tramo para 3ºA y 3ºADi. Estructural: cada compartida es UNA actividad
        // con UNA plaza cuyos subgrupos enlazan ambos grupos, luego ambos grupos
        // comparten el tramo de la instancia por construcción. Se afirma que
        // cada instancia compartida toca, en efecto, a los dos grupos.
        // ------------------------------------------------------------------
        for (String cod : List.of("EF-3ºA-y-Di", "Tec-3ºA-y-Di")) {
            Actividad compartida = actividad(problema, cod);
            for (int idx = 1; idx <= compartida.repeticionesPorSemana(); idx++) {
                Set<GrupoAdministrativo> tocados = gruposTocados(compartida, idx);
                assertThat(tocados)
                        .as("%s#%d compartida toca 3ºA y 3ºADi a la vez", cod, idx)
                        .containsExactlyInAnyOrder(g3A, g3ADi);
                // y queda colocada en un tramo (un único tramo, por ser una
                // instancia): ambos grupos lo comparten por construcción.
                assertThat(tramoDe(solucion, compartida, idx))
                        .as("%s#%d colocada", cod, idx)
                        .isNotNull();
            }
        }

        // ------------------------------------------------------------------
        // Criterio 3 — LEN2 (profesor de AmbSL, propia del Di) no aparece en
        // ningún otro sitio cuando está con 3ºADi. LEN2 solo imparte AmbSL en
        // este dataset; combinado con 0 colisiones de profesor del verificador,
        // queda probado que LEN2 está exclusivamente en sus sesiones del Di.
        // ------------------------------------------------------------------
        Set<String> profesoresFueraDeAmbSL = new LinkedHashSet<>();
        for (Actividad a : problema.actividades()) {
            if (a.codigo().equals("AmbSL-3ºADi")) {
                continue;
            }
            for (Plaza p : a.plazas()) {
                p.profesores().forEach(pr -> profesoresFueraDeAmbSL.add(pr.codigo()));
            }
        }
        assertThat(profesoresFueraDeAmbSL)
                .as("LEN2 no imparte ninguna otra actividad fuera de AmbSL del Di")
                .doesNotContain("LEN2");

        // ------------------------------------------------------------------
        // Criterio 4 — distinguir, por tramo, qué población está en
        // diversificación. Validación PROGRAMÁTICA (no inspección visual del
        // CLI; D16 queda fuera de Fase 4). Para cada tramo: o bien 3ºA y 3ºADi
        // van juntos en una compartida (una sola instancia toca ambos), o bien
        // cada grupo está en su propia sesión (instancias disjuntas), nunca dos
        // instancias distintas tocando el MISMO grupo. Eso último ya lo prohíbe
        // S9; aquí se afirma que existe al menos un tramo de cada clase, para que
        // el dataset ejercite ambos casos (compartida y divergencia).
        // ------------------------------------------------------------------
        int tramosCompartidos = 0;   // una instancia toca ambos grupos
        int tramosDivergentes = 0;   // 3ºA y 3ºADi en instancias distintas

        for (Tramo t : problema.tramos()) {
            List<Actividad> enTramo = actividadesEnTramo(problema, solucion, t);

            boolean hayCompartidaQueTocaAmbos = false;
            boolean toca3A = false;
            boolean toca3ADi = false;
            int instTocan3A = 0;
            int instTocan3ADi = 0;

            for (Actividad a : enTramo) {
                // una instancia de 'a' está en t; mira a qué grupos toca esa actividad
                Set<GrupoAdministrativo> gs = gruposTocadosPorActividad(a);
                boolean a3A = gs.contains(g3A);
                boolean a3ADi = gs.contains(g3ADi);
                if (a3A) { toca3A = true; instTocan3A++; }
                if (a3ADi) { toca3ADi = true; instTocan3ADi++; }
                if (a3A && a3ADi) {
                    hayCompartidaQueTocaAmbos = true;
                }
            }

            // Invariante del criterio 4: ningún grupo es tocado por dos
            // instancias distintas en el mismo tramo (espejo de S9; refuerzo).
            assertThat(instTocan3A)
                    .as("3ºA tocado por a lo sumo una instancia en el tramo %s", t.codigo())
                    .isLessThanOrEqualTo(1);
            assertThat(instTocan3ADi)
                    .as("3ºADi tocado por a lo sumo una instancia en el tramo %s", t.codigo())
                    .isLessThanOrEqualTo(1);

            if (hayCompartidaQueTocaAmbos) {
                tramosCompartidos++;
            } else if (toca3A && toca3ADi) {
                tramosDivergentes++;
            }
        }

        assertThat(tramosCompartidos)
                .as("existe al menos un tramo donde 3ºA y 3ºADi van juntos (compartida)")
                .isGreaterThanOrEqualTo(1);
        assertThat(tramosDivergentes)
                .as("existe al menos un tramo donde 3ºA y 3ºADi divergen "
                        + "(instancias distintas) — ejercita la ceguera de S9 al grupoPadre")
                .isGreaterThanOrEqualTo(1);
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

    private GrupoAdministrativo grupo(ProblemaHorario problema, String codigo) {
        return problema.grupos().stream()
                .filter(g -> g.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("grupo no encontrado: " + codigo));
    }

    private Tramo tramoDe(SolucionHorario solucion, Actividad actividad, int indice) {
        ActividadInstancia inst = new ActividadInstancia(actividad, indice);
        return solucion.tramoDeInstancia(inst)
                .orElseThrow(() -> new AssertionError(
                        "instancia sin tramo: " + actividad.codigo() + "#" + indice));
    }

    /** Grupos tocados por una instancia concreta (vía los subgrupos de sus plazas). */
    private Set<GrupoAdministrativo> gruposTocados(Actividad actividad, int indice) {
        // El conjunto de grupos tocados depende solo de la actividad (sus plazas
        // y subgrupos), no del índice de instancia; el índice se acepta por
        // simetría con el resto de helpers y para fijar que la instancia existe.
        return gruposTocadosPorActividad(actividad);
    }

    private Set<GrupoAdministrativo> gruposTocadosPorActividad(Actividad actividad) {
        Set<GrupoAdministrativo> gs = new LinkedHashSet<>();
        for (Plaza p : actividad.plazas()) {
            for (Subgrupo s : p.subgrupos()) {
                gs.add(s.grupo());
            }
        }
        return gs;
    }

    /** Actividades con alguna instancia colocada en el tramo dado. */
    private List<Actividad> actividadesEnTramo(ProblemaHorario problema,
                                               SolucionHorario solucion, Tramo t) {
        List<Actividad> out = new ArrayList<>();
        for (Actividad a : problema.actividades()) {
            for (int idx = 1; idx <= a.repeticionesPorSemana(); idx++) {
                ActividadInstancia inst = new ActividadInstancia(a, idx);
                if (solucion.tramoDeInstancia(inst).filter(t::equals).isPresent()) {
                    out.add(a);
                    break; // basta una instancia de esta actividad en t
                }
            }
        }
        return out;
    }
}
