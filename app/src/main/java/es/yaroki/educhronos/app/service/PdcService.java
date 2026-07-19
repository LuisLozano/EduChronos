package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.SubgrupoRepository;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.GrupoDTO;
import es.yaroki.educhronos.app.web.dto.PdcRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del sub-recurso PDC (§4.1, Bloque 8.5-D1): alta/consulta/borrado
 * de un grupo de Diversificación ({@link TipoGrupo#DIVERSIFICACION_PDC}) COLGADO de su
 * grupo ordinario padre. Distinto y separado de {@link GrupoService} (que sigue
 * RESTRINGIDO a ORDINARIOS y no se toca): el PDC no se crea por el CRUD plano de grupos
 * sino por esta vía compuesta.
 *
 * <p><b>Alta compuesta en UNA transacción</b> ({@link #crear}): crea el
 * {@code GrupoAdministrativo} PDC —nivel HEREDADO del padre, {@code grupoPadre = padre}—
 * y su subgrupo mono-Di {@code codigo + "-Completo"} cuya población es SOLO el PDC (nunca
 * el padre: regla S23; enlazar el padre haría INFEASIBLE al solver).
 *
 * <p>Toda la validación vive AQUÍ, una sola vez, en este orden:
 * <ul>
 *   <li>padre inexistente por id → {@link NoSuchElementException} (→ 404);
 *   <li>{@code padre.tipo != ORDINARIO} → {@link IllegalArgumentException} (→ 400);
 *   <li>{@code codigo} nulo o en blanco → 400;
 *   <li>{@code codigo} ya usado por otro grupo → 400;
 *   <li>el padre YA tiene un PDC (vía {@code contarGruposHijos}) → 400;
 *   <li>el código derivado del subgrupo ya existe → 400 que NOMBRA el código chocado.
 * </ul>
 *
 * <p><b>Borrado</b> ({@link #borrar}): borra el PDC y su subgrupo mono-Di, pero ANTES
 * comprueba la referencia ENTRANTE del subgrupo ({@code contarPlazas}); si está en alguna
 * plaza lanza {@link ReferenciaEntranteException} (→ 409) y no borra nada. No usa
 * {@code contarSubgrupos}/{@code contarGruposHijos} como guarda: contarían el propio
 * agregado que este borrado posee (su subgrupo mono-Di), un falso positivo.
 */
@Service
public class PdcService {

    /** Sufijo del subgrupo mono-Di derivado del código del PDC (regla de alta (b)). */
    private static final String SUFIJO_SUBGRUPO = "-Completo";

    private final GrupoAdministrativoRepository grupoRepositorio;
    private final SubgrupoRepository subgrupoRepositorio;

    public PdcService(GrupoAdministrativoRepository grupoRepositorio,
                      SubgrupoRepository subgrupoRepositorio) {
        this.grupoRepositorio = grupoRepositorio;
        this.subgrupoRepositorio = subgrupoRepositorio;
    }

    /**
     * Alta compuesta del PDC + su subgrupo mono-Di. Ver el orden de validación en el
     * Javadoc de clase. {@link NoSuchElementException} (→ 404) si el padre no existe;
     * {@link IllegalArgumentException} (→ 400) en cualquier fallo de validación.
     */
    @Transactional
    public GrupoDTO crear(Long idPadre, PdcRequest peticion) {
        GrupoAdministrativo padre = grupoRepositorio.findById(idPadre)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + idPadre));
        if (padre.getTipo() != TipoGrupo.ORDINARIO) {
            throw new IllegalArgumentException(
                    "El grupo padre no es ORDINARIO: " + padre.getTipo().name());
        }
        if (peticion == null || peticion.codigo() == null || peticion.codigo().isBlank()) {
            throw new IllegalArgumentException("codigo es obligatorio");
        }
        String codigo = peticion.codigo();
        grupoRepositorio.findByCodigo(codigo).ifPresent(existente -> {
            throw new IllegalArgumentException("Ya existe un grupo con codigo " + codigo);
        });
        if (grupoRepositorio.contarGruposHijos(idPadre) > 0) {
            throw new IllegalArgumentException(
                    "El grupo padre " + padre.getCodigo() + " ya tiene un PDC");
        }
        String codigoSubgrupo = codigo + SUFIJO_SUBGRUPO;
        subgrupoRepositorio.findByCodigo(codigoSubgrupo).ifPresent(existente -> {
            throw new IllegalArgumentException(
                    "El codigo de subgrupo derivado ya existe: " + codigoSubgrupo);
        });

        GrupoAdministrativo pdc = grupoRepositorio.save(new GrupoAdministrativo(
                codigo, padre.getNivel(), TipoGrupo.DIVERSIFICACION_PDC, padre));
        // Población SOLO el PDC, nunca el padre (regla S23).
        subgrupoRepositorio.save(new Subgrupo(codigoSubgrupo, Set.of(pdc)));
        return aDTO(pdc);
    }

    /**
     * El PDC de un padre. {@link NoSuchElementException} (→ 404) si el padre no existe
     * O si no tiene PDC (nunca 200 con null).
     */
    @Transactional(readOnly = true)
    public GrupoDTO obtener(Long idPadre) {
        if (!grupoRepositorio.existsById(idPadre)) {
            throw new NoSuchElementException("No existe grupo con id " + idPadre);
        }
        return grupoRepositorio.findByGrupoPadre_Id(idPadre)
                .map(PdcService::aDTO)
                .orElseThrow(() -> new NoSuchElementException(
                        "El grupo con id " + idPadre + " no tiene PDC"));
    }

    /**
     * Borra el PDC de un padre y su subgrupo mono-Di. {@link NoSuchElementException}
     * (→ 404) si el padre no existe o no tiene PDC; {@link ReferenciaEntranteException}
     * (→ 409, sin borrar nada) si el subgrupo mono-Di está en alguna plaza.
     */
    @Transactional
    public void borrar(Long idPadre) {
        if (!grupoRepositorio.existsById(idPadre)) {
            throw new NoSuchElementException("No existe grupo con id " + idPadre);
        }
        GrupoAdministrativo pdc = grupoRepositorio.findByGrupoPadre_Id(idPadre)
                .orElseThrow(() -> new NoSuchElementException(
                        "El grupo con id " + idPadre + " no tiene PDC"));
        Subgrupo subgrupo = subgrupoRepositorio.findByCodigo(pdc.getCodigo() + SUFIJO_SUBGRUPO)
                .orElseThrow(() -> new NoSuchElementException(
                        "El PDC " + pdc.getCodigo() + " no tiene subgrupo mono-Di"));

        List<Referencia> entrantes = List.of(
                new Referencia("plaza(s)", subgrupoRepositorio.contarPlazas(subgrupo.getId())));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }

        // Orden: primero el subgrupo (limpia su join subgrupo_grupo), flush para que las filas
        // desaparezcan ANTES de borrar el grupo con foreign_keys=ON (lección S73), luego el PDC.
        subgrupoRepositorio.delete(subgrupo);
        subgrupoRepositorio.flush();
        grupoRepositorio.delete(pdc);
    }

    private static GrupoDTO aDTO(GrupoAdministrativo grupo) {
        return new GrupoDTO(
                grupo.getId(), grupo.getCodigo(),
                grupo.getNivel().getCodigo(), grupo.getTipo().name());
    }
}
