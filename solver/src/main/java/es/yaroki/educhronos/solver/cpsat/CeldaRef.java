package es.yaroki.educhronos.solver.cpsat;

import java.util.Objects;

/**
 * Referencia a una celda del horario implicada en una {@link Violacion}, por
 * claves de NEGOCIO (Fase 8, Bloque 8.3-A). NADA de JPA ni {@code sesionId}: la
 * frontera dura solver ↔ persistencia se mantiene (D-4). El puente a
 * {@code sesionId} lo hará {@code app/} usando esta misma clave, que coincide con
 * la que ya expone {@code SesionVistaDTO} (indice + actividadCodigo + plazaCodigo).
 *
 * <p>{@code actividadCodigo} + {@code indice} identifican la
 * {@code ActividadInstancia} culpable. {@code indice} es el índice <b>1-based</b>
 * del dominio ({@code ActividadInstancia.indice()} es 1..repeticionesPorSemana),
 * transportado <b>tal cual, sin reindexar</b>: cualquier traducción aquí sería un
 * bug silencioso frente al puente de {@code app/} y a {@code SesionVistaDTO.indice},
 * que vienen del mismo origen.
 *
 * <p>{@code plazaCodigo} es NULLABLE. No-null SOLO en {@link ReglaDura#SOLAPE_AULA}
 * (D15: el aula se cuenta por plaza, no por instancia, porque dos plazas de la
 * misma instancia con la misma aula son colisión). Null en todas las demás reglas,
 * que se atribuyen a nivel de instancia.
 */
public record CeldaRef(String actividadCodigo, int indice, String plazaCodigo) {

    public CeldaRef {
        Objects.requireNonNull(actividadCodigo, "actividadCodigo no puede ser null");
        if (indice < 1) {
            throw new IllegalArgumentException(
                    "indice debe ser >= 1 (1-based del dominio): " + indice);
        }
        // plazaCodigo intencionadamente nullable (no-null solo en SOLAPE_AULA).
    }
}
