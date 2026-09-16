package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Casos de {@link HorarioPdf} (S149, C-exportacion-pdf-grupo). JUnit puro, sin Spring y
 * sin base de datos: la función recibe la proyección y un {@link ContextoPdf} construidos
 * a mano y devuelve bytes.
 *
 * <p>Los asertos NO se hacen sobre los bytes crudos sino sobre el PDF RELEÍDO con
 * {@link PdfReader} y {@link PdfTextExtractor}, que vienen en el mismo OpenPDF. La
 * diferencia importa: buscar una subcadena en el fichero probaría que los caracteres
 * están escritos en algún sitio, no que salgan en la página ni en qué página. Releer es
 * lo más cerca que se puede estar de lo que ve quien lo imprime sin mirar el papel.
 *
 * <p>El extractor colapsa la maquetación a texto plano: sirve para afirmar QUÉ se dice y
 * en qué página, no dónde cae en la hoja. Lo que no es texto —los sombreados— se afirma
 * sobre el FLUJO DE CONTENIDO ya descomprimido. La geometría fina se mide en la corrida
 * real (M3-2), no aquí.
 */
class HorarioPdfTest {

    /**
     * Los dos grises tal como el PDF los escribe. NO son "0.88" y "0.93" literales:
     * {@code java.awt.Color} guarda cada canal en 8 bits, así que 0,88 se cuantiza a
     * 224/255 = 0,87843 y 0,93 a 237/255 = 0,92941. Escribir aquí el número redondo daría
     * un test que no puede pasar nunca; escribir el cuantizado ata el aserto a lo que de
     * verdad va al fichero.
     */
    private static final String GRIS_RECREO = "0.87843 0.87843 0.87843 rg";
    private static final String GRIS_BANDA = "0.92941 0.92941 0.92941 rg";

    /** La jornada del banco de referencia: 3 lectivos, recreo, 3 lectivos. */
    private static final JornadaDTO JORNADA = new JornadaDTO(true, List.of(
            tramo(1, true, 1, "08:00", "09:00"),
            tramo(2, true, 2, "09:00", "10:00"),
            tramo(3, true, 3, "10:00", "11:00"),
            tramo(4, false, null, "11:00", "11:30"),
            tramo(5, true, 4, "11:30", "12:30"),
            tramo(6, true, 5, "12:30", "13:30"),
            tramo(7, true, 6, "13:30", "14:30")));

    // ------------------------------------------------------------------ C1: páginas y orden

