package es.yaroki.educhronos.app.service;

import java.util.Objects;

/**
 * Un hallazgo de la pre-validación de condiciones necesarias (Fase 8, Bloque 8.4-A,
 * deuda D18): una comparación {@code demanda > disponible} que falló, con la entidad
 * concreta que la provoca.
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
 * @param demanda       tramos que la entidad NECESITA según el catálogo
 * @param disponible    tramos que la entidad TIENE; el aviso se emite si
 *                      {@code demanda > disponible} (la igualdad NO es un fallo)
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
