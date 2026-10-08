package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
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

    /** Una guardia en un tramo no lectivo (S213). */
    public static final String REGLA_GUARDIA_EN_TRAMO_NO_LECTIVO = "GUARDIA_EN_TRAMO_NO_LECTIVO";

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

        Map<Tramo, TramoSemanal> idxTramo =
                SolucionMapper.indiceTramos(problema, tramoRepository.findAll());
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

        List<ViolacionGuardiaDTO> violacionesGuardia = comprobarGuardias(problema, solucion, idxTramo,
                generadorService.cargarGuardias(horarioId), generadorService.cargarDatosCuadre());

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

        List<Guardia> ordenadas = new ArrayList<>(guardias);
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
                String codigo = entidad.getDia().name() + " " + HH_MM.format(entidad.getHoraInicio()) + "-"
                        + HH_MM.format(entidad.getHoraFin());
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
}
