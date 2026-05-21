package es.yaroki.educhronos.solver.domain;

import java.util.Objects;
import java.util.Optional;

public record GrupoAdministrativo(
        String codigo,
        TipoGrupo tipo,
        Optional<GrupoAdministrativo> grupoPadre) {

    public GrupoAdministrativo {
        Objects.requireNonNull(codigo,     "codigo no puede ser null");
        Objects.requireNonNull(tipo,       "tipo no puede ser null");
        Objects.requireNonNull(grupoPadre, "grupoPadre no puede ser null (usa Optional.empty())");
        // I5: PDC debe tener grupo padre
        if (tipo == TipoGrupo.DIVERSIFICACION_PDC && grupoPadre.isEmpty())
            throw new IllegalArgumentException(
                    "Un grupo DIVERSIFICACION_PDC debe tener grupoPadre: " + codigo);
    }
}