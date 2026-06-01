package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Tramo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Imprime un horario en formato tabular según una VistaHorario.
 *
 * Para cada clave de la vista (grupo, profesor, ...) genera una sub-tabla
 * con filas = órdenes de tramo (T1..T6) y columnas = días (Lun..Vie).
 * El ancho de las columnas es dinámico, calculado sobre el contenido real,
 * sin truncamiento.
 *
 * El printer es agnóstico al tipo de vista; toda la especificidad vive en
 * la VistaHorario inyectada.
 */
final class HorarioPrinter {

    private static final String[] NOMBRES_DIA = {"Lun", "Mar", "Mié", "Jue", "Vie"};

    private HorarioPrinter() { }

    static <K> void imprimir(
            PrintStream out,
            ProblemaHorario problema,
            List<SesionMaterializada> sesiones,
            VistaHorario<K> vista) {

        out.println("--- " + vista.titulo() + " ---");

        List<Integer> diasPresentes = diasPresentesEn(problema);
        List<Integer> ordenesPresentes = ordenesPresentesEn(problema);
        Map<DiaOrden, Tramo> mapaTramos = indexarTramos(problema);

        Map<K, Map<Tramo, List<SesionMaterializada>>> indice =
                construirIndice(sesiones, vista);

        for (K clave : vista.filas(problema)) {
            Map<Tramo, List<SesionMaterializada>> porTramo =
                    indice.getOrDefault(clave, Collections.emptyMap());
            imprimirSubTabla(out, vista, clave, diasPresentes, ordenesPresentes,
                    mapaTramos, porTramo);
        }
    }

    // -- pre-cálculos --------------------------------------------------

    private static List<Integer> diasPresentesEn(ProblemaHorario problema) {
        return problema.tramos().stream()
                .map(Tramo::diaSemana).distinct().sorted().toList();
    }

    private static List<Integer> ordenesPresentesEn(ProblemaHorario problema) {
        return problema.tramos().stream()
                .map(Tramo::ordenEnDia).distinct().sorted().toList();
    }

    private static Map<DiaOrden, Tramo> indexarTramos(ProblemaHorario problema) {
        return problema.tramos().stream().collect(Collectors.toMap(
                t -> new DiaOrden(t.diaSemana(), t.ordenEnDia()),
                t -> t));
    }

    private static <K> Map<K, Map<Tramo, List<SesionMaterializada>>> construirIndice(
            List<SesionMaterializada> sesiones, VistaHorario<K> vista) {
        Map<K, Map<Tramo, List<SesionMaterializada>>> indice = new HashMap<>();
        for (SesionMaterializada s : sesiones) {
            for (K clave : vista.filasDe(s)) {
                indice.computeIfAbsent(clave, k -> new HashMap<>())
                        .computeIfAbsent(s.tramo(), t -> new ArrayList<>())
                        .add(s);
            }
        }
        return indice;
    }

    // -- impresión de una sub-tabla -----------------------------------

    private static <K> void imprimirSubTabla(
            PrintStream out,
            VistaHorario<K> vista,
            K clave,
            List<Integer> diasPresentes,
            List<Integer> ordenesPresentes,
            Map<DiaOrden, Tramo> mapaTramos,
            Map<Tramo, List<SesionMaterializada>> porTramo) {

        int nDias = diasPresentes.size();
        int nOrdenes = ordenesPresentes.size();

        // 1. Calcular contenido y ancho dinámico
        String[][] celdas = new String[nOrdenes][nDias];
        int anchoColumna = "Mié".length();    // mínimo: cabecera de día

        for (int r = 0; r < nOrdenes; r++) {
            int orden = ordenesPresentes.get(r);
            for (int c = 0; c < nDias; c++) {
                int dia = diasPresentes.get(c);
                Tramo t = mapaTramos.get(new DiaOrden(dia, orden));
                String contenido = "";
                if (t != null) {
                    List<SesionMaterializada> ses = porTramo.get(t);
                    if (ses != null && !ses.isEmpty()) {
                        // En estado válido del solver no debería haber >1
                        // sesión por (clave, tramo). Si las hubiera (violación),
                        // las apilamos con " / " para que sea visible.
                        contenido = ses.stream()
                                .map(vista::contenidoCelda)
                                .collect(Collectors.joining(" / "));
                    }
                }
                celdas[r][c] = contenido;
                anchoColumna = Math.max(anchoColumna, contenido.length());
            }
        }

        // Ancho de la columna de etiqueta de tramo (T1..T6 → 2 chars)
        int anchoEtiquetaTramo = 2;
        for (int orden : ordenesPresentes) {
            anchoEtiquetaTramo = Math.max(anchoEtiquetaTramo,
                    ("T" + orden).length());
        }

        // 2. Imprimir
        out.println();
        out.println("[ " + vista.etiquetaFila(clave) + " ]");

        // Cabecera de días
        StringBuilder cab = new StringBuilder();
        cab.append(pad("", anchoEtiquetaTramo)).append(" |");
        for (int dia : diasPresentes) {
            cab.append(" ").append(pad(NOMBRES_DIA[dia - 1], anchoColumna)).append(" |");
        }
        out.println(cab.toString());

        // Separador horizontal
        out.println(separador(anchoEtiquetaTramo, anchoColumna, nDias));

        // Filas de tramos
        for (int r = 0; r < nOrdenes; r++) {
            int orden = ordenesPresentes.get(r);
            StringBuilder fila = new StringBuilder();
            fila.append(pad("T" + orden, anchoEtiquetaTramo)).append(" |");
            for (int c = 0; c < nDias; c++) {
                fila.append(" ").append(pad(celdas[r][c], anchoColumna)).append(" |");
            }
            out.println(fila.toString());
        }
    }

    private static String separador(int anchoEtiqueta, int anchoColumna, int nDias) {
        StringBuilder sb = new StringBuilder();
        sb.append("-".repeat(anchoEtiqueta + 1)).append("+");
        for (int i = 0; i < nDias; i++) {
            sb.append("-".repeat(anchoColumna + 2)).append("+");
        }
        return sb.toString();
    }

    private static String pad(String s, int ancho) {
        if (s.length() >= ancho) return s;
        return s + " ".repeat(ancho - s.length());
    }

    private record DiaOrden(int dia, int orden) { }
}
