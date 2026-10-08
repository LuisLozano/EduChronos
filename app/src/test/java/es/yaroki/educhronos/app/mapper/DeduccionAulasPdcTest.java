package es.yaroki.educhronos.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaAula;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.Nivel;
import es.yaroki.educhronos.app.catalog.PatronTemporal;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.RolAulaAsignatura;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.TipoActividad;
import es.yaroki.educhronos.app.catalog.TipoAula;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.service.AvisoPrevalidacion;
import es.yaroki.educhronos.app.service.DatosAulas;
import es.yaroki.educhronos.app.service.DatosCuadre;
import es.yaroki.educhronos.app.service.PrevalidacionService;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Clases solo de PDC (S209, T3): una plaza de CLASE cuyos subgrupos son TODOS de grupos PDC, de uno o
 * de varios, no recibe el aula de referencia del grupo padre, que a esa hora está ocupada por el
 * resto del grupo. Su dominio sale solo de las reglas de su asignatura. Una plaza con algún grupo
 * ordinario no cambia: el PDC sigue contando como su padre. Fixtures de {@link DeduccionAulasTest}.
 */
class DeduccionAulasPdcTest {

    private static final Nivel NIVEL = new Nivel("3ESO", 3);
    private static final Profesor PROF = new Profesor("P1", "Profesor");
    private long siguienteId = 1;

    private Aula aula(String codigo, Integer capacidad) {
        Aula a = new Aula(codigo, TipoAula.ORDINARIA, capacidad, null, null, null);
        ReflectionTestUtils.setField(a, "id", siguienteId++);
        return a;
    }

    private Aula aula(String codigo) {
        return aula(codigo, null);
    }

    private Asignatura asignatura(String codigo) {
        Asignatura a = new Asignatura(codigo, codigo);
        ReflectionTestUtils.setField(a, "id", siguienteId++);
        return a;
    }

    private static GrupoAdministrativo ordinario(String codigo, Aula aulaDeGrupo) {
        GrupoAdministrativo g = new GrupoAdministrativo(codigo, NIVEL, TipoGrupo.ORDINARIO, null);
        g.setAulaReferencia(aulaDeGrupo);
        return g;
    }

    private static GrupoAdministrativo pdc(String codigo, GrupoAdministrativo padre) {
        return new GrupoAdministrativo(codigo, NIVEL, TipoGrupo.DIVERSIFICACION_PDC, padre);
    }

    private static Subgrupo subgrupo(String codigo, Integer alumnos, GrupoAdministrativo... grupos) {
        Subgrupo s = new Subgrupo(codigo, Set.of(grupos));
        s.setAlumnos(alumnos);
        return s;
    }

    private static Actividad clase(Asignatura asig) {
        Actividad a = new Actividad("ACT", asig, 1, 1, PatronTemporal.NEUTRA, false);
        a.setTipo(TipoActividad.CLASE);
        return a;
    }

    private static Plaza plaza(Actividad act, Asignatura asig, Aula fija, Subgrupo... subgrupos) {
        return act.agregarPlaza("ACT-P" + (act.getPlazas().size() + 1), asig, fija, Set.of(PROF),
                Set.of(), Set.of(subgrupos));
    }

    private static Map<Long, List<AsignaturaAula>> reglas(AsignaturaAula... filas) {
        return DeduccionAulas.indicePorAsignatura(Arrays.asList(filas));
    }

    private static List<String> codigos(List<Aula> aulas) {
        return aulas.stream().map(Aula::getCodigo).toList();
    }

    @Test
    void soloDeUnPdcSinReglas_sinAulaPosibleConElMotivoNuevo() {
        GrupoAdministrativo padre = ordinario("3ºA", aula("R3A"));
        Asignatura amb = asignatura("ÁmbCM");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, null, subgrupo("3ºADi-Completo", null, pdc("3ºADi", padre)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, Map.of());

        assertThat(d.vacio()).isTrue();
        assertThat(d.motivo()).isEqualTo(DeduccionAulas.SOLO_PDC);
        assertThat(d.motivo()).isEqualTo("es una clase solo de PDC: no usa el aula del grupo padre, que a esa"
                + " hora está ocupada. Marca un aula para su asignatura o escríbela en la clase");
    }

