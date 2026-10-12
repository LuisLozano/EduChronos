package es.yaroki.educhronos.app.service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * El reparto de las guardias ordinarias no llega al mínimo de profesores de guardia en algún tramo
 * (S213, C-reparto-guardias). La lanza la generación DESPUÉS del solve y ANTES de guardar nada, así
 * que no queda ningún horario: el controlador la traduce a un 422 {@code GUARDIAS_SIN_REPARTO}.
 *
 * <p>El mensaje es el texto para el usuario (aprobado en S213) y nombra cada tramo con
 * {@link PrevalidacionService#nombreDeTramo}. {@link #getDeficits()} lleva la misma lista
 * estructurada, para los tests y para la línea del log.
 *
 * <p>Desde S220 la lanza también el reparto de las guardias de recreo, por {@link #deRecreo}: un recreo
 * no tiene {@code Tramo} de dominio, así que quien la lanza da el mensaje y el resumen del log ya
 * escritos, y {@link #getDeficits()} va vacía. La causa del 422 es la misma.
 */
public class GuardiasSinRepartoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<RepartoGuardias.Deficit> deficits;

    private final String resumen;

    GuardiasSinRepartoException(List<RepartoGuardias.Deficit> deficits) {
        super(construirMensaje(deficits));
        this.deficits = List.copyOf(deficits);
        this.resumen = deficits.stream()
                .map(d -> d.tramo().codigo() + ":" + d.alcanzadas() + "/" + d.minimo())
                .collect(Collectors.joining(","));
    }

    private GuardiasSinRepartoException(String mensaje, String resumen) {
        super(mensaje);
        this.deficits = List.of();
        this.resumen = resumen;
    }

    /** El reparto de las guardias de recreo no sale (S220): mensaje para el usuario y resumen del log. */
    static GuardiasSinRepartoException deRecreo(String mensaje, String resumen) {
        return new GuardiasSinRepartoException(mensaje, resumen);
    }

    /** Los tramos que no llegan al mínimo, en orden de posición, con sus guardias y el mínimo; vacía en el recreo. */
    public List<RepartoGuardias.Deficit> getDeficits() {
        return deficits;
    }

    /** {@code L2:1/2,L4:0/2}: los déficits en una sola palabra, para la línea {@code clave=valor} del log. */
    String resumen() {
        return resumen;
    }

    private static String construirMensaje(List<RepartoGuardias.Deficit> deficits) {
        if (deficits.isEmpty()) {
            throw new IllegalArgumentException("GuardiasSinRepartoException exige al menos un tramo con déficit");
        }
        int minimo = deficits.get(0).minimo();
        String lista = ViaRepartoGuardias.listaDeTramos(deficits);
        return "No se pueden repartir las guardias ordinarias: en estas horas no se llega al mínimo de "
                + minimo + " profesores de guardia: " + lista + ". Los profesores libres en esas horas no"
                + " tienen guardias suficientes. Baja el mínimo, da más guardias a esos profesores o deja"
                + " libres a más profesores en esas horas.";
    }
}
