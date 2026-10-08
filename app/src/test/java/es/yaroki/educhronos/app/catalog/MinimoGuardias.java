package es.yaroki.educhronos.app.catalog;

import es.yaroki.educhronos.app.service.ConfiguracionGuardias;

/**
 * Fija en la base de un test el mínimo de profesores de guardia por tramo (S212, C-dato-guardias):
 * inserta o actualiza la fila de {@link ConfiguracionGuardias#CLAVE}. Los tests que generan sobre un
 * catálogo sin guardias lo ponen a 0, un centro que no usa guardias; sin él valdría el 4 por defecto
 * y toda generación acabaría en GUARDIAS_INSUFICIENTES.
 */
public final class MinimoGuardias {

    private MinimoGuardias() {
    }

    public static void fijar(ConfiguracionRepository configuraciones, int minimo) {
        configuraciones.save(new Configuracion(ConfiguracionGuardias.CLAVE, Integer.toString(minimo)));
    }
}
