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
 * Fase 5 — escala de 4ºESO COMPLETO (Turno 2: ordinario + Diversificación): el
 * solver resuelve los 4 grupos ordinarios (4A/4B/4C/4D) MÁS los dos grupos de
 * diversificación (4APDC, 4DPDC) en un único fixture, construido desde los seis
 * volcados fieles grupo-4-ESO-{A,B,C,D,A-PDC,D-PDC}.json y verificado plaza a
 * plaza contra ellos.
 *
 * <p>Mismo linaje AISLADO de 4º que {@link SolverHorarioEscala4EsoTest}
 * (decisión 1a; separado de {@code SolverHorarioEscalaInstitutoTest}): cierra 4º
 * como nivel completo antes de la fusión de niveles (criterios 1-2 de Fase 5).
 *
 * <p>ESTRUCTURA INTRODUCIDA respecto a 4º ordinario aislado (S28):
 * <ul>
 *   <li><b>Tipo 5 (Diversificación/PDC) a escala con DOS grupos Di que cursan
 *       el ámbito JUNTOS</b>: 4APDC y 4DPDC como grupos
 *       {@code DIVERSIFICACION_PDC} con grupoPadre 4A/4D. Sus 26 sesiones de
 *       ámbito son IDÉNTICAS tramo a tramo (verificado celda a celda: mismo
 *       día/asignatura/profesor/aula); los alumnos de diversificación de 4A y 4D
 *       cursan el ámbito juntos en B04, igual que el 3PDC de S23 reunía a los Di
 *       de 3A/3B/3C en el tronco A8. Por eso el ámbito es UNA actividad con
 *       subgrupo compartido {4APDC,4DPDC}, no dos linajes separados (modelarlo
 *       separado duplica la demanda de B04 → 47 h en 30 tramos → INFEASIBLE).</li>
 *   <li><b>Regla S23 (subgrupo Di lista SÓLO los Di, no los ordinarios)</b>: el
 *       subgrupo de ámbito {4APDC,4DPDC} NO incluye 4A/4D. Listar los padres
 *       reproduciría el INFEASIBLE de S23 (las 26 h de ámbito se sumarían a las
 *       30 h ordinarias de 4A/4D vía tocaGrupo). Las 26 sesiones se agrupan en 8
 *       actividades por (asignatura, profesor): EXPRE rota entre C01 y TALL1
 *       según el día y se modela con una plaza de aulasCandidatas [C01, TALL1],
 *       no como dos actividades por aula (patrón de S28).</li>
 *   <li><b>Reintegración del PDC en su grupo de origen</b>: a diferencia del
 *       ámbito (común a ambos Di), EF y tutoría son específicas de cada Di con
 *       SU grupo de origen: 4APDC con 4A (EFI1/Gim), 4DPDC con 4D (EFI3/Pista).
 *       Plaza ÚNICA conjunta con subgrupo {4X,4XPDC} (clase física compartida,
 *       verificada celda a celda). No se duplican.</li>
 *   <li><b>Agrupamiento de nivel ampliado a 6 grupos</b>: el bloque Rel+ATEDU
 *       del V4 cubre ahora los 4 ordinarios MÁS los 2 PDC, repartidos en las 5
 *       bandas (1 Rel + 4 ATEDU), como una sola actividad multi-plaza.</li>
 * </ul>
 *
 * <p>DEUDA CONSCIENTE (no afecta a la factibilidad; a confirmar con el centro):
 * <ul>
 *   <li>Reparto de población del PDC en las 5 bandas del V4 (B-completo): calca
 *       el patrón de los ordinarios (un subgrupo por banda), pero el volcado no
 *       demuestra cuántos alumnos del PDC van a cada banda. Invariante de
 *       población viva (subgrupo ≠ alumno).</li>
 *   <li>La deuda DIG/TEC/FOPP de 6 h del 4º ordinario (S28) se mantiene sin
 *       cambios; el PDC no la toca.</li>
 *   <li>La tutoría (TUT4) se modela como actividad ordinaria: el dominio del
 *       solver no transporta "tutor obligatorio" (S8 no se ejercita aquí).</li>
 * </ul>
 *
 * <p>Nota sobre D4 (Gim/Pista compartido): 4º completo NO agrava D4 respecto a
 * 4º ordinario. El PDC hace EF reintegrado en su grupo (misma plaza física), no
 * añade presión sobre Gim/Pista. Los tramos calientes siguen siendo J3 y M5
 * (AFAVS pone Gim+Pista simultáneos para los 4 ordinarios). D4 sólo morderá al
 * fusionar con otro nivel que compita por esos tramos.
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver} en modo
 * factibilidad pura (sin objetivo): tiempo hasta PRIMERA solución factible.
 * Semilla fija (42) para reproducibilidad. El {@code @Timeout} actúa como red.
 */
class SolverHorarioEscala4EsoDiTest {

    private static final String FIXTURE = "/fixtures/problema-5-escala-4ESO-Di.json";

    /** Coherente con el linaje de factibilidad: misma red de 600 s que escala-instituto. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Escala 4ºESO completo (4 ordinarios + 2 PDC): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void escala4EsoCompleto() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset cargado (cuadra con el fixture verificado:
        // 6 grupos = 4 ordinarios + 2 PDC; 30 tramos; 116 subgrupos;
        // 39 actividades = 31 de 4º ordinario + 8 de ámbito compartido de los
        // dos PDC (EXPRE unificado con aulasCandidatas [C01, TALL1]). El ámbito
        // es compartido {4APDC,4DPDC}, no duplicado por PDC.
        assertThat(problema.grupos())
                .as("6 grupos: 4 ordinarios (4A/4B/4C/4D) + 2 PDC (4APDC/4DPDC)")
                .hasSize(6);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("116 subgrupos")
                .hasSize(116);
        assertThat(problema.actividades())
                .as("39 actividades: 31 de 4º ordinario + 8 de ámbito compartido PDC")
                .hasSize(39);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Factibilidad: si el solver hubiera agotado el límite sin solución,
        // resolver() habría lanzado HorarioInfactibleException y el test
        // fallaría antes de llegar aquí. El horario real existe (verificado:
        // cero conflictos físicos profesor/aula sobre tramos reales) ⇒ debe ser
        // factible; un INFEASIBLE sería hallazgo de modelado del solver.
        System.out.printf(
                "[ESCALA] 4ºESO completo (4 ord + 2 PDC): solución factible en %.3f s (límite %.0f s)%n",
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
                SolverHorarioEscala4EsoDiTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
