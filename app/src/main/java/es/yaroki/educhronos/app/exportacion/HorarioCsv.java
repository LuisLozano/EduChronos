package es.yaroki.educhronos.app.exportacion;

import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Serializa un {@link HorarioProyeccionDTO} a CSV. Función PURA: entra el DTO que
 * devuelve {@code GeneradorHorarioService.proyectar}, salen los bytes del fichero.
 * No toca JPA, no navega entidades y no reordena: el CSV es la proyección APLANADA,
 * una línea por {@link SesionVistaDTO} en el mismo orden en que vienen (que ya es
 * {@code (dia, tramo, asignaturaCodigo)}, fijado en {@code proyectar}).
 *
 * <p><b>Por qué {@code ";"} y no {@code ","}.</b> El destino es Excel con
 * configuración regional española, donde el separador de lista del sistema es el
 * punto y coma: con comas, Excel mete la línea entera en la primera celda. El
 * separador de LISTA dentro de una celda (profesores, grupos, subgrupos) es
 * {@code "/"} por lo mismo: la coma ya la usa {@code nombre_completo} del profesor
 * («Apellidos, Nombre»), medido en el M2 de S148 —58 de 59—, así que unir por coma
 * haría ilegible justo la columna más poblada.
 *
 * <p><b>Por qué el BOM.</b> Sin BOM, Excel abre el fichero en la página de códigos
 * del sistema y las tildes y la {@code º} de los códigos de grupo salen rotas. El
 * BOM es lo que le dice que es UTF-8. NO se emite la línea {@code sep=;} que a veces
 * se recomienda para forzar el separador: al no ser el BOM lo primero del fichero,
 * Excel lo ignora y se vuelve al problema que el BOM resuelve. Se verifica sobre
 * Excel en Windows en H4; hasta entonces es una decisión razonada, no medida.
 *
 * <p>Escape RFC 4180, y sólo cuando hace falta: un campo va entre comillas si y sólo
 * si contiene el separador, una comilla doble o un salto de línea, y dentro cada
 * comilla se dobla. Espacios y tildes NO entrecomillan —no lo necesitan, y hacerlo
 * ensuciaría todas las celdas de nombres—.
 */
public final class HorarioCsv {

    /** Cabecera del fichero. El orden manda: los registros se escriben en este mismo. */
    private static final String CABECERA =
            "Día;Tramo;Asignatura;Nombre asignatura;Profesores;Aula;Grupos;Subgrupos;"
                    + "Actividad;Plaza;Índice;Sesión";

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String CRLF = "\r\n";
    private static final char SEPARADOR = ';';
    private static final String UNION_LISTA = "/";

    private HorarioCsv() {
    }

    /**
     * Bytes del CSV: BOM UTF-8 + cabecera + un registro por sesión, todos terminados
     * en CRLF (el último también).
     *
     * @throws IllegalStateException si un elemento de una lista multivalor contiene
     *     {@code "/"}, que es el separador de esa lista: el fichero mentiría sobre
     *     cuántos elementos hay y nadie podría deshacer la unión.
     */
    public static byte[] escribir(HorarioProyeccionDTO proyeccion) {
        StringBuilder texto = new StringBuilder();
        texto.append(CABECERA).append(CRLF);

        for (SesionVistaDTO sesion : proyeccion.sesiones()) {
            registro(texto,
                    Integer.toString(sesion.dia()),
                    Integer.toString(sesion.tramo()),
                    sesion.asignaturaCodigo(),
                    sesion.asignaturaNombre(),
                    unir(sesion.profesores(), "Profesores"),
                    sesion.aulaCodigo(),
                    unir(sesion.grupos(), "Grupos"),
                    unir(sesion.subgrupos(), "Subgrupos"),
                    sesion.actividadCodigo(),
                    sesion.plazaCodigo(),
                    Integer.toString(sesion.indice()),
                    Long.toString(sesion.sesionId()));
        }

        byte[] cuerpo = texto.toString().getBytes(StandardCharsets.UTF_8);
        byte[] salida = new byte[BOM.length + cuerpo.length];
        System.arraycopy(BOM, 0, salida, 0, BOM.length);
        System.arraycopy(cuerpo, 0, salida, BOM.length, cuerpo.length);
        return salida;
    }

    /** Una línea: los campos escapados, unidos por el separador, y CRLF al final. */
    private static void registro(StringBuilder texto, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                texto.append(SEPARADOR);
            }
            texto.append(escapar(campos[i]));
        }
        texto.append(CRLF);
    }

    /** Entrecomilla SI Y SÓLO SI hace falta, doblando las comillas de dentro. */
    private static String escapar(String campo) {
        boolean necesita = campo.indexOf(SEPARADOR) >= 0
                || campo.indexOf('"') >= 0
                || campo.indexOf('\r') >= 0
                || campo.indexOf('\n') >= 0;
        if (!necesita) {
            return campo;
        }
        return '"' + campo.replace("\"", "\"\"") + '"';
    }

    /**
     * Une una lista multivalor con {@code "/"}. Lista vacía da campo vacío. Un
     * elemento que contenga el propio separador aborta: es la única condición del
     * catálogo que este formato NO puede representar, y callarla produciría un CSV
     * que se lee bien y dice algo falso.
     */
    private static String unir(List<String> valores, String columna) {
        for (String valor : valores) {
            if (valor.contains(UNION_LISTA)) {
                throw new IllegalStateException("La columna " + columna
                        + " no se puede exportar: el valor «" + valor + "» contiene «"
                        + UNION_LISTA + "», que es el separador de la lista");
            }
        }
        return String.join(UNION_LISTA, valores);
    }
}
