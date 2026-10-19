package es.yaroki.educhronos.app.escritorio;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pide a Windows que no suspenda el equipo ni apague la pantalla, con
 * {@code PowerCreateRequest}/{@code PowerSetRequest} de {@code kernel32} por JNA (S238,
 * C-sin-suspension). Es lo que hace un reproductor de vídeo: mientras la petición está en
 * vigor, {@code powercfg /requests} la enseña con su motivo.
 *
 * <p><b>Las firmas, la estructura y las constantes son las de la sonda</b> que pasó en Windows
 * (rama experimental de S238, {@code Sonda.java:21-46}). La interfaz de {@code kernel32} entra
 * por el constructor para poder probar la clase con un doble, sin Windows.
 *
 * <p><b>No lanza nunca.</b> Si Windows no crea la petición o no acepta una de las dos, se
 * avisa por el log, se suelta lo que se hubiera puesto y se devuelve una petición que no hace
 * nada: la generación sigue igual, solo que el equipo puede suspenderse.
 */
public final class MantenerDespiertoWindows implements MantenerDespierto {

    private static final Logger LOG = LoggerFactory.getLogger(MantenerDespiertoWindows.class);

    /** {@code PowerRequestDisplayRequired}: que no se apague la pantalla. */
    static final int DISPLAY = 0;

    /** {@code PowerRequestSystemRequired}: que no se suspenda el equipo. */
    static final int SYSTEM = 1;

    /** {@code POWER_REQUEST_CONTEXT_VERSION}. */
    static final int VERSION = 0;

    /** {@code POWER_REQUEST_CONTEXT_SIMPLE_STRING}: el motivo es una cadena sin más. */
    static final int FLAGS = 1;

    /** Lo que se usa de {@code kernel32}; se carga con {@code Native.load("kernel32", ...)}. */
    public interface Kernel32Energia extends Library {
        Pointer PowerCreateRequest(ReasonContext contexto);
        boolean PowerSetRequest(Pointer peticion, int tipo);
        boolean PowerClearRequest(Pointer peticion, int tipo);
        boolean CloseHandle(Pointer objeto);
    }

    /**
     * {@code REASON_CONTEXT}: ULONG Version; DWORD Flags; union { struct Detailed (HMODULE, ULONG,
     * ULONG, LPWSTR*); LPWSTR SimpleReasonString; }. El primer puntero de la unión lleva el texto.
     */
    @Structure.FieldOrder({"Version", "Flags", "Razon", "LocalizedReasonId", "ReasonStringCount", "ReasonStrings"})
    public static class ReasonContext extends Structure {
        public int Version;
        public int Flags;
        public Pointer Razon;
        public int LocalizedReasonId;
        public int ReasonStringCount;
        public Pointer ReasonStrings;
    }

    private final Kernel32Energia kernel32;

    public MantenerDespiertoWindows(Kernel32Energia kernel32) {
        this.kernel32 = kernel32;
    }

    @Override
    public Peticion pedir(String motivo) {
        try {
            return pedirAWindows(motivo);
        } catch (LinkageError | RuntimeException e) {
            LOG.warn("No se ha podido pedir a Windows que no suspenda el equipo mientras se genera", e);
            return Peticion.NINGUNA;
        }
    }

    private Peticion pedirAWindows(String motivo) {
        // WCHAR_SIZE y no 2 como en la sonda: en Windows es 2, pero en Linux (la suite) es 4.
        Memory texto = new Memory((motivo.length() + 1L) * Native.WCHAR_SIZE);
        texto.setWideString(0, motivo);
        ReasonContext contexto = new ReasonContext();
        contexto.Version = VERSION;
        contexto.Flags = FLAGS;
        contexto.Razon = texto;
        Pointer handle = kernel32.PowerCreateRequest(contexto);
        if (handle == null || Pointer.nativeValue(handle) == -1L) {
            LOG.warn("Windows no ha creado la petición de energía (PowerCreateRequest, error {}):"
                    + " el equipo puede suspenderse mientras se genera", Native.getLastError());
            return Peticion.NINGUNA;
        }
        PeticionWindows peticion = new PeticionWindows(handle, texto);
        try {
            if (!peticion.poner(SYSTEM) || !peticion.poner(DISPLAY)) {
                peticion.liberar();
                return Peticion.NINGUNA;
            }
        } catch (LinkageError | RuntimeException e) {
            // Con el handle ya creado, lo puesto se suelta aquí; el aviso lo da pedir().
            peticion.liberar();
            throw e;
        }
        LOG.info("Windows no suspenderá el equipo ni apagará la pantalla mientras se genera");
        return peticion;
    }

    /**
     * La petición en vigor. Guarda el texto del motivo hasta cerrarse: Windows lo recibe por
     * puntero, y la sonda lo tenía en un campo estático para que no se liberase antes.
     */
    private final class PeticionWindows implements Peticion {

        private final Pointer handle;
        private final long inicio = System.nanoTime();
        private Memory texto;
        private boolean sistema;
        private boolean pantalla;
        private boolean cerrada;

        PeticionWindows(Pointer handle, Memory texto) {
            this.handle = handle;
            this.texto = texto;
        }

        /** Pone una de las dos; si Windows no la acepta, lo avisa con su código de error. */
        boolean poner(int tipo) {
            if (!kernel32.PowerSetRequest(handle, tipo)) {
                LOG.warn("Windows no ha aceptado la petición {} (PowerSetRequest, error {}):"
                        + " el equipo puede suspenderse mientras se genera", nombre(tipo), Native.getLastError());
                return false;
            }
            if (tipo == SYSTEM) {
                sistema = true;
            } else {
                pantalla = true;
            }
            return true;
        }

        @Override
        public void close() {
            if (cerrada) {
                return;
            }
            liberar();
            LOG.info("Windows vuelve a poder suspender el equipo y apagar la pantalla duracionMs={}",
                    (System.nanoTime() - inicio) / 1_000_000);
        }

        /** Quita lo puesto y cierra el handle, una sola vez. Lo que falle se avisa, no se lanza. */
        void liberar() {
            if (cerrada) {
                return;
            }
            cerrada = true;
            try {
                if (sistema && !kernel32.PowerClearRequest(handle, SYSTEM)) {
                    LOG.warn("PowerClearRequest SYSTEM ha fallado, error {}", Native.getLastError());
                }
                if (pantalla && !kernel32.PowerClearRequest(handle, DISPLAY)) {
                    LOG.warn("PowerClearRequest DISPLAY ha fallado, error {}", Native.getLastError());
                }
                if (!kernel32.CloseHandle(handle)) {
                    LOG.warn("CloseHandle de la petición de energía ha fallado, error {}", Native.getLastError());
                }
            } catch (LinkageError | RuntimeException e) {
                LOG.warn("No se ha podido soltar la petición de energía de Windows", e);
            }
            texto = null;
        }
    }

    private static String nombre(int tipo) {
        return tipo == SYSTEM ? "SYSTEM" : "DISPLAY";
    }
}
