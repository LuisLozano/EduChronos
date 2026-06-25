package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 5, Bloque 11: escala 1ºBach completo (4 grupos ordinarios A/B/C/D),
 * linaje AISLADO (no entra en la curva de escala-instituto hasta la fusión).
 *
 * Ejercita por primera vez a escala real:
 *  - 2 bloques de optativas transversales Tipo 7 sobre los 4 grupos (OPT1/OPT2),
 *    con la optativa DTec (4h) compartida entre ambos bloques (subgrupo único, I6).
 *  - 3 bloques de modalidad sobre subconjuntos del nivel: ciencias {A,B} (Bio/TecIn),
 *    humanidades {C,D} (Latín/MCCSS; HMC con profesor distinto por grupo; LU;
 *    ECO con dos plazas paralelas; GRI sólo en la oferta de D).
 *  - Religión/PTVE intra-grupo, un tramo por grupo (tutoría implícita en PTVE, S8/Hallazgo E).
 *  - Aula variable como aulasCandidatas (EF de C en Gim/Pista; LU; ECO-a; GRI).
 *
 * Modelado: subgrupos mono-grupo listados en la plaza (estilo linaje instituto,
 * frontera Fase 2→3 corregida en S14), NO subgrupos Lectura B N:M. Las dos
 * representaciones son válidas; se calca la del instituto para fusión limpia.
 *
 * NO cierra criterios de Fase 5 (1ºBach aislado no es el instituto completo;
 * faltan 2ºBach y FPB, y la fusión con ESO).
 */
class SolverHorarioEscala1BachTest {

    @Test
    void resuelveFactibleSinViolaciones() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = getClass().getResourceAsStream(
                "/fixtures/problema-5-escala-1bach.json")) {
            assertNotNull(in, "No se encuentra el fixture problema-5-escala-1bach.json");
            problema = new ProblemaHorarioJsonLoader().cargar(in);
        }

        // Sanity check del dataset (4 grupos / 30 tramos / 65 subgrupos / 30 actividades)
        assertEquals(4,  problema.grupos().size(),      "grupos");
        assertEquals(30, problema.tramos().size(),      "tramos");
        assertEquals(65, problema.subgrupos().size(),   "subgrupos");
        assertEquals(30, problema.actividades().size(), "actividades");

        // Factibilidad pura (mismo régimen que la curva de escala): red 600 s, semilla 42
        SolverHorario solver = new SolverHorario(600.0, 42);
        SolucionHorario solucion = solver.resolver(problema);
        assertNotNull(solucion, "El solver debe encontrar solución factible");

        // Verificador independiente: 0 violaciones de restricciones duras
        var resultado = new VerificadorSolucion().verificar(problema, solucion);
        assertTrue(resultado.esValida(),
                "Violaciones duras: " + resultado.violaciones());
    }
}
