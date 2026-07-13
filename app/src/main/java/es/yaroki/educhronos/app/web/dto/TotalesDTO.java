package es.yaroki.educhronos.app.web.dto;

/**
 * Totales blandos de un horario (Fase 8, Bloque 8.3-C), obtenidos de los gemelos
 * independientes del verificador: {@code contarVentanasProfesor} (suma de los valores
 * del mapa), {@code contarPenalizacionConsecutivasProfesor} y
 * {@code contarPenalizacionIndisponibilidadBlanda}. Son conteos SIN signo del coste
 * actual; NO tienen por qué coincidir con la suma de los {@code delta} contrafactuales.
 */
public record TotalesDTO(int ventanas, int consecutivas, int indispBlanda) {
}
