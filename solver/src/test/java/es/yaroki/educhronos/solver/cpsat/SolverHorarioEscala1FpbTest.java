package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 5, Bloque 12 (Sub-bloque B de FPB): escala 1ºFPB real, linaje AISLADO.
 *
 * Primera prueba a ESCALA de D13 (cerrada en src/main en S33, Sub-bloque A):
 * bloques de 2 y 3 tramos consecutivos conviviendo con sesiones sueltas sobre
 * un horario real completo de 30 sesiones, donde la lista blanca de inicios
 * (ModeloCpSat.iniciosValidosDeBloque) discrimina inicios válidos de los que
 * desbordan el día o cruzan el recreo.
 *
 * Generado programáticamente desde el volcado fiel grupo-1-FPB.json (estructura)
 * y aula-Taller-3.json (cruce CS↔TALL3). Decisiones de modelado:
 *  - Aula técnica como TALL_FPB nominal (Opción 1): el listado por aula no
 *    contiene el taller técnico de FPB (Hallazgo H confirmado por código: 0
 *    sesiones técnicas de 1FPB en TALL3). CS sí usa TALL3 (aula real listada).
 *  - MECSO modelado DESDE LOS DATOS (blk-2 ×1 MAR T5-T6 + blk-3 ×1 JUE T4-T6),
 *    no según la reconstrucción en papel de §6.6 (blk-2 ×2 + suelta). Mismo
 *    total (5 tramos), estructura distinta: añade un segundo bloque-3 (junto a
 *    PS), reforzando la prueba de D13. Jerarquía PDF → volcado → modelo.
 *  - CS cruzando el recreo (LUN/MIÉ T3+T4) va como dos sueltas, no como bloque
 *    (D9/Hallazgo I). Bloque-2 de CS sólo VIE T1-T2 (no cruza recreo).
 *  - Todo NEUTRA: factibilidad pura, sin término blando de distribución que
 *    pudiera enmascarar un INFEASIBLE de D13.
 *
 * NO cierra criterios de Fase 5 (1ºFPB aislado no es el instituto completo;
 * faltan 2ºFPB, 2ºBach y la fusión con ESO).
 */
class SolverHorarioEscala1FpbTest {

    @Test
    void resuelveFactibleSinViolaciones() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = getClass().getResourceAsStream(
                "/fixtures/problema-5-escala-1fpb.json")) {
            assertNotNull(in, "No se encuentra el fixture problema-5-escala-1fpb.json");
            problema = new ProblemaHorarioJsonLoader().cargar(in);
        }

        // Sanity check del dataset (1 grupo / 30 tramos / 1 subgrupo / 11 actividades)
        assertEquals(1,  problema.grupos().size(),      "grupos");
        assertEquals(30, problema.tramos().size(),      "tramos");
        assertEquals(1,  problema.subgrupos().size(),   "subgrupos");
        assertEquals(11, problema.actividades().size(), "actividades");

        // Factibilidad pura (mismo régimen que la curva de escala): red 600 s, semilla 42
        SolverHorario solver = new SolverHorario(600.0, 42);
        SolucionHorario solucion = solver.resolver(problema);
        assertNotNull(solucion, "El solver debe encontrar solución factible");

        // Verificador independiente: 0 violaciones de restricciones duras
        // (incluye el espejo de D13: verificarBloquesConsecutivos + no-solape
        // por tramo ocupado, sobre los dos bloques-3 y los cinco bloques-2)
        var resultado = new VerificadorSolucion().verificar(problema, solucion);
        assertTrue(resultado.esValida(),
                "Violaciones duras: " + resultado.violaciones());
    }
}
