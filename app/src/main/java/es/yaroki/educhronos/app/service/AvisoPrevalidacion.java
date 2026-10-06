package es.yaroki.educhronos.app.service;

import java.util.Objects;

/**
 * Un hallazgo de la pre-validación del catálogo (Fase 8, Bloque 8.4-A, deuda D18): una
 * comparación entre {@code demanda} y {@code disponible} que falló, con la entidad concreta
 * que la provoca. Las reglas de CAPACIDAD se emiten si {@code demanda > disponible} (la
 * igualdad NO es un fallo); las de CUADRE (S203), si {@code demanda ≠ disponible}, por
 * exceso o por defecto. S8 y el pin sobre DURA codifican 1 contra 0.
 *
 * <p>Es el equivalente estructurado de
 * {@link ReferenciaEntranteException.Referencia}: existe para que los tests y la UI
 * aseveren ESTRUCTURA (qué regla, qué entidad, qué dos números) en vez de hacer
 * substring de un mensaje de texto.
 *
 * @param severidad     si aborta la generación ({@link Severidad#ERROR}) o solo informa
 * @param regla         identificador estable de la comprobación (ver las constantes
 *                      {@code REGLA_*} de {@link PrevalidacionService})
 * @param entidadCodigo código natural de la entidad señalada (profesor, actividad o
 *                      grupo); es el {@code codigo} del dominio del solver, no un id JPA
 * @param demanda       tramos que la entidad NECESITA según el catálogo; en el cuadre, las
 *                      horas de clase configuradas
 * @param disponible    tramos que la entidad TIENE; en el cuadre, las horas declaradas
 * @param descripcion   texto legible que explica el hallazgo
 */
public record AvisoPrevalidacion(
        Severidad severidad,
        String regla,
        String entidadCodigo,
        int demanda,
        int disponible,
        String descripcion) {

    public AvisoPrevalidacion {
        Objects.requireNonNull(severidad,     "severidad no puede ser null");
        Objects.requireNonNull(regla,         "regla no puede ser null");
        Objects.requireNonNull(entidadCodigo, "entidadCodigo no puede ser null");
        Objects.requireNonNull(descripcion,   "descripcion no puede ser null");
    }
}
