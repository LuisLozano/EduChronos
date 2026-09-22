package es.yaroki.educhronos.app.curso;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * La identidad del curso abierto, en memoria (O-curso, S159).
 *
 * <p><b>Por qué en memoria y no una consulta por petición.</b> La fase B de
 * C-duplicado-guarda tiene que decidir si rechaza CADA escritura, y bajar a la base para
 * eso pondría una consulta en el camino de todas las peticiones. El dato cambia una vez en
 * la vida del proceso —al archivar, duplicando— y lo cambia quien lo escribe.
 *
 * <p><b>Por qué {@link SmartInitializingSingleton}.</b> La carga tiene que ocurrir después
 * de que {@code schema.sql} haya creado la tabla y antes de que el servidor web acepte la
 * primera petición. Un {@code @PostConstruct} corre demasiado pronto —el inicializador de
 * scripts es otro singleton y el orden no está garantizado— y un
 * {@code ApplicationReadyEvent} demasiado tarde: Tomcat ya escucha. Los singletons se
 * preinstancian enteros antes de {@code finishRefresh()}, que es donde arranca el servidor.
 *
 * <p>Los campos son {@code volatile} porque los escribe el hilo que duplica y los leen los
 * hilos de Tomcat.
 *
 * <p><b>Las tres operaciones largas se excluyen entre sí, y las transiciones son ATÓMICAS</b>
 * (S160, invariante I2 de C-selector-curso). Generar un horario, duplicar un curso y cambiar
 * de curso no pueden solaparse como quieran: un cambio de curso a mitad de un solve dejaría
 * el horario escrito en la base equivocada, y un solve arrancado a mitad de un cambio leería
 * un catálogo y escribiría en otro. Los tres {@code intentarIniciar…} son
 * {@code synchronized} y miran y marcan en el MISMO bloque: un «¿se puede?» seguido de un
 * «pues allá voy» en dos llamadas sueltas dejaría entre medias una ventana en la que otro
 * hilo también cree que se puede.
 *
 * <p>Tabla de exclusiones, que es el contrato entero:
 *
 * <pre>
 *   quiere empezar ↓   | generando &gt; 0 | duplicando | cambiando
 *   ───────────────────┼───────────────┼────────────┼──────────
 *   generar            |      SÍ       |     SÍ     |    NO
 *   duplicar           |      NO       |     NO     |    NO
 *   cambiar de curso   |      NO       |     NO     |    NO
 * </pre>
 *
 * <p>Generar es la única que admite compañía, y sólo de otra generación: dos solves a la vez
 * son dos lecturas y dos escrituras independientes sobre la misma base, que es lo que la
 * aplicación ya hacía antes de S160 y no se estrecha ahora. Por eso {@code generando} es un
 * CONTADOR y no un booleano: con un booleano, el segundo solve en terminar apagaría el
 * indicador con el primero aún dentro, y un cambio de curso se colaría en medio. Que
 * duplicar no aparezca como obstáculo para generar no es un descuido: {@code GuardaSoloLectura}
 * ya rechaza el {@code POST /api/horarios} con un 403 mientras se duplica, así que esa
 * combinación no llega hasta aquí por la vía HTTP, y bloquearla también aquí no añadiría
 * nada que se pueda observar.
 */
@Component
public class EstadoCurso implements SmartInitializingSingleton {

    /**
     * Se está cambiando de curso. Vive AQUÍ y no en quien la usa porque la consultan dos
     * capas que no se conocen —{@code GuardaSoloLectura}, que contesta 503 a la API, y
     * {@code GeneradorHorarioService}, que contesta 409 a quien pide un horario—, y el
     * símbolo que viaja al cliente tiene que ser el mismo en las dos.
     */
    public static final String CURSO_CAMBIANDO = "CURSO_CAMBIANDO";

    private final CursoRepository cursos;

    private volatile String nombre;

    private volatile boolean archivado;

    /**
     * Hay un duplicado a medias AHORA MISMO. Lo mira la guarda de solo lectura para rechazar
     * escrituras mientras dura (fase B de C-duplicado-guarda).
     */
    private volatile boolean duplicando;

    /**
     * CUÁNTOS solves hay en marcha. Contador y no bandera: ver la tabla de exclusiones en el
     * javadoc de clase.
     */
    private volatile int generando;

    /**
     * Se está abriendo otra base AHORA MISMO (S160). Mientras dura, la guarda rechaza toda
     * petición de la API con un 503 —también las de lectura, que es lo que lo distingue de
     * los otros dos indicadores—: durante el cambio, el pool al que iría a parar una consulta
     * puede ser el de antes o el de después, y responder con datos de una base que ya no es
     * la abierta es peor que decir que se espere un momento.
     */
    private volatile boolean cambiando;

    public EstadoCurso(CursoRepository cursos) {
        this.cursos = cursos;
    }

    @Override
    public void afterSingletonsInstantiated() {
        recargar();
    }

