package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Lo que {@link VistaPdf} y {@link PaginaPorRecurso} deciden por su cuenta: la traducción
 * del query param y las piezas de texto que cada vista compone (desde S215 las de grupo,
 * profesor y aula viven en {@link PaginaPorRecurso}). La maqueta se prueba en {@code HorarioPdfTest},
 * que es quien la dibuja.
 */
class VistaPdfTest {

    // ------------------------------------------------------------------ desdeParametro

    @Test
    void elParametroDeUnaVistaDevuelveEsaVista() {
        assertThat(VistaPdf.desdeParametro("grupo")).contains(VistaPdf.GRUPO);
    }

    @Test
    void elParametroProfesorDevuelveLaVistaDeProfesor() {
        assertThat(VistaPdf.desdeParametro("profesor")).contains(VistaPdf.PROFESOR);
    }

    /**
     * Un valor que no nombra ninguna vista no es una vista por defecto: es vacío. Si
     * cayera al defecto, pedir una vista mal escrita devolvería un PDF de otra cosa con
     * un 200 y nadie se enteraría.
     */
    @Test
    void elParametroAulaDevuelveLaVistaDeAula() {
        assertThat(VistaPdf.desdeParametro("aula")).contains(VistaPdf.AULA);
    }

    @Test
    void elParametroGuardiasDevuelveLaVistaDeGuardias() {
        assertThat(VistaPdf.desdeParametro("guardias")).contains(VistaPdf.GUARDIAS);
    }

    @Test
    void unParametroDesconocidoNoDevuelveNingunaVista() {
        assertThat(VistaPdf.desdeParametro("trimestre")).isEmpty();
    }

    /** {@code null} se trata como el valor inventado, y NO revienta con un NPE. */
    @Test
    void unParametroNuloNoDevuelveNingunaVistaYNoLanza() {
        assertThat(VistaPdf.desdeParametro(null)).isEmpty();
    }

    // ------------------------------------------------------------------ texto de entrada

    /**
     * El orden de la vista de profesor es ASIGNATURA, AULA y GRUPOS: es el del centro, y
     * NO el de la vista de grupo con las piezas cambiadas de sitio. Los dos grupos van
     * unidos por la barra, sin espacio, porque son un solo dato.
     */
    @Test
    void laEntradaDeProfesorVaEnOrdenAsignaturaAulaGrupos() {
        SesionVistaDTO sesion = sesion("DTec", List.of("DIB2"), "Taller 1 Aula Plástica",
                List.of("1B-A", "1B-B"));

        assertThat(PaginaPorRecurso.PROFESOR.textoDeEntrada(sesion))
                .isEqualTo("DTec Taller 1 Aula Plástica 1B-A/1B-B");
    }

    /**
     * (S201, p1) Sin aula —una reunión o una función— la entrada de profesor no lleva el
     * tramo del aula: ni un {@code "null"} ni un espacio de más.
     */
    @Test
    void laEntradaDeProfesorSinAulaNoLlevaElTramoDelAula() {
        SesionVistaDTO sesion = sesion("RED", List.of("P1", "P2"), null, List.of("1B-A"));

        String texto = PaginaPorRecurso.PROFESOR.textoDeEntrada(sesion);

        assertThat(texto).doesNotContain("null").endsWith("1B-A").isEqualTo("RED 1B-A");
    }

    /** El de grupo no se mueve: asignatura, profesor y aula, como desde S149. */
    @Test
    void laEntradaDeGrupoSigueEnOrdenAsignaturaProfesorAula() {
        SesionVistaDTO sesion = sesion("DTec", List.of("DIB2"), "Taller 1 Aula Plástica",
                List.of("1B-A", "1B-B"));

        assertThat(PaginaPorRecurso.GRUPO.textoDeEntrada(sesion))
                .isEqualTo("DTec DIB2 Taller 1 Aula Plástica");
    }

    /**
     * El orden de la vista de aula es ASIGNATURA, PROFESORES y GRUPOS, que es justo lo que
     * anuncia su clave de lectura. Con dos de cada uno: así un orden intercambiado no
     * puede colarse por parecerse.
     */
    @Test
    void laEntradaDeAulaVaEnOrdenAsignaturaProfesoresGrupos() {
        SesionVistaDTO sesion = sesion("LCL", List.of("LEN2", "LEN8"), "A5",
                List.of("2ºA", "2ºB"));

        assertThat(PaginaPorRecurso.AULA.textoDeEntrada(sesion)).isEqualTo("LCL LEN2/LEN8 2ºA/2ºB");
    }

