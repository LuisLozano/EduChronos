package es.yaroki.educhronos.solver.cli;

/**
 * Códigos de salida del programa CLI.
 *
 *   0 — resolución correcta, 0 violaciones duras
 *   1 — problema infactible (HorarioInfactibleException)
 *   2 — error de invocación o JSON inválido (ProblemaInvalidoException)
 *   3 — solución obtenida pero el verificador encontró violaciones
 *       (no debería ocurrir; síntoma de bug en el modelo CP-SAT)
 */
enum CodigoSalida {

    OK(0),
    INFACTIBLE(1),
    ENTRADA_INVALIDA(2),
    VIOLACIONES_DURAS(3);

    private final int valor;

    CodigoSalida(int valor) {
        this.valor = valor;
    }

    int valor() {
        return valor;
    }
}
