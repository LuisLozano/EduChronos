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
 * Fase 5 — escala de 4ºESO ordinario (Turno 1, sin Diversificación): el solver
 * resuelve los 4 grupos ordinarios de 4º (4A/4B/4C/4D) construidos desde el
 * volcado fiel grupo-4-ESO-{A,B,C,D}.json y verificados plaza a plaza contra él.
 *
 * <p>Este test NO es un punto de la curva de escala del instituto (linaje
 * separado del de {@link SolverHorarioEscalaInstitutoTest}, decisión 1a): 4º se
 * mide AISLADO porque introduce un salto de régimen propio (D4) que se quiere
 * observar sin el ruido de 1º/2º/3º. La fusión al instituto completo es un paso
 * posterior (criterios 1-2 de Fase 5), no este.
 *
 * <p>ESTRUCTURA INTRODUCIDA respecto a los escalones de ESO ya validados (donde
 * 4º es el primer nivel con optatividad densa como esqueleto del horario, no
 * como añadido):
 * <ul>
 *   <li><b>Tres patrones de agrupamiento inter-grupo simultáneos en un mismo
 *       nivel</b> (Hallazgo K): bloques transversales sobre los 4 grupos
 *       (DT+Ref+CeH+AFAVS los días M5/J3; Rel+ATEDU el V4), sobre 3 grupos
 *       {A,B,D} (FQ+DIG; Biol+TEC+FOPP), y sobre 2 grupos {A,B} (mates partidas
 *       MatAp/MatAc). C y D llevan itinerario propio (C: letras con LAT/ECO;
 *       D: mixto) modelado como ordinarias mono-plaza.</li>
 *   <li><b>Plazas compartidas inter-grupo (Tipo 7)</b>: las optativas de nivel
 *       (TEC/DIG/FOPP) son plazas únicas con un solo profesor y un aula única,
 *       cursadas por subgrupos de varios grupos a la vez; los profesores y
 *       aulas NO se clonan (verificado físicamente celda a celda). EXPRE se
 *       desdobla por capacidad de taller (DIB1/TALL1 para A,B; DIB2/C01 para
 *       C,D) dentro de la misma actividad-bloque.</li>
 *   <li><b>D4 a saturación</b>: AFAVS pone Gim y Pista simultáneos para los 4
 *       grupos (bloques M5 y J3). Es el primer punto donde el recurso Gim/Pista
 *       aprieta de verdad en 4º; el horario real existe, luego hay solución.</li>
 *   <li><b>Cuello INF1/A12In</b>: DIG lo imparte un único profesor (INF1) en un
 *       aula única (A12In) repartido entre dos bloques distintos (6 sesiones).
 *       Tensa el modelo sin hacerlo infactible (6 ≤ 30); si el solver declarase
 *       INFEASIBLE, sería hallazgo de modelado del solver, no del dataset.</li>
 * </ul>
 *
 * <p>DEUDA CONSCIENTE (no afecta a la factibilidad; a confirmar con el centro):
 * <ul>
 *   <li>Las optativas DIG/TEC/FOPP suman 6 h entre dos bloques de perfil
 *       distinto, lo que NO encaja con la optativa única de 3 h de la
 *       prematrícula. Por precaución se modelan como población propia por bloque
 *       (sin reuso de subgrupo entre bloques), de modo que S3 no las acopla:
 *       ¿son la misma optativa de 6 h o franjas distintas? Pendiente del centro.
 *       En cambio DT/CeH/AFAVS sí reusan subgrupo entre los bloques M5 y J3
 *       (misma partición de alumnos demostrada por el volcado).</li>
 *   <li>La tutoría (TUT4) se modela como actividad ordinaria: el dominio del
 *       solver no transporta "tutor obligatorio" (S8 no se ejercita aquí).</li>
 * </ul>
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver} en modo
 * factibilidad pura (sin objetivo): tiempo hasta PRIMERA solución factible.
 * Semilla fija (42) para reproducibilidad. El {@code @Timeout} actúa como red.
 */
class SolverHorarioEscala4EsoTest {

    private static final String FIXTURE = "/fixtures/problema-5-escala-4ESO.json";

    /** Coherente con el linaje de factibilidad: misma red de 600 s que escala-instituto. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Escala 4ºESO ordinario (4 grupos, sin Di): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void escala4EsoOrdinario() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset cargado (cuadra con el fixture verificado:
        // 4 grupos ordinarios, 30 tramos, 96 subgrupos, 31 actividades
        // = 7 bloques transversales NEUTRA + 24 ordinarias DISTRIBUIDA).
        assertThat(problema.grupos())
                .as("4 grupos ordinarios de 4º (4A/4B/4C/4D)")
                .hasSize(4);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("96 subgrupos")
                .hasSize(96);
        assertThat(problema.actividades())
                .as("31 actividades: 7 bloques transversales + 24 ordinarias")
                .hasSize(31);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Factibilidad: si el solver hubiera agotado el límite sin solución,
        // resolver() habría lanzado HorarioInfactibleException y el test
        // fallaría antes de llegar aquí. El horario real existe ⇒ debe ser
        // factible; un INFEASIBLE sería hallazgo de modelado del solver.
        System.out.printf(
                "[ESCALA] 4ºESO ordinario (4 grupos, sin Di): solución factible en %.3f s (límite %.0f s)%n",
                segundos, MAX_SEGUNDOS);
        assertThat(segundos)
                .as("tiempo hasta primera solución factible < 600 s")
                .isLessThan(MAX_SEGUNDOS);

        // Cero restricciones duras violadas (red de seguridad independiente).
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioEscala4EsoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
