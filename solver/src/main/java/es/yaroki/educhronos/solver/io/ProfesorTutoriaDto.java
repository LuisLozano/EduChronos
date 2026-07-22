// ProfesorTutoriaDto.java
package es.yaroki.educhronos.solver.io;

/**
 * DTO de una tutoría: referencias por código de negocio a profesor y grupo, más el rol
 * como texto. El mapper resuelve los códigos contra los catálogos de la pasada 1 y
 * traduce el rol a {@code domain.RolTutoria}. Orden de componentes alineado con el
 * record de dominio {@code ProfesorTutoria} (profesor, grupo, rol).
 */
public record ProfesorTutoriaDto(
        String profesor,
        String grupo,
        String rol) { }