    /**
     * El orden de las páginas lo manda el CATÁLOGO, no la aparición en las sesiones. El
     * caso enfrenta las dos vías a propósito: la proyección presenta {@code 2ºZ} primero
     * —es la sesión del lunes a primera hora— y el catálogo dice {@code 1ºA} primero. Con
     * el orden antiguo este aserto cae, que es justamente el cambio que se está fijando.
     */
    @Test
    void elOrdenDeLasPaginasEsElDelCatalogoYNoElDeAparicion() throws IOException {
        SesionVistaDTO apareceAntes = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "2ºZ");
        SesionVistaDTO apareceDespues = sesion(1, 2, "LEN", "Lengua", List.of("LEN1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(apareceAntes, apareceDespues)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA", "2ºZ"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(titulo(reader, 1)).isEqualTo("1ºA");
        assertThat(titulo(reader, 2)).isEqualTo("2ºZ");
        reader.close();
    }

    /**
     * Un grupo del horario que NO esté en el catálogo no se pierde: se imprime al final.
     * Callar una página sería peor que descolocarla —el grupo existe y tiene clases—, y
     * este aserto es el que impide que un filtro «solo lo que esté en el catálogo» entre
     * de tapadillo.
     */
    @Test
    void unGrupoAusenteDelCatalogoSeImprimeAlFinalYNoSePierde() throws IOException {
        SesionVistaDTO conocido = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");
        SesionVistaDTO desconocido = sesion(1, 2, "LEN", "Lengua", List.of("LEN1"), "A5", "9ºX");

        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(desconocido, conocido)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(titulo(reader, 1)).isEqualTo("1ºA");
        assertThat(titulo(reader, 2)).isEqualTo("9ºX");
        reader.close();
    }

    /** Un grupo que comparte plaza con otro sale en LAS DOS páginas, no solo en la primera. */
    @Test
    void unaPlazaDeDosGruposApareceEnLasDosPaginas() throws IOException {
        SesionVistaDTO compartida = new SesionVistaDTO(
                1L, 0, 1, 1, "EF", "Educación Física", List.of("EFI2"), "Gimnasio",
                List.of(), List.of("1ºA", "1ºB"), "EF-1", "EF-1-P1");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(compartida)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA", "1ºB"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        assertThat(texto(reader, 1)).contains("EF EFI2 Gimnasio");
        assertThat(texto(reader, 2)).contains("EF EFI2 Gimnasio");
        reader.close();
    }

    // ------------------------------------------------------------------ C2: cabecera de página

    /**
     * Con tutor, la línea va bajo el título y lleva el NOMBRE, no el código: el mapa que
     * entra ya trae el nombre resuelto.
     */
    @Test
    void conTutorLaPaginaLlevaSuLineaConElNombreCompleto() throws IOException {
        SesionVistaDTO sesion = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sesion)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(),
                        Map.of("1ºA", "Macías Magro, Sonia")));

        PdfReader reader = new PdfReader(pdf);
        assertThat(texto(reader, 1)).contains("Tutor: Macías Magro, Sonia");
        reader.close();
    }

    /**
     * Sin tutor NO se imprime la línea: ni «Tutor:» a secas, ni «sin tutor», ni un hueco.
     * Se afirma la AUSENCIA DEL RÓTULO entero —{@code "Tutor"}— y no solo la del nombre:
     * un {@code "Tutor: "} vacío pasaría un aserto que solo mirase el nombre, y es
     * exactamente el fallo que este caso existe para impedir.
     */
    @Test
    void sinTutorLaPaginaNoLlevaNiElRotuloDeTutor() throws IOException {
        SesionVistaDTO sesion = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sesion)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        assertThat(texto(reader, 1)).doesNotContain("Tutor");
        reader.close();
    }

    /**
     * La clave de lectura va en CADA página, no solo en la primera: cada hoja de un
     * horario se reparte por separado, y la segunda no puede depender de la primera para
     * ser legible.
     */
    @Test
    void laClaveDeLecturaVaEnTodasLasPaginas() throws IOException {
        SesionVistaDTO uno = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");
        SesionVistaDTO otro = sesion(1, 2, "LEN", "Lengua", List.of("LEN1"), "A5", "1ºB");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(uno, otro)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA", "1ºB"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        assertThat(texto(reader, 1)).contains("Asignatura - Profesor - Aula");
        assertThat(texto(reader, 2)).contains("Asignatura - Profesor - Aula");
        reader.close();
    }

    // ------------------------------------------------------------------ C3: la entrada

    /**
     * La entrada es {@code "<asignatura> <profesores/> <aula>"} y el AULA CON ESPACIOS
     * sale íntegra. Este es el caso que rompe cualquier maquetador que parta el texto por
     * espacios para tratarlo como tres campos: {@code "Taller 1 Aula Plástica"} tiene tres
     * espacios dentro y es el código de aula más largo del banco.
     */
    @Test
    void laEntradaLlevaElAulaConEspaciosIntegraYLosProfesoresUnidosPorBarra() throws IOException {
        SesionVistaDTO conAulaLarga = sesion(
                1, 1, "EXPRE", "Expresión", List.of("DIB1"), "Taller 1 Aula Plástica", "4ºA");
        SesionVistaDTO coDocencia = sesion(
                2, 1, "LCL", "Lengua Castellana", List.of("LEN2", "LEN8"), "A5", "4ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(conAulaLarga, coDocencia)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("4ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        // Se afirma sobre el texto con los espacios NORMALIZADOS porque la entrada larga
        // NO cabe de una pieza en los 96,2 pt de la columna y el maquetador la parte en
        // "…Taller 1 Aula" / "Plástica" (medido en el M2: 33 caracteres son 120 pt a 7,99).
        // Ese corte es la conducta correcta —lo decide el ancho medido por la fuente— y es
        // justo lo que distingue este caso de un split por espacios: un maquetador que
        // tratase la entrada como tres campos perdería todo lo que sigue a "Taller", y
        // entonces la cadena completa no estaría ni siquiera tras normalizar.
        String pagina = normalizado(texto(reader, 1));
        assertThat(pagina).contains("EXPRE DIB1 Taller 1 Aula Plástica");
        assertThat(pagina).contains("LCL LEN2/LEN8 A5");
        reader.close();
    }

    /**
     * NO SE CONDENSA NADA: las seis entradas de una celda se imprimen enteras y no aparece
     * la marca {@code +N} de la rejilla de pantalla. Seis es el máximo real del banco (22
     * celdas lo alcanzan), así que el caso no es hipotético.
     */
    @Test
    void lasSeisEntradasDeUnaCeldaSeImprimenEnterasYSinMarcaDeRecorte() throws IOException {
        List<SesionVistaDTO> seis = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            seis.add(sesion(1, 1, "CyR" + i, "Cultura y Recursos " + i,
                    List.of("INF" + i), "A1" + i, "1ºA"));
        }

        byte[] pdf = HorarioPdf.escribir(proyeccion(seis),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String pagina = texto(reader, 1);
        for (int i = 1; i <= 6; i++) {
            assertThat(pagina).contains("CyR" + i + " INF" + i + " A1" + i);
        }
        assertThat(pagina).doesNotContain("+4").doesNotContain("+N");
        reader.close();
    }

    // ------------------------------------------------------------------ C4: los sombreados

    /**
     * Los dos grises se emiten: el del recreo ({@code 0.88}) y el de las bandas alternas
     * ({@code 0.93}). No hay forma de leer un fondo con el extractor de texto, así que el
     * aserto va sobre el FLUJO DE CONTENIDO descomprimido, donde el color de relleno se
     * escribe como operador {@code rg}.
     *
     * <p>La página de este caso tiene una celda de dos entradas (banda) y su fila de
     * recreo, así que los dos grises tienen que estar.
     */
    @Test
    void emiteElGrisDelRecreoYElDeLasBandasAlternas() throws IOException {
        SesionVistaDTO una = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");
        SesionVistaDTO otra = sesion(1, 1, "LEN", "Lengua", List.of("LEN1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(una, otra)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String flujo = flujoDePagina(reader, 1);
        assertThat(flujo).as("gris del recreo").contains(GRIS_RECREO);
        assertThat(flujo).as("gris de banda").contains(GRIS_BANDA);
        reader.close();
    }

    /**
     * La otra mitad: una página SIN celdas de varias entradas no lleva ni una banda. Sin
     * este caso, un fondo gris pintado siempre —en toda celda— pasaría el test de arriba y
     * dejaría la página entera rayada.
     */
    @Test
    void sinCeldasDeVariasEntradasNoSeEmiteNingunGrisDeBanda() throws IOException {
        SesionVistaDTO sola = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sola)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String flujo = flujoDePagina(reader, 1);
        assertThat(flujo).as("el recreo sí se sombrea").contains(GRIS_RECREO);
        assertThat(flujo).as("pero ninguna banda").doesNotContain(GRIS_BANDA);
        reader.close();
    }

    /**
     * EL FONDO CUBRE LA ENTRADA COMPLETA, sus dos líneas incluidas. Es la razón de ser de
     * la tabla anidada y lo que las demás fixturas no pueden ver: todas sus entradas caben
     * en una línea, y ahí una banda por entrada y una banda por línea son indistinguibles.
     *
     * <p>Aquí la PRIMERA entrada de la celda mide 33 caracteres y no cabe en los 96,2 pt de
     * la columna, así que se parte. Se afirma sobre las ALTURAS de los rectángulos grises:
     * la entrada partida tiene que dar UNA banda de dos líneas, no dos de una. Con las
     * bandas alternando por línea saldrían dos de una línea y el aserto cae.
     *
     * <p>Tres entradas y no dos porque la alternancia empieza en gris: con dos, la segunda
     * sería blanca y el único rectángulo gris no diría nada sobre las demás.
     */
    @Test
    void laBandaDeUnaEntradaPartidaCubreSusDosLineas() throws IOException {
        SesionVistaDTO larga = sesion(
                1, 1, "EXPRE", "Expresión", List.of("DIB1"), "Taller 1 Aula Plástica", "4ºA");
        SesionVistaDTO media = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "4ºA");
        SesionVistaDTO corta = sesion(1, 1, "LEN", "Lengua", List.of("LEN1"), "A6", "4ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(larga, media, corta)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("4ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        float unaLinea = HorarioPdf.CUERPO * HorarioPdf.INTERLINEADO + 1f;
        List<Float> altos = altosDeBanda(reader, 1);

        // GUARDA: la celda tiene tres entradas, así que la alternancia pinta dos bandas.
        assertThat(altos).as("bandas grises (entradas 1.ª y 3.ª)").hasSize(2);
        assertThat(altos.get(0)).as("la entrada partida, bajo UN solo fondo de dos líneas")
                .isCloseTo(2 * unaLinea - 1f, within(0.05f));
        assertThat(altos.get(1)).as("la tercera entrada, de una línea")
                .isCloseTo(unaLinea, within(0.05f));
    }

    /**
     * El recreo va DONDE LA JORNADA LO PONE: entre el tercer y el cuarto tramo lectivo, no
     * al final. El test anterior de horas afirma que la fila existe y con qué rango, no en
     * qué posición; una maqueta que sacara todos los recreos al pie imprimiría exactamente
     * el mismo texto.
     *
     * <p>Se afirma por COORDENADA VERTICAL: las celdas de la columna de horas se apilan de
     * arriba abajo, así que ordenando sus rectángulos por {@code y} descendente se obtiene
     * el orden de las filas tal como se ven. La fila 0 es la cabecera de días; el recreo,
     * que es la única sombreada, tiene que ser la cuarta fila de datos.
     */
    @Test
    void laFilaDelRecreoVaEntreElTercerYElCuartoTramoLectivo() throws IOException {
        SesionVistaDTO sesion = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sesion)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        List<Float> filas = filasDeLaColumnaDeHoras(reader, 1);

        // GUARDA: cabecera + los siete tramos de la jornada de referencia.
        assertThat(filas).as("filas de la rejilla").hasSize(8);
        assertThat(filas.indexOf(yDelRecreo(reader, 1)))
                .as("posición del recreo contando la cabecera como fila 0").isEqualTo(4);
        reader.close();
    }

    /**
     * AIRE BAJO LA ÚLTIMA BANDA (S149, M5). Va en el CONTENEDOR, no en las bandas: por eso
     * {@link #laBandaDeUnaEntradaPartidaCubreSusDosLineas} sigue midiendo las mismas
     * alturas y este caso mide otra cosa, la distancia entre el suelo de la última banda y
     * el filete inferior de la celda.
     *
     * <p>Antes de M5 esa distancia era CERO en las 192 celdas de varias entradas del
     * documento real: con el contenedor a relleno 0 por los cuatro lados, el alto de la
     * celda era exactamente la suma de sus entradas y la última línea se pegaba a la raya.
     *
     * <p>Tres entradas y no dos porque la alternancia empieza en gris: con dos, la última
     * sería blanca y no habría banda cuyo suelo medir.
     */
    @Test
    void bajoLaUltimaBandaQuedaAireHastaElFileteDeLaCelda() throws IOException {
        List<SesionVistaDTO> tres = List.of(
                sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA"),
                sesion(1, 1, "LEN", "Lengua", List.of("LEN1"), "A6", "1ºA"),
                sesion(1, 1, "ING", "Inglés", List.of("ING1"), "A7", "1ºA"));

        byte[] pdf = HorarioPdf.escribir(proyeccion(tres),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String flujo = flujoDePagina(reader, 1);

        // GUARDA: las tres entradas dan dos bandas (la 1.ª y la 3.ª).
        List<Float> altos = altosDeBanda(reader, 1);
        assertThat(altos).as("bandas grises").hasSize(2);

        float sueloUltimaBanda = sueloDeLaBandaMasBaja(flujo);
        // La celda es el rectángulo CON TRAZO que contiene esa banda, no el más bajo de la
        // columna: en la primera columna hay una celda por fila de la jornada.
        float xCelda = HorarioPdf.MARGEN + HorarioPdf.COL_HORAS;
        float sueloCelda = rectangulosConTrazo(flujo).stream()
                .filter(r -> Math.abs(r[0] - xCelda) < 0.01f
                        && Math.abs(r[2] - HorarioPdf.COL_DIA) < 0.01f
                        && r[1] <= sueloUltimaBanda && sueloUltimaBanda <= r[1] + r[3])
                .map(r -> r[1])
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new AssertionError("ninguna celda contiene la banda"));
        assertThat(sueloUltimaBanda - sueloCelda)
                .as("aire entre la última banda y el filete de abajo")
                .isCloseTo(HorarioPdf.AIRE_BAJO_BANDAS, within(0.05f));
        reader.close();
    }

    // ------------------------------------------------------------------ C5: las horas

    /**
     * Las horas salen DE LA JORNADA, con el rango {@code "HH:mm-HH:mm"}, y la fila de
     * recreo está con las suyas. La jornada de este caso NO es la de referencia: empieza a
     * las 07:15, para que un rango escrito a mano en el código —o heredado de la plantilla
     * de {@code JornadaService}— no pueda pasar el aserto.
     */
    @Test
    void lasHorasDeLasFilasSalenDeLaJornadaRecreoIncluido() throws IOException {
        JornadaDTO otraJornada = new JornadaDTO(true, List.of(
                tramo(1, true, 1, "07:15", "08:10"),
                tramo(2, false, null, "08:10", "08:40"),
                tramo(3, true, 2, "08:40", "09:35")));

        byte[] pdf = HorarioPdf.escribir(
                proyeccion(List.of(sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA"))),
                VistaPdf.GRUPO, contexto(otraJornada, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String pagina = texto(reader, 1);
        assertThat(pagina).contains("07:15-08:10");
        assertThat(pagina).contains("08:10-08:40");
        assertThat(pagina).contains("08:40-09:35");
        assertThat(pagina).contains("Recreo");
        reader.close();
    }

    /** Sin tramos no hay rejilla que pintar, y eso es un fallo de integridad, no un 404. */
    @Test
    void jornadaSinTramosAbortaConIllegalState() {
        assertThatThrownBy(() -> HorarioPdf.escribir(
                proyeccion(List.of(sesion(1, 1, "MAT", "Mates", List.of("M1"), "A5", "1ºA"))),
                VistaPdf.GRUPO,
                contexto(new JornadaDTO(true, List.of()), List.of("1ºA"), Map.of(), Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene tramos");
    }

    // ------------------------------------------------------------------ C6: la leyenda

    /**
     * La leyenda lleva {@code "CÓDIGO — Nombre"} de los profesores y las asignaturas DE ESA
     * PÁGINA. El profesor de la otra página es el aserto que importa: si la leyenda se
     * montara una sola vez para todo el documento, {@code LEN1} aparecería también en la
     * página de 1ºA.
     */
    @Test
    void laLeyendaNombraSoloLosCodigosDeSuPropiaPagina() throws IOException {
        SesionVistaDTO enPrimera = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");
        SesionVistaDTO enSegunda = sesion(1, 2, "LCL", "Lengua Castellana", List.of("LEN1"), "A5", "1ºB");
        Map<String, String> nombres = Map.of(
                "MAT1", "Macías Magro, Sonia",
                "LEN1", "Crespo Saborido, Ana María");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(enPrimera, enSegunda)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA", "1ºB"), nombres, Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String primera = texto(reader, 1);
        assertThat(primera).contains("MAT1 — Macías Magro, Sonia");
        assertThat(primera).contains("MAT — Matemáticas");
        assertThat(primera).doesNotContain("LEN1");
        assertThat(texto(reader, 2)).contains("LEN1 — Crespo Saborido, Ana María");
        reader.close();
    }

    /**
     * Los encabezados están Y las columnas son de verdad lo que dicen: profesores a la
     * izquierda, asignaturas a la derecha, cada bloque independiente del otro.
     *
     * <p>El caso pone MÁS profesores que asignaturas (tres contra una) precisamente porque
     * la maquetación anterior partía una lista única por la mitad: con ella, el tercer
     * profesor caía bajo el encabezado «Asignaturas». El texto plano NO puede distinguir
     * las dos maquetaciones —el extractor concatena las dos celdas de una misma fila—, así
     * que el aserto va por COORDENADA: se cuentan los bloques de texto que caen en la x de
     * cada columna, y las filas de la leyenda.
     *
     * <p>Eso es lo que separa dos listas independientes de una lista única partida por la
     * mitad. Con tres profesores y una asignatura: dos listas dan 1+3 bloques a la
     * izquierda, 1+1 a la derecha y 1+3 filas; la lista partida da 1+2 y 1+2 en 1+2 filas.
     * Las dos maquetaciones imprimen EXACTAMENTE el mismo texto, y por eso el aserto de
     * contenido de abajo no basta y se conserva solo como guarda de que nada se perdió.
     */
    @Test
    void laLeyendaLlevaEncabezadosYCadaColumnaSoloLoSuyo() throws IOException {
        SesionVistaDTO conTres = new SesionVistaDTO(
                1L, 0, 1, 1, "MAT", "Matemáticas", List.of("MAT1", "MAT2", "MAT3"), "A5",
                List.of(), List.of("1ºA"), "MAT-ACT", "MAT-P1");
        Map<String, String> nombres = Map.of(
                "MAT1", "Uno Uno", "MAT2", "Dos Dos", "MAT3", "Tres Tres");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(conTres)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), nombres, Map.of()));

        PdfReader reader = new PdfReader(pdf);

        // GUARDA de contenido: los cuatro códigos y los dos encabezados están en la página.
        String pagina = texto(reader, 1);
        assertThat(pagina).contains("Profesores").contains("Asignaturas");
        assertThat(pagina).contains("MAT1 — Uno Uno").contains("MAT2 — Dos Dos")
                .contains("MAT3 — Tres Tres").contains("MAT — Matemáticas");

        // EL ASERTO: la x de cada bloque. La columna izquierda empieza en el margen más el
        // relleno de celda; la derecha, media rejilla más allá. Ningún bloque de la rejilla
        // cae en esas dos x (sus celdas van centradas), así que contar por x aísla la
        // leyenda sin necesidad de recortar por altura.
        float xIzquierda = HorarioPdf.MARGEN + HorarioPdf.PADDING;
        float xDerecha = HorarioPdf.MARGEN
                + (HorarioPdf.COL_HORAS + 5 * HorarioPdf.COL_DIA) / 2f + HorarioPdf.PADDING;

        List<float[]> bloques = bloquesDeTexto(reader, 1);
        assertThat(enColumna(bloques, xIzquierda))
                .as("encabezado + los TRES profesores, a la izquierda").isEqualTo(4);
        assertThat(enColumna(bloques, xDerecha))
                .as("encabezado + la ÚNICA asignatura, a la derecha").isEqualTo(2);
        assertThat(filasDeLeyenda(bloques, xIzquierda, xDerecha))
                .as("alto de la leyenda = encabezado + la columna más larga").isEqualTo(4);
        reader.close();
    }

    /**
     * Un profesor que no está en el mapa se imprime SOLO CON SU CÓDIGO. Es una ausencia,
     * no una corrupción: la página sigue diciendo la verdad —ese código da clase ahí— y
     * callar la entrada entera dejaría al lector sin saber qué significa el código que ve
     * en la rejilla.
     */
    @Test
    void unProfesorSinNombreEnElMapaSaleConSuCodigoYSinRaya() throws IOException {
        SesionVistaDTO sesion = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT9"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sesion)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        String pagina = texto(reader, 1);
        assertThat(pagina).contains("MAT9");
        assertThat(pagina).doesNotContain("MAT9 —");
        reader.close();
    }

    // ------------------------------------------------------------------ C7: las fuentes

    /**
     * Las fuentes van EMPOTRADAS. Se comprueba sobre el descriptor de fuente de la página
     * —{@code FontFile2} presente—, que es exactamente lo que mira {@code pdffonts} para
     * decir {@code emb: yes}. Sin empotrar, el PDF se ve distinto en cada máquina y las
     * tildes dependen de lo que tenga instalado quien lo abra.
     */
    @Test
    void lasFuentesDeLaPaginaVanEmpotradas() throws IOException {
        SesionVistaDTO sesion = sesion(1, 1, "MAT", "Matemáticas", List.of("MAT1"), "A5", "1ºA");

        byte[] pdf = HorarioPdf.escribir(proyeccion(List.of(sesion)),
                VistaPdf.GRUPO, contexto(JORNADA, List.of("1ºA"), Map.of(), Map.of()));

        PdfReader reader = new PdfReader(pdf);
        List<PdfDictionary> descriptores = descriptoresDeFuente(reader);
        assertThat(descriptores).as("la página usa al menos una fuente").isNotEmpty();
        assertThat(descriptores).allSatisfy(
                d -> assertThat(d.get(PdfName.FONTFILE2)).as("FontFile2 del descriptor").isNotNull());
        reader.close();
    }

    // ------------------------------------------------------------------ utilidades

    private static String texto(PdfReader reader, int pagina) throws IOException {
        return new PdfTextExtractor(reader).getTextFromPage(pagina);
    }

    /** La primera línea no vacía de una página, que es el título del grupo. */
    private static String titulo(PdfReader reader, int pagina) throws IOException {
        for (String linea : texto(reader, pagina).split("\\R")) {
            if (!linea.isBlank()) {
                return linea.trim();
            }
        }
        return "";
    }

    /** El flujo de contenido de una página, YA DESCOMPRIMIDO por el propio lector. */
    private static String flujoDePagina(PdfReader reader, int pagina) throws IOException {
        return new String(reader.getPageContent(pagina), StandardCharsets.ISO_8859_1);
    }

    /**
     * El texto de una página con toda racha de espacios y saltos colapsada a un espacio.
     * Es lo que hay que usar cuando el aserto cruza un salto de línea que pone la
     * MAQUETACIÓN: afirmar sobre el texto crudo ataría el test al ancho de columna, que no
     * es lo que ese caso dice.
     */
    private static String normalizado(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }

    /**
     * Los descriptores de fuente de la página 1, atravesando las fuentes compuestas: una
     * TrueType empotrada como CID ({@code IDENTITY_H}) cuelga su descriptor de la fuente
     * DESCENDIENTE, no de la de primer nivel.
     */
    private static List<PdfDictionary> descriptoresDeFuente(PdfReader reader) {
        List<PdfDictionary> descriptores = new ArrayList<>();
        PdfDictionary recursos = reader.getPageN(1).getAsDict(PdfName.RESOURCES);
        PdfDictionary fuentes = recursos.getAsDict(PdfName.FONT);
        for (PdfName clave : fuentes.getKeys()) {
            PdfDictionary fuente = fuentes.getAsDict(clave);
            PdfDictionary descriptor = fuente.getAsDict(PdfName.FONTDESCRIPTOR);
            if (descriptor == null && fuente.getAsArray(PdfName.DESCENDANTFONTS) != null) {
                descriptor = fuente.getAsArray(PdfName.DESCENDANTFONTS)
                        .getAsDict(0).getAsDict(PdfName.FONTDESCRIPTOR);
            }
            if (descriptor != null) {
                descriptores.add(descriptor);
            }
        }
        return descriptores;
    }

    /**
     * Los bloques de texto de una página como pares {@code (x, y)}, en el orden del flujo.
     * Cada celda con texto emite un {@code Tm} con su posición ABSOLUTA, que es la única
     * forma de saber en qué columna cayó: el extractor de texto colapsa las columnas.
     */
    private static List<float[]> bloquesDeTexto(PdfReader reader, int pagina) throws IOException {
        List<float[]> bloques = new ArrayList<>();
        Matcher m = Pattern.compile("1 0 0 1 ([\\d.]+) ([\\d.]+) Tm")
                .matcher(flujoDePagina(reader, pagina));
        while (m.find()) {
            bloques.add(new float[] {Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2))});
        }
        return bloques;
    }

    /** Cuántos bloques de texto caen exactamente en esa x. */
    private static long enColumna(List<float[]> bloques, float x) {
        return bloques.stream().filter(b -> Math.abs(b[0] - x) < 0.01f).count();
    }

    /** Cuántas filas distintas ocupa la leyenda (y distintas en sus dos columnas). */
    private static long filasDeLeyenda(List<float[]> bloques, float xIzq, float xDer) {
        return bloques.stream()
                .filter(b -> Math.abs(b[0] - xIzq) < 0.01f || Math.abs(b[0] - xDer) < 0.01f)
                .map(b -> b[1])
                .distinct()
                .count();
    }

    /** Alturas de los rectángulos de banda gris de una página, en orden de emisión. */
    private static List<Float> altosDeBanda(PdfReader reader, int pagina) throws IOException {
        List<Float> altos = new ArrayList<>();
        Matcher m = Pattern.compile(Pattern.quote(GRIS_BANDA)
                        + "\\s+[\\d.]+ [\\d.]+ [\\d.]+ ([\\d.]+) re\\s+f")
                .matcher(flujoDePagina(reader, pagina));
        while (m.find()) {
            altos.add(Float.parseFloat(m.group(1)));
        }
        return altos;
    }

    /**
     * Las {@code y} de las filas de la columna de horas, de arriba abajo. Se filtran por x y
     * ancho COMPARANDO NÚMEROS, no texto: el flujo escribe {@code "28"} donde la constante
     * vale {@code 28.0f}, y un regex construido con la constante no casaría nada —fallo que
     * este test cometió antes de medirse—. Se deduplican porque cada fila emite su
     * rectángulo dos veces: el borde y, si va sombreada, el relleno.
     */
    private static List<Float> filasDeLaColumnaDeHoras(PdfReader reader, int pagina)
            throws IOException {
        List<Float> ys = new ArrayList<>();
        for (float[] r : rectangulos(flujoDePagina(reader, pagina))) {
            if (Math.abs(r[0] - HorarioPdf.MARGEN) < 0.01f
                    && Math.abs(r[2] - HorarioPdf.COL_HORAS) < 0.01f
                    && !ys.contains(r[1])) {
                ys.add(r[1]);
            }
        }
        ys.sort(Comparator.reverseOrder());
        return ys;
    }

    /** La {@code y} de la fila sombreada de la columna de horas, que es la del recreo. */
    private static Float yDelRecreo(PdfReader reader, int pagina) throws IOException {
        String flujo = flujoDePagina(reader, pagina);
        int desde = flujo.indexOf(GRIS_RECREO);
        assertThat(desde).as("hay una fila sombreada").isNotNegative();
        for (float[] r : rectangulos(flujo.substring(desde))) {
            if (Math.abs(r[0] - HorarioPdf.MARGEN) < 0.01f
                    && Math.abs(r[2] - HorarioPdf.COL_HORAS) < 0.01f) {
                return r[1];
            }
        }
        throw new AssertionError("el sombreado no cubre la columna de horas");
    }

    /** Los rectángulos DIBUJADOS CON TRAZO, que son los filetes de las celdas. */
    private static List<float[]> rectangulosConTrazo(String flujo) {
        List<float[]> rects = new ArrayList<>();
        Matcher m = Pattern.compile("([\\d.]+) ([\\d.]+) ([\\d.]+) ([\\d.]+) re\\s*\\nS")
                .matcher(flujo);
        while (m.find()) {
            rects.add(new float[] {
                Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)),
                Float.parseFloat(m.group(3)), Float.parseFloat(m.group(4))});
        }
        return rects;
    }

    /** La {@code y} del borde inferior de la banda gris más baja de la página. */
    private static float sueloDeLaBandaMasBaja(String flujo) {
        Matcher m = Pattern.compile(Pattern.quote(GRIS_BANDA)
                        + "\\s+[\\d.]+ ([\\d.]+) [\\d.]+ [\\d.]+ re\\s+f").matcher(flujo);
        float suelo = Float.MAX_VALUE;
        while (m.find()) {
            suelo = Math.min(suelo, Float.parseFloat(m.group(1)));
        }
        assertThat(suelo).as("hay al menos una banda").isLessThan(Float.MAX_VALUE);
        return suelo;
    }

    /** Todos los rectángulos del flujo como {@code (x, y, ancho, alto)}, en orden. */
    private static List<float[]> rectangulos(String flujo) {
        List<float[]> rects = new ArrayList<>();
        Matcher m = Pattern.compile("([\\d.]+) ([\\d.]+) ([\\d.]+) ([\\d.]+) re")
                .matcher(flujo);
        while (m.find()) {
            rects.add(new float[] {
                Float.parseFloat(m.group(1)), Float.parseFloat(m.group(2)),
                Float.parseFloat(m.group(3)), Float.parseFloat(m.group(4))});
        }
        return rects;
    }

    private static ContextoPdf contexto(JornadaDTO jornada, List<String> orden,
                                        Map<String, String> nombres,
                                        Map<String, String> tutores) {
        return new ContextoPdf(jornada, orden, nombres, tutores);
    }

    private static TramoJornadaDTO tramo(int orden, boolean lectivo, Integer ordenEnDia,
                                         String inicio, String fin) {
        return new TramoJornadaDTO("LUNES", inicio, fin, lectivo, orden, ordenEnDia);
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
