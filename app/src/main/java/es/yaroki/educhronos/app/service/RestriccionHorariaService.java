package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHoraria;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHorariaRepository;
import es.yaroki.educhronos.app.catalog.TipoRestriccion;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.web.dto.RestriccionHorariaDTO;
import es.yaroki.educhronos.app.web.dto.RestriccionHorariaRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del sub-recurso RESTRICCIONES HORARIAS de un profesor (§4.3,
 * Bloque 8.5-E): consulta y REEMPLAZO TOTAL de las indisponibilidades de un profesor.
 * Réplica estructural de {@link TutoriaService} (S77), que a su vez replica
 * {@code AsignaturaService.reemplazarAulasCompatibles} (S75): el {@code PUT} no edita
 * fila a fila, borra lo que hay y escribe lo que llega, y es IDEMPOTENTE.
 *
 * <p><b>Orden de validación</b> (importa: el primer fallo gana y determina el HTTP):
 * <ol>
 *   <li>profesor inexistente por id → {@link NoSuchElementException} (→ 404);
 *   <li>{@code (dia, ordenEnDia)} que no casa con ningún tramo LECTIVO →
 *       {@link IllegalArgumentException} (→ 400) que nombra el par;
 *   <li>{@code tipo} no parseable a {@link TipoRestriccion} → 400 que NOMBRA el valor
 *       malo y lista los válidos (patrón {@code parseTipoAula});
 *   <li>el mismo {@code (dia, ordenEnDia)} repetido en la lista entrante → 400 que lo
 *       nombra.
 * </ol>
 * Cada paso se resuelve en una pasada COMPLETA sobre la lista antes de empezar el
 * siguiente (los pasos 2 y 3 son dos pasadas, no un bucle único por elemento): así el
 * orden de arriba se cumple ENTRE ELEMENTOS DISTINTOS y no depende de en qué posición
 * venga cada fallo. Un cuerpo con tipo malo en el elemento 1 y tramo inexistente en el
 * 2 falla por el tramo, no por el tipo.
 *
 * <p><b>Todo se valida SOBRE LA LISTA ENTRANTE y ANTES de escribir nada.</b> No se
 * consulta el estado actual del profesor: como el {@code PUT} es un reemplazo total, lo
 * que ya hubiera en la tabla va a desaparecer y no puede entrar en conflicto con lo
 * entrante. Y no hay red debajo para el duplicado: {@code profesor_restriccion_horaria}
 * NO tiene UNIQUE (profesor, tramo) —su PK es el id sintético— así que si esta guarda se
 * retira, la escritura PASA y la base queda con dos filas para el mismo par sin que nada
 * proteste. La guarda es lo ÚNICO que sostiene la regla (verificado por mutación, A5).
 *
 * <p><b>El tramo se referencia por (dia, ordenEnDia)</b>, no por {@code TramoSemanal.id}
 * (D-2), INVIRTIENDO {@code CatalogoMapper.indiceOrdenEnDia} igual que
 * {@code BloqueoService.resolverTramo}: fuente única de la renumeración (deuda D30), aquí
 * no se reimplementa. Como ese índice ya excluye los recreos, un par que caiga en recreo
 * simplemente no empareja y da 400.
 */
@Service
public class RestriccionHorariaService {

    /**
     * Peso con el que se escribe TODA restricción, sin excepción: el API no lo expone
     * (ver {@link RestriccionHorariaRequest}). Es el peso unitario que
     * {@code ModeloCpSat.PESO_INDISP_BLANDA} asume para las BLANDA; en las DURA el campo
     * es semánticamente irrelevante (el solver no lo lee) pero la columna es NOT NULL, de
     * modo que también recibe 1. No se escribe 0 a propósito: dejaría a las BLANDA sin
     * penalización efectiva si el solver empezara a ponderar por él.
     */
    static final int PESO_POR_DEFECTO = 1;

    private final ProfesorRepository profesorRepositorio;
    private final TramoSemanalRepository tramoRepositorio;
    private final ProfesorRestriccionHorariaRepository restriccionRepositorio;

