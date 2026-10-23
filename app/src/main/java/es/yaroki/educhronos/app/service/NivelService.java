package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Asignatura;
import es.yaroki.educhronos.app.catalog.AsignaturaRepository;
import es.yaroki.educhronos.app.catalog.Nivel;
import es.yaroki.educhronos.app.catalog.NivelRepository;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Nombrados;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.NivelDTO;
import es.yaroki.educhronos.app.web.dto.NivelRequest;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Nivel} (§4.1, Bloque 8.5-A', réplica
 * del piloto {@code AsignaturaService}): listado ordenado, consulta por id, alta,
 * edición y borrado. Toda la validación vive AQUÍ, una sola vez:
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) unicidad de {@code codigo}: en el alta, ningún otro registro con ese
 *       código; en la edición, ninguno SALVO la propia entidad.
 * </ul>
 *
 * <p>El {@code orden} es un {@code int} sin regla de validación: solo fija el
 * criterio de ordenación de {@link #listar()} (D-1: por {@code orden}, NO por
 * {@code codigo} como el resto de raíces).
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" lanza
 * {@link NoSuchElementException} (→ 404 en el controlador); un fallo de validación
 * lanza {@link IllegalArgumentException} (→ 400). En el {@code PUT} ambos son
 * posibles y el controlador los distingue por TIPO, no por endpoint.
 *
 * <p>La edición muta la entidad gestionada vía {@link Nivel#actualizar} (no hay
 * setters): el flush transaccional la persiste sin {@code save} explícito.
 *
 * <p><b>Asignaturas del nivel</b> (S250, C-asignatura-con-nivel). Su código lleva el del nivel
 * ({@code Mat-1ESO}): renombrar el nivel lo recompone en la misma transacción, y borrar un nivel
 * con asignaturas da 409 y las nombra, como el borrado de una asignatura con actividades.
 */
@Service
public class NivelService {

    private final NivelRepository repositorio;
    private final AsignaturaRepository asignaturaRepositorio;

    public NivelService(NivelRepository repositorio, AsignaturaRepository asignaturaRepositorio) {
        this.repositorio = repositorio;
        this.asignaturaRepositorio = asignaturaRepositorio;
    }

    /** Todos los niveles como {@link NivelDTO}, ORDENADOS por {@code orden} (D-1). */
    @Transactional(readOnly = true)
    public List<NivelDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparingInt(Nivel::getOrden))
                .map(NivelService::aDTO)
                .toList();
    }

    /** Un nivel por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public NivelDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(NivelService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe nivel con id " + id));
    }

    /**
     * Da de alta un nivel. {@link IllegalArgumentException} (→ 400) si falla la
     * validación (a) o si ya existe otro con el mismo código (b).
     */
    @Transactional
    public NivelDTO crear(NivelRequest peticion) {
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe un nivel con codigo " + peticion.codigo());
        });
        Nivel guardado = repositorio.save(new Nivel(peticion.codigo(), peticion.orden()));
        return aDTO(guardado);
    }

    /**
     * Edita un nivel existente. {@link NoSuchElementException} (→ 404) si el id no
     * existe; {@link IllegalArgumentException} (→ 400) si falla la validación (a) o
     * si el código pisa al de OTRO nivel (b). Reguardar la propia entidad con su
     * mismo código es válido: la unicidad se excluye a sí misma por id.
     *
     * <p>Si el código cambia, el de cada asignatura del nivel se recompone con el nuevo (S250).
     * ANTES de tocar nada se comprueba que ninguno de los códigos nuevos sea ya el de una
     * asignatura de otro nivel o sin nivel: si alguno lo es, 400 que lo nombra y no cambia nada.
     * Las del propio nivel no cuentan, porque cambian todas a la vez.
     */
    @Transactional
    public NivelDTO editar(Long id, NivelRequest peticion) {
        Nivel entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe nivel con id " + id));
        validar(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException(
                            "Ya existe otro nivel con codigo " + peticion.codigo());
                });
        List<Asignatura> suyas = peticion.codigo().equals(entidad.getCodigo())
                ? List.of()
                : asignaturaRepositorio.findByNivelId(id);
        for (Asignatura asignatura : suyas) {
            String nuevo = Asignatura.componerCodigo(asignatura.getCodigoCorto(), peticion.codigo());
            asignaturaRepositorio.findByCodigo(nuevo)
                    .filter(otra -> otra.getNivel() == null || !otra.getNivel().getId().equals(id))
                    .ifPresent(otra -> {
                        throw new IllegalArgumentException("No se puede renombrar el nivel: la asignatura "
                                + asignatura.getCodigo() + " pasaría a " + nuevo
                                + ", que ya es el código de otra asignatura");
                    });
        }
        entidad.actualizar(peticion.codigo(), peticion.orden());
        suyas.forEach(Asignatura::recomponerCodigo);
        return aDTO(entidad);
    }

    /** Borra un nivel por id. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional
    public void borrar(Long id) {
        Nivel entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe nivel con id " + id));
        List<String> asignaturas = asignaturaRepositorio.findByNivelId(id).stream()
                .map(Asignatura::getCodigo)
                .toList();
        List<Referencia> entrantes = List.of(
                new Referencia("grupo(s)", repositorio.contarGrupos(id)),
                new Referencia("asignatura(s)", asignaturas.size()));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            // Las asignaturas se nombran (S250), como las actividades y plazas de una asignatura.
            throw new ReferenciaEntranteException(entrantes,
                    List.of(new Nombrados("Asignaturas", asignaturas)));
        }
        repositorio.delete(entidad);
    }

    /** Regla (a): código no nulo ni en blanco. */
    private static void validar(NivelRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
    }

    private static NivelDTO aDTO(Nivel nivel) {
        return new NivelDTO(nivel.getId(), nivel.getCodigo(), nivel.getOrden());
    }
}
