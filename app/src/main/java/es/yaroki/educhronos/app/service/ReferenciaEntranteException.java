package es.yaroki.educhronos.app.service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Se intenta borrar una entidad de catálogo que OTRAS filas todavía referencian
 * (Bloque 8.5-C2b). Es la tercera familia de excepción del CRUD, junto a
 * {@link java.util.NoSuchElementException} (→404) y {@link IllegalArgumentException}
 * (→400): los controladores la traducen a 409 CONFLICT.
 *
 * <p><b>Por qué existe.</b> Desde 8.5-C2a-DDL las FK del esquema son reales y el pragma
 * {@code foreign_keys=ON} las hace morder (S73), pero un borrado que las viole aborta con
 * una {@code SQLException} opaca que Spring convierte en 500. Esta excepción es la PRIMERA
 * línea de defensa: cada {@code Service.borrar} consulta el mapa inverso de FK ANTES de
 * llamar a {@code delete}, y falla con un mensaje que dice QUIÉN lo impide y CUÁNTOS son.
 * El mordisco de la FK queda como red de seguridad de última instancia, no como mecanismo.
 *
 * <p><b>Desglose estructurado.</b> Además del mensaje, expone la lista de
 * {@link Referencia} que lo motivaron ({@link #getReferencias()}), para que los tests
 * aseveren estructura (referente + conteo real) en vez de hacer substring del texto.
 */
public class ReferenciaEntranteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Un referente que impide el borrado, con su conteo REAL de filas apuntando. */
    public record Referencia(String referente, long conteo) { }

    private final transient List<Referencia> referencias;

    /**
     * Construye la excepción a partir del mapa inverso ya consultado. La lista se FILTRA a
     * los conteos > 0 (los referentes con cero filas no son un impedimento y no se nombran),
     * y el orden de llegada se conserva: es el del mapa de FK de {@code schema.sql}, que es
     * el que el servicio recorre.
     *
     * @throws IllegalArgumentException si tras filtrar no queda ningún referente: lanzar
     *     "no se puede borrar" sin un solo motivo sería un bug del servicio llamante, no un
     *     conflicto real, y debe reventar aquí y no llegar al usuario como un 409 vacío.
     */
    public ReferenciaEntranteException(List<Referencia> referencias) {
        super(construirMensaje(referencias));
        this.referencias = referencias.stream().filter(r -> r.conteo() > 0).toList();
    }

    /** Los referentes que impiden el borrado (conteo > 0), en el orden del mapa de FK. */
    public List<Referencia> getReferencias() {
        return referencias;
    }

    /** {@code "No se puede borrar: referenciada por 2 plaza(s), 1 sesion(es)"}. */
    private static String construirMensaje(List<Referencia> referencias) {
        List<Referencia> vivas = referencias.stream().filter(r -> r.conteo() > 0).toList();
        if (vivas.isEmpty()) {
            throw new IllegalArgumentException(
                    "ReferenciaEntranteException exige al menos un referente con conteo > 0");
        }
        return vivas.stream()
                .map(r -> r.conteo() + " " + r.referente())
                .collect(Collectors.joining(", ", "No se puede borrar: referenciada por ", ""));
    }
}
