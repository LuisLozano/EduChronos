package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.SubgrupoDTO;
import es.yaroki.educhronos.app.web.dto.SubgrupoRequest;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del CRUD de {@code Subgrupo} (§4.2, Bloque 8.5-B, réplica del
 * piloto {@code AulaService}): listado ordenado por código, consulta por id, alta,
 * edición y borrado. Toda la validación vive AQUÍ, una sola vez:
 *
 * <ul>
 *   <li>(a) {@code codigo} no nulo ni en blanco;
 *   <li>(b) {@code grupos} no nulo ni vacío (D-nueva-1, invariante I6: un subgrupo
 *       necesita ≥1 grupo);
 *   <li>(c) cada código de {@code grupos} resoluble vía
 *       {@link GrupoAdministrativoRepository#findByCodigo} (D-nueva-4, Opción 1): si
 *       ALGUNO no existe, un 400 que nombra el código faltante;
 *   <li>(d) unicidad de {@code codigo}: en el alta ningún otro registro con ese
 *       código; en la edición ninguno SALVO la propia entidad (exclusión por id);
 *   <li>(e) SOLO EN EDICIÓN Y BORRADO: la entidad existente no es el subgrupo mono-Di
 *       de un PDC. Ver {@link #esMonoDiDePdc}.
 * </ul>
 *
 * <p>La edición REEMPLAZA por completo la población de grupos (D-nueva, vía
 * {@link Subgrupo#actualizar}), no hace unión ni deltas.
 *
 * <p><b>Dos familias de excepción, dos códigos HTTP.</b> "No encontrado" lanza
 * {@link NoSuchElementException} (→ 404); un fallo de validación lanza
 * {@link IllegalArgumentException} (→ 400, con el mensaje en el reason). El controlador
 * los distingue por TIPO.
 *
 * <p><b>(e) es 400 y NO 409, aunque suene a "esto lo posee otro".</b> El 409 está
 * reservado en todo el proyecto a la REFERENCIA ENTRANTE —filas de fuera que apuntan a
 * la entidad, contadas por el mapa inverso de FK ({@link ReferenciaEntranteException})—
 * y aquí no hay ninguna: lo que falla es la petición en sí, contra un estado que se lee
 * de la propia entidad. Es la misma corrección que S76 aplicó a "un PDC por padre".
 */
@Service
public class SubgrupoService {

    private final SubgrupoRepository repositorio;
    private final GrupoAdministrativoRepository grupoRepositorio;

    public SubgrupoService(SubgrupoRepository repositorio,
                           GrupoAdministrativoRepository grupoRepositorio) {
        this.repositorio = repositorio;
        this.grupoRepositorio = grupoRepositorio;
    }

    /** Todos los subgrupos como {@link SubgrupoDTO}, ORDENADOS por código. */
    @Transactional(readOnly = true)
    public List<SubgrupoDTO> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(Subgrupo::getCodigo))
                .map(SubgrupoService::aDTO)
                .toList();
    }

    /** Un subgrupo por su id sintético. {@link NoSuchElementException} (→ 404) si no existe. */
    @Transactional(readOnly = true)
    public SubgrupoDTO obtener(Long id) {
        return repositorio.findById(id)
                .map(SubgrupoService::aDTO)
                .orElseThrow(() -> new NoSuchElementException("No existe subgrupo con id " + id));
    }

    /**
     * Da de alta un subgrupo. {@link IllegalArgumentException} (→ 400) si falla la
     * validación (a)/(b)/(c) o si ya existe otro con el mismo código (d).
     */
    @Transactional
    public SubgrupoDTO crear(SubgrupoRequest peticion) {
        validarCodigo(peticion);
        Set<GrupoAdministrativo> grupos = resolverGrupos(peticion);
        repositorio.findByCodigo(peticion.codigo()).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "Ya existe un subgrupo con codigo " + peticion.codigo());
        });
        Subgrupo guardado = repositorio.save(new Subgrupo(peticion.codigo(), grupos));
        return aDTO(guardado);
    }

    /**
     * Edita un subgrupo existente. {@link NoSuchElementException} (→ 404) si el id no
     * existe; {@link IllegalArgumentException} (→ 400) si falla la validación
     * (a)/(b)/(c)/(e) o si el código pisa al de OTRO subgrupo (d). Reguardar la propia
     * entidad con su mismo código es válido: la unicidad se excluye a sí misma por id.
     * La población de grupos se reemplaza por completo.
     *
     * <p>(e) va PRIMERA, antes de mirar el cuerpo: es una precondición del RECURSO, no
     * del payload. Renombrar el mono-Di es lo más dañino que se puede hacer aquí —el
     * agregado PDC lo localiza por código derivado, no por FK—, así que se ataja antes
     * de validar nada más.
     */
    @Transactional
    public SubgrupoDTO editar(Long id, SubgrupoRequest peticion) {
        Subgrupo entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe subgrupo con id " + id));
        if (esMonoDiDePdc(entidad)) {
            throw new IllegalArgumentException(
                    "El subgrupo " + entidad.getCodigo() + " es el subgrupo mono-Di de un grupo"
                            + " PDC y no se edita por este flujo: se gestiona con su PDC, en"
                            + " /api/grupos/{idPadre}/pdc");
        }
        validarCodigo(peticion);
        Set<GrupoAdministrativo> grupos = resolverGrupos(peticion);
        repositorio.findByCodigo(peticion.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException(
                            "Ya existe otro subgrupo con codigo " + peticion.codigo());
                });
        entidad.actualizar(peticion.codigo(), grupos);
        return aDTO(entidad);
    }

    /**
     * Borra un subgrupo por id. {@link NoSuchElementException} (→ 404) si no existe;
     * {@link IllegalArgumentException} (→ 400) si es el mono-Di de un PDC (e);
     * {@link ReferenciaEntranteException} (→ 409) si alguna plaza lo retiene.
     *
     * <p>(e) va antes que el conteo de referentes: si nadie lo usara, el borrado saldría
     * con 204 y dejaría al PDC sin la mitad de su agregado —y, de rebote, borrable por el
     * {@code DELETE /api/grupos/{id}} plano, cuya guarda cuenta precisamente ese subgrupo—.
     */
    @Transactional
    public void borrar(Long id) {
        Subgrupo entidad = repositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe subgrupo con id " + id));
        if (esMonoDiDePdc(entidad)) {
            throw new IllegalArgumentException(
                    "El subgrupo " + entidad.getCodigo() + " es el subgrupo mono-Di de un grupo"
                            + " PDC y no se borra por este flujo: lo borra el borrado de su PDC,"
                            + " en /api/grupos/{idPadre}/pdc");
        }
        // Solo plaza_subgrupo es referente ENTRANTE: subgrupo_grupo es la POBLACIÓN propia del
        // subgrupo (Subgrupo.grupos es owner del @ManyToMany), que Hibernate limpia al borrar; no
        // impide el borrado (contarla vetaría a TODO subgrupo, que exige >=1 grupo). Ver Actividad.
        List<Referencia> entrantes = List.of(
                new Referencia("plaza(s)", repositorio.contarPlazas(id)));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
        repositorio.delete(entidad);
    }

    /**
     * Regla (e): ¿es esta la mitad "subgrupo" del agregado PDC, la que crea
     * {@code PdcService} en el alta compuesta y borra en el borrado? Predicado, no
     * validación: lo consumen los DOS caminos de escritura hacia la entidad —
     * {@link #editar} y {@link #borrar}—, cada uno con su mensaje, porque el verbo
     * cambia. Dos llamantes en la misma clase no piden un funnel; no hay un tercero.
     *
     * <p><b>El predicado es EXACTAMENTE UN grupo Y de tipo {@code DIVERSIFICACION_PDC}.</b>
     * El "exactamente uno" no es detalle de implementación, es la mitad del significado:
     * un subgrupo cuya población son VARIOS grupos PDC es el ámbito COMPARTIDO de los Di
     * (el tronco A8 que cursan juntos los diversificados de varios grupos, §6.2/S23), un
     * caso legítimo del modelo que no pertenece a ningún agregado y que debe seguir
     * siendo editable y borrable por este CRUD. Relajarlo a "contiene algún grupo PDC"
     * dejaría ese caso sin forma de gestionarse por la API.
     *
     * <p>Se resuelve EN MEMORIA sobre la relación ya cargada: {@code Subgrupo.grupos} es
     * el lado propietario del N:M, así que navegarlo dentro de la transacción es una
     * lectura que la sesión ya sabe hacer. No hace falta query ni repositorio nuevo.
     */
    private static boolean esMonoDiDePdc(Subgrupo subgrupo) {
        Set<GrupoAdministrativo> poblacion = subgrupo.getGrupos();
        return poblacion.size() == 1
                && poblacion.iterator().next().getTipo() == TipoGrupo.DIVERSIFICACION_PDC;
    }

    /** Regla (a): código no nulo ni en blanco. */
    private static void validarCodigo(SubgrupoRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
    }

    /**
     * Reglas (b) y (c): la lista de códigos no es nula ni vacía, y cada código resuelve
     * a un grupo existente. Un código no resoluble produce un 400 que lo nombra.
     * Devuelve el {@code Set} de grupos ya resueltos para el ctor/actualizar.
     */
    private Set<GrupoAdministrativo> resolverGrupos(SubgrupoRequest peticion) {
        if (peticion.grupos() == null || peticion.grupos().isEmpty()) {
            throw new IllegalArgumentException("un subgrupo necesita al menos un grupo");
        }
        Set<GrupoAdministrativo> resueltos = new LinkedHashSet<>();
        for (String codigoGrupo : peticion.grupos()) {
            GrupoAdministrativo grupo = grupoRepositorio.findByCodigo(codigoGrupo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe grupo con codigo " + codigoGrupo));
            resueltos.add(grupo);
        }
        return resueltos;
    }

    private static SubgrupoDTO aDTO(Subgrupo subgrupo) {
        List<String> codigos = subgrupo.getGrupos().stream()
                .map(GrupoAdministrativo::getCodigo)
                .sorted()
                .toList();
        return new SubgrupoDTO(subgrupo.getId(), subgrupo.getCodigo(), codigos);
    }
}
