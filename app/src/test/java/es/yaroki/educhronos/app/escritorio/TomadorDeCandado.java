package es.yaroki.educhronos.app.escritorio;

import java.nio.file.Path;

/**
 * Auxiliar de {@link InstanciaUnicaTest}: un programa que toma el candado de una carpeta,
 * avisa y se queda quieto hasta que lo maten.
 *
 * <p>Existe porque el candado de {@link InstanciaUnica} es del SISTEMA OPERATIVO y por
 * definición no se puede ejercer dentro de una sola JVM: los cerrojos de fichero de Java se
 * contabilizan por proceso, así que «otro Educhronos ya abierto» sólo se puede probar con
 * OTRO proceso de verdad. No es un test: no lleva {@code @Test} y nadie lo ejecuta salvo el
 * {@code ProcessBuilder} de ese spec.
 */
public final class TomadorDeCandado {

    /** Lo que imprime cuando ya tiene el candado; el spec espera exactamente esta línea. */
    public static final String LISTO = "LISTO";

    /** Lo que imprime si NO lo consigue, para que un fallo del andamio no parezca un acierto. */
    public static final String NO = "NO";

    private TomadorDeCandado() {}

    public static void main(String[] args) throws InterruptedException {
        boolean tomado = InstanciaUnica.intentarTomar(Path.of(args[0]));
        System.out.println(tomado ? LISTO : NO);
        System.out.flush();
        // Se queda vivo con el candado puesto hasta que el spec lo mate. El tope es una red
        // de seguridad por si el spec muriera antes de matarlo: un proceso huérfano
        // aguantando un cerrojo sería un fallo difícil de ver en la siguiente corrida.
        Thread.sleep(120_000);
    }
}