    public RestriccionHorariaService(
            ProfesorRepository profesorRepositorio,
            TramoSemanalRepository tramoRepositorio,
            ProfesorRestriccionHorariaRepository restriccionRepositorio) {
        this.profesorRepositorio = profesorRepositorio;
        this.tramoRepositorio = tramoRepositorio;
        this.restriccionRepositorio = restriccionRepositorio;
    }

    /**
     * Las restricciones horarias de un profesor, ORDENADAS por (dia, ordenEnDia).
     * {@link NoSuchElementException} (→ 404) si el profesor no existe. Lista vacía =
     * profesor sin restricciones (no es un 404).
     */
    @Transactional(readOnly = true)
    public List<RestriccionHorariaDTO> obtener(Long idProfesor) {
        Profesor profesor = profesorRepositorio.findById(idProfesor)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe profesor con id " + idProfesor));
        Map<Long, Integer> ordenEnDia =
                CatalogoMapper.indiceOrdenEnDia(tramoRepositorio.findAll());
        return ordenar(restriccionRepositorio.findByProfesor(profesor), ordenEnDia);
    }

    /**
     * REEMPLAZO TOTAL e idempotente de las restricciones horarias de un profesor: borra
     * las filas actuales y crea las de {@code peticiones}. Lista vacía o nula = borrar
     * todas (profesor sin restricciones), y es un {@code PUT} legítimo, no un error. Ver
     * el orden de validación en el Javadoc de clase. Devuelve la lista resultante
     * ordenada por (dia, ordenEnDia).
     */
    @Transactional
    public List<RestriccionHorariaDTO> reemplazar(
            Long idProfesor, List<RestriccionHorariaRequest> peticiones) {

        Profesor profesor = profesorRepositorio.findById(idProfesor)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe profesor con id " + idProfesor));

        List<RestriccionHorariaRequest> entrantes =
                peticiones == null ? List.<RestriccionHorariaRequest>of() : peticiones;

        List<TramoSemanal> tramos = tramoRepositorio.findAll();
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramos);

        // Pasada 1: resolver el tramo de TODOS los elementos (400). Completa, no fundida
        // con la 2: si el elemento 1 trae un tipo malo y el 2 un tramo inexistente, manda
        // el mensaje del tramo. Un bucle único por elemento daría el del tipo y rompería
        // el orden documentado.
        List<TramoSemanal> resueltos = new ArrayList<>();
        for (RestriccionHorariaRequest peticion : entrantes) {
            resueltos.add(resolverTramo(peticion, tramos, ordenEnDia));
        }

        // Pasada 2: parsear el tipo de TODOS los elementos (400).
        List<ProfesorRestriccionHoraria> nuevas = new ArrayList<>();
        for (int i = 0; i < entrantes.size(); i++) {
            RestriccionHorariaRequest peticion = entrantes.get(i);
            TipoRestriccion tipo = parseTipo(peticion == null ? null : peticion.tipo());
            nuevas.add(new ProfesorRestriccionHoraria(
                    profesor,
                    resueltos.get(i),
                    tipo,
                    PESO_POR_DEFECTO,
                    peticion == null ? null : peticion.motivo()));
        }

        // Pasada 3: ningún (dia, ordenEnDia) repetido. Sin red de esquema debajo: la
        // tabla no tiene UNIQUE (profesor, tramo), así que sin esta guarda entrarían DOS
        // filas para el mismo tramo y el 200 sería una mentira silenciosa. Se compara por
        // el par de la PETICIÓN, que es lo que el cliente escribió y lo que el mensaje
        // debe nombrar.
        Set<String> vistos = new LinkedHashSet<>();
        for (RestriccionHorariaRequest peticion : entrantes) {
            String par = peticion.dia() + "-" + peticion.ordenEnDia();
            if (!vistos.add(par)) {
                throw new IllegalArgumentException(
                        "tramo repetido en las restricciones: (dia=" + peticion.dia()
                                + ", ordenEnDia=" + peticion.ordenEnDia() + ")");
            }
        }

