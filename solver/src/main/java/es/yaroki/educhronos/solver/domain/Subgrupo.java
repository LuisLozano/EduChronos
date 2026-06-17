package es.yaroki.educhronos.solver.domain;

import java.util.Set;

public record Subgrupo(String codigo, Set<GrupoAdministrativo> grupos) {

    public Subgrupo {
        if (codigo == null) {
            throw new NullPointerException("codigo no puede ser null");
        }
        if (grupos == null || grupos.isEmpty()) {
            throw new IllegalArgumentException(
                    "subgrupo '" + codigo + "': debe pertenecer al menos a un grupo");
        }
        grupos = Set.copyOf(grupos);   // inmutable + rechaza nulls internos
    }

    /** Igualdad por identidad de codigo: el codigo de subgrupo es unico (lo
     *  garantiza el mapper). Sobreescrito para que la igualdad no dependa del
     *  conjunto de grupos y el modelo sea robusto a cambios de componentes. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subgrupo other)) return false;
        return codigo.equals(other.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}