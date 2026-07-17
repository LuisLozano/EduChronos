package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.ActividadRepository;
import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.PatronTemporal;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.ActividadDTO;
import es.yaroki.educhronos.app.web.dto.ActividadRequest;
import es.yaroki.educhronos.app.web.dto.PlazaDTO;
import es.yaroki.educhronos.app.web.dto.PlazaRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Actividad} como AGREGADO (§4.6, Bloque
 * 8.5-C1): la actividad es la raíz y sus {@link Plaza}s viajan embebidas; NO hay endpoint
 * de plazas. Toda la validación vive AQUÍ, una sola vez (la entidad JPA no valida nada,
 * D-B5-2):
 *
 * <ol>
 *   <li>escalares: {@code codigo} no en blanco; {@code duracionTramos}≥1;
 *       {@code repeticionesPorSemana}≥1; {@code patronTemporal} parseable a
 *       {@link PatronTemporal} (valor malo → 400 que lo nombra y lista los válidos);
 *       la {@code asignatura} de la actividad es opcional, pero si viene debe existir;
 *   <li>≥1 plaza por actividad;
 *   <li>XOR de aula por plaza: {@code aulaFija} y {@code aulasCandidatas} son mutuamente
 *       excluyentes y exactamente una está presente;
 *   <li>I7: cada plaza necesita ≥1 profesor;
 *   <li>I2: ningún subgrupo puede aparecer en dos plazas de la misma actividad (400 que lo
 *       NOMBRA, detectado CRUZANDO plazas, no dentro de una);
 *   <li>referencias por código (patrón 8.5-B): asignatura/aula/profesor/subgrupo no
 *       resoluble → 400 que nombra el código;
 *   <li>unicidad de {@code codigo} de actividad, excluida por id en la edición.
 * </ol>
 *
 * <p><b>Derivación del código de plaza</b> (el usuario no lo teclea): {@code {codigo}-P{n}}
 * con n 1-based en orden de llegada. Se REGENERA entero en el alta y en cada PUT: el
 * {@code orphanRemoval} borra las plazas viejas ({@link Actividad#limpiarPlazas}) y se
 * crean nuevas 1..n ({@link Actividad#agregarPlaza}).
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" (id inexistente)
 * lanza {@link NoSuchElementException} (→ 404); un fallo de validación lanza
 * {@link IllegalArgumentException} (→ 400, con el mensaje en el reason). El controlador los
 * distingue por TIPO. El servicio NO usa setters de la entidad: construye con el ctor
 * público y muta con {@code actualizar}/{@code agregarPlaza}.
 */
@Service
public class ActividadService {

    private final ActividadRepository repositorio;
    private final AsignaturaRepository asignaturaRepositorio;
    private final AulaRepository aulaRepositorio;
    private final ProfesorRepository profesorRepositorio;
    private final SubgrupoRepository subgrupoRepositorio;

    public ActividadService(ActividadRepository repositorio,
            AsignaturaRepository asignaturaRepositorio,
            AulaRepository aulaRepositorio,
            ProfesorRepository profesorRepositorio,
            SubgrupoRepository subgrupoRepositorio) {
        this.repositorio = repositorio;
        this.asignaturaRepositorio = asignaturaRepositorio;
        this.aulaRepositorio = aulaRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.subgrupoRepositorio = subgrupoRepositorio;
    }

    /** Todas las actividades como {@link ActividadDTO}, ORDENADAS por código. */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Actividad::getCodigo))
                .map(ActividadService::aDTO)
                .toList();
    }

    /** Una actividad por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public ActividadDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(ActividadService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe actividad con id " + id));
    }

    /**
     * Da de alta una actividad con sus plazas. {@link IllegalArgumentException} (→ 400) si
     * falla cualquier regla de validación o si ya existe otra con el mismo código.
     */
    @Transactional
    public ActividadDTO crear(ActividadRequest peticion) {
        validarEscalares(peticion);
        PatronTemporal patron = parsePatron(peticion.patronTemporal());
        validarPlazas(peticion.plazas());
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe una actividad con codigo " + peticion.codigo());
        });
        Asignatura asignatura = resolverAsignaturaActividad(peticion.asignatura());
        Actividad actividad = new Actividad(peticion.codigo(), asignatura,
                peticion.duracionTramos(), peticion.repeticionesPorSemana(),
                patron, peticion.requiereTutor());
        agregarPlazas(actividad, peticion);
        Actividad guardada = repositorio.save(actividad);
        return aDTO(guardada);
    }

    /**
     * Edita una actividad existente reemplazando sus campos y RECONCILIANDO sus plazas por
     * POSICIÓN (ver {@link #reconciliarPlazas}). {@link NoSuchElementException} (→ 404) si el
     * id no existe; {@link IllegalArgumentException} (→ 400) si falla la validación o si el
     * código pisa al de OTRA actividad. Reguardar la propia entidad con su mismo código es
     * válido: la unicidad se excluye a sí misma por id.
     */
    @Transactional
    public ActividadDTO editar(Long id, ActividadRequest peticion) {
        Actividad entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe actividad con id " + id));
        validarEscalares(peticion);
        PatronTemporal patron = parsePatron(peticion.patronTemporal());
        validarPlazas(peticion.plazas());
        repositorio.findByCodigo(peticion.codigo())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new IllegalArgumentException(
                            "Ya existe otra actividad con codigo " + peticion.codigo());
                });
        Asignatura asignatura = resolverAsignaturaActividad(peticion.asignatura());
        entidad.actualizar(peticion.codigo(), asignatura, peticion.duracionTramos(),
                peticion.repeticionesPorSemana(), patron, peticion.requiereTutor());
        reconciliarPlazas(entidad, peticion.plazas());
        return aDTO(entidad);
    }

    /** Borra una actividad por id. {@link NoSuchElementException} (→ 404) si no existe. El
     * cascade borra sus plazas; NO se comprueban referencias entrantes (eso es 8.5-C2). */
    @Transactional
    public void borrar(Long id) {
        Actividad entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe actividad con id " + id));
        List<Referencia> entrantes = List.of(
                new Referencia("sesion(es) bloqueada(s)", repositorio.contarSesionesBloqueadas(id)),
                new Referencia("sesion(es)", repositorio.contarSesionesSobreSusPlazas(id)),
                new Referencia("aula(s) bloqueada(s)", repositorio.contarAulasBloqueadas(id)));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
        repositorio.delete(entidad);
    }

    // ------------------------------------------------------------- validación

    /** Regla (1) escalares: código no en blanco; duración y repeticiones ≥1. */
    private static void validarEscalares(ActividadRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
        if (peticion.duracionTramos() < 1) {
            throw new IllegalArgumentException("duracionTramos debe ser >= 1");
        }
        if (peticion.repeticionesPorSemana() < 1) {
            throw new IllegalArgumentException("repeticionesPorSemana debe ser >= 1");
        }
    }

    /** Regla (1) patrón: no en blanco y parseable a {@link PatronTemporal}. */
    private static PatronTemporal parsePatron(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("patronTemporal es obligatorio");
        }
        try {
            return PatronTemporal.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "patronTemporal invalido: '" + valor + "'. Valores validos: "
                            + Arrays.toString(PatronTemporal.values()));
        }
    }

    /**
     * Reglas (2), (3), (4) y (5), sobre los CÓDIGOS del request (sin tocar la BD): ≥1
     * plaza; por plaza el XOR de aula y el I7 de profesores; y el I2 CRUZANDO plazas —los
     * subgrupos de cada plaza se deduplican dentro de la plaza (un repetido en la propia
     * lista NO cuenta) y se comprueba que no reaparezcan en otra plaza.
     */
    private static void validarPlazas(List<PlazaRequest> plazas) {
        if (plazas == null || plazas.isEmpty()) {
            throw new IllegalArgumentException("una actividad necesita al menos una plaza");
        }
        Set<String> subgruposDeOtrasPlazas = new HashSet<>();
        for (PlazaRequest plaza : plazas) {
            validarXor(plaza);
            validarProfesores(plaza);
            Set<String> deEstaPlaza = new LinkedHashSet<>(
                    plaza.subgrupos() == null ? List.of() : plaza.subgrupos());
            for (String codigo : deEstaPlaza) {
                if (!subgruposDeOtrasPlazas.add(codigo)) {
                    throw new IllegalArgumentException(
                            "el subgrupo " + codigo + " aparece en mas de una plaza de la actividad");
                }
            }
        }
    }

    /** Regla (3) XOR: exactamente una de aulaFija / aulasCandidatas. */
    private static void validarXor(PlazaRequest plaza) {
        boolean tieneFija = plaza.aulaFija() != null && !plaza.aulaFija().isBlank();
        boolean tieneCandidatas = plaza.aulasCandidatas() != null && !plaza.aulasCandidatas().isEmpty();
        if (tieneFija && tieneCandidatas) {
            throw new IllegalArgumentException(
                    "una plaza no puede tener aula fija y aulas candidatas a la vez");
        }
        if (!tieneFija && !tieneCandidatas) {
            throw new IllegalArgumentException(
                    "una plaza necesita aula fija o al menos un aula candidata");
        }
    }

    /** Regla (4) I7: cada plaza necesita ≥1 profesor. */
    private static void validarProfesores(PlazaRequest plaza) {
        if (plaza.profesores() == null || plaza.profesores().isEmpty()) {
            throw new IllegalArgumentException("una plaza necesita al menos un profesor (I7)");
        }
    }

    // --------------------------------------------------- resolución + montaje

    /** Regla (6) para la actividad: null admitido; si viene, debe existir. */
    private Asignatura resolverAsignaturaActividad(String codigo) {
        if (codigo == null) {
            return null;
        }
        return asignaturaRepositorio.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe asignatura con codigo " + codigo));
    }

    /**
     * Monta las plazas del ALTA: por cada {@link PlazaRequest}, en orden de llegada, deriva
     * el código posicional simple {@code {codigo}-P{n}} (1..n, una sola vez) y la crea.
     */
    private void agregarPlazas(Actividad actividad, ActividadRequest peticion) {
        List<PlazaRequest> plazas = peticion.plazas();
        for (int i = 0; i < plazas.size(); i++) {
            crearPlaza(actividad, peticion.codigo() + "-P" + (i + 1), plazas.get(i));
        }
    }

    /**
     * Reconciliación POSICIONAL de las plazas en el PUT (Bloque 8.5-C1, tras reevaluar y
     * DESCARTAR la regeneración total): la plaza entrante {@code i} reconcilia con la
     * existente {@code i} —ordenadas por {@code id}, su orden de creación—, CONSERVANDO su
     * código estable y actualizando solo el contenido; las existentes en posición ≥ m se
     * BORRAN (orphanRemoval al salir de la colección); las entrantes que exceden se CREAN con
     * código {@code {codigo}-P{siguiente}}, donde {@code siguiente = máximo N en los sufijos
     * -P{N} VIVOS + 1} (puede reusar un hueco dejado por un borrado: es seguro, la unicidad
     * se mantiene en todo instante y ni Sesion ni el solver emparejan por código fuera de su
     * ciclo). Ningún alta reasigna el código de una plaza viva, así que ningún INSERT colisiona
     * con la UNIQUE: no hace falta flush() explícito. El emparejamiento por posición es una
     * decisión de UX provisional (roza D-F8.6-a), a confirmar con el formulario de Fase 8.6.
     */
    private void reconciliarPlazas(Actividad actividad, List<PlazaRequest> entrantes) {
        List<Plaza> existentes = new ArrayList<>(actividad.getPlazas());
        existentes.sort(Comparator.comparing(Plaza::getId));
        int k = existentes.size();
        int m = entrantes.size();

        // (1) posiciones comunes: conservar código, actualizar contenido.
        for (int i = 0; i < Math.min(k, m); i++) {
            aplicarContenido(existentes.get(i), entrantes.get(i));
        }
        // (2) sobran existentes (m < k): borrar posiciones ≥ m (orphanRemoval).
        for (int i = m; i < k; i++) {
            actividad.getPlazas().remove(existentes.get(i));
        }
        // (3) faltan (m > k): crear nuevas con -P{siguiente}, sin reasignar las vivas.
        if (m > k) {
            int siguiente = maxSufijoVivo(actividad) + 1;
            for (int i = k; i < m; i++) {
                crearPlaza(actividad, actividad.getCodigo() + "-P" + siguiente, entrantes.get(i));
                siguiente++;
            }
        }
    }

    /** Crea una plaza resolviendo sus cuatro familias de referencia (regla 6) por código. */
    private void crearPlaza(Actividad actividad, String codigoPlaza, PlazaRequest plaza) {
        actividad.agregarPlaza(codigoPlaza,
                resolverAsignaturaPlaza(plaza.asignatura()),
                resolverAulaFija(plaza.aulaFija()),
                resolverProfesores(plaza.profesores()),
                resolverAulas(plaza.aulasCandidatas()),
                resolverSubgrupos(plaza.subgrupos()));
    }

    /** Actualiza el contenido de una plaza viva (regla 6), CONSERVANDO su código estable. */
    private void aplicarContenido(Plaza plaza, PlazaRequest entrante) {
        plaza.actualizar(
                resolverAsignaturaPlaza(entrante.asignatura()),
                resolverAulaFija(entrante.aulaFija()),
                resolverProfesores(entrante.profesores()),
                resolverAulas(entrante.aulasCandidatas()),
                resolverSubgrupos(entrante.subgrupos()));
    }

    /** Máximo N en los sufijos {@code -P{N}} de las plazas VIVAS; 0 si ninguno casa el patrón. */
    private static int maxSufijoVivo(Actividad actividad) {
        int max = 0;
        for (Plaza plaza : actividad.getPlazas()) {
            max = Math.max(max, sufijoDe(plaza.getCodigo()));
        }
        return max;
    }

    /** Extrae el N de un código {@code ...-P{N}}; 0 si el patrón no casa. */
    private static int sufijoDe(String codigo) {
        int idx = codigo.lastIndexOf("-P");
        if (idx < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(codigo.substring(idx + 2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Asignatura resolverAsignaturaPlaza(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("la asignatura de la plaza es obligatoria");
        }
        return asignaturaRepositorio.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe asignatura con codigo " + codigo));
    }

    private Aula resolverAulaFija(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return aulaRepositorio.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe aula con codigo " + codigo));
    }

    private Set<Aula> resolverAulas(List<String> codigos) {
        Set<Aula> resueltas = new LinkedHashSet<>();
        if (codigos != null) {
            for (String codigo : codigos) {
                resueltas.add(aulaRepositorio.findByCodigo(codigo)
                        .orElseThrow(() -> new IllegalArgumentException("No existe aula con codigo " + codigo)));
            }
        }
        return resueltas;
    }

    private Set<Profesor> resolverProfesores(List<String> codigos) {
        Set<Profesor> resueltos = new LinkedHashSet<>();
        for (String codigo : codigos) {   // no vacío: garantizado por validarProfesores (I7)
            resueltos.add(profesorRepositorio.findByCodigo(codigo)
                    .orElseThrow(() -> new IllegalArgumentException("No existe profesor con codigo " + codigo)));
        }
        return resueltos;
    }

    private Set<Subgrupo> resolverSubgrupos(List<String> codigos) {
        Set<Subgrupo> resueltos = new LinkedHashSet<>();
        if (codigos != null) {
            for (String codigo : codigos) {
                resueltos.add(subgrupoRepositorio.findByCodigo(codigo)
                        .orElseThrow(() -> new IllegalArgumentException("No existe subgrupo con codigo " + codigo)));
            }
        }
        return resueltos;
    }

    // ------------------------------------------------------------- proyección

    private static ActividadDTO aDTO(Actividad actividad) {
        List<PlazaDTO> plazas = actividad.getPlazas().stream()
                .sorted(Comparator.comparing(Plaza::getCodigo))
                .map(ActividadService::aDTO)
                .toList();
        return new ActividadDTO(
                actividad.getId(),
                actividad.getCodigo(),
                actividad.getAsignatura() == null ? null : actividad.getAsignatura().getCodigo(),
                actividad.getDuracionTramos(),
                actividad.getRepeticionesPorSemana(),
                actividad.getPatronTemporal().name(),
                actividad.isRequiereTutor(),
                plazas);
    }

    private static PlazaDTO aDTO(Plaza plaza) {
        return new PlazaDTO(
                plaza.getId(),
                plaza.getCodigo(),
                plaza.getAsignatura().getCodigo(),
                plaza.getAulaFija() == null ? null : plaza.getAulaFija().getCodigo(),
                codigosOrdenados(plaza.getAulasCandidatas().stream().map(Aula::getCodigo)),
                codigosOrdenados(plaza.getProfesores().stream().map(Profesor::getCodigo)),
                codigosOrdenados(plaza.getSubgrupos().stream().map(Subgrupo::getCodigo)));
    }

    /** Orden estable de códigos: los {@code Set} M:N no tienen orden intrínseco. */
    private static List<String> codigosOrdenados(Stream<String> codigos) {
        return codigos.sorted().toList();
    }
}
