package es.yaroki.educhronos.app.escritorio;

/**
 * Pide al sistema que no suspenda el equipo ni apague la pantalla mientras dura algo largo
 * (S238, C-sin-suspension): la generación de un horario puede tardar minutos, y un portátil
 * que se suspende a mitad la deja sin terminar.
 *
 * <p>Hay dos: {@link MantenerDespiertoWindows}, que se lo pide a Windows, y
 * {@link MantenerDespiertoNulo}, que no hace nada y es el de cualquier otro sistema. Ninguno
 * lanza: quedarse sin la petición empeora la espera, pero no es motivo para no generar.
 */
public interface MantenerDespierto {

    /**
     * Pide que el equipo no se suspenda hasta que se cierre la petición devuelta.
     *
     * @param motivo el texto que el sistema enseña a quien pregunte por qué no se suspende
     * @return la petición; si no se pudo pedir, una que no hace nada
     */
    Peticion pedir(String motivo);

    /** Una petición en vigor. Cerrarla la suelta; cerrarla otra vez no hace nada. */
    interface Peticion extends AutoCloseable {

        /** La que no pide nada: la del nulo y la que queda cuando pedir falla. */
        Peticion NINGUNA = () -> { };

        @Override
        void close();
    }
}
