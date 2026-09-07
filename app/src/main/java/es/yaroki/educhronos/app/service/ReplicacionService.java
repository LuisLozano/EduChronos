package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.AsignacionRequest;
import es.yaroki.educhronos.app.web.dto.BloqueDTO;
import es.yaroki.educhronos.app.web.dto.ParteDeshacerDTO;
import es.yaroki.educhronos.app.web.dto.PlanReplicacionDTO;
import es.yaroki.educhronos.app.web.dto.PlazaDescableadaDTO;
import es.yaroki.educhronos.app.web.dto.ReplicacionRequest;
import es.yaroki.educhronos.app.web.dto.ViaDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del sub-recurso REPLICACIÓN (Bloque S139, C-replicación-alta):
 * poblar un grupo ordinario recién creado copiando la estructura de un HERMANO suyo del
 * mismo nivel —sus subgrupos y el cableado de esos subgrupos a las plazas de los bloques—,
 * en vez de teclear a mano las decenas de altas que eso supone (medido: replicar {@code 1ºA}
 * toca 12 actividades; {@code 4ºA}, otras 12).
 *
 * <p><b>Y el gesto inverso (Bloque S140, C-alta-reversible):</b> {@link #deshacer} deja el grupo
 * DESNUDO otra vez, para poder replicar de otro hermano sin desmontar a mano lo que el alta
 * cableó. No es el espejo del alta y no lo pretende —la decisión de reparto no se persiste, así
 * que no hay nada que revertir—: lee el estado y lo suelta. El porqué, en su Javadoc.
 *
 * <p><b>Vocabulario, y es todo el algoritmo.</b>
 * <ul>
 *   <li><b>BLOQUE</b>: actividad con MÁS DE UNA plaza. Las de una sola no se tocan (ver abajo).
 *   <li><b>U(actividad)</b>: unión de los grupos de los subgrupos de TODAS sus plazas.
 *   <li><b>REPLICADO</b>: toda plaza cubre EXACTAMENTE U. El grupo nuevo entra por la misma vía
 *       que su original y no hay nada que preguntar.
 *   <li><b>REPARTE</b>: alguna plaza cubre un subconjunto estricto de U. Cómo se reparte el
 *       grupo nuevo entre esas vías NO está en el catálogo: lo decide el usuario.
 *   <li><b>SUFIJO</b> de un subgrupo del hermano: su código sin el prefijo {@code {hermano}-}.
 *   <li><b>ESPEJO</b>: subgrupo nuevo {@code {grupoNuevo}-{SUFIJO}}, con población el grupo nuevo.
 * </ul>
 *
 * <p><b>El sufijo se recorta por el PREFIJO del hermano, nunca por el primer guion.</b> Siete
 * de los veintiocho grupos del centro llevan un guion en su propio código ({@code 1B-A} …
 * {@code 2B-C}), y "lo que sigue al primer guion" produce basura en 107 de los 334 subgrupos
 * reales: de {@code 1B-A-DTec} saldría {@code 1B-E-A-DTec} en vez de {@code 1B-E-DTec}. Un
 * subgrupo del hermano que no empiece por el prefijo aborta el alta ENTERA con un 400 que lo
 * nombra, en vez de inventarle un código.
 *
 * <p><b>Las actividades de UNA SOLA PLAZA no se tocan</b>, y por eso su espejo —típicamente el
 * {@code -Completo}— queda creado y con CERO plazas. Es intencionado: el troncal del grupo
 * nuevo es una decisión de horario que esta operación no toma. Hoy no existe en el catálogo
 * real ni un solo subgrupo sin plazas, así que es un estado inédito; nada lo rechaza (el
 * dominio del solver exige ≥1 GRUPO, no ≥1 plaza, y las tres reglas de prevalidación no miran
 * subgrupos huérfanos).
 *
 * <p><b>Toda la validación ocurre ANTES de la primera escritura</b>, y el orden importa: el 404
 * del recurso primero, luego las precondiciones del par (grupo, hermano), luego el catálogo
 * derivado, y por último los dependientes. Dos familias de excepción y tres códigos HTTP, como
 * el resto del proyecto: {@link NoSuchElementException} → 404, {@link IllegalArgumentException}
 * → 400, {@link ReferenciaEntranteException} → 409.
 *
 * <p><b>Por qué la guarda de dependientes cubre TODAS las actividades que tocan al hermano</b>,
 * incluidas las de una plaza que esta operación no va a modificar: es la misma guarda ROMA que
 * {@code ActividadService.editar} aplica desde S109, y por el mismo motivo. Si el catálogo del
 * grupo nuevo se va a mover, un horario ya generado sobre esa parte del catálogo deja de
 * describir lo que hay; que la fila concreta cuelgue de una actividad intocada no lo hace menos
 * cierto. Se delega en {@link ActividadService#exigirSinDependientes} —pública desde S139— en
 * vez de recontar aquí: si las dos rutas contaran cosas distintas, se podría replicar sobre un
 * estado que la edición rechaza.
 */
@Service
public class ReplicacionService {

    /** Separador entre el código del grupo y el sufijo, en los códigos de subgrupo. */
    private static final String SEPARADOR = "-";

    private final GrupoAdministrativoRepository grupoRepositorio;
    private final SubgrupoRepository subgrupoRepositorio;
    private final ActividadRepository actividadRepositorio;
    private final ActividadService actividadService;

    public ReplicacionService(GrupoAdministrativoRepository grupoRepositorio,
                              SubgrupoRepository subgrupoRepositorio,
                              ActividadRepository actividadRepositorio,
                              ActividadService actividadService) {
        this.grupoRepositorio = grupoRepositorio;
        this.subgrupoRepositorio = subgrupoRepositorio;
        this.actividadRepositorio = actividadRepositorio;
        this.actividadService = actividadService;
    }

    /**
     * Lo que la replicación HARÍA, sin escribir nada. Mismas validaciones que el POST salvo las
     * que dependen del cuerpo (asignaciones) y la de dependientes: el plan se puede consultar
     * aunque haya un horario colgando, porque consultarlo no rompe nada.
     */
    @Transactional(readOnly = true)
    public PlanReplicacionDTO planificar(Long id, String codigoHermano) {
        return aPlan(analizar(id, codigoHermano));
    }

    /**
     * Ejecuta la replicación completa en UNA transacción. Devuelve el mismo plan que habría
     * devuelto el GET, ya materializado.
     *
     * <p>Orden: analizar y validar TODO (incluidas las asignaciones y los dependientes), y solo
     * entonces escribir. Entre la validación y la escritura no se consulta nada más.
     */
    @Transactional
    public PlanReplicacionDTO replicar(Long id, ReplicacionRequest peticion) {
        if (peticion == null) {
            throw new IllegalArgumentException("el cuerpo de la peticion es obligatorio");
        }
        Analisis analisis = analizar(id, peticion.hermano());
        Map<String, Long> decisiones = validarAsignaciones(analisis, peticion.asignaciones());
        exigirTodasSinDependientes(analisis);

        // ---- a partir de aquí, escritura; ya no puede fallar por validación.
        Map<String, Subgrupo> espejos = crearEspejos(analisis);
        cablearReplicados(analisis, espejos);
        cablearReparto(analisis, espejos, decisiones);
        return aPlan(analisis);
    }

    /**
     * Deja el grupo DESNUDO en UNA transacción: le quita todos sus subgrupos de cuantas plazas
     * los lleven y después los borra. Devuelve el parte de lo hecho.
     *
     * <p><b>No revierte "la última replicación": no hay nada persistido que la identifique.</b>
     * El alta no deja rastro de qué creó —ni marca en {@code subgrupo}, ni tabla de operaciones—,
     * así que "lo que hizo aquel POST" no es una pregunta que la base pueda responder. Lo que
     * este método sabe contestar es otra: qué subgrupos tiene HOY este grupo. Los borra todos.
     * Si alguien replicó y luego editó a mano —añadió un optativo, movió un espejo de vía—, esa
     * edición se pierde con lo demás.
     *
     * <p>Es correcto para el escenario que motiva el gesto —septiembre: se replica, se mira, no
     * cuadra, se deshace y se vuelve a replicar de otro hermano— y es exactamente por eso que
     * vive en un sub-recurso NOMBRADO y no en el {@code DELETE} del grupo: quien escribe
     * {@code DELETE /api/grupos/{id}/replicacion} está pidiendo pelar el grupo, no quitar el
     * último cambio.
     *
     * <p><b>El deshacer NO es el espejo del alta, y esa es la trampa.</b> {@link #cablearReparto}
     * eligió la vía con un mapa que venía del CUERPO del POST y que no se persiste: la decisión
     * no se puede recomputar. Así que aquí no se reconstruye nada — se LEE el estado, buscando
     * el ESPEJO en las plazas en vez del original. Una sola travesía, idéntica para los bloques
     * replicados y para los de reparto, que es lo que hace que la asimetría no importe.
     *
     * <p>Orden: resolver, las tres guardas, y solo entonces escribir. {@link NoSuchElementException}
     * (→ 404) si el grupo no existe; {@link ReferenciaEntranteException} (→ 409) si algún
     * subgrupo no es exclusivo del grupo o si alguna actividad afectada tiene dependientes.
     */
    @Transactional
    public ParteDeshacerDTO deshacer(Long id) {
        GrupoAdministrativo grupo = grupoRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + id));

        // (a) Los subgrupos del grupo, con el MISMO barrido que usa el alta sobre el hermano.
        List<Subgrupo> propios = subgruposDe(grupo);
        if (propios.isEmpty()) {
            return new ParteDeshacerDTO(grupo.getCodigo(), List.of(), List.of());
        }
        exigirSubgruposExclusivos(grupo, propios);                                  // (b) G2

        List<Actividad> actividades = actividadRepositorio.findConPoblacionPorGrupo(
                grupo.getCodigo());
        for (Actividad actividad : actividades) {                                   // (c) G3
            actividadService.exigirSinDependientes(actividad.getId(), "deshacer");
        }
        List<Plaza> plazas = plazasDe(actividades);
        List<Retirada> retiradas = planificarRetiradas(plazas, propios);
        exigirNingunaPlazaVacia(plazas, retiradas);                                 // (d) G4

        // ---- (e) a partir de aquí, escritura; ya no puede fallar por validación.
        // El parte se arma ANTES de tocar nada: después, los subgrupos están borrados y las
        // plazas ya no los llevan, así que no habría de dónde leerlo.
        ParteDeshacerDTO parte = aParte(grupo, propios, retiradas);
        for (Retirada retirada : retiradas) {
            retirada.plaza().quitarSubgrupo(retirada.subgrupo());
        }
        // El flush separa soltar las filas de plaza_subgrupo de borrar los subgrupos, porque
        // plaza_subgrupo.subgrupo_id es NO ACTION en schema.sql (no está entre las siete
        // cascadas) y con foreign_keys=ON el borrado mordería. MEDIDO en S140: quitarlo NO
        // rompe ningún test — Hibernate ya emite las actualizaciones de colección antes que
        // los borrados de entidad, así que hoy el orden sale bien solo. Se deja porque
        // depender de ese orden implícito es exactamente lo que costó la lección de S73, y
        // porque PdcService.borrar —donde sí hace falta, al borrar además el grupo— lo
        // escribe igual: dos deshaceres que se leen distinto invitan a cambiar el que no
        // "hace falta".
        actividadRepositorio.saveAll(actividades);
        actividadRepositorio.flush();
        propios.forEach(subgrupoRepositorio::delete);
        return parte;
    }

    // ─────────────────────────────────────────────────────────── guardas del deshacer

    /**
     * G2: cada subgrupo del grupo tiene EXACTAMENTE UN grupo asociado (a) y ese grupo es el
     * que se está pelando (b). Si no, 409 nombrando el subgrupo: un subgrupo compartido con
     * otro grupo —la "Lectura B" de Bachillerato, cuyos alumnos vienen de varios grupos del
     * nivel— no es población que este grupo posea, y borrarlo se llevaría por delante a los
     * demás.
     *
     * <p><b>Las dos comprobaciones se escriben aunque (b) sea hoy inalcanzable.</b> Lo es
     * porque {@link #subgruposDe} ya filtra por "contiene a este grupo", así que un subgrupo
     * con un solo grupo tiene forzosamente ESE. Escribir solo (a) es lo que convertiría la
     * regla en un accidente del filtro: el día que la resolución de los subgrupos cambie —por
     * código derivado, pongamos, en vez de por asociación— (b) deja de ser redundante y es la
     * única que impide borrar el subgrupo de otro. Es el defecto que S139 introdujo al pasar
     * del contrato al guion, y no se repite.
     *
     * <p>El conteo del 409 nunca puede salir 0 —lo que reventaría en el ctor de
     * {@link ReferenciaEntranteException}— y no por suerte: si (a) falla es porque hay ≥2
     * grupos y uno es el propio, luego ≥1 ajeno; si (b) fallara, el único grupo sería el ajeno.
     */
    private static void exigirSubgruposExclusivos(GrupoAdministrativo grupo,
                                                  List<Subgrupo> propios) {
        for (Subgrupo subgrupo : propios) {
            Set<GrupoAdministrativo> grupos = subgrupo.getGrupos();
            long ajenos = grupos.stream()
                    .filter(g -> !g.getId().equals(grupo.getId()))
                    .count();
            if (grupos.size() != 1) {
                throw compartido(grupo, subgrupo, ajenos);      // (a) exactamente uno
            }
            if (!grupos.iterator().next().getId().equals(grupo.getId())) {
                throw compartido(grupo, subgrupo, ajenos);      // (b) y es el nuestro
            }
        }
    }

    /** El 409 de G2, con el subgrupo nombrado y el número de grupos ajenos que lo retienen. */
    private static ReferenciaEntranteException compartido(GrupoAdministrativo grupo,
                                                          Subgrupo subgrupo, long ajenos) {
        return new ReferenciaEntranteException(
                List.of(new Referencia("grupo(s) ajeno(s) a " + grupo.getCodigo()
                        + " en el subgrupo " + subgrupo.getCodigo(), ajenos)),
                "deshacer");
    }

    /**
     * G4: ninguna plaza puede quedar con CERO subgrupos. <b>No es una guarda de negocio: es un
     * ASERTO DE PRODUCCIÓN.</b> Una plaza sin población no describe nada —el solver no sabría a
     * quién sentar en ella— y ningún flujo del catálogo puede producirla.
     *
     * <p>Que dispare significa que el razonamiento de este método es falso, no que el usuario
     * haya pedido algo ilegal, y por eso lanza {@link IllegalStateException} —que sale como 500
     * y se ve— en vez de un 4xx que lo presentaría como un caso de uso previsible. Se evalúa
     * ANTES de escribir, sobre la resta simulada, para que el aserto no deje media transacción
     * hecha si alguna vez se cumple.
     *
     * <p>Hoy no puede dispararse: una plaza en la que hay un espejo del grupo lleva también, por
     * construcción del alta, al subgrupo original del hermano, que no es del grupo y sobrevive.
     * Lo que lo haría cierto sería un catálogo editado a mano hasta dejar una plaza poblada solo
     * por este grupo — y ahí se quiere ruido, no silencio.
     */
    private static void exigirNingunaPlazaVacia(List<Plaza> plazas, List<Retirada> retiradas) {
        Map<Long, Long> aQuitar = new LinkedHashMap<>();
        for (Retirada retirada : retiradas) {
            aQuitar.merge(retirada.plaza().getId(), 1L, Long::sum);
        }
        for (Plaza plaza : plazas) {
            long quedan = plaza.getSubgrupos().size() - aQuitar.getOrDefault(plaza.getId(), 0L);
            if (quedan <= 0) {
                throw new IllegalStateException(
                        "Deshacer dejaria SIN SUBGRUPOS la plaza con id " + plaza.getId()
                                + " (" + plaza.getCodigo() + ", actividad "
                                + plaza.getActividad().getCodigo() + "): una plaza sin"
                                + " poblacion no describe nada y ningun flujo puede producirla");
            }
        }
    }

    // ───────────────────────────────────────────────────────── travesía del deshacer

    /**
     * Las plazas de esas actividades, DEDUPLICADAS por id. <b>Es la única puerta a las plazas
     * en este servicio: ni el alta ni el deshacer leen {@code actividad.getPlazas()}.</b>
     *
     * <p><b>Por qué.</b> {@code Actividad.plazas} es un {@code List} y
     * {@code findConPoblacionPorGrupo} le hace {@code left join fetch} de {@code p.subgrupos}.
     * Una colección-bolsa fetch-joineada recibe una entrada por FILA del producto cartesiano y
     * el {@code distinct} del HQL solo desduplica la raíz, así que {@code getPlazas()} devuelve
     * cada plaza repetida tantas veces como subgrupos tenga.
     *
     * <p><b>Lo que costaba, medido sobre el catálogo real (S140).</b> 30 de las 219 actividades
     * tienen UNA plaza con ≥2 subgrupos —todas de la misma forma: la plaza compartida entre un
     * grupo ordinario y su PDC, {@code EF-3ºA+3ºADi}—, y afectaban a los cinco ordinarios con
     * PDC (3ºA/3ºB/3ºC con 4 actividades cada uno, 4ºA/4ºD con 2). El GET reportaba 4 vías
     * donde hay 2, y el POST metía el {@code -Completo} del grupo nuevo en esa plaza, dejándolo
     * con 1 plaza donde el Javadoc de clase promete 0: el grupo nuevo acababa sentado en la
     * clase de otro. En el deshacer, la misma bolsa hacía que G4 restara tres retiradas donde
     * había una y abortase una operación legítima.
     *
     * <p><b>Se desduplica aquí y no en la consulta</b> —que la comparten otras rutas cuyo
     * comportamiento no está medido— y el resultado viaja DENTRO de {@link Bloque}, para que un
     * consumidor no pueda volver a la bolsa sin que se note.
     */
    private static List<Plaza> plazasDe(List<Actividad> actividades) {
        Map<Long, Plaza> porId = new LinkedHashMap<>();
        for (Actividad actividad : actividades) {
            for (Plaza plaza : actividad.getPlazas()) {
                porId.putIfAbsent(plaza.getId(), plaza);
            }
        }
        return List.copyOf(porId.values());
    }

    /**
     * La travesía ÚNICA: por cada plaza que toca al grupo, qué subgrupos de los suyos hay que
     * soltar. No distingue bloque replicado de bloque de reparto ni mira cuántas plazas tiene
     * la actividad — busca el ESPEJO, y donde esté, sale.
     *
     * <p>{@code List.copyOf} del conjunto antes de recorrerlo, por la misma razón que
     * {@link #cablearReplicados}: la colección se muta después sobre las mismas plazas.
     *
     * <p>El emparejamiento va por CÓDIGO y no por identidad de objeto aunque las dos consultas
     * compartan contexto de persistencia: el código de subgrupo es único en el esquema, así que
     * es una clave tan buena como el id y no depende de que las dos lecturas devuelvan la misma
     * instancia.
     *
     * <p>Se ordena por (actividad, plaza, subgrupo) para que el parte sea estable entre
     * llamadas: el orden de {@code Set} no lo es, y un parte que baraje sus filas es difícil
     * de comparar contra el anterior.
     */
    private static List<Retirada> planificarRetiradas(List<Plaza> plazas, List<Subgrupo> propios) {
        Map<String, Subgrupo> porCodigo = new LinkedHashMap<>();
        propios.forEach(subgrupo -> porCodigo.put(subgrupo.getCodigo(), subgrupo));

        List<Retirada> retiradas = new ArrayList<>();
        for (Plaza plaza : plazas) {
            for (Subgrupo presente : List.copyOf(plaza.getSubgrupos())) {
                Subgrupo propio = porCodigo.get(presente.getCodigo());
                if (propio != null) {
                    retiradas.add(new Retirada(plaza, propio));
                }
            }
        }
        retiradas.sort(Comparator
                .comparing((Retirada r) -> r.plaza().getActividad().getCodigo())
                .thenComparing(r -> r.plaza().getCodigo())
                .thenComparing(r -> r.subgrupo().getCodigo()));
        return retiradas;
    }

    private static ParteDeshacerDTO aParte(GrupoAdministrativo grupo, List<Subgrupo> propios,
                                           List<Retirada> retiradas) {
        return new ParteDeshacerDTO(
                grupo.getCodigo(),
                propios.stream().map(Subgrupo::getCodigo).toList(),
                retiradas.stream()
                        .map(r -> new PlazaDescableadaDTO(
                                r.plaza().getId(),
                                r.plaza().getCodigo(),
                                r.plaza().getActividad().getCodigo(),
                                r.subgrupo().getCodigo()))
                        .toList());
    }

    // ───────────────────────────────────────────────────────── análisis y validación

    /**
     * Resuelve el par (grupo, hermano), deriva los espejos y clasifica los bloques. Todas las
     * validaciones que NO dependen del cuerpo viven aquí, así que el GET y el POST rechazan
     * exactamente lo mismo y con el mismo mensaje.
     */
    private Analisis analizar(Long id, String codigoHermano) {
        GrupoAdministrativo grupo = grupoRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + id));
        if (codigoHermano == null || codigoHermano.isBlank()) {
            throw new IllegalArgumentException("hermano es obligatorio");
        }
        GrupoAdministrativo hermano = grupoRepositorio.findByCodigo(codigoHermano)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe grupo con codigo " + codigoHermano));
        if (hermano.getId().equals(grupo.getId())) {
            throw new IllegalArgumentException(
                    "el hermano no puede ser el propio grupo " + grupo.getCodigo());
        }
        if (!hermano.getNivel().getId().equals(grupo.getNivel().getId())) {
            throw new IllegalArgumentException(
                    "el hermano " + hermano.getCodigo() + " es del nivel "
                            + hermano.getNivel().getCodigo() + " y el grupo "
                            + grupo.getCodigo() + " del nivel " + grupo.getNivel().getCodigo());
        }
        if (grupo.getTipo() != TipoGrupo.ORDINARIO) {
            throw new IllegalArgumentException(
                    "El grupo " + grupo.getCodigo() + " es de tipo " + grupo.getTipo().name()
                            + " y no se replica por este flujo: solo los ORDINARIOS");
        }
        if (grupoRepositorio.contarSubgrupos(grupo.getId()) > 0) {
            throw new IllegalArgumentException(
                    "El grupo " + grupo.getCodigo() + " ya tiene subgrupos: la replicacion"
                            + " puebla un grupo recien creado, no fusiona con lo que ya hay");
        }

        List<Subgrupo> delHermano = subgruposDe(hermano);
        Map<String, String> espejoPorOriginal = derivarEspejos(grupo, hermano, delHermano);
        List<Actividad> actividades = actividadRepositorio.findConPoblacionPorGrupo(
                hermano.getCodigo());
        // Las plazas se DEDUPLICAN antes de clasificar: la consulta las trae repetidas (ver
        // plazasDe). Contar la bolsa hacía pasar por bloque a una actividad de plaza única.
        return new Analisis(grupo, hermano, espejoPorOriginal, actividades,
                clasificar(plazasDe(actividades)));
    }

    /**
     * Los subgrupos del hermano, ordenados por código.
     *
     * <p>Barrido de {@code findAll} en memoria porque {@code Subgrupo.grupos} es el lado
     * PROPIETARIO del N:M y {@code GrupoAdministrativo} no tiene el inverso: no hay
     * {@code grupo.getSubgrupos()} que navegar ni consulta que lo devuelva, y añadir una sería
     * tocar un repositorio que este cambio no toca. Es el mismo barrido que
     * {@code GeneradorHorarioService} hace para cargar el catálogo.
     */
    private List<Subgrupo> subgruposDe(GrupoAdministrativo hermano) {
        return subgrupoRepositorio.findAll().stream()
                .filter(s -> s.getGrupos().stream()
                        .anyMatch(g -> g.getId().equals(hermano.getId())))
                .sorted(Comparator.comparing(Subgrupo::getCodigo))
                .toList();
    }

    /**
     * Código de espejo por código de original, recortando el sufijo por el PREFIJO del hermano.
     * Un original que no empiece por {@code {hermano}-} aborta con 400 nombrándolo: no se le
     * inventa un sufijo ni se le salta, porque saltárselo dejaría al grupo nuevo con menos
     * subgrupos que su molde y sin decir cuál falta.
     *
     * <p>Un código de espejo que ya exista también aborta, ANTES de escribir nada. La
     * comprobación se hace aquí y no delegando en {@code SubgrupoService}: esta ruta construye
     * las entidades directamente, así que la unicidad que aquel valida no la protege.
     */
    private Map<String, String> derivarEspejos(GrupoAdministrativo grupo,
                                               GrupoAdministrativo hermano,
                                               List<Subgrupo> delHermano) {
        String prefijo = hermano.getCodigo() + SEPARADOR;
        Map<String, String> porOriginal = new LinkedHashMap<>();
        for (Subgrupo original : delHermano) {
            String codigo = original.getCodigo();
            if (!codigo.startsWith(prefijo)) {
                throw new IllegalArgumentException(
                        "El subgrupo " + codigo + " del hermano " + hermano.getCodigo()
                                + " no empieza por '" + prefijo + "' y no se le puede derivar"
                                + " un sufijo: renombralo antes de replicar");
            }
            String espejo = grupo.getCodigo() + SEPARADOR + codigo.substring(prefijo.length());
            subgrupoRepositorio.findByCodigo(espejo).ifPresent(existente -> {
                throw new IllegalArgumentException(
                        "El codigo de subgrupo derivado ya existe: " + espejo);
            });
            porOriginal.put(codigo, espejo);
        }
        return porOriginal;
    }

    /**
     * Clasifica en REPLICADO / REPARTE las actividades que son BLOQUE, descartando las de una
     * sola plaza. El predicado es la igualdad EXACTA con U: relajarlo a "contenido en U" lo
     * haría cierto por construcción y todo bloque saldría replicado.
     *
     * <p><b>Recibe PLAZAS ya deduplicadas, no actividades, y las reagrupa.</b> Contar
     * {@code actividad.getPlazas()} —que es lo que hacía hasta S140— era contar la bolsa
     * inflada por el fetch join, y una actividad de UNA plaza con dos subgrupos medía 2: se
     * colaba como bloque, salía REPLICADO (todas sus "vías" son la misma plaza, luego todas
     * cubren U) y {@link #cablearReplicados} le metía el espejo, justo lo que la regla "las de
     * una sola plaza no se tocan" prohíbe. Reagrupar desde las plazas hace que el recuento sea
     * el de filas de {@code plaza}, que es el que la regla nombra.
     *
     * <p>Cada {@link Bloque} se lleva SUS plazas ya deduplicadas para que ningún consumidor
     * —cableado, proyección, destinos de reparto— vuelva a {@code getPlazas()} por su cuenta.
     */
    private static List<Bloque> clasificar(List<Plaza> plazas) {
        Map<Long, List<Plaza>> porActividad = new LinkedHashMap<>();
        for (Plaza plaza : plazas) {
            porActividad.computeIfAbsent(plaza.getActividad().getId(), id -> new ArrayList<>())
                    .add(plaza);
        }

        List<Bloque> bloques = new ArrayList<>();
        for (List<Plaza> vias : porActividad.values()) {
            if (vias.size() <= 1) {
                continue;   // no es bloque: no se toca
            }
            Set<String> union = new HashSet<>();
            for (Plaza plaza : vias) {
                union.addAll(gruposDe(plaza));
            }
            boolean replicado = vias.stream().allMatch(plaza -> gruposDe(plaza).equals(union));
            bloques.add(new Bloque(vias.get(0).getActividad(), List.copyOf(vias), replicado));
        }
        bloques.sort(Comparator.comparing(b -> b.actividad().getCodigo()));
        return bloques;
    }

    /** Códigos de los grupos que una plaza cubre, vía los grupos de sus subgrupos. */
    private static Set<String> gruposDe(Plaza plaza) {
        Set<String> codigos = new HashSet<>();
        for (Subgrupo subgrupo : plaza.getSubgrupos()) {
            subgrupo.getGrupos().forEach(g -> codigos.add(g.getCodigo()));
        }
        return codigos;
    }

    /**
     * Comprueba el cuerpo contra el plan y devuelve, por código de espejo, la plaza elegida
     * (valor {@code null} = "no cablear", que es legítimo).
     *
     * <p>Cada espejo que participa en un bloque de REPARTO debe aparecer EXACTAMENTE UNA VEZ.
     * La unicidad no es formalismo: es lo que hace imposible violar I2 por esta vía —un espejo
     * no puede acabar en dos plazas de la misma actividad—, invariante que {@code ActividadService}
     * hace cumplir en su CRUD pero que esta ruta no atraviesa, y que el esquema tampoco impide
     * ({@code plaza_subgrupo} tiene PK {@code (plaza_id, subgrupo_id)}, que admite el mismo
     * subgrupo en dos plazas distintas de la misma actividad).
     */
    private Map<String, Long> validarAsignaciones(Analisis analisis,
                                                  List<AsignacionRequest> asignaciones) {
        Map<String, Destino> destinos = analisis.destinosDeReparto();
        Set<String> esperados = destinos.keySet();

        Map<String, Long> decisiones = new LinkedHashMap<>();
        for (AsignacionRequest asignacion : asignaciones == null ? List.<AsignacionRequest>of()
                : asignaciones) {
            if (asignacion == null || asignacion.subgrupo() == null
                    || asignacion.subgrupo().isBlank()) {
                throw new IllegalArgumentException(
                        "cada asignacion debe nombrar el subgrupo espejo al que se refiere");
            }
            String espejo = asignacion.subgrupo();
            if (!esperados.contains(espejo)) {
                throw new IllegalArgumentException(
                        "El subgrupo " + espejo + " no corresponde a ningun bloque de reparto:"
                                + " los bloques replicados no se asignan a mano y las"
                                + " actividades de una sola plaza no se tocan");
            }
            if (decisiones.containsKey(espejo)) {
                throw new IllegalArgumentException(
                        "El subgrupo " + espejo + " aparece en mas de una asignacion");
            }
            Destino destino = destinos.get(espejo);
            Long plaza = asignacion.plaza();
            if (plaza != null && !destino.plazas().contains(plaza)) {
                throw new IllegalArgumentException(
                        "La plaza con id " + plaza + " no pertenece a " + destino.nombrar()
                                + ", que es donde participa el original de " + espejo
                                + ": una asignacion solo puede elegir entre las vias de SU"
                                + " propio bloque");
            }
            decisiones.put(espejo, plaza);
        }
        Set<String> ausentes = new LinkedHashSet<>(esperados);
        ausentes.removeAll(decisiones.keySet());
        if (!ausentes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Falta la asignacion de " + ausentes.size() + " subgrupo(s) de bloques de"
                            + " reparto: " + ausentes);
        }
        return decisiones;
    }

    /**
     * {@link ActividadService#exigirSinDependientes} sobre CADA actividad que toca al hermano,
     * una por una. Una por una y no agregando conteos a propósito: el constructor de
     * {@link ReferenciaEntranteException} revienta con {@link IllegalArgumentException} si
     * todos los conteos son 0, y un 409 agregado a mano convertiría el conflicto en un 400 con
     * un mensaje interno en cuanto no hubiera dependientes.
     */
    private void exigirTodasSinDependientes(Analisis analisis) {
        for (Long id : analisis.actividadesAfectadas()) {
            actividadService.exigirSinDependientes(id, "replicar");
        }
    }

    // ───────────────────────────────────────────────────────────────────── escritura

    /** Paso 1: los espejos. {@code IDENTITY} asigna el id en el propio {@code save}. */
    private Map<String, Subgrupo> crearEspejos(Analisis analisis) {
        Map<String, Subgrupo> porOriginal = new LinkedHashMap<>();
        for (Map.Entry<String, String> entrada : analisis.espejoPorOriginal().entrySet()) {
            porOriginal.put(entrada.getKey(), subgrupoRepositorio.save(
                    new Subgrupo(entrada.getValue(), Set.of(analisis.grupo()))));
        }
        return porOriginal;
    }

    /**
     * Paso 2: en un bloque REPLICADO, el espejo entra en LA plaza que contenía a su original, y
     * solo en esa. Cero decisiones del usuario, pero tampoco cableado a ciegas: cablearlo en
     * todas las plazas del bloque metería al grupo nuevo en vías que su hermano no cursa.
     */
    private void cablearReplicados(Analisis analisis, Map<String, Subgrupo> espejos) {
        for (Bloque bloque : analisis.bloques()) {
            if (!bloque.replicado()) {
                continue;
            }
            for (Plaza plaza : bloque.plazas()) {
                for (Subgrupo original : List.copyOf(plaza.getSubgrupos())) {
                    Subgrupo espejo = espejos.get(original.getCodigo());
                    if (espejo != null) {
                        plaza.agregarSubgrupo(espejo);
                    }
                }
            }
        }
    }

    /** Paso 3: en un bloque de REPARTO, solo la plaza que nombre la decisión; null no cablea. */
    private void cablearReparto(Analisis analisis, Map<String, Subgrupo> espejos,
                                Map<String, Long> decisiones) {
        Map<Long, Plaza> porId = new LinkedHashMap<>();
        for (Bloque bloque : analisis.bloques()) {
            if (!bloque.replicado()) {
                bloque.plazas().forEach(p -> porId.put(p.getId(), p));
            }
        }
        for (Map.Entry<String, Subgrupo> entrada : espejos.entrySet()) {
            Long plazaId = decisiones.get(entrada.getValue().getCodigo());
            if (plazaId != null) {
                porId.get(plazaId).agregarSubgrupo(entrada.getValue());
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────── proyección

    private static PlanReplicacionDTO aPlan(Analisis analisis) {
        return new PlanReplicacionDTO(
                analisis.grupo().getCodigo(),
                analisis.hermano().getCodigo(),
                List.copyOf(analisis.espejoPorOriginal().values()),
                analisis.bloques().stream().filter(Bloque::replicado)
                        .map(b -> aBloque(b, analisis.hermano())).toList(),
                analisis.bloques().stream().filter(b -> !b.replicado())
                        .map(b -> aBloque(b, analisis.hermano())).toList());
    }

    private static BloqueDTO aBloque(Bloque bloque, GrupoAdministrativo hermano) {
        List<ViaDTO> vias = bloque.plazas().stream()
                .sorted(Comparator.comparing(Plaza::getCodigo))
                .map(plaza -> new ViaDTO(
                        plaza.getId(),
                        plaza.getCodigo(),
                        plaza.getAsignatura().getCodigo(),
                        gruposDe(plaza).stream().sorted().toList(),
                        gruposDe(plaza).contains(hermano.getCodigo())))
                .toList();
        return new BloqueDTO(bloque.actividad().getCodigo(), vias);
    }

    // ──────────────────────────────────────────────────────────────── tipos internos

    /**
     * Un bloque ya clasificado: la actividad, SUS plazas —deduplicadas por {@link #plazasDe},
     * nunca {@code actividad.getPlazas()}— y si todas sus vías cubren el mismo U.
     *
     * <p>Que las plazas viajen DENTRO del bloque es lo que impide que la corrección de S140 se
     * pierda: un consumidor que volviera a la bolsa de la actividad recuperaría los duplicados
     * sin que nada se lo dijera.
     */
    private record Bloque(Actividad actividad, List<Plaza> plazas, boolean replicado) { }

    /**
     * Una retirada pendiente del deshacer: el {@code subgrupo} sale de la {@code plaza}. La
     * actividad no se guarda aparte —{@code plaza.getActividad()} la da, y ya está cargada en
     * el contexto— para que no haya dos respuestas posibles a de qué actividad es la plaza.
     */
    private record Retirada(Plaza plaza, Subgrupo subgrupo) { }

    /**
     * Dónde puede aterrizar UN espejo: las actividades de reparto en las que participa su
     * original y las vías de esas actividades. Es lo que convierte "pertenece a algún bloque de
     * reparto" —que dejaba colar el espejo del bloque A en una vía del bloque B— en "pertenece
     * al SUYO".
     */
    private record Destino(Set<String> actividades, Set<Long> plazas) {

        /** La actividad esperada, para el mensaje del 400. */
        String nombrar() {
            return actividades.size() == 1
                    ? "la actividad " + actividades.iterator().next()
                    : "las actividades " + actividades;
        }
    }

    /**
     * El estado derivado que comparten validación, escritura y proyección, calculado UNA vez.
     * Que sea inmutable y se pase entero es lo que garantiza que el plan que devuelve el POST
     * describe la misma clasificación sobre la que se ha escrito.
     */
    private record Analisis(GrupoAdministrativo grupo,
                            GrupoAdministrativo hermano,
                            Map<String, String> espejoPorOriginal,
                            List<Actividad> actividades,
                            List<Bloque> bloques) {

        /**
         * Los ids de TODAS las actividades que tocan al hermano, bloques o no. Las de una sola
         * plaza entran aquí aunque no se vayan a modificar: la guarda es roma a propósito (ver
         * el Javadoc de clase).
         */
        Set<Long> actividadesAfectadas() {
            Set<Long> ids = new LinkedHashSet<>();
            actividades.forEach(a -> ids.add(a.getId()));
            return ids;
        }

        /**
         * Por cada espejo que hay que asignar, DÓNDE puede ir: las actividades de reparto en
         * las que participa su original y las vías de ESAS actividades, no las de cualquier
         * bloque de reparto. Las claves son, por construcción, el conjunto exacto de espejos
         * que el cuerpo debe nombrar.
         *
         * <p>El mapa se acumula en vez de asignarse porque un mismo subgrupo puede participar
         * en más de una actividad (medido en el catálogo real: 50 de los 334 aparecen en dos).
         * Hoy ninguno participa en dos bloques de REPARTO a la vez, así que cada destino tiene
         * una sola actividad; si algún día lo hiciera, el modelo de "una asignación por espejo"
         * se le quedaría corto —cablearía una vía y dejaría la otra muda— y esto al menos lo
         * dejaría visible en el mensaje en vez de elegir una en silencio.
         */
        Map<String, Destino> destinosDeReparto() {
            Map<String, Destino> destinos = new LinkedHashMap<>();
            for (Bloque bloque : bloques) {
                if (bloque.replicado()) {
                    continue;
                }
                Set<Long> vias = new LinkedHashSet<>();
                bloque.plazas().forEach(p -> vias.add(p.getId()));
                for (Plaza plaza : bloque.plazas()) {
                    for (Subgrupo subgrupo : plaza.getSubgrupos()) {
                        String espejo = espejoPorOriginal.get(subgrupo.getCodigo());
                        if (espejo == null) {
                            continue;
                        }
                        Destino destino = destinos.computeIfAbsent(espejo,
                                clave -> new Destino(new LinkedHashSet<>(), new LinkedHashSet<>()));
                        destino.actividades().add(bloque.actividad().getCodigo());
                        destino.plazas().addAll(vias);
                    }
                }
            }
            return destinos;
        }
    }
}
