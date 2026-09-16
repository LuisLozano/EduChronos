package es.yaroki.educhronos.app.exportacion;

import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que {@link HorarioPdf} necesita ADEMÁS de la proyección y de la
 * {@link VistaPdf} (S149, C-exportacion-pdf-grupo; generalizado en S150). Son los datos
 * de catálogo que la proyección no lleva, ya resueltos por
 * {@code ExportacionHorarioService}: la función de documento sigue siendo pura y no
 * consulta nada.
 *
 * <p><b>Por qué un record y no cuatro parámetros sueltos.</b> Dos de los campos son
 * {@code Map<String, String>} y se confundirían en una llamada posicional sin que el
 * compilador dijese nada: pasar la línea de recurso donde van los nombres de profesor
 * daría un PDF plausible y equivocado. Con nombres de componente, ese error no se puede
 * escribir.
 *
 * <p><b>Los componentes son NEUTROS respecto de la vista</b> desde S150: el recurso de
 * página es un grupo en {@link VistaPdf#GRUPO}, y el contexto no tiene por qué saber
 * cuál. Quien traduce el recurso a un dato de catálogo es el servicio que compone esto.
 *
 * @param jornada malla horaria del centro; de aquí salen las horas de reloj de las filas
 * @param ordenDeRecursos códigos de recurso en el orden en que deben salir las páginas,
 *     tal como los lista el catálogo de esa vista (ver {@link HorarioPdf#escribir})
 * @param nombresDeProfesor código de profesor → nombre de catálogo, para la leyenda
 * @param lineaPorRecurso código de recurso → VALOR de la línea que va bajo el título, SIN
 *     su rótulo, que lo pone {@link VistaPdf#rotuloDeLinea()}. Un recurso AUSENTE del
 *     mapa es un recurso sin ese dato, y su página no lleva la línea: lo que no tiene
 *     fuente se calla, no se rellena
 */
public record ContextoPdf(
        JornadaDTO jornada,
        List<String> ordenDeRecursos,
        Map<String, String> nombresDeProfesor,
        Map<String, String> lineaPorRecurso) {
}
