package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

/**
 * Bloqueo manual de una {@link ActividadInstancia} a un {@link Tramo} concreto
 * antes de resolver (Fase 8, Bloque 8.2a). Cierra el mecanismo del criterio 5 de
 * Fase 3: el usuario fija ("pina") una instancia en un tramo y el solver lo
 * respeta como restricción DURA.
 *
 * <p>El pin es a nivel de INSTANCIA, no de plaza: todas las plazas de una
 * instancia comparten su tramo, así que un desdoble pinado cae simultáneo en el
 * tramo indicado sin ambigüedad. El pin de AULA se difiere a 8.2b: requiere el par
 * (plaza, aula) —una instancia de desdoble tiene varias plazas, cada una con su
 * aula— y por eso este record NO lleva aula.
 */
public record SesionBloqueada(ActividadInstancia instancia, Tramo tramo) {

    public SesionBloqueada {
        Objects.requireNonNull(instancia, "instancia no puede ser null");
        Objects.requireNonNull(tramo, "tramo no puede ser null");
    }
}
