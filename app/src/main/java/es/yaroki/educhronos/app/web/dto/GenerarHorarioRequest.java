package es.yaroki.educhronos.app.web.dto;

import es.yaroki.educhronos.app.service.ViaSolver;

/**
 * Cuerpo del {@code POST /api/horarios}: parámetros de una generación de horario
 * (parametrización del solve, deuda D29). Solo datos, sin lógica: todos los campos
 * son OPCIONALES (tipos envoltorio, admiten {@code null}).
 *
 * <p>El defaulting y la validación NO viven aquí (los DTO de esta capa no llevan
 * lógica): los aplica {@code GeneradorHorarioService.generar} —{@code nombre} a
 * {@code "Horario " + Instant.now()}, {@code via} a {@link ViaSolver#OPTIMIZACION},
 * {@code maxSegundos}/{@code semilla} a los defectos del solver— y valida que
 * {@code maxSegundos} sea positivo. Un {@code via} fuera del enum lo rechaza el
 * propio deserializador (Jackson) con 400 antes de llegar al servicio.
 *
 * <p>Un cuerpo ausente o vacío ({@code {}}) equivale a todos los campos nulos:
 * genera con los valores por defecto.
 */
public record GenerarHorarioRequest(
        String nombre,
        Integer maxSegundos,
        Integer semilla,
        ViaSolver via) {
}
