package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

public record Subgrupo(String codigo, GrupoAdministrativo grupo) {

    public Subgrupo {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        Objects.requireNonNull(grupo,  "grupo no puede ser null");
    }
}