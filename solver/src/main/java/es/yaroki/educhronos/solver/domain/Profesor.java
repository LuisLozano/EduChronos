package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

public record Profesor(String codigo, String nombre) {

    public Profesor {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        Objects.requireNonNull(nombre,  "nombre no puede ser null");
    }
}