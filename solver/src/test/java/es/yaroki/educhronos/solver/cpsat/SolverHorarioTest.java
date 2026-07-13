package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests del Bloque 4. El fixture {@code problema-solver-minimo.json} está
 * diseñado para activar las cuatro restricciones duras de forma independiente:
 * {@code MAT8} y {@code A5} compartidos entre dos subgrupos distintos,
 * {@code LEN2} compartido entre la co-docencia LCL-1A y Ref-1B, y actividades
 * {@code DISTRIBUIDA} con varias repeticiones sobre tres días.
 */
class SolverHorarioTest {

    private static final String FIXTURE = "/fixtures/problema-solver-minimo.json";

    @Test
    void resuelveElFixtureMinimoSinViolaciones() throws Exception {
        ProblemaHorario problema = cargarFixture();

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        ResultadoVerificacion resultado =
                new VerificadorSolucion().verificar(problema, solucion);

        assertThat(resultado.esValida())
                .as("violaciones detectadas: %s", resultado.violaciones())
                .isTrue();
    }

    @Test
    void todasLasInstanciasQuedanColocadas() throws Exception {
        ProblemaHorario problema = cargarFixture();

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        // 3 + 3 + 2 + 2 = 10 instancias esperadas.
        List<ActividadInstancia> esperadas = Expansion.todas(problema);
        assertThat(esperadas).hasSize(10);
        for (ActividadInstancia inst : esperadas) {
            assertThat(solucion.tramoDeInstancia(inst))
                    .as("instancia %s#%d", inst.actividad().codigo(), inst.indice())
                    .isPresent();
        }
    }

    @Test
    void laCoDocenciaOcupaAAmbosProfesores() throws Exception {
        // LEN2 co-imparte LCL-1A y, además, imparte Ref-1B. Si el no-solape de
        // profesor no contara a LEN2 dentro de la plaza de co-docencia, LCL-1A
        // y Ref-1B podrían caer en el mismo tramo. Verificamos que no ocurre.
        ProblemaHorario problema = cargarFixture();

        SolucionHorario solucion = new SolverHorario().resolver(problema);

        var tramosLcl = tramosDe(problema, solucion, "LCL-1A");
        var tramosRef = tramosDe(problema, solucion, "Ref-1B");

        assertThat(tramosLcl)
                .as("LCL-1A (co-docencia LEN2+LEN8) y Ref-1B (LEN2) no comparten tramo")
                .doesNotContainAnyElementsOf(tramosRef);
    }

    @Test
    void elVerificadorDetectaUnSolapeDeProfesor() throws Exception {
        // Garantiza que el verificador no pasa de forma vacía: ante una
        // solución corrupta a mano, debe reportar la violación.
        ProblemaHorario problema = cargarFixture();
        SolucionHorario valida = new SolverHorario().resolver(problema);

        ActividadInstancia matA = buscar(problema, "Mat-1A", 1);
        ActividadInstancia matB = buscar(problema, "Mat-1B", 1);

        // Mat-1A y Mat-1B comparten el profesor MAT8: forzarlas al mismo tramo
        // es un solape de profesor.
        Map<ActividadInstancia, Tramo> corrupta = new LinkedHashMap<>(valida.asignaciones());
        corrupta.put(matB, valida.tramoDeInstancia(matA).orElseThrow());

        ResultadoVerificacion resultado = new VerificadorSolucion()
                .verificar(problema, new SolucionHorario(corrupta));

        assertThat(resultado.esValida()).isFalse();
        assertThat(resultado.violaciones())
                .anyMatch(v -> v.regla() == ReglaDura.SOLAPE_PROFESOR
                        && "MAT8".equals(v.recursoCodigo())
                        && v.celdas().size() == 2);
    }

    // --------------------------------------------------------------- helpers

    private ProblemaHorario cargarFixture() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(FIXTURE)) {
            assertThat(in).as("fixture %s en el classpath de test", FIXTURE).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private ActividadInstancia buscar(ProblemaHorario problema, String codigoActividad, int indice) {
        return Expansion.todas(problema).stream()
                .filter(i -> i.actividad().codigo().equals(codigoActividad) && i.indice() == indice)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No existe la instancia " + codigoActividad + "#" + indice));
    }

    private java.util.Set<Tramo> tramosDe(ProblemaHorario problema,
                                          SolucionHorario solucion,
                                          String codigoActividad) {
        java.util.Set<Tramo> out = new java.util.HashSet<>();
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (inst.actividad().codigo().equals(codigoActividad)) {
                solucion.tramoDeInstancia(inst).ifPresent(out::add);
            }
        }
        return out;
    }
}
