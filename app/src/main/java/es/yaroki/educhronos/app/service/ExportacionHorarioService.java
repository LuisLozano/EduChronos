package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.RolTutoria;
import es.yaroki.educhronos.app.exportacion.ContextoPdf;
import es.yaroki.educhronos.app.exportacion.HorarioPdf;
import es.yaroki.educhronos.app.exportacion.VistaPdf;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.ProfesorDTO;
import es.yaroki.educhronos.app.web.dto.TutoriaDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Compone el PDF de horario (S149, C-exportacion-pdf-grupo; generalizado en S150). Es el
 * único sitio del Cambio que conoce a la vez las fuentes que la página necesita;
 * {@link HorarioPdf} es una función pura y el controlador solo enruta.
 *
 * <p><b>Las fuentes, y por qué son varias y no una.</b>
 * <ul>
 *   <li>{@link GeneradorHorarioService#proyectar(Long)}: el horario aplanado. Es la MISMA
 *       vía que usan el {@code GET /{id}/proyeccion} y el CSV de S148, no una consulta
 *       nueva.
 *   <li>{@link JornadaService#obtenerJornada()}: las horas de reloj. La proyección numera
 *       el tramo 1..6 y no dice a qué hora empieza; el papel se lee por la hora.
 *   <li>{@link ProfesorService#listar()}: los nombres de profesor para la leyenda. La
 *       proyección lleva CÓDIGOS de profesor ({@code Profesor::getCodigo}); el nombre de
 *       catálogo no viaja en ella.
 *   <li>{@link GrupoService#listar()}: el ORDEN de las páginas de {@link VistaPdf#GRUPO}.
 *       No se inventa aquí ni se ordena alfabéticamente por cuenta propia: se reutiliza
 *       el mismo orden con el que la aplicación lista los grupos en pantalla, para que el
 *       papel y la UI coincidan.
 *   <li>{@link TutoriaService#obtener(Long)}: el tutor de cada grupo, para la línea que
 *       va bajo el título en esa misma vista. Un grupo sin TUTOR_PRINCIPAL no aporta
 *       entrada y su página sale sin esa línea.
 * </ul>
 * La ASIGNATURA no se cruza: su {@code nombreCompleto} ya va en la proyección, y pedirlo
 * otra vez sería otro camino más a un dato que ya está servido.
 *
 * <p><b>Lo que este servicio NO hace.</b> No toca {@code horario.getSesiones()} ni monta
 * un tercer mapeo desde {@code Sesion}: eso duplicaría la lógica de aplanado que
 * {@code proyectar} ya resuelve dentro de su transacción, con la trampa añadida de los
 * proxies LAZY fuera de ella. Compone salidas de servicios, no entidades.
 *
 * <p><b>La única {@link IllegalArgumentException} que puede salir de aquí es la del id
 * inexistente</b>, y sale de {@code proyectar}. Es un contrato, no una casualidad:
 * {@link HorarioPdf} lanza {@link IllegalStateException} para sus propias guardas
 * precisamente para que el controlador pueda traducir la primera a 404 sin capturar de
 * más. Ni la jornada, ni el listado de profesores, ni el de grupos, ni las
 * tutorías fallan por ausencia —la jornada sintetiza su malla de referencia, y una lista
 * vacía es una lista—.
 */
@Service
public class ExportacionHorarioService {

    /** Separa los grupos de un tutor en su línea. Va con espacio: es una enumeración leída. */
    private static final String SEPARADOR_GRUPOS = ", ";

    private final GeneradorHorarioService generador;
    private final JornadaService jornadaService;
    private final ProfesorService profesorService;
    private final GrupoService grupoService;
    private final TutoriaService tutoriaService;

    public ExportacionHorarioService(GeneradorHorarioService generador,
                                     JornadaService jornadaService,
                                     ProfesorService profesorService,
                                     GrupoService grupoService,
                                     TutoriaService tutoriaService) {
        this.generador = generador;
        this.jornadaService = jornadaService;
        this.profesorService = profesorService;
        this.grupoService = grupoService;
        this.tutoriaService = tutoriaService;
    }

    /**
     * El PDF de un horario, una página A4 por recurso de la vista pedida.
     *
     * <p>Lo que depende de la vista es el CONTEXTO —qué catálogo da el orden de páginas y
     * qué dato va en la línea bajo el título—, no la serialización: por eso el reparto es
     * un {@code switch} sobre la vista que devuelve un {@link ContextoPdf}, y la llamada a
     * {@link HorarioPdf#escribir} es una sola y común. Una vista nueva añade su rama y no
     * toca nada de esto.
     *
     * @throws IllegalArgumentException si no existe un horario con ese id (→ 404)
     */
    public byte[] pdf(Long horarioId, VistaPdf vista) {
        HorarioProyeccionDTO proyeccion = generador.proyectar(horarioId);
        JornadaDTO jornada = jornadaService.obtenerJornada();
        Map<String, String> nombres = nombresDeProfesor();

        ContextoPdf contexto = switch (vista) {
            case GRUPO -> {
                List<GrupoDTO> grupos = grupoService.listar();
                yield new ContextoPdf(
                        jornada,
                        grupos.stream().map(GrupoDTO::codigo).toList(),
                        nombres,
                        tutoresPorGrupo(grupos, nombres));
            }
            // El orden de páginas sale de las CLAVES de `nombres`, que no es un atajo: ese
            // mapa se llena recorriendo `profesorService.listar()` —ordenado por código— y
            // es un LinkedHashMap, así que conserva ese orden. Volver a pedir el listado
            // solo para quedarse con los códigos sería preguntar dos veces lo mismo.
            case PROFESOR -> new ContextoPdf(
                    jornada,
                    List.copyOf(nombres.keySet()),
                    nombres,
                    gruposTutelados(grupoService.listar()));
        };
        return HorarioPdf.escribir(proyeccion, vista, contexto);
    }

    /**
     * Código de profesor → los grupos que TUTELA, unidos por {@value #SEPARADOR_GRUPOS},
     * para la línea que va bajo el título en {@link VistaPdf#PROFESOR}.
     *
     * <p>Es el REVERSO de {@link #tutoresPorGrupo}: la misma fuente —{@code TutoriaService}
     * y el mismo rol TUTOR_PRINCIPAL— leída al revés. Se invierte aquí y no se pide de otra
     * manera porque una segunda vía a las tutorías podría discrepar de la primera, y dos
     * páginas del mismo PDF dirían cosas distintas del mismo hecho.
     *
     * <p>El orden de los grupos de un profesor es el de {@code GrupoService.listar()},
     * heredado del recorrido: el mismo orden con el que salen las páginas de la vista de
     * grupo, y no el azar de un mapa. Un profesor SIN tutoría no entra en el mapa y su
     * página se imprime sin esa línea, igual que un grupo sin tutor en la vista de grupo.
     *
     * <p>El {@code TutoriaDTO} trae el CÓDIGO del profesor ({@code TutoriaDTO#profesor()}),
     * que es justo la clave que la vista de profesor usa como recurso de página: no hace
     * falta resolver ningún id.
     */
    private Map<String, String> gruposTutelados(List<GrupoDTO> grupos) {
        Map<String, List<String>> porProfesor = new LinkedHashMap<>();
        for (GrupoDTO grupo : grupos) {
            for (TutoriaDTO tutoria : tutoriaService.obtener(grupo.id())) {
                if (RolTutoria.TUTOR_PRINCIPAL.name().equals(tutoria.rol())) {
                    porProfesor.computeIfAbsent(tutoria.profesor(), c -> new ArrayList<>())
                            .add(grupo.codigo());
                    break;
                }
            }
        }
        Map<String, String> lineas = new LinkedHashMap<>();
        porProfesor.forEach((codigo, suyos) ->
                lineas.put(codigo, String.join(SEPARADOR_GRUPOS, suyos)));
        return lineas;
    }

    /**
     * Código de grupo → nombre del TUTOR_PRINCIPAL, para la línea de tutor de cada página
     * de {@link VistaPdf#GRUPO}. Un grupo SIN tutor principal NO entra en el mapa: su
     * página se imprime sin esa línea, que es lo que corresponde a un dato ausente.
     *
     * <p>Se pregunta grupo a grupo por {@code TutoriaService.obtener} en vez de barrer el
     * repositorio de tutorías: son 28 consultas en un endpoint de informe, y a cambio esta
     * clase sigue componiendo SERVICIOS y no entidades —y hereda gratis el orden por rol
     * que aquél garantiza, con el principal primero—.
     *
     * <p>El {@code TutoriaDTO} trae el CÓDIGO del profesor, no su nombre, así que se
     * resuelve contra el mismo mapa de nombres que usa la leyenda: una sola fuente para
     * los dos sitios donde la página nombra a una persona. Un código sin nombre en
     * catálogo se queda como código, igual que en la leyenda.
     */
    private Map<String, String> tutoresPorGrupo(List<GrupoDTO> grupos,
                                                Map<String, String> nombresProfesor) {
        Map<String, String> tutores = new LinkedHashMap<>();
        for (GrupoDTO grupo : grupos) {
            for (TutoriaDTO tutoria : tutoriaService.obtener(grupo.id())) {
                if (RolTutoria.TUTOR_PRINCIPAL.name().equals(tutoria.rol())) {
                    tutores.put(grupo.codigo(),
                            nombresProfesor.getOrDefault(tutoria.profesor(), tutoria.profesor()));
                    break;
                }
            }
        }
        return tutores;
    }

    /**
     * Código de profesor → nombre de catálogo. {@link LinkedHashMap} y no
     * {@code Collectors.toMap} para no depender de qué hace ese colector ante un código
     * repetido: aquí el último gana, explícitamente. El código es único en la tabla
     * ({@code unique} en el esquema), así que el caso no debería darse.
     */
    private Map<String, String> nombresDeProfesor() {
        Map<String, String> nombres = new LinkedHashMap<>();
        for (ProfesorDTO profesor : profesorService.listar()) {
            nombres.put(profesor.codigo(), profesor.nombreCompleto());
        }
        return nombres;
    }
}
