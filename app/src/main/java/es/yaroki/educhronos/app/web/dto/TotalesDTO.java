package es.yaroki.educhronos.app.web.dto;

/**
 * Totales blandos de un horario (Fase 8, Bloque 8.3-C), obtenidos de los gemelos
 * independientes del verificador: {@code contarVentanasProfesor} (suma de los valores
 * del mapa), {@code contarPenalizacionConsecutivasProfesor},
 * {@code contarPenalizacionIndisponibilidadBlanda} y, desde S208,
 * {@code contarPenalizacionAulaNoPreferida} (tramos de clases fuera de su aula preferida). Son
 * conteos SIN signo del coste actual; NO tienen por qué coincidir con la suma de los
 * {@code delta} contrafactuales. Con los pesos a 1, los cuatro suman el objetivo del solver.
 * {@code aulaNoPreferida} no tiene penalizaciones por celda: mover una sesión no cambia su aula.
 */
public record TotalesDTO(int ventanas, int consecutivas, int indispBlanda, int aulaNoPreferida) {
}
