package es.yaroki.educhronos.solver.domain;

import java.util.Objects;

/**
 * Franja horaria de un día concreto.
 * diaSemana: 1=lunes … 5=viernes
 * ordenEnDia: 1..6 (tramos lectivos; el recreo no es un Tramo)
 */
public record Tramo(String codigo, int diaSemana, int ordenEnDia) {

    public Tramo {
        Objects.requireNonNull(codigo, "codigo no puede ser null");
        if (diaSemana < 1 || diaSemana > 5)
            throw new IllegalArgumentException(
                    "diaSemana debe estar entre 1 y 5, recibido: " + diaSemana);
        if (ordenEnDia < 1 || ordenEnDia > 6)
            throw new IllegalArgumentException(
                    "ordenEnDia debe estar entre 1 y 6, recibido: " + ordenEnDia);
    }
}