    @Test
    void elAvisoDeClaseSinAulaLlevaElTextoVisible() {
        ProblemaHorario vacio = new ProblemaHorario(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
        List<AvisoPrevalidacion> avisos = PrevalidacionService.prevalidar(vacio, DatosCuadre.VACIO,
                new DatosAulas(List.of(new DatosAulas.PlazaSinAula("ÁmbCM-3ºADi", "ÁmbCM-3ºADi-P1",
                        DeduccionAulas.SOLO_PDC))));

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.regla()).isEqualTo(PrevalidacionService.REGLA_CLASE_SIN_AULA_POSIBLE);
            assertThat(a.descripcion()).isEqualTo("La plaza 'ÁmbCM-3ºADi-P1' de la actividad 'ÁmbCM-3ºADi' no"
                    + " tiene ninguna aula posible: es una clase solo de PDC: no usa el aula del grupo padre, que"
                    + " a esa hora está ocupada. Marca un aula para su asignatura o escríbela en la clase.");
        });
    }

    @Test
    void soloDeDosPdcDePadresDistintos_sinAulaPosibleConElMotivoNuevo() {
        GrupoAdministrativo a = ordinario("3ºA", aula("R3A"));
        GrupoAdministrativo b = ordinario("3ºB", aula("R3B"));
        Asignatura amb = asignatura("ÁmbSL");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, null,
                subgrupo("3ºADi-Completo", null, pdc("3ºADi", a)), subgrupo("3ºBDi-Completo", null, pdc("3ºBDi", b)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, Map.of());

        assertThat(codigos(d.aulas())).isEmpty();
        assertThat(d.motivo()).isEqualTo(DeduccionAulas.SOLO_PDC);
    }

    @Test
    void soloDePdcConUnaPreferida_elDominioEsEsaAulaYEsPreferidaEfectiva() {
        Aula diver = aula("B04");
        GrupoAdministrativo padre = ordinario("3ºA", aula("R3A"));
        Asignatura amb = asignatura("ÁmbCM");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, null, subgrupo("3ºADi-Completo", null, pdc("3ºADi", padre)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(amb, diver, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d.aulas())).containsExactly("B04");
        assertThat(codigos(d.preferidas())).containsExactly("B04");
    }

    @Test
    void soloDePdcConUnaExclusiva_elDominioEsLaExclusiva() {
        Aula diver = aula("B04");
        GrupoAdministrativo padre = ordinario("3ºA", aula("R3A"));
        Asignatura amb = asignatura("ÁmbCM");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, null, subgrupo("3ºADi-Completo", null, pdc("3ºADi", padre)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(amb, diver, RolAulaAsignatura.EXCLUSIVA)));

        assertThat(codigos(d.aulas())).containsExactly("B04");
        assertThat(d.preferidas()).isEmpty();
    }

    @Test
    void soloDePdc_laPreferidaPasaPorLosFiltrosDeSiempre() {
        Aula pequena = aula("B04", 5);
        Aula fuera = aula("A8");
        fuera.setEnUso(false);
        GrupoAdministrativo padre = ordinario("3ºA", aula("R3A"));
        Asignatura amb = asignatura("ÁmbCM");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, null, subgrupo("3ºADi-Completo", 10, pdc("3ºADi", padre)));

        DeduccionAulas.Dominio sinSitio = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(amb, pequena, RolAulaAsignatura.PREFERIDA)));
        DeduccionAulas.Dominio noSeUsa = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(amb, fuera, RolAulaAsignatura.PREFERIDA)));

        assertThat(sinSitio.vacio()).isTrue();
        assertThat(sinSitio.motivo()).isEqualTo("ninguna de sus aulas posibles tiene sitio para 10 alumnos");
        assertThat(noSeUsa.vacio()).isTrue();
        assertThat(noSeUsa.motivo()).isEqualTo(DeduccionAulas.TODAS_NO_SE_USAN);
    }

    @Test
    void plazaMixtaDePdcYGrupoOrdinario_elPdcSigueContandoComoSuPadre() {
        GrupoAdministrativo a = ordinario("3ºA", aula("R3A"));
        GrupoAdministrativo b = ordinario("3ºB", aula("R3B"));
        Asignatura ef = asignatura("EF");
        Actividad act = clase(ef);
        Plaza plaza = plaza(act, ef, null,
                subgrupo("3ºADi-Completo", null, pdc("3ºADi", a)), subgrupo("3ºB-EF", null, b));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()).aulas())).containsExactly("R3A", "R3B");
    }

    @Test
    void plazaSinGrupos_noEsSoloDePdc_motivoGenerico() {
        Asignatura mat = asignatura("Mat");
        Actividad act = clase(mat);
        Plaza plaza = plaza(act, mat, null);

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, Map.of());

        assertThat(d.vacio()).isTrue();
        assertThat(d.motivo()).isEqualTo(DeduccionAulas.SIN_REGLAS);
    }

    @Test
    void soloDePdcConAulaEscrita_mandaLoEscrito() {
        Aula escrita = aula("B04");
        GrupoAdministrativo padre = ordinario("3ºA", aula("R3A"));
        Asignatura amb = asignatura("ÁmbCM");
        Actividad act = clase(amb);
        Plaza plaza = plaza(act, amb, escrita, subgrupo("3ºADi-Completo", null, pdc("3ºADi", padre)));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()).aulas())).containsExactly("B04");
    }
}
