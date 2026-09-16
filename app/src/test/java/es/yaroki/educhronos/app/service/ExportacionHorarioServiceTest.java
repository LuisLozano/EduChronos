package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import es.yaroki.educhronos.app.exportacion.VistaPdf;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.ProfesorDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import es.yaroki.educhronos.app.web.dto.TutoriaDTO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Casos de {@link ExportacionHorarioService} (S149, C-exportacion-pdf-grupo), la clase que
 * compone las cinco fuentes del PDF. Con DOBLES de las cinco: aquí no se prueba ni el
 * aplanado, ni la jornada, ni la maqueta —cada uno tiene los suyos—, sino el CRUCE, que es
 * lo único que vive en esta clase y lo que la campaña de mutación de M3-3 dejó al
 * descubierto (el mutante 3, «el tutor sale con su código en vez de su nombre», sobrevivía
 * porque esta clase no tenía ni un test).
 *
 * <p>Los asertos se hacen sobre el PDF resultante releído, y no sobre llamadas al
 * serializador: lo que importa es que el dato cruzado llegue a la página. Un
 * {@code verify} de que se pasó tal mapa no distinguiría un mapa bien construido de uno
 * con los valores equivocados.
 *
 * <p>Lo que NO se prueba aquí y no es olvido: el parámetro {@code vista} no existe en esta
 * clase —lo valida el controlador antes de llamarla, y lo cubre
 * {@code HorarioControllerHttpTest.getPdf_conVistaDesconocida_devuelve400YNoLlegaAProyectar},
 * que además verifica que este servicio no llega a ser invocado—.
 */
@ExtendWith(MockitoExtension.class)
class ExportacionHorarioServiceTest {

    private static final JornadaDTO JORNADA = new JornadaDTO(true, List.of(
            new TramoJornadaDTO("LUNES", "08:00", "09:00", true, 1, 1),
            new TramoJornadaDTO("LUNES", "09:00", "09:30", false, 2, null)));

    @Mock private GeneradorHorarioService generador;
    @Mock private JornadaService jornadaService;
    @Mock private ProfesorService profesorService;
    @Mock private GrupoService grupoService;
    @Mock private TutoriaService tutoriaService;

    @InjectMocks private ExportacionHorarioService servicio;

    /**
     * EL CRUCE QUE MATA AL MUTANTE 3: el {@code TutoriaDTO} trae el CÓDIGO del profesor y la
     * página tiene que imprimir su NOMBRE. Se afirman las dos mitades —que está el nombre y
     * que NO está el código— porque un servicio que pasara el código sin resolver seguiría
     * imprimiendo una línea de tutor perfectamente plausible.
     */
    @Test
    void elTutorSeImprimeConSuNombreDeCatalogoYNoConSuCodigo() throws IOException {
        cablearUnGrupo("1ºA", 10L);
        when(profesorService.listar()).thenReturn(
                List.of(new ProfesorDTO(7L, "MAT1", "Macías Magro, Sonia")));
        when(tutoriaService.obtener(10L)).thenReturn(
                List.of(new TutoriaDTO("MAT1", "TUTOR_PRINCIPAL")));

        String pagina = texto(servicio.pdf(1L, VistaPdf.GRUPO));

        assertThat(pagina).contains("Tutor: Macías Magro, Sonia");
        assertThat(pagina).doesNotContain("Tutor: MAT1");
    }

    /**
     * El mismo mapa de nombres alimenta las dos cosas: la línea de tutor y la leyenda. Es
     * una propiedad de diseño, no una coincidencia —hay UNA fuente para el nombre de una
     * persona en la página—, y se comprueba con un único profesor que es a la vez tutor y
     * docente: su nombre tiene que salir en los dos sitios.
     */
    @Test
    void elMismoMapaDeNombresAlimentaElTutorYLaLeyenda() throws IOException {
        cablearUnGrupo("1ºA", 10L);
        when(profesorService.listar()).thenReturn(
                List.of(new ProfesorDTO(7L, "MAT1", "Macías Magro, Sonia")));
        when(tutoriaService.obtener(10L)).thenReturn(
                List.of(new TutoriaDTO("MAT1", "TUTOR_PRINCIPAL")));

        String pagina = texto(servicio.pdf(1L, VistaPdf.GRUPO));

        assertThat(pagina).contains("Tutor: Macías Magro, Sonia");
        assertThat(pagina).contains("MAT1 — Macías Magro, Sonia");
    }

