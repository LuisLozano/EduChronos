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
 * Fase 5 — medición de escala: el solver resuelve 1ºESO + 2ºESO + 3ºESO (10
 * grupos ordinarios reales del instituto: 4 de 1º + 3 de 2º + 3 de 3º), dataset
 * construido desde el volcado fiel y verificado plaza a plaza contra él.
 *
 * <p>Objetivo de este test: ATACAR EL CRITERIO 1 DE FASE 5 (el solver termina en
 * menos de 10 minutos para el instituto completo) midiendo el tiempo sobre un
 * subconjunto creciente, y el CRITERIO 2 (cero restricciones duras violadas).
 * No es un test de discriminación: no aísla una restricción, acumula volumen.
 *
 * <p>Punto de la curva de escala que mide este test: 10 grupos. El punto anterior
 * (7 grupos → 0,317 s, 0 duras, Sesión 20) queda registrado en el plan; el
 * fixture de escala es único y crece, de modo que la curva se traza en el
 * registro del plan, no reejecutando datasets históricos (decisión S20/S21).
 *
 * <p>ESCALÓN AÑADIDO EN ESTE BLOQUE (3ºESO, Sesión 21): escala pura, ningún
 * cambio de dominio. Estructura introducida respecto a 1º+2º:
 * <ul>
 *   <li>Dos actividades coordinadas de nivel rep=1 (Bloque-3ESO-MAR/JUE), K=6
 *       cada una (CyR×2 desdoblado + refuerzo×3 + BioNu), mayor densidad de
 *       simultaneidad por tramo que cualquier bloque de 1º/2º. CyR y BioNu
 *       comparten subgrupo entre ambos días (continuidad del alumno,
 *       Hallazgo D / I6); el refuerzo rota (RefLe martes / RefMt jueves).</li>
 *   <li>Religión partida en dos actividades multi-grupo: AB (3A+3B, viernes) y
 *       C (3C sola, lunes), Hallazgo F.</li>
 *   <li>Sesiones compartidas ordinario+Di (EF, EPVA, Tec, TUT3) modeladas con
 *       subgrupo del grupo ordinario solo: el subgrupo Di NO se incluye en este
 *       escalón (PDC a escala diferido a bloque posterior, decisión S21).</li>
 * </ul>
 *
 * <p>DEUDA CONSCIENTE (particiones plausibles, a confirmar con el centro; no
 * afectan a la factibilidad, solo al reparto concreto de alumnos):
 * <ul>
 *   <li>Reparto de la población de nivel entre las plazas de las coordinadas
 *       (qué alumnos van a CyR-Inf vs CyR-Tec vs cada profesor de refuerzo vs
 *       BioNu).</li>
 *   <li>Reparto de los no-religión de 3A/3B entre las dos plazas ATED del
 *       viernes (FIL1 vs ING3).</li>
 *   <li>Geo→Geogr normalizado en 3º (divergencia de extracción del volcado de
 *       3ºB; misma materia, 3 h/sem en los tres grupos).</li>
 * </ul>
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver}, que
 * incluye construcción del modelo + solve + extracción (el tiempo que el
 * usuario percibe). El solver corre en modo factibilidad pura (sin objetivo):
 * lo medido es tiempo hasta PRIMERA solución factible, no hasta óptimo. La
 * semilla fija (42) hace la medición reproducible.
 *
 * <p>El {@code @Timeout} de 660 s actúa como red: si el solver no termina en
 * ~11 min, el test falla (señal de Fase 5: "no converge tras 15 min").
 */
class SolverHorarioEscalaInstitutoTest {

    private static final String FIXTURE = "/fixtures/problema-5-escala-instituto.json";

    /** Límite del solver: 600 s = 10 min (criterio 1 de Fase 5). */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName("Escala 1º+2º+3º ESO (10 grupos): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void escala1y2y3ESO() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset cargado (cuadra con el fixture verificado:
        // 4 grupos de 1º + 3 de 2º + 3 de 3º = 10; 110 actividades).
        assertThat(problema.grupos()).as("10 grupos: 4 de 1º + 3 de 2º + 3 de 3º").hasSize(10);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.actividades()).as("110 actividades").hasSize(110);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Criterio 1 de Fase 5: < 10 minutos (el solver internamente ya está
        // capado a 600 s; si hubiera agotado el límite sin factible, resolver()
        // habría lanzado HorarioInfactibleException y el test fallaría antes).
        System.out.printf(
                "[ESCALA] 1º+2º+3º ESO (10 grupos): solución factible en %.3f s (límite %.0f s)%n",
                segundos, MAX_SEGUNDOS);
        assertThat(segundos)
                .as("tiempo hasta primera solución factible < 600 s (criterio 1 Fase 5)")
                .isLessThan(MAX_SEGUNDOS);

        // Criterio 2 de Fase 5: cero restricciones duras violadas.
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioEscalaInstitutoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
