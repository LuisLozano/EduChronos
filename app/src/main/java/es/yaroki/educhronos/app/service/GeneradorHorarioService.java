package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHorariaRepository;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación que orquesta la generación de un horario: carga el
 * catálogo desde los repositorios JPA, lo traduce al modelo puro del solver con
 * {@link CatalogoMapper} y resuelve con {@link SolverHorario}. Primer bean de
 * servicio del módulo {@code app/} (Fase 6, Bloque 8).
 *
 * <p><b>Frontera transaccional deliberada.</b> {@link #cargarProblema()} corre
 * dentro de una única transacción de solo lectura: la carga de las ocho
 * colecciones Y su mapeo ocurren ahí, porque el mapper navega relaciones LAZY por
 * IDENTIDAD DE OBJETO (el {@code grupoPadre} de un grupo, la población de un
 * subgrupo, el {@code TramoSemanal} de una restricción). Fuera de la sesión de
 * Hibernate eso lanzaría {@code LazyInitializationException}; peor aún, listas
 * cargadas en contextos de persistencia distintos darían proxies distintos y la
 * resolución por referencia fallaría EN SILENCIO. {@link #generar()} NO es
 * transaccional: la transacción se abre y cierra en {@code cargarProblema()}, y
 * la resolución (potencialmente larga) corre sobre un POJO ya desligado de JPA,
 * sin mantener abierta la conexión SQLite.
 */
@Service
public class GeneradorHorarioService {

    private final TramoSemanalRepository tramoRepository;
    private final AulaRepository aulaRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final ProfesorRepository profesorRepository;
    private final GrupoAdministrativoRepository grupoRepository;
    private final SubgrupoRepository subgrupoRepository;
    private final ActividadRepository actividadRepository;
    private final ProfesorRestriccionHorariaRepository restriccionRepository;

    public GeneradorHorarioService(
            TramoSemanalRepository tramoRepository,
            AulaRepository aulaRepository,
            AsignaturaRepository asignaturaRepository,
            ProfesorRepository profesorRepository,
            GrupoAdministrativoRepository grupoRepository,
            SubgrupoRepository subgrupoRepository,
            ActividadRepository actividadRepository,
            ProfesorRestriccionHorariaRepository restriccionRepository) {
        this.tramoRepository = tramoRepository;
        this.aulaRepository = aulaRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.profesorRepository = profesorRepository;
        this.grupoRepository = grupoRepository;
        this.subgrupoRepository = subgrupoRepository;
        this.actividadRepository = actividadRepository;
        this.restriccionRepository = restriccionRepository;
    }

    /**
     * Carga el catálogo completo y lo ensambla en un {@link ProblemaHorario}.
     * Toda la carga y el mapeo suceden dentro de esta transacción de solo lectura
     * (ver nota de frontera transaccional en el javadoc de clase). El
     * {@code ProblemaHorario} devuelto es un POJO puro, ya desligado de JPA.
     *
     * <p>La integridad referencial del catálogo (huérfanos, códigos duplicados) la
     * hace cumplir el propio {@link CatalogoMapper}; este servicio deja propagar
     * sus {@code IllegalArgumentException} / {@code IllegalStateException}.
     */
    @Transactional(readOnly = true)
    public ProblemaHorario cargarProblema() {
        return CatalogoMapper.aProblemaHorario(
                tramoRepository.findAll(),
                aulaRepository.findAll(),
                asignaturaRepository.findAll(),
                profesorRepository.findAll(),
                grupoRepository.findAll(),
                subgrupoRepository.findAll(),
                actividadRepository.findAll(),
                restriccionRepository.findAll());
    }

    /**
     * Genera un horario: carga el problema (transacción abierta y cerrada en
     * {@link #cargarProblema()}) y lo resuelve FUERA de transacción con la
     * configuración por defecto del solver (120 s, semilla 42).
     */
    public ResultadoOptimizacion generar() {
        ProblemaHorario problema = cargarProblema();
        return new SolverHorario().resolverOptimizandoConDetalle(problema);
    }
}