    /**
     * Un grupo SIN tutoría no aporta entrada al mapa, y su página sale sin la línea. El
     * caso monta DOS grupos —uno con tutor y otro sin— en el mismo documento: así el aserto
     * distingue «no se imprime para quien no tiene» de «no se imprime nunca», que un
     * documento de una sola página confundiría.
     */
    @Test
    void unGrupoSinTutoriaNoAportaEntradaYSuPaginaSaleSinLinea() throws IOException {
        when(generador.proyectar(1L)).thenReturn(proyeccion(List.of(
                sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA"),
                sesion(1, 1, "LEN", "Lengua", List.of("LEN1"), "A6", "1ºB"))));
        when(jornadaService.obtenerJornada()).thenReturn(JORNADA);
        when(grupoService.listar()).thenReturn(List.of(
                new GrupoDTO(10L, "1ºA", "ESO1", "ORDINARIO"),
                new GrupoDTO(11L, "1ºB", "ESO1", "ORDINARIO")));
        when(profesorService.listar()).thenReturn(
                List.of(new ProfesorDTO(7L, "MAT1", "Macías Magro, Sonia")));
        when(tutoriaService.obtener(10L)).thenReturn(
                List.of(new TutoriaDTO("MAT1", "TUTOR_PRINCIPAL")));
        when(tutoriaService.obtener(11L)).thenReturn(List.of());

        byte[] pdf = servicio.pdf(1L, VistaPdf.GRUPO);

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(new PdfTextExtractor(reader).getTextFromPage(1))
                .contains("Tutor: Macías Magro, Sonia");
        assertThat(new PdfTextExtractor(reader).getTextFromPage(2))
                .as("el grupo sin tutoría no lleva ni el rótulo")
                .doesNotContain("Tutor");
        reader.close();
    }

    /**
     * Solo el {@code TUTOR_PRINCIPAL} da la línea: un {@code CO_TUTOR} no es el tutor del
     * grupo (I4 los distingue). El co-tutor va PRIMERO en la lista de este caso para que el
     * aserto no lo pueda pasar un código que se quede con el primer elemento.
     */
    @Test
    void entreVariasTutoriasSoloElPrincipalDaLaLinea() throws IOException {
        cablearUnGrupo("1ºA", 10L);
        when(profesorService.listar()).thenReturn(List.of(
                new ProfesorDTO(7L, "MAT1", "Macías Magro, Sonia"),
                new ProfesorDTO(8L, "LEN1", "Crespo Saborido, Ana María")));
        when(tutoriaService.obtener(10L)).thenReturn(List.of(
                new TutoriaDTO("LEN1", "CO_TUTOR"),
                new TutoriaDTO("MAT1", "TUTOR_PRINCIPAL")));

        String pagina = texto(servicio.pdf(1L, VistaPdf.GRUPO));

        assertThat(pagina).contains("Tutor: Macías Magro, Sonia");
        assertThat(pagina).doesNotContain("Tutor: Crespo Saborido, Ana María");
    }

    /**
     * El horario se obtiene COMPONIENDO {@code proyectar(id)} y nada más. El
     * {@code verifyNoMoreInteractions} es el aserto de verdad: la prohibición del Cambio es
     * no abrir una segunda vía a las sesiones —{@code horario.getSesiones()} o un tercer
     * mapeo desde {@code Sesion}—, y cualquiera de esas necesitaría otra llamada a este
     * colaborador, que es el único que sabe llegar al horario.
     */
    @Test
    void componeProyectarYNoAbreOtraViaALasSesiones() {
        cablearUnGrupo("1ºA", 10L);
        when(profesorService.listar()).thenReturn(List.of());
        when(tutoriaService.obtener(anyLong())).thenReturn(List.of());

        servicio.pdf(1L, VistaPdf.GRUPO);

        verify(generador).proyectar(1L);
        verifyNoMoreInteractions(generador);
    }

    // ------------------------------------------------------------------ utilidades

    /** Cablea el caso mínimo de un solo grupo con una clase. */
    private void cablearUnGrupo(String codigo, Long idGrupo) {
        when(generador.proyectar(1L)).thenReturn(proyeccion(List.of(
                sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", codigo))));
        when(jornadaService.obtenerJornada()).thenReturn(JORNADA);
        when(grupoService.listar()).thenReturn(
                List.of(new GrupoDTO(idGrupo, codigo, "ESO1", "ORDINARIO")));
    }

    private static String texto(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    private static HorarioProyeccionDTO proyeccion(List<SesionVistaDTO> sesiones) {
        return new HorarioProyeccionDTO(
                1L, "Horario de prueba", "BORRADOR", "FEASIBLE", 0.0, 0.0,
                "2026-09-15T00:00:00Z", sesiones);
    }

    private static SesionVistaDTO sesion(int dia, int tramo, String codigo, String nombre,
                                         List<String> profesores, String aula, String grupo) {
        return new SesionVistaDTO(
                (long) codigo.hashCode(), 0, dia, tramo, codigo, nombre, profesores, aula,
                List.of(), List.of(grupo), codigo + "-ACT", codigo + "-P1");
    }
}
