package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Lo que un horario SIN guardias exporta hoy, fijado ANTES de que las guardias entren en la
 * exportación (S215, C-exportacion-guardias, E1 del T2): B3 para el CSV y C5 para el PDF de
 * profesor. Son tests de caracterización: el valor esperado es la salida literal del código
 * de 3a51608, y su papel es que añadir las guardias no mueva un byte del CSV ni una línea
 * del PDF de un horario que no las tiene.
 *
 * <p>El montaje mezcla a propósito los casos que más fácilmente se moverían: un bloque de
 * dos tramos, una co-docencia con dos grupos, una reunión sin aula ni grupos (S201) y un
 * campo con el separador dentro, que obliga a entrecomillar.
 */
class ExportacionSinGuardiasTest {

    /** 3 lectivos, recreo, 3 lectivos: la jornada del banco de referencia. */
    private static final JornadaDTO JORNADA = new JornadaDTO(true, List.of(
            tramo(1, true, 1, "08:00", "09:00"),
            tramo(2, true, 2, "09:00", "10:00"),
            tramo(3, true, 3, "10:00", "11:00"),
            tramo(4, false, null, "11:00", "11:30"),
            tramo(5, true, 4, "11:30", "12:30"),
            tramo(6, true, 5, "12:30", "13:30"),
            tramo(7, true, 6, "13:30", "14:30")));

    private static final List<SesionVistaDTO> SESIONES = List.of(
            new SesionVistaDTO(11L, 1, 1, 1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5",
                    List.of("1ºA-Completo"), List.of("1ºA"), "MAT-1ºA", "MAT-1ºA-P1"),
            new SesionVistaDTO(12L, 1, 1, 2, 2, "TEC", "Tecnología", List.of("TEC1"), "Taller 1",
                    List.of("1ºA-Completo"), List.of("1ºA"), "TEC-1ºA", "TEC-1ºA-P1"),
            new SesionVistaDTO(13L, 2, 2, 4, 1, "LEN", "Lengua; Literatura", List.of("LEN1", "MAT1"),
                    "A6", List.of("1ºA-s1", "1ºB-s1"), List.of("1ºA", "1ºB"), "LEN-1º", "LEN-1º-P1"),
            new SesionVistaDTO(14L, 1, 3, 6, 1, "RED", "Reunión de departamento", List.of("LEN1"),
                    null, List.of(), List.of(), "RED-DEP", "RED-DEP-P1"));

    private static final Map<String, String> NOMBRES = Map.of(
            "LEN1", "Profesor Uno", "MAT1", "Profesora Dos", "TEC1", "Profesor Tres");

    // ------------------------------------------------------------------ B3

