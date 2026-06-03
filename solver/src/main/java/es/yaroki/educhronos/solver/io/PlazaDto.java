// PlazaDto.java
package es.yaroki.educhronos.solver.io;

import java.util.List;

public record PlazaDto(
        String codigo,
        String asignatura,
        List<String> profesores,          // array no vacio (I7)
        String aulaFija,                  // nullable
        List<String> aulasCandidatas,     // nullable u []; el solver elige aula entre estas (Fase 3)
        List<String> subgrupos) { }