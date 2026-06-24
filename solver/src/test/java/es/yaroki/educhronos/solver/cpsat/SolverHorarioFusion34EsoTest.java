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
 * Fase 5 — FUSIÓN DE NIVELES (Bloque 9): primer fixture que reúne dos linajes
 * antes independientes en un único problema. Funde 3º ESO (3A/3B/3C + 3PDC,
 * extraído del linaje {@code problema-5-escala-instituto}) con 4º ESO completo
 * (4A/4B/4C/4D + 4APDC/4DPDC, linaje {@code problema-5-escala-4ESO-Di}).
 *
 * <p>OBJETIVO DEL BLOQUE: ejercitar por primera vez <b>D4 (Gim/Pista compartido
 * entre cursos)</b>. Hasta S29 cada nivel tuvo Gim/Pista para sí (4º aislado y
 * 4º completo no agravan D4: el PDC reintegra EF en su grupo de origen). D4 sólo
 * muerde fusionando niveles que compitan por esos espacios en los mismos tramos.
 * El par 3º+4º se elige por atribución limpia: 4º ya satura Gim+Pista en sus dos
 * bloques NEUTRA (AFAVS pone ambos espacios simultáneos en J3/M5), y 3º trae
 * EF-3A y EF-3C clavadas a Pista (aulaFija) más EF-3B en Gim. Si el solver no
 * logra encajar la EF de 3º sin chocar con los bloques de 4º, el INFEASIBLE es
 * atribuible a D4 y no al ruido de un fixture grande.
 *
 * <p>MODELADO (Opción B, conservador — decisión de S30): se funde respetando el
 * modelado existente de cada linaje SIN relajar nada. En particular, la EF de 3º
 * conserva su {@code aulaFija} (no se convierte a {@code aulasCandidatas
 * [Gim,Pista]}). Es deliberado: si la rigidez de aula fija fuerza INFEASIBLE,
 * habremos probado que D4 muerde con el modelado conservador, que es la prueba
 * que el proyecto espera desde Fase 1. Un FACTIBLE conseguido relajando a la vez
 * no distinguiría si la fija habría bastado. El segundo turno (relajar a
 * candidatas) sólo se abre si este test sale INFEASIBLE, con atribución perfecta.
 *
 * <p>UNIFICACIÓN DE CATÁLOGOS (trabajo previo, cruzado POR CÓDIGO, no a ojo):
 * <ul>
 *   <li><b>16 profesores compartidos por código</b> (BYG3, DIB2, EFI1, EFI3,
 *       FRA1, GH4, INF1, ING2, ING3, LEN1, LEN7, MAT4, MAT5, MAT6, ORI1, REL1):
 *       fundidos asumiendo código = persona (convención del proyecto, clave
 *       natural). Carga tras fusión ≤ 17 h (GH4); ninguno satura los 30 tramos.</li>
 *   <li><b>7 aulas compartidas</b> (A12In, A6, A9, B07, Gim, Pista, TALL1):
 *       misma definición física; diferencias sólo cosméticas en el nombre
 *       ("Aula 10" vs "Aula A10"), unificadas al nombre del linaje instituto.</li>
 *   <li><b>Cero colisiones</b> de código de grupo, subgrupo y actividad entre 3º
 *       y 4º; tramos idénticos entre linajes.</li>
 * </ul>
 *
 * <p>CUADRE POR GRUPO heredado de cada linaje sin tocar: los 9 grupos ordinarios
 * (3A/3B/3C + 4A/4B/4C/4D + los Di que reintegran) cuadran a 30 h; <b>3PDC suma
 * 22 h</b> porque conserva la "opción 2" de S23 (sus 8 sesiones compartidas con
 * el ordinario se imputan a 3A/3B/3C vía tocaGrupo, no al subgrupo Di). Esto
 * diverge del PDC de 4º (que cuadra a 30 porque EF/tutoría son plaza conjunta
 * {4X,4XPDC}): son dos decisiones de modelado conscientes y distintas, ambas
 * fundidas tal cual. La fusión NO reconcilia los dos estilos de PDC.
 *
 * <p>Medición wall-clock alrededor de {@link SolverHorario#resolver} en modo
 * factibilidad pura (sin objetivo): tiempo hasta PRIMERA solución factible.
 * Semilla fija (42) para reproducibilidad. El {@code @Timeout} actúa como red.
 */
class SolverHorarioFusion34EsoTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-3-4-eso.json";

    /** Coherente con el linaje de factibilidad: misma red de 600 s que los demás escala. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Fusión 3º+4º ESO (D4: Gim/Pista compartido): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void fusion34Eso() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset fundido (cuadra con la unificación verificada
        // por código): 10 grupos = 4 de 3º (3A/3B/3C/3PDC) + 6 de 4º
        // (4A/4B/4C/4D/4APDC/4DPDC); 30 tramos; 155 subgrupos = 39 de 3º + 116
        // de 4º (sin solape de códigos); 79 actividades = 40 de 3º + 39 de 4º.
        assertThat(problema.grupos())
                .as("10 grupos: 4 de 3º (3A/3B/3C/3PDC) + 6 de 4º (4A/4B/4C/4D/4APDC/4DPDC)")
                .hasSize(10);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("155 subgrupos: 39 de 3º + 116 de 4º")
                .hasSize(155);
        assertThat(problema.actividades())
                .as("79 actividades: 40 de 3º + 39 de 4º")
                .hasSize(79);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Factibilidad: si el solver agotara el límite sin solución, resolver()
        // lanzaría HorarioInfactibleException y el test fallaría antes de aquí.
        // Un INFEASIBLE en esta fusión NO es un bug del test: sería la primera
        // evidencia de que D4 muerde (Gim/Pista no caben para 3º+4º a la vez con
        // el modelado conservador de aula fija). Ese resultado abre el segundo
        // turno (relajar EF de 3º a aulasCandidatas [Gim,Pista]).
        System.out.printf(
                "[FUSION] 3º+4º ESO (D4 Gim/Pista): solución factible en %.3f s (límite %.0f s)%n",
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
                SolverHorarioFusion34EsoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
