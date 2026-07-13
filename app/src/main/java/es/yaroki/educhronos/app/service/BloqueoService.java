package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.AulaBloqueada;
import es.yaroki.educhronos.app.catalog.AulaBloqueadaRepository;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.SesionBloqueada;
import es.yaroki.educhronos.app.catalog.SesionBloqueadaRepository;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.web.dto.AulaPinDTO;
import es.yaroki.educhronos.app.web.dto.BloqueoDTO;
import es.yaroki.educhronos.app.web.dto.BloqueoRequest;
import es.yaroki.educhronos.app.web.dto.TramoRefDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación de los bloqueos manuales del centro (§4.7, Bloque
 * 8.2b-iv): alta/reemplazo, listado y borrado de un pin de TRAMO
 * ({@link SesionBloqueada}) con sus N pines de AULA ({@link AulaBloqueada}). Un
 * bloqueo es un ÚNICO recurso indivisible: no hay endpoint de pin de aula suelto,
 * porque el dominio no admite ese estado (D-1). Endpoint propio, fuera de
 * {@code GeneradorHorarioService} (D-7).
 *
 * <p><b>Validación contra el catálogo JPA (D-3), no vía {@code BloqueoMapper}.</b>
 * El alta valida las cinco reglas (actividad existe; índice 1-based; tramo lectivo
 * por (dia, ordenEnDia); plaza de la actividad; plaza de aula variable con aula
 * candidata) leyendo el catálogo directamente. Es un ESPEJO FRÁGIL, deliberado y
 * consciente, de las que {@code BloqueoMapper} ya valida sobre el dominio; se
 * compensa con el test de contrato del bloque (que da de alta por esta vía y
 * comprueba que {@code cargarProblema()} las mapea sin excepción). No se unifican:
 * unificar sería refactorizar {@code BloqueoMapper}, fuera de alcance.
 *
 * <p>El tramo se referencia por el par natural (dia 1..5, ordenEnDia 1..6, recreos
 * excluidos) que ve la UI, nunca por {@code TramoSemanal.id} (D-2). La resolución
 * (dia, orden) → {@code TramoSemanal} INVIERTE {@code CatalogoMapper.indiceOrdenEnDia}
 * (fuente única de la renumeración, deuda D30): no se reimplementa la numeración por
 * día.
 */
@Service
public class BloqueoService {

    private final ActividadRepository actividadRepository;
    private final AulaRepository aulaRepository;
    private final TramoSemanalRepository tramoRepository;
    private final SesionBloqueadaRepository sesionBloqueadaRepository;
    private final AulaBloqueadaRepository aulaBloqueadaRepository;

    public BloqueoService(
            ActividadRepository actividadRepository,
            AulaRepository aulaRepository,
            TramoSemanalRepository tramoRepository,
            SesionBloqueadaRepository sesionBloqueadaRepository,
            AulaBloqueadaRepository aulaBloqueadaRepository) {
        this.actividadRepository = actividadRepository;
        this.aulaRepository = aulaRepository;
        this.tramoRepository = tramoRepository;
        this.sesionBloqueadaRepository = sesionBloqueadaRepository;
        this.aulaBloqueadaRepository = aulaBloqueadaRepository;
    }

