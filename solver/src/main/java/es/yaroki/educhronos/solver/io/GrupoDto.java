// GrupoDto.java
package es.yaroki.educhronos.solver.io;

public record GrupoDto(
        String codigo,
        String tipo,              // ORDINARIO | DIVERSIFICACION_PDC
        String grupoPadre) { }    // nullable; obligatorio si tipo == DIVERSIFICACION_PDC (I5)