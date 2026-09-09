package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.solver.cpsat.Violacion;
import java.util.List;
import java.util.Objects;

/**
 * El movimiento de una instancia no se puede hacer (S143). Lleva la
 * {@link CausaMovimiento} —símbolo estable, no prosa— y, solo para
 * {@link CausaMovimiento#VIOLA_REGLA_DURA}, las violaciones NUEVAS que provoca.
 *
 * <p>Transporta {@code solver.cpsat.Violacion} de dominio, no los DTO de web: el
 * servicio no sabe de la capa REST. El controlador es quien aplana a
 * {@code ViolacionDTO}.
 */
public class MovimientoRechazadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient CausaMovimiento causa;
    private final transient List<Violacion> violaciones;

    public MovimientoRechazadoException(CausaMovimiento causa, String mensaje) {
        this(causa, mensaje, List.of());
    }

    public MovimientoRechazadoException(
            CausaMovimiento causa, String mensaje, List<Violacion> violaciones) {
        super(mensaje);
        this.causa = Objects.requireNonNull(causa, "causa no puede ser null");
        this.violaciones = List.copyOf(
                Objects.requireNonNull(violaciones, "violaciones no puede ser null (usa List.of())"));
    }

    public CausaMovimiento causa() {
        return causa;
    }

    /** Violaciones que APARECEN por el movimiento; vacía salvo en VIOLA_REGLA_DURA. */
    public List<Violacion> violaciones() {
        return violaciones;
    }
}
