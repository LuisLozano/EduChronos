package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica la ATRIBUCIÓN CONTRAFACTUAL de las reglas BLANDAS por celda
 * ({@link VerificadorSolucion#atribuirBlandas}, Fase 8, Bloque 8.3-B). Las
 * soluciones se fabrican a mano (sin solver), como en
 * {@link VerificadorSolucionAtribucionTest}, para que el conjunto de posiciones de
 * cada profesor sea DETERMINISTA y el delta esperado se pueda enumerar en diseño.
 *
 * <p>El oro del bloque es el DELTA CON SIGNO: la atribución no es culpabilidad sino
 * una derivada {@code penalización_actual − penalización_sin_esa_celda}. Una celda
 * puede tener delta negativo (tapa un hueco: moverla EMPEORA), y el atribuidor NO
 * lo normaliza a 0.
 */
class VerificadorSolucionAtribucionBlandaTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final PatronTemporal NEUTRA = PatronTemporal.NEUTRA;
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("SG", Set.of(GRUPO));
    private static final Aula AULA = new Aula("AU", "Aula");
    private static final Profesor MAT8 = new Profesor("MAT8", "Mates");

    private final VerificadorSolucion verificador = new VerificadorSolucion();

    /** Actividad mono-plaza, mono-instancia, para MAT8. */
    private static Actividad actividad(String cod) {
        Plaza plaza = new Plaza(
                cod + "-P1", ASIG, Set.of(MAT8),
                Optional.of(AULA), Set.of(), Set.of(SG));
        return new Actividad(cod, Optional.of(ASIG), 1, 1, NEUTRA, List.of(plaza), false);
    }

    private static ProblemaHorario problema(List<Tramo> tramos, List<Actividad> actividades) {
        return new ProblemaHorario(
                tramos,
                List.of(AULA), List.of(ASIG), List.of(MAT8), List.of(GRUPO), List.of(SG),
                actividades,
                List.of(),    // restriccionesHorarias
                List.of(),    // bloqueos
                List.of());   // tutorias
    }

    private static ActividadInstancia inst(ProblemaHorario problema, String codActividad) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codActividad))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no expandida: " + codActividad));
    }

    private static Penalizacion penalizacionDe(AtribucionBlanda atribucion,
                                               CeldaRef celda, ReglaBlanda regla) {
        List<Penalizacion> pens = atribucion.porCelda().get(celda);
        assertThat(pens).as("la celda %s debe tener penalizaciones", celda).isNotNull();
        return pens.stream()
                .filter(p -> p.regla() == regla)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "sin " + regla + " en " + celda + " (tiene: " + pens + ")"));
    }

    // ----------------------------------------------------------------------
    // 2) ORO — DELTA NEGATIVO. La celda del MEDIO tapa un hueco: moverla EMPEORA.
    // ----------------------------------------------------------------------
    @Test
    void ventanaProfesor_celdaDelMedioTapaHueco_deltaExactamenteMenosUno() {
        // MAT8 con 3 clases contiguas en el día 1: posiciones {1,2,3} = 0 ventanas.
        // Quitar la del medio (pos 2) deja {1,3} = 1 ventana. delta = 0 − 1 = −1.
        Tramo lun1 = new Tramo("LUN-1", 1, 1);
        Tramo lun2 = new Tramo("LUN-2", 1, 2);
        Tramo lun3 = new Tramo("LUN-3", 1, 3);
        Actividad a1 = actividad("ACT-1");
        Actividad a2 = actividad("ACT-2");
        Actividad a3 = actividad("ACT-3");
        ProblemaHorario problema =
                problema(List.of(lun1, lun2, lun3), List.of(a1, a2, a3));

        SolucionHorario sol = new SolucionHorario(Map.of(
                inst(problema, "ACT-1"), lun1,
                inst(problema, "ACT-2"), lun2,   // la del medio
                inst(problema, "ACT-3"), lun3));

        AtribucionBlanda atr = verificador.atribuirBlandas(problema, sol);

        CeldaRef celdaMedio = new CeldaRef("ACT-2", 1, null);
        Penalizacion p = penalizacionDe(atr, celdaMedio, ReglaBlanda.VENTANA_PROFESOR);
        assertThat(p.delta())
                .as("la celda del medio tapa un hueco: delta EXACTAMENTE −1 (un clamp lo rompería)")
                .isEqualTo(-1);
        assertThat(p.recursoCodigo()).isEqualTo("MAT8");
        assertThat(p.tramoCodigo())
                .as("VENTANA_PROFESOR penaliza el día, no un tramo: tramoCodigo null")
                .isNull();

        // Discriminación: solo la celda del medio aporta. Los extremos (pos 1 y 3)
        // dan delta 0 AQUÍ porque quitarlos reduce span y nClases en 1 cada uno y la
        // resta se cancela — NO porque los extremos nunca aporten: en {1,3,4} quitar
        // el extremo 1 da delta +1 (ver el test de {1,3}). El caso {1,2,3} es contiguo.
        assertThat(atr.porCelda())
                .as("solo la celda que tapa el hueco aporta; las demás no aparecen")
                .containsOnlyKeys(celdaMedio);
        assertThat(atr.deltaTotal(celdaMedio)).isEqualTo(-1);
    }

    // ----------------------------------------------------------------------
    // 3) ORO — DELTA POSITIVO. Mover una punta del hueco MEJORA.
    // ----------------------------------------------------------------------
    @Test
    void ventanaProfesor_celdaEnPuntaDeHueco_deltaExactamenteMasUno() {
        // MAT8 con clases en {1,3} del día 1: 1 ventana (hueco en pos 2). Quitar la
        // de pos 1 deja {3} = 0 ventanas. delta = 1 − 0 = +1: moverla MEJORA.
        Tramo lun1 = new Tramo("LUN-1", 1, 1);
        Tramo lun3 = new Tramo("LUN-3", 1, 3);
        Actividad a1 = actividad("ACT-1");
        Actividad a3 = actividad("ACT-3");
        ProblemaHorario problema = problema(List.of(lun1, lun3), List.of(a1, a3));

        SolucionHorario sol = new SolucionHorario(Map.of(
                inst(problema, "ACT-1"), lun1,
                inst(problema, "ACT-3"), lun3));

        AtribucionBlanda atr = verificador.atribuirBlandas(problema, sol);

        CeldaRef celda1 = new CeldaRef("ACT-1", 1, null);
        Penalizacion p = penalizacionDe(atr, celda1, ReglaBlanda.VENTANA_PROFESOR);
        assertThat(p.delta())
                .as("mover la punta del hueco elimina la ventana: delta EXACTAMENTE +1")
                .isEqualTo(1);
        // Discrimina que el atribuidor no devuelve siempre 0.
        assertThat(atr.deltaTotal(celda1)).isEqualTo(1);
    }

    // ----------------------------------------------------------------------
    // 4) CONSISTENCIA DEL GEMELO. Los gemelos, tras el refactor de extracción,
    //    devuelven VALORES CONOCIDOS (no comparados consigo mismos).
    // ----------------------------------------------------------------------
    @Test
    void gemelosTrasExtraccion_devuelvenValoresConocidos() {
        // MAT8: día 1 (LUN) en {1,5} → ventanas span(5) − 2 = 3, consecutivas 0.
        //       día 2 (MAR) en {1,2,3,4} → ventanas 0, consecutivas max(0,4−3) = 1.
        // Totales enumerados en diseño: ventanas = 3, exceso consecutivas = 1.
        Tramo lun1 = new Tramo("LUN-1", 1, 1);
        Tramo lun5 = new Tramo("LUN-5", 1, 5);
        Tramo mar1 = new Tramo("MAR-1", 2, 1);
        Tramo mar2 = new Tramo("MAR-2", 2, 2);
        Tramo mar3 = new Tramo("MAR-3", 2, 3);
        Tramo mar4 = new Tramo("MAR-4", 2, 4);
        List<Actividad> acts = List.of(
                actividad("L1"), actividad("L5"),
                actividad("M1"), actividad("M2"), actividad("M3"), actividad("M4"));
        ProblemaHorario problema =
                problema(List.of(lun1, lun5, mar1, mar2, mar3, mar4), acts);

        SolucionHorario sol = new SolucionHorario(Map.of(
                inst(problema, "L1"), lun1,
                inst(problema, "L5"), lun5,
                inst(problema, "M1"), mar1,
                inst(problema, "M2"), mar2,
                inst(problema, "M3"), mar3,
                inst(problema, "M4"), mar4));

        assertThat(verificador.contarVentanasProfesor(problema, sol).get(MAT8))
                .as("ventanas: {1,5} día1 (3) + {1,2,3,4} día2 (0) = 3")
                .isEqualTo(3);
        assertThat(verificador.contarPenalizacionConsecutivasProfesor(problema, sol))
                .as("exceso consecutivas: {1,5} día1 (0) + {1,2,3,4} día2 (1) = 1")
                .isEqualTo(1);
    }

    // ----------------------------------------------------------------------
    // 5) INDISPONIBILIDAD_BLANDA. Reusa el fixture de oro fuerte de 6c; la sesión
    //    que cae en el tramo vetado tiene delta 1 y tramoCodigo NO-NULL.
    // ----------------------------------------------------------------------
    @Test
    void indisponibilidadBlanda_sesionEnTramoVetado_deltaUnoConTramoNoNull() throws Exception {
        // Fixture 6c: P1, tramos L1(d1), L2(d2), L3(d3) — todos BLANDA-vetados L1 y L2.
        // Solución a mano determinista: ACT-A en L1 (vetado), ACT-B en L3 (limpio).
        ProblemaHorario problema =
                cargar("/fixtures/problema-6c-indisp-blanda-oro-fuerte.json");
        Tramo l1 = tramoDe(problema, "L1");
        Tramo l3 = tramoDe(problema, "L3");
        Profesor p1 = problema.profesores().stream()
                .filter(p -> p.codigo().equals("P1")).findFirst().orElseThrow();

        SolucionHorario sol = new SolucionHorario(Map.of(
                inst(problema, "ACT-A"), l1,   // cae en tramo vetado-blando
                inst(problema, "ACT-B"), l3)); // tramo limpio

        AtribucionBlanda atr = verificador.atribuirBlandas(problema, sol);

        CeldaRef celdaVetada = new CeldaRef("ACT-A", 1, null);
        Penalizacion p = penalizacionDe(atr, celdaVetada, ReglaBlanda.INDISPONIBILIDAD_BLANDA);
        assertThat(p.delta()).as("una preferencia blanda incumplida: delta 1").isEqualTo(1);
        assertThat(p.recursoCodigo()).isEqualTo(p1.codigo());
        assertThat(p.tramoCodigo())
                .as("INDISPONIBILIDAD_BLANDA es LOCAL a un tramo: tramoCodigo no-null (el vetado)")
                .isEqualTo("L1");

        // La sesión en tramo limpio NO aparece con esa regla (ni con ninguna: es la
        // única clase de su día → 0 ventanas, 0 consecutivas, 0 blandas).
        assertThat(atr.porCelda())
                .as("solo la sesión en tramo vetado aporta")
                .containsOnlyKeys(celdaVetada);
    }

    // ----------------------------------------------------------------------
    // 6) Una celda sin ninguna penalización NO APARECE en porCelda.
    // ----------------------------------------------------------------------
    @Test
    void celdaSinPenalizacion_noApareceEnElMapa() {
        // MAT8 con 2 clases contiguas en {1,2} del día 1: 0 ventanas, 0 consecutivas,
        // sin restricciones. Ninguna celda aporta → el mapa queda VACÍO.
        Tramo lun1 = new Tramo("LUN-1", 1, 1);
        Tramo lun2 = new Tramo("LUN-2", 1, 2);
        Actividad a1 = actividad("ACT-1");
        Actividad a2 = actividad("ACT-2");
        ProblemaHorario problema = problema(List.of(lun1, lun2), List.of(a1, a2));

        SolucionHorario sol = new SolucionHorario(Map.of(
                inst(problema, "ACT-1"), lun1,
                inst(problema, "ACT-2"), lun2));

        AtribucionBlanda atr = verificador.atribuirBlandas(problema, sol);

        assertThat(atr.porCelda())
                .as("sin ventanas ni consecutivas ni blandas: ninguna celda aparece")
                .isEmpty();
        assertThat(atr.deltaTotal(new CeldaRef("ACT-1", 1, null))).isZero();
    }

    private ProblemaHorario cargar(String ruta) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("fixture no encontrado: " + ruta);
            }
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private static Tramo tramoDe(ProblemaHorario problema, String codigo) {
        return problema.tramos().stream()
                .filter(t -> t.codigo().equals(codigo))
                .findFirst().orElseThrow();
    }
}
