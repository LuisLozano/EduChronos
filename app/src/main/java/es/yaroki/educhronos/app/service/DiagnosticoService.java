package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.web.dto.CeldaRefDTO;
import es.yaroki.educhronos.app.web.dto.DiagnosticoDTO;
import es.yaroki.educhronos.app.web.dto.PenalizacionDTO;
import es.yaroki.educhronos.app.web.dto.TotalesDTO;
import es.yaroki.educhronos.app.web.dto.ViolacionDTO;
import es.yaroki.educhronos.solver.cpsat.AtribucionBlanda;
import es.yaroki.educhronos.solver.cpsat.CeldaRef;
import es.yaroki.educhronos.solver.cpsat.Penalizacion;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        TotalesDTO totales = new TotalesDTO(ventanas, consecutivas, indispBlanda);

        return new DiagnosticoDTO(violaciones, penalizaciones, totales);
    }
}
