package es.yaroki.educhronos.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaAula;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.Dia;
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
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.service.AvisoPrevalidacion;
import es.yaroki.educhronos.app.service.DatosAulas;
import es.yaroki.educhronos.app.service.DatosCuadre;
import es.yaroki.educhronos.app.service.PrevalidacionService;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Aulas posibles de cada plaza de CLASE (S207, C-deduccion-aulas, A): {@link DeduccionAulas} y su
 * uso en {@link CatalogoMapper}. Puros: entidades en memoria, sin Spring ni base. Los ids que
 * fijan el orden (A6) se ponen con {@link ReflectionTestUtils}, porque una entidad sin persistir
 * no tiene id.
 */
class DeduccionAulasTest {

    private static final Nivel NIVEL = new Nivel("1ESO", 1);
    private static final Profesor PROF = new Profesor("P1", "Profesor");
    private long siguienteId = 1;

    // ─────────────────────────────────────────────────────────── fixtures

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

    private static GrupoAdministrativo grupo(String codigo, Aula aulaDeGrupo) {
        GrupoAdministrativo g = new GrupoAdministrativo(codigo, NIVEL, TipoGrupo.ORDINARIO, null);
        g.setAulaReferencia(aulaDeGrupo);
        return g;
    }

    private static Subgrupo subgrupo(String codigo, Integer alumnos, GrupoAdministrativo... grupos) {
        Subgrupo s = new Subgrupo(codigo, Set.of(grupos));
        s.setAlumnos(alumnos);
        return s;
    }

    private static Actividad actividad(Asignatura asig, TipoActividad tipo) {
        Actividad a = new Actividad("ACT", asig, 1, 1, PatronTemporal.NEUTRA, false);
        a.setTipo(tipo);
        return a;
    }

    private static Plaza plaza(Actividad act, Asignatura asig, Aula fija, Set<Aula> candidatas,
                               Subgrupo... subgrupos) {
        return act.agregarPlaza("ACT-P" + (act.getPlazas().size() + 1), asig, fija, Set.of(PROF),
                candidatas, Set.of(subgrupos));
    }

    private static Map<Long, List<AsignaturaAula>> reglas(AsignaturaAula... filas) {
        return DeduccionAulas.indicePorAsignatura(Arrays.asList(filas));
    }

    private static List<String> codigos(DeduccionAulas.Dominio d) {
        return d.aulas().stream().map(Aula::getCodigo).toList();
    }

    // ─────────────────────────────────────────────────────── A1, aula escrita

