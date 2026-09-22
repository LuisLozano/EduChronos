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
 */
@Component
public class EstadoCurso implements SmartInitializingSingleton {

    private final CursoRepository cursos;

    private volatile String nombre;

    private volatile boolean archivado;

    /**
     * Hay un duplicado a medias AHORA MISMO. Lo mira la guarda de solo lectura para rechazar
     * escrituras mientras dura (fase B de C-duplicado-guarda).
     */
    private volatile boolean duplicando;

    public EstadoCurso(CursoRepository cursos) {
        this.cursos = cursos;
    }

    @Override
    public void afterSingletonsInstantiated() {
        cursos.findById(Curso.ID)
                .ifPresent(
                        curso -> {
                            this.nombre = curso.getNombre();
                            this.archivado = curso.isArchivado();
                        });
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

    /**
     * Empieza un duplicado. Desde aquí y hasta {@link #terminarDuplicado()} la guarda
     * rechaza toda escritura: es lo que cierra la ventana que la fase A dejó abierta entre la
     * copia y el archivado del origen, donde una escritura entrante se habría quedado en el
     * curso viejo sin llegar al nuevo.
     */
    public void iniciarDuplicado() {
        this.duplicando = true;
    }

    /**
     * Termina el duplicado, haya salido bien o mal. Se llama desde un {@code finally}: si
     * sólo se llamara en el camino de éxito, un nombre mal escrito dejaría la aplicación
     * rechazando escrituras para siempre, sin nada archivado y sin forma de salir salvo
     * reiniciar.
     */
    public void terminarDuplicado() {
        this.duplicando = false;
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
