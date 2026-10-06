package es.yaroki.educhronos.app.service;

/**
 * Regla del total declarado de profesores, grupos y PDC (S203, C-totales-y-cargo, D9), una sola
 * vez para los tres servicios: null es «sin total», 0 vale y un negativo es un 400 que nombra el
 * campo. Vive en el servicio y no en un {@code check} de la base, como el resto de la validación.
 */
final class TotalDeclarado {

    private TotalDeclarado() {
    }

    /** Devuelve el total tal cual si es null o ≥ 0; {@link IllegalArgumentException} (→ 400) si es negativo. */
    static Integer validar(Integer total) {
        if (total != null && total < 0) {
            throw new IllegalArgumentException("totalDeclarado no puede ser negativo: " + total);
        }
        return total;
    }
}