    @Test
    void elCsvDeUnHorarioSinGuardiasEsElDeHoyByteAByte() {
        String esperado = "﻿"
                + "Día;Tramo;Asignatura;Nombre asignatura;Profesores;Aula;Grupos;Subgrupos;"
                + "Actividad;Plaza;Índice;Sesión\r\n"
                + "1;1;MAT;Matemáticas;MAT1;A5;1ºA;1ºA-Completo;MAT-1ºA;MAT-1ºA-P1;1;11\r\n"
                + "1;2;TEC;Tecnología;TEC1;Taller 1;1ºA;1ºA-Completo;TEC-1ºA;TEC-1ºA-P1;1;12\r\n"
                + "1;3;TEC;Tecnología;TEC1;Taller 1;1ºA;1ºA-Completo;TEC-1ºA;TEC-1ºA-P1;1;12\r\n"
                + "2;4;LEN;\"Lengua; Literatura\";LEN1/MAT1;A6;1ºA/1ºB;1ºA-s1/1ºB-s1;LEN-1º;LEN-1º-P1;2;13\r\n"
                + "3;6;RED;Reunión de departamento;LEN1;;;;RED-DEP;RED-DEP-P1;1;14\r\n";

        byte[] csv = HorarioCsv.escribir(proyeccion());

        assertThat(csv).containsExactly(esperado.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ C5

    @Test
    void elPdfDeProfesorDeUnHorarioSinGuardiasEsElDeHoy() throws IOException {
        byte[] pdf = HorarioPdf.escribir(proyeccion(), VistaPdf.PROFESOR, new ContextoPdf(
                JORNADA, List.of("LEN1", "MAT1", "TEC1", "SIN1"), NOMBRES, Map.of("MAT1", "1ºA")));

        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(3);
            assertThat(texto(reader, 1)).isEqualTo(PAGINA_LEN1);
            assertThat(texto(reader, 2)).isEqualTo(PAGINA_MAT1);
            assertThat(texto(reader, 3)).isEqualTo(PAGINA_TEC1);
        } finally {
            reader.close();
        }
    }

    /*
     * El texto de cada página tal como lo devuelve hoy PdfTextExtractor (3a51608): el
     * extractor colapsa las columnas, así que cada fila de la rejilla es una línea con lo que
     * haya en ella de lunes a viernes. SIN1 está en el catálogo y no tiene clases: no tiene
     * página. La reunión sin aula sale como su asignatura sola.
     */
    private static final String PAGINA_LEN1 =
            "LEN1 — Profesor Uno\n"
            + " Asignatura - Aula - Grupo\n"
            + " Lunes Martes Miércoles Jueves Viernes\n"
            + " 08:00-09:00\n"
            + " 09:00-10:00\n"
            + " 10:00-11:00\n"
            + " 11:00-11:30 Recreo\n"
            + " 11:30-12:30 LEN A6 1ºA/1ºB\n"
            + " 12:30-13:30\n"
            + " 13:30-14:30 RED\n"
            + " Asignaturas\n"
            + " LEN — Lengua; Literatura\n"
            + " RED — Reunión de departamento";
    private static final String PAGINA_MAT1 =
            "MAT1 — Profesora Dos\n"
            + " Tutor de: 1ºA\n"
            + " Asignatura - Aula - Grupo\n"
            + " Lunes Martes Miércoles Jueves Viernes\n"
            + " 08:00-09:00 MAT A5 1ºA\n"
            + " 09:00-10:00\n"
            + " 10:00-11:00\n"
            + " 11:00-11:30 Recreo\n"
            + " 11:30-12:30 LEN A6 1ºA/1ºB\n"
            + " 12:30-13:30\n"
            + " 13:30-14:30\n"
            + " Asignaturas\n"
            + " LEN — Lengua; Literatura\n"
            + " MAT — Matemáticas";
    private static final String PAGINA_TEC1 =
            "TEC1 — Profesor Tres\n"
            + " Asignatura - Aula - Grupo\n"
            + " Lunes Martes Miércoles Jueves Viernes\n"
            + " 08:00-09:00\n"
            + " 09:00-10:00 TEC Taller 1 1ºA\n"
            + " 10:00-11:00 TEC Taller 1 1ºA\n"
            + " 11:00-11:30 Recreo\n"
            + " 11:30-12:30\n"
            + " 12:30-13:30\n"
            + " 13:30-14:30\n"
            + " Asignaturas\n"
            + " TEC — Tecnología";

    // ------------------------------------------------------------------ utilidades

    /** La proyección del montaje. Es el ÚNICO sitio de esta clase que construye el DTO. */
    private static HorarioProyeccionDTO proyeccion() {
        return new HorarioProyeccionDTO(
                1L, "Horario sin guardias", "BORRADOR", "FEASIBLE", 0.0, 0.0,
                "2026-10-09T00:00:00Z", SESIONES, List.of());
    }

    private static String texto(PdfReader reader, int pagina) throws IOException {
        return new PdfTextExtractor(reader).getTextFromPage(pagina);
    }

    private static TramoJornadaDTO tramo(int orden, boolean lectivo, Integer ordenEnDia,
                                         String inicio, String fin) {
        return new TramoJornadaDTO("LUNES", inicio, fin, lectivo, orden, ordenEnDia);
    }
}
