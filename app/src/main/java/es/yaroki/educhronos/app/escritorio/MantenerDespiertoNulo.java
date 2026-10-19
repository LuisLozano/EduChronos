package es.yaroki.educhronos.app.escritorio;

/**
 * El {@link MantenerDespierto} de todo sistema que no sea Windows, y el que queda cuando JNA no
 * carga: no pide nada y no escribe nada en el log.
 */
public final class MantenerDespiertoNulo implements MantenerDespierto {

    @Override
    public Peticion pedir(String motivo) {
        return Peticion.NINGUNA;
    }
}
