package es.yaroki.educhronos.solver.io;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SesionBloqueada;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemaHorarioJsonLoaderTest {

    private final ProblemaHorarioJsonLoader loader = new ProblemaHorarioJsonLoader();

    @Test
    void cargaDatasetMinimoValidoSinExcepciones() throws Exception {
        ProblemaHorario problema;
        try (InputStream in = recurso("/fixtures/problema-minimo.json")) {
            problema = loader.cargar(in);
        }
        assertThat(problema.grupos()).hasSize(1);
        assertThat(problema.subgrupos()).hasSize(1);
        assertThat(problema.profesores()).hasSize(4);
        assertThat(problema.actividades()).hasSize(3);

        Actividad lcl = problema.actividades().stream()
                .filter(a -> a.codigo().equals("LCL-1A"))
                .findFirst().orElseThrow();
        assertThat(lcl.plazas()).hasSize(1);

        Plaza plazaLcl = lcl.plazas().get(0);
        assertThat(plazaLcl.profesores())
                .extracting(Profesor::codigo)
                .containsExactlyInAnyOrder("LEN2", "LEN8");   // soporte de co-docencia
        assertThat(plazaLcl.aulaFija()).isPresent();
        assertThat(plazaLcl.aulasCandidatas()).isEmpty();
    }

    @Test
    void rechazaReferenciaAProfesorInexistente() {
        String json = """
            { "tramos": [], "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"1A-Completo","grupos":["1A"]}],
              "actividades": [{"codigo":"Mat-1A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P1","asignatura":"Mat","profesores":["FANTASMA"],
                 "aulaFija":"A5","subgrupos":["1A-Completo"]}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("FANTASMA");
    }

    @Test
    void rechazaPlazaSinProfesores() {
        String json = """
            { "tramos": [], "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"1A-Completo","grupos":["1A"]}],
              "actividades": [{"codigo":"Mat-1A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P1","asignatura":"Mat","profesores":[],
                 "aulaFija":"A5","subgrupos":["1A-Completo"]}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("I7");
    }

    @Test
    void cargaAulasCandidatasResueltas() throws Exception {
        String json = """
            { "tramos": [], "aulas": [{"codigo":"A5","nombre":"Aula 5"},{"codigo":"A6","nombre":"Aula 6"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"1A-Completo","grupos":["1A"]}],
              "actividades": [{"codigo":"Mat-1A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P1","asignatura":"Mat","profesores":["MAT8"],
                 "aulasCandidatas":["A5","A6"],"subgrupos":["1A-Completo"]}]}] }
            """;
        ProblemaHorario problema = loader.cargar(stream(json));

        Plaza plaza = problema.actividades().get(0).plazas().get(0);
        assertThat(plaza.aulaFija()).isEmpty();
        assertThat(plaza.aulasCandidatas())
                .extracting(Aula::codigo)
                .containsExactlyInAnyOrder("A5", "A6");
    }

    @Test
    void rechazaGrupoPdcSinGrupoPadre() {
        String json = """
            { "tramos": [], "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"3ADi","tipo":"DIVERSIFICACION_PDC"}],
              "subgrupos": [{"codigo":"S","grupos":["3ADi"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("grupoPadre");
    }

    @Test
    void rechazaSubgrupoEnDosPlazasDeLaMismaActividad() {
        String json = """
            { "tramos": [], "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"},{"codigo":"MAT9","nombre":"P9"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P1","asignatura":"Mat","profesores":["MAT8"],"aulaFija":"A5","subgrupos":["S"]},
                {"codigo":"P2","asignatura":"Mat","profesores":["MAT9"],"aulaFija":"A5","subgrupos":["S"]}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("I2");
    }

    @Test
    void cargaRestriccionHorariaDuraResuelta() throws Exception {
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}],
              "restriccionesHorarias": [
                {"profesor":"MAT8","tramo":"LUN-1","tipo":"DURA","motivo":"reduccion"}] }
            """;
        ProblemaHorario problema = loader.cargar(stream(json));

        assertThat(problema.restriccionesHorarias()).hasSize(1);
        RestriccionHoraria r = problema.restriccionesHorarias().get(0);
        assertThat(r.profesor().codigo()).isEqualTo("MAT8");
        assertThat(r.tramo().codigo()).isEqualTo("LUN-1");
        assertThat(r.tipo()).isEqualTo(TipoRestriccion.DURA);
        assertThat(r.peso()).isEqualTo(1);               // default aplicado
        assertThat(r.motivo()).contains("reduccion");
    }

    @Test
    void cargaProblemaSinRestriccionesHorarias() throws Exception {
        // La sección es opcional: un problema sin ella carga con lista vacía.
        ProblemaHorario problema;
        try (InputStream in = recurso("/fixtures/problema-minimo.json")) {
            problema = loader.cargar(in);
        }
        assertThat(problema.restriccionesHorarias()).isEmpty();
    }

    @Test
    void rechazaRestriccionHorariaConTramoInexistente() {
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}],
              "restriccionesHorarias": [
                {"profesor":"MAT8","tramo":"FANTASMA","tipo":"DURA"}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("FANTASMA");
    }

    @Test
    void cargaBloqueoResuelto() throws Exception {
        // La actividad "A" (rep 2) se pina en su instancia #2 al tramo LUN-1.
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1},
                         {"codigo":"LUN-2","diaSemana":1,"ordenEnDia":2}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":2,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}],
              "bloqueos": [{"actividad":"A","indice":2,"tramo":"LUN-1"}] }
            """;
        ProblemaHorario problema = loader.cargar(stream(json));

        assertThat(problema.bloqueos()).hasSize(1);
        SesionBloqueada b = problema.bloqueos().get(0);
        assertThat(b.instancia().actividad().codigo()).isEqualTo("A");
        assertThat(b.instancia().indice()).isEqualTo(2);
        assertThat(b.tramo().codigo()).isEqualTo("LUN-1");
    }

    @Test
    void rechazaBloqueoConIndiceFueraDeRango() {
        // La actividad "A" tiene rep 1: el indice 2 no existe. La validacion es la
        // del constructor de ActividadInstancia, traducida a ProblemaInvalidoException.
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}],
              "bloqueos": [{"actividad":"A","indice":2,"tramo":"LUN-1"}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("indice");
    }

    @Test
    void cargaBloqueoConPinDeAula() throws Exception {
        // La plaza P (aula variable {A5,A6}) se pina al aula A6 en la instancia A#1.
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"},{"codigo":"A6","nombre":"Aula 6"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulasCandidatas":["A5","A6"],"subgrupos":["S"]}]}],
              "bloqueos": [{"actividad":"A","indice":1,"tramo":"LUN-1",
                "aulasPinadas":[{"plaza":"P","aula":"A6"}]}] }
            """;
        ProblemaHorario problema = loader.cargar(stream(json));

        assertThat(problema.bloqueos()).hasSize(1);
        SesionBloqueada b = problema.bloqueos().get(0);
        assertThat(b.aulasPinadas()).hasSize(1);
        Plaza plaza = problema.actividades().get(0).plazas().get(0);
        assertThat(b.aulasPinadas().get(plaza).codigo()).isEqualTo("A6");
    }

    @Test
    void rechazaPinDeAulaSobrePlazaConAulaFija() {
        // 2A: la plaza P tiene aulaFija A5; pinar un aula sobre ella es invalido.
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulaFija":"A5","subgrupos":["S"]}]}],
              "bloqueos": [{"actividad":"A","indice":1,"tramo":"LUN-1",
                "aulasPinadas":[{"plaza":"P","aula":"A5"}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("aula fija");
    }

    @Test
    void rechazaPinDeAulaFueraDeCandidatas() {
        // 4C: la plaza P tiene candidatas {A5}; pinar A6 (no candidata) es invalido.
        String json = """
            { "tramos": [{"codigo":"LUN-1","diaSemana":1,"ordenEnDia":1}],
              "aulas": [{"codigo":"A5","nombre":"Aula 5"},{"codigo":"A6","nombre":"Aula 6"}],
              "asignaturas": [{"codigo":"Mat","nombre":"Matematicas"}],
              "profesores": [{"codigo":"MAT8","nombre":"P"}],
              "grupos": [{"codigo":"1A","tipo":"ORDINARIO"}],
              "subgrupos": [{"codigo":"S","grupos":["1A"]}],
              "actividades": [{"codigo":"A","asignatura":"Mat","repeticionesPorSemana":1,
                "duracionTramos":1,"patronTemporal":"NEUTRA","plazas":[
                {"codigo":"P","asignatura":"Mat","profesores":["MAT8"],
                 "aulasCandidatas":["A5"],"subgrupos":["S"]}]}],
              "bloqueos": [{"actividad":"A","indice":1,"tramo":"LUN-1",
                "aulasPinadas":[{"plaza":"P","aula":"A6"}]}] }
            """;
        assertThatThrownBy(() -> loader.cargar(stream(json)))
                .isInstanceOf(ProblemaInvalidoException.class)
                .hasMessageContaining("aulasCandidatas");
    }

    private InputStream recurso(String ruta) {
        InputStream in = getClass().getResourceAsStream(ruta);
        if (in == null) {
            throw new IllegalStateException("recurso de test no encontrado: " + ruta);
        }
        return in;
    }

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}