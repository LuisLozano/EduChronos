package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import es.yaroki.educhronos.app.exportacion.VistaPdf.BloqueLeyenda;
import es.yaroki.educhronos.app.web.dto.GuardiaVistaDTO;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Las guardias en el PDF (S215, C-exportacion-guardias): en la página de cada profesor
 * (C1-C4) y en la página de guardias ({@link VistaPdf#GUARDIAS}, D2-D6 y enmiendas ED1, ED2,
 * ED4). JUnit puro, como {@code HorarioPdfTest}: entra una proyección construida a mano y se
 * afirma sobre el PDF RELEÍDO con el extractor de OpenPDF, o sobre el flujo de contenido
 * descomprimido cuando lo que se mide no es texto (los cuerpos).
 *
 * <p>El extractor colapsa las columnas: cada fila de la rejilla sale como una línea con lo
 * que haya en ella de lunes a viernes. Los asertos de celda usan filas donde solo hay un día
 * ocupado, para que el texto de la fila sea el de la celda.
 */
class HorarioPdfGuardiasTest {

    /** 3 lectivos, recreo, 3 lectivos: la jornada del banco de referencia. */
    private static final JornadaDTO JORNADA = new JornadaDTO(true, List.of(
            tramo(1, true, 1, "08:00", "09:00"),
            tramo(2, true, 2, "09:00", "10:00"),
            tramo(3, true, 3, "10:00", "11:00"),
            tramo(4, false, null, "11:00", "11:30"),
            tramo(5, true, 4, "11:30", "12:30"),
            tramo(6, true, 5, "12:30", "13:30"),
            tramo(7, true, 6, "13:30", "14:30")));

    private static final SesionVistaDTO MAT_LUNES_1 = new SesionVistaDTO(
            1L, 1, 1, 1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5",
            List.of("1ºA-Completo"), List.of("1ºA"), "MAT-1ºA", "MAT-1ºA-P1");

    private static final Map<String, String> NOMBRES = Map.of(
            "MAT1", "Profesora Dos", "GUA1", "Profesor Cuatro", "LEN1", "Profesor Uno",
            "NOAP", "Profesor Que No Aparece");

    // ------------------------------------------------------------------ C1, C2: entrada «Guardia»

    /**
     * C1 y C2: la guardia es una entrada «Guardia» en la celda de su profesor, DESPUÉS de las
     * sesiones de esa celda. El caso fuerza la coincidencia con una sesión (que la condición 2
     * impide en un horario real) porque es el único modo de ver el orden; y pone otra guardia
     * sola, en otra fila, para ver la entrada sin nada al lado.
     */
    @Test
    void enLaPaginaDelProfesorLaGuardiaVaEnSuCeldaTrasLasSesiones() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(
                        new GuardiaVistaDTO("MAT1", 1, 1), new GuardiaVistaDTO("MAT1", 1, 2))),
                VistaPdf.PROFESOR, contextoProfesor(List.of("MAT1")));

        PdfReader reader = new PdfReader(pdf);
        try {
            String pagina = normalizado(texto(reader, 1));
            assertThat(pagina).contains("08:00-09:00 MAT A5 1ºA Guardia 09:00-10:00");
            assertThat(pagina).contains("09:00-10:00 Guardia 10:00-11:00");
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ C3: página de quien solo tiene guardias

    /**
     * C3: un profesor SIN clases pero con guardias tiene página, en el orden del catálogo
     * (va antes que MAT1 porque el catálogo lo pone antes), con su entrada «Guardia» y sin
     * leyenda: no tiene asignaturas que traducir.
     */
    @Test
    void unProfesorConSoloGuardiasTienePaginaEnElOrdenDelCatalogo() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(new GuardiaVistaDTO("GUA1", 2, 5))),
                VistaPdf.PROFESOR, contextoProfesor(List.of("GUA1", "MAT1", "SIN1")));

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
            String gua = texto(reader, 1);
            assertThat(titulo(gua)).isEqualTo("GUA1 — Profesor Cuatro");
            assertThat(normalizado(gua)).contains("12:30-13:30 Guardia 13:30-14:30");
            assertThat(gua).doesNotContain("Asignaturas");
            assertThat(titulo(texto(reader, 2))).isEqualTo("MAT1 — Profesora Dos");
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ C4: sin leyenda para «Guardia»

    @Test
    void laLeyendaDelProfesorNoLlevaEntradaParaGuardia() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(new GuardiaVistaDTO("MAT1", 1, 2))),
                VistaPdf.PROFESOR, contextoProfesor(List.of("MAT1")));

        PdfReader reader = new PdfReader(pdf);
        try {
            String pagina = texto(reader, 1);
            String leyenda = pagina.substring(pagina.indexOf("Asignaturas"));
            assertThat(leyenda).contains("MAT — Matemáticas");
            assertThat(leyenda).doesNotContain("Guardia");
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ grupo y aula: las guardias no entran

    /** Las guardias no cambian ni una línea de las páginas de grupo y de aula. */
    @Test
    void lasPaginasDeGrupoYDeAulaNoCambianConGuardias() throws IOException {
        List<GuardiaVistaDTO> guardias = List.of(
                new GuardiaVistaDTO("MAT1", 1, 2), new GuardiaVistaDTO("GUA1", 1, 1));
        for (VistaPdf vista : List.of(VistaPdf.GRUPO, VistaPdf.AULA)) {
            ContextoPdf contexto = new ContextoPdf(JORNADA, List.of("1ºA", "A5"), NOMBRES, Map.of());
            assertThat(textos(HorarioPdf.escribir(proyeccion(List.of(MAT_LUNES_1), guardias), vista, contexto)))
                    .as("vista %s", vista)
                    .isEqualTo(textos(HorarioPdf.escribir(
                            proyeccion(List.of(MAT_LUNES_1), List.of()), vista, contexto)));
        }
    }

    // ------------------------------------------------------------------ D2, D6: la página de guardias

    /**
     * D2 y D6: UNA página A4 vertical, con el título «Guardias» (S221, X4; hasta S220
     * «Guardias ordinarias»), la clave «Profesores de guardia», la cabecera de días, la columna
     * de horas y el recreo; y solo los dos cuerpos de las demás vistas, 7,99 y 10.
     */
    @Test
    void laPaginaDeGuardiasEsUnaA4ConTituloClaveRejillaYLosCuerposDeSiempre() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(new GuardiaVistaDTO("MAT1", 1, 2))),
                VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            Rectangle hoja = reader.getPageSize(1);
            assertThat(hoja.getWidth()).isEqualTo(595f);
            assertThat(hoja.getHeight()).isEqualTo(842f);
            String[] lineas = texto(reader, 1).split("\\R");
            assertThat(lineas[0].trim()).isEqualTo("Guardias");
            assertThat(lineas[1].trim()).isEqualTo("Profesores de guardia");
            assertThat(texto(reader, 1)).contains(
                    "Lunes Martes Miércoles Jueves Viernes", "08:00-09:00", "13:30-14:30");
            assertThat(filas(texto(reader, 1))).as("banda de días: la hora y «Recreo» en dos líneas")
                    .contains("\n11:00-11:30\nRecreo\n11:30-12:30\n");
            assertThat(cuerpos(reader)).containsExactlyInAnyOrder("7.99", "10");
        } finally {
            reader.close();
        }
    }

    /** La página de guardias no pinta clases: la sesión del lunes a primera hora no sale. */
    @Test
    void laPaginaDeGuardiasNoPintaLasSesiones() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(new GuardiaVistaDTO("GUA1", 1, 2))),
                VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(texto(reader, 1)).doesNotContain("MAT A5", "Matemáticas");
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ D3: códigos ordenados

    /** D3: en la celda, los códigos ORDENADOS y unidos por «, », aunque lleguen desordenados. */
    @Test
    void enLaPaginaDeGuardiasCadaCeldaLlevaLosCodigosOrdenadosYUnidosPorComa() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(), List.of(
                        new GuardiaVistaDTO("MAT1", 1, 1),
                        new GuardiaVistaDTO("GUA1", 1, 1),
                        new GuardiaVistaDTO("LEN1", 1, 1))),
                VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(normalizado(texto(reader, 1)))
                    .contains("08:00-09:00 GUA1, LEN1, MAT1 09:00-10:00");
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ D4: leyenda

    /**
     * D4 y ED4: la leyenda trae a los profesores que APARECEN, con su nombre, y a nadie más
     * —NOAP está en el catálogo de nombres y no tiene guardia—; en tres bloques «Profesores».
     */
    @Test
    void laLeyendaDeLaPaginaDeGuardiasTraeSoloALosQueAparecenEnTresBloques() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(), List.of(
                        new GuardiaVistaDTO("MAT1", 1, 1),
                        new GuardiaVistaDTO("LEN1", 2, 3),
                        new GuardiaVistaDTO("GUA1", 3, 6))),
                VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            String pagina = texto(reader, 1);
            assertThat(pagina).contains(
                    "GUA1 — Profesor Cuatro", "LEN1 — Profesor Uno", "MAT1 — Profesora Dos");
            assertThat(pagina).doesNotContain("NOAP", "Profesor Que No Aparece");
            // La clave de lectura («Profesores de guardia») y los tres encabezados: cuatro.
            assertThat(pagina.split("Profesores", -1)).as("clave y tres encabezados").hasSize(5);
        } finally {
            reader.close();
        }
    }

    /**
     * ED4: tercios CONSECUTIVOS por código; con {@code n} profesores, los primeros
     * {@code n % 3} bloques llevan uno más. Un código sin nombre de catálogo sale solo.
     */
    @Test
    void laLeyendaDeGuardiasRepartePorCodigoEnTerciosConsecutivos() {
        List<GuardiaVistaDTO> siete = new ArrayList<>();
        for (String codigo : List.of("G7", "G1", "G4", "G2", "G6", "G3", "G5")) {
            siete.add(new GuardiaVistaDTO(codigo, 1, 1));
        }
        siete.add(new GuardiaVistaDTO("G1", 2, 2));

        List<BloqueLeyenda> bloques = HorarioPdf.leyendaDeGuardias(siete, Map.of("G1", "Uno"));

        assertThat(bloques).extracting(BloqueLeyenda::encabezado)
                .containsExactly("Profesores", "Profesores", "Profesores");
        assertThat(bloques).extracting(BloqueLeyenda::lineas).containsExactly(
                List.of("G1 — Uno", "G2", "G3"), List.of("G4", "G5"), List.of("G6", "G7"));
        assertThat(HorarioPdf.leyendaDeGuardias(
                List.of(new GuardiaVistaDTO("G1", 1, 1), new GuardiaVistaDTO("G2", 1, 1)), Map.of()))
                .extracting(BloqueLeyenda::lineas)
                .as("con dos profesores no se crea un tercer bloque vacío")
                .containsExactly(List.of("G1"), List.of("G2"));
        assertThat(HorarioPdf.leyendaDeGuardias(List.of(), Map.of())).isEmpty();
    }

    // ------------------------------------------------------------------ D5: sin guardias

    /** D5: sin guardias la página sale igual —una, con la rejilla— vacía y sin leyenda. */
    @Test
    void sinGuardiasLaPaginaSaleConLaRejillaVaciaYSinLeyenda() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of()), VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String pagina = texto(reader, 1);
            assertThat(titulo(pagina)).isEqualTo("Guardias");
            assertThat(pagina).contains("Lunes", "08:00-09:00");
            assertThat(filas(pagina)).contains("\n11:00-11:30\nRecreo\n11:30-12:30\n");
            assertThat(pagina).doesNotContain("Profesores —", "—");
            assertThat(pagina.split("Profesores", -1)).as("solo la clave de lectura").hasSize(2);
        } finally {
            reader.close();
        }
    }

    // ------------------------------------------------------------------ S221: guardias de recreo

    /** Jornada con DOS recreos el lunes: 10:00-10:30 (recreo 1) y 11:30-12:00 (recreo 2). */
    private static final JornadaDTO JORNADA_DOS_RECREOS = new JornadaDTO(true, List.of(
            tramo(1, true, 1, "08:00", "09:00"),
            tramo(2, true, 2, "09:00", "10:00"),
            tramo(3, false, null, "10:00", "10:30"),
            tramo(4, true, 3, "10:30", "11:30"),
            tramo(5, false, null, "11:30", "12:00"),
            tramo(6, true, 4, "12:00", "13:00")));

    private static GuardiaVistaDTO recreo(String profesor, int dia, int recreo) {
        return new GuardiaVistaDTO(profesor, dia, null, recreo, GuardiaVistaDTO.RECREO);
    }

    private static GuardiaVistaDTO biblioteca(String profesor, int dia, int recreo) {
        return new GuardiaVistaDTO(profesor, dia, null, recreo, GuardiaVistaDTO.BIBLIOTECA);
    }

    /**
     * D5 (S221): en la página de un profesor, el recreo es una banda de días: la hora y «Recreo»
     * en la columna de horas y, en la celda del día, «Guardia» por su guardia de RECREO y
     * «Biblioteca» por la de BIBLIOTECA. Con dos recreos, cada guardia va en la banda de SU
     * recreo: la de biblioteca del martes en el segundo, la de recreo del lunes en el primero.
     */
    @Test
    void enLaPaginaDelProfesorCadaGuardiaDeRecreoVaEnLaBandaDeSuRecreoYSuDia() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(MAT_LUNES_1), List.of(recreo("MAT1", 1, 1), biblioteca("MAT1", 2, 2))),
                VistaPdf.PROFESOR, new ContextoPdf(JORNADA_DOS_RECREOS, List.of("MAT1"), NOMBRES, Map.of()));

        PdfReader reader = new PdfReader(pdf);
        try {
            String pagina = texto(reader, 1);
            assertThat(filas(pagina)).contains("\n10:00-10:30\nRecreo\nGuardia\n10:30-11:30\n");
            assertThat(filas(pagina)).contains("\n11:30-12:00\nRecreo\nBiblioteca\n12:00-13:00\n");
            assertThat(pagina.split("Guardia", -1)).as("una sola «Guardia»").hasSize(2);
            assertThat(pagina.split("Biblioteca", -1)).as("una sola «Biblioteca»").hasSize(2);
        } finally {
            reader.close();
        }
    }

    /**
     * D5 (S221): un profesor con SOLO guardias de recreo tiene página, con su banda; y la
     * leyenda de la página de guardias lo nombra.
     */
    @Test
    void quienSoloTieneGuardiasDeRecreoTienePaginaYEstaEnLaLeyendaDeGuardias() throws IOException {
        HorarioProyeccionDTO proyeccion = proyeccion(List.of(MAT_LUNES_1), List.of(recreo("GUA1", 3, 1)));

        PdfReader reader = new PdfReader(HorarioPdf.escribir(
                proyeccion, VistaPdf.PROFESOR, contextoProfesor(List.of("GUA1", "MAT1"))));
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
            assertThat(titulo(texto(reader, 1))).isEqualTo("GUA1 — Profesor Cuatro");
            assertThat(filas(texto(reader, 1))).contains("\n11:00-11:30\nRecreo\nGuardia\n11:30-12:30\n");
        } finally {
            reader.close();
        }
        assertThat(texto(new PdfReader(HorarioPdf.escribir(proyeccion, VistaPdf.GUARDIAS, contextoGuardias())), 1))
                .contains("GUA1 — Profesor Cuatro");
    }

    /**
     * D5 y X3 (S221): en la página de guardias, la celda del recreo de un día lleva los códigos
     * de RECREO ordenados y unidos por «, » y, en línea aparte, «Biblioteca: COD». El código de
     * biblioteca NO entra en la lista de recreo.
     */
    @Test
    void enLaPaginaDeGuardiasElRecreoLlevaLosCodigosYAparteLaBiblioteca() throws IOException {
        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(), List.of(
                        recreo("MAT1", 1, 1), recreo("GUA1", 1, 1), biblioteca("LEN1", 1, 1))),
                VistaPdf.GUARDIAS, contextoGuardias());

        PdfReader reader = new PdfReader(pdf);
        try {
            String pagina = texto(reader, 1);
            assertThat(filas(pagina)).contains("\n11:00-11:30\nRecreo\nGUA1, MAT1\nBiblioteca: LEN1\n11:30-12:30\n");
            assertThat(pagina).doesNotContain("GUA1, LEN1", "LEN1, MAT1");
        } finally {
            reader.close();
        }
    }

    /**
     * D5 (S221): con dos recreos, la página de guardias pinta cada código en la banda de su
     * recreo, y las de grupo y aula siguen con la banda única de siempre, una por recreo: la
     * hora y «Recreo» en la MISMA línea.
     */
    @Test
    void conDosRecreosGuardiasVaPorBandaYGrupoYAulaSiguenConLaBandaUnica() throws IOException {
        HorarioProyeccionDTO proyeccion = proyeccion(List.of(MAT_LUNES_1), List.of(
                recreo("GUA1", 2, 1), recreo("LEN1", 2, 2)));

        String guardias = texto(new PdfReader(HorarioPdf.escribir(proyeccion, VistaPdf.GUARDIAS,
                new ContextoPdf(JORNADA_DOS_RECREOS, List.of(), NOMBRES, Map.of()))), 1);
        assertThat(filas(guardias)).contains(
                "\n10:00-10:30\nRecreo\nGUA1\n10:30-11:30\n", "\n11:30-12:00\nRecreo\nLEN1\n12:00-13:00\n");

        for (VistaPdf vista : List.of(VistaPdf.GRUPO, VistaPdf.AULA)) {
            String pagina = texto(new PdfReader(HorarioPdf.escribir(proyeccion, vista,
                    new ContextoPdf(JORNADA_DOS_RECREOS, List.of("1ºA", "A5"), NOMBRES, Map.of()))), 1);
            assertThat(filas(pagina)).as("vista %s", vista)
                    .contains("\n10:00-10:30 Recreo\n10:30-11:30", "\n11:30-12:00 Recreo\n12:00-13:00");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static HorarioProyeccionDTO proyeccion(List<SesionVistaDTO> sesiones,
                                                   List<GuardiaVistaDTO> guardias) {
        return new HorarioProyeccionDTO(
                1L, "Horario con guardias", "BORRADOR", "FEASIBLE", 0.0, 0.0,
                "2026-10-09T00:00:00Z", sesiones, guardias);
    }

    private static ContextoPdf contextoProfesor(List<String> orden) {
        return new ContextoPdf(JORNADA, orden, NOMBRES, Map.of());
    }

    private static ContextoPdf contextoGuardias() {
        return new ContextoPdf(JORNADA, List.of(), NOMBRES, Map.of());
    }

    private static String texto(PdfReader reader, int pagina) throws IOException {
        return new PdfTextExtractor(reader).getTextFromPage(pagina);
    }

    /** El texto de todas las páginas, una por elemento. */
    private static List<String> textos(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            List<String> paginas = new ArrayList<>();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                paginas.add(texto(reader, i));
            }
            return paginas;
        } finally {
            reader.close();
        }
    }

    private static String titulo(String pagina) {
        return pagina.split("\\R")[0].trim();
    }

    /**
     * Las líneas del texto extraído, cada una sin los blancos de los bordes, unidas por un salto
     * (S221): el extractor antepone un espacio a cada línea salvo la primera, y los asertos de
     * las bandas de recreo comparan líneas seguidas.
     */
    private static String filas(String texto) {
        return String.join("\n", texto.lines().map(String::trim).toList());
    }

    private static String normalizado(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }

    /** Los cuerpos distintos de los operadores {@code Tf} del flujo de la página 1. */
    private static Set<String> cuerpos(PdfReader reader) throws IOException {
        String flujo = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        Set<String> cuerpos = new TreeSet<>();
        Matcher m = Pattern.compile("/\\S+ ([\\d.]+) Tf").matcher(flujo);
        while (m.find()) {
            cuerpos.add(m.group(1));
        }
        return cuerpos;
    }

    private static TramoJornadaDTO tramo(int orden, boolean lectivo, Integer ordenEnDia,
                                         String inicio, String fin) {
        return new TramoJornadaDTO("LUNES", inicio, fin, lectivo, orden, ordenEnDia);
    }
}
