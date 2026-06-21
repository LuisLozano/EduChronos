package es.yaroki.educhronos.solver.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Restricción horaria de un profesor sobre un tramo concreto.
 * Espejo de la entidad ProfesorRestriccionHoraria del modelo de Fase 1 (§4.3).
 *
 * Relación dirigida: RestriccionHoraria -> Profesor y RestriccionHoraria -> Tramo.
 * Los catálogos Profesor y Tramo NO conocen esta entidad (no se acoplan).
 *
 * peso: solo es semánticamente relevante si tipo == BLANDA (penalización en la
 *       función objetivo). En DURA se ignora (decisión 6b); se conserva el valor
 *       por defecto para no perder el dato si el JSON lo trae.
 */
public record RestriccionHoraria(
        Profesor profesor,
        Tramo tramo,
        TipoRestriccion tipo,
        int peso,
        Optional<String> motivo) {

    public RestriccionHoraria {
        Objects.requireNonNull(profesor, "profesor no puede ser null");
        Objects.requireNonNull(tramo,    "tramo no puede ser null");
        Objects.requireNonNull(tipo,     "tipo no puede ser null");
        Objects.requireNonNull(motivo,   "motivo no puede ser null (use Optional.empty())");
    }
}