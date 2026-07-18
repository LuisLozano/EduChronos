package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaAulaCompatible;
import es.yaroki.educhronos.app.catalog.AsignaturaAulaCompatibleRepository;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.TipoAula;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.AsignaturaDTO;
import es.yaroki.educhronos.app.web.dto.AsignaturaRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Asignatura} (§4.1, Bloque 8.5-A,
 * piloto del patrón CRUD de catálogo): listado ordenado, consulta por id,
 * alta, edición y borrado. Toda la validación vive AQUÍ, una sola vez (no hay
 * espejo que reflejar, a diferencia del alta de bloqueos):
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) {@code nombreCompleto} no nulo ni en blanco;
 *   <li>(c) unicidad de {@code codigo}: en el alta, ningún otro registro con ese
 *       código; en la edición, ninguno SALVO la propia entidad (una asignatura
 *       puede reguardarse con su mismo código).
 * </ul>
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado"
 * (id inexistente en consulta/edición/borrado) lanza {@link NoSuchElementException}
 * (→ 404 en el controlador); un fallo de validación lanza
 * {@link IllegalArgumentException} (→ 400). En el {@code PUT} ambos son posibles y
 * el controlador los distingue por TIPO, no por endpoint.
 *
 * <p>La edición muta la entidad gestionada vía {@link Asignatura#actualizar} (no hay
 * setters, decisión de diseño del bloque): el flush transaccional la persiste sin
 * {@code save} explícito.
 */
@Service
public class AsignaturaService {

    private final AsignaturaRepository repositorio;
    private final AsignaturaAulaCompatibleRepository compatibilidadRepositorio;

    public AsignaturaService(AsignaturaRepository repositorio,
            AsignaturaAulaCompatibleRepository compatibilidadRepositorio) {
        this.repositorio = repositorio;
        this.compatibilidadRepositorio = compatibilidadRepositorio;
    }

    /** Todas las asignaturas como {@link AsignaturaDTO}, ORDENADAS por código. */
    @Transactional(readOnly = true)
    public List<AsignaturaDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Asignatura::getCodigo))
                .map(AsignaturaService::aDTO)
                .toList();
    }

    /** Una asignatura por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public AsignaturaDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(AsignaturaService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe asignatura con id " + id));
    }

    /**
     * Da de alta una asignatura. {@link IllegalArgumentException} (→ 400) si falla
     * la validación (a)/(b) o si ya existe otra con el mismo código (c).
     */
    @Transactional
    public AsignaturaDTO crear(AsignaturaRequest peticion) {
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe una asignatura con codigo " + peticion.codigo());
        });
        Asignatura guardada =
                repositorio.save(new Asignatura(peticion.codigo(), peticion.nombreCompleto()));
        return aDTO(guardada);
    }

    /**
     * Edita una asignatura existente. {@link NoSuchElementException} (→ 404) si el id
     * no existe; {@link IllegalArgumentException} (→ 400) si falla la validación (a)/(b)
     * o si el código pisa al de OTRA asignatura (c). Reguardar la propia entidad con su
     * mismo código es válido: la unicidad se excluye a sí misma por id.
     */
    @Transactional
    public AsignaturaDTO editar(Long id, AsignaturaRequest peticion) {
        Asignatura entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe asignatura con id " + id));
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new IllegalArgumentException(
                            "Ya existe otra asignatura con codigo " + peticion.codigo());
                });
        entidad.actualizar(peticion.codigo(), peticion.nombreCompleto());
        return aDTO(entidad);
    }

    /** Borra una asignatura por id. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional
    public void borrar(Long id) {
        Asignatura entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe asignatura con id " + id));
        List<Referencia> entrantes = List.of(
                new Referencia("actividad(es)", repositorio.contarActividades(id)),
                new Referencia("plaza(s)", repositorio.contarPlazas(id)));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
        repositorio.delete(entidad);
    }

    // ------------------------------------------ compatibilidades de aula (I3, §4.7)

    /**
     * Tipos de aula compatibles de una asignatura, como nombres de {@link TipoAula} ORDENADOS
     * por el orden natural del enum. {@link NoSuchElementException} (→ 404) si el id no existe.
     * Lista vacía = asignatura irrestricta (semántica C de §4.7).
     */
    @Transactional(readOnly = true)
    public List<String> obtenerAulasCompatibles(Long id) {
        Asignatura entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe asignatura con id " + id));
        return ordenar(tiposDe(entidad));
    }

    /**
     * REEMPLAZO TOTAL e idempotente de las compatibilidades de una asignatura: borra las filas
     * actuales y crea las de {@code tiposCrudos}. Lista vacía = borrar todas (irrestricta).
     * {@link NoSuchElementException} (→ 404) si el id no existe; {@link IllegalArgumentException}
     * (→ 400) si algún valor no parsea a {@link TipoAula} (lo nombra y lista los válidos) o si la
     * lista trae un tipo duplicado (lo nombra). Devuelve la lista resultante ordenada por enum.
     */
    @Transactional
    public List<String> reemplazarAulasCompatibles(Long id, List<String> tiposCrudos) {
        Asignatura entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe asignatura con id " + id));
        Set<TipoAula> nuevos = new LinkedHashSet<>();
        for (String crudo : tiposCrudos == null ? List.<String>of() : tiposCrudos) {
            TipoAula tipo = parseTipoAula(crudo);
            if (!nuevos.add(tipo)) {
                throw new IllegalArgumentException("tipo de aula duplicado: " + tipo.name());
            }
        }
        // Borra lo actual y FLUSHEA antes de insertar: así el DELETE precede al INSERT y no choca
        // con la UNIQUE (asignatura, tipo) cuando la lista repite un tipo ya presente (idempotencia).
        compatibilidadRepositorio.deleteAll(compatibilidadRepositorio.findByAsignatura(entidad));
        compatibilidadRepositorio.flush();
        for (TipoAula tipo : nuevos) {
            compatibilidadRepositorio.save(new AsignaturaAulaCompatible(entidad, tipo));
        }
        return ordenar(nuevos);
    }

    /** Los {@link TipoAula} compatibles de una asignatura (sin orden intrínseco). */
    private Set<TipoAula> tiposDe(Asignatura asignatura) {
        return compatibilidadRepositorio.findByAsignatura(asignatura).stream()
                .map(AsignaturaAulaCompatible::getTipoAula)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Nombres de {@link TipoAula} ordenados por el orden natural del enum (orden de declaración). */
    private static List<String> ordenar(Collection<TipoAula> tipos) {
        return tipos.stream().sorted().map(TipoAula::name).toList();
    }

    /** Parsea un nombre a {@link TipoAula}; valor malo → 400 que lo nombra y lista los válidos
     * (mismo patrón que {@code parsePatron} en {@code ActividadService}). */
    private static TipoAula parseTipoAula(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "tipo de aula en blanco. Valores validos: " + Arrays.toString(TipoAula.values()));
        }
        try {
            return TipoAula.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "tipo de aula invalido: '" + valor + "'. Valores validos: "
                            + Arrays.toString(TipoAula.values()));
        }
    }

    /** Reglas (a) y (b): código y nombre no nulos ni en blanco. */
    private static void validar(AsignaturaRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
        if (peticion.nombreCompleto() == null || peticion.nombreCompleto().isBlank()) {
            throw new IllegalArgumentException("nombreCompleto es obligatorio");
        }
    }

    private static AsignaturaDTO aDTO(Asignatura asignatura) {
        return new AsignaturaDTO(
                asignatura.getId(), asignatura.getCodigo(), asignatura.getNombreCompleto());
    }
}
