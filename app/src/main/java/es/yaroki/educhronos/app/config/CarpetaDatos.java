package es.yaroki.educhronos.app.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Dónde viven los datos de Educhronos y cómo se crea esa carpeta (condición 7 de
 * O-instalación).
 *
 * <p><b>Por qué está aparte del post-procesador.</b> Esta decisión la necesitan DOS
 * llamadores que no se parecen en nada: {@link RutaBaseDatosEnvironmentPostProcessor}, que
 * corre dentro del arranque de Spring y tiene un {@code Environment} a mano, y el modo
 * escritorio, que la necesita ANTES de que exista contexto alguno —para el candado de
 * instancia única y para el fichero de log, los dos en esa misma carpeta—. Nada de lo que
 * hay aquí toca Spring: son {@code System.getProperty}, {@code System.getenv} y disco, todo
 * entrando por parámetro.
 *
 * <p>La clase no cambia ni una decisión de S153: es el mismo código, movido. El
 * post-procesador conserva sus dos métodos estáticos delegando aquí, porque su spec de 14
 * casos los llama por su nombre y ese spec no se toca.
 */
public final class CarpetaDatos {

    /** Nombre del fichero de base de datos dentro de la carpeta de datos. */
    public static final String NOMBRE_FICHERO = "educhronos.db";

    /** Carpeta bajo {@code %LOCALAPPDATA%}: en Windows los nombres van capitalizados. */
    public static final String CARPETA_WINDOWS = "Educhronos";

    /** Carpeta bajo el directorio de datos XDG: en el resto de sistemas, en minúsculas. */
    public static final String CARPETA_POSIX = "educhronos";

    /**
     * Fichero que dice QUÉ base de la carpeta está abierta (O-curso, S159). Sin extensión y
     * con nombre de prosa: no es una base, es un puntero de una línea.
     */
    public static final String NOMBRE_PUNTERO = "curso-abierto";

    private CarpetaDatos() {}

    /**
     * Resuelve la carpeta de datos leyendo el sistema de verdad y la crea. Es la entrada que
     * usa el modo escritorio desde {@code main}, sin {@code Environment} ni contexto de
     * Spring por ningún lado.
     *
     * @return la carpeta, ya existente y escribible
     * @throws IllegalStateException si no se puede decidir, crear o escribir en ella
     */
    public static Path resolverYCrear() {
        Path carpeta = resolver(
                System.getProperty("os.name"), System::getenv, System.getProperty("user.home"));
        crear(carpeta);
        return carpeta;
    }

    /**
     * Decide la carpeta de datos. Método PURO: no toca disco ni lee {@code System.*}, todo
     * lo que necesita entra por parámetro. Así las dos ramas de sistema operativo se prueban
     * desde cualquier máquina, que es la única forma de cubrir la de Windows.
     *
     * <p>En Windows manda {@code %LOCALAPPDATA%} y, si falta o viene en blanco,
     * {@code %USERPROFILE%\AppData\Local}. En el resto manda {@code $XDG_DATA_HOME} y, si
     * falta o viene en blanco, {@code <user.home>/.local/share}, que es el valor por defecto
     * que fija el propio estándar XDG: medido en S153, la variable está sin definir incluso
     * en un escritorio Linux corriente, así que el fallback no es el caso raro.
     *
     * @param nombreSistema valor de {@code os.name}; {@code null} se trata como no-Windows
     * @param entorno lectura de variables de entorno, normalmente {@code System::getenv}
     * @param directorioPersonal valor de {@code user.home}
     * @throws IllegalStateException si el sistema no ofrece ninguna de sus dos vías
     */
    public static Path resolver(
            String nombreSistema, UnaryOperator<String> entorno, String directorioPersonal) {
        if (esWindows(nombreSistema)) {
            String localAppData = entorno.apply("LOCALAPPDATA");
            if (tieneValor(localAppData)) {
                return Path.of(localAppData, CARPETA_WINDOWS);
            }
            String perfilUsuario = entorno.apply("USERPROFILE");
            if (tieneValor(perfilUsuario)) {
                return Path.of(perfilUsuario, "AppData", "Local", CARPETA_WINDOWS);
            }
            throw new IllegalStateException(
                    "No se puede decidir dónde guardar los datos de Educhronos: en Windows hacen "
                            + "falta LOCALAPPDATA o USERPROFILE, y las dos vienen vacías.");
        }

        String datosXdg = entorno.apply("XDG_DATA_HOME");
        if (tieneValor(datosXdg)) {
            return Path.of(datosXdg, CARPETA_POSIX);
        }
        if (tieneValor(directorioPersonal)) {
            return Path.of(directorioPersonal, ".local", "share", CARPETA_POSIX);
        }
        throw new IllegalStateException(
                "No se puede decidir dónde guardar los datos de Educhronos: hacen falta "
                        + "XDG_DATA_HOME o user.home, y las dos vienen vacías.");
    }