    @Test
    void a1_aulaEscrita_elDominioEsLoEscrito_aunqueHayaReglas() {
        Aula r = aula("R");
        Aula x = aula("X");
        Aula p = aula("P");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, x, Set.of(), subgrupo("G-1", null, grupo("G", r)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(mat, p, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("X");
    }

    @Test
    void a1_candidatasEscritas_elDominioSonLasCandidatas() {
        Aula x = aula("X");
        Aula y = aula("Y");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(y, x), subgrupo("G-1", null, grupo("G", null)));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()))).containsExactly("X", "Y");
    }

    // ───────────────────────────────────────────────────── A2, sin aula escrita

    @Test
    void a2_unPdcCuentaComoSuPadre_recibeElAulaDelPadre() {
        Aula delPadre = aula("R3A");
        Aula delPdc = aula("R3ADi");
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo padre = grupo("3ºA", delPadre);
        GrupoAdministrativo pdc = new GrupoAdministrativo("3ºADi", NIVEL, TipoGrupo.DIVERSIFICACION_PDC, padre);
        pdc.setAulaReferencia(delPdc);
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("3ºADi-Completo", null, pdc));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()))).containsExactly("R3A");
    }

    @Test
    void a2_unaClaseDeVariosGruposRecibeLasAulasDeTodos() {
        Aula ra = aula("RA");
        Aula rb = aula("RB");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(),
                subgrupo("A-1", null, grupo("A", ra)), subgrupo("B-1", null, grupo("B", rb)),
                subgrupo("C-1", null, grupo("C", null)));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()))).containsExactly("RA", "RB");
    }

    @Test
    void a2_lasExclusivasExcluyenElAulaDeGrupoYLasPreferidas() {
        Aula r = aula("R");
        Aula e = aula("E");
        Aula p = aula("P");
        Asignatura mus = asignatura("Mús");
        Actividad act = actividad(mus, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mus, null, Set.of(), subgrupo("G-1", null, grupo("G", r)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, reglas(
                new AsignaturaAula(mus, e, RolAulaAsignatura.EXCLUSIVA),
                new AsignaturaAula(mus, p, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("E");
    }

    @Test
    void a2_lasPreferidasSeSumanAlAulaDeGrupo() {
        Aula r = aula("R");
        Aula p = aula("P");
        Asignatura bio = asignatura("Bio");
        Asignatura otra = asignatura("Otra");
        Aula deOtra = aula("Q");
        Actividad act = actividad(bio, TipoActividad.CLASE);
        Plaza plaza = plaza(act, bio, null, Set.of(), subgrupo("G-1", null, grupo("G", r)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, reglas(
                new AsignaturaAula(bio, p, RolAulaAsignatura.PREFERIDA),
                new AsignaturaAula(otra, deOtra, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).as("las de otra asignatura no cuentan").containsExactly("R", "P");
    }

    @Test
    void a2_sinAulaDeGrupoNiAulasDeLaAsignatura_vacioConSuMotivo() {
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("G-1", null, grupo("G", null)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, Map.of());

        assertThat(d.vacio()).isTrue();
        assertThat(d.motivo()).isEqualTo("sin aula de grupo ni aulas de la asignatura");
    }

    // ────────────────────────────────────────────────────────── A3, filtros

    @Test
    void a3_noSeUsaFiltraLoDeducido() {
        Aula r = aula("R");
        r.setEnUso(false);
        Aula p = aula("P");
        Asignatura bio = asignatura("Bio");
        Actividad act = actividad(bio, TipoActividad.CLASE);
        Plaza plaza = plaza(act, bio, null, Set.of(), subgrupo("G-1", null, grupo("G", r)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(bio, p, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("P");
    }

    @Test
    void a3_noSeUsaFiltraTambienLoEscrito_conElMotivoDelAulaEscrita() {
        Aula x = aula("X");
        x.setEnUso(false);
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, x, Set.of(), subgrupo("G-1", null, grupo("G", aula("R"))));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, Map.of());

        assertThat(d.vacio()).as("lo escrito manda: no cae a la deducción").isTrue();
        assertThat(d.motivo()).isEqualTo("el aula escrita X está marcada No se usa");
    }

    @Test
    void a3_todasLasDeducidasNoSeUsan_motivoGeneral() {
        Aula r = aula("R");
        r.setEnUso(false);
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("G-1", null, grupo("G", r)));

        assertThat(DeduccionAulas.dominio(act, plaza, Map.of()).motivo())
                .isEqualTo("todas sus aulas posibles están marcadas No se usa");
    }

    @Test
    void a3_capacidadIgualALosAlumnosCabe_yUnaMenosNo() {
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo g = grupo("G", null);
        Subgrupo s1 = subgrupo("G-1", 12, g);
        Subgrupo s2 = subgrupo("G-2", 8, g);

        Actividad cabe = actividad(mat, TipoActividad.CLASE);
        Plaza enVeinte = plaza(cabe, mat, aula("X", 20), Set.of(), s1, s2);
        assertThat(codigos(DeduccionAulas.dominio(cabe, enVeinte, Map.of()))).containsExactly("X");

        Actividad noCabe = actividad(mat, TipoActividad.CLASE);
        Plaza enDiecinueve = plaza(noCabe, mat, aula("Y", 19), Set.of(), s1, s2);
        DeduccionAulas.Dominio d = DeduccionAulas.dominio(noCabe, enDiecinueve, Map.of());
        assertThat(d.vacio()).isTrue();
        assertThat(d.motivo()).isEqualTo("el aula escrita Y no tiene sitio para 20 alumnos");
    }

    @Test
    void a3_capacidadDesconocida_sinLimite() {
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, aula("X", null), Set.of(), subgrupo("G-1", 500, grupo("G", null)));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()))).containsExactly("X");
    }

    @Test
    void a3_alumnosDesconocidosCuentanCero() {
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo g = grupo("G", null);
        Subgrupo doce = subgrupo("G-1", 12, g);
        Subgrupo sinDato = subgrupo("G-2", null, g);

        Actividad cabe = actividad(mat, TipoActividad.CLASE);
        Plaza enDoce = plaza(cabe, mat, aula("X", 12), Set.of(), doce, sinDato);
        assertThat(codigos(DeduccionAulas.dominio(cabe, enDoce, Map.of()))).containsExactly("X");

        Actividad noCabe = actividad(mat, TipoActividad.CLASE);
        Plaza enOnce = plaza(noCabe, mat, aula("Y", 11), Set.of(), doce, sinDato);
        assertThat(DeduccionAulas.dominio(noCabe, enOnce, Map.of()).motivo())
                .isEqualTo("el aula escrita Y no tiene sitio para 12 alumnos");
    }

    @Test
    void a3_ningunaDeducidaTieneSitio_motivoGeneral() {
        Aula r = aula("R", 10);
        Aula p = aula("P", 15);
        Asignatura bio = asignatura("Bio");
        Actividad act = actividad(bio, TipoActividad.CLASE);
        Plaza plaza = plaza(act, bio, null, Set.of(), subgrupo("G-1", 25, grupo("G", r)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(bio, p, RolAulaAsignatura.PREFERIDA)));

        assertThat(d.motivo()).isEqualTo("ninguna de sus aulas posibles tiene sitio para 25 alumnos");
    }

    /**
     * Causas mezcladas: manda la que eliminó la ÚLTIMA aula en orden de id. Aquí R (id menor) no
     * se usa y P (id mayor) es pequeña: el motivo es el de capacidad. Al revés, el de «No se usa».
     */
    @Test
    void a3_causasMezcladas_mandaLaQueEliminoLaUltimaAula() {
        Asignatura bio = asignatura("Bio");
        Aula r = aula("R", null);
        r.setEnUso(false);
        Aula p = aula("P", 5);
        Actividad act = actividad(bio, TipoActividad.CLASE);
        Plaza plaza = plaza(act, bio, null, Set.of(), subgrupo("G-1", 20, grupo("G", r)));
        assertThat(DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(bio, p, RolAulaAsignatura.PREFERIDA))).motivo())
                .isEqualTo("ninguna de sus aulas posibles tiene sitio para 20 alumnos");

        Aula r2 = aula("R2", 5);
        Aula p2 = aula("P2", null);
        p2.setEnUso(false);
        Actividad act2 = actividad(bio, TipoActividad.CLASE);
        Plaza plaza2 = plaza(act2, bio, null, Set.of(), subgrupo("G-2", 20, grupo("G2", r2)));
        assertThat(DeduccionAulas.dominio(act2, plaza2,
                reglas(new AsignaturaAula(bio, p2, RolAulaAsignatura.PREFERIDA))).motivo())
                .isEqualTo("todas sus aulas posibles están marcadas No se usa");
    }

    @Test
    void a3_variasCandidatasEscritasEliminadas_motivoGeneral() {
        Aula x = aula("X");
        Aula y = aula("Y");
        x.setEnUso(false);
        y.setEnUso(false);
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(x, y), subgrupo("G-1", null, grupo("G", null)));

        assertThat(DeduccionAulas.dominio(act, plaza, Map.of()).motivo())
                .isEqualTo("todas sus aulas posibles están marcadas No se usa");
    }

    // ──────────────────────────────────────────────────────────── A5, A6

    @Test
    void a5_laDeduccionNoAplicaFueraDeClase() {
        Asignatura red = asignatura("Red");
        Actividad reunion = actividad(red, TipoActividad.REUNION);
        Plaza plaza = plaza(reunion, red, aula("X"), Set.of());

        assertThat(DeduccionAulas.aplica(reunion)).isFalse();
        assertThat(DeduccionAulas.aplica(actividad(red, TipoActividad.FUNCION))).isFalse();
        assertThat(DeduccionAulas.aplica(actividad(red, TipoActividad.CLASE))).isTrue();
        assertThatThrownBy(() -> DeduccionAulas.dominio(reunion, plaza, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a6_ordenPorIdAscendente_noPorCodigoNiPorOrdenDeEntrada() {
        Aula z = aula("Z");   // id menor
        Aula m = aula("M");
        Aula a = aula("A");   // id mayor
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(),
                subgrupo("G3-1", null, grupo("G3", a)), subgrupo("G1-1", null, grupo("G1", z)),
                subgrupo("G2-1", null, grupo("G2", m)));

        assertThat(codigos(DeduccionAulas.dominio(act, plaza, Map.of()))).containsExactly("Z", "M", "A");
    }

    // ───────────────────────────────────────── A4 y construcción del problema

    private static TramoSemanal lunes1() {
        return new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1, null);
    }

    @Test
    void a4_unaAulaPosibleEsAulaFija_variasSonCandidatas_ningunaEsSinAula() {
        Aula r = aula("R");
        Aula p = aula("P");
        Asignatura mat = asignatura("Mat");
        Asignatura bio = asignatura("Bio");
        GrupoAdministrativo g = grupo("G", r);
        GrupoAdministrativo sinAula = grupo("H", null);
        Subgrupo sg = subgrupo("G-1", null, g);
        Subgrupo sh = subgrupo("H-1", null, sinAula);
        Actividad unaAula = actividad(mat, TipoActividad.CLASE);
        unaAula.setCodigo("UNA");
        plaza(unaAula, mat, null, Set.of(), sg);
        Actividad dosAulas = actividad(bio, TipoActividad.CLASE);
        dosAulas.setCodigo("DOS");
        plaza(dosAulas, bio, null, Set.of(), sg);
        Actividad ninguna = actividad(mat, TipoActividad.CLASE);
        ninguna.setCodigo("NINGUNA");
        plaza(ninguna, mat, null, Set.of(), sh);
        unaAula.getPlazas().get(0).setCodigo("UNA-P1");
        dosAulas.getPlazas().get(0).setCodigo("DOS-P1");
        ninguna.getPlazas().get(0).setCodigo("NINGUNA-P1");

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(lunes1()), List.of(r, p), List.of(mat, bio), List.of(PROF),
                List.of(g, sinAula), List.of(sg, sh), List.of(unaAula, dosAulas, ninguna),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new AsignaturaAula(bio, p, RolAulaAsignatura.PREFERIDA)));

        es.yaroki.educhronos.solver.domain.Plaza enUna = problema.actividades().get(0).plazas().get(0);
        es.yaroki.educhronos.solver.domain.Plaza enDos = problema.actividades().get(1).plazas().get(0);
        es.yaroki.educhronos.solver.domain.Plaza enNinguna = problema.actividades().get(2).plazas().get(0);
        assertThat(enUna.aulaFija()).map(es.yaroki.educhronos.solver.domain.Aula::codigo).contains("R");
        assertThat(enUna.aulasCandidatas()).isEmpty();
        assertThat(enDos.aulaFija()).isEmpty();
        assertThat(enDos.aulasCandidatas()).extracting(es.yaroki.educhronos.solver.domain.Aula::codigo)
                .containsExactlyInAnyOrder("R", "P");
        assertThat(enNinguna.aulaFija()).isEmpty();
        assertThat(enNinguna.aulasCandidatas()).isEmpty();
    }

    @Test
    void a4_unaSolaCandidataEscritaQueSobreviveLlegaComoFija() {
        Aula x = aula("X");
        Aula y = aula("Y");
        y.setEnUso(false);
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo g = grupo("G", null);
        Subgrupo sg = subgrupo("G-1", null, g);
        Actividad act = actividad(mat, TipoActividad.CLASE);
        plaza(act, mat, null, Set.of(x, y), sg);

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(lunes1()), List.of(x, y), List.of(mat), List.of(PROF), List.of(g),
                List.of(sg), List.of(act), List.of(), List.of(), List.of(), List.of(), List.of());

        es.yaroki.educhronos.solver.domain.Plaza plaza = problema.actividades().get(0).plazas().get(0);
        assertThat(plaza.aulaFija()).map(es.yaroki.educhronos.solver.domain.Aula::codigo).contains("X");
        assertThat(plaza.aulasCandidatas()).isEmpty();
    }

    @Test
    void a5_reunionYFuncionLleganComoEstan_sinFiltros() {
        Aula x = aula("X", 1);
        x.setEnUso(false);
        Aula y = aula("Y");
        Asignatura red = asignatura("Red");
        Actividad reunion = actividad(red, TipoActividad.REUNION);
        reunion.setCodigo("REU");
        plaza(reunion, red, x, Set.of());
        Actividad funcion = actividad(red, TipoActividad.FUNCION);
        funcion.setCodigo("FUN");
        plaza(funcion, red, null, Set.of(y));
        reunion.getPlazas().get(0).setCodigo("REU-P1");
        funcion.getPlazas().get(0).setCodigo("FUN-P1");

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(lunes1()), List.of(x, y), List.of(red), List.of(PROF), List.of(),
                List.of(), List.of(reunion, funcion), List.of(), List.of(), List.of(), List.of(),
                List.of());

        assertThat(problema.actividades().get(0).plazas().get(0).aulaFija())
                .as("«No se usa» no filtra fuera de CLASE").isPresent();
        assertThat(problema.actividades().get(1).plazas().get(0).aulasCandidatas())
                .as("una candidata sola sigue siendo candidata fuera de CLASE").hasSize(1);
    }

    @Test
    void elProblemaLlevaCapacidadesYAlumnos_soloLosNoNulos() {
        Aula conCapacidad = aula("A1", 25);
        Aula sinCapacidad = aula("A2", null);
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo g = grupo("G", null);
        Subgrupo conAlumnos = subgrupo("G-1", 12, g);
        Subgrupo sinAlumnos = subgrupo("G-2", null, g);

        ProblemaHorario problema = CatalogoMapper.aProblemaHorario(
                List.of(lunes1()), List.of(conCapacidad, sinCapacidad), List.of(mat), List.of(PROF),
                List.of(g), List.of(conAlumnos, sinAlumnos), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());

        assertThat(problema.capacidadesDeAula()).containsExactlyEntriesOf(Map.of("A1", 25));
        assertThat(problema.alumnosDeSubgrupo()).containsExactlyEntriesOf(Map.of("G-1", 12));
    }

    // ─────────────────────────── Preferidas efectivas (S208, C-preferencias-aulas, A1-A5)

    private static List<String> preferidas(DeduccionAulas.Dominio d) {
        return d.preferidas().stream().map(Aula::getCodigo).toList();
    }

    private static List<TramoSemanal> cincoTramos() {
        return java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(o -> new TramoSemanal(Dia.LUNES, LocalTime.of(7 + o, 0),
                        LocalTime.of(8 + o, 0), true, o, null))
                .toList();
    }

    /** (a) P1: aula de grupo A y PREFERIDA L → posibles {A, L}, preferidas {L}. */
    @Test
    void s208a_p1_unGrupo_posiblesAulaDeGrupoYPreferida_preferidasSoloLaPreferida() {
        Aula a = aula("A");
        Aula l = aula("L");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("G-1", null, grupo("G", a)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("A", "L");
        assertThat(preferidas(d)).containsExactly("L");
    }

    /** (b) P2: dos grupos con aulas A y B y PREFERIDA L → preferidas {L}. */
    @Test
    void s208b_p2_variosGrupos_preferidasSoloLaPreferida() {
        Aula a = aula("A");
        Aula b = aula("B");
        Aula l = aula("L");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(),
                subgrupo("G-1", null, grupo("G", a)), subgrupo("H-1", null, grupo("H", b)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("A", "B", "L");
        assertThat(preferidas(d)).containsExactly("L");
    }

    /**
     * (c) G1 y G2: la asignatura de la plaza no tiene PREFERIDAS (otra sí) → sin preferidas, ni
     * las aulas de grupo, que son posibles pero no preferidas.
     */
    @Test
    void s208c_g1YG2_sinPreferidas() {
        Aula a = aula("A");
        Aula b = aula("B");
        Aula l = aula("L");
        Asignatura mat = asignatura("Mat");
        Asignatura bio = asignatura("Bio");
        Map<Long, List<AsignaturaAula>> conReglaDeOtra =
                reglas(new AsignaturaAula(bio, l, RolAulaAsignatura.PREFERIDA));
        Actividad g1 = actividad(mat, TipoActividad.CLASE);
        Plaza unGrupo = plaza(g1, mat, null, Set.of(), subgrupo("G-1", null, grupo("G", a)));
        Actividad g2 = actividad(mat, TipoActividad.CLASE);
        Plaza dosGrupos = plaza(g2, mat, null, Set.of(),
                subgrupo("G-2", null, grupo("G", a)), subgrupo("H-1", null, grupo("H", b)));

        DeduccionAulas.Dominio d1 = DeduccionAulas.dominio(g1, unGrupo, conReglaDeOtra);
        DeduccionAulas.Dominio d2 = DeduccionAulas.dominio(g2, dosGrupos, conReglaDeOtra);

        assertThat(codigos(d1)).containsExactly("A");
        assertThat(preferidas(d1)).isEmpty();
        assertThat(codigos(d2)).containsExactly("A", "B");
        assertThat(preferidas(d2)).isEmpty();
    }

    /**
     * (d) Aula escrita (decisión L): sin preferidas aunque la asignatura las tenga, y aunque el
     * aula escrita sea una de ellas.
     */
    @Test
    void s208d_aulaEscrita_fijaOCandidatas_sinPreferidas() {
        Aula a = aula("A");
        Aula l = aula("L");
        Aula y = aula("Y");
        Asignatura mat = asignatura("Mat");
        Map<Long, List<AsignaturaAula>> conPreferida =
                reglas(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA));
        Actividad conFija = actividad(mat, TipoActividad.CLASE);
        Plaza fija = plaza(conFija, mat, l, Set.of(), subgrupo("G-1", null, grupo("G", a)));
        Actividad conCandidatas = actividad(mat, TipoActividad.CLASE);
        Plaza candidatas = plaza(conCandidatas, mat, null, Set.of(l, y),
                subgrupo("G-2", null, grupo("G", a)));

        DeduccionAulas.Dominio dFija = DeduccionAulas.dominio(conFija, fija, conPreferida);
        DeduccionAulas.Dominio dCand = DeduccionAulas.dominio(conCandidatas, candidatas, conPreferida);

        assertThat(codigos(dFija)).containsExactly("L");
        assertThat(preferidas(dFija)).isEmpty();
        assertThat(codigos(dCand)).containsExactly("L", "Y");
        assertThat(preferidas(dCand)).isEmpty();
    }

    /** (e) Una PREFERIDA «No se usa» sale del dominio y de las preferidas; la otra queda. */
    @Test
    void s208e_preferidaNoSeUsa_fueraDeLasPreferidas() {
        Aula a = aula("A");
        Aula l = aula("L");
        l.setEnUso(false);
        Aula m = aula("M");
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("G-1", null, grupo("G", a)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, reglas(
                new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA),
                new AsignaturaAula(mat, m, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("A", "M");
        assertThat(preferidas(d)).containsExactly("M");
    }

    /** (f) Una PREFERIDA sin sitio para los alumnos sale de las preferidas; la otra queda. */
    @Test
    void s208f_preferidaSinCapacidad_fueraDeLasPreferidas() {
        Aula a = aula("A");
        Aula l = aula("L", 10);
        Aula m = aula("M", 30);
        Asignatura mat = asignatura("Mat");
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("G-1", 20, grupo("G", a)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, reglas(
                new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA),
                new AsignaturaAula(mat, m, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("A", "M");
        assertThat(preferidas(d)).containsExactly("M");
    }

    /** (g) Asignatura con EXCLUSIVAS: el dominio son ellas y ninguna es preferida. */
    @Test
    void s208g_exclusivas_sinPreferidas() {
        Aula a = aula("A");
        Aula e1 = aula("E1");
        Aula e2 = aula("E2");
        Asignatura mus = asignatura("Mús");
        Actividad act = actividad(mus, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mus, null, Set.of(), subgrupo("G-1", null, grupo("G", a)));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza, reglas(
                new AsignaturaAula(mus, e1, RolAulaAsignatura.EXCLUSIVA),
                new AsignaturaAula(mus, e2, RolAulaAsignatura.EXCLUSIVA)));

        assertThat(codigos(d)).containsExactly("E1", "E2");
        assertThat(preferidas(d)).isEmpty();
    }

    /**
     * (h) La plaza de un PDC se comporta como la de su padre: posibles {aula del padre, L} (no la
     * del PDC) y preferidas {L}.
     */
    @Test
    void s208h_pdc_comoSuPadre() {
        Aula delPadre = aula("R3A");
        Aula delPdc = aula("R3ADi");
        Aula l = aula("L");
        Asignatura mat = asignatura("Mat");
        GrupoAdministrativo padre = grupo("3ºA", delPadre);
        GrupoAdministrativo pdc = new GrupoAdministrativo("3ºADi", NIVEL, TipoGrupo.DIVERSIFICACION_PDC, padre);
        pdc.setAulaReferencia(delPdc);
        Actividad act = actividad(mat, TipoActividad.CLASE);
        Plaza plaza = plaza(act, mat, null, Set.of(), subgrupo("3ºADi-Completo", null, pdc));

        DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, plaza,
                reglas(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA)));

        assertThat(codigos(d)).containsExactly("R3A", "L");
        assertThat(preferidas(d)).containsExactly("L");
    }

    /**
     * Catálogo de (i) y (j): UNA (P1, aula de grupo A, PREFERIDA L de Mat), DOS (P2, grupos G y
     * H con A y B, Mat), G1 (Bio, sin preferidas) y SIN (Len, grupo sin aula y su única PREFERIDA
     * N marcada «No se usa»: dominio vacío). Cinco tramos, para que ninguna regla aritmética salte.
     */
    private record Catalogo(List<Aula> aulas, List<Asignatura> asignaturas,
                            List<GrupoAdministrativo> grupos, List<Subgrupo> subgrupos,
                            List<Actividad> actividades, List<AsignaturaAula> reglas) {
        ProblemaHorario problema() {
            return CatalogoMapper.aProblemaHorario(cincoTramos(), aulas, asignaturas,
                    List.of(PROF), grupos, subgrupos, actividades, List.of(), List.of(), List.of(),
                    List.of(), reglas);
        }
    }

    private Catalogo catalogoConPreferidas() {
        Aula a = aula("A");
        Aula b = aula("B");
        Aula l = aula("L");
        Aula n = aula("N");
        n.setEnUso(false);
        Asignatura mat = asignatura("Mat");
        Asignatura bio = asignatura("Bio");
        Asignatura len = asignatura("Len");
        GrupoAdministrativo g = grupo("G", a);
        GrupoAdministrativo h = grupo("H", b);
        GrupoAdministrativo k = grupo("K", null);
        Subgrupo sg = subgrupo("G-1", null, g);
        Subgrupo sh = subgrupo("H-1", null, h);
        Subgrupo sk = subgrupo("K-1", null, k);
        Actividad una = actividad(mat, TipoActividad.CLASE);
        una.setCodigo("UNA");
        plaza(una, mat, null, Set.of(), sg).setCodigo("UNA-P1");
        Actividad dos = actividad(mat, TipoActividad.CLASE);
        dos.setCodigo("DOS");
        plaza(dos, mat, null, Set.of(), sg, sh).setCodigo("DOS-P1");
        Actividad g1 = actividad(bio, TipoActividad.CLASE);
        g1.setCodigo("G1");
        plaza(g1, bio, null, Set.of(), sh).setCodigo("G1-P1");
        Actividad sin = actividad(len, TipoActividad.CLASE);
        sin.setCodigo("SIN");
        plaza(sin, len, null, Set.of(), sk).setCodigo("SIN-P1");
        return new Catalogo(List.of(a, b, l, n), List.of(mat, bio, len), List.of(g, h, k),
                List.of(sg, sh, sk), List.of(una, dos, g1, sin),
                List.of(new AsignaturaAula(mat, l, RolAulaAsignatura.PREFERIDA),
                        new AsignaturaAula(len, n, RolAulaAsignatura.PREFERIDA)));
    }

    /**
     * (i) La prevalidación no cambia con las preferidas: sobre el catálogo con preferidas da lo
     * mismo que sobre el mismo problema sin ellas, y lo de siempre (la plaza SIN sin aula
     * posible). Los datos de aulas se arman como {@code GeneradorHorarioService.cargarDatosAulas}.
     */
    @Test
    void s208i_laPrevalidacionNoCambiaConLasPreferidas() {
        Catalogo c = catalogoConPreferidas();
        ProblemaHorario conPreferidas = c.problema();
        ProblemaHorario sinPreferidas = new ProblemaHorario(conPreferidas.tramos(),
                conPreferidas.aulas(), conPreferidas.asignaturas(), conPreferidas.profesores(),
                conPreferidas.grupos(), conPreferidas.subgrupos(), conPreferidas.actividades(),
                conPreferidas.restriccionesHorarias(), conPreferidas.bloqueos(),
                conPreferidas.tutorias(), conPreferidas.capacidadesDeAula(),
                conPreferidas.alumnosDeSubgrupo());
        Map<Long, List<AsignaturaAula>> indice = DeduccionAulas.indicePorAsignatura(c.reglas());
        List<DatosAulas.PlazaSinAula> sinAula = new ArrayList<>();
        for (Actividad act : c.actividades()) {
            for (Plaza p : act.getPlazas()) {
                DeduccionAulas.Dominio d = DeduccionAulas.dominio(act, p, indice);
                if (d.vacio()) {
                    sinAula.add(new DatosAulas.PlazaSinAula(act.getCodigo(), p.getCodigo(), d.motivo()));
                }
            }
        }
        DatosAulas datos = new DatosAulas(sinAula);

        List<AvisoPrevalidacion> avisos =
                PrevalidacionService.prevalidar(conPreferidas, DatosCuadre.VACIO, datos);

        assertThat(avisos).isEqualTo(
                PrevalidacionService.prevalidar(sinPreferidas, DatosCuadre.VACIO, datos));
        assertThat(avisos).extracting(AvisoPrevalidacion::regla)
                .containsExactly(PrevalidacionService.REGLA_CLASE_SIN_AULA_POSIBLE);
        assertThat(sinAula).singleElement().satisfies(s -> {
            assertThat(s.plazaCodigo()).isEqualTo("SIN-P1");
            assertThat(s.motivo()).isEqualTo(DeduccionAulas.TODAS_NO_SE_USAN);
        });
    }

    /** (j) El problema lleva las preferidas por código de plaza: solo UNA y DOS, con {L}. */
    @Test
    void s208j_elProblemaLlevaLasPreferidasPorCodigoDePlaza() {
        ProblemaHorario problema = catalogoConPreferidas().problema();

        assertThat(problema.preferidasDePlaza()).containsExactlyInAnyOrderEntriesOf(
                Map.of("UNA-P1", Set.of("L"), "DOS-P1", Set.of("L")));
    }
}
