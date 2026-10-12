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

    /** Los dos mínimos de cada recreo (S219): profesores de guardia y, de ellos, en la biblioteca. */
    public static void fijarRecreo(ConfiguracionRepository configuraciones, int minimoRecreo, int minimoBiblioteca) {
        configuraciones.save(new Configuracion(ConfiguracionGuardias.CLAVE_MINIMO_RECREO,
                Integer.toString(minimoRecreo)));
        configuraciones.save(new Configuracion(ConfiguracionGuardias.CLAVE_MINIMO_BIBLIOTECA,
                Integer.toString(minimoBiblioteca)));
    }
}
