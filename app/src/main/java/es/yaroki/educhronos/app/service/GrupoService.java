package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Nivel;
import es.yaroki.educhronos.app.catalog.NivelRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.GrupoRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code GrupoAdministrativo} (§4.1, Bloque 8.5-B,
 * réplica del piloto {@code AulaService}), RESTRINGIDO a grupos ORDINARIOS: listado
 * ordenado por código, consulta por id, alta, edición y borrado. Toda la validación
 * vive AQUÍ, una sola vez:
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) {@code tipo} no nulo ni en blanco, PARSEABLE a {@link TipoGrupo} y, por
 *       D-nueva-2 (lista blanca), igual a {@code ORDINARIO}: PDC y virtuales de
 *       optativa se crean por otros flujos y aquí se rechazan con un 400 que nombra
 *       el tipo;
 *   <li>(c) {@code nivel} resoluble por código vía {@link NivelRepository#findByCodigo}
 *       (D-nueva: la FK viaja como clave natural, no como id);
 *   <li>(d) unicidad de {@code codigo}: en el alta ningún otro registro con ese
 *       código; en la edición ninguno SALVO la propia entidad (exclusión por id).
 * </ul>
 *
 * <p>{@code grupoPadre} queda SIEMPRE null en este bloque (los PDC que lo usan son
 * 8.5-D): el ctor lo recibe null y el Request ni lo expone.
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" lanza
 * {@link NoSuchElementException} (→ 404 en el controlador); un fallo de validación
 * lanza {@link IllegalArgumentException} (→ 400, con el mensaje en el reason). En el
 * {@code PUT} ambos son posibles y el controlador los distingue por TIPO.
 */
@Service
public class GrupoService {

    private final GrupoAdministrativoRepository repositorio;
    private final NivelRepository nivelRepositorio;

    public GrupoService(GrupoAdministrativoRepository repositorio, NivelRepository nivelRepositorio) {
        this.repositorio = repositorio;
        this.nivelRepositorio = nivelRepositorio;
    }

    /** Todos los grupos como {@link GrupoDTO}, ORDENADOS por código. */
    @Transactional(readOnly = true)
    public List<GrupoDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(GrupoAdministrativo::getCodigo))
                .map(GrupoService::aDTO)
                .toList();
    }

    /** Un grupo por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public GrupoDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(GrupoService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + id));
    }

    /**
     * Da de alta un grupo ordinario. {@link IllegalArgumentException} (→ 400) si falla
     * la validación (a)/(b)/(c) o si ya existe otro con el mismo código (d).
     */
    @Transactional
    public GrupoDTO crear(GrupoRequest peticion) {
        validarCodigo(peticion);
        TipoGrupo tipo = validarTipo(peticion);
        Nivel nivel = resolverNivel(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe un grupo con codigo " + peticion.codigo());
        });
        GrupoAdministrativo guardado = repositorio.save(
                new GrupoAdministrativo(peticion.codigo(), nivel, tipo, null));
        return aDTO(guardado);
    }

    /**
     * Edita un grupo existente. {@link NoSuchElementException} (→ 404) si el id no
     * existe; {@link IllegalArgumentException} (→ 400) si falla la validación
     * (a)/(b)/(c) o si el código pisa al de OTRO grupo (d). Reguardar la propia
     * entidad con su mismo código es válido: la unicidad se excluye a sí misma por id.
     */
    @Transactional
    public GrupoDTO editar(Long id, GrupoRequest peticion) {
        GrupoAdministrativo entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + id));
        validarCodigo(peticion);
        TipoGrupo tipo = validarTipo(peticion);
        Nivel nivel = resolverNivel(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException(
                            "Ya existe otro grupo con codigo " + peticion.codigo());
                });
        entidad.actualizar(peticion.codigo(), nivel, tipo);
        return aDTO(entidad);
    }

    /** Borra un grupo por id. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional
    public void borrar(Long id) {
        GrupoAdministrativo entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + id));
        List<Referencia> entrantes = List.of(
                new Referencia("subgrupo(s)", repositorio.contarSubgrupos(id)),
                new Referencia("grupo(s) hijo(s)", repositorio.contarGruposHijos(id)));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
        repositorio.delete(entidad);
    }

    /** Regla (a): código no nulo ni en blanco. */
    private static void validarCodigo(GrupoRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
    }

    /**
     * Regla (b): tipo no en blanco, parseable a {@link TipoGrupo} y, por lista blanca
     * (D-nueva-2), igual a {@code ORDINARIO}. Un tipo desconocido produce un 400 que
     * nombra el valor y lista los válidos; un tipo no ordinario, un 400 que nombra el
     * tipo rechazado. Devuelve el enum ya resuelto.
     */
    private static TipoGrupo validarTipo(GrupoRequest peticion) {
        if (peticion.tipo() == null || peticion.tipo().isBlank()) {
            throw new IllegalArgumentException("tipo es obligatorio");
        }
        TipoGrupo tipo;
        try {
            tipo = TipoGrupo.valueOf(peticion.tipo());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "tipo invalido: '" + peticion.tipo() + "'. Valores validos: "
                            + Arrays.toString(TipoGrupo.values()));
        }
        if (tipo != TipoGrupo.ORDINARIO) {
            throw new IllegalArgumentException(
                    "tipo no permitido en este flujo: " + tipo.name());
        }
        return tipo;
    }

    /**
     * Regla (c): resuelve el nivel por su código de negocio. Un código en blanco o no
     * existente produce un 400 que nombra el código recibido.
     */
    private Nivel resolverNivel(GrupoRequest peticion) {
        if (peticion.nivel() == null || peticion.nivel().isBlank()) {
            throw new IllegalArgumentException("nivel es obligatorio");
        }
        return nivelRepositorio.findByCodigo(peticion.nivel())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe nivel con codigo " + peticion.nivel()));
    }

    private static GrupoDTO aDTO(GrupoAdministrativo grupo) {
        return new GrupoDTO(
                grupo.getId(), grupo.getCodigo(),
                grupo.getNivel().getCodigo(), grupo.getTipo().name());
    }
}
