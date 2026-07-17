package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.AulaRepository;
import es.yaroki.educhronos.app.catalog.TipoAula;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.AulaDTO;
import es.yaroki.educhronos.app.web.dto.AulaRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Aula} (§4.1, Bloque 8.5-A', réplica del
 * piloto {@code AsignaturaService}): listado ordenado, consulta por id, alta,
 * edición y borrado. Toda la validación vive AQUÍ, una sola vez:
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) {@code tipo} no nulo ni en blanco y PARSEABLE a {@link TipoAula};
 *   <li>(c) unicidad de {@code codigo}: en el alta, ningún otro registro con ese
 *       código; en la edición, ninguno SALVO la propia entidad.
 * </ul>
 *
 * <p>{@code capacidad}/{@code edificio}/{@code planta}/{@code sector} son nullable
 * de verdad (D-4): llegan y se persisten null sin validarse.
 *
 * <p><b>El tipo se parsea con {@link TipoAula#valueOf} DENTRO de {@link #validar}</b>
 * (que devuelve el enum ya resuelto): si el valor no existe, se relanza como
 * {@link IllegalArgumentException} con un mensaje accionable que NOMBRA el valor
 * recibido y lista los válidos ({@code Arrays.toString(TipoAula.values())}). Por eso
 * la ENTRADA ({@code AulaRequest.tipo}) es String — para poder dar ese 400 en vez
 * del error opaco de deserialización de un enum. La SALIDA ({@code AulaDTO.tipo})
 * también es String ({@code TipoAula.name()}), pero por coherencia con el patrón de
 * borde de 7A (ver javadoc de {@code AulaDTO}).
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" lanza
 * {@link NoSuchElementException} (→ 404 en el controlador); un fallo de validación
 * lanza {@link IllegalArgumentException} (→ 400). En el {@code PUT} ambos son
 * posibles y el controlador los distingue por TIPO, no por endpoint.
 *
 * <p>La edición muta la entidad gestionada vía {@link Aula#actualizar} (no hay
 * setters): el flush transaccional la persiste sin {@code save} explícito.
 */
@Service
public class AulaService {

    private final AulaRepository repositorio;

    public AulaService(AulaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Todas las aulas como {@link AulaDTO}, ORDENADAS por código. */
    @Transactional(readOnly = true)
    public List<AulaDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Aula::getCodigo))
                .map(AulaService::aDTO)
                .toList();
    }

    /** Un aula por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public AulaDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(AulaService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe aula con id " + id));
    }

    /**
     * Da de alta un aula. {@link IllegalArgumentException} (→ 400) si falla la
     * validación (a)/(b) o si ya existe otra con el mismo código (c).
     */
    @Transactional
    public AulaDTO crear(AulaRequest peticion) {
        TipoAula tipo = validar(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe un aula con codigo " + peticion.codigo());
        });
        Aula guardada = repositorio.save(new Aula(
                peticion.codigo(), tipo, peticion.capacidad(),
                peticion.edificio(), peticion.planta(), peticion.sector()));
        return aDTO(guardada);
    }

    /**
     * Edita un aula existente. {@link NoSuchElementException} (→ 404) si el id no
     * existe; {@link IllegalArgumentException} (→ 400) si falla la validación (a)/(b)
     * o si el código pisa al de OTRA aula (c). Reguardar la propia entidad con su
     * mismo código es válido: la unicidad se excluye a sí misma por id.
     */
    @Transactional
    public AulaDTO editar(Long id, AulaRequest peticion) {
        Aula entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe aula con id " + id));
        TipoAula tipo = validar(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new IllegalArgumentException(
                            "Ya existe otra aula con codigo " + peticion.codigo());
                });
        entidad.actualizar(peticion.codigo(), tipo, peticion.capacidad(),
                peticion.edificio(), peticion.planta(), peticion.sector());
        return aDTO(entidad);
    }

    /**
     * Borra un aula por id. {@link NoSuchElementException} (→ 404) si no existe;
     * {@link ReferenciaEntranteException} (→ 409) si alguien la referencia todavía.
     *
     * <p>PILOTO del borrado amable (8.5-C2b): consulta las CUATRO FK entrantes del mapa
     * ({@code schema.sql}) antes del {@code delete} y, si alguna tiene filas, aborta
     * nombrándolas con su conteo real en vez de dejar morder a la FK (500 opaco).
     */
    @Transactional
    public void borrar(Long id) {
        Aula entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe aula con id " + id));
        List<Referencia> entrantes = List.of(
                new Referencia("plaza(s)", repositorio.contarPlazasConAulaFija(id)),
                new Referencia("plaza(s) candidata(s)", repositorio.contarPlazasCandidatas(id)),
                new Referencia("aula(s) bloqueada(s)", repositorio.contarAulasBloqueadas(id)),
                new Referencia("sesion(es)", repositorio.contarSesiones(id)));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
        repositorio.delete(entidad);
    }

    /**
     * Reglas (a) y (b) y parseo del tipo: código no en blanco, tipo no en blanco y
     * parseable a {@link TipoAula}. Devuelve el enum ya resuelto para no reparsear en
     * el alta/edición. Un tipo desconocido produce un 400 accionable que nombra el
     * valor y lista los válidos. Los cuatro campos nullable (D-4) no se validan.
     */
    private static TipoAula validar(AulaRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
        if (peticion.tipo() == null || peticion.tipo().isBlank()) {
            throw new IllegalArgumentException("tipo es obligatorio");
        }
        try {
            return TipoAula.valueOf(peticion.tipo());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "tipo invalido: '" + peticion.tipo() + "'. Valores validos: "
                            + Arrays.toString(TipoAula.values()));
        }
    }

    private static AulaDTO aDTO(Aula aula) {
        return new AulaDTO(
                aula.getId(), aula.getCodigo(), aula.getTipo().name(),
                aula.getCapacidad(), aula.getEdificio(), aula.getPlanta(), aula.getSector());
    }
}
