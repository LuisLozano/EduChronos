package es.yaroki.educhronos.app.service;

/**
 * El horario pedido no existe (S226, condición 6 de O-interfaz). La lanzan
 * {@link GeneradorHorarioService#cargarHorario} y {@link GeneradorHorarioService#proyectar}
 * con el mismo mensaje de siempre.
 *
 * <p>Extiende {@link IllegalArgumentException} a propósito: los llamadores que ya capturan
 * esa familia —el movimiento, la proyección, el CSV y el PDF— siguen igual. El que necesita
 * distinguirla es el diagnóstico, donde una {@code IllegalArgumentException} cualquiera
 * puede ser también una incoherencia de los datos del curso, que no es un 404.
 */
public class HorarioInexistenteException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public HorarioInexistenteException(String mensaje) {
        super(mensaje);
    }
}
