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
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Indisponibilidad horaria del profesorado en el VERIFICADOR, por tramo OCUPADO (S165,
 * a partir de los hallazgos T2 y T4 de su M2):
 * <ul>
 *   <li>DURA → {@link ReglaDura#INDISPONIBILIDAD_PROFESOR}, una violación por
 *       (instancia, profesor, tramo ocupado vetado).</li>
 *   <li>BLANDA → {@link VerificadorSolucion#contarPenalizacionIndisponibilidadBlanda} y
 *       el término {@code INDISPONIBILIDAD_BLANDA} de
 *       {@link VerificadorSolucion#atribuirBlandas} cuentan cada tramo ocupado.</li>
 * </ul>
 *
 * <p>Soluciones fabricadas a mano, sin solver (patrón de
 * {@link VerificadorSolucionAtribucionBlandaTest}), para que los tramos ocupados sean
 * deterministas y cada valor esperado se derive a mano en el comentario del test. Una
 * sola actividad por problema: no hay solapes, ventanas ni consecutivas que puedan
 * aportar violaciones o penalizaciones ajenas a la regla aseverada.
 *
 * <p>Jornada sintética: un día con tres tramos lectivos L1, L2, L3 ({@code ordenEnDia}
 * 1..3, antes del recreo), de modo que un bloque de 2 que arranca en L1 ocupa {L1, L2},
 * y uno que arranca en L3 desborda el día (bloque imposible, D13).
 */
class VerificadorSolucionIndisponibilidadTest {

    private static final Asignatura ASIG = new Asignatura("ASG", "Asignatura cualquiera");
    private static final GrupoAdministrativo GRUPO =
            new GrupoAdministrativo("G", TipoGrupo.ORDINARIO, Optional.empty());
    private static final Subgrupo SG = new Subgrupo("SG", Set.of(GRUPO));
    private static final Aula AULA = new Aula("AU", "Aula");
    private static final Profesor MAT8 = new Profesor("MAT8", "Mates");
    private static final Profesor LEN1 = new Profesor("LEN1", "Lengua");
    private static final Tramo L1 = new Tramo("L1", 1, 1);
    private static final Tramo L2 = new Tramo("L2", 1, 2);
    private static final Tramo L3 = new Tramo("L3", 1, 3);

    private final VerificadorSolucion verificador = new VerificadorSolucion();

    /** Actividad mono-plaza y mono-instancia de {@code duracion} tramos. */
    private static Actividad actividad(String cod, int duracion, Profesor... profesores) {
        Plaza plaza = new Plaza(cod + "-P1", ASIG, Set.of(profesores),
                Optional.of(AULA), Set.of(), Set.of(SG));
        return new Actividad(cod, Optional.of(ASIG), 1, duracion, PatronTemporal.NEUTRA,
                List.of(plaza), false);
    }

    private static RestriccionHoraria dura(Profesor p, Tramo t) {
        return new RestriccionHoraria(p, t, TipoRestriccion.DURA, 1, Optional.empty());
    }

    private static RestriccionHoraria blanda(Profesor p, Tramo t) {
        return new RestriccionHoraria(p, t, TipoRestriccion.BLANDA, 1, Optional.empty());
    }

    private static ProblemaHorario problema(Actividad actividad,
                                            List<RestriccionHoraria> restricciones) {
        return new ProblemaHorario(
                List.of(L1, L2, L3),
                List.of(AULA), List.of(ASIG), List.of(LEN1, MAT8), List.of(GRUPO), List.of(SG),
                List.of(actividad),
                restricciones,
                List.of(),    // bloqueos
                List.of());   // tutorias
    }

    private static ActividadInstancia inst(ProblemaHorario problema) {
        return Expansion.todas(problema).get(0);
    }

    /** La solución con la única instancia del problema colocada en {@code inicio}. */
    private static SolucionHorario en(ProblemaHorario problema, Tramo inicio) {
        return new SolucionHorario(Map.of(inst(problema), inicio));
    }

    private List<Violacion> indisponibilidad(ProblemaHorario problema, SolucionHorario sol) {
        return verificador.verificar(problema, sol).violaciones().stream()
                .filter(v -> v.regla() == ReglaDura.INDISPONIBILIDAD_PROFESOR)
                .toList();
    }

    private static List<Penalizacion> blandasDe(AtribucionBlanda atr, CeldaRef celda) {
        return atr.porCelda().getOrDefault(celda, List.of()).stream()
                .filter(p -> p.regla() == ReglaBlanda.INDISPONIBILIDAD_BLANDA)
                .toList();
    }

    private static final CeldaRef CELDA_A = new CeldaRef("A", 1, null);

    // ======================================================== DURA (1.1)

    @Test
    void dura_instanciaEnTramoVetado_unaViolacionConProfesorTramoYCelda() {
        // A (d=1, MAT8) en L1; DURA (MAT8, L1). Ocupa {L1}; vetados de MAT8 = {L1}.
        // Intersección {L1} → exactamente 1 violación, y es la ÚNICA de la solución
        // (una sola actividad: sin solapes, colocada, bloque de 1).
        ProblemaHorario p = problema(actividad("A", 1, MAT8), List.of(dura(MAT8, L1)));

        List<Violacion> todas = verificador.verificar(p, en(p, L1)).violaciones();

        assertThat(todas).singleElement().satisfies(v -> {
            assertThat(v.regla()).isEqualTo(ReglaDura.INDISPONIBILIDAD_PROFESOR);
            assertThat(v.recursoCodigo()).isEqualTo("MAT8");
            assertThat(v.tramoCodigo()).isEqualTo("L1");
            assertThat(v.celdas()).containsExactly(CELDA_A);
        });
    }

    @Test
    void dura_instanciaEnTramoLibre_ningunaViolacion() {
        // A (d=1, MAT8) en L1; la DURA es (MAT8, L2). Ocupa {L1} ∩ {L2} = ∅ → 0.
        // La solución entera es válida: el veto de otro tramo no contamina.
        ProblemaHorario p = problema(actividad("A", 1, MAT8), List.of(dura(MAT8, L2)));

        assertThat(verificador.verificar(p, en(p, L1)).violaciones()).isEmpty();
    }

    @Test
    void dura_coDocencia_soloElProfesorVetadoRecibeLaViolacion() {
        // A (d=1) con MAT8 y LEN1 en la MISMA plaza, en L1. Solo LEN1 tiene DURA en L1.
        // MAT8: vetados ∅ → 0. LEN1: {L1} ∩ {L1} → 1. Total 1, a nombre de LEN1.
        ProblemaHorario p = problema(actividad("A", 1, MAT8, LEN1), List.of(dura(LEN1, L1)));

        assertThat(indisponibilidad(p, en(p, L1))).singleElement().satisfies(v -> {
            assertThat(v.recursoCodigo()).isEqualTo("LEN1");
            assertThat(v.tramoCodigo()).isEqualTo("L1");
        });
    }

    /**
     * DECISIÓN DOCUMENTADA: dos filas DURA idénticas {@code (MAT8, L1)} cuentan UNA
     * violación. La violación es el hecho «MAT8 ocupa un tramo vetado», no «incumple la
     * fila N»: el verificador agrupa los vetos en un {@code Set} de tramos por profesor,
     * como el modelo ({@code restriccionIndisponibilidadProfesor}) y la sobrecarga de la
     * pre-validación (DURA por tramo distinto). Con una {@code List} saldrían 2.
     */
    @Test
    void dura_dosRestriccionesIgualesDelMismoProfesorYTramo_cuentanUna() {
        ProblemaHorario p = problema(actividad("A", 1, MAT8),
                List.of(dura(MAT8, L1), dura(MAT8, L1)));

        assertThat(indisponibilidad(p, en(p, L1))).hasSize(1);
    }

    @Test
    void dura_bloqueConTramoInteriorVetado_violacionEnElInterior() {
        // B (d=2, MAT8) arranca en L1 → ocupa {L1, L2}. DURA (MAT8, L2): el INTERIOR.
        // {L1, L2} ∩ {L2} = {L2} → 1 violación con tramoCodigo L2. Es el caso de S165,
        // M2, T2/T1: mirando solo el inicio (L1) saldrían 0.
        ProblemaHorario p = problema(actividad("B", 2, MAT8), List.of(dura(MAT8, L2)));

        assertThat(indisponibilidad(p, en(p, L1))).singleElement().satisfies(v -> {
            assertThat(v.recursoCodigo()).isEqualTo("MAT8");
            assertThat(v.tramoCodigo()).isEqualTo("L2");
            assertThat(v.celdas()).containsExactly(new CeldaRef("B", 1, null));
        });
    }

    @Test
    void dura_bloqueQueCubreDosTramosVetados_unaViolacionPorTramo() {
        // B (d=2, MAT8) en L1 → ocupa {L1, L2}; DURA en L1 y en L2.
        // {L1, L2} ∩ {L1, L2} → 2 violaciones, tramos L1 y L2 (una por tramo ocupado).
        ProblemaHorario p = problema(actividad("B", 2, MAT8),
                List.of(dura(MAT8, L1), dura(MAT8, L2)));

        assertThat(indisponibilidad(p, en(p, L1)))
                .extracting(Violacion::tramoCodigo)
                .containsExactlyInAnyOrder("L1", "L2");
    }

    @Test
    void dura_bloqueImposible_noDuplicaElAvisoDelBloque() {
        // B (d=2) arranca en L3: L4 no existe en el día → bloque imposible (D13), que ya
        // reporta BLOQUE_IMPOSIBLE. DURA (MAT8, L3) sobre el inicio: NO se añade
        // INDISPONIBILIDAD_PROFESOR. Total: 1 violación, la del bloque.
        ProblemaHorario p = problema(actividad("B", 2, MAT8), List.of(dura(MAT8, L3)));

        assertThat(verificador.verificar(p, en(p, L3)).violaciones())
                .extracting(Violacion::regla)
                .containsExactly(ReglaDura.BLOQUE_IMPOSIBLE);
    }

    @Test
    void dura_instanciaSinColocar_noDuplicaElAvisoDeSinColocar() {
        // Solución vacía: A sin tramo → solo INSTANCIA_SIN_COLOCAR, aunque MAT8 tenga
        // DURA en todos los tramos (no hay tramo ocupado que comparar).
        ProblemaHorario p = problema(actividad("A", 1, MAT8),
                List.of(dura(MAT8, L1), dura(MAT8, L2), dura(MAT8, L3)));

        assertThat(verificador.verificar(p, new SolucionHorario(Map.of())).violaciones())
                .extracting(Violacion::regla)
                .containsExactly(ReglaDura.INSTANCIA_SIN_COLOCAR);
    }

    @Test
    void dura_restriccionBlanda_noEsViolacionDura() {
        // A en L1 con BLANDA (MAT8, L1): la blanda penaliza, no invalida → 0 violaciones.
        ProblemaHorario p = problema(actividad("A", 1, MAT8), List.of(blanda(MAT8, L1)));

        assertThat(verificador.verificar(p, en(p, L1)).violaciones()).isEmpty();
    }

    // ======================================================== BLANDA (1.2)

    @Test
    void blanda_bloqueConTramoInteriorVetado_penalizaUno() {
        // B (d=2, MAT8) en L1 → ocupa {L1, L2}; BLANDA (MAT8, L2), el interior.
        // Recuento: 1 restricción × 1 instancia que ocupa L2 = 1 (en S165, M2, T4 daba 0).
        // Atribución: una Penalizacion en la celda B#1, tramo L2, delta 1.
        ProblemaHorario p = problema(actividad("B", 2, MAT8), List.of(blanda(MAT8, L2)));
        SolucionHorario sol = en(p, L1);

        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, sol)).isEqualTo(1);
        assertThat(blandasDe(verificador.atribuirBlandas(p, sol), new CeldaRef("B", 1, null)))
                .singleElement().satisfies(pen -> {
                    assertThat(pen.recursoCodigo()).isEqualTo("MAT8");
                    assertThat(pen.tramoCodigo()).isEqualTo("L2");
                    assertThat(pen.delta()).isEqualTo(1);
                });
    }

    @Test
    void blanda_bloqueQueCubreDosTramosVetados_penalizaDosYLaAtribucionSumaIgual() {
        // B (d=2) en L1 → {L1, L2}; BLANDA en L1 y en L2 → 1 + 1 = 2.
        // Atribución: dos Penalizacion (L1: 1, L2: 1); su suma, 2, iguala al recuento.
        ProblemaHorario p = problema(actividad("B", 2, MAT8),
                List.of(blanda(MAT8, L1), blanda(MAT8, L2)));
        SolucionHorario sol = en(p, L1);

        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, sol)).isEqualTo(2);
        List<Penalizacion> pens =
                blandasDe(verificador.atribuirBlandas(p, sol), new CeldaRef("B", 1, null));
        assertThat(pens).extracting(Penalizacion::tramoCodigo)
                .containsExactlyInAnyOrder("L1", "L2");
        assertThat(pens.stream().mapToInt(Penalizacion::delta).sum()).isEqualTo(2);
    }

    @Test
    void blanda_bloqueAdyacenteAlVeto_noPenaliza() {
        // B (d=2) en L1 → {L1, L2}; BLANDA en L3, justo después del bloque.
        // {L1, L2} no contiene L3 → 0, y la celda no lleva INDISPONIBILIDAD_BLANDA.
        ProblemaHorario p = problema(actividad("B", 2, MAT8), List.of(blanda(MAT8, L3)));
        SolucionHorario sol = en(p, L1);

        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, sol)).isZero();
        assertThat(blandasDe(verificador.atribuirBlandas(p, sol), new CeldaRef("B", 1, null)))
                .isEmpty();
    }

    @Test
    void blanda_duracionUno_mismoValorQueAntes() {
        // A (d=1) en L2 con BLANDA en L2 y en L3: ocupados = {L2} → solo la de L2 → 1.
        // Con duración 1 «ocupado» y «de inicio» coinciden: el valor previo al cambio.
        ProblemaHorario p = problema(actividad("A", 1, MAT8),
                List.of(blanda(MAT8, L2), blanda(MAT8, L3)));
        SolucionHorario sol = en(p, L2);

        assertThat(verificador.contarPenalizacionIndisponibilidadBlanda(p, sol)).isEqualTo(1);
        assertThat(blandasDe(verificador.atribuirBlandas(p, sol), CELDA_A))
                .singleElement().extracting(Penalizacion::tramoCodigo).isEqualTo("L2");
    }
}
