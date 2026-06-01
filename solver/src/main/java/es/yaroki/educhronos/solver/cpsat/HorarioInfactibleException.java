package es.yaroki.educhronos.solver.cpsat;

/**
 * El solver no encontró un horario factible, o el modelo CP-SAT es inválido.
 *
 * <p>Es distinta de la excepción de carga del Bloque 3
 * ({@code ProblemaInvalidoException}): aquella indica un problema de entrada
 * malformado; ésta indica que un problema bien formado no tiene solución que
 * respete todas las restricciones duras.
 */
public class HorarioInfactibleException extends RuntimeException {

    public HorarioInfactibleException(String mensaje) {
        super(mensaje);
    }
}
