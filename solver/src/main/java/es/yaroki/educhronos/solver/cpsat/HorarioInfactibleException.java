package es.yaroki.educhronos.solver.cpsat;

/**
 * El solver no encontró un horario factible, o el modelo CP-SAT es inválido.
 *
 * <p>Es distinta de la excepción de carga del Bloque 3
 * ({@code ProblemaInvalidoException}): aquella indica un problema de entrada
 * malformado; ésta indica que un problema bien formado no tiene solución que
 * respete todas las restricciones duras.
 *
 * <p><b>El veredicto viaja aparte del mensaje (S118).</b> El estado CP-SAT ya se
 * interpolaba en el texto, pero como prosa: el borde HTTP no podía leerlo sin
 * parsear una frase, y por eso los tres desenlaces —catálogo imposible,
 * presupuesto agotado, modelo inválido— salían como el mismo 422. {@link #estado()}
 * los separa sin tocar ni una palabra de los mensajes, que siguen siendo los de
 * antes porque hay tests de endpoint que los asertan.
 *
 * <p><b>Ambos accesores pueden ser null, y eso significa algo.</b> Null NO es
 * "desconocido": es <i>no hubo solve</i>. Se lanza así cuando el fallo ocurre
 * construyendo el modelo, antes de que exista ningún {@code CpSolver} del que
 * obtener un estado (ver {@code ModeloCpSat}, catálogo sin tramos). El borde HTTP
 * debe tratarlo como configuración incompleta, no como un fallo interno.
 */
public class HorarioInfactibleException extends RuntimeException {

    private final String estado;
    private final Integer segundos;

    /**
     * Fallo SIN solve: se abortó construyendo el modelo. Deja {@link #estado()} y
     * {@link #segundos()} en null porque no hay ninguno que dar, no porque se hayan
     * perdido.
     */
    public HorarioInfactibleException(String mensaje) {
        this(mensaje, null, null);
    }

    /**
     * Fallo CON solve.
     *
     * @param mensaje  texto para el log y para {@code status().reason()}; no cambia
     *                 respecto a las versiones previas.
     * @param estado   nombre del {@code CpSolverStatus} con que terminó el solve.
     * @param segundos presupuesto con que se corrió, en segundos. Es lo que hace
     *                 accionable un {@code UNKNOWN}: sin él, "se agotó el tiempo" no
     *                 dice cuánto tiempo era, y quien lo lee no sabe si pedir más.
     */
    public HorarioInfactibleException(String mensaje, String estado, Integer segundos) {
        super(mensaje);
        this.estado = estado;
        this.segundos = segundos;
    }

    /** Estado CP-SAT del solve, o null si no llegó a haber solve. */
    public String estado() {
        return estado;
    }

    /** Presupuesto de la corrida en segundos, o null si no llegó a haber solve. */
    public Integer segundos() {
        return segundos;
    }
}
