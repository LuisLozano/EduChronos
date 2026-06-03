package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntervalVar;
import es.yaroki.educhronos.solver.domain.Aula;
import java.util.Objects;

/**
 * Una opción de aula para una plaza con {@code aulasCandidatas}: el aula
 * concreta, su literal de presencia y el intervalo opcional que ese literal
 * gobierna.
 *
 * <p>El solver elige exactamente una opción por plaza ({@code addExactlyOne}
 * sobre los {@code presencia} de la plaza). El {@code intervalo} es opcional
 * ({@code newOptionalFixedSizeIntervalVar}): solo participa en el
 * {@code addNoOverlap} del aula cuando {@code presencia} es verdadero. Su
 * {@code start} es el {@code tramoIndex} compartido de la instancia, no una
 * variable propia: el aula es lo único que varía por opción, el tramo ya está
 * fijado por la instancia.
 *
 * <p>Paquete-privado y en {@code cpsat}: porta tipos OR-Tools, que no pueden
 * filtrarse al paquete {@code domain} (decisión permanente).
 */
record AulaOpcion(Aula aula, BoolVar presencia, IntervalVar intervalo) {

    AulaOpcion {
        Objects.requireNonNull(aula,      "aula no puede ser null");
        Objects.requireNonNull(presencia, "presencia no puede ser null");
        Objects.requireNonNull(intervalo, "intervalo no puede ser null");
    }
}
