package es.yaroki.educhronos.app.escritorio;

/**
 * Si este arranque es el del bundle de escritorio o el de siempre.
 *
 * <p>Lo decide una propiedad de sistema y no un perfil de Spring ni un argumento, por dos
 * razones: la pone {@code jpackage} con {@code --java-options}, que sólo sabe pasar opciones
 * de JVM, y hace falta leerla en {@code main} ANTES de construir nada de Spring.
 *
 * <p>{@link #PROPIEDAD} es la única fuente del literal. El
 * {@code scripts/empaquetar-windows.ps1} lo copia tal cual, y el paso 8 de S154 comprueba
 * con {@code grep} que las dos cadenas son idénticas: si alguien renombra la propiedad aquí
 * y no allí, el bundle arrancaría en modo servidor —sin navegador, sin bandeja y sin forma
 * de cerrarlo— sin dar un solo error.
 */
public final class ModoEscritorio {

    /** Propiedad de sistema que activa el modo escritorio. Única fuente del literal. */
    public static final String PROPIEDAD = "educhronos.escritorio";

    /** El único valor que activa el modo. */
    static final String VALOR_ACTIVO = "true";

    private ModoEscritorio() {}

    /** Lee la propiedad de verdad. Lo que decide vive en {@link #activo(String)}. */
    public static boolean activo() {
        return activo(System.getProperty(PROPIEDAD));
    }

    /**
     * Función PURA sobre el valor de la propiedad, para poder probarla sin tocar las
     * propiedades de sistema de la JVM que corre la suite.
     *
     * <p><b>La comparación es EXACTA</b>: distingue mayúsculas y no recorta espacios, así que
     * {@code "TRUE"}, {@code "True"} y {@code "true "} NO activan el modo. Es deliberado.
     * Quien produce este valor no es una persona tecleando, sino una constante de este mismo
     * código copiada al guion de empaquetado, de modo que la tolerancia no compra nada real y
     * a cambio haría ambiguo el contrato. {@code null} —la propiedad sin definir— es el caso
     * corriente del arranque de desarrollo.
     *
     * @param valor valor de la propiedad, o {@code null} si no está definida
     */
    public static boolean activo(String valor) {
        return VALOR_ACTIVO.equals(valor);
    }
}