    /**
     * Lista todos los bloqueos como {@link BloqueoDTO} planos (D-6), simétricos al
     * body del alta y con el {@code id} que necesita el {@code DELETE}. El tramo se
     * proyecta al par (dia, ordenEnDia) invirtiendo {@code indiceOrdenEnDia}.
     */
    @Transactional(readOnly = true)
    public List<BloqueoDTO> listar() {
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramoRepository.findAll());
        List<BloqueoDTO> resultado = new ArrayList<>();
        for (SesionBloqueada pin : sesionBloqueadaRepository.findAll()) {
            resultado.add(new BloqueoDTO(
                    pin.getId(),
                    pin.getActividad().getCodigo(),
                    pin.getIndice(),
                    tramoRef(pin.getTramoInicio(), ordenEnDia),
                    aulasDe(pin.getActividad(), pin.getIndice())));
        }
        return resultado;
    }

    /**
     * Da de alta o REEMPLAZA (idempotente por instancia, D-4) un bloqueo. El body
     * describe el estado COMPLETO del pin (PUT semántico, D-5): un pin previo para
     * la misma (actividad, indice) se borra —incluidos sus pines de aula— y se
     * reescribe; {@code aulas} nulo o vacío deja el pin SIN pines de aula. Devuelve
     * el {@link BloqueoDTO} resultante. Aborta con {@link IllegalArgumentException}
     * (→ 400 en el controlador) si falla cualquiera de las reglas (a)–(e) de D-3,
     * con el dato concreto en el mensaje.
     */
    @Transactional
    public BloqueoDTO guardar(BloqueoRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");

        // (a) existe la actividad.
        Actividad actividad = actividadRepository.findByCodigo(peticion.actividadCodigo())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe actividad con codigo " + peticion.actividadCodigo()));

        // (b) indice 1-based.
        int indice = peticion.indice();
        if (indice < 1) {
            throw new IllegalArgumentException("indice debe ser >= 1; recibido " + indice);
        }

        // (c) el tramo (dia, ordenEnDia) es un tramo lectivo existente.
        if (peticion.tramo() == null) {
            throw new IllegalArgumentException("tramo es obligatorio");
        }
        List<TramoSemanal> tramos = tramoRepository.findAll();
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramos);
        TramoSemanal tramo = resolverTramo(peticion.tramo().dia(), peticion.tramo().orden(),
                tramos, ordenEnDia);

        // (d)+(e) cada pin de aula: plaza de ESTA actividad, de aula variable, aula candidata.
        List<AulaPinDTO> pinesAula = peticion.aulas() != null ? peticion.aulas() : List.of();
        List<PlazaAula> resueltos = new ArrayList<>(pinesAula.size());
        for (AulaPinDTO pin : pinesAula) {
            Plaza plaza = actividad.getPlazas().stream()
                    .filter(p -> p.getCodigo().equals(pin.plazaCodigo()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "la plaza " + pin.plazaCodigo() + " no pertenece a la actividad "
                                    + actividad.getCodigo()));
            if (plaza.getAulaFija() != null) {
                throw new IllegalArgumentException(
                        "la plaza " + pin.plazaCodigo() + " es de aula fija; no admite pin de aula");
            }
            Aula aula = aulaRepository.findByCodigo(pin.aulaCodigo())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe aula con codigo " + pin.aulaCodigo()));
            boolean candidata = plaza.getAulasCandidatas().stream()
                    .anyMatch(a -> a.getCodigo().equals(pin.aulaCodigo()));
            if (!candidata) {
                throw new IllegalArgumentException(
                        "el aula " + pin.aulaCodigo() + " no es candidata de la plaza "
                                + pin.plazaCodigo());
            }
            resueltos.add(new PlazaAula(plaza, aula));
        }

        // Reemplazo (D-4/D-5): borra el pin previo y sus aulas antes de reescribir, para
        // no chocar con la restriccion unica (actividad_id, indice).
        aulaBloqueadaRepository.deleteByActividadAndIndice(actividad, indice);
        sesionBloqueadaRepository.findByActividadAndIndice(actividad, indice)
                .ifPresent(sesionBloqueadaRepository::delete);
        sesionBloqueadaRepository.flush();

        SesionBloqueada pinTramo =
                sesionBloqueadaRepository.save(new SesionBloqueada(actividad, indice, tramo));
        for (PlazaAula pa : resueltos) {
            aulaBloqueadaRepository.save(
                    new AulaBloqueada(actividad, indice, pa.plaza(), pa.aula()));
        }

        return new BloqueoDTO(
                pinTramo.getId(),
                actividad.getCodigo(),
                indice,
                tramoRef(tramo, ordenEnDia),
                aulasDe(actividad, indice));
    }

    /**
     * Borra un bloqueo por el {@code id} de su {@link SesionBloqueada}, incluidos
     * sus pines de aula (D-1). Aborta con {@link IllegalArgumentException} (→ 404 en
     * el controlador) si el id no existe.
     */
    @Transactional
    public void borrar(Long id) {
        SesionBloqueada pin = sesionBloqueadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe bloqueo con id " + id));
        aulaBloqueadaRepository.deleteByActividadAndIndice(pin.getActividad(), pin.getIndice());
        sesionBloqueadaRepository.delete(pin);
    }

    /**
     * Resuelve el par (dia 1..5, orden = ordenEnDia 1..6) al {@link TramoSemanal}
     * lectivo correspondiente INVIRTIENDO el índice {@code getId() → ordenEnDia} de
     * {@code CatalogoMapper.indiceOrdenEnDia} (que ya excluye los recreos): un tramo
     * de recreo no aparece en el índice, así que un (dia, orden) que caiga en recreo
     * o fuera de rango no empareja y aborta con 400.
     */
    private TramoSemanal resolverTramo(int dia, int orden,
            List<TramoSemanal> tramos, Map<Long, Integer> ordenEnDia) {
        for (TramoSemanal t : tramos) {
            Integer ordenDeT = ordenEnDia.get(t.getId());
            if (ordenDeT != null && ordenDeT == orden && t.getDia().ordinal() + 1 == dia) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "No existe tramo lectivo con (dia=" + dia + ", orden=" + orden + ")");
    }

    /** Proyecta un {@link TramoSemanal} lectivo al par (dia, ordenEnDia) que ve la UI. */
    private TramoRefDTO tramoRef(TramoSemanal tramo, Map<Long, Integer> ordenEnDia) {
        Integer orden = ordenEnDia.get(tramo.getId());
        if (orden == null) {
            throw new IllegalStateException("El tramoInicio " + tramo.getId()
                    + " de un bloqueo no es lectivo (¿recreo o ausente del catálogo?)");
        }
        return new TramoRefDTO(tramo.getDia().ordinal() + 1, orden);
    }

    /** Pines de aula de una instancia, ordenados por código de plaza para salida estable. */
    private List<AulaPinDTO> aulasDe(Actividad actividad, int indice) {
        return aulaBloqueadaRepository.findByActividadAndIndice(actividad, indice).stream()
                .map(a -> new AulaPinDTO(a.getPlaza().getCodigo(), a.getAula().getCodigo()))
                .sorted(Comparator.comparing(AulaPinDTO::plazaCodigo)
                        .thenComparing(AulaPinDTO::aulaCodigo))
                .toList();
    }

    /** Par (plaza, aula) ya resuelto y validado, listo para materializar una {@link AulaBloqueada}. */
    private record PlazaAula(Plaza plaza, Aula aula) { }
}
