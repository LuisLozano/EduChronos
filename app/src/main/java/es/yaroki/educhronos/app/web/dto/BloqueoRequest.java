package es.yaroki.educhronos.app.web.dto;

import java.util.List;

/**
 * Cuerpo del {@code POST /api/bloqueos}: un bloqueo manual completo (§4.7, Bloque
 * 8.2b-iv) = UN pin de tramo ({@code sesion_bloqueada}) sobre la instancia
 * ({@code actividadCodigo}, {@code indice}) MÁS sus N pines de aula
 * ({@code aula_bloqueada}). No existe endpoint para pines de aula sueltos: el
 * dominio no admite ese estado (D-1).
 *
 * <p><b>Semántica de REEMPLAZO (D-5, "PUT semántico").</b> El body describe el
 * estado COMPLETO del pin: {@code aulas} ausente ({@code null}) o vacío se tratan
 * IGUAL (el pin queda sin pines de aula). No hay merge parcial.
 *
 * <p>Solo datos, sin lógica (patrón de los DTO de 7A): el defaulting y la
 * validación viven en {@code BloqueoService}.
 */
public record BloqueoRequest(
        String actividadCodigo,
        int indice,
        TramoRefDTO tramo,
        List<AulaPinDTO> aulas) {
}
