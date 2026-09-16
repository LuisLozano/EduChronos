package es.yaroki.educhronos.app.exportacion;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Chunk;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.SplitCharacter;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import es.yaroki.educhronos.app.exportacion.VistaPdf.BloqueLeyenda;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializa un {@link HorarioProyeccionDTO} a PDF, una página A4 vertical por RECURSO
 * (S149, C-exportacion-pdf-grupo; generalizado en S150). Función PURA, misma filosofía
 * que {@link HorarioCsv}: entran la proyección, la {@link VistaPdf} que dice por qué
 * recurso se pagina y un {@link ContextoPdf} con los datos de catálogo ya resueltos, y
 * salen los bytes del fichero. No toca JPA, no navega entidades, no consulta nada.
 *
 * <p><b>Qué sabe esta clase y qué no.</b> Sabe MAQUETAR: la rejilla, la jornada, las
 * fuentes empotradas, las bandas de una celda de varias entradas y el presupuesto de
 * página. NO sabe por qué recurso se agrupa, qué dice la clave de lectura, qué texto
 * lleva una entrada, qué rótulo lleva la línea bajo el título ni qué bloques tiene la
 * leyenda: todo eso lo pregunta a la {@link VistaPdf}. Lo que sigue aquí es lo que es
 * igual en cualquier paginación de un horario.
 *
 * <p><b>Por qué necesita un contexto, y el CSV no.</b> La proyección lleva el par
 * {@code (dia, tramo)} como ORDINALES (1..5, 1..6) y los profesores como CÓDIGOS; eso le
 * basta a una hoja de cálculo, pero un horario impreso se lee por la hora de reloj y por
 * el nombre de la persona, y encima quiere el dato que va bajo el título. Nada de eso
 * está en {@link SesionVistaDTO}. La asignatura NO se cruza: su nombre ya viaja en la
 * proyección ({@code asignaturaNombre}).
 *
 * <p><b>PRESUPUESTO DE PÁGINA</b>, medido en el M2 de S149 sobre el banco real y no
 * negociable desde aquí:
 * <ul>
 *   <li>A4 vertical con {@value #MARGEN} pt de margen: útil 539 x 786 pt.
 *   <li>Columna de horas {@value #COL_HORAS} pt + cinco columnas de día de
 *       {@value #COL_DIA} pt = 539 pt exactos. Relleno {@value #PADDING} pt por lado.
 *   <li>Cuerpo {@value #CUERPO} pt en rejilla, cabecera, columna de horas, línea bajo el
 *       título, clave de lectura y leyenda; nada por debajo. Título
 *       {@value #TITULO} pt en negrita.
 *   <li>Interlineado {@value #INTERLINEADO}.
 * </ul>
 * La fuente es DejaVu Sans Condensed EMPOTRADA desde el classpath ({@code /fuentes}),
 * nunca leída de las fuentes del sistema: en producción esto corre en un Windows limpio
 * donde {@code /usr/share/fonts} no existe. La licencia de la familia (Bitstream Vera)
 * exige adjuntar el aviso y viaja al lado, en {@code fuentes/LICENCIA-DejaVu.txt}.
 *
 * <p><b>NO SE CONDENSA NADA.</b> La rejilla de pantalla recorta las celdas que no caben y
 * lo marca con {@code +N} (D11), porque su alto es fijo; el papel no tiene esa
 * limitación, así que aquí las celdas se imprimen ENTERAS. En el banco de referencia hay
 * 22 celdas de seis entradas —dos en cada uno de once grupos—, y las seis se ven.
 *
 * <p><b>El texto de una entrada es UNA CADENA, no tres campos.</b> Los códigos de aula
 * llevan espacios dentro ({@code "Taller 1 Aula Plástica"}, {@code "A12 Informática"}),
 * de modo que partir por espacios para maquetar rompería el dato. El salto de línea lo
 * decide el ancho medido por la fuente. Como esa cadena no lleva separadores, la página
 * imprime bajo el título la CLAVE DE LECTURA que dicta la vista
 * ({@link VistaPdf#claveDeLectura()}): sin ella una celda de tres palabras es ambigua.
 *
 * <p>Los fallos de integridad abortan con {@link IllegalStateException} y NUNCA con
 * {@link IllegalArgumentException}: el controlador traduce esta última a 404 para el id
 * inexistente, y un error de este serializador no es un recurso que falte.
 */
public final class HorarioPdf {

    /** Margen de página, los cuatro lados. */
    static final float MARGEN = 28f;

    /** Ancho de la columna de horas. */
    static final float COL_HORAS = 58f;

    /** Ancho de cada una de las cinco columnas de día. */
    static final float COL_DIA = 96.2f;

    /** Relleno interior de celda, por lado. */
    static final float PADDING = 2f;

    /**
     * Aire BAJO la última banda de una celda de varias entradas (S149, M5). Va en el
     * contenedor y no en las bandas, y solo abajo, por dos razones que se miden en el
     * papel: el relleno lateral del contenedor tiene que seguir siendo CERO —es lo que
     * hace que las bandas lleguen al borde de la columna y se lean como bandas y no como
     * rectángulos flotando—, y subirlo dentro de cada banda separaría también las entradas
     * entre sí, que es un cambio de aspecto que nadie pidió y que en una celda de seis
     * costaría 12 pt de alto en vez de 1,5.
     *
     * <p>Lo que corrige: con el contenedor a relleno 0 por los cuatro lados, el alto de la
     * celda era EXACTAMENTE la suma de sus entradas, así que la última línea quedaba a los
     * 0,50 pt de la propia banda del filete inferior —medido en M4: las 192 celdas de
     * varias entradas del documento, sin excepción—, mientras que una celda de una sola
     * entrada disfrutaba de los {@value #PADDING} pt de siempre. Esto iguala el trato.
     */
    static final float AIRE_BAJO_BANDAS = 1.5f;

    /** Cuerpo de TODO el texto de la página salvo el título. Nada baja de aquí. */
    static final float CUERPO = 7.99f;

    /** Cuerpo del título de la página. */
    static final float TITULO = 10f;

    /** Factor de interlineado. */
    static final float INTERLINEADO = 1.15f;

    /** Gris de la fila de recreo, ancho completo. */
    private static final Color GRIS_RECREO = new Color(0.88f, 0.88f, 0.88f);

    /**
     * Gris de las bandas alternas dentro de una celda de varias entradas. Muy claro a
     * propósito: la banda separa entradas, no las jerarquiza, y el texto sigue en negro.
     */
    private static final Color GRIS_BANDA = new Color(0.93f, 0.93f, 0.93f);

    /**
     * Rótulos de la cabecera de días. Se escriben aquí y no se toman del
     * {@code dia} de la jornada a propósito: aquello es el {@code name()} de un enum
     * ({@code "MIERCOLES"}, sin tilde y en mayúsculas), que es una clave, no un rótulo
     * impreso. El índice del array es {@code dia - 1}, con {@code dia} 1..5 tal como lo
     * numera {@link SesionVistaDTO}.
     */
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};

    /**
     * La barra con la que las vistas unen una lista DENTRO del texto de una entrada. Se
     * toma de {@link VistaPdf#SEPARADOR_LISTA} y no se reescribe aquí: si aquel cambiara,
     * el corte de línea tiene que seguirle, no quedarse partiendo por un carácter que ya
     * no separa nada.
     */
    private static final char BARRA = VistaPdf.SEPARADOR_LISTA.charAt(0);

    /**
     * DÓNDE se puede partir una entrada que no cabe a lo ancho: solo DESPUÉS de un
     * espacio o de una {@value #BARRA}. Nunca dentro de un código.
     *
     * <p>Lo que corrige, medido en el M3-2 de S150 sobre las entradas reales del banco y
     * con la construcción de celda de esta clase: el corte por defecto de OpenPDF parte
     * donde le cabe, y en la vista de profesor eso producía
     * {@code "…1B-A/1B-B/1B-"} / {@code "C/1B-D"} —el grupo {@code 1B-C} roto en dos
     * líneas— y {@code "…4ºD/4"} / {@code "ºDDi EFI1"}, con {@code 4ºDDi} partido tras su
     * primer carácter. Un código partido no es un código: quien lee el papel no puede
     * saber si {@code "1B-"} es un grupo o el principio de otro.
     *
     * <p>El guion NO entra en la lista a propósito, y es justo el carácter que causaba el
     * primer caso: {@code 1B-C} lo lleva dentro. Partir tras un guion es legítimo en prosa
     * y es exactamente lo que aquí hay que impedir.
     *
     * <p><b>Esto no mueve la vista de grupo.</b> Medido antes de escribirlo sobre las 311
     * entradas distintas que esa vista produce en el banco: ninguna se parte en un sitio
     * distinto con esta regla que sin ella. Y ningún trozo indivisible desborda la
     * columna —el más ancho es {@code "Laboratorio "}, 43,53 pt sobre los 92,20 pt de
     * hueco útil—, así que la regla nunca deja una entrada sin sitio donde partir.
     */
    private static final SplitCharacter CORTE_QUE_NO_PARTE_CODIGOS =
            (start, current, end, cc, ck) -> cc[current] == ' ' || cc[current] == BARRA;

    private HorarioPdf() {
    }

    /**
     * Bytes del PDF: una página por recurso de la {@code vista}.
     *
     * <p><b>El orden de las páginas es el del catálogo, no el de la proyección.</b> Lo
     * manda {@link ContextoPdf#ordenDeRecursos()}, que el servicio toma del listado con
     * el que la aplicación enseña ese catálogo en su pantalla, para que quien busque un
     * recurso en el papel lo encuentre donde la UI le ha enseñado a buscarlo. El orden de
     * aparición en {@code sesiones}, que es el que salía antes, no es un orden: es el
     * rastro de por dónde empezó el lunes. Un recurso del horario que no estuviera en esa
     * lista NO se pierde: se imprime al final, en orden de aparición, porque callar una
     * página sería peor que descolocarla.
     *
     * @throws IllegalStateException si la jornada no trae tramos, o si un tramo lectivo
     *     no tiene {@code ordenEnDia}
     */
    public static byte[] escribir(HorarioProyeccionDTO proyeccion, VistaPdf vista,
                                  ContextoPdf contexto) {

        List<TramoJornadaDTO> filas = filasDeTramo(contexto.jornada());
        List<String> recursos = ordenarRecursos(proyeccion, vista, contexto.ordenDeRecursos());

        BaseFont normal = cargarFuente("DejaVuSansCondensed.ttf");
        BaseFont negrita = cargarFuente("DejaVuSansCondensed-Bold.ttf");
        Font fCuerpo = new Font(normal, CUERPO);
        Font fNegrita = new Font(negrita, CUERPO);
        Font fTitulo = new Font(negrita, TITULO);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, MARGEN, MARGEN, MARGEN, MARGEN);
        PdfWriter.getInstance(doc, salida);
        doc.open();

        for (int i = 0; i < recursos.size(); i++) {
            if (i > 0) {
                doc.newPage();
            }
            String recurso = recursos.get(i);
            List<SesionVistaDTO> delRecurso = proyeccion.sesiones().stream()
                    .filter(s -> vista.recursosDe(s).contains(recurso))
                    .toList();

            doc.add(titulo(vista.tituloDe(recurso, contexto), fTitulo));
            String linea = contexto.lineaPorRecurso().get(recurso);
            if (linea != null && !linea.isBlank()) {
                doc.add(lineaSuelta(vista.rotuloDeLinea() + linea, fCuerpo));
            }
            doc.add(lineaSuelta(vista.claveDeLectura(), fCuerpo));
            doc.add(rejilla(delRecurso, filas, vista, fCuerpo, fNegrita));

            // Una leyenda sin una sola línea NO se imprime, ni siquiera sus encabezados:
            // en una página vacía —las que la vista de aula sí saca— «Profesores» y
            // «Asignaturas» sobre la nada anuncian un contenido que no está. Las páginas
            // con clases siempre traen al menos el bloque de asignaturas lleno, así que
            // esto no las toca.
            List<BloqueLeyenda> bloques =
                    vista.leyendaDe(delRecurso, contexto.nombresDeProfesor());
            if (bloques.stream().anyMatch(bloque -> !bloque.lineas().isEmpty())) {
                doc.add(leyenda(bloques, fCuerpo, fNegrita));
            }
        }

        doc.close();
        return salida.toByteArray();
    }

    // ------------------------------------------------------------------ fuentes

    /**
     * Carga una fuente del classpath y la EMPOTRA. Los bytes se leen a memoria y se le
     * pasan a {@link BaseFont#createFont}: el constructor por ruta buscaría en el sistema
     * de ficheros, que es justo lo que no puede haber en el entorno de destino. El
     * primer argumento sigue siendo el nombre con extensión porque de él deduce OpenPDF
     * el tipo de fuente y es la clave de su caché.
     */
    private static BaseFont cargarFuente(String recurso) {
        String ruta = "/fuentes/" + recurso;
        try (InputStream in = HorarioPdf.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("No está en el classpath la fuente " + ruta);
            }
            return BaseFont.createFont(recurso, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la fuente " + ruta, e);
        }
    }

    // ------------------------------------------------------------------ filas y recursos

    /**
     * Las filas de la rejilla: los tramos de UN día de la jornada, en orden de
     * {@code orden}, recreo incluido. Vale con un día porque la jornada se define por DÍA
     * TIPO y {@code JornadaService.expandir} lo replica idéntico en los cinco; tomar el
     * primero que aparece evita imponer aquí un sexto lugar donde se sepa eso.
     */
    private static List<TramoJornadaDTO> filasDeTramo(JornadaDTO jornada) {
        if (jornada.tramos() == null || jornada.tramos().isEmpty()) {
            throw new IllegalStateException("La jornada no tiene tramos: no hay rejilla que pintar");
        }
        String diaReferencia = jornada.tramos().get(0).dia();
        return jornada.tramos().stream()
                .filter(t -> diaReferencia.equals(t.dia()))
                .sorted(Comparator.comparingInt(TramoJornadaDTO::orden))
                .toList();
    }

    /**
     * Los recursos que tienen página, en el orden del catálogo. Se recorre
     * {@code ordenDeRecursos} y se queda con los que de verdad aparecen en la proyección
     * —un recurso del catálogo sin clases no tiene nada que imprimir—, SALVO que la vista
     * pida el catálogo entero ({@link VistaPdf#incluyeRecursosSinSesiones()}), en cuyo
     * caso todos tienen página y las de los que no dan clase salen vacías.
     *
     * <p>Los que están en la proyección pero NO en el catálogo van al final, por
     * aparición, y eso vale para las dos políticas: es la regla de S149 y dice que callar
     * una página es peor que descolocarla. Con el catálogo entero ese caso debería ser
     * imposible —el catálogo es la fuente de la que salen los recursos—, pero si pasara,
     * un recurso descolocado sigue siendo mejor que un recurso perdido.
     */
    private static List<String> ordenarRecursos(HorarioProyeccionDTO proyeccion, VistaPdf vista,
                                                List<String> ordenDeRecursos) {
        Set<String> conHorario = new LinkedHashSet<>();
        for (SesionVistaDTO sesion : proyeccion.sesiones()) {
            conHorario.addAll(vista.recursosDe(sesion));
        }
        List<String> ordenados = new ArrayList<>(conHorario.size());
        for (String recurso : ordenDeRecursos) {
            boolean tieneClases = conHorario.remove(recurso);
            if (tieneClases || vista.incluyeRecursosSinSesiones()) {
                ordenados.add(recurso);
            }
        }
        ordenados.addAll(conHorario);
        return ordenados;
    }

    // ------------------------------------------------------------------ página

    private static Paragraph titulo(String texto, Font fTitulo) {
        Paragraph p = new Paragraph(texto, fTitulo);
        p.setLeading(TITULO * INTERLINEADO);
        return p;
    }

    /** Una línea de cuerpo bajo el título (la de la vista, clave de lectura). */
    private static Paragraph lineaSuelta(String texto, Font fCuerpo) {
        Paragraph p = new Paragraph(texto, fCuerpo);
        p.setLeading(CUERPO * INTERLINEADO);
        return p;
    }

    /**
     * La rejilla: cabecera de días + una fila por tramo de la jornada. Los tramos
     * lectivos llevan sus cinco celdas de día; el recreo es UNA banda de cinco columnas,
     * porque no hay nada que colocar en ella y cinco celdas vacías dirían lo contrario.
     * Esa fila va SOMBREADA de punta a punta, columna de horas incluida: el gris es lo
     * que hace que el ojo salte el corte sin leerlo.
     */
    private static PdfPTable rejilla(List<SesionVistaDTO> sesiones,
                                     List<TramoJornadaDTO> filas,
                                     VistaPdf vista,
                                     Font fCuerpo, Font fNegrita) {

        PdfPTable tabla = new PdfPTable(6);
        tabla.setTotalWidth(new float[] {COL_HORAS, COL_DIA, COL_DIA, COL_DIA, COL_DIA, COL_DIA});
        tabla.setLockedWidth(true);
        tabla.setSpacingBefore(CUERPO * INTERLINEADO);
        tabla.setSpacingAfter(CUERPO * INTERLINEADO);

        tabla.addCell(celdaTexto("", fNegrita, Element.ALIGN_CENTER));
        for (String dia : DIAS) {
            tabla.addCell(celdaTexto(dia, fNegrita, Element.ALIGN_CENTER));
        }

        for (TramoJornadaDTO fila : filas) {
            PdfPCell horas = celdaTexto(fila.horaInicio() + "-" + fila.horaFin(),
                    fNegrita, Element.ALIGN_CENTER);

            if (!fila.esLectivo()) {
                horas.setBackgroundColor(GRIS_RECREO);
                tabla.addCell(horas);
                PdfPCell recreo = celdaTexto("Recreo", fCuerpo, Element.ALIGN_CENTER);
                recreo.setColspan(DIAS.length);
                recreo.setBackgroundColor(GRIS_RECREO);
                tabla.addCell(recreo);
                continue;
            }

            tabla.addCell(horas);
            Integer tramo = fila.ordenEnDia();
            if (tramo == null) {
                throw new IllegalStateException(
                        "El tramo lectivo de orden " + fila.orden() + " no trae ordenEnDia");
            }
            for (int dia = 1; dia <= DIAS.length; dia++) {
                tabla.addCell(celdaDeHorario(entradasDe(sesiones, vista, dia, tramo), fCuerpo));
            }
        }
        return tabla;
    }

    /**
     * Las entradas de una celda, en el orden en que vienen de la proyección. Cada una es
     * la cadena que compone {@link VistaPdf#textoDeEntrada}, montada allí y nunca vuelta
     * a partir aquí.
     */
    private static List<String> entradasDe(List<SesionVistaDTO> sesiones, VistaPdf vista,
                                           int dia, int tramo) {
        List<String> entradas = new ArrayList<>();
        for (SesionVistaDTO s : sesiones) {
            if (s.dia() == dia && s.tramo() == tramo) {
                entradas.add(vista.textoDeEntrada(s));
            }
        }
        return entradas;
    }

    /**
     * Celda de horario. Con UNA entrada (o ninguna) es una celda de texto normal, blanca.
     *
     * <p>Con VARIAS, una TABLA ANIDADA de una fila por entrada, con el fondo alternando
     * gris claro y blanco desde la primera. La tabla anidada no es adorno: una entrada que
     * no cabe a lo ancho se parte en dos líneas —{@code "…Taller 1 Aula" / "Plástica"}— y,
     * pintada como párrafos sueltos, no hay forma de saber si esas dos líneas son una
     * entrada o dos. El fondo de la fila anidada cubre la entrada COMPLETA, las dos
     * líneas, porque el alto de esa fila lo fija su propio texto ya maquetado.
     */
    private static PdfPCell celdaDeHorario(List<String> entradas, Font fCuerpo) {
        PdfPCell celda = new PdfPCell();
        if (entradas.size() <= 1) {
            celda.setPadding(PADDING);
            // Una celda compuesta SIN elementos se pinta con alto cero y descuadra la
            // fila; la frase vacía le da el alto de una línea.
            celda.addElement(parrafo(entradas.isEmpty() ? "" : entradas.get(0), fCuerpo));
            return celda;
        }

        // Sin relleno LATERAL en la celda contenedora: así las bandas llegan al borde de
        // la columna y se ven como bandas, no como rectángulos flotando. El relleno del
        // texto lo pone cada fila anidada. Abajo sí hay aire, para que la última línea no
        // se pegue al filete que separa esta celda de la de debajo.
        celda.setPadding(0f);
        celda.setPaddingBottom(AIRE_BAJO_BANDAS);
        PdfPTable bandas = new PdfPTable(1);
        bandas.setWidthPercentage(100f);
        for (int i = 0; i < entradas.size(); i++) {
            PdfPCell banda = new PdfPCell(
                    conCorteDeCodigos(new Phrase(entradas.get(i), fCuerpo)));
            banda.setBorder(Rectangle.NO_BORDER);
            banda.setPaddingLeft(PADDING);
            banda.setPaddingRight(PADDING);
            banda.setPaddingTop(0.5f);
            banda.setPaddingBottom(0.5f);
            banda.setLeading(0f, INTERLINEADO);
            if (i % 2 == 0) {
                banda.setBackgroundColor(GRIS_BANDA);
            }
            bandas.addCell(banda);
        }
        celda.addElement(bandas);
        return celda;
    }

    /**
     * La leyenda del pie: UNA COLUMNA POR BLOQUE de los que declara la vista
     * ({@link VistaPdf#leyendaDe}), con encabezado en negrita sobre cada una. Solo los
     * códigos QUE APARECEN EN ESA PÁGINA, que es cosa de la vista; aquí solo se reparte
     * el ancho y se apilan las líneas.
     *
     * <p>Las columnas son listas independientes, no una lista partida por la mitad: con
     * un encabezado encima, una entrada colada al final de la columna vecina sería una
     * mentira tipográfica. El alto de la leyenda es el del bloque MÁS LARGO, no la media.
     *
     * <p>Los nombres de catálogo salen tal cual están en el origen, truncados incluidos:
     * eso es un dato del centro, no algo que el exportador deba maquillar.
     */
    private static PdfPTable leyenda(List<BloqueLeyenda> bloques, Font fCuerpo, Font fNegrita) {

        PdfPTable tabla = new PdfPTable(bloques.size());
        float[] anchos = new float[bloques.size()];
        Arrays.fill(anchos, (COL_HORAS + DIAS.length * COL_DIA) / bloques.size());
        tabla.setTotalWidth(anchos);
        tabla.setLockedWidth(true);

        int alto = 0;
        for (BloqueLeyenda bloque : bloques) {
            tabla.addCell(celdaDeLeyenda(bloque.encabezado(), fNegrita));
            alto = Math.max(alto, bloque.lineas().size());
        }
        for (int f = 0; f < alto; f++) {
            for (BloqueLeyenda bloque : bloques) {
                List<String> lineas = bloque.lineas();
                tabla.addCell(celdaDeLeyenda(f < lineas.size() ? lineas.get(f) : "", fCuerpo));
            }
        }
        return tabla;
    }

    // ------------------------------------------------------------------ piezas

    private static Paragraph parrafo(String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setLeading(CUERPO * INTERLINEADO);
        return conCorteDeCodigos(p);
    }

    /**
     * Le pone a los trozos de una frase la regla de corte
     * {@link #CORTE_QUE_NO_PARTE_CODIGOS}. Se hace sobre la frase YA construida, y no
     * montando los {@link Chunk} a mano, para no cambiar de paso cómo se arma el texto:
     * lo único que se añade es dónde puede romperse.
     */
    private static <T extends Phrase> T conCorteDeCodigos(T frase) {
        for (Element trozo : frase.getChunks()) {
            ((Chunk) trozo).setSplitCharacter(CORTE_QUE_NO_PARTE_CODIGOS);
        }
        return frase;
    }

    private static PdfPCell celdaTexto(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(PADDING);
        celda.setHorizontalAlignment(alineacion);
        celda.setLeading(0f, INTERLINEADO);
        return celda;
    }

    /** Celda de leyenda: sin rejilla alrededor, que es ruido en un pie de página. */
    private static PdfPCell celdaDeLeyenda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(PADDING);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setLeading(0f, INTERLINEADO);
        return celda;
    }
}
