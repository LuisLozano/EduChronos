package es.yaroki.educhronos.app.web.dto;

/**
 * Una violación de las guardias de un horario (S213, C-reparto-guardias, condición 3 de
 * O-guardias): la {@code regla}, el profesor y el tramo cuando la regla los tiene (pueden ser
 * {@code null}) y el {@code mensaje} para el usuario. Va en su propia lista de
 * {@link DiagnosticoDTO}, aparte de las violaciones por celda: una guardia no es una celda del
 * horario. El {@code tramoCodigo} es el del dominio («L1») en un tramo lectivo y
 * {@code DIA HH:mm-HH:mm} en uno que no lo es.
 */
public record ViolacionGuardiaDTO(String regla, String profesorCodigo, String tramoCodigo, String mensaje) {
}
