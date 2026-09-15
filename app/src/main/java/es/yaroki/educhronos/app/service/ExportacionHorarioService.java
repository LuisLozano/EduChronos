package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.RolTutoria;
import es.yaroki.educhronos.app.exportacion.ContextoPdf;
import es.yaroki.educhronos.app.exportacion.HorarioPdf;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.ProfesorDTO;
import es.yaroki.educhronos.app.web.dto.TutoriaDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Compone el PDF de horario por grupo (S149, C-exportacion-pdf-grupo). Es el único sitio
 * del Cambio que conoce a la vez las cinco fuentes que la página necesita; {@link HorarioPdf}
 * es una función pura y el controlador solo enruta.
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
 *   <li>{@link GrupoService#listar()}: el ORDEN de las páginas. No se inventa aquí ni se
 *       ordena alfabéticamente por cuenta propia: se reutiliza el mismo orden con el que
 *       la aplicación lista los grupos en pantalla, para que el papel y la UI coincidan.
 *   <li>{@link TutoriaService#obtener(Long)}: el tutor de cada grupo, para la línea que
 *       va bajo el título. Un grupo sin TUTOR_PRINCIPAL no aporta entrada y su página
 *       sale sin esa línea.
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
     * El PDF de un horario, una página A4 por grupo.
     *
     * @throws IllegalArgumentException si no existe un horario con ese id (→ 404)
     */
    public byte[] pdfPorGrupo(Long horarioId) {
        HorarioProyeccionDTO proyeccion = generador.proyectar(horarioId);
        JornadaDTO jornada = jornadaService.obtenerJornada();
        Map<String, String> nombres = nombresDeProfesor();
        List<GrupoDTO> grupos = grupoService.listar();

        ContextoPdf contexto = new ContextoPdf(
                jornada,
                grupos.stream().map(GrupoDTO::codigo).toList(),
                nombres,
                tutoresPorGrupo(grupos, nombres));
        return HorarioPdf.escribir(proyeccion, contexto);
    }

    /**
     * Código de grupo → nombre del TUTOR_PRINCIPAL, para la línea de tutor de cada página.
     * Un grupo SIN tutor principal NO entra en el mapa: su página se imprime sin esa línea,
     * que es lo que corresponde a un dato ausente.
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
