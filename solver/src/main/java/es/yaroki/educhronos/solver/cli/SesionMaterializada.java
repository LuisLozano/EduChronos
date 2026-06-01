package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.Tramo;

import java.util.Objects;

/**
 * Aplanado de una sesión planificada para la presentación CLI.
 * Una ActividadInstancia colocada en un Tramo produce tantas
 * SesionMaterializada como plazas tenga su actividad.
 *
 * Estructura intermedia de la capa de presentación. No es parte del
 * dominio: solo se usa para construir las vistas tabulares.
 */
record SesionMaterializada(Tramo tramo, ActividadInstancia instancia, Plaza plaza) {

    SesionMaterializada {
        Objects.requireNonNull(tramo,     "tramo no puede ser null");
        Objects.requireNonNull(instancia, "instancia no puede ser null");
        Objects.requireNonNull(plaza,     "plaza no puede ser null");
    }
}