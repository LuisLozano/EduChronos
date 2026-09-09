package es.yaroki.educhronos.app.web.dto;

/**
 * Referencia a una INSTANCIA de actividad por su clave de negocio (S144).
 *
 * <p>Es el par ({@code actividadCodigo}, {@code indice}) que ya usan {@code CeldaRef},
 * {@code SesionVistaDTO}, {@code MoverInstanciaRequest} y el alta de bloqueos; nunca un
 * {@code sesionId}, porque una instancia son de 1 a 6 filas de {@code sesion} y se
 * mueven todas o ninguna.
 *
 * <p>NO lleva destino: en el intercambio el destino de cada una es el tramo que ocupa la
 * otra, así que no hay par (dia, orden) en el cuerpo.
 */
public record ReferenciaInstancia(String actividadCodigo, int indice) {
}