    /**
     * Crea la carpeta con todos sus padres y comprueba que se puede escribir en ella. Va
     * aparte de {@link #resolver} porque esto sí toca disco.
     *
     * <p><b>La guarda {@link Files#isWritable} NO la cubre ningún caso de prueba</b>
     * (D-guarda-escritura-sin-caso, medido en el M3 de S153: el mutante que la suprime
     * SOBREVIVE a los 14 casos). El único caso de carpeta imposible usa un fichero como
     * padre, y ahí revienta antes {@code createDirectories}, así que esta línea no llega a
     * ejecutarse nunca en la suite. No se cubre por dos razones: un {@code chmod 0555} sobre
     * un directorio temporal no discrimina si la suite corre como root —y la Fase 12 traerá
     * runners—, y en Windows {@code isWritable} no significa lo mismo que en POSIX: mira el
     * atributo de solo lectura, no los permisos efectivos, de modo que una carpeta sin
     * permiso de escritura real puede darlo por escribible. La guarda se queda porque el
     * diagnóstico que produce vale la pena: sin ella, el fallo sale luego como
     * SQLITE_CANTOPEN dentro del inicializador de scripts, sin nombrar la carpeta.
     *
     * @throws IllegalStateException nombrando la ruta, si no se puede crear o no es escribible
     */
    public static void crear(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se puede crear la carpeta de datos de Educhronos: " + carpeta
                            + ". Compruebe los permisos o arranque con "
                            + "--spring.datasource.url=jdbc:sqlite:<ruta de la base>",
                    e);
        }
        if (!Files.isWritable(carpeta)) {
            throw new IllegalStateException(
                    "La carpeta de datos de Educhronos no permite escribir: " + carpeta
                            + ". Compruebe los permisos o arranque con "
                            + "--spring.datasource.url=jdbc:sqlite:<ruta de la base>");
        }
    }

    /**
     * La base que hay que abrir dentro de la carpeta de datos: la que diga el puntero
     * {@link #NOMBRE_PUNTERO} o, si no hay puntero utilizable, {@link #NOMBRE_FICHERO}
     * (O-curso, S159).
     *
     * <p><b>El puntero guarda un NOMBRE de fichero, no una ruta</b>, y aquí se exige que lo
     * sea: un contenido con separadores se descarta en vez de resolverse. Si no, un puntero
     * con {@code ../../algo} abriría una base de fuera de la carpeta de datos, y el fichero
     * lo escribe un programa pero lo puede editar cualquiera.
     *
     * <p><b>Un puntero roto no impide arrancar.</b> Apunta a un fichero que ya no está, o
     * trae basura: se abre {@code educhronos.db} y se avisa. La alternativa —fallar— dejaría
     * al centro sin poder entrar por un fichero de una línea que él no escribió.
     *
     * <p>Toca disco, como {@link #crear}: mira si el puntero existe y si su destino existe.
     *
     * @param carpeta carpeta de datos, ya creada
     * @return el fichero de base a abrir, dentro de esa carpeta
     */
    public static Path baseAbierta(Path carpeta) {
        return baseAbierta(carpeta, aviso -> {});
    }

    /**
     * Igual que {@link #baseAbierta(Path)}, y ADEMÁS cuenta lo que le ha pasado. Es la que
     * usa el post-procesador, que tiene un log diferido donde registrar el aviso; la de un
     * argumento existe para probar la decisión sin montar un log.
     *
     * @param carpeta carpeta de datos, ya creada
     * @param aviso recibe un texto cuando hay un puntero y no se ha podido usar
     * @return el fichero de base a abrir, dentro de esa carpeta
     */
    public static Path baseAbierta(Path carpeta, Consumer<String> aviso) {
        Path porDefecto = carpeta.resolve(NOMBRE_FICHERO);
        Path puntero = carpeta.resolve(NOMBRE_PUNTERO);
        if (!Files.isRegularFile(puntero)) {
            return porDefecto;
        }
        String contenido;
        try {
            contenido = Files.readString(puntero, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se puede leer el puntero de curso abierto: " + puntero, e);
        }
        if (contenido.isEmpty() || !esNombreSimple(contenido)) {
            aviso.accept(
                    "El puntero " + puntero + " no contiene un nombre de fichero utilizable ("
                            + contenido + "): se abre " + NOMBRE_FICHERO + ".");
            return porDefecto;
        }
        Path apuntada = carpeta.resolve(contenido);
        if (!Files.isRegularFile(apuntada)) {
            aviso.accept(
                    "El puntero " + puntero + " apunta a " + contenido
                            + ", que no está en la carpeta de datos: se abre "
                            + NOMBRE_FICHERO + ".");
            return porDefecto;
        }
        return apuntada;
    }

    /**
     * ¿Es un nombre de fichero suelto? Un solo segmento, sin separador de NINGUNO de los dos
     * sistemas: la comprobación tiene que rechazar {@code ..\\otra} también cuando corre en
     * POSIX, porque el fichero puede venir de una carpeta sincronizada desde Windows.
     */
    private static boolean esNombreSimple(String contenido) {
        return !contenido.contains("/")
                && !contenido.contains("\\")
                && Path.of(contenido).getNameCount() == 1
                && !contenido.equals("..")
                && !contenido.equals(".");
    }

    private static boolean esWindows(String nombreSistema) {
        return nombreSistema != null && nombreSistema.toLowerCase(Locale.ROOT).contains("windows");
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
