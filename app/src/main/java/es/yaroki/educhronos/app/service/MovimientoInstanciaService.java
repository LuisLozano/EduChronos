package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.SesionBloqueadaRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.web.dto.MoverInstanciaRequest;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.cpsat.Violacion;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recoloca una INSTANCIA de actividad en otro tramo, con el veredicto emitido por el
 * SERVIDOR antes de escribir nada (S143, C-mover-sesion-backend).
 *
 * <p><b>Servicio PROPIO, fuera de {@code GeneradorHorarioService}</b>, por el mismo
 * motivo que {@code DiagnosticoService}: aquel ya arrastra 13 repositorios inyectados
 * (D-F8.2b-iii-A-a) y no se le añade nada. Colabora con él solo por sus métodos
 * públicos de carga ({@link GeneradorHorarioService#cargarProblema} y
 * {@link GeneradorHorarioService#cargarHorario}), sin duplicar la carga del catálogo.
 *
 * <p><b>Frontera transaccional (CRÍTICA, S62).</b> {@link #mover} es
 * {@code @Transactional}: la carga del problema, la del horario con sus sesiones, el
 * índice {@code Tramo → TramoSemanal} y la escritura ocurren TODAS dentro de la MISMA
 * sesión de Hibernate, y {@code cargarProblema()} se invoca UNA sola vez por petición.
 * Es obligatorio: {@code SolucionMapper.aSolucionHorario} cruza cada
 * {@code Sesion.getTramoInicio()} contra {@code idxTramo} por IDENTIDAD DE OBJETO de
 * {@code TramoSemanal} (que no sobreescribe {@code equals}), y {@code BloqueoMapper}
 * cruza el pin contra un {@code IdentityHashMap}; dos cargas separadas darían
 * instancias distintas, la inversión no emparejaría y el pin se perdería SIN
 * EXCEPCIÓN. La lista {@code tramoRepository.findAll()} se toma una vez y se reusa
 * por eso mismo.
 *
 * <p><b>El veredicto es por DIFERENCIA, no por filtro.</b> Se verifica la solución tal
 * como está, se construye EN MEMORIA la candidata con la instancia en el tramo nuevo,
 * se verifica otra vez, y se rechaza solo con las violaciones que APARECEN. Un horario
 * generado con violaciones preexistentes (o con S8 incumplido, que ni siquiera mira la
 * solución) no puede quedar congelado: el usuario solo responde de lo que su
 * movimiento causa. La resta es de MULTICONJUNTOS y se apoya en que {@code Violacion}
 * y {@code CeldaRef} son records con {@code equals} por valor en toda su profundidad
 * ({@code ReglaDura} es enum, {@code celdas} es una lista inmutable de records); el
 * orden de la lista de violaciones NO es estable entre pasadas —el verificador agrupa
 * por {@code HashMap} de recurso—, por eso se resta por conteo y nunca por posición.
 *
 * <p><b>El aula no cambia.</b> La candidata reusa {@code aulasElegidas()} tal cual y
 * solo reasigna el tramo, así que un {@code SOLAPE_AULA} en el destino es una
 * violación nueva legítima y el movimiento se rechaza. Cambiar de aula es otra
 * operación.
 */
@Service
public class MovimientoInstanciaService {

    private final GeneradorHorarioService generadorService;
    private final TramoSemanalRepository tramoRepository;
    private final SesionRepository sesionRepository;
    private final SesionBloqueadaRepository sesionBloqueadaRepository;
    private final ActividadRepository actividadRepository;
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    public MovimientoInstanciaService(
            GeneradorHorarioService generadorService,
            TramoSemanalRepository tramoRepository,
            SesionRepository sesionRepository,
            SesionBloqueadaRepository sesionBloqueadaRepository,
            ActividadRepository actividadRepository) {
        this.generadorService = generadorService;
        this.tramoRepository = tramoRepository;
        this.sesionRepository = sesionRepository;
        this.sesionBloqueadaRepository = sesionBloqueadaRepository;
        this.actividadRepository = actividadRepository;
    }

    /**
     * Mueve la instancia ({@code actividadCodigo}, {@code indice}) del horario
     * {@code horarioId} al tramo ({@code dia}, {@code orden}), conservando el aula de
     * cada fila, y devuelve las filas resultantes RELEÍDAS del repositorio.
     *
     * <p>Es IDEMPOTENTE: mover al tramo que ya ocupa devuelve 200 sin escribir.
     *
     * <p>Aborta con {@link MovimientoRechazadoException} y su {@link CausaMovimiento}
     * en todo lo demás. NADA se escribe antes de tener el veredicto, y este servicio
     * NO toca {@code sesion_bloqueada} ni {@code aula_bloqueada}: los pines no son de
     * este Cambio.
     */
    @Transactional
    public List<SesionVistaDTO> mover(Long horarioId, MoverInstanciaRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.actividadCodigo() == null) {
            throw new MovimientoRechazadoException(
                    CausaMovimiento.INSTANCIA_INEXISTENTE, "actividadCodigo es obligatorio");
        }
        if (peticion.indice() < 1) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "indice debe ser >= 1 (1-based del dominio); recibido " + peticion.indice());
        }

        // Solo para decidir el 404: las filas se reconsultan aparte, no se leen de su
        // colección inversa.
        try {
            generadorService.cargarHorario(horarioId);
        } catch (IllegalArgumentException e) {
            throw new MovimientoRechazadoException(
                    CausaMovimiento.HORARIO_INEXISTENTE, e.getMessage());
        }

        // Una sola lectura de tramos para TODA la petición: el índice de la solución y
        // la resolución del destino tienen que hablar de las MISMAS instancias (S62).
        List<TramoSemanal> tramos = tramoRepository.findAll();
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramos);
        TramoSemanal destino = resolverTramo(peticion.dia(), peticion.orden(), tramos, ordenEnDia);

        // RECONSULTADAS, no la colección inversa del horario: esa refleja lo que
        // Hibernate tenga cargado y puede venir vacía (D-post-horario-sin-sesiones).
        List<Sesion> filas = sesionRepository.findParaInstancia(
                horarioId, peticion.actividadCodigo(), peticion.indice());
        if (filas.isEmpty()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "El horario " + horarioId + " no tiene la instancia ("
                            + peticion.actividadCodigo() + ", " + peticion.indice() + ")");
        }

        // El PIN manda sobre el arrastre y se comprueba ANTES que la idempotencia: una
        // instancia pinada está clavada, y "clavada" es un estado del recurso, no del
        // movimiento. Se comprueba por (actividad, indice), la clave del pin; NO se
        // escribe en sesion_bloqueada.
        Actividad actividadJpa = actividadRepository.findByCodigo(peticion.actividadCodigo())
                .orElseThrow(() -> new MovimientoRechazadoException(
                        CausaMovimiento.INSTANCIA_INEXISTENTE,
                        "No existe actividad con codigo " + peticion.actividadCodigo()));
        if (sesionBloqueadaRepository
                .findByActividadAndIndice(actividadJpa, peticion.indice()).isPresent()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_PINADA,
                    "La instancia (" + peticion.actividadCodigo() + ", " + peticion.indice()
                            + ") está pinada; quita el pin antes de moverla");
        }

        // Idempotencia: mover a donde ya está no es un movimiento y no escribe.
        boolean yaEnDestino = filas.stream()
                .allMatch(s -> Objects.equals(s.getTramoInicio().getId(), destino.getId()));
        if (yaEnDestino) {
            return releerInstancia(horarioId, peticion, ordenEnDia);
        }

        // ---- VEREDICTO. Un solo cargarProblema() por petición.
        ProblemaHorario problema = generadorService.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo = SolucionMapper.indiceTramos(problema, tramos);
        SolucionHorario actual = SolucionMapper.aSolucionHorario(
                problema, sesionRepository.findByHorarioId(horarioId), idxTramo);

        ResultadoVerificacion antes = verificador.verificar(problema, actual);
        SolucionHorario candidata = conInstanciaEn(problema, actual, idxTramo, destino, peticion);
        ResultadoVerificacion despues = verificador.verificar(problema, candidata);

        List<Violacion> nuevas = soloNuevas(despues.violaciones(), antes.violaciones());
        if (!nuevas.isEmpty()) {
            throw new MovimientoRechazadoException(CausaMovimiento.VIOLA_REGLA_DURA,
                    "El movimiento de (" + peticion.actividadCodigo() + ", " + peticion.indice()
                            + ") a (dia=" + peticion.dia() + ", orden=" + peticion.orden()
                            + ") provoca " + nuevas.size() + " violación(es) dura(s) nueva(s)",
                    nuevas);
        }

        // ---- ESCRITURA. Solo aquí, y solo el tramo: el aula de cada fila se conserva.
        for (Sesion s : filas) {
            s.moverA(destino);
        }
        sesionRepository.saveAll(filas);
        sesionRepository.flush();

        return releerInstancia(horarioId, peticion, ordenEnDia);
    }

    /**
     * Construye EN MEMORIA la solución candidata: la misma de siempre con la instancia
     * reasignada al tramo destino. {@code aulasElegidas()} se reusa tal cual —el
     * movimiento no cambia de aula—, así que un solape de aula en el destino aflora
     * como violación nueva en vez de esconderse.
     */
    private SolucionHorario conInstanciaEn(
            ProblemaHorario problema, SolucionHorario actual,
            Map<Tramo, TramoSemanal> idxTramo, TramoSemanal destino,
            MoverInstanciaRequest peticion) {

        es.yaroki.educhronos.solver.domain.Actividad actividad = problema.actividades().stream()
                .filter(a -> a.codigo().equals(peticion.actividadCodigo()))
                .findFirst()
                .orElseThrow(() -> new MovimientoRechazadoException(
                        CausaMovimiento.INSTANCIA_INEXISTENTE,
                        "La actividad " + peticion.actividadCodigo() + " no está en el problema"));
        if (peticion.indice() > actividad.repeticionesPorSemana()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "indice " + peticion.indice() + " fuera de rango para la actividad "
                            + peticion.actividadCodigo());
        }
        ActividadInstancia instancia = new ActividadInstancia(actividad, peticion.indice());

        // Inversión de idxTramo por ID, no por identidad de objeto: ambos lados salen de
        // la MISMA lista de tramos de esta transacción, así que son la misma instancia;
        // comparar el id lo deja explícito y no depende de esa coincidencia.
        Tramo tramoDestino = idxTramo.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().getId(), destino.getId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new MovimientoRechazadoException(
                        CausaMovimiento.TRAMO_INEXISTENTE,
                        "El tramo destino no tiene Tramo de dominio (¿recreo o no lectivo?)"));

        Map<ActividadInstancia, Tramo> asignaciones = new HashMap<>(actual.asignaciones());
        asignaciones.put(instancia, tramoDestino);
        return new SolucionHorario(asignaciones, actual.aulasElegidas());
    }

    /**
     * Violaciones del DESPUÉS que no estaban en el ANTES, restando MULTICONJUNTOS: dos
     * violaciones iguales por valor en el antes tapan dos en el después, no una. Nunca
     * por posición ni por índice: el orden que produce el verificador no es estable
     * (agrupa por {@code HashMap} de recurso). Preserva el orden del después.
     */
    private static List<Violacion> soloNuevas(List<Violacion> despues, List<Violacion> antes) {
        Map<Violacion, Integer> disponibles = new HashMap<>();
        for (Violacion v : antes) {
            disponibles.merge(v, 1, Integer::sum);
        }
        List<Violacion> nuevas = new ArrayList<>();
        for (Violacion v : despues) {
            Integer quedan = disponibles.get(v);
            if (quedan != null && quedan > 0) {
                disponibles.put(v, quedan - 1);
            } else {
                nuevas.add(v);
            }
        }
        return nuevas;
    }

    /**
     * Filas de la instancia RECONSULTADAS al repositorio, nunca la colección inversa de
     * la entidad en memoria (D-post-horario-sin-sesiones): mismo patrón que
     * {@code BloqueoService.aulasDe}, que también reconsulta en vez de fiarse de lo
     * que tenga cargado el {@code HorarioGenerado}.
     *
     * <p>La forma es la de {@code GET /{id}/proyeccion} para esa instancia: los mismos
     * {@link SesionVistaDTO}, con el mismo {@code ordenEnDia} de
     * {@code CatalogoMapper.indiceOrdenEnDia} y las mismas listas ordenadas. Es un
     * ESPEJO deliberado del mapeo de {@code GeneradorHorarioService.proyectar} —no se
     * le añade lógica a ese servicio (D-F8.2b-iii-A-a)—, y se compensa con el test de
     * contrato que compara ambas salidas para la misma instancia.
     */
    private List<SesionVistaDTO> releerInstancia(
            Long horarioId, MoverInstanciaRequest peticion, Map<Long, Integer> ordenEnDia) {

        List<Sesion> filas = sesionRepository.findParaInstancia(
                horarioId, peticion.actividadCodigo(), peticion.indice());

        List<SesionVistaDTO> salida = new ArrayList<>(filas.size());
        for (Sesion sesion : filas) {
            TramoSemanal tramo = sesion.getTramoInicio();
            Integer ordenTramo = ordenEnDia.get(tramo.getId());
            if (ordenTramo == null) {
                throw new IllegalStateException("El tramoInicio " + tramo.getId()
                        + " de una sesion del horario " + horarioId
                        + " no está en el índice de tramos lectivos (¿recreo o ausente del catálogo?)");
            }
            var plaza = sesion.getPlaza();
            var asignatura = plaza.getAsignatura();
            salida.add(new SesionVistaDTO(
                    sesion.getId(), sesion.getIndice(), tramo.getDia().ordinal() + 1, ordenTramo,
                    asignatura.getCodigo(), asignatura.getNombreCompleto(),
                    plaza.getProfesores().stream().map(p -> p.getCodigo()).sorted().toList(),
                    sesion.getAula().getCodigo(),
                    plaza.getSubgrupos().stream().map(sg -> sg.getCodigo()).sorted().toList(),
                    plaza.getSubgrupos().stream()
                            .flatMap(sg -> sg.getGrupos().stream())
                            .map(g -> g.getCodigo())
                            .distinct().sorted().toList(),
                    plaza.getActividad().getCodigo(), plaza.getCodigo()));
        }
        salida.sort(Comparator.comparingInt(SesionVistaDTO::dia)
                .thenComparingInt(SesionVistaDTO::tramo)
                .thenComparing(SesionVistaDTO::plazaCodigo));
        return salida;
    }

    /**
     * Resuelve (dia 1..5, orden = ordenEnDia 1..6) al {@link TramoSemanal} lectivo,
     * INVIRTIENDO {@code CatalogoMapper.indiceOrdenEnDia} igual que
     * {@code BloqueoService.resolverTramo}: un recreo no aparece en ese índice, así que
     * un par que caiga en recreo o fuera de rango no empareja y sale por
     * {@link CausaMovimiento#TRAMO_INEXISTENTE}. No se reimplementa la numeración.
     */
    private TramoSemanal resolverTramo(int dia, int orden,
            List<TramoSemanal> tramos, Map<Long, Integer> ordenEnDia) {
        for (TramoSemanal t : tramos) {
            Integer ordenDeT = ordenEnDia.get(t.getId());
            if (ordenDeT != null && ordenDeT == orden && t.getDia().ordinal() + 1 == dia) {
                return t;
            }
        }
        throw new MovimientoRechazadoException(CausaMovimiento.TRAMO_INEXISTENTE,
                "No existe tramo lectivo con (dia=" + dia + ", orden=" + orden + ")");
    }
}
