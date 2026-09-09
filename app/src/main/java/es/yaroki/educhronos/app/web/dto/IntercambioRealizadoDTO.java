package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code 200} de un intercambio (S144): las filas de LAS DOS instancias, ya
 * permutadas y RELEÍDAS del repositorio.
 *
 * <p><b>Dos listas y no una concatenada</b>: quien llama pidió por dos referencias y
 * tiene que poder mirar cada lado por separado sin reagrupar por {@code actividadCodigo}.
 * Cada lista tiene la misma forma que las {@code sesiones} de
 * {@code GET /{id}/proyeccion} para esa instancia (D-proyeccion-instancia-espejo, que
 * con este endpoint pasa a usarse DOS veces por respuesta).
 *
 * @param primera filas de la instancia {@code primera} de la petición, ya en el tramo
 *                que ocupaba {@code segunda}.
 * @param segunda ídem, en el tramo que ocupaba {@code primera}.
 */
public record IntercambioRealizadoDTO(
        List<SesionVistaDTO> primera, List<SesionVistaDTO> segunda) {
}
