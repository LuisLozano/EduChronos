package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.web.dto.AsignacionRequest;
import es.yaroki.educhronos.app.web.dto.BloqueDTO;
import es.yaroki.educhronos.app.web.dto.PlanReplicacionDTO;
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
        return new Analisis(grupo, hermano, espejoPorOriginal, actividades,
                clasificar(actividades));
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
     */
    private static List<Bloque> clasificar(List<Actividad> actividades) {
        List<Bloque> bloques = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.getPlazas().size() <= 1) {
                continue;   // no es bloque: no se toca
            }
            Set<String> union = new HashSet<>();
            for (Plaza plaza : actividad.getPlazas()) {
                union.addAll(gruposDe(plaza));
            }
            boolean replicado = actividad.getPlazas().stream()
                    .allMatch(plaza -> gruposDe(plaza).equals(union));
            bloques.add(new Bloque(actividad, replicado));
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
        Set<String> esperados = analisis.espejosDeReparto();
        Set<Long> plazasValidas = analisis.plazasDeReparto();

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
            Long plaza = asignacion.plaza();
            if (plaza != null && !plazasValidas.contains(plaza)) {
                throw new IllegalArgumentException(
                        "La plaza con id " + plaza + " no pertenece a ningun bloque de reparto"
                                + " que toque al hermano " + analisis.hermano().getCodigo());
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
            for (Plaza plaza : bloque.actividad().getPlazas()) {
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
                bloque.actividad().getPlazas().forEach(p -> porId.put(p.getId(), p));
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
        List<ViaDTO> vias = bloque.actividad().getPlazas().stream()
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

    /** Un bloque ya clasificado: la actividad y si todas sus vías cubren el mismo U. */
    private record Bloque(Actividad actividad, boolean replicado) { }

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

        /** Códigos de espejo cuyo original participa en algún bloque de REPARTO. */
        Set<String> espejosDeReparto() {
            Set<String> codigos = new LinkedHashSet<>();
            for (Bloque bloque : bloques) {
                if (bloque.replicado()) {
                    continue;
                }
                for (Plaza plaza : bloque.actividad().getPlazas()) {
                    for (Subgrupo subgrupo : plaza.getSubgrupos()) {
                        String espejo = espejoPorOriginal.get(subgrupo.getCodigo());
                        if (espejo != null) {
                            codigos.add(espejo);
                        }
                    }
                }
            }
            return codigos;
        }

        /** Ids de las plazas que una asignación puede nombrar: las de los bloques de reparto. */
        Set<Long> plazasDeReparto() {
            Set<Long> ids = new LinkedHashSet<>();
            for (Bloque bloque : bloques) {
                if (!bloque.replicado()) {
                    bloque.actividad().getPlazas().forEach(p -> ids.add(p.getId()));
                }
            }
            return ids;
        }
    }
}
