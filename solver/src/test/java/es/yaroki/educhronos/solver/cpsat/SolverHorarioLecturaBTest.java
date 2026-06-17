package es.yaroki.educhronos.solver.cpsat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.io.ProblemaHorarioJsonLoader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Discriminacion de Lectura B (subgrupo multi-grupo, Subgrupo->grupos N:M).
 *
 * <p>Dataset real: el bloque de optativas de 1ºBachillerato. En el centro de
 * referencia, C y D comparten cinco optativas simultaneas (DTec/ANAP/TEstI/
 * TICO/DA), cada una con un unico profesor y aula, en el mismo tramo (verificado
 * cruzando grupo-1BACH-C.json y grupo-1BACH-D.json: dia 1 t3 y dia 2 t2). Cada
 * optativa es un grupo-clase cuya poblacion son alumnos de C Y de D mezclados:
 * un subgrupo que pertenece a DOS grupos. Esto es Lectura B, distinta de la
 * Lectura A multi-grupo (dos grupos ENTEROS juntos, p.ej. LU C+D o Religion):
 * alli cada subgrupo es mono-grupo; aqui un subgrupo abarca dos grupos.
 *
 * <p>El fixture recorta el bloque a tres optativas (TICO/DTec/DA), con profesor
 * y aula distintos entre si, para que la unica razon de bloqueo de grupo sea el
 * subgrupo multi-grupo y no una colision de recurso.
 *
 * <p>DEUDA (invariante de poblacion): el volcado fiel da las SESIONES (existe
 * TICO simultanea para C y D), no el reparto nominal de alumnos entre optativas.
 * Que la particion concreta sea la real del centro no lo verifica este test ni
 * ningun componente; se confirma con el centro. Lo que el test prueba es la
 * MECANICA N:M: que un subgrupo de dos grupos bloquea ambos.
 *
 * <p>Discriminacion (lo que hace este test imposible bajo el dominio viejo 1:1):
 * en el fixture infactible, el bloque de optativas ocupa 1BachC <i>a traves del
 * subgrupo multi-grupo</i>, y LCL-1BachC compite por el unico tramo -> colision
 * de grupo C -> infactible. Si el subgrupo perteneciera solo a D (1:1), C estaria
 * libre y LCL-1BachC cabria: seria factible. La infactibilidad depende, pues, de
 * la pertenencia del subgrupo a C, expresable solo con N:M.
 */
class SolverHorarioLecturaBTest {

    private static final String FIXTURE_FACTIBLE =
            "/fixtures/problema-5-lecturab-optativas-bach.json";
    private static final String FIXTURE_INFACTIBLE =
            "/fixtures/problema-5-lecturab-optativas-bach-infactible.json";

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("Lectura B: el bloque de optativas C+D es factible y toca ambos grupos")
    void bloqueOptativasMultigrupoEsFactible() throws Exception {
        ProblemaHorario problema = cargar(FIXTURE_FACTIBLE);

        // Sanity: el fixture contiene al menos un subgrupo multi-grupo (Lectura B).
        long multigrupo = problema.subgrupos().stream()
                .filter(s -> s.grupos().size() > 1)
                .count();
        assertThat(multigrupo)
                .as("el fixture contiene subgrupos multi-grupo (Lectura B)")
                .isGreaterThanOrEqualTo(1);

        SolucionHorario solucion = new SolverHorario().resolver(problema);
        assertThat(solucion).as("solucion factible").isNotNull();

        assertThat(new VerificadorSolucion().verificar(problema, solucion).violaciones())
                .as("violaciones de restricciones duras")
                .isEmpty();

        // El bloque de optativas toca C y D a la vez (via los subgrupos multi-grupo).
        assertThat(gruposTocadosPorActividad(problema, "Optativas-1Bach-CD"))
                .as("el bloque de optativas cubre 1BachC y 1BachD")
                .contains("1BachC", "1BachD");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    @DisplayName("Lectura B: el subgrupo multi-grupo bloquea 1BachC (prueba negativa)")
    void optativaMultigrupoBloqueaAmbosGrupos_infactible() throws Exception {
        ProblemaHorario problema = cargar(FIXTURE_INFACTIBLE);

        // El bloque de optativas ocupa 1BachC via subgrupo multi-grupo; LCL-1BachC
        // compite por el unico tramo. Sin N:M, C estaria libre y seria factible.
        assertThatThrownBy(() -> new SolverHorario().resolver(problema))
                .isInstanceOf(HorarioInfactibleException.class);
    }

    // ---- helpers ----

    private ProblemaHorario cargar(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("fixture %s en classpath", path).isNotNull();
            return new ProblemaHorarioJsonLoader().cargar(in);
        }
    }

    private List<String> gruposTocadosPorActividad(ProblemaHorario problema, String codigoActividad) {
        List<String> grupos = new ArrayList<>();
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            if (!inst.actividad().codigo().equals(codigoActividad)) {
                continue;
            }
            for (Plaza plaza : inst.actividad().plazas()) {
                for (Subgrupo sg : plaza.subgrupos()) {
                    for (GrupoAdministrativo g : sg.grupos()) {
                        if (!grupos.contains(g.codigo())) {
                            grupos.add(g.codigo());
                        }
                    }
                }
            }
        }
        return grupos;
    }
}