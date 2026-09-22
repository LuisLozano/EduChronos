package es.yaroki.educhronos.app.curso;

import org.springframework.http.HttpStatus;

/**
 * Una operación de curso rechazada por una razón PREVISTA: el nombre no vale, el curso ya
 * existe, el curso está archivado (O-curso, S159).
 *
 * <p>Lleva el status y una {@code causa} —símbolo estable del contrato, no texto para el
 * usuario— porque el controlador las traduce a un cuerpo propio, por el motivo que explica
 * {@link es.yaroki.educhronos.app.web.dto.RechazoCursoDTO}. Lo que NO es previsto (un fallo
 * de E/S, un SQL roto) no pasa por aquí: sube tal cual y acaba en 500.
 */
public class RechazoCursoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient HttpStatus status;

    private final String causa;

    public RechazoCursoException(HttpStatus status, String causa, String mensaje) {
        super(mensaje);
        this.status = status;
        this.causa = causa;
    }

    public HttpStatus status() {
        return status;
    }

    public String causa() {
        return causa;
    }
}
