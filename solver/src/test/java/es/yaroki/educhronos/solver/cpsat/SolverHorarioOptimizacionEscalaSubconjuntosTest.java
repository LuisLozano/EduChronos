package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.ortools.sat.CpSolverStatus;
import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5 — Bloque 18 (S43). EXPERIMENTO PAREADO DE ATRIBUCIÓN: ¿qué bloque
 * estructural endurece la OPTIMIZACIÓN a escala (la NO convergencia de D23), no la
 * factibilidad (demostrada con FPB dentro, S36)?
 *
 * <p>Tres puntos sobre el MISMO fixture de instituto completo
 * ({@code problema-5-fusion-instituto-completo.json}), recortado EN MEMORIA por
 * bloque académico para que la única diferencia entre puntos sean los grupos
 * retirados (catálogo —tramos/aulas/asignaturas/profesores— idéntico en los tres):
 * <ul>
 *   <li>P0 — instituto completo (26 grupos: ESO + Bach + FPB). Línea base.</li>
 *   <li>P1 — sin FPB (24 grupos: ESO + Bach).</li>
 *   <li>P2 — solo ESO (17 grupos, incluidos los 3 PDC).</li>
 * </ul>
 *
 * <p>ATRIBUCIÓN. La frontera de bloque es separable sin referencias colgantes
 * (verificado por réplica en S43: ningún subgrupo ni actividad cruza ESO/Bach/FPB).
 * La lectura se hace sobre DELTAS de estado/cota entre puntos, NO sobre el objetivo
 * absoluto, que por D25 no es reproducible entre runs (varianza ±7). Si P1≈P0 y P2
 * converge o sube mucho la cota ⟹ el factor es Bach (FPB inocente); si P1 ya
 * converge ⟹ FPB era el factor.
 *
 * <p>DISCIPLINA D25. Cada punto agota ~600 s y CP-SAT satura los núcleos; estos tres
 * métodos SOLO son fiables corridos AISLADOS:
 * {@code mvn test -Pescala -Dtest=SolverHorarioOptimizacionEscalaSubconjuntosTest#p0base}
 * (y p1SinFpb, p2SoloEso). Corridos juntos, el dato de objetivo/estado es ruido.
 *
 * <p>CONTRATO, no umbral. Cada método asevera solo FEASIBLE-o-mejor + 0 duras +
 * objetivo≥cota; registra estado/objetivo/cota/tiempo por stdout. El umbral del
 * criterio 3 exige datos del centro: verde NO significa "calidad cumplida".
 */
@Tag("escala")
class SolverHorarioOptimizacionEscalaSubconjuntosTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-instituto-completo.json";
    private static final double MAX_SEGUNDOS = 600.0;

    /** Códigos de los grupos FPB y Bach (identificación por código: ambos son ORDINARIO). */
    private static final Set<String> CODIGOS_FPB = Set.of("1FPB", "2FPB");
    private static final Set<String> CODIGOS_BACH =
            Set.of("1BA", "1BB", "1BC", "1BD", "2BA", "2BB", "2BC");

    @Test
    @DisplayName("P0 instituto completo (26 grupos): línea base, estado/objetivo/cota registrados")
    @Timeout(720)
    void p0base() throws Exception {
        medir("P0-base-26grupos", cargar(), 26, 341, 229);
    }

    @Test
    @DisplayName("P1 sin FPB (24 grupos = ESO + Bach): estado/objetivo/cota registrados")
    @Timeout(720)
    void p1SinFpb() throws Exception {
        ProblemaHorario base = cargar();
        ProblemaHorario p1 = recortar(base, g -> !CODIGOS_FPB.contains(g));
        medir("P1-sinFPB-24grupos", p1, 24, 339, 208);
    }

    @Test
    @DisplayName("P2 solo ESO (17 grupos, incl. PDC): estado/objetivo/cota registrados")
    @Timeout(720)
    void p2SoloEso() throws Exception {
        ProblemaHorario base = cargar();
        ProblemaHorario p2 =
                recortar(base, g -> !CODIGOS_FPB.contains(g) && !CODIGOS_BACH.contains(g));
        medir("P2-soloESO-17grupos", p2, 17, 232, 155);
    }

    /**
     * Mide la optimización a escala sobre {@code problema} y registra el resultado.
     * Las tres cifras esperadas (grupos/subgrupos/actividades) son las verificadas por
     * réplica en S43; sirven de fail-fast si el fixture o el recorte cambian.
     */
    private static void medir(
            String etiqueta,
            ProblemaHorario problema,
            int gruposEsperados,
            int subgruposEsperados,
            int actividadesEsperadas)
            throws Exception {

        assertThat(problema.grupos()).as("%s: nº grupos", etiqueta).hasSize(gruposEsperados);
        assertThat(problema.subgrupos())
                .as("%s: nº subgrupos", etiqueta)
                .hasSize(subgruposEsperados);
        assertThat(problema.actividades())
                .as("%s: nº actividades", etiqueta)
                .hasSize(actividadesEsperadas);
        assertThat(problema.tramos()).as("%s: 30 tramos (catálogo intacto)", etiqueta).hasSize(30);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        ResultadoOptimizacion resultado = solver.resolverOptimizandoConDetalle(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;
        SolucionHorario solucion = resultado.solucion();
        CpSolverStatus estado = resultado.estado();
        double objetivo = resultado.objetivo();
        double cota = resultado.cotaInferior();

        System.out.printf(
                "[OPT-ESCALA-SUB] %s: %.3f s (límite %.0f s) | estado=%s | "
                        + "objetivo=%.1f cota=%.1f gap=%.1f%n",
                etiqueta, segundos, MAX_SEGUNDOS, estado, objetivo, cota, objetivo - cota);

        assertThat(estado)
                .as("%s: estado óptimo probado o mejor-hallada por timeout", etiqueta)
                .isIn(CpSolverStatus.OPTIMAL, CpSolverStatus.FEASIBLE);
        assertThat(objetivo)
                .as("%s: objetivo (%.1f) ≥ cota (%.1f)", etiqueta, objetivo, cota)
                .isGreaterThanOrEqualTo(cota);

        VerificadorSolucion verificador = new VerificadorSolucion();
        var verificacion = verificador.verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("%s: violaciones duras: %s", etiqueta, verificacion.violaciones())
                .isTrue();
    }

    /**
     * Reconstruye un {@link ProblemaHorario} conservando solo los grupos cuyo código
     * cumple {@code conservaGrupo}, arrastrando coherentemente subgrupos y actividades
     * y dejando el catálogo (tramos/aulas/asignaturas/profesores/restricciones) intacto.
     *
     * <p>Fail-fast: como ningún subgrupo ni actividad cruza la frontera de bloque
     * (réplica S43), el recorte no puede dejar referencias colgantes. Se asevera para
     * blindar contra cambios futuros del fixture: un subgrupo solo se conserva si TODOS
     * sus grupos se conservan; si conservara unos sí y otros no, el dato sería corrupto.
     */
    private static ProblemaHorario recortar(
            ProblemaHorario base, java.util.function.Predicate<String> conservaGrupo) {

        Set<GrupoAdministrativo> gruposVivos =
                base.grupos().stream()
                        .filter(g -> conservaGrupo.test(g.codigo()))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> codigosVivos =
                gruposVivos.stream()
                        .map(GrupoAdministrativo::codigo)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<Subgrupo> subgruposVivos =
                base.subgrupos().stream()
                        .filter(s -> particionLimpia(s, codigosVivos, conservaGrupo))
                        .toList();
        Set<Subgrupo> setSubgruposVivos = Set.copyOf(subgruposVivos);

        List<Actividad> actividadesVivas =
                base.actividades().stream()
                        .filter(a -> actividadViva(a, setSubgruposVivos))
                        .toList();

        return new ProblemaHorario(
                base.tramos(),
                base.aulas(),
                base.asignaturas(),
                base.profesores(),
                List.copyOf(gruposVivos),
                subgruposVivos,
                actividadesVivas,
                base.restriccionesHorarias(),
                base.bloqueos(),
                base.tutorias());
    }

    /** Un subgrupo es coherente si TODOS sus grupos sobreviven o NINGUNO; mixto = error de dato. */
    private static boolean particionLimpia(
            Subgrupo s, Set<String> codigosVivos, java.util.function.Predicate<String> conservaGrupo) {
        long vivos = s.grupos().stream().map(GrupoAdministrativo::codigo).filter(codigosVivos::contains).count();
        long total = s.grupos().size();
        if (vivos != 0 && vivos != total) {
            throw new IllegalStateException(
                    "Subgrupo " + s.codigo() + " cruza la frontera del recorte: " + s.grupos());
        }
        return vivos == total; // conservar solo si TODOS viven
    }

    /** Una actividad sobrevive si TODAS las plazas referencian subgrupos vivos; mixta = error. */
    private static boolean actividadViva(Actividad a, Set<Subgrupo> setSubgruposVivos) {
        long plazasVivas =
                a.plazas().stream().filter(p -> plazaViva(p, setSubgruposVivos)).count();
        long total = a.plazas().size();
        if (plazasVivas != 0 && plazasVivas != total) {
            throw new IllegalStateException(
                    "Actividad " + a.codigo() + " cruza la frontera del recorte (plazas mixtas)");
        }
        return plazasVivas == total;
    }

    private static boolean plazaViva(Plaza p, Set<Subgrupo> setSubgruposVivos) {
        return p.subgrupos().stream().allMatch(setSubgruposVivos::contains);
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                     SolverHorarioOptimizacionEscalaSubconjuntosTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}