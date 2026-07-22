package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

/**
 * Tutoría de un profesor sobre un grupo (§4.1, invariante I4).
 *
 * <p>Componente de dominio del par (profesor, grupo) con su {@link RolTutoria}. El orden
 * de los componentes replica el del constructor de la entidad JPA {@code app.catalog
 * .ProfesorTutoria} —(profesor, grupo, rol)— para que el mapper reconcilie ambos mundos
 * sin invertir argumentos.
 */
public record ProfesorTutoria(
        Profesor profesor,
        GrupoAdministrativo grupo,
        RolTutoria rol) {

    public ProfesorTutoria {
        Objects.requireNonNull(profesor, "profesor no puede ser null");
        Objects.requireNonNull(grupo,    "grupo no puede ser null");
        Objects.requireNonNull(rol,      "rol no puede ser null");
    }
}