    // ------------------------------------------------------------------ recursos y catálogo

    /** Solo la de aula imprime el catálogo entero; las otras dos, lo que tiene clases. */
    @Test
    void soloLaVistaDeAulaIncluyeRecursosSinSesiones() {
        assertThat(PaginaPorRecurso.GRUPO.incluyeRecursosSinSesiones()).isFalse();
        assertThat(PaginaPorRecurso.PROFESOR.incluyeRecursosSinSesiones()).isFalse();
        assertThat(PaginaPorRecurso.AULA.incluyeRecursosSinSesiones()).isTrue();
    }

    /**
     * Una sesión sin código de aula no aporta página en vez de abrir una titulada con un
     * hueco. Desde S201 pasa con datos reales: una reunión o una función sin aula no aparece
     * en el PDF de aula (p2 del contrato de S201).
     */
    @Test
    void enVistaDeAulaUnaSesionSinAulaNoAportaRecurso() {
        assertThat(PaginaPorRecurso.AULA.recursosDe(sesion("LCL", List.of("LEN2"), null, List.of("2ºA"))))
                .isEmpty();
        assertThat(PaginaPorRecurso.AULA.recursosDe(sesion("LCL", List.of("LEN2"), "  ", List.of("2ºA"))))
                .isEmpty();
        assertThat(PaginaPorRecurso.AULA.recursosDe(sesion("LCL", List.of("LEN2"), "A5", List.of("2ºA"))))
                .containsExactly("A5");
    }

    // ------------------------------------------------------------------ titulo

    @Test
    void elTituloDeProfesorLlevaSuCodigoYSuNombreSeparadosPorRaya() {
        ContextoPdf contexto = contexto(Map.of("DIB2", "Ramírez Soto, Ana"));

        assertThat(PaginaPorRecurso.PROFESOR.tituloDe("DIB2", contexto))
                .isEqualTo("DIB2 — Ramírez Soto, Ana");
    }

    /** Sin nombre en catálogo queda el código SOLO: ni raya suelta ni hueco detrás. */
    @Test
    void elTituloDeUnProfesorSinNombreEsSoloSuCodigo() {
        assertThat(PaginaPorRecurso.PROFESOR.tituloDe("DIB2", contexto(Map.of())))
                .isEqualTo("DIB2");
    }

    /** La página de un grupo se titula con su código tal cual, y el contexto no pinta. */
    @Test
    void elTituloDeGrupoEsSuCodigoTalCual() {
        assertThat(PaginaPorRecurso.GRUPO.tituloDe("1B-A", contexto(Map.of("DIB2", "Ramírez Soto, Ana"))))
                .isEqualTo("1B-A");
    }

    // ------------------------------------------------------------------ leyenda

    /**
     * La página de un profesor NO lleva bloque de profesores: el único que saldría es el
     * de la propia página, que ya está en el título.
     */
    @Test
    void laLeyendaDeProfesorTieneUnSoloBloqueYEsElDeAsignaturas() {
        List<SesionVistaDTO> sesiones = List.of(
                sesion("DTec", List.of("DIB2"), "Taller 1", List.of("1B-A")));

        assertThat(PaginaPorRecurso.PROFESOR.leyendaDe(sesiones, Map.of("DIB2", "Ramírez Soto, Ana")))
                .singleElement()
                .satisfies(bloque -> {
                    assertThat(bloque.encabezado()).isEqualTo("Asignaturas");
                    assertThat(bloque.lineas()).containsExactly("DTec — Dibujo Técnico");
                });
    }

    // ------------------------------------------------------------------ piezas

    private static ContextoPdf contexto(Map<String, String> nombres) {
        return new ContextoPdf(null, List.of(), nombres, Map.of());
    }

    private static SesionVistaDTO sesion(String asignatura, List<String> profesores,
                                         String aula, List<String> grupos) {
        return new SesionVistaDTO(1L, 0, 1, 1, 1, asignatura, "Dibujo Técnico",
                profesores, aula, List.of(), grupos, asignatura + "-ACT", asignatura + "-P1");
    }
}
