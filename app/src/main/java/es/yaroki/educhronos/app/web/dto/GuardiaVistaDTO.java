package es.yaroki.educhronos.app.web.dto;

import java.util.Set;

/**
 * Proyección plana de una guardia repartida (S215, C-exportacion-guardias): el profesor que
 * está de guardia y dónde. Solo datos; lo ensambla {@code GeneradorHorarioService.proyectar}.
 * {@code dia} es 1..5 (lunes..viernes), como en {@link SesionVistaDTO}.
 *
 * <p>Desde S221 (C-exportacion-guardias-recreo, D1) lleva también las guardias de recreo.
 * {@code tipo} es el {@code name()} de {@code TipoGuardia}: ORDINARIA, RECREO o BIBLIOTECA.
 * Una ORDINARIA está en un tramo de clase: {@code tramo} es su ordenEnDia lectivo 1..6, con la
 * MISMA conversión que las sesiones, y {@code recreo} es null. Una de RECREO o de BIBLIOTECA
 * está en un recreo: {@code recreo} es su número de orden entre los no lectivos de su día,
 * desde 1 ({@code CatalogoMapper.indiceRecreoEnDia}), y {@code tramo} es null. El constructor
 * compacto rechaza cualquier otra combinación con {@link IllegalArgumentException}: un DTO que
 * dijera dos sitios a la vez, o ninguno, no se podría pintar.
 */
public record GuardiaVistaDTO(String profesorCodigo, int dia, Integer tramo, Integer recreo,
                              String tipo) {

    public static final String ORDINARIA = "ORDINARIA";
    public static final String RECREO = "RECREO";
    public static final String BIBLIOTECA = "BIBLIOTECA";

    private static final Set<String> TIPOS = Set.of(ORDINARIA, RECREO, BIBLIOTECA);

    public GuardiaVistaDTO {
        if (!TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de guardia desconocido: " + tipo);
        }
        boolean ordinaria = ORDINARIA.equals(tipo);
        if (ordinaria && (tramo == null || recreo != null)) {
            throw new IllegalArgumentException("Una guardia ORDINARIA lleva tramo y no recreo: tramo "
                    + tramo + ", recreo " + recreo);
        }
        if (!ordinaria && (recreo == null || tramo != null)) {
            throw new IllegalArgumentException("Una guardia de " + tipo + " lleva recreo y no tramo: tramo "
                    + tramo + ", recreo " + recreo);
        }
        if ((tramo != null && tramo < 1) || (recreo != null && recreo < 1)) {
            throw new IllegalArgumentException("El tramo y el recreo de una guardia empiezan en 1: tramo "
                    + tramo + ", recreo " + recreo);
        }
    }

    /** Una guardia ORDINARIA, la única que había hasta S221. */
    public GuardiaVistaDTO(String profesorCodigo, int dia, int tramo) {
        this(profesorCodigo, dia, tramo, null, ORDINARIA);
    }

    /** Si está en un recreo (RECREO o BIBLIOTECA) y no en un tramo de clase. */
    public boolean esDeRecreo() {
        return !ORDINARIA.equals(tipo);
    }
}
