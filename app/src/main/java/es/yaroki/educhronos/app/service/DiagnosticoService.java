package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.TipoGuardia;
import es.yaroki.educhronos.app.web.dto.CeldaRefDTO;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import es.yaroki.educhronos.app.web.dto.PenalizacionDTO;
import es.yaroki.educhronos.app.web.dto.TotalesDTO;
import es.yaroki.educhronos.app.web.dto.ViolacionDTO;
import es.yaroki.educhronos.app.web.dto.ViolacionGuardiaDTO;
import es.yaroki.educhronos.solver.cpsat.AtribucionBlanda;
import es.yaroki.educhronos.solver.cpsat.CeldaRef;
import es.yaroki.educhronos.solver.cpsat.Penalizacion;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del DIAGNÓSTICO de un horario generado (Fase 8, Bloque
 * 8.3-C): reconstruye la {@link SolucionHorario} de dominio a partir de las
 * {@link Sesion} persistidas y la pasa por el {@link VerificadorSolucion} para
 * obtener las violaciones DURAS por celda, las penalizaciones BLANDAS contrafactuales
 * por celda y los totales blandos. Servicio PROPIO, fuera de
 * {@code GeneradorHorarioService} (que ya arrastra la deuda de 12 repos,
 * D-F8.2b-iii-A-a); colabora con él solo por sus métodos públicos de carga
 * reutilizables ({@link GeneradorHorarioService#cargarProblema} y
 * {@link GeneradorHorarioService#cargarHorario}), sin duplicar la carga del catálogo.
 *
 * <p><b>Frontera transaccional (CRÍTICA, misma razón que {@code cargarProblema()}, S62).</b>
 * {@link #diagnosticar} es {@code @Transactional(readOnly = true)}: la carga del problema,
 * la del horario con sus sesiones y la construcción del índice {@code Tramo → TramoSemanal}
 * ocurren TODAS dentro de la MISMA sesión de Hibernate. Es obligatorio porque
 * {@link SolucionMapper#aSolucionHorario} invierte {@code idxTramo} y cruza cada
 * {@code Sesion.getTramoInicio()} contra él por IDENTIDAD DE OBJETO de {@code TramoSemanal}
 * (que no sobreescribe {@code equals}): fuera de una única transacción, dos {@code findAll}
 * darían instancias distintas y la inversión no emparejaría.
 *
 * <p><b>Por qué delega en {@link GeneradorHorarioService#cargarProblema()} en vez de
 * cargar el catálogo por su cuenta:</b> ese método es {@code @Transactional(readOnly
 * = true)} y lee los bloqueos vigentes DENTRO de la misma transacción que el resto
 * del catálogo. Es obligatorio: {@code BloqueoMapper} cruza el pin de tramo por
 * IDENTIDAD DE OBJETO contra un {@code IdentityHashMap}, y una carga propia daría
 * instancias {@code TramoSemanal} distintas, perdiendo el pin SIN EXCEPCIÓN (S62).
 * NO reimplementar la carga aquí. Se delega por método público; NO se heredan sus
 * repositorios (D-F8.2b-iii-A-a: 12 repos inyectados).
 */
@Service
public class DiagnosticoService {

    /** Un profesor con un número de guardias distinto del suyo actual (S213). */
    public static final String REGLA_GUARDIAS_NUMERO_DISTINTO = "GUARDIAS_NUMERO_DISTINTO";

    /** Un tramo lectivo con menos guardias que el mínimo actual (S213). */
    public static final String REGLA_GUARDIAS_BAJO_MINIMO = "GUARDIAS_BAJO_MINIMO";

    /** Una guardia en un tramo que cubre alguna actividad del profesor (S213). */
    public static final String REGLA_GUARDIA_EN_TRAMO_OCUPADO = "GUARDIA_EN_TRAMO_OCUPADO";

    /** Una guardia en un «No puede» del profesor (S213). */
    public static final String REGLA_GUARDIA_EN_NO_PUEDE = "GUARDIA_EN_NO_PUEDE";

    /** Una guardia ORDINARIA en un tramo no lectivo (S213; solo ORDINARIA desde S220). */
    public static final String REGLA_GUARDIA_EN_TRAMO_NO_LECTIVO = "GUARDIA_EN_TRAMO_NO_LECTIVO";

    /** Una guardia de RECREO o de BIBLIOTECA en un tramo lectivo (S220). */
    public static final String REGLA_GUARDIA_RECREO_EN_TRAMO_LECTIVO = "GUARDIA_RECREO_EN_TRAMO_LECTIVO";

    /** Un profesor con un número de guardias de recreo distinto del suyo actual (S220). */
    public static final String REGLA_GUARDIAS_RECREO_NUMERO_DISTINTO = "GUARDIAS_RECREO_NUMERO_DISTINTO";

    /** Un recreo con menos guardias de recreo (RECREO o BIBLIOTECA) que el mínimo actual (S220). */
    public static final String REGLA_RECREO_BAJO_MINIMO = "RECREO_BAJO_MINIMO";

    /** Un recreo con menos guardias de BIBLIOTECA que el mínimo actual (S220). */
    public static final String REGLA_BIBLIOTECA_BAJO_MINIMO = "BIBLIOTECA_BAJO_MINIMO";

    /** La hora de un tramo como la escribe {@code JornadaService} (S213, nombre de un tramo no lectivo). */
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final GeneradorHorarioService generadorService;
    private final TramoSemanalRepository tramoRepository;
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    public DiagnosticoService(
            GeneradorHorarioService generadorService,
            TramoSemanalRepository tramoRepository) {
        this.generadorService = generadorService;
        this.tramoRepository = tramoRepository;
    }

    /**
     * Diagnostica el horario {@code horarioId}: reconstruye su solución y la verifica.
     * Aborta con {@link IllegalArgumentException} (→ 404 en el controlador) si el horario
     * no existe (lo propaga {@link GeneradorHorarioService#cargarHorario}).
     */
    @Transactional(readOnly = true)
    public DiagnosticoDTO diagnosticar(Long horarioId) {
        ProblemaHorario problema = generadorService.cargarProblema();
        HorarioGenerado horario = generadorService.cargarHorario(horarioId);
        List<Sesion> sesiones = horario.getSesiones();

        List<TramoSemanal> tramos = tramoRepository.findAll();
        Map<Tramo, TramoSemanal> idxTramo = SolucionMapper.indiceTramos(problema, tramos);
        SolucionHorario solucion = SolucionMapper.aSolucionHorario(problema, sesiones, idxTramo);

        ResultadoVerificacion resultado = verificador.verificar(problema, solucion);
        AtribucionBlanda atribucion = verificador.atribuirBlandas(problema, solucion);

        List<ViolacionDTO> violaciones = resultado.violaciones().stream()
                .map(v -> new ViolacionDTO(
                        v.regla().name(),
                        v.recursoCodigo(),
                        v.tramoCodigo(),
                        v.celdas().stream()
                                .map(c -> new CeldaRefDTO(c.actividadCodigo(), c.indice(), c.plazaCodigo()))
                                .toList(),
                        v.descripcion()))
                .toList();

        List<PenalizacionDTO> penalizaciones = new ArrayList<>();
        for (Map.Entry<CeldaRef, List<Penalizacion>> e : atribucion.porCelda().entrySet()) {
            CeldaRef celda = e.getKey();
            for (Penalizacion p : e.getValue()) {
                penalizaciones.add(new PenalizacionDTO(
                        p.regla().name(), celda.actividadCodigo(), celda.indice(),
                        p.tramoCodigo(), p.delta()));
            }
        }

        int ventanas = verificador.contarVentanasProfesor(problema, solucion).values().stream()
                .mapToInt(Integer::intValue).sum();
        int consecutivas = verificador.contarPenalizacionConsecutivasProfesor(problema, solucion);
        int indispBlanda = verificador.contarPenalizacionIndisponibilidadBlanda(problema, solucion);
        int aulaNoPreferida = verificador.contarPenalizacionAulaNoPreferida(problema, solucion);
        TotalesDTO totales = new TotalesDTO(ventanas, consecutivas, indispBlanda, aulaNoPreferida);

        List<Guardia> guardias = generadorService.cargarGuardias(horarioId);
        DatosCuadre cuadre = generadorService.cargarDatosCuadre();
        List<ViolacionGuardiaDTO> violacionesGuardia = new ArrayList<>(
                comprobarGuardias(problema, solucion, idxTramo, guardias, cuadre));
        violacionesGuardia.addAll(comprobarGuardiasDeRecreo(tramos, idxTramo, guardias, cuadre));

        return new DiagnosticoDTO(violaciones, penalizaciones, totales, violacionesGuardia);
    }

    /**
     * Las violaciones de las guardias guardadas de un horario (S213, condición 3 de O-guardias),
     * contra el catálogo ACTUAL: las guardias y el mínimo de hoy, que es lo que el horario tendría
     * que cumplir. Un horario anterior al esquema 6 no tiene guardias y, con mínimo mayor que 0,
     * las incumple todas; es lo correcto.
     *
     * <p><b>Código propio, no el del reparto.</b> La ocupación sale de
     * {@link VerificadorSolucion#tramosOcupados}, el mismo camino que usa {@code verificar}, y no
     * de {@code RepartoGuardias}, que la calcula a su manera: si los dos se equivocaran, tendrían
     * que equivocarse igual. «Como mucho una guardia por profesor y tramo» no se comprueba: la
     * garantiza la única de {@code guardia}.
     *
     * <p>Desde S220 solo mira las guardias ORDINARIA: las de recreo las comprueba
     * {@link #comprobarGuardiasDeRecreo}.
     */
    private static List<ViolacionGuardiaDTO> comprobarGuardias(
            ProblemaHorario problema, SolucionHorario solucion, Map<Tramo, TramoSemanal> idxTramo,
            List<Guardia> guardias, DatosCuadre cuadre) {
        Map<Long, Tramo> tramoPorId = new HashMap<>();
        idxTramo.forEach((tramo, entidad) -> tramoPorId.put(entidad.getId(), tramo));
        int minimo = cuadre.minimoGuardiasPorTramo();

        // Por profesor y tramo, la actividad que lo ocupa (la de código menor si hay varias).
        Map<String, Map<Tramo, String>> actividadEn = new HashMap<>();
        for (Map.Entry<ActividadInstancia, Tramo> colocada : solucion.asignaciones().entrySet()) {
            Actividad actividad = colocada.getKey().actividad();
            List<Tramo> cubiertos = VerificadorSolucion.tramosOcupados(
                            colocada.getValue(), actividad.duracionTramos(), problema)
                    .orElse(List.of(colocada.getValue()));
            for (Plaza plaza : actividad.plazas()) {
                for (Profesor profesor : plaza.profesores()) {
                    for (Tramo tramo : cubiertos) {
                        actividadEn.computeIfAbsent(profesor.codigo(), c -> new HashMap<>())
                                .merge(tramo, actividad.codigo(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
                    }
                }
            }
        }
        Map<String, Set<Tramo>> noPuede = new HashMap<>();
        for (RestriccionHoraria restriccion : problema.restriccionesHorarias()) {
            if (restriccion.tipo() == TipoRestriccion.DURA) {
                noPuede.computeIfAbsent(restriccion.profesor().codigo(), c -> new HashSet<>())
                        .add(restriccion.tramo());
            }
        }

        List<Guardia> ordenadas = new ArrayList<>(guardias.stream()
                .filter(g -> g.getTipo() == TipoGuardia.ORDINARIA)
                .toList());
        ordenadas.sort(Comparator.comparing((Guardia g) -> g.getProfesor().getCodigo())
                .thenComparingInt(g -> g.getTramo().getDia().ordinal())
                .thenComparingInt(g -> g.getTramo().getOrden()));
        Map<String, Integer> porProfesor = new TreeMap<>();
        Map<Tramo, Integer> porTramo = new HashMap<>();
        List<ViolacionGuardiaDTO> deCadaGuardia = new ArrayList<>();
        for (Guardia guardia : ordenadas) {
            String profesor = guardia.getProfesor().getCodigo();
            TramoSemanal entidad = guardia.getTramo();
            porProfesor.merge(profesor, 1, Integer::sum);
            if (!entidad.isEsLectivo()) {
                String codigo = nombreNoLectivo(entidad);
                deCadaGuardia.add(new ViolacionGuardiaDTO(REGLA_GUARDIA_EN_TRAMO_NO_LECTIVO, profesor, codigo,
                        profesor + " tiene guardia en tramo " + codigo + ", que no es hora de clase."));
                continue;
            }
            Tramo tramo = tramoPorId.get(entidad.getId());
            if (tramo == null) {
                throw new IllegalStateException("El tramo lectivo id=" + entidad.getId()
                        + " de una guardia no está en el índice de tramos del problema");
            }
            porTramo.merge(tramo, 1, Integer::sum);
            String nombre = PrevalidacionService.nombreDeTramo(tramo);
            String actividad = actividadEn.getOrDefault(profesor, Map.of()).get(tramo);
            if (actividad != null) {
                deCadaGuardia.add(new ViolacionGuardiaDTO(REGLA_GUARDIA_EN_TRAMO_OCUPADO, profesor, tramo.codigo(),
                        profesor + " tiene guardia en " + nombre + ", donde tiene " + actividad + "."));
            }
            if (noPuede.getOrDefault(profesor, Set.of()).contains(tramo)) {
                deCadaGuardia.add(new ViolacionGuardiaDTO(REGLA_GUARDIA_EN_NO_PUEDE, profesor, tramo.codigo(),
                        profesor + " tiene guardia en " + nombre + ", marcado como \"No puede\"."));
            }
        }

        List<ViolacionGuardiaDTO> violaciones = new ArrayList<>();
        Set<String> profesores = new TreeSet<>(cuadre.guardiasPorProfesor().keySet());
        profesores.addAll(porProfesor.keySet());
        for (String profesor : profesores) {
            int tiene = porProfesor.getOrDefault(profesor, 0);
            int corresponden = cuadre.guardiasPorProfesor().getOrDefault(profesor, 0);
            if (tiene != corresponden) {
                violaciones.add(new ViolacionGuardiaDTO(REGLA_GUARDIAS_NUMERO_DISTINTO, profesor, null,
                        profesor + " tiene " + tiene + " guardias y le corresponden " + corresponden + "."));
            }
        }
        for (Tramo tramo : problema.tramos()) {
            int deGuardia = porTramo.getOrDefault(tramo, 0);
            if (deGuardia < minimo) {
                violaciones.add(new ViolacionGuardiaDTO(REGLA_GUARDIAS_BAJO_MINIMO, null, tramo.codigo(),
                        PrevalidacionService.nombreDeTramo(tramo) + ": " + deGuardia
                                + " profesores de guardia y el mínimo es " + minimo + "."));
            }
        }
        violaciones.addAll(deCadaGuardia);
        return violaciones;
    }

    /**
     * Las violaciones de las guardias de recreo de un horario (S220, C-reparto-equilibrado, el
     * diagnóstico de la condición 4 de O-guardias-especiales), contra el catálogo ACTUAL: las
     * guardias de recreo de cada profesor y los dos mínimos por recreo de hoy.
     *
     * <p><b>Código propio, no el del reparto.</b> Los recreos los determina aquí, con los tramos de
     * la jornada: todo tramo no lectivo, por día y orden. No usa {@code RepartoGuardias} ni
     * {@code ViaRepartoGuardias}. Una guardia de BIBLIOTECA es también una guardia de recreo: cuenta
     * para el número del profesor, para el mínimo del recreo y para el de biblioteca. «Como mucho
     * una por recreo» no se comprueba: la garantiza la única de {@code guardia}.
     */
    private static List<ViolacionGuardiaDTO> comprobarGuardiasDeRecreo(
            List<TramoSemanal> tramos, Map<Tramo, TramoSemanal> idxTramo, List<Guardia> guardias,
            DatosCuadre cuadre) {
        Map<Long, Tramo> tramoPorId = new HashMap<>();
        idxTramo.forEach((tramo, entidad) -> tramoPorId.put(entidad.getId(), tramo));
        List<TramoSemanal> recreos = tramos.stream()
                .filter(t -> !t.isEsLectivo())
                .sorted(Comparator.comparingInt((TramoSemanal t) -> t.getDia().ordinal())
                        .thenComparingInt(TramoSemanal::getOrden))
                .toList();

        List<Guardia> deRecreo = new ArrayList<>(guardias.stream()
                .filter(g -> g.getTipo() != TipoGuardia.ORDINARIA)
                .toList());
        deRecreo.sort(Comparator.comparing((Guardia g) -> g.getProfesor().getCodigo())
                .thenComparingInt(g -> g.getTramo().getDia().ordinal())
                .thenComparingInt(g -> g.getTramo().getOrden()));
        Map<String, Integer> porProfesor = new TreeMap<>();
        Map<Long, Integer> porRecreo = new HashMap<>();
        Map<Long, Integer> bibliotecaPorRecreo = new HashMap<>();
        List<ViolacionGuardiaDTO> deCadaGuardia = new ArrayList<>();
        for (Guardia guardia : deRecreo) {
            String profesor = guardia.getProfesor().getCodigo();
            TramoSemanal entidad = guardia.getTramo();
            porProfesor.merge(profesor, 1, Integer::sum);
            if (entidad.isEsLectivo()) {
                Tramo tramo = tramoPorId.get(entidad.getId());
                if (tramo == null) {
                    throw new IllegalStateException("El tramo lectivo id=" + entidad.getId()
                            + " de una guardia de recreo no está en el índice de tramos del problema");
                }
                deCadaGuardia.add(new ViolacionGuardiaDTO(REGLA_GUARDIA_RECREO_EN_TRAMO_LECTIVO, profesor,
                        tramo.codigo(), profesor + " tiene guardia de recreo en " + nombreNoLectivo(entidad)
                                + ", que es hora de clase."));
                continue;
            }
            porRecreo.merge(entidad.getId(), 1, Integer::sum);
            if (guardia.getTipo() == TipoGuardia.BIBLIOTECA) {
                bibliotecaPorRecreo.merge(entidad.getId(), 1, Integer::sum);
            }
        }

        List<ViolacionGuardiaDTO> violaciones = new ArrayList<>();
        Set<String> profesores = new TreeSet<>(cuadre.guardiasRecreoPorProfesor().keySet());
        profesores.addAll(porProfesor.keySet());
        for (String profesor : profesores) {
            int tiene = porProfesor.getOrDefault(profesor, 0);
            int corresponden = cuadre.guardiasRecreoPorProfesor().getOrDefault(profesor, 0);
            if (tiene != corresponden) {
                violaciones.add(new ViolacionGuardiaDTO(REGLA_GUARDIAS_RECREO_NUMERO_DISTINTO, profesor, null,
                        profesor + " tiene " + tiene + " guardias de recreo y le corresponden " + corresponden
                                + "."));
            }
        }
        int minimo = cuadre.minimoGuardiasPorRecreo();
        for (TramoSemanal recreo : recreos) {
            int deGuardia = porRecreo.getOrDefault(recreo.getId(), 0);
            if (deGuardia < minimo) {
                String nombre = nombreNoLectivo(recreo);
                violaciones.add(new ViolacionGuardiaDTO(REGLA_RECREO_BAJO_MINIMO, null, nombre,
                        nombre + ": " + deGuardia + " profesores de guardia y el mínimo es " + minimo + "."));
            }
        }
        int minimoBiblioteca = cuadre.minimoBibliotecaPorRecreo();
        for (TramoSemanal recreo : recreos) {
            int enBiblioteca = bibliotecaPorRecreo.getOrDefault(recreo.getId(), 0);
            if (enBiblioteca < minimoBiblioteca) {
                String nombre = nombreNoLectivo(recreo);
                violaciones.add(new ViolacionGuardiaDTO(REGLA_BIBLIOTECA_BAJO_MINIMO, null, nombre,
                        nombre + ": " + enBiblioteca + " profesores de guardia en la biblioteca y el mínimo es "
                                + minimoBiblioteca + "."));
            }
        }
        violaciones.addAll(deCadaGuardia);
        return violaciones;
    }

    /** {@code LUNES 10:00-10:30}: un tramo por su día y sus horas, como lo escribe {@code JornadaService}. */
    private static String nombreNoLectivo(TramoSemanal tramo) {
        return tramo.getDia().name() + " " + HH_MM.format(tramo.getHoraInicio()) + "-"
                + HH_MM.format(tramo.getHoraFin());
    }
}
