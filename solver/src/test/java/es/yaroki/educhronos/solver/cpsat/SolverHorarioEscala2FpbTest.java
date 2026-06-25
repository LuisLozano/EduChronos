package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 5, Sub-bloque C de FPB: escala 2ºFPB real, linaje AISLADO. Cierra el
 * nivel FPB completo (junto a 1ºFPB del Sub-bloque B).
 *
 * Vuelve a ejercitar D13 a ESCALA, con MAS carga que 1ºFPB: 3 bloques de 3
 * tramos (MEC LUN T4-T6, MEC VIE T4-T6, ELE MIE T4-T6) + 5 bloques de 2 tramos
 * + 9 sesiones sueltas, sobre 30 sesiones de un grupo. La lista blanca de
 * inicios (ModeloCpSat.iniciosValidosDeBloque) discrimina inicios validos de
 * los que desbordan el dia o cruzan el recreo.
 *
 * Generado programaticamente desde el volcado fiel grupo-2-FPB.json (estructura)
 * y aula-Taller-3.json (cruce CyS<->TALL3). Decisiones de modelado:
 *  - Aula tecnica como TALL_FPB nominal (Opcion 1, heredada del Sub-bloque B):
 *    el listado por aula no contiene el taller tecnico de FPB (Hallazgo H
 *    confirmado por codigo: 0 sesiones tecnicas de 2FPB en TALL3, 25 celdas con
 *    aula=null en el volcado). CyS si usa TALL3 (aula real listada; las 5 celdas
 *    de CyS del volcado de grupo coinciden EXACTO con las 5 de 2FPB en TALL3).
 *  - Troceo bloque/suelta por regla determinista (validada): secuencia contigua
 *    maximal de misma asignatura+profesor dentro del dia, sin cruzar el recreo,
 *    = un bloque de esa longitud; apariciones aisladas = sueltas. El volcado da
 *    posiciones ocupadas, no etiqueta el troceo (a diferencia de 1FPB): la regla
 *    es la fuente del troceo, trazable y determinista (riesgo Hallazgo K asumido
 *    y validado con el usuario).
 *  - MEC (11h) troceada [blk-3 x2 + blk-2 x1 + suelta x3]; ELE (7h) [blk-3 x1 +
 *    blk-2 x2]; CA [blk-2 x2]; CyS [blk-2 x2 + suelta x1]; PI [suelta x2];
 *    Tut [suelta x1]. Tutor del grupo: PAU1 (no PAU2 como en 1FPB).
 *  - Pares que cruzan el recreo van como dos sueltas, no como bloque
 *    (D9/Hallazgo I): la regla de troceo corta en la frontera del recreo.
 *  - Todo NEUTRA: factibilidad pura, sin termino blando de distribucion que
 *    pudiera enmascarar un INFEASIBLE de D13. Neutraliza ademas D12 (palomar:
 *    MEC=11 y ELE=7 superan los 5 dias; la distribucion por dia no se activa).
 *
 * NO cierra criterios de Fase 5 (2ºFPB aislado no es el instituto completo;
 * falta 2ºBach y la fusion con ESO). Si cierra el nivel FPB.
 */
class SolverHorarioEscala2FpbTest {

    @Test
    void resuelveFactibleSinViolaciones() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = getClass().getResourceAsStream(
                "/fixtures/problema-5-escala-2fpb.json")) {
            assertNotNull(in, "No se encuentra el fixture problema-5-escala-2fpb.json");
            problema = new ProblemaHorarioJsonLoader().cargar(in);
        }

        // Sanity check del dataset (1 grupo / 30 tramos / 1 subgrupo / 10 actividades)
        assertEquals(1,  problema.grupos().size(),      "grupos");
        assertEquals(30, problema.tramos().size(),      "tramos");
        assertEquals(1,  problema.subgrupos().size(),   "subgrupos");
        assertEquals(10, problema.actividades().size(), "actividades");

        // Factibilidad pura (mismo regimen que la curva de escala): red 600 s, semilla 42
        SolverHorario solver = new SolverHorario(600.0, 42);
        SolucionHorario solucion = solver.resolver(problema);
        assertNotNull(solucion, "El solver debe encontrar solucion factible");

        // Verificador independiente: 0 violaciones de restricciones duras
        // (incluye el espejo de D13: verificarBloquesConsecutivos + no-solape
        // por tramo ocupado, sobre los tres bloques-3 y los cinco bloques-2)
        var resultado = new VerificadorSolucion().verificar(problema, solucion);
        assertTrue(resultado.esValida(),
                "Violaciones duras: " + resultado.violaciones());
    }
}