    /**
     * Relee la identidad del curso de la base ABIERTA y la sustituye entera (S160).
     *
     * <p>Sustituye y no completa: si la base que ahora está abierta no tiene fila de curso
     * —condición 6, una base de antes de S159—, lo que queda es {@code nombre} nulo y
     * {@code archivado} falso, y NO lo que hubiera de la base anterior. Con un
     * {@code ifPresent} a secas, cambiar a una base sin fila dejaría a la aplicación diciendo
     * que el curso abierto se llama como el de antes, y la guarda seguiría rechazando
     * escrituras por un archivado que ya no gobierna nada.
     *
     * <p>Lee por el repositorio y no por JDBC a propósito: quien tiene que contestar es el
     * pool VIGENTE, o sea la base que acaba de abrirse, sin que este objeto tenga que saber
     * qué fichero es.
     */
    public void recargar() {
        Curso curso = cursos.findById(Curso.ID).orElse(null);
        this.nombre = curso == null ? null : curso.getNombre();
        this.archivado = curso != null && curso.isArchivado();
    }

    /** Nombre del curso abierto, o {@code null} si la base no lo trae (condición 6). */
    public String nombre() {
        return nombre;
    }

    /** ¿Es el curso abierto de solo lectura? */
    public boolean archivado() {
        return archivado;
    }

    /** ¿Se está creando un curso nuevo en este instante? */
    public boolean duplicando() {
        return duplicando;
    }

    /** ¿Se está abriendo otra base en este instante? */
    public boolean cambiando() {
        return cambiando;
    }

    /** Cuántos horarios se están generando ahora mismo. Cero es «ninguno». */
    public int generando() {
        return generando;
    }

    /**
     * Empieza un duplicado, si se puede. Desde aquí y hasta {@link #terminarDuplicado()} la
     * guarda rechaza toda escritura: es lo que cierra la ventana que la fase A dejó abierta
     * entre la copia y el archivado del origen, donde una escritura entrante se habría
     * quedado en el curso viejo sin llegar al nuevo.
     *
     * <p>Desde S160 devuelve un booleano y puede decir que no: duplicar lee la base entera
     * con {@code VACUUM INTO}, y hacerlo mientras un solve escribe el horario o mientras se
     * está cambiando de base daría una copia de un curso que ya no es el que el usuario ve.
     *
     * @return {@code true} si el duplicado queda marcado; {@code false} si hay un solve, otro
     *     duplicado o un cambio de curso en marcha
     */
    public synchronized boolean intentarIniciarDuplicado() {
        if (cambiando || duplicando || generando > 0) {
            return false;
        }
        this.duplicando = true;
        return true;
    }

    /**
     * Termina el duplicado, haya salido bien o mal. Se llama desde un {@code finally}: si
     * sólo se llamara en el camino de éxito, un nombre mal escrito dejaría la aplicación
     * rechazando escrituras para siempre, sin nada archivado y sin forma de salir salvo
     * reiniciar.
     */
    public synchronized void terminarDuplicado() {
        this.duplicando = false;
    }

    /**
     * Da de alta un solve, si se puede (S160).
     *
     * @return {@code true} si queda contado; {@code false} si se está cambiando de curso
     */
    public synchronized boolean intentarIniciarGeneracion() {
        if (cambiando) {
            return false;
        }
        this.generando++;
        return true;
    }

    /**
     * Da de baja un solve. Se llama desde un {@code finally}, por lo mismo que
     * {@link #terminarDuplicado()}: un solve que revienta no puede dejar la cuenta alta para
     * siempre, porque entonces no se podría volver a cambiar de curso sin reiniciar.
     *
     * <p>No baja de cero: si un desajuste dejara la cuenta negativa, el bloqueo del cambio
     * dejaría de funcionar sin que nada lo dijera.
     */
    public synchronized void terminarGeneracion() {
        if (this.generando > 0) {
            this.generando--;
        }
    }

    /**
     * Empieza un cambio de curso, si se puede (S160). Es la operación más exigente de las
     * tres: necesita la casa vacía, porque va a tirar del pool que los demás están usando.
     *
     * @return {@code true} si el cambio queda marcado; {@code false} si hay un solve, un
     *     duplicado u otro cambio en marcha
     */
    public synchronized boolean intentarIniciarCambio() {
        if (generando > 0 || duplicando || cambiando) {
            return false;
        }
        this.cambiando = true;
        return true;
    }

    /** Termina el cambio, haya salido bien o mal. En un {@code finally}, siempre. */
    public synchronized void terminarCambio() {
        this.cambiando = false;
    }

    /**
     * Deja constancia de que la base abierta acaba de quedar archivada. Lo llama quien
     * duplica, DESPUÉS de haberlo escrito en el fichero: esto no persiste nada, refleja lo
     * que ya está en disco.
     *
     * @param nombre nombre con el que quedó archivado el curso
     */
    public void marcarArchivado(String nombre) {
        this.nombre = nombre;
        this.archivado = true;
    }
}
