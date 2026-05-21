package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

public record Asignatura(String codigo, String nombre) {

    public Asignatura {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        Objects.requireNonNull(nombre,  "nombre no puede ser null");
    }
}