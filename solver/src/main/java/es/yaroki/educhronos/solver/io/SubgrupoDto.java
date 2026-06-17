// SubgrupoDto.java
package es.yaroki.educhronos.solver.io;

import java.util.List;

public record SubgrupoDto(
        String codigo,
        List<String> grupos) { }         // codigo de un unico GrupoAdministrativo