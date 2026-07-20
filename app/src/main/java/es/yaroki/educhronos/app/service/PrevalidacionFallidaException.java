package es.yaroki.educhronos.app.service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * La pre-validación de condiciones necesarias encontró al menos un
 * {@link Severidad#ERROR} y la generación no debe llegar al solver (Fase 8,
 * Bloque 8.4-A, deuda D18). Los controladores la traducen a 422 UNPROCESSABLE_ENTITY,
 * el MISMO status que {@link es.yaroki.educhronos.solver.cpsat.HorarioInfactibleException}
 * ({@code HorarioController:59}): es el mismo hecho —problema bien formado sin
 * solución posible— detectado ANTES de gastar el presupuesto del solver, y con el
 * recurso culpable nombrado en vez de un {@code CpSolverStatus} opaco.
 *
 * <p><b>Desglose estructurado.</b> Igual que {@link ReferenciaEntranteException},
 * además del mensaje expone la lista de {@link AvisoPrevalidacion} que la motivaron
 * ({@link #getAvisos()}), para que los tests aseveren estructura (regla + entidad +
 * los dos números) en vez de hacer substring del texto.
 *
 * <p><b>Qué lleva dentro.</b> La lista COMPLETA que produjo la pre-validación, no solo
 * los errores: los {@link Severidad#AVISO} acompañan al 422 porque son contexto útil
 * para diagnosticar el catálogo. La guarda del constructor, en cambio, solo mira los
 * ERROR: una excepción de aborto sin un solo motivo de aborto sería un bug del
 * llamante. Difiere aquí de {@code ReferenciaEntranteException}, que sí filtra su
 * lista, porque allí los elementos descartados (conteo 0) no aportaban información.
 */
public class PrevalidacionFallidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<AvisoPrevalidacion> avisos;

    /**
     * @param avisos la salida completa de {@code PrevalidacionService.prevalidar()}
     *     (errores y avisos), en el orden en que se computó.
     * @throws IllegalArgumentException si la lista está vacía o no contiene ningún
     *     {@link Severidad#ERROR}: abortar la generación sin un motivo que lo justifique
     *     sería un bug del servicio llamante, no un problema del catálogo, y debe
     *     reventar aquí en vez de llegar al usuario como un 422 injustificado.
     */
    public PrevalidacionFallidaException(List<AvisoPrevalidacion> avisos) {
        super(construirMensaje(avisos));
        this.avisos = List.copyOf(avisos);
    }

    /** La salida completa de la pre-validación (ERROR y AVISO), en orden de cómputo. */
    public List<AvisoPrevalidacion> getAvisos() {
        return avisos;
    }

    /** Solo los hallazgos que justifican el aborto. Nunca vacía (lo garantiza el ctor). */
    public List<AvisoPrevalidacion> getErrores() {
        return avisos.stream().filter(a -> a.severidad() == Severidad.ERROR).toList();
    }

    /**
     * {@code "Pre-validacion fallida: MAT1 necesita 31 tramos y dispone de 30, ..."}.
     * Solo nombra los ERROR: son los que abortan.
     */
    private static String construirMensaje(List<AvisoPrevalidacion> avisos) {
        List<AvisoPrevalidacion> errores = avisos.stream()
                .filter(a -> a.severidad() == Severidad.ERROR).toList();
        if (errores.isEmpty()) {
            throw new IllegalArgumentException(
                    "PrevalidacionFallidaException exige al menos un aviso de severidad ERROR");
        }
        return errores.stream()
                .map(AvisoPrevalidacion::descripcion)
                .collect(Collectors.joining("; ", "Pre-validacion fallida: ", ""));
    }
}
