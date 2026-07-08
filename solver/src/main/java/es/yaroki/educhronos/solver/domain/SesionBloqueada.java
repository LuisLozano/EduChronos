package es.yaroki.educhronos.solver.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Bloqueo manual de una {@link ActividadInstancia} antes de resolver (Fase 8).
 * Fija el {@link Tramo} de la instancia como restricción DURA (Bloque 8.2a) y,
 * opcionalmente, el AULA de plazas concretas de aula variable (Bloque 8.2b).
 * Cierra el mecanismo del criterio 5 de Fase 3.
 *
 * <p>El pin de TRAMO es a nivel de INSTANCIA: todas las plazas de una instancia
 * comparten su tramo, así que un desdoble pinado cae simultáneo en el tramo
 * indicado sin ambigüedad.
 *
 * <p>El pin de AULA es POR PLAZA: {@code aulasPinadas} asocia una plaza de aula
 * variable de la instancia con una de sus {@code aulasCandidatas}. Una instancia
 * de desdoble tiene varias plazas, cada una con su aula, y por eso el pin se hace
 * por el par (plaza, aula). Las plazas ausentes del mapa quedan libres: un pin
 * puede fijar unas plazas y dejar otras al solver. {@link Map#of()} = pin de solo
 * tramo (comportamiento retrocompatible de 8.2a). La validación de que la plaza
 * es de aula variable y de que el aula está entre sus candidatas vive en la capa
 * {@code io} (entrada de usuario); {@code cpsat} la reprueba como salvaguarda.
 */
public record SesionBloqueada(ActividadInstancia instancia, Tramo tramo,
                              Map<Plaza, Aula> aulasPinadas) {

    public SesionBloqueada {
        Objects.requireNonNull(instancia, "instancia no puede ser null");
        Objects.requireNonNull(tramo, "tramo no puede ser null");
        Objects.requireNonNull(aulasPinadas, "aulasPinadas no puede ser null (usa Map.of())");
        aulasPinadas = Map.copyOf(aulasPinadas);
    }
}
