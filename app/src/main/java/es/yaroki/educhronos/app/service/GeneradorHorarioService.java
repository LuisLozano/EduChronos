package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHorariaRepository;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.HorarioGeneradoRepository;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.web.dto.HorarioProyeccionDTO;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.solver.cpsat.ResultadoOptimizacion;
import es.yaroki.educhronos.solver.cpsat.SolverHorario;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
 * resolución por referencia fallaría EN SILENCIO. {@link #generar(Integer, Integer,
 * ViaSolver, String)} NO es transaccional: la transacción se abre y cierra en
 * {@code cargarProblema()}, y
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
     * Orquesta la generación completa de un horario y lo PERSISTE (Fase 8,
     * Bloque 8.1; cierra D29 para la vía de OPTIMIZACIÓN). Encadena
     * {@link #cargarProblema()} → resolución del solver → {@link #guardar} y
     * devuelve la cabecera persistida.
     *
     * <p><b>Frontera transaccional (deliberada).</b> Este método NO es
     * {@code @Transactional}: {@code cargarProblema()} abre y cierra su transacción
     * de solo lectura, la resolución (potencialmente larga) corre FUERA de toda
     * transacción sobre un POJO ya desligado de JPA, y {@code guardar()} abre la
     * transacción de escritura al final. Envolver el solve en una transacción
     * mantendría abierta la conexión SQLite durante la búsqueda (ver nota de clase).
     *
     * <p>Todos los parámetros son opcionales: {@code maxSegundos} y {@code semilla}
     * caen a los valores por defecto del solver cuando ambos faltan; {@code via} a
     * {@link ViaSolver#OPTIMIZACION} (única vía de 8.1); {@code nombre} a
     * {@code "Horario " + Instant.now()} si viene nulo o en blanco.
     *
     * @throws IllegalArgumentException si {@code maxSegundos} se especifica y no
     *         es estrictamente positivo.
     * @throws es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException si el
     *         problema no admite un horario factible.
     */
    public HorarioGenerado generar(Integer maxSegundos, Integer semilla, ViaSolver via, String nombre) {
        if (maxSegundos != null && maxSegundos <= 0) {
            throw new IllegalArgumentException(
                    "maxSegundos debe ser > 0 si se especifica; recibido " + maxSegundos);
        }
        ViaSolver viaEfectiva = via != null ? via : ViaSolver.OPTIMIZACION;
        String nombreEfectivo = (nombre != null && !nombre.isBlank())
                ? nombre : "Horario " + Instant.now();

        ProblemaHorario problema = cargarProblema();

        // Ambos nulos ⇒ delega en los valores por defecto del solver (no se duplican
        // aquí); si viene solo uno, el otro cae al mismo defecto (120 s / semilla 42).
        SolverHorario solver = (maxSegundos == null && semilla == null)
                ? new SolverHorario()
                : new SolverHorario(
                        maxSegundos != null ? maxSegundos : 120.0,
                        semilla != null ? semilla : 42);

        ResultadoOptimizacion resultado = switch (viaEfectiva) {
            case OPTIMIZACION -> solver.resolverOptimizandoConDetalle(problema);
        };

        return guardar(resultado, problema, nombreEfectivo);
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

    /**
     * Proyecta un {@link HorarioGenerado} a un {@link HorarioProyeccionDTO} plano,
     * listo para las vistas de Fase 7 (grupo, profesor, aula). Todo el mapeo —y la
     * navegación de las relaciones LAZY de cada {@link Sesion} (tramo, plaza,
     * asignatura, profesores, aula, subgrupos, grupos)— ocurre DENTRO de esta
     * transacción de solo lectura, con la sesión de Hibernate abierta; devolver un
     * DTO desligado evita {@code LazyInitializationException} en el consumidor
     * (mismo motivo que la frontera transaccional de {@link #cargarProblema()}).
     *
     * <p>El {@code ordenEnDia} (campo {@code tramo} del DTO, 1..6) no es un getter
     * de {@code TramoSemanal} —cuyo {@code orden} es un índice GLOBAL con recreos—,
     * sino la renumeración por día que produce {@link CatalogoMapper#indiceOrdenEnDia}
     * (fuente única, deuda D30). Se construye UNA vez por proyección y se cruza por
     * {@code tramoInicio.getId()}. Un tramo sin entrada en ese índice (no lectivo o
     * ausente del catálogo) aborta ruidosamente: nunca se proyecta con un valor por
     * defecto.
     *
     * <p>{@code grupos} es la unión SIN duplicados de los grupos de todos los
     * subgrupos de la plaza (D-F7-1), y una co-docencia se queda en UNA sola
     * {@link SesionVistaDTO} con varios {@code profesores} (D-F7-2). Profesores,
     * subgrupos y grupos van ordenados alfabéticamente para salida estable; las
     * sesiones, por {@code (dia, tramo, asignaturaCodigo)}.
     */
    @Transactional(readOnly = true)
    public HorarioProyeccionDTO proyectar(Long horarioId) {
        HorarioGenerado horario = horarioRepository.findById(horarioId).orElseThrow(() ->
                new IllegalArgumentException("No existe HorarioGenerado con id " + horarioId));

        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramoRepository.findAll());

        List<SesionVistaDTO> sesiones = new ArrayList<>();
        for (Sesion sesion : horario.getSesiones()) {
            TramoSemanal tramo = sesion.getTramoInicio();
            int dia = tramo.getDia().ordinal() + 1;
            Integer ordenTramo = ordenEnDia.get(tramo.getId());
            if (ordenTramo == null) {
                throw new IllegalStateException("El tramoInicio " + tramo.getId()
                        + " de una sesion del horario " + horarioId
                        + " no está en el índice de tramos lectivos (¿recreo o ausente del catálogo?)");
            }

            Plaza plaza = sesion.getPlaza();
            Asignatura asignatura = plaza.getAsignatura();

            List<String> profesores = plaza.getProfesores().stream()
                    .map(Profesor::getCodigo).sorted().toList();
            List<String> subgrupos = plaza.getSubgrupos().stream()
                    .map(Subgrupo::getCodigo).sorted().toList();
            List<String> grupos = plaza.getSubgrupos().stream()
                    .flatMap(sg -> sg.getGrupos().stream())
                    .map(GrupoAdministrativo::getCodigo)
                    .distinct().sorted().toList();

            sesiones.add(new SesionVistaDTO(
                    sesion.getId(), sesion.getIndice(), dia, ordenTramo,
                    asignatura.getCodigo(), asignatura.getNombreCompleto(),
                    profesores, sesion.getAula().getCodigo(),
                    subgrupos, grupos,
                    plaza.getActividad().getCodigo(), plaza.getCodigo()));
        }

        sesiones.sort(Comparator.comparingInt(SesionVistaDTO::dia)
                .thenComparingInt(SesionVistaDTO::tramo)
                .thenComparing(SesionVistaDTO::asignaturaCodigo));

        return new HorarioProyeccionDTO(
                horario.getId(), horario.getNombre(), horario.getEstado().name(),
                horario.getEstadoSolver(), horario.getObjetivo(), horario.getCotaInferior(),
                horario.getFechaGeneracion().toString(), sesiones);
    }
}
