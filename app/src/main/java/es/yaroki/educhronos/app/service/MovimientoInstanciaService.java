package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.SesionBloqueadaRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.mapper.SolucionMapper;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.GuardiaRepository;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.persistence.SesionRepository;
import es.yaroki.educhronos.app.web.dto.IntercambiarInstanciasRequest;
import es.yaroki.educhronos.app.web.dto.IntercambioRealizadoDTO;
import es.yaroki.educhronos.app.web.dto.MoverInstanciaRequest;
import es.yaroki.educhronos.app.web.dto.ReferenciaInstancia;
import es.yaroki.educhronos.app.web.dto.SesionVistaDTO;
import es.yaroki.educhronos.solver.cpsat.ReglaDura;
import es.yaroki.educhronos.solver.cpsat.ResultadoVerificacion;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.cpsat.Violacion;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 *
 * <p><b>Las guardias se recalculan enteras</b> (S214, C-ajuste-guardias, condición 5 de
 * O-guardias). Si las clases admiten el ajuste, se reparten las guardias sobre la CANDIDATA,
 * con las guardias por profesor y el mínimo de la configuración actual, ANTES de escribir nada.
 * Sin reparto, el ajuste se rechaza con {@link CausaMovimiento#GUARDIAS_SIN_REPARTO} y no se
 * escribe nada; con reparto, se escriben las sesiones y se sustituyen las guardias del horario,
 * en la misma transacción. Vale igual para un horario sin filas de guardia: recibe las suyas en
 * el primer ajuste aceptado. Las ramas que devuelven 200 sin escribir no recalculan.
 */
@Service
public class MovimientoInstanciaService {

    private final GeneradorHorarioService generadorService;
    private final TramoSemanalRepository tramoRepository;
    private final SesionRepository sesionRepository;
    private final SesionBloqueadaRepository sesionBloqueadaRepository;
    private final ActividadRepository actividadRepository;
    private final GuardiaRepository guardiaRepository;
    private final ProfesorRepository profesorRepository;
    private final VerificadorSolucion verificador = new VerificadorSolucion();

    /** Texto del rechazo sin reparto de guardias (S214, X); le sigue la lista de tramos sin mínimo. */
    static final String MENSAJE_GUARDIAS_SIN_REPARTO = "No se puede hacer este cambio: después de él no hay"
            + " forma de repartir las guardias. Tramos que no llegan al mínimo de profesores de guardia: ";

    public MovimientoInstanciaService(
            GeneradorHorarioService generadorService,
            TramoSemanalRepository tramoRepository,
            SesionRepository sesionRepository,
            SesionBloqueadaRepository sesionBloqueadaRepository,
            ActividadRepository actividadRepository,
            GuardiaRepository guardiaRepository,
            ProfesorRepository profesorRepository) {
        this.generadorService = generadorService;
        this.tramoRepository = tramoRepository;
        this.sesionRepository = sesionRepository;
        this.sesionBloqueadaRepository = sesionBloqueadaRepository;
        this.actividadRepository = actividadRepository;
        this.guardiaRepository = guardiaRepository;
        this.profesorRepository = profesorRepository;
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

        // Para decidir el 404 y como dueño de las guardias recalculadas: las filas se
        // reconsultan aparte, no se leen de su colección inversa.
        HorarioGenerado horario;
        try {
            horario = generadorService.cargarHorario(horarioId);
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
            return releerInstancia(
                horarioId, peticion.actividadCodigo(), peticion.indice(), ordenEnDia);
        }

        // ---- VEREDICTO. Un solo cargarProblema() por petición.
        ProblemaHorario problema = generadorService.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo = SolucionMapper.indiceTramos(problema, tramos);
        SolucionHorario actual = SolucionMapper.aSolucionHorario(
                problema, sesionRepository.findByHorarioId(horarioId), idxTramo);

        ResultadoVerificacion antes = verificador.verificar(problema, actual);
        SolucionHorario candidata = conInstanciaEn(
                problema, actual, idxTramo, destino,
                peticion.actividadCodigo(), peticion.indice());
        ResultadoVerificacion despues = verificador.verificar(problema, candidata);

        List<Violacion> nuevas = soloNuevas(despues.violaciones(), antes.violaciones());
        if (!nuevas.isEmpty()) {
            throw new MovimientoRechazadoException(CausaMovimiento.VIOLA_REGLA_DURA,
                    "El movimiento de (" + peticion.actividadCodigo() + ", " + peticion.indice()
                            + ") a (dia=" + peticion.dia() + ", orden=" + peticion.orden()
                            + ") provoca " + nuevas.size() + " violación(es) dura(s) nueva(s)",
                    nuevas);
        }

        // ---- GUARDIAS, sobre la candidata y antes de escribir: sin reparto no se escribe nada.
        List<Guardia> guardias = guardiasTras(horario, problema, candidata, idxTramo);

        // ---- ESCRITURA. Solo aquí, y solo el tramo: el aula de cada fila se conserva.
        for (Sesion s : filas) {
            s.moverA(destino);
        }
        sesionRepository.saveAll(filas);
        sesionRepository.flush();
        sustituirGuardias(horarioId, guardias);

        return releerInstancia(
                horarioId, peticion.actividadCodigo(), peticion.indice(), ordenEnDia);
    }

    /**
     * INTERCAMBIA los tramos de dos instancias del horario {@code horarioId} (S144,
     * C-intercambiar-instancias): cada una pasa al tramo que ocupaba la otra,
     * CONSERVANDO su aula, y devuelve las filas de ambas RELEÍDAS del repositorio.
     *
     * <p><b>No es "dos movimientos".</b> Encadenar dos {@link #mover} rechazaría casi
     * todo intercambio legal: el primero dejaría a la primera instancia encima de la
     * segunda y el veredicto vería un solape que el estado final no tiene. Por eso el
     * veredicto se emite UNA vez sobre la solución con las DOS ya permutadas, y por eso
     * {@link #conInstanciaEn} tuvo que encadenar.
     *
     * <p><b>NO hay {@code TRAMO_INEXISTENTE}</b>: el cuerpo no trae ningún par
     * (dia, orden), así que {@link #resolverTramo} no entra. El destino de cada una es
     * el {@code TramoSemanal} que la otra ya ocupa, tomado de sus propias filas.
     *
     * <p><b>Orden de las comprobaciones</b>, el mismo desempate que fijó S143 y una
     * decisión nueva:
     * <ol>
     *   <li>forma del cuerpo y {@code INSTANCIAS_IGUALES} (400) — no depende del estado;</li>
     *   <li>horario (404), filas de cada lado (404, DICIENDO cuál);</li>
     *   <li>pin de cada lado (409) — <b>antes</b> del no-op, igual que en {@code mover}:
     *       una instancia pinada está clavada, y "clavada" es un estado del recurso, no
     *       del movimiento. Dos instancias en el MISMO tramo con una pinada dan 409, no
     *       200;</li>
     *   <li>no-op: si ambas ocupan el mismo tramo, permutarlas no cambia nada y se
     *       devuelve 200 SIN escribir;</li>
     *   <li>veredicto por diferencia y, solo entonces, escritura.</li>
     * </ol>
     *
     * <p>Una sola transacción y un solo {@code cargarProblema()} para las dos mutaciones,
     * por la misma frontera crítica del javadoc de clase (S62). NO toca
     * {@code sesion_bloqueada} ni {@code aula_bloqueada}.
     */
    @Transactional
    public IntercambioRealizadoDTO intercambiar(
            Long horarioId, IntercambiarInstanciasRequest peticion) {

        Objects.requireNonNull(peticion, "peticion no puede ser null");
        ReferenciaInstancia primera = exigirReferencia(peticion.primera(), "primera");
        ReferenciaInstancia segunda = exigirReferencia(peticion.segunda(), "segunda");

        if (primera.actividadCodigo().equals(segunda.actividadCodigo())
                && primera.indice() == segunda.indice()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIAS_IGUALES,
                    "Las dos instancias son la misma (" + primera.actividadCodigo() + ", "
                            + primera.indice() + "); intercambiar algo consigo mismo no es"
                            + " una operación");
        }

        // Para decidir el 404 del horario y como dueño de las guardias recalculadas; las filas
        // se reconsultan aparte.
        HorarioGenerado horario;
        try {
            horario = generadorService.cargarHorario(horarioId);
        } catch (IllegalArgumentException e) {
            throw new MovimientoRechazadoException(
                    CausaMovimiento.HORARIO_INEXISTENTE, e.getMessage());
        }

        // Una sola lectura de tramos para TODA la petición (S62).
        List<TramoSemanal> tramos = tramoRepository.findAll();
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramos);

        // RECONSULTADAS, nunca la colección inversa del horario
        // (D-post-horario-sin-sesiones, esquivada aquí por CUARTA vez a mano).
        List<Sesion> filasPrimera = filasDe(horarioId, primera, "primera");
        List<Sesion> filasSegunda = filasDe(horarioId, segunda, "segunda");

        // El PIN manda, y se comprueba ANTES de la idempotencia: mismo desempate que S143.
        exigirSinPin(primera, "primera");
        exigirSinPin(segunda, "segunda");

        // Tramos de partida, CAPTURADOS antes de escribir nada: la escritura muta las
        // filas, y leer el destino de la otra después daría el tramo ya cambiado.
        TramoSemanal tramoPrimera = filasPrimera.get(0).getTramoInicio();
        TramoSemanal tramoSegunda = filasSegunda.get(0).getTramoInicio();

        // Idempotencia: permutar dos instancias del mismo tramo no es un intercambio.
        if (Objects.equals(tramoPrimera.getId(), tramoSegunda.getId())) {
            return releerAmbas(horarioId, primera, segunda, ordenEnDia);
        }

        // ---- VEREDICTO. Un solo cargarProblema() por petición, y UNA sola verificación
        // del después: sobre la solución con las DOS instancias ya permutadas.
        ProblemaHorario problema = generadorService.cargarProblema();
        Map<Tramo, TramoSemanal> idxTramo = SolucionMapper.indiceTramos(problema, tramos);
        SolucionHorario actual = SolucionMapper.aSolucionHorario(
                problema, sesionRepository.findByHorarioId(horarioId), idxTramo);

        ResultadoVerificacion antes = verificador.verificar(problema, actual);
        SolucionHorario candidata = conInstanciaEn(problema, actual, idxTramo, tramoSegunda,
                primera.actividadCodigo(), primera.indice());
        candidata = conInstanciaEn(problema, candidata, idxTramo, tramoPrimera,
                segunda.actividadCodigo(), segunda.indice());
        ResultadoVerificacion despues = verificador.verificar(problema, candidata);

        List<Violacion> nuevas = soloNuevas(despues.violaciones(), antes.violaciones());
        if (!nuevas.isEmpty()) {
            throw new MovimientoRechazadoException(CausaMovimiento.VIOLA_REGLA_DURA,
                    "El intercambio de (" + primera.actividadCodigo() + ", " + primera.indice()
                            + ") con (" + segunda.actividadCodigo() + ", " + segunda.indice()
                            + ") provoca " + nuevas.size() + " violación(es) dura(s) nueva(s)",
                    nuevas);
        }

        // ---- GUARDIAS, sobre la candidata y antes de escribir: sin reparto no se escribe nada.
        List<Guardia> guardias = guardiasTras(horario, problema, candidata, idxTramo);

        // ---- ESCRITURA. Solo aquí, y solo el tramo: el aula de cada fila se conserva.
        for (Sesion s : filasPrimera) {
            s.moverA(tramoSegunda);
        }
        for (Sesion s : filasSegunda) {
            s.moverA(tramoPrimera);
        }
        sesionRepository.saveAll(filasPrimera);
        sesionRepository.saveAll(filasSegunda);
        sesionRepository.flush();
        sustituirGuardias(horarioId, guardias);

        return releerAmbas(horarioId, primera, segunda, ordenEnDia);
    }

    /**
     * Las guardias del horario tras el ajuste, repartidas sobre la {@code candidata} con las
     * guardias por profesor y el mínimo de la configuración ACTUAL (S214, W). No escribe: si no
     * hay reparto, aborta con {@link CausaMovimiento#GUARDIAS_SIN_REPARTO} antes de que se toque
     * ninguna fila. El invariante roto de la segunda fase sale como en la generación
     * ({@code IllegalStateException}, un 500).
     */
    private List<Guardia> guardiasTras(HorarioGenerado horario, ProblemaHorario problema,
            SolucionHorario candidata, Map<Tramo, TramoSemanal> idxTramo) {
        RepartoGuardias.Resultado reparto = ViaRepartoGuardias.repartir(
                problema, candidata, generadorService.cargarDatosCuadre());
        if (reparto.fallo()) {
            throw new MovimientoRechazadoException(CausaMovimiento.GUARDIAS_SIN_REPARTO,
                    MENSAJE_GUARDIAS_SIN_REPARTO + ViaRepartoGuardias.listaDeTramos(reparto.deficits()));
        }
        return ViaRepartoGuardias.filas(
                horario, problema, idxTramo, profesorRepository.findAll(), reparto.asignaciones());
    }

    /**
     * Sustituye las guardias del horario por las recalculadas: borrado masivo, en el acto, y
     * después el alta (E2). Al revés, la única (horario, profesor, tramo) saltaría en las que
     * coinciden con las de antes, que son casi todas.
     */
    private void sustituirGuardias(Long horarioId, List<Guardia> guardias) {
        guardiaRepository.borrarDeHorario(horarioId);
        guardiaRepository.saveAll(guardias);
        guardiaRepository.flush();
    }

    /** Cuerpo del 200 del intercambio: {@link #releerInstancia} una vez por lado. */
    private IntercambioRealizadoDTO releerAmbas(Long horarioId, ReferenciaInstancia primera,
            ReferenciaInstancia segunda, Map<Long, Integer> ordenEnDia) {
        return new IntercambioRealizadoDTO(
                releerInstancia(horarioId, primera.actividadCodigo(), primera.indice(), ordenEnDia),
                releerInstancia(horarioId, segunda.actividadCodigo(), segunda.indice(), ordenEnDia));
    }

    /**
     * Valida la forma de un lado del cuerpo. El {@code lado} viaja en el mensaje porque
     * un 404 que no diga CUÁL de las dos instancias falta obliga a quien llama a
     * adivinar; con dos referencias en el cuerpo, "no existe" a secas no es accionable.
     */
    private static ReferenciaInstancia exigirReferencia(ReferenciaInstancia ref, String lado) {
        if (ref == null || ref.actividadCodigo() == null) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "La instancia '" + lado + "' es obligatoria y necesita actividadCodigo");
        }
        if (ref.indice() < 1) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "La instancia '" + lado + "' tiene indice " + ref.indice()
                            + "; debe ser >= 1 (1-based del dominio)");
        }
        return ref;
    }

    /** Filas de un lado, con el 404 que dice cuál falta. */
    private List<Sesion> filasDe(Long horarioId, ReferenciaInstancia ref, String lado) {
        List<Sesion> filas = sesionRepository.findParaInstancia(
                horarioId, ref.actividadCodigo(), ref.indice());
        if (filas.isEmpty()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "La instancia '" + lado + "' (" + ref.actividadCodigo() + ", "
                            + ref.indice() + ") no está en el horario " + horarioId);
        }
        return filas;
    }

    /** Rechaza un lado pinado. Se comprueba por (actividad, indice), la clave del pin. */
    private void exigirSinPin(ReferenciaInstancia ref, String lado) {
        Actividad actividadJpa = actividadRepository.findByCodigo(ref.actividadCodigo())
                .orElseThrow(() -> new MovimientoRechazadoException(
                        CausaMovimiento.INSTANCIA_INEXISTENTE,
                        "No existe actividad con codigo " + ref.actividadCodigo()
                                + " (instancia '" + lado + "')"));
        if (sesionBloqueadaRepository
                .findByActividadAndIndice(actividadJpa, ref.indice()).isPresent()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_PINADA,
                    "La instancia '" + lado + "' (" + ref.actividadCodigo() + ", "
                            + ref.indice() + ") está pinada; quita el pin antes de"
                            + " intercambiarla");
        }
    }

    /**
     * Construye EN MEMORIA la solución candidata: la misma de siempre con la instancia
     * reasignada al tramo destino. {@code aulasElegidas()} se reusa tal cual —ni el
     * movimiento ni el intercambio cambian de aula—, así que un solape de aula en el
     * destino aflora como violación nueva en vez de esconderse.
     *
     * <p><b>ENCADENA</b> (S144): toma las asignaciones de la solución que RECIBE, no de
     * ninguna de partida guardada, así que aplicarla dos veces —la segunda sobre lo que
     * devolvió la primera— compone las dos relocalizaciones. Es lo que hace
     * {@link #intercambiar}, y por eso el parámetro es el par ({@code actividadCodigo},
     * {@code indice}) y no un {@code MoverInstanciaRequest}: al intercambio no le llega
     * ningún (dia, orden) que meter en uno sintético.
     *
     * <p>La rama {@code TRAMO_INEXISTENTE} de la inversión es inalcanzable desde
     * {@link #intercambiar}: allí el destino es el tramo donde YA está la otra instancia,
     * y si ese {@code TramoSemanal} no tuviera {@code Tramo} de dominio,
     * {@code SolucionMapper.aSolucionHorario} habría abortado antes de llegar aquí.
     */
    private SolucionHorario conInstanciaEn(
            ProblemaHorario problema, SolucionHorario actual,
            Map<Tramo, TramoSemanal> idxTramo, TramoSemanal destino,
            String actividadCodigo, int indice) {

        es.yaroki.educhronos.solver.domain.Actividad actividad = problema.actividades().stream()
                .filter(a -> a.codigo().equals(actividadCodigo))
                .findFirst()
                .orElseThrow(() -> new MovimientoRechazadoException(
                        CausaMovimiento.INSTANCIA_INEXISTENTE,
                        "La actividad " + actividadCodigo + " no está en el problema"));
        if (indice > actividad.repeticionesPorSemana()) {
            throw new MovimientoRechazadoException(CausaMovimiento.INSTANCIA_INEXISTENTE,
                    "indice " + indice + " fuera de rango para la actividad " + actividadCodigo);
        }
        ActividadInstancia instancia = new ActividadInstancia(actividad, indice);

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
            disponibles.merge(sinTramoSiNoDependeDeEl(v), 1, Integer::sum);
        }
        List<Violacion> nuevas = new ArrayList<>();
        for (Violacion v : despues) {
            Violacion clave = sinTramoSiNoDependeDeEl(v);
            Integer quedan = disponibles.get(clave);
            if (quedan != null && quedan > 0) {
                disponibles.put(clave, quedan - 1);
            } else {
                nuevas.add(v);
            }
        }
        return nuevas;
    }

    /** Reglas de aula de S207 que dependen de (plaza, aula) y no del tramo, aunque lo lleven. */
    private static final Set<ReglaDura> REGLAS_SIN_TRAMO =
            EnumSet.of(ReglaDura.CAPACIDAD_AULA, ReglaDura.AULA_FUERA_DE_REGLAS);

    /**
     * Clave de comparación del veredicto (S207, C-deduccion-aulas, D). {@code CAPACIDAD_AULA} y
     * {@code AULA_FUERA_DE_REGLAS} llevan el tramo de inicio como información, pero no dependen
     * de él: mover la sesión no cambia ni su aula ni sus alumnos. Comparadas con el tramo, la
     * violación preexistente de la instancia movida saldría «nueva» en el tramo destino y
     * bloquearía todo movimiento de esa sesión. Se comparan sin él; las demás, tal cual.
     */
    private static Violacion sinTramoSiNoDependeDeEl(Violacion v) {
        return REGLAS_SIN_TRAMO.contains(v.regla())
                ? new Violacion(v.regla(), v.recursoCodigo(), null, v.celdas(), v.descripcion())
                : v;
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
            Long horarioId, String actividadCodigo, int indice, Map<Long, Integer> ordenEnDia) {

        List<Sesion> filas = sesionRepository.findParaInstancia(
                horarioId, actividadCodigo, indice);

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
                    sesion.getPlaza().getActividad().getDuracionTramos(),
                    asignatura.getCodigo(), asignatura.getNombreCompleto(),
                    plaza.getProfesores().stream().map(p -> p.getCodigo()).sorted().toList(),
                    sesion.getAula() == null ? null : sesion.getAula().getCodigo(),
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
