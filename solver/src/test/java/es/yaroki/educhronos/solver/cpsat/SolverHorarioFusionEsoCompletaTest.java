package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Fase 5 — FUSIÓN ESO COMPLETA (Bloque 10): prueba decisiva de D4. Reúne los dos
 * linajes de fusión previos en un único problema con toda la ESO: 1º+2º+3º
 * (linaje {@code problema-5-escala-instituto}: 1A/1B/1C/1D, 2A/2B/2C, 3A/3B/3C +
 * 3PDC) y 4º completo (linaje {@code problema-5-escala-4ESO-Di}: 4A/4B/4C/4D +
 * 4APDC/4DPDC). 17 grupos en total.
 *
 * <p>OBJETIVO DEL BLOQUE: someter <b>D4 (Gim/Pista compartido entre cursos)</b> a
 * competencia REAL, no holgada. El par 3º+4º (Bloque 9, S30) salió FACTIBLE en
 * 0,300 s con Gim a 8 h y Pista a 10 h sobre 30: holgura cómoda, no competencia.
 * La fusión ESO completa lleva la demanda de Gim a <b>26 h sobre 30</b> (EF de 1º
 * 4×3=12 + EF de 2º 3×2=6 + EF-3B 2 + EFis-4A 2 + EFis-4C 2 + AFAVS-EFI1 en los
 * dos bloques NEUTRA de 4º 2) y Pista a 10 h. El cuello no es el volumen sumado
 * (36 h entre dos espacios de 30 caben), sino que Gim tiene solo 4 tramos de
 * margen para 26 sesiones distintas, y dos de ellas (los NEUTRA de 4º, AFAVS-EFI1
 * en Gim + AFAVS-EFI3 en Pista, J3/M5) están rígidamente acopladas a tramos donde
 * compiten con la EF clavada a Pista de 3º (EF-3A, EF-3C en aulaFija). Ahí muerde
 * D4 si va a morder.
 *
 * <p>MODELADO (Opción B, conservador — misma decisión que S30): se funden los dos
 * linajes respetando su modelado SIN relajar nada. La EF de 3º conserva su
 * {@code aulaFija} (no se convierte a {@code aulasCandidatas [Gim,Pista]}). Es
 * deliberado: si la rigidez de aula fija fuerza INFEASIBLE, habremos probado que
 * D4 muerde a escala con el modelado conservador, que es la prueba que el proyecto
 * espera desde Fase 1. Un FACTIBLE conseguido relajando a la vez no distinguiría
 * si la fija habría bastado. El segundo turno (relajar EF de 3º —y, si procede, la
 * de niveles bajos— a aulasCandidatas) sólo se abre si este test sale INFEASIBLE,
 * con atribución limpia a D4.
 *
 * <p>UNIFICACIÓN DE CATÁLOGOS (trabajo previo, cruzado POR CÓDIGO, no a ojo):
 * <ul>
 *   <li><b>22 profesores compartidos por código</b> entre los linajes 1º-3º y 4º
 *       (el par 3º+4º de S30 veía 16; al sumar 1º/2º aparecen más): fundidos
 *       asumiendo código = persona (convención del proyecto, clave natural). En
 *       diferencias de nombre se prioriza el no-placeholder. Carga tras fusión
 *       ≤ 18 h; ninguno satura los 30 tramos.</li>
 *   <li><b>12 aulas compartidas</b> (A5, A6, A9, A10, A11, A14, A12In, B07, C00,
 *       Gim, Pista, TALL1): misma definición física; diferencias sólo cosméticas
 *       en el nombre ("Aula 10" vs "Aula A10"), unificadas al nombre del linaje
 *       instituto. 4 aulas sólo en 4º (A2, A15, B04, C01).</li>
 *   <li><b>Cero colisiones</b> de código de grupo, subgrupo y actividad entre los
 *       dos linajes; tramos idénticos. (Los códigos de PLAZA no son clave de
 *       identidad —el mapper no los deduplica—: el linaje instituto reutiliza
 *       B2-PEPA/B2-CyR/B2-Fr2 entre las dos actividades de bloque de 2º; legítimo.)</li>
 * </ul>
 *
 * <p>CUADRE POR GRUPO heredado de cada linaje sin tocar: los 16 grupos ordinarios
 * cuadran a 30 h; <b>3PDC suma 22 h</b> porque conserva la "opción 2" de S23 (sus
 * 8 sesiones compartidas con el ordinario se imputan a 3A/3B/3C vía tocaGrupo, no
 * al subgrupo Di). Los Di de 4º cuadran a 30 (EF/tutoría son plaza conjunta
 * {4X,4XPDC}): son dos estilos de modelado de PDC conscientes y distintos, ambos
 * fundidos tal cual. La fusión NO reconcilia los dos estilos.
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver} en modo
 * factibilidad pura (sin objetivo): tiempo hasta PRIMERA solución factible.
 * Semilla fija (42) para reproducibilidad. El {@code @Timeout} actúa como red.
 */
class SolverHorarioFusionEsoCompletaTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-eso-completa.json";

    /** Coherente con el linaje de factibilidad: misma red de 600 s que los demás escala. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Fusión ESO completa 1º-4º (D4: Gim 26/30 en competencia real): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void fusionEsoCompleta() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset fundido (cuadra con la unificación verificada
        // por código): 17 grupos = 11 de 1º-3º (1A/1B/1C/1D + 2A/2B/2C +
        // 3A/3B/3C/3PDC) + 6 de 4º (4A/4B/4C/4D/4APDC/4DPDC); 30 tramos; 232
        // subgrupos = 116 + 116 (sin solape de códigos); 155 actividades = 116
        // de 1º-3º + 39 de 4º.
        assertThat(problema.grupos())
                .as("17 grupos: 11 de 1º-3º + 6 de 4º")
                .hasSize(17);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("232 subgrupos: 116 de 1º-3º + 116 de 4º")
                .hasSize(232);
        assertThat(problema.actividades())
                .as("155 actividades: 116 de 1º-3º + 39 de 4º")
                .hasSize(155);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Factibilidad: si el solver agotara el límite sin solución, resolver()
        // lanzaría HorarioInfactibleException y el test fallaría antes de aquí.
        // Un INFEASIBLE en esta fusión NO es un bug del test: sería la primera
        // evidencia de que D4 muerde a ESCALA (Gim/Pista no caben para toda la
        // ESO a la vez con el modelado conservador de aula fija). Ese resultado
        // abre el segundo turno (relajar EF a aulasCandidatas [Gim,Pista]).
        System.out.printf(
                "[FUSION-ESO] 1º-4º completa (D4 Gim 26/30): solución factible en %.3f s (límite %.0f s)%n",
                segundos, MAX_SEGUNDOS);
        assertThat(segundos)
                .as("tiempo hasta primera solución factible < 600 s")
                .isLessThan(MAX_SEGUNDOS);

        // Cero restricciones duras violadas (red de seguridad independiente).
        // Incluye S2 (aula sin doble uso): es donde D4 se manifestaría si el
        // solver intentara poner dos cursos en Gim/Pista en el mismo tramo.
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioFusionEsoCompletaTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
