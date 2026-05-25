// TramoDto.java
package es.yaroki.educhronos.solver.io;

public record TramoDto(
        String codigo,
        Integer diaSemana,        // 1..5
        Integer ordenEnDia) { }   // 1..6