        // Borra lo actual y FLUSHEA antes de insertar: el DELETE precede al INSERT, que es
        // lo que hace idempotente al PUT cuando la lista repite un tramo ya presente.
        // Patrón LITERAL de AsignaturaService.reemplazarAulasCompatibles (S75).
        restriccionRepositorio.deleteAll(restriccionRepositorio.findByProfesor(profesor));
        restriccionRepositorio.flush();
        for (ProfesorRestriccionHoraria restriccion : nuevas) {
            restriccionRepositorio.save(restriccion);
        }
        return ordenar(nuevas, ordenEnDia);
    }

    /**
     * Resuelve el par (dia 1..5, ordenEnDia 1..6) al {@link TramoSemanal} LECTIVO
     * correspondiente invirtiendo {@code CatalogoMapper.indiceOrdenEnDia} (que ya excluye
     * los recreos): un tramo de recreo no aparece en el índice, así que un par que caiga
     * en recreo o fuera de rango no empareja y aborta con 400. Copia deliberada de
     * {@code BloqueoService.resolverTramo}, el precedente vivo de esta inversión.
     */
    private TramoSemanal resolverTramo(
            RestriccionHorariaRequest peticion,
            List<TramoSemanal> tramos,
            Map<Long, Integer> ordenEnDia) {

        if (peticion == null) {
            throw new IllegalArgumentException("restriccion en blanco");
        }
        for (TramoSemanal t : tramos) {
            Integer ordenDeT = ordenEnDia.get(t.getId());
            if (ordenDeT != null
                    && ordenDeT == peticion.ordenEnDia()
                    && t.getDia().ordinal() + 1 == peticion.dia()) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "No existe tramo lectivo con (dia=" + peticion.dia()
                        + ", ordenEnDia=" + peticion.ordenEnDia() + ")");
    }

    /** Parsea un nombre a {@link TipoRestriccion}; valor malo → 400 que lo nombra y lista
     * los válidos (mismo patrón que {@code parseTipoAula} en {@code AsignaturaService}). */
    private static TipoRestriccion parseTipo(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "tipo en blanco. Valores validos: "
                            + Arrays.toString(TipoRestriccion.values()));
        }
        try {
            return TipoRestriccion.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "tipo invalido: '" + valor + "'. Valores validos: "
                            + Arrays.toString(TipoRestriccion.values()));
        }
    }

    /**
     * Restricciones ordenadas por (dia, ordenEnDia) para que la respuesta sea
     * determinista. Un tramo ausente del índice (una restricción sobre un recreo, que el
     * alta por este servicio NO permite crear pero que podría existir por otra vía) es un
     * error de integridad y aborta explícitamente, igual que
     * {@code BloqueoService.tramoRef}: es preferible a proyectarlo con un orden inventado.
     */
    private static List<RestriccionHorariaDTO> ordenar(
            List<ProfesorRestriccionHoraria> restricciones, Map<Long, Integer> ordenEnDia) {

        return restricciones.stream()
                .map(r -> new RestriccionHorariaDTO(
                        r.getTipo().name(),
                        r.getTramo().getDia().ordinal() + 1,
                        ordenDe(r, ordenEnDia),
                        r.getMotivo()))
                .sorted(Comparator.comparingInt(RestriccionHorariaDTO::dia)
                        .thenComparingInt(RestriccionHorariaDTO::ordenEnDia))
                .toList();
    }

    private static int ordenDe(
            ProfesorRestriccionHoraria restriccion, Map<Long, Integer> ordenEnDia) {
        Integer orden = ordenEnDia.get(restriccion.getTramo().getId());
        if (orden == null) {
            throw new IllegalStateException("El tramo " + restriccion.getTramo().getId()
                    + " de una restriccion horaria no es lectivo (¿recreo o ausente del catálogo?)");
        }
        return orden;
    }
}
