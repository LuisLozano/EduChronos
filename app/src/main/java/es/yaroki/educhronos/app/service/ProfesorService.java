package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.web.dto.ProfesorDTO;
import es.yaroki.educhronos.app.web.dto.ProfesorRequest;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Profesor} (§4.1, Bloque 8.5-A', réplica
 * del piloto {@code AsignaturaService}): listado ordenado, consulta por id, alta,
 * edición y borrado. Toda la validación vive AQUÍ, una sola vez:
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) {@code nombreCompleto} no nulo ni en blanco;
 *   <li>(c) unicidad de {@code codigo}: en el alta, ningún otro registro con ese
 *       código; en la edición, ninguno SALVO la propia entidad.
 * </ul>
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" lanza
 * {@link NoSuchElementException} (→ 404 en el controlador); un fallo de validación
 * lanza {@link IllegalArgumentException} (→ 400). En el {@code PUT} ambos son
 * posibles y el controlador los distingue por TIPO, no por endpoint.
 *
 * <p>La edición muta la entidad gestionada vía {@link Profesor#actualizar} (no hay
 * setters): el flush transaccional la persiste sin {@code save} explícito.
 */
@Service
public class ProfesorService {

    private final ProfesorRepository repositorio;

    public ProfesorService(ProfesorRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Todos los profesores como {@link ProfesorDTO}, ORDENADOS por código. */
    @Transactional(readOnly = true)
    public List<ProfesorDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Profesor::getCodigo))
                .map(ProfesorService::aDTO)
                .toList();
    }

    /** Un profesor por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public ProfesorDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(ProfesorService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe profesor con id " + id));
    }

    /**
     * Da de alta un profesor. {@link IllegalArgumentException} (→ 400) si falla la
     * validación (a)/(b) o si ya existe otro con el mismo código (c).
     */
    @Transactional
    public ProfesorDTO crear(ProfesorRequest peticion) {
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe un profesor con codigo " + peticion.codigo());
        });
        Profesor guardado =
                repositorio.save(new Profesor(peticion.codigo(), peticion.nombreCompleto()));
        return aDTO(guardado);
    }

    /**
     * Edita un profesor existente. {@link NoSuchElementException} (→ 404) si el id no
     * existe; {@link IllegalArgumentException} (→ 400) si falla la validación (a)/(b)
     * o si el código pisa al de OTRO profesor (c). Reguardar la propia entidad con su
     * mismo código es válido: la unicidad se excluye a sí misma por id.
     */
    @Transactional
    public ProfesorDTO editar(Long id, ProfesorRequest peticion) {
        Profesor entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe profesor con id " + id));
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException(
                            "Ya existe otro profesor con codigo " + peticion.codigo());
                });
        entidad.actualizar(peticion.codigo(), peticion.nombreCompleto());
        return aDTO(entidad);
    }

    /** Borra un profesor por id. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional
    public void borrar(Long id) {
        Profesor entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe profesor con id " + id));
        repositorio.delete(entidad);
    }

    /** Reglas (a) y (b): código y nombre no nulos ni en blanco. */
    private static void validar(ProfesorRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
        if (peticion.nombreCompleto() == null || peticion.nombreCompleto().isBlank()) {
            throw new IllegalArgumentException("nombreCompleto es obligatorio");
        }
    }

    private static ProfesorDTO aDTO(Profesor profesor) {
        return new ProfesorDTO(
                profesor.getId(), profesor.getCodigo(), profesor.getNombreCompleto());
    }
}
