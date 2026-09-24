package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Lo que {@link VistaPdf} decide por su cuenta: la traducción del query param y las
 * piezas de texto que cada vista compone. La maqueta se prueba en {@code HorarioPdfTest},
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

        assertThat(VistaPdf.PROFESOR.textoDeEntrada(sesion))
                .isEqualTo("DTec Taller 1 Aula Plástica 1B-A/1B-B");
    }

    /** El de grupo no se mueve: asignatura, profesor y aula, como desde S149. */
    @Test
    void laEntradaDeGrupoSigueEnOrdenAsignaturaProfesorAula() {
        SesionVistaDTO sesion = sesion("DTec", List.of("DIB2"), "Taller 1 Aula Plástica",
                List.of("1B-A", "1B-B"));

        assertThat(VistaPdf.GRUPO.textoDeEntrada(sesion))
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

        assertThat(VistaPdf.AULA.textoDeEntrada(sesion)).isEqualTo("LCL LEN2/LEN8 2ºA/2ºB");
    }

    // ------------------------------------------------------------------ recursos y catálogo

    /** Solo la de aula imprime el catálogo entero; las otras dos, lo que tiene clases. */
    @Test
    void soloLaVistaDeAulaIncluyeRecursosSinSesiones() {
        assertThat(VistaPdf.GRUPO.incluyeRecursosSinSesiones()).isFalse();
        assertThat(VistaPdf.PROFESOR.incluyeRecursosSinSesiones()).isFalse();
        assertThat(VistaPdf.AULA.incluyeRecursosSinSesiones()).isTrue();
    }

    /**
     * Una sesión sin código de aula no aporta página en vez de abrir una titulada con un
     * hueco. No puede pasar con los datos del esquema, pero ésta es una función pura que
     * recibe DTOs de quien sea.
     */
    @Test
    void enVistaDeAulaUnaSesionSinAulaNoAportaRecurso() {
        assertThat(VistaPdf.AULA.recursosDe(sesion("LCL", List.of("LEN2"), null, List.of("2ºA"))))
                .isEmpty();
        assertThat(VistaPdf.AULA.recursosDe(sesion("LCL", List.of("LEN2"), "  ", List.of("2ºA"))))
                .isEmpty();
        assertThat(VistaPdf.AULA.recursosDe(sesion("LCL", List.of("LEN2"), "A5", List.of("2ºA"))))
                .containsExactly("A5");
    }

    // ------------------------------------------------------------------ titulo

    @Test
    void elTituloDeProfesorLlevaSuCodigoYSuNombreSeparadosPorRaya() {
        ContextoPdf contexto = contexto(Map.of("DIB2", "Ramírez Soto, Ana"));

        assertThat(VistaPdf.PROFESOR.tituloDe("DIB2", contexto))
                .isEqualTo("DIB2 — Ramírez Soto, Ana");
    }

    /** Sin nombre en catálogo queda el código SOLO: ni raya suelta ni hueco detrás. */
    @Test
    void elTituloDeUnProfesorSinNombreEsSoloSuCodigo() {
        assertThat(VistaPdf.PROFESOR.tituloDe("DIB2", contexto(Map.of())))
                .isEqualTo("DIB2");
    }

    /** La página de un grupo se titula con su código tal cual, y el contexto no pinta. */
    @Test
    void elTituloDeGrupoEsSuCodigoTalCual() {
        assertThat(VistaPdf.GRUPO.tituloDe("1B-A", contexto(Map.of("DIB2", "Ramírez Soto, Ana"))))
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

        assertThat(VistaPdf.PROFESOR.leyendaDe(sesiones, Map.of("DIB2", "Ramírez Soto, Ana")))
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
