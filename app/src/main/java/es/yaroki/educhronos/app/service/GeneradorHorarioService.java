package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHorariaRepository;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private final HorarioGeneradoRepository horarioRepository;
    private final SesionRepository sesionRepository;

    public GeneradorHorarioService(
            TramoSemanalRepository tramoRepository,
            AulaRepository aulaRepository,
            AsignaturaRepository asignaturaRepository,
            ProfesorRepository profesorRepository,
            GrupoAdministrativoRepository grupoRepository,
            SubgrupoRepository subgrupoRepository,
            ActividadRepository actividadRepository,
            ProfesorRestriccionHorariaRepository restriccionRepository,
            HorarioGeneradoRepository horarioRepository,
            SesionRepository sesionRepository) {
        this.tramoRepository = tramoRepository;
        this.aulaRepository = aulaRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.profesorRepository = profesorRepository;
        this.grupoRepository = grupoRepository;
        this.subgrupoRepository = subgrupoRepository;
        this.actividadRepository = actividadRepository;
        this.restriccionRepository = restriccionRepository;
        this.horarioRepository = horarioRepository;
        this.sesionRepository = sesionRepository;
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

    /**
     * Persiste la salida del solver como un {@link HorarioGenerado} recuperable
     * (Fase 6, Bloque 9). Transaccional de escritura: construye los índices por
     * código (plazas y aulas) y el puente {@code Tramo → TramoSemanal}
     * ({@link SolucionMapper#indiceTramos}), crea la cabecera con la metadata del
     * resultado ({@code estadoSolver = estado().name()}, objetivo y cota) y
     * materializa una {@link Sesion} por plaza colocada. Cualquier código o tramo
     * sin correspondencia aborta ruidosamente (lo hace el mapper); no se escribe
     * ninguna Sesion con FK null.
     *
     * <p>Las plazas se obtienen navegando el agregado {@code Actividad → Plaza}
     * (no hay repositorio propio de la entidad dependiente {@code Plaza}); la
     * navegación LAZY se resuelve dentro de esta transacción.
     */
    @Transactional
    public HorarioGenerado guardar(ResultadoOptimizacion resultado, ProblemaHorario problema, String nombre) {
        Objects.requireNonNull(resultado, "resultado no puede ser null");
        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(nombre, "nombre no puede ser null");

        Map<String, Plaza> idxPlaza = actividadRepository.findAll().stream()
                .flatMap(a -> a.getPlazas().stream())
                .collect(Collectors.toMap(Plaza::getCodigo, p -> p));
        Map<String, Aula> idxAula = aulaRepository.findAll().stream()
                .collect(Collectors.toMap(Aula::getCodigo, a -> a));
        Map<Tramo, es.yaroki.educhronos.app.catalog.TramoSemanal> idxTramo =
                SolucionMapper.indiceTramos(problema, tramoRepository.findAll());

        HorarioGenerado horario = new HorarioGenerado(
                nombre, Instant.now(), resultado.estado().name(),
                resultado.objetivo(), resultado.cotaInferior());
        horarioRepository.save(horario);

        List<Sesion> sesiones = SolucionMapper.aSesiones(
                horario, problema, resultado.solucion(), idxPlaza, idxAula, idxTramo);
        sesionRepository.saveAll(sesiones);

        return horario;
    }

    /**
     * Recupera un {@link HorarioGenerado} con sus {@link Sesion} (entidades JPA).
     * No reconstruye un {@code SolucionHorario} de dominio (D-B9-5). Fuerza la
     * inicialización de las sesiones dentro de la transacción para que el
     * consumidor las lea tras cerrarse.
     */
    @Transactional(readOnly = true)
    public HorarioGenerado cargarHorario(Long id) {
        HorarioGenerado horario = horarioRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("No existe HorarioGenerado con id " + id));
        horario.getSesiones().size(); // fuerza la carga LAZY dentro de la transacción
        return horario;
    }
}
