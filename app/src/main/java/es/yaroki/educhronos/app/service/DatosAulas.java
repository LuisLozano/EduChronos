package es.yaroki.educhronos.app.service;

import java.util.List;
import java.util.Objects;

/**
 * Lo que la prevalidación de aulas necesita y el {@code ProblemaHorario} del solver no lleva
 * (S207, C-deduccion-aulas, B1): qué plazas de CLASE se han quedado sin ninguna aula posible y
 * por qué. En el problema esas plazas viajan sin aula, igual que una reunión, así que el motivo
 * solo puede venir de aquí. Molde de {@link DatosCuadre}.
 *
 * <p>Lo construye {@link GeneradorHorarioService#cargarDatosAulas()} con el mismo componente que
 * construye el problema ({@code DeduccionAulas}), para que los dos no discrepen nunca.
 *
 * @param sinAulaPosible plazas de CLASE con el dominio de aulas vacío, en el orden del catálogo
 */
public record DatosAulas(List<PlazaSinAula> sinAulaPosible) {

    /** Ninguna plaza sin aula posible: las reglas de aulas no emiten nada. */
    public static final DatosAulas VACIO = new DatosAulas(List.of());

    public DatosAulas {
        Objects.requireNonNull(sinAulaPosible, "sinAulaPosible no puede ser null");
        sinAulaPosible = List.copyOf(sinAulaPosible);
    }

    /** Una plaza de CLASE sin aula posible, por códigos, con el motivo que verá el usuario. */
    public record PlazaSinAula(String actividadCodigo, String plazaCodigo, String motivo) {
        public PlazaSinAula {
            Objects.requireNonNull(actividadCodigo, "actividadCodigo no puede ser null");
            Objects.requireNonNull(plazaCodigo, "plazaCodigo no puede ser null");
            Objects.requireNonNull(motivo, "motivo no puede ser null");
        }
    }
}
