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
 * Fase 5 — FUSIÓN INSTITUTO COMPLETO (Bloque 13): el problema entero en un único
 * fixture. Reúne los CINCO linajes de fusión previos: ESO completa 1º-4º (linaje
 * {@code problema-5-fusion-eso-completa}, 17 grupos), 1ºBach (linaje
 * {@code problema-5-escala-1bach}, 4 grupos), 2ºBach (nuevo en este bloque,
 * 3 grupos), 1ºFPB y 2ºFPB (linajes {@code problema-5-escala-1fpb/2fpb}, 1 grupo
 * cada uno). 26 grupos en total: 23 ordinarios + 3 PDC.
 *
 * <p>OBJETIVO DEL BLOQUE: mover los criterios 1-2 de Fase 5, que están definidos
 * sobre el instituto completo y que NINGÚN bloque previo ejercitaba (todos eran
 * subconjuntos). Mide el siguiente punto de la curva de coste no lineal vigilada
 * desde S31 (la ESO completa, 17 grupos, salió FACTIBLE en 2,1 s): aquí pasamos a
 * 26 grupos cruzando por primera vez los catálogos Bach↔ESO↔FPB.
 *
 * <p>2ºBach (plegado dentro de la fusión, no como bloque aislado previo, según el
 * aprendizaje de S31 sobre no trocear la fusión) aporta la complejidad estructural
 * nueva: optatividad transversal ABC de 4h repartida en dos bloques NEUTRA con el
 * subgrupo de DT compartido entre ambos (I6, mismo patrón que DTec en 1ºBach), y
 * modalidades transversales sobre el par B+C (Geografía, Economía, Mat.CCSS)
 * entrelazadas con bloques internos de cada grupo (BIOL/Física propias de B;
 * HART/Latín/Griego propias de C). Las plazas de modalidad que en el horario real
 * rotaban de aula usan {@code aulasCandidatas} (mecanismo previsto por el mapper en
 * {@code verificarAulasFijasDisjuntas}: dos plazas simultáneas con candidatas
 * solapadas las resuelve el solver eligiendo aulas distintas; con {@code aulaFija}
 * compartida el mapper habría rechazado el fixture por S2).
 *
 * <p>D4 (Gim/Pista) a escala total: 2ºBach NO añade presión (no tiene Educación
 * Física en su currículo). La demanda de Gim/Pista es la heredada de la ESO
 * completa (Gim 26/30, medida en S31) más la EF de 1ºBach (aulaFija, no
 * candidatas). La fusión no relaja el modelado conservador de aula fija de la EF:
 * si la rigidez forzara INFEASIBLE, sería evidencia limpia de que D4 muerde a
 * escala total, y abriría el segundo turno (relajar EF a {@code aulasCandidatas
 * [Gim,Pista]}), igual criterio que S30/S31.
 *
 * <p>UNIFICACIÓN DE CATÁLOGOS (trabajo previo, cruzado POR CÓDIGO, no a ojo):
 * <ul>
 *   <li><b>Profesores compartidos entre niveles</b>, fundidos asumiendo
 *       código = persona (clave natural del proyecto): p. ej. CLA1 y FIL2 dan
 *       clase en 1ºBach y 2ºBach; FIS3 da Física en ESO/Bach y el módulo CA de
 *       2ºFPB; GH1 y FOL3 cruzan ESO/Bach con FPB. Carga máxima tras fusión
 *       23 tramos/sem (FIS4); ninguno satura los 30. 59 profesores en total
 *       (50 de ESO + 4 nuevos de 2ºBach + PAU2/TEC1 de 1ºFPB + PAU1 de 2ºFPB,
 *       descontando los compartidos por código).</li>
 *   <li><b>Aulas unificadas</b> (35): 26 de ESO + las nuevas de 2ºBach (A4, A13,
 *       B02, B06, COM1, COM4 — las cuatro últimas ya venían de 1ºBach) +
 *       TALL3 (aula teórica CS) + los dos talleres técnicos FPB separados. Los
 *       fixtures aislados de 1ºFPB y 2ºFPB colapsaban su taller práctico al código
 *       inventado TALL_FPB (Hallazgo H: el PDF de aulas no detalla los talleres
 *       FPB). Aislados cuadran; fundidos, compartir TALL_FPB fuerza 49 tramos/30
 *       = INFEASIBLE. El centro confirmó que 1ºFPB (carrocería: AMO/MECSO/PS) y
 *       2ºFPB (electromecánica: MEC/ELE) usan talleres FÍSICOS DISTINTOS: se
 *       separan en TALL_FPB_1 (24 tramos) y TALL_FPB_2 (25 tramos).</li>
 *   <li><b>Tramos unificados</b> a los 30 canónicos D1-1..D5-6: los linajes FPB
 *       declaraban L1..V6 pero ninguna actividad referencia tramos por código
 *       (todas NEUTRA/DISTRIBUIDA sin posición fija), así que la unificación es
 *       inocua.</li>
 *   <li><b>Cero colisiones</b> de código de grupo, subgrupo y actividad entre los
 *       cinco linajes (prefijo 2BA/2BB/2BC para 2ºBach, limpio frente al 2A/2B/2C
 *       de la ESO). Los códigos de PLAZA repetidos (B2-PEPA/B2-CyR/B2-Fr2 de 2º
 *       ESO; HMC de 1ºBach) no son clave de identidad: el mapper no los deduplica.</li>
 * </ul>
 *
 * <p>CUADRE POR GRUPO heredado sin reconciliar: 25 grupos a 30 h; <b>3PDC a 22 h</b>
 * (opción 2 de S23, sus 8 h compartidas se imputan al ordinario). Los PDC de 4º a
 * 30. 2ºBach cuadra a 30 por construcción (réplica de las 30 posiciones reales del
 * PDF, contando cada bloque de optatividad/modalidad como un único slot del grupo).
 *
 * <p>Medición wall-clock en modo factibilidad pura (sin objetivo): tiempo hasta
 * PRIMERA solución factible. Semilla fija (42). El {@code @Timeout} actúa como red.
 */
