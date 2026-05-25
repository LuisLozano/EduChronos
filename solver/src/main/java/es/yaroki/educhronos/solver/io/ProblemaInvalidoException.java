package es.yaroki.educhronos.solver.io;

/** Error de carga: el JSON es sintacticamente valido pero el contenido no es un ProblemaHorario valido. */
public class ProblemaInvalidoException extends RuntimeException {
    public ProblemaInvalidoException(String mensaje) { super(mensaje); }
    public ProblemaInvalidoException(String mensaje, Throwable causa) { super(mensaje, causa); }
}