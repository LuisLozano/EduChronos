package es.yaroki.educhronos.app.exportacion;

import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que {@link HorarioPdf} necesita ADEMÁS de la proyección (S149,
 * C-exportacion-pdf-grupo). Son los datos de catálogo que la proyección no lleva, ya
 * resueltos por {@code ExportacionHorarioService}: la función de documento sigue siendo
 * pura y no consulta nada.
 *
 * <p><b>Por qué un record y no cuatro parámetros sueltos.</b> Dos de los campos son
 * {@code Map<String, String>} y se confundirían en una llamada posicional sin que el
 * compilador dijese nada: pasar los tutores donde van los nombres de profesor daría un
 * PDF plausible y equivocado. Con nombres de componente, ese error no se puede escribir.
 *
 * @param jornada malla horaria del centro; de aquí salen las horas de reloj de las filas
 * @param ordenDeGrupos códigos de grupo en el orden en que deben salir las páginas, tal
 *     como los lista {@code GrupoService.listar()} (ver {@link HorarioPdf#escribir})
 * @param nombresDeProfesor código de profesor → nombre de catálogo, para la leyenda
 * @param tutoresPorGrupo código de grupo → nombre del TUTOR_PRINCIPAL. Un grupo AUSENTE
 *     del mapa es un grupo sin tutor, y su página no lleva línea de tutor: lo que no
 *     tiene fuente se calla, no se rellena
 */
public record ContextoPdf(
        JornadaDTO jornada,
        List<String> ordenDeGrupos,
        Map<String, String> nombresDeProfesor,
        Map<String, String> tutoresPorGrupo) {
}
