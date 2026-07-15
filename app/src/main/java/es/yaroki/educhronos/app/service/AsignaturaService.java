package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.web.dto.AsignaturaDTO;
import es.yaroki.educhronos.app.web.dto.AsignaturaRequest;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
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

    public AsignaturaService(AsignaturaRepository repositorio) {
        this.repositorio = repositorio;
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
        repositorio.delete(entidad);
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
