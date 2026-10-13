package es.yaroki.educhronos.app.service;

import java.util.ArrayList;
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
 * <p><b>La acción es un parámetro</b> (S109). El borrado dejó de ser el único uso: la EDICIÓN
 * de una actividad con dependientes también se rechaza con 409 (la estructura no se edita en
 * caliente, precedente de C-jornada/S107), y decirle al usuario "no se puede borrar" cuando
 * lo que ha intentado es un PUT sería falso. El constructor de un solo argumento conserva
 * {@code "borrar"} y con él el mensaje exacto que hoy ven los DELETE y sus tests.
 *
 * <p><b>Desglose estructurado.</b> Además del mensaje, expone la lista de
 * {@link Referencia} que lo motivaron ({@link #getReferencias()}), para que los tests
 * aseveren estructura (referente + conteo real) en vez de hacer substring del texto.
 *
 * <p><b>Códigos nombrados</b> (S225, condición 4 de O-interfaz). El conteo dice cuántas filas
 * impiden el borrado, pero no cuáles, y el usuario tiene que ir a buscarlas. El tercer
 * constructor recibe además listas de {@link Nombrados} que el mensaje añade al final, cada una
 * SOLO si no está vacía: {@code ". Plazas: A-P1, B-P1 (y 3 más)"}. Los códigos se ordenan aquí y
 * se cortan en {@link #MAXIMO_NOMBRADOS}; quien llama los pasa tal cual salen de la consulta.
 */
public class ReferenciaEntranteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Tope de códigos por lista en el mensaje; del resto solo se dice cuántos son. */
    static final int MAXIMO_NOMBRADOS = 10;

    /** Lo que el usuario puede hacer ante un borrado rechazado (S226); solo con la acción "borrar". */
    static final String QUE_HACER_AL_BORRAR =
            "Para poder borrar, cambia o borra antes lo que se indica y vuelve a intentarlo.";

    /** Un referente que impide el borrado, con su conteo REAL de filas apuntando. */
    public record Referencia(String referente, long conteo) { }

    /** Códigos de las filas de un referente, con el rótulo que los presenta («Plazas»). */
    public record Nombrados(String rotulo, List<String> codigos) { }

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
        this(referencias, "borrar");
    }

    /**
     * Igual que el anterior, nombrando la ACCIÓN que se rechaza ({@code "borrar"},
     * {@code "editar"}): es el verbo que entra en el mensaje y por tanto en el reason del 409.
     */
    public ReferenciaEntranteException(List<Referencia> referencias, String accion) {
        super(cerrar(construirMensaje(referencias, accion), accion));
        this.referencias = referencias.stream().filter(r -> r.conteo() > 0).toList();
    }

    /**
     * Borrado rechazado que, además de contar, NOMBRA las filas que lo impiden: cada lista de
     * {@code nombrados} no vacía añade {@code ". <rótulo>: a, b, … (y N más)"} al mensaje de los
     * otros constructores. Las vacías no añaden nada, así que un rechazo sin esas filas (un
     * profesor que solo tiene guardias) da el mismo texto que antes.
     */
    public ReferenciaEntranteException(List<Referencia> referencias, List<Nombrados> nombrados) {
        super(cerrar(construirMensaje(referencias, "borrar") + nombrar(nombrados), "borrar"));
        this.referencias = referencias.stream().filter(r -> r.conteo() > 0).toList();
    }

    /** Los referentes que impiden el borrado (conteo > 0), en el orden del mapa de FK. */
    public List<Referencia> getReferencias() {
        return referencias;
    }

    /** {@code "No se puede borrar: referenciada por 2 plaza(s), 1 sesion(es)"}. */
    private static String construirMensaje(List<Referencia> referencias, String accion) {
        List<Referencia> vivas = referencias.stream().filter(r -> r.conteo() > 0).toList();
        if (vivas.isEmpty()) {
            throw new IllegalArgumentException(
                    "ReferenciaEntranteException exige al menos un referente con conteo > 0");
        }
        return vivas.stream()
                .map(r -> r.conteo() + " " + r.referente())
                .collect(Collectors.joining(", ", "No se puede " + accion + ": referenciada por ", ""));
    }

    /**
     * Cierra el mensaje de un BORRADO con {@link #QUE_HACER_AL_BORRAR}, como frase propia: punto y
     * espacio, o solo espacio si ya acaba en punto (S226, condición 5 de O-interfaz). Con otra
     * acción ("editar", "deshacer", "guardar") lo deja como está: la frase habla de borrar.
     */
    private static String cerrar(String mensaje, String accion) {
        if (!"borrar".equals(accion)) {
            return mensaje;
        }
        return mensaje + (mensaje.endsWith(".") ? " " : ". ") + QUE_HACER_AL_BORRAR;
    }

    /** {@code ". Plazas: A-P1, B-P1 (y 2 más)"} por cada lista no vacía, en el orden recibido. */
    private static String nombrar(List<Nombrados> nombrados) {
        StringBuilder texto = new StringBuilder();
        for (Nombrados lista : nombrados) {
            if (lista.codigos().isEmpty()) {
                continue;
            }
            List<String> ordenados = lista.codigos().stream().sorted().toList();
            List<String> visibles = new ArrayList<>();
            for (String codigo : ordenados) {
                if (visibles.size() >= MAXIMO_NOMBRADOS) {
                    break;
                }
                visibles.add(codigo);
            }
            texto.append(". ").append(lista.rotulo()).append(": ").append(String.join(", ", visibles));
            int resto = ordenados.size() - visibles.size();
            if (resto > 0) {
                texto.append(" (y ").append(resto).append(" más)");
            }
        }
        return texto.toString();
    }
}
