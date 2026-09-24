package es.yaroki.educhronos.app.exportacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Casos de {@link HorarioCsv} (S148, C-exportacion-csv). JUnit puro, sin Spring y
 * sin base de datos: la función recibe un DTO construido a mano y devuelve bytes,
 * así que no hay nada que arrancar.
 *
 * <p>Los asertos se hacen sobre los BYTES o sobre el texto decodificado en UTF-8, y
 * nunca sobre la plataforma por defecto: el formato de fichero es lo que se prueba,
 * y el BOM y la codificación son parte de él.
 */
class HorarioCsvTest {

    private static final String CABECERA =
            "Día;Tramo;Asignatura;Nombre asignatura;Profesores;Aula;Grupos;Subgrupos;"
                    + "Actividad;Plaza;Índice;Sesión";

    // ------------------------------------------------------------------ C1

    @Test
    void empiezaPorElBomUtf8YLaPrimeraLineaEsLaCabeceraExacta() {
        byte[] csv = HorarioCsv.escribir(proyeccion(List.of()));

        assertThat(new byte[] {csv[0], csv[1], csv[2]})
                .as("BOM UTF-8")
                .containsExactly((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(lineas(csv)[0]).isEqualTo(CABECERA);
    }

    // ------------------------------------------------------------------ C2

    @Test
    void escribeUnRegistroPorSesionEnElOrdenDeLaListaDeEntrada() {
        // Ids en orden DESCENDENTE a propósito: si el escritor reordenase por id (o
        // por cualquier campo natural), este aserto caería. Con 7 antes que 99 un
        // reordenamiento sería invisible.
        SesionVistaDTO primera = sesion(99L, 3, 4, "LEN", "Lengua");
        SesionVistaDTO segunda = sesion(7L, 1, 2, "MAT", "Matematicas");

        String[] lineas = lineas(HorarioCsv.escribir(proyeccion(List.of(primera, segunda))));

        assertThat(lineas).hasSize(3);
        assertThat(columna(lineas[1], "Sesión")).isEqualTo("99");
        assertThat(columna(lineas[2], "Sesión")).isEqualTo("7");
    }

    @Test
    void sinSesionesEsSoloBomCabeceraYCrlf() {
        byte[] csv = HorarioCsv.escribir(proyeccion(List.of()));

        assertThat(texto(csv)).isEqualTo(CABECERA + "\r\n");
    }

    // ------------------------------------------------------------------ C3

    @Test
    void cadaCampoCaeEnSuColumnaYLasListasSeUnenConBarra() {
        SesionVistaDTO sesion = new SesionVistaDTO(
                42L, 2, 3, 5, 1, "MAT", "Matematicas",
                List.of("P-MAT", "P-AYU"), "A-12",
                List.of(), // subgrupos VACÍA: el campo tiene que salir vacío
                List.of("1ºA", "1ºB"),
                "Mat-1ºA", "Mat-1ºA-P1");

        String[] lineas = lineas(HorarioCsv.escribir(proyeccion(List.of(sesion))));
        String fila = lineas[1];

        assertThat(columna(fila, "Día")).isEqualTo("3");
        assertThat(columna(fila, "Tramo")).isEqualTo("5");
        assertThat(columna(fila, "Asignatura")).isEqualTo("MAT");
        assertThat(columna(fila, "Nombre asignatura")).isEqualTo("Matematicas");
        assertThat(columna(fila, "Profesores")).isEqualTo("P-MAT/P-AYU");
        assertThat(columna(fila, "Aula")).isEqualTo("A-12");
        assertThat(columna(fila, "Grupos")).isEqualTo("1ºA/1ºB");
        assertThat(columna(fila, "Subgrupos")).isEmpty();
        assertThat(columna(fila, "Actividad")).isEqualTo("Mat-1ºA");
        assertThat(columna(fila, "Plaza")).isEqualTo("Mat-1ºA-P1");
        assertThat(columna(fila, "Índice")).isEqualTo("2");
        assertThat(columna(fila, "Sesión")).isEqualTo("42");
    }

    // ------------------------------------------------------------------ bloques (S170)

    /**
     * Una sesión de un bloque de dos tramos se escribe en DOS registros, uno por tramo que
     * ocupa, iguales en todo salvo la columna Tramo —misma Sesión, mismo Índice—. Al lado va
     * una sesión de un tramo, que sigue dando un único registro: el aserto de tamaño cuenta
     * las dos a la vez.
     */
    @Test
    void unaSesionDeDosTramosDaUnRegistroPorTramoIgualesSalvoElTramo() {
        SesionVistaDTO bloque = new SesionVistaDTO(
                42L, 1, 2, 3, 2, "TEC", "Tecnologia",
                List.of("P-TEC"), "T-1",
                List.of("1ºA-s1"), List.of("1ºA"),
                "Tec-1ºA", "Tec-1ºA-P1");
        SesionVistaDTO suelta = sesion(7L, 1, 5, "MAT", "Matematicas");

        String[] lineas = lineas(HorarioCsv.escribir(proyeccion(List.of(bloque, suelta))));

        assertThat(lineas).as("cabecera + 2 del bloque + 1 de la suelta").hasSize(4);
        assertThat(columna(lineas[1], "Tramo")).isEqualTo("3");
        assertThat(columna(lineas[2], "Tramo")).isEqualTo("4");
        assertThat(sinColumna(lineas[1], "Tramo")).isEqualTo(sinColumna(lineas[2], "Tramo"));
        assertThat(columna(lineas[1], "Sesión")).isEqualTo("42");
        assertThat(columna(lineas[3], "Sesión")).isEqualTo("7");
        assertThat(columna(lineas[3], "Tramo")).isEqualTo("5");
    }

    // ------------------------------------------------------------------ C4

    @Test
    void entrecomillaElCampoQueLlevaElSeparador() {
        String csv = texto(HorarioCsv.escribir(unaSesionConNombre("Física; y Química")));

        assertThat(csv).contains(";\"Física; y Química\";");
    }

    @Test
    void entrecomillaYDoblaLaComillaDeDentro() {
        String csv = texto(HorarioCsv.escribir(unaSesionConNombre("Taller \"A\"")));

        assertThat(csv).contains(";\"Taller \"\"A\"\"\";");
    }

    @Test
    void entrecomillaElCampoQueLlevaUnSaltoDeLinea() {
        String csv = texto(HorarioCsv.escribir(unaSesionConNombre("Primera\nSegunda")));

        assertThat(csv).contains(";\"Primera\nSegunda\";");
    }

    @Test
    void noEntrecomillaUnCampoConEspaciosYTildes() {
        String csv = texto(HorarioCsv.escribir(unaSesionConNombre("Educación Física")));

        assertThat(csv).contains(";Educación Física;");
        assertThat(csv).doesNotContain("\"");
    }

    // ------------------------------------------------------------------ C5

    @Test
    void todosLosRegistrosTerminanEnCrlfIncluidoElUltimo() {
        byte[] csv = HorarioCsv.escribir(proyeccion(List.of(
                sesion(1L, 1, 1, "MAT", "Matematicas"),
                sesion(2L, 1, 2, "LEN", "Lengua"))));
        String contenido = texto(csv);

        assertThat(contenido).endsWith("\r\n");
        assertThat(contenido.split("\r\n", -1))
                .as("cabecera + 2 registros + el resto vacío tras el CRLF final")
                .hasSize(4);
        // Ningún dato de esta proyección lleva saltos, así que TODA \n del fichero
        // tiene que ser la mitad de un CRLF.
        for (int i = 0; i < contenido.length(); i++) {
            if (contenido.charAt(i) == '\n') {
                assertThat(i).isGreaterThan(0);
                assertThat(contenido.charAt(i - 1)).isEqualTo('\r');
            }
        }
    }

    // ------------------------------------------------------------------ C6

    @Test
    void codificaEnUtf8LosCodigosConOrdinalYLasTildes() {
        SesionVistaDTO sesion = new SesionVistaDTO(
                1L, 1, 1, 1, 1, "EF", "Plástica",
                List.of("P-EF"), "A-1",
                List.of("3ºADi-Completo"), List.of("3ºADi"),
                "EF-3ºA", "EF-3ºA-P1");

        byte[] csv = HorarioCsv.escribir(proyeccion(List.of(sesion)));

        assertThat(texto(csv)).contains("3ºADi").contains("Plástica");
        // º = C2 BA en UTF-8 (en Latin-1 sería un solo byte BA): comprobarlo sobre el
        // texto ya decodificado no distinguiría una codificación de otra.
        assertThat(csv).containsSequence((byte) 0xC2, (byte) 0xBA);
        assertThat(csv).containsSequence(
                "Plástica".getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ C7

    @Test
    void abortaSiUnProfesorContieneLaBarraDeUnion() {
        SesionVistaDTO sesion = new SesionVistaDTO(
                1L, 1, 1, 1, 1, "MAT", "Matematicas",
                List.of("P/MAT"), "A-1",
                List.of("1ºA-s1"), List.of("1ºA"),
                "Mat-1ºA", "Mat-1ºA-P1");

        assertThatThrownBy(() -> HorarioCsv.escribir(proyeccion(List.of(sesion))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P/MAT")
                .hasMessageContaining("Profesores");
    }

    @Test
    void abortaSiUnGrupoContieneLaBarraDeUnion() {
        SesionVistaDTO sesion = new SesionVistaDTO(
                1L, 1, 1, 1, 1, "MAT", "Matematicas",
                List.of("P-MAT"), "A-1",
                List.of("1ºA-s1"), List.of("1ºA/B"),
                "Mat-1ºA", "Mat-1ºA-P1");

        assertThatThrownBy(() -> HorarioCsv.escribir(proyeccion(List.of(sesion))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1ºA/B")
                .hasMessageContaining("Grupos");
    }

    @Test
    void abortaSiUnSubgrupoContieneLaBarraDeUnion() {
        SesionVistaDTO sesion = new SesionVistaDTO(
                1L, 1, 1, 1, 1, "MAT", "Matematicas",
                List.of("P-MAT"), "A-1",
                List.of("1ºA/s1"), List.of("1ºA"),
                "Mat-1ºA", "Mat-1ºA-P1");

        assertThatThrownBy(() -> HorarioCsv.escribir(proyeccion(List.of(sesion))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1ºA/s1")
                .hasMessageContaining("Subgrupos");
    }

    // ------------------------------------------------------------------ fixture

    private static HorarioProyeccionDTO proyeccion(List<SesionVistaDTO> sesiones) {
        return new HorarioProyeccionDTO(
                1L, "Horario de prueba", "BORRADOR", "OPTIMAL", 0.0, 0.0,
                "2026-09-11T00:00:00Z", sesiones);
    }

    private static SesionVistaDTO sesion(
            Long id, int indice, int tramo, String codigo, String nombre) {
        return new SesionVistaDTO(
                id, indice, 1, tramo, 1, codigo, nombre,
                List.of("P-" + codigo), "A-" + codigo,
                List.of("1ºA-s1"), List.of("1ºA"),
                codigo + "-1ºA", codigo + "-1ºA-P1");
    }

    /** Una proyección de una sesión cuyo único rasgo es el nombre de asignatura. */
    private static HorarioProyeccionDTO unaSesionConNombre(String nombreAsignatura) {
        return proyeccion(List.of(new SesionVistaDTO(
                1L, 1, 1, 1, 1, "ASI", nombreAsignatura,
                List.of("P-ASI"), "A-1",
                List.of("1ºA-s1"), List.of("1ºA"),
                "Asi-1ºA", "Asi-1ºA-P1")));
    }

    /** El fichero sin el BOM, decodificado en UTF-8. */
    private static String texto(byte[] csv) {
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
    }

    /** Las líneas de datos, partiendo por CRLF y sin el vacío final. */
    private static String[] lineas(byte[] csv) {
        String contenido = texto(csv);
        return contenido.substring(0, contenido.length() - 2).split("\r\n", -1);
    }

    /**
     * El valor de una columna POR NOMBRE, no por posición fija: si el escritor
     * cambiara el orden de los campos sin cambiar la cabecera, el aserto caería en
     * el campo, que es donde debe caer.
     *
     * <p>Partición simple por {@code ";"}: vale porque ninguna fila que use este
     * helper lleva campos entrecomillados (los del escape se comprueban sobre el
     * texto crudo, en C4).
     */
    private static String columna(String fila, String nombre) {
        List<String> cabecera = List.of(CABECERA.split(";", -1));
        int i = cabecera.indexOf(nombre);
        if (i < 0) {
            throw new IllegalArgumentException("No existe la columna " + nombre);
        }
        String[] campos = fila.split(";", -1);
        if (campos.length != cabecera.size()) {
            throw new IllegalStateException(
                    "La fila tiene " + campos.length + " campos y la cabecera " + cabecera.size());
        }
        return campos[i];
    }

    /** La fila con esa columna quitada, para comparar dos filas en todo lo demás. */
    private static List<String> sinColumna(String fila, String nombre) {
        int i = List.of(CABECERA.split(";", -1)).indexOf(nombre);
        List<String> campos = new ArrayList<>(List.of(fila.split(";", -1)));
        campos.remove(i);
        return campos;
    }
}