class SolverHorarioFusionInstitutoCompletoTest {

    private static final String FIXTURE = "/fixtures/problema-5-fusion-instituto-completo.json";

    /** Coherente con el linaje de factibilidad: misma red de 600 s que los demás escala. */
    private static final double MAX_SEGUNDOS = 600.0;

    @Test
    @DisplayName(
            "Fusión instituto completo 26 grupos (ESO+Bach+FPB, criterios 1-2 de Fase 5): factible, 0 duras, tiempo medido")
    @Timeout(660)
    void fusionInstitutoCompleto() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset fundido (cuadra con la unificación verificada
        // por código): 26 grupos = 17 de ESO completa + 4 de 1ºBach + 3 de 2ºBach
        // + 1 de 1ºFPB + 1 de 2ºFPB; 30 tramos; 341 subgrupos = 232 + 65 + 42 + 1
        // + 1; 229 actividades = 155 + 30 + 23 + 11 + 10.
        assertThat(problema.grupos())
                .as("26 grupos: 17 ESO + 4 1ºBach + 3 2ºBach + 1 1ºFPB + 1 2ºFPB")
                .hasSize(26);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("341 subgrupos: 232 + 65 + 42 + 1 + 1")
                .hasSize(341);
        assertThat(problema.actividades())
                .as("229 actividades: 155 + 30 + 23 + 11 + 10")
                .hasSize(229);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Factibilidad: si el solver agotara el límite sin solución, resolver()
        // lanzaría HorarioInfactibleException y el test fallaría antes de aquí.
        // Un INFEASIBLE en esta fusión sería la primera evidencia de que el
        // instituto completo no cabe con el modelado conservador (D4 a escala
        // total, o acoplamiento de recursos Bach↔ESO↔FPB). Ese resultado abre el
        // segundo turno con atribución limpia.
        System.out.printf(
                "[FUSION-INSTITUTO] 26 grupos (ESO+Bach+FPB): solución factible en %.3f s (límite %.0f s)%n",
                segundos, MAX_SEGUNDOS);
        assertThat(segundos)
                .as("tiempo hasta primera solución factible < 600 s")
                .isLessThan(MAX_SEGUNDOS);

        // Cero restricciones duras violadas (red de seguridad independiente del
        // solver). Incluye S2 (aula sin doble uso) y D13 (bloques FPB de 2-3
        // tramos sin cruzar recreo ni desbordar el día).
        var verificacion = new VerificadorSolucion().verificar(problema, solucion);
        assertThat(verificacion.esValida())
                .as("violaciones duras: %s", verificacion.violaciones())
                .isTrue();
    }

    private static ProblemaHorario cargar() throws Exception {
        try (InputStream in =
                SolverHorarioFusionInstitutoCompletoTest.class.getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture en classpath: %s", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }
}
