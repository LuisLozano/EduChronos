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
 * grupos ordinarios reales del instituto: 4 de 1º + 3 de 2º + 3 de 3º) más el
 * subgrupo de Diversificación (PDC) de 3º, dataset construido desde el volcado
 * fiel y verificado plaza a plaza contra él.
 *
 * <p>Objetivo de este test: ATACAR EL CRITERIO 1 DE FASE 5 (el solver termina en
 * menos de 10 minutos para el instituto completo) midiendo el tiempo sobre un
 * subconjunto creciente, y el CRITERIO 2 (cero restricciones duras violadas).
 * No es un test de discriminación: no aísla una restricción, acumula volumen.
 *
 * <p>Punto de la curva de escala que mide este test: 10 grupos + 1 subgrupo Di
 * (22 sesiones nuevas en A8). Los puntos anteriores (7 grupos → 0,317 s,
 * Sesión 20; 10 grupos sin Di → 0,469 s nominal, Sesión 21) quedan registrados
 * en el plan; el fixture de escala es único y crece, de modo que la curva se
 * traza en el registro del plan, no reejecutando datasets históricos
 * (decisión S20/S21).
 *
 * <p>ESCALÓN AÑADIDO EN ESTE BLOQUE (PDC de 3º a escala, Sesión 23): primer
 * grupo de Diversificación a escala. Estructura introducida respecto al escalón
 * anterior (3º ordinario):
 * <ul>
 *   <li>Un grupo administrativo propio {@code 3PDC} de tipo
 *       {@code DIVERSIFICACION_PDC} con {@code grupoPadre = 3C}: el dominio
 *       exige padre (invariante I5) y el dato fiel lo da, porque el PDC está
 *       adscrito administrativamente a 3C (su envoltura ordinaria —tutoría,
 *       Religión, EF— coincide con la de 3C; deuda 3ºPDC↔3ºCDi del plan). La
 *       población procede de 3A+3B+3C, pero eso es información de población (no
 *       modelada), distinta de la adscripción administrativa. El grupoPadre no
 *       entra en ninguna restricción (restriccionNoSolapeGrupo es ciega a él),
 *       así que 3C como padre no acopla el Di con 3C en el scheduling. El PDC
 *       NO es el
 *       patrón Lectura B de las optativas de Bach: allí el subgrupo lista varios
 *       grupos porque los bloquea enteros (todos los alumnos se redistribuyen);
 *       aquí el Di saca solo una parte de cada grupo y el resto sigue en clase
 *       ordinaria simultánea, así que el Di es un grupo independiente que solo
 *       se bloquea a sí mismo (restriccionNoSolapeGrupo es ciega al grupoPadre,
 *       ver Javadoc de ModeloCpSat). Su subgrupo {@code 3PDC} lista
 *       {@code grupos = {3PDC}} únicamente. El volcado fiel confirma tronco A8
 *       idéntico para los tres PDC (A/B/C) y sin solapamiento interno (una
 *       asignatura por tramo): es un único grupo Di, no tres.</li>
 *   <li>Seis actividades de tronco propio, todas mono-plaza con aula fija A8 y
 *       patrón DISTRIBUIDA: ÁmbCM (8 h, MAT4), ÁmbSL (7 h, LEN2), OyD (2 h,
 *       FRA1), RefMt (2 h, MAT7), IngDi (2 h, ING2), TPMAR (1 h, ORI1) = 22 h.
 *       Carga A8 = 22/30; carga máxima de profesor (ordinario + Di) = 14/30.</li>
 *   <li>Las sesiones compartidas ordinario+Di (EF, EPVA, Tec, Rel, ATED, TUT3)
 *       se quedan modeladas con el subgrupo del grupo ordinario solo: el
 *       alumno de diversificación se reincorpora a su grupo de procedencia en
 *       esos tramos y es, literalmente, alumno de ese grupo. El subgrupo Di NO
 *       se añade como participante explícito en esas plazas (decisión S23,
 *       opción 2): evita el doble conteo de población (3PDC ⊂ 3A ∪ 3B ∪ 3C) que
 *       activaría D3 al introducir capacidades reales de aula. Consecuencia: el
 *       subgrupo Di tiene 22 de sus 30 sesiones modeladas como propias; las 8
 *       restantes se observan desde el grupo ordinario (problema de
 *       presentación, no de modelo).</li>
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
 *   <li>Población nominal del subgrupo Di: el volcado da las SESIONES del
 *       tronco, no qué alumnos concretos de 3A/3B/3C lo cursan. El dominio
 *       modela el subgrupo, no la partición de alumnos.</li>
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
    @DisplayName(
            "Escala 1º+2º+3º ESO + PDC 3º (10 grupos + 1 subgrupo Di): factible, 0 violaciones duras, tiempo medido")
    @Timeout(660)
    void escala1y2y3ESOconPDC() throws Exception {
        ProblemaHorario problema = cargar();

        // Sanity check del dataset cargado (cuadra con el fixture verificado:
        // 4 grupos de 1º + 3 de 2º + 3 de 3º = 10 ordinarios + el grupo Di 3PDC
        // (DIVERSIFICACION_PDC) = 11 grupos).
        assertThat(problema.grupos())
                .as("11 grupos: 10 ordinarios (4 de 1º + 3 de 2º + 3 de 3º) + grupo Di 3PDC")
                .hasSize(11);
        assertThat(problema.tramos()).as("30 tramos").hasSize(30);
        assertThat(problema.subgrupos())
                .as("116 subgrupos: 115 del escalón anterior + subgrupo 3PDC")
                .hasSize(116);
        assertThat(problema.actividades())
                .as("116 actividades: 110 del escalón anterior + 6 de tronco Di")
                .hasSize(116);

        SolverHorario solver = new SolverHorario(MAX_SEGUNDOS, 42);

        long t0 = System.nanoTime();
        SolucionHorario solucion = solver.resolver(problema);
        long t1 = System.nanoTime();

        double segundos = (t1 - t0) / 1_000_000_000.0;

        // Criterio 1 de Fase 5: < 10 minutos (el solver internamente ya está
        // capado a 600 s; si hubiera agotado el límite sin factible, resolver()
        // habría lanzado HorarioInfactibleException y el test fallaría antes).
        System.out.printf(
                "[ESCALA] 1º+2º+3º ESO + PDC 3º (10 grupos + 1 subgrupo Di): solución factible en %.3f s (límite %.0f s)%n",
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
