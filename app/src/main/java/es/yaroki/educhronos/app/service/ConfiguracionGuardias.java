package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.ConfiguracionRepository;

/**
 * El mínimo de profesores de guardia por tramo de clase del centro (S212, C-dato-guardias): dónde
 * se guarda y cuánto vale si no se ha guardado. Es el ÚNICO sitio que lo lee, y no es un bean:
 * lo llaman {@link GeneradorHorarioService} (al construir {@link DatosCuadre}) y
 * {@link ConfiguracionGuardiasService} (el endpoint), cada uno con su repositorio. Así la
 * generación no gana un colaborador nuevo que los slices de test tendrían que importar.
 *
 * <p>El valor vive en la tabla clave-valor {@code configuracion} con {@link #CLAVE}. Sin fila
 * vale {@link #MINIMO_POR_DEFECTO}: un centro que nunca lo ha tocado necesita 4 profesores de
 * guardia en cada tramo, y 0 quiere decir que el centro no usa guardias.
 */
public final class ConfiguracionGuardias {

    /** Clave del mínimo en la tabla {@code configuracion}. */
    public static final String CLAVE = "guardias.minimoPorTramo";

    /** El mínimo cuando la tabla no tiene la {@link #CLAVE}. */
    public static final int MINIMO_POR_DEFECTO = 4;

    private ConfiguracionGuardias() {
    }

    /** El mínimo guardado, o {@link #MINIMO_POR_DEFECTO} si no hay fila. */
    public static int minimoPorTramo(ConfiguracionRepository repositorio) {
        return repositorio.findById(CLAVE)
                .map(fila -> Integer.parseInt(fila.getValor()))
                .orElse(MINIMO_POR_DEFECTO);
    }
}
