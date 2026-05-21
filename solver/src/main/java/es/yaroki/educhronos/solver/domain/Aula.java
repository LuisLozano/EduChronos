package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

public record Aula(String codigo, String nombre) {

    public Aula {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        Objects.requireNonNull(nombre,  "nombre no puede ser null");
    }
